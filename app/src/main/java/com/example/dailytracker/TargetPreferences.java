package com.example.dailytracker;

import android.content.Context;
import android.content.SharedPreferences;

public class TargetPreferences {

    private static final String PREFS = "targets_prefs";
    private static final String KEY_WAKE = "target_wake";
    private static final String KEY_HT = "target_ht";
    private static final String KEY_LT = "target_lt";
    private static final String KEY_IT = "target_it";

    // Save targets
    public static void saveTargets(Context context, int wakeMinutes, int ht, int lt, int it) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_WAKE, wakeMinutes)
                .putInt(KEY_HT, ht)
                .putInt(KEY_LT, lt)
                .putInt(KEY_IT, it)
                .apply();
    }

    // Getters with defaults (recommended targets)
    public static int getWakeTime(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_WAKE, 7 * 60);
    }

    public static int getHT(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_HT, 60);
    }

    public static int getLT(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_LT, 120);
    }

    public static int getIT(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_IT, 180);
    }
}
