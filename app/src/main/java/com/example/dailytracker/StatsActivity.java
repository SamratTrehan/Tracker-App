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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class StatsActivity extends AppCompatActivity {

    Button btnToday, btnDay, btnMonth;
    TextView selectedPeriodText;
    TableLayout statsTable;
    DBHelper dbHelper;
    BarChart barChart;

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
        barChart = findViewById(R.id.barChart);

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
        displayWeeklyBarChart();
    }

    private void displayWeeklyBarChart() {
        // Prepare arrays for 7 days of data
        boolean isDarkTheme = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        ArrayList<String> dayLabels = new ArrayList<>();
        ArrayList<BarEntry> htEntries = new ArrayList<>();
        ArrayList<BarEntry> ltEntries = new ArrayList<>();
        ArrayList<BarEntry> itEntries = new ArrayList<>();

        // Build last 7 dates
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat labelSdf = new SimpleDateFormat("MM/dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        for (int i = 6; i >= 0; i--) {
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.add(Calendar.DAY_OF_YEAR, -i);

            String date = sdf.format(cal.getTime());
            dayLabels.add(labelSdf.format(cal.getTime()));

            int index = 6 - i;

            Cursor c = dbHelper.getEntryByDate(date);
            int ht = 0, lt = 0, it = 0;
            if (c != null && c.moveToFirst()) {
                ht = c.getInt(2);
                lt = c.getInt(3);
                it = c.getInt(4);
            }
            if (c != null) c.close();

            htEntries.add(new BarEntry(index, ht));
            ltEntries.add(new BarEntry(index, lt));
            itEntries.add(new BarEntry(index, it));
        }

        // Make 3 BarDataSets
        BarDataSet setHT = new BarDataSet(htEntries, "Health Time");
        setHT.setColor(Color.parseColor("#2196F3")); // Blue
        BarDataSet setLT = new BarDataSet(ltEntries, "Learning Time");
        setLT.setColor(Color.parseColor("#43A047")); // Green
        BarDataSet setIT = new BarDataSet(itEntries, "Implementation Time");
        setIT.setColor(Color.parseColor("#FB8C00")); // Orange

        // Grouped Bar Chart
        float groupSpace = 0.12f;
        float barSpace = 0.04f;
        float barWidth = 0.26f;

        BarData data = new BarData(setHT, setLT, setIT);
        data.setBarWidth(barWidth);

        barChart.setData(data);

        // Setup axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int i = Math.round(value);
                if (i >= 0 && i < dayLabels.size()) {
                    return dayLabels.get(i);
                } else {
                    return "";
                }
            }
        });
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        barChart.getDescription().setText("Last 7 Days (by log date)");
        barChart.setDrawGridBackground(false);
        barChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        barChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        int gridColor, axisTextColor, chartBgColor;
        if (isDarkTheme) {
            gridColor = getResources().getColor(R.color.textSecondary);
            axisTextColor = getResources().getColor(R.color.textPrimary);
            chartBgColor = getResources().getColor(R.color.md_theme_dark_surface);
        } else {
            gridColor = getResources().getColor(R.color.textSecondary);
            axisTextColor = getResources().getColor(R.color.textPrimary);
            chartBgColor = getResources().getColor(R.color.md_theme_light_surface);
        }

        // Chart background and grid
        barChart.setBackgroundColor(chartBgColor);
        barChart.getXAxis().setTextColor(axisTextColor);
        barChart.getAxisLeft().setTextColor(axisTextColor);
        barChart.getAxisRight().setTextColor(axisTextColor);
        barChart.getXAxis().setGridColor(gridColor);
        barChart.getAxisLeft().setGridColor(gridColor);
        barChart.getAxisRight().setGridColor(gridColor);
        barChart.getLegend().setTextColor(axisTextColor);
        barChart.getDescription().setTextColor(axisTextColor);
        // Animate
        barChart.animateY(1000);

        // Group bars
        barChart.getXAxis().setAxisMinimum(-0.5f);
        barChart.getXAxis().setAxisMaximum(6.5f);
        barChart.groupBars(0f, groupSpace, barSpace);

        barChart.invalidate();
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
                    -calculateDeviation(avgWake, targetWake));

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
}
