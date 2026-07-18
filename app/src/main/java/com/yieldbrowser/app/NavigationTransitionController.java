package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_ACCENT;
import static com.yieldbrowser.app.BrowserConstants.COLOR_BG;
import static com.yieldbrowser.app.BrowserConstants.COLOR_SUBTEXT;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Owns the search-to-page loading overlay and fade transition state. */
final class NavigationTransitionController {
    private final FrameLayout overlay;
    private final View home;
    private final WebView webView;
    private boolean active;

    NavigationTransitionController(FrameLayout overlay, View home, WebView webView) {
        this.overlay = overlay;
        this.home = home;
        this.webView = webView;
    }

    static FrameLayout createOverlay(Activity activity) {
        FrameLayout overlay = new FrameLayout(activity);
        overlay.setBackgroundColor(COLOR_BG);
        overlay.setAlpha(0f);

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(activity, 24), dp(activity, 24), dp(activity, 24), dp(activity, 24));

        ProgressBar spinner = new ProgressBar(activity);
        try {
            spinner.setIndeterminateTintList(ColorStateList.valueOf(COLOR_ACCENT));
        } catch (Exception ignored) {
        }
        box.addView(spinner, new LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 42)));

        TextView label = new TextView(activity);
        label.setText("Memuat halaman...");
        label.setTextColor(COLOR_SUBTEXT);
        label.setTextSize(14);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-2, -2);
        labelParams.setMargins(0, dp(activity, 14), 0, 0);
        box.addView(label, labelParams);

        overlay.addView(box, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        return overlay;
    }

    void start() {
        active = true;
        try {
            if (overlay != null) {
                overlay.bringToFront();
                overlay.setVisibility(View.VISIBLE);
                overlay.setAlpha(1f);
            }
            if (home != null) home.setVisibility(View.GONE);
            if (webView != null) {
                webView.setAlpha(0f);
                webView.setVisibility(View.VISIBLE);
            }
        } catch (Exception ignored) {
        }
    }

    void finish() {
        if (!active) return;
        active = false;
        try {
            if (webView != null) {
                webView.setVisibility(View.VISIBLE);
                webView.animate().alpha(1f).setDuration(180).start();
            }
            if (overlay != null) {
                overlay.animate()
                        .alpha(0f)
                        .setDuration(180)
                        .withEndAction(() -> {
                            try {
                                overlay.setVisibility(View.GONE);
                                overlay.setAlpha(0f);
                            } catch (Exception ignored) {
                            }
                        })
                        .start();
            }
        } catch (Exception ignored) {
            if (overlay != null) overlay.setVisibility(View.GONE);
            if (webView != null) webView.setAlpha(1f);
        }
    }

    void cancel() {
        active = false;
        try {
            if (overlay != null) {
                overlay.setVisibility(View.GONE);
                overlay.setAlpha(0f);
            }
            if (webView != null) webView.setAlpha(1f);
        } catch (Exception ignored) {
        }
    }

    boolean isActive() {
        return active;
    }

    private static int dp(Activity activity, int value) {
        return YieldUi.dp(activity, value);
    }
}
