package com.example.coursework.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import com.example.coursework.Model.YogaCourse;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CourseDAO {
    public static final String TABLE_NAME = "yoga_courses";
    public static final String ID_COLUMN = "id";
    public static final String DAY_OF_WEEK_COLUMN = "day_of_week";
    public static final String START_DAY_COLUMN = "start_day";
    public static final String TIME_COLUMN = "time";
    public static final String CAPACITY_COLUMN = "capacity";
    public static final String DURATION_COLUMN = "duration";
    public static final String PRICE_COLUMN = "price";
    public static final String CLASS_TYPE_COLUMN = "class_type_id";
    public static final String DESCRIPTION_COLUMN = "description";
    public static final String IMAGE_URL_COLUMN = "image_url";
    public static final String LOCAL_IMAGE_URI_COLUMN = "local_image_uri";

    public static final String CREATE_TABLE = String.format(
            "CREATE TABLE %s (" +
                    " %s TEXT PRIMARY KEY, " +
                    " %s TEXT, " +
                    " %s TEXT, " +
                    " %s TEXT, " +
                    " %s INTEGER, " +
                    " %s INTEGER, " +
                    " %s REAL, " +
                    " %s TEXT, " +
                    " %s TEXT, " +
                    " %s TEXT, " +
                    " %s TEXT, " +
                    "FOREIGN KEY(%s) REFERENCES %s(%s))",
            TABLE_NAME, ID_COLUMN, DAY_OF_WEEK_COLUMN, START_DAY_COLUMN, TIME_COLUMN,
            CAPACITY_COLUMN, DURATION_COLUMN, PRICE_COLUMN, CLASS_TYPE_COLUMN, DESCRIPTION_COLUMN, IMAGE_URL_COLUMN, LOCAL_IMAGE_URI_COLUMN,
            CLASS_TYPE_COLUMN, ClassTypeDAO.TABLE_NAME, ClassTypeDAO.ID_COLUMN);



    private SQLiteDatabase db;
    private final Context context;
    private ClassDAO classDAO;

    public CourseDAO(SQLiteDatabase db, Context context) {
        this.db = db;
        this.context = context;
        this.classDAO = new ClassDAO(db, context);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void insertOrUpdateCourse(YogaCourse course) {
        boolean isNetworkAvailable = isNetworkAvailable();

        if (course.getLocalImageUri() != null && course.getLocalImageUri().startsWith("content://")) {
            String localImagePath = saveImageToAppStorage(Uri.parse(course.getLocalImageUri()));
            course.setLocalImageUri(localImagePath);
            Log.e("Image","Save as " + localImagePath);
        }

        if (course.getId() == null || course.getId().isEmpty()) {
            String generatedId = UUID.randomUUID().toString();
            course.setId(generatedId);
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> courseData = createCourseDataMap(course);

        firestore.collection("yoga_courses")
                .document(course.getId())
                .set(courseData)
                .addOnSuccessListener(aVoid -> {
                    if (isNetworkAvailable()){
                        uploadImageToFirebaseStorage(course);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreSync", "Error syncing course with Firestore", e);
                });
        insertCourseIntoSQLite(course);

    }

    private void uploadImageToFirebaseStorage(YogaCourse course) {
        if (course.getLocalImageUri() != null) {
            Log.e("Image", course.getLocalImageUri());
            File file = new File(course.getLocalImageUri());
            Log.e("Image", String.valueOf(file.exists()));
            if (file.exists()) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageReference = storage.getReference().child("yoga_images/" + System.currentTimeMillis() + ".jpg");

                Uri fileUri = Uri.fromFile(file);
                Log.e("Image", "Co file");
                storageReference.putFile(fileUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            storageReference.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                Log.e("Image", downloadUri.toString());
                                course.setImageUrl(downloadUri.toString());
                                updateImageUrlInSQLite(course.getId(), downloadUri.toString());
                                syncToFirestore(course);
                            });
                        })
                        .addOnFailureListener(e -> Log.e("FirebaseStorage", "Error uploading image to Firebase", e));
            }
        }
    }

    private void updateImageUrlInSQLite(String courseId, String imageUrl) {
        ContentValues values = new ContentValues();
        values.put(IMAGE_URL_COLUMN, imageUrl);
        db.update(TABLE_NAME, values, ID_COLUMN + "=?", new String[]{courseId});
    }

    public String saveImageToAppStorage(Uri imageUri) {
        File directory = new File(context.getExternalFilesDir(null), "yoga_images");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(directory, fileName);

        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
             OutputStream outputStream = new FileOutputStream(file)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            Log.e("ImageSave", "Image save success");
        } catch (IOException e) {
            Log.e("ImageSave", "Error saving image to app storage", e);
            return null;
        }

        return file.getAbsolutePath();
    }

    private void insertCourseIntoSQLite(YogaCourse course) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, course.getId());
        values.put(DAY_OF_WEEK_COLUMN, course.getDayOfWeek());
        values.put(START_DAY_COLUMN, course.getStartDay());
        values.put(TIME_COLUMN, course.getTime());
        values.put(CAPACITY_COLUMN, course.getCapacity());
        values.put(DURATION_COLUMN, course.getDuration());
        values.put(PRICE_COLUMN, course.getPrice());
        values.put(CLASS_TYPE_COLUMN, course.getClassTypeId());
        values.put(DESCRIPTION_COLUMN, course.getDescription());
        values.put(IMAGE_URL_COLUMN, course.getImageUrl());
        values.put(LOCAL_IMAGE_URI_COLUMN, course.getLocalImageUri());

        Cursor cursor = db.query(TABLE_NAME, null, ID_COLUMN + "=?", new String[]{course.getId()}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            db.update(TABLE_NAME, values, ID_COLUMN + "=?", new String[]{course.getId()});
        } else {
            db.insert(TABLE_NAME, null, values);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    public interface CompletionListener {
        void onComplete(boolean success);
    }

    public void syncCoursesFromFirestoreToSQLite(CompletionListener listener) {
        if (!isNetworkAvailable()) {
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("yoga_courses")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> documents = task.getResult().getDocuments();
                        List<String> firestoreCourseIds = new ArrayList<>();

                        for (DocumentSnapshot document : documents) {
                            firestoreCourseIds.add(document.getId());

                            String id = document.getString("id");
                            String dayOfWeek = document.getString("day_of_week");
                            String startDay = document.getString("start_day");
                            String time = document.getString("time");
                            long capacity = document.getLong("capacity");
                            long duration = document.getLong("duration");
                            double price = document.getDouble("price");
                            String classTypeId = document.getString("class_type_id");
                            String description = document.getString("description");
                            String imageUrl = document.getString("image_url");

                            YogaCourse course = new YogaCourse(
                                    id, dayOfWeek, startDay, time, (int) capacity,
                                    (int) duration, price, classTypeId, description, imageUrl, null
                            );

                            YogaCourse existingCourse = getCourseById(id);

                            boolean isImageUrlDifferent = existingCourse != null && imageUrl != null && !imageUrl.equals(existingCourse.getImageUrl());
                            boolean needsLocalImageUpdate = existingCourse == null || existingCourse.getLocalImageUri() == null || (imageUrl != null && isImageUrlDifferent);

                            if (existingCourse != null && existingCourse.getLocalImageUri() != null && imageUrl == null) {
                                File oldImageFile = new File(existingCourse.getLocalImageUri());
                                if (oldImageFile.exists()) {
                                    oldImageFile.delete();
                                }
                                course.setLocalImageUri(null);
                            } else if (needsLocalImageUpdate) {
                                if (existingCourse != null && existingCourse.getLocalImageUri() != null) {
                                    File oldImageFile = new File(existingCourse.getLocalImageUri());
                                    if (oldImageFile.exists()) {
                                        oldImageFile.delete();
                                    }
                                }
                                if (imageUrl != null) {
                                    downloadImageAndUpdateCourse(course);
                                }
                            } else {
                                course.setLocalImageUri(existingCourse.getLocalImageUri());
                            }

                            insertOrUpdateCourse(course);
                        }

                        removeDeletedCoursesFromSQLite(firestoreCourseIds);

                        if (listener != null) listener.onComplete(true);
                    } else {
                        Log.e("FirestoreSync", "Error getting courses from Firestore", task.getException());
                        if (listener != null) listener.onComplete(false);
                    }
                });
    }


    private void updateCourseWithFirestoreId(String oldId, String newId) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, newId);
        db.update(TABLE_NAME, values, ID_COLUMN + "=?", new String[]{oldId});
    }

    private List<YogaCourse> getTemporaryCourses() {
        List<YogaCourse> temporaryCourses = new ArrayList<>();
        Cursor cursor = db.query(TABLE_NAME, null, ID_COLUMN + " LIKE ?", new String[]{"%-%-%-%-%"}, null, null, null);

        if (cursor != null) {
            try {
                int firestoreIdIndex = cursor.getColumnIndexOrThrow(ID_COLUMN);
                int dayOfWeekIndex = cursor.getColumnIndexOrThrow(DAY_OF_WEEK_COLUMN);
                int startDayIndex = cursor.getColumnIndexOrThrow(START_DAY_COLUMN);
                int timeIndex = cursor.getColumnIndexOrThrow(TIME_COLUMN);
                int capacityIndex = cursor.getColumnIndexOrThrow(CAPACITY_COLUMN);
                int durationIndex = cursor.getColumnIndexOrThrow(DURATION_COLUMN);
                int priceIndex = cursor.getColumnIndexOrThrow(PRICE_COLUMN);
                int classTypeIdIndex = cursor.getColumnIndexOrThrow(CLASS_TYPE_COLUMN);
                int descriptionIndex = cursor.getColumnIndexOrThrow(DESCRIPTION_COLUMN);
                int imageUrlIndex = cursor.getColumnIndexOrThrow(IMAGE_URL_COLUMN);
                int localImageUriIndex = cursor.getColumnIndexOrThrow(LOCAL_IMAGE_URI_COLUMN);

                while (cursor.moveToNext()) {
                    String firestoreId = cursor.getString(firestoreIdIndex);
                    String dayOfWeek = cursor.getString(dayOfWeekIndex);
                    String startDay = cursor.getString(startDayIndex);
                    String time = cursor.getString(timeIndex);
                    int capacity = cursor.getInt(capacityIndex);
                    int duration = cursor.getInt(durationIndex);
                    double price = cursor.getDouble(priceIndex);
                    String classTypeId = cursor.getString(classTypeIdIndex);
                    String description = cursor.getString(descriptionIndex);
                    String imageUrl = cursor.getString(imageUrlIndex);
                    String localImageUri = cursor.getString(localImageUriIndex);

                    YogaCourse course = new YogaCourse(firestoreId, dayOfWeek, startDay, time, capacity, duration, price, classTypeId, description, imageUrl, localImageUri);
                    temporaryCourses.add(course);
                }
            } catch (Exception e) {
                Log.e("CourseDAO", "Error retrieving temporary courses from SQLite", e);
            } finally {
                cursor.close();
            }
        }
        return temporaryCourses;
    }


    private void downloadImageAndUpdateCourse(YogaCourse course) {
        if (course.getImageUrl() != null) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReferenceFromUrl(course.getImageUrl());

            storageReference.getBytes(1024 * 1024)
                    .addOnSuccessListener(bytes -> {
                        String localPath = saveImageToAppStorage(bytes);
                        course.setLocalImageUri(localPath);
                        insertCourseIntoSQLite(course);
                    })
                    .addOnFailureListener(e -> Log.e("Storage", "Failed to download image", e));
        }
    }

    private String saveImageToAppStorage(byte[] bytes) {
        File directory = new File(context.getExternalFilesDir(null), "yoga_images");

        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(directory, fileName);

        try {
            OutputStream outputStream = new FileOutputStream(file);
            outputStream.write(bytes);
            outputStream.close();
        } catch (IOException e) {
            Log.e("ImageSave", "Failed to save image", e);
            return null;
        }

        return file.getAbsolutePath();
    }

    public YogaCourse getCourseById(String courseId) {
        YogaCourse course = null;

        Cursor cursor = db.query(TABLE_NAME, null, ID_COLUMN + "=?", new String[]{courseId}, null, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(ID_COLUMN));
                    String dayOfWeek = cursor.getString(cursor.getColumnIndexOrThrow(DAY_OF_WEEK_COLUMN));
                    String startDay = cursor.getString(cursor.getColumnIndexOrThrow(START_DAY_COLUMN));
                    String time = cursor.getString(cursor.getColumnIndexOrThrow(TIME_COLUMN));
                    int capacity = cursor.getInt(cursor.getColumnIndexOrThrow(CAPACITY_COLUMN));
                    int duration = cursor.getInt(cursor.getColumnIndexOrThrow(DURATION_COLUMN));
                    double price = cursor.getDouble(cursor.getColumnIndexOrThrow(PRICE_COLUMN));
                    String classTypeId = cursor.getString(cursor.getColumnIndexOrThrow(CLASS_TYPE_COLUMN));
                    String description = cursor.getString(cursor.getColumnIndexOrThrow(DESCRIPTION_COLUMN));
                    String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_URL_COLUMN));
                    String localImageUri = cursor.getString(cursor.getColumnIndexOrThrow(LOCAL_IMAGE_URI_COLUMN));

                    // Tạo đối tượng YogaCourse từ dữ liệu
                    course = new YogaCourse(id, dayOfWeek, startDay, time, capacity, duration, price, classTypeId, description, imageUrl, localImageUri);
                }
            } catch (Exception e) {
                Log.e("CourseDAO", "Error retrieving course by ID from SQLite", e);
            } finally {
                cursor.close();
            }
        }

        return course;
    }


    public ArrayList<YogaCourse> getAllCourses() {
        if (isNetworkAvailable()) {
            syncCoursesFromFirestoreToSQLite(success -> {});
        }

        ArrayList<YogaCourse> courses = new ArrayList<>();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, START_DAY_COLUMN);

        if (cursor != null) {
            try {
                int firestoreIdIndex = cursor.getColumnIndexOrThrow(ID_COLUMN);
                int dayOfWeekIndex = cursor.getColumnIndexOrThrow(DAY_OF_WEEK_COLUMN);
                int startDayIndex = cursor.getColumnIndexOrThrow(START_DAY_COLUMN);
                int timeIndex = cursor.getColumnIndexOrThrow(TIME_COLUMN);
                int capacityIndex = cursor.getColumnIndexOrThrow(CAPACITY_COLUMN);
                int durationIndex = cursor.getColumnIndexOrThrow(DURATION_COLUMN);
                int priceIndex = cursor.getColumnIndexOrThrow(PRICE_COLUMN);
                int classTypeIdIndex = cursor.getColumnIndexOrThrow(CLASS_TYPE_COLUMN);
                int descriptionIndex = cursor.getColumnIndexOrThrow(DESCRIPTION_COLUMN);
                int imageUrlIndex = cursor.getColumnIndexOrThrow(IMAGE_URL_COLUMN);
                int localImageUriIndex = cursor.getColumnIndexOrThrow(LOCAL_IMAGE_URI_COLUMN);

                if (cursor.moveToFirst()) {
                    do {
                        String firestoreId = cursor.getString(firestoreIdIndex);
                        String dayOfWeek = cursor.getString(dayOfWeekIndex);
                        String startDay = cursor.getString(startDayIndex);
                        String time = cursor.getString(timeIndex);
                        int capacity = cursor.getInt(capacityIndex);
                        int duration = cursor.getInt(durationIndex);
                        double price = cursor.getDouble(priceIndex);
                        String classTypeId = cursor.getString(classTypeIdIndex);
                        String description = cursor.getString(descriptionIndex);
                        String imageUrl = cursor.getString(imageUrlIndex);
                        String localImageUri = cursor.getString(localImageUriIndex);

                        courses.add(new YogaCourse(firestoreId, dayOfWeek, startDay, time, capacity, duration, price, classTypeId, description, imageUrl, localImageUri));
                    } while (cursor.moveToNext());
                }
            } catch (IllegalArgumentException e) {
                Log.e("CourseDAO", "Column missing in database: " + e.getMessage());
            } finally {
                cursor.close();
            }
        }

        return courses;
    }

    public void syncToFirestore(YogaCourse course) {

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Map<String, Object> courseData = createCourseDataMap(course);

        firestore.collection("yoga_courses")
                .document(course.getId())
                .set(courseData)
                .addOnSuccessListener(aVoid -> Log.d("FirestoreSync", "Course synced successfully"))
                .addOnFailureListener(e -> Log.e("FirestoreSync", "Error syncing course", e));
    }

    public void removeDeletedCoursesFromSQLite(List<String> firestoreCourseIds) {
        Cursor cursor = db.query(TABLE_NAME, new String[]{ID_COLUMN}, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String localCourseId = cursor.getString(cursor.getColumnIndexOrThrow(ID_COLUMN));
                if (!firestoreCourseIds.contains(localCourseId)) {
                    deleteCourseFromSQLite(localCourseId);
                }
            }
            cursor.close();
        }
    }

    public void deleteCourseFromSQLite(String courseId) {
        classDAO.deleteClassesByCourseId(courseId);

        Cursor cursor = db.query(TABLE_NAME, new String[]{LOCAL_IMAGE_URI_COLUMN}, ID_COLUMN + " = ?", new String[]{courseId}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String localImageUri = cursor.getString(cursor.getColumnIndexOrThrow(LOCAL_IMAGE_URI_COLUMN));
            if (localImageUri != null) {
                File imageFile = new File(localImageUri);
                if (imageFile.exists()) {
                    imageFile.delete();
                }
            }
            cursor.close();
        }

        db.delete(TABLE_NAME, ID_COLUMN + " = ?", new String[]{courseId});
    }

    public void deleteCourse(String courseId) {
        if (isNetworkAvailable()){
            Cursor cursor = db.query(TABLE_NAME, new String[]{IMAGE_URL_COLUMN}, ID_COLUMN + " = ?", new String[]{courseId}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_URL_COLUMN));
                cursor.close();

                if (imageUrl != null) {
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference photoRef = storage.getReferenceFromUrl(imageUrl);
                    photoRef.delete().addOnSuccessListener(aVoid -> Log.d("FirebaseStorage", "Image deleted successfully"))
                            .addOnFailureListener(e -> Log.e("FirebaseStorage", "Failed to delete image", e));
                }
            }
        }

        classDAO.deleteClassesByCourseId(courseId);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("yoga_courses")
                .document(courseId)
                .delete()
                .addOnCompleteListener(aVoid -> {
                    Log.d("FirestoreDelete", "Course deleted from Firestore successfully");

                })
                .addOnFailureListener(e -> Log.e("FirestoreDelete", "Error deleting course from Firestore", e));
        deleteCourseFromSQLite(courseId);
    }

    public void deleteAllCourses() {
        Cursor cursor = db.query(TABLE_NAME, new String[]{ID_COLUMN, LOCAL_IMAGE_URI_COLUMN, IMAGE_URL_COLUMN}, null, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String courseId = cursor.getString(cursor.getColumnIndexOrThrow(ID_COLUMN));
                String localImageUri = cursor.getString(cursor.getColumnIndexOrThrow(LOCAL_IMAGE_URI_COLUMN));
                String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(IMAGE_URL_COLUMN));

                if (localImageUri != null) {
                    File imageFile = new File(localImageUri);
                    if (imageFile.exists()) {
                        if (imageFile.delete()) {
                            Log.d("LocalImageDelete", "Local image deleted successfully");
                        } else {
                            Log.e("LocalImageDelete", "Error deleting local image");
                        }
                    }
                }

                if (imageUrl != null) {
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference photoRef = storage.getReferenceFromUrl(imageUrl);
                    photoRef.delete()
                            .addOnSuccessListener(aVoid -> Log.d("FirebaseStorageDelete", "Image deleted successfully from Firebase Storage"))
                            .addOnFailureListener(e -> Log.e("FirebaseStorageDelete", "Failed to delete image from Firebase Storage", e));
                }

                deleteCourseFromSQLite(courseId);
            }
            cursor.close();
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("yoga_courses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        snapshot.getReference().delete()
                                .addOnSuccessListener(aVoid -> Log.d("FirestoreDelete", "Course deleted successfully from Firestore"))
                                .addOnFailureListener(e -> Log.e("FirestoreDelete", "Error deleting course from Firestore", e));
                    }
                });
    }


    public static String calculateTime(String startTime, int duration) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(timeFormat.parse(startTime));
            calendar.add(Calendar.MINUTE, duration);
            return timeFormat.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return "Invalid Time";
        }
    }


    private Map<String, Object> createCourseDataMap(YogaCourse course) {
        Map<String, Object> courseData = new HashMap<>();
        courseData.put(ID_COLUMN, course.getId());
        courseData.put(DAY_OF_WEEK_COLUMN, course.getDayOfWeek());
        courseData.put(START_DAY_COLUMN, course.getStartDay());
        courseData.put(TIME_COLUMN, course.getTime());
        courseData.put(CAPACITY_COLUMN, course.getCapacity());
        courseData.put(DURATION_COLUMN, course.getDuration());
        courseData.put(PRICE_COLUMN, course.getPrice());
        courseData.put(CLASS_TYPE_COLUMN, course.getClassTypeId());
        courseData.put(DESCRIPTION_COLUMN, course.getDescription());
        courseData.put(IMAGE_URL_COLUMN, course.getImageUrl());
        courseData.put(LOCAL_IMAGE_URI_COLUMN, course.getLocalImageUri());
        return courseData;
    }
}
