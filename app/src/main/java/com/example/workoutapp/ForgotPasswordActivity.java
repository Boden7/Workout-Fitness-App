package com.example.workoutapp;

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
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailInput;
    private Button requestResetButton;
    private Button backButton;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgotPassword), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.currentEmail);
        requestResetButton = findViewById(R.id.requestReset);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        requestResetButton.setOnClickListener(v -> handleRequestReset());
    }

    private void handleRequestReset() {
        String email = emailInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Please enter your email");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            return;
        }

        requestResetButton.setEnabled(false);

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    requestResetButton.setEnabled(true);
                    Toast.makeText(
                            this,
                            "Reset link sent. Check your email.",
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    requestResetButton.setEnabled(true);
                    Toast.makeText(
                            this,
                            "Failed to send reset link: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
