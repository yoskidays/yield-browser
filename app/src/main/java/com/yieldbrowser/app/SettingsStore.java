package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.KEY_NIGHT_EXCEPTIONS;

import android.content.SharedPreferences;

import java.util.HashSet;

/**
 * Centralized persistence for Yield browser settings.
 *
 * <p>The load/save bodies are moved verbatim from MainActivity; the only change is that each
 * settings field is now reached through the passed-in MainActivity reference (a.field). Every
 * SharedPreferences key, default value, ordering, and one-time migration is preserved exactly,
 * so stored user settings load and save identically to before.</p>
 */
final class SettingsStore {
    private SettingsStore() {
        // Utility class.
    }

    static void load(MainActivity a, SharedPreferences p) {
        a.speedMode = p.getBoolean("speedMode", false);
        a.safeMode = p.getBoolean("safeMode", true);
        a.nightMode = p.getBoolean("nightMode", true);
        a.nightModeOption = p.getString("nightModeOption", a.nightMode ? "ON" : "OFF");
        a.hideGoogleTranslateBar = p.getBoolean("hideGoogleTranslateBar", true);
        a.translateTargetLang = p.getString("translateTargetLang", "id");
        a.translateTargetLabel = p.getString("translateTargetLabel", a.translateLanguageLabel(a.translateTargetLang));
        a.nightModeExceptions.clear();
        a.nightModeExceptions.addAll(p.getStringSet(KEY_NIGHT_EXCEPTIONS, new HashSet<>()));
        a.readerMode = p.getBoolean("readerMode", false);
        a.adBlock = p.getBoolean("adBlock", true);
        a.adBlockPopupBlocker = p.getBoolean("adBlockPopupBlocker", true);
        a.adBlockRedirectBlocker = p.getBoolean("adBlockRedirectBlocker", true);
        a.adBlockScriptIframeBlocker = p.getBoolean("adBlockScriptIframeBlocker", true);
        a.adBlockClickHijackBlocker = p.getBoolean("adBlockClickHijackBlocker", true);
        if (!p.getBoolean("adBlockDefaultOnV095", false)) {
            a.adBlock = true;
            p.edit()
                    .putBoolean("adBlock", true)
                    .putBoolean("adBlockDefaultOnV095", true)
                    .apply();
        }
        a.adBlockRedirectToTempTab = p.getBoolean("adBlockRedirectToTempTab", true);
        a.adBlockAutoCloseAdTabs = p.getBoolean("adBlockAutoCloseAdTabs", true);
        a.dataSaver = p.getBoolean("dataSaver", false);
        a.httpsFirstEnabled = p.getBoolean("httpsFirstEnabled", true);
        a.desktopMode = p.getBoolean("desktopMode", false);
        a.forceMobileModeAfterUpdateIfNeeded(p);
        a.textZoom = p.getInt("textZoom", 100);
        a.shortcutDownload = p.getBoolean("shortcutDownload", true);
        a.shortcutReloadWebsite = p.getBoolean("shortcutReloadWebsite", true);
        a.shortcutBookmark = p.getBoolean("shortcutBookmark", false);
        a.shortcutPrivate = p.getBoolean("shortcutPrivate", true);
        a.shortcutAdBlock = p.getBoolean("shortcutAdBlock", true);
        a.shortcutReader = p.getBoolean("shortcutReader", false);
        a.shortcutNightMode = p.getBoolean("shortcutNightMode", false);
        a.shortcutQrScan = p.getBoolean("shortcutQrScan", false);
        a.shortcutHistory = p.getBoolean("shortcutHistory", true);
        a.shortcutFindPage = p.getBoolean("shortcutFindPage", false);
        a.shortcutShare = p.getBoolean("shortcutShare", false);
        a.shortcutFullscreen = p.getBoolean("shortcutFullscreen", false);
        a.shortcutBlockElement = p.getBoolean("shortcutBlockElement", true);
        a.shortcutSiteFilter = p.getBoolean("shortcutSiteFilter", true);
        a.videoControlsEnabled = p.getBoolean("videoControlsEnabled", true);
        a.videoBufferBooster = p.getBoolean("videoBufferBooster", true);
        a.hlsSegmentPrefetch = p.getBoolean("hlsSegmentPrefetch", true);
        a.videoFloatingPlayer = p.getBoolean("videoFloatingPlayer", false);
        if (!p.getBoolean("disableAutoPipOnMinimizeV0910", false)) {
            a.videoFloatingPlayer = false;
            p.edit()
                    .putBoolean("videoFloatingPlayer", false)
                    .putBoolean("disableAutoPipOnMinimizeV0910", true)
                    .apply();
        }
        a.videoBackgroundPlay = p.getBoolean("videoBackgroundPlay", true);
        a.shortcutVideoControls = p.getBoolean("shortcutVideoControls", false);
        a.videoSpeed = p.getFloat("videoSpeed", 1.0f);
        a.selectedVideoQuality = p.getString("selectedVideoQuality", "Auto");
        a.downloadSubfolder = p.getString("downloadSubfolder", "Download");
        a.selectedDownloadTreeUri = p.getString("selectedDownloadTreeUri", "");
        a.downloadDynamic4Connections = p.getBoolean("downloadDynamic4Connections", true);
        a.downloadAutoRetry = p.getBoolean("downloadAutoRetry", true);
        a.downloadHlsEnabled = p.getBoolean("downloadHlsEnabled", true);
        a.downloadPlayWhileDownloadingEnabled = p.getBoolean("downloadPlayWhileDownloadingEnabled", true);
        a.downloadSpeedLimitKBps = p.getInt("downloadSpeedLimitKBps", 0);
        a.downloadQueueEnabled = p.getBoolean("downloadQueueEnabled", true);
        a.downloadMaxActive = Math.max(1, Math.min(4, p.getInt("downloadMaxActive", 2)));
        a.topIconReload = p.getBoolean("topIconReload", true);
        a.topIconBookmark = p.getBoolean("topIconBookmark", true);
        a.topIconTranslate = p.getBoolean("topIconTranslate", true);
        a.searchEngine = p.getString("searchEngine", "Google");

        if (!p.getBoolean("menuDefaultsV030", false)) {
            a.shortcutDownload = true;
            a.shortcutReloadWebsite = true;
            a.shortcutBookmark = false;
            a.shortcutPrivate = true;
            a.shortcutAdBlock = true;
            a.shortcutReader = false;
            a.shortcutNightMode = false;
            a.shortcutQrScan = false;
            a.shortcutHistory = true;
            a.shortcutFindPage = false;
            a.shortcutShare = false;
            a.shortcutFullscreen = false;
            a.shortcutBlockElement = true;
            a.shortcutSiteFilter = true;
            a.shortcutVideoControls = false;

            p.edit()
                    .putBoolean("shortcutDownload", a.shortcutDownload)
                    .putBoolean("shortcutReloadWebsite", a.shortcutReloadWebsite)
                    .putBoolean("shortcutBookmark", a.shortcutBookmark)
                    .putBoolean("shortcutPrivate", a.shortcutPrivate)
                    .putBoolean("shortcutAdBlock", a.shortcutAdBlock)
                    .putBoolean("shortcutReader", a.shortcutReader)
                    .putBoolean("shortcutNightMode", a.shortcutNightMode)
                    .putBoolean("shortcutQrScan", a.shortcutQrScan)
                    .putBoolean("shortcutHistory", a.shortcutHistory)
                    .putBoolean("shortcutFindPage", a.shortcutFindPage)
                    .putBoolean("shortcutShare", a.shortcutShare)
                    .putBoolean("shortcutFullscreen", a.shortcutFullscreen)
                    .putBoolean("shortcutBlockElement", a.shortcutBlockElement)
                    .putBoolean("shortcutSiteFilter", a.shortcutSiteFilter)
                    .putBoolean("shortcutVideoControls", a.shortcutVideoControls)
                    .putBoolean("menuDefaultsV030", true)
                    .apply();
        }
        }

    static void save(MainActivity a, SharedPreferences p) {
        p.edit()
                .putBoolean("speedMode", a.speedMode)
                .putBoolean("safeMode", a.safeMode)
                .putBoolean("hideGoogleTranslateBar", a.hideGoogleTranslateBar)
                .putString("translateTargetLang", a.translateTargetLang)
                .putString("translateTargetLabel", a.translateTargetLabel)
                .putBoolean("nightMode", !"OFF".equals(a.nightModeOption))
                .putString("nightModeOption", a.nightModeOption)
                .putStringSet(KEY_NIGHT_EXCEPTIONS, new HashSet<>(a.nightModeExceptions))
                .putBoolean("readerMode", a.readerMode)
                .putBoolean("adBlock", a.adBlock)
                .putBoolean("adBlockPopupBlocker", a.adBlockPopupBlocker)
                .putBoolean("adBlockRedirectBlocker", a.adBlockRedirectBlocker)
                .putBoolean("adBlockScriptIframeBlocker", a.adBlockScriptIframeBlocker)
                .putBoolean("adBlockClickHijackBlocker", a.adBlockClickHijackBlocker)
                .putBoolean("adBlockDefaultOnV095", true)
                .putBoolean("adBlockRedirectToTempTab", a.adBlockRedirectToTempTab)
                .putBoolean("adBlockAutoCloseAdTabs", a.adBlockAutoCloseAdTabs)
                .putBoolean("dataSaver", a.dataSaver)
                .putBoolean("httpsFirstEnabled", a.httpsFirstEnabled)
                .putBoolean("desktopMode", a.desktopMode)
                .putInt("textZoom", a.textZoom)
                .putBoolean("shortcutDownload", a.shortcutDownload)
                .putBoolean("shortcutReloadWebsite", a.shortcutReloadWebsite)
                .putBoolean("shortcutBookmark", a.shortcutBookmark)
                .putBoolean("shortcutPrivate", a.shortcutPrivate)
                .putBoolean("shortcutAdBlock", a.shortcutAdBlock)
                .putBoolean("shortcutReader", a.shortcutReader)
                .putBoolean("shortcutNightMode", a.shortcutNightMode)
                .putBoolean("shortcutQrScan", a.shortcutQrScan)
                .putBoolean("shortcutHistory", a.shortcutHistory)
                .putBoolean("shortcutFindPage", a.shortcutFindPage)
                .putBoolean("shortcutShare", a.shortcutShare)
                .putBoolean("shortcutFullscreen", a.shortcutFullscreen)
                .putBoolean("shortcutBlockElement", a.shortcutBlockElement)
                .putBoolean("shortcutSiteFilter", a.shortcutSiteFilter)
                .putBoolean("videoControlsEnabled", a.videoControlsEnabled)
                .putBoolean("videoBufferBooster", a.videoBufferBooster)
                .putBoolean("hlsSegmentPrefetch", a.hlsSegmentPrefetch)
                .putBoolean("videoFloatingPlayer", a.videoFloatingPlayer)
                .putBoolean("disableAutoPipOnMinimizeV0910", true)
                .putBoolean("videoBackgroundPlay", a.videoBackgroundPlay)
                .putBoolean("shortcutVideoControls", a.shortcutVideoControls)
                .putFloat("videoSpeed", a.videoSpeed)
                .putString("selectedVideoQuality", a.selectedVideoQuality)
                .putString("downloadSubfolder", a.downloadSubfolder)
                .putString("selectedDownloadTreeUri", a.selectedDownloadTreeUri)
                .putBoolean("downloadDynamic4Connections", a.downloadDynamic4Connections)
                .putBoolean("downloadAutoRetry", a.downloadAutoRetry)
                .putBoolean("downloadHlsEnabled", a.downloadHlsEnabled)
                .putBoolean("downloadPlayWhileDownloadingEnabled", a.downloadPlayWhileDownloadingEnabled)
                .putInt("downloadSpeedLimitKBps", a.downloadSpeedLimitKBps)
                .putBoolean("downloadQueueEnabled", a.downloadQueueEnabled)
                .putInt("downloadMaxActive", a.downloadMaxActive)
                .putBoolean("topIconReload", a.topIconReload)
                .putBoolean("topIconBookmark", a.topIconBookmark)
                .putBoolean("topIconTranslate", a.topIconTranslate)
                .putString("searchEngine", a.searchEngine)
                .putBoolean("menuDefaultsV030", true)
                .apply();
        }
}
