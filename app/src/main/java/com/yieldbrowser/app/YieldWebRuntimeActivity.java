package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.*;
import static com.yieldbrowser.app.StorageCodec.decode;
import static com.yieldbrowser.app.StorageCodec.encode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.LruCache;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.webkit.ScriptHandler;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

abstract class YieldWebRuntimeActivity extends YieldDownloadActivity {

boolean handleLongPressedLink(WebView sourceView) {
        if (elementPickerActive) return false;
        if (sourceView == null || sourceView != webView
                || sourceView.getVisibility() != View.VISIBLE) {
            return false;
        }

        WebView.HitTestResult hitResult;
        try {
            hitResult = sourceView.getHitTestResult();
        } catch (RuntimeException ignored) {
            return false;
        }
        if (hitResult == null) return false;

        int type = hitResult.getType();
        if (type != WebView.HitTestResult.SRC_ANCHOR_TYPE
                && type != WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            return false;
        }

        final String fallbackHref = hitResult.getExtra();
        final String pageUrl = sourceView.getUrl();
        final AtomicBoolean delivered = new AtomicBoolean(false);

        Handler hrefHandler = new Handler(Looper.getMainLooper(), message -> {
            if (!delivered.compareAndSet(false, true)) return true;
            Bundle data = message != null ? message.getData() : null;
            String resolvedHref = data != null ? data.getString("url") : null;
            showLongPressedLinkMenu(sourceView, resolvedHref, fallbackHref, pageUrl);
            return true;
        });

        try {
            Message hrefMessage = hrefHandler.obtainMessage();
            sourceView.requestFocusNodeHref(hrefMessage);
        } catch (RuntimeException ignored) {
            // The delayed fallback below still uses HitTestResult#getExtra().
        }

        mainHandler.postDelayed(() -> {
            if (delivered.compareAndSet(false, true)) {
                showLongPressedLinkMenu(sourceView, null, fallbackHref, pageUrl);
            }
        }, 180L);
        return true;
    }

    void showLongPressedLinkMenu(WebView sourceView, String requestedHref,
                                         String fallbackHref, String pageUrl) {
        if (sourceView == null || sourceView != webView
                || sourceView.getVisibility() != View.VISIBLE) {
            return;
        }

        String resolvedUrl = LongPressLinkPolicy.resolveHttpUrl(requestedHref, pageUrl);
        if (resolvedUrl == null) {
            resolvedUrl = LongPressLinkPolicy.resolveHttpUrl(fallbackHref, pageUrl);
        }
        if (resolvedUrl == null) return;

        final String targetUrl = resolvedUrl;
        new AlertDialog.Builder(this)
                .setItems(new CharSequence[]{"Buka link di tab baru"}, (dialog, which) -> {
                    if (which == 0) openLongPressedLinkInNewTab(targetUrl);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    void openLongPressedLinkInNewTab(String url) {
        String targetUrl = LongPressLinkPolicy.resolveHttpUrl(url, getEffectiveCurrentUrl());
        if (targetUrl == null) return;
        if (safeMode && isUnsafeUrl(targetUrl)) {
            QuietToast.makeText(this, "Diblokir Safe Browsing sederhana",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }

        newTabInCurrentProfile();
        if (addressBar != null) addressBar.setText(targetUrl);
        openAddressBarUrl();
    }

    @SuppressLint("SetJavaScriptEnabled")
    void configureWebView() {
        applyBrowserSettings();
        webView.addJavascriptInterface(new VideoBridge(this), "YieldVideoBridge");
        webView.addJavascriptInterface(new AdBlockBridge(this), "YieldAdBlockBridge");
        webView.addJavascriptInterface(new TranslateBridge(this), "YieldTranslateBridge");
        installShieldDocumentStartScript(webView);
        applyProfileCookiePolicy(webView);
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            beginDownloadFromWeb(url, contentDisposition, mimeType, userAgent);
        });
        final WebView configuredWebView = webView;
        configuredWebView.setOnLongClickListener(v -> {
            if (!(v instanceof WebView)) return false;
            return handleLongPressedLink((WebView) v);
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (view != webView) return false;
                String u = request.getUrl().toString();
                String currentUrl = view != null ? view.getUrl() : "";
                TabInfo requestTab = findTabByWebView(view);
                if (requestTab == null && view == webView) requestTab = getCurrentTab();
                if ((currentUrl == null || currentUrl.length() == 0) && requestTab != null) currentUrl = getTabReferenceUrl(requestTab);
                boolean mainFrame = request != null && request.isForMainFrame();
                boolean hasGesture = false;
                try { if (Build.VERSION.SDK_INT >= 24 && request != null) hasGesture = request.hasGesture(); } catch (Exception ignored) {}

                if (safeMode && isUnsafeUrl(u)) {
                    QuietToast.makeText(YieldWebRuntimeActivity.this, "Diblokir Safe Browsing sederhana", QuietToast.LENGTH_SHORT).show();
                    return true;
                }

                if (mainFrame && isTrustedDownloadIntentUrl(u)) {
                    markTrustedMainFrameNavigation(u);
                    prepareTabForMainFrameNavigation(requestTab, u);
                    return startHttpsFirstOverrideIfNeeded(view, u, requestTab);
                }

                // v0.10.06: popup/direct-link mobile sering membuka about:blank terlebih dahulu.
                // Pada reader, dokumen transien itu tidak boleh menggantikan chapter utama.
                if (mainFrame && ReaderCompatibilityPolicy.isTransientBlankUrl(u)
                        && isShieldReaderOrCompatibilityContext(currentUrl)) {
                    restoreAfterBlockedNavigation(view, u);
                    return true;
                }

                // v0.9.45: direct image main-frame harus dicegat sebelum normal user navigation
                // atau compatibility mode. Kalau tidak, klik/redirect gambar komik (.jpg/.jpeg/.webp)
                // bisa mengambil alih tab utama dan berubah menjadi halaman gambar mentah.
                String referenceUrlForDirectImage = (currentUrl != null && currentUrl.length() > 0) ? currentUrl : getTabReferenceUrl(requestTab);
                if ((referenceUrlForDirectImage == null || referenceUrlForDirectImage.length() == 0) && lastSafeHttpUrl != null) referenceUrlForDirectImage = lastSafeHttpUrl;
                if (mainFrame && !isTrustedMainFrameNavigation(u) && isDirectImageMainFrameNavigation(u, referenceUrlForDirectImage)) {
                    restoreAfterBlockedNavigation(view, u);
                    return true;
                }

                // v0.9.99 Shield Engine V2: block both cross-site click hijacks and
                // same-origin relay URLs (for example /r/<token>) before the main tab leaves
                // the reader. Clean same-site chapter/navigation URLs remain allowed.
                if (mainFrame) {
                    boolean legacySuspicious = isKnownPopupHost(u)
                            || isLikelyAdClickUrl(u)
                            || isAdUrl(u)
                            || isSuspiciousPopupNavigation(u, currentUrl);
                    if (shouldShieldBlockMainFrame(u, currentUrl, hasGesture, legacySuspicious)) {
                        restoreAfterBlockedNavigation(view, u);
                        return true;
                    }
                }

                // v0.9.50: Smart Redirect Context.
                // Domain yang biasanya iklan/direct-link tetap diblokir jika muncul otomatis,
                // tetapi jika benar-benar berasal dari klik user atau hasil pencarian, izinkan
                // dibuka dengan compatibility mode agar link situs mirip Lordborg tidak mati.
                if (mainFrame && isContextAllowedSuspiciousMainFrameNavigation(u, currentUrl, hasGesture)) {
                    markTrustedMainFrameNavigation(u);
                    prepareTabForMainFrameNavigation(requestTab, u);
                    enableSiteCompatibilityModeForUrl(u);
                    return startHttpsFirstOverrideIfNeeded(view, u, requestTab);
                }

                // v0.9.46: strict compatibility navigation. Host utama dibiarkan jalan polos,
                // tetapi popup/redirect iklan lintas-domain tetap dipindah ke tab sementara.
                if (mainFrame && (isStrictSiteCompatibilityUrl(u) || isStrictSiteCompatibilityUrl(currentUrl))) {
                    String targetHost = normalizeHostForAdBlock(u);
                    String currentHost = normalizeHostForAdBlock(currentUrl);
                    boolean sameSite = currentHost.length() > 0 && targetHost.length() > 0 && sameOrSubDomain(targetHost, currentHost);
                    boolean fromSearch = isSearchEngineResultNavigation(u, currentUrl);
                    if (isExternalSchemeUrl(u)) {
                        restoreAfterBlockedNavigation(view, u);
                        return true;
                    }
                    if (!sameSite && !fromSearch && isCompatibilityAdNavigation(u, currentUrl, hasGesture)) {
                        restoreAfterBlockedNavigation(view, u);
                        return true;
                    }
                    markTrustedMainFrameNavigation(u);
                    prepareTabForMainFrameNavigation(requestTab, u);
                    if (isStrictSiteCompatibilityUrl(u)) enableSiteCompatibilityModeForUrl(u);
                    return startHttpsFirstOverrideIfNeeded(view, u, requestTab);
                }

                // v0.9.97: Compatibility Ad Shield. First-party reader navigation remains
                // untouched, while suspicious cross-site popup/click-hijack destinations are
                // blocked before they can replace the chapter in the main tab.
                if (mainFrame && (isSiteCompatibilityModeActiveForUrl(u)
                        || isSiteCompatibilityModeActiveForUrl(currentUrl)
                        || isReloadLoopGuardActiveForUrl(currentUrl))) {
                    boolean suspiciousCrossSite = isCompatibilityAdNavigation(u, currentUrl, hasGesture);
                    if (suspiciousCrossSite) {
                        restoreAfterBlockedNavigation(view, u);
                        return true;
                    }
                    markTrustedMainFrameNavigation(u);
                    prepareTabForMainFrameNavigation(requestTab, u);
                    return startHttpsFirstOverrideIfNeeded(view, u, requestTab);
                }

                if (mainFrame && isNormalUserMainFrameNavigation(u, currentUrl, hasGesture)) {
                    markTrustedMainFrameNavigation(u);
                    prepareTabForMainFrameNavigation(requestTab, u);
                    return startHttpsFirstOverrideIfNeeded(view, u, requestTab);
                }

                if (request.isForMainFrame() && isExternalSchemeUrl(u)) {
                    restoreAfterBlockedNavigation(view, u);
                    return true;
                }

                if (adBlock && (adBlockRedirectBlocker || adBlockClickHijackBlocker) && request.isForMainFrame()
                        && !isTrustedMainFrameNavigation(u)
                        && !isNormalUserMainFrameNavigation(u, currentUrl, hasGesture)
                        && (isSuspiciousPopupNavigation(u, currentUrl) || isLikelyAdClickUrl(u))) {
                    restoreAfterBlockedNavigation(view, u);
                    return true;
                }
                if (mainFrame) return startHttpsFirstOverrideIfNeeded(view, u, requestTab);
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // v0.9.81: shouldInterceptRequest berjalan di thread Chromium/background.
                // Jangan pernah memanggil WebView.getUrl()/getTitle()/method WebView apa pun di sini.
                // Android akan crash: "A WebView method was called on thread 'ThreadPoolForeg'".
                // Ambil URL halaman dari cache main-thread saja.
                TabInfo requestOwner = findTabByWebView(view);
                if (requestOwner == null && view != webView) return super.shouldInterceptRequest(view, request);
                if (requestOwner == null) requestOwner = getCurrentTab();
                String u = request.getUrl().toString();
                String pageUrl = requestOwner != null && requestOwner.currentPageUrlForRequest != null ? requestOwner.currentPageUrlForRequest : "";
                if ((pageUrl == null || pageUrl.length() == 0) && requestOwner != null) pageUrl = getTabReferenceUrl(requestOwner);
                if ((pageUrl == null || pageUrl.length() == 0) && view == webView) pageUrl = currentPageUrlForRequest != null ? currentPageUrlForRequest : "";
                if ((pageUrl == null || pageUrl.length() == 0) && lastSafeHttpUrl != null) pageUrl = lastSafeHttpUrl;
                // v0.9.44: compatibility mode bersifat universal per-domain. Jika aktif, jangan
                // intercept resource sama sekali untuk halaman itu. Ini menyelesaikan situs yang
                // reload/blank karena mendeteksi resource diblokir walau user sedang mencoba masuk.
                // v0.9.63: YouTube player tidak boleh diblokir di network layer sama sekali.
                // Pada Android WebView, memblokir doubleclick/google ads metadata dari halaman YouTube
                // sering membuat player utama menunggu iklan lalu layar menjadi hitam/stuck.
                // Jadi khusus YouTube, semua request dibiarkan lewat dan ad handling hanya dilakukan
                // oleh script YouTube Safe AdBlock yang klik Skip / speed iklan secara aman.
                if (isTrustedDownloadIntentUrl(u) || isTrustedDownloadIntentUrl(pageUrl)) {
                    return super.shouldInterceptRequest(view, request);
                }
                if (isYouTubePageUrl(pageUrl) || isYouTubePageUrl(u)) {
                    return super.shouldInterceptRequest(view, request);
                }
                boolean compatibilityPageRequest = isStrictSiteCompatibilityUrl(pageUrl)
                        || isSiteCompatibilityModeActiveForUrl(pageUrl)
                        || isReloadLoopGuardActiveForUrl(pageUrl);
                if (compatibilityPageRequest) {
                    // Preserve first-party scripts, reader images and media. Only block a
                    // high-confidence third-party ad/popup resource so compatibility mode does
                    // not degrade into "ad blocker off".
                    if (adBlock && adBlockScriptIframeBlocker) {
                        boolean legacyHardAd = isCompatibilityThirdPartyAdResource(u, pageUrl)
                                || isAdUrl(u) || isKnownPopupHost(u);
                        if (ShieldEngineV2.shouldBlockSubresource(u, pageUrl, legacyHardAd)) {
                            return buildBlockedResponse(u);
                        }
                    }
                    return super.shouldInterceptRequest(view, request);
                }
                // v0.9.42: kalau suatu host masuk compatibility/reload-loop guard, jangan blokir
                // resource first-party-nya. Banyak situs lama memakai nama file/folder berisi "ad"
                // untuk script/menu internal; kalau ikut diblokir halaman bisa blank.
                if (isReloadLoopGuardActiveForUrl(pageUrl) && isFirstPartyResourceForCurrentPage(u, pageUrl)) {
                    return super.shouldInterceptRequest(view, request);
                }
                if (isTrustedMainFrameNavigation(pageUrl) && isFirstPartyResourceForCurrentPage(u, pageUrl)) {
                    return super.shouldInterceptRequest(view, request);
                }
                if (adBlock && adBlockScriptIframeBlocker && !isMediaResourceUrl(u) && !isYoutubeCoreUrl(u)) {
                    boolean legacyHardAd = isAdUrl(u) || isKnownPopupHost(u);
                    if (ShieldEngineV2.shouldBlockSubresource(u, pageUrl, legacyHardAd)) {
                        return buildBlockedResponse(u);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (BrowserWebErrorHandler.handle(
                        webView,
                        view,
                        request,
                        error,
                        adBlock,
                        lastSafeHttpUrl,
                        YieldWebRuntimeActivity.this::handleHttpsFirstMainFrameFailure,
                        YieldWebRuntimeActivity.this::isSiteCompatibilityModeActiveForUrl,
                        YieldWebRuntimeActivity.this::isExternalSchemeUrl,
                        YieldWebRuntimeActivity.this::isTrustedMainFrameNavigation,
                        YieldWebRuntimeActivity.this::isKnownPopupHost,
                        YieldWebRuntimeActivity.this::isLikelyAdClickUrl,
                        YieldWebRuntimeActivity.this::isAdUrl,
                        YieldWebRuntimeActivity.this::isSuspiciousPopupNavigation,
                        YieldWebRuntimeActivity.this::restoreAfterBlockedNavigation,
                        () -> isSmoothSearchTransitionActive(),
                        YieldWebRuntimeActivity.this::finishSmoothSearchTransition)) {
                    return;
                }
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (view == webView && elementPickerActive) finishElementPicker(false);
                BrowserPageStartCoordinator.Preparation pageStart =
                        BrowserPageStartCoordinator.prepare(
                                webView,
                                view,
                                url,
                                lastSafeHttpUrl,
                                YieldWebRuntimeActivity.this::findTabByWebView,
                                YieldWebRuntimeActivity.this::getCurrentTab,
                                YieldWebRuntimeActivity.this::getTabReferenceUrl,
                                YieldWebRuntimeActivity.this::extractOriginalUrl,
                                ReaderCompatibilityPolicy::isTransientBlankUrl,
                                YieldWebRuntimeActivity.this::isShieldReaderOrCompatibilityContext,
                                YieldWebRuntimeActivity.this::isKnownPopupHost,
                                YieldWebRuntimeActivity.this::isLikelyAdClickUrl,
                                YieldWebRuntimeActivity.this::isAdUrl,
                                YieldWebRuntimeActivity.this::isSuspiciousPopupNavigation,
                                YieldWebRuntimeActivity.this::shouldShieldBlockMainFrame,
                                YieldWebRuntimeActivity.this::restoreAfterBlockedNavigation,
                                YieldWebRuntimeActivity.this::isStrictSiteCompatibilityUrl,
                                YieldWebRuntimeActivity.this::registerNavigationLoopGuard,
                                YieldWebRuntimeActivity.this::isSiteCompatibilityModeActiveForUrl);
                if (pageStart.inactiveView) {
                    super.onPageStarted(view, url, favicon);
                    return;
                }
                if (pageStart.restored) return;

                TabInfo activeOwner = pageStart.owner;
                String safeBeforePageStarted = pageStart.safeReferenceUrl;
                String startedUrl = pageStart.startedUrl;
                currentPageUrlForRequest = startedUrl;
                if (activeOwner != null) activeOwner.currentPageUrlForRequest = currentPageUrlForRequest;
                webHorizontalGestureGuard = false;
                webHorizontalGestureGuardHost = hostOfUrl(currentPageUrlForRequest);
                syncNightModeWebSettingsForUrl(currentPageUrlForRequest);

                BrowserPageStartCoordinator.applyProfile(
                        pageStart,
                        desktopMode,
                        () -> enableSiteCompatibilityModeForUrl(url),
                        YieldWebRuntimeActivity.this::applyPlainCompatibilitySettings,
                        () -> scheduleCompatibilityLoadFallback(url),
                        YieldWebRuntimeActivity.this::applyBrowserSettings,
                        () -> {
                            try {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                            } catch (Exception ignored) {
                            }
                        },
                        delay -> mainHandler.postDelayed(
                                YieldWebRuntimeActivity.this::applyDesktopViewportIfNeeded, delay),
                        delay -> mainHandler.postDelayed(
                                YieldWebRuntimeActivity.this::applyMobileViewportIfNeeded, delay));
                super.onPageStarted(view, url, favicon);
                if (pendingHideKeyboardAfterNavigation) {
                    blurWebInputsAndHideKeyboard();
                }
                BrowserPageStartCoordinator.handleNavigation(
                        view,
                        url,
                        activeOwner,
                        safeBeforePageStarted,
                        adBlock,
                        adBlockRedirectBlocker,
                        () -> {
                            if (isSmoothSearchTransitionActive() && navigationLoadingOverlay != null) {
                                navigationLoadingOverlay.bringToFront();
                            }
                        },
                        YieldWebRuntimeActivity.this::shouldRecordHistoryUrl,
                        () -> historyClearLock = false,
                        YieldWebRuntimeActivity.this::addBrowserHistory,
                        YieldWebRuntimeActivity.this::getEffectiveCurrentUrl,
                        YieldWebRuntimeActivity.this::getTabReferenceUrl,
                        YieldWebRuntimeActivity.this::isCompatibilityNavigationFlow,
                        YieldWebRuntimeActivity.this::isTrustedMainFrameNavigation,
                        YieldWebRuntimeActivity.this::isDirectImageMainFrameNavigation,
                        YieldWebRuntimeActivity.this::isExternalSchemeUrl,
                        YieldWebRuntimeActivity.this::isSearchEngineResultNavigation,
                        YieldWebRuntimeActivity.this::isSuspiciousPopupNavigation,
                        YieldWebRuntimeActivity.this::isLikelyAdClickUrl,
                        YieldWebRuntimeActivity.this::restoreAfterBlockedNavigation);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                BrowserPageCommitCoordinator.Result pageCommit =
                        BrowserPageCommitCoordinator.handle(
                                view == webView, view, url,
                                YieldWebRuntimeActivity.this::extractOriginalUrl,
                                YieldWebRuntimeActivity.this::findTabByWebView,
                                YieldWebRuntimeActivity.this::getCurrentTab,
                                YieldWebRuntimeActivity.this::commitTabUrlIfSafe,
                                YieldWebRuntimeActivity.this::handleHttpsFirstNavigationSuccess,
                                finalUrl -> currentPageUrlForRequest = finalUrl,
                                YieldWebRuntimeActivity.this::syncNightModeWebSettingsForUrl,
                                YieldWebRuntimeActivity.this::scheduleNightModeSyncForPage);
                if (pageCommit.inactiveView) return;
                String finalUrl = pageCommit.finalUrl;
                BrowserPageCommitCoordinator.applyEffects(
                        finalUrl, adBlock,
                        YieldWebRuntimeActivity.this::isStrictSiteCompatibilityUrl,
                        YieldWebRuntimeActivity.this::isSiteCompatibilityModeActiveForUrl,
                        YieldWebRuntimeActivity.this::syncShieldRuntimeState,
                        YieldWebRuntimeActivity.this::injectShieldEngineV2Fallback,
                        YieldWebRuntimeActivity.this::injectAdBlockCssEarly,
                        YieldWebRuntimeActivity.this::injectCompatibilityAdShield,
                        YieldWebRuntimeActivity.this::scheduleUniversalReaderCompatibilityRepair,
                        YieldWebRuntimeActivity.this::hasUserFiltersForCurrentHost,
                        YieldWebRuntimeActivity.this::applyUserFiltersForCurrentPage);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (BrowserPageFinishCoordinator.handleInactive(
                        view != webView, view, url,
                        YieldWebRuntimeActivity.this::findTabByWebView,
                        YieldWebRuntimeActivity.this::extractOriginalUrl,
                        YieldWebRuntimeActivity.this::commitTabUrlIfSafe,
                        BrowserPageFinishCoordinator::getTitle,
                        BrowserPageFinishCoordinator::saveWebState)) {
                    super.onPageFinished(view, url);
                    return;
                }
                BrowserPageFinishCoordinator.Result pageFinish =
                        BrowserPageFinishCoordinator.handleActive(
                                view, url, YieldWebRuntimeActivity.this::extractOriginalUrl,
                                YieldWebRuntimeActivity.this::findTabByWebView, YieldWebRuntimeActivity.this::getCurrentTab,
                                YieldWebRuntimeActivity.this::handleHttpsFirstNavigationSuccess,
                                finalUrl -> currentPageUrlForRequest = finalUrl,
                                YieldWebRuntimeActivity.this::scheduleHorizontalGestureGuardCheck,
                                YieldWebRuntimeActivity.this::shouldRecordHistoryUrl,
                                YieldWebRuntimeActivity.this::canCommitUrlToTab,
                                finalUrl -> lastSafeHttpUrl = finalUrl,
                                () -> webView != null && webView.getVisibility() == View.VISIBLE,
                                finalUrl -> addressBar.setText(finalUrl),
                                () -> progressBar.setVisibility(View.GONE));
                String finalUrl = pageFinish.finalUrl;
                TabInfo currentTab = pageFinish.owner;
                BrowserPageFinishPolicy.Profile pageFinishProfile =
                        BrowserPageFinishCoordinator.prepareProfile(
                                finalUrl, YieldWebRuntimeActivity.this::isStrictSiteCompatibilityUrl,
                                YieldWebRuntimeActivity.this::isReloadLoopGuardActiveForUrl,
                                YieldWebRuntimeActivity.this::isSiteCompatibilityModeActiveForUrl,
                                YieldWebRuntimeActivity.this::applyPlainCompatibilitySettings,
                                YieldWebRuntimeActivity.this::cancelSmoothSearchTransition);
                boolean pageReloadGuarded = BrowserPageFinishPolicy.isReloadGuarded(pageFinishProfile);
                if (pageReloadGuarded) {
                    BrowserPageFinishCoordinator.applyGuardedEffects(
                            pageFinishProfile, finalUrl, adBlock, desktopMode,
                            YieldWebRuntimeActivity.this::applyPlainCompatibilitySettings,
                            YieldWebRuntimeActivity.this::scheduleNightModeSyncForPage,
                            YieldWebRuntimeActivity.this::injectCompatibilityAdShield,
                            delay -> mainHandler.postDelayed(
                                    YieldWebRuntimeActivity.this::injectCompatibilityAdShield, delay),
                            YieldWebRuntimeActivity.this::scheduleUniversalReaderCompatibilityRepair,
                            delay -> mainHandler.postDelayed(
                                    YieldWebRuntimeActivity.this::applyDesktopViewportIfNeeded, delay),
                            delay -> mainHandler.postDelayed(
                                    YieldWebRuntimeActivity.this::applyMobileViewportIfNeeded, delay));
                }
                BrowserPageFinishCoordinator.applyNormalEffects(
                        pageFinishProfile, finalUrl, desktopMode, readerMode, adBlock,
                        YieldWebRuntimeActivity.this::applyViewportForCurrentMode,
                        delay -> mainHandler.postDelayed(
                                YieldWebRuntimeActivity.this::applyViewportForCurrentMode, delay),
                        delay -> mainHandler.postDelayed(
                                YieldWebRuntimeActivity.this::applyDesktopViewportIfNeeded, delay),
                        YieldWebRuntimeActivity.this::injectReaderMode,
                        () -> {
                            injectShieldEngineV2Fallback();
                            injectPremiumAdBlock();
                            injectYouTubeSafeAdBlockV6();
                        },
                        delay -> mainHandler.postDelayed(() -> {
                            injectPremiumAdBlock();
                            injectYouTubeSafeAdBlockV6();
                        }, delay),
                        YieldWebRuntimeActivity.this::scheduleUniversalBlankCompatibilityRecovery,
                        YieldWebRuntimeActivity.this::scheduleUniversalReaderCompatibilityRepair,
                        YieldWebRuntimeActivity.this::updateVideoControlsVisibility);
                BrowserPageFinishCoordinator.applyUserFilterEffects(
                        hasUserFiltersForCurrentHost(),
                        YieldWebRuntimeActivity.this::applyUserFiltersForCurrentPage,
                        delay -> mainHandler.postDelayed(
                                YieldWebRuntimeActivity.this::applyUserFiltersForCurrentPage, delay));
                currentTab = BrowserPageFinishCoordinator.finalizeHistory(
                        view, currentTab, YieldWebRuntimeActivity.this::getCurrentTab, finalUrl,
                        YieldWebRuntimeActivity.this::shouldRecordHistoryUrl,
                        YieldWebRuntimeActivity.this::addBrowserHistory,
                        BrowserPageFinishCoordinator::getTitle,
                        YieldWebRuntimeActivity.this::commitTabUrlIfSafe,
                        YieldWebRuntimeActivity.this::saveTabsSession);
                BrowserPageFinishCoordinator.applyNormalCompletionEffects(
                        pageFinishProfile, finalUrl, url,
                        () -> videoControlsManualHidden = false,
                        YieldWebRuntimeActivity.this::injectVideoPlaybackWatcher,
                        YieldWebRuntimeActivity.this::scheduleNightModeSyncForPage,
                        YieldWebRuntimeActivity.this::detectTranslateProxyBlocked);
                BrowserPageFinishCoordinator.applyTranslateToolbarEffects(
                        pageFinishProfile, hideGoogleTranslateBar, url,
                        YieldWebRuntimeActivity.this::isGoogleTranslatedUrl,
                        delay -> mainHandler.postDelayed(
                                YieldWebRuntimeActivity.this::hideGoogleTranslateToolbar, delay));
                BrowserPageFinishCoordinator.applyCompatibleTranslateEffects(
                        pageFinishProfile, translateEnabled, compatibleTranslateActive,
                        translateManuallyDisabled, url,
                        YieldWebRuntimeActivity.this::isGoogleTranslatedUrl,
                        () -> translateSessionToken,
                        (token, delay) -> mainHandler.postDelayed(
                                () -> translatePageCompatible(token), delay));
                BrowserPageFinishCoordinator.applyKeyboardEffects(
                        pendingHideKeyboardAfterNavigation,
                        YieldWebRuntimeActivity.this::blurWebInputsAndHideKeyboard,
                        () -> pendingHideKeyboardAfterNavigation = false,
                        (action, delay) -> mainHandler.postDelayed(action, delay));
                BrowserPageFinishCoordinator.applyFinalEffects(
                        isSmoothSearchTransitionActive(),
                        delay -> mainHandler.postDelayed(
                                YieldWebRuntimeActivity.this::finishSmoothSearchTransition, delay),
                        YieldWebRuntimeActivity.this::scheduleCloseDetectedAdTabs,
                        YieldWebRuntimeActivity.this::updateTopActionStates);
            }
        });

        webView.setWebChromeClient(new BrowserChromeClient(
                (view, newProgress) -> BrowserChromeProgressHandler.handle(
                        webView,
                        view,
                        newProgress,
                        homeScroll != null && homeScroll.getVisibility() == View.VISIBLE,
                        progressBar),
                (view, callback) -> BrowserChromeFullscreenHandler.show(
                        fullscreenVideoView,
                        view,
                        callback,
                        getWindow(),
                        topBarView,
                        bottomNavView,
                        (fullscreenView, fullscreenCallback, originalVisibility) -> {
                            fullscreenVideoView = fullscreenView;
                            fullscreenVideoCallback = fullscreenCallback;
                            originalSystemUiVisibility = originalVisibility;
                        },
                        YieldWebRuntimeActivity.this::setRequestedOrientation,
                        YieldWebRuntimeActivity.this::moveVideoControlsToFullscreenOverlay,
                        YieldWebRuntimeActivity.this::updateVideoModeToggleButton,
                        YieldWebRuntimeActivity.this::checkAndShowVideoControls),
                () -> BrowserChromeFullscreenHandler.hide(
                        fullscreenVideoView,
                        fullscreenVideoCallback,
                        getWindow(),
                        () -> fullscreenVideoView = null,
                        () -> fullscreenVideoCallback = null,
                        YieldWebRuntimeActivity.this::restoreAfterVideoFullscreen,
                        YieldWebRuntimeActivity.this::updateVideoModeToggleButton)));
    }

    @SuppressLint("SetJavaScriptEnabled")
    boolean isSystemDarkMode() {
        try {
            int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return mode == Configuration.UI_MODE_NIGHT_YES;
        } catch (Exception e) {
            return false;
        }
    }

    String getCurrentHostForSettings() {
        try {
            String url = getEffectiveCurrentUrl();
            if (url == null || url.length() == 0) {
                if (webView != null && webView.getUrl() != null) url = extractOriginalUrl(webView.getUrl());
            }
            if (url == null || url.length() == 0) return "";
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return "";
            if (host.startsWith("www.")) host = host.substring(4);
            return host.toLowerCase(Locale.US);
        } catch (Exception e) {
            return "";
        }
    }

    boolean isNightModeActiveForCurrentSite() {
        return isNightModeActiveForUrl(getEffectiveCurrentUrl());
    }

    boolean isNightModeActiveForUrl(String url) {
        boolean active;
        if ("OFF".equals(nightModeOption)) {
            active = false;
        } else if ("AUTO".equals(nightModeOption)) {
            active = isSystemDarkMode();
        } else {
            active = true;
        }

        String host = getHostForNightMode(url);
        if (host.length() > 0 && nightModeExceptions.contains(host)) {
            active = false;
        }
        return active;
    }

    String getHostForNightMode(String url) {
        try {
            String target = url;
            if (target == null || target.length() == 0) target = getEffectiveCurrentUrl();
            if (target == null || target.length() == 0) return "";
            Uri uri = Uri.parse(target);
            String host = uri.getHost();
            if (host == null) return "";
            if (host.startsWith("www.")) host = host.substring(4);
            return host.toLowerCase(Locale.US);
        } catch (Exception e) {
            return "";
        }
    }

    boolean isAlgorithmicDarkeningSupported() {
        try {
            return WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING);
        } catch (Throwable ignored) {
            return false;
        }
    }

    void applyAlgorithmicDarkening(WebSettings settings, boolean active) {
        if (settings == null) return;
        try {
            // WebSettings#setForceDark is a no-op for apps targeting API 33+.
            // AndroidX WebKit delegates this call to the installed WebView provider,
            // so Android 10/11 can use algorithmic darkening when the provider supports it.
            if (isAlgorithmicDarkeningSupported()) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, active);
            }
        } catch (Throwable ignored) {
        }
    }

    void syncNightModeWebSettingsForUrl(String url) {
        if (webView == null) return;
        boolean active = isNightModeActiveForUrl(url);
        try {
            applyAlgorithmicDarkening(webView.getSettings(), active);
        } catch (Exception ignored) {
        }
        try {
            webView.setBackgroundColor(active ? COLOR_BG : Color.WHITE);
        } catch (Exception ignored) {
        }
    }

    void scheduleNightModeSyncForPage(String url) {
        if (webView == null) return;
        final int token = ++nightModeApplyToken;
        final String targetUrl = url != null ? url : getEffectiveCurrentUrl();
        syncNightModeWebSettingsForUrl(targetUrl);
        applyNightModeToWebPage();
        mainHandler.postDelayed(() -> { if (token == nightModeApplyToken) applyNightModeToWebPage(); }, 220);
        mainHandler.postDelayed(() -> { if (token == nightModeApplyToken) applyNightModeToWebPage(); }, 900);
        mainHandler.postDelayed(() -> { if (token == nightModeApplyToken) applyNightModeToWebPage(); }, 2200);
    }

    void setNightModeOptionAndApply(String option, boolean reloadPage) {
        nightModeOption = option;
        nightMode = !"OFF".equals(nightModeOption);
        nightModeApplyToken++;
        saveSettings();
        applyBrowserSettings();
        syncNightModeWebSettingsForUrl(getEffectiveCurrentUrl());
        scheduleNightModeSyncForPage(getEffectiveCurrentUrl());

        if (reloadPage && webView != null && webView.getVisibility() == View.VISIBLE) {
            mainHandler.postDelayed(() -> {
                try {
                    String url = getEffectiveCurrentUrl();
                    if (url == null || url.length() == 0) url = webView != null ? webView.getUrl() : "";
                    if (url != null && isHttpOrHttpsUrl(url)) {
                        hardReloadUrlWithCurrentBrowserMode(url, true);
                    }
                } catch (Exception ignored) {
                }
            }, 260);
        }
    }

    String nightModeLabel() {
        if ("OFF".equals(nightModeOption)) return "OFF";
        if ("AUTO".equals(nightModeOption)) return "Auto ikut sistem";
        return "ON";
    }

    void applyNightModeToWebPage() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) return;
        String pageUrl = getEffectiveCurrentUrl();
        boolean compatibilityMode = isSiteCompatibilityModeActiveForUrl(pageUrl)
                || isStrictSiteCompatibilityUrl(pageUrl);

        syncNightModeWebSettingsForUrl(pageUrl);
        boolean active = isNightModeActiveForUrl(pageUrl);

        try {
            // Do not rely solely on WebViewFeature.ALGORITHMIC_DARKENING. Some Android 11
            // providers report support while leaving light pages visually unchanged. The page-side
            // engine is therefore always applied and remains reversible when night mode is disabled.
            runPageScript(active
                    ? NightModePageScript.enable(compatibilityMode)
                    : NightModePageScript.disable());
            webView.setBackgroundColor(active ? COLOR_BG : Color.WHITE);
        } catch (Exception ignored) {
        }
    }

    void disableNightModeCompletely(boolean reloadPage) {
        setNightModeOptionAndApply("OFF", reloadPage);
    }

    void showNightModeSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(12));
        box.setBackground(roundRect(Color.parseColor("#2B2D33"), dp(18), 0, Color.TRANSPARENT));

        TextView title = new TextView(this);
        title.setText("Mode Malam");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));
        box.addView(title);

        box.addView(nightChoiceRow("OFF", "OFF".equals(nightModeOption), v -> {
            disableNightModeCompletely(true);
            updateTopActionStates();
            QuietToast.makeText(this, "Mode Malam: OFF", QuietToast.LENGTH_SHORT).show();
        }));
        box.addView(nightChoiceRow("ON", "ON".equals(nightModeOption), v -> {
            setNightModeOptionAndApply("ON", true);
            updateTopActionStates();
            QuietToast.makeText(this, "Mode Malam: ON", QuietToast.LENGTH_SHORT).show();
        }));
        box.addView(nightChoiceRow("Auto ikut sistem", "AUTO".equals(nightModeOption), v -> {
            setNightModeOptionAndApply("AUTO", true);
            updateTopActionStates();
            QuietToast.makeText(this, "Mode Malam: Auto ikut sistem", QuietToast.LENGTH_SHORT).show();
        }));
        box.addView(nightChoiceRow("Bersihkan style gelap halaman ini", false, v -> {
            disableNightModeCompletely(true);
            updateTopActionStates();
            QuietToast.makeText(this, "Style gelap dibersihkan", QuietToast.LENGTH_SHORT).show();
        }));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setGravity(Gravity.END);
        buttons.setPadding(0, dp(10), 0, 0);

        TextView exception = dialogTextButton("PENGECUALIAN SITUS");
        exception.setOnClickListener(v -> showNightModeExceptionDialog());
        buttons.addView(exception);

        TextView cancel = dialogTextButton("TUTUP");
        cancel.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(cancel);

        box.addView(buttons);

        dialog.setContentView(box);
        dialog.show();
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.86f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
    }

    View nightChoiceRow(String label, boolean checked, View.OnClickListener listener) {
        return SettingsUi.nightChoiceRow(this, label, checked, listener);
    }

    TextView dialogTextButton(String text) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(COLOR_ACCENT);
        btn.setTextSize(13);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(12), dp(10), dp(12), dp(10));
        return btn;
    }

    void showNightModeExceptionDialog() {
        String host = getCurrentHostForSettings();
        if (host.length() == 0) {
            QuietToast.makeText(this, "Buka situs dulu untuk mengatur pengecualian", QuietToast.LENGTH_SHORT).show();
            return;
        }

        boolean excepted = nightModeExceptions.contains(host);
        String message = excepted
                ? host + " sedang dikecualikan dari Mode Malam. Hapus pengecualian?"
                : "Kecualikan " + host + " dari Mode Malam?";

        new AlertDialog.Builder(this)
                .setTitle("Pengecualian per situs")
                .setMessage(message)
                .setPositiveButton(excepted ? "Hapus pengecualian" : "Kecualikan situs", (d, w) -> {
                    if (excepted) nightModeExceptions.remove(host);
                    else nightModeExceptions.add(host);
                    saveSettings();
                    applyBrowserSettings();
                    scheduleNightModeSyncForPage(getEffectiveCurrentUrl());
                    QuietToast.makeText(this, excepted ? "Pengecualian dihapus" : "Situs dikecualikan", QuietToast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    void loadBrowserUrl(String url) {
        if (url == null) return;
        try { activateTabWebView(getCurrentTab(), true); } catch (Exception ignored) {}
        if (webView == null) return;
        String cleanUrl = BrowserLoadRequestPolicy.trimInput(url);
        cleanUrl = normalizeUrlForCurrentBrowserMode(cleanUrl);
        if (cleanUrl == null || cleanUrl.length() == 0) return;
        TabInfo activeLoadTab = getCurrentTab();
        cleanUrl = prepareHttpsFirstNavigation(cleanUrl, activeLoadTab);
        if (addressBar != null && webView.getVisibility() == View.VISIBLE) addressBar.setText(cleanUrl);
        if (activeLoadTab != null && isHttpOrHttpsUrl(cleanUrl)) {
            activeLoadTab.url = cleanUrl;
            activeLoadTab.currentPageUrlForRequest = cleanUrl;
        }

        if (BrowserLoadRequestPolicy.isDirectWebViewUrl(cleanUrl)) {
            webView.loadUrl(cleanUrl);
            return;
        }

        final String targetUrl = cleanUrl;
        BrowserLoadExecutionCoordinator.execute(
                targetUrl,
                isStrictSiteCompatibilityUrl(targetUrl),
                desktopMode,
                () -> {
                    currentPageUrlForRequest = targetUrl;
                    if (activeLoadTab != null) activeLoadTab.currentPageUrlForRequest = targetUrl;
                },
                () -> markTrustedMainFrameNavigation(targetUrl),
                () -> prepareTabForMainFrameNavigation(activeLoadTab, targetUrl),
                () -> enableSiteCompatibilityModeForUrl(targetUrl),
                YieldWebRuntimeActivity.this::applyPlainCompatibilitySettings,
                YieldWebRuntimeActivity.this::loadCompatibilityUrlWithCurrentMode,
                delay -> mainHandler.postDelayed(
                        YieldWebRuntimeActivity.this::applyDesktopViewportIfNeeded, delay),
                YieldWebRuntimeActivity.this::scheduleCompatibilityLoadFallback,
                YieldWebRuntimeActivity.this::applyBrowserSettings,
                () -> BrowserLoadRequestPolicy.requestHeaders(
                        desktopMode, getMobileUserAgent(), getDesktopUserAgent()),
                (target, headers) -> webView.loadUrl(target, headers),
                target -> webView.loadUrl(target));
    }

    void scheduleMobileViewportReset() {
        MobileViewportResetCoordinator.schedule(
                desktopMode,
                browserModeToken,
                (action, delay) -> mainHandler.postDelayed(action, delay),
                () -> browserModeToken,
                () -> desktopMode,
                YieldWebRuntimeActivity.this::applyMobileViewportIfNeeded);
    }

    void toggleDesktopModeSafely() {
        boolean previousDesktopMode = desktopMode;
        try {
            String targetUrl = getSafeReloadUrlForModeChange();
            boolean wasShowingWeb = webView != null && webView.getVisibility() == View.VISIBLE;
            desktopMode = BrowserModeTogglePolicy.nextDesktopMode(desktopMode);
            targetUrl = normalizeUrlForCurrentBrowserMode(targetUrl);
            saveSettings();

            BrowserModeTogglePolicy.Plan plan = BrowserModeTogglePolicy.plan(
                    desktopMode, wasShowingWeb, targetUrl);
            if (plan.updateAddressBar) addressBar.setText(targetUrl);

            if (plan.hardReload) {
                hardReloadUrlWithCurrentBrowserMode(targetUrl, true);
            } else {
                if (plan.applySettings) applyBrowserSettings();
                if (plan.showHome) showHome();
            }

            QuietToast.makeText(
                    this, plan.statusMessage, QuietToast.LENGTH_SHORT).show();
        } catch (Exception e) {
            desktopMode = previousDesktopMode;
            try {
                applyBrowserSettings();
                saveSettings();
            } catch (Exception ignored) {}
        }
    }

    String getSafeReloadUrlForModeChange() {
        String currentWebUrl = webView != null ? webView.getUrl() : "";
        String currentAddressUrl = addressBar != null ? addressBar.getText().toString() : "";
        return BrowserModeReloadSelector.select(
                new String[]{currentWebUrl, currentAddressUrl, lastSafeHttpUrl},
                YieldWebRuntimeActivity.this::extractOriginalUrl,
                YieldWebRuntimeActivity.this::isSafeUrlForModeReload);
    }

    boolean isSafeUrlForModeReload(String url, boolean explicitCurrentPage) {
        boolean baseSafe = BrowserModeReloadPolicy.isBaseSafe(
                isHttpOrHttpsUrl(url),
                isExternalSchemeUrl(url),
                isImageResourceUrl(url),
                isMediaResourceUrl(url),
                BrowserLoadRequestPolicy.isDirectWebViewUrl(url));
        if (!baseSafe) return false;
        if (explicitCurrentPage) return true;
        return BrowserModeReloadPolicy.isFallbackClassificationSafe(
                isLikelyAdClickUrl(url),
                isKnownPopupHost(url),
                isAdUrl(url));
    }

    String normalizeUrlForCurrentBrowserMode(String url) {
        return BrowserModeUrlNormalizer.normalize(
                url,
                desktopMode,
                YieldWebRuntimeActivity.this::extractOriginalUrl,
                YieldWebRuntimeActivity.this::isHttpOrHttpsUrl,
                YieldWebRuntimeActivity.this::isYouTubePageUrl);
    }

    void applyBrowserSettings() {
        if (webView == null) return;
        boolean privateProfile = isPrivateWebView(webView);
        boolean youtubePage = isYouTubePlaybackUrl(getEffectiveCurrentUrl());
        boolean nightActive = isNightModeActiveForCurrentSite();
        BrowserWebViewSettings.apply(
                webView,
                new BrowserWebViewSettings.Config(
                        privateProfile,
                        speedMode,
                        videoBufferBooster,
                        youtubePage,
                        videoBackgroundPlay,
                        adBlock,
                        adBlockPopupBlocker,
                        dataSaver,
                        textZoom,
                        desktopMode,
                        getMobileUserAgent(),
                        getDesktopUserAgent(),
                        nightActive,
                        nightActive ? COLOR_BG : Color.WHITE),
                this::applyAlgorithmicDarkening);
    }

    void applyMobileProfile(WebSettings settings) {
        BrowserWebViewSettings.applyMobileProfile(
                webView, settings, getMobileUserAgent());
    }

    void applyDesktopProfile(WebSettings settings) {
        BrowserWebViewSettings.applyDesktopProfile(
                webView, settings, getDesktopUserAgent());
    }

    String getMobileUserAgent() {
        return BrowserUtils.getMobileUserAgent();
    }

    String getDesktopUserAgent() {
        return BrowserUtils.getDesktopUserAgent();
    }

    String hostOfUrl(String url) {
        return BrowserUrlIdentityPolicy.normalizedHost(
                url,
                YieldWebRuntimeActivity.this::extractOriginalUrl,
                value -> Uri.parse(value).getHost());
    }

    String navigationLoopKey(String url) {
        return BrowserUrlIdentityPolicy.navigationLoopKey(
                url, YieldWebRuntimeActivity.this::extractOriginalUrl);
    }

    void enableSiteCompatibilityModeForUrl(String url) {
        try {
            if (!isHttpOrHttpsUrl(url)) return;
            SiteCompatibilityActivationPolicy.Plan plan =
                    SiteCompatibilityActivationPolicy.plan(
                            true,
                            hostOfUrl(url),
                            System.currentTimeMillis(),
                            System.currentTimeMillis(),
                            siteCompatibilityToastLastMs);
            if (!plan.activate) return;
            siteCompatibilityHost = plan.host;
            siteCompatibilityUntilMs = plan.untilMs;
            try {
                siteCompatibilityHosts.put(plan.host, plan.untilMs);
            } catch (Exception ignored) {
            }
            if (plan.showToast) {
                siteCompatibilityToastLastMs = System.currentTimeMillis();
                QuietToast.makeText(this, "Mode kompatibel aktif untuk situs ini", QuietToast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
        }
    }

    boolean isSiteCompatibilityModeActiveForUrl(String url) {
        try {
            SiteCompatibilityActivePolicy.Result result =
                    SiteCompatibilityActivePolicy.evaluate(
                            hostOfUrl(url),
                            siteCompatibilityHost,
                            siteCompatibilityUntilMs,
                            siteCompatibilityHosts,
                            System.currentTimeMillis());
            try {
                for (String dead : result.expiredHosts) siteCompatibilityHosts.remove(dead);
            } catch (Exception ignored) {
            }
            return result.active;
        } catch (Exception e) {
            return false;
        }
    }

    boolean isStrictSiteCompatibilityUrl(String url) {
        try {
            return StrictCompatibilityUrlPolicy.isStrict(
                    hostOfUrl(url),
                    YieldWebRuntimeActivity.this::isKnownStrictCompatibilityHost,
                    () -> isSiteCompatibilityModeActiveForUrl(url));
        } catch (Exception e) {
            return false;
        }
    }

    boolean isKnownStrictCompatibilityHost(String host) {
        return StrictCompatibilityHostPolicy.isKnownHost(host, STRICT_COMPAT_HOSTS);
    }

    boolean isCompatibilityNavigationFlow(String targetUrl, String sourceUrl) {
        try {
            boolean compatibility = CompatibilityNavigationContextPolicy.any(
                    () -> isSiteCompatibilityModeActiveForUrl(targetUrl),
                    () -> isSiteCompatibilityModeActiveForUrl(sourceUrl),
                    () -> isStrictSiteCompatibilityUrl(targetUrl),
                    () -> isStrictSiteCompatibilityUrl(sourceUrl));
            if (!compatibility) return false;
            return CompatibilityNavigationPolicy.isFlow(
                    true,
                    hostOfUrl(targetUrl),
                    hostOfUrl(sourceUrl),
                    YieldWebRuntimeActivity.this::sameOrSubDomain);
        } catch (Exception e) {
            return false;
        }
    }

    void repairUniversalReaderPage(String url) {
        if (webView == null || !ReaderCompatibilityPolicy.isEligiblePageUrl(url)) return;
        runPageScript(UniversalReaderRepairScript.build());
    }

    void scheduleUniversalReaderCompatibilityRepair(String url) {
        if (!ReaderCompatibilityPolicy.isEligiblePageUrl(url)) return;
        final TabInfo expectedTab = findTabByWebView(webView);
        final WebView expectedView = webView;
        if (expectedTab == null || expectedView == null) return;
        final String expectedHost = hostOfUrl(url);
        final boolean compatibilityMode = isStrictSiteCompatibilityUrl(url)
                || isSiteCompatibilityModeActiveForUrl(url)
                || isReloadLoopGuardActiveForUrl(url);
        final boolean readerPathHint = ReaderCompatibilityPolicy.hasReaderPathHint(url);
        long[] schedule = ReaderCompatibilityPolicy.retrySchedule(compatibilityMode, readerPathHint);
        for (long delay : schedule) {
            postForActiveTab(expectedTab, expectedView, delay, () -> {
                try {
                    String active = ReaderRepairTargetPolicy.resolve(
                            expectedView.getUrl(),
                            expectedTab.currentPageUrlForRequest,
                            expectedHost,
                            YieldWebRuntimeActivity.this::extractOriginalUrl,
                            ReaderCompatibilityPolicy::isEligiblePageUrl,
                            YieldWebRuntimeActivity.this::hostOfUrl,
                            YieldWebRuntimeActivity.this::sameOrSubDomain);
                    if (active == null) return;
                    repairUniversalReaderPage(active);
                } catch (Exception ignored) {
                }
            });
        }
    }

    void applyPlainCompatibilitySettings() {
        if (webView == null) return;
        try {
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setLoadsImagesAutomatically(true);
            try { settings.setBlockNetworkImage(false); } catch (Exception ignored) {}
            settings.setCacheMode(isPrivateWebView(webView) ? WebSettings.LOAD_NO_CACHE : WebSettings.LOAD_DEFAULT);
            // v0.9.97: compatibility mode must not silently disable the popup blocker.
            settings.setJavaScriptCanOpenWindowsAutomatically(!(adBlock && adBlockPopupBlocker));
            settings.setSupportMultipleWindows(false);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);
            settings.setSupportZoom(true);
            settings.setTextZoom(textZoom <= 0 ? 100 : textZoom);
            if (Build.VERSION.SDK_INT >= 21) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            if (desktopMode) applyDesktopProfile(settings);
            else applyMobileProfile(settings);
            applyProfileCookiePolicy(webView);
            boolean activeNight = isNightModeActiveForCurrentSite();
            applyAlgorithmicDarkening(settings, activeNight);
            try { webView.setBackgroundColor(activeNight ? COLOR_BG : Color.WHITE); } catch (Exception ignored) {}
        } catch (Exception ignored) {
        }
    }

    void loadCompatibilityUrlWithCurrentMode(String cleanUrl) {
        if (webView == null || cleanUrl == null || cleanUrl.trim().length() == 0) return;
        try {
            Map<String, String> headers = CompatibilityLoadRequestPolicy.requestHeaders(
                    desktopMode,
                    desktopMode ? getDesktopUserAgent() : null);
            if (headers.isEmpty()) webView.loadUrl(cleanUrl);
            else webView.loadUrl(cleanUrl, headers);
        } catch (Exception e) {
            try { webView.loadUrl(cleanUrl); } catch (Exception ignored) {}
        }
    }

    void scheduleCompatibilityLoadFallback(String url) {
        final TabInfo expectedTab = findTabByWebView(webView);
        final WebView expectedView = webView;
        final String expectedHost = hostOfUrl(url);
        postForActiveTab(expectedTab, expectedView, 3500L, () -> {
            try {
                String active = CompatibilityLoadFallbackPolicy.resolve(
                        expectedView.getUrl(),
                        expectedTab.currentPageUrlForRequest,
                        expectedHost,
                        YieldWebRuntimeActivity.this::extractOriginalUrl,
                        YieldWebRuntimeActivity.this::hostOfUrl,
                        YieldWebRuntimeActivity.this::sameOrSubDomain);
                if (active == null) return;
                cancelSmoothSearchTransition();
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (expectedView.getVisibility() != View.VISIBLE) {
                    if (homeScroll != null) homeScroll.setVisibility(View.GONE);
                    expectedView.setVisibility(View.VISIBLE);
                }
                expectedView.setAlpha(1f);
                scheduleUniversalReaderCompatibilityRepair(active);
            } catch (Exception ignored) {
            }
        });
    }

    String decodeEvaluateJavascriptString(String value) {
        try {
            if (value == null) return "";
            Object parsed = new org.json.JSONTokener(value).nextValue();
            return parsed == null ? "" : String.valueOf(parsed);
        } catch (Exception e) {
            return value == null ? "" : value;
        }
    }

    boolean isUniversalCompatibilityCandidateUrl(String url) {
        try {
            if (!adBlock) return false;
            if (!isHttpOrHttpsUrl(url)) return false;
            if (isStrictSiteCompatibilityUrl(url) || isSiteCompatibilityModeActiveForUrl(url)) return false;
            if (ShieldEngineV2.isSearchResultsPage(url)) return false;
            if (isImageResourceUrl(url) || isMediaResourceUrl(url)) return false;
            return UniversalCompatibilityCandidatePolicy.isCandidate(
                    url, hostOfUrl(url));
        } catch (Exception e) {
            return false;
        }
    }

    boolean shouldRetryCompatibilityRecovery(String url) {
        try {
            String host = hostOfUrl(url);
            if (host == null || host.length() == 0) return false;
            CompatibilityRecoveryThrottlePolicy.Plan plan =
                    CompatibilityRecoveryThrottlePolicy.plan(
                            host,
                            navigationLoopKey(url),
                            autoCompatibilityRecoveryHost,
                            autoCompatibilityRecoveryKey,
                            autoCompatibilityRecoveryUntilMs,
                            System.currentTimeMillis());
            if (!plan.retry) return false;
            autoCompatibilityRecoveryHost = plan.host;
            autoCompatibilityRecoveryKey = plan.key;
            autoCompatibilityRecoveryUntilMs = plan.untilMs;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    boolean isLikelyBlankCompatibilityReport(String report) {
        return BlankCompatibilityReportPolicy.isLikelyBlank(report);
    }

    void scheduleUniversalBlankCompatibilityRecovery(String url) {
        try {
            final TabInfo expectedTab = findTabByWebView(webView);
            final WebView expectedView = webView;
            if (expectedTab == null || expectedView == null) return;
            if (!isUniversalCompatibilityCandidateUrl(url)) return;
            final String expectedHost = hostOfUrl(url);
            final String expectedKey = navigationLoopKey(url);
            if (expectedHost.length() == 0 || expectedKey.length() == 0) return;
            final long generation = expectedTab.webViewGeneration;
            postForActiveTab(expectedTab, expectedView, 1600L, () -> {
                try {
                    String activeUrl = BlankRecoveryTargetPolicy.resolve(
                            expectedView.getUrl(),
                            expectedTab.currentPageUrlForRequest,
                            expectedHost,
                            expectedKey,
                            YieldWebRuntimeActivity.this::extractOriginalUrl,
                            YieldWebRuntimeActivity.this::hostOfUrl,
                            YieldWebRuntimeActivity.this::navigationLoopKey,
                            YieldWebRuntimeActivity.this::sameOrSubDomain);
                    if (activeUrl == null) return;
                    if (isStrictSiteCompatibilityUrl(activeUrl)
                            || isSiteCompatibilityModeActiveForUrl(activeUrl)) return;
                    String js = "(function(){try{var b=document.body, d=document.documentElement; if(!b){return '0|0|0|0|0|0|'+document.readyState;}"
                            + "var t=((b.innerText||'').replace(/\\s+/g,'')).length;"
                            + "var n=b.querySelectorAll('*').length;"
                            + "var m=b.querySelectorAll('img,svg,canvas,video,iframe,object,embed').length;"
                            + "var l=b.querySelectorAll('a').length;"
                            + "var h=(b.innerHTML||'').length;"
                            + "var s=Math.max(b.scrollHeight||0,(d&&d.scrollHeight)||0);"
                            + "return [t,n,m,l,h,s,document.readyState].join('|');"
                            + "}catch(e){return '0|0|0|0|0|0|error';}})();";
                    expectedView.evaluateJavascript(js, value -> {
                        try {
                            if (!isLiveTabWebView(expectedTab, expectedView, generation)
                                    || !isCurrentTabInfo(expectedTab)) return;
                            String currentUrl = BlankRecoveryTargetPolicy.resolve(
                                    expectedView.getUrl(),
                                    expectedTab.currentPageUrlForRequest,
                                    expectedHost,
                                    expectedKey,
                                    YieldWebRuntimeActivity.this::extractOriginalUrl,
                                    YieldWebRuntimeActivity.this::hostOfUrl,
                                    YieldWebRuntimeActivity.this::navigationLoopKey,
                                    YieldWebRuntimeActivity.this::sameOrSubDomain);
                            if (currentUrl == null) return;
                            String decoded = decodeEvaluateJavascriptString(value);
                            if (!isLikelyBlankCompatibilityReport(decoded)) return;
                            if (!shouldRetryCompatibilityRecovery(currentUrl)) return;
                            enableSiteCompatibilityModeForUrl(currentUrl);
                            applyPlainCompatibilitySettings();
                            loadCompatibilityUrlWithCurrentMode(currentUrl);
                        } catch (Exception ignored) {
                        }
                    });
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    boolean isReloadLoopGuardActiveForUrl(String url) {
        return ReloadLoopGuardActivePolicy.isActive(
                reloadLoopGuardHost,
                reloadLoopGuardUntilMs,
                System.currentTimeMillis(),
                () -> hostOfUrl(url));
    }

    boolean registerNavigationLoopGuard(String url) {
        if (!isHttpOrHttpsUrl(url)) return false;
        long now = System.currentTimeMillis();
        String host = hostOfUrl(url);
        if (host.length() == 0) return false;
        String key = navigationLoopKey(url);
        if (key.length() == 0) return false;

        // User-initiated navigation/search result jangan dihitung sebagai reload loop.
        if (isTrustedMainFrameNavigation(url)) return false;

        if (isReloadLoopGuardActiveForUrl(url)) {
            // Guard aktif untuk host ini, tapi jangan anggap semua menu internal sebagai loop.
            // Hanya URL yang sama persis dengan pemicu loop yang diberi status guarded.
            return key.equals(reloadLoopGuardKey);
        }

        ReloadLoopRegistrationPolicy.Plan plan = ReloadLoopRegistrationPolicy.plan(
                key,
                reloadLoopLastKey,
                reloadLoopWindowStartMs,
                reloadLoopCount,
                reloadLoopToastLastMs,
                now);
        reloadLoopLastKey = plan.lastKey;
        reloadLoopWindowStartMs = plan.windowStartMs;
        reloadLoopCount = plan.count;

        if (plan.guardTriggered) {
            reloadLoopGuardHost = host;
            reloadLoopGuardKey = key;
            reloadLoopGuardUntilMs = plan.guardUntilMs;
            enableSiteCompatibilityModeForUrl(url);
            if (plan.showToast) {
                reloadLoopToastLastMs = now;
                QuietToast.makeText(this, "Reload loop dicegah untuk situs ini", QuietToast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    void runPageScript(String js) {
        if (webView == null) return;
        PageScriptExecutionPolicy.Plan plan = PageScriptExecutionPolicy.plan(
                js, Build.VERSION.SDK_INT >= 19);
        if (!plan.execute) return;
        try {
            if (plan.evaluateJavascript) webView.evaluateJavascript(plan.payload, null);
            else webView.loadUrl(plan.payload);
        } catch (Exception ignored) {
        }
    }

    void applyViewportForCurrentMode() {
        ViewportModeApplyPolicy.Mode mode = ViewportModeApplyPolicy.mode(
                webView != null, desktopMode);
        if (mode == ViewportModeApplyPolicy.Mode.NONE) return;
        applyBrowserSettings();
        if (mode == ViewportModeApplyPolicy.Mode.DESKTOP) applyDesktopViewportIfNeeded();
        else applyMobileViewportIfNeeded();
    }

    void applyMobileViewportIfNeeded() {
        if (desktopMode || webView == null) return;
        try { applyMobileProfile(webView.getSettings()); } catch (Exception ignored) {}
        injectMobileViewportReset();
    }

    void injectMobileViewportReset() {
        if (desktopMode || webView == null) return;
        runPageScript(ViewportScriptPolicy.mobileResetScript());
    }

    void applyDesktopViewportIfNeeded() {
        if (!desktopMode || webView == null) return;
        try { applyDesktopProfile(webView.getSettings()); } catch (Exception ignored) {}
        injectDesktopViewportLock();
    }

    void injectDesktopViewportLock() {
        if (!desktopMode || webView == null) return;
        runPageScript(ViewportScriptPolicy.desktopLockScript());
    }

    void markTrustedMainFrameNavigation(String url) {
        try {
            if (!isHttpOrHttpsUrl(url)) return;
            TrustedMainFramePolicy.Activation activation =
                    TrustedMainFramePolicy.activation(
                            true,
                            normalizeHostForAdBlock(url),
                            System.currentTimeMillis());
            if (!activation.activate) return;
            trustedMainFrameHost = activation.host;
            trustedMainFrameUntilMs = activation.untilMs;
        } catch (Exception ignored) {
        }
    }

    boolean isTrustedMainFrameNavigation(String url) {
        try {
            return TrustedMainFramePolicy.isTrusted(
                    trustedMainFrameHost,
                    trustedMainFrameUntilMs,
                    System.currentTimeMillis(),
                    normalizeHostForAdBlock(url),
                    YieldWebRuntimeActivity.this::sameOrSubDomain);
        } catch (Exception e) {
            return false;
        }
    }

    boolean isSearchEngineResultNavigation(String targetUrl, String currentUrl) {
        // v0.10.07: universal search-result allow lane. Do not depend on a fixed list of
        // .com domains: regional Google/Yahoo/Yandex domains and self-hosted SearX/SearXNG
        // pages are recognized from their search URL shape by ShieldEngineV2.
        return SearchResultNavigationPolicy.isAllowed(
                isHttpOrHttpsUrl(targetUrl),
                ShieldEngineV2.isSearchResultsPage(currentUrl));
    }

    boolean isCompatibilityAdNavigation(String targetUrl, String currentUrl, boolean hasGesture) {
        try {
            CompatibilityAdNavigationPreflightPolicy.Decision preflight =
                    CompatibilityAdNavigationPreflightPolicy.initial(
                            adBlock,
                            isHttpOrHttpsUrl(targetUrl),
                            isExternalSchemeUrl(targetUrl));
            if (preflight.resolved) return preflight.block;
            if (CompatibilityAdNavigationPreflightPolicy.isExplicitlyAllowed(
                    isTrustedDownloadIntentUrl(targetUrl),
                    isSearchEngineResultNavigation(targetUrl, currentUrl))) return false;

            String targetHost = normalizeHostForAdBlock(targetUrl);
            String currentHost = normalizeHostForAdBlock(currentUrl);
            if (targetHost.length() == 0) return false;
            if (currentHost.length() > 0 && sameOrSubDomain(targetHost, currentHost)) return false;

            boolean suspicious = isKnownPopupHost(targetUrl)
                    || isLikelyAdClickUrl(targetUrl)
                    || isAdUrl(targetUrl)
                    || isSuspiciousPopupNavigation(targetUrl, currentUrl);
            boolean explicitlyTrusted = isTrustedMainFrameNavigation(targetUrl);
            return ReaderCompatibilityPolicy.shouldBlockCrossSiteNavigation(
                    targetUrl, currentUrl, hasGesture, suspicious, explicitlyTrusted);
        } catch (Exception e) {
            return false;
        }
    }

    boolean isCompatibilityContentAssetUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);
        if (isImageResourceUrl(u) || isMediaResourceUrl(u)) return true;
        return CompatibilityContentAssetPolicy.isFontAsset(u);
    }

    boolean isCompatibilityThirdPartyAdResource(String resourceUrl, String pageUrl) {
        try {
            if (!CompatibilityThirdPartyResourcePreflightPolicy.shouldClassify(
                    adBlock,
                    isHttpOrHttpsUrl(resourceUrl),
                    isHttpOrHttpsUrl(pageUrl),
                    isTrustedDownloadIntentUrl(resourceUrl),
                    isYoutubeCoreUrl(resourceUrl))) return false;
            boolean hardAd = isKnownPopupHost(resourceUrl) || isAdUrl(resourceUrl);
            return ReaderCompatibilityPolicy.shouldBlockThirdPartyResource(
                    resourceUrl, pageUrl, hardAd, isCompatibilityContentAssetUrl(resourceUrl));
        } catch (Exception e) {
            return false;
        }
    }

    boolean isContextAllowedSuspiciousMainFrameNavigation(String targetUrl, String currentUrl, boolean hasGesture) {
        try {
            if (!isHttpOrHttpsUrl(targetUrl)) return false;
            if (isExternalSchemeUrl(targetUrl) || isMediaResourceUrl(targetUrl) || isYoutubeCoreUrl(targetUrl)) return false;
            String targetHost = normalizeHostForAdBlock(targetUrl);
            String currentHost = normalizeHostForAdBlock(currentUrl);
            if (targetHost.length() == 0) return false;

            boolean sameSite = currentHost.length() > 0 && sameOrSubDomain(targetHost, currentHost);
            boolean fromSearch = isSearchEngineResultNavigation(targetUrl, currentUrl);
            boolean suspicious = isKnownPopupHost(targetUrl)
                    || isLikelyAdClickUrl(targetUrl)
                    || isAdUrl(targetUrl)
                    || (currentHost.length() > 0 && isSuspiciousPopupNavigation(targetUrl, currentUrl));

            SuspiciousMainFrameContextPolicy.Decision contextDecision =
                    SuspiciousMainFrameContextPolicy.beforeCompatibility(
                            suspicious, sameSite, fromSearch);
            if (contextDecision.resolved) return contextDecision.allow;

            // v0.9.97: on compatibility/reader pages, a touch event can be stolen by an
            // advertising click-hijacker. Do not treat that inherited user gesture as consent
            // to leave the reader for a suspicious cross-site destination.
            boolean compatibilitySource = isStrictSiteCompatibilityUrl(currentUrl)
                    || isSiteCompatibilityModeActiveForUrl(currentUrl)
                    || isReloadLoopGuardActiveForUrl(currentUrl)
                    || ShieldEngineV2.isPopupIsolationContentPage(currentUrl);
            return SuspiciousMainFrameContextPolicy.allowCrossSite(
                    compatibilitySource,
                    hasGesture,
                    currentHost.length() > 0);
        } catch (Exception e) {
            return false;
        }
    }

    boolean isNormalUserMainFrameNavigation(String targetUrl, String currentUrl, boolean hasGesture) {
        if (!isHttpOrHttpsUrl(targetUrl)) return false;
        if (isExternalSchemeUrl(targetUrl) || isMediaResourceUrl(targetUrl) || isYoutubeCoreUrl(targetUrl)) return false;
        String targetHost = normalizeHostForAdBlock(targetUrl);
        String currentHost = normalizeHostForAdBlock(currentUrl);
        if (targetHost.length() == 0) return false;
        if (currentHost.length() > 0 && sameOrSubDomain(targetHost, currentHost)) return true;
        if (isSearchEngineResultNavigation(targetUrl, currentUrl)) return true;
        if (isContextAllowedSuspiciousMainFrameNavigation(targetUrl, currentUrl, hasGesture)) return true;
        // A gesture on a reader image can be stolen by a direct-link script. Do not promote an
        // unknown cross-site destination to trusted navigation merely because hasGesture is true.
        if (ShieldEngineV2.isReaderOrContentPage(currentUrl)
                || ShieldEngineV2.isPopupIsolationContentPage(currentUrl)
                || ReaderCompatibilityPolicy.hasReaderPathHint(currentUrl)) return false;
        return NormalMainFrameContextPolicy.allowUnknownCrossSite(
                hasGesture,
                isKnownPopupHost(targetUrl),
                isAdUrl(targetUrl),
                isLikelyAdClickUrl(targetUrl));
    }

    boolean isFirstPartyResourceForCurrentPage(String resourceUrl, String pageUrl) {
        try {
            return FirstPartyResourcePolicy.isFirstParty(
                    resourceUrl,
                    pageUrl,
                    YieldWebRuntimeActivity.this::isHttpOrHttpsUrl,
                    YieldWebRuntimeActivity.this::normalizeHostForAdBlock,
                    YieldWebRuntimeActivity.this::sameOrSubDomain);
        } catch (Exception e) {
            return false;
        }
    }

    boolean isTrustedDownloadIntentUrl(String url) {
        try {
            return TrustedDownloadIntentPolicy.isTrusted(
                    url,
                    YieldWebRuntimeActivity.this::isHttpOrHttpsUrl,
                    YieldWebRuntimeActivity.this::normalizeHostForAdBlock,
                    YieldWebRuntimeActivity.this::isTrustedDownloadHostForAllow,
                    YieldWebRuntimeActivity.this::hasTrustedDownloadMarker,
                    YieldWebRuntimeActivity.this::hasDirectFileDownloadExtension,
                    YieldWebRuntimeActivity.this::hasHardAdClickToken,
                    YieldWebRuntimeActivity.this::isSuspiciousAdHostForDownloadAllow);
        } catch (Exception e) {
            return false;
        }
    }

    boolean isTrustedDownloadHostForAllow(String host) {
        return DownloadUrlPolicy.isTrustedDownloadHostForAllow(host);
    }

    boolean hasTrustedDownloadMarker(String u) {
        return DownloadUrlPolicy.hasTrustedDownloadMarker(u);
    }

    boolean hasDirectFileDownloadExtension(String u) {
        return DownloadUrlPolicy.hasDirectFileDownloadExtension(u);
    }

    boolean hasHardAdClickToken(String u) {
        return DownloadUrlPolicy.hasHardAdClickToken(u);
    }

    boolean isSuspiciousAdHostForDownloadAllow(String host) {
        return DownloadUrlPolicy.isSuspiciousAdHostForDownloadAllow(host);
    }

    boolean isUnsafeUrl(String url) {
        String u = url.toLowerCase();
        return u.contains("phishing") || u.contains("malware") || u.contains("virus") || u.contains("scam");
    }

    String normalizeHostForAdBlock(String url) {
        return AdBlockHostPolicy.normalize(url, BrowserUtils::getHostLower);
    }

    boolean sameOrSubDomain(String host, String baseHost) {
        return AdBlockHostPolicy.sameOrSubDomain(host, baseHost);
    }

    boolean isKnownPopupHost(String url) {
        return PopupHostPolicy.isKnown(
                url,
                normalizeHostForAdBlock(url),
                YieldWebRuntimeActivity.this::isTrustedDownloadIntentUrl);
    }

    boolean isHttpOrHttpsUrl(String url) {
        return UrlSchemePolicy.isHttpOrHttps(url);
    }

    // ===== HTTPS-First Navigation (v0.9.98) =====
    void clearAllHttpsFirstRuntimeState() {
        try {
            for (TabInfo tab : tabs) clearHttpsFirstPendingState(tab, true);
        } catch (Exception ignored) {
        }
    }

    void clearHttpsFirstPendingState(TabInfo tab, boolean clearFallbackGuard) {
        if (tab == null) return;
        tab.pendingHttpsOriginalUrl = "";
        tab.pendingHttpsUpgradeUrl = "";
        tab.pendingHttpsStartedAtMs = 0L;
        tab.httpsFallbackInProgress = false;
        if (clearFallbackGuard) {
            tab.httpsFallbackHost = "";
            tab.httpsFallbackAllowedUntilMs = 0L;
        }
    }

    boolean isHttpUrl(String url) {
        return UrlSchemePolicy.isHttp(url);
    }

    boolean isHttpsUrl(String url) {
        return UrlSchemePolicy.isHttps(url);
    }

    boolean isPrivateIpv4Host(String host) {
        return LocalHostPolicy.isPrivateIpv4(host);
    }

    boolean isLocalOrPrivateHost(String host) {
        return LocalHostPolicy.isLocalOrPrivate(host);
    }

    boolean isHttpsFirstExemptUrl(String url) {
        return HttpsFirstExemptionPolicy.isExempt(
                url,
                YieldWebRuntimeActivity.this::isHttpUrl,
                YieldWebRuntimeActivity.this::isLocalOrPrivateHost);
    }

    String buildHttpsUpgradeUrl(String httpUrl) {
        return HttpsFirstUpgradePolicy.upgrade(
                httpUrl,
                YieldWebRuntimeActivity.this::isHttpUrl,
                YieldWebRuntimeActivity.this::isHttpsFirstExemptUrl);
    }

    boolean isHttpsFallbackGuardActive(TabInfo tab, String httpUrl) {
        if (tab == null) return false;
        return HttpsFallbackGuardPolicy.isActive(
                httpUrl,
                tab.httpsFallbackAllowedUntilMs,
                System.currentTimeMillis(),
                tab.httpsFallbackHost,
                YieldWebRuntimeActivity.this::isHttpUrl,
                YieldWebRuntimeActivity.this::hostOfUrl);
    }

    String prepareHttpsFirstNavigation(String rawUrl, TabInfo tab) {
        HttpsNavigationPreparePolicy.Result result = HttpsNavigationPreparePolicy.prepare(
                rawUrl,
                httpsFirstEnabled,
                tab != null,
                tab != null && tab.httpsFallbackInProgress,
                tab != null ? tab.pendingHttpsOriginalUrl : "",
                YieldWebRuntimeActivity.this::isHttpUrl,
                YieldWebRuntimeActivity.this::isHttpsFirstExemptUrl,
                url -> isHttpsFallbackGuardActive(tab, url),
                YieldWebRuntimeActivity.this::buildHttpsUpgradeUrl);
        if (tab != null) {
            if (result.consumeFallbackInProgress) tab.httpsFallbackInProgress = false;
            if (result.clearPendingState) {
                tab.pendingHttpsOriginalUrl = "";
                tab.pendingHttpsUpgradeUrl = "";
                tab.pendingHttpsStartedAtMs = 0L;
            } else if (result.startPendingState) {
                tab.pendingHttpsOriginalUrl = rawUrl.trim();
                tab.pendingHttpsUpgradeUrl = result.targetUrl;
                tab.pendingHttpsStartedAtMs = System.currentTimeMillis();
            }
        }
        return result.targetUrl;
    }

    boolean startHttpsFirstOverrideIfNeeded(WebView view, String targetUrl, TabInfo tab) {
        String secure = HttpsOverrideStartPolicy.resolve(
                view != null,
                targetUrl,
                YieldWebRuntimeActivity.this::isHttpUrl,
                url -> prepareHttpsFirstNavigation(url, tab));
        if (secure == null) return false;
        markTrustedMainFrameNavigation(secure);
        prepareTabForMainFrameNavigation(tab, secure);
        if (tab != null) {
            tab.url = secure;
            tab.currentPageUrlForRequest = secure;
        }
        if (view == webView && addressBar != null) addressBar.setText(secure);
        loadBrowserUrl(secure);
        return true;
    }

    boolean isHttpsFallbackEligibleError(int errorCode) {
        return BrowserUtils.isHttpsFallbackEligibleError(errorCode);
    }

    boolean handleHttpsFirstMainFrameFailure(WebView view, String failedUrl, int errorCode) {
        try {
            if (!HttpsNavigationFailurePolicy.passesPreflight(
                    httpsFirstEnabled,
                    view != null,
                    failedUrl,
                    errorCode,
                    YieldWebRuntimeActivity.this::isHttpsUrl,
                    YieldWebRuntimeActivity.this::isHttpsFallbackEligibleError)) return false;
            TabInfo tab = findTabByWebView(view);
            if (tab == null && view == webView) tab = getCurrentTab();
            if (tab == null || !HttpsNavigationFailurePolicy.hasRelatedPendingFailure(
                    tab.pendingHttpsOriginalUrl,
                    tab.pendingHttpsUpgradeUrl,
                    failedUrl,
                    YieldWebRuntimeActivity.this::hostOfUrl,
                    YieldWebRuntimeActivity.this::sameOrSubDomain)) return false;

            String fallback = tab.pendingHttpsOriginalUrl;
            tab.httpsFallbackHost = hostOfUrl(fallback);
            tab.httpsFallbackAllowedUntilMs = System.currentTimeMillis() + 300000L;
            tab.httpsFallbackInProgress = true;
            tab.pendingHttpsUpgradeUrl = "";
            tab.pendingHttpsStartedAtMs = 0L;
            tab.url = fallback;
            tab.currentPageUrlForRequest = fallback;
            if (view == webView && addressBar != null) addressBar.setText(fallback);
            try { view.stopLoading(); } catch (Exception ignored) {}
            final TabInfo expectedTab = tab;
            mainHandler.post(() -> {
                if (expectedTab.closed || expectedTab.webView != view || view != webView) return;
                loadBrowserUrl(fallback);
            });
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    boolean equivalentUrlIgnoringSchemeAndFragment(String a, String b) {
        return EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(a, b);
    }

    void upgradeBookmarksAfterHttpsSuccess(String originalHttpUrl, String finalHttpsUrl) {
        if (!HttpsBookmarkUpgradePolicy.isEligible(
                originalHttpUrl,
                finalHttpsUrl,
                YieldWebRuntimeActivity.this::isHttpUrl,
                YieldWebRuntimeActivity.this::isHttpsUrl)) return;
        boolean changed = false;
        for (BookmarkItemData item : bookmarkData) {
            if (item == null || !HttpsBookmarkUpgradePolicy.shouldUpgrade(
                    item.url,
                    originalHttpUrl,
                    finalHttpsUrl,
                    YieldWebRuntimeActivity.this::isHttpUrl,
                    YieldWebRuntimeActivity.this::buildHttpsUpgradeUrl,
                    YieldWebRuntimeActivity.this::equivalentUrlIgnoringSchemeAndFragment)) continue;
            item.url = finalHttpsUrl;
            changed = true;
        }
        if (changed) saveBookmarkData();
    }

    void handleHttpsFirstNavigationSuccess(TabInfo tab, String finalUrl) {
        if (tab == null) return;
        HttpsNavigationSuccessPolicy.Action action = HttpsNavigationSuccessPolicy.evaluate(
                finalUrl,
                tab.pendingHttpsOriginalUrl,
                tab.pendingHttpsUpgradeUrl,
                tab.httpsFallbackHost,
                YieldWebRuntimeActivity.this::isHttpsUrl,
                YieldWebRuntimeActivity.this::hostOfUrl,
                YieldWebRuntimeActivity.this::sameOrSubDomain);
        if (action == HttpsNavigationSuccessPolicy.Action.IGNORE
                || action == HttpsNavigationSuccessPolicy.Action.KEEP_FALLBACK) return;
        if (action == HttpsNavigationSuccessPolicy.Action.CLEAR_FALLBACK) {
            tab.httpsFallbackHost = "";
            tab.httpsFallbackAllowedUntilMs = 0L;
            return;
        }
        boolean related = action == HttpsNavigationSuccessPolicy.Action.COMPLETE_RELATED;
        if (related) upgradeBookmarksAfterHttpsSuccess(tab.pendingHttpsOriginalUrl, finalUrl);
        clearHttpsFirstPendingState(tab, related);
    }

    boolean isExternalSchemeUrl(String url) {
        return NavigationUrlSignalPolicy.isExternalScheme(url);
    }

    boolean isLikelyAdClickUrl(String url) {
        return NavigationUrlSignalPolicy.isLikelyAdClick(
                url,
                YieldWebRuntimeActivity.this::isMediaResourceUrl,
                YieldWebRuntimeActivity.this::isYoutubeCoreUrl,
                YieldWebRuntimeActivity.this::isTrustedDownloadIntentUrl);
    }

    void restoreAfterBlockedNavigation(WebView view, String blockedUrl) {
        try {
            TabInfo owner = findTabByWebView(view);
            if (owner == null && view == webView) owner = getCurrentTab();
            final TabInfo targetTab = owner;
            final WebView targetView = view;
            final long targetGeneration = targetTab != null ? targetTab.webViewGeneration : -1L;
            String tabSafeUrl = getTabReferenceUrl(targetTab);
            if ((tabSafeUrl == null || tabSafeUrl.length() == 0)
                    && isCurrentTabInfo(targetTab) && lastSafeHttpUrl != null) {
                tabSafeUrl = lastSafeHttpUrl;
            }

            if (isTrustedDownloadIntentUrl(blockedUrl)) {
                return;
            }
            boolean shieldRelay = blockedUrl != null && tabSafeUrl != null
                    && ShieldEngineV2.isHighConfidenceSameOriginRelay(blockedUrl, tabSafeUrl,
                    isShieldReaderOrCompatibilityContext(tabSafeUrl));
            boolean shieldBlockedMainFrame = blockedUrl != null && tabSafeUrl != null
                    && ShieldEngineV2.shouldBlockMainFrameNavigation(blockedUrl, tabSafeUrl, false,
                    isShieldReaderOrCompatibilityContext(tabSafeUrl), false, false);
            boolean transientBlankNavigation = ReaderCompatibilityPolicy.isTransientBlankUrl(blockedUrl);
            boolean directImageAgainstSafeUrl = isDirectImageMainFrameNavigation(blockedUrl, tabSafeUrl);
            // A short-lived tab domain-switch allowance must not whitelist a stolen reader touch.
            // Honor trusted navigation only when the reader boundary did not classify the target.
            if ((isTrustedMainFrameNavigation(blockedUrl)
                    || isTrustedMainFrameNavigationForTab(targetTab, blockedUrl))
                    && !shieldBlockedMainFrame && !transientBlankNavigation
                    && !directImageAgainstSafeUrl) {
                return;
            }
            boolean blockedIsAdLike = shieldRelay || shieldBlockedMainFrame || transientBlankNavigation
                    || isExternalSchemeUrl(blockedUrl) || isKnownPopupHost(blockedUrl)
                    || isLikelyAdClickUrl(blockedUrl) || isSuspiciousPopupNavigation(blockedUrl, tabSafeUrl);
            if (!blockedIsAdLike && (isSiteCompatibilityModeActiveForUrl(blockedUrl) || isSiteCompatibilityModeActiveForUrl(tabSafeUrl)
                    || isReloadLoopGuardActiveForUrl(blockedUrl) || isReloadLoopGuardActiveForUrl(tabSafeUrl))) {
                // v0.9.44/v0.9.46: jangan stopLoading/goBack pada host kompatibel jika targetnya
                // memang halaman situs utama. Popup iklan tetap boleh dipindahkan ke tab sementara.
                return;
            }
            String currentBefore = view != null ? view.getUrl() : "";
            boolean navigationAlreadyChanged = blockedUrl != null
                    && currentBefore != null
                    && currentBefore.length() > 0
                    && blockedUrl.equals(currentBefore);
            boolean directImageNavigation = directImageAgainstSafeUrl
                    || isDirectImageMainFrameNavigation(blockedUrl, currentBefore);

            // Ad/direct image dipisah ke tab baru, tab utama tidak di-reload agar gambar/komik
            // yang sedang loading tidak berubah menjadi halaman .jpg/.jpeg mentah.
            if (directImageNavigation) {
                captureDirectImageToTempTab(blockedUrl);
            } else if (!transientBlankNavigation) {
                captureAdRedirectToTempTab(blockedUrl);
            }

            if (view != null && (navigationAlreadyChanged || directImageNavigation
                    || transientBlankNavigation || isExternalSchemeUrl(blockedUrl))) {
                view.stopLoading();
            }

            final String fallbackUrl = tabSafeUrl;
            if (isCurrentTabInfo(targetTab) && fallbackUrl != null && fallbackUrl.length() > 0) {
                addressBar.setText(fallbackUrl);
            }

            mainHandler.postDelayed(() -> {
                try {
                    WebView restoreView = targetView;
                    if (restoreView == null && targetTab != null) restoreView = targetTab.webView;
                    if (!isLiveTabWebView(targetTab, restoreView, targetGeneration)) return;

                    String current = restoreView.getUrl();
                    boolean currentBad = ReaderCompatibilityPolicy.isTransientBlankUrl(current)
                            || isExternalSchemeUrl(current)
                            || isLikelyAdClickUrl(current)
                            || isDirectImageMainFrameNavigation(current, fallbackUrl)
                            || ShieldEngineV2.isHighConfidenceSameOriginRelay(current, fallbackUrl,
                            isShieldReaderOrCompatibilityContext(fallbackUrl))
                            || (blockedUrl != null && blockedUrl.equals(current));

                    // Recover hanya pada WebView pemilik tab tersebut. Jangan pakai webView global,
                    // karena global bisa sudah menunjuk tab lain setelah close/switch tab.
                    if (currentBad) {
                        boolean deterministicFallback = fallbackUrl != null && fallbackUrl.length() > 0
                                && (isStrictSiteCompatibilityUrl(fallbackUrl)
                                || isSiteCompatibilityModeActiveForUrl(fallbackUrl)
                                || isReloadLoopGuardActiveForUrl(fallbackUrl)
                                || isShieldReaderOrCompatibilityContext(fallbackUrl)
                                || ReaderCompatibilityPolicy.isTransientBlankUrl(current));
                        // goBack can return to an intermediate about:blank/ad history entry.
                        // Reader pages and transient blank documents always recover directly to
                        // the tab-owned last safe URL instead of walking polluted WebView history.
                        if (deterministicFallback) {
                            if (targetTab != null) prepareTabForMainFrameNavigation(targetTab, fallbackUrl);
                            restoreView.loadUrl(fallbackUrl);
                        } else if (restoreView.canGoBack()) {
                            restoreView.goBack();
                        } else if (fallbackUrl != null && fallbackUrl.length() > 0) {
                            if (targetTab != null) prepareTabForMainFrameNavigation(targetTab, fallbackUrl);
                            restoreView.loadUrl(fallbackUrl);
                        }
                    }

                    if (isCurrentTabInfo(targetTab) && fallbackUrl != null && fallbackUrl.length() > 0) {
                        addressBar.setText(fallbackUrl);
                    }
                } catch (Exception ignored) {
                }
            }, 120);
        } catch (Exception ignored) {
        }
    }

    boolean isSuspiciousPopupNavigation(String targetUrl, String currentUrl) {
        if (targetUrl == null || targetUrl.length() == 0) return false;
        String lower = targetUrl.toLowerCase(Locale.US);
        if (lower.startsWith("about:") || lower.startsWith("javascript:")
                || lower.startsWith("data:") || lower.startsWith("blob:")) {
            return false;
        }

        // Video playback tidak boleh diblokir oleh AdBlock.
        if (isMediaResourceUrl(lower) || isYoutubeCoreUrl(lower) || isTrustedDownloadIntentUrl(targetUrl)) return false;
        if (isExternalSchemeUrl(lower) || isLikelyAdClickUrl(lower)) return true;

        String targetHost = normalizeHostForAdBlock(targetUrl);
        String currentHost = normalizeHostForAdBlock(currentUrl);
        if (targetHost.length() == 0) return false;
        if (sameOrSubDomain(targetHost, currentHost)) return false;

        if (isKnownPopupHost(targetUrl) || isAdUrl(targetUrl)) return true;

        // Banyak iklan click hijack memakai domain acak murah dengan path token panjang.
        if ((targetHost.endsWith(".shop") || targetHost.endsWith(".xyz") || targetHost.endsWith(".top")
                || targetHost.endsWith(".site") || targetHost.endsWith(".space") || targetHost.endsWith(".online")
                || targetHost.endsWith(".live") || targetHost.endsWith(".fun") || targetHost.endsWith(".lol"))
                && lower.matches(".*https?://[^/]+/[a-z0-9_-]{8,}.*")) {
            return true;
        }

        return false;
    }

    boolean isYoutubeCoreUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);
        return u.contains("youtube.com/")
                || u.contains("m.youtube.com/")
                || u.contains("www.youtube.com/")
                || u.contains("youtu.be/")
                || u.contains("youtubei/v1/")
                || u.contains("ytimg.com/")
                || u.contains("ggpht.com/")
                || u.contains("googlevideo.com/");
    }

    boolean isMediaResourceUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);
        return u.contains("googlevideo.com/videoplayback")
                || u.contains("youtube.com/videoplayback")
                || u.contains("/videoplayback")
                || u.contains(".mp4")
                || u.contains(".m3u8")
                || u.contains(".mpd")
                || u.contains(".webm")
                || u.contains(".mkv")
                || u.contains(".mov")
                || u.contains(".avi")
                || u.contains(".ts")
                || u.contains(".m4s")
                || u.contains("mime=video")
                || u.contains("mime%3dvideo")
                || u.contains("content-type=video")
                || u.contains("application/vnd.apple.mpegurl")
                || u.contains("application/x-mpegurl");
    }

    boolean isImageResourceUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);
        return u.contains(".jpg")
                || u.contains(".jpeg")
                || u.contains(".png")
                || u.contains(".webp")
                || u.contains(".gif")
                || u.contains("image/jpeg")
                || u.contains("image/png")
                || u.contains("image/webp")
                || u.contains("image/gif");
    }

    boolean isDirectImageMainFrameNavigation(String targetUrl, String currentUrl) {
        if (!isImageResourceUrl(targetUrl)) return false;
        if (currentUrl == null || currentUrl.length() == 0) return false;
        if (!isHttpOrHttpsUrl(currentUrl)) return false;
        return !isImageResourceUrl(currentUrl);
    }

    WebResourceResponse buildBlockedResponse(String url) {
        try {
            boolean image = isImageResourceUrl(url);
            String mime = image ? "image/gif" : "text/plain";
            byte[] body = image ? TRANSPARENT_GIF_BYTES : new byte[0];
            WebResourceResponse resp = new WebResourceResponse(mime, "utf-8", new ByteArrayInputStream(body));
            try {
                Map<String, String> headers = new HashMap<>();
                headers.put("Access-Control-Allow-Origin", "*");
                headers.put("Cache-Control", "no-store");
                resp.setResponseHeaders(headers);
                resp.setStatusCodeAndReasonPhrase(200, "OK");
            } catch (Exception ignored) {
            }
            return resp;
        } catch (Exception e) {
            return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
        }
    }

    boolean isAdUrl(String url) {
        if (!adBlock || url == null) return false;
        String u = url.toLowerCase(Locale.US);

        if (isTrustedDownloadIntentUrl(url)) return false;
        if (isMediaResourceUrl(u)) return false;
        if (isYoutubeCoreUrl(u)) return false;

        for (String b : AD_URL_HOST_PATTERNS) {
            if (u.contains(b)) return true;
        }

        return isPopUnderOrAdAsset(u);
    }

    boolean isPopUnderOrAdAsset(String u) {
        if (isTrustedDownloadIntentUrl(u)) return false;
        return u.contains("/ads?")
                || u.contains("/ads/")
                || u.contains("/adserver/")
                || u.contains("/adservice/")
                || u.contains("/advertisement/")
                || u.contains("/banner/")
                || u.contains("popunder")
                || u.contains("popupads")
                || u.contains("sponsor")
                || u.contains("trackingpixel")
                || u.contains("prebid")
                || u.contains("clickunder")
                || u.contains("onclickads")
                || u.contains("interstitial")
                || u.contains("adclick")
                || u.contains("ad_click")
                || u.contains("adurl=")
                || u.contains("utm_medium=affiliates")
                || u.contains("deep_and_deferred")
                || u.contains("navigate_url=")
                || u.contains("reactpath")
                || u.contains("click_id")
                || u.contains("af_click")
                || u.contains("/vast")
                || u.contains("vast.xml")
                || u.contains("=vast")
                || u.contains("vpaid");
    }

    // ===== AdBlock / video ad handling =====
    // v0.9.92: cosmetic filtering profesional (gaya Brave/UC).
    // Daftar selector ini KURATIF dan aman: tidak lagi memakai pola blanket seperti
    // [class*=ad-] / [id*=ad-] yang pada versi lama keliru menyembunyikan elemen sah seperti
    // download-button, upload-zone, read-more, breadcrumb, header, thread, dll.
    // Memakai token-match [class~='...'] (mencocokkan kata utuh, bukan substring) + selector
    // kelas iklan spesifik + iframe iklan berdasarkan src.
    String buildAdBlockCosmeticCss() {
        return BrowserUtils.buildAdBlockCosmeticCss();
    }

    // Injektor stylesheet idempoten: membuat sekali, lalu hanya memperbarui isi jika berubah.
    // CSS memakai tanda kutip tunggal pada attribute selector sehingga aman ditempel di string JS.
    String buildCosmeticCssInjectorJs() {
        String css = buildAdBlockCosmeticCss();
        return "javascript:(function(){try{"
                + "var css=\"" + css + "\";"
                + "var id='yield-adblock-cosmetic';"
                + "var el=document.getElementById(id);"
                + "if(!el){el=document.createElement('style');el.id=id;el.setAttribute('type','text/css');(document.head||document.documentElement||document.body).appendChild(el);}"
                + "if(el.textContent!==css){el.textContent=css;}"
                + "}catch(e){}})();";
    }

    // Dipanggil sangat awal (onPageCommitVisible) supaya iklan tersembunyi sebelum sempat tampil,
    // sehingga tidak ada kedipan (FOUC) seperti browser profesional. Hanya stylesheet, tanpa logika berat.
    void injectAdBlockCssEarly() {
        if (webView == null || !adBlock || !adBlockScriptIframeBlocker) return;
        try {
            String cur = getEffectiveCurrentUrl();
            if (isYouTubePageUrl(cur)) return;
            if (isSiteCompatibilityModeActiveForUrl(cur)) return;
            runPageScript(buildCosmeticCssInjectorJs());
        } catch (Exception ignored) {
        }
    }

    // ===== v0.9.92: "Blokir elemen" (element picker, gaya uBlock/Brave) =====
    // Escape string agar aman ditempel di dalam literal JS berkutip-ganda.

    void loadUserElementFilters() {
        if (userElementFiltersLoaded) return;
        userElementFiltersLoaded = true;
        UserElementFilterStore.load(
                getSharedPreferences(PREFS, MODE_PRIVATE), userElementFilters);
    }

    void persistUserElementFiltersForHost(String host) {
        UserElementFilterStore.persistHost(
                getSharedPreferences(PREFS, MODE_PRIVATE), userElementFilters, host);
    }

    LinkedHashSet<String> userFiltersForHost(String host) {
        loadUserElementFilters();
        return UserElementFilterStore.filtersForHost(userElementFilters, host);
    }

    boolean hasUserFiltersForCurrentHost() {
        LinkedHashSet<String> s = userFiltersForHost(hostOfUrl(getEffectiveCurrentUrl()));
        return s != null && !s.isEmpty();
    }

    boolean addUserElementFilter(String host, String selector) {
        loadUserElementFilters();
        boolean added = UserElementFilterStore.add(userElementFilters, host, selector);
        if (added) persistUserElementFiltersForHost(host);
        return added;
    }

    void removeUserElementFilter(String host, String selector) {
        loadUserElementFilters();
        if (UserElementFilterStore.remove(userElementFilters, host, selector)) {
            persistUserElementFiltersForHost(host);
        }
    }

    String buildUserFilterCss(String host) {
        loadUserElementFilters();
        return UserElementFilterStore.buildCss(userElementFilters, host);
    }

    // Filter manual merupakan fitur mandiri: selalu diterapkan walaupun AdBlock utama OFF dan
    // tetap aktif pada compatibility mode. CSS tersimpan per host dan dipasang ulang bila situs
    // mengganti <head> atau menghapus style saat SPA/dynamic navigation.
    void applyUserFiltersForCurrentPage() {
        if (webView == null) return;
        try {
            String host = hostOfUrl(getEffectiveCurrentUrl());
            runPageScript(UserElementFilterStore.buildPageScript(buildUserFilterCss(host)));
        } catch (Exception ignored) {
        }
    }

    String buildElementPickerJs() {
        return ElementPickerScript.build();
    }

    void startElementPicker() {
        String url = getEffectiveCurrentUrl();
        boolean httpPage = url != null && (url.startsWith("http://") || url.startsWith("https://"));
        if (!httpPage || webView == null || webView.getVisibility() != View.VISIBLE) {
            QuietToast.makeText(this, "Buka halaman web dulu untuk memblokir elemen", QuietToast.LENGTH_SHORT).show();
            return;
        }
        elementPickerActive = true;
        if (elementPickerDialog != null) {
            try { elementPickerDialog.dismiss(); } catch (Exception ignored) {}
            elementPickerDialog = null;
        }
        runPageScript(buildElementPickerJs());
        QuietToast.makeText(this, "Ketuk beberapa elemen; tekan X di bar atas untuk selesai", QuietToast.LENGTH_LONG).show();
    }

    void onPickerElementSelected(String selector, String preview,
                                         int matchCount, String selectedTag) {
        if (!elementPickerActive) return;
        if (!UserElementFilterPolicy.isSafeSelector(selector, selectedTag)) {
            QuietToast.makeText(this,
                    "Elemen penting atau selector tidak aman tidak dapat diblokir",
                    QuietToast.LENGTH_SHORT).show();
            continueElementPickerAfterSelection();
            return;
        }
        final String cleanSelector = selector.trim();
        final String host = hostOfUrl(getEffectiveCurrentUrl());
        if (host == null || host.length() == 0) {
            QuietToast.makeText(this,
                    "Tidak bisa menentukan domain halaman ini",
                    QuietToast.LENGTH_SHORT).show();
            finishElementPicker(false);
            return;
        }
        if (elementPickerDialog != null) {
            try {
                elementPickerDialog.dismiss();
            } catch (Exception ignored) {
            }
            elementPickerDialog = null;
        }
        if (elementFilterDialogController == null) {
            elementFilterDialogController = new ElementFilterDialogController(this);
        }
        elementPickerDialog = elementFilterDialogController.createPickerDialog(
                cleanSelector,
                preview,
                matchCount,
                host,
                new ElementFilterDialogController.PickerCallback() {
                    @Override
                    public void onBlock(String targetHost, String targetSelector) {
                        boolean added = addUserElementFilter(targetHost, targetSelector);
                        applyUserFiltersForCurrentPage();
                        continueElementPickerAfterSelection();
                        QuietToast.makeText(YieldWebRuntimeActivity.this,
                                added ? "Elemen diblokir permanen di " + targetHost
                                        : "Filter ini sudah tersimpan di " + targetHost,
                                QuietToast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onParentRequested() {
                        elementPickerDialog = null;
                        mainHandler.postDelayed(() -> runPageScript(
                                "javascript:(function(){try{if(window.__yieldPickerParent)window.__yieldPickerParent();}catch(e){}})();"
                        ), 80L);
                    }

                    @Override
                    public void onContinueRequested() {
                        continueElementPickerAfterSelection();
                    }
                });
        elementPickerDialog.setOnDismissListener(dialog -> {
            if (elementPickerDialog == dialog) elementPickerDialog = null;
        });
        try {
            elementPickerDialog.show();
        } catch (Exception ignored) {
        }
    }

    void continueElementPickerAfterSelection() {
        if (!elementPickerActive) return;
        if (elementPickerDialog != null) {
            try { elementPickerDialog.dismiss(); } catch (Exception ignored) {}
            elementPickerDialog = null;
        }
        runPageScript("javascript:(function(){try{if(window.__yieldPickerContinue)window.__yieldPickerContinue();}catch(e){}})();");
    }

    void finishElementPicker(boolean committed) {
        boolean wasActive = elementPickerActive;
        elementPickerActive = false;
        if (elementPickerDialog != null) {
            try { elementPickerDialog.dismiss(); } catch (Exception ignored) {}
            elementPickerDialog = null;
        }
        if (!wasActive && webView == null) return;
        String fn = committed ? "__yieldPickerCommit" : "__yieldPickerCancel";
        runPageScript("javascript:(function(){try{if(window." + fn + ")window." + fn + "();}catch(e){}})();");
    }

    void showUserFiltersManager() {
        final String host = hostOfUrl(getEffectiveCurrentUrl());
        loadUserElementFilters();
        LinkedHashSet<String> selectors =
                host == null || host.length() == 0 ? null : userElementFilters.get(host);
        if (elementFilterDialogController == null) {
            elementFilterDialogController = new ElementFilterDialogController(this);
        }
        elementFilterDialogController.showManager(
                host,
                selectors,
                new ElementFilterDialogController.ManagerCallback() {
                    @Override
                    public void onRemove(String targetHost, String selector) {
                        removeUserElementFilter(targetHost, selector);
                        applyUserFiltersForCurrentPage();
                    }

                    @Override
                    public void onClear(String targetHost) {
                        if (UserElementFilterStore.clearHost(userElementFilters, targetHost)) {
                            persistUserElementFiltersForHost(targetHost);
                            applyUserFiltersForCurrentPage();
                        }
                        QuietToast.makeText(YieldWebRuntimeActivity.this,
                                "Filter situs dihapus",
                                QuietToast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onRefreshRequested() {
                        showUserFiltersManager();
                    }
                });
    }

    void injectPremiumAdBlock() {
        if (webView == null || !adBlock) return;
        // v0.9.54: YouTube punya engine adblock khusus yang lebih aman.
        // Jangan jalankan AdBlock Premium global di YouTube karena script umum bisa salah
        // membaca video utama sebagai iklan lalu mempercepat/mute video asli.
        if (isYouTubePageUrl(getEffectiveCurrentUrl())) {
            injectYouTubeSafeAdBlockV6();
            return;
        }
        if (isSiteCompatibilityModeActiveForUrl(getEffectiveCurrentUrl())) return;

        // Pasang stylesheet cosmetic lebih dulu (hide-before-paint, gaya Brave/UC).
        injectAdBlockCssEarly();

        String popupEnabled = adBlockPopupBlocker ? "true" : "false";
        // Reader/content pages use the DOM-aware Shield Engine V2 click guard only. The older
        // premium listener is intentionally disabled there so one touch is not consumed twice
        // before a legitimate Next/Previous Chapter control receives it.
        String clickEnabled = ShieldEngineV2.shouldUseLegacyClickGuard(
                getEffectiveCurrentUrl(), adBlockClickHijackBlocker) ? "true" : "false";
        String scriptEnabled = adBlockScriptIframeBlocker ? "true" : "false";

        String js = BrowserPageScripts.premiumAdBlock(popupEnabled, clickEnabled, scriptEnabled);
        runPageScript(js);
    }

    void injectCompatibilityAdShield() {
        if (webView == null) return;
        String pageUrl = getEffectiveCurrentUrl();
        if (!(isStrictSiteCompatibilityUrl(pageUrl)
                || isSiteCompatibilityModeActiveForUrl(pageUrl)
                || isReloadLoopGuardActiveForUrl(pageUrl)
                || ReaderCompatibilityPolicy.hasReaderPathHint(pageUrl)
                || ShieldEngineV2.isPopupIsolationContentPage(pageUrl))) return;
        injectShieldEngineV2Fallback();
    }

    boolean isYouTubePageUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);
        return u.contains("youtube.com") || u.contains("m.youtube.com") || u.contains("www.youtube.com") || u.contains("youtu.be");
    }

    boolean isYouTubePlaybackUrl(String url) {
        if (isYouTubePageUrl(url)) return true;
        if (currentPageUrlForRequest != null && isYouTubePageUrl(currentPageUrlForRequest)) return true;
        try {
            TabInfo tab = getCurrentTab();
            if (tab != null && isYouTubePageUrl(tab.url)) return true;
        } catch (Exception ignored) {}
        return false;
    }

    void injectYouTubeSafeAdBlockV6() {
        if (webView == null || !adBlock) return;
        if (!isYouTubePlaybackUrl(getEffectiveCurrentUrl())) return;
        // v0.9.87: YouTube assistant khusus iklan.
        // Tidak autoplay saat halaman/video baru dibuka tanpa iklan.
        // Saat iklan terdeteksi: coba maju +10 detik secara periodik, klik Skip/Lewati jika muncul,
        // lalu auto-resume satu kali jika video utama kepause setelah iklan dilewati.
        String js = BrowserPageScripts.youtubeSafeAdBlock();
        runPageScript(js);
    }

    void stopYouTubeAutoAssistantNow() {
        if (webView == null) return;
        if (!isYouTubePlaybackUrl(getEffectiveCurrentUrl())) return;
        // Kill-switch khusus YouTube: saat AdBlock dimatikan, script lama yang sudah
        // terlanjur masuk ke halaman berhenti tanpa perlu reload.
        String js = BrowserPageScripts.stopYouTubeAssistant();
        runPageScript(js);
    }

    void injectReaderMode() {
        String js = "javascript:(function(){document.body.style.maxWidth='720px';document.body.style.margin='auto';document.body.style.lineHeight='1.7';document.body.style.fontSize='18px';document.body.style.background='#111318';document.body.style.color='#F5F7FA';})()";
        runPageScript(js);
    }

    void hideKeyboardAndClearFocus(View focusView) {
        try {
            View target = focusView != null ? focusView : getCurrentFocus();
            if (target != null) target.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                View tokenView = target != null ? target : getWindow().getDecorView();
                imm.hideSoftInputFromWindow(tokenView.getWindowToken(), 0);
            }
            if (webView != null) webView.requestFocus();
        } catch (Exception ignored) {
        }
    }

    void blurWebInputsAndHideKeyboard() {
        hideKeyboardAndClearFocus(webView != null ? webView : getWindow().getDecorView());
        try {
            if (webView != null) {
                webView.evaluateJavascript("(function(){try{if(document.activeElement)document.activeElement.blur();document.querySelectorAll('input,textarea,[contenteditable=true]').forEach(function(e){try{e.blur();}catch(x){}});}catch(e){}})();", null);
            }
        } catch (Exception ignored) {
        }
    }

    void openHomeSearchUrl() {
        if (homeSearchInput == null) {
            openAddressBarUrl();
            return;
        }
        hideKeyboardAndClearFocus(homeSearchInput);
        pendingHideKeyboardAfterNavigation = true;
        String text = homeSearchInput.getText().toString().trim();
        if (text.length() == 0) {
            addressBar.requestFocus();
            return;
        }
        addressBar.setText(text);
        openAddressBarUrl();
    }

    void openAddressBarUrl() {
        hideKeyboardAndClearFocus(addressBar);
        pendingHideKeyboardAfterNavigation = true;
        String text = addressBar.getText().toString().trim();
        if (text.length() == 0) {
            navigateCurrentTabHome();
            return;
        }
        String url;
        boolean barePublicHost = !text.startsWith("http://")
                && !text.startsWith("https://")
                && text.contains(".")
                && !text.contains(" ");
        if (text.startsWith("http://") || text.startsWith("https://")) {
            url = text;
        } else if (barePublicHost) {
            // Keep HTTPS as the visible/default destination, but retain an HTTP
            // candidate so HTTPS-First can fall back only after a real connection
            // failure. When HTTPS-First is disabled, schemeless public hosts still
            // default to HTTPS rather than silently downgrading.
            url = httpsFirstEnabled ? "http://" + text : "https://" + text;
        } else {
            url = buildSearchUrl(text);
        }

        TabInfo currentTab = getCurrentTab();
        url = prepareHttpsFirstNavigation(url, currentTab);
        if (addressBar != null) addressBar.setText(url);
        currentTab.url = url;
        currentTab.title = url;
        saveTabsSession();
        boolean fromHomeSearch = homeScroll != null && homeScroll.getVisibility() == View.VISIBLE;
        if (fromHomeSearch) {
            startSmoothSearchTransition();
        } else {
            if (webView != null) {
                webView.setAlpha(1f);
                webView.setVisibility(View.VISIBLE);
            }
            if (homeScroll != null) homeScroll.setVisibility(View.GONE);
        }
        updateVideoControlsVisibility();
        if (shouldRecordHistoryUrl(url)) addBrowserHistory(url, url);
        applyBrowserSettings();
        if (translateEnabled && !translateManuallyDisabled) loadTranslatedPage(url);
        else loadBrowserUrl(url);
        if (!desktopMode) {
            mainHandler.postDelayed(() -> applyMobileViewportIfNeeded(), 300);
            mainHandler.postDelayed(() -> applyMobileViewportIfNeeded(), 1200);
        }
        if (fromHomeSearch) {
            mainHandler.postDelayed(() -> finishSmoothSearchTransition(), 1400);
        }
        updateTopActionStates();
        updateTabsCountUi();
    }

    String buildSearchUrl(String query) {
        String q;
        try {
            q = URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            q = query.replace(" ", "+");
        }

        if ("Bing".equals(searchEngine)) return "https://www.bing.com/search?q=" + q;
        if ("DuckDuckGo".equals(searchEngine)) return "https://duckduckgo.com/?q=" + q;
        if ("Yahoo".equals(searchEngine)) return "https://search.yahoo.com/search?p=" + q;
        if ("Yandex".equals(searchEngine)) return "https://yandex.com/search/?text=" + q;
        return "https://www.google.com/search?q=" + q;
    }

    void loadShortcuts() {
        shortcutsData.clear();
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);

        if (!p.contains("shortcuts")) {
            shortcutsData.add(new ShortcutItemData("Google", "https://www.google.com"));
            shortcutsData.add(new ShortcutItemData("GitHub", "https://github.com"));
            shortcutsData.add(new ShortcutItemData("YouTube", "https://m.youtube.com"));
            saveShortcuts();
            return;
        }

        String saved = p.getString("shortcuts", "");
        saved = normalizeStoredRows(saved);
        if (saved.length() == 0) return;

        HashSet<String> seenUrls = new HashSet<>();
        String[] lines = saved.split("\n");
        for (String line : lines) {
            if (line == null) continue;
            line = line.trim();
            if (line.length() == 0) continue;
            String[] parts = line.split("\\|", 2);
            if (parts.length == 2) {
                String label = decode(parts[0]).trim();
                String url = normalizeShortcutUrl(decode(parts[1]).trim());
                if (url == null || seenUrls.contains(url)) continue;
                if (label.length() == 0) label = guessLabelFromUrl(url);
                shortcutsData.add(new ShortcutItemData(label, url));
                seenUrls.add(url);
            }
        }

        saveShortcuts();
    }

    String normalizeStoredRows(String saved) {
        if (saved == null || saved.length() == 0) return "";
        return saved.replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    void saveShortcuts() {
        StringBuilder sb = new StringBuilder();
        HashSet<String> seenUrls = new HashSet<>();
        for (ShortcutItemData item : shortcutsData) {
            if (item == null) continue;
            String url = normalizeShortcutUrl(item.url);
            if (url == null || seenUrls.contains(url)) continue;
            String label = item.label == null || item.label.trim().length() == 0 ? guessLabelFromUrl(url) : item.label.trim();
            sb.append(encode(label)).append("|").append(encode(url)).append('\n');
            seenUrls.add(url);
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("shortcuts", sb.toString()).commit();
    }

    boolean isLikelyDesktopOnlyHost(String host) {
        try {
            if (host == null || host.length() == 0) return false;
            String h = host.toLowerCase(Locale.US);
            if (h.startsWith("www.")) h = h.substring(4);
            String[] known = new String[]{
                    "h-metrics.com"
            };
            for (String base : known) {
                if (h.equals(base) || h.endsWith("." + base)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    void scheduleHorizontalGestureGuardCheck(String url) {
        try {
            if (webView == null || !isHttpOrHttpsUrl(url)) return;
            final String expectedHost = hostOfUrl(url);
            if (expectedHost == null || expectedHost.length() == 0) return;
            if (isLikelyDesktopOnlyHost(expectedHost)) {
                webHorizontalGestureGuard = true;
                webHorizontalGestureGuardHost = expectedHost;
            }
            mainHandler.postDelayed(() -> {
                try {
                    if (webView == null) return;
                    String active = getEffectiveCurrentUrl();
                    if (active == null || active.length() == 0) active = currentPageUrlForRequest;
                    String activeHost = hostOfUrl(active);
                    if (!sameOrSubDomain(activeHost, expectedHost)) return;
                    String js = "(function(){try{var d=document.documentElement,b=document.body;"
                            + "var vw=Math.max(window.innerWidth||0,(d&&d.clientWidth)||0);"
                            + "var sw=Math.max((d&&d.scrollWidth)||0,(b&&b.scrollWidth)||0);"
                            + "var hasWide=sw>vw+80;"
                            + "var fixedWide=false;try{document.querySelectorAll('table,canvas,iframe,video,.container,.wrapper,main,body>*').forEach(function(e){if(fixedWide)return;var r=e.getBoundingClientRect&&e.getBoundingClientRect();if(r&&r.width>vw+80)fixedWide=true;});}catch(x){}"
                            + "return (hasWide||fixedWide)?'1':'0';"
                            + "}catch(e){return '0';}})();";
                    webView.evaluateJavascript(js, value -> {
                        try {
                            String current = getEffectiveCurrentUrl();
                            if (current == null || current.length() == 0) current = currentPageUrlForRequest;
                            String currentHost = hostOfUrl(current);
                            if (!sameOrSubDomain(currentHost, expectedHost)) return;
                            String decoded = decodeEvaluateJavascriptString(value);
                            if ("1".equals(decoded) || isLikelyDesktopOnlyHost(currentHost)) {
                                webHorizontalGestureGuard = true;
                                webHorizontalGestureGuardHost = currentHost;
                            } else if (!desktopMode && !isSiteCompatibilityModeActiveForUrl(current) && !isStrictSiteCompatibilityUrl(current)) {
                                webHorizontalGestureGuard = false;
                                webHorizontalGestureGuardHost = currentHost;
                            }
                        } catch (Exception ignored) {
                        }
                    });
                } catch (Exception ignored) {
                }
            }, 900L);
        } catch (Exception ignored) {
        }
    }

    void initializeSwipeNavigation(View root) {
        swipeNavigationController = new SwipeNavigationController(
                this,
                homeScroll,
                webView,
                YieldWebRuntimeActivity.this::shouldProtectWebHorizontalSwipeGesture,
                YieldWebRuntimeActivity.this::restoreHiddenWebPage,
                YieldWebRuntimeActivity.this::showHome);
        swipeNavigationController.install(root);
    }

    boolean shouldProtectWebHorizontalSwipeGesture() {
        try {
            if (webView == null || webView.getVisibility() != View.VISIBLE) return false;
            String url = getEffectiveCurrentUrl();
            if (url == null || url.length() == 0) url = currentPageUrlForRequest;
            String host = hostOfUrl(url);
            String h = host == null ? "" : host.toLowerCase(Locale.US);
            if (h.startsWith("www.")) h = h.substring(4);

            // Domain contoh yang memang tampil desktop dan sering perlu geser horizontal.
            if (h.equals("h-metrics.com") || h.endsWith(".h-metrics.com")) return true;

            // Universal: saat halaman terdeteksi lebih lebar dari viewport atau masuk compatibility/desktop,
            // custom swipe navigation dimatikan agar tidak back otomatis.
            if (webHorizontalGestureGuard) {
                String guardHost = webHorizontalGestureGuardHost == null ? "" : webHorizontalGestureGuardHost;
                if (guardHost.length() == 0 || sameOrSubDomain(host, guardHost)) return true;
            }
            if (desktopMode) return true;
            if (isSiteCompatibilityModeActiveForUrl(url) || isStrictSiteCompatibilityUrl(url) || isReloadLoopGuardActiveForUrl(url)) return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    void restoreHiddenWebPage() {
        try {
            TabInfo tab = getCurrentTab();
            String tabUrl = tab.url == null ? "" : tab.url;
            String currentWebUrl = webView != null ? webView.getUrl() : "";

            if ((currentWebUrl == null || currentWebUrl.length() == 0) && tabUrl.length() == 0) {
                QuietToast.makeText(this, "Belum ada halaman sebelumnya", QuietToast.LENGTH_SHORT).show();
                return;
            }

            homeScroll.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);

            String restoreUrl = currentWebUrl != null && currentWebUrl.length() > 0 ? extractOriginalUrl(currentWebUrl) : tabUrl;
            if (restoreUrl != null && restoreUrl.length() > 0) {
                addressBar.setText(restoreUrl);
            }

            if ((currentWebUrl == null || currentWebUrl.length() == 0) && tabUrl.length() > 0) {
                if (translateEnabled && !translateManuallyDisabled) loadTranslatedPage(tabUrl);
                else loadBrowserUrl(tabUrl);
            }

            updateVideoControlsVisibility();
            updateTopActionStates();
            QuietToast.makeText(this, "Halaman terakhir dibuka lagi", QuietToast.LENGTH_SHORT).show();
        } catch (Exception e) {
            QuietToast.makeText(this, "Tidak ada halaman untuk dibuka", QuietToast.LENGTH_SHORT).show();
        }
    }

    void startSmoothSearchTransition() {
        if (navigationTransitionController != null) navigationTransitionController.start();
    }

    void finishSmoothSearchTransition() {
        if (navigationTransitionController != null) navigationTransitionController.finish();
    }

    void cancelSmoothSearchTransition() {
        if (navigationTransitionController != null) navigationTransitionController.cancel();
    }

    boolean isSmoothSearchTransitionActive() {
        return navigationTransitionController != null && navigationTransitionController.isActive();
    }

    void navigateCurrentTabHome() {
        invalidateTabScopedAsyncWork();
        try {
            saveCurrentTabState();
            TabInfo tab = getCurrentTab();
            tab.resetToHome();
            destroyTabWebView(tab);
            activateNavigationContextForTab(tab);
            skipNextShowHomeTabSave = true;
            showHome();
            saveTabsSession();
        } catch (Exception ignored) {
            skipNextShowHomeTabSave = true;
            showHome();
        }
    }

    void showHome() {
        if (elementPickerActive) finishElementPicker(false);
        cancelSmoothSearchTransition();
        // Internal callers may render Home without mutating an already-empty tab. The explicit
        // Home button uses navigateCurrentTabHome(), which clears URL, history state, and WebView.
        try {
            if (skipNextShowHomeTabSave) {
                skipNextShowHomeTabSave = false;
            } else if (!historyClearLock) {
                saveCurrentTabState();
            }
        } catch (Exception ignored) { skipNextShowHomeTabSave = false; }
        try {
            if (webView != null) webView.stopLoading();
        } catch (Exception ignored) {}
        // Home must never keep a stale loading indicator visible. A hidden WebView can still
        // finish a late callback after stopLoading(), so reset the browser-level progress UI here.
        try {
            if (progressBar != null) {
                progressBar.setProgress(0);
                progressBar.setVisibility(View.GONE);
            }
        } catch (Exception ignored) {}

        if (homeScroll != null) homeScroll.setVisibility(View.VISIBLE);
        if (webView != null) webView.setVisibility(View.GONE);
        if (addressBar != null) addressBar.setText("");
        if (homeSearchInput != null) homeSearchInput.setText("");

        updateVideoControlsVisibility();
        updateTopActionStates();
        updateTabsCountUi();
    }

    void updateTopActionStates() {
        if (reloadButton != null) {
            reloadButton.setVisibility(topIconReload ? View.VISIBLE : View.GONE);
            reloadButton.setColorFilter(Color.parseColor("#E9EDF5"));
        }
        if (bookmarkButton != null) {
            bookmarkButton.setVisibility(topIconBookmark ? View.VISIBLE : View.GONE);
            String url = getEffectiveCurrentUrl();
            boolean bookmarked = url != null && getBookmarks().contains(url);
            bookmarkButton.setColorFilter(bookmarked ? COLOR_ACCENT : Color.parseColor("#E9EDF5"));
        }
        if (translateButton != null) {
            translateButton.setVisibility(topIconTranslate ? View.VISIBLE : View.GONE);
            translateButton.setColorFilter(translateEnabled ? COLOR_ACCENT : Color.parseColor("#E9EDF5"));
        }
    }

    String getEffectiveCurrentUrl() {
        if (webView != null && webView.getVisibility() == View.VISIBLE) {
            String raw = extractOriginalUrl(webView.getUrl());
            if (raw != null && raw.length() > 0) return raw;
        }
        if (homeScroll != null && homeScroll.getVisibility() == View.VISIBLE) return null;
        return normalizeInputToUrl(addressBar != null ? addressBar.getText().toString().trim() : "");
    }

    String normalizeInputToUrl(String text) {
        if (text == null || text.trim().length() == 0) return null;
        text = text.trim();
        if (text.startsWith("https://translate.google.com/translate") || text.startsWith("http://translate.google.com/translate")) return extractOriginalUrl(text);
        if (text.startsWith("http://") || text.startsWith("https://")) return text;
        if (text.contains(".") && !text.contains(" ")) return "https://" + text;
        return null;
    }

    String extractOriginalUrl(String url) {
        if (url == null) return null;
        if (url.startsWith("https://translate.google.com/translate") || url.startsWith("http://translate.google.com/translate")) {
            int idx = url.indexOf("&u=");
            if (idx != -1) return url.substring(idx + 3).replace("%3A", ":").replace("%2F", "/").replace("%3F", "?").replace("%3D", "=").replace("%26", "&");
        }
        return url;
    }

    Set<String> getBookmarks() {
        return BookmarkStore.getBookmarks(
                bookmarkData, getSharedPreferences(PREFS, MODE_PRIVATE));
    }

    BookmarkItemData findBookmarkByUrl(String url) {
        return BookmarkStore.findByUrl(bookmarkData, url);
    }

    List<String> getBookmarkFolders() {
        return BookmarkStore.getFolders(
                bookmarkData, getSharedPreferences(PREFS, MODE_PRIVATE));
    }

    int countBookmarksInFolder(String folder) {
        return BookmarkStore.countInFolder(bookmarkData, folder);
    }

    void loadBookmarkData() {
        BookmarkStore.load(
                bookmarkData,
                getSharedPreferences(PREFS, MODE_PRIVATE),
                YieldWebRuntimeActivity.this::guessLabelFromUrl);
    }

    void saveBookmarkData() {
        BookmarkStore.save(
                bookmarkData, getSharedPreferences(PREFS, MODE_PRIVATE));
    }

    void loadSettings() {
        SettingsStore.load(this, getSharedPreferences(PREFS, MODE_PRIVATE));
    }

    void saveSettings() {
        SettingsStore.save(this, getSharedPreferences(PREFS, MODE_PRIVATE));
    }

    ImageButton smallTopIcon(int iconRes, String desc, View.OnClickListener listener) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setColorFilter(Color.parseColor("#E9EDF5"));
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(dp(4), dp(4), dp(4), dp(4));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setContentDescription(desc);
        button.setOnClickListener(listener);
        return button;
    }

    @Override
    protected void onUserLeaveHint() {
        // Android Home/Recent harus minimize normal, bukan masuk Picture-in-Picture/floating window.
        // State halaman terakhir tetap disimpan agar saat dibuka lagi kembali ke posisi terakhir.
        try {
            saveCurrentTabState();
            saveTabsSession();
            if (webView != null && webView.getUrl() != null && canCommitUrlToTab(getCurrentTab(), webView.getUrl())) {
                lastSafeHttpUrl = cleanTabSessionUrl(webView.getUrl());
            }
        } catch (Exception ignored) {
        }
        super.onUserLeaveHint();
    }

    @Override
    public void onBackPressed() {
        if (fullscreenVideoView != null) {
            exitVideoFullscreenToPortraitMode();
            return;
        }
        if (topBarView != null && topBarView.getVisibility() == View.GONE && webView != null && webView.getVisibility() == View.VISIBLE) {
            exitVideoFullscreenToPortraitMode();
            return;
        }
        if (topBarView != null && topBarView.getVisibility() == View.GONE) {
            exitFullscreenMode();
            return;
        }
        if (webView != null && webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
        } else if (webView != null && webView.getVisibility() == View.VISIBLE) {
            // Reaching Home through Android Back is a real exit from the current page, not merely
            // a visual hide. Destroying the tab WebView stops JavaScript, media, redirects, network
            // activity, and late WebView callbacks from continuing behind the Home screen.
            navigateCurrentTabHome();
        } else {
            super.onBackPressed();
        }
    }

    // ===== Small UI helpers =====
    // Implementations live in YieldUi so future extracted UI classes can reuse them without
    // holding a MainActivity reference. These thin wrappers keep every existing call site unchanged.
    View space(int height) {
        return YieldUi.space(this, height);
    }

    GradientDrawable roundRect(int fillColor, int radius, int strokeWidth, int strokeColor) {
        return YieldUi.roundRect(fillColor, radius, strokeWidth, strokeColor);
    }

    int dp(int value) {
        return YieldUi.dp(this, value);
    }
}
