package com.ds.eventwish.ui.templates;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.Map;

public class TemplatesFragment extends Fragment implements TemplateAdapter.OnTemplateClickListener {
    private TemplateViewModel viewModel;
    private TemplateAdapter adapter;
    private RecyclerView recyclerView;
    private ChipGroup categoryChipGroup;
    private View loadingView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_templates, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupViewModel();
        setupRecyclerView();
        setupScrollListener();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.templatesRecyclerView);
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup);
        loadingView = view.findViewById(R.id.loadingView);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TemplateViewModel.class);
        
        viewModel.getTemplates().observe(getViewLifecycleOwner(), templates -> {
            adapter.setTemplates(templates);
            loadingView.setVisibility(View.GONE);
        });
        
        viewModel.getCategories().observe(getViewLifecycleOwner(), this::setupCategoryChips);
        
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                loadingView.setVisibility(View.GONE);
            }
        });
        
        // Initial load
        loadingView.setVisibility(View.VISIBLE);
        viewModel.loadTemplates(true);
    }

    private void setupRecyclerView() {
        adapter = new TemplateAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
    }

    private void setupScrollListener() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();
                    viewModel.loadMoreIfNeeded(lastVisibleItem, totalItemCount);
                }
            }
        });
    }

    private void setupCategoryChips(Map<String, Integer> categories) {
        categoryChipGroup.removeAllViews();
        
        // Add "All" chip
        Chip allChip = createChip("All");
        allChip.setChecked(true);
        categoryChipGroup.addView(allChip);
        
        // Add category chips
        for (Map.Entry<String, Integer> entry : categories.entrySet()) {
            Chip chip = createChip(entry.getKey() + " (" + entry.getValue() + ")");
            categoryChipGroup.addView(chip);
        }
    }

    private Chip createChip(String text) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setCheckedIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.chip_background_color);
        
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                String category = text.split(" \\(")[0];
                viewModel.setCategory(category.equals("All") ? null : category);
                loadingView.setVisibility(View.VISIBLE);
            }
        });
        
        return chip;
    }

    @Override
    public void onTemplateClick(Template template) {
        if (template != null && template.getId() != null) {
            Bundle args = new Bundle();
            args.putString("templateId", template.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_templates_to_template_detail, args);
        } else {
            Toast.makeText(requireContext(), "Invalid template", Toast.LENGTH_SHORT).show();
        }
    }
}
