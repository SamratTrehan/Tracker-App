package com.example.dailytracker;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Calendar;

public class TargetSetupActivity extends AppCompatActivity {

    TextView instructionText, wakeTimeDisplay;
    Button pickWakeTimeBtn, saveBtn;
    EditText htInput, ltInput, itInput;
    int wakeMinutes = 7 * 60; // Default to 7:00 AM

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_target_setup);

        instructionText = findViewById(R.id.instructionText);
        wakeTimeDisplay = findViewById(R.id.wakeTimeDisplay);
        pickWakeTimeBtn = findViewById(R.id.pickWakeTimeBtn);
        htInput = findViewById(R.id.htInput);
        ltInput = findViewById(R.id.ltInput);
        itInput = findViewById(R.id.itInput);
        saveBtn = findViewById(R.id.saveBtn);

        wakeTimeDisplay.setText("Wake-up Time: " + DateUtils.formatTime(wakeMinutes));
        htInput.setText("60");
        ltInput.setText("120");
        itInput.setText("180");

        pickWakeTimeBtn.setOnClickListener(v -> {
            int hour = wakeMinutes / 60;
            int minute = wakeMinutes % 60;

            TimePickerDialog timePicker = new TimePickerDialog(this, (tp, h, m) -> {
                wakeMinutes = h * 60 + m;
                wakeTimeDisplay.setText("Wake-up Time: " + DateUtils.formatTime(h, m));
            }, hour, minute, false);
            timePicker.show();
        });

        saveBtn.setOnClickListener(v -> {
            try {
                int ht = Integer.parseInt(htInput.getText().toString());
                int lt = Integer.parseInt(ltInput.getText().toString());
                int it = Integer.parseInt(itInput.getText().toString());

                TargetPreferences.saveTargets(this, wakeMinutes, ht, lt, it);

                SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                prefs.edit().putBoolean("setup_done", true).apply();

                startActivity(new Intent(this, MainActivity.class));
                finish();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
