package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SwipeNavigationControllerTest {
    @Test
    public void eligibleGestureRequiresFastHorizontalMovement() {
        assertTrue(SwipeNavigationController.isEligibleGesture(120f, 20f, 500L, 90, 120));
        assertTrue(SwipeNavigationController.isEligibleGesture(-120f, -20f, 900L, 90, 120));
        assertFalse(SwipeNavigationController.isEligibleGesture(89f, 0f, 500L, 90, 120));
        assertFalse(SwipeNavigationController.isEligibleGesture(120f, 121f, 500L, 90, 120));
        assertFalse(SwipeNavigationController.isEligibleGesture(120f, 0f, 901L, 90, 120));
    }

    @Test
    public void longPressBlocksAdAndShieldDestinationsWhenAdBlockIsEnabled() {
        assertTrue(SwipeNavigationController.shouldBlockResolvedLongPressLink(
                false, false, true, false, true, false));
        assertTrue(SwipeNavigationController.shouldBlockResolvedLongPressLink(
                false, false, true, false, false, true));
        assertFalse(SwipeNavigationController.shouldBlockResolvedLongPressLink(
                false, false, false, false, true, true));
    }

    @Test
    public void trustedDownloadBypassesAdClassificationButNotSafeBrowsing() {
        assertFalse(SwipeNavigationController.shouldBlockResolvedLongPressLink(
                true, false, true, true, true, true));
        assertTrue(SwipeNavigationController.shouldBlockResolvedLongPressLink(
                true, true, true, true, false, false));
    }

    @Test
    public void linkLookupScansBehindLargeTransparentOverlays() {
        String script = SwipeNavigationController.buildLinkLookupScript(12f, 24f);
        assertTrue(script.contains("elementsFromPoint"));
        assertTrue(script.contains("area>viewport*0.35"));
        assertTrue(script.contains("opacity>=0.08"));
    }

    @Test
    public void linkLookupMarksSelectableArticleTextForNativeCopyMode() {
        String script = SwipeNavigationController.buildLinkLookupScript(12f, 24f);
        assertTrue(script.contains("caretRangeFromPoint"));
        assertTrue(script.contains("__YIELD_SELECT_TEXT__"));
        assertTrue(script.contains("userSelect!=='none'"));
        assertTrue(script.contains("webkitUserSelect!=='none'"));
        assertTrue(script.contains("a[href],button,input,textarea,select"));
    }
}
