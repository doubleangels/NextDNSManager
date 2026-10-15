package com.doubleangels.nextdnsmanagement.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

/**
 * Optimized image loading utility with memory management and caching.
 * Provides efficient image loading with proper memory cleanup and recycling.
 */
public class ImageLoader {

    /**
     * Loads a drawable resource into an ImageView.
     * Resource drawables are loaded synchronously on the calling thread since
     * {@link ContextCompat#getDrawable} is fast (no I/O) and does not need a background thread.
     * Using AsyncTask (now removed in API 34+) or Handler here would add overhead for no benefit.
     *
     * @param context       The context for resource access.
     * @param imageView     The ImageView to load the image into.
     * @param drawableResId The drawable resource ID to load.
     */
    public static void loadDrawable(@NonNull Context context, @NonNull ImageView imageView, int drawableResId) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, drawableResId);
            if (drawable != null) {
                imageView.setImageDrawable(drawable);
            }
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }
    }

}
