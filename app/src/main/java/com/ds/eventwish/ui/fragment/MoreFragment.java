package com.ds.eventwish.ui.fragment;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.FragmentMoreBinding;
import com.google.android.material.card.MaterialCardView;
import androidx.fragment.app.Fragment;

public class MoreFragment extends Fragment {

    private FragmentMoreBinding binding;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding = FragmentMoreBinding.bind(view);

        binding.cardNotificationPreferences.setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_global_notificationPreferencesFragment));
    }
} 