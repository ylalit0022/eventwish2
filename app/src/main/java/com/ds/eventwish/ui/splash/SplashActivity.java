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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final long SPLASH_DURATION = 2000; // 2 seconds
    private static final long SIGN_IN_TIMEOUT = 15000; // 15 seconds max for sign-in operations
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make the activity full screen
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        setContentView(R.layout.activity_splash);

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

        // Start splash flow
        startSplashFlow();
        
        // Set up timeout for splash screen
        setupTimeout();
    }
    
    private void setupTimeout() {
        // Set a maximum timeout for the splash screen
        timeoutHandler.postDelayed(() -> {
            if (!isFinishing() && !forceNavigated) {
                Log.w(TAG, "Splash screen timeout reached, forcing navigation to main");
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
        Log.d(TAG, "trySilentSignIn: attempting silent sign-in");
        authManager.silentSignIn()
            .addOnSuccessListener(account -> {
                Log.d(TAG, "Silent sign-in successful");
                handleGoogleSignInAccount(Tasks.forResult(account));
            })
            .addOnFailureListener(e -> {
                Log.d(TAG, "Silent sign-in failed, showing sign-in button", e);
                showSignInButton();
            });
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
            Intent signInIntent = authManager.getGoogleSignInClient().getSignInIntent();
            Log.d(TAG, "startGoogleSignIn: created sign-in intent, launching activity");
            signInLauncher.launch(signInIntent);
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
        Log.d(TAG, "handleGoogleSignInAccount: processing sign-in account task");
        try {
            authManager.handleSignInResult(task)
                .addOnSuccessListener(user -> {
                    Log.d(TAG, "handleGoogleSignInAccount: successfully signed in with Firebase, uid: " + user.getUid());
                    onSignInSuccess(user);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "handleGoogleSignInAccount: Firebase auth failed", e);
                    if (e instanceof ApiException) {
                        ApiException apiException = (ApiException) e;
                        int statusCode = apiException.getStatusCode();
                        Log.e(TAG, "handleGoogleSignInAccount: API exception status code: " + statusCode);
                        if (statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                            onSignInFailure(new Exception(getString(R.string.sign_in_cancelled)));
                        } else if (statusCode == GoogleSignInStatusCodes.NETWORK_ERROR) {
                            onSignInFailure(new Exception(getString(R.string.sign_in_network_error)));
                        } else {
                            onSignInFailure(new Exception(getString(R.string.sign_in_failed) + " (code: " + statusCode + ")"));
                        }
                    } else {
                        onSignInFailure(e);
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "handleGoogleSignInAccount: unexpected error", e);
            onSignInFailure(e);
        }
    }

    private void onSignInSuccess(FirebaseUser user) {
        Log.d(TAG, "onSignInSuccess: sign-in completed successfully for user: " + user.getUid() + 
                  ", email: " + user.getEmail() + ", display name: " + user.getDisplayName());
        isSigningIn = false;
        signInStatus.setText(R.string.sign_in_success);
        showSignInSuccess();
        
        // Navigate to main activity after a short delay
        new Handler().postDelayed(() -> {
            Log.d(TAG, "onSignInSuccess: navigating to main activity");
            startMainActivity();
        }, 1500); // Increased delay to show success animation
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
} 