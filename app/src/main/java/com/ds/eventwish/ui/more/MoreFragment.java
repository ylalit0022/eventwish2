package com.ds.eventwish.ui.more;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentMoreBinding;
import com.ds.eventwish.ui.base.BaseFragment;

public class MoreFragment extends BaseFragment {
    private FragmentMoreBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupClickListeners();
    }

    private void setupClickListeners() {
//        binding.settingsCard.setOnClickListener(v -> {
//            // Navigate to settings
//            // Navigation.findNavController(v).navigate(R.id.action_more_to_settings);
//        });

        binding.aboutCard.setOnClickListener(v -> {
            // Navigate to about
            Navigation.findNavController(v).navigate(R.id.action_more_to_about);
        });

        binding.helpCard.setOnClickListener(v -> {
            // Navigate to contact
            Navigation.findNavController(v).navigate(R.id.action_more_to_contact);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
