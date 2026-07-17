from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "HttpsBookmarkUpgradePolicy.isEligible(" in text:
    print("HTTPS bookmark upgrade delegation already installed")
    raise SystemExit(0)

old_method = '''    private void upgradeBookmarksAfterHttpsSuccess(String originalHttpUrl, String finalHttpsUrl) {
        if (!isHttpUrl(originalHttpUrl) || !isHttpsUrl(finalHttpsUrl)) return;
        boolean changed = false;
        for (BookmarkItemData item : bookmarkData) {
            if (item == null || !isHttpUrl(item.url)) continue;
            String candidate = buildHttpsUpgradeUrl(item.url);
            if (equivalentUrlIgnoringSchemeAndFragment(item.url, originalHttpUrl)
                    || equivalentUrlIgnoringSchemeAndFragment(candidate, finalHttpsUrl)) {
                item.url = finalHttpsUrl;
                changed = true;
            }
        }
        if (changed) saveBookmarkData();
    }
'''

new_method = '''    private void upgradeBookmarksAfterHttpsSuccess(String originalHttpUrl, String finalHttpsUrl) {
        if (!HttpsBookmarkUpgradePolicy.isEligible(
                originalHttpUrl,
                finalHttpsUrl,
                MainActivity.this::isHttpUrl,
                MainActivity.this::isHttpsUrl)) return;
        boolean changed = false;
        for (BookmarkItemData item : bookmarkData) {
            if (item == null || !HttpsBookmarkUpgradePolicy.shouldUpgrade(
                    item.url,
                    originalHttpUrl,
                    finalHttpsUrl,
                    MainActivity.this::isHttpUrl,
                    MainActivity.this::buildHttpsUpgradeUrl,
                    MainActivity.this::equivalentUrlIgnoringSchemeAndFragment)) continue;
            item.url = finalHttpsUrl;
            changed = true;
        }
        if (changed) saveBookmarkData();
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy HTTPS bookmark upgrade method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("HttpsBookmarkUpgradePolicy.isEligible(") != 1:
    raise SystemExit("Expected one bookmark eligibility delegation")
if text.count("HttpsBookmarkUpgradePolicy.shouldUpgrade(") != 1:
    raise SystemExit("Expected one bookmark upgrade decision delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity HTTPS bookmark upgrade decisions delegated")
