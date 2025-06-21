package com.ds.eventwish.ui.ads;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.SponsoredAd;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying sponsored ads in a ViewPager2
 */
public class SponsoredAdPagerAdapter extends RecyclerView.Adapter<SponsoredAdPagerAdapter.AdViewHolder> {
    
    private final List<SponsoredAd> adsList = new ArrayList<>();
    private final Context context;
    private OnAdClickListener clickListener;
    
    /**
     * Interface for handling ad clicks
     */
    public interface OnAdClickListener {
        void onAdClicked(SponsoredAd ad);
    }
    
    public SponsoredAdPagerAdapter(Context context) {
        this.context = context;
    }
    
    /**
     * Set click listener for ads
     * @param listener The click listener implementation
     */
    public void setOnAdClickListener(OnAdClickListener listener) {
        this.clickListener = listener;
    }
    
    /**
     * Replace all ads with new list
     * @param newAdsList List of ads to display
     */
    public void setAds(List<SponsoredAd> newAdsList) {
        adsList.clear();
        if (newAdsList != null) {
            adsList.addAll(newAdsList);
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public AdViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_sponsored_ad, parent, false);
        
        // Pages in ViewPager2 must use match_parent for both dimensions
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp != null) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            view.setLayoutParams(lp);
        } else {
            view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }
        
        return new AdViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AdViewHolder holder, int position) {
        holder.bind(adsList.get(position));
    }
    
    @Override
    public int getItemCount() {
        return adsList.size();
    }
    
    /**
     * Get ad at specific position
     * @param position Position in the adapter
     * @return SponsoredAd at position or null if position is invalid
     */
    public SponsoredAd getAdAt(int position) {
        if (position >= 0 && position < adsList.size()) {
            return adsList.get(position);
        }
        return null;
    }
    
    /**
     * ViewHolder for sponsored ads
     */
    class AdViewHolder extends RecyclerView.ViewHolder {
        private final ImageView adImage;
        private final TextView adTitle;
        private final TextView adDescription;
        private final CardView container;
        
        AdViewHolder(View itemView) {
            super(itemView);
            adImage = itemView.findViewById(R.id.sponsored_ad_image);
            adTitle = itemView.findViewById(R.id.sponsored_ad_title);
            adDescription = itemView.findViewById(R.id.sponsored_ad_description);
            container = itemView.findViewById(R.id.sponsored_ad_container);
        }
        
        void bind(SponsoredAd ad) {
            if (ad == null) return;
            
            // Set title and description
            adTitle.setText(ad.getTitle());
            adDescription.setText(ad.getDescription());
            
            // Load image with Glide
            if (ad.getImageUrl() != null && !ad.getImageUrl().isEmpty()) {
                Glide.with(context)
                     .load(ad.getImageUrl())
                     .apply(new RequestOptions()
                             .placeholder(R.drawable.placeholder_image)
                             .error(R.drawable.error_image))
                     .into(adImage);
            }
            
            // Set click listener
            container.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAdClicked(ad);
                }
            });
        }
    }
} 