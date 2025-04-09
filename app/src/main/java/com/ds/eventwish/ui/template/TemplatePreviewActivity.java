package com.ds.eventwish.ui.template;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.ui.home.adapter.TemplateAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that showcases the new template preview design with
 * full-screen image and transparent-to-black gradient overlays
 */
public class TemplatePreviewActivity extends AppCompatActivity {
    
    private static final String TAG = "TemplatePreviewActivity";
    private static final String EXTRA_TEMPLATE_ID = "template_id";
    private static final String EXTRA_TEMPLATE_TITLE = "template_title";
    private static final String EXTRA_TEMPLATE_CATEGORY = "template_category";
    private static final String EXTRA_TEMPLATE_IMAGE_URL = "template_image_url";
    
    // UI elements
    private ImageView templateBackgroundImage;
    private TextView templateTitle;
    private TextView categoryText;
    private ImageView categoryIcon;
    private ImageButton backButton;
    private ImageButton shareButton;
    private ImageButton favoriteButton;
    private MaterialButton previewButton;
    private MaterialButton useTemplateButton;
    private RecyclerView templatesRecyclerView;
    
    private TemplateAdapter relatedTemplatesAdapter;
    
    /**
     * Static method to create an intent to launch this activity
     */
    public static Intent createIntent(Context context, String templateId, String title, 
                                     String category, String imageUrl) {
        Intent intent = new Intent(context, TemplatePreviewActivity.class);
        intent.putExtra(EXTRA_TEMPLATE_ID, templateId);
        intent.putExtra(EXTRA_TEMPLATE_TITLE, title);
        intent.putExtra(EXTRA_TEMPLATE_CATEGORY, category);
        intent.putExtra(EXTRA_TEMPLATE_IMAGE_URL, imageUrl);
        return intent;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_preview);
        
        // Extract template data from intent
        Intent intent = getIntent();
        String templateId = intent.getStringExtra(EXTRA_TEMPLATE_ID);
        String title = intent.getStringExtra(EXTRA_TEMPLATE_TITLE);
        String category = intent.getStringExtra(EXTRA_TEMPLATE_CATEGORY);
        String imageUrl = intent.getStringExtra(EXTRA_TEMPLATE_IMAGE_URL);
        
        // Initialize views
        initViews();
        
        // Set template data
        setupTemplateData(title, category, imageUrl);
        
        // Set up click listeners
        setupClickListeners();
        
        // Set up related templates
        setupRelatedTemplates(category);
    }
    
    private void initViews() {
        templateBackgroundImage = findViewById(R.id.templateBackgroundImage);
        templateTitle = findViewById(R.id.templateTitle);
        categoryText = findViewById(R.id.categoryText);
        categoryIcon = findViewById(R.id.categoryIcon);
        backButton = findViewById(R.id.backButton);
        shareButton = findViewById(R.id.shareButton);
        favoriteButton = findViewById(R.id.favoriteButton);
        previewButton = findViewById(R.id.previewButton);
        useTemplateButton = findViewById(R.id.useTemplateButton);
        templatesRecyclerView = findViewById(R.id.templatesRecyclerView);
    }
    
    private void setupTemplateData(String title, String category, String imageUrl) {
        // Set title
        templateTitle.setText(title);
        
        // Set category
        categoryText.setText(category);
        
        // Load background image using Glide
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .centerCrop()
            .into(templateBackgroundImage);
    }
    
    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());
        
        // Share button
        shareButton.setOnClickListener(v -> 
            Toast.makeText(this, "Share template", Toast.LENGTH_SHORT).show());
        
        // Favorite button
        favoriteButton.setOnClickListener(v -> {
            // Toggle favorite state
            boolean isFavorited = toggleFavorite();
            Toast.makeText(this, 
                isFavorited ? "Added to favorites" : "Removed from favorites", 
                Toast.LENGTH_SHORT).show();
        });
        
        // Preview button
        previewButton.setOnClickListener(v -> 
            Toast.makeText(this, "Preview template", Toast.LENGTH_SHORT).show());
        
        // Use template button
        useTemplateButton.setOnClickListener(v -> 
            Toast.makeText(this, "Using template", Toast.LENGTH_SHORT).show());
    }
    
    private boolean toggleFavorite() {
        // Toggle the favorite icon
        boolean isFavorited = favoriteButton.getTag() != null && 
                             (boolean) favoriteButton.getTag();
        
        isFavorited = !isFavorited;
        favoriteButton.setTag(isFavorited);
        
        // Set the appropriate icon
        favoriteButton.setImageResource(isFavorited ? 
            R.drawable.ic_favorite_filled : R.drawable.ic_favorite);
        
        return isFavorited;
    }
    
    private void setupRelatedTemplates(String category) {
        // Initialize RecyclerView with a grid layout
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        templatesRecyclerView.setLayoutManager(layoutManager);
        
        // Create a mock list of related templates
        List<Template> relatedTemplates = createMockRelatedTemplates(category);
        
        // Create and set the adapter
        relatedTemplatesAdapter = new TemplateAdapter(template -> {
            // Template click handler (for demo purposes only)
            Toast.makeText(this, "Selected: " + template.getTitle(), 
                Toast.LENGTH_SHORT).show();
        });
        
        templatesRecyclerView.setAdapter(relatedTemplatesAdapter);
        
        // Update the adapter with templates
        relatedTemplatesAdapter.updateTemplates(relatedTemplates);
    }
    
    /**
     * Create mock templates for UI demonstration purposes
     */
    private List<Template> createMockRelatedTemplates(String category) {
        List<Template> mockTemplates = new ArrayList<>();
        
        // Create 6 mock templates for demonstration
        for (int i = 1; i <= 6; i++) {
            Template template = new Template();
            template.setId("template_" + i);
            template.setTitle(category + " Template " + i);
            template.setCategory(category);
            // Note: In a real app, you would set proper image URLs
            mockTemplates.add(template);
        }
        
        return mockTemplates;
    }
} 