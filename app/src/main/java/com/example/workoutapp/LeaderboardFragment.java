package com.example.workoutapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LeaderboardFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LeaderboardAdapter adapter;
    private List<LeaderboardUser> userList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        recyclerView = view.findViewById(R.id.leaderboardRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        userList = new ArrayList<>();
        adapter = new LeaderboardAdapter(userList);
        recyclerView.setAdapter(adapter);

        fetchFriendsAndLoadLeaderboard();

        return view;
    }

    private void fetchFriendsAndLoadLeaderboard() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 1. Get current user's document to find friends
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> friends = (List<String>) documentSnapshot.get("friends");
                        if (friends == null) friends = new ArrayList<>();
                        
                        // Add current user to the list to be fetched
                        if (!friends.contains(currentUid)) {
                            friends.add(currentUid);
                        }

                        if (friends.isEmpty()) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "No friends to show", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        fetchUsersData(friends);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error fetching user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchUsersData(List<String> uids) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<LeaderboardUser> tempUsers = new ArrayList<>();
        int[] completedCount = {0};

        for (String uid : uids) {
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            String email = doc.getString("email");
                            
                            // 1. Process Name logic
                            if (name == null || name.isEmpty()) {
                                if (email != null && !email.isEmpty()) {
                                    int atIndex = email.indexOf("@");
                                    if (atIndex != -1) {
                                        name = email.substring(0, atIndex);
                                    } else {
                                        name = email;
                                    }
                                } else {
                                    name = "Unknown User";
                                }
                            }

                            Long xp = doc.getLong("totalXP");
                            if (xp == null) xp = 0L;

                            LeaderboardUser user = new LeaderboardUser(uid, name, xp);
                            
                            // 2. Assign Random Avatar
                            assignRandomAvatar(user);
                            
                            tempUsers.add(user);
                        }
                        
                        completedCount[0]++;
                        if (completedCount[0] == uids.size()) {
                            processLeaderboard(tempUsers);
                        }
                    })
                    .addOnFailureListener(e -> {
                        completedCount[0]++;
                        if (completedCount[0] == uids.size()) {
                            processLeaderboard(tempUsers);
                        }
                    });
        }
    }

    private void assignRandomAvatar(LeaderboardUser user) {
        int[] avatars = {
            R.drawable.boy,
            R.drawable.girl,
            R.drawable.man,
            R.drawable.woman
        };
        Random random = new Random();
        // Use hash code of UID or name to keep avatar consistent for the same user across reloads if desired.
        // But request said "random", usually implies "pick one". 
        // To make it consistent per session but random per user, we can just pick one.
        // If we want it to change every time we load the fragment, simple random is fine.
        // If we want it semi-consistent without storing in DB, use UID hash.
        // I will use simple random as requested "randomly". 
        // However, using UID hash is better UX so the user doesn't change gender every refresh.
        // Let's use simple Random for now as per "randomly".
        
        int index = random.nextInt(avatars.length);
        user.setAvatarResId(avatars[index]);
    }

    private void processLeaderboard(List<LeaderboardUser> users) {
        // Sort by TotalXP Descending
        Collections.sort(users, (u1, u2) -> Long.compare(u2.getTotalXP(), u1.getTotalXP()));

        // Assign ranks
        for (int i = 0; i < users.size(); i++) {
            users.get(i).setRank(i + 1);
        }

        userList.clear();
        userList.addAll(users);
        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
    }

    // Inner Adapter Class
    private class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
        private List<LeaderboardUser> users;

        public LeaderboardAdapter(List<LeaderboardUser> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LeaderboardUser user = users.get(position);
            holder.rankText.setText(String.valueOf(user.getRank()));
            holder.nameText.setText(user.getName());
            holder.xpText.setText(user.getTotalXP() + " XP");
            
            if (user.getAvatarResId() != 0) {
                holder.avatarImage.setImageResource(user.getAvatarResId());
            }
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView rankText, nameText, xpText;
            ImageView avatarImage;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                rankText = itemView.findViewById(R.id.rank);
                nameText = itemView.findViewById(R.id.userName);
                xpText = itemView.findViewById(R.id.userXP);
                avatarImage = itemView.findViewById(R.id.avatar);
            }
        }
    }
}