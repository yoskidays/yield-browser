package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class BrowserChromeClientTest {
    @Test
    public void dispatchesProgressShowAndHideCallbacksExactlyOnce() {
        AtomicInteger progressCalls = new AtomicInteger();
        AtomicInteger showCalls = new AtomicInteger();
        AtomicInteger hideCalls = new AtomicInteger();
        AtomicInteger lastProgress = new AtomicInteger();

        BrowserChromeClient client = new BrowserChromeClient(
                (view, progress) -> {
                    progressCalls.incrementAndGet();
                    lastProgress.set(progress);
                },
                (view, callback) -> showCalls.incrementAndGet(),
                hideCalls::incrementAndGet);

        client.onProgressChanged(null, 73);
        client.onShowCustomView(null, null);
        client.onHideCustomView();

        assertEquals(1, progressCalls.get());
        assertEquals(73, lastProgress.get());
        assertEquals(1, showCalls.get());
        assertEquals(1, hideCalls.get());
    }
}
