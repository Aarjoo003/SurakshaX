package com.aarjoo.surakshax;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class Register_Activity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        findViewById(R.id.btnContinue).setOnClickListener(v -> {
            startActivity(new Intent(this, Contacts.class));
        });
    }
}