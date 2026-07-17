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


public class MainActivity extends Activity
        implements TranslateBridge.Callback, VideoBridge.Callback, AdBlockBridge.Callback {

    private static final String EXTRA_OPEN_TAB_SWITCHER = "yield.open_tab_switcher";
    private static final String EXTRA_CREATE_TAB = "yield.create_tab";
    private static final String EXTRA_OPEN_URL = "yield.open_url";

    /**
     * Network fallback work must never run on the Android main thread. The finalizer is deliberately
     * single-threaded so two large completed files cannot saturate storage at the same time.
     */
    private static final ExecutorService DOWNLOAD_IO_EXECUTOR = Executors.newCachedThreadPool(runnable ->
            createBackgroundDownloadThread(runnable, "Yield-Download-IO"));
    private static final ExecutorService DOWNLOAD_FINALIZE_EXECUTOR = Executors.newSingleThreadExecutor(runnable ->
            createBackgroundDownloadThread(runnable, "Yield-Download-Finalize"));

    private static Thread createBackgroundDownloadThread(Runnable runnable, String name) {
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

    // ===== UI references =====

    private EditText addressBar;
    private EditText homeSearchInput;
    private ProgressBar progressBar;
    private WebView webView;
    private ScrollView homeScroll;
    private FrameLayout contentFrame;
    private FrameLayout navigationLoadingOverlay;
    private boolean smoothSearchTransitionActive = false;
    private ImageButton reloadButton;
    private ImageButton bookmarkButton;
    private ImageButton translateButton;
    private View topBarView;
    private View bottomNavView;
    private TextView tabsCountText;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean downloadUiRefreshPosted = new AtomicBoolean(false);

    // ===== History Engine V2 =====
    private static final int HISTORY_PAGE_SIZE = 50;
    private static final String KEY_HISTORY_ENGINE_V2_INITIALIZED = "history_engine_v2_initialized";
    private HistoryRepository historyRepository;
    private Dialog activeHistoryDialog;
    private HistoryListAdapter activeHistoryAdapter;
    private RecyclerView activeHistoryRecyclerView;
    private TextView activeHistoryEmptyView;
    private ProgressBar activeHistoryProgress;
    private String activeHistoryQuery = "";
    private long activeHistoryBeforeTime = Long.MAX_VALUE;
    private long activeHistoryBeforeId = Long.MAX_VALUE;
    private boolean activeHistoryLoading = false;
    private boolean activeHistoryEndReached = false;
    private int activeHistoryGeneration = 0;
    private Runnable pendingHistorySearch;
    private boolean historyV2InitializationStarted = false;

    // A bounded favicon pipeline prevents one thread/request per history row.
    private final HistoryFaviconLoader historyFaviconLoader =
            new HistoryFaviconLoader(mainHandler);

    // ===== Video and fullscreen state =====
    private LinearLayout videoControlsBar;
    private TextView videoSpeedLabel;
    private TextView videoQualityLabel;
    private TextView videoModeToggleButton;
    private ImageView videoPlayPauseIcon;
    private View videoPlayPauseButton;
    String selectedVideoQuality = "Auto";
    private boolean videoControlsManualHidden = false;
    private ViewGroup videoControlsOriginalParent;
    private ViewGroup.LayoutParams videoControlsOriginalLayoutParams;
    private int videoControlsOriginalIndex = -1;
    private boolean videoControlsInFullscreen = false;
    private View fullscreenVideoView;
    private WebChromeClient.CustomViewCallback fullscreenVideoCallback;
    private int originalSystemUiVisibility = 0;

    // ===== Navigation gesture state =====
    private float swipeStartX = 0f;
    private float swipeStartY = 0f;
    private long swipeStartTime = 0L;
    // v0.9.69: situs desktop/horizontal-scroll seperti h-metrics.com tidak boleh
    // dianggap gesture Back saat user menggeser halaman ke samping.
    private boolean webHorizontalGestureGuard = false;
    private String webHorizontalGestureGuardHost = "";

    // ===== Download manager UI state =====
    private RecyclerView activeDownloadRecyclerView;
    private DownloadListAdapter activeDownloadAdapter;
    private LinearLayout activeDownloadCategoryPanel;
    private LinearLayout activeDownloadControlsPanel;
    private TextView activeDownloadTitleView;
    private TextView activeDownloadStorageView;
    private TextView activeDownloadRunningTab;
    private TextView activeDownloadCompletedTab;
    private TextView activeDownloadEmptyView;
    private Dialog activeDownloadDialog;
    private String activeDownloadSection = "Mengunduh";
    private boolean downloadUiTickerRunning;
    private final Runnable downloadUiTicker = new Runnable() {
        @Override
        public void run() {
            if (!downloadUiTickerRunning || activeDownloadDialog == null
                    || !activeDownloadDialog.isShowing()) return;
            renderDownloadList();
            mainHandler.postDelayed(this, getDownloadUiTickerDelayMs());
        }
    };
    private String activeDownloadCategory = "Semua";
    private String activeDownloadSearchQuery = "";
    private String activeDownloadSort = "Tanggal";
    private boolean downloadSelectMode = false;
    private final Set<Integer> selectedDownloadIds = new HashSet<>();
    private final Object downloadHistoryLock = new Object();
    private String lastDownloadControlsSignature = "";
    private long lastDownloadStorageUiMs;
    private String cachedDownloadStorageText = "Penyimpanan tersedia";

    private int tabCount = 1;
    private int nextDownloadId = 1000;

    // ===== Translation state =====
    private boolean translateEnabled = false;
    boolean hideGoogleTranslateBar = true;
    private String lastTranslateOriginalUrl = "";
    private boolean compatibleTranslateActive = false;
    private boolean translateManuallyDisabled = true;
    private int translateSessionToken = 0;
    private long lastCompatibleTranslateStartedAt = 0L;
    String translateTargetLang = "id";
    String translateTargetLabel = "Indonesia";

    // ===== Browser settings state =====
    boolean speedMode = false;
    boolean safeMode = true;
    boolean nightMode = true;
    String nightModeOption = "ON";
    final Set<String> nightModeExceptions = new HashSet<>();
    private int nightModeApplyToken = 0;
    boolean readerMode = false;
    boolean adBlock = true;
    boolean adBlockPopupBlocker = true;
    boolean adBlockRedirectBlocker = true;
    boolean adBlockScriptIframeBlocker = true;
    boolean adBlockClickHijackBlocker = true;
    boolean adBlockRedirectToTempTab = true;
    boolean adBlockAutoCloseAdTabs = true;
    // v0.9.99: document-start protection is installed once per WebView and kept silent.
    private final Map<WebView, ScriptHandler> shieldDocumentStartHandlers =
            Collections.synchronizedMap(new WeakHashMap<>());
    // v0.9.92: "Blokir elemen" (gaya uBlock/Brave). Filter kosmetik manual per host yang
    // disembunyikan dengan display:none dan diterapkan ulang setiap halaman dibuka.
    private final Map<String, LinkedHashSet<String>> userElementFilters = new LinkedHashMap<>();
    private boolean userElementFiltersLoaded = false;
    private boolean elementPickerActive = false;
    private AlertDialog elementPickerDialog = null;
    boolean dataSaver = false;
    // v0.9.98: HTTPS-First tries the secure origin before plain HTTP for public sites.
    boolean httpsFirstEnabled = true;
    boolean desktopMode = false;
    private int browserModeToken = 0;
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
    private boolean downloadQueuePaused = false;
    boolean topIconReload = true;
    boolean topIconBookmark = true;
    boolean topIconTranslate = true;

    // ===== App data collections =====
    private final ArrayList<DownloadItem> downloadItems = new ArrayList<>();
    private final ArrayList<ShortcutItemData> shortcutsData = new ArrayList<>();
    private final ArrayList<HistoryItemData> historyData = new ArrayList<>();
    private final ArrayList<BookmarkItemData> bookmarkData = new ArrayList<>();
    private final ArrayList<TabInfo> tabs = new ArrayList<>();
    private int currentTabIndex = 0;
    private LinearLayout shortcutContainer;
    String searchEngine = "Google";
    private String lastSafeHttpUrl = "";
    // v0.9.43: cache URL halaman aktif untuk thread shouldInterceptRequest.
    // shouldInterceptRequest berjalan di thread Chromium, jadi dilarang memanggil WebView.getUrl() di sana.
    private volatile String currentPageUrlForRequest = "";
    private boolean pendingHideKeyboardAfterNavigation = false;
    // v0.9.75: saat membuat tab baru kosong, showHome tidak boleh menyimpan URL WebView lama ke tab baru.
    private boolean skipNextShowHomeTabSave = false;
    // v0.9.84-multitab: saat tab aktif ditutup, switch berikutnya tidak boleh menyimpan
    // address bar/WebView lama ke tab pengganti. Ini mencegah tab lain ikut berubah
    // menjadi URL tab video/YouTube yang baru ditutup.
    private boolean skipNextSwitchTabStateSave = false;
    private boolean historyClearLock = false;
    /** True only inside the dedicated :incognito WebView process/profile. */
    private boolean dedicatedPrivateProfile = false;

    // ===== Navigation and compatibility guards =====
    // v0.9.41: guard untuk situs yang memaksa reload berulang.
    // Guard ini mencegah Yield ikut menambah DOM injection/recover navigation berulang
    // pada situs berat iklan/anti-adblock, misalnya lordborg.com.
    private String reloadLoopLastKey = "";
    private long reloadLoopWindowStartMs = 0L;
    private int reloadLoopCount = 0;
    private String reloadLoopGuardHost = "";
    private String reloadLoopGuardKey = "";
    private long reloadLoopGuardUntilMs = 0L;
    private long reloadLoopToastLastMs = 0L;
    // Strict compatibility hosts are centralized in BrowserConstants.
    // v0.9.42: navigasi klik user/search result tidak boleh dianggap redirect iklan.
    private String trustedMainFrameHost = "";
    private long trustedMainFrameUntilMs = 0L;

    // v0.9.44: Universal Site Compatibility Guard.
    // Dipakai untuk situs yang anti-adblock/auto-reload/blank supaya Yield berhenti
    // melakukan restore, intercept agresif, dan DOM injection pada domain tersebut.
    private String siteCompatibilityHost = "";
    private long siteCompatibilityUntilMs = 0L;
    private long siteCompatibilityToastLastMs = 0L;
    // v0.9.78: compatibility mode tidak boleh cuma 1 host global.
    // Jika dua tab berbeda sama-sama butuh compatibility (misal Lordborg + Invest Tracing),
    // keduanya harus tetap aktif dan tidak saling mematikan.
    private final Map<String, Long> siteCompatibilityHosts = new LinkedHashMap<>();
    // v0.9.65: universal blank-page compatibility recovery.
    // Jika halaman menjadi blank hanya saat AdBlock ON, host akan otomatis
    // direload sekali dalam compatibility mode tanpa perlu didaftarkan manual.
    private String autoCompatibilityRecoveryHost = "";
    private String autoCompatibilityRecoveryKey = "";
    private long autoCompatibilityRecoveryUntilMs = 0L;

    // ===== WebView JavaScript bridge callbacks =====
    // The JS bridges (TranslateBridge, VideoBridge, AdBlockBridge) are now top-level forwarding
    // shells; MainActivity implements their callback interfaces. The bodies below are unchanged from
    // the former inner-class implementations, so behavior is identical.

    @Override
    public void translateText(int index, String text) {
        if (text == null) return;
        final String clean = text.trim();
        if (clean.length() < 2 || clean.length() > 450) return;
        final int token = translateSessionToken;
        if (!isCompatibleTranslateAllowed(token)) return;

        new Thread(() -> {
            String translated = translateTextViaGoogle(clean);
            if (translated == null || translated.trim().length() == 0) return;
            runOnUiThread(() -> {
                if (isCompatibleTranslateAllowed(token)) applyCompatibleTranslation(index, translated);
            });
        }).start();
    }

    @Override
    public void onCollected(int count) {
        final int token = translateSessionToken;
        runOnUiThread(() -> {
            if (isCompatibleTranslateAllowed(token)) {
                QuietToast.makeText(this, "Translate kompatibel berjalan: " + count + " teks", QuietToast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onVideoPlaying() {
        runOnUiThread(() -> {
            updateVideoPlayPauseButtonState(true);
            showVideoControlsIfAllowed();
        });
    }

    @Override
    public void onVideoTapped() {
        runOnUiThread(() -> {
            videoControlsManualHidden = false;
            showVideoControlsIfAllowed();
        });
    }

    @Override
    public void onVideoStopped() {
        runOnUiThread(() -> {
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
        runOnUiThread(() -> nativeTapWebViewAtRatio(xRatio, yRatio));
    }

    @Override
    public void onAdRedirect(String url) {
        runOnUiThread(() -> captureAdRedirectToTempTab(url));
    }

    @Override
    public void onElementPicked(String selector, String preview) {
        runOnUiThread(() -> onPickerElementSelected(selector, preview, -1, ""));
    }

    @Override
    public void onElementPickedV2(String selector, String preview, int matchCount, String tagName) {
        runOnUiThread(() -> onPickerElementSelected(selector, preview, matchCount, tagName));
    }

    @Override
    public void onPickerExited() {
        runOnUiThread(() -> {
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

    private TabInfo createProfileTab(String normalTitle, String privateTitle, String url,
                                     boolean requestedPrivate, boolean adTab) {
        boolean privateTab = dedicatedPrivateProfile || requestedPrivate;
        String title = privateTab ? privateTitle : normalTitle;
        return new TabInfo(title, url, privateTab, adTab);
    }

    private TabInfo createProfileTab(String normalTitle, String privateTitle, String url,
                                     boolean requestedPrivate) {
        return createProfileTab(normalTitle, privateTitle, url, requestedPrivate, false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        topBarView = createTopBar();
        root.addView(topBarView, new LinearLayout.LayoutParams(-1, -2));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        progressBar.setProgressDrawable(new ColorDrawable(COLOR_ACCENT));
        root.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(3)));

        contentFrame = new FrameLayout(this);
        contentFrame.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1));

        homeScroll = createHomeContent();
        contentFrame.addView(homeScroll, new FrameLayout.LayoutParams(-1, -1));

        TabInfo initialTab = getCurrentTab();
        webView = createBrowserWebView(initialTab, View.GONE);
        try { initialTab.webView = webView; } catch (Exception ignored) {}
        contentFrame.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        navigationLoadingOverlay = createNavigationLoadingOverlay();
        navigationLoadingOverlay.setVisibility(View.GONE);
        contentFrame.addView(navigationLoadingOverlay, new FrameLayout.LayoutParams(-1, -1));

        root.addView(contentFrame);

        videoControlsBar = createVideoControlsBar();
        videoControlsBar.setVisibility(View.GONE);
        root.addView(videoControlsBar, new LinearLayout.LayoutParams(-1, dp(66)));

        bottomNavView = createBottomNav();
        root.addView(bottomNavView, new LinearLayout.LayoutParams(-1, dp(64)));

        setContentView(root);
        installSwipeNavigation(root);
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
            try {
                getContentResolver().takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception ignored) {
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
        downloadUiTickerRunning = false;
        mainHandler.removeCallbacks(downloadUiTicker);
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

    private void discardPrivateTabsForExplicitExit() {
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

    private void handleOpenDownloadsIntent(Intent intent) {
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
    private View createTopBar() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(14), dp(12), dp(14), dp(10));
        wrap.setBackgroundColor(COLOR_BG);

        if (dedicatedPrivateProfile) {
            wrap.addView(createPrivateProfileStrip(), new LinearLayout.LayoutParams(-1, dp(42)));
            wrap.addView(space(dp(8)));
        }

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(12), dp(8), dp(8), dp(8));
        bar.setBackground(roundRect(COLOR_SURFACE_2, dp(18), dp(1), COLOR_BORDER));

        ImageView searchIcon = new ImageView(this);
        searchIcon.setImageResource(R.drawable.ic_search);
        searchIcon.setColorFilter(Color.parseColor("#D3D8E0"));
        bar.addView(searchIcon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        addressBar = new EditText(this);
        addressBar.setBackgroundColor(Color.TRANSPARENT);
        addressBar.setHint("Telusuri Google atau ketik URL");
        addressBar.setHintTextColor(Color.parseColor("#A7ADB8"));
        addressBar.setTextColor(COLOR_TEXT);
        addressBar.setTextSize(16);
        addressBar.setSingleLine(true);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        addressBar.setPadding(dp(10), 0, dp(8), 0);
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
            if (actionId == EditorInfo.IME_ACTION_GO || isEnter) {
                hideKeyboardAndClearFocus(v);
                openAddressBarUrl();
                return true;
            }
            return false;
        });
        bar.addView(addressBar, new LinearLayout.LayoutParams(0, -2, 1));

        reloadButton = smallTopIcon(R.drawable.ic_refresh, "Reload website", v -> reloadCurrentWebsite());
        LinearLayout.LayoutParams reloadParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        reloadParams.setMargins(dp(2), 0, 0, 0);
        bar.addView(reloadButton, reloadParams);

        bookmarkButton = smallTopIcon(R.drawable.ic_star, "Bookmark", v -> toggleBookmark());
        LinearLayout.LayoutParams bookmarkParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        bookmarkParams.setMargins(dp(2), 0, 0, 0);
        bar.addView(bookmarkButton, bookmarkParams);

        translateButton = smallTopIcon(R.drawable.ic_translate, "Translate", v -> toggleTranslate());
        LinearLayout.LayoutParams translateParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        translateParams.setMargins(dp(2), 0, 0, 0);
        bar.addView(translateButton, translateParams);

        ImageButton menu = smallTopIcon(R.drawable.ic_menu_more, "Menu", v -> showQuickMenu());
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        menuParams.setMargins(dp(2), 0, 0, 0);
        bar.addView(menu, menuParams);

        wrap.addView(bar, new LinearLayout.LayoutParams(-1, dp(54)));
        return wrap;
    }

    private View createPrivateProfileStrip() {
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setPadding(dp(12), dp(5), dp(6), dp(5));
        strip.setBackground(roundRect(Color.parseColor("#26143D"), dp(16), dp(1),
                Color.parseColor("#6941A5")));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_private);
        icon.setColorFilter(Color.parseColor("#D8C5FF"));
        strip.addView(icon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(9), 0, dp(8), 0);
        TextView title = new TextView(this);
        title.setText("Mode Privat");
        title.setTextColor(Color.WHITE);
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        texts.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("Profil terisolasi");
        subtitle.setTextColor(Color.parseColor("#BFADE2"));
        subtitle.setTextSize(10);
        texts.addView(subtitle);
        strip.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView normal = new TextView(this);
        normal.setText("Umum");
        normal.setContentDescription("Beralih ke tab umum");
        normal.setTextColor(Color.parseColor("#17121F"));
        normal.setTextSize(12);
        normal.setTypeface(Typeface.DEFAULT_BOLD);
        normal.setGravity(Gravity.CENTER);
        normal.setBackground(roundRect(Color.parseColor("#D8C5FF"), dp(14), 0, Color.TRANSPARENT));
        normal.setOnClickListener(v -> openNormalBrowserSpace());
        strip.addView(normal, new LinearLayout.LayoutParams(dp(72), dp(30)));
        return strip;
    }

    private View createPrivateHomeActions() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackground(roundRect(Color.parseColor("#21162F"), dp(20), dp(1),
                Color.parseColor("#5B3A82")));

        TextView info = new TextView(this);
        info.setText("Tab pada ruang ini tetap privat. Gunakan ruang Umum untuk sesi browsing biasa.");
        info.setTextColor(Color.parseColor("#D8CDE8"));
        info.setTextSize(13);
        card.addView(info);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(42));
        actionsParams.setMargins(0, dp(12), 0, 0);

        TextView normal = new TextView(this);
        normal.setText("Buka tab umum");
        normal.setTextColor(Color.WHITE);
        normal.setTextSize(13);
        normal.setTypeface(Typeface.DEFAULT_BOLD);
        normal.setGravity(Gravity.CENTER);
        normal.setBackground(roundRect(Color.parseColor("#37303F"), dp(16), dp(1),
                Color.parseColor("#665B70")));
        normal.setOnClickListener(v -> openNormalBrowserSpace());
        actions.addView(normal, new LinearLayout.LayoutParams(0, -1, 1));

        TextView addPrivate = new TextView(this);
        addPrivate.setText("+ Tab privat");
        addPrivate.setTextColor(Color.WHITE);
        addPrivate.setTextSize(13);
        addPrivate.setTypeface(Typeface.DEFAULT_BOLD);
        addPrivate.setGravity(Gravity.CENTER);
        addPrivate.setBackground(roundRect(Color.parseColor("#6D28D9"), dp(16), 0,
                Color.TRANSPARENT));
        addPrivate.setOnClickListener(v -> newTabInCurrentProfile());
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(0, -1, 1);
        addParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(addPrivate, addParams);
        card.addView(actions, actionsParams);
        return card;
    }

    private ScrollView createHomeContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(10), dp(16), dp(24));
        content.setMinimumHeight(getResources().getDisplayMetrics().heightPixels - dp(120));
        content.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#20232A"), Color.parseColor("#15171C")}));

        content.addView(space(dp(16)));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleYield = new TextView(this);
        titleYield.setText("Yield");
        titleYield.setTextColor(Color.parseColor("#F4F6FA"));
        titleYield.setTextSize(31);
        titleYield.setLetterSpacing(-0.01f);
        titleYield.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleRow.addView(titleYield);

        TextView titleBrowser = new TextView(this);
        titleBrowser.setText(dedicatedPrivateProfile ? " Privat" : " Browser");
        titleBrowser.setTextColor(dedicatedPrivateProfile
                ? Color.parseColor("#C4A7FF") : Color.parseColor("#DDA13A"));
        titleBrowser.setTextSize(31);
        titleBrowser.setLetterSpacing(-0.01f);
        titleBrowser.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleRow.addView(titleBrowser);

        content.addView(titleRow);

        TextView subtitle = new TextView(this);
        subtitle.setText(dedicatedPrivateProfile
                ? "Sesi terisolasi. Riwayat dan data situs tidak disimpan."
                : "Cepat, ringan, dan siap dipakai.");
        subtitle.setTextColor(COLOR_SUBTEXT);
        subtitle.setTextSize(17);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-2, -2);
        subParams.setMargins(0, dp(8), 0, dp(24));
        content.addView(subtitle, subParams);

        if (dedicatedPrivateProfile) {
            content.addView(createPrivateHomeActions());
            content.addView(space(dp(18)));
        }

        LinearLayout searchCard = new LinearLayout(this);
        searchCard.setOrientation(LinearLayout.HORIZONTAL);
        searchCard.setGravity(Gravity.CENTER_VERTICAL);
        searchCard.setPadding(dp(16), dp(10), dp(10), dp(10));
        searchCard.setBackground(roundRect(Color.parseColor("#252830"), dp(26), dp(1), Color.parseColor("#232730")));

        homeSearchInput = new EditText(this);
        homeSearchInput.setBackgroundColor(Color.TRANSPARENT);
        homeSearchInput.setHint("Telusuri atau ketik URL");
        homeSearchInput.setHintTextColor(Color.parseColor("#9BA2AE"));
        homeSearchInput.setTextColor(COLOR_TEXT);
        homeSearchInput.setTextSize(16);
        homeSearchInput.setSingleLine(true);
        homeSearchInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        homeSearchInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        homeSearchInput.setPadding(0, 0, dp(10), 0);
        homeSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
            if (actionId == EditorInfo.IME_ACTION_GO || isEnter) {
                hideKeyboardAndClearFocus(v);
                openHomeSearchUrl();
                return true;
            }
            return false;
        });
        searchCard.addView(homeSearchInput, new LinearLayout.LayoutParams(0, -2, 1));

        TextView searchButton = new TextView(this);
        searchButton.setText("Cari");
        searchButton.setTextSize(16);
        searchButton.setTypeface(Typeface.DEFAULT_BOLD);
        searchButton.setGravity(Gravity.CENTER);
        searchButton.setTextColor(Color.parseColor("#111111"));
        searchButton.setBackground(roundRect(COLOR_ACCENT, dp(24), 0, Color.TRANSPARENT));
        searchButton.setOnClickListener(v -> { hideKeyboardAndClearFocus(homeSearchInput); openHomeSearchUrl(); });
        searchCard.addView(searchButton, new LinearLayout.LayoutParams(dp(96), dp(48)));

        content.addView(searchCard, new LinearLayout.LayoutParams(-1, -2));
        content.addView(space(dp(28)));

        LinearLayout rowTitle = new LinearLayout(this);
        rowTitle.setOrientation(LinearLayout.HORIZONTAL);
        rowTitle.setGravity(Gravity.CENTER_VERTICAL);
        TextView pintasan = new TextView(this);
        pintasan.setText("Pintasan");
        pintasan.setTextColor(COLOR_TEXT);
        pintasan.setTextSize(18);
        pintasan.setTypeface(Typeface.DEFAULT_BOLD);
        rowTitle.addView(pintasan, new LinearLayout.LayoutParams(0, -2, 1));

        TextView helper = new TextView(this);
        helper.setText("Tekan lama untuk mengedit");
        helper.setTextColor(COLOR_SUBTEXT);
        helper.setTextSize(13);
        rowTitle.addView(helper);
        content.addView(rowTitle);
        content.addView(space(dp(14)));

        HorizontalScrollView shortcutScroll = new HorizontalScrollView(this);
        shortcutScroll.setHorizontalScrollBarEnabled(false);
        shortcutContainer = new LinearLayout(this);
        shortcutContainer.setOrientation(LinearLayout.HORIZONTAL);
        renderShortcuts();
        shortcutScroll.addView(shortcutContainer);
        content.addView(shortcutScroll, new LinearLayout.LayoutParams(-1, -2));

        content.addView(space(dp(28)));
content.addView(space(dp(36)));

        TextView footer = new TextView(this);
        footer.setText("Yield Browser • Modern download manager");
        footer.setTextColor(Color.parseColor("#808794"));
        footer.setTextSize(13);
        content.addView(footer);

        scrollView.addView(content, new ScrollView.LayoutParams(-1, -1));
        return scrollView;
    }

    private void renderShortcuts() {
        if (shortcutContainer == null) return;
        shortcutContainer.removeAllViews();
        for (ShortcutItemData item : shortcutsData) {
            shortcutContainer.addView(shortcutItem(item));
        }
        shortcutContainer.addView(addShortcutItem());
    }

    private LinearLayout shortcutItem(ShortcutItemData shortcut) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(dp(84), -2);
        itemParams.setMargins(0, 0, dp(10), 0);
        item.setLayoutParams(itemParams);

        FrameLayout iconWrap = new FrameLayout(this);
        LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(dp(60), dp(60));
        item.addView(iconWrap, wrapParams);

        TextView fallback = new TextView(this);
        fallback.setText(getShortcutInitial(shortcut.label));
        fallback.setTextColor(Color.WHITE);
        fallback.setTypeface(Typeface.DEFAULT_BOLD);
        fallback.setTextSize(16);
        fallback.setGravity(Gravity.CENTER);
        fallback.setBackground(roundRect(colorForShortcut(shortcut.label), dp(22), dp(1), Color.parseColor("#31353C")));
        iconWrap.addView(fallback, new FrameLayout.LayoutParams(-1, -1));

        ImageView favicon = new ImageView(this);
        favicon.setPadding(dp(12), dp(12), dp(12), dp(12));
        favicon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        favicon.setBackground(roundRect(Color.parseColor("#1C1F26"), dp(22), dp(1), Color.parseColor("#31353C")));
        favicon.setVisibility(View.GONE);
        iconWrap.addView(favicon, new FrameLayout.LayoutParams(-1, -1));
        loadFavicon(shortcut.url, favicon, fallback);

        TextView delete = new TextView(this);
        delete.setText("×");
        delete.setTextColor(Color.WHITE);
        delete.setTextSize(15);
        delete.setTypeface(Typeface.DEFAULT_BOLD);
        delete.setGravity(Gravity.CENTER);
        delete.setBackground(roundRect(Color.parseColor("#E5484D"), dp(12), 0, Color.TRANSPARENT));
        delete.setVisibility(View.GONE);
        FrameLayout.LayoutParams deleteParams = new FrameLayout.LayoutParams(dp(24), dp(24));
        deleteParams.gravity = Gravity.TOP | Gravity.START;
        deleteParams.leftMargin = dp(-2);
        deleteParams.topMargin = dp(-2);
        iconWrap.addView(delete, deleteParams);

        TextView text = new TextView(this);
        text.setText(shortcut.label);
        text.setTextColor(COLOR_TEXT);
        text.setTextSize(13);
        text.setGravity(Gravity.CENTER);
        text.setSingleLine(true);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(-1, -2);
        textParams.setMargins(0, dp(8), 0, 0);
        item.addView(text, textParams);

        item.setOnClickListener(v -> {
            if (delete.getVisibility() == View.VISIBLE) {
                delete.setVisibility(View.GONE);
                return;
            }
            addressBar.setText(shortcut.url);
            openAddressBarUrl();
        });
        item.setOnLongClickListener(v -> {
            delete.setVisibility(View.VISIBLE);
            QuietToast.makeText(this, "Ketuk X untuk hapus pintasan", QuietToast.LENGTH_SHORT).show();
            return true;
        });
        delete.setOnClickListener(v -> {
            shortcutsData.remove(shortcut);
            saveShortcuts();
            renderShortcuts();
            QuietToast.makeText(this, "Pintasan dihapus", QuietToast.LENGTH_SHORT).show();
        });

        return item;
    }

    private LinearLayout addShortcutItem() {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(dp(84), -2);
        itemParams.setMargins(0, 0, dp(10), 0);
        item.setLayoutParams(itemParams);

        TextView circle = new TextView(this);
        circle.setText("+");
        circle.setTextColor(Color.WHITE);
        circle.setTypeface(Typeface.DEFAULT_BOLD);
        circle.setTextSize(24);
        circle.setGravity(Gravity.CENTER);
        circle.setBackground(roundRect(Color.parseColor("#1C1F26"), dp(22), dp(1), Color.parseColor("#31353C")));
        item.addView(circle, new LinearLayout.LayoutParams(dp(60), dp(60)));

        TextView text = new TextView(this);
        text.setText("Tambah");
        text.setTextColor(COLOR_TEXT);
        text.setTextSize(13);
        text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(-1, -2);
        textParams.setMargins(0, dp(8), 0, 0);
        item.addView(text, textParams);

        item.setOnClickListener(v -> showAddShortcutDialog());
        return item;
    }

    private void showAddShortcutDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(4), dp(8), 0);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Nama pintasan, contoh: YouTube");
        nameInput.setSingleLine(true);
        box.addView(nameInput);

        EditText urlInput = new EditText(this);
        urlInput.setHint("URL website, contoh: youtube.com");
        urlInput.setSingleLine(true);
        urlInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        String current = getEffectiveCurrentUrl();
        if (current != null) urlInput.setText(current);
        box.addView(urlInput);

        new AlertDialog.Builder(this)
                .setTitle("Tambah pintasan")
                .setView(box)
                .setPositiveButton("Tambah", (dialog, which) -> {
                    String url = normalizeShortcutUrl(urlInput.getText().toString().trim());
                    if (url == null) {
                        QuietToast.makeText(this, "URL tidak valid", QuietToast.LENGTH_SHORT).show();
                        return;
                    }
                    String label = nameInput.getText().toString().trim();
                    if (label.length() == 0) label = guessLabelFromUrl(url);
                    for (int i = shortcutsData.size() - 1; i >= 0; i--) {
                        ShortcutItemData oldItem = shortcutsData.get(i);
                        if (oldItem != null && url.equals(normalizeShortcutUrl(oldItem.url))) {
                            shortcutsData.remove(i);
                        }
                    }
                    shortcutsData.add(new ShortcutItemData(label, url));
                    saveShortcuts();
                    renderShortcuts();
                    QuietToast.makeText(this, "Pintasan ditambahkan", QuietToast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private String normalizeShortcutUrl(String text) {
        if (text == null || text.trim().length() == 0) return null;
        text = text.trim();
        if (!text.startsWith("http://") && !text.startsWith("https://")) {
            if (!text.contains(".") || text.contains(" ")) return null;
            text = "https://" + text;
        }
        return text;
    }

    private String guessLabelFromUrl(String url) {
        try {
            String clean = url.replace("https://", "").replace("http://", "");
            if (clean.startsWith("www.")) clean = clean.substring(4);
            int slash = clean.indexOf("/");
            if (slash > 0) clean = clean.substring(0, slash);
            String host = clean.split("\\.")[0];
            if (host.length() == 0) return "Web";
            return host.substring(0, 1).toUpperCase() + host.substring(1);
        } catch (Exception e) {
            return "Web";
        }
    }

    private String getShortcutInitial(String label) {
        if (label == null || label.trim().length() == 0) return "?";
        String trimmed = label.trim();
        if (trimmed.length() >= 2 && trimmed.equals(trimmed.toUpperCase())) return trimmed.substring(0, 2);
        return trimmed.substring(0, 1).toUpperCase();
    }

    private int colorForShortcut(String label) {
        int[] colors = new int[]{
                Color.parseColor("#4285F4"), Color.parseColor("#24292F"), Color.parseColor("#FF0033"),
                Color.parseColor("#1DA1F2"), Color.parseColor("#22C55E"), Color.parseColor("#8B5CF6"),
                Color.parseColor("#F97316")
        };
        int idx = Math.abs((label == null ? 0 : label.hashCode())) % colors.length;
        return colors[idx];
    }

    private void loadFavicon(String url, ImageView target, TextView fallback) {
        historyFaviconLoader.load(url, target, fallback);
    }

    private View createBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(4), dp(8), dp(6));
        nav.setBackgroundColor(Color.parseColor("#090A0D"));

        nav.addView(bottomNavButton(R.drawable.ic_home, "Home", v -> navigateCurrentTabHome()));
        nav.addView(bottomNavButton(R.drawable.ic_bookmark, "Bookmark", v -> showBookmarkList()));
        nav.addView(bottomNavButton(R.drawable.ic_search, "Search", v -> {
            if (homeScroll != null && homeScroll.getVisibility() == View.VISIBLE && homeSearchInput != null) {
                homeSearchInput.requestFocus();
            } else {
                addressBar.requestFocus();
            }
        }));
        nav.addView(tabsNavButton(v -> showTabsPanel()));
        nav.addView(bottomNavButton(R.drawable.ic_menu_more, "Menu", v -> showQuickMenu()));
        return nav;
    }

    private LinearLayout bottomNavButton(int iconRes, String description, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setOnClickListener(listener);
        item.setContentDescription(description);
        item.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#F6F7FA"));
        item.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        return item;
    }

    private LinearLayout tabsNavButton(View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setOnClickListener(listener);
        item.setContentDescription("Tabs");
        item.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1));

        FrameLayout box = new FrameLayout(this);
        item.addView(box, new LinearLayout.LayoutParams(dp(24), dp(24)));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_tabs);
        icon.setColorFilter(Color.parseColor("#F6F7FA"));
        box.addView(icon, new FrameLayout.LayoutParams(-1, -1));

        tabsCountText = new TextView(this);
        tabsCountText.setText(String.valueOf(tabCount));
        tabsCountText.setTextColor(Color.parseColor("#F6F7FA"));
        tabsCountText.setTextSize(10);
        tabsCountText.setTypeface(Typeface.DEFAULT_BOLD);
        tabsCountText.setGravity(Gravity.CENTER);
        box.addView(tabsCountText, new FrameLayout.LayoutParams(-1, -1));
        return item;
    }



    private TabInfo findTabByWebView(WebView candidate) {
        return TabWebViewLifecycle.findOwner(tabs, candidate);
    }

    private boolean isLiveTabWebView(TabInfo tab, WebView candidate, long generation) {
        return TabWebViewLifecycle.isLive(tabs, tab, candidate, generation);
    }

    private boolean isPrivateWebView(WebView candidate) {
        return TabWebViewLifecycle.isPrivate(dedicatedPrivateProfile, tabs, candidate);
    }

    private void applyProfileCookiePolicy(WebView candidate) {
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

    private void postForActiveTab(TabInfo tab, WebView candidate, long delayMs, Runnable action) {
        if (tab == null || candidate == null || action == null) return;
        final long generation = tab.webViewGeneration;
        mainHandler.postDelayed(() -> {
            if (!isLiveTabWebView(tab, candidate, generation) || !isCurrentTabInfo(tab)) return;
            action.run();
        }, delayMs);
    }

    private void attachWebViewToContentFrame(WebView candidate) {
        TabWebViewLifecycle.attach(
                contentFrame, homeScroll, navigationLoadingOverlay, candidate);
    }

    private WebView ensureTabWebView(TabInfo tab, int visibility) {
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

    private void hideInactiveTabWebViews(WebView active) {
        TabWebViewLifecycle.hideInactive(tabs, active);
    }

    private void invalidateTabScopedAsyncWork() {
        translateSessionToken++;
        nightModeApplyToken++;
        browserModeToken++;
        pendingHideKeyboardAfterNavigation = false;
        cancelSmoothSearchTransition();
    }

    private void activateNavigationContextForTab(TabInfo tab) {
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

    private void activateTabWebView(TabInfo tab, boolean showWebPage) {
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

    private void destroyTabWebView(TabInfo tab) {
        if (tab == null) return;
        WebView doomed = tab.webView;
        if (doomed != null && doomed == webView && elementPickerActive) {
            finishElementPicker(false);
        }
        if (doomed != null && doomed == webView) webView = null;
        webView = TabWebViewLifecycle.destroy(
                tab, webView, contentFrame, this::removeShieldDocumentStartScript);
    }

    private boolean hasLivePage(WebView candidate) {
        return TabWebViewLifecycle.hasLivePage(candidate, this::extractOriginalUrl);
    }

    private void ensureDefaultTab() {
        if (tabs.isEmpty()) {
            tabs.add(createProfileTab("Tab utama", "Tab privat", "", false));
        }
        currentTabIndex = TabNavigationPolicy.clampIndex(currentTabIndex, tabs.size());
        tabCount = TabNavigationPolicy.countForUi(tabs.size());
    }

    private TabInfo getCurrentTab() {
        ensureDefaultTab();
        return tabs.get(currentTabIndex);
    }

    private boolean isCurrentPrivateTab() {
        return getCurrentTab().privateTab;
    }
    private boolean isCurrentTabInfo(TabInfo tab) {
        try {
            return TabNavigationPolicy.isCurrentTab(
                    tabs, currentTabIndex, tab, tab != null && tab.closed);
        } catch (Exception e) {
            return false;
        }
    }

    private String getTabReferenceUrl(TabInfo tab) {
        if (tab == null) return "";
        String ref = cleanTabSessionUrl(tab.lastSafeUrl);
        if (ref.length() == 0) ref = cleanTabSessionUrl(tab.url);
        if (ref.length() == 0) ref = cleanTabSessionUrl(tab.currentPageUrlForRequest);
        return ref;
    }

    private void prepareTabForMainFrameNavigation(TabInfo tab, String url) {
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

    private boolean isTrustedMainFrameNavigationForTab(TabInfo tab, String url) {
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

    private boolean isSameIsolatedSite(String host, String baseHost) {
        if (host == null || baseHost == null || host.length() == 0 || baseHost.length() == 0) return false;
        return sameOrSubDomain(host, baseHost) || sameOrSubDomain(baseHost, host);
    }

    private boolean isTabIsolationAllowed(TabInfo tab, String url) {
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

    private void syncTabIsolationAfterCommit(TabInfo tab, String url) {
        if (tab == null || tab.closed || url == null) return;
        try {
            String host = BrowserUrlUtils.safeHostForTabIsolation(url);
            if (host.length() > 0) tab.isolationHost = host;
            tab.currentPageUrlForRequest = cleanTabSessionUrl(url);
        } catch (Exception ignored) {
        }
    }

    private String cleanTabSessionUrl(String url) {
        try {
            String clean = extractOriginalUrl(url);
            if (clean == null) clean = url;
            return clean == null ? "" : clean.trim();
        } catch (Exception e) {
            return url == null ? "" : url.trim();
        }
    }

    private boolean isTemporaryDirectAdUrl(String url) {
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

    private boolean isRestorableTabSessionUrl(String url) {
        String clean = cleanTabSessionUrl(url);
        if (clean.length() == 0) return true;
        String lower = clean.toLowerCase(Locale.US);
        if (!isHttpOrHttpsUrl(clean)) return false;
        if (lower.startsWith("about:") || lower.startsWith("javascript:")
                || lower.startsWith("data:") || lower.startsWith("blob:")) return false;
        if (isImageResourceUrl(clean)) return false;
        return !isTemporaryDirectAdUrl(clean);
    }

    private boolean canCommitUrlToTab(TabInfo tab, String url) {
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

    private void commitTabUrlIfSafe(TabInfo tab, String candidateUrl, String candidateTitle) {
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

    private String getSafeUrlForSession(TabInfo tab) {
        if (tab == null) return "";
        String url = cleanTabSessionUrl(tab.url);
        if (url.length() > 0 && canCommitUrlToTab(tab, url)) return url;
        String fallback = cleanTabSessionUrl(tab.lastSafeUrl);
        if (fallback.length() > 0 && isRestorableTabSessionUrl(fallback)) return fallback;
        return url.length() == 0 ? "" : null;
    }

    private boolean isPersistableTab(TabInfo tab) {
        return tab != null && TabSessionPolicy.shouldPersist(tab.closed, tab.privateTab, tab.adTab);
    }

    private TabInfo findPersistedSelectionCandidate() {
        if (tabs.isEmpty()) return null;
        boolean[] persistable = new boolean[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) persistable[i] = isPersistableTab(tabs.get(i));
        int selected = TabSessionPolicy.nearestPersistableIndex(persistable, currentTabIndex);
        return selected >= 0 ? tabs.get(selected) : null;
    }

    private void restoreTabsSession() {
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

    private void saveTabsSession() {
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

    private void restoreActiveTabAfterLaunch() {
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

    private void saveCurrentTabState() {
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

    private void updateTabsCountUi() {
        tabCount = TabNavigationPolicy.countForUi(tabs.size());
        if (tabsCountText != null) {
            tabsCountText.setText(String.valueOf(tabCount));
        }
    }

    private void newTabInCurrentProfile() {
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

    private void launchDedicatedPrivateProfile() {
        launchDedicatedPrivateProfile(false, null);
    }

    private void launchDedicatedPrivateProfile(boolean openTabSwitcher) {
        launchDedicatedPrivateProfile(openTabSwitcher, null);
    }

    private void launchDedicatedPrivateProfile(boolean openTabSwitcher, String openUrl) {
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

    private void launchNormalProfile(boolean openTabSwitcher, boolean createTab) {
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

    private void handleProfileSpaceIntent(Intent intent) {
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

    private void createOrReuseBlankProfileTab() {
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

    private void openUrlInCurrentProfileTab(String url) {
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

    private void openUrlInPrivateSpace(String url) {
        if (url == null || url.trim().isEmpty()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !dedicatedPrivateProfile) {
            launchDedicatedPrivateProfile(false, url);
            return;
        }
        if (!dedicatedPrivateProfile) newPrivateTab();
        openUrlInCurrentProfileTab(url);
    }

    private void openNormalBrowserSpace() {
        if (!dedicatedPrivateProfile) return;
        launchNormalProfile(false, false);
    }

    private void openPrivateBrowserSpace() {
        if (dedicatedPrivateProfile) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            launchDedicatedPrivateProfile(false);
        } else {
            newPrivateTab();
        }
    }


    private void newPrivateTab() {
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

    private void switchToTab(int index) {
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

    private void switchToTab(TabInfo tab) {
        if (tab == null || tab.closed) return;
        int index = tabs.indexOf(tab);
        if (index >= 0) switchToTab(index);
    }

    private void closeTab(TabInfo removed) {
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


    private void showTabsPanel() {
        TabInfo current = getCurrentTab();
        boolean defaultPrivate = BrowserSpacePolicy.isPrivateSpace(
                dedicatedPrivateProfile, current != null && current.privateTab, Build.VERSION.SDK_INT);
        showTabsPanelForSpace(defaultPrivate);
    }

    private void showTabsPanelForSpace(boolean privateSpace) {
        // On modern Android each profile lives in its own process and task. Selecting the other
        // segment brings that space forward; it never converts a live tab between profiles.
        if (BrowserSpacePolicy.mustOpenOtherProcess(
                privateSpace, dedicatedPrivateProfile, Build.VERSION.SDK_INT)) {
            if (privateSpace) launchDedicatedPrivateProfile(true);
            else launchNormalProfile(true, false);
            return;
        }

        saveCurrentTabState();

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(14));
        root.setBackgroundColor(COLOR_BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText(privateSpace ? "Tab Privat" : "Tab Umum");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        heading.addView(title);
        TextView count = new TextView(this);
        int visibleCount = countTabsForSpace(privateSpace);
        count.setText(visibleCount + " tab terbuka");
        count.setTextColor(COLOR_SUBTEXT);
        count.setTextSize(12);
        heading.addView(count);
        header.addView(heading, new LinearLayout.LayoutParams(0, dp(58), 1));

        TextView plus = new TextView(this);
        plus.setText("+");
        plus.setContentDescription(privateSpace ? "Buka tab privat baru" : "Buka tab umum baru");
        plus.setTextColor(privateSpace ? Color.WHITE : Color.parseColor("#111111"));
        plus.setTextSize(28);
        plus.setTypeface(Typeface.DEFAULT_BOLD);
        plus.setGravity(Gravity.CENTER);
        plus.setBackground(roundRect(privateSpace ? Color.parseColor("#6D28D9") : COLOR_ACCENT,
                dp(18), 0, Color.TRANSPARENT));
        plus.setOnClickListener(v -> {
            dialog.dismiss();
            if (privateSpace) {
                if (dedicatedPrivateProfile) newTabInCurrentProfile();
                else newPrivateTab();
            } else {
                if (dedicatedPrivateProfile) launchNormalProfile(false, true);
                else newTabInCurrentProfile();
            }
        });
        header.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(42)));

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(Color.parseColor("#D7DAE0"));
        close.setTextSize(34);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        closeParams.setMargins(dp(8), 0, 0, 0);
        header.addView(close, closeParams);
        root.addView(header);

        LinearLayout selector = new LinearLayout(this);
        selector.setOrientation(LinearLayout.HORIZONTAL);
        selector.setPadding(dp(4), dp(4), dp(4), dp(4));
        selector.setBackground(roundRect(Color.parseColor("#17191F"), dp(20), dp(1), COLOR_BORDER));
        TextView normalChip = profileSpaceChip("Umum", !privateSpace, false);
        TextView privateChip = profileSpaceChip("Privat", privateSpace, true);
        normalChip.setOnClickListener(v -> {
            if (!privateSpace) return;
            dialog.dismiss();
            showTabsPanelForSpace(false);
        });
        privateChip.setOnClickListener(v -> {
            if (privateSpace) return;
            dialog.dismiss();
            showTabsPanelForSpace(true);
        });
        selector.addView(normalChip, new LinearLayout.LayoutParams(0, dp(42), 1));
        LinearLayout.LayoutParams privateChipParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        privateChipParams.setMargins(dp(6), 0, 0, 0);
        selector.addView(privateChip, privateChipParams);
        LinearLayout.LayoutParams selectorParams = new LinearLayout.LayoutParams(-1, dp(50));
        selectorParams.setMargins(0, dp(8), 0, dp(12));
        root.addView(selector, selectorParams);

        TextView hint = new TextView(this);
        hint.setText(privateSpace
                ? "Tab privat memakai profil terisolasi dan tidak disimpan ke sesi umum."
                : "Tab umum menyimpan sesi agar dapat dipulihkan saat aplikasi dibuka kembali.");
        hint.setTextColor(COLOR_SUBTEXT);
        hint.setTextSize(13);
        hint.setPadding(dp(2), 0, dp(2), dp(12));
        root.addView(hint);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);

        int displayNumber = 0;
        for (TabInfo tab : tabs) {
            if (tab == null || tab.closed || tab.privateTab != privateSpace) continue;
            displayNumber++;
            list.addView(tabRow(tab, displayNumber, dialog, privateSpace));
        }

        if (displayNumber == 0) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(48), dp(20), dp(48));
            ImageView icon = new ImageView(this);
            icon.setImageResource(privateSpace ? R.drawable.ic_private : R.drawable.ic_tabs);
            icon.setColorFilter(privateSpace ? Color.parseColor("#C4A7FF") : COLOR_ACCENT);
            empty.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));
            TextView emptyTitle = new TextView(this);
            emptyTitle.setText(privateSpace ? "Belum ada tab privat" : "Belum ada tab umum");
            emptyTitle.setTextColor(COLOR_TEXT);
            emptyTitle.setTextSize(17);
            emptyTitle.setTypeface(Typeface.DEFAULT_BOLD);
            emptyTitle.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams et = new LinearLayout.LayoutParams(-1, -2);
            et.setMargins(0, dp(14), 0, dp(6));
            empty.addView(emptyTitle, et);
            TextView emptyHint = new TextView(this);
            emptyHint.setText("Tekan + untuk membuka tab baru di ruang ini.");
            emptyHint.setTextColor(COLOR_SUBTEXT);
            emptyHint.setTextSize(13);
            emptyHint.setGravity(Gravity.CENTER);
            empty.addView(emptyHint);
            list.addView(empty);
        }

        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.CENTER;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(lp);
        }
        dialog.show();
    }

    private int countTabsForSpace(boolean privateSpace) {
        int count = 0;
        for (TabInfo tab : tabs) {
            if (tab != null && !tab.closed && tab.privateTab == privateSpace) count++;
        }
        return count;
    }


    private TextView profileSpaceChip(String label, boolean selected, boolean privateSpace) {
        return SettingsUi.profileSpaceChip(this, label, selected, privateSpace);
    }


    private View tabRow(TabInfo tab, int displayNumber, Dialog dialog, boolean privateSpace) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(10), dp(12));
        boolean active = tab == getCurrentTab();
        int activeColor = privateSpace ? Color.parseColor("#8B5CF6") : COLOR_ACCENT;
        row.setBackground(roundRect(active ? Color.parseColor("#20232A") : Color.parseColor("#15171D"),
                dp(18), dp(1), active ? activeColor : COLOR_BORDER));

        TextView badge = new TextView(this);
        badge.setText(tab.adTab ? "Ad" : (privateSpace ? "P" : String.valueOf(displayNumber)));
        badge.setTextColor(privateSpace ? Color.WHITE : Color.parseColor("#111111"));
        badge.setTextSize(13);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(roundRect(privateSpace ? Color.parseColor("#6D28D9") : COLOR_ACCENT,
                dp(14), 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        badgeParams.setMargins(0, 0, dp(12), 0);
        row.addView(badge, badgeParams);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        String tabTitle = tab.title == null || tab.title.length() == 0
                ? (privateSpace ? "Tab privat" : "Tab baru") : tab.title;
        title.setText(tabTitle);
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(title);

        TextView url = new TextView(this);
        String urlText = tab.url == null || tab.url.length() == 0 ? "Halaman awal" : tab.url;
        url.setText(privateSpace ? "Privat • " + urlText : urlText);
        url.setTextColor(COLOR_SUBTEXT);
        url.setTextSize(12);
        url.setSingleLine(true);
        url.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(url);

        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView close = new TextView(this);
        close.setText("×");
        close.setContentDescription("Tutup tab");
        close.setTextColor(Color.WHITE);
        close.setTextSize(26);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> {
            closeTab(tab);
            if (!isFinishing()) switchDialogSmooth(dialog, () -> showTabsPanelForSpace(privateSpace));
        });
        row.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));

        row.setOnClickListener(v -> {
            dialog.dismiss();
            switchToTab(tab);
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(params);
        return row;
    }


    private void showQuickMenu() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(12), dp(12), dp(12), dp(12));
        menu.setBackground(roundRect(Color.parseColor("#2B2D33"), dp(24), dp(1), Color.parseColor("#2D333D")));

        if (shortcutDownload) {
            menu.addView(menuRow(R.drawable.ic_download_modern, "Unduhan Yield", v -> {
                switchDialogSmooth(dialog, () -> showDownloadManager());
            }));
        }
        if (shortcutBookmark) {
            menu.addView(menuRow(R.drawable.ic_bookmark, "Bookmark", v -> {
                switchDialogSmooth(dialog, () -> showBookmarkList());
            }));
        }
        if (shortcutPrivate) {
            String profileAction = dedicatedPrivateProfile ? "Beralih ke tab umum" : "Buka ruang privat";
            menu.addView(menuRow(R.drawable.ic_private, profileAction, v -> {
                dialog.dismiss();
                if (dedicatedPrivateProfile) openNormalBrowserSpace();
                else openPrivateBrowserSpace();
            }));
        }
        if (shortcutAdBlock) {
            menu.addView(menuRow(R.drawable.ic_shield, "AdBlock " + (adBlock ? "ON" : "OFF"), v -> {
                adBlock = !adBlock;
                if (!adBlock) stopYouTubeAutoAssistantNow();
                onShieldSettingsChanged();
                dialog.dismiss();
            }));
        }
        if (shortcutReader) {
            menu.addView(menuRow(R.drawable.ic_reader, "Reader Mode " + (readerMode ? "ON" : "OFF"), v -> {
                readerMode = !readerMode;
                saveSettings();
                dialog.dismiss();
                QuietToast.makeText(this, readerMode ? "Reader mode aktif" : "Reader mode nonaktif", QuietToast.LENGTH_SHORT).show();
            }));
        }
        if (shortcutNightMode) {
            menu.addView(menuRow(R.drawable.ic_night, "Mode Malam: " + nightModeLabel(), v -> {
                switchDialogSmooth(dialog, () -> showNightModeSettingsDialog());
            }));
        }
        if (shortcutQrScan) {
            menu.addView(menuRow(R.drawable.ic_qr_scan, "Pindai QR Code", v -> {
                dialog.dismiss();
                openQrScanner();
            }));
        }
        if (shortcutHistory) {
            menu.addView(menuRow(R.drawable.ic_history, "Riwayat", v -> {
                switchDialogSmooth(dialog, () -> showHistoryPanel());
            }));
        }
        if (shortcutFindPage) {
            menu.addView(menuRow(R.drawable.ic_find_page, "Cari di halaman", v -> {
                switchDialogSmooth(dialog, () -> showFindInPageDialog());
            }));
        }
        if (shortcutShare) {
            menu.addView(menuRow(R.drawable.ic_share, "Bagikan halaman", v -> {
                dialog.dismiss();
                shareCurrentPage();
            }));
        }
        if (shortcutFullscreen) {
            menu.addView(menuRow(R.drawable.ic_fullscreen, "Layar penuh", v -> {
                dialog.dismiss();
                toggleFullscreenMode();
            }));
        }
        if (shortcutVideoControls) {
            menu.addView(menuRow(R.drawable.ic_video_control, "Kontrol video " + (videoControlsEnabled ? "ON" : "OFF"), v -> {
                videoControlsEnabled = !videoControlsEnabled;
                saveSettings();
                updateVideoControlsVisibility();
                dialog.dismiss();
                QuietToast.makeText(this, videoControlsEnabled ? "Kontrol video aktif" : "Kontrol video nonaktif", QuietToast.LENGTH_SHORT).show();
            }));
        }

        if (shortcutReloadWebsite) {
            menu.addView(menuRow(R.drawable.ic_refresh, "Reload website", v -> {
                dialog.dismiss();
                reloadCurrentWebsite();
            }));
        }

        if (shortcutBlockElement) {
            menu.addView(menuRow(R.drawable.ic_block_element, "Blokir elemen", v -> {
                dialog.dismiss();
                startElementPicker();
            }));
        }
        if (shortcutSiteFilter) {
            menu.addView(menuRow(R.drawable.ic_safe, "Filter situs ini", v -> {
                switchDialogSmooth(dialog, () -> showUserFiltersManager());
            }));
        }

        menu.addView(menuDivider());
        menu.addView(menuRow(R.drawable.ic_settings, "Setelan", v -> {
            switchDialogSmooth(dialog, () -> showSettingsPanel());
        }));
        menu.addView(menuRow(R.drawable.ic_customize, "Sesuaikan menu", v -> {
            switchDialogSmooth(dialog, () -> showCustomizeMenuPanel());
        }));
        menu.addView(menuRow(R.drawable.ic_exit, "Keluar", v -> {
            try {
                if (!dedicatedPrivateProfile) {
                    recordCurrentPageToHistory();
                    recordWebViewBackForwardHistory();
                    saveBrowserHistory();
                }
            } catch (Exception ignored) {}
            discardPrivateTabsForExplicitExit();
            dialog.dismiss();
            if (dedicatedPrivateProfile) launchNormalProfile(false, false);
            finish();
        }));

        dialog.setContentView(menu);
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM | Gravity.END;
            lp.width = dp(292);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.x = dp(12);
            lp.y = dp(76);
            window.setAttributes(lp);
        }
        dialog.show();
    }

    private String getAppVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (info != null && info.versionName != null && info.versionName.length() > 0) {
                return info.versionName;
            }
        } catch (Exception ignored) {
        }
        return "0.9.99";
    }

    private void showAboutYieldDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#17191E"));
        root.setPadding(dp(18), dp(18), dp(18), dp(18));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_back);
        back.setColorFilter(Color.parseColor("#D8D8DB"));
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(dp(28), dp(28));
        header.addView(back, backLp);

        TextView title = new TextView(this);
        title.setText("Tentang Yield");
        title.setTextColor(Color.parseColor("#EDEDF0"));
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1f);
        titleLp.setMargins(dp(18), 0, dp(18), 0);
        header.addView(title, titleLp);

        ImageView close = new ImageView(this);
        close.setImageResource(R.drawable.ic_close);
        close.setColorFilter(Color.parseColor("#D8D8DB"));
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(dp(28), dp(28));
        header.addView(close, closeLp);

        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.setMargins(0, dp(20), 0, 0);
        root.addView(aboutInfoCard("Versi aplikasi", "Yield Browser " + getAppVersionName()), cardLp);

        String osInfo = "Android " + Build.VERSION.RELEASE + " ; Build/" + Build.ID;
        LinearLayout.LayoutParams cardLp2 = new LinearLayout.LayoutParams(-1, -2);
        cardLp2.setMargins(0, dp(10), 0, 0);
        root.addView(aboutInfoCard("Sistem operasi", osInfo), cardLp2);

        LinearLayout.LayoutParams cardLp3 = new LinearLayout.LayoutParams(-1, -2);
        cardLp3.setMargins(0, dp(10), 0, 0);
        root.addView(aboutInfoCard("Developer", "develop by yoski days"), cardLp3);

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(-1, 0, 1f));

        back.setOnClickListener(v -> dialog.dismiss());
        close.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(root);
        dialog.show();
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#17191E")));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }


    private View aboutInfoCard(String heading, String value) {
        return SettingsUi.aboutInfoCard(this, heading, value);
    }

    private void showSettingsPanel() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(16), dp(14), dp(16));
        panel.setBackground(roundRect(Color.parseColor("#2B2D33"), dp(24), dp(1), Color.parseColor("#3A3D45")));
        scroll.addView(panel);

        TextView title = new TextView(this);
        title.setText("Setelan Yield");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(dp(8), 0, 0, dp(8));
        panel.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Pusat semua fitur. Kalau shortcut menu dimatikan, fitur tetap bisa dibuka dari sini.");
        hint.setTextColor(COLOR_SUBTEXT);
        hint.setTextSize(13);
        hint.setPadding(dp(8), 0, dp(8), dp(12));
        panel.addView(hint);

        panel.addView(actionRow(R.drawable.ic_translate, "Translate", "Pilihan translate, hide bar Google Translate, dan translate teks halaman.", v -> {
            showTranslateOptionsDialog();
        }));

        panel.addView(sectionTitle("Pusat fitur"));
        panel.addView(actionRow(R.drawable.ic_download_modern, "Unduhan Yield", "Riwayat, progress, lokasi penyimpanan, dan engine 2 koneksi.", v -> {
            switchDialogSmooth(dialog, () -> showDownloadSettingsPanel());
        }));
        panel.addView(actionRow(R.drawable.ic_bookmark, "Bookmark", "Buka daftar bookmark yang tersimpan.", v -> {
            switchDialogSmooth(dialog, () -> showBookmarkList());
        }));
        panel.addView(actionRow(R.drawable.ic_private,
                dedicatedPrivateProfile ? "Tab umum" : "Ruang privat",
                dedicatedPrivateProfile
                        ? "Kembali ke profil umum tanpa mencampur data sesi privat."
                        : "Buka profil terisolasi tanpa menyimpan riwayat ke sesi umum.", v -> {
            dialog.dismiss();
            if (dedicatedPrivateProfile) openNormalBrowserSpace();
            else openPrivateBrowserSpace();
        }));
        panel.addView(actionRow(R.drawable.ic_customize, "Sesuaikan menu", "Atur shortcut yang muncul di menu utama.", v -> {
            switchDialogSmooth(dialog, () -> showCustomizeMenuPanel());
        }));
        panel.addView(actionRow(R.drawable.ic_qr_scan, "Pindai QR Code", "Scan QR untuk membuka link atau mencari teks.", v -> {
            dialog.dismiss();
            openQrScanner();
        }));
        panel.addView(actionRow(R.drawable.ic_search_engine, "Search engine: " + searchEngine, "Pilih Google, Bing, DuckDuckGo, Yahoo, atau Yandex.", v -> {
            showSearchEngineDialog();
        }));

        panel.addView(sectionTitle("Alat halaman"));
        panel.addView(actionRow(R.drawable.ic_block_element, "Blokir elemen", "Pilih elemen pada halaman aktif untuk disembunyikan. Tetap tersedia di Setelan meski shortcut menu utama dimatikan.", v -> {
            dialog.dismiss();
            startElementPicker();
        }));
        panel.addView(actionRow(R.drawable.ic_safe, "Filter situs ini", "Kelola filter dan elemen yang diblokir untuk situs aktif. Tetap tersedia di Setelan meski shortcut menu utama dimatikan.", v -> {
            switchDialogSmooth(dialog, () -> showUserFiltersManager());
        }));
        panel.addView(actionRow(R.drawable.ic_history, "Riwayat browsing", "Lihat dan buka kembali halaman yang pernah dikunjungi.", v -> {
            switchDialogSmooth(dialog, () -> showHistoryPanel());
        }));
        panel.addView(actionRow(R.drawable.ic_find_page, "Cari di halaman", "Cari teks pada halaman web yang sedang terbuka.", v -> {
            showFindInPageDialog();
        }));
        panel.addView(actionRow(R.drawable.ic_share, "Bagikan halaman", "Bagikan link halaman saat ini ke aplikasi lain.", v -> {
            shareCurrentPage();
        }));
        panel.addView(actionRow(R.drawable.ic_copy_link, "Salin link halaman", "Salin URL halaman saat ini ke clipboard.", v -> {
            copyCurrentLink();
        }));
        panel.addView(actionRow(R.drawable.ic_page_info, "Info halaman", "Tampilkan judul dan URL halaman.", v -> {
            showPageInfoDialog();
        }));
        panel.addView(actionRow(R.drawable.ic_fullscreen, "Mode layar penuh", "Sembunyikan toolbar dan navigasi sementara.", v -> {
            toggleFullscreenMode();
        }));
        panel.addView(settingRow(R.drawable.ic_video_control, "Kontrol video online", "Tampilkan tombol Play, Pause, Stop, dan Speed pada player web.", videoControlsEnabled, v -> {
            videoControlsEnabled = !videoControlsEnabled;
            updateVideoControlsVisibility();
            saveSettings();
            switchDialogSmooth(dialog, () -> showSettingsPanel());
        }));
        panel.addView(actionRow(R.drawable.ic_video_control, "Optimasi video online", "Buffer booster, HLS prefetch, kualitas, floating player, dan background play.", v -> {
            showVideoOptimizationDialog();
        }));
        panel.addView(actionRow(R.drawable.ic_save_page, "Simpan halaman offline", "Simpan halaman saat ini sebagai web archive.", v -> {
            saveCurrentPageOffline();
        }));

        panel.addView(sectionTitle("Fitur browsing"));
        panel.addView(settingRow(R.drawable.ic_speed, "Mode cepat", "Optimasi cache, gambar, dan resource.", speedMode, v -> { speedMode = !speedMode; applyBrowserSettings(); saveSettings(); }));
        panel.addView(settingRow(R.drawable.ic_safe, "Safe browsing", "Blokir URL berisiko sederhana.", safeMode, v -> { safeMode = !safeMode; saveSettings(); }));
        panel.addView(settingRow(R.drawable.ic_safe, "HTTPS-First", "Coba koneksi HTTPS lebih dulu. Jika HTTPS benar-benar tidak tersedia, kembali ke HTTP dengan perlindungan loop. Alamat lokal dan port khusus dikecualikan.", httpsFirstEnabled, v -> {
            httpsFirstEnabled = !httpsFirstEnabled;
            if (!httpsFirstEnabled) clearAllHttpsFirstRuntimeState();
            saveSettings();
        }));
        panel.addView(actionRow(R.drawable.ic_night, "Mode Malam: " + nightModeLabel(), "OFF, ON, Auto ikut sistem, dan pengecualian situs. Tidak menutup menu setelan.", v -> {
            showNightModeSettingsDialog();
        }));
        panel.addView(settingRow(R.drawable.ic_reader, "Reader / novel mode", "Mode baca ringan untuk artikel.", readerMode, v -> { readerMode = !readerMode; saveSettings(); }));
        panel.addView(actionRow(R.drawable.ic_shield, "AdBlock: " + (adBlock ? "ON" : "OFF"), getAdBlockSummary(), v -> {
            showAdBlockSettingsDialog();
        }));
        panel.addView(settingRow(R.drawable.ic_data_saver, "Hemat data", "Matikan gambar otomatis saat browsing.", dataSaver, v -> { dataSaver = !dataSaver; applyBrowserSettings(); saveSettings(); }));
        panel.addView(settingRow(R.drawable.ic_desktop, "Desktop mode", "Paksa tampilan lebar seperti PC/laptop.", desktopMode, v -> {
            toggleDesktopModeSafely();
        }));
        panel.addView(actionRow(R.drawable.ic_text_size, "Ukuran teks: " + textZoom + "%", "Atur ukuran teks dengan slider persentase.", v -> {
            showTextZoomDialog(dialog);
        }));
        panel.addView(actionRow(R.drawable.ic_clear, "Bersihkan cache", "Hapus cache WebView.", v -> { if (webView != null) webView.clearCache(true); QuietToast.makeText(this, "Cache dibersihkan", QuietToast.LENGTH_SHORT).show(); }));


        panel.addView(sectionTitle("Informasi"));
        panel.addView(actionRow(R.drawable.ic_info, "Tentang Yield", "Versi aplikasi dan informasi developer.", v -> {
            showAboutYieldDialog();
        }));
        dialog.setContentView(scroll);
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.82f);
            window.setAttributes(lp);
        }
        dialog.show();
    }

    private String getAdBlockSummary() {
        return adBlock
                ? "Perlindungan otomatis tanpa notifikasi yang mengganggu."
                : "Perlindungan situs sedang nonaktif.";
    }

    private void showAdBlockSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setBackground(roundRect(Color.parseColor("#26292F"), dp(24), dp(1), COLOR_BORDER));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(14));
        scroll.addView(box, new ScrollView.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("AdBlock");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText("Semua perlindungan iklan tetap berada di dalam AdBlock: popup, redirect, click hijack, script, iframe, dan tracker. Media utama tetap diprioritaskan.");
        info.setTextColor(COLOR_SUBTEXT);
        info.setTextSize(13);
        info.setLineSpacing(0, 1.05f);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, -2);
        infoLp.setMargins(0, dp(8), 0, dp(12));
        box.addView(info, infoLp);

        box.addView(adBlockSwitchRow("AdBlock aktif", "Master ON/OFF semua proteksi iklan.", adBlock, v -> {
            adBlock = !adBlock;
            if (!adBlock) stopYouTubeAutoAssistantNow();
            onShieldSettingsChanged();
        }));

        box.addView(adBlockSwitchRow("Blokir popup iklan", "Matikan window.open dan pop-up otomatis.", adBlockPopupBlocker, v -> {
            adBlockPopupBlocker = !adBlockPopupBlocker;
            onShieldSettingsChanged();
        }));

        box.addView(adBlockSwitchRow("Blokir redirect iklan", "Cegah halaman pindah ke domain iklan random.", adBlockRedirectBlocker, v -> {
            adBlockRedirectBlocker = !adBlockRedirectBlocker;
            onShieldSettingsChanged();
        }));

        box.addView(adBlockSwitchRow("Blokir script/iframe iklan", "Blokir resource iklan, script, iframe, dan tracker.", adBlockScriptIframeBlocker, v -> {
            adBlockScriptIframeBlocker = !adBlockScriptIframeBlocker;
            onShieldSettingsChanged();
        }));

        box.addView(adBlockSwitchRow("Proteksi click hijack", "Cegah klik area web membuka link iklan tersembunyi.", adBlockClickHijackBlocker, v -> {
            adBlockClickHijackBlocker = !adBlockClickHijackBlocker;
            onShieldSettingsChanged();
        }));

        box.addView(adBlockSwitchRow("Alihkan iklan ke tab sementara", "Jika web memaksa redirect iklan, buka sebagai tab iklan sementara agar tab utama tetap aman.", adBlockRedirectToTempTab, v -> {
            adBlockRedirectToTempTab = !adBlockRedirectToTempTab;
            onShieldSettingsChanged();
        }));

        box.addView(adBlockSwitchRow("Auto close tab iklan", "Tab hasil redirect/popup iklan otomatis ditutup agar tab tidak menumpuk.", adBlockAutoCloseAdTabs, v -> {
            adBlockAutoCloseAdTabs = !adBlockAutoCloseAdTabs;
            onShieldSettingsChanged();
        }));

        TextView note = new TextView(this);
        note.setText("Catatan: video playback tidak diberi tombol khusus karena selalu dilindungi otomatis agar file video/YouTube/GoogleVideo tidak terblokir.");
        note.setTextColor(COLOR_SUBTEXT);
        note.setTextSize(12);
        note.setLineSpacing(0, 1.05f);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2);
        noteLp.setMargins(0, dp(8), 0, dp(4));
        box.addView(note, noteLp);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.END);
        bottom.setPadding(0, dp(8), 0, 0);

        TextView close = dialogTextButton("TUTUP");
        close.setOnClickListener(v -> dialog.dismiss());
        bottom.addView(close);
        box.addView(bottom);

        dialog.setContentView(scroll);
        dialog.show();
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
    }


    private View adBlockSwitchRow(String title, String desc, boolean enabled, View.OnClickListener listener) {
        return SettingsUi.adBlockSwitchRow(this, title, desc, enabled, listener);
    }

    private void openQrScanner() {
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

    private void showQrScannerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(14), dp(14), dp(14));
        panel.setBackground(roundRect(Color.parseColor("#2B2D33"), dp(24), dp(1), Color.parseColor("#3A3D45")));

        TextView title = new TextView(this);
        title.setText("Pindai QR Code");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(title);

        TextView hint = new TextView(this);
        hint.setText("Arahkan kamera ke QR. Jika QR berisi link, Yield akan langsung membukanya.");
        hint.setTextColor(COLOR_SUBTEXT);
        hint.setTextSize(13);
        hint.setPadding(0, dp(6), 0, dp(10));
        panel.addView(hint);

        QrScannerView scanner = new QrScannerView(this, result -> {
            dialog.dismiss();
            handleQrResult(result);
        });
        panel.addView(scanner, new LinearLayout.LayoutParams(-1, dp(360)));

        TextView cancel = new TextView(this);
        cancel.setText("Tutup");
        cancel.setTextColor(Color.WHITE);
        cancel.setGravity(Gravity.CENTER);
        cancel.setTextSize(16);
        cancel.setTypeface(Typeface.DEFAULT_BOLD);
        cancel.setPadding(0, dp(12), 0, dp(4));
        cancel.setOnClickListener(v -> dialog.dismiss());
        panel.addView(cancel, new LinearLayout.LayoutParams(-1, dp(52)));

        dialog.setOnDismissListener(d -> scanner.stopCamera());

        dialog.setContentView(panel);
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
        dialog.show();
    }

    private void handleQrResult(String result) {
        if (result == null || result.trim().length() == 0) {
            QuietToast.makeText(this, "QR kosong", QuietToast.LENGTH_SHORT).show();
            return;
        }
        String value = result.trim();
        QuietToast.makeText(this, "QR terbaca", QuietToast.LENGTH_SHORT).show();
        addressBar.setText(value);
        openAddressBarUrl();
    }
    private void showSearchEngineDialog() {
        String[] engines = new String[]{"Google", "Bing", "DuckDuckGo", "Yahoo", "Yandex"};
        int checked = 0;
        for (int i = 0; i < engines.length; i++) {
            if (engines[i].equals(searchEngine)) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle("Pilih search engine")
                .setSingleChoiceItems(engines, checked, (dialog, which) -> {
                    searchEngine = engines[which];
                    saveSettings();
                    dialog.dismiss();
                    // v0.9.68: jangan tutup/recreate panel Settings setelah memilih search engine.
                    // Recreate dialog lama membuat efek kedip/flicker balik ke home.
                    // Nilai search engine langsung tersimpan dan dipakai oleh pencarian berikutnya.
                    QuietToast.makeText(this, "Search engine: " + searchEngine, QuietToast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showCustomizeMenuPanel() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(16), dp(14), dp(18));
        panel.setBackgroundColor(Color.parseColor("#1E2024"));
        scroll.addView(panel);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(18));

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextColor(Color.WHITE);
        back.setTextSize(42);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> dialog.dismiss());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView title = new TextView(this);
        title.setText("Sesuaikan menu");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(Color.WHITE);
        close.setTextSize(36);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));

        panel.addView(header);

        TextView topSub = new TextView(this);
        topSub.setText("Icon atas");
        topSub.setTextColor(COLOR_SUBTEXT);
        topSub.setTextSize(16);
        topSub.setTypeface(Typeface.DEFAULT_BOLD);
        topSub.setPadding(dp(8), 0, 0, dp(12));
        panel.addView(topSub);

        panel.addView(customizeToggleRow(R.drawable.ic_refresh, "Reload di address bar", topIconReload, v -> {
            topIconReload = !topIconReload;
            saveSettings();
            updateTopActionStates();
        }));
        panel.addView(customizeToggleRow(R.drawable.ic_bookmark, "Bookmark di address bar", topIconBookmark, v -> {
            topIconBookmark = !topIconBookmark;
            saveSettings();
            updateTopActionStates();
        }));
        panel.addView(customizeToggleRow(R.drawable.ic_translate, "Translate di address bar", topIconTranslate, v -> {
            topIconTranslate = !topIconTranslate;
            saveSettings();
            updateTopActionStates();
        }));

        TextView sub = new TextView(this);
        sub.setText("Menu utama");
        sub.setTextColor(COLOR_SUBTEXT);
        sub.setTextSize(16);
        sub.setTypeface(Typeface.DEFAULT_BOLD);
        sub.setPadding(dp(8), 0, 0, dp(12));
        panel.addView(sub);

        panel.addView(customizeToggleRow(R.drawable.ic_refresh, "Reload website", shortcutReloadWebsite, v -> { shortcutReloadWebsite = !shortcutReloadWebsite; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_download_modern, "Unduhan Yield", shortcutDownload, v -> { shortcutDownload = !shortcutDownload; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_bookmark, "Bookmark", shortcutBookmark, v -> { shortcutBookmark = !shortcutBookmark; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_private, "Privat", shortcutPrivate, v -> { shortcutPrivate = !shortcutPrivate; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_shield, "AdBlock", shortcutAdBlock, v -> { shortcutAdBlock = !shortcutAdBlock; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_reader, "Reader / Novel Mode", shortcutReader, v -> { shortcutReader = !shortcutReader; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_night, "Night Mode", shortcutNightMode, v -> { shortcutNightMode = !shortcutNightMode; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_qr_scan, "Pindai QR Code", shortcutQrScan, v -> { shortcutQrScan = !shortcutQrScan; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_history, "Riwayat", shortcutHistory, v -> { shortcutHistory = !shortcutHistory; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_find_page, "Cari di halaman", shortcutFindPage, v -> { shortcutFindPage = !shortcutFindPage; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_share, "Bagikan halaman", shortcutShare, v -> { shortcutShare = !shortcutShare; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_fullscreen, "Layar penuh", shortcutFullscreen, v -> { shortcutFullscreen = !shortcutFullscreen; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_block_element, "Blokir elemen", shortcutBlockElement, v -> { shortcutBlockElement = !shortcutBlockElement; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_safe, "Filter situs ini", shortcutSiteFilter, v -> { shortcutSiteFilter = !shortcutSiteFilter; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_video_control, "Kontrol video", shortcutVideoControls, v -> { shortcutVideoControls = !shortcutVideoControls; saveSettings(); }));

        dialog.setContentView(scroll);
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.9f);
            window.setAttributes(lp);
        }
        dialog.show();
    }

private void showDownloadSettingsPanel() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(18));
        panel.setBackground(roundRect(Color.parseColor("#2B2D33"), dp(24), dp(1), Color.parseColor("#3A3D45")));

        TextView title = new TextView(this);
        title.setText("Pengaturan Unduhan");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(title);

        TextView path = new TextView(this);
        path.setText(getDownloadLocationText());
        path.setTextColor(COLOR_SUBTEXT);
        path.setTextSize(13);
        path.setPadding(0, dp(8), 0, dp(12));
        panel.addView(path);

        panel.addView(actionRow(R.drawable.ic_download_modern, "Buka riwayat unduhan", "Lihat file, progress, open, hapus riwayat/file.", v -> {
            switchDialogSmooth(dialog, () -> showDownloadManager());
        }));
        panel.addView(actionRow(R.drawable.ic_folder, "Lokasi / folder unduhan", "Default: Download/Yield Browser, atau pilih folder HP.", v -> {
            showDownloadFolderDialog(dialog);
        }));
        panel.addView(actionRow(R.drawable.ic_clear, "Bersihkan riwayat selesai", "Hanya menghapus riwayat, file tetap aman.", v -> {
            synchronized (downloadItems) {
                for (int i = downloadItems.size() - 1; i >= 0; i--) {
                    if (!"running".equals(downloadItems.get(i).status)) {
                        downloadItems.remove(i);
                    }
                }
            }
            saveDownloadHistory();
            QuietToast.makeText(this, "Riwayat unduhan selesai dibersihkan", QuietToast.LENGTH_SHORT).show();
            switchDialogSmooth(dialog, () -> showDownloadSettingsPanel());
        }));

        panel.addView(actionRow(R.drawable.ic_settings, "Yield Fast Download", getAdvancedDownloadSummary(), v -> {
            showAdvancedDownloadFeaturesDialog(dialog);
        }));

        panel.addView(actionRow(R.drawable.ic_settings, "Download Queue: " + (downloadQueueEnabled ? "ON" : "OFF"), getDownloadQueueSummary(), v -> {
            showDownloadQueueSettingsDialog();
        }));
        dialog.setContentView(panel);
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
        dialog.show();
    }

    private String getAdvancedDownloadSummary() {
        String limit = downloadSpeedLimitKBps > 0 ? (downloadSpeedLimitKBps + " KB/s") : "tanpa limit";
        return "Dynamic 2/4 koneksi, retry, HLS/m3u8, speed limiter: " + limit;
    }

    private String getDownloadQueueSummary() {
        return "Maks aktif: " + downloadMaxActive + " • aktif: " + countActiveDownloads() + " • antri: " + countQueuedDownloads();
    }

    private void showDownloadQueueSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setBackground(roundRect(Color.parseColor("#26292F"), dp(24), dp(1), COLOR_BORDER));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(14));
        scroll.addView(box, new ScrollView.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("Download Queue");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText("Atur antrian download tanpa keluar dari menu.");
        info.setTextColor(COLOR_SUBTEXT);
        info.setTextSize(13);
        info.setPadding(0, dp(8), 0, dp(12));
        box.addView(info);

        final TextView[] statusText = new TextView[1];

        box.addView(videoOptSwitchRow("Download Queue", "Batasi download aktif agar koneksi lebih stabil.", downloadQueueEnabled, v -> {
            downloadQueueEnabled = !downloadQueueEnabled;
            downloadQueuePaused = false;
            saveSettings();
            pumpDownloadQueue();
            if (statusText[0] != null) statusText[0].setText(getDownloadQueueSummary());
            refreshDownloadPanel();
        }));

        TextView maxTitle = new TextView(this);
        maxTitle.setText("Batas maksimal download aktif");
        maxTitle.setTextColor(Color.WHITE);
        maxTitle.setTextSize(15);
        maxTitle.setTypeface(Typeface.DEFAULT_BOLD);
        maxTitle.setPadding(0, dp(8), 0, dp(8));
        box.addView(maxTitle);

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        choices.setGravity(Gravity.CENTER);
        choices.setPadding(0, 0, 0, dp(8));
        box.addView(choices, new LinearLayout.LayoutParams(-1, -2));

        final TextView[] c2 = new TextView[1];
        final TextView[] c3 = new TextView[1];
        final TextView[] c4 = new TextView[1];

        Runnable refreshChoices = () -> {
            updateQueueChoiceChip(c2[0], 2);
            updateQueueChoiceChip(c3[0], 3);
            updateQueueChoiceChip(c4[0], 4);
            if (statusText[0] != null) statusText[0].setText(getDownloadQueueSummary());
        };

        c2[0] = queueChoiceChip("2", 2, refreshChoices);
        c3[0] = queueChoiceChip("3", 3, refreshChoices);
        c4[0] = queueChoiceChip("4", 4, refreshChoices);

        choices.addView(c2[0], new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams c3Lp = new LinearLayout.LayoutParams(0, dp(46), 1);
        c3Lp.setMargins(dp(8), 0, 0, 0);
        choices.addView(c3[0], c3Lp);
        LinearLayout.LayoutParams c4Lp = new LinearLayout.LayoutParams(0, dp(46), 1);
        c4Lp.setMargins(dp(8), 0, 0, 0);
        choices.addView(c4[0], c4Lp);
        refreshChoices.run();

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);
        actionRow.setPadding(0, dp(6), 0, dp(8));
        box.addView(actionRow, new LinearLayout.LayoutParams(-1, -2));

        TextView pause = downloadToolButton("Pause semua");
        pause.setOnClickListener(v -> {
            pauseAllDownloads();
            if (statusText[0] != null) statusText[0].setText(getDownloadQueueSummary());
        });
        actionRow.addView(pause, new LinearLayout.LayoutParams(0, dp(42), 1));

        TextView resume = downloadToolButton("Resume semua");
        resume.setOnClickListener(v -> {
            resumeAllDownloads();
            if (statusText[0] != null) statusText[0].setText(getDownloadQueueSummary());
        });
        LinearLayout.LayoutParams resumeLp = new LinearLayout.LayoutParams(0, dp(42), 1);
        resumeLp.setMargins(dp(8), 0, 0, 0);
        actionRow.addView(resume, resumeLp);

        TextView sort = downloadToolButton("Urutkan: Antrian");
        sort.setOnClickListener(v -> {
            activeDownloadSort = "Antrian";
            renderDownloadList();
            QuietToast.makeText(this, "Tampilan diurutkan berdasarkan antrian", QuietToast.LENGTH_SHORT).show();
        });
        box.addView(sort, new LinearLayout.LayoutParams(-1, dp(42)));

        statusText[0] = new TextView(this);
        statusText[0].setText(getDownloadQueueSummary());
        statusText[0].setTextColor(COLOR_SUBTEXT);
        statusText[0].setTextSize(12);
        statusText[0].setGravity(Gravity.CENTER);
        statusText[0].setPadding(0, dp(12), 0, dp(4));
        box.addView(statusText[0]);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.END);
        bottom.setPadding(0, dp(8), 0, 0);

        TextView close = dialogTextButton("TUTUP");
        close.setOnClickListener(v -> dialog.dismiss());
        bottom.addView(close);
        box.addView(bottom);

        dialog.setContentView(scroll);
        dialog.show();
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
    }

    private TextView queueChoiceChip(String label, int value, Runnable refresh) {
        TextView chip = SettingsUi.queueChoiceChip(this, label, v -> {
            downloadMaxActive = value;
            downloadQueueEnabled = true;
            downloadQueuePaused = false;
            saveSettings();
            pumpDownloadQueue();
            refreshDownloadPanel();
            if (refresh != null) refresh.run();
            QuietToast.makeText(this, "Maksimal download aktif: " + value, QuietToast.LENGTH_SHORT).show();
        });
        updateQueueChoiceChip(chip, value);
        return chip;
    }

    private void updateQueueChoiceChip(TextView chip, int value) {
        if (chip == null) return;
        boolean selected = downloadMaxActive == value;
        chip.setTextColor(selected ? Color.parseColor("#111111") : Color.WHITE);
        chip.setBackground(roundRect(selected ? COLOR_ACCENT : Color.parseColor("#20232A"), dp(18), dp(1), selected ? COLOR_ACCENT : COLOR_BORDER));
    }
    private void showAdvancedDownloadFeaturesDialog(Dialog parentDialog) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setBackground(roundRect(Color.parseColor("#26292F"), dp(24), dp(1), COLOR_BORDER));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(14));
        scroll.addView(box, new ScrollView.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("Yield Fast Download");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText("Safe Brave-Class Layer v3: memilih Safe 1, Stable 2, Balanced 3, atau Turbo 4 koneksi sesuai host, file, dan prediksi bandwidth.");
        info.setTextColor(COLOR_SUBTEXT);
        info.setTextSize(13);
        info.setLineSpacing(0, 1.05f);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, -2);
        infoLp.setMargins(0, dp(8), 0, dp(12));
        box.addView(info, infoLp);

        box.addView(advancedSwitchRow("Smart Turbo Download", "Auto pilih Safe 1, Stable 2, Balanced 3, atau Turbo 4 koneksi. Ada fallback aman jika server mulai throttle.", downloadDynamic4Connections, v -> {
            downloadDynamic4Connections = !downloadDynamic4Connections;
            saveSettings();
        }));

        box.addView(advancedSwitchRow("Retry otomatis", "Jika koneksi putus, Yield mencoba ulang sampai 3x dan lanjut dari progres terakhir.", downloadAutoRetry, v -> {
            downloadAutoRetry = !downloadAutoRetry;
            saveSettings();
        }));

        box.addView(advancedSwitchRow("Download HLS/m3u8", "Playlist m3u8 akan dideteksi dan segmen video digabung ke file TS.", downloadHlsEnabled, v -> {
            downloadHlsEnabled = !downloadHlsEnabled;
            saveSettings();
        }));

        box.addView(advancedSwitchRow("Putar sambil mengunduh", "Video progresif dapat ditonton dari bagian yang sudah tersedia. Download tetap berjalan di latar belakang.", downloadPlayWhileDownloadingEnabled, v -> {
            downloadPlayWhileDownloadingEnabled = !downloadPlayWhileDownloadingEnabled;
            saveSettings();
        }));

        box.addView(advancedInfoRow("Player internal + HTTP Range lokal"));
        box.addView(advancedInfoRow("Smart resume + hard pause"));
        box.addView(advancedInfoRow("Bandwidth prediction"));
        box.addView(advancedInfoRow("Per-file optimization"));
        box.addView(advancedInfoRow("Host-aware stable mode"));

        TextView limiter = darkDialogActionButton("SPEED LIMITER: " + (downloadSpeedLimitKBps > 0 ? downloadSpeedLimitKBps + " KB/s" : "OFF"));
        limiter.setOnClickListener(v -> showSpeedLimiterDialog(dialog, parentDialog));
        LinearLayout.LayoutParams limiterLp = new LinearLayout.LayoutParams(-1, dp(46));
        limiterLp.setMargins(0, dp(2), 0, 0);
        box.addView(limiter, limiterLp);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.END);
        bottom.setPadding(0, dp(8), 0, 0);

        TextView close = dialogTextButton("TUTUP");
        close.setOnClickListener(v -> {
            dialog.dismiss();
            if (parentDialog != null) {
                parentDialog.dismiss();
                showDownloadSettingsPanel();
            }
        });
        bottom.addView(close);
        box.addView(bottom);

        dialog.setContentView(scroll);
        dialog.show();
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9f);
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.78f);
            window.setAttributes(lp);
        }
    }


    private View advancedSwitchRow(String title, String desc, boolean enabled, View.OnClickListener listener) {
        return SettingsUi.advancedSwitchRow(this, title, desc, enabled, listener);
    }


    private View advancedInfoRow(String title) {
        return SettingsUi.advancedInfoRow(this, title);
    }


    private void showSpeedLimiterDialog(Dialog advancedDialog, Dialog parentDialog) {
        String[] labels = new String[]{"OFF", "256 KB/s", "512 KB/s", "1024 KB/s", "2048 KB/s"};
        int[] values = new int[]{0, 256, 512, 1024, 2048};
        int checked = 0;
        for (int i = 0; i < values.length; i++) if (values[i] == downloadSpeedLimitKBps) checked = i;

        new AlertDialog.Builder(this)
                .setTitle("Speed limiter")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    downloadSpeedLimitKBps = values[which];
                    saveSettings();
                    QuietToast.makeText(this, "Speed limiter: " + labels[which], QuietToast.LENGTH_SHORT).show();
                    d.dismiss();
                    if (advancedDialog != null) advancedDialog.dismiss();
                    showAdvancedDownloadFeaturesDialog(parentDialog);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showVideoControlsIfAllowed() {
        if (!videoControlsEnabled || videoControlsManualHidden || webView == null || webView.getVisibility() != View.VISIBLE) {
            return;
        }
        if (videoControlsBar != null) videoControlsBar.setVisibility(View.VISIBLE);
        if (videoSpeedLabel != null) videoSpeedLabel.setText(formatVideoSpeed(videoSpeed));
        if (videoQualityLabel != null) videoQualityLabel.setText(selectedVideoQuality == null ? "Auto" : selectedVideoQuality);
        updateVideoModeToggleButton();
        refreshVideoPlayPauseButtonState();
    }

    private LinearLayout createVideoControlsBar() {
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

    private View videoTextButton(String text, String label, View.OnClickListener listener) {
        return VideoUi.videoTextButton(this, text, label, listener);
    }

    private View videoButton(int iconRes, String label, View.OnClickListener listener) {
        LinearLayout wrap = VideoUi.videoButton(this, iconRes, label, listener);
        if ("Play/Pause".equals(label)) videoPlayPauseIcon = (ImageView) wrap.getChildAt(0);
        return wrap;
    }

    private void updateVideoPlayPauseButtonState(boolean playing) {
        try {
            if (videoPlayPauseIcon == null) return;
            videoPlayPauseIcon.setImageResource(playing ? R.drawable.ic_video_pause : R.drawable.ic_video_play);
            if (videoPlayPauseButton != null) {
                videoPlayPauseButton.setContentDescription(playing ? "Pause" : "Play");
            }
        } catch (Exception ignored) {
        }
    }

    private void refreshVideoPlayPauseButtonState() {
        try {
            if (webView == null || videoPlayPauseIcon == null) return;
            webView.evaluateJavascript("(function(){try{var v=document.querySelector('video');return !!(v&&!v.paused&&!v.ended);}catch(e){return false;}})();", value -> updateVideoPlayPauseButtonState("true".equals(value)));
        } catch (Exception ignored) {
        }
    }

    private void applyVideoControlsFullscreenLayout(boolean fullscreen) {
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

    private void nativeTapWebViewAtRatio(double xRatio, double yRatio) {
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

    private void updateVideoControlsVisibility() {
        if (videoControlsBar == null) return;
        // Jangan muncul hanya karena halaman punya video.
        // Kontrol baru muncul setelah JS mendeteksi video benar-benar play/playing.
        videoControlsBar.setVisibility(View.GONE);
        videoControlsManualHidden = false;
        injectVideoPlaybackWatcher();
    }

    private void seekVideoBySeconds(int seconds) {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            QuietToast.makeText(this, "Buka video dulu", QuietToast.LENGTH_SHORT).show();
            return;
        }
        String js = "javascript:(function(){var v=document.querySelector('video');if(v){try{v.currentTime=Math.max(0,Math.min((v.duration||999999),v.currentTime+" + seconds + "));}catch(e){} }})()";
        runPageScript(js);
        refreshVideoPlayPauseButtonState();
    }

    private void checkAndShowVideoControls() {
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

    private void toggleVideoFullLandscapeButton() {
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

    private void updateVideoModeToggleButton() {
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
    private boolean isVideoFullscreenActive() {
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

    private void exitVideoFullscreenToPortraitMode() {
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
    private void enterVideoFullscreen() {
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

    private void openAppVideoFullscreenFallback() {
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
    private void restoreAfterVideoFullscreen() {
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

    private void forcePortraitAfterVideoFullscreen() {
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } catch (Exception ignored) {
        }
    }

    private void moveVideoControlsToFullscreenOverlay() {
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

    private void restoreVideoControlsFromFullscreenOverlay() {
        try {
            if (videoControlsBar == null || !videoControlsInFullscreen) return;

            ViewGroup currentParent = (ViewGroup) videoControlsBar.getParent();
            if (currentParent != null) currentParent.removeView(videoControlsBar);

            if (videoControlsOriginalParent != null) {
                int index = videoControlsOriginalIndex;
                if (index < 0 || index > videoControlsOriginalParent.getChildCount()) {
                    index = videoControlsOriginalParent.getChildCount();
                }
                if (videoControlsOriginalLayoutParams != null) {
                    videoControlsOriginalParent.addView(videoControlsBar, index, videoControlsOriginalLayoutParams);
                } else {
                    videoControlsOriginalParent.addView(videoControlsBar, index);
                }
            }

            videoControlsBar.setBackgroundColor(Color.parseColor("#101217"));
            applyVideoControlsFullscreenLayout(false);
            videoControlsInFullscreen = false;
            updateVideoModeToggleButton();
            videoControlsOriginalParent = null;
            videoControlsOriginalLayoutParams = null;
            videoControlsOriginalIndex = -1;
        } catch (Exception ignored) {
        }
    }
    private void showVideoOptimizationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setBackground(roundRect(Color.parseColor("#26292F"), dp(24), dp(1), COLOR_BORDER));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(14));
        scroll.addView(box, new ScrollView.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("Optimasi Video");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText("Fitur ringan untuk streaming. Yang sudah otomatis tidak digandakan agar tidak crash.");
        info.setTextColor(COLOR_SUBTEXT);
        info.setTextSize(13);
        info.setLineSpacing(0, 1.05f);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, -2);
        infoLp.setMargins(0, dp(8), 0, dp(12));
        box.addView(info, infoLp);

        box.addView(videoOptSwitchRow("Video preload / buffer booster", "Memaksa preload auto dan cache lebih agresif saat ada video.", videoBufferBooster, v -> {
            videoBufferBooster = !videoBufferBooster;
            applyBrowserSettings();
            injectVideoOptimizationIfNeeded();
            saveSettings();
        }));

        box.addView(videoOptSwitchRow("HLS segment prefetch", "Mendeteksi m3u8 dan mencoba prefetch ringan segmen berikutnya.", hlsSegmentPrefetch, v -> {
            hlsSegmentPrefetch = !hlsSegmentPrefetch;
            injectVideoOptimizationIfNeeded();
            saveSettings();
        }));

        box.addView(videoOptSwitchRow("Minimize normal / tanpa floating", "Saat tombol Home/Recent Android ditekan, Yield tidak jadi jendela melayang dan akan balik ke tampilan terakhir.", !videoFloatingPlayer, v -> {
            videoFloatingPlayer = false;
            saveSettings();
            QuietToast.makeText(this, "Minimize normal aktif", QuietToast.LENGTH_SHORT).show();
        }));

        box.addView(videoOptSwitchRow("Background play ringan", "Video tidak dipaksa pause saat aplikasi masuk background jika WebView mendukung.", videoBackgroundPlay, v -> {
            videoBackgroundPlay = !videoBackgroundPlay;
            applyBrowserSettings();
            injectVideoOptimizationIfNeeded();
            saveSettings();
        }));

        box.addView(videoOptInfoRow("Auto detect kualitas video", "Sudah aktif di tombol kualitas 240p–720p; tidak dibuat dobel."));
        box.addView(videoOptInfoRow("Download kualitas 240p–720p", "Diperkuat via deteksi source/player. Situs yang menyembunyikan URL tetap mengikuti batas player."));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.END);
        bottom.setPadding(0, dp(8), 0, 0);

        TextView close = dialogTextButton("TUTUP");
        close.setOnClickListener(v -> dialog.dismiss());
        bottom.addView(close);
        box.addView(bottom);

        dialog.setContentView(scroll);
        dialog.show();
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
    }


    private View videoOptSwitchRow(String title, String desc, boolean enabled, View.OnClickListener listener) {
        return SettingsUi.videoOptSwitchRow(this, title, desc, enabled, listener);
    }


    private View videoOptInfoRow(String title, String desc) {
        return SettingsUi.videoOptInfoRow(this, title, desc);
    }

    private void detectVideoQualities() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) return;
        try {
            webView.evaluateJavascript("(function(){try{return (window.__yieldVideoQualities||[]).join(', ');}catch(e){return '';}})()", value -> {
                String result = value == null ? "" : value.replace("\"", "");
                if (result.length() > 0) {
                    QuietToast.makeText(this, "Kualitas terdeteksi: " + result + "p", QuietToast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void showVideoQualityDialog() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            QuietToast.makeText(this, "Buka video dulu", QuietToast.LENGTH_SHORT).show();
            return;
        }

        injectVideoOptimizationIfNeeded();
        detectVideoQualities();

        String[] labels = new String[]{"Auto", "240p", "360p", "480p", "720p"};
        int checked = 0;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(selectedVideoQuality)) {
                checked = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Kualitas video")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    setVideoQuality(labels[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void setVideoQuality(String quality) {
        if (quality == null) quality = "Auto";
        selectedVideoQuality = quality;
        if (videoQualityLabel != null) videoQualityLabel.setText(selectedVideoQuality);
        saveSettings();

        if ("Auto".equals(quality)) {
            try {
                webView.evaluateJavascript("(function(){try{if(window.videojs){var ps=window.videojs.getPlayers?window.videojs.getPlayers():{};for(var k in ps){var p=ps[k];if(p&&p.qualityLevels){var ql=p.qualityLevels();for(var i=0;i<ql.length;i++)ql[i].enabled=true;}}}return 'auto';}catch(e){return 'auto_err';}})()", null);
            } catch (Exception ignored) {}
            QuietToast.makeText(this, "Kualitas video: Auto", QuietToast.LENGTH_SHORT).show();
            return;
        }

        String target = quality.replace("p", "");
        String js =
                "(function(){"
                        + "try{"
                        + "var target='" + target + "';"
                        + "var video=null;"
                        + "var videos=document.querySelectorAll('video');"
                        + "for(var i=0;i<videos.length;i++){var r=videos[i].getBoundingClientRect();if(r.width>60&&r.height>40){video=videos[i];break;}}"
                        + "if(!video&&videos.length>0)video=videos[0];"
                        + "if(!video)return 'no_video';"
                        + "var current=0;try{current=video.currentTime||0;}catch(e){}"
                        + "var paused=true;try{paused=video.paused;}catch(e){}"
                        + "function hasQ(s){s=(s||'').toLowerCase();return s.indexOf(target+'p')>-1||s.indexOf(target+'_')>-1||s.indexOf(target+'-')>-1||s.indexOf(target+'.')>-1||s.indexOf('height='+target)>-1||s.indexOf('res='+target)>-1||s.indexOf('quality='+target)>-1;}"
                        + "function switchSrc(src,type){try{if(!src)return false;video.pause();video.src=src;if(type)video.type=type;video.load();video.addEventListener('loadedmetadata',function(){try{video.currentTime=current;if(!paused)video.play();}catch(e){}},{once:true});return true;}catch(e){return false;}}"
                        + "var sources=video.querySelectorAll('source[src]');"
                        + "for(var s=0;s<sources.length;s++){var src=sources[s].src||sources[s].getAttribute('src')||'';var label=(sources[s].getAttribute('label')||sources[s].getAttribute('res')||sources[s].getAttribute('data-quality')||'');if(hasQ(src)||hasQ(label)){if(switchSrc(src,sources[s].type||''))return 'changed_source';}}"
                        + "var vsrc=video.currentSrc||video.src||'';"
                        + "if(hasQ(vsrc))return 'already';"
                        + "try{if(window.jwplayer){var j=window.jwplayer();var levels=j.getQualityLevels?j.getQualityLevels():[];for(var jx=0;jx<levels.length;jx++){var q=(levels[jx].label||levels[jx].height||'')+'';if(hasQ(q)){j.setCurrentQuality(jx);return 'changed_jwplayer';}}}}catch(e){}"
                        + "try{if(window.videojs){var players=window.videojs.getPlayers?window.videojs.getPlayers():{};for(var k in players){var p=players[k];if(p&&p.qualityLevels){var ql=p.qualityLevels();for(var qi=0;qi<ql.length;qi++){var h=(ql[qi].height||'')+'';if(hasQ(h)){for(var qj=0;qj<ql.length;qj++)ql[qj].enabled=false;ql[qi].enabled=true;return 'changed_videojs';}}}}}}catch(e){}"
                        + "var clickables=document.querySelectorAll('button,[role=button],li,a,span,div');"
                        + "for(var c=0;c<clickables.length&&c<800;c++){var t=(clickables[c].innerText||clickables[c].textContent||clickables[c].getAttribute('aria-label')||'').trim();if(hasQ(t)&&t.length<40){try{clickables[c].click();return 'clicked_quality';}catch(e){}}}"
                        + "return 'not_found';"
                        + "}catch(e){return 'error';}"
                        + "})()";

        try {
            webView.evaluateJavascript(js, value -> {
                String result = value == null ? "" : value.replace("\"", "");
                if (result.contains("changed") || result.contains("clicked") || result.contains("already")) {
                    QuietToast.makeText(this, "Kualitas video: " + selectedVideoQuality, QuietToast.LENGTH_SHORT).show();
                } else if (result.contains("no_video")) {
                    QuietToast.makeText(this, "Video belum ditemukan", QuietToast.LENGTH_SHORT).show();
                } else {
                    QuietToast.makeText(this, "Kualitas " + selectedVideoQuality + " tidak tersedia di player ini", QuietToast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            QuietToast.makeText(this, "Gagal mengubah kualitas video", QuietToast.LENGTH_SHORT).show();
        }
    }

    private void showVideoSpeedDialog() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            QuietToast.makeText(this, "Buka video dulu", QuietToast.LENGTH_SHORT).show();
            return;
        }

        String[] speeds = new String[]{"0.5x", "1x", "1.25x", "1.5x", "2x"};
        float[] values = new float[]{0.5f, 1.0f, 1.25f, 1.5f, 2.0f};
        int checked = 1;
        for (int i = 0; i < values.length; i++) {
            if (Math.abs(values[i] - videoSpeed) < 0.01f) checked = i;
        }

        new AlertDialog.Builder(this)
                .setTitle("Kecepatan video")
                .setSingleChoiceItems(speeds, checked, (dialog, which) -> {
                    setVideoSpeed(values[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void setVideoSpeed(float speed) {
        videoSpeed = speed;
        if (videoSpeedLabel != null) {
            videoSpeedLabel.setText(formatVideoSpeed(videoSpeed));
        }
        String speedText = String.valueOf(videoSpeed);
        webView.loadUrl("javascript:(function(){var v=document.querySelector('video');if(v){v.playbackRate=" + speedText + ";}})()");
        saveSettings();
        QuietToast.makeText(this, "Speed video: " + formatVideoSpeed(videoSpeed), QuietToast.LENGTH_SHORT).show();
    }

    private void injectVideoPlaybackWatcher() {
        if (webView == null) return;
        if (isSiteCompatibilityModeActiveForUrl(getEffectiveCurrentUrl())) return;
        boolean youtubePage = isYouTubePlaybackUrl(getEffectiveCurrentUrl());
        String js = "javascript:(function(){"
                + "window.__yieldVideoWatcherInstalled=true;"
                + "var Y_BUF=" + (!youtubePage && videoBufferBooster ? "true" : "false") + ",Y_HLS=" + (!youtubePage && hlsSegmentPrefetch ? "true" : "false") + ",Y_BG=" + (!youtubePage && videoBackgroundPlay ? "true" : "false") + ";"
                + "function qOf(s){s=(s||'').toLowerCase();var m=s.match(/(240|360|480|720|1080)p|height=(240|360|480|720|1080)|res=(240|360|480|720|1080)|quality=(240|360|480|720|1080)/);return m?(m[1]||m[2]||m[3]||m[4]||''):'';}"
                + "function collectQ(v){try{var out=[];var ss=v.querySelectorAll('source[src]');for(var i=0;i<ss.length;i++){var q=qOf((ss[i].src||'')+' '+(ss[i].getAttribute('label')||'')+' '+(ss[i].getAttribute('res')||''));if(q&&out.indexOf(q)<0)out.push(q);}try{if(window.jwplayer){var j=window.jwplayer();var lv=j.getQualityLevels?j.getQualityLevels():[];for(var a=0;a<lv.length;a++){var q=(lv[a].height||lv[a].label||'')+'';q=q.replace(/[^0-9]/g,'');if(q&&out.indexOf(q)<0)out.push(q);}}}catch(e){}try{if(window.videojs){var ps=window.videojs.getPlayers?window.videojs.getPlayers():{};for(var k in ps){var p=ps[k];if(p&&p.qualityLevels){var ql=p.qualityLevels();for(var b=0;b<ql.length;b++){var h=(ql[b].height||'')+'';if(h&&out.indexOf(h)<0)out.push(h);}}}}}catch(e){}if(out.length){window.__yieldVideoQualities=out.sort(function(a,b){return parseInt(a)-parseInt(b);});}}catch(e){}}"
                + "function prefetch(u){try{if(!Y_HLS||!u||window.__yieldPrefetch&&window.__yieldPrefetch[u])return;if(!window.__yieldPrefetch)window.__yieldPrefetch={};window.__yieldPrefetch[u]=true;fetch(u,{method:'GET',mode:'no-cors',cache:'force-cache'}).catch(function(){});}catch(e){}}"
                + "function hlsProbe(v){try{if(!Y_HLS)return;var u=(v.currentSrc||v.src||'');if(u&&u.indexOf('.m3u8')>-1)prefetch(u);document.querySelectorAll('source[src]').forEach(function(s){var x=s.src||'';if(x.indexOf('.m3u8')>-1)prefetch(x);});performance.getEntriesByType('resource').slice(-40).forEach(function(r){var x=r.name||'';if(x.indexOf('.m3u8')>-1||x.indexOf('.ts')>-1||x.indexOf('.m4s')>-1)prefetch(x);});}catch(e){}}"
                + "function boost(v){try{if(Y_BUF){v.preload='auto';v.setAttribute('preload','auto');try{v.load();}catch(e){}}if(Y_BG){document.addEventListener('visibilitychange',function(){try{if(document.hidden&&v&&!v.paused){setTimeout(function(){try{v.play();}catch(e){}},250);}}catch(e){}},true);}}catch(e){}}"
                + "function hook(v){"
                + " if(!v||v.__yieldHooked)return;"
                + " v.__yieldHooked=true;"
                + " boost(v);collectQ(v);hlsProbe(v);"
                + " var show=function(){try{collectQ(v);hlsProbe(v);if(window.YieldVideoBridge)YieldVideoBridge.onVideoPlaying();}catch(e){}};"
                + " var tap=function(){try{if(window.YieldVideoBridge)YieldVideoBridge.onVideoTapped();}catch(e){}};"
                + " var hide=function(){try{if(window.YieldVideoBridge)YieldVideoBridge.onVideoStopped();}catch(e){}};"
                + " v.addEventListener('play',show,true);"
                + " v.addEventListener('playing',show,true);"
                + " v.addEventListener('loadedmetadata',show,true);"
                + " v.addEventListener('loadeddata',show,true);"
                + " v.addEventListener('canplay',show,true);"
                + " v.addEventListener('click',tap,true);"
                + " v.addEventListener('touchend',tap,true);"
                + " v.addEventListener('pause',hide,true);"
                + " v.addEventListener('ended',hide,true);"
                + " if(!v.paused&&!v.ended&&v.readyState>2)show();"
                + "}"
                + "function scan(){try{var vs=document.querySelectorAll('video');for(var i=0;i<vs.length;i++){hook(vs[i]);}}catch(e){}}"
                + "scan();setInterval(scan,2500);"
                + "try{new MutationObserver(scan).observe(document.documentElement,{childList:true,subtree:true});}catch(e){}"
                + "})()";
        try {
            runPageScript(js);
        } catch (Exception ignored) {}
    }

    private void injectVideoOptimizationIfNeeded() {
        injectVideoPlaybackWatcher();
    }

    private void controlVideo(String action) {
        injectVideoPlaybackWatcher();
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            QuietToast.makeText(this, "Buka video dulu", QuietToast.LENGTH_SHORT).show();
            return;
        }

        String js;
        if ("play".equals(action)) {
            js = "(function(){var v=document.querySelector('video');if(v){v.play();'play';}else{'no_video';}})()";
        } else if ("pause".equals(action)) {
            js = "(function(){var v=document.querySelector('video');if(v){v.pause();'pause';}else{'no_video';}})()";
        } else if ("toggle".equals(action)) {
            js = "(function(){try{var v=document.querySelector('video');if(!v)return 'no_video';if(v.paused||v.ended){v.play();return 'play';}else{v.pause();return 'pause';}}catch(e){return 'error';}})()";
        } else {
            js = "(function(){var v=document.querySelector('video');if(v){v.pause();try{v.currentTime=0;}catch(e){}'stop';}else{'no_video';}})()";
        }
        try {
            webView.evaluateJavascript(js, value -> refreshVideoPlayPauseButtonState());
        } catch (Exception e) {
            runPageScript("javascript:" + js);
            refreshVideoPlayPauseButtonState();
        }
    }
    private String formatVideoSpeed(float speed) {
        return VideoUi.formatVideoSpeed(speed);
    }

    private void showHistoryPanel() {
        if (dedicatedPrivateProfile || isCurrentPrivateTab()) {
            QuietToast.makeText(this, "Riwayat tidak disimpan dalam mode Privat",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }
        initializeHistoryEngineV2();
        recordCurrentPageToHistory();

        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(18), dp(18), dp(18), dp(10));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Riwayat");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        ImageButton close = plainIconButton(R.drawable.ic_exit, v -> dialog.dismiss());
        top.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        root.addView(top);

        EditText searchInput = darkSearchInput("Cari judul, situs, atau alamat");
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(50));
        searchParams.setMargins(0, dp(14), 0, dp(10));
        root.addView(searchInput, searchParams);

        TextView clearText = new TextView(this);
        clearText.setText("Hapus semua riwayat");
        clearText.setTextColor(Color.parseColor("#F97352"));
        clearText.setTextSize(14);
        clearText.setTypeface(Typeface.DEFAULT_BOLD);
        clearText.setGravity(Gravity.CENTER_VERTICAL);
        clearText.setPadding(0, dp(4), 0, dp(10));
        root.addView(clearText, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout listFrame = new FrameLayout(this);
        RecyclerView recycler = new RecyclerView(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recycler.setLayoutManager(layoutManager);
        recycler.setItemAnimator(null);
        recycler.setHasFixedSize(false);
        recycler.setClipToPadding(false);
        recycler.setPadding(0, 0, 0, dp(18));

        TextView empty = new TextView(this);
        empty.setText("Riwayat masih kosong.");
        empty.setTextColor(COLOR_SUBTEXT);
        empty.setTextSize(16);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);

        ProgressBar loading = new ProgressBar(this);
        loading.setIndeterminate(true);
        loading.setVisibility(View.GONE);

        listFrame.addView(recycler, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams emptyParams = new FrameLayout.LayoutParams(-1, -1);
        emptyParams.gravity = Gravity.CENTER;
        listFrame.addView(empty, emptyParams);
        FrameLayout.LayoutParams loadingParams = new FrameLayout.LayoutParams(dp(44), dp(44));
        loadingParams.gravity = Gravity.CENTER;
        listFrame.addView(loading, loadingParams);
        root.addView(listFrame, new LinearLayout.LayoutParams(-1, 0, 1));

        HistoryListAdapter adapter = new HistoryListAdapter(this, new HistoryListAdapter.Listener() {
            @Override
            public void onOpen(HistoryItemData item) {
                if (item == null || item.url == null || item.url.trim().isEmpty()) return;
                dialog.dismiss();
                addressBar.setText(item.url);
                openAddressBarUrl();
            }

            @Override
            public void onDelete(HistoryItemData item) {
                if (item == null || historyRepository == null) return;
                if (activeHistoryAdapter != null) activeHistoryAdapter.removeById(item.id);
                updateActiveHistoryEmptyState();
                historyRepository.deleteById(item.id, success -> {
                    if (!success) {
                        QuietToast.makeText(MainActivity.this,
                                "Riwayat gagal dihapus. Daftar dimuat ulang.", QuietToast.LENGTH_SHORT).show();
                        resetActiveHistoryQuery(activeHistoryQuery);
                    }
                });
            }

            @Override
            public void onBindFavicon(String url, ImageView target, TextView fallback) {
                loadFavicon(url, target, fallback);
            }
        });
        recycler.setAdapter(adapter);

        activeHistoryDialog = dialog;
        activeHistoryAdapter = adapter;
        activeHistoryRecyclerView = recycler;
        activeHistoryEmptyView = empty;
        activeHistoryProgress = loading;

        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                int last = layoutManager.findLastVisibleItemPosition();
                if (HistoryPanelPresentation.shouldLoadNextPage(
                        dy,
                        activeHistoryLoading,
                        activeHistoryEndReached,
                        last,
                        adapter.getItemCount())) {
                    loadNextHistoryPage();
                }
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pendingHistorySearch != null) mainHandler.removeCallbacks(pendingHistorySearch);
                String query = HistoryPanelPresentation.normalizeQuery(
                        s == null ? "" : s.toString());
                pendingHistorySearch = () -> resetActiveHistoryQuery(query);
                mainHandler.postDelayed(pendingHistorySearch,
                        HistoryPanelPresentation.SEARCH_DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        clearText.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Hapus semua riwayat?")
                .setMessage("Riwayat browsing akan dihapus permanen. Tab yang sedang terbuka tidak ditutup.")
                .setPositiveButton("Hapus", (d, which) -> clearBrowserHistoryManually(() -> {
                    if (activeHistoryAdapter != null) activeHistoryAdapter.clear();
                    activeHistoryEndReached = true;
                    updateActiveHistoryEmptyState();
                }))
                .setNegativeButton("Batal", null)
                .show());

        dialog.setContentView(root);
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event != null
                    && event.getAction() == KeyEvent.ACTION_UP) {
                d.dismiss();
                return true;
            }
            return false;
        });
        dialog.setOnDismissListener(d -> {
            activeHistoryGeneration++;
            if (pendingHistorySearch != null) mainHandler.removeCallbacks(pendingHistorySearch);
            if (activeHistoryDialog == dialog) {
                activeHistoryDialog = null;
                activeHistoryAdapter = null;
                activeHistoryRecyclerView = null;
                activeHistoryEmptyView = null;
                activeHistoryProgress = null;
                activeHistoryLoading = false;
            }
        });
        dialog.show();
        applyDarkFullscreenDialog(dialog);
        resetActiveHistoryQuery("");
    }

    private void resetActiveHistoryQuery(String query) {
        if (activeHistoryDialog == null || !activeHistoryDialog.isShowing()) return;
        activeHistoryGeneration++;
        activeHistoryQuery = HistoryPanelPresentation.normalizeQuery(query);
        activeHistoryBeforeTime = Long.MAX_VALUE;
        activeHistoryBeforeId = Long.MAX_VALUE;
        activeHistoryEndReached = false;
        activeHistoryLoading = false;
        if (activeHistoryAdapter != null) activeHistoryAdapter.clear();
        if (activeHistoryRecyclerView != null) activeHistoryRecyclerView.scrollToPosition(0);
        updateActiveHistoryEmptyState();
        loadNextHistoryPage();
    }

    private void loadNextHistoryPage() {
        if (historyRepository == null || activeHistoryAdapter == null
                || activeHistoryDialog == null || !activeHistoryDialog.isShowing()
                || activeHistoryLoading || activeHistoryEndReached) return;

        activeHistoryLoading = true;
        int generation = activeHistoryGeneration;
        setActiveHistoryLoading(true);
        historyRepository.queryPage(
                activeHistoryQuery,
                activeHistoryBeforeTime,
                activeHistoryBeforeId,
                HISTORY_PAGE_SIZE,
                page -> {
                    if (generation != activeHistoryGeneration || activeHistoryDialog == null
                            || !activeHistoryDialog.isShowing() || activeHistoryAdapter == null) return;
                    activeHistoryLoading = false;
                    setActiveHistoryLoading(false);
                    if (page == null || page.isEmpty()) {
                        activeHistoryEndReached = true;
                        updateActiveHistoryEmptyState();
                        return;
                    }
                    activeHistoryAdapter.appendPage(page);
                    HistoryItemData last = page.get(page.size() - 1);
                    activeHistoryBeforeTime = last.time;
                    activeHistoryBeforeId = last.id;
                    activeHistoryEndReached = HistoryPanelPresentation.isEndReached(
                            page.size(), HISTORY_PAGE_SIZE);
                    updateActiveHistoryEmptyState();
                }
        );
    }

    private void setActiveHistoryLoading(boolean loading) {
        if (activeHistoryProgress == null) return;
        boolean adapterEmpty = activeHistoryAdapter == null || activeHistoryAdapter.isEmpty();
        activeHistoryProgress.setVisibility(
                HistoryPanelPresentation.shouldShowInitialLoading(loading, adapterEmpty)
                        ? View.VISIBLE : View.GONE);
    }

    private void updateActiveHistoryEmptyState() {
        if (activeHistoryEmptyView == null || activeHistoryAdapter == null) return;
        boolean adapterEmpty = activeHistoryAdapter.isEmpty();
        boolean show = HistoryPanelPresentation.shouldShowEmpty(
                activeHistoryLoading, adapterEmpty);
        activeHistoryEmptyView.setText(
                HistoryPanelPresentation.emptyMessage(activeHistoryQuery));
        activeHistoryEmptyView.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    private void applyDarkFullscreenDialog(Dialog dialog) {
        // Panel tetap full layar, tetapi status bar TETAP terlihat dengan background hitam.
        try {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                getWindow().setStatusBarColor(COLOR_BG);
                getWindow().setNavigationBarColor(Color.parseColor("#15171C"));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int activityFlags = getWindow().getDecorView().getSystemUiVisibility();
                activityFlags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    activityFlags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                getWindow().getDecorView().setSystemUiVisibility(activityFlags);
            }
        } catch (Exception ignored) {
        }

        Window window = dialog.getWindow();
        if (window == null) return;

        window.setBackgroundDrawable(new ColorDrawable(COLOR_BG));
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.gravity = Gravity.TOP;
        lp.dimAmount = 0f;
        window.setAttributes(lp);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(COLOR_BG);
            window.setNavigationBarColor(Color.parseColor("#15171C"));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = window.getDecorView().getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void showBookmarkHomePanel() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(18), dp(18), dp(18), dp(10));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        ImageButton back = plainIconButton(R.drawable.ic_back, v -> dialog.dismiss());
        top.addView(back, new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView title = new TextView(this);
        title.setText("Bookmark");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.setMargins(dp(8), 0, 0, 0);
        top.addView(title, titleParams);

        ImageButton filter = plainIconButton(R.drawable.ic_customize, v -> showBookmarkSortMenu(v));
        top.addView(filter, new LinearLayout.LayoutParams(dp(40), dp(40)));
        ImageButton addFolder = plainIconButton(R.drawable.ic_add_tab, v -> showCreateBookmarkFolderDialog(() -> switchDialogSmooth(dialog, () -> showBookmarkHomePanel())));
        top.addView(addFolder, new LinearLayout.LayoutParams(dp(40), dp(40)));
        ImageButton close = plainIconButton(R.drawable.ic_exit, v -> dialog.dismiss());
        top.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        ImageButton more = plainIconButton(R.drawable.ic_menu_more, v -> showBookmarkMainMenu(v, dialog));
        top.addView(more, new LinearLayout.LayoutParams(dp(40), dp(40)));
        root.addView(top);

        EditText search = darkSearchInput("Telusuri bookmark");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(54));
        sp.setMargins(0, dp(14), 0, dp(16));
        root.addView(search, sp);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        java.util.ArrayList<String> folders = new java.util.ArrayList<>(getBookmarkFolders());
        for (String folder : folders) {
            list.addView(bookmarkFolderRow(folder, countBookmarksInFolder(folder), dialog));
        }

        dialog.setContentView(root);
        dialog.show();
        applyDarkFullscreenDialog(dialog);
    }

    private void showBookmarkFolderPanel(String folder) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(18), dp(18), dp(18), dp(10));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        ImageButton back = plainIconButton(R.drawable.ic_back, v -> switchDialogSmooth(dialog, () -> showBookmarkHomePanel()));
        top.addView(back, new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView title = new TextView(this);
        title.setText(folder);
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.setMargins(dp(8), 0, 0, 0);
        top.addView(title, titleParams);

        ImageButton filter = plainIconButton(R.drawable.ic_customize, v -> showBookmarkSortMenu(v));
        top.addView(filter, new LinearLayout.LayoutParams(dp(40), dp(40)));
        ImageButton addFolder = plainIconButton(R.drawable.ic_add_tab, v -> showCreateBookmarkFolderDialog(() -> switchDialogSmooth(dialog, () -> showBookmarkHomePanel())));
        top.addView(addFolder, new LinearLayout.LayoutParams(dp(40), dp(40)));
        ImageButton close = plainIconButton(R.drawable.ic_exit, v -> dialog.dismiss());
        top.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        ImageButton more = plainIconButton(R.drawable.ic_menu_more, v -> showBookmarkMainMenu(v, dialog));
        top.addView(more, new LinearLayout.LayoutParams(dp(40), dp(40)));
        root.addView(top);

        EditText search = darkSearchInput("Telusuri bookmark");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(54));
        sp.setMargins(0, dp(14), 0, dp(12));
        root.addView(search, sp);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        final Runnable[] renderRef = new Runnable[1];
        renderRef[0] = () -> {
            list.removeAllViews();
            String q = search.getText().toString().trim().toLowerCase(Locale.US);
            java.util.ArrayList<BookmarkItemData> items = new java.util.ArrayList<>();
            for (BookmarkItemData item : bookmarkData) {
                if (folder.equals(item.folder)) {
                    String hay = ((item.title == null ? "" : item.title) + " " + item.url).toLowerCase(Locale.US);
                    if (q.length() == 0 || hay.contains(q)) items.add(item);
                }
            }
            if (items.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("Belum ada bookmark di folder ini.");
                empty.setTextColor(COLOR_SUBTEXT);
                empty.setTextSize(16);
                empty.setPadding(0, dp(20), 0, 0);
                list.addView(empty);
            } else {
                for (BookmarkItemData item : items) list.addView(bookmarkItemRow(item, dialog, renderRef[0]));
            }
        };
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){ if (renderRef[0] != null) renderRef[0].run(); }
            public void afterTextChanged(android.text.Editable s){}
        });
        renderRef[0].run();

        dialog.setContentView(root);
        dialog.show();
        applyDarkFullscreenDialog(dialog);
    }

    private View bookmarkFolderRow(String folder, int count, Dialog parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(14), 0, dp(14));
        row.setOnClickListener(v -> switchDialogSmooth(parent, () -> showBookmarkFolderPanel(folder)));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_folder);
        icon.setColorFilter(Color.parseColor("#5B2A1F"));
        icon.setPadding(dp(12), dp(12), dp(12), dp(12));
        icon.setBackground(roundRect(Color.parseColor("#E6E8EF"), dp(28), 0, Color.TRANSPARENT));
        row.addView(icon, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1);
        tp.setMargins(dp(14), 0, 0, 0);
        TextView t = new TextView(this);
        t.setText(folder + " (" + count + ")");
        t.setTextColor(Color.WHITE);
        t.setTextSize(17);
        texts.addView(t);
        row.addView(texts, tp);
        return row;
    }

    private View bookmarkItemRow(BookmarkItemData item, Dialog dialog, Runnable refresh) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setOnClickListener(v -> { dialog.dismiss(); addressBar.setText(item.url); openAddressBarUrl(); });

        FrameLayout iconWrap = new FrameLayout(this);
        TextView fallback = circleLetter(bookmarkInitial(item), Color.parseColor("#1F232A"), Color.WHITE);
        iconWrap.addView(fallback, new FrameLayout.LayoutParams(-1, -1));
        ImageView favicon = new ImageView(this);
        favicon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        favicon.setPadding(dp(9), dp(9), dp(9), dp(9));
        favicon.setBackground(roundRect(Color.parseColor("#2A2D33"), dp(23), 0, Color.TRANSPARENT));
        favicon.setVisibility(View.GONE);
        iconWrap.addView(favicon, new FrameLayout.LayoutParams(-1, -1));
        loadFavicon(item.url, favicon, fallback);
        row.addView(iconWrap, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1);
        tp.setMargins(dp(14), 0, dp(8), 0);
        TextView title = new TextView(this);
        title.setText(item.title == null || item.title.length() == 0 ? item.url : item.title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setSingleLine(true);
        texts.addView(title);
        TextView url = new TextView(this);
        url.setText(shortHost(item.url));
        url.setTextColor(COLOR_SUBTEXT);
        url.setTextSize(13);
        url.setSingleLine(true);
        texts.addView(url);
        row.addView(texts, tp);

        TextView more = new TextView(this);
        more.setText("⋮");
        more.setTextColor(Color.parseColor("#D3D8E1"));
        more.setTextSize(22);
        more.setGravity(Gravity.CENTER);
        more.setOnClickListener(v -> showBookmarkItemMenu(v, item, dialog, refresh));
        row.addView(more, new LinearLayout.LayoutParams(dp(36), dp(36)));
        return row;
    }

    private void showBookmarkItemMenu(View anchor, BookmarkItemData item, Dialog dialog, Runnable refresh) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Pilih");
        menu.getMenu().add("Edit");
        menu.getMenu().add("Salin link");
        menu.getMenu().add("Pindahkan ke...");
        menu.getMenu().add("Hapus");
        menu.getMenu().add("Berpindah ke atas");
        menu.getMenu().add("Buka di tab baru");
        menu.getMenu().add("Buka di tab Privat");
        menu.setOnMenuItemClickListener(mi -> {
            String t = String.valueOf(mi.getTitle());
            if ("Pilih".equals(t)) {
                QuietToast.makeText(this, "Mode pilih bookmark aktif", QuietToast.LENGTH_SHORT).show();
            } else if ("Edit".equals(t)) {
                showEditBookmarkDialog(item, dialog, refresh);
            } else if ("Salin link".equals(t)) {
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cb != null) cb.setPrimaryClip(ClipData.newPlainText("bookmark", item.url));
                QuietToast.makeText(this, "Link bookmark disalin", QuietToast.LENGTH_SHORT).show();
            } else if ("Pindahkan ke...".equals(t)) {
                showMoveBookmarkDialog(item, dialog, refresh);
            } else if ("Hapus".equals(t)) {
                bookmarkData.remove(item);
                saveBookmarkData();
                refreshDialogSmooth(dialog, refresh);
            } else if ("Berpindah ke atas".equals(t)) {
                bookmarkData.remove(item);
                bookmarkData.add(0, item);
                saveBookmarkData();
                refreshDialogSmooth(dialog, refresh);
            } else if ("Buka di tab baru".equals(t)) {
                dialog.dismiss();
                newTabInCurrentProfile();
                addressBar.setText(item.url);
                openAddressBarUrl();
            } else if ("Buka di tab Privat".equals(t)) {
                dialog.dismiss();
                openUrlInPrivateSpace(item.url);
            }
            return true;
        });
        menu.show();
    }

    private void showEditBookmarkDialog(BookmarkItemData item, Dialog dialog, Runnable refresh) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(4), dp(8), 0);
        EditText title = new EditText(this);
        title.setHint("Judul");
        title.setText(item.title);
        box.addView(title);
        EditText url = new EditText(this);
        url.setHint("URL");
        url.setText(item.url);
        box.addView(url);
        new AlertDialog.Builder(this)
                .setTitle("Edit bookmark")
                .setView(box)
                .setPositiveButton("Simpan", (d,w) -> {
                    item.title = title.getText().toString().trim();
                    item.url = normalizeShortcutUrl(url.getText().toString().trim());
                    if (item.url == null) item.url = url.getText().toString().trim();
                    saveBookmarkData();
                    refreshDialogSmooth(dialog, refresh);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showMoveBookmarkDialog(BookmarkItemData item, Dialog dialog, Runnable refresh) {
        java.util.ArrayList<String> folders = new java.util.ArrayList<>(getBookmarkFolders());
        String[] arr = folders.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Pindahkan ke folder")
                .setItems(arr, (d,w) -> {
                    item.folder = arr[w];
                    saveBookmarkData();
                    refreshDialogSmooth(dialog, refresh);
                })
                .show();
    }

    private void showCreateBookmarkFolderDialog(Runnable onDone) {
        EditText input = new EditText(this);
        input.setHint("Nama folder");
        new AlertDialog.Builder(this)
                .setTitle("Folder bookmark baru")
                .setView(input)
                .setPositiveButton("Tambah", (d,w) -> {
                    String name = input.getText().toString().trim();
                    if (name.length() == 0) return;
                    java.util.Set<String> folders = new java.util.LinkedHashSet<>(getBookmarkFolders());
                    folders.add(name);
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putStringSet(KEY_BOOKMARK_FOLDERS, new java.util.LinkedHashSet<>(folders)).apply();
                    QuietToast.makeText(this, "Folder ditambahkan", QuietToast.LENGTH_SHORT).show();
                    if (onDone != null) onDone.run();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showBookmarkSortMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Urutkan terbaru");
        menu.getMenu().add("Urutkan A-Z");
        menu.setOnMenuItemClickListener(mi -> {
            String t = String.valueOf(mi.getTitle());
            if (t.contains("A-Z")) {
                java.util.Collections.sort(bookmarkData, (a,b) -> safeBookmarkTitle(a).compareToIgnoreCase(safeBookmarkTitle(b)));
            } else {
                java.util.Collections.sort(bookmarkData, (a,b) -> Long.compare(b.time, a.time));
            }
            saveBookmarkData();
            QuietToast.makeText(this, "Urutan bookmark diperbarui", QuietToast.LENGTH_SHORT).show();
            return true;
        });
        menu.show();
    }

    private void showBookmarkMainMenu(View anchor, Dialog dialog) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Pilih");
        menu.getMenu().add("Tutup");
        menu.setOnMenuItemClickListener(mi -> {
            if ("Tutup".equals(String.valueOf(mi.getTitle()))) dialog.dismiss();
            else QuietToast.makeText(this, "Mode pilih bookmark aktif", QuietToast.LENGTH_SHORT).show();
            return true;
        });
        menu.show();
    }

    private void switchDialogSmooth(Dialog currentDialog, Runnable openNext) {
        try {
            if (openNext != null) openNext.run();
        } catch (Exception e) {
            QuietToast.makeText(this, "Gagal membuka menu: " + e.getMessage(), QuietToast.LENGTH_SHORT).show();
        }

        if (currentDialog != null) {
            mainHandler.postDelayed(() -> {
                try {
                    if (currentDialog.isShowing()) currentDialog.dismiss();
                } catch (Exception ignored) {
                }
            }, 120);
        }
    }

    private void refreshDialogSmooth(Dialog dialog, Runnable render) {
        try {
            if (render != null) render.run();
        } catch (Exception ignored) {
        }
        if (dialog != null && !dialog.isShowing()) {
            try { dialog.show(); } catch (Exception ignored) {}
        }
    }

    private ImageButton plainIconButton(int iconRes, View.OnClickListener listener) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(iconRes);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setColorFilter(Color.parseColor("#E9EDF5"));
        b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        b.setOnClickListener(listener);
        return b;
    }

    private EditText darkSearchInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(Color.parseColor("#A5ACB8"));
        input.setTextColor(Color.WHITE);
        input.setSingleLine(true);
        input.setBackground(roundRect(Color.parseColor("#2A2C32"), dp(16), 0, Color.TRANSPARENT));
        input.setPadding(dp(18), 0, dp(18), 0);
        return input;
    }

    private TextView circleLetter(String text, int bg, int fg) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(fg);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(16);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(roundRect(bg, dp(23), 0, Color.TRANSPARENT));
        return tv;
    }
    private String safeBookmarkTitle(BookmarkItemData item) {
        return item.title == null || item.title.length() == 0 ? item.url : item.title;
    }

    private String bookmarkInitial(BookmarkItemData item) {
        String t = safeBookmarkTitle(item);
        return t == null || t.length() == 0 ? "B" : t.substring(0,1).toUpperCase(Locale.US);
    }

    private String historyInitial(HistoryItemData item) {
        String t = item.title == null || item.title.length() == 0 ? item.url : item.title;
        return t == null || t.length() == 0 ? "H" : t.substring(0,1).toUpperCase(Locale.US);
    }

    private String shortHost(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return url;
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception e) {
            return url;
        }
    }

    private String historyDayLabel(long timeMs) {
        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar item = java.util.Calendar.getInstance();
        item.setTimeInMillis(timeMs);
        java.util.Calendar yesterday = java.util.Calendar.getInstance();
        yesterday.add(java.util.Calendar.DAY_OF_YEAR, -1);
        String dateText = new SimpleDateFormat("dd MMM yyyy", Locale.US).format(new Date(timeMs));
        if (sameDay(now, item)) return "Hari ini - " + dateText;
        if (sameDay(yesterday, item)) return "Kemarin - " + dateText;
        return dateText;
    }

    private boolean sameDay(java.util.Calendar a, java.util.Calendar b) {
        return BrowserUtils.sameDay(a, b);
    }

    private void showFindInPageDialog() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            QuietToast.makeText(this, "Buka halaman dulu untuk mencari teks", QuietToast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), 0, dp(8), 0);

        EditText input = new EditText(this);
        input.setHint("Cari teks di halaman");
        input.setSingleLine(true);
        box.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Cari di halaman")
                .setView(box)
                .setPositiveButton("Cari", (dialog, which) -> {
                    String q = input.getText().toString().trim();
                    if (q.length() == 0) return;
                    webView.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
                        if (isDoneCounting) {
                            QuietToast.makeText(this, numberOfMatches + " hasil ditemukan", QuietToast.LENGTH_SHORT).show();
                        }
                    });
                    webView.findAllAsync(q);
                })
                .setNeutralButton("Berikutnya", (dialog, which) -> webView.findNext(true))
                .setNegativeButton("Tutup", null)
                .show();
    }

    private void shareCurrentPage() {
        String url = getEffectiveCurrentUrl();
        if (url == null || url.length() == 0) {
            QuietToast.makeText(this, "Belum ada halaman untuk dibagikan", QuietToast.LENGTH_SHORT).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(send, "Bagikan halaman"));
    }

    private void copyCurrentLink() {
        String url = getEffectiveCurrentUrl();
        if (url == null || url.length() == 0) {
            QuietToast.makeText(this, "Belum ada link untuk disalin", QuietToast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Yield Browser URL", url));
            QuietToast.makeText(this, "Link disalin", QuietToast.LENGTH_SHORT).show();
        }
    }

    private void showPageInfoDialog() {
        String url = getEffectiveCurrentUrl();
        String title = webView != null ? webView.getTitle() : "Yield Browser";
        if (url == null) url = "Home";
        new AlertDialog.Builder(this)
                .setTitle("Info halaman")
                .setMessage("Judul:\\n" + (title == null ? "-" : title) + "\\n\\nURL:\\n" + url)
                .setPositiveButton("Salin link", (dialog, which) -> copyCurrentLink())
                .setNegativeButton("Tutup", null)
                .show();
    }

    private void toggleFullscreenMode() {
        boolean fullscreen = topBarView != null && topBarView.getVisibility() == View.VISIBLE;
        if (fullscreen) {
            if (topBarView != null) topBarView.setVisibility(View.GONE);
            if (bottomNavView != null) bottomNavView.setVisibility(View.GONE);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            QuietToast.makeText(this, "Mode layar penuh aktif. Tekan Back untuk keluar.", QuietToast.LENGTH_LONG).show();
        } else {
            exitFullscreenMode();
        }
    }

    private void exitFullscreenMode() {
        if (topBarView != null) topBarView.setVisibility(View.VISIBLE);
        if (bottomNavView != null) bottomNavView.setVisibility(View.VISIBLE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    private void saveCurrentPageOffline() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            QuietToast.makeText(this, "Buka halaman dulu untuk disimpan", QuietToast.LENGTH_SHORT).show();
            return;
        }
        try {
            File dir = new File(getExternalFilesDir(null), "OfflinePages");
            if (!dir.exists()) dir.mkdirs();
            String safeName = "page_" + System.currentTimeMillis() + ".mht";
            File out = new File(dir, safeName);
            webView.saveWebArchive(out.getAbsolutePath());
            QuietToast.makeText(this, "Halaman disimpan: " + out.getName(), QuietToast.LENGTH_LONG).show();
        } catch (Exception e) {
            QuietToast.makeText(this, "Gagal menyimpan halaman: " + e.getMessage(), QuietToast.LENGTH_SHORT).show();
        }
    }

    private boolean shouldRecordHistoryUrl(String url) {
        if (url == null || url.length() == 0) return false;
        String cleanUrl = extractOriginalUrl(url);
        if (cleanUrl == null || cleanUrl.length() == 0) return false;
        return isHttpOrHttpsUrl(cleanUrl)
                && !isLikelyAdClickUrl(cleanUrl)
                && !isImageResourceUrl(cleanUrl)
                && !cleanUrl.startsWith("about:")
                && !cleanUrl.startsWith("javascript:");
    }

    private void initializeHistoryEngineV2() {
        if (dedicatedPrivateProfile) return;
        if (historyRepository == null) {
            historyRepository = HistoryRepository.getInstance(getApplicationContext());
        }

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_HISTORY_ENGINE_V2_INITIALIZED, false)) return;
        if (historyV2InitializationStarted) return;

        // The user requested a clean V2 start: legacy history is removed once and is never
        // migrated or dual-written. The completion marker is written only after SQLite confirms
        // the reset, so a process kill cannot leave a half-initialized engine marked as complete.
        historyV2InitializationStarted = true;
        historyClearLock = true;
        clearLegacyHistoryStorageNow();
        historyRepository.clearAll(success -> {
            historyV2InitializationStarted = false;
            historyClearLock = false;
            if (success) {
                prefs.edit().putBoolean(KEY_HISTORY_ENGINE_V2_INITIALIZED, true).apply();
                recordCurrentPageToHistory();
                if (activeHistoryDialog != null && activeHistoryDialog.isShowing()) {
                    mainHandler.postDelayed(() -> resetActiveHistoryQuery(activeHistoryQuery), 120L);
                }
            }
        });
    }

    private void clearLegacyHistoryStorageNow() {
        try {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .remove(KEY_BROWSER_HISTORY)
                    .remove(KEY_BROWSER_HISTORY_BACKUP)
                    .remove(KEY_BROWSER_HISTORY_V2)
                    .remove(KEY_BROWSER_HISTORY_V3)
                    .apply();
        } catch (Exception ignored) {
        }
        try {
            getSharedPreferences(PREFS_HISTORY_V2, MODE_PRIVATE).edit().clear().apply();
        } catch (Exception ignored) {
        }
        try {
            File file = new File(new File(getFilesDir(), HISTORY_V3_FOLDER), HISTORY_V3_PUBLIC_FILE);
            if (file.exists()) file.delete();
        } catch (Exception ignored) {
        }
        try {
            File file = new File(getFilesDir(), HISTORY_V3_FILE);
            if (file.exists()) file.delete();
        } catch (Exception ignored) {
        }
        try {
            File external = getExternalFilesDir(null);
            if (external != null) {
                File file = new File(new File(external, HISTORY_V3_FOLDER), HISTORY_V3_PUBLIC_FILE);
                if (file.exists()) file.delete();
            }
        } catch (Exception ignored) {
        }
        historyData.clear();
    }

    /**
     * History V2 records committed navigations directly. Re-scanning the complete WebView
     * back/forward stack is intentionally disabled because it created duplicate writes and UI
     * stalls in the legacy engine.
     */
    private void recordWebViewBackForwardHistory() {
        // Intentionally empty.
    }

    private void recordCurrentPageToHistory() {
        try {
            if (historyClearLock || dedicatedPrivateProfile || isCurrentPrivateTab()) return;
            if (webView == null) return;
            String url = webView.getUrl();
            if (shouldRecordHistoryUrl(url)) addBrowserHistory(webView.getTitle(), url);
        } catch (Exception ignored) {
        }
    }

    private void addBrowserHistory(String title, String url) {
        if (historyClearLock || dedicatedPrivateProfile || isCurrentPrivateTab()) return;
        if (!shouldRecordHistoryUrl(url)) return;
        initializeHistoryEngineV2();
        if (historyRepository == null) return;

        String cleanUrl = extractOriginalUrl(url);
        if (cleanUrl == null || cleanUrl.trim().isEmpty()) return;
        String safeTitle = title == null || title.trim().isEmpty() ? cleanUrl : title.trim();
        historyRepository.recordVisit(safeTitle, cleanUrl, System.currentTimeMillis());
    }

    /** Retained for old call sites; V2 never performs a blocking load on the UI thread. */
    private void loadBrowserHistory() {
        initializeHistoryEngineV2();
    }

    /** Retained for old call sites; every V2 write is persisted transactionally by SQLite. */
    private void saveBrowserHistory() {
        // No-op. HistoryRepository is the only source of truth.
    }

    private void clearBrowserHistoryManually(Runnable afterClear) {
        if (dedicatedPrivateProfile || isCurrentPrivateTab()) return;
        initializeHistoryEngineV2();
        if (historyRepository == null) return;

        historyClearLock = true;
        clearLegacyHistoryStorageNow();
        historyRepository.clearAll(success -> {
            historyClearLock = false;
            if (success) {
                lastSafeHttpUrl = "";
                try {
                    for (TabInfo tab : tabs) {
                        if (tab != null && tab.webView != null) {
                            tab.webView.clearHistory();
                            tab.webView.clearFormData();
                        }
                    }
                    if (webView != null) {
                        webView.clearHistory();
                        webView.clearFormData();
                    }
                } catch (Exception ignored) {
                }
                QuietToast.makeText(this, "Semua riwayat telah dihapus", QuietToast.LENGTH_SHORT).show();
                if (afterClear != null) afterClear.run();
            } else {
                QuietToast.makeText(this, "Riwayat gagal dihapus", QuietToast.LENGTH_SHORT).show();
                resetActiveHistoryQuery(activeHistoryQuery);
            }
        });
    }


    private void showTextZoomDialog(Dialog parentDialog) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setBackground(roundRect(Color.parseColor("#2B2D33"), dp(24), dp(1), Color.parseColor("#3A3D45")));

        TextView title = new TextView(this);
        title.setText("Ukuran teks");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Geser untuk mengatur ukuran teks halaman web.");
        desc.setTextColor(COLOR_SUBTEXT);
        desc.setTextSize(13);
        desc.setPadding(0, dp(8), 0, dp(14));
        panel.addView(desc);

        TextView percent = new TextView(this);
        percent.setText(textZoom + "%");
        percent.setTextColor(COLOR_ACCENT);
        percent.setTextSize(30);
        percent.setTypeface(Typeface.DEFAULT_BOLD);
        percent.setGravity(Gravity.CENTER);
        panel.addView(percent, new LinearLayout.LayoutParams(-1, -2));

        SeekBar slider = new SeekBar(this);
        slider.setMax(80); // 70% sampai 150%
        int current = textZoom;
        if (current < 70) current = 70;
        if (current > 150) current = 150;
        slider.setProgress(current - 70);
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(-1, dp(52));
        sliderParams.setMargins(0, dp(10), 0, dp(8));
        panel.addView(slider, sliderParams);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.HORIZONTAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);

        TextView small = new TextView(this);
        small.setText("70%");
        small.setTextColor(COLOR_SUBTEXT);
        small.setTextSize(12);
        labels.addView(small, new LinearLayout.LayoutParams(0, -2, 1));

        TextView normal = new TextView(this);
        normal.setText("100%");
        normal.setTextColor(COLOR_SUBTEXT);
        normal.setTextSize(12);
        normal.setGravity(Gravity.CENTER);
        labels.addView(normal, new LinearLayout.LayoutParams(0, -2, 1));

        TextView big = new TextView(this);
        big.setText("150%");
        big.setTextColor(COLOR_SUBTEXT);
        big.setTextSize(12);
        big.setGravity(Gravity.RIGHT);
        labels.addView(big, new LinearLayout.LayoutParams(0, -2, 1));
        panel.addView(labels);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams buttonRowParams = new LinearLayout.LayoutParams(-1, dp(52));
        buttonRowParams.setMargins(0, dp(18), 0, 0);

        TextView reset = new TextView(this);
        reset.setText("Reset");
        reset.setTextColor(Color.WHITE);
        reset.setGravity(Gravity.CENTER);
        reset.setTextSize(15);
        reset.setTypeface(Typeface.DEFAULT_BOLD);
        reset.setBackground(roundRect(Color.parseColor("#2A2E36"), dp(18), dp(1), COLOR_BORDER));
        buttons.addView(reset, new LinearLayout.LayoutParams(0, dp(46), 1));

        TextView save = new TextView(this);
        save.setText("Simpan");
        save.setTextColor(Color.parseColor("#111111"));
        save.setGravity(Gravity.CENTER);
        save.setTextSize(15);
        save.setTypeface(Typeface.DEFAULT_BOLD);
        save.setBackground(roundRect(COLOR_ACCENT, dp(18), 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        saveParams.setMargins(dp(10), 0, 0, 0);
        buttons.addView(save, saveParams);
        panel.addView(buttons, buttonRowParams);

        final int[] selectedZoom = new int[]{current};

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedZoom[0] = progress + 70;
                percent.setText(selectedZoom[0] + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        reset.setOnClickListener(v -> {
            selectedZoom[0] = 100;
            slider.setProgress(30);
            percent.setText("100%");
        });

        save.setOnClickListener(v -> {
            textZoom = selectedZoom[0];
            applyBrowserSettings();
            saveSettings();
            QuietToast.makeText(this, "Ukuran teks: " + textZoom + "%", QuietToast.LENGTH_SHORT).show();
            dialog.dismiss();
            parentDialog.dismiss();
            showSettingsPanel();
        });

        dialog.setContentView(panel);
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
        dialog.show();
    }

    private String getDownloadLocationText() {
        if (selectedDownloadTreeUri != null && selectedDownloadTreeUri.length() > 0) {
            return "Lokasi sekarang:\nFolder HP dipilih\n" + selectedDownloadTreeUri;
        }
        return "Lokasi sekarang:\nDefault: Download/Yield Browser\nStaging: " + getDownloadDirectory().getAbsolutePath();
    }

    private void showDownloadFolderDialog(Dialog parentDialog) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(20), dp(22), dp(16));
        box.setBackground(roundRect(Color.parseColor("#2B2D33"), dp(22), dp(1), COLOR_BORDER));

        TextView title = new TextView(this);
        title.setText("Folder unduhan");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText("Default hasil download masuk ke folder Download/Yield Browser. Folder app tetap dipakai sebagai staging agar download 2 koneksi tetap stabil.");
        info.setTextColor(COLOR_SUBTEXT);
        info.setTextSize(14);
        info.setLineSpacing(0, 1.08f);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, -2);
        infoLp.setMargins(0, dp(8), 0, dp(14));
        box.addView(info, infoLp);

        EditText input = new EditText(this);
        input.setText(downloadSubfolder);
        input.setSingleLine(true);
        input.setHint("Nama subfolder staging");
        input.setHintTextColor(Color.parseColor("#8D929C"));
        input.setTextColor(Color.WHITE);
        input.setTextSize(17);
        input.setSelectAllOnFocus(false);
        try {
            input.setBackgroundTintList(ColorStateList.valueOf(COLOR_ACCENT));
        } catch (Exception ignored) {}
        box.addView(input, new LinearLayout.LayoutParams(-1, -2));

        TextView current = new TextView(this);
        current.setText(getDownloadLocationText());
        current.setTextColor(COLOR_SUBTEXT);
        current.setTextSize(12);
        current.setLineSpacing(0, 1.05f);
        LinearLayout.LayoutParams curLp = new LinearLayout.LayoutParams(-1, -2);
        curLp.setMargins(0, dp(12), 0, dp(10));
        box.addView(current, curLp);

        TextView choose = darkDialogActionButton("PILIH FOLDER HP");
        choose.setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.length() == 0) value = "Download";
            value = value.replace("/", "-").replace("\\", "-");
            downloadSubfolder = value;
            saveSettings();
            dialog.dismiss();
            if (parentDialog != null) parentDialog.dismiss();
            chooseExternalDownloadFolder();
        });
        box.addView(choose, new LinearLayout.LayoutParams(-1, dp(46)));

        TextView reset = darkDialogActionButton("RESET DEFAULT\nDOWNLOAD/YIELD BROWSER");
        reset.setTextSize(12);
        reset.setSingleLine(false);
        reset.setMaxLines(2);
        reset.setLineSpacing(0, 0.92f);
        reset.setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.length() == 0) value = "Download";
            value = value.replace("/", "-").replace("\\", "-");
            downloadSubfolder = value;
            selectedDownloadTreeUri = "";
            saveSettings();
            QuietToast.makeText(this, "Default: Download/Yield Browser", QuietToast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (parentDialog != null) {
                parentDialog.dismiss();
                showDownloadSettingsPanel();
            }
        });
        box.addView(reset, new LinearLayout.LayoutParams(-1, dp(62)));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.END);
        bottom.setPadding(0, dp(8), 0, 0);

        TextView cancel = dialogTextButton("BATAL");
        cancel.setOnClickListener(v -> dialog.dismiss());
        bottom.addView(cancel);

        TextView save = dialogTextButton("SIMPAN");
        save.setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.length() == 0) value = "Download";
            value = value.replace("/", "-").replace("\\", "-");
            downloadSubfolder = value;
            saveSettings();
            QuietToast.makeText(this, "Subfolder staging disimpan", QuietToast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (parentDialog != null) {
                parentDialog.dismiss();
                showDownloadSettingsPanel();
            }
        });
        bottom.addView(save);

        box.addView(bottom);

        dialog.setContentView(box);
        dialog.show();
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
    }

    private TextView darkDialogActionButton(String text) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(13);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setSingleLine(false);
        btn.setMaxLines(2);
        btn.setLineSpacing(0, 0.95f);
        btn.setPadding(dp(12), dp(8), dp(12), dp(8));
        btn.setBackground(roundRect(Color.parseColor("#343740"), dp(14), dp(1), COLOR_BORDER));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(46));
        lp.setMargins(0, dp(6), 0, 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private void chooseExternalDownloadFolder() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            startActivityForResult(intent, REQ_PICK_DOWNLOAD_FOLDER);
        } catch (Exception e) {
            QuietToast.makeText(this, "Pemilih folder tidak tersedia", QuietToast.LENGTH_SHORT).show();
        }
    }


    private TextView sectionTitle(String text) {
        return SettingsUi.sectionTitle(this, text);
    }


    private View menuDivider() {
        return SettingsUi.menuDivider(this);
    }


    private View customizeToggleRow(int iconRes, String label, boolean enabled, View.OnClickListener listener) {
        return SettingsUi.customizeToggleRow(this, iconRes, label, enabled, listener);
    }


    private View settingRow(int iconRes, String title, String desc, boolean enabled, View.OnClickListener listener) {
        return SettingsUi.settingRow(this, iconRes, title, desc, enabled, listener);
    }


    private View actionRow(int iconRes, String title, String desc, View.OnClickListener listener) {
        return SettingsUi.actionRow(this, iconRes, title, desc, listener);
    }


    private LinearLayout baseSettingsRow(int iconRes, String title, String desc, View.OnClickListener listener) {
        return SettingsUi.baseSettingsRow(this, iconRes, title, desc, listener);
    }


    private View menuRow(int iconRes, String label, View.OnClickListener listener) {
        return SettingsUi.menuRow(this, iconRes, label, listener);
    }


    // ===== Download manager UI =====
    private void showDownloadManager() {
        if (activeDownloadDialog != null && activeDownloadDialog.isShowing()) {
            renderDownloadList();
            return;
        }

        Dialog dialog = new Dialog(this);
        activeDownloadDialog = dialog;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(10));
        root.setBackgroundColor(COLOR_BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        activeDownloadTitleView = new TextView(this);
        activeDownloadTitleView.setText("Unduhan");
        activeDownloadTitleView.setTextColor(Color.WHITE);
        activeDownloadTitleView.setTextSize(27);
        activeDownloadTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(activeDownloadTitleView, new LinearLayout.LayoutParams(0, dp(50), 1));

        ImageButton search = smallTopIcon(R.drawable.ic_search, "Cari unduhan", v -> showDownloadSearchDialog());
        header.addView(search, new LinearLayout.LayoutParams(dp(42), dp(42)));

        ImageButton settings = smallTopIcon(R.drawable.ic_settings, "Pengaturan unduhan", v -> showDownloadSettingsPanel());
        header.addView(settings, new LinearLayout.LayoutParams(dp(42), dp(42)));

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(Color.parseColor("#D7DAE0"));
        close.setTextSize(34);
        close.setGravity(Gravity.CENTER);
        close.setContentDescription("Tutup Download Manager");
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));
        root.addView(header);

        activeDownloadStorageView = new TextView(this);
        activeDownloadStorageView.setTextColor(COLOR_SUBTEXT);
        activeDownloadStorageView.setTextSize(12);
        activeDownloadStorageView.setPadding(0, 0, 0, dp(10));
        root.addView(activeDownloadStorageView);

        LinearLayout sectionTabs = new LinearLayout(this);
        sectionTabs.setOrientation(LinearLayout.HORIZONTAL);
        sectionTabs.setGravity(Gravity.CENTER_VERTICAL);
        sectionTabs.setPadding(0, dp(2), 0, dp(10));
        activeDownloadRunningTab = downloadSectionTab("Mengunduh");
        activeDownloadCompletedTab = downloadSectionTab("Selesai");
        sectionTabs.addView(activeDownloadRunningTab, new LinearLayout.LayoutParams(0, dp(42), 1));
        LinearLayout.LayoutParams completedTabParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        completedTabParams.setMargins(dp(8), 0, 0, 0);
        sectionTabs.addView(activeDownloadCompletedTab, completedTabParams);
        root.addView(sectionTabs);
        renderDownloadSectionTabs();

        HorizontalScrollView categoryScroll = new HorizontalScrollView(this);
        categoryScroll.setHorizontalScrollBarEnabled(false);
        activeDownloadCategoryPanel = new LinearLayout(this);
        activeDownloadCategoryPanel.setOrientation(LinearLayout.HORIZONTAL);
        activeDownloadCategoryPanel.setPadding(0, 0, 0, dp(8));
        categoryScroll.addView(activeDownloadCategoryPanel);
        root.addView(categoryScroll, new LinearLayout.LayoutParams(-1, -2));
        renderDownloadCategoryChips();

        activeDownloadControlsPanel = new LinearLayout(this);
        activeDownloadControlsPanel.setOrientation(LinearLayout.VERTICAL);
        root.addView(activeDownloadControlsPanel, new LinearLayout.LayoutParams(-1, -2));

        activeDownloadRecyclerView = new RecyclerView(this);
        activeDownloadRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        activeDownloadRecyclerView.setHasFixedSize(false);
        activeDownloadRecyclerView.setItemAnimator(null);
        activeDownloadRecyclerView.setClipToPadding(false);
        activeDownloadRecyclerView.setPadding(0, dp(8), 0, dp(18));
        activeDownloadAdapter = new DownloadListAdapter(new DownloadListAdapter.Callback() {
            @Override
            public void onRowClick(int downloadId, View anchor) {
                DownloadItem item = findDownloadItemById(downloadId);
                if (item == null) return;
                if (downloadSelectMode) {
                    toggleDownloadSelection(item);
                } else if (canPlayDownloadInsideYield(item)
                        && ("running".equals(item.status) || "paused".equals(item.status)
                        || "failed".equals(item.status) || "completed".equals(item.status)
                        || "verifying".equals(item.status) || "saving".equals(item.status))) {
                    playDownloadInsideYield(item);
                } else if ("completed".equals(item.status)) {
                    openDownloadedFile(item);
                } else if ("failed".equals(item.status) || "paused".equals(item.status)
                        || "queued".equals(item.status) || "running".equals(item.status)) {
                    showDownloadItemMenu(anchor, item);
                }
            }

            @Override
            public boolean onRowLongClick(int downloadId) {
                DownloadItem item = findDownloadItemById(downloadId);
                if (item == null) return false;
                downloadSelectMode = true;
                toggleDownloadSelection(item);
                return true;
            }

            @Override
            public void onPrimaryAction(int downloadId) {
                DownloadItem item = findDownloadItemById(downloadId);
                if (item != null) handleDownloadPrimaryAction(item);
            }

            @Override
            public void onMore(int downloadId, View anchor) {
                DownloadItem item = findDownloadItemById(downloadId);
                if (item != null) showDownloadItemMenu(anchor, item);
            }
        });
        activeDownloadRecyclerView.setAdapter(activeDownloadAdapter);

        FrameLayout listFrame = new FrameLayout(this);
        listFrame.addView(activeDownloadRecyclerView, new FrameLayout.LayoutParams(-1, -1));
        activeDownloadEmptyView = new TextView(this);
        activeDownloadEmptyView.setTextColor(COLOR_SUBTEXT);
        activeDownloadEmptyView.setTextSize(15);
        activeDownloadEmptyView.setGravity(Gravity.CENTER);
        activeDownloadEmptyView.setPadding(dp(20), dp(40), dp(20), dp(40));
        activeDownloadEmptyView.setVisibility(View.GONE);
        listFrame.addView(activeDownloadEmptyView, new FrameLayout.LayoutParams(-1, -1));
        root.addView(listFrame, new LinearLayout.LayoutParams(-1, 0, 1));

        dialog.setContentView(root);
        dialog.setOnDismissListener(d -> {
            downloadUiTickerRunning = false;
            mainHandler.removeCallbacks(downloadUiTicker);
            activeDownloadDialog = null;
            activeDownloadRecyclerView = null;
            activeDownloadAdapter = null;
            activeDownloadCategoryPanel = null;
            activeDownloadControlsPanel = null;
            activeDownloadTitleView = null;
            activeDownloadStorageView = null;
            activeDownloadRunningTab = null;
            activeDownloadCompletedTab = null;
            activeDownloadEmptyView = null;
            downloadSelectMode = false;
            selectedDownloadIds.clear();
            lastDownloadControlsSignature = "";
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.CENTER;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(lp);
        }

        renderDownloadList();
        downloadUiTickerRunning = true;
        mainHandler.removeCallbacks(downloadUiTicker);
        mainHandler.postDelayed(downloadUiTicker, getDownloadUiTickerDelayMs());
    }

    private TextView downloadSectionTab(String label) {
        return SettingsUi.downloadSectionTab(this, label, v -> {
            if (label.equals(activeDownloadSection)) return;
            activeDownloadSection = label;
            downloadSelectMode = false;
            selectedDownloadIds.clear();
            lastDownloadControlsSignature = "";
            renderDownloadSectionTabs();
            renderDownloadList();
        });
    }

    private void renderDownloadSectionTabs() {
        styleDownloadSectionTab(activeDownloadRunningTab, "Mengunduh".equals(activeDownloadSection));
        styleDownloadSectionTab(activeDownloadCompletedTab, "Selesai".equals(activeDownloadSection));
    }

    private void styleDownloadSectionTab(TextView tab, boolean selected) {
        if (tab == null) return;
        tab.setTextColor(selected ? Color.parseColor("#141414") : Color.WHITE);
        tab.setBackground(roundRect(selected ? COLOR_ACCENT : Color.parseColor("#20242B"),
                dp(17), dp(1), selected ? COLOR_ACCENT : COLOR_BORDER));
    }

    private void renderDownloadCategoryChips() {
        if (activeDownloadCategoryPanel == null) return;
        activeDownloadCategoryPanel.removeAllViews();
        String[] labels = {"Semua", "Video", "APK", "Dokumen", "Musik", "Lainnya"};
        for (String label : labels) activeDownloadCategoryPanel.addView(downloadCategoryChip(label));
    }

    private TextView downloadCategoryChip(String label) {
        boolean selected = label.equals(activeDownloadCategory);
        return SettingsUi.downloadCategoryChip(this, label, selected, v -> {
            activeDownloadCategory = label;
            selectedDownloadIds.clear();
            downloadSelectMode = false;
            lastDownloadControlsSignature = "";
            renderDownloadCategoryChips();
            renderDownloadList();
        });
    }

    private void showDownloadSearchDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(activeDownloadSearchQuery);
        input.setHint("Cari nama file atau sumber");
        input.setSelectAllOnFocus(true);

        new AlertDialog.Builder(this)
                .setTitle("Cari unduhan")
                .setView(input)
                .setPositiveButton("Cari", (dialog, which) -> {
                    activeDownloadSearchQuery = input.getText().toString().trim();
                    renderDownloadList();
                })
                .setNeutralButton("Reset", (dialog, which) -> {
                    activeDownloadSearchQuery = "";
                    renderDownloadList();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showDownloadSortDialog() {
        String[] options = {"Tanggal", "Antrian", "Nama", "Ukuran"};
        int checked = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(activeDownloadSort)) checked = i;
        }

        new AlertDialog.Builder(this)
                .setTitle("Urutkan unduhan")
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    activeDownloadSort = options[which];
                    lastDownloadControlsSignature = "";
                    dialog.dismiss();
                    renderDownloadList();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void renderDownloadList() {
        if (activeDownloadAdapter == null) return;
        ArrayList<DownloadItem> items = getFilteredDownloadItems();
        ArrayList<DownloadUiItem> snapshots = new ArrayList<>(items.size());
        for (DownloadItem item : items) snapshots.add(buildDownloadUiItem(item));
        activeDownloadAdapter.submitList(snapshots);
        if (activeDownloadEmptyView != null) {
            boolean empty = snapshots.isEmpty();
            activeDownloadEmptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            activeDownloadEmptyView.setText(DownloadPanelPresentation.emptyMessage(
                    activeDownloadSection, activeDownloadSearchQuery));
        }

        if (activeDownloadTitleView != null) {
            activeDownloadTitleView.setText(DownloadPanelPresentation.title(
                    downloadSelectMode, selectedDownloadIds.size()));
        }
        if (activeDownloadStorageView != null) {
            long now = System.currentTimeMillis();
            boolean finalizing = hasFinalizingDownload();
            // getTotalSpace/getFreeSpace may block on slow flash while a multi-GB file is copied.
            // Reuse the cached value until the export finishes instead of querying storage on UI.
            if (!finalizing && now - lastDownloadStorageUiMs
                    >= DownloadFinalizationPolicy.STORAGE_QUERY_INTERVAL_MS) {
                cachedDownloadStorageText = getStorageUsageText();
                lastDownloadStorageUiMs = now;
            }
            int active = countActiveDownloads();
            int queued = countQueuedDownloads();
            activeDownloadStorageView.setText(DownloadPanelPresentation.storageSummary(
                    cachedDownloadStorageText, finalizing, active, queued));
        }
        renderDownloadControlsIfNeeded();
    }

    private void renderDownloadControlsIfNeeded() {
        if (activeDownloadControlsPanel == null) return;
        String signature = DownloadPanelPresentation.controlsSignature(
                activeDownloadSection,
                activeDownloadSort,
                downloadSelectMode,
                selectedDownloadIds.size(),
                countActiveDownloads(),
                countQueuedDownloads(),
                downloadQueuePaused,
                downloadMaxActive,
                countCompletedDownloadHistory());
        if (signature.equals(lastDownloadControlsSignature)) return;
        lastDownloadControlsSignature = signature;
        activeDownloadControlsPanel.removeAllViews();
        activeDownloadControlsPanel.addView(downloadToolRow());
        if ("Mengunduh".equals(activeDownloadSection)) {
            activeDownloadControlsPanel.addView(downloadQueueControlRow());
        }
    }

    private View downloadToolRow() {
        return DownloadControlsFactory.createToolRow(
                this,
                activeDownloadSort,
                downloadSelectMode,
                activeDownloadSection,
                countCompletedDownloadHistory(),
                this::showDownloadSortDialog,
                () -> {
                    downloadSelectMode = !downloadSelectMode;
                    selectedDownloadIds.clear();
                    lastDownloadControlsSignature = "";
                    renderDownloadList();
                },
                this::confirmClearCompletedDownloadHistory,
                this::shareSelectedDownloads,
                this::deleteSelectedDownloads);
    }

    private View downloadQueueControlRow() {
        return DownloadControlsFactory.createQueueRow(
                this,
                countActiveDownloads(),
                downloadMaxActive,
                countQueuedDownloads(),
                this::pauseAllDownloads,
                this::resumeAllDownloads,
                this::showDownloadQueueSettingsDialog);
    }

    private TextView downloadToolButton(String text) {
        return DownloadControlsFactory.createButton(this, text);
    }

    private ArrayList<DownloadItem> getFilteredDownloadItems() {
        synchronized (downloadItems) {
            return DownloadListPolicy.filterAndSort(
                    downloadItems,
                    activeDownloadSection,
                    activeDownloadCategory,
                    activeDownloadSearchQuery,
                    activeDownloadSort);
        }
    }

    private String getDownloadCategory(DownloadItem item) {
        return DownloadItemUtils.getDownloadCategory(item);
    }

    private String normalizeDetectedCategory(String raw) {
        return DownloadItemUtils.normalizeDetectedCategory(raw);
    }

    private String inferDownloadCategoryFromData(String fileName, String url, String mimeType) {
        return DownloadItemUtils.inferDownloadCategoryFromData(fileName, url, mimeType);
    }

    private DownloadUiItem buildDownloadUiItem(DownloadItem item) {
        return DownloadUiItemFactory.build(
                item,
                getDownloadCategory(item),
                getDownloadHost(item),
                getDownloadSize(item),
                canPlayDownloadInsideYield(item),
                getDownloadQueuePosition(item),
                downloadSelectMode,
                selectedDownloadIds.contains(item.id));
    }

    private int getDownloadQueuePosition(DownloadItem target) {
        int position = 0;
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if (!"queued".equals(item.status)) continue;
                position++;
                if (item == target || item.id == target.id) return position;
            }
        }
        return 0;
    }

    private DownloadItem findDownloadItemById(int id) {
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) if (item.id == id) return item;
        }
        return null;
    }


    private long getDownloadSize(DownloadItem item) {
        return DownloadItemUtils.getDownloadSize(item);
    }

    private String getDownloadHost(DownloadItem item) {
        return DownloadItemUtils.getDownloadHost(item);
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private void resetDownloadSpeedState(DownloadItem item) {
        DownloadItemUtils.resetDownloadSpeedState(item);
    }

    private void updateDownloadSpeed(DownloadItem item, long currentBytes) {
        DownloadItemUtils.updateDownloadSpeed(item, currentBytes);
    }

    private String readableSpeed(double bytesPerSecond) {
        return BrowserUtils.readableSpeed(bytesPerSecond);
    }

    private String readableFileSize(long size) {
        if (size <= 0) return "0 B";
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        double value = size;
        int index = 0;
        while (value >= 1024 && index < units.length - 1) {
            value /= 1024.0;
            index++;
        }
        String formatted = String.format(Locale.US, index == 0 ? "%.0f %s" : "%.2f %s", value, units[index]);
        return formatted.replace(".00", "");
    }

    private String getStorageUsageText() {
        try {
            File dir = getDownloadDirectory();
            long total = dir.getTotalSpace();
            long free = dir.getFreeSpace();
            long used = total - free;
            return readableFileSize(used) + " terpakai dari " + readableFileSize(total);
        } catch (Exception e) {
            return "Penyimpanan tersedia";
        }
    }

    private void toggleDownloadSelection(DownloadItem item) {
        if (selectedDownloadIds.contains(item.id)) selectedDownloadIds.remove(item.id);
        else selectedDownloadIds.add(item.id);
        renderDownloadList();
    }

    private ArrayList<DownloadItem> getSelectedDownloads() {
        ArrayList<DownloadItem> selected = new ArrayList<>();
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if (selectedDownloadIds.contains(item.id)) selected.add(item);
            }
        }
        return selected;
    }

    private void shareSelectedDownloads() {
        ArrayList<DownloadItem> selected = getSelectedDownloads();
        if (selected.isEmpty()) {
            QuietToast.makeText(this, "Belum ada file dipilih", QuietToast.LENGTH_SHORT).show();
            return;
        }
        if (selected.size() == 1) {
            shareDownloadedFile(selected.get(0));
            return;
        }
        QuietToast.makeText(this, "Bagikan multi-file akan disempurnakan setelah FileProvider aktif", QuietToast.LENGTH_LONG).show();
    }

    private int countCompletedDownloadHistory() {
        synchronized (downloadItems) {
            return DownloadHistoryClearPolicy.countClearable(downloadItems);
        }
    }

    private void confirmClearCompletedDownloadHistory() {
        int count = countCompletedDownloadHistory();
        if (count <= 0) {
            QuietToast.makeText(this, "Tidak ada riwayat unduhan selesai",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Hapus semua riwayat unduhan?")
                .setMessage(count + " riwayat unduhan selesai akan dihapus dari daftar. "
                        + "File yang sudah tersimpan tetap ada di perangkat.")
                .setPositiveButton("Hapus semua", (dialog, which) -> clearCompletedDownloadHistory())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void clearCompletedDownloadHistory() {
        ArrayList<DownloadItem> removed = new ArrayList<>();
        synchronized (downloadItems) {
            for (int i = downloadItems.size() - 1; i >= 0; i--) {
                DownloadItem item = downloadItems.get(i);
                if (!DownloadHistoryClearPolicy.isClearable(item)) continue;
                item.runGeneration++;
                item.pauseRequested = true;
                item.status = "removed";
                removed.add(item);
                downloadItems.remove(i);
            }
        }

        if (removed.isEmpty()) {
            QuietToast.makeText(this, "Tidak ada riwayat unduhan selesai",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }

        for (DownloadItem item : removed) {
            stopActiveDownloadTransports(item);
            cancelDownloadNotification(item);
            selectedDownloadIds.remove(item.id);
        }
        downloadSelectMode = false;
        lastDownloadControlsSignature = "";
        saveDownloadHistory();
        renderDownloadList();
        updateDownloadKeepAliveState();
        mainHandler.post(this::pumpDownloadQueue);
        QuietToast.makeText(this, removed.size() + " riwayat unduhan dihapus. File tetap tersimpan.",
                QuietToast.LENGTH_SHORT).show();
    }

    private void deleteSelectedDownloads() {
        ArrayList<DownloadItem> selected = getSelectedDownloads();
        if (selected.isEmpty()) {
            QuietToast.makeText(this, "Belum ada file dipilih", QuietToast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Hapus " + selected.size() + " file?")
                .setMessage("File dan riwayat yang dipilih akan dihapus.")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    for (DownloadItem item : selected) {
                        removeDownloadItem(item, true);
                    }
                    selectedDownloadIds.clear();
                    downloadSelectMode = false;
                    renderDownloadList();
                })
                .setNegativeButton("Batal", null)
                .show();
    }


    // ===== Download queue manager =====
    private boolean isActiveDownloadStatus(String status) {
        return BrowserUtils.isActiveDownloadStatus(status);
    }

    private boolean hasFinalizingDownload() {
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if (item != null && ("verifying".equals(item.status) || "saving".equals(item.status))) {
                    return true;
                }
            }
        }
        return false;
    }

    private long getDownloadUiTickerDelayMs() {
        return hasFinalizingDownload()
                ? DownloadFinalizationPolicy.UI_TICK_FINALIZING_MS
                : DownloadFinalizationPolicy.UI_TICK_NORMAL_MS;
    }

    private int countActiveDownloads() {
        int count = 0;
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if (isActiveDownloadStatus(item.status)) count++;
            }
        }
        return count;
    }

    private boolean isCurrentDownloadRun(DownloadItem item, int generation) {
        return item != null && item.runGeneration == generation && !"removed".equals(item.status);
    }

    private DownloadItem findForegroundActiveDownload() {
        DownloadItem selected = null;
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if (item == null || !isActiveDownloadStatus(item.status)) continue;
                if (selected == null || item.speedBytesPerSecond > selected.speedBytesPerSecond) {
                    selected = item;
                }
            }
        }
        return selected;
    }

    private int getVisibleDownloadProgressPercent(DownloadItem item) {
        return DownloadItemUtils.getVisibleDownloadProgressPercent(item);
    }

    private String getForegroundDownloadText(DownloadItem item) {
        if (item == null) return "Mengunduh";
        if ("saving".equals(item.status)) {
            return "Menyimpan ke Downloads… " + getVisibleDownloadProgressPercent(item) + "%";
        }
        if ("verifying".equals(item.status)) return "Memverifikasi file…";
        double speed = item.smoothedSpeedBytesPerSecond > 0
                ? item.smoothedSpeedBytesPerSecond : item.speedBytesPerSecond;
        String text = "Mengunduh • " + getVisibleDownloadProgressPercent(item) + "%";
        if (speed > 0) text += " • " + readableSpeed(speed);
        return text;
    }

    private void updateDownloadKeepAliveState() {
        try {
            int activeCount = countActiveDownloads();
            DownloadItem selected = findForegroundActiveDownload();
            if (activeCount > 0 && selected != null) {
                DownloadKeepAliveService.startOrUpdate(this, selected, activeCount,
                        getForegroundDownloadText(selected));
            } else {
                DownloadKeepAliveService.stop(this);
            }
        } catch (Exception ignored) {
        }
    }


    private int countQueuedDownloads() {
        int count = 0;
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if ("queued".equals(item.status)) count++;
            }
        }
        return count;
    }

    private DownloadItem findNextQueuedDownload() {
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if ("queued".equals(item.status)) return item;
            }
        }
        return null;
    }

    private void enqueueOrStartDownload(DownloadItem item, File out) {
        if (item == null || out == null || "removed".equals(item.status)) return;
        if ("running".equals(item.status)) return;
        if (!downloadQueueEnabled) {
            startQueuedDownloadNow(item);
            return;
        }

        item.status = "queued";
        item.pauseRequested = false;
        item.engineInfo = "Antri • menunggu slot";
        saveDownloadHistory();
        refreshDownloadPanel();

        if (!downloadQueuePaused && countActiveDownloads() < Math.max(1, downloadMaxActive)) {
            startQueuedDownloadNow(item);
        } else {
            showDownloadNotification(item, "Masuk antrian", true);
        }
    }

    private void startQueuedDownloadNow(DownloadItem item) {
        if (item == null || "removed".equals(item.status)) return;
        try {
            if ("running".equals(item.status)) return;
            if (!"queued".equals(item.status) && !"paused".equals(item.status)) return;
            if (downloadQueueEnabled && countActiveDownloads() >= Math.max(1, downloadMaxActive)) return;

            File out = new File(item.path);
            synchronized (item.stateLock) {
                item.runGeneration++;
                item.status = "running";
                item.pauseRequested = false;
                resetDownloadSpeedState(item);
                item.activeConnectionLimit = 0;
                item.lastSpeedTimeMs = 0;
                item.lastSpeedBytes = item.downloadedBytes;
                item.engineInfo = item.downloadedBytes > 0 ? "Melanjutkan koneksi" : "Mengecek koneksi";
                item.rateLimiter.reset();
            }

            saveDownloadHistory();
            refreshDownloadPanel();
            cancelDownloadNotification(item);
            updateDownloadKeepAliveState();
            showDownloadNotification(item, item.downloadedBytes > 0 ? "Melanjutkan unduhan" : "Mulai mengunduh", true);
            startTwoConnectionDownload(item, out);
        } catch (Exception e) {
            failDownload(item, e.getMessage());
        }
    }

    private void pumpDownloadQueue() {
        if (downloadQueuePaused) return;

        if (!downloadQueueEnabled) {
            DownloadItem item;
            while ((item = findNextQueuedDownload()) != null) {
                startQueuedDownloadNow(item);
            }
            return;
        }

        int safety = 0;
        while (countActiveDownloads() < Math.max(1, downloadMaxActive) && safety < 8) {
            DownloadItem next = findNextQueuedDownload();
            if (next == null) break;
            startQueuedDownloadNow(next);
            safety++;
        }
    }

    private void pauseAllDownloads() {
        downloadQueuePaused = true;
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if ("running".equals(item.status)) {
                    item.runGeneration++;
                    item.pauseRequested = true;
                    stopActiveDownloadTransports(item);
                    item.status = "paused";
                    resetDownloadSpeedState(item);
                    item.engineInfo = getConnectionLabel(item) + " • dijeda";
                    showDownloadNotification(item, "Unduhan dijeda", false);
                } else if ("queued".equals(item.status)) {
                    item.runGeneration++;
                    item.status = "paused";
                    item.engineInfo = "Dijeda dari antrian";
                }
            }
        }
        saveDownloadHistory();
        refreshDownloadPanel();
        updateDownloadKeepAliveState();
        QuietToast.makeText(this, "Semua download dijeda", QuietToast.LENGTH_SHORT).show();
    }

    private void resumeAllDownloads() {
        downloadQueuePaused = false;
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if ("paused".equals(item.status)) {
                    item.status = "queued";
                    item.pauseRequested = false;
                    item.engineInfo = "Antri • menunggu slot";
                    resetDownloadSpeedState(item);
                }
            }
        }
        saveDownloadHistory();
        refreshDownloadPanel();
        QuietToast.makeText(this, "Semua download masuk antrian", QuietToast.LENGTH_SHORT).show();
        pumpDownloadQueue();
    }

    private void prioritizeQueuedDownload(DownloadItem item, boolean startIfPossible) {
        if (item == null) return;
        synchronized (downloadItems) {
            int idx = downloadItems.indexOf(item);
            if (idx > 0) {
                downloadItems.remove(idx);
                downloadItems.add(0, item);
            }
            if ("paused".equals(item.status)) {
                item.status = "queued";
                item.pauseRequested = false;
            }
            if ("queued".equals(item.status)) {
                item.engineInfo = "Prioritas • menunggu slot";
            }
        }
        activeDownloadSort = "Antrian";
        saveDownloadHistory();
        refreshDownloadPanel();
        QuietToast.makeText(this, "File diprioritaskan", QuietToast.LENGTH_SHORT).show();
        if (startIfPossible) pumpDownloadQueue();
    }

    private void moveQueuedDownload(DownloadItem item, int direction) {
        if (item == null) return;
        synchronized (downloadItems) {
            int idx = downloadItems.indexOf(item);
            if (idx < 0) return;
            int target = idx + direction;
            if (target < 0 || target >= downloadItems.size()) return;
            downloadItems.remove(idx);
            downloadItems.add(target, item);
        }
        activeDownloadSort = "Antrian";
        saveDownloadHistory();
        refreshDownloadPanel();
        QuietToast.makeText(this, direction < 0 ? "Naik antrian" : "Turun antrian", QuietToast.LENGTH_SHORT).show();
    }

    private void handleDownloadPrimaryAction(DownloadItem item) {
        if (item == null) return;

        long now = System.currentTimeMillis();
        if (now - item.lastActionClickMs < 650) {
            return;
        }
        item.lastActionClickMs = now;

        try {
            String status = item.status == null ? "" : item.status;
            if ("running".equals(status)) {
                pauseDownloadItem(item);
            } else if ("paused".equals(status)) {
                resumeDownloadItem(item);
            } else if ("queued".equals(status)) {
                if (!downloadQueuePaused && countActiveDownloads() < Math.max(1, downloadMaxActive)) {
                    startQueuedDownloadNow(item);
                } else {
                    prioritizeQueuedDownload(item, true);
                }
            } else if ("failed".equals(status)) {
                reloadDownloadItem(item);
            } else if ("completed".equals(status)) {
                openDownloadedFile(item);
            }
        } catch (Exception e) {
            QuietToast.makeText(this, "Aksi download gagal: " + e.getMessage(), QuietToast.LENGTH_SHORT).show();
        }

        refreshDownloadPanel();
    }

    private String getConnectionLabel(DownloadItem item) {
        if (item == null) return "Premium Fast";
        if (item.hlsDownload) return "HLS";
        int visibleConnections = item.activeConnectionLimit > 0 ? item.activeConnectionLimit : item.connectionCount;
        if (visibleConnections >= 4) return "4 koneksi";
        if (visibleConnections == 3) return "3 koneksi";
        if (visibleConnections >= 2) return "2 koneksi";
        if (visibleConnections == 1) return "1 koneksi";
        return "Premium Fast • anti-hotlink safe";
    }

    private void pauseDownloadItem(DownloadItem item) {
        if (item == null || (!"running".equals(item.status) && !"queued".equals(item.status))) return;
        item.runGeneration++;
        item.pauseRequested = true;
        stopActiveDownloadTransports(item);
        item.status = "paused";
        resetDownloadSpeedState(item);
        item.engineInfo = getConnectionLabel(item) + " • dijeda";
        saveDownloadHistory();
        refreshDownloadPanel();
        updateDownloadKeepAliveState();
        showDownloadNotification(item, "Unduhan dijeda", false);
        QuietToast.makeText(this, "Unduhan dijeda", QuietToast.LENGTH_SHORT).show();
        mainHandler.postDelayed(this::pumpDownloadQueue, 250);
    }

    private void resumeDownloadItem(DownloadItem item) {
        if (item == null || !"paused".equals(item.status)) return;
        try {
            item.pauseRequested = false;
            resetDownloadSpeedState(item);
            item.lastSpeedTimeMs = 0;
            item.lastSpeedBytes = item.downloadedBytes;
            downloadQueuePaused = false;

            File staged = item.path == null || item.path.isEmpty() ? null : new File(item.path);
            boolean networkPayloadComplete = staged != null && staged.exists() && staged.length() > 0
                    && ((!item.hlsDownload && item.totalBytes > 0
                            && item.downloadedBytes >= item.totalBytes
                            && staged.length() == item.totalBytes)
                        || (item.hlsDownload && item.totalBytes > 0
                            && item.hlsCompletedSegments >= item.totalBytes));
            if (networkPayloadComplete) {
                item.runGeneration++;
                item.status = "verifying";
                item.engineInfo = "Memulihkan tahap penyimpanan";
                refreshDownloadPanel();
                updateDownloadKeepAliveState();
                completeDownload(item);
                return;
            }

            boolean slotAvailable = !downloadQueueEnabled || countActiveDownloads() < Math.max(1, downloadMaxActive);
            if (slotAvailable) {
                item.engineInfo = "Melanjutkan koneksi";
                saveDownloadHistory();
                refreshDownloadPanel();
                QuietToast.makeText(this, "Melanjutkan unduhan", QuietToast.LENGTH_SHORT).show();
                startQueuedDownloadNow(item);
            } else {
                item.status = "queued";
                item.engineInfo = "Antri • menunggu slot";
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, "Masuk antrian", true);
                QuietToast.makeText(this, "Slot penuh, unduhan masuk antrian", QuietToast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            failDownload(item, e.getMessage());
        }
    }

    private void reloadDownloadItem(DownloadItem item) {
        if (item == null) return;
        try {
            item.runGeneration++;
            stopActiveDownloadTransports(item);
            item.pauseRequested = false;
            item.status = "queued";
            item.progress = 0;
            item.downloadedBytes = 0;
            item.totalBytes = 0;
            item.connectionCount = 0;
            item.activeConnectionLimit = 0;
            clearDynamicPartState(item);
            item.resolvedUrl = "";
            item.etag = "";
            item.lastModified = "";
            item.hlsCompletedSegments = 0;
            item.hlsOutputBytes = 0;
            item.hlsPlaylistFingerprint = "";
            item.hlsInitMapWritten = false;
            item.finalizeProgress = 0;
            item.finalizeBytes = 0;
            item.finalizeTotalBytes = 0;
            item.turboTargetConnections = 0;
            item.turboProfile = "";
            item.turboRetryPenalty = 0;
            resetDownloadSpeedState(item);
            item.lastSpeedTimeMs = 0;
            item.lastSpeedBytes = 0;
            item.engineInfo = "Mengecek koneksi";
            item.failReason = "";
            item.retryCount = 0;

            File out = new File(item.path);
            if (out.exists() && !out.delete()) throw new Exception("File lama tidak dapat dihapus");

            saveDownloadHistory();
            refreshDownloadPanel();
            updateDownloadKeepAliveState();
            showDownloadNotification(item, "Masuk antrian reload", true);
            QuietToast.makeText(this, "Download dimulai ulang dan masuk antrian", QuietToast.LENGTH_SHORT).show();
            enqueueOrStartDownload(item, out);
        } catch (Exception e) {
            failDownload(item, e.getMessage());
        }
    }

    private void showDownloadItemMenu(View anchor, DownloadItem item) {
        PopupMenu popup = new PopupMenu(this, anchor);

        if ("running".equals(item.status)) {
            popup.getMenu().add(0, 10, 0, "Jeda / Pause");
        } else if ("queued".equals(item.status)) {
            popup.getMenu().add(0, 13, 0, "Prioritaskan / mulai berikutnya");
            popup.getMenu().add(0, 14, 1, "Naik antrian");
            popup.getMenu().add(0, 15, 2, "Turun antrian");
            popup.getMenu().add(0, 10, 3, "Jeda");
        } else if ("paused".equals(item.status)) {
            popup.getMenu().add(0, 11, 0, "Lanjutkan");
            popup.getMenu().add(0, 12, 1, "Premium Fast • reload");
        } else if ("failed".equals(item.status)) {
            popup.getMenu().add(0, 12, 0, "Premium Fast • reload");
        }

        if (canPlayDownloadInsideYield(item)
                && ("running".equals(item.status) || "paused".equals(item.status)
                || "failed".equals(item.status) || "verifying".equals(item.status)
                || "saving".equals(item.status))) {
            popup.getMenu().add(0, 20, 0, "Putar sambil mengunduh");
        }

        if ("completed".equals(item.status)) {
            if (canPlayDownloadInsideYield(item)) {
                popup.getMenu().add(0, 20, 0, "Tonton di Yield");
            }
            popup.getMenu().add(0, 1, 1, "Buka dengan aplikasi lain");
            popup.getMenu().add(0, 2, 2, "Bagikan");
            popup.getMenu().add(0, 3, 3, "Ganti nama");
        }

        popup.getMenu().add(0, 4, 4, "Hapus riwayat");
        popup.getMenu().add(0, 5, 5, "Hapus file + riwayat");

        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == 10) {
                pauseDownloadItem(item);
                return true;
            } else if (id == 11) {
                resumeDownloadItem(item);
                return true;
            } else if (id == 12) {
                reloadDownloadItem(item);
                return true;
            } else if (id == 13) {
                prioritizeQueuedDownload(item, true);
                return true;
            } else if (id == 14) {
                moveQueuedDownload(item, -1);
                return true;
            } else if (id == 15) {
                moveQueuedDownload(item, 1);
                return true;
            } else if (id == 20) {
                playDownloadInsideYield(item);
                return true;
            } else if (id == 1) {
                openDownloadedFile(item);
                return true;
            } else if (id == 2) {
                shareDownloadedFile(item);
                return true;
            } else if (id == 3) {
                renameDownloadedFile(item);
                return true;
            } else if (id == 4) {
                removeDownloadItem(item, false);
                return true;
            } else if (id == 5) {
                removeDownloadItem(item, true);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private boolean canPlayDownloadInsideYield(DownloadItem item) {
        if (!downloadPlayWhileDownloadingEnabled || item == null || item.hlsDownload) return false;
        if (!"Video".equals(getDownloadCategory(item))) return false;
        if (!ProgressivePlaybackPolicy.supportsContainer(item.fileName, item.url, item.hlsDownload)) {
            return false;
        }
        if (item.publicUri != null && !item.publicUri.isEmpty()) return true;
        if (item.path == null || item.path.isEmpty()) return false;
        try {
            File file = new File(item.path);
            return file.exists() || "queued".equals(item.status) || "running".equals(item.status);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void playDownloadInsideYield(DownloadItem item) {
        if (item == null) return;
        if (!canPlayDownloadInsideYield(item)) {
            if (item.hlsDownload || looksLikeHlsDownload(item.url, item.fileName)) {
                QuietToast.makeText(this, "Video HLS dapat dibuka setelah proses penggabungan selesai",
                        QuietToast.LENGTH_LONG).show();
            } else {
                QuietToast.makeText(this, "Format ini belum mendukung putar sambil mengunduh",
                        QuietToast.LENGTH_SHORT).show();
            }
            return;
        }
        try {
            ProgressiveDownloadServer.PlaybackSession session =
                    ProgressiveDownloadServer.open(this, item);
            Intent intent = new Intent(this, ProgressiveVideoActivity.class);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_MEDIA_URL, session.mediaUrl);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_STATUS_URL, session.statusUrl);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_CLOSE_URL, session.closeUrl);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_TITLE, item.fileName);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_PRIVATE_SESSION,
                    dedicatedPrivateProfile || isCurrentPrivateTab());
            String originUrl = ProgressivePlaybackPolicy.selectOriginUrl(item.resolvedUrl, item.url);
            String playbackUserAgent = safeText(item.userAgent);
            if (playbackUserAgent.isEmpty()) {
                playbackUserAgent = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/120 Mobile Safari/537.36 YieldBrowser";
            }
            intent.putExtra(ProgressiveVideoActivity.EXTRA_ORIGIN_URL, originUrl);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_ORIGIN_USER_AGENT, playbackUserAgent);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_ORIGIN_REFERER, safeText(item.referer));
            intent.putExtra(ProgressiveVideoActivity.EXTRA_ORIGIN_COOKIE,
                    buildAntiHotlinkCookieHeader(originUrl, item));
            startActivity(intent);
        } catch (Exception e) {
            QuietToast.makeText(this, "Player belum dapat dibuka: " + safeText(e.getMessage()),
                    QuietToast.LENGTH_LONG).show();
        }
    }

    private void shareDownloadedFile(DownloadItem item) {
        try {
            Uri uri = getBestDownloadUri(item);
            if (uri == null) {
                QuietToast.makeText(this, "File tidak ditemukan", QuietToast.LENGTH_SHORT).show();
                return;
            }
            try {
                StrictMode.class.getMethod("disableDeathOnFileUriExposure").invoke(null);
            } catch (Exception ignored) {}
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Bagikan file"));
        } catch (Exception e) {
            QuietToast.makeText(this, "Gagal membagikan file", QuietToast.LENGTH_SHORT).show();
        }
    }

    private void renameDownloadedFile(DownloadItem item) {
        if (item == null || getBestDownloadUri(item) == null) {
            QuietToast.makeText(this, "File tidak ditemukan", QuietToast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(item.fileName);
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("Ganti nama")
                .setView(input)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        QuietToast.makeText(this, "Nama file kosong", QuietToast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        boolean renamed = false;
                        if (item.publicUri != null && !item.publicUri.isEmpty()) {
                            Uri uri = Uri.parse(item.publicUri);
                            try {
                                Uri renamedUri = DocumentsContract.renameDocument(
                                        getContentResolver(), uri, newName);
                                if (renamedUri != null) {
                                    item.publicUri = renamedUri.toString();
                                    renamed = true;
                                }
                            } catch (Exception ignored) {
                                ContentValues values = new ContentValues();
                                values.put(MediaStore.MediaColumns.DISPLAY_NAME, newName);
                                renamed = getContentResolver().update(uri, values, null, null) > 0;
                            }
                        }
                        if (!renamed && item.path != null && !item.path.isEmpty()) {
                            File currentFile = new File(item.path);
                            File parent = currentFile.getParentFile();
                            File newFile = parent == null ? null : new File(parent, newName);
                            if (newFile != null && !newFile.exists() && currentFile.renameTo(newFile)) {
                                item.path = newFile.getAbsolutePath();
                                renamed = true;
                            }
                        }
                        if (renamed) {
                            item.fileName = newName;
                            saveDownloadHistory();
                            renderDownloadList();
                            QuietToast.makeText(this, "Nama file diperbarui", QuietToast.LENGTH_SHORT).show();
                        } else {
                            QuietToast.makeText(this, "Gagal mengganti nama", QuietToast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        QuietToast.makeText(this, "Gagal mengganti nama", QuietToast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void removeDownloadItem(DownloadItem item, boolean deleteFile) {
        if (item == null) return;
        item.runGeneration++;
        item.pauseRequested = true;
        item.status = "removed";
        stopActiveDownloadTransports(item);
        cancelDownloadNotification(item);

        synchronized (downloadItems) {
            downloadItems.remove(item);
        }
        if (deleteFile && item.publicUri != null && !item.publicUri.isEmpty()) {
            try { getContentResolver().delete(Uri.parse(item.publicUri), null, null); }
            catch (Exception ignored) {}
        }
        if (deleteFile && item.path != null) {
            try {
                File file = new File(item.path);
                if (file.exists()) file.delete();
            } catch (Exception ignored) {}
        }
        selectedDownloadIds.remove(item.id);
        saveDownloadHistory();
        renderDownloadList();
        updateDownloadKeepAliveState();
        mainHandler.post(this::pumpDownloadQueue);
        QuietToast.makeText(this, deleteFile ? "File + riwayat dihapus" : "Riwayat dihapus", QuietToast.LENGTH_SHORT).show();
    }

    private Uri getBestDownloadUri(DownloadItem item) {
        try {
            if (item != null && item.publicUri != null && item.publicUri.length() > 0) {
                return Uri.parse(item.publicUri);
            }
        } catch (Exception ignored) {}
        try {
            if (item != null && item.path != null && item.path.length() > 0) {
                File file = new File(item.path);
                if (file.exists()) return Uri.fromFile(file);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void openDownloadedFile(DownloadItem item) {
        try {
            Uri uri = getBestDownloadUri(item);
            if (uri == null) {
                QuietToast.makeText(this, "File tidak ditemukan", QuietToast.LENGTH_SHORT).show();
                return;
            }
            try {
                StrictMode.class.getMethod("disableDeathOnFileUriExposure").invoke(null);
            } catch (Exception ignored) {}
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String ext = MimeTypeMap.getFileExtensionFromUrl(item.fileName);
            String mime = ext != null ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.US)) : null;
            if (mime == null) mime = "*/*";
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            QuietToast.makeText(this, "Tidak ada aplikasi untuk membuka file ini", QuietToast.LENGTH_SHORT).show();
        }
    }

    private void refreshDownloadPanel() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            renderDownloadList();
            return;
        }
        // Coalesce worker progress events so a fast connection or large-file copy cannot flood
        // the main thread with hundreds of redundant RecyclerView refresh callbacks.
        if (!downloadUiRefreshPosted.compareAndSet(false, true)) return;
        mainHandler.post(() -> {
            downloadUiRefreshPosted.set(false);
            renderDownloadList();
        });
    }

    private String getCurrentPageForReferer() {
        try {
            if (webView != null && webView.getUrl() != null) return webView.getUrl();
        } catch (Exception ignored) {}
        try {
            String current = getEffectiveCurrentUrl();
            if (current != null) return current;
        } catch (Exception ignored) {}
        return "";
    }



    // ===== Download engine =====

    private void beginDownloadFromWeb(String url, String contentDisposition, String mimeType, String userAgent) {
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        beginDownload(url, fileName, userAgent, getCurrentPageForReferer());
    }

    private void beginDownload(String fileUrl, String guessedFileName, String userAgent, String referer) {
        try {
            ensureDownloadNotificationPermission();
            fileUrl = normalizeGoogleDriveDownloadUrl(fileUrl);
            String fileName = guessedFileName;
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = URLUtil.guessFileName(fileUrl, null, null);
            }
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "yield_download_" + System.currentTimeMillis() + ".bin";
            }

            File dir = getDownloadDirectory();
            if (!dir.exists()) dir.mkdirs();
            fileName = autoRenameDownloadFile(fileName, fileUrl, "");
            File out = uniqueFile(new File(dir, fileName));
            DownloadItem item = new DownloadItem(nextDownloadId++, fileUrl, out.getName(),
                    out.getAbsolutePath(), "queued", 0);
            item.engineInfo = "Mengecek koneksi";
            item.userAgent = userAgent == null ? "" : userAgent;
            item.referer = referer == null ? "" : referer;
            item.cookieHeader = buildAntiHotlinkCookieHeader(fileUrl, item);
            item.categoryHint = inferDownloadCategoryFromData(fileName, fileUrl, "");

            synchronized (downloadItems) {
                downloadItems.add(item);
            }
            saveDownloadHistory();
            refreshDownloadPanel();
            showDownloadNotification(item, "Masuk antrian", true);
            showDownloadStartedBanner(item);
            enqueueOrStartDownload(item, out);
        } catch (Exception e) {
            QuietToast.makeText(this, "Gagal memulai unduhan: " + e.getMessage(), QuietToast.LENGTH_SHORT).show();
        }
    }



    private void showDownloadStartedBanner(DownloadItem item) {
        runOnUiThread(() -> {
            if (contentFrame == null || item == null) {
                QuietToast.makeText(this, "Unduhan dimulai", QuietToast.LENGTH_SHORT).show();
                return;
            }
            LinearLayout banner = new LinearLayout(this);
            banner.setOrientation(LinearLayout.HORIZONTAL);
            banner.setGravity(Gravity.CENTER_VERTICAL);
            banner.setPadding(dp(14), dp(10), dp(10), dp(10));
            banner.setBackground(roundRect(Color.parseColor("#242830"), dp(18), dp(1), COLOR_BORDER));
            banner.setAlpha(0f);
            banner.setTranslationY(dp(18));

            LinearLayout textWrap = new LinearLayout(this);
            textWrap.setOrientation(LinearLayout.VERTICAL);
            TextView title = new TextView(this);
            title.setText("Unduhan dimulai");
            title.setTextColor(Color.WHITE);
            title.setTextSize(14);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            textWrap.addView(title);
            TextView name = new TextView(this);
            name.setText(item.fileName);
            name.setTextColor(COLOR_SUBTEXT);
            name.setTextSize(12);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            textWrap.addView(name);
            banner.addView(textWrap, new LinearLayout.LayoutParams(0, -2, 1));

            TextView open = new TextView(this);
            open.setText("Lihat");
            open.setTextColor(COLOR_ACCENT);
            open.setTextSize(13);
            open.setTypeface(Typeface.DEFAULT_BOLD);
            open.setGravity(Gravity.CENTER);
            open.setPadding(dp(14), dp(8), dp(14), dp(8));
            open.setOnClickListener(v -> {
                try { contentFrame.removeView(banner); } catch (Exception ignored) {}
                showDownloadManager();
            });
            banner.addView(open, new LinearLayout.LayoutParams(-2, dp(42)));

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
            lp.setMargins(dp(14), dp(14), dp(14), dp(76));
            contentFrame.addView(banner, lp);
            banner.animate().alpha(1f).translationY(0f).setDuration(180L).start();
            mainHandler.postDelayed(() -> {
                if (banner.getParent() == null) return;
                banner.animate().alpha(0f).translationY(dp(14)).setDuration(180L)
                        .withEndAction(() -> {
                            try { contentFrame.removeView(banner); } catch (Exception ignored) {}
                        }).start();
            }, 4200L);
        });
    }

    private void ensureDownloadNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (preferences.getBoolean("downloadNotificationPermissionAsked", false)) return;
        preferences.edit().putBoolean("downloadNotificationPermissionAsked", true).apply();
        try {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_DOWNLOAD_NOTIFICATIONS);
        } catch (Exception ignored) {
        }
    }

    private File getDownloadDirectory() {
        File base;
        if ("Download".equals(downloadSubfolder)) {
            base = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        } else {
            File root = getExternalFilesDir(null);
            base = root != null ? new File(root, downloadSubfolder) : new File(getFilesDir(), downloadSubfolder);
        }
        if (base == null) base = getFilesDir();
        if (!base.exists()) base.mkdirs();
        return base;
    }

    private File uniqueFile(File file) {
        if (!file.exists()) return file;
        String name = file.getName();
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        File parent = file.getParentFile();
        int index = 1;
        File candidate;
        do {
            candidate = new File(parent, base + " (" + index + ")" + ext);
            index++;
        } while (candidate.exists());
        return candidate;
    }

    private String getOriginFromUrl(String value) {
        try {
            if (value == null || value.isEmpty()) return "";
            Uri uri = Uri.parse(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) return "";
            int port = uri.getPort();
            return port > 0 ? scheme + "://" + host + ":" + port : scheme + "://" + host;
        } catch (Exception e) {
            return "";
        }
    }

    private String getRootUrl(String value) {
        String origin = getOriginFromUrl(value);
        return origin.isEmpty() ? "" : origin + "/";
    }

    private String getHostLower(String value) {
        return BrowserUtils.getHostLower(value);
    }

    private boolean isGoogleDriveHost(String url) {
        String host = getHostLower(url);
        return host.equals("drive.google.com")
                || host.endsWith(".drive.google.com")
                || host.equals("drive.usercontent.google.com")
                || host.endsWith(".drive.usercontent.google.com")
                || host.equals("docs.google.com")
                || host.endsWith(".docs.google.com")
                || host.endsWith(".googleusercontent.com");
    }

    private boolean isStableDownloadHost(String url) {
        return DownloadUrlPolicy.isStableDownloadHost(url);
    }

    private boolean isTurboFriendlyHost(String url) {
        String host = getHostLower(url);
        if (host.isEmpty() || isStableDownloadHost(url) || isGoogleDriveHost(url)) return false;
        return host.contains("cdn")
                || host.contains("cloudfront.net")
                || host.contains("bunnycdn")
                || host.contains("r2.cloudflarestorage.com")
                || host.contains("backblazeb2.com")
                || host.contains("wasabisys.com")
                || host.contains("digitaloceanspaces.com")
                || host.contains("storage.googleapis.com")
                || host.contains("githubusercontent.com")
                || host.contains("github.com");
    }

    private String normalizeGoogleDriveDownloadUrl(String value) {
        return DownloadUrlPolicy.normalizeGoogleDriveDownloadUrl(value);
    }

    private boolean looksLikeArchiveOrApp(String fileName, String url, String contentType) {
        String name = (fileName == null ? "" : fileName).toLowerCase(Locale.US);
        String link = (url == null ? "" : url).toLowerCase(Locale.US);
        String type = (contentType == null ? "" : contentType).toLowerCase(Locale.US);
        return name.endsWith(".apk") || name.endsWith(".zip") || name.endsWith(".rar")
                || name.endsWith(".7z") || name.endsWith(".tar") || name.endsWith(".gz")
                || link.contains(".apk") || link.contains(".zip") || link.contains(".rar")
                || link.contains(".7z") || type.contains("zip") || type.contains("rar")
                || type.contains("apk") || type.contains("octet-stream");
    }

    // v0.9.92: ukuran buffer baca/tulis adaptif. Untuk file besar, buffer lebih besar mengurangi
    // jumlah operasi I/O ke flash sehingga throughput naik; untuk file kecil tetap kecil agar hemat
    // RAM (buffer dialokasikan per koneksi; maksimum 512KB x 4 koneksi = 2MB per unduhan).
    private int chooseDownloadBufferSize(long totalBytes) {
        return BrowserUtils.chooseDownloadBufferSize(totalBytes);
    }

    private int chooseSmartDownloadConnections(DownloadItem item, long totalBytes, String contentType) {
        String url = item == null ? "" : item.url;
        String name = item == null ? "" : item.fileName;
        int chosen;
        String reason;
        int score = 50;

        if (item != null && item.turboTargetConnections > 0
                && item.turboProfile != null && item.turboProfile.contains("fallback")) {
            chosen = item.turboTargetConnections;
            reason = item.turboProfile;
        } else if (!downloadDynamic4Connections) {
            chosen = DOWNLOAD_CONNECTIONS_STABLE;
            reason = "Smart normal";
        } else if (isGoogleDriveHost(url)) {
            if (totalBytes >= DOWNLOAD_BALANCED_UNKNOWN_FILE && item != null && item.turboRetryPenalty == 0) {
                chosen = DOWNLOAD_CONNECTIONS_BALANCED;
                reason = "Google Drive adaptive 3";
            } else {
                chosen = DOWNLOAD_CONNECTIONS_STABLE;
                reason = "Google Drive stable 2";
            }
        } else if (isStableDownloadHost(url)) {
            chosen = DOWNLOAD_STABLE_HOST_LIMIT;
            reason = "Stable host v3";
        } else if (totalBytes > 0 && totalBytes < 16L * 1024L * 1024L) {
            chosen = 1;
            reason = "Small file v3";
        } else {
            boolean video = looksLikeVideoDownload(url, name, contentType);
            boolean turboHost = isTurboFriendlyHost(url);
            boolean archiveOrApp = looksLikeArchiveOrApp(name, url, contentType);
            if (turboHost) score += 26;
            if (video && totalBytes >= DOWNLOAD_TURBO_MIN_LARGE_FILE) score += 35;
            else if (video) score += 18;
            if (totalBytes >= DOWNLOAD_TURBO_UNKNOWN_LARGE_FILE) score += 24;
            else if (totalBytes >= DOWNLOAD_BALANCED_UNKNOWN_FILE) score += 12;
            if (archiveOrApp && !turboHost) score -= 10;
            if (url != null && url.toLowerCase(Locale.US).contains("token=")) score -= 4;
            if (item != null) score -= Math.min(25, item.turboRetryPenalty * 8);

            if (video && totalBytes >= DOWNLOAD_TURBO_MIN_LARGE_FILE) {
                chosen = DOWNLOAD_CONNECTIONS_DYNAMIC_MAX;
                reason = "Turbo video >50MB v3";
            } else if (score >= DOWNLOAD_V3_SCORE_TURBO) {
                chosen = DOWNLOAD_CONNECTIONS_DYNAMIC_MAX;
                reason = "Turbo score " + score;
            } else if (score >= DOWNLOAD_V3_SCORE_BALANCED) {
                chosen = DOWNLOAD_CONNECTIONS_BALANCED;
                reason = "Balanced score " + score;
            } else {
                chosen = DOWNLOAD_CONNECTIONS_STABLE;
                reason = "Stable score " + score;
            }
        }

        if (item != null) {
            item.turboTargetConnections = chosen;
            item.turboProfile = reason;
            item.turboStabilityScore = 100;
            item.turboSlowSamples = 0;
            item.turboHealthySamples = 0;
            item.turboJitterScore = 0;
            item.turboPeakSpeedBytesPerSecond = 0;
        }
        return Math.max(1, Math.min(DOWNLOAD_CONNECTIONS_DYNAMIC_MAX, chosen));
    }

    private String getTurboLabel(DownloadItem item, int connections) {
        String profile = item != null && item.turboProfile != null && !item.turboProfile.isEmpty()
                ? item.turboProfile : "Smart v3";
        if (connections >= 4) return "Turbo 4 koneksi • " + profile;
        if (connections == 3) return "Balanced 3 koneksi • " + profile;
        if (connections >= 2) return "Stable 2 koneksi • " + profile;
        return "Safe 1 koneksi • " + profile;
    }

    private void registerDownloadConnection(DownloadItem item, HttpURLConnection connection) {
        if (item == null || connection == null) return;
        synchronized (item.activeConnections) {
            if (!item.activeConnections.contains(connection)) item.activeConnections.add(connection);
        }
    }

    private void unregisterDownloadConnection(DownloadItem item, HttpURLConnection connection) {
        if (item == null || connection == null) return;
        synchronized (item.activeConnections) {
            item.activeConnections.remove(connection);
        }
    }

    private void registerDownloadStream(DownloadItem item, InputStream stream) {
        if (item == null || stream == null) return;
        synchronized (item.activeStreams) {
            if (!item.activeStreams.contains(stream)) item.activeStreams.add(stream);
        }
    }

    private void unregisterDownloadStream(DownloadItem item, InputStream stream) {
        if (item == null || stream == null) return;
        synchronized (item.activeStreams) {
            item.activeStreams.remove(stream);
        }
    }

    private void stopActiveDownloadTransports(DownloadItem item) {
        if (item == null) return;
        ArrayList<InputStream> streams;
        ArrayList<HttpURLConnection> connections;
        synchronized (item.activeStreams) {
            streams = new ArrayList<>(item.activeStreams);
            item.activeStreams.clear();
        }
        synchronized (item.activeConnections) {
            connections = new ArrayList<>(item.activeConnections);
            item.activeConnections.clear();
        }
        for (InputStream stream : streams) {
            try { stream.close(); } catch (Exception ignored) {}
        }
        for (HttpURLConnection connection : connections) {
            try { connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    private void resetTurboSampling(DownloadItem item) {
        if (item == null) return;
        item.turboAvgSpeedBytesPerSecond = 0;
        item.turboLastSampleBytes = item.downloadedBytes;
        item.turboLastSampleTimeMs = System.currentTimeMillis();
        item.turboSlowSamples = 0;
        item.turboHealthySamples = 0;
        item.turboJitterScore = 0;
        item.turboPeakSpeedBytesPerSecond = 0;
        item.turboLastPersistMs = 0;
        item.turboStabilityScore = 100;
    }

    private void maybePersistDownloadProgress(DownloadItem item) {
        long now = System.currentTimeMillis();
        if (item != null && now - item.lastProgressPersistMs >= 1800L) {
            item.lastProgressPersistMs = now;
            saveDownloadHistory();
        }
    }

    private void updateTurboPrediction(DownloadItem item, long currentBytes) {
        if (item == null) return;
        long now = System.currentTimeMillis();
        boolean sampled = false;
        synchronized (item.stateLock) {
            if (item.turboLastSampleTimeMs <= 0) {
                item.turboLastSampleTimeMs = now;
                item.turboLastSampleBytes = currentBytes;
                return;
            }
            long elapsed = now - item.turboLastSampleTimeMs;
            if (elapsed < 1500) return;
            long delta = Math.max(0, currentBytes - item.turboLastSampleBytes);
            double sample = (delta * 1000.0) / Math.max(1, elapsed);
            if (sample > item.turboPeakSpeedBytesPerSecond) item.turboPeakSpeedBytesPerSecond = sample;
            double previousAvg = item.turboAvgSpeedBytesPerSecond;
            item.turboAvgSpeedBytesPerSecond = previousAvg <= 0
                    ? sample : previousAvg * 0.72 + sample * 0.28;
            double ratio = item.turboAvgSpeedBytesPerSecond > 0
                    ? sample / Math.max(1.0, item.turboAvgSpeedBytesPerSecond) : 1.0;
            double jitter = previousAvg > 0
                    ? Math.abs(sample - previousAvg) / Math.max(1.0, previousAvg) : 0;
            item.turboJitterScore = item.turboJitterScore * 0.70
                    + Math.min(1.5, jitter) * 30.0;
            if (ratio < 0.30 || (delta == 0 && elapsed >= 3500)) {
                item.turboSlowSamples++;
                item.turboHealthySamples = 0;
            } else if (ratio >= 0.70) {
                item.turboHealthySamples++;
                if (item.turboSlowSamples > 0) item.turboSlowSamples--;
            }
            double penalty = 0;
            if (ratio < 0.45) penalty += 10;
            if (item.turboJitterScore > 18) penalty += 6;
            if (item.turboRetryPenalty > 0) penalty += Math.min(18, item.turboRetryPenalty * 6);
            item.turboStabilityScore = Math.max(0,
                    Math.min(100, item.turboStabilityScore + (ratio >= 0.70 ? 3 : 0) - penalty));
            item.turboLastSampleBytes = currentBytes;
            item.turboLastSampleTimeMs = now;
            sampled = true;
        }
        if (sampled) maybePersistDownloadProgress(item);
    }

    private int getV3FallbackConnections(DownloadItem item) {
        if (item == null) return DOWNLOAD_CONNECTIONS_STABLE;
        if (item.connectionCount >= 4) {
            if (item.turboStabilityScore < 22 || item.turboRetryPenalty >= 2) return DOWNLOAD_CONNECTIONS_STABLE;
            return DOWNLOAD_CONNECTIONS_BALANCED;
        }
        if (item.connectionCount == 3) return DOWNLOAD_CONNECTIONS_STABLE;
        return 1;
    }

    private boolean shouldFallbackTurboToStable(DownloadItem item) {
        return DownloadItemUtils.shouldFallbackTurboToStable(item);
    }

    private String buildAntiHotlinkCookieHeader(String fileUrl, DownloadItem item) {
        try {
            LinkedHashSet<String> cookies = new LinkedHashSet<>();
            if (item != null && item.cookieHeader != null && !item.cookieHeader.trim().isEmpty()) {
                for (String part : item.cookieHeader.split(";")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) cookies.add(trimmed);
                }
            }
            String[] sources = new String[]{fileUrl, item != null ? item.referer : "",
                    getRootUrl(fileUrl), item != null ? getRootUrl(item.referer) : ""};
            for (String source : sources) {
                if (source == null || source.isEmpty()) continue;
                String cookie = CookieManager.getInstance().getCookie(source);
                if (cookie == null || cookie.trim().isEmpty()) continue;
                for (String part : cookie.split(";")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) cookies.add(trimmed);
                }
            }
            StringBuilder value = new StringBuilder();
            for (String cookie : cookies) {
                if (value.length() > 0) value.append("; ");
                value.append(cookie);
            }
            return value.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private HttpURLConnection openDownloadConnection(String url, DownloadItem item, String range) throws Exception {
        boolean primary = item != null && url != null && url.equals(item.url);
        String cached = primary && item.resolvedUrl != null ? item.resolvedUrl : "";
        Exception lastError = null;

        for (int attempt = 0; attempt < (cached.isEmpty() ? 1 : 2); attempt++) {
            String current = attempt == 0 && !cached.isEmpty() ? cached : url;
            for (int redirect = 0; redirect < 8; redirect++) {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(current).openConnection();
                    registerDownloadConnection(item, connection);
                    connection.setInstanceFollowRedirects(false);
                    if (range != null && !range.isEmpty()) connection.setRequestProperty("Range", range);
                    applyDownloadHeaders(connection, current, item, primary && range != null && !range.isEmpty());
                    connection.connect();
                    int code = connection.getResponseCode();
                    if (isRedirectCode(code)) {
                        String location = connection.getHeaderField("Location");
                        unregisterDownloadConnection(item, connection);
                        connection.disconnect();
                        if (location == null || location.isEmpty()) throw new Exception("Redirect tanpa lokasi");
                        current = new URL(new URL(current), location).toString();
                        continue;
                    }
                    if (attempt == 0 && !cached.isEmpty()
                            && (code == HttpURLConnection.HTTP_UNAUTHORIZED
                            || code == HttpURLConnection.HTTP_FORBIDDEN)) {
                        unregisterDownloadConnection(item, connection);
                        connection.disconnect();
                        item.resolvedUrl = "";
                        break;
                    }
                    if (primary && code < 400) item.resolvedUrl = current;
                    return connection;
                } catch (Exception e) {
                    lastError = e;
                    if (connection != null) {
                        unregisterDownloadConnection(item, connection);
                        try { connection.disconnect(); } catch (Exception ignored) {}
                    }
                    break;
                }
            }
        }
        throw lastError != null ? lastError : new Exception("Terlalu banyak redirect");
    }

    private boolean isRedirectCode(int code) {
        return BrowserUtils.isRedirectCode(code);
    }

    private void validateDownloadResponse(HttpURLConnection connection) throws Exception {
        int code = connection.getResponseCode();
        if (code >= 400) throw new Exception("Server menolak unduhan HTTP " + code);
        String contentType = connection.getContentType();
        String disposition = connection.getHeaderField("Content-Disposition");
        if (contentType != null && contentType.toLowerCase(Locale.US).contains("text/html")) {
            String lowerDisposition = disposition == null ? "" : disposition.toLowerCase(Locale.US);
            if (!lowerDisposition.contains("attachment") && !lowerDisposition.contains("filename=")) {
                throw new Exception("Link mengarah ke halaman HTML, bukan file. Klik tombol download asli.");
            }
        }
    }

    private void applyDownloadHeaders(HttpURLConnection connection, String fileUrl,
                                      DownloadItem item, boolean applyIfRange) {
        connection.setConnectTimeout(DOWNLOAD_CONNECT_TIMEOUT);
        connection.setReadTimeout(DOWNLOAD_READ_TIMEOUT);
        connection.setUseCaches(false);
        String userAgent = item == null ? "" : item.userAgent;
        if (userAgent == null || userAgent.isEmpty()) {
            userAgent = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120 Mobile Safari/537.36 YieldBrowser";
        }
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setRequestProperty("Connection", "keep-alive");

        String referer = item == null ? "" : item.referer;
        if (referer != null && !referer.isEmpty()) connection.setRequestProperty("Referer", referer);
        if (!isGoogleDriveHost(fileUrl) && referer != null && !referer.isEmpty()
                && getHostLower(referer).equals(getHostLower(fileUrl))) {
            String origin = getOriginFromUrl(referer);
            if (!origin.isEmpty()) connection.setRequestProperty("Origin", origin);
        }
        if (applyIfRange && item != null) {
            if (item.etag != null && !item.etag.isEmpty()) connection.setRequestProperty("If-Range", item.etag);
            else if (item.lastModified != null && !item.lastModified.isEmpty()) {
                connection.setRequestProperty("If-Range", item.lastModified);
            }
        }
        String cookie = buildAntiHotlinkCookieHeader(fileUrl, item);
        if (!cookie.isEmpty()) connection.setRequestProperty("Cookie", cookie);
    }

    private void captureRemoteIdentity(DownloadItem item, HttpURLConnection connection) throws Exception {
        if (item == null || connection == null) return;
        String newEtag = safeHeader(connection.getHeaderField("ETag"));
        String newLastModified = safeHeader(connection.getHeaderField("Last-Modified"));
        synchronized (item.stateLock) {
            if (!item.etag.isEmpty() && !newEtag.isEmpty() && !item.etag.equals(newEtag)) {
                throw new Exception("File di server berubah (ETag berbeda)");
            }
            if (item.etag.isEmpty() && !item.lastModified.isEmpty() && !newLastModified.isEmpty()
                    && !item.lastModified.equals(newLastModified)) {
                throw new Exception("File di server berubah (Last-Modified berbeda)");
            }
            if (item.etag.isEmpty() && !newEtag.isEmpty()) item.etag = newEtag;
            if (item.lastModified.isEmpty() && !newLastModified.isEmpty()) item.lastModified = newLastModified;
        }
    }

    private String safeHeader(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isProtocolOrIdentityFailure(String reason) {
        if (reason == null) return false;
        return reason.contains("Range")
                || reason.contains("Content-Range")
                || reason.contains("Panjang respons")
                || reason.contains("Ukuran file berubah")
                || reason.contains("File di server berubah");
    }

    private boolean isPermanentDownloadError(String reason) {
        return DownloadUrlPolicy.isPermanentDownloadError(reason);
    }

    private void startTwoConnectionDownload(DownloadItem item, File out) {
        if (item == null || out == null || "removed".equals(item.status)) return;
        if (downloadHlsEnabled && (item.hlsDownload || looksLikeHlsDownload(item.url, item.fileName))) {
            startHlsDownload(item, out);
            return;
        }
        boolean resume = out.exists() && item.downloadedBytes > 0 && "running".equals(item.status);
        if (resume && item.connectionCount == 1) {
            startSingleConnectionDownloadAsync(item, out);
        } else if (resume && item.connectionCount >= 3) {
            startDynamicMultiConnectionDownload(item, out);
        } else if (resume || !downloadDynamic4Connections) {
            startLegacyTwoConnectionDownload(item, out);
        } else {
            startDynamicMultiConnectionDownload(item, out);
        }
    }


    private void startSingleConnectionDownloadAsync(DownloadItem item, File out) {
        if (item == null || out == null || "removed".equals(item.status)) return;
        final int generation = item.runGeneration;
        DOWNLOAD_IO_EXECUTOR.execute(() -> {
            if (!isCurrentDownloadRun(item, generation)) return;
            downloadSingle(item, out);
        });
    }

    private boolean looksLikeHlsDownload(String url, String fileName) {
        return DownloadUrlPolicy.looksLikeHlsDownload(url, fileName);
    }

    private boolean looksLikeVideoDownload(String url, String fileName, String contentType) {
        return BrowserUtils.looksLikeVideoDownload(url, fileName, contentType);
    }

    private String autoRenameDownloadFile(String fileName, String url, String contentType) {
        String name = fileName == null ? "" : fileName.trim();
        if (name.isEmpty()) name = "yield_download_" + System.currentTimeMillis();
        if (looksLikeHlsDownload(url, name)) {
            if (name.toLowerCase(Locale.US).endsWith(".m3u8")) name = name.substring(0, name.length() - 5) + ".ts";
            else if (!name.toLowerCase(Locale.US).endsWith(".ts")) name += ".ts";
        }
        if (looksLikeVideoDownload(url, name, contentType)) {
            name = name.replaceAll("(?i)videoplayback(\\.[a-z0-9]+)?$", "yield_video$1");
            if (!name.contains(".")) name += ".mp4";
        }
        return name.replace("/", "_").replace("\\", "_").replace(":", "_");
    }

    private void setDynamicPartState(DownloadItem item, int part, long start, long end, long done) {
        synchronized (item.stateLock) {
            if (part == 1) { item.part1Start = start; item.part1End = end; item.part1Done = done; }
            else if (part == 2) { item.part2Start = start; item.part2End = end; item.part2Done = done; }
            else if (part == 3) { item.part3Start = start; item.part3End = end; item.part3Done = done; }
            else if (part == 4) { item.part4Start = start; item.part4End = end; item.part4Done = done; }
        }
    }

    private long getDynamicPartStart(DownloadItem item, int part) {
        return BrowserUtils.getDynamicPartStart(item, part);
    }

    private long getDynamicPartEnd(DownloadItem item, int part) {
        return BrowserUtils.getDynamicPartEnd(item, part);
    }

    private long getDynamicPartDone(DownloadItem item, int part) {
        return BrowserUtils.getDynamicPartDone(item, part);
    }

    private void addDynamicPartDone(DownloadItem item, int part, long delta) {
        synchronized (item.stateLock) {
            long length = DownloadProtocol.expectedLength(getDynamicPartStart(item, part), getDynamicPartEnd(item, part));
            long value = Math.min(length, getDynamicPartDone(item, part) + delta);
            setDynamicPartState(item, part, getDynamicPartStart(item, part), getDynamicPartEnd(item, part), value);
        }
    }

    private void clearDynamicPartState(DownloadItem item) {
        if (item == null) return;
        synchronized (item.stateLock) {
            item.part1Start = item.part1End = item.part1Done = 0;
            item.part2Start = item.part2End = item.part2Done = 0;
            item.part3Start = item.part3End = item.part3Done = 0;
            item.part4Start = item.part4End = item.part4Done = 0;
        }
    }

    private boolean hasDynamicResumeState(DownloadItem item, int connections, long total, File file) {
        if (item == null || connections < 3 || total <= 0 || file == null || file.length() != total) return false;
        if (item.connectionCount != connections || item.totalBytes != total) return false;
        long sum = 0;
        for (int part = 1; part <= connections; part++) {
            long start = getDynamicPartStart(item, part);
            long end = getDynamicPartEnd(item, part);
            long done = getDynamicPartDone(item, part);
            long length = DownloadProtocol.expectedLength(start, end);
            if (length <= 0 || done < 0 || done > length) return false;
            sum += done;
        }
        return sum > 0;
    }

    private void initializeDynamicParts(DownloadItem item, int connections, long total) {
        clearDynamicPartState(item);
        long base = total / connections;
        long remainder = total % connections;
        long start = 0;
        for (int part = 1; part <= connections; part++) {
            long length = base + (part <= remainder ? 1 : 0);
            long end = start + length - 1;
            setDynamicPartState(item, part, start, end, 0);
            start = end + 1;
        }
    }

    private boolean verifyMultipartComplete(DownloadItem item, int connections, long total, File file) {
        if (file == null || !file.exists() || file.length() != total) return false;
        long sum = 0;
        for (int part = 1; part <= connections; part++) {
            long expected = DownloadProtocol.expectedLength(getDynamicPartStart(item, part), getDynamicPartEnd(item, part));
            long done = getDynamicPartDone(item, part);
            if (expected <= 0 || done != expected) return false;
            sum += done;
        }
        return sum == total && item.downloadedBytes == total;
    }

    private void resetForCleanRestart(DownloadItem item, File out) {
        stopActiveDownloadTransports(item);
        clearDynamicPartState(item);
        item.downloadedBytes = 0;
        item.totalBytes = 0;
        item.progress = 0;
        item.connectionCount = 0;
        item.activeConnectionLimit = 0;
        item.etag = "";
        item.lastModified = "";
        item.resolvedUrl = "";
        item.rateLimiter.reset();
        if (out != null && out.exists()) out.delete();
    }

    private void startDynamicMultiConnectionDownload(DownloadItem item, File out) {
        final int generation = item.runGeneration;
        item.status = "running";
        item.pauseRequested = false;
        item.engineInfo = item.downloadedBytes > 0 ? "Mengecek resume multipart" : "Mengecek koneksi adaptif";
        refreshDownloadPanel();

        new Thread(() -> {
            final File[] outRef = new File[]{out};
            HttpURLConnection probe = null;
            try {
                if (!isCurrentDownloadRun(item, generation)) return;
                boolean resumeCandidate = out.exists() && item.downloadedBytes > 0 && item.connectionCount >= 3;
                probe = openDownloadConnection(item.url, item, "bytes=0-0");
                validateDownloadResponse(probe);
                DownloadProtocol.RangeInfo probeRange = DownloadProtocol.requireRange(probe, 0, 0, -1);
                if (probeRange.total <= 1) throw new Exception("Ukuran multipart tidak tersedia");
                captureRemoteIdentity(item, probe);
                long total = probeRange.total;
                String contentType = probe.getContentType();
                String disposition = probe.getHeaderField("Content-Disposition");
                if (!resumeCandidate) {
                    String betterName = autoRenameDownloadFile(
                            URLUtil.guessFileName(item.url, disposition, contentType), item.url, contentType);
                    if (betterName != null && !betterName.isEmpty() && !betterName.equals(item.fileName)) {
                        File renamed = uniqueFile(new File(outRef[0].getParentFile(), betterName));
                        item.fileName = renamed.getName();
                        item.path = renamed.getAbsolutePath();
                        outRef[0] = renamed;
                    }
                }
                item.categoryHint = inferDownloadCategoryFromData(item.fileName, item.url, contentType);
                unregisterDownloadConnection(item, probe);
                probe.disconnect();
                probe = null;

                int smartConnections = chooseSmartDownloadConnections(item, total, contentType);
                if (smartConnections <= 2 && !resumeCandidate) {
                    if (isCurrentDownloadRun(item, generation)) startLegacyTwoConnectionDownload(item, outRef[0]);
                    return;
                }
                int connections = resumeCandidate ? item.connectionCount : smartConnections;
                if (connections < 3) {
                    if (isCurrentDownloadRun(item, generation)) startLegacyTwoConnectionDownload(item, outRef[0]);
                    return;
                }
                int workerSlots = Math.max(1, Math.min(connections, smartConnections));
                boolean canResume = resumeCandidate
                        && hasDynamicResumeState(item, connections, total, outRef[0]);
                if (!canResume) {
                    initializeDynamicParts(item, connections, total);
                    item.downloadedBytes = 0;
                    item.progress = 0;
                    RandomAccessFile random = new RandomAccessFile(outRef[0], "rw");
                    random.setLength(total);
                    random.close();
                } else {
                    long resumed = 0;
                    for (int part = 1; part <= connections; part++) resumed += getDynamicPartDone(item, part);
                    item.downloadedBytes = resumed;
                    item.progress = (int) Math.min(99, resumed * 100 / Math.max(1, total));
                }
                item.totalBytes = total;
                item.connectionCount = connections;
                item.activeConnectionLimit = workerSlots;
                item.engineInfo = (canResume ? "Resume " : "") + getTurboLabel(item, workerSlots);
                item.failReason = "";
                item.rateLimiter.reset();
                resetTurboSampling(item);
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, item.engineInfo, true);

                AtomicLong done = new AtomicLong(item.downloadedBytes);
                AtomicBoolean ok = new AtomicBoolean(true);
                String[] workerError = new String[]{""};
                Semaphore limiter = new Semaphore(workerSlots);
                ArrayList<Thread> workers = new ArrayList<>();
                for (int part = 1; part <= connections; part++) {
                    final int partIndex = part;
                    final long rangeStart = getDynamicPartStart(item, part) + getDynamicPartDone(item, part);
                    final long rangeEnd = getDynamicPartEnd(item, part);
                    if (rangeStart > rangeEnd) continue;
                    Thread worker = new Thread(() -> {
                        boolean acquired = false;
                        try {
                            limiter.acquire();
                            acquired = true;
                            downloadRangeDynamic(item, outRef[0], rangeStart, rangeEnd, done, total,
                                    ok, workerError, partIndex, workerSlots, generation);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            ok.set(false);
                            setWorkerError(workerError, "Worker multipart terinterupsi");
                        } finally {
                            if (acquired) limiter.release();
                        }
                    }, "Yield-Part-" + partIndex);
                    workers.add(worker);
                    worker.start();
                }
                for (Thread worker : workers) worker.join();
                if (!isCurrentDownloadRun(item, generation)) return;
                if (item.pauseRequested || "paused".equals(item.status)) {
                    item.status = "paused";
                    resetDownloadSpeedState(item);
                    item.engineInfo = getTurboLabel(item, workerSlots) + " • dijeda";
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    showDownloadNotification(item, "Unduhan dijeda", false);
                    updateDownloadKeepAliveState();
                    return;
                }
                if (ok.get() && verifyMultipartComplete(item, connections, total, outRef[0])) {
                    completeDownload(item);
                    return;
                }

                String reason = getWorkerError(workerError, "Multipart tidak lengkap");
                boolean protocolFailure = isProtocolOrIdentityFailure(reason);
                if (protocolFailure || shouldFallbackTurboToStable(item)) {
                    item.turboTargetConnections = Math.max(1, getV3FallbackConnections(item));
                    item.turboProfile = item.turboTargetConnections >= 2
                            ? "auto fallback stable v3" : "auto fallback safe v3";
                    resetForCleanRestart(item, outRef[0]);
                    item.status = "running";
                    if (item.turboTargetConnections >= 2) startLegacyTwoConnectionDownload(item, outRef[0]);
                    else downloadSingle(item, outRef[0]);
                } else {
                    failDownload(item, reason);
                }
            } catch (Exception e) {
                if (probe != null) {
                    unregisterDownloadConnection(item, probe);
                    try { probe.disconnect(); } catch (Exception ignored) {}
                }
                if (!isCurrentDownloadRun(item, generation)) return;
                if (item.pauseRequested || "paused".equals(item.status)) return;
                boolean resume = outRef[0].exists() && item.downloadedBytes > 0;
                if (!resume || isProtocolOrIdentityFailure(e.getMessage())) {
                    resetForCleanRestart(item, outRef[0]);
                    item.status = "running";
                    startLegacyTwoConnectionDownload(item, outRef[0]);
                } else {
                    failDownload(item, e.getMessage());
                }
            }
        }, "Yield-Dynamic-Probe").start();
    }

    private void downloadRangeDynamic(DownloadItem item, File out, long start, long end,
                                      AtomicLong done, long total, AtomicBoolean ok,
                                      String[] workerError, int partIndex, int connections,
                                      int generation) {
        if (start > end || !isCurrentDownloadRun(item, generation)) return;
        HttpURLConnection connection = null;
        InputStream input = null;
        RandomAccessFile random = null;
        long expected = DownloadProtocol.expectedLength(start, end);
        long written = 0;
        try {
            connection = openDownloadConnection(item.url, item, "bytes=" + start + "-" + end);
            validateDownloadResponse(connection);
            DownloadProtocol.requireRange(connection, start, end, total);
            captureRemoteIdentity(item, connection);
            input = connection.getInputStream();
            registerDownloadStream(item, input);
            random = new RandomAccessFile(out, "rw");
            random.seek(start);
            byte[] buffer = new byte[chooseDownloadBufferSize(item.totalBytes)];
            while (written < expected) {
                if (!isCurrentDownloadRun(item, generation)
                        || item.pauseRequested || "paused".equals(item.status)) return;
                int request = (int) Math.min(buffer.length, expected - written);
                int length = input.read(buffer, 0, request);
                if (length < 0) break;
                random.write(buffer, 0, length);
                item.rateLimiter.acquire(length, downloadSpeedLimitKBps);
                written += length;
                addDynamicPartDone(item, partIndex, length);
                long current = done.addAndGet(length);
                item.downloadedBytes = current;
                updateDownloadSpeed(item, current);
                updateTurboPrediction(item, current);
                int percent = (int) Math.min(99, current * 100 / Math.max(1, total));
                if (percent != item.progress) {
                    item.progress = percent;
                    if (percent % 2 == 0 || percent >= 99) {
                        refreshDownloadPanel();
                        maybePersistDownloadProgress(item);
                        showDownloadNotification(item, getConnectionLabel(item) + " • " + percent
                                + "% • " + readableSpeed(item.speedBytesPerSecond), true);
                    }
                }
            }
            if (written != expected) throw new Exception("Range terputus: " + written + "/" + expected);
        } catch (Exception e) {
            if (isCurrentDownloadRun(item, generation)
                    && !item.pauseRequested && !"paused".equals(item.status)) {
                ok.set(false);
                setWorkerError(workerError, e.getMessage());
            }
        } finally {
            try { if (random != null) random.close(); } catch (Exception ignored) {}
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            unregisterDownloadStream(item, input);
            unregisterDownloadConnection(item, connection);
            try { if (connection != null) connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    private void setWorkerError(String[] holder, String reason) {
        synchronized (holder) {
            if (holder[0] == null || holder[0].isEmpty()) {
                holder[0] = reason == null || reason.isEmpty() ? "Koneksi terputus" : reason;
            }
        }
    }

    private String getWorkerError(String[] holder, String fallback) {
        synchronized (holder) {
            return holder[0] == null || holder[0].isEmpty() ? fallback : holder[0];
        }
    }

    private void startLegacyTwoConnectionDownload(DownloadItem item, File out) {
        final int generation = item.runGeneration;
        final boolean resumeAttempt = out.exists() && item.downloadedBytes > 0
                && item.connectionCount == DOWNLOAD_CONNECTIONS_STABLE;
        item.status = "running";
        item.pauseRequested = false;
        item.engineInfo = "Mengecek koneksi Range";
        refreshDownloadPanel();

        new Thread(() -> {
            final File[] outRef = new File[]{out};
            HttpURLConnection probe = null;
            try {
                if (!isCurrentDownloadRun(item, generation)) return;
                long previousTotal = item.totalBytes;
                probe = openDownloadConnection(item.url, item, "bytes=0-0");
                validateDownloadResponse(probe);
                DownloadProtocol.RangeInfo range = DownloadProtocol.requireRange(probe, 0, 0, -1);
                captureRemoteIdentity(item, probe);
                long total = range.total;
                String contentType = probe.getContentType();
                String disposition = probe.getHeaderField("Content-Disposition");
                if (!resumeAttempt) {
                    String betterName = autoRenameDownloadFile(
                            URLUtil.guessFileName(item.url, disposition, contentType), item.url, contentType);
                    if (betterName != null && !betterName.isEmpty() && !betterName.equals(item.fileName)) {
                        File renamed = uniqueFile(new File(outRef[0].getParentFile(), betterName));
                        item.fileName = renamed.getName();
                        item.path = renamed.getAbsolutePath();
                        outRef[0] = renamed;
                    }
                }
                item.categoryHint = inferDownloadCategoryFromData(item.fileName, item.url, contentType);
                int smart = item.turboTargetConnections > 0 ? item.turboTargetConnections
                        : chooseSmartDownloadConnections(item, total, contentType);
                unregisterDownloadConnection(item, probe);
                probe.disconnect();
                probe = null;

                if (total <= 1 || smart < 2) {
                    if (resumeAttempt) resetForCleanRestart(item, outRef[0]);
                    item.connectionCount = 1;
                    item.activeConnectionLimit = 1;
                    item.engineInfo = getTurboLabel(item, 1);
                    downloadSingle(item, outRef[0]);
                    return;
                }

                long firstEnd = total / 2L - 1L;
                long secondStart = firstEnd + 1L;
                boolean canResume = resumeAttempt && previousTotal == total
                        && outRef[0].length() == total
                        && item.part1Start == 0 && item.part1End == firstEnd
                        && item.part2Start == secondStart && item.part2End == total - 1
                        && item.part1Done >= 0 && item.part1Done <= firstEnd + 1
                        && item.part2Done >= 0 && item.part2Done <= total - secondStart;
                if (!canResume) {
                    item.part1Start = 0;
                    item.part1End = firstEnd;
                    item.part1Done = 0;
                    item.part2Start = secondStart;
                    item.part2End = total - 1;
                    item.part2Done = 0;
                    item.downloadedBytes = 0;
                    item.progress = 0;
                    RandomAccessFile random = new RandomAccessFile(outRef[0], "rw");
                    random.setLength(total);
                    random.close();
                } else {
                    item.downloadedBytes = item.part1Done + item.part2Done;
                    item.progress = (int) Math.min(99, item.downloadedBytes * 100 / Math.max(1, total));
                }
                item.totalBytes = total;
                item.connectionCount = 2;
                item.activeConnectionLimit = 2;
                item.engineInfo = (canResume ? "Resume " : "") + getTurboLabel(item, 2);
                item.failReason = "";
                item.rateLimiter.reset();
                resetTurboSampling(item);
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, item.engineInfo, true);

                AtomicLong done = new AtomicLong(item.downloadedBytes);
                AtomicBoolean ok = new AtomicBoolean(true);
                String[] workerError = new String[]{""};
                long range1Start = item.part1Start + item.part1Done;
                long range2Start = item.part2Start + item.part2Done;
                Thread first = new Thread(() -> downloadRange(item, outRef[0], range1Start,
                        item.part1End, done, total, ok, workerError, 1, generation), "Yield-Part-1");
                Thread second = new Thread(() -> downloadRange(item, outRef[0], range2Start,
                        item.part2End, done, total, ok, workerError, 2, generation), "Yield-Part-2");
                first.start();
                second.start();
                first.join();
                second.join();
                if (!isCurrentDownloadRun(item, generation)) return;
                if (item.pauseRequested || "paused".equals(item.status)) {
                    item.status = "paused";
                    resetDownloadSpeedState(item);
                    item.engineInfo = "Stable 2 koneksi • dijeda";
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    showDownloadNotification(item, "Unduhan dijeda", false);
                    updateDownloadKeepAliveState();
                    return;
                }
                if (ok.get() && verifyMultipartComplete(item, 2, total, outRef[0])) {
                    completeDownload(item);
                    return;
                }
                String reason = getWorkerError(workerError, "Download 2 koneksi tidak lengkap");
                boolean protocolFailure = isProtocolOrIdentityFailure(reason);
                if (protocolFailure) {
                    resetForCleanRestart(item, outRef[0]);
                    item.status = "running";
                    item.connectionCount = 1;
                    item.activeConnectionLimit = 1;
                    item.turboTargetConnections = 1;
                    item.turboProfile = "auto fallback safe v3";
                    downloadSingle(item, outRef[0]);
                } else {
                    failDownload(item, reason);
                }
            } catch (Exception e) {
                if (probe != null) {
                    unregisterDownloadConnection(item, probe);
                    try { probe.disconnect(); } catch (Exception ignored) {}
                }
                if (!isCurrentDownloadRun(item, generation)) return;
                if (item.pauseRequested || "paused".equals(item.status)) return;
                if (!resumeAttempt || isProtocolOrIdentityFailure(e.getMessage())) {
                    resetForCleanRestart(item, outRef[0]);
                    item.status = "running";
                    item.connectionCount = 1;
                    item.activeConnectionLimit = 1;
                    item.turboTargetConnections = 1;
                    item.turboProfile = "auto fallback safe v3";
                    downloadSingle(item, outRef[0]);
                } else {
                    failDownload(item, e.getMessage());
                }
            }
        }, "Yield-Stable-Probe").start();
    }

    private void downloadRange(DownloadItem item, File out, long start, long end,
                               AtomicLong done, long total, AtomicBoolean ok,
                               String[] workerError, int partIndex, int generation) {
        if (start > end || !isCurrentDownloadRun(item, generation)) return;
        HttpURLConnection connection = null;
        InputStream input = null;
        RandomAccessFile random = null;
        long expected = DownloadProtocol.expectedLength(start, end);
        long written = 0;
        try {
            connection = openDownloadConnection(item.url, item, "bytes=" + start + "-" + end);
            validateDownloadResponse(connection);
            DownloadProtocol.requireRange(connection, start, end, total);
            captureRemoteIdentity(item, connection);
            input = connection.getInputStream();
            registerDownloadStream(item, input);
            random = new RandomAccessFile(out, "rw");
            random.seek(start);
            byte[] buffer = new byte[chooseDownloadBufferSize(item.totalBytes)];
            while (written < expected) {
                if (!isCurrentDownloadRun(item, generation)
                        || item.pauseRequested || "paused".equals(item.status)) return;
                int request = (int) Math.min(buffer.length, expected - written);
                int length = input.read(buffer, 0, request);
                if (length < 0) break;
                random.write(buffer, 0, length);
                item.rateLimiter.acquire(length, downloadSpeedLimitKBps);
                written += length;
                synchronized (item.stateLock) {
                    if (partIndex == 1) item.part1Done = Math.min(item.part1End - item.part1Start + 1,
                            item.part1Done + length);
                    else item.part2Done = Math.min(item.part2End - item.part2Start + 1,
                            item.part2Done + length);
                }
                long current = done.addAndGet(length);
                item.downloadedBytes = current;
                updateDownloadSpeed(item, current);
                updateTurboPrediction(item, current);
                int percent = (int) Math.min(99, current * 100 / Math.max(1, total));
                if (percent != item.progress) {
                    item.progress = percent;
                    if (percent % 2 == 0 || percent >= 99) {
                        refreshDownloadPanel();
                        maybePersistDownloadProgress(item);
                        showDownloadNotification(item, "Stable 2 koneksi • " + percent
                                + "% • " + readableSpeed(item.speedBytesPerSecond), true);
                    }
                }
            }
            if (written != expected) throw new Exception("Range terputus: " + written + "/" + expected);
        } catch (Exception e) {
            if (isCurrentDownloadRun(item, generation)
                    && !item.pauseRequested && !"paused".equals(item.status)) {
                ok.set(false);
                setWorkerError(workerError, e.getMessage());
            }
        } finally {
            try { if (random != null) random.close(); } catch (Exception ignored) {}
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            unregisterDownloadStream(item, input);
            unregisterDownloadConnection(item, connection);
            try { if (connection != null) connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    private void startHlsDownload(DownloadItem item, File out) {
        final int generation = item.runGeneration;
        item.status = "running";
        item.pauseRequested = false;
        item.hlsDownload = true;
        item.connectionCount = 1;
        item.activeConnectionLimit = 1;
        item.engineInfo = "HLS/m3u8";
        item.categoryHint = "Video";
        refreshDownloadPanel();

        new Thread(() -> {
            File actualOutput = out;
            try {
                HlsPlaylistParser.Playlist playlist = resolveHlsPlaylist(item.url, item, generation, 0);
                if (!isCurrentDownloadRun(item, generation)) return;
                if (playlist.unsupportedEncryption) {
                    throw new Exception("Metode enkripsi HLS tidak didukung; download dihentikan agar file tidak korup");
                }
                if (playlist.segments.isEmpty()) throw new Exception("Playlist HLS kosong/tidak didukung");

                boolean fragmentedMp4 = playlist.initMap != null;
                String expectedExtension = fragmentedMp4 ? ".mp4" : ".ts";
                if (item.hlsCompletedSegments == 0 && item.hlsOutputBytes == 0
                        && !item.fileName.toLowerCase(Locale.US).endsWith(expectedExtension)) {
                    String base = item.fileName.replaceAll("(?i)\\.(m3u8|ts|mp4)$", "");
                    File renamed = uniqueFile(new File(out.getParentFile(), base + expectedExtension));
                    item.fileName = renamed.getName();
                    item.path = renamed.getAbsolutePath();
                    actualOutput = renamed;
                } else {
                    actualOutput = new File(item.path);
                }

                String fingerprint = buildHlsFingerprint(playlist);
                Map<String, byte[]> hlsKeyCache = new HashMap<>();
                boolean resumeValid = actualOutput.exists()
                        && fingerprint.equals(item.hlsPlaylistFingerprint)
                        && item.hlsCompletedSegments >= 0
                        && item.hlsCompletedSegments <= playlist.segments.size()
                        && item.hlsOutputBytes == actualOutput.length();
                if (!resumeValid) {
                    if (actualOutput.exists()) actualOutput.delete();
                    item.hlsCompletedSegments = 0;
                    item.hlsOutputBytes = 0;
                    item.hlsInitMapWritten = false;
                    item.hlsPlaylistFingerprint = fingerprint;
                }
                item.totalBytes = playlist.segments.size();
                item.downloadedBytes = item.hlsCompletedSegments;
                item.progress = (int) Math.min(99,
                        item.hlsCompletedSegments * 100L / Math.max(1, playlist.segments.size()));
                item.rateLimiter.reset();
                resetTurboSampling(item);
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, "HLS • " + playlist.segments.size() + " segmen", true);

                if (playlist.initMap != null && !item.hlsInitMapWritten) {
                    long mapStartLength = actualOutput.exists() ? actualOutput.length() : 0;
                    try {
                        appendHlsResource(item, actualOutput, playlist.initMap, generation, hlsKeyCache);
                    } catch (Exception e) {
                        try {
                            RandomAccessFile rollback = new RandomAccessFile(actualOutput, "rw");
                            rollback.setLength(mapStartLength);
                            rollback.close();
                            item.hlsOutputBytes = mapStartLength;
                        } catch (Exception ignored) {}
                        throw e;
                    }
                    item.hlsInitMapWritten = true;
                    item.hlsOutputBytes = actualOutput.length();
                    saveDownloadHistory();
                }

                for (int index = item.hlsCompletedSegments; index < playlist.segments.size(); index++) {
                    if (!isCurrentDownloadRun(item, generation)
                            || item.pauseRequested || "paused".equals(item.status)) break;
                    long segmentStartLength = actualOutput.exists() ? actualOutput.length() : 0;
                    try {
                        appendHlsResource(item, actualOutput, playlist.segments.get(index), generation, hlsKeyCache);
                    } catch (Exception e) {
                        try {
                            RandomAccessFile rollback = new RandomAccessFile(actualOutput, "rw");
                            rollback.setLength(segmentStartLength);
                            rollback.close();
                        } catch (Exception ignored) {}
                        throw e;
                    }
                    item.hlsCompletedSegments = index + 1;
                    item.hlsOutputBytes = actualOutput.length();
                    item.downloadedBytes = item.hlsCompletedSegments;
                    item.progress = (int) Math.min(99,
                            item.hlsCompletedSegments * 100L / Math.max(1, playlist.segments.size()));
                    updateDownloadSpeed(item, item.hlsOutputBytes);
                    refreshDownloadPanel();
                    maybePersistDownloadProgress(item);
                    if (index % 3 == 0 || index == playlist.segments.size() - 1) {
                        showDownloadNotification(item, "HLS • " + item.progress + "% • "
                                + readableSpeed(item.speedBytesPerSecond), true);
                    }
                }
                if (!isCurrentDownloadRun(item, generation)) return;
                if (item.pauseRequested || "paused".equals(item.status)) {
                    item.status = "paused";
                    resetDownloadSpeedState(item);
                    item.engineInfo = "HLS • dijeda pada segmen " + item.hlsCompletedSegments;
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    showDownloadNotification(item, "HLS dijeda", false);
                    updateDownloadKeepAliveState();
                } else if (item.hlsCompletedSegments == playlist.segments.size()
                        && actualOutput.exists() && actualOutput.length() > 0) {
                    item.hlsOutputBytes = actualOutput.length();
                    completeDownload(item);
                } else {
                    failDownload(item, "HLS tidak lengkap");
                }
            } catch (Exception e) {
                if (!isCurrentDownloadRun(item, generation)) return;
                if (item.pauseRequested || "paused".equals(item.status)) {
                    item.status = "paused";
                    resetDownloadSpeedState(item);
                    item.engineInfo = "HLS • dijeda";
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    showDownloadNotification(item, "HLS dijeda", false);
                    updateDownloadKeepAliveState();
                } else {
                    failDownload(item, e.getMessage());
                }
            }
        }, "Yield-HLS").start();
    }

    private HlsPlaylistParser.Playlist resolveHlsPlaylist(String playlistUrl, DownloadItem item,
                                                          int generation, int depth) throws Exception {
        if (depth > 5) throw new Exception("Terlalu banyak level playlist HLS");
        String text = readUrlText(playlistUrl, item, generation);
        HlsPlaylistParser.Playlist playlist = HlsPlaylistParser.parse(playlistUrl, text);
        if (!playlist.variants.isEmpty()) {
            return resolveHlsPlaylist(playlist.variants.get(0).url, item, generation, depth + 1);
        }
        return playlist;
    }

    private String readUrlText(String url, DownloadItem item, int generation) throws Exception {
        HttpURLConnection connection = null;
        InputStream input = null;
        BufferedReader reader = null;
        try {
            if (!isCurrentDownloadRun(item, generation)) throw new Exception("Sesi download berubah");
            connection = openDownloadConnection(url, item, "");
            validateDownloadResponse(connection);
            input = connection.getInputStream();
            registerDownloadStream(item, input);
            reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!isCurrentDownloadRun(item, generation)
                        || item.pauseRequested || "paused".equals(item.status)) {
                    throw new Exception("Sesi download berubah");
                }
                text.append(line).append('\n');
            }
            return text.toString();
        } finally {
            try { if (reader != null) reader.close(); } catch (Exception ignored) {}
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            unregisterDownloadStream(item, input);
            unregisterDownloadConnection(item, connection);
            try { if (connection != null) connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    private void appendHlsResource(DownloadItem item, File output,
                                   HlsPlaylistParser.Resource resource, int generation,
                                   Map<String, byte[]> keyCache) throws Exception {
        HttpURLConnection connection = null;
        InputStream input = null;
        RandomAccessFile random = null;
        long expected = -1;
        long networkBytes = 0;
        try {
            String rangeHeader = "";
            if (resource.range != null) {
                if (resource.key != null) {
                    throw new Exception("AES-128 HLS dengan byte-range belum didukung secara aman");
                }
                rangeHeader = "bytes=" + resource.range.start + "-" + resource.range.end;
                expected = resource.range.end - resource.range.start + 1L;
            }
            connection = openDownloadConnection(resource.url, item, rangeHeader);
            validateDownloadResponse(connection);
            if (resource.range != null) {
                DownloadProtocol.requireRange(connection, resource.range.start, resource.range.end, -1);
            }
            input = connection.getInputStream();
            registerDownloadStream(item, input);
            random = new RandomAccessFile(output, "rw");
            random.seek(random.length());

            if (resource.key != null) {
                byte[] encrypted = readHlsResourceBytes(item, input, expected, generation);
                networkBytes = encrypted.length;
                byte[] key = getHlsKeyBytes(item, resource.key.url, generation, keyCache);
                byte[] plain = HlsAes128.decrypt(encrypted, key,
                        resource.key.explicitIv, resource.sequence);
                random.write(plain);
                item.hlsOutputBytes = random.length();
                updateDownloadSpeed(item, item.hlsOutputBytes);
            } else {
                byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
                while (expected < 0 || networkBytes < expected) {
                    if (!isCurrentDownloadRun(item, generation)
                            || item.pauseRequested || "paused".equals(item.status)) {
                        throw new Exception("Sesi download berubah");
                    }
                    int request = expected < 0 ? buffer.length
                            : (int) Math.min(buffer.length, expected - networkBytes);
                    int length = input.read(buffer, 0, request);
                    if (length < 0) break;
                    random.write(buffer, 0, length);
                    item.rateLimiter.acquire(length, downloadSpeedLimitKBps);
                    networkBytes += length;
                    item.hlsOutputBytes = random.length();
                    updateDownloadSpeed(item, item.hlsOutputBytes);
                }
            }
            if (expected >= 0 && networkBytes != expected) {
                throw new Exception("Byte-range HLS tidak lengkap");
            }
        } finally {
            try { if (random != null) random.close(); } catch (Exception ignored) {}
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            unregisterDownloadStream(item, input);
            unregisterDownloadConnection(item, connection);
            try { if (connection != null) connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    private byte[] readHlsResourceBytes(DownloadItem item, InputStream input,
                                        long expected, int generation) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[DOWNLOAD_BUFFER_SIZE];
        long readTotal = 0;
        while (expected < 0 || readTotal < expected) {
            if (!isCurrentDownloadRun(item, generation)
                    || item.pauseRequested || "paused".equals(item.status)) {
                throw new Exception("Sesi download berubah");
            }
            int request = expected < 0 ? chunk.length : (int) Math.min(chunk.length, expected - readTotal);
            int length = input.read(chunk, 0, request);
            if (length < 0) break;
            item.rateLimiter.acquire(length, downloadSpeedLimitKBps);
            buffer.write(chunk, 0, length);
            readTotal += length;
            if (readTotal > 128L * 1024L * 1024L) {
                throw new Exception("Segmen HLS terenkripsi terlalu besar");
            }
        }
        if (expected >= 0 && readTotal != expected) throw new Exception("Segmen HLS tidak lengkap");
        return buffer.toByteArray();
    }

    private byte[] getHlsKeyBytes(DownloadItem item, String keyUrl, int generation,
                                  Map<String, byte[]> cache) throws Exception {
        byte[] cached = cache.get(keyUrl);
        if (cached != null) return cached;
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            connection = openDownloadConnection(keyUrl, item, "");
            validateDownloadResponse(connection);
            long contentLength = connection.getContentLengthLong();
            if (contentLength > 0 && contentLength != 16) {
                throw new Exception("Kunci AES-128 HLS tidak valid");
            }
            input = connection.getInputStream();
            registerDownloadStream(item, input);
            ByteArrayOutputStream keyBuffer = new ByteArrayOutputStream(17);
            byte[] chunk = new byte[17];
            while (keyBuffer.size() <= 16) {
                if (!isCurrentDownloadRun(item, generation)
                        || item.pauseRequested || "paused".equals(item.status)) {
                    throw new Exception("Sesi download berubah");
                }
                int length = input.read(chunk, 0, Math.min(chunk.length, 17 - keyBuffer.size()));
                if (length < 0) break;
                item.rateLimiter.acquire(length, downloadSpeedLimitKBps);
                keyBuffer.write(chunk, 0, length);
            }
            byte[] key = keyBuffer.toByteArray();
            if (key.length != 16) throw new Exception("Kunci AES-128 HLS tidak valid");
            cache.put(keyUrl, key);
            return key;
        } finally {
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            unregisterDownloadStream(item, input);
            unregisterDownloadConnection(item, connection);
            try { if (connection != null) connection.disconnect(); } catch (Exception ignored) {}
        }
    }

private String buildHlsFingerprint(HlsPlaylistParser.Playlist playlist) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        if (playlist.initMap != null) updateHlsFingerprint(digest, playlist.initMap);
        for (HlsPlaylistParser.Resource segment : playlist.segments) updateHlsFingerprint(digest, segment);
        byte[] hash = digest.digest();
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte value : hash) hex.append(String.format(Locale.US, "%02x", value & 0xff));
        return hex.toString();
    }

    private void updateHlsFingerprint(MessageDigest digest, HlsPlaylistParser.Resource resource) {
        String normalized = normalizeUrlForFingerprint(resource.url);
        String range = resource.range == null ? "" : "@" + resource.range.start + "-" + resource.range.end;
        String key = "";
        if (resource.key != null) {
            key = "#key=" + normalizeUrlForFingerprint(resource.key.url)
                    + "#iv=" + bytesToHex(resource.key.explicitIv)
                    + "#seq=" + resource.sequence;
        }
        digest.update((normalized + range + key + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] value) {
        if (value == null) return "";
        StringBuilder hex = new StringBuilder(value.length * 2);
        for (byte item : value) hex.append(String.format(Locale.US, "%02x", item & 0xff));
        return hex.toString();
    }

    private String normalizeUrlForFingerprint(String value) {
        try {
            URI uri = new URI(value);
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null).toString();
        } catch (Exception ignored) {
            int question = value == null ? -1 : value.indexOf('?');
            return question >= 0 ? value.substring(0, question) : String.valueOf(value);
        }
    }

    private void applySpeedLimit(DownloadItem item, int bytesRead) throws InterruptedException {
        if (item != null) item.rateLimiter.acquire(bytesRead, downloadSpeedLimitKBps);
    }

    private void downloadSingle(DownloadItem item, File out) {
        final int generation = item.runGeneration;
        if (!isCurrentDownloadRun(item, generation)) return;
        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;
        try {
            boolean resume = out.exists() && out.length() > 0 && item.downloadedBytes > 0
                    && item.connectionCount == 1;
            long resumeFrom = resume ? Math.min(item.downloadedBytes, out.length()) : 0;
            connection = openDownloadConnection(item.url, item,
                    resumeFrom > 0 ? "bytes=" + resumeFrom + "-" : "");
            validateDownloadResponse(connection);
            int code = connection.getResponseCode();
            boolean append = false;
            long total;
            if (resumeFrom > 0 && code == HttpURLConnection.HTTP_PARTIAL) {
                DownloadProtocol.RangeInfo range = DownloadProtocol.requireRangeFrom(connection,
                        resumeFrom, item.totalBytes);
                captureRemoteIdentity(item, connection);
                append = true;
                total = range.total > 0 ? range.total : item.totalBytes;
            } else {
                if (resumeFrom > 0) {
                    resumeFrom = 0;
                    item.downloadedBytes = 0;
                    item.progress = 0;
                    item.etag = "";
                    item.lastModified = "";
                }
                captureRemoteIdentity(item, connection);
                long contentLength = connection.getContentLengthLong();
                total = contentLength > 0 ? contentLength : -1;
            }

            String disposition = connection.getHeaderField("Content-Disposition");
            if (!append) {
                String betterName = autoRenameDownloadFile(
                        URLUtil.guessFileName(item.url, disposition, connection.getContentType()),
                        item.url, connection.getContentType());
                if (betterName != null && !betterName.isEmpty() && !betterName.equals(item.fileName)) {
                    File renamed = uniqueFile(new File(out.getParentFile(), betterName));
                    item.fileName = renamed.getName();
                    item.path = renamed.getAbsolutePath();
                    out = renamed;
                }
            }
            item.categoryHint = inferDownloadCategoryFromData(item.fileName, item.url, connection.getContentType());
            item.connectionCount = 1;
            item.activeConnectionLimit = 1;
            item.engineInfo = getTurboLabel(item, 1);
            item.totalBytes = total;
            item.failReason = "";
            item.rateLimiter.reset();
            input = connection.getInputStream();
            registerDownloadStream(item, input);
            output = new FileOutputStream(out, append);
            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            long done = resumeFrom;
            item.lastSpeedTimeMs = 0;
            item.lastSpeedBytes = done;
            resetTurboSampling(item);
            int length;
            while ((length = input.read(buffer)) != -1) {
                if (!isCurrentDownloadRun(item, generation)
                        || item.pauseRequested || "paused".equals(item.status)) break;
                output.write(buffer, 0, length);
                applySpeedLimit(item, length);
                done += length;
                item.downloadedBytes = done;
                updateDownloadSpeed(item, done);
                updateTurboPrediction(item, done);
                if (total > 0) {
                    if (done > total) throw new Exception("File melebihi ukuran server");
                    int percent = (int) Math.min(99, done * 100 / Math.max(1, total));
                    if (percent != item.progress) {
                        item.progress = percent;
                        if (percent % 2 == 0 || percent >= 99) {
                            refreshDownloadPanel();
                            maybePersistDownloadProgress(item);
                            showDownloadNotification(item, "Safe 1 koneksi • " + percent
                                    + "% • " + readableSpeed(item.speedBytesPerSecond), true);
                        }
                    }
                }
            }
            output.flush();
            output.close();
            output = null;
            if (!isCurrentDownloadRun(item, generation)) return;
            if (item.pauseRequested || "paused".equals(item.status)) {
                item.status = "paused";
                resetDownloadSpeedState(item);
                item.engineInfo = "Safe 1 koneksi • dijeda";
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, "Unduhan dijeda", false);
                updateDownloadKeepAliveState();
            } else {
                if (total > 0 && (done != total || !out.exists() || out.length() != total)) {
                    throw new Exception("File tidak lengkap: " + done + "/" + total);
                }
                if (!out.exists() || out.length() <= 0) throw new Exception("File hasil download kosong");
                item.downloadedBytes = out.length();
                completeDownload(item);
            }
        } catch (Exception e) {
            if (!isCurrentDownloadRun(item, generation)) return;
            if (isProtocolOrIdentityFailure(e.getMessage()) && !item.pauseRequested) {
                resetForCleanRestart(item, out);
                item.status = "running";
                item.connectionCount = 1;
                item.activeConnectionLimit = 1;
                downloadSingle(item, out);
                return;
            }
            if (item.pauseRequested || "paused".equals(item.status)) {
                item.status = "paused";
                resetDownloadSpeedState(item);
                item.engineInfo = "Safe 1 koneksi • dijeda";
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, "Unduhan dijeda", false);
                updateDownloadKeepAliveState();
            } else {
                failDownload(item, e.getMessage());
            }
        } finally {
            try { if (output != null) output.close(); } catch (Exception ignored) {}
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            unregisterDownloadStream(item, input);
            unregisterDownloadConnection(item, connection);
            try { if (connection != null) connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    private void completeDownload(DownloadItem item) {
        if (item == null || "removed".equals(item.status) || "completed".equals(item.status)) return;
        synchronized (item.stateLock) {
            if (item.finalizationQueued) return;
            item.finalizationQueued = true;
            item.pauseRequested = false;
            item.retryCount = 0;
            resetDownloadSpeedState(item);
            item.status = "verifying";
            item.finalizeProgress = 5;
            item.finalizeBytes = 0L;
            item.finalizeTotalBytes = Math.max(1L, item.downloadedBytes);
            item.engineInfo = "Memverifikasi file";
        }
        refreshDownloadPanel();
        showDownloadNotification(item, "Memverifikasi file…", true);

        try {
            DOWNLOAD_FINALIZE_EXECUTOR.execute(() -> performCompleteDownload(item));
        } catch (RuntimeException error) {
            item.finalizationQueued = false;
            failDownload(item, "Finalisasi tidak dapat dijalankan: " + safeText(error.getMessage()));
        }
    }

    /** Performs all file verification and multi-gigabyte export work away from the UI thread. */
    private void performCompleteDownload(DownloadItem item) {
        boolean completed = false;
        try {
            if (item == null || "removed".equals(item.status)) return;
            stopActiveDownloadTransports(item);
            saveDownloadHistory();

            File source = item.path == null ? null : new File(item.path);
            if (source == null || !source.exists() || source.length() <= 0) {
                failDownload(item, "File hasil download tidak ditemukan");
                return;
            }
            long sourceLength = source.length();
            if (!item.hlsDownload && item.totalBytes > 0 && sourceLength != item.totalBytes) {
                failDownload(item, "Ukuran file tidak sesuai setelah download");
                return;
            }
            if ("removed".equals(item.status)) return;

            synchronized (item.stateLock) {
                item.status = "saving";
                item.finalizeProgress = 0;
                item.finalizeBytes = 0L;
                item.finalizeTotalBytes = Math.max(1L, sourceLength);
                item.engineInfo = "Menyimpan ke Downloads";
            }
            saveDownloadHistory();
            refreshDownloadPanel();
            showDownloadNotification(item, "Menyimpan ke Downloads…", true);

            boolean exported = exportCompletedDownload(item, source);
            if ("removed".equals(item.status)) return;

            synchronized (item.stateLock) {
                item.progress = 100;
                item.finalizeProgress = 100;
                item.finalizeBytes = Math.max(item.finalizeBytes, sourceLength);
                item.status = "completed";
                item.engineInfo = exported
                        ? getConnectionLabel(item) + " • tersimpan"
                        : getConnectionLabel(item) + " • tersimpan di aplikasi";
            }
            saveDownloadHistory();
            refreshDownloadPanel();
            updateDownloadKeepAliveState();
            showDownloadNotification(item, exported
                    ? "Unduhan selesai" : "Unduhan selesai • penyimpanan lokal", false);
            runOnUiThread(() -> QuietToast.makeText(this, "Unduhan selesai: " + item.fileName,
                    QuietToast.LENGTH_SHORT).show());
            completed = true;
        } catch (Throwable error) {
            if (item != null && !"removed".equals(item.status)) {
                failDownload(item, "Finalisasi gagal: " + safeText(error.getMessage()));
            }
        } finally {
            if (item != null) item.finalizationQueued = false;
            if (completed) mainHandler.postDelayed(this::pumpDownloadQueue, 180L);
        }
    }

    private boolean exportCompletedDownload(DownloadItem item, File source) {
        if (item == null || source == null || !source.exists()) return false;
        try {
            Uri exported = selectedDownloadTreeUri != null && !selectedDownloadTreeUri.isEmpty()
                    ? copyFileToSelectedTree(source, item.fileName, item)
                    : copyFileToDefaultDownloads(source, item.fileName, item);
            if (exported == null) return false;
            item.publicUri = exported.toString();
            if (source.exists() && source.delete()) item.path = "";
            return true;
        } catch (Exception e) {
            item.failReason = "Gagal menyalin ke Downloads: " + safeText(e.getMessage());
            return false;
        }
    }

    private Uri copyFileToSelectedTree(File source, String fileName, DownloadItem item) throws Exception {
        Uri treeUri = Uri.parse(selectedDownloadTreeUri);
        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri,
                DocumentsContract.getTreeDocumentId(treeUri));
        Uri newFile = DocumentsContract.createDocument(getContentResolver(), documentUri,
                getMimeTypeForName(fileName), fileName);
        if (newFile == null) throw new Exception("Tidak bisa membuat file di folder HP");
        try {
            copyFileToUri(source, newFile, item);
            return newFile;
        } catch (Exception e) {
            try { DocumentsContract.deleteDocument(getContentResolver(), newFile); }
            catch (Exception ignored) {}
            throw e;
        }
    }

    private Uri copyFileToDefaultDownloads(File source, String fileName, DownloadItem item) throws Exception {
        String mime = getMimeTypeForName(fileName);
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/Yield Browser");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("Tidak bisa membuat file di Downloads");
            try {
                copyFileToUri(source, uri, item);
                ContentValues done = new ContentValues();
                done.put(MediaStore.MediaColumns.IS_PENDING, 0);
                getContentResolver().update(uri, done, null, null);
                return uri;
            } catch (Exception e) {
                try { getContentResolver().delete(uri, null, null); } catch (Exception ignored) {}
                throw e;
            }
        }
        File directory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "Yield Browser");
        if (!directory.exists()) directory.mkdirs();
        File target = uniqueFile(new File(directory, fileName));
        try {
            copyFileToFile(source, target, item);
            return Uri.fromFile(target);
        } catch (Exception e) {
            try { if (target.exists()) target.delete(); } catch (Exception ignored) {}
            throw e;
        }
    }

    private void copyFileToUri(File source, Uri uri, DownloadItem item) throws Exception {
        try (InputStream input = new FileInputStream(source);
             OutputStream output = getContentResolver().openOutputStream(uri, "w")) {
            if (output == null) throw new Exception("Output folder tidak tersedia");
            copyWithFinalizeProgress(input, output, source.length(), item);
        }
    }

    private void copyFileToFile(File source, File target, DownloadItem item) throws Exception {
        try (InputStream input = new FileInputStream(source);
             OutputStream output = new FileOutputStream(target)) {
            copyWithFinalizeProgress(input, output, source.length(), item);
        }
    }

    private void copyWithFinalizeProgress(InputStream input, OutputStream output,
                                          long totalBytes, DownloadItem item) throws Exception {
        byte[] buffer = new byte[DownloadFinalizationPolicy.COPY_BUFFER_BYTES];
        long copied = 0L;
        long lastProgressUpdateMs = 0L;
        long lastPersistMs = 0L;
        long lastNotificationMs = 0L;
        long lastCooperativePauseBytes = 0L;
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (item != null && (item.pauseRequested || "removed".equals(item.status))) {
                throw new Exception("Penyimpanan dibatalkan");
            }
            output.write(buffer, 0, read);
            copied += read;
            long now = System.currentTimeMillis();

            if (item != null && DownloadFinalizationPolicy.isUpdateDue(now, lastProgressUpdateMs,
                    DownloadFinalizationPolicy.PROGRESS_UPDATE_INTERVAL_MS, copied, totalBytes)) {
                item.finalizeBytes = copied;
                item.finalizeTotalBytes = Math.max(1L, totalBytes);
                item.finalizeProgress = DownloadFinalizationPolicy.progressPercent(copied, totalBytes);
                refreshDownloadPanel();
                lastProgressUpdateMs = now;
            }

            if (item != null && DownloadFinalizationPolicy.isUpdateDue(now, lastPersistMs,
                    DownloadFinalizationPolicy.HISTORY_PERSIST_INTERVAL_MS, copied, totalBytes)) {
                item.lastProgressPersistMs = now;
                saveDownloadHistory();
                lastPersistMs = now;
            }

            if (item != null && DownloadFinalizationPolicy.isUpdateDue(now, lastNotificationMs,
                    DownloadFinalizationPolicy.NOTIFICATION_UPDATE_INTERVAL_MS, copied, totalBytes)) {
                showDownloadNotification(item,
                        "Menyimpan ke Downloads… " + item.finalizeProgress + "%", true);
                lastNotificationMs = now;
            }

            // A tiny cooperative pause gives the UI and storage provider a chance to run on
            // devices with slower flash without materially reducing large-file throughput.
            if (copied - lastCooperativePauseBytes
                    >= DownloadFinalizationPolicy.COOPERATIVE_PAUSE_EVERY_BYTES) {
                android.os.SystemClock.sleep(DownloadFinalizationPolicy.COOPERATIVE_PAUSE_MS);
                lastCooperativePauseBytes = copied;
            }
        }
        output.flush();
        if (item != null) {
            item.finalizeBytes = copied;
            item.finalizeTotalBytes = Math.max(1L, totalBytes);
            item.finalizeProgress = 100;
            refreshDownloadPanel();
        }
    }


    private String getMimeTypeForName(String fileName) {
        try {
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
            if (extension != null && !extension.isEmpty()) {
                String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        extension.toLowerCase(Locale.US));
                if (mime != null) return mime;
            }
        } catch (Exception ignored) {}
        return "application/octet-stream";
    }

    private void failDownload(DownloadItem item, String reason) {
        if (item == null || "removed".equals(item.status)) return;
        stopActiveDownloadTransports(item);
        if (item.connectionCount >= 3) {
            item.turboRetryPenalty++;
            if (shouldFallbackTurboToStable(item)) {
                int fallback = getV3FallbackConnections(item);
                item.turboTargetConnections = fallback;
                item.turboProfile = fallback >= 3 ? "auto fallback balanced v3"
                        : fallback >= 2 ? "auto fallback stable v3" : "auto fallback safe v3";
            }
        }

        if (downloadAutoRetry && !isPermanentDownloadError(reason)
                && !item.pauseRequested && item.retryCount < DOWNLOAD_RETRY_MAX) {
            item.retryCount++;
            item.runGeneration++;
            final int retryGeneration = item.runGeneration;
            item.status = "running";
            item.pauseRequested = false;
            resetDownloadSpeedState(item);
            item.failReason = reason == null ? "Koneksi terputus" : reason;
            item.engineInfo = "Retry otomatis " + item.retryCount + "/" + DOWNLOAD_RETRY_MAX;
            saveDownloadHistory();
            refreshDownloadPanel();
            updateDownloadKeepAliveState();
            showDownloadNotification(item, item.engineInfo, true);
            mainHandler.postDelayed(() -> {
                if (!isCurrentDownloadRun(item, retryGeneration) || !"running".equals(item.status)) return;
                try {
                    startTwoConnectionDownload(item, new File(item.path));
                } catch (Exception e) {
                    failDownload(item, e.getMessage());
                }
            }, 1500L * item.retryCount);
            return;
        }

        item.runGeneration++;
        item.status = "failed";
        item.pauseRequested = false;
        resetDownloadSpeedState(item);
        item.failReason = reason == null ? "Koneksi/server menolak unduhan" : reason;
        item.engineInfo = getConnectionLabel(item) + " • terputus/gagal";
        saveDownloadHistory();
        refreshDownloadPanel();
        updateDownloadKeepAliveState();
        showDownloadNotification(item, "Unduhan gagal • buka detail", false);
        runOnUiThread(() -> QuietToast.makeText(this,
                "Unduhan gagal. Buka Download untuk reload/detail.", QuietToast.LENGTH_LONG).show());
        mainHandler.postDelayed(this::pumpDownloadQueue, 300);
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_DOWNLOADS,
                    "Yield Downloads", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Progress unduhan Yield Browser");
            channel.setSound(null, null);
            channel.enableVibration(false);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                manager.deleteNotificationChannel("yield_download_engine");
            }
        }
    }

    private void cancelDownloadNotification(DownloadItem item) {
        if (item == null) return;
        try {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.cancel(item.id);
        } catch (Exception ignored) {}
    }

    private void showDownloadNotification(DownloadItem item, String text, boolean ongoing) {
        if (item == null || "removed".equals(item.status)) return;

        // A running download is represented by the foreground service's progress notification.
        // Do not create a second "engine/background" notification or a duplicate per-item one.
        if (isActiveDownloadStatus(item.status) && ongoing) {
            updateDownloadKeepAliveState();
            return;
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pending;
        if (dedicatedPrivateProfile) {
            Intent openPrivate = new Intent(this, PrivateBrowserActivity.class);
            openPrivate.putExtra("open_downloads", true);
            openPrivate.putExtra("download_id", item.id);
            openPrivate.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            pending = PendingIntent.getActivity(this, item.id + 12000, openPrivate, flags);
        } else {
            Intent intent = new Intent(this, DownloadOpenReceiver.class);
            intent.setAction(ACTION_OPEN_DOWNLOADS);
            intent.putExtra("open_downloads", true);
            intent.putExtra("download_id", item.id);
            pending = PendingIntent.getBroadcast(this, item.id + 12000, intent, flags);
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_DOWNLOADS)
                : new Notification.Builder(this);
        String line = text;
        if ("paused".equals(item.status)) line = "Dijeda • " + getConnectionLabel(item);
        builder.setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(item.fileName)
                .setContentText(line)
                .setContentIntent(pending)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setOnlyAlertOnce(true)
                .setAutoCancel(!ongoing)
                .setOngoing(ongoing)
                .setVisibility(Notification.VISIBILITY_PRIVATE);
        if (ongoing || "paused".equals(item.status)) {
            builder.setProgress(100, getVisibleDownloadProgressPercent(item), false);
        }
        manager.notify(item.id, builder.build());
    }

    private void saveDownloadHistory() {
        synchronized (downloadHistoryLock) {
            ArrayList<String> rows = new ArrayList<>();
            synchronized (downloadItems) {
                for (DownloadItem item : downloadItems) {
                    if (DownloadHistoryCodec.shouldPersist(item)) {
                        rows.add(DownloadHistoryCodec.serialize(item));
                    }
                }
            }
            StringBuilder ordered = new StringBuilder();
            for (String row : rows) {
                if (ordered.length() > 0) ordered.append('\n');
                ordered.append(row);
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_DOWNLOAD_HISTORY_ORDERED_V2, ordered.toString())
                    .remove(KEY_DOWNLOAD_HISTORY)
                    .apply();
        }
    }

    private void loadDownloadHistory() {
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        ArrayList<String> rows = new ArrayList<>();
        String ordered = preferences.getString(KEY_DOWNLOAD_HISTORY_ORDERED_V2, "");
        if (ordered != null && !ordered.isEmpty()) {
            Collections.addAll(rows, ordered.split("\\n"));
        } else {
            Set<String> legacy = preferences.getStringSet(KEY_DOWNLOAD_HISTORY, new LinkedHashSet<>());
            if (legacy != null) rows.addAll(legacy);
        }
        synchronized (downloadItems) {
            downloadItems.clear();
            for (String row : rows) {
                DownloadItem item = DownloadHistoryCodec.deserialize(row, nextDownloadId++);
                if (item == null) continue;
                try {
                    if (item.path != null && !item.path.isEmpty()) {
                        File savedFile = new File(item.path);
                        boolean missing = !savedFile.exists() && ("completed".equals(item.status)
                                || "paused".equals(item.status) || "failed".equals(item.status));
                        if (missing) cancelDownloadNotification(item);
                    }
                } catch (RuntimeException ignored) {}
                if (item.categoryHint == null || item.categoryHint.isEmpty()) {
                    item.categoryHint = inferDownloadCategoryFromData(item.fileName, item.url, "");
                }
                downloadItems.add(item);
            }
        }
        saveDownloadHistory();
        updateDownloadKeepAliveState();
    }


    private void toggleBookmark() {
        String url = getEffectiveCurrentUrl();
        if (url == null || url.length() == 0) {
            QuietToast.makeText(this, "Belum ada situs untuk dibookmark", QuietToast.LENGTH_SHORT).show();
            return;
        }
        BookmarkItemData existing = findBookmarkByUrl(url);
        if (existing != null) {
            bookmarkData.remove(existing);
            saveBookmarkData();
            QuietToast.makeText(this, "Bookmark dihapus", QuietToast.LENGTH_SHORT).show();
        } else {
            String title = (webView != null && webView.getTitle() != null && webView.getTitle().trim().length() > 0) ? webView.getTitle().trim() : guessLabelFromUrl(url);
            bookmarkData.add(0, new BookmarkItemData(title, url, "Bookmark seluler", System.currentTimeMillis()));
            saveBookmarkData();
            QuietToast.makeText(this, "Situs ditambahkan ke bookmark", QuietToast.LENGTH_SHORT).show();
        }
        updateTopActionStates();
    }

    private void showBookmarkList() {
        showBookmarkHomePanel();
    }

    private void toggleTranslate() {
        showTranslateOptionsDialog();
    }

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

    private void showTranslateLanguageDialog() {
        String[] labels = new String[]{"Indonesia", "Inggris", "Jepang", "Korea", "Mandarin", "Arab", "Jerman", "Prancis", "Spanyol", "Rusia", "Melayu"};
        String[] codes = new String[]{"id", "en", "ja", "ko", "zh-CN", "ar", "de", "fr", "es", "ru", "ms"};

        int checked = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(translateTargetLang)) {
                checked = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Pilih bahasa target")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    translateTargetLang = codes[which];
                    translateTargetLabel = labels[which];
                    saveSettings();
                    QuietToast.makeText(this, "Bahasa translate: " + translateTargetLabel, QuietToast.LENGTH_SHORT).show();

                    if (translateEnabled && compatibleTranslateActive && !translateManuallyDisabled) {
                        clearCompatibleTranslationMarks();
                        final int token = translateSessionToken;
                        mainHandler.postDelayed(() -> translatePageCompatible(token), 250);
                    } else if (translateEnabled && !translateManuallyDisabled && isGoogleTranslatedUrl(webView != null ? webView.getUrl() : "")) {
                        String raw = lastTranslateOriginalUrl != null && lastTranslateOriginalUrl.length() > 0 ? lastTranslateOriginalUrl : getOriginalForTranslate(webView.getUrl());
                        if (raw != null && raw.length() > 0) loadTranslatedPage(raw);
                    }

                    updateTopActionStates();
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showTranslateOptionsDialog() {
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

    private String getOriginalForTranslate(String maybeUrl) {
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

    private boolean isGoogleTranslatedUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.US);
        return lower.contains("translate.google.") || lower.contains(".translate.goog") || lower.contains("_x_tr_sl=");
    }

    private void detectTranslateProxyBlocked(String url) {
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

    private void startCompatibleTranslateSession(String originalUrl) {
        translateEnabled = true;
        compatibleTranslateActive = true;
        translateManuallyDisabled = false;
        translateSessionToken++;
        if (originalUrl != null && originalUrl.length() > 0) lastTranslateOriginalUrl = originalUrl;
    }

    private void startGoogleTranslateSession(String originalUrl) {
        translateEnabled = true;
        compatibleTranslateActive = false;
        translateManuallyDisabled = false;
        translateSessionToken++;
        if (originalUrl != null && originalUrl.length() > 0) lastTranslateOriginalUrl = originalUrl;
    }

    private void disableTranslateAndRestore(String currentUrl) {
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

    private boolean isCompatibleTranslateAllowed(int token) {
        return token == translateSessionToken && translateEnabled && compatibleTranslateActive && !translateManuallyDisabled;
    }

    private void runJsOnCurrentPage(String script) {
        if (webView == null || script == null || script.length() == 0) return;
        try {
            String code = script;
            if (code.startsWith("javascript:")) code = code.substring("javascript:".length());
            webView.evaluateJavascript(code, null);
        } catch (Exception ignored) {
        }
    }

    private void translatePageCompatible() {
        translatePageCompatible(translateSessionToken);
    }

    private void translatePageCompatible(int token) {
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

    private void continueCompatibleTranslation() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            QuietToast.makeText(this, "Buka website dulu", QuietToast.LENGTH_SHORT).show();
            return;
        }
        startCompatibleTranslateSession(getOriginalForTranslate(getEffectiveCurrentUrl()));
        updateTopActionStates();
        translatePageCompatible();
    }

    private void clearCompatibleTranslationMarks() {
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

    private void applyCompatibleTranslation(int index, String translated) {
        if (webView == null) return;
        try {
            if (!isCompatibleTranslateAllowed(translateSessionToken)) return;
            String js = "javascript:(function(){try{if(window.__yieldApplyTranslation)window.__yieldApplyTranslation("
                    + index + "," + org.json.JSONObject.quote(translated) + ");}catch(e){}})()";
            runJsOnCurrentPage(js);
        } catch (Exception ignored) {
        }
    }

    private String translateTextViaGoogle(String text) {
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

    private void loadTranslatedPage(String originalUrl) {
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

    private void hideGoogleTranslateToolbar() {
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

    private void unblockTranslatedPageClicks() {
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

    private void showGoogleTranslateBar() {
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

    private void translatePageTextOnly() {
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

    private void reloadCurrentWebsite() {
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

    private boolean captureAdRedirectToTempTab(String url) {
        return captureBlockedNavigationToTempTab(url, false);
    }

    private boolean captureDirectImageToTempTab(String url) {
        return captureBlockedNavigationToTempTab(url, true);
    }

    private boolean captureBlockedNavigationToTempTab(String url, boolean allowWhenAdBlockOff) {
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

    private void closeAdTabSilently(TabInfo adTab, int fallbackIndex) {
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

    private void scheduleCloseDetectedAdTabs() {
        if (!adBlockAutoCloseAdTabs) return;
        mainHandler.postDelayed(this::closeDetectedAdTabs, 900);
    }

    private void closeDetectedAdTabs() {
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
    private WebView createBrowserWebView(TabInfo owner, int visibility) {
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

    private void recreateBrowserWebViewForMode(String targetUrl, boolean showWebPage) {
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

    private void hardReloadUrlWithCurrentBrowserMode(String targetUrl, boolean showWebPage) {
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

    private void removeShieldDocumentStartScript(WebView target) {
        if (target == null) return;
        try {
            ScriptHandler handler = shieldDocumentStartHandlers.remove(target);
            if (handler != null && WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                handler.remove();
            }
        } catch (Exception ignored) {
        }
    }

    private void installShieldDocumentStartScript(WebView target) {
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

    private void injectShieldEngineV2Fallback() {
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

    private void syncShieldRuntimeState() {
        if (webView == null) return;
        try {
            runPageScript(ShieldPageScript.runtimeConfig(adBlock, adBlockPopupBlocker,
                    adBlockRedirectBlocker, adBlockScriptIframeBlocker,
                    adBlockClickHijackBlocker));
        } catch (Exception ignored) {
        }
    }

    private void onShieldSettingsChanged() {
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

    private boolean isShieldReaderOrCompatibilityContext(String url) {
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

    private boolean shouldShieldBlockMainFrame(String targetUrl, String sourceUrl,
                                               boolean hasGesture, boolean legacySuspicious) {
        if (!adBlock || !(adBlockRedirectBlocker || adBlockClickHijackBlocker)) return false;
        boolean context = isShieldReaderOrCompatibilityContext(sourceUrl);
        boolean explicitlyTrusted = isTrustedDownloadIntentUrl(targetUrl)
                || isSearchEngineResultNavigation(targetUrl, sourceUrl);
        return ShieldEngineV2.shouldBlockMainFrameNavigation(targetUrl, sourceUrl,
                hasGesture, context, explicitlyTrusted, legacySuspicious);
    }

    private boolean handleLongPressedLink(WebView sourceView) {
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

    private void showLongPressedLinkMenu(WebView sourceView, String requestedHref,
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

    private void openLongPressedLinkInNewTab(String url) {
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
    private void configureWebView() {
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
                    QuietToast.makeText(MainActivity.this, "Diblokir Safe Browsing sederhana", QuietToast.LENGTH_SHORT).show();
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
                        MainActivity.this::handleHttpsFirstMainFrameFailure,
                        MainActivity.this::isSiteCompatibilityModeActiveForUrl,
                        MainActivity.this::isExternalSchemeUrl,
                        MainActivity.this::isTrustedMainFrameNavigation,
                        MainActivity.this::isKnownPopupHost,
                        MainActivity.this::isLikelyAdClickUrl,
                        MainActivity.this::isAdUrl,
                        MainActivity.this::isSuspiciousPopupNavigation,
                        MainActivity.this::restoreAfterBlockedNavigation,
                        () -> smoothSearchTransitionActive,
                        MainActivity.this::finishSmoothSearchTransition)) {
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
                                MainActivity.this::findTabByWebView,
                                MainActivity.this::getCurrentTab,
                                MainActivity.this::getTabReferenceUrl,
                                MainActivity.this::extractOriginalUrl,
                                ReaderCompatibilityPolicy::isTransientBlankUrl,
                                MainActivity.this::isShieldReaderOrCompatibilityContext,
                                MainActivity.this::isKnownPopupHost,
                                MainActivity.this::isLikelyAdClickUrl,
                                MainActivity.this::isAdUrl,
                                MainActivity.this::isSuspiciousPopupNavigation,
                                MainActivity.this::shouldShieldBlockMainFrame,
                                MainActivity.this::restoreAfterBlockedNavigation,
                                MainActivity.this::isStrictSiteCompatibilityUrl,
                                MainActivity.this::registerNavigationLoopGuard,
                                MainActivity.this::isSiteCompatibilityModeActiveForUrl);
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
                        MainActivity.this::applyPlainCompatibilitySettings,
                        () -> scheduleCompatibilityLoadFallback(url),
                        MainActivity.this::applyBrowserSettings,
                        () -> {
                            try {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                            } catch (Exception ignored) {
                            }
                        },
                        delay -> mainHandler.postDelayed(
                                MainActivity.this::applyDesktopViewportIfNeeded, delay),
                        delay -> mainHandler.postDelayed(
                                MainActivity.this::applyMobileViewportIfNeeded, delay));
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
                            if (smoothSearchTransitionActive && navigationLoadingOverlay != null) {
                                navigationLoadingOverlay.bringToFront();
                            }
                        },
                        MainActivity.this::shouldRecordHistoryUrl,
                        () -> historyClearLock = false,
                        MainActivity.this::addBrowserHistory,
                        MainActivity.this::getEffectiveCurrentUrl,
                        MainActivity.this::getTabReferenceUrl,
                        MainActivity.this::isCompatibilityNavigationFlow,
                        MainActivity.this::isTrustedMainFrameNavigation,
                        MainActivity.this::isDirectImageMainFrameNavigation,
                        MainActivity.this::isExternalSchemeUrl,
                        MainActivity.this::isSearchEngineResultNavigation,
                        MainActivity.this::isSuspiciousPopupNavigation,
                        MainActivity.this::isLikelyAdClickUrl,
                        MainActivity.this::restoreAfterBlockedNavigation);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                BrowserPageCommitCoordinator.Result pageCommit =
                        BrowserPageCommitCoordinator.handle(
                                view == webView, view, url,
                                MainActivity.this::extractOriginalUrl,
                                MainActivity.this::findTabByWebView,
                                MainActivity.this::getCurrentTab,
                                MainActivity.this::commitTabUrlIfSafe,
                                MainActivity.this::handleHttpsFirstNavigationSuccess,
                                finalUrl -> currentPageUrlForRequest = finalUrl,
                                MainActivity.this::syncNightModeWebSettingsForUrl,
                                MainActivity.this::scheduleNightModeSyncForPage);
                if (pageCommit.inactiveView) return;
                String finalUrl = pageCommit.finalUrl;
                BrowserPageCommitCoordinator.applyEffects(
                        finalUrl, adBlock,
                        MainActivity.this::isStrictSiteCompatibilityUrl,
                        MainActivity.this::isSiteCompatibilityModeActiveForUrl,
                        MainActivity.this::syncShieldRuntimeState,
                        MainActivity.this::injectShieldEngineV2Fallback,
                        MainActivity.this::injectAdBlockCssEarly,
                        MainActivity.this::injectCompatibilityAdShield,
                        MainActivity.this::scheduleUniversalReaderCompatibilityRepair,
                        MainActivity.this::hasUserFiltersForCurrentHost,
                        MainActivity.this::applyUserFiltersForCurrentPage);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (BrowserPageFinishCoordinator.handleInactive(
                        view != webView, view, url,
                        MainActivity.this::findTabByWebView,
                        MainActivity.this::extractOriginalUrl,
                        MainActivity.this::commitTabUrlIfSafe,
                        BrowserPageFinishCoordinator::getTitle,
                        BrowserPageFinishCoordinator::saveWebState)) {
                    super.onPageFinished(view, url);
                    return;
                }
                BrowserPageFinishCoordinator.Result pageFinish =
                        BrowserPageFinishCoordinator.handleActive(
                                view, url, MainActivity.this::extractOriginalUrl,
                                MainActivity.this::findTabByWebView, MainActivity.this::getCurrentTab,
                                MainActivity.this::handleHttpsFirstNavigationSuccess,
                                finalUrl -> currentPageUrlForRequest = finalUrl,
                                MainActivity.this::scheduleHorizontalGestureGuardCheck,
                                MainActivity.this::shouldRecordHistoryUrl,
                                MainActivity.this::canCommitUrlToTab,
                                finalUrl -> lastSafeHttpUrl = finalUrl,
                                () -> webView != null && webView.getVisibility() == View.VISIBLE,
                                finalUrl -> addressBar.setText(finalUrl),
                                () -> progressBar.setVisibility(View.GONE));
                String finalUrl = pageFinish.finalUrl;
                TabInfo currentTab = pageFinish.owner;
                BrowserPageFinishPolicy.Profile pageFinishProfile =
                        BrowserPageFinishCoordinator.prepareProfile(
                                finalUrl, MainActivity.this::isStrictSiteCompatibilityUrl,
                                MainActivity.this::isReloadLoopGuardActiveForUrl,
                                MainActivity.this::isSiteCompatibilityModeActiveForUrl,
                                MainActivity.this::applyPlainCompatibilitySettings,
                                MainActivity.this::cancelSmoothSearchTransition);
                boolean pageReloadGuarded = BrowserPageFinishPolicy.isReloadGuarded(pageFinishProfile);
                if (pageReloadGuarded) {
                    BrowserPageFinishCoordinator.applyGuardedEffects(
                            pageFinishProfile, finalUrl, adBlock, desktopMode,
                            MainActivity.this::applyPlainCompatibilitySettings,
                            MainActivity.this::scheduleNightModeSyncForPage,
                            MainActivity.this::injectCompatibilityAdShield,
                            delay -> mainHandler.postDelayed(
                                    MainActivity.this::injectCompatibilityAdShield, delay),
                            MainActivity.this::scheduleUniversalReaderCompatibilityRepair,
                            delay -> mainHandler.postDelayed(
                                    MainActivity.this::applyDesktopViewportIfNeeded, delay),
                            delay -> mainHandler.postDelayed(
                                    MainActivity.this::applyMobileViewportIfNeeded, delay));
                }
                BrowserPageFinishCoordinator.applyNormalEffects(
                        pageFinishProfile, finalUrl, desktopMode, readerMode, adBlock,
                        MainActivity.this::applyViewportForCurrentMode,
                        delay -> mainHandler.postDelayed(
                                MainActivity.this::applyViewportForCurrentMode, delay),
                        delay -> mainHandler.postDelayed(
                                MainActivity.this::applyDesktopViewportIfNeeded, delay),
                        MainActivity.this::injectReaderMode,
                        () -> {
                            injectShieldEngineV2Fallback();
                            injectPremiumAdBlock();
                            injectYouTubeSafeAdBlockV6();
                        },
                        delay -> mainHandler.postDelayed(() -> {
                            injectPremiumAdBlock();
                            injectYouTubeSafeAdBlockV6();
                        }, delay),
                        MainActivity.this::scheduleUniversalBlankCompatibilityRecovery,
                        MainActivity.this::scheduleUniversalReaderCompatibilityRepair,
                        MainActivity.this::updateVideoControlsVisibility);
                BrowserPageFinishCoordinator.applyUserFilterEffects(
                        hasUserFiltersForCurrentHost(),
                        MainActivity.this::applyUserFiltersForCurrentPage,
                        delay -> mainHandler.postDelayed(
                                MainActivity.this::applyUserFiltersForCurrentPage, delay));
                currentTab = BrowserPageFinishCoordinator.finalizeHistory(
                        view, currentTab, MainActivity.this::getCurrentTab, finalUrl,
                        MainActivity.this::shouldRecordHistoryUrl,
                        MainActivity.this::addBrowserHistory,
                        BrowserPageFinishCoordinator::getTitle,
                        MainActivity.this::commitTabUrlIfSafe,
                        MainActivity.this::saveTabsSession);
                BrowserPageFinishCoordinator.applyNormalCompletionEffects(
                        pageFinishProfile, finalUrl, url,
                        () -> videoControlsManualHidden = false,
                        MainActivity.this::injectVideoPlaybackWatcher,
                        MainActivity.this::scheduleNightModeSyncForPage,
                        MainActivity.this::detectTranslateProxyBlocked);
                BrowserPageFinishCoordinator.applyTranslateToolbarEffects(
                        pageFinishProfile, hideGoogleTranslateBar, url,
                        MainActivity.this::isGoogleTranslatedUrl,
                        delay -> mainHandler.postDelayed(
                                MainActivity.this::hideGoogleTranslateToolbar, delay));
                BrowserPageFinishCoordinator.applyCompatibleTranslateEffects(
                        pageFinishProfile, translateEnabled, compatibleTranslateActive,
                        translateManuallyDisabled, url,
                        MainActivity.this::isGoogleTranslatedUrl,
                        () -> translateSessionToken,
                        (token, delay) -> mainHandler.postDelayed(
                                () -> translatePageCompatible(token), delay));
                BrowserPageFinishCoordinator.applyKeyboardEffects(
                        pendingHideKeyboardAfterNavigation,
                        MainActivity.this::blurWebInputsAndHideKeyboard,
                        () -> pendingHideKeyboardAfterNavigation = false,
                        (action, delay) -> mainHandler.postDelayed(action, delay));
                BrowserPageFinishCoordinator.applyFinalEffects(
                        smoothSearchTransitionActive,
                        delay -> mainHandler.postDelayed(
                                MainActivity.this::finishSmoothSearchTransition, delay),
                        MainActivity.this::scheduleCloseDetectedAdTabs,
                        MainActivity.this::updateTopActionStates);
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
                        MainActivity.this::setRequestedOrientation,
                        MainActivity.this::moveVideoControlsToFullscreenOverlay,
                        MainActivity.this::updateVideoModeToggleButton,
                        MainActivity.this::checkAndShowVideoControls),
                () -> BrowserChromeFullscreenHandler.hide(
                        fullscreenVideoView,
                        fullscreenVideoCallback,
                        getWindow(),
                        () -> fullscreenVideoView = null,
                        () -> fullscreenVideoCallback = null,
                        MainActivity.this::restoreAfterVideoFullscreen,
                        MainActivity.this::updateVideoModeToggleButton)));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private boolean isSystemDarkMode() {
        try {
            int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return mode == Configuration.UI_MODE_NIGHT_YES;
        } catch (Exception e) {
            return false;
        }
    }

    private String getCurrentHostForSettings() {
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

    private boolean isNightModeActiveForCurrentSite() {
        return isNightModeActiveForUrl(getEffectiveCurrentUrl());
    }

    private boolean isNightModeActiveForUrl(String url) {
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

    private String getHostForNightMode(String url) {
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

    private boolean isAlgorithmicDarkeningSupported() {
        try {
            return WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void applyAlgorithmicDarkening(WebSettings settings, boolean active) {
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

    private void syncNightModeWebSettingsForUrl(String url) {
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

    private void scheduleNightModeSyncForPage(String url) {
        if (webView == null) return;
        final int token = ++nightModeApplyToken;
        final String targetUrl = url != null ? url : getEffectiveCurrentUrl();
        syncNightModeWebSettingsForUrl(targetUrl);
        applyNightModeToWebPage();
        mainHandler.postDelayed(() -> { if (token == nightModeApplyToken) applyNightModeToWebPage(); }, 220);
        mainHandler.postDelayed(() -> { if (token == nightModeApplyToken) applyNightModeToWebPage(); }, 900);
        mainHandler.postDelayed(() -> { if (token == nightModeApplyToken) applyNightModeToWebPage(); }, 2200);
    }

    private void setNightModeOptionAndApply(String option, boolean reloadPage) {
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

    private String nightModeLabel() {
        if ("OFF".equals(nightModeOption)) return "OFF";
        if ("AUTO".equals(nightModeOption)) return "Auto ikut sistem";
        return "ON";
    }

    private void applyNightModeToWebPage() {
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

    private void disableNightModeCompletely(boolean reloadPage) {
        setNightModeOptionAndApply("OFF", reloadPage);
    }

    private void showNightModeSettingsDialog() {
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


    private View nightChoiceRow(String label, boolean checked, View.OnClickListener listener) {
        return SettingsUi.nightChoiceRow(this, label, checked, listener);
    }

    private TextView dialogTextButton(String text) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(COLOR_ACCENT);
        btn.setTextSize(13);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(12), dp(10), dp(12), dp(10));
        return btn;
    }

    private void showNightModeExceptionDialog() {
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

    private void loadBrowserUrl(String url) {
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
                MainActivity.this::applyPlainCompatibilitySettings,
                MainActivity.this::loadCompatibilityUrlWithCurrentMode,
                delay -> mainHandler.postDelayed(
                        MainActivity.this::applyDesktopViewportIfNeeded, delay),
                MainActivity.this::scheduleCompatibilityLoadFallback,
                MainActivity.this::applyBrowserSettings,
                () -> BrowserLoadRequestPolicy.requestHeaders(
                        desktopMode, getMobileUserAgent(), getDesktopUserAgent()),
                (target, headers) -> webView.loadUrl(target, headers),
                target -> webView.loadUrl(target));
    }

    private void scheduleMobileViewportReset() {
        MobileViewportResetCoordinator.schedule(
                desktopMode,
                browserModeToken,
                (action, delay) -> mainHandler.postDelayed(action, delay),
                () -> browserModeToken,
                () -> desktopMode,
                MainActivity.this::applyMobileViewportIfNeeded);
    }

    void forceMobileModeAfterUpdateIfNeeded(SharedPreferences p) {
        MobileModeMigration.apply(p, () -> desktopMode = false);
    }

    private void toggleDesktopModeSafely() {
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
    private String getSafeReloadUrlForModeChange() {
        String currentWebUrl = webView != null ? webView.getUrl() : "";
        String currentAddressUrl = addressBar != null ? addressBar.getText().toString() : "";
        return BrowserModeReloadSelector.select(
                new String[]{currentWebUrl, currentAddressUrl, lastSafeHttpUrl},
                MainActivity.this::extractOriginalUrl,
                MainActivity.this::isSafeUrlForModeReload);
    }


    private boolean isSafeUrlForModeReload(String url, boolean explicitCurrentPage) {
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

    private String normalizeUrlForCurrentBrowserMode(String url) {
        return BrowserModeUrlNormalizer.normalize(
                url,
                desktopMode,
                MainActivity.this::extractOriginalUrl,
                MainActivity.this::isHttpOrHttpsUrl,
                MainActivity.this::isYouTubePageUrl);
    }

    private void applyBrowserSettings() {
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

    private void applyMobileProfile(WebSettings settings) {
        BrowserWebViewSettings.applyMobileProfile(
                webView, settings, getMobileUserAgent());
    }

    private void applyDesktopProfile(WebSettings settings) {
        BrowserWebViewSettings.applyDesktopProfile(
                webView, settings, getDesktopUserAgent());
    }

    private String getMobileUserAgent() {
        return BrowserUtils.getMobileUserAgent();
    }

    private String getDesktopUserAgent() {
        return BrowserUtils.getDesktopUserAgent();
    }

    private String hostOfUrl(String url) {
        return BrowserUrlIdentityPolicy.normalizedHost(
                url,
                MainActivity.this::extractOriginalUrl,
                value -> Uri.parse(value).getHost());
    }

    private String navigationLoopKey(String url) {
        return BrowserUrlIdentityPolicy.navigationLoopKey(
                url, MainActivity.this::extractOriginalUrl);
    }

    private void enableSiteCompatibilityModeForUrl(String url) {
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

    private boolean isSiteCompatibilityModeActiveForUrl(String url) {
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

    private boolean isStrictSiteCompatibilityUrl(String url) {
        try {
            return StrictCompatibilityUrlPolicy.isStrict(
                    hostOfUrl(url),
                    MainActivity.this::isKnownStrictCompatibilityHost,
                    () -> isSiteCompatibilityModeActiveForUrl(url));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isKnownStrictCompatibilityHost(String host) {
        return StrictCompatibilityHostPolicy.isKnownHost(host, STRICT_COMPAT_HOSTS);
    }

    private boolean isCompatibilityNavigationFlow(String targetUrl, String sourceUrl) {
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
                    MainActivity.this::sameOrSubDomain);
        } catch (Exception e) {
            return false;
        }
    }

    private void repairUniversalReaderPage(String url) {
        if (webView == null || !ReaderCompatibilityPolicy.isEligiblePageUrl(url)) return;
        runPageScript(UniversalReaderRepairScript.build());
    }

    private void scheduleUniversalReaderCompatibilityRepair(String url) {
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
                            MainActivity.this::extractOriginalUrl,
                            ReaderCompatibilityPolicy::isEligiblePageUrl,
                            MainActivity.this::hostOfUrl,
                            MainActivity.this::sameOrSubDomain);
                    if (active == null) return;
                    repairUniversalReaderPage(active);
                } catch (Exception ignored) {
                }
            });
        }
    }

    private void applyPlainCompatibilitySettings() {
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


    private void loadCompatibilityUrlWithCurrentMode(String cleanUrl) {
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

    private void scheduleCompatibilityLoadFallback(String url) {
        final TabInfo expectedTab = findTabByWebView(webView);
        final WebView expectedView = webView;
        final String expectedHost = hostOfUrl(url);
        postForActiveTab(expectedTab, expectedView, 3500L, () -> {
            try {
                String active = CompatibilityLoadFallbackPolicy.resolve(
                        expectedView.getUrl(),
                        expectedTab.currentPageUrlForRequest,
                        expectedHost,
                        MainActivity.this::extractOriginalUrl,
                        MainActivity.this::hostOfUrl,
                        MainActivity.this::sameOrSubDomain);
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

    private String decodeEvaluateJavascriptString(String value) {
        try {
            if (value == null) return "";
            Object parsed = new org.json.JSONTokener(value).nextValue();
            return parsed == null ? "" : String.valueOf(parsed);
        } catch (Exception e) {
            return value == null ? "" : value;
        }
    }

    private boolean isUniversalCompatibilityCandidateUrl(String url) {
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

    private boolean shouldRetryCompatibilityRecovery(String url) {
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

    private boolean isLikelyBlankCompatibilityReport(String report) {
        return BlankCompatibilityReportPolicy.isLikelyBlank(report);
    }

    private void scheduleUniversalBlankCompatibilityRecovery(String url) {
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
                            MainActivity.this::extractOriginalUrl,
                            MainActivity.this::hostOfUrl,
                            MainActivity.this::navigationLoopKey,
                            MainActivity.this::sameOrSubDomain);
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
                                    MainActivity.this::extractOriginalUrl,
                                    MainActivity.this::hostOfUrl,
                                    MainActivity.this::navigationLoopKey,
                                    MainActivity.this::sameOrSubDomain);
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

    private boolean isReloadLoopGuardActiveForUrl(String url) {
        return ReloadLoopGuardActivePolicy.isActive(
                reloadLoopGuardHost,
                reloadLoopGuardUntilMs,
                System.currentTimeMillis(),
                () -> hostOfUrl(url));
    }
    private boolean registerNavigationLoopGuard(String url) {
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

    private void runPageScript(String js) {
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

    private void applyViewportForCurrentMode() {
        if (webView == null) return;
        applyBrowserSettings();
        if (desktopMode) applyDesktopViewportIfNeeded();
        else applyMobileViewportIfNeeded();
    }

    private void applyMobileViewportIfNeeded() {
        if (desktopMode || webView == null) return;
        try { applyMobileProfile(webView.getSettings()); } catch (Exception ignored) {}
        injectMobileViewportReset();
    }

    private void injectMobileViewportReset() {
        if (desktopMode || webView == null) return;
        String js = "javascript:(function(){try{"
                + "var h=document.head||document.getElementsByTagName('head')[0]||document.documentElement;"
                + "var m=document.querySelector('meta[name=viewport]');"
                + "if(!m){m=document.createElement('meta');m.name='viewport';h.appendChild(m);}"
                + "m.setAttribute('content','width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes');"
                + "document.documentElement.style.removeProperty('min-width');"
                + "document.documentElement.style.removeProperty('width');"
                + "if(document.body){document.body.style.removeProperty('min-width');document.body.style.removeProperty('width');}"
                + "try{window.dispatchEvent(new Event('resize'));}catch(e){}"
                + "}catch(e){}})()";
        try {
            if (Build.VERSION.SDK_INT >= 19) webView.evaluateJavascript(js.replace("javascript:", ""), null);
            else webView.loadUrl(js);
        } catch (Exception ignored) {}
    }

    private void applyDesktopViewportIfNeeded() {
        if (!desktopMode || webView == null) return;
        try { applyDesktopProfile(webView.getSettings()); } catch (Exception ignored) {}
        injectDesktopViewportLock();
    }

    private void injectDesktopViewportLock() {
        if (!desktopMode || webView == null) return;
        String js = "javascript:(function(){try{"
                + "var w=1200;"
                + "var h=document.head||document.getElementsByTagName('head')[0]||document.documentElement;"
                + "var m=document.querySelector('meta[name=viewport]');"
                + "if(!m){m=document.createElement('meta');m.name='viewport';h.appendChild(m);}"
                + "m.setAttribute('content','width='+w+', initial-scale=1.0, maximum-scale=5.0, user-scalable=yes');"
                + "document.documentElement.style.minWidth=w+'px';"
                + "if(document.body){document.body.style.minWidth=w+'px';}"
                + "}catch(e){}})()";
        try {
            if (Build.VERSION.SDK_INT >= 19) webView.evaluateJavascript(js.replace("javascript:", ""), null);
            else webView.loadUrl(js);
        } catch (Exception ignored) {}
    }

    private void markTrustedMainFrameNavigation(String url) {
        try {
            if (!isHttpOrHttpsUrl(url)) return;
            String host = normalizeHostForAdBlock(url);
            if (host.length() == 0) return;
            trustedMainFrameHost = host;
            trustedMainFrameUntilMs = System.currentTimeMillis() + 10000L;
        } catch (Exception ignored) {
        }
    }

    private boolean isTrustedMainFrameNavigation(String url) {
        try {
            if (trustedMainFrameHost == null || trustedMainFrameHost.length() == 0) return false;
            if (System.currentTimeMillis() > trustedMainFrameUntilMs) return false;
            String host = normalizeHostForAdBlock(url);
            return host.length() > 0 && sameOrSubDomain(host, trustedMainFrameHost);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSearchEngineResultNavigation(String targetUrl, String currentUrl) {
        if (!isHttpOrHttpsUrl(targetUrl)) return false;
        // v0.10.07: universal search-result allow lane. Do not depend on a fixed list of
        // .com domains: regional Google/Yahoo/Yandex domains and self-hosted SearX/SearXNG
        // pages are recognized from their search URL shape by ShieldEngineV2.
        return ShieldEngineV2.isSearchResultsPage(currentUrl);
    }

    private boolean isCompatibilityAdNavigation(String targetUrl, String currentUrl, boolean hasGesture) {
        try {
            if (!adBlock || !isHttpOrHttpsUrl(targetUrl)) return isExternalSchemeUrl(targetUrl);
            if (isTrustedDownloadIntentUrl(targetUrl) || isSearchEngineResultNavigation(targetUrl, currentUrl)) return false;

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

    private boolean isCompatibilityContentAssetUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);
        return isImageResourceUrl(u)
                || isMediaResourceUrl(u)
                || u.matches(".*\\.(?:woff2?|ttf|otf)(?:$|[?#]).*");
    }

    private boolean isCompatibilityThirdPartyAdResource(String resourceUrl, String pageUrl) {
        try {
            if (!adBlock || !isHttpOrHttpsUrl(resourceUrl) || !isHttpOrHttpsUrl(pageUrl)) return false;
            if (isTrustedDownloadIntentUrl(resourceUrl) || isYoutubeCoreUrl(resourceUrl)) return false;
            boolean hardAd = isKnownPopupHost(resourceUrl) || isAdUrl(resourceUrl);
            return ReaderCompatibilityPolicy.shouldBlockThirdPartyResource(
                    resourceUrl, pageUrl, hardAd, isCompatibilityContentAssetUrl(resourceUrl));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isContextAllowedSuspiciousMainFrameNavigation(String targetUrl, String currentUrl, boolean hasGesture) {
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

            if (!suspicious) return false;
            if (sameSite) return true;
            if (fromSearch) return true;

            // v0.9.97: on compatibility/reader pages, a touch event can be stolen by an
            // advertising click-hijacker. Do not treat that inherited user gesture as consent
            // to leave the reader for a suspicious cross-site destination.
            boolean compatibilitySource = isStrictSiteCompatibilityUrl(currentUrl)
                    || isSiteCompatibilityModeActiveForUrl(currentUrl)
                    || isReloadLoopGuardActiveForUrl(currentUrl)
                    || ShieldEngineV2.isPopupIsolationContentPage(currentUrl);
            if (compatibilitySource) return false;

            return hasGesture && currentHost.length() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isNormalUserMainFrameNavigation(String targetUrl, String currentUrl, boolean hasGesture) {
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
        return hasGesture && !isKnownPopupHost(targetUrl) && !isAdUrl(targetUrl) && !isLikelyAdClickUrl(targetUrl);
    }

    private boolean isFirstPartyResourceForCurrentPage(String resourceUrl, String pageUrl) {
        try {
            if (!isHttpOrHttpsUrl(resourceUrl) || !isHttpOrHttpsUrl(pageUrl)) return false;
            String resourceHost = normalizeHostForAdBlock(resourceUrl);
            String pageHost = normalizeHostForAdBlock(pageUrl);
            return resourceHost.length() > 0 && pageHost.length() > 0 && sameOrSubDomain(resourceHost, pageHost);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTrustedDownloadIntentUrl(String url) {
        try {
            if (url == null) return false;
            String raw = url.trim();
            if (!isHttpOrHttpsUrl(raw)) return false;

            String u = raw.toLowerCase(Locale.US);
            String decoded = u;
            try { decoded = URLDecoder.decode(u, "UTF-8").toLowerCase(Locale.US); } catch (Exception ignored) {}
            String host = normalizeHostForAdBlock(raw);
            if (host.length() == 0) return false;

            boolean trustedHost = isTrustedDownloadHostForAllow(host);
            boolean marker = hasTrustedDownloadMarker(u) || hasTrustedDownloadMarker(decoded);
            boolean file = hasDirectFileDownloadExtension(u) || hasDirectFileDownloadExtension(decoded);
            boolean hardAdToken = hasHardAdClickToken(u) || hasHardAdClickToken(decoded);
            boolean suspiciousHost = isSuspiciousAdHostForDownloadAllow(host);

            // Universal download-safe lane: tombol download asli tidak boleh ikut keblokir AdBlock.
            if (trustedHost && (marker || file)) return true;
            if ((marker || file) && !suspiciousHost && !hardAdToken) return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTrustedDownloadHostForAllow(String host) {
        return DownloadUrlPolicy.isTrustedDownloadHostForAllow(host);
    }

    private boolean hasTrustedDownloadMarker(String u) {
        return DownloadUrlPolicy.hasTrustedDownloadMarker(u);
    }

    private boolean hasDirectFileDownloadExtension(String u) {
        return DownloadUrlPolicy.hasDirectFileDownloadExtension(u);
    }

    private boolean hasHardAdClickToken(String u) {
        if (u == null) return false;
        return u.contains("adclick")
                || u.contains("ad_click")
                || u.contains("adurl=")
                || u.contains("click_id")
                || u.contains("af_click")
                || u.contains("clickunder")
                || u.contains("popunder")
                || u.contains("popupads")
                || u.contains("onclickads")
                || u.contains("interstitial")
                || u.contains("utm_medium=affiliates")
                || u.contains("deep_and_deferred")
                || u.contains("navigate_url=")
                || u.contains("reactpath");
    }

    private boolean isSuspiciousAdHostForDownloadAllow(String host) {
        return DownloadUrlPolicy.isSuspiciousAdHostForDownloadAllow(host);
    }

    private boolean isUnsafeUrl(String url) {
        String u = url.toLowerCase();
        return u.contains("phishing") || u.contains("malware") || u.contains("virus") || u.contains("scam");
    }

    private String normalizeHostForAdBlock(String url) {
        try {
            if (url == null || url.length() == 0) return "";
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return "";
            host = host.toLowerCase(Locale.US);
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception e) {
            return "";
        }
    }

    private boolean sameOrSubDomain(String host, String baseHost) {
        if (host == null || baseHost == null || host.length() == 0 || baseHost.length() == 0) return false;
        return host.equals(baseHost) || host.endsWith("." + baseHost);
    }

    // v0.9.92: dipindah ke konstanta static (lihat catatan AD_URL_HOST_PATTERNS).
    private static final String[] POPUP_HOST_PATTERNS = new String[]{
            "hotterydiseur", "sewarsremeets", "sewarsremeet", "onclickads", "clickadu", "popads", "popcash",
            "popunder", "adsterra", "propellerads", "hilltopads", "exoclick", "trafficjunky", "juicyads",
            "admaven", "pushpush", "pushengage", "pushwoosh", "realsrv", "invest-tracing", "highperformanceformat",
            "highperformancedisplayformat", "xmladfeed", "rotator", "smartlink", "adnxs", "rubiconproject",
            "taboola", "outbrain", "mgid", "revcontent", "doubleclick", "googlesyndication", "googleadservices"
    };

    private boolean isKnownPopupHost(String url) {
        String host = normalizeHostForAdBlock(url);
        if (host.length() == 0) return false;
        if (isTrustedDownloadIntentUrl(url)) return false;

        for (String s : POPUP_HOST_PATTERNS) {
            if (host.contains(s)) return true;
        }

        if (host.endsWith(".cfd") || host.endsWith(".click") || host.endsWith(".cam") || host.endsWith(".monster")
                || host.endsWith(".quest") || host.endsWith(".buzz") || host.endsWith(".icu") || host.endsWith(".cyou")) {
            return true;
        }

        String u = url.toLowerCase(Locale.US);
        return u.contains("/popunder") || u.contains("/popup") || u.contains("/redirect")
                || u.contains("/push/") || u.contains("?utm_source=ad") || u.contains("&ad_id=")
                || u.contains("?ad_id=") || u.contains("/prebid") || u.contains("/vast") || u.contains("/vpaid");
    }

    private boolean isHttpOrHttpsUrl(String url) {
        if (url == null) return false;
        String u = url.trim().toLowerCase(Locale.US);
        return u.startsWith("http://") || u.startsWith("https://");
    }

    // ===== HTTPS-First Navigation (v0.9.98) =====
    private void clearAllHttpsFirstRuntimeState() {
        try {
            for (TabInfo tab : tabs) clearHttpsFirstPendingState(tab, true);
        } catch (Exception ignored) {
        }
    }

    private void clearHttpsFirstPendingState(TabInfo tab, boolean clearFallbackGuard) {
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

    private boolean isHttpUrl(String url) {
        return url != null && url.trim().toLowerCase(Locale.US).startsWith("http://");
    }

    private boolean isHttpsUrl(String url) {
        return url != null && url.trim().toLowerCase(Locale.US).startsWith("https://");
    }

    private boolean isPrivateIpv4Host(String host) {
        try {
            String[] parts = host.split("\\.");
            if (parts.length != 4) return false;
            int[] octets = new int[4];
            for (int i = 0; i < 4; i++) {
                if (parts[i].length() == 0 || parts[i].length() > 3) return false;
                octets[i] = Integer.parseInt(parts[i]);
                if (octets[i] < 0 || octets[i] > 255) return false;
            }
            return octets[0] == 10
                    || octets[0] == 127
                    || (octets[0] == 169 && octets[1] == 254)
                    || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31)
                    || (octets[0] == 192 && octets[1] == 168)
                    || octets[0] == 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isLocalOrPrivateHost(String host) {
        if (host == null || host.trim().length() == 0) return true;
        String h = host.trim().toLowerCase(Locale.US);
        if (h.startsWith("[") && h.endsWith("]")) h = h.substring(1, h.length() - 1);
        if (h.equals("localhost") || h.equals("::1") || h.equals("0:0:0:0:0:0:0:1")) return true;
        if (h.endsWith(".localhost") || h.endsWith(".local") || h.endsWith(".lan")
                || h.endsWith(".home") || h.endsWith(".internal") || h.endsWith(".test")
                || h.endsWith(".invalid") || h.endsWith(".onion")) return true;
        if (!h.contains(".") && !h.contains(":")) return true;
        if (isPrivateIpv4Host(h)) return true;
        if (h.contains(":")) {
            return h.startsWith("fc") || h.startsWith("fd") || h.startsWith("fe8")
                    || h.startsWith("fe9") || h.startsWith("fea") || h.startsWith("feb");
        }
        return false;
    }

    private boolean isHttpsFirstExemptUrl(String url) {
        try {
            if (!isHttpUrl(url)) return true;
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            if (host == null || isLocalOrPrivateHost(host)) return true;
            int port = uri.getPort();
            return port != -1 && port != 80 && port != 443;
        } catch (Exception ignored) {
            return true;
        }
    }

    private String buildHttpsUpgradeUrl(String httpUrl) {
        try {
            if (!isHttpUrl(httpUrl) || isHttpsFirstExemptUrl(httpUrl)) return httpUrl;
            URI source = new URI(httpUrl.trim());
            int sourcePort = source.getPort();
            int securePort = sourcePort == 80 ? -1 : sourcePort;
            URI secure = new URI(
                    "https",
                    source.getUserInfo(),
                    source.getHost(),
                    securePort,
                    source.getPath(),
                    source.getQuery(),
                    source.getFragment());
            return secure.toASCIIString();
        } catch (Exception ignored) {
            return httpUrl;
        }
    }

    private boolean isHttpsFallbackGuardActive(TabInfo tab, String httpUrl) {
        if (tab == null || !isHttpUrl(httpUrl)) return false;
        if (System.currentTimeMillis() > tab.httpsFallbackAllowedUntilMs) return false;
        String host = hostOfUrl(httpUrl);
        return host.length() > 0 && host.equals(tab.httpsFallbackHost);
    }

    private String prepareHttpsFirstNavigation(String rawUrl, TabInfo tab) {
        if (rawUrl == null) return null;
        String clean = rawUrl.trim();
        if (!httpsFirstEnabled || !isHttpUrl(clean) || isHttpsFirstExemptUrl(clean)) return clean;

        if (tab != null && tab.httpsFallbackInProgress) {
            String expected = tab.pendingHttpsOriginalUrl;
            tab.httpsFallbackInProgress = false;
            if (expected != null && expected.equals(clean)) {
                tab.pendingHttpsOriginalUrl = "";
                tab.pendingHttpsUpgradeUrl = "";
                tab.pendingHttpsStartedAtMs = 0L;
                return clean;
            }
        }
        if (isHttpsFallbackGuardActive(tab, clean)) return clean;

        String secure = buildHttpsUpgradeUrl(clean);
        if (secure == null || secure.equals(clean)) return clean;
        if (tab != null) {
            tab.pendingHttpsOriginalUrl = clean;
            tab.pendingHttpsUpgradeUrl = secure;
            tab.pendingHttpsStartedAtMs = System.currentTimeMillis();
        }
        return secure;
    }

    private boolean startHttpsFirstOverrideIfNeeded(WebView view, String targetUrl, TabInfo tab) {
        if (view == null || !isHttpUrl(targetUrl)) return false;
        String secure = prepareHttpsFirstNavigation(targetUrl, tab);
        if (secure == null || secure.equals(targetUrl)) return false;
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

    private boolean isHttpsFallbackEligibleError(int errorCode) {
        return BrowserUtils.isHttpsFallbackEligibleError(errorCode);
    }

    private boolean handleHttpsFirstMainFrameFailure(WebView view, String failedUrl, int errorCode) {
        try {
            if (!httpsFirstEnabled || view == null || !isHttpsUrl(failedUrl)
                    || !isHttpsFallbackEligibleError(errorCode)) return false;
            TabInfo tab = findTabByWebView(view);
            if (tab == null && view == webView) tab = getCurrentTab();
            if (tab == null || tab.pendingHttpsOriginalUrl == null || tab.pendingHttpsOriginalUrl.length() == 0) return false;
            String expectedHost = hostOfUrl(tab.pendingHttpsUpgradeUrl);
            String failedHost = hostOfUrl(failedUrl);
            if (expectedHost.length() == 0 || failedHost.length() == 0
                    || (!sameOrSubDomain(failedHost, expectedHost) && !sameOrSubDomain(expectedHost, failedHost))) {
                return false;
            }

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

    private boolean equivalentUrlIgnoringSchemeAndFragment(String a, String b) {
        try {
            URI ua = new URI(a);
            URI ub = new URI(b);
            String ha = ua.getHost() == null ? "" : ua.getHost().toLowerCase(Locale.US);
            String hb = ub.getHost() == null ? "" : ub.getHost().toLowerCase(Locale.US);
            if (!ha.equals(hb)) return false;
            int pa = ua.getPort();
            int pb = ub.getPort();
            if (pa == 80 || pa == 443) pa = -1;
            if (pb == 80 || pb == 443) pb = -1;
            if (pa != pb) return false;
            String pathA = ua.getPath() == null || ua.getPath().length() == 0 ? "/" : ua.getPath();
            String pathB = ub.getPath() == null || ub.getPath().length() == 0 ? "/" : ub.getPath();
            String queryA = ua.getQuery() == null ? "" : ua.getQuery();
            String queryB = ub.getQuery() == null ? "" : ub.getQuery();
            return pathA.equals(pathB) && queryA.equals(queryB);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void upgradeBookmarksAfterHttpsSuccess(String originalHttpUrl, String finalHttpsUrl) {
        if (!isHttpUrl(originalHttpUrl) || !isHttpsUrl(finalHttpsUrl)) return;
        boolean changed = false;
        for (BookmarkItemData item : bookmarkData) {
            if (item == null || !isHttpUrl(item.url)) continue;
            String candidate = buildHttpsUpgradeUrl(item.url);
            if (equivalentUrlIgnoringSchemeAndFragment(item.url, originalHttpUrl)
                    || equivalentUrlIgnoringSchemeAndFragment(candidate, finalHttpsUrl)) {
                item.url = finalHttpsUrl;
                changed = true;
            }
        }
        if (changed) saveBookmarkData();
    }

    private void handleHttpsFirstNavigationSuccess(TabInfo tab, String finalUrl) {
        if (tab == null || !isHttpsUrl(finalUrl)) return;
        String original = tab.pendingHttpsOriginalUrl;
        if (original == null || original.length() == 0) {
            String finalHost = hostOfUrl(finalUrl);
            if (finalHost.equals(tab.httpsFallbackHost)) {
                tab.httpsFallbackHost = "";
                tab.httpsFallbackAllowedUntilMs = 0L;
            }
            return;
        }
        String expectedHost = hostOfUrl(tab.pendingHttpsUpgradeUrl);
        String finalHost = hostOfUrl(finalUrl);
        boolean related = expectedHost.length() > 0 && finalHost.length() > 0
                && (sameOrSubDomain(finalHost, expectedHost) || sameOrSubDomain(expectedHost, finalHost));
        if (related) upgradeBookmarksAfterHttpsSuccess(original, finalUrl);
        clearHttpsFirstPendingState(tab, related);
    }
    private boolean isExternalSchemeUrl(String url) {
        if (url == null || url.trim().length() == 0) return false;
        String u = url.trim().toLowerCase(Locale.US);
        if (u.startsWith("http://") || u.startsWith("https://")) return false;
        if (u.startsWith("about:") || u.startsWith("javascript:") || u.startsWith("data:")
                || u.startsWith("blob:") || u.startsWith("file:")) return false;
        return u.matches("^[a-z][a-z0-9+.-]*:.*");
    }

    private boolean isLikelyAdClickUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);
        if (isMediaResourceUrl(u) || isYoutubeCoreUrl(u) || isTrustedDownloadIntentUrl(u)) return false;
        return isExternalSchemeUrl(u)
                || u.contains("utm_medium=affiliates")
                || u.contains("utm_source=an_")
                || u.contains("affiliate")
                || u.contains("aff_sub")
                || u.contains("deep_and_deferred")
                || u.contains("navigate_url=")
                || u.contains("reactpath")
                || u.contains("click_id")
                || u.contains("adclick")
                || u.contains("ad_click")
                || u.contains("adurl=")
                || u.contains("af_click")
                || u.contains("tracking_id")
                || u.contains("campaign_id")
                || u.startsWith("shopeeid:")
                || u.startsWith("lazada:")
                || u.startsWith("tokopedia:")
                || u.startsWith("intent:")
                || u.startsWith("market:");
    }

    private void restoreAfterBlockedNavigation(WebView view, String blockedUrl) {
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

    private boolean isSuspiciousPopupNavigation(String targetUrl, String currentUrl) {
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

    private boolean isYoutubeCoreUrl(String url) {
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

    private boolean isMediaResourceUrl(String url) {
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

    private boolean isImageResourceUrl(String url) {
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

    private boolean isDirectImageMainFrameNavigation(String targetUrl, String currentUrl) {
        if (!isImageResourceUrl(targetUrl)) return false;
        if (currentUrl == null || currentUrl.length() == 0) return false;
        if (!isHttpOrHttpsUrl(currentUrl)) return false;
        return !isImageResourceUrl(currentUrl);
    }

    // v0.9.92: blocklist host dipindah ke konstanta static agar tidak dialokasikan ulang
    // pada setiap shouldInterceptRequest (dipanggil ratusan kali per halaman). Semantik
    // pencocokan tetap sama (substring contains pada URL) sehingga perilaku tidak berubah,
    // tetapi tekanan GC dan jank saat memuat/menggulir halaman berkurang signifikan.
    private static final String[] AD_URL_HOST_PATTERNS = new String[]{
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
    private static final byte[] TRANSPARENT_GIF_BYTES = new byte[]{
            (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61,
            (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x80, (byte) 0x00, (byte) 0x00,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x21, (byte) 0xF9, (byte) 0x04, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x2C, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
            (byte) 0x02, (byte) 0x02, (byte) 0x44, (byte) 0x01, (byte) 0x00, (byte) 0x3B
    };

    private WebResourceResponse buildBlockedResponse(String url) {
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

    private boolean isAdUrl(String url) {
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
    private boolean isPopUnderOrAdAsset(String u) {
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
    private String buildAdBlockCosmeticCss() {
        return BrowserUtils.buildAdBlockCosmeticCss();
    }

    // Injektor stylesheet idempoten: membuat sekali, lalu hanya memperbarui isi jika berubah.
    // CSS memakai tanda kutip tunggal pada attribute selector sehingga aman ditempel di string JS.
    private String buildCosmeticCssInjectorJs() {
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
    private void injectAdBlockCssEarly() {
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
    private String escapeForJsDoubleQuotes(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '<': sb.append("\\u003C"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private void loadUserElementFilters() {
        if (userElementFiltersLoaded) return;
        userElementFiltersLoaded = true;
        try {
            SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
            Set<String> hosts = p.getStringSet("ef_hosts", null);
            if (hosts == null) return;
            for (String host : hosts) {
                if (host == null || host.length() == 0) continue;
                Set<String> sels = p.getStringSet("ef_" + host, null);
                if (sels == null || sels.isEmpty()) continue;
                userElementFilters.put(host, new LinkedHashSet<>(sels));
            }
        } catch (Exception ignored) {
        }
    }

    private void persistUserElementFiltersForHost(String host) {
        if (host == null || host.length() == 0) return;
        try {
            SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
            SharedPreferences.Editor e = p.edit();
            LinkedHashSet<String> sels = userElementFilters.get(host);
            Set<String> hosts = new LinkedHashSet<>(p.getStringSet("ef_hosts", new LinkedHashSet<>()));
            if (sels == null || sels.isEmpty()) {
                e.remove("ef_" + host);
                hosts.remove(host);
            } else {
                e.putStringSet("ef_" + host, new LinkedHashSet<>(sels));
                hosts.add(host);
            }
            e.putStringSet("ef_hosts", hosts);
            e.apply();
        } catch (Exception ignored) {
        }
    }

    private LinkedHashSet<String> userFiltersForHost(String host) {
        loadUserElementFilters();
        if (host == null || host.length() == 0) return null;
        return userElementFilters.get(host);
    }

    private boolean hasUserFiltersForCurrentHost() {
        LinkedHashSet<String> s = userFiltersForHost(hostOfUrl(getEffectiveCurrentUrl()));
        return s != null && !s.isEmpty();
    }

    private boolean addUserElementFilter(String host, String selector) {
        if (host == null || host.length() == 0 || selector == null) return false;
        selector = selector.trim();
        if (!UserElementFilterPolicy.isSafeSelector(selector, "")) return false;
        loadUserElementFilters();
        LinkedHashSet<String> set = userElementFilters.get(host);
        if (set == null) {
            set = new LinkedHashSet<>();
            userElementFilters.put(host, set);
        }
        boolean added = set.add(selector);
        if (added) persistUserElementFiltersForHost(host);
        return added;
    }

    private void removeUserElementFilter(String host, String selector) {
        if (host == null || selector == null) return;
        loadUserElementFilters();
        LinkedHashSet<String> set = userElementFilters.get(host);
        if (set == null) return;
        if (set.remove(selector)) {
            if (set.isEmpty()) userElementFilters.remove(host);
            persistUserElementFiltersForHost(host);
        }
    }

    private String buildUserFilterCss(String host) {
        LinkedHashSet<String> set = userFiltersForHost(host);
        if (set == null || set.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        final String hideRule = "{display:none!important;visibility:hidden!important;"
                + "height:0!important;min-height:0!important;max-height:0!important;"
                + "margin:0!important;padding:0!important;border:0!important;"
                + "overflow:hidden!important;pointer-events:none!important;}\n";
        for (String sel : set) {
            if (!UserElementFilterPolicy.isSafeSelector(sel, "")) continue;
            sb.append(sel.trim()).append(hideRule);
        }
        return sb.toString();
    }

    // Filter manual merupakan fitur mandiri: selalu diterapkan walaupun AdBlock utama OFF dan
    // tetap aktif pada compatibility mode. CSS tersimpan per host dan dipasang ulang bila situs
    // mengganti <head> atau menghapus style saat SPA/dynamic navigation.
    private void applyUserFiltersForCurrentPage() {
        if (webView == null) return;
        try {
            String cur = getEffectiveCurrentUrl();
            String host = hostOfUrl(cur);
            String css = buildUserFilterCss(host);
            String js = "javascript:(function(){try{"
                    + "var css=\"" + escapeForJsDoubleQuotes(css == null ? "" : css) + "\";"
                    + "var id='yield-user-filters';"
                    + "window.__yieldUserFiltersCss=css;"
                    + "window.__yieldEnsureUserFilters=function(){try{"
                    + "var root=document.head||document.documentElement||document.body;if(!root)return;"
                    + "var el=document.getElementById(id);"
                    + "if(!window.__yieldUserFiltersCss){if(el)el.textContent='';return;}"
                    + "if(!el){el=document.createElement('style');el.id=id;el.setAttribute('type','text/css');root.appendChild(el);}"
                    + "if(el.textContent!==window.__yieldUserFiltersCss)el.textContent=window.__yieldUserFiltersCss;"
                    + "}catch(e){}};"
                    + "window.__yieldEnsureUserFilters();"
                    + "if(!css){"
                    + "if(window.__yieldUserFiltersObserver){try{window.__yieldUserFiltersObserver.disconnect();}catch(e){}window.__yieldUserFiltersObserver=null;}"
                    + "return;"
                    + "}"
                    + "if(!window.__yieldUserFiltersObserver&&window.MutationObserver&&document.documentElement){"
                    + "window.__yieldUserFiltersObserver=new MutationObserver(function(){window.__yieldEnsureUserFilters();});"
                    + "window.__yieldUserFiltersObserver.observe(document.documentElement,{childList:true,subtree:true});"
                    + "}"
                    + "setTimeout(window.__yieldEnsureUserFilters,250);"
                    + "setTimeout(window.__yieldEnsureUserFilters,1000);"
                    + "setTimeout(window.__yieldEnsureUserFilters,3000);"
                    + "}catch(e){}})();";
            runPageScript(js);
        } catch (Exception ignored) {
        }
    }

    private String buildElementPickerJs() {
        return ElementPickerScript.build();
    }

    private void startElementPicker() {
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

    private void onPickerElementSelected(String selector, String preview,
                                         int matchCount, String selectedTag) {
        if (!elementPickerActive) return;
        if (!UserElementFilterPolicy.isSafeSelector(selector, selectedTag)) {
            QuietToast.makeText(this, "Elemen penting atau selector tidak aman tidak dapat diblokir",
                    QuietToast.LENGTH_SHORT).show();
            continueElementPickerAfterSelection();
            return;
        }
        final String sel = selector.trim();
        final String host = hostOfUrl(getEffectiveCurrentUrl());
        if (host == null || host.length() == 0) {
            QuietToast.makeText(this, "Tidak bisa menentukan domain halaman ini",
                    QuietToast.LENGTH_SHORT).show();
            finishElementPicker(false);
            return;
        }
        if (elementPickerDialog != null) {
            try { elementPickerDialog.dismiss(); } catch (Exception ignored) {}
            elementPickerDialog = null;
        }

        String previewText = preview == null ? "" : preview.trim();
        if (previewText.length() > 180) previewText = previewText.substring(0, 180) + "\u2026";
        int safeCount = UserElementFilterPolicy.normalizedMatchCount(matchCount);
        StringBuilder msg = new StringBuilder();
        msg.append("Selector:\n").append(sel);
        if (safeCount > 0) {
            msg.append("\n\nAkan menyembunyikan ")
                    .append(safeCount)
                    .append(safeCount == 1 ? " elemen." : " elemen pada halaman ini.");
        }
        if (previewText.length() > 0) msg.append("\n\nPratinjau:\n").append(previewText);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Blokir elemen ini?");
        b.setMessage(msg.toString());
        b.setPositiveButton("Blokir & lanjut", (d, w) -> {
            boolean added = addUserElementFilter(host, sel);
            applyUserFiltersForCurrentPage();
            continueElementPickerAfterSelection();
            QuietToast.makeText(this,
                    added ? "Elemen diblokir permanen di " + host
                            : "Filter ini sudah tersimpan di " + host,
                    QuietToast.LENGTH_SHORT).show();
        });
        b.setNeutralButton("Naik 1 induk", (d, w) -> {
            elementPickerDialog = null;
            mainHandler.postDelayed(() -> runPageScript(
                    "javascript:(function(){try{if(window.__yieldPickerParent)window.__yieldPickerParent();}catch(e){}})();"
            ), 80L);
        });
        b.setNegativeButton("Batal pilihan", (d, w) -> continueElementPickerAfterSelection());
        b.setOnCancelListener(d -> continueElementPickerAfterSelection());
        elementPickerDialog = b.create();
        elementPickerDialog.setOnDismissListener(d -> {
            if (elementPickerDialog == d) elementPickerDialog = null;
        });
        try { elementPickerDialog.show(); } catch (Exception ignored) {}
    }

    private void continueElementPickerAfterSelection() {
        if (!elementPickerActive) return;
        if (elementPickerDialog != null) {
            try { elementPickerDialog.dismiss(); } catch (Exception ignored) {}
            elementPickerDialog = null;
        }
        runPageScript("javascript:(function(){try{if(window.__yieldPickerContinue)window.__yieldPickerContinue();}catch(e){}})();");
    }

    private void finishElementPicker(boolean committed) {
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

    private void showUserFiltersManager() {
        final String host = hostOfUrl(getEffectiveCurrentUrl());
        loadUserElementFilters();
        LinkedHashSet<String> set = (host == null || host.length() == 0) ? null : userElementFilters.get(host);

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackground(roundRect(Color.parseColor("#2B2D33"), dp(20), dp(1), Color.parseColor("#2D333D")));

        TextView title = new TextView(this);
        title.setText("Filter elemen \u2014 " + (host == null || host.length() == 0 ? "(situs ini)" : host));
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setPadding(0, 0, 0, dp(10));
        root.addView(title);

        if (set == null || set.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada elemen yang diblokir di situs ini.\nGunakan \"Blokir elemen\" lalu ketuk iklannya.");
            empty.setTextColor(Color.parseColor("#B7BDC8"));
            empty.setTextSize(14);
            root.addView(empty);
        } else {
            ScrollView sv = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            for (final String sel : new ArrayList<>(set)) {
                LinearLayout rowL = new LinearLayout(this);
                rowL.setOrientation(LinearLayout.HORIZONTAL);
                rowL.setGravity(Gravity.CENTER_VERTICAL);
                rowL.setPadding(0, dp(8), 0, dp(8));

                TextView t = new TextView(this);
                t.setText(sel);
                t.setTextColor(Color.parseColor("#E6E9EF"));
                t.setTextSize(13);
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
                rowL.addView(t, tp);

                TextView del = new TextView(this);
                del.setText("Hapus");
                del.setTextColor(Color.parseColor("#F87171"));
                del.setTextSize(14);
                del.setPadding(dp(12), dp(6), dp(6), dp(6));
                del.setOnClickListener(v -> {
                    removeUserElementFilter(host, sel);
                    applyUserFiltersForCurrentPage();
                    dialog.dismiss();
                    showUserFiltersManager();
                });
                rowL.addView(del);
                list.addView(rowL);
            }
            sv.addView(list);
            LinearLayout.LayoutParams svp = new LinearLayout.LayoutParams(-1, dp(320));
            root.addView(sv, svp);

            root.addView(menuDivider());
            root.addView(menuRow(R.drawable.ic_clear, "Hapus semua filter situs ini", v -> {
                if (host != null && host.length() > 0) {
                    userElementFilters.remove(host);
                    persistUserElementFiltersForHost(host);
                    applyUserFiltersForCurrentPage();
                }
                dialog.dismiss();
                QuietToast.makeText(this, "Filter situs dihapus", QuietToast.LENGTH_SHORT).show();
            }));
        }

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = dp(330);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
        dialog.show();
    }


    private void injectPremiumAdBlock() {
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

        String js = "javascript:(function(){"
                + "if(window.__yieldAdBlockPremiumV2){try{if(window.__yieldABRun)window.__yieldABRun();}catch(e){}return;}"
                + "window.__yieldAdBlockPremiumV2=true;window.__yieldAdBlockPremium=true;"
                + "var Y_POPUP=" + popupEnabled + ",Y_CLICK=" + clickEnabled + ",Y_SCRIPT=" + scriptEnabled + ";"
                + "function hostOf(u){try{var a=document.createElement('a');a.href=u;return (a.hostname||'').replace(/^www\\./,'').toLowerCase();}catch(e){return '';}}"
                + "function media(u){try{var s=(u||'').toLowerCase();return s.indexOf('googlevideo.com/videoplayback')>-1||s.indexOf('/videoplayback')>-1||s.indexOf('.mp4')>-1||s.indexOf('.m3u8')>-1||s.indexOf('.mpd')>-1||s.indexOf('.webm')>-1||s.indexOf('.m4s')>-1||s.indexOf('.ts')>-1||s.indexOf('mime=video')>-1||s.indexOf('mime%3dvideo')>-1;}catch(e){return false;}}"
                + "function safeVideoHost(h){return h.indexOf('youtube.com')>-1||h.indexOf('youtu.be')>-1||h.indexOf('googlevideo.com')>-1||h.indexOf('ytimg.com')>-1||h.indexOf('ggpht.com')>-1;}"
                + "function dlintent(u){try{var s=(u||'').toLowerCase();var h=hostOf(u);var trusted=h==='drive.usercontent.google.com'||h==='drive.google.com'||h==='docs.google.com'||h.endsWith('.googleusercontent.com')||h==='github.com'||h.endsWith('.githubusercontent.com')||h.endsWith('.sourceforge.net')||h.endsWith('.mediafire.com')||h.endsWith('.dropbox.com')||h.endsWith('.dropboxusercontent.com')||h==='onedrive.live.com'||h==='1drv.ms'||h==='mega.nz'||h.endsWith('.mega.nz')||h==='pixeldrain.com'||h==='gofile.io'||h==='archive.org';var marker=s.indexOf('/download')>-1||s.indexOf('download=')>-1||s.indexOf('export=download')>-1||s.indexOf('dl=1')>-1||s.indexOf('response-content-disposition=attachment')>-1||s.indexOf('content-disposition=attachment')>-1||s.indexOf('filename=')>-1||s.indexOf('confirm=')>-1||s.indexOf('uuid=')>-1||s.indexOf('/releases/download/')>-1;var file=false;var ex=['.zip','.rar','.7z','.apk','.apks','.xapk','.pdf','.doc','.docx','.xls','.xlsx','.ppt','.pptx','.csv','.txt','.epub','.mp3','.m4a','.wav','.ogg','.mp4','.mkv','.webm','.avi','.mov','.ts','.m3u8','.iso','.img','.bin','.exe','.msi'];for(var i=0;i<ex.length;i++){if(s.indexOf(ex[i])>-1){file=true;break;}}var hard=s.indexOf('adclick')>-1||s.indexOf('ad_click')>-1||s.indexOf('adurl=')>-1||s.indexOf('click_id')>-1||s.indexOf('af_click')>-1||s.indexOf('clickunder')>-1||s.indexOf('popunder')>-1||s.indexOf('popupads')>-1||s.indexOf('onclickads')>-1||s.indexOf('interstitial')>-1||s.indexOf('utm_medium=affiliates')>-1||s.indexOf('deep_and_deferred')>-1||s.indexOf('navigate_url=')>-1||s.indexOf('reactpath')>-1;var badhost=h.endsWith('.cfd')||h.endsWith('.click')||h.endsWith('.cam')||h.endsWith('.monster')||h.endsWith('.quest')||h.endsWith('.buzz')||h.endsWith('.icu')||h.endsWith('.cyou')||h.indexOf('onclickads')>-1||h.indexOf('clickadu')>-1||h.indexOf('popads')>-1||h.indexOf('popcash')>-1||h.indexOf('propellerads')>-1||h.indexOf('adsterra')>-1||h.indexOf('hilltopads')>-1||h.indexOf('exoclick')>-1||h.indexOf('doubleclick')>-1||h.indexOf('googlesyndication')>-1||h.indexOf('googleadservices')>-1;return (trusted&&(marker||file))||((marker||file)&&!hard&&!badhost);}catch(e){return false;}}"
                + "function bad(u){try{if(!u||media(u)||dlintent(u))return false;var s=(u||'').toLowerCase();if(/^[a-z][a-z0-9+.-]*:/.test(s)&&s.indexOf('http://')!==0&&s.indexOf('https://')!==0&&s.indexOf('javascript:')!==0&&s.indexOf('data:')!==0&&s.indexOf('blob:')!==0)return true;if(/(utm_medium=affiliates|deep_and_deferred|navigate_url=|reactpath|click_id|adclick|ad_click|adurl=|af_click|tracking_id|campaign_id)/.test(s))return true;var h=hostOf(u);var cur=(location.hostname||'').replace(/^www\\./,'').toLowerCase();if(!h||h===cur||h.endsWith('.'+cur))return false;if(safeVideoHost(h))return false;if(/(hotterydiseur|sewarsremeets|onclickads|clickadu|popads|popcash|propellerads|adsterra|hilltopads|exoclick|realsrv|invest-tracing|doubleclick|googlesyndication|googleadservices)/.test(h))return true;if(/\\.(cfd|click|cam|monster|quest|buzz|icu|cyou)$/.test(h))return true;if(/\\.(shop|xyz|top|site|space|online|live|fun|lol)$/.test(h)&&/[\\/][a-z0-9_-]{8,}/.test(s))return true;if(/(popunder|popup|redirect|adclick|clickunder|interstitial|push)/.test(s))return true;return false;}catch(e){return false;}}"
                + "function visibleAnchor(a){try{if(!a||!a.getBoundingClientRect)return false;var r=a.getBoundingClientRect();var t=((a.innerText||a.textContent||a.getAttribute('aria-label')||a.title||'')+'').trim();var hasMedia=!!a.querySelector('img,svg,picture,button');return r.width>12&&r.height>8&&(t.length>0||hasMedia);}catch(e){return false;}}"
                + "function allowClickedAnchor(a,u){try{if(!a||!u)return false;var s=(u||'').toLowerCase();if(media(s)||dlintent(s))return true;var h=hostOf(u);var cur=(location.hostname||'').replace(/^www\\./,'').toLowerCase();if(!h)return false;if(h===cur||h.endsWith('.'+cur))return true;if(!visibleAnchor(a))return false;if(/(intent:|market:|shopeeid:|lazada:|tokopedia:|adclick|ad_click|adurl=|clickunder|popunder|popup|interstitial|push)/.test(s))return false;return true;}catch(e){return false;}}"
                + "if(Y_POPUP&&!window.__yieldOpenPatched){window.__yieldOpenPatched=true;var oldOpen=window.open;window.open=function(u,n,f){if(bad(u)){try{if(window.YieldAdBlockBridge)YieldAdBlockBridge.onAdRedirect(String(u));}catch(e){}return {closed:true,focus:function(){},close:function(){}};}try{return oldOpen.call(window,u,n,f);}catch(e){return {closed:true,focus:function(){},close:function(){}};}};}"
                + "if(Y_CLICK&&!window.__yieldClickPatched){window.__yieldClickPatched=true;document.addEventListener('click',function(e){try{var a=e.target&&e.target.closest?e.target.closest('a[href]'):null;if(a&&bad(a.href)&&!allowClickedAnchor(a,a.href)){try{if(window.YieldAdBlockBridge)YieldAdBlockBridge.onAdRedirect(String(a.href));}catch(ee){}e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();return false;}}catch(x){}},true);document.addEventListener('auxclick',function(e){try{var a=e.target&&e.target.closest?e.target.closest('a[href]'):null;if(a&&bad(a.href)&&!allowClickedAnchor(a,a.href)){try{if(window.YieldAdBlockBridge)YieldAdBlockBridge.onAdRedirect(String(a.href));}catch(ee){}e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();return false;}}catch(x){}},true);}"
                + "function softHide(s){try{document.querySelectorAll(s).forEach(function(e){if(!e||e.tagName==='VIDEO')return;e.style.setProperty('display','none','important');e.style.setProperty('visibility','hidden','important');e.style.setProperty('height','0px','important');e.style.setProperty('min-height','0px','important');e.style.setProperty('overflow','hidden','important');});}catch(x){}}"
                + "function hardHide(s){try{document.querySelectorAll(s).forEach(function(e){if(!e||e.tagName==='VIDEO')return;e.style.setProperty('display','none','important');e.remove&&e.remove();});}catch(x){}}"
                + "function clickSkip(){try{document.querySelectorAll('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytp-skip-ad-button__button,button[class*=skip],button[id*=skip],.skip,.skip-ad,.skip-button,.vjs-skip-button,.jw-skip,.jw-skiptext,.jw-skip-icon').forEach(function(b){try{var t=(b.innerText||b.textContent||'').toLowerCase();if(t.indexOf('skip')>-1||t.indexOf('lewati')>-1||t.length<25||String(b.className).toLowerCase().indexOf('skip')>-1)b.click();}catch(e){}});}catch(e){}}"
                + "function isYT(){var h=(location.hostname||'').toLowerCase();return h.indexOf('youtube.com')>-1||h.indexOf('youtu.be')>-1;}"
                + "function isYouTubeAd(){try{var p=document.querySelector('.html5-video-player');var cls=p?String(p.className):'';return cls.indexOf('ad-showing')>-1||cls.indexOf('ad-interrupting')>-1||!!document.querySelector('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytp-ad-text,.ytp-ad-image-overlay,.ytp-ad-player-overlay');}catch(e){return false;}}"
                + "function isGenericVideoAd(){try{var s=(document.body&&document.body.innerText||'').toLowerCase();var text=/(advertisement|sponsored|iklan|ads? will end|ad will end|skip ad|lewati iklan|continue to video)/.test(s);var el=!!document.querySelector('.vast,.vpaid,.ima-ad-container,.ima-ad,.googleima,.ad-container,.ad-overlay,.video-ads,.preroll,.pre-roll,.midroll,.mid-roll,.jw-ad,.jw-flag-ads,.jw-ad-visible,.vjs-ad-playing,.vjs-ima3-ad-container,.vjs-ad-container,.plyr__ads,.ad-player,.ad-wrapper,[class*=vast],[class*=vpaid],[class*=preroll],[class*=midroll],[id*=preroll],[id*=midroll]');return text||el;}catch(e){return false;}}"
                + "function safeSpeedAd(v,fast){try{if(!v)return;if(!v.__yieldAdSaved){v.__yieldWasMuted=v.muted;v.__yieldWasRate=v.playbackRate||1;v.__yieldAdSaved=true;v.__yieldAdTicks=0;}v.__yieldAdTicks=(v.__yieldAdTicks||0)+1;v.muted=true;try{v.defaultMuted=true;}catch(e){}try{v.playbackRate=Math.max(fast,16);}catch(e){}try{v.defaultPlaybackRate=Math.max(fast,16);}catch(e){}v.play&&v.play().catch(function(){});if(isFinite(v.duration)&&v.duration>1&&v.duration<240){var cur=isFinite(v.currentTime)?v.currentTime:0;var step=v.__yieldAdTicks>3?20:12;var target=cur+step;if(v.__yieldAdTicks>7&&v.duration<90)target=v.duration-0.18;var next=Math.min(v.duration-0.12,target);if(next>cur){v.currentTime=next;try{v.dispatchEvent(new Event('seeking'));v.dispatchEvent(new Event('timeupdate'));}catch(e){}}}}catch(e){}}"
                + "function restoreVideo(v){try{if(v&&v.__yieldAdSaved){v.muted=!!v.__yieldWasMuted;try{v.defaultMuted=!!v.__yieldWasMuted;}catch(e){}v.playbackRate=v.__yieldWasRate||1;try{v.defaultPlaybackRate=v.__yieldWasRate||1;}catch(e){}v.__yieldAdSaved=false;v.__yieldAdTicks=0;}}catch(e){}}"
                + "function hideYouTubeSponsorBlocks(){try{var sels=['ytm-promoted-video-renderer','ytm-promoted-sparkles-web-renderer','ytm-ad-slot-renderer','ytm-companion-slot-renderer','ytd-promoted-video-renderer','ytd-display-ad-renderer','ytd-ad-slot-renderer','ytd-companion-slot-renderer','#player-ads'];sels.forEach(function(s){try{document.querySelectorAll(s).forEach(function(e){if(!e||e.querySelector('video'))return;e.style.setProperty('display','none','important');e.style.setProperty('height','0px','important');e.style.setProperty('overflow','hidden','important');});}catch(x){}});}catch(e){}}"
                + "function bypassVideoAds(){try{clickSkip();var yt=isYT()&&isYouTubeAd();var gen=!yt&&isGenericVideoAd();var v=document.querySelector('video');if(yt){safeSpeedAd(v,16);softHide('.ytp-ad-overlay-container,.ytp-ad-text,.ytp-ad-image-overlay');return;}if(gen){safeSpeedAd(v,8);hardHide('.ima-ad-container,.ima-ad,.googleima,.ad-overlay,.video-ads,.preroll,.pre-roll,.midroll,.mid-roll,.jw-ad,.jw-ad-visible,.vjs-ima3-ad-container,.vjs-ad-container,.plyr__ads,.ad-player,.ad-wrapper,[class*=vast],[class*=vpaid]');return;}restoreVideo(v);}catch(e){}}"
                + "function ytState(){try{window.__yieldYTAdV2=window.__yieldYTAdV2||{active:false,lastAd:0,ticks:0,skipClicks:0};return window.__yieldYTAdV2;}catch(e){return {active:false,lastAd:0,ticks:0};}}"
                + "function repairYouTubeVisibility(){try{['html','body','#player','#movie_player','.html5-video-player','video','ytm-app','ytd-app','#app','#content','#contents'].forEach(function(s){document.querySelectorAll(s).forEach(function(e){try{e.style.removeProperty('display');e.style.removeProperty('visibility');e.style.removeProperty('height');e.style.removeProperty('min-height');e.style.removeProperty('opacity');}catch(x){}});});}catch(e){}}"
                + "function installYouTubeSafeStyle(){try{if(document.getElementById('yield-yt-safe-ad-style'))return;var st=document.createElement('style');st.id='yield-yt-safe-ad-style';st.textContent='.ytp-ad-overlay-container,.ytp-ad-text,.ytp-ad-image-overlay,.ytp-ad-player-overlay,.ytp-ad-player-overlay-instream-info,.ytp-ad-preview-container,.ytp-ad-action-interstitial,.ytp-ad-survey,.ytp-ad-progress-list,.ytp-ad-button,.ytp-ad-badge,.ytp-paid-content-overlay,ytd-player-legacy-desktop-watch-ads-renderer,ytd-action-companion-ad-renderer,ytd-display-ad-renderer,ytd-promoted-video-renderer,ytd-ad-slot-renderer,ytd-companion-slot-renderer,ytm-promoted-video-renderer,ytm-promoted-sparkles-web-renderer,ytm-ad-slot-renderer,ytm-companion-slot-renderer,#player-ads{display:none!important;visibility:hidden!important;opacity:0!important;pointer-events:none!important;max-height:0!important;overflow:hidden!important;}';(document.head||document.documentElement).appendChild(st);}catch(e){}}"
                + "function ytAdVideo(){try{return document.querySelector('.html5-video-player.ad-showing video,.html5-video-player.ad-interrupting video,#movie_player.ad-showing video,#movie_player.ad-interrupting video')||document.querySelector('#movie_player video')||document.querySelector('video');}catch(e){return null;}}"
                + "function youtubeAdSignalV2(){try{var p=document.querySelector('.html5-video-player,#movie_player');var cls=p?String(p.className||''):'';if(/ad-showing|ad-interrupting|ad-created|ad-playing/.test(cls))return true;var q='.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytp-skip-ad-button__button,.ytp-ad-preview-container,.ytp-ad-player-overlay,.ytp-ad-player-overlay-instream-info,.ytp-ad-text,.ytp-ad-image-overlay,.ytp-ad-simple-ad-badge,.ytp-ad-badge,.ytp-ad-action-interstitial,.ytp-ad-survey,.video-ads.ytp-ad-module';if(document.querySelector(q))return true;var t=((document.querySelector('.ytp-ad-text,.ytp-ad-preview-text,.ytp-ad-simple-ad-badge,.ytp-ad-badge')||{}).textContent||'').toLowerCase();return /(ad|ads|iklan|sponsored|skip|lewati)/.test(t);}catch(e){return false;}}"
                + "function clickYouTubeSkipV2(){try{var sels=['.ytp-ad-skip-button','.ytp-ad-skip-button-modern','.ytp-skip-ad-button','.ytp-skip-ad-button__button','.ytm-ad-skip-button','button[aria-label*=\"Skip\"]','button[aria-label*=\"skip\"]','button[aria-label*=\"Lewati\"]','button[aria-label*=\"lewati\"]','button[class*=skip]'];sels.forEach(function(s){document.querySelectorAll(s).forEach(function(b){try{if(!b||b.disabled)return;var r=b.getBoundingClientRect?b.getBoundingClientRect():{width:1,height:1};var txt=((b.innerText||b.textContent||b.getAttribute('aria-label')||b.title||'')+'').toLowerCase();if(r.width>=0&&r.height>=0&&(txt.indexOf('skip')>-1||txt.indexOf('lewati')>-1||String(b.className).toLowerCase().indexOf('skip')>-1)){b.click();ytState().skipClicks++;}}catch(x){}});});}catch(e){}}"
                + "function fastForwardYouTubeAdV2(v){try{var st=ytState();if(!v)return;var now=Date.now();st.lastAd=now;st.ticks=(st.ticks||0)+1;if(!st.active){st.active=true;st.savedMuted=v.muted;st.savedRate=v.playbackRate||1;st.savedDefaultRate=v.defaultPlaybackRate||1;st.savedVolume=v.volume;}try{v.muted=true;v.defaultMuted=true;if(v.volume>0.01)v.volume=0;}catch(e){}try{v.playbackRate=16;v.defaultPlaybackRate=16;}catch(e){}try{if(v.paused&&v.play)v.play().catch(function(){});}catch(e){}try{if(isFinite(v.duration)&&v.duration>1&&v.duration<240){var cur=isFinite(v.currentTime)?v.currentTime:0;var jump=st.ticks>2?18:8;var target=cur+jump;if(st.ticks>5||st.skipClicks>0)target=v.duration-0.16;var next=Math.min(v.duration-0.12,target);if(next>cur){v.currentTime=next;v.dispatchEvent(new Event('timeupdate'));}}}catch(e){}}catch(e){}}"
                + "function restoreYouTubeVideoV2(v){try{var st=ytState();if(!st.active)return;if(Date.now()-(st.lastAd||0)<1700)return;if(v){try{v.muted=!!st.savedMuted;v.defaultMuted=!!st.savedMuted;}catch(e){}try{if(isFinite(st.savedVolume))v.volume=st.savedVolume;}catch(e){}try{v.playbackRate=st.savedRate||1;v.defaultPlaybackRate=st.savedDefaultRate||st.savedRate||1;}catch(e){}}st.active=false;st.ticks=0;st.skipClicks=0;}catch(e){}}"
                + "function bypassYouTubeAdsV2(){try{if(!isYT())return false;repairYouTubeVisibility();installYouTubeSafeStyle();clickYouTubeSkipV2();clickSkip();var v=ytAdVideo();var ad=youtubeAdSignalV2();if(ad){softHide('.ytp-ad-overlay-container,.ytp-ad-text,.ytp-ad-image-overlay,.ytp-ad-player-overlay,.ytp-ad-player-overlay-instream-info,.ytp-ad-preview-container,.ytp-ad-action-interstitial,.ytp-ad-survey');fastForwardYouTubeAdV2(v);return true;}restoreYouTubeVideoV2(v);return false;}catch(e){return false;}}"
                + "function bypassVideoAdsV2(){try{if(bypassYouTubeAdsV2())return;var gen=isGenericVideoAd();var v=document.querySelector('video');if(gen){safeSpeedAd(v,8);hardHide('.ima-ad-container,.ima-ad,.googleima,.ad-overlay,.video-ads,.preroll,.pre-roll,.midroll,.mid-roll,.jw-ad,.jw-ad-visible,.vjs-ima3-ad-container,.vjs-ad-container,.plyr__ads,.ad-player,.ad-wrapper,[class*=vast],[class*=vpaid]');return;}restoreVideo(v);}catch(e){}}"
                + "function setupYouTubeAdObserver(){try{if(window.__yieldYTAdObserverInstalled)return;window.__yieldYTAdObserverInstalled=true;var run=function(){try{bypassYouTubeAdsV2();}catch(e){}};var mo=new MutationObserver(function(){run();});mo.observe(document.documentElement||document,{childList:true,subtree:true,attributes:true,attributeFilter:['class','style','aria-label']});document.addEventListener('yt-navigate-finish',function(){setTimeout(run,80);setTimeout(run,500);},true);document.addEventListener('visibilitychange',run,true);setInterval(run,180);}catch(e){}}"
                + "function clean(){"
                + "var yt=isYT();try{if(yt){['html','body','ytm-app','ytd-app','#app','#content','#contents','ytm-browse','ytm-watch','ytm-single-column-watch-next-results-renderer','ytm-item-section-renderer','ytm-rich-grid-renderer','ytm-video-with-context-renderer','ytm-playlist-video-renderer','ytm-slim-video-metadata-renderer','ytm-watch-metadata','ytd-page-manager','ytd-watch-flexy','ytd-rich-grid-renderer'].forEach(function(s){document.querySelectorAll(s).forEach(function(e){e.style.removeProperty('display');e.style.removeProperty('visibility');e.style.removeProperty('height');e.style.removeProperty('min-height');e.style.removeProperty('overflow');e.style.removeProperty('opacity');});});}}catch(e){}try{if(yt&&document.body&&document.body.innerText.trim().length<20&&document.querySelector('ytm-app')){['html','body','ytm-app','#app','#content','#contents'].forEach(function(s){document.querySelectorAll(s).forEach(function(e){e.style.cssText=e.style.cssText.replace(/display\\s*:\\s*none\\s*!important;?/gi,'').replace(/height\\s*:\\s*0px\\s*!important;?/gi,'').replace(/visibility\\s*:\\s*hidden\\s*!important;?/gi,'');});});}}catch(e){}bypassVideoAdsV2();if(yt){hideYouTubeSponsorBlocks();setupYouTubeAdObserver();}"
                + "try{if(Y_CLICK)document.querySelectorAll('a[target=_blank],a[target=\\\"_blank\\\"]').forEach(function(a){if(bad(a.href)){a.removeAttribute('target');a.setAttribute('rel','noopener noreferrer');}});}catch(e){}"
                + "var selectors=yt?["
                + "'ytd-display-ad-renderer','ytd-promoted-video-renderer','ytd-ad-slot-renderer','ytd-companion-slot-renderer','ytd-banner-promo-renderer','ytd-in-feed-ad-layout-renderer','ytd-promoted-sparkles-web-renderer','ytm-promoted-sparkles-web-renderer','ytm-promoted-video-renderer','ytm-ad-slot-renderer','ytm-companion-slot-renderer','ytm-in-feed-ad-layout-renderer','ytm-display-ad-renderer','#player-ads','.ytp-ad-overlay-container','.ytp-ad-text','.ytp-ad-image-overlay','.ytp-ad-progress-list','.GoogleActiveViewElement'"
                + "]:["
                + "'.adsbygoogle','iframe[id*=ad]','iframe[src*=ads]','iframe[src*=doubleclick]','iframe[src*=onclickads]','iframe[src*=clickadu]','iframe[src*=popads]','iframe[src*=propellerads]','[id*=ad-]','[id^=ad_]','[class*=ad-]','[class*=ads-]','[class*=advert]','.GoogleActiveViewElement','.ad-banner','.ad-container','.advertisement','.sponsored','.ima-ad-container','.ima-ad','.googleima','.ad-overlay','.video-ads','.preroll','.pre-roll','.midroll','.mid-roll','.vjs-ima3-ad-container','.vjs-ad-container','.plyr__ads'"
                + "];"
                + "if(Y_SCRIPT&&yt){selectors.forEach(hardHide);}"
                + "try{if(Y_SCRIPT)document.querySelectorAll('iframe[src],script[src]').forEach(function(el){var u=el.src||'';if(bad(u)){el.remove();}});}catch(e){}"
                + "try{document.body.style.setProperty('overflow','auto','important');}catch(e){}"
                + "}"
                + "window.__yieldABRun=clean;"
                + "var __abP=false;function __abS(){if(__abP)return;__abP=true;(window.requestAnimationFrame||function(f){setTimeout(f,16);})(function(){__abP=false;try{clean();}catch(e){}});}"
                + "try{if(window.__yieldABObs&&window.__yieldABObs.disconnect)window.__yieldABObs.disconnect();}catch(e){}"
                + "try{window.__yieldABObs=new MutationObserver(function(){__abS();});window.__yieldABObs.observe(document.documentElement||document.body||document,{childList:true,subtree:true});}catch(e){}"
                + "try{if(window.__yieldABInt)clearInterval(window.__yieldABInt);}catch(e){}"
                + "window.__yieldABInt=setInterval(function(){try{if(document.querySelector('video'))clean();}catch(e){}},700);"
                + "clean();setTimeout(clean,120);setTimeout(clean,500);setTimeout(clean,1500);setTimeout(clean,3500);"
                + "})();";
        runPageScript(js);
    }

    private void injectCompatibilityAdShield() {
        if (webView == null) return;
        String pageUrl = getEffectiveCurrentUrl();
        if (!(isStrictSiteCompatibilityUrl(pageUrl)
                || isSiteCompatibilityModeActiveForUrl(pageUrl)
                || isReloadLoopGuardActiveForUrl(pageUrl)
                || ReaderCompatibilityPolicy.hasReaderPathHint(pageUrl)
                || ShieldEngineV2.isPopupIsolationContentPage(pageUrl))) return;
        injectShieldEngineV2Fallback();
    }

    private boolean isYouTubePageUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);
        return u.contains("youtube.com") || u.contains("m.youtube.com") || u.contains("www.youtube.com") || u.contains("youtu.be");
    }

    private boolean isYouTubePlaybackUrl(String url) {
        if (isYouTubePageUrl(url)) return true;
        if (currentPageUrlForRequest != null && isYouTubePageUrl(currentPageUrlForRequest)) return true;
        try {
            TabInfo tab = getCurrentTab();
            if (tab != null && isYouTubePageUrl(tab.url)) return true;
        } catch (Exception ignored) {}
        return false;
    }

    private void injectYouTubeSafeAdBlockV6() {
        if (webView == null || !adBlock) return;
        if (!isYouTubePlaybackUrl(getEffectiveCurrentUrl())) return;
        // v0.9.87: YouTube assistant khusus iklan.
        // Tidak autoplay saat halaman/video baru dibuka tanpa iklan.
        // Saat iklan terdeteksi: coba maju +10 detik secara periodik, klik Skip/Lewati jika muncul,
        // lalu auto-resume satu kali jika video utama kepause setelah iklan dilewati.
        String js = "javascript:"
                + "(function(){\n"
                + "try{\n"
                + "  var h=(location.hostname||'').toLowerCase();\n"
                + "  if(h.indexOf('youtube.com')<0 && h.indexOf('youtu.be')<0) return;\n"
                + "  var W=window;\n"
                + "  W.__yieldYTAssistantDisabled=false;\n"
                + "  W.__yieldYTNoForcePlay=true;\n"
                + "  if(W.__yieldYTAutoCycleV70){try{W.__yieldYTAutoCycleV70.phase='monitor';W.__yieldYTAutoCycleV70.lastAdSeen=0;W.__yieldYTAutoCycleV70.coolStart=0;W.__yieldYTAutoCycleV70.hadAd=false;}catch(e){}}\n"
                + "  W.__yieldYTPassiveResume=W.__yieldYTPassiveResume||{installed:false,lastUrl:'',lastSkipAt:0,resumeUntil:0,lastUserInputAt:0,lastResumeAt:0,lastAdSeekAt:0,lastAdPlayAt:0,lastAdSeenAt:0,hadAd:false};\n"
                + "  var S=W.__yieldYTPassiveResume;\n"
                + "  if(S.lastUrl!==location.href){S.lastUrl=location.href;S.lastSkipAt=0;S.resumeUntil=0;S.lastUserInputAt=0;S.lastResumeAt=0;S.lastAdSeekAt=0;S.lastAdPlayAt=0;S.lastAdSeenAt=0;S.hadAd=false;}\n"
                + "  function now(){return Date.now?Date.now():(new Date()).getTime();}\n"
                + "  function qsa(s,r){try{return Array.prototype.slice.call((r||document).querySelectorAll(s));}catch(e){return [];} }\n"
                + "  function visible(el){try{if(!el)return false;var r=el.getBoundingClientRect();var cs=getComputedStyle(el);return r.width>1&&r.height>1&&cs.display!=='none'&&cs.visibility!=='hidden'&&parseFloat(cs.opacity||'1')>0.01;}catch(e){return !!el;} }\n"
                + "  function txt(el){try{return ((el.innerText||el.textContent||el.getAttribute('aria-label')||el.getAttribute('title')||'')+'').replace(/\\s+/g,' ').trim().toLowerCase();}catch(e){return '';} }\n"
                + "  function player(){try{return document.querySelector('#movie_player,.html5-video-player,ytd-player,ytm-player,#player-container-id,#player-container,#player');}catch(e){return null;}}\n"
                + "  function video(){try{return document.querySelector('#movie_player video,.html5-video-player video,ytd-player video,ytm-player video,#player video,video');}catch(e){return null;}}\n"
                + "  function adShowing(){try{var p=player();var cls=p?String(p.className||''):'';var root=p||document;var skip=qsa('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytp-skip-ad-button__button,.ytm-ad-skip-button',root).some(visible);var ui=qsa('.ytp-ad-player-overlay,.ytp-ad-player-overlay-instream-info,.ytp-ad-preview-container,.ytp-ad-text,.ytp-ad-simple-ad-badge,.ytp-ad-badge,.video-ads.ytp-ad-module,.ytp-ad-action-interstitial',root).some(visible);var pt=txt(root);var word=/(bersponsor|sponsored|kunjungi pengiklan|visit advertiser|lewati iklan|skip ad|iklan\\s*\\u2022|ad\\s*1\\s*of|ad\\s*2\\s*of|1\\s*dari\\s*2|2\\s*dari\\s*2)/i.test(pt);return cls.indexOf('ad-showing')>-1||cls.indexOf('ad-interrupting')>-1||skip||ui||word;}catch(e){return false;}}\n"
                + "  function installStyle(){try{if(document.getElementById('yield-yt-passive-style'))return;var st=document.createElement('style');st.id='yield-yt-passive-style';st.textContent='ytd-display-ad-renderer,ytd-promoted-video-renderer,ytd-ad-slot-renderer,ytd-companion-slot-renderer,ytd-banner-promo-renderer,ytd-in-feed-ad-layout-renderer,ytd-promoted-sparkles-web-renderer,ytm-promoted-sparkles-web-renderer,ytm-promoted-video-renderer,ytm-ad-slot-renderer,ytm-companion-slot-renderer,ytm-in-feed-ad-layout-renderer,ytm-display-ad-renderer,#player-ads,.ytp-ad-overlay-container,.ytp-ad-text,.ytp-ad-image-overlay,.ytp-ad-progress-list{display:none!important;visibility:hidden!important;opacity:0!important;max-height:0!important;overflow:hidden!important;}';(document.head||document.documentElement).appendChild(st);}catch(e){}}\n"
                + "  function cleanCards(){try{installStyle();qsa('ytd-display-ad-renderer,ytd-promoted-video-renderer,ytd-ad-slot-renderer,ytd-companion-slot-renderer,ytd-banner-promo-renderer,ytd-in-feed-ad-layout-renderer,ytd-promoted-sparkles-web-renderer,ytm-promoted-sparkles-web-renderer,ytm-promoted-video-renderer,ytm-ad-slot-renderer,ytm-companion-slot-renderer,ytm-in-feed-ad-layout-renderer,ytm-display-ad-renderer,#player-ads').forEach(function(e){try{if(e.querySelector&&e.querySelector('video'))return;e.style.setProperty('display','none','important');e.style.setProperty('visibility','hidden','important');e.style.setProperty('height','0px','important');e.style.setProperty('max-height','0px','important');e.style.setProperty('overflow','hidden','important');}catch(x){}});}catch(e){}}\n"
                + "  function resumeAfterSkipOnce(){try{if(W.__yieldYTAssistantDisabled)return false;var t=now();if(!S.resumeUntil||t>S.resumeUntil)return false;if((S.lastResumeAt||0)>(S.lastSkipAt||0))return false;if((S.lastUserInputAt||0)>(S.lastSkipAt||0)+80)return false;if(adShowing())return false;var v=video();if(!v||v.ended||!v.paused)return false;S.lastResumeAt=t;var p=v.play&&v.play();if(p&&p.catch)p.catch(function(){});return true;}catch(e){return false;}}\n"
                + "  function scheduleResumeAfterSkip(){try{if(W.__yieldYTAssistantDisabled)return;S.lastSkipAt=now();S.resumeUntil=S.lastSkipAt+4500;S.lastResumeAt=0;[550,1200,2200,3400].forEach(function(d){setTimeout(resumeAfterSkipOnce,d);});}catch(e){}}\n"
                + "  function nodeInfo(el){try{var out='';var n=el;var p=player();for(var i=0;n&&i<5;i++,n=n.parentElement){if(p&&n===p&&n!==el)break;out+=' '+txt(n)+' '+String(n.className||'')+' '+String(n.id||'')+' '+String(n.getAttribute&&n.getAttribute('aria-label')||'')+' '+String(n.getAttribute&&n.getAttribute('title')||'')+' '+String(n.getAttribute&&n.getAttribute('data-title-no-tooltip')||'');}return out.toLowerCase();}catch(e){return '';}}\n"
                + "  function badSkipTarget(el){try{var s=nodeInfo(el);return /(kunjungi|visit|situs|site|advertiser|pengiklan|sponsor|bersponsor|cta|call-to-action|menu|more|overflow|setting|quality|caption|subtitle|mute|volume|fullscreen|share|like|subscribe|search|account|buka apl|open app)/i.test(s)&&!/(skip|lewati|ad-skip|skip-ad|ytp-ad-skip)/i.test(s);}catch(e){return true;}}\n"
                + "  function skipLike(el){try{return /(skip|lewati|ad-skip|skip-ad|ytp-ad-skip|ytp-skip-ad|ad_skip|skip_button|skipbutton)/i.test(nodeInfo(el));}catch(e){return false;}}\n"
                + "  function tapSkip(el){try{if(!el||!visible(el)||el.disabled||badSkipTarget(el))return false;try{el.scrollIntoView&&el.scrollIntoView({block:'nearest',inline:'nearest'});}catch(x){}try{['pointerdown','mousedown','mouseup','touchstart','touchend','click'].forEach(function(ev){try{el.dispatchEvent(new Event(ev,{bubbles:true,cancelable:true}));}catch(e){}});}catch(x){}try{el.click&&el.click();}catch(x){}scheduleResumeAfterSkip();return true;}catch(e){return false;}}\n"
                + "  function iconSkipFallback(root){try{if(!adShowing())return false;var p=player();if(!p)return false;var pr=p.getBoundingClientRect();var list=qsa('button,[role=button],.ytp-button,.ytp-ad-button-icon,.ytp-ad-skip-button-icon,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytp-skip-ad-button__button',root||p);for(var i=0;i<list.length;i++){var b=list[i];try{if(!visible(b)||b.disabled||badSkipTarget(b))continue;var info=nodeInfo(b);if(/(skip|lewati|ad-skip|skip-ad|ytp-ad-skip|ytp-skip-ad|ad_skip)/i.test(info))return tapSkip(b);var r=b.getBoundingClientRect();var right=r.left>pr.left+pr.width*0.54&&r.right<=pr.right+8;var mid=r.top>pr.top+pr.height*0.18&&r.bottom<pr.bottom-pr.height*0.08;var size=r.width>=22&&r.height>=22&&r.width<=150&&r.height<=100;var icon=!!(b.querySelector&&b.querySelector('svg,path,use,yt-icon'))||/(icon|button|ytp)/i.test(String(b.className||''));if(right&&mid&&size&&icon)return tapSkip(b);}catch(x){}}return false;}catch(e){return false;}}\n"
                + "  function clickSkipOnly(){try{if(W.__yieldYTAssistantDisabled)return false;var root=player()||document;var sels=['.ytp-ad-skip-button','.ytp-ad-skip-button-modern','.ytp-ad-skip-button-container','.ytp-ad-skip-button-slot','.ytp-skip-ad-button','.ytp-skip-ad-button__button','.ytm-ad-skip-button','.ytp-ad-skip-button-icon','.ytp-ad-button-icon','button[aria-label*=\"Skip\"]','button[aria-label*=\"skip\"]','button[aria-label*=\"Lewati\"]','button[aria-label*=\"lewati\"]','[class*=\"ad-skip\"]','[class*=\"skip-ad\"]','[id*=\"ad-skip\"]','[id*=\"skip-ad\"]'];var clicked=false;sels.forEach(function(sel){qsa(sel,root).forEach(function(b){try{if(clicked||!visible(b)||b.disabled)return;if(skipLike(b)||/skip|lewati/i.test(txt(b))){clicked=tapSkip(b);}}catch(x){}});});if(!clicked)clicked=iconSkipFallback(root);return clicked;}catch(e){return false;}}\n"
                + "  function forwardAd10sSafe(){try{if(W.__yieldYTAssistantDisabled)return false;if(!adShowing())return false;var v=video();if(!v||v.ended)return false;var t=now();S.lastAdSeenAt=t;S.hadAd=true;if(v.paused&&t-(S.lastAdPlayAt||0)>1200){S.lastAdPlayAt=t;try{var p0=v.play&&v.play();if(p0&&p0.catch)p0.catch(function(){});}catch(x){}}if(t-(S.lastAdSeekAt||0)<850)return false;var cur=Number(v.currentTime||0);var dur=Number(v.duration||0);if(!isFinite(cur))return false;S.lastAdSeekAt=t;try{var pl=player();if(pl&&pl.focus)pl.focus();var ev=new KeyboardEvent('keydown',{key:'l',code:'KeyL',keyCode:76,which:76,bubbles:true,cancelable:true});document.dispatchEvent(ev);}catch(x){}try{if(isFinite(dur)&&dur>0){v.currentTime=Math.min(Math.max(cur+10,cur),Math.max(0,dur-0.15));}else{v.currentTime=cur+10;}}catch(x){}return true;}catch(e){return false;}}\n"
                + "  function run(){try{if(W.__yieldYTAssistantDisabled)return;cleanCards();var clicked=clickSkipOnly();if(!clicked)forwardAd10sSafe();resumeAfterSkipOnce();}catch(e){}}\n"
                + "  W.__yieldYTPassiveRun=run;\n"
                + "  if(!S.installed){S.installed=true;['pointerdown','touchstart','mousedown','keydown'].forEach(function(ev){try{document.addEventListener(ev,function(e){try{if(!e||e.isTrusted)S.lastUserInputAt=now();}catch(x){}},true);}catch(e){}});try{var tm=null;var mo=new MutationObserver(function(){clearTimeout(tm);tm=setTimeout(run,120);});mo.observe(document.documentElement||document,{childList:true,subtree:true,attributes:true,attributeFilter:['class','style','aria-label','title']});}catch(e){} ['yt-navigate-finish','yt-page-data-updated','spfdone'].forEach(function(ev){try{document.addEventListener(ev,function(){setTimeout(run,120);setTimeout(run,700);},true);}catch(e){}});setInterval(run,900);}\n"
                + "  setTimeout(run,80);setTimeout(run,400);setTimeout(run,1200);setTimeout(run,3000);\n"
                + "}catch(e){}\n"
                + "})();\n";
        runPageScript(js);
    }


    private void stopYouTubeAutoAssistantNow() {
        if (webView == null) return;
        if (!isYouTubePlaybackUrl(getEffectiveCurrentUrl())) return;
        // Kill-switch khusus YouTube: saat AdBlock dimatikan, script lama yang sudah
        // terlanjur masuk ke halaman berhenti tanpa perlu reload.
        String js = "javascript:"
                + "(function(){try{"
                + "window.__yieldYTAssistantDisabled=true;"
                + "window.__yieldYTNoForcePlay=true;"
                + "if(window.__yieldYTPassiveResume){"
                + "window.__yieldYTPassiveResume.lastSkipAt=0;"
                + "window.__yieldYTPassiveResume.resumeUntil=0;"
                + "window.__yieldYTPassiveResume.lastResumeAt=0;"
                + "window.__yieldYTPassiveResume.lastAdSeekAt=0;"
                + "window.__yieldYTPassiveResume.lastAdPlayAt=0;"
                + "window.__yieldYTPassiveResume.lastAdSeenAt=0;"
                + "window.__yieldYTPassiveResume.hadAd=false;"
                + "}"
                + "window.__yieldYTPassiveRun=function(){};"
                + "var st=document.getElementById('yield-yt-passive-style');"
                + "if(st&&st.parentNode)st.parentNode.removeChild(st);"
                + "}catch(e){}})();";
        runPageScript(js);
    }

    private void injectReaderMode() {
        String js = "javascript:(function(){document.body.style.maxWidth='720px';document.body.style.margin='auto';document.body.style.lineHeight='1.7';document.body.style.fontSize='18px';document.body.style.background='#111318';document.body.style.color='#F5F7FA';})()";
        runPageScript(js);
    }

    private void hideKeyboardAndClearFocus(View focusView) {
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

    private void blurWebInputsAndHideKeyboard() {
        hideKeyboardAndClearFocus(webView != null ? webView : getWindow().getDecorView());
        try {
            if (webView != null) {
                webView.evaluateJavascript("(function(){try{if(document.activeElement)document.activeElement.blur();document.querySelectorAll('input,textarea,[contenteditable=true]').forEach(function(e){try{e.blur();}catch(x){}});}catch(e){}})();", null);
            }
        } catch (Exception ignored) {
        }
    }

    private void openHomeSearchUrl() {
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

    private void openAddressBarUrl() {
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

    private String buildSearchUrl(String query) {
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

    private void loadShortcuts() {
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

    private String normalizeStoredRows(String saved) {
        if (saved == null || saved.length() == 0) return "";
        return saved.replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    private void saveShortcuts() {
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

    private boolean isLikelyDesktopOnlyHost(String host) {
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

    private void scheduleHorizontalGestureGuardCheck(String url) {
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

    private void installSwipeNavigation(View root) {
        View.OnTouchListener listener = (v, event) -> handleSwipeTouch(event);
        try {
            if (root != null) root.setOnTouchListener(listener);
            if (homeScroll != null) homeScroll.setOnTouchListener(listener);
            if (webView != null) webView.setOnTouchListener(listener);
        } catch (Exception ignored) {
        }
    }

    private boolean handleSwipeTouch(MotionEvent event) {
        if (event == null) return false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                swipeStartX = event.getX();
                swipeStartY = event.getY();
                swipeStartTime = System.currentTimeMillis();
                return false;

            case MotionEvent.ACTION_UP:
                float dx = event.getX() - swipeStartX;
                float dy = event.getY() - swipeStartY;
                long duration = System.currentTimeMillis() - swipeStartTime;

                if (duration > 900) return false;
                if (Math.abs(dx) < dp(90)) return false;
                if (Math.abs(dy) > dp(120)) return false;

                // v0.9.71: Edge-only swipe navigation.
                // Sebelumnya swipe horizontal dari tengah layar bisa ikut terbaca sebagai Back.
                // Sekarang custom Back/Forward hanya aktif jika sentuhan dimulai dari pinggir layar.
                // Pada situs desktop/horizontal-scroll, area pinggir dibuat lebih sempit lagi.
                boolean horizontalProtected = shouldProtectWebHorizontalSwipeGesture();
                int screenWidth = getResources() != null && getResources().getDisplayMetrics() != null
                        ? getResources().getDisplayMetrics().widthPixels : 0;
                int edgeLimit = horizontalProtected ? dp(16) : dp(30);
                boolean fromLeftEdge = swipeStartX <= edgeLimit;
                boolean fromRightEdge = screenWidth > 0 && swipeStartX >= (screenWidth - edgeLimit);
                if (!fromLeftEdge && !fromRightEdge) return false;

                // Arah lama tetap dipertahankan: swipe kiri = Back, swipe kanan = Forward.
                // Namun harus dimulai dari edge yang searah agar scroll horizontal tengah tidak memicu navigasi.
                if (dx < 0) {
                    if (!fromRightEdge) return false;
                    navigateSwipeBack();
                } else {
                    if (!fromLeftEdge) return false;
                    navigateSwipeForward();
                }
                return false;
        }
        return false;
    }

    private boolean shouldProtectWebHorizontalSwipeGesture() {
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

    private void navigateSwipeBack() {
        try {
            boolean homeVisible = homeScroll != null
                    && homeScroll.getVisibility() == View.VISIBLE;
            boolean webVisible = webView != null
                    && webView.getVisibility() == View.VISIBLE;
            boolean canGoBack = webView != null && webView.canGoBack();
            TabNavigationPolicy.BackAction action = TabNavigationPolicy.backAction(
                    homeVisible, webVisible, canGoBack);
            if (action == TabNavigationPolicy.BackAction.RESTORE_PAGE) {
                restoreHiddenWebPage();
            } else if (action == TabNavigationPolicy.BackAction.WEB_BACK) {
                webView.goBack();
            } else {
                showHome();
            }
        } catch (Exception ignored) {
        }
    }

    private void navigateSwipeForward() {
        try {
            boolean homeVisible = homeScroll != null
                    && homeScroll.getVisibility() == View.VISIBLE;
            boolean webVisible = webView != null
                    && webView.getVisibility() == View.VISIBLE;
            boolean canGoForward = webView != null && webView.canGoForward();
            TabNavigationPolicy.ForwardAction action = TabNavigationPolicy.forwardAction(
                    homeVisible, webVisible, canGoForward);
            if (action == TabNavigationPolicy.ForwardAction.WEB_FORWARD) {
                webView.goForward();
            } else if (action == TabNavigationPolicy.ForwardAction.RESTORE_AND_FORWARD) {
                restoreHiddenWebPage();
                webView.goForward();
            } else if (action == TabNavigationPolicy.ForwardAction.RESTORE_PAGE) {
                restoreHiddenWebPage();
            }
        } catch (Exception ignored) {
        }
    }

    private void restoreHiddenWebPage() {
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

    private FrameLayout createNavigationLoadingOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(COLOR_BG);
        overlay.setAlpha(0f);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));

        ProgressBar spinner = new ProgressBar(this);
        try {
            spinner.setIndeterminateTintList(ColorStateList.valueOf(COLOR_ACCENT));
        } catch (Exception ignored) {
        }
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(42), dp(42));
        box.addView(spinner, sp);

        TextView label = new TextView(this);
        label.setText("Memuat halaman...");
        label.setTextColor(COLOR_SUBTEXT);
        label.setTextSize(14);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, dp(14), 0, 0);
        box.addView(label, lp);

        overlay.addView(box, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        return overlay;
    }

    private void startSmoothSearchTransition() {
        smoothSearchTransitionActive = true;
        try {
            if (navigationLoadingOverlay != null) {
                navigationLoadingOverlay.bringToFront();
                navigationLoadingOverlay.setVisibility(View.VISIBLE);
                navigationLoadingOverlay.setAlpha(1f);
            }
            if (homeScroll != null) homeScroll.setVisibility(View.GONE);
            if (webView != null) {
                webView.setAlpha(0f);
                webView.setVisibility(View.VISIBLE);
            }
        } catch (Exception ignored) {
        }
    }

    private void finishSmoothSearchTransition() {
        if (!smoothSearchTransitionActive) return;
        smoothSearchTransitionActive = false;
        try {
            if (webView != null) {
                webView.setVisibility(View.VISIBLE);
                webView.animate().alpha(1f).setDuration(180).start();
            }
            if (navigationLoadingOverlay != null) {
                navigationLoadingOverlay.animate()
                        .alpha(0f)
                        .setDuration(180)
                        .withEndAction(() -> {
                            try {
                                navigationLoadingOverlay.setVisibility(View.GONE);
                                navigationLoadingOverlay.setAlpha(0f);
                            } catch (Exception ignored) {
                            }
                        })
                        .start();
            }
        } catch (Exception ignored) {
            if (navigationLoadingOverlay != null) navigationLoadingOverlay.setVisibility(View.GONE);
            if (webView != null) webView.setAlpha(1f);
        }
    }

    private void cancelSmoothSearchTransition() {
        smoothSearchTransitionActive = false;
        try {
            if (navigationLoadingOverlay != null) {
                navigationLoadingOverlay.setVisibility(View.GONE);
                navigationLoadingOverlay.setAlpha(0f);
            }
            if (webView != null) webView.setAlpha(1f);
        } catch (Exception ignored) {
        }
    }

    private void navigateCurrentTabHome() {
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

    private void showHome() {
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

    private void updateTopActionStates() {
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

    private String getEffectiveCurrentUrl() {
        if (webView != null && webView.getVisibility() == View.VISIBLE) {
            String raw = extractOriginalUrl(webView.getUrl());
            if (raw != null && raw.length() > 0) return raw;
        }
        if (homeScroll != null && homeScroll.getVisibility() == View.VISIBLE) return null;
        return normalizeInputToUrl(addressBar != null ? addressBar.getText().toString().trim() : "");
    }

    private String normalizeInputToUrl(String text) {
        if (text == null || text.trim().length() == 0) return null;
        text = text.trim();
        if (text.startsWith("https://translate.google.com/translate") || text.startsWith("http://translate.google.com/translate")) return extractOriginalUrl(text);
        if (text.startsWith("http://") || text.startsWith("https://")) return text;
        if (text.contains(".") && !text.contains(" ")) return "https://" + text;
        return null;
    }

    private String extractOriginalUrl(String url) {
        if (url == null) return null;
        if (url.startsWith("https://translate.google.com/translate") || url.startsWith("http://translate.google.com/translate")) {
            int idx = url.indexOf("&u=");
            if (idx != -1) return url.substring(idx + 3).replace("%3A", ":").replace("%2F", "/").replace("%3F", "?").replace("%3D", "=").replace("%26", "&");
        }
        return url;
    }

    private Set<String> getBookmarks() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (BookmarkItemData item : bookmarkData) {
            if (item.url != null && item.url.length() > 0) set.add(item.url);
        }
        if (!set.isEmpty()) return new HashSet<>(set);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        Set<String> saved = prefs.getStringSet(KEY_BOOKMARKS, new HashSet<>());
        return new HashSet<>(saved);
    }
    private BookmarkItemData findBookmarkByUrl(String url) {
        if (url == null) return null;
        for (BookmarkItemData item : bookmarkData) {
            if (url.equals(item.url)) return item;
        }
        return null;
    }

    private List<String> getBookmarkFolders() {
        LinkedHashSet<String> folders = new LinkedHashSet<>();
        folders.add("Bookmark seluler");
        folders.add("Daftar bacaan");
        try {
            Set<String> saved = getSharedPreferences(PREFS, MODE_PRIVATE).getStringSet(KEY_BOOKMARK_FOLDERS, new LinkedHashSet<>());
            if (saved != null) folders.addAll(saved);
        } catch (Exception ignored) {}
        for (BookmarkItemData item : bookmarkData) {
            if (item.folder != null && item.folder.trim().length() > 0) folders.add(item.folder.trim());
        }
        return new ArrayList<>(folders);
    }

    private int countBookmarksInFolder(String folder) {
        int count = 0;
        for (BookmarkItemData item : bookmarkData) {
            if (folder.equals(item.folder)) count++;
        }
        return count;
    }

    private void loadBookmarkData() {
        bookmarkData.clear();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String raw = prefs.getString(KEY_BOOKMARK_DATA, "");
        if (raw != null && raw.trim().length() > 0) {
            for (String line : raw.split("\n")) {
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    try {
                        bookmarkData.add(new BookmarkItemData(decode(parts[0]), decode(parts[1]), decode(parts[2]), Long.parseLong(parts[3])));
                    } catch (Exception ignored) {}
                }
            }
        }
        if (bookmarkData.isEmpty()) {
            Set<String> legacy = prefs.getStringSet(KEY_BOOKMARKS, new HashSet<>());
            for (String url : legacy) {
                bookmarkData.add(new BookmarkItemData(guessLabelFromUrl(url), url, "Bookmark seluler", System.currentTimeMillis()));
            }
            saveBookmarkData();
        }
    }

    private void saveBookmarkData() {
        ArrayList<String> saved = new ArrayList<>();
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        LinkedHashSet<String> folders = new LinkedHashSet<>(getBookmarkFolders());
        for (BookmarkItemData item : bookmarkData) {
            if (item.url == null || item.url.trim().length() == 0) continue;
            saved.add(encode(item.title) + "|" + encode(item.url) + "|" + encode(item.folder) + "|" + item.time);
            urls.add(item.url);
            if (item.folder != null && item.folder.trim().length() > 0) folders.add(item.folder.trim());
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_BOOKMARK_DATA, TextUtils.join("\n", saved))
                .putStringSet(KEY_BOOKMARKS, new HashSet<>(urls))
                .putStringSet(KEY_BOOKMARK_FOLDERS, new LinkedHashSet<>(folders))
                .apply();
    }

    private void loadSettings() {
        SettingsStore.load(this, getSharedPreferences(PREFS, MODE_PRIVATE));
    }

    private void saveSettings() {
        SettingsStore.save(this, getSharedPreferences(PREFS, MODE_PRIVATE));
    }

    private ImageButton smallTopIcon(int iconRes, String desc, View.OnClickListener listener) {
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
    private View space(int height) {
        return YieldUi.space(this, height);
    }

    private GradientDrawable roundRect(int fillColor, int radius, int strokeWidth, int strokeColor) {
        return YieldUi.roundRect(fillColor, radius, strokeWidth, strokeColor);
    }

    private int dp(int value) {
        return YieldUi.dp(this, value);
    }
}
