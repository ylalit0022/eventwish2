package com.ds.eventwish.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.ds.eventwish.R;
import com.ds.eventwish.databinding.ItemGreetingBinding;
import java.util.ArrayList;
import java.util.List;

public class GreetingsAdapter extends RecyclerView.Adapter<GreetingsAdapter.GreetingViewHolder> {
    private List<GreetingItem> greetings;
    private List<GreetingItem> filteredGreetings;
    private String currentCategory = "";
    private String currentSearchQuery = "";
    private OnItemClickListener listener;

    public GreetingsAdapter() {
        this.greetings = new ArrayList<>();
        this.filteredGreetings = new ArrayList<>();
        addSampleData();
    }

    private void addSampleData() {
        greetings.add(new GreetingItem(1, "Happy Sunday", "Motivational", "https://example.com/sunday.jpg"));
        greetings.add(new GreetingItem(2, "Daily Bible Verse", "Bible Verses", "https://example.com/bible.jpg"));
        greetings.add(new GreetingItem(3, "Love Quote of the Day", "Love", "https://example.com/love.jpg"));
        greetings.add(new GreetingItem(4, "Special Birthday Wishes", "Day's Special", "https://example.com/birthday.jpg"));
        greetings.add(new GreetingItem(5, "Morning Motivation", "Motivational", "https://example.com/morning.jpg"));
        greetings.add(new GreetingItem(6, "Evening Prayer", "Bible Verses", "https://example.com/prayer.jpg"));
        applyFilters();
        filteredGreetings.addAll(greetings);
    }

    @NonNull
    @Override
    public GreetingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGreetingBinding binding = ItemGreetingBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new GreetingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GreetingViewHolder holder, int position) {
        holder.bind(filteredGreetings.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return filteredGreetings.size();
    }

    public void filter(String query) {
        this.currentSearchQuery = query.toLowerCase();
        applyFilters();
    }

    public void filterByCategory(String category) {
        this.currentCategory = category;
        applyFilters();
    }

    public void resetFilter() {
        this.currentCategory = "";
        this.currentSearchQuery = "";
        applyFilters();
    }

    private void applyFilters() {
        filteredGreetings.clear();
        
        for (GreetingItem greeting : greetings) {
            boolean matchesCategory = currentCategory.isEmpty() || 
                                   greeting.getCategory().equals(currentCategory);
            
            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                                  greeting.getTitle().toLowerCase().contains(currentSearchQuery) ||
                                  greeting.getCategory().toLowerCase().contains(currentSearchQuery);
            
            if (matchesCategory && matchesSearch) {
                filteredGreetings.add(greeting);
            }
        }
        
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(GreetingItem greeting);
    }

    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }    

    static class GreetingViewHolder extends RecyclerView.ViewHolder {
        private final ItemGreetingBinding binding;

        GreetingViewHolder(@NonNull ItemGreetingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }

        void bind(GreetingItem greeting, OnItemClickListener listener) {
            binding.greetingTitle.setText(greeting.getTitle());
            binding.greetingCategory.setText(greeting.getCategory());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(greeting);
                }
            });

            

            // // Load image with Glide
            Glide.with(binding.getRoot().getContext())
                .load(greeting.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .centerCrop()
                .into(binding.greetingImageView);

            // Set click listener
            binding.getRoot().setOnClickListener(v -> {
                android.os.Bundle args = new android.os.Bundle();
                args.putInt("greetingId", greeting.getId());
                Navigation.findNavController(v).navigate(
                    R.id.action_home_to_detail,
                    args
                );
            });
        }
        
    }
    
}
