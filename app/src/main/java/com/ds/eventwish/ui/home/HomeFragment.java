package com.ds.eventwish.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
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
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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
import com.ds.eventwish.data.model.Category;
import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.ui.base.BaseFragment;
import com.ds.eventwish.ui.home.adapter.RecommendedTemplateAdapter;
import com.ds.eventwish.ui.home.adapter.CategoriesAdapter;
import com.ds.eventwish.data.repository.CategoryIconRepository;
import com.ds.eventwish.data.repository.TemplateRepository;
import com.ds.eventwish.data.remote.ApiClient;
import com.ds.eventwish.data.remote.ApiService;
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

import com.google.android.material.appbar.AppBarLayout;

import com.ds.eventwish.ui.home.SortOption;
import com.ds.eventwish.ui.home.TimeFilter;

import java.util.Collections;
import android.graphics.Rect;

import com.ds.eventwish.ads.InterstitialAdManager;
import com.ds.eventwish.ads.AdMobRepository;
import com.ds.eventwish.data.repository.UserRepository;
import com.ds.eventwish.utils.AnalyticsUtils;
import com.ds.eventwish.ui.ads.SponsoredAdCarousel;

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
    private OnBackPressedCallback backPressedCallback;
    private int lastScrollPosition = 0;
    private InterstitialAdManager interstitialAdManager;
    private boolean isAdLoading = false;
    private Template pendingTemplate = null;
    private int templateClickCount = 0;
    private static final int AD_SHOW_THRESHOLD = 3; // Show ad after every 3 template clicks
<<<<<<< HEAD
    private SponsoredAdView sponsoredAdView;
    private boolean hasShownEndMessage = false; // Add this flag at the class level
    private long lastPaginationCheck = 0;
    private static final long PAGINATION_CHECK_INTERVAL = 1500; // 1.5 seconds between checks
=======
    private SponsoredAdCarousel sponsoredAdCarousel;
>>>>>>> c9d6bc131c97ff1e271900b9a0cfd19fd38917f4

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        // Initialize ViewModels
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        viewModel.init(requireContext());
        festivalViewModel = new ViewModelProvider(this).get(FestivalViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel.init(requireContext());

        Log.d(TAG, "onViewCreated called");

        // Initialize CategoryIconRepository
        if (categoryIconRepository == null) {
            try {
                categoryIconRepository = CategoryIconRepository.getInstance(requireContext());
                Log.d(TAG, "CategoryIconRepository initialized in onViewCreated");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize CategoryIconRepository in onViewCreated", e);
                // Use default implementation if available
                categoryIconRepository = CategoryIconRepository.getInstance();
            }
            
            // Force immediate refresh of category icons
            if (categoryIconRepository != null) {
                categoryIconRepository.refreshCategoryIcons();
            }
        }

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
        
        // Restore saved state if available
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }

        // Ensure filter chips are initially hidden
        binding.filterChipsScrollView.setVisibility(View.GONE);
        binding.timeFilterScrollView.setVisibility(View.GONE);

        // Force immediate category loading - now with higher priority loading
        Log.d(TAG, "Forcing immediate category loading in onViewCreated");
        // Add a small delay to ensure UI is ready
        binding.getRoot().post(() -> {
            // First check if repository has initialized categories
            TemplateRepository repo = TemplateRepository.getInstance();
            Map<String, Integer> existingCategories = repo.getCategories().getValue();
            
            if (existingCategories != null && !existingCategories.isEmpty()) {
                Log.d(TAG, "Categories already loaded from repository: " + existingCategories.size());
                // Ensure the adapter has these categories
                ensureCategoriesVisible();
            } else {
                Log.d(TAG, "Repository doesn't have categories, forcing reload");
                viewModel.forceReloadCategories();
            }
            
            // Always refresh icons regardless of categories
            if (categoryIconRepository != null) {
                Log.d(TAG, "Refreshing category icons in onViewCreated post");
                categoryIconRepository.refreshCategoryIcons();
            }
        });

        // Ensure categories are visible
        ensureCategoriesVisible();
        
        // Restore fullscreen mode if needed
        if (viewModel.isFullscreenMode()) {
            toggleFullscreenMode(true);
        }
        
        // Mark as not in background since we're creating the view
        wasInBackground = false;
        
        // Mark fragment as resumed - consistent with lifecycle
        isResumed = true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the CategoryIconRepository
        try {
            categoryIconRepository = CategoryIconRepository.getInstance(requireContext());
            Log.d(TAG, "CategoryIconRepository initialized in onCreate");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize CategoryIconRepository", e);
            // Use default implementation if available
            categoryIconRepository = CategoryIconRepository.getInstance();
        }
        
        // Initialize the TemplateRepository to ensure categories are loaded
        try {
            TemplateRepository.init(requireContext());
            Log.d(TAG, "TemplateRepository initialized in onCreate");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TemplateRepository", e);
            // Fallback to getInstance()
            TemplateRepository.getInstance();
        }

        // Initialize InterstitialAdManager
        ApiService apiService = ApiClient.getClient();
        interstitialAdManager = new InterstitialAdManager(requireContext());
        Log.d(TAG, "InterstitialAdManager initialized in onCreate");

        // Handle back press for exit confirmation
        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding != null && (binding.timeFilterScrollView.getVisibility() == View.VISIBLE ||
                        binding.filterChipsScrollView.getVisibility() == View.VISIBLE)) {

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
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // No need to duplicate tracking code here as it's now in BaseFragment
        
        isResumed = true;
        
        // Reset pagination and end message flags on resume
        hasShownEndMessage = false;
        viewModel.setPaginationInProgress(false);
        lastPaginationCheck = 0;
        
        // If templates are already loaded, clear any error state
        if (viewModel.getTemplates().getValue() != null && 
            !viewModel.getTemplates().getValue().isEmpty()) {
            hideError();
        }
        
        // Check if we're coming back from another fragment
        if (wasInBackground) {
            Log.d(TAG, "Returning from background");
            
            // Reset pagination flag when returning to fragment
            viewModel.setPaginationInProgress(false);
            
            // Don't refresh category icons if not necessary
            if (categoryIconRepository != null) {
                if (!categoryIconRepository.isInitialized()) {
                    Log.d(TAG, "Refreshing category icons in onResume - repository not initialized");
                    categoryIconRepository.refreshCategoryIcons();
                } else {
                    // Check if it's been a while since we last refreshed
                    Log.d(TAG, "Category icon repository already initialized, checking if refresh needed");
                }
            }
            
            // First handle category persistence - this is critical for UI consistency
            if (viewModel != null) {
                // Force categories to be visible without reloading them if possible
                if (viewModel.areCategoriesLoaded()) {
                    Log.d(TAG, "Categories already loaded, refreshing UI");
                    
                    // First get the saved category selection
                    String selectedCategory = viewModel.getSelectedCategory();
                    Log.d(TAG, "Restoring selected category: " + (selectedCategory != null ? selectedCategory : "All"));
                    
                    // Set prevention flag to avoid order changes during selection update
                    if (categoriesAdapter != null) {
                        categoriesAdapter.preventCategoryChanges(true);
                        try {
                            // Refresh categories UI but maintain selection
                            ensureCategoriesVisible();
                            
                            // Explicitly apply selection after ensuring visibility
                            categoriesAdapter.updateSelectedCategory(selectedCategory);
                        } finally {
                            // Always ensure we reset the prevention flag
                            categoriesAdapter.preventCategoryChanges(false);
                        }
                    }
                } else if (categoriesAdapter == null || categoriesAdapter.getItemCount() <= 1) {
                    // Only reload categories if adapter is empty
                    Log.d(TAG, "Explicitly loading categories in onResume because adapter is empty");
                    viewModel.loadCategories();
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
        } else {
            // Even if we're not coming from background, still ensure categories are displayed properly
            ensureCategoriesVisible();
        }
        
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

        // Load interstitial ad if not already loaded or loading
        if (!interstitialAdManager.isAdLoaded() && !isAdLoading) {
            isAdLoading = true;
            interstitialAdManager.loadAd(new InterstitialAdManager.InterstitialAdCallback() {
                @Override
                public void onAdLoaded() {
                    isAdLoading = false;
                    Log.d(TAG, "Interstitial ad loaded successfully");
                    
                    // If there's a pending template, show the ad now
                    if (pendingTemplate != null) {
                        showAdAndNavigate(pendingTemplate);
                        pendingTemplate = null;
                    }
                }

                @Override
                public void onAdFailedToLoad(String error) {
                    isAdLoading = false;
                    Log.e(TAG, "Failed to load interstitial ad: " + error);
                    
                    // If there's a pending template, navigate directly
                    if (pendingTemplate != null) {
                        navigateToTemplateDetail(pendingTemplate.getId());
                        pendingTemplate = null;
                    }
                }

                @Override
                public void onAdShown() {
                    Log.d(TAG, "Interstitial ad shown");
                }

                @Override
                public void onAdDismissed() {
                    Log.d(TAG, "Interstitial ad dismissed");
                    // Load the next ad after this one is dismissed
                    loadNextAd();
                }

                @Override
                public void onAdClicked() {
                    Log.d(TAG, "Interstitial ad clicked");
                }

                @Override
                public void onAdShowFailed(String error) {
                    Log.e(TAG, "Failed to show interstitial ad: " + error);
                    // If there's a pending template, navigate directly
                    if (pendingTemplate != null) {
                        navigateToTemplateDetail(pendingTemplate.getId());
                        pendingTemplate = null;
                    }
                }
            });
        }

<<<<<<< HEAD
        // Handle sponsored ad when the fragment resumes
        if (sponsoredAdView != null) {
            try {
                // First make sure the sponsoredAdView is visible
                sponsoredAdView.setVisibility(View.VISIBLE);
                
                Log.d(TAG, "Handling sponsored ad in onResume");
                
                // Call methods in a specific order to ensure proper state restoration
                sponsoredAdView.handleResume();
                
                // Force reload of the ad content to ensure image is displayed
                sponsoredAdView.forceReload();
                
                // Ensure text fields are visible if they contain content
                sponsoredAdView.ensureTextVisible();
                
                // Enable rotation if supported
                sponsoredAdView.enableRotation(true);
                
                Log.d(TAG, "Sponsored ad handling complete in onResume");
            } catch (Exception e) {
                Log.e(TAG, "Error handling sponsored ad in onResume: " + e.getMessage());
            }
=======
        // Refresh sponsored ads when the fragment resumes
        if (sponsoredAdCarousel != null) {
            // Use our category-based targeting method instead of simple refresh
            refreshSponsoredAdForCurrentCategory();
            Log.d(TAG, "Refreshed sponsored ads with category targeting on resume");
>>>>>>> c9d6bc131c97ff1e271900b9a0cfd19fd38917f4
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("sortOption", viewModel.getCurrentSortOption());
        outState.putSerializable("timeFilter", viewModel.getCurrentTimeFilter());
        outState.putBoolean("isFullscreenMode", viewModel.isFullscreenMode());
        outState.putInt("lastScrollPosition", lastScrollPosition);
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Restore scroll position
            final int scrollPosition = savedInstanceState.getInt("scrollPosition", 0);
            if (scrollPosition > 0) {
                binding.templatesRecyclerView.post(() -> binding.templatesRecyclerView.scrollToPosition(scrollPosition));
            }

            // Restore fullscreen mode
            boolean isFullscreen = savedInstanceState.getBoolean("isFullscreen", false);
            if (isFullscreen) {
                toggleFullscreenMode(true);
            }

            // Restore selected category
            String selectedCategory = savedInstanceState.getString("selectedCategory");
            if (selectedCategory != null) {
                viewModel.setCategory(selectedCategory);
            }

            // Restore sort option and time filter
            SortOption sortOption = (SortOption) savedInstanceState.getSerializable("sortOption");
            if (sortOption != null) {
                viewModel.setSortOption(sortOption);
            }

            TimeFilter timeFilter = (TimeFilter) savedInstanceState.getSerializable("timeFilter");
            if (timeFilter != null) {
                viewModel.setTimeFilter(timeFilter);
            }

            // Restore filter visibility
            if (savedInstanceState.getBoolean("filterChipsVisible", false)) {
                binding.filterChipsScrollView.setVisibility(View.VISIBLE);
            }
            if (savedInstanceState.getBoolean("timeFilterVisible", false)) {
                binding.timeFilterScrollView.setVisibility(View.VISIBLE);
            }
        }
    }

    private int getScrollPosition() {
        if (binding != null && binding.templatesRecyclerView != null && binding.templatesRecyclerView.getLayoutManager() != null) {
            if (binding.templatesRecyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                return ((LinearLayoutManager) binding.templatesRecyclerView.getLayoutManager())
                        .findFirstVisibleItemPosition();
            }
        }
        return 0;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (backPressedCallback != null) {
            backPressedCallback.remove();
        }
        if (interstitialAdManager != null) {
            interstitialAdManager.destroy();
        }
        
        // Cleanup sponsored ad view
        if (sponsoredAdCarousel != null) {
            sponsoredAdCarousel.cleanup();
            sponsoredAdCarousel = null;
        }
        
        binding = null;
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
        
        // Set up fullscreen toggle functionality
        binding.fullscreenToggleIcon.setOnClickListener(v -> {
            toggleFullscreenMode(true);
        });
        
        // Set up exit fullscreen button
        binding.exitFullscreenButton.setOnClickListener(v -> {
            toggleFullscreenMode(false);
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
        setupChips();
        
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

        // Initialize sponsored ad view at the bottom of home screen
        sponsoredAdCarousel = binding.sponsoredAdCarousel;
        if (sponsoredAdCarousel != null) {
            // Use "category_below" location instead of "home_bottom" to match server ad
<<<<<<< HEAD
            sponsoredAdView.initialize("category_below", getViewLifecycleOwner(), requireActivity());
            // Enable ad rotation with 3-minute interval for better user experience
            sponsoredAdView.enableRotation(true);
            sponsoredAdView.setRotationIntervalMinutes(3);
            Log.d(TAG, "Initialized sponsored ad view with rotation enabled");
            
            // DEBUGGING: Watch for these logs to track ad rotation:
            // - "LocalRotationManager": Shows rotation timing and ad selection
            // - "SponsoredAdView": Shows UI updates and animations
            // - "SponsoredAdRepository": Shows network and cache operations
            // Filter LogCat with: "Rotat|LocalRotation|SponsoredAd"
=======
            sponsoredAdCarousel.initialize("category_below", getViewLifecycleOwner(), requireActivity());
            Log.d(TAG, "Initialized sponsored ad carousel with location: category_below");
            
            // Observe the selected category to refresh targeted ads when category changes
            viewModel.getSortOption().observe(getViewLifecycleOwner(), sortOption -> {
                refreshSponsoredAdForCurrentCategory();
            });
            
            // Also refresh ads when time filter changes as user may be looking for specific content
            viewModel.getTimeFilter().observe(getViewLifecycleOwner(), timeFilter -> {
                refreshSponsoredAdForCurrentCategory();
            });
        } else {
            Log.e(TAG, "Failed to find sponsored ad carousel view");
>>>>>>> c9d6bc131c97ff1e271900b9a0cfd19fd38917f4
        }
    }

    private void setupCategoriesAdapter() {
        // Initialize the categories adapter with loading state
        categoriesAdapter = new CategoriesAdapter(requireContext());
        
        // Set up RecyclerView with horizontal layout
        binding.categoriesRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.categoriesRecyclerView.setAdapter(categoriesAdapter);
        
        // Initialize with just the "All" category with proper icon
        List<Category> initialCategories = new ArrayList<>();
        Category allCategory = createCategory(null, "All", null);
        initialCategories.add(allCategory);
        categoriesAdapter.updateCategories(initialCategories);
        
        // Set up click listeners
        categoriesAdapter.setOnCategoryClickListener((category, position) -> {
            String categoryId = category.getId();
            String categoryName = category.getName();
            
            // Reset pagination-related flags when changing categories
            hasShownEndMessage = false;
            viewModel.setPaginationInProgress(false);
            lastPaginationCheck = 0;
            
            // Preserve current categories before changing the selection
            List<Category> currentCategories = categoriesAdapter.getVisibleCategories();
            
            if ("All".equals(categoryName) || categoryId == null) {
                viewModel.setCategory(null);
                
                // Track "All" category click
                AnalyticsUtils.trackCategoryClick("All");
            } else {
                viewModel.setCategory(categoryName);
                
                // Track category click in UserRepository
                UserRepository.getInstance(requireContext()).trackCategoryClick(categoryName);
                
                // Also track in Analytics
                AnalyticsUtils.trackCategoryClick(categoryName);
                
                // Show loading Snackbar when selecting a category
                showCategoryLoadingSnackbar(categoryName);
            }
            
            // Update selected category without changing the category list
            categoriesAdapter.updateSelectedCategory(categoryId);
            
            // Refresh sponsored ad when category changes
            refreshSponsoredAdForCurrentCategory();
        });

        categoriesAdapter.setOnMoreClickListener(this::showCategoriesBottomSheet);
        
        // Make sure to initialize the category icon repository if needed
        if (categoryIconRepository != null && !categoryIconRepository.isInitialized()) {
            Log.d(TAG, "Initializing CategoryIconRepository in setupCategoriesAdapter");
            categoryIconRepository.refreshCategoryIcons();
        }
        
        // Observe categories from the ViewModel
        viewModel.getCategories().observe(getViewLifecycleOwner(), categoriesMap -> {
            if (categoriesMap != null && !categoriesMap.isEmpty()) {
                Log.d(TAG, "Received " + categoriesMap.size() + " categories");
                
                // Convert categories map to list for the adapter
                List<String> categoryList = new ArrayList<>(categoriesMap.keySet());
                
                // Convert string list to Category list
                List<Category> categoryObjectList = new ArrayList<>();
                
                // Add "All" category first
                categoryObjectList.add(createCategory(null, "All", null));
                
                // Add other categories
                for (String name : categoryList) {
                    if (!"All".equalsIgnoreCase(name)) {
                        String normalizedName = normalizeCategory(name);
                        // Try to get the complete CategoryIcon object first
                        CategoryIcon categoryIcon = null;
                        if (categoryIconRepository != null) {
                            categoryIcon = categoryIconRepository.getCategoryIconByCategory(normalizedName);
                        }
                        
                        Category category;
                        if (categoryIcon != null) {
                            // Use the full CategoryIcon object if available
                            category = new Category(normalizedName, name, categoryIcon);
                            Log.d(TAG, "Using full CategoryIcon object for " + name);
                        } else {
                            // Fall back to getting just the URL
                            String iconUrl = categoryIconRepository != null ? 
                                categoryIconRepository.getCategoryIconUrl(normalizedName) : null;
                            category = createCategory(normalizedName, name, iconUrl);
                        }
                        
                        // Set template count
                        if (categoriesMap.containsKey(name)) {
                            category.setTemplateCount(categoriesMap.get(name));
                        }
                        categoryObjectList.add(category);
                    }
                }

                // Add More category explicitly if there are more than 3 categories
                if (categoryObjectList.size() > 3) {
                    Category moreCategory = createCategory("more", "More", null);
                    moreCategory.setDisplayOrder(1000); // High number to ensure it's last
                    categoryObjectList.add(moreCategory);
                }

                // Prevent categoriesAdapter changes during update to avoid UI flickering
                if (categoriesAdapter != null) {
                    // Get currently selected category before updating
                    String currentSelection = null;
                    int currentSelectedPosition = categoriesAdapter.getSelectedPosition();
                    if (currentSelectedPosition >= 0) {
                        List<Category> visibleCategories = categoriesAdapter.getVisibleCategories();
                        if (currentSelectedPosition < visibleCategories.size()) {
                            Category selectedCategory = visibleCategories.get(currentSelectedPosition);
                            if (selectedCategory != null) {
                                currentSelection = selectedCategory.getId();
                            }
                        }
                    }
                    
                    // If no current selection, use the one from ViewModel
                    if (currentSelection == null) {
                        currentSelection = viewModel.getSelectedCategory();
                    }
                    
                    // Now update the adapter with the changes safely
                    categoriesAdapter.preventCategoryChanges(true);
                    try {
                        // Update adapter with category objects
                        categoriesAdapter.updateCategories(categoryObjectList);
                        
                        // Restore selection
                        categoriesAdapter.updateSelectedCategory(currentSelection);
                    } finally {
                        categoriesAdapter.preventCategoryChanges(false);
                    }
                }
            } else {
                // Load categories if none are available
                viewModel.loadCategories();
            }
        });
        
        // Also observe category icons from the repository
        if (categoryIconRepository != null) {
            categoryIconRepository.getCategoryIcons().observe(getViewLifecycleOwner(), icons -> {
                if (icons != null && !icons.isEmpty()) {
                    Log.d(TAG, "Received " + icons.size() + " category icons from repository");
                    // When icons change, refresh the categories
                    ensureCategoriesVisible();
                }
            });
        }
    }

    private void showCategoriesBottomSheet(List<Category> remainingCategories) {
        // Use BottomSheetDialog for better appearance and behavior
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_categories, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Get views
        RecyclerView categoriesRecyclerView = bottomSheetView.findViewById(R.id.categoriesRecyclerView);
        TextView titleView = bottomSheetView.findViewById(R.id.title);
        TextView subtitleView = bottomSheetView.findViewById(R.id.subtitle);

        // Set the title and subtitle with template counts
        int totalTemplates = 0;
        for (Category category : remainingCategories) {
            totalTemplates += category.getTemplateCount();
        }
        titleView.setText("All Categories");
        subtitleView.setText(String.format("Browse %d categories with %d templates", 
                remainingCategories.size(), totalTemplates));

        // Set up the grid for categories
        BottomSheetCategoriesAdapter bottomSheetAdapter = new BottomSheetCategoriesAdapter(requireContext());
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 2);
        categoriesRecyclerView.setLayoutManager(gridLayoutManager);
        categoriesRecyclerView.setAdapter(bottomSheetAdapter);
        
        // Apply item decoration for spacing
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        categoriesRecyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacingInPixels, true));
        
        // Set the current selected category
        String selectedCategory = viewModel.getSelectedCategory();
        bottomSheetAdapter.setSelectedCategory(selectedCategory);
        
        // Update with categories
        bottomSheetAdapter.updateCategories(remainingCategories);

        // Set click listener
        bottomSheetAdapter.setOnCategoryClickListener((category, position) -> {
            String categoryId = category.getId();
            String categoryName = category.getName();
            
            if ("All".equals(categoryName) || categoryId == null) {
                viewModel.setCategory(null);
                
                // Track "All" category click
                AnalyticsUtils.trackCategoryClick("All");
            } else {
                viewModel.setCategory(categoryName);
                
                // Track category click in UserRepository
                UserRepository.getInstance(requireContext()).trackCategoryClick(categoryName);
                
                // Also track in Analytics
                AnalyticsUtils.trackCategoryClick(categoryName);
                
                // Show loading indicator for selected category
                showCategoryLoadingSnackbar(categoryName);
            }
            
            // Update the main adapter with the new selection
            categoriesAdapter.updateSelectedCategory(categoryId);
            
            // Dismiss the bottom sheet with a slight delay for better UX
            new Handler().postDelayed(bottomSheetDialog::dismiss, 150);
        });

        // Set behavior for expanded state
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        
        // Show the sheet
        bottomSheetDialog.show();
    }

    private void setupChips() {
        binding.chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            SortOption selectedSortOption;
            if (checkedId == R.id.chipTrending) {
                selectedSortOption = SortOption.TRENDING;
            } else if (checkedId == R.id.chipNewest) {
                selectedSortOption = SortOption.NEWEST;
            } else if (checkedId == R.id.chipOldest) {
                selectedSortOption = SortOption.OLDEST;
            } else {
                selectedSortOption = SortOption.MOST_USED;
            }
            viewModel.setSortOption(selectedSortOption);
        });

        binding.chipGroupTimeFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            TimeFilter selectedTimeFilter;
            if (checkedId == R.id.chipAllTime) {
                selectedTimeFilter = TimeFilter.ALL;
            } else if (checkedId == R.id.chipToday) {
                selectedTimeFilter = TimeFilter.TODAY;
            } else if (checkedId == R.id.chipThisWeek) {
                selectedTimeFilter = TimeFilter.THIS_WEEK;
            } else if (checkedId == R.id.chipThisMonth) {
                selectedTimeFilter = TimeFilter.THIS_MONTH;
            } else {
                selectedTimeFilter = TimeFilter.THIS_YEAR;
            }
            viewModel.setTimeFilter(selectedTimeFilter);
        });
    }

    /**
     * Update chip selections based on current sort and time filter
     */
    private void updateChipSelections() {
        if (binding == null) return;
        
        // Get current states
        SortOption currentSort = viewModel.getCurrentSortOption();
        TimeFilter currentTimeFilter = viewModel.getCurrentTimeFilter();
        
        // Update sort chips
        binding.chipTrending.setChecked(currentSort == SortOption.TRENDING);
        binding.chipNewest.setChecked(currentSort == SortOption.NEWEST);
        binding.chipOldest.setChecked(currentSort == SortOption.OLDEST);
        binding.chipMostUsed.setChecked(currentSort == SortOption.MOST_USED);
        
        // Update time filter chips
        binding.chipAllTime.setChecked(currentTimeFilter == TimeFilter.ALL);
        binding.chipToday.setChecked(currentTimeFilter == TimeFilter.TODAY);
        binding.chipThisWeek.setChecked(currentTimeFilter == TimeFilter.THIS_WEEK);
        binding.chipThisMonth.setChecked(currentTimeFilter == TimeFilter.THIS_MONTH);
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
        SortOption currentSortOption = viewModel.getCurrentSortOption();
        TimeFilter currentTimeFilter = viewModel.getCurrentTimeFilter();
        
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
            case ALL:
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
            SortOption selectedSortOption;
            if (radioTrending.isChecked()) {
                selectedSortOption = SortOption.TRENDING;
            } else if (radioNewest.isChecked()) {
                selectedSortOption = SortOption.NEWEST;
            } else if (radioOldest.isChecked()) {
                selectedSortOption = SortOption.OLDEST;
            } else {
                selectedSortOption = SortOption.MOST_USED;
            }
            
            // Determine which time filter is selected
            TimeFilter selectedTimeFilter;
            if (radioAllTime.isChecked()) {
                selectedTimeFilter = TimeFilter.ALL;
            } else if (radioToday.isChecked()) {
                selectedTimeFilter = TimeFilter.TODAY;
            } else if (radioThisWeek.isChecked()) {
                selectedTimeFilter = TimeFilter.THIS_WEEK;
            } else {
                selectedTimeFilter = TimeFilter.THIS_MONTH;
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
            viewModel.setSortOption(SortOption.TRENDING);
            viewModel.setTimeFilter(TimeFilter.ALL);
            viewModel.setCategory(null);
            
            // Update UI
            updateChipSelections();
            
            // Hide the bottom sheet
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });
        
        // Show the bottom sheet
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
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
                
                // Only check for more items when scrolling stops (IDLE state)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();
                    
                    // Only trigger pagination when:
                    // 1. We're at the very end of the list (last 2 items)
                    // 2. Not currently loading
                    // 3. We haven't checked for more items recently
                    long currentTime = System.currentTimeMillis();
                    if (lastVisibleItem >= 0 && 
                        totalItemCount > 0 &&
                        lastVisibleItem >= totalItemCount - 2 && // Stricter threshold (last 2 items)
                        !viewModel.isPaginationInProgress() &&
                        currentTime - lastPaginationCheck > PAGINATION_CHECK_INTERVAL) {
                        
                        // Update the timestamp
                        lastPaginationCheck = currentTime;
                        
                        // Call loadMoreItems
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
            
            int lastPosition = layoutManager.findLastCompletelyVisibleItemPosition();
            int totalItems = layoutManager.getItemCount();
            
            // Check if we're near the end of the list AND not just regular scrolling
            if (lastPosition >= 0 && totalItems > 0 && 
                lastPosition >= totalItems - VISIBLE_THRESHOLD) {
                
                // Check if we have more pages to load
                if (viewModel.hasMorePagesToLoad()) {
                    // Reset the end message flag since we're loading more
                    hasShownEndMessage = false;
                    
                    // Show loading indicator
                    binding.bottomLoadingView.setVisibility(View.VISIBLE);
                    
                    // Disable SwipeRefreshLayout when loading more items
                    binding.swipeRefreshLayout.setEnabled(false);
                    
                    // Trigger loading next page WITHOUT showing a snackbar
                    viewModel.loadMoreIfNeeded(lastPosition, totalItems);
                } else if (!hasShownEndMessage) {
                    // We're at the end and haven't shown the message yet
                    hasShownEndMessage = true; // Set flag so we don't show it again
                    
                    // Store the timestamp of when we showed the message
                    viewModel.setLastEndMessageTime(System.currentTimeMillis());
                    
                    if (getActivity() != null) {
                        // Only show the message if we haven't shown it recently
                        if (viewModel.canShowEndMessage()) {
                            Toast.makeText(requireContext(), 
                                "No more templates available", 
                                Toast.LENGTH_SHORT).show();
                            
                            // Log the toast event for debugging
                            Log.d(TAG, "Displayed 'No more templates' toast message");
                        } else {
                            Log.d(TAG, "Suppressed 'No more templates' toast - shown too recently");
                        }
                    }
                    
                    // Make sure loading indicator is hidden
                    binding.bottomLoadingView.setVisibility(View.GONE);
                    
                    // Re-enable SwipeRefreshLayout
                    binding.swipeRefreshLayout.setEnabled(true);
                } else {
                    // We've already shown the message, just make sure UI is in correct state
                    binding.bottomLoadingView.setVisibility(View.GONE);
                    binding.swipeRefreshLayout.setEnabled(true);
                }
            } else {
                // Not near the end, ensure UI is reset
                binding.bottomLoadingView.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setEnabled(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading more items", e);
            binding.bottomLoadingView.setVisibility(View.GONE);
            binding.swipeRefreshLayout.setEnabled(true);
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
            
            // Reset the end message flag since we're refreshing data
            hasShownEndMessage = false;
            
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
        // Observe categories from the ViewModel immediately
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
                    List<Category> visibleCategories = categoriesAdapter.getVisibleCategories();
                    if (currentSelectedPosition >= 0 && currentSelectedPosition < visibleCategories.size()) {
                        currentlySelectedCategory = visibleCategories.get(currentSelectedPosition).getId();
                        Log.d(TAG, "Current selected category before update: " + currentlySelectedCategory);
                    }
                }
                
                // Make sure category icons are loaded
                categoryIconRepository.loadCategoryIcons();
                
                // Convert categories map to list for the adapter
                List<String> categoryNames = new ArrayList<>(categories.keySet());
                
                // Add "All" category if it doesn't exist
                if (!categoryNames.contains("All")) {
                    categoryNames.add(0, "All");
                }
                
                // Convert to Category objects for the adapter
                List<Category> categoryList = new ArrayList<>();
                for (String name : categoryNames) {
                    Category category = createCategory(
                            "All".equals(name) ? null : name.toLowerCase(), 
                            name, 
                            null);
                    
                    // Add count if available
                    if (categories.containsKey(name)) {
                        category.setTemplateCount(categories.get(name));
                    }
                    
                    categoryList.add(category);
                }
                
                // Prevent categoriesAdapter changes during update to avoid UI flickering
                if (categoriesAdapter != null) {
                    categoriesAdapter.preventCategoryChanges(true);
                    try {
                        // Update adapter with categories
                        categoriesAdapter.updateCategories(categoryList);
                        
                        // Restore selection if needed
                        if (currentlySelectedCategory != null) {
                            categoriesAdapter.updateSelectedCategory(currentlySelectedCategory);
                        } else {
                            // Default to "All" category if no selection
                            categoriesAdapter.updateSelectedCategory(null);
                        }
                    } finally {
                        categoriesAdapter.preventCategoryChanges(false);
                    }
                }
                
                // Make categories visible
                binding.categoriesRecyclerView.setVisibility(View.VISIBLE);
            }
        });
        
        // Trigger category load immediately
        viewModel.forceReloadCategories();
        
        // Observe category icons
        categoryIconRepository.getCategoryIcons().observe(getViewLifecycleOwner(), categoryIcons -> {
            Log.d(TAG, "Received " + (categoryIcons != null ? categoryIcons.size() : 0) + " category icons");
            // Force refresh of categories adapter if we have categories
            if (categoriesAdapter != null && categoriesAdapter.getItemCount() > 0) {
                categoriesAdapter.notifyDataSetChanged();
            }
        });
        
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
                int count = newIds.size();
                Log.d(TAG, "New template IDs updated: " + count + (count > 0 ? ", First ID: " + newIds.iterator().next() : ""));
                adapter.setNewTemplateIds(newIds);
                
                // Force a refresh of all visible items to ensure badges are displayed
                if (count > 0 && layoutManager != null) {
                    int firstVisible = layoutManager.findFirstVisibleItemPosition();
                    int lastVisible = layoutManager.findLastVisibleItemPosition();
                    if (firstVisible >= 0 && lastVisible >= 0) {
                        adapter.notifyItemRangeChanged(firstVisible, lastVisible - firstVisible + 1);
                    }
                }
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
                String userMessage;
                if (error.contains("504")) {
                    userMessage = "Server timeout. Please check your internet connection and try again.";
                } else if (error.contains("404")) {
                    userMessage = "Content not found. Please refresh and try again.";
                } else if (error.contains("403")) {
                    userMessage = "Access denied. Please check your permissions.";
                } else if (error.contains("500")) {
                    userMessage = "Server error. Please try again later.";
                } else if (error.toLowerCase().contains("timeout") || error.toLowerCase().contains("failed to connect")) {
                    userMessage = "Connection timeout. Please check your internet and try again.";
                } else {
                    userMessage = error;
                }
                
                // Show error in retry layout
                binding.retryLayout.setVisibility(View.VISIBLE);
                binding.templatesRecyclerView.setVisibility(View.GONE);
                TextView errorText = binding.retryLayout.findViewById(R.id.errorText);
                if (errorText != null) {
                    errorText.setText(userMessage);
                }
                
                // Setup retry button
                Button retryButton = binding.retryLayout.findViewById(R.id.retryButton);
                if (retryButton != null) {
                    retryButton.setOnClickListener(v -> {
                        // Clear the error state immediately to prevent it from showing again
                        viewModel.clearErrorState();
                        
                        // Hide retry layout and show templates
                        binding.retryLayout.setVisibility(View.GONE);
                        binding.templatesRecyclerView.setVisibility(View.VISIBLE);
                        
                        // Force reload templates
                        viewModel.loadTemplates(true);
                    });
                }
                
                // Also show a toast for immediate feedback
                Toast.makeText(requireContext(), userMessage, Toast.LENGTH_SHORT).show();
            } else {
                // Hide retry layout if there's no error
                binding.retryLayout.setVisibility(View.GONE);
                binding.templatesRecyclerView.setVisibility(View.VISIBLE);
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


    @Override
    public void onTemplateClick(Template template) {
        if (template == null) {
            Log.e(TAG, "Null template clicked");
            return;
        }
        
        Log.d(TAG, "Template clicked: " + template.getTitle());
        
        // Track template view with enhanced method
        AnalyticsUtils.trackTemplateView(
            template.getId(),
            template.getTitle(),
            template.getCategory()
        );
        
        // Track category click if template has a category
        if (template.getCategory() != null) {
            UserRepository.getInstance(requireContext()).trackCategoryClick(template.getCategory());
        }
        
        // Increment template click counter
        templateClickCount++;
        
        // Decide whether to show ad based on click counter
        if (templateClickCount >= AD_SHOW_THRESHOLD) {
            // Show ad if available, otherwise navigate directly
            showAdAndNavigate(template);
            // Reset counter after showing ad
            templateClickCount = 0;
        } else {
            // Navigate directly without showing ad
            navigateToTemplateDetail(template.getId());
        }
    }
    
    /**
     * Navigate to template detail screen
     * @param templateId Template ID to navigate to
     */
    private void navigateToTemplateDetail(String templateId) {
        if (!isAdded()) return;
        
        // Navigate directly since ads are disabled
        Bundle args = new Bundle();
        args.putString("templateId", templateId);
        Navigation.findNavController(requireView()).navigate(R.id.action_home_to_template_detail, args);
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
        if (binding.categoriesRecyclerView.getAdapter() == null) {
            Log.d(TAG, "Categories RecyclerView adapter is null, setting adapter");
            binding.categoriesRecyclerView.setAdapter(categoriesAdapter);
        }
        
        if (categoriesAdapter != null) {
            // Show loading state while checking for data
            categoriesAdapter.setLoading(true);
            
            // First refresh category icons, especially if we haven't loaded them yet
            if (categoryIconRepository != null) {
                // Force refresh of icons to ensure they're available
                if (!categoryIconRepository.isInitialized()) {
                    Log.d(TAG, "Refreshing category icons in ensureCategoriesVisible");
                    categoryIconRepository.refreshCategoryIcons();
                }
            }
            
            Map<String, Integer> categoriesMap = viewModel.getCategories().getValue();
            if (categoriesMap != null && !categoriesMap.isEmpty()) {
                // Get currently selected category before updating
                String currentSelection = null;
                int currentSelectedPosition = categoriesAdapter.getSelectedPosition();
                if (currentSelectedPosition >= 0) {
                    List<Category> visibleCategories = categoriesAdapter.getVisibleCategories();
                    if (currentSelectedPosition < visibleCategories.size()) {
                        Category selectedCategory = visibleCategories.get(currentSelectedPosition);
                        if (selectedCategory != null) {
                            currentSelection = selectedCategory.getId();
                        }
                    }
                }
                
                // If no current selection, use the one from ViewModel
                if (currentSelection == null) {
                    currentSelection = viewModel.getSelectedCategory();
                }
                
                categoriesAdapter.setLoading(false);
                
                // Create a list of Category objects
                List<Category> categoryObjectList = new ArrayList<>();
                // Add "All" category first
                categoryObjectList.add(createCategory(null, "All", null));
                
                // Convert map entries to Category objects
                for (Map.Entry<String, Integer> entry : categoriesMap.entrySet()) {
                    String name = entry.getKey();
                    if (!"All".equalsIgnoreCase(name)) {
                        String normalizedName = normalizeCategory(name);
                        // Try to get the complete CategoryIcon object first
                        CategoryIcon categoryIcon = null;
                        if (categoryIconRepository != null) {
                            categoryIcon = categoryIconRepository.getCategoryIconByCategory(normalizedName);
                        }
                        
                        Category category;
                        if (categoryIcon != null) {
                            // Use the full CategoryIcon object if available
                            category = new Category(normalizedName, name, categoryIcon);
                            Log.d(TAG, "Using full CategoryIcon object for " + name);
                        } else {
                            // Fall back to getting just the URL
                            String iconUrl = categoryIconRepository != null ? 
                                categoryIconRepository.getCategoryIconUrl(normalizedName) : null;
                            category = createCategory(normalizedName, name, iconUrl);
                        }
                        
                        // Set template count
                        if (categoriesMap.containsKey(name)) {
                            category.setTemplateCount(categoriesMap.get(name));
                        }
                        categoryObjectList.add(category);
                    }
                }
                
                // Add More category explicitly if there are more than 3 categories
                if (categoryObjectList.size() > 3) {
                    Category moreCategory = createCategory("more", "More", null);
                    moreCategory.setDisplayOrder(1000); // High number to ensure it's last
                    categoryObjectList.add(moreCategory);
                }

                // Prevent categoriesAdapter changes during update to avoid UI flickering
                categoriesAdapter.preventCategoryChanges(true);
                try {
                    // Update adapter with category objects
                    categoriesAdapter.updateCategories(categoryObjectList);
                    
                    // Apply selected category without triggering another update
                    categoriesAdapter.updateSelectedCategory(currentSelection);
                } finally {
                    categoriesAdapter.preventCategoryChanges(false);
                }
                
                // Show the categories recycler view
                binding.categoriesRecyclerView.setVisibility(View.VISIBLE);
            } else {
                // Load categories if none are available
                viewModel.loadCategories();
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

    private void toggleFullscreenMode(boolean isFullscreen) {
        if (isFullscreen) {
            // Hide AppBarLayout (header section)
            binding.appBarLayout.setVisibility(View.GONE);
            
            // Update AppBarLayout parameters to not take up space
            CoordinatorLayout.LayoutParams appBarLayoutParams = (CoordinatorLayout.LayoutParams) binding.appBarLayout.getLayoutParams();
            appBarLayoutParams.height = 0;
            binding.appBarLayout.setLayoutParams(appBarLayoutParams);
            
            // Make sure behavior is not affecting scroll
            appBarLayoutParams.setBehavior(null);
            
            // Hide bottom navigation
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
            
            // Hide AdBannerView
//            binding.adBannerView.setVisibility(View.GONE);
            
            // Show exit fullscreen button
            binding.exitFullscreenButton.setVisibility(View.VISIBLE);
            
            // Adjust recycler view to use full screen space
            ViewGroup.MarginLayoutParams layoutParams = (MarginLayoutParams) binding.swipeRefreshLayout.getLayoutParams();
            layoutParams.topMargin = 0;
            layoutParams.bottomMargin = 0;
            binding.swipeRefreshLayout.setLayoutParams(layoutParams);
            
            // Remove the appbar scrolling behavior from SwipeRefreshLayout
            CoordinatorLayout.LayoutParams swipeParams = (CoordinatorLayout.LayoutParams) binding.swipeRefreshLayout.getLayoutParams();
            swipeParams.setBehavior(null);
            binding.swipeRefreshLayout.setLayoutParams(swipeParams);
            
            // Update the layout
            binding.getRoot().requestLayout();
            
            // Save the fullscreen state
            viewModel.setFullscreenMode(true);
        } else {
            // Restore AppBarLayout (header section)
            binding.appBarLayout.setVisibility(View.VISIBLE);
            
            // Restore AppBarLayout parameters
            CoordinatorLayout.LayoutParams appBarLayoutParams = (CoordinatorLayout.LayoutParams) binding.appBarLayout.getLayoutParams();
            appBarLayoutParams.height = CoordinatorLayout.LayoutParams.WRAP_CONTENT;
            binding.appBarLayout.setLayoutParams(appBarLayoutParams);
            
            // Restore behavior
            AppBarLayout.Behavior behavior = new AppBarLayout.Behavior();
            behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
                @Override
                public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                    return true;
                }
            });
            appBarLayoutParams.setBehavior(behavior);
            
            // Show bottom navigation
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
            }

            
            // Hide exit fullscreen button
            binding.exitFullscreenButton.setVisibility(View.GONE);
            
            // Reset recycler view margins
            ViewGroup.MarginLayoutParams layoutParams = (MarginLayoutParams) binding.swipeRefreshLayout.getLayoutParams();
            layoutParams.topMargin = getResources().getDimensionPixelSize(R.dimen.recycler_view_top_margin);
            layoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.recycler_view_bottom_margin);
            binding.swipeRefreshLayout.setLayoutParams(layoutParams);
            
            // Restore the appbar scrolling behavior to SwipeRefreshLayout
            CoordinatorLayout.LayoutParams swipeParams = (CoordinatorLayout.LayoutParams) binding.swipeRefreshLayout.getLayoutParams();
            swipeParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
            binding.swipeRefreshLayout.setLayoutParams(swipeParams);
            
            // Update the layout
            binding.getRoot().requestLayout();
            
            // Save the fullscreen state
            viewModel.setFullscreenMode(false);
        }
    }

    /**
     * Helper method to create a Category object with proper icon handling
     */
    private Category createCategory(String id, String name, String iconUrl) {
        // Normalize category name for consistent lookup
        String normalizedName = normalizeCategory(name);
        
        // Get icon from repository if not provided
        if (iconUrl == null && categoryIconRepository != null) {
            // Try to get the full CategoryIcon object first
            CategoryIcon icon = categoryIconRepository.getCategoryIconByCategory(normalizedName);
            if (icon != null && icon.getCategoryIcon() != null && !icon.getCategoryIcon().isEmpty()) {
                Log.d(TAG, "Found CategoryIcon object for " + normalizedName + ": " + icon.getCategoryIcon());
                return new Category(id, name, icon);
            }
            
            // Fallback to just getting the URL
            iconUrl = categoryIconRepository.getCategoryIconUrl(normalizedName);
            Log.d(TAG, "Fetched icon URL for " + normalizedName + ": " + (iconUrl != null ? iconUrl : "null"));
        }
        
        CategoryIcon icon = null;
        if (iconUrl != null && !iconUrl.isEmpty()) {
            icon = new CategoryIcon(id, normalizedName, iconUrl);
            Log.d(TAG, "Created new CategoryIcon for " + normalizedName + " with URL: " + iconUrl);
        } else {
            Log.w(TAG, "No icon URL found for category: " + normalizedName);
        }
        
        return new Category(id, name, icon);
    }

    /**
     * Normalize a category name for consistent lookup
     */
    private String normalizeCategory(String category) {
        if (category == null) return "";
        return category.toLowerCase().trim();
    }

    private void showCategoryLoadingSnackbar(String category) {
        if (category == null || category.isEmpty()) {
            return;
        }
        
        try {
            Snackbar snackbar = Snackbar.make(
                binding.getRoot(),
                "Loading " + category + " templates...",
                Snackbar.LENGTH_SHORT
            );
            
            // Store in ViewModel to allow dismissal if needed
            viewModel.setCurrentSnackbar(snackbar);
            
            // Show the snackbar
            snackbar.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing snackbar: " + e.getMessage());
        }
    }

    /**
     * Grid spacing decoration for the category grid
     */
    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacing;
        private final boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, 
                                  @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;

                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }

    private void showAdAndNavigate(Template template) {
        if (interstitialAdManager != null) {
            if (!interstitialAdManager.isAdLoaded() && !isAdLoading) {
                isAdLoading = true;
                pendingTemplate = template;
                interstitialAdManager.loadAd(new InterstitialAdManager.InterstitialAdCallback() {
                    @Override
                    public void onAdLoaded() {
                        isAdLoading = false;
                        if (pendingTemplate != null) {
                            showAdAndNavigate(pendingTemplate);
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(String error) {
                        isAdLoading = false;
                        // Navigate directly if ad fails to load
                        if (pendingTemplate != null) {
                            navigateToTemplateDetail(pendingTemplate.getId());
                            pendingTemplate = null;
                        }
                    }

                    @Override
                    public void onAdShown() {
                        // Ad is being shown
                        Log.d(TAG, "Interstitial ad shown");
                    }

                    @Override
                    public void onAdDismissed() {
                        // Navigate after ad is dismissed
                        if (pendingTemplate != null) {
                            navigateToTemplateDetail(pendingTemplate.getId());
                            pendingTemplate = null;
                            // Preload next ad
                            loadNextAd();
                        }
                    }

                    @Override
                    public void onAdClicked() {
                        // Ad was clicked
                        Log.d(TAG, "Interstitial ad clicked");
                    }

                    @Override
                    public void onAdShowFailed(String error) {
                        // Navigate directly if ad fails to show
                        if (pendingTemplate != null) {
                            navigateToTemplateDetail(pendingTemplate.getId());
                            pendingTemplate = null;
                        }
                    }
                });
            } else if (interstitialAdManager.isAdLoaded()) {
                pendingTemplate = template;
                interstitialAdManager.showAd(requireActivity(), new InterstitialAdManager.InterstitialAdCallback() {
                    @Override
                    public void onAdLoaded() {}

                    @Override
                    public void onAdFailedToLoad(String error) {
                        // Navigate directly if ad fails to load
                        if (pendingTemplate != null) {
                            navigateToTemplateDetail(pendingTemplate.getId());
                            pendingTemplate = null;
                        }
                    }

                    @Override
                    public void onAdShown() {
                        Log.d(TAG, "Interstitial ad shown");
                    }

                    @Override
                    public void onAdDismissed() {
                        // Navigate after ad is dismissed
                        if (pendingTemplate != null) {
                            navigateToTemplateDetail(pendingTemplate.getId());
                            pendingTemplate = null;
                            // Preload next ad
                            loadNextAd();
                        }
                    }

                    @Override
                    public void onAdClicked() {
                        Log.d(TAG, "Interstitial ad clicked");
                    }

                    @Override
                    public void onAdShowFailed(String error) {
                        // Navigate directly if ad fails to show
                        if (pendingTemplate != null) {
                            navigateToTemplateDetail(pendingTemplate.getId());
                            pendingTemplate = null;
                        }
                    }
                });
            } else {
                // Navigate directly if ad is not ready
                navigateToTemplateDetail(template.getId());
            }
        } else {
            // Navigate directly if ad manager is not available
            navigateToTemplateDetail(template.getId());
        }
    }

    private void loadNextAd() {
        if (interstitialAdManager != null && !isAdLoading) {
            isAdLoading = true;
            interstitialAdManager.loadAd(new InterstitialAdManager.InterstitialAdCallback() {
                @Override
                public void onAdLoaded() {
                    isAdLoading = false;
                }

                @Override
                public void onAdFailedToLoad(String error) {
                    isAdLoading = false;
                }

                @Override
                public void onAdShown() {}

                @Override
                public void onAdDismissed() {}

                @Override
                public void onAdClicked() {}

                @Override
                public void onAdShowFailed(String error) {}
            });
        }
    }

<<<<<<< HEAD
    @Override
    public void onPause() {
        super.onPause();
        
        // Explicitly mark that this fragment is in background
        wasInBackground = true;
        isResumed = false;
        
        // Save scroll position
        lastScrollPosition = getScrollPosition();
        if (viewModel != null) {
            viewModel.setLastVisiblePosition(lastScrollPosition);
        }
        
        // Pause sponsored ad rotation when the fragment is not visible
        if (sponsoredAdView != null) {
            try {
                // Properly handle sponsored ad pause
                sponsoredAdView.handlePause();
                Log.d(TAG, "Called handlePause on sponsoredAdView");
            } catch (Exception e) {
                Log.e(TAG, "Error handling sponsored ad in onPause: " + e.getMessage());
            }
        }
=======
    /**
     * Refresh sponsored ad based on the current category
     * This targets ads to the specific category the user is viewing
     */
    private void refreshSponsoredAdForCurrentCategory() {
        if (sponsoredAdCarousel == null) return;
        
        String currentCategory = viewModel.getSelectedCategory();
        String adLocation = "category_below"; // Default location
        
        // If we have a specific category, try more targeted location
        if (currentCategory != null && !currentCategory.isEmpty() && !"All".equalsIgnoreCase(currentCategory)) {
            adLocation = "category_" + currentCategory.toLowerCase();
            Log.d(TAG, "Using category-specific ad location: " + adLocation);
        }
        
        // First try category-specific ad location
        sponsoredAdCarousel.initialize(adLocation, getViewLifecycleOwner(), requireActivity());
        
        // Ensure ad is visible after category change
        if (sponsoredAdCarousel.getVisibility() != View.VISIBLE) {
            Log.d(TAG, "Setting sponsored ad view to visible");
            sponsoredAdCarousel.setVisibility(View.VISIBLE);
        }
        
        // Set a delayed check to ensure the carousel is still visible
        new Handler().postDelayed(() -> {
            if (sponsoredAdCarousel != null && sponsoredAdCarousel.getVisibility() != View.VISIBLE) {
                Log.d(TAG, "Reset sponsored ad carousel visibility after delay");
                sponsoredAdCarousel.setVisibility(View.VISIBLE);
            }
        }, 1000); // Check after 1 second
>>>>>>> c9d6bc131c97ff1e271900b9a0cfd19fd38917f4
    }
}


