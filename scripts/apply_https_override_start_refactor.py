from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "HttpsOverrideStartPolicy.resolve(" in text:
    print("HTTPS override start delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean startHttpsFirstOverrideIfNeeded(WebView view, String targetUrl, TabInfo tab) {
        if (view == null || !isHttpUrl(targetUrl)) return false;
        String secure = prepareHttpsFirstNavigation(targetUrl, tab);
        if (secure == null || secure.equals(targetUrl)) return false;
        markTrustedMainFrameNavigation(secure);
        prepareTabForMainFrameNavigation(tab, secure);
        if (tab != null) {
            tab.url = secure;
            tab.currentPageUrlForRequest = secure;
        }
        if (view == webView && addressBar != null) addressBar.setText(secure);
        loadBrowserUrl(secure);
        return true;
    }
'''

new_method = '''    private boolean startHttpsFirstOverrideIfNeeded(WebView view, String targetUrl, TabInfo tab) {
        String secure = HttpsOverrideStartPolicy.resolve(
                view != null,
                targetUrl,
                MainActivity.this::isHttpUrl,
                url -> prepareHttpsFirstNavigation(url, tab));
        if (secure == null) return false;
        markTrustedMainFrameNavigation(secure);
        prepareTabForMainFrameNavigation(tab, secure);
        if (tab != null) {
            tab.url = secure;
            tab.currentPageUrlForRequest = secure;
        }
        if (view == webView && addressBar != null) addressBar.setText(secure);
        loadBrowserUrl(secure);
        return true;
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy HTTPS override start method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("HttpsOverrideStartPolicy.resolve(") != 1:
    raise SystemExit("Expected exactly one HTTPS override start delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity HTTPS override start delegated")
