package com.ds.eventwish.ui.contact;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.ds.eventwish.data.model.Contact;
import com.ds.eventwish.data.repository.ContactRepository;

public class ContactViewModel extends AndroidViewModel {
    private static final String TAG = "ContactViewModel";
    
    private final ContactRepository repository;
    private final LiveData<Contact> contactContent;
    private final LiveData<String> error;
    private final LiveData<Boolean> loading;

    public ContactViewModel(Application application) {
        super(application);
        repository = new ContactRepository(application);
        contactContent = repository.getContact();
        error = repository.getError();
        loading = repository.getLoading();
    }

    public void loadContactContent() {
        Log.d(TAG, "Loading contact content");
        repository.fetchContact();
    }

    public void refreshContactContent() {
        Log.d(TAG, "Refreshing contact content");
        repository.fetchContact(true);
    }

    public LiveData<Contact> getContactContent() {
        return contactContent;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }
} 