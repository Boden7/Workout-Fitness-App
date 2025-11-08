package com.example.workoutapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private ImageView profileImage, settingsIcon, copyUserIdIcon;
    private TextView username, userId, joinDate, friendsCount, workoutCount;
    private LinearLayout friendsButton, historyButton;
    private LinearLayout friendsListContainer;
    private View selectedTab;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        profileImage = view.findViewById(R.id.profile_image);
        settingsIcon = view.findViewById(R.id.settings_icon);
        copyUserIdIcon = view.findViewById(R.id.copy_userid_icon);
        username = view.findViewById(R.id.username);
        userId = view.findViewById(R.id.user_id);
        joinDate = view.findViewById(R.id.join_date);
        friendsCount = view.findViewById(R.id.friends_count);
        workoutCount = view.findViewById(R.id.workout_count);
        friendsButton = view.findViewById(R.id.friends_button);
        historyButton = view.findViewById(R.id.history_button);
        friendsListContainer = view.findViewById(R.id.friends_list_container);

        loadUserData();

        settingsIcon.setOnClickListener(v -> {
            // Handle settings icon click
            Toast.makeText(getContext(), "Settings clicked", Toast.LENGTH_SHORT).show();
        });

        copyUserIdIcon.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("User ID", userId.getText().toString().replace("User ID: ", ""));
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "User ID copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        friendsButton.setOnClickListener(v -> {
            updateSelection(friendsButton);
            friendsListContainer.setVisibility(View.VISIBLE);
            displayFriendsList();
        });

        historyButton.setOnClickListener(v -> {
            updateSelection(historyButton);
            friendsListContainer.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Exercise History coming soon!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void updateSelection(View newSelection) {
        if (selectedTab != null) {
            selectedTab.setBackgroundColor(Color.TRANSPARENT);
        }
        newSelection.setBackgroundColor(Color.LTGRAY);
        selectedTab = newSelection;
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(uid);

            // Load profile image from drawable
            profileImage.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.boy));

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String email = documentSnapshot.getString("email");
                    if (email != null) {
                        username.setText(email.split("@")[0]);
                    }
                    userId.setText("User ID: " + uid);

                    // Get user creation date
                    if (currentUser.getMetadata() != null) {
                        long creationTimestamp = currentUser.getMetadata().getCreationTimestamp();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        joinDate.setText("Joined: " + sdf.format(new Date(creationTimestamp)));
                    }

                    List<String> friends = (List<String>) documentSnapshot.get("friends");
                    if (friends != null) {
                        friendsCount.setText(String.valueOf(friends.size()));
                    }

                    Long totalWorkout = documentSnapshot.getLong("totalWorkout");
                    if (totalWorkout != null) {
                        workoutCount.setText(String.valueOf(totalWorkout));
                    }

                } else {
                    Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void displayFriendsList() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(uid);

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<String> friendIds = (List<String>) documentSnapshot.get("friends");
                    if (friendIds != null && !friendIds.isEmpty()) {
                        friendsListContainer.removeAllViews(); // Clear previous list
                        LayoutInflater inflater = LayoutInflater.from(getContext());
                        for (String friendId : friendIds) {
                            db.collection("users").document(friendId).get().addOnSuccessListener(friendDoc -> {
                                if (friendDoc.exists()) {
                                    String email = friendDoc.getString("email");
                                    if (email != null) {
                                        View friendItemView = inflater.inflate(R.layout.friend_list_item, friendsListContainer, false);
                                        ImageView friendAvatar = friendItemView.findViewById(R.id.friend_avatar);
                                        TextView friendUsername = friendItemView.findViewById(R.id.friend_username);

                                        friendUsername.setText(email.split("@")[0]);
                                        friendAvatar.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.boy));
                                        friendsListContainer.addView(friendItemView);
                                    }
                                }
                            });
                        }
                    } else {
                        friendsListContainer.removeAllViews();
                        TextView noFriendsView = new TextView(getContext());
                        noFriendsView.setText("You have no friends yet.");
                        friendsListContainer.addView(noFriendsView);
                    }
                }
            });
        }
    }
}
