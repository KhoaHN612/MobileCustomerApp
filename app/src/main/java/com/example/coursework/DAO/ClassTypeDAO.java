package com.example.coursework.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.coursework.Model.ClassType;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClassTypeDAO {
    public static final String TABLE_NAME = "class_type";
    public static final String ID_COLUMN = "id";
    public static final String NAME_COLUMN = "name";

    public static final String CREATE_TABLE = String.format(
            "CREATE TABLE %s (" +
                    "%s TEXT PRIMARY KEY, " +
                    "%s TEXT UNIQUE NOT NULL)",
            TABLE_NAME, ID_COLUMN, NAME_COLUMN);

    private final SQLiteDatabase db;
    private final Context context;

    public ClassTypeDAO(SQLiteDatabase db, Context context) {
        this.db = db;
        this.context = context;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public List<ClassType> getAllClassTypes() {
        if (isNetworkAvailable()) {
            syncClassTypesWithFirestore();
        }

        List<ClassType> classTypes = new ArrayList<>();

        Cursor cursor = db.query(TABLE_NAME, new String[]{ID_COLUMN, NAME_COLUMN}, null, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(ID_COLUMN));
                String name = cursor.getString(cursor.getColumnIndex(NAME_COLUMN));
                classTypes.add(new ClassType(id, name));
            }
            cursor.close();
        }
        return classTypes;
    }

    public void initializeDefaultClassTypes() {
        Log.d("ClassTypeDAO", "Initializing default class types...");

        List<String> defaultClassTypes = Arrays.asList("Flow Yoga", "Aerial Yoga", "Family Yoga");

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("class_types")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("ClassTypeDAO", "Successfully fetched class types from Firestore.");

                        List<String> classTypes = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            String typeName = document.getString(NAME_COLUMN);
                            classTypes.add(typeName);

                            Log.d("ClassTypeDAO", "Checking if Firestore class type exists in SQLite: " + typeName);

                            if (!isClassTypeInSQLite(typeName)) {
                                insertClassTypeIntoSQLite(document.getId(), typeName);
                                Log.d("ClassTypeDAO", "Inserted Firestore class type into SQLite: " + typeName);
                            }
                        }

                        for (String typeName : defaultClassTypes) {
                            Log.d("ClassTypeDAO", "Checking if default class type exists: " + typeName);

                            if (!classTypes.contains(typeName) && !isClassTypeInSQLite(typeName)) {
                                Log.d("ClassTypeDAO", "Default class type not found, inserting: " + typeName);
                                insertOrUpdateClassType(new ClassType(null, typeName));
                            } else {
                                Log.d("ClassTypeDAO", "Default class type already exists: " + typeName);
                            }
                        }
                    } else {
                        Log.e("FirestoreSync", "Error fetching class types from Firestore", task.getException());
                    }
                });
    }

    public void insertOrUpdateClassType(ClassType classType) {
        Log.d("ClassTypeDAO", "Inserting or updating class type: " + classType.getName());

        if (isClassTypeInSQLite(classType.getName())) {
            Log.d("ClassTypeDAO", "Class type already exists in SQLite, skipping insert: " + classType.getName());
            return;
        } else {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();

            firestore.collection("class_types")
                    .whereEqualTo(NAME_COLUMN, classType.getName())
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            Log.d("ClassTypeDAO", "Class type not found in Firestore, adding: " + classType.getName());

                            if (classType.getId() == null || classType.getId().isEmpty()){
                                String id = UUID.randomUUID().toString();
                                classType.setId(id);
                            }

                            insertClassTypeIntoSQLite(classType.getId(), classType.getName());

                            Map<String, Object> classTypeData = createClassTypeDataMap(classType);
                            firestore.collection("class_types")
                                    .document(classType.getId())
                                    .set(classTypeData)
                                    .addOnSuccessListener(documentReference -> {
                                        Log.d("ClassTypeDAO", "Added class type to Firestore and SQLite: " + classType.getName());
                                    })
                                    .addOnFailureListener(e -> Log.e("FirestoreSync", "Error adding class type to Firestore", e));
                        } else {
                            String id = querySnapshot.getDocuments().get(0).getId();
                            insertClassTypeIntoSQLite(id, classType.getName());
                            Log.d("ClassTypeDAO", "Class type already exists in Firestore, synced to SQLite: " + classType.getName());
                        }
                    })
                    .addOnFailureListener(e -> Log.e("FirestoreSync", "Error checking class type in Firestore", e));
        }
    }

    public void syncClassTypesWithFirestore() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("class_types")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            String firestoreId = document.getString(ID_COLUMN);
                            String typeName = document.getString(NAME_COLUMN);

                            Log.d("ClassTypeDAO", "Syncing Firestore class type to SQLite: " + typeName);

                            if (!isClassTypeInSQLite(typeName)) {
                                insertClassTypeIntoSQLite(firestoreId, typeName);
                                Log.d("ClassTypeDAO", "Synced class type to SQLite: " + typeName);
                            }
                        }
                    } else {
                        Log.e("FirestoreSync", "Error syncing class types", task.getException());
                    }
                });
    }

    private void insertClassTypeIntoSQLite(String typeId, String typeName) {
        if (isClassTypeInSQLite(typeName)) {
            Log.d("ClassTypeDAO", "Class type already exists in SQLite, skipping insert into SQLite: " + typeName);
            return;
        }

        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, typeId);
        values.put(NAME_COLUMN, typeName);
        db.insert(TABLE_NAME, null, values);

        Log.d("ClassTypeDAO", "Inserted class type into SQLite: " + typeName);
    }

    private boolean isClassTypeInSQLite(String typeName) {
        Cursor cursor = db.query(TABLE_NAME, null, NAME_COLUMN + "=?", new String[]{typeName}, null, null, null);
        boolean exists = (cursor != null && cursor.moveToFirst());
        if (cursor != null) {
            cursor.close();
        }
        Log.d("ClassTypeDAO", "Class type exists in SQLite (" + typeName + "): " + exists);
        return exists;
    }

    public String getClassTypeNameById(String classTypeId) {
        String classTypeName = null;
        Cursor cursor = db.query(TABLE_NAME, new String[]{NAME_COLUMN}, ID_COLUMN + "=?", new String[]{classTypeId}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            classTypeName = cursor.getString(cursor.getColumnIndex(NAME_COLUMN));
            cursor.close();
        }
        return classTypeName;
    }

    private Map<String, Object> createClassTypeDataMap(ClassType classType) {
        Map<String, Object> classTypeData = new HashMap<>();
        classTypeData.put("id", classType.getId());
        classTypeData.put("name", classType.getName());
        return classTypeData;
    }
}
