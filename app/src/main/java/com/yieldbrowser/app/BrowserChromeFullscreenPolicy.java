package com.yieldbrowser.app;

/** Pure lifecycle decisions for WebChromeClient custom fullscreen views. */
final class BrowserChromeFullscreenPolicy {
    enum ShowAction {
        SHOW,
        REJECT_DUPLICATE
    }

    enum HideAction {
        HIDE,
        NO_OP
    }

    private BrowserChromeFullscreenPolicy() {
    }

    static ShowAction showAction(boolean fullscreenViewAlreadyPresent) {
        return fullscreenViewAlreadyPresent ? ShowAction.REJECT_DUPLICATE : ShowAction.SHOW;
    }

    static HideAction hideAction(boolean fullscreenViewPresent) {
        return fullscreenViewPresent ? HideAction.HIDE : HideAction.NO_OP;
    }
}
