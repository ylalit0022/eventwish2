package com.ds.eventwish.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentHomeBinding;
import com.ds.eventwish.data.model.Category;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.ui.base.BaseFragment;
import com.ds.eventwish.ui.home.adapter.TemplateAdapter;
import com.ds.eventwish.ui.home.adapter.CategoriesAdapter;
import com.ds.eventwish.ui.viewmodel.CoinsViewModel;
import com.ds.eventwish.ui.dialog.UnifiedAdRewardDialog;
import com.ds.eventwish.ui.festival.FestivalViewModel;
import com.ds.eventwish.data.repository.CategoryIconRepository;
import com.ds.eventwish.ui.views.OfflineIndicatorView;
import com.ds.eventwish.ui.views.StaleDataIndicatorView;
import com.ds.eventwish.utils.NetworkUtils;
import com.ds.eventwish.utils.FeatureManager;

import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.JustifyContent;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexWrap;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeFragment extends BaseFragment implements TemplateAdapter.OnTemplateClickListener {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private FestivalViewModel festivalViewModel;
    private CoinsViewModel coinsViewModel;
    private TemplateAdapter adapter;
    private CategoriesAdapter categoriesAdapter;
    private GridLayoutManager layoutManager;
    private static final int VISIBLE_THRESHOLD = 5;
    private BottomNavigationView bottomNav;
    private long backPressedTime;
    private static final long BACK_PRESS_DELAY = 2000; // 2 seconds
    private CategoryIconRepository categoryIconRepository;
    private boolean wasInBackground = false;
    private NetworkUtils networkUtils;
    private FeatureManager featureManager;

    private androidx.lifecycle.Observer<Integer> coinsObserver;
    private MenuItem coinsMenuItem;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize view models
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        festivalViewModel = new ViewModelProvider(requireActivity()).get(FestivalViewModel.class);
        
        // Initialize CoinsViewModel
        try {
            coinsViewModel = new ViewModelProvider(requireActivity()).get(CoinsViewModel.class);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing CoinsViewModel", e);
            coinsViewModel = new ViewModelProvider(this).get(CoinsViewModel.class);
        }
        
        // Initialize other components
        categoryIconRepository = CategoryIconRepository.getInstance();
        networkUtils = NetworkUtils.getInstance(requireContext());
        featureManager = FeatureManager.getInstance(requireContext());
        
        // Setup UI components
        setupRecyclerView();
        setupUi();
        setupObservers();
        setupMenuProvider();
        setupCoinsObserver();
    }

    private void setupRecyclerView() {
        adapter = new TemplateAdapter(this);
        layoutManager = new GridLayoutManager(requireContext(), 1);
        binding.templatesRecyclerView.setLayoutManager(layoutManager);
        binding.templatesRecyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        // Observe templates
        viewModel.getTemplates().observe(getViewLifecycleOwner(), templates -> {
            Log.d(TAG, "Templates updated: " + (templates != null ? templates.size() : 0));
            if (templates != null) {
                adapter.updateTemplates(templates);
                viewModel.checkForNewTemplates(templates);
                
                // Hide loader if needed
                if (binding.swipeRefreshLayout.isRefreshing()) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
                
                // Show or hide empty view
                if (templates.isEmpty()) {
                    binding.emptyView.setVisibility(View.VISIBLE);
                } else {
                    binding.emptyView.setVisibility(View.GONE);
                }
            }
        });
        
        // Observe new template IDs
        viewModel.getNewTemplateIds().observe(getViewLifecycleOwner(), newTemplateIds -> {
            Log.d(TAG, "New template IDs updated: " + (newTemplateIds != null ? newTemplateIds.size() : 0));
            if (adapter != null) {
                adapter.setNewTemplates(newTemplateIds);
            }
        });
        
        // Observe loading state
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "Loading state changed: " + isLoading);
            if (isLoading) {
                // Show loading indicators only if we don't have data yet
                if (adapter.getItemCount() == 0) {
                    /*
                    binding.shimmerFrameLayout.setVisibility(View.VISIBLE);
                    binding.shimmerFrameLayout.startShimmer();
                    */
                    // Use progress indicator instead
                    binding.loadingProgressBar.setVisibility(View.VISIBLE);
                }
            } else {
                /*
                binding.shimmerFrameLayout.stopShimmer();
                binding.shimmerFrameLayout.setVisibility(View.GONE);
                */
                binding.loadingProgressBar.setVisibility(View.GONE);
                
                // Make sure swipe refresh is not stuck
                if (binding.swipeRefreshLayout.isRefreshing()) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
        
        // Observe categories
        viewModel.getCategoryObjects().observe(getViewLifecycleOwner(), categories -> {
            Log.d(TAG, "Categories updated: " + (categories != null ? categories.size() : 0));
            if (categories != null && !categories.isEmpty() && categoriesAdapter != null) {
                categoriesAdapter.updateCategories(categories);
                
                // Restore selected category if any
                String selectedCategory = viewModel.getSelectedCategory();
                if (selectedCategory != null) {
                    categoriesAdapter.updateSelectedCategory(selectedCategory);
                }
            }
        });
        
        // Set up SwipeRefreshLayout
        setupSwipeRefresh();
        
        // Set up offline and stale data indicators
        setupIndicators();

        // Set up bottom navigation
        setupBottomNavigation();

        // Ensure filter chips are initially hidden
        binding.filterChipsScrollView.setVisibility(View.GONE);
        binding.timeFilterScrollView.setVisibility(View.GONE);

        // Explicitly load categories to ensure they're displayed
        Log.d(TAG, "Explicitly loading categories in onViewCreated");
        viewModel.loadCategories();

        // Ensure categories are visible
        ensureCategoriesVisible();
    }

    private void setupMenuProvider() {
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_home, menu);
                
                // Get the coins menu item
                coinsMenuItem = menu.findItem(R.id.action_ads);
                
                // Add API tester menu item
                MenuItem apiTesterItem = menu.add(Menu.NONE, R.id.action_api_tester, Menu.NONE, "API Tester");
                apiTesterItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                
                // Add database log menu item
                MenuItem dbLogItem = menu.add(Menu.NONE, R.id.action_db_log, Menu.NONE, "Database Log");
                dbLogItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                
                // Add premium features menu item
                MenuItem premiumFeaturesItem = menu.add(Menu.NONE, R.id.action_premium_features, Menu.NONE, "Premium Features");
                premiumFeaturesItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                
                // Update the coins menu item with current value
                updateCoinsMenuItem();
            }
            
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                // Handle menu item selection
                if (menuItem.getItemId() == R.id.action_ads) {
                    showAdRewardDialog();
                    return true;
                } else if (menuItem.getItemId() == R.id.action_api_tester) {
                    // ... existing code
                } else if (menuItem.getItemId() == R.id.action_premium_features) {
                    showPremiumFeaturesDialog();
                    return true;
                }
                
                return false;
            }
        };
        
        requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), androidx.lifecycle.Lifecycle.State.RESUMED);
    }

    /**
     * Update the coins menu item title with animation
     */
    private void updateCoinsMenuItem() {
        try {
            if (coinsMenuItem != null && isAdded() && !isDetached()) {
                // Get current coins
                int coins = 0;
                if (coinsViewModel != null) {
                    try {
                        coins = coinsViewModel.getCurrentCoins();
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting current coins count", e);
                    }
                }
                
                final int coinValue = coins;
                Log.d(TAG, "Updating coins menu item to: " + coinValue);
                
                // Run on UI thread to avoid exceptions
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            // Set the updated title
                            String coinsText = getString(R.string.coins_format, coinValue);
                            
                            // Check if the value changed from previous
                            String currentText = coinsMenuItem.getTitle().toString();
                            boolean valueChanged = !currentText.equals(coinsText);
                            
                            // Set the new text
                            coinsMenuItem.setTitle(coinsText);
                            
                            // Animate icon if value changed to draw attention
                            if (valueChanged && coinsMenuItem.getActionView() != null) {
                                Log.d(TAG, "Animating coins icon due to value change");
                                
                                View actionView = coinsMenuItem.getActionView();
                                
                                // Scale animation
                                actionView.animate()
                                    .scaleX(1.5f)
                                    .scaleY(1.5f)
                                    .setDuration(200)
                                    .withEndAction(() -> {
                                        // Scale back to normal
                                        actionView.animate()
                                            .scaleX(1.0f)
                                            .scaleY(1.0f)
                                            .setDuration(200)
                                            .start();
                                    })
                                    .start();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating coins menu item", e);
                        }
                    });
                } else {
                    Log.d(TAG, "Skip updating coins menu item - not attached or menu not created yet");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in updateCoinsMenuItem", e);
        }
    }
    
    /**
     * Setup the coins observer
     */
    private void setupCoinsObserver() {
        Log.d(TAG, "Setting up coins observer");
        
        if (coinsViewModel == null) {
            try {
                coinsViewModel = new ViewModelProvider(requireActivity()).get(CoinsViewModel.class);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing CoinsViewModel", e);
                coinsViewModel = new ViewModelProvider(this).get(CoinsViewModel.class);
            }
        }
        
        // Remove any existing observer to avoid duplicates
        if (coinsObserver != null) {
            Log.d(TAG, "Removing existing coins observer");
            coinsViewModel.getCoinsLiveData().removeObserver(coinsObserver);
        }
        
        // Create a new coins observer that updates the menu
        coinsObserver = coins -> {
            Log.d(TAG, "Coins observer triggered with value: " + coins);
            updateCoinsMenuItem();
        };
        
        // Register observer with the view lifecycle owner
        coinsViewModel.getCoinsLiveData().observe(getViewLifecycleOwner(), coinsObserver);
        
        // Also observe coin update events for guaranteed updates
        coinsViewModel.getCoinUpdateEvent().observe(getViewLifecycleOwner(), coins -> {
            Log.d(TAG, "Coin update event received in HomeFragment: " + coins);
            updateCoinsMenuItem();
        });
        
        // Force a refresh to ensure we have the latest value
        Log.d(TAG, "Initial background refresh in setupCoinsObserver");
        coinsViewModel.forceBackgroundRefresh(success -> {
            Log.d(TAG, "Initial coins background refresh completed: " + (success ? "success" : "failed"));
            
            // Update UI immediately even if refresh failed
            if (isAdded() && !isDetached()) {
                updateCoinsMenuItem();
            }
        });
        
        // Schedule periodic refreshes
        startPeriodicCoinsRefresh();
    }
    
    // Handler for periodic refreshes
    private final Handler periodicRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable periodicRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && !isDetached() && coinsViewModel != null) {
                Log.d(TAG, "Running periodic coins refresh");
                
                // Use standard refresh for periodic updates to reduce server load
                coinsViewModel.refreshCoinsCount(success -> {
                    Log.d(TAG, "Periodic refresh completed: " + (success ? "success" : "failed"));
                });
                
                // Schedule next refresh
                periodicRefreshHandler.postDelayed(this, 60000); // 1 minute
            }
        }
    };
    
    /**
     * Start periodic refreshes of coins data
     */
    private void startPeriodicCoinsRefresh() {
        // Remove any existing callbacks to avoid duplicates
        periodicRefreshHandler.removeCallbacks(periodicRefreshRunnable);
        
        // Schedule first refresh after a delay
        periodicRefreshHandler.postDelayed(periodicRefreshRunnable, 60000); // 1 minute delay
        
        Log.d(TAG, "Scheduled periodic coins refresh");
    }
    
    /**
     * Stop periodic refreshes to prevent memory leaks
     */
    private void stopPeriodicCoinsRefresh() {
        periodicRefreshHandler.removeCallbacks(periodicRefreshRunnable);
        Log.d(TAG, "Stopped periodic coins refresh");
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        Log.d(TAG, "onResume called");
        
        // Original onResume functionality
        if (viewModel != null) {
            // Clear any stale data flag to fetch fresh data
            viewModel.setStaleData(false);
            
            // Only load templates if we have no templates or it's been a while since the last refresh
            if (!viewModel.hasTemplates() || viewModel.isRefreshNeeded()) {
                viewModel.loadTemplates(false);
            }
        }
        
        // New functionality: Full background refresh for coins when returning to fragment
        if (coinsViewModel != null) {
            Log.d(TAG, "Full background refresh for coins in onResume");
            try {
                // Use background refresh for more reliable results
                coinsViewModel.forceBackgroundRefresh(success -> {
                    Log.d(TAG, "Coins background refresh in onResume completed: " + (success ? "success" : "failed"));
                    
                    // Always update UI regardless of refresh result
                    if (isAdded() && !isDetached()) {
                        updateCoinsMenuItem();
                    }
                });
                
                // Restart periodic refresh
                stopPeriodicCoinsRefresh();
                startPeriodicCoinsRefresh();
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing coins in onResume", e);
                
                // Try a fallback refresh if background refresh failed
                coinsViewModel.refreshCoinsCount();
            }
        }
        
        // Check if we're coming back from another fragment
        if (wasInBackground) {
            Log.d(TAG, "Returning from background");
            
            // Refresh category icons
            if (categoryIconRepository != null) {
                categoryIconRepository.refreshCategoryIcons();
            }
            
            // Always load categories when returning to the fragment
            if (viewModel != null) {
                Log.d(TAG, "Loading categories in onResume");
                viewModel.loadCategories();
            }
            
            // Restore selected category
            if (categoriesAdapter != null && viewModel != null) {
                String selectedCategory = viewModel.getSelectedCategory();
                Log.d(TAG, "Restoring selected category: " + (selectedCategory != null ? selectedCategory : "All"));
                
                // Update the selected category in the adapter
                categoriesAdapter.updateSelectedCategory(selectedCategory);
                
                // Make sure the categories adapter is refreshed
                categoriesAdapter.notifyDataSetChanged();
            }
            
            // Refresh templates if needed
            if (viewModel != null && viewModel.shouldRefreshOnReturn()) {
                viewModel.loadTemplates(true);
            }
            
            // Update chip selections based on current filters
            updateChipSelections();
            
            // Restore scroll position if needed
            if (layoutManager != null && viewModel != null) {
                int lastPosition = viewModel.getLastVisiblePosition();
                if (lastPosition > 0) {
                    layoutManager.scrollToPosition(lastPosition);
                }
            }
            
            wasInBackground = false;
        }
        
        // Ensure categories are visible
        ensureCategoriesVisible();
        
        // Check for unread festivals - using the correct method
        if (festivalViewModel != null) {
            // Refresh festivals to check for new ones
            festivalViewModel.refreshFestivals();
            
            // Update the notification badge based on unread count
            festivalViewModel.getUnreadCount().observe(getViewLifecycleOwner(), count -> {
                if (count != null && count > 0) {
                    binding.notificationBadge.setVisibility(View.VISIBLE);
                    binding.notificationBadge.setText(count <= 9 ? String.valueOf(count) : "9+");
                } else {
                    binding.notificationBadge.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        
        Log.d(TAG, "onPause called");
        
        // Save the current scroll position
        int position = layoutManager.findFirstVisibleItemPosition();
        if (position >= 0) {
            viewModel.saveScrollPosition(position);
            Log.d(TAG, "Saved scroll position in onPause: " + position);
        }
        
        // Save the current page for pagination
        if (viewModel.getCurrentPage() > 1) {
            Log.d(TAG, "Saved current page in onPause: " + viewModel.getCurrentPage());
        }
        
        // Mark that we're going to background
        wasInBackground = true;
    }

    private void setupUi() {
        // Setup header icons
        binding.refreshIcon.setOnClickListener(v -> {
            // Show loading indicator
            binding.swipeRefreshLayout.setRefreshing(true);
            
            // Clear cache and refresh data
            viewModel.clearCacheAndRefresh();
            
            // Hide the refresh indicator
            binding.refreshIndicator.setVisibility(View.GONE);
        });
        
        // Setup test button for debugging
        binding.refreshIcon.setOnLongClickListener(v -> {
            // Toggle the indicator for testing
            boolean currentState = binding.refreshIndicator.getVisibility() == View.VISIBLE;
            binding.refreshIndicator.setVisibility(currentState ? View.GONE : View.VISIBLE);
            
            // Show a toast with the current state
            Toast.makeText(requireContext(), 
                          "Indicator " + (currentState ? "hidden" : "shown") + " for testing", 
                          Toast.LENGTH_SHORT).show();
            
            return true;
        });
        
        // Set up notification badge click listener
        binding.notificationIcon.setOnClickListener(v -> {
            // Navigate to notification fragment
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.action_home_to_festival_notification);
            
            // Mark all festivals as read
            festivalViewModel.markAllAsRead();
            
            // Hide the badge immediately
            binding.notificationBadge.setVisibility(View.GONE);
        });
        
        // Observe unread festival count
        festivalViewModel.getUnreadCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                binding.notificationBadge.setVisibility(View.VISIBLE);
                if (count <= 9) {
                    binding.notificationBadge.setText(String.valueOf(count));
                } else {
                    binding.notificationBadge.setText("9+");
                }
            } else {
                binding.notificationBadge.setVisibility(View.GONE);
            }
        });

        setupCategoriesRecyclerView();

        // Set up filter chips
        setupFilterChips();
        
        // Set up filter icon
        ImageView filterIcon = binding.filterIcon;
        filterIcon.setOnClickListener(v -> {
            // Check if filter chips are visible
            boolean areChipsVisible = binding.filterChipsScrollView.getVisibility() == View.VISIBLE;
            
            if (areChipsVisible) {
                // Hide filter chips
                binding.filterChipsScrollView.setVisibility(View.GONE);
                binding.timeFilterScrollView.setVisibility(View.GONE);
            } else {
                // Show filter chips
                binding.filterChipsScrollView.setVisibility(View.VISIBLE);
                binding.timeFilterScrollView.setVisibility(View.VISIBLE);
                
                // Update chip selections to reflect current state
                updateChipSelections();
            }
        });
        
        // Set up long click listener to show the bottom sheet for advanced filtering
        filterIcon.setOnLongClickListener(v -> {
            showFilterBottomSheet();
            return true;
        });
    }

    private void setupFilterChips() {
        // Set up sort option chips
        binding.chipTrending.setOnClickListener(v -> 
            viewModel.setSortOption(HomeViewModel.SortOption.TRENDING));
        binding.chipNewest.setOnClickListener(v -> 
            viewModel.setSortOption(HomeViewModel.SortOption.NEWEST));
        binding.chipOldest.setOnClickListener(v -> 
            viewModel.setSortOption(HomeViewModel.SortOption.OLDEST));
        binding.chipMostUsed.setOnClickListener(v -> 
            viewModel.setSortOption(HomeViewModel.SortOption.MOST_USED));
        
        // Set up time filter chips
        binding.chipAllTime.setOnClickListener(v -> 
            viewModel.setTimeFilter(HomeViewModel.TimeFilter.ALL_TIME));
        binding.chipToday.setOnClickListener(v -> 
            viewModel.setTimeFilter(HomeViewModel.TimeFilter.TODAY));
        binding.chipThisWeek.setOnClickListener(v -> 
            viewModel.setTimeFilter(HomeViewModel.TimeFilter.THIS_WEEK));
        binding.chipThisMonth.setOnClickListener(v -> 
            viewModel.setTimeFilter(HomeViewModel.TimeFilter.THIS_MONTH));
        
        // Add a button to show advanced filter options
        if (binding.advancedFilterButton != null) {
            binding.advancedFilterButton.setOnClickListener(v -> showFilterBottomSheet());
        }
        
        updateChipSelections();
    }
    
    private void updateChipSelections() {
        // Update sort option chips
        binding.chipTrending.setChecked(viewModel.getCurrentSortOption() == HomeViewModel.SortOption.TRENDING);
        binding.chipNewest.setChecked(viewModel.getCurrentSortOption() == HomeViewModel.SortOption.NEWEST);
        binding.chipOldest.setChecked(viewModel.getCurrentSortOption() == HomeViewModel.SortOption.OLDEST);
        binding.chipMostUsed.setChecked(viewModel.getCurrentSortOption() == HomeViewModel.SortOption.MOST_USED);
        
        // Update time filter chips
        binding.chipAllTime.setChecked(viewModel.getCurrentTimeFilter() == HomeViewModel.TimeFilter.ALL_TIME);
        binding.chipToday.setChecked(viewModel.getCurrentTimeFilter() == HomeViewModel.TimeFilter.TODAY);
        binding.chipThisWeek.setChecked(viewModel.getCurrentTimeFilter() == HomeViewModel.TimeFilter.THIS_WEEK);
        binding.chipThisMonth.setChecked(viewModel.getCurrentTimeFilter() == HomeViewModel.TimeFilter.THIS_MONTH);
    }
    
    private void showFilterBottomSheet() {
        // Get the bottom sheet view directly from the binding
        View bottomSheetView = binding.filterBottomSheet.getRoot();
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
        
        // Set up radio buttons
        RadioButton radioTrending = bottomSheetView.findViewById(R.id.radioTrending);
        RadioButton radioNewest = bottomSheetView.findViewById(R.id.radioNewest);
        RadioButton radioOldest = bottomSheetView.findViewById(R.id.radioOldest);
        RadioButton radioMostUsed = bottomSheetView.findViewById(R.id.radioMostUsed);
        
        RadioButton radioAllTime = bottomSheetView.findViewById(R.id.radioAllTime);
        RadioButton radioToday = bottomSheetView.findViewById(R.id.radioToday);
        RadioButton radioThisWeek = bottomSheetView.findViewById(R.id.radioThisWeek);
        RadioButton radioThisMonth = bottomSheetView.findViewById(R.id.radioThisMonth);
        
        Button applyButton = bottomSheetView.findViewById(R.id.applyFiltersButton);
        Button resetButton = bottomSheetView.findViewById(R.id.resetFiltersButton);
        
        // Set initial state based on current selections
        HomeViewModel.SortOption currentSortOption = viewModel.getCurrentSortOption();
        HomeViewModel.TimeFilter currentTimeFilter = viewModel.getCurrentTimeFilter();
        
        // Set the appropriate radio button checked based on current sort option
        switch (currentSortOption) {
            case TRENDING:
                radioTrending.setChecked(true);
                break;
            case NEWEST:
                radioNewest.setChecked(true);
                break;
            case OLDEST:
                radioOldest.setChecked(true);
                break;
            case MOST_USED:
                radioMostUsed.setChecked(true);
                break;
        }
        
        // Set the appropriate radio button checked based on current time filter
        switch (currentTimeFilter) {
            case ALL_TIME:
                radioAllTime.setChecked(true);
                break;
            case TODAY:
                radioToday.setChecked(true);
                break;
            case THIS_WEEK:
                radioThisWeek.setChecked(true);
                break;
            case THIS_MONTH:
                radioThisMonth.setChecked(true);
                break;
        }
        
        // Apply button click listener
        applyButton.setOnClickListener(v -> {
            // Determine which sort option is selected
            HomeViewModel.SortOption selectedSortOption;
            if (radioTrending.isChecked()) {
                selectedSortOption = HomeViewModel.SortOption.TRENDING;
            } else if (radioNewest.isChecked()) {
                selectedSortOption = HomeViewModel.SortOption.NEWEST;
            } else if (radioOldest.isChecked()) {
                selectedSortOption = HomeViewModel.SortOption.OLDEST;
            } else {
                selectedSortOption = HomeViewModel.SortOption.MOST_USED;
            }
            
            // Determine which time filter is selected
            HomeViewModel.TimeFilter selectedTimeFilter;
            if (radioAllTime.isChecked()) {
                selectedTimeFilter = HomeViewModel.TimeFilter.ALL_TIME;
            } else if (radioToday.isChecked()) {
                selectedTimeFilter = HomeViewModel.TimeFilter.TODAY;
            } else if (radioThisWeek.isChecked()) {
                selectedTimeFilter = HomeViewModel.TimeFilter.THIS_WEEK;
            } else {
                selectedTimeFilter = HomeViewModel.TimeFilter.THIS_MONTH;
            }
            
            // Apply the filters
            viewModel.setSortOption(selectedSortOption);
            viewModel.setTimeFilter(selectedTimeFilter);
            
            // Update chip selections
            updateChipSelections();
            
            // Hide the bottom sheet
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });
        
        // Reset button click listener
        resetButton.setOnClickListener(v -> {
            // Reset to default values
            viewModel.setSortOption(HomeViewModel.SortOption.TRENDING);
            viewModel.setTimeFilter(HomeViewModel.TimeFilter.ALL_TIME);
            viewModel.setCategory(null);
            
            // Update UI
            updateChipSelections();
            
            // Hide the bottom sheet
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });
        
        // Show the bottom sheet
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void showCategoriesBottomSheet(List<String> categories) {
        Log.d(TAG, "Showing categories bottom sheet with " + categories.size() + " categories");
        
        // Create a bottom sheet dialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_categories, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        RecyclerView categoriesRecyclerView = bottomSheetView.findViewById(R.id.categoriesRecyclerView);

        // Convert string categories to Category objects for the bottom sheet
        List<com.ds.eventwish.ui.home.Category> bottomSheetCategories = new ArrayList<>();
        for (String categoryName : categories) {
            bottomSheetCategories.add(new com.ds.eventwish.ui.home.Category(
                categoryName.toLowerCase(), // ID
                categoryName,              // Name
                ""                         // No icon URL
            ));
        }

        Log.d(TAG, "Showing " + bottomSheetCategories.size() + " categories in bottom sheet");

        // Create the adapter for the bottom sheet
        CategoriesAdapter bottomSheetAdapter = new CategoriesAdapter(
            requireContext(),
            bottomSheetCategories,
            (category, position) -> {
                // Handle category click from bottom sheet
                Log.d(TAG, "Selected category from bottom sheet: " + category.getName());
                viewModel.setSelectedCategory(category.getId());
                categoriesAdapter.updateSelectedCategory(category.getId());
                bottomSheetDialog.dismiss();
            }
        );
        
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 3);
        categoriesRecyclerView.setLayoutManager(gridLayoutManager);
        // Add padding and rounded corners to the RecyclerView
        categoriesRecyclerView.setPadding(16, 16, 16, 16);
        categoriesRecyclerView.setClipToPadding(false);
        categoriesRecyclerView.setBackgroundResource(R.drawable.rounded_corners_bg);
        categoriesRecyclerView.setAdapter(bottomSheetAdapter);

        bottomSheetDialog.show();
    }

    @Override
    public void onTemplateClick(Template template) {
        try {
            Log.d(TAG, "Template clicked: " + (template != null ? template.getId() : "null"));
            
            // Check if template is valid
            if (template == null) {
                Log.e(TAG, "Attempted to click on null template");
                return;
            }
            
            // Check if it's an HTML template that needs premium features
            if (template.getType() != null && template.getType().equals("html")) {
                try {
                    if (featureManager == null) {
                        featureManager = FeatureManager.getInstance(requireContext());
                        Log.d(TAG, "FeatureManager initialized in onTemplateClick");
                    }
                    
                    if (!featureManager.isFeatureUnlocked(FeatureManager.HTML_EDITING)) {
                        // Show unlock dialog manually instead of using checkFeatureAccess which might cause issues
                        String message = featureManager.getLockedFeatureMessage(FeatureManager.HTML_EDITING);
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        
                        // Use the safer way to show the dialog
                        try {
                            UnifiedAdRewardDialog dialog = UnifiedAdRewardDialog.newInstance("Unlock HTML Editing")
                                .setCallback(new UnifiedAdRewardDialog.AdRewardCallback() {
                                    @Override
                                    public void onCoinsEarned(int amount) {
                                        Toast.makeText(requireContext(), "You earned " + amount + " coins!", Toast.LENGTH_SHORT).show();
                                    }
                                    
                                    @Override
                                    public void onFeatureUnlocked(int durationDays) {
                                        Toast.makeText(requireContext(), "Feature unlocked for " + durationDays + " days!", Toast.LENGTH_SHORT).show();
                                        
                                        // Now that it's unlocked, navigate to the detail
                                        navigateToTemplateDetail(template);
                                    }
                                    
                                    @Override
                                    public void onDismissed() {
                                        // Nothing to do here
                                    }
                                });
                            
                            dialog.show(requireActivity().getSupportFragmentManager());
                        } catch (Exception e) {
                            Log.e(TAG, "Error showing unlock dialog", e);
                            // Fall back to a simple toast
                            Toast.makeText(requireContext(), 
                                "This feature requires premium access. Please check the coins menu to unlock.",
                                Toast.LENGTH_LONG).show();
                        }
                        
                        return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking feature access", e);
                    // Continue with normal navigation if there's an error checking access
                }
            }
            
            // Continue with normal template click handling
            navigateToTemplateDetail(template);
        } catch (Exception e) {
            Log.e(TAG, "Error in onTemplateClick", e);
            // Show user-friendly error
            Toast.makeText(requireContext(), 
                "Something went wrong. Please try again.", 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    // Helper method to navigate to template detail
    private void navigateToTemplateDetail(Template template) {
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            viewModel.setSelectedTemplate(template);
            
            Bundle args = new Bundle();
            args.putString("templateId", template.getId());
            navController.navigate(R.id.action_home_to_template_detail, args);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to template detail", e);
        }
    }

    private void ensureCategoriesVisible() {
        if (binding != null && binding.categoriesRecyclerView != null) {
            Log.d(TAG, "Ensuring categories are visible - current visibility: " + 
                  (binding.categoriesRecyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
            binding.categoriesRecyclerView.setVisibility(View.VISIBLE);
            
            // Setup adapter if not already set
            if (binding.categoriesRecyclerView.getAdapter() == null) {
                Log.d(TAG, "Categories adapter not set, setting up now");
                setupCategoriesRecyclerView();
            } else {
                Log.d(TAG, "Categories adapter already set, items: " + 
                      (categoriesAdapter != null ? categoriesAdapter.getItemCount() : "NULL ADAPTER"));
            }
            
            // Load categories if adapter is empty
            if (categoriesAdapter == null) {
                Log.e(TAG, "Categories adapter is null, creating a new one");
                setupCategoriesRecyclerView();
            } else if (categoriesAdapter.getItemCount() == 0) {
                Log.d(TAG, "Categories adapter is empty, loading categories");
                viewModel.loadCategories();
                
                // Add a second attempt after a delay if still empty
                new android.os.Handler().postDelayed(() -> {
                    if (isAdded() && !isDetached() && categoriesAdapter.getItemCount() == 0) {
                        Log.d(TAG, "Categories still empty after delay, trying again");
                        viewModel.loadCategories();
                    }
                }, 3000);
            }
        } else {
            Log.e(TAG, "Cannot ensure categories are visible - binding or recyclerView is null");
        }
    }

    private void setupCategoriesRecyclerView() {
        Log.d(TAG, "Setting up categories RecyclerView");
        
        // Create adapter with context, empty list, and click listener
        categoriesAdapter = new CategoriesAdapter(
            requireContext(),
            new ArrayList<>(),
            (category, position) -> {
                // Handle category click
                Log.d(TAG, "Category clicked: " + category.getName());
                if (viewModel != null) {
                    viewModel.setSelectedCategory(category.getId());
                }
            }
        );
        
        // Set up with horizontal layout
        LinearLayoutManager layoutManager = new LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.categoriesRecyclerView.setLayoutManager(layoutManager);
        binding.categoriesRecyclerView.setAdapter(categoriesAdapter);
        
        // Ensure recyclerview is visible
        binding.categoriesRecyclerView.setVisibility(View.VISIBLE);
        
        // Add padding for better appearance
        int padding = getResources().getDimensionPixelSize(R.dimen.category_padding);
        binding.categoriesRecyclerView.setPadding(padding, 0, padding, 0);
        binding.categoriesRecyclerView.setClipToPadding(false);
        
        // Log categories adapter state
        if (viewModel != null && viewModel.getCategoryObjects().getValue() != null) {
            List<com.ds.eventwish.ui.home.Category> categories = viewModel.getCategoryObjects().getValue();
            Log.d(TAG, "Categories available from ViewModel: " + categories.size());
            categoriesAdapter.updateCategories(categories);
        } else {
            Log.d(TAG, "No categories available from ViewModel, will be loaded later");
        }
    }

    private void showAdRewardDialog() {
        Log.d(TAG, "Showing ad reward dialog");
        
        // Create and show the UnifiedAdRewardDialog with proper initialization
        UnifiedAdRewardDialog dialog = UnifiedAdRewardDialog.newInstance("Earn Coins")
            .setCallback(new UnifiedAdRewardDialog.AdRewardCallback() {
                @Override
                public void onCoinsEarned(int amount) {
                    // Show toast message
                    Toast.makeText(requireContext(), 
                        getString(R.string.coins_earned, amount), 
                        Toast.LENGTH_SHORT).show();
                    
                    // Explicitly refresh coins after dialog closes
                    if (coinsViewModel != null) {
                        Log.d(TAG, "Forcing coins refresh after reward");
                        coinsViewModel.refreshCoinsCount();
                    }
                }
                
                @Override
                public void onFeatureUnlocked(int durationDays) {
                    Toast.makeText(requireContext(), 
                        getString(R.string.feature_unlocked, durationDays), 
                        Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onDismissed() {
                    // Force a final refresh when dialog is dismissed
                    if (coinsViewModel != null) {
                        Log.d(TAG, "Final coins refresh after dialog dismissed");
                        coinsViewModel.refreshCoinsCount();
                    }
                }
            });
        
        dialog.show(requireActivity().getSupportFragmentManager());
        
        // Log for debugging
        Log.d(TAG, "Ad reward dialog shown");
    }

    private void showPremiumFeaturesDialog() {
        Log.d(TAG, "Showing premium features dialog");
        
        // Create and show the UnifiedAdRewardDialog with proper initialization
        UnifiedAdRewardDialog dialog = UnifiedAdRewardDialog.newInstance("Premium Features")
            .setCallback(new UnifiedAdRewardDialog.AdRewardCallback() {
                @Override
                public void onCoinsEarned(int amount) {
                    Toast.makeText(requireContext(), 
                        getString(R.string.coins_earned, amount), 
                        Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onFeatureUnlocked(int durationDays) {
                    Toast.makeText(requireContext(), 
                        getString(R.string.feature_unlocked, durationDays), 
                        Toast.LENGTH_SHORT).show();
                    
                    // Refresh UI if needed after unlocking feature
                    if (viewModel != null) {
                        viewModel.refreshForFeatureChange();
                    }
                }
                
                @Override
                public void onDismissed() {
                    // Nothing to do here
                }
            });
        
        dialog.show(requireActivity().getSupportFragmentManager());
        
        // Log for debugging
        Log.d(TAG, "Premium features dialog shown");
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Swipe refresh triggered");
            viewModel.clearCacheAndRefresh();
            
            // Add timeout to prevent infinite loading
            new android.os.Handler().postDelayed(() -> {
                if (binding != null && binding.swipeRefreshLayout.isRefreshing()) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                }
            }, 5000); // 5-second timeout
        });
    }
    
    private void setupIndicators() {
        // Implementation of setupIndicators
        Log.d(TAG, "Setting up indicators");
        
        // Observe loading state to show/hide indicators
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                // Show loading indicators
                binding.swipeRefreshLayout.setRefreshing(true);
                
                // Hide no results if showing
                if (binding.emptyView != null) {
                    binding.emptyView.setVisibility(View.GONE);
                }
            } else {
                // Hide loading indicators after a small delay to avoid flickering
                new android.os.Handler().postDelayed(() -> {
                    if (binding != null) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        
                        // Check if we have templates to display
                        if (viewModel.getTemplates().getValue() == null || 
                            viewModel.getTemplates().getValue().isEmpty()) {
                            // Show no results message
                            if (binding.emptyView != null) {
                                binding.emptyView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            // Hide no results message
                            if (binding.emptyView != null) {
                                binding.emptyView.setVisibility(View.GONE);
                            }
                        }
                    }
                }, 300);
            }
        });
        
        // Update the refresh indicator when refresh is needed
        viewModel.getRefreshNeeded().observe(getViewLifecycleOwner(), refreshNeeded -> {
            if (binding != null && binding.refreshIndicator != null) {
                binding.refreshIndicator.setVisibility(refreshNeeded ? View.VISIBLE : View.GONE);
            }
        });
    }
    
    private void setupBottomNavigation() {
        // Implementation of setupBottomNavigation
        Log.d(TAG, "Setting up bottom navigation");
        
        // Use requireActivity() to find the bottom navigation, but check if it exists first
        try {
            MainActivity mainActivity = (MainActivity) requireActivity();
            if (mainActivity != null) {
                bottomNav = mainActivity.findViewById(R.id.bottomNavigation); // Using bottomNavigation as found in activity_main.xml
                if (bottomNav != null) {
                    // Ensure home is selected
                    bottomNav.setSelectedItemId(R.id.navigation_home);
                    
                    // Update selected navigation item when this fragment is visible
                    bottomNav.post(() -> {
                        if (bottomNav.getSelectedItemId() != R.id.navigation_home) {
                            bottomNav.setSelectedItemId(R.id.navigation_home);
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom navigation", e);
            // Non-critical error, can be ignored
        }
    }
    
    private void initViewModels() {
        Log.d(TAG, "Initializing ViewModels");
        
        // Initialize main ViewModel if not already initialized
        if (viewModel == null) {
            viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        }
        
        // Set up templates observer
        viewModel.getTemplates().observe(getViewLifecycleOwner(), templates -> {
            if (templates != null) {
                Log.d(TAG, "Templates updated: " + templates.size());
                if (adapter != null) {
                    adapter.updateTemplates(templates);
                    
                    // Hide loading and no results views
                    if (binding != null) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                        
                        if (templates.isEmpty()) {
                            if (binding.emptyView != null) {
                                binding.emptyView.setVisibility(View.VISIBLE);
                            }
                        } else {
                            if (binding.emptyView != null) {
                                binding.emptyView.setVisibility(View.GONE);
                            }
                        }
                    }
                }
            }
        });
        
        // Set up category observer
        viewModel.getCategoryObjects().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null && categoriesAdapter != null) {
                Log.d(TAG, "Categories updated: " + categories.size());
                categoriesAdapter.updateCategories(categories);
                
                // Update selected category if needed
                String selectedCategory = viewModel.getSelectedCategory();
                if (selectedCategory != null) {
                    categoriesAdapter.updateSelectedCategory(selectedCategory);
                }
            }
        });
        
        // Initialize coins ViewModel if not already done
        if (coinsViewModel == null) {
            coinsViewModel = new ViewModelProvider(requireActivity()).get(CoinsViewModel.class);
            Log.d(TAG, "CoinsViewModel initialized");
            
            // Setup coins observer for UI updates
            coinsObserver = coinsCount -> {
                Log.d(TAG, "Coins updated: " + coinsCount);
                // Handle coins update in the activity or in the menu creation callback
                // Menu might not be created yet, so we'll handle it there
            };
            
            // Get initial coins count
            coinsViewModel.refreshCoinsCount();
            
            // Setup periodic refresh
            startPeriodicCoinsRefresh();
        }
        
        // Initialize festival ViewModel if not already done
        if (festivalViewModel == null) {
            festivalViewModel = new ViewModelProvider(requireActivity()).get(FestivalViewModel.class);
            Log.d(TAG, "FestivalViewModel initialized");
            festivalViewModel.refreshFestivals();
        }
        
        // Initialize icons repository if needed
        if (categoryIconRepository == null) {
            categoryIconRepository = CategoryIconRepository.getInstance();
            Log.d(TAG, "CategoryIconRepository initialized");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Stop periodic refreshes to prevent memory leaks
        stopPeriodicCoinsRefresh();
        
        // Clean up observers to prevent memory leaks
        if (coinsViewModel != null && coinsObserver != null) {
            coinsViewModel.getCoinsLiveData().removeObserver(coinsObserver);
        }
        
        // Clear reference to binding
        binding = null;
    }
}
