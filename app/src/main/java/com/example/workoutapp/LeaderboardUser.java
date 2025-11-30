package com.example.workoutapp;

public class LeaderboardUser {
    private String uid;
    private String name;
    private long totalXP;
    private int rank;
    private int avatarResId;

    public LeaderboardUser(String uid, String name, long totalXP) {
        this.uid = uid;
        this.name = name;
        this.totalXP = totalXP;
        this.avatarResId = 0; // Default or unassigned
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTotalXP() {
        return totalXP;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getAvatarResId() {
        return avatarResId;
    }

    public void setAvatarResId(int avatarResId) {
        this.avatarResId = avatarResId;
    }
}