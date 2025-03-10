package com.ds.eventwish.workers;

import android.app.Notification;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestListenableWorkerBuilder;

import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.repository.FestivalRepository;
import com.ds.eventwish.utils.NotificationHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class FestivalNotificationWorkerTest {

    @Mock
    private FestivalRepository mockRepository;
    
    @Mock
    private Context mockContext;
    
    private FestivalNotificationWorker worker;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create a test worker
        Context context = ApplicationProvider.getApplicationContext();
        worker = TestListenableWorkerBuilder.from(context, FestivalNotificationWorker.class).build();
        
        // Mock the repository singleton
        FestivalRepository.setTestInstance(mockRepository);
    }
    
    @Test
    public void testDoWork_withUpcomingFestivals_showsNotifications() {
        // Arrange
        List<Festival> upcomingFestivals = createTestFestivals(3);
        when(mockRepository.getUnnotifiedUpcomingFestivals()).thenReturn(upcomingFestivals);
        
        // Use MockedStatic for NotificationHelper
        try (MockedStatic<NotificationHelper> mockedHelper = Mockito.mockStatic(NotificationHelper.class)) {
            // Act
            ListenableWorker.Result result = worker.doWork();
            
            // Assert
            assertEquals(ListenableWorker.Result.success(), result);
            
            // Verify notifications were shown for each festival
            mockedHelper.verify(() -> 
                NotificationHelper.showNotification(
                    any(Context.class), 
                    anyInt(), 
                    any(Notification.class)
                ), 
                times(3)
            );
            
            // Verify each festival was marked as notified
            for (Festival festival : upcomingFestivals) {
                verify(mockRepository).markAsNotified(festival.getId());
            }
        }
    }
    
    @Test
    public void testDoWork_withNoUpcomingFestivals_doesNotShowNotifications() {
        // Arrange
        List<Festival> emptyList = new ArrayList<>();
        when(mockRepository.getUnnotifiedUpcomingFestivals()).thenReturn(emptyList);
        
        // Use MockedStatic for NotificationHelper
        try (MockedStatic<NotificationHelper> mockedHelper = Mockito.mockStatic(NotificationHelper.class)) {
            // Act
            ListenableWorker.Result result = worker.doWork();
            
            // Assert
            assertEquals(ListenableWorker.Result.success(), result);
            
            // Verify no notifications were shown
            mockedHelper.verify(() -> 
                NotificationHelper.showNotification(
                    any(Context.class), 
                    anyInt(), 
                    any(Notification.class)
                ), 
                times(0)
            );
        }
    }
    
    private List<Festival> createTestFestivals(int count) {
        List<Festival> festivals = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Festival festival = new Festival();
            festival.setId("festival_" + i);
            festival.setName("Test Festival " + i);
            festival.setDate(new Date(System.currentTimeMillis() + (i * 86400000))); // Add i days
            festival.setDescription("Test description for festival " + i);
            festivals.add(festival);
        }
        return festivals;
    }
} 