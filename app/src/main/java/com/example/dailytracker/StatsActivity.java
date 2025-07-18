package com.example.dailytracker;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Calendar;
import java.util.Locale;

public class StatsActivity extends AppCompatActivity {

    Button btnToday, btnDay, btnMonth;
    TextView selectedPeriodText;
    TableLayout statsTable;
    DBHelper dbHelper;

    String selectedDate;
    String selectedMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        btnToday = findViewById(R.id.btnToday);
        btnDay = findViewById(R.id.btnDay);
        btnMonth = findViewById(R.id.btnMonth);
        selectedPeriodText = findViewById(R.id.selectedPeriodText);
        statsTable = findViewById(R.id.statsTable);
        dbHelper = new DBHelper(this);

        selectedDate = DateUtils.getTodayDate();
        selectedMonth = selectedDate.substring(0, 7);

        btnToday.setOnClickListener(v -> {
            selectedPeriodText.setText("Showing: Today");
            selectedDate = DateUtils.getTodayDate();
            displayStatsForDay(selectedDate);
        });

        btnDay.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        selectedPeriodText.setText("Showing: " + selectedDate);
                        displayStatsForDay(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH))
                    .show();
        });

        btnMonth.setOnClickListener(v -> showMonthYearPicker());

        displayStatsForDay(selectedDate);
    }

    private void displayStatsForDay(String date) {
        statsTable.removeAllViews();

        Cursor cursor = dbHelper.getEntryByDate(date);

        int wake = -1, ht = -1, lt = -1, it = -1;
        if (cursor != null && cursor.moveToFirst()) {
            wake = cursor.getInt(1);
            ht = cursor.getInt(2);
            lt = cursor.getInt(3);
            it = cursor.getInt(4);
        }
        if (cursor != null) cursor.close();

        int targetWake = TargetPreferences.getWakeTime(this);
        int targetHT = TargetPreferences.getHT(this);
        int targetLT = TargetPreferences.getLT(this);
        int targetIT = TargetPreferences.getIT(this);

        addHeaderRow();

        if (wake == -1) {
            TableRow row = new TableRow(this);
            TextView noData = new TextView(this);
            noData.setText("No data for selected day.");
            row.addView(noData);
            statsTable.addView(row);
        } else {
            appendRow("WT",
                    DateUtils.formatTime(wake),
                    DateUtils.formatTime(targetWake),
                    "-",
                    calculateDeviation(wake, targetWake));

            appendRow("HT",
                    ht + " mins",
                    targetHT + " mins",
                    "-",
                    calculateDeviation(ht, targetHT));

            appendRow("LT",
                    lt + " mins",
                    targetLT + " mins",
                    "-",
                    calculateDeviation(lt, targetLT));

            appendRow("IT",
                    it + " mins",
                    targetIT + " mins",
                    "-",
                    calculateDeviation(it, targetIT));
        }
    }

    private void displayStatsForMonth(String month) {
        statsTable.removeAllViews();

        Cursor cursor = dbHelper.getEntriesForMonth(month);

        int count = 0;
        int totalWake = 0, totalHT = 0, totalLT = 0, totalIT = 0;

        while (cursor.moveToNext()) {
            totalWake += cursor.getInt(1);
            totalHT += cursor.getInt(2);
            totalLT += cursor.getInt(3);
            totalIT += cursor.getInt(4);
            count++;
        }
        cursor.close();

        addHeaderRow();

        if (count == 0) {
            TableRow row = new TableRow(this);
            TextView noData = new TextView(this);
            noData.setText("No data for selected month.");
            row.addView(noData);
            statsTable.addView(row);
        } else {
            int avgWake = totalWake / count;
            int avgHT = totalHT / count;
            int avgLT = totalLT / count;
            int avgIT = totalIT / count;

            int targetWake = TargetPreferences.getWakeTime(this);
            int targetHT = TargetPreferences.getHT(this);
            int targetLT = TargetPreferences.getLT(this);
            int targetIT = TargetPreferences.getIT(this);

            appendRow("WT",
                    "--",
                    DateUtils.formatTime(targetWake),
                    DateUtils.formatTime(avgWake),
                    calculateDeviation(avgWake, targetWake));

            appendRow("HT",
                    "--",
                    targetHT + " mins",
                    avgHT + " mins",
                    calculateDeviation(avgHT, targetHT));

            appendRow("LT",
                    "--",
                    targetLT + " mins",
                    avgLT + " mins",
                    calculateDeviation(avgLT, targetLT));

            appendRow("IT",
                    "--",
                    targetIT + " mins",
                    avgIT + " mins",
                    calculateDeviation(avgIT, targetIT));
        }
    }

    private void showMonthYearPicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedMonth = String.format(Locale.getDefault(), "%04d-%02d", year, month + 1);
                    selectedPeriodText.setText("Showing: " + selectedMonth);
                    displayStatsForMonth(selectedMonth);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        try {
            java.lang.reflect.Field[] datePickerFields = datePickerDialog.getDatePicker().getClass().getDeclaredFields();
            for (java.lang.reflect.Field field : datePickerFields) {
                if ("mDaySpinner".equals(field.getName()) || "mDayPicker".equals(field.getName())) {
                    field.setAccessible(true);
                    Object dayPicker = field.get(datePickerDialog.getDatePicker());
                    ((View) dayPicker).setVisibility(View.GONE);
                }
            }
        } catch (Exception ignored) {
        }

        datePickerDialog.show();
    }

    private void addHeaderRow() {
        TableRow headerRow = new TableRow(this);
        int headerColor = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant);
        headerRow.setBackgroundColor(headerColor);

        headerRow.addView(buildBoldCell("Cat."));
        headerRow.addView(buildBoldCell("Min"));
        headerRow.addView(buildBoldCell("Target"));
        headerRow.addView(buildBoldCell("Avg."));
        headerRow.addView(buildBoldCell("Dev."));

        statsTable.addView(headerRow);
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void appendRow(String cat, String min, String target, String avg, float deviation) {
        TableRow row = new TableRow(this);

        row.addView(buildCell(cat));
        row.addView(buildCell(min));
        row.addView(buildCell(target));

        TextView avgView = buildCell(avg);
        row.addView(avgView);  // No color logic anymore

        TextView devView = buildCell(String.format(Locale.getDefault(), "%.2f%%", deviation));
        row.addView(devView);

        statsTable.addView(row);
    }


    private TextView buildCell(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setPadding(8, 12, 8, 12);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private TextView buildBoldCell(String text) {
        TextView tv = buildCell(text);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private float calculateDeviation(int value, int target) {
        if (target == 0) return 0f;
        float result = ((float) (value - target) / target) * 100f;
        return result;
    }

    private int parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.equals("--")) return Integer.MAX_VALUE;
        try {
            String[] parts = timeStr.split(" ");
            String[] hm = parts[0].split(":");
            int h = Integer.parseInt(hm[0]);
            int m = Integer.parseInt(hm[1]);
            if (parts[1].equalsIgnoreCase("PM") && h != 12) {
                h += 12;
            } else if (parts[1].equalsIgnoreCase("AM") && h == 12) {
                h = 0;
            }
            return h * 60 + m;
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }
}
