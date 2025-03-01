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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.ds.eventwish.ui.festival.FestivalViewModel;

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
        
        Log.d(TAG, "onViewCreated");
        
        // Initialize ViewModel with context for template update manager
        viewModel.init(requireContext());
        festivalViewModel = new ViewModelProvider(this).get(FestivalViewModel.class);
        
        // Remove any references to Action Bar or Toolbar
        // Example of setting a background color
        binding.getRoot().setBackgroundColor(getResources().getColor(R.color.soft_background)); // Use the soft background color
        
        setupUI();
        setupRecyclerView();
        setupImpressionTracking();
        setupSearch();
        setupSwipeRefresh();
        setupObservers();
        setupBottomNavigation();
        
        // Initial load of templates
        viewModel.loadTemplates(false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Handle back press for exit confirmation
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (backPressedTime + BACK_PRESS_DELAY > System.currentTimeMillis()) {
                    requireActivity().finish();
                } else {
                    Toast.makeText(requireContext(), "Press back again to exit", Toast.LENGTH_SHORT).show();
                }
                backPressedTime = System.currentTimeMillis();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        
        Log.d(TAG, "onResume - Last position: " + viewModel.getLastVisiblePosition());
        
        // Check for new templates when the fragment resumes
        if (viewModel != null) {
            List<Template> currentTemplates = viewModel.getTemplates().getValue();
            if (currentTemplates != null && !currentTemplates.isEmpty()) {
                Log.d(TAG, "Checking for new templates on resume");
                viewModel.checkForNewTemplates(currentTemplates);
                
                // Restore scroll position safely
                final int position = viewModel.getLastVisiblePosition();
                if (position > 0 && position < currentTemplates.size()) {
                    // Use post to ensure RecyclerView is ready
                    binding.templatesRecyclerView.post(() -> {
                        Log.d(TAG, "Restoring scroll position in onResume: " + position);
                        layoutManager.scrollToPosition(position);
                        
                        // Load more templates if needed to restore previous state
                        if (viewModel.getCurrentPage() > 1) {
                            Log.d(TAG, "Ensuring all pages are loaded up to: " + viewModel.getCurrentPage());
                            for (int i = 1; i <= viewModel.getCurrentPage(); i++) {
                                viewModel.loadTemplates(false);
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        
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
    }

    private void setupUI() {
        // Setup header icons
        binding.refreshIcon.setOnClickListener(v -> {
            // Disable the refresh icon temporarily to prevent multiple clicks
            binding.refreshIcon.setEnabled(false);
            
            // Clear the new templates indicator
            viewModel.clearNewTemplatesFlag();
            
            // Refresh templates
            viewModel.loadTemplates(true);
            
            // Reset scroll position to avoid inconsistency
            viewModel.saveScrollPosition(0);
            
            // Hide the indicator immediately
            binding.refreshIndicator.setVisibility(View.GONE);
            
            // Re-enable the refresh icon after a delay
            binding.refreshIcon.postDelayed(() -> binding.refreshIcon.setEnabled(true), 1000);
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
        
        binding.notificationIcon.setOnClickListener(v -> {
            // Navigate to notifications or show notification panel
            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show();
            // You can add navigation to a notification screen here
            // NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            // navController.navigate(R.id.action_homeFragment_to_notificationFragment);
            
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

        categoriesAdapter.setOnCategoryClickListener((category, position) -> {
            if (category.equals("All")) {
                viewModel.setCategory(null);
            } else {
                viewModel.setCategory(category);
            }
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
    }

    private void showCategoriesBottomSheet(List<String> categories) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_categories, null);
        RecyclerView categoriesRecyclerView = bottomSheetView.findViewById(R.id.categoriesRecyclerView);
        
        // Add "All" category to the list if not already present
        List<String> allCategories = new ArrayList<>();
        if (!categories.contains("All")) {
            allCategories.add("All");
        }
        allCategories.addAll(categories);
        
        Log.d(TAG, "Bottom sheet showing " + allCategories.size() + " categories");
        for (String category : allCategories) {
            Log.d(TAG, "Bottom sheet category: " + category);
        }
        
        CategoriesAdapter bottomSheetAdapter = new CategoriesAdapter();
        categoriesRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        categoriesRecyclerView.setAdapter(bottomSheetAdapter);
        bottomSheetAdapter.updateCategories(allCategories);

        bottomSheetAdapter.setOnCategoryClickListener((category, position) -> {
            Log.d(TAG, "Bottom sheet category selected: " + category);
            if (category.equals("All")) {
                viewModel.setCategory(null);
            } else {
                viewModel.setCategory(category);
            }
            categoriesAdapter.updateSelectedCategory(category);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void setupRecyclerView() {
        adapter = new TemplateAdapter(this);
        layoutManager = new GridLayoutManager(requireContext(), 2);
        binding.templatesRecyclerView.setLayoutManager(layoutManager);
        binding.templatesRecyclerView.setAdapter(adapter);
        
        // Set item animator to null to prevent animation glitches
        binding.templatesRecyclerView.setItemAnimator(null);

        binding.templatesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                try {
                    if (dy > 0) { // Check if scrolling down
                        int totalItemCount = layoutManager.getItemCount();
                        int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();
    
                        Log.d(TAG, "Scrolling - last visible: " + lastVisibleItem +
                                " - total: " + totalItemCount +
                                " - threshold: " + (totalItemCount - VISIBLE_THRESHOLD));
    
                        // Load more when last visible item is within VISIBLE_THRESHOLD of the end
                        if (lastVisibleItem >= 0 && 
                            totalItemCount > 0 && 
                            lastVisibleItem >= totalItemCount - VISIBLE_THRESHOLD) {
                            loadMoreItems();
                        }
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

    private void setupSearch() {
        binding.searchView.setIconified(false); // Ensure it's always expanded
        binding.searchView.clearFocus(); // Prevents immediate focus on load

        binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.setSearchQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.setSearchQuery(newText);
                return true;
            }
        });
    }

    private void setupObservers() {
        // Loading state observer
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.swipeRefreshLayout.setRefreshing(isLoading);
            // Disable refresh icon during loading
            binding.refreshIcon.setEnabled(!isLoading);
            binding.refreshIcon.setAlpha(isLoading ? 0.5f : 1.0f);
        });
        
        // Templates observer
        viewModel.getTemplates().observe(getViewLifecycleOwner(), templates -> {
            if (templates != null && !templates.isEmpty()) {
                binding.templatesRecyclerView.setVisibility(View.VISIBLE);
                binding.emptyView.setVisibility(View.GONE);
                
                // Use a new list to avoid RecyclerView inconsistency
                List<Template> newList = new ArrayList<>(templates);
                
                // Important: Use post to avoid RecyclerView inconsistency
                binding.templatesRecyclerView.post(() -> {
                    adapter.submitList(newList);
                    
                    // Restore scroll position if returning from template detail
                    // Only after the list has been updated
                    if (viewModel.getLastVisiblePosition() > 0 && 
                        viewModel.getLastVisiblePosition() < newList.size()) {
                        int position = viewModel.getLastVisiblePosition();
                        Log.d(TAG, "Restoring scroll position to: " + position);
                        layoutManager.scrollToPosition(position);
                    }
                });
                
                // Check for new templates
                viewModel.checkForNewTemplates(templates);
            } else {
                binding.templatesRecyclerView.setVisibility(View.GONE);
                binding.emptyView.setVisibility(View.VISIBLE);
            }
            
            binding.swipeRefreshLayout.setRefreshing(false);
            binding.bottomLoadingView.setVisibility(View.GONE);
        });

        // Categories observer
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            Log.d(TAG, "Categories updated - size: " + (categories != null ? categories.size() : 0));
            if (categories != null && !categories.isEmpty()) {
                List<String> categoryList = new ArrayList<>();
                categoryList.add("All"); // Add "All" as the first category
                categoryList.addAll(categories.keySet());
                
                Log.d(TAG, "Category list created with " + categoryList.size() + " items");
                for (String category : categoryList) {
                    Log.d(TAG, "Category in list: " + category);
                }
                
                categoriesAdapter.updateCategories(categoryList);
                
                // Maintain selected position if category is selected
                if (viewModel.getCurrentCategory() != null) {
                    int position = categoryList.indexOf(viewModel.getCurrentCategory());
                    Log.d(TAG, "Selected category: " + viewModel.getCurrentCategory() + " at position: " + position);
                    if (position >= 0) {
                        categoriesAdapter.setSelectedPosition(position);
                    }
                } else {
                    Log.d(TAG, "No category selected, defaulting to All");
                    categoriesAdapter.setSelectedPosition(0); // Select "All" by default
                }
            } else {
                Log.d(TAG, "Categories map is empty or null");
                // If no categories, just show "All"
                List<String> defaultList = new ArrayList<>();
                defaultList.add("All");
                categoriesAdapter.updateCategories(defaultList);
                categoriesAdapter.setSelectedPosition(0);
            }
        });

        // New templates indicator observer
        viewModel.getHasNewTemplates().observe(getViewLifecycleOwner(), hasNew -> {
            binding.refreshIndicator.setVisibility(hasNew ? View.VISIBLE : View.GONE);
            Log.d(TAG, "New templates indicator updated: " + hasNew);
            
            // For debugging - show a toast when the indicator changes
            if (hasNew) {
                Toast.makeText(requireContext(), "New templates available!", Toast.LENGTH_SHORT).show();
            }
        });

        // Error observer
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.templatesRecyclerView.setVisibility(View.VISIBLE);
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG)
                    .setAction("Retry", v -> viewModel.loadTemplates(true))
                    .show();
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
        try {
            // Save the current scroll position before navigating
            int position = layoutManager.findFirstVisibleItemPosition();
            if (position >= 0) {
                viewModel.saveScrollPosition(position);
                Log.d(TAG, "Saving position before navigation: " + position);
            }
            
            // Mark the template as viewed to avoid showing the indicator for it
            if (template.getId() != null) {
                viewModel.markTemplateAsViewed(template.getId());
            }
            
            // Navigate to template detail
            HomeFragmentDirections.ActionHomeToTemplateDetail action = 
                HomeFragmentDirections.actionHomeToTemplateDetail(template.getId());
            Navigation.findNavController(requireView()).navigate(action);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to template detail", e);
            Toast.makeText(requireContext(), "Error opening template", Toast.LENGTH_SHORT).show();
        }
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
}
