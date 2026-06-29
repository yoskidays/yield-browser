package com.yieldbrowser.app;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Stateless UI primitives shared across the browser UI.
 *
 * <p>These helpers used to live as private instance methods on {@code MainActivity}. They are pure
 * builders that only need a {@link Context} for density/resource access, so moving them here lets
 * future extracted UI classes reuse them without holding a {@code MainActivity} reference. Behavior
 * is unchanged: {@code MainActivity} keeps thin wrappers that delegate to these methods, so every
 * existing call site continues to compile and behave exactly as before.</p>
 */
final class YieldUi {
    private YieldUi() {
        // Utility class.
    }

    /** Converts a dp value to integer pixels using the context's current display density. */
    static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * Builds a rounded-rectangle {@link GradientDrawable}. A stroke is only applied when
     * {@code strokeWidth > 0}, matching the original inline behavior.
     */
    static GradientDrawable roundRect(int fillColor, int radius, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    /** Creates a horizontal spacer view of the given pixel height that fills the parent width. */
    static View space(Context context, int height) {
        View v = new View(context);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, height));
        return v;
    }
}
