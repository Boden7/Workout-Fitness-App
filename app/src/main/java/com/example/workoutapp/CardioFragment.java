package com.example.workoutapp;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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

            // Calculate total minutes before resetting
            int elapsedMinutes = viewModel.seconds / 60;
            minutesText.setText(String.valueOf(elapsedMinutes));

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(timerRunnable);
    }
}
