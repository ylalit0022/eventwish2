package com.ds.eventwish.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.local.FlashyMessageDatabase;
import com.ds.eventwish.data.local.dao.FlashyMessageDao;
import com.ds.eventwish.data.local.entity.FlashyMessageEntity;
import com.ds.eventwish.utils.AppExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FlashyMessageRepositoryTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private Context mockContext;

    @Mock
    private FlashyMessageDatabase mockDatabase;

    @Mock
    private FlashyMessageDao mockDao;

    @Mock
    private SharedPreferences mockSharedPrefs;

    @Mock
    private SharedPreferences.Editor mockEditor;

    private FlashyMessageRepository repository;

    @Before
    public void setup() {
        // Setup SharedPreferences mock
        when(mockContext.getSharedPreferences(anyString(), eq(Context.MODE_PRIVATE)))
                .thenReturn(mockSharedPrefs);
        when(mockSharedPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.remove(anyString())).thenReturn(mockEditor);

        // Setup Database mock
        when(mockDatabase.flashyMessageDao()).thenReturn(mockDao);
        when(FlashyMessageDatabase.getInstance(mockContext)).thenReturn(mockDatabase);

        // Setup immediate executor for testing
        AppExecutors.getInstance().diskIO();
        
        // Create repository instance
        repository = FlashyMessageRepository.getInstance(mockContext);
    }

    @Test
    public void saveMessage() {
        // Arrange
        String id = "test_id";
        String title = "Test Title";
        String message = "Test Message";

        // Act
        repository.saveMessage(id, title, message);

        // Assert
        verify(mockDao).insert(any(FlashyMessageEntity.class));
    }

    @Test
    public void getUnreadMessages() {
        // Arrange
        MutableLiveData<List<FlashyMessageEntity>> liveData = new MutableLiveData<>();
        List<FlashyMessageEntity> unreadMessages = Arrays.asList(
            new FlashyMessageEntity("1", "Title 1", "Message 1"),
            new FlashyMessageEntity("2", "Title 2", "Message 2")
        );
        when(mockDao.getUnreadMessages()).thenReturn(liveData);
        liveData.setValue(unreadMessages);

        // Act
        repository.getUnreadMessages();

        // Assert
        verify(mockDao).getUnreadMessages();
    }

    @Test
    public void markMessageAsRead() {
        // Arrange
        String messageId = "test_id";

        // Act
        repository.markMessageAsRead(messageId);

        // Assert
        verify(mockDao).markAsRead(messageId);
    }

    @Test
    public void updateDisplayingState() {
        // Arrange
        String messageId = "test_id";
        boolean isDisplaying = true;

        // Act
        repository.updateDisplayingState(messageId, isDisplaying);

        // Assert
        verify(mockDao).updateDisplayingState(messageId, isDisplaying);
    }

    @Test
    public void resetAllDisplayingStates() {
        // Act
        repository.resetAllDisplayingStates();

        // Assert
        verify(mockDao).resetAllDisplayingStates();
    }

    @Test
    public void getNextMessageToDisplay() {
        // Arrange
        FlashyMessageEntity message = new FlashyMessageEntity("test_id", "Test Title", "Test Message");
        when(mockDao.getNextMessageToDisplay()).thenReturn(message);

        // Act
        repository.getNextMessageToDisplay(new FlashyMessageRepository.MessageCallback() {
            @Override
            public void onMessageLoaded(FlashyMessageEntity message) {
                // Assert
                verify(mockDao).getNextMessageToDisplay();
                verify(mockDao).updateDisplayingState(message.getId(), true);
            }

            @Override
            public void onError(Exception e) {
                // Should not be called
                throw new AssertionError("Unexpected error", e);
            }
        });
    }

    @Test
    public void clearAllMessages() {
        // Act
        repository.clearAllMessages();

        // Assert
        verify(mockDao).deleteAll();
    }

    @Test
    public void migrateFromSharedPreferences() {
        // Arrange
        String jsonMessages = "[{\"id\":\"1\",\"title\":\"Title 1\",\"message\":\"Message 1\"}," +
                            "{\"id\":\"2\",\"title\":\"Title 2\",\"message\":\"Message 2\"}]";
        when(mockSharedPrefs.getString(anyString(), anyString())).thenReturn(jsonMessages);

        // Act - Migration happens in constructor
        FlashyMessageRepository.getInstance(mockContext);

        // Assert
        verify(mockDao).insertAll(any());
        verify(mockEditor).remove(anyString());
        verify(mockEditor).apply();
    }
} 