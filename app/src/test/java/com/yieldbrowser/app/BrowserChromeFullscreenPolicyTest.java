package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BrowserChromeFullscreenPolicyTest {
    @Test
    public void acceptsFirstFullscreenViewAndRejectsDuplicate() {
        assertEquals(BrowserChromeFullscreenPolicy.ShowAction.SHOW,
                BrowserChromeFullscreenPolicy.showAction(false));
        assertEquals(BrowserChromeFullscreenPolicy.ShowAction.REJECT_DUPLICATE,
                BrowserChromeFullscreenPolicy.showAction(true));
    }

    @Test
    public void hidesOnlyWhenFullscreenViewExists() {
        assertEquals(BrowserChromeFullscreenPolicy.HideAction.HIDE,
                BrowserChromeFullscreenPolicy.hideAction(true));
        assertEquals(BrowserChromeFullscreenPolicy.HideAction.NO_OP,
                BrowserChromeFullscreenPolicy.hideAction(false));
    }
}
