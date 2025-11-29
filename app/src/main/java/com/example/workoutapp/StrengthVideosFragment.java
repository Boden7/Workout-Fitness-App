package com.example.workoutapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StrengthVideosFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_strenvideos, container, false);

        ImageButton backButton = view.findViewById(R.id.backButton);
        ImageView video7 = view.findViewById(R.id.video7);
        ImageView video8 = view.findViewById(R.id.video8);
        ImageView video9 = view.findViewById(R.id.video9);

        // Back to Strength page
        backButton.setOnClickListener(v ->
                getParentFragmentManager().popBackStack()
        );

        video7.setOnClickListener(v -> openVideo("video7"));
        video8.setOnClickListener(v -> openVideo("video8"));
        video9.setOnClickListener(v -> openVideo("video9"));

        return view;
    }

    private void openVideo(String videoName) {
        VideoPlayerFragment fragment = VideoPlayerFragment.newInstance(videoName);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
