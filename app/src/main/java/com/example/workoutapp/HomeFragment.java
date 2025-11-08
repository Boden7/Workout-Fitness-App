package com.example.workoutapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class HomeFragment extends Fragment {
    private TextView tvStreaks, tvXP, tvBadges, tvTime, tvLevel, tvCourses;
    private FirebaseAuth Auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        tvStreaks = view.findViewById(R.id.tvStreaks);
        tvXP      = view.findViewById(R.id.tvXP);
        tvBadges  = view.findViewById(R.id.tvBadges);
        tvTime    = view.findViewById(R.id.tvTime);
        tvLevel    = view.findViewById(R.id.tvLevel);
        tvCourses = view.findViewById(R.id.tvCourses);

        db   = FirebaseFirestore.getInstance();
        Auth = FirebaseAuth.getInstance();

        loadUserData();

        return view;
    }

    private void loadUserData() {
        if (Auth.getCurrentUser() == null) {
            tvStreaks.setText("-");
            tvXP.setText("-");
            tvBadges.setText("-");
            tvTime.setText("-");
            tvLevel.setText("-");
            tvCourses.setText("-");
            return;
        }

        String userId = Auth.getCurrentUser().getUid();
        DocumentReference docRef = db.collection("users").document(userId);


        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {

                Long streaks = documentSnapshot.getLong("streak");
                Long xp      = documentSnapshot.getLong("totalXP");
                Long time    = documentSnapshot.getLong("totalTime");
                Long level    = documentSnapshot.getLong("level");
                Long courses = documentSnapshot.getLong("course");

                List<Object> badgesList = (List<Object>) documentSnapshot.get("badges");
                int badgeCount = 0;
                if (badgesList != null) {
                    badgeCount = badgesList.size();
                }


                tvStreaks.setText((streaks != null ? String.valueOf(streaks) : "-"));
                tvXP.setText((xp != null ? String.valueOf(xp) : "-"));
                tvBadges.setText( (badgesList != null ? String.valueOf(badgeCount) : "-"));
                tvTime.setText((time != null ? String.valueOf(time) : "-"));
                tvLevel.setText((level != null ? String.valueOf(level) : "-"));
                tvCourses.setText((courses != null ? String.valueOf(courses) : "-"));
            } else {
                tvStreaks.setText("-"); tvXP.setText("-"); tvBadges.setText("-");
                tvTime.setText("-");    tvLevel.setText("-"); tvCourses.setText("-");
            }
        }).addOnFailureListener(e -> {
            tvStreaks.setText("-"); tvXP.setText("-"); tvBadges.setText("-");
            tvTime.setText("-");    tvLevel.setText("-"); tvCourses.setText("-");
            // Log.e("HomeFragment", "Firestore read failed", e);
        });
    }

}

