from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "BrowserModeUrlNormalizer.normalize(" in text:
    print("Browser mode URL normalizer delegation already installed")
    raise SystemExit(0)

old_method = '''    private String normalizeUrlForCurrentBrowserMode(String url) {
        try {
            if (url == null) return null;
            String clean = extractOriginalUrl(url);
            if (clean == null || clean.trim().length() == 0) clean = url;
            if (!isHttpOrHttpsUrl(clean)) return clean;
            if (isYouTubePageUrl(clean)) {
                if (desktopMode) {
                    clean = clean.replace("https://m.youtube.com", "https://www.youtube.com")
                            .replace("http://m.youtube.com", "https://www.youtube.com");
                } else {
                    clean = clean.replace("https://www.youtube.com", "https://m.youtube.com")
                            .replace("http://www.youtube.com", "https://m.youtube.com")
                            .replace("https://youtube.com", "https://m.youtube.com")
                            .replace("http://youtube.com", "https://m.youtube.com");
                }
            }
            return clean;
        } catch (Exception e) {
            return url;
        }
    }
'''

new_method = '''    private String normalizeUrlForCurrentBrowserMode(String url) {
        return BrowserModeUrlNormalizer.normalize(
                url,
                desktopMode,
                MainActivity.this::extractOriginalUrl,
                MainActivity.this::isHttpOrHttpsUrl,
                MainActivity.this::isYouTubePageUrl);
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy browser mode URL normalizer, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("BrowserModeUrlNormalizer.normalize(") != 1:
    raise SystemExit("Expected exactly one browser mode URL normalizer delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity browser mode URL normalization delegated")
