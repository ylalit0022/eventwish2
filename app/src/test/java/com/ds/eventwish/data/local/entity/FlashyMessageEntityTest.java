package com.ds.eventwish.data.local.entity;

import static org.junit.Assert.*;
import org.junit.Test;

public class FlashyMessageEntityTest {

    @Test
    public void testEntityCreation() {
        String id = "test_id";
        String title = "Test Title";
        String message = "Test Message";

        FlashyMessageEntity entity = new FlashyMessageEntity(id, title, message);

        assertEquals("ID should match", id, entity.getId());
        assertEquals("Title should match", title, entity.getTitle());
        assertEquals("Message should match", message, entity.getMessage());
        assertFalse("New message should not be read", entity.isRead());
        assertFalse("New message should not be displaying", entity.isDisplaying());
        assertEquals("Default priority should be 1", 1, entity.getPriority());
        assertTrue("Timestamp should be recent", 
            Math.abs(System.currentTimeMillis() - entity.getTimestamp()) < 1000);
    }

    @Test
    public void testSetters() {
        FlashyMessageEntity entity = new FlashyMessageEntity("id", "title", "message");

        // Test ID setter
        entity.setId("new_id");
        assertEquals("ID should be updated", "new_id", entity.getId());

        // Test title setter
        entity.setTitle("New Title");
        assertEquals("Title should be updated", "New Title", entity.getTitle());

        // Test message setter
        entity.setMessage("New Message");
        assertEquals("Message should be updated", "New Message", entity.getMessage());

        // Test read state setter
        entity.setRead(true);
        assertTrue("Read state should be updated", entity.isRead());

        // Test displaying state setter
        entity.setDisplaying(true);
        assertTrue("Displaying state should be updated", entity.isDisplaying());

        // Test priority setter
        entity.setPriority(2);
        assertEquals("Priority should be updated", 2, entity.getPriority());

        // Test timestamp setter
        long newTimestamp = System.currentTimeMillis() - 1000;
        entity.setTimestamp(newTimestamp);
        assertEquals("Timestamp should be updated", newTimestamp, entity.getTimestamp());
    }

    @Test
    public void testNullValues() {
        // Test with null ID (should throw exception)
        try {
            new FlashyMessageEntity(null, "title", "message");
            fail("Should throw NullPointerException for null ID");
        } catch (NullPointerException e) {
            // Expected
        }

        // Create valid entity for testing null setters
        FlashyMessageEntity entity = new FlashyMessageEntity("id", "title", "message");

        // Test setting null ID
        try {
            entity.setId(null);
            fail("Should throw NullPointerException for null ID");
        } catch (NullPointerException e) {
            // Expected
        }

        // Test setting null title (should be allowed)
        entity.setTitle(null);
        assertNull("Title should be null", entity.getTitle());

        // Test setting null message (should be allowed)
        entity.setMessage(null);
        assertNull("Message should be null", entity.getMessage());
    }

    @Test
    public void testStateTransitions() {
        FlashyMessageEntity entity = new FlashyMessageEntity("id", "title", "message");

        // Test read state transitions
        assertFalse("Initial read state should be false", entity.isRead());
        entity.setRead(true);
        assertTrue("Read state should be true", entity.isRead());
        entity.setRead(false);
        assertFalse("Read state should be false", entity.isRead());

        // Test displaying state transitions
        assertFalse("Initial displaying state should be false", entity.isDisplaying());
        entity.setDisplaying(true);
        assertTrue("Displaying state should be true", entity.isDisplaying());
        entity.setDisplaying(false);
        assertFalse("Displaying state should be false", entity.isDisplaying());
    }
} 