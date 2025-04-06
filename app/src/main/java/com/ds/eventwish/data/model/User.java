package com.ds.eventwish.data.model;

public class User {
    private String phoneNumber;
    private int coins;
    private long lastActive;
    private boolean isUnlocked;
    private long unlockExpiry;

    /**
     * Default constructor
     */
    public User() {
        // Required empty constructor
    }

    public User(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.coins = 0;
        this.lastActive = System.currentTimeMillis();
        this.isUnlocked = false;
        this.unlockExpiry = 0;
    }

    // Add getters and setters
}
