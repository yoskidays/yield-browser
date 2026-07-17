from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "CompatibilityContentAssetPolicy.isFontAsset(" in text:
    print("Compatibility content-asset delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean isCompatibilityContentAssetUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);
        return isImageResourceUrl(u)
                || isMediaResourceUrl(u)
                || u.matches(".*\\\\.(?:woff2?|ttf|otf)(?:$|[?#]).*");
    }
'''

new_method = '''    private boolean isCompatibilityContentAssetUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase(Locale.US);
        if (isImageResourceUrl(u) || isMediaResourceUrl(u)) return true;
        return CompatibilityContentAssetPolicy.isFontAsset(u);
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy compatibility content-asset method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("CompatibilityContentAssetPolicy.isFontAsset(") != 1:
    raise SystemExit("Expected exactly one compatibility font-asset delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity compatibility content-asset detection delegated")
