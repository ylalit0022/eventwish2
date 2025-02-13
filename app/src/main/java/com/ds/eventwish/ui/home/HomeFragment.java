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

public class HomeFragment extends BaseFragment implements TemplateAdapter.OnTemplateClickListener {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
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
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Remove any references to Action Bar or Toolbar
        // Example of setting a background color
        binding.getRoot().setBackgroundColor(getResources().getColor(R.color.soft_background)); // Use the soft background color

        setupViewModel();
        setupUI();
        setupRecyclerView();
        setupSearch();
        setupObservers();
        setupSwipeRefresh();
        setupBottomNavigation();
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

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
    }

    private void setupUI() {
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
            showCategoriesBottomSheet(remainingCategories);
        });
    }

    private void showCategoriesBottomSheet(List<String> categories) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_categories, null);
        RecyclerView categoriesRecyclerView = bottomSheetView.findViewById(R.id.categoriesRecyclerView);
        
        CategoriesAdapter bottomSheetAdapter = new CategoriesAdapter();
        categoriesRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        categoriesRecyclerView.setAdapter(bottomSheetAdapter);
        bottomSheetAdapter.updateCategories(categories);

        bottomSheetAdapter.setOnCategoryClickListener((category, position) -> {
            viewModel.setCategory(category);
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

        binding.templatesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0) { // Check if scrolling down
                    int totalItemCount = layoutManager.getItemCount();
                    int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();

                    Log.d(TAG, "Scrolling - last visible: " + lastVisibleItem +
                            " - total: " + totalItemCount +
                            " - threshold: " + (totalItemCount - VISIBLE_THRESHOLD));

                    // Load more when last visible item is within VISIBLE_THRESHOLD of the end
                    if (lastVisibleItem >= totalItemCount - VISIBLE_THRESHOLD) {
                        loadMoreItems();
                    }
                }
            }
        });
    }

    private void loadMoreItems() {
        Log.d(TAG, "Loading more items");
        binding.bottomLoadingView.setVisibility(View.VISIBLE);
        viewModel.loadMoreIfNeeded(
                layoutManager.findLastCompletelyVisibleItemPosition(),
                layoutManager.getItemCount()
        );
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.loadTemplates(true);
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
        // Templates observer
        viewModel.getTemplates().observe(getViewLifecycleOwner(), templates -> {
            Log.d(TAG, "Received templates: " + (templates != null ? templates.size() : 0));
            
            if (templates != null) {
                adapter.submitList(new ArrayList<>(templates));
                boolean isEmpty = templates.isEmpty();
                binding.emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                binding.templatesRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }
            
            binding.swipeRefreshLayout.setRefreshing(false);
            binding.bottomLoadingView.setVisibility(View.GONE);
        });

        // Categories observer
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            Log.d(TAG, "Categories updated - size: " + (categories != null ? categories.size() : 0));
            if (categories != null) {
                List<String> categoryList = new ArrayList<>();
                categoryList.add("All"); // Add "All" as the first category
                categoryList.addAll(categories.keySet());
                categoriesAdapter.updateCategories(categoryList);
                
                // Maintain selected position if category is selected
                if (viewModel.getCurrentCategory() != null) {
                    int position = categoryList.indexOf(viewModel.getCurrentCategory());
                    if (position >= 0) {
                        categoriesAdapter.setSelectedPosition(position);
                    }
                }
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
        HomeFragmentDirections.ActionHomeToTemplateDetail action = 
            HomeFragmentDirections.actionHomeToTemplateDetail(template.getId());
        Navigation.findNavController(requireView()).navigate(action);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
