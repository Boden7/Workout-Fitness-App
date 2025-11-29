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

        // BACK BUTTON
        ImageView backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        // Thumbnails
        ImageView video1 = view.findViewById(R.id.video1);
        ImageView video2 = view.findViewById(R.id.video2);
        ImageView video3 = view.findViewById(R.id.video3);

        video1.setOnClickListener(v -> openVideo("video1"));
        video2.setOnClickListener(v -> openVideo("video2"));
        video3.setOnClickListener(v -> openVideo("video3"));

        return view;
    }

    private void openVideo(String videoName) {
        Fragment fragment = VideoPlayerFragment.newInstance(videoName);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
//video7:https://www.youtube.com/watch?v=5WkW5DQUdEM
//video8:https://www.youtube.com/watch?v=N1n_c4N0p-0
//video9: https://www.youtube.com/watch?v=ynUw0YsrmSg
//video10:https://www.youtube.com/watch?v=K1oPPW72-QY
//video11:https://www.youtube.com/watch?v=XR51UhfcKoU
//video4: https://www.youtube.com/watch?v=Jv7cs7LWpmA&t=2s
//video5: https://www.youtube.com/watch?v=q2NZyW5EP5A
//video6: https://www.youtube.com/watch?v=PqqJBaE4srs