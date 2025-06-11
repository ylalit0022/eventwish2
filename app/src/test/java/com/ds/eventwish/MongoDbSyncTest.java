package com.ds.eventwish;

import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
import com.ds.eventwish.data.remote.FirestoreManager;
import com.ds.eventwish.data.model.response.BaseResponse;
import com.ds.eventwish.util.SecureTokenManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for MongoDB sync functionality on Android client
 * Tests the FirestoreManager.updateUserProfileInMongoDB method
 */
@RunWith(MockitoJUnitRunner.class)
public class MongoDbSyncTest {

    @Mock
    private FirebaseUser mockFirebaseUser;
    
    @Mock
    private FirebaseAuth mockFirebaseAuth;
    
    @Mock
    private ApiService mockApiService;
    
    @Mock
    private SecureTokenManager mockSecureTokenManager;
    
    @Mock
    private ApiClient mockApiClient;
    
    @Mock
    private Call<BaseResponse<Void>> mockCall;
    
    @Mock
    private Response<BaseResponse<Void>> mockResponse;
    
    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        // Mock Firebase User
        when(mockFirebaseUser.getUid()).thenReturn("test_firebase_uid");
        when(mockFirebaseUser.getDisplayName()).thenReturn("Test User");
        when(mockFirebaseUser.getEmail()).thenReturn("test@example.com");
        when(mockFirebaseUser.getPhotoUrl()).thenReturn(android.net.Uri.parse("https://example.com/photo.jpg"));
        
        // Mock SecureTokenManager
        when(mockSecureTokenManager.getDeviceId()).thenReturn("test_device_id");
        
        // Mock ApiClient
        when(ApiClient.isInitialized()).thenReturn(true);
        when(ApiClient.getClient()).thenReturn(mockApiService);
        
        // Mock successful API response
        when(mockApiService.updateUserProfile(any())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
    }
    
    /**
     * Test MongoDB sync method with valid user data
     */
    @Test
    public void testUpdateUserProfileInMongoDB_success() throws ExecutionException, InterruptedException {
        // Given a valid Firebase user
        
        // When updating user profile in MongoDB
        Task<Void> task = FirestoreManager.getInstance().updateUserProfileInMongoDB(mockFirebaseUser);
        
        // Then the task should complete successfully
        Tasks.await(task);
        assertTrue(task.isSuccessful());
        
        // Verify the API call was made with correct data
        verify(mockApiService).updateUserProfile(Mockito.argThat(userData -> {
            // Check that all required fields are present
            return userData.containsKey("uid") && 
                   userData.containsKey("deviceId") &&
                   userData.containsKey("displayName") &&
                   userData.containsKey("email") &&
                   userData.containsKey("profilePhoto") &&
                   userData.containsKey("lastOnline") &&
                   userData.get("uid").equals("test_firebase_uid");
        }));
    }
    
    /**
     * Test MongoDB sync with 404 error (endpoint not found)
     * This should be treated as non-critical and not fail the task
     */
    @Test
    public void testUpdateUserProfileInMongoDB_404Error() throws ExecutionException, InterruptedException {
        // Given a 404 API response
        when(mockResponse.isSuccessful()).thenReturn(false);
        when(mockResponse.code()).thenReturn(404);
        
        // When updating user profile in MongoDB
        Task<Void> task = FirestoreManager.getInstance().updateUserProfileInMongoDB(mockFirebaseUser);
        
        // Then the task should still complete successfully (non-critical error)
        Tasks.await(task);
        assertTrue(task.isSuccessful());
    }
    
    /**
     * Test MongoDB sync with network error
     * This should be treated as non-critical and not fail the task
     */
    @Test
    public void testUpdateUserProfileInMongoDB_networkError() throws ExecutionException, InterruptedException {
        // Given a network error
        when(mockCall.execute()).thenThrow(new java.io.IOException("Network error"));
        
        // When updating user profile in MongoDB
        Task<Void> task = FirestoreManager.getInstance().updateUserProfileInMongoDB(mockFirebaseUser);
        
        // Then the task should still complete successfully (non-critical error)
        Tasks.await(task);
        assertTrue(task.isSuccessful());
    }
    
    /**
     * Test when ApiClient is not initialized
     * This should be treated as non-critical and not fail the task
     */
    @Test
    public void testUpdateUserProfileInMongoDB_apiClientNotInitialized() throws ExecutionException, InterruptedException {
        // Given ApiClient not initialized
        when(ApiClient.isInitialized()).thenReturn(false);
        
        // When updating user profile in MongoDB
        Task<Void> task = FirestoreManager.getInstance().updateUserProfileInMongoDB(mockFirebaseUser);
        
        // Then the task should still complete successfully
        Tasks.await(task);
        assertTrue(task.isSuccessful());
    }
} 