package com.yieldbrowser.app;

import android.view.MotionEvent;
import android.view.View;

/** Owns edge-swipe gesture state and applies the existing tab navigation policy. */
final class SwipeNavigationController {
    interface Host {
        int dp(int value);

        int screenWidth();

        boolean shouldProtectHorizontalSwipe();

        boolean homeVisible();

        boolean webVisible();

        boolean canGoBack();

        boolean canGoForward();

        void restoreHiddenPage();

        void goBack();

        void goForward();

        void showHome();
    }

    private final Host host;
    private float startX;
    private float startY;
    private long startTime;

    SwipeNavigationController(Host host) {
        this.host = host;
    }

    void install(View root, View home, View web) {
        View.OnTouchListener listener = (view, event) -> handle(event);
        if (root != null) root.setOnTouchListener(listener);
        if (home != null) home.setOnTouchListener(listener);
        if (web != null) web.setOnTouchListener(listener);
    }

    boolean handle(MotionEvent event) {
        if (event == null || host == null) return false;
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
        if (!isEligibleGesture(dx, dy, duration, host.dp(90), host.dp(120))) return false;

        boolean protectedHorizontal = host.shouldProtectHorizontalSwipe();
        int screenWidth = host.screenWidth();
        int edgeLimit = protectedHorizontal ? host.dp(16) : host.dp(30);
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
                host.homeVisible(), host.webVisible(), host.canGoBack());
        if (action == TabNavigationPolicy.BackAction.RESTORE_PAGE) {
            host.restoreHiddenPage();
        } else if (action == TabNavigationPolicy.BackAction.WEB_BACK) {
            host.goBack();
        } else {
            host.showHome();
        }
    }

    private void navigateForward() {
        TabNavigationPolicy.ForwardAction action = TabNavigationPolicy.forwardAction(
                host.homeVisible(), host.webVisible(), host.canGoForward());
        if (action == TabNavigationPolicy.ForwardAction.WEB_FORWARD) {
            host.goForward();
        } else if (action == TabNavigationPolicy.ForwardAction.RESTORE_AND_FORWARD) {
            host.restoreHiddenPage();
            host.goForward();
        } else if (action == TabNavigationPolicy.ForwardAction.RESTORE_PAGE) {
            host.restoreHiddenPage();
        }
    }
}
