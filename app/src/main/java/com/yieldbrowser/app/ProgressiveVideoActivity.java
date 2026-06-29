package com.yieldbrowser.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Rational;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Internal player for progressive playback while a Yield download continues.
 *
 * v0.10.15 uses Media3 ExoPlayer with automatic SurfaceView/TextureView fallback
 * and a complete responsive video control panel mounted outside the SurfaceView layer.
 * This guarantees that the full controls remain visible on Realme Android 11, including
 * portrait mode, where SurfaceView can cover normal overlay children.
 * Realme devices on Android 11 start with SurfaceView and automatically retry with
 * TextureView if playback time advances without a visible frame.
 */
public final class ProgressiveVideoActivity extends Activity {
    static final String EXTRA_MEDIA_URL = "yield_media_url";
    static final String EXTRA_STATUS_URL = "yield_status_url";
    static final String EXTRA_CLOSE_URL = "yield_close_url";
    static final String EXTRA_TITLE = "yield_video_title";
    static final String EXTRA_PRIVATE_SESSION = "yield_private_video_session";
    static final String EXTRA_ORIGIN_URL = "yield_origin_url";
    static final String EXTRA_ORIGIN_USER_AGENT = "yield_origin_user_agent";
    static final String EXTRA_ORIGIN_REFERER = "yield_origin_referer";
    static final String EXTRA_ORIGIN_COOKIE = "yield_origin_cookie";

    private static final long LOCAL_PREPARE_TIMEOUT_MS = 15_000L;
    private static final long ORIGIN_PREPARE_TIMEOUT_MS = 22_000L;
    private static final long FIRST_FRAME_TIMEOUT_MS = 8_000L;
    private static final long CONTROLS_HIDE_DELAY_MS = 6_000L;
    private static final long TAP_MAX_DURATION_MS = 550L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private FrameLayout playerFrame;
    private View videoOutputView;
    private TextureView textureView;
    private SurfaceView surfaceView;
    private ExoPlayer player;
    private ProgressBar loading;
    private ProgressBar downloadProgress;
    private TextView loadingText;
    private TextView statusText;
    private TextView retryButton;
    private View topBar;
    private View bottomBar;
    private View controlsBar;
    private TextView rewindButton;
    private TextView playPauseBtn;
    private TextView forwardButton;
    private SeekBar seekBar;
    private TextView currentTimeText;
    private TextView durationText;
    private TextView speedButton;
    private TextView qualityButton;
    private TextView fullscreenButton;
    private LinearLayout controlActionRowsHost;

    private boolean userSeeking;
    private boolean realmeAndroid11;
    private boolean alternateOutputTried;
    private int videoOutputMode = VideoOutputCompatibilityPolicy.OUTPUT_TEXTURE;
    private boolean controlsVisible;
    private int lastProgressPercent;
    private int controlTouchSlop;
    private float playbackSpeed = 1f;
    private boolean fullscreenMode;

    private String mediaUrl = "";
    private String statusUrl = "";
    private String closeUrl = "";
    private String originUrl = "";
    private String originUserAgent = "";
    private String originReferer = "";
    private String originCookie = "";

    private boolean usingOrigin;
    private boolean originAvailable;
    private boolean firstFrameSeen;
    private boolean preparing;
    private boolean prepared;
    private boolean destroyed;
    private boolean statusTerminal;
    private boolean manualRetryRequired;
    private int playbackErrors;
    private long resumePositionMs;
    private volatile boolean statusFetchInFlight;

    private int videoWidth;
    private int videoHeight;
    private float videoPixelRatio = 1f;

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
            if ((prepared || firstFrameSeen) && controlsVisible) updateSeekUi();
            handler.postDelayed(this, 500L);
        }
    };

    private final Runnable hideControlsRunnable = () -> setControlsVisible(false);

    private final Runnable preparationWatchdog = new Runnable() {
        @Override
        public void run() {
            if (destroyed || !preparing) return;
            if (retryWithAlternateVideoOutput("Renderer video pertama terlalu lama menunggu")) {
                return;
            }
            if (!usingOrigin && !originUrl.isEmpty()) {
                switchToOrigin("Player lokal terlalu lama menunggu data");
            } else {
                preparing = false;
                showFatal("Video belum dapat disiapkan. Coba lagi atau buka dengan pemutar lain.");
            }
        }
    };

    private final Runnable firstFrameWatchdog = new Runnable() {
        @Override
        public void run() {
            if (destroyed || !prepared || firstFrameSeen) return;

            long position = safePosition();
            if (retryWithAlternateVideoOutput(position > 500L
                    ? "Audio berjalan tetapi gambar belum tampil"
                    : "Frame video pertama belum tampil")) {
                return;
            }
            if (!usingOrigin && !originUrl.isEmpty()) {
                switchToOrigin(position > 500L
                        ? "Audio berjalan tetapi gambar lokal tidak tampil"
                        : "Frame video lokal belum tampil");
                return;
            }

            pausePlayerQuietly();
            showFatal(position > 500L
                    ? "Suara atau waktu video berjalan, tetapi gambar tidak dapat dirender oleh decoder internal."
                    : "Frame video tidak berhasil tampil pada perangkat ini.");
        }
    };

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (destroyed) return;

            if (playbackState == Player.STATE_BUFFERING) {
                if (!firstFrameSeen) {
                    showWaiting(usingOrigin
                            ? "Memuat streaming cadangan…"
                            : "Membaca data video yang tersedia…");
                }
                return;
            }

            if (playbackState == Player.STATE_READY) {
                handler.removeCallbacks(preparationWatchdog);
                preparing = false;
                prepared = true;
                manualRetryRequired = false;
                playbackErrors = 0;
                loading.setVisibility(View.VISIBLE);
                if (!firstFrameSeen) {
                    showWaiting("Menunggu frame video pertama…");
                    handler.removeCallbacks(firstFrameWatchdog);
                    handler.postDelayed(firstFrameWatchdog, FIRST_FRAME_TIMEOUT_MS);
                }
                updatePlayPauseGlyph();
                setControlsVisible(true);
                return;
            }

            if (playbackState == Player.STATE_ENDED) {
                handler.removeCallbacks(firstFrameWatchdog);
                statusText.setText("Pemutaran selesai");
                updatePlayPauseGlyph();
                setControlsVisible(true);
            }
        }

        @Override
        public void onRenderedFirstFrame() {
            if (destroyed) return;
            firstFrameSeen = true;
            prepared = true;
            preparing = false;
            manualRetryRequired = false;
            handler.removeCallbacks(preparationWatchdog);
            handler.removeCallbacks(firstFrameWatchdog);
            hideWaiting();
            updatePlayPauseGlyph();
            // Show the controls only after a real frame is visible. Previously the
            // auto-hide timer could expire while the decoder was still preparing.
            setControlsVisible(true);
        }

        @Override
        public void onVideoSizeChanged(VideoSize size) {
            videoWidth = Math.max(0, size.width);
            videoHeight = Math.max(0, size.height);
            videoPixelRatio = size.pixelWidthHeightRatio > 0f
                    ? size.pixelWidthHeightRatio : 1f;
            applyVideoAspectRatio();
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            updatePlayPauseGlyph();
            if (isPlaying && firstFrameSeen) {
                hideWaiting();
                scheduleHideControls();
            } else if (prepared || firstFrameSeen) {
                setControlsVisible(true);
            }
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            onPlaybackError(error);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        videoOutputMode = VideoOutputCompatibilityPolicy.preferredOutput(
                Build.VERSION.SDK_INT, Build.BRAND, Build.MANUFACTURER, Build.MODEL);
        realmeAndroid11 = videoOutputMode == VideoOutputCompatibilityPolicy.OUTPUT_SURFACE;
        controlTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
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
        originUrl = safe(getIntent().getStringExtra(EXTRA_ORIGIN_URL));
        originUserAgent = safe(getIntent().getStringExtra(EXTRA_ORIGIN_USER_AGENT));
        originReferer = safe(getIntent().getStringExtra(EXTRA_ORIGIN_REFERER));
        originCookie = safe(getIntent().getStringExtra(EXTRA_ORIGIN_COOKIE));
        originAvailable = !originUrl.isEmpty();

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
        pip.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? View.VISIBLE : View.GONE);
        pip.setOnClickListener(v -> enterPip());
        toolbar.addView(pip, new LinearLayout.LayoutParams(dp(48), dp(54)));
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(54)));

        playerFrame = new FrameLayout(this);
        playerFrame.setBackgroundColor(Color.BLACK);
        installControlTapTarget(playerFrame);
        playerFrame.addOnLayoutChangeListener((v, left, top, right, bottom,
                                               oldLeft, oldTop, oldRight, oldBottom) ->
                applyVideoAspectRatio());

        installVideoOutputView();

        LinearLayout waiting = new LinearLayout(this);
        waiting.setOrientation(LinearLayout.VERTICAL);
        waiting.setGravity(Gravity.CENTER);
        waiting.setPadding(dp(24), dp(24), dp(24), dp(24));
        waiting.setClickable(false);

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
            manualRetryRequired = false;
            alternateOutputTried = false;
            selectPreferredVideoOutput();
            retryButton.setVisibility(View.GONE);
            startPlayback(true);
        });
        LinearLayout.LayoutParams retryLp = new LinearLayout.LayoutParams(dp(150), dp(42));
        retryLp.setMargins(0, dp(18), 0, 0);
        waiting.addView(retryButton, retryLp);

        playerFrame.addView(waiting, new FrameLayout.LayoutParams(-1, -1));
        loadingText.setTag(waiting);
        root.addView(playerFrame, new LinearLayout.LayoutParams(-1, 0, 1));

        // Keep the complete controller outside playerFrame. SurfaceView is rendered in a
        // separate native layer on several Realme/Android 11 builds and can cover overlay
        // children even when bringToFront()/elevation are used. A sibling below the video
        // surface cannot be covered, so all six controls remain visible in portrait.
        controlsBar = buildControlsBar();
        controlsBar.setVisibility(View.GONE);
        root.addView(controlsBar, new LinearLayout.LayoutParams(-1, -2));

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

        downloadProgress = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        downloadProgress.setMax(100);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(-1, dp(5));
        progressLp.setMargins(0, dp(8), 0, 0);
        statusPanel.addView(downloadProgress, progressLp);
        root.addView(statusPanel, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    private void startPlayback(boolean force) {
        if (destroyed || preparing || (prepared && !force)) return;

        manualRetryRequired = false;
        preparing = true;
        prepared = false;
        firstFrameSeen = false;
        retryButton.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
        showWaiting(usingOrigin
                ? "Membuka streaming cadangan sambil download tetap berjalan…"
                : "Menyiapkan video dari data download yang tersedia…");

        handler.removeCallbacks(preparationWatchdog);
        handler.removeCallbacks(firstFrameWatchdog);
        releasePlayer(true);

        String source = usingOrigin ? originUrl : mediaUrl;
        if (source.isEmpty()) {
            preparing = false;
            showFatal("Sumber video tidak tersedia");
            return;
        }

        try {
            DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(15_000)
                    .setReadTimeoutMs(35_000);

            Map<String, String> headers = usingOrigin
                    ? buildOriginHeaders() : buildLocalHeaders();
            if (!headers.isEmpty()) httpFactory.setDefaultRequestProperties(headers);
            if (usingOrigin && !originUserAgent.isEmpty()) {
                httpFactory.setUserAgent(originUserAgent);
            }

            DefaultDataSource.Factory dataSourceFactory =
                    new DefaultDataSource.Factory(this, httpFactory);
            DefaultMediaSourceFactory mediaSourceFactory =
                    new DefaultMediaSourceFactory(dataSourceFactory);
            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this)
                    .setEnableDecoderFallback(true);

            player = new ExoPlayer.Builder(this, renderersFactory)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .build();
            player.addListener(playerListener);
            player.setHandleAudioBecomingNoisy(true);
            player.setPlaybackSpeed(playbackSpeed);
            attachVideoOutput(player);
            player.setMediaItem(MediaItem.fromUri(Uri.parse(source)));
            if (resumePositionMs > 0L) player.seekTo(resumePositionMs);
            player.prepare();
            player.play();

            handler.postDelayed(preparationWatchdog,
                    usingOrigin ? ORIGIN_PREPARE_TIMEOUT_MS : LOCAL_PREPARE_TIMEOUT_MS);
        } catch (Exception e) {
            preparing = false;
            releasePlayer(false);
            if (retryWithAlternateVideoOutput("Renderer video tidak dapat dibuat")) {
                return;
            }
            if (!usingOrigin && !originUrl.isEmpty()) {
                switchToOrigin("Player lokal tidak dapat dibuka");
            } else {
                showFatal("Player tidak dapat membuka video");
            }
        }
    }

    private void onPlaybackError(PlaybackException error) {
        handler.removeCallbacks(preparationWatchdog);
        handler.removeCallbacks(firstFrameWatchdog);
        resumePositionMs = Math.max(resumePositionMs, safePosition());
        preparing = false;
        prepared = false;
        firstFrameSeen = false;
        playbackErrors++;
        releasePlayer(false);

        if (retryWithAlternateVideoOutput("Decoder pertama gagal merender video")) {
            return;
        }
        if (!usingOrigin && !originUrl.isEmpty()) {
            switchToOrigin("Data lokal belum cocok dengan decoder perangkat");
            return;
        }

        String detail = error == null ? "" : safe(error.getMessage());
        String message = "Format atau decoder video belum dapat diputar di player internal";
        if (!detail.isEmpty()) message += ". " + trimError(detail);
        showFatal(message);
    }

    private void switchToOrigin(String reason) {
        if (destroyed || usingOrigin || originUrl.isEmpty()) return;
        handler.removeCallbacks(preparationWatchdog);
        handler.removeCallbacks(firstFrameWatchdog);
        resumePositionMs = Math.max(resumePositionMs, safePosition());
        releasePlayer(false);

        usingOrigin = true;
        preparing = false;
        prepared = false;
        firstFrameSeen = false;
        manualRetryRequired = false;
        alternateOutputTried = false;
        selectPreferredVideoOutput();
        statusText.setText("Mode streaming cadangan • download tetap berjalan");
        showWaiting(reason + ". Beralih ke sumber video…");
        handler.postDelayed(() -> startPlayback(true), 250L);
    }

    private Map<String, String> buildLocalHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "identity");
        return headers;
    }

    private Map<String, String> buildOriginHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "identity");
        if (!originUserAgent.isEmpty()) headers.put("User-Agent", originUserAgent);
        if (!originReferer.isEmpty()) {
            headers.put("Referer", originReferer);
            String origin = originFrom(originReferer);
            if (!origin.isEmpty()) headers.put("Origin", origin);
        }
        if (!originCookie.isEmpty()) headers.put("Cookie", originCookie);
        return headers;
    }

    private String originFrom(String value) {
        try {
            URL url = new URL(value);
            int port = url.getPort();
            boolean defaultPort = port < 0
                    || ("http".equalsIgnoreCase(url.getProtocol()) && port == 80)
                    || ("https".equalsIgnoreCase(url.getProtocol()) && port == 443);
            return url.getProtocol() + "://" + url.getHost()
                    + (defaultPort ? "" : ":" + port);
        } catch (Exception ignored) {
            return "";
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
                boolean serverOriginAvailable = json.optBoolean(
                        "originAvailable", originAvailable);
                boolean preferOrigin = json.optBoolean("preferOrigin", false);
                statusTerminal = "completed".equals(state) || "failed".equals(state)
                        || "removed".equals(state);

                runOnUiThread(() -> {
                    originAvailable = serverOriginAvailable || !originUrl.isEmpty();
                    if (preferOrigin && !usingOrigin && !originUrl.isEmpty()
                            && !preparing && !prepared && !manualRetryRequired) {
                        usingOrigin = true;
                    }
                    applyStatus(state, progress, speed, playable);
                });
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
        downloadProgress.setProgress(lastProgressPercent);

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
            text = "Download gagal • bagian yang tersedia tetap dapat diputar";
        } else {
            text = "Download tetap berjalan di latar belakang";
        }
        if (usingOrigin && !"completed".equals(state)) {
            text += " • streaming cadangan aktif";
        }
        statusText.setText(text);

        if (playable && !preparing && !prepared && !manualRetryRequired) {
            startPlayback(false);
        }
        if (!playable && !prepared && !preparing && !manualRetryRequired) {
            showWaiting("Menunggu data awal video… " + progress + "%");
        }
    }

    private void applyVideoAspectRatio() {
        if (playerFrame == null || videoOutputView == null
                || videoWidth <= 0 || videoHeight <= 0) return;

        playerFrame.post(() -> {
            if (destroyed || playerFrame == null || videoOutputView == null) return;
            int frameWidth = playerFrame.getWidth();
            int frameHeight = playerFrame.getHeight();
            if (frameWidth <= 0 || frameHeight <= 0) return;

            float videoRatio = (videoWidth * videoPixelRatio) / Math.max(1f, videoHeight);
            float frameRatio = frameWidth / (float) frameHeight;
            int width;
            int height;
            if (frameRatio > videoRatio) {
                height = frameHeight;
                width = Math.max(1, Math.round(height * videoRatio));
            } else {
                width = frameWidth;
                height = Math.max(1, Math.round(width / videoRatio));
            }

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    width, height, Gravity.CENTER);
            videoOutputView.setLayoutParams(lp);
        });
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
        manualRetryRequired = true;
        showWaiting(message);
        if (loading != null) loading.setVisibility(View.GONE);
        if (retryButton != null) retryButton.setVisibility(View.VISIBLE);
    }

    private void enterPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || isInPictureInPictureMode()) return;
        try {
            Rational ratio = videoWidth > 0 && videoHeight > 0
                    ? new Rational(Math.max(1, videoWidth), Math.max(1, videoHeight))
                    : new Rational(16, 9);
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(ratio)
                    .build();
            enterPictureInPictureMode(params);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                              android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        if (topBar != null) topBar.setVisibility(
                isInPictureInPictureMode || fullscreenMode || !controlsVisible
                        ? View.GONE : View.VISIBLE);
        if (bottomBar != null) bottomBar.setVisibility(
                isInPictureInPictureMode || fullscreenMode ? View.GONE : View.VISIBLE);
        if (isInPictureInPictureMode) setControlsVisible(false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handler.post(() -> {
            applyVideoAspectRatio();
            updateFullscreenButton();
            if (fullscreenMode) {
                hideSystemUi();
                if (topBar != null) topBar.setVisibility(View.GONE);
                if (bottomBar != null) bottomBar.setVisibility(View.GONE);
            }
            rebuildControlActionRows();
        });
    }

    @Override
    public void onBackPressed() {
        if (fullscreenMode) {
            exitFullscreenMode();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) return;
        if (player != null && player.isPlaying()) {
            resumePositionMs = Math.max(resumePositionMs, safePosition());
            player.pause();
            updatePlayPauseGlyph();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (fullscreenMode) hideSystemUi();
        if (prepared && player != null && !manualRetryRequired
                && player.getPlaybackState() != Player.STATE_ENDED) {
            player.play();
            updatePlayPauseGlyph();
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
        releasePlayer(false);
        notifySessionClosed();
        super.onDestroy();
    }

    private void releasePlayer(boolean preservePosition) {
        ExoPlayer current = player;
        player = null;
        if (current == null) return;
        try {
            if (preservePosition) {
                resumePositionMs = Math.max(resumePositionMs,
                        Math.max(0L, current.getCurrentPosition()));
            }
            current.removeListener(playerListener);
            if (surfaceView != null) {
                current.clearVideoSurfaceView(surfaceView);
            } else if (textureView != null) {
                current.clearVideoTextureView(textureView);
            } else {
                current.clearVideoSurface();
            }
            current.release();
        } catch (Exception ignored) {
        }
    }

    private void pausePlayerQuietly() {
        try {
            if (player != null) {
                resumePositionMs = Math.max(resumePositionMs, safePosition());
                player.pause();
            }
        } catch (Exception ignored) {
        }
    }

    private long safePosition() {
        try {
            return player == null ? 0L : Math.max(0L, player.getCurrentPosition());
        } catch (Exception ignored) {
            return 0L;
        }
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

    private void installVideoOutputView() {
        if (playerFrame == null) return;

        if (videoOutputView != null) {
            playerFrame.removeView(videoOutputView);
        }
        textureView = null;
        surfaceView = null;

        if (videoOutputMode == VideoOutputCompatibilityPolicy.OUTPUT_SURFACE) {
            surfaceView = new SurfaceView(this);
            surfaceView.setContentDescription("Video");
            videoOutputView = surfaceView;
        } else {
            textureView = new TextureView(this);
            // Non-opaque avoids a vendor compositor edge case where the texture can stay black
            // even though playback time advances. The player frame itself remains black.
            textureView.setOpaque(false);
            textureView.setContentDescription("Video");
            videoOutputView = textureView;
        }

        installControlTapTarget(videoOutputView);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                -1, -1, Gravity.CENTER);
        playerFrame.addView(videoOutputView, 0, lp);
        // controlsBar is intentionally a sibling below playerFrame, not an overlay.
    }

    private void attachVideoOutput(ExoPlayer target) {
        if (target == null) return;
        if (surfaceView != null) {
            target.setVideoSurfaceView(surfaceView);
        } else if (textureView != null) {
            target.setVideoTextureView(textureView);
        }
    }

    private boolean retryWithAlternateVideoOutput(String reason) {
        if (destroyed || alternateOutputTried) return false;

        alternateOutputTried = true;
        resumePositionMs = Math.max(resumePositionMs, safePosition());
        handler.removeCallbacks(preparationWatchdog);
        handler.removeCallbacks(firstFrameWatchdog);
        releasePlayer(false);

        videoOutputMode = VideoOutputCompatibilityPolicy.alternateOutput(videoOutputMode);
        installVideoOutputView();
        preparing = false;
        prepared = false;
        firstFrameSeen = false;
        manualRetryRequired = false;
        showWaiting(reason + ". Mengganti renderer video…");
        handler.postDelayed(() -> startPlayback(true), 220L);
        return true;
    }

    private void selectPreferredVideoOutput() {
        int preferred = realmeAndroid11
                ? VideoOutputCompatibilityPolicy.OUTPUT_SURFACE
                : VideoOutputCompatibilityPolicy.OUTPUT_TEXTURE;
        if (videoOutputMode != preferred || videoOutputView == null) {
            videoOutputMode = preferred;
            installVideoOutputView();
        }
    }

    private View buildControlsBar() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(6), dp(6), dp(6), dp(6));
        panel.setBackgroundColor(Color.parseColor("#F0000000"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            panel.setElevation(dp(18));
        }
        panel.setClickable(true);
        panel.setOnClickListener(v -> {
            // Consume panel clicks so a button press never toggles the whole controller.
        });

        controlActionRowsHost = new LinearLayout(this);
        controlActionRowsHost.setOrientation(LinearLayout.VERTICAL);

        rewindButton = videoControlButton("−10s", "Mundur 10 detik", false,
                v -> seekBySeconds(-10));

        playPauseBtn = videoControlButton("▶", "Play atau pause", false,
                v -> togglePlayPause());
        playPauseBtn.setTextSize(19);

        forwardButton = videoControlButton("+10s", "Maju 10 detik", false,
                v -> seekBySeconds(10));

        speedButton = videoControlButton("1x", "Kecepatan video", true,
                v -> showPlaybackSpeedDialog());

        qualityButton = videoControlButton("Auto", "Kualitas video otomatis", false,
                v -> showQualityInfoDialog());

        fullscreenButton = videoControlButton("Full", "Masuk layar penuh", false,
                v -> toggleFullscreenMode());

        rebuildControlActionRows();
        panel.addView(controlActionRowsHost,
                new LinearLayout.LayoutParams(-1, -2));

        LinearLayout timelineRow = new LinearLayout(this);
        timelineRow.setOrientation(LinearLayout.HORIZONTAL);
        timelineRow.setGravity(Gravity.CENTER_VERTICAL);
        timelineRow.setPadding(dp(4), 0, dp(4), 0);

        currentTimeText = new TextView(this);
        currentTimeText.setText("0:00");
        currentTimeText.setTextColor(Color.parseColor("#E8EBF0"));
        currentTimeText.setTextSize(12);
        currentTimeText.setGravity(Gravity.CENTER);
        timelineRow.addView(currentTimeText,
                new LinearLayout.LayoutParams(dp(50), dp(42)));

        seekBar = new SeekBar(this);
        seekBar.setMax(1000);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    long duration = safeDuration();
                    if (duration > 0) {
                        currentTimeText.setText(formatTime(duration * progress / 1000L));
                    }
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
        timelineRow.addView(seekBar,
                new LinearLayout.LayoutParams(0, -2, 1f));

        durationText = new TextView(this);
        durationText.setText("0:00");
        durationText.setTextColor(Color.parseColor("#E8EBF0"));
        durationText.setTextSize(12);
        durationText.setGravity(Gravity.CENTER);
        timelineRow.addView(durationText,
                new LinearLayout.LayoutParams(dp(50), dp(42)));

        panel.addView(timelineRow, new LinearLayout.LayoutParams(-1, dp(44)));
        return panel;
    }

    /**
     * Portrait uses two rows of three buttons so every action is visible without
     * clipping. Landscape keeps all six actions in one row, matching the browser
     * toolbar style. This method may be called again after rotation.
     */
    private void rebuildControlActionRows() {
        if (controlActionRowsHost == null || rewindButton == null
                || playPauseBtn == null || forwardButton == null
                || speedButton == null || qualityButton == null
                || fullscreenButton == null) return;

        detachFromParent(rewindButton);
        detachFromParent(playPauseBtn);
        detachFromParent(forwardButton);
        detachFromParent(speedButton);
        detachFromParent(qualityButton);
        detachFromParent(fullscreenButton);
        controlActionRowsHost.removeAllViews();

        boolean portrait = getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_LANDSCAPE;
        if (portrait) {
            LinearLayout first = createControlActionRow();
            first.addView(rewindButton);
            first.addView(playPauseBtn);
            first.addView(forwardButton);
            controlActionRowsHost.addView(first,
                    new LinearLayout.LayoutParams(-1, dp(56)));

            LinearLayout second = createControlActionRow();
            second.addView(speedButton);
            second.addView(qualityButton);
            second.addView(fullscreenButton);
            controlActionRowsHost.addView(second,
                    new LinearLayout.LayoutParams(-1, dp(56)));
        } else {
            LinearLayout row = createControlActionRow();
            row.addView(rewindButton);
            row.addView(playPauseBtn);
            row.addView(forwardButton);
            row.addView(speedButton);
            row.addView(qualityButton);
            row.addView(fullscreenButton);
            controlActionRowsHost.addView(row,
                    new LinearLayout.LayoutParams(-1, dp(58)));
        }
    }

    private LinearLayout createControlActionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private void detachFromParent(View view) {
        if (view == null || !(view.getParent() instanceof android.view.ViewGroup)) return;
        ((android.view.ViewGroup) view.getParent()).removeView(view);
    }

    private TextView videoControlButton(String text, String description,
                                        boolean accent, View.OnClickListener listener) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(accent ? Color.parseColor("#111111") : Color.WHITE);
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setIncludeFontPadding(false);
        button.setContentDescription(description);
        button.setBackground(roundRect(
                accent ? Color.parseColor("#FF9F1C") : Color.parseColor("#20232A"),
                dp(17), dp(1),
                accent ? Color.TRANSPARENT : Color.parseColor("#3A3F49")));
        button.setOnClickListener(v -> {
            handler.removeCallbacks(hideControlsRunnable);
            listener.onClick(v);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        button.setLayoutParams(lp);
        return button;
    }

    private void seekBySeconds(int seconds) {
        if (player == null) return;
        long duration = safeDuration();
        long target = Math.max(0L, safePosition() + seconds * 1000L);
        if (duration > 0L) target = Math.min(duration, target);

        if (!usingOrigin && duration > 0L
                && lastProgressPercent > 0 && lastProgressPercent < 100) {
            long downloadedCap = duration * lastProgressPercent / 100L;
            target = Math.min(target, downloadedCap);
        }

        try {
            player.seekTo(target);
            updateSeekUi();
            setControlsVisible(true);
        } catch (Exception ignored) {
        }
    }

    private void showPlaybackSpeedDialog() {
        final String[] labels = {"0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"};
        final float[] values = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f};
        int checked = 2;
        for (int i = 0; i < values.length; i++) {
            if (Math.abs(values[i] - playbackSpeed) < 0.001f) {
                checked = i;
                break;
            }
        }

        handler.removeCallbacks(hideControlsRunnable);
        new AlertDialog.Builder(this)
                .setTitle("Kecepatan video")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    playbackSpeed = values[which];
                    try {
                        if (player != null) player.setPlaybackSpeed(playbackSpeed);
                    } catch (Exception ignored) {
                    }
                    updateSpeedButton();
                    dialog.dismiss();
                    setControlsVisible(true);
                })
                .setNegativeButton("Batal", (dialog, which) -> setControlsVisible(true))
                .setOnCancelListener(dialog -> setControlsVisible(true))
                .show();
    }

    private void showQualityInfoDialog() {
        handler.removeCallbacks(hideControlsRunnable);
        new AlertDialog.Builder(this)
                .setTitle("Kualitas video: Auto")
                .setMessage("Video download memakai kualitas file sumber. Karena file MP4 ini hanya memiliki satu track video, kualitas tidak dapat diubah tanpa mengunduh sumber lain.")
                .setPositiveButton("OK", (dialog, which) -> setControlsVisible(true))
                .setOnCancelListener(dialog -> setControlsVisible(true))
                .show();
    }

    private void updateSpeedButton() {
        if (speedButton == null) return;
        String label;
        if (Math.abs(playbackSpeed - Math.round(playbackSpeed)) < 0.001f) {
            label = Math.round(playbackSpeed) + "x";
        } else {
            label = String.format(Locale.US, "%.2fx", playbackSpeed)
                    .replace(".00x", "x")
                    .replace("0x", "x");
        }
        speedButton.setText(label);
    }

    private void toggleFullscreenMode() {
        if (fullscreenMode) exitFullscreenMode();
        else enterFullscreenMode();
    }

    private void enterFullscreenMode() {
        fullscreenMode = true;
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } catch (Exception ignored) {
        }
        hideSystemUi();
        if (topBar != null) topBar.setVisibility(View.GONE);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
        updateFullscreenButton();
        setControlsVisible(true);
    }

    private void exitFullscreenMode() {
        fullscreenMode = false;
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } catch (Exception ignored) {
        }
        showSystemUi();
        if (bottomBar != null) bottomBar.setVisibility(View.VISIBLE);
        updateFullscreenButton();
        setControlsVisible(true);
    }

    private void updateFullscreenButton() {
        if (fullscreenButton == null) return;
        fullscreenButton.setText(fullscreenMode ? "Exit" : "Full");
        fullscreenButton.setContentDescription(fullscreenMode
                ? "Keluar dari layar penuh" : "Masuk layar penuh");
    }

    private void hideSystemUi() {
        try {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } catch (Exception ignored) {
        }
    }

    private void showSystemUi() {
        try {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        } catch (Exception ignored) {
        }
    }

    private void installControlTapTarget(View target) {
        if (target == null) return;
        target.setClickable(true);
        target.setOnClickListener(v -> toggleControls());
        target.setOnTouchListener(new View.OnTouchListener() {
            float downX;
            float downY;
            long downAt;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event == null) return false;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        downAt = event.getEventTime();
                        return true;
                    case MotionEvent.ACTION_UP:
                        float dx = Math.abs(event.getX() - downX);
                        float dy = Math.abs(event.getY() - downY);
                        long elapsed = event.getEventTime() - downAt;
                        if (dx <= controlTouchSlop && dy <= controlTouchSlop
                                && elapsed <= TAP_MAX_DURATION_MS) {
                            view.performClick();
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    private void toggleControls() {
        setControlsVisible(!controlsVisible);
    }

    private void setControlsVisible(boolean visible) {
        controlsVisible = visible;
        if (controlsBar != null) controlsBar.setVisibility(
                visible ? View.VISIBLE : View.GONE);
        if (topBar != null && !isInPictureInPictureMode()) {
            topBar.setVisibility(visible && !fullscreenMode ? View.VISIBLE : View.GONE);
        }
        handler.removeCallbacks(hideControlsRunnable);
        if (visible) {
            updateSeekUi();
            updatePlayPauseGlyph();
            scheduleHideControls();
        }
    }

    private void scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable);
        boolean playing = player != null && player.isPlaying();
        if (playing && firstFrameSeen && !userSeeking) {
            handler.postDelayed(hideControlsRunnable, CONTROLS_HIDE_DELAY_MS);
        }
    }

    private void togglePlayPause() {
        if (player == null) {
            if (!manualRetryRequired) startPlayback(true);
            return;
        }
        if (player.isPlaying()) player.pause();
        else player.play();
        updatePlayPauseGlyph();
        setControlsVisible(true);
    }

    private void updatePlayPauseGlyph() {
        if (playPauseBtn == null) return;
        boolean playing = player != null && player.isPlaying();
        playPauseBtn.setText(playing ? "❚❚" : "▶");
        updateSpeedButton();
        updateFullscreenButton();
        if (playing) scheduleHideControls();
        else handler.removeCallbacks(hideControlsRunnable);
    }

    private void updateSeekUi() {
        if (seekBar == null) return;
        long duration = safeDuration();
        long position = safePosition();
        if (duration > 0) {
            if (!userSeeking) {
                seekBar.setProgress((int) Math.min(1000L,
                        position * 1000L / Math.max(1L, duration)));
            }
            durationText.setText(formatTime(duration));

            int buffered = player == null ? 0 : Math.max(0, player.getBufferedPercentage());
            int secondary = usingOrigin
                    ? buffered * 10
                    : Math.max(buffered, lastProgressPercent) * 10;
            seekBar.setSecondaryProgress(Math.min(1000, secondary));
        }
        if (!userSeeking) currentTimeText.setText(formatTime(position));
    }

    private long safeDuration() {
        try {
            long duration = player == null ? 0L : player.getDuration();
            return duration > 0L ? duration : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private void seekToProgress(int progress) {
        long duration = safeDuration();
        if (duration <= 0L || player == null) return;
        long target = duration * progress / 1000L;

        if (!usingOrigin && lastProgressPercent > 0 && lastProgressPercent < 100) {
            long cap = duration * lastProgressPercent / 100L;
            if (target > cap) target = cap;
        }

        try {
            player.seekTo(target);
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

    private static GradientDrawable roundRect(int fillColor, int radius,
                                              int strokeWidth, int strokeColor) {
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
        return String.format(Locale.US, "%.1f MB/s",
                bytesPerSecond / (1024.0 * 1024.0));
    }

    private static String trimError(String value) {
        String normalized = value == null ? "" : value.replace('\n', ' ').trim();
        if (normalized.length() <= 120) return normalized;
        return normalized.substring(0, 117) + "…";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
