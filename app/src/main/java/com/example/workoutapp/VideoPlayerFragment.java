package com.example.workoutapp;

import android.widget.TextView;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_video_player, container, false);

        VideoView videoView = view.findViewById(R.id.videoView);
        Button btnPlay = view.findViewById(R.id.btnPlay);
        Button btnPause = view.findViewById(R.id.btnPause);
        Button btnStop = view.findViewById(R.id.btnStop);
        ImageButton backButton = view.findViewById(R.id.backButton);
        TextView videoTitle = view.findViewById(R.id.videoTitle);

        // Back button
        backButton.setOnClickListener(v ->
                getParentFragmentManager().popBackStack()
        );

        String videoName = getArguments().getString(ARG_VIDEO_NAME);

        // Set the title
        videoTitle.setText(getVideoTitle(videoName));

        // Load the video
        Uri videoUri = Uri.parse("android.resource://" +
                requireActivity().getPackageName() + "/raw/" + videoName);

        videoView.setVideoURI(videoUri);
        videoView.requestFocus();

        btnPlay.setOnClickListener(v -> videoView.start());
        btnPause.setOnClickListener(v -> videoView.pause());
        btnStop.setOnClickListener(v -> {
            videoView.pause();
            videoView.seekTo(0);
        });

        videoView.setOnCompletionListener(mp ->
                incrementWatchCount(videoName)
        );

        return view;
    }

    private String getVideoTitle(String videoName) {
        switch (videoName) {
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


            default: return "Workout Video";
        }
    }

    private void incrementWatchCount(String videoName) {
        Context context = getActivity();
        if (context == null) return;

        String key = "watch_count_" + videoName;
        int count = context.getSharedPreferences("video_prefs", Context.MODE_PRIVATE)
                .getInt(key, 0);

        context.getSharedPreferences("video_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt(key, count + 1)
                .apply();

        System.out.println(videoName + " watched fully " + (count + 1) + " times");
    }
}
