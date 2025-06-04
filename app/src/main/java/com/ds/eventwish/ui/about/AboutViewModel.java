package com.ds.eventwish.ui.about;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.ds.eventwish.data.model.About;
import com.ds.eventwish.data.repository.AboutRepository;

public class AboutViewModel extends AndroidViewModel {
    private static final String TAG = "AboutViewModel";
    
    private final AboutRepository repository;
    private final LiveData<About> aboutContent;
    private final LiveData<String> error;
    private final LiveData<Boolean> loading;

    public AboutViewModel(Application application) {
        super(application);
        repository = new AboutRepository(application);
        aboutContent = repository.getAbout();
        error = repository.getError();
        loading = repository.getLoading();
    }

    public void loadAboutContent() {
        Log.d(TAG, "Loading about content");
        repository.fetchAbout();
    }

    public void refreshAboutContent() {
        Log.d(TAG, "Refreshing about content");
        repository.fetchAbout(true);
    }

    public LiveData<About> getAboutContent() {
        return aboutContent;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }
} 