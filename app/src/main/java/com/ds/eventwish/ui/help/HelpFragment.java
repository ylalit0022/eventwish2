package com.ds.eventwish.ui.help;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.ds.eventwish.databinding.FragmentHelpBinding;
import com.ds.eventwish.ui.base.BaseFragment;

public class HelpFragment extends BaseFragment {
    private FragmentHelpBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHelpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupFAQs();
    }

    private void setupFAQs() {
        binding.contactCard.setOnClickListener(v -> {
            // Open email client with support email
            // String email = "support@eventwish.com";
            // Intent intent = new Intent(Intent.ACTION_SENDTO);
            // intent.setData(Uri.parse("mailto:" + email));
            // startActivity(Intent.createChooser(intent, "Send Email"));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
