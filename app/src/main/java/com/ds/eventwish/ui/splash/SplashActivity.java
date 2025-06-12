package com.ds.eventwish.ui.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.util.Log;

import com.ds.eventwish.R;
import com.ds.eventwish.ui.MainActivity;
import com.ds.eventwish.data.auth.AuthManager;
import com.ds.eventwish.data.remote.FirestoreManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ds.eventwish.utils.NetworkUtils;
import com.google.firebase.inappmessaging.FirebaseInAppMessaging;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DURATION = 2000; // 2 seconds
    private static final long SIGN_IN_TIMEOUT = 30000; // 30 seconds max for sign-in operations
    private ImageView logoImageView;
    private TextView appNameTextView;
    private ProgressBar loadingProgressBar;
    private ImageView backArrow;
    private SignInButton signInButton;
    private TextView signInStatus;
    private View signInProgressContainer;
    private View signInProgressIndicator;
    private View signInSuccessIcon;
    private AuthManager authManager;
    private ActivityResultLauncher<Intent> signInLauncher;
    private boolean isSigningIn = false;
    private Handler timeoutHandler = new Handler();
    private boolean forceNavigated = false;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make the activity full screen
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        setContentView(R.layout.activity_splash);

        // Pause Firebase In-App Messaging until user is authenticated
        pauseInAppMessaging();

        // Initialize AuthManager
        authManager = AuthManager.getInstance();
        authManager.initialize(this);

        // Initialize views
        logoImageView = findViewById(R.id.logoImageView);
        appNameTextView = findViewById(R.id.appNameTextView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        backArrow = findViewById(R.id.backArrow);
        signInButton = findViewById(R.id.signInButton);
        signInStatus = findViewById(R.id.signInStatus);
        signInProgressContainer = findViewById(R.id.signInProgressContainer);
        signInProgressIndicator = findViewById(R.id.signInProgressIndicator);
        signInSuccessIcon = findViewById(R.id.signInSuccessIcon);

        // Initialize sign-in launcher
        signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::handleSignInResult
        );

        // Set up sign-in button
        signInButton.setOnClickListener(v -> startGoogleSignIn());

        // Set up back arrow
        backArrow.setOnClickListener(v -> navigateToMain());
        
        // Initialize AuthStateListener
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                Log.d(TAG, "AuthStateListener: User is signed in");
            } else {
                Log.d(TAG, "AuthStateListener: User is signed out");
                // Only show sign-in button if we're not already trying to sign in
                if (!isSigningIn && !forceNavigated) {
                    showSignInButton();
                }
            }
        };

        // Start splash flow
        startSplashFlow();
        
        // Set up timeout for splash screen
        setupTimeout();
    }
    
    private void setupTimeout() {
        // Set a maximum timeout for the splash screen
        timeoutHandler.postDelayed(() -> {
            if (!isFinishing() && !forceNavigated) {
                Log.w(TAG, "Splash screen timeout reached");
                
                // Check if user is authenticated
                boolean userAuthenticated = getSharedPreferences("auth_prefs", MODE_PRIVATE)
                    .getBoolean("user_authenticated", false);
                
                // If sign-in button is visible and user is not authenticated, don't force navigate
                if (signInButton.getVisibility() == View.VISIBLE && !userAuthenticated) {
                    Log.d(TAG, "Timeout reached but sign-in button is visible and user not authenticated, waiting for user action");
                    // Don't set forceNavigated flag to allow user to sign in
                    // Just cancel any pending operations
                    if (isSigningIn) {
                        signInStatus.setText(R.string.sign_in_timeout);
                        isSigningIn = false;
                    }
                    return;
                }
                
                // Otherwise, force navigation
                forceNavigated = true;
                
                // Cancel any pending operations
                if (isSigningIn) {
                    signInStatus.setText(R.string.sign_in_timeout);
                    isSigningIn = false;
                }
                
                // Force navigation to main activity
                navigateToMain();
            }
        }, SIGN_IN_TIMEOUT);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Add AuthStateListener
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove AuthStateListener
        if (authStateListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending timeout callbacks
        timeoutHandler.removeCallbacksAndMessages(null);
    }

    private void startSplashFlow() {
        // Start animations
        startSplashAnimations();

        // Try silent sign-in first
        trySilentSignIn();
    }

    private void trySilentSignIn() {
        // First check if user explicitly signed out using SharedPreferences
        boolean userAuthenticated = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            .getBoolean("user_authenticated", false);
            
        if (!userAuthenticated) {
            Log.d(TAG, "trySilentSignIn: User was explicitly signed out, showing sign-in button");
            showSignInButton();
            return;
        }
        
        // Check if user is already signed in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already signed in: " + currentUser.getUid());
            
            // Refresh token to ensure it's valid
            currentUser.getIdToken(true)
                .addOnSuccessListener(tokenResult -> {
                    Log.d(TAG, "Token refresh successful, token length: " + 
                          (tokenResult.getToken() != null ? tokenResult.getToken().length() : 0));
                    onSignInSuccess(currentUser);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Token refresh failed, user may need to re-authenticate", e);
                    // Token refresh failed, user needs to sign in again
                    authManager.signOut();
                    showSignInButton();
                });
        } else {
            // No current user, try silent sign-in
            authManager.trySilentSignIn()
                .addOnSuccessListener(account -> {
                    Log.d(TAG, "Silent sign-in successful");
                    handleGoogleSignInAccount(Tasks.forResult(account));
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Silent sign-in failed, showing sign-in button", e);
                    showSignInButton();
                });
        }
    }

    private void showSignInButton() {
        signInButton.setVisibility(View.VISIBLE);
        signInButton.setEnabled(true);
        signInStatus.setVisibility(View.VISIBLE);
        signInStatus.setText(R.string.sign_in_required);
        loadingProgressBar.setVisibility(View.GONE);
    }

    private void startSplashAnimations() {
        try {
            // Fade in logo
            logoImageView.setAlpha(0f);
            logoImageView.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            // Scale up app name
            appNameTextView.setScaleX(0f);
            appNameTextView.setScaleY(0f);
            appNameTextView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(1000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();

            // Show loading progress
            loadingProgressBar.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error starting splash animations", e);
            // If animations fail, still show the views
            logoImageView.setAlpha(1f);
            appNameTextView.setScaleX(1f);
            appNameTextView.setScaleY(1f);
            loadingProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void navigateToMain() {
        // Check if already navigating to avoid multiple calls
        if (isFinishing() || forceNavigated) return;
        
        // Check if user is authenticated
        boolean userAuthenticated = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            .getBoolean("user_authenticated", false);
        
        // If sign-in button is visible and user is not authenticated, don't navigate
        if (signInButton.getVisibility() == View.VISIBLE && !userAuthenticated) {
            Log.d(TAG, "navigateToMain: Sign-in button is visible and user not authenticated, waiting for user action");
            return;
        }
        
        forceNavigated = true;
        
        // Cancel any pending timeouts
        timeoutHandler.removeCallbacksAndMessages(null);

        try {
            // Fade out animations
            View decorView = getWindow().getDecorView();
            decorView.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            startMainActivity();
                        }
                    })
                    .start();
        } catch (Exception e) {
            Log.e(TAG, "Error during fade out animation", e);
            // If animation fails, just start the main activity
            startMainActivity();
        }
    }

    private void startMainActivity() {
        // Ensure in-app messaging is enabled before leaving
        resumeInAppMessaging();
        
        // Start MainActivity
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        
        // Apply fade out transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        
        // Close splash activity
        finish();
    }

    private void startGoogleSignIn() {
        if (isSigningIn) {
            Log.d(TAG, "startGoogleSignIn: already in progress, ignoring request");
            return;
        }

        Log.d(TAG, "startGoogleSignIn: initiating sign-in flow");
        isSigningIn = true;
        signInButton.setEnabled(false);
        signInStatus.setText(R.string.signing_in);
        showSignInProgress();

        try {
            authManager.getSignInIntent(signInLauncher);
            Log.d(TAG, "startGoogleSignIn: created sign-in intent, launching activity");
        } catch (Exception e) {
            Log.e(TAG, "startGoogleSignIn: failed to start sign-in", e);
            onSignInFailure(e);
        }
    }

    private void showSignInProgress() {
        signInProgressContainer.setVisibility(View.VISIBLE);
        signInProgressIndicator.setVisibility(View.VISIBLE);
        signInSuccessIcon.setVisibility(View.GONE);
        loadingProgressBar.setVisibility(View.GONE);
    }

    private void showSignInSuccess() {
        signInProgressIndicator.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    signInProgressIndicator.setVisibility(View.GONE);
                    signInSuccessIcon.setAlpha(0f);
                    signInSuccessIcon.setVisibility(View.VISIBLE);
                    signInSuccessIcon.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }

    private void handleSignInResult(ActivityResult result) {
        Log.d(TAG, "handleSignInResult: received result with code: " + result.getResultCode() + 
                  ", data present: " + (result.getData() != null));
        
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            try {
                Log.d(TAG, "handleSignInResult: attempting to get account from intent");
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                handleGoogleSignInAccount(task);
            } catch (Exception e) {
                Log.e(TAG, "handleSignInResult: error getting sign in account", e);
                onSignInFailure(new Exception(getString(R.string.sign_in_failed)));
            }
        } else if (result.getResultCode() == RESULT_CANCELED) {
            Log.d(TAG, "handleSignInResult: sign-in was cancelled by user");
            onSignInFailure(new Exception(getString(R.string.sign_in_cancelled)));
        } else {
            Log.e(TAG, "handleSignInResult: sign-in failed with result code: " + result.getResultCode());
            onSignInFailure(new Exception(getString(R.string.sign_in_failed)));
        }
    }

    private void handleGoogleSignInAccount(Task<GoogleSignInAccount> task) {
        try {
            // Add more detailed error logging
            Log.d(TAG, "Processing Google Sign-In result: " + 
                  (task.isSuccessful() ? "Success" : "Failed - " + 
                   (task.getException() != null ? task.getException().getMessage() : "Unknown error")));
            
            // Get Google Sign-In account
            GoogleSignInAccount account = task.getResult(ApiException.class);
            
            // Handle specific error cases
            if (account == null) {
                Log.e(TAG, "Sign-in failed: GoogleSignInAccount is null");
                onSignInFailure(new Exception("GoogleSignInAccount is null"));
                return;
            }
            
            // Extract account information for logging
            String id = account.getId();
            String email = account.getEmail();
            String displayName = account.getDisplayName();
            Log.d(TAG, "Google Sign-In success: ID=" + id + ", email=" + email + ", name=" + displayName);
            
            // Get Firebase credential from Google Sign-In account
            authManager.signInWithGoogle(account)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "Firebase auth with Google successful");
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        onSignInSuccess(user);
                    } else {
                        Log.e(TAG, "Firebase auth successful but user is null");
                        onSignInFailure(new Exception("Firebase user is null after successful auth"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firebase auth with Google failed", e);
                    onSignInFailure(e);
                });
        } catch (ApiException e) {
            // Handle specific API exception errors
            String errorMessage;
            switch (e.getStatusCode()) {
                case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                    errorMessage = "Sign-in was cancelled";
                    break;
                case GoogleSignInStatusCodes.NETWORK_ERROR:
                    errorMessage = "Network error during sign-in";
                    break;
                default:
                    errorMessage = "Sign-in failed: " + e.getStatusCode() + " - " + e.getMessage();
                    break;
            }
            Log.e(TAG, errorMessage, e);
            onSignInFailure(e);
        } catch (Exception e) {
            // Handle any other unexpected exceptions
            Log.e(TAG, "Unexpected error in Google Sign-In", e);
            onSignInFailure(e);
        }
    }

    private void onSignInSuccess(FirebaseUser user) {
        // Show sign-in success UI
        showSignInSuccess();
        
        // Log user information for debugging
        Log.d(TAG, "User sign-in success - UID: " + user.getUid() + 
              ", Display name: " + user.getDisplayName() + 
              ", Email: " + user.getEmail() + 
              ", Is anonymous: " + user.isAnonymous() + 
              ", Provider data count: " + user.getProviderData().size());
              
        for (com.google.firebase.auth.UserInfo profile : user.getProviderData()) {
            Log.d(TAG, "Provider ID: " + profile.getProviderId() + 
                  ", Provider UID: " + profile.getUid() + 
                  ", Provider email: " + profile.getEmail());
        }
        
        // Sync user with MongoDB after successful Firebase authentication
        authManager.syncUserWithMongoDB(user)
            .addOnSuccessListener(mongoUser -> {
                Log.d(TAG, "MongoDB sync successful - User: " + mongoUser.getUid());
                
                // Store authentication state
                getSharedPreferences("auth_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("user_authenticated", true)
                    .apply();
                
                // Resume in-app messaging
                resumeInAppMessaging();
                
                // Add a delay before navigating to main
                new Handler().postDelayed(this::navigateToMain, 1000);
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "MongoDB sync failed, but continuing with Firebase auth", e);
                
                // Store authentication state anyway since Firebase auth was successful
                getSharedPreferences("auth_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("user_authenticated", true)
                    .apply();
                
                // Resume in-app messaging
                resumeInAppMessaging();
                
                // Add a delay before navigating to main
                new Handler().postDelayed(this::navigateToMain, 1000);
            });
    }

    private void onSignInFailure(Exception e) {
        Log.e(TAG, "onSignInFailure: sign-in failed", e);
        isSigningIn = false;
        signInButton.setEnabled(true);
        signInStatus.setText(e.getMessage());
        signInProgressContainer.setVisibility(View.GONE);
        loadingProgressBar.setVisibility(View.GONE);
        
        // Log additional details about the error
        if (e instanceof ApiException) {
            ApiException apiException = (ApiException) e;
            Log.e(TAG, "onSignInFailure: API exception details - " +
                      "Status code: " + apiException.getStatusCode() + 
                      ", Status message: " + apiException.getStatusMessage() + 
                      ", Message: " + apiException.getMessage());
        }
    }

    private void pauseInAppMessaging() {
        try {
            FirebaseInAppMessaging.getInstance().setMessagesSuppressed(true);
            Log.d(TAG, "Firebase In-App Messaging suppressed during sign-in");
        } catch (Exception e) {
            Log.e(TAG, "Failed to suppress In-App Messaging", e);
        }
    }
    
    private void resumeInAppMessaging() {
        try {
            FirebaseInAppMessaging.getInstance().setMessagesSuppressed(false);
            Log.d(TAG, "Firebase In-App Messaging resumed after authentication");
        } catch (Exception e) {
            Log.e(TAG, "Failed to resume In-App Messaging", e);
        }
    }
} 