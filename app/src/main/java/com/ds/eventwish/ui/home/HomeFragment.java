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
import com.ds.eventwish.ui.home.adapter.RecommendedTemplateAdapter;
import com.ds.eventwish.ui.festival.FestivalViewModel;
import com.ds.eventwish.data.repository.CategoryIconRepository;
import com.ds.eventwish.ui.views.OfflineIndicatorView;
import com.ds.eventwish.ui.views.StaleDataIndicatorView;
import com.ds.eventwish.utils.NetworkUtils;
import com.ds.eventwish.utils.FeatureManager;
import com.ds.eventwish.EventWishApplication;
import com.ds.eventwish.data.repository.UserRepository;
import com.ds.eventwish.data.repository.EngagementRepository;
import com.ds.eventwish.data.model.EngagementData;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.ds.eventwish.utils.LogUtils;

public class HomeFragment extends BaseFragment implements TemplateAdapter.OnTemplateClickListener, RecommendedTemplateAdapter.TemplateClickListener {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private FestivalViewModel festivalViewModel;
    private MenuItem premiumMenuItem;
    private RecommendedTemplateAdapter mainAdapter;
    private TemplateAdapter horizontalRecommendationsAdapter;
    private com.ds.eventwish.ui.home.CategoriesAdapter categoriesAdapter;
    private GridLayoutManager layoutManager;
    private static final int VISIBLE_THRESHOLD = 5;
    private BottomNavigationView bottomNav;
    private long backPressedTime;
    private static final long BACK_PRESS_DELAY = 2000; // 2 seconds window for double back press
    private CategoryIconRepository categoryIconRepository;
    private boolean wasInBackground = false;
    private NetworkUtils networkUtils;
    private FeatureManager featureManager;
    private UserRepository userRepository;
    private TemplateAdapter recommendationsAdapter;
    private View recommendationsSection;
    private ShimmerFrameLayout recommendationsShimmer;
    private RecyclerView recommendationsRecyclerView;
    private boolean doubleBackToExitPressedOnce = false;
    private EngagementRepository engagementRepository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setHasOptionsMenu(true);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.d(TAG, "onViewCreated called");
        
        // Early initialization of CategoryIconRepository
        initCategoryIconRepository();
        
        // Initialize view models
        initViewModels();
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Set the EngagementRepository on the adapter
        if (mainAdapter != null) {
            mainAdapter.setEngagementRepository(engagementRepository);
        }
        
        // Set CategoryIconRepository on template adapter
        if (mainAdapter != null) {
            mainAdapter.setCategoryIconRepository(categoryIconRepository);
        }
        
        // Ensure filter chips are initially hidden
        binding.filterChipsScrollView.setVisibility(View.GONE);
        binding.timeFilterScrollView.setVisibility(View.GONE);
        
        // Setup UI components
        setupUi();
        
        // Setup categories RecyclerView
        setupCategoriesRecyclerView();
        
        // Setup observers for LiveData
        setupObservers();
        
        // Setup menu provider
        setupMenuProvider();
        
        // Setup back press handling
        setupBackPressHandling();
        
        // Setup Main UI
        setupUi();
        
        // Initialize network utilities
        networkUtils = NetworkUtils.getInstance(requireContext());
        
        // Initialize feature manager
        featureManager = FeatureManager.getInstance(requireContext());
        
        // Initialize user repository
        userRepository = UserRepository.getInstance(requireContext());
        
        // Initialize engagement repository
        engagementRepository = EngagementRepository.getInstance(requireContext());
        
        // Setup recommendations section
        setupRecommendationsSection();
        
        // Start periodic refresh handler
        startPeriodicRefresh();
    }

    private void setupRecyclerView() {
        // Initialize the adapter with click listener
        mainAdapter = new RecommendedTemplateAdapter(this);
        mainAdapter.setCategoryIconRepository(categoryIconRepository);
        
        // Get EngagementRepository for tracking
        try {
            engagementRepository = EngagementRepository.getInstance(requireContext());
            mainAdapter.setEngagementRepository(engagementRepository);
            Log.d(TAG, "EngagementRepository set on RecommendedTemplateAdapter");
        } catch (Exception e) {
            Log.e(TAG, "Error setting EngagementRepository: " + e.getMessage(), e);
        }
        
        // Configure layout manager - use 2 columns for default
        int spanCount = 2;
        layoutManager = new GridLayoutManager(getContext(), spanCount);
        
        // Configure layout manager to handle full-width section headers
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // If it's a header, make it full width
                return mainAdapter.getItemViewType(position) == RecommendedTemplateAdapter.VIEW_TYPE_HEADER 
                    ? layoutManager.getSpanCount() : 1;
            }
        });
        
        binding.templatesRecyclerView.setLayoutManager(layoutManager);
        binding.templatesRecyclerView.setAdapter(mainAdapter);
        
        // Add pagination listener
        binding.templatesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private boolean isPaginationRequestPending = false;
            private long lastLoadRequestTime = 0;
            private static final long MIN_REQUEST_INTERVAL = 500; // Minimum time between requests in ms
            
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                // Skip pagination checks if currently loading
                if (viewModel.getLoading().getValue() != null && viewModel.getLoading().getValue()) {
                    Log.d(TAG, "Already loading, skipping pagination check");
                    return;
                }
                
                // Skip if pagination request is already pending
                if (isPaginationRequestPending) {
                    Log.d(TAG, "Pagination request pending, skipping");
                    return;
                }
                
                // Throttle requests to avoid multiple rapid calls
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLoadRequestTime < MIN_REQUEST_INTERVAL) {
                    Log.d(TAG, "Request throttled, waiting for interval");
                    return;
                }
                
                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                
                // Prevent category changes during scrolling
                if (categoriesAdapter != null) {
                    categoriesAdapter.preventCategoryChanges(true);
                }
                
                // Check if we're near the end of the list
                if (!viewModel.isFirstLoad() && viewModel.hasMorePages() && lastVisibleItem + VISIBLE_THRESHOLD >= totalItemCount) {
                    Log.d(TAG, "Near end of list, loading more templates");
                    
                    // Set pagination request as pending
                    isPaginationRequestPending = true;
                    lastLoadRequestTime = currentTime;
                    
                    // Load more templates
                    viewModel.loadMoreTemplates();
                    
                    // Reset pending flag after delay
                    new Handler().postDelayed(() -> isPaginationRequestPending = false, 1000);
                } else {
                    // If not loading more, allow category changes
                    if (categoriesAdapter != null) {
                        categoriesAdapter.preventCategoryChanges(false);
                    }
                }
                
                // Save the last visible position for state restoration
                viewModel.saveScrollPosition(layoutManager.findFirstVisibleItemPosition());
            }
            
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                
                // If user is actively dragging or settling after a fling
                boolean isScrolling = newState == RecyclerView.SCROLL_STATE_DRAGGING || 
                                      newState == RecyclerView.SCROLL_STATE_SETTLING;
                
                // Prevent category changes during active scrolling
                if (categoriesAdapter != null) {
                    categoriesAdapter.preventCategoryChanges(isScrolling);
                    Log.d(TAG, "Category changes " + (isScrolling ? "prevented" : "allowed") + 
                          " during scroll state: " + newState);
                }
                
                // When scrolling stops, save current position
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                    viewModel.saveScrollPosition(firstVisiblePosition);
                    Log.d(TAG, "Saved scroll position: " + firstVisiblePosition);
                }
            }
        });
        
        Log.d(TAG, "RecyclerView setup complete with " + spanCount + " columns");
    }

    /**
     * Set up back press handling with double press to exit
     */
    private void setupBackPressHandling() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Check if this is the second back press within 2 seconds
                if (backPressedTime + BACK_PRESS_DELAY > System.currentTimeMillis()) {
                    // Second back press within time window, exit the app
                    this.setEnabled(false);
                    requireActivity().onBackPressed();
                } else {
                    // First back press, show message
                    Toast.makeText(requireContext(), 
                        R.string.press_back_again_to_exit, 
                        Toast.LENGTH_SHORT).show();
                }
                
                // Update the time of this back press
                backPressedTime = System.currentTimeMillis();
            }
        };
        
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
    }

    private void setupObservers() {
        // Observe templates
        viewModel.getTemplates().observe(getViewLifecycleOwner(), templates -> {
            Log.d(TAG, "Template list updated: " + (templates != null ? templates.size() : 0) + " templates");
            
            // Stop loading animations
            binding.swipeRefreshLayout.setRefreshing(false);
            
            // Track if this is first load vs pagination
            boolean isFirstLoad = viewModel.isFirstLoad();
            
            if (templates != null && !templates.isEmpty()) {
                // Hide error state if we have templates
                binding.retryLayout.setVisibility(View.GONE);
                binding.templatesRecyclerView.setVisibility(View.VISIBLE);
                
                // Get recommended template IDs from the view model
                Set<String> recommendedIds = new HashSet<>();
                if (viewModel.getRecommendedTemplateIds().getValue() != null) {
                    recommendedIds = viewModel.getRecommendedTemplateIds().getValue();
                }
                
                // Add explicitly recommended templates from the templates list
                if (templates != null) {
                    for (Template template : templates) {
                        if (template.isRecommended()) {
                            recommendedIds.add(template.getId());
                        }
                    }
                }
                
                // Update main adapter with templates and recommendations
                mainAdapter.submitListWithRecommendations(templates, recommendedIds);
                
                // For first load, scroll to top
                if (isFirstLoad) {
                    binding.templatesRecyclerView.scrollToPosition(0);
                    Log.d(TAG, "First load - scrolling to top");
                }
                
                // Always check for new templates
                viewModel.checkForNewTemplates(templates);
                
                // Set new template IDs
                Set<String> newTemplateIds = viewModel.getNewTemplateIds().getValue();
                if (newTemplateIds != null) {
                    mainAdapter.setNewTemplateIds(newTemplateIds);
                }
                
                // Hide loading indicator
                binding.swipeRefreshLayout.setRefreshing(false);
            } else {
                // No templates - show empty state
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.retryLayout.setVisibility(View.VISIBLE);
                binding.templatesRecyclerView.setVisibility(View.GONE);
                
                // Empty adapter
                mainAdapter.updateTemplates(new ArrayList<>());
            }
            
            // Allow category changes again after templates are loaded
            if (categoriesAdapter != null) {
                categoriesAdapter.preventCategoryChanges(false);
            }
            
            // Update error/empty state
            updateEmptyAndErrorState();
        });
        
        // Observe loading state
        setupLoadingObserver();

        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Log.e(TAG, "Error observed: " + errorMsg);
                // Display error message
                showErrorMessage(errorMsg);
            } else {
                // Hide error message
                binding.retryLayout.setVisibility(View.GONE);
            }
            
            // Update empty/error state
            updateEmptyAndErrorState();
        });

        viewModel.getCategoryObjects().observe(getViewLifecycleOwner(), categories -> {
            Log.d(TAG, "Category objects updated: " + categories.size());
            
            // Convert category objects to strings for the new adapter
            List<String> categoryNames = new ArrayList<>();
            if (categories != null) {
                for (com.ds.eventwish.ui.home.Category categoryObj : categories) {
                    if (categoryObj != null && categoryObj.getName() != null) {
                        categoryNames.add(categoryObj.getName());
                    }
                }
            }
            
            // Check if adapter is initialized, or initialize it if needed
            if (categoriesAdapter == null) {
                categoriesAdapter = new com.ds.eventwish.ui.home.CategoriesAdapter(requireContext());
                categoriesAdapter.setOnCategoryClickListener((category, position) -> {
                    // Handle category click
                    if (viewModel != null) {
                        viewModel.setSelectedCategory(category);
                    }
                });
                
                if (binding.categoriesRecyclerView.getAdapter() == null) {
                    binding.categoriesRecyclerView.setAdapter(categoriesAdapter);
                }
            }
            
            // Update categories in adapter
            categoriesAdapter.updateCategories(categoryNames);
        });
        
        // Observe new template IDs
        viewModel.getNewTemplateIds().observe(getViewLifecycleOwner(), newTemplateIds -> {
            Log.d(TAG, "New template IDs updated: " + (newTemplateIds != null ? newTemplateIds.size() : 0));
            if (mainAdapter != null) {
                mainAdapter.setNewTemplateIds(newTemplateIds);
            }
        });
        
        // Observe recommended template IDs
        viewModel.getRecommendedTemplateIds().observe(getViewLifecycleOwner(), recommendedIds -> {
            Log.d(TAG, "Recommended template IDs updated: " + (recommendedIds != null ? recommendedIds.size() : 0));
            if (mainAdapter != null) {
                mainAdapter.setRecommendedTemplateIds(recommendedIds);
            }
        });
        
        // Set up SwipeRefreshLayout
        setupSwipeRefresh();
        
        // Set up offline and stale data indicators
        setupIndicators();

        // Set up bottom navigation
        setupBottomNavigation();

        // Explicitly load categories to ensure they're displayed
        Log.d(TAG, "Explicitly loading categories in onViewCreated");
        viewModel.loadCategories();

        // Ensure categories are visible
        ensureCategoriesVisible();

        // Initialize recommendations section views
        recommendationsSection = binding.recommendationsSection;
        recommendationsShimmer = binding.recommendationsShimmer;
        recommendationsRecyclerView = binding.recommendationsRecyclerView;

        // Set up recommendations adapter
        horizontalRecommendationsAdapter = new TemplateAdapter(this);

        // Configure RecyclerView
        recommendationsRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recommendationsRecyclerView.setAdapter(horizontalRecommendationsAdapter);

        // Hide the recommendations section initially
        recommendationsSection.setVisibility(View.GONE);

        // Observe recommendations
        viewModel.getRecommendedTemplates().observe(getViewLifecycleOwner(), templates -> {
            Log.d(TAG, "Recommended templates updated, count: " + 
                    (templates != null ? templates.size() : 0));
            if (templates != null && !templates.isEmpty()) {
                // Mark these templates as recommended for the badge to appear
                horizontalRecommendationsAdapter.submitListWithRecommendations(
                    templates, 
                    templates.stream()
                        .map(Template::getId)
                        .collect(java.util.stream.Collectors.toSet())
                );
                recommendationsSection.setVisibility(View.VISIBLE);
                recommendationsShimmer.setVisibility(View.GONE);
            } else {
                recommendationsSection.setVisibility(View.GONE);
            }
        });

        viewModel.isRecommendationsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "Recommendations loading state: " + isLoading);
            if (isLoading) {
                recommendationsShimmer.setVisibility(View.VISIBLE);
                recommendationsShimmer.startShimmer();
                // Show the recommendations section when loading so users can see the shimmer
                recommendationsSection.setVisibility(View.VISIBLE);
            } else {
                recommendationsShimmer.stopShimmer();
                recommendationsShimmer.setVisibility(View.GONE);
            }
        });

        viewModel.getRecommendationsError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "Error loading recommendations: " + error);
                recommendationsSection.setVisibility(View.GONE);
            }
        });
    }

    private void setupMenuProvider() {
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.home_menu, menu);
                
                // Get the premium menu item
                premiumMenuItem = menu.findItem(R.id.action_premium);
                
                // Add API tester menu item
                MenuItem apiTesterItem = menu.add(Menu.NONE, R.id.action_api_tester, Menu.NONE, "API Tester");
                apiTesterItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                
                // Add database log menu item
                MenuItem dbLogItem = menu.add(Menu.NONE, R.id.action_db_log, Menu.NONE, "Database Log");
                dbLogItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                
                // Update the premium menu item with current value
                updatePremiumMenuItem();
            }
            
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                // Handle menu item selection
                if (menuItem.getItemId() == R.id.action_premium) {
                    showAdRewardDialogForFeature();
                    return true;
                } else if (menuItem.getItemId() == R.id.action_refresh) {
                    // Refresh templates
                    binding.swipeRefreshLayout.setRefreshing(true);
                    viewModel.refreshTemplates();
                    return true;
                } else if (menuItem.getItemId() == R.id.action_settings) {
                    // Navigate to settings
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    navController.navigate(R.id.navigation_more);
                    return true;
                } else if (menuItem.getItemId() == R.id.action_api_tester) {
                    // Navigate to API tester
                    return true;
                } else if (menuItem.getItemId() == R.id.action_db_log) {
                    // Navigate to DB log
                    return true;
                }
                
                return false;
            }
        };
        
        requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), androidx.lifecycle.Lifecycle.State.RESUMED);
    }

    private void updatePremiumMenuItem() {
        // Coins functionality is removed, so this is now a no-op
    }
    
    private final Handler periodicRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable periodicRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded() && !isDetached()) {
                // Refresh templates
                viewModel.refreshTemplates();
            }
            // Schedule next run
            periodicRefreshHandler.postDelayed(this, 30000); // 30 seconds
        }
    };

    private void startPeriodicRefresh() {
        // DISABLED: This method is kept for manual refresh functionality only
        // and should not be called automatically to reduce server load
        periodicRefreshHandler.postDelayed(periodicRefreshRunnable, 30000); // First run after 30 seconds
    }

    private void stopPeriodicRefresh() {
        // Remove callbacks
        periodicRefreshHandler.removeCallbacks(periodicRefreshRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Check if we were in background
        if (wasInBackground) {
            Log.d(TAG, "Coming back from background, refreshing data");
            
            // Refresh data
            viewModel.refreshTemplates();
            
            wasInBackground = false;
        } else {
            // When coming back from another fragment (not from background),
            // we don't want to reload all data, but we should ensure UI is up-to-date
            Log.d(TAG, "Resuming from another fragment, updating UI without full reload");
            
            // Make sure categories are visible
            ensureCategoriesVisible();
            
            // Restore scroll position if needed
            if (layoutManager != null) {
                binding.templatesRecyclerView.getLayoutManager().scrollToPosition(viewModel.getLastVisiblePosition());
            }
        }
        
        // Check if we need to refresh recommendations
        if (viewModel.shouldRefreshRecommendations()) {
            viewModel.getPersonalizedRecommendations();
        }
        
        // DISABLE automatic periodic refresh to reduce server load
        // startPeriodicRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        
        // Only set background flag if we're actually going to background
        // and not just navigating to another fragment
        if (!isNavigatingToAnotherFragment()) {
            Log.d(TAG, "Going to background, setting background flag");
            wasInBackground = true;
        } else {
            Log.d(TAG, "Navigating to another fragment, not setting background flag");
        }
        
        // Save current scroll position
        if (layoutManager != null) {
            int position = layoutManager.findFirstVisibleItemPosition();
            viewModel.saveScrollPosition(position);
        }
        
        // Stop periodic refresh
        stopPeriodicRefresh();
    }

    // Helper method to determine if we're navigating to another fragment
    private boolean isNavigatingToAnotherFragment() {
        if (getActivity() == null) return false;
        
        // Check if we're navigating to another fragment within the app
        // by checking if there's a pending navigation
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        return navController.getCurrentDestination() != null && 
               navController.getCurrentDestination().getId() != R.id.navigation_home;
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
            viewModel.setSelectedCategory(null);
            
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

        Log.d(TAG, "Showing " + categories.size() + " categories in bottom sheet");

        // Create the adapter for the bottom sheet with proper context
        com.ds.eventwish.ui.home.CategoriesAdapter bottomSheetAdapter = 
            new com.ds.eventwish.ui.home.CategoriesAdapter(requireContext());
        
        // Track the currently selected category
        String currentCategory = viewModel.getSelectedCategory();
        
        // Configure adapter callbacks
        bottomSheetAdapter.setOnCategoryClickListener((category, position) -> {
            // Handle category click from bottom sheet
            Log.d(TAG, "Selected category from bottom sheet: " + category);
            
            // Close dialog first to prevent UI jank
            bottomSheetDialog.dismiss();
            
            // Update main categories adapter
            if (categoriesAdapter != null) {
                categoriesAdapter.updateSelectedCategory(category);
            }
            
            // Update view model with selected category
            viewModel.setSelectedCategory(category);
        });
        
        // Update the adapter with the categories
        bottomSheetAdapter.updateCategories(categories);
        
        // Highlight the current category
        if (currentCategory != null) {
            bottomSheetAdapter.updateSelectedCategory(currentCategory);
        }
        
        // Configure the RecyclerView
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 3);
        categoriesRecyclerView.setLayoutManager(gridLayoutManager);
        
        // Add padding and rounded corners to the RecyclerView
        categoriesRecyclerView.setPadding(16, 16, 16, 16);
        categoriesRecyclerView.setClipToPadding(false);
        categoriesRecyclerView.setAdapter(bottomSheetAdapter);

        // Show the bottom sheet
        bottomSheetDialog.show();
    }

    @Override
    public void onTemplateClick(Template template) {
        if (template == null) return;
        
        Log.d(TAG, "User clicked on template: " + template.getId() + " - " + template.getTitle());
        
        // Track template view in ViewModel
        viewModel.onTemplateClick(template);
        
        // Track in engagement repository with source
        try {
            if (engagementRepository != null) {
                String source = template.isRecommended() ? 
                    EngagementData.SOURCE_RECOMMENDATION : EngagementData.SOURCE_DIRECT;
                    
                // Track the template engagement with appropriate source
                engagementRepository.trackTemplateView(
                    template.getId(), 
                    template.getCategory(),
                    source
                );
                
                Log.d(TAG, "Tracked template view in engagement repository: " + 
                      template.getId() + " with source: " + source);
            } else {
                Log.w(TAG, "EngagementRepository not initialized, tracking skipped");
            }
        } catch (Exception e) {
            // Prevent engagement tracking errors from disrupting the user experience
            Log.e(TAG, "Error tracking template engagement", e);
        }
        
        // Navigate to template detail
        navigateToCustomize(template);
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
        // Initialize adapter with context
        categoriesAdapter = new com.ds.eventwish.ui.home.CategoriesAdapter(requireContext());
        
        categoriesAdapter.setOnCategoryClickListener((category, position) -> {
            Log.d(TAG, "📋 Category clicked: " + category);
            
            // Add a safety check to prevent errors with CategoryIconRepository
            if (categoryIconRepository == null) {
                Log.e(TAG, "CategoryIconRepository not initialized, reinitializing");
                initCategoryIconRepository();
            }
            
            // Check if currently loading more content - prevent category change during load
            if (viewModel.getLoading().getValue() != null && viewModel.getLoading().getValue()) {
                Log.d(TAG, "Ignoring category click - content currently loading");
                return;
            }
            
            setCategory(category);
        });
        
        categoriesAdapter.setOnMoreClickListener(remainingCategories -> {
            // Show a bottom sheet with all categories
            showCategoriesBottomSheet(remainingCategories);
        });
        
        binding.categoriesRecyclerView.setLayoutManager(
            new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        binding.categoriesRecyclerView.setAdapter(categoriesAdapter);
        
        // Observe categories from HomeViewModel
        viewModel.getCategoryObjects().observe(getViewLifecycleOwner(), categoryObjects -> {
            if (categoryObjects != null && !categoryObjects.isEmpty()) {
                List<String> categories = new ArrayList<>();
                for (com.ds.eventwish.ui.home.Category categoryObj : categoryObjects) {
                    categories.add(categoryObj.getName());
                }
                
                Log.d(TAG, "📋 Categories updated: " + categories.size() + " categories available");
                categoriesAdapter.updateCategories(categories);
                
                // For debugging
                if (categories.size() > 0) {
                    StringBuilder sb = new StringBuilder("Available categories: ");
                    for (int i = 0; i < Math.min(5, categories.size()); i++) {
                        sb.append(categories.get(i)).append(", ");
                    }
                    if (categories.size() > 5) {
                        sb.append("and ").append(categories.size() - 5).append(" more");
                    }
                    Log.d(TAG, sb.toString());
                }
            } else {
                Log.w(TAG, "Empty categories list from view model");
            }
        });
    }

    private void showAdRewardDialog() {
        // UnifiedAdRewardDialog.showDialog(getParentFragmentManager(), new UnifiedAdRewardDialog.OnRewardListener() {
        //     @Override
        //     public void onRewardEarned(int amount) {
        //         // Handle reward earned
        //         handleRewardEarned(amount);
        //     }

        //     @Override
        //     public void onRewardFailed() {
        //         // Handle reward failed
        //         handleRewardFailed();
        //     }

        //     @Override
        //     public void onDialogDismissed() {
        //         // Handle dialog dismissed
        //         handleDialogDismissed();
        //     }
        // });
    }

    private void showAdRewardDialogForFeature() {
        // UnifiedAdRewardDialog.showDialog(getParentFragmentManager(), new UnifiedAdRewardDialog.OnRewardListener() {
        //     @Override
        //     public void onRewardEarned(int amount) {
        //         // Handle reward earned
        //         handleFeatureRewardEarned(amount);
        //     }

        //     @Override
        //     public void onRewardFailed() {
        //         // Handle reward failed
        //         handleFeatureRewardFailed();
        //     }

        //     @Override
        //     public void onDialogDismissed() {
        //         // Handle dialog dismissed
        //         handleFeatureDialogDismissed();
        //     }
        // });
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
                if (mainAdapter != null) {
                    mainAdapter.updateTemplates(templates);
                    
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
                Log.d(TAG, "Category objects updated: " + categories.size());
                
                // Convert category objects to strings for the new adapter
                List<String> categoryNames = new ArrayList<>();
                if (categories != null) {
                    for (com.ds.eventwish.ui.home.Category category : categories) {
                        if (category != null && category.getName() != null) {
                            categoryNames.add(category.getName());
                        }
                    }
                }
                
                // Check if adapter is initialized, or initialize it if needed
                if (categoriesAdapter == null) {
                    categoriesAdapter = new com.ds.eventwish.ui.home.CategoriesAdapter(requireContext());
                    categoriesAdapter.setOnCategoryClickListener((category, position) -> {
                        // Handle category click
                        if (viewModel != null) {
                            viewModel.setSelectedCategory(category);
                        }
                    });
                    
                    if (binding.categoriesRecyclerView.getAdapter() == null) {
                        binding.categoriesRecyclerView.setAdapter(categoriesAdapter);
                    }
                }
                
                // Update categories in adapter
                categoriesAdapter.updateCategories(categoryNames);
            }
        });
        
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
        
        // Clean up observers
        if (viewModel != null) {
            viewModel.resetSelectedCategory();
        }
        
        // Removed coinsViewModel cleanup
        
        binding = null;
    }

    private void handleRewardEarned(int amount) {
        // Premium functionality is now enabled by default, just show a success message
        Toast.makeText(requireContext(), getString(R.string.reward_earned_message), Toast.LENGTH_SHORT).show();
    }

    private void handleFeatureRewardEarned(int amount) {
        // Premium functionality is now enabled by default, just show a success message
        Toast.makeText(requireContext(), getString(R.string.feature_unlocked_message), Toast.LENGTH_SHORT).show();
    }

    private void handleRewardFailed() {
        Toast.makeText(requireContext(), R.string.reward_error, Toast.LENGTH_SHORT).show();
    }

    private void handleFeatureRewardFailed() {
        Toast.makeText(requireContext(), R.string.reward_error, Toast.LENGTH_SHORT).show();
    }

    private void handleDialogDismissed() {
        // No-op now that ads are removed
    }

    private void handleFeatureDialogDismissed() {
        // No-op now that ads are removed
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * Update premium display to show premium status
     */
    private void updatePremiumDisplay() {
        // Premium functionality is enabled by default, so this is now a no-op
    }

    /**
     * Refresh all data in the home screen
     */
    private void refreshData() {
        Log.d(TAG, "Manual refresh triggered by user");
        
        // Refresh templates
        if (viewModel != null) {
            viewModel.refreshTemplates();
            
            // Refresh recommendations
            viewModel.getPersonalizedRecommendations();
        }
        
        // Show the swipe refresh indicator
        if (binding != null) {
            binding.swipeRefreshLayout.setRefreshing(true);
        }
    }

    /**
     * Navigate to the template customize screen
     * @param template Template to customize
     */
    private void navigateToCustomize(Template template) {
        if (template == null) return;
        
        // Navigate to template detail/customize screen
        Bundle args = new Bundle();
        args.putString("templateId", template.getId());
        args.putString("templateTitle", template.getTitle());
        args.putString("templateCategory", template.getCategory());
        args.putString("templateThumbnail", template.getThumbnailUrl());
        
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.action_home_to_template_detail, args);
            
            // Mark the template as viewed if it was new
            if (mainAdapter != null) {
                mainAdapter.markAsViewed(template.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Navigation error", e);
            Toast.makeText(requireContext(), "Error opening template", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Setup the recommendations section
     */
    private void setupRecommendationsSection() {
        // Initialize recommendations section views
        recommendationsSection = binding.recommendationsSection;
        recommendationsShimmer = binding.recommendationsShimmer;
        recommendationsRecyclerView = binding.recommendationsRecyclerView;

        // Set up recommendations adapter
        horizontalRecommendationsAdapter = new TemplateAdapter(this);

        // Configure RecyclerView
        recommendationsRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recommendationsRecyclerView.setAdapter(horizontalRecommendationsAdapter);

        // Show the recommendations section with shimmer while loading
        recommendationsSection.setVisibility(View.VISIBLE);
        recommendationsShimmer.setVisibility(View.VISIBLE);
        recommendationsShimmer.startShimmer();

        // Observe recommendations
        viewModel.getRecommendedTemplates().observe(getViewLifecycleOwner(), templates -> {
            Log.d(TAG, "Recommended templates updated, count: " + 
                    (templates != null ? templates.size() : 0));
            if (templates != null && !templates.isEmpty()) {
                // Mark these templates as recommended for the badge to appear
                horizontalRecommendationsAdapter.submitListWithRecommendations(
                    templates, 
                    templates.stream()
                        .map(Template::getId)
                        .collect(java.util.stream.Collectors.toSet())
                );
                recommendationsSection.setVisibility(View.VISIBLE);
                recommendationsShimmer.setVisibility(View.GONE);
            } else {
                // If we have no recommendations but are still loading, keep the shimmer visible
                if (viewModel.isRecommendationsLoading().getValue() != null &&
                    viewModel.isRecommendationsLoading().getValue()) {
                    recommendationsSection.setVisibility(View.VISIBLE);
                    recommendationsShimmer.setVisibility(View.VISIBLE);
                } else {
                    recommendationsSection.setVisibility(View.GONE);
                }
            }
        });

        viewModel.isRecommendationsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "Recommendations loading state: " + isLoading);
            if (isLoading) {
                recommendationsShimmer.setVisibility(View.VISIBLE);
                recommendationsShimmer.startShimmer();
                // Show the recommendations section when loading so users can see the shimmer
                recommendationsSection.setVisibility(View.VISIBLE);
            } else {
                recommendationsShimmer.stopShimmer();
                recommendationsShimmer.setVisibility(View.GONE);
            }
        });

        viewModel.getRecommendationsError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "Error loading recommendations: " + error);
                recommendationsSection.setVisibility(View.GONE);
            }
        });
        
        // Load personalized recommendations
        viewModel.getPersonalizedRecommendations();
    }

    /**
     * Update the empty state and error state based on templates and error status
     */
    private void updateEmptyAndErrorState() {
        List<Template> templates = viewModel.getTemplates().getValue();
        String errorMsg = viewModel.getError().getValue();
        boolean isLoading = viewModel.getLoading().getValue() != null && viewModel.getLoading().getValue();
        
        if (templates == null || templates.isEmpty()) {
            if (isLoading && viewModel.getCurrentPage() == 1) {
                // Still loading first page, show loading indicator
                binding.emptyView.setVisibility(View.GONE);
                binding.templatesRecyclerView.setVisibility(View.GONE);
                binding.retryLayout.setVisibility(View.GONE);
            } else if (errorMsg != null && !errorMsg.isEmpty()) {
                // Error occurred and no templates, show error view
                binding.emptyView.setVisibility(View.GONE);
                binding.templatesRecyclerView.setVisibility(View.GONE);
                binding.retryLayout.setVisibility(View.VISIBLE);
            } else {
                // No templates and no error, show empty view
                binding.emptyView.setVisibility(View.VISIBLE);
                binding.templatesRecyclerView.setVisibility(View.GONE);
                binding.retryLayout.setVisibility(View.GONE);
            }
        } else {
            // We have templates, show them
            binding.emptyView.setVisibility(View.GONE);
            binding.templatesRecyclerView.setVisibility(View.VISIBLE);
            
            // If we have both templates and an error, show a snackbar instead of the error layout
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Snackbar.make(binding.getRoot(), errorMsg, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry, v -> viewModel.refreshTemplates())
                    .show();
            }
        }
    }
    
    /**
     * Show error message in the error layout
     */
    private void showErrorMessage(String message) {
        binding.retryLayout.setVisibility(View.VISIBLE);
        binding.errorText.setText(message);
        binding.retryButton.setOnClickListener(v -> {
            // Clear error and retry loading
            viewModel.clearError();
            viewModel.refreshTemplates();
        });
    }

    private void setCategory(String category) {
        if (category == null) {
            Log.w(TAG, "Attempted to set null category");
            return;
        }
        
        Log.d(TAG, "🔄 Setting category: " + category);
        
        // Normalize "all" vs "All" category handling for consistency
        String normalizedCategory = category;
        if ("all".equalsIgnoreCase(category)) {
            normalizedCategory = "All"; // Always use "All" for display
            Log.d(TAG, "Normalized category 'all' to 'All' for consistency");
        }
        
        // Start timing for category change
        LogUtils.startTimer(TAG, "categoryChange");
        LogUtils.category(TAG, "Selected", normalizedCategory);
        
        // Set the category in view model
        viewModel.setSelectedCategory(normalizedCategory);
        
        // Update UI for category selection
        if (categoriesAdapter != null) {
            categoriesAdapter.updateSelectedCategory(normalizedCategory);
        }
        
        // Track category visit with enhanced metrics
        try {
            EngagementRepository engagementRepository = 
                EngagementRepository.getInstance(requireContext());
            
            // For the "All" category, track as "all" (lowercase for backend)
            String categoryId = normalizedCategory.equalsIgnoreCase("All") ? 
                "all" : normalizedCategory.toLowerCase();
            
            // Track with source as direct user selection
            engagementRepository.trackCategoryVisit(
                categoryId, 
                EngagementData.SOURCE_DIRECT
            );
            
            Log.d(TAG, "📊 Tracked enhanced category visit: " + categoryId);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error tracking category visit", e);
            
            // Fallback to legacy tracking if enhanced tracking fails
            if (userRepository != null) {
                if (normalizedCategory.equalsIgnoreCase("All")) {
                    userRepository.updateUserActivity("all");
                } else {
                    userRepository.updateUserActivity(normalizedCategory.toLowerCase());
                }
                Log.d(TAG, "📊 Fallback category tracking: " + 
                      (normalizedCategory.equalsIgnoreCase("All") ? "all" : normalizedCategory.toLowerCase()));
            }
        }
        
        // Scroll to top when changing category
        if (binding.templatesRecyclerView != null) {
            binding.templatesRecyclerView.smoothScrollToPosition(0);
        }
        
        LogUtils.endTimer(TAG, "categoryChange");
    }

    private void initCategoryIconRepository() {
        try {
            Log.d(TAG, "Initializing CategoryIconRepository with context");
            
            // Force create with context to ensure proper initialization
            categoryIconRepository = CategoryIconRepository.getInstance(requireContext());
            
            // Force refresh on initialization to ensure icons are loaded
            categoryIconRepository.refreshCategoryIcons();
            
            // Observe icon loading state to update UI when icons change
            categoryIconRepository.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
                Log.d(TAG, "CategoryIconRepository loading state: " + isLoading);
                
                // When icon loading completes, update adapters
                if (isLoading != null && !isLoading) {
                    if (categoriesAdapter != null) {
                        // Force refresh the categories UI
                        categoriesAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Categories UI refreshed after icon loading");
                    }
                }
            });
            
            Log.d(TAG, "CategoryIconRepository initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing CategoryIconRepository: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    private void updateCategoriesAdapter(List<String> categories) {
        // Ensure categories exist
        if (categories == null || categories.isEmpty()) {
            Log.w(TAG, "Empty categories list in updateCategoriesAdapter");
            return;
        }
        
        if (categoriesAdapter == null) {
            Log.d(TAG, "Creating new CategoriesAdapter with " + categories.size() + " categories");
            categoriesAdapter = new com.ds.eventwish.ui.home.CategoriesAdapter(requireContext());
            categoriesAdapter.setOnCategoryClickListener((category, position) -> {
                Log.d(TAG, "Category selected: " + category);
                viewModel.setSelectedCategory(category);
            });
            
            binding.categoriesRecyclerView.setAdapter(categoriesAdapter);
        }
        
        // Log the categories being updated
        StringBuilder categoryLog = new StringBuilder("Updating categories: ");
        for (String category : categories) {
            categoryLog.append(category).append(", ");
        }
        Log.d(TAG, categoryLog.toString());
        
        // Update the adapter
        categoriesAdapter.updateCategories(categories);
    }

    // Observer setup for loading status
    private void setupLoadingObserver() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                // Show/hide loading indicators
                if (isLoading) {
                    // Show loading indicator for first load
                    if (viewModel.isFirstLoad()) {
                        binding.swipeRefreshLayout.setRefreshing(true);
                    } else {
                        // For pagination, only show bottom loading indicator
                        binding.bottomLoadingView.setVisibility(View.VISIBLE);
                    }
                } else {
                    // Hide loading indicators
                    binding.swipeRefreshLayout.setRefreshing(false);
                    binding.bottomLoadingView.setVisibility(View.GONE);
                }
                
                // Prevent category changes during loading to maintain stability
                if (categoriesAdapter != null) {
                    categoriesAdapter.setPreventCategoryChanges(isLoading);
                    Log.d(TAG, isLoading ? 
                        "🔒 Preventing category changes during loading" : 
                        "🔓 Category changes allowed - loading completed");
                }
            }
        });
    }
}
