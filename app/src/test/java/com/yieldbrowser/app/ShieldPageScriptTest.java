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
        assertTrue(script.contains(
                "onTrustedDownloadGesture(String(location.href||''))"));
        assertTrue(script.contains(
                "onTrustedDownloadOpen(abs(u),String(location.href||''))"));
        assertTrue(script.contains("function openTrustedDownloadClick"));
        assertTrue(script.contains("e.type!=='click'&&e.type!=='auxclick'"));
        assertTrue(script.contains(
                "if(u&&openTrustedDownloadClick(e,e.target,u))return"));
        assertTrue(script.contains("return{closed:false,focus:function(){},close:function(){}}"));
        assertTrue(script.contains("return{closed:true,focus:function(){},close:function(){}}"));
        assertTrue(script.contains("function fakeRewardAd"));
        assertTrue(script.contains("get\\s*paid"));
        assertTrue(script.contains("subscribe\\s*(?:&|and)?\\s*watch"));
        assertFalse(script.contains("Toast"));
        assertFalse(script.contains("blockedCount"));
    }

    @Test
    public void protectsDownloadListingClicksBeforePageAdHandlersRun() {
        String script = ShieldPageScript.documentStart(true, true, true, true, true);
        assertTrue(script.contains("function downloadSite"));
        assertTrue(script.contains("dramaencode\\.net"));
        assertTrue(script.contains("uptobox\\.com"));
        assertTrue(script.contains("files\\.fm"));
        assertTrue(script.contains("mp4upload\\.com"));
        assertTrue(script.contains("moonlighttha"));
        assertTrue(script.contains("!downloadPage()&&!control&&!recent"));
        assertTrue(script.contains("downloadPage()||Date.now()<=(S.downloadClickUntil||0)"));
        assertTrue(script.contains("if(!downloadControl(node))return false"));
    }

    @Test
    public void runtimeConfigCanDisableProtectionWithoutReload() {
        String script = ShieldPageScript.runtimeConfig(false, false, false, false, false);
        assertTrue(script.contains("enabled:false"));
        assertTrue(script.contains("__yieldShieldV2SetConfig"));
    }
}
