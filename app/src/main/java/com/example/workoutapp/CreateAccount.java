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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

public class CreateAccount extends AppCompatActivity {
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

        emailInput = findViewById(R.id.createEmail);
        passwordInput = findViewById(R.id.createPassword);
        confirmPasswordInput = findViewById(R.id.createPassword2);

        auth = FirebaseAuth.getInstance();

        Button createButton = findViewById(R.id.createButton);
        createButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();

            // Validate fields
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(CreateAccount.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                confirmPasswordInput.setError("Passwords do not match");
                return;
            }
            else {
                confirmPasswordInput.setError(null);
            }

            // Create Firebase user
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(CreateAccount.this, task -> {
                        if (!task.isSuccessful()) {
                            String msg = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Unknown error";
                            Toast.makeText(CreateAccount.this, "Sign up failed: " + msg, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        FirebaseUser fbUser = (task.getResult() != null) ? task.getResult().getUser() : null;
                        if (fbUser == null) {
                            Toast.makeText(CreateAccount.this, "Sign up successful, but no Firebase user returned.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String uid = fbUser.getUid();
                        String userEmail = fbUser.getEmail();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        DocumentReference userRef = db.collection("users").document(uid);
                        DocumentReference counterRef = db.collection("_meta").document("leaderboard");

                        db.runTransaction(transaction -> {
                            // 1) Read current counter
                            long current = 0L;
                            DocumentSnapshot counterSnap = transaction.get(counterRef);
                            if (counterSnap.exists()) {
                                Long c = counterSnap.getLong("current");
                                if (c != null) current = c;
                            }
                            long next = current + 1;

                            // 2) Update counter
                            Map<String, Object> counterUpdate = new HashMap<>();
                            counterUpdate.put("current", next);
                            transaction.set(counterRef, counterUpdate, SetOptions.merge());

                            // 3) Create user document
                            Map<String, Object> userDoc = new HashMap<>();
                            userDoc.put("userID", uid);
                            userDoc.put("email", userEmail);
                            userDoc.put("level", 1);
                            userDoc.put("streak", 0);
                            userDoc.put("badges", new ArrayList<String>());
                            userDoc.put("totalXP", 0);
                            userDoc.put("totalTime", 0);
                            userDoc.put("course", 0);
                            userDoc.put("receiveNotifs", false);
                            userDoc.put("lastWorkoutId", null);
                            userDoc.put("lastWorkoutDate", null);
                            userDoc.put("friends", new ArrayList<String>());
                            userDoc.put("leaderboard", next);

                            transaction.set(userRef, userDoc);
                            return null;
                        }).addOnSuccessListener(unused -> {
                            Toast.makeText(CreateAccount.this, "Account created!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(CreateAccount.this, Login.class);
                            startActivity(intent);
                        }).addOnFailureListener(e -> {
                            Toast.makeText(CreateAccount.this, "Failed to set up user in database: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    });


        });

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(CreateAccount.this, Login.class);
            startActivity(intent);
        });
    }
}
