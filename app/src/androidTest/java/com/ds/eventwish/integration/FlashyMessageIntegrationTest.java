package com.ds.eventwish.integration;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ds.eventwish.data.local.FlashyMessageDatabase;
import com.ds.eventwish.data.local.dao.FlashyMessageDao;
import com.ds.eventwish.data.local.entity.FlashyMessageEntity;
import com.ds.eventwish.data.repository.FlashyMessageRepository;
import com.ds.eventwish.utils.FlashyMessageManager;
import com.ds.eventwish.utils.LiveDataTestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class FlashyMessageIntegrationTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FlashyMessageDatabase database;
    private FlashyMessageDao dao;
    private Context context;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, FlashyMessageDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = database.flashyMessageDao();
    }

    @After
    public void cleanup() {
        database.close();
    }

    @Test
    public void testMessageCreationAndRetrieval() throws Exception {
        // Save message through manager
        String id = "test_id";
        String title = "Test Title";
        String message = "Test Message";
        FlashyMessageManager.saveFlashyMessage(context, id, title, message);

        // Wait for async operation to complete
        Thread.sleep(100);

        // Verify message in database through DAO
        FlashyMessageEntity entity = dao.getById(id);
        assertNotNull("Message should be saved in database", entity);
        assertEquals("Title should match", title, entity.getTitle());
        assertEquals("Message should match", message, entity.getMessage());
        assertFalse("Message should not be read", entity.isRead());
    }

    @Test
    public void testMessageDisplayFlow() throws Exception {
        // Save multiple messages
        FlashyMessageManager.saveFlashyMessage(context, "id1", "Title 1", "Message 1");
        FlashyMessageManager.saveFlashyMessage(context, "id2", "Title 2", "Message 2");

        // Wait for async operations to complete
        Thread.sleep(100);

        // Get next message to display
        CountDownLatch latch = new CountDownLatch(1);
        final FlashyMessageManager.Message[] displayedMessage = new FlashyMessageManager.Message[1];

        FlashyMessageManager.getNextMessage(context, new FlashyMessageManager.MessageCallback() {
            @Override
            public void onMessageLoaded(FlashyMessageManager.Message message) {
                displayedMessage[0] = message;
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                throw new AssertionError("Unexpected error", e);
            }
        });

        // Wait for callback
        assertTrue("Callback should be called", latch.await(2, TimeUnit.SECONDS));
        assertNotNull("Should get a message to display", displayedMessage[0]);

        // Mark message as shown
        FlashyMessageManager.markMessageAsShown(context, displayedMessage[0].getId());

        // Wait for async operation to complete
        Thread.sleep(100);

        // Verify message state in database
        FlashyMessageEntity entity = dao.getById(displayedMessage[0].getId());
        assertTrue("Message should be marked as read", entity.isRead());
        assertFalse("Message should not be displaying", entity.isDisplaying());
    }

    @Test
    public void testMessageStateManagement() throws Exception {
        // Save a message
        String id = "test_id";
        FlashyMessageManager.saveFlashyMessage(context, id, "Title", "Message");

        // Wait for async operation to complete
        Thread.sleep(100);

        // Reset display states
        FlashyMessageManager.resetDisplayState(context);

        // Wait for async operation to complete
        Thread.sleep(100);

        // Verify all messages are not displaying
        List<FlashyMessageEntity> messages = LiveDataTestUtil.getValue(dao.getAllMessages());
        for (FlashyMessageEntity message : messages) {
            assertFalse("Message should not be displaying", message.isDisplaying());
        }

        // Clear all messages
        FlashyMessageManager.clearMessages(context);

        // Wait for async operation to complete
        Thread.sleep(100);

        // Verify no messages remain
        messages = LiveDataTestUtil.getValue(dao.getAllMessages());
        assertTrue("All messages should be cleared", messages.isEmpty());
    }

    @Test
    public void testConcurrentOperations() throws Exception {
        // Create multiple threads to save messages
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    FlashyMessageManager.saveFlashyMessage(context,
                            "id_" + index,
                            "Title " + index,
                            "Message " + index);
                    completionLatch.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            threads[i].start();
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all operations to complete
        completionLatch.await(5, TimeUnit.SECONDS);

        // Wait for async operations to complete
        Thread.sleep(100);

        // Verify all messages were saved
        List<FlashyMessageEntity> messages = LiveDataTestUtil.getValue(dao.getAllMessages());
        assertEquals("All messages should be saved", threadCount, messages.size());
    }

    @Test
    public void testErrorHandling() throws Exception {
        // Test with invalid message ID
        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] caughtException = new Exception[1];

        FlashyMessageManager.getNextMessage(context, new FlashyMessageManager.MessageCallback() {
            @Override
            public void onMessageLoaded(FlashyMessageManager.Message message) {
                // No message should be found
                assertTrue("No message should be found", message == null);
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                caughtException[0] = e;
                latch.countDown();
            }
        });

        assertTrue("Callback should be called", latch.await(2, TimeUnit.SECONDS));
    }
} 