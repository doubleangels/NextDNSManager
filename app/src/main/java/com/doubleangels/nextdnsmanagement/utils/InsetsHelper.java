package com.doubleangels.nextdnsmanagement.utils;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Applies window inset padding for edge-to-edge layouts.
 */
public final class InsetsHelper {

    private static final int SYSTEM_BARS_AND_CUTOUT = WindowInsetsCompat.Type.systemBars()
            | WindowInsetsCompat.Type.displayCutout();
    private static final int SYSTEM_BARS = WindowInsetsCompat.Type.systemBars();

    private InsetsHelper() {
    }

    public static void installOnRoot(View root) {
        if (root instanceof ViewGroup) {
            ViewGroupCompat.installCompatInsetsDispatch((ViewGroup) root);
        }
    }

    public static void applyStatusBarScrimHeight(View scrim) {
        ViewCompat.setOnApplyWindowInsetsListener(scrim, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(SYSTEM_BARS_AND_CUTOUT);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = bars.top;
            view.setLayoutParams(layoutParams);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(scrim);
    }

    public static void applyToolbarTopInsets(View toolbar) {
        final int actionBarHeight = toolbar.getResources().getDimensionPixelSize(
                androidx.appcompat.R.dimen.abc_action_bar_default_height_material);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(SYSTEM_BARS_AND_CUTOUT);
            view.setPadding(view.getPaddingLeft(), bars.top, view.getPaddingRight(), view.getPaddingBottom());
            view.setMinimumHeight(bars.top + actionBarHeight);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(toolbar);
    }

    public static void applySystemBarPadding(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(SYSTEM_BARS_AND_CUTOUT);
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(view);
    }

    public static void applyBottomSystemBarPadding(View view) {
        final int initialLeft = view.getPaddingLeft();
        final int initialTop = view.getPaddingTop();
        final int initialRight = view.getPaddingRight();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(SYSTEM_BARS);
            v.setPadding(initialLeft, initialTop, initialRight, bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
