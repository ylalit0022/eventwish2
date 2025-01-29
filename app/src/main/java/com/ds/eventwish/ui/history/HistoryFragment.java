package com.ds.eventwish.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.ds.eventwish.databinding.FragmentHistoryBinding;
import com.ds.eventwish.ui.base.BaseFragment;
import com.ds.eventwish.ui.home.GreetingItem;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends BaseFragment {
    private FragmentHistoryBinding binding;
    private HistoryAdapter historyAdapter;
    private SharedPrefsManager sharedPrefsManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPrefsManager = new SharedPrefsManager(requireContext());
        setupRecyclerView();
        loadHistoryData();
    }

    private void setupRecyclerView() {
        historyAdapter = new HistoryAdapter();
        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.historyRecyclerView.setAdapter(historyAdapter);
    }

    private void loadHistoryData() {
        List<GreetingItem> historyItems = sharedPrefsManager.getHistoryItems();
        historyAdapter.submitList(historyItems);
        
        // if (historyItems.isEmpty()) {
        //     binding.emptyText.setVisibility(View.VISIBLE);
        //     binding.historyRecyclerView.setVisibility(View.GONE);
        // } else {
        //     binding.emptyText.setVisibility(View.GONE);
        //     binding.historyRecyclerView.setVisibility(View.VISIBLE);
        //     historyAdapter.submitList(historyItems);
        // }
    }

    public void addToHistory(GreetingItem item) {
        sharedPrefsManager.saveHistoryItem(item);
        loadHistoryData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
