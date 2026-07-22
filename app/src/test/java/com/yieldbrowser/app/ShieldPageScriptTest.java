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
        assertTrue(script.contains("uptobox\\.(?:com|net)"));
        assertTrue(script.contains("files\\.fm"));
        assertTrue(script.contains("mp4upload\\.(?:com|net)"));
        assertTrue(script.contains("fastdown\\.io"));
        assertTrue(script.contains("fast-down\\.com"));
        assertTrue(script.contains("moonlighttha"));
        assertTrue(script.contains("!downloadPage()&&!control&&!recent"));
        assertTrue(script.contains("downloadPage()||Date.now()<=(S.downloadClickUntil||0)"));
        assertTrue(script.contains("if(!downloadControl(node))return false"));
        assertTrue(script.contains("function shieldDownloadPress"));
        assertTrue(script.contains("function stopEventOnly"));
        assertTrue(script.contains("listenWindow('click',clickGuard,false)"));
        assertTrue(script.contains("listenWindow(t,shieldDownloadPress,true)"));
    }

    @Test
    public void recoversDownloadTargetFromDataAttributesAndUnderAdOverlay() {
        String script = ShieldPageScript.documentStart(true, true, true, true, true);
        assertTrue(script.contains("var u=candidateUrl(e.target)"));
        assertTrue(script.contains("function downloadAtPoint"));
        assertTrue(script.contains("function recoverDownloadClick"));
        assertTrue(script.contains("if(recoverDownloadClick(e,blocked))return"));
        assertTrue(script.contains("[data-href],[data-url],[data-link]"));
        assertTrue(script.contains("YieldAdBlockBridge.onTrustedDownloadGesture"));
    }

    @Test
    public void repairsFullPageAdsWhenSourceTabBecomesVisibleAgain() {
        String script = ShieldPageScript.documentStart(true, true, true, true, true);
        assertTrue(script.contains("function downloadTakeoverText"));
        assertTrue(script.contains("thirdPartyTakeover"));
        assertTrue(script.contains("iframe,object,embed"));
        assertTrue(script.contains("visibilitychange"));
        assertTrue(script.contains("pageshow"));
        assertTrue(script.contains("setTimeout(clean,180)"));
        assertTrue(script.contains("lucky\\s*77"));
    }

    @Test
    public void protectsOploverzHomepageBeforeFirstInteraction() {
        String script = ShieldPageScript.documentStart(true, true, true, true, true);
        assertTrue(script.contains("function adHeavyPortal"));
        assertTrue(script.contains("oploverz\\.[a-z0-9-]+"));
        assertTrue(script.contains("function portalAllowedHost"));
        assertTrue(script.contains("function portalPopupTimer"));
        assertTrue(script.contains("nativeInterval=W.setInterval"));
        assertTrue(script.contains("#overplay,.overplay"));
        assertTrue(script.contains("[class*=overplay]"));
        assertTrue(script.contains("overplay|sponsor"));
    }

    @Test
    public void runtimeConfigCanDisableProtectionWithoutReload() {
        String script = ShieldPageScript.runtimeConfig(false, false, false, false, false);
        assertTrue(script.contains("enabled:false"));
        assertTrue(script.contains("__yieldShieldV2SetConfig"));
    }
}
