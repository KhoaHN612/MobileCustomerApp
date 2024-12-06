package com.example.coursework.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.coursework.Model.Order;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderDAO {
    public static final String TABLE_NAME = "orders";
    public static final String ID_COLUMN = "id";
    public static final String CLASS_ID_COLUMN = "class_id";
    public static final String ORDER_DATE_COLUMN = "order_date";
    public static final String USER_ID_COLUMN = "user_id";

    public static final String CREATE_TABLE = String.format(
            "CREATE TABLE %s (" +
                    " %s TEXT PRIMARY KEY, " +
                    " %s TEXT, " +
                    " %s TEXT, " +
                    " %s TEXT)",
            TABLE_NAME, ID_COLUMN, CLASS_ID_COLUMN, ORDER_DATE_COLUMN, USER_ID_COLUMN);

    private SQLiteDatabase db;
    private final Context context;

    public OrderDAO(SQLiteDatabase db, Context context) {
        this.db = db;
        this.context = context;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void syncOrdersFromFirestoreToSQLite() {
        if (!isNetworkAvailable()) {
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("orders")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> firestoreOrderIds = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            firestoreOrderIds.add(document.getId());

                            String firestoreId = document.getString(ID_COLUMN);
                            String classId = document.getString(CLASS_ID_COLUMN);
                            String orderDate = document.getString(ORDER_DATE_COLUMN);
                            String userId = document.getString(USER_ID_COLUMN);

                            Order order = new Order(firestoreId, classId, orderDate, userId);
                            insertOrUpdateOrderInSQLite(order);
                        }
                        removeDeletedOrdersFromSQLite(firestoreOrderIds);
                    } else {
                        Log.e("FirestoreSync", "Error getting orders from Firestore", task.getException());
                    }
                });
    }

    private void insertOrUpdateOrderInSQLite(Order order) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, order.getId());
        values.put(CLASS_ID_COLUMN, order.getClassId());
        values.put(ORDER_DATE_COLUMN, order.getOrderDate());
        values.put(USER_ID_COLUMN, order.getUserId());

        Cursor cursor = db.query(TABLE_NAME, null, ID_COLUMN + "=?", new String[]{order.getId()}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            db.update(TABLE_NAME, values, ID_COLUMN + "=?", new String[]{order.getId()});
        } else {
            db.insert(TABLE_NAME, null, values);
        }
        if (cursor != null) cursor.close();
    }

    private void removeDeletedOrdersFromSQLite(List<String> firestoreOrderIds) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{ID_COLUMN}, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String localOrderId = cursor.getString(cursor.getColumnIndexOrThrow(ID_COLUMN));
                if (!firestoreOrderIds.contains(localOrderId)) {
                    deleteOrderFromSQLite(localOrderId);
                }
            }
            cursor.close();
        }
    }

    public void deleteOrderFromSQLite(String firestoreId) {
        db.delete(TABLE_NAME, ID_COLUMN + " = ?", new String[]{firestoreId});
    }

    public ArrayList<Order> getOrdersByClassId(String classId) {
        ArrayList<Order> orders = new ArrayList<>();
        Cursor cursor = db.query(TABLE_NAME, null, CLASS_ID_COLUMN + "=?", new String[]{classId}, null, null, ORDER_DATE_COLUMN);

        if (cursor != null) {
            try {
                int firestoreIdIndex = cursor.getColumnIndexOrThrow(ID_COLUMN);
                int orderDateIndex = cursor.getColumnIndexOrThrow(ORDER_DATE_COLUMN);
                int userEmailIndex = cursor.getColumnIndexOrThrow(USER_ID_COLUMN);

                while (cursor.moveToNext()) {
                    String firestoreId = cursor.getString(firestoreIdIndex);
                    String orderDate = cursor.getString(orderDateIndex);
                    String userEmail = cursor.getString(userEmailIndex);

                    orders.add(new Order(firestoreId, classId, orderDate, userEmail));
                }
            } catch (Exception e) {
                Log.e("OrderDAO", "Error retrieving orders by class ID from SQLite", e);
            } finally {
                cursor.close();
            }
        }
        return orders;
    }

    public void deleteOrderByClassId(String classId) {
        db.delete(TABLE_NAME, CLASS_ID_COLUMN + " = ?", new String[]{classId});

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("orders")
                .whereEqualTo(CLASS_ID_COLUMN, classId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        snapshot.getReference().delete()
                                .addOnSuccessListener(aVoid -> Log.d("FirestoreDelete", "Order deleted successfully from Firestore"))
                                .addOnFailureListener(e -> Log.e("FirestoreDelete", "Error deleting order from Firestore", e));
                    }
                })
                .addOnFailureListener(e -> Log.e("FirestoreFetch", "Error fetching orders to delete from Firestore", e));
    }

    public void deleteAllOrders() {
        db.delete(TABLE_NAME, null, null);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("orders")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        snapshot.getReference().delete()
                                .addOnSuccessListener(aVoid -> Log.d("FirestoreDelete", "Order deleted successfully from Firestore"))
                                .addOnFailureListener(e -> Log.e("FirestoreDelete", "Error deleting order from Firestore", e));
                    }
                });
    }

    private Map<String, Object> createOrderDataMap(Order order) {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put(ID_COLUMN, order.getId());
        orderData.put(USER_ID_COLUMN, order.getUserId());
        orderData.put(CLASS_ID_COLUMN, order.getClassId());
        orderData.put(ORDER_DATE_COLUMN, order.getOrderDate());
        return orderData;
    }

}
