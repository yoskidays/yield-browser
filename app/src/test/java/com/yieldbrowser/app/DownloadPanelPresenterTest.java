package com.yieldbrowser.app;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DownloadPanelPresenterTest {
    @Test
    public void queuePositionCountsOnlyQueuedItems() {
        DownloadItem running = new DownloadItem();
        running.id = 1;
        running.status = "running";
        DownloadItem first = new DownloadItem();
        first.id = 2;
        first.status = "queued";
        DownloadItem second = new DownloadItem();
        second.id = 3;
        second.status = "queued";
        List<DownloadItem> items = Arrays.asList(running, first, second);

        assertEquals(1, DownloadPanelPresenter.queuePosition(items, first));
        assertEquals(2, DownloadPanelPresenter.queuePosition(items, second));
        assertEquals(0, DownloadPanelPresenter.queuePosition(items, running));
    }

    @Test
    public void completedMenuPreservesPlaybackAndFileActions() {
        List<DownloadItemMenuController.Entry> entries =
                DownloadItemMenuController.entriesFor("completed", true);
        assertEquals(6, entries.size());
        assertEquals(DownloadItemMenuController.Action.PLAY, entries.get(0).action);
        assertEquals(DownloadItemMenuController.Action.OPEN_EXTERNAL, entries.get(1).action);
        assertEquals(DownloadItemMenuController.Action.DELETE_FILE,
                entries.get(entries.size() - 1).action);
    }

    @Test
    public void queuedMenuPreservesQueueControlsAndRemovalActions() {
        List<DownloadItemMenuController.Entry> entries =
                DownloadItemMenuController.entriesFor("queued", false);
        assertEquals(6, entries.size());
        assertEquals(DownloadItemMenuController.Action.PRIORITIZE, entries.get(0).action);
        assertEquals(DownloadItemMenuController.Action.PAUSE, entries.get(3).action);
        assertEquals(DownloadItemMenuController.Action.DELETE_FILE,
                entries.get(entries.size() - 1).action);
    }
}
