package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DownloadFinalizationPolicyTest {
    @Test
    public void progressIsClampedAndReachesOneHundred() {
        assertEquals(0, DownloadFinalizationPolicy.progressPercent(0, 100));
        assertEquals(50, DownloadFinalizationPolicy.progressPercent(50, 100));
        assertEquals(100, DownloadFinalizationPolicy.progressPercent(100, 100));
        assertEquals(100, DownloadFinalizationPolicy.progressPercent(120, 100));
    }

    @Test
    public void invalidTotalsReturnZero() {
        assertEquals(0, DownloadFinalizationPolicy.progressPercent(50, 0));
        assertEquals(0, DownloadFinalizationPolicy.progressPercent(50, -1));
    }

    @Test
    public void firstProgressSampleIsAlwaysDue() {
        assertTrue(DownloadFinalizationPolicy.isUpdateDue(100, 0, 750, 1, 100));
    }

    @Test
    public void updateIsThrottledBeforeInterval() {
        assertFalse(DownloadFinalizationPolicy.isUpdateDue(1_500, 1_000, 750, 50, 100));
        assertTrue(DownloadFinalizationPolicy.isUpdateDue(1_750, 1_000, 750, 50, 100));
    }

    @Test
    public void completionForcesFinalUpdate() {
        assertTrue(DownloadFinalizationPolicy.isUpdateDue(1_010, 1_000, 5_000, 100, 100));
    }
}
