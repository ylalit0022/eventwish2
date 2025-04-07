package com.ds.eventwish.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.ui.base.BaseFragment;
import com.ds.eventwish.ui.home.adapter.RecommendedTemplateAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.JustifyContent;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexWrap;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.ds.eventwish.ui.festival.FestivalViewModel;
import com.ds.eventwish.data.repository.CategoryIconRepository;
import com.ds.eventwish.ads.AdMobManager;
import com.ds.eventwish.EventWishApplication;
import com.ds.eventwish.ads.AdBannerView;

public class HomeFragment extends BaseFragment implements RecommendedTemplateAdapter.TemplateClickListener {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private FestivalViewModel festivalViewModel;
    private RecommendedTemplateAdapter adapter;
    private CategoriesAdapter categoriesAdapter;
    private GridLayoutManager layoutManager;
    private static final int VISIBLE_THRESHOLD = 5;
    private BottomNavigationView bottomNav;
    private long backPressedTime;
    private static final long BACK_PRESS_DELAY = 2000; // 2 seconds
    private CategoryIconRepository categoryIconRepository;
    private boolean wasInBackground = false;
    private boolean isResumed = false;
    private AdMobManager adMobManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "onViewCreated called");

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        viewModel.init(requireContext());

        // Initialize FestivalViewModel
        festivalViewModel = new ViewModelProvider(this).get(FestivalViewModel.class);

        // Initialize CategoryIconRepository
        categoryIconRepository = CategoryIconRepository.getInstance();

        // Set up UI components
        setupUI();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up SwipeRefreshLayout
        setupSwipeRefresh();

        // Set up observers - do this before loading data
        setupObservers();

        // Set up bottom navigation
        setupBottomNavigation();

        // Set up AdMob Banner
        setupAdBanner();

        // Ensure filter chips are initially hidden
        binding.filterChipsScrollView.setVisibility(View.GONE);
        binding.timeFilterScrollView.setVisibility(View.GONE);

        // Explicitly load categories to ensure they're displayed
        Log.d(TAG, "Explicitly loading categories in onViewCreated");
        viewModel.loadCategories();

        // Ensure categories are visible
        ensureCategoriesVisible();
        
        // Mark as not in background since we're creating the view
        wasInBackground = false;
        
        // Mark fragment as resumed - consistent with lifecycle
        isResumed = true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the CategoryIconRepository
        categoryIconRepository = CategoryIconRepository.getInstance();
        Log.d(TAG, "CategoryIconRepository initialized in onCreate");

        // Handle back press for exit confirmation
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.timeFilterScrollView.getVisibility() == View.VISIBLE ||
                        binding.filterChipsScrollView.getVisibility() == View.VISIBLE) {

                    binding.timeFilterScrollView.setVisibility(View.GONE);
                    binding.filterChipsScrollView.setVisibility(View.GONE);

                } else {
                    if (Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                            .getCurrentDestination().getId() == R.id.navigation_home) {
                        if (backPressedTime + BACK_PRESS_DELAY > System.currentTimeMillis()) {
                            requireActivity().finish();
                        } else {
                            Toast.makeText(requireContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
                            backPressedTime = System.currentTimeMillis();
                        }
                    } else {
                        setEnabled(false);
                        requireActivity().onBackPressed();
                        setEnabled(true);
                    }
                }
            }
        });

    }
        @Override
    public void onResume() {
        super.onResume();
        
        // Mark fragment as resumed
        isResumed = true;
        
        Log.d(TAG, "onResume called" + (wasInBackground ? " (returning from background)" : ""));
        
        // Check if we're coming back from another fragment
        if (wasInBackground) {
            Log.d(TAG, "Returning from background");
            
            // Reset pagination flag when returning to fragment
            viewModel.setPaginationInProgress(false);
            
            // Don't refresh category icons if not necessary
            if (categoryIconRepository != null && !categoryIconRepository.isInitialized()) {
                categoryIconRepository.refreshCategoryIcons();
            }
            
            // First handle category persistence - this is critical for UI consistency
            if (viewModel != null) {
                // Force categories to be visible without reloading them if possible
                if (viewModel.areCategoriesLoaded()) {
                    Log.d(TAG, "Categories already loaded, notifying observers");
                    viewModel.loadCategories(); // This will just notify observers if already loaded
                } else if (categoriesAdapter == null || categoriesAdapter.getItemCount() <= 1) {
                    // Only reload categories if adapter is empty
                    Log.d(TAG, "Explicitly loading categories in onResume because adapter is empty");
                    viewModel.loadCategories();
                }
                
                // Restore selected category
                if (categoriesAdapter != null) {
                    String selectedCategory = viewModel.getSelectedCategory();
                    Log.d(TAG, "Restoring selected category: " + (selectedCategory != null ? selectedCategory : "All"));
                    
                    // Set prevention flag to avoid order changes during selection update
                    categoriesAdapter.preventCategoryChanges(true);
                    try {
                        // Update the selected category in the adapter without reloading data
                        categoriesAdapter.updateSelectedCategory(selectedCategory);
                    } finally {
                        // Always ensure we reset the prevention flag
                        categoriesAdapter.preventCategoryChanges(false);
                    }
                }
            }
            
            // Force UI update without reloading templates
            if (viewModel != null && adapter != null) {
                List<Template> currentTemplates = viewModel.getTemplates().getValue();
                if (currentTemplates != null && !currentTemplates.isEmpty()) {
                    Log.d(TAG, "Refreshing UI with existing " + currentTemplates.size() + " templates");
                    adapter.updateTemplates(new ArrayList<>(currentTemplates));
                    
                    // Restore scroll position
                    int lastPosition = viewModel.getLastVisiblePosition();
                    if (lastPosition > 0) {
                        Log.d(TAG, "Restoring scroll position to: " + lastPosition);
                        binding.templatesRecyclerView.post(() -> {
                            // Check if position is valid
                            if (lastPosition < currentTemplates.size()) {
                                layoutManager.scrollToPosition(lastPosition);
                            }
                        });
                    }
                }
            }
            
            // Only refresh templates if specifically needed (and not often)
            if (viewModel != null && viewModel.shouldRefreshOnReturn()) {
                Log.d(TAG, "Refreshing templates because shouldRefreshOnReturn is true");
                viewModel.loadTemplates(false); // Use false to avoid clearing existing data
            } else {
                Log.d(TAG, "Skipping template refresh, using existing data");
            }
            
            // Update chip selections based on current filters without reloading data
            updateChipSelections();
            
            wasInBackground = false;
        }
        
        // Ensure categories are visible without reloading
        ensureCategoriesVisible();
        
        // Check for unread festivals - using the correct method
        if (festivalViewModel != null) {
            // Only refresh festivals if we're coming from background
            if (wasInBackground) {
                festivalViewModel.refreshFestivals();
            }
            
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
        
        // Mark fragment as paused
        isResumed = false;
        
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

    private void setupUI() {
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

        setupCategoriesAdapter();
        
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

    private void setupCategoriesAdapter() {
        // Initialize the categories adapter with at least the "All" category first
        categoriesAdapter = new CategoriesAdapter(requireContext());
        
        // Set up RecyclerView
        binding.categoriesRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.categoriesRecyclerView.setAdapter(categoriesAdapter);
        
        // Always start with at least some default categories including All
        List<String> initialCategories = new ArrayList<>();
        initialCategories.add("All");
        initialCategories.add("Birthday");
        initialCategories.add("Wedding");
        initialCategories.add("Holiday");
        initialCategories.add("Anniversary");
        initialCategories.add("Graduation");
        initialCategories.add("Party");
        categoriesAdapter.updateCategories(initialCategories);
        
        // Set up click listeners
        categoriesAdapter.setOnCategoryClickListener((category, position) -> {
            if (category.equals("All")) {
                viewModel.setCategory(null);
            } else {
                viewModel.setCategory(category);
                
                // Show loading Snackbar when selecting a category
                showCategoryLoadingSnackbar(category);
            }
            // Update the adapter's selected position
            categoriesAdapter.setSelectedPosition(position);
        });

        categoriesAdapter.setOnMoreClickListener(remainingCategories -> {
            // Display the category selection bottom sheet with all categories
            showCategoriesBottomSheet(remainingCategories);
        });
        
        // If we already have categories in the ViewModel, update the adapter immediately
        if (viewModel != null) {
            Map<String, Integer> categories = viewModel.getCategories().getValue();
            if (categories != null && !categories.isEmpty()) {
                Log.d(TAG, "Setting up categories adapter with " + categories.size() + " categories from ViewModel");
                
                // Convert categories map to list for the adapter
                List<String> categoryList = new ArrayList<>(categories.keySet());
                
                // Add "All" category if it doesn't exist
                if (!categoryList.contains("All")) {
                    categoryList.add(0, "All");
                }
                
                // Update the adapter
                categoriesAdapter.updateCategories(categoryList);
            } 
            
            // Force a category load regardless - this will use cached categories if available
            // or load new ones if needed
            Log.d(TAG, "Forcing initial load of categories");
            viewModel.forceReloadCategories();
        }
    }

    /**
     * Shows a loading Snackbar when a category is selected
     */
    private void showCategoryLoadingSnackbar(String category) {
        if (getActivity() == null || bottomNav == null) return;
        
        // Dismiss any existing Snackbar
        viewModel.dismissCurrentSnackbar();
        
        // Create a new Snackbar
        Snackbar snackbar = Snackbar.make(binding.getRoot(), 
            "Loading " + category + " templates...", 
            Snackbar.LENGTH_SHORT);
        
        // Position the Snackbar above bottom navigation
        View snackbarView = snackbar.getView();
        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
            (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) 
            snackbarView.getLayoutParams();
        params.setMargins(0, 0, 0, bottomNav.getHeight());
        snackbarView.setLayoutParams(params);
        
        // Apply gradient background
        snackbarView.setBackgroundResource(R.drawable.gradient_snackbar_background);
        
        // Apply fade-in animation
        snackbarView.setAlpha(0f);
        snackbarView.animate()
            .alpha(1f)
            .setDuration(300)
            .start();
        
        // Add loading icon
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setTextColor(android.graphics.Color.WHITE);
            
            // Add loading icon with animation
            android.widget.ImageView loadingIcon = new android.widget.ImageView(requireContext());
            loadingIcon.setImageResource(R.drawable.ic_loading);
            
            // Apply rotation animation
            android.view.animation.Animation rotation = android.view.animation.AnimationUtils.loadAnimation(
                    requireContext(), R.anim.rotate);
            loadingIcon.startAnimation(rotation);
            
            // Set icon padding
            loadingIcon.setPadding(0, 0, 16, 0);
            
            // Add the loading icon to the Snackbar layout
            android.widget.LinearLayout snackbarLayout = (android.widget.LinearLayout) textView.getParent();
            snackbarLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            snackbarLayout.addView(loadingIcon, 0);
        }
        
        // Store the Snackbar reference for later dismissal
        viewModel.setCurrentSnackbar(snackbar);
        
        // Show the Snackbar
        snackbar.show();
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
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_categories, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Set rounded corners
        bottomSheetView.setBackgroundResource(R.drawable.rounded_corners_bg);

        RecyclerView categoriesRecyclerView = bottomSheetView.findViewById(R.id.categoriesRecyclerView);

        List<String> visibleCategories = categoriesAdapter.getVisibleCategories();
        List<String> allCategories = new ArrayList<>(viewModel.getCategories().getValue().keySet());

        // Remove all visible categories including "All" and "More"
        List<String> bottomSheetCategories = new ArrayList<>(allCategories);
        bottomSheetCategories.removeAll(visibleCategories);
        bottomSheetCategories.remove("More"); // Just in case "More" is not in visibleCategories

        Log.d(TAG, "Showing " + bottomSheetCategories.size() + " categories in bottom sheet");

        CategoriesAdapter bottomSheetAdapter = new CategoriesAdapter();
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 3);
        categoriesRecyclerView.setLayoutManager(gridLayoutManager);
        // Add padding and rounded corners to the RecyclerView
        categoriesRecyclerView.setPadding(16, 16, 16, 16);
        categoriesRecyclerView.setClipToPadding(false);
        categoriesRecyclerView.setBackgroundResource(R.drawable.rounded_corners_bg); // Make sure to create this drawable
        categoriesRecyclerView.setAdapter(bottomSheetAdapter);
        bottomSheetAdapter.updateCategories(bottomSheetCategories);

        bottomSheetAdapter.setOnCategoryClickListener((category, position) -> {
            Log.d(TAG, "Selected category from bottom sheet: " + category);
            viewModel.setCategory(category);
            categoriesAdapter.updateSelectedCategory(category);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }


    private void setupRecyclerView() {
        adapter = new RecommendedTemplateAdapter(this);
        layoutManager = new GridLayoutManager(requireContext(), 1);
        
        // Configure layout manager to handle full-width section headers
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Section headers should span the full width
                return adapter.getItemViewType(position) == RecommendedTemplateAdapter.VIEW_TYPE_HEADER ? 
                    layoutManager.getSpanCount() : 1;
            }
        });
        
        binding.templatesRecyclerView.setLayoutManager(layoutManager);
        binding.templatesRecyclerView.setAdapter(adapter);
        
        // Set item animator to null to prevent animation glitches
        binding.templatesRecyclerView.setItemAnimator(null);

        // Track if we've already shown the scrolling Snackbar
        final boolean[] hasShownScrollSnackbar = {false};
        
        binding.templatesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private int scrollThreshold = 20;  // Increased threshold for better detection
            private int totalDy = 0; // Track total scroll distance
            private boolean isAppBarHidden = false;
            private boolean endTriggered = false; // Track if we already triggered loading at the end

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                totalDy += dy; // Track total scroll distance
                
                // Show category scrolling Snackbar when user starts scrolling significantly
                if (Math.abs(dy) > 10 && !hasShownScrollSnackbar[0] && viewModel.getSelectedCategory() != null) {
                    showCategoryScrollingSnackbar(viewModel.getSelectedCategory());
                    hasShownScrollSnackbar[0] = true;
                }
                
                try {
                    if (totalDy > scrollThreshold && !isAppBarHidden) {
                        // Scrolling Down → Collapse AppBar
                        isAppBarHidden = true;
                        binding.appBarLayout.setExpanded(false, true);
                    } else if (totalDy < -scrollThreshold && isAppBarHidden) {
                        // Scrolling Up → Expand AppBar
                        isAppBarHidden = false;
                        binding.appBarLayout.setExpanded(true, true);
                    }

                    // Get current scroll position
                    int totalItemCount = layoutManager.getItemCount();
                    int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                    
                    // Check if we need to load more items
                    if (!viewModel.isPaginationInProgress() && // Not already loading more
                        !endTriggered && // Haven't triggered loading at this position
                        lastVisibleItem >= 0 &&
                        totalItemCount > 0 &&
                        lastVisibleItem >= totalItemCount - VISIBLE_THRESHOLD) {
                        
                        // We've reached near the end, trigger loading more
                        Log.d(TAG, "Scroll detected near end, loading more items");
                        endTriggered = true; // Mark that we've triggered loading
                        loadMoreItems(); 
                    } else if (lastVisibleItem < totalItemCount - VISIBLE_THRESHOLD - 5) {
                        // We've scrolled away from the end, reset the trigger
                        endTriggered = false;
                    }
                    
                    // If user has scrolled significantly, save the position
                    if (dy != 0 && firstVisibleItem >= 0) {
                        viewModel.saveScrollPosition(firstVisibleItem);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in scroll listener", e);
                }
            }
            
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                
                // When user starts scrolling, reset the Snackbar show flag for the next pause/resume cycle
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    // Reset flag only if we've been in a different category for a while
                    if (viewModel.hasSelectedCategoryChangedRecently()) {
                        hasShownScrollSnackbar[0] = false;
                    }
                }
                
                // When scrolling stops, check if we need to load more
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();
                    
                    if (lastVisibleItem >= 0 && 
                        totalItemCount > 0 &&
                        lastVisibleItem >= totalItemCount - VISIBLE_THRESHOLD && 
                        !viewModel.isPaginationInProgress()) {
                        
                        loadMoreItems();
                    }
                }
            }
        });

        // Set up impression tracking
        setupImpressionTracking();

        // Observe recommended template IDs
        viewModel.getRecommendedTemplateIds().observe(getViewLifecycleOwner(), recommendedIds -> {
            if (recommendedIds != null && adapter != null) {
                Log.d(TAG, "Recommended template IDs updated: " + recommendedIds.size());
                adapter.setRecommendedTemplateIds(recommendedIds);
            }
        });
    }

    private void setupImpressionTracking() {
        binding.templatesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                try {
                    // Get visible items
                    int firstVisible = layoutManager.findFirstVisibleItemPosition();
                    int lastVisible = layoutManager.findLastVisibleItemPosition();
                    
                    if (firstVisible >= 0 && lastVisible >= 0) {
                        // For each visible position
                        for (int i = firstVisible; i <= lastVisible; i++) {
                            // Get the item at this position
                            if (i >= 0 && i < adapter.getItemCount()) {
                                int viewType = adapter.getItemViewType(i);
                                
                                // Only track templates, not headers
                                if (viewType == RecommendedTemplateAdapter.VIEW_TYPE_TEMPLATE) {
                                    Object item = adapter.getItem(i);
                                    if (item instanceof Template) {
                                        Template template = (Template) item;
                                        if (template.getId() != null) {
                                            viewModel.markTemplateAsViewed(template.getId());
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error tracking template impressions", e);
                }
            }
        });
    }

    private void loadMoreItems() {
        try {
            // Skip if already loading or if pagination is in progress
            if (viewModel.getLoading().getValue() == Boolean.TRUE || 
                viewModel.isPaginationInProgress()) {
                Log.d(TAG, "Skipping loadMoreItems - already loading or pagination in progress");
                return;
            }
            
            Log.d(TAG, "Loading more items");
            binding.bottomLoadingView.setVisibility(View.VISIBLE);
            
            // Disable SwipeRefreshLayout when loading more items
            binding.swipeRefreshLayout.setEnabled(false);
            
            // Show Snackbar for pagination loading
            if (getActivity() != null && bottomNav != null) {
                Snackbar snackbar = Snackbar.make(binding.getRoot(), 
                    "Loading more templates...", 
                    Snackbar.LENGTH_INDEFINITE);
                
                // Position the Snackbar above bottom navigation
                View snackbarView = snackbar.getView();
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                    (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) 
                    snackbarView.getLayoutParams();
                params.setMargins(0, 0, 0, bottomNav.getHeight());
                snackbarView.setLayoutParams(params);
                
                // Make Snackbar more colorful
                snackbarView.setBackgroundResource(R.drawable.gradient_snackbar_background);
                
                // Apply animation to Snackbar
                snackbarView.setAlpha(0f);
                snackbarView.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .start();
                
                // Add loading animation
                TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                if (textView != null) {
                    textView.setTextColor(android.graphics.Color.WHITE);
                    
                    // Use our custom loading icon with animation
                    android.widget.ImageView loadingIcon = new android.widget.ImageView(requireContext());
                    loadingIcon.setImageResource(R.drawable.ic_loading);
                    
                    // Apply rotation animation
                    android.view.animation.Animation rotation = android.view.animation.AnimationUtils.loadAnimation(
                            requireContext(), R.anim.rotate);
                    loadingIcon.startAnimation(rotation);
                    
                    // Set padding for the icon
                    loadingIcon.setPadding(0, 0, 16, 0);
                    
                    // Add the loading icon to the Snackbar layout
                    android.widget.LinearLayout snackbarLayout = (android.widget.LinearLayout) textView.getParent();
                    snackbarLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    snackbarLayout.addView(loadingIcon, 0);
                    
                    // Set text 
                    textView.setText("Loading more templates...");
                }
                
                // Store the snackbar reference to dismiss it later
                viewModel.setCurrentSnackbar(snackbar);
                
                snackbar.show();
            }
            
            int lastPosition = layoutManager.findLastCompletelyVisibleItemPosition();
            int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
            int totalItems = layoutManager.getItemCount();
            
            // Save the current first visible position
            if (firstVisiblePosition >= 0) {
                Log.d(TAG, "Current first visible position: " + firstVisiblePosition);
                // Don't overwrite the saved position with pagination position
                // Only store this temporarily for maintaining position during pagination
            }
            
            if (lastPosition >= 0 && totalItems > 0 && lastPosition < totalItems - 1) {
                // Check if we're near the end of the list
                if (lastPosition + VISIBLE_THRESHOLD >= totalItems) {
                    Log.d(TAG, "Near end of list, loading more items");
                    viewModel.loadMoreIfNeeded(lastPosition, totalItems);
                } else {
                    binding.bottomLoadingView.setVisibility(View.GONE);
                    // Re-enable SwipeRefreshLayout
                    binding.swipeRefreshLayout.setEnabled(true);
                    // Dismiss snackbar if exists
                    viewModel.dismissCurrentSnackbar();
                }
            } else {
                binding.bottomLoadingView.setVisibility(View.GONE);
                // Re-enable SwipeRefreshLayout
                binding.swipeRefreshLayout.setEnabled(true);
                // Dismiss snackbar if exists
                viewModel.dismissCurrentSnackbar();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading more items", e);
            binding.bottomLoadingView.setVisibility(View.GONE);
            // Re-enable SwipeRefreshLayout
            binding.swipeRefreshLayout.setEnabled(true);
            // Dismiss snackbar if exists
            viewModel.dismissCurrentSnackbar();
            viewModel.setPaginationInProgress(false);
        }
    }

    private void setupSwipeRefresh() {
        //hide Retry Layout
        binding.retryLayout.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            // Reset scroll position to avoid inconsistency
            viewModel.saveScrollPosition(0);
            
            // Clear the new templates indicator
            viewModel.clearNewTemplatesFlag();
            
            // Refresh templates
            viewModel.loadTemplates(true);
            
            // Hide the indicator immediately
            binding.refreshIndicator.setVisibility(View.GONE);
            
            // Disable refresh icon temporarily
            binding.refreshIcon.setEnabled(false);
            binding.refreshIcon.postDelayed(() -> binding.refreshIcon.setEnabled(true), 1000);
        });
    }

    private void setupObservers() {
        // Observe templates from the ViewModel
        viewModel.getTemplates().observe(getViewLifecycleOwner(), templates -> {
            // Skip updates if fragment is not resumed to avoid unnecessary processing
            if (!isResumed) {
                Log.d(TAG, "Template update received while fragment not resumed, skipping");
                return;
            }
            
            Log.d(TAG, "Templates updated - size: " + (templates != null ? templates.size() : 0));
            if (templates != null && !templates.isEmpty()) {
                // Create a new list to avoid modification issues
                List<Template> newList = new ArrayList<>(templates);
                
                // Check for new templates
                viewModel.checkForNewTemplates(newList);
                
                // Mark some templates as recommended for testing (in a real app, this would come from the server)
                // This is just for demonstration purposes
                Set<String> recommendedIds = new HashSet<>();
                for (int i = 0; i < Math.min(newList.size(), 5); i++) {
                    Template template = newList.get(i);
                    if (template != null && template.getId() != null) {
                        recommendedIds.add(template.getId());
                        template.setRecommended(true);
                    }
                }
                viewModel.setRecommendedTemplateIds(recommendedIds);
                
                // Update the adapter with the new list
                binding.templatesRecyclerView.post(() -> {
                    // Check if we're in pagination mode or regular update
                    boolean isPagination = viewModel.isPaginationInProgress();
                    
                    // Update adapter with new templates
                    adapter.updateTemplates(newList);
                    
                    // Only restore scroll position if this is NOT a pagination update
                    // For pagination, we want to maintain the current scroll position
                    if (!isPagination) {
                        // Get saved position from ViewModel
                        int savedPosition = viewModel.getLastVisiblePosition();
                        if (savedPosition > 0 && savedPosition < newList.size()) {
                            Log.d(TAG, "Restoring scroll position to: " + savedPosition);
                            layoutManager.scrollToPosition(savedPosition);
                        }
                    } else {
                        // For pagination, we'll maintain the current position
                        // Reset pagination flag now that we've handled the update
                        Log.d(TAG, "Pagination update complete, maintaining current scroll position");
                        viewModel.setPaginationInProgress(false);
                    }
                    
                    // Show empty state if needed
                    binding.emptyView.setVisibility(newList.isEmpty() ? View.VISIBLE : View.GONE);
                    
                    // Hide loading indicator
                    binding.bottomLoadingView.setVisibility(View.GONE);
                    
                    // Re-enable SwipeRefreshLayout after loading
                    binding.swipeRefreshLayout.setEnabled(true);
                    
                    // Dismiss any loading snackbar
                    viewModel.dismissCurrentSnackbar();
                });
            } else if (templates != null && templates.isEmpty()) {
                // Handle empty state
                binding.templatesRecyclerView.post(() -> {
                    adapter.updateTemplates(new ArrayList<>());
                    binding.emptyView.setVisibility(View.VISIBLE);
                    binding.bottomLoadingView.setVisibility(View.GONE);
                    
                    // Re-enable SwipeRefreshLayout after loading
                    binding.swipeRefreshLayout.setEnabled(true);
                    
                    // Dismiss any loading snackbar
                    viewModel.dismissCurrentSnackbar();
                });
            }
        });
        
        // Observe new template IDs
        viewModel.getNewTemplateIds().observe(getViewLifecycleOwner(), newIds -> {
            if (newIds != null && adapter != null) {
                Log.d(TAG, "New template IDs updated: " + newIds.size());
                adapter.setNewTemplateIds(newIds);
            }
        });
        
        // Observe loading state
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "Loading state updated: " + isLoading);
            
            // Only update SwipeRefreshLayout if it's enabled and this is not pagination
            if (!viewModel.isPaginationInProgress() && binding.swipeRefreshLayout.isEnabled()) {
                binding.swipeRefreshLayout.setRefreshing(isLoading);
            }
            
            // Show/hide shimmer based on loading state
            if (isLoading) {
//                binding.shimmerLayout.setVisibility(View.VISIBLE);
//                binding.shimmerLayout.startShimmer();
                binding.templatesRecyclerView.setVisibility(View.GONE);
                binding.emptyView.setVisibility(View.GONE);
            } else {
//                binding.shimmerLayout.stopShimmer();
//                binding.shimmerLayout.setVisibility(View.GONE);
                binding.templatesRecyclerView.setVisibility(View.VISIBLE);
                
                // If this was pagination, dismiss the snackbar
                if (viewModel.isPaginationInProgress()) {
                    viewModel.dismissCurrentSnackbar();
                }
                
                // Re-enable SwipeRefreshLayout
                binding.swipeRefreshLayout.setEnabled(true);
            }
        });
        
        // Observe error state
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "Error: " + error);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Observe category icons
        categoryIconRepository.getCategoryIcons().observe(getViewLifecycleOwner(), categoryIcons -> {
            Log.d(TAG, "Received " + (categoryIcons != null ? categoryIcons.size() : 0) + " category icons");
            // Force refresh of categories adapter if we have categories
            if (categoriesAdapter != null && categoriesAdapter.getItemCount() > 0) {
                categoriesAdapter.notifyDataSetChanged();
            }
        });
        
        // Observe categories from the ViewModel
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            // Skip updates if fragment is not resumed to avoid unnecessary processing
            if (!isResumed) {
                Log.d(TAG, "Categories update received while fragment not resumed, skipping");
                return;
            }
            
            if (categories != null && !categories.isEmpty()) {
                Log.d(TAG, "Received " + categories.size() + " categories");
                
                // Store the current selection before update
                String currentlySelectedCategory = null;
                int currentSelectedPosition = -1;
                
                if (categoriesAdapter != null) {
                    currentSelectedPosition = categoriesAdapter.getSelectedPosition();
                    List<String> visibleCategories = categoriesAdapter.getVisibleCategories();
                    if (currentSelectedPosition >= 0 && currentSelectedPosition < visibleCategories.size()) {
                        currentlySelectedCategory = visibleCategories.get(currentSelectedPosition);
                        Log.d(TAG, "Current selected category before update: " + currentlySelectedCategory);
                    }
                }
                
                // Make sure category icons are loaded
                categoryIconRepository.loadCategoryIcons();
                
                // Convert categories map to list for the adapter
                List<String> categoryList = new ArrayList<>(categories.keySet());
                
                // Add "All" category if it doesn't exist
                if (!categoryList.contains("All")) {
                    categoryList.add(0, "All");
                }
                
                // Update adapter with prevention flag to maintain stability
                if (categoriesAdapter != null) {
                    categoriesAdapter.preventCategoryChanges(true);
                    try {
                        categoriesAdapter.updateCategories(categoryList);
                        
                        // Restore selection if we had one
                        if (currentlySelectedCategory != null) {
                            categoriesAdapter.updateSelectedCategory(currentlySelectedCategory);
                        } else {
                            // Use ViewModel's selected category if available
                            String selectedCategory = viewModel.getSelectedCategory();
                            if (selectedCategory != null) {
                                categoriesAdapter.updateSelectedCategory(selectedCategory);
                            }
                        }
                    } finally {
                        categoriesAdapter.preventCategoryChanges(false);
                    }
                }
            }
        });
        
        // Observe new templates indicator
        viewModel.getHasNewTemplates().observe(getViewLifecycleOwner(), hasNew -> {
            binding.refreshIndicator.setVisibility(hasNew ? View.VISIBLE : View.GONE);
        });
        
        // Observe unread festival count
        festivalViewModel.getUnreadCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                binding.notificationBadge.setVisibility(View.VISIBLE);
                binding.notificationBadge.setText(count <= 9 ? String.valueOf(count) : "9+");
            } else {
                binding.notificationBadge.setVisibility(View.GONE);
            }
        });
    }

    private void setupBottomNavigation() {
        if (getActivity() instanceof MainActivity) {
            bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            bottomNav.setVisibility(View.VISIBLE);
            // Set the selected item to home
            bottomNav.setSelectedItemId(R.id.navigation_home);
        }
    }

    /**
     * Set up AdMob banner
     */
    private void setupAdBanner() {
        try {
            // Try to get AdMobManager
            adMobManager = ((EventWishApplication) requireActivity().getApplication()).getAdMobManager();
            
            // Set up the AdBanner if AdMobManager is initialized
            if (adMobManager != null && adMobManager.isInitialized()) {
                Log.d(TAG, "Setting up ad banner with AdMobManager");
                
                // Observe ad enabled state
                adMobManager.getEnabledState().observe(getViewLifecycleOwner(), isEnabled -> {
                    if (binding != null && binding.adBannerView != null) {
                        if (isEnabled) {
                            binding.adBannerView.setVisibility(View.VISIBLE);
                        } else {
                            binding.adBannerView.setVisibility(View.GONE);
                        }
                    }
                });
                
                // Set test mode based on AdMobManager
                if (binding.adBannerView != null) {
                    binding.adBannerView.setTestMode(adMobManager.isTestModeEnabled());
                }
            } else {
                // Hide the banner if AdMobManager is not initialized
                Log.d(TAG, "AdMobManager not initialized, hiding ad banner");
                if (binding != null && binding.adBannerView != null) {
                    binding.adBannerView.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up ad banner: " + e.getMessage(), e);
            // Hide the banner on error
            if (binding != null && binding.adBannerView != null) {
                binding.adBannerView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onTemplateClick(Template template) {
        if (template == null) return;
        
        // Mark the template as viewed
        viewModel.markTemplateAsViewed(template.getId());
        
        // Save the current scroll position
        int position = layoutManager.findFirstVisibleItemPosition();
        if (position >= 0) {
            viewModel.saveScrollPosition(position);
        }
        
        // Navigate to the template detail screen
        Bundle args = new Bundle();
        args.putString("templateId", template.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_home_to_template_detail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Don't clear the position when destroying the view
        // This allows us to restore the position when coming back
        
        binding = null;
    }

    // Test method to simulate new templates (for development/testing only)
    private void testNewTemplatesIndicator() {
        // Toggle the indicator for testing
        boolean currentState = binding.refreshIndicator.getVisibility() == View.VISIBLE;
        binding.refreshIndicator.setVisibility(currentState ? View.GONE : View.VISIBLE);
        
        // Show a toast message
        Toast.makeText(requireContext(), 
                      currentState ? "Test: Indicator hidden" : "Test: New templates indicator shown", 
                      Toast.LENGTH_SHORT).show();
        
        Log.d(TAG, "Test: Toggled new templates indicator to " + !currentState);
    }

    /**
     * Ensure that the categories section is visible with proper state
     */
    private void ensureCategoriesVisible() {
        // Make sure the categories RecyclerView is visible
        if (binding.categoriesRecyclerView != null) {
            binding.categoriesRecyclerView.setVisibility(View.VISIBLE);
            
            // Log the current state
            Log.d(TAG, "Categories RecyclerView visibility: " + 
                  (binding.categoriesRecyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT VISIBLE"));
            
            // Check if adapter is set
            if (binding.categoriesRecyclerView.getAdapter() == null) {
                Log.d(TAG, "Categories RecyclerView adapter is null, setting adapter");
                binding.categoriesRecyclerView.setAdapter(categoriesAdapter);
            }
            
            // Check if adapter has data
            if (categoriesAdapter != null) {
                Log.d(TAG, "Categories adapter item count: " + categoriesAdapter.getItemCount());
                
                // Save existing selection if any
                String currentSelectedCategory = categoriesAdapter.getSelectedCategory();
                int currentSelectedPosition = categoriesAdapter.getSelectedPosition();
                
                // Only load categories if adapter is completely empty
                if (categoriesAdapter.getItemCount() == 0 && viewModel != null) {
                    Map<String, Integer> categories = viewModel.getCategories().getValue();
                    if (categories != null && !categories.isEmpty()) {
                        Log.d(TAG, "Updating categories adapter with " + categories.size() + " categories from cached data");
                        
                        // Convert categories map to list for the adapter
                        List<String> categoryList = new ArrayList<>(categories.keySet());
                        
                        // Add "All" category if it doesn't exist
                        if (!categoryList.contains("All")) {
                            categoryList.add(0, "All");
                        }
                        
                        // Prevent category changes during this update
                        categoriesAdapter.preventCategoryChanges(true);
                        try {
                            // Update the adapter
                            categoriesAdapter.updateCategories(categoryList);
                            
                            // Restore selected category - try multiple sources in order of priority
                            String selectedCategory = null;
                            
                            // 1. Previously selected category from adapter if it exists
                            if (currentSelectedCategory != null && !currentSelectedCategory.equals("All") &&
                                    categoryList.contains(currentSelectedCategory)) {
                                selectedCategory = currentSelectedCategory;
                                Log.d(TAG, "Restoring selection from adapter: " + selectedCategory);
                            } 
                            // 2. Category from ViewModel if present
                            else if (viewModel.getSelectedCategory() != null) {
                                selectedCategory = viewModel.getSelectedCategory();
                                Log.d(TAG, "Restoring selection from ViewModel: " + selectedCategory);
                            }
                            
                            // Update the selection in the adapter
                            if (selectedCategory != null) {
                                categoriesAdapter.updateSelectedCategory(selectedCategory);
                            } else {
                                // Default to "All"
                                categoriesAdapter.setSelectedPosition(0);
                            }
                        } finally {
                            categoriesAdapter.preventCategoryChanges(false);
                        }
                    } else {
                        // If no categories are available, only then load them from the repository
                        Log.d(TAG, "No categories available in ViewModel, loading categories");
                        viewModel.loadCategories();
                        
                        // Add "All" category as a fallback only if adapter is empty
                        List<String> fallbackList = new ArrayList<>();
                        fallbackList.add("All");
                        categoriesAdapter.updateCategories(fallbackList);
                    }
                } else if (categoriesAdapter.getItemCount() > 0) {
                    // If adapter already has data, just make sure the selected category is correct
                    // without changing the order or triggering a reload
                    if (viewModel != null) {
                        String selectedCategory = viewModel.getSelectedCategory();
                        if (selectedCategory != null || currentSelectedPosition != 0) {
                            Log.d(TAG, "Ensuring selected category is consistent with ViewModel: " + 
                                  (selectedCategory != null ? selectedCategory : "All"));
                            
                            // Only update if needed, using prevention flag
                            categoriesAdapter.preventCategoryChanges(true);
                            try {
                                categoriesAdapter.updateSelectedCategory(selectedCategory);
                            } finally {
                                categoriesAdapter.preventCategoryChanges(false);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows a Snackbar when the user scrolls in a specific category
     */
    private void showCategoryScrollingSnackbar(String category) {
        if (getActivity() == null || bottomNav == null || category == null) return;
        
        // Dismiss any existing Snackbar
        viewModel.dismissCurrentSnackbar();
        
        // Create a new Snackbar with a browsing message
        String displayCategory = category.equals("All") ? "all" : category;
        Snackbar snackbar = Snackbar.make(binding.getRoot(), 
            "Browsing " + displayCategory + " templates", 
            Snackbar.LENGTH_SHORT);
        
        // Position the Snackbar above bottom navigation
        View snackbarView = snackbar.getView();
        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
            (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) 
            snackbarView.getLayoutParams();
        params.setMargins(0, 0, 0, bottomNav.getHeight());
        snackbarView.setLayoutParams(params);
        
        // Apply gradient background
        snackbarView.setBackgroundResource(R.drawable.gradient_snackbar_background);
        
        // Apply fade-in animation
        snackbarView.setAlpha(0f);
        snackbarView.animate()
            .alpha(1f)
            .setDuration(300)
            .start();
        
        // Store the Snackbar reference for later dismissal
        viewModel.setCurrentSnackbar(snackbar);
        
        // Show the Snackbar
        snackbar.show();
    }
}

