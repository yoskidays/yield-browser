package com.yieldbrowser.app;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

import java.util.function.BooleanSupplier;

/** Owns edge-swipe gesture state and applies the existing tab navigation policy. */
final class SwipeNavigationController {
    private final Activity activity;
    private final View home;
    private final WebView webView;
    private final BooleanSupplier horizontalProtection;
    private final Runnable restoreHiddenPage;
    private final Runnable showHome;
    private float startX;
    private float startY;
    private long startTime;

    SwipeNavigationController(Activity activity,
                              View home,
                              WebView webView,
                              BooleanSupplier horizontalProtection,
                              Runnable restoreHiddenPage,
                              Runnable showHome) {
        this.activity = activity;
        this.home = home;
        this.webView = webView;
        this.horizontalProtection = horizontalProtection;
        this.restoreHiddenPage = restoreHiddenPage;
        this.showHome = showHome;
    }

    void install(View root) {
        View.OnTouchListener listener = (view, event) -> handle(event);
        if (root != null) root.setOnTouchListener(listener);
        if (home != null) home.setOnTouchListener(listener);
        if (webView != null) webView.setOnTouchListener(listener);
    }

    boolean handle(MotionEvent event) {
        if (event == null) return false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startX = event.getX();
            startY = event.getY();
            startTime = System.currentTimeMillis();
            return false;
        }
        if (event.getAction() != MotionEvent.ACTION_UP) return false;

        float dx = event.getX() - startX;
        float dy = event.getY() - startY;
        long duration = System.currentTimeMillis() - startTime;
        if (!isEligibleGesture(dx, dy, duration, dp(90), dp(120))) return false;

        boolean protectedHorizontal = horizontalProtection != null
                && horizontalProtection.getAsBoolean();
        int screenWidth = activity != null
                && activity.getResources() != null
                && activity.getResources().getDisplayMetrics() != null
                ? activity.getResources().getDisplayMetrics().widthPixels : 0;
        int edgeLimit = protectedHorizontal ? dp(16) : dp(30);
        boolean fromLeftEdge = startX <= edgeLimit;
        boolean fromRightEdge = screenWidth > 0 && startX >= screenWidth - edgeLimit;
        if (!fromLeftEdge && !fromRightEdge) return false;

        if (dx < 0) {
            if (fromRightEdge) navigateBack();
        } else if (fromLeftEdge) {
            navigateForward();
        }
        return false;
    }

    static boolean isEligibleGesture(float dx,
                                     float dy,
                                     long duration,
                                     int minHorizontal,
                                     int maxVertical) {
        return duration <= 900
                && Math.abs(dx) >= minHorizontal
                && Math.abs(dy) <= maxVertical;
    }

    private void navigateBack() {
        TabNavigationPolicy.BackAction action = TabNavigationPolicy.backAction(
                home != null && home.getVisibility() == View.VISIBLE,
                webView != null && webView.getVisibility() == View.VISIBLE,
                webView != null && webView.canGoBack());
        if (action == TabNavigationPolicy.BackAction.RESTORE_PAGE) {
            run(restoreHiddenPage);
        } else if (action == TabNavigationPolicy.BackAction.WEB_BACK) {
            if (webView != null) webView.goBack();
        } else {
            run(showHome);
        }
    }

    private void navigateForward() {
        TabNavigationPolicy.ForwardAction action = TabNavigationPolicy.forwardAction(
                home != null && home.getVisibility() == View.VISIBLE,
                webView != null && webView.getVisibility() == View.VISIBLE,
                webView != null && webView.canGoForward());
        if (action == TabNavigationPolicy.ForwardAction.WEB_FORWARD) {
            if (webView != null) webView.goForward();
        } else if (action == TabNavigationPolicy.ForwardAction.RESTORE_AND_FORWARD) {
            run(restoreHiddenPage);
            if (webView != null) webView.goForward();
        } else if (action == TabNavigationPolicy.ForwardAction.RESTORE_PAGE) {
            run(restoreHiddenPage);
        }
    }

    private void run(Runnable action) {
        if (action != null) action.run();
    }

    private int dp(int value) {
        return YieldUi.dp(activity, value);
    }
}
