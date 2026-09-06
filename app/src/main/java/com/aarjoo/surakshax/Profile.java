package com.aarjoo.surakshax;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class Profile extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        findViewById(R.id.btnEditContacts).setOnClickListener(v -> startActivity(new Intent(this, Contacts.class)));
        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(this, Permission.class)));
        findViewById(R.id.btnSave).setOnClickListener(v -> finish());
    }
}