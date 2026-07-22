package com.yieldbrowser.app;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

import org.json.JSONTokener;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

/** Owns edge-swipe gesture state and applies the existing tab navigation policy. */
final class SwipeNavigationController {
    private static final long LONG_PRESS_LINK_CACHE_MAX_AGE_MS = 2_000L;

    private final Activity activity;
    private final View home;
    private final WebView webView;
    private final BooleanSupplier horizontalProtection;
    private final Runnable restoreHiddenPage;
    private final Runnable showHome;
    private final Set<WebView> boundWebViews =
            Collections.newSetFromMap(new WeakHashMap<>());
    private float startX;
    private float startY;
    private long startTime;
    private WebView gestureWebView;
    private WebView lastTouchedWebView;
    private long lastWebTouchAt;
    private int linkResolveToken;
    private String cachedLongPressLink = "";

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
        View.OnTouchListener listener = this::handle;
        if (root != null) root.setOnTouchListener(listener);
        if (home != null) home.setOnTouchListener(listener);
        bindWebView(webView);
    }

    void bindWebView(WebView candidate) {
        if (candidate == null) return;
        if (!boundWebViews.add(candidate)) return;

        candidate.setLongClickable(true);
        candidate.setHapticFeedbackEnabled(true);
        candidate.setOnTouchListener((view, event) -> {
            cacheLinkUnderTouch(candidate, event);
            return handle(view, event);
        });
        candidate.setOnLongClickListener(view -> handleWebViewLongPress(candidate));
    }

    private void cacheLinkUnderTouch(WebView candidate, MotionEvent event) {
        if (candidate == null || event == null
                || event.getAction() != MotionEvent.ACTION_DOWN) {
            return;
        }

        lastTouchedWebView = candidate;
        lastWebTouchAt = System.currentTimeMillis();
        cachedLongPressLink = "";
        final int resolveToken = ++linkResolveToken;
        final String pageUrl = candidate.getUrl();

        float scale = 1f;
        try {
            scale = candidate.getScale();
        } catch (RuntimeException ignored) {
        }
        if (scale <= 0f) scale = 1f;

        float cssX = event.getX() / scale;
        float cssY = event.getY() / scale;
        String script = buildLinkLookupScript(cssX, cssY);

        try {
            candidate.evaluateJavascript(script, value -> {
                if (resolveToken != linkResolveToken
                        || candidate != lastTouchedWebView
                        || value == null) {
                    return;
                }
                String href = decodeJavascriptString(value);
                String resolved = LongPressLinkPolicy.resolveHttpUrl(href, pageUrl);
                cachedLongPressLink = resolved == null ? "" : resolved;
            });
        } catch (RuntimeException ignored) {
            cachedLongPressLink = "";
        }
    }

    private boolean handleWebViewLongPress(WebView candidate) {
        if (!(activity instanceof YieldWebRuntimeActivity) || candidate == null) return false;
        YieldWebRuntimeActivity host = (YieldWebRuntimeActivity) activity;

        // Keep the fast native HitTestResult path for ordinary anchors.
        if (host.handleLongPressedLink(candidate)) return true;

        // Ad-heavy pages often wrap links in spans, images, or transparent layers.
        // Use the DOM anchor cached from ACTION_DOWN without weakening the ad shield.
        if (candidate != lastTouchedWebView
                || cachedLongPressLink.length() == 0
                || System.currentTimeMillis() - lastWebTouchAt
                > LONG_PRESS_LINK_CACHE_MAX_AGE_MS) {
            return false;
        }

        host.showLongPressedLinkMenu(
                candidate, cachedLongPressLink, null, candidate.getUrl());
        return true;
    }

    static String buildLinkLookupScript(float cssX, float cssY) {
        return "(function(){try{"
                + "var n=document.elementFromPoint(" + Float.toString(cssX) + ","
                + Float.toString(cssY) + ");"
                + "var guard=0;"
                + "while(n&&guard++<16){"
                + "if(n.nodeType===1){"
                + "if(n.tagName==='A'&&n.href)return n.href;"
                + "if(n.closest){var a=n.closest('a[href]');if(a&&a.href)return a.href;}"
                + "}"
                + "var r=n.getRootNode?n.getRootNode():null;"
                + "n=n.parentElement||(r&&r.host?r.host:null);"
                + "}"
                + "}catch(e){}return '';})()";
    }

    static String decodeJavascriptString(String value) {
        if (value == null || "null".equals(value)) return "";
        try {
            Object decoded = new JSONTokener(value).nextValue();
            return decoded instanceof String ? ((String) decoded).trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    boolean handle(MotionEvent event) {
        return handle(null, event);
    }

    boolean handle(View source, MotionEvent event) {
        if (event == null) return false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startX = event.getX();
            startY = event.getY();
            startTime = System.currentTimeMillis();
            gestureWebView = source instanceof WebView
                    ? (WebView) source : currentWebView();
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
            if (fromRightEdge) navigateBack(gestureWebView);
        } else if (fromLeftEdge) {
            navigateForward(gestureWebView);
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

    private WebView currentWebView() {
        if (activity instanceof YieldActivityState) {
            WebView current = ((YieldActivityState) activity).webView;
            if (current != null) return current;
        }
        return webView;
    }

    private void navigateBack(WebView target) {
        WebView active = target == null ? currentWebView() : target;
        TabNavigationPolicy.BackAction action = TabNavigationPolicy.backAction(
                home != null && home.getVisibility() == View.VISIBLE,
                active != null && active.getVisibility() == View.VISIBLE,
                active != null && active.canGoBack());
        if (action == TabNavigationPolicy.BackAction.RESTORE_PAGE) {
            run(restoreHiddenPage);
        } else if (action == TabNavigationPolicy.BackAction.WEB_BACK) {
            if (active != null) active.goBack();
        } else {
            run(showHome);
        }
    }

    private void navigateForward(WebView target) {
        WebView active = target == null ? currentWebView() : target;
        TabNavigationPolicy.ForwardAction action = TabNavigationPolicy.forwardAction(
                home != null && home.getVisibility() == View.VISIBLE,
                active != null && active.getVisibility() == View.VISIBLE,
                active != null && active.canGoForward());
        if (action == TabNavigationPolicy.ForwardAction.WEB_FORWARD) {
            if (active != null) active.goForward();
        } else if (action == TabNavigationPolicy.ForwardAction.RESTORE_AND_FORWARD) {
            run(restoreHiddenPage);
            if (active != null) active.goForward();
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
