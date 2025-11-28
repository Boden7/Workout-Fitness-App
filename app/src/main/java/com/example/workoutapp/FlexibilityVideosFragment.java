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

public class FlexibilityVideosFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_flexvideos, container, false);

        ImageButton backButton = view.findViewById(R.id.backButton);
        ImageView video1 = view.findViewById(R.id.video1);
        ImageView video2 = view.findViewById(R.id.video2);
        ImageView video3 = view.findViewById(R.id.video3);

        // Back to Flexibility page
        backButton.setOnClickListener(v ->
                getParentFragmentManager().popBackStack()
        );

        video1.setOnClickListener(v -> openVideo("video4"));
        video2.setOnClickListener(v -> openVideo("video5"));
        video3.setOnClickListener(v -> openVideo("video6"));

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
