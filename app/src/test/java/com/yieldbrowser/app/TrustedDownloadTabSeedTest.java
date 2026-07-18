package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TrustedDownloadTabSeedTest {
    @Test
    public void goFileTabStartsAsNormalIsolatedTab() {
        String url = "https://gofile.io/d/jFEynh";
        TabInfo tab = new TabInfo("Download", url, false, false);

        assertFalse(tab.adTab);
        assertEquals(url, tab.url);
        assertEquals(url, tab.lastSafeUrl);
        assertEquals(url, tab.currentPageUrlForRequest);
        assertEquals("gofile.io", tab.isolationHost);
    }
}
