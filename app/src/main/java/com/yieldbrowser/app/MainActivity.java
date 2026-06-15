
package com.yieldbrowser.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.DownloadListener;
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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

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

    private EditText addressBar;
    private ProgressBar progressBar;
    private WebView webView;
    private ScrollView homeScroll;
    private ImageButton bookmarkButton;
    private ImageButton translateButton;
    private int tabCount = 1;

    private boolean translateEnabled = false;
    private boolean speedMode = false;
    private boolean safeMode = true;
    private boolean nightMode = true;
    private boolean readerMode = false;
    private boolean adBlock = false;
    private boolean dataSaver = false;
    private boolean desktopMode = false;
    private int textZoom = 100;
    private String lastDownloadInfo = "Belum ada unduhan.";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();
        getWindow().setStatusBarColor(COLOR_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);

        root.addView(createTopBar(), new LinearLayout.LayoutParams(-1, -2));

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
        root.addView(createBottomNav(), new LinearLayout.LayoutParams(-1, dp(64)));

        setContentView(root);
        updateTopActionStates();
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

        TextView searchHint = new TextView(this);
        searchHint.setText("Telusuri atau ketik URL");
        searchHint.setTextColor(Color.parseColor("#9BA2AE"));
        searchHint.setTextSize(16);
        searchCard.addView(searchHint, new LinearLayout.LayoutParams(0, -2, 1));

        TextView searchButton = new TextView(this);
        searchButton.setText("Cari");
        searchButton.setTextSize(16);
        searchButton.setTypeface(Typeface.DEFAULT_BOLD);
        searchButton.setGravity(Gravity.CENTER);
        searchButton.setTextColor(Color.parseColor("#111111"));
        searchButton.setBackground(roundRect(COLOR_ACCENT, dp(24), 0, Color.TRANSPARENT));
        searchButton.setOnClickListener(v -> openAddressBarUrl());
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
        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        shortcuts.addView(shortcutItem("Google", "G", Color.parseColor("#4285F4"), "https://www.google.com"));
        shortcuts.addView(shortcutItem("GitHub", "GH", Color.parseColor("#24292F"), "https://github.com"));
        shortcuts.addView(shortcutItem("YouTube", "YT", Color.parseColor("#FF0033"), "https://m.youtube.com"));
        shortcuts.addView(shortcutItem("Tambah", "+", Color.parseColor("#1C1F26"), null));
        shortcutScroll.addView(shortcuts);
        content.addView(shortcutScroll, new LinearLayout.LayoutParams(-1, -2));

        content.addView(space(dp(28)));

        TextView more = new TextView(this);
        more.setText("Fitur UC-style tersedia di Menu > Setelan. Download memakai engine Yield 2 koneksi.");
        more.setTextColor(COLOR_SUBTEXT);
        more.setTextSize(14);
        content.addView(more);

        content.addView(space(dp(36)));

        TextView footer = new TextView(this);
        footer.setText("Yield Browser • Home modern + IDM-style downloader");
        footer.setTextColor(Color.parseColor("#808794"));
        footer.setTextSize(13);
        content.addView(footer);

        scrollView.addView(content, new ScrollView.LayoutParams(-1, -1));
        return scrollView;
    }

    private LinearLayout shortcutItem(String label, String badgeText, int badgeColor, final String url) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(dp(84), -2);
        itemParams.setMargins(0, 0, dp(10), 0);
        item.setLayoutParams(itemParams);

        TextView circle = new TextView(this);
        circle.setText(badgeText);
        circle.setTextColor(Color.WHITE);
        circle.setTypeface(Typeface.DEFAULT_BOLD);
        circle.setTextSize(16);
        circle.setGravity(Gravity.CENTER);
        circle.setBackground(roundRect(badgeColor, dp(22), dp(1), Color.parseColor("#31353C")));
        item.addView(circle, new LinearLayout.LayoutParams(dp(60), dp(60)));

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(COLOR_TEXT);
        text.setTextSize(13);
        text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(-1, -2);
        textParams.setMargins(0, dp(8), 0, 0);
        item.addView(text, textParams);

        item.setOnClickListener(v -> {
            if (url == null) {
                Toast.makeText(this, "Tambah shortcut akan dibuat di update berikutnya", Toast.LENGTH_SHORT).show();
            } else {
                addressBar.setText(url);
                openAddressBarUrl();
            }
        });
        return item;
    }

    private View createBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(4), dp(8), dp(6));
        nav.setBackgroundColor(Color.parseColor("#090A0D"));

        nav.addView(bottomNavButton(R.drawable.ic_home, "Home", v -> showHome()));
        nav.addView(bottomNavButton(R.drawable.ic_bookmark, "Bookmark", v -> showBookmarkList()));
        nav.addView(bottomNavButton(R.drawable.ic_search, "Search", v -> addressBar.requestFocus()));
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

        menu.addView(menuRow(R.drawable.ic_download_modern, "Unduhan Yield", v -> {
            dialog.dismiss();
            showDownloadManager();
        }));
        menu.addView(menuRow(R.drawable.ic_bookmark, "Bookmark", v -> {
            dialog.dismiss();
            showBookmarkList();
        }));
        menu.addView(menuRow(R.drawable.ic_private, "Privat", v -> {
            dialog.dismiss();
            Toast.makeText(this, "Mode privat placeholder", Toast.LENGTH_SHORT).show();
        }));
        menu.addView(menuRow(R.drawable.ic_settings, "Setelan", v -> {
            dialog.dismiss();
            showSettingsPanel();
        }));
        menu.addView(menuRow(R.drawable.ic_customize, "Sesuaikan menu", v -> {
            dialog.dismiss();
            Toast.makeText(this, "Kustomisasi menu placeholder", Toast.LENGTH_SHORT).show();
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
        hint.setText("Fitur ala UC tanpa cloud/sync. Semua fitur utama diatur dari sini.");
        hint.setTextColor(COLOR_SUBTEXT);
        hint.setTextSize(13);
        hint.setPadding(dp(8), 0, dp(8), dp(12));
        panel.addView(hint);

        panel.addView(settingRow(R.drawable.ic_speed, "Mode cepat", "Optimasi cache, gambar, dan resource.", speedMode, v -> {
            speedMode = !speedMode;
            applyBrowserSettings();
            saveSettings();
            dialog.dismiss();
            showSettingsPanel();
        }));
        panel.addView(settingRow(R.drawable.ic_safe, "Safe browsing", "Blokir URL berisiko sederhana.", safeMode, v -> {
            safeMode = !safeMode;
            saveSettings();
            dialog.dismiss();
            showSettingsPanel();
        }));
        panel.addView(settingRow(R.drawable.ic_night, "Night mode", "Tampilan gelap untuk home dan menu.", nightMode, v -> {
            nightMode = !nightMode;
            saveSettings();
            Toast.makeText(this, "Night mode akan penuh di versi UI berikutnya", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            showSettingsPanel();
        }));
        panel.addView(settingRow(R.drawable.ic_reader, "Reader / novel mode", "Mode baca ringan untuk artikel.", readerMode, v -> {
            readerMode = !readerMode;
            saveSettings();
            Toast.makeText(this, readerMode ? "Reader mode aktif" : "Reader mode nonaktif", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            showSettingsPanel();
        }));
        panel.addView(settingRow(R.drawable.ic_shield, "Ad block", "Filter iklan sederhana berbasis URL.", adBlock, v -> {
            adBlock = !adBlock;
            saveSettings();
            Toast.makeText(this, adBlock ? "Ad block aktif" : "Ad block nonaktif", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            showSettingsPanel();
        }));
        panel.addView(settingRow(R.drawable.ic_data_saver, "Hemat data", "Matikan gambar otomatis saat browsing.", dataSaver, v -> {
            dataSaver = !dataSaver;
            applyBrowserSettings();
            saveSettings();
            dialog.dismiss();
            showSettingsPanel();
        }));
        panel.addView(settingRow(R.drawable.ic_desktop, "Desktop mode", "Ganti user agent ke desktop.", desktopMode, v -> {
            desktopMode = !desktopMode;
            applyBrowserSettings();
            saveSettings();
            if (webView != null && webView.getVisibility() == View.VISIBLE) webView.reload();
            dialog.dismiss();
            showSettingsPanel();
        }));
        panel.addView(actionRow(R.drawable.ic_text_size, "Ukuran teks: " + textZoom + "%", "Ketuk untuk menaikkan 10%, maksimal 150%.", v -> {
            textZoom += 10;
            if (textZoom > 150) textZoom = 80;
            applyBrowserSettings();
            saveSettings();
            dialog.dismiss();
            showSettingsPanel();
        }));
        panel.addView(actionRow(R.drawable.ic_clear, "Bersihkan cache", "Hapus cache WebView.", v -> {
            if (webView != null) webView.clearCache(true);
            Toast.makeText(this, "Cache dibersihkan", Toast.LENGTH_SHORT).show();
        }));
        panel.addView(actionRow(R.drawable.ic_download_modern, "Download manager Yield", "Engine 2 koneksi paralel seperti IDM.", v -> {
            dialog.dismiss();
            showDownloadManager();
        }));

        dialog.setContentView(scroll);
        if (dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.78f);
            window.setAttributes(lp);
        }
        dialog.show();
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
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setBackground(roundRect(Color.parseColor("#171A20"), dp(24), dp(1), Color.parseColor("#2D333D")));

        TextView title = new TextView(this);
        title.setText("Unduhan Yield");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Download manager internal dengan 2 koneksi paralel. File disimpan di folder Android/data/com.yieldbrowser.install/files/Download.");
        desc.setTextColor(COLOR_SUBTEXT);
        desc.setTextSize(13);
        desc.setPadding(0, dp(8), 0, dp(12));
        panel.addView(desc);

        EditText urlInput = new EditText(this);
        urlInput.setSingleLine(false);
        urlInput.setMinLines(2);
        urlInput.setTextColor(COLOR_TEXT);
        urlInput.setHintTextColor(COLOR_SUBTEXT);
        urlInput.setHint("Tempel URL file di sini");
        urlInput.setText(webView != null && webView.getVisibility() == View.VISIBLE ? webView.getUrl() : "");
        urlInput.setBackground(roundRect(Color.parseColor("#101217"), dp(14), dp(1), COLOR_BORDER));
        urlInput.setPadding(dp(12), dp(8), dp(12), dp(8));
        panel.addView(urlInput, new LinearLayout.LayoutParams(-1, dp(86)));

        TextView start = new TextView(this);
        start.setText("Mulai Download 2 Koneksi");
        start.setTextColor(Color.parseColor("#111111"));
        start.setTextSize(16);
        start.setTypeface(Typeface.DEFAULT_BOLD);
        start.setGravity(Gravity.CENTER);
        start.setBackground(roundRect(COLOR_ACCENT, dp(20), 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(-1, dp(48));
        startParams.setMargins(0, dp(12), 0, dp(10));
        panel.addView(start, startParams);

        TextView info = new TextView(this);
        info.setText(lastDownloadInfo);
        info.setTextColor(COLOR_SUBTEXT);
        info.setTextSize(13);
        panel.addView(info);

        start.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            if (url.length() == 0 || !(url.startsWith("http://") || url.startsWith("https://"))) {
                Toast.makeText(this, "URL file tidak valid", Toast.LENGTH_SHORT).show();
                return;
            }
            lastDownloadInfo = "Memulai unduhan 2 koneksi...";
            info.setText(lastDownloadInfo);
            startTwoConnectionDownload(url, info);
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

    private void startTwoConnectionDownload(String fileUrl, TextView info) {
        new Thread(() -> {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection head = (HttpURLConnection) url.openConnection();
                head.setRequestMethod("GET");
                head.setRequestProperty("Range", "bytes=0-0");
                head.connect();

                String fileName = URLUtil.guessFileName(fileUrl, null, null);
                if (fileName == null || fileName.trim().length() == 0) fileName = "yield_download.bin";

                File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (dir == null) dir = getFilesDir();
                if (!dir.exists()) dir.mkdirs();

                File out = new File(dir, fileName);
                int response = head.getResponseCode();
                String contentRange = head.getHeaderField("Content-Range");
                long total = parseTotalSize(contentRange);

                if (response == 206 && total > 1) {
                    head.disconnect();
                    long mid = total / 2;
                    RandomAccessFile raf = new RandomAccessFile(out, "rw");
                    raf.setLength(total);
                    raf.close();

                    long[] done = new long[]{0};
                    Thread t1 = new Thread(() -> downloadRange(fileUrl, out, 0, mid, done, total, info));
                    Thread t2 = new Thread(() -> downloadRange(fileUrl, out, mid + 1, total - 1, done, total, info));
                    t1.start();
                    t2.start();
                    t1.join();
                    t2.join();

                    lastDownloadInfo = "Selesai: " + out.getAbsolutePath();
                    runOnUiThread(() -> info.setText(lastDownloadInfo));
                } else {
                    head.disconnect();
                    downloadSingle(fileUrl, out, info);
                }
            } catch (Exception e) {
                lastDownloadInfo = "Gagal: " + e.getMessage();
                runOnUiThread(() -> info.setText(lastDownloadInfo));
            }
        }).start();
    }

    private void downloadRange(String fileUrl, File out, long start, long end, long[] done, long total, TextView info) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
            conn.setRequestProperty("Range", "bytes=" + start + "-" + end);
            conn.connect();

            InputStream in = conn.getInputStream();
            RandomAccessFile raf = new RandomAccessFile(out, "rw");
            raf.seek(start);

            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                raf.write(buffer, 0, len);
                synchronized (done) {
                    done[0] += len;
                    int percent = (int) Math.min(100, (done[0] * 100) / total);
                    runOnUiThread(() -> info.setText("Mengunduh 2 koneksi... " + percent + "%"));
                }
            }
            raf.close();
            in.close();
            conn.disconnect();
        } catch (Exception ignored) {
        }
    }

    private void downloadSingle(String fileUrl, File out, TextView info) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
            conn.connect();
            long total = conn.getContentLengthLong();
            InputStream in = conn.getInputStream();
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buffer = new byte[8192];
            int len;
            long done = 0;
            while ((len = in.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                done += len;
                if (total > 0) {
                    int percent = (int) Math.min(100, (done * 100) / total);
                    runOnUiThread(() -> info.setText("Server tidak support range. Mengunduh 1 koneksi... " + percent + "%"));
                }
            }
            fos.close();
            in.close();
            conn.disconnect();

            lastDownloadInfo = "Selesai: " + out.getAbsolutePath();
            runOnUiThread(() -> info.setText(lastDownloadInfo));
        } catch (Exception e) {
            lastDownloadInfo = "Gagal: " + e.getMessage();
            runOnUiThread(() -> info.setText(lastDownloadInfo));
        }
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
        new AlertDialog.Builder(this)
                .setTitle("Bookmark")
                .setItems(items, (dialog, which) -> {
                    addressBar.setText(items[which]);
                    openAddressBarUrl();
                })
                .setNegativeButton("Tutup", null)
                .show();
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

    private void configureWebView() {
        applyBrowserSettings();
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            lastDownloadInfo = "Terdeteksi file: " + URLUtil.guessFileName(url, contentDisposition, mimeType);
            showDownloadManagerWithUrl(url);
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
                    return new WebResourceResponse("text/plain", "utf-8", null);
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                String shownUrl = extractOriginalUrl(url);
                addressBar.setText(shownUrl != null ? shownUrl : url);
                progressBar.setVisibility(View.GONE);
                if (readerMode) injectReaderMode();
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

    private void showDownloadManagerWithUrl(String url) {
        showDownloadManager();
        Toast.makeText(this, "Buka Unduhan Yield, URL file sudah bisa ditempel otomatis di versi berikutnya.", Toast.LENGTH_LONG).show();
        lastDownloadInfo = "URL file: " + url;
    }

    private boolean isUnsafeUrl(String url) {
        String u = url.toLowerCase();
        return u.contains("phishing") || u.contains("malware") || u.contains("virus") || u.contains("scam");
    }

    private boolean isAdUrl(String url) {
        return url.contains("doubleclick") || url.contains("googlesyndication") || url.contains("/ads") || url.contains("adservice") || url.contains("tracking");
    }

    private void injectReaderMode() {
        String js = "javascript:(function(){document.body.style.maxWidth='720px';document.body.style.margin='auto';document.body.style.lineHeight='1.7';document.body.style.fontSize='18px';document.body.style.background='#111318';document.body.style.color='#F5F7FA';})()";
        webView.loadUrl(js);
    }

    private void openAddressBarUrl() {
        String text = addressBar.getText().toString().trim();
        if (text.length() == 0) {
            showHome();
            return;
        }
        String url;
        if (text.startsWith("http://") || text.startsWith("https://")) {
            url = text;
        } else if (text.contains(".") && !text.contains(" ")) {
            url = "https://" + text;
        } else {
            url = "https://www.google.com/search?q=" + text.replace(" ", "+");
        }
        webView.setVisibility(View.VISIBLE);
        homeScroll.setVisibility(View.GONE);
        if (translateEnabled) loadTranslatedPage(url);
        else webView.loadUrl(url);
        updateTopActionStates();
    }

    private void showHome() {
        homeScroll.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        updateTopActionStates();
    }

    private void updateTopActionStates() {
        if (bookmarkButton != null) {
            String url = getEffectiveCurrentUrl();
            boolean bookmarked = url != null && getBookmarks().contains(url);
            bookmarkButton.setColorFilter(bookmarked ? COLOR_ACCENT : Color.parseColor("#E9EDF5"));
        }
        if (translateButton != null) {
            translateButton.setColorFilter(translateEnabled ? COLOR_ACCENT : Color.parseColor("#E9EDF5"));
        }
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
            if (idx != -1) {
                return url.substring(idx + 3).replace("%3A", ":").replace("%2F", "/").replace("%3F", "?").replace("%3D", "=").replace("%26", "&");
            }
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
