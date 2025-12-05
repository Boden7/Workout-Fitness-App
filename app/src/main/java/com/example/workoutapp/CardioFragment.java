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

    private int steps = 0;
    private Handler stepHandler = new Handler();

    // Save today's date for step persistence
    private String getTodayDate() {
        return LocalDate.now().toString();
    }

    // ======================================
    // TIMER RUNNABLE
    // ======================================
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

    // ======================================
    // STEP RUNNABLE
    // ======================================
    private final Runnable stepRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel.isRunning) {

                int randomStep = 1 + (int)(Math.random() * 3);
                steps += randomStep;

                if (getView() != null) {
                    TextView stepsText = getView().findViewById(R.id.stepsText);
                    stepsText.setText(String.valueOf(steps));
                }

                int minutes = viewModel.seconds / 60;

                if (getView() != null) {
                    TextView minutesText = getView().findViewById(R.id.minutesText);
                    minutesText.setText(String.valueOf(minutes));
                }

                stepHandler.postDelayed(this, 1000);
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
        TextView stepsText = view.findViewById(R.id.stepsText);

        // ======================================
        // RESTORE STEPS FROM TODAY
        // ======================================
        android.content.SharedPreferences prefs =
                requireActivity().getSharedPreferences("cardio_session", android.content.Context.MODE_PRIVATE);

        String savedDate = prefs.getString("steps_date", "");
        int savedSteps = prefs.getInt("steps_today", 0);

        if (savedDate.equals(getTodayDate())) {

            steps = savedSteps;
        } else {
            steps = 0;

            prefs.edit()
                    .putInt("steps_today", 0)
                    .putString("steps_date", getTodayDate())
                    .apply();
        }


        stepsText.setText(String.valueOf(steps));

        // ======================================
        // RESTORE TIMER
        // ======================================
        int minutes = viewModel.seconds / 60;
        int secs = viewModel.seconds % 60;
        cardioTimerText.setText(String.format("%02d:%02d", minutes, secs));
        minutesText.setText(String.valueOf(minutes));

        if (viewModel.isRunning) {
            startButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            handler.post(timerRunnable);
            stepHandler.post(stepRunnable);
        }

        // ======================================
        // START BUTTON
        // ======================================
        startButton.setOnClickListener(v -> {
            viewModel.startTime = System.currentTimeMillis() - viewModel.seconds * 1000;
            viewModel.isRunning = true;

            startButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);

            handler.post(timerRunnable);
            stepHandler.post(stepRunnable);
        });

        // ======================================
        // PAUSE BUTTON
        // ======================================
        pauseButton.setOnClickListener(v -> {
            viewModel.isRunning = false;

            pauseButton.setVisibility(View.GONE);
            startButton.setVisibility(View.VISIBLE);

            handler.removeCallbacks(timerRunnable);
            stepHandler.removeCallbacks(stepRunnable);
        });

        // ======================================
        // STOP BUTTON
        // ======================================
        stopButton.setOnClickListener(v -> {
            viewModel.isRunning = false;

            int mins = viewModel.seconds / 60;

            minutesText.setText(String.valueOf(mins));

            // Save total cardio stats to Firestore
            saveCardioStats(mins, steps);

            // Save today's steps LOCALLY so UI doesn't reset
            requireActivity()
                    .getSharedPreferences("cardio_session", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putInt("steps_today", steps)
                    .putString("steps_date", getTodayDate())
                    .apply();

            if (mins > 0) {
                long xpToAdd = mins * 100L;
                updateUserStats(xpToAdd, mins);

                logWorkout(xpToAdd, mins, "cardio");

                Toast.makeText(getContext(),
                        "You earned " + xpToAdd + " XP and logged " + mins + " mins!",
                        Toast.LENGTH_SHORT).show();

                if (mins >= 10) {
                    updateWorkoutAndStreak();
                }
            }

            // Reset only the timer (NOT steps)
            viewModel.seconds = 0;
            cardioTimerText.setText("00:00");

            pauseButton.setVisibility(View.GONE);
            startButton.setVisibility(View.VISIBLE);

            handler.removeCallbacks(timerRunnable);
            stepHandler.removeCallbacks(stepRunnable);
        });

        // Back button
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        // See All Videos
        TextView seeAll = view.findViewById(R.id.seeAll);
        seeAll.setOnClickListener(v -> {
            Fragment cardioVideosFragment = new CardioVideosFragment();
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, cardioVideosFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    // =========================================================
    // SAVE CARDIO STATS TO FIRESTORE
    // =========================================================
    private void saveCardioStats(int minutes, int steps) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference ref = db.collection("users").document(uid);

        ref.update(
                        "cardioMinutes", FieldValue.increment(minutes),
                        "cardioSteps", FieldValue.increment(steps)
                )
                .addOnSuccessListener(a -> Log.d("CardioFragment", "Cardio stats saved!"))
                .addOnFailureListener(e -> Log.e("CardioFragment", "Failed to save cardio stats", e));
    }

    // =========================================================
    // STREAK + XP
    // =========================================================
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
                newStreak = 1;
                xpBonus = 500;
            } else {
                LocalDate lastDate = lastTS.toDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                if (today.isEqual(lastDate)) {
                    xpBonus = 0;
                } else if (today.minusDays(1).isEqual(lastDate)) {
                    newStreak = currentStreak + 1;

                    if (newStreak == 2) xpBonus = 1000;
                    else if (newStreak >= 3) xpBonus = 1500;
                    else xpBonus = 500;

                } else {
                    newStreak = 1;
                    xpBonus = 500;
                }
            }

            if (xpBonus > 0) {
                long finalXp = xpBonus;

                userRef.update(
                        "streak", newStreak,
                        "lastWorkoutDate", Timestamp.now(),
                        "totalXP", FieldValue.increment(xpBonus)
                ).addOnSuccessListener(aVoid -> checkForLevelUp(uid, finalXp));

                Toast.makeText(getContext(),
                        "You completed today's streak! Day " + newStreak + " — Bonus: " + xpBonus + " XP!",
                        Toast.LENGTH_LONG).show();

            } else {
                userRef.update("lastWorkoutDate", Timestamp.now());
            }
        });
    }

    private void updateUserStats(long xpToAdd, int minutesToAdd) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.update(
                        "totalXP", FieldValue.increment(xpToAdd),
                        "totalTime", FieldValue.increment(minutesToAdd),
                        "totalWorkout", FieldValue.increment(1)
                ).addOnSuccessListener(aVoid -> checkForLevelUp(uid, xpToAdd))
                .addOnFailureListener(e -> {
                    Log.e("CardioFragment", "Failed to update stats", e);
                });
    }

    private void checkForLevelUp(String uid, long addedXP) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    Long totalXP = doc.getLong("totalXP");
                    if (totalXP == null) return;

                    long currentLevel = totalXP / 1000;
                    long previousLevel = (totalXP - addedXP) / 1000;

                    if (currentLevel > previousLevel) {
                        String badgeName = currentLevel + " Level Badge";

                        FirebaseFirestore.getInstance().collection("users")
                                .document(uid)
                                .update(
                                        "level", currentLevel,
                                        "badges", FieldValue.arrayUnion(badgeName)
                                );

                        Toast.makeText(getContext(),
                                "Level Up! You're now Level " + currentLevel,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void logWorkout(long xp, int durationMinutes, String type) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        java.util.Map<String, Object> entry = new java.util.HashMap<>();
        entry.put("userID", uid);
        entry.put("xp", xp);
        entry.put("duration", durationMinutes);
        entry.put("date", Timestamp.now());
        entry.put("workoutType", type);

        db.collection("workouts")
                .add(entry)
                .addOnSuccessListener(r -> Log.d("CardioFragment", "Workout logged"))
                .addOnFailureListener(e -> Log.e("CardioFragment", "Error logging workout", e));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(timerRunnable);
        stepHandler.removeCallbacks(stepRunnable);
    }
}
