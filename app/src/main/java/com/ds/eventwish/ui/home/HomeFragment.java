package com.ds.eventwish.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentHomeBinding;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.ui.base.BaseFragment;
import com.ds.eventwish.ui.templates.TemplateAdapter;
import com.google.android.material.chip.Chip;
import java.util.Map;

public class HomeFragment extends BaseFragment implements TemplateAdapter.OnTemplateClickListener {
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private TemplateAdapter adapter;
    private boolean isLoadingMore = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        binding.categoriesChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    String category = chip.getText().toString();
                    if (category.equals("All")) {
                        viewModel.setCategory(null);
                    } else {
                        String[] parts = category.split(" \\(");
                        viewModel.setCategory(parts[0]);
                    }
                }
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new TemplateAdapter(this);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        binding.greetingsRecyclerView.setLayoutManager(layoutManager);
        binding.greetingsRecyclerView.setAdapter(adapter);
        
        binding.greetingsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                
                if (!isLoadingMore && lastVisibleItem >= totalItemCount - 4) {
                    isLoadingMore = true;
                    binding.bottomLoadingView.setVisibility(View.VISIBLE);
                    viewModel.loadMoreIfNeeded(lastVisibleItem, totalItemCount);
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.loadTemplates(true);
        });
    }

    private void setupSearch() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setSearchQuery(s.toString());
            }
        });
    }

    private void setupObservers() {
        viewModel.getTemplates().observe(getViewLifecycleOwner(), templates -> {
            adapter.setTemplates(templates);
            binding.swipeRefreshLayout.setRefreshing(false);
            binding.bottomLoadingView.setVisibility(View.GONE);
            isLoadingMore = false;
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                if (!binding.swipeRefreshLayout.isRefreshing()) {
                    binding.loadingProgressBar.setVisibility(View.VISIBLE);
                }
            } else {
                binding.loadingProgressBar.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showError(error);
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.bottomLoadingView.setVisibility(View.GONE);
                isLoadingMore = false;
            }
        });
    }

    @Override
    public void onTemplateClick(Template template) {
        Bundle args = new Bundle();
        args.putString("templateId", template.getId());
        Navigation.findNavController(requireView())
            .navigate(R.id.action_home_to_template_detail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
