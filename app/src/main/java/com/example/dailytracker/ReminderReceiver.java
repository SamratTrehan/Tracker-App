package com.example.dailytracker;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!new DBHelper(context).entryExists(DateUtils.getTodayDate())) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder")
                    //.setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Daily Input Reminder")
                    .setContentText("Don’t forget to enter your daily AT, HT, LT, and IT!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            Intent notifyIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.setContentIntent(pendingIntent);

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(1, builder.build());
        }
    }
}