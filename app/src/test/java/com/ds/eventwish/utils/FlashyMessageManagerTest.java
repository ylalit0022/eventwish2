package com.ds.eventwish.utils;

import android.content.Context;
import android.util.Log;

import com.ds.eventwish.data.local.entity.FlashyMessageEntity;
import com.ds.eventwish.data.repository.FlashyMessageRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FlashyMessageManagerTest {

    @Mock
    private Context mockContext;

    @Mock
    private FlashyMessageRepository mockRepository;

    @Before
    public void setup() {
        // Mock FlashyMessageRepository.getInstance
        when(FlashyMessageRepository.getInstance(any(Context.class))).thenReturn(mockRepository);

        // Mock Android Log class
        try (MockedStatic<Log> mockedLog = mockStatic(Log.class)) {
            mockedLog.when(() -> Log.e(anyString(), anyString())).thenReturn(0);
            mockedLog.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
        }
    }

    @Test
    public void saveFlashyMessage() {
        // Arrange
        String id = "test_id";
        String title = "Test Title";
        String message = "Test Message";

        // Act
        FlashyMessageManager.saveFlashyMessage(mockContext, id, title, message);

        // Assert
        verify(mockRepository).saveMessage(id, title, message);
    }

    @Test
    public void saveFlashyMessage_nullParameters() {
        // Act & Assert - should not throw exception
        FlashyMessageManager.saveFlashyMessage(null, "id", "title", "message");
        FlashyMessageManager.saveFlashyMessage(mockContext, null, "title", "message");
        FlashyMessageManager.saveFlashyMessage(mockContext, "id", null, "message");
        FlashyMessageManager.saveFlashyMessage(mockContext, "id", "title", null);
    }

    @Test
    public void getNextMessage() {
        // Arrange
        FlashyMessageEntity entity = new FlashyMessageEntity("test_id", "Test Title", "Test Message");
        doAnswer((Answer<Void>) invocation -> {
            FlashyMessageRepository.MessageCallback callback = invocation.getArgument(0);
            callback.onMessageLoaded(entity);
            return null;
        }).when(mockRepository).getNextMessageToDisplay(any());

        // Act
        FlashyMessageManager.getNextMessage(mockContext, new FlashyMessageManager.MessageCallback() {
            @Override
            public void onMessageLoaded(FlashyMessageManager.Message message) {
                // Assert
                assert message != null;
                assert message.getId().equals(entity.getId());
                assert message.getTitle().equals(entity.getTitle());
                assert message.getMessage().equals(entity.getMessage());
            }

            @Override
            public void onError(Exception e) {
                throw new AssertionError("Unexpected error", e);
            }
        });
    }

    @Test
    public void getNextMessage_error() {
        // Arrange
        Exception testException = new Exception("Test error");
        doAnswer((Answer<Void>) invocation -> {
            FlashyMessageRepository.MessageCallback callback = invocation.getArgument(0);
            callback.onError(testException);
            return null;
        }).when(mockRepository).getNextMessageToDisplay(any());

        // Act
        FlashyMessageManager.getNextMessage(mockContext, new FlashyMessageManager.MessageCallback() {
            @Override
            public void onMessageLoaded(FlashyMessageManager.Message message) {
                throw new AssertionError("Should not be called");
            }

            @Override
            public void onError(Exception e) {
                // Assert
                assert e.getMessage().equals(testException.getMessage());
            }
        });
    }

    @Test
    public void markMessageAsShown() {
        // Arrange
        String messageId = "test_id";

        // Act
        FlashyMessageManager.markMessageAsShown(mockContext, messageId);

        // Assert
        verify(mockRepository).markMessageAsRead(messageId);
        verify(mockRepository).updateDisplayingState(messageId, false);
    }

    @Test
    public void markMessageAsShown_nullParameters() {
        // Act & Assert - should not throw exception
        FlashyMessageManager.markMessageAsShown(null, "id");
        FlashyMessageManager.markMessageAsShown(mockContext, null);
    }

    @Test
    public void resetDisplayState() {
        // Act
        FlashyMessageManager.resetDisplayState(mockContext);

        // Assert
        verify(mockRepository).resetAllDisplayingStates();
    }

    @Test
    public void resetDisplayState_nullContext() {
        // Act & Assert - should not throw exception
        FlashyMessageManager.resetDisplayState(null);
    }

    @Test
    public void clearMessages() {
        // Act
        FlashyMessageManager.clearMessages(mockContext);

        // Assert
        verify(mockRepository).clearAllMessages();
    }

    @Test
    public void clearMessages_nullContext() {
        // Act & Assert - should not throw exception
        FlashyMessageManager.clearMessages(null);
    }

    @Test
    public void message_gettersAndConstructor() {
        // Arrange
        String id = "test_id";
        String title = "Test Title";
        String message = "Test Message";

        // Act
        FlashyMessageManager.Message flashyMessage = new FlashyMessageManager.Message(id, title, message);

        // Assert
        assert flashyMessage.getId().equals(id);
        assert flashyMessage.getTitle().equals(title);
        assert flashyMessage.getMessage().equals(message);
    }
} 