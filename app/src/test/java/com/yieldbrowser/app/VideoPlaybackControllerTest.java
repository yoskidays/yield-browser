package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VideoPlaybackControllerTest {
    @Test
    public void selectedSpeedUsesMatchingIndexAndDefaultsToNormal() {
        float[] values = new float[]{0.5f, 1.0f, 1.25f, 1.5f, 2.0f};
        assertEquals(1, VideoPlaybackController.selectedSpeedIndex(values, 3.0f));
        assertEquals(3, VideoPlaybackController.selectedSpeedIndex(values, 1.5f));
    }

    @Test
    public void videoControlScriptsPreserveSemanticActions() {
        assertTrue(BrowserPageScripts.videoControl("play").contains("v.play()"));
        assertTrue(BrowserPageScripts.videoControl("pause").contains("v.pause()"));
        assertTrue(BrowserPageScripts.videoControl("toggle").contains("v.paused"));
    }

    @Test
    public void queueLimitIsRestrictedToSupportedChoices() {
        assertEquals(2, DownloadQueueDialogController.normalizeMaxActive(1));
        assertEquals(3, DownloadQueueDialogController.normalizeMaxActive(3));
        assertEquals(4, DownloadQueueDialogController.normalizeMaxActive(8));
    }
}
