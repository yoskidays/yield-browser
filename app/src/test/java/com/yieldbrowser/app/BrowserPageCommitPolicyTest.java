package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class BrowserPageCommitPolicyTest {
    @Test
    public void extractedUrlKeepsPriorityOverRawUrl() {
        assertEquals("https://original.example/page",
                BrowserPageCommitPolicy.chooseFinalUrl(
                        "https://original.example/page",
                        "https://proxy.example/page"));
        assertEquals("https://proxy.example/page",
                BrowserPageCommitPolicy.chooseFinalUrl(
                        null, "https://proxy.example/page"));
    }

    @Test
    public void viewOwnerKeepsPriorityOverCurrentTabFallback() {
        TabInfo viewOwner = new TabInfo("View", "", false);
        TabInfo currentOwner = new TabInfo("Current", "", false);

        assertSame(viewOwner,
                BrowserPageCommitPolicy.chooseOwner(viewOwner, currentOwner));
        assertSame(currentOwner,
                BrowserPageCommitPolicy.chooseOwner(null, currentOwner));
    }
}
