package com.ds.eventwish.ui.festival;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.CategoryIcon;
import com.ds.eventwish.data.model.Festival;
import com.ds.eventwish.data.model.FestivalTemplate;
import com.ds.eventwish.ui.festival.adapter.TemplateAdapter;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class FestivalNotificationFragment extends Fragment {
    private static final String TAG = "FestivalNotification";

    private FestivalViewModel viewModel;
    private LinearLayout festivalsContainer;
    private LinearLayout loadingLayout;
    private LinearLayout errorLayout;
    private LinearLayout emptyLayout;
    private Button retryButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NavController navController;
    private TextView errorText;

    public static FestivalNotificationFragment newInstance() {
        return new FestivalNotificationFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_festival_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(FestivalViewModel.class);
    
        // Get NavController for navigation
        navController = Navigation.findNavController(view);
    
        // Initialize views
        festivalsContainer = view.findViewById(R.id.festivalsContainer);
        loadingLayout = view.findViewById(R.id.loadingLayout);
        errorLayout = view.findViewById(R.id.errorLayout);
        emptyLayout = view.findViewById(R.id.emptyLayout);
        retryButton = view.findViewById(R.id.retryButton);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        errorText = errorLayout.findViewById(R.id.errorTextView);
    
        // Set up retry button
        retryButton.setOnClickListener(v -> loadFestivals());
    
        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadFestivals);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    
        // Load upcoming festivals
        loadFestivals();
    }

    private void setCategoryIcon(ImageView imageView, Festival festival) {
        if (festival.getCategoryIcon() != null && festival.getCategoryIcon().getCategoryIcon() != null) {
            CategoryIcon icon = festival.getCategoryIcon();
            String iconUrl = icon.getCategoryIcon();
            Log.d(TAG, "Setting category icon - Category: " + festival.getCategory() + ", URL: " + iconUrl);
            
            // Load the icon from URL using Glide
            Glide.with(imageView.getContext())
                    .load(iconUrl)
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground))
                    .centerCrop()
                    .into(imageView);
        } else {
            // Load default icon if no URL is available
            Log.w(TAG, "No icon URL found for category: " + festival.getCategory() +" - ICON URL: "+ 
                  (festival.getCategoryIcon() != null ? festival.getCategoryIcon().getCategoryIcon() : "null"));
            Glide.with(imageView.getContext())
                    .load(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .into(imageView);
        }
    }

    private void loadFestivals() {
        loadingLayout.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.GONE);
        festivalsContainer.setVisibility(View.GONE);

        viewModel.loadFestivals();
        viewModel.getFestivals().observe(getViewLifecycleOwner(), result -> {
            loadingLayout.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);

            if (result.isSuccess()) {
                List<Festival> festivals = result.getData();
                if (festivals != null && !festivals.isEmpty()) {
                    displayFestivals(festivals);
                    festivalsContainer.setVisibility(View.VISIBLE);
                    emptyLayout.setVisibility(View.GONE);
                } else {
                    festivalsContainer.setVisibility(View.GONE);
                    emptyLayout.setVisibility(View.VISIBLE);
                }
            } else {
                errorLayout.setVisibility(View.VISIBLE);
                festivalsContainer.setVisibility(View.GONE);
                errorText.setText(result.getError());
            }
        });
    }

    private void displayFestivals(List<Festival> festivals) {
        festivalsContainer.removeAllViews();
        for (Festival festival : festivals) {
            View festivalView = getLayoutInflater().inflate(R.layout.item_festival_category, festivalsContainer, false);

            TextView festivalName = festivalView.findViewById(R.id.festivalName);
            TextView festivalDate = festivalView.findViewById(R.id.festivalDate);
            TextView festivalDescription = festivalView.findViewById(R.id.festivalDescription);
            TextView categoryTitle = festivalView.findViewById(R.id.categoryTitle);
            ImageView categoryIcon = festivalView.findViewById(R.id.categoryIcon);
            RecyclerView templatesRecyclerView = festivalView.findViewById(R.id.templatesRecyclerView);

            festivalName.setText(festival.getName());
            festivalDescription.setText(festival.getDescription());
            categoryTitle.setText(festival.getCategory());

            // Format and set the date
            try {
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
                festivalDate.setText(outputFormat.format(festival.getDate()));
            } catch (Exception e) {
                festivalDate.setText("Date unavailable");
                Log.e(TAG, "Error formatting date", e);
            }

            // Set category icon
            setCategoryIcon(categoryIcon, festival);

            // Setup templates RecyclerView
            templatesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

            List<FestivalTemplate> templates = festival.getTemplates();
            if (templates != null && !templates.isEmpty()) {
                TemplateAdapter adapter = new TemplateAdapter(templates, template -> {
                    navigateToTemplateDetail(template);
                });
                templatesRecyclerView.setAdapter(adapter);
                templatesRecyclerView.setVisibility(View.VISIBLE);
            } else {
                // No templates available
                templatesRecyclerView.setVisibility(View.GONE);
                Log.d(TAG, "No templates available for festival: " + festival.getName());
            }

            // Add the festival view to the container
            festivalsContainer.addView(festivalView);
        }
    }

    private void navigateToTemplateDetail(FestivalTemplate template) {
        // Navigate to template detail screen with the template ID
        if (template != null && template.getId() != null) {
            Log.d(TAG, "Navigating to template detail with ID: " + template.getId());

            // Navigate using NavController with the template ID
            if (navController != null) {
                Bundle args = new Bundle();
                args.putString("templateId", template.getId());
                navController.navigate(R.id.navigation_template_detail, args);
            } else {
                Log.e(TAG, "NavController is null, cannot navigate");
                Toast.makeText(requireContext(), "Error navigating to template", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Cannot navigate: template or template ID is null");
            Toast.makeText(requireContext(), "Invalid template", Toast.LENGTH_SHORT).show();
        }
    }
}