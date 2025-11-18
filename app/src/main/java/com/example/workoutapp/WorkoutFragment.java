package com.example.workoutapp;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.ZoneId;

public class WorkoutFragment extends Fragment {

    private WorkoutViewModel viewModel;
    private TextView timerText;
    private Button startButton, pauseButton, stopButton, finishButton;
    private LinearLayout pauseStopContainer;
    private Handler handler = new Handler();

    // Timer Runnable
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel.isRunning) {
                long elapsedMillis = System.currentTimeMillis() - viewModel.startTime;
                int totalSeconds = (int) (elapsedMillis / 1000);
                viewModel.seconds = totalSeconds;

                int minutes = totalSeconds / 60;
                int secs = totalSeconds % 60;
                timerText.setText(String.format("%02d:%02d", minutes, secs));

                handler.postDelayed(this, 1000);
            }
        }
    };

    public WorkoutFragment() {
        super(R.layout.fragment_workout);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_workout, container, false);

        // ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(WorkoutViewModel.class);

        // UI references
        timerText = view.findViewById(R.id.timerText);
        startButton = view.findViewById(R.id.startButton);
        pauseButton = view.findViewById(R.id.pauseButton);
        stopButton = view.findViewById(R.id.stopButton);
        pauseStopContainer = view.findViewById(R.id.pauseStopContainer);
        finishButton = view.findViewById(R.id.btn_finish_workout);

        LinearLayout categoryCardio = view.findViewById(R.id.categoryCardio);
        LinearLayout categoryFlexibility = view.findViewById(R.id.categoryFlexibility);
        LinearLayout categoryStrength = view.findViewById(R.id.categoryStrength);
        LinearLayout categoryHIIT = view.findViewById(R.id.categoryHIIT);

        // Restore timer display and buttons
        int minutes = viewModel.seconds / 60;
        int secs = viewModel.seconds % 60;
        timerText.setText(String.format("%02d:%02d", minutes, secs));

        if (viewModel.isRunning) {
            startButton.setVisibility(View.GONE);
            pauseStopContainer.setVisibility(View.VISIBLE);
            handler.post(runnable);
        } else if (viewModel.seconds > 0) {
            startButton.setText("Resume");
            startButton.setVisibility(View.VISIBLE);
            pauseStopContainer.setVisibility(View.GONE);
        } else {
            startButton.setText("Start");
            startButton.setVisibility(View.VISIBLE);
            pauseStopContainer.setVisibility(View.GONE);
        }

        // Timer buttons
        startButton.setOnClickListener(v -> {
            viewModel.isRunning = true;
            if (viewModel.startTime == 0) {
                viewModel.startTime = System.currentTimeMillis();
            } else {
                viewModel.startTime = System.currentTimeMillis() - viewModel.seconds * 1000;
            }
            startButton.setVisibility(View.GONE);
            pauseStopContainer.setVisibility(View.VISIBLE);
            handler.post(runnable);
        });

        pauseButton.setOnClickListener(v -> {
            viewModel.isRunning = false;
            startButton.setText("Resume");
            startButton.setVisibility(View.VISIBLE);
            pauseStopContainer.setVisibility(View.GONE);
        });

        stopButton.setOnClickListener(v -> {
            viewModel.isRunning = false;
            viewModel.seconds = 0;
            viewModel.startTime = 0;
            timerText.setText("00:00");
            startButton.setText("Start");
            startButton.setVisibility(View.VISIBLE);
            pauseStopContainer.setVisibility(View.GONE);
        });

        Button finishBtn = view.findViewById(R.id.btn_finish_workout);
        finishBtn.setOnClickListener(v -> updateWorkoutAndStreak());

        // Category navigation
        categoryCardio.setOnClickListener(v -> navigateToCategory(new CardioFragment()));
        categoryFlexibility.setOnClickListener(v -> navigateToCategory(new FlexibilityFragment()));
        categoryStrength.setOnClickListener(v -> navigateToCategory(new StrengthFragment()));
        categoryHIIT.setOnClickListener(v -> navigateToCategory(new OtherActivitiesFragment()));

        return view;
    }

    private void navigateToCategory(Fragment fragment) {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void updateWorkoutAndStreak() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(doc -> {
            Timestamp lastTS = doc.getTimestamp("lastWorkoutDate");
            Long streak = doc.getLong("streak");
            if (streak == null) streak = 0L;

            LocalDate today = LocalDate.now();

            if (lastTS == null) {
                streak = 1L;
            } else {
                LocalDate lastDate = lastTS.toDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                if (today.isEqual(lastDate)) {
                    // same day → no change
                } else if (today.minusDays(1).isEqual(lastDate)) {
                    streak += 1;
                } else {
                    streak = 1L;
                    ;
                }
            }

            userRef.update(
                    "streak", streak,
                    "lastWorkoutDate", Timestamp.now()
            );
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(runnable);
    }
}
