package com.yieldbrowser.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class DownloadUiMetricsTest {
    @Test
    public void progressUsesBasisPointPrecision() {
        assertEquals(3_330, DownloadUiMetrics.progressBasisPoints(333, 1_000, 0));
        assertEquals(5_000, DownloadUiMetrics.progressBasisPoints(0, 0, 50));
    }

    @Test
    public void speedSmoothingReducesVisualJumps() {
        double smoothed = DownloadUiMetrics.smoothSpeed(6_000_000, 2_000_000);
        assertTrue(smoothed > 2_000_000);
        assertTrue(smoothed < 6_000_000);
        assertEquals(5_120_000, smoothed, 1.0);
    }

    @Test
    public void etaUsesSmoothedThroughput() {
        assertEquals(50, DownloadUiMetrics.estimateRemainingSeconds(
                100_000_000, 50_000_000, 1_000_000));
        assertEquals(-1, DownloadUiMetrics.estimateRemainingSeconds(0, 0, 1_000_000));
    }
}
