package com.ds.eventwish.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.databinding.FragmentProfileBinding;
import com.ds.eventwish.ui.adapter.HorizontalTemplateAdapter;

import java.util.List;

public class ProfileFragment extends Fragment implements HorizontalTemplateAdapter.OnTemplateInteractionListener {

    private FragmentProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private HorizontalTemplateAdapter likedTemplatesAdapter;
    private HorizontalTemplateAdapter favoritedTemplatesAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        
        // Set up user profile
        profileViewModel.getUsername().observe(getViewLifecycleOwner(), username -> {
            binding.usernameText.setText(username);
        });
        
        profileViewModel.getEmail().observe(getViewLifecycleOwner(), email -> {
            binding.emailText.setText(email);
        });
        
        binding.editProfileButton.setOnClickListener(v -> {
            // Handle edit profile click
            Toast.makeText(getContext(), "Edit profile feature coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Set up liked templates recycler view
        setupLikedTemplatesRecyclerView();
        
        // Set up favorited templates recycler view
        setupFavoritedTemplatesRecyclerView();
        
        // Load templates data
        loadTemplatesData();
    }
    
    private void setupLikedTemplatesRecyclerView() {
        likedTemplatesAdapter = new HorizontalTemplateAdapter(requireContext(), this);
        binding.likedTemplatesRecyclerView.setAdapter(likedTemplatesAdapter);
        
        // Show loading indicator
        binding.likedTemplatesProgressBar.setVisibility(View.VISIBLE);
    }
    
    private void setupFavoritedTemplatesRecyclerView() {
        favoritedTemplatesAdapter = new HorizontalTemplateAdapter(requireContext(), this);
        binding.favoritedTemplatesRecyclerView.setAdapter(favoritedTemplatesAdapter);
        
        // Show loading indicator
        binding.favoritedTemplatesProgressBar.setVisibility(View.VISIBLE);
    }
    
    private void loadTemplatesData() {
        // Load liked templates
        profileViewModel.getMostRecentlyLikedTemplates().observe(getViewLifecycleOwner(), templates -> {
            binding.likedTemplatesProgressBar.setVisibility(View.GONE);
            
            if (templates != null && !templates.isEmpty()) {
                likedTemplatesAdapter.setTemplates(templates);
                binding.likedTemplatesRecyclerView.setVisibility(View.VISIBLE);
                binding.noLikedTemplatesText.setVisibility(View.GONE);
            } else {
                binding.likedTemplatesRecyclerView.setVisibility(View.GONE);
                binding.noLikedTemplatesText.setVisibility(View.VISIBLE);
            }
        });
        
        // Load favorited templates
        profileViewModel.getMostRecentlyFavoritedTemplates().observe(getViewLifecycleOwner(), templates -> {
            binding.favoritedTemplatesProgressBar.setVisibility(View.GONE);
            
            if (templates != null && !templates.isEmpty()) {
                favoritedTemplatesAdapter.setTemplates(templates);
                binding.favoritedTemplatesRecyclerView.setVisibility(View.VISIBLE);
                binding.noFavoritedTemplatesText.setVisibility(View.GONE);
            } else {
                binding.favoritedTemplatesRecyclerView.setVisibility(View.GONE);
                binding.noFavoritedTemplatesText.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    // HorizontalTemplateAdapter.OnTemplateInteractionListener implementation
    
    @Override
    public void onTemplateClick(Template template) {
        // Navigate to template detail
        Bundle args = new Bundle();
        args.putString("templateId", template.getId());
        Navigation.findNavController(requireView()).navigate(R.id.action_navigation_profile_to_templateDetailFragment, args);
    }
    
    @Override
    public void onTemplateLike(Template template) {
        // Toggle like state
        boolean newLikeState = !template.isLiked();
        profileViewModel.toggleTemplateLike(template.getId(), newLikeState);
    }
    
    @Override
    public void onTemplateFavorite(Template template) {
        // Toggle favorite state
        boolean newFavoriteState = !template.isFavorited();
        profileViewModel.toggleTemplateFavorite(template.getId(), newFavoriteState);
    }
} 