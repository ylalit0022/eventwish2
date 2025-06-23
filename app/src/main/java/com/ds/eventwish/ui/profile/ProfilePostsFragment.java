package com.ds.eventwish.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.eventwish.R;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.ui.adapter.GridTemplateAdapter;

import java.util.ArrayList;

/**
 * Fragment for displaying user's posts in a grid layout
 */
public class ProfilePostsFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private TextView emptyTextView;
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
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        emptyTextView = view.findViewById(R.id.emptyTextView);
        
        // Set up RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        adapter = new GridTemplateAdapter(requireContext());
        recyclerView.setAdapter(adapter);
        
        // Set empty view text
        emptyTextView.setText(R.string.profile_no_posts);
        
        // Get ViewModel
        viewModel = new ViewModelProvider(requireParentFragment()).get(ProfileViewModel.class);
        
        // Load data
        loadData();
    }
    
    private void loadData() {
        // In a real app, this would load from the ViewModel
        // For now, we'll just show an empty state
        adapter.setTemplates(new ArrayList<>());
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
    }
}
