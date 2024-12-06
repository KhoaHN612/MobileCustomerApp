package com.example.coursework.DAO;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class CartDAO
{
    private SQLiteDatabase db;
    private final Context context;

    public CartDAO(SQLiteDatabase db, Context context) {
        this.db = db;
        this.context = context;
    }

    public void deleteAllCartItems() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("carts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        snapshot.getReference().update("items", new ArrayList<>())
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FirestoreCartUpdate", "All items removed from cart successfully");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FirestoreCartUpdate", "Error removing items from cart", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreCartFetch", "Error retrieving carts for item deletion", e);
                });
    }

}
