package com.yieldbrowser.app;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Internal player for progressive playback while a Yield download continues in the background. */
public final class ProgressiveVideoActivity extends Activity {
    static final String EXTRA_MEDIA_URL = "yield_media_url";
    static final String EXTRA_STATUS_URL = "yield_status_url";
    static final String EXTRA_CLOSE_URL = "yield_close_url";
    static final String EXTRA_TITLE = "yield_video_title";
    static final String EXTRA_PRIVATE_SESSION = "yield_private_video_session";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private VideoView videoView;
    private ProgressBar loading;
    private ProgressBar downloadProgress;
    private TextView loadingText;
    private TextView statusText;
    private TextView retryButton;
    private View topBar;
    private View bottomBar;
    // v0.9.92: kontrol video kustom (MediaController bawaan sering tidak muncul di FrameLayout).
    private View controlsBar;
    private TextView playPauseBtn;
    private SeekBar seekBar;
    private TextView currentTimeText;
    private TextView durationText;
    private boolean userSeeking;
    private boolean controlsVisible;
    private int lastProgressPercent;

    private String mediaUrl = "";
    private String statusUrl = "";
    private String closeUrl = "";
    private boolean preparing;
    private boolean prepared;
    private boolean destroyed;
    private boolean statusTerminal;
    private int playbackErrors;
    private int resumePositionMs;
    private volatile boolean statusFetchInFlight;

    private final Runnable statusTicker = new Runnable() {
        @Override
        public void run() {
            if (destroyed) return;
            fetchStatus();
            handler.postDelayed(this, 1000L);
        }
    };

    private final Runnable controlsTick = new Runnable() {
        @Override
        public void run() {
            if (destroyed) return;
            if (prepared && controlsVisible) updateSeekUi();
            handler.postDelayed(this, 500L);
        }
    };

    private final Runnable hideControlsRunnable = () -> setControlsVisible(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (getIntent().getBooleanExtra(EXTRA_PRIVATE_SESSION, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        mediaUrl = safe(getIntent().getStringExtra(EXTRA_MEDIA_URL));
        statusUrl = safe(getIntent().getStringExtra(EXTRA_STATUS_URL));
        closeUrl = safe(getIntent().getStringExtra(EXTRA_CLOSE_URL));
        String title = safe(getIntent().getStringExtra(EXTRA_TITLE));
        if (title.isEmpty()) title = "Video Yield";

        buildUi(title);
        if (mediaUrl.isEmpty() || statusUrl.isEmpty()) {
            showFatal("Sesi video tidak tersedia");
            return;
        }
        handler.post(statusTicker);
        handler.post(controlsTick);
    }

    private void buildUi(String title) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), 0, dp(8), 0);
        toolbar.setBackgroundColor(Color.parseColor("#111318"));
        topBar = toolbar;

        TextView back = actionText("‹", 34);
        back.setContentDescription("Kembali");
        back.setOnClickListener(v -> finish());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(54)));

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        toolbar.addView(titleView, new LinearLayout.LayoutParams(0, dp(54), 1));

        TextView pip = actionText("▣", 21);
        pip.setContentDescription("Picture in Picture");
        pip.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        pip.setOnClickListener(v -> enterPip());
        toolbar.addView(pip, new LinearLayout.LayoutParams(dp(48), dp(54)));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(54)));

        FrameLayout playerFrame = new FrameLayout(this);
        playerFrame.setBackgroundColor(Color.BLACK);
        videoView = new VideoView(this);
        videoView.setBackgroundColor(Color.BLACK);
        playerFrame.addView(videoView, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        View tapCatcher = new View(this);
        tapCatcher.setBackgroundColor(Color.TRANSPARENT);
        tapCatcher.setOnClickListener(v -> toggleControls());
        playerFrame.addView(tapCatcher, new FrameLayout.LayoutParams(-1, -1));

        controlsBar = buildControlsBar();
        controlsBar.setVisibility(View.GONE);
        playerFrame.addView(controlsBar, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        LinearLayout waiting = new LinearLayout(this);
        waiting.setOrientation(LinearLayout.VERTICAL);
        waiting.setGravity(Gravity.CENTER);
        waiting.setPadding(dp(24), dp(24), dp(24), dp(24));
        loading = new ProgressBar(this);
        waiting.addView(loading, new LinearLayout.LayoutParams(dp(42), dp(42)));
        loadingText = new TextView(this);
        loadingText.setText("Menyiapkan video…");
        loadingText.setTextColor(Color.parseColor("#E8EBF0"));
        loadingText.setTextSize(14);
        loadingText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams loadingTextLp = new LinearLayout.LayoutParams(-1, -2);
        loadingTextLp.setMargins(0, dp(14), 0, 0);
        waiting.addView(loadingText, loadingTextLp);

        retryButton = new TextView(this);
        retryButton.setText("COBA LAGI");
        retryButton.setTextColor(Color.BLACK);
        retryButton.setTextSize(13);
        retryButton.setTypeface(Typeface.DEFAULT_BOLD);
        retryButton.setGravity(Gravity.CENTER);
        retryButton.setBackground(roundRect(
                Color.parseColor("#F4C542"), dp(18), 0, Color.TRANSPARENT));
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(v -> {
            playbackErrors = 0;
            retryButton.setVisibility(View.GONE);
            startPlayback(true);
        });
        LinearLayout.LayoutParams retryLp = new LinearLayout.LayoutParams(dp(150), dp(42));
        retryLp.setMargins(0, dp(18), 0, 0);
        waiting.addView(retryButton, retryLp);
        playerFrame.addView(waiting, new FrameLayout.LayoutParams(-1, -1));
        loadingText.setTag(waiting);

        root.addView(playerFrame, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout statusPanel = new LinearLayout(this);
        statusPanel.setOrientation(LinearLayout.VERTICAL);
        statusPanel.setPadding(dp(16), dp(10), dp(16), dp(12));
        statusPanel.setBackgroundColor(Color.parseColor("#111318"));
        bottomBar = statusPanel;

        statusText = new TextView(this);
        statusText.setText("Download tetap berjalan di latar belakang");
        statusText.setTextColor(Color.parseColor("#D7DCE4"));
        statusText.setTextSize(12);
        statusPanel.addView(statusText, new LinearLayout.LayoutParams(-1, -2));

        downloadProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        downloadProgress.setMax(100);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(-1, dp(5));
        progressLp.setMargins(0, dp(8), 0, 0);
        statusPanel.addView(downloadProgress, progressLp);
        root.addView(statusPanel, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);

        videoView.setOnPreparedListener(this::onPrepared);
        videoView.setOnCompletionListener(mp -> {
            statusText.setText("Pemutaran selesai");
            updatePlayPauseGlyph();
            setControlsVisible(true);
        });
        videoView.setOnErrorListener((mp, what, extra) -> onPlaybackError());
    }

    private void onPrepared(MediaPlayer player) {
        preparing = false;
        prepared = true;
        playbackErrors = 0;
        if (resumePositionMs > 0) videoView.seekTo(resumePositionMs);
        hideWaiting();
        videoView.start();
        updatePlayPauseGlyph();
        setControlsVisible(true);
    }

    private boolean onPlaybackError() {
        preparing = false;
        prepared = false;
        playbackErrors++;
        try {
            resumePositionMs = Math.max(0, videoView.getCurrentPosition());
            videoView.stopPlayback();
        } catch (Exception ignored) {
        }
        showWaiting(statusTerminal
                ? "Format video belum dapat diputar oleh perangkat ini"
                : "Menunggu bagian video berikutnya…");
        if (!statusTerminal && playbackErrors <= 4) {
            handler.postDelayed(() -> startPlayback(true), 1800L);
        } else {
            loading.setVisibility(View.GONE);
            retryButton.setVisibility(View.VISIBLE);
        }
        return true;
    }

    private void startPlayback(boolean force) {
        if (destroyed || preparing || (prepared && !force)) return;
        preparing = true;
        prepared = false;
        retryButton.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
        showWaiting("Menyiapkan video dari data yang tersedia…");
        try {
            if (force) videoView.stopPlayback();
            videoView.setVideoURI(Uri.parse(mediaUrl));
            videoView.requestFocus();
        } catch (Exception e) {
            preparing = false;
            showFatal("Player tidak dapat membuka video");
        }
    }

    private void fetchStatus() {
        if (statusFetchInFlight) return;
        statusFetchInFlight = true;
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(statusUrl).openConnection();
                connection.setConnectTimeout(2200);
                connection.setReadTimeout(2200);
                connection.setUseCaches(false);
                int code = connection.getResponseCode();
                if (code != 200) return;
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                }
                JSONObject json = new JSONObject(body.toString());
                String state = json.optString("status", "");
                int progress = json.optInt("progress", 0);
                long speed = json.optLong("speedBytesPerSecond", 0L);
                boolean playable = json.optBoolean("playable", false);
                statusTerminal = "completed".equals(state) || "failed".equals(state)
                        || "removed".equals(state);
                runOnUiThread(() -> applyStatus(state, progress, speed, playable));
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
                statusFetchInFlight = false;
            }
        }, "yield-player-status").start();
    }

    private void applyStatus(String state, int progress, long speed, boolean playable) {
        if (destroyed) return;
        lastProgressPercent = Math.max(0, Math.min(100, progress));
        downloadProgress.setProgress(Math.max(0, Math.min(100, progress)));
        String text;
        if ("running".equals(state)) {
            text = "Mengunduh " + progress + "%";
            if (speed > 0L) text += " • " + readableSpeed(speed);
        } else if ("paused".equals(state)) {
            text = "Download dijeda • video menggunakan data yang sudah tersedia";
        } else if ("queued".equals(state)) {
            text = "Menunggu antrean download";
        } else if ("verifying".equals(state)) {
            text = "Download selesai • memverifikasi file";
        } else if ("saving".equals(state)) {
            text = "Menyimpan video ke folder Downloads";
        } else if ("completed".equals(state)) {
            text = "Download selesai • video tersimpan";
            downloadProgress.setProgress(100);
        } else if ("failed".equals(state)) {
            text = "Download gagal • bagian yang sudah tersedia tetap dapat diputar";
        } else {
            text = "Download tetap berjalan di latar belakang";
        }
        statusText.setText(text);
        if (playable && !preparing && !prepared) startPlayback(false);
        if (!playable && !prepared && !preparing) {
            showWaiting("Menunggu data awal video… " + progress + "%");
        }
    }

    private void showWaiting(String text) {
        View waiting = loadingText == null ? null : (View) loadingText.getTag();
        if (waiting != null) waiting.setVisibility(View.VISIBLE);
        if (loadingText != null) loadingText.setText(text);
    }

    private void hideWaiting() {
        View waiting = loadingText == null ? null : (View) loadingText.getTag();
        if (waiting != null) waiting.setVisibility(View.GONE);
    }

    private void showFatal(String message) {
        showWaiting(message);
        if (loading != null) loading.setVisibility(View.GONE);
        if (retryButton != null) retryButton.setVisibility(View.VISIBLE);
    }

    private void enterPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || isInPictureInPictureMode()) return;
        try {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9))
                    .build();
            enterPictureInPictureMode(params);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                              android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (topBar != null) topBar.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
        if (bottomBar != null) bottomBar.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
        if (isInPictureInPictureMode) setControlsVisible(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) return;
        try {
            if (videoView != null && videoView.isPlaying()) {
                resumePositionMs = videoView.getCurrentPosition();
                videoView.pause();
                updatePlayPauseGlyph();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (prepared && videoView != null && !videoView.isPlaying()) {
                videoView.start();
                updatePlayPauseGlyph();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        try {
            if (videoView != null) videoView.stopPlayback();
        } catch (Exception ignored) {
        }
        notifySessionClosed();
        super.onDestroy();
    }

    private void notifySessionClosed() {
        if (closeUrl.isEmpty()) return;
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(closeUrl).openConnection();
                connection.setConnectTimeout(1200);
                connection.setReadTimeout(1200);
                connection.getResponseCode();
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "yield-player-close").start();
    }

    private View buildControlsBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(6), dp(10), dp(6));
        bar.setBackgroundColor(Color.parseColor("#CC000000"));

        playPauseBtn = new TextView(this);
        playPauseBtn.setText("\u275A\u275A"); // pause glyph
        playPauseBtn.setTextColor(Color.WHITE);
        playPauseBtn.setTextSize(18);
        playPauseBtn.setGravity(Gravity.CENTER);
        playPauseBtn.setOnClickListener(v -> togglePlayPause());
        bar.addView(playPauseBtn, new LinearLayout.LayoutParams(dp(42), dp(42)));

        currentTimeText = new TextView(this);
        currentTimeText.setText("0:00");
        currentTimeText.setTextColor(Color.parseColor("#E8EBF0"));
        currentTimeText.setTextSize(12);
        LinearLayout.LayoutParams ctLp = new LinearLayout.LayoutParams(-2, -2);
        ctLp.setMargins(dp(6), 0, dp(6), 0);
        bar.addView(currentTimeText, ctLp);

        seekBar = new SeekBar(this);
        seekBar.setMax(1000);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    long dur = safeDuration();
                    if (dur > 0) currentTimeText.setText(formatTime(dur * progress / 1000L));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                userSeeking = true;
                handler.removeCallbacks(hideControlsRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                userSeeking = false;
                seekToProgress(sb.getProgress());
                scheduleHideControls();
            }
        });
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(0, -2, 1f);
        bar.addView(seekBar, sbLp);

        durationText = new TextView(this);
        durationText.setText("0:00");
        durationText.setTextColor(Color.parseColor("#E8EBF0"));
        durationText.setTextSize(12);
        LinearLayout.LayoutParams dtLp = new LinearLayout.LayoutParams(-2, -2);
        dtLp.setMargins(dp(6), 0, dp(6), 0);
        bar.addView(durationText, dtLp);

        return bar;
    }

    private void toggleControls() {
        setControlsVisible(!controlsVisible);
    }

    private void setControlsVisible(boolean visible) {
        controlsVisible = visible;
        if (controlsBar != null) controlsBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (topBar != null) topBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        handler.removeCallbacks(hideControlsRunnable);
        if (visible) {
            updateSeekUi();
            updatePlayPauseGlyph();
            scheduleHideControls();
        }
    }

    private void scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable);
        boolean playing;
        try {
            playing = videoView != null && videoView.isPlaying();
        } catch (Exception e) {
            playing = false;
        }
        if (playing && !userSeeking) handler.postDelayed(hideControlsRunnable, 3500L);
    }

    private void togglePlayPause() {
        if (videoView == null) return;
        try {
            if (videoView.isPlaying()) videoView.pause();
            else videoView.start();
        } catch (Exception ignored) {
        }
        updatePlayPauseGlyph();
        setControlsVisible(true);
    }

    private void updatePlayPauseGlyph() {
        if (playPauseBtn == null) return;
        boolean playing;
        try {
            playing = videoView != null && videoView.isPlaying();
        } catch (Exception e) {
            playing = false;
        }
        playPauseBtn.setText(playing ? "\u275A\u275A" : "\u25B6");
        if (playing) scheduleHideControls();
        else handler.removeCallbacks(hideControlsRunnable);
    }

    private void updateSeekUi() {
        if (seekBar == null || videoView == null) return;
        long dur = safeDuration();
        long pos;
        try {
            pos = Math.max(0, videoView.getCurrentPosition());
        } catch (Exception e) {
            pos = 0;
        }
        if (dur > 0) {
            if (!userSeeking) seekBar.setProgress((int) (pos * 1000L / dur));
            durationText.setText(formatTime(dur));
            if (lastProgressPercent > 0) seekBar.setSecondaryProgress(lastProgressPercent * 10);
        }
        if (!userSeeking) currentTimeText.setText(formatTime(pos));
    }

    private long safeDuration() {
        try {
            int d = videoView != null ? videoView.getDuration() : 0;
            return Math.max(0, d);
        } catch (Exception e) {
            return 0;
        }
    }

    private void seekToProgress(int progress) {
        long dur = safeDuration();
        if (dur <= 0 || videoView == null) return;
        long target = dur * progress / 1000L;
        // Batasi seek hingga bagian yang sudah terunduh agar tidak melompat ke "lubang" data.
        if (lastProgressPercent > 0 && lastProgressPercent < 100) {
            long cap = dur * lastProgressPercent / 100L;
            if (target > cap) target = cap;
        }
        try {
            videoView.seekTo((int) target);
        } catch (Exception ignored) {
        }
    }

    private static String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long totalSec = ms / 1000L;
        long h = totalSec / 3600L;
        long m = (totalSec % 3600L) / 60L;
        long s = totalSec % 60L;
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        return String.format(Locale.US, "%d:%02d", m, s);
    }

    private TextView actionText(String text, int sizeSp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(sizeSp);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(Color.TRANSPARENT);
        return view;
    }

    private static GradientDrawable roundRect(int fillColor, int radius, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String readableSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024L) return bytesPerSecond + " B/s";
        if (bytesPerSecond < 1024L * 1024L) {
            return String.format(Locale.US, "%.1f KB/s", bytesPerSecond / 1024.0);
        }
        return String.format(Locale.US, "%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
