package com.example.workoutapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
    private TextView username, userHandleAndJoinDate, friendsCount;
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
        initializeViews(view);

        setupRecyclerView();
        loadUserData();
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        profileImage = view.findViewById(R.id.profile_image);
        settingsIcon = view.findViewById(R.id.settings_icon);
        username = view.findViewById(R.id.username);
        userHandleAndJoinDate = view.findViewById(R.id.user_handle_and_join_date);
        friendsCount = view.findViewById(R.id.friends_count);
        friendsListRecyclerView = view.findViewById(R.id.friends_list_recyclerview);
        inviteButton = view.findViewById(R.id.invite_button);
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

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long profilePictureID = documentSnapshot.getLong("profilePictureID");
                    long safeProfilePictureId = (profilePictureID != null) ? profilePictureID : 1L;

                    if (safeProfilePictureId == 1) {
                        profileImage.setImageResource(R.drawable.boy);
                    } else if (safeProfilePictureId == 2) {
                        profileImage.setImageResource(R.drawable.man);
                    } else if (safeProfilePictureId == 3) {
                        profileImage.setImageResource(R.drawable.girl);
                    } else {
                        profileImage.setImageResource(R.drawable.woman);
                    }

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
                    } else {
                        friendsCount.setText("0");
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
                        String name = friendDoc.getString("name");
                        String email = friendDoc.getString("email");
                        Long profilePictureID = friendDoc.getLong("profilePictureID");

                        if (email != null) {
                            Map<String, String> friend = new HashMap<>();
                            friend.put("username", name);
                            friend.put("email", email);
                            friend.put("profilePictureID", String.valueOf(profilePictureID));
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
    public static class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

        private final List<Map<String, String>> friends;
        private final Context context;

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
            holder.friendHandle.setText(friend.get("email"));

            // Safely get and parse profilePictureID
            String profilePictureIdString = friend.get("profilePictureID");
            long profilePicture;

            try {
                if (profilePictureIdString != null && !profilePictureIdString.equals("null")) {
                    profilePicture = Long.parseLong(profilePictureIdString);
                } else {
                    profilePicture = 1L; // Default to 1 if the value is null
                }
            } catch (NumberFormatException e) {
                profilePicture = 1L; // Default to 1 if parsing fails
            }

            // Assign Avatar based on the safe profilePicture value
            if (profilePicture == 1) {
                holder.friendAvatar.setImageResource(R.drawable.boy);
            } else if (profilePicture == 2) {
                holder.friendAvatar.setImageResource(R.drawable.man);
            } else if (profilePicture == 3) {
                holder.friendAvatar.setImageResource(R.drawable.girl);
            } else {
                holder.friendAvatar.setImageResource(R.drawable.woman);
            }
        }

        @Override
        public int getItemCount() {
            return friends.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
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
