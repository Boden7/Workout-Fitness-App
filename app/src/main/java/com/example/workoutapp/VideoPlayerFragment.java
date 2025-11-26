package com.example.workoutapp;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

        String videoName = getArguments().getString(ARG_VIDEO_NAME);
        Uri videoUri = Uri.parse("android.resource://" + getActivity().getPackageName() + "/raw/" + videoName);

        videoView.setVideoURI(videoUri);
        videoView.requestFocus();

        // Buttons
        btnPlay.setOnClickListener(v -> videoView.start());
        btnPause.setOnClickListener(v -> videoView.pause());
        btnStop.setOnClickListener(v -> {
            videoView.pause();
            videoView.seekTo(0);
        });

        // Count when video finishes
        videoView.setOnCompletionListener(mp -> incrementWatchCount(videoName));

        return view;
    }

    private void incrementWatchCount(String videoName) {
        Context context = getActivity();
        if (context == null) return;

        String key = "watch_count_" + videoName;
        int count = context.getSharedPreferences("video_prefs", Context.MODE_PRIVATE)
                .getInt(key, 0);

        count++;

        context.getSharedPreferences("video_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt(key, count)
                .apply();


        System.out.println(videoName + " watched fully " + count + " times");
    }
}
