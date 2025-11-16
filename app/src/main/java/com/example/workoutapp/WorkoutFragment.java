package com.example.workoutapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.ZoneId;

public class WorkoutFragment extends Fragment {

    public WorkoutFragment() {
        super(R.layout.fragment_workout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button finishBtn = view.findViewById(R.id.btn_finish_workout);

        finishBtn.setOnClickListener(v -> updateWorkoutAndStreak());
    }

    private void updateWorkoutAndStreak() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(doc -> {
            Timestamp lastTS = doc.getTimestamp("lastWorkoutDate");
            long streak = doc.getLong("streak");

            LocalDate today = LocalDate.now();

            if (lastTS == null) {
                // first workout ever
                streak = 1;

            } else {
                LocalDate lastDate = lastTS.toDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                if (today.isEqual(lastDate)) {
                    // same day workout → streak unchanged

                } else if (today.minusDays(1).isEqual(lastDate)) {
                    // consecutive day workout
                    streak += 1;

                } else {
                    // streak broken → reset
                    streak = 1;
                }
            }

            // Update user document
            userRef.update(
                    "streak", streak,
                    "lastWorkoutDate", Timestamp.now()
            );
        });
    }
}
