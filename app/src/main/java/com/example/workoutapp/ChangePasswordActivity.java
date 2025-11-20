package com.example.workoutapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText currentPasswordInput;
    private EditText newPasswordInput;
    private EditText newPasswordConfirmInput;
    private Button confirmChangeButton;
    private Button backButton;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.changePassword), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        currentPasswordInput = findViewById(R.id.currentPassword);
        newPasswordInput = findViewById(R.id.newPassword);
        newPasswordConfirmInput = findViewById(R.id.newPasswordConfirm);
        confirmChangeButton = findViewById(R.id.confirmChange);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> {
            finish();
        });

        confirmChangeButton.setOnClickListener(v -> handleChangePassword());
    }

    private void handleChangePassword() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No user is signed in.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        String currentPassword = currentPasswordInput.getText().toString();
        String newPassword = newPasswordInput.getText().toString();
        String newPasswordConfirm = newPasswordConfirmInput.getText().toString();

        // Basic validation
        if (TextUtils.isEmpty(currentPassword) ||
                TextUtils.isEmpty(newPassword) ||
                TextUtils.isEmpty(newPasswordConfirm)) {

            Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!newPassword.equals(newPasswordConfirm)) {
            newPasswordConfirmInput.setError("Passwords do not match");
            return;
        }

        // Enforce password requirements
        if (newPassword.length() < 8) {
            newPasswordInput.setError("Password must be at least 8 characters");
            return;
        }
        if (!newPassword.matches(".*[A-Z].*")) {  // Uppercase
            newPasswordInput.setError("Must contain at least one uppercase letter");
            return;
        }
        if (!newPassword.matches(".*[a-z].*")) {  // Lowercase
            newPasswordInput.setError("Must contain at least one lowercase letter");
            return;
        }
        if (!newPassword.matches(".*\\d.*")) {  // Number
            newPasswordInput.setError("Must contain at least one number");
            return;
        }
        if (!newPassword.matches(".*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/].*")) { // Special char
            newPasswordInput.setError("Must contain at least one special character");
            return;
        }

        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "No email associated with this account.", Toast.LENGTH_LONG).show();
            return;
        }

        confirmChangeButton.setEnabled(false);

        // Re-authenticate with current password and update to new password
        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);
        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(unused2 -> {
                                confirmChangeButton.setEnabled(true);
                                Toast.makeText(
                                        this,
                                        "Password updated successfully.",
                                        Toast.LENGTH_LONG
                                ).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                confirmChangeButton.setEnabled(true);
                                Toast.makeText(
                                        this,
                                        "Failed to update password: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    confirmChangeButton.setEnabled(true);
                    Toast.makeText(
                            this,
                            "Re-authentication failed. Check your current password.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
