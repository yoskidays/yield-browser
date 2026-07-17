from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "BrowserModeReloadSelector.select(" in text:
    print("Browser mode reload URL delegation already installed")
    raise SystemExit(0)

old_selector = '''    private String getSafeReloadUrlForModeChange() {
        String currentWebUrl = webView != null ? webView.getUrl() : "";
        String currentAddressUrl = addressBar != null ? addressBar.getText().toString() : "";
        String[] candidates = new String[]{
                currentWebUrl,
                currentAddressUrl,
                lastSafeHttpUrl
        };

        for (int i = 0; i < candidates.length; i++) {
            String candidate = candidates[i];
            String clean = extractOriginalUrl(candidate);
            if (clean == null || clean.trim().length() == 0) clean = candidate;
            boolean explicitCurrentPage = i < 2;
            if (isSafeUrlForModeReload(clean, explicitCurrentPage)) return clean;
        }
        return null;
    }
'''

new_selector = '''    private String getSafeReloadUrlForModeChange() {
        String currentWebUrl = webView != null ? webView.getUrl() : "";
        String currentAddressUrl = addressBar != null ? addressBar.getText().toString() : "";
        return BrowserModeReloadSelector.select(
                new String[]{currentWebUrl, currentAddressUrl, lastSafeHttpUrl},
                MainActivity.this::extractOriginalUrl,
                MainActivity.this::isSafeUrlForModeReload);
    }
'''

old_safety = '''    private boolean isSafeUrlForModeReload(String url, boolean explicitCurrentPage) {
        if (!isHttpOrHttpsUrl(url)) return false;
        if (isExternalSchemeUrl(url)) return false;
        if (isImageResourceUrl(url) || isMediaResourceUrl(url)) return false;
        String lower = url == null ? "" : url.toLowerCase(Locale.US);
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("about:")) return false;
        // v0.9.67: Desktop/Mobile toggle adalah aksi user eksplisit.
        // Untuk halaman yang sedang dibuka seperti invest-tracing.com, jangan ditolak hanya
        // karena host-nya masuk daftar popup/ad. Jika ditolak, toggle tidak reload dan UA lama tersisa.
        if (explicitCurrentPage) return true;
        if (isLikelyAdClickUrl(url)) return false;
        if (isKnownPopupHost(url)) return false;
        if (isAdUrl(url)) return false;
        return true;
    }
'''

new_safety = '''    private boolean isSafeUrlForModeReload(String url, boolean explicitCurrentPage) {
        boolean baseSafe = BrowserModeReloadPolicy.isBaseSafe(
                isHttpOrHttpsUrl(url),
                isExternalSchemeUrl(url),
                isImageResourceUrl(url),
                isMediaResourceUrl(url),
                BrowserLoadRequestPolicy.isDirectWebViewUrl(url));
        if (!baseSafe) return false;
        if (explicitCurrentPage) return true;
        return BrowserModeReloadPolicy.isFallbackClassificationSafe(
                isLikelyAdClickUrl(url),
                isKnownPopupHost(url),
                isAdUrl(url));
    }
'''

if text.count(old_selector) != 1:
    raise SystemExit(f"Expected one legacy mode reload selector, found {text.count(old_selector)}")
if text.count(old_safety) != 1:
    raise SystemExit(f"Expected one legacy mode reload safety method, found {text.count(old_safety)}")

text = text.replace(old_selector, new_selector, 1)
text = text.replace(old_safety, new_safety, 1)

if text.count("BrowserModeReloadSelector.select(") != 1:
    raise SystemExit("Expected exactly one mode reload selector delegation")
if text.count("BrowserModeReloadPolicy.isBaseSafe(") != 1:
    raise SystemExit("Expected exactly one base safety delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity browser mode reload URL selection delegated")
