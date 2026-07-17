from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "HttpsNavigationPreparePolicy.prepare(" in text:
    print("HTTPS navigation prepare delegation already installed")
    raise SystemExit(0)

old_method = '''    private String prepareHttpsFirstNavigation(String rawUrl, TabInfo tab) {
        if (rawUrl == null) return null;
        String clean = rawUrl.trim();
        if (!httpsFirstEnabled || !isHttpUrl(clean) || isHttpsFirstExemptUrl(clean)) return clean;

        if (tab != null && tab.httpsFallbackInProgress) {
            String expected = tab.pendingHttpsOriginalUrl;
            tab.httpsFallbackInProgress = false;
            if (expected != null && expected.equals(clean)) {
                tab.pendingHttpsOriginalUrl = "";
                tab.pendingHttpsUpgradeUrl = "";
                tab.pendingHttpsStartedAtMs = 0L;
                return clean;
            }
        }
        if (isHttpsFallbackGuardActive(tab, clean)) return clean;

        String secure = buildHttpsUpgradeUrl(clean);
        if (secure == null || secure.equals(clean)) return clean;
        if (tab != null) {
            tab.pendingHttpsOriginalUrl = clean;
            tab.pendingHttpsUpgradeUrl = secure;
            tab.pendingHttpsStartedAtMs = System.currentTimeMillis();
        }
        return secure;
    }
'''

new_method = '''    private String prepareHttpsFirstNavigation(String rawUrl, TabInfo tab) {
        HttpsNavigationPreparePolicy.Result result = HttpsNavigationPreparePolicy.prepare(
                rawUrl,
                httpsFirstEnabled,
                tab != null,
                tab != null && tab.httpsFallbackInProgress,
                tab != null ? tab.pendingHttpsOriginalUrl : "",
                MainActivity.this::isHttpUrl,
                MainActivity.this::isHttpsFirstExemptUrl,
                url -> isHttpsFallbackGuardActive(tab, url),
                MainActivity.this::buildHttpsUpgradeUrl);
        if (tab != null) {
            if (result.consumeFallbackInProgress) tab.httpsFallbackInProgress = false;
            if (result.clearPendingState) {
                tab.pendingHttpsOriginalUrl = "";
                tab.pendingHttpsUpgradeUrl = "";
                tab.pendingHttpsStartedAtMs = 0L;
            } else if (result.startPendingState) {
                tab.pendingHttpsOriginalUrl = rawUrl.trim();
                tab.pendingHttpsUpgradeUrl = result.targetUrl;
                tab.pendingHttpsStartedAtMs = System.currentTimeMillis();
            }
        }
        return result.targetUrl;
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy HTTPS navigation prepare method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("HttpsNavigationPreparePolicy.prepare(") != 1:
    raise SystemExit("Expected exactly one HTTPS navigation prepare delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity HTTPS navigation preparation delegated")
