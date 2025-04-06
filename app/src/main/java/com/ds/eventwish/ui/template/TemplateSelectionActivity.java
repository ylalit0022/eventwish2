package com.ds.eventwish.ui.template;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.ActivityTemplateSelectionBinding;
import com.ds.eventwish.utils.GridSpacingItemDecoration;
import java.util.ArrayList;
import java.util.List;

public class TemplateSelectionActivity extends AppCompatActivity {
    private static final String TAG = "TemplateSelection";
    private ActivityTemplateSelectionBinding binding;
    private TemplateAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTemplateSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Setup toolbar with back button
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Get category ID from intent if available
        String categoryId = getIntent().getStringExtra("category_id");
        String categoryName = getIntent().getStringExtra("category_name");
        
        // Set title
        if (categoryName != null && !categoryName.isEmpty()) {
            setTitle(categoryName);
        } else {
            setTitle(R.string.all_templates);
        }
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Load templates
        loadTemplates(categoryId);
    }
    
    private void setupRecyclerView() {
        // Initialize adapter
        adapter = new TemplateAdapter(template -> {
            // Handle template click
            onTemplateSelected(template);
        });
        
        // Set up grid layout
        int spanCount = 2; // Number of columns
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        binding.recyclerViewTemplates.setLayoutManager(layoutManager);
        
        // Add spacing
        int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        binding.recyclerViewTemplates.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, true));
        
        // Set adapter
        binding.recyclerViewTemplates.setAdapter(adapter);
    }
    
    private void loadTemplates(String categoryId) {
        // Show loading indicator
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerViewTemplates.setVisibility(View.GONE);
        binding.emptyView.setVisibility(View.GONE);
        
        // For now, just create some dummy templates
        // In a real app, you would load these from a repository or API
        List<Template> templates = createDummyTemplates();
        
        // Filter by category if needed
        if (categoryId != null && !categoryId.isEmpty()) {
            List<Template> filteredTemplates = new ArrayList<>();
            for (Template template : templates) {
                if (categoryId.equals(template.getCategoryId())) {
                    filteredTemplates.add(template);
                }
            }
            templates = filteredTemplates;
        }
        
        // Update adapter with templates
        adapter.submitList(templates);
        
        // Show appropriate view based on results
        binding.progressBar.setVisibility(View.GONE);
        if (templates.isEmpty()) {
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.recyclerViewTemplates.setVisibility(View.GONE);
        } else {
            binding.emptyView.setVisibility(View.GONE);
            binding.recyclerViewTemplates.setVisibility(View.VISIBLE);
        }
    }
    
    private void onTemplateSelected(Template template) {
        // Navigate to template customization screen
        Log.d(TAG, "Template selected: " + template.getId());
        
        // In a real app, you would navigate to a customization screen
        // For example:
        // Intent intent = new Intent(this, TemplateCustomizeActivity.class);
        // intent.putExtra("template_id", template.getId());
        // startActivity(intent);
    }
    
    private List<Template> createDummyTemplates() {
        List<Template> templates = new ArrayList<>();
        
        // Add some dummy templates
        templates.add(new Template("1", "Birthday Wish", "birthday", "https://example.com/image1.jpg"));
        templates.add(new Template("2", "Anniversary", "anniversary", "https://example.com/image2.jpg"));
        templates.add(new Template("3", "Graduation", "graduation", "https://example.com/image3.jpg"));
        templates.add(new Template("4", "Wedding", "wedding", "https://example.com/image4.jpg"));
        templates.add(new Template("5", "Baby Shower", "baby", "https://example.com/image5.jpg"));
        templates.add(new Template("6", "Get Well Soon", "health", "https://example.com/image6.jpg"));
        
        return templates;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        binding.shimmerLayout.startShimmer();
    }
    
    @Override
    protected void onPause() {
        binding.shimmerLayout.stopShimmer();
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 