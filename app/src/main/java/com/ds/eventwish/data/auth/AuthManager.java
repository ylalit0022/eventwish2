package com.ds.eventwish.data.auth;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import com.ds.eventwish.data.model.User;
import com.ds.eventwish.data.repository.UserRepository;

/**
 * Manages authentication state and Google Sign-In
 */
public class AuthManager {
    private static final String TAG = "AuthManager";

    private static volatile AuthManager instance;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private GoogleSignInClient googleSignInClient;
    private Context applicationContext;
    private boolean isInitialized = false;

    /**
     * Get singleton instance
     */
    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            synchronized (AuthManager.class) {
                if (instance == null) {
                    instance = new AuthManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize with context
     */
    public void initialize(Context context) {
        if (isInitialized) {
            Log.d(TAG, "initialize: already initialized");
            return;
        }

        Log.d(TAG, "initialize: starting with context: " + context);
        applicationContext = context.getApplicationContext();

        try {
            // Configure Google Sign In
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(com.ds.eventwish.R.string.google_web_client_id))
                    .requestEmail()
                    .build();
            Log.d(TAG, "initialize: created GoogleSignInOptions");

            googleSignInClient = GoogleSignIn.getClient(context, gso);
            Log.d(TAG, "initialize: created GoogleSignInClient");
            
            // Check if there's a previously signed in account
            GoogleSignInAccount lastAccount = GoogleSignIn.getLastSignedInAccount(context);
            if (lastAccount != null) {
                Log.d(TAG, "initialize: found previously signed in account: " + lastAccount.getEmail());
            } else {
                Log.d(TAG, "initialize: no previously signed in account found");
            }

            isInitialized = true;
            Log.d(TAG, "initialize: completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "initialize: failed to initialize", e);
            throw e;
        }
    }

    /**
     * Check if user is already signed in
     */
    public boolean isSignedIn() {
        return auth.getCurrentUser() != null && GoogleSignIn.getLastSignedInAccount(applicationContext) != null;
    }

    /**
     * Get current Firebase user
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Try silent sign in
     */
    public Task<GoogleSignInAccount> trySilentSignIn() {
        if (!isInitialized) {
            Log.e(TAG, "trySilentSignIn: not initialized");
            return Tasks.forException(new IllegalStateException("AuthManager not initialized"));
        }

        Log.d(TAG, "trySilentSignIn: attempting silent sign in");
        return googleSignInClient.silentSignIn();
    }

    /**
     * Sign out
     */
    public Task<Void> signOut() {
        if (!isInitialized) {
            Log.e(TAG, "signOut: not initialized");
            return Tasks.forException(new IllegalStateException("AuthManager not initialized"));
        }

        Log.d(TAG, "signOut: signing out");
        
        // First sign out from Firebase
        auth.signOut();
        
        // Then sign out from Google
        return googleSignInClient.signOut()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "signOut: success");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "signOut: failure", e);
            });
    }

    /**
     * Get Google sign in intent
     */
    public Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount> getSignInIntent(ActivityResultLauncher<Intent> launcher) {
        if (!isInitialized) {
            Log.e(TAG, "getSignInIntent: not initialized");
            return Tasks.forException(new IllegalStateException("AuthManager not initialized"));
        }

        Log.d(TAG, "getSignInIntent: launching sign in intent");
        launcher.launch(googleSignInClient.getSignInIntent());
        
        // This method doesn't actually return the account, it just launches the intent
        // The account will be returned in onActivityResult
        return Tasks.forCanceled();
    }

    /**
     * Handle sign in result
     */
    public Task<GoogleSignInAccount> handleSignInResult(android.content.Intent data) {
        if (!isInitialized) {
            Log.e(TAG, "handleSignInResult: not initialized");
            return Tasks.forException(new IllegalStateException("AuthManager not initialized"));
        }

        Log.d(TAG, "handleSignInResult: processing sign in result");
        
        try {
            // Parse the result
            com.google.android.gms.tasks.Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            
            // Get the account
            GoogleSignInAccount account = task.getResult(ApiException.class);
            
            if (account != null) {
                Log.d(TAG, "handleSignInResult: success, email: " + account.getEmail());
                return Tasks.forResult(account);
            } else {
                Log.e(TAG, "handleSignInResult: account is null");
                return Tasks.forException(new ApiException(new Status(CommonStatusCodes.INTERNAL_ERROR)));
            }
        } catch (ApiException e) {
            // Log the error code
            Log.e(TAG, "handleSignInResult: failed with status code: " + e.getStatusCode());
            
            // Special handling for cancelled sign-in
            if (e.getStatusCode() == CommonStatusCodes.CANCELED) {
                Log.d(TAG, "handleSignInResult: user cancelled sign in");
            }
            
            return Tasks.forException(e);
        }
    }

    /**
     * Sign in with Google
     */
    public Task<com.google.firebase.auth.AuthResult> signInWithGoogle(GoogleSignInAccount account) {
        Log.d(TAG, "signInWithGoogle: starting sign-in with Google account: " + account.getEmail());
        
        if (account == null) {
            Log.e(TAG, "signInWithGoogle: account is null");
            return Tasks.forException(new IllegalArgumentException("GoogleSignInAccount cannot be null"));
        }
        
        String idToken = account.getIdToken();
        if (idToken == null) {
            Log.e(TAG, "signInWithGoogle: ID token is null");
            return Tasks.forException(new IllegalStateException("ID token is null"));
        }
        
        // Sign in with Google credential
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        Log.d(TAG, "signInWithGoogle: created AuthCredential, starting Firebase signIn");
        
        return auth.signInWithCredential(credential)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                Log.d(TAG, "signInWithGoogle: success, Firebase uid: " + user.getUid() + 
                      ", email: " + user.getEmail() + ", display name: " + user.getDisplayName());
                
                // Sync with MongoDB after successful Firebase authentication
                syncUserWithMongoDB(user);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "signInWithGoogle: failure", e);
            });
    }
    
    /**
     * Sync user with MongoDB after successful Firebase authentication
     * @param firebaseUser Firebase user object
     * @return Task representing the operation
     */
    public Task<User> syncUserWithMongoDB(FirebaseUser firebaseUser) {
        if (applicationContext == null) {
            Log.e(TAG, "syncUserWithMongoDB: applicationContext is null");
            return Tasks.forException(new IllegalStateException("AuthManager not properly initialized"));
        }
        
        Log.d(TAG, "syncUserWithMongoDB: syncing user with MongoDB: " + firebaseUser.getUid());
        
        // Get UserRepository instance
        UserRepository userRepository = UserRepository.getInstance(applicationContext);
        
        // Call the sync method
        return userRepository.syncUserWithMongoDB(firebaseUser)
            .addOnSuccessListener(user -> {
                Log.d(TAG, "syncUserWithMongoDB: success, MongoDB user: " + user.getUid());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "syncUserWithMongoDB: failure", e);
            });
    }
    
    /**
     * Force refresh of ID token
     * @return Task with token string result
     */
    public Task<String> refreshIdToken() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "refreshIdToken: no user signed in");
            return Tasks.forException(new IllegalStateException("No user signed in"));
        }

        Log.d(TAG, "refreshIdToken: refreshing token for user: " + user.getUid());
        
        return user.getIdToken(true)
            .continueWith(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String token = task.getResult().getToken();
                    Log.d(TAG, "refreshIdToken: success, token length: " + (token != null ? token.length() : 0));
                    return token;
                } else {
                    Log.e(TAG, "refreshIdToken: failed to get token", task.getException());
                    throw new Exception("Failed to refresh token", task.getException());
                }
            });
    }
} 