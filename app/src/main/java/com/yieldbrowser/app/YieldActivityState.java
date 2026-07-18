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

abstract class YieldActivityState extends Activity
        implements TranslateBridge.Callback, VideoBridge.Callback, AdBlockBridge.Callback {

    static final String EXTRA_OPEN_TAB_SWITCHER = "yield.open_tab_switcher";
    static final String EXTRA_CREATE_TAB = "yield.create_tab";
    static final String EXTRA_OPEN_URL = "yield.open_url";

    /**
     * Network fallback work must never run on the Android main thread. The finalizer is deliberately
     * single-threaded so two large completed files cannot saturate storage at the same time.
     */
    static final ExecutorService DOWNLOAD_IO_EXECUTOR = Executors.newCachedThreadPool(runnable ->
            createBackgroundDownloadThread(runnable, "Yield-Download-IO"));
    static final ExecutorService DOWNLOAD_FINALIZE_EXECUTOR = Executors.newSingleThreadExecutor(runnable ->
            createBackgroundDownloadThread(runnable, "Yield-Download-Finalize"));

    // ===== UI references =====

    EditText addressBar;
    EditText homeSearchInput;
    ProgressBar progressBar;
    WebView webView;
    ScrollView homeScroll;
    FrameLayout contentFrame;
    FrameLayout navigationLoadingOverlay;
    NavigationTransitionController navigationTransitionController;
    SwipeNavigationController swipeNavigationController;
    ImageButton reloadButton;
    ImageButton bookmarkButton;
    ImageButton translateButton;
    View topBarView;
    View bottomNavView;
    TextView tabsCountText;
    BrowserShellUi browserShellUi;
    PageToolsController pageToolsController;
    BrowserUtilityDialogsController browserUtilityDialogsController;
    final Handler mainHandler = new Handler(Looper.getMainLooper());
    final LifecycleCallbackGate lifecycleCallbackGate = new LifecycleCallbackGate();
    final AtomicBoolean downloadUiRefreshPosted = new AtomicBoolean(false);
    volatile long lastTrustedDownloadGestureAtMs = 0L;
    volatile String lastTrustedDownloadSourceUrl = "";

    void runOnUiThreadIfAlive(Runnable action) {
        if (action == null || !lifecycleCallbackGate.isActive()) return;
        runOnUiThread(() -> {
            if (lifecycleCallbackGate.isActive()) action.run();
        });
    }

    @Override
    public void onTrustedDownloadGesture(String sourceUrl) {
        lastTrustedDownloadSourceUrl = sourceUrl == null ? "" : sourceUrl.trim();
        lastTrustedDownloadGestureAtMs = System.currentTimeMillis();
    }

    @Override
    public void onTrustedDownloadOpen(String url, String sourceUrl) {
        runOnUiThreadIfAlive(() -> openTrustedDownloadPopupIfAllowed(url, sourceUrl));
    }

    boolean openTrustedDownloadPopupIfAllowed(String url) {
        return openTrustedDownloadPopupIfAllowed(url, lastTrustedDownloadSourceUrl);
    }

    boolean openTrustedDownloadPopupIfAllowed(String url, String callbackSourceUrl) {
        String clean = url == null ? "" : url.trim();
        if (clean.length() == 0) return false;
        String gestureSource = lastTrustedDownloadSourceUrl == null
                ? "" : lastTrustedDownloadSourceUrl.trim();
        long now = System.currentTimeMillis();
        boolean allowed = TrustedDownloadPopupPolicy.canOpen(
                ShieldEngineV2.isDownloadPage(gestureSource),
                TrustedDownloadPopupPolicy.sameSourcePage(
                        gestureSource, callbackSourceUrl),
                isTrustedDownloadIntentUrl(clean),
                lastTrustedDownloadGestureAtMs,
                now);
        if (!allowed) return false;

        lastTrustedDownloadGestureAtMs = 0L;
        lastTrustedDownloadSourceUrl = "";
        newTabInCurrentProfile();
        markTrustedMainFrameNavigation(clean);
        prepareTabForMainFrameNavigation(getCurrentTab(), clean);
        if (addressBar != null) addressBar.setText(clean);
        openAddressBarUrl();
        return true;
    }

    // ===== History Engine V2 =====
    static final String KEY_HISTORY_ENGINE_V2_INITIALIZED = "history_engine_v2_initialized";
    HistoryRepository historyRepository;
    HistoryPanelController historyPanelController;
    boolean historyV2InitializationStarted = false;

    // A bounded favicon pipeline prevents one thread/request per history row.
    final HistoryFaviconLoader historyFaviconLoader =
            new HistoryFaviconLoader(mainHandler);

    // ===== Video and fullscreen state =====
    LinearLayout videoControlsBar;
    TextView videoSpeedLabel;
    TextView videoQualityLabel;
    TextView videoModeToggleButton;
    ImageView videoPlayPauseIcon;
    View videoPlayPauseButton;
    VideoPlaybackController videoPlaybackController;
    DownloadQueueDialogController downloadQueueDialogController;
    String selectedVideoQuality = "Auto";
    boolean videoControlsManualHidden = false;
    ViewGroup videoControlsOriginalParent;
    ViewGroup.LayoutParams videoControlsOriginalLayoutParams;
    int videoControlsOriginalIndex = -1;
    boolean videoControlsInFullscreen = false;
    View fullscreenVideoView;
    WebChromeClient.CustomViewCallback fullscreenVideoCallback;
    int originalSystemUiVisibility = 0;

    // ===== Navigation gesture state =====
    // v0.9.69: situs desktop/horizontal-scroll seperti h-metrics.com tidak boleh
    // dianggap gesture Back saat user menggeser halaman ke samping.
    boolean webHorizontalGestureGuard = false;
    String webHorizontalGestureGuardHost = "";

    // ===== Download manager UI state =====
    RecyclerView activeDownloadRecyclerView;
    DownloadListAdapter activeDownloadAdapter;
    LinearLayout activeDownloadCategoryPanel;
    LinearLayout activeDownloadControlsPanel;
    TextView activeDownloadTitleView;
    TextView activeDownloadStorageView;
    TextView activeDownloadRunningTab;
    TextView activeDownloadCompletedTab;
    TextView activeDownloadEmptyView;
    Dialog activeDownloadDialog;
    DownloadManagerShell downloadManagerShell;
    DownloadManagerShell.Bindings activeDownloadBindings;
    String activeDownloadSection = "Mengunduh";
    boolean downloadUiTickerRunning;
    final Runnable downloadUiTicker = new Runnable() {
        @Override
        public void run() {
            if (!downloadUiTickerRunning || activeDownloadDialog == null
                    || !activeDownloadDialog.isShowing()) return;
            renderDownloadList();
            mainHandler.postDelayed(this, getDownloadUiTickerDelayMs());
        }
    };
    String activeDownloadCategory = "Semua";
    String activeDownloadSearchQuery = "";
    String activeDownloadSort = "Tanggal";
    boolean downloadSelectMode = false;
    final Set<Integer> selectedDownloadIds = new HashSet<>();
    final Object downloadHistoryLock = new Object();
    DownloadPanelPresenter downloadPanelPresenter;
    DownloadItemMenuController downloadItemMenuController;

    int tabCount = 1;
    int nextDownloadId = 1000;

    // ===== Translation state =====
    boolean translateEnabled = false;
    boolean hideGoogleTranslateBar = true;
    String lastTranslateOriginalUrl = "";
    boolean compatibleTranslateActive = false;
    boolean translateManuallyDisabled = true;
    int translateSessionToken = 0;
    long lastCompatibleTranslateStartedAt = 0L;
    String translateTargetLang = "id";
    String translateTargetLabel = "Indonesia";

    // ===== Browser settings state =====
    boolean speedMode = false;
    boolean safeMode = true;
    boolean nightMode = true;
    String nightModeOption = "ON";
    final Set<String> nightModeExceptions = new HashSet<>();
    int nightModeApplyToken = 0;
    boolean readerMode = false;
    boolean adBlock = true;
    boolean adBlockPopupBlocker = true;
    boolean adBlockRedirectBlocker = true;
    boolean adBlockScriptIframeBlocker = true;
    boolean adBlockClickHijackBlocker = true;
    boolean adBlockRedirectToTempTab = true;
    boolean adBlockAutoCloseAdTabs = true;
    // v0.9.99: document-start protection is installed once per WebView and kept silent.
    final Map<WebView, ScriptHandler> shieldDocumentStartHandlers =
            Collections.synchronizedMap(new WeakHashMap<>());
    // v0.9.92: "Blokir elemen" (gaya uBlock/Brave). Filter kosmetik manual per host yang
    // disembunyikan dengan display:none dan diterapkan ulang setiap halaman dibuka.
    final Map<String, LinkedHashSet<String>> userElementFilters = new LinkedHashMap<>();
    boolean userElementFiltersLoaded = false;
    boolean elementPickerActive = false;
    AlertDialog elementPickerDialog = null;
    ElementFilterDialogController elementFilterDialogController;
    boolean dataSaver = false;
    // v0.9.98: HTTPS-First tries the secure origin before plain HTTP for public sites.
    boolean httpsFirstEnabled = true;
    boolean desktopMode = false;
    int browserModeToken = 0;
    int textZoom = 100;

    // Home and top-bar customization.
    boolean shortcutDownload = true;
    boolean shortcutReloadWebsite = true;
    boolean shortcutBookmark = false;
    boolean shortcutPrivate = true;
    boolean shortcutAdBlock = true;
    boolean shortcutReader = false;
    boolean shortcutNightMode = false;
    boolean shortcutQrScan = false;
    boolean shortcutHistory = true;
    boolean shortcutFindPage = false;
    boolean shortcutShare = false;
    boolean shortcutFullscreen = false;
    // These two site tools are visible by default, but can now be hidden from
    // the main overflow menu through "Sesuaikan menu".
    boolean shortcutBlockElement = true;
    boolean shortcutSiteFilter = true;

    // Video feature settings.
    boolean videoControlsEnabled = true;
    boolean videoBufferBooster = true;
    boolean hlsSegmentPrefetch = true;
    boolean videoFloatingPlayer = true;
    boolean videoBackgroundPlay = true;
    boolean shortcutVideoControls = false;
    float videoSpeed = 1.0f;

    // Download feature settings.
    String downloadSubfolder = "Download";
    String selectedDownloadTreeUri = "";
    boolean downloadDynamic4Connections = true;
    boolean downloadAutoRetry = true;
    boolean downloadHlsEnabled = true;
    boolean downloadPlayWhileDownloadingEnabled = true;
    int downloadSpeedLimitKBps = 0;
    boolean downloadQueueEnabled = true;
    int downloadMaxActive = 2;
    boolean downloadQueuePaused = false;
    boolean topIconReload = true;
    boolean topIconBookmark = true;
    boolean topIconTranslate = true;

    // ===== App data collections =====
    final ArrayList<DownloadItem> downloadItems = new ArrayList<>();
    final ArrayList<ShortcutItemData> shortcutsData = new ArrayList<>();
    final ArrayList<HistoryItemData> historyData = new ArrayList<>();
    final ArrayList<BookmarkItemData> bookmarkData = new ArrayList<>();
    final ArrayList<TabInfo> tabs = new ArrayList<>();
    int currentTabIndex = 0;
    LinearLayout shortcutContainer;
    String searchEngine = "Google";
    String lastSafeHttpUrl = "";
    // v0.9.43: cache URL halaman aktif untuk thread shouldInterceptRequest.
    // shouldInterceptRequest berjalan di thread Chromium, jadi dilarang memanggil WebView.getUrl() di sana.
    volatile String currentPageUrlForRequest = "";
    boolean pendingHideKeyboardAfterNavigation = false;
    // v0.9.75: saat membuat tab baru kosong, showHome tidak boleh menyimpan URL WebView lama ke tab baru.
    boolean skipNextShowHomeTabSave = false;
    // v0.9.84-multitab: saat tab aktif ditutup, switch berikutnya tidak boleh menyimpan
    // address bar/WebView lama ke tab pengganti. Ini mencegah tab lain ikut berubah
    // menjadi URL tab video/YouTube yang baru ditutup.
    boolean skipNextSwitchTabStateSave = false;
    boolean historyClearLock = false;
    /** True only inside the dedicated :incognito WebView process/profile. */
    boolean dedicatedPrivateProfile = false;

    // ===== Navigation and compatibility guards =====
    // v0.9.41: guard untuk situs yang memaksa reload berulang.
    // Guard ini mencegah Yield ikut menambah DOM injection/recover navigation berulang
    // pada situs berat iklan/anti-adblock, misalnya lordborg.com.
    String reloadLoopLastKey = "";
    long reloadLoopWindowStartMs = 0L;
    int reloadLoopCount = 0;
    String reloadLoopGuardHost = "";
    String reloadLoopGuardKey = "";
    long reloadLoopGuardUntilMs = 0L;
    long reloadLoopToastLastMs = 0L;
    // Strict compatibility hosts are centralized in BrowserConstants.
    // v0.9.42: navigasi klik user/search result tidak boleh dianggap redirect iklan.
    String trustedMainFrameHost = "";
    long trustedMainFrameUntilMs = 0L;

    // v0.9.44: Universal Site Compatibility Guard.
    // Dipakai untuk situs yang anti-adblock/auto-reload/blank supaya Yield berhenti
    // melakukan restore, intercept agresif, dan DOM injection pada domain tersebut.
    String siteCompatibilityHost = "";
    long siteCompatibilityUntilMs = 0L;
    long siteCompatibilityToastLastMs = 0L;
    // v0.9.78: compatibility mode tidak boleh cuma 1 host global.
    // Jika dua tab berbeda sama-sama butuh compatibility (misal Lordborg + Invest Tracing),
    // keduanya harus tetap aktif dan tidak saling mematikan.
    final Map<String, Long> siteCompatibilityHosts = new LinkedHashMap<>();
    // v0.9.65: universal blank-page compatibility recovery.
    // Jika halaman menjadi blank hanya saat AdBlock ON, host akan otomatis
    // direload sekali dalam compatibility mode tanpa perlu didaftarkan manual.
    String autoCompatibilityRecoveryHost = "";
    String autoCompatibilityRecoveryKey = "";
    long autoCompatibilityRecoveryUntilMs = 0L;

    String translateLanguageLabel(String code) {
        if ("id".equals(code)) return "Indonesia";
        if ("en".equals(code)) return "Inggris";
        if ("ja".equals(code)) return "Jepang";
        if ("ko".equals(code)) return "Korea";
        if ("zh-CN".equals(code)) return "Mandarin";
        if ("ar".equals(code)) return "Arab";
        if ("de".equals(code)) return "Jerman";
        if ("fr".equals(code)) return "Prancis";
        if ("es".equals(code)) return "Spanyol";
        if ("ru".equals(code)) return "Rusia";
        if ("ms".equals(code)) return "Melayu";
        return "Indonesia";
    }

    void forceMobileModeAfterUpdateIfNeeded(SharedPreferences p) {
        MobileModeMigration.apply(p, () -> desktopMode = false);
    }

    // v0.9.92: blocklist host dipindah ke konstanta static agar tidak dialokasikan ulang
    // pada setiap shouldInterceptRequest (dipanggil ratusan kali per halaman). Semantik
    // pencocokan tetap sama (substring contains pada URL) sehingga perilaku tidak berubah,
    // tetapi tekanan GC dan jank saat memuat/menggulir halaman berkurang signifikan.
    static final String[] AD_URL_HOST_PATTERNS = new String[]{
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "pagead2.googlesyndication.com",
            "adservice.google.",
            "googleads.g.doubleclick.net",
            "securepubads.g.doubleclick.net",
            "tpc.googlesyndication.com",
            "adsystem.com",
            "amazon-adsystem.com",
            "adnxs.com",
            "rubiconproject.com",
            "pubmatic.com",
            "openx.net",
            "criteo.com",
            "taboola.com",
            "outbrain.com",
            "adsrvr.org",
            "yieldmo.com",
            "mgid.com",
            "revcontent.com",
            "moatads.com",
            "scorecardresearch.com",
            "quantserve.com",
            "hotjar.com",
            "googletagmanager.com/gtm.js",
            "google-analytics.com",
            "analytics.google.com",
            "facebook.com/tr",
            "connect.facebook.net",
            "bat.bing.com",
            "mc.yandex.ru",
            "static.ads-twitter.com",
            "analytics.tiktok.com",
            "unityads.unity3d.com",
            "applovin.com",
            "adcolony.com",
            "onclickads.net",
            "clickadu.com",
            "popads.net",
            "popcash.net",
            "propellerads.com",
            "adsterra.com",
            "hilltopads.net",
            "exoclick.com",
            "trafficjunky.net",
            "juicyads.com",
            "admaven.com",
            "realsrv.com"
    };

    // 1x1 GIF transparan (43 byte). Dikembalikan untuk request gambar iklan yang diblokir agar
    // tidak muncul ikon "broken image" seperti pada browser profesional (Brave/UC).
    static final byte[] TRANSPARENT_GIF_BYTES = new byte[]{
            (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61,
            (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x80, (byte) 0x00, (byte) 0x00,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x21, (byte) 0xF9, (byte) 0x04, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x2C, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
            (byte) 0x02, (byte) 0x02, (byte) 0x44, (byte) 0x01, (byte) 0x00, (byte) 0x3B
    };

    static Thread createBackgroundDownloadThread(Runnable runnable, String name) {
        Thread thread = new Thread(() -> {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            } catch (Exception ignored) {
            }
            runnable.run();
        }, name);
        thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
        return thread;
    }

    // Cross-layer virtual contract generated from the original MainActivity methods.
    protected abstract boolean useDedicatedPrivateProfile();
    abstract TabInfo createProfileTab(String normalTitle, String privateTitle, String url,
                                     boolean requestedPrivate, boolean adTab);
    abstract TabInfo createProfileTab(String normalTitle, String privateTitle, String url,
                                     boolean requestedPrivate);
    abstract void discardPrivateTabsForExplicitExit();
    abstract void handleOpenDownloadsIntent(Intent intent);
    abstract void initializeBrowserShellUi();
    abstract void renderShortcuts();
    abstract String normalizeShortcutUrl(String text);
    abstract String guessLabelFromUrl(String url);
    abstract void loadFavicon(String url, ImageView target, TextView fallback);
    abstract TabInfo findTabByWebView(WebView candidate);
    abstract boolean isLiveTabWebView(TabInfo tab, WebView candidate, long generation);
    abstract boolean isPrivateWebView(WebView candidate);
    abstract void applyProfileCookiePolicy(WebView candidate);
    abstract void postForActiveTab(TabInfo tab, WebView candidate, long delayMs, Runnable action);
    abstract void attachWebViewToContentFrame(WebView candidate);
    abstract WebView ensureTabWebView(TabInfo tab, int visibility);
    abstract void hideInactiveTabWebViews(WebView active);
    abstract void invalidateTabScopedAsyncWork();
    abstract void activateNavigationContextForTab(TabInfo tab);
    abstract void activateTabWebView(TabInfo tab, boolean showWebPage);
    abstract void destroyTabWebView(TabInfo tab);
    abstract boolean hasLivePage(WebView candidate);
    abstract void ensureDefaultTab();
    abstract TabInfo getCurrentTab();
    abstract boolean isCurrentPrivateTab();
    abstract boolean isCurrentTabInfo(TabInfo tab);
    abstract String getTabReferenceUrl(TabInfo tab);
    abstract void prepareTabForMainFrameNavigation(TabInfo tab, String url);
    abstract boolean isTrustedMainFrameNavigationForTab(TabInfo tab, String url);
    abstract boolean isSameIsolatedSite(String host, String baseHost);
    abstract boolean isTabIsolationAllowed(TabInfo tab, String url);
    abstract void syncTabIsolationAfterCommit(TabInfo tab, String url);
    abstract String cleanTabSessionUrl(String url);
    abstract boolean isTemporaryDirectAdUrl(String url);
    abstract boolean isRestorableTabSessionUrl(String url);
    abstract boolean canCommitUrlToTab(TabInfo tab, String url);
    abstract void commitTabUrlIfSafe(TabInfo tab, String candidateUrl, String candidateTitle);
    abstract String getSafeUrlForSession(TabInfo tab);
    abstract boolean isPersistableTab(TabInfo tab);
    abstract TabInfo findPersistedSelectionCandidate();
    abstract void restoreTabsSession();
    abstract void saveTabsSession();
    abstract void restoreActiveTabAfterLaunch();
    abstract void saveCurrentTabState();
    abstract void updateTabsCountUi();
    abstract void newTabInCurrentProfile();
    abstract void launchDedicatedPrivateProfile();
    abstract void launchDedicatedPrivateProfile(boolean openTabSwitcher);
    abstract void launchDedicatedPrivateProfile(boolean openTabSwitcher, String openUrl);
    abstract void launchNormalProfile(boolean openTabSwitcher, boolean createTab);
    abstract void handleProfileSpaceIntent(Intent intent);
    abstract void createOrReuseBlankProfileTab();
    abstract void openUrlInCurrentProfileTab(String url);
    abstract void openUrlInPrivateSpace(String url);
    abstract void openNormalBrowserSpace();
    abstract void openPrivateBrowserSpace();
    abstract void newPrivateTab();
    abstract void switchToTab(int index);
    abstract void switchToTab(TabInfo tab);
    abstract void closeTab(TabInfo removed);
    abstract void showTabsPanel();
    abstract void showTabsPanelForSpace(boolean privateSpace);
    abstract void showQuickMenu();
    abstract void showAboutYieldDialog();
    abstract void showSettingsPanel();
    abstract void showAdBlockSettingsDialog();
    abstract void openQrScanner();
    abstract void showQrScannerDialog();
    abstract void showSearchEngineDialog();
    abstract void showCustomizeMenuPanel();
    abstract void showDownloadSettingsPanel();
    abstract String getDownloadQueueSummary();
    abstract void showDownloadQueueSettingsDialog();
    abstract void showVideoControlsIfAllowed();
    abstract LinearLayout createVideoControlsBar();
    abstract View videoTextButton(String text, String label, View.OnClickListener listener);
    abstract View videoButton(int iconRes, String label, View.OnClickListener listener);
    abstract void updateVideoPlayPauseButtonState(boolean playing);
    abstract void refreshVideoPlayPauseButtonState();
    abstract void applyVideoControlsFullscreenLayout(boolean fullscreen);
    abstract void nativeTapWebViewAtRatio(double xRatio, double yRatio);
    abstract void updateVideoControlsVisibility();
    abstract void seekVideoBySeconds(int seconds);
    abstract void checkAndShowVideoControls();
    abstract void toggleVideoFullLandscapeButton();
    abstract void updateVideoModeToggleButton();
    abstract boolean isVideoFullscreenActive();
    abstract void exitVideoFullscreenToPortraitMode();
    abstract void enterVideoFullscreen();
    abstract void openAppVideoFullscreenFallback();
    abstract void restoreAfterVideoFullscreen();
    abstract void forcePortraitAfterVideoFullscreen();
    abstract void moveVideoControlsToFullscreenOverlay();
    abstract void restoreVideoControlsFromFullscreenOverlay();
    abstract void showVideoOptimizationDialog();
    abstract View videoOptSwitchRow(String title,
                                   String desc,
                                   boolean enabled,
                                   View.OnClickListener listener);
    abstract void ensureVideoPlaybackController();
    abstract void detectVideoQualities();
    abstract void showVideoQualityDialog();
    abstract void setVideoQuality(String quality);
    abstract void showVideoSpeedDialog();
    abstract void setVideoSpeed(float speed);
    abstract void injectVideoPlaybackWatcher();
    abstract void injectVideoOptimizationIfNeeded();
    abstract void controlVideo(String action);
    abstract void showHistoryPanel();
    abstract void refreshHistoryPanelIfShowing();
    abstract void showBookmarkHomePanel();
    abstract void switchDialogSmooth(Dialog currentDialog, Runnable openNext);
    abstract void ensurePageToolsController();
    abstract void showFindInPageDialog();
    abstract void shareCurrentPage();
    abstract void copyCurrentLink();
    abstract void showPageInfoDialog();
    abstract void toggleFullscreenMode();
    abstract void exitFullscreenMode();
    abstract void saveCurrentPageOffline();
    abstract boolean shouldRecordHistoryUrl(String url);
    abstract void initializeHistoryEngineV2();
    abstract void clearLegacyHistoryStorageNow();
    abstract void recordWebViewBackForwardHistory();
    abstract void recordCurrentPageToHistory();
    abstract void addBrowserHistory(String title, String url);
    abstract void loadBrowserHistory();
    abstract void saveBrowserHistory();
    abstract void clearBrowserHistoryManually(Runnable afterClear);
    abstract void ensureBrowserUtilityDialogsController();
    abstract void showTextZoomDialog(Dialog parentDialog);
    abstract String getDownloadLocationText();
    abstract void showDownloadFolderDialog(Dialog parentDialog);
    abstract void chooseExternalDownloadFolder();
    abstract TextView sectionTitle(String text);
    abstract View menuDivider();
    abstract View customizeToggleRow(int iconRes, String label, boolean enabled, View.OnClickListener listener);
    abstract View settingRow(int iconRes, String title, String desc, boolean enabled, View.OnClickListener listener);
    abstract View actionRow(int iconRes, String title, String desc, View.OnClickListener listener);
    abstract LinearLayout baseSettingsRow(int iconRes, String title, String desc, View.OnClickListener listener);
    abstract View menuRow(int iconRes, String label, View.OnClickListener listener);
    abstract void showDownloadManager();
    abstract void clearDownloadManagerBindings();
    abstract void renderDownloadSectionTabs();
    abstract void renderDownloadCategoryChips();
    abstract void showDownloadSearchDialog();
    abstract void showDownloadSortDialog();
    abstract void ensureDownloadPanelPresenter();
    abstract void invalidateDownloadControls();
    abstract void renderDownloadList();
    abstract TextView downloadToolButton(String text);
    abstract String getDownloadCategory(DownloadItem item);
    abstract String normalizeDetectedCategory(String raw);
    abstract String inferDownloadCategoryFromData(String fileName, String url, String mimeType);
    abstract DownloadItem findDownloadItemById(int id);
    abstract long getDownloadSize(DownloadItem item);
    abstract String getDownloadHost(DownloadItem item);
    abstract String safeText(String text);
    abstract void resetDownloadSpeedState(DownloadItem item);
    abstract void updateDownloadSpeed(DownloadItem item, long currentBytes);
    abstract String readableSpeed(double bytesPerSecond);
    abstract String readableFileSize(long size);
    abstract String getStorageUsageText();
    abstract void toggleDownloadSelection(DownloadItem item);
    abstract ArrayList<DownloadItem> getSelectedDownloads();
    abstract void shareSelectedDownloads();
    abstract int countCompletedDownloadHistory();
    abstract void confirmClearCompletedDownloadHistory();
    abstract void clearCompletedDownloadHistory();
    abstract void deleteSelectedDownloads();
    abstract boolean isActiveDownloadStatus(String status);
    abstract boolean hasFinalizingDownload();
    abstract long getDownloadUiTickerDelayMs();
    abstract int countActiveDownloads();
    abstract boolean isCurrentDownloadRun(DownloadItem item, int generation);
    abstract DownloadItem findForegroundActiveDownload();
    abstract int getVisibleDownloadProgressPercent(DownloadItem item);
    abstract String getForegroundDownloadText(DownloadItem item);
    abstract void updateDownloadKeepAliveState();
    abstract int countQueuedDownloads();
    abstract DownloadItem findNextQueuedDownload();
    abstract void enqueueOrStartDownload(DownloadItem item, File out);
    abstract void startQueuedDownloadNow(DownloadItem item);
    abstract void pumpDownloadQueue();
    abstract void pauseAllDownloads();
    abstract void resumeAllDownloads();
    abstract void prioritizeQueuedDownload(DownloadItem item, boolean startIfPossible);
    abstract void moveQueuedDownload(DownloadItem item, int direction);
    abstract void handleDownloadPrimaryAction(DownloadItem item);
    abstract String getConnectionLabel(DownloadItem item);
    abstract void pauseDownloadItem(DownloadItem item);
    abstract void resumeDownloadItem(DownloadItem item);
    abstract void reloadDownloadItem(DownloadItem item);
    abstract void showDownloadItemMenu(View anchor, DownloadItem item);
    abstract boolean canPlayDownloadInsideYield(DownloadItem item);
    abstract void playDownloadInsideYield(DownloadItem item);
    abstract void shareDownloadedFile(DownloadItem item);
    abstract void renameDownloadedFile(DownloadItem item);
    abstract void removeDownloadItem(DownloadItem item, boolean deleteFile);
    abstract Uri getBestDownloadUri(DownloadItem item);
    abstract void openDownloadedFile(DownloadItem item);
    abstract void refreshDownloadPanel();
    abstract String getCurrentPageForReferer();
    abstract void beginDownloadFromWeb(String url, String contentDisposition, String mimeType, String userAgent);
    abstract void beginDownload(String fileUrl, String guessedFileName, String userAgent, String referer);
    abstract void showDownloadStartedBanner(DownloadItem item);
    abstract void ensureDownloadNotificationPermission();
    abstract File getDownloadDirectory();
    abstract File uniqueFile(File file);
    abstract String getOriginFromUrl(String value);
    abstract String getRootUrl(String value);
    abstract String getHostLower(String value);
    abstract boolean isGoogleDriveHost(String url);
    abstract boolean isStableDownloadHost(String url);
    abstract boolean isTurboFriendlyHost(String url);
    abstract String normalizeGoogleDriveDownloadUrl(String value);
    abstract boolean looksLikeArchiveOrApp(String fileName, String url, String contentType);
    abstract int chooseDownloadBufferSize(long totalBytes);
    abstract int chooseSmartDownloadConnections(DownloadItem item, long totalBytes, String contentType);
    abstract String getTurboLabel(DownloadItem item, int connections);
    abstract void registerDownloadConnection(DownloadItem item, HttpURLConnection connection);
    abstract void unregisterDownloadConnection(DownloadItem item, HttpURLConnection connection);
    abstract void registerDownloadStream(DownloadItem item, InputStream stream);
    abstract void unregisterDownloadStream(DownloadItem item, InputStream stream);
    abstract void stopActiveDownloadTransports(DownloadItem item);
    abstract void resetTurboSampling(DownloadItem item);
    abstract void maybePersistDownloadProgress(DownloadItem item);
    abstract void updateTurboPrediction(DownloadItem item, long currentBytes);
    abstract int getV3FallbackConnections(DownloadItem item);
    abstract boolean shouldFallbackTurboToStable(DownloadItem item);
    abstract String buildAntiHotlinkCookieHeader(String fileUrl, DownloadItem item);
    abstract HttpURLConnection openDownloadConnection(String url, DownloadItem item, String range) throws Exception;
    abstract boolean isRedirectCode(int code);
    abstract void validateDownloadResponse(HttpURLConnection connection) throws Exception;
    abstract void applyDownloadHeaders(HttpURLConnection connection, String fileUrl,
                                      DownloadItem item, boolean applyIfRange);
    abstract void captureRemoteIdentity(DownloadItem item, HttpURLConnection connection) throws Exception;
    abstract String safeHeader(String value);
    abstract boolean isProtocolOrIdentityFailure(String reason);
    abstract boolean isPermanentDownloadError(String reason);
    abstract void startTwoConnectionDownload(DownloadItem item, File out);
    abstract void startSingleConnectionDownloadAsync(DownloadItem item, File out);
    abstract boolean looksLikeHlsDownload(String url, String fileName);
    abstract boolean looksLikeVideoDownload(String url, String fileName, String contentType);
    abstract String autoRenameDownloadFile(String fileName, String url, String contentType);
    abstract void setDynamicPartState(DownloadItem item, int part, long start, long end, long done);
    abstract long getDynamicPartStart(DownloadItem item, int part);
    abstract long getDynamicPartEnd(DownloadItem item, int part);
    abstract long getDynamicPartDone(DownloadItem item, int part);
    abstract void addDynamicPartDone(DownloadItem item, int part, long delta);
    abstract void clearDynamicPartState(DownloadItem item);
    abstract boolean hasDynamicResumeState(DownloadItem item, int connections, long total, File file);
    abstract void initializeDynamicParts(DownloadItem item, int connections, long total);
    abstract boolean verifyMultipartComplete(DownloadItem item, int connections, long total, File file);
    abstract void resetForCleanRestart(DownloadItem item, File out);
    abstract void startDynamicMultiConnectionDownload(DownloadItem item, File out);
    abstract void downloadRangeDynamic(DownloadItem item, File out, long start, long end,
                                      AtomicLong done, long total, AtomicBoolean ok,
                                      String[] workerError, int partIndex, int connections,
                                      int generation);
    abstract void setWorkerError(String[] holder, String reason);
    abstract String getWorkerError(String[] holder, String fallback);
    abstract void startLegacyTwoConnectionDownload(DownloadItem item, File out);
    abstract void downloadRange(DownloadItem item, File out, long start, long end,
                               AtomicLong done, long total, AtomicBoolean ok,
                               String[] workerError, int partIndex, int generation);
    abstract void startHlsDownload(DownloadItem item, File out);
    abstract HlsPlaylistParser.Playlist resolveHlsPlaylist(String playlistUrl, DownloadItem item,
                                                          int generation, int depth) throws Exception;
    abstract String readUrlText(String url, DownloadItem item, int generation) throws Exception;
    abstract void appendHlsResource(DownloadItem item, File output,
                                   HlsPlaylistParser.Resource resource, int generation,
                                   Map<String, byte[]> keyCache) throws Exception;
    abstract byte[] readHlsResourceBytes(DownloadItem item, InputStream input,
                                        long expected, int generation) throws Exception;
    abstract byte[] getHlsKeyBytes(DownloadItem item, String keyUrl, int generation,
                                  Map<String, byte[]> cache) throws Exception;
    abstract String buildHlsFingerprint(HlsPlaylistParser.Playlist playlist) throws Exception;
    abstract void updateHlsFingerprint(MessageDigest digest, HlsPlaylistParser.Resource resource);
    abstract String bytesToHex(byte[] value);
    abstract String normalizeUrlForFingerprint(String value);
    abstract void applySpeedLimit(DownloadItem item, int bytesRead) throws InterruptedException;
    abstract void downloadSingle(DownloadItem item, File out);
    abstract void completeDownload(DownloadItem item);
    abstract void performCompleteDownload(DownloadItem item);
    abstract boolean exportCompletedDownload(DownloadItem item, File source);
    abstract Uri copyFileToSelectedTree(File source, String fileName, DownloadItem item) throws Exception;
    abstract Uri copyFileToDefaultDownloads(File source, String fileName, DownloadItem item) throws Exception;
    abstract void copyFileToUri(File source, Uri uri, DownloadItem item) throws Exception;
    abstract void copyFileToFile(File source, File target, DownloadItem item) throws Exception;
    abstract void copyWithFinalizeProgress(InputStream input, OutputStream output,
                                          long totalBytes, DownloadItem item) throws Exception;
    abstract String getMimeTypeForName(String fileName);
    abstract void failDownload(DownloadItem item, String reason);
    abstract void createNotificationChannel();
    abstract void cancelDownloadNotification(DownloadItem item);
    abstract void showDownloadNotification(DownloadItem item, String text, boolean ongoing);
    abstract void saveDownloadHistory();
    abstract void loadDownloadHistory();
    abstract void toggleBookmark();
    abstract void showBookmarkList();
    abstract void toggleTranslate();
    abstract void showTranslateLanguageDialog();
    abstract void showTranslateOptionsDialog();
    abstract String getOriginalForTranslate(String maybeUrl);
    abstract boolean isGoogleTranslatedUrl(String url);
    abstract void detectTranslateProxyBlocked(String url);
    abstract void startCompatibleTranslateSession(String originalUrl);
    abstract void startGoogleTranslateSession(String originalUrl);
    abstract void disableTranslateAndRestore(String currentUrl);
    abstract boolean isCompatibleTranslateAllowed(int token);
    abstract void runJsOnCurrentPage(String script);
    abstract void translatePageCompatible();
    abstract void translatePageCompatible(int token);
    abstract void continueCompatibleTranslation();
    abstract void clearCompatibleTranslationMarks();
    abstract void applyCompatibleTranslation(int index, String translated);
    abstract String translateTextViaGoogle(String text);
    abstract void loadTranslatedPage(String originalUrl);
    abstract void hideGoogleTranslateToolbar();
    abstract void unblockTranslatedPageClicks();
    abstract void showGoogleTranslateBar();
    abstract void translatePageTextOnly();
    abstract void reloadCurrentWebsite();
    abstract boolean captureAdRedirectToTempTab(String url);
    abstract boolean captureDirectImageToTempTab(String url);
    abstract boolean captureBlockedNavigationToTempTab(String url, boolean allowWhenAdBlockOff);
    abstract void closeAdTabSilently(TabInfo adTab, int fallbackIndex);
    abstract void scheduleCloseDetectedAdTabs();
    abstract void closeDetectedAdTabs();
    abstract WebView createBrowserWebView(TabInfo owner, int visibility);
    abstract void recreateBrowserWebViewForMode(String targetUrl, boolean showWebPage);
    abstract void hardReloadUrlWithCurrentBrowserMode(String targetUrl, boolean showWebPage);
    abstract void removeShieldDocumentStartScript(WebView target);
    abstract void installShieldDocumentStartScript(WebView target);
    abstract void injectShieldEngineV2Fallback();
    abstract void syncShieldRuntimeState();
    abstract void onShieldSettingsChanged();
    abstract boolean isShieldReaderOrCompatibilityContext(String url);
    abstract boolean shouldShieldBlockMainFrame(String targetUrl, String sourceUrl,
                                               boolean hasGesture, boolean legacySuspicious);
    abstract boolean handleLongPressedLink(WebView sourceView);
    abstract void showLongPressedLinkMenu(WebView sourceView, String requestedHref,
                                         String fallbackHref, String pageUrl);
    abstract void openLongPressedLinkInNewTab(String url);
    abstract void configureWebView();
    abstract boolean isSystemDarkMode();
    abstract String getCurrentHostForSettings();
    abstract boolean isNightModeActiveForCurrentSite();
    abstract boolean isNightModeActiveForUrl(String url);
    abstract String getHostForNightMode(String url);
    abstract boolean isAlgorithmicDarkeningSupported();
    abstract void applyAlgorithmicDarkening(WebSettings settings, boolean active);
    abstract void syncNightModeWebSettingsForUrl(String url);
    abstract void scheduleNightModeSyncForPage(String url);
    abstract void setNightModeOptionAndApply(String option, boolean reloadPage);
    abstract String nightModeLabel();
    abstract void applyNightModeToWebPage();
    abstract void disableNightModeCompletely(boolean reloadPage);
    abstract void showNightModeSettingsDialog();
    abstract View nightChoiceRow(String label, boolean checked, View.OnClickListener listener);
    abstract TextView dialogTextButton(String text);
    abstract void showNightModeExceptionDialog();
    abstract void loadBrowserUrl(String url);
    abstract void scheduleMobileViewportReset();
    abstract void toggleDesktopModeSafely();
    abstract String getSafeReloadUrlForModeChange();
    abstract boolean isSafeUrlForModeReload(String url, boolean explicitCurrentPage);
    abstract String normalizeUrlForCurrentBrowserMode(String url);
    abstract void applyBrowserSettings();
    abstract void applyMobileProfile(WebSettings settings);
    abstract void applyDesktopProfile(WebSettings settings);
    abstract String getMobileUserAgent();
    abstract String getDesktopUserAgent();
    abstract String hostOfUrl(String url);
    abstract String navigationLoopKey(String url);
    abstract void enableSiteCompatibilityModeForUrl(String url);
    abstract boolean isSiteCompatibilityModeActiveForUrl(String url);
    abstract boolean isStrictSiteCompatibilityUrl(String url);
    abstract boolean isKnownStrictCompatibilityHost(String host);
    abstract boolean isCompatibilityNavigationFlow(String targetUrl, String sourceUrl);
    abstract void repairUniversalReaderPage(String url);
    abstract void scheduleUniversalReaderCompatibilityRepair(String url);
    abstract void applyPlainCompatibilitySettings();
    abstract void loadCompatibilityUrlWithCurrentMode(String cleanUrl);
    abstract void scheduleCompatibilityLoadFallback(String url);
    abstract String decodeEvaluateJavascriptString(String value);
    abstract boolean isUniversalCompatibilityCandidateUrl(String url);
    abstract boolean shouldRetryCompatibilityRecovery(String url);
    abstract boolean isLikelyBlankCompatibilityReport(String report);
    abstract void scheduleUniversalBlankCompatibilityRecovery(String url);
    abstract boolean isReloadLoopGuardActiveForUrl(String url);
    abstract boolean registerNavigationLoopGuard(String url);
    abstract void runPageScript(String js);
    abstract void applyViewportForCurrentMode();
    abstract void applyMobileViewportIfNeeded();
    abstract void injectMobileViewportReset();
    abstract void applyDesktopViewportIfNeeded();
    abstract void injectDesktopViewportLock();
    abstract void markTrustedMainFrameNavigation(String url);
    abstract boolean isTrustedMainFrameNavigation(String url);
    abstract boolean isSearchEngineResultNavigation(String targetUrl, String currentUrl);
    abstract boolean isCompatibilityAdNavigation(String targetUrl, String currentUrl, boolean hasGesture);
    abstract boolean isCompatibilityContentAssetUrl(String url);
    abstract boolean isCompatibilityThirdPartyAdResource(String resourceUrl, String pageUrl);
    abstract boolean isContextAllowedSuspiciousMainFrameNavigation(String targetUrl, String currentUrl, boolean hasGesture);
    abstract boolean isNormalUserMainFrameNavigation(String targetUrl, String currentUrl, boolean hasGesture);
    abstract boolean isFirstPartyResourceForCurrentPage(String resourceUrl, String pageUrl);
    abstract boolean isTrustedDownloadIntentUrl(String url);
    abstract boolean isTrustedDownloadHostForAllow(String host);
    abstract boolean hasTrustedDownloadMarker(String u);
    abstract boolean hasDirectFileDownloadExtension(String u);
    abstract boolean hasHardAdClickToken(String u);
    abstract boolean isSuspiciousAdHostForDownloadAllow(String host);
    abstract boolean isUnsafeUrl(String url);
    abstract String normalizeHostForAdBlock(String url);
    abstract boolean sameOrSubDomain(String host, String baseHost);
    abstract boolean isKnownPopupHost(String url);
    abstract boolean isHttpOrHttpsUrl(String url);
    abstract void clearAllHttpsFirstRuntimeState();
    abstract void clearHttpsFirstPendingState(TabInfo tab, boolean clearFallbackGuard);
    abstract boolean isHttpUrl(String url);
    abstract boolean isHttpsUrl(String url);
    abstract boolean isPrivateIpv4Host(String host);
    abstract boolean isLocalOrPrivateHost(String host);
    abstract boolean isHttpsFirstExemptUrl(String url);
    abstract String buildHttpsUpgradeUrl(String httpUrl);
    abstract boolean isHttpsFallbackGuardActive(TabInfo tab, String httpUrl);
    abstract String prepareHttpsFirstNavigation(String rawUrl, TabInfo tab);
    abstract boolean startHttpsFirstOverrideIfNeeded(WebView view, String targetUrl, TabInfo tab);
    abstract boolean isHttpsFallbackEligibleError(int errorCode);
    abstract boolean handleHttpsFirstMainFrameFailure(WebView view, String failedUrl, int errorCode);
    abstract boolean equivalentUrlIgnoringSchemeAndFragment(String a, String b);
    abstract void upgradeBookmarksAfterHttpsSuccess(String originalHttpUrl, String finalHttpsUrl);
    abstract void handleHttpsFirstNavigationSuccess(TabInfo tab, String finalUrl);
    abstract boolean isExternalSchemeUrl(String url);
    abstract boolean isLikelyAdClickUrl(String url);
    abstract void restoreAfterBlockedNavigation(WebView view, String blockedUrl);
    abstract boolean isSuspiciousPopupNavigation(String targetUrl, String currentUrl);
    abstract boolean isYoutubeCoreUrl(String url);
    abstract boolean isMediaResourceUrl(String url);
    abstract boolean isImageResourceUrl(String url);
    abstract boolean isDirectImageMainFrameNavigation(String targetUrl, String currentUrl);
    abstract WebResourceResponse buildBlockedResponse(String url);
    abstract boolean isAdUrl(String url);
    abstract boolean isPopUnderOrAdAsset(String u);
    abstract String buildAdBlockCosmeticCss();
    abstract String buildCosmeticCssInjectorJs();
    abstract void injectAdBlockCssEarly();
    abstract void loadUserElementFilters();
    abstract void persistUserElementFiltersForHost(String host);
    abstract LinkedHashSet<String> userFiltersForHost(String host);
    abstract boolean hasUserFiltersForCurrentHost();
    abstract boolean addUserElementFilter(String host, String selector);
    abstract void removeUserElementFilter(String host, String selector);
    abstract String buildUserFilterCss(String host);
    abstract void applyUserFiltersForCurrentPage();
    abstract String buildElementPickerJs();
    abstract void startElementPicker();
    abstract void onPickerElementSelected(String selector, String preview,
                                         int matchCount, String selectedTag);
    abstract void continueElementPickerAfterSelection();
    abstract void finishElementPicker(boolean committed);
    abstract void showUserFiltersManager();
    abstract void injectPremiumAdBlock();
    abstract void injectCompatibilityAdShield();
    abstract boolean isYouTubePageUrl(String url);
    abstract boolean isYouTubePlaybackUrl(String url);
    abstract void injectYouTubeSafeAdBlockV6();
    abstract void stopYouTubeAutoAssistantNow();
    abstract void injectReaderMode();
    abstract void hideKeyboardAndClearFocus(View focusView);
    abstract void blurWebInputsAndHideKeyboard();
    abstract void openHomeSearchUrl();
    abstract void openAddressBarUrl();
    abstract String buildSearchUrl(String query);
    abstract void loadShortcuts();
    abstract String normalizeStoredRows(String saved);
    abstract void saveShortcuts();
    abstract boolean isLikelyDesktopOnlyHost(String host);
    abstract void scheduleHorizontalGestureGuardCheck(String url);
    abstract void initializeSwipeNavigation(View root);
    abstract boolean shouldProtectWebHorizontalSwipeGesture();
    abstract void restoreHiddenWebPage();
    abstract void startSmoothSearchTransition();
    abstract void finishSmoothSearchTransition();
    abstract void cancelSmoothSearchTransition();
    abstract boolean isSmoothSearchTransitionActive();
    abstract void navigateCurrentTabHome();
    abstract void showHome();
    abstract void updateTopActionStates();
    abstract String getEffectiveCurrentUrl();
    abstract String normalizeInputToUrl(String text);
    abstract String extractOriginalUrl(String url);
    abstract Set<String> getBookmarks();
    abstract BookmarkItemData findBookmarkByUrl(String url);
    abstract List<String> getBookmarkFolders();
    abstract int countBookmarksInFolder(String folder);
    abstract void loadBookmarkData();
    abstract void saveBookmarkData();
    abstract void loadSettings();
    abstract void saveSettings();
    abstract ImageButton smallTopIcon(int iconRes, String desc, View.OnClickListener listener);
    abstract View space(int height);
    abstract GradientDrawable roundRect(int fillColor, int radius, int strokeWidth, int strokeColor);
    abstract int dp(int value);
}
