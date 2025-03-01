package com.ds.eventwish.ui.festival;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.model.FestivalTemplate;
import com.ds.eventwish.data.repository.FestivalRepository;
import com.ds.eventwish.workers.FestivalNotificationWorker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Test activity to manually trigger festival notifications for testing purposes.
 * This is not part of the main app flow but useful for development and testing.
 */
public class TestFestivalActivity extends AppCompatActivity {
    
    private FestivalRepository repository;
    private Executor executor;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_festival);
        
        repository = FestivalRepository.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
        
        Button addFestivalButton = findViewById(R.id.add_test_festival_button);
        Button triggerNotificationButton = findViewById(R.id.trigger_notification_button);
        Button clearFestivalsButton = findViewById(R.id.clear_festivals_button);
        
        addFestivalButton.setOnClickListener(v -> addTestFestival());
        triggerNotificationButton.setOnClickListener(v -> triggerNotification());
        clearFestivalsButton.setOnClickListener(v -> clearFestivals());
    }
    
    private void addTestFestival() {
        // Create a test festival with today's date
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        
        List<FestivalTemplate> templates = new ArrayList<>();
        templates.add(new FestivalTemplate(
                "template1",
                "Template 1",
                "This is a test template for the festival",
                "https://example.com/image.jpg",
                "general"
        ));
        
        Festival festival = new Festival(
                "test_festival_" + System.currentTimeMillis(),
                "Test Festival",
                "This is a test festival for notification testing",
                today,
                "general",
                "https://example.com/festival.jpg",
                true,
                templates
        );
        
        // Add the festival to the database
        executor.execute(() -> {
            repository.insertFestival(festival);
            runOnUiThread(() -> Toast.makeText(this, 
                    "Test festival added with today's date", 
                    Toast.LENGTH_SHORT).show());
        });
    }
    
    private void triggerNotification() {
        // Trigger the notification worker
        OneTimeWorkRequest notificationWorkRequest = 
                new OneTimeWorkRequest.Builder(FestivalNotificationWorker.class)
                        .build();
        
        WorkManager.getInstance(this).enqueue(notificationWorkRequest);
        
        Toast.makeText(this, 
                "Festival notification worker triggered", 
                Toast.LENGTH_SHORT).show();
    }
    
    private void clearFestivals() {
        // Clear all festivals from the database
        executor.execute(() -> {
            repository.deleteAllFestivals();
            runOnUiThread(() -> Toast.makeText(this, 
                    "All festivals cleared from database", 
                    Toast.LENGTH_SHORT).show());
        });
    }
}
