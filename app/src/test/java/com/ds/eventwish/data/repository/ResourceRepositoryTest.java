package com.ds.eventwish.data.repository;

import static com.google.common.base.Verify.verify;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.ds.eventwish.data.cache.ResourceCache;
import com.ds.eventwish.data.model.Resource;
import com.ds.eventwish.data.model.ResourceType;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.utils.AppExecutors;
import com.ds.eventwish.utils.ErrorHandler;
import com.ds.eventwish.utils.NetworkUtils;
import com.google.ar.core.Config;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ResourceRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private ResourceRepository resourceRepository;
    private Context context;

    @Mock
    private ResourceCache mockResourceCache;

    @Mock
    private ApiService mockApiService;

    @Mock
    private NetworkUtils mockNetworkUtils;

    @Mock
    private ErrorHandler mockErrorHandler;

    @Mock
    private AppExecutors mockExecutors;

    @Mock
    private Call<JsonObject> mockCall;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        context = RuntimeEnvironment.application;

        // Mock AppExecutors to run tasks immediately
        Executor directExecutor = new DirectExecutor();
        when(mockExecutors.diskIO()).thenReturn(directExecutor);
        when(mockExecutors.mainThread()).thenReturn(directExecutor);

        // Mock ApiClient
        ApiClient mockApiClient = mock(ApiClient.class);
        when(mockApiClient.getApiService()).thenReturn(mockApiService);

        // Create test repository with mocked dependencies
        resourceRepository = new ResourceRepository(
                context,
                mockResourceCache,
                mockApiService,
                mockExecutors,
                mockNetworkUtils,
                mockErrorHandler
        );
    }

    @Test
    public void testLoadResourceFromCache() throws InterruptedException {
        // Setup test data
        JsonObject cachedData = new JsonObject();
        cachedData.addProperty("id", "123");
        cachedData.addProperty("name", "Cached Resource");

        // Mock cache hit
        when(mockResourceCache.get(anyString())).thenReturn(cachedData);
        when(mockNetworkUtils.isConnected()).thenReturn(true);
        when(mockNetworkUtils.isConnectionMetered()).thenReturn(false);

        // Mock API call for background refresh
        when(mockApiService.getResource(anyString(), anyString(), anyMap())).thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<JsonObject> callback = invocation.getArgument(0);
            JsonObject networkData = new JsonObject();
            networkData.addProperty("id", "123");
            networkData.addProperty("name", "Updated Resource");
            callback.onResponse(mockCall, Response.success(networkData));
            return null;
        }).when(mockCall).enqueue(any());

        // Load resource
        LiveData<Resource<JsonObject>> result = resourceRepository.loadResource(
                ResourceType.TEMPLATE,
                "123",
                false
        );

        // Observe the LiveData
        Resource<JsonObject> loadedResource = observeForTesting(result);

        // Verify cache was checked
        verify(mockResourceCache).get(eq("template_123"));

        // Verify result is success with cached data
        assertNotNull("Resource should not be null", loadedResource);
        assertEquals("Resource status should be SUCCESS", Resource.Status.SUCCESS, loadedResource.getStatus());
        assertNotNull("Resource data should not be null", loadedResource.getData());
        assertEquals("Resource data should match cached data", "Cached Resource", loadedResource.getData().get("name").getAsString());
        assertFalse("Resource should not be marked as stale", loadedResource.isStale());

        // Verify background refresh was attempted
        verify(mockApiService).getResource(eq("templates"), eq("123"), anyMap());
    }

    @Test
    public void testLoadResourceFromNetwork() throws InterruptedException {
        // Setup test data
        JsonObject networkData = new JsonObject();
        networkData.addProperty("id", "456");
        networkData.addProperty("name", "Network Resource");

        // Mock cache miss
        when(mockResourceCache.get(anyString())).thenReturn(null);
        when(mockNetworkUtils.isConnected()).thenReturn(true);

        // Mock API call
        when(mockApiService.getResource(anyString(), anyString(), anyMap())).thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<JsonObject> callback = invocation.getArgument(0);
            Headers headers = Headers.of("ETag", "\"123456\"");
            callback.onResponse(mockCall, Response.success(networkData, headers));
            return null;
        }).when(mockCall).enqueue(any());

        // Load resource
        LiveData<Resource<JsonObject>> result = resourceRepository.loadResource(
                ResourceType.CATEGORY,
                "456",
                false
        );

        // Observe the LiveData
        Resource<JsonObject> loadedResource = observeForTesting(result);

        // Verify cache was checked
        verify(mockResourceCache).get(eq("category_456"));

        // Verify API was called
        verify(mockApiService).getResource(eq("categorys"), eq("456"), anyMap());

        // Verify result is success with network data
        assertNotNull("Resource should not be null", loadedResource);
        assertEquals("Resource status should be SUCCESS", Resource.Status.SUCCESS, loadedResource.getStatus());
        assertNotNull("Resource data should not be null", loadedResource.getData());
        assertEquals("Resource data should match network data", "Network Resource", loadedResource.getData().get("name").getAsString());

        // Verify data was cached
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JsonObject> dataCaptor = ArgumentCaptor.forClass(JsonObject.class);
        ArgumentCaptor<Long> expirationCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockResourceCache).put(keyCaptor.capture(), dataCaptor.capture(), expirationCaptor.capture());
        
        assertEquals("Cache key should match", "category_456", keyCaptor.getValue());
        assertEquals("Cached data should match network data", "Network Resource", dataCaptor.getValue().get("name").getAsString());
    }

    @Test
    public void testLoadResourceOffline() throws InterruptedException {
        // Setup test data
        JsonObject cachedData = new JsonObject();
        cachedData.addProperty("id", "789");
        cachedData.addProperty("name", "Offline Resource");

        // Mock offline state with cache hit
        when(mockResourceCache.get(anyString())).thenReturn(cachedData);
        when(mockNetworkUtils.isConnected()).thenReturn(false);

        // Load resource
        LiveData<Resource<JsonObject>> result = resourceRepository.loadResource(
                ResourceType.ICON,
                "789",
                false
        );

        // Observe the LiveData
        Resource<JsonObject> loadedResource = observeForTesting(result);

        // Verify cache was checked
        verify(mockResourceCache).get(eq("icon_789"));

        // Verify API was not called
        verify(mockApiService, times(0)).getResource(anyString(), anyString(), anyMap());

        // Verify result is success with stale flag
        assertNotNull("Resource should not be null", loadedResource);
        assertEquals("Resource status should be SUCCESS", Resource.Status.SUCCESS, loadedResource.getStatus());
        assertNotNull("Resource data should not be null", loadedResource.getData());
        assertEquals("Resource data should match cached data", "Offline Resource", loadedResource.getData().get("name").getAsString());
        assertTrue("Resource should be marked as stale", loadedResource.isStale());
    }

    @Test
    public void testLoadResourceOfflineNoCache() throws InterruptedException {
        // Mock offline state with cache miss
        when(mockResourceCache.get(anyString())).thenReturn(null);
        when(mockNetworkUtils.isConnected()).thenReturn(false);

        // Load resource
        LiveData<Resource<JsonObject>> result = resourceRepository.loadResource(
                ResourceType.EVENT,
                "999",
                false
        );

        // Observe the LiveData
        Resource<JsonObject> loadedResource = observeForTesting(result);

        // Verify cache was checked
        verify(mockResourceCache).get(eq("event_999"));

        // Verify API was not called
        verify(mockApiService, times(0)).getResource(anyString(), anyString(), anyMap());

        // Verify result is error
        assertNotNull("Resource should not be null", loadedResource);
        assertEquals("Resource status should be ERROR", Resource.Status.ERROR, loadedResource.getStatus());
        assertTrue("Error message should mention offline state", loadedResource.getMessage().contains("No internet connection"));

        // Verify error handler was called
        verify(mockErrorHandler).handleError(
                eq(ErrorHandler.ErrorType.OFFLINE),
                anyString(),
                eq(ErrorHandler.ErrorSeverity.MEDIUM)
        );
    }

    @Test
    public void testLoadResourceNetworkError() throws InterruptedException {
        // Mock cache miss
        when(mockResourceCache.get(anyString())).thenReturn(null);
        when(mockNetworkUtils.isConnected()).thenReturn(true);

        // Mock API call with error
        when(mockApiService.getResource(anyString(), anyString(), anyMap())).thenReturn(mockCall);
        doAnswer(invocation -> {
            Callback<JsonObject> callback = invocation.getArgument(0);
            callback.onFailure(mockCall, new IOException("Network error"));
            return null;
        }).when(mockCall).enqueue(any());

        // Load resource
        LiveData<Resource<JsonObject>> result = resourceRepository.loadResource(
                ResourceType.USER,
                "111",
                false
        );

        // Observe the LiveData
        Resource<JsonObject> loadedResource = observeForTesting(result);

        // Verify cache was checked
        verify(mockResourceCache).get(eq("user_111"));

        // Verify API was called
        verify(mockApiService).getResource(eq("users"), eq("111"), anyMap());

        // Verify result is error
        assertNotNull("Resource should not be null", loadedResource);
        assertEquals("Resource status should be ERROR", Resource.Status.ERROR, loadedResource.getStatus());
    }

    @Test
    public void testClearCache() {
        // Call clear cache
        resourceRepository.clearCache(ResourceType.TEMPLATE, "123");

        // Verify cache was cleared
        verify(mockResourceCache).remove(eq("template_123"));
    }

    @Test
    public void testClearCacheByType() {
        // Call clear cache by type
        resourceRepository.clearCache(ResourceType.CATEGORY);

        // Verify cache was cleared by prefix
        verify(mockResourceCache).removeByPrefix(eq("category_"));
    }

    @Test
    public void testClearAllCaches() {
        // Call clear all caches
        resourceRepository.clearAllCaches();

        // Verify all caches were cleared
        verify(mockResourceCache).clear();
    }

    /**
     * Helper method to observe LiveData for testing
     */
    private <T> T observeForTesting(final LiveData<T> liveData) throws InterruptedException {
        final Object[] data = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T o) {
                data[0] = o;
                latch.countDown();
                liveData.removeObserver(this);
            }
        };
        liveData.observeForever(observer);
        latch.await(2, TimeUnit.SECONDS);
        //noinspection unchecked
        return (T) data[0];
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