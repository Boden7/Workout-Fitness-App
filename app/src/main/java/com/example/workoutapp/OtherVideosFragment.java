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

public class OtherVideosFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_othervideos, container, false);

        ImageButton backButton = view.findViewById(R.id.backButton);
        ImageView video10 = view.findViewById(R.id.video10);
        ImageView video11 = view.findViewById(R.id.video11);

        // Go back
        backButton.setOnClickListener(v ->
                getParentFragmentManager().popBackStack()
        );

        // Open videos
        video10.setOnClickListener(v -> openVideo("video10"));
        video11.setOnClickListener(v -> openVideo("video11"));

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
