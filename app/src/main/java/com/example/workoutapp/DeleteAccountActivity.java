package com.example.workoutapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

public class DeleteAccountActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Button confirmDeleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_delete_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.deleteAccount), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        confirmDeleteButton = findViewById(R.id.confirmDelete);
        confirmDeleteButton.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "No user is signed in.", Toast.LENGTH_LONG).show();
                return;
            }
            confirmDeleteButton.setEnabled(false);
            deleteWorkouts(user);
        });

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            finish();
        });
    }

    private void deleteWorkouts(FirebaseUser user) {
        String uid = user.getUid();

        // Delete all workouts for the user
        db.collection("workouts")
                .whereEqualTo("userID", uid)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        deleteUserInfo(querySnapshot, uid, user)
                )
                .addOnFailureListener(e -> {
                    confirmDeleteButton.setEnabled(true);
                    Toast.makeText(this, "Failed to load user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void deleteUserInfo(QuerySnapshot workoutsSnapshot, String uid, FirebaseUser user) {
        // Delete user info
        WriteBatch batch = db.batch();

        workoutsSnapshot.getDocuments().forEach(doc -> batch.delete(doc.getReference()));

        DocumentReference userRef = db.collection("users").document(uid);
        batch.delete(userRef);

        batch.commit()
                .addOnSuccessListener(unused -> decrementLeaderboardAndDeleteAuth(user))
                .addOnFailureListener(e -> {
                    confirmDeleteButton.setEnabled(true);
                    Toast.makeText(this, "Failed to delete user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void decrementLeaderboardAndDeleteAuth(FirebaseUser user) {
        DocumentReference counterRef = db.collection("_meta").document("leaderboard");

        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(counterRef);
            if (snap.exists()) {
                Long current = snap.getLong("current");
                if (current != null && current > 0) {
                    long next = current - 1;
                    transaction.update(counterRef, "current", next);
                }
            }
            return null;
        }).addOnSuccessListener(unused -> {
            deleteFirebaseAccount(user);
        }).addOnFailureListener(e -> {
            confirmDeleteButton.setEnabled(true);
            Toast.makeText(this, "Failed to update leaderboard: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
    private void incrementLeaderboardOnFailure() {
        DocumentReference counterRef = db.collection("_meta").document("leaderboard");

        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(counterRef);
            if (snap.exists()) {
                Long current = snap.getLong("current");
                if (current != null && current > 0) {
                    long next = current + 1;
                    transaction.update(counterRef, "current", next);
                }
            }
            return null;
        });
    }

    private void deleteFirebaseAccount(FirebaseUser user) {
        user.delete()
                .addOnSuccessListener(unused -> {
                    auth.signOut();
                    Toast.makeText(this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(DeleteAccountActivity.this, Login.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    confirmDeleteButton.setEnabled(true);

                    if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                        Toast.makeText(
                                this,
                                "Please log in again and then delete your account.",
                                Toast.LENGTH_LONG
                        ).show();
                        incrementLeaderboardOnFailure();
                        auth.signOut();
                        Intent intent = new Intent(DeleteAccountActivity.this, Login.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(
                                this,
                                "Failed to delete account: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        incrementLeaderboardOnFailure();
                    }
                });

    }
}