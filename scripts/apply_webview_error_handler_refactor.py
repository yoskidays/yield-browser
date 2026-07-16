from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "BrowserWebErrorHandler.handle(" in text:
    print("WebView error handler delegation already installed")
    raise SystemExit(0)

signature = "            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {"
if text.count(signature) != 1:
    raise SystemExit(f"Expected one onReceivedError callback, found {text.count(signature)}")

start = text.index("            @Override\n" + signature)
brace = text.find("{", start)
depth = 0
in_string = False
escaped = False
quote = ""
end = -1
for index in range(brace, len(text)):
    char = text[index]
    if in_string:
        if escaped:
            escaped = False
        elif char == "\\":
            escaped = True
        elif char == quote:
            in_string = False
        continue
    if char in ('"', "'"):
        in_string = True
        quote = char
    elif char == "{":
        depth += 1
    elif char == "}":
        depth -= 1
        if depth == 0:
            end = index + 1
            break
if end < 0:
    raise SystemExit("Unclosed onReceivedError callback")

replacement = '''            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (BrowserWebErrorHandler.handle(
                        webView,
                        view,
                        request,
                        error,
                        adBlock,
                        lastSafeHttpUrl,
                        MainActivity.this::handleHttpsFirstMainFrameFailure,
                        MainActivity.this::isSiteCompatibilityModeActiveForUrl,
                        MainActivity.this::isExternalSchemeUrl,
                        MainActivity.this::isTrustedMainFrameNavigation,
                        MainActivity.this::isKnownPopupHost,
                        MainActivity.this::isLikelyAdClickUrl,
                        MainActivity.this::isAdUrl,
                        MainActivity.this::isSuspiciousPopupNavigation,
                        MainActivity.this::restoreAfterBlockedNavigation,
                        () -> smoothSearchTransitionActive,
                        MainActivity.this::finishSmoothSearchTransition)) {
                    return;
                }
                super.onReceivedError(view, request, error);
            }'''

text = text[:start] + replacement + text[end:]

if text.count("BrowserWebErrorHandler.handle(") != 1:
    raise SystemExit("Expected exactly one WebView error handler delegation")
if "errorText.contains(\"unknown_url_scheme\")" in text:
    raise SystemExit("Legacy inline unknown-scheme error logic remained")

path.write_text(text, encoding="utf-8")
print("MainActivity WebView error handling delegated")
