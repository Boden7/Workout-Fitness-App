package com.example.workoutapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FlexibilityFragment extends Fragment {

    private TextView videosWatchedCounter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate your Flexibility page
        View view = inflater.inflate(R.layout.fragment_flexibility, container, false);

        ImageButton backButton = view.findViewById(R.id.backButton);
        TextView seeAll = view.findViewById(R.id.seeAll);
        videosWatchedCounter = view.findViewById(R.id.videosWatchedCounter);

        // Back button behavior
        backButton.setOnClickListener(v ->
                getParentFragmentManager().popBackStack()
        );

        // Open Flexibility Videos list
        seeAll.setOnClickListener(v -> {
            Fragment flexVideosFragment = new FlexibilityVideosFragment();
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, flexVideosFragment)
                    .addToBackStack(null)
                    .commit();
        });

        updateVideosWatched();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateVideosWatched(); // updates when returning from video player
    }

    private void updateVideosWatched() {
        if (getActivity() == null) return;

        SharedPreferences prefs = getActivity()
                .getSharedPreferences("video_prefs", Context.MODE_PRIVATE);

        String[] allVideos = {
                "video1","video2","video3",
                "video4","video5","video6",
                "video7","video8","video9",
                "video10","video11"
        };
        int uniqueWatched = 0;
        for (String name : allVideos) {
            int count = prefs.getInt("watch_count_" + name, 0);
            if (count > 0) uniqueWatched++;
        }

        videosWatchedCounter.setText("Total Videos Watched: " + uniqueWatched);
    }
}

//video4 https://www.youtube.com/watch?v=Jv7cs7LWpmA
//video5 https://www.youtube.com/watch?v=GjnEzVgL9y8
//video6 https://www.youtube.com/watch?v=E-Ax65ty7ho
