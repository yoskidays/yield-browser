package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_BORDER;
import static com.yieldbrowser.app.BrowserConstants.COLOR_SUBTEXT;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Builds the lightweight video-optimization settings dialog from immutable state. */
final class VideoOptimizationDialogController {
    enum Action {
        BUFFER_BOOSTER,
        HLS_PREFETCH,
        MINIMIZE_NORMAL,
        BACKGROUND_PLAY
    }

    interface ActionHandler {
        void handle(Action action);
    }

    static final class State {
        final boolean bufferBooster;
        final boolean hlsPrefetch;
        final boolean floatingPlayer;
        final boolean backgroundPlay;

        State(boolean bufferBooster,
              boolean hlsPrefetch,
              boolean floatingPlayer,
              boolean backgroundPlay) {
            this.bufferBooster = bufferBooster;
            this.hlsPrefetch = hlsPrefetch;
            this.floatingPlayer = floatingPlayer;
            this.backgroundPlay = backgroundPlay;
        }
    }

    private final Activity activity;

    VideoOptimizationDialogController(Activity activity) {
        this.activity = activity;
    }

    void show(State state, ActionHandler handler) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setBackground(YieldUi.roundRect(
                Color.parseColor("#26292F"), dp(24), dp(1), COLOR_BORDER));

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(14));
        scroll.addView(box, new ScrollView.LayoutParams(-1, -2));

        TextView title = new TextView(activity);
        title.setText("Optimasi Video");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
        box.addView(title);

        TextView info = new TextView(activity);
        info.setText("Fitur ringan untuk streaming. Yang sudah otomatis tidak digandakan agar tidak crash.");
        info.setTextColor(COLOR_SUBTEXT);
        info.setTextSize(13);
        info.setLineSpacing(0, 1.05f);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(-1, -2);
        infoParams.setMargins(0, dp(8), 0, dp(12));
        box.addView(info, infoParams);

        box.addView(toggle(
                "Video preload / buffer booster",
                "Memaksa preload auto dan cache lebih agresif saat ada video.",
                state.bufferBooster,
                Action.BUFFER_BOOSTER,
                handler));
        box.addView(toggle(
                "HLS segment prefetch",
                "Mendeteksi m3u8 dan mencoba prefetch ringan segmen berikutnya.",
                state.hlsPrefetch,
                Action.HLS_PREFETCH,
                handler));
        box.addView(toggle(
                "Minimize normal / tanpa floating",
                "Saat tombol Home/Recent Android ditekan, Yield tidak jadi jendela melayang dan akan balik ke tampilan terakhir.",
                !state.floatingPlayer,
                Action.MINIMIZE_NORMAL,
                handler));
        box.addView(toggle(
                "Background play ringan",
                "Video tidak dipaksa pause saat aplikasi masuk background jika WebView mendukung.",
                state.backgroundPlay,
                Action.BACKGROUND_PLAY,
                handler));

        box.addView(SettingsUi.videoOptInfoRow(
                activity,
                "Auto detect kualitas video",
                "Sudah aktif di tombol kualitas 240p–720p; tidak dibuat dobel."));
        box.addView(SettingsUi.videoOptInfoRow(
                activity,
                "Download kualitas 240p–720p",
                "Diperkuat via deteksi source/player. Situs yang menyembunyikan URL tetap mengikuti batas player."));

        LinearLayout bottom = new LinearLayout(activity);
        bottom.setGravity(Gravity.END);
        bottom.setPadding(0, dp(8), 0, 0);
        TextView close = new TextView(activity);
        close.setText("TUTUP");
        close.setTextColor(Color.parseColor("#77A7FF"));
        close.setTextSize(14);
        close.setTypeface(Typeface.DEFAULT_BOLD);
        close.setGravity(Gravity.CENTER);
        close.setPadding(dp(16), dp(10), dp(16), dp(10));
        close.setOnClickListener(view -> dialog.dismiss());
        bottom.addView(close);
        box.addView(bottom);

        dialog.setContentView(scroll);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9f);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
    }

    static boolean displayedMinimizeNormal(State state) {
        return state != null && !state.floatingPlayer;
    }

    static boolean toggledFloatingPlayer(boolean currentFloatingPlayer) {
        return !currentFloatingPlayer;
    }

    private View toggle(String title,
                        String description,
                        boolean enabled,
                        Action action,
                        ActionHandler handler) {
        return SettingsUi.videoOptSwitchRow(
                activity,
                title,
                description,
                enabled,
                view -> {
                    if (handler != null) handler.handle(action);
                });
    }

    private int dp(int value) {
        return YieldUi.dp(activity, value);
    }
}
