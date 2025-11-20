package com.example.workoutapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.Collections;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat notifSwitch;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private DocumentReference userRef;

    private boolean settingFromCode = false;

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

        notifSwitch = findViewById(R.id.notifSwitch);
        notifSwitch.setEnabled(false);

        Button changeEmailButton = findViewById(R.id.changeEmailButton);
        changeEmailButton.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ChangeEmailActivity.class));
        });

        Button changePasswordButton = findViewById(R.id.changePasswordButton);
        changePasswordButton.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ChangePasswordActivity.class));
        });

        Button forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        forgotPasswordButton.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ForgotPasswordActivity.class));
        });

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(this, "You have been logged out.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SettingsActivity.this, Login.class));
        });

        Button deleteAccountButton = findViewById(R.id.deleteAccountButton);
        deleteAccountButton.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, DeleteAccountActivity.class));
        });

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            finish();
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser current = auth.getCurrentUser();
        if (current == null) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = db.collection("users").document(current.getUid());

        // Load initial value
        userRef.get().addOnSuccessListener(this::applyInitialSwitchState)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    notifSwitch.setEnabled(true);
                });

        // Update Firestore db
        notifSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (settingFromCode) return;

            notifSwitch.setEnabled(false);

            userRef.set(
                    Collections.singletonMap("receiveNotifs", isChecked),
                    SetOptions.merge()
            ).addOnSuccessListener(unused -> {
                notifSwitch.setEnabled(true);
            }).addOnFailureListener(e -> {
                settingFromCode = true;
                notifSwitch.setChecked(!isChecked);
                settingFromCode = false;

                notifSwitch.setEnabled(true);
                Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        });
    }

    private void applyInitialSwitchState(DocumentSnapshot snap) {
        boolean value = false;
        if (snap.exists()) {
            Boolean b = snap.getBoolean("receiveNotifs");
            if (b != null) value = b;
        }
        settingFromCode = true;
        notifSwitch.setChecked(value);
        settingFromCode = false;
        notifSwitch.setEnabled(true);
    }
}
