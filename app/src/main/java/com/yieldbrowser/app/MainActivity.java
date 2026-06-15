
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
import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.DownloadListener;
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

public class MainActivity extends Activity {
    private static final int COLOR_BG = Color.parseColor("#0C0D10");
    private static final int COLOR_SURFACE_2 = Color.parseColor("#20232A");
    private static final int COLOR_BORDER = Color.parseColor("#2A2E36");
    private static final int COLOR_TEXT = Color.parseColor("#F5F7FA");
    private static final int COLOR_SUBTEXT = Color.parseColor("#B7BDC8");
    private static final int COLOR_ACCENT = Color.parseColor("#F39A22");
    private static final int COLOR_ON = Color.parseColor("#22C55E");
    private static final String PREFS = "yield_browser_prefs";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String KEY_DOWNLOAD_HISTORY = "download_history";
    private static final String KEY_BROWSER_HISTORY = "browser_history";
    private static final String CHANNEL_DOWNLOADS = "yield_downloads";
    private static final String ACTION_OPEN_DOWNLOADS = "com.yieldbrowser.app.OPEN_DOWNLOADS";
    private static final int REQ_CAMERA_QR = 2401;

    private EditText addressBar;
    private EditText homeSearchInput;
    private ProgressBar progressBar;
    private WebView webView;
    private ScrollView homeScroll;
    private ImageButton bookmarkButton;
    private ImageButton translateButton;
    private View topBarView;
    private View bottomNavView;
    private LinearLayout videoControlsBar;
    private TextView videoSpeedLabel;

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
    private boolean speedMode = false;
    private boolean safeMode = true;
    private boolean nightMode = true;
    private boolean readerMode = false;
    private boolean adBlock = false;
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
    private boolean shortcutVideoControls = false;
    private float videoSpeed = 1.0f;
    private String downloadSubfolder = "Download";

    private final ArrayList<DownloadItem> downloadItems = new ArrayList<>();
    private final ArrayList<ShortcutItemData> shortcutsData = new ArrayList<>();
    private final ArrayList<HistoryItemData> historyData = new ArrayList<>();
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

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();
        loadDownloadHistory();
        loadShortcuts();
        loadBrowserHistory();
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
    protected void onResume() {
        super.onResume();
        handleOpenDownloadsIntent(getIntent());
    }

    private View createTopBar() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(14), dp(12), dp(14), dp(10));
        wrap.setBackgroundColor(COLOR_BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
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

        bookmarkButton = smallTopIcon(R.drawable.ic_star, "Bookmark", v -> toggleBookmark());
        bar.addView(bookmarkButton, new LinearLayout.LayoutParams(dp(30), dp(30)));

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
                new int[]{Color.parseColor("#111318"), Color.parseColor("#0A0B0E")}));

        content.addView(space(dp(16)));

        TextView title = new TextView(this);
        title.setText("Yield Browser");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(title);

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
        searchCard.setBackground(roundRect(Color.parseColor("#11141A"), dp(26), dp(1), Color.parseColor("#232730")));

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

        TextView more = new TextView(this);
        more.setText("Download langsung berjalan saat tombol unduh ditekan. Detail ada di Menu > Unduhan Yield.");
        more.setTextColor(COLOR_SUBTEXT);
        more.setTextSize(14);
        content.addView(more);

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
        nav.addView(tabsNavButton(v -> Toast.makeText(this, "Jumlah tab: " + tabCount, Toast.LENGTH_SHORT).show()));
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

        TextView count = new TextView(this);
        count.setText(String.valueOf(tabCount));
        count.setTextColor(Color.parseColor("#F6F7FA"));
        count.setTextSize(10);
        count.setTypeface(Typeface.DEFAULT_BOLD);
        count.setGravity(Gravity.CENTER);
        box.addView(count, new FrameLayout.LayoutParams(-1, -1));
        return item;
    }

    private void showQuickMenu() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(12), dp(12), dp(12), dp(12));
        menu.setBackground(roundRect(Color.parseColor("#171A20"), dp(24), dp(1), Color.parseColor("#2D333D")));

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
                Toast.makeText(this, "Mode privat akan dibuat setelah sistem tab stabil", Toast.LENGTH_SHORT).show();
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
            menu.addView(menuRow(R.drawable.ic_night, "Night Mode " + (nightMode ? "ON" : "OFF"), v -> {
                nightMode = !nightMode;
                saveSettings();
                dialog.dismiss();
                Toast.makeText(this, nightMode ? "Night mode aktif" : "Night mode nonaktif", Toast.LENGTH_SHORT).show();
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

    private void showSettingsPanel() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(16), dp(14), dp(16));
        panel.setBackground(roundRect(Color.parseColor("#171A20"), dp(24), dp(1), Color.parseColor("#2D333D")));
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

        panel.addView(sectionTitle("Pusat fitur"));
        panel.addView(actionRow(R.drawable.ic_download_modern, "Unduhan Yield", "Riwayat, progress, lokasi penyimpanan, dan engine 2 koneksi.", v -> {
            dialog.dismiss();
            showDownloadSettingsPanel();
        }));
        panel.addView(actionRow(R.drawable.ic_bookmark, "Bookmark", "Buka daftar bookmark yang tersimpan.", v -> {
            dialog.dismiss();
            showBookmarkList();
        }));
        panel.addView(actionRow(R.drawable.ic_private, "Privat", "Mode privat dan tab privat.", v -> {
            Toast.makeText(this, "Mode privat akan dibuat setelah sistem tab stabil", Toast.LENGTH_SHORT).show();
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
        panel.addView(actionRow(R.drawable.ic_save_page, "Simpan halaman offline", "Simpan halaman saat ini sebagai web archive.", v -> {
            saveCurrentPageOffline();
        }));

        panel.addView(sectionTitle("Fitur browsing"));
        panel.addView(settingRow(R.drawable.ic_speed, "Mode cepat", "Optimasi cache, gambar, dan resource.", speedMode, v -> { speedMode = !speedMode; applyBrowserSettings(); saveSettings(); dialog.dismiss(); showSettingsPanel(); }));
        panel.addView(settingRow(R.drawable.ic_safe, "Safe browsing", "Blokir URL berisiko sederhana.", safeMode, v -> { safeMode = !safeMode; saveSettings(); dialog.dismiss(); showSettingsPanel(); }));
        panel.addView(settingRow(R.drawable.ic_night, "Night mode", "Tampilan gelap untuk home dan menu.", nightMode, v -> { nightMode = !nightMode; saveSettings(); dialog.dismiss(); showSettingsPanel(); }));
        panel.addView(settingRow(R.drawable.ic_reader, "Reader / novel mode", "Mode baca ringan untuk artikel.", readerMode, v -> { readerMode = !readerMode; saveSettings(); dialog.dismiss(); showSettingsPanel(); }));
        panel.addView(settingRow(R.drawable.ic_shield, "Ad block", "Filter iklan sederhana berbasis URL.", adBlock, v -> { adBlock = !adBlock; saveSettings(); dialog.dismiss(); showSettingsPanel(); }));
        panel.addView(settingRow(R.drawable.ic_data_saver, "Hemat data", "Matikan gambar otomatis saat browsing.", dataSaver, v -> { dataSaver = !dataSaver; applyBrowserSettings(); saveSettings(); dialog.dismiss(); showSettingsPanel(); }));
        panel.addView(settingRow(R.drawable.ic_desktop, "Desktop mode", "Ganti user agent ke desktop.", desktopMode, v -> { desktopMode = !desktopMode; applyBrowserSettings(); saveSettings(); if (webView != null && webView.getVisibility() == View.VISIBLE) webView.reload(); dialog.dismiss(); showSettingsPanel(); }));
        panel.addView(actionRow(R.drawable.ic_text_size, "Ukuran teks: " + textZoom + "%", "Atur ukuran teks dengan slider persentase.", v -> {
            showTextZoomDialog(dialog);
        }));
        panel.addView(actionRow(R.drawable.ic_clear, "Bersihkan cache", "Hapus cache WebView.", v -> { if (webView != null) webView.clearCache(true); Toast.makeText(this, "Cache dibersihkan", Toast.LENGTH_SHORT).show(); }));

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
        panel.setBackground(roundRect(Color.parseColor("#171A20"), dp(24), dp(1), Color.parseColor("#2D333D")));

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

        TextView sub = new TextView(this);
        sub.setText("Menu utama");
        sub.setTextColor(COLOR_SUBTEXT);
        sub.setTextSize(16);
        sub.setTypeface(Typeface.DEFAULT_BOLD);
        sub.setPadding(dp(8), 0, 0, dp(12));
        panel.addView(sub);

        panel.addView(customizeToggleRow(R.drawable.ic_download_modern, "Unduhan Yield", shortcutDownload, v -> { shortcutDownload = !shortcutDownload; saveSettings(); dialog.dismiss(); showCustomizeMenuPanel(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_bookmark, "Bookmark", shortcutBookmark, v -> { shortcutBookmark = !shortcutBookmark; saveSettings(); dialog.dismiss(); showCustomizeMenuPanel(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_private, "Privat", shortcutPrivate, v -> { shortcutPrivate = !shortcutPrivate; saveSettings(); dialog.dismiss(); showCustomizeMenuPanel(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_shield, "AdBlock Premium", shortcutAdBlock, v -> { shortcutAdBlock = !shortcutAdBlock; saveSettings(); dialog.dismiss(); showCustomizeMenuPanel(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_reader, "Reader / Novel Mode", shortcutReader, v -> { shortcutReader = !shortcutReader; saveSettings(); dialog.dismiss(); showCustomizeMenuPanel(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_night, "Night Mode", shortcutNightMode, v -> { shortcutNightMode = !shortcutNightMode; saveSettings(); dialog.dismiss(); showCustomizeMenuPanel(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_qr_scan, "Pindai QR Code", shortcutQrScan, v -> { shortcutQrScan = !shortcutQrScan; saveSettings(); dialog.dismiss(); showCustomizeMenuPanel(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_history, "Riwayat", shortcutHistory, v -> { shortcutHistory = !shortcutHistory; saveSettings(); dialog.dismiss(); showCustomizeMenuPanel(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_find_page, "Cari di halaman", shortcutFindPage, v -> { shortcutFindPage = !shortcutFindPage; saveSettings(); dialog.dismiss(); showCustomizeMenuPanel(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_share, "Bagikan halaman", shortcutShare, v -> { shortcutShare = !shortcutShare; saveSettings(); dialog.dismiss(); showCustomizeMenuPanel(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_fullscreen, "Layar penuh", shortcutFullscreen, v -> { shortcutFullscreen = !shortcutFullscreen; saveSettings(); dialog.dismiss(); showCustomizeMenuPanel(); }));
        panel.addView(customizeToggleRow(R.drawable.ic_video_control, "Kontrol video", shortcutVideoControls, v -> { shortcutVideoControls = !shortcutVideoControls; saveSettings(); dialog.dismiss(); showCustomizeMenuPanel(); }));

        TextView fixed = new TextView(this);
        fixed.setText("Tetap tampil");
        fixed.setTextColor(COLOR_SUBTEXT);
        fixed.setTextSize(16);
        fixed.setTypeface(Typeface.DEFAULT_BOLD);
        fixed.setPadding(dp(8), dp(18), 0, dp(12));
        panel.addView(fixed);

        panel.addView(fixedMenuRow(R.drawable.ic_settings, "Setelan"));
        panel.addView(fixedMenuRow(R.drawable.ic_customize, "Sesuaikan menu"));
        panel.addView(fixedMenuRow(R.drawable.ic_exit, "Keluar"));

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
        panel.setBackground(roundRect(Color.parseColor("#171A20"), dp(24), dp(1), Color.parseColor("#2D333D")));

        TextView title = new TextView(this);
        title.setText("Pengaturan Unduhan");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(title);

        TextView path = new TextView(this);
        path.setText("Lokasi sekarang:\\n" + getDownloadDirectory().getAbsolutePath());
        path.setTextColor(COLOR_SUBTEXT);
        path.setTextSize(13);
        path.setPadding(0, dp(8), 0, dp(12));
        panel.addView(path);

        panel.addView(actionRow(R.drawable.ic_download_modern, "Buka riwayat unduhan", "Lihat file, progress, open, hapus riwayat/file.", v -> {
            dialog.dismiss();
            showDownloadManager();
        }));
        panel.addView(actionRow(R.drawable.ic_folder, "Atur subfolder penyimpanan", "Contoh: Download, Video, Anime, Dokumen.", v -> {
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
        panel.addView(actionRow(R.drawable.ic_settings, "Engine download", "2 koneksi paralel, fallback ke 1 koneksi jika server tidak support range.", v -> {
            Toast.makeText(this, "Engine Yield: 2 koneksi paralel aktif", Toast.LENGTH_LONG).show();
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

    private LinearLayout createVideoControlsBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(6), dp(10), dp(6));
        bar.setBackgroundColor(Color.parseColor("#101217"));

        TextView title = new TextView(this);
        title.setText("Video");
        title.setTextColor(COLOR_SUBTEXT);
        title.setTextSize(12);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(title, new LinearLayout.LayoutParams(dp(54), -1));

        bar.addView(videoButton(R.drawable.ic_video_play, "Play", v -> controlVideo("play")));
        bar.addView(videoButton(R.drawable.ic_video_pause, "Pause", v -> controlVideo("pause")));
        bar.addView(videoButton(R.drawable.ic_video_stop, "Stop", v -> controlVideo("stop")));

        videoSpeedLabel = new TextView(this);
        videoSpeedLabel.setText("1x");
        videoSpeedLabel.setTextColor(Color.parseColor("#111111"));
        videoSpeedLabel.setTextSize(15);
        videoSpeedLabel.setTypeface(Typeface.DEFAULT_BOLD);
        videoSpeedLabel.setGravity(Gravity.CENTER);
        videoSpeedLabel.setBackground(roundRect(COLOR_ACCENT, dp(18), 0, Color.TRANSPARENT));
        videoSpeedLabel.setOnClickListener(v -> cycleVideoSpeed());
        LinearLayout.LayoutParams speedParams = new LinearLayout.LayoutParams(dp(70), dp(42));
        speedParams.setMargins(dp(8), 0, 0, 0);
        bar.addView(videoSpeedLabel, speedParams);

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(Color.WHITE);
        close.setTextSize(24);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> {
            videoControlsEnabled = false;
            saveSettings();
            updateVideoControlsVisibility();
            Toast.makeText(this, "Kontrol video disembunyikan", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        closeParams.setMargins(dp(8), 0, 0, 0);
        bar.addView(close, closeParams);

        return bar;
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

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1);
        params.setMargins(dp(4), 0, dp(4), 0);
        wrap.setLayoutParams(params);
        return wrap;
    }

    private void updateVideoControlsVisibility() {
        if (videoControlsBar == null) return;
        boolean show = videoControlsEnabled && webView != null && webView.getVisibility() == View.VISIBLE;
        videoControlsBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void controlVideo(String action) {
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
        if (videoSpeed < 0.75f) videoSpeed = 1.0f;
        else if (videoSpeed < 1.0f) videoSpeed = 1.25f;
        else if (videoSpeed < 1.25f) videoSpeed = 1.5f;
        else if (videoSpeed < 1.5f) videoSpeed = 2.0f;
        else videoSpeed = 0.5f;

        if (videoSpeedLabel != null) {
            videoSpeedLabel.setText(formatVideoSpeed(videoSpeed));
        }

        String speed = String.valueOf(videoSpeed);
        webView.loadUrl("javascript:(function(){var v=document.querySelector('video');if(v){v.playbackRate=" + speed + ";}})()");
        Toast.makeText(this, "Speed video: " + formatVideoSpeed(videoSpeed), Toast.LENGTH_SHORT).show();
    }

    private String formatVideoSpeed(float speed) {
        if (speed == 1.0f) return "1x";
        if (speed == 2.0f) return "2x";
        if (speed == 0.5f) return "0.5x";
        if (speed == 1.25f) return "1.25x";
        if (speed == 1.5f) return "1.5x";
        return speed + "x";
    }

    private void showHistoryPanel() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(18));
        panel.setBackground(roundRect(Color.parseColor("#171A20"), dp(24), dp(1), Color.parseColor("#2D333D")));
        scroll.addView(panel);

        TextView title = new TextView(this);
        title.setText("Riwayat browsing");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(title);

        if (historyData.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Riwayat masih kosong.");
            empty.setTextColor(COLOR_SUBTEXT);
            empty.setTextSize(14);
            empty.setPadding(0, dp(16), 0, dp(16));
            panel.addView(empty);
        } else {
            for (HistoryItemData item : historyData) {
                panel.addView(historyRow(item, dialog));
            }

            TextView clear = new TextView(this);
            clear.setText("Bersihkan semua riwayat");
            clear.setTextColor(Color.WHITE);
            clear.setGravity(Gravity.CENTER);
            clear.setTypeface(Typeface.DEFAULT_BOLD);
            clear.setTextSize(15);
            clear.setBackground(roundRect(Color.parseColor("#E5484D"), dp(18), 0, Color.TRANSPARENT));
            LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(-1, dp(48));
            clearParams.setMargins(0, dp(12), 0, 0);
            panel.addView(clear, clearParams);
            clear.setOnClickListener(v -> {
                historyData.clear();
                saveBrowserHistory();
                dialog.dismiss();
                Toast.makeText(this, "Riwayat dibersihkan", Toast.LENGTH_SHORT).show();
            });
        }

        dialog.setContentView(scroll);
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.75f);
            window.setAttributes(lp);
        }
        dialog.show();
    }

    private View historyRow(HistoryItemData item, Dialog dialog) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(11), dp(10), dp(11));
        row.setOnClickListener(v -> {
            dialog.dismiss();
            addressBar.setText(item.url);
            openAddressBarUrl();
        });

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_history);
        icon.setColorFilter(Color.parseColor("#F3F5F8"));
        row.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMargins(dp(14), 0, 0, 0);

        TextView title = new TextView(this);
        title.setText(item.title == null || item.title.length() == 0 ? item.url : item.title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setSingleLine(true);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        texts.addView(title);

        TextView url = new TextView(this);
        url.setText(item.url);
        url.setTextColor(COLOR_SUBTEXT);
        url.setTextSize(12);
        url.setSingleLine(true);
        texts.addView(url);

        row.addView(texts, params);
        return row;
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
        panel.setBackground(roundRect(Color.parseColor("#171A20"), dp(24), dp(1), Color.parseColor("#2D333D")));

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

    private void showDownloadFolderDialog(Dialog parentDialog) {
        final EditText input = new EditText(this);
        input.setText(downloadSubfolder);
        input.setSingleLine(true);
        input.setHint("Nama subfolder");
        input.setTextColor(Color.BLACK);

        new AlertDialog.Builder(this)
                .setTitle("Subfolder unduhan")
                .setMessage("Folder berada di dalam direktori app Yield Browser agar aman di Android 11.")
                .setView(input)
                .setPositiveButton("Simpan", (d, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.length() == 0) value = "Download";
                    value = value.replace("/", "-").replace("\\\\", "-");
                    downloadSubfolder = value;
                    saveSettings();
                    Toast.makeText(this, "Folder unduhan diubah", Toast.LENGTH_SHORT).show();
                    parentDialog.dismiss();
                    showDownloadSettingsPanel();
                })
                .setNegativeButton("Batal", null)
                .show();
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
        row.setOnClickListener(listener);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(70));
        rowParams.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#E7E8EA"));
        row.addView(icon, new LinearLayout.LayoutParams(dp(26), dp(26)));

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(17);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMargins(dp(18), 0, dp(12), 0);
        row.addView(text, textParams);

        TextView toggle = new TextView(this);
        toggle.setText(enabled ? "ON" : "OFF");
        toggle.setTextColor(Color.WHITE);
        toggle.setGravity(Gravity.CENTER);
        toggle.setTypeface(Typeface.DEFAULT_BOLD);
        toggle.setTextSize(12);
        toggle.setBackground(roundRect(enabled ? Color.parseColor("#FF715C") : Color.parseColor("#5A5D63"), dp(18), 0, Color.TRANSPARENT));
        row.addView(toggle, new LinearLayout.LayoutParams(dp(58), dp(34)));

        return row;
    }

    private View fixedMenuRow(int iconRes, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(roundRect(Color.parseColor("#303236"), dp(10), 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(64));
        rowParams.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#E7E8EA"));
        row.addView(icon, new LinearLayout.LayoutParams(dp(26), dp(26)));

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(17);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMargins(dp(18), 0, dp(12), 0);
        row.addView(text, textParams);

        TextView fixed = new TextView(this);
        fixed.setText("Tetap");
        fixed.setTextColor(COLOR_SUBTEXT);
        fixed.setGravity(Gravity.CENTER);
        fixed.setTypeface(Typeface.DEFAULT_BOLD);
        fixed.setTextSize(12);
        fixed.setBackground(roundRect(Color.parseColor("#24262B"), dp(18), dp(1), COLOR_BORDER));
        row.addView(fixed, new LinearLayout.LayoutParams(dp(58), dp(34)));

        return row;
    }

    private View settingRow(int iconRes, String title, String desc, boolean enabled, View.OnClickListener listener) {
        LinearLayout row = baseSettingsRow(iconRes, title, desc, listener);
        TextView status = new TextView(this);
        status.setText(enabled ? "ON" : "OFF");
        status.setTextColor(enabled ? COLOR_ON : COLOR_SUBTEXT);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setBackground(roundRect(enabled ? Color.parseColor("#15351F") : Color.parseColor("#2A2E36"), dp(12), dp(1), enabled ? COLOR_ON : COLOR_BORDER));
        row.addView(status, new LinearLayout.LayoutParams(dp(46), dp(28)));
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
        root.setBackgroundColor(Color.parseColor("#0C0D10"));

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
                activeDownloadDialog.dismiss();
                showDownloadManager();
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
        String name = safeText(item.fileName).toLowerCase(Locale.US);
        if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm") || name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".3gp")) return "Video";
        if (name.endsWith(".apk") || name.endsWith(".xapk") || name.endsWith(".apks")) return "APK";
        if (name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".flac")) return "Musik";
        if (name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".ppt") || name.endsWith(".pptx") || name.endsWith(".txt")) return "Dokumen";
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
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(34), dp(34));
            checkParams.setMargins(0, 0, dp(10), 0);
            row.addView(check, checkParams);
        }

        ImageView icon = new ImageView(this);
        icon.setImageResource(getDownloadIcon(item));
        icon.setColorFilter(Color.parseColor("#E8EAED"));
        icon.setPadding(dp(13), dp(13), dp(13), dp(13));
        icon.setBackground(roundRect(Color.parseColor("#1E222A"), dp(18), 0, Color.TRANSPARENT));
        row.addView(icon, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout textWrap = new LinearLayout(this);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMargins(dp(12), 0, dp(8), 0);

        TextView name = new TextView(this);
        name.setText(item.fileName);
        name.setTextColor(Color.WHITE);
        name.setTextSize(15);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        textWrap.addView(name);

        TextView sub = new TextView(this);
        sub.setText(getDownloadSubtitle(item));
        sub.setTextColor(COLOR_SUBTEXT);
        sub.setTextSize(13);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        textWrap.addView(sub);

        if ("running".equals(item.status) || "paused".equals(item.status)) {
            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress(item.progress);
            bar.setProgressDrawable(new ColorDrawable(COLOR_ACCENT));
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(-1, dp(4));
            barParams.setMargins(0, dp(8), 0, 0);
            textWrap.addView(bar, barParams);

            TextView percent = new TextView(this);
            percent.setText(item.progress + "%");
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
            action.setTextSize(18);
            action.setTypeface(Typeface.DEFAULT_BOLD);
            action.setGravity(Gravity.CENTER);
            action.setBackground(roundRect(Color.parseColor("#20232A"), dp(18), dp(1), COLOR_BORDER));
            action.setOnClickListener(v -> {
                if ("running".equals(item.status)) pauseDownloadItem(item);
                else if ("paused".equals(item.status)) resumeDownloadItem(item);
                else reloadDownloadItem(item);
            });
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(dp(40), dp(40));
            actionParams.setMargins(0, 0, dp(4), 0);
            row.addView(action, actionParams);
        }

        TextView more = new TextView(this);
        more.setText("⋮");
        more.setTextColor(Color.parseColor("#D6D9DE"));
        more.setTextSize(30);
        more.setGravity(Gravity.CENTER);
        more.setOnClickListener(v -> showDownloadItemMenu(v, item));
        row.addView(more, new LinearLayout.LayoutParams(dp(42), dp(56)));

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

    private int getDownloadIcon(DownloadItem item) {
        String cat = getDownloadCategory(item);
        if ("Video".equals(cat)) return R.drawable.ic_video_control;
        if ("APK".equals(cat)) return R.drawable.ic_file;
        if ("Musik".equals(cat)) return R.drawable.ic_file;
        if ("Dokumen".equals(cat)) return R.drawable.ic_file;
        if ("running".equals(item.status) || "paused".equals(item.status)) return R.drawable.ic_download_modern;
        return R.drawable.ic_file;
    }

    private String getDownloadSubtitle(DownloadItem item) {
        String size = readableFileSize(getDownloadSize(item));
        String host = getDownloadHost(item);
        String engine = getConnectionLabel(item);
        String status;

        if ("running".equals(item.status)) {
            status = "download • " + item.progress + "% • " + engine;
        } else if ("paused".equals(item.status)) {
            status = "dijeda • " + item.progress + "% • " + engine;
        } else if ("failed".equals(item.status)) {
            status = "terputus/gagal • reload tersedia • " + engine;
        } else {
            status = host.length() > 0 ? host + " • " + engine : "selesai • " + engine;
        }

        if (host.length() > 0 && ("running".equals(item.status) || "paused".equals(item.status) || "failed".equals(item.status))) {
            return size + " • " + status + " • " + host;
        }
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
        if (item == null) return "Menunggu koneksi";
        if (item.connectionCount >= 2) return item.connectionCount + " koneksi paralel";
        if (item.connectionCount == 1) return "1 koneksi";
        return "Mengecek koneksi";
    }

    private void pauseDownloadItem(DownloadItem item) {
        if (item == null || !"running".equals(item.status)) return;
        item.pauseRequested = true;
        item.status = "paused";
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
            if (out.exists()) out.delete();
            item.progress = 0;
            item.downloadedBytes = 0;
            item.totalBytes = 0;
            item.connectionCount = 0;
            item.status = "running";
            item.pauseRequested = false;
            item.engineInfo = "Melanjutkan ulang split download";
            saveDownloadHistory();
            refreshDownloadPanel();
            showDownloadNotification(item, "Melanjutkan unduhan", true);
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
            item.engineInfo = "Reload dari awal • mengecek split download";

            File out = new File(item.path);
            if (out.exists()) {
                out.delete();
            }

            saveDownloadHistory();
            refreshDownloadPanel();
            showDownloadNotification(item, "Reload dari awal", true);
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
            popup.getMenu().add(0, 12, 1, "Reload dari awal");
        } else if ("failed".equals(item.status)) {
            popup.getMenu().add(0, 12, 0, "Reload dari awal");
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
            File file = new File(item.path);
            if (!file.exists()) {
                Toast.makeText(this, "File tidak ditemukan", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                StrictMode.class.getMethod("disableDeathOnFileUriExposure").invoke(null);
            } catch (Exception ignored) {}
            Uri uri = Uri.fromFile(file);
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
        synchronized (downloadItems) {
            downloadItems.remove(item);
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

    private void openDownloadedFile(DownloadItem item) {
        try {
            File file = new File(item.path);
            if (!file.exists()) {
                Toast.makeText(this, "File tidak ditemukan", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                StrictMode.class.getMethod("disableDeathOnFileUriExposure").invoke(null);
            } catch (Exception ignored) {}
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(file);
            String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
            String mime = ext != null ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.US)) : null;
            if (mime == null) mime = "*/*";
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Tidak ada aplikasi untuk membuka file ini", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshDownloadPanel() {
        if (activeDownloadListPanel == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            renderDownloadList();
        } else {
            mainHandler.post(this::renderDownloadList);
        }
    }

    private void beginDownloadFromWeb(String url, String contentDisposition, String mimeType) {
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        beginDownload(url, fileName);
    }

    private void beginDownload(String fileUrl, String guessedFileName) {
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

            File out = uniqueFile(new File(dir, fileName));
            DownloadItem item = new DownloadItem(nextDownloadId++, fileUrl, out.getName(), out.getAbsolutePath(), "running", 0);
            item.connectionCount = 0;
            item.engineInfo = "Menyiapkan split download";
            synchronized (downloadItems) {
                downloadItems.add(0, item);
            }
            refreshDownloadPanel();
            showDownloadNotification(item, "Mulai mengunduh", true);
            Toast.makeText(this, "Unduhan dimulai. Ketuk notifikasi untuk detail.", Toast.LENGTH_LONG).show();

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

    private void applyDownloadHeaders(HttpURLConnection conn, String fileUrl) {
        try {
            String ua = webView != null ? webView.getSettings().getUserAgentString() : null;
            if (ua == null || ua.length() == 0) {
                ua = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36 YieldBrowser";
            }
            conn.setRequestProperty("User-Agent", ua);
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Connection", "keep-alive");
            String cookie = CookieManager.getInstance().getCookie(fileUrl);
            if (cookie != null && cookie.length() > 0) {
                conn.setRequestProperty("Cookie", cookie);
            }
        } catch (Exception ignored) {
        }
    }

    private void startTwoConnectionDownload(DownloadItem item, File out) {
        item.status = "running";
        item.pauseRequested = false;
        item.engineInfo = "Mengecek dukungan split download";
        refreshDownloadPanel();

        new Thread(() -> {
            try {
                HttpURLConnection head = (HttpURLConnection) new URL(item.url).openConnection();
                head.setRequestMethod("GET");
                head.setRequestProperty("Range", "bytes=0-0");
                applyDownloadHeaders(head, item.url);
                head.connect();

                int response = head.getResponseCode();
                String contentRange = head.getHeaderField("Content-Range");
                long total = parseTotalSize(contentRange);

                if (item.pauseRequested || "paused".equals(item.status)) {
                    head.disconnect();
                    return;
                }

                if (response == 206 && total > 1) {
                    head.disconnect();
                    item.connectionCount = 2;
                    item.engineInfo = "IDM-style split • 2 koneksi paralel";
                    item.totalBytes = total;
                    item.downloadedBytes = 0;
                    item.progress = 0;
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    showDownloadNotification(item, "Mengunduh 2 koneksi", true);

                    RandomAccessFile raf = new RandomAccessFile(out, "rw");
                    raf.setLength(total);
                    raf.close();

                    long mid = total / 2;
                    long[] done = new long[]{0};
                    boolean[] ok = new boolean[]{true};
                    Thread t1 = new Thread(() -> downloadRange(item, out, 0, mid, done, total, ok));
                    Thread t2 = new Thread(() -> downloadRange(item, out, mid + 1, total - 1, done, total, ok));
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
                    else failDownload(item, "Koneksi split gagal");
                } else {
                    head.disconnect();
                    item.connectionCount = 1;
                    item.engineInfo = "Server tidak support split • 1 koneksi";
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

    private void downloadRange(DownloadItem item, File out, long start, long end, long[] done, long total, boolean[] ok) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(item.url).openConnection();
            conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
            applyDownloadHeaders(conn, item.url);
            conn.connect();

            InputStream in = conn.getInputStream();
            RandomAccessFile raf = new RandomAccessFile(out, "rw");
            raf.seek(start);

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                if (item.pauseRequested || "paused".equals(item.status)) {
                    ok[0] = false;
                    break;
                }
                raf.write(buffer, 0, len);
                synchronized (done) {
                    done[0] += len;
                    item.downloadedBytes = done[0];
                    int percent = (int) Math.min(99, (done[0] * 100) / total);
                    if (percent != item.progress) {
                        item.progress = percent;
                        refreshDownloadPanel();
                        showDownloadNotification(item, "2 koneksi paralel • " + percent + "%", true);
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
            HttpURLConnection conn = (HttpURLConnection) new URL(item.url).openConnection();
            applyDownloadHeaders(conn, item.url);
            conn.connect();
            long total = conn.getContentLengthLong();
            item.totalBytes = total;
            InputStream in = conn.getInputStream();
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buffer = new byte[8192];
            int len;
            long done = 0;
            while ((len = in.read(buffer)) != -1) {
                if (item.pauseRequested || "paused".equals(item.status)) {
                    break;
                }
                fos.write(buffer, 0, len);
                done += len;
                item.downloadedBytes = done;
                if (total > 0) {
                    int percent = (int) Math.min(99, (done * 100) / total);
                    if (percent != item.progress) {
                        item.progress = percent;
                        refreshDownloadPanel();
                        showDownloadNotification(item, "1 koneksi • " + percent + "%", true);
                    }
                }
            }
            fos.close();
            in.close();
            conn.disconnect();
            if (item.pauseRequested || "paused".equals(item.status)) {
                item.status = "paused";
                item.engineInfo = getConnectionLabel(item) + " • dijeda";
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, "Unduhan dijeda", false);
            } else {
                completeDownload(item);
            }
        } catch (Exception e) {
            failDownload(item, e.getMessage());
        }
    }

    private void completeDownload(DownloadItem item) {
        item.progress = 100;
        item.status = "completed";
        saveDownloadHistory();
        refreshDownloadPanel();
        showDownloadNotification(item, "Unduhan selesai", false);
        runOnUiThread(() -> Toast.makeText(this, "Unduhan selesai: " + item.fileName, Toast.LENGTH_SHORT).show());
    }

    private void failDownload(DownloadItem item, String reason) {
        item.status = "failed";
        item.pauseRequested = false;
        if (item.engineInfo == null || item.engineInfo.length() == 0) {
            item.engineInfo = getConnectionLabel(item) + " • terputus/gagal";
        } else {
            item.engineInfo = item.engineInfo + " • terputus/gagal";
        }
        saveDownloadHistory();
        refreshDownloadPanel();
        showDownloadNotification(item, "Unduhan gagal • klik untuk detail", false);
        runOnUiThread(() -> Toast.makeText(this, "Unduhan gagal. Klik reload untuk download dari awal.", Toast.LENGTH_LONG).show());
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

    private void showDownloadNotification(DownloadItem item, String text, boolean ongoing) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(ACTION_OPEN_DOWNLOADS);
        intent.setData(Uri.parse("yieldbrowser://downloads/" + item.id + "/" + System.currentTimeMillis()));
        intent.putExtra("open_downloads", true);
        intent.putExtra("download_id", item.id);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getActivity(this, item.id + 9000, intent, flags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_DOWNLOADS)
                : new Notification.Builder(this);

        String line = text;
        if ("running".equals(item.status)) {
            line = text + " • " + getConnectionLabel(item);
        } else if ("paused".equals(item.status)) {
            line = "Dijeda • " + getConnectionLabel(item);
        }

        builder.setSmallIcon(R.drawable.ic_download_modern)
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
                if (item.status.equals("completed") || item.status.equals("failed")) {
                    saved.add(encode(item.url) + "|" + encode(item.fileName) + "|" + encode(item.path) + "|" + item.status + "|" + item.progress + "|" + item.totalBytes + "|" + item.downloadedBytes + "|" + item.connectionCount + "|" + encode(item.engineInfo));
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
                            item.engineInfo = item.connectionCount > 1 ? "IDM-style split • 2 koneksi paralel" : "1 koneksi";
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
        Set<String> bookmarks = getBookmarks();
        if (bookmarks.contains(url)) {
            bookmarks.remove(url);
            saveBookmarks(bookmarks);
            Toast.makeText(this, "Bookmark dihapus", Toast.LENGTH_SHORT).show();
        } else {
            bookmarks.add(url);
            saveBookmarks(bookmarks);
            Toast.makeText(this, "Situs ditambahkan ke bookmark", Toast.LENGTH_SHORT).show();
        }
        updateTopActionStates();
    }

    private void showBookmarkList() {
        Set<String> bookmarks = getBookmarks();
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, "Bookmark masih kosong", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = bookmarks.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Bookmark").setItems(items, (dialog, which) -> {
            addressBar.setText(items[which]);
            openAddressBarUrl();
        }).setNegativeButton("Tutup", null).show();
    }

    private void toggleTranslate() {
        translateEnabled = !translateEnabled;
        updateTopActionStates();
        if (webView.getVisibility() != View.VISIBLE) {
            Toast.makeText(this, translateEnabled ? "Mode translate aktif" : "Mode translate nonaktif", Toast.LENGTH_SHORT).show();
            return;
        }
        String raw = extractOriginalUrl(webView.getUrl());
        if (translateEnabled) {
            loadTranslatedPage(raw);
            Toast.makeText(this, "Translate aktif", Toast.LENGTH_SHORT).show();
        } else {
            if (raw != null && raw.length() > 0) webView.loadUrl(raw);
            Toast.makeText(this, "Translate nonaktif", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTranslatedPage(String originalUrl) {
        if (originalUrl == null || originalUrl.length() == 0) return;
        try {
            String encoded = URLEncoder.encode(originalUrl, "UTF-8");
            webView.loadUrl("https://translate.google.com/translate?sl=auto&tl=id&u=" + encoded);
        } catch (Exception e) {
            webView.loadUrl(originalUrl);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        applyBrowserSettings();
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            beginDownloadFromWeb(url, contentDisposition, mimeType);
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String u = request.getUrl().toString();
                if (safeMode && isUnsafeUrl(u)) {
                    Toast.makeText(MainActivity.this, "Diblokir Safe Browsing sederhana", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String u = request.getUrl().toString().toLowerCase();
                if (adBlock && isAdUrl(u)) {
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                String shownUrl = extractOriginalUrl(url);
                addressBar.setText(shownUrl != null ? shownUrl : url);
                progressBar.setVisibility(View.GONE);
                if (readerMode) injectReaderMode();
                if (adBlock) injectPremiumAdBlock();
                updateVideoControlsVisibility();
                addBrowserHistory(view.getTitle(), shownUrl != null ? shownUrl : url);
                updateTopActionStates();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
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
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(speedMode ? WebSettings.LOAD_CACHE_ELSE_NETWORK : WebSettings.LOAD_DEFAULT);
        settings.setLoadsImagesAutomatically(!dataSaver);
        settings.setTextZoom(textZoom);
        if (desktopMode) {
            settings.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/120 Safari/537.36 YieldBrowser");
        } else {
            settings.setUserAgentString(null);
        }
    }

    private boolean isUnsafeUrl(String url) {
        String u = url.toLowerCase();
        return u.contains("phishing") || u.contains("malware") || u.contains("virus") || u.contains("scam");
    }

    private boolean isAdUrl(String url) {
        if (!adBlock || url == null) return false;
        String u = url.toLowerCase();

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
                "adcolony.com"
        };

        for (String b : blocked) {
            if (u.contains(b)) return true;
        }

        return isYoutubeAdUrl(u) || isPopUnderOrAdAsset(u);
    }

    private boolean isYoutubeAdUrl(String u) {
        return u.contains("youtube.com/pagead/")
                || u.contains("youtube.com/api/stats/ads")
                || u.contains("youtube.com/get_midroll_info")
                || u.contains("youtube.com/ptracking")
                || u.contains("youtube.com/pcs/activeview")
                || u.contains("youtube.com/youtubei/v1/log_event")
                || u.contains("youtube.com/youtubei/v1/player/ad_break")
                || u.contains("youtube.com/youtubei/v1/ad")
                || u.contains("googlevideo.com/initplayback")
                || (u.contains("googlevideo.com/videoplayback") && (u.contains("oad=") || u.contains("ctier=") || u.contains("oad=1")));
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
                || u.contains("vast")
                || u.contains("vpaid");
    }

    private void injectPremiumAdBlock() {
        if (webView == null) return;
        String js = "javascript:(function(){"
                + "if(window.__yieldAdBlockPremium)return;window.__yieldAdBlockPremium=true;"
                + "function hide(s){try{document.querySelectorAll(s).forEach(function(e){e.style.setProperty('display','none','important');e.remove&&e.remove();});}catch(x){}}"
                + "function clean(){"
                + "var selectors=["
                + "'.adsbygoogle','iframe[id*=ad]','iframe[src*=ads]','iframe[src*=doubleclick]',"
                + "'[id*=ad-]','[id^=ad_]','[class*=ad-]','[class*=ads-]','[class*=advert]',"
                + "'ytd-display-ad-renderer','ytd-promoted-video-renderer','ytd-ad-slot-renderer','ytd-companion-slot-renderer',"
                + "'ytd-banner-promo-renderer','ytd-in-feed-ad-layout-renderer','ytd-promoted-sparkles-web-renderer',"
                + "'.ytp-ad-module','.video-ads','.ytp-ad-overlay-container','.ytp-ad-player-overlay','.ytp-ad-text','.ytp-ad-image-overlay',"
                + "'.ytp-ad-skip-button-container','.ytp-ad-preview-container','.ytp-ad-progress-list','.ytp-ad-player-overlay-layout',"
                + "'.GoogleActiveViewElement','.ad-showing'];"
                + "selectors.forEach(hide);"
                + "try{document.querySelectorAll('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button').forEach(function(b){b.click();});}catch(e){}"
                + "try{var v=document.querySelector('video');if(v&&document.querySelector('.ad-showing')){v.muted=true;if(isFinite(v.duration)&&v.duration>0)v.currentTime=v.duration;}}catch(e){}"
                + "try{document.body.style.setProperty('overflow','auto','important');}catch(e){}"
                + "}"
                + "clean();setInterval(clean,700);"
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

        webView.setVisibility(View.VISIBLE);
        homeScroll.setVisibility(View.GONE);
        updateVideoControlsVisibility();
        if (translateEnabled) loadTranslatedPage(url);
        else webView.loadUrl(url);
        updateTopActionStates();
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

    private void showHome() {
        homeScroll.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        updateVideoControlsVisibility();
        updateTopActionStates();
    }

    private void updateTopActionStates() {
        if (bookmarkButton != null) {
            String url = getEffectiveCurrentUrl();
            boolean bookmarked = url != null && getBookmarks().contains(url);
            bookmarkButton.setColorFilter(bookmarked ? COLOR_ACCENT : Color.parseColor("#E9EDF5"));
        }
        if (translateButton != null) translateButton.setColorFilter(translateEnabled ? COLOR_ACCENT : Color.parseColor("#E9EDF5"));
    }

    private String getEffectiveCurrentUrl() {
        if (webView != null && webView.getVisibility() == View.VISIBLE) {
            String raw = extractOriginalUrl(webView.getUrl());
            if (raw != null && raw.length() > 0) return raw;
        }
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
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        Set<String> saved = prefs.getStringSet(KEY_BOOKMARKS, new HashSet<>());
        return new HashSet<>(saved);
    }

    private void saveBookmarks(Set<String> bookmarks) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putStringSet(KEY_BOOKMARKS, new HashSet<>(bookmarks)).apply();
    }

    private void loadSettings() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        speedMode = p.getBoolean("speedMode", false);
        safeMode = p.getBoolean("safeMode", true);
        nightMode = p.getBoolean("nightMode", true);
        readerMode = p.getBoolean("readerMode", false);
        adBlock = p.getBoolean("adBlock", false);
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
        shortcutVideoControls = p.getBoolean("shortcutVideoControls", false);
        videoSpeed = p.getFloat("videoSpeed", 1.0f);
        downloadSubfolder = p.getString("downloadSubfolder", "Download");
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
                .putBoolean("nightMode", nightMode)
                .putBoolean("readerMode", readerMode)
                .putBoolean("adBlock", adBlock)
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
                .putBoolean("shortcutVideoControls", shortcutVideoControls)
                .putFloat("videoSpeed", videoSpeed)
                .putString("downloadSubfolder", downloadSubfolder)
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

    @Override
    public void onBackPressed() {
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
