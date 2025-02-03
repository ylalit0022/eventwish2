package com.ds.eventwish.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentHomeBinding;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.ui.base.BaseFragment;
import com.ds.eventwish.ui.home.adapter.TemplateAdapter;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeFragment extends BaseFragment implements TemplateAdapter.OnTemplateClickListener {
    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private TemplateAdapter adapter;
    private CategoriesAdapter categoriesAdapter;
    private GridLayoutManager layoutManager;
    private static final int VISIBLE_THRESHOLD = 5;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // hide toolbar
        ((AppCompatActivity) requireActivity()).getSupportActionBar().hide();

        setupViewModel();
        setupUI();
        setupRecyclerView();
        setupSearch();
        setupObservers();
        setupSwipeRefresh();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
    }

    private void setupUI() {
        // Setup categories RecyclerView
        categoriesAdapter = new CategoriesAdapter();
        binding.categoriesRecyclerView.setAdapter(categoriesAdapter);
        binding.categoriesRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        categoriesAdapter.setOnCategoryClickListener((category, position) -> {
            if (category.equals("All")) {
                viewModel.setCategory(null);
            } else {
                viewModel.setCategory(category);
            }
            categoriesAdapter.setSelectedPosition(position);
        });
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
            Log.d(TAG, "Swipe refresh triggered");
            binding.bottomLoadingView.setVisibility(View.GONE);
            viewModel.loadTemplates(true);
        });
    }
//old mthod
//    private void setupSearch() {
//        binding.searchView.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {}
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                Log.d(TAG, "Search query changed: " + s.toString());
//                binding.bottomLoadingView.setVisibility(View.GONE);
//                viewModel.setSearchQuery(s.toString());
//            }
//        });
//    }

    private void setupSearch (){
        //SearchView searchView = view.findViewById(R.id.searchView);
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
        viewModel.getTemplates().observe(getViewLifecycleOwner(), templates -> {
            Log.d(TAG, "Templates updated - size: " + (templates != null ? templates.size() : 0));
            if (templates != null) {
                adapter.submitList(new ArrayList<>(templates)); // Create new list to force update
                binding.emptyView.setVisibility(templates.isEmpty() ? View.VISIBLE : View.GONE);
                binding.templatesRecyclerView.setVisibility(templates.isEmpty() ? View.GONE : View.VISIBLE);
            }
            binding.swipeRefreshLayout.setRefreshing(false);
            binding.bottomLoadingView.setVisibility(View.GONE);
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            Log.d(TAG, "Categories updated - size: " + (categories != null ? categories.size() : 0));
            if (categories != null) {
                List<String> categoryList = new ArrayList<>();
                categoryList.add("All"); // Add "All" as the first category
                categoryList.addAll(categories.keySet());
                categoriesAdapter.updateCategories(categoryList);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Log.e(TAG, "Error received: " + error);
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.bottomLoadingView.setVisibility(View.GONE);
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "Loading state changed: " + isLoading);
            if (!isLoading) {
                binding.bottomLoadingView.setVisibility(View.GONE);
            }
        });
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
