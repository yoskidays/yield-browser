package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrustedDownloadPopupPolicyTest {
    @Test
    public void allowsTrustedTargetImmediatelyAfterDownloadGesture() {
        assertTrue(TrustedDownloadPopupPolicy.canOpen(
                true, true, 10_000L, 12_000L));
    }

    @Test
    public void rejectsExpiredOrUnrelatedPopup() {
        assertFalse(TrustedDownloadPopupPolicy.canOpen(
                true, true, 10_000L, 14_001L));
        assertFalse(TrustedDownloadPopupPolicy.canOpen(
                false, true, 10_000L, 11_000L));
        assertFalse(TrustedDownloadPopupPolicy.canOpen(
                true, false, 10_000L, 11_000L));
        assertFalse(TrustedDownloadPopupPolicy.canOpen(
                true, true, 0L, 11_000L));
    }
}
