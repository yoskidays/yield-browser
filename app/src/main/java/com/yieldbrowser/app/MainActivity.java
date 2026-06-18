
package com.yieldbrowser.app;

import android.annotation.SuppressLint;
import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.ContentValues;
import android.content.pm.PackageInfo;
import android.content.pm.ActivityInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Rational;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
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
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

public class MainActivity extends Activity {

    // ===== Theme / constants =====
    private static final int COLOR_BG = Color.parseColor("#15171C");
    private static final int COLOR_SURFACE_2 = Color.parseColor("#2A2D33");
    private static final int COLOR_BORDER = Color.parseColor("#3A3D45");
    private static final int COLOR_TEXT = Color.parseColor("#F5F7FA");
    private static final int COLOR_SUBTEXT = Color.parseColor("#B7BDC8");
    private static final int COLOR_ACCENT = Color.parseColor("#F39A22");
    private static final int COLOR_ON = Color.parseColor("#22C55E");
    private static final String PREFS = "yield_browser_prefs";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String KEY_BOOKMARK_DATA = "bookmark_data";
    private static final String KEY_BOOKMARK_FOLDERS = "bookmark_folders";
    private static final String KEY_DOWNLOAD_HISTORY = "download_history";
    private static final String KEY_BROWSER_HISTORY = "browser_history";
    private static final String KEY_BROWSER_HISTORY_BACKUP = "browser_history_backup";
    private static final String KEY_BROWSER_HISTORY_V2 = "browser_history_v2";
    private static final String KEY_BROWSER_HISTORY_V3 = "browser_history_v3";
    private static final String HISTORY_V3_FILE = "yield_browser_history_v3.txt";
    private static final String HISTORY_V3_FOLDER = "Yield Browser/History";
    private static final String HISTORY_V3_PUBLIC_FILE = "history.txt";
    private static final String PREFS_HISTORY_V2 = "yield_browser_history_store";
    private static final String KEY_NIGHT_EXCEPTIONS = "night_mode_exceptions";
    // v0.9.82: persist tab session agar tab tetap terbuka setelah aplikasi ditutup/dibuka lagi.
    private static final String KEY_TABS_SESSION_V1 = "tabs_session_v1";
    private static final String KEY_TABS_CURRENT_INDEX_V1 = "tabs_current_index_v1";
    private static final String CHANNEL_DOWNLOADS = "yield_downloads";
    private static final String ACTION_OPEN_DOWNLOADS = "com.yieldbrowser.app.OPEN_DOWNLOADS";
    private static final int DOWNLOAD_CONNECTIONS_PREMIUM = 2;
    private static final int DOWNLOAD_CONNECTIONS_DYNAMIC_MAX = 4;
    private static final int DOWNLOAD_RETRY_MAX = 3;
    private static final int DESKTOP_VIEWPORT_WIDTH = 1280;
    private static final int DOWNLOAD_BUFFER_SIZE = 64 * 1024;
    private static final int DOWNLOAD_CONNECT_TIMEOUT = 15000;
    private static final int DOWNLOAD_READ_TIMEOUT = 30000;
    private static final int REQ_CAMERA_QR = 2401;
    private static final int REQ_PICK_DOWNLOAD_FOLDER = 2402;

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
    private LinearLayout videoControlsBar;
    private TextView videoSpeedLabel;
    private TextView videoQualityLabel;
    private TextView videoModeToggleButton;
    private ImageView videoPlayPauseIcon;
    private View videoPlayPauseButton;
    private String selectedVideoQuality = "Auto";
    private boolean videoControlsManualHidden = false;
    private ViewGroup videoControlsOriginalParent;
    private ViewGroup.LayoutParams videoControlsOriginalLayoutParams;
    private int videoControlsOriginalIndex = -1;
    private boolean videoControlsInFullscreen = false;
    private View fullscreenVideoView;
    private WebChromeClient.CustomViewCallback fullscreenVideoCallback;
    private int originalSystemUiVisibility = 0;
    private int originalRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private boolean videoLandscapeModeActive = false;
    private int orientationBeforeVideoLandscape = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private boolean fullscreenStartedFromVideoLandscape = false;
    private TextView tabsCountText;
    private float swipeStartX = 0f;
    private float swipeStartY = 0f;
    private long swipeStartTime = 0L;
    // v0.9.69: situs desktop/horizontal-scroll seperti h-metrics.com tidak boleh
    // dianggap gesture Back saat user menggeser halaman ke samping.
    private boolean webHorizontalGestureGuard = false;
    private String webHorizontalGestureGuardHost = "";

    private LinearLayout activeDownloadListPanel;
    private Dialog activeDownloadDialog;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean pendingOpenDownloads = false;
    private String activeDownloadCategory = "Semua";
    private String activeDownloadSearchQuery = "";
    private String activeDownloadSort = "Tanggal";
    private boolean downloadSelectMode = false;
    private final Set<Integer> selectedDownloadIds = new HashSet<>();

    private int tabCount = 1;
    private int nextDownloadId = 1000;
    private boolean translateEnabled = false;
    private boolean hideGoogleTranslateBar = true;
    private String lastTranslateOriginalUrl = "";
    private boolean compatibleTranslateActive = false;
    private boolean translateManuallyDisabled = true;
    private int translateSessionToken = 0;
    private long lastCompatibleTranslateStartedAt = 0L;
    private String translateTargetLang = "id";
    private String translateTargetLabel = "Indonesia";

    // ===== Browser settings state =====
    private boolean speedMode = false;
    private boolean safeMode = true;
    private boolean nightMode = true;
    private String nightModeOption = "ON";
    private final Set<String> nightModeExceptions = new HashSet<>();
    private int nightModeApplyToken = 0;
    private String lastNightModeSyncUrl = "";
    private boolean readerMode = false;
    private boolean adBlock = true;
    private boolean adBlockPopupBlocker = true;
    private boolean adBlockRedirectBlocker = true;
    private boolean adBlockScriptIframeBlocker = true;
    private boolean adBlockClickHijackBlocker = true;
    private boolean adBlockRedirectToTempTab = true;
    private boolean adBlockAutoCloseAdTabs = true;
    private boolean dataSaver = false;
    private boolean desktopMode = false;
    private String mobileUserAgent = "";
    private int browserModeToken = 0;
    private int textZoom = 100;

    private boolean shortcutDownload = true;
    private boolean shortcutReloadWebsite = true;
    private boolean shortcutBookmark = false;
    private boolean shortcutPrivate = true;
    private boolean shortcutAdBlock = true;
    private boolean shortcutReader = false;
    private boolean shortcutNightMode = false;
    private boolean shortcutQrScan = false;
    private boolean shortcutHistory = true;
    private boolean shortcutFindPage = false;
    private boolean shortcutShare = false;
    private boolean shortcutFullscreen = false;
    private boolean videoControlsEnabled = true;
    private boolean videoBufferBooster = true;
    private boolean hlsSegmentPrefetch = true;
    private boolean videoFloatingPlayer = true;
    private boolean videoBackgroundPlay = true;
    private boolean shortcutVideoControls = false;
    private float videoSpeed = 1.0f;
    private String downloadSubfolder = "Download";
    private String selectedDownloadTreeUri = "";
    private boolean downloadDynamic4Connections = true;
    private boolean downloadAutoRetry = true;
    private boolean downloadHlsEnabled = true;
    private int downloadSpeedLimitKBps = 0;
    private boolean downloadQueueEnabled = true;
    private int downloadMaxActive = 2;
    private boolean downloadQueuePaused = false;
    private boolean topIconReload = true;
    private boolean topIconBookmark = true;
    private boolean topIconTranslate = true;


    // ===== App data collections =====
    private final ArrayList<DownloadItem> downloadItems = new ArrayList<>();
    private final ArrayList<ShortcutItemData> shortcutsData = new ArrayList<>();
    private final ArrayList<HistoryItemData> historyData = new ArrayList<>();
    private final ArrayList<BookmarkItemData> bookmarkData = new ArrayList<>();
    private final ArrayList<TabInfo> tabs = new ArrayList<>();
    private int currentTabIndex = 0;
    private LinearLayout shortcutContainer;
    private String searchEngine = "Google";
    private String lastSafeHttpUrl = "";
    // v0.9.43: cache URL halaman aktif untuk thread shouldInterceptRequest.
    // shouldInterceptRequest berjalan di thread Chromium, jadi dilarang memanggil WebView.getUrl() di sana.
    private volatile String currentPageUrlForRequest = "";
    private boolean pendingHideKeyboardAfterNavigation = false;
    // v0.9.75: saat membuat tab baru kosong, showHome tidak boleh menyimpan URL WebView lama ke tab baru.
    private boolean skipNextShowHomeTabSave = false;
    private boolean historyClearLock = false;

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
    // v0.9.46/v0.9.64: situs yang sensitif/anti-adblock/anti-redirect butuh mode WebView polos.
    // Mode ini tidak mengirim header buatan, tidak melakukan inject, dan tidak intercept resource.
    // v0.9.64 menambah domain seperti instant-monitor.com yang blank saat AdBlock ON,
    // tanpa mengubah adblock situs lain yang sudah stabil.
    private static final String[] STRICT_COMPAT_HOSTS = new String[]{
            "lordborg.com",
            "instant-monitor.com"
    };
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


    // ===== Data models =====
    private static class DownloadItem {
        int id;
        String url;
        String fileName;
        String path;
        String status;
        int progress;
        long totalBytes;
        long downloadedBytes;

        int connectionCount = 0;
        boolean pauseRequested = false;
        String engineInfo = "Menunggu koneksi";

        String userAgent = "";
        String referer = "";
        String failReason = "";
        String categoryHint = "";
        String publicUri = "";
        int retryCount = 0;
        boolean hlsDownload = false;
        long part1Start = 0;
        long part1End = 0;
        long part1Done = 0;
        long part2Start = 0;
        long part2End = 0;
        long part2Done = 0;
        long part3Start = 0;
        long part3End = 0;
        long part3Done = 0;
        long part4Start = 0;
        long part4End = 0;
        long part4Done = 0;
        double speedBytesPerSecond = 0;
        long lastSpeedTimeMs = 0;
        long lastSpeedBytes = 0;
        long lastActionClickMs = 0;
        DownloadItem(int id, String url, String fileName, String path, String status, int progress) {
            this.id = id;
            this.url = url;
            this.fileName = fileName;
            this.path = path;
            this.status = status;
            this.progress = progress;
        }
    }

    private static class ShortcutItemData {
        String label;
        String url;

        ShortcutItemData(String label, String url) {
            this.label = label;
            this.url = url;
        }
    }

    private static class HistoryItemData {
        String title;
        String url;
        long time;

        HistoryItemData(String title, String url, long time) {
            this.title = title;
            this.url = url;
            this.time = time;
        }
    }


    private static class BookmarkItemData {
        String title;
        String url;
        String folder;
        long time;

        BookmarkItemData(String title, String url, String folder, long time) {
            this.title = title;
            this.url = url;
            this.folder = folder;
            this.time = time;
        }
    }

    private static class TabInfo {
        String title;
        String url;
        boolean privateTab;
        boolean adTab;
        // v0.9.80: setiap tab punya WebView sendiri agar tab compatibility
        // tidak menimpa URL/state tab lain dan perpindahan tab tidak reload ulang.
        transient WebView webView;
        // v0.9.78: simpan state ringan WebView per tab sebagai fallback restore.
        Bundle webState;
        // v0.9.84: URL aman terakhir per tab. Ini mencegah direct-link/iklan
        // yang muncul saat app dibuka ulang menimpa semua tab yang sebelumnya stabil.
        String lastSafeUrl = "";
        String lastSafeTitle = "";

        TabInfo(String title, String url, boolean privateTab) {
            this(title, url, privateTab, false);
        }

        TabInfo(String title, String url, boolean privateTab, boolean adTab) {
            this.title = title;
            this.url = url;
            this.privateTab = privateTab;
            this.adTab = adTab;
            if (url != null && url.length() > 0 && !adTab) {
                this.lastSafeUrl = url;
                this.lastSafeTitle = title != null ? title : url;
            }
        }
    }

    private class TranslateBridge {
        @JavascriptInterface
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

        @JavascriptInterface
        public void onCollected(int count) {
            final int token = translateSessionToken;
            runOnUiThread(() -> {
                if (isCompatibleTranslateAllowed(token)) {
                    Toast.makeText(MainActivity.this, "Translate kompatibel berjalan: " + count + " teks", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private class VideoBridge {
        @JavascriptInterface
        public void onVideoPlaying() {
            runOnUiThread(() -> {
                updateVideoPlayPauseButtonState(true);
                showVideoControlsIfAllowed();
            });
        }

        @JavascriptInterface
        public void onVideoTapped() {
            runOnUiThread(() -> {
                videoControlsManualHidden = false;
                showVideoControlsIfAllowed();
            });
        }

        @JavascriptInterface
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

        @JavascriptInterface
        public void tapAtRatio(double xRatio, double yRatio) {
            runOnUiThread(() -> nativeTapWebViewAtRatio(xRatio, yRatio));
        }
    }

    private class AdBlockBridge {
        @JavascriptInterface
        public void onAdRedirect(String url) {
            runOnUiThread(() -> captureAdRedirectToTempTab(url, "Iklan/direct link"));
        }
    }



    // ===== Activity lifecycle =====
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();
        loadDownloadHistory();
        loadShortcuts();
        loadBrowserHistory();
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

        webView = createBrowserWebView(View.GONE);
        try { getCurrentTab().webView = webView; } catch (Exception ignored) {}
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
        mainHandler.postDelayed(this::pumpDownloadQueue, 650);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenDownloadsIntent(intent);
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
            Toast.makeText(this, "Folder HP dipilih untuk hasil download", Toast.LENGTH_SHORT).show();
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
        try {
            saveCurrentTabState();
            saveTabsSession();
            recordCurrentPageToHistory();
            recordWebViewBackForwardHistory();
            saveBrowserHistory();
        } catch (Exception ignored) {}
        if (videoBackgroundPlay && webView != null) {
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
            saveTabsSession();
            recordCurrentPageToHistory();
            recordWebViewBackForwardHistory();
            saveBrowserHistory();
        } catch (Exception ignored) {}
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBrowserHistory();
        handleOpenDownloadsIntent(getIntent());
    }

    @Override
    protected void onDestroy() {
        try {
            saveCurrentTabState();
            saveTabsSession();
            recordCurrentPageToHistory();
            recordWebViewBackForwardHistory();
            saveBrowserHistory();
        } catch (Exception ignored) {}
        super.onDestroy();
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
        titleBrowser.setText(" Browser");
        titleBrowser.setTextColor(Color.parseColor("#DDA13A"));
        titleBrowser.setTextSize(31);
        titleBrowser.setLetterSpacing(-0.01f);
        titleBrowser.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleRow.addView(titleBrowser);

        content.addView(titleRow);

        TextView subtitle = new TextView(this);
        subtitle.setText("Cepat, ringan, dan siap dipakai.");
        subtitle.setTextColor(COLOR_SUBTEXT);
        subtitle.setTextSize(17);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-2, -2);
        subParams.setMargins(0, dp(8), 0, dp(24));
        content.addView(subtitle, subParams);

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
            Toast.makeText(this, "Ketuk X untuk hapus pintasan", Toast.LENGTH_SHORT).show();
            return true;
        });
        delete.setOnClickListener(v -> {
            shortcutsData.remove(shortcut);
            saveShortcuts();
            renderShortcuts();
            Toast.makeText(this, "Pintasan dihapus", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, "URL tidak valid", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Pintasan ditambahkan", Toast.LENGTH_SHORT).show();
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
        new Thread(() -> {
            try {
                String faviconUrl = "https://www.google.com/s2/favicons?sz=96&domain_url=" + URLEncoder.encode(url, "UTF-8");
                HttpURLConnection conn = (HttpURLConnection) new URL(faviconUrl).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                InputStream input = conn.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                conn.disconnect();
                if (bitmap != null) {
                    runOnUiThread(() -> {
                        target.setImageBitmap(bitmap);
                        target.setVisibility(View.VISIBLE);
                        fallback.setVisibility(View.GONE);
                    });
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private View createBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(4), dp(8), dp(6));
        nav.setBackgroundColor(Color.parseColor("#090A0D"));

        nav.addView(bottomNavButton(R.drawable.ic_home, "Home", v -> showHome()));
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
        if (candidate == null) return null;
        try {
            for (TabInfo tab : tabs) {
                if (tab != null && tab.webView == candidate) return tab;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void attachWebViewToContentFrame(WebView candidate) {
        if (candidate == null || contentFrame == null) return;
        try {
            if (candidate.getParent() == null) {
                int insertIndex = 0;
                try {
                    insertIndex = homeScroll != null && homeScroll.getParent() == contentFrame ? 1 : contentFrame.getChildCount();
                    if (navigationLoadingOverlay != null && navigationLoadingOverlay.getParent() == contentFrame) {
                        int overlayIndex = contentFrame.indexOfChild(navigationLoadingOverlay);
                        if (overlayIndex >= 0) insertIndex = Math.max(0, overlayIndex);
                    }
                } catch (Exception ignored) {
                    insertIndex = contentFrame.getChildCount();
                }
                contentFrame.addView(candidate, insertIndex, new FrameLayout.LayoutParams(-1, -1));
            }
            if (navigationLoadingOverlay != null) navigationLoadingOverlay.bringToFront();
        } catch (Exception ignored) {
        }
    }

    private WebView ensureTabWebView(TabInfo tab, int visibility) {
        if (tab == null) return webView;
        try {
            if (tab.webView == null) {
                WebView created = createBrowserWebView(visibility);
                tab.webView = created;
                attachWebViewToContentFrame(created);
            } else {
                webView = tab.webView;
                attachWebViewToContentFrame(webView);
                try { applyBrowserSettings(); } catch (Exception ignored) {}
                try { CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true); } catch (Exception ignored) {}
                webView.setVisibility(visibility);
            }
            return tab.webView;
        } catch (Exception e) {
            return webView;
        }
    }

    private void hideInactiveTabWebViews(WebView active) {
        try {
            for (TabInfo tab : tabs) {
                if (tab != null && tab.webView != null && tab.webView != active) {
                    tab.webView.setVisibility(View.GONE);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void activateTabWebView(TabInfo tab, boolean showWebPage) {
        try {
            WebView target = ensureTabWebView(tab, showWebPage ? View.VISIBLE : View.GONE);
            webView = target;
            hideInactiveTabWebViews(target);
            if (homeScroll != null) homeScroll.setVisibility(showWebPage ? View.GONE : View.VISIBLE);
            if (target != null) target.setVisibility(showWebPage ? View.VISIBLE : View.GONE);
            if (navigationLoadingOverlay != null) navigationLoadingOverlay.bringToFront();
        } catch (Exception ignored) {
        }
    }

    private void destroyTabWebView(TabInfo tab) {
        if (tab == null || tab.webView == null) return;
        WebView doomed = tab.webView;
        tab.webView = null;
        try { doomed.stopLoading(); } catch (Exception ignored) {}
        try { if (contentFrame != null) contentFrame.removeView(doomed); } catch (Exception ignored) {}
        try { doomed.destroy(); } catch (Exception ignored) {}
        if (webView == doomed) webView = null;
    }

    private boolean hasLivePage(WebView candidate) {
        try {
            if (candidate == null) return false;
            String live = extractOriginalUrl(candidate.getUrl());
            if (live == null || live.length() == 0) return false;
            String lower = live.toLowerCase(Locale.US);
            return !lower.equals("about:blank") && !lower.startsWith("data:");
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureDefaultTab() {
        if (tabs.isEmpty()) {
            tabs.add(new TabInfo("Tab utama", "", false));
        }
        currentTabIndex = Math.max(0, Math.min(currentTabIndex, tabs.size() - 1));
        tabCount = tabs.size();
    }

    private TabInfo getCurrentTab() {
        ensureDefaultTab();
        return tabs.get(currentTabIndex);
    }

    private boolean isCurrentPrivateTab() {
        return getCurrentTab().privateTab;
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
        if (!isRestorableTabSessionUrl(clean)) return false;

        String referenceUrl = "";
        if (tab != null && tab.lastSafeUrl != null && tab.lastSafeUrl.length() > 0) {
            referenceUrl = tab.lastSafeUrl;
        } else if (lastSafeHttpUrl != null && lastSafeHttpUrl.length() > 0) {
            referenceUrl = lastSafeHttpUrl;
        }

        if (referenceUrl != null && referenceUrl.length() > 0) {
            String targetHost = normalizeHostForAdBlock(clean);
            String referenceHost = normalizeHostForAdBlock(referenceUrl);
            boolean sameSite = targetHost.length() > 0 && referenceHost.length() > 0
                    && sameOrSubDomain(targetHost, referenceHost);
            boolean compatibilityAllowed = isStrictSiteCompatibilityUrl(clean) || isSiteCompatibilityModeActiveForUrl(clean);
            if (!sameSite && !compatibilityAllowed && isSuspiciousPopupNavigation(clean, referenceUrl)) {
                return false;
            }
        }

        return true;
    }

    private void commitTabUrlIfSafe(TabInfo tab, String candidateUrl, String candidateTitle) {
        if (tab == null) return;
        String clean = cleanTabSessionUrl(candidateUrl);
        if (tab.adTab) {
            tab.url = clean;
            if (candidateTitle != null && candidateTitle.trim().length() > 0) tab.title = candidateTitle;
            return;
        }

        if (clean.length() > 0 && canCommitUrlToTab(tab, clean)) {
            tab.url = clean;
            tab.lastSafeUrl = clean;
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

    private void restoreTabsSession() {
        try {
            SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
            String raw = p.getString(KEY_TABS_SESSION_V1, "");
            if (raw == null || raw.trim().length() == 0) return;

            ArrayList<TabInfo> restored = new ArrayList<>();
            String[] rows = raw.split("\n");
            for (String row : rows) {
                if (row == null || row.trim().length() == 0) continue;
                String[] parts = row.split("\t", -1);
                if (parts.length < 4) continue;
                String title = decode(parts[0]);
                String url = decode(parts[1]);
                boolean privateTab = "1".equals(parts[2]);
                boolean adTab = "1".equals(parts[3]);
                if (adTab) continue; // tab iklan sementara tidak dipulihkan setelah restart.
                if (title == null || title.trim().length() == 0) title = privateTab ? "Tab privat" : "Tab baru";
                if (url == null) url = "";
                url = cleanTabSessionUrl(url);
                if (url.length() > 0 && !isRestorableTabSessionUrl(url)) continue;
                TabInfo restoredTab = new TabInfo(title, url, privateTab, false);
                if (url.length() > 0) {
                    restoredTab.lastSafeUrl = url;
                    restoredTab.lastSafeTitle = title;
                }
                restored.add(restoredTab);
            }
            if (!restored.isEmpty()) {
                tabs.clear();
                tabs.addAll(restored);
                currentTabIndex = Math.max(0, Math.min(p.getInt(KEY_TABS_CURRENT_INDEX_V1, 0), tabs.size() - 1));
                tabCount = tabs.size();
            }
        } catch (Exception ignored) {
        }
    }

    private void saveTabsSession() {
        try {
            if (tabs.isEmpty()) ensureDefaultTab();
            StringBuilder sb = new StringBuilder();
            int savedIndex = 0;
            int savedCount = 0;
            for (int i = 0; i < tabs.size(); i++) {
                TabInfo tab = tabs.get(i);
                if (tab == null || tab.adTab) continue;
                String url = getSafeUrlForSession(tab);
                if (url == null) continue;
                if (i == currentTabIndex) savedIndex = savedCount;
                String title = tab.title == null ? "" : tab.title;
                if (url.length() > 0 && tab.lastSafeTitle != null && tab.lastSafeTitle.length() > 0
                        && !canCommitUrlToTab(tab, tab.url)) {
                    title = tab.lastSafeTitle;
                }
                if (sb.length() > 0) sb.append('\n');
                sb.append(encode(title)).append('\t')
                        .append(encode(url)).append('\t')
                        .append(tab.privateTab ? "1" : "0").append('\t')
                        .append("0");
                savedCount++;
            }
            if (savedCount <= 0) {
                sb.append(encode("Tab utama")).append('\t').append(encode("")).append('\t').append("0	0");
                savedIndex = 0;
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_TABS_SESSION_V1, sb.toString())
                    .putInt(KEY_TABS_CURRENT_INDEX_V1, Math.max(0, Math.min(savedIndex, Math.max(0, savedCount - 1))))
                    .apply();
        } catch (Exception ignored) {
        }
    }

    private void restoreActiveTabAfterLaunch() {
        try {
            ensureDefaultTab();
            TabInfo tab = getCurrentTab();
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
            if (webView != null && webView.getVisibility() == View.VISIBLE && webView.getUrl() != null) {
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
                String maybeUrl = normalizeInputToUrl(addressBar.getText().toString().trim());
                if (maybeUrl != null) commitTabUrlIfSafe(tab, maybeUrl, tab.title);
            }
        } catch (Exception ignored) {
        }
    }

    private void updateTabsCountUi() {
        tabCount = Math.max(1, tabs.size());
        if (tabsCountText != null) {
            tabsCountText.setText(String.valueOf(tabCount));
        }
    }

    private void newNormalTab() {
        saveCurrentTabState();
        tabs.add(new TabInfo("Tab baru", "", false));
        currentTabIndex = tabs.size() - 1;
        activateTabWebView(getCurrentTab(), false);
        updateTabsCountUi();
        addressBar.setText("");
        if (homeSearchInput != null) homeSearchInput.setText("");
        skipNextShowHomeTabSave = true;
        showHome();
        saveTabsSession();
        Toast.makeText(this, "Tab baru dibuat", Toast.LENGTH_SHORT).show();
    }

    private void newPrivateTab() {
        saveCurrentTabState();
        tabs.add(new TabInfo("Tab privat", "", true));
        currentTabIndex = tabs.size() - 1;
        activateTabWebView(getCurrentTab(), false);
        updateTabsCountUi();
        addressBar.setText("");
        if (homeSearchInput != null) homeSearchInput.setText("");
        try {
            if (webView != null) {
                webView.clearHistory();
                webView.clearCache(false);
            }
        } catch (Exception ignored) {
        }
        skipNextShowHomeTabSave = true;
        showHome();
        saveTabsSession();
        Toast.makeText(this, "Tab privat aktif", Toast.LENGTH_SHORT).show();
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        saveCurrentTabState();
        currentTabIndex = index;
        TabInfo tab = getCurrentTab();
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
                scheduleNightModeSyncForPage(tab.url);
                scheduleHorizontalGestureGuardCheck(tab.url);
                if (isStrictSiteCompatibilityUrl(tab.url) || isSiteCompatibilityModeActiveForUrl(tab.url)) {
                    applyPlainCompatibilitySettingsForUrl(tab.url);
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
        Toast.makeText(this, tab.privateTab ? "Tab privat" : "Tab aktif", Toast.LENGTH_SHORT).show();
    }

    private void closeTab(int index) {
        if (tabs.isEmpty() || index < 0 || index >= tabs.size()) return;

        boolean closingCurrent = index == currentTabIndex;
        TabInfo removed = tabs.remove(index);
        destroyTabWebView(removed);

        if (tabs.isEmpty()) {
            tabs.add(new TabInfo("Tab utama", "", false));
            currentTabIndex = 0;
            addressBar.setText("");
            if (homeSearchInput != null) homeSearchInput.setText("");
            activateTabWebView(getCurrentTab(), false);
            skipNextShowHomeTabSave = true;
            showHome();
        } else {
            if (currentTabIndex > index) currentTabIndex--;
            if (currentTabIndex >= tabs.size()) currentTabIndex = tabs.size() - 1;
            if (closingCurrent) {
                switchToTab(currentTabIndex);
            } else {
                TabInfo active = getCurrentTab();
                activateTabWebView(active, active.url != null && active.url.length() > 0 && homeScroll != null && homeScroll.getVisibility() != View.VISIBLE);
            }
        }

        if (removed.privateTab) {
            try { if (webView != null && isCurrentPrivateTab()) webView.clearCache(false); } catch (Exception ignored) {}
        }

        updateTabsCountUi();
        saveTabsSession();
        Toast.makeText(this, "Tab ditutup", Toast.LENGTH_SHORT).show();
    }

    private void showTabsPanel() {
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

        TextView title = new TextView(this);
        title.setText("Tab");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));

        TextView privateButton = new TextView(this);
        privateButton.setText("Privat");
        privateButton.setTextColor(Color.WHITE);
        privateButton.setTextSize(14);
        privateButton.setTypeface(Typeface.DEFAULT_BOLD);
        privateButton.setGravity(Gravity.CENTER);
        privateButton.setBackground(roundRect(Color.parseColor("#20232A"), dp(18), dp(1), COLOR_BORDER));
        privateButton.setOnClickListener(v -> {
            dialog.dismiss();
            newPrivateTab();
        });
        LinearLayout.LayoutParams privateParams = new LinearLayout.LayoutParams(dp(78), dp(42));
        privateParams.setMargins(0, 0, dp(8), 0);
        header.addView(privateButton, privateParams);

        TextView plus = new TextView(this);
        plus.setText("+");
        plus.setTextColor(Color.parseColor("#111111"));
        plus.setTextSize(28);
        plus.setTypeface(Typeface.DEFAULT_BOLD);
        plus.setGravity(Gravity.CENTER);
        plus.setBackground(roundRect(COLOR_ACCENT, dp(18), 0, Color.TRANSPARENT));
        plus.setOnClickListener(v -> {
            dialog.dismiss();
            newNormalTab();
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

        TextView hint = new TextView(this);
        hint.setText("Ketuk tab untuk pindah. Tekan × untuk menutup tab.");
        hint.setTextColor(COLOR_SUBTEXT);
        hint.setTextSize(13);
        hint.setPadding(0, 0, 0, dp(12));
        root.addView(hint);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);

        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            list.addView(tabRow(tabs.get(i), index, dialog));
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

    private View tabRow(TabInfo tab, int index, Dialog dialog) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(10), dp(12));
        row.setBackground(roundRect(index == currentTabIndex ? Color.parseColor("#20232A") : Color.parseColor("#15171D"), dp(18), dp(1), index == currentTabIndex ? COLOR_ACCENT : COLOR_BORDER));

        TextView badge = new TextView(this);
        badge.setText(tab.adTab ? "Ad" : (tab.privateTab ? "P" : String.valueOf(index + 1)));
        badge.setTextColor(tab.privateTab ? Color.WHITE : Color.parseColor("#111111"));
        badge.setTextSize(13);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(roundRect(tab.privateTab ? Color.parseColor("#6D28D9") : COLOR_ACCENT, dp(14), 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        badgeParams.setMargins(0, 0, dp(12), 0);
        row.addView(badge, badgeParams);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        String tabTitle = tab.title == null || tab.title.length() == 0 ? (tab.privateTab ? "Tab privat" : "Tab baru") : tab.title;
        title.setText(tabTitle);
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(title);

        TextView url = new TextView(this);
        String urlText = tab.url == null || tab.url.length() == 0 ? "Halaman awal" : tab.url;
        url.setText(tab.privateTab ? "Privat • " + urlText : urlText);
        url.setTextColor(COLOR_SUBTEXT);
        url.setTextSize(12);
        url.setSingleLine(true);
        url.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(url);

        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(Color.WHITE);
        close.setTextSize(26);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> {
            closeTab(index);
            switchDialogSmooth(dialog, () -> showTabsPanel());
        });
        row.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));

        row.setOnClickListener(v -> {
            dialog.dismiss();
            switchToTab(index);
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
            menu.addView(menuRow(R.drawable.ic_private, "Privat", v -> {
                dialog.dismiss();
                newPrivateTab();
            }));
        }
        if (shortcutAdBlock) {
            menu.addView(menuRow(R.drawable.ic_shield, "AdBlock Premium " + (adBlock ? "ON" : "OFF"), v -> {
                adBlock = !adBlock;
                saveSettings();
                dialog.dismiss();
                Toast.makeText(this, adBlock ? "Ad Block aktif" : "Ad Block nonaktif", Toast.LENGTH_SHORT).show();
            }));
        }
        if (shortcutReader) {
            menu.addView(menuRow(R.drawable.ic_reader, "Reader Mode " + (readerMode ? "ON" : "OFF"), v -> {
                readerMode = !readerMode;
                saveSettings();
                dialog.dismiss();
                Toast.makeText(this, readerMode ? "Reader mode aktif" : "Reader mode nonaktif", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, videoControlsEnabled ? "Kontrol video aktif" : "Kontrol video nonaktif", Toast.LENGTH_SHORT).show();
            }));
        }

        if (shortcutReloadWebsite) {
            menu.addView(menuRow(R.drawable.ic_refresh, "Reload website", v -> {
                dialog.dismiss();
                reloadCurrentWebsite();
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
                recordCurrentPageToHistory();
                recordWebViewBackForwardHistory();
                saveBrowserHistory();
            } catch (Exception ignored) {}
            dialog.dismiss();
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
        return "0.9.31";
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
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(roundRect(Color.parseColor("#2A2D33"), dp(20), dp(1), Color.parseColor("#343841")));

        TextView t1 = new TextView(this);
        t1.setText(heading);
        t1.setTextColor(Color.parseColor("#E9E9EC"));
        t1.setTextSize(18);
        t1.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(t1);

        TextView t2 = new TextView(this);
        t2.setText(value);
        t2.setTextColor(Color.parseColor("#BFC2C9"));
        t2.setTextSize(15);
        t2.setLineSpacing(0f, 1.1f);
        LinearLayout.LayoutParams t2lp = new LinearLayout.LayoutParams(-1, -2);
        t2lp.setMargins(0, dp(6), 0, 0);
        card.addView(t2, t2lp);

        return card;
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
        panel.addView(actionRow(R.drawable.ic_private, "Privat", "Buka tab privat tanpa menyimpan riwayat.", v -> {
            dialog.dismiss();
            newPrivateTab();
        }));
        panel.addView(actionRow(R.drawable.ic_customize, "Sesuaikan menu", "Atur shortcut yang muncul di menu utama.", v -> {
            switchDialogSmooth(dialog, () -> showCustomizeMenuPanel());
        }));
        panel.addView(actionRow(R.drawable.ic_qr_scan, "Pindai QR Code", "Scan QR untuk membuka link atau mencari teks.", v -> {
            dialog.dismiss();
            openQrScanner();
        }));
        panel.addView(actionRow(R.drawable.ic_search_engine, "Search engine: " + searchEngine, "Pilih Google, Bing, DuckDuckGo, Yahoo, atau Yandex.", v -> {
            showSearchEngineDialog(dialog);
        }));

        panel.addView(sectionTitle("Alat halaman"));
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
        panel.addView(actionRow(R.drawable.ic_night, "Mode Malam: " + nightModeLabel(), "OFF, ON, Auto ikut sistem, dan pengecualian situs. Tidak menutup menu setelan.", v -> {
            showNightModeSettingsDialog();
        }));
        panel.addView(settingRow(R.drawable.ic_reader, "Reader / novel mode", "Mode baca ringan untuk artikel.", readerMode, v -> { readerMode = !readerMode; saveSettings(); }));
        panel.addView(actionRow(R.drawable.ic_shield, "AdBlock Premium: " + (adBlock ? "ON" : "OFF"), getAdBlockSummary(), v -> {
            showAdBlockSettingsDialog();
        }));
        panel.addView(settingRow(R.drawable.ic_data_saver, "Hemat data", "Matikan gambar otomatis saat browsing.", dataSaver, v -> { dataSaver = !dataSaver; applyBrowserSettings(); saveSettings(); }));
        panel.addView(settingRow(R.drawable.ic_desktop, "Desktop mode", "Paksa tampilan lebar seperti PC/laptop.", desktopMode, v -> {
            toggleDesktopModeSafely();
        }));
        panel.addView(actionRow(R.drawable.ic_text_size, "Ukuran teks: " + textZoom + "%", "Atur ukuran teks dengan slider persentase.", v -> {
            showTextZoomDialog(dialog);
        }));
        panel.addView(actionRow(R.drawable.ic_clear, "Bersihkan cache", "Hapus cache WebView.", v -> { if (webView != null) webView.clearCache(true); Toast.makeText(this, "Cache dibersihkan", Toast.LENGTH_SHORT).show(); }));


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
        if (!adBlock) return "AdBlock mati. Buka untuk pilih proteksi.";
        ArrayList<String> active = new ArrayList<>();
        if (adBlockPopupBlocker) active.add("popup");
        if (adBlockRedirectBlocker) active.add("redirect");
        if (adBlockScriptIframeBlocker) active.add("script/iframe");
        if (adBlockClickHijackBlocker) active.add("click hijack");
        if (adBlockRedirectToTempTab) active.add("tab iklan");
        if (adBlockAutoCloseAdTabs) active.add("auto close");
        if (active.isEmpty()) return "AdBlock aktif, belum ada proteksi detail ON.";
        return "Aktif: " + TextUtils.join(", ", active);
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
        title.setText("AdBlock Premium");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText("Atur proteksi iklan satu per satu. Video playback tetap otomatis di-allow agar tidak ikut keblok.");
        info.setTextColor(COLOR_SUBTEXT);
        info.setTextSize(13);
        info.setLineSpacing(0, 1.05f);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, -2);
        infoLp.setMargins(0, dp(8), 0, dp(12));
        box.addView(info, infoLp);

        box.addView(adBlockSwitchRow("AdBlock aktif", "Master ON/OFF semua proteksi iklan.", adBlock, v -> {
            adBlock = !adBlock;
            applyBrowserSettings();
            if (adBlock && webView != null && webView.getVisibility() == View.VISIBLE) injectPremiumAdBlock();
            saveSettings();
        }));

        box.addView(adBlockSwitchRow("Blokir popup iklan", "Matikan window.open dan pop-up otomatis.", adBlockPopupBlocker, v -> {
            adBlockPopupBlocker = !adBlockPopupBlocker;
            applyBrowserSettings();
            saveSettings();
        }));

        box.addView(adBlockSwitchRow("Blokir redirect iklan", "Cegah halaman pindah ke domain iklan random.", adBlockRedirectBlocker, v -> {
            adBlockRedirectBlocker = !adBlockRedirectBlocker;
            saveSettings();
        }));

        box.addView(adBlockSwitchRow("Blokir script/iframe iklan", "Blokir resource iklan, script, iframe, dan tracker.", adBlockScriptIframeBlocker, v -> {
            adBlockScriptIframeBlocker = !adBlockScriptIframeBlocker;
            saveSettings();
        }));

        box.addView(adBlockSwitchRow("Proteksi click hijack", "Cegah klik area web membuka link iklan tersembunyi.", adBlockClickHijackBlocker, v -> {
            adBlockClickHijackBlocker = !adBlockClickHijackBlocker;
            if (adBlock && webView != null && webView.getVisibility() == View.VISIBLE) injectPremiumAdBlock();
            saveSettings();
        }));

        box.addView(adBlockSwitchRow("Alihkan iklan ke tab sementara", "Jika web memaksa redirect iklan, buka sebagai tab iklan sementara agar tab utama tetap aman.", adBlockRedirectToTempTab, v -> {
            adBlockRedirectToTempTab = !adBlockRedirectToTempTab;
            saveSettings();
        }));

        box.addView(adBlockSwitchRow("Auto close tab iklan", "Tab hasil redirect/popup iklan otomatis ditutup agar tab tidak menumpuk.", adBlockAutoCloseAdTabs, v -> {
            adBlockAutoCloseAdTabs = !adBlockAutoCloseAdTabs;
            saveSettings();
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
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(roundRect(Color.parseColor("#30333A"), dp(18), dp(1), Color.parseColor("#3A3D45")));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowLp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F5F6F8"));
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        texts.addView(t);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(Color.parseColor("#AEB4BF"));
        d.setTextSize(12);
        d.setLineSpacing(0, 1.03f);
        d.setPadding(0, dp(6), dp(8), 0);
        texts.addView(d);

        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView status = new TextView(this);
        final boolean[] current = new boolean[]{enabled};
        status.setText(current[0] ? "ON" : "OFF");
        status.setTextColor(current[0] ? Color.parseColor("#65D889") : COLOR_SUBTEXT);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setBackground(roundRect(current[0] ? Color.parseColor("#173A25") : Color.parseColor("#3A3D45"), dp(16), dp(1), current[0] ? Color.parseColor("#20B35A") : Color.parseColor("#4A4D55")));
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(dp(58), dp(32));
        statusLp.setMargins(dp(12), 0, 0, 0);
        row.addView(status, statusLp);

        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            current[0] = !current[0];
            status.setText(current[0] ? "ON" : "OFF");
            status.setTextColor(current[0] ? Color.parseColor("#65D889") : COLOR_SUBTEXT);
            status.setBackground(roundRect(current[0] ? Color.parseColor("#173A25") : Color.parseColor("#3A3D45"), dp(16), dp(1), current[0] ? Color.parseColor("#20B35A") : Color.parseColor("#4A4D55")));
        });
        return row;
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
                Toast.makeText(this, "Izin kamera diperlukan untuk pindai QR", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "QR kosong", Toast.LENGTH_SHORT).show();
            return;
        }
        String value = result.trim();
        Toast.makeText(this, "QR terbaca", Toast.LENGTH_SHORT).show();
        addressBar.setText(value);
        openAddressBarUrl();
    }

    private interface QrResultListener {
        void onResult(String text);
    }

    private class QrScannerView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
        private Camera camera;
        private final MultiFormatReader reader = new MultiFormatReader();
        private final QrResultListener listener;
        private boolean scanned = false;

        QrScannerView(Activity context, QrResultListener listener) {
            super(context);
            this.listener = listener;
            getHolder().addCallback(this);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            startCamera(holder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            stopCamera();
        }

        private void startCamera(SurfaceHolder holder) {
            try {
                camera = Camera.open();
                Camera.Parameters params = camera.getParameters();
                try {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    camera.setParameters(params);
                } catch (Exception ignored) {
                }
                camera.setDisplayOrientation(90);
                camera.setPreviewDisplay(holder);
                camera.setPreviewCallback(this);
                camera.startPreview();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Kamera tidak bisa dibuka: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        void stopCamera() {
            try {
                if (camera != null) {
                    camera.setPreviewCallback(null);
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
            } catch (Exception ignored) {
            }
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (scanned || camera == null) return;
            try {
                Camera.Size size = camera.getParameters().getPreviewSize();
                PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                        data, size.width, size.height,
                        0, 0, size.width, size.height,
                        false
                );
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                Result result = reader.decodeWithState(bitmap);
                if (result != null && result.getText() != null) {
                    scanned = true;
                    stopCamera();
                    listener.onResult(result.getText());
                }
            } catch (NotFoundException ignored) {
                reader.reset();
            } catch (Exception ignored) {
                reader.reset();
            }
        }
    }

    private void showSearchEngineDialog(Dialog parentDialog) {
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
                    Toast.makeText(this, "Search engine: " + searchEngine, Toast.LENGTH_SHORT).show();
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
        panel.addView(customizeToggleRow(R.drawable.ic_shield, "AdBlock Premium", shortcutAdBlock, v -> { shortcutAdBlock = !shortcutAdBlock; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_reader, "Reader / Novel Mode", shortcutReader, v -> { shortcutReader = !shortcutReader; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_night, "Night Mode", shortcutNightMode, v -> { shortcutNightMode = !shortcutNightMode; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_qr_scan, "Pindai QR Code", shortcutQrScan, v -> { shortcutQrScan = !shortcutQrScan; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_history, "Riwayat", shortcutHistory, v -> { shortcutHistory = !shortcutHistory; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_find_page, "Cari di halaman", shortcutFindPage, v -> { shortcutFindPage = !shortcutFindPage; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_share, "Bagikan halaman", shortcutShare, v -> { shortcutShare = !shortcutShare; saveSettings(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_fullscreen, "Layar penuh", shortcutFullscreen, v -> { shortcutFullscreen = !shortcutFullscreen; saveSettings(); }));
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
            Toast.makeText(this, "Riwayat unduhan selesai dibersihkan", Toast.LENGTH_SHORT).show();
            switchDialogSmooth(dialog, () -> showDownloadSettingsPanel());
        }));

        panel.addView(actionRow(R.drawable.ic_settings, "Yield Fast Download", getAdvancedDownloadSummary(), v -> {
            showAdvancedDownloadFeaturesDialog(dialog);
        }));

        panel.addView(actionRow(R.drawable.ic_settings, "Download Queue: " + (downloadQueueEnabled ? "ON" : "OFF"), getDownloadQueueSummary(), v -> {
            showDownloadQueueSettingsDialog(dialog);
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

    private void showDownloadQueueSettingsDialog(Dialog parentDialog) {
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
            Toast.makeText(this, "Tampilan diurutkan berdasarkan antrian", Toast.LENGTH_SHORT).show();
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
        TextView chip = new TextView(this);
        chip.setText(label + " file aktif");
        chip.setGravity(Gravity.CENTER);
        chip.setTextSize(14);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setSingleLine(true);
        chip.setOnClickListener(v -> {
            downloadMaxActive = value;
            downloadQueueEnabled = true;
            downloadQueuePaused = false;
            saveSettings();
            pumpDownloadQueue();
            refreshDownloadPanel();
            if (refresh != null) refresh.run();
            Toast.makeText(this, "Maksimal download aktif: " + value, Toast.LENGTH_SHORT).show();
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

    private void showMaxActiveDownloadDialog(Dialog parentDialog) {
        // Fallback lama dipertahankan untuk kompatibilitas, tapi tidak menutup parent menu agar tidak kedip.
        String[] labels = new String[]{"2 file aktif", "3 file aktif", "4 file aktif"};
        int checked = Math.max(0, Math.min(2, downloadMaxActive - 2));
        new AlertDialog.Builder(this)
                .setTitle("Batas maksimal download aktif")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    downloadMaxActive = which + 2;
                    downloadQueueEnabled = true;
                    downloadQueuePaused = false;
                    saveSettings();
                    pumpDownloadQueue();
                    refreshDownloadPanel();
                    d.dismiss();
                    Toast.makeText(this, "Maksimal download aktif: " + downloadMaxActive, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
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
        info.setText("Pengaturan download cepat Yield.");
        info.setTextColor(COLOR_SUBTEXT);
        info.setTextSize(13);
        info.setLineSpacing(0, 1.05f);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, -2);
        infoLp.setMargins(0, dp(8), 0, dp(12));
        box.addView(info, infoLp);

        box.addView(advancedSwitchRow("Dynamic 2/4 koneksi", "File besar otomatis pakai 4 koneksi jika server support Range.", downloadDynamic4Connections, v -> {
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

        box.addView(advancedInfoRow("Smart resume"));
        box.addView(advancedInfoRow("Auto detect video file"));
        box.addView(advancedInfoRow("Auto rename file"));
        box.addView(advancedInfoRow("Real-time speed"));

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
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(roundRect(Color.parseColor("#30333A"), dp(18), dp(1), Color.parseColor("#3A3D45")));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowLp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F5F6F8"));
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        texts.addView(t);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(Color.parseColor("#AEB4BF"));
        d.setTextSize(12);
        d.setLineSpacing(0, 1.03f);
        d.setPadding(0, dp(6), dp(8), 0);
        texts.addView(d);

        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView status = new TextView(this);
        final boolean[] current = new boolean[]{enabled};
        status.setText(current[0] ? "ON" : "OFF");
        status.setTextColor(current[0] ? Color.parseColor("#65D889") : COLOR_SUBTEXT);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setBackground(roundRect(current[0] ? Color.parseColor("#173A25") : Color.parseColor("#3A3D45"), dp(16), dp(1), current[0] ? Color.parseColor("#20B35A") : Color.parseColor("#4A4D55")));
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(dp(58), dp(32));
        statusLp.setMargins(dp(12), 0, 0, 0);
        row.addView(status, statusLp);

        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            current[0] = !current[0];
            status.setText(current[0] ? "ON" : "OFF");
            status.setTextColor(current[0] ? Color.parseColor("#65D889") : COLOR_SUBTEXT);
            status.setBackground(roundRect(current[0] ? Color.parseColor("#173A25") : Color.parseColor("#3A3D45"), dp(16), dp(1), current[0] ? Color.parseColor("#20B35A") : Color.parseColor("#4A4D55")));
        });
        return row;
    }

    private View advancedInfoRow(String title) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(roundRect(Color.parseColor("#2C2F36"), dp(16), dp(1), Color.parseColor("#373B43")));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(52));
        rowLp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowLp);

        TextView check = new TextView(this);
        check.setText("✓");
        check.setTextColor(Color.parseColor("#65D889"));
        check.setTextSize(16);
        check.setTypeface(Typeface.DEFAULT_BOLD);
        check.setGravity(Gravity.CENTER);
        check.setBackground(roundRect(Color.parseColor("#173A25"), dp(14), 0, Color.TRANSPARENT));
        row.addView(check, new LinearLayout.LayoutParams(dp(30), dp(30)));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F5F6F8"));
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, -2, 1);
        tLp.setMargins(dp(12), 0, 0, 0);
        row.addView(t, tLp);
        return row;
    }

    private View advancedInfoRow(String title, String desc) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(roundRect(Color.parseColor("#2C2F36"), dp(16), dp(1), Color.parseColor("#373B43")));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowLp);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F5F6F8"));
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        row.addView(t);

        if (desc != null && desc.length() > 0) {
            TextView d = new TextView(this);
            d.setText(desc);
            d.setTextColor(Color.parseColor("#AEB4BF"));
            d.setTextSize(12);
            d.setPadding(0, dp(6), 0, 0);
            row.addView(d);
        }
        return row;
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
                    Toast.makeText(this, "Speed limiter: " + labels[which], Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Kontrol video disembunyikan. Ketuk video untuk munculkan lagi.", Toast.LENGTH_SHORT).show();
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
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(11);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(Color.parseColor("#20232A"), dp(18), dp(1), COLOR_BORDER));
        button.setOnClickListener(listener);
        button.setContentDescription(label);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(48));
        params.setMargins(dp(3), 0, dp(3), 0);
        button.setLayoutParams(params);
        return button;
    }

    private View videoButton(int iconRes, String label, View.OnClickListener listener) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setGravity(Gravity.CENTER);
        wrap.setBackground(roundRect(Color.parseColor("#20232A"), dp(18), dp(1), COLOR_BORDER));
        wrap.setOnClickListener(listener);
        wrap.setContentDescription(label);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE);
        wrap.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        if ("Play/Pause".equals(label)) videoPlayPauseIcon = icon;

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(48));
        params.setMargins(dp(3), 0, dp(3), 0);
        wrap.setLayoutParams(params);
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
            Toast.makeText(this, "Buka video dulu", Toast.LENGTH_SHORT).show();
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

    private void toggleVideoLandscapeMode() {
        // Mode Lans sudah dihapus. Jika ada pemanggilan lama, arahkan ke tombol fullscreen.
        toggleVideoFullLandscapeButton();
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
            videoLandscapeModeActive = false;
            fullscreenStartedFromVideoLandscape = false;

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
            Toast.makeText(this, "Keluar dari fullscreen video", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            try {
                restoreAfterVideoFullscreen();
                forcePortraitAfterVideoFullscreen();
            } catch (Exception ignored) {}
        }
    }

    private void enterVideoLandscapeMode() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            Toast.makeText(this, "Buka video dulu", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (homeScroll != null && homeScroll.getVisibility() == View.VISIBLE) {
                restoreHiddenWebPage();
            }
            if (!videoLandscapeModeActive) {
                orientationBeforeVideoLandscape = getRequestedOrientation();
            }
            videoLandscapeModeActive = true;
            fullscreenStartedFromVideoLandscape = false;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            if (!desktopMode) {
                applyBrowserSettings();
                applyMobileViewportIfNeeded();
                mainHandler.postDelayed(() -> applyMobileViewportIfNeeded(), 450);
            }
            restoreVideoControlsFromFullscreenOverlay();
            if (topBarView != null) topBarView.setVisibility(View.VISIBLE);
            if (bottomNavView != null) bottomNavView.setVisibility(View.VISIBLE);
            updateVideoModeToggleButton();
            videoControlsManualHidden = false;
            checkAndShowVideoControls();
            Toast.makeText(this, "Mode landscape video aktif. Tekan Full untuk layar penuh.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Mode landscape tidak didukung di halaman ini", Toast.LENGTH_SHORT).show();
        }
    }

    private void exitVideoLandscapeMode() {
        exitVideoLandscapeMode(true);
    }

    private void exitVideoLandscapeMode(boolean showToast) {
        try {
            if (!videoLandscapeModeActive) return;
            videoLandscapeModeActive = false;
            fullscreenStartedFromVideoLandscape = false;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            setRequestedOrientation(orientationBeforeVideoLandscape);
            if (!desktopMode) {
                applyBrowserSettings();
                applyMobileViewportIfNeeded();
                mainHandler.postDelayed(() -> applyMobileViewportIfNeeded(), 450);
            }
            restoreVideoControlsFromFullscreenOverlay();
            if (topBarView != null) topBarView.setVisibility(View.VISIBLE);
            if (bottomNavView != null) bottomNavView.setVisibility(View.VISIBLE);
            updateVideoModeToggleButton();
            checkAndShowVideoControls();
            if (showToast) Toast.makeText(this, "Mode landscape video selesai", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    private void enterVideoFullscreen() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            Toast.makeText(this, "Buka video dulu", Toast.LENGTH_SHORT).show();
            return;
        }
        videoLandscapeModeActive = false;
        fullscreenStartedFromVideoLandscape = false;

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
            videoLandscapeModeActive = false;
            fullscreenStartedFromVideoLandscape = false;
            originalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            originalRequestedOrientation = getRequestedOrientation();
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
            Toast.makeText(this, "Mode fullscreen tidak didukung di halaman ini", Toast.LENGTH_SHORT).show();
        }
    }

    private void exitAppVideoFullscreenFallback() {
        try {
            restoreAfterVideoFullscreen();
        } catch (Exception ignored) {
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
            videoLandscapeModeActive = false;
            fullscreenStartedFromVideoLandscape = false;
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

    private String getVideoOptimizationSummary() {
        ArrayList<String> items = new ArrayList<>();
        if (videoBufferBooster) items.add("buffer");
        if (hlsSegmentPrefetch) items.add("HLS prefetch");
        if (videoFloatingPlayer) items.add("fullscreen helper");
        if (videoBackgroundPlay) items.add("background");
        if (items.isEmpty()) return "Semua optimasi video tambahan mati.";
        return "Aktif: " + TextUtils.join(", ", items);
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
            Toast.makeText(this, "Minimize normal aktif", Toast.LENGTH_SHORT).show();
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
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(roundRect(Color.parseColor("#30333A"), dp(18), dp(1), Color.parseColor("#3A3D45")));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowLp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F5F6F8"));
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        texts.addView(t);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(Color.parseColor("#AEB4BF"));
        d.setTextSize(12);
        d.setLineSpacing(0, 1.03f);
        d.setPadding(0, dp(6), dp(8), 0);
        texts.addView(d);

        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView status = new TextView(this);
        final boolean[] current = new boolean[]{enabled};
        status.setText(current[0] ? "ON" : "OFF");
        status.setTextColor(current[0] ? Color.parseColor("#65D889") : COLOR_SUBTEXT);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setBackground(roundRect(current[0] ? Color.parseColor("#173A25") : Color.parseColor("#3A3D45"), dp(16), dp(1), current[0] ? Color.parseColor("#20B35A") : Color.parseColor("#4A4D55")));
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(dp(58), dp(32));
        statusLp.setMargins(dp(12), 0, 0, 0);
        row.addView(status, statusLp);

        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            current[0] = !current[0];
            status.setText(current[0] ? "ON" : "OFF");
            status.setTextColor(current[0] ? Color.parseColor("#65D889") : COLOR_SUBTEXT);
            status.setBackground(roundRect(current[0] ? Color.parseColor("#173A25") : Color.parseColor("#3A3D45"), dp(16), dp(1), current[0] ? Color.parseColor("#20B35A") : Color.parseColor("#4A4D55")));
        });
        return row;
    }

    private View videoOptInfoRow(String title, String desc) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(roundRect(Color.parseColor("#2C2F36"), dp(16), dp(1), Color.parseColor("#373B43")));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowLp);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F5F6F8"));
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        row.addView(t);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(Color.parseColor("#AEB4BF"));
        d.setTextSize(12);
        d.setPadding(0, dp(6), 0, 0);
        row.addView(d);
        return row;
    }

    private void detectVideoQualities() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) return;
        try {
            webView.evaluateJavascript("(function(){try{return (window.__yieldVideoQualities||[]).join(', ');}catch(e){return '';}})()", value -> {
                String result = value == null ? "" : value.replace("\"", "");
                if (result.length() > 0) {
                    Toast.makeText(this, "Kualitas terdeteksi: " + result + "p", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void showVideoQualityDialog() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            Toast.makeText(this, "Buka video dulu", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Kualitas video: Auto", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Kualitas video: " + selectedVideoQuality, Toast.LENGTH_SHORT).show();
                } else if (result.contains("no_video")) {
                    Toast.makeText(this, "Video belum ditemukan", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Kualitas " + selectedVideoQuality + " tidak tersedia di player ini", Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Gagal mengubah kualitas video", Toast.LENGTH_SHORT).show();
        }
    }

    private void showVideoSpeedDialog() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            Toast.makeText(this, "Buka video dulu", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "Speed video: " + formatVideoSpeed(videoSpeed), Toast.LENGTH_SHORT).show();
    }

    private void injectVideoPlaybackWatcher() {
        if (webView == null) return;
        if (isSiteCompatibilityModeActiveForUrl(getEffectiveCurrentUrl())) return;
        String js = "javascript:(function(){"
                + "window.__yieldVideoWatcherInstalled=true;"
                + "var Y_BUF=" + (videoBufferBooster ? "true" : "false") + ",Y_HLS=" + (hlsSegmentPrefetch ? "true" : "false") + ",Y_BG=" + (videoBackgroundPlay ? "true" : "false") + ";"
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
            Toast.makeText(this, "Buka video dulu", Toast.LENGTH_SHORT).show();
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

    private void cycleVideoSpeed() {
        if (videoSpeed < 0.75f) setVideoSpeed(1.0f);
        else if (videoSpeed < 1.1f) setVideoSpeed(1.25f);
        else if (videoSpeed < 1.35f) setVideoSpeed(1.5f);
        else if (videoSpeed < 1.75f) setVideoSpeed(2.0f);
        else setVideoSpeed(0.5f);
    }

    private String formatVideoSpeed(float speed) {
        if (Math.abs(speed - 1.0f) < 0.01f) return "1x";
        if (Math.abs(speed - 2.0f) < 0.01f) return "2x";
        if (Math.abs(speed - 0.5f) < 0.01f) return "0.5x";
        if (Math.abs(speed - 1.25f) < 0.01f) return "1.25x";
        if (Math.abs(speed - 1.5f) < 0.01f) return "1.5x";
        return speed + "x";
    }

    private void showHistoryPanel() {
        recordCurrentPageToHistory();
        recordWebViewBackForwardHistory();
        loadBrowserHistory();
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(18), dp(18), dp(18), dp(10));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Histori");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        ImageButton search = plainIconButton(R.drawable.ic_search, v -> Toast.makeText(this, "Cari di histori akan menyaring daftar otomatis saat teks diketik", Toast.LENGTH_SHORT).show());
        top.addView(search, new LinearLayout.LayoutParams(dp(40), dp(40)));
        ImageButton close = plainIconButton(R.drawable.ic_exit, v -> dialog.dismiss());
        top.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        root.addView(top);

        TextView clearText = new TextView(this);
        clearText.setText("Kelola / hapus riwayat...");
        clearText.setTextColor(Color.parseColor("#F97352"));
        clearText.setTextSize(15);
        clearText.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(-1, -2);
        clearParams.setMargins(0, dp(16), 0, dp(12));
        root.addView(clearText, clearParams);
        clearText.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Kelola riwayat")
                    .setMessage("Riwayat tidak dihapus otomatis. Hapus semua riwayat browsing?")
                    .setPositiveButton("Hapus", (d, w) -> {
                        clearBrowserHistoryManually();
                        switchDialogSmooth(dialog, () -> showHistoryPanel());
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);

        renderHistoryList(list, dialog);

        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        dialog.setContentView(root);
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event != null && event.getAction() == KeyEvent.ACTION_UP) {
                d.dismiss();
                return true;
            }
            return false;
        });
        dialog.show();
        applyDarkFullscreenDialog(dialog);
    }

    private void renderHistoryList(LinearLayout list, Dialog dialog) {
        if (list == null) return;
        try {
            list.removeAllViews();
            if (historyData.isEmpty()) {
                TextView empty = new TextView(this);
                empty.setText("Riwayat masih kosong.");
                empty.setTextColor(COLOR_SUBTEXT);
                empty.setTextSize(16);
                empty.setPadding(0, dp(20), 0, 0);
                list.addView(empty);
                return;
            }

            String lastHeader = "";
            ArrayList<HistoryItemData> snapshot = new ArrayList<>(historyData);
            for (HistoryItemData item : snapshot) {
                if (item == null) continue;
                String header = historyDayLabel(item.time);
                if (!header.equals(lastHeader)) {
                    TextView section = new TextView(this);
                    section.setText(header);
                    section.setTextColor(COLOR_SUBTEXT);
                    section.setTextSize(14);
                    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
                    sp.setMargins(0, dp(10), 0, dp(8));
                    list.addView(section, sp);
                    lastHeader = header;
                }
                list.addView(historyRow(item, dialog, list));
            }
        } catch (Exception ignored) {
        }
    }

    private void removeHistoryItemExact(HistoryItemData item) {
        if (item == null) return;
        try {
            String targetUrl = item.url == null ? "" : item.url;
            long targetTime = item.time;
            for (int i = historyData.size() - 1; i >= 0; i--) {
                HistoryItemData old = historyData.get(i);
                if (old == null) continue;
                String oldUrl = old.url == null ? "" : old.url;
                if (old == item || (oldUrl.equals(targetUrl) && old.time == targetTime)) {
                    historyData.remove(i);
                }
            }
        } catch (Exception ignored) {
            try { historyData.remove(item); } catch (Exception ignored2) {}
        }
    }

    private View historyRow(HistoryItemData item, Dialog dialog) {
        return historyRow(item, dialog, null);
    }

    private View historyRow(HistoryItemData item, Dialog dialog, LinearLayout parentList) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setOnClickListener(v -> {
            dialog.dismiss();
            addressBar.setText(item.url);
            openAddressBarUrl();
        });

        FrameLayout iconWrap = new FrameLayout(this);
        LinearLayout.LayoutParams iw = new LinearLayout.LayoutParams(dp(42), dp(42));
        TextView fallback = circleLetter(historyInitial(item), Color.parseColor("#2C3038"), Color.parseColor("#DCE2EC"));
        iconWrap.addView(fallback, new FrameLayout.LayoutParams(-1, -1));
        ImageView favicon = new ImageView(this);
        favicon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        favicon.setPadding(dp(9), dp(9), dp(9), dp(9));
        favicon.setBackground(roundRect(Color.parseColor("#2A2D33"), dp(21), 0, Color.TRANSPARENT));
        favicon.setVisibility(View.GONE);
        iconWrap.addView(favicon, new FrameLayout.LayoutParams(-1, -1));
        loadFavicon(item.url, favicon, fallback);
        row.addView(iconWrap, iw);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1);
        tp.setMargins(dp(14), 0, dp(10), 0);

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

        TextView delete = new TextView(this);
        delete.setText("×");
        delete.setTextColor(Color.parseColor("#C9CED8"));
        delete.setTextSize(24);
        delete.setGravity(Gravity.CENTER);
        delete.setClickable(true);
        delete.setFocusable(true);
        delete.setOnTouchListener((v, event) -> {
            if (event != null && event.getAction() == MotionEvent.ACTION_UP) v.performClick();
            return true;
        });
        delete.setOnClickListener(v -> {
            removeHistoryItemExact(item);
            saveBrowserHistory();
            if (parentList != null) {
                renderHistoryList(parentList, dialog);
            } else {
                try { if (dialog != null && dialog.isShowing()) dialog.dismiss(); } catch (Exception ignored) {}
                showHistoryPanel();
            }
        });
        row.addView(delete, new LinearLayout.LayoutParams(dp(36), dp(36)));
        return row;
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
                Toast.makeText(this, "Mode pilih bookmark aktif", Toast.LENGTH_SHORT).show();
            } else if ("Edit".equals(t)) {
                showEditBookmarkDialog(item, dialog, refresh);
            } else if ("Salin link".equals(t)) {
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cb != null) cb.setPrimaryClip(ClipData.newPlainText("bookmark", item.url));
                Toast.makeText(this, "Link bookmark disalin", Toast.LENGTH_SHORT).show();
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
                newNormalTab();
                addressBar.setText(item.url);
                openAddressBarUrl();
            } else if ("Buka di tab Privat".equals(t)) {
                dialog.dismiss();
                newPrivateTab();
                addressBar.setText(item.url);
                openAddressBarUrl();
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
                    Toast.makeText(this, "Folder ditambahkan", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Urutan bookmark diperbarui", Toast.LENGTH_SHORT).show();
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
            else Toast.makeText(this, "Mode pilih bookmark aktif", Toast.LENGTH_SHORT).show();
            return true;
        });
        menu.show();
    }

    private void switchDialogSmooth(Dialog currentDialog, Runnable openNext) {
        try {
            if (openNext != null) openNext.run();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuka menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private TextView circleEmoji(String text, int bg, int fg) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(fg);
        tv.setTextSize(22);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(roundRect(bg, dp(28), 0, Color.TRANSPARENT));
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
        return a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR)
                && a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR);
    }

    private void showFindInPageDialog() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            Toast.makeText(this, "Buka halaman dulu untuk mencari teks", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(this, numberOfMatches + " hasil ditemukan", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Belum ada halaman untuk dibagikan", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Belum ada link untuk disalin", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Yield Browser URL", url));
            Toast.makeText(this, "Link disalin", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Mode layar penuh aktif. Tekan Back untuk keluar.", Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, "Buka halaman dulu untuk disimpan", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File dir = new File(getExternalFilesDir(null), "OfflinePages");
            if (!dir.exists()) dir.mkdirs();
            String safeName = "page_" + System.currentTimeMillis() + ".mht";
            File out = new File(dir, safeName);
            webView.saveWebArchive(out.getAbsolutePath());
            Toast.makeText(this, "Halaman disimpan: " + out.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal menyimpan halaman: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private SharedPreferences historyStore() {
        return getSharedPreferences(PREFS_HISTORY_V2, MODE_PRIVATE);
    }

    private File historyV3File() {
        File dir = new File(getFilesDir(), HISTORY_V3_FOLDER);
        try { if (!dir.exists()) dir.mkdirs(); } catch (Exception ignored) {}
        return new File(dir, HISTORY_V3_PUBLIC_FILE);
    }

    private File historyV3LegacyFile() {
        return new File(getFilesDir(), HISTORY_V3_FILE);
    }

    private File historyV3ExternalFile() {
        try {
            File base = getExternalFilesDir(null);
            if (base == null) return null;
            File dir = new File(base, HISTORY_V3_FOLDER);
            try { if (!dir.exists()) dir.mkdirs(); } catch (Exception ignored) {}
            return new File(dir, HISTORY_V3_PUBLIC_FILE);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readTextFileSafe(File file) {
        if (file == null || !file.exists()) return "";
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            long length = Math.max(0, Math.min(file.length(), 1024L * 1024L));
            byte[] data = new byte[(int) length];
            int len = fis.read(data);
            if (len <= 0) return "";
            return new String(data, 0, len, "UTF-8");
        } catch (Exception ignored) {
            return "";
        } finally {
            try { if (fis != null) fis.close(); } catch (Exception ignored) {}
        }
    }

    private void writeTextFileSafe(File file, String value) {
        if (file == null || value == null) return;
        FileOutputStream fos = null;
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            fos = new FileOutputStream(file, false);
            fos.write(value.getBytes("UTF-8"));
            fos.flush();
            try { fos.getFD().sync(); } catch (Exception ignored) {}
        } catch (Exception ignored) {
        } finally {
            try { if (fos != null) fos.close(); } catch (Exception ignored) {}
        }
    }

    private String normalizeHistoryRowsText(String saved) {
        if (saved == null || saved.length() == 0) return "";
        // Perbaikan penting: versi lama menyimpan pemisah sebagai teks "\\n".
        // Normalisasi dulu agar bisa dibaca sebagai baris sungguhan.
        return saved.replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace("\r", "\n");
    }

    private String serializeHistoryList(ArrayList<HistoryItemData> list) {
        StringBuilder sb = new StringBuilder();
        if (list == null) return "";
        for (HistoryItemData item : list) {
            if (item == null || !shouldRecordHistoryUrl(item.url)) continue;
            sb.append(encode(item.title)).append("|")
                    .append(encode(item.url)).append("|")
                    .append(item.time).append('\n');
        }
        return sb.toString();
    }

    private void persistHistoryEverywhere() {
        String value = serializeHistoryList(historyData);

        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_BROWSER_HISTORY, value)
                .putString(KEY_BROWSER_HISTORY_BACKUP, value)
                .putString(KEY_BROWSER_HISTORY_V2, value)
                .putString(KEY_BROWSER_HISTORY_V3, value)
                .commit();

        historyStore().edit()
                .putString(KEY_BROWSER_HISTORY_V2, value)
                .putString(KEY_BROWSER_HISTORY_V3, value)
                .commit();

        writeTextFileSafe(historyV3File(), value);
        writeTextFileSafe(historyV3LegacyFile(), value);
        writeTextFileSafe(historyV3ExternalFile(), value);
    }

    private void mergeHistoryRows(String saved, ArrayList<HistoryItemData> out) {
        if (saved == null || saved.length() == 0 || out == null) return;
        saved = normalizeHistoryRowsText(saved);
        if (saved.length() == 0) return;

        String[] rows = saved.split("\n");
        for (String row : rows) {
            if (row == null) continue;
            row = row.trim();
            if (row.length() == 0) continue;

            String[] parts = row.split("\\|", 3);
            if (parts.length == 3) {
                try {
                    String title = decode(parts[0]);
                    String url = decode(parts[1]);
                    long time = Long.parseLong(parts[2].trim());
                    if (!shouldRecordHistoryUrl(url)) continue;

                    boolean duplicate = false;
                    for (HistoryItemData old : out) {
                        if (old != null && url.equals(old.url)) {
                            if (time > old.time) {
                                old.title = title;
                                old.time = time;
                            }
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) out.add(new HistoryItemData(title, url, time));
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void recordWebViewBackForwardHistory() {
        try {
            if (historyClearLock) return;
            if (isCurrentPrivateTab()) return;
            if (webView == null) return;
            WebBackForwardList list = webView.copyBackForwardList();
            if (list == null) return;
            for (int i = 0; i < list.getSize(); i++) {
                WebHistoryItem item = list.getItemAtIndex(i);
                if (item == null) continue;
                String url = item.getUrl();
                String title = item.getTitle();
                if (shouldRecordHistoryUrl(url)) {
                    addBrowserHistory(title, url);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void recordCurrentPageToHistory() {
        try {
            if (historyClearLock) return;
            if (isCurrentPrivateTab()) return;
            if (webView == null) return;
            String url = webView.getUrl();
            if (shouldRecordHistoryUrl(url)) {
                addBrowserHistory(webView.getTitle(), url);
            }
        } catch (Exception ignored) {
        }
    }

    private void addBrowserHistory(String title, String url) {
        if (isCurrentPrivateTab()) return;
        if (!shouldRecordHistoryUrl(url)) return;
        String cleanUrl = extractOriginalUrl(url);
        if (cleanUrl == null || cleanUrl.length() == 0) return;

        for (int i = historyData.size() - 1; i >= 0; i--) {
            HistoryItemData old = historyData.get(i);
            if (old == null || cleanUrl.equals(old.url)) {
                historyData.remove(i);
            }
        }

        String safeTitle = title == null || title.trim().length() == 0 ? cleanUrl : title.trim();
        historyData.add(0, new HistoryItemData(safeTitle, cleanUrl, System.currentTimeMillis()));
        while (historyData.size() > 500) historyData.remove(historyData.size() - 1);
        persistHistoryEverywhere();
    }

    private void loadBrowserHistory() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences h = historyStore();

        ArrayList<HistoryItemData> loaded = new ArrayList<>();
        mergeHistoryRows(readTextFileSafe(historyV3File()), loaded);
        mergeHistoryRows(readTextFileSafe(historyV3LegacyFile()), loaded);
        mergeHistoryRows(readTextFileSafe(historyV3ExternalFile()), loaded);
        mergeHistoryRows(h.getString(KEY_BROWSER_HISTORY_V3, ""), loaded);
        mergeHistoryRows(h.getString(KEY_BROWSER_HISTORY_V2, ""), loaded);
        mergeHistoryRows(p.getString(KEY_BROWSER_HISTORY_V3, ""), loaded);
        mergeHistoryRows(p.getString(KEY_BROWSER_HISTORY_V2, ""), loaded);
        mergeHistoryRows(p.getString(KEY_BROWSER_HISTORY, ""), loaded);
        mergeHistoryRows(p.getString(KEY_BROWSER_HISTORY_BACKUP, ""), loaded);

        historyData.clear();
        if (!loaded.isEmpty()) {
            Collections.sort(loaded, (a, b) -> Long.compare(b.time, a.time));
            while (loaded.size() > 500) loaded.remove(loaded.size() - 1);
            historyData.addAll(loaded);
            persistHistoryEverywhere();
        }
    }

    private void saveBrowserHistory() {
        persistHistoryEverywhere();
    }

    private void clearBrowserHistoryManually() {
        historyClearLock = true;
        historyData.clear();
        lastSafeHttpUrl = "";

        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_BROWSER_HISTORY, "")
                .putString(KEY_BROWSER_HISTORY_BACKUP, "")
                .putString(KEY_BROWSER_HISTORY_V2, "")
                .putString(KEY_BROWSER_HISTORY_V3, "")
                .commit();
        historyStore().edit()
                .putString(KEY_BROWSER_HISTORY_V2, "")
                .putString(KEY_BROWSER_HISTORY_V3, "")
                .commit();
        try {
            File f = historyV3File();
            if (f.exists()) f.delete();
        } catch (Exception ignored) {
        }
        try {
            File f = historyV3LegacyFile();
            if (f.exists()) f.delete();
        } catch (Exception ignored) {
        }
        try {
            File f = historyV3ExternalFile();
            if (f != null && f.exists()) f.delete();
        } catch (Exception ignored) {
        }

        try {
            if (webView != null) {
                webView.stopLoading();
                webView.clearHistory();
                webView.clearFormData();
                webView.loadUrl("about:blank");
                mainHandler.postDelayed(() -> {
                    try { if (webView != null) webView.clearHistory(); } catch (Exception ignored) {}
                }, 350);
            }
        } catch (Exception ignored) {
        }

        try {
            TabInfo currentTab = getCurrentTab();
            if (currentTab != null) {
                currentTab.url = "";
                currentTab.title = "Tab utama";
                currentTab.adTab = false;
            }
        } catch (Exception ignored) {
        }

        try {
            if (addressBar != null) addressBar.setText("");
            showHome();
        } catch (Exception ignored) {
        }
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
            Toast.makeText(this, "Ukuran teks: " + textZoom + "%", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Default: Download/Yield Browser", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Subfolder staging disimpan", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Pemilih folder tidak tersedia", Toast.LENGTH_SHORT).show();
        }
    }

    private TextView sectionTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(COLOR_SUBTEXT);
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(dp(8), dp(14), 0, dp(8));
        return title;
    }

    private View menuDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#2D333D"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
        params.setMargins(dp(12), dp(8), dp(12), dp(8));
        divider.setLayoutParams(params);
        return divider;
    }

    private View customizeToggleRow(int iconRes, String label, boolean enabled, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(roundRect(Color.parseColor("#383A3E"), dp(10), 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(70));
        rowParams.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#E7E8EA"));
        row.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(16);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMargins(dp(14), 0, dp(8), 0);
        row.addView(text, textParams);

        TextView status = new TextView(this);
        final boolean[] current = new boolean[]{enabled};
        status.setText(current[0] ? "ON" : "OFF");
        status.setTextColor(current[0] ? COLOR_ON : COLOR_SUBTEXT);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setBackground(roundRect(current[0] ? Color.parseColor("#15351F") : Color.parseColor("#343740"), dp(12), dp(1), current[0] ? COLOR_ON : COLOR_BORDER));
        row.addView(status, new LinearLayout.LayoutParams(dp(46), dp(28)));

        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            current[0] = !current[0];
            status.setText(current[0] ? "ON" : "OFF");
            status.setTextColor(current[0] ? COLOR_ON : COLOR_SUBTEXT);
            status.setBackground(roundRect(current[0] ? Color.parseColor("#15351F") : Color.parseColor("#343740"), dp(12), dp(1), current[0] ? COLOR_ON : COLOR_BORDER));
        });
        return row;
    }

    private View settingRow(int iconRes, String title, String desc, boolean enabled, View.OnClickListener listener) {
        LinearLayout row = baseSettingsRow(iconRes, title, desc, null);
        TextView status = new TextView(this);
        final boolean[] current = new boolean[]{enabled};
        status.setText(current[0] ? "ON" : "OFF");
        status.setTextColor(current[0] ? COLOR_ON : COLOR_SUBTEXT);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setBackground(roundRect(current[0] ? Color.parseColor("#15351F") : Color.parseColor("#343740"), dp(12), dp(1), current[0] ? COLOR_ON : COLOR_BORDER));
        row.addView(status, new LinearLayout.LayoutParams(dp(46), dp(28)));

        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            current[0] = !current[0];
            status.setText(current[0] ? "ON" : "OFF");
            status.setTextColor(current[0] ? COLOR_ON : COLOR_SUBTEXT);
            status.setBackground(roundRect(current[0] ? Color.parseColor("#15351F") : Color.parseColor("#343740"), dp(12), dp(1), current[0] ? COLOR_ON : COLOR_BORDER));
        });
        return row;
    }

    private View actionRow(int iconRes, String title, String desc, View.OnClickListener listener) {
        LinearLayout row = baseSettingsRow(iconRes, title, desc, listener);
        ImageView arrow = new ImageView(this);
        arrow.setImageResource(R.drawable.ic_forward);
        arrow.setColorFilter(COLOR_SUBTEXT);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(20), dp(20)));
        return row;
    }

    private LinearLayout baseSettingsRow(int iconRes, String title, String desc, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(11), dp(10), dp(11));
        row.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#F3F5F8"));
        row.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textBoxParams = new LinearLayout.LayoutParams(0, -2, 1);
        textBoxParams.setMargins(dp(14), 0, dp(8), 0);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        textBox.addView(titleView);

        TextView descView = new TextView(this);
        descView.setText(desc);
        descView.setTextColor(COLOR_SUBTEXT);
        descView.setTextSize(12);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        descParams.setMargins(0, dp(3), 0, 0);
        textBox.addView(descView, descParams);

        row.addView(textBox, textBoxParams);
        return row;
    }

    private View menuRow(int iconRes, String label, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(14), dp(14), dp(14));
        row.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#F3F5F8"));
        row.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(17);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(-2, -2);
        textParams.setMargins(dp(18), 0, 0, 0);
        row.addView(text, textParams);
        return row;
    }


    // ===== Download manager UI =====
    private void showDownloadManager() {
        Dialog dialog = new Dialog(this);
        activeDownloadDialog = dialog;
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(10));
        root.setBackgroundColor(COLOR_BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText(downloadSelectMode ? selectedDownloadIds.size() + " dipilih" : "Download");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(52), 1));

        ImageButton settings = smallTopIcon(R.drawable.ic_settings, "Pengaturan unduhan", v -> showDownloadSettingsPanel());
        header.addView(settings, new LinearLayout.LayoutParams(dp(44), dp(44)));

        ImageButton search = smallTopIcon(R.drawable.ic_search, "Cari unduhan", v -> showDownloadSearchDialog());
        header.addView(search, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(Color.parseColor("#D7DAE0"));
        close.setTextSize(36);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
        root.addView(header);

        TextView storage = new TextView(this);
        storage.setText(getStorageUsageText());
        storage.setTextColor(COLOR_SUBTEXT);
        storage.setTextSize(13);
        storage.setPadding(0, dp(2), 0, dp(12));
        root.addView(storage);

        View line = new View(this);
        line.setBackgroundColor(Color.parseColor("#2A2E36"));
        root.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));

        HorizontalScrollView categoryScroll = new HorizontalScrollView(this);
        categoryScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout categories = new LinearLayout(this);
        categories.setOrientation(LinearLayout.HORIZONTAL);
        categories.setPadding(0, dp(12), 0, dp(8));
        categories.addView(downloadCategoryChip("Semua"));
        categories.addView(downloadCategoryChip("Video"));
        categories.addView(downloadCategoryChip("APK"));
        categories.addView(downloadCategoryChip("Dokumen"));
        categories.addView(downloadCategoryChip("Musik"));
        categories.addView(downloadCategoryChip("Lainnya"));
        categoryScroll.addView(categories);
        root.addView(categoryScroll, new LinearLayout.LayoutParams(-1, -2));

        activeDownloadListPanel = new LinearLayout(this);
        activeDownloadListPanel.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(activeDownloadListPanel);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        renderDownloadList();

        dialog.setContentView(root);
        dialog.setOnDismissListener(d -> {
            activeDownloadDialog = null;
            activeDownloadListPanel = null;
            downloadSelectMode = false;
            selectedDownloadIds.clear();
        });

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

    private TextView downloadCategoryChip(String label) {
        TextView chip = new TextView(this);
        boolean selected = label.equals(activeDownloadCategory);
        chip.setText(label);
        chip.setTextColor(selected ? Color.parseColor("#111111") : Color.WHITE);
        chip.setTextSize(13);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(14), 0, dp(14), 0);
        chip.setBackground(roundRect(selected ? COLOR_ACCENT : Color.parseColor("#20232A"), dp(18), dp(1), selected ? COLOR_ACCENT : COLOR_BORDER));
        chip.setOnClickListener(v -> {
            activeDownloadCategory = label;
            selectedDownloadIds.clear();
            downloadSelectMode = false;
            if (activeDownloadDialog != null && activeDownloadDialog.isShowing()) {
                Dialog currentDialog = activeDownloadDialog;
                currentDialog.dismiss();
                mainHandler.post(this::showDownloadManager);
            } else {
                renderDownloadList();
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(36));
        params.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(params);
        return chip;
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
                    dialog.dismiss();
                    renderDownloadList();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void renderDownloadList() {
        if (activeDownloadListPanel == null) return;
        activeDownloadListPanel.removeAllViews();

        activeDownloadListPanel.addView(downloadToolRow());
        activeDownloadListPanel.addView(downloadQueueControlRow());

        ArrayList<DownloadItem> items = getFilteredDownloadItems();

        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            String msg = activeDownloadSearchQuery.length() > 0
                    ? "Tidak ada unduhan yang cocok."
                    : "Belum ada unduhan di kategori ini.";
            empty.setText(msg);
            empty.setTextColor(COLOR_SUBTEXT);
            empty.setTextSize(15);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(48), 0, dp(48));
            activeDownloadListPanel.addView(empty, new LinearLayout.LayoutParams(-1, -2));
            return;
        }

        TextView section = new TextView(this);
        section.setText(getDownloadSectionTitle(items.size()));
        section.setTextColor(COLOR_SUBTEXT);
        section.setTextSize(16);
        section.setTypeface(Typeface.DEFAULT_BOLD);
        section.setPadding(0, dp(16), 0, dp(8));
        activeDownloadListPanel.addView(section);

        for (DownloadItem item : items) {
            activeDownloadListPanel.addView(downloadRow(item));
        }
    }

    private View downloadToolRow() {
        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(0, dp(8), 0, dp(4));

        TextView sort = downloadToolButton("Urut: " + activeDownloadSort);
        sort.setOnClickListener(v -> showDownloadSortDialog());
        tools.addView(sort, new LinearLayout.LayoutParams(0, dp(42), 1));

        TextView select = downloadToolButton(downloadSelectMode ? "Batal pilih" : "Pilih");
        select.setOnClickListener(v -> {
            downloadSelectMode = !downloadSelectMode;
            selectedDownloadIds.clear();
            renderDownloadList();
        });
        LinearLayout.LayoutParams selectParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        selectParams.setMargins(dp(8), 0, 0, 0);
        tools.addView(select, selectParams);

        if (downloadSelectMode) {
            TextView share = downloadToolButton("Bagikan");
            share.setOnClickListener(v -> shareSelectedDownloads());
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(42), 1);
            p.setMargins(dp(8), 0, 0, 0);
            tools.addView(share, p);

            TextView delete = downloadToolButton("Hapus");
            delete.setTextColor(Color.WHITE);
            delete.setBackground(roundRect(Color.parseColor("#E5484D"), dp(18), 0, Color.TRANSPARENT));
            delete.setOnClickListener(v -> deleteSelectedDownloads());
            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, dp(42), 1);
            p2.setMargins(dp(8), 0, 0, 0);
            tools.addView(delete, p2);
        }

        return tools;
    }

    private View downloadQueueControlRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView pause = downloadToolButton("Pause semua");
        pause.setOnClickListener(v -> pauseAllDownloads());
        row.addView(pause, new LinearLayout.LayoutParams(0, dp(38), 1));

        TextView resume = downloadToolButton("Resume semua");
        resume.setOnClickListener(v -> resumeAllDownloads());
        LinearLayout.LayoutParams resumeLp = new LinearLayout.LayoutParams(0, dp(38), 1);
        resumeLp.setMargins(dp(8), 0, 0, 0);
        row.addView(resume, resumeLp);

        TextView queue = downloadToolButton("Queue " + countActiveDownloads() + "/" + downloadMaxActive + " • " + countQueuedDownloads());
        queue.setOnClickListener(v -> showDownloadQueueSettingsDialog(activeDownloadDialog));
        LinearLayout.LayoutParams queueLp = new LinearLayout.LayoutParams(0, dp(38), 1);
        queueLp.setMargins(dp(8), 0, 0, 0);
        row.addView(queue, queueLp);

        return row;
    }

    private TextView downloadToolButton(String text) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setBackground(roundRect(Color.parseColor("#20232A"), dp(18), dp(1), COLOR_BORDER));
        return button;
    }

    private ArrayList<DownloadItem> getFilteredDownloadItems() {
        ArrayList<DownloadItem> result = new ArrayList<>();
        synchronized (downloadItems) {
            for (DownloadItem item : new ArrayList<>(downloadItems)) {
                if (!matchesDownloadCategory(item, activeDownloadCategory)) continue;
                if (!matchesDownloadSearch(item)) continue;
                result.add(item);
            }
        }

        if ("Antrian".equals(activeDownloadSort)) return result;

        Collections.sort(result, (a, b) -> {
            if ("Nama".equals(activeDownloadSort)) {
                return safeText(a.fileName).compareToIgnoreCase(safeText(b.fileName));
            }
            if ("Ukuran".equals(activeDownloadSort)) {
                long sa = getDownloadSize(a);
                long sb = getDownloadSize(b);
                return Long.compare(sb, sa);
            }
            return Integer.compare(b.id, a.id);
        });
        return result;
    }

    private boolean matchesDownloadSearch(DownloadItem item) {
        if (activeDownloadSearchQuery == null || activeDownloadSearchQuery.trim().length() == 0) return true;
        String q = activeDownloadSearchQuery.toLowerCase();
        return safeText(item.fileName).toLowerCase().contains(q)
                || safeText(item.url).toLowerCase().contains(q)
                || getDownloadHost(item).toLowerCase().contains(q);
    }

    private boolean matchesDownloadCategory(DownloadItem item, String category) {
        if ("Semua".equals(category)) return true;
        String type = getDownloadCategory(item);
        return category.equals(type);
    }

    private String getDownloadCategory(DownloadItem item) {
        String hinted = normalizeDetectedCategory(item.categoryHint);
        if (hinted.length() > 0) return hinted;
        String detected = inferDownloadCategoryFromData(item.fileName, item.url, "");
        item.categoryHint = detected;
        return detected;
    }

    private String normalizeDetectedCategory(String raw) {
        if (raw == null) return "";
        raw = raw.trim();
        if ("Video".equals(raw) || "APK".equals(raw) || "Dokumen".equals(raw) || "Musik".equals(raw) || "Lainnya".equals(raw)) {
            return raw;
        }
        return "";
    }

    private String inferDownloadCategoryFromData(String fileName, String url, String mimeType) {
        String mime = safeText(mimeType).toLowerCase(Locale.US);
        if (mime.startsWith("video/")) return "Video";
        if (mime.startsWith("audio/")) return "Musik";
        if (mime.equals("application/vnd.android.package-archive")) return "APK";
        if (mime.contains("pdf") || mime.contains("word") || mime.contains("excel") || mime.contains("powerpoint") || mime.startsWith("text/")) return "Dokumen";

        String combined = (safeText(fileName) + " " + safeText(url)).toLowerCase(Locale.US);
        if (combined.contains(".mp4") || combined.contains(".mkv") || combined.contains(".webm") || combined.contains(".avi") || combined.contains(".mov") || combined.contains(".3gp") || combined.contains(".m3u8")) return "Video";
        if (combined.contains(".apk") || combined.contains(".xapk") || combined.contains(".apks")) return "APK";
        if (combined.contains(".mp3") || combined.contains(".m4a") || combined.contains(".wav") || combined.contains(".ogg") || combined.contains(".flac")) return "Musik";
        if (combined.contains(".pdf") || combined.contains(".doc") || combined.contains(".docx") || combined.contains(".xls") || combined.contains(".xlsx") || combined.contains(".ppt") || combined.contains(".pptx") || combined.contains(".txt")) return "Dokumen";

        return "Lainnya";
    }

    private String getDownloadSectionTitle(int count) {
        String base = activeDownloadCategory;
        if (activeDownloadSearchQuery != null && activeDownloadSearchQuery.length() > 0) {
            base += " • hasil \"" + activeDownloadSearchQuery + "\"";
        }
        return base + " - " + count + " file";
    }

    private View downloadRow(DownloadItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        if (downloadSelectMode) {
            TextView check = new TextView(this);
            boolean selected = selectedDownloadIds.contains(item.id);
            check.setText(selected ? "✓" : "");
            check.setTextColor(Color.parseColor("#111111"));
            check.setTextSize(18);
            check.setTypeface(Typeface.DEFAULT_BOLD);
            check.setGravity(Gravity.CENTER);
            check.setBackground(roundRect(selected ? COLOR_ACCENT : Color.parseColor("#20232A"), dp(16), dp(1), selected ? COLOR_ACCENT : COLOR_BORDER));
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(32), dp(32));
            checkParams.setMargins(0, 0, dp(10), 0);
            row.addView(check, checkParams);
        }

        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMargins(0, 0, dp(8), 0);

        LinearLayout titleLine = new LinearLayout(this);
        titleLine.setOrientation(LinearLayout.HORIZONTAL);
        titleLine.setGravity(Gravity.CENTER_VERTICAL);

        TextView categoryBadge = new TextView(this);
        categoryBadge.setText(getDownloadCategory(item));
        categoryBadge.setTextColor(Color.parseColor("#111111"));
        categoryBadge.setTextSize(10);
        categoryBadge.setTypeface(Typeface.DEFAULT_BOLD);
        categoryBadge.setGravity(Gravity.CENTER);
        categoryBadge.setPadding(dp(8), 0, dp(8), 0);
        categoryBadge.setBackground(roundRect(COLOR_ACCENT, dp(10), 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(-2, dp(22));
        badgeParams.setMargins(0, 0, dp(8), 0);
        titleLine.addView(categoryBadge, badgeParams);

        TextView name = new TextView(this);
        name.setText(item.fileName);
        name.setTextColor(Color.WHITE);
        name.setTextSize(15);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        titleLine.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        textWrap.addView(titleLine);

        TextView sub = new TextView(this);
        sub.setText(getDownloadSubtitle(item));
        sub.setTextColor(COLOR_SUBTEXT);
        sub.setTextSize(12);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.setMargins(0, dp(3), 0, 0);
        textWrap.addView(sub, subParams);

        TextView engine = new TextView(this);
        engine.setText(getDownloadEngineVisibleText(item));
        engine.setTextColor(item.connectionCount >= 2 ? COLOR_ON : COLOR_ACCENT);
        engine.setTextSize(12);
        engine.setTypeface(Typeface.DEFAULT_BOLD);
        engine.setSingleLine(true);
        engine.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams engineParams = new LinearLayout.LayoutParams(-1, -2);
        engineParams.setMargins(0, dp(3), 0, 0);
        textWrap.addView(engine, engineParams);

        if ("running".equals(item.status) || "paused".equals(item.status) || "queued".equals(item.status)) {
            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress(item.progress);
            bar.setProgressDrawable(new ColorDrawable(COLOR_ACCENT));
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(-1, dp(4));
            barParams.setMargins(0, dp(8), 0, 0);
            textWrap.addView(bar, barParams);

            TextView percent = new TextView(this);
            percent.setText(item.progress + "% • " + readableSpeed(item.speedBytesPerSecond));
            percent.setTextColor(Color.parseColor("#C9CDD4"));
            percent.setTextSize(11);
            percent.setPadding(0, dp(4), 0, 0);
            textWrap.addView(percent);
        }

        row.addView(textWrap, textParams);

        if ("running".equals(item.status) || "paused".equals(item.status) || "queued".equals(item.status) || "failed".equals(item.status)) {
            TextView action = new TextView(this);
            if ("running".equals(item.status)) {
                action.setText("Ⅱ");
            } else if ("paused".equals(item.status) || "queued".equals(item.status)) {
                action.setText("▶");
            } else {
                action.setText("↻");
            }
            action.setTextColor(Color.WHITE);
            action.setTextSize(18);
            action.setTypeface(Typeface.DEFAULT_BOLD);
            action.setGravity(Gravity.CENTER);
            action.setMinWidth(dp(48));
            action.setMinHeight(dp(48));
            action.setPadding(dp(4), 0, dp(4), 0);
            action.setClickable(true);
            action.setFocusable(true);
            action.setHapticFeedbackEnabled(true);
            action.setBackground(roundRect(Color.parseColor("#20232A"), dp(22), dp(1), COLOR_BORDER));
            action.setOnClickListener(v -> handleDownloadPrimaryAction(item));
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(dp(48), dp(48));
            actionParams.setMargins(0, 0, dp(8), 0);
            row.addView(action, actionParams);
            action.bringToFront();
        }

        TextView more = new TextView(this);
        more.setText("⋮");
        more.setTextColor(Color.parseColor("#D6D9DE"));
        more.setTextSize(28);
        more.setGravity(Gravity.CENTER);
        more.setOnClickListener(v -> showDownloadItemMenu(v, item));
        row.addView(more, new LinearLayout.LayoutParams(dp(42), dp(52)));

        row.setOnClickListener(v -> {
            if (downloadSelectMode) {
                toggleDownloadSelection(item);
            } else if ("completed".equals(item.status)) {
                openDownloadedFile(item);
            } else if ("failed".equals(item.status) || "paused".equals(item.status) || "queued".equals(item.status)) {
                showDownloadItemMenu(v, item);
            } else {
                Toast.makeText(this, getConnectionLabel(item) + " • " + item.progress + "%", Toast.LENGTH_SHORT).show();
            }
        });

        row.setOnLongClickListener(v -> {
            downloadSelectMode = true;
            toggleDownloadSelection(item);
            return true;
        });

        return row;
    }

    private String getDownloadEngineVisibleText(DownloadItem item) {
        if (item == null) return "Mengecek koneksi";
        if ("queued".equals(item.status)) return item.engineInfo != null && item.engineInfo.length() > 0 ? item.engineInfo : "Antri • menunggu slot";
        if (item.hlsDownload) return "HLS/m3u8";
        if (item.connectionCount >= 4) return "4 koneksi sukses";
        if (item.connectionCount >= 2) return "2 koneksi sukses";
        if (item.connectionCount == 1) return "1 koneksi sukses";
        if ("paused".equals(item.status)) return "Dijeda";
        if ("failed".equals(item.status)) return "Gagal • klik ↻";
        return "Mengecek koneksi";
    }

    private String getDownloadSubtitle(DownloadItem item) {
        String size = readableFileSize(getDownloadSize(item));
        String host = getDownloadHost(item);
        String status;

        if ("running".equals(item.status)) {
            status = "download • " + item.progress + "%";
        } else if ("queued".equals(item.status)) {
            status = "antri • menunggu slot";
        } else if ("paused".equals(item.status)) {
            status = "dijeda • " + item.progress + "%";
        } else if ("failed".equals(item.status)) {
            status = item.failReason != null && item.failReason.length() > 0 ? "gagal • " + item.failReason : "terputus/gagal";
        } else {
            status = "selesai";
        }

        if (host.length() > 0) return size + " • " + status + " • " + host;
        return size + " • " + status;
    }

    private long getDownloadSize(DownloadItem item) {
        if (item.totalBytes > 0) return item.totalBytes;
        if (item.downloadedBytes > 0) return item.downloadedBytes;
        try {
            if (item.path != null) {
                File f = new File(item.path);
                if (f.exists()) return f.length();
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private String getDownloadHost(DownloadItem item) {
        try {
            if (item.url != null) {
                Uri uri = Uri.parse(item.url);
                String host = uri.getHost();
                return host == null ? "" : host;
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private void updateDownloadSpeed(DownloadItem item, long currentBytes) {
        if (item == null) return;
        long now = System.currentTimeMillis();

        if (item.lastSpeedTimeMs <= 0) {
            item.lastSpeedTimeMs = now;
            item.lastSpeedBytes = currentBytes;
            item.speedBytesPerSecond = 0;
            return;
        }

        long elapsed = now - item.lastSpeedTimeMs;
        if (elapsed < 800) return;

        long delta = currentBytes - item.lastSpeedBytes;
        if (delta < 0) delta = 0;

        item.speedBytesPerSecond = (delta * 1000.0) / Math.max(1, elapsed);
        item.lastSpeedTimeMs = now;
        item.lastSpeedBytes = currentBytes;
    }

    private String readableSpeed(double bytesPerSecond) {
        if (bytesPerSecond <= 0) return "0 KB/s";
        if (bytesPerSecond >= 1024 * 1024) {
            return String.format(java.util.Locale.US, "%.2f MB/s", bytesPerSecond / (1024.0 * 1024.0)).replace(".00", "");
        }
        if (bytesPerSecond >= 1024) {
            return String.format(java.util.Locale.US, "%.1f KB/s", bytesPerSecond / 1024.0).replace(".0", "");
        }
        return String.format(java.util.Locale.US, "%.0f B/s", bytesPerSecond);
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
            Toast.makeText(this, "Belum ada file dipilih", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selected.size() == 1) {
            shareDownloadedFile(selected.get(0));
            return;
        }
        Toast.makeText(this, "Bagikan multi-file akan disempurnakan setelah FileProvider aktif", Toast.LENGTH_LONG).show();
    }

    private void deleteSelectedDownloads() {
        ArrayList<DownloadItem> selected = getSelectedDownloads();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Belum ada file dipilih", Toast.LENGTH_SHORT).show();
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
    private int countActiveDownloads() {
        int count = 0;
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if ("running".equals(item.status)) count++;
            }
        }
        return count;
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
        if (item == null || out == null) return;
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

        if (!downloadQueuePaused && countActiveDownloads() < Math.max(2, downloadMaxActive)) {
            startQueuedDownloadNow(item);
        } else {
            showDownloadNotification(item, "Masuk antrian", true);
        }
    }

    private void startQueuedDownloadNow(DownloadItem item) {
        if (item == null) return;
        try {
            // Jangan start ulang item yang sudah running. Ini mencegah dobel thread/crash
            // saat tombol play diklik berkali-kali.
            if ("running".equals(item.status)) return;
            if (!"queued".equals(item.status) && !"paused".equals(item.status)) return;
            if (downloadQueueEnabled && "queued".equals(item.status) && countActiveDownloads() >= Math.max(2, downloadMaxActive)) return;

            File out = new File(item.path);
            item.status = "running";
            item.pauseRequested = false;
            item.speedBytesPerSecond = 0;
            item.lastSpeedTimeMs = 0;
            item.lastSpeedBytes = item.downloadedBytes;
            item.engineInfo = item.downloadedBytes > 0 ? "Melanjutkan koneksi" : "Mengecek koneksi";

            saveDownloadHistory();
            refreshDownloadPanel();
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
        while (countActiveDownloads() < Math.max(2, downloadMaxActive) && safety < 8) {
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
                    item.pauseRequested = true;
                    item.status = "paused";
                    item.speedBytesPerSecond = 0;
                    item.engineInfo = getConnectionLabel(item) + " sukses • dijeda";
                    showDownloadNotification(item, "Unduhan dijeda", false);
                } else if ("queued".equals(item.status)) {
                    item.status = "paused";
                    item.engineInfo = "Dijeda dari antrian";
                }
            }
        }
        saveDownloadHistory();
        refreshDownloadPanel();
        Toast.makeText(this, "Semua download dijeda", Toast.LENGTH_SHORT).show();
    }

    private void resumeAllDownloads() {
        downloadQueuePaused = false;
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if ("paused".equals(item.status)) {
                    item.status = "queued";
                    item.pauseRequested = false;
                    item.engineInfo = "Antri • menunggu slot";
                    item.speedBytesPerSecond = 0;
                }
            }
        }
        saveDownloadHistory();
        refreshDownloadPanel();
        Toast.makeText(this, "Semua download masuk antrian", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "File diprioritaskan", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, direction < 0 ? "Naik antrian" : "Turun antrian", Toast.LENGTH_SHORT).show();
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
                if (!downloadQueuePaused && countActiveDownloads() < Math.max(2, downloadMaxActive)) {
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
            Toast.makeText(this, "Aksi download gagal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        refreshDownloadPanel();
    }

    private String getConnectionLabel(DownloadItem item) {
        if (item == null) return "Premium Fast";
        if (item.hlsDownload) return "HLS";
        if (item.connectionCount >= 4) return "4 koneksi";
        if (item.connectionCount >= 2) return "2 koneksi";
        if (item.connectionCount == 1) return "1 koneksi";
        return "Premium Fast • anti-hotlink safe";
    }

    private void pauseDownloadItem(DownloadItem item) {
        if (item == null || !"running".equals(item.status)) return;
        item.pauseRequested = true;
        item.status = "paused";
        item.speedBytesPerSecond = 0;
        item.engineInfo = getConnectionLabel(item) + " sukses • dijeda";
        saveDownloadHistory();
        refreshDownloadPanel();
        showDownloadNotification(item, "Unduhan dijeda", false);
        Toast.makeText(this, "Unduhan dijeda", Toast.LENGTH_SHORT).show();
        mainHandler.postDelayed(this::pumpDownloadQueue, 650);
    }

    private void resumeDownloadItem(DownloadItem item) {
        if (item == null || !"paused".equals(item.status)) return;
        try {
            item.pauseRequested = false;
            item.speedBytesPerSecond = 0;
            item.lastSpeedTimeMs = 0;
            item.lastSpeedBytes = item.downloadedBytes;
            downloadQueuePaused = false;

            boolean slotAvailable = !downloadQueueEnabled || countActiveDownloads() < Math.max(2, downloadMaxActive);
            if (slotAvailable) {
                item.engineInfo = "Melanjutkan koneksi";
                saveDownloadHistory();
                refreshDownloadPanel();
                Toast.makeText(this, "Melanjutkan unduhan", Toast.LENGTH_SHORT).show();
                startQueuedDownloadNow(item);
            } else {
                item.status = "queued";
                item.engineInfo = "Antri • menunggu slot";
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, "Masuk antrian", true);
                Toast.makeText(this, "Slot penuh, unduhan masuk antrian", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            failDownload(item, e.getMessage());
        }
    }

    private void reloadDownloadItem(DownloadItem item) {
        if (item == null) return;
        try {
            item.pauseRequested = false;
            item.status = "queued";
            item.progress = 0;
            item.downloadedBytes = 0;
            item.totalBytes = 0;
            item.connectionCount = 0;
            item.part1Start = 0;
            item.part1End = 0;
            item.part1Done = 0;
            item.part2Start = 0;
            item.part2End = 0;
            item.part2Done = 0;
            item.speedBytesPerSecond = 0;
            item.lastSpeedTimeMs = 0;
            item.lastSpeedBytes = 0;
            item.engineInfo = "Mengecek koneksi";
            item.retryCount = 0;

            File out = new File(item.path);
            if (out.exists()) {
                out.delete();
            }

            saveDownloadHistory();
            refreshDownloadPanel();
            showDownloadNotification(item, "Masuk antrian reload", true);
            Toast.makeText(this, "Download dimulai ulang dan masuk antrian", Toast.LENGTH_SHORT).show();

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

        if ("completed".equals(item.status)) {
            popup.getMenu().add(0, 1, 1, "Open");
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

    private void shareDownloadedFile(DownloadItem item) {
        try {
            Uri uri = getBestDownloadUri(item);
            if (uri == null) {
                Toast.makeText(this, "File tidak ditemukan", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Gagal membagikan file", Toast.LENGTH_SHORT).show();
        }
    }

    private void renameDownloadedFile(DownloadItem item) {
        File currentFile = new File(item.path);
        if (!currentFile.exists()) {
            Toast.makeText(this, "File tidak ditemukan", Toast.LENGTH_SHORT).show();
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
                    if (newName.length() == 0) {
                        Toast.makeText(this, "Nama file kosong", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    File newFile = new File(currentFile.getParentFile(), newName);
                    if (newFile.exists()) {
                        Toast.makeText(this, "Nama file sudah ada", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (currentFile.renameTo(newFile)) {
                        item.fileName = newName;
                        item.path = newFile.getAbsolutePath();
                        saveDownloadHistory();
                        renderDownloadList();
                        Toast.makeText(this, "Nama file diperbarui", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Gagal mengganti nama", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void removeDownloadItem(DownloadItem item, boolean deleteFile) {
        if (item == null) return;

        // Kalau item download dihapus, notifikasi harus ikut hilang.
        // Untuk download berjalan/paused, minta thread berhenti juga.
        item.pauseRequested = true;
        item.status = "removed";
        cancelDownloadNotification(item);

        synchronized (downloadItems) {
            downloadItems.remove(item);
        }

        if (deleteFile && item.publicUri != null && item.publicUri.length() > 0) {
            try {
                getContentResolver().delete(Uri.parse(item.publicUri), null, null);
            } catch (Exception ignored) {}
        }

        if (deleteFile && item.path != null) {
            try {
                File f = new File(item.path);
                if (f.exists()) f.delete();
            } catch (Exception ignored) {}
        }

        selectedDownloadIds.remove(item.id);
        saveDownloadHistory();
        renderDownloadList();
        Toast.makeText(this, deleteFile ? "File + riwayat dihapus" : "Riwayat dihapus", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "File tidak ditemukan", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Tidak ada aplikasi untuk membuka file ini", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshDownloadPanel() {
        runOnUiThread(() -> renderDownloadList());
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
    private void beginDownloadFromWeb(String url, String contentDisposition, String mimeType) {
        beginDownloadFromWeb(url, contentDisposition, mimeType, webView != null ? webView.getSettings().getUserAgentString() : "");
    }

    private void beginDownloadFromWeb(String url, String contentDisposition, String mimeType, String userAgent) {
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        beginDownload(url, fileName, userAgent, getCurrentPageForReferer());
    }

    private void beginDownload(String fileUrl, String guessedFileName, String userAgent, String referer) {
        try {
            String fileName = guessedFileName;
            if (fileName == null || fileName.trim().length() == 0) {
                fileName = URLUtil.guessFileName(fileUrl, null, null);
            }
            if (fileName == null || fileName.trim().length() == 0) {
                fileName = "yield_download_" + System.currentTimeMillis() + ".bin";
            }

            File dir = getDownloadDirectory();
            if (!dir.exists()) dir.mkdirs();

            fileName = autoRenameDownloadFile(fileName, fileUrl, "");
            File out = uniqueFile(new File(dir, fileName));
            DownloadItem item = new DownloadItem(nextDownloadId++, fileUrl, out.getName(), out.getAbsolutePath(), "queued", 0);
            item.connectionCount = 0;
            item.speedBytesPerSecond = 0;
            item.lastSpeedTimeMs = 0;
            item.lastSpeedBytes = 0;
            item.engineInfo = "Mengecek koneksi";
            item.userAgent = userAgent == null ? "" : userAgent;
            item.referer = referer == null ? "" : referer;
            item.failReason = "";
            item.retryCount = 0;
            item.categoryHint = inferDownloadCategoryFromData(fileName, fileUrl, "");

            synchronized (downloadItems) {
                downloadItems.add(item);
            }
            saveDownloadHistory();
            refreshDownloadPanel();
            showDownloadNotification(item, "Masuk antrian", true);

            Toast.makeText(this, "Unduhan dimulai. Membuka menu Download.", Toast.LENGTH_SHORT).show();
            mainHandler.postDelayed(() -> {
                if (activeDownloadDialog == null || !activeDownloadDialog.isShowing()) {
                    showDownloadManager();
                } else {
                    renderDownloadList();
                }
            }, 250);

            startTwoConnectionDownload(item, out);
        } catch (Exception e) {
            Toast.makeText(this, "Gagal memulai unduhan: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        int i = 1;
        File candidate;
        do {
            candidate = new File(parent, base + " (" + i + ")" + ext);
            i++;
        } while (candidate.exists());
        return candidate;
    }

    private String getOriginFromUrl(String value) {
        try {
            if (value == null || value.length() == 0) return "";
            Uri uri = Uri.parse(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) return "";
            int port = uri.getPort();
            if (port > 0) return scheme + "://" + host + ":" + port;
            return scheme + "://" + host;
        } catch (Exception e) {
            return "";
        }
    }

    private String getRootUrl(String value) {
        String origin = getOriginFromUrl(value);
        return origin.length() > 0 ? origin + "/" : "";
    }

    private String buildAntiHotlinkCookieHeader(String fileUrl, DownloadItem item) {
        try {
            LinkedHashSet<String> cookies = new LinkedHashSet<>();

            String[] sources = new String[]{
                    fileUrl,
                    item != null ? item.referer : "",
                    getRootUrl(fileUrl),
                    item != null ? getRootUrl(item.referer) : "",
                    "https://gofile.io/",
                    "https://www.gofile.io/"
            };

            for (String source : sources) {
                if (source == null || source.length() == 0) continue;
                String cookie = CookieManager.getInstance().getCookie(source);
                if (cookie == null || cookie.trim().length() == 0) continue;
                String[] parts = cookie.split(";");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.length() > 0) cookies.add(trimmed);
                }
            }

            StringBuilder sb = new StringBuilder();
            for (String cookie : cookies) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(cookie);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private HttpURLConnection openDownloadConnection(String url, DownloadItem item, String range) throws Exception {
        String current = url;
        for (int redirect = 0; redirect < 6; redirect++) {
            HttpURLConnection conn = (HttpURLConnection) new URL(current).openConnection();
            conn.setInstanceFollowRedirects(false);
            if (range != null && range.length() > 0) {
                conn.setRequestProperty("Range", range);
            }
            applyDownloadHeaders(conn, current, item);
            conn.connect();

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == HttpURLConnection.HTTP_SEE_OTHER ||
                    code == 307 ||
                    code == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.length() == 0) {
                    throw new Exception("Redirect tanpa lokasi");
                }
                URL base = new URL(current);
                current = new URL(base, location).toString();
                continue;
            }
            return conn;
        }
        throw new Exception("Terlalu banyak redirect");
    }

    private void validateDownloadResponse(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        if (code >= 400) {
            throw new Exception("Server menolak unduhan HTTP " + code);
        }
        String contentType = conn.getContentType();
        if (contentType != null) {
            String lower = contentType.toLowerCase();
            if (lower.contains("text/html") && conn.getContentLengthLong() > 0 && conn.getContentLengthLong() < 1024 * 1024) {
                throw new Exception("Link mengarah ke halaman HTML, bukan file. Coba klik tombol download asli di halaman.");
            }
        }
    }

    private void applyDownloadHeaders(HttpURLConnection conn, String fileUrl) {
        applyDownloadHeaders(conn, fileUrl, null);
    }

    private void applyDownloadHeaders(HttpURLConnection conn, String fileUrl, DownloadItem item) {
        try {
            conn.setConnectTimeout(DOWNLOAD_CONNECT_TIMEOUT);
            conn.setReadTimeout(DOWNLOAD_READ_TIMEOUT);
            conn.setUseCaches(false);

            String ua = item != null ? item.userAgent : "";
            if (ua == null || ua.length() == 0) {
                ua = webView != null ? webView.getSettings().getUserAgentString() : null;
            }
            if (ua == null || ua.length() == 0) {
                ua = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36 YieldBrowser";
            }

            String referer = item != null ? item.referer : "";
            if (referer == null || referer.length() == 0) referer = getCurrentPageForReferer();
            String origin = getOriginFromUrl(referer);
            if (origin.length() == 0) origin = getOriginFromUrl(fileUrl);

            conn.setRequestProperty("User-Agent", ua);
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("Pragma", "no-cache");

            // Anti-hotlink protection: make the request look like it came from the real page.
            if (referer != null && referer.length() > 0) conn.setRequestProperty("Referer", referer);
            if (origin != null && origin.length() > 0) conn.setRequestProperty("Origin", origin);
            conn.setRequestProperty("Sec-Fetch-Dest", "empty");
            conn.setRequestProperty("Sec-Fetch-Mode", "cors");
            conn.setRequestProperty("Sec-Fetch-Site", "same-origin");
            conn.setRequestProperty("X-Requested-With", getPackageName());

            String cookie = buildAntiHotlinkCookieHeader(fileUrl, item);
            if (cookie != null && cookie.length() > 0) {
                conn.setRequestProperty("Cookie", cookie);
            }
        } catch (Exception ignored) {
        }
    }

    private void startTwoConnectionDownload(DownloadItem item, File out) {
        if (item == null || out == null) return;

        if (downloadHlsEnabled && looksLikeHlsDownload(item.url, item.fileName)) {
            startHlsDownload(item, out);
            return;
        }

        boolean resumeAttempt = out.exists() && item.downloadedBytes > 0 && ("running".equals(item.status) || "paused".equals(item.status));
        if (resumeAttempt && item.connectionCount >= 4) {
            startDynamicMultiConnectionDownload(item, out);
            return;
        }
        if (resumeAttempt || !downloadDynamic4Connections) {
            startLegacyTwoConnectionDownload(item, out);
            return;
        }

        startDynamicMultiConnectionDownload(item, out);
    }

    private boolean looksLikeHlsDownload(String url, String fileName) {
        String u = (url == null ? "" : url).toLowerCase(Locale.US);
        String n = (fileName == null ? "" : fileName).toLowerCase(Locale.US);
        return u.contains(".m3u8") || n.endsWith(".m3u8") || u.contains("mpegurl");
    }

    private boolean looksLikeVideoDownload(String url, String fileName, String contentType) {
        String u = (url == null ? "" : url).toLowerCase(Locale.US);
        String n = (fileName == null ? "" : fileName).toLowerCase(Locale.US);
        String c = (contentType == null ? "" : contentType).toLowerCase(Locale.US);
        return c.startsWith("video/") || c.contains("mpegurl") || u.contains(".m3u8") ||
                n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".webm") || n.endsWith(".avi") ||
                n.endsWith(".mov") || n.endsWith(".ts") || n.endsWith(".m3u8") ||
                u.contains(".mp4") || u.contains(".mkv") || u.contains(".webm") || u.contains(".ts");
    }

    private String autoRenameDownloadFile(String fileName, String url, String contentType) {
        String name = fileName == null ? "" : fileName.trim();
        if (name.length() == 0) name = "yield_download_" + System.currentTimeMillis();

        if (looksLikeHlsDownload(url, name)) {
            if (name.toLowerCase(Locale.US).endsWith(".m3u8")) name = name.substring(0, name.length() - 5) + ".ts";
            else if (!name.toLowerCase(Locale.US).endsWith(".ts")) name = name + ".ts";
        }

        if (looksLikeVideoDownload(url, name, contentType)) {
            name = name.replaceAll("(?i)videoplayback(\\.[a-z0-9]+)?$", "yield_video$1");
            if (!name.contains(".")) name = name + ".mp4";
        }

        return name.replace("/", "_").replace("\\", "_").replace(":", "_");
    }

    private void setDynamicPartState(DownloadItem item, int part, long start, long end, long done) {
        if (item == null) return;
        if (part == 1) {
            item.part1Start = start;
            item.part1End = end;
            item.part1Done = done;
        } else if (part == 2) {
            item.part2Start = start;
            item.part2End = end;
            item.part2Done = done;
        } else if (part == 3) {
            item.part3Start = start;
            item.part3End = end;
            item.part3Done = done;
        } else if (part == 4) {
            item.part4Start = start;
            item.part4End = end;
            item.part4Done = done;
        }
    }

    private long getDynamicPartStart(DownloadItem item, int part) {
        if (item == null) return 0;
        if (part == 1) return item.part1Start;
        if (part == 2) return item.part2Start;
        if (part == 3) return item.part3Start;
        if (part == 4) return item.part4Start;
        return 0;
    }

    private long getDynamicPartEnd(DownloadItem item, int part) {
        if (item == null) return -1;
        if (part == 1) return item.part1End;
        if (part == 2) return item.part2End;
        if (part == 3) return item.part3End;
        if (part == 4) return item.part4End;
        return -1;
    }

    private long getDynamicPartDone(DownloadItem item, int part) {
        if (item == null) return 0;
        if (part == 1) return item.part1Done;
        if (part == 2) return item.part2Done;
        if (part == 3) return item.part3Done;
        if (part == 4) return item.part4Done;
        return 0;
    }

    private void addDynamicPartDone(DownloadItem item, int part, long delta) {
        if (item == null || delta <= 0) return;
        if (part == 1) item.part1Done += delta;
        else if (part == 2) item.part2Done += delta;
        else if (part == 3) item.part3Done += delta;
        else if (part == 4) item.part4Done += delta;
    }

    private void clearDynamicPartState(DownloadItem item) {
        if (item == null) return;
        item.part1Start = 0;
        item.part1End = 0;
        item.part1Done = 0;
        item.part2Start = 0;
        item.part2End = 0;
        item.part2Done = 0;
        item.part3Start = 0;
        item.part3End = 0;
        item.part3Done = 0;
        item.part4Start = 0;
        item.part4End = 0;
        item.part4Done = 0;
    }

    private boolean hasDynamicResumeState(DownloadItem item, int connections, long total) {
        if (item == null || connections < 3 || total <= 0) return false;
        if (item.connectionCount != connections || item.totalBytes != total) return false;
        for (int part = 1; part <= connections; part++) {
            long start = getDynamicPartStart(item, part);
            long end = getDynamicPartEnd(item, part);
            long done = getDynamicPartDone(item, part);
            long len = end - start + 1;
            if (end < start || len <= 0 || done < 0 || done > len) return false;
        }
        return getDynamicPartDone(item, 1) + getDynamicPartDone(item, 2) + getDynamicPartDone(item, 3) + getDynamicPartDone(item, 4) > 0;
    }

    private void clampDynamicPartDone(DownloadItem item, int connections) {
        if (item == null) return;
        for (int part = 1; part <= connections; part++) {
            long start = getDynamicPartStart(item, part);
            long end = getDynamicPartEnd(item, part);
            long done = getDynamicPartDone(item, part);
            long len = Math.max(0, end - start + 1);
            if (done < 0) done = 0;
            if (done > len) done = len;
            setDynamicPartState(item, part, start, end, done);
        }
    }

    private void startDynamicMultiConnectionDownload(DownloadItem item, File out) {
        item.status = "running";
        item.pauseRequested = false;
        item.engineInfo = item.downloadedBytes > 0 ? "Mengecek resume 4 koneksi" : "Mengecek 2/4 koneksi";
        refreshDownloadPanel();

        new Thread(() -> {
            HttpURLConnection head = null;
            try {
                final File[] outRef = new File[]{out};
                boolean resumeCandidate = out.exists() && item.downloadedBytes > 0 && item.connectionCount >= 4;

                head = openDownloadConnection(item.url, item, "bytes=0-0");
                validateDownloadResponse(head);

                int responseCode = head.getResponseCode();
                String contentRange = head.getHeaderField("Content-Range");
                long total = parseTotalSize(contentRange);
                if (total <= 0) total = head.getContentLengthLong();

                String cd = head.getHeaderField("Content-Disposition");
                try {
                    String betterName = autoRenameDownloadFile(URLUtil.guessFileName(item.url, cd, head.getContentType()), item.url, head.getContentType());
                    if (!resumeCandidate && betterName != null && betterName.length() > 0 && !betterName.equals(item.fileName)) {
                        File newFile = uniqueFile(new File(outRef[0].getParentFile(), betterName));
                        item.fileName = newFile.getName();
                        item.path = newFile.getAbsolutePath();
                        outRef[0] = newFile;
                    }
                    item.categoryHint = inferDownloadCategoryFromData(item.fileName, item.url, head.getContentType());
                } catch (Exception ignored) {}

                boolean rangeOk = responseCode == 206 && total > 1024 * 1024 * 2;
                if (head != null) head.disconnect();

                if (!rangeOk) {
                    if (resumeCandidate) {
                        item.status = "failed";
                        item.pauseRequested = false;
                        item.engineInfo = "Server tidak mendukung resume Range";
                        item.failReason = "Server menolak resume. Gunakan reload untuk mulai ulang.";
                        saveDownloadHistory();
                        refreshDownloadPanel();
                        showDownloadNotification(item, "Server menolak resume", false);
                        runOnUiThread(() -> Toast.makeText(this, "Server menolak resume. Tekan reload untuk mulai ulang.", Toast.LENGTH_LONG).show());
                        mainHandler.postDelayed(this::pumpDownloadQueue, 700);
                        return;
                    }
                    startLegacyTwoConnectionDownload(item, outRef[0]);
                    return;
                }

                int connections = total >= 32L * 1024L * 1024L ? DOWNLOAD_CONNECTIONS_DYNAMIC_MAX : DOWNLOAD_CONNECTIONS_PREMIUM;
                if (connections <= 2) {
                    startLegacyTwoConnectionDownload(item, outRef[0]);
                    return;
                }

                final long finalTotal = total;
                final int finalConnections = connections;
                final File finalOutFile = outRef[0];

                boolean canResumeDynamic = resumeCandidate
                        && outRef[0].exists()
                        && hasDynamicResumeState(item, finalConnections, finalTotal);

                long chunk = finalTotal / finalConnections;

                if (!canResumeDynamic) {
                    if (resumeCandidate) {
                        // Download 4 koneksi dari versi lama belum punya posisi per-part.
                        // Untuk mencegah file korup, mulai ulang 4 koneksi secara jelas.
                        runOnUiThread(() -> Toast.makeText(this, "Resume 4 koneksi lama belum punya data part. Mulai ulang 4 koneksi.", Toast.LENGTH_LONG).show());
                    }

                    clearDynamicPartState(item);
                    item.totalBytes = finalTotal;
                    item.connectionCount = finalConnections;
                    item.engineInfo = finalConnections + " koneksi sukses";
                    item.downloadedBytes = 0;
                    item.progress = 0;
                    item.retryCount = 0;

                    for (int i = 0; i < finalConnections; i++) {
                        int part = i + 1;
                        long start = i * chunk;
                        long end = i == finalConnections - 1 ? finalTotal - 1 : ((i + 1) * chunk) - 1;
                        setDynamicPartState(item, part, start, end, 0);
                    }

                    try {
                        RandomAccessFile raf = new RandomAccessFile(finalOutFile, "rw");
                        raf.setLength(finalTotal);
                        raf.close();
                    } catch (Exception ignored) {}
                } else {
                    clampDynamicPartDone(item, finalConnections);
                    long resumed = 0;
                    for (int part = 1; part <= finalConnections; part++) {
                        resumed += getDynamicPartDone(item, part);
                    }
                    item.totalBytes = finalTotal;
                    item.connectionCount = finalConnections;
                    item.engineInfo = "Resume " + finalConnections + " koneksi";
                    item.downloadedBytes = resumed;
                    item.progress = (int) Math.min(99, (resumed * 100) / Math.max(1, finalTotal));
                    runOnUiThread(() -> Toast.makeText(this, "Melanjutkan " + finalConnections + " koneksi dari " + item.progress + "%", Toast.LENGTH_SHORT).show());
                }

                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, item.engineInfo, true);

                final long[] done = new long[]{item.downloadedBytes};
                final boolean[] ok = new boolean[]{true};
                ArrayList<Thread> threads = new ArrayList<>();

                for (int i = 0; i < finalConnections; i++) {
                    final int part = i + 1;
                    final long start = getDynamicPartStart(item, part) + getDynamicPartDone(item, part);
                    final long end = getDynamicPartEnd(item, part);
                    if (start > end) continue;
                    Thread t = new Thread(() -> downloadRangeDynamic(item, finalOutFile, start, end, done, finalTotal, ok, part, finalConnections));
                    threads.add(t);
                    t.start();
                }

                for (Thread t : threads) t.join();

                if (item.pauseRequested || "paused".equals(item.status)) {
                    item.status = "paused";
                    item.speedBytesPerSecond = 0;
                    item.engineInfo = finalConnections + " koneksi sukses • dijeda";
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    showDownloadNotification(item, "Unduhan dijeda", false);
                    return;
                }

                if (ok[0]) completeDownload(item);
                else {
                    if (resumeCandidate) {
                        failDownload(item, "Resume 4 koneksi terputus");
                        return;
                    }
                    if (outRef[0].exists()) outRef[0].delete();
                    item.downloadedBytes = 0;
                    item.progress = 0;
                    item.connectionCount = 0;
                    clearDynamicPartState(item);
                    item.engineInfo = "Fallback 2 koneksi";
                    startLegacyTwoConnectionDownload(item, outRef[0]);
                }
            } catch (Exception e) {
                if (head != null) try { head.disconnect(); } catch (Exception ignored) {}
                if (out.exists() && item.downloadedBytes > 0 && item.connectionCount >= 4) {
                    failDownload(item, "Resume 4 koneksi gagal: " + e.getMessage());
                } else {
                    startLegacyTwoConnectionDownload(item, out);
                }
            }
        }).start();
    }

    private void downloadRangeDynamic(DownloadItem item, File out, long start, long end, long[] done, long total, boolean[] ok, int partIndex, int connections) {
        if (start > end) return;
        HttpURLConnection conn = null;
        InputStream in = null;
        RandomAccessFile raf = null;
        try {
            conn = openDownloadConnection(item.url, item, "bytes=" + start + "-" + end);
            validateDownloadResponse(conn);
            in = conn.getInputStream();
            raf = new RandomAccessFile(out, "rw");
            raf.seek(start);
            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) != -1) {
                if (item.pauseRequested || "paused".equals(item.status)) {
                    ok[0] = false;
                    break;
                }
                raf.write(buffer, 0, len);
                applySpeedLimit(len, connections);
                synchronized (done) {
                    done[0] += len;
                    addDynamicPartDone(item, partIndex, len);
                    item.downloadedBytes = done[0];
                    updateDownloadSpeed(item, done[0]);
                    int percent = (int) Math.min(99, (done[0] * 100) / Math.max(1, total));
                    if (percent != item.progress) {
                        item.progress = percent;
                        if (percent % 2 == 0 || percent >= 99) {
                            refreshDownloadPanel();
                            showDownloadNotification(item, connections + " koneksi • " + percent + "% • " + readableSpeed(item.speedBytesPerSecond), true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            ok[0] = false;
        } finally {
            try { if (raf != null) raf.close(); } catch (Exception ignored) {}
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.disconnect(); } catch (Exception ignored) {}
        }
    }

    private void startLegacyTwoConnectionDownload(DownloadItem item, File out) {
        boolean resumeAttempt = out.exists() && item.downloadedBytes > 0 && ("running".equals(item.status) || "paused".equals(item.status));
        item.status = "running";
        item.pauseRequested = false;
        item.engineInfo = "Mengecek koneksi";
        refreshDownloadPanel();

        new Thread(() -> {
            try {
                HttpURLConnection head = null;
                try {
                    head = openDownloadConnection(item.url, item, "bytes=0-0");
                    validateDownloadResponse(head);

                    int response = head.getResponseCode();
                    String contentRange = head.getHeaderField("Content-Range");
                    long total = parseTotalSize(contentRange);
                    try {
                        String cd = head.getHeaderField("Content-Disposition");
                        String betterName = URLUtil.guessFileName(item.url, cd, head.getContentType());
                        if (betterName != null && betterName.length() > 0) {
                            item.fileName = betterName;
                        }
                        item.categoryHint = inferDownloadCategoryFromData(item.fileName, item.url, head.getContentType());
                    } catch (Exception ignored) {}

                    if (item.pauseRequested || "paused".equals(item.status)) {
                        head.disconnect();
                        return;
                    }

                    if (response == 206 && total > 1) {
                        head.disconnect();
                        item.connectionCount = DOWNLOAD_CONNECTIONS_PREMIUM;
                        item.engineInfo = "2 koneksi sukses";
                        item.totalBytes = total;
                        item.failReason = "";

                        long mid = total / 2;
                        boolean canResumeSplit = resumeAttempt
                                && out.exists()
                                && item.totalBytes == total
                                && item.part1End == mid
                                && item.part2Start == mid + 1
                                && (item.part1Done > 0 || item.part2Done > 0);

                        if (!canResumeSplit) {
                            item.part1Start = 0;
                            item.part1End = mid;
                            item.part1Done = 0;
                            item.part2Start = mid + 1;
                            item.part2End = total - 1;
                            item.part2Done = 0;
                            item.downloadedBytes = 0;
                            item.progress = 0;
                            RandomAccessFile raf = new RandomAccessFile(out, "rw");
                            raf.setLength(total);
                            raf.close();
                        } else {
                            long p1Len = item.part1End - item.part1Start + 1;
                            long p2Len = item.part2End - item.part2Start + 1;
                            if (item.part1Done > p1Len) item.part1Done = p1Len;
                            if (item.part2Done > p2Len) item.part2Done = p2Len;
                            item.downloadedBytes = item.part1Done + item.part2Done;
                            item.progress = (int) Math.min(99, (item.downloadedBytes * 100) / total);
                        }

                        saveDownloadHistory();
                        refreshDownloadPanel();
                        showDownloadNotification(item, "2 koneksi", true);

                        long[] done = new long[]{item.part1Done + item.part2Done};
                        boolean[] ok = new boolean[]{true};

                        long range1Start = item.part1Start + item.part1Done;
                        long range2Start = item.part2Start + item.part2Done;

                        Thread t1 = new Thread(() -> downloadRange(item, out, range1Start, item.part1End, done, total, ok, 1));
                        Thread t2 = new Thread(() -> downloadRange(item, out, range2Start, item.part2End, done, total, ok, 2));
                        t1.start();
                        t2.start();
                        t1.join();
                        t2.join();

                        if (item.pauseRequested || "paused".equals(item.status)) {
                            saveDownloadHistory();
                            refreshDownloadPanel();
                            showDownloadNotification(item, "Unduhan dijeda", false);
                            return;
                        }

                        if (ok[0]) completeDownload(item);
                        else {
                            item.connectionCount = 1;
                            item.engineInfo = "1 koneksi sukses";
                            item.progress = 0;
                            item.downloadedBytes = 0;
                            item.part1Done = 0;
                            item.part2Done = 0;
                            if (out.exists()) out.delete();
                            saveDownloadHistory();
                            refreshDownloadPanel();
                            downloadSingle(item, out);
                        }
                    } else {
                        if (head != null) head.disconnect();
                        item.connectionCount = 1;
                        item.engineInfo = "1 koneksi sukses";
                        saveDownloadHistory();
                        refreshDownloadPanel();
                        downloadSingle(item, out);
                    }
                } catch (Exception splitError) {
                    if (head != null) try { head.disconnect(); } catch (Exception ignored) {}
                    item.connectionCount = 1;
                    item.engineInfo = "1 koneksi sukses";
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    downloadSingle(item, out);
                }
            } catch (Exception e) {
                if (item.pauseRequested || "paused".equals(item.status)) {
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    showDownloadNotification(item, "Unduhan dijeda", false);
                } else {
                    failDownload(item, e.getMessage());
                }
            }
        }).start();
    }

    private void prefetchHlsSegmentsForDownload(ArrayList<String> segments, DownloadItem item) {
        if (!hlsSegmentPrefetch || segments == null || segments.isEmpty()) return;
        int max = Math.min(4, segments.size());
        for (int i = 0; i < max; i++) {
            final String u = segments.get(i);
            new Thread(() -> {
                HttpURLConnection conn = null;
                InputStream in = null;
                try {
                    conn = openDownloadConnection(u, item, "");
                    validateDownloadResponse(conn);
                    in = conn.getInputStream();
                    byte[] buffer = new byte[16 * 1024];
                    int read = 0;
                    while (read < 64 * 1024) {
                        int len = in.read(buffer);
                        if (len <= 0) break;
                        read += len;
                    }
                } catch (Exception ignored) {
                } finally {
                    try { if (in != null) in.close(); } catch (Exception ignored) {}
                    try { if (conn != null) conn.disconnect(); } catch (Exception ignored) {}
                }
            }).start();
        }
    }

    private void startHlsDownload(DownloadItem item, File out) {
        item.status = "running";
        item.pauseRequested = false;
        item.hlsDownload = true;
        item.connectionCount = 1;
        item.engineInfo = "HLS/m3u8";
        item.categoryHint = "Video";
        if (!item.fileName.toLowerCase(Locale.US).endsWith(".ts")) {
            File renamed = uniqueFile(new File(out.getParentFile(), autoRenameDownloadFile(item.fileName, item.url, "application/vnd.apple.mpegurl")));
            item.fileName = renamed.getName();
            item.path = renamed.getAbsolutePath();
            out = renamed;
        }
        refreshDownloadPanel();

        new Thread(() -> {
            try {
                ArrayList<String> segments = resolveHlsSegments(item.url, item);
                if (segments.isEmpty()) throw new Exception("Playlist HLS kosong/tidak didukung");

                prefetchHlsSegmentsForDownload(segments, item);

                item.totalBytes = segments.size();
                item.downloadedBytes = 0;
                item.progress = 0;
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, "HLS • " + segments.size() + " segmen", true);

                FileOutputStream fos = new FileOutputStream(new File(item.path), false);
                byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];

                for (int i = 0; i < segments.size(); i++) {
                    if (item.pauseRequested || "paused".equals(item.status)) break;
                    HttpURLConnection conn = openDownloadConnection(segments.get(i), item, "");
                    validateDownloadResponse(conn);
                    InputStream in = conn.getInputStream();
                    int len;
                    long segmentBytes = 0;
                    while ((len = in.read(buffer)) != -1) {
                        if (item.pauseRequested || "paused".equals(item.status)) break;
                        fos.write(buffer, 0, len);
                        segmentBytes += len;
                        applySpeedLimit(len, 1);
                        updateDownloadSpeed(item, segmentBytes);
                    }
                    in.close();
                    conn.disconnect();
                    item.downloadedBytes = i + 1;
                    item.progress = (int) Math.min(99, ((i + 1) * 100.0) / Math.max(1, segments.size()));
                    refreshDownloadPanel();
                    if (i % 3 == 0 || i == segments.size() - 1) {
                        showDownloadNotification(item, "HLS • " + item.progress + "% • " + readableSpeed(item.speedBytesPerSecond), true);
                    }
                }
                fos.close();

                if (item.pauseRequested || "paused".equals(item.status)) {
                    item.status = "paused";
                    item.engineInfo = "HLS dijeda";
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    showDownloadNotification(item, "HLS dijeda", false);
                } else {
                    completeDownload(item);
                }
            } catch (Exception e) {
                failDownload(item, e.getMessage());
            }
        }).start();
    }

    private ArrayList<String> resolveHlsSegments(String playlistUrl, DownloadItem item) throws Exception {
        String text = readUrlText(playlistUrl, item);
        String base = getBaseUrl(playlistUrl);
        ArrayList<String> variants = new ArrayList<>();
        ArrayList<String> segments = new ArrayList<>();
        String[] lines = text.split("\\n");
        boolean nextVariant = false;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.length() == 0) continue;
            if (line.startsWith("#EXT-X-STREAM-INF")) { nextVariant = true; continue; }
            if (nextVariant && !line.startsWith("#")) { variants.add(resolveUrl(base, line)); nextVariant = false; continue; }
            nextVariant = false;
            if (!line.startsWith("#")) segments.add(resolveUrl(base, line));
        }
        if (!variants.isEmpty()) return resolveHlsSegments(variants.get(variants.size() - 1), item);
        return segments;
    }

    private String readUrlText(String url, DownloadItem item) throws Exception {
        HttpURLConnection conn = openDownloadConnection(url, item, "");
        validateDownloadResponse(conn);
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        reader.close();
        conn.disconnect();
        return sb.toString();
    }

    private String getBaseUrl(String url) {
        try {
            int idx = url.lastIndexOf('/');
            if (idx >= 0) return url.substring(0, idx + 1);
        } catch (Exception ignored) {}
        return url;
    }

    private String resolveUrl(String base, String value) {
        try { return new URL(new URL(base), value).toString(); }
        catch (Exception e) { return value; }
    }

    private void applySpeedLimit(int bytesRead, int activeConnections) {
        try {
            if (downloadSpeedLimitKBps <= 0 || bytesRead <= 0) return;
            int connections = Math.max(1, activeConnections);
            double bytesPerSecond = downloadSpeedLimitKBps * 1024.0;
            long sleep = (long) ((bytesRead * 1000.0 * connections) / bytesPerSecond);
            if (sleep > 0) Thread.sleep(Math.min(350, sleep));
        } catch (Exception ignored) {}
    }

    private void downloadRange(DownloadItem item, File out, long start, long end, long[] done, long total, boolean[] ok, int partIndex) {
        if (start > end) return;
        try {
            HttpURLConnection conn = openDownloadConnection(item.url, item, "bytes=" + start + "-" + end);
            validateDownloadResponse(conn);

            InputStream in = conn.getInputStream();
            RandomAccessFile raf = new RandomAccessFile(out, "rw");
            raf.seek(start);

            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) != -1) {
                if (item.pauseRequested || "paused".equals(item.status)) {
                    ok[0] = false;
                    break;
                }
                raf.write(buffer, 0, len);
                applySpeedLimit(len, Math.max(1, item.connectionCount));
                synchronized (done) {
                    done[0] += len;
                    if (partIndex == 1) item.part1Done += len;
                    else item.part2Done += len;
                    item.downloadedBytes = done[0];
                    updateDownloadSpeed(item, done[0]);
                    int percent = (int) Math.min(99, (done[0] * 100) / total);
                    if (percent != item.progress) {
                        item.progress = percent;
                        if (percent % 2 == 0 || percent >= 99) {
                            refreshDownloadPanel();
                            showDownloadNotification(item, "2 koneksi • " + percent + "% • " + readableSpeed(item.speedBytesPerSecond), true);
                        }
                    }
                }
            }
            raf.close();
            in.close();
            conn.disconnect();
        } catch (Exception e) {
            ok[0] = false;
        }
    }

    private void downloadSingle(DownloadItem item, File out) {
        try {
            boolean resumeSingle = out.exists() && item.downloadedBytes > 0 && item.connectionCount == 1;
            long resumeFrom = resumeSingle ? Math.min(item.downloadedBytes, out.length()) : 0;

            HttpURLConnection conn = openDownloadConnection(item.url, item, resumeFrom > 0 ? "bytes=" + resumeFrom + "-" : "");
            validateDownloadResponse(conn);

            int code = conn.getResponseCode();
            boolean append = resumeFrom > 0 && code == 206;
            if (!append) {
                resumeFrom = 0;
                item.downloadedBytes = 0;
                item.progress = 0;
            }

            String cd = conn.getHeaderField("Content-Disposition");
            try {
                String betterName = autoRenameDownloadFile(URLUtil.guessFileName(item.url, cd, conn.getContentType()), item.url, conn.getContentType());
                if (betterName != null && betterName.length() > 0 && !betterName.equals(item.fileName) && resumeFrom == 0) {
                    File newFile = uniqueFile(new File(out.getParentFile(), betterName));
                    item.fileName = newFile.getName();
                    item.path = newFile.getAbsolutePath();
                    out = newFile;
                }
                item.categoryHint = inferDownloadCategoryFromData(item.fileName, item.url, conn.getContentType());
            } catch (Exception ignored) {}

            long contentLen = conn.getContentLengthLong();
            long total = item.totalBytes > 0 && append ? item.totalBytes : (contentLen > 0 ? contentLen + resumeFrom : item.totalBytes);
            item.totalBytes = total;

            InputStream in = conn.getInputStream();
            FileOutputStream fos = new FileOutputStream(out, append);
            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            int len;
            long done = resumeFrom;
            item.lastSpeedTimeMs = 0;
            item.lastSpeedBytes = done;

            while ((len = in.read(buffer)) != -1) {
                if (item.pauseRequested || "paused".equals(item.status)) {
                    break;
                }
                fos.write(buffer, 0, len);
                applySpeedLimit(len, 1);
                done += len;
                item.downloadedBytes = done;
                updateDownloadSpeed(item, done);
                if (total > 0) {
                    int percent = (int) Math.min(99, (done * 100) / total);
                    if (percent != item.progress) {
                        item.progress = percent;
                        if (percent % 2 == 0 || percent >= 99) {
                            refreshDownloadPanel();
                            showDownloadNotification(item, "1 koneksi • " + percent + "% • " + readableSpeed(item.speedBytesPerSecond), true);
                        }
                    }
                }
            }
            fos.close();
            in.close();
            conn.disconnect();

            if (item.pauseRequested || "paused".equals(item.status)) {
                item.status = "paused";
                item.speedBytesPerSecond = 0;
                item.engineInfo = "Dijeda";
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, "Unduhan dijeda", false);
            } else {
                completeDownload(item);
            }
        } catch (Exception e) {
            if (item.pauseRequested || "paused".equals(item.status)) {
                item.status = "paused";
                item.speedBytesPerSecond = 0;
                item.engineInfo = "Dijeda";
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, "Unduhan dijeda", false);
            } else {
                failDownload(item, e.getMessage());
            }
        }
    }

    private void completeDownload(DownloadItem item) {
        item.progress = 100;
        item.status = "completed";
        item.speedBytesPerSecond = 0;
        item.retryCount = 0;
        exportCompletedDownload(item);
        saveDownloadHistory();
        refreshDownloadPanel();
        showDownloadNotification(item, "Unduhan selesai", false);
        runOnUiThread(() -> Toast.makeText(this, "Unduhan selesai: " + item.fileName, Toast.LENGTH_SHORT).show());
        mainHandler.postDelayed(this::pumpDownloadQueue, 500);
    }

    private void exportCompletedDownload(DownloadItem item) {
        if (item == null || item.path == null || item.path.length() == 0) return;
        File source = new File(item.path);
        if (!source.exists()) return;

        try {
            Uri exported;
            if (selectedDownloadTreeUri != null && selectedDownloadTreeUri.length() > 0) {
                exported = copyFileToSelectedTree(source, item.fileName);
            } else {
                exported = copyFileToDefaultDownloads(source, item.fileName);
            }

            if (exported != null) {
                item.publicUri = exported.toString();
                item.engineInfo = getConnectionLabel(item) + " • tersimpan";
                runOnUiThread(() -> Toast.makeText(this, "Disimpan ke folder unduhan", Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "Download selesai, tapi gagal salin ke folder HP", Toast.LENGTH_LONG).show());
        }
    }

    private Uri copyFileToSelectedTree(File source, String fileName) throws Exception {
        Uri treeUri = Uri.parse(selectedDownloadTreeUri);
        Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
        String mime = getMimeTypeForName(fileName);
        Uri newFile = DocumentsContract.createDocument(getContentResolver(), docUri, mime, fileName);
        if (newFile == null) throw new Exception("Tidak bisa membuat file di folder HP");
        copyFileToUri(source, newFile);
        return newFile;
    }

    private Uri copyFileToDefaultDownloads(File source, String fileName) throws Exception {
        String mime = getMimeTypeForName(fileName);

        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Yield Browser");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("Tidak bisa membuat file di Downloads");
            copyFileToUri(source, uri);

            ContentValues done = new ContentValues();
            done.put(MediaStore.MediaColumns.IS_PENDING, 0);
            getContentResolver().update(uri, done, null, null);
            return uri;
        } else {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Yield Browser");
            if (!dir.exists()) dir.mkdirs();
            File out = uniqueFile(new File(dir, fileName));
            copyFileToFile(source, out);
            return Uri.fromFile(out);
        }
    }

    private void copyFileToUri(File source, Uri uri) throws Exception {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(source);
            out = getContentResolver().openOutputStream(uri, "w");
            if (out == null) throw new Exception("Output folder tidak tersedia");
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }

    private void copyFileToFile(File source, File target) throws Exception {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(target);
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            out.flush();
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }

    private String getMimeTypeForName(String fileName) {
        try {
            String ext = MimeTypeMap.getFileExtensionFromUrl(fileName);
            if (ext != null && ext.length() > 0) {
                String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.US));
                if (mime != null) return mime;
            }
        } catch (Exception ignored) {}
        return "application/octet-stream";
    }

    private void failDownload(DownloadItem item, String reason) {
        if (item == null) return;

        if (downloadAutoRetry && !item.pauseRequested && item.retryCount < DOWNLOAD_RETRY_MAX) {
            item.retryCount++;
            item.status = "running";
            item.pauseRequested = false;
            item.speedBytesPerSecond = 0;
            item.failReason = reason == null ? "Koneksi terputus" : reason;
            item.engineInfo = "Retry otomatis " + item.retryCount + "/" + DOWNLOAD_RETRY_MAX;
            saveDownloadHistory();
            refreshDownloadPanel();
            showDownloadNotification(item, item.engineInfo, true);

            mainHandler.postDelayed(() -> {
                try {
                    File out = new File(item.path);
                    if (!"running".equals(item.status)) return;
                    startTwoConnectionDownload(item, out);
                } catch (Exception e) {
                    failDownload(item, e.getMessage());
                }
            }, 1500L * item.retryCount);
            return;
        }

        item.status = "failed";
        item.pauseRequested = false;
        item.speedBytesPerSecond = 0;
        item.failReason = reason == null ? "Koneksi/server menolak unduhan" : reason;
        if (item.engineInfo == null || item.engineInfo.length() == 0) {
            item.engineInfo = getConnectionLabel(item) + " • terputus/gagal";
        } else if (!item.engineInfo.contains("terputus")) {
            item.engineInfo = item.engineInfo + " • terputus/gagal";
        }
        saveDownloadHistory();
        refreshDownloadPanel();
        showDownloadNotification(item, "Unduhan gagal • buka detail", false);
        runOnUiThread(() -> Toast.makeText(this, "Unduhan gagal. Buka Download untuk reload/detail.", Toast.LENGTH_LONG).show());
        mainHandler.postDelayed(this::pumpDownloadQueue, 700);
    }

    private long parseTotalSize(String contentRange) {
        try {
            if (contentRange == null) return -1;
            int slash = contentRange.lastIndexOf('/');
            if (slash == -1) return -1;
            return Long.parseLong(contentRange.substring(slash + 1).trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_DOWNLOADS, "Yield Downloads", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Progress unduhan Yield Browser");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void cancelDownloadNotification(DownloadItem item) {
        if (item == null) return;
        try {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(item.id);
            }
        } catch (Exception ignored) {
        }
    }

    private void showDownloadNotification(DownloadItem item, String text, boolean ongoing) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        Intent intent = new Intent(this, DownloadOpenReceiver.class);
        intent.setAction(ACTION_OPEN_DOWNLOADS);
        intent.putExtra("open_downloads", true);
        intent.putExtra("download_id", item.id);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, item.id + 12000, intent, flags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_DOWNLOADS)
                : new Notification.Builder(this);

        String line = text;
        if ("running".equals(item.status)) {
            line = text + " • " + getConnectionLabel(item);
        } else if ("paused".equals(item.status)) {
            line = "Dijeda • " + getConnectionLabel(item);
        }

        builder.setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(item.fileName)
                .setContentText(line)
                .setContentIntent(pendingIntent)
                .setAutoCancel(!ongoing)
                .setOngoing(ongoing);

        if ("running".equals(item.status) || "paused".equals(item.status)) {
            builder.setProgress(100, Math.max(0, Math.min(100, item.progress)), false);
        } else {
            builder.setProgress(0, 0, false);
        }

        manager.notify(item.id, builder.build());
    }

    private void saveDownloadHistory() {
        Set<String> saved = new HashSet<>();
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if (item.status.equals("completed") || item.status.equals("failed") || item.status.equals("paused") || item.status.equals("running") || item.status.equals("queued")) {
                    saved.add(encode(item.url) + "|" + encode(item.fileName) + "|" + encode(item.path) + "|" + item.status + "|" + item.progress + "|" + item.totalBytes + "|" + item.downloadedBytes + "|" + item.connectionCount + "|" + encode(item.engineInfo) + "|" + encode(item.userAgent) + "|" + encode(item.referer) + "|" + encode(item.failReason) + "|" + encode(item.categoryHint) + "|"
                            + item.part1Start + "|" + item.part1End + "|" + item.part1Done + "|"
                            + item.part2Start + "|" + item.part2End + "|" + item.part2Done + "|"
                            + item.part3Start + "|" + item.part3End + "|" + item.part3Done + "|"
                            + item.part4Start + "|" + item.part4End + "|" + item.part4Done + "|"
                            + encode(item.publicUri) + "|" + item.retryCount + "|" + item.hlsDownload);
                }
            }
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putStringSet(KEY_DOWNLOAD_HISTORY, saved).apply();
    }

    private void loadDownloadHistory() {
        Set<String> saved = getSharedPreferences(PREFS, MODE_PRIVATE).getStringSet(KEY_DOWNLOAD_HISTORY, new HashSet<>());
        synchronized (downloadItems) {
            downloadItems.clear();
            for (String row : saved) {
                try {
                    String[] parts = row.split("\\|", -1);
                    if (parts.length >= 5) {
                        DownloadItem item = new DownloadItem(nextDownloadId++, decode(parts[0]), decode(parts[1]), decode(parts[2]), parts[3], Integer.parseInt(parts[4]));
                        if (parts.length >= 7) {
                            try { item.totalBytes = Long.parseLong(parts[5]); } catch (Exception ignored) {}
                            try { item.downloadedBytes = Long.parseLong(parts[6]); } catch (Exception ignored) {}
                        }
                        if (parts.length >= 8) {
                            try { item.connectionCount = Integer.parseInt(parts[7]); } catch (Exception ignored) {}
                        }
                        if (parts.length >= 9) {
                            item.engineInfo = decode(parts[8]);
                        } else {
                            item.engineInfo = item.connectionCount > 1 ? "2 koneksi sukses" : "1 koneksi";
                        }
                        if (parts.length >= 10) item.userAgent = decode(parts[9]);
                        if (parts.length >= 11) item.referer = decode(parts[10]);
                        if (parts.length >= 12) item.failReason = decode(parts[11]);
                        if (parts.length >= 13) item.categoryHint = decode(parts[12]);
                        if (parts.length >= 19) {
                            try { item.part1Start = Long.parseLong(parts[13]); } catch (Exception ignored) {}
                            try { item.part1End = Long.parseLong(parts[14]); } catch (Exception ignored) {}
                            try { item.part1Done = Long.parseLong(parts[15]); } catch (Exception ignored) {}
                            try { item.part2Start = Long.parseLong(parts[16]); } catch (Exception ignored) {}
                            try { item.part2End = Long.parseLong(parts[17]); } catch (Exception ignored) {}
                            try { item.part2Done = Long.parseLong(parts[18]); } catch (Exception ignored) {}
                        }
                        if (parts.length >= 28) {
                            try { item.part3Start = Long.parseLong(parts[19]); } catch (Exception ignored) {}
                            try { item.part3End = Long.parseLong(parts[20]); } catch (Exception ignored) {}
                            try { item.part3Done = Long.parseLong(parts[21]); } catch (Exception ignored) {}
                            try { item.part4Start = Long.parseLong(parts[22]); } catch (Exception ignored) {}
                            try { item.part4End = Long.parseLong(parts[23]); } catch (Exception ignored) {}
                            try { item.part4Done = Long.parseLong(parts[24]); } catch (Exception ignored) {}
                            item.publicUri = decode(parts[25]);
                            try { item.retryCount = Integer.parseInt(parts[26]); } catch (Exception ignored) {}
                            item.hlsDownload = Boolean.parseBoolean(parts[27]);
                        } else {
                            if (parts.length >= 20) item.publicUri = decode(parts[19]);
                            if (parts.length >= 21) try { item.retryCount = Integer.parseInt(parts[20]); } catch (Exception ignored) {}
                            if (parts.length >= 22) item.hlsDownload = Boolean.parseBoolean(parts[21]);
                        }
                        if ("running".equals(item.status)) {
                            item.status = "paused";
                            item.speedBytesPerSecond = 0;
                            item.engineInfo = "Dijeda";
                        }
                        try {
                            if (item.path != null && item.path.length() > 0) {
                                File savedFile = new File(item.path);
                                if (!savedFile.exists() && ("completed".equals(item.status) || "paused".equals(item.status) || "failed".equals(item.status))) {
                                    cancelDownloadNotification(item);
                                }
                            }
                        } catch (Exception ignored) {}
                        if (item.categoryHint == null || item.categoryHint.length() == 0) {
                            item.categoryHint = inferDownloadCategoryFromData(item.fileName, item.url, "");
                        }
                        downloadItems.add(item);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private String encode(String value) {
        try { return URLEncoder.encode(value == null ? "" : value, "UTF-8"); }
        catch (Exception e) { return ""; }
    }

    private String decode(String value) {
        try { return URLDecoder.decode(value == null ? "" : value, "UTF-8"); }
        catch (Exception e) { return ""; }
    }

    private void toggleBookmark() {
        String url = getEffectiveCurrentUrl();
        if (url == null || url.length() == 0) {
            Toast.makeText(this, "Belum ada situs untuk dibookmark", Toast.LENGTH_SHORT).show();
            return;
        }
        BookmarkItemData existing = findBookmarkByUrl(url);
        if (existing != null) {
            bookmarkData.remove(existing);
            saveBookmarkData();
            Toast.makeText(this, "Bookmark dihapus", Toast.LENGTH_SHORT).show();
        } else {
            String title = (webView != null && webView.getTitle() != null && webView.getTitle().trim().length() > 0) ? webView.getTitle().trim() : guessLabelFromUrl(url);
            bookmarkData.add(0, new BookmarkItemData(title, url, "Bookmark seluler", System.currentTimeMillis()));
            saveBookmarkData();
            Toast.makeText(this, "Situs ditambahkan ke bookmark", Toast.LENGTH_SHORT).show();
        }
        updateTopActionStates();
    }

    private void showBookmarkList() {
        showBookmarkHomePanel();
    }

    private void toggleTranslate() {
        showTranslateOptionsDialog();
    }

    private String translateLanguageLabel(String code) {
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
                    Toast.makeText(this, "Bahasa translate: " + translateTargetLabel, Toast.LENGTH_SHORT).show();

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
                            Toast.makeText(this, "Buka website dulu untuk translate", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        startCompatibleTranslateSession(original);
                        updateTopActionStates();
                        translatePageCompatible();
                    } else if (selected.startsWith("Lanjutkan translate")) {
                        continueCompatibleTranslation();
                    } else if (selected.startsWith("Google Translate proxy")) {
                        if (original == null || original.length() == 0) {
                            Toast.makeText(this, "Buka website dulu untuk translate", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        startGoogleTranslateSession(original);
                        updateTopActionStates();
                        loadTranslatedPage(original);
                    } else if (selected.startsWith("Tampilkan bar")) {
                        hideGoogleTranslateBar = false;
                        saveSettings();
                        showGoogleTranslateBar();
                        Toast.makeText(this, "Bar Google Translate ditampilkan", Toast.LENGTH_SHORT).show();
                    } else if (selected.startsWith("Sembunyikan bar")) {
                        hideGoogleTranslateBar = true;
                        saveSettings();
                        unblockTranslatedPageClicks();
                        Toast.makeText(this, "Bar Google Translate disembunyikan dan klik website diaktifkan", Toast.LENGTH_SHORT).show();
                    } else if (selected.startsWith("Terjemahkan teks")) {
                        translatePageTextOnly();
                    } else if (selected.startsWith("Reload")) {
                        reloadCurrentWebsite();
                    } else if (selected.startsWith("Aktifkan klik")) {
                        unblockTranslatedPageClicks();
                        Toast.makeText(this, "Klik menu website diaktifkan", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Website menolak Google Translate. Beralih ke mode kompatibel.", Toast.LENGTH_LONG).show();
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
        Toast.makeText(this, "Translate dimatikan", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Buka website dulu untuk translate kompatibel", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Translate kompatibel aktif ke " + translateTargetLabel, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Translate kompatibel gagal", Toast.LENGTH_SHORT).show();
        }
    }

    private void continueCompatibleTranslation() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            Toast.makeText(this, "Buka website dulu", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Membuka Google Translate ke " + translateTargetLabel, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Buka halaman dulu", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            webView.evaluateJavascript("(function(){return (document.body&&document.body.innerText?document.body.innerText:'').slice(0,3500);})()", value -> {
                try {
                    String text = value == null ? "" : value;
                    if (text.startsWith("\"") && text.endsWith("\"")) text = text.substring(1, text.length() - 1);
                    text = text.replace("\\n", "\n").replace("\\\"", "\"").replace("\\u003C", "<");
                    if (text.trim().length() == 0) {
                        Toast.makeText(this, "Teks halaman tidak terbaca", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startGoogleTranslateSession(getOriginalForTranslate(getEffectiveCurrentUrl()));
                    compatibleTranslateActive = false;
                    updateTopActionStates();
                    String encoded = URLEncoder.encode(text, "UTF-8");
                    loadBrowserUrl("https://translate.google.com/?sl=auto&tl=" + translateTargetLang + "&op=translate&text=" + encoded);
                    Toast.makeText(this, "Menerjemahkan teks halaman ke " + translateTargetLabel, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal ambil teks halaman", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Translate teks tidak didukung di halaman ini", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, desktopMode ? "Reload desktop mode" : "Reload mobile mode", Toast.LENGTH_SHORT).show();
            } else {
                if (url != null && url.length() > 0) {
                    addressBar.setText(url);
                    openAddressBarUrl();
                } else {
                    Toast.makeText(this, "Belum ada website untuk reload", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Gagal reload website", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean captureAdRedirectToTempTab(String url, String reason) {
        return captureBlockedNavigationToTempTab(url, reason, false);
    }

    private boolean captureDirectImageToTempTab(String url, String reason) {
        return captureBlockedNavigationToTempTab(url, reason, true);
    }

    private boolean captureBlockedNavigationToTempTab(String url, String reason, boolean allowWhenAdBlockOff) {
        if ((!allowWhenAdBlockOff && !adBlock) || url == null || url.trim().length() == 0) return false;
        if (isMediaResourceUrl(url) || isYoutubeCoreUrl(url)) return false;

        String safeUrl = url.trim();

        if (!adBlockRedirectToTempTab) {
            scheduleCloseDetectedAdTabs();
            return true;
        }

        synchronized (tabs) {
            for (TabInfo tab : tabs) {
                if (tab != null && tab.adTab && safeUrl.equals(tab.url)) {
                    updateTabsCountUi();
                    return true;
                }
            }
        }

        int protectedIndex = currentTabIndex;
        TabInfo adTab = new TabInfo("Tab iklan", safeUrl, false, true);
        tabs.add(adTab);
        updateTabsCountUi();

        Toast.makeText(this, "Direct link dibuka di tab baru", Toast.LENGTH_SHORT).show();

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
            tabs.remove(index);
            destroyTabWebView(adTab);

            if (tabs.isEmpty()) {
                tabs.add(new TabInfo("Tab utama", "", false));
                currentTabIndex = 0;
                addressBar.setText("");
                if (homeSearchInput != null) homeSearchInput.setText("");
                activateTabWebView(getCurrentTab(), false);
                skipNextShowHomeTabSave = true;
                showHome();
            } else {
                if (currentTabIndex > index) currentTabIndex--;
                if (currentTabIndex >= tabs.size()) currentTabIndex = tabs.size() - 1;
                if (closingCurrent) {
                    currentTabIndex = Math.max(0, Math.min(fallbackIndex, tabs.size() - 1));
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
                TabInfo t = tabs.get(i);
                if (t == null) continue;
                // v0.9.78: jangan auto-close tab normal hanya karena URL-nya mirip popup/ad host.
                // Beberapa situs seperti invest-tracing.com memang bisa dibuka manual dan butuh compatibility.
                // Auto-close hanya untuk tab yang benar-benar dibuat sebagai tab iklan sementara.
                if (!t.adTab) continue;

                boolean closingCurrent = i == currentTabIndex;
                tabs.remove(i);
                destroyTabWebView(t);
                changed = true;

                if (closingCurrent) {
                    currentTabIndex = Math.max(0, Math.min(currentTabIndex - 1, tabs.size() - 1));
                } else if (currentTabIndex > i) {
                    currentTabIndex--;
                }
            }

            if (tabs.isEmpty()) {
                tabs.add(new TabInfo("Tab utama", "", false));
                currentTabIndex = 0;
                addressBar.setText("");
                if (homeSearchInput != null) homeSearchInput.setText("");
                activateTabWebView(getCurrentTab(), false);
                skipNextShowHomeTabSave = true;
                showHome();
            } else if (currentTabIndex < 0 || currentTabIndex >= tabs.size()) {
                currentTabIndex = Math.max(0, Math.min(currentTabIndex, tabs.size() - 1));
            }
            if (changed && !tabs.isEmpty()) {
                switchToTab(currentTabIndex);
            }

            if (changed) {
                updateTabsCountUi();
                Toast.makeText(this, "Tab iklan otomatis ditutup", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView createBrowserWebView(int visibility) {
        WebView fresh = new WebView(this);
        fresh.setVisibility(visibility);
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

        WebView fresh = createBrowserWebView(showWebPage ? View.VISIBLE : View.GONE);
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

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        applyBrowserSettings();
        webView.addJavascriptInterface(new VideoBridge(), "YieldVideoBridge");
        webView.addJavascriptInterface(new AdBlockBridge(), "YieldAdBlockBridge");
        webView.addJavascriptInterface(new TranslateBridge(), "YieldTranslateBridge");
        try {
            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            if (Build.VERSION.SDK_INT >= 21) cm.setAcceptThirdPartyCookies(webView, true);
        } catch (Exception ignored) {}
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            beginDownloadFromWeb(url, contentDisposition, mimeType, userAgent);
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (view != webView) return false;
                String u = request.getUrl().toString();
                String currentUrl = view != null ? view.getUrl() : "";
                boolean mainFrame = request != null && request.isForMainFrame();
                boolean hasGesture = false;
                try { if (Build.VERSION.SDK_INT >= 24 && request != null) hasGesture = request.hasGesture(); } catch (Exception ignored) {}

                if (safeMode && isUnsafeUrl(u)) {
                    Toast.makeText(MainActivity.this, "Diblokir Safe Browsing sederhana", Toast.LENGTH_SHORT).show();
                    return true;
                }

                // v0.9.45: direct image main-frame harus dicegat sebelum normal user navigation
                // atau compatibility mode. Kalau tidak, klik/redirect gambar komik (.jpg/.jpeg/.webp)
                // bisa mengambil alih tab utama dan berubah menjadi halaman gambar mentah.
                String referenceUrlForDirectImage = (currentUrl != null && currentUrl.length() > 0) ? currentUrl : lastSafeHttpUrl;
                if (mainFrame && !isTrustedMainFrameNavigation(u) && isDirectImageMainFrameNavigation(u, referenceUrlForDirectImage)) {
                    restoreAfterBlockedNavigation(view, u, "Gambar/direct link dibuka di tab baru");
                    return true;
                }

                // v0.9.50: Smart Redirect Context.
                // Domain yang biasanya iklan/direct-link tetap diblokir jika muncul otomatis,
                // tetapi jika benar-benar berasal dari klik user atau hasil pencarian, izinkan
                // dibuka dengan compatibility mode agar link situs mirip Lordborg tidak mati.
                if (mainFrame && isContextAllowedSuspiciousMainFrameNavigation(u, currentUrl, hasGesture)) {
                    markTrustedMainFrameNavigation(u);
                    enableSiteCompatibilityModeForUrl(u, "user-context");
                    return false;
                }

                // v0.9.46: strict compatibility navigation. Host utama dibiarkan jalan polos,
                // tetapi popup/redirect iklan lintas-domain tetap dipindah ke tab sementara.
                if (mainFrame && (isStrictSiteCompatibilityUrl(u) || isStrictSiteCompatibilityUrl(currentUrl))) {
                    String targetHost = normalizeHostForAdBlock(u);
                    String currentHost = normalizeHostForAdBlock(currentUrl);
                    boolean sameSite = currentHost.length() > 0 && targetHost.length() > 0 && sameOrSubDomain(targetHost, currentHost);
                    boolean fromSearch = isSearchEngineResultNavigation(u, currentUrl);
                    if (isExternalSchemeUrl(u)) {
                        restoreAfterBlockedNavigation(view, u, "Iklan/direct link");
                        return true;
                    }
                    if (!sameSite && !fromSearch && (isKnownPopupHost(u) || isLikelyAdClickUrl(u) || isSuspiciousPopupNavigation(u, currentUrl))) {
                        restoreAfterBlockedNavigation(view, u, "Iklan/direct link");
                        return true;
                    }
                    markTrustedMainFrameNavigation(u);
                    if (isStrictSiteCompatibilityUrl(u)) enableSiteCompatibilityModeForUrl(u, "strict-navigation");
                    return false;
                }

                // v0.9.44: kalau domain sedang dalam compatibility mode, jangan blokir navigasi
                // main-frame. Situs anti-adblock sering memakai redirect internal/menu yang kalau
                // ditahan akan membuat halaman blank atau balik ke home.
                if (mainFrame && (isSiteCompatibilityModeActiveForUrl(u) || isSiteCompatibilityModeActiveForUrl(currentUrl))) {
                    markTrustedMainFrameNavigation(u);
                    return false;
                }

                if (mainFrame && isNormalUserMainFrameNavigation(u, currentUrl, hasGesture)) {
                    markTrustedMainFrameNavigation(u);
                    return false;
                }

                if (request.isForMainFrame() && isExternalSchemeUrl(u)) {
                    restoreAfterBlockedNavigation(view, u, "Iklan/direct link");
                    return true;
                }

                if (adBlock && (adBlockRedirectBlocker || adBlockClickHijackBlocker) && request.isForMainFrame()
                        && !isTrustedMainFrameNavigation(u)
                        && !isNormalUserMainFrameNavigation(u, currentUrl, hasGesture)
                        && (isSuspiciousPopupNavigation(u, currentUrl) || isLikelyAdClickUrl(u))) {
                    restoreAfterBlockedNavigation(view, u, "Iklan/direct link");
                    return true;
                }
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // v0.9.81: shouldInterceptRequest berjalan di thread Chromium/background.
                // Jangan pernah memanggil WebView.getUrl()/getTitle()/method WebView apa pun di sini.
                // Android akan crash: "A WebView method was called on thread 'ThreadPoolForeg'".
                // Ambil URL halaman dari cache main-thread saja.
                if (view != webView) return super.shouldInterceptRequest(view, request);
                String u = request.getUrl().toString();
                String pageUrl = currentPageUrlForRequest != null ? currentPageUrlForRequest : "";
                if ((pageUrl == null || pageUrl.length() == 0)) {
                    TabInfo owner = findTabByWebView(view);
                    if (owner != null && owner.url != null) pageUrl = owner.url;
                }
                if ((pageUrl == null || pageUrl.length() == 0) && lastSafeHttpUrl != null) pageUrl = lastSafeHttpUrl;
                // v0.9.44: compatibility mode bersifat universal per-domain. Jika aktif, jangan
                // intercept resource sama sekali untuk halaman itu. Ini menyelesaikan situs yang
                // reload/blank karena mendeteksi resource diblokir walau user sedang mencoba masuk.
                // v0.9.63: YouTube player tidak boleh diblokir di network layer sama sekali.
                // Pada Android WebView, memblokir doubleclick/google ads metadata dari halaman YouTube
                // sering membuat player utama menunggu iklan lalu layar menjadi hitam/stuck.
                // Jadi khusus YouTube, semua request dibiarkan lewat dan ad handling hanya dilakukan
                // oleh script YouTube Safe AdBlock yang klik Skip / speed iklan secara aman.
                if (isYouTubePageUrl(pageUrl) || isYouTubePageUrl(u)) {
                    return super.shouldInterceptRequest(view, request);
                }
                if (isStrictSiteCompatibilityUrl(pageUrl) || isStrictSiteCompatibilityUrl(u) || isSiteCompatibilityModeActiveForUrl(pageUrl)) {
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
                if (adBlock && adBlockScriptIframeBlocker && !isMediaResourceUrl(u) && !isYoutubeCoreUrl(u) && (isAdUrl(u) || isKnownPopupHost(u))) {
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (view != webView) {
                    super.onReceivedError(view, request, error);
                    return;
                }
                try {
                    String failedUrl = request != null && request.getUrl() != null ? request.getUrl().toString() : "";
                    int errorCode = Build.VERSION.SDK_INT >= 23 && error != null ? error.getErrorCode() : 0;
                    String errorText = error != null && error.getDescription() != null
                            ? String.valueOf(error.getDescription()).toLowerCase(Locale.US)
                            : "";
                    if (request != null && request.isForMainFrame()
                            && isSiteCompatibilityModeActiveForUrl(failedUrl)) {
                        // Compatibility mode: biarkan WebView menampilkan hasil/error asli situs,
                        // jangan restore/goBack karena itu bisa memicu loop ke home.
                        return;
                    }
                    if (request != null && request.isForMainFrame()
                            && (isExternalSchemeUrl(failedUrl)
                            || errorCode == WebViewClient.ERROR_UNSUPPORTED_SCHEME
                            || errorText.contains("unknown_url_scheme")
                            || (adBlock && !isTrustedMainFrameNavigation(failedUrl) && (isKnownPopupHost(failedUrl)
                            || isLikelyAdClickUrl(failedUrl)
                            || isAdUrl(failedUrl)
                            || isSuspiciousPopupNavigation(failedUrl, lastSafeHttpUrl))))) {
                        restoreAfterBlockedNavigation(view, failedUrl, "Iklan/direct link");
                        return;
                    }
                } catch (Exception ignored) {
                }
                if (smoothSearchTransitionActive) finishSmoothSearchTransition();
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (view != webView) {
                    TabInfo owner = findTabByWebView(view);
                    if (owner != null) {
                        String inactiveUrl = extractOriginalUrl(url) != null ? extractOriginalUrl(url) : url;
                        String inactiveTitle = view != null ? view.getTitle() : null;
                        commitTabUrlIfSafe(owner, inactiveUrl, inactiveTitle);
                    }
                    super.onPageStarted(view, url, favicon);
                    return;
                }
                String safeBeforePageStarted = lastSafeHttpUrl;
                currentPageUrlForRequest = extractOriginalUrl(url) != null ? extractOriginalUrl(url) : url;
                webHorizontalGestureGuard = false;
                webHorizontalGestureGuardHost = hostOfUrl(currentPageUrlForRequest);
                syncNightModeWebSettingsForUrl(currentPageUrlForRequest);
                boolean strictCompatibilityActive = isStrictSiteCompatibilityUrl(url);
                boolean reloadLoopGuarded = registerNavigationLoopGuard(url);
                boolean siteCompatibilityActive = isSiteCompatibilityModeActiveForUrl(url);
                if (strictCompatibilityActive) {
                    enableSiteCompatibilityModeForUrl(url, "strict-start");
                    applyPlainCompatibilitySettingsForUrl(url);
                    scheduleCompatibilityLoadFallback(url);
                } else {
                    applyBrowserSettings();
                }
                if (!strictCompatibilityActive && !reloadLoopGuarded && !siteCompatibilityActive) {
                    if (desktopMode) {
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 250);
                    } else {
                        mainHandler.postDelayed(() -> applyMobileViewportIfNeeded(), 250);
                    }
                } else {
                    // v0.9.42: jangan stopLoading untuk seluruh host. Stop loading membuat halaman
                    // seperti lordborg.com blank saat user klik menu internal. Guard cukup mematikan
                    // injection/restore agresif, sementara load utama tetap dibiarkan selesai.
                    try { if (progressBar != null) progressBar.setVisibility(View.GONE); } catch (Exception ignored) {}
                    // v0.9.60: Compatibility mode tidak boleh mematikan Desktop Mode.
                    // Untuk situs model Lordborg, tetap terapkan desktop profile/viewport saat toggle Desktop ON,
                    // tapi hanya di domain compatibility agar situs lain yang sudah stabil tidak berubah.
                    if (desktopMode) {
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 280);
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 1200);
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 2600);
                    } else {
                        mainHandler.postDelayed(() -> applyMobileViewportIfNeeded(), 280);
                    }
                }
                super.onPageStarted(view, url, favicon);
                if (pendingHideKeyboardAfterNavigation) {
                    blurWebInputsAndHideKeyboard();
                }
                try {
                    if (smoothSearchTransitionActive && navigationLoadingOverlay != null) {
                        navigationLoadingOverlay.bringToFront();
                    }
                    if (shouldRecordHistoryUrl(url)) {
                        historyClearLock = false;
                        addBrowserHistory(url, url);
                    }
                    String currentUrl = getEffectiveCurrentUrl();
                    String referenceUrlForDirectImage = (safeBeforePageStarted != null && safeBeforePageStarted.length() > 0)
                            ? safeBeforePageStarted
                            : currentUrl;
                    if (!isTrustedMainFrameNavigation(url) && isDirectImageMainFrameNavigation(url, referenceUrlForDirectImage)) {
                        restoreAfterBlockedNavigation(view, url, "Gambar/direct link dibuka di tab baru");
                        return;
                    }
                    if (isExternalSchemeUrl(url)) {
                        restoreAfterBlockedNavigation(view, url, "Link aplikasi/iklan diblokir");
                        return;
                    }
                    if (adBlock && adBlockRedirectBlocker
                            && !isTrustedMainFrameNavigation(url)
                            && !isSearchEngineResultNavigation(url, currentUrl)
                            && (isSuspiciousPopupNavigation(url, currentUrl) || isLikelyAdClickUrl(url))) {
                        restoreAfterBlockedNavigation(view, url, "Iklan/direct link");
                        return;
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                if (view != webView) {
                    TabInfo owner = findTabByWebView(view);
                    if (owner != null) commitTabUrlIfSafe(owner, extractOriginalUrl(url) != null ? extractOriginalUrl(url) : url, owner.title);
                    return;
                }
                String shownUrl = extractOriginalUrl(url);
                String finalUrl = shownUrl != null ? shownUrl : url;
                currentPageUrlForRequest = finalUrl;
                syncNightModeWebSettingsForUrl(finalUrl);
                scheduleNightModeSyncForPage(finalUrl);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (view != webView) {
                    TabInfo owner = findTabByWebView(view);
                    if (owner != null) {
                        String inactiveUrl = extractOriginalUrl(url) != null ? extractOriginalUrl(url) : url;
                        String inactiveTitle = view != null ? view.getTitle() : null;
                        commitTabUrlIfSafe(owner, inactiveUrl, inactiveTitle);
                        try {
                            Bundle state = new Bundle();
                            WebBackForwardList saved = view.saveState(state);
                            if (saved != null && saved.getSize() > 0) owner.webState = state;
                        } catch (Exception ignored) {}
                    }
                    super.onPageFinished(view, url);
                    return;
                }
                String shownUrl = extractOriginalUrl(url);
                String finalUrl = shownUrl != null ? shownUrl : url;
                currentPageUrlForRequest = finalUrl;
                scheduleHorizontalGestureGuardCheck(finalUrl);
                if (shouldRecordHistoryUrl(finalUrl) && canCommitUrlToTab(getCurrentTab(), finalUrl)) {
                    lastSafeHttpUrl = finalUrl;
                }
                if (webView != null && webView.getVisibility() == View.VISIBLE) {
                    addressBar.setText(finalUrl);
                }
                progressBar.setVisibility(View.GONE);
                boolean pageReloadGuarded = isStrictSiteCompatibilityUrl(finalUrl) || isReloadLoopGuardActiveForUrl(finalUrl) || isSiteCompatibilityModeActiveForUrl(finalUrl);
                if (isStrictSiteCompatibilityUrl(finalUrl)) {
                    applyPlainCompatibilitySettingsForUrl(finalUrl);
                    cancelSmoothSearchTransition();
                }
                if (pageReloadGuarded) {
                    // v0.9.60: situs compatibility tetap harus mengikuti Desktop/Mobile toggle.
                    // Jangan inject adblock/fitur berat, cukup profile + viewport sesuai mode aktif.
                    if (desktopMode) {
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 350);
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 1200);
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 2600);
                    } else {
                        mainHandler.postDelayed(() -> applyMobileViewportIfNeeded(), 350);
                    }
                }
                if (!pageReloadGuarded) {
                    applyViewportForCurrentMode();
                    mainHandler.postDelayed(() -> applyViewportForCurrentMode(), 600);
                    mainHandler.postDelayed(() -> applyViewportForCurrentMode(), 1800);
                    if (desktopMode) {
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 350);
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 1200);
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 2600);
                    }
                    if (readerMode) injectReaderMode();
                    if (adBlock) {
                        injectPremiumAdBlock();
                        injectYouTubeSafeAdBlockV6();
                        mainHandler.postDelayed(() -> { injectPremiumAdBlock(); injectYouTubeSafeAdBlockV6(); }, 1800);
                        mainHandler.postDelayed(() -> { injectPremiumAdBlock(); injectYouTubeSafeAdBlockV6(); }, 5200);
                    }
                    scheduleUniversalBlankCompatibilityRecovery(finalUrl);
                    updateVideoControlsVisibility();
                }
                TabInfo currentTab = getCurrentTab();
                if (shouldRecordHistoryUrl(finalUrl) && !currentTab.privateTab) {
                    addBrowserHistory(view.getTitle(), finalUrl);
                }
                if (shouldRecordHistoryUrl(finalUrl)) {
                    commitTabUrlIfSafe(currentTab, finalUrl, view.getTitle());
                    saveTabsSession();
                    // Histori sudah dicatat di atas agar tersimpan lebih cepat.
                }
                if (!pageReloadGuarded) {
                    videoControlsManualHidden = false;
                    injectVideoPlaybackWatcher();
                    scheduleNightModeSyncForPage(finalUrl);
                    detectTranslateProxyBlocked(url);
                }
                if (!pageReloadGuarded && hideGoogleTranslateBar && isGoogleTranslatedUrl(url)) {
                    mainHandler.postDelayed(() -> hideGoogleTranslateToolbar(), 250);
                    mainHandler.postDelayed(() -> hideGoogleTranslateToolbar(), 800);
                    mainHandler.postDelayed(() -> hideGoogleTranslateToolbar(), 1800);
                    mainHandler.postDelayed(() -> hideGoogleTranslateToolbar(), 3500);
                    mainHandler.postDelayed(() -> hideGoogleTranslateToolbar(), 6000);
                }
                if (!pageReloadGuarded && translateEnabled && compatibleTranslateActive && !translateManuallyDisabled && !isGoogleTranslatedUrl(url)) {
                    final int token = translateSessionToken;
                    mainHandler.postDelayed(() -> translatePageCompatible(token), 600);
                    mainHandler.postDelayed(() -> translatePageCompatible(token), 2200);
                }
                if (pendingHideKeyboardAfterNavigation) {
                    blurWebInputsAndHideKeyboard();
                    mainHandler.postDelayed(() -> blurWebInputsAndHideKeyboard(), 250);
                    mainHandler.postDelayed(() -> {
                        blurWebInputsAndHideKeyboard();
                        pendingHideKeyboardAfterNavigation = false;
                    }, 900);
                }
                if (smoothSearchTransitionActive) {
                    mainHandler.postDelayed(() -> finishSmoothSearchTransition(), 220);
                }
                scheduleCloseDetectedAdTabs();
                updateTopActionStates();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (view != webView) return;
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreenVideoView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                fullscreenVideoView = view;
                fullscreenVideoCallback = callback;
                videoLandscapeModeActive = false;
                fullscreenStartedFromVideoLandscape = false;
                originalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
                originalRequestedOrientation = getRequestedOrientation();

                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.addView(fullscreenVideoView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

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
                checkAndShowVideoControls();
            }

            @Override
            public void onHideCustomView() {
                if (fullscreenVideoView == null) return;

                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.removeView(fullscreenVideoView);
                fullscreenVideoView = null;

                if (fullscreenVideoCallback != null) {
                    fullscreenVideoCallback.onCustomViewHidden();
                    fullscreenVideoCallback = null;
                }

                restoreAfterVideoFullscreen();
                updateVideoModeToggleButton();
            }
        });
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

    private void syncNightModeWebSettingsForUrl(String url) {
        if (webView == null) return;
        boolean active = isNightModeActiveForUrl(url);
        try {
            WebSettings settings = webView.getSettings();
            if (Build.VERSION.SDK_INT >= 29) {
                settings.setForceDark(active ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
            }
            if (Build.VERSION.SDK_INT >= 33) {
                settings.setAlgorithmicDarkeningAllowed(active);
            }
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
        if (isSiteCompatibilityModeActiveForUrl(pageUrl)) return;

        syncNightModeWebSettingsForUrl(pageUrl);
        boolean active = isNightModeActiveForUrl(pageUrl);
        lastNightModeSyncUrl = pageUrl != null ? pageUrl : "";

        String js;
        if (active) {
            // v0.9.47: Night ON memasang style gelap tunggal dan membersihkan style light-off.
            js =
                    "javascript:(function(){"
                            + "try{"
                            + "var light=document.getElementById('yield-light-style');if(light)light.remove();"
                            + "var id='yield-night-style';"
                            + "var old=document.getElementById(id);if(old)old.remove();"
                            + "var s=document.createElement('style');s.id=id;"
                            + "s.innerHTML="
                            + "'html,body{background:#0b0d10!important;color-scheme:dark!important;}' + "
                            + "':root{color-scheme:dark!important;}' + "
                            + "'input,textarea,select{color-scheme:dark!important;}' + "
                            + "'img,video,canvas,svg,picture{filter:none!important;}';"
                            + "(document.head||document.documentElement).appendChild(s);"
                            + "var meta=document.querySelector('meta[name=color-scheme]');"
                            + "if(!meta){meta=document.createElement('meta');meta.name='color-scheme';(document.head||document.documentElement).appendChild(meta);}"
                            + "meta.setAttribute('content','dark light');"
                            + "}"
                            + "catch(e){}"
                            + "})()";
        } else {
            // v0.9.47: OFF harus menang melawan bfcache/history dan sisa dark CSS sebelumnya.
            // Karena beberapa halaman tersimpan dari riwayat membawa DOM gelap lama, pasang light guard ringan.
            js =
                    "javascript:(function(){"
                            + "try{"
                            + "var ids=['yield-night-style','yield-dark-style','yield-force-dark'];"
                            + "for(var i=0;i<ids.length;i++){var x=document.getElementById(ids[i]);if(x)x.remove();}"
                            + "var html=document.documentElement;"
                            + "if(html){html.style.colorScheme='light';html.style.background='';html.style.backgroundColor='';html.classList.remove('dark','night','night-mode','dark-mode');}"
                            + "if(document.body){document.body.style.colorScheme='light';document.body.style.background='';document.body.style.backgroundColor='';document.body.style.color='';document.body.classList.remove('dark','night','night-mode','dark-mode');}"
                            + "var meta=document.querySelector('meta[name=color-scheme]');"
                            + "if(!meta){meta=document.createElement('meta');meta.name='color-scheme';(document.head||document.documentElement).appendChild(meta);}"
                            + "meta.setAttribute('content','light');"
                            + "var light=document.getElementById('yield-light-style');if(light)light.remove();"
                            + "light=document.createElement('style');light.id='yield-light-style';"
                            + "light.innerHTML=':root,html,body{color-scheme:light!important;} html,body{background:#ffffff!important;} input,textarea,select{color-scheme:light!important;} img,video,canvas,svg,picture{filter:none!important;}';"
                            + "(document.head||document.documentElement).appendChild(light);"
                            + "}"
                            + "catch(e){}"
                            + "})()";
        }

        try {
            runPageScript(js);
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
            Toast.makeText(this, "Mode Malam: OFF", Toast.LENGTH_SHORT).show();
        }));
        box.addView(nightChoiceRow("ON", "ON".equals(nightModeOption), v -> {
            setNightModeOptionAndApply("ON", true);
            updateTopActionStates();
            Toast.makeText(this, "Mode Malam: ON", Toast.LENGTH_SHORT).show();
        }));
        box.addView(nightChoiceRow("Auto ikut sistem", "AUTO".equals(nightModeOption), v -> {
            setNightModeOptionAndApply("AUTO", true);
            updateTopActionStates();
            Toast.makeText(this, "Mode Malam: Auto ikut sistem", Toast.LENGTH_SHORT).show();
        }));
        box.addView(nightChoiceRow("Bersihkan style gelap halaman ini", false, v -> {
            disableNightModeCompletely(true);
            updateTopActionStates();
            Toast.makeText(this, "Style gelap dibersihkan", Toast.LENGTH_SHORT).show();
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
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(12), dp(4), dp(12));

        TextView radio = new TextView(this);
        radio.setText(checked ? "◉" : "○");
        radio.setTextColor(checked ? COLOR_ACCENT : COLOR_SUBTEXT);
        radio.setTextSize(24);
        radio.setGravity(Gravity.CENTER);
        row.addView(radio, new LinearLayout.LayoutParams(dp(42), dp(42)));

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(18);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1);
        tp.setMargins(dp(10), 0, 0, 0);
        row.addView(text, tp);

        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            View parent = (View) row.getParent();
            if (parent instanceof LinearLayout) {
                LinearLayout list = (LinearLayout) parent;
                for (int i = 0; i < list.getChildCount(); i++) {
                    View child = list.getChildAt(i);
                    if (child instanceof LinearLayout) {
                        LinearLayout childRow = (LinearLayout) child;
                        if (childRow.getChildCount() > 0 && childRow.getChildAt(0) instanceof TextView) {
                            TextView r = (TextView) childRow.getChildAt(0);
                            if ("◉".contentEquals(r.getText()) || "○".contentEquals(r.getText())) {
                                r.setText("○");
                                r.setTextColor(COLOR_SUBTEXT);
                            }
                        }
                    }
                }
            }
            radio.setText("◉");
            radio.setTextColor(COLOR_ACCENT);
        });
        return row;
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
            Toast.makeText(this, "Buka situs dulu untuk mengatur pengecualian", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, excepted ? "Pengecualian dihapus" : "Situs dikecualikan", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void loadBrowserUrl(String url) {
        if (url == null) return;
        try { activateTabWebView(getCurrentTab(), true); } catch (Exception ignored) {}
        if (webView == null) return;
        String cleanUrl = url.trim();
        cleanUrl = normalizeUrlForCurrentBrowserMode(cleanUrl);
        if (cleanUrl == null || cleanUrl.length() == 0) return;

        String lower = cleanUrl.toLowerCase(Locale.US);
        if (lower.startsWith("javascript:") || lower.startsWith("about:") || lower.startsWith("data:")) {
            webView.loadUrl(cleanUrl);
            return;
        }

        try {
            currentPageUrlForRequest = cleanUrl;
            markTrustedMainFrameNavigation(cleanUrl);

            // v0.9.46: untuk situs sensitif seperti lordborg.com, jangan pakai header buatan
            // dan jangan inject/restore agresif. Beberapa situs anti-security menganggap custom
            // Sec-CH/User-Agent header sebagai request tidak normal lalu loading/blank terus.
            if (isStrictSiteCompatibilityUrl(cleanUrl)) {
                enableSiteCompatibilityModeForUrl(cleanUrl, "strict-site");
                applyPlainCompatibilitySettingsForUrl(cleanUrl);
                loadCompatibilityUrlWithCurrentMode(cleanUrl);
                if (desktopMode) {
                    mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 350);
                    mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 1300);
                    mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 2800);
                }
                scheduleCompatibilityLoadFallback(cleanUrl);
                return;
            }

            applyBrowserSettings();
            Map<String, String> headers = new LinkedHashMap<>();
            if (desktopMode) {
                headers.put("User-Agent", getDesktopUserAgent());
                headers.put("Sec-CH-UA-Mobile", "?0");
                headers.put("Sec-CH-UA-Platform", "\"Windows\"");
                headers.put("Upgrade-Insecure-Requests", "1");
            } else {
                headers.put("User-Agent", getMobileUserAgent());
                headers.put("Sec-CH-UA-Mobile", "?1");
                headers.put("Sec-CH-UA-Platform", "\"Android\"");
            }
            headers.put("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7");
            webView.loadUrl(cleanUrl, headers);
        } catch (Exception e) {
            try { webView.loadUrl(cleanUrl); } catch (Exception ignored) {}
        }
    }

    private void scheduleMobileViewportReset() {
        if (desktopMode) return;
        int token = browserModeToken;
        mainHandler.postDelayed(() -> { if (token == browserModeToken && !desktopMode) applyMobileViewportIfNeeded(); }, 120);
        mainHandler.postDelayed(() -> { if (token == browserModeToken && !desktopMode) applyMobileViewportIfNeeded(); }, 500);
        mainHandler.postDelayed(() -> { if (token == browserModeToken && !desktopMode) applyMobileViewportIfNeeded(); }, 1200);
    }

    private void forceMobileModeAfterUpdateIfNeeded(SharedPreferences p) {
        try {
            if (!p.getBoolean("forceMobileModeV0939", false)) {
                desktopMode = false;
                p.edit()
                        .putBoolean("desktopMode", false)
                        .putBoolean("forceMobileModeV0939", true)
                        .apply();
            }
        } catch (Exception ignored) {
        }
    }

    private void toggleDesktopModeSafely() {
        boolean previousDesktopMode = desktopMode;
        try {
            String targetUrl = getSafeReloadUrlForModeChange();
            boolean wasShowingWeb = webView != null && webView.getVisibility() == View.VISIBLE;
            desktopMode = !desktopMode;
            targetUrl = normalizeUrlForCurrentBrowserMode(targetUrl);
            saveSettings();

            if (targetUrl != null && targetUrl.length() > 0) {
                addressBar.setText(targetUrl);
            }

            if (wasShowingWeb && targetUrl != null && targetUrl.length() > 0) {
                // v0.9.40: Browser-professional style mode switch.
                // Jangan pakai reload biasa, karena WebView/Google bisa mempertahankan DOM lama
                // sehingga Desktop ON/OFF baru terasa setelah pencarian baru.
                // Solusinya: buat ulang WebView, terapkan profile mode dulu, baru request ulang URL.
                hardReloadUrlWithCurrentBrowserMode(targetUrl, true);
            } else {
                applyBrowserSettings();
                if (!wasShowingWeb) showHome();
            }

            Toast.makeText(this, desktopMode ? "Desktop mode aktif" : "Mode mobile aktif", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            desktopMode = previousDesktopMode;
            try {
                applyBrowserSettings();
                saveSettings();
            } catch (Exception ignored) {}
        }
    }

    private void loadUrlAfterHardMobileReset(String targetUrl) {
        if (targetUrl == null || targetUrl.trim().length() == 0) return;
        desktopMode = false;
        saveSettings();
        recreateBrowserWebViewForMode(targetUrl, true);
    }

    private String getSafeReloadUrlForModeChange() {
        String currentWebUrl = webView != null ? webView.getUrl() : "";
        String currentAddressUrl = addressBar != null ? addressBar.getText().toString() : "";
        String[] candidates = new String[]{
                currentWebUrl,
                currentAddressUrl,
                lastSafeHttpUrl
        };

        for (int i = 0; i < candidates.length; i++) {
            String candidate = candidates[i];
            String clean = extractOriginalUrl(candidate);
            if (clean == null || clean.trim().length() == 0) clean = candidate;
            boolean explicitCurrentPage = i < 2;
            if (isSafeUrlForModeReload(clean, explicitCurrentPage)) return clean;
        }
        return null;
    }

    private boolean isSafeUrlForModeReload(String url) {
        return isSafeUrlForModeReload(url, false);
    }

    private boolean isSafeUrlForModeReload(String url, boolean explicitCurrentPage) {
        if (!isHttpOrHttpsUrl(url)) return false;
        if (isExternalSchemeUrl(url)) return false;
        if (isImageResourceUrl(url) || isMediaResourceUrl(url)) return false;
        String lower = url == null ? "" : url.toLowerCase(Locale.US);
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("about:")) return false;
        // v0.9.67: Desktop/Mobile toggle adalah aksi user eksplisit.
        // Untuk halaman yang sedang dibuka seperti invest-tracing.com, jangan ditolak hanya
        // karena host-nya masuk daftar popup/ad. Jika ditolak, toggle tidak reload dan UA lama tersisa.
        if (explicitCurrentPage) return true;
        if (isLikelyAdClickUrl(url)) return false;
        if (isKnownPopupHost(url)) return false;
        if (isAdUrl(url)) return false;
        return true;
    }

    private String normalizeUrlForCurrentBrowserMode(String url) {
        try {
            if (url == null) return null;
            String clean = extractOriginalUrl(url);
            if (clean == null || clean.trim().length() == 0) clean = url;
            if (!isHttpOrHttpsUrl(clean)) return clean;
            if (isYouTubePageUrl(clean)) {
                if (desktopMode) {
                    clean = clean.replace("https://m.youtube.com", "https://www.youtube.com")
                            .replace("http://m.youtube.com", "https://www.youtube.com");
                } else {
                    clean = clean.replace("https://www.youtube.com", "https://m.youtube.com")
                            .replace("http://www.youtube.com", "https://m.youtube.com")
                            .replace("https://youtube.com", "https://m.youtube.com")
                            .replace("http://youtube.com", "https://m.youtube.com");
                }
            }
            return clean;
        } catch (Exception e) {
            return url;
        }
    }

    private void applyBrowserSettings() {
        if (webView == null) return;
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setSupportMultipleWindows(false);
        try { settings.setMediaPlaybackRequiresUserGesture(!videoBackgroundPlay); } catch (Exception ignored) {}
        settings.setJavaScriptCanOpenWindowsAutomatically(!(adBlock && adBlockPopupBlocker));
        settings.setDatabaseEnabled(true);
        settings.setCacheMode((speedMode || videoBufferBooster) ? WebSettings.LOAD_CACHE_ELSE_NETWORK : WebSettings.LOAD_DEFAULT);
        settings.setLoadsImagesAutomatically(!dataSaver);
        settings.setTextZoom(textZoom <= 0 ? 100 : textZoom);

        if (desktopMode) {
            applyDesktopProfile(settings);
        } else {
            applyMobileProfile(settings);
        }

        try {
            boolean activeNight = isNightModeActiveForCurrentSite();
            if (Build.VERSION.SDK_INT >= 29) {
                settings.setForceDark(activeNight ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
            }
            if (Build.VERSION.SDK_INT >= 33) {
                settings.setAlgorithmicDarkeningAllowed(activeNight);
            }
        } catch (Exception ignored) {
        }

        try {
            webView.setBackgroundColor(isNightModeActiveForCurrentSite() ? COLOR_BG : Color.WHITE);
        } catch (Exception ignored) {
        }
    }

    private void applyMobileProfile(WebSettings settings) {
        if (settings == null) return;
        settings.setUserAgentString(getMobileUserAgent());
        settings.setUseWideViewPort(false);
        settings.setLoadWithOverviewMode(false);
        try { settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING); } catch (Exception ignored) {}
        try { webView.setInitialScale(0); } catch (Exception ignored) {}
        try { webView.setHorizontalScrollBarEnabled(false); } catch (Exception ignored) {}
        try { webView.setVerticalScrollBarEnabled(true); } catch (Exception ignored) {}
    }

    private void applyDesktopProfile(WebSettings settings) {
        if (settings == null) return;
        settings.setUserAgentString(getDesktopUserAgent());
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        try { settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL); } catch (Exception ignored) {}
        // Desktop mode must feel like a real browser desktop-site toggle:
        // start zoomed out enough to show the wide page, but keep pinch zoom available.
        try { if (webView != null) webView.setInitialScale(65); } catch (Exception ignored) {}
        try { if (webView != null) webView.setHorizontalScrollBarEnabled(true); } catch (Exception ignored) {}
        try { if (webView != null) webView.setVerticalScrollBarEnabled(true); } catch (Exception ignored) {}
    }

    private String getMobileUserAgent() {
        return "Mozilla/5.0 (Linux; Android 11; RMX1971) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";
    }

    private String getDesktopUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    }

    private String hostOfUrl(String url) {
        try {
            String clean = extractOriginalUrl(url);
            if (clean == null || clean.trim().length() == 0) clean = url;
            Uri uri = Uri.parse(clean);
            String host = uri.getHost();
            if (host == null) return "";
            host = host.toLowerCase(Locale.US);
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception e) {
            return "";
        }
    }

    private String navigationLoopKey(String url) {
        try {
            String clean = extractOriginalUrl(url);
            if (clean == null || clean.trim().length() == 0) clean = url;
            if (clean == null) return "";
            int hash = clean.indexOf('#');
            if (hash >= 0) clean = clean.substring(0, hash);
            return clean.trim().toLowerCase(Locale.US);
        } catch (Exception e) {
            return url == null ? "" : url.trim().toLowerCase(Locale.US);
        }
    }

    private void enableSiteCompatibilityModeForUrl(String url, String reason) {
        try {
            if (!isHttpOrHttpsUrl(url)) return;
            String host = hostOfUrl(url);
            if (host == null || host.length() == 0) return;
            long until = System.currentTimeMillis() + 300000L;
            siteCompatibilityHost = host;
            siteCompatibilityUntilMs = until;
            try {
                String normalized = host.toLowerCase(Locale.US);
                if (normalized.startsWith("www.")) normalized = normalized.substring(4);
                siteCompatibilityHosts.put(normalized, until);
            } catch (Exception ignored) {
            }
            if (System.currentTimeMillis() - siteCompatibilityToastLastMs > 8000L) {
                siteCompatibilityToastLastMs = System.currentTimeMillis();
                Toast.makeText(this, "Mode kompatibel aktif untuk situs ini", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isSiteCompatibilityModeActiveForUrl(String url) {
        try {
            long now = System.currentTimeMillis();
            String host = hostOfUrl(url);
            if (host == null || host.length() == 0) return false;
            String h = host.toLowerCase(Locale.US);
            if (h.startsWith("www.")) h = h.substring(4);

            // Legacy single-host guard tetap didukung.
            if (siteCompatibilityHost != null && siteCompatibilityHost.length() > 0 && now <= siteCompatibilityUntilMs) {
                String legacy = siteCompatibilityHost.toLowerCase(Locale.US);
                if (legacy.startsWith("www.")) legacy = legacy.substring(4);
                if (h.equals(legacy) || h.endsWith("." + legacy)) return true;
            }

            // v0.9.78: multi-host compatibility untuk banyak tab.
            try {
                ArrayList<String> expired = new ArrayList<>();
                for (Map.Entry<String, Long> e : siteCompatibilityHosts.entrySet()) {
                    String base = e.getKey();
                    long until = e.getValue() == null ? 0L : e.getValue();
                    if (now > until) {
                        expired.add(base);
                        continue;
                    }
                    if (base != null && base.length() > 0 && (h.equals(base) || h.endsWith("." + base))) return true;
                }
                for (String dead : expired) siteCompatibilityHosts.remove(dead);
            } catch (Exception ignored) {
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isStrictSiteCompatibilityUrl(String url) {
        try {
            String host = hostOfUrl(url);
            if (host == null || host.length() == 0) return false;
            if (isKnownStrictCompatibilityHost(host)) return true;
            return isSiteCompatibilityModeActiveForUrl(url);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isKnownStrictCompatibilityHost(String host) {
        try {
            if (host == null || host.length() == 0) return false;
            String h = host.toLowerCase(Locale.US);
            if (h.startsWith("www.")) h = h.substring(4);
            for (String base : STRICT_COMPAT_HOSTS) {
                if (h.equals(base) || h.endsWith("." + base)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void applyPlainCompatibilitySettingsForUrl(String url) {
        if (webView == null) return;
        try {
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setLoadsImagesAutomatically(true);
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
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
            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            if (Build.VERSION.SDK_INT >= 21) cm.setAcceptThirdPartyCookies(webView, true);
            try { webView.setBackgroundColor(Color.WHITE); } catch (Exception ignored) {}
        } catch (Exception ignored) {
        }
    }


    private void loadCompatibilityUrlWithCurrentMode(String cleanUrl) {
        if (webView == null || cleanUrl == null || cleanUrl.trim().length() == 0) return;
        try {
            if (desktopMode) {
                // Minimal desktop request untuk situs compatibility: cukup UA desktop + bahasa.
                // Hindari header Sec-CH custom yang dulu bisa memicu security/blank di situs berat iklan.
                Map<String, String> headers = new LinkedHashMap<>();
                headers.put("User-Agent", getDesktopUserAgent());
                headers.put("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7");
                webView.loadUrl(cleanUrl, headers);
            } else {
                webView.loadUrl(cleanUrl);
            }
        } catch (Exception e) {
            try { webView.loadUrl(cleanUrl); } catch (Exception ignored) {}
        }
    }

    private void scheduleCompatibilityLoadFallback(String url) {
        final String expectedHost = hostOfUrl(url);
        mainHandler.postDelayed(() -> {
            try {
                if (webView == null) return;
                String active = getEffectiveCurrentUrl();
                String activeHost = hostOfUrl(active);
                if (expectedHost.length() > 0 && activeHost.length() > 0 && sameOrSubDomain(activeHost, expectedHost)) {
                    // Jangan biarkan overlay transisi/search menutup halaman terlalu lama.
                    cancelSmoothSearchTransition();
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (webView.getVisibility() != View.VISIBLE) {
                        if (homeScroll != null) homeScroll.setVisibility(View.GONE);
                        webView.setVisibility(View.VISIBLE);
                    }
                    webView.setAlpha(1f);
                }
            } catch (Exception ignored) {}
        }, 3500);
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
            if (isImageResourceUrl(url) || isMediaResourceUrl(url)) return false;
            String lowerUrl = url == null ? "" : url.toLowerCase(Locale.US);
            if (lowerUrl.contains(".pdf") || lowerUrl.contains("application/pdf")) return false;
            String host = hostOfUrl(url);
            if (host == null || host.length() == 0) return false;
            String h = host.toLowerCase(Locale.US);
            if (h.startsWith("www.")) h = h.substring(4);
            if (h.equals("youtube.com") || h.endsWith(".youtube.com") || h.equals("youtu.be")) return false;
            if (h.equals("google.com") || h.endsWith(".google.com") || h.equals("google.co.id") || h.endsWith(".google.co.id")) return false;
            if (h.equals("bing.com") || h.endsWith(".bing.com")) return false;
            if (h.equals("duckduckgo.com") || h.endsWith(".duckduckgo.com")) return false;
            if (h.equals("startpage.com") || h.endsWith(".startpage.com")) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean shouldRetryCompatibilityRecovery(String url) {
        try {
            String host = hostOfUrl(url);
            if (host == null || host.length() == 0) return false;
            String key = navigationLoopKey(url);
            long now = System.currentTimeMillis();
            if (host.equals(autoCompatibilityRecoveryHost)
                    && key.equals(autoCompatibilityRecoveryKey)
                    && now < autoCompatibilityRecoveryUntilMs) {
                return false;
            }
            autoCompatibilityRecoveryHost = host;
            autoCompatibilityRecoveryKey = key;
            autoCompatibilityRecoveryUntilMs = now + 300000L;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isLikelyBlankCompatibilityReport(String report) {
        try {
            String raw = report == null ? "" : report.trim();
            if (raw.length() == 0) return false;
            String[] parts = raw.split("\\|", -1);
            if (parts.length < 7) return false;
            int textLen = Integer.parseInt(parts[0]);
            int nodeCount = Integer.parseInt(parts[1]);
            int mediaCount = Integer.parseInt(parts[2]);
            int linkCount = Integer.parseInt(parts[3]);
            int htmlLen = Integer.parseInt(parts[4]);
            int scrollHeight = Integer.parseInt(parts[5]);
            String readyState = parts[6];
            boolean complete = "complete".equalsIgnoreCase(readyState) || "interactive".equalsIgnoreCase(readyState);
            if (!complete) return false;
            boolean stronglyBlank = textLen <= 8 && mediaCount == 0 && nodeCount <= 18 && htmlLen <= 1600;
            boolean sparseBlank = textLen <= 20 && mediaCount == 0 && nodeCount <= 30 && linkCount <= 8 && htmlLen <= 3000;
            boolean viewportBlank = textLen <= 8 && mediaCount == 0 && nodeCount <= 40 && scrollHeight >= 300;
            return stronglyBlank || sparseBlank || viewportBlank;
        } catch (Exception e) {
            return false;
        }
    }

    private void scheduleUniversalBlankCompatibilityRecovery(String url) {
        try {
            if (webView == null) return;
            if (!isUniversalCompatibilityCandidateUrl(url)) return;
            final String expectedHost = hostOfUrl(url);
            final String expectedKey = navigationLoopKey(url);
            if (expectedHost.length() == 0 || expectedKey.length() == 0) return;
            mainHandler.postDelayed(() -> {
                try {
                    if (webView == null) return;
                    String activeUrl = getEffectiveCurrentUrl();
                    if (activeUrl == null || activeUrl.length() == 0) activeUrl = currentPageUrlForRequest;
                    String activeHost = hostOfUrl(activeUrl);
                    String activeKey = navigationLoopKey(activeUrl);
                    if (!sameOrSubDomain(activeHost, expectedHost)) return;
                    if (!expectedKey.equals(activeKey)) return;
                    if (isStrictSiteCompatibilityUrl(activeUrl) || isSiteCompatibilityModeActiveForUrl(activeUrl)) return;
                    String js = "(function(){try{var b=document.body, d=document.documentElement; if(!b){return '0|0|0|0|0|0|'+document.readyState;}"
                            + "var t=((b.innerText||'').replace(/\\s+/g,'')).length;"
                            + "var n=b.querySelectorAll('*').length;"
                            + "var m=b.querySelectorAll('img,svg,canvas,video,iframe,object,embed').length;"
                            + "var l=b.querySelectorAll('a').length;"
                            + "var h=(b.innerHTML||'').length;"
                            + "var s=Math.max(b.scrollHeight||0,(d&&d.scrollHeight)||0);"
                            + "return [t,n,m,l,h,s,document.readyState].join('|');"
                            + "}catch(e){return '0|0|0|0|0|0|error';}})();";
                    webView.evaluateJavascript(js, value -> {
                        try {
                            if (webView == null) return;
                            String currentUrl = getEffectiveCurrentUrl();
                            if (currentUrl == null || currentUrl.length() == 0) currentUrl = currentPageUrlForRequest;
                            String currentHost = hostOfUrl(currentUrl);
                            String currentKey = navigationLoopKey(currentUrl);
                            if (!sameOrSubDomain(currentHost, expectedHost)) return;
                            if (!expectedKey.equals(currentKey)) return;
                            String decoded = decodeEvaluateJavascriptString(value);
                            if (!isLikelyBlankCompatibilityReport(decoded)) return;
                            if (!shouldRetryCompatibilityRecovery(currentUrl)) return;
                            enableSiteCompatibilityModeForUrl(currentUrl, "auto-blank-recovery");
                            applyPlainCompatibilitySettingsForUrl(currentUrl);
                            loadCompatibilityUrlWithCurrentMode(currentUrl);
                        } catch (Exception ignored) {
                        }
                    });
                } catch (Exception ignored) {
                }
            }, 1600L);
        } catch (Exception ignored) {
        }
    }

    private boolean isReloadLoopGuardActiveForUrl(String url) {
        long now = System.currentTimeMillis();
        if (reloadLoopGuardHost == null || reloadLoopGuardHost.length() == 0 || now > reloadLoopGuardUntilMs) return false;
        String host = hostOfUrl(url);
        return host.length() > 0 && (host.equals(reloadLoopGuardHost) || host.endsWith("." + reloadLoopGuardHost));
    }

    private boolean isCurrentPageReloadGuarded() {
        String url = getEffectiveCurrentUrl();
        if ((url == null || url.length() == 0) && webView != null) url = webView.getUrl();
        return isReloadLoopGuardActiveForUrl(url);
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

        if (key.equals(reloadLoopLastKey) && (now - reloadLoopWindowStartMs) <= 12000L) {
            reloadLoopCount++;
        } else {
            reloadLoopLastKey = key;
            reloadLoopWindowStartMs = now;
            reloadLoopCount = 1;
        }

        if (reloadLoopCount >= 4) {
            reloadLoopGuardHost = host;
            reloadLoopGuardKey = key;
            reloadLoopGuardUntilMs = now + 120000L;
            enableSiteCompatibilityModeForUrl(url, "reload-loop");
            reloadLoopCount = 0;
            if ((now - reloadLoopToastLastMs) > 6000L) {
                reloadLoopToastLastMs = now;
                Toast.makeText(this, "Reload loop dicegah untuk situs ini", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    private void runPageScript(String js) {
        if (webView == null || js == null || js.length() == 0) return;
        try {
            String code = js;
            if (code.startsWith("javascript:")) code = code.substring("javascript:".length());
            if (Build.VERSION.SDK_INT >= 19) webView.evaluateJavascript(code, null);
            else webView.loadUrl("javascript:" + code);
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

    private boolean isSearchEngineHost(String url) {
        String host = normalizeHostForAdBlock(url);
        if (host.length() == 0) return false;
        return host.equals("google.com") || host.endsWith(".google.com")
                || host.equals("bing.com") || host.endsWith(".bing.com")
                || host.equals("duckduckgo.com") || host.endsWith(".duckduckgo.com")
                || host.equals("yahoo.com") || host.endsWith(".yahoo.com")
                || host.equals("yandex.com") || host.endsWith(".yandex.com");
    }

    private boolean isSearchEngineResultNavigation(String targetUrl, String currentUrl) {
        if (!isHttpOrHttpsUrl(targetUrl) || !isSearchEngineHost(currentUrl)) return false;
        // v0.9.50: hasil pencarian adalah aksi user. Jangan otomatis membatalkan
        // domain yang terlihat seperti ads/direct-link; Java guard berikutnya yang
        // menentukan apakah perlu compatibility mode atau tab sementara.
        return true;
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

    private boolean isKnownPopupHost(String url) {
        String host = normalizeHostForAdBlock(url);
        if (host.length() == 0) return false;

        String[] exactOrContains = new String[]{
                "hotterydiseur", "sewarsremeets", "sewarsremeet", "onclickads", "clickadu", "popads", "popcash",
                "popunder", "adsterra", "propellerads", "hilltopads", "exoclick", "trafficjunky", "juicyads",
                "admaven", "pushpush", "pushengage", "pushwoosh", "realsrv", "invest-tracing", "highperformanceformat",
                "highperformancedisplayformat", "xmladfeed", "rotator", "smartlink", "adnxs", "rubiconproject",
                "taboola", "outbrain", "mgid", "revcontent", "doubleclick", "googlesyndication", "googleadservices"
        };
        for (String s : exactOrContains) {
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

    private boolean isSafeMainFrameUrl(String url) {
        return isHttpOrHttpsUrl(url) && !isLikelyAdClickUrl(url) && !isSuspiciousPopupNavigation(url, lastSafeHttpUrl);
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
        if (isMediaResourceUrl(u) || isYoutubeCoreUrl(u)) return false;
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

    private void restoreAfterBlockedNavigation(WebView view, String blockedUrl, String reason) {
        try {
            if (isTrustedMainFrameNavigation(blockedUrl)) {
                return;
            }
            boolean blockedIsAdLike = isExternalSchemeUrl(blockedUrl) || isKnownPopupHost(blockedUrl)
                    || isLikelyAdClickUrl(blockedUrl) || isSuspiciousPopupNavigation(blockedUrl, lastSafeHttpUrl);
            if (!blockedIsAdLike && (isSiteCompatibilityModeActiveForUrl(blockedUrl) || isSiteCompatibilityModeActiveForUrl(lastSafeHttpUrl)
                    || isReloadLoopGuardActiveForUrl(blockedUrl) || isReloadLoopGuardActiveForUrl(lastSafeHttpUrl))) {
                // v0.9.44/v0.9.46: jangan stopLoading/goBack pada host kompatibel jika targetnya
                // memang halaman situs utama. Popup iklan tetap boleh dipindahkan ke tab sementara.
                return;
            }
            String currentBefore = view != null ? view.getUrl() : "";
            boolean navigationAlreadyChanged = blockedUrl != null
                    && currentBefore != null
                    && currentBefore.length() > 0
                    && blockedUrl.equals(currentBefore);
            boolean directImageNavigation = isDirectImageMainFrameNavigation(blockedUrl, currentBefore);

            // Ad/direct image dipisah ke tab baru, tab utama tidak di-reload agar gambar/komik
            // yang sedang loading tidak berubah menjadi halaman .jpg/.jpeg mentah.
            if (directImageNavigation) {
                captureDirectImageToTempTab(blockedUrl, reason);
            } else {
                captureAdRedirectToTempTab(blockedUrl, reason);
            }

            if (view != null && (navigationAlreadyChanged || directImageNavigation || isExternalSchemeUrl(blockedUrl))) {
                view.stopLoading();
            }

            if (lastSafeHttpUrl != null && lastSafeHttpUrl.length() > 0) {
                addressBar.setText(lastSafeHttpUrl);
            }

            mainHandler.postDelayed(() -> {
                try {
                    if (webView == null) return;

                    String current = webView.getUrl();
                    boolean currentBad = current == null
                            || current.length() == 0
                            || isExternalSchemeUrl(current)
                            || isLikelyAdClickUrl(current)
                            || isDirectImageMainFrameNavigation(current, lastSafeHttpUrl)
                            || (blockedUrl != null && blockedUrl.equals(current));

                    // Hanya recover kalau tab utama sudah benar-benar berubah ke halaman iklan/error.
                    // Kalau masih di halaman komik/asli, jangan reload dan jangan goBack.
                    if (currentBad) {
                        if (webView.canGoBack()) {
                            webView.goBack();
                        } else if (lastSafeHttpUrl != null && lastSafeHttpUrl.length() > 0) {
                            loadBrowserUrl(lastSafeHttpUrl);
                        }
                    }

                    if (lastSafeHttpUrl != null && lastSafeHttpUrl.length() > 0) {
                        addressBar.setText(lastSafeHttpUrl);
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
        if (isMediaResourceUrl(lower) || isYoutubeCoreUrl(lower)) return false;
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

    private boolean isAdUrl(String url) {
        if (!adBlock || url == null) return false;
        String u = url.toLowerCase(Locale.US);

        if (isMediaResourceUrl(u)) return false;
        if (isYoutubeAdMetadataUrl(u)) return true;
        if (isYoutubeCoreUrl(u)) return false;

        String[] blocked = new String[]{
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

        for (String b : blocked) {
            if (u.contains(b)) return true;
        }

        return isYoutubeAdUrl(u) || isPopUnderOrAdAsset(u);
    }

    private boolean isYoutubeAdMetadataUrl(String u) {
        // YouTube mobile/WebView sangat sensitif.
        // Jangan blokir request YouTube/GoogleVideo di network layer karena bisa membuat halaman/video blank.
        // Iklan YouTube ditangani lewat JS speed-skip dan tombol Skip Ad saja.
        return false;
    }

    private boolean isYoutubeAdUrl(String u) {
        // Jangan blokir endpoint internal YouTube/GoogleVideo karena bisa membuat video awal gagal load.
        // Iklan YouTube ditangani lewat cosmetic JS skip/hide yang lebih aman.
        return false;
    }

    private boolean isPopUnderOrAdAsset(String u) {
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
                || u.contains("onclick")
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
                || u.contains("vast")
                || u.contains("vpaid");
    }


    // ===== AdBlock / video ad handling =====
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

        String popupEnabled = adBlockPopupBlocker ? "true" : "false";
        String clickEnabled = adBlockClickHijackBlocker ? "true" : "false";
        String scriptEnabled = adBlockScriptIframeBlocker ? "true" : "false";

        String js = "javascript:(function(){"
                + "window.__yieldAdBlockPremium=true;"
                + "var Y_POPUP=" + popupEnabled + ",Y_CLICK=" + clickEnabled + ",Y_SCRIPT=" + scriptEnabled + ";"
                + "function hostOf(u){try{var a=document.createElement('a');a.href=u;return (a.hostname||'').replace(/^www\\./,'').toLowerCase();}catch(e){return '';}}"
                + "function media(u){try{var s=(u||'').toLowerCase();return s.indexOf('googlevideo.com/videoplayback')>-1||s.indexOf('/videoplayback')>-1||s.indexOf('.mp4')>-1||s.indexOf('.m3u8')>-1||s.indexOf('.mpd')>-1||s.indexOf('.webm')>-1||s.indexOf('.m4s')>-1||s.indexOf('.ts')>-1||s.indexOf('mime=video')>-1||s.indexOf('mime%3dvideo')>-1;}catch(e){return false;}}"
                + "function safeVideoHost(h){return h.indexOf('youtube.com')>-1||h.indexOf('youtu.be')>-1||h.indexOf('googlevideo.com')>-1||h.indexOf('ytimg.com')>-1||h.indexOf('ggpht.com')>-1;}"
                + "function bad(u){try{if(!u||media(u))return false;var s=(u||'').toLowerCase();if(/^[a-z][a-z0-9+.-]*:/.test(s)&&s.indexOf('http://')!==0&&s.indexOf('https://')!==0&&s.indexOf('javascript:')!==0&&s.indexOf('data:')!==0&&s.indexOf('blob:')!==0)return true;if(/(utm_medium=affiliates|deep_and_deferred|navigate_url=|reactpath|click_id|adclick|ad_click|adurl=|af_click|tracking_id|campaign_id)/.test(s))return true;var h=hostOf(u);var cur=(location.hostname||'').replace(/^www\\./,'').toLowerCase();if(!h||h===cur||h.endsWith('.'+cur))return false;if(safeVideoHost(h))return false;if(/(hotterydiseur|sewarsremeets|onclickads|clickadu|popads|popcash|propellerads|adsterra|hilltopads|exoclick|realsrv|invest-tracing|doubleclick|googlesyndication|googleadservices)/.test(h))return true;if(/\\.(cfd|click|cam|monster|quest|buzz|icu|cyou)$/.test(h))return true;if(/\\.(shop|xyz|top|site|space|online|live|fun|lol)$/.test(h)&&/[\\/][a-z0-9_-]{8,}/.test(s))return true;if(/(popunder|popup|redirect|adclick|clickunder|interstitial|push)/.test(s))return true;return false;}catch(e){return false;}}"
                + "function visibleAnchor(a){try{if(!a||!a.getBoundingClientRect)return false;var r=a.getBoundingClientRect();var t=((a.innerText||a.textContent||a.getAttribute('aria-label')||a.title||'')+'').trim();var hasMedia=!!a.querySelector('img,svg,picture,button');return r.width>12&&r.height>8&&(t.length>0||hasMedia);}catch(e){return false;}}"
                + "function allowClickedAnchor(a,u){try{if(!a||!u)return false;var s=(u||'').toLowerCase();if(media(s))return true;var h=hostOf(u);var cur=(location.hostname||'').replace(/^www\\./,'').toLowerCase();if(!h)return false;if(h===cur||h.endsWith('.'+cur))return true;if(!visibleAnchor(a))return false;if(/(intent:|market:|shopeeid:|lazada:|tokopedia:|adclick|ad_click|adurl=|clickunder|popunder|popup|interstitial|push)/.test(s))return false;return true;}catch(e){return false;}}"
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
                + "if(Y_SCRIPT){if(!yt)selectors.forEach(hardHide);}"
                + "try{if(Y_SCRIPT)document.querySelectorAll('iframe[src],script[src]').forEach(function(el){var u=el.src||'';if(bad(u)){el.remove();}});}catch(e){}"
                + "try{document.body.style.setProperty('overflow','auto','important');}catch(e){}"
                + "}"
                + "clean();setInterval(clean,250);setTimeout(clean,120);setTimeout(clean,500);setTimeout(clean,1200);setTimeout(clean,2600);setTimeout(clean,5200);"
                + "})();";
        runPageScript(js);
    }

    private boolean isYouTubePageUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);
        return u.contains("youtube.com") || u.contains("m.youtube.com") || u.contains("www.youtube.com") || u.contains("youtu.be");
    }

    private void injectYouTubeSafeAdBlockV6() {
        if (webView == null || !adBlock) return;
        if (!isYouTubePageUrl(getEffectiveCurrentUrl())) return;
        // v0.9.73: YouTube Auto Cycle Ad Bypass + auto resume main video after ad.
        // Alur: iklan terdeteksi -> bantu klik Skip/Lewati atau majukan +10 detik
        // dengan mekanisme yang sama seperti kontrol +10s Yield -> setelah iklan lewat,
        // engine tidur sampai video utama berjalan sekitar 2 menit -> aktif lagi untuk iklan berikutnya.
        // Tidak memakai playbackRate, mute, force play, currentTime liar saat video utama normal,
        // dan tetap tidak memblokir googlevideo/ytimg/youtubei.
        String js = "javascript:"
                + "(function(){\n"
                + "try{\n"
                + "  var host=(location.hostname||'').toLowerCase();\n"
                + "  if(host.indexOf('youtube.com')<0 && host.indexOf('youtu.be')<0) return;\n"
                + "  var W=window;\n"
                + "  W.__yieldYTAutoCycleV70=W.__yieldYTAutoCycleV70||{installed:false,lastUrl:'',phase:'initial',lastSkip:0,lastAssist:0,lastAdSeen:0,hadAd:false,coolStart:0,coolBase:0,coolTarget:0,lastResume:0};\n"
                + "  var S=W.__yieldYTAutoCycleV70;\n"
                + "  var cur=location.href; if(S.lastUrl!==cur){S.lastUrl=cur;S.phase='initial';S.lastSkip=0;S.lastAssist=0;S.lastAdSeen=0;S.hadAd=false;S.coolStart=0;S.coolBase=0;S.coolTarget=0;}\n"
                + "  function qsa(sel,root){try{return Array.prototype.slice.call((root||document).querySelectorAll(sel));}catch(e){return [];} }\n"
                + "  function txt(el){try{return ((el&&((el.innerText||el.textContent||el.getAttribute('aria-label')||el.getAttribute('title')||'')))+'').replace(/\\s+/g,' ').trim().toLowerCase();}catch(e){return '';} }\n"
                + "  function visible(el){try{if(!el)return false;var r=el.getBoundingClientRect();var cs=getComputedStyle(el);return r.width>2&&r.height>2&&cs.display!=='none'&&cs.visibility!=='hidden'&&parseFloat(cs.opacity||'1')>0.01;}catch(e){return !!el;} }\n"
                + "  function player(){return document.querySelector('#movie_player,.html5-video-player,ytd-player,ytm-player,#player-container-id,#player-container,#player');}\n"
                + "  function video(){try{return document.querySelector('#movie_player video,.html5-video-player video,ytd-player video,ytm-player video,#player video,video');}catch(e){return null;}}\n"
                + "  function inPlayer(el){try{return !!(el&&el.closest&&el.closest('#movie_player,.html5-video-player,ytd-player,ytm-player,#player-container-id,#player-container,#player'));}catch(e){return false;} }\n"
                + "  function playerText(){try{var p=player();return p?txt(p):'';}catch(e){return '';} }\n"
                + "  function adInfo(){try{var p=player();var pt=playerText();var skip=qsa('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytp-skip-ad-button__button,.ytm-ad-skip-button,.videoAdUiSkipButton,.ytp-ad-skip-button-container',p||document).some(visible);var ui=qsa('.ytp-ad-player-overlay,.ytp-ad-player-overlay-instream-info,.ytp-ad-preview-container,.ytp-ad-text,.ytp-ad-simple-ad-badge,.ytp-ad-badge,.video-ads.ytp-ad-module,.ytp-ad-action-interstitial',p||document).some(visible);var word=/(bersponsor|sponsored|kunjungi pengiklan|visit advertiser|lewati iklan|lewati|skip ad|iklan\\s*\\u2022|ad\\s*1\\s*of|ad\\s*2\\s*of|1\\s*dari\\s*2|2\\s*dari\\s*2)/i.test(pt);return {ad:!!(skip||ui||word),skip:skip,ui:ui,word:word};}catch(e){return {ad:false,skip:false,ui:false,word:false};}}\n"
                + "  function fire(el,type){try{var ev;if(type.indexOf('touch')===0&&typeof TouchEvent!=='undefined'){ev=new TouchEvent(type,{bubbles:true,cancelable:true});}else if(type.indexOf('pointer')===0&&typeof PointerEvent!=='undefined'){ev=new PointerEvent(type,{bubbles:true,cancelable:true,pointerId:1,pointerType:'touch',isPrimary:true});}else{ev=new MouseEvent(type,{bubbles:true,cancelable:true,view:window});}el.dispatchEvent(ev);return true;}catch(e){return false;}}\n"
                + "  function strongClick(el){try{if(!el)return false;['pointerover','pointerenter','pointerdown','touchstart','mousedown','pointerup','touchend','mouseup','click'].forEach(function(ev){fire(el,ev);});try{el.click();}catch(e){}return true;}catch(e){try{el.click();return true;}catch(x){return false;}}}\n"
                + "  function coordinateClick(el){try{if(!el||!el.getBoundingClientRect)return false;var r=el.getBoundingClientRect();if(r.width<2||r.height<2)return false;var x=Math.max(1,Math.min(window.innerWidth-2,r.left+r.width/2));var y=Math.max(1,Math.min(window.innerHeight-2,r.top+r.height/2));var target=document.elementFromPoint(x,y)||el;['pointerdown','touchstart','mousedown','pointerup','touchend','mouseup','click'].forEach(function(type){try{var ev;if(type.indexOf('pointer')===0&&typeof PointerEvent!=='undefined'){ev=new PointerEvent(type,{bubbles:true,cancelable:true,clientX:x,clientY:y,pointerId:1,pointerType:'touch',isPrimary:true});}else{ev=new MouseEvent(type,{bubbles:true,cancelable:true,view:window,clientX:x,clientY:y});}target.dispatchEvent(ev);}catch(e){}});try{target.click();}catch(e){}try{if(window.YieldVideoBridge&&window.innerWidth>0&&window.innerHeight>0){window.YieldVideoBridge.tapAtRatio(x/window.innerWidth,y/window.innerHeight);}}catch(e){}return true;}catch(e){return false;}}\n"
                + "  function clickBest(el){try{var best=el;var t=el;for(var i=0;i<7&&t;i++,t=t.parentElement){var tag=(t.tagName||'').toLowerCase();var role=(t.getAttribute&&t.getAttribute('role')||'').toLowerCase();var cls=String(t.className||'').toLowerCase();var tx=txt(t);if(tag==='button'||role==='button'||cls.indexOf('skip')>-1||cls.indexOf('ytp-ad-skip')>-1||tx.indexOf('lewati')>-1||tx.indexOf('skip')>-1){best=t;break;}}var a=strongClick(best), b=coordinateClick(best);return a||b;}catch(e){return strongClick(el)||coordinateClick(el);}}\n"
                + "  function findSkipTargets(){try{var p=player();var roots=[p,document];var out=[];var sels=['.ytp-ad-skip-button','.ytp-ad-skip-button-modern','.ytp-skip-ad-button','.ytp-skip-ad-button__button','.ytm-ad-skip-button','.videoAdUiSkipButton','.ytp-ad-skip-button-container','.ytp-ad-skip-button-text','.ytp-ad-skip-button-icon','button[aria-label]','button','div[role=button]','a[role=button]','tp-yt-paper-button','span','div'];roots.forEach(function(root){if(!root)return;sels.forEach(function(sel){qsa(sel,root).forEach(function(el){if(out.indexOf(el)<0)out.push(el);});});});return out;}catch(e){return [];} }\n"
                + "  function clickSkip(){try{var now=Date.now();if(now-(S.lastSkip||0)<180)return false;var info=adInfo();var list=findSkipTargets();for(var i=0;i<list.length;i++){var el=list[i];if(!visible(el))continue;var t=txt(el), c=String(el.className||'').toLowerCase(), a=String(el.getAttribute&&el.getAttribute('aria-label')||'').toLowerCase();var ok=(t.indexOf('lewati')>-1||t.indexOf('skip')>-1||t.indexOf('abaikan')>-1||a.indexOf('lewati')>-1||a.indexOf('skip')>-1||c.indexOf('skip')>-1||c.indexOf('ytp-ad-skip')>-1);if(!ok)continue;var safe=inPlayer(el)||c.indexOf('skip')>-1||info.ad;if(safe){S.lastSkip=now;var r=clickBest(el);if(r)enterCooldownPending('skip');return r;}}return false;}catch(e){return false;}}\n"
                + "  function resumeMainIfPaused(reason){try{var now=Date.now();var info=adInfo();if(info.ad)return false;var v=video();if(!v||!v.paused)return false;if(now-(S.lastResume||0)<1200)return false;var recentAd=(now-(S.lastAdSeen||0)<14000)||(S.phase==='cooldown_pending')||(S.phase==='cooldown'&&now-(S.coolStart||0)<12000);if(!recentAd)return false;S.lastResume=now;var p=player();var btns=[];['.ytp-large-play-button','.ytp-play-button','button[aria-label*=\"Play\"]','button[aria-label*=\"Putar\"]','button[title*=\"Play\"]','button[title*=\"Putar\"]'].forEach(function(sel){qsa(sel,p||document).forEach(function(b){if(btns.indexOf(b)<0)btns.push(b);});});for(var i=0;i<btns.length;i++){var b=btns[i];var tx=txt(b);var cls=String(b.className||'').toLowerCase();var ar=String(b.getAttribute&&b.getAttribute('aria-label')||'').toLowerCase();if(visible(b)&&(tx.indexOf('pause')<0&&tx.indexOf('jeda')<0&&ar.indexOf('pause')<0&&ar.indexOf('jeda')<0)){clickBest(b);break;}}try{if(v.play)v.play().catch(function(){});}catch(e){}try{if(v.paused&&v.getBoundingClientRect){var r=v.getBoundingClientRect();var x=Math.max(1,Math.min(window.innerWidth-2,r.left+r.width/2));var y=Math.max(1,Math.min(window.innerHeight-2,r.top+r.height/2));if(window.YieldVideoBridge&&window.innerWidth>0&&window.innerHeight>0){window.YieldVideoBridge.tapAtRatio(x/window.innerWidth,y/window.innerHeight);}}}catch(e){}try{v.dispatchEvent(new Event('play'));v.dispatchEvent(new Event('canplay'));}catch(e){}return true;}catch(e){return false;}}\n"
                + "  function yieldForward10Assist(){try{var now=Date.now();if(now-(S.lastAssist||0)<850)return false;var info=adInfo();if(!info.ad)return false;var v=video();if(!v)return false;var cur=(typeof v.currentTime==='number'&&isFinite(v.currentTime))?v.currentTime:0;var dur=(typeof v.duration==='number'&&isFinite(v.duration))?v.duration:999999;var next=Math.max(0,Math.min(dur-0.15,cur+10));if(next<=cur+0.2)return false;S.lastAssist=now;v.currentTime=next;try{v.dispatchEvent(new Event('seeking'));v.dispatchEvent(new Event('timeupdate'));}catch(e){}return true;}catch(e){return false;}}\n"
                + "  function enterCooldownPending(reason){try{S.phase='cooldown_pending';S.hadAd=false;S.coolStart=Date.now();setTimeout(function(){try{if(adInfo().ad){S.phase='assist';return;}var v=video();var ct=(v&&typeof v.currentTime==='number'&&isFinite(v.currentTime))?v.currentTime:0;S.phase='cooldown';S.coolBase=ct;S.coolTarget=ct+120;S.coolStart=Date.now();setTimeout(function(){try{resumeMainIfPaused('cooldown-start');}catch(e){}},120);setTimeout(function(){try{resumeMainIfPaused('cooldown-start-2');}catch(e){}},900);}catch(e){S.phase='cooldown';S.coolStart=Date.now();S.coolTarget=999999;}},2200);}catch(e){}}\n"
                + "  function cooldownDone(){try{if(S.phase!=='cooldown')return false;var v=video();var ct=(v&&typeof v.currentTime==='number'&&isFinite(v.currentTime))?v.currentTime:0;if(ct>=(S.coolTarget||0)||Date.now()-(S.coolStart||0)>150000){S.phase='monitor';return true;}return false;}catch(e){return false;}}\n"
                + "  function run(){try{var now=Date.now();var info=adInfo();if(S.phase==='cooldown_pending'){if(info.ad){S.phase='assist';S.hadAd=true;S.lastAdSeen=now;if(clickSkip())return;yieldForward10Assist();return;}resumeMainIfPaused('pending');return;}if(S.phase==='cooldown'){if(info.ad){S.phase='assist';S.hadAd=true;S.lastAdSeen=now;if(clickSkip())return;yieldForward10Assist();return;}resumeMainIfPaused('cooldown');cooldownDone();return;}if(info.ad){S.phase='assist';S.hadAd=true;S.lastAdSeen=now;if(clickSkip())return;yieldForward10Assist();return;}if(S.phase==='assist'&&S.hadAd&&now-(S.lastAdSeen||0)>2600){enterCooldownPending('ad-ended');setTimeout(function(){try{resumeMainIfPaused('after-ad-ended');}catch(e){}},1000);return;}var v=video();var ct=(v&&typeof v.currentTime==='number'&&isFinite(v.currentTime))?v.currentTime:0;if(S.phase==='initial'&&ct>8){enterCooldownPending('main-start');return;}}catch(e){}}\n"
                + "  W.__yieldYTAutoCycleRun=run;\n"
                + "  if(!S.installed){S.installed=true;try{var timer=null;var mo=new MutationObserver(function(){clearTimeout(timer);timer=setTimeout(run,60);});mo.observe(document.documentElement||document,{childList:true,subtree:true,attributes:true,attributeFilter:['class','style','aria-label','title']});}catch(e){} ['yt-navigate-start','yt-navigate-finish','yt-page-data-updated','spfdone','visibilitychange','touchstart','touchend','pointerup','click','play','timeupdate'].forEach(function(ev){try{document.addEventListener(ev,function(){setTimeout(run,40);setTimeout(run,170);setTimeout(run,600);},true);}catch(e){}});setInterval(run,420);}\n"
                + "  setTimeout(run,30);setTimeout(run,160);setTimeout(run,420);setTimeout(run,1000);setTimeout(run,2200);setTimeout(run,4200);\n"
                + "}catch(e){}\n"
                + "})();\n";
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
            showHome();
            return;
        }
        String url;
        if (text.startsWith("http://") || text.startsWith("https://")) url = text;
        else if (text.contains(".") && !text.contains(" ")) url = "https://" + text;
        else url = buildSearchUrl(text);

        TabInfo currentTab = getCurrentTab();
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
            if (homeScroll != null && homeScroll.getVisibility() == View.VISIBLE) {
                restoreHiddenWebPage();
                return;
            }

            if (webView != null && webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
                webView.goBack();
                return;
            }

            showHome();
        } catch (Exception ignored) {
        }
    }

    private void navigateSwipeForward() {
        try {
            if (webView != null && webView.getVisibility() == View.VISIBLE && webView.canGoForward()) {
                webView.goForward();
                return;
            }

            if (homeScroll != null && homeScroll.getVisibility() == View.VISIBLE) {
                if (webView != null && webView.canGoForward()) {
                    restoreHiddenWebPage();
                    webView.goForward();
                } else {
                    restoreHiddenWebPage();
                }
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
                Toast.makeText(this, "Belum ada halaman sebelumnya", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Halaman terakhir dibuka lagi", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Tidak ada halaman untuk dibuka", Toast.LENGTH_SHORT).show();
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

    private void showHome() {
        cancelSmoothSearchTransition();
        if (videoLandscapeModeActive) {
            exitVideoLandscapeMode(false);
        }
        // Home hanya menyembunyikan halaman web, bukan menghapus state halaman.
        // Jadi kalau tidak sengaja kepencet Home, halaman terakhir masih bisa dikembalikan lewat gesture.
        // Saat tab baru/privat baru dibuat, jangan simpan URL lama ke tab kosong baru.
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

        homeScroll.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
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

    private void saveBookmarks(Set<String> bookmarks) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>(bookmarks);
        ArrayList<BookmarkItemData> next = new ArrayList<>();
        for (String url : normalized) {
            BookmarkItemData ex = findBookmarkByUrl(url);
            if (ex != null) next.add(ex);
            else next.add(new BookmarkItemData(guessLabelFromUrl(url), url, "Bookmark seluler", System.currentTimeMillis()));
        }
        bookmarkData.clear();
        bookmarkData.addAll(next);
        saveBookmarkData();
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
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        speedMode = p.getBoolean("speedMode", false);
        safeMode = p.getBoolean("safeMode", true);
        nightMode = p.getBoolean("nightMode", true);
        nightModeOption = p.getString("nightModeOption", nightMode ? "ON" : "OFF");
        hideGoogleTranslateBar = p.getBoolean("hideGoogleTranslateBar", true);
        translateTargetLang = p.getString("translateTargetLang", "id");
        translateTargetLabel = p.getString("translateTargetLabel", translateLanguageLabel(translateTargetLang));
        nightModeExceptions.clear();
        nightModeExceptions.addAll(p.getStringSet(KEY_NIGHT_EXCEPTIONS, new HashSet<>()));
        readerMode = p.getBoolean("readerMode", false);
        adBlock = p.getBoolean("adBlock", true);
        adBlockPopupBlocker = p.getBoolean("adBlockPopupBlocker", true);
        adBlockRedirectBlocker = p.getBoolean("adBlockRedirectBlocker", true);
        adBlockScriptIframeBlocker = p.getBoolean("adBlockScriptIframeBlocker", true);
        adBlockClickHijackBlocker = p.getBoolean("adBlockClickHijackBlocker", true);
        if (!p.getBoolean("adBlockDefaultOnV095", false)) {
            adBlock = true;
            p.edit()
                    .putBoolean("adBlock", true)
                    .putBoolean("adBlockDefaultOnV095", true)
                    .apply();
        }
        adBlockRedirectToTempTab = p.getBoolean("adBlockRedirectToTempTab", true);
        adBlockAutoCloseAdTabs = p.getBoolean("adBlockAutoCloseAdTabs", true);
        dataSaver = p.getBoolean("dataSaver", false);
        desktopMode = p.getBoolean("desktopMode", false);
        forceMobileModeAfterUpdateIfNeeded(p);
        textZoom = p.getInt("textZoom", 100);
        shortcutDownload = p.getBoolean("shortcutDownload", true);
        shortcutReloadWebsite = p.getBoolean("shortcutReloadWebsite", true);
        shortcutBookmark = p.getBoolean("shortcutBookmark", false);
        shortcutPrivate = p.getBoolean("shortcutPrivate", true);
        shortcutAdBlock = p.getBoolean("shortcutAdBlock", true);
        shortcutReader = p.getBoolean("shortcutReader", false);
        shortcutNightMode = p.getBoolean("shortcutNightMode", false);
        shortcutQrScan = p.getBoolean("shortcutQrScan", false);
        shortcutHistory = p.getBoolean("shortcutHistory", true);
        shortcutFindPage = p.getBoolean("shortcutFindPage", false);
        shortcutShare = p.getBoolean("shortcutShare", false);
        shortcutFullscreen = p.getBoolean("shortcutFullscreen", false);
        videoControlsEnabled = p.getBoolean("videoControlsEnabled", true);
        videoBufferBooster = p.getBoolean("videoBufferBooster", true);
        hlsSegmentPrefetch = p.getBoolean("hlsSegmentPrefetch", true);
        videoFloatingPlayer = p.getBoolean("videoFloatingPlayer", false);
        if (!p.getBoolean("disableAutoPipOnMinimizeV0910", false)) {
            videoFloatingPlayer = false;
            p.edit()
                    .putBoolean("videoFloatingPlayer", false)
                    .putBoolean("disableAutoPipOnMinimizeV0910", true)
                    .apply();
        }
        videoBackgroundPlay = p.getBoolean("videoBackgroundPlay", true);
        shortcutVideoControls = p.getBoolean("shortcutVideoControls", false);
        videoSpeed = p.getFloat("videoSpeed", 1.0f);
        selectedVideoQuality = p.getString("selectedVideoQuality", "Auto");
        downloadSubfolder = p.getString("downloadSubfolder", "Download");
        selectedDownloadTreeUri = p.getString("selectedDownloadTreeUri", "");
        downloadDynamic4Connections = p.getBoolean("downloadDynamic4Connections", true);
        downloadAutoRetry = p.getBoolean("downloadAutoRetry", true);
        downloadHlsEnabled = p.getBoolean("downloadHlsEnabled", true);
        downloadSpeedLimitKBps = p.getInt("downloadSpeedLimitKBps", 0);
        downloadQueueEnabled = p.getBoolean("downloadQueueEnabled", true);
        downloadMaxActive = Math.max(2, Math.min(4, p.getInt("downloadMaxActive", 2)));
        topIconReload = p.getBoolean("topIconReload", true);
        topIconBookmark = p.getBoolean("topIconBookmark", true);
        topIconTranslate = p.getBoolean("topIconTranslate", true);
        searchEngine = p.getString("searchEngine", "Google");

        if (!p.getBoolean("menuDefaultsV030", false)) {
            shortcutDownload = true;
            shortcutReloadWebsite = true;
            shortcutBookmark = false;
            shortcutPrivate = true;
            shortcutAdBlock = true;
            shortcutReader = false;
            shortcutNightMode = false;
            shortcutQrScan = false;
            shortcutHistory = true;
            shortcutFindPage = false;
            shortcutShare = false;
            shortcutFullscreen = false;
            shortcutVideoControls = false;

            p.edit()
                    .putBoolean("shortcutDownload", shortcutDownload)
                    .putBoolean("shortcutReloadWebsite", shortcutReloadWebsite)
                    .putBoolean("shortcutBookmark", shortcutBookmark)
                    .putBoolean("shortcutPrivate", shortcutPrivate)
                    .putBoolean("shortcutAdBlock", shortcutAdBlock)
                    .putBoolean("shortcutReader", shortcutReader)
                    .putBoolean("shortcutNightMode", shortcutNightMode)
                    .putBoolean("shortcutQrScan", shortcutQrScan)
                    .putBoolean("shortcutHistory", shortcutHistory)
                    .putBoolean("shortcutFindPage", shortcutFindPage)
                    .putBoolean("shortcutShare", shortcutShare)
                    .putBoolean("shortcutFullscreen", shortcutFullscreen)
                    .putBoolean("shortcutVideoControls", shortcutVideoControls)
                    .putBoolean("menuDefaultsV030", true)
                    .apply();
        }
    }

    private void saveSettings() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean("speedMode", speedMode)
                .putBoolean("safeMode", safeMode)
                .putBoolean("hideGoogleTranslateBar", hideGoogleTranslateBar)
                .putString("translateTargetLang", translateTargetLang)
                .putString("translateTargetLabel", translateTargetLabel)
                .putBoolean("nightMode", !"OFF".equals(nightModeOption))
                .putString("nightModeOption", nightModeOption)
                .putStringSet(KEY_NIGHT_EXCEPTIONS, new HashSet<>(nightModeExceptions))
                .putBoolean("readerMode", readerMode)
                .putBoolean("adBlock", adBlock)
                .putBoolean("adBlockPopupBlocker", adBlockPopupBlocker)
                .putBoolean("adBlockRedirectBlocker", adBlockRedirectBlocker)
                .putBoolean("adBlockScriptIframeBlocker", adBlockScriptIframeBlocker)
                .putBoolean("adBlockClickHijackBlocker", adBlockClickHijackBlocker)
                .putBoolean("adBlockDefaultOnV095", true)
                .putBoolean("adBlockRedirectToTempTab", adBlockRedirectToTempTab)
                .putBoolean("adBlockAutoCloseAdTabs", adBlockAutoCloseAdTabs)
                .putBoolean("dataSaver", dataSaver)
                .putBoolean("desktopMode", desktopMode)
                .putInt("textZoom", textZoom)
                .putBoolean("shortcutDownload", shortcutDownload)
                .putBoolean("shortcutReloadWebsite", shortcutReloadWebsite)
                .putBoolean("shortcutBookmark", shortcutBookmark)
                .putBoolean("shortcutPrivate", shortcutPrivate)
                .putBoolean("shortcutAdBlock", shortcutAdBlock)
                .putBoolean("shortcutReader", shortcutReader)
                .putBoolean("shortcutNightMode", shortcutNightMode)
                .putBoolean("shortcutQrScan", shortcutQrScan)
                .putBoolean("shortcutHistory", shortcutHistory)
                .putBoolean("shortcutFindPage", shortcutFindPage)
                .putBoolean("shortcutShare", shortcutShare)
                .putBoolean("shortcutFullscreen", shortcutFullscreen)
                .putBoolean("videoControlsEnabled", videoControlsEnabled)
                .putBoolean("videoBufferBooster", videoBufferBooster)
                .putBoolean("hlsSegmentPrefetch", hlsSegmentPrefetch)
                .putBoolean("videoFloatingPlayer", videoFloatingPlayer)
                .putBoolean("disableAutoPipOnMinimizeV0910", true)
                .putBoolean("videoBackgroundPlay", videoBackgroundPlay)
                .putBoolean("shortcutVideoControls", shortcutVideoControls)
                .putFloat("videoSpeed", videoSpeed)
                .putString("selectedVideoQuality", selectedVideoQuality)
                .putString("downloadSubfolder", downloadSubfolder)
                .putString("selectedDownloadTreeUri", selectedDownloadTreeUri)
                .putBoolean("downloadDynamic4Connections", downloadDynamic4Connections)
                .putBoolean("downloadAutoRetry", downloadAutoRetry)
                .putBoolean("downloadHlsEnabled", downloadHlsEnabled)
                .putInt("downloadSpeedLimitKBps", downloadSpeedLimitKBps)
                .putBoolean("downloadQueueEnabled", downloadQueueEnabled)
                .putInt("downloadMaxActive", downloadMaxActive)
                .putBoolean("topIconReload", topIconReload)
                .putBoolean("topIconBookmark", topIconBookmark)
                .putBoolean("topIconTranslate", topIconTranslate)
                .putString("searchEngine", searchEngine)
                .putBoolean("menuDefaultsV030", true)
                .apply();
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

    private boolean hasActiveWebVideo() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) return false;
        final boolean[] result = new boolean[]{false};
        try {
            webView.evaluateJavascript("(function(){try{var v=document.querySelector('video');return !!(v&&!v.paused&&!v.ended&&v.readyState>1);}catch(e){return false;}})()", value -> {
                result[0] = "true".equals(value);
            });
        } catch (Exception ignored) {}
        return result[0];
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

        if (videoLandscapeModeActive) {
            exitVideoLandscapeMode(false);
            forcePortraitAfterVideoFullscreen();
            return;
        }

        if (topBarView != null && topBarView.getVisibility() == View.GONE) {
            exitFullscreenMode();
            return;
        }
        if (webView.getVisibility() == View.VISIBLE && webView.canGoBack()) webView.goBack();
        else if (webView.getVisibility() == View.VISIBLE) showHome();
        else super.onBackPressed();
    }


    // ===== Small UI helpers =====
    private View space(int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, height));
        return v;
    }

    private GradientDrawable roundRect(int fillColor, int radius, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}