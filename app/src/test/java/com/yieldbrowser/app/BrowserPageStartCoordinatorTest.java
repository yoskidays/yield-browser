package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrowserPageStartCoordinatorTest {
    @Test
    public void recordsEarlyHistoryBeforeRestoringDirectImageNavigation() {
        Scenario scenario = new Scenario();
        scenario.directImage = true;

        assertTrue(scenario.handle());
        assertEquals(1, scenario.overlayCalls);
        assertEquals(1, scenario.historyUnlockCalls);
        assertEquals(1, scenario.historyRecordCalls);
        assertEquals(1, scenario.restoreCalls);
        assertEquals(scenario.rawUrl, scenario.restoredUrl);
    }

    @Test
    public void compatibilityFlowBypassesImageAndRedirectGuards() {
        Scenario scenario = new Scenario();
        scenario.compatibilityFlow = true;
        scenario.directImage = true;
        scenario.suspiciousPopup = true;
        scenario.likelyAdClick = true;

        assertFalse(scenario.handle());
        assertEquals(0, scenario.restoreCalls);
    }

    @Test
    public void externalSchemeStillRestoresDuringCompatibilityFlow() {
        Scenario scenario = new Scenario();
        scenario.compatibilityFlow = true;
        scenario.externalScheme = true;

        assertTrue(scenario.handle());
        assertEquals(1, scenario.restoreCalls);
    }

    @Test
    public void searchResultsAndTrustedNavigationBypassRedirectRecovery() {
        Scenario searchResult = new Scenario();
        searchResult.searchResult = true;
        searchResult.suspiciousPopup = true;
        assertFalse(searchResult.handle());

        Scenario trusted = new Scenario();
        trusted.trustedNavigation = true;
        trusted.directImage = true;
        trusted.suspiciousPopup = true;
        assertFalse(trusted.handle());
    }

    @Test
    public void suspiciousPopupAndLikelyAdClickTriggerRedirectRecovery() {
        Scenario suspicious = new Scenario();
        suspicious.suspiciousPopup = true;
        assertTrue(suspicious.handle());

        Scenario likelyAd = new Scenario();
        likelyAd.likelyAdClick = true;
        assertTrue(likelyAd.handle());
    }

    private static final class Scenario {
        final String rawUrl = "https://target.example/page";
        boolean recordHistory = true;
        boolean compatibilityFlow;
        boolean trustedNavigation;
        boolean directImage;
        boolean externalScheme;
        boolean searchResult;
        boolean suspiciousPopup;
        boolean likelyAdClick;
        int overlayCalls;
        int historyUnlockCalls;
        int historyRecordCalls;
        int restoreCalls;
        String restoredUrl;

        boolean handle() {
            return BrowserPageStartCoordinator.handleNavigation(
                    null,
                    rawUrl,
                    null,
                    "https://safe.example/page",
                    true,
                    true,
                    () -> overlayCalls++,
                    url -> recordHistory,
                    () -> historyUnlockCalls++,
                    (title, url) -> historyRecordCalls++,
                    () -> "https://current.example/page",
                    tab -> "https://tab.example/page",
                    (target, source) -> compatibilityFlow,
                    url -> trustedNavigation,
                    (target, source) -> directImage,
                    url -> externalScheme,
                    (target, source) -> searchResult,
                    (target, source) -> suspiciousPopup,
                    url -> likelyAdClick,
                    (view, url) -> {
                        restoreCalls++;
                        restoredUrl = url;
                    });
        }
    }
}
