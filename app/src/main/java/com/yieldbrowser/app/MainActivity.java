
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
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
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
    private static final String KEY_NIGHT_EXCEPTIONS = "night_mode_exceptions";
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
    private ImageButton reloadButton;
    private ImageButton bookmarkButton;
    private ImageButton translateButton;
    private View topBarView;
    private View bottomNavView;
    private LinearLayout videoControlsBar;
    private TextView videoSpeedLabel;
    private TextView videoQualityLabel;
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
    private TextView tabsCountText;
    private float swipeStartX = 0f;
    private float swipeStartY = 0f;
    private long swipeStartTime = 0L;

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
    private String translateTargetLang = "id";
    private String translateTargetLabel = "Indonesia";
    private boolean speedMode = false;
    private boolean safeMode = true;
    private boolean nightMode = true;
    private String nightModeOption = "ON";
    private final Set<String> nightModeExceptions = new HashSet<>();
    private boolean readerMode = false;
    private boolean adBlock = false;
    private boolean adBlockPopupBlocker = true;
    private boolean adBlockRedirectBlocker = true;
    private boolean adBlockScriptIframeBlocker = true;
    private boolean adBlockClickHijackBlocker = true;
    private boolean adBlockRedirectToTempTab = true;
    private boolean adBlockAutoCloseAdTabs = true;
    private boolean dataSaver = false;
    private boolean desktopMode = false;
    private int textZoom = 100;

    private boolean shortcutDownload = true;
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
    private boolean topIconReload = true;
    private boolean topIconBookmark = true;
    private boolean topIconTranslate = true;

    private final ArrayList<DownloadItem> downloadItems = new ArrayList<>();
    private final ArrayList<ShortcutItemData> shortcutsData = new ArrayList<>();
    private final ArrayList<HistoryItemData> historyData = new ArrayList<>();
    private final ArrayList<BookmarkItemData> bookmarkData = new ArrayList<>();
    private final ArrayList<TabInfo> tabs = new ArrayList<>();
    private int currentTabIndex = 0;
    private LinearLayout shortcutContainer;
    private String searchEngine = "Google";

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
        double speedBytesPerSecond = 0;
        long lastSpeedTimeMs = 0;
        long lastSpeedBytes = 0;
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

        TabInfo(String title, String url, boolean privateTab) {
            this(title, url, privateTab, false);
        }

        TabInfo(String title, String url, boolean privateTab, boolean adTab) {
            this.title = title;
            this.url = url;
            this.privateTab = privateTab;
            this.adTab = adTab;
        }
    }

    private class TranslateBridge {
        @JavascriptInterface
        public void translateText(int index, String text) {
            if (text == null) return;
            final String clean = text.trim();
            if (clean.length() < 2 || clean.length() > 450) return;

            new Thread(() -> {
                String translated = translateTextViaGoogle(clean);
                if (translated == null || translated.trim().length() == 0) return;
                runOnUiThread(() -> applyCompatibleTranslation(index, translated));
            }).start();
        }

        @JavascriptInterface
        public void onCollected(int count) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Translate kompatibel berjalan: " + count + " teks", Toast.LENGTH_SHORT).show());
        }
    }

    private class VideoBridge {
        @JavascriptInterface
        public void onVideoPlaying() {
            runOnUiThread(() -> showVideoControlsIfAllowed());
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
                if (videoControlsBar != null) videoControlsBar.setVisibility(View.GONE);
            });
        }
    }

    private class AdBlockBridge {
        @JavascriptInterface
        public void onAdRedirect(String url) {
            runOnUiThread(() -> captureAdRedirectToTempTab(url, "Popup iklan"));
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();
        loadDownloadHistory();
        loadShortcuts();
        loadBrowserHistory();
        loadBookmarkData();
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

        FrameLayout contentFrame = new FrameLayout(this);
        contentFrame.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1));

        homeScroll = createHomeContent();
        contentFrame.addView(homeScroll, new FrameLayout.LayoutParams(-1, -1));

        webView = new WebView(this);
        webView.setVisibility(View.GONE);
        configureWebView();
        contentFrame.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        root.addView(contentFrame);

        videoControlsBar = createVideoControlsBar();
        videoControlsBar.setVisibility(View.GONE);
        root.addView(videoControlsBar, new LinearLayout.LayoutParams(-1, dp(58)));

        bottomNavView = createBottomNav();
        root.addView(bottomNavView, new LinearLayout.LayoutParams(-1, dp(64)));

        setContentView(root);
        installSwipeNavigation(root);
        updateTopActionStates();
        handleOpenDownloadsIntent(getIntent());

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
        if (videoBackgroundPlay && webView != null) {
            try {
                webView.evaluateJavascript("(function(){try{var v=document.querySelector('video');if(v&&!v.paused){v.play().catch(function(){});}return 'keep';}catch(e){return 'err';}})()", null);
            } catch (Exception ignored) {
            }
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleOpenDownloadsIntent(getIntent());
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
        searchButton.setOnClickListener(v -> openHomeSearchUrl());
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

    private void saveCurrentTabState() {
        if (tabs.isEmpty()) return;
        TabInfo tab = getCurrentTab();
        try {
            if (webView != null && webView.getVisibility() == View.VISIBLE && webView.getUrl() != null) {
                String url = extractOriginalUrl(webView.getUrl());
                tab.url = url == null ? "" : url;
                String title = webView.getTitle();
                if (title != null && title.trim().length() > 0) tab.title = title;
                else if (tab.url != null && tab.url.length() > 0) tab.title = tab.url;
            } else if (addressBar != null && addressBar.getText() != null) {
                String maybeUrl = normalizeInputToUrl(addressBar.getText().toString().trim());
                if (maybeUrl != null) tab.url = maybeUrl;
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
        updateTabsCountUi();
        addressBar.setText("");
        if (homeSearchInput != null) homeSearchInput.setText("");
        showHome();
        Toast.makeText(this, "Tab baru dibuat", Toast.LENGTH_SHORT).show();
    }

    private void newPrivateTab() {
        saveCurrentTabState();
        tabs.add(new TabInfo("Tab privat", "", true));
        currentTabIndex = tabs.size() - 1;
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
        showHome();
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
            showHome();
        } else {
            addressBar.setText(tab.url);
            webView.setVisibility(View.VISIBLE);
            homeScroll.setVisibility(View.GONE);
            updateVideoControlsVisibility();
            if (translateEnabled) loadTranslatedPage(tab.url);
            else webView.loadUrl(tab.url);
        }

        Toast.makeText(this, tab.privateTab ? "Tab privat" : "Tab aktif", Toast.LENGTH_SHORT).show();
    }

    private void closeTab(int index) {
        if (tabs.isEmpty() || index < 0 || index >= tabs.size()) return;

        boolean closingCurrent = index == currentTabIndex;
        TabInfo removed = tabs.remove(index);

        if (tabs.isEmpty()) {
            tabs.add(new TabInfo("Tab utama", "", false));
            currentTabIndex = 0;
            addressBar.setText("");
            showHome();
        } else {
            if (currentTabIndex >= tabs.size()) currentTabIndex = tabs.size() - 1;
            if (closingCurrent) {
                switchToTab(currentTabIndex);
            }
        }

        if (removed.privateTab) {
            try {
                if (webView != null) {
                    webView.clearHistory();
                    webView.clearCache(false);
                }
            } catch (Exception ignored) {
            }
        }

        updateTabsCountUi();
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
            dialog.dismiss();
            showTabsPanel();
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
                dialog.dismiss();
                showDownloadManager();
            }));
        }
        if (shortcutBookmark) {
            menu.addView(menuRow(R.drawable.ic_bookmark, "Bookmark", v -> {
                dialog.dismiss();
                showBookmarkList();
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
                dialog.dismiss();
                showNightModeSettingsDialog();
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
                dialog.dismiss();
                showHistoryPanel();
            }));
        }
        if (shortcutFindPage) {
            menu.addView(menuRow(R.drawable.ic_find_page, "Cari di halaman", v -> {
                dialog.dismiss();
                showFindInPageDialog();
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

        menu.addView(menuRow(R.drawable.ic_refresh, "Reload website", v -> {
            dialog.dismiss();
            reloadCurrentWebsite();
        }));

        menu.addView(menuDivider());
        menu.addView(menuRow(R.drawable.ic_settings, "Setelan", v -> {
            dialog.dismiss();
            showSettingsPanel();
        }));
        menu.addView(menuRow(R.drawable.ic_customize, "Sesuaikan menu", v -> {
            dialog.dismiss();
            showCustomizeMenuPanel();
        }));
        menu.addView(menuRow(R.drawable.ic_exit, "Keluar", v -> {
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
        return "0.9.0";
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
            dialog.dismiss();
            showDownloadSettingsPanel();
        }));
        panel.addView(actionRow(R.drawable.ic_bookmark, "Bookmark", "Buka daftar bookmark yang tersimpan.", v -> {
            dialog.dismiss();
            showBookmarkList();
        }));
        panel.addView(actionRow(R.drawable.ic_private, "Privat", "Buka tab privat tanpa menyimpan riwayat.", v -> {
            dialog.dismiss();
            newPrivateTab();
        }));
        panel.addView(actionRow(R.drawable.ic_customize, "Sesuaikan menu", "Atur shortcut yang muncul di menu utama.", v -> {
            dialog.dismiss();
            showCustomizeMenuPanel();
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
            dialog.dismiss();
            showHistoryPanel();
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
            dialog.dismiss();
            showSettingsPanel();
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
        panel.addView(settingRow(R.drawable.ic_desktop, "Desktop mode", "Paksa tampilan lebar seperti PC/laptop.", desktopMode, v -> { desktopMode = !desktopMode; applyBrowserSettings(); saveSettings(); if (webView != null && webView.getVisibility() == View.VISIBLE) webView.reload(); }));
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
                    parentDialog.dismiss();
                    showSettingsPanel();
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
            dialog.dismiss();
            showDownloadManager();
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
            dialog.dismiss();
            showDownloadSettingsPanel();
        }));

        panel.addView(actionRow(R.drawable.ic_settings, "Fitur download lanjutan", getAdvancedDownloadSummary(), v -> {
            showAdvancedDownloadFeaturesDialog(dialog);
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
        title.setText("Fitur UC Download");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText("Pengaturan download lanjutan Yield.");
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
    }

    private LinearLayout createVideoControlsBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(6), dp(8), dp(6));
        bar.setBackgroundColor(Color.parseColor("#101217"));

        bar.addView(videoTextButton("−10s", "Mundur 10 detik", v -> seekVideoBySeconds(-10)));
        bar.addView(videoButton(R.drawable.ic_video_play, "Play", v -> controlVideo("play")));
        bar.addView(videoButton(R.drawable.ic_video_pause, "Pause", v -> controlVideo("pause")));
        bar.addView(videoTextButton("+10s", "Maju 10 detik", v -> seekVideoBySeconds(10)));

        videoSpeedLabel = new TextView(this);
        videoSpeedLabel.setText("1x");
        videoSpeedLabel.setTextColor(Color.parseColor("#111111"));
        videoSpeedLabel.setTextSize(14);
        videoSpeedLabel.setTypeface(Typeface.DEFAULT_BOLD);
        videoSpeedLabel.setGravity(Gravity.CENTER);
        videoSpeedLabel.setBackground(roundRect(COLOR_ACCENT, dp(18), 0, Color.TRANSPARENT));
        videoSpeedLabel.setOnClickListener(v -> showVideoSpeedDialog());
        LinearLayout.LayoutParams speedParams = new LinearLayout.LayoutParams(dp(52), dp(42));
        speedParams.setMargins(dp(4), 0, 0, 0);
        bar.addView(videoSpeedLabel, speedParams);

        videoQualityLabel = new TextView(this);
        videoQualityLabel.setText(selectedVideoQuality == null ? "Auto" : selectedVideoQuality);
        videoQualityLabel.setTextColor(Color.WHITE);
        videoQualityLabel.setTextSize(12);
        videoQualityLabel.setTypeface(Typeface.DEFAULT_BOLD);
        videoQualityLabel.setGravity(Gravity.CENTER);
        videoQualityLabel.setBackground(roundRect(Color.parseColor("#20232A"), dp(18), dp(1), COLOR_BORDER));
        videoQualityLabel.setOnClickListener(v -> showVideoQualityDialog());
        LinearLayout.LayoutParams qualityParams = new LinearLayout.LayoutParams(dp(56), dp(42));
        qualityParams.setMargins(dp(4), 0, 0, 0);
        bar.addView(videoQualityLabel, qualityParams);

        TextView full = new TextView(this);
        full.setText("Full");
        full.setTextColor(Color.WHITE);
        full.setTextSize(12);
        full.setTypeface(Typeface.DEFAULT_BOLD);
        full.setGravity(Gravity.CENTER);
        full.setBackground(roundRect(Color.parseColor("#20232A"), dp(18), dp(1), COLOR_BORDER));
        full.setOnClickListener(v -> enterVideoFullscreen());
        LinearLayout.LayoutParams fullParams = new LinearLayout.LayoutParams(dp(48), dp(42));
        fullParams.setMargins(dp(4), 0, 0, 0);
        bar.addView(full, fullParams);

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(Color.WHITE);
        close.setTextSize(22);
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
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(34), dp(42));
        closeParams.setMargins(dp(4), 0, 0, 0);
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

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(42));
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
        wrap.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(46), dp(42));
        params.setMargins(dp(4), 0, dp(4), 0);
        wrap.setLayoutParams(params);
        return wrap;
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
        webView.loadUrl(js);
        Toast.makeText(this, (seconds > 0 ? "Maju " : "Mundur ") + Math.abs(seconds) + " detik", Toast.LENGTH_SHORT).show();
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

    private void enterVideoFullscreen() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) {
            Toast.makeText(this, "Buka video dulu", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Mode layar penuh video", Toast.LENGTH_SHORT).show();
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
            videoControlsManualHidden = false;
            checkAndShowVideoControls();
            Toast.makeText(this, "Mode layar penuh aktif. Tekan Back untuk keluar.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Mode fullscreen tidak didukung di halaman ini", Toast.LENGTH_SHORT).show();
        }
    }

    private void exitAppVideoFullscreenFallback() {
        try {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
            setRequestedOrientation(originalRequestedOrientation);
            restoreVideoControlsFromFullscreenOverlay();
            if (topBarView != null) topBarView.setVisibility(View.VISIBLE);
            if (bottomNavView != null) bottomNavView.setVisibility(View.VISIBLE);
            checkAndShowVideoControls();
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

            FrameLayout decor = (FrameLayout) getWindow().getDecorView();
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            );
            lp.setMargins(0, 0, 0, 0);
            decor.addView(videoControlsBar, lp);

            videoControlsInFullscreen = true;
            videoControlsManualHidden = false;
            videoControlsBar.bringToFront();
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

            videoControlsInFullscreen = false;
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
        if (videoFloatingPlayer) items.add("floating");
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

        box.addView(videoOptSwitchRow("Floating player", "Tombol layar penuh/floating ringan; pakai PiP Android jika tersedia.", videoFloatingPlayer, v -> {
            videoFloatingPlayer = !videoFloatingPlayer;
            saveSettings();
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
            webView.loadUrl(js);
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
            js = "javascript:(function(){var v=document.querySelector('video');if(v){v.play();'play';}else{'no_video';}})()";
        } else if ("pause".equals(action)) {
            js = "javascript:(function(){var v=document.querySelector('video');if(v){v.pause();'pause';}else{'no_video';}})()";
        } else {
            js = "javascript:(function(){var v=document.querySelector('video');if(v){v.pause();try{v.currentTime=0;}catch(e){}'stop';}else{'no_video';}})()";
        }
        webView.loadUrl(js);
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
        clearText.setText("Hapus data penjelajahan...");
        clearText.setTextColor(Color.parseColor("#F97352"));
        clearText.setTextSize(15);
        clearText.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(-1, -2);
        clearParams.setMargins(0, dp(16), 0, dp(12));
        root.addView(clearText, clearParams);
        clearText.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Hapus riwayat")
                    .setMessage("Hapus semua riwayat browsing?")
                    .setPositiveButton("Hapus", (d, w) -> {
                        historyData.clear();
                        saveBrowserHistory();
                        dialog.dismiss();
                        showHistoryPanel();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);

        if (historyData.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Riwayat masih kosong.");
            empty.setTextColor(COLOR_SUBTEXT);
            empty.setTextSize(16);
            empty.setPadding(0, dp(20), 0, 0);
            list.addView(empty);
        } else {
            String lastHeader = "";
            for (HistoryItemData item : historyData) {
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
                list.addView(historyRow(item, dialog));
            }
        }

        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        dialog.setContentView(root);
        dialog.show();
        applyDarkFullscreenDialog(dialog);
    }

    private View historyRow(HistoryItemData item, Dialog dialog) {
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
        delete.setOnClickListener(v -> {
            historyData.remove(item);
            saveBrowserHistory();
            dialog.dismiss();
            showHistoryPanel();
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
        ImageButton addFolder = plainIconButton(R.drawable.ic_add_tab, v -> showCreateBookmarkFolderDialog(() -> { dialog.dismiss(); showBookmarkHomePanel(); }));
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

        ImageButton back = plainIconButton(R.drawable.ic_back, v -> { dialog.dismiss(); showBookmarkHomePanel(); });
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
        ImageButton addFolder = plainIconButton(R.drawable.ic_add_tab, v -> showCreateBookmarkFolderDialog(() -> { dialog.dismiss(); showBookmarkHomePanel(); }));
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

        Runnable render = () -> {
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
                for (BookmarkItemData item : items) list.addView(bookmarkItemRow(item, dialog));
            }
        };
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){ render.run(); }
            public void afterTextChanged(android.text.Editable s){}
        });
        render.run();

        dialog.setContentView(root);
        dialog.show();
        applyDarkFullscreenDialog(dialog);
    }

    private View bookmarkFolderRow(String folder, int count, Dialog parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(14), 0, dp(14));
        row.setOnClickListener(v -> { parent.dismiss(); showBookmarkFolderPanel(folder); });

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

    private View bookmarkItemRow(BookmarkItemData item, Dialog dialog) {
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
        more.setOnClickListener(v -> showBookmarkItemMenu(v, item, dialog));
        row.addView(more, new LinearLayout.LayoutParams(dp(36), dp(36)));
        return row;
    }

    private void showBookmarkItemMenu(View anchor, BookmarkItemData item, Dialog dialog) {
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
                showEditBookmarkDialog(item, dialog);
            } else if ("Salin link".equals(t)) {
                ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cb != null) cb.setPrimaryClip(ClipData.newPlainText("bookmark", item.url));
                Toast.makeText(this, "Link bookmark disalin", Toast.LENGTH_SHORT).show();
            } else if ("Pindahkan ke...".equals(t)) {
                showMoveBookmarkDialog(item, dialog);
            } else if ("Hapus".equals(t)) {
                bookmarkData.remove(item);
                saveBookmarkData();
                dialog.dismiss();
                showBookmarkFolderPanel(item.folder);
            } else if ("Berpindah ke atas".equals(t)) {
                bookmarkData.remove(item);
                bookmarkData.add(0, item);
                saveBookmarkData();
                dialog.dismiss();
                showBookmarkFolderPanel(item.folder);
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

    private void showEditBookmarkDialog(BookmarkItemData item, Dialog dialog) {
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
                    dialog.dismiss();
                    showBookmarkFolderPanel(item.folder);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showMoveBookmarkDialog(BookmarkItemData item, Dialog dialog) {
        java.util.ArrayList<String> folders = new java.util.ArrayList<>(getBookmarkFolders());
        String[] arr = folders.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Pindahkan ke folder")
                .setItems(arr, (d,w) -> {
                    item.folder = arr[w];
                    saveBookmarkData();
                    dialog.dismiss();
                    showBookmarkFolderPanel(item.folder);
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

    private void addBrowserHistory(String title, String url) {
        if (url == null || url.length() == 0 || url.startsWith("javascript:")) return;
        String cleanUrl = extractOriginalUrl(url);
        if (cleanUrl == null || cleanUrl.length() == 0) return;
        for (int i = historyData.size() - 1; i >= 0; i--) {
            if (cleanUrl.equals(historyData.get(i).url)) {
                historyData.remove(i);
            }
        }
        historyData.add(0, new HistoryItemData(title == null ? cleanUrl : title, cleanUrl, System.currentTimeMillis()));
        while (historyData.size() > 100) historyData.remove(historyData.size() - 1);
        saveBrowserHistory();
    }

    private void loadBrowserHistory() {
        historyData.clear();
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_BROWSER_HISTORY, "");
        if (saved == null || saved.length() == 0) return;
        String[] rows = saved.split("\\n");
        for (String row : rows) {
            String[] parts = row.split("\\|", 3);
            if (parts.length == 3) {
                try {
                    historyData.add(new HistoryItemData(decode(parts[0]), decode(parts[1]), Long.parseLong(parts[2])));
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void saveBrowserHistory() {
        StringBuilder sb = new StringBuilder();
        for (HistoryItemData item : historyData) {
            sb.append(encode(item.title)).append("|")
                    .append(encode(item.url)).append("|")
                    .append(item.time).append("\\n");
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_BROWSER_HISTORY, sb.toString()).apply();
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
        String[] options = {"Tanggal", "Nama", "Ukuran"};
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

        if ("running".equals(item.status) || "paused".equals(item.status)) {
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

        if ("running".equals(item.status) || "paused".equals(item.status) || "failed".equals(item.status)) {
            TextView action = new TextView(this);
            if ("running".equals(item.status)) {
                action.setText("Ⅱ");
            } else if ("paused".equals(item.status)) {
                action.setText("▶");
            } else {
                action.setText("↻");
            }
            action.setTextColor(Color.WHITE);
            action.setTextSize(17);
            action.setTypeface(Typeface.DEFAULT_BOLD);
            action.setGravity(Gravity.CENTER);
            action.setBackground(roundRect(Color.parseColor("#20232A"), dp(18), dp(1), COLOR_BORDER));
            action.setOnClickListener(v -> {
                if ("running".equals(item.status)) pauseDownloadItem(item);
                else if ("paused".equals(item.status)) resumeDownloadItem(item);
                else reloadDownloadItem(item);
            });
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(dp(38), dp(38));
            actionParams.setMargins(0, 0, dp(4), 0);
            row.addView(action, actionParams);
        }

        TextView more = new TextView(this);
        more.setText("⋮");
        more.setTextColor(Color.parseColor("#D6D9DE"));
        more.setTextSize(28);
        more.setGravity(Gravity.CENTER);
        more.setOnClickListener(v -> showDownloadItemMenu(v, item));
        row.addView(more, new LinearLayout.LayoutParams(dp(34), dp(52)));

        row.setOnClickListener(v -> {
            if (downloadSelectMode) {
                toggleDownloadSelection(item);
            } else if ("completed".equals(item.status)) {
                openDownloadedFile(item);
            } else if ("failed".equals(item.status) || "paused".equals(item.status)) {
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
        item.engineInfo = getConnectionLabel(item) + " • dijeda";
        saveDownloadHistory();
        refreshDownloadPanel();
        showDownloadNotification(item, "Unduhan dijeda", false);
        Toast.makeText(this, "Unduhan dijeda", Toast.LENGTH_SHORT).show();
    }

    private void resumeDownloadItem(DownloadItem item) {
        if (item == null || !"paused".equals(item.status)) return;
        try {
            File out = new File(item.path);
            item.status = "running";
            item.pauseRequested = false;
            item.speedBytesPerSecond = 0;
            item.lastSpeedTimeMs = 0;
            item.lastSpeedBytes = item.downloadedBytes;
            item.engineInfo = item.connectionCount >= 2 ? "2 koneksi sukses" : "1 koneksi sukses";
            saveDownloadHistory();
            refreshDownloadPanel();
            showDownloadNotification(item, "Melanjutkan unduhan", true);
            Toast.makeText(this, "Melanjutkan unduhan dari posisi terakhir", Toast.LENGTH_SHORT).show();
            startTwoConnectionDownload(item, out);
        } catch (Exception e) {
            failDownload(item, e.getMessage());
        }
    }

    private void reloadDownloadItem(DownloadItem item) {
        if (item == null) return;
        try {
            item.pauseRequested = false;
            item.status = "running";
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
            showDownloadNotification(item, "Premium Fast • reload", true);
            Toast.makeText(this, "Download dimulai ulang dari awal", Toast.LENGTH_SHORT).show();

            startTwoConnectionDownload(item, out);
        } catch (Exception e) {
            failDownload(item, e.getMessage());
        }
    }

    private void showDownloadItemMenu(View anchor, DownloadItem item) {
        PopupMenu popup = new PopupMenu(this, anchor);

        if ("running".equals(item.status)) {
            popup.getMenu().add(0, 10, 0, "Jeda / Pause");
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
            DownloadItem item = new DownloadItem(nextDownloadId++, fileUrl, out.getName(), out.getAbsolutePath(), "running", 0);
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
                downloadItems.add(0, item);
            }
            saveDownloadHistory();
            refreshDownloadPanel();
            showDownloadNotification(item, "Mulai mengunduh", true);

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

    private void startDynamicMultiConnectionDownload(DownloadItem item, File out) {
        item.status = "running";
        item.pauseRequested = false;
        item.engineInfo = "Mengecek 2/4 koneksi";
        refreshDownloadPanel();

        new Thread(() -> {
            HttpURLConnection head = null;
            try {
                final File[] outRef = new File[]{out};
                head = openDownloadConnection(item.url, item, "bytes=0-0");
                validateDownloadResponse(head);

                int responseCode = head.getResponseCode();
                String contentRange = head.getHeaderField("Content-Range");
                long total = parseTotalSize(contentRange);
                if (total <= 0) total = head.getContentLengthLong();

                String cd = head.getHeaderField("Content-Disposition");
                try {
                    String betterName = autoRenameDownloadFile(URLUtil.guessFileName(item.url, cd, head.getContentType()), item.url, head.getContentType());
                    if (betterName != null && betterName.length() > 0 && !betterName.equals(item.fileName)) {
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
                    startLegacyTwoConnectionDownload(item, outRef[0]);
                    return;
                }

                int connections = total >= 32L * 1024L * 1024L ? DOWNLOAD_CONNECTIONS_DYNAMIC_MAX : DOWNLOAD_CONNECTIONS_PREMIUM;
                if (connections <= 2) {
                    startLegacyTwoConnectionDownload(item, outRef[0]);
                    return;
                }

                item.totalBytes = total;
                item.connectionCount = connections;
                item.engineInfo = connections + " koneksi sukses";
                item.downloadedBytes = 0;
                item.progress = 0;
                item.retryCount = 0;

                try {
                    RandomAccessFile raf = new RandomAccessFile(outRef[0], "rw");
                    raf.setLength(total);
                    raf.close();
                } catch (Exception ignored) {}

                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, connections + " koneksi", true);

                final long finalTotal = total;
                final int finalConnections = connections;
                final File finalOutFile = outRef[0];

                long chunk = finalTotal / finalConnections;
                final long[] done = new long[]{0};
                final boolean[] ok = new boolean[]{true};
                ArrayList<Thread> threads = new ArrayList<>();

                for (int i = 0; i < finalConnections; i++) {
                    final int part = i + 1;
                    final long start = i * chunk;
                    final long end = i == finalConnections - 1 ? finalTotal - 1 : ((i + 1) * chunk) - 1;
                    Thread t = new Thread(() -> downloadRangeDynamic(item, finalOutFile, start, end, done, finalTotal, ok, part, finalConnections));
                    threads.add(t);
                    t.start();
                }

                for (Thread t : threads) t.join();

                if (item.pauseRequested || "paused".equals(item.status)) {
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    showDownloadNotification(item, "Unduhan dijeda", false);
                    return;
                }

                if (ok[0]) completeDownload(item);
                else {
                    if (outRef[0].exists()) outRef[0].delete();
                    item.downloadedBytes = 0;
                    item.progress = 0;
                    item.connectionCount = 0;
                    item.engineInfo = "Fallback 2 koneksi";
                    startLegacyTwoConnectionDownload(item, outRef[0]);
                }
            } catch (Exception e) {
                if (head != null) try { head.disconnect(); } catch (Exception ignored) {}
                startLegacyTwoConnectionDownload(item, out);
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
                if (item.status.equals("completed") || item.status.equals("failed") || item.status.equals("paused") || item.status.equals("running")) {
                    saved.add(encode(item.url) + "|" + encode(item.fileName) + "|" + encode(item.path) + "|" + item.status + "|" + item.progress + "|" + item.totalBytes + "|" + item.downloadedBytes + "|" + item.connectionCount + "|" + encode(item.engineInfo) + "|" + encode(item.userAgent) + "|" + encode(item.referer) + "|" + encode(item.failReason) + "|" + encode(item.categoryHint) + "|" + item.part1Start + "|" + item.part1End + "|" + item.part1Done + "|" + item.part2Start + "|" + item.part2End + "|" + item.part2Done + "|" + encode(item.publicUri) + "|" + item.retryCount + "|" + item.hlsDownload);
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
                        if (parts.length >= 20) item.publicUri = decode(parts[19]);
                        if (parts.length >= 21) try { item.retryCount = Integer.parseInt(parts[20]); } catch (Exception ignored) {}
                        if (parts.length >= 22) item.hlsDownload = Boolean.parseBoolean(parts[21]);
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

                    if (translateEnabled && compatibleTranslateActive) {
                        clearCompatibleTranslationMarks();
                        mainHandler.postDelayed(() -> translatePageCompatible(), 250);
                    } else if (translateEnabled && isGoogleTranslatedUrl(webView != null ? webView.getUrl() : "")) {
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
        if (translateEnabled || isGoogleTranslatedUrl(webView != null ? webView.getUrl() : "")) {
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
                        translateEnabled = true;
                        compatibleTranslateActive = true;
                        lastTranslateOriginalUrl = original;
                        updateTopActionStates();
                        translatePageCompatible();
                    } else if (selected.startsWith("Lanjutkan translate")) {
                        translateEnabled = true;
                        compatibleTranslateActive = true;
                        updateTopActionStates();
                        continueCompatibleTranslation();
                    } else if (selected.startsWith("Google Translate proxy")) {
                        if (original == null || original.length() == 0) {
                            Toast.makeText(this, "Buka website dulu untuk translate", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        translateEnabled = true;
                        compatibleTranslateActive = false;
                        lastTranslateOriginalUrl = original;
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
                        translateEnabled = false;
                        compatibleTranslateActive = false;
                        clearCompatibleTranslationMarks();
                        updateTopActionStates();
                        String raw = getOriginalForTranslate(current);
                        if ((raw == null || raw.length() == 0) && lastTranslateOriginalUrl.length() > 0) raw = lastTranslateOriginalUrl;
                        if (raw != null && raw.length() > 0 && isGoogleTranslatedUrl(webView != null ? webView.getUrl() : "")) {
                            webView.loadUrl(raw);
                        }
                        Toast.makeText(this, "Translate dimatikan", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Website menolak Google Translate. Beralih ke mode kompatibel.", Toast.LENGTH_LONG).show();
                    String raw = lastTranslateOriginalUrl != null && lastTranslateOriginalUrl.length() > 0 ? lastTranslateOriginalUrl : getOriginalForTranslate(url);
                    if (raw != null && raw.length() > 0) {
                        compatibleTranslateActive = true;
                        webView.loadUrl(raw);
                        mainHandler.postDelayed(() -> translatePageCompatible(), 1800);
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void translatePageCompatible() {
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
            webView.loadUrl(js);
            translateEnabled = true;
            compatibleTranslateActive = true;
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
        compatibleTranslateActive = true;
        translateEnabled = true;
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
            webView.loadUrl(js);
        } catch (Exception ignored) {
        }
    }

    private void applyCompatibleTranslation(int index, String translated) {
        if (webView == null) return;
        try {
            String js = "javascript:(function(){try{if(window.__yieldApplyTranslation)window.__yieldApplyTranslation("
                    + index + "," + org.json.JSONObject.quote(translated) + ");}catch(e){}})()";
            webView.loadUrl(js);
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
            compatibleTranslateActive = false;
            lastTranslateOriginalUrl = originalUrl;
            String encoded = URLEncoder.encode(originalUrl, "UTF-8");
            webView.loadUrl("https://translate.google.com/translate?sl=auto&tl=" + translateTargetLang + "&u=" + encoded);
            updateTopActionStates();
            Toast.makeText(this, "Membuka Google Translate ke " + translateTargetLabel, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            webView.loadUrl(originalUrl);
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
            webView.loadUrl(js);
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
            webView.loadUrl(js);
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
            webView.loadUrl(js);
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
                    translateEnabled = true;
                    compatibleTranslateActive = false;
                    updateTopActionStates();
                    String encoded = URLEncoder.encode(text, "UTF-8");
                    webView.loadUrl("https://translate.google.com/?sl=auto&tl=" + translateTargetLang + "&op=translate&text=" + encoded);
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
            if (webView != null && webView.getVisibility() == View.VISIBLE) {
                webView.reload();
                Toast.makeText(this, "Website dimuat ulang", Toast.LENGTH_SHORT).show();
            } else {
                String url = getEffectiveCurrentUrl();
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
        if (!adBlock || url == null || url.trim().length() == 0) return false;
        if (isMediaResourceUrl(url) || isYoutubeCoreUrl(url)) return false;

        if (!adBlockRedirectToTempTab) {
            scheduleCloseDetectedAdTabs();
            return true;
        }

        String safeUrl = url.trim();
        int protectedIndex = currentTabIndex;
        TabInfo adTab = new TabInfo("Iklan diblokir", safeUrl, false, true);
        tabs.add(adTab);
        updateTabsCountUi();

        Toast.makeText(this, "Iklan dialihkan ke tab sementara", Toast.LENGTH_SHORT).show();

        if (adBlockAutoCloseAdTabs) {
            mainHandler.postDelayed(() -> closeAdTabSilently(adTab, protectedIndex), 1200);
            mainHandler.postDelayed(this::closeDetectedAdTabs, 2200);
        }
        return true;
    }

    private void closeAdTabSilently(TabInfo adTab, int fallbackIndex) {
        try {
            int index = tabs.indexOf(adTab);
            if (index < 0) return;

            boolean closingCurrent = index == currentTabIndex;
            tabs.remove(index);

            if (tabs.isEmpty()) {
                tabs.add(new TabInfo("Tab utama", "", false));
                currentTabIndex = 0;
                addressBar.setText("");
                showHome();
            } else {
                if (currentTabIndex > index) currentTabIndex--;
                if (currentTabIndex >= tabs.size()) currentTabIndex = tabs.size() - 1;
                if (closingCurrent) {
                    currentTabIndex = Math.max(0, Math.min(fallbackIndex, tabs.size() - 1));
                    TabInfo tab = getCurrentTab();
                    if (tab.url == null || tab.url.length() == 0) {
                        addressBar.setText("");
                        showHome();
                    } else {
                        addressBar.setText(tab.url);
                        webView.setVisibility(View.VISIBLE);
                        homeScroll.setVisibility(View.GONE);
                        webView.loadUrl(tab.url);
                    }
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
                boolean suspicious = t.adTab || (t.url != null && t.url.length() > 0 && isSuspiciousPopupNavigation(t.url, getEffectiveCurrentUrl()));
                if (!suspicious) continue;

                boolean closingCurrent = i == currentTabIndex;
                tabs.remove(i);
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
                showHome();
            } else if (currentTabIndex < 0 || currentTabIndex >= tabs.size()) {
                currentTabIndex = Math.max(0, Math.min(currentTabIndex, tabs.size() - 1));
            }

            if (changed) {
                updateTabsCountUi();
                Toast.makeText(this, "Tab iklan otomatis ditutup", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        applyBrowserSettings();
        webView.addJavascriptInterface(new VideoBridge(), "YieldVideoBridge");
        webView.addJavascriptInterface(new AdBlockBridge(), "YieldAdBlockBridge");
        webView.addJavascriptInterface(new TranslateBridge(), "YieldTranslateBridge");
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            beginDownloadFromWeb(url, contentDisposition, mimeType, userAgent);
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String u = request.getUrl().toString();
                if (safeMode && isUnsafeUrl(u)) {
                    Toast.makeText(MainActivity.this, "Diblokir Safe Browsing sederhana", Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (adBlock && (adBlockRedirectBlocker || adBlockClickHijackBlocker) && request.isForMainFrame() && isSuspiciousPopupNavigation(u, view != null ? view.getUrl() : "")) {
                    captureAdRedirectToTempTab(u, "Popup/redirect iklan");
                    return true;
                }
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String u = request.getUrl().toString();
                if (adBlock && adBlockScriptIframeBlocker && !isMediaResourceUrl(u) && !isYoutubeCoreUrl(u) && (isAdUrl(u) || isKnownPopupHost(u))) {
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                try {
                    if (adBlock && adBlockRedirectBlocker && isSuspiciousPopupNavigation(url, getEffectiveCurrentUrl())) {
                        view.stopLoading();
                        captureAdRedirectToTempTab(url, "Redirect iklan");
                        if (view.canGoBack()) view.goBack();
                        return;
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                String shownUrl = extractOriginalUrl(url);
                addressBar.setText(shownUrl != null ? shownUrl : url);
                progressBar.setVisibility(View.GONE);
                applyDesktopViewportIfNeeded();
                mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 600);
                mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 1800);
                if (readerMode) injectReaderMode();
                if (adBlock) injectPremiumAdBlock();
                updateVideoControlsVisibility();
                TabInfo currentTab = getCurrentTab();
                currentTab.url = shownUrl != null ? shownUrl : url;
                currentTab.title = view.getTitle() != null && view.getTitle().length() > 0 ? view.getTitle() : currentTab.url;
                if (!currentTab.privateTab) {
                    addBrowserHistory(view.getTitle(), shownUrl != null ? shownUrl : url);
                }
                videoControlsManualHidden = false;
                injectVideoPlaybackWatcher();
                applyNightModeToWebPage();
                detectTranslateProxyBlocked(url);
                if (hideGoogleTranslateBar && isGoogleTranslatedUrl(url)) {
                    mainHandler.postDelayed(() -> hideGoogleTranslateToolbar(), 250);
                    mainHandler.postDelayed(() -> hideGoogleTranslateToolbar(), 800);
                    mainHandler.postDelayed(() -> hideGoogleTranslateToolbar(), 1800);
                    mainHandler.postDelayed(() -> hideGoogleTranslateToolbar(), 3500);
                    mainHandler.postDelayed(() -> hideGoogleTranslateToolbar(), 6000);
                }
                if (compatibleTranslateActive && !isGoogleTranslatedUrl(url)) {
                    mainHandler.postDelayed(() -> translatePageCompatible(), 600);
                    mainHandler.postDelayed(() -> translatePageCompatible(), 2200);
                }
                scheduleCloseDetectedAdTabs();
                updateTopActionStates();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
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

                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
                setRequestedOrientation(originalRequestedOrientation);

                restoreVideoControlsFromFullscreenOverlay();
                if (topBarView != null) topBarView.setVisibility(View.VISIBLE);
                if (bottomNavView != null) bottomNavView.setVisibility(View.VISIBLE);
                checkAndShowVideoControls();
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
        boolean active;
        if ("OFF".equals(nightModeOption)) {
            active = false;
        } else if ("AUTO".equals(nightModeOption)) {
            active = isSystemDarkMode();
        } else {
            active = true;
        }

        String host = getCurrentHostForSettings();
        if (host.length() > 0 && nightModeExceptions.contains(host)) {
            active = false;
        }
        return active;
    }

    private String nightModeLabel() {
        if ("OFF".equals(nightModeOption)) return "OFF";
        if ("AUTO".equals(nightModeOption)) return "Auto ikut sistem";
        return "ON";
    }

    private void applyNightModeToWebPage() {
        if (webView == null || webView.getVisibility() != View.VISIBLE) return;

        boolean active = isNightModeActiveForCurrentSite();
        String js;
        if (active) {
            // Mode malam aman: jangan paksa semua teks/div/card karena Google AI/Search bisa rusak.
            // WebView ForceDark yang menangani darkening utama, CSS ini hanya mencegah white flash.
            js =
                    "javascript:(function(){"
                            + "try{"
                            + "var id='yield-night-style';"
                            + "var old=document.getElementById(id);if(old)old.remove();"
                            + "var s=document.createElement('style');s.id=id;"
                            + "s.innerHTML="
                            + "'html,body{background:#0b0d10!important;color-scheme:dark!important;}' + "
                            + "':root{color-scheme:dark!important;}' + "
                            + "'input,textarea,select{color-scheme:dark!important;}' + "
                            + "'img,video,canvas,svg,picture{filter:none!important;}';"
                            + "document.head.appendChild(s);"
                            + "}catch(e){}"
                            + "})()";
        } else {
            // OFF harus benar-benar membersihkan efek gelap tanpa menutup panel setelan.
            js =
                    "javascript:(function(){"
                            + "try{"
                            + "var ids=['yield-night-style','yield-dark-style','yield-force-dark'];"
                            + "for(var i=0;i<ids.length;i++){var x=document.getElementById(ids[i]);if(x)x.remove();}"
                            + "document.documentElement.style.colorScheme='light';"
                            + "document.documentElement.style.background='';"
                            + "document.documentElement.style.backgroundColor='';"
                            + "document.documentElement.classList.remove('dark','night','night-mode','dark-mode');"
                            + "if(document.body){"
                            + "document.body.style.colorScheme='light';"
                            + "document.body.style.background='';"
                            + "document.body.style.backgroundColor='';"
                            + "document.body.style.color='';"
                            + "document.body.classList.remove('dark','night','night-mode','dark-mode');"
                            + "}"
                            + "var metas=document.querySelectorAll('meta[name=color-scheme]');"
                            + "for(var m=0;m<metas.length;m++){metas[m].setAttribute('content','light');}"
                            + "}catch(e){}"
                            + "})()";
        }

        try {
            webView.loadUrl(js);
            webView.setBackgroundColor(active ? COLOR_BG : Color.WHITE);
        } catch (Exception ignored) {
        }
    }

    private void disableNightModeCompletely(boolean reloadPage) {
        nightModeOption = "OFF";
        nightMode = false;
        saveSettings();

        try {
            if (webView != null) {
                webView.setBackgroundColor(Color.WHITE);
                WebSettings settings = webView.getSettings();
                if (Build.VERSION.SDK_INT >= 29) {
                    settings.setForceDark(WebSettings.FORCE_DARK_OFF);
                }
            }
        } catch (Exception ignored) {
        }

        applyBrowserSettings();
        applyNightModeToWebPage();

        if (reloadPage && webView != null && webView.getVisibility() == View.VISIBLE) {
            mainHandler.postDelayed(() -> {
                try {
                    String url = webView.getUrl();
                    if (url != null && url.length() > 0) webView.reload();
                } catch (Exception ignored) {
                }
            }, 250);
        }
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
            disableNightModeCompletely(false);
            updateTopActionStates();
            Toast.makeText(this, "Mode Malam: OFF", Toast.LENGTH_SHORT).show();
        }));
        box.addView(nightChoiceRow("ON", "ON".equals(nightModeOption), v -> {
            nightModeOption = "ON";
            nightMode = isNightModeActiveForCurrentSite();
            saveSettings();
            applyBrowserSettings();
            applyNightModeToWebPage();
            updateTopActionStates();
            Toast.makeText(this, "Mode Malam: ON", Toast.LENGTH_SHORT).show();
        }));
        box.addView(nightChoiceRow("Auto ikut sistem", "AUTO".equals(nightModeOption), v -> {
            nightModeOption = "AUTO";
            nightMode = isNightModeActiveForCurrentSite();
            saveSettings();
            applyBrowserSettings();
            applyNightModeToWebPage();
            updateTopActionStates();
            Toast.makeText(this, "Mode Malam: Auto ikut sistem", Toast.LENGTH_SHORT).show();
        }));
        box.addView(nightChoiceRow("Bersihkan style gelap halaman ini", false, v -> {
            disableNightModeCompletely(false);
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
                    applyNightModeToWebPage();
                    Toast.makeText(this, excepted ? "Pengecualian dihapus" : "Situs dikecualikan", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void applyBrowserSettings() {
        if (webView == null) return;
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setSupportMultipleWindows(false);
        try { settings.setMediaPlaybackRequiresUserGesture(!videoBackgroundPlay); } catch (Exception ignored) {}
        settings.setJavaScriptCanOpenWindowsAutomatically(!(adBlock && adBlockPopupBlocker));
        settings.setDatabaseEnabled(true);
        settings.setCacheMode((speedMode || videoBufferBooster) ? WebSettings.LOAD_CACHE_ELSE_NETWORK : WebSettings.LOAD_DEFAULT);
        settings.setLoadsImagesAutomatically(!dataSaver);
        settings.setTextZoom(textZoom);

        try {
            settings.setLayoutAlgorithm(desktopMode ? WebSettings.LayoutAlgorithm.NORMAL : WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        } catch (Exception ignored) {
        }

        if (desktopMode) {
            settings.setUserAgentString(getDesktopUserAgent());
            try { webView.setInitialScale(55); } catch (Exception ignored) {}
        } else {
            settings.setUserAgentString(null);
            try { webView.setInitialScale(100); } catch (Exception ignored) {}
        }

        try {
            if (Build.VERSION.SDK_INT >= 29) {
                settings.setForceDark(isNightModeActiveForCurrentSite() ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
            }
        } catch (Exception ignored) {
        }

        try {
            webView.setBackgroundColor(isNightModeActiveForCurrentSite() ? COLOR_BG : Color.WHITE);
        } catch (Exception ignored) {
        }
    }

    private String getDesktopUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    }

    private void applyDesktopViewportIfNeeded() {
        if (!desktopMode || webView == null || webView.getVisibility() != View.VISIBLE) return;
        try {
            String js = "(function(){"
                    + "try{"
                    + "var w=" + DESKTOP_VIEWPORT_WIDTH + ";"
                    + "var current=Math.max(document.documentElement.clientWidth||0,window.innerWidth||0,360);"
                    + "var scale=Math.max(0.25,Math.min(0.75,current/w));"
                    + "var m=document.querySelector('meta[name=viewport]');"
                    + "if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}"
                    + "m.setAttribute('content','width='+w+', initial-scale='+scale+', minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes');"
                    + "document.documentElement.style.minWidth=w+'px';"
                    + "if(document.body){document.body.style.minWidth=w+'px';document.body.style.overflowX='auto';}"
                    + "var mobile=document.querySelectorAll('[class*=mobile],[id*=mobile]');"
                    + "for(var i=0;i<mobile.length&&i<80;i++){mobile[i].className=(mobile[i].className+' desktop-view').replace(/\bmobile\b/g,'desktop');}"
                    + "return 'desktop_viewport_'+w;"
                    + "}catch(e){return 'desktop_error';}"
                    + "})()";
            webView.evaluateJavascript(js, null);
        } catch (Exception ignored) {
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
                "admaven", "pushpush", "pushengage", "pushwoosh", "realsrv", "highperformanceformat",
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

    private boolean isSuspiciousPopupNavigation(String targetUrl, String currentUrl) {
        if (targetUrl == null || targetUrl.length() == 0) return false;
        String lower = targetUrl.toLowerCase(Locale.US);
        if (lower.startsWith("about:") || lower.startsWith("javascript:") || lower.startsWith("data:")) return false;
        if (lower.startsWith("mailto:") || lower.startsWith("tel:") || lower.startsWith("intent:")) return false;
        // Video playback tidak boleh diblokir oleh AdBlock.
        if (isMediaResourceUrl(lower) || isYoutubeCoreUrl(lower)) return false;

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

    private boolean isAdUrl(String url) {
        if (!adBlock || url == null) return false;
        String u = url.toLowerCase(Locale.US);

        if (isMediaResourceUrl(u)) return false;
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
                || u.contains("vast")
                || u.contains("vpaid");
    }

    private void injectPremiumAdBlock() {
        if (webView == null || !adBlock) return;

        String popupEnabled = adBlockPopupBlocker ? "true" : "false";
        String clickEnabled = adBlockClickHijackBlocker ? "true" : "false";
        String scriptEnabled = adBlockScriptIframeBlocker ? "true" : "false";

        String js = "javascript:(function(){"
                + "window.__yieldAdBlockPremium=true;"
                + "var Y_POPUP=" + popupEnabled + ",Y_CLICK=" + clickEnabled + ",Y_SCRIPT=" + scriptEnabled + ";"
                + "function hostOf(u){try{var a=document.createElement('a');a.href=u;return (a.hostname||'').replace(/^www\\./,'').toLowerCase();}catch(e){return '';}}"
                + "function media(u){try{var s=(u||'').toLowerCase();return s.indexOf('googlevideo.com/videoplayback')>-1||s.indexOf('/videoplayback')>-1||s.indexOf('.mp4')>-1||s.indexOf('.m3u8')>-1||s.indexOf('.mpd')>-1||s.indexOf('.webm')>-1||s.indexOf('.m4s')>-1||s.indexOf('.ts')>-1||s.indexOf('mime=video')>-1||s.indexOf('mime%3dvideo')>-1;}catch(e){return false;}}"
                + "function safeVideoHost(h){return h.indexOf('youtube.com')>-1||h.indexOf('youtu.be')>-1||h.indexOf('googlevideo.com')>-1||h.indexOf('ytimg.com')>-1;}"
                + "function bad(u){try{if(!u||media(u))return false;var h=hostOf(u);var s=(u||'').toLowerCase();var cur=(location.hostname||'').replace(/^www\\./,'').toLowerCase();if(!h||h===cur||h.endsWith('.'+cur))return false;if(safeVideoHost(h))return false;if(/(hotterydiseur|sewarsremeets|onclickads|clickadu|popads|popcash|propellerads|adsterra|hilltopads|exoclick|realsrv|doubleclick|googlesyndication|googleadservices)/.test(h))return true;if(/\\.(cfd|click|cam|monster|quest|buzz|icu|cyou)$/.test(h))return true;if(/\\.(shop|xyz|top|site|space|online|live|fun|lol)$/.test(h)&&/[\\/][a-z0-9_-]{8,}/.test(s))return true;if(/(popunder|popup|redirect|adclick|clickunder|interstitial|push)/.test(s))return true;return false;}catch(e){return false;}}"
                + "if(Y_POPUP&&!window.__yieldOpenPatched){window.__yieldOpenPatched=true;var oldOpen=window.open;window.open=function(u,n,f){if(bad(u)){try{if(window.YieldAdBlockBridge)YieldAdBlockBridge.onAdRedirect(String(u));}catch(e){}console.log('Yield isolated popup',u);return {closed:true,focus:function(){},close:function(){}};}try{return oldOpen.call(window,u,n,f);}catch(e){return {closed:true,focus:function(){},close:function(){}};}};}"
                + "if(Y_CLICK&&!window.__yieldClickPatched){window.__yieldClickPatched=true;document.addEventListener('click',function(e){try{var a=e.target&&e.target.closest?e.target.closest('a[href]'):null;if(a&&bad(a.href)){try{if(window.YieldAdBlockBridge)YieldAdBlockBridge.onAdRedirect(String(a.href));}catch(ee){}e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();console.log('Yield isolated ad link',a.href);return false;}}catch(x){}},true);document.addEventListener('auxclick',function(e){try{var a=e.target&&e.target.closest?e.target.closest('a[href]'):null;if(a&&bad(a.href)){try{if(window.YieldAdBlockBridge)YieldAdBlockBridge.onAdRedirect(String(a.href));}catch(ee){}e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();return false;}}catch(x){}},true);}"
                + "function hide(s){try{document.querySelectorAll(s).forEach(function(e){e.style.setProperty('display','none','important');e.remove&&e.remove();});}catch(x){}}"
                + "function clickSkip(){try{document.querySelectorAll('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button').forEach(function(b){b.click();});}catch(e){}}"
                + "function clean(){"
                + "var host=(location.hostname||'').toLowerCase();"
                + "var isYT=host.indexOf('youtube.com')>-1||host.indexOf('youtu.be')>-1;"
                + "try{if(Y_CLICK)document.querySelectorAll('a[target=_blank],a[target=\\\"_blank\\\"]').forEach(function(a){if(bad(a.href)){a.removeAttribute('target');a.setAttribute('rel','noopener noreferrer');}});}catch(e){}"
                + "var selectors=isYT?["
                + "'ytd-display-ad-renderer','ytd-promoted-video-renderer','ytd-ad-slot-renderer','ytd-companion-slot-renderer',"
                + "'ytd-banner-promo-renderer','ytd-in-feed-ad-layout-renderer','ytd-promoted-sparkles-web-renderer',"
                + "'.ytp-ad-module','.ytp-ad-overlay-container','.ytp-ad-text','.ytp-ad-image-overlay',"
                + "'.ytp-ad-skip-button-container','.ytp-ad-preview-container','.ytp-ad-progress-list','.GoogleActiveViewElement'"
                + "]:["
                + "'.adsbygoogle','iframe[id*=ad]','iframe[src*=ads]','iframe[src*=doubleclick]',"
                + "'iframe[src*=onclickads]','iframe[src*=clickadu]','iframe[src*=popads]','iframe[src*=propellerads]',"
                + "'[id*=ad-]','[id^=ad_]','[class*=ad-]','[class*=ads-]','[class*=advert]',"
                + "'.GoogleActiveViewElement','.ad-banner','.ad-container','.advertisement','.sponsored'"
                + "];"
                + "if(Y_SCRIPT)selectors.forEach(hide);"
                + "try{if(Y_SCRIPT)document.querySelectorAll('iframe[src],script[src]').forEach(function(el){var u=el.src||'';if(bad(u)){el.remove();}});}catch(e){}"
                + "if(isYT)clickSkip();"
                + "try{document.body.style.setProperty('overflow','auto','important');}catch(e){}"
                + "}"
                + "clean();setInterval(clean,900);"
                + "})();";
        webView.loadUrl(js);
    }

    private void injectReaderMode() {
        String js = "javascript:(function(){document.body.style.maxWidth='720px';document.body.style.margin='auto';document.body.style.lineHeight='1.7';document.body.style.fontSize='18px';document.body.style.background='#111318';document.body.style.color='#F5F7FA';})()";
        webView.loadUrl(js);
    }

    private void openHomeSearchUrl() {
        if (homeSearchInput == null) {
            openAddressBarUrl();
            return;
        }
        String text = homeSearchInput.getText().toString().trim();
        if (text.length() == 0) {
            addressBar.requestFocus();
            return;
        }
        addressBar.setText(text);
        openAddressBarUrl();
    }

    private void openAddressBarUrl() {
        String text = addressBar.getText().toString().trim();
        if (text.length() == 0) {
            showHome();
            return;
        }
        String url;
        if (text.startsWith("http://") || text.startsWith("https://")) url = text;
        else if (text.contains(".") && !text.contains(" ")) url = "https://" + text;
        else url = "https://www.google.com/search?q=" + text.replace(" ", "+");

        TabInfo currentTab = getCurrentTab();
        currentTab.url = url;
        currentTab.title = url;
        webView.setVisibility(View.VISIBLE);
        homeScroll.setVisibility(View.GONE);
        updateVideoControlsVisibility();
        if (translateEnabled) loadTranslatedPage(url);
        else webView.loadUrl(url);
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
        String saved = p.getString("shortcuts", "");
        if (saved == null || saved.trim().length() == 0) {
            shortcutsData.add(new ShortcutItemData("Google", "https://www.google.com"));
            shortcutsData.add(new ShortcutItemData("GitHub", "https://github.com"));
            shortcutsData.add(new ShortcutItemData("YouTube", "https://m.youtube.com"));
            return;
        }
        String[] lines = saved.split("\\n");
        for (String line : lines) {
            String[] parts = line.split("\\|", 2);
            if (parts.length == 2) {
                shortcutsData.add(new ShortcutItemData(decode(parts[0]), decode(parts[1])));
            }
        }
        if (shortcutsData.isEmpty()) {
            shortcutsData.add(new ShortcutItemData("Google", "https://www.google.com"));
        }
    }

    private void saveShortcuts() {
        StringBuilder sb = new StringBuilder();
        for (ShortcutItemData item : shortcutsData) {
            sb.append(encode(item.label)).append("|").append(encode(item.url)).append("\\n");
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("shortcuts", sb.toString()).apply();
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

                if (dx < 0) {
                    navigateSwipeBack();
                } else {
                    navigateSwipeForward();
                }
                return false;
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
                if (translateEnabled) loadTranslatedPage(tabUrl);
                else webView.loadUrl(tabUrl);
            }

            updateVideoControlsVisibility();
            updateTopActionStates();
            Toast.makeText(this, "Halaman terakhir dibuka lagi", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Tidak ada halaman untuk dibuka", Toast.LENGTH_SHORT).show();
        }
    }

    private void showHome() {
        // Home hanya menyembunyikan halaman web, bukan menghapus state halaman.
        // Jadi kalau tidak sengaja kepencet Home, halaman terakhir masih bisa dikembalikan lewat gesture.
        try {
            saveCurrentTabState();
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
        adBlock = p.getBoolean("adBlock", false);
        adBlockPopupBlocker = p.getBoolean("adBlockPopupBlocker", true);
        adBlockRedirectBlocker = p.getBoolean("adBlockRedirectBlocker", true);
        adBlockScriptIframeBlocker = p.getBoolean("adBlockScriptIframeBlocker", true);
        adBlockClickHijackBlocker = p.getBoolean("adBlockClickHijackBlocker", true);
        adBlockRedirectToTempTab = p.getBoolean("adBlockRedirectToTempTab", true);
        adBlockAutoCloseAdTabs = p.getBoolean("adBlockAutoCloseAdTabs", true);
        dataSaver = p.getBoolean("dataSaver", false);
        desktopMode = p.getBoolean("desktopMode", false);
        textZoom = p.getInt("textZoom", 100);
        shortcutDownload = p.getBoolean("shortcutDownload", true);
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
        videoFloatingPlayer = p.getBoolean("videoFloatingPlayer", true);
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
        topIconReload = p.getBoolean("topIconReload", true);
        topIconBookmark = p.getBoolean("topIconBookmark", true);
        topIconTranslate = p.getBoolean("topIconTranslate", true);
        searchEngine = p.getString("searchEngine", "Google");

        if (!p.getBoolean("menuDefaultsV030", false)) {
            shortcutDownload = true;
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
                .putBoolean("nightMode", isNightModeActiveForCurrentSite())
                .putString("nightModeOption", nightModeOption)
                .putStringSet(KEY_NIGHT_EXCEPTIONS, new HashSet<>(nightModeExceptions))
                .putBoolean("readerMode", readerMode)
                .putBoolean("adBlock", adBlock)
                .putBoolean("adBlockPopupBlocker", adBlockPopupBlocker)
                .putBoolean("adBlockRedirectBlocker", adBlockRedirectBlocker)
                .putBoolean("adBlockScriptIframeBlocker", adBlockScriptIframeBlocker)
                .putBoolean("adBlockClickHijackBlocker", adBlockClickHijackBlocker)
                .putBoolean("adBlockRedirectToTempTab", adBlockRedirectToTempTab)
                .putBoolean("adBlockAutoCloseAdTabs", adBlockAutoCloseAdTabs)
                .putBoolean("dataSaver", dataSaver)
                .putBoolean("desktopMode", desktopMode)
                .putInt("textZoom", textZoom)
                .putBoolean("shortcutDownload", shortcutDownload)
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
        super.onUserLeaveHint();
        if (!videoFloatingPlayer || webView == null || webView.getVisibility() != View.VISIBLE) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(16, 9))
                        .build();
                // PiP hanya dicoba ringan. Kalau halaman/player tidak mendukung, Android akan abaikan/throw.
                enterPictureInPictureMode(params);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onBackPressed() {
        if (fullscreenVideoView != null) {
            try {
                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.removeView(fullscreenVideoView);
            } catch (Exception ignored) {}
            fullscreenVideoView = null;
            restoreVideoControlsFromFullscreenOverlay();
            if (fullscreenVideoCallback != null) {
                fullscreenVideoCallback.onCustomViewHidden();
                fullscreenVideoCallback = null;
            }
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(originalSystemUiVisibility);
            setRequestedOrientation(originalRequestedOrientation);
            if (topBarView != null) topBarView.setVisibility(View.VISIBLE);
            if (bottomNavView != null) bottomNavView.setVisibility(View.VISIBLE);
            checkAndShowVideoControls();
            return;
        }
        if (topBarView != null && topBarView.getVisibility() == View.GONE && webView != null && webView.getVisibility() == View.VISIBLE) {
            exitAppVideoFullscreenFallback();
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