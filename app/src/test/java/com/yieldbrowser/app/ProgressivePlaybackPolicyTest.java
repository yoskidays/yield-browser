package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProgressivePlaybackPolicyTest {
    @Test
    public void supportsCommonProgressiveVideoContainers() {
        assertTrue(ProgressivePlaybackPolicy.supportsContainer("movie.mp4", "", false));
        assertTrue(ProgressivePlaybackPolicy.supportsContainer("yield_video", "https://cdn/video.webm", false));
        assertTrue(ProgressivePlaybackPolicy.supportsContainer("videoplayback", "https://cdn/file", false));
        assertFalse(ProgressivePlaybackPolicy.supportsContainer("playlist.ts", "https://cdn/master.m3u8", true));
        assertFalse(ProgressivePlaybackPolicy.supportsContainer("archive.zip", "https://cdn/archive.zip", false));
    }

    @Test
    public void sequentialDownloadOnlyExposesWrittenPrefix() {
        long available = ProgressivePlaybackPolicy.availableEndExclusive(
                128, 1_000, 1, 400, false, null, null, null);
        assertEquals(400, available);
        assertEquals(700, ProgressivePlaybackPolicy.availableEndExclusive(
                700, 1_000, 1, 400, false, null, null, null));
    }

    @Test
    public void multipartDownloadDoesNotExposeSparseHoles() {
        long[] starts = {0, 500};
        long[] ends = {499, 999};
        long[] done = {200, 350};
        assertEquals(200, ProgressivePlaybackPolicy.availableEndExclusive(
                0, 1_000, 2, 0, false, starts, ends, done));
        assertEquals(850, ProgressivePlaybackPolicy.availableEndExclusive(
                550, 1_000, 2, 0, false, starts, ends, done));
        assertEquals(300, ProgressivePlaybackPolicy.availableEndExclusive(
                300, 1_000, 2, 0, false, starts, ends, done));
    }

    @Test
    public void completedDownloadExposesWholeFile() {
        assertEquals(1_000, ProgressivePlaybackPolicy.availableEndExclusive(
                900, 1_000, 4, 0, true,
                new long[]{0, 250, 500, 750},
                new long[]{249, 499, 749, 999},
                new long[]{0, 0, 0, 0}));
    }
}
