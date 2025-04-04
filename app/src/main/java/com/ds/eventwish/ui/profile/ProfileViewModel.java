package com.ds.eventwish.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ProfileViewModel extends ViewModel {
    
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> email = new MutableLiveData<>();

    public ProfileViewModel() {
        // Initialize with default values or load from repository
        username.setValue("User");
        email.setValue("user@example.com");
    }

    public LiveData<String> getUsername() {
        return username;
    }

    public LiveData<String> getEmail() {
        return email;
    }

    public void updateProfile(String username, String email) {
        this.username.setValue(username);
        this.email.setValue(email);
        // Save to repository or preferences
    }
} 