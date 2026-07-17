from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "BrowserUrlIdentityPolicy.normalizedHost(" in text:
    print("Browser URL identity delegation already installed")
    raise SystemExit(0)

old_host = '''    private String hostOfUrl(String url) {
        try {
            String clean = extractOriginalUrl(url);
            if (clean == null || clean.trim().length() == 0) clean = url;
            Uri uri = Uri.parse(clean);
            String host = uri.getHost();
            if (host == null) return "";
            host = host.toLowerCase(Locale.US);
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception e) {
            return "";
        }
    }
'''

new_host = '''    private String hostOfUrl(String url) {
        return BrowserUrlIdentityPolicy.normalizedHost(
                url,
                MainActivity.this::extractOriginalUrl,
                value -> Uri.parse(value).getHost());
    }
'''

old_key = '''    private String navigationLoopKey(String url) {
        try {
            String clean = extractOriginalUrl(url);
            if (clean == null || clean.trim().length() == 0) clean = url;
            if (clean == null) return "";
            int hash = clean.indexOf('#');
            if (hash >= 0) clean = clean.substring(0, hash);
            return clean.trim().toLowerCase(Locale.US);
        } catch (Exception e) {
            return url == null ? "" : url.trim().toLowerCase(Locale.US);
        }
    }
'''

new_key = '''    private String navigationLoopKey(String url) {
        return BrowserUrlIdentityPolicy.navigationLoopKey(
                url, MainActivity.this::extractOriginalUrl);
    }
'''

if text.count(old_host) != 1:
    raise SystemExit(f"Expected one legacy hostOfUrl method, found {text.count(old_host)}")
if text.count(old_key) != 1:
    raise SystemExit(f"Expected one legacy navigationLoopKey method, found {text.count(old_key)}")

text = text.replace(old_host, new_host, 1)
text = text.replace(old_key, new_key, 1)

if text.count("BrowserUrlIdentityPolicy.normalizedHost(") != 1:
    raise SystemExit("Expected exactly one normalizedHost delegation")
if text.count("BrowserUrlIdentityPolicy.navigationLoopKey(") != 1:
    raise SystemExit("Expected exactly one navigationLoopKey delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity URL identity helpers delegated")
