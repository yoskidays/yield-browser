from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "StrictCompatibilityHostPolicy.isKnownHost(" in text:
    print("Strict compatibility host delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean isKnownStrictCompatibilityHost(String host) {
        try {
            if (host == null || host.length() == 0) return false;
            String h = host.toLowerCase(Locale.US);
            if (h.startsWith("www.")) h = h.substring(4);
            for (String base : STRICT_COMPAT_HOSTS) {
                if (h.equals(base) || h.endsWith("." + base)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
'''

new_method = '''    private boolean isKnownStrictCompatibilityHost(String host) {
        return StrictCompatibilityHostPolicy.isKnownHost(host, STRICT_COMPAT_HOSTS);
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy strict compatibility host method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("StrictCompatibilityHostPolicy.isKnownHost(") != 1:
    raise SystemExit("Expected exactly one strict compatibility host delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity strict compatibility host matching delegated")
