package com.example.workoutapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;

public class StreakCalendar extends Fragment {

    private GridLayout gridCalendar;
    private TextView tvMonth, btnPrev, btnNext;
    private LocalDate selectedMonth;
    private TextView tvStreakCount;

    public StreakCalendar() {
        super(R.layout.fragment_streak_calendar);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gridCalendar = view.findViewById(R.id.gridCalendar);
        tvMonth = view.findViewById(R.id.tvMonth);
        btnPrev = view.findViewById(R.id.btnPrev);
        btnNext = view.findViewById(R.id.btnNext);

        // Start from this month
        selectedMonth = LocalDate.now().withDayOfMonth(1);
        tvStreakCount = view.findViewById(R.id.tvStreakCount);
        loadStreakFromFirestore();

        btnPrev.setOnClickListener(v -> {
            selectedMonth = selectedMonth.minusMonths(1);
            loadStreakFromFirestore();
        });

        btnNext.setOnClickListener(v -> {
            selectedMonth = selectedMonth.plusMonths(1);
            loadStreakFromFirestore();
        });
    }

    private void loadStreakFromFirestore() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    Long streak = doc.getLong("streak");
                    Timestamp lastTS = doc.getTimestamp("lastWorkoutDate");


                    if (streak != null) {
                        tvStreakCount.setText( streak + " days");
                    } else {
                        tvStreakCount.setText("0 days");
                    }


                    HashSet<LocalDate> streakDays = new HashSet<>();

                    if (streak != null && lastTS != null) {
                        LocalDate lastDate = lastTS.toDate()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();

                        for (int i = 0; i < streak; i++) {
                            streakDays.add(lastDate.minusDays(i));
                        }
                    }


                    buildCalendar(streakDays);
                });
    }


    private void buildCalendar(HashSet<LocalDate> streakDays) {

        gridCalendar.removeAllViews();

        LocalDate firstDay = selectedMonth;
        int offset = firstDay.getDayOfWeek().getValue() % 7;
        int daysInMonth = firstDay.lengthOfMonth();

        String title = selectedMonth.getMonth().toString() + " " + selectedMonth.getYear();
        tvMonth.setText(title);

        for (int i = 0; i < 42; i++) {
            TextView tv = new TextView(getContext());
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(16);

            if (i >= offset && i < offset + daysInMonth) {
                int dayNum = i - offset + 1;
                LocalDate date = selectedMonth.withDayOfMonth(dayNum);

                tv.setText(String.valueOf(dayNum));

                if (streakDays.contains(date)) {
                    tv.setBackgroundResource(R.drawable.bg_streak_day);
                    tv.setTextColor(Color.WHITE);
                }

            } else {
                tv.setText("");
            }

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 120;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(6, 6, 6, 6);

            gridCalendar.addView(tv, params);
        }
    }
}
