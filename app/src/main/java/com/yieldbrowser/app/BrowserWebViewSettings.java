package com.yieldbrowser.app;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

/** Applies the mechanical Android WebView/WebSettings profile selected by MainActivity. */
final class BrowserWebViewSettings {
    interface DarkeningApplier {
        void apply(WebSettings settings, boolean enabled);
    }

    static final class Config {
        final boolean privateProfile;
        final boolean speedMode;
        final boolean videoBufferBooster;
        final boolean youtubePage;
        final boolean videoBackgroundPlay;
        final boolean adBlock;
        final boolean popupBlocker;
        final boolean dataSaver;
        final int textZoom;
        final boolean desktopMode;
        final String mobileUserAgent;
        final String desktopUserAgent;
        final boolean nightModeActive;
        final int backgroundColor;

        Config(boolean privateProfile,
               boolean speedMode,
               boolean videoBufferBooster,
               boolean youtubePage,
               boolean videoBackgroundPlay,
               boolean adBlock,
               boolean popupBlocker,
               boolean dataSaver,
               int textZoom,
               boolean desktopMode,
               String mobileUserAgent,
               String desktopUserAgent,
               boolean nightModeActive,
               int backgroundColor) {
            this.privateProfile = privateProfile;
            this.speedMode = speedMode;
            this.videoBufferBooster = videoBufferBooster;
            this.youtubePage = youtubePage;
            this.videoBackgroundPlay = videoBackgroundPlay;
            this.adBlock = adBlock;
            this.popupBlocker = popupBlocker;
            this.dataSaver = dataSaver;
            this.textZoom = textZoom;
            this.desktopMode = desktopMode;
            this.mobileUserAgent = mobileUserAgent == null ? "" : mobileUserAgent;
            this.desktopUserAgent = desktopUserAgent == null ? "" : desktopUserAgent;
            this.nightModeActive = nightModeActive;
            this.backgroundColor = backgroundColor;
        }
    }

    private BrowserWebViewSettings() {
    }

    static void prepareNewWebView(WebView target, boolean privateProfile) {
        if (target == null || !privateProfile) return;
        try { target.setSaveEnabled(false); } catch (Exception ignored) {}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                target.setImportantForAutofill(
                        View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
            } catch (Exception ignored) {
            }
        }
        try { target.clearHistory(); } catch (Exception ignored) {}
        try { target.clearFormData(); } catch (Exception ignored) {}
        try { target.clearCache(true); } catch (Exception ignored) {}
    }

    @SuppressLint("SetJavaScriptEnabled")
    static void apply(WebView target, Config config, DarkeningApplier darkeningApplier) {
        if (target == null || config == null) return;
        WebSettings settings = target.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setSupportMultipleWindows(false);
        try {
            settings.setMediaPlaybackRequiresUserGesture(
                    BrowserWebViewSettingsPolicy.mediaPlaybackRequiresUserGesture(
                            config.youtubePage, config.videoBackgroundPlay));
        } catch (Exception ignored) {
        }
        settings.setJavaScriptCanOpenWindowsAutomatically(
                BrowserWebViewSettingsPolicy.canOpenWindowsAutomatically(
                        config.adBlock, config.popupBlocker));
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(toAndroidCacheMode(
                BrowserWebViewSettingsPolicy.cacheStrategy(
                        config.privateProfile,
                        config.speedMode,
                        config.videoBufferBooster,
                        config.youtubePage)));
        try {
            settings.setSaveFormData(
                    BrowserWebViewSettingsPolicy.saveFormData(config.privateProfile));
        } catch (Exception ignored) {
        }
        try {
            settings.setAllowFileAccess(
                    BrowserWebViewSettingsPolicy.allowFileAccess(config.privateProfile));
        } catch (Exception ignored) {
        }
        try { settings.setAllowContentAccess(true); } catch (Exception ignored) {}
        settings.setLoadsImagesAutomatically(
                BrowserWebViewSettingsPolicy.loadsImagesAutomatically(config.dataSaver));
        settings.setTextZoom(
                BrowserWebViewSettingsPolicy.normalizedTextZoom(config.textZoom));

        if (config.desktopMode) {
            applyDesktopProfile(target, settings, config.desktopUserAgent);
        } else {
            applyMobileProfile(target, settings, config.mobileUserAgent);
        }

        if (darkeningApplier != null) {
            try { darkeningApplier.apply(settings, config.nightModeActive); }
            catch (Exception ignored) {}
        }
        try { target.setBackgroundColor(config.backgroundColor); }
        catch (Exception ignored) {}
    }

    static void applyMobileProfile(WebView target,
                                   WebSettings settings,
                                   String userAgent) {
        if (target == null || settings == null) return;
        settings.setUserAgentString(userAgent == null ? "" : userAgent);
        settings.setUseWideViewPort(
                BrowserWebViewSettingsPolicy.useWideViewPort(false));
        settings.setLoadWithOverviewMode(
                BrowserWebViewSettingsPolicy.loadWithOverviewMode(false));
        try {
            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        } catch (Exception ignored) {
        }
        try { target.setInitialScale(BrowserWebViewSettingsPolicy.initialScale(false)); }
        catch (Exception ignored) {}
        try {
            target.setHorizontalScrollBarEnabled(
                    BrowserWebViewSettingsPolicy.horizontalScrollBarEnabled(false));
        } catch (Exception ignored) {
        }
        try { target.setVerticalScrollBarEnabled(true); } catch (Exception ignored) {}
    }

    static void applyDesktopProfile(WebView target,
                                    WebSettings settings,
                                    String userAgent) {
        if (target == null || settings == null) return;
        settings.setUserAgentString(userAgent == null ? "" : userAgent);
        settings.setUseWideViewPort(
                BrowserWebViewSettingsPolicy.useWideViewPort(true));
        settings.setLoadWithOverviewMode(
                BrowserWebViewSettingsPolicy.loadWithOverviewMode(true));
        try { settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL); }
        catch (Exception ignored) {}
        try { target.setInitialScale(BrowserWebViewSettingsPolicy.initialScale(true)); }
        catch (Exception ignored) {}
        try {
            target.setHorizontalScrollBarEnabled(
                    BrowserWebViewSettingsPolicy.horizontalScrollBarEnabled(true));
        } catch (Exception ignored) {
        }
        try { target.setVerticalScrollBarEnabled(true); } catch (Exception ignored) {}
    }

    private static int toAndroidCacheMode(
            BrowserWebViewSettingsPolicy.CacheStrategy strategy) {
        if (strategy == BrowserWebViewSettingsPolicy.CacheStrategy.NO_CACHE) {
            return WebSettings.LOAD_NO_CACHE;
        }
        if (strategy == BrowserWebViewSettingsPolicy.CacheStrategy.CACHE_ELSE_NETWORK) {
            return WebSettings.LOAD_CACHE_ELSE_NETWORK;
        }
        return WebSettings.LOAD_DEFAULT;
    }
}
