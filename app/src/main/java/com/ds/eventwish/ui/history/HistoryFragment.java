package com.ds.eventwish.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.databinding.FragmentHistoryBinding;
import com.ds.eventwish.ui.base.BaseFragment;

public class HistoryFragment extends BaseFragment implements HistoryAdapter.OnHistoryItemClickListener {
    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private HistoryAdapter historyAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Hide toolbar
        ((AppCompatActivity) requireActivity()).getSupportActionBar().hide();

        setupViewModel();
        setupRecyclerView();
        observeViewModel();
        viewModel.loadHistory();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
    }

    private void setupRecyclerView() {
        historyAdapter = new HistoryAdapter(this);
        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.historyRecyclerView.setAdapter(historyAdapter);
    }

    private void observeViewModel() {
        viewModel.getHistoryItems().observe(getViewLifecycleOwner(), wishes -> {
            historyAdapter.submitList(wishes);
            updateEmptyState(wishes.isEmpty());
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                binding.emptyText.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showError(error);
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        binding.emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.historyRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onViewClick(SharedWish wish) {
        if (wish != null && wish.getShortCode() != null) {
            HistoryFragmentDirections.ActionHistoryToSharedWish action = 
                HistoryFragmentDirections.actionHistoryToSharedWish(wish.getShortCode());
            Navigation.findNavController(requireView()).navigate(action);
        }
    }

    @Override
    public void onShareClick(SharedWish wish) {
        if (wish != null && wish.getShortCode() != null) {
            String shareUrl = "https://eventwishes.onrender.com/wish/" + wish.getShortCode();
            String shareText = getString(R.string.share_wish_text, shareUrl);
            
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.setType("text/plain");
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
