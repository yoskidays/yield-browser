from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "HttpsNavigationSuccessPolicy.evaluate(" in text:
    print("HTTPS navigation success delegation already installed")
    raise SystemExit(0)

old_method = '''    private void handleHttpsFirstNavigationSuccess(TabInfo tab, String finalUrl) {
        if (tab == null || !isHttpsUrl(finalUrl)) return;
        String original = tab.pendingHttpsOriginalUrl;
        if (original == null || original.length() == 0) {
            String finalHost = hostOfUrl(finalUrl);
            if (finalHost.equals(tab.httpsFallbackHost)) {
                tab.httpsFallbackHost = "";
                tab.httpsFallbackAllowedUntilMs = 0L;
            }
            return;
        }
        String expectedHost = hostOfUrl(tab.pendingHttpsUpgradeUrl);
        String finalHost = hostOfUrl(finalUrl);
        boolean related = HttpsHostRelationPolicy.areRelated(
                expectedHost,
                finalHost,
                MainActivity.this::sameOrSubDomain);
        if (related) upgradeBookmarksAfterHttpsSuccess(original, finalUrl);
        clearHttpsFirstPendingState(tab, related);
    }
'''

new_method = '''    private void handleHttpsFirstNavigationSuccess(TabInfo tab, String finalUrl) {
        if (tab == null) return;
        HttpsNavigationSuccessPolicy.Action action = HttpsNavigationSuccessPolicy.evaluate(
                finalUrl,
                tab.pendingHttpsOriginalUrl,
                tab.pendingHttpsUpgradeUrl,
                tab.httpsFallbackHost,
                MainActivity.this::isHttpsUrl,
                MainActivity.this::hostOfUrl,
                MainActivity.this::sameOrSubDomain);
        if (action == HttpsNavigationSuccessPolicy.Action.IGNORE
                || action == HttpsNavigationSuccessPolicy.Action.KEEP_FALLBACK) return;
        if (action == HttpsNavigationSuccessPolicy.Action.CLEAR_FALLBACK) {
            tab.httpsFallbackHost = "";
            tab.httpsFallbackAllowedUntilMs = 0L;
            return;
        }
        boolean related = action == HttpsNavigationSuccessPolicy.Action.COMPLETE_RELATED;
        if (related) upgradeBookmarksAfterHttpsSuccess(tab.pendingHttpsOriginalUrl, finalUrl);
        clearHttpsFirstPendingState(tab, related);
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy HTTPS navigation success method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("HttpsNavigationSuccessPolicy.evaluate(") != 1:
    raise SystemExit("Expected exactly one HTTPS navigation success delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity HTTPS navigation success delegated")
