package com.aarjoo.surakshax;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import com.google.android.gms.tasks.CancellationTokenSource;

import org.json.JSONException;
import org.json.JSONObject;

public class Dashboard extends AppCompatActivity {

    private EditText etAudio, etFall, etHR;
    private Button btnAnalyze;

    private double latitude = 0.0;
    private double longitude = 0.0;

    // Local Flask REST API URL:
    // Configured for real phone on Wi-Fi (Laptop IP: 10.220.117.184):
    private final String FLASK_URL =
            "http://10.220.117.184:5000/analyze_status";

    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // --------------------------------------------------
        // INITIALIZE LOCATION
        // --------------------------------------------------

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        // --------------------------------------------------
        // EXISTING BUTTONS
        // --------------------------------------------------

        findViewById(R.id.btnTriggerSOS).setOnClickListener(v -> {

            getCurrentLocationForSOS();

        });

        findViewById(R.id.btnMaps).setOnClickListener(v ->
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("geo:0,0?q=police+station")
                )));

        findViewById(R.id.btnCall112).setOnClickListener(v ->
                startActivity(new Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:112")
                )));

        findViewById(R.id.btnCall1091).setOnClickListener(v ->
                startActivity(new Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:1091")
                )));

        findViewById(R.id.btnProfile).setOnClickListener(v ->
                startActivity(new Intent(this, Profile.class)));

        // --------------------------------------------------
        // TEST MODE INPUTS
        // --------------------------------------------------

        etAudio = findViewById(R.id.etAudio);
        etFall = findViewById(R.id.etFall);
        etHR = findViewById(R.id.etHR);

        btnAnalyze = findViewById(R.id.btnAnalyze);

        // --------------------------------------------------
        // REQUEST LOCATION PERMISSION
        // --------------------------------------------------

        requestLocationPermission();

        // --------------------------------------------------
        // ANALYZE BUTTON
        // --------------------------------------------------

        btnAnalyze.setOnClickListener(v -> {

            String audioText =
                    etAudio.getText().toString().trim();

            String fallText =
                    etFall.getText().toString().trim();

            String hrText =
                    etHR.getText().toString().trim();

            if (audioText.isEmpty()
                    || fallText.isEmpty()
                    || hrText.isEmpty()) {

                Toast.makeText(
                        this,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            try {

                Double.parseDouble(audioText);
                Double.parseDouble(fallText);
                Double.parseDouble(hrText);

            } catch (NumberFormatException e) {

                Toast.makeText(
                        this,
                        "Please enter valid numbers",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            fetchCurrentLocationAndAnalyze();
        });
    }

    // ======================================================
    // REQUEST LOCATION PERMISSION
    // ======================================================

    private void requestLocationPermission() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );
        }
    }

    // ======================================================
    // ANALYZE - GET CURRENT LOCATION
    // ======================================================

    private void fetchCurrentLocationAndAnalyze() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(
                    this,
                    "Please allow location permission",
                    Toast.LENGTH_LONG
            ).show();

            requestLocationPermission();

            return;
        }

        Toast.makeText(
                this,
                "Getting current location...",
                Toast.LENGTH_SHORT
        ).show();

        CancellationTokenSource cancellationTokenSource =
                new CancellationTokenSource();

        fusedLocationClient
                .getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.getToken()
                )
                .addOnSuccessListener(location -> {

                    if (location != null) {

                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                        Toast.makeText(
                                this,
                                "Location found!",
                                Toast.LENGTH_SHORT
                        ).show();

                        analyzeWithLocation();

                    } else {

                        Toast.makeText(
                                this,
                                "GPS location unavailable. Set emulator location first.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Location error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // ======================================================
    // SEND DATA TO RENDER
    // ======================================================

    private void analyzeWithLocation() {

        double audio =
                Double.parseDouble(
                        etAudio.getText().toString().trim()
                );

        double fall =
                Double.parseDouble(
                        etFall.getText().toString().trim()
                );

        double hr =
                Double.parseDouble(
                        etHR.getText().toString().trim()
                );

        sendDataToRender(
                audio,
                fall,
                hr,
                latitude,
                longitude
        );
    }

    // ======================================================
    // BACKEND API
    // ======================================================

    private void sendDataToRender(
            double audio,
            double fall,
            double hr,
            double lat,
            double lon
    ) {

        Toast.makeText(
                this,
                "Analyzing situation...",
                Toast.LENGTH_SHORT
        ).show();

        RequestQueue queue =
                Volley.newRequestQueue(this);

        JSONObject jsonBody =
                new JSONObject();

        try {

            jsonBody.put("audio_score", audio);
            jsonBody.put("fall_force", fall);
            jsonBody.put("heart_rate", hr);
            jsonBody.put("latitude", lat);
            jsonBody.put("longitude", lon);

        } catch (JSONException e) {

            Toast.makeText(
                    this,
                    "JSON Error",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        JsonObjectRequest request =
                new JsonObjectRequest(
                        Request.Method.POST,
                        FLASK_URL,
                        jsonBody,

                        response -> {

                            try {

                                String decision =
                                        response.getString("decision");

                                Toast.makeText(
                                        this,
                                        "Flask Backend Connected!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                // ----------------------------------
                                // CRITICAL
                                // ----------------------------------

                                if (decision.equalsIgnoreCase("CRITICAL")) {

                                    Intent intent =
                                            new Intent(
                                                    Dashboard.this,
                                                    Alerts.class
                                            );

                                    intent.putExtra(
                                            "lat",
                                            lat
                                    );

                                    intent.putExtra(
                                            "lon",
                                            lon
                                    );

                                    startActivity(intent);

                                }

                                // ----------------------------------
                                // SAFE
                                // ----------------------------------

                                else {

                                    Toast.makeText(
                                            this,
                                            "User Safe (Normal Activity)",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }

                            } catch (JSONException e) {

                                Toast.makeText(
                                        this,
                                        "Invalid backend response",
                                        Toast.LENGTH_LONG
                                ).show();

                                e.printStackTrace();
                            }
                        },

                        error -> {

                            Toast.makeText(
                                    this,
                                    "Connection Failed! Make sure Flask ('python app.py') is running.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );

        queue.add(request);
    }

    // ======================================================
    // SOS BUTTON - GET LOCATION FIRST
    // ======================================================

    private void getCurrentLocationForSOS() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(
                    this,
                    "Please allow location permission",
                    Toast.LENGTH_LONG
            ).show();

            requestLocationPermission();

            return;
        }

        Toast.makeText(
                this,
                "Getting SOS location...",
                Toast.LENGTH_SHORT
        ).show();

        CancellationTokenSource cancellationTokenSource =
                new CancellationTokenSource();

        fusedLocationClient
                .getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cancellationTokenSource.getToken()
                )
                .addOnSuccessListener(location -> {

                    Intent intent =
                            new Intent(
                                    Dashboard.this,
                                    Alerts.class
                            );

                    if (location != null) {

                        intent.putExtra(
                                "lat",
                                location.getLatitude()
                        );

                        intent.putExtra(
                                "lon",
                                location.getLongitude()
                        );

                    } else {

                        // No GPS available
                        intent.putExtra(
                                "lat",
                                0.0
                        );

                        intent.putExtra(
                                "lon",
                                0.0
                        );
                    }

                    startActivity(intent);
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Could not get SOS location",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // ======================================================
    // PERMISSION RESULT
    // ======================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST) {

            boolean granted = false;

            for (int result : grantResults) {

                if (result ==
                        PackageManager.PERMISSION_GRANTED) {

                    granted = true;
                    break;
                }
            }

            if (granted) {

                Toast.makeText(
                        this,
                        "Location permission granted",
                        Toast.LENGTH_SHORT
                ).show();

            } else {

                Toast.makeText(
                        this,
                        "Location permission denied",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}