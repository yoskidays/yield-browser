from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "CompatibilityLoadRequestPolicy.requestHeaders(" in text:
    print("Compatibility load request delegation already installed")
    raise SystemExit(0)

old_block = '''            if (desktopMode) {
                // Minimal desktop request untuk situs compatibility: cukup UA desktop + bahasa.
                // Hindari header Sec-CH custom yang dulu bisa memicu security/blank di situs berat iklan.
                Map<String, String> headers = new LinkedHashMap<>();
                headers.put("User-Agent", getDesktopUserAgent());
                headers.put("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7");
                webView.loadUrl(cleanUrl, headers);
            } else {
                webView.loadUrl(cleanUrl);
            }
'''

new_block = '''            Map<String, String> headers = CompatibilityLoadRequestPolicy.requestHeaders(
                    desktopMode,
                    desktopMode ? getDesktopUserAgent() : null);
            if (headers.isEmpty()) webView.loadUrl(cleanUrl);
            else webView.loadUrl(cleanUrl, headers);
'''

if text.count(old_block) != 1:
    raise SystemExit(
        f"Expected one legacy compatibility load request block, found {text.count(old_block)}")

text = text.replace(old_block, new_block, 1)

if text.count("CompatibilityLoadRequestPolicy.requestHeaders(") != 1:
    raise SystemExit("Expected exactly one compatibility load request delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity compatibility load request delegated")
