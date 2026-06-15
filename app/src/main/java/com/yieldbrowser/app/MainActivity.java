package com.yieldbrowser.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;

public class MainActivity extends Activity {
    private WebView webView;
    private EditText addressBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(246,248,251));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(8), dp(8), dp(8));
        toolbar.setBackgroundColor(Color.rgb(11,58,120));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(64)));

        TextView logo = new TextView(this);
        logo.setText("Yield");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(18);
        logo.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(logo, new LinearLayout.LayoutParams(dp(60), -1));

        toolbar.addView(iconButton(R.drawable.ic_back, v -> { if (webView.canGoBack()) webView.goBack(); }), new LinearLayout.LayoutParams(dp(42), dp(42)));
        toolbar.addView(iconButton(R.drawable.ic_forward, v -> { if (webView.canGoForward()) webView.goForward(); }), new LinearLayout.LayoutParams(dp(42), dp(42)));
        toolbar.addView(iconButton(R.drawable.ic_refresh, v -> webView.reload()), new LinearLayout.LayoutParams(dp(42), dp(42)));

        addressBar = new EditText(this);
        addressBar.setSingleLine(true);
        addressBar.setTextSize(14);
        addressBar.setHint("Search or enter URL");
        addressBar.setText("https://www.google.com");
        addressBar.setPadding(dp(12), 0, dp(12), 0);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(Color.WHITE);
        barBg.setCornerRadius(dp(18));
        addressBar.setBackground(barBg);
        toolbar.addView(addressBar, new LinearLayout.LayoutParams(0, dp(42), 1));

        toolbar.addView(iconButton(R.drawable.ic_go, v -> loadFromBar()), new LinearLayout.LayoutParams(dp(42), dp(42)));
        toolbar.addView(iconButton(R.drawable.ic_download, v -> Toast.makeText(this, "Download manager coming next", Toast.LENGTH_SHORT).show()), new LinearLayout.LayoutParams(dp(42), dp(42)));
        toolbar.addView(iconButton(R.drawable.ic_home, v -> { addressBar.setText("https://www.google.com"); loadFromBar(); }), new LinearLayout.LayoutParams(dp(42), dp(42)));

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient(){ @Override public void onPageFinished(WebView view, String url){ addressBar.setText(url); }});
        webView.setWebChromeClient(new WebChromeClient());
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        loadFromBar();
    }

    private ImageButton iconButton(int res, View.OnClickListener listener) {
        ImageButton b = new ImageButton(this);
        b.setImageResource(res);
        b.setColorFilter(Color.WHITE);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(dp(9), dp(9), dp(9), dp(9));
        b.setOnClickListener(listener);
        return b;
    }

    private void loadFromBar() {
        String text = addressBar.getText().toString().trim();
        if (text.length() == 0) return;
        String url;
        if (text.startsWith("http://") || text.startsWith("https://")) url = text;
        else if (text.contains(".") && !text.contains(" ")) url = "https://" + text;
        else url = "https://www.google.com/search?q=" + text.replace(" ", "+");
        webView.loadUrl(url);
    }

    @Override public void onBackPressed() { if (webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
