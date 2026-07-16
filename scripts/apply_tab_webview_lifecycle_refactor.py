from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "TabWebViewLifecycle.ensure(" in text:
    print("Tab WebView lifecycle delegation already installed")
    raise SystemExit(0)


def replace_block(source: str, signature: str, replacement: str) -> str:
    if source.count(signature) != 1:
        raise SystemExit(f"Expected one block for {signature!r}, found {source.count(signature)}")
    start = source.index(signature)
    brace = source.find("{", start)
    if brace < 0:
        raise SystemExit(f"Opening brace not found for {signature}")

    depth = 0
    in_string = False
    escaped = False
    quote = ""
    end = -1
    for index in range(brace, len(source)):
        char = source[index]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                in_string = False
            continue
        if char in ('"', "'"):
            in_string = True
            quote = char
        elif char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                end = index + 1
                break
    if end < 0:
        raise SystemExit(f"Unclosed block for {signature}")
    return source[:start] + replacement + source[end:]


text = replace_block(
    text,
    "    private static final class WebViewBinding {",
    "",
)
text = text.replace("WebViewBinding", "TabWebViewLifecycle.Binding")

replacements = {
    "    private TabInfo findTabByWebView(WebView candidate) {": '''    private TabInfo findTabByWebView(WebView candidate) {
        return TabWebViewLifecycle.findOwner(tabs, candidate);
    }''',
    "    private boolean isLiveTabWebView(TabInfo tab, WebView candidate, long generation) {": '''    private boolean isLiveTabWebView(TabInfo tab, WebView candidate, long generation) {
        return TabWebViewLifecycle.isLive(tabs, tab, candidate, generation);
    }''',
    "    private boolean isPrivateWebView(WebView candidate) {": '''    private boolean isPrivateWebView(WebView candidate) {
        return TabWebViewLifecycle.isPrivate(dedicatedPrivateProfile, tabs, candidate);
    }''',
    "    private void attachWebViewToContentFrame(WebView candidate) {": '''    private void attachWebViewToContentFrame(WebView candidate) {
        TabWebViewLifecycle.attach(
                contentFrame, homeScroll, navigationLoadingOverlay, candidate);
    }''',
    "    private WebView ensureTabWebView(TabInfo tab, int visibility) {": '''    private WebView ensureTabWebView(TabInfo tab, int visibility) {
        return TabWebViewLifecycle.ensure(
                tab,
                visibility,
                webView,
                this::createBrowserWebView,
                this::attachWebViewToContentFrame,
                candidate -> {
                    webView = candidate;
                    try { applyBrowserSettings(); } catch (Exception ignored) {}
                    try { applyProfileCookiePolicy(candidate); } catch (Exception ignored) {}
                });
    }''',
    "    private void hideInactiveTabWebViews(WebView active) {": '''    private void hideInactiveTabWebViews(WebView active) {
        TabWebViewLifecycle.hideInactive(tabs, active);
    }''',
    "    private void activateTabWebView(TabInfo tab, boolean showWebPage) {": '''    private void activateTabWebView(TabInfo tab, boolean showWebPage) {
        try {
            activateNavigationContextForTab(tab);
            WebView target = ensureTabWebView(tab, showWebPage ? View.VISIBLE : View.GONE);
            webView = target;
            hideInactiveTabWebViews(target);
            TabWebViewLifecycle.activateSurface(
                    homeScroll, navigationLoadingOverlay, target, showWebPage);
        } catch (Exception ignored) {
        }
    }''',
    "    private void destroyTabWebView(TabInfo tab) {": '''    private void destroyTabWebView(TabInfo tab) {
        if (tab == null) return;
        WebView doomed = tab.webView;
        if (doomed != null && doomed == webView && elementPickerActive) {
            finishElementPicker(false);
        }
        if (doomed != null && doomed == webView) webView = null;
        webView = TabWebViewLifecycle.destroy(
                tab, webView, contentFrame, this::removeShieldDocumentStartScript);
    }''',
    "    private boolean hasLivePage(WebView candidate) {": '''    private boolean hasLivePage(WebView candidate) {
        return TabWebViewLifecycle.hasLivePage(candidate, this::extractOriginalUrl);
    }''',
}

for signature, replacement in replacements.items():
    text = replace_block(text, signature, replacement)

if "WebViewBinding" in text:
    raise SystemExit("Legacy WebViewBinding reference remained after guarded replacement")
if text.count("TabWebViewLifecycle.ensure(") != 1:
    raise SystemExit("Unexpected number of lifecycle ensure delegations")
if text.count("new TabWebViewLifecycle.Binding(") < 1:
    raise SystemExit("WebView creation no longer installs a tab binding")

path.write_text(text, encoding="utf-8")
print("MainActivity per-tab WebView lifecycle delegated")
