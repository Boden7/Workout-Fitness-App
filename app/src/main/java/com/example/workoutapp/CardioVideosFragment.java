package com.example.workoutapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CardioVideosFragment extends Fragment {

    public CardioVideosFragment() {
        super(R.layout.fragment_cardiovideos);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        ImageView video1 = view.findViewById(R.id.video1);
        ImageView video2 = view.findViewById(R.id.video2);
        ImageView video3 = view.findViewById(R.id.video3);

        video1.setOnClickListener(v -> openVideo("video1"));
        video2.setOnClickListener(v -> openVideo("video2"));
        video3.setOnClickListener(v -> openVideo("video3"));

        return view;
    }

    // Method must be outside onCreateView
    private void openVideo(String videoName) {
        Fragment fragment = VideoPlayerFragment.newInstance(videoName);
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment) // your fragment container in activity
                .addToBackStack(null)
                .commit();
    }
}
