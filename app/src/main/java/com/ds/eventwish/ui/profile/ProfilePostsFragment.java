package com.ds.eventwish.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.ui.adapter.GridTemplateAdapter;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;

/**
 * Fragment for displaying user's posts in a grid layout
 */
public class ProfilePostsFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private MaterialTextView emptyTextView;
    private GridTemplateAdapter adapter;
    private ProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_grid, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        try {
            // Initialize views with explicit type checking
            recyclerView = view.findViewById(R.id.recyclerView);
            emptyView = view.findViewById(R.id.emptyView);
            
            View emptyTextViewCandidate = view.findViewById(R.id.emptyTextView);
            if (emptyTextViewCandidate instanceof MaterialTextView) {
                emptyTextView = (MaterialTextView) emptyTextViewCandidate;
            } else {
                // Fallback to find TextView if MaterialTextView cast fails
                emptyTextView = view.findViewById(R.id.emptyTextView);
            }
            
            // Set up RecyclerView
            if (recyclerView != null) {
                recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
                adapter = new GridTemplateAdapter(requireContext());
                recyclerView.setAdapter(adapter);
            }
            
            // Set empty view text
            if (emptyTextView != null) {
                emptyTextView.setText(R.string.profile_no_posts);
            }
            
            // Get ViewModel
            viewModel = new ViewModelProvider(requireParentFragment()).get(ProfileViewModel.class);
            
            // Load data
            loadData();
            
        } catch (Exception e) {
            // Log error and handle gracefully
            android.util.Log.e("ProfilePostsFragment", "Error initializing views", e);
        }
    }
    
    private void loadData() {
        try {
            if (adapter != null) {
                // In a real app, this would load from the ViewModel
                // For now, we'll just show an empty state
                adapter.setTemplates(new ArrayList<>());
            }
            
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
            
            if (emptyView != null) {
                emptyView.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            android.util.Log.e("ProfilePostsFragment", "Error loading data", e);
        }
    }
}
