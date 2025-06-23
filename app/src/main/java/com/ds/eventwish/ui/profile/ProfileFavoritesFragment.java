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
// import com.ds.eventwish.ui.template.TemplateDetailActivity;

import java.util.List;

public class ProfileFavoritesFragment extends Fragment {

    private ProfileViewModel viewModel;
    private GridTemplateAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private TextView emptyTextView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(ProfileViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_grid, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        emptyTextView = view.findViewById(R.id.emptyTextView);
        
        // Set empty view text
        emptyTextView.setText(R.string.profile_no_favorites);
        
        setupRecyclerView();
        observeData();
    }

    private void setupRecyclerView() {
        adapter = new GridTemplateAdapter(requireContext());
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        recyclerView.setAdapter(adapter);
        
        adapter.setOnItemClickListener(template -> {
            // Navigate to template detail
            // TemplateDetailActivity.start(requireContext(), template.getId());
        });
    }

    private void observeData() {
        viewModel.getFavoriteTemplates().observe(getViewLifecycleOwner(), this::updateTemplates);
    }

    private void updateTemplates(List<Template> templates) {
        if (templates == null || templates.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            adapter.setTemplates(templates);
        }
    }
}
