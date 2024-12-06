package com.example.coursework.DAO;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "yoga_app.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ClassTypeDAO.CREATE_TABLE);
        db.execSQL(CourseDAO.CREATE_TABLE);
        db.execSQL(UserDAO.CREATE_TABLE);
        db.execSQL(ClassDAO.CREATE_TABLE);
        db.execSQL(OrderDAO.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ClassTypeDAO.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + CourseDAO.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + UserDAO.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ClassDAO.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + OrderDAO.TABLE_NAME);
        onCreate(db);
    }
}
