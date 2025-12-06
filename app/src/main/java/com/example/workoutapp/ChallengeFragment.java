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

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
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
    private TextView timeChallengeTitle; // Dynamic Title
    private ProgressBar timeProgress;
    private TextView timeProgressText;
    private ImageView timeChest;
    private Button selectFriendsButton;
    private HorizontalScrollView participantsContainer;
    private LinearLayout participantsAvatarLayout;

    // Data
    private List<String> friendIds = new ArrayList<>();
    private Map<String, String> friendDetails = new HashMap<>();
    private List<String> selectedFriendIds = new ArrayList<>();
    private final int MAX_GROUP_PARTICIPANTS = 2;
    private Map<String, Object> groupTimeSnapshot = new HashMap<>();

    // Reward System Data
    private Map<String, Object> challengeStatus = new HashMap<>();
    public static final String STREAK_CHALLENGE = "streak";
    public static final String LESSONS_CHALLENGE = "lessons";
    public static final String GROUP_CHALLENGE = "group";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_challenge, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initializeViews(view);
        loadAllChallengeData();

        return view;
    }

    private void initializeViews(View view) {
        streakProgress = view.findViewById(R.id.streak_progress);
        streakProgressText = view.findViewById(R.id.streak_progress_text);
        streakChest = view.findViewById(R.id.streak_chest);
        lessonsProgress = view.findViewById(R.id.lessons_progress);
        lessonsProgressText = view.findViewById(R.id.lessons_progress_text);
        lessonsChest = view.findViewById(R.id.lessons_chest);

        timeChallengeTitle = view.findViewById(R.id.time_challenge_title);
        timeProgress = view.findViewById(R.id.time_progress);
        timeProgressText = view.findViewById(R.id.time_progress_text);
        timeChest = view.findViewById(R.id.time_chest);
        selectFriendsButton = view.findViewById(R.id.select_friends_button);
        participantsContainer = view.findViewById(R.id.participants_container);
        participantsAvatarLayout = view.findViewById(R.id.participants_avatar_layout);

        selectFriendsButton.setOnClickListener(v -> showFriendSelectionDialog());
        selectFriendsButton.setEnabled(false);
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
                if (documentSnapshot.contains("challenges")) {
                    challengeStatus = (Map<String, Object>) documentSnapshot.get("challenges");
                }
                if (documentSnapshot.contains("groupChallengeTimeSnapshot")) {
                    groupTimeSnapshot = (Map<String, Object>) documentSnapshot.get("groupChallengeTimeSnapshot");
                }

                loadPersonalChallenges(documentSnapshot);

                if (documentSnapshot.get("friends") instanceof List) {
                    friendIds = (List<String>) documentSnapshot.get("friends");
                }

                fetchFriendDetails(() -> {
                    if (documentSnapshot.contains("groupChallengeParticipants")) {
                        selectedFriendIds = (List<String>) documentSnapshot.get("groupChallengeParticipants");
                    }
                    calculateGroupChallengeProgress();
                });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            selectFriendsButton.setEnabled(true);
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

        int lastClaimedStreakLevel = 0;
        if (challengeStatus.containsKey(STREAK_CHALLENGE) && challengeStatus.get(STREAK_CHALLENGE) instanceof Map) {
            Map<String, Object> status = (Map<String, Object>) challengeStatus.get(STREAK_CHALLENGE);
            if (status.containsKey("lastClaimedLevel")) {
                lastClaimedStreakLevel = ((Long) status.get("lastClaimedLevel")).intValue();
            }
        }
        int newStreakLevel = lastClaimedStreakLevel + 1;
        updateChallengeUI(streakProgress, streakProgressText, streakChest, currentStreak, newStreakLevel, STREAK_CHALLENGE, 500, "Streak Level " + newStreakLevel, newStreakLevel);

        Long totalWorkout = userSnapshot.getLong("totalWorkout");
        int currentTotalWorkout = totalWorkout != null ? totalWorkout.intValue() : 0;
        updateChallengeUI(lessonsProgress, lessonsProgressText, lessonsChest, currentTotalWorkout, 5, LESSONS_CHALLENGE, 1000, "Lesson Learner", 1);
    }

    private void calculateGroupChallengeProgress() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        int lastClaimedGroupLevel = 0;
        if (challengeStatus.containsKey(GROUP_CHALLENGE) && challengeStatus.get(GROUP_CHALLENGE) instanceof Map) {
            Map<String, Object> status = (Map<String, Object>) challengeStatus.get(GROUP_CHALLENGE);
            if (status.containsKey("lastClaimedLevel")) {
                lastClaimedGroupLevel = ((Long) status.get("lastClaimedLevel")).intValue();
            }
        }
        int newGroupLevel = lastClaimedGroupLevel + 1;
        int groupTimeTarget = 30 + (lastClaimedGroupLevel * 15);
        String groupBadgeName = "Team Player Level " + newGroupLevel;
        if(timeChallengeTitle != null) {
            timeChallengeTitle.setText("Workout " + groupTimeTarget + " mins with friends");
        }

        if (selectedFriendIds.isEmpty()) {
            updateChallengeUI(timeProgress, timeProgressText, timeChest, 0, groupTimeTarget, GROUP_CHALLENGE, 1500, groupBadgeName, newGroupLevel);
            updateGroupChallengeUI();
            return;
        }

        List<String> participantUids = new ArrayList<>(selectedFriendIds);
        participantUids.add(currentUser.getUid());

        final long[] totalCurrentTime = {0};
        final int[] usersProcessed = {0};

        for (String uid : participantUids) {
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Long time = doc.getLong("totalTime");
                    totalCurrentTime[0] += (time != null) ? time : 0;
                }
                usersProcessed[0]++;
                if (usersProcessed[0] == participantUids.size()) {
                    long totalSnapshotTime = 0;
                    for (String pUid : participantUids) {
                        if (groupTimeSnapshot.containsKey(pUid)) {
                            totalSnapshotTime += ((Long) groupTimeSnapshot.get(pUid));
                        }
                    }
                    long progressDelta = totalCurrentTime[0] - totalSnapshotTime;
                    updateChallengeUI(timeProgress, timeProgressText, timeChest, (int) progressDelta, groupTimeTarget, GROUP_CHALLENGE, 1500, groupBadgeName, newGroupLevel);
                }
            });
        }
        updateGroupChallengeUI();
    }

    private void updateChallengeUI(ProgressBar pb, TextView text, ImageView chest, int current, int target, String key, int xp, String badge, int level) {
        pb.setMax(target);
        int progress = Math.min(current, target);
        animateProgressBar(pb, progress);
        text.setText(progress + "/" + target);

        boolean isCompleted = current >= target;
        boolean isClaimedToday = false;

        if (challengeStatus.containsKey(key) && challengeStatus.get(key) instanceof Map) {
            Map<String, Object> status = (Map<String, Object>) challengeStatus.get(key);
            if (status.containsKey("lastClaimedTimestamp")) {
                Timestamp ts = (Timestamp) status.get("lastClaimedTimestamp");
                if (ts != null && isToday(ts.toDate().getTime())) {
                     int lastClaimedLevel = status.containsKey("lastClaimedLevel") ? ((Long) status.get("lastClaimedLevel")).intValue() : 0;
                     if (key.equals(STREAK_CHALLENGE) || key.equals(GROUP_CHALLENGE)){
                        if(lastClaimedLevel >= level){
                            isClaimedToday = true;
                        }
                     } else {
                         isClaimedToday = true;
                     }
                }
            }
        }

        chest.setOnClickListener(null);
        if (isCompleted) {
            if (isClaimedToday) {
                chest.setImageResource(R.drawable.ic_chest_opened);
                chest.setAlpha(1.0f);
                chest.setEnabled(false);
            } else {
                chest.setImageResource(R.drawable.ic_chest);
                chest.setAlpha(1.0f);
                chest.setEnabled(true);
                chest.setOnClickListener(v -> claimReward(key, xp, badge, level));
            }
        } else {
            chest.setImageResource(R.drawable.ic_chest);
            chest.setAlpha(0.4f);
            chest.setEnabled(false);
        }
    }

    private void claimReward(String key, int xp, String badge, int level) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        new AlertDialog.Builder(getContext()).setTitle("Challenge Complete!").setMessage("You earned: \n\n+ " + xp + " XP\n+ '" + badge + "' Badge").setPositiveButton("Claim", (dialog, which) -> {
            DocumentReference userRef = db.collection("users").document(currentUser.getUid());
            Map<String, Object> updates = new HashMap<>();
            updates.put("totalXP", FieldValue.increment(xp));
            updates.put("badges", FieldValue.arrayUnion(badge));
            updates.put("challenges." + key + ".lastClaimedTimestamp", FieldValue.serverTimestamp());

            if (key.equals(STREAK_CHALLENGE) || key.equals(GROUP_CHALLENGE)) {
                updates.put("challenges." + key + ".lastClaimedLevel", level);
            }
            if (key.equals(GROUP_CHALLENGE)) {
                updates.put("groupChallengeParticipants", new ArrayList<>());
                updates.put("groupChallengeTimeSnapshot", new HashMap<>());
            }

            userRef.update(updates).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Reward claimed!", Toast.LENGTH_SHORT).show();
                loadAllChallengeData();
            }).addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to claim reward.", Toast.LENGTH_SHORT).show());
        }).show();
    }

    private boolean isToday(long timestamp) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(timestamp);
        Calendar cal2 = Calendar.getInstance();
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
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

        new AlertDialog.Builder(getContext()).setTitle("Select up to " + MAX_GROUP_PARTICIPANTS + " friends")
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


    private void updateGroupChallengeInFirebase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        DocumentReference ownerRef = db.collection("users").document(currentUser.getUid());
        List<String> participantUids = new ArrayList<>(selectedFriendIds);
        participantUids.add(currentUser.getUid());

        Map<String, Object> snapshotMap = new HashMap<>();
        final int[] usersProcessed = {0};

        for (String uid : participantUids) {
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    snapshotMap.put(uid, doc.getLong("totalTime"));
                }
                usersProcessed[0]++;
                if (usersProcessed[0] == participantUids.size()) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("groupChallengeParticipants", selectedFriendIds);
                    updates.put("groupChallengeTimeSnapshot", snapshotMap);
                    ownerRef.update(updates).addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Group challenge started!", Toast.LENGTH_SHORT).show();
                        loadAllChallengeData();
                    }).addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to start challenge.", Toast.LENGTH_SHORT).show());
                }
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
        avatar.setImageResource(R.drawable.boy);
        name.setText(friendDetails.getOrDefault(userId, "Friend"));
        participantsAvatarLayout.addView(participantView);
    }

    private void animateProgressBar(ProgressBar pb, int newProgress) {
        if (pb == null) return;
        ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), newProgress);
        animation.setDuration(800);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }
}
