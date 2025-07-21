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

import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.Locale;

public class StatsActivity extends AppCompatActivity {

    Button btnToday, btnDay, btnMonth;
    TextView selectedPeriodText;
    TableLayout statsTable;
    TableLayout historyTable;
    MaterialButton loadMoreBtn;
    DBHelper dbHelper;

    String selectedDate;
    String selectedMonth;

    // Paging state for history section
    private static final int HISTORY_PAGE_SIZE = 30;
    private int historyOffset = 0;
    private int lastLoadedRows = 0; // for tracking how many were loaded last

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
        historyTable = findViewById(R.id.historyTable);
        loadMoreBtn = findViewById(R.id.loadMoreHistoryBtn);

        dbHelper = new DBHelper(this);

        selectedDate = DateUtils.getTodayDate();
        selectedMonth = selectedDate.substring(0, 7);

        btnToday.setOnClickListener(v -> {
            selectedPeriodText.setText("Showing: Today");
            selectedDate = DateUtils.getTodayDate();
            displayStatsForDay(selectedDate);
            resetHistory();
        });

        btnDay.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        selectedPeriodText.setText("Showing: " + selectedDate);
                        displayStatsForDay(selectedDate);
                        resetHistory();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH))
                    .show();
        });

        btnMonth.setOnClickListener(v -> showMonthYearPicker());

        // Load stats and history (first 30) on opening page
        displayStatsForDay(selectedDate);
        resetHistory();

        loadMoreBtn.setOnClickListener(v -> {
            historyOffset += lastLoadedRows; // move offset forward by how many we just loaded
            displayHistorySection(false);
        });
    }

    private void resetHistory() {
        historyOffset = 0;
        displayHistorySection(true); // true = full refresh (clear table)
    }

    // ===== MAIN/DAY STATS LOGIC =====

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

        int[] averages = getOverallAverages(); // [wake, ht, lt, it]

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
                    DateUtils.formatTime(averages[0]));

            appendRow("HT",
                    ht + " mins",
                    targetHT + " mins",
                    averages[1] + " mins");

            appendRow("LT",
                    lt + " mins",
                    targetLT + " mins",
                    averages[2] + " mins");

            appendRow("IT",
                    it + " mins",
                    targetIT + " mins",
                    averages[3] + " mins");
        }
    }

    // ===== MONTH STATS LOGIC UNCHANGED =====

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
                    DateUtils.formatTime(avgWake));

            appendRow("HT",
                    "--",
                    targetHT + " mins",
                    avgHT + " mins");

            appendRow("LT",
                    "--",
                    targetLT + " mins",
                    avgLT + " mins");

            appendRow("IT",
                    "--",
                    targetIT + " mins",
                    avgIT + " mins");
        }
    }

    // ===== HISTORY SECTION WITH APPEND =====

    /**
     * @param clearTable If true, clear the table and start fresh.
     *                   If false, just append to existing rows.
     */
    private void displayHistorySection(boolean clearTable) {
        if (clearTable) {
            historyTable.removeAllViews();

            TableRow header = new TableRow(this);
            int headerColor = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant);
            header.setBackgroundColor(headerColor);
            header.addView(buildBoldCell("Date"));
            header.addView(buildBoldCell("Wake-Up"));
            header.addView(buildBoldCell("HT (min)"));
            header.addView(buildBoldCell("LT (min)"));
            header.addView(buildBoldCell("IT (min)"));
            historyTable.addView(header);

            historyOffset = 0; // reset paging
        }

        // Load next page of rows from current offset
        Cursor cursor = dbHelper.getEntriesPaged(HISTORY_PAGE_SIZE, historyOffset);

        int rows = 0;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String date = cursor.getString(0);
                int wakeTime = cursor.getInt(1);
                int ht = cursor.getInt(2);
                int lt = cursor.getInt(3);
                int it = cursor.getInt(4);

                TableRow row = new TableRow(this);
                row.addView(buildCell(date));
                row.addView(buildCell(DateUtils.formatTime(wakeTime)));
                row.addView(buildCell(String.valueOf(ht)));
                row.addView(buildCell(String.valueOf(lt)));
                row.addView(buildCell(String.valueOf(it)));

                historyTable.addView(row);
                rows++;
            } while (cursor.moveToNext());
        } else if (clearTable) {
            // Only show this if it's the first load and there's nothing in the database
            TableRow row = new TableRow(this);
            TextView tv = new TextView(this);
            tv.setText("No entries yet.");
            row.addView(tv);
            historyTable.addView(row);
        }
        if (cursor != null) cursor.close();

        lastLoadedRows = rows; // track for next paging
        int totalEntries = dbHelper.getEntryCount();
        if (historyOffset + rows < totalEntries) {
            loadMoreBtn.setVisibility(View.VISIBLE);
        } else {
            loadMoreBtn.setVisibility(View.GONE);
        }
    }



    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void addHeaderRow() {
        TableRow headerRow = new TableRow(this);
        int headerColor = getThemeColor(com.google.android.material.R.attr.colorSurfaceVariant);
        headerRow.setBackgroundColor(headerColor);

        headerRow.addView(buildBoldCell("Cat."));
        headerRow.addView(buildBoldCell("Time"));
        headerRow.addView(buildBoldCell("Target"));
        headerRow.addView(buildBoldCell("Avg."));

        statsTable.addView(headerRow);
    }

    private void appendRow(String cat, String min, String target, String avg) {
        TableRow row = new TableRow(this);

        row.addView(buildCell(cat));
        row.addView(buildCell(min));
        row.addView(buildCell(target));
        row.addView(buildCell(avg));

        statsTable.addView(row);
    }

    // === Cell builders ===

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

    /**
     * Returns averages: [wake, ht, lt, it], or {-1, -1, -1, -1} if no data.
     */
    private int[] getOverallAverages() {
        Cursor cursor = dbHelper.getAllEntries();
        int count = 0, wakeSum = 0, htSum = 0, ltSum = 0, itSum = 0;

        if (cursor != null) {
            while (cursor.moveToNext()) {
                wakeSum += cursor.getInt(1);
                htSum += cursor.getInt(2);
                ltSum += cursor.getInt(3);
                itSum += cursor.getInt(4);
                count++;
            }
            cursor.close();
        }
        if (count == 0) return new int[]{-1, -1, -1, -1};
        return new int[]{
                wakeSum / count,
                htSum / count,
                ltSum / count,
                itSum / count
        };
    }

    private void showMonthYearPicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedMonth = String.format(Locale.getDefault(), "%04d-%02d", year, month + 1);
                    selectedPeriodText.setText("Showing: " + selectedMonth);
                    displayStatsForMonth(selectedMonth);
                    resetHistory();
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
}
