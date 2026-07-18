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

public class MainActivity extends YieldWebRuntimeActivity
        implements TranslateBridge.Callback, VideoBridge.Callback, AdBlockBridge.Callback {

// ===== WebView JavaScript bridge callbacks =====
    // The JS bridges (TranslateBridge, VideoBridge, AdBlockBridge) are now top-level forwarding
    // shells; MainActivity implements their callback interfaces. The bodies below are unchanged from
    // the former inner-class implementations, so behavior is identical.

    @Override
    public void translateText(int index, String text) {
        if (!lifecycleCallbackGate.isActive() || text == null) return;
        final String clean = text.trim();
        if (clean.length() < 2 || clean.length() > 450) return;
        final int token = translateSessionToken;
        if (!isCompatibleTranslateAllowed(token)) return;

        new Thread(() -> {
            String translated = translateTextViaGoogle(clean);
            if (translated == null || translated.trim().length() == 0) return;
            runOnUiThreadIfAlive(() -> {
                if (isCompatibleTranslateAllowed(token)) applyCompatibleTranslation(index, translated);
            });
        }).start();
    }

    @Override
    public void onCollected(int count) {
        final int token = translateSessionToken;
        runOnUiThreadIfAlive(() -> {
            if (isCompatibleTranslateAllowed(token)) {
                QuietToast.makeText(this, "Translate kompatibel berjalan: " + count + " teks", QuietToast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onVideoPlaying() {
        runOnUiThreadIfAlive(() -> {
            updateVideoPlayPauseButtonState(true);
            showVideoControlsIfAllowed();
        });
    }

    @Override
    public void onVideoTapped() {
        runOnUiThreadIfAlive(() -> {
            videoControlsManualHidden = false;
            showVideoControlsIfAllowed();
        });
    }

    @Override
    public void onVideoStopped() {
        runOnUiThreadIfAlive(() -> {
            // Jangan sembunyikan kontrol saat user menekan Pause.
            // Tombol Play/Pause cukup berubah ikon agar kontrol tetap bisa dipakai lagi.
            updateVideoPlayPauseButtonState(false);
            if (videoControlsBar != null && videoControlsEnabled && webView != null && webView.getVisibility() == View.VISIBLE) {
                videoControlsBar.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void tapAtRatio(double xRatio, double yRatio) {
        runOnUiThreadIfAlive(() -> nativeTapWebViewAtRatio(xRatio, yRatio));
    }

    @Override
    public void onAdRedirect(String url) {
        runOnUiThreadIfAlive(() -> captureAdRedirectToTempTab(url));
    }

    @Override
    public void onElementPicked(String selector, String preview) {
        runOnUiThreadIfAlive(() -> onPickerElementSelected(selector, preview, -1, ""));
    }

    @Override
    public void onElementPickedV2(String selector, String preview, int matchCount, String tagName) {
        runOnUiThreadIfAlive(() -> onPickerElementSelected(selector, preview, matchCount, tagName));
    }

    @Override
    public void onPickerExited() {
        runOnUiThreadIfAlive(() -> {
            elementPickerActive = false;
            if (elementPickerDialog != null) {
                try { elementPickerDialog.dismiss(); } catch (Exception ignored) {}
                elementPickerDialog = null;
            }
        });
    }

    // ===== Activity lifecycle =====
    /** Overridden by PrivateBrowserActivity, which runs in a separate WebView data-directory process. */
    protected boolean useDedicatedPrivateProfile() {
        return false;
    }

    TabInfo createProfileTab(String normalTitle, String privateTitle, String url,
                                     boolean requestedPrivate, boolean adTab) {
        boolean privateTab = dedicatedPrivateProfile || requestedPrivate;
        String title = privateTab ? privateTitle : normalTitle;
        return new TabInfo(title, url, privateTab, adTab);
    }

    TabInfo createProfileTab(String normalTitle, String privateTitle, String url,
                                     boolean requestedPrivate) {
        return createProfileTab(normalTitle, privateTitle, url, requestedPrivate, false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lifecycleCallbackGate.markActive();
        dedicatedPrivateProfile = useDedicatedPrivateProfile();
        if (dedicatedPrivateProfile && !YieldBrowserApplication.isIncognitoProfileReady()) {
            QuietToast.makeText(this, "Profil privat terisolasi tidak tersedia pada perangkat ini",
                    QuietToast.LENGTH_LONG).show();
            finish();
            return;
        }
        loadSettings();
        loadDownloadHistory();
        loadShortcuts();
        if (!dedicatedPrivateProfile) initializeHistoryEngineV2();
        else historyData.clear();
        loadBookmarkData();
        restoreTabsSession();
        ensureDefaultTab();
        createNotificationChannel();
        getWindow().setStatusBarColor(COLOR_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);

        initializeBrowserShellUi();
        topBarView = browserShellUi.createTopBar();
        addressBar = browserShellUi.addressBar();
        reloadButton = browserShellUi.reloadButton();
        bookmarkButton = browserShellUi.bookmarkButton();
        translateButton = browserShellUi.translateButton();
        root.addView(topBarView, new LinearLayout.LayoutParams(-1, -2));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        progressBar.setProgressDrawable(new ColorDrawable(COLOR_ACCENT));
        root.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(3)));

        contentFrame = new FrameLayout(this);
        contentFrame.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1));

        homeScroll = browserShellUi.createHomeContent();
        homeSearchInput = browserShellUi.homeSearchInput();
        contentFrame.addView(homeScroll, new FrameLayout.LayoutParams(-1, -1));

        TabInfo initialTab = getCurrentTab();
        webView = createBrowserWebView(initialTab, View.GONE);
        try { initialTab.webView = webView; } catch (Exception ignored) {}
        contentFrame.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        navigationLoadingOverlay = NavigationTransitionController.createOverlay(this);
        navigationLoadingOverlay.setVisibility(View.GONE);
        contentFrame.addView(navigationLoadingOverlay, new FrameLayout.LayoutParams(-1, -1));
        navigationTransitionController = new NavigationTransitionController(
                navigationLoadingOverlay, homeScroll, webView);

        root.addView(contentFrame);

        videoControlsBar = createVideoControlsBar();
        videoControlsBar.setVisibility(View.GONE);
        root.addView(videoControlsBar, new LinearLayout.LayoutParams(-1, dp(66)));

        bottomNavView = browserShellUi.createBottomNav(tabCount);
        tabsCountText = browserShellUi.tabsCountText();
        root.addView(bottomNavView, new LinearLayout.LayoutParams(-1, dp(64)));

        setContentView(root);
        initializeSwipeNavigation(root);
        restoreActiveTabAfterLaunch();
        updateTopActionStates();
        handleOpenDownloadsIntent(getIntent());
        handleProfileSpaceIntent(getIntent());
        mainHandler.postDelayed(this::pumpDownloadQueue, 650);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenDownloadsIntent(intent);
        handleProfileSpaceIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_DOWNLOAD_FOLDER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri treeUri = data.getData();
            int takeFlags = PersistableUriPermissionPolicy.takeFlags(
                    data.getFlags(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (!PersistableUriPermissionPolicy.hasAccess(takeFlags)) {
                QuietToast.makeText(this, "Izin folder tidak diberikan",
                        QuietToast.LENGTH_SHORT).show();
                return;
            }
            try {
                getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
            } catch (Exception e) {
                QuietToast.makeText(this, "Izin folder tidak dapat disimpan",
                        QuietToast.LENGTH_SHORT).show();
                return;
            }
            selectedDownloadTreeUri = treeUri.toString();
            saveSettings();
            QuietToast.makeText(this, "Folder HP dipilih untuk hasil download", QuietToast.LENGTH_SHORT).show();
            showDownloadSettingsPanel();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            getWindow().setStatusBarColor(COLOR_BG);
            if (webView != null && webView.getVisibility() == View.VISIBLE) {
                saveCurrentTabState();
                updateTopActionStates();
                injectVideoPlaybackWatcher();
                checkAndShowVideoControls();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onPause() {
        if (elementPickerActive) finishElementPicker(false);
        downloadUiTickerRunning = false;
        mainHandler.removeCallbacks(downloadUiTicker);
        try {
            saveCurrentTabState();
            if (!dedicatedPrivateProfile) {
                saveTabsSession();
                recordCurrentPageToHistory();
            }
        } catch (Exception ignored) {}
        if (videoBackgroundPlay && webView != null && !isYouTubePlaybackUrl(getEffectiveCurrentUrl())) {
            try {
                webView.evaluateJavascript("(function(){try{var v=document.querySelector('video');if(v&&!v.paused){v.play().catch(function(){});}return 'keep';}catch(e){return 'err';}})()", null);
            } catch (Exception ignored) {
            }
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        try {
            saveCurrentTabState();
            if (!dedicatedPrivateProfile) {
                saveTabsSession();
                recordCurrentPageToHistory();
            }
        } catch (Exception ignored) {}
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleOpenDownloadsIntent(getIntent());
        if (activeDownloadDialog != null && activeDownloadDialog.isShowing()) {
            downloadUiTickerRunning = true;
            mainHandler.removeCallbacks(downloadUiTicker);
            mainHandler.post(downloadUiTicker);
        }
    }

    @Override
    protected void onDestroy() {
        lifecycleCallbackGate.markDestroyed();
        downloadUiTickerRunning = false;
        mainHandler.removeCallbacksAndMessages(null);
        try {
            if (activeDownloadDialog != null && activeDownloadDialog.isShowing()) {
                activeDownloadDialog.dismiss();
            }
        } catch (Exception ignored) {}
        try {
            saveCurrentTabState();
            if (!dedicatedPrivateProfile) {
                saveTabsSession();
                recordCurrentPageToHistory();
            }
            for (int i = tabs.size() - 1; i >= 0; i--) {
                TabInfo tab = tabs.get(i);
                if (tab != null) {
                    tab.closed = true;
                    destroyTabWebView(tab);
                }
            }
            tabs.clear();
        } catch (Exception ignored) {}
        historyFaviconLoader.shutdown();
        super.onDestroy();
    }

    void discardPrivateTabsForExplicitExit() {
        try {
            invalidateTabScopedAsyncWork();
            for (int i = tabs.size() - 1; i >= 0; i--) {
                TabInfo tab = tabs.get(i);
                if (tab == null || !tab.privateTab) continue;
                tab.closed = true;
                destroyTabWebView(tab);
                tabs.remove(i);
            }
            if (tabs.isEmpty()) tabs.add(createProfileTab("Tab utama", "Tab privat", "", false));
            currentTabIndex = Math.max(0, Math.min(currentTabIndex, tabs.size() - 1));
            TabInfo active = getCurrentTab();
            activateNavigationContextForTab(active);
            webView = active.webView;
            if (addressBar != null) addressBar.setText(active.url == null ? "" : active.url);
            // Prevent onPause/onStop/onDestroy from using stale private address-bar text as a
            // fallback for an empty normal tab after the private tab has been discarded.
            skipNextShowHomeTabSave = true;
            saveTabsSession();
        } catch (Exception ignored) {
        }
    }

    void handleOpenDownloadsIntent(Intent intent) {
        if (intent == null) return;
        boolean shouldOpen = ACTION_OPEN_DOWNLOADS.equals(intent.getAction()) || intent.getBooleanExtra("open_downloads", false);
        if (!shouldOpen) return;

        intent.setAction(null);
        intent.removeExtra("open_downloads");

        if (activeDownloadDialog != null && activeDownloadDialog.isShowing()) {
            return;
        }

        if (mainHandler != null) {
            mainHandler.post(() -> showDownloadManager());
        } else {
            showDownloadManager();
        }
    }

    // ===== Main UI =====
    void initializeBrowserShellUi() {
        browserShellUi = new BrowserShellUi(
                this,
                dedicatedPrivateProfile,
                shortcutsData,
                new BrowserShellUi.Host() {
                    @Override
                    public void hideKeyboardAndClearFocus(View view) {
                        MainActivity.this.hideKeyboardAndClearFocus(view);
                    }

                    @Override
                    public void openAddressBarUrl() {
                        MainActivity.this.openAddressBarUrl();
                    }

                    @Override
                    public void openHomeSearchUrl() {
                        MainActivity.this.openHomeSearchUrl();
                    }

                    @Override
                    public void reloadCurrentWebsite() {
                        MainActivity.this.reloadCurrentWebsite();
                    }

                    @Override
                    public void toggleBookmark() {
                        MainActivity.this.toggleBookmark();
                    }

                    @Override
                    public void toggleTranslate() {
                        MainActivity.this.toggleTranslate();
                    }

                    @Override
                    public void showQuickMenu() {
                        MainActivity.this.showQuickMenu();
                    }

                    @Override
                    public void openNormalBrowserSpace() {
                        MainActivity.this.openNormalBrowserSpace();
                    }

                    @Override
                    public void newTabInCurrentProfile() {
                        MainActivity.this.newTabInCurrentProfile();
                    }

                    @Override
                    public void navigateCurrentTabHome() {
                        MainActivity.this.navigateCurrentTabHome();
                    }

                    @Override
                    public void showBookmarkList() {
                        MainActivity.this.showBookmarkList();
                    }

                    @Override
                    public void showTabsPanel() {
                        MainActivity.this.showTabsPanel();
                    }

                    @Override
                    public String currentUrl() {
                        return MainActivity.this.getEffectiveCurrentUrl();
                    }

                    @Override
                    public String normalizeShortcutUrl(String value) {
                        return MainActivity.this.normalizeShortcutUrl(value);
                    }

                    @Override
                    public String guessLabelFromUrl(String url) {
                        return MainActivity.this.guessLabelFromUrl(url);
                    }

                    @Override
                    public void saveShortcuts() {
                        MainActivity.this.saveShortcuts();
                    }

                    @Override
                    public void loadFavicon(String url, ImageView target, TextView fallback) {
                        MainActivity.this.loadFavicon(url, target, fallback);
                    }

                    @Override
                    public void showMessage(String message) {
                        QuietToast.makeText(MainActivity.this, message,
                                QuietToast.LENGTH_SHORT).show();
                    }
                });
    }

    void renderShortcuts() {
        if (browserShellUi != null) browserShellUi.renderShortcuts();
    }

    String normalizeShortcutUrl(String text) {
        if (text == null || text.trim().length() == 0) return null;
        text = text.trim();
        if (!text.startsWith("http://") && !text.startsWith("https://")) {
            if (!text.contains(".") || text.contains(" ")) return null;
            text = "https://" + text;
        }
        return text;
    }

    String guessLabelFromUrl(String url) {
        try {
            String clean = url.replace("https://", "").replace("http://", "");
            if (clean.startsWith("www.")) clean = clean.substring(4);
            int slash = clean.indexOf("/");
            if (slash > 0) clean = clean.substring(0, slash);
            String host = clean.split("[.]")[0];
            if (host.length() == 0) return "Web";
            return host.substring(0, 1).toUpperCase() + host.substring(1);
        } catch (Exception e) {
            return "Web";
        }
    }

    void loadFavicon(String url, ImageView target, TextView fallback) {
        historyFaviconLoader.load(url, target, fallback);
    }

    TabInfo findTabByWebView(WebView candidate) {
        return TabWebViewLifecycle.findOwner(tabs, candidate);
    }

    boolean isLiveTabWebView(TabInfo tab, WebView candidate, long generation) {
        return TabWebViewLifecycle.isLive(tabs, tab, candidate, generation);
    }

    boolean isPrivateWebView(WebView candidate) {
        return TabWebViewLifecycle.isPrivate(dedicatedPrivateProfile, tabs, candidate);
    }

    void applyProfileCookiePolicy(WebView candidate) {
        if (candidate == null) return;
        try {
            CookieManager manager = CookieManager.getInstance();
            // Cookies remain enabled inside the profile so logins work. In the dedicated private
            // process they are stored in the incognito data directory and deleted on profile exit.
            manager.setAcceptCookie(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                manager.setAcceptThirdPartyCookies(candidate,
                        PrivateProfilePolicy.allowThirdPartyCookies(isPrivateWebView(candidate)));
            }
        } catch (Exception ignored) {
        }
    }

    void postForActiveTab(TabInfo tab, WebView candidate, long delayMs, Runnable action) {
        if (tab == null || candidate == null || action == null) return;
        final long generation = tab.webViewGeneration;
        mainHandler.postDelayed(() -> {
            if (!isLiveTabWebView(tab, candidate, generation) || !isCurrentTabInfo(tab)) return;
            action.run();
        }, delayMs);
    }

    void attachWebViewToContentFrame(WebView candidate) {
        TabWebViewLifecycle.attach(
                contentFrame, homeScroll, navigationLoadingOverlay, candidate);
    }

    WebView ensureTabWebView(TabInfo tab, int visibility) {
        return TabWebViewLifecycle.ensure(
                tab,
                visibility,
                webView,
                this::createBrowserWebView,
                this::attachWebViewToContentFrame,
                candidate -> {
                    webView = candidate;
                    try { applyBrowserSettings(); } catch (Exception ignored) {}
                    try { applyProfileCookiePolicy(candidate); } catch (Exception ignored) {}
                });
    }

    void hideInactiveTabWebViews(WebView active) {
        TabWebViewLifecycle.hideInactive(tabs, active);
    }

    void invalidateTabScopedAsyncWork() {
        translateSessionToken++;
        nightModeApplyToken++;
        browserModeToken++;
        pendingHideKeyboardAfterNavigation = false;
        cancelSmoothSearchTransition();
    }

    void activateNavigationContextForTab(TabInfo tab) {
        if (tab == null || tab.closed) {
            currentPageUrlForRequest = "";
            lastSafeHttpUrl = "";
            trustedMainFrameHost = "";
            trustedMainFrameUntilMs = 0L;
            return;
        }
        String activeUrl = cleanTabSessionUrl(tab.currentPageUrlForRequest);
        if (activeUrl.length() == 0) activeUrl = cleanTabSessionUrl(tab.url);
        String safeUrl = cleanTabSessionUrl(tab.lastSafeUrl);
        if (safeUrl.length() == 0) safeUrl = activeUrl;
        currentPageUrlForRequest = activeUrl;
        lastSafeHttpUrl = safeUrl;

        // Trust windows are navigation-scoped, never transferable between tabs.
        trustedMainFrameHost = "";
        trustedMainFrameUntilMs = 0L;
        reloadLoopLastKey = "";
        reloadLoopWindowStartMs = 0L;
        reloadLoopCount = 0;
        reloadLoopGuardHost = "";
        reloadLoopGuardKey = "";
        reloadLoopGuardUntilMs = 0L;
        siteCompatibilityHost = "";
        siteCompatibilityUntilMs = 0L;
        autoCompatibilityRecoveryHost = "";
        autoCompatibilityRecoveryKey = "";
        autoCompatibilityRecoveryUntilMs = 0L;
        webHorizontalGestureGuard = false;
        webHorizontalGestureGuardHost = "";
    }

    void activateTabWebView(TabInfo tab, boolean showWebPage) {
        try {
            activateNavigationContextForTab(tab);
            WebView target = ensureTabWebView(tab, showWebPage ? View.VISIBLE : View.GONE);
            webView = target;
            hideInactiveTabWebViews(target);
            TabWebViewLifecycle.activateSurface(
                    homeScroll, navigationLoadingOverlay, target, showWebPage);
        } catch (Exception ignored) {
        }
    }

    void destroyTabWebView(TabInfo tab) {
        if (tab == null) return;
        WebView doomed = tab.webView;
        if (doomed != null && doomed == webView && elementPickerActive) {
            finishElementPicker(false);
        }
        if (doomed != null && doomed == webView) webView = null;
        webView = TabWebViewLifecycle.destroy(
                tab, webView, contentFrame, this::removeShieldDocumentStartScript);
    }

    boolean hasLivePage(WebView candidate) {
        return TabWebViewLifecycle.hasLivePage(candidate, this::extractOriginalUrl);
    }

    void ensureDefaultTab() {
        if (tabs.isEmpty()) {
            tabs.add(createProfileTab("Tab utama", "Tab privat", "", false));
        }
        currentTabIndex = TabNavigationPolicy.clampIndex(currentTabIndex, tabs.size());
        tabCount = TabNavigationPolicy.countForUi(tabs.size());
    }

    TabInfo getCurrentTab() {
        ensureDefaultTab();
        return tabs.get(currentTabIndex);
    }

    boolean isCurrentPrivateTab() {
        return getCurrentTab().privateTab;
    }

    boolean isCurrentTabInfo(TabInfo tab) {
        try {
            return TabNavigationPolicy.isCurrentTab(
                    tabs, currentTabIndex, tab, tab != null && tab.closed);
        } catch (Exception e) {
            return false;
        }
    }

    String getTabReferenceUrl(TabInfo tab) {
        if (tab == null) return "";
        String ref = cleanTabSessionUrl(tab.lastSafeUrl);
        if (ref.length() == 0) ref = cleanTabSessionUrl(tab.url);
        if (ref.length() == 0) ref = cleanTabSessionUrl(tab.currentPageUrlForRequest);
        return ref;
    }

    void prepareTabForMainFrameNavigation(TabInfo tab, String url) {
        if (tab == null || tab.closed || url == null || url.trim().length() == 0) return;
        try {
            String clean = cleanTabSessionUrl(url);
            if (!isHttpOrHttpsUrl(clean)) return;
            String host = BrowserUrlUtils.safeHostForTabIsolation(clean);
            if (host.length() == 0) return;
            long now = System.currentTimeMillis();
            tab.trustedNavigationHost = host;
            tab.trustedNavigationUntilMs = now + 15000L;
            // Saat user mengetik URL / klik hasil pencarian / klik link nyata, izinkan transisi domain
            // sebentar agar redirect login/CDN/front-door tidak dianggap bocor dari tab lain.
            tab.domainSwitchAllowedUntilMs = now + 15000L;
            tab.currentPageUrlForRequest = clean;
        } catch (Exception ignored) {
        }
    }

    boolean isTrustedMainFrameNavigationForTab(TabInfo tab, String url) {
        try {
            if (tab == null || url == null || url.length() == 0) return false;
            long now = System.currentTimeMillis();
            String host = BrowserUrlUtils.safeHostForTabIsolation(url);
            if (host.length() == 0) return false;
            if (tab.trustedNavigationHost != null && tab.trustedNavigationHost.length() > 0 && now <= tab.trustedNavigationUntilMs) {
                String trusted = tab.trustedNavigationHost.toLowerCase(Locale.US);
                if (sameOrSubDomain(host, trusted) || sameOrSubDomain(trusted, host)) return true;
            }
            return now <= tab.domainSwitchAllowedUntilMs && !isTemporaryDirectAdUrl(url) && !isKnownPopupHost(url) && !isLikelyAdClickUrl(url);
        } catch (Exception e) {
            return false;
        }
    }

    boolean isSameIsolatedSite(String host, String baseHost) {
        if (host == null || baseHost == null || host.length() == 0 || baseHost.length() == 0) return false;
        return sameOrSubDomain(host, baseHost) || sameOrSubDomain(baseHost, host);
    }

    boolean isTabIsolationAllowed(TabInfo tab, String url) {
        try {
            if (tab == null) return false;
            String clean = cleanTabSessionUrl(url);
            if (clean.length() == 0) return true;
            if (!isRestorableTabSessionUrl(clean)) return false;
            String targetHost = BrowserUrlUtils.safeHostForTabIsolation(clean);
            if (targetHost.length() == 0) return false;

            String baseHost = tab.isolationHost == null ? "" : tab.isolationHost;
            if (baseHost.length() == 0) {
                String ref = getTabReferenceUrl(tab);
                baseHost = BrowserUrlUtils.safeHostForTabIsolation(ref);
            }
            if (baseHost.length() == 0) return true;
            if (isSameIsolatedSite(targetHost, baseHost)) return true;
            if (isTrustedMainFrameNavigationForTab(tab, clean) || isTrustedMainFrameNavigation(clean)) return true;
            if (isSearchEngineResultNavigation(clean, getTabReferenceUrl(tab))) return true;
            if (isStrictSiteCompatibilityUrl(clean) || isSiteCompatibilityModeActiveForUrl(clean)) return true;

            // Kalau WebView/tab lama mencoba menyimpan direct-link, iklan, atau domain asing tanpa
            // jendela navigasi user, jangan biarkan menimpa tab ini.
            if (isTemporaryDirectAdUrl(clean) || isKnownPopupHost(clean) || isLikelyAdClickUrl(clean) || isAdUrl(clean)) return false;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    void syncTabIsolationAfterCommit(TabInfo tab, String url) {
        if (tab == null || tab.closed || url == null) return;
        try {
            String host = BrowserUrlUtils.safeHostForTabIsolation(url);
            if (host.length() > 0) tab.isolationHost = host;
            tab.currentPageUrlForRequest = cleanTabSessionUrl(url);
        } catch (Exception ignored) {
        }
    }

    String cleanTabSessionUrl(String url) {
        try {
            String clean = extractOriginalUrl(url);
            if (clean == null) clean = url;
            return clean == null ? "" : clean.trim();
        } catch (Exception e) {
            return url == null ? "" : url.trim();
        }
    }

    boolean isTemporaryDirectAdUrl(String url) {
        String clean = cleanTabSessionUrl(url);
        if (clean.length() == 0) return false;
        String lower = clean.toLowerCase(Locale.US);
        if (isExternalSchemeUrl(lower) || isLikelyAdClickUrl(lower) || isAdUrl(lower)) return true;

        String host = normalizeHostForAdBlock(clean);
        // Marketplace short links sering muncul sebagai direct-link iklan/popup.
        // Jangan blokir browsing-nya, tapi jangan biarkan tersimpan sebagai sesi tab normal.
        if (host.equals("s.shopee.co.id") || host.equals("shope.ee")
                || host.equals("s.lazada.co.id") || host.equals("c.lazada.co.id")
                || host.equals("s.lazada.com") || host.equals("c.lazada.com")) {
            return true;
        }

        return lower.contains("/popunder")
                || lower.contains("/popup")
                || lower.contains("clickunder")
                || lower.contains("adclick")
                || lower.contains("ad_click")
                || lower.contains("adurl=")
                || lower.contains("af_click")
                || lower.contains("deep_and_deferred")
                || lower.contains("navigate_url=")
                || lower.contains("reactpath")
                || lower.contains("click_id");
    }

    boolean isRestorableTabSessionUrl(String url) {
        String clean = cleanTabSessionUrl(url);
        if (clean.length() == 0) return true;
        String lower = clean.toLowerCase(Locale.US);
        if (!isHttpOrHttpsUrl(clean)) return false;
        if (lower.startsWith("about:") || lower.startsWith("javascript:")
                || lower.startsWith("data:") || lower.startsWith("blob:")) return false;
        if (isImageResourceUrl(clean)) return false;
        return !isTemporaryDirectAdUrl(clean);
    }

    boolean canCommitUrlToTab(TabInfo tab, String url) {
        String clean = cleanTabSessionUrl(url);
        if (clean.length() == 0) return true;
        if (tab != null && tab.adTab) return true;
        if (!isRestorableTabSessionUrl(clean)) return false;
        if (tab == null || tab.closed) return false;

        String safeReference = getTabReferenceUrl(tab);
        if (adBlock && safeReference != null && safeReference.length() > 0
                && ShieldEngineV2.isHighConfidenceSameOriginRelay(clean, safeReference,
                isShieldReaderOrCompatibilityContext(safeReference))) {
            return false;
        }

        // v0.9.84-smart-isolation: URL hanya boleh masuk ke tab pemiliknya sendiri.
        // Jika domain berbeda tanpa navigasi user/trusted, anggap itu stale state dari tab lain,
        // direct-link, atau efek WebView yang baru ditutup.
        if (!isTabIsolationAllowed(tab, clean)) return false;

        String referenceUrl = getTabReferenceUrl(tab);
        if (referenceUrl != null && referenceUrl.length() > 0) {
            String targetHost = normalizeHostForAdBlock(clean);
            String referenceHost = normalizeHostForAdBlock(referenceUrl);
            boolean sameSite = targetHost.length() > 0 && referenceHost.length() > 0
                    && (sameOrSubDomain(targetHost, referenceHost) || sameOrSubDomain(referenceHost, targetHost));
            boolean compatibilityAllowed = isStrictSiteCompatibilityUrl(clean) || isSiteCompatibilityModeActiveForUrl(clean);
            boolean trustedForThisTab = isTrustedMainFrameNavigationForTab(tab, clean) || isTrustedMainFrameNavigation(clean);
            if (!sameSite && !compatibilityAllowed && !trustedForThisTab && isSuspiciousPopupNavigation(clean, referenceUrl)) {
                return false;
            }
        }

        return true;
    }

    void commitTabUrlIfSafe(TabInfo tab, String candidateUrl, String candidateTitle) {
        if (tab == null || tab.closed) return;
        String clean = cleanTabSessionUrl(candidateUrl);
        if (tab.adTab) {
            tab.url = clean;
            tab.currentPageUrlForRequest = clean;
            if (candidateTitle != null && candidateTitle.trim().length() > 0) tab.title = candidateTitle;
            return;
        }

        if (clean.length() > 0 && canCommitUrlToTab(tab, clean)) {
            tab.url = clean;
            tab.lastSafeUrl = clean;
            syncTabIsolationAfterCommit(tab, clean);
            String title = candidateTitle != null && candidateTitle.trim().length() > 0 ? candidateTitle : clean;
            tab.title = title;
            tab.lastSafeTitle = title;
        } else if (clean.length() == 0) {
            tab.url = "";
        } else if (tab.lastSafeUrl != null && tab.lastSafeUrl.length() > 0) {
            tab.url = tab.lastSafeUrl;
            if (tab.lastSafeTitle != null && tab.lastSafeTitle.length() > 0) tab.title = tab.lastSafeTitle;
        }
    }

    String getSafeUrlForSession(TabInfo tab) {
        if (tab == null) return "";
        String url = cleanTabSessionUrl(tab.url);
        if (url.length() > 0 && canCommitUrlToTab(tab, url)) return url;
        String fallback = cleanTabSessionUrl(tab.lastSafeUrl);
        if (fallback.length() > 0 && isRestorableTabSessionUrl(fallback)) return fallback;
        return url.length() == 0 ? "" : null;
    }

    boolean isPersistableTab(TabInfo tab) {
        return tab != null && TabSessionPolicy.shouldPersist(tab.closed, tab.privateTab, tab.adTab);
    }

    TabInfo findPersistedSelectionCandidate() {
        if (tabs.isEmpty()) return null;
        boolean[] persistable = new boolean[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) persistable[i] = isPersistableTab(tabs.get(i));
        int selected = TabSessionPolicy.nearestPersistableIndex(persistable, currentTabIndex);
        return selected >= 0 ? tabs.get(selected) : null;
    }

    void restoreTabsSession() {
        if (dedicatedPrivateProfile) {
            tabs.clear();
            tabs.add(createProfileTab("Tab utama", "Tab privat", "", true));
            currentTabIndex = 0;
            tabCount = 1;
            return;
        }
        try {
            TabSessionStore.StoredSession stored = TabSessionStore.read(this);
            if (stored.raw.trim().length() == 0) return;

            ArrayList<TabInfo> restored = new ArrayList<>();
            ArrayList<Integer> restoredSourceIndexes = new ArrayList<>();
            for (TabSessionCodec.Record record : TabSessionCodec.decode(stored.raw)) {
                // Private and ad tabs remain ephemeral even when reading legacy session rows.
                if (!TabSessionPolicy.shouldRestore(record.privateTab, record.adTab)) continue;

                String title = TabSessionCodec.normalizedTitle(record.title);
                String url = cleanTabSessionUrl(record.url);
                if (url.length() > 0 && !isRestorableTabSessionUrl(url)) continue;

                TabInfo restoredTab = new TabInfo(record.tabId, title, url, false, false);
                if (url.length() > 0) {
                    restoredTab.lastSafeUrl = url;
                    restoredTab.lastSafeTitle = title;
                    restoredTab.currentPageUrlForRequest = url;
                    String host = record.isolationHost.length() > 0
                            ? record.isolationHost
                            : BrowserUrlUtils.safeHostForTabIsolation(url);
                    restoredTab.isolationHost = host == null ? "" : host;
                }
                restored.add(restoredTab);
                restoredSourceIndexes.add(record.sourceIndex);
            }

            if (!restored.isEmpty()) {
                tabs.clear();
                tabs.addAll(restored);
                int selected = TabSessionPolicy.restoredSelectionIndex(
                        restoredSourceIndexes, stored.requestedIndex);
                currentTabIndex = TabNavigationPolicy.clampIndex(
                        selected < 0 ? 0 : selected, tabs.size());
                tabCount = TabNavigationPolicy.countForUi(tabs.size());
            }
        } catch (Exception ignored) {
        }
    }

    void saveTabsSession() {
        if (!PrivateProfilePolicy.shouldPersistBrowserSession(dedicatedPrivateProfile)) return;
        try {
            if (tabs.isEmpty()) ensureDefaultTab();
            TabInfo persistedSelection = findPersistedSelectionCandidate();
            ArrayList<TabSessionCodec.Record> records = new ArrayList<>();
            int savedIndex = 0;

            for (TabInfo tab : tabs) {
                if (!isPersistableTab(tab)) continue;
                String url = getSafeUrlForSession(tab);
                if (url == null) continue;
                if (tab == persistedSelection) savedIndex = records.size();

                String title = tab.title == null ? "" : tab.title;
                if (url.length() > 0 && tab.lastSafeTitle != null
                        && tab.lastSafeTitle.length() > 0
                        && !canCommitUrlToTab(tab, tab.url)) {
                    title = tab.lastSafeTitle;
                }

                String isolationHost = tab.isolationHost == null ? "" : tab.isolationHost;
                if (isolationHost.length() == 0 && url.length() > 0) {
                    isolationHost = BrowserUrlUtils.safeHostForTabIsolation(url);
                }
                records.add(TabSessionCodec.Record.persisted(
                        title, url, isolationHost, tab.id));
            }

            if (records.isEmpty()) {
                TabInfo fallback = createProfileTab("Tab utama", "Tab privat", "", false);
                records.add(TabSessionCodec.Record.persisted(
                        fallback.title, "", "", fallback.id));
                savedIndex = 0;
            }

            TabSessionStore.write(
                    this,
                    TabSessionCodec.encode(records),
                    TabSessionPolicy.persistedSelectionIndex(savedIndex, records.size()));
        } catch (Exception ignored) {
        }
    }

    void restoreActiveTabAfterLaunch() {
        try {
            ensureDefaultTab();
            TabInfo tab = getCurrentTab();
            activateNavigationContextForTab(tab);
            updateTabsCountUi();
            if (tab.url != null && tab.url.length() > 0 && isRestorableTabSessionUrl(tab.url)) {
                addressBar.setText(tab.url);
                WebView target = ensureTabWebView(tab, View.VISIBLE);
                webView = target;
                hideInactiveTabWebViews(target);
                if (homeScroll != null) homeScroll.setVisibility(View.GONE);
                if (target != null) {
                    target.setAlpha(1f);
                    target.setVisibility(View.VISIBLE);
                    target.bringToFront();
                }
                if (navigationLoadingOverlay != null) navigationLoadingOverlay.bringToFront();
                applyBrowserSettings();
                currentPageUrlForRequest = tab.url;
                tab.currentPageUrlForRequest = tab.url;
                if (translateEnabled && !translateManuallyDisabled) loadTranslatedPage(tab.url);
                else loadBrowserUrl(tab.url);
            } else {
                addressBar.setText("");
                if (homeSearchInput != null) homeSearchInput.setText("");
                activateTabWebView(tab, false);
                skipNextShowHomeTabSave = true;
                showHome();
            }
        } catch (Exception ignored) {
        }
    }

    void saveCurrentTabState() {
        if (tabs.isEmpty()) return;
        if (skipNextShowHomeTabSave) return;
        TabInfo tab = getCurrentTab();
        try {
            if (homeScroll != null && homeScroll.getVisibility() == View.VISIBLE) return;
            // v0.9.84-smart-isolation: jangan pernah menyimpan URL dari WebView yang bukan milik tab aktif.
            // Ini menutup celah saat tab video ditutup, address bar/WebView lama masih membawa URL tab tertutup.
            if (tab != null && tab.webView != null && webView != null && webView != tab.webView) return;
            if (webView != null && webView.getVisibility() == View.VISIBLE && webView.getUrl() != null) {
                TabInfo owner = findTabByWebView(webView);
                if (owner != null && owner != tab) return;
                String url = extractOriginalUrl(webView.getUrl());
                String title = webView.getTitle();
                commitTabUrlIfSafe(tab, url, title);
                try {
                    Bundle state = new Bundle();
                    WebBackForwardList saved = webView.saveState(state);
                    if (saved != null && saved.getSize() > 0) tab.webState = state;
                } catch (Exception ignored) {
                }
            } else if (addressBar != null && addressBar.getText() != null) {
                // Address bar hanya fallback untuk tab kosong/tanpa live WebView. Kalau tab sudah punya
                // WebView hidup, teks address bar bisa saja stale dari tab yang baru ditutup.
                if (tab != null && tab.webView != null && hasLivePage(tab.webView)) return;
                String maybeUrl = normalizeInputToUrl(addressBar.getText().toString().trim());
                if (maybeUrl != null) commitTabUrlIfSafe(tab, maybeUrl, tab.title);
            }
        } catch (Exception ignored) {
        }
    }

    void updateTabsCountUi() {
        tabCount = TabNavigationPolicy.countForUi(tabs.size());
        if (tabsCountText != null) {
            tabsCountText.setText(String.valueOf(tabCount));
        }
    }

    void newTabInCurrentProfile() {
        saveCurrentTabState();
        invalidateTabScopedAsyncWork();
        tabs.add(createProfileTab("Tab baru", "Tab privat", "", false));
        currentTabIndex = tabs.size() - 1;
        activateTabWebView(getCurrentTab(), false);
        updateTabsCountUi();
        addressBar.setText("");
        if (homeSearchInput != null) homeSearchInput.setText("");
        skipNextShowHomeTabSave = true;
        showHome();
        saveTabsSession();
        QuietToast.makeText(this, dedicatedPrivateProfile ? "Tab privat baru" : "Tab baru dibuat",
                QuietToast.LENGTH_SHORT).show();
    }

    void launchDedicatedPrivateProfile() {
        launchDedicatedPrivateProfile(false, null);
    }

    void launchDedicatedPrivateProfile(boolean openTabSwitcher) {
        launchDedicatedPrivateProfile(openTabSwitcher, null);
    }

    void launchDedicatedPrivateProfile(boolean openTabSwitcher, String openUrl) {
        try {
            Intent intent = new Intent(this, PrivateBrowserActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (openTabSwitcher) intent.putExtra(EXTRA_OPEN_TAB_SWITCHER, true);
            if (openUrl != null && !openUrl.trim().isEmpty()) {
                intent.putExtra(EXTRA_OPEN_URL, openUrl.trim());
            }
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            QuietToast.makeText(this, "Profil privat tidak dapat dibuka", QuietToast.LENGTH_SHORT).show();
        }
    }

    void launchNormalProfile(boolean openTabSwitcher, boolean createTab) {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (openTabSwitcher) intent.putExtra(EXTRA_OPEN_TAB_SWITCHER, true);
            if (createTab) intent.putExtra(EXTRA_CREATE_TAB, true);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            QuietToast.makeText(this, "Tab umum tidak dapat dibuka", QuietToast.LENGTH_SHORT).show();
        }
    }

    void handleProfileSpaceIntent(Intent intent) {
        if (intent == null) return;
        boolean createTab = intent.getBooleanExtra(EXTRA_CREATE_TAB, false);
        boolean openSwitcher = intent.getBooleanExtra(EXTRA_OPEN_TAB_SWITCHER, false);
        String openUrl = intent.getStringExtra(EXTRA_OPEN_URL);
        boolean hasUrl = openUrl != null && !openUrl.trim().isEmpty();
        if (!createTab && !openSwitcher && !hasUrl) return;

        intent.removeExtra(EXTRA_CREATE_TAB);
        intent.removeExtra(EXTRA_OPEN_TAB_SWITCHER);
        intent.removeExtra(EXTRA_OPEN_URL);
        final String requestedUrl = hasUrl ? openUrl.trim() : null;
        mainHandler.post(() -> {
            if (requestedUrl != null) openUrlInCurrentProfileTab(requestedUrl);
            else if (createTab) createOrReuseBlankProfileTab();
            if (openSwitcher) showTabsPanel();
        });
    }

    void createOrReuseBlankProfileTab() {
        TabInfo current = getCurrentTab();
        boolean reusable = current != null
                && (current.url == null || current.url.isEmpty())
                && !hasLivePage(current.webView);
        if (reusable) {
            addressBar.setText("");
            if (homeSearchInput != null) homeSearchInput.setText("");
            skipNextShowHomeTabSave = true;
            showHome();
            return;
        }
        newTabInCurrentProfile();
    }

    void openUrlInCurrentProfileTab(String url) {
        String normalized = normalizeInputToUrl(url);
        if (normalized == null || normalized.isEmpty()) return;
        TabInfo current = getCurrentTab();
        boolean canReuseEmpty = current != null
                && (current.url == null || current.url.isEmpty())
                && !hasLivePage(current.webView);
        if (!canReuseEmpty) newTabInCurrentProfile();
        if (addressBar != null) addressBar.setText(normalized);
        openAddressBarUrl();
    }

    void openUrlInPrivateSpace(String url) {
        if (url == null || url.trim().isEmpty()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !dedicatedPrivateProfile) {
            launchDedicatedPrivateProfile(false, url);
            return;
        }
        if (!dedicatedPrivateProfile) newPrivateTab();
        openUrlInCurrentProfileTab(url);
    }

    void openNormalBrowserSpace() {
        if (!dedicatedPrivateProfile) return;
        launchNormalProfile(false, false);
    }

    void openPrivateBrowserSpace() {
        if (dedicatedPrivateProfile) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            launchDedicatedPrivateProfile(false);
        } else {
            newPrivateTab();
        }
    }

    void newPrivateTab() {
        // Android 9+ supports a dedicated WebView data directory per process. This is the only
        // safe way to keep cookies, service workers, cache, DOM storage and HTTP auth separate
        // from the normal browser profile while still using Android WebView.
        if (PrivateProfilePolicy.shouldLaunchDedicatedProfile(
                dedicatedPrivateProfile, Build.VERSION.SDK_INT)) {
            launchDedicatedPrivateProfile();
            return;
        }

        // Compatibility fallback for Android 6-8, where WebView data-directory suffixes are not
        // available. The tab remains non-persistent and receives stricter per-WebView settings,
        // but cannot provide full cookie-store isolation from the normal process.
        saveCurrentTabState();
        invalidateTabScopedAsyncWork();
        tabs.add(createProfileTab("Tab baru", "Tab privat", "", true));
        currentTabIndex = tabs.size() - 1;
        activateTabWebView(getCurrentTab(), false);
        updateTabsCountUi();
        addressBar.setText("");
        if (homeSearchInput != null) homeSearchInput.setText("");
        skipNextShowHomeTabSave = true;
        showHome();
        saveTabsSession();
        QuietToast.makeText(this, "Tab privat aktif", QuietToast.LENGTH_SHORT).show();
    }

    void switchToTab(int index) {
        if (!TabNavigationPolicy.isValidIndex(index, tabs.size())) return;
        boolean changingTab = TabNavigationPolicy.changesTab(index, currentTabIndex);
        if (changingTab && elementPickerActive) finishElementPicker(false);
        boolean saveBeforeSwitch = TabNavigationPolicy.shouldSaveBeforeSwitch(
                skipNextSwitchTabStateSave);
        skipNextSwitchTabStateSave = false;
        if (saveBeforeSwitch) saveCurrentTabState();
        if (changingTab) invalidateTabScopedAsyncWork();
        currentTabIndex = index;
        TabInfo tab = getCurrentTab();
        activateNavigationContextForTab(tab);
        updateTabsCountUi();

        if (tab.url == null || tab.url.length() == 0) {
            addressBar.setText("");
            if (homeSearchInput != null) homeSearchInput.setText("");
            activateTabWebView(tab, false);
            skipNextShowHomeTabSave = true;
            showHome();
        } else {
            addressBar.setText(tab.url);
            WebView target = ensureTabWebView(tab, View.GONE);
            webView = target;
            hideInactiveTabWebViews(target);
            // v0.9.80: jika tab sudah punya WebView hidup, cukup show WebView-nya.
            // Jangan load ulang URL, agar Komiknesia/Lordborg/Invest-Tracing tidak saling ketularan state.
            if (hasLivePage(target)) {
                if (homeScroll != null) homeScroll.setVisibility(View.GONE);
                if (target != null) {
                    target.setAlpha(1f);
                    target.setVisibility(View.VISIBLE);
                    target.bringToFront();
                }
                if (navigationLoadingOverlay != null) navigationLoadingOverlay.bringToFront();
                currentPageUrlForRequest = tab.url;
                tab.currentPageUrlForRequest = tab.url;
                scheduleNightModeSyncForPage(tab.url);
                scheduleHorizontalGestureGuardCheck(tab.url);
                if (hasUserFiltersForCurrentHost()) {
                    applyUserFiltersForCurrentPage();
                    mainHandler.postDelayed(MainActivity.this::applyUserFiltersForCurrentPage, 250);
                }
                if (isStrictSiteCompatibilityUrl(tab.url) || isSiteCompatibilityModeActiveForUrl(tab.url)) {
                    applyPlainCompatibilitySettings();
                    if (desktopMode) {
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 250);
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 1200);
                    } else {
                        mainHandler.postDelayed(() -> applyMobileViewportIfNeeded(), 250);
                    }
                }
                updateVideoControlsVisibility();
            } else {
                startSmoothSearchTransition();
                updateVideoControlsVisibility();
                boolean restored = false;
                if (!translateEnabled && tab.webState != null && target != null) {
                    try {
                        applyBrowserSettings();
                        currentPageUrlForRequest = tab.url;
                        tab.currentPageUrlForRequest = tab.url;
                        WebBackForwardList restoredList = target.restoreState(tab.webState);
                        restored = restoredList != null && restoredList.getSize() > 0;
                        if (restored) {
                            if (homeScroll != null) homeScroll.setVisibility(View.GONE);
                            target.setVisibility(View.VISIBLE);
                            scheduleNightModeSyncForPage(tab.url);
                            scheduleHorizontalGestureGuardCheck(tab.url);
                            mainHandler.postDelayed(() -> finishSmoothSearchTransition(), 120);
                            mainHandler.postDelayed(() -> finishSmoothSearchTransition(), 420);
                        }
                    } catch (Exception ignored) {
                        restored = false;
                    }
                }
                if (!restored) {
                    if (homeScroll != null) homeScroll.setVisibility(View.GONE);
                    if (target != null) target.setVisibility(View.VISIBLE);
                    if (translateEnabled && !translateManuallyDisabled) loadTranslatedPage(tab.url);
                    else loadBrowserUrl(tab.url);
                    mainHandler.postDelayed(() -> finishSmoothSearchTransition(), 3500);
                }
            }
        }

        saveTabsSession();
        QuietToast.makeText(this, tab.privateTab ? "Tab privat aktif" : "Tab umum aktif",
                QuietToast.LENGTH_SHORT).show();
    }

    void switchToTab(TabInfo tab) {
        if (tab == null || tab.closed) return;
        int index = tabs.indexOf(tab);
        if (index >= 0) switchToTab(index);
    }

    void closeTab(TabInfo removed) {
        if (removed == null || removed.closed || tabs.isEmpty()) return;
        int index = tabs.indexOf(removed);
        if (index < 0) return;

        TabInfo activeBeforeClose = getCurrentTab();
        boolean closingCurrent = activeBeforeClose == removed;
        TabInfo replacement = activeBeforeClose;
        if (closingCurrent && tabs.size() > 1) {
            replacement = index < tabs.size() - 1 ? tabs.get(index + 1) : tabs.get(index - 1);
        }

        if (closingCurrent) invalidateTabScopedAsyncWork();
        removed.closed = true;
        destroyTabWebView(removed);
        tabs.remove(index);

        if (tabs.isEmpty()) {
            if (BrowserSpacePolicy.shouldReturnToNormalAfterLastPrivateTab(
                    dedicatedPrivateProfile, tabs.size())) {
                updateTabsCountUi();
                // Professional profile behavior: closing the final private tab returns the user
                // to the existing normal browser space instead of leaving them at the launcher.
                launchNormalProfile(false, false);
                finish();
                return;
            }
            TabInfo fallback = createProfileTab("Tab utama", "Tab privat", "", false);
            tabs.add(fallback);
            currentTabIndex = 0;
            activateTabWebView(fallback, false);
            addressBar.setText("");
            if (homeSearchInput != null) homeSearchInput.setText("");
            skipNextShowHomeTabSave = true;
            showHome();
        } else {
            int replacementIndex = tabs.indexOf(replacement);
            currentTabIndex = TabNavigationPolicy.indexAfterClosingCurrent(
                    index, replacementIndex, tabs.size());
            if (closingCurrent) {
                skipNextSwitchTabStateSave = true;
                switchToTab(currentTabIndex);
            } else {
                TabInfo active = getCurrentTab();
                activateNavigationContextForTab(active);
                boolean showPage = TabNavigationPolicy.shouldShowPage(
                        active.url,
                        homeScroll != null && homeScroll.getVisibility() == View.VISIBLE);
                activateTabWebView(active, showPage);
            }
        }

        updateTabsCountUi();
        saveTabsSession();
        QuietToast.makeText(this, removed.privateTab ? "Tab privat ditutup" : "Tab ditutup",
                QuietToast.LENGTH_SHORT).show();
    }

    void showTabsPanel() {
        TabInfo current = getCurrentTab();
        boolean defaultPrivate = BrowserSpacePolicy.isPrivateSpace(
                dedicatedPrivateProfile,
                current != null && current.privateTab,
                Build.VERSION.SDK_INT);
        showTabsPanelForSpace(defaultPrivate);
    }

    void showTabsPanelForSpace(boolean privateSpace) {
        if (BrowserSpacePolicy.mustOpenOtherProcess(
                privateSpace, dedicatedPrivateProfile, Build.VERSION.SDK_INT)) {
            if (privateSpace) launchDedicatedPrivateProfile(true);
            else launchNormalProfile(true, false);
            return;
        }
        saveCurrentTabState();
        new TabsPanelController(this, tabs, getCurrentTab(), new TabsPanelController.Host() {
            @Override
            public void selectSpace(boolean selectedPrivateSpace) {
                showTabsPanelForSpace(selectedPrivateSpace);
            }

            @Override
            public void createTab(boolean selectedPrivateSpace) {
                if (selectedPrivateSpace) {
                    if (dedicatedPrivateProfile) newTabInCurrentProfile();
                    else newPrivateTab();
                } else if (dedicatedPrivateProfile) {
                    launchNormalProfile(false, true);
                } else {
                    newTabInCurrentProfile();
                }
            }

            @Override
            public void selectTab(TabInfo tab) {
                switchToTab(tab);
            }

            @Override
            public void closeTab(TabInfo tab, boolean selectedPrivateSpace) {
                MainActivity.this.closeTab(tab);
            }

            @Override
            public boolean isActivityFinishing() {
                return isFinishing();
            }
        }).show(privateSpace);
    }

    void showQuickMenu() {
        QuickMenuController.State state = new QuickMenuController.State(
                shortcutDownload, shortcutBookmark, shortcutPrivate, shortcutAdBlock,
                shortcutReader, shortcutNightMode, shortcutQrScan, shortcutHistory,
                shortcutFindPage, shortcutShare, shortcutFullscreen, shortcutVideoControls,
                shortcutReloadWebsite, shortcutBlockElement, shortcutSiteFilter,
                dedicatedPrivateProfile, adBlock, readerMode, videoControlsEnabled,
                nightModeLabel());
        new QuickMenuController(this, mainHandler, state, action -> {
            switch (action) {
                case DOWNLOADS: showDownloadManager(); break;
                case BOOKMARKS: showBookmarkList(); break;
                case PROFILE:
                    if (dedicatedPrivateProfile) openNormalBrowserSpace();
                    else openPrivateBrowserSpace();
                    break;
                case AD_BLOCK:
                    adBlock = !adBlock;
                    if (!adBlock) stopYouTubeAutoAssistantNow();
                    onShieldSettingsChanged();
                    break;
                case READER:
                    readerMode = !readerMode;
                    saveSettings();
                    QuietToast.makeText(this,
                            readerMode ? "Reader mode aktif" : "Reader mode nonaktif",
                            QuietToast.LENGTH_SHORT).show();
                    break;
                case NIGHT_MODE: showNightModeSettingsDialog(); break;
                case QR_SCAN: openQrScanner(); break;
                case HISTORY: showHistoryPanel(); break;
                case FIND_PAGE: showFindInPageDialog(); break;
                case SHARE: shareCurrentPage(); break;
                case FULLSCREEN: toggleFullscreenMode(); break;
                case VIDEO_CONTROLS:
                    videoControlsEnabled = !videoControlsEnabled;
                    saveSettings();
                    updateVideoControlsVisibility();
                    QuietToast.makeText(this,
                            videoControlsEnabled ? "Kontrol video aktif" : "Kontrol video nonaktif",
                            QuietToast.LENGTH_SHORT).show();
                    break;
                case RELOAD: reloadCurrentWebsite(); break;
                case BLOCK_ELEMENT: startElementPicker(); break;
                case SITE_FILTER: showUserFiltersManager(); break;
                case SETTINGS: showSettingsPanel(); break;
                case CUSTOMIZE: showCustomizeMenuPanel(); break;
                case EXIT:
                    try {
                        if (!dedicatedPrivateProfile) {
                            recordCurrentPageToHistory();
                            recordWebViewBackForwardHistory();
                            saveBrowserHistory();
                        }
                    } catch (Exception ignored) {
                    }
                    discardPrivateTabsForExplicitExit();
                    if (dedicatedPrivateProfile) launchNormalProfile(false, false);
                    finish();
                    break;
            }
        }).show();
    }

    void showAboutYieldDialog() {
        QuickMenuController.showAbout(this);
    }

    void showSettingsPanel() {
        SettingsPanelController.MainState state = new SettingsPanelController.MainState(
                dedicatedPrivateProfile, searchEngine, videoControlsEnabled, speedMode,
                safeMode, httpsFirstEnabled, nightModeLabel(), readerMode, adBlock,
                adBlock ? "Perlindungan otomatis tanpa notifikasi yang mengganggu."
                        : "Perlindungan situs sedang nonaktif.",
                dataSaver, desktopMode, textZoom);
        new SettingsPanelController(this, mainHandler).showMain(state, (action, owner) -> {
            switch (action) {
                case TRANSLATE: showTranslateOptionsDialog(); break;
                case DOWNLOAD_SETTINGS: showDownloadSettingsPanel(); break;
                case BOOKMARKS: showBookmarkList(); break;
                case PROFILE:
                    if (dedicatedPrivateProfile) openNormalBrowserSpace();
                    else openPrivateBrowserSpace();
                    break;
                case CUSTOMIZE: showCustomizeMenuPanel(); break;
                case QR_SCAN: openQrScanner(); break;
                case SEARCH_ENGINE: showSearchEngineDialog(); break;
                case BLOCK_ELEMENT: startElementPicker(); break;
                case SITE_FILTER: showUserFiltersManager(); break;
                case HISTORY: showHistoryPanel(); break;
                case FIND_PAGE: showFindInPageDialog(); break;
                case SHARE: shareCurrentPage(); break;
                case COPY_LINK: copyCurrentLink(); break;
                case PAGE_INFO: showPageInfoDialog(); break;
                case FULLSCREEN: toggleFullscreenMode(); break;
                case VIDEO_CONTROLS:
                    videoControlsEnabled = !videoControlsEnabled;
                    updateVideoControlsVisibility();
                    saveSettings();
                    showSettingsPanel();
                    break;
                case VIDEO_OPTIMIZATION: showVideoOptimizationDialog(); break;
                case SAVE_OFFLINE: saveCurrentPageOffline(); break;
                case SPEED_MODE:
                    speedMode = !speedMode;
                    applyBrowserSettings();
                    saveSettings();
                    break;
                case SAFE_MODE: safeMode = !safeMode; saveSettings(); break;
                case HTTPS_FIRST:
                    httpsFirstEnabled = !httpsFirstEnabled;
                    if (!httpsFirstEnabled) clearAllHttpsFirstRuntimeState();
                    saveSettings();
                    break;
                case NIGHT_MODE: showNightModeSettingsDialog(); break;
                case READER_MODE: readerMode = !readerMode; saveSettings(); break;
                case AD_BLOCK_SETTINGS: showAdBlockSettingsDialog(); break;
                case DATA_SAVER:
                    dataSaver = !dataSaver;
                    applyBrowserSettings();
                    saveSettings();
                    break;
                case DESKTOP_MODE: toggleDesktopModeSafely(); break;
                case TEXT_ZOOM: showTextZoomDialog(owner); break;
                case CLEAR_CACHE:
                    if (webView != null) webView.clearCache(true);
                    QuietToast.makeText(this, "Cache dibersihkan",
                            QuietToast.LENGTH_SHORT).show();
                    break;
                case ABOUT: showAboutYieldDialog(); break;
            }
        });
    }

    void showAdBlockSettingsDialog() {
        SettingsPanelController.AdBlockState state = new SettingsPanelController.AdBlockState(
                adBlock, adBlockPopupBlocker, adBlockRedirectBlocker,
                adBlockScriptIframeBlocker, adBlockClickHijackBlocker,
                adBlockRedirectToTempTab, adBlockAutoCloseAdTabs);
        new SettingsPanelController(this, mainHandler).showAdBlock(state, (action, owner) -> {
            switch (action) {
                case MASTER:
                    adBlock = !adBlock;
                    if (!adBlock) stopYouTubeAutoAssistantNow();
                    break;
                case POPUP: adBlockPopupBlocker = !adBlockPopupBlocker; break;
                case REDIRECT: adBlockRedirectBlocker = !adBlockRedirectBlocker; break;
                case SCRIPT_IFRAME:
                    adBlockScriptIframeBlocker = !adBlockScriptIframeBlocker;
                    break;
                case CLICK_HIJACK:
                    adBlockClickHijackBlocker = !adBlockClickHijackBlocker;
                    break;
                case TEMP_TAB: adBlockRedirectToTempTab = !adBlockRedirectToTempTab; break;
                case AUTO_CLOSE: adBlockAutoCloseAdTabs = !adBlockAutoCloseAdTabs; break;
            }
            onShieldSettingsChanged();
        });
    }

    void openQrScanner() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_QR);
            return;
        }
        showQrScannerDialog();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_QR) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showQrScannerDialog();
            } else {
                QuietToast.makeText(this, "Izin kamera diperlukan untuk pindai QR", QuietToast.LENGTH_SHORT).show();
            }
        }
    }

    void showQrScannerDialog() {
        BrowserUtilityDialogs.showQrScanner(this, result -> {
            String value = BrowserUtilityDialogs.normalizeQrValue(result);
            if (value.length() == 0) {
                QuietToast.makeText(this, "QR kosong", QuietToast.LENGTH_SHORT).show();
                return;
            }
            QuietToast.makeText(this, "QR terbaca", QuietToast.LENGTH_SHORT).show();
            addressBar.setText(value);
            openAddressBarUrl();
        });
    }

    void showSearchEngineDialog() {
        BrowserUtilityDialogs.showSearchEngine(this, searchEngine, selected -> {
            searchEngine = selected;
            saveSettings();
            QuietToast.makeText(this,
                    "Search engine: " + searchEngine,
                    QuietToast.LENGTH_SHORT).show();
        });
    }

    void showCustomizeMenuPanel() {
        SettingsPanelController.CustomizeState state =
                new SettingsPanelController.CustomizeState(
                        topIconReload, topIconBookmark, topIconTranslate,
                        shortcutReloadWebsite, shortcutDownload, shortcutBookmark,
                        shortcutPrivate, shortcutAdBlock, shortcutReader, shortcutNightMode,
                        shortcutQrScan, shortcutHistory, shortcutFindPage, shortcutShare,
                        shortcutFullscreen, shortcutBlockElement, shortcutSiteFilter,
                        shortcutVideoControls);
        new SettingsPanelController(this, mainHandler).showCustomize(state, (action, owner) -> {
            switch (action) {
                case TOP_RELOAD: topIconReload = !topIconReload; updateTopActionStates(); break;
                case TOP_BOOKMARK: topIconBookmark = !topIconBookmark; updateTopActionStates(); break;
                case TOP_TRANSLATE: topIconTranslate = !topIconTranslate; updateTopActionStates(); break;
                case RELOAD: shortcutReloadWebsite = !shortcutReloadWebsite; break;
                case DOWNLOAD: shortcutDownload = !shortcutDownload; break;
                case BOOKMARK: shortcutBookmark = !shortcutBookmark; break;
                case PRIVATE: shortcutPrivate = !shortcutPrivate; break;
                case AD_BLOCK: shortcutAdBlock = !shortcutAdBlock; break;
                case READER: shortcutReader = !shortcutReader; break;
                case NIGHT_MODE: shortcutNightMode = !shortcutNightMode; break;
                case QR_SCAN: shortcutQrScan = !shortcutQrScan; break;
                case HISTORY: shortcutHistory = !shortcutHistory; break;
                case FIND_PAGE: shortcutFindPage = !shortcutFindPage; break;
                case SHARE: shortcutShare = !shortcutShare; break;
                case FULLSCREEN: shortcutFullscreen = !shortcutFullscreen; break;
                case BLOCK_ELEMENT: shortcutBlockElement = !shortcutBlockElement; break;
                case SITE_FILTER: shortcutSiteFilter = !shortcutSiteFilter; break;
                case VIDEO_CONTROLS: shortcutVideoControls = !shortcutVideoControls; break;
            }
            saveSettings();
        });
    }

    void showDownloadSettingsPanel() {
        new DownloadSettingsController(this, new DownloadSettingsController.Host() {
            @Override
            public DownloadSettingsController.State state() {
                return new DownloadSettingsController.State(
                        getDownloadLocationText(),
                        downloadQueueEnabled,
                        downloadMaxActive,
                        downloadDynamic4Connections,
                        downloadAutoRetry,
                        downloadHlsEnabled,
                        downloadPlayWhileDownloadingEnabled,
                        downloadSpeedLimitKBps);
            }

            @Override
            public String queueSummary() {
                return getDownloadQueueSummary();
            }

            @Override
            public void handle(DownloadSettingsController.Action action,
                               int value,
                               Dialog ownerDialog) {
                switch (action) {
                    case OPEN_MANAGER:
                        showDownloadManager();
                        break;
                    case CHOOSE_FOLDER:
                        showDownloadFolderDialog(ownerDialog);
                        break;
                    case CLEAR_COMPLETED:
                        synchronized (downloadItems) {
                            for (int i = downloadItems.size() - 1; i >= 0; i--) {
                                if (!"running".equals(downloadItems.get(i).status)) {
                                    downloadItems.remove(i);
                                }
                            }
                        }
                        saveDownloadHistory();
                        QuietToast.makeText(MainActivity.this,
                                "Riwayat unduhan selesai dibersihkan",
                                QuietToast.LENGTH_SHORT).show();
                        break;
                    case TOGGLE_QUEUE:
                        downloadQueueEnabled = !downloadQueueEnabled;
                        downloadQueuePaused = false;
                        saveSettings();
                        pumpDownloadQueue();
                        refreshDownloadPanel();
                        break;
                    case SET_MAX_ACTIVE:
                        downloadMaxActive = value;
                        downloadQueueEnabled = true;
                        downloadQueuePaused = false;
                        saveSettings();
                        pumpDownloadQueue();
                        refreshDownloadPanel();
                        QuietToast.makeText(MainActivity.this,
                                "Maksimal download aktif: " + value,
                                QuietToast.LENGTH_SHORT).show();
                        break;
                    case PAUSE_ALL:
                        pauseAllDownloads();
                        break;
                    case RESUME_ALL:
                        resumeAllDownloads();
                        break;
                    case SORT_QUEUE:
                        activeDownloadSort = "Antrian";
                        renderDownloadList();
                        QuietToast.makeText(MainActivity.this,
                                "Tampilan diurutkan berdasarkan antrian",
                                QuietToast.LENGTH_SHORT).show();
                        break;
                    case TOGGLE_DYNAMIC_CONNECTIONS:
                        downloadDynamic4Connections = !downloadDynamic4Connections;
                        saveSettings();
                        break;
                    case TOGGLE_AUTO_RETRY:
                        downloadAutoRetry = !downloadAutoRetry;
                        saveSettings();
                        break;
                    case TOGGLE_HLS:
                        downloadHlsEnabled = !downloadHlsEnabled;
                        saveSettings();
                        break;
                    case TOGGLE_PLAY_WHILE_DOWNLOADING:
                        downloadPlayWhileDownloadingEnabled =
                                !downloadPlayWhileDownloadingEnabled;
                        saveSettings();
                        break;
                    case SET_SPEED_LIMIT:
                        downloadSpeedLimitKBps = value;
                        saveSettings();
                        QuietToast.makeText(MainActivity.this,
                                "Speed limiter: "
                                        + DownloadSettingsController.speedLabel(value),
                                QuietToast.LENGTH_SHORT).show();
                        break;
                }
            }
        }).showMain();
    }

    String getDownloadQueueSummary() {
        return "Maks aktif: " + downloadMaxActive
                + " • aktif: " + countActiveDownloads()
                + " • antri: " + countQueuedDownloads();
    }

    void showDownloadQueueSettingsDialog() {
        if (downloadQueueDialogController == null) {
            downloadQueueDialogController = new DownloadQueueDialogController(this);
        }
        downloadQueueDialogController.show(
                new DownloadQueueDialogController.State(
                        downloadQueueEnabled, downloadMaxActive),
                () -> {
                    downloadQueueEnabled = !downloadQueueEnabled;
                    downloadQueuePaused = false;
                    saveSettings();
                    pumpDownloadQueue();
                    refreshDownloadPanel();
                },
                value -> {
                    downloadMaxActive = value;
                    downloadQueueEnabled = true;
                    downloadQueuePaused = false;
                    saveSettings();
                    pumpDownloadQueue();
                    refreshDownloadPanel();
                    QuietToast.makeText(this,
                            "Maksimal download aktif: " + value,
                            QuietToast.LENGTH_SHORT).show();
                },
                MainActivity.this::pauseAllDownloads,
                MainActivity.this::resumeAllDownloads,
                () -> {
                    activeDownloadSort = "Antrian";
                    renderDownloadList();
                    QuietToast.makeText(this,
                            "Tampilan diurutkan berdasarkan antrian",
                            QuietToast.LENGTH_SHORT).show();
                },
                MainActivity.this::getDownloadQueueSummary);
    }

    void showVideoControlsIfAllowed() {
        if (!videoControlsEnabled || videoControlsManualHidden || webView == null || webView.getVisibility() != View.VISIBLE) {
            return;
        }
        if (videoControlsBar != null) videoControlsBar.setVisibility(View.VISIBLE);
        if (videoSpeedLabel != null) videoSpeedLabel.setText(VideoUi.formatVideoSpeed(videoSpeed));
        if (videoQualityLabel != null) videoQualityLabel.setText(selectedVideoQuality == null ? "Auto" : selectedVideoQuality);
        updateVideoModeToggleButton();
        refreshVideoPlayPauseButtonState();
    }

    LinearLayout createVideoControlsBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(6), dp(6), dp(6), dp(6));
        bar.setBackgroundColor(Color.parseColor("#101217"));

        bar.addView(videoTextButton("−10s", "Mundur 10 detik", v -> seekVideoBySeconds(-10)));
        videoPlayPauseButton = videoButton(R.drawable.ic_video_play, "Play/Pause", v -> controlVideo("toggle"));
        bar.addView(videoPlayPauseButton);
        bar.addView(videoTextButton("+10s", "Maju 10 detik", v -> seekVideoBySeconds(10)));

        videoSpeedLabel = new TextView(this);
        videoSpeedLabel.setText("1x");
        videoSpeedLabel.setTextColor(Color.parseColor("#111111"));
        videoSpeedLabel.setTextSize(13);
        videoSpeedLabel.setTypeface(Typeface.DEFAULT_BOLD);
        videoSpeedLabel.setGravity(Gravity.CENTER);
        videoSpeedLabel.setBackground(roundRect(COLOR_ACCENT, dp(18), 0, Color.TRANSPARENT));
        videoSpeedLabel.setOnClickListener(v -> showVideoSpeedDialog());
        LinearLayout.LayoutParams speedParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        speedParams.setMargins(dp(3), 0, dp(3), 0);
        bar.addView(videoSpeedLabel, speedParams);

        videoQualityLabel = new TextView(this);
        videoQualityLabel.setText(selectedVideoQuality == null ? "Auto" : selectedVideoQuality);
        videoQualityLabel.setTextColor(Color.WHITE);
        videoQualityLabel.setTextSize(11);
        videoQualityLabel.setTypeface(Typeface.DEFAULT_BOLD);
        videoQualityLabel.setGravity(Gravity.CENTER);
        videoQualityLabel.setBackground(roundRect(Color.parseColor("#20232A"), dp(18), dp(1), COLOR_BORDER));
        videoQualityLabel.setOnClickListener(v -> showVideoQualityDialog());
        LinearLayout.LayoutParams qualityParams = new LinearLayout.LayoutParams(dp(52), dp(48));
        qualityParams.setMargins(dp(3), 0, dp(3), 0);
        bar.addView(videoQualityLabel, qualityParams);

        videoModeToggleButton = new TextView(this);
        videoModeToggleButton.setText("Full");
        videoModeToggleButton.setTextColor(Color.WHITE);
        videoModeToggleButton.setTextSize(12);
        videoModeToggleButton.setTypeface(Typeface.DEFAULT_BOLD);
        videoModeToggleButton.setGravity(Gravity.CENTER);
        videoModeToggleButton.setContentDescription("Masuk layar penuh video");
        videoModeToggleButton.setBackground(roundRect(Color.parseColor("#20232A"), dp(18), dp(1), COLOR_BORDER));
        videoModeToggleButton.setOnClickListener(v -> toggleVideoFullLandscapeButton());
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(dp(56), dp(48));
        modeParams.setMargins(dp(3), 0, dp(3), 0);
        bar.addView(videoModeToggleButton, modeParams);

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(Color.WHITE);
        close.setTextSize(24);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> {
            videoControlsManualHidden = true;
            if (videoControlsBar != null) videoControlsBar.setVisibility(View.GONE);
            QuietToast.makeText(this, "Kontrol video disembunyikan. Ketuk video untuk munculkan lagi.", QuietToast.LENGTH_SHORT).show();
            mainHandler.postDelayed(() -> {
                videoControlsManualHidden = false;
                checkAndShowVideoControls();
            }, 2500);
        });
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(36), dp(48));
        closeParams.setMargins(dp(3), 0, dp(3), 0);
        bar.addView(close, closeParams);

        return bar;
    }

    View videoTextButton(String text, String label, View.OnClickListener listener) {
        return VideoUi.videoTextButton(this, text, label, listener);
    }

    View videoButton(int iconRes, String label, View.OnClickListener listener) {
        LinearLayout wrap = VideoUi.videoButton(this, iconRes, label, listener);
        if ("Play/Pause".equals(label)) videoPlayPauseIcon = (ImageView) wrap.getChildAt(0);
        return wrap;
    }

    void updateVideoPlayPauseButtonState(boolean playing) {
        try {
            if (videoPlayPauseIcon == null) return;
            videoPlayPauseIcon.setImageResource(playing ? R.drawable.ic_video_pause : R.drawable.ic_video_play);
            if (videoPlayPauseButton != null) {
                videoPlayPauseButton.setContentDescription(playing ? "Pause" : "Play");
            }
        } catch (Exception ignored) {
        }
    }

    void refreshVideoPlayPauseButtonState() {
        try {
            if (webView == null || videoPlayPauseIcon == null) return;
            webView.evaluateJavascript("(function(){try{var v=document.querySelector('video');return !!(v&&!v.paused&&!v.ended);}catch(e){return false;}})();", value -> updateVideoPlayPauseButtonState("true".equals(value)));
        } catch (Exception ignored) {
        }
    }

    void applyVideoControlsFullscreenLayout(boolean fullscreen) {
        try {
            if (videoControlsBar == null) return;
            videoControlsBar.setPadding(dp(fullscreen ? 12 : 6), dp(fullscreen ? 7 : 6), dp(fullscreen ? 12 : 6), dp(fullscreen ? 7 : 6));
            for (int i = 0; i < videoControlsBar.getChildCount(); i++) {
                View child = videoControlsBar.getChildAt(i);
                ViewGroup.LayoutParams raw = child.getLayoutParams();
                if (!(raw instanceof LinearLayout.LayoutParams)) continue;
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) raw;
                int margin = fullscreen ? dp(7) : dp(3);
                lp.setMargins(margin, 0, margin, 0);
                if (child == videoPlayPauseButton) lp.width = dp(fullscreen ? 60 : 48);
                else if (child == videoModeToggleButton) lp.width = dp(fullscreen ? 70 : 56);
                else if (child == videoSpeedLabel) lp.width = dp(fullscreen ? 60 : 48);
                else if (child == videoQualityLabel) lp.width = dp(fullscreen ? 66 : 52);
                else lp.width = dp(fullscreen ? 62 : 48);
                child.setLayoutParams(lp);
            }
        } catch (Exception ignored) {
        }
    }

    void nativeTapWebViewAtRatio(double xRatio, double yRatio) {
        try {
            if (webView == null) return;
            if (xRatio < 0 || yRatio < 0 || xRatio > 1 || yRatio > 1) return;
            final float x = (float) (webView.getWidth() * xRatio);
            final float y = (float) (webView.getHeight() * yRatio);
            if (x < 1 || y < 1 || x > webView.getWidth() - 1 || y > webView.getHeight() - 1) return;
            long now = android.os.SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
            MotionEvent up = MotionEvent.obtain(now, now + 70, MotionEvent.ACTION_UP, x, y, 0);
            webView.dispatchTouchEvent(down);
            webView.dispatchTouchEvent(up);
            down.recycle();
            up.recycle();
        } catch (Exception ignored) {
        }
    }

    void updateVideoControlsVisibility() {
        if (videoControlsBar == null) return;
        // Jangan muncul hanya karena halaman punya video.
        // Kontrol baru muncul setelah JS mendeteksi video benar-benar play/playing.
        videoControlsBar.setVisibility(View.GONE);
        videoControlsManualHidden = false;
        injectVideoPlaybackWatcher();
    }

    void seekVideoBySeconds(int seconds) {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            QuietToast.makeText(this, "Buka video dulu", QuietToast.LENGTH_SHORT).show();
            return;
        }
        String js = "javascript:(function(){var v=document.querySelector('video');if(v){try{v.currentTime=Math.max(0,Math.min((v.duration||999999),v.currentTime+" + seconds + "));}catch(e){} }})()";
        runPageScript(js);
        refreshVideoPlayPauseButtonState();
    }

    void checkAndShowVideoControls() {
        if (webView == null || videoControlsBar == null || !videoControlsEnabled) return;
        try {
            webView.evaluateJavascript("(function(){var v=document.querySelector('video');return !!(v&&!v.paused&&!v.ended);})()", value -> {
                if ("true".equals(value)) {
                    showVideoControlsIfAllowed();
                }
            });
        } catch (Exception ignored) {
        }
    }

    void toggleVideoFullLandscapeButton() {
        // v0.9.51: mode Lans dihapus. Tombol ini hanya mengatur Fullscreen Video.
        // Full -> masuk fullscreen landscape. Exit/Back -> kembali ke browser normal portrait.
        if (isVideoFullscreenActive()) {
            exitVideoFullscreenToPortraitMode();
            updateVideoModeToggleButton();
            mainHandler.postDelayed(() -> updateVideoModeToggleButton(), 250);
            return;
        }
        enterVideoFullscreen();
        updateVideoModeToggleButton();
        mainHandler.postDelayed(() -> updateVideoModeToggleButton(), 300);
        mainHandler.postDelayed(() -> updateVideoModeToggleButton(), 900);
    }

    void updateVideoModeToggleButton() {
        try {
            if (videoModeToggleButton == null) return;
            if (isVideoFullscreenActive()) {
                videoModeToggleButton.setText("Exit");
                videoModeToggleButton.setContentDescription("Keluar dari layar penuh video");
            } else {
                videoModeToggleButton.setText("Full");
                videoModeToggleButton.setContentDescription("Masuk layar penuh video");
            }
        } catch (Exception ignored) {
        }
    }

    boolean isVideoFullscreenActive() {
        try {
            if (fullscreenVideoView != null) return true;
            return topBarView != null
                    && topBarView.getVisibility() == View.GONE
                    && webView != null
                    && webView.getVisibility() == View.VISIBLE;
        } catch (Exception e) {
            return false;
        }
    }

    void exitVideoFullscreenToPortraitMode() {
        try {

            if (fullscreenVideoView != null) {
                try {
                    FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                    decor.removeView(fullscreenVideoView);
                } catch (Exception ignored) {}
                fullscreenVideoView = null;
                if (fullscreenVideoCallback != null) {
                    try { fullscreenVideoCallback.onCustomViewHidden(); } catch (Exception ignored) {}
                    fullscreenVideoCallback = null;
                }
            }

            restoreAfterVideoFullscreen();
            forcePortraitAfterVideoFullscreen();
            restoreVideoControlsFromFullscreenOverlay();
            if (topBarView != null) topBarView.setVisibility(View.VISIBLE);
            if (bottomNavView != null) bottomNavView.setVisibility(View.VISIBLE);
            updateVideoModeToggleButton();
            videoControlsManualHidden = false;
            mainHandler.postDelayed(() -> { updateVideoModeToggleButton(); checkAndShowVideoControls(); }, 180);
            QuietToast.makeText(this, "Keluar dari fullscreen video", QuietToast.LENGTH_SHORT).show();
        } catch (Exception e) {
            try {
                restoreAfterVideoFullscreen();
                forcePortraitAfterVideoFullscreen();
            } catch (Exception ignored) {}
        }
    }

    void enterVideoFullscreen() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            QuietToast.makeText(this, "Buka video dulu", QuietToast.LENGTH_SHORT).show();
            return;
        }

        String js =
                "(function(){"
                        + "try{"
                        + "var v=null;"
                        + "var videos=document.querySelectorAll('video');"
                        + "for(var i=0;i<videos.length;i++){var r=videos[i].getBoundingClientRect();if(r.width>80&&r.height>60){v=videos[i];break;}}"
                        + "if(!v&&videos.length>0)v=videos[0];"
                        + "if(v){"
                        + " if(v.requestFullscreen){v.requestFullscreen();return 'video_requestFullscreen';}"
                        + " if(v.webkitRequestFullscreen){v.webkitRequestFullscreen();return 'video_webkitRequestFullscreen';}"
                        + " if(v.webkitEnterFullscreen){v.webkitEnterFullscreen();return 'video_webkitEnterFullscreen';}"
                        + "}"
                        + "var btns=document.querySelectorAll('button,[role=button],div,span');"
                        + "for(var b=0;b<btns.length&&b<900;b++){"
                        + " var t=((btns[b].innerText||btns[b].textContent||btns[b].getAttribute('aria-label')||btns[b].title||'')+'').toLowerCase();"
                        + " var c=((btns[b].className||'')+'').toLowerCase();"
                        + " if(t.indexOf('fullscreen')>-1||t.indexOf('layar penuh')>-1||t.indexOf('full screen')>-1||c.indexOf('fullscreen')>-1){try{btns[b].click();return 'clicked_fullscreen';}catch(e){}}"
                        + "}"
                        + "var el=document.documentElement;"
                        + "if(el.requestFullscreen){el.requestFullscreen();return 'page_fullscreen';}"
                        + "if(el.webkitRequestFullscreen){el.webkitRequestFullscreen();return 'page_webkit_fullscreen';}"
                        + "return 'not_supported';"
                        + "}catch(e){return 'error';}"
                        + "})()";

        try {
            webView.evaluateJavascript(js, value -> {
                String result = value == null ? "" : value.replace("\"", "");
                if (result.contains("not_supported") || result.contains("error")) {
                    openAppVideoFullscreenFallback();
                } else {
                    updateVideoModeToggleButton();
                    mainHandler.postDelayed(() -> {
                        if (!isVideoFullscreenActive()) openAppVideoFullscreenFallback();
                        updateVideoModeToggleButton();
                    }, 450);
                    mainHandler.postDelayed(() -> updateVideoModeToggleButton(), 900);
                }
            });
        } catch (Exception e) {
            openAppVideoFullscreenFallback();
        }
    }

    void openAppVideoFullscreenFallback() {
        try {
            if (homeScroll != null && homeScroll.getVisibility() == View.VISIBLE) {
                restoreHiddenWebPage();
            }
            originalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            if (topBarView != null) topBarView.setVisibility(View.GONE);
            if (bottomNavView != null) bottomNavView.setVisibility(View.GONE);
            moveVideoControlsToFullscreenOverlay();
            updateVideoModeToggleButton();
            videoControlsManualHidden = false;
            checkAndShowVideoControls();
        } catch (Exception e) {
            QuietToast.makeText(this, "Mode fullscreen tidak didukung di halaman ini", QuietToast.LENGTH_SHORT).show();
        }
    }

    void restoreAfterVideoFullscreen() {
        try {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
            forcePortraitAfterVideoFullscreen();
            restoreVideoControlsFromFullscreenOverlay();
            if (topBarView != null) topBarView.setVisibility(View.VISIBLE);
            if (bottomNavView != null) bottomNavView.setVisibility(View.VISIBLE);
            updateVideoModeToggleButton();
            videoControlsManualHidden = false;
            checkAndShowVideoControls();
        } catch (Exception ignored) {
        }
    }

    void forcePortraitAfterVideoFullscreen() {
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } catch (Exception ignored) {
        }
    }

    void moveVideoControlsToFullscreenOverlay() {
        try {
            if (videoControlsBar == null || videoControlsInFullscreen) return;

            ViewGroup parent = (ViewGroup) videoControlsBar.getParent();
            if (parent != null) {
                videoControlsOriginalParent = parent;
                videoControlsOriginalLayoutParams = videoControlsBar.getLayoutParams();
                videoControlsOriginalIndex = parent.indexOfChild(videoControlsBar);
                parent.removeView(videoControlsBar);
            }

            // Fullscreen: posisi tetap di bawah-tengah seperti request user.
            // Dibuat floating pill dengan margin bawah agar tidak menutupi timeline/durasi bawaan player.
            FrameLayout decor = (FrameLayout) getWindow().getDecorView();
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
            );
            lp.setMargins(dp(8), 0, dp(8), dp(22));
            videoControlsBar.setBackground(roundRect(Color.parseColor("#D0101217"), dp(24), dp(1), Color.parseColor("#30343C")));
            applyVideoControlsFullscreenLayout(true);
            decor.addView(videoControlsBar, lp);

            videoControlsInFullscreen = true;
            videoControlsManualHidden = false;
            videoControlsBar.bringToFront();
            updateVideoModeToggleButton();
            videoControlsBar.setVisibility(View.VISIBLE);
        } catch (Exception ignored) {
        }
    }

    void showTranslateOptionsDialog() {
        String current = getEffectiveCurrentUrl();
        String original = getOriginalForTranslate(current);

        ArrayList<String> options = new ArrayList<>();
        options.add("Pilih bahasa target: " + translateTargetLabel);
        options.add("Terjemahkan halaman ke " + translateTargetLabel + " (kompatibel)");
        options.add("Lanjutkan translate bagian belum diterjemahkan");
        options.add("Google Translate proxy (ada bar bahasa)");
        options.add(hideGoogleTranslateBar ? "Tampilkan bar Google Translate" : "Sembunyikan bar Google Translate");
        options.add("Terjemahkan teks halaman saja");
        options.add("Reload website");
        options.add("Aktifkan klik menu website");
        if (translateEnabled || compatibleTranslateActive || isGoogleTranslatedUrl(webView != null ? webView.getUrl() : "")) {
            options.add("Matikan translate / buka halaman asli");
        }

        String[] items = options.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Translate")
                .setItems(items, (dialog, which) -> {
                    String selected = items[which];
                    if (selected.startsWith("Pilih bahasa")) {
                        showTranslateLanguageDialog();
                    } else if (selected.startsWith("Terjemahkan halaman")) {
                        if (original == null || original.length() == 0) {
                            QuietToast.makeText(this, "Buka website dulu untuk translate", QuietToast.LENGTH_SHORT).show();
                            return;
                        }
                        startCompatibleTranslateSession(original);
                        updateTopActionStates();
                        translatePageCompatible();
                    } else if (selected.startsWith("Lanjutkan translate")) {
                        continueCompatibleTranslation();
                    } else if (selected.startsWith("Google Translate proxy")) {
                        if (original == null || original.length() == 0) {
                            QuietToast.makeText(this, "Buka website dulu untuk translate", QuietToast.LENGTH_SHORT).show();
                            return;
                        }
                        startGoogleTranslateSession(original);
                        updateTopActionStates();
                        loadTranslatedPage(original);
                    } else if (selected.startsWith("Tampilkan bar")) {
                        hideGoogleTranslateBar = false;
                        saveSettings();
                        showGoogleTranslateBar();
                        QuietToast.makeText(this, "Bar Google Translate ditampilkan", QuietToast.LENGTH_SHORT).show();
                    } else if (selected.startsWith("Sembunyikan bar")) {
                        hideGoogleTranslateBar = true;
                        saveSettings();
                        unblockTranslatedPageClicks();
                        QuietToast.makeText(this, "Bar Google Translate disembunyikan dan klik website diaktifkan", QuietToast.LENGTH_SHORT).show();
                    } else if (selected.startsWith("Terjemahkan teks")) {
                        translatePageTextOnly();
                    } else if (selected.startsWith("Reload")) {
                        reloadCurrentWebsite();
                    } else if (selected.startsWith("Aktifkan klik")) {
                        unblockTranslatedPageClicks();
                        QuietToast.makeText(this, "Klik menu website diaktifkan", QuietToast.LENGTH_SHORT).show();
                    } else if (selected.startsWith("Matikan")) {
                        disableTranslateAndRestore(current);
                    }
                })
                .setNegativeButton("Tutup", null)
                .show();
    }

    String getOriginalForTranslate(String maybeUrl) {
        try {
            String url = maybeUrl;
            if ((url == null || url.length() == 0) && webView != null) url = webView.getUrl();
            if (url == null || url.length() == 0) return "";

            String extracted = extractOriginalUrl(url);
            if (extracted != null && extracted.length() > 0 && !isGoogleTranslatedUrl(extracted)) return extracted;

            if (isGoogleTranslatedUrl(url) && lastTranslateOriginalUrl != null && lastTranslateOriginalUrl.length() > 0) {
                return lastTranslateOriginalUrl;
            }
            return url;
        } catch (Exception e) {
            return "";
        }
    }

    boolean isGoogleTranslatedUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.US);
        return lower.contains("translate.google.") || lower.contains(".translate.goog") || lower.contains("_x_tr_sl=");
    }

    void detectTranslateProxyBlocked(String url) {
        if (!isGoogleTranslatedUrl(url) || webView == null) return;
        try {
            webView.evaluateJavascript("(function(){var t=(document.body&&document.body.innerText?document.body.innerText:'').toLowerCase();return t.indexOf('aku bukan robot')>-1||t.indexOf('not a robot')>-1||t.indexOf('captcha')>-1||t.indexOf('verify')>-1;})()", value -> {
                if ("true".equals(value)) {
                    if (!translateEnabled || translateManuallyDisabled) return;
                    QuietToast.makeText(this, "Website menolak Google Translate. Beralih ke mode kompatibel.", QuietToast.LENGTH_LONG).show();
                    String raw = lastTranslateOriginalUrl != null && lastTranslateOriginalUrl.length() > 0 ? lastTranslateOriginalUrl : getOriginalForTranslate(url);
                    if (raw != null && raw.length() > 0) {
                        startCompatibleTranslateSession(raw);
                        final int token = translateSessionToken;
                        loadBrowserUrl(raw);
                        mainHandler.postDelayed(() -> translatePageCompatible(token), 1800);
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    void startCompatibleTranslateSession(String originalUrl) {
        translateEnabled = true;
        compatibleTranslateActive = true;
        translateManuallyDisabled = false;
        translateSessionToken++;
        if (originalUrl != null && originalUrl.length() > 0) lastTranslateOriginalUrl = originalUrl;
    }

    void startGoogleTranslateSession(String originalUrl) {
        translateEnabled = true;
        compatibleTranslateActive = false;
        translateManuallyDisabled = false;
        translateSessionToken++;
        if (originalUrl != null && originalUrl.length() > 0) lastTranslateOriginalUrl = originalUrl;
    }

    void disableTranslateAndRestore(String currentUrl) {
        translateSessionToken++;
        translateEnabled = false;
        compatibleTranslateActive = false;
        translateManuallyDisabled = true;
        lastCompatibleTranslateStartedAt = 0L;
        clearCompatibleTranslationMarks();
        updateTopActionStates();
        String raw = getOriginalForTranslate(currentUrl);
        if ((raw == null || raw.length() == 0) && lastTranslateOriginalUrl != null && lastTranslateOriginalUrl.length() > 0) raw = lastTranslateOriginalUrl;
        if (raw != null && raw.length() > 0 && isGoogleTranslatedUrl(webView != null ? webView.getUrl() : "")) {
            loadBrowserUrl(raw);
        }
        lastTranslateOriginalUrl = "";
        QuietToast.makeText(this, "Translate dimatikan", QuietToast.LENGTH_SHORT).show();
    }

    boolean isCompatibleTranslateAllowed(int token) {
        return token == translateSessionToken && translateEnabled && compatibleTranslateActive && !translateManuallyDisabled;
    }

    void runJsOnCurrentPage(String script) {
        if (webView == null || script == null || script.length() == 0) return;
        try {
            String code = script;
            if (code.startsWith("javascript:")) code = code.substring("javascript:".length());
            webView.evaluateJavascript(code, null);
        } catch (Exception ignored) {
        }
    }

    void translatePageCompatible() {
        translatePageCompatible(translateSessionToken);
    }

    void translatePageCompatible(int token) {
        if (!isCompatibleTranslateAllowed(token)) return;
        long now = System.currentTimeMillis();
        if (lastCompatibleTranslateStartedAt > 0 && now - lastCompatibleTranslateStartedAt < 900) return;
        lastCompatibleTranslateStartedAt = now;
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            QuietToast.makeText(this, "Buka website dulu untuk translate kompatibel", QuietToast.LENGTH_SHORT).show();
            return;
        }

        String js =
                "javascript:(function(){"
                        + "try{"
                        + "window.__yieldCompatibleTranslateRunning=true;"
                        + "if(!window.__yieldTextNodes)window.__yieldTextNodes=[];"
                        + "function skip(n){"
                        + " var p=n.parentNode;if(!p)return true;"
                        + " if(n.__yieldTranslated)return true;"
                        + " var tag=(p.nodeName||'').toLowerCase();"
                        + " if(['script','style','noscript','textarea','input','select','option','code','pre'].indexOf(tag)>=0)return true;"
                        + " if(p.closest&&p.closest('[contenteditable=true],.notranslate,#yield-hide-translate-bar'))return true;"
                        + " return false;"
                        + "}"
                        + "var walker=document.createTreeWalker(document.body||document.documentElement,NodeFilter.SHOW_TEXT,{acceptNode:function(n){"
                        + " if(skip(n))return NodeFilter.FILTER_REJECT;"
                        + " var t=(n.nodeValue||'').replace(/\\s+/g,' ').trim();"
                        + " if(t.length<2||t.length>900)return NodeFilter.FILTER_REJECT;"
                        + " if(/^[-–—→\\s\\d.,:;!?'\"()\\[\\]{}]+$/.test(t))return NodeFilter.FILTER_REJECT;"
                        + " return NodeFilter.FILTER_ACCEPT;"
                        + "}},false);"
                        + "var node;var max=260;var sent=0;"
                        + "window.__yieldApplyTranslation=function(idx,text){try{var n=window.__yieldTextNodes[idx];if(n&&text){if(!n.__yieldOriginal)n.__yieldOriginal=n.nodeValue;n.nodeValue=text;n.__yieldTranslated=true;}}catch(e){}};"
                        + "while((node=walker.nextNode())&&sent<max){"
                        + " var text=(node.nodeValue||'').replace(/\\s+/g,' ').trim();"
                        + " var idx=window.__yieldTextNodes.length;"
                        + " window.__yieldTextNodes.push(node);"
                        + " try{YieldTranslateBridge.translateText(idx,text);}catch(e){}"
                        + " sent++;"
                        + "}"
                        + "window.__yieldCompatibleTranslateRunning=false;"
                        + "try{YieldTranslateBridge.onCollected(sent);}catch(e){}"
                        + "}catch(e){try{YieldTranslateBridge.onCollected(0);}catch(x){}}"
                        + "})()";
        try {
            if (!isCompatibleTranslateAllowed(token)) return;
            runJsOnCurrentPage(js);
            updateTopActionStates();
            QuietToast.makeText(this, "Translate kompatibel aktif ke " + translateTargetLabel, QuietToast.LENGTH_SHORT).show();
        } catch (Exception e) {
            QuietToast.makeText(this, "Translate kompatibel gagal", QuietToast.LENGTH_SHORT).show();
        }
    }

    void continueCompatibleTranslation() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            QuietToast.makeText(this, "Buka website dulu", QuietToast.LENGTH_SHORT).show();
            return;
        }
        startCompatibleTranslateSession(getOriginalForTranslate(getEffectiveCurrentUrl()));
        updateTopActionStates();
        translatePageCompatible();
    }

    void clearCompatibleTranslationMarks() {
        if (webView == null) return;
        String js =
                "javascript:(function(){"
                        + "try{"
                        + "if(window.__yieldTextNodes){for(var i=0;i<window.__yieldTextNodes.length;i++){var n=window.__yieldTextNodes[i];if(n&&n.__yieldOriginal){n.nodeValue=n.__yieldOriginal;n.__yieldTranslated=false;}}}"
                        + "window.__yieldTextNodes=[];"
                        + "window.__yieldCompatibleTranslateRunning=false;"
                        + "}catch(e){}"
                        + "})()";
        try {
            runJsOnCurrentPage(js);
        } catch (Exception ignored) {
        }
    }

    void applyCompatibleTranslation(int index, String translated) {
        if (webView == null) return;
        try {
            if (!isCompatibleTranslateAllowed(translateSessionToken)) return;
            String js = "javascript:(function(){try{if(window.__yieldApplyTranslation)window.__yieldApplyTranslation("
                    + index + "," + org.json.JSONObject.quote(translated) + ");}catch(e){}})()";
            runJsOnCurrentPage(js);
        } catch (Exception ignored) {
        }
    }

    String translateTextViaGoogle(String text) {
        HttpURLConnection conn = null;
        try {
            String q = URLEncoder.encode(text, "UTF-8");
            URL url = new URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" + translateTargetLang + "&dt=t&q=" + q);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 YieldBrowser");
            conn.setRequestProperty("Accept", "application/json,text/plain,*/*");

            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            org.json.JSONArray root = new org.json.JSONArray(response.toString());
            org.json.JSONArray sentences = root.getJSONArray(0);
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < sentences.length(); i++) {
                org.json.JSONArray part = sentences.getJSONArray(i);
                out.append(part.optString(0, ""));
            }
            String result = out.toString().trim();
            return result.length() > 0 ? result : text;
        } catch (Exception e) {
            return text;
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }
    }

    void loadTranslatedPage(String originalUrl) {
        if (originalUrl == null || originalUrl.length() == 0) return;
        try {
            if (!translateEnabled || translateManuallyDisabled) startGoogleTranslateSession(originalUrl);
            compatibleTranslateActive = false;
            lastTranslateOriginalUrl = originalUrl;
            String encoded = URLEncoder.encode(originalUrl, "UTF-8");
            loadBrowserUrl("https://translate.google.com/translate?sl=auto&tl=" + translateTargetLang + "&u=" + encoded);
            updateTopActionStates();
            QuietToast.makeText(this, "Membuka Google Translate ke " + translateTargetLabel, QuietToast.LENGTH_SHORT).show();
        } catch (Exception e) {
            loadBrowserUrl(originalUrl);
        }
    }

    void hideGoogleTranslateToolbar() {
        if (webView == null) return;
        String js =
                "javascript:(function(){"
                        + "try{"
                        + "var id='yield-hide-translate-bar';"
                        + "var old=document.getElementById(id);if(old)old.remove();"
                        + "var s=document.createElement('style');s.id=id;"
                        + "s.innerHTML="
                        + "'iframe.skiptranslate,.skiptranslate,body>.skiptranslate,#goog-gt-tt,.goog-te-banner-frame,.goog-te-balloon-frame,#google_translate_element,#gt-appbar{display:none!important;visibility:hidden!important;height:0!important;max-height:0!important;overflow:hidden!important;pointer-events:none!important;opacity:0!important;}' + "
                        + "'.goog-text-highlight{background:transparent!important;box-shadow:none!important;border:none!important;}' + "
                        + "'body{top:0!important;margin-top:0!important;padding-top:0!important;}' + "
                        + "'html{margin-top:0!important;padding-top:0!important;}';"
                        + "document.head.appendChild(s);"
                        + "if(document.body){document.body.style.top='0px';document.body.style.marginTop='0px';document.body.style.paddingTop='0px';}"
                        + "var frames=document.querySelectorAll('iframe');for(var i=0;i<frames.length;i++){try{var src=frames[i].src||'';var cls=frames[i].className||'';if(src.indexOf('translate')>-1||String(cls).indexOf('skiptranslate')>-1){frames[i].style.display='none';frames[i].style.height='0';frames[i].style.pointerEvents='none';}}catch(e){}}"
                        + "}catch(e){}"
                        + "})()";
        try {
            runJsOnCurrentPage(js);
        } catch (Exception ignored) {
        }
    }

    void unblockTranslatedPageClicks() {
        if (webView == null) return;
        try {
            hideGoogleTranslateToolbar();
            String js =
                    "javascript:(function(){"
                            + "try{"
                            + "var all=document.querySelectorAll('a,button,input,select,textarea,label,summary,[onclick],[role=button],[tabindex],li,nav,menu');"
                            + "for(var i=0;i<all.length;i++){all[i].style.pointerEvents='auto';all[i].style.touchAction='auto';}"
                            + "if(document.body){document.body.style.pointerEvents='auto';document.body.style.touchAction='auto';}"
                            + "}catch(e){}"
                            + "})()";
            runJsOnCurrentPage(js);
        } catch (Exception ignored) {
        }
    }

    void showGoogleTranslateBar() {
        if (webView == null) return;
        String js =
                "javascript:(function(){"
                        + "try{var s=document.getElementById('yield-hide-translate-bar');if(s)s.remove();document.body.style.top='';}"
                        + "catch(e){}"
                        + "})()";
        try {
            runJsOnCurrentPage(js);
        } catch (Exception ignored) {
        }
    }

    void translatePageTextOnly() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            QuietToast.makeText(this, "Buka halaman dulu", QuietToast.LENGTH_SHORT).show();
            return;
        }
        try {
            webView.evaluateJavascript("(function(){return (document.body&&document.body.innerText?document.body.innerText:'').slice(0,3500);})()", value -> {
                try {
                    String text = value == null ? "" : value;
                    if (text.startsWith("\"") && text.endsWith("\"")) text = text.substring(1, text.length() - 1);
                    text = text.replace("\\n", "\n").replace("\\\"", "\"").replace("\\u003C", "<");
                    if (text.trim().length() == 0) {
                        QuietToast.makeText(this, "Teks halaman tidak terbaca", QuietToast.LENGTH_SHORT).show();
                        return;
                    }
                    startGoogleTranslateSession(getOriginalForTranslate(getEffectiveCurrentUrl()));
                    compatibleTranslateActive = false;
                    updateTopActionStates();
                    String encoded = URLEncoder.encode(text, "UTF-8");
                    loadBrowserUrl("https://translate.google.com/?sl=auto&tl=" + translateTargetLang + "&op=translate&text=" + encoded);
                    QuietToast.makeText(this, "Menerjemahkan teks halaman ke " + translateTargetLabel, QuietToast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    QuietToast.makeText(this, "Gagal ambil teks halaman", QuietToast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            QuietToast.makeText(this, "Translate teks tidak didukung di halaman ini", QuietToast.LENGTH_SHORT).show();
        }
    }

    void reloadCurrentWebsite() {
        try {
            String url = getSafeReloadUrlForModeChange();
            if (url == null || url.trim().length() == 0) url = getEffectiveCurrentUrl();
            url = normalizeUrlForCurrentBrowserMode(url);

            if (webView != null && webView.getVisibility() == View.VISIBLE && url != null && url.length() > 0) {
                // v0.9.40: reload di browser mode harus hard reload, bukan webView.reload().
                // webView.reload() sering memakai document/UA lama, jadi Desktop ON/OFF tidak langsung berubah.
                hardReloadUrlWithCurrentBrowserMode(url, true);
                QuietToast.makeText(this, desktopMode ? "Reload desktop mode" : "Reload mobile mode", QuietToast.LENGTH_SHORT).show();
            } else {
                if (url != null && url.length() > 0) {
                    addressBar.setText(url);
                    openAddressBarUrl();
                } else {
                    QuietToast.makeText(this, "Belum ada website untuk reload", QuietToast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            QuietToast.makeText(this, "Gagal reload website", QuietToast.LENGTH_SHORT).show();
        }
    }

    boolean captureAdRedirectToTempTab(String url) {
        return captureBlockedNavigationToTempTab(url, false);
    }

    boolean captureDirectImageToTempTab(String url) {
        return captureBlockedNavigationToTempTab(url, true);
    }

    boolean captureBlockedNavigationToTempTab(String url, boolean allowWhenAdBlockOff) {
        if ((!allowWhenAdBlockOff && !adBlock) || url == null || url.trim().length() == 0) return false;
        if (isMediaResourceUrl(url) || isYoutubeCoreUrl(url)) return false;

        String safeUrl = url.trim();

        if (!adBlockRedirectToTempTab) {
            scheduleCloseDetectedAdTabs();
            return true;
        }

        // v0.10.18: quarantine must still create an isolated ad tab even when auto-close is on.
        // The protected tab remains selected, while the ad tab is marked and closed silently.
        synchronized (tabs) {
            for (TabInfo tab : tabs) {
                if (tab != null && tab.adTab && safeUrl.equals(tab.url)) {
                    updateTabsCountUi();
                    return true;
                }
            }
        }

        int protectedIndex = currentTabIndex;
        TabInfo adTab = createProfileTab("Tab iklan", "Tab iklan privat", safeUrl, false, true);
        tabs.add(adTab);
        updateTabsCountUi();

        QuietToast.makeText(this, "Direct link dibuka di tab baru", QuietToast.LENGTH_SHORT).show();

        // Tetap tidak mengganggu tab utama. Jika auto-close aktif, tab iklan ditutup sedikit lebih lama
        // agar tidak terasa seperti flicker.
        if (adBlockAutoCloseAdTabs) {
            mainHandler.postDelayed(() -> closeAdTabSilently(adTab, protectedIndex), 4500);
            mainHandler.postDelayed(this::closeDetectedAdTabs, 6500);
        }
        return true;
    }

    void closeAdTabSilently(TabInfo adTab, int fallbackIndex) {
        try {
            if (adTab == null || !adTab.adTab) return;
            int index = tabs.indexOf(adTab);
            if (index < 0) return;

            boolean closingCurrent = index == currentTabIndex;
            if (closingCurrent) invalidateTabScopedAsyncWork();
            adTab.closed = true;
            destroyTabWebView(adTab);
            tabs.remove(index);

            if (tabs.isEmpty()) {
                tabs.add(createProfileTab("Tab utama", "Tab privat", "", false));
                currentTabIndex = 0;
                addressBar.setText("");
                if (homeSearchInput != null) homeSearchInput.setText("");
                activateTabWebView(getCurrentTab(), false);
                skipNextShowHomeTabSave = true;
                showHome();
            } else {
                currentTabIndex = TabNavigationPolicy.indexAfterClosingAdTab(
                        currentTabIndex, index, fallbackIndex, tabs.size(), closingCurrent);
                if (closingCurrent) {
                    skipNextSwitchTabStateSave = true;
                    switchToTab(currentTabIndex);
                }
            }

            updateTabsCountUi();
        } catch (Exception ignored) {
        }
    }

    void scheduleCloseDetectedAdTabs() {
        if (!adBlockAutoCloseAdTabs) return;
        mainHandler.postDelayed(this::closeDetectedAdTabs, 900);
    }

    void closeDetectedAdTabs() {
        try {
            if (!adBlockAutoCloseAdTabs || tabs.isEmpty()) return;

            boolean changed = false;
            for (int i = tabs.size() - 1; i >= 0; i--) {
                TabInfo tab = tabs.get(i);
                if (tab == null || !tab.adTab) continue;

                boolean closingCurrent = i == currentTabIndex;
                if (closingCurrent) invalidateTabScopedAsyncWork();
                tab.closed = true;
                destroyTabWebView(tab);
                tabs.remove(i);
                changed = true;

                currentTabIndex = TabNavigationPolicy.indexAfterDetectedAdRemoval(
                        currentTabIndex, i, tabs.size(), closingCurrent);
            }

            if (tabs.isEmpty()) {
                tabs.add(createProfileTab("Tab utama", "Tab privat", "", false));
                currentTabIndex = 0;
                addressBar.setText("");
                if (homeSearchInput != null) homeSearchInput.setText("");
                activateTabWebView(getCurrentTab(), false);
                skipNextShowHomeTabSave = true;
                showHome();
            } else if (!TabNavigationPolicy.isValidIndex(currentTabIndex, tabs.size())) {
                currentTabIndex = TabNavigationPolicy.clampIndex(currentTabIndex, tabs.size());
            }

            if (changed && !tabs.isEmpty()) {
                skipNextSwitchTabStateSave = true;
                switchToTab(currentTabIndex);
            }

            if (changed) {
                updateTabsCountUi();
            }
        } catch (RuntimeException ignored) {
            // A stale WebView/tab must not interrupt browsing in the remaining tabs.
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    WebView createBrowserWebView(TabInfo owner, int visibility) {
        WebView fresh = new WebView(this);
        fresh.setVisibility(visibility);
        if (owner != null) {
            long generation = ++owner.webViewGeneration;
            fresh.setTag(new TabWebViewLifecycle.Binding(owner, generation));
        }
        boolean privateProfile = dedicatedPrivateProfile || (owner != null && owner.privateTab);
        BrowserWebViewSettings.prepareNewWebView(fresh, privateProfile);
        webView = fresh;
        configureWebView();
        return fresh;
    }

    void recreateBrowserWebViewForMode(String targetUrl, boolean showWebPage) {
        if (contentFrame == null) {
            if (targetUrl != null && targetUrl.trim().length() > 0) loadBrowserUrl(targetUrl);
            return;
        }
        int token = ++browserModeToken;
        TabInfo activeTab = getCurrentTab();
        try { destroyTabWebView(activeTab); } catch (Exception ignored) {}

        WebView fresh = createBrowserWebView(activeTab, showWebPage ? View.VISIBLE : View.GONE);
        activeTab.webView = fresh;
        attachWebViewToContentFrame(fresh);
        hideInactiveTabWebViews(fresh);

        if (showWebPage) {
            if (homeScroll != null) homeScroll.setVisibility(View.GONE);
            fresh.setVisibility(View.VISIBLE);
        }
        updateVideoControlsVisibility();
        if (targetUrl != null && targetUrl.trim().length() > 0) {
            mainHandler.postDelayed(() -> {
                if (token != browserModeToken) return;
                applyBrowserSettings();
                loadBrowserUrl(targetUrl);
            }, 180);
        }
    }

    void hardReloadUrlWithCurrentBrowserMode(String targetUrl, boolean showWebPage) {
        if (targetUrl == null || targetUrl.trim().length() == 0) return;
        try {
            targetUrl = normalizeUrlForCurrentBrowserMode(targetUrl);
            if (addressBar != null) addressBar.setText(targetUrl);
            recreateBrowserWebViewForMode(targetUrl, showWebPage);
            if (!desktopMode) scheduleMobileViewportReset();
        } catch (Exception e) {
            try {
                targetUrl = normalizeUrlForCurrentBrowserMode(targetUrl);
                applyBrowserSettings();
                loadBrowserUrl(targetUrl);
                if (!desktopMode) scheduleMobileViewportReset();
            } catch (Exception ignored) {}
        }
    }

    void removeShieldDocumentStartScript(WebView target) {
        if (target == null) return;
        try {
            ScriptHandler handler = shieldDocumentStartHandlers.remove(target);
            if (handler != null && WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                handler.remove();
            }
        } catch (Exception ignored) {
        }
    }

    void installShieldDocumentStartScript(WebView target) {
        if (target == null) return;
        removeShieldDocumentStartScript(target);
        try {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return;
            ScriptHandler handler = WebViewCompat.addDocumentStartJavaScript(
                    target,
                    ShieldPageScript.documentStart(adBlock, adBlockPopupBlocker,
                            adBlockRedirectBlocker, adBlockScriptIframeBlocker,
                            adBlockClickHijackBlocker),
                    Collections.singleton("*"));
            shieldDocumentStartHandlers.put(target, handler);
        } catch (Exception ignored) {
            // The installed Android System WebView may not expose document-start injection.
            // Page-commit/page-finished fallback remains active below.
        }
    }

    void injectShieldEngineV2Fallback() {
        if (webView == null) return;
        try {
            runPageScript(ShieldPageScript.documentStart(adBlock, adBlockPopupBlocker,
                    adBlockRedirectBlocker, adBlockScriptIframeBlocker,
                    adBlockClickHijackBlocker));
            runPageScript(ShieldPageScript.runtimeConfig(adBlock, adBlockPopupBlocker,
                    adBlockRedirectBlocker, adBlockScriptIframeBlocker,
                    adBlockClickHijackBlocker));
        } catch (Exception ignored) {
        }
    }

    void syncShieldRuntimeState() {
        if (webView == null) return;
        try {
            runPageScript(ShieldPageScript.runtimeConfig(adBlock, adBlockPopupBlocker,
                    adBlockRedirectBlocker, adBlockScriptIframeBlocker,
                    adBlockClickHijackBlocker));
        } catch (Exception ignored) {
        }
    }

    void onShieldSettingsChanged() {
        applyBrowserSettings();
        installShieldDocumentStartScript(webView);
        syncShieldRuntimeState();
        if (adBlock && webView != null && webView.getVisibility() == View.VISIBLE) {
            injectShieldEngineV2Fallback();
            if (!isSiteCompatibilityModeActiveForUrl(getEffectiveCurrentUrl())) {
                injectPremiumAdBlock();
            }
        }
        if (hasUserFiltersForCurrentHost()) applyUserFiltersForCurrentPage();
        saveSettings();
    }

    boolean isShieldReaderOrCompatibilityContext(String url) {
        // Search result pages intentionally navigate across domains and must never inherit the
        // strict reader boundary, even when their path is /search or their DOM is image-heavy.
        if (ShieldEngineV2.isSearchResultsPage(url)) return false;
        return isStrictSiteCompatibilityUrl(url)
                || isSiteCompatibilityModeActiveForUrl(url)
                || isReloadLoopGuardActiveForUrl(url)
                || ReaderCompatibilityPolicy.hasReaderPathHint(url)
                || ShieldEngineV2.isReaderOrContentPage(url)
                || ShieldEngineV2.isPopupIsolationContentPage(url);
    }

    boolean shouldShieldBlockMainFrame(String targetUrl, String sourceUrl,
                                               boolean hasGesture, boolean legacySuspicious) {
        if (!adBlock || !(adBlockRedirectBlocker || adBlockClickHijackBlocker)) return false;
        boolean context = isShieldReaderOrCompatibilityContext(sourceUrl);
        boolean explicitlyTrusted = isTrustedDownloadIntentUrl(targetUrl)
                || isSearchEngineResultNavigation(targetUrl, sourceUrl);
        return ShieldEngineV2.shouldBlockMainFrameNavigation(targetUrl, sourceUrl,
                hasGesture, context, explicitlyTrusted, legacySuspicious);
    }
}
