from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "UrlSchemePolicy.isHttpOrHttps(" in text:
    print("URL scheme policy delegation already installed")
    raise SystemExit(0)

old_combined = '''    private boolean isHttpOrHttpsUrl(String url) {
        if (url == null) return false;
        String u = url.trim().toLowerCase(Locale.US);
        return u.startsWith("http://") || u.startsWith("https://");
    }
'''
new_combined = '''    private boolean isHttpOrHttpsUrl(String url) {
        return UrlSchemePolicy.isHttpOrHttps(url);
    }
'''

old_http = '''    private boolean isHttpUrl(String url) {
        return url != null && url.trim().toLowerCase(Locale.US).startsWith("http://");
    }
'''
new_http = '''    private boolean isHttpUrl(String url) {
        return UrlSchemePolicy.isHttp(url);
    }
'''

old_https = '''    private boolean isHttpsUrl(String url) {
        return url != null && url.trim().toLowerCase(Locale.US).startsWith("https://");
    }
'''
new_https = '''    private boolean isHttpsUrl(String url) {
        return UrlSchemePolicy.isHttps(url);
    }
'''

for name, old in (("combined", old_combined), ("http", old_http), ("https", old_https)):
    if text.count(old) != 1:
        raise SystemExit(f"Expected one legacy {name} scheme method, found {text.count(old)}")

text = text.replace(old_combined, new_combined, 1)
text = text.replace(old_http, new_http, 1)
text = text.replace(old_https, new_https, 1)

if text.count("UrlSchemePolicy.isHttpOrHttps(") != 1:
    raise SystemExit("Expected one combined scheme delegation")
if text.count("UrlSchemePolicy.isHttp(") != 1:
    raise SystemExit("Expected one HTTP scheme delegation")
if text.count("UrlSchemePolicy.isHttps(") != 1:
    raise SystemExit("Expected one HTTPS scheme delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity URL scheme helpers delegated")
