package com.yieldbrowser.app;

import android.graphics.Color;

/** Central application constants shared by the browser UI and support components. */
final class BrowserConstants {
    private BrowserConstants() {
        // Utility class.
    }

    // Theme.
    static final int COLOR_BG = Color.parseColor("#15171C");
    static final int COLOR_SURFACE_2 = Color.parseColor("#2A2D33");
    static final int COLOR_BORDER = Color.parseColor("#3A3D45");
    static final int COLOR_TEXT = Color.parseColor("#F5F7FA");
    static final int COLOR_SUBTEXT = Color.parseColor("#B7BDC8");
    static final int COLOR_ACCENT = Color.parseColor("#F39A22");
    static final int COLOR_ON = Color.parseColor("#22C55E");

    // Preferences and persisted data.
    static final String PREFS = "yield_browser_prefs";
    static final String KEY_BOOKMARKS = "bookmarks";
    static final String KEY_BOOKMARK_DATA = "bookmark_data";
    static final String KEY_BOOKMARK_FOLDERS = "bookmark_folders";
    static final String KEY_DOWNLOAD_HISTORY = "download_history";
    static final String KEY_DOWNLOAD_HISTORY_ORDERED_V2 = "download_history_ordered_v2";
    static final String KEY_BROWSER_HISTORY = "browser_history";
    static final String KEY_BROWSER_HISTORY_BACKUP = "browser_history_backup";
    static final String KEY_BROWSER_HISTORY_V2 = "browser_history_v2";
    static final String KEY_BROWSER_HISTORY_V3 = "browser_history_v3";
    static final String PREFS_HISTORY_V2 = "yield_browser_history_store";
    static final String KEY_NIGHT_EXCEPTIONS = "night_mode_exceptions";
    static final String KEY_TABS_SESSION_V1 = "tabs_session_v1";
    static final String KEY_TABS_CURRENT_INDEX_V1 = "tabs_current_index_v1";

    // Browser history files.
    static final String HISTORY_V3_FILE = "yield_browser_history_v3.txt";
    static final String HISTORY_V3_FOLDER = "Yield Browser/History";
    static final String HISTORY_V3_PUBLIC_FILE = "history.txt";

    // Intents and notifications.
    static final String CHANNEL_DOWNLOADS = "yield_downloads";
    static final String ACTION_OPEN_DOWNLOADS = "com.yieldbrowser.app.OPEN_DOWNLOADS";

    // Download engine.
    static final int DOWNLOAD_CONNECTIONS_STABLE = 2;
    static final int DOWNLOAD_CONNECTIONS_BALANCED = 3;
    static final int DOWNLOAD_CONNECTIONS_DYNAMIC_MAX = 4;
    static final int DOWNLOAD_TURBO_MIN_LARGE_FILE = 50 * 1024 * 1024;
    static final int DOWNLOAD_TURBO_UNKNOWN_LARGE_FILE = 256 * 1024 * 1024;
    static final int DOWNLOAD_BALANCED_UNKNOWN_FILE = 64 * 1024 * 1024;
    static final int DOWNLOAD_STABLE_HOST_LIMIT = 2;
    static final int DOWNLOAD_V3_SCORE_BALANCED = 62;
    static final int DOWNLOAD_V3_SCORE_TURBO = 80;
    static final int DOWNLOAD_RETRY_MAX = 3;
    static final int DOWNLOAD_BUFFER_SIZE = 128 * 1024;
    static final int DOWNLOAD_CONNECT_TIMEOUT = 15_000;
    static final int DOWNLOAD_READ_TIMEOUT = 30_000;

    // Activity request codes.
    static final int REQ_CAMERA_QR = 2401;
    static final int REQ_PICK_DOWNLOAD_FOLDER = 2402;
    static final int REQ_DOWNLOAD_NOTIFICATIONS = 2403;

    // Sites requiring a minimally modified WebView profile.
    static final String[] STRICT_COMPAT_HOSTS = {
            "lordborg.com",
            "instant-monitor.com"
    };
}
