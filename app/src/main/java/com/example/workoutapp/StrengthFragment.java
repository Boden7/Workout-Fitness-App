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

public class StrengthFragment extends Fragment {

    private TextView videosWatchedCounter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_strength, container, false);

        ImageButton backButton = view.findViewById(R.id.backButton);
        TextView seeAll = view.findViewById(R.id.seeAll);
        videosWatchedCounter = view.findViewById(R.id.videosWatchedCounter);

        // Back button
        backButton.setOnClickListener(v ->
                getParentFragmentManager().popBackStack()
        );

        // Open Strength Videos list
        seeAll.setOnClickListener(v -> {
            Fragment strengthVideosFragment = new StrengthVideosFragment();
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, strengthVideosFragment)
                    .addToBackStack(null)
                    .commit();
        });

        updateVideosWatched();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateVideosWatched();
    }

    private void updateVideosWatched() {
        if (getActivity() == null) return;

        SharedPreferences prefs =
                getActivity().getSharedPreferences("video_prefs", Context.MODE_PRIVATE);

        String[] strengthVideos = {"video7", "video8", "video9"};

        int uniqueWatched = 0;
        for (String name : strengthVideos) {
            int count = prefs.getInt("watch_count_" + name, 0);
            if (count > 0) uniqueWatched++;
        }

        videosWatchedCounter.setText("Videos Watched: " + uniqueWatched);
    }
}

