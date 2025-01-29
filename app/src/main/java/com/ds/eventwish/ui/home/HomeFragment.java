package com.ds.eventwish.ui.home;

import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import com.ds.eventwish.ui.home.Category;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentHomeBinding;
import com.ds.eventwish.ui.base.BaseFragment;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends BaseFragment {
    private FragmentHomeBinding binding;
    private GreetingsAdapter greetingsAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI();
        setupRecyclerView();
        setupSearch();
        setupClickListeners();
    }

    private void setupUI() {
          // Add method to load categories
          loadCategories();
        binding.categoriesChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = group.findViewById(checkedIds.get(0));
                if (chip != null) {
                    filterGreetingsByCategory(chip.getText().toString());
                }
            } else {
                // No filter selected, show all
                greetingsAdapter.resetFilter();
            }
        });
    }

    private void loadCategories() {
        List<Category> categories = getCategoryData(); // Get from your data source
        
        // Clear existing chips
        binding.categoriesChipGroup.removeAllViews();
        
        // Add "All" category chip
        addChip("All");
        
        // Add category chips dynamically
        for (Category category : categories) {
            addChip(category.getName());
        }
    }
    
    private void addChip(String categoryName) {
        Chip chip = new Chip(requireContext());
        chip.setText(categoryName);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(true);
        
        // Set chip styling
        chip.setChipBackgroundColorResource(R.color.chip_background_color);
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.chip_text_color));
        
        binding.categoriesChipGroup.addView(chip);
    }

    private List<Category> getCategoryData() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("1", "Birthday", ""));
        categories.add(new Category("2", "Anniversary", ""));
        categories.add(new Category("3", "Wedding", ""));
        categories.add(new Category("4", "Festivals", ""));
        categories.add(new Category("5", "Holi", ""));
        return categories;
    }

    private void setupRecyclerView() {
        greetingsAdapter = new GreetingsAdapter();
        binding.greetingsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.greetingsRecyclerView.setAdapter(greetingsAdapter);
    }

    private void setupSearch() {
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                greetingsAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupClickListeners() {
//        binding.menuButton.setOnClickListener(v -> {
//            // Implement menu click
//        });

    }

    private void filterGreetingsByCategory(String category) {
        greetingsAdapter.filterByCategory(category);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
