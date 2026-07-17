from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "BrowserLoadRequestPolicy.requestHeaders(" in text:
    print("Browser load request policy delegation already installed")
    raise SystemExit(0)

old_trim = "        String cleanUrl = url.trim();\n"
new_trim = "        String cleanUrl = BrowserLoadRequestPolicy.trimInput(url);\n"

old_direct = '''        String lower = cleanUrl.toLowerCase(Locale.US);
        if (lower.startsWith("javascript:") || lower.startsWith("about:") || lower.startsWith("data:")) {
            webView.loadUrl(cleanUrl);
            return;
        }
'''
new_direct = '''        if (BrowserLoadRequestPolicy.isDirectWebViewUrl(cleanUrl)) {
            webView.loadUrl(cleanUrl);
            return;
        }
'''

old_headers = '''            Map<String, String> headers = new LinkedHashMap<>();
            if (desktopMode) {
                headers.put("User-Agent", getDesktopUserAgent());
                headers.put("Sec-CH-UA-Mobile", "?0");
                headers.put("Sec-CH-UA-Platform", "\\\"Windows\\\"");
            } else {
                headers.put("User-Agent", getMobileUserAgent());
                headers.put("Sec-CH-UA-Mobile", "?1");
                headers.put("Sec-CH-UA-Platform", "\\\"Android\\\"");
            }
            headers.put("Upgrade-Insecure-Requests", "1");
            headers.put("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7");
            webView.loadUrl(cleanUrl, headers);
'''
new_headers = '''            Map<String, String> headers = BrowserLoadRequestPolicy.requestHeaders(
                    desktopMode, getMobileUserAgent(), getDesktopUserAgent());
            webView.loadUrl(cleanUrl, headers);
'''

for label, old in (("trim", old_trim), ("direct URL", old_direct), ("headers", old_headers)):
    if text.count(old) != 1:
        raise SystemExit(f"Expected one legacy {label} block, found {text.count(old)}")

text = text.replace(old_trim, new_trim, 1)
text = text.replace(old_direct, new_direct, 1)
text = text.replace(old_headers, new_headers, 1)

if text.count("BrowserLoadRequestPolicy.trimInput(") != 1:
    raise SystemExit("Expected exactly one trim delegation")
if text.count("BrowserLoadRequestPolicy.isDirectWebViewUrl(") != 1:
    raise SystemExit("Expected exactly one direct URL delegation")
if text.count("BrowserLoadRequestPolicy.requestHeaders(") != 1:
    raise SystemExit("Expected exactly one header delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity browser load request preparation delegated")
