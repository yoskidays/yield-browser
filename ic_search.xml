package com.yieldbrowser.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#050609"));
        window.setNavigationBarColor(Color.parseColor("#050609"));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#050609"));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(dp(24), dp(24), dp(24), dp(24));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.yield_logo_splash);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setAlpha(0f);
        logo.setScaleX(0.72f);
        logo.setScaleY(0.72f);
        logo.setRotation(-4f);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(188), dp(188));
        content.addView(logo, logoLp);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER);
        titleRow.setAlpha(0f);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-2, -2);
        titleLp.setMargins(0, dp(16), 0, 0);

        TextView yield = new TextView(this);
        yield.setText("Yield");
        yield.setTextColor(Color.parseColor("#F5F7FA"));
        yield.setTextSize(26);
        yield.setTypeface(Typeface.DEFAULT_BOLD);
        yield.setIncludeFontPadding(false);
        titleRow.addView(yield);

        TextView browser = new TextView(this);
        browser.setText(" Browser");
        browser.setTextColor(Color.parseColor("#DDA13A"));
        browser.setTextSize(26);
        browser.setTypeface(Typeface.DEFAULT_BOLD);
        browser.setIncludeFontPadding(false);
        titleRow.addView(browser);

        content.addView(titleRow, titleLp);

        View line = new View(this);
        line.setBackgroundResource(R.drawable.splash_glow_line);
        line.setAlpha(0f);
        line.setScaleX(0.15f);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(dp(96), dp(3));
        lineLp.setMargins(0, dp(13), 0, 0);
        content.addView(line, lineLp);

        TextView tagline = new TextView(this);
        tagline.setText("Fast • Safe • Smooth");
        tagline.setTextColor(Color.parseColor("#9EA6B4"));
        tagline.setTextSize(12);
        tagline.setLetterSpacing(0.18f);
        tagline.setAlpha(0f);
        tagline.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(-2, -2);
        tagLp.setMargins(0, dp(16), 0, 0);
        content.addView(tagline, tagLp);

        root.addView(content, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        setContentView(root);

        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0f)
                .setDuration(620)
                .setStartDelay(80)
                .start();

        titleRow.animate()
                .alpha(1f)
                .translationY(-dp(4))
                .setDuration(420)
                .setStartDelay(520)
                .start();

        line.animate()
                .alpha(1f)
                .scaleX(1f)
                .setDuration(430)
                .setStartDelay(760)
                .start();

        tagline.animate()
                .alpha(1f)
                .setDuration(360)
                .setStartDelay(900)
                .start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.setAction(getIntent() != null ? getIntent().getAction() : null);
            if (getIntent() != null && getIntent().getExtras() != null) {
                intent.putExtras(getIntent().getExtras());
            }
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1450);
    }
}
