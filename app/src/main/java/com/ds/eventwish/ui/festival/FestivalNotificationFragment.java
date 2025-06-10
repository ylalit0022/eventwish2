package com.ds.eventwish.ui.festival;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.ds.eventwish.ui.base.BaseFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.model.FestivalTemplate;
import com.ds.eventwish.data.model.Result;
import com.ds.eventwish.ui.festival.adapter.TemplateAdapter;
import com.ds.eventwish.ui.views.OfflineIndicatorView;
import com.ds.eventwish.ui.views.StaleDataIndicatorView;
import com.ds.eventwish.utils.EventWishNotificationManager;
import com.ds.eventwish.utils.NetworkUtils;
import com.ds.eventwish.utils.NotificationPermissionManager;
import com.ds.eventwish.utils.NotificationScheduler;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FestivalNotificationFragment extends BaseFragment {
    private static final String TAG = "FestivalNotification";
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // This fragment requires authentication (enforced by BaseFragment)
    }

    private FestivalViewModel viewModel;
    private LinearLayout festivalsContainer;
    private LinearLayout loadingLayout;
    private LinearLayout errorLayout;
    private LinearLayout emptyLayout;
    private ShimmerFrameLayout shimmerFrameLayout;
    private Button retryButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NavController navController;
    private TextView errorText;
    private TextView errorDescriptionText;
    private Observer<Result<List<Festival>>> festivalsObserver;
    private boolean isDataLoaded = false;
    private NetworkUtils networkUtils;
    private OfflineIndicatorView offlineIndicator;
    private StaleDataIndicatorView staleDataIndicator;
    private Button testNotificationButton;
    
    // Map to store countdown timers to prevent memory leaks
    private Map<String, CountDownTimer> countdownTimers = new HashMap<>();

    public static FestivalNotificationFragment newInstance() {
        return new FestivalNotificationFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_festival_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupViewModel();
        setupObservers();
        setupListeners();
        
        if (!isDataLoaded) {
            loadFestivals();
        }
    }

    private void initializeViews(@NonNull View view) {
        festivalsContainer = view.findViewById(R.id.festivalsContainer);
        loadingLayout = view.findViewById(R.id.loadingLayout);
        errorLayout = view.findViewById(R.id.errorLayout);
        emptyLayout = view.findViewById(R.id.emptyLayout);
        retryButton = view.findViewById(R.id.retryButton);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        errorText = view.findViewById(R.id.errorTextView);
        errorDescriptionText = view.findViewById(R.id.errorDescriptionTextView);
        shimmerFrameLayout = view.findViewById(R.id.shimmerFrameLayout);
        offlineIndicator = view.findViewById(R.id.offlineIndicator);
        staleDataIndicator = view.findViewById(R.id.staleDataIndicator);
        navController = Navigation.findNavController(view);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(requireActivity()).get(FestivalViewModel.class);
        networkUtils = NetworkUtils.getInstance(requireContext());
    }

    private void setupObservers() {
        createFestivalsObserver();
        viewModel.getFestivals().observe(getViewLifecycleOwner(), festivalsObserver);
        
        // Observe network state
        networkUtils.getNetworkAvailability().observe(getViewLifecycleOwner(), this::handleNetworkState);
        
        // Observe stale data
        viewModel.getStaleData().observe(getViewLifecycleOwner(), isStale -> {
            if (staleDataIndicator != null) {
                staleDataIndicator.setVisibility(isStale ? View.VISIBLE : View.GONE);
            }
        });

        // Observe cache snackbar
        viewModel.getShowCacheSnackbar().observe(getViewLifecycleOwner(), this::handleCacheSnackbar);
    }

    private void setupListeners() {
        retryButton.setOnClickListener(v -> loadFestivals());
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "SwipeRefresh triggered");
            viewModel.refreshFestivals();
        });
        
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        if (offlineIndicator != null) {
            offlineIndicator.setRetryListener(this::loadFestivals);
        }
        
        if (staleDataIndicator != null) {
            staleDataIndicator.setRefreshListener(this::loadFestivals);
        }
    }

    private void hideAllStateViews() {
        // Hide all state containers
        errorLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.GONE);
        festivalsContainer.setVisibility(View.GONE);
        
        // Don't hide shimmer here as it's handled separately
        if (!swipeRefreshLayout.isRefreshing()) {
            shimmerFrameLayout.setVisibility(View.GONE);
            shimmerFrameLayout.stopShimmer();
        }
        
        // Hide indicators
        if (offlineIndicator != null) {
            offlineIndicator.setVisibility(View.GONE);
        }
        if (staleDataIndicator != null) {
            staleDataIndicator.setVisibility(View.GONE);
        }
    }

    private void showLoading() {
        hideAllStateViews();
        
        // Only show shimmer if not refreshing
        if (!swipeRefreshLayout.isRefreshing()) {
            shimmerFrameLayout.startShimmer();
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            loadingLayout.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoading() {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
        loadingLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showContent() {
        hideAllStateViews();
        festivalsContainer.setVisibility(View.VISIBLE);
    }

    private void showNetworkError(boolean isNetworkError) {
        hideAllStateViews();
        errorLayout.setVisibility(View.VISIBLE);
        
        if (offlineIndicator != null && isNetworkError) {
            offlineIndicator.setVisibility(View.VISIBLE);
        }
    }

    private void showEmpty() {
        hideAllStateViews();
        emptyLayout.setVisibility(View.VISIBLE);
    }

    private void handleNetworkState(boolean isConnected) {
        if (offlineIndicator != null) {
            offlineIndicator.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        }
        
        // Only handle initial load state
        if (!isDataLoaded) {
            if (!isConnected) {
                loadFestivals(); // Try loading from cache first
            } else {
                refreshFestivals();
            }
        }
    }

    private void handleCacheSnackbar(Boolean showSnackbar) {
        if (showSnackbar != null && showSnackbar && getView() != null) {
            Snackbar.make(getView(), "Showing data from cache", Snackbar.LENGTH_LONG)
                    .setAction("Refresh", v -> refreshFestivals())
                    .show();
            viewModel.clearCacheSnackbarFlag();
        }
    }

    private void createFestivalsObserver() {
        festivalsObserver = result -> {
            Log.d(TAG, "Received result: " + (result.isSuccess() ? "success" : "error"));
            
            if (result.isSuccess()) {
                List<Festival> festivals = result.getData();
                if (festivals != null && !festivals.isEmpty()) {
                    showContent();
                    displayFestivals(festivals);
                    isDataLoaded = true;
                    viewModel.setStaleData(result.isStale());
                } else {
                    showEmpty();
                }
            } else {
                boolean isNetworkError = result.getError() != null && 
                    (result.getError().contains("offline") || 
                     result.getError().contains("network") || 
                     result.getError().contains("connection"));

                if (isDataLoaded && festivalsContainer.getVisibility() == View.VISIBLE) {
                    // Keep showing existing content
                    showContent();
                } else {
                    // Show error state for initial load
                    showNetworkError(isNetworkError);
                    
                    if (isNetworkError) {
                        errorText.setText("No Internet Connection");
                        errorDescriptionText.setText("Please check your internet connection and try again");
                    } else {
                        errorText.setText("Something went wrong");
                        errorDescriptionText.setText("We're having trouble loading festivals. Please try again");
                    }
                }
            }
            
            // Always stop loading states at the end
            hideLoading();
        };
    }

    private void setCategoryIcon(ImageView imageView, Festival festival) {
        if (festival.getCategoryIcon() != null && festival.getCategoryIcon().getCategoryIcon() != null) {
            CategoryIcon icon = festival.getCategoryIcon();
            String iconUrl = icon.getCategoryIcon();
            Log.d(TAG, "Setting category icon - Category: " + festival.getCategory() + ", URL: " + iconUrl);

            // Load the icon from URL using Glide with improved caching
            Glide.with(imageView.getContext())
                    .load(iconUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)) // Cache both original and resized images
                    .centerCrop()
                    .into(imageView);
        } else {
            // Load default icon if no URL is available
            Log.w(TAG, "No icon URL found for category: " + festival.getCategory() +" - ICON URL: "+
                    (festival.getCategoryIcon() != null ? festival.getCategoryIcon().getCategoryIcon() : "null"));
            Glide.with(imageView.getContext())
                    .load(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(imageView);
        }
    }

    private void displayFestivals(List<Festival> festivals) {
        festivalsContainer.removeAllViews();
        Log.d(TAG, "Displaying " + festivals.size() + " festivals");
        
        // Cancel any existing countdown timers
        for (CountDownTimer timer : countdownTimers.values()) {
            if (timer != null) {
                timer.cancel();
            }
        }
        countdownTimers.clear();

        for (Festival festival : festivals) {
            View festivalView = getLayoutInflater().inflate(R.layout.item_festival_category, festivalsContainer, false);

            TextView festivalName = festivalView.findViewById(R.id.festivalName);
            TextView festivalDate = festivalView.findViewById(R.id.festivalDate);
            TextView festivalDescription = festivalView.findViewById(R.id.festivalDescription);
            TextView categoryTitle = festivalView.findViewById(R.id.categoryTitle);
            ImageView categoryIcon = festivalView.findViewById(R.id.categoryIcon);
            RecyclerView templatesRecyclerView = festivalView.findViewById(R.id.templatesRecyclerView);
            
            // Look for the info container to properly position the countdown
            LinearLayout infoContainer = festivalView.findViewById(R.id.festivalInfoContainer);
            
            // Create countdown TextView with proper styling
            TextView countdownView = new TextView(requireContext());
            countdownView.setId(View.generateViewId());
            countdownView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            countdownView.setTextSize(14);
            countdownView.setPadding(0, 4, 0, 8); // Add padding for better spacing
            
            // Set layout params for better positioning
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            countdownView.setLayoutParams(params);
            
            // Add the countdown view to the proper container
            if (infoContainer != null) {
                // Add countdown right after festival date for better grouping
                int insertIndex = -1;
                
                // Find the festival date view to insert after it
                for (int i = 0; i < infoContainer.getChildCount(); i++) {
                    View child = infoContainer.getChildAt(i);
                    if (child.getId() == R.id.festivalDate) {
                        insertIndex = i + 1;
                        break;
                    }
                }
                
                // If festival date was found, insert after it, otherwise add to end
                if (insertIndex != -1) {
                    infoContainer.addView(countdownView, insertIndex);
                } else {
                    infoContainer.addView(countdownView);
                }
                
                Log.d(TAG, "Added countdown view to info container");
            } else {
                // Fallback: look for any LinearLayout to add the countdown
                ViewGroup container = festivalView.findViewById(R.id.festivalContainer);
                if (container instanceof LinearLayout) {
                    // Add the countdown view after the festival name
                    int insertIndex = 1; // Default position
                    
                    // Try to find a good insertion point
                    for (int i = 0; i < container.getChildCount(); i++) {
                        View child = container.getChildAt(i);
                        if (child instanceof TextView && 
                            (child.getId() == R.id.festivalName || child.getId() == R.id.festivalDate)) {
                            insertIndex = i + 1;
                            break;
                        }
                    }
                    
                    ((LinearLayout) container).addView(countdownView, insertIndex);
                    Log.d(TAG, "Added countdown view to main container");
                } else {
                    Log.e(TAG, "Could not find a suitable container for countdown view");
                }
            }

            // Set festival data
            festivalName.setText(festival.getName());

            // Format date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            festivalDate.setText(dateFormat.format(festival.getDate()));

            // Set description
            festivalDescription.setText(festival.getDescription());

            // Set category
            categoryTitle.setText(festival.getCategory());

            // Set category icon
            setCategoryIcon(categoryIcon, festival);
            
            // Set up countdown timer if festival is in the future
            setupCountdownTimer(festival, countdownView);

            // Set up templates recycler view
            if (festival.getTemplates() != null && !festival.getTemplates().isEmpty()) {
                templatesRecyclerView.setVisibility(View.VISIBLE);
                templatesRecyclerView.setLayoutManager(new LinearLayoutManager(
                        requireContext(), LinearLayoutManager.HORIZONTAL, false));
                setupTemplateClickListener(templatesRecyclerView, festival.getTemplates());
            } else {
                templatesRecyclerView.setVisibility(View.GONE);
            }

            festivalsContainer.addView(festivalView);
        }
    }
    
    /**
     * Set up countdown timer for a festival
     * @param festival The festival to set up countdown for
     * @param countdownView The TextView to display countdown
     */
    private void setupCountdownTimer(Festival festival, TextView countdownView) {
        if (countdownView == null) return;
        
        Date festivalDate = festival.getDate();
        Date now = new Date();
        
        // If festival date is in the past, show "Event has passed" message
        if (festivalDate.before(now)) {
            countdownView.setText("Event has passed");
            countdownView.setVisibility(View.VISIBLE);
            return;
        }
        
        // Calculate time difference in milliseconds
        long timeDiff = festivalDate.getTime() - now.getTime();
        
        // If festival is today, show "Today!" message
        Calendar festivalCal = Calendar.getInstance();
        festivalCal.setTime(festivalDate);
        
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTime(now);
        
        if (festivalCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            festivalCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)) {
            countdownView.setText("\u2022 Today! \u2022");
            countdownView.setTextColor(getResources().getColor(R.color.purple_500));
            countdownView.setVisibility(View.VISIBLE);
            return;
        }
        
        // Create and start countdown timer
        CountDownTimer timer = new CountDownTimer(timeDiff, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Calculate days, hours, minutes, seconds
                long days = TimeUnit.MILLISECONDS.toDays(millisUntilFinished);
                millisUntilFinished -= TimeUnit.DAYS.toMillis(days);
                
                long hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished);
                millisUntilFinished -= TimeUnit.HOURS.toMillis(hours);
                
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                millisUntilFinished -= TimeUnit.MINUTES.toMillis(minutes);
                
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished);
                
                // Format countdown text with better readability
                String countdownText;
                if (days > 0) {
                    // For longer time periods, show days and hours
                    countdownText = String.format(Locale.getDefault(), 
                            "\u2022 Coming in %d days %dh \u2022", days, hours);
                } else if (hours > 0) {
                    // For medium time periods, show hours and minutes
                    countdownText = String.format(Locale.getDefault(), 
                            "\u2022 Coming in %d hours %dm \u2022", hours, minutes);
                } else {
                    // For short time periods, show minutes and seconds
                    countdownText = String.format(Locale.getDefault(), 
                            "\u2022 Coming soon: %dm %ds \u2022", minutes, seconds);
                }
                
                // Update countdown view
                countdownView.setText(countdownText);
                countdownView.setVisibility(View.VISIBLE);
            }
            
            @Override
            public void onFinish() {
                countdownView.setText("\u2022 Happening now! \u2022");
                countdownView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                countdownView.setVisibility(View.VISIBLE);
            }
        };
        
        // Start the timer
        timer.start();
        
        // Store the timer to cancel it later
        countdownTimers.put(festival.getId(), timer);
    }

    private void navigateToTemplateDetail(FestivalTemplate template) {
        if (navController == null) {
            navController = Navigation.findNavController(requireView());
        }
        
        // Navigate to template detail fragment
        Bundle args = new Bundle();
        args.putString("templateId", template.getId());
        args.putString("templateName", template.getTitle());
        args.putString("templateContent", template.getContent());
        args.putString("templateImage", template.getImageUrl());
        args.putString("templateCategory", template.getCategory());
        
        Log.d(TAG, "Navigating to template detail with ID: " + template.getId());
        navController.navigate(R.id.action_festival_notification_to_template_detail, args);
    }

    private void setupTemplateClickListener(RecyclerView templatesRecyclerView, List<FestivalTemplate> templates) {
        // Create click listener
        TemplateAdapter.OnTemplateClickListener clickListener = template -> {
            Log.d(TAG, "Template clicked: " + template.getId());
            navigateToTemplateDetail(template);
        };

        // Create adapter with templates and click listener
        TemplateAdapter adapter = new TemplateAdapter(templates, clickListener);
        templatesRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel all countdown timers to prevent memory leaks
        for (CountDownTimer timer : countdownTimers.values()) {
            if (timer != null) {
                timer.cancel();
            }
        }
        countdownTimers.clear();
    }

    @Override
    public void onPause() {
        super.onPause();
        viewModel.clearMemoryCache();
    }

    /**
     * Add a test button for notifications
     */
    private void addTestNotificationButton() {
        // Create a button
        testNotificationButton = new Button(requireContext());
        testNotificationButton.setText("Test Notifications");
        testNotificationButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        
        // Add button to the container
        festivalsContainer.addView(testNotificationButton, 0);
        
        // Set click listener
        testNotificationButton.setOnClickListener(v -> {
            // Check notification permission
            if (NotificationPermissionManager.hasNotificationPermission(requireContext())) {
                // Show test notification
                showTestNotification();
            } else {
                // Request notification permission
                NotificationPermissionManager.requestNotificationPermission(requireActivity());
            }
        });
    }
    
    /**
     * Show a test notification
     */
    private void showTestNotification() {
        // Get the first festival from the list
        List<Festival> festivals = viewModel.getFestivals().getValue().getData();
        
        if (festivals != null && !festivals.isEmpty()) {
            Festival festival = festivals.get(0);
            
            // Show a notification for this festival
            int notificationId = EventWishNotificationManager.showFestivalNotification(
                    requireContext(),
                    festival,
                    0); // 0 days until (today)
            
            if (notificationId != -1) {
                Toast.makeText(requireContext(), 
                        "Test notification sent for " + festival.getName(), 
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), 
                        "Failed to send test notification", 
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // No festivals available, run the worker instead
            NotificationScheduler.runFestivalNotificationsNow(requireContext());
            
            Toast.makeText(requireContext(), 
                    "Running notification worker...", 
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshFestivals() {
        if (!swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
            viewModel.refreshFestivals();
        }
    }

    private void loadFestivals() {
        hideAllStateViews();
        showLoading();
        viewModel.loadFestivals();
    }
}