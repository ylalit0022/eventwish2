# Sponsored Ads Implementation Guide

This document provides instructions for implementing sponsored banner ads in the EventWish Android app.

## Overview

The sponsored ads system allows you to show banner advertisements below the category section in the app. The ads are served from your own backend, and clicks and impressions are tracked for analytics.

## Server-side Implementation (Already completed)

The backend implementation includes:

1. **SponsoredAd Model**: Stores ad information including image URL, redirect URL, display dates, and tracking stats.
2. **API Endpoints**:
   - `GET /api/sponsored-ads` - Retrieves active ads
   - `POST /api/sponsored-ads/viewed/:id` - Records an impression
   - `POST /api/sponsored-ads/clicked/:id` - Records a click
   - `GET /api/sponsored-ads/stats/:id` - Gets ad statistics (requires API key)

3. **Seeding**: Sample ads are included in the seed script.

## Android Implementation Guide

### 1. Create the SponsoredAd Model

Create a new class `SponsoredAd.java` in the `com.ds.eventwish.data.model` package:

```java
package com.ds.eventwish.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class SponsoredAd {
    @SerializedName("id")
    private String id;
    
    @SerializedName("image_url")
    private String imageUrl;
    
    @SerializedName("redirect_url")
    private String redirectUrl;
    
    @SerializedName("status")
    private boolean status;
    
    @SerializedName("start_date")
    private Date startDate;
    
    @SerializedName("end_date")
    private Date endDate;
    
    @SerializedName("location")
    private String location;
    
    @SerializedName("priority")
    private int priority;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("description")
    private String description;
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public String getRedirectUrl() {
        return redirectUrl;
    }
    
    public boolean isStatus() {
        return status;
    }
    
    public Date getStartDate() {
        return startDate;
    }
    
    public Date getEndDate() {
        return endDate;
    }
    
    public String getLocation() {
        return location;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
}
```

### 2. Create Response Wrapper

Create a class to handle the API response:

```java
package com.ds.eventwish.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SponsoredAdResponse {
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("ads")
    private List<SponsoredAd> ads;
    
    @SerializedName("message")
    private String message;
    
    public boolean isSuccess() {
        return success;
    }
    
    public List<SponsoredAd> getAds() {
        return ads;
    }
    
    public String getMessage() {
        return message;
    }
}
```

### 3. Update API Service

Add new endpoints to the `ApiService.java` interface:

```java
@GET("sponsored-ads")
Call<SponsoredAdResponse> getSponsoredAds(@Query("location") String location);

@POST("sponsored-ads/viewed/{id}")
Call<BaseResponse> recordAdImpression(@Path("id") String id, @Header("x-device-id") String deviceId);

@POST("sponsored-ads/clicked/{id}")
Call<BaseResponse> recordAdClick(@Path("id") String id, @Header("x-device-id") String deviceId);
```

### 4. Create SponsoredAdRepository

Create a repository class to manage ads:

```java
package com.ds.eventwish.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ds.eventwish.data.api.ApiClient;
import com.ds.eventwish.data.api.ApiService;
import com.ds.eventwish.data.model.BaseResponse;
import com.ds.eventwish.data.model.SponsoredAd;
import com.ds.eventwish.data.model.SponsoredAdResponse;
import com.ds.eventwish.utils.DeviceUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SponsoredAdRepository {
    private static final String TAG = "SponsoredAdRepository";
    private final ApiService apiService;
    private final MutableLiveData<List<SponsoredAd>> sponsoredAdsLiveData = new MutableLiveData<>();
    private final String deviceId;

    public SponsoredAdRepository(Context context) {
        apiService = ApiClient.getClient().create(ApiService.class);
        deviceId = DeviceUtils.getDeviceId(context);
    }

    public LiveData<List<SponsoredAd>> getSponsoredAds(String location) {
        fetchSponsoredAds(location);
        return sponsoredAdsLiveData;
    }

    private void fetchSponsoredAds(String location) {
        apiService.getSponsoredAds(location).enqueue(new Callback<SponsoredAdResponse>() {
            @Override
            public void onResponse(Call<SponsoredAdResponse> call, Response<SponsoredAdResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    sponsoredAdsLiveData.postValue(response.body().getAds());
                    Log.d(TAG, "Fetched " + response.body().getAds().size() + " sponsored ads");
                } else {
                    sponsoredAdsLiveData.postValue(new ArrayList<>());
                    Log.e(TAG, "Error fetching sponsored ads: " + (response.body() != null ? response.body().getMessage() : "Unknown error"));
                }
            }

            @Override
            public void onFailure(Call<SponsoredAdResponse> call, Throwable t) {
                sponsoredAdsLiveData.postValue(new ArrayList<>());
                Log.e(TAG, "Network error fetching sponsored ads", t);
            }
        });
    }

    public void recordImpression(String adId) {
        apiService.recordAdImpression(adId, deviceId).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                Log.d(TAG, "Ad impression recorded for ad: " + adId);
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                Log.e(TAG, "Failed to record ad impression", t);
            }
        });
    }

    public void recordClick(String adId) {
        apiService.recordAdClick(adId, deviceId).enqueue(new Callback<BaseResponse>() {
            @Override
            public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                Log.d(TAG, "Ad click recorded for ad: " + adId);
            }

            @Override
            public void onFailure(Call<BaseResponse> call, Throwable t) {
                Log.e(TAG, "Failed to record ad click", t);
            }
        });
    }
}
```

### 5. Create SponsoredAdView

Create a custom view to display sponsored ads:

```java
package com.ds.eventwish.ui.common;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.ds.eventwish.R;
import com.ds.eventwish.data.model.SponsoredAd;
import com.ds.eventwish.data.repository.SponsoredAdRepository;

public class SponsoredAdView extends FrameLayout {
    private ImageView adImageView;
    private TextView adTitleView;
    private SponsoredAdRepository repository;
    private SponsoredAd currentAd;

    public SponsoredAdView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SponsoredAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SponsoredAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_sponsored_ad, this, true);
        repository = new SponsoredAdRepository(context);
        
        adImageView = findViewById(R.id.ad_image);
        adTitleView = findViewById(R.id.ad_title);
        
        setVisibility(GONE); // Hide until we have an ad to show
    }

    public void loadAd(String location) {
        repository.getSponsoredAds(location).observe(
            (androidx.lifecycle.LifecycleOwner) getContext(),
            ads -> {
                if (ads != null && !ads.isEmpty()) {
                    // Show the first ad (highest priority)
                    displayAd(ads.get(0));
                } else {
                    setVisibility(GONE);
                }
            }
        );
    }

    private void displayAd(SponsoredAd ad) {
        currentAd = ad;
        
        // Load image with Glide
        Glide.with(getContext())
            .load(ad.getImageUrl())
            .into(adImageView);
            
        // Set title if available
        if (ad.getTitle() != null && !ad.getTitle().isEmpty()) {
            adTitleView.setText(ad.getTitle());
            adTitleView.setVisibility(VISIBLE);
        } else {
            adTitleView.setVisibility(GONE);
        }
        
        // Show the ad view
        setVisibility(VISIBLE);
        
        // Record impression
        repository.recordImpression(ad.getId());
        
        // Set click listener
        setOnClickListener(v -> {
            try {
                // Record click
                repository.recordClick(ad.getId());
                
                // Open browser with redirect URL
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(ad.getRedirectUrl()));
                getContext().startActivity(browserIntent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
```

### 6. Create Layout for SponsoredAdView

Create a new layout file `res/layout/view_sponsored_ad.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<merge xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <androidx.cardview.widget.CardView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_margin="8dp"
        app:cardCornerRadius="8dp"
        app:cardElevation="2dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <TextView
                android:id="@+id/sponsored_label"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="end"
                android:layout_margin="4dp"
                android:text="Sponsored"
                android:textColor="@android:color/darker_gray"
                android:textSize="10sp" />

            <ImageView
                android:id="@+id/ad_image"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:adjustViewBounds="true"
                android:scaleType="fitCenter" />

            <TextView
                android:id="@+id/ad_title"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_margin="8dp"
                android:gravity="center"
                android:textColor="@android:color/black"
                android:textSize="14sp"
                android:textStyle="bold"
                android:visibility="gone"
                tools:text="Special Offer"
                tools:visibility="visible" />

        </LinearLayout>
    </androidx.cardview.widget.CardView>
</merge>
```

### 7. Add the SponsoredAdView Below the Category Section

In your `fragment_home.xml` layout (or wherever your category section is), add:

```xml
<!-- Add this below your category RecyclerView -->
<com.ds.eventwish.ui.common.SponsoredAdView
    android:id="@+id/sponsored_ad_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:layout_marginBottom="8dp" />
```

### 8. Initialize the Ad in Your Fragment

In your fragment:

```java
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    
    // Initialize other UI components
    
    // Load sponsored ad below categories
    SponsoredAdView sponsoredAdView = view.findViewById(R.id.sponsored_ad_view);
    sponsoredAdView.loadAd("category_below");
}
```

## Testing

1. Run the backend seed script to populate sample ads:
   ```
   node scripts/seedSponsoredAds.js
   ```

2. Verify the API endpoints work by making requests to:
   - GET http://localhost:3000/api/sponsored-ads
   - POST http://localhost:3000/api/sponsored-ads/viewed/:id
   - POST http://localhost:3000/api/sponsored-ads/clicked/:id

3. Implement the Android client code and ensure ads appear below the category section.

4. Test impression and click tracking by checking the MongoDB database.

## Ad Management

To manage ads (create, update, disable), currently you need to directly modify the database. 

In the future, a web admin interface could be implemented to manage these ads more easily.

## Troubleshooting

1. **No ads showing**: Verify that there are active ads in the database with current dates and `status: true`.

2. **Images not loading**: Ensure the image URLs are accessible from the device and properly stored in the database.

3. **Clicks not tracking**: Check for network issues and ensure the API endpoint is properly implemented.

4. **Performance issues**: If loading ads slows down the UI, consider loading them asynchronously or implementing caching. 