package com.example.workoutapp;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.ZoneId;

public class CardioFragment extends Fragment {

    private WorkoutViewModel viewModel;
    private TextView cardioTimerText;
    private Handler handler = new Handler();

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel.isRunning) {
                long elapsedMillis = System.currentTimeMillis() - viewModel.startTime;
                int totalSeconds = (int) (elapsedMillis / 1000);
                viewModel.seconds = totalSeconds;

                int minutes = totalSeconds / 60;
                int secs = totalSeconds % 60;

                cardioTimerText.setText(String.format("%02d:%02d", minutes, secs));

                handler.postDelayed(this, 1000);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_cardio, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(WorkoutViewModel.class);

        cardioTimerText = view.findViewById(R.id.timerCarText);
        ImageButton startButton = view.findViewById(R.id.startButton);
        ImageButton pauseButton = view.findViewById(R.id.pauseButton);
        ImageButton stopButton = view.findViewById(R.id.stopButton);
        ImageButton backButton = view.findViewById(R.id.backButton);
        TextView minutesText = view.findViewById(R.id.minutesText);

        // Restore timer text
        int minutes = viewModel.seconds / 60;
        int secs = viewModel.seconds % 60;
        cardioTimerText.setText(String.format("%02d:%02d", minutes, secs));

        // If timer is running, show pause button
        if (viewModel.isRunning) {
            startButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            handler.post(timerRunnable);
        }

        // Start button
        startButton.setOnClickListener(v -> {
            viewModel.startTime = System.currentTimeMillis() - viewModel.seconds * 1000;
            viewModel.isRunning = true;
            startButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            handler.post(timerRunnable);
        });

        // Pause button
        pauseButton.setOnClickListener(v -> {
            viewModel.isRunning = false;
            pauseButton.setVisibility(View.GONE);
            startButton.setVisibility(View.VISIBLE);
            handler.removeCallbacks(timerRunnable);
        });

        // Stop button
        stopButton.setOnClickListener(v -> {
            viewModel.isRunning = false;

            int mins = viewModel.seconds / 60;
            
            // Update local UI for minutes
            minutesText.setText(String.valueOf(mins));

            if (mins > 0) {
                // 1. Update Basic Stats (XP, Time, Count)
                long xpToAdd = mins * 100L;
                updateUserStats(xpToAdd, mins);
                Toast.makeText(getContext(), "You earned " + xpToAdd + " XP and logged " + mins + " mins!", Toast.LENGTH_SHORT).show();

                // 2. Check Streak Condition (> 10 mins)
                if (mins >= 10) {
                    updateWorkoutAndStreak();
                } else {
                    Toast.makeText(getContext(), "Workout needs to be at least 10 mins to update streak.", Toast.LENGTH_LONG).show();
                }
            }

            // Reset timer
            viewModel.seconds = 0;
            cardioTimerText.setText("00:00");
            pauseButton.setVisibility(View.GONE);
            startButton.setVisibility(View.VISIBLE);

            handler.removeCallbacks(timerRunnable);
        });

        // Back button
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        TextView seeAll = view.findViewById(R.id.seeAll);
        seeAll.setOnClickListener(v -> {
            // Create the fragment you want to navigate to
            Fragment cardioVideosFragment = new CardioVideosFragment();

            // Perform the fragment transaction
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, cardioVideosFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void updateWorkoutAndStreak() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(doc -> {
            Timestamp lastTS = doc.getTimestamp("lastWorkoutDate");
            Long currentStreak = doc.getLong("streak");
            if (currentStreak == null) currentStreak = 0L;

            LocalDate today = LocalDate.now();
            long newStreak = currentStreak;
            long xpBonus = 0;

            if (lastTS == null) {
                // First time ever
                newStreak = 1;
                xpBonus = 500;
            } else {
                LocalDate lastDate = lastTS.toDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                if (today.isEqual(lastDate)) {
                    // Same day: No streak change, no XP bonus
                    newStreak = currentStreak;
                    xpBonus = 0;
                } else if (today.minusDays(1).isEqual(lastDate)) {
                    // Consecutive day: Increment streak
                    newStreak = currentStreak + 1;
                    
                    // Calculate Bonus based on NEW Streak
                    if (newStreak == 2) {
                        xpBonus = 1000;
                    } else if (newStreak >= 3) {
                        xpBonus = 1500;
                    } else {
                        xpBonus = 500; 
                    }
                } else {
                    // Streak broken (missed a day): Reset to 1
                    newStreak = 1;
                    xpBonus = 500;
                }
            }

            // Perform Updates
            if (xpBonus > 0) {
                long finalXpBonus = xpBonus;
                // Update Streak, Date, and Add Bonus XP
                userRef.update(
                        "streak", newStreak,
                        "lastWorkoutDate", Timestamp.now(),
                        "totalXP", FieldValue.increment(xpBonus)
                ).addOnSuccessListener(aVoid -> {
                    // Check if this bonus XP caused a level up
                    checkForLevelUp(uid, finalXpBonus);
                });
                Toast.makeText(getContext(), "You completed today's streak (Day " + newStreak + ")! Bonus: " + xpBonus + " XP!", Toast.LENGTH_LONG).show();
            } else {
                // Same day update (just update timestamp to latest time)
                userRef.update("lastWorkoutDate", Timestamp.now());
                Toast.makeText(getContext(), "You have already completed today's streak.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserStats(long xpToAdd, int minutesToAdd) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DocumentReference userRef = db.collection("users").document(uid);
            // Atomically increment "totalXP", "totalTime", "totalWorkout"
            userRef.update(
                "totalXP", FieldValue.increment(xpToAdd),
                "totalTime", FieldValue.increment(minutesToAdd),
                "totalWorkout", FieldValue.increment(1)
            ).addOnSuccessListener(aVoid -> {
                // Check if this regular XP caused a level up
                checkForLevelUp(uid, xpToAdd);
            }).addOnFailureListener(e -> {
                Log.e("CardioFragment", "Error updating stats", e);
                Toast.makeText(getContext(), "Failed to update stats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void checkForLevelUp(String uid, long addedXP) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long totalXP = documentSnapshot.getLong("totalXP");
                    if (totalXP != null) {
                        long currentLevel = totalXP / 1000;
                        // Calculate what the XP was before the addition
                        long previousLevel = (totalXP - addedXP) / 1000;
                        
                        if (currentLevel > previousLevel) {
                            String badgeName = currentLevel + " Level Badge";
                            
                            // Update level and add badge in Firestore
                            FirebaseFirestore.getInstance().collection("users").document(uid)
                                    .update(
                                        "level", currentLevel,
                                        "badges", FieldValue.arrayUnion(badgeName)
                                    );
                            Toast.makeText(getContext(), "Level Up! You are now Level " + currentLevel + "! Badge Earned: " + badgeName, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(timerRunnable);
    }
}