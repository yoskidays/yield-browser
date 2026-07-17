from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "HttpsNavigationFailurePolicy.passesPreflight(" in text:
    print("HTTPS navigation failure delegation already installed")
    raise SystemExit(0)

old_block = '''        try {
            if (!httpsFirstEnabled || view == null || !isHttpsUrl(failedUrl)
                    || !isHttpsFallbackEligibleError(errorCode)) return false;
            TabInfo tab = findTabByWebView(view);
            if (tab == null && view == webView) tab = getCurrentTab();
            if (tab == null || tab.pendingHttpsOriginalUrl == null || tab.pendingHttpsOriginalUrl.length() == 0) return false;
            String expectedHost = hostOfUrl(tab.pendingHttpsUpgradeUrl);
            String failedHost = hostOfUrl(failedUrl);
            if (!HttpsHostRelationPolicy.areRelated(
                    expectedHost,
                    failedHost,
                    MainActivity.this::sameOrSubDomain)) return false;

            String fallback = tab.pendingHttpsOriginalUrl;
'''

new_block = '''        try {
            if (!HttpsNavigationFailurePolicy.passesPreflight(
                    httpsFirstEnabled,
                    view != null,
                    failedUrl,
                    errorCode,
                    MainActivity.this::isHttpsUrl,
                    MainActivity.this::isHttpsFallbackEligibleError)) return false;
            TabInfo tab = findTabByWebView(view);
            if (tab == null && view == webView) tab = getCurrentTab();
            if (tab == null || !HttpsNavigationFailurePolicy.hasRelatedPendingFailure(
                    tab.pendingHttpsOriginalUrl,
                    tab.pendingHttpsUpgradeUrl,
                    failedUrl,
                    MainActivity.this::hostOfUrl,
                    MainActivity.this::sameOrSubDomain)) return false;

            String fallback = tab.pendingHttpsOriginalUrl;
'''

if text.count(old_block) != 1:
    raise SystemExit(
        f"Expected one legacy HTTPS navigation failure preflight, found {text.count(old_block)}")

text = text.replace(old_block, new_block, 1)

if text.count("HttpsNavigationFailurePolicy.passesPreflight(") != 1:
    raise SystemExit("Expected one HTTPS failure preflight delegation")
if text.count("HttpsNavigationFailurePolicy.hasRelatedPendingFailure(") != 1:
    raise SystemExit("Expected one HTTPS pending failure delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity HTTPS navigation failure delegated")
