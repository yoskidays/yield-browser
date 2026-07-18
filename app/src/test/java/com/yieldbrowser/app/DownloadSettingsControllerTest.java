package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DownloadSettingsControllerTest {
    @Test
    public void summariesPreserveLimiterLabels() {
        assertEquals(
                "Dynamic 2/4 koneksi, retry, HLS/m3u8, speed limiter: tanpa limit",
                DownloadSettingsController.advancedSummary(0));
        assertEquals(
                "Dynamic 2/4 koneksi, retry, HLS/m3u8, speed limiter: 512 KB/s",
                DownloadSettingsController.advancedSummary(512));
        assertEquals("OFF", DownloadSettingsController.speedLabel(0));
        assertEquals("2048 KB/s", DownloadSettingsController.speedLabel(2048));
    }

    @Test
    public void speedIndexFallsBackToOffForUnknownValues() {
        assertEquals(0, DownloadSettingsController.speedIndex(0));
        assertEquals(2, DownloadSettingsController.speedIndex(512));
        assertEquals(4, DownloadSettingsController.speedIndex(2048));
        assertEquals(0, DownloadSettingsController.speedIndex(999));
    }

    @Test
    public void stateNormalizesInvalidNumericValues() {
        DownloadSettingsController.State state = new DownloadSettingsController.State(
                null, true, 0, true, true, true, true, -5);
        assertEquals("", state.locationText);
        assertEquals(1, state.maxActive);
        assertEquals(0, state.speedLimitKbps);
    }
}
