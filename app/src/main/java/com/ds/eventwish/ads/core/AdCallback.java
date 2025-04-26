package com.ds.eventwish.ads.core;

import com.ds.eventwish.data.model.ads.AdUnit;

import java.util.List;

/**
 * Interface for handling ad-related events.
 */
public interface AdCallback {
    /**
     * Called when ad units are successfully fetched.
     * @param adUnits List of ad units
     */
    void onSuccess(List<AdUnit> adUnits);

    /**
     * Called when there is an error fetching ad units.
     * @param error Error message
     */
    void onError(String error);

    /**
     * Called when an ad is loaded successfully.
     * @param adUnit The ad unit that was loaded
     */
    default void onAdLoaded(AdUnit adUnit) {}

    /**
     * Called when an ad fails to load.
     * @param adUnit The ad unit that failed to load
     * @param error Error message
     */
    default void onAdFailedToLoad(AdUnit adUnit, String error) {}

    /**
     * Called when an ad is shown to the user.
     * @param adUnit The ad unit that was shown
     */
    default void onAdShown(AdUnit adUnit) {}

    /**
     * Called when an ad is clicked.
     * @param adUnit The ad unit that was clicked
     */
    default void onAdClicked(AdUnit adUnit) {}

    /**
     * Called when an ad is closed by the user.
     * @param adUnit The ad unit that was closed
     */
    default void onAdClosed(AdUnit adUnit) {}

    /**
     * Called when an ad impression is recorded.
     * @param adUnit The ad unit that recorded an impression
     */
    default void onAdImpression(AdUnit adUnit) {}

    /**
     * Called when revenue is earned from an ad.
     * @param adUnit The ad unit that earned revenue
     * @param revenue The amount of revenue earned
     */
    default void onAdRevenue(AdUnit adUnit, double revenue) {}
} 