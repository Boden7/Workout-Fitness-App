package com.example.workoutapp;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChallengeFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Personal Challenges
    private ProgressBar streakProgress, lessonsProgress;
    private TextView streakProgressText, lessonsProgressText;
    private ImageView streakChest, lessonsChest;

    // Group Challenge
    private ProgressBar timeProgress;
    private TextView timeProgressText;
    private ImageView timeChest;
    private Button selectFriendsButton;
    private HorizontalScrollView participantsContainer;
    private LinearLayout participantsAvatarLayout;

    private List<String> friendIds = new ArrayList<>();
    private Map<String, String> friendDetails = new HashMap<>(); // Store friend ID -> name
    private List<String> selectedFriendIds = new ArrayList<>();
    private final int MAX_GROUP_PARTICIPANTS = 2;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_challenge, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initializeViews(view);
        loadAllChallengeData();

        selectFriendsButton.setOnClickListener(v -> showFriendSelectionDialog());

        return view;
    }

    private void initializeViews(View view) {
        streakProgress = view.findViewById(R.id.streak_progress);
        streakProgressText = view.findViewById(R.id.streak_progress_text);
        streakChest = view.findViewById(R.id.streak_chest);
        lessonsProgress = view.findViewById(R.id.lessons_progress);
        lessonsProgressText = view.findViewById(R.id.lessons_progress_text);
        lessonsChest = view.findViewById(R.id.lessons_chest);

        timeProgress = view.findViewById(R.id.time_progress);
        timeProgressText = view.findViewById(R.id.time_progress_text);
        timeChest = view.findViewById(R.id.time_chest);
        selectFriendsButton = view.findViewById(R.id.select_friends_button);
        participantsContainer = view.findViewById(R.id.participants_container);
        participantsAvatarLayout = view.findViewById(R.id.participants_avatar_layout);

        selectFriendsButton.setEnabled(false); // Disable until friend data is loaded
    }

    private void loadAllChallengeData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to see challenges", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                loadPersonalChallenges(documentSnapshot);

                if (documentSnapshot.get("friends") instanceof List) {
                    friendIds = (List<String>) documentSnapshot.get("friends");
                }

                // Chain async operations: fetch names -> then load group challenge state
                fetchFriendDetails(() -> {
                    if (documentSnapshot.contains("groupChallengeParticipants") && documentSnapshot.get("groupChallengeParticipants") instanceof List) {
                        selectedFriendIds = (List<String>) documentSnapshot.get("groupChallengeParticipants");
                    }
                    calculateGroupChallengeProgress();
                });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            selectFriendsButton.setEnabled(true); // Enable button even on failure
        });
    }

    private void fetchFriendDetails(Runnable onComplete) {
        if (friendIds == null || friendIds.isEmpty()) {
            selectFriendsButton.setEnabled(true);
            if (onComplete != null) onComplete.run();
            return;
        }

        friendDetails.clear();
        final int[] friendsProcessed = {0};
        for (String friendId : friendIds) {
            db.collection("users").document(friendId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    friendDetails.put(doc.getId(), doc.getString("email").split("@")[0]);
                }
                friendsProcessed[0]++;
                if (friendsProcessed[0] == friendIds.size()) {
                    selectFriendsButton.setEnabled(true);
                    if (onComplete != null) onComplete.run();
                }
            });
        }
    }

    private void loadPersonalChallenges(DocumentSnapshot userSnapshot) {
        Long streak = userSnapshot.getLong("streak");
        int currentStreak = streak != null ? streak.intValue() : 0;
        updateChallengeProgress(streakProgress, streakProgressText, streakChest, currentStreak, 1);

        Long totalWorkout = userSnapshot.getLong("totalWorkout");
        int currentTotalWorkout = totalWorkout != null ? totalWorkout.intValue() : 0;
        updateChallengeProgress(lessonsProgress, lessonsProgressText, lessonsChest, currentTotalWorkout, 5);
    }

    private void calculateGroupChallengeProgress() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        List<String> participantUids = new ArrayList<>(selectedFriendIds);
        participantUids.add(currentUser.getUid());

        final int[] totalTime = {0};
        final int[] usersProcessed = {0};
        int groupTimeTarget = 30;

        if (participantUids.isEmpty()) {
            updateChallengeProgress(timeProgress, timeProgressText, timeChest, 0, groupTimeTarget);
            updateGroupChallengeUI();
            return;
        }

        for (String uid : participantUids) {
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Long time = doc.getLong("totalTime"); // Using totalTime as requested
                    totalTime[0] += (time != null) ? time.intValue() : 0;
                }
                usersProcessed[0]++;
                if (usersProcessed[0] == participantUids.size()) {
                    updateChallengeProgress(timeProgress, timeProgressText, timeChest, totalTime[0], groupTimeTarget);
                }
            });
        }
        updateGroupChallengeUI();
    }

    private void showFriendSelectionDialog() {
        if (friendIds == null || friendIds.isEmpty()) {
            Toast.makeText(getContext(), "You have no friends to challenge!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] friendDisplayNames = new String[friendIds.size()];
        boolean[] checkedItems = new boolean[friendIds.size()];
        for (int i = 0; i < friendIds.size(); i++) {
            String id = friendIds.get(i);
            friendDisplayNames[i] = friendDetails.getOrDefault(id, "Loading...");
            if (selectedFriendIds.contains(id)) {
                checkedItems[i] = true;
            }
        }

        List<String> tempSelected = new ArrayList<>(selectedFriendIds);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select up to " + MAX_GROUP_PARTICIPANTS + " friends")
                .setMultiChoiceItems(friendDisplayNames, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        if (tempSelected.size() >= MAX_GROUP_PARTICIPANTS) {
                            Toast.makeText(getContext(), "You can only select up to " + MAX_GROUP_PARTICIPANTS + " friends.", Toast.LENGTH_SHORT).show();
                            ((AlertDialog) dialog).getListView().setItemChecked(which, false);
                        } else {
                            tempSelected.add(friendIds.get(which));
                        }
                    } else {
                        tempSelected.remove(friendIds.get(which));
                    }
                })
                .setPositiveButton("Confirm", (dialog, which) -> {
                    selectedFriendIds = new ArrayList<>(tempSelected);
                    updateGroupChallengeInFirebase();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateGroupChallengeInFirebase(){
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            DocumentReference userRef = db.collection("users").document(currentUser.getUid());
            userRef.update("groupChallengeParticipants", selectedFriendIds)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Group challenge updated!", Toast.LENGTH_SHORT).show();
                        calculateGroupChallengeProgress();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to update group challenge.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateGroupChallengeUI() {
        participantsAvatarLayout.removeAllViews();
        if (selectedFriendIds.isEmpty()) {
            selectFriendsButton.setVisibility(View.VISIBLE);
            participantsContainer.setVisibility(View.GONE);
        } else {
            selectFriendsButton.setVisibility(View.GONE);
            participantsContainer.setVisibility(View.VISIBLE);
            for (String friendId : selectedFriendIds) {
                addParticipantToLayout(friendId);
            }
        }
    }

    private void addParticipantToLayout(String userId) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View participantView = inflater.inflate(R.layout.item_participant, participantsAvatarLayout, false);

        CircleImageView avatar = participantView.findViewById(R.id.participant_avatar);
        TextView name = participantView.findViewById(R.id.participant_name);

        avatar.setImageResource(R.drawable.boy); // Placeholder image
        name.setText(friendDetails.getOrDefault(userId, "Friend")); // Name is now available

        participantsAvatarLayout.addView(participantView);
    }

    private void animateProgressBar(ProgressBar pb, int newProgress) {
        if (pb == null) return;
        ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), newProgress);
        animation.setDuration(800); // 800ms animation
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    private void updateChallengeProgress(ProgressBar progressBar, TextView progressText, ImageView chest, int currentProgress, int target) {
        if (progressBar == null) return;
        progressBar.setMax(target);
        int progress = Math.min(currentProgress, target);

        animateProgressBar(progressBar, progress);

        progressText.setText(progress + "/" + target);

        chest.setVisibility(View.VISIBLE); // Chest is always visible
        if (currentProgress >= target) {
            chest.setAlpha(1.0f); // Bright
        } else {
            chest.setAlpha(0.4f); // Dim
        }
    }
}
