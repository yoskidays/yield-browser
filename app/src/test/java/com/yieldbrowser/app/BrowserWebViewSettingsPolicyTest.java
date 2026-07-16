package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrowserWebViewSettingsPolicyTest {
    @Test
    public void selectsCacheModeWithoutBreakingPrivateOrYoutubeProfiles() {
        assertEquals(BrowserWebViewSettingsPolicy.CacheStrategy.NO_CACHE,
                BrowserWebViewSettingsPolicy.cacheStrategy(true, true, true, false));
        assertEquals(BrowserWebViewSettingsPolicy.CacheStrategy.CACHE_ELSE_NETWORK,
                BrowserWebViewSettingsPolicy.cacheStrategy(false, true, false, true));
        assertEquals(BrowserWebViewSettingsPolicy.CacheStrategy.CACHE_ELSE_NETWORK,
                BrowserWebViewSettingsPolicy.cacheStrategy(false, false, true, false));
        assertEquals(BrowserWebViewSettingsPolicy.CacheStrategy.DEFAULT,
                BrowserWebViewSettingsPolicy.cacheStrategy(false, false, true, true));
    }

    @Test
    public void preservesPlaybackAndPopupRules() {
        assertTrue(BrowserWebViewSettingsPolicy.mediaPlaybackRequiresUserGesture(true, true));
        assertTrue(BrowserWebViewSettingsPolicy.mediaPlaybackRequiresUserGesture(false, false));
        assertFalse(BrowserWebViewSettingsPolicy.mediaPlaybackRequiresUserGesture(false, true));

        assertFalse(BrowserWebViewSettingsPolicy.canOpenWindowsAutomatically(true, true));
        assertTrue(BrowserWebViewSettingsPolicy.canOpenWindowsAutomatically(true, false));
        assertTrue(BrowserWebViewSettingsPolicy.canOpenWindowsAutomatically(false, true));
    }

    @Test
    public void normalizesPrivacyDataSaverAndTextZoom() {
        assertEquals(100, BrowserWebViewSettingsPolicy.normalizedTextZoom(0));
        assertEquals(125, BrowserWebViewSettingsPolicy.normalizedTextZoom(125));
        assertFalse(BrowserWebViewSettingsPolicy.loadsImagesAutomatically(true));
        assertTrue(BrowserWebViewSettingsPolicy.loadsImagesAutomatically(false));
        assertFalse(BrowserWebViewSettingsPolicy.saveFormData(true));
        assertFalse(BrowserWebViewSettingsPolicy.allowFileAccess(true));
        assertTrue(BrowserWebViewSettingsPolicy.saveFormData(false));
    }

    @Test
    public void selectsStableMobileAndDesktopProfiles() {
        assertFalse(BrowserWebViewSettingsPolicy.useWideViewPort(false));
        assertFalse(BrowserWebViewSettingsPolicy.loadWithOverviewMode(false));
        assertEquals(0, BrowserWebViewSettingsPolicy.initialScale(false));
        assertFalse(BrowserWebViewSettingsPolicy.horizontalScrollBarEnabled(false));

        assertTrue(BrowserWebViewSettingsPolicy.useWideViewPort(true));
        assertTrue(BrowserWebViewSettingsPolicy.loadWithOverviewMode(true));
        assertEquals(65, BrowserWebViewSettingsPolicy.initialScale(true));
        assertTrue(BrowserWebViewSettingsPolicy.horizontalScrollBarEnabled(true));
    }
}
