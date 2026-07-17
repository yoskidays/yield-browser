from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "StrictCompatibilityUrlPolicy.isStrict(" in text:
    print("Strict compatibility URL delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean isStrictSiteCompatibilityUrl(String url) {
        try {
            String host = hostOfUrl(url);
            if (host == null || host.length() == 0) return false;
            if (isKnownStrictCompatibilityHost(host)) return true;
            return isSiteCompatibilityModeActiveForUrl(url);
        } catch (Exception e) {
            return false;
        }
    }
'''

new_method = '''    private boolean isStrictSiteCompatibilityUrl(String url) {
        try {
            return StrictCompatibilityUrlPolicy.isStrict(
                    hostOfUrl(url),
                    MainActivity.this::isKnownStrictCompatibilityHost,
                    () -> isSiteCompatibilityModeActiveForUrl(url));
        } catch (Exception e) {
            return false;
        }
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy strict compatibility URL method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("StrictCompatibilityUrlPolicy.isStrict(") != 1:
    raise SystemExit("Expected exactly one strict compatibility URL delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity strict compatibility URL decision delegated")
