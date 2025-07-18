package com.example.dailytracker;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.OutputStream;

public class CSVExporter {
    public static void exportToCSV(Context context) {
        try {
            DBHelper dbHelper = new DBHelper(context);
            Cursor cursor = dbHelper.getAllEntries();

            String fileName = "tracker_data_" + System.currentTimeMillis() + ".csv";

            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, "text/csv");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.Downloads.IS_PENDING, 1);
            }

            Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
            Uri fileUri = resolver.insert(collection, contentValues);

            if (fileUri != null) {
                OutputStream out = resolver.openOutputStream(fileUri);

                if (out != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Date,Wake Time,HT,LT,IT\n");

                    while (cursor.moveToNext()) {
                        sb.append(cursor.getString(0)).append(",") // date
                                .append(DateUtils.formatTime(cursor.getInt(1))).append(",") // wake_time
                                .append(cursor.getInt(2)).append(",") // HT
                                .append(cursor.getInt(3)).append(",") // LT
                                .append(cursor.getInt(4)).append("\n"); // IT
                    }

                    out.write(sb.toString().getBytes());
                    out.flush();
                    out.close();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear();
                        contentValues.put(MediaStore.Downloads.IS_PENDING, 0);
                        resolver.update(fileUri, contentValues, null, null);
                    }

                    Toast.makeText(context, "CSV saved to Downloads\n" + fileName, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Failed to open output stream", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show();
            }

            cursor.close();
        } catch (Exception e) {
            Toast.makeText(context, "Export Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
