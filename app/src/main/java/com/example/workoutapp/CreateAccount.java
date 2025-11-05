package com.example.workoutapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;

public class CreateAccount extends AppCompatActivity {
    private EditText usernameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.create_account);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.createAccount),
                (v, insets) -> {
                    final int types = WindowInsetsCompat.Type.systemBars();
                    final android.graphics.Insets systemBars = insets.getInsets(types).toPlatformInsets();
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                }
        );

        usernameInput = findViewById(R.id.createUsername);
        emailInput = findViewById(R.id.createEmail);
        passwordInput = findViewById(R.id.createPassword);
        confirmPasswordInput = findViewById(R.id.createPassword2);

        auth = FirebaseAuth.getInstance();

        Button createButton = findViewById(R.id.createButton);
        createButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();

            // Validate fields
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email)
                    || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(CreateAccount.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                confirmPasswordInput.setError("Passwords do not match");
                return;
            } else {
                confirmPasswordInput.setError(null);
            }

            // Create Firebase user
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(CreateAccount.this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(CreateAccount.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                            // Uncomment when home page is done
                            //Intent intent = new Intent(CreateAccount.this, Home.class);
                            //startActivity(intent);
                        } else {
                            String msg = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Unknown error";
                            Toast.makeText(CreateAccount.this, "Sign up failed: " + msg, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(CreateAccount.this, Login.class);
            startActivity(intent);
        });
    }
}
