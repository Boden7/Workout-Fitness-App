package com.example.workoutapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button changeEmailButton = findViewById(R.id.changeEmailButton);
        changeEmailButton.setOnClickListener(v -> {
            // Uncomment when the ChangeEmailActivity class is added
            //Intent intent = new Intent(SettingsActivity.this, ChangeEmailActivity.class);
            //startActivity(intent);
        });

        Button changePasswordButton = findViewById(R.id.changePasswordButton);
        changePasswordButton.setOnClickListener(v -> {
            // Uncomment when the ChangePasswordActivity class is added to GitHub
            //Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
            //startActivity(intent);
        });

        Button deleteAccountButton = findViewById(R.id.deleteAccountButton);
        deleteAccountButton.setOnClickListener(v -> {
            // Uncomment when the DeleteAccountActivity class is added to GitHub
            //Intent intent = new Intent(SettingsActivity.this, DeleteAccountActivity.class);
            //startActivity(intent);
        });

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            // Uncomment when the Profile class is added to GitHub
            //Intent intent = new Intent(SettingsActivity.this, Profile.class);
            //startActivity(intent);
        });
    }
}
