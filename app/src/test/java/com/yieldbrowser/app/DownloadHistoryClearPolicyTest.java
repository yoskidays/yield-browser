package com.yieldbrowser.app;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DownloadHistoryClearPolicyTest {
    private DownloadItem item(int id, String status) {
        return new DownloadItem(id, "https://example.org/file-" + id,
                "file-" + id + ".mp4", "/tmp/file-" + id + ".mp4", status, 100);
    }

    @Test
    public void onlyCompletedItemsAreClearableHistory() {
        assertTrue(DownloadHistoryClearPolicy.isClearable(item(1, "completed")));
        assertFalse(DownloadHistoryClearPolicy.isClearable(item(2, "running")));
        assertFalse(DownloadHistoryClearPolicy.isClearable(item(3, "paused")));
        assertFalse(DownloadHistoryClearPolicy.isClearable(item(4, "failed")));
        assertFalse(DownloadHistoryClearPolicy.isClearable(null));
    }

    @Test
    public void countsOnlyCompletedRecords() {
        assertEquals(2, DownloadHistoryClearPolicy.countClearable(Arrays.asList(
                item(1, "completed"),
                item(2, "running"),
                item(3, "completed"),
                item(4, "failed")
        )));
    }
}
