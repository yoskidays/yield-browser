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
    private static final String SELECTABLE_TEXT_MARKER = "__YIELD_SELECT_TEXT__";

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
    private boolean cachedLongPressSelectableText;

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
        cachedLongPressSelectableText = false;
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
                String result = decodeJavascriptString(value);
                if (SELECTABLE_TEXT_MARKER.equals(result)) {
                    cachedLongPressLink = "";
                    cachedLongPressSelectableText = true;
                    return;
                }
                String resolved = LongPressLinkPolicy.resolveHttpUrl(result, pageUrl);
                cachedLongPressLink = resolved == null ? "" : resolved;
                cachedLongPressSelectableText = false;
            });
        } catch (RuntimeException ignored) {
            cachedLongPressLink = "";
            cachedLongPressSelectableText = false;
        }
    }

    private boolean handleWebViewLongPress(WebView candidate) {
        if (!(activity instanceof YieldWebRuntimeActivity) || candidate == null) return false;
        YieldWebRuntimeActivity host = (YieldWebRuntimeActivity) activity;
        String pageUrl = candidate.getUrl();
        boolean recentTouch = candidate == lastTouchedWebView
                && System.currentTimeMillis() - lastWebTouchAt
                <= LONG_PRESS_LINK_CACHE_MAX_AGE_MS;

        // Resolve from ACTION_DOWN first. This path can look through transparent advertising
        // overlays and avoids treating the overlay anchor as the user's intended link.
        if (recentTouch && cachedLongPressLink.length() > 0) {
            if (shouldBlockLongPressTarget(host, cachedLongPressLink, pageUrl)) {
                QuietToast.makeText(activity, "Link iklan diblokir",
                        QuietToast.LENGTH_SHORT).show();
                return true;
            }

            host.showLongPressedLinkMenu(
                    candidate, cachedLongPressLink, null, pageUrl);
            return true;
        }

        // Keep ordinary article text owned by WebView so Android can show its native selection
        // handles and Copy/Select all/Share action mode.
        if (recentTouch && cachedLongPressSelectableText) {
            return false;
        }

        // On shielded/ad-heavy pages, only consume an unresolved non-text long press. This keeps
        // hidden-overlay protection without stealing native selection from actual text.
        if (host.adBlock && isShieldedLongPressPage(host, pageUrl)) {
            return true;
        }

        // Keep the native HitTestResult path as a fallback for ordinary sites.
        return host.handleLongPressedLink(candidate);
    }

    private static boolean shouldBlockLongPressTarget(YieldWebRuntimeActivity host,
                                                      String targetUrl,
                                                      String pageUrl) {
        if (host == null || targetUrl == null || targetUrl.length() == 0) return true;

        boolean unsafe = host.isUnsafeUrl(targetUrl);
        if (host.isTrustedDownloadIntentUrl(targetUrl)) {
            return shouldBlockResolvedLongPressLink(
                    host.safeMode, unsafe, host.adBlock,
                    true, false, false);
        }

        boolean highConfidenceAd = host.isKnownPopupHost(targetUrl)
                || host.isLikelyAdClickUrl(targetUrl)
                || host.isAdUrl(targetUrl)
                || host.isSuspiciousPopupNavigation(targetUrl, pageUrl);
        boolean shieldBlocked = host.shouldShieldBlockMainFrame(
                targetUrl, pageUrl, true, highConfidenceAd);
        if (isShieldedLongPressPage(host, pageUrl)) {
            shieldBlocked = shieldBlocked
                    || host.isCompatibilityAdNavigation(targetUrl, pageUrl, true);
        }

        return shouldBlockResolvedLongPressLink(
                host.safeMode, unsafe, host.adBlock,
                false, highConfidenceAd, shieldBlocked);
    }

    static boolean shouldBlockResolvedLongPressLink(boolean safeMode,
                                                     boolean unsafe,
                                                     boolean adBlock,
                                                     boolean trustedDownload,
                                                     boolean highConfidenceAd,
                                                     boolean shieldBlocked) {
        if (safeMode && unsafe) return true;
        if (trustedDownload) return false;
        return adBlock && (highConfidenceAd || shieldBlocked);
    }

    private static boolean isShieldedLongPressPage(YieldWebRuntimeActivity host,
                                                    String pageUrl) {
        if (host == null) return false;
        return host.isShieldReaderOrCompatibilityContext(pageUrl)
                || host.isStrictSiteCompatibilityUrl(pageUrl)
                || host.isSiteCompatibilityModeActiveForUrl(pageUrl)
                || host.isReloadLoopGuardActiveForUrl(pageUrl);
    }

    static String buildLinkLookupScript(float cssX, float cssY) {
        String x = Float.toString(cssX);
        String y = Float.toString(cssY);
        return "(function(){try{"
                + "var px=" + x + ",py=" + y + ",textHit=false;"
                + "try{var cr=document.caretRangeFromPoint?document.caretRangeFromPoint(px,py):null;"
                + "var tn=cr&&cr.startContainer,te=tn&&tn.nodeType===3?tn.parentElement:null;"
                + "var interactive=te&&te.closest?te.closest('a[href],button,input,textarea,select,[role=button],[role=link],[onclick]'):null;"
                + "var ts=te&&window.getComputedStyle?getComputedStyle(te):null;"
                + "if(tn&&tn.nodeType===3&&String(tn.nodeValue||'').trim().length>0&&!interactive"
                + "&&(!ts||(ts.userSelect!=='none'&&ts.webkitUserSelect!=='none')))textHit=true;}catch(caretError){}"
                + "var list=document.elementsFromPoint?document.elementsFromPoint(px,py)"
                + ":[document.elementFromPoint(px,py)];"
                + "var seen=[];"
                + "for(var i=0;i<list.length;i++){"
                + "var n=list[i],guard=0;"
                + "while(n&&guard++<16){"
                + "if(n.nodeType===1){"
                + "var a=n.tagName==='A'&&n.href?n:(n.closest?n.closest('a[href]'):null);"
                + "if(a&&a.href&&seen.indexOf(a)<0){"
                + "seen.push(a);"
                + "var s=window.getComputedStyle?getComputedStyle(a):null;"
                + "var r=a.getBoundingClientRect?a.getBoundingClientRect():null;"
                + "var area=r?Math.max(0,r.width)*Math.max(0,r.height):0;"
                + "var viewport=Math.max(1,innerWidth*innerHeight);"
                + "var text=((a.innerText||a.getAttribute('aria-label')||'')+'').trim();"
                + "var media=!!(a.querySelector&&a.querySelector('img,video,picture,svg'));"
                + "var pos=s?s.position:'';"
                + "var opacity=s?parseFloat(s.opacity||'1'):1;"
                + "var overlay=(pos==='fixed'||pos==='absolute')&&area>viewport*0.35"
                + "&&text.length===0&&!media;"
                + "if(!overlay&&opacity>=0.08&&(!s||s.pointerEvents!=='none'))return a.href;"
                + "}"
                + "}"
                + "var root=n.getRootNode?n.getRootNode():null;"
                + "n=n.parentElement||(root&&root.host?root.host:null);"
                + "}"
                + "}"
                + "if(textHit)return '" + SELECTABLE_TEXT_MARKER + "';"
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
