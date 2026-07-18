package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrustedDownloadPopupPolicyTest {
    private static final String SOURCE =
            "https://ratudrakor.biz.id/2026/07/16/download-drama-korea-see-you-at-work-tomorrow-episode-8-subtitle-indonesia/";

    @Test
    public void allowsTrustedTargetImmediatelyAfterMatchingDownloadGesture() {
        assertTrue(TrustedDownloadPopupPolicy.sameSourcePage(SOURCE, SOURCE + "#server-gofile"));
        assertTrue(TrustedDownloadPopupPolicy.canOpen(
                true, true, true, 10_000L, 12_000L));
    }

    @Test
    public void rejectsExpiredUnrelatedOrRacedPopup() {
        assertFalse(TrustedDownloadPopupPolicy.canOpen(
                true, true, true, 10_000L, 14_001L));
        assertFalse(TrustedDownloadPopupPolicy.canOpen(
                false, true, true, 10_000L, 11_000L));
        assertFalse(TrustedDownloadPopupPolicy.canOpen(
                true, false, true, 10_000L, 11_000L));
        assertFalse(TrustedDownloadPopupPolicy.canOpen(
                true, true, false, 10_000L, 11_000L));
        assertFalse(TrustedDownloadPopupPolicy.canOpen(
                true, true, true, 0L, 11_000L));
        assertFalse(TrustedDownloadPopupPolicy.sameSourcePage(
                SOURCE, "https://ratudrakor.biz.id/"));
        assertFalse(TrustedDownloadPopupPolicy.sameSourcePage(SOURCE, ""));
    }
}
