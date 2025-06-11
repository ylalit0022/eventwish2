package com.ds.eventwish;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.ds.eventwish.data.auth.AuthManager;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.FirestoreManager;
import com.ds.eventwish.test.utils.EspressoIdlingResource;
import com.ds.eventwish.ui.splash.SplashActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for Google Sign-In to MongoDB sync flow
 * 
 * NOTE: This test requires:
 * 1. A valid Google account on the test device
 * 2. The app to be properly configured with Firebase
 * 3. The backend server to be running and accessible
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class GoogleSignInToMongoDbTest {

    private IdlingResource idlingResource;
    
    @Before
    public void registerIdlingResource() {
        idlingResource = EspressoIdlingResource.getIdlingResource();
        IdlingRegistry.getInstance().register(idlingResource);
    }
    
    @After
    public void unregisterIdlingResource() {
        if (idlingResource != null) {
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
        
        // Sign out after the test
        try {
            if (AuthManager.getInstance().isSignedIn()) {
                Tasks.await(AuthManager.getInstance().signOut(), 30, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Test the full flow:
     * 1. Launch the app
     * 2. Sign in with Google
     * 3. Verify MongoDB sync is triggered
     * 
     * Note: This test is more of a template since it requires user interaction
     * and a real Google account. In a real test environment, you would use
     * Firebase Test Lab or mock the authentication process.
     */
    @Test
    public void testGoogleSignInToMongoDbSync() throws Exception {
        // Launch the SplashActivity
        ActivityScenario<SplashActivity> scenario = ActivityScenario.launch(SplashActivity.class);
        
        // Wait for UI to load
        Thread.sleep(2000);
        
        // Click the Google Sign-In button
        // Note: This would typically trigger the Google Sign-In UI which requires manual interaction
        // For automated testing, you would need to set up Firebase Test Lab or mock the auth process
        onView(withId(R.id.sign_in_button)).check(matches(isDisplayed()));
        // onView(withId(R.id.sign_in_button)).perform(click());
        
        // For testing purposes, here's how you would verify MongoDB sync after a successful sign-in:
        scenario.onActivity(activity -> {
            // Assume we're signed in
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                try {
                    // Verify MongoDB sync is triggered
                    FirestoreManager firestoreManager = FirestoreManager.getInstance();
                    Tasks.await(firestoreManager.updateUserProfileInMongoDB(user), 30, TimeUnit.SECONDS);
                    
                    // Verify API client is initialized
                    assertTrue("API client should be initialized", ApiClient.isInitialized());
                    
                    // Additional checks:
                    // - Check if user data is sent correctly
                    // - Verify Firebase UID is passed to MongoDB
                    // - Check if profile data (name, email, photo) is synced
                } catch (Exception e) {
                    throw new RuntimeException("MongoDB sync test failed", e);
                }
            }
        });
    }
} 