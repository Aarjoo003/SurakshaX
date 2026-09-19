package com.aarjoo.surakshax;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Alerts extends AppCompatActivity {

    private TextView tvCountdown;
    private CountDownTimer countDownTimer;
    private boolean isSafePressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alerts);

        // SMS Permission
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.SEND_SMS},
                102
        );

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {

            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            onBackPressed();
        });

        // Countdown
        tvCountdown = findViewById(R.id.tvCountdown);

        countDownTimer = new CountDownTimer(
                10000,
                1000
        ) {

            @Override
            public void onTick(long millisUntilFinished) {

                tvCountdown.setText(
                        "" + (millisUntilFinished / 1000)
                );
            }

            @Override
            public void onFinish() {

                if (!isSafePressed) {

                    tvCountdown.setText("0");

                    sendEmergencyAlert();
                }
            }

        }.start();

        // Safe Button
        Button btnSafe = findViewById(R.id.btnSafe);

        btnSafe.setOnClickListener(v -> {

            isSafePressed = true;

            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            Intent intent =
                    new Intent(
                            Alerts.this,
                            Happy_Screen.class
                    );

            startActivity(intent);

            finish();
        });
    }

    private void sendEmergencyAlert() {

        double lat =
                getIntent().getDoubleExtra("lat", 0.0);

        double lon =
                getIntent().getDoubleExtra("lon", 0.0);

        String mapsLink =
                "https://www.google.com/maps?q="
                        + lat + "," + lon;

        SharedPreferences prefs = getSharedPreferences(Contacts.PREFS_NAME, MODE_PRIVATE);
        String emergencyContact1 = prefs.getString(Contacts.KEY_GUARDIAN_PHONE, "9302414220");
        String emergencyContact2 = prefs.getString(Contacts.KEY_GUARDIAN_PHONE_2, "");

        String message =
                "EMERGENCY! SurakshaX detected danger. "
                        + "My Location: "
                        + mapsLink;

        try {

            SmsManager smsManager =
                    SmsManager.getDefault();

            // Send to Primary Guardian
            if (!emergencyContact1.isEmpty()) {
                smsManager.sendTextMessage(
                        emergencyContact1,
                        null,
                        message,
                        null,
                        null
                );
            }

            // Send to Secondary Guardian if set
            if (!emergencyContact2.isEmpty()) {
                smsManager.sendTextMessage(
                        emergencyContact2,
                        null,
                        message,
                        null,
                        null
                );
            }

            Toast.makeText(
                    this,
                    "SOS Sent to Guardians with Live GPS Location!",
                    Toast.LENGTH_LONG
            ).show();

            // Cloud Logging to Firebase Firestore
            try {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Map<String, Object> alertLog = new HashMap<>();
                alertLog.put("userId", user != null ? user.getUid() : "anonymous_device");
                alertLog.put("latitude", lat);
                alertLog.put("longitude", lon);
                alertLog.put("maps_link", mapsLink);
                alertLog.put("status", "CRITICAL_ALERT");
                alertLog.put("timestamp", System.currentTimeMillis());

                FirebaseFirestore.getInstance()
                        .collection("emergency_alerts")
                        .add(alertLog);
            } catch (Exception firestoreEx) {
                // Offline fallback - safe to ignore
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "SMS Failed! Check Permissions.",
                    Toast.LENGTH_SHORT
            ).show();

            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}