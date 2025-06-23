package com.ds.eventwish.ui.profile;

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
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.databinding.FragmentProfileBinding;
import com.google.android.material.tabs.TabLayoutMediator;

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
        
        // Set up user profile
        setupUserProfile();
        
        // Set up stats
        setupStats();
        
        // Set up edit profile button
        binding.editProfileButton.setOnClickListener(v -> {
            // Handle edit profile click
            Toast.makeText(getContext(), "Edit profile feature coming soon", Toast.LENGTH_SHORT).show();
        });
        
        // Set up ViewPager and TabLayout
        setupViewPager();
    }
    
    private void setupUserProfile() {
        profileViewModel.getUsername().observe(getViewLifecycleOwner(), username -> {
            binding.usernameText.setText(username);
        });
        
        profileViewModel.getEmail().observe(getViewLifecycleOwner(), email -> {
            binding.emailText.setText(email);
        });
        
        // Set bio text
        binding.bioText.setText(getString(R.string.profile_bio_default));
    }
    
    private void setupStats() {
        // Set up posts count
        profileViewModel.getPostsCount().observe(getViewLifecycleOwner(), count -> {
            binding.postsCount.setText(String.valueOf(count));
        });
        
        // Set up likes count
        profileViewModel.getLikesCount().observe(getViewLifecycleOwner(), count -> {
            binding.likesCount.setText(String.valueOf(count));
        });
        
        // Set up favorites count
        profileViewModel.getFavoritesCount().observe(getViewLifecycleOwner(), count -> {
            binding.favoritesCount.setText(String.valueOf(count));
        });
        
        // Set click listeners for stats
        binding.likesStatsContainer.setOnClickListener(v -> {
            binding.tabLayout.getTabAt(1).select();
        });
        
        binding.favoritesStatsContainer.setOnClickListener(v -> {
            binding.tabLayout.getTabAt(2).select();
        });
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