package com.yieldbrowser.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int COLOR_PRIMARY = Color.rgb(11, 58, 120);
    private static final int COLOR_PRIMARY_DARK = Color.rgb(7, 40, 84);
    private static final int COLOR_SURFACE = Color.rgb(246, 248, 251);
    private static final int COLOR_NAV_SURFACE = Color.WHITE;
    private static final int COLOR_ICON_DARK = Color.rgb(11, 58, 120);

    private WebView webView;
    private EditText addressBar;
    private ProgressBar progressBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(COLOR_PRIMARY_DARK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_SURFACE);

        root.addView(createSearchToolbar(), new LinearLayout.LayoutParams(-1, dp(58)));
        root.addView(createNavigationToolbar(), new LinearLayout.LayoutParams(-1, dp(48)));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(3)));

        webView = new WebView(this);
        configureWebView();
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        loadUrlFromAddressBar();
    }

    private LinearLayout createSearchToolbar() {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(10), dp(7), dp(10), dp(7));
        toolbar.setBackgroundColor(COLOR_PRIMARY);

        TextView brand = new TextView(this);
        brand.setText("Yield");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(18);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(brand, new LinearLayout.LayoutParams(dp(62), -1));

        addressBar = new EditText(this);
        addressBar.setSingleLine(true);
        addressBar.setTextSize(15);
        addressBar.setTextColor(Color.rgb(28, 32, 36));
        addressBar.setHintTextColor(Color.rgb(120, 128, 138));
        addressBar.setHint("Cari atau masukkan alamat web");
        addressBar.setText("https://www.google.com");
        addressBar.setSelectAllOnFocus(true);
        addressBar.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setPadding(dp(14), 0, dp(14), 0);
        addressBar.setBackground(roundRect(Color.WHITE, dp(22), 0, Color.TRANSPARENT));
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            boolean isEnter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP;
            if (actionId == EditorInfo.IME_ACTION_GO || isEnter) {
                loadUrlFromAddressBar();
                return true;
            }
            return false;
        });

        LinearLayout.LayoutParams addressParams = new LinearLayout.LayoutParams(0, dp(44), 1);
        addressParams.setMargins(dp(6), 0, dp(8), 0);
        toolbar.addView(addressBar, addressParams);

        ImageButton goButton = iconButton(R.drawable.ic_go, Color.WHITE, "Go", v -> loadUrlFromAddressBar());
        toolbar.addView(goButton, new LinearLayout.LayoutParams(dp(44), dp(44)));
        return toolbar;
    }

    private LinearLayout createNavigationToolbar() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(3), dp(8), dp(3));
        nav.setBackgroundColor(COLOR_NAV_SURFACE);

        nav.addView(navIcon(R.drawable.ic_back, "Back", v -> {
            if (webView != null && webView.canGoBack()) webView.goBack();
        }));

        nav.addView(navIcon(R.drawable.ic_forward, "Forward", v -> {
            if (webView != null && webView.canGoForward()) webView.goForward();
        }));

        nav.addView(navIcon(R.drawable.ic_refresh, "Refresh", v -> {
            if (webView != null) webView.reload();
        }));

        nav.addView(navIcon(R.drawable.ic_download, "Download", v -> {
            String currentUrl = webView != null ? webView.getUrl() : "";
            Toast.makeText(this, "Download manager siap ditambahkan setelah UI fix", Toast.LENGTH_SHORT).show();
        }));

        nav.addView(navIcon(R.drawable.ic_home, "Home", v -> {
            addressBar.setText("https://www.google.com");
            loadUrlFromAddressBar();
        }));

        return nav;
    }

    private View navIcon(int drawableRes, String description, View.OnClickListener listener) {
        ImageButton button = iconButton(drawableRes, COLOR_ICON_DARK, description, listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        button.setLayoutParams(params);
        return button;
    }

    private ImageButton iconButton(int drawableRes, int tintColor, String description, View.OnClickListener listener) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(drawableRes);
        button.setColorFilter(tintColor);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(dp(9), dp(9), dp(9), dp(9));
        button.setContentDescription(description);
        button.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        button.setOnClickListener(listener);
        return button;
    }

    private GradientDrawable roundRect(int fillColor, int radius, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

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

    private void loadUrlFromAddressBar() {
        String text = addressBar.getText().toString().trim();
        if (text.length() == 0) return;

        String url;
        if (text.startsWith("http://") || text.startsWith("https://")) {
            url = text;
        } else if (text.contains(".") && !text.contains(" ")) {
            url = "https://" + text;
        } else {
            url = "https://www.google.com/search?q=" + text.replace(" ", "+");
        }

        if (webView != null) webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
