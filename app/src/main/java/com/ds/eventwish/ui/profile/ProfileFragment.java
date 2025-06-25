package com.ds.eventwish.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.databinding.FragmentProfileBinding;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Button;
import com.bumptech.glide.Glide;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private ProfilePagerAdapter pagerAdapter;

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
        
        // Set up toolbar
        binding.toolbar.setNavigationOnClickListener(v -> {
            // Navigate to settings
            Toast.makeText(getContext(), "Settings clicked", Toast.LENGTH_SHORT).show();
        });
        
        // Set up swipe refresh
        setupSwipeRefresh();
        
        // Set up user profile
        setupUserProfile();
        
        // Set up stats
        setupStats();
        
        // Set up ViewPager and TabLayout
        setupViewPager();
    }
    
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            com.google.android.material.R.color.design_default_color_primary,
            com.google.android.material.R.color.design_default_color_secondary
        );
        
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            // Refresh all data
            profileViewModel.refreshUserData();
            
            // Observe refresh completion
            profileViewModel.isRefreshing().observe(getViewLifecycleOwner(), isRefreshing -> {
                binding.swipeRefreshLayout.setRefreshing(isRefreshing);
            });
        });
    }
    
    private void setupUserProfile() {
        // Observe username changes
        profileViewModel.getUsername().observe(getViewLifecycleOwner(), username -> {
            if (username != null && !username.isEmpty()) {
            binding.usernameText.setText(username);
            } else {
                binding.usernameText.setText(R.string.default_username);
            }
        });
        
        // Observe email changes
        profileViewModel.getEmail().observe(getViewLifecycleOwner(), email -> {
            if (email != null && !email.isEmpty()) {
            binding.emailText.setText(email);
            } else {
                binding.emailText.setText(R.string.default_email);
            }
        });
        
        // Observe profile photo changes
        profileViewModel.getProfilePhoto().observe(getViewLifecycleOwner(), photoUrl -> {
            if (photoUrl != null) {
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .circleCrop()
                    .into(binding.profileImage);
            } else {
                binding.profileImage.setImageResource(R.drawable.default_profile);
            }
        });

        // Observe error messages
        profileViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        // Set up edit profile button
        binding.editProfileButton.setOnClickListener(v -> {
            showEditProfileDialog();
        });
    }

    private void showEditProfileDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        
        TextInputLayout nameInputLayout = dialogView.findViewById(R.id.nameInputLayout);
        TextInputEditText nameInput = dialogView.findViewById(R.id.nameInput);
        nameInput.setText(binding.usernameText.getText());
        
        AlertDialog dialog = builder.setTitle(R.string.edit_profile_title)
               .setView(dialogView)
               .setPositiveButton(R.string.save, null)
               .setNegativeButton(R.string.cancel, null)
               .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String newName = nameInput.getText().toString().trim();
                if (newName.isEmpty()) {
                    nameInputLayout.setError(getString(R.string.name_required));
                    return;
                }

                // Clear any previous errors
                nameInputLayout.setError(null);

                // Update profile - keep existing email
                String currentEmail = binding.emailText.getText().toString();
                profileViewModel.updateProfile(newName, currentEmail);
                
                // Error handling is now done through the ViewModel's error LiveData
                profileViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
                    if (error != null) {
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    } else {
                Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                    }
                });
            });
        });

        dialog.show();
    }
    
    private void setupStats() {
        // Show loading state initially
        binding.postsCount.setText("-");
        binding.likesCount.setText("-");
        binding.favoritesCount.setText("-");
        
        // Set up posts count
        profileViewModel.getPostsCount().observe(getViewLifecycleOwner(), count -> {
            binding.postsCount.setText(String.valueOf(count));
            Log.d("ProfileFragment", "Posts count updated: " + count);
        });
        
        // Set up likes count
        profileViewModel.getLikesCount().observe(getViewLifecycleOwner(), count -> {
            binding.likesCount.setText(String.valueOf(count));
            Log.d("ProfileFragment", "Likes count updated: " + count);
        });
        
        // Set up favorites count
        profileViewModel.getFavoritesCount().observe(getViewLifecycleOwner(), count -> {
            binding.favoritesCount.setText(String.valueOf(count));
            Log.d("ProfileFragment", "Favorites count updated: " + count);
        });
        
        // Set click listeners for stats
        binding.likesStatsContainer.setOnClickListener(v -> {
            binding.tabLayout.getTabAt(1).select();
        });
        
        binding.favoritesStatsContainer.setOnClickListener(v -> {
            binding.tabLayout.getTabAt(2).select();
        });

        // Observe refresh state
        profileViewModel.isRefreshing().observe(getViewLifecycleOwner(), isRefreshing -> {
            binding.swipeRefreshLayout.setRefreshing(isRefreshing);
            if (isRefreshing) {
                Log.d("ProfileFragment", "Refreshing profile data...");
            }
        });

        // Force a refresh to ensure we have latest data
        profileViewModel.refreshUserData();
    }
    
    private void setupViewPager() {
        // Initialize the adapter
        pagerAdapter = new ProfilePagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            // Set tab icons and text programmatically
            switch (position) {
                case 0:
                    tab.setIcon(R.drawable.ic_grid);
                    tab.setText(R.string.profile_posts);
                    break;
                case 1:
                    tab.setIcon(R.drawable.ic_heart_outline);
                    tab.setText(R.string.profile_likes);
                    break;
                case 2:
                    tab.setIcon(R.drawable.ic_bookmark_outline);
                    tab.setText(R.string.profile_favorites);
                    break;
            }
        }).attach();
        
        // Set initial tab
        binding.viewPager.setCurrentItem(0, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    
    /**
     * Adapter for the profile content tabs
     */
    private static class ProfilePagerAdapter extends FragmentStateAdapter {
        
        public ProfilePagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Create appropriate fragment based on position
            switch (position) {
                case 0:
                    return new ProfilePostsFragment();
                case 1:
                    return new ProfileLikesFragment();
                case 2:
                    return new ProfileFavoritesFragment();
                default:
                    return new ProfilePostsFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 3; // Posts, Likes, Favorites
        }
    }
} 