package com.example.dailytracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "tracker.db";
    private static final int DB_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS entries (" +
                "date TEXT PRIMARY KEY, " +
                "wake_time INTEGER, " +
                "ht INTEGER, " +
                "lt INTEGER, " +
                "it INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS entries");
        onCreate(db);
    }

    public boolean insertEntry(String date, int wake, int ht, int lt, int it) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("date", date);
        contentValues.put("wake_time", wake);
        contentValues.put("ht", ht);
        contentValues.put("lt", lt);
        contentValues.put("it", it);
        long result = db.insert("entries", null, contentValues);
        if (result == -1) {
            Log.e("DBHelper", "Failed to insert: " + date + ", wake=" + wake);
        }
        return result != -1;
    }

    public boolean entryExists(String date) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM entries WHERE date = ?", new String[]{date});
        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
        }
        cursor.close();
        return exists;
    }

    public Cursor getEntriesPaged(int limit, int offset) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT * FROM entries ORDER BY date DESC LIMIT ? OFFSET ?",
                new String[]{String.valueOf(limit), String.valueOf(offset)});
    }

    public int getEntryCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM entries", null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // Only updates the columns that are not null (user changed)
    public boolean updatePartialEntry(String date, Integer wake, Integer ht, Integer lt, Integer it) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        if (wake != null) values.put("wake_time", wake);
        if (ht != null) values.put("ht", ht);
        if (lt != null) values.put("lt", lt);
        if (it != null) values.put("it", it);

        if (values.size() == 0) return false; // Nothing to update

        int rows = db.update("entries", values, "date = ?", new String[]{date});
        return rows == 1;
    }


    public Cursor getAllEntries() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery("SELECT * FROM entries", null);
    }

    public Cursor getEntriesForMonth(String month) {
        // month format "yyyy-MM"
        SQLiteDatabase db = this.getReadableDatabase();
        String likePattern = month + "%";  // e.g. "2025-07%"
        return db.rawQuery("SELECT * FROM entries WHERE date LIKE ?", new String[]{likePattern});
    }

    public Cursor getEntryByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM entries WHERE date = ?";
        return db.rawQuery(query, new String[]{date});
    }
}