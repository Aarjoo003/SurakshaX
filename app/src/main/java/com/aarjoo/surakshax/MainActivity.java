package com.aarjoo.surakshax;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // LOGIN: Purana user seedha Dashboard par
        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, Dashboard.class));
            finish();
        });

        // REGISTER: Naya user lambe flow par
        findViewById(R.id.tvRegisterLink).setOnClickListener(v -> {
            startActivity(new Intent(this, Register_Activity.class));
        });
    }
}