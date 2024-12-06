package com.example.coursework.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.coursework.Model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class UserDAO {
    public static final String TABLE_NAME = "users";
    public static final String ID_COLUMN = "id";
    public static final String USERNAME_COLUMN = "user_name";
    public static final String EMAIL_COLUMN = "email";
    public static final String PASSWORD_COLUMN = "password";
    public static final String ROLE_COLUMN = "role";

    public static final String CREATE_TABLE = String.format(
            "CREATE TABLE %s (" +
                    "   %s TEXT PRIMARY KEY, " +
                    "   %s TEXT UNIQUE, " +
                    "   %s TEXT, " +
                    "   %s TEXT, " +
                    "   %s TEXT)",
            TABLE_NAME, ID_COLUMN, EMAIL_COLUMN, USERNAME_COLUMN, PASSWORD_COLUMN, ROLE_COLUMN );

    private SQLiteDatabase db;
    private Context context;

    public UserDAO(SQLiteDatabase db, Context context) {
        this.db = db;
        this.context = context;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public long insertUser(User user) {
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(UUID.randomUUID().toString());
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> userData = createUserDataMap(user);

        insertUserIntoSQLite(user);
        firestore.collection("users")
                .document(user.getId())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreSync", "User added/updated in Firestore successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreSync", "Error adding user to Firestore", e);
                });

        return -1;
    }

    private void insertUserIntoSQLite(User user) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, user.getId());
        values.put(EMAIL_COLUMN, user.getEmail());
        values.put(USERNAME_COLUMN, user.getUserName());
        values.put(PASSWORD_COLUMN, user.getPassword());
        values.put(ROLE_COLUMN, user.getRole());

        db.insert(TABLE_NAME, null, values);
    }

    private void updateUserInSQLite(User user) {
        ContentValues values = new ContentValues();
        values.put(USERNAME_COLUMN, user.getUserName());
        values.put(PASSWORD_COLUMN, user.getPassword());
        values.put(ROLE_COLUMN, user.getRole());

        db.update(TABLE_NAME, values, EMAIL_COLUMN + "=?", new String[]{user.getEmail()});
    }

    public void syncUsersFromFirestoreToSQLite() {
        if (!isNetworkAvailable()) return;

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot document : task.getResult()) {
                            String id = document.getString(ID_COLUMN);;
                            String email = document.getString(EMAIL_COLUMN);
                            String userName = document.getString(USERNAME_COLUMN);
                            String password = document.getString(PASSWORD_COLUMN);
                            String role = document.getString(ROLE_COLUMN);

                            User user = getUserByEmail(email);
                            if (user == null) {
                                insertUserIntoSQLite(new User(id, email, password, role, userName));
                            } else {
                                updateUserInSQLite(new User(id, email, password, role, userName));
                            }
                        }
                    } else {
                        Log.e("FirestoreSync", "Error getting users from Firestore", task.getException());
                    }
                });
    }

    private void updateUserIdInSQLite(String oldId, String newId) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, newId);
        db.update(TABLE_NAME, values, ID_COLUMN + "=?", new String[]{oldId});
    }

    public void updateUser(User user) {
        ContentValues values = new ContentValues();
        values.put(USERNAME_COLUMN, user.getUserName());
        values.put(PASSWORD_COLUMN, user.getPassword());
        values.put(ROLE_COLUMN, user.getRole());
        db.update(TABLE_NAME, values, EMAIL_COLUMN + "=?", new String[]{user.getEmail()});

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> userData = createUserDataMap(user);
        firestore.collection("users")
                .document(user.getId())
                .update(userData)
                .addOnSuccessListener(aVoid -> Log.d("FirestoreUpdate", "User updated in Firestore successfully"))
                .addOnFailureListener(e -> Log.e("FirestoreUpdate", "Error updating user in Firestore", e));
    }

    public List<String> getAllUserNames() {
        List<String> userNames = new ArrayList<>();
        Cursor cursor = db.query(TABLE_NAME, new String[]{USERNAME_COLUMN}, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                userNames.add(cursor.getString(cursor.getColumnIndex(USERNAME_COLUMN)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return userNames;
    }

    public List<String> getAllAdminUserNames() {
        List<String> userNames = new ArrayList<>();
        String selection = ROLE_COLUMN + "=?";
        String[] selectionArgs = new String[]{"Admin"};

        Cursor cursor = db.query(TABLE_NAME, new String[]{USERNAME_COLUMN}, selection, selectionArgs, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                userNames.add(cursor.getString(cursor.getColumnIndexOrThrow(USERNAME_COLUMN)));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return userNames;
    }

    public User getUserById(String userId) {
        Cursor cursor = db.query(TABLE_NAME, null, ID_COLUMN + "=?", new String[]{userId}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndex(ID_COLUMN));
            String emailDb = cursor.getString(cursor.getColumnIndex(EMAIL_COLUMN));
            String userName = cursor.getString(cursor.getColumnIndex(USERNAME_COLUMN));
            String password = cursor.getString(cursor.getColumnIndex(PASSWORD_COLUMN));
            String role = cursor.getString(cursor.getColumnIndex(ROLE_COLUMN));
            cursor.close();
            return new User(id, emailDb, password, role, userName);
        }
        return null;
    }

    public User getUserByEmail(String email) {
        Cursor cursor = db.query(TABLE_NAME, null, EMAIL_COLUMN + "=?", new String[]{email}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndex(ID_COLUMN));
            String emailDb = cursor.getString(cursor.getColumnIndex(EMAIL_COLUMN));
            String userName = cursor.getString(cursor.getColumnIndex(USERNAME_COLUMN));
            String password = cursor.getString(cursor.getColumnIndex(PASSWORD_COLUMN));
            String role = cursor.getString(cursor.getColumnIndex(ROLE_COLUMN));
            cursor.close();
            return new User(id, emailDb, password, role, userName);
        }
        return null;
    }

    public boolean authenticateUser(String email, String passwordInput) {
        User user = getUserByEmail(email);

        if (user != null && Objects.equals(user.getRole(), "Admin")) {
            String hashedInputPassword = hashPassword(passwordInput);
            return user.getPassword().equals(hashedInputPassword);
        }
        return false;
    }

    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isUserNameExists(String username) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{USERNAME_COLUMN}, USERNAME_COLUMN + "=?", new String[]{username}, null, null, null);
        boolean exists = cursor != null && cursor.moveToFirst();
        if (cursor != null) {
            cursor.close();
        }
        return exists;
    }

    public boolean isEmailExists(String email) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{EMAIL_COLUMN}, EMAIL_COLUMN + "=?", new String[]{email}, null, null, null);
        boolean exists = cursor != null && cursor.moveToFirst();
        if (cursor != null) {
            cursor.close();
        }
        return exists;
    }

    public List<User> getUsersByRole(String role) {
        syncUsersFromFirestoreToSQLite();
        List<User> users = new ArrayList<>();
        Cursor cursor = db.query(TABLE_NAME, null, ROLE_COLUMN + "=?", new String[]{role}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndex(ID_COLUMN));
                String email = cursor.getString(cursor.getColumnIndex(EMAIL_COLUMN));
                String userName = cursor.getString(cursor.getColumnIndex(USERNAME_COLUMN));
                String password = cursor.getString(cursor.getColumnIndex(PASSWORD_COLUMN));
                String userRole = cursor.getString(cursor.getColumnIndex(ROLE_COLUMN));
                users.add(new User(id, email, password, userRole, userName));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return users;
    }

    public void updateUserRole(String userId, String newRole) {
        ContentValues values = new ContentValues();
        values.put(ROLE_COLUMN, newRole);
        db.update(TABLE_NAME, values, ID_COLUMN + "=?", new String[]{userId});

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users")
                .document(userId)
                .update(ROLE_COLUMN, newRole)
                .addOnSuccessListener(aVoid -> Log.d("FirestoreUpdate", "User role updated in Firestore successfully"))
                .addOnFailureListener(e -> Log.e("FirestoreUpdate", "Error updating user role in Firestore", e));
    }

    public void deleteUser(String userId) {
        db.delete(TABLE_NAME, ID_COLUMN + "=?", new String[]{userId});

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("FirestoreDelete", "User deleted from Firestore successfully"))
                .addOnFailureListener(e -> Log.e("FirestoreDelete", "Error deleting user from Firestore", e));
    }

    private Map<String, Object> createUserDataMap(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put(ID_COLUMN, user.getId());
        userData.put(USERNAME_COLUMN, user.getUserName());
        userData.put(EMAIL_COLUMN, user.getEmail());
        userData.put(PASSWORD_COLUMN, user.getPassword());
        userData.put(ROLE_COLUMN, user.getRole());
        return userData;
    }

    public void isUserNameExistOnFirestore(String userName, FirestoreUserCallback callback) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection(TABLE_NAME)
                .whereEqualTo(USERNAME_COLUMN, userName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        callback.onCallback(true);
                    } else {
                        callback.onCallback(false);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e));
    }

    public interface FirestoreUserCallback {
        void onCallback(boolean exists);
        void onError(Exception e);
    }
}
