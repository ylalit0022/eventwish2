package com.ds.eventwish.data.auth;

import android.content.Context;
import android.util.Log;

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

/**
 * Manages authentication state and Google Sign-In
 */
public class AuthManager {
    private static final String TAG = "AuthManager";
    private static volatile AuthManager instance;
    private final FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private boolean isInitialized = false;
    private Context applicationContext;

    private AuthManager() {
        auth = FirebaseAuth.getInstance();
    }

    public static AuthManager getInstance() {
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
     * Initialize the AuthManager with application context
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
    public Task<GoogleSignInAccount> silentSignIn() {
        if (!isInitialized) {
            Log.e(TAG, "silentSignIn: AuthManager not initialized");
            return Tasks.forException(new IllegalStateException("AuthManager not initialized"));
        }

        Log.d(TAG, "silentSignIn: attempting silent sign-in");
        GoogleSignInAccount lastAccount = GoogleSignIn.getLastSignedInAccount(applicationContext);
        if (lastAccount != null) {
            Log.d(TAG, "silentSignIn: found last signed in account, email: " + lastAccount.getEmail() + ", id: " + lastAccount.getId());
            return Tasks.forResult(lastAccount);
        }
        Log.d(TAG, "silentSignIn: no cached account found, trying silent sign-in");

        return googleSignInClient.silentSignIn()
            .addOnSuccessListener(account -> {
                Log.d(TAG, "silentSignIn: successful, email: " + account.getEmail() + ", id: " + account.getId());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "silentSignIn: failed with exception", e);
                if (e instanceof ApiException) {
                    ApiException apiException = (ApiException) e;
                    Log.e(TAG, "silentSignIn: API exception status code: " + apiException.getStatusCode() + 
                              ", message: " + apiException.getMessage());
                }
            });
    }

    /**
     * Get Google Sign In Client
     */
    public GoogleSignInClient getGoogleSignInClient() {
        checkInitialized();
        return googleSignInClient;
    }

    /**
     * Handle Google Sign In Result
     */
    public Task<FirebaseUser> handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        Log.d(TAG, "handleSignInResult: starting to handle sign-in result");
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account == null) {
                Log.e(TAG, "handleSignInResult: account is null");
                return Tasks.forException(new ApiException(new Status(CommonStatusCodes.ERROR)));
            }
            Log.d(TAG, "handleSignInResult: Google sign in successful, email: " + account.getEmail() + 
                      ", id: " + account.getId() + ", has idToken: " + (account.getIdToken() != null));
            return firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            Log.e(TAG, "handleSignInResult: failed with status code: " + e.getStatusCode() + 
                      ", status message: " + e.getStatusMessage() + 
                      ", error message: " + e.getMessage());
            return Tasks.forException(e);
        } catch (Exception e) {
            Log.e(TAG, "handleSignInResult: unexpected error", e);
            return Tasks.forException(e);
        }
    }

    /**
     * Sign out from both Firebase and Google
     */
    public Task<Void> signOut() {
        if (!isInitialized) {
            Log.e(TAG, "signOut: not initialized");
            return Tasks.forException(new IllegalStateException("AuthManager not initialized"));
        }

        Log.d(TAG, "signOut: signing out");
        return Tasks.whenAll(
            googleSignInClient.signOut(),
            Tasks.call(() -> {
                auth.signOut();
                return null;
            })
        ).addOnCompleteListener(task -> {
            Log.d(TAG, "signOut: completed, success=" + task.isSuccessful());
            
            // Clear authentication state in AuthStateManager
            if (applicationContext != null) {
                com.ds.eventwish.utils.AuthStateManager.getInstance(applicationContext).clearAuthentication();
                
                // Also clear old SharedPreferences for backward compatibility
                applicationContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("user_authenticated", false)
                    .remove("user_id")
                    .apply();
            }
        });
    }

    /**
     * Authenticate with Firebase using Google credentials
     */
    private Task<FirebaseUser> firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle: starting Firebase auth, account id: " + acct.getId() + 
                  ", email: " + acct.getEmail() + ", display name: " + acct.getDisplayName());

        String idToken = acct.getIdToken();
        if (idToken == null) {
            Log.e(TAG, "firebaseAuthWithGoogle: ID token is null");
            return Tasks.forException(new IllegalStateException("ID token is null"));
        }

        // Sign in with Google credential
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        Log.d(TAG, "firebaseAuthWithGoogle: created AuthCredential, starting Firebase signIn");

        return auth.signInWithCredential(credential)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        Log.d(TAG, "firebaseAuthWithGoogle: success, Firebase uid: " + user.getUid() + 
                                  ", email: " + user.getEmail() + ", display name: " + user.getDisplayName());
                        return user;
                    } else {
                        Log.e(TAG, "firebaseAuthWithGoogle: failure", task.getException());
                        throw task.getException();
                    }
                });
    }

    private void checkInitialized() {
        if (!isInitialized) {
            throw new IllegalStateException("AuthManager must be initialized before use");
        }
    }
    
    /**
     * Force refresh of ID token
     * @return Task with token string result
     */
    public Task<String> refreshIdToken() {
        Log.d(TAG, "refreshIdToken: attempting to refresh ID token");
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "refreshIdToken: no user logged in");
            return Tasks.forException(new Exception("No user logged in"));
        }
        
        return user.getIdToken(true)
            .continueWith(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Log.d(TAG, "refreshIdToken: token refresh successful");
                    return task.getResult().getToken();
                }
                Log.e(TAG, "refreshIdToken: failed to refresh token", task.getException());
                throw new Exception("Failed to refresh token");
            });
    }
} 