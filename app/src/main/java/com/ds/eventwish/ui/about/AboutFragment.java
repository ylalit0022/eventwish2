package com.ds.eventwish.ui.about;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.ds.eventwish.databinding.FragmentAboutBinding;
import com.ds.eventwish.ui.base.BaseFragment;

public class AboutFragment extends BaseFragment {
    private FragmentAboutBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupContent();
    }

    private void setupContent() {
        binding.versionText.setText("Version 1.0");
        
        binding.privacyCard.setOnClickListener(v -> {
            // Open privacy policy in browser
        });
        
        binding.termsCard.setOnClickListener(v -> {
            // Open terms of service in browser
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
