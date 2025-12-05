package com.example.workoutapp;

import android.widget.TextView;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class VideoPlayerFragment extends Fragment {

    private static final String ARG_VIDEO_NAME = "video_name";

    public static VideoPlayerFragment newInstance(String videoName) {
        VideoPlayerFragment fragment = new VideoPlayerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VIDEO_NAME, videoName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(R.layout.fragment_video_player, container, false);

        VideoView videoView = view.findViewById(R.id.videoView);
        Button btnPlay = view.findViewById(R.id.btnPlay);
        Button btnPause = view.findViewById(R.id.btnPause);
        Button btnStop = view.findViewById(R.id.btnStop);
        ImageButton backButton = view.findViewById(R.id.backButton);
        TextView videoTitle = view.findViewById(R.id.videoTitle);

        backButton.setOnClickListener(v -> {
            pauseLinkedTimer();
            getParentFragmentManager().popBackStack();
        });

        String videoName = getArguments().getString(ARG_VIDEO_NAME);
        videoTitle.setText(getVideoTitle(videoName));

        Uri videoUri = Uri.parse("android.resource://" +
                requireActivity().getPackageName() + "/raw/" + videoName);

        videoView.setVideoURI(videoUri);
        videoView.requestFocus();

        // === PLAY BUTTON ===
        btnPlay.setOnClickListener(v -> {
            videoView.start();
            startLinkedTimer();
        });

        // === PAUSE BUTTON ===
        btnPause.setOnClickListener(v -> {
            videoView.pause();
            pauseLinkedTimer();
        });

        // === STOP BUTTON ===
        btnStop.setOnClickListener(v -> {
            videoView.pause();
            videoView.seekTo(0);
            stopLinkedTimer();
            Toast.makeText(requireContext(),
                    "Video stopped — workout timer has been reset.",
                    Toast.LENGTH_SHORT).show();
        });

        // When video finishes
        videoView.setOnCompletionListener(mp -> {
            incrementWatchCount(videoName);
            stopLinkedTimer();
        });

        return view;
    }

    // ----------------------------
    // Find WorkoutFragment by tag
    // ----------------------------
    private WorkoutFragment getWorkoutFragment() {
        return (WorkoutFragment) getActivity()
                .getSupportFragmentManager()
                .findFragmentByTag("WORKOUT_FRAGMENT");
    }

    private void startLinkedTimer() {
        WorkoutFragment w = getWorkoutFragment();
        if (w != null) w.startTimerFromVideo();
    }

    private void pauseLinkedTimer() {
        WorkoutFragment w = getWorkoutFragment();
        if (w != null) w.pauseTimerFromVideo();
    }

    private void stopLinkedTimer() {
        WorkoutFragment w = getWorkoutFragment();
        if (w != null) w.stopTimerFromVideo();
    }

    // ----------------------------
    // Pause timer when leaving fragment
    // ----------------------------
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pauseLinkedTimer(); // <-- IMPORTANT FIX
    }

    // ----------------------------
    // Title lookup
    // ----------------------------
    private String getVideoTitle(String name) {
        switch (name) {
            case "video1": return "Best Ever 3 Minute Cardio Workout";
            case "video2": return "High-Intensity Cardio Burn";
            case "video3": return "10 Minutes At Home Cardio";
            case "video4": return "How To STRETCH Your Upper Body in 3 Minutes";
            case "video5": return "3 Minutes Full Body Stretch";
            case "video6": return "3 Minutes Neck Fix";
            case "video7": return "SHORT HIIT WORKOUT";
            case "video8": return "STRONG CORE 3-Minute Workout Challenge";
            case "video9": return "Bowflex® Bodyweight Workout";
            case "video10": return "3 Minute Ab Workout";
            case "video11": return "3 MINUTE EASY TO FOLLOW DANCE WORKOUT";
        }
        return "Workout Video";
    }

    // ----------------------------
    // Watch count + Firestore
    // ----------------------------
    private void incrementWatchCount(String videoName) {
        Context context = getActivity();
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences("video_prefs", Context.MODE_PRIVATE);

        String key = "watch_count_" + videoName;
        int count = prefs.getInt(key, 0);

        prefs.edit().putInt(key, count + 1).apply();

        updateTotalUniqueVideosWatched(context);
    }

    private void updateTotalUniqueVideosWatched(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("video_prefs", Context.MODE_PRIVATE);

        String[] allVideos = {
                "video1","video2","video3",
                "video4","video5","video6",
                "video7","video8","video9",
                "video10","video11"
        };

        int total = 0;

        for (String v : allVideos) {
            if (prefs.getInt("watch_count_" + v, 0) > 0) total++;
        }

        pushTotalToFirestore(total);
    }

    private void pushTotalToFirestore(int totalWatched) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference ref = db.collection("course").document(uid);

        Map<String, Object> data = new HashMap<>();
        data.put("totalVideosWatched", totalWatched);
        data.put("lastUpdated", Timestamp.now());

        ref.set(data, SetOptions.merge());
    }
}

