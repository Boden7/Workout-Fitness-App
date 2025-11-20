package com.example.workoutapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChangeEmailActivity extends AppCompatActivity {

    private EditText currentEmailInput;
    private EditText newEmailInput;
    private EditText passwordInput;
    private Button confirmChangeButton;
    private Button backButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_email);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.changeEmail), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        currentEmailInput = findViewById(R.id.currentEmail);
        newEmailInput = findViewById(R.id.newEmail);
        passwordInput = findViewById(R.id.password);
        confirmChangeButton = findViewById(R.id.confirmChange);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> {
            finish();
        });

        confirmChangeButton.setOnClickListener(v -> handleChangeEmail());
    }

    private void handleChangeEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No user is signed in.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        String currentEmail = currentEmailInput.getText().toString().trim();
        String newEmail = newEmailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        // Basic validation
        if (currentEmail.isEmpty() || newEmail.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            currentEmailInput.setError("Enter a valid email");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            newEmailInput.setError("Enter a valid email");
            return;
        }

        if (currentEmail.equals(newEmail)) {
            Toast.makeText(this, "New email must be different from current email.", Toast.LENGTH_LONG).show();
            return;
        }

        String authEmail = user.getEmail();
        if (authEmail == null || !authEmail.equals(currentEmail)) {
            currentEmailInput.setError("Does not match your current account email.");
            Toast.makeText(this, "Current email does not match the one on your account.", Toast.LENGTH_LONG).show();
            return;
        }

        confirmChangeButton.setEnabled(false);

        // Re-authenticate with current email + password
        AuthCredential credential = EmailAuthProvider.getCredential(authEmail, password);
        user.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    // Send verification email and schedule email change
                    user.verifyBeforeUpdateEmail(newEmail)
                            .addOnSuccessListener(unused2 -> {
                                DocumentReference userRef = db.collection("users")
                                        .document(user.getUid());
                                userRef.update("email", newEmail)
                                        .addOnSuccessListener(unused3 -> {
                                            confirmChangeButton.setEnabled(true);
                                            Toast.makeText(
                                                    this,
                                                    "Verification email sent. Please confirm the change from your new email.",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e2 -> {
                                            confirmChangeButton.setEnabled(true);
                                            Toast.makeText(
                                                    this,
                                                    "Login email change scheduled, but failed to update profile: "
                                                            + e2.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                confirmChangeButton.setEnabled(true);
                                Toast.makeText(
                                        this,
                                        "Failed to start email change: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    confirmChangeButton.setEnabled(true);
                    Toast.makeText(
                            this,
                            "Re-authentication failed. Check your password and try again.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
