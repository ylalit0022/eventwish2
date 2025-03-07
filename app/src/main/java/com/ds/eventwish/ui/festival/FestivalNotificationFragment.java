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
import androidx.lifecycle.Observer;
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
import com.ds.eventwish.data.model.Result;
import com.ds.eventwish.databinding.FragmentFestivalNotificationBinding;
import com.ds.eventwish.ui.festival.adapter.TemplateAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;

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
    private ShimmerFrameLayout shimmerFrameLayout;
    private Button retryButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NavController navController;
    private TextView errorText;
    private Observer<Result<List<Festival>>> festivalsObserver;
    private boolean isDataLoaded = false;

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
        viewModel = new ViewModelProvider(requireActivity()).get(FestivalViewModel.class);

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
        shimmerFrameLayout = view.findViewById(R.id.shimmerFrameLayout);

        shimmerFrameLayout.startShimmer();


        // Set up retry button
        retryButton.setOnClickListener(v -> refreshFestivals());

        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshFestivals);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        // Create the festivals observer
        createFestivalsObserver();

        // Observe festivals
        viewModel.getFestivals().observe(getViewLifecycleOwner(), festivalsObserver);

        // Only load festivals if we haven't loaded them yet
        if (!isDataLoaded) {
            loadFestivals();
        }
    }

    private void createFestivalsObserver() {

        shimmerFrameLayout.startShimmer();
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        loadingLayout.setVisibility(View.VISIBLE);
        // Create a single observer that we can reuse
        festivalsObserver = result -> {
            loadingLayout.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);

            if (result.isSuccess()) {
                List<Festival> festivals = result.getData();
                if (festivals != null && !festivals.isEmpty()) {
                    displayFestivals(festivals);
                    festivalsContainer.setVisibility(View.VISIBLE);
                    emptyLayout.setVisibility(View.GONE);
                    errorLayout.setVisibility(View.GONE);
                    isDataLoaded = true;
                } else {
                    festivalsContainer.setVisibility(View.GONE);
                    emptyLayout.setVisibility(View.VISIBLE);
                    errorLayout.setVisibility(View.GONE);
                }
            } else {
                errorLayout.setVisibility(View.VISIBLE);
                festivalsContainer.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.GONE);
                errorText.setText(result.getError());
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        // Only load data on first app launch, not when switching fragments
        if (!isDataLoaded) {
            Log.d(TAG, "First time loading data");
            loadFestivals();
        } else {
            Log.d(TAG, "Data already loaded, not refreshing");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // No need to remove observer - it will be handled by the lifecycle owner
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

    private void refreshFestivals() {
        Log.d(TAG, "Manually refreshing festivals from server");
        swipeRefreshLayout.setRefreshing(true);
        errorLayout.setVisibility(View.GONE);
        shimmerFrameLayout.startShimmer();
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        loadingLayout.setVisibility(View.VISIBLE);

        // Force a refresh from the server
        viewModel.refreshFestivals();
    }

    private void loadFestivals() {
        shimmerFrameLayout.startShimmer();
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        loadingLayout.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.GONE);
        festivalsContainer.setVisibility(View.GONE);

        // Load festivals from cache first
        viewModel.loadFestivals();
    }

    private void displayFestivals(List<Festival> festivals) {
        festivalsContainer.removeAllViews();
        Log.d(TAG, "Displaying " + festivals.size() + " festivals");

        for (Festival festival : festivals) {
            View festivalView = getLayoutInflater().inflate(R.layout.item_festival_category, festivalsContainer, false);

            TextView festivalName = festivalView.findViewById(R.id.festivalName);
            TextView festivalDate = festivalView.findViewById(R.id.festivalDate);
            TextView festivalDescription = festivalView.findViewById(R.id.festivalDescription);
            TextView categoryTitle = festivalView.findViewById(R.id.categoryTitle);
            ImageView categoryIcon = festivalView.findViewById(R.id.categoryIcon);
            RecyclerView templatesRecyclerView = festivalView.findViewById(R.id.templatesRecyclerView);

            // Set festival data

            // Set festival data
            festivalName.setText(festival.getName());

            // Format date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            festivalDate.setText(dateFormat.format(festival.getDate()));

            // Set description
            festivalDescription.setText(festival.getDescription());

            // Set category
            categoryTitle.setText(festival.getCategory());

            // Set category icon
            setCategoryIcon(categoryIcon, festival);

            // Set up templates recycler view
            if (festival.getTemplates() != null && !festival.getTemplates().isEmpty()) {
                templatesRecyclerView.setVisibility(View.VISIBLE);
                templatesRecyclerView.setLayoutManager(new LinearLayoutManager(
                        requireContext(), LinearLayoutManager.HORIZONTAL, false));

                // Create click listener
                TemplateAdapter.OnTemplateClickListener clickListener = template -> {
                    Log.d(TAG, "Template clicked: " + template.getId());
                    navigateToTemplateDetail(template);
                };

                // Create adapter with templates and click listener
                TemplateAdapter adapter = new TemplateAdapter(festival.getTemplates(), clickListener);
                templatesRecyclerView.setAdapter(adapter);
            } else {
                templatesRecyclerView.setVisibility(View.GONE);
            }

            festivalsContainer.addView(festivalView);
        }
    }

    private void navigateToTemplateDetail(FestivalTemplate template) {
        // Navigate to template detail fragment
        Bundle args = new Bundle();
        args.putString("templateId", template.getId());
        navController.navigate(R.id.action_festival_notification_to_template_detail, args);
    }
}