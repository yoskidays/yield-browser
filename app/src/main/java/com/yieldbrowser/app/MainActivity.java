package com.yieldbrowser.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
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

public class MainActivity extends Activity {
    private static final int COLOR_BG = Color.parseColor("#0C0D10");
    private static final int COLOR_SURFACE = Color.parseColor("#17191D");
    private static final int COLOR_SURFACE_2 = Color.parseColor("#20232A");
    private static final int COLOR_BORDER = Color.parseColor("#2A2E36");
    private static final int COLOR_TEXT = Color.parseColor("#F5F7FA");
    private static final int COLOR_SUBTEXT = Color.parseColor("#B7BDC8");
    private static final int COLOR_ACCENT = Color.parseColor("#F39A22");

    private EditText addressBar;
    private ProgressBar progressBar;
    private WebView webView;
    private ScrollView homeScroll;
    private int tabCount = 1;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        addressBar.setPadding(dp(10), 0, dp(10), 0);
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
            if (actionId == EditorInfo.IME_ACTION_GO || isEnter) {
                openAddressBarUrl();
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams addressParams = new LinearLayout.LayoutParams(0, -2, 1);
        bar.addView(addressBar, addressParams);

        ImageButton download = smallTopIcon(R.drawable.ic_download, "Unduhan", v -> Toast.makeText(this, "Menu unduhan akan ditambahkan di fitur berikutnya", Toast.LENGTH_SHORT).show());
        bar.addView(download, new LinearLayout.LayoutParams(dp(34), dp(34)));

        ImageButton menu = smallTopIcon(R.drawable.ic_menu_more, "Menu", v -> showQuickMenu());
        LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        menuParams.setMargins(dp(4), 0, 0, 0);
        bar.addView(menu, menuParams);

        wrap.addView(bar, new LinearLayout.LayoutParams(-1, dp(52)));
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
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(0, -2, 1);
        searchCard.addView(searchHint, hintParams);

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
        more.setText("Tambahkan shortcut favorit atau mulai mencari dari kolom di atas.");
        more.setTextColor(COLOR_SUBTEXT);
        more.setTextSize(14);
        content.addView(more);

        content.addView(space(dp(36)));

        TextView footer = new TextView(this);
        footer.setText("Yield Browser • Home awal mirip browser modern");
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
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(dp(92), -2);
        itemParams.setMargins(0, 0, dp(12), 0);
        item.setLayoutParams(itemParams);

        TextView circle = new TextView(this);
        circle.setText(badgeText);
        circle.setTextColor(Color.WHITE);
        circle.setTypeface(Typeface.DEFAULT_BOLD);
        circle.setTextSize(18);
        circle.setGravity(Gravity.CENTER);
        circle.setBackground(roundRect(badgeColor, dp(26), dp(1), Color.parseColor("#31353C")));
        LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        item.addView(circle, circleParams);

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(COLOR_TEXT);
        text.setTextSize(14);
        text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(-1, -2);
        textParams.setMargins(0, dp(10), 0, 0);
        item.addView(text, textParams);

        item.setOnClickListener(v -> {
            if (url == null) {
                Toast.makeText(this, "Tambah shortcut akan dibuat di update berikutnya", Toast.LENGTH_SHORT).show();
            } else {
                addressBar.setText(url);
                openAddressBarUrl();
            }
        });
        item.setOnLongClickListener(v -> {
            Toast.makeText(this, "Edit shortcut: " + label, Toast.LENGTH_SHORT).show();
            return true;
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
        nav.addView(bottomNavButton(R.drawable.ic_bookmark, "Bookmark", v -> Toast.makeText(this, "Bookmark akan disiapkan di update berikutnya", Toast.LENGTH_SHORT).show()));
        nav.addView(bottomNavButton(R.drawable.ic_search, "Search", v -> addressBar.requestFocus()));
        nav.addView(bottomNavButton(R.drawable.ic_tabs, "Tabs", v -> Toast.makeText(this, "Jumlah tab: " + tabCount, Toast.LENGTH_SHORT).show()));
        nav.addView(bottomNavButton(R.drawable.ic_menu_more, "Menu", v -> showQuickMenu()));
        return nav;
    }

    private LinearLayout bottomNavButton(int iconRes, String description, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setOnClickListener(listener);
        item.setContentDescription(description);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        item.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#F6F7FA"));
        item.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));

        if ("Tabs".equals(description)) {
            TextView count = new TextView(this);
            count.setText(String.valueOf(tabCount));
            count.setTextColor(Color.parseColor("#D7DBE3"));
            count.setTextSize(10);
            count.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(-2, -2);
            countParams.setMargins(0, dp(2), 0, 0);
            item.addView(count, countParams);
        }
        return item;
    }

    private void showQuickMenu() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(12), dp(12), dp(12), dp(12));
        menu.setBackground(roundRect(Color.parseColor("#171A20"), dp(24), dp(1), Color.parseColor("#2D333D")));

        menu.addView(menuRow(R.drawable.ic_download, "Unduhan", v -> {
            dialog.dismiss();
            Toast.makeText(this, "Unduhan akan diaktifkan bersama fitur download manager", Toast.LENGTH_SHORT).show();
        }));
        menu.addView(menuRow(R.drawable.ic_bookmark, "Bookmark", v -> {
            dialog.dismiss();
            Toast.makeText(this, "Bookmark akan dibuat di update berikutnya", Toast.LENGTH_SHORT).show();
        }));
        menu.addView(menuRow(R.drawable.ic_private, "Privat", v -> {
            dialog.dismiss();
            Toast.makeText(this, "Mode privat placeholder", Toast.LENGTH_SHORT).show();
        }));
        menu.addView(menuRow(R.drawable.ic_shield, "Ad Block", v -> {
            dialog.dismiss();
            Toast.makeText(this, "Ad Block placeholder", Toast.LENGTH_SHORT).show();
        }));
        menu.addView(menuRow(R.drawable.ic_settings, "Setelan", v -> {
            dialog.dismiss();
            Toast.makeText(this, "Setelan placeholder", Toast.LENGTH_SHORT).show();
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
            lp.width = dp(280);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.x = dp(12);
            lp.y = dp(76);
            window.setAttributes(lp);
        }
        dialog.show();
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

    private ImageButton smallTopIcon(int iconRes, String desc, View.OnClickListener listener) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconRes);
        button.setColorFilter(Color.parseColor("#E9EDF5"));
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(dp(6), dp(6), dp(6), dp(6));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setContentDescription(desc);
        button.setOnClickListener(listener);
        return button;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setDatabaseEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                addressBar.setText(url);
                progressBar.setVisibility(View.GONE);
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
        webView.loadUrl(url);
    }

    private void showHome() {
        homeScroll.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
        } else if (webView.getVisibility() == View.VISIBLE) {
            showHome();
        } else {
            super.onBackPressed();
        }
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
