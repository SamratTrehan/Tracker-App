package com.example.dailytracker;

import android.content.Context;
import android.content.SharedPreferences;

public class TargetPreferences {

    private static final String PREFS = "targets_prefs";
    private static final String KEY_WAKE = "target_wake";
    private static final String KEY_HT = "target_ht";
    private static final String KEY_LT = "target_lt";
    private static final String KEY_IT = "target_it";
    private static final String KEY_FT = "target_ft"; // FT key

    // Updated: Save targets including FT
    public static void saveTargets(Context context, int wakeMinutes, int ht, int lt, int it, int ft) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_WAKE, wakeMinutes)
                .putInt(KEY_HT, ht)
                .putInt(KEY_LT, lt)
                .putInt(KEY_IT, it)
                .putInt(KEY_FT, ft)
                .apply();
    }

    // Overload for backward compatibility with old callers:
    public static void saveTargets(Context context, int wakeMinutes, int ht, int lt, int it) {
        saveTargets(context, wakeMinutes, ht, lt, it, 60); // Default FT = 60
    }

    // Getters with sensible defaults
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

    // New: Family Time Target Getter
    public static int getFT(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_FT, 60); // default 60
    }
}
