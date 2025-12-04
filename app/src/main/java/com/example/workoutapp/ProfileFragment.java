package com.example.workoutapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private ImageView profileImage, settingsIcon;
    private TextView username, userHandleAndJoinDate, friendsCount, workoutCount, historyPlaceholder, historyText, friendsText;
    private CardView friendsButton, historyButton;
    private RecyclerView friendsListRecyclerView;
    private MaterialButton inviteButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FriendsAdapter friendsAdapter;
    private List<Map<String, String>> friendsData = new ArrayList<>();
    private String userId;

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
        username = view.findViewById(R.id.username);
        userHandleAndJoinDate = view.findViewById(R.id.user_handle_and_join_date);
        friendsCount = view.findViewById(R.id.friends_count);
        workoutCount = view.findViewById(R.id.workout_count);
        friendsButton = view.findViewById(R.id.friends_button);
        historyButton = view.findViewById(R.id.history_button);
        friendsListRecyclerView = view.findViewById(R.id.friends_list_recyclerview);
        inviteButton = view.findViewById(R.id.invite_button);
        historyPlaceholder = view.findViewById(R.id.history_placeholder);
        historyText = view.findViewById(R.id.history_text);
        friendsText = view.findViewById(R.id.friends_text);

        setupRecyclerView();
        loadUserData();
        setupClickListeners();

        // Default selection
        selectTab(friendsButton);

        return view;
    }

    private void setupRecyclerView() {
        friendsAdapter = new FriendsAdapter(getContext(), friendsData);
        friendsListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        friendsListRecyclerView.setAdapter(friendsAdapter);
    }

    private void setupClickListeners() {
        settingsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivity(intent);
                });
        inviteButton.setOnClickListener(v -> showAddFriendDialog());

        friendsButton.setOnClickListener(v -> selectTab(friendsButton));
        historyButton.setOnClickListener(v -> selectTab(historyButton));
    }

    private void selectTab(View tab) {
        // Reset both tabs to default state
        friendsButton.setCardBackgroundColor(Color.TRANSPARENT);
        friendsCount.setTextColor(Color.BLACK);
        friendsText.setTextColor(Color.DKGRAY);
        friendsText.setAlpha(0.5f);

        historyButton.setCardBackgroundColor(Color.TRANSPARENT);
        workoutCount.setTextColor(Color.BLACK);
        historyText.setTextColor(Color.DKGRAY);
        historyText.setAlpha(0.5f);

        // Set selected tab state
        if (tab.getId() == R.id.friends_button) {
            friendsButton.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.red_light));
            friendsCount.setTextColor(Color.WHITE);
            friendsText.setTextColor(Color.WHITE);
            friendsText.setAlpha(1.0f);
            friendsListRecyclerView.setVisibility(View.VISIBLE);
            historyPlaceholder.setVisibility(View.GONE);
        } else {
            historyButton.setCardBackgroundColor(ContextCompat.getColor(getContext(), R.color.red_light));
            workoutCount.setTextColor(Color.WHITE);
            historyText.setTextColor(Color.WHITE);
            historyText.setAlpha(1.0f);
            friendsListRecyclerView.setVisibility(View.GONE);
            historyPlaceholder.setVisibility(View.VISIBLE);
        }
    }

    private void showAddFriendDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_friend, null);
        final EditText friendIdInput = dialogView.findViewById(R.id.friend_id_input);
        final TextView myUserIdText = dialogView.findViewById(R.id.my_user_id_text);
        final ImageView copyUserIdButton = dialogView.findViewById(R.id.copy_user_id_button);

        if (userId != null && !userId.isEmpty()) {
            myUserIdText.setText(userId);
            copyUserIdButton.setEnabled(true);
            copyUserIdButton.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("User ID", userId);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getContext(), "User ID copied!", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            myUserIdText.setText("User ID not found");
            copyUserIdButton.setEnabled(false);
            copyUserIdButton.setAlpha(0.5f);
        }

        builder.setView(dialogView)
                .setPositiveButton("Add", (dialog, id) -> {
                    String friendId = friendIdInput.getText().toString().trim();
                    if (!friendId.isEmpty()) {
                        addFriend(friendId);
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());
        builder.create().show();
    }

    private void addFriend(String friendId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userRef.update("friends", FieldValue.arrayUnion(friendId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Friend added successfully!", Toast.LENGTH_SHORT).show();
                    loadUserData(); // Refresh user data and friends list
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add friend.", Toast.LENGTH_SHORT).show());
    }


    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(uid);

            profileImage.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.boy));

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String email = documentSnapshot.getString("email");
                    userId = documentSnapshot.getString("userID");
                    username.setText(documentSnapshot.getString("name"));

                    if (email != null && currentUser.getMetadata() != null) {
                        long creationTimestamp = currentUser.getMetadata().getCreationTimestamp();
                        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                        String joinDate = "Joined " + sdf.format(new Date(creationTimestamp));
                        userHandleAndJoinDate.setText(email + " • " + joinDate);
                    }

                    List<String> friendIds = (List<String>) documentSnapshot.get("friends");
                    if (friendIds != null) {
                        friendsCount.setText(String.valueOf(friendIds.size()));
                        displayFriendsList(friendIds);
                    }

                    Long totalWorkout = documentSnapshot.getLong("totalWorkout");
                    if (totalWorkout != null) {
                        workoutCount.setText(String.valueOf(totalWorkout));
                    }
                    else{
                        workoutCount.setText("0");
                    }

                } else {
                    Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void displayFriendsList(List<String> friendIds) {
        if (friendIds != null && !friendIds.isEmpty()) {
            friendsData.clear();
            for (String friendId : friendIds) {
                db.collection("users").document(friendId).get().addOnSuccessListener(friendDoc -> {
                    if (friendDoc.exists()) {
                        String email = friendDoc.getString("email");

                        if (email != null) {
                            Map<String, String> friend = new HashMap<>();
                            friend.put("username", email.split("@")[0]);
                            friend.put("email", email);
                            friendsData.add(friend);
                            friendsAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        } else {
            friendsData.clear();
            friendsAdapter.notifyDataSetChanged();
        }
    }

    // --- Inner Class for FriendsAdapter ---
    public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

        private List<Map<String, String>> friends;
        private Context context;

        public FriendsAdapter(Context context, List<Map<String, String>> friends) {
            this.context = context;
            this.friends = friends;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, String> friend = friends.get(position);
            holder.friendUsername.setText(friend.get("username"));
            holder.friendHandle.setText("@" + friend.get("email"));
            holder.friendAvatar.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.boy));
        }

        @Override
        public int getItemCount() {
            return friends.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView friendAvatar;
            TextView friendUsername;
            TextView friendHandle;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                friendAvatar = itemView.findViewById(R.id.friend_avatar);
                friendUsername = itemView.findViewById(R.id.friend_username);
                friendHandle = itemView.findViewById(R.id.friend_handle);
            }
        }
    }
}
