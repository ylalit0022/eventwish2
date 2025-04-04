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
import com.ds.eventwish.ui.festival.FestivalViewModel;
import com.ds.eventwish.data.repository.CategoryIconRepository;
import com.ds.eventwish.ui.views.OfflineIndicatorView;
import com.ds.eventwish.ui.views.StaleDataIndicatorView;
import com.ds.eventwish.utils.NetworkUtils;
import com.ds.eventwish.utils.FeatureManager;
import com.ds.eventwish.EventWishApplication;
import com.ds.eventwish.data.repository.UserRepository;

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
    private MenuItem premiumMenuItem;
    private TemplateAdapter adapter;
    private CategoriesAdapter categoriesAdapter;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setHasOptionsMenu(true);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize view models
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        festivalViewModel = new ViewModelProvider(requireActivity()).get(FestivalViewModel.class);
        
        // Initialize other components
        categoryIconRepository = CategoryIconRepository.getInstance();
        networkUtils = NetworkUtils.getInstance(requireContext());
        featureManager = FeatureManager.getInstance(requireContext());
        userRepository = EventWishApplication.getInstance().getUserRepository();
        
        // Force refresh category icons to ensure they're loaded immediately
        categoryIconRepository.refreshCategoryIcons();
        
        // Setup back press handling
        setupBackPressHandling();
        
        // Setup UI components
        setupRecyclerView();
        setupUi();
        
        // Show swipe refresh indicator immediately to indicate initial data loading
        binding.swipeRefreshLayout.setRefreshing(true);
        
        setupObservers();
        setupMenuProvider();
    }

    private void setupRecyclerView() {
        adapter = new TemplateAdapter(this);
        layoutManager = new GridLayoutManager(requireContext(), 1);
        binding.templatesRecyclerView.setLayoutManager(layoutManager);
        binding.templatesRecyclerView.setAdapter(adapter);
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
                    
                    // Ensure swipe refresh indicator is showing for initial load
                    if (!binding.swipeRefreshLayout.isRefreshing()) {
                        binding.swipeRefreshLayout.setRefreshing(true);
                    }
                }
            } else {
                /*
                binding.shimmerFrameLayout.stopShimmer();
                binding.shimmerFrameLayout.setVisibility(View.GONE);
                */
                binding.loadingProgressBar.setVisibility(View.GONE);
                
                // Give a small delay before hiding the refresh indicator for better UX
                new Handler().postDelayed(() -> {
                    if (binding != null && binding.swipeRefreshLayout.isRefreshing()) {
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                }, 300); // 300ms delay for better visibility
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
        navigateToTemplateDetail(template);
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
                
                // Track category visit in UserRepository
                if (userRepository != null) {
                    // For the "All" category, track as null (server handles this as "all")
                    if (category.getId() == null) {
                        userRepository.updateUserActivity("all");
                    } else {
                        userRepository.updateUserActivity(category.getId());
                    }
                    Log.d(TAG, "Tracking category visit: " + (category.getId() != null ? category.getId() : "all"));
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
        
        // Load categories immediately
        viewModel.loadCategories();
        
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

    private void updatePremiumDisplay() {
        // Premium functionality is enabled by default, so this is now a no-op
    }
}
