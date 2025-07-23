package com.example.dailytracker;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView wakeTimeDisplay, selectedDateText;
    EditText htInput, ltInput, itInput, ftInput;
    Button pickWakeTimeBtn, submitBtn, viewStatsBtn, exportBtn, clearAllBtn, pickDateBtn, editValuesBtn;
    DBHelper dbHelper;
    int wakeMinutes = -1;
    String today, selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean setupDone = prefs.getBoolean("setup_done", false);
        if (!setupDone) {
            Intent intent = new Intent(this, TargetSetupActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        wakeTimeDisplay = findViewById(R.id.wakeTimeDisplay);
        pickWakeTimeBtn = findViewById(R.id.pickWakeTimeBtn);
        htInput = findViewById(R.id.htInput);
        ltInput = findViewById(R.id.ltInput);
        itInput = findViewById(R.id.itInput);
        ftInput = findViewById(R.id.ftInput);
        submitBtn = findViewById(R.id.submitBtn);
        viewStatsBtn = findViewById(R.id.viewStatsBtn);
        exportBtn = findViewById(R.id.exportBtn);
        clearAllBtn = findViewById(R.id.clearAllBtn);
        editValuesBtn = findViewById(R.id.editValuesBtn);

        dbHelper = new DBHelper(this);

        wakeTimeDisplay.setText("Select Wake-up Time");
        selectedDateText = findViewById(R.id.selectedDateText);
        pickDateBtn = findViewById(R.id.pickDateBtn);

        // Default to today's date
        selectedDate = DateUtils.getTodayDate();
        selectedDateText.setText("Selected Date: Today");

        pickDateBtn.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    MainActivity.this,
                    (view1, year, month, dayOfMonth) -> {
                        Calendar selectedCal = Calendar.getInstance();
                        selectedCal.set(year, month, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        selectedDate = sdf.format(selectedCal.getTime());

                        // If selected date is today, say "Today"
                        String today = DateUtils.getTodayDate();
                        if (selectedDate.equals(today)) {
                            selectedDateText.setText("Selected Date: Today");
                        } else {
                            selectedDateText.setText("Selected Date: " + selectedDate);
                        }
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            datePickerDialog.show();
        });

        editValuesBtn.setOnClickListener(view -> {
            String date = selectedDate;

            Integer wake = wakeMinutes != -1 ? wakeMinutes : null;
            Integer ht = (!htInput.getText().toString().isEmpty())
                    ? Integer.parseInt(htInput.getText().toString()) : null;
            Integer lt = (!ltInput.getText().toString().isEmpty())
                    ? Integer.parseInt(ltInput.getText().toString()) : null;
            Integer it = (!itInput.getText().toString().isEmpty())
                    ? Integer.parseInt(itInput.getText().toString()) : null;
            Integer ft = (!ftInput.getText().toString().isEmpty())
                    ? Integer.parseInt(ftInput.getText().toString()) : null;

            if (!dbHelper.entryExists(date)) {
                Toast.makeText(this, "No entry exists for this date to edit.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (wake == null && ht == null && lt == null && it == null && ft == null) {
                Toast.makeText(this, "Enter at least one value to update.", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean updated = dbHelper.updatePartialEntry(date, wake, ht, lt, it, ft);

            if (updated) {
                Toast.makeText(this, "Entry updated successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error: Could not update entry.", Toast.LENGTH_SHORT).show();
            }
        });

        pickWakeTimeBtn.setOnClickListener(view -> {
            int targetWakeMinutes = TargetPreferences.getWakeTime(this);
            int hour = targetWakeMinutes / 60;
            int minute = targetWakeMinutes % 60;

            TimePickerDialog timePicker = new TimePickerDialog(this, (tp, h, m) -> {
                wakeMinutes = h * 60 + m;
                wakeTimeDisplay.setText("Wake-up Time: " + DateUtils.formatTime(h, m));
            }, hour, minute, false);
            timePicker.show();
        });

        submitBtn.setOnClickListener(view -> {
            if (wakeMinutes == -1) {
                Toast.makeText(this, "Please pick wake-up time", Toast.LENGTH_SHORT).show();
                return;
            }

            String date = selectedDate;
            int ht, lt, it, ft;

            try {
                ht = Integer.parseInt(htInput.getText().toString());
                lt = Integer.parseInt(ltInput.getText().toString());
                it = Integer.parseInt(itInput.getText().toString());
                ft = Integer.parseInt(ftInput.getText().toString());
            } catch (Exception e) {
                Toast.makeText(this, "Please enter all values", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.entryExists(date)) {
                Toast.makeText(this, "Entry already exists!", Toast.LENGTH_SHORT).show();
            } else {
                boolean inserted = dbHelper.insertEntry(date, wakeMinutes, ht, lt, it, ft);
                Toast.makeText(this, inserted ? "Data Saved" : "Error saving data", Toast.LENGTH_SHORT).show();
            }
        });

        viewStatsBtn.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, StatsActivity.class));
        });

        exportBtn.setOnClickListener(view -> {
            CSVExporter.exportToCSV(this);
        });

        clearAllBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Reset Targets")
                    .setMessage("This will take you to the target setup screen to update your targets. Your existing data will not be deleted. Continue?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Intent intent = new Intent(MainActivity.this, TargetSetupActivity.class);
                        startActivity(intent);
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        createNotificationChannel();
        scheduleNotification();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel("reminder", "Daily Reminder", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    private void scheduleNotification() {
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 21);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (alarmManager != null) {
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }
}
