package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.*;
import static com.yieldbrowser.app.StorageCodec.decode;
import static com.yieldbrowser.app.StorageCodec.encode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.LruCache;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceError;
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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.webkit.ScriptHandler;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

abstract class YieldDownloadActivity extends YieldActivityState {

void restoreVideoControlsFromFullscreenOverlay() {
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

            videoControlsBar.setBackgroundColor(Color.parseColor("#101217"));
            applyVideoControlsFullscreenLayout(false);
            videoControlsInFullscreen = false;
            updateVideoModeToggleButton();
            videoControlsOriginalParent = null;
            videoControlsOriginalLayoutParams = null;
            videoControlsOriginalIndex = -1;
        } catch (Exception ignored) {
        }
    }

    void showVideoOptimizationDialog() {
        VideoOptimizationDialogController.State state =
                new VideoOptimizationDialogController.State(
                        videoBufferBooster,
                        hlsSegmentPrefetch,
                        videoFloatingPlayer,
                        videoBackgroundPlay);
        new VideoOptimizationDialogController(this).show(state, action -> {
            switch (action) {
                case BUFFER_BOOSTER:
                    videoBufferBooster = !videoBufferBooster;
                    applyBrowserSettings();
                    injectVideoOptimizationIfNeeded();
                    saveSettings();
                    break;
                case HLS_PREFETCH:
                    hlsSegmentPrefetch = !hlsSegmentPrefetch;
                    injectVideoOptimizationIfNeeded();
                    saveSettings();
                    break;
                case MINIMIZE_NORMAL:
                    videoFloatingPlayer =
                            VideoOptimizationDialogController.toggledFloatingPlayer(
                                    videoFloatingPlayer);
                    saveSettings();
                    QuietToast.makeText(this,
                            videoFloatingPlayer
                                    ? "Floating player aktif"
                                    : "Minimize normal aktif",
                            QuietToast.LENGTH_SHORT).show();
                    break;
                case BACKGROUND_PLAY:
                    videoBackgroundPlay = !videoBackgroundPlay;
                    applyBrowserSettings();
                    injectVideoOptimizationIfNeeded();
                    saveSettings();
                    break;
            }
        });
    }

    View videoOptSwitchRow(String title,
                                   String desc,
                                   boolean enabled,
                                   View.OnClickListener listener) {
        return SettingsUi.videoOptSwitchRow(this, title, desc, enabled, listener);
    }

    void ensureVideoPlaybackController() {
        if (videoPlaybackController == null) {
            videoPlaybackController = new VideoPlaybackController(this);
        }
    }

    void detectVideoQualities() {
        ensureVideoPlaybackController();
        videoPlaybackController.detectQualities(webView);
    }

    void showVideoQualityDialog() {
        ensureVideoPlaybackController();
        videoPlaybackController.showQualityDialog(
                webView,
                selectedVideoQuality,
                YieldDownloadActivity.this::injectVideoOptimizationIfNeeded,
                YieldDownloadActivity.this::setVideoQuality);
    }

    void setVideoQuality(String quality) {
        selectedVideoQuality = quality == null ? "Auto" : quality;
        ensureVideoPlaybackController();
        videoPlaybackController.applyQuality(
                webView,
                selectedVideoQuality,
                videoQualityLabel,
                YieldDownloadActivity.this::saveSettings);
    }

    void showVideoSpeedDialog() {
        ensureVideoPlaybackController();
        videoPlaybackController.showSpeedDialog(
                webView,
                videoSpeed,
                YieldDownloadActivity.this::setVideoSpeed);
    }

    void setVideoSpeed(float speed) {
        videoSpeed = speed;
        ensureVideoPlaybackController();
        videoPlaybackController.applySpeed(
                webView,
                videoSpeed,
                videoSpeedLabel,
                YieldDownloadActivity.this::saveSettings);
    }

    void injectVideoPlaybackWatcher() {
        if (webView == null) return;
        if (isSiteCompatibilityModeActiveForUrl(getEffectiveCurrentUrl())) return;
        boolean youtubePage = isYouTubePlaybackUrl(getEffectiveCurrentUrl());
        String js = BrowserPageScripts.videoPlaybackWatcher(
                youtubePage,
                videoBufferBooster,
                hlsSegmentPrefetch,
                videoBackgroundPlay);
        try {
            runPageScript(js);
        } catch (Exception ignored) {
        }
    }

    void injectVideoOptimizationIfNeeded() {
        injectVideoPlaybackWatcher();
    }

    void controlVideo(String action) {
        injectVideoPlaybackWatcher();
        ensureVideoPlaybackController();
        videoPlaybackController.control(
                webView,
                action,
                YieldDownloadActivity.this::refreshVideoPlayPauseButtonState);
    }

    void showHistoryPanel() {
        if (dedicatedPrivateProfile || isCurrentPrivateTab()) {
            QuietToast.makeText(this, "Riwayat tidak disimpan dalam mode Privat",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }
        initializeHistoryEngineV2();
        recordCurrentPageToHistory();
        historyPanelController = new HistoryPanelController(
                this,
                mainHandler,
                historyRepository,
                url -> {
                    addressBar.setText(url);
                    openAddressBarUrl();
                },
                YieldDownloadActivity.this::loadFavicon,
                YieldDownloadActivity.this::clearBrowserHistoryManually);
        historyPanelController.show();
    }

    void refreshHistoryPanelIfShowing() {
        if (historyPanelController == null || !historyPanelController.isShowing()) return;
        mainHandler.postDelayed(historyPanelController::refresh, 120L);
    }

    void showBookmarkHomePanel() {
        BookmarkPanelController controller = new BookmarkPanelController(
                this,
                mainHandler,
                bookmarkData,
                getSharedPreferences(PREFS, MODE_PRIVATE),
                YieldDownloadActivity.this::normalizeShortcutUrl,
                YieldDownloadActivity.this::loadFavicon,
                url -> {
                    addressBar.setText(url);
                    openAddressBarUrl();
                },
                url -> {
                    newTabInCurrentProfile();
                    addressBar.setText(url);
                    openAddressBarUrl();
                },
                YieldDownloadActivity.this::openUrlInPrivateSpace);
        controller.showHome();
    }

    void switchDialogSmooth(Dialog currentDialog, Runnable openNext) {
        try {
            if (openNext != null) openNext.run();
        } catch (Exception e) {
            QuietToast.makeText(this, "Gagal membuka menu: " + e.getMessage(), QuietToast.LENGTH_SHORT).show();
        }

        if (currentDialog != null) {
            mainHandler.postDelayed(() -> {
                try {
                    if (currentDialog.isShowing()) currentDialog.dismiss();
                } catch (Exception ignored) {
                }
            }, 120);
        }
    }

    void ensurePageToolsController() {
        if (pageToolsController == null) pageToolsController = new PageToolsController(this);
    }

    void showFindInPageDialog() {
        ensurePageToolsController();
        pageToolsController.showFindInPage(webView);
    }

    void shareCurrentPage() {
        ensurePageToolsController();
        pageToolsController.sharePage(getEffectiveCurrentUrl());
    }

    void copyCurrentLink() {
        ensurePageToolsController();
        pageToolsController.copyLink(getEffectiveCurrentUrl());
    }

    void showPageInfoDialog() {
        ensurePageToolsController();
        pageToolsController.showPageInfo(webView, getEffectiveCurrentUrl());
    }

    void toggleFullscreenMode() {
        ensurePageToolsController();
        pageToolsController.toggleFullscreen(topBarView, bottomNavView);
    }

    void exitFullscreenMode() {
        ensurePageToolsController();
        pageToolsController.exitFullscreen(topBarView, bottomNavView);
    }

    void saveCurrentPageOffline() {
        ensurePageToolsController();
        pageToolsController.savePageOffline(webView);
    }

    boolean shouldRecordHistoryUrl(String url) {
        if (url == null || url.length() == 0) return false;
        String cleanUrl = extractOriginalUrl(url);
        if (cleanUrl == null || cleanUrl.length() == 0) return false;
        return isHttpOrHttpsUrl(cleanUrl)
                && !isLikelyAdClickUrl(cleanUrl)
                && !isImageResourceUrl(cleanUrl)
                && !cleanUrl.startsWith("about:")
                && !cleanUrl.startsWith("javascript:");
    }

    void initializeHistoryEngineV2() {
        if (dedicatedPrivateProfile) return;
        if (historyRepository == null) {
            historyRepository = HistoryRepository.getInstance(getApplicationContext());
        }

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_HISTORY_ENGINE_V2_INITIALIZED, false)) return;
        if (historyV2InitializationStarted) return;

        // The user requested a clean V2 start: legacy history is removed once and is never
        // migrated or dual-written. The completion marker is written only after SQLite confirms
        // the reset, so a process kill cannot leave a half-initialized engine marked as complete.
        historyV2InitializationStarted = true;
        historyClearLock = true;
        clearLegacyHistoryStorageNow();
        historyRepository.clearAll(success -> {
            historyV2InitializationStarted = false;
            historyClearLock = false;
            if (success) {
                prefs.edit().putBoolean(KEY_HISTORY_ENGINE_V2_INITIALIZED, true).apply();
                recordCurrentPageToHistory();
                refreshHistoryPanelIfShowing();
            }
        });
    }

    void clearLegacyHistoryStorageNow() {
        try {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .remove(KEY_BROWSER_HISTORY)
                    .remove(KEY_BROWSER_HISTORY_BACKUP)
                    .remove(KEY_BROWSER_HISTORY_V2)
                    .remove(KEY_BROWSER_HISTORY_V3)
                    .apply();
        } catch (Exception ignored) {
        }
        try {
            getSharedPreferences(PREFS_HISTORY_V2, MODE_PRIVATE).edit().clear().apply();
        } catch (Exception ignored) {
        }
        try {
            File file = new File(new File(getFilesDir(), HISTORY_V3_FOLDER), HISTORY_V3_PUBLIC_FILE);
            if (file.exists()) file.delete();
        } catch (Exception ignored) {
        }
        try {
            File file = new File(getFilesDir(), HISTORY_V3_FILE);
            if (file.exists()) file.delete();
        } catch (Exception ignored) {
        }
        try {
            File external = getExternalFilesDir(null);
            if (external != null) {
                File file = new File(new File(external, HISTORY_V3_FOLDER), HISTORY_V3_PUBLIC_FILE);
                if (file.exists()) file.delete();
            }
        } catch (Exception ignored) {
        }
        historyData.clear();
    }

    /**
     * History V2 records committed navigations directly. Re-scanning the complete WebView
     * back/forward stack is intentionally disabled because it created duplicate writes and UI
     * stalls in the legacy engine.
     */
    void recordWebViewBackForwardHistory() {
        // Intentionally empty.
    }

    void recordCurrentPageToHistory() {
        try {
            if (historyClearLock || dedicatedPrivateProfile || isCurrentPrivateTab()) return;
            if (webView == null) return;
            String url = webView.getUrl();
            if (shouldRecordHistoryUrl(url)) addBrowserHistory(webView.getTitle(), url);
        } catch (Exception ignored) {
        }
    }

    void addBrowserHistory(String title, String url) {
        if (historyClearLock || dedicatedPrivateProfile || isCurrentPrivateTab()) return;
        if (!shouldRecordHistoryUrl(url)) return;
        initializeHistoryEngineV2();
        if (historyRepository == null) return;

        String cleanUrl = extractOriginalUrl(url);
        if (cleanUrl == null || cleanUrl.trim().isEmpty()) return;
        String safeTitle = title == null || title.trim().isEmpty() ? cleanUrl : title.trim();
        historyRepository.recordVisit(safeTitle, cleanUrl, System.currentTimeMillis());
    }

    /** Retained for old call sites; V2 never performs a blocking load on the UI thread. */
    void loadBrowserHistory() {
        initializeHistoryEngineV2();
    }

    /** Retained for old call sites; every V2 write is persisted transactionally by SQLite. */
    void saveBrowserHistory() {
        // No-op. HistoryRepository is the only source of truth.
    }

    void clearBrowserHistoryManually(Runnable afterClear) {
        if (dedicatedPrivateProfile || isCurrentPrivateTab()) return;
        initializeHistoryEngineV2();
        if (historyRepository == null) return;

        historyClearLock = true;
        clearLegacyHistoryStorageNow();
        historyRepository.clearAll(success -> {
            historyClearLock = false;
            if (success) {
                lastSafeHttpUrl = "";
                try {
                    for (TabInfo tab : tabs) {
                        if (tab != null && tab.webView != null) {
                            tab.webView.clearHistory();
                            tab.webView.clearFormData();
                        }
                    }
                    if (webView != null) {
                        webView.clearHistory();
                        webView.clearFormData();
                    }
                } catch (Exception ignored) {
                }
                QuietToast.makeText(this, "Semua riwayat telah dihapus", QuietToast.LENGTH_SHORT).show();
                if (afterClear != null) afterClear.run();
            } else {
                QuietToast.makeText(this, "Riwayat gagal dihapus", QuietToast.LENGTH_SHORT).show();
                refreshHistoryPanelIfShowing();
            }
        });
    }

    void ensureBrowserUtilityDialogsController() {
        if (browserUtilityDialogsController == null) {
            browserUtilityDialogsController = new BrowserUtilityDialogsController(this);
        }
    }

    void showTextZoomDialog(Dialog parentDialog) {
        ensureBrowserUtilityDialogsController();
        browserUtilityDialogsController.showTextZoom(
                parentDialog,
                textZoom,
                new BrowserUtilityDialogsController.TextZoomHandler() {
                    @Override
                    public void saveZoom(int zoom) {
                        textZoom = zoom;
                        applyBrowserSettings();
                        saveSettings();
                    }

                    @Override
                    public void reopenSettings() {
                        showSettingsPanel();
                    }
                });
    }

    String getDownloadLocationText() {
        if (selectedDownloadTreeUri != null && selectedDownloadTreeUri.length() > 0) {
            return "Lokasi sekarang:\nFolder HP dipilih\n" + selectedDownloadTreeUri;
        }
        return "Lokasi sekarang:\nDefault: Download/Yield Browser\nStaging: " + getDownloadDirectory().getAbsolutePath();
    }

    void showDownloadFolderDialog(Dialog parentDialog) {
        ensureBrowserUtilityDialogsController();
        browserUtilityDialogsController.showDownloadFolder(
                parentDialog,
                downloadSubfolder,
                getDownloadLocationText(),
                new BrowserUtilityDialogsController.DownloadFolderHandler() {
                    @Override
                    public void saveSubfolder(String subfolder) {
                        downloadSubfolder = subfolder;
                        saveSettings();
                    }

                    @Override
                    public void choosePhoneFolder(String subfolder) {
                        downloadSubfolder = subfolder;
                        saveSettings();
                        chooseExternalDownloadFolder();
                    }

                    @Override
                    public void resetDefault(String subfolder) {
                        downloadSubfolder = subfolder;
                        selectedDownloadTreeUri = "";
                        saveSettings();
                    }

                    @Override
                    public void reopenDownloadSettings() {
                        showDownloadSettingsPanel();
                    }
                });
    }

    void chooseExternalDownloadFolder() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            startActivityForResult(intent, REQ_PICK_DOWNLOAD_FOLDER);
        } catch (Exception e) {
            QuietToast.makeText(this, "Pemilih folder tidak tersedia", QuietToast.LENGTH_SHORT).show();
        }
    }

    TextView sectionTitle(String text) {
        return SettingsUi.sectionTitle(this, text);
    }

    View menuDivider() {
        return SettingsUi.menuDivider(this);
    }

    View customizeToggleRow(int iconRes, String label, boolean enabled, View.OnClickListener listener) {
        return SettingsUi.customizeToggleRow(this, iconRes, label, enabled, listener);
    }

    View settingRow(int iconRes, String title, String desc, boolean enabled, View.OnClickListener listener) {
        return SettingsUi.settingRow(this, iconRes, title, desc, enabled, listener);
    }

    View actionRow(int iconRes, String title, String desc, View.OnClickListener listener) {
        return SettingsUi.actionRow(this, iconRes, title, desc, listener);
    }

    LinearLayout baseSettingsRow(int iconRes, String title, String desc, View.OnClickListener listener) {
        return SettingsUi.baseSettingsRow(this, iconRes, title, desc, listener);
    }

    View menuRow(int iconRes, String label, View.OnClickListener listener) {
        return SettingsUi.menuRow(this, iconRes, label, listener);
    }

    // ===== Download manager UI =====
    void showDownloadManager() {
        if (activeDownloadDialog != null && activeDownloadDialog.isShowing()) {
            renderDownloadList();
            return;
        }
        downloadManagerShell = new DownloadManagerShell(this);
        activeDownloadBindings = downloadManagerShell.show(
                activeDownloadSection,
                activeDownloadCategory,
                new DownloadListAdapter.Callback() {
                    @Override
                    public void onRowClick(int downloadId, View anchor) {
                        DownloadItem item = findDownloadItemById(downloadId);
                        if (item == null) return;
                        if (downloadSelectMode) {
                            toggleDownloadSelection(item);
                        } else if (canPlayDownloadInsideYield(item)
                                && ("running".equals(item.status)
                                || "paused".equals(item.status)
                                || "failed".equals(item.status)
                                || "completed".equals(item.status)
                                || "verifying".equals(item.status)
                                || "saving".equals(item.status))) {
                            playDownloadInsideYield(item);
                        } else if ("completed".equals(item.status)) {
                            openDownloadedFile(item);
                        } else if ("failed".equals(item.status)
                                || "paused".equals(item.status)
                                || "queued".equals(item.status)
                                || "running".equals(item.status)) {
                            showDownloadItemMenu(anchor, item);
                        }
                    }

                    @Override
                    public boolean onRowLongClick(int downloadId) {
                        DownloadItem item = findDownloadItemById(downloadId);
                        if (item == null) return false;
                        downloadSelectMode = true;
                        toggleDownloadSelection(item);
                        return true;
                    }

                    @Override
                    public void onPrimaryAction(int downloadId) {
                        DownloadItem item = findDownloadItemById(downloadId);
                        if (item != null) handleDownloadPrimaryAction(item);
                    }

                    @Override
                    public void onMore(int downloadId, View anchor) {
                        DownloadItem item = findDownloadItemById(downloadId);
                        if (item != null) showDownloadItemMenu(anchor, item);
                    }
                },
                new DownloadManagerShell.Callback() {
                    @Override
                    public void onSearchRequested() {
                        showDownloadSearchDialog();
                    }

                    @Override
                    public void onSettingsRequested() {
                        showDownloadSettingsPanel();
                    }

                    @Override
                    public void onSectionSelected(String section) {
                        if (section.equals(activeDownloadSection)) return;
                        activeDownloadSection = section;
                        downloadSelectMode = false;
                        selectedDownloadIds.clear();
                        invalidateDownloadControls();
                        renderDownloadSectionTabs();
                        renderDownloadList();
                    }

                    @Override
                    public void onCategorySelected(String category) {
                        activeDownloadCategory = category;
                        selectedDownloadIds.clear();
                        downloadSelectMode = false;
                        invalidateDownloadControls();
                        renderDownloadCategoryChips();
                        renderDownloadList();
                    }

                    @Override
                    public void onDismissed() {
                        clearDownloadManagerBindings();
                    }
                });
        activeDownloadDialog = activeDownloadBindings.dialog;
        activeDownloadRecyclerView = activeDownloadBindings.recyclerView;
        activeDownloadAdapter = activeDownloadBindings.adapter;
        activeDownloadCategoryPanel = activeDownloadBindings.categoryPanel;
        activeDownloadControlsPanel = activeDownloadBindings.controlsPanel;
        activeDownloadTitleView = activeDownloadBindings.titleView;
        activeDownloadStorageView = activeDownloadBindings.storageView;
        activeDownloadRunningTab = activeDownloadBindings.runningTab;
        activeDownloadCompletedTab = activeDownloadBindings.completedTab;
        activeDownloadEmptyView = activeDownloadBindings.emptyView;

        renderDownloadList();
        downloadUiTickerRunning = true;
        mainHandler.removeCallbacks(downloadUiTicker);
        mainHandler.postDelayed(downloadUiTicker, getDownloadUiTickerDelayMs());
    }

    void clearDownloadManagerBindings() {
        downloadUiTickerRunning = false;
        mainHandler.removeCallbacks(downloadUiTicker);
        activeDownloadDialog = null;
        activeDownloadRecyclerView = null;
        activeDownloadAdapter = null;
        activeDownloadCategoryPanel = null;
        activeDownloadControlsPanel = null;
        activeDownloadTitleView = null;
        activeDownloadStorageView = null;
        activeDownloadRunningTab = null;
        activeDownloadCompletedTab = null;
        activeDownloadEmptyView = null;
        activeDownloadBindings = null;
        downloadSelectMode = false;
        selectedDownloadIds.clear();
        invalidateDownloadControls();
    }

    void renderDownloadSectionTabs() {
        if (downloadManagerShell != null) {
            downloadManagerShell.styleSectionTabs(activeDownloadBindings, activeDownloadSection);
        }
    }

    void renderDownloadCategoryChips() {
        if (downloadManagerShell == null) return;
        downloadManagerShell.renderCategories(
                activeDownloadBindings,
                activeDownloadCategory,
                new DownloadManagerShell.Callback() {
                    @Override
                    public void onSearchRequested() {
                    }

                    @Override
                    public void onSettingsRequested() {
                    }

                    @Override
                    public void onSectionSelected(String section) {
                    }

                    @Override
                    public void onCategorySelected(String category) {
                        activeDownloadCategory = category;
                        selectedDownloadIds.clear();
                        downloadSelectMode = false;
                        invalidateDownloadControls();
                        renderDownloadCategoryChips();
                        renderDownloadList();
                    }

                    @Override
                    public void onDismissed() {
                    }
                });
    }

    void showDownloadSearchDialog() {
        if (downloadManagerShell == null) downloadManagerShell = new DownloadManagerShell(this);
        downloadManagerShell.showSearchDialog(activeDownloadSearchQuery, query -> {
            activeDownloadSearchQuery = query;
            renderDownloadList();
        });
    }

    void showDownloadSortDialog() {
        if (downloadManagerShell == null) downloadManagerShell = new DownloadManagerShell(this);
        downloadManagerShell.showSortDialog(activeDownloadSort, sort -> {
            activeDownloadSort = sort;
            invalidateDownloadControls();
            renderDownloadList();
        });
    }

    void ensureDownloadPanelPresenter() {
        if (downloadPanelPresenter != null) return;
        downloadPanelPresenter = new DownloadPanelPresenter(
                this,
                downloadItems,
                new DownloadPanelPresenter.Host() {
                    @Override
                    public boolean canPlay(DownloadItem item) {
                        return canPlayDownloadInsideYield(item);
                    }

                    @Override
                    public boolean hasFinalizingDownload() {
                        return YieldDownloadActivity.this.hasFinalizingDownload();
                    }

                    @Override
                    public String storageUsageText() {
                        return getStorageUsageText();
                    }

                    @Override
                    public int activeCount() {
                        return countActiveDownloads();
                    }

                    @Override
                    public int queuedCount() {
                        return countQueuedDownloads();
                    }

                    @Override
                    public int completedHistoryCount() {
                        return countCompletedDownloadHistory();
                    }

                    @Override
                    public void showSort() {
                        showDownloadSortDialog();
                    }

                    @Override
                    public void toggleSelectMode() {
                        downloadSelectMode = !downloadSelectMode;
                        selectedDownloadIds.clear();
                        invalidateDownloadControls();
                        renderDownloadList();
                    }

                    @Override
                    public void clearCompletedHistory() {
                        confirmClearCompletedDownloadHistory();
                    }

                    @Override
                    public void shareSelected() {
                        shareSelectedDownloads();
                    }

                    @Override
                    public void deleteSelected() {
                        deleteSelectedDownloads();
                    }

                    @Override
                    public void pauseAll() {
                        pauseAllDownloads();
                    }

                    @Override
                    public void resumeAll() {
                        resumeAllDownloads();
                    }

                    @Override
                    public void showQueueSettings() {
                        showDownloadQueueSettingsDialog();
                    }
                });
    }

    void invalidateDownloadControls() {
        if (downloadPanelPresenter != null) downloadPanelPresenter.invalidateControls();
    }

    void renderDownloadList() {
        if (activeDownloadBindings == null) return;
        ensureDownloadPanelPresenter();
        downloadPanelPresenter.render(
                activeDownloadBindings,
                new DownloadPanelPresenter.State(
                        activeDownloadSection,
                        activeDownloadCategory,
                        activeDownloadSearchQuery,
                        activeDownloadSort,
                        downloadSelectMode,
                        selectedDownloadIds,
                        downloadQueuePaused,
                        downloadMaxActive));
    }

    TextView downloadToolButton(String text) {
        return DownloadControlsFactory.createButton(this, text);
    }

    String getDownloadCategory(DownloadItem item) {
        return DownloadItemUtils.getDownloadCategory(item);
    }

    String normalizeDetectedCategory(String raw) {
        return DownloadItemUtils.normalizeDetectedCategory(raw);
    }

    String inferDownloadCategoryFromData(String fileName, String url, String mimeType) {
        return DownloadItemUtils.inferDownloadCategoryFromData(fileName, url, mimeType);
    }

    DownloadItem findDownloadItemById(int id) {
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) if (item.id == id) return item;
        }
        return null;
    }

    long getDownloadSize(DownloadItem item) {
        return DownloadItemUtils.getDownloadSize(item);
    }

    String getDownloadHost(DownloadItem item) {
        return DownloadItemUtils.getDownloadHost(item);
    }

    String safeText(String text) {
        return text == null ? "" : text;
    }

    void resetDownloadSpeedState(DownloadItem item) {
        DownloadItemUtils.resetDownloadSpeedState(item);
    }

    void updateDownloadSpeed(DownloadItem item, long currentBytes) {
        DownloadItemUtils.updateDownloadSpeed(item, currentBytes);
    }

    String readableSpeed(double bytesPerSecond) {
        return BrowserUtils.readableSpeed(bytesPerSecond);
    }

    String readableFileSize(long size) {
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

    String getStorageUsageText() {
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

    void toggleDownloadSelection(DownloadItem item) {
        if (selectedDownloadIds.contains(item.id)) selectedDownloadIds.remove(item.id);
        else selectedDownloadIds.add(item.id);
        renderDownloadList();
    }

    ArrayList<DownloadItem> getSelectedDownloads() {
        ArrayList<DownloadItem> selected = new ArrayList<>();
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if (selectedDownloadIds.contains(item.id)) selected.add(item);
            }
        }
        return selected;
    }

    void shareSelectedDownloads() {
        ArrayList<DownloadItem> selected = getSelectedDownloads();
        if (selected.isEmpty()) {
            QuietToast.makeText(this, "Belum ada file dipilih", QuietToast.LENGTH_SHORT).show();
            return;
        }
        if (selected.size() == 1) {
            shareDownloadedFile(selected.get(0));
            return;
        }
        QuietToast.makeText(this, "Bagikan multi-file akan disempurnakan setelah FileProvider aktif", QuietToast.LENGTH_LONG).show();
    }

    int countCompletedDownloadHistory() {
        synchronized (downloadItems) {
            return DownloadHistoryClearPolicy.countClearable(downloadItems);
        }
    }

    void confirmClearCompletedDownloadHistory() {
        int count = countCompletedDownloadHistory();
        if (count <= 0) {
            QuietToast.makeText(this, "Tidak ada riwayat unduhan selesai",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Hapus semua riwayat unduhan?")
                .setMessage(count + " riwayat unduhan selesai akan dihapus dari daftar. "
                        + "File yang sudah tersimpan tetap ada di perangkat.")
                .setPositiveButton("Hapus semua", (dialog, which) -> clearCompletedDownloadHistory())
                .setNegativeButton("Batal", null)
                .show();
    }

    void clearCompletedDownloadHistory() {
        ArrayList<DownloadItem> removed = new ArrayList<>();
        synchronized (downloadItems) {
            for (int i = downloadItems.size() - 1; i >= 0; i--) {
                DownloadItem item = downloadItems.get(i);
                if (!DownloadHistoryClearPolicy.isClearable(item)) continue;
                item.runGeneration++;
                item.pauseRequested = true;
                item.status = "removed";
                removed.add(item);
                downloadItems.remove(i);
            }
        }

        if (removed.isEmpty()) {
            QuietToast.makeText(this, "Tidak ada riwayat unduhan selesai",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }

        for (DownloadItem item : removed) {
            stopActiveDownloadTransports(item);
            cancelDownloadNotification(item);
            selectedDownloadIds.remove(item.id);
        }
        downloadSelectMode = false;
        invalidateDownloadControls();
        saveDownloadHistory();
        renderDownloadList();
        updateDownloadKeepAliveState();
        mainHandler.post(this::pumpDownloadQueue);
        QuietToast.makeText(this, removed.size() + " riwayat unduhan dihapus. File tetap tersimpan.",
                QuietToast.LENGTH_SHORT).show();
    }

    void deleteSelectedDownloads() {
        ArrayList<DownloadItem> selected = getSelectedDownloads();
        if (selected.isEmpty()) {
            QuietToast.makeText(this, "Belum ada file dipilih", QuietToast.LENGTH_SHORT).show();
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

    // ===== Download queue manager =====
    boolean isActiveDownloadStatus(String status) {
        return BrowserUtils.isActiveDownloadStatus(status);
    }

    boolean hasFinalizingDownload() {
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if (item != null && ("verifying".equals(item.status) || "saving".equals(item.status))) {
                    return true;
                }
            }
        }
        return false;
    }

    long getDownloadUiTickerDelayMs() {
        return hasFinalizingDownload()
                ? DownloadFinalizationPolicy.UI_TICK_FINALIZING_MS
                : DownloadFinalizationPolicy.UI_TICK_NORMAL_MS;
    }

    int countActiveDownloads() {
        int count = 0;
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if (isActiveDownloadStatus(item.status)) count++;
            }
        }
        return count;
    }

    boolean isCurrentDownloadRun(DownloadItem item, int generation) {
        return item != null && item.runGeneration == generation && !"removed".equals(item.status);
    }

    DownloadItem findForegroundActiveDownload() {
        DownloadItem selected = null;
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if (item == null || !isActiveDownloadStatus(item.status)) continue;
                if (selected == null || item.speedBytesPerSecond > selected.speedBytesPerSecond) {
                    selected = item;
                }
            }
        }
        return selected;
    }

    int getVisibleDownloadProgressPercent(DownloadItem item) {
        return DownloadItemUtils.getVisibleDownloadProgressPercent(item);
    }

    String getForegroundDownloadText(DownloadItem item) {
        if (item == null) return "Mengunduh";
        if ("saving".equals(item.status)) {
            return "Menyimpan ke Downloads… " + getVisibleDownloadProgressPercent(item) + "%";
        }
        if ("verifying".equals(item.status)) return "Memverifikasi file…";
        double speed = item.smoothedSpeedBytesPerSecond > 0
                ? item.smoothedSpeedBytesPerSecond : item.speedBytesPerSecond;
        String text = "Mengunduh • " + getVisibleDownloadProgressPercent(item) + "%";
        if (speed > 0) text += " • " + readableSpeed(speed);
        return text;
    }

    void updateDownloadKeepAliveState() {
        try {
            int activeCount = countActiveDownloads();
            DownloadItem selected = findForegroundActiveDownload();
            if (activeCount > 0 && selected != null) {
                DownloadKeepAliveService.startOrUpdate(this, selected, activeCount,
                        getForegroundDownloadText(selected));
            } else {
                DownloadKeepAliveService.stop(this);
            }
        } catch (Exception ignored) {
        }
    }

    int countQueuedDownloads() {
        int count = 0;
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if ("queued".equals(item.status)) count++;
            }
        }
        return count;
    }

    DownloadItem findNextQueuedDownload() {
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if ("queued".equals(item.status)) return item;
            }
        }
        return null;
    }

    void enqueueOrStartDownload(DownloadItem item, File out) {
        if (item == null || out == null || "removed".equals(item.status)) return;
        if ("running".equals(item.status)) return;
        if (!downloadQueueEnabled) {
            startQueuedDownloadNow(item);
            return;
        }

        item.status = "queued";
        item.pauseRequested = false;
        item.engineInfo = "Antri • menunggu slot";
        saveDownloadHistory();
        refreshDownloadPanel();

        if (!downloadQueuePaused && countActiveDownloads() < Math.max(1, downloadMaxActive)) {
            startQueuedDownloadNow(item);
        } else {
            showDownloadNotification(item, "Masuk antrian", true);
        }
    }

    void startQueuedDownloadNow(DownloadItem item) {
        if (item == null || "removed".equals(item.status)) return;
        try {
            if ("running".equals(item.status)) return;
            if (!"queued".equals(item.status) && !"paused".equals(item.status)) return;
            if (downloadQueueEnabled && countActiveDownloads() >= Math.max(1, downloadMaxActive)) return;

            File out = new File(item.path);
            synchronized (item.stateLock) {
                item.runGeneration++;
                item.status = "running";
                item.pauseRequested = false;
                resetDownloadSpeedState(item);
                item.activeConnectionLimit = 0;
                item.lastSpeedTimeMs = 0;
                item.lastSpeedBytes = item.downloadedBytes;
                item.engineInfo = item.downloadedBytes > 0 ? "Melanjutkan koneksi" : "Mengecek koneksi";
                item.rateLimiter.reset();
            }

            saveDownloadHistory();
            refreshDownloadPanel();
            cancelDownloadNotification(item);
            updateDownloadKeepAliveState();
            showDownloadNotification(item, item.downloadedBytes > 0 ? "Melanjutkan unduhan" : "Mulai mengunduh", true);
            startTwoConnectionDownload(item, out);
        } catch (Exception e) {
            failDownload(item, e.getMessage());
        }
    }

    void pumpDownloadQueue() {
        if (downloadQueuePaused) return;

        if (!downloadQueueEnabled) {
            DownloadItem item;
            while ((item = findNextQueuedDownload()) != null) {
                startQueuedDownloadNow(item);
            }
            return;
        }

        int safety = 0;
        while (countActiveDownloads() < Math.max(1, downloadMaxActive) && safety < 8) {
            DownloadItem next = findNextQueuedDownload();
            if (next == null) break;
            startQueuedDownloadNow(next);
            safety++;
        }
    }

    void pauseAllDownloads() {
        downloadQueuePaused = true;
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if ("running".equals(item.status)) {
                    item.runGeneration++;
                    item.pauseRequested = true;
                    stopActiveDownloadTransports(item);
                    item.status = "paused";
                    resetDownloadSpeedState(item);
                    item.engineInfo = getConnectionLabel(item) + " • dijeda";
                    showDownloadNotification(item, "Unduhan dijeda", false);
                } else if ("queued".equals(item.status)) {
                    item.runGeneration++;
                    item.status = "paused";
                    item.engineInfo = "Dijeda dari antrian";
                }
            }
        }
        saveDownloadHistory();
        refreshDownloadPanel();
        updateDownloadKeepAliveState();
        QuietToast.makeText(this, "Semua download dijeda", QuietToast.LENGTH_SHORT).show();
    }

    void resumeAllDownloads() {
        downloadQueuePaused = false;
        synchronized (downloadItems) {
            for (DownloadItem item : downloadItems) {
                if ("paused".equals(item.status)) {
                    item.status = "queued";
                    item.pauseRequested = false;
                    item.engineInfo = "Antri • menunggu slot";
                    resetDownloadSpeedState(item);
                }
            }
        }
        saveDownloadHistory();
        refreshDownloadPanel();
        QuietToast.makeText(this, "Semua download masuk antrian", QuietToast.LENGTH_SHORT).show();
        pumpDownloadQueue();
    }

    void prioritizeQueuedDownload(DownloadItem item, boolean startIfPossible) {
        if (item == null) return;
        synchronized (downloadItems) {
            int idx = downloadItems.indexOf(item);
            if (idx > 0) {
                downloadItems.remove(idx);
                downloadItems.add(0, item);
            }
            if ("paused".equals(item.status)) {
                item.status = "queued";
                item.pauseRequested = false;
            }
            if ("queued".equals(item.status)) {
                item.engineInfo = "Prioritas • menunggu slot";
            }
        }
        activeDownloadSort = "Antrian";
        saveDownloadHistory();
        refreshDownloadPanel();
        QuietToast.makeText(this, "File diprioritaskan", QuietToast.LENGTH_SHORT).show();
        if (startIfPossible) pumpDownloadQueue();
    }

    void moveQueuedDownload(DownloadItem item, int direction) {
        if (item == null) return;
        synchronized (downloadItems) {
            int idx = downloadItems.indexOf(item);
            if (idx < 0) return;
            int target = idx + direction;
            if (target < 0 || target >= downloadItems.size()) return;
            downloadItems.remove(idx);
            downloadItems.add(target, item);
        }
        activeDownloadSort = "Antrian";
        saveDownloadHistory();
        refreshDownloadPanel();
        QuietToast.makeText(this, direction < 0 ? "Naik antrian" : "Turun antrian", QuietToast.LENGTH_SHORT).show();
    }

    void handleDownloadPrimaryAction(DownloadItem item) {
        if (item == null) return;

        long now = System.currentTimeMillis();
        if (now - item.lastActionClickMs < 650) {
            return;
        }
        item.lastActionClickMs = now;

        try {
            String status = item.status == null ? "" : item.status;
            if ("running".equals(status)) {
                pauseDownloadItem(item);
            } else if ("paused".equals(status)) {
                resumeDownloadItem(item);
            } else if ("queued".equals(status)) {
                if (!downloadQueuePaused && countActiveDownloads() < Math.max(1, downloadMaxActive)) {
                    startQueuedDownloadNow(item);
                } else {
                    prioritizeQueuedDownload(item, true);
                }
            } else if ("failed".equals(status)) {
                reloadDownloadItem(item);
            } else if ("completed".equals(status)) {
                openDownloadedFile(item);
            }
        } catch (Exception e) {
            QuietToast.makeText(this, "Aksi download gagal: " + e.getMessage(), QuietToast.LENGTH_SHORT).show();
        }

        refreshDownloadPanel();
    }

    String getConnectionLabel(DownloadItem item) {
        if (item == null) return "Premium Fast";
        if (item.hlsDownload) return "HLS";
        int visibleConnections = item.activeConnectionLimit > 0 ? item.activeConnectionLimit : item.connectionCount;
        if (visibleConnections >= 4) return "4 koneksi";
        if (visibleConnections == 3) return "3 koneksi";
        if (visibleConnections >= 2) return "2 koneksi";
        if (visibleConnections == 1) return "1 koneksi";
        return "Premium Fast • anti-hotlink safe";
    }

    void pauseDownloadItem(DownloadItem item) {
        if (item == null || (!"running".equals(item.status) && !"queued".equals(item.status))) return;
        item.runGeneration++;
        item.pauseRequested = true;
        stopActiveDownloadTransports(item);
        item.status = "paused";
        resetDownloadSpeedState(item);
        item.engineInfo = getConnectionLabel(item) + " • dijeda";
        saveDownloadHistory();
        refreshDownloadPanel();
        updateDownloadKeepAliveState();
        showDownloadNotification(item, "Unduhan dijeda", false);
        QuietToast.makeText(this, "Unduhan dijeda", QuietToast.LENGTH_SHORT).show();
        mainHandler.postDelayed(this::pumpDownloadQueue, 250);
    }

    void resumeDownloadItem(DownloadItem item) {
        if (item == null || !"paused".equals(item.status)) return;
        try {
            item.pauseRequested = false;
            resetDownloadSpeedState(item);
            item.lastSpeedTimeMs = 0;
            item.lastSpeedBytes = item.downloadedBytes;
            downloadQueuePaused = false;

            File staged = item.path == null || item.path.isEmpty() ? null : new File(item.path);
            boolean networkPayloadComplete = staged != null && staged.exists() && staged.length() > 0
                    && ((!item.hlsDownload && item.totalBytes > 0
                            && item.downloadedBytes >= item.totalBytes
                            && staged.length() == item.totalBytes)
                        || (item.hlsDownload && item.totalBytes > 0
                            && item.hlsCompletedSegments >= item.totalBytes));
            if (networkPayloadComplete) {
                item.runGeneration++;
                item.status = "verifying";
                item.engineInfo = "Memulihkan tahap penyimpanan";
                refreshDownloadPanel();
                updateDownloadKeepAliveState();
                completeDownload(item);
                return;
            }

            boolean slotAvailable = !downloadQueueEnabled || countActiveDownloads() < Math.max(1, downloadMaxActive);
            if (slotAvailable) {
                item.engineInfo = "Melanjutkan koneksi";
                saveDownloadHistory();
                refreshDownloadPanel();
                QuietToast.makeText(this, "Melanjutkan unduhan", QuietToast.LENGTH_SHORT).show();
                startQueuedDownloadNow(item);
            } else {
                item.status = "queued";
                item.engineInfo = "Antri • menunggu slot";
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, "Masuk antrian", true);
                QuietToast.makeText(this, "Slot penuh, unduhan masuk antrian", QuietToast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            failDownload(item, e.getMessage());
        }
    }

    void reloadDownloadItem(DownloadItem item) {
        if (item == null) return;
        try {
            item.runGeneration++;
            stopActiveDownloadTransports(item);
            item.pauseRequested = false;
            item.status = "queued";
            item.progress = 0;
            item.downloadedBytes = 0;
            item.totalBytes = 0;
            item.connectionCount = 0;
            item.activeConnectionLimit = 0;
            clearDynamicPartState(item);
            item.resolvedUrl = "";
            item.etag = "";
            item.lastModified = "";
            item.hlsCompletedSegments = 0;
            item.hlsOutputBytes = 0;
            item.hlsPlaylistFingerprint = "";
            item.hlsInitMapWritten = false;
            item.finalizeProgress = 0;
            item.finalizeBytes = 0;
            item.finalizeTotalBytes = 0;
            item.turboTargetConnections = 0;
            item.turboProfile = "";
            item.turboRetryPenalty = 0;
            resetDownloadSpeedState(item);
            item.lastSpeedTimeMs = 0;
            item.lastSpeedBytes = 0;
            item.engineInfo = "Mengecek koneksi";
            item.failReason = "";
            item.retryCount = 0;

            File out = new File(item.path);
            if (out.exists() && !out.delete()) throw new Exception("File lama tidak dapat dihapus");

            saveDownloadHistory();
            refreshDownloadPanel();
            updateDownloadKeepAliveState();
            showDownloadNotification(item, "Masuk antrian reload", true);
            QuietToast.makeText(this, "Download dimulai ulang dan masuk antrian", QuietToast.LENGTH_SHORT).show();
            enqueueOrStartDownload(item, out);
        } catch (Exception e) {
            failDownload(item, e.getMessage());
        }
    }

    void showDownloadItemMenu(View anchor, DownloadItem item) {
        if (downloadItemMenuController == null) {
            downloadItemMenuController = new DownloadItemMenuController(this);
        }
        downloadItemMenuController.show(
                anchor,
                item,
                canPlayDownloadInsideYield(item),
                action -> {
                    switch (action) {
                        case PAUSE: pauseDownloadItem(item); break;
                        case RESUME: resumeDownloadItem(item); break;
                        case RELOAD: reloadDownloadItem(item); break;
                        case PRIORITIZE: prioritizeQueuedDownload(item, true); break;
                        case MOVE_UP: moveQueuedDownload(item, -1); break;
                        case MOVE_DOWN: moveQueuedDownload(item, 1); break;
                        case PLAY: playDownloadInsideYield(item); break;
                        case OPEN_EXTERNAL: openDownloadedFile(item); break;
                        case SHARE: shareDownloadedFile(item); break;
                        case RENAME: renameDownloadedFile(item); break;
                        case REMOVE_HISTORY: removeDownloadItem(item, false); break;
                        case DELETE_FILE: removeDownloadItem(item, true); break;
                    }
                });
    }

    boolean canPlayDownloadInsideYield(DownloadItem item) {
        if (!downloadPlayWhileDownloadingEnabled || item == null || item.hlsDownload) return false;
        if (!"Video".equals(getDownloadCategory(item))) return false;
        if (!ProgressivePlaybackPolicy.supportsContainer(item.fileName, item.url, item.hlsDownload)) {
            return false;
        }
        if (item.publicUri != null && !item.publicUri.isEmpty()) return true;
        if (item.path == null || item.path.isEmpty()) return false;
        try {
            File file = new File(item.path);
            return file.exists() || "queued".equals(item.status) || "running".equals(item.status);
        } catch (Exception ignored) {
            return false;
        }
    }

    void playDownloadInsideYield(DownloadItem item) {
        if (item == null) return;
        if (!canPlayDownloadInsideYield(item)) {
            if (item.hlsDownload || looksLikeHlsDownload(item.url, item.fileName)) {
                QuietToast.makeText(this, "Video HLS dapat dibuka setelah proses penggabungan selesai",
                        QuietToast.LENGTH_LONG).show();
            } else {
                QuietToast.makeText(this, "Format ini belum mendukung putar sambil mengunduh",
                        QuietToast.LENGTH_SHORT).show();
            }
            return;
        }
        try {
            ProgressiveDownloadServer.PlaybackSession session =
                    ProgressiveDownloadServer.open(this, item);
            Intent intent = new Intent(this, ProgressiveVideoActivity.class);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_MEDIA_URL, session.mediaUrl);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_STATUS_URL, session.statusUrl);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_CLOSE_URL, session.closeUrl);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_TITLE, item.fileName);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_PRIVATE_SESSION,
                    dedicatedPrivateProfile || isCurrentPrivateTab());
            String originUrl = ProgressivePlaybackPolicy.selectOriginUrl(item.resolvedUrl, item.url);
            String playbackUserAgent = safeText(item.userAgent);
            if (playbackUserAgent.isEmpty()) {
                playbackUserAgent = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/120 Mobile Safari/537.36 YieldBrowser";
            }
            intent.putExtra(ProgressiveVideoActivity.EXTRA_ORIGIN_URL, originUrl);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_ORIGIN_USER_AGENT, playbackUserAgent);
            intent.putExtra(ProgressiveVideoActivity.EXTRA_ORIGIN_REFERER, safeText(item.referer));
            intent.putExtra(ProgressiveVideoActivity.EXTRA_ORIGIN_COOKIE,
                    buildAntiHotlinkCookieHeader(originUrl, item));
            startActivity(intent);
        } catch (Exception e) {
            QuietToast.makeText(this, "Player belum dapat dibuka: " + safeText(e.getMessage()),
                    QuietToast.LENGTH_LONG).show();
        }
    }

    void shareDownloadedFile(DownloadItem item) {
        try {
            Uri uri = getBestDownloadUri(item);
            if (uri == null) {
                QuietToast.makeText(this, "File tidak ditemukan", QuietToast.LENGTH_SHORT).show();
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
            QuietToast.makeText(this, "Gagal membagikan file", QuietToast.LENGTH_SHORT).show();
        }
    }

    void renameDownloadedFile(DownloadItem item) {
        if (item == null || getBestDownloadUri(item) == null) {
            QuietToast.makeText(this, "File tidak ditemukan", QuietToast.LENGTH_SHORT).show();
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
                    if (newName.isEmpty()) {
                        QuietToast.makeText(this, "Nama file kosong", QuietToast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        boolean renamed = false;
                        if (item.publicUri != null && !item.publicUri.isEmpty()) {
                            Uri uri = Uri.parse(item.publicUri);
                            try {
                                Uri renamedUri = DocumentsContract.renameDocument(
                                        getContentResolver(), uri, newName);
                                if (renamedUri != null) {
                                    item.publicUri = renamedUri.toString();
                                    renamed = true;
                                }
                            } catch (Exception ignored) {
                                ContentValues values = new ContentValues();
                                values.put(MediaStore.MediaColumns.DISPLAY_NAME, newName);
                                renamed = getContentResolver().update(uri, values, null, null) > 0;
                            }
                        }
                        if (!renamed && item.path != null && !item.path.isEmpty()) {
                            File currentFile = new File(item.path);
                            File parent = currentFile.getParentFile();
                            File newFile = parent == null ? null : new File(parent, newName);
                            if (newFile != null && !newFile.exists() && currentFile.renameTo(newFile)) {
                                item.path = newFile.getAbsolutePath();
                                renamed = true;
                            }
                        }
                        if (renamed) {
                            item.fileName = newName;
                            saveDownloadHistory();
                            renderDownloadList();
                            QuietToast.makeText(this, "Nama file diperbarui", QuietToast.LENGTH_SHORT).show();
                        } else {
                            QuietToast.makeText(this, "Gagal mengganti nama", QuietToast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        QuietToast.makeText(this, "Gagal mengganti nama", QuietToast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    void removeDownloadItem(DownloadItem item, boolean deleteFile) {
        if (item == null) return;
        item.runGeneration++;
        item.pauseRequested = true;
        item.status = "removed";
        stopActiveDownloadTransports(item);
        cancelDownloadNotification(item);

        synchronized (downloadItems) {
            downloadItems.remove(item);
        }
        if (deleteFile && item.publicUri != null && !item.publicUri.isEmpty()) {
            try { getContentResolver().delete(Uri.parse(item.publicUri), null, null); }
            catch (Exception ignored) {}
        }
        if (deleteFile && item.path != null) {
            try {
                File file = new File(item.path);
                if (file.exists()) file.delete();
            } catch (Exception ignored) {}
        }
        selectedDownloadIds.remove(item.id);
        saveDownloadHistory();
        renderDownloadList();
        updateDownloadKeepAliveState();
        mainHandler.post(this::pumpDownloadQueue);
        QuietToast.makeText(this, deleteFile ? "File + riwayat dihapus" : "Riwayat dihapus", QuietToast.LENGTH_SHORT).show();
    }

    Uri getBestDownloadUri(DownloadItem item) {
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

    void openDownloadedFile(DownloadItem item) {
        try {
            Uri uri = getBestDownloadUri(item);
            if (uri == null) {
                QuietToast.makeText(this, "File tidak ditemukan", QuietToast.LENGTH_SHORT).show();
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
            QuietToast.makeText(this, "Tidak ada aplikasi untuk membuka file ini", QuietToast.LENGTH_SHORT).show();
        }
    }

    void refreshDownloadPanel() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            renderDownloadList();
            return;
        }
        // Coalesce worker progress events so a fast connection or large-file copy cannot flood
        // the main thread with hundreds of redundant RecyclerView refresh callbacks.
        if (!downloadUiRefreshPosted.compareAndSet(false, true)) return;
        mainHandler.post(() -> {
            downloadUiRefreshPosted.set(false);
            renderDownloadList();
        });
    }

    String getCurrentPageForReferer() {
        try {
            if (webView != null && webView.getUrl() != null) return webView.getUrl();
        } catch (Exception ignored) {}
        try {
            String current = getEffectiveCurrentUrl();
            if (current != null) return current;
        } catch (Exception ignored) {}
        return "";
    }

    // ===== Download engine =====

    void beginDownloadFromWeb(String url, String contentDisposition, String mimeType, String userAgent) {
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        beginDownload(url, fileName, userAgent, getCurrentPageForReferer());
    }

    void beginDownload(String fileUrl, String guessedFileName, String userAgent, String referer) {
        try {
            ensureDownloadNotificationPermission();
            fileUrl = normalizeGoogleDriveDownloadUrl(fileUrl);
            String fileName = guessedFileName;
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = URLUtil.guessFileName(fileUrl, null, null);
            }
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "yield_download_" + System.currentTimeMillis() + ".bin";
            }

            File dir = getDownloadDirectory();
            if (!dir.exists()) dir.mkdirs();
            fileName = autoRenameDownloadFile(fileName, fileUrl, "");
            File out = uniqueFile(new File(dir, fileName));
            DownloadItem item = new DownloadItem(nextDownloadId++, fileUrl, out.getName(),
                    out.getAbsolutePath(), "queued", 0);
            item.engineInfo = "Mengecek koneksi";
            item.userAgent = userAgent == null ? "" : userAgent;
            item.referer = referer == null ? "" : referer;
            item.cookieHeader = buildAntiHotlinkCookieHeader(fileUrl, item);
            item.categoryHint = inferDownloadCategoryFromData(fileName, fileUrl, "");

            synchronized (downloadItems) {
                downloadItems.add(item);
            }
            saveDownloadHistory();
            refreshDownloadPanel();
            showDownloadNotification(item, "Masuk antrian", true);
            showDownloadStartedBanner(item);
            enqueueOrStartDownload(item, out);
        } catch (Exception e) {
            QuietToast.makeText(this, "Gagal memulai unduhan: " + e.getMessage(), QuietToast.LENGTH_SHORT).show();
        }
    }

    void showDownloadStartedBanner(DownloadItem item) {
        runOnUiThread(() -> {
            if (contentFrame == null || item == null) {
                QuietToast.makeText(this, "Unduhan dimulai", QuietToast.LENGTH_SHORT).show();
                return;
            }
            LinearLayout banner = new LinearLayout(this);
            banner.setOrientation(LinearLayout.HORIZONTAL);
            banner.setGravity(Gravity.CENTER_VERTICAL);
            banner.setPadding(dp(14), dp(10), dp(10), dp(10));
            banner.setBackground(roundRect(Color.parseColor("#242830"), dp(18), dp(1), COLOR_BORDER));
            banner.setAlpha(0f);
            banner.setTranslationY(dp(18));

            LinearLayout textWrap = new LinearLayout(this);
            textWrap.setOrientation(LinearLayout.VERTICAL);
            TextView title = new TextView(this);
            title.setText("Unduhan dimulai");
            title.setTextColor(Color.WHITE);
            title.setTextSize(14);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            textWrap.addView(title);
            TextView name = new TextView(this);
            name.setText(item.fileName);
            name.setTextColor(COLOR_SUBTEXT);
            name.setTextSize(12);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            textWrap.addView(name);
            banner.addView(textWrap, new LinearLayout.LayoutParams(0, -2, 1));

            TextView open = new TextView(this);
            open.setText("Lihat");
            open.setTextColor(COLOR_ACCENT);
            open.setTextSize(13);
            open.setTypeface(Typeface.DEFAULT_BOLD);
            open.setGravity(Gravity.CENTER);
            open.setPadding(dp(14), dp(8), dp(14), dp(8));
            open.setOnClickListener(v -> {
                try { contentFrame.removeView(banner); } catch (Exception ignored) {}
                showDownloadManager();
            });
            banner.addView(open, new LinearLayout.LayoutParams(-2, dp(42)));

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
            lp.setMargins(dp(14), dp(14), dp(14), dp(76));
            contentFrame.addView(banner, lp);
            banner.animate().alpha(1f).translationY(0f).setDuration(180L).start();
            mainHandler.postDelayed(() -> {
                if (banner.getParent() == null) return;
                banner.animate().alpha(0f).translationY(dp(14)).setDuration(180L)
                        .withEndAction(() -> {
                            try { contentFrame.removeView(banner); } catch (Exception ignored) {}
                        }).start();
            }, 4200L);
        });
    }

    void ensureDownloadNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (preferences.getBoolean("downloadNotificationPermissionAsked", false)) return;
        preferences.edit().putBoolean("downloadNotificationPermissionAsked", true).apply();
        try {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_DOWNLOAD_NOTIFICATIONS);
        } catch (Exception ignored) {
        }
    }

    File getDownloadDirectory() {
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

    File uniqueFile(File file) {
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
        int index = 1;
        File candidate;
        do {
            candidate = new File(parent, base + " (" + index + ")" + ext);
            index++;
        } while (candidate.exists());
        return candidate;
    }

    String getOriginFromUrl(String value) {
        try {
            if (value == null || value.isEmpty()) return "";
            Uri uri = Uri.parse(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) return "";
            int port = uri.getPort();
            return port > 0 ? scheme + "://" + host + ":" + port : scheme + "://" + host;
        } catch (Exception e) {
            return "";
        }
    }

    String getRootUrl(String value) {
        String origin = getOriginFromUrl(value);
        return origin.isEmpty() ? "" : origin + "/";
    }

    String getHostLower(String value) {
        return BrowserUtils.getHostLower(value);
    }

    boolean isGoogleDriveHost(String url) {
        String host = getHostLower(url);
        return host.equals("drive.google.com")
                || host.endsWith(".drive.google.com")
                || host.equals("drive.usercontent.google.com")
                || host.endsWith(".drive.usercontent.google.com")
                || host.equals("docs.google.com")
                || host.endsWith(".docs.google.com")
                || host.endsWith(".googleusercontent.com");
    }

    boolean isStableDownloadHost(String url) {
        return DownloadUrlPolicy.isStableDownloadHost(url);
    }

    boolean isTurboFriendlyHost(String url) {
        String host = getHostLower(url);
        if (host.isEmpty() || isStableDownloadHost(url) || isGoogleDriveHost(url)) return false;
        return host.contains("cdn")
                || host.contains("cloudfront.net")
                || host.contains("bunnycdn")
                || host.contains("r2.cloudflarestorage.com")
                || host.contains("backblazeb2.com")
                || host.contains("wasabisys.com")
                || host.contains("digitaloceanspaces.com")
                || host.contains("storage.googleapis.com")
                || host.contains("githubusercontent.com")
                || host.contains("github.com");
    }

    String normalizeGoogleDriveDownloadUrl(String value) {
        return DownloadUrlPolicy.normalizeGoogleDriveDownloadUrl(value);
    }

    boolean looksLikeArchiveOrApp(String fileName, String url, String contentType) {
        String name = (fileName == null ? "" : fileName).toLowerCase(Locale.US);
        String link = (url == null ? "" : url).toLowerCase(Locale.US);
        String type = (contentType == null ? "" : contentType).toLowerCase(Locale.US);
        return name.endsWith(".apk") || name.endsWith(".zip") || name.endsWith(".rar")
                || name.endsWith(".7z") || name.endsWith(".tar") || name.endsWith(".gz")
                || link.contains(".apk") || link.contains(".zip") || link.contains(".rar")
                || link.contains(".7z") || type.contains("zip") || type.contains("rar")
                || type.contains("apk") || type.contains("octet-stream");
    }

    // v0.9.92: ukuran buffer baca/tulis adaptif. Untuk file besar, buffer lebih besar mengurangi
    // jumlah operasi I/O ke flash sehingga throughput naik; untuk file kecil tetap kecil agar hemat
    // RAM (buffer dialokasikan per koneksi; maksimum 512KB x 4 koneksi = 2MB per unduhan).
    int chooseDownloadBufferSize(long totalBytes) {
        return BrowserUtils.chooseDownloadBufferSize(totalBytes);
    }

    int chooseSmartDownloadConnections(DownloadItem item, long totalBytes, String contentType) {
        String url = item == null ? "" : item.url;
        String name = item == null ? "" : item.fileName;
        int chosen;
        String reason;
        int score = 50;

        if (item != null && item.turboTargetConnections > 0
                && item.turboProfile != null && item.turboProfile.contains("fallback")) {
            chosen = item.turboTargetConnections;
            reason = item.turboProfile;
        } else if (!downloadDynamic4Connections) {
            chosen = DOWNLOAD_CONNECTIONS_STABLE;
            reason = "Smart normal";
        } else if (isGoogleDriveHost(url)) {
            if (totalBytes >= DOWNLOAD_BALANCED_UNKNOWN_FILE && item != null && item.turboRetryPenalty == 0) {
                chosen = DOWNLOAD_CONNECTIONS_BALANCED;
                reason = "Google Drive adaptive 3";
            } else {
                chosen = DOWNLOAD_CONNECTIONS_STABLE;
                reason = "Google Drive stable 2";
            }
        } else if (isStableDownloadHost(url)) {
            chosen = DOWNLOAD_STABLE_HOST_LIMIT;
            reason = "Stable host v3";
        } else if (totalBytes > 0 && totalBytes < 16L * 1024L * 1024L) {
            chosen = 1;
            reason = "Small file v3";
        } else {
            boolean video = looksLikeVideoDownload(url, name, contentType);
            boolean turboHost = isTurboFriendlyHost(url);
            boolean archiveOrApp = looksLikeArchiveOrApp(name, url, contentType);
            if (turboHost) score += 26;
            if (video && totalBytes >= DOWNLOAD_TURBO_MIN_LARGE_FILE) score += 35;
            else if (video) score += 18;
            if (totalBytes >= DOWNLOAD_TURBO_UNKNOWN_LARGE_FILE) score += 24;
            else if (totalBytes >= DOWNLOAD_BALANCED_UNKNOWN_FILE) score += 12;
            if (archiveOrApp && !turboHost) score -= 10;
            if (url != null && url.toLowerCase(Locale.US).contains("token=")) score -= 4;
            if (item != null) score -= Math.min(25, item.turboRetryPenalty * 8);

            if (video && totalBytes >= DOWNLOAD_TURBO_MIN_LARGE_FILE) {
                chosen = DOWNLOAD_CONNECTIONS_DYNAMIC_MAX;
                reason = "Turbo video >50MB v3";
            } else if (score >= DOWNLOAD_V3_SCORE_TURBO) {
                chosen = DOWNLOAD_CONNECTIONS_DYNAMIC_MAX;
                reason = "Turbo score " + score;
            } else if (score >= DOWNLOAD_V3_SCORE_BALANCED) {
                chosen = DOWNLOAD_CONNECTIONS_BALANCED;
                reason = "Balanced score " + score;
            } else {
                chosen = DOWNLOAD_CONNECTIONS_STABLE;
                reason = "Stable score " + score;
            }
        }

        if (item != null) {
            item.turboTargetConnections = chosen;
            item.turboProfile = reason;
            item.turboStabilityScore = 100;
            item.turboSlowSamples = 0;
            item.turboHealthySamples = 0;
            item.turboJitterScore = 0;
            item.turboPeakSpeedBytesPerSecond = 0;
        }
        return Math.max(1, Math.min(DOWNLOAD_CONNECTIONS_DYNAMIC_MAX, chosen));
    }

    String getTurboLabel(DownloadItem item, int connections) {
        String profile = item != null && item.turboProfile != null && !item.turboProfile.isEmpty()
                ? item.turboProfile : "Smart v3";
        if (connections >= 4) return "Turbo 4 koneksi • " + profile;
        if (connections == 3) return "Balanced 3 koneksi • " + profile;
        if (connections >= 2) return "Stable 2 koneksi • " + profile;
        return "Safe 1 koneksi • " + profile;
    }

    void registerDownloadConnection(DownloadItem item, HttpURLConnection connection) {
        if (item == null || connection == null) return;
        synchronized (item.activeConnections) {
            if (!item.activeConnections.contains(connection)) item.activeConnections.add(connection);
        }
    }

    void unregisterDownloadConnection(DownloadItem item, HttpURLConnection connection) {
        if (item == null || connection == null) return;
        synchronized (item.activeConnections) {
            item.activeConnections.remove(connection);
        }
    }

    void registerDownloadStream(DownloadItem item, InputStream stream) {
        if (item == null || stream == null) return;
        synchronized (item.activeStreams) {
            if (!item.activeStreams.contains(stream)) item.activeStreams.add(stream);
        }
    }

    void unregisterDownloadStream(DownloadItem item, InputStream stream) {
        if (item == null || stream == null) return;
        synchronized (item.activeStreams) {
            item.activeStreams.remove(stream);
        }
    }

    void stopActiveDownloadTransports(DownloadItem item) {
        if (item == null) return;
        ArrayList<InputStream> streams;
        ArrayList<HttpURLConnection> connections;
        synchronized (item.activeStreams) {
            streams = new ArrayList<>(item.activeStreams);
            item.activeStreams.clear();
        }
        synchronized (item.activeConnections) {
            connections = new ArrayList<>(item.activeConnections);
            item.activeConnections.clear();
        }
        for (InputStream stream : streams) {
            try { stream.close(); } catch (Exception ignored) {}
        }
        for (HttpURLConnection connection : connections) {
            try { connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    void resetTurboSampling(DownloadItem item) {
        if (item == null) return;
        item.turboAvgSpeedBytesPerSecond = 0;
        item.turboLastSampleBytes = item.downloadedBytes;
        item.turboLastSampleTimeMs = System.currentTimeMillis();
        item.turboSlowSamples = 0;
        item.turboHealthySamples = 0;
        item.turboJitterScore = 0;
        item.turboPeakSpeedBytesPerSecond = 0;
        item.turboLastPersistMs = 0;
        item.turboStabilityScore = 100;
    }

    void maybePersistDownloadProgress(DownloadItem item) {
        long now = System.currentTimeMillis();
        if (item != null && now - item.lastProgressPersistMs >= 1800L) {
            item.lastProgressPersistMs = now;
            saveDownloadHistory();
        }
    }

    void updateTurboPrediction(DownloadItem item, long currentBytes) {
        if (item == null) return;
        long now = System.currentTimeMillis();
        boolean sampled = false;
        synchronized (item.stateLock) {
            if (item.turboLastSampleTimeMs <= 0) {
                item.turboLastSampleTimeMs = now;
                item.turboLastSampleBytes = currentBytes;
                return;
            }
            long elapsed = now - item.turboLastSampleTimeMs;
            if (elapsed < 1500) return;
            long delta = Math.max(0, currentBytes - item.turboLastSampleBytes);
            double sample = (delta * 1000.0) / Math.max(1, elapsed);
            if (sample > item.turboPeakSpeedBytesPerSecond) item.turboPeakSpeedBytesPerSecond = sample;
            double previousAvg = item.turboAvgSpeedBytesPerSecond;
            item.turboAvgSpeedBytesPerSecond = previousAvg <= 0
                    ? sample : previousAvg * 0.72 + sample * 0.28;
            double ratio = item.turboAvgSpeedBytesPerSecond > 0
                    ? sample / Math.max(1.0, item.turboAvgSpeedBytesPerSecond) : 1.0;
            double jitter = previousAvg > 0
                    ? Math.abs(sample - previousAvg) / Math.max(1.0, previousAvg) : 0;
            item.turboJitterScore = item.turboJitterScore * 0.70
                    + Math.min(1.5, jitter) * 30.0;
            if (ratio < 0.30 || (delta == 0 && elapsed >= 3500)) {
                item.turboSlowSamples++;
                item.turboHealthySamples = 0;
            } else if (ratio >= 0.70) {
                item.turboHealthySamples++;
                if (item.turboSlowSamples > 0) item.turboSlowSamples--;
            }
            double penalty = 0;
            if (ratio < 0.45) penalty += 10;
            if (item.turboJitterScore > 18) penalty += 6;
            if (item.turboRetryPenalty > 0) penalty += Math.min(18, item.turboRetryPenalty * 6);
            item.turboStabilityScore = Math.max(0,
                    Math.min(100, item.turboStabilityScore + (ratio >= 0.70 ? 3 : 0) - penalty));
            item.turboLastSampleBytes = currentBytes;
            item.turboLastSampleTimeMs = now;
            sampled = true;
        }
        if (sampled) maybePersistDownloadProgress(item);
    }

    int getV3FallbackConnections(DownloadItem item) {
        if (item == null) return DOWNLOAD_CONNECTIONS_STABLE;
        if (item.connectionCount >= 4) {
            if (item.turboStabilityScore < 22 || item.turboRetryPenalty >= 2) return DOWNLOAD_CONNECTIONS_STABLE;
            return DOWNLOAD_CONNECTIONS_BALANCED;
        }
        if (item.connectionCount == 3) return DOWNLOAD_CONNECTIONS_STABLE;
        return 1;
    }

    boolean shouldFallbackTurboToStable(DownloadItem item) {
        return DownloadItemUtils.shouldFallbackTurboToStable(item);
    }

    String buildAntiHotlinkCookieHeader(String fileUrl, DownloadItem item) {
        try {
            LinkedHashSet<String> cookies = new LinkedHashSet<>();
            if (item != null && item.cookieHeader != null && !item.cookieHeader.trim().isEmpty()) {
                for (String part : item.cookieHeader.split(";")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) cookies.add(trimmed);
                }
            }
            String[] sources = new String[]{fileUrl, item != null ? item.referer : "",
                    getRootUrl(fileUrl), item != null ? getRootUrl(item.referer) : ""};
            for (String source : sources) {
                if (source == null || source.isEmpty()) continue;
                String cookie = CookieManager.getInstance().getCookie(source);
                if (cookie == null || cookie.trim().isEmpty()) continue;
                for (String part : cookie.split(";")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) cookies.add(trimmed);
                }
            }
            StringBuilder value = new StringBuilder();
            for (String cookie : cookies) {
                if (value.length() > 0) value.append("; ");
                value.append(cookie);
            }
            return value.toString();
        } catch (Exception e) {
            return "";
        }
    }

    HttpURLConnection openDownloadConnection(String url, DownloadItem item, String range) throws Exception {
        boolean primary = item != null && url != null && url.equals(item.url);
        String cached = primary && item.resolvedUrl != null ? item.resolvedUrl : "";
        Exception lastError = null;

        for (int attempt = 0; attempt < (cached.isEmpty() ? 1 : 2); attempt++) {
            String current = attempt == 0 && !cached.isEmpty() ? cached : url;
            for (int redirect = 0; redirect < 8; redirect++) {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(current).openConnection();
                    registerDownloadConnection(item, connection);
                    connection.setInstanceFollowRedirects(false);
                    if (range != null && !range.isEmpty()) connection.setRequestProperty("Range", range);
                    applyDownloadHeaders(connection, current, item, primary && range != null && !range.isEmpty());
                    connection.connect();
                    int code = connection.getResponseCode();
                    if (isRedirectCode(code)) {
                        String location = connection.getHeaderField("Location");
                        unregisterDownloadConnection(item, connection);
                        connection.disconnect();
                        if (location == null || location.isEmpty()) throw new Exception("Redirect tanpa lokasi");
                        current = new URL(new URL(current), location).toString();
                        continue;
                    }
                    if (attempt == 0 && !cached.isEmpty()
                            && (code == HttpURLConnection.HTTP_UNAUTHORIZED
                            || code == HttpURLConnection.HTTP_FORBIDDEN)) {
                        unregisterDownloadConnection(item, connection);
                        connection.disconnect();
                        item.resolvedUrl = "";
                        break;
                    }
                    if (primary && code < 400) item.resolvedUrl = current;
                    return connection;
                } catch (Exception e) {
                    lastError = e;
                    if (connection != null) {
                        unregisterDownloadConnection(item, connection);
                        try { connection.disconnect(); } catch (Exception ignored) {}
                    }
                    break;
                }
            }
        }
        throw lastError != null ? lastError : new Exception("Terlalu banyak redirect");
    }

    boolean isRedirectCode(int code) {
        return BrowserUtils.isRedirectCode(code);
    }

    void validateDownloadResponse(HttpURLConnection connection) throws Exception {
        int code = connection.getResponseCode();
        if (code >= 400) throw new Exception("Server menolak unduhan HTTP " + code);
        String contentType = connection.getContentType();
        String disposition = connection.getHeaderField("Content-Disposition");
        if (contentType != null && contentType.toLowerCase(Locale.US).contains("text/html")) {
            String lowerDisposition = disposition == null ? "" : disposition.toLowerCase(Locale.US);
            if (!lowerDisposition.contains("attachment") && !lowerDisposition.contains("filename=")) {
                throw new Exception("Link mengarah ke halaman HTML, bukan file. Klik tombol download asli.");
            }
        }
    }

    void applyDownloadHeaders(HttpURLConnection connection, String fileUrl,
                                      DownloadItem item, boolean applyIfRange) {
        connection.setConnectTimeout(DOWNLOAD_CONNECT_TIMEOUT);
        connection.setReadTimeout(DOWNLOAD_READ_TIMEOUT);
        connection.setUseCaches(false);
        String userAgent = item == null ? "" : item.userAgent;
        if (userAgent == null || userAgent.isEmpty()) {
            userAgent = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120 Mobile Safari/537.36 YieldBrowser";
        }
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setRequestProperty("Connection", "keep-alive");

        String referer = item == null ? "" : item.referer;
        if (referer != null && !referer.isEmpty()) connection.setRequestProperty("Referer", referer);
        if (!isGoogleDriveHost(fileUrl) && referer != null && !referer.isEmpty()
                && getHostLower(referer).equals(getHostLower(fileUrl))) {
            String origin = getOriginFromUrl(referer);
            if (!origin.isEmpty()) connection.setRequestProperty("Origin", origin);
        }
        if (applyIfRange && item != null) {
            if (item.etag != null && !item.etag.isEmpty()) connection.setRequestProperty("If-Range", item.etag);
            else if (item.lastModified != null && !item.lastModified.isEmpty()) {
                connection.setRequestProperty("If-Range", item.lastModified);
            }
        }
        String cookie = buildAntiHotlinkCookieHeader(fileUrl, item);
        if (!cookie.isEmpty()) connection.setRequestProperty("Cookie", cookie);
    }

    void captureRemoteIdentity(DownloadItem item, HttpURLConnection connection) throws Exception {
        if (item == null || connection == null) return;
        String newEtag = safeHeader(connection.getHeaderField("ETag"));
        String newLastModified = safeHeader(connection.getHeaderField("Last-Modified"));
        synchronized (item.stateLock) {
            if (!item.etag.isEmpty() && !newEtag.isEmpty() && !item.etag.equals(newEtag)) {
                throw new Exception("File di server berubah (ETag berbeda)");
            }
            if (item.etag.isEmpty() && !item.lastModified.isEmpty() && !newLastModified.isEmpty()
                    && !item.lastModified.equals(newLastModified)) {
                throw new Exception("File di server berubah (Last-Modified berbeda)");
            }
            if (item.etag.isEmpty() && !newEtag.isEmpty()) item.etag = newEtag;
            if (item.lastModified.isEmpty() && !newLastModified.isEmpty()) item.lastModified = newLastModified;
        }
    }

    String safeHeader(String value) {
        return value == null ? "" : value.trim();
    }

    boolean isProtocolOrIdentityFailure(String reason) {
        if (reason == null) return false;
        return reason.contains("Range")
                || reason.contains("Content-Range")
                || reason.contains("Panjang respons")
                || reason.contains("Ukuran file berubah")
                || reason.contains("File di server berubah");
    }

    boolean isPermanentDownloadError(String reason) {
        return DownloadUrlPolicy.isPermanentDownloadError(reason);
    }

    void startTwoConnectionDownload(DownloadItem item, File out) {
        if (item == null || out == null || "removed".equals(item.status)) return;
        if (downloadHlsEnabled && (item.hlsDownload || looksLikeHlsDownload(item.url, item.fileName))) {
            startHlsDownload(item, out);
            return;
        }
        boolean resume = out.exists() && item.downloadedBytes > 0 && "running".equals(item.status);
        if (resume && item.connectionCount == 1) {
            startSingleConnectionDownloadAsync(item, out);
        } else if (resume && item.connectionCount >= 3) {
            startDynamicMultiConnectionDownload(item, out);
        } else if (resume || !downloadDynamic4Connections) {
            startLegacyTwoConnectionDownload(item, out);
        } else {
            startDynamicMultiConnectionDownload(item, out);
        }
    }

    void startSingleConnectionDownloadAsync(DownloadItem item, File out) {
        if (item == null || out == null || "removed".equals(item.status)) return;
        final int generation = item.runGeneration;
        DOWNLOAD_IO_EXECUTOR.execute(() -> {
            if (!isCurrentDownloadRun(item, generation)) return;
            downloadSingle(item, out);
        });
    }

    boolean looksLikeHlsDownload(String url, String fileName) {
        return DownloadUrlPolicy.looksLikeHlsDownload(url, fileName);
    }

    boolean looksLikeVideoDownload(String url, String fileName, String contentType) {
        return BrowserUtils.looksLikeVideoDownload(url, fileName, contentType);
    }

    String autoRenameDownloadFile(String fileName, String url, String contentType) {
        String name = fileName == null ? "" : fileName.trim();
        if (name.isEmpty()) name = "yield_download_" + System.currentTimeMillis();
        if (looksLikeHlsDownload(url, name)) {
            if (name.toLowerCase(Locale.US).endsWith(".m3u8")) name = name.substring(0, name.length() - 5) + ".ts";
            else if (!name.toLowerCase(Locale.US).endsWith(".ts")) name += ".ts";
        }
        if (looksLikeVideoDownload(url, name, contentType)) {
            name = name.replaceAll("(?i)videoplayback(\\.[a-z0-9]+)?$", "yield_video$1");
            if (!name.contains(".")) name += ".mp4";
        }
        return name.replace("/", "_").replace("\\", "_").replace(":", "_");
    }

    void setDynamicPartState(DownloadItem item, int part, long start, long end, long done) {
        synchronized (item.stateLock) {
            if (part == 1) { item.part1Start = start; item.part1End = end; item.part1Done = done; }
            else if (part == 2) { item.part2Start = start; item.part2End = end; item.part2Done = done; }
            else if (part == 3) { item.part3Start = start; item.part3End = end; item.part3Done = done; }
            else if (part == 4) { item.part4Start = start; item.part4End = end; item.part4Done = done; }
        }
    }

    long getDynamicPartStart(DownloadItem item, int part) {
        return BrowserUtils.getDynamicPartStart(item, part);
    }

    long getDynamicPartEnd(DownloadItem item, int part) {
        return BrowserUtils.getDynamicPartEnd(item, part);
    }

    long getDynamicPartDone(DownloadItem item, int part) {
        return BrowserUtils.getDynamicPartDone(item, part);
    }

    void addDynamicPartDone(DownloadItem item, int part, long delta) {
        synchronized (item.stateLock) {
            long length = DownloadProtocol.expectedLength(getDynamicPartStart(item, part), getDynamicPartEnd(item, part));
            long value = Math.min(length, getDynamicPartDone(item, part) + delta);
            setDynamicPartState(item, part, getDynamicPartStart(item, part), getDynamicPartEnd(item, part), value);
        }
    }

    void clearDynamicPartState(DownloadItem item) {
        if (item == null) return;
        synchronized (item.stateLock) {
            item.part1Start = item.part1End = item.part1Done = 0;
            item.part2Start = item.part2End = item.part2Done = 0;
            item.part3Start = item.part3End = item.part3Done = 0;
            item.part4Start = item.part4End = item.part4Done = 0;
        }
    }

    boolean hasDynamicResumeState(DownloadItem item, int connections, long total, File file) {
        if (item == null || connections < 3 || total <= 0 || file == null || file.length() != total) return false;
        if (item.connectionCount != connections || item.totalBytes != total) return false;
        long sum = 0;
        for (int part = 1; part <= connections; part++) {
            long start = getDynamicPartStart(item, part);
            long end = getDynamicPartEnd(item, part);
            long done = getDynamicPartDone(item, part);
            long length = DownloadProtocol.expectedLength(start, end);
            if (length <= 0 || done < 0 || done > length) return false;
            sum += done;
        }
        return sum > 0;
    }

    void initializeDynamicParts(DownloadItem item, int connections, long total) {
        clearDynamicPartState(item);
        long base = total / connections;
        long remainder = total % connections;
        long start = 0;
        for (int part = 1; part <= connections; part++) {
            long length = base + (part <= remainder ? 1 : 0);
            long end = start + length - 1;
            setDynamicPartState(item, part, start, end, 0);
            start = end + 1;
        }
    }

    boolean verifyMultipartComplete(DownloadItem item, int connections, long total, File file) {
        if (file == null || !file.exists() || file.length() != total) return false;
        long sum = 0;
        for (int part = 1; part <= connections; part++) {
            long expected = DownloadProtocol.expectedLength(getDynamicPartStart(item, part), getDynamicPartEnd(item, part));
            long done = getDynamicPartDone(item, part);
            if (expected <= 0 || done != expected) return false;
            sum += done;
        }
        return sum == total && item.downloadedBytes == total;
    }

    void resetForCleanRestart(DownloadItem item, File out) {
        stopActiveDownloadTransports(item);
        clearDynamicPartState(item);
        item.downloadedBytes = 0;
        item.totalBytes = 0;
        item.progress = 0;
        item.connectionCount = 0;
        item.activeConnectionLimit = 0;
        item.etag = "";
        item.lastModified = "";
        item.resolvedUrl = "";
        item.rateLimiter.reset();
        if (out != null && out.exists()) out.delete();
    }

    void startDynamicMultiConnectionDownload(DownloadItem item, File out) {
        final int generation = item.runGeneration;
        item.status = "running";
        item.pauseRequested = false;
        item.engineInfo = item.downloadedBytes > 0 ? "Mengecek resume multipart" : "Mengecek koneksi adaptif";
        refreshDownloadPanel();

        new Thread(() -> {
            final File[] outRef = new File[]{out};
            HttpURLConnection probe = null;
            try {
                if (!isCurrentDownloadRun(item, generation)) return;
                boolean resumeCandidate = out.exists() && item.downloadedBytes > 0 && item.connectionCount >= 3;
                probe = openDownloadConnection(item.url, item, "bytes=0-0");
                validateDownloadResponse(probe);
                DownloadProtocol.RangeInfo probeRange = DownloadProtocol.requireRange(probe, 0, 0, -1);
                if (probeRange.total <= 1) throw new Exception("Ukuran multipart tidak tersedia");
                captureRemoteIdentity(item, probe);
                long total = probeRange.total;
                String contentType = probe.getContentType();
                String disposition = probe.getHeaderField("Content-Disposition");
                if (!resumeCandidate) {
                    String betterName = autoRenameDownloadFile(
                            URLUtil.guessFileName(item.url, disposition, contentType), item.url, contentType);
                    if (betterName != null && !betterName.isEmpty() && !betterName.equals(item.fileName)) {
                        File renamed = uniqueFile(new File(outRef[0].getParentFile(), betterName));
                        item.fileName = renamed.getName();
                        item.path = renamed.getAbsolutePath();
                        outRef[0] = renamed;
                    }
                }
                item.categoryHint = inferDownloadCategoryFromData(item.fileName, item.url, contentType);
                unregisterDownloadConnection(item, probe);
                probe.disconnect();
                probe = null;

                int smartConnections = chooseSmartDownloadConnections(item, total, contentType);
                if (smartConnections <= 2 && !resumeCandidate) {
                    if (isCurrentDownloadRun(item, generation)) startLegacyTwoConnectionDownload(item, outRef[0]);
                    return;
                }
                int connections = resumeCandidate ? item.connectionCount : smartConnections;
                if (connections < 3) {
                    if (isCurrentDownloadRun(item, generation)) startLegacyTwoConnectionDownload(item, outRef[0]);
                    return;
                }
                int workerSlots = Math.max(1, Math.min(connections, smartConnections));
                boolean canResume = resumeCandidate
                        && hasDynamicResumeState(item, connections, total, outRef[0]);
                if (!canResume) {
                    initializeDynamicParts(item, connections, total);
                    item.downloadedBytes = 0;
                    item.progress = 0;
                    RandomAccessFile random = new RandomAccessFile(outRef[0], "rw");
                    random.setLength(total);
                    random.close();
                } else {
                    long resumed = 0;
                    for (int part = 1; part <= connections; part++) resumed += getDynamicPartDone(item, part);
                    item.downloadedBytes = resumed;
                    item.progress = (int) Math.min(99, resumed * 100 / Math.max(1, total));
                }
                item.totalBytes = total;
                item.connectionCount = connections;
                item.activeConnectionLimit = workerSlots;
                item.engineInfo = (canResume ? "Resume " : "") + getTurboLabel(item, workerSlots);
                item.failReason = "";
                item.rateLimiter.reset();
                resetTurboSampling(item);
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, item.engineInfo, true);

                AtomicLong done = new AtomicLong(item.downloadedBytes);
                AtomicBoolean ok = new AtomicBoolean(true);
                String[] workerError = new String[]{""};
                Semaphore limiter = new Semaphore(workerSlots);
                ArrayList<Thread> workers = new ArrayList<>();
                for (int part = 1; part <= connections; part++) {
                    final int partIndex = part;
                    final long rangeStart = getDynamicPartStart(item, part) + getDynamicPartDone(item, part);
                    final long rangeEnd = getDynamicPartEnd(item, part);
                    if (rangeStart > rangeEnd) continue;
                    Thread worker = new Thread(() -> {
                        boolean acquired = false;
                        try {
                            limiter.acquire();
                            acquired = true;
                            downloadRangeDynamic(item, outRef[0], rangeStart, rangeEnd, done, total,
                                    ok, workerError, partIndex, workerSlots, generation);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            ok.set(false);
                            setWorkerError(workerError, "Worker multipart terinterupsi");
                        } finally {
                            if (acquired) limiter.release();
                        }
                    }, "Yield-Part-" + partIndex);
                    workers.add(worker);
                    worker.start();
                }
                for (Thread worker : workers) worker.join();
                if (!isCurrentDownloadRun(item, generation)) return;
                if (item.pauseRequested || "paused".equals(item.status)) {
                    item.status = "paused";
                    resetDownloadSpeedState(item);
                    item.engineInfo = getTurboLabel(item, workerSlots) + " • dijeda";
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    showDownloadNotification(item, "Unduhan dijeda", false);
                    updateDownloadKeepAliveState();
                    return;
                }
                if (ok.get() && verifyMultipartComplete(item, connections, total, outRef[0])) {
                    completeDownload(item);
                    return;
                }

                String reason = getWorkerError(workerError, "Multipart tidak lengkap");
                boolean protocolFailure = isProtocolOrIdentityFailure(reason);
                if (protocolFailure || shouldFallbackTurboToStable(item)) {
                    item.turboTargetConnections = Math.max(1, getV3FallbackConnections(item));
                    item.turboProfile = item.turboTargetConnections >= 2
                            ? "auto fallback stable v3" : "auto fallback safe v3";
                    resetForCleanRestart(item, outRef[0]);
                    item.status = "running";
                    if (item.turboTargetConnections >= 2) startLegacyTwoConnectionDownload(item, outRef[0]);
                    else downloadSingle(item, outRef[0]);
                } else {
                    failDownload(item, reason);
                }
            } catch (Exception e) {
                if (probe != null) {
                    unregisterDownloadConnection(item, probe);
                    try { probe.disconnect(); } catch (Exception ignored) {}
                }
                if (!isCurrentDownloadRun(item, generation)) return;
                if (item.pauseRequested || "paused".equals(item.status)) return;
                boolean resume = outRef[0].exists() && item.downloadedBytes > 0;
                if (!resume || isProtocolOrIdentityFailure(e.getMessage())) {
                    resetForCleanRestart(item, outRef[0]);
                    item.status = "running";
                    startLegacyTwoConnectionDownload(item, outRef[0]);
                } else {
                    failDownload(item, e.getMessage());
                }
            }
        }, "Yield-Dynamic-Probe").start();
    }

    void downloadRangeDynamic(DownloadItem item, File out, long start, long end,
                                      AtomicLong done, long total, AtomicBoolean ok,
                                      String[] workerError, int partIndex, int connections,
                                      int generation) {
        if (start > end || !isCurrentDownloadRun(item, generation)) return;
        HttpURLConnection connection = null;
        InputStream input = null;
        RandomAccessFile random = null;
        long expected = DownloadProtocol.expectedLength(start, end);
        long written = 0;
        try {
            connection = openDownloadConnection(item.url, item, "bytes=" + start + "-" + end);
            validateDownloadResponse(connection);
            DownloadProtocol.requireRange(connection, start, end, total);
            captureRemoteIdentity(item, connection);
            input = connection.getInputStream();
            registerDownloadStream(item, input);
            random = new RandomAccessFile(out, "rw");
            random.seek(start);
            byte[] buffer = new byte[chooseDownloadBufferSize(item.totalBytes)];
            while (written < expected) {
                if (!isCurrentDownloadRun(item, generation)
                        || item.pauseRequested || "paused".equals(item.status)) return;
                int request = (int) Math.min(buffer.length, expected - written);
                int length = input.read(buffer, 0, request);
                if (length < 0) break;
                random.write(buffer, 0, length);
                item.rateLimiter.acquire(length, downloadSpeedLimitKBps);
                written += length;
                addDynamicPartDone(item, partIndex, length);
                long current = done.addAndGet(length);
                item.downloadedBytes = current;
                updateDownloadSpeed(item, current);
                updateTurboPrediction(item, current);
                int percent = (int) Math.min(99, current * 100 / Math.max(1, total));
                if (percent != item.progress) {
                    item.progress = percent;
                    if (percent % 2 == 0 || percent >= 99) {
                        refreshDownloadPanel();
                        maybePersistDownloadProgress(item);
                        showDownloadNotification(item, getConnectionLabel(item) + " • " + percent
                                + "% • " + readableSpeed(item.speedBytesPerSecond), true);
                    }
                }
            }
            if (written != expected) throw new Exception("Range terputus: " + written + "/" + expected);
        } catch (Exception e) {
            if (isCurrentDownloadRun(item, generation)
                    && !item.pauseRequested && !"paused".equals(item.status)) {
                ok.set(false);
                setWorkerError(workerError, e.getMessage());
            }
        } finally {
            try { if (random != null) random.close(); } catch (Exception ignored) {}
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            unregisterDownloadStream(item, input);
            unregisterDownloadConnection(item, connection);
            try { if (connection != null) connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    void setWorkerError(String[] holder, String reason) {
        synchronized (holder) {
            if (holder[0] == null || holder[0].isEmpty()) {
                holder[0] = reason == null || reason.isEmpty() ? "Koneksi terputus" : reason;
            }
        }
    }

    String getWorkerError(String[] holder, String fallback) {
        synchronized (holder) {
            return holder[0] == null || holder[0].isEmpty() ? fallback : holder[0];
        }
    }

    void startLegacyTwoConnectionDownload(DownloadItem item, File out) {
        final int generation = item.runGeneration;
        final boolean resumeAttempt = out.exists() && item.downloadedBytes > 0
                && item.connectionCount == DOWNLOAD_CONNECTIONS_STABLE;
        item.status = "running";
        item.pauseRequested = false;
        item.engineInfo = "Mengecek koneksi Range";
        refreshDownloadPanel();

        new Thread(() -> {
            final File[] outRef = new File[]{out};
            HttpURLConnection probe = null;
            try {
                if (!isCurrentDownloadRun(item, generation)) return;
                long previousTotal = item.totalBytes;
                probe = openDownloadConnection(item.url, item, "bytes=0-0");
                validateDownloadResponse(probe);
                DownloadProtocol.RangeInfo range = DownloadProtocol.requireRange(probe, 0, 0, -1);
                captureRemoteIdentity(item, probe);
                long total = range.total;
                String contentType = probe.getContentType();
                String disposition = probe.getHeaderField("Content-Disposition");
                if (!resumeAttempt) {
                    String betterName = autoRenameDownloadFile(
                            URLUtil.guessFileName(item.url, disposition, contentType), item.url, contentType);
                    if (betterName != null && !betterName.isEmpty() && !betterName.equals(item.fileName)) {
                        File renamed = uniqueFile(new File(outRef[0].getParentFile(), betterName));
                        item.fileName = renamed.getName();
                        item.path = renamed.getAbsolutePath();
                        outRef[0] = renamed;
                    }
                }
                item.categoryHint = inferDownloadCategoryFromData(item.fileName, item.url, contentType);
                int smart = item.turboTargetConnections > 0 ? item.turboTargetConnections
                        : chooseSmartDownloadConnections(item, total, contentType);
                unregisterDownloadConnection(item, probe);
                probe.disconnect();
                probe = null;

                if (total <= 1 || smart < 2) {
                    if (resumeAttempt) resetForCleanRestart(item, outRef[0]);
                    item.connectionCount = 1;
                    item.activeConnectionLimit = 1;
                    item.engineInfo = getTurboLabel(item, 1);
                    downloadSingle(item, outRef[0]);
                    return;
                }

                long firstEnd = total / 2L - 1L;
                long secondStart = firstEnd + 1L;
                boolean canResume = resumeAttempt && previousTotal == total
                        && outRef[0].length() == total
                        && item.part1Start == 0 && item.part1End == firstEnd
                        && item.part2Start == secondStart && item.part2End == total - 1
                        && item.part1Done >= 0 && item.part1Done <= firstEnd + 1
                        && item.part2Done >= 0 && item.part2Done <= total - secondStart;
                if (!canResume) {
                    item.part1Start = 0;
                    item.part1End = firstEnd;
                    item.part1Done = 0;
                    item.part2Start = secondStart;
                    item.part2End = total - 1;
                    item.part2Done = 0;
                    item.downloadedBytes = 0;
                    item.progress = 0;
                    RandomAccessFile random = new RandomAccessFile(outRef[0], "rw");
                    random.setLength(total);
                    random.close();
                } else {
                    item.downloadedBytes = item.part1Done + item.part2Done;
                    item.progress = (int) Math.min(99, item.downloadedBytes * 100 / Math.max(1, total));
                }
                item.totalBytes = total;
                item.connectionCount = 2;
                item.activeConnectionLimit = 2;
                item.engineInfo = (canResume ? "Resume " : "") + getTurboLabel(item, 2);
                item.failReason = "";
                item.rateLimiter.reset();
                resetTurboSampling(item);
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, item.engineInfo, true);

                AtomicLong done = new AtomicLong(item.downloadedBytes);
                AtomicBoolean ok = new AtomicBoolean(true);
                String[] workerError = new String[]{""};
                long range1Start = item.part1Start + item.part1Done;
                long range2Start = item.part2Start + item.part2Done;
                Thread first = new Thread(() -> downloadRange(item, outRef[0], range1Start,
                        item.part1End, done, total, ok, workerError, 1, generation), "Yield-Part-1");
                Thread second = new Thread(() -> downloadRange(item, outRef[0], range2Start,
                        item.part2End, done, total, ok, workerError, 2, generation), "Yield-Part-2");
                first.start();
                second.start();
                first.join();
                second.join();
                if (!isCurrentDownloadRun(item, generation)) return;
                if (item.pauseRequested || "paused".equals(item.status)) {
                    item.status = "paused";
                    resetDownloadSpeedState(item);
                    item.engineInfo = "Stable 2 koneksi • dijeda";
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    showDownloadNotification(item, "Unduhan dijeda", false);
                    updateDownloadKeepAliveState();
                    return;
                }
                if (ok.get() && verifyMultipartComplete(item, 2, total, outRef[0])) {
                    completeDownload(item);
                    return;
                }
                String reason = getWorkerError(workerError, "Download 2 koneksi tidak lengkap");
                boolean protocolFailure = isProtocolOrIdentityFailure(reason);
                if (protocolFailure) {
                    resetForCleanRestart(item, outRef[0]);
                    item.status = "running";
                    item.connectionCount = 1;
                    item.activeConnectionLimit = 1;
                    item.turboTargetConnections = 1;
                    item.turboProfile = "auto fallback safe v3";
                    downloadSingle(item, outRef[0]);
                } else {
                    failDownload(item, reason);
                }
            } catch (Exception e) {
                if (probe != null) {
                    unregisterDownloadConnection(item, probe);
                    try { probe.disconnect(); } catch (Exception ignored) {}
                }
                if (!isCurrentDownloadRun(item, generation)) return;
                if (item.pauseRequested || "paused".equals(item.status)) return;
                if (!resumeAttempt || isProtocolOrIdentityFailure(e.getMessage())) {
                    resetForCleanRestart(item, outRef[0]);
                    item.status = "running";
                    item.connectionCount = 1;
                    item.activeConnectionLimit = 1;
                    item.turboTargetConnections = 1;
                    item.turboProfile = "auto fallback safe v3";
                    downloadSingle(item, outRef[0]);
                } else {
                    failDownload(item, e.getMessage());
                }
            }
        }, "Yield-Stable-Probe").start();
    }

    void downloadRange(DownloadItem item, File out, long start, long end,
                               AtomicLong done, long total, AtomicBoolean ok,
                               String[] workerError, int partIndex, int generation) {
        if (start > end || !isCurrentDownloadRun(item, generation)) return;
        HttpURLConnection connection = null;
        InputStream input = null;
        RandomAccessFile random = null;
        long expected = DownloadProtocol.expectedLength(start, end);
        long written = 0;
        try {
            connection = openDownloadConnection(item.url, item, "bytes=" + start + "-" + end);
            validateDownloadResponse(connection);
            DownloadProtocol.requireRange(connection, start, end, total);
            captureRemoteIdentity(item, connection);
            input = connection.getInputStream();
            registerDownloadStream(item, input);
            random = new RandomAccessFile(out, "rw");
            random.seek(start);
            byte[] buffer = new byte[chooseDownloadBufferSize(item.totalBytes)];
            while (written < expected) {
                if (!isCurrentDownloadRun(item, generation)
                        || item.pauseRequested || "paused".equals(item.status)) return;
                int request = (int) Math.min(buffer.length, expected - written);
                int length = input.read(buffer, 0, request);
                if (length < 0) break;
                random.write(buffer, 0, length);
                item.rateLimiter.acquire(length, downloadSpeedLimitKBps);
                written += length;
                synchronized (item.stateLock) {
                    if (partIndex == 1) item.part1Done = Math.min(item.part1End - item.part1Start + 1,
                            item.part1Done + length);
                    else item.part2Done = Math.min(item.part2End - item.part2Start + 1,
                            item.part2Done + length);
                }
                long current = done.addAndGet(length);
                item.downloadedBytes = current;
                updateDownloadSpeed(item, current);
                updateTurboPrediction(item, current);
                int percent = (int) Math.min(99, current * 100 / Math.max(1, total));
                if (percent != item.progress) {
                    item.progress = percent;
                    if (percent % 2 == 0 || percent >= 99) {
                        refreshDownloadPanel();
                        maybePersistDownloadProgress(item);
                        showDownloadNotification(item, "Stable 2 koneksi • " + percent
                                + "% • " + readableSpeed(item.speedBytesPerSecond), true);
                    }
                }
            }
            if (written != expected) throw new Exception("Range terputus: " + written + "/" + expected);
        } catch (Exception e) {
            if (isCurrentDownloadRun(item, generation)
                    && !item.pauseRequested && !"paused".equals(item.status)) {
                ok.set(false);
                setWorkerError(workerError, e.getMessage());
            }
        } finally {
            try { if (random != null) random.close(); } catch (Exception ignored) {}
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            unregisterDownloadStream(item, input);
            unregisterDownloadConnection(item, connection);
            try { if (connection != null) connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    void startHlsDownload(DownloadItem item, File out) {
        final int generation = item.runGeneration;
        item.status = "running";
        item.pauseRequested = false;
        item.hlsDownload = true;
        item.connectionCount = 1;
        item.activeConnectionLimit = 1;
        item.engineInfo = "HLS/m3u8";
        item.categoryHint = "Video";
        refreshDownloadPanel();

        new Thread(() -> {
            File actualOutput = out;
            try {
                HlsPlaylistParser.Playlist playlist = resolveHlsPlaylist(item.url, item, generation, 0);
                if (!isCurrentDownloadRun(item, generation)) return;
                if (playlist.unsupportedEncryption) {
                    throw new Exception("Metode enkripsi HLS tidak didukung; download dihentikan agar file tidak korup");
                }
                if (playlist.segments.isEmpty()) throw new Exception("Playlist HLS kosong/tidak didukung");

                boolean fragmentedMp4 = playlist.initMap != null;
                String expectedExtension = fragmentedMp4 ? ".mp4" : ".ts";
                if (item.hlsCompletedSegments == 0 && item.hlsOutputBytes == 0
                        && !item.fileName.toLowerCase(Locale.US).endsWith(expectedExtension)) {
                    String base = item.fileName.replaceAll("(?i)\\.(m3u8|ts|mp4)$", "");
                    File renamed = uniqueFile(new File(out.getParentFile(), base + expectedExtension));
                    item.fileName = renamed.getName();
                    item.path = renamed.getAbsolutePath();
                    actualOutput = renamed;
                } else {
                    actualOutput = new File(item.path);
                }

                String fingerprint = buildHlsFingerprint(playlist);
                Map<String, byte[]> hlsKeyCache = new HashMap<>();
                boolean resumeValid = actualOutput.exists()
                        && fingerprint.equals(item.hlsPlaylistFingerprint)
                        && item.hlsCompletedSegments >= 0
                        && item.hlsCompletedSegments <= playlist.segments.size()
                        && item.hlsOutputBytes == actualOutput.length();
                if (!resumeValid) {
                    if (actualOutput.exists()) actualOutput.delete();
                    item.hlsCompletedSegments = 0;
                    item.hlsOutputBytes = 0;
                    item.hlsInitMapWritten = false;
                    item.hlsPlaylistFingerprint = fingerprint;
                }
                item.totalBytes = playlist.segments.size();
                item.downloadedBytes = item.hlsCompletedSegments;
                item.progress = (int) Math.min(99,
                        item.hlsCompletedSegments * 100L / Math.max(1, playlist.segments.size()));
                item.rateLimiter.reset();
                resetTurboSampling(item);
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, "HLS • " + playlist.segments.size() + " segmen", true);

                if (playlist.initMap != null && !item.hlsInitMapWritten) {
                    long mapStartLength = actualOutput.exists() ? actualOutput.length() : 0;
                    try {
                        appendHlsResource(item, actualOutput, playlist.initMap, generation, hlsKeyCache);
                    } catch (Exception e) {
                        try {
                            RandomAccessFile rollback = new RandomAccessFile(actualOutput, "rw");
                            rollback.setLength(mapStartLength);
                            rollback.close();
                            item.hlsOutputBytes = mapStartLength;
                        } catch (Exception ignored) {}
                        throw e;
                    }
                    item.hlsInitMapWritten = true;
                    item.hlsOutputBytes = actualOutput.length();
                    saveDownloadHistory();
                }

                for (int index = item.hlsCompletedSegments; index < playlist.segments.size(); index++) {
                    if (!isCurrentDownloadRun(item, generation)
                            || item.pauseRequested || "paused".equals(item.status)) break;
                    long segmentStartLength = actualOutput.exists() ? actualOutput.length() : 0;
                    try {
                        appendHlsResource(item, actualOutput, playlist.segments.get(index), generation, hlsKeyCache);
                    } catch (Exception e) {
                        try {
                            RandomAccessFile rollback = new RandomAccessFile(actualOutput, "rw");
                            rollback.setLength(segmentStartLength);
                            rollback.close();
                        } catch (Exception ignored) {}
                        throw e;
                    }
                    item.hlsCompletedSegments = index + 1;
                    item.hlsOutputBytes = actualOutput.length();
                    item.downloadedBytes = item.hlsCompletedSegments;
                    item.progress = (int) Math.min(99,
                            item.hlsCompletedSegments * 100L / Math.max(1, playlist.segments.size()));
                    updateDownloadSpeed(item, item.hlsOutputBytes);
                    refreshDownloadPanel();
                    maybePersistDownloadProgress(item);
                    if (index % 3 == 0 || index == playlist.segments.size() - 1) {
                        showDownloadNotification(item, "HLS • " + item.progress + "% • "
                                + readableSpeed(item.speedBytesPerSecond), true);
                    }
                }
                if (!isCurrentDownloadRun(item, generation)) return;
                if (item.pauseRequested || "paused".equals(item.status)) {
                    item.status = "paused";
                    resetDownloadSpeedState(item);
                    item.engineInfo = "HLS • dijeda pada segmen " + item.hlsCompletedSegments;
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    showDownloadNotification(item, "HLS dijeda", false);
                    updateDownloadKeepAliveState();
                } else if (item.hlsCompletedSegments == playlist.segments.size()
                        && actualOutput.exists() && actualOutput.length() > 0) {
                    item.hlsOutputBytes = actualOutput.length();
                    completeDownload(item);
                } else {
                    failDownload(item, "HLS tidak lengkap");
                }
            } catch (Exception e) {
                if (!isCurrentDownloadRun(item, generation)) return;
                if (item.pauseRequested || "paused".equals(item.status)) {
                    item.status = "paused";
                    resetDownloadSpeedState(item);
                    item.engineInfo = "HLS • dijeda";
                    saveDownloadHistory();
                    refreshDownloadPanel();
                    showDownloadNotification(item, "HLS dijeda", false);
                    updateDownloadKeepAliveState();
                } else {
                    failDownload(item, e.getMessage());
                }
            }
        }, "Yield-HLS").start();
    }

    HlsPlaylistParser.Playlist resolveHlsPlaylist(String playlistUrl, DownloadItem item,
                                                          int generation, int depth) throws Exception {
        if (depth > 5) throw new Exception("Terlalu banyak level playlist HLS");
        String text = readUrlText(playlistUrl, item, generation);
        HlsPlaylistParser.Playlist playlist = HlsPlaylistParser.parse(playlistUrl, text);
        if (!playlist.variants.isEmpty()) {
            return resolveHlsPlaylist(playlist.variants.get(0).url, item, generation, depth + 1);
        }
        return playlist;
    }

    String readUrlText(String url, DownloadItem item, int generation) throws Exception {
        HttpURLConnection connection = null;
        InputStream input = null;
        BufferedReader reader = null;
        try {
            if (!isCurrentDownloadRun(item, generation)) throw new Exception("Sesi download berubah");
            connection = openDownloadConnection(url, item, "");
            validateDownloadResponse(connection);
            input = connection.getInputStream();
            registerDownloadStream(item, input);
            reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!isCurrentDownloadRun(item, generation)
                        || item.pauseRequested || "paused".equals(item.status)) {
                    throw new Exception("Sesi download berubah");
                }
                text.append(line).append('\n');
            }
            return text.toString();
        } finally {
            try { if (reader != null) reader.close(); } catch (Exception ignored) {}
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            unregisterDownloadStream(item, input);
            unregisterDownloadConnection(item, connection);
            try { if (connection != null) connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    void appendHlsResource(DownloadItem item, File output,
                                   HlsPlaylistParser.Resource resource, int generation,
                                   Map<String, byte[]> keyCache) throws Exception {
        HttpURLConnection connection = null;
        InputStream input = null;
        RandomAccessFile random = null;
        long expected = -1;
        long networkBytes = 0;
        try {
            String rangeHeader = "";
            if (resource.range != null) {
                if (resource.key != null) {
                    throw new Exception("AES-128 HLS dengan byte-range belum didukung secara aman");
                }
                rangeHeader = "bytes=" + resource.range.start + "-" + resource.range.end;
                expected = resource.range.end - resource.range.start + 1L;
            }
            connection = openDownloadConnection(resource.url, item, rangeHeader);
            validateDownloadResponse(connection);
            if (resource.range != null) {
                DownloadProtocol.requireRange(connection, resource.range.start, resource.range.end, -1);
            }
            input = connection.getInputStream();
            registerDownloadStream(item, input);
            random = new RandomAccessFile(output, "rw");
            random.seek(random.length());

            if (resource.key != null) {
                byte[] encrypted = readHlsResourceBytes(item, input, expected, generation);
                networkBytes = encrypted.length;
                byte[] key = getHlsKeyBytes(item, resource.key.url, generation, keyCache);
                byte[] plain = HlsAes128.decrypt(encrypted, key,
                        resource.key.explicitIv, resource.sequence);
                random.write(plain);
                item.hlsOutputBytes = random.length();
                updateDownloadSpeed(item, item.hlsOutputBytes);
            } else {
                byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
                while (expected < 0 || networkBytes < expected) {
                    if (!isCurrentDownloadRun(item, generation)
                            || item.pauseRequested || "paused".equals(item.status)) {
                        throw new Exception("Sesi download berubah");
                    }
                    int request = expected < 0 ? buffer.length
                            : (int) Math.min(buffer.length, expected - networkBytes);
                    int length = input.read(buffer, 0, request);
                    if (length < 0) break;
                    random.write(buffer, 0, length);
                    item.rateLimiter.acquire(length, downloadSpeedLimitKBps);
                    networkBytes += length;
                    item.hlsOutputBytes = random.length();
                    updateDownloadSpeed(item, item.hlsOutputBytes);
                }
            }
            if (expected >= 0 && networkBytes != expected) {
                throw new Exception("Byte-range HLS tidak lengkap");
            }
        } finally {
            try { if (random != null) random.close(); } catch (Exception ignored) {}
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            unregisterDownloadStream(item, input);
            unregisterDownloadConnection(item, connection);
            try { if (connection != null) connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    byte[] readHlsResourceBytes(DownloadItem item, InputStream input,
                                        long expected, int generation) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[DOWNLOAD_BUFFER_SIZE];
        long readTotal = 0;
        while (expected < 0 || readTotal < expected) {
            if (!isCurrentDownloadRun(item, generation)
                    || item.pauseRequested || "paused".equals(item.status)) {
                throw new Exception("Sesi download berubah");
            }
            int request = expected < 0 ? chunk.length : (int) Math.min(chunk.length, expected - readTotal);
            int length = input.read(chunk, 0, request);
            if (length < 0) break;
            item.rateLimiter.acquire(length, downloadSpeedLimitKBps);
            buffer.write(chunk, 0, length);
            readTotal += length;
            if (readTotal > 128L * 1024L * 1024L) {
                throw new Exception("Segmen HLS terenkripsi terlalu besar");
            }
        }
        if (expected >= 0 && readTotal != expected) throw new Exception("Segmen HLS tidak lengkap");
        return buffer.toByteArray();
    }

    byte[] getHlsKeyBytes(DownloadItem item, String keyUrl, int generation,
                                  Map<String, byte[]> cache) throws Exception {
        byte[] cached = cache.get(keyUrl);
        if (cached != null) return cached;
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            connection = openDownloadConnection(keyUrl, item, "");
            validateDownloadResponse(connection);
            long contentLength = connection.getContentLengthLong();
            if (contentLength > 0 && contentLength != 16) {
                throw new Exception("Kunci AES-128 HLS tidak valid");
            }
            input = connection.getInputStream();
            registerDownloadStream(item, input);
            ByteArrayOutputStream keyBuffer = new ByteArrayOutputStream(17);
            byte[] chunk = new byte[17];
            while (keyBuffer.size() <= 16) {
                if (!isCurrentDownloadRun(item, generation)
                        || item.pauseRequested || "paused".equals(item.status)) {
                    throw new Exception("Sesi download berubah");
                }
                int length = input.read(chunk, 0, Math.min(chunk.length, 17 - keyBuffer.size()));
                if (length < 0) break;
                item.rateLimiter.acquire(length, downloadSpeedLimitKBps);
                keyBuffer.write(chunk, 0, length);
            }
            byte[] key = keyBuffer.toByteArray();
            if (key.length != 16) throw new Exception("Kunci AES-128 HLS tidak valid");
            cache.put(keyUrl, key);
            return key;
        } finally {
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            unregisterDownloadStream(item, input);
            unregisterDownloadConnection(item, connection);
            try { if (connection != null) connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    String buildHlsFingerprint(HlsPlaylistParser.Playlist playlist) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        if (playlist.initMap != null) updateHlsFingerprint(digest, playlist.initMap);
        for (HlsPlaylistParser.Resource segment : playlist.segments) updateHlsFingerprint(digest, segment);
        byte[] hash = digest.digest();
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte value : hash) hex.append(String.format(Locale.US, "%02x", value & 0xff));
        return hex.toString();
    }

    void updateHlsFingerprint(MessageDigest digest, HlsPlaylistParser.Resource resource) {
        String normalized = normalizeUrlForFingerprint(resource.url);
        String range = resource.range == null ? "" : "@" + resource.range.start + "-" + resource.range.end;
        String key = "";
        if (resource.key != null) {
            key = "#key=" + normalizeUrlForFingerprint(resource.key.url)
                    + "#iv=" + bytesToHex(resource.key.explicitIv)
                    + "#seq=" + resource.sequence;
        }
        digest.update((normalized + range + key + "\n").getBytes(StandardCharsets.UTF_8));
    }

    String bytesToHex(byte[] value) {
        if (value == null) return "";
        StringBuilder hex = new StringBuilder(value.length * 2);
        for (byte item : value) hex.append(String.format(Locale.US, "%02x", item & 0xff));
        return hex.toString();
    }

    String normalizeUrlForFingerprint(String value) {
        try {
            URI uri = new URI(value);
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null).toString();
        } catch (Exception ignored) {
            int question = value == null ? -1 : value.indexOf('?');
            return question >= 0 ? value.substring(0, question) : String.valueOf(value);
        }
    }

    void applySpeedLimit(DownloadItem item, int bytesRead) throws InterruptedException {
        if (item != null) item.rateLimiter.acquire(bytesRead, downloadSpeedLimitKBps);
    }

    void downloadSingle(DownloadItem item, File out) {
        final int generation = item.runGeneration;
        if (!isCurrentDownloadRun(item, generation)) return;
        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;
        try {
            boolean resume = out.exists() && out.length() > 0 && item.downloadedBytes > 0
                    && item.connectionCount == 1;
            long resumeFrom = resume ? Math.min(item.downloadedBytes, out.length()) : 0;
            connection = openDownloadConnection(item.url, item,
                    resumeFrom > 0 ? "bytes=" + resumeFrom + "-" : "");
            validateDownloadResponse(connection);
            int code = connection.getResponseCode();
            boolean append = false;
            long total;
            if (resumeFrom > 0 && code == HttpURLConnection.HTTP_PARTIAL) {
                DownloadProtocol.RangeInfo range = DownloadProtocol.requireRangeFrom(connection,
                        resumeFrom, item.totalBytes);
                captureRemoteIdentity(item, connection);
                append = true;
                total = range.total > 0 ? range.total : item.totalBytes;
            } else {
                if (resumeFrom > 0) {
                    resumeFrom = 0;
                    item.downloadedBytes = 0;
                    item.progress = 0;
                    item.etag = "";
                    item.lastModified = "";
                }
                captureRemoteIdentity(item, connection);
                long contentLength = connection.getContentLengthLong();
                total = contentLength > 0 ? contentLength : -1;
            }

            String disposition = connection.getHeaderField("Content-Disposition");
            if (!append) {
                String betterName = autoRenameDownloadFile(
                        URLUtil.guessFileName(item.url, disposition, connection.getContentType()),
                        item.url, connection.getContentType());
                if (betterName != null && !betterName.isEmpty() && !betterName.equals(item.fileName)) {
                    File renamed = uniqueFile(new File(out.getParentFile(), betterName));
                    item.fileName = renamed.getName();
                    item.path = renamed.getAbsolutePath();
                    out = renamed;
                }
            }
            item.categoryHint = inferDownloadCategoryFromData(item.fileName, item.url, connection.getContentType());
            item.connectionCount = 1;
            item.activeConnectionLimit = 1;
            item.engineInfo = getTurboLabel(item, 1);
            item.totalBytes = total;
            item.failReason = "";
            item.rateLimiter.reset();
            input = connection.getInputStream();
            registerDownloadStream(item, input);
            output = new FileOutputStream(out, append);
            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            long done = resumeFrom;
            item.lastSpeedTimeMs = 0;
            item.lastSpeedBytes = done;
            resetTurboSampling(item);
            int length;
            while ((length = input.read(buffer)) != -1) {
                if (!isCurrentDownloadRun(item, generation)
                        || item.pauseRequested || "paused".equals(item.status)) break;
                output.write(buffer, 0, length);
                applySpeedLimit(item, length);
                done += length;
                item.downloadedBytes = done;
                updateDownloadSpeed(item, done);
                updateTurboPrediction(item, done);
                if (total > 0) {
                    if (done > total) throw new Exception("File melebihi ukuran server");
                    int percent = (int) Math.min(99, done * 100 / Math.max(1, total));
                    if (percent != item.progress) {
                        item.progress = percent;
                        if (percent % 2 == 0 || percent >= 99) {
                            refreshDownloadPanel();
                            maybePersistDownloadProgress(item);
                            showDownloadNotification(item, "Safe 1 koneksi • " + percent
                                    + "% • " + readableSpeed(item.speedBytesPerSecond), true);
                        }
                    }
                }
            }
            output.flush();
            output.close();
            output = null;
            if (!isCurrentDownloadRun(item, generation)) return;
            if (item.pauseRequested || "paused".equals(item.status)) {
                item.status = "paused";
                resetDownloadSpeedState(item);
                item.engineInfo = "Safe 1 koneksi • dijeda";
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, "Unduhan dijeda", false);
                updateDownloadKeepAliveState();
            } else {
                if (total > 0 && (done != total || !out.exists() || out.length() != total)) {
                    throw new Exception("File tidak lengkap: " + done + "/" + total);
                }
                if (!out.exists() || out.length() <= 0) throw new Exception("File hasil download kosong");
                item.downloadedBytes = out.length();
                completeDownload(item);
            }
        } catch (Exception e) {
            if (!isCurrentDownloadRun(item, generation)) return;
            if (isProtocolOrIdentityFailure(e.getMessage()) && !item.pauseRequested) {
                resetForCleanRestart(item, out);
                item.status = "running";
                item.connectionCount = 1;
                item.activeConnectionLimit = 1;
                downloadSingle(item, out);
                return;
            }
            if (item.pauseRequested || "paused".equals(item.status)) {
                item.status = "paused";
                resetDownloadSpeedState(item);
                item.engineInfo = "Safe 1 koneksi • dijeda";
                saveDownloadHistory();
                refreshDownloadPanel();
                showDownloadNotification(item, "Unduhan dijeda", false);
                updateDownloadKeepAliveState();
            } else {
                failDownload(item, e.getMessage());
            }
        } finally {
            try { if (output != null) output.close(); } catch (Exception ignored) {}
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            unregisterDownloadStream(item, input);
            unregisterDownloadConnection(item, connection);
            try { if (connection != null) connection.disconnect(); } catch (Exception ignored) {}
        }
    }

    void completeDownload(DownloadItem item) {
        if (item == null || "removed".equals(item.status) || "completed".equals(item.status)) return;
        synchronized (item.stateLock) {
            if (item.finalizationQueued) return;
            item.finalizationQueued = true;
            item.pauseRequested = false;
            item.retryCount = 0;
            resetDownloadSpeedState(item);
            item.status = "verifying";
            item.finalizeProgress = 5;
            item.finalizeBytes = 0L;
            item.finalizeTotalBytes = Math.max(1L, item.downloadedBytes);
            item.engineInfo = "Memverifikasi file";
        }
        refreshDownloadPanel();
        showDownloadNotification(item, "Memverifikasi file…", true);

        try {
            DOWNLOAD_FINALIZE_EXECUTOR.execute(() -> performCompleteDownload(item));
        } catch (RuntimeException error) {
            item.finalizationQueued = false;
            failDownload(item, "Finalisasi tidak dapat dijalankan: " + safeText(error.getMessage()));
        }
    }

    /** Performs all file verification and multi-gigabyte export work away from the UI thread. */
    void performCompleteDownload(DownloadItem item) {
        boolean completed = false;
        try {
            if (item == null || "removed".equals(item.status)) return;
            stopActiveDownloadTransports(item);
            saveDownloadHistory();

            File source = item.path == null ? null : new File(item.path);
            if (source == null || !source.exists() || source.length() <= 0) {
                failDownload(item, "File hasil download tidak ditemukan");
                return;
            }
            long sourceLength = source.length();
            if (!item.hlsDownload && item.totalBytes > 0 && sourceLength != item.totalBytes) {
                failDownload(item, "Ukuran file tidak sesuai setelah download");
                return;
            }
            if ("removed".equals(item.status)) return;

            synchronized (item.stateLock) {
                item.status = "saving";
                item.finalizeProgress = 0;
                item.finalizeBytes = 0L;
                item.finalizeTotalBytes = Math.max(1L, sourceLength);
                item.engineInfo = "Menyimpan ke Downloads";
            }
            saveDownloadHistory();
            refreshDownloadPanel();
            showDownloadNotification(item, "Menyimpan ke Downloads…", true);

            boolean exported = exportCompletedDownload(item, source);
            if ("removed".equals(item.status)) return;

            synchronized (item.stateLock) {
                item.progress = 100;
                item.finalizeProgress = 100;
                item.finalizeBytes = Math.max(item.finalizeBytes, sourceLength);
                item.status = "completed";
                item.engineInfo = exported
                        ? getConnectionLabel(item) + " • tersimpan"
                        : getConnectionLabel(item) + " • tersimpan di aplikasi";
            }
            saveDownloadHistory();
            refreshDownloadPanel();
            updateDownloadKeepAliveState();
            showDownloadNotification(item, exported
                    ? "Unduhan selesai" : "Unduhan selesai • penyimpanan lokal", false);
            runOnUiThread(() -> QuietToast.makeText(this, "Unduhan selesai: " + item.fileName,
                    QuietToast.LENGTH_SHORT).show());
            completed = true;
        } catch (Throwable error) {
            if (item != null && !"removed".equals(item.status)) {
                failDownload(item, "Finalisasi gagal: " + safeText(error.getMessage()));
            }
        } finally {
            if (item != null) item.finalizationQueued = false;
            if (completed) mainHandler.postDelayed(this::pumpDownloadQueue, 180L);
        }
    }

    boolean exportCompletedDownload(DownloadItem item, File source) {
        if (item == null || source == null || !source.exists()) return false;
        try {
            Uri exported = selectedDownloadTreeUri != null && !selectedDownloadTreeUri.isEmpty()
                    ? copyFileToSelectedTree(source, item.fileName, item)
                    : copyFileToDefaultDownloads(source, item.fileName, item);
            if (exported == null) return false;
            item.publicUri = exported.toString();
            if (source.exists() && source.delete()) item.path = "";
            return true;
        } catch (Exception e) {
            item.failReason = "Gagal menyalin ke Downloads: " + safeText(e.getMessage());
            return false;
        }
    }

    Uri copyFileToSelectedTree(File source, String fileName, DownloadItem item) throws Exception {
        Uri treeUri = Uri.parse(selectedDownloadTreeUri);
        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri,
                DocumentsContract.getTreeDocumentId(treeUri));
        Uri newFile = DocumentsContract.createDocument(getContentResolver(), documentUri,
                getMimeTypeForName(fileName), fileName);
        if (newFile == null) throw new Exception("Tidak bisa membuat file di folder HP");
        try {
            copyFileToUri(source, newFile, item);
            return newFile;
        } catch (Exception e) {
            try { DocumentsContract.deleteDocument(getContentResolver(), newFile); }
            catch (Exception ignored) {}
            throw e;
        }
    }

    Uri copyFileToDefaultDownloads(File source, String fileName, DownloadItem item) throws Exception {
        String mime = getMimeTypeForName(fileName);
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/Yield Browser");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new Exception("Tidak bisa membuat file di Downloads");
            try {
                copyFileToUri(source, uri, item);
                ContentValues done = new ContentValues();
                done.put(MediaStore.MediaColumns.IS_PENDING, 0);
                getContentResolver().update(uri, done, null, null);
                return uri;
            } catch (Exception e) {
                try { getContentResolver().delete(uri, null, null); } catch (Exception ignored) {}
                throw e;
            }
        }
        File directory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "Yield Browser");
        if (!directory.exists()) directory.mkdirs();
        File target = uniqueFile(new File(directory, fileName));
        try {
            copyFileToFile(source, target, item);
            return Uri.fromFile(target);
        } catch (Exception e) {
            try { if (target.exists()) target.delete(); } catch (Exception ignored) {}
            throw e;
        }
    }

    void copyFileToUri(File source, Uri uri, DownloadItem item) throws Exception {
        try (InputStream input = new FileInputStream(source);
             OutputStream output = getContentResolver().openOutputStream(uri, "w")) {
            if (output == null) throw new Exception("Output folder tidak tersedia");
            copyWithFinalizeProgress(input, output, source.length(), item);
        }
    }

    void copyFileToFile(File source, File target, DownloadItem item) throws Exception {
        try (InputStream input = new FileInputStream(source);
             OutputStream output = new FileOutputStream(target)) {
            copyWithFinalizeProgress(input, output, source.length(), item);
        }
    }

    void copyWithFinalizeProgress(InputStream input, OutputStream output,
                                          long totalBytes, DownloadItem item) throws Exception {
        byte[] buffer = new byte[DownloadFinalizationPolicy.COPY_BUFFER_BYTES];
        long copied = 0L;
        long lastProgressUpdateMs = 0L;
        long lastPersistMs = 0L;
        long lastNotificationMs = 0L;
        long lastCooperativePauseBytes = 0L;
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (item != null && (item.pauseRequested || "removed".equals(item.status))) {
                throw new Exception("Penyimpanan dibatalkan");
            }
            output.write(buffer, 0, read);
            copied += read;
            long now = System.currentTimeMillis();

            if (item != null && DownloadFinalizationPolicy.isUpdateDue(now, lastProgressUpdateMs,
                    DownloadFinalizationPolicy.PROGRESS_UPDATE_INTERVAL_MS, copied, totalBytes)) {
                item.finalizeBytes = copied;
                item.finalizeTotalBytes = Math.max(1L, totalBytes);
                item.finalizeProgress = DownloadFinalizationPolicy.progressPercent(copied, totalBytes);
                refreshDownloadPanel();
                lastProgressUpdateMs = now;
            }

            if (item != null && DownloadFinalizationPolicy.isUpdateDue(now, lastPersistMs,
                    DownloadFinalizationPolicy.HISTORY_PERSIST_INTERVAL_MS, copied, totalBytes)) {
                item.lastProgressPersistMs = now;
                saveDownloadHistory();
                lastPersistMs = now;
            }

            if (item != null && DownloadFinalizationPolicy.isUpdateDue(now, lastNotificationMs,
                    DownloadFinalizationPolicy.NOTIFICATION_UPDATE_INTERVAL_MS, copied, totalBytes)) {
                showDownloadNotification(item,
                        "Menyimpan ke Downloads… " + item.finalizeProgress + "%", true);
                lastNotificationMs = now;
            }

            // A tiny cooperative pause gives the UI and storage provider a chance to run on
            // devices with slower flash without materially reducing large-file throughput.
            if (copied - lastCooperativePauseBytes
                    >= DownloadFinalizationPolicy.COOPERATIVE_PAUSE_EVERY_BYTES) {
                android.os.SystemClock.sleep(DownloadFinalizationPolicy.COOPERATIVE_PAUSE_MS);
                lastCooperativePauseBytes = copied;
            }
        }
        output.flush();
        if (item != null) {
            item.finalizeBytes = copied;
            item.finalizeTotalBytes = Math.max(1L, totalBytes);
            item.finalizeProgress = 100;
            refreshDownloadPanel();
        }
    }

    String getMimeTypeForName(String fileName) {
        try {
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
            if (extension != null && !extension.isEmpty()) {
                String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        extension.toLowerCase(Locale.US));
                if (mime != null) return mime;
            }
        } catch (Exception ignored) {}
        return "application/octet-stream";
    }

    void failDownload(DownloadItem item, String reason) {
        if (item == null || "removed".equals(item.status)) return;
        stopActiveDownloadTransports(item);
        if (item.connectionCount >= 3) {
            item.turboRetryPenalty++;
            if (shouldFallbackTurboToStable(item)) {
                int fallback = getV3FallbackConnections(item);
                item.turboTargetConnections = fallback;
                item.turboProfile = fallback >= 3 ? "auto fallback balanced v3"
                        : fallback >= 2 ? "auto fallback stable v3" : "auto fallback safe v3";
            }
        }

        if (downloadAutoRetry && !isPermanentDownloadError(reason)
                && !item.pauseRequested && item.retryCount < DOWNLOAD_RETRY_MAX) {
            item.retryCount++;
            item.runGeneration++;
            final int retryGeneration = item.runGeneration;
            item.status = "running";
            item.pauseRequested = false;
            resetDownloadSpeedState(item);
            item.failReason = reason == null ? "Koneksi terputus" : reason;
            item.engineInfo = "Retry otomatis " + item.retryCount + "/" + DOWNLOAD_RETRY_MAX;
            saveDownloadHistory();
            refreshDownloadPanel();
            updateDownloadKeepAliveState();
            showDownloadNotification(item, item.engineInfo, true);
            mainHandler.postDelayed(() -> {
                if (!isCurrentDownloadRun(item, retryGeneration) || !"running".equals(item.status)) return;
                try {
                    startTwoConnectionDownload(item, new File(item.path));
                } catch (Exception e) {
                    failDownload(item, e.getMessage());
                }
            }, 1500L * item.retryCount);
            return;
        }

        item.runGeneration++;
        item.status = "failed";
        item.pauseRequested = false;
        resetDownloadSpeedState(item);
        item.failReason = reason == null ? "Koneksi/server menolak unduhan" : reason;
        item.engineInfo = getConnectionLabel(item) + " • terputus/gagal";
        saveDownloadHistory();
        refreshDownloadPanel();
        updateDownloadKeepAliveState();
        showDownloadNotification(item, "Unduhan gagal • buka detail", false);
        runOnUiThread(() -> QuietToast.makeText(this,
                "Unduhan gagal. Buka Download untuk reload/detail.", QuietToast.LENGTH_LONG).show());
        mainHandler.postDelayed(this::pumpDownloadQueue, 300);
    }

    void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_DOWNLOADS,
                    "Yield Downloads", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Progress unduhan Yield Browser");
            channel.setSound(null, null);
            channel.enableVibration(false);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                manager.deleteNotificationChannel("yield_download_engine");
            }
        }
    }

    void cancelDownloadNotification(DownloadItem item) {
        if (item == null) return;
        try {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.cancel(item.id);
        } catch (Exception ignored) {}
    }

    void showDownloadNotification(DownloadItem item, String text, boolean ongoing) {
        if (item == null || "removed".equals(item.status)) return;

        // A running download is represented by the foreground service's progress notification.
        // Do not create a second "engine/background" notification or a duplicate per-item one.
        if (isActiveDownloadStatus(item.status) && ongoing) {
            updateDownloadKeepAliveState();
            return;
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pending;
        if (dedicatedPrivateProfile) {
            Intent openPrivate = new Intent(this, PrivateBrowserActivity.class);
            openPrivate.putExtra("open_downloads", true);
            openPrivate.putExtra("download_id", item.id);
            openPrivate.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            pending = PendingIntent.getActivity(this, item.id + 12000, openPrivate, flags);
        } else {
            Intent intent = new Intent(this, DownloadOpenReceiver.class);
            intent.setAction(ACTION_OPEN_DOWNLOADS);
            intent.putExtra("open_downloads", true);
            intent.putExtra("download_id", item.id);
            pending = PendingIntent.getBroadcast(this, item.id + 12000, intent, flags);
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_DOWNLOADS)
                : new Notification.Builder(this);
        String line = text;
        if ("paused".equals(item.status)) line = "Dijeda • " + getConnectionLabel(item);
        builder.setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(item.fileName)
                .setContentText(line)
                .setContentIntent(pending)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setOnlyAlertOnce(true)
                .setAutoCancel(!ongoing)
                .setOngoing(ongoing)
                .setVisibility(Notification.VISIBILITY_PRIVATE);
        if (ongoing || "paused".equals(item.status)) {
            builder.setProgress(100, getVisibleDownloadProgressPercent(item), false);
        }
        manager.notify(item.id, builder.build());
    }

    void saveDownloadHistory() {
        synchronized (downloadHistoryLock) {
            ArrayList<String> rows = new ArrayList<>();
            synchronized (downloadItems) {
                for (DownloadItem item : downloadItems) {
                    if (DownloadHistoryCodec.shouldPersist(item)) {
                        rows.add(DownloadHistoryCodec.serialize(item));
                    }
                }
            }
            StringBuilder ordered = new StringBuilder();
            for (String row : rows) {
                if (ordered.length() > 0) ordered.append('\n');
                ordered.append(row);
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_DOWNLOAD_HISTORY_ORDERED_V2, ordered.toString())
                    .remove(KEY_DOWNLOAD_HISTORY)
                    .apply();
        }
    }

    void loadDownloadHistory() {
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        ArrayList<String> rows = new ArrayList<>();
        String ordered = preferences.getString(KEY_DOWNLOAD_HISTORY_ORDERED_V2, "");
        if (ordered != null && !ordered.isEmpty()) {
            Collections.addAll(rows, ordered.split("\\n"));
        } else {
            Set<String> legacy = preferences.getStringSet(KEY_DOWNLOAD_HISTORY, new LinkedHashSet<>());
            if (legacy != null) rows.addAll(legacy);
        }
        synchronized (downloadItems) {
            downloadItems.clear();
            for (String row : rows) {
                DownloadItem item = DownloadHistoryCodec.deserialize(row, nextDownloadId++);
                if (item == null) continue;
                try {
                    if (item.path != null && !item.path.isEmpty()) {
                        File savedFile = new File(item.path);
                        boolean missing = !savedFile.exists() && ("completed".equals(item.status)
                                || "paused".equals(item.status) || "failed".equals(item.status));
                        if (missing) cancelDownloadNotification(item);
                    }
                } catch (RuntimeException ignored) {}
                if (item.categoryHint == null || item.categoryHint.isEmpty()) {
                    item.categoryHint = inferDownloadCategoryFromData(item.fileName, item.url, "");
                }
                downloadItems.add(item);
            }
        }
        saveDownloadHistory();
        updateDownloadKeepAliveState();
    }

    void toggleBookmark() {
        String url = getEffectiveCurrentUrl();
        if (url == null || url.length() == 0) {
            QuietToast.makeText(this, "Belum ada situs untuk dibookmark", QuietToast.LENGTH_SHORT).show();
            return;
        }
        BookmarkItemData existing = findBookmarkByUrl(url);
        if (existing != null) {
            bookmarkData.remove(existing);
            saveBookmarkData();
            QuietToast.makeText(this, "Bookmark dihapus", QuietToast.LENGTH_SHORT).show();
        } else {
            String title = (webView != null && webView.getTitle() != null && webView.getTitle().trim().length() > 0) ? webView.getTitle().trim() : guessLabelFromUrl(url);
            bookmarkData.add(0, new BookmarkItemData(title, url, "Bookmark seluler", System.currentTimeMillis()));
            saveBookmarkData();
            QuietToast.makeText(this, "Situs ditambahkan ke bookmark", QuietToast.LENGTH_SHORT).show();
        }
        updateTopActionStates();
    }

    void showBookmarkList() {
        showBookmarkHomePanel();
    }

    void toggleTranslate() {
        showTranslateOptionsDialog();
    }

    void showTranslateLanguageDialog() {
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
                    QuietToast.makeText(this, "Bahasa translate: " + translateTargetLabel, QuietToast.LENGTH_SHORT).show();

                    if (translateEnabled && compatibleTranslateActive && !translateManuallyDisabled) {
                        clearCompatibleTranslationMarks();
                        final int token = translateSessionToken;
                        mainHandler.postDelayed(() -> translatePageCompatible(token), 250);
                    } else if (translateEnabled && !translateManuallyDisabled && isGoogleTranslatedUrl(webView != null ? webView.getUrl() : "")) {
                        String raw = lastTranslateOriginalUrl != null && lastTranslateOriginalUrl.length() > 0 ? lastTranslateOriginalUrl : getOriginalForTranslate(webView.getUrl());
                        if (raw != null && raw.length() > 0) loadTranslatedPage(raw);
                    }

                    updateTopActionStates();
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
