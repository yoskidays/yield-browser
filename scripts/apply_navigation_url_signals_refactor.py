from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "NavigationUrlSignalPolicy.isExternalScheme(" in text:
    print("Navigation URL signal delegation already installed")
    raise SystemExit(0)

old_external = '''    private boolean isExternalSchemeUrl(String url) {
        if (url == null || url.trim().length() == 0) return false;
        String u = url.trim().toLowerCase(Locale.US);
        if (u.startsWith("http://") || u.startsWith("https://")) return false;
        if (u.startsWith("about:") || u.startsWith("javascript:") || u.startsWith("data:")
                || u.startsWith("blob:") || u.startsWith("file:")) return false;
        return u.matches("^[a-z][a-z0-9+.-]*:.*");
    }
'''
new_external = '''    private boolean isExternalSchemeUrl(String url) {
        return NavigationUrlSignalPolicy.isExternalScheme(url);
    }
'''

old_ad = '''    private boolean isLikelyAdClickUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);
        if (isMediaResourceUrl(u) || isYoutubeCoreUrl(u) || isTrustedDownloadIntentUrl(u)) return false;
        return isExternalSchemeUrl(u)
                || u.contains("utm_medium=affiliates")
                || u.contains("utm_source=an_")
                || u.contains("affiliate")
                || u.contains("aff_sub")
                || u.contains("deep_and_deferred")
                || u.contains("navigate_url=")
                || u.contains("reactpath")
                || u.contains("click_id")
                || u.contains("adclick")
                || u.contains("ad_click")
                || u.contains("adurl=")
                || u.contains("af_click")
                || u.contains("tracking_id")
                || u.contains("campaign_id")
                || u.startsWith("shopeeid:")
                || u.startsWith("lazada:")
                || u.startsWith("tokopedia:")
                || u.startsWith("intent:")
                || u.startsWith("market:");
    }
'''
new_ad = '''    private boolean isLikelyAdClickUrl(String url) {
        return NavigationUrlSignalPolicy.isLikelyAdClick(
                url,
                MainActivity.this::isMediaResourceUrl,
                MainActivity.this::isYoutubeCoreUrl,
                MainActivity.this::isTrustedDownloadIntentUrl);
    }
'''

if text.count(old_external) != 1:
    raise SystemExit(
        f"Expected one legacy external scheme method, found {text.count(old_external)}")
if text.count(old_ad) != 1:
    raise SystemExit(
        f"Expected one legacy likely-ad-click method, found {text.count(old_ad)}")

text = text.replace(old_external, new_external, 1)
text = text.replace(old_ad, new_ad, 1)

if text.count("NavigationUrlSignalPolicy.isExternalScheme(") != 1:
    raise SystemExit("Expected exactly one external scheme delegation")
if text.count("NavigationUrlSignalPolicy.isLikelyAdClick(") != 1:
    raise SystemExit("Expected exactly one likely-ad-click delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity navigation URL signals delegated")
