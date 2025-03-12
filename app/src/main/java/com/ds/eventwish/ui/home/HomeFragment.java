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
import com.ds.eventwish.ui.home.adapter.TemplateAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.JustifyContent;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexWrap;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.ds.eventwish.ui.festival.FestivalViewModel;
import com.ds.eventwish.data.repository.CategoryIconRepository;

public class HomeFragment extends BaseFragment implements TemplateAdapter.OnTemplateClickListener {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private FestivalViewModel festivalViewModel;
    private TemplateAdapter adapter;
    private CategoriesAdapter categoriesAdapter;
    private GridLayoutManager layoutManager;
    private static final int VISIBLE_THRESHOLD = 5;
    private BottomNavigationView bottomNav;
    private long backPressedTime;
    private static final long BACK_PRESS_DELAY = 2000; // 2 seconds
    private CategoryIconRepository categoryIconRepository;
    private boolean wasInBackground = false;

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

        // Set up observers
        setupObservers();

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
        
        Log.d(TAG, "onResume called");
        
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
        
        Log.d(TAG, "onResume - Last position: " + viewModel.getLastVisiblePosition());
        
        // Check for new templates when the fragment resumes
        if (viewModel != null) {
            List<Template> currentTemplates = viewModel.getTemplates().getValue();
            if (currentTemplates != null && !currentTemplates.isEmpty()) {
                Log.d(TAG, "Checking for new templates on resume");
                // Restore scroll position safely
                final int position = viewModel.getLastVisiblePosition();
                if (position > 0 && position < currentTemplates.size()) {
                    // Use post to ensure RecyclerView is ready
                    binding.templatesRecyclerView.post(() -> {
                        Log.d(TAG, "Restoring scroll position in onResume: " + position);
                        layoutManager.scrollToPosition(position);
                    });
                }
                
                // Load templates with a delay to prevent timeout
                binding.templatesRecyclerView.postDelayed(() -> {
                    if (isAdded() && !isDetached() && !isRemoving()) {
                        // Only check for new templates if we're still active
                        if (!viewModel.getLoading().getValue()) {
                            viewModel.checkForNewTemplates(currentTemplates);
                            if (viewModel.getCurrentPage() > 1) {
                                Log.d(TAG, "Ensuring all pages are loaded up to: " + viewModel.getCurrentPage());
                                viewModel.loadTemplates(false);
                            }
                        }
                    }
                }, 1000); // Increased delay to 1 second
            }
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

        categoriesAdapter = new CategoriesAdapter();
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(requireContext());
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW);
        flexboxLayoutManager.setJustifyContent(JustifyContent.FLEX_START);
        flexboxLayoutManager.setAlignItems(AlignItems.FLEX_START);
        flexboxLayoutManager.setFlexWrap(FlexWrap.WRAP);

        binding.categoriesRecyclerView.setLayoutManager(flexboxLayoutManager);
        binding.categoriesRecyclerView.setAdapter(categoriesAdapter);
        
        // Ensure the categories RecyclerView is visible
        binding.categoriesRecyclerView.setVisibility(View.VISIBLE);
        
        // Log the initial state
        Log.d(TAG, "Categories RecyclerView initial visibility: " + 
              (binding.categoriesRecyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "NOT VISIBLE"));
        
        // Always add the "All" category first
        List<String> initialCategories = new ArrayList<>();
        initialCategories.add("All");
        categoriesAdapter.updateCategories(initialCategories);
        
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
            } else {
                // If no categories are available, load them
                Log.d(TAG, "No categories available in ViewModel, loading categories");
                viewModel.loadCategories();
            }
        }

        categoriesAdapter.setOnCategoryClickListener((category, position) -> {
            if (category.equals("All")) {
                viewModel.setCategory(null);
            } else {
                viewModel.setCategory(category);
            }
            // Update the adapter's selected position
            categoriesAdapter.setSelectedPosition(position);
        });

        categoriesAdapter.setOnMoreClickListener(remainingCategories -> {
            // Get all categories except "All" which is already visible
            List<String> allCategoriesExceptAll = new ArrayList<>(viewModel.getCategories().getValue().keySet());
            // Remove categories that are already visible (except "All" which should be in the bottom sheet)
            for (int i = 1; i < categoriesAdapter.getVisibleCategories().size() - 1; i++) {
                allCategoriesExceptAll.remove(categoriesAdapter.getVisibleCategories().get(i));
            }
            Log.d(TAG, "Showing more categories, count: " + allCategoriesExceptAll.size());
            showCategoriesBottomSheet(allCategoriesExceptAll);
        });

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
        adapter = new TemplateAdapter(this);
        layoutManager = new GridLayoutManager(requireContext(), 1);
        binding.templatesRecyclerView.setLayoutManager(layoutManager);

        binding.templatesRecyclerView.setAdapter(adapter);
        
        // Set item animator to null to prevent animation glitches
        binding.templatesRecyclerView.setItemAnimator(null);

        binding.templatesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private int scrollThreshold = 20;  // Increased threshold for better detection
            private int totalDy = 0; // Track total scroll distance
            private boolean isAppBarHidden = false;

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                totalDy += dy; // Track total scroll distance

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

                    // Preserve existing "Load More" logic
                    int totalItemCount = layoutManager.getItemCount();
                    int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();

                    if (lastVisibleItem >= 0 &&
                            totalItemCount > 0 &&
                            lastVisibleItem >= totalItemCount - VISIBLE_THRESHOLD) {
                        loadMoreItems();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in scroll listener", e);
                }
            }
        });

    }

    private void setupImpressionTracking() {
        binding.templatesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                try {
                    // Track visible templates for impression
                    int firstVisible = layoutManager.findFirstVisibleItemPosition();
                    int lastVisible = layoutManager.findLastVisibleItemPosition();
                    
                    if (firstVisible >= 0 && lastVisible >= 0) {
                        List<Template> templates = viewModel.getTemplates().getValue();
                        if (templates != null) {
                            for (int i = firstVisible; i <= lastVisible && i < templates.size(); i++) {
                                Template template = templates.get(i);
                                if (template != null && template.getId() != null) {
                                    viewModel.markTemplateAsViewed(template.getId());
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
            Log.d(TAG, "Loading more items");
            binding.bottomLoadingView.setVisibility(View.VISIBLE);
            
            int lastPosition = layoutManager.findLastCompletelyVisibleItemPosition();
            int totalItems = layoutManager.getItemCount();
            
            if (lastPosition >= 0 && totalItems > 0 && lastPosition < totalItems - 1) {
                viewModel.loadMoreIfNeeded(lastPosition, totalItems);
            } else {
                binding.bottomLoadingView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading more items", e);
            binding.bottomLoadingView.setVisibility(View.GONE);
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
            Log.d(TAG, "Templates updated - size: " + (templates != null ? templates.size() : 0));
            if (templates != null && binding != null) {
                // Create a new list to avoid modification issues
                List<Template> newList = new ArrayList<>(templates);
                
                // Check for new templates
                viewModel.checkForNewTemplates(newList);
                
                // Update the adapter with the new list
                binding.templatesRecyclerView.post(() -> {
                    // Check if binding is still valid
                    if (binding == null) {
                        Log.d(TAG, "Binding is null in post runnable, skipping UI update");
                        return;
                    }
                    
                    adapter.submitList(newList);
                    
                    // Restore scroll position if needed
                    int savedPosition = viewModel.getLastVisiblePosition();
                    if (savedPosition > 0 && savedPosition < newList.size()) {
                        Log.d(TAG, "Restoring scroll position to: " + savedPosition);
                        layoutManager.scrollToPosition(savedPosition);
                    }
                    
                    // Show empty state if needed
                    if (binding != null && binding.emptyView != null) {
                        binding.emptyView.setVisibility(newList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
            }
        });
        
        // Observe new template IDs
        viewModel.getNewTemplateIds().observe(getViewLifecycleOwner(), newIds -> {
            if (newIds != null && adapter != null) {
                Log.d(TAG, "New template IDs updated: " + newIds.size());
                adapter.setNewTemplates(newIds);
            }
        });
        
        // Observe loading state
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "Loading state updated: " + isLoading);
            if (binding != null) {
                binding.swipeRefreshLayout.setRefreshing(isLoading);
                
                // Show/hide shimmer based on loading state
                if (isLoading) {
//                    binding.shimmerLayout.setVisibility(View.VISIBLE);
//                    binding.shimmerLayout.startShimmer();
                    binding.templatesRecyclerView.setVisibility(View.GONE);
                    if (binding.emptyView != null) {
                        binding.emptyView.setVisibility(View.GONE);
                    }
                } else {
//                    binding.shimmerLayout.stopShimmer();
//                    binding.shimmerLayout.setVisibility(View.GONE);
                    binding.templatesRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
        
        // Observe error state
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty() && getContext() != null) {
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
            if (categories != null && !categories.isEmpty()) {
                Log.d(TAG, "Received " + categories.size() + " categories");
                
                // Make sure category icons are loaded
                categoryIconRepository.loadCategoryIcons();
                
                // Convert categories map to list for the adapter
                List<String> categoryList = new ArrayList<>(categories.keySet());
                
                // Add "All" category if it doesn't exist
                if (!categoryList.contains("All")) {
                    categoryList.add(0, "All");
                }
                
                // Update the adapter
                categoriesAdapter.updateCategories(categoryList);
            }
        });
        
        // Observe new templates indicator
        viewModel.getHasNewTemplates().observe(getViewLifecycleOwner(), hasNew -> {
            if (binding != null) {
                binding.refreshIndicator.setVisibility(hasNew ? View.VISIBLE : View.GONE);
            }
        });
        
        // Observe unread festival count
        festivalViewModel.getUnreadCount().observe(getViewLifecycleOwner(), count -> {
            if (binding != null) {
                if (count != null && count > 0) {
                    binding.notificationBadge.setVisibility(View.VISIBLE);
                    binding.notificationBadge.setText(count <= 9 ? String.valueOf(count) : "9+");
                } else {
                    binding.notificationBadge.setVisibility(View.GONE);
                }
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
        
        // Cancel any pending operations on the RecyclerView
        if (binding != null && binding.templatesRecyclerView != null) {
            binding.templatesRecyclerView.removeCallbacks(null);
        }
        
        // Clear binding reference
        binding = null;
        
        Log.d(TAG, "onDestroyView: binding set to null");
    }

    // Test method to simulate new templates (for development/testing only)
    private void testNewTemplatesIndicator() {
        // Check if binding is still valid
        if (binding == null) {
            Log.d(TAG, "Binding is null in testNewTemplatesIndicator, skipping UI update");
            return;
        }
        
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
     * Ensure that the categories section is visible
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
                
                // If adapter is empty or only has "All" category, check for categories in ViewModel
                if ((categoriesAdapter.getItemCount() <= 1) && viewModel != null) {
                    Map<String, Integer> categories = viewModel.getCategories().getValue();
                    if (categories != null && !categories.isEmpty()) {
                        Log.d(TAG, "Updating categories adapter with " + categories.size() + " categories");
                        
                        // Convert categories map to list for the adapter
                        List<String> categoryList = new ArrayList<>(categories.keySet());
                        
                        // Add "All" category if it doesn't exist
                        if (!categoryList.contains("All")) {
                            categoryList.add(0, "All");
                        }
                        
                        // Update the adapter
                        categoriesAdapter.updateCategories(categoryList);
                    } else {
                        // If no categories are available, load them
                        Log.d(TAG, "No categories available in ViewModel, loading categories");
                        viewModel.loadCategories();
                        
                        // Add "All" category as a fallback
                        if (categoriesAdapter.getItemCount() == 0) {
                            List<String> fallbackList = new ArrayList<>();
                            fallbackList.add("All");
                            categoriesAdapter.updateCategories(fallbackList);
                        }
                    }
                    
                    // Make sure the selected category is highlighted
                    if (viewModel != null) {
                        String selectedCategory = viewModel.getSelectedCategory();
                        categoriesAdapter.updateSelectedCategory(selectedCategory);
                    }
                }
            }
        }
    }
}
