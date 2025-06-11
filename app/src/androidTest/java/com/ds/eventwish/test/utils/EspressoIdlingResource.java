package com.ds.eventwish.test.utils;

import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

/**
 * Utility class for Espresso testing of asynchronous operations
 */
public class EspressoIdlingResource {
    private static final String RESOURCE = "GLOBAL";
    private static final CountingIdlingResource idlingResource = new CountingIdlingResource(RESOURCE);

    /**
     * Increment the counter of pending operations
     */
    public static void increment() {
        idlingResource.increment();
    }

    /**
     * Decrement the counter of pending operations
     * Call this when your async operation completes
     */
    public static void decrement() {
        if (!idlingResource.isIdleNow()) {
            idlingResource.decrement();
        }
    }

    /**
     * Get the IdlingResource instance
     */
    public static IdlingResource getIdlingResource() {
        return idlingResource;
    }
} 