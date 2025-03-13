package com.ds.eventwish.data.cache;

import android.content.Context;
import android.os.Handler;

import com.ds.eventwish.utils.AppExecutors;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ResourceCacheTest {

    private ResourceCache resourceCache;
    private Context context;

    @Mock
    private AppExecutors mockExecutors;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = RuntimeEnvironment.application;
        
        // Create a test cache directory
        File cacheDir = new File(context.getCacheDir(), "test_cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        
        // Mock AppExecutors to run tasks immediately
        mockExecutors = mock(AppExecutors.class);
        Executor directExecutor = new DirectExecutor();
        when(mockExecutors.diskIO()).thenReturn(directExecutor);
        when(mockExecutors.mainThread()).thenReturn(directExecutor);
        
        // Initialize ResourceCache with test context
        resourceCache = ResourceCache.getInstance(context);
    }

    @Test
    public void testPutAndGet() {
        // Create test data
        JsonObject testObject = new JsonObject();
        testObject.addProperty("id", "123");
        testObject.addProperty("name", "Test Object");
        
        // Put in cache
        resourceCache.put("test_key", testObject);
        
        // Get from cache
        JsonObject retrievedObject = resourceCache.get("test_key");
        
        // Verify
        assertNotNull("Retrieved object should not be null", retrievedObject);
        assertEquals("123", retrievedObject.get("id").getAsString());
        assertEquals("Test Object", retrievedObject.get("name").getAsString());
    }

    @Test
    public void testExpiration() throws InterruptedException {
        // Create test data
        JsonObject testObject = new JsonObject();
        testObject.addProperty("id", "456");
        
        // Put in cache with short expiration (100ms)
        resourceCache.put("expiring_key", testObject, 100);
        
        // Verify it exists initially
        JsonObject retrievedObject = resourceCache.get("expiring_key");
        assertNotNull("Object should exist before expiration", retrievedObject);
        
        // Wait for expiration
        Thread.sleep(150);
        
        // Verify it's gone after expiration
        JsonObject expiredObject = resourceCache.get("expiring_key");
        assertNull("Object should be null after expiration", expiredObject);
    }

    @Test
    public void testRemove() {
        // Create test data
        JsonObject testObject = new JsonObject();
        testObject.addProperty("id", "789");
        
        // Put in cache
        resourceCache.put("remove_key", testObject);
        
        // Verify it exists
        assertTrue("Cache should contain the key", resourceCache.contains("remove_key"));
        
        // Remove from cache
        resourceCache.remove("remove_key");
        
        // Verify it's gone
        assertFalse("Cache should not contain the key after removal", resourceCache.contains("remove_key"));
    }

    @Test
    public void testClear() {
        // Create test data
        JsonObject testObject1 = new JsonObject();
        testObject1.addProperty("id", "111");
        
        JsonObject testObject2 = new JsonObject();
        testObject2.addProperty("id", "222");
        
        // Put in cache
        resourceCache.put("key1", testObject1);
        resourceCache.put("key2", testObject2);
        
        // Verify they exist
        assertTrue("Cache should contain key1", resourceCache.contains("key1"));
        assertTrue("Cache should contain key2", resourceCache.contains("key2"));
        
        // Clear cache
        resourceCache.clear();
        
        // Verify they're gone
        assertFalse("Cache should not contain key1 after clear", resourceCache.contains("key1"));
        assertFalse("Cache should not contain key2 after clear", resourceCache.contains("key2"));
    }

    @Test
    public void testRemoveByPrefix() {
        // Create test data
        JsonObject testObject1 = new JsonObject();
        testObject1.addProperty("id", "333");
        
        JsonObject testObject2 = new JsonObject();
        testObject2.addProperty("id", "444");
        
        JsonObject testObject3 = new JsonObject();
        testObject3.addProperty("id", "555");
        
        // Put in cache with different prefixes
        resourceCache.put("prefix1_key1", testObject1);
        resourceCache.put("prefix1_key2", testObject2);
        resourceCache.put("prefix2_key1", testObject3);
        
        // Verify they exist
        assertTrue(resourceCache.contains("prefix1_key1"));
        assertTrue(resourceCache.contains("prefix1_key2"));
        assertTrue(resourceCache.contains("prefix2_key1"));
        
        // Remove by prefix
        resourceCache.removeByPrefix("prefix1_");
        
        // Verify prefix1 keys are gone but prefix2 key remains
        assertFalse(resourceCache.contains("prefix1_key1"));
        assertFalse(resourceCache.contains("prefix1_key2"));
        assertTrue(resourceCache.contains("prefix2_key1"));
    }

    /**
     * Simple executor that runs tasks immediately on the calling thread
     */
    private static class DirectExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
} 