package com.example.dailytracker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateUtils {
    public static String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime());
    }

    public static String formatTime(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.getTime());
    }

    public static String formatTime(int minutes) {
        int hour = minutes / 60;
        int min = minutes % 60;
        return formatTime(hour, min);
    }
}
