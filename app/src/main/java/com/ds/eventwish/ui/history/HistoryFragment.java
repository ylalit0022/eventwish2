package com.ds.eventwish.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.ds.eventwish.MainActivity;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.databinding.FragmentHistoryBinding;
import com.ds.eventwish.ui.base.BaseFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ds.eventwish.utils.SocialShareUtil;

import java.util.ArrayList;

public class HistoryFragment extends BaseFragment implements HistoryAdapter.OnHistoryItemClickListener {
    private static final String TAG = "HistoryFragment";
    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private HistoryAdapter historyAdapter;
    private BottomNavigationView bottomNav;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Initializing HistoryFragment");
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating view");
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Setting up UI components");

        // Get bottom navigation from activity
        if (getActivity() instanceof MainActivity) {
            bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            bottomNav.setVisibility(View.VISIBLE);
        }

        setupRecyclerView();
        setupSwipeRefresh();
        setupRetryButton();
        setupClearHistoryButton();
        observeViewModel();
    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: Initializing RecyclerView");
        historyAdapter = new HistoryAdapter(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(historyAdapter);
    }

    private void setupSwipeRefresh() {
        Log.d(TAG, "setupSwipeRefresh: Setting up SwipeRefreshLayout");
        binding.swipeRefresh.setOnRefreshListener(() -> {
            binding.swipeRefresh.setRefreshing(true);
            viewModel.loadHistory();
        });
    }

    private void setupRetryButton() {
        Log.d(TAG, "setupRetryButton: Setting up retry button");
        binding.retryLayout.retryButton.setOnClickListener(v -> viewModel.loadHistory());
    }

    private void setupClearHistoryButton() {
        Log.d(TAG, "setupClearHistoryButton: Setting up clear history button");
        binding.clearHistoryButton.setOnClickListener(v -> showClearHistoryDialog());
    }

    private void showClearHistoryDialog() {
        Log.d(TAG, "showClearHistoryDialog: Showing clear history confirmation dialog");
        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.clear_history_title)
            .setMessage(R.string.clear_history_message)
            .setPositiveButton(R.string.clear, (dialog, which) -> {
                Log.d(TAG, "Clear history confirmed");
                viewModel.clearHistory();
                // Force refresh the adapter
                historyAdapter.submitList(null);
                historyAdapter.submitList(new ArrayList<>());
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void observeViewModel() {
        Log.d(TAG, "observeViewModel: Setting up observers");
        
        viewModel.getHistoryItems().observe(getViewLifecycleOwner(), wishes -> {
            Log.d(TAG, "History items updated: " + (wishes != null ? wishes.size() : 0) + " items");
            historyAdapter.submitList(wishes);
            updateViewStates(wishes == null || wishes.isEmpty(), false, null);
            binding.swipeRefresh.setRefreshing(false);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "Loading state changed: " + isLoading);
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading) {
                binding.swipeRefresh.setRefreshing(false);
            }
            if (isLoading) {
                binding.retryLayout.getRoot().setVisibility(View.GONE);
                binding.emptyView.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Log.e(TAG, "Error received: " + error);
                updateViewStates(false, true, error);
                binding.swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void updateViewStates(boolean isEmpty, boolean isError, String errorMessage) {
        Log.d(TAG, String.format("updateViewStates: isEmpty=%s, isError=%s, errorMessage=%s", 
            isEmpty, isError, errorMessage));
            
        binding.recyclerView.setVisibility(isEmpty || isError ? View.GONE : View.VISIBLE);
        binding.emptyView.setVisibility(isEmpty && !isError ? View.VISIBLE : View.GONE);
        binding.retryLayout.getRoot().setVisibility(isError ? View.VISIBLE : View.GONE);
        binding.progressBar.setVisibility(View.GONE);
        
        if (isError && errorMessage != null) {
            binding.retryLayout.errorText.setText(errorMessage);
        }
    }

    @Override
    public void onViewClick(SharedWish wish) {
        if (wish != null && wish.getShortCode() != null) {
            Log.d(TAG, "onViewClick: Navigating to wish detail with code: " + wish.getShortCode());
            Navigation.findNavController(requireView())
                .navigate(HistoryFragmentDirections.actionHistoryToSharedWish(wish.getShortCode()));
        }
    }

    @Override
    public void onShareClick(SharedWish wish) {
        if (wish != null && wish.getShortCode() != null) {
            Log.d(TAG, "onShareClick: Sharing wish with code: " + wish.getShortCode());
            
            // Add to history before sharing
            viewModel.addToHistory(wish);
            
            String shareUrl = getString(R.string.share_url_format, wish.getShortCode());
            String shareText = getString(R.string.share_wish_text, shareUrl);
            
            try {
                // Use the new SocialShareUtil to create a sharing intent with preview image
                String previewUrl = wish.getPreviewUrl();
                String title = getString(R.string.share_wish_title);
                Log.d(TAG, "Sharing with preview URL: " + previewUrl);
                
                Intent chooserIntent = SocialShareUtil.createChooserIntent(
                        requireContext(),
                        shareText,
                        previewUrl,
                        title,
                        getString(R.string.share_via));
                
                startActivity(chooserIntent);
                Log.d(TAG, "Share intent started successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error creating sharing intent", e);
                
                // Fallback to simple text sharing
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                shareIntent.setType("text/plain");
                
                try {
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
                    Log.d(TAG, "Fallback share intent started successfully");
                } catch (Exception ex) {
                    Log.e(TAG, "Error starting fallback share intent", ex);
                    Toast.makeText(requireContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Log.e(TAG, "onShareClick: Invalid wish or shortCode");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: Cleaning up resources");
        binding = null;
    }
}
