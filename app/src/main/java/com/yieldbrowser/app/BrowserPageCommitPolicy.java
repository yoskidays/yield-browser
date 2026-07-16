package com.yieldbrowser.app;

/** Pure fallback decisions used during WebViewClient#onPageCommitVisible. */
final class BrowserPageCommitPolicy {
    private BrowserPageCommitPolicy() {
    }

    static String chooseFinalUrl(String extractedUrl, String rawUrl) {
        return extractedUrl != null ? extractedUrl : rawUrl;
    }

    static TabInfo chooseOwner(TabInfo viewOwner, TabInfo currentOwner) {
        return viewOwner != null ? viewOwner : currentOwner;
    }
}
