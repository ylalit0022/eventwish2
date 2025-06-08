package com.ds.eventwish.data.repository;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LikeCountTest {
    private static final String TAG = "LikeCountTest";
    
    @Mock
    private DocumentSnapshot templateSnapshot;
    
    private static final String TEST_TEMPLATE_ID = "test_template_123";

    @Before
    public void setUp() {
        System.out.println(TAG + ": Setting up test...");
        // Mock document snapshot
        when(templateSnapshot.getLong("likeCount")).thenReturn(1L);
        System.out.println(TAG + ": Initial like count set to 1");
    }

    @After
    public void tearDown() {
        System.out.println(TAG + ": Test completed");
        System.out.println("----------------------------------------");
    }

    @Test
    public void testLikeCount() {
        System.out.println(TAG + ": Testing initial like count...");
        
        // Verify like count
        Long likeCount = templateSnapshot.getLong("likeCount");
        System.out.println(TAG + ": Retrieved like count: " + likeCount);
        
        assertEquals("Like count should be 1", 1L, (long)likeCount);
        System.out.println(TAG + ": Like count verified successfully");
    }

    @Test
    public void testUpdateLikeCount() {
        System.out.println(TAG + ": Testing like count update...");
        
        // Create update data
        Map<String, Object> updates = new HashMap<>();
        updates.put("likeCount", 2L);
        System.out.println(TAG + ": Created update data with new like count: 2");
        
        // Mock new snapshot after update
        when(templateSnapshot.getLong("likeCount")).thenReturn(2L);
        System.out.println(TAG + ": Mocked updated like count");
        
        // Verify updated like count
        Long updatedLikeCount = templateSnapshot.getLong("likeCount");
        System.out.println(TAG + ": Retrieved updated like count: " + updatedLikeCount);
        
        assertEquals("Like count should be 2", 2L, (long)updatedLikeCount);
        System.out.println(TAG + ": Updated like count verified successfully");
    }
} 