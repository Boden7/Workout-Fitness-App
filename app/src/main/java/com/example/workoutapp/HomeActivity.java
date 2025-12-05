package com.example.workoutapp;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.home_toolbar);
        toolbar.setLogo(R.drawable.ic_streaks);

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setItemIconTintList(null);

        nav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (itemId == R.id.nav_workout) {
                selectedFragment = new WorkoutFragment();

                // ⭐ IMPORTANT: Give WorkoutFragment a tag
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment, "WORKOUT_FRAGMENT")
                        .commit();
                return true;
            } else if (itemId == R.id.nav_challenge) {
                selectedFragment = new ChallengeFragment();
            } else if (itemId == R.id.nav_leaderboard) {
                selectedFragment = new LeaderboardFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

            if (current instanceof StreakCalendar) {
                toolbar.setLogo(null);
                toolbar.setNavigationIcon(R.drawable.ic_back);
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            } else {
                toolbar.setNavigationIcon(null);
                toolbar.setLogo(R.drawable.ic_streaks);
            }
        });

        // Default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // Fire icon → streak calendar
        toolbar.setOnClickListener(v -> {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StreakCalendar())
                    .addToBackStack(null)
                    .commit();
        });
    }
}
