package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShieldPageScriptTest {
    @Test
    public void buildsSilentUniversalDocumentStartScript() {
        String script = ShieldPageScript.documentStart(true, true, true, true, true);
        assertTrue(script.contains("__yieldShieldV2State"));
        assertTrue(script.contains("MutationObserver"));
        assertTrue(script.contains("sameRelay"));
        assertTrue(script.contains("safeReaderNav"));
        assertTrue(script.contains("navControl"));
        assertTrue(script.contains("elementsFromPoint"));
        assertTrue(script.contains("recoverReaderClick"));
        assertTrue(script.contains("location.assign(safe)"));
        assertTrue(script.contains("touchstart"));
        assertTrue(script.contains("pointerdown"));
        assertTrue(script.contains("passive:false"));
        assertTrue(script.contains("eventPoint"));
        assertTrue(script.contains("function searchPage"));
        assertTrue(script.contains("if(searchPage())return false"));
        assertTrue(script.contains("function badNav"));
        assertTrue(script.contains("return reader()&&!!h&&!!c&&!same(h,c)"));
        assertTrue(script.contains("lastBlockedAt"));
        assertTrue(script.contains("downloadClickUntil"));
        assertTrue(script.contains("onTrustedDownloadGesture"));
        assertTrue(script.contains("onTrustedDownloadOpen"));
        assertTrue(script.contains("function fakeRewardAd"));
        assertTrue(script.contains("get\\s*paid"));
        assertTrue(script.contains("subscribe\\s*(?:&|and)?\\s*watch"));
        assertFalse(script.contains("Toast"));
        assertFalse(script.contains("blockedCount"));
    }

    @Test
    public void runtimeConfigCanDisableProtectionWithoutReload() {
        String script = ShieldPageScript.runtimeConfig(false, false, false, false, false);
        assertTrue(script.contains("enabled:false"));
        assertTrue(script.contains("__yieldShieldV2SetConfig"));
    }
}
