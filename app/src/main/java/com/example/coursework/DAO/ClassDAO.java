package com.example.coursework.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.coursework.Model.YogaClass;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClassDAO {
    public static final String TABLE_NAME = "yoga_classes";
    public static final String ID_COLUMN = "id";
    public static final String COURSE_ID_COLUMN = "course_id";
    public static final String DATE_COLUMN = "date";
    public static final String TEACHER_COLUMN = "teacher";
    public static final String COMMENTS_COLUMN = "comments";

    public static final String CREATE_TABLE = String.format(
            "CREATE TABLE %s (" +
                    " %s TEXT PRIMARY KEY, " +
                    " %s TEXT, " +
                    " %s TEXT, " +
                    " %s TEXT, " +
                    " %s TEXT, " +
                    "FOREIGN KEY(%s) REFERENCES yoga_courses(%s))",
            TABLE_NAME, ID_COLUMN, COURSE_ID_COLUMN, DATE_COLUMN, TEACHER_COLUMN, COMMENTS_COLUMN,
            COURSE_ID_COLUMN, COURSE_ID_COLUMN);

    private SQLiteDatabase db;
    private final Context context;
    private OrderDAO orderDAO;

    public ClassDAO(SQLiteDatabase db, Context context) {
        this.db = db;
        this.context = context;
        this.orderDAO = new OrderDAO(db, context);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void insertOrUpdateClass(YogaClass yogaClass) {
        if (yogaClass.getId() == null || yogaClass.getId().isEmpty()) {
            yogaClass.setId(UUID.randomUUID().toString());
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> classData = createClassDataMap(yogaClass);

        insertClassIntoSQLite(yogaClass);

        firestore.collection("yoga_classes")
                .document(yogaClass.getId())
                .set(classData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreSync", "Class synced successfully with Firestore");
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreSync", "Error syncing class with Firestore", e);
                });
        
    }

    private void insertClassIntoSQLite(YogaClass yogaClass) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, yogaClass.getId());
        values.put(COURSE_ID_COLUMN, yogaClass.getCourseId());
        values.put(DATE_COLUMN, yogaClass.getDate());
        values.put(TEACHER_COLUMN, yogaClass.getTeacher());
        values.put(COMMENTS_COLUMN, yogaClass.getComments());

        Cursor cursor = db.query(TABLE_NAME, null, ID_COLUMN + "=?", new String[]{yogaClass.getId()}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            db.update(TABLE_NAME, values, ID_COLUMN + "=?", new String[]{yogaClass.getId()});
        } else {
            db.insert(TABLE_NAME, null, values);
        }
        if (cursor != null) cursor.close();
    }

    public void syncClassesFromFirestoreToSQLite() {
        if (!isNetworkAvailable()){
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("yoga_classes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> classIds = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            classIds.add(document.getId());

                            String id = document.getString(ID_COLUMN);
                            String courseId = document.getString(COURSE_ID_COLUMN);
                            String date = document.getString(DATE_COLUMN);
                            String teacher = document.getString(TEACHER_COLUMN);
                            String comments = document.getString(COMMENTS_COLUMN);

                            YogaClass yogaClass = new YogaClass(id, courseId, date, teacher, comments);
                            insertOrUpdateClass(yogaClass);
                        }
                        removeDeletedClassesFromSQLite(classIds);
                    } else {
                        Log.e("FirestoreSync", "Error getting classes from Firestore", task.getException());
                    }
                });
    }

    public interface CompletionListener {
        void onComplete(boolean success);
    }

    public void syncClassesFromFirestoreToSQLite(CompletionListener listener) {

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("yoga_classes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> classIds = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            classIds.add(document.getId());

                            String id = document.getString(ID_COLUMN);
                            String courseId = document.getString(COURSE_ID_COLUMN);
                            String date = document.getString(DATE_COLUMN);
                            String teacher = document.getString(TEACHER_COLUMN);
                            String comments = document.getString(COMMENTS_COLUMN);

                            YogaClass yogaClass = new YogaClass(id, courseId, date, teacher, comments);
                            insertOrUpdateClass(yogaClass);
                        }
                        removeDeletedClassesFromSQLite(classIds);

                        if (listener != null) listener.onComplete(true);
                    } else {
                        Log.e("FirestoreSync", "Error getting classes from Firestore", task.getException());
                        if (listener != null) listener.onComplete(false);
                    }
                });
    }

    private void removeDeletedClassesFromSQLite(List<String> firestoreClassIds) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{ID_COLUMN}, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String localClassId = cursor.getString(cursor.getColumnIndexOrThrow(ID_COLUMN));
                if (!firestoreClassIds.contains(localClassId)) {
                    deleteClassFromSQLite(localClassId);
                }
            }
            cursor.close();
        }
    }

    public void deleteClassFromSQLite(String firestoreId) {
        db.delete(TABLE_NAME, ID_COLUMN + " = ?", new String[]{firestoreId});
    }

    private void deleteClassesFromSQLiteByCourseId(String courseId) {
        db.delete("yoga_classes", "course_id = ?", new String[]{courseId});
        Log.d("ClassDAO", "All classes for course " + courseId + " deleted from SQLite");
    }


    public void deleteClassesByCourseId(String courseId) {

        List<YogaClass> classesToDelete = getClassesByCourseId(courseId);
        for (YogaClass yogaClass : classesToDelete) {
            orderDAO.deleteOrderByClassId(yogaClass.getId());
        }

        deleteClassesFromSQLiteByCourseId(courseId);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("yoga_classes")
                .whereEqualTo("course_id", courseId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            String classId = document.getId();

                            document.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("FirestoreDelete", "Class deleted from Firestore successfully");
                                    })
                                    .addOnFailureListener(e -> Log.e("FirestoreDelete", "Error deleting class from Firestore", e));
                        }
                    }
                });
    }

    public void deleteClassById(String id) {
        orderDAO.deleteOrderByClassId(id);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("carts")
                .whereArrayContains("items", id)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        List<String> items = (List<String>) snapshot.get("items");
                        if (items != null) {
                            items.remove(id);
                            snapshot.getReference().update("items", items);
                            if (items.isEmpty()) {
                                snapshot.getReference().delete();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FirestoreCartDelete", "Error deleting cart items", e));

        deleteClassFromSQLite(id);

        firestore.collection("yoga_classes")
                .document(id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreDelete", "Class deleted from Firestore successfully");
                })
                .addOnFailureListener(e -> Log.e("FirestoreDelete", "Error deleting class from Firestore", e));
    }

    private Map<String, Object> createClassDataMap(YogaClass yogaClass) {
        Map<String, Object> classData = new HashMap<>();
        classData.put(ID_COLUMN, yogaClass.getId());
        classData.put(COURSE_ID_COLUMN, yogaClass.getCourseId());
        classData.put(DATE_COLUMN, yogaClass.getDate());
        classData.put(TEACHER_COLUMN, yogaClass.getTeacher());
        classData.put(COMMENTS_COLUMN, yogaClass.getComments());
        return classData;
    }

    public ArrayList<YogaClass> getAllClasses() {
        if (isNetworkAvailable()){
            syncClassesFromFirestoreToSQLite();
        }
        ArrayList<YogaClass> classes = new ArrayList<>();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, DATE_COLUMN);

        if (cursor != null) {
            try {
                int firestoreIdIndex = cursor.getColumnIndexOrThrow(ID_COLUMN);
                int courseIdIndex = cursor.getColumnIndexOrThrow(COURSE_ID_COLUMN);
                int dateIndex = cursor.getColumnIndexOrThrow(DATE_COLUMN);
                int teacherIndex = cursor.getColumnIndexOrThrow(TEACHER_COLUMN);
                int commentsIndex = cursor.getColumnIndexOrThrow(COMMENTS_COLUMN);

                while (cursor.moveToNext()) {
                    String firestoreId = cursor.getString(firestoreIdIndex);
                    String courseId = cursor.getString(courseIdIndex);
                    String date = cursor.getString(dateIndex);
                    String teacher = cursor.getString(teacherIndex);
                    String comments = cursor.getString(commentsIndex);

                    classes.add(new YogaClass(firestoreId, courseId, date, teacher, comments));
                }
            } catch (Exception e) {
                Log.e("ClassDAO", "Error retrieving classes from SQLite", e);
            } finally {
                cursor.close();
            }
        }
        return classes;
    }

    public ArrayList<YogaClass> getClassesByCourseId(String courseId) {
        if (isNetworkAvailable()){
            syncClassesFromFirestoreToSQLite();
        }
        ArrayList<YogaClass> classes = new ArrayList<>();
        Cursor cursor = db.query(TABLE_NAME, null, COURSE_ID_COLUMN + "=?", new String[]{courseId}, null, null, DATE_COLUMN);

        if (cursor != null) {
            try {
                int firestoreIdIndex = cursor.getColumnIndexOrThrow(ID_COLUMN);
                int dateIndex = cursor.getColumnIndexOrThrow(DATE_COLUMN);
                int teacherIndex = cursor.getColumnIndexOrThrow(TEACHER_COLUMN);
                int commentsIndex = cursor.getColumnIndexOrThrow(COMMENTS_COLUMN);

                while (cursor.moveToNext()) {
                    String firestoreId = cursor.getString(firestoreIdIndex);
                    String date = cursor.getString(dateIndex);
                    String teacher = cursor.getString(teacherIndex);
                    String comments = cursor.getString(commentsIndex);

                    classes.add(new YogaClass(firestoreId, courseId, date, teacher, comments));
                }
            } catch (Exception e) {
                Log.e("ClassDAO", "Error retrieving classes by course ID from SQLite", e);
            } finally {
                cursor.close();
            }
        }
        return classes;
    }

    public void deleteAllClasses() {
        db.delete(TABLE_NAME, null, null);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("yoga_classes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        snapshot.getReference().delete()
                                .addOnSuccessListener(aVoid -> Log.d("FirestoreDelete", "Class deleted successfully from Firestore"))
                                .addOnFailureListener(e -> Log.e("FirestoreDelete", "Error deleting class from Firestore", e));
                    }
                });
    }
}
