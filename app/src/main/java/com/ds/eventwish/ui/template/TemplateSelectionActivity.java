package com.ds.eventwish.ui.template;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.ActivityTemplateSelectionBinding;
import com.ds.eventwish.utils.GridSpacingItemDecoration;
import java.util.List;
import java.util.stream.Collectors;

public class TemplateSelectionActivity extends AppCompatActivity implements TemplateAdapter.OnTemplateInteractionListener {
    private static final String TAG = "TemplateSelectionActivity";
    public static final String EXTRA_SELECTED_TEMPLATE_ID = "selected_template_id";
    public static final String EXTRA_CATEGORY_ID = "category_id";

    private ActivityTemplateSelectionBinding binding;
    private RecyclerView recyclerView;
    private TemplateAdapter adapter;
    private TemplateViewModel viewModel;
    private List<com.ds.eventwish.ui.template.Template> templates;
    
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
        
        String categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        String categoryName = getIntent().getStringExtra("category_name");
        
        // Set title
        if (categoryName != null && !categoryName.isEmpty()) {
            setTitle(categoryName);
        } else {
            setTitle(R.string.all_templates);
        }
        
        setupRecyclerView();
        setupViewModel();
        observeTemplates();
        
        if (categoryId != null && !categoryId.isEmpty()) {
            viewModel.loadTemplatesForCategory(categoryId);
        } else {
            viewModel.loadTemplates();
        }
    }
    
    private void setupRecyclerView() {
        recyclerView = binding.recyclerView;
        adapter = new TemplateAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TemplateViewModel.class);
    }
    
    private void observeTemplates() {
        viewModel.getTemplates().observe(this, templates -> {
            if (templates != null) {
                this.templates = templates;
                adapter.submitList(templates);
                
                // Show appropriate view based on results
                binding.progressBar.setVisibility(View.GONE);
                if (templates.isEmpty()) {
                    binding.emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    binding.emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    
    @Override
    public void onTemplateClick(com.ds.eventwish.ui.template.Template template) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SELECTED_TEMPLATE_ID, template.getId());
        setResult(RESULT_OK, intent);
        finish();
    }
    
    @Override
    public void onTemplateLike(com.ds.eventwish.ui.template.Template template) {
        handleLikeClick(template);
    }
    
    @Override
    public void onTemplateFavorite(com.ds.eventwish.ui.template.Template template) {
        handleFavoriteClick(template);
    }
    
    private void handleLikeClick(com.ds.eventwish.ui.template.Template template) {
        viewModel.toggleLike(template.getId());
    }

    private void handleFavoriteClick(com.ds.eventwish.ui.template.Template template) {
        viewModel.toggleFavorite(template.getId());
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