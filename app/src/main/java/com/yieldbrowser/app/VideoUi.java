package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_BORDER;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Stateless builders and formatters for the in-page video controls UI.
 *
 * <p>These used to be private instance methods on {@code MainActivity}. They are pure: they only
 * need a {@link Context} plus the caller-supplied arguments (including any
 * {@link View.OnClickListener}), and never read or write {@code MainActivity} state. The one prior
 * side effect of {@code videoButton} (capturing the Play/Pause icon into a field) stays on the
 * {@code MainActivity} side, in the thin wrapper that calls this builder, so behavior is unchanged.</p>
 */
final class VideoUi {
    private VideoUi() {
        // Utility class.
    }

    static View videoTextButton(Context context, String text, String label, View.OnClickListener listener) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(11);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(YieldUi.roundRect(Color.parseColor("#20232A"), YieldUi.dp(context, 18), YieldUi.dp(context, 1), COLOR_BORDER));
        button.setOnClickListener(listener);
        button.setContentDescription(label);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(YieldUi.dp(context, 48), YieldUi.dp(context, 48));
        params.setMargins(YieldUi.dp(context, 3), 0, YieldUi.dp(context, 3), 0);
        button.setLayoutParams(params);
        return button;
    }

    /**
     * Builds the icon button. The Play/Pause icon is the first (and only) child of the returned
     * wrap; the caller captures it when {@code label} is "Play/Pause".
     */
    static LinearLayout videoButton(Context context, int iconRes, String label, View.OnClickListener listener) {
        LinearLayout wrap = new LinearLayout(context);
        wrap.setGravity(Gravity.CENTER);
        wrap.setBackground(YieldUi.roundRect(Color.parseColor("#20232A"), YieldUi.dp(context, 18), YieldUi.dp(context, 1), COLOR_BORDER));
        wrap.setOnClickListener(listener);
        wrap.setContentDescription(label);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE);
        wrap.addView(icon, new LinearLayout.LayoutParams(YieldUi.dp(context, 24), YieldUi.dp(context, 24)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(YieldUi.dp(context, 48), YieldUi.dp(context, 48));
        params.setMargins(YieldUi.dp(context, 3), 0, YieldUi.dp(context, 3), 0);
        wrap.setLayoutParams(params);
        return wrap;
    }

    static String formatVideoSpeed(float speed) {
        if (Math.abs(speed - 1.0f) < 0.01f) return "1x";
        if (Math.abs(speed - 2.0f) < 0.01f) return "2x";
        if (Math.abs(speed - 0.5f) < 0.01f) return "0.5x";
        if (Math.abs(speed - 1.25f) < 0.01f) return "1.25x";
        if (Math.abs(speed - 1.5f) < 0.01f) return "1.5x";
        return speed + "x";
    }
}
