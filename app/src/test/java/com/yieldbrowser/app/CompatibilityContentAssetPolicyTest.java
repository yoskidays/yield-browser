package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompatibilityContentAssetPolicyTest {
    @Test
    public void recognizesSupportedFontExtensions() {
        assertTrue(CompatibilityContentAssetPolicy.isFontAsset("https://cdn.example/font.woff"));
        assertTrue(CompatibilityContentAssetPolicy.isFontAsset("https://cdn.example/font.woff2"));
        assertTrue(CompatibilityContentAssetPolicy.isFontAsset("https://cdn.example/font.ttf"));
        assertTrue(CompatibilityContentAssetPolicy.isFontAsset("https://cdn.example/font.otf"));
    }

    @Test
    public void recognizesFontExtensionsBeforeQueryOrFragment() {
        assertTrue(CompatibilityContentAssetPolicy.isFontAsset(
                "https://cdn.example/font.woff2?v=3"));
        assertTrue(CompatibilityContentAssetPolicy.isFontAsset(
                "https://cdn.example/font.ttf#cached"));
    }

    @Test
    public void rejectsMissingUnsupportedAndEmbeddedExtensions() {
        assertFalse(CompatibilityContentAssetPolicy.isFontAsset(null));
        assertFalse(CompatibilityContentAssetPolicy.isFontAsset(""));
        assertFalse(CompatibilityContentAssetPolicy.isFontAsset(
                "https://cdn.example/image.png"));
        assertFalse(CompatibilityContentAssetPolicy.isFontAsset(
                "https://cdn.example/font.woff2.backup"));
    }
}
