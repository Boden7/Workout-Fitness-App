package com.example.workoutapp;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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

public class WorkoutFragment extends Fragment {

    private WorkoutViewModel viewModel;
    private TextView timerText;
    private Button startButton, pauseButton, stopButton;
    private LinearLayout pauseStopContainer;
    private Handler handler = new Handler();

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel.isRunning) {
                long elapsedMillis = System.currentTimeMillis() - viewModel.startTime;
                viewModel.seconds = (int) (elapsedMillis / 1000);

                int min = viewModel.seconds / 60;
                int sec = viewModel.seconds % 60;
                timerText.setText(String.format("%02d:%02d", min, sec));

                handler.postDelayed(this, 1000);
            }
        }
    };

    public WorkoutFragment() {
        super(R.layout.fragment_workout);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_workout, container, false);

        // ViewModel shared across fragments
        viewModel = new ViewModelProvider(requireActivity()).get(WorkoutViewModel.class);

        timerText = view.findViewById(R.id.timerText);
        startButton = view.findViewById(R.id.startButton);
        pauseButton = view.findViewById(R.id.pauseButton);
        stopButton = view.findViewById(R.id.stopButton);
        pauseStopContainer = view.findViewById(R.id.pauseStopContainer);

        // Restore timer text
        updateTimerText();

        // Restore button UI state
        restoreTimerUIState();

        // --------------------------
        // START / RESUME BUTTON
        // --------------------------
        startButton.setOnClickListener(v -> {
            startTimerInternal();
            startButton.setVisibility(View.GONE);
            pauseStopContainer.setVisibility(View.VISIBLE);
            pauseButton.setVisibility(View.VISIBLE);
        });

        // --------------------------
        // PAUSE BUTTON
        // --------------------------
        pauseButton.setOnClickListener(v -> {
            pauseTimerInternal();

            // Show **only resume**
            startButton.setText("Resume");
            startButton.setVisibility(View.VISIBLE);
            pauseStopContainer.setVisibility(View.GONE); // hide Pause + Stop completely
        });

        // --------------------------
        // STOP BUTTON
        // --------------------------
        stopButton.setOnClickListener(v -> {
            completeAndResetWorkout();
            resetTimerUI();
        });

        // Category navigation (unchanged)
        view.findViewById(R.id.categoryCardio)
                .setOnClickListener(v -> navigate(new CardioFragment()));

        view.findViewById(R.id.categoryFlexibility)
                .setOnClickListener(v -> navigate(new FlexibilityFragment()));

        view.findViewById(R.id.categoryStrength)
                .setOnClickListener(v -> navigate(new StrengthFragment()));

        view.findViewById(R.id.categoryHIIT)
                .setOnClickListener(v -> navigate(new OtherActivitiesFragment()));

        return view;
    }

    private void navigate(Fragment f) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }

    // --------------------------
    // INTERNAL TIMER LOGIC
    // --------------------------
    private void startTimerInternal() {
        if (!viewModel.isRunning) {
            viewModel.isRunning = true;
            viewModel.startTime = System.currentTimeMillis() - viewModel.seconds * 1000L;
            handler.post(runnable);
        }
    }

    private void pauseTimerInternal() {
        viewModel.isRunning = false;
    }

    private void resetTimerValues() {
        viewModel.isRunning = false;
        viewModel.seconds = 0;
        viewModel.startTime = 0;
        timerText.setText("00:00");
    }

    private void updateTimerText() {
        int min = viewModel.seconds / 60;
        int sec = viewModel.seconds % 60;
        timerText.setText(String.format("%02d:%02d", min, sec));
    }

    private void restoreTimerUIState() {
        if (viewModel.isRunning) {
            startButton.setVisibility(View.GONE);
            pauseStopContainer.setVisibility(View.VISIBLE);
            handler.post(runnable);
        } else if (viewModel.seconds > 0) {
            // paused
            startButton.setText("Resume");
            startButton.setVisibility(View.VISIBLE);
            pauseStopContainer.setVisibility(View.GONE);
        } else {
            // never started
            startButton.setText("Start");
            startButton.setVisibility(View.VISIBLE);
            pauseStopContainer.setVisibility(View.GONE);
        }
    }

    private void resetTimerUI() {
        startButton.setText("Start");
        startButton.setVisibility(View.VISIBLE);
        pauseStopContainer.setVisibility(View.GONE);
        pauseButton.setVisibility(View.GONE);
    }

    // --------------------------
    // METHODS FOR VIDEO PLAYER
    // --------------------------
    public void startTimerFromVideo() {
        startTimerInternal();
    }

    public void pauseTimerFromVideo() {
        pauseTimerInternal();

        startButton.setText("Resume");
        startButton.setVisibility(View.VISIBLE);
        pauseStopContainer.setVisibility(View.GONE);
    }

    public void stopTimerFromVideo() {
        completeAndResetWorkout();

        startButton.setText("Start");
        startButton.setVisibility(View.VISIBLE);

        pauseStopContainer.setVisibility(View.GONE);
        pauseButton.setVisibility(View.GONE);

        timerText.setText("00:00");
    }

    private void completeAndResetWorkout() {
        int mins = viewModel.seconds / 60;

        if (mins > 0) {
            long xpToAdd = mins * 100L;

            updateUserStats(xpToAdd, mins);
            logWorkout(xpToAdd, mins, "general");

            if (mins >= 10) updateWorkoutAndStreak();
        }

        resetTimerValues();
    }

    private void updateWorkoutAndStreak() { /* unchanged */ }
    private void updateUserStats(long xpToAdd, int minutesToAdd) { /* unchanged */ }
    private void checkForLevelUp(String uid, long addedXP) { /* unchanged */ }
    private void logWorkout(long xp, int durationMinutes, String type) { /* unchanged */ }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(runnable);
    }
}
