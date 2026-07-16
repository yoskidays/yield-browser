from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "BrowserWebViewSettings.apply(webView" in text:
    print("WebView settings delegation already installed")
    raise SystemExit(0)


def replace_method(source: str, signature: str, replacement: str) -> str:
    if source.count(signature) != 1:
        raise SystemExit(
            f"Expected exactly one method {signature!r}, found {source.count(signature)}"
        )
    start = source.index(signature)
    brace = source.find("{", start)
    if brace < 0:
        raise SystemExit(f"Opening brace missing for {signature}")

    depth = 0
    in_string = False
    escaped = False
    quote = ""
    end = -1
    for index in range(brace, len(source)):
        char = source[index]
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
        raise SystemExit(f"Unclosed method {signature}")
    return source[:start] + replacement + source[end:]


private_block = '''        boolean privateProfile = dedicatedPrivateProfile || (owner != null && owner.privateTab);
        if (privateProfile) {
            try { fresh.setSaveEnabled(false); } catch (Exception ignored) {}
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { fresh.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS); }
                catch (Exception ignored) {}
            }
            try { fresh.clearHistory(); } catch (Exception ignored) {}
            try { fresh.clearFormData(); } catch (Exception ignored) {}
            try { fresh.clearCache(true); } catch (Exception ignored) {}
        }
'''
private_replacement = '''        boolean privateProfile = dedicatedPrivateProfile || (owner != null && owner.privateTab);
        BrowserWebViewSettings.prepareNewWebView(fresh, privateProfile);
'''
if text.count(private_block) != 1:
    raise SystemExit(
        f"Expected one private WebView initialization block, found {text.count(private_block)}"
    )
text = text.replace(private_block, private_replacement, 1)

apply_settings = '''    private void applyBrowserSettings() {
        if (webView == null) return;
        boolean privateProfile = isPrivateWebView(webView);
        boolean youtubePage = isYouTubePlaybackUrl(getEffectiveCurrentUrl());
        boolean nightActive = isNightModeActiveForCurrentSite();
        BrowserWebViewSettings.apply(
                webView,
                new BrowserWebViewSettings.Config(
                        privateProfile,
                        speedMode,
                        videoBufferBooster,
                        youtubePage,
                        videoBackgroundPlay,
                        adBlock,
                        adBlockPopupBlocker,
                        dataSaver,
                        textZoom,
                        desktopMode,
                        getMobileUserAgent(),
                        getDesktopUserAgent(),
                        nightActive,
                        nightActive ? COLOR_BG : Color.WHITE),
                this::applyAlgorithmicDarkening);
    }'''

mobile_profile = '''    private void applyMobileProfile(WebSettings settings) {
        BrowserWebViewSettings.applyMobileProfile(
                webView, settings, getMobileUserAgent());
    }'''

desktop_profile = '''    private void applyDesktopProfile(WebSettings settings) {
        BrowserWebViewSettings.applyDesktopProfile(
                webView, settings, getDesktopUserAgent());
    }'''

text = replace_method(
    text,
    "    private void applyBrowserSettings() {",
    apply_settings,
)
text = replace_method(
    text,
    "    private void applyMobileProfile(WebSettings settings) {",
    mobile_profile,
)
text = replace_method(
    text,
    "    private void applyDesktopProfile(WebSettings settings) {",
    desktop_profile,
)

if text.count("BrowserWebViewSettings.apply(webView") != 0:
    raise SystemExit("Unexpected compact settings call; guarded multiline call expected")
if text.count("BrowserWebViewSettings.apply(") != 1:
    raise SystemExit("Expected exactly one BrowserWebViewSettings.apply delegation")
if text.count("BrowserWebViewSettings.prepareNewWebView(") != 1:
    raise SystemExit("Expected exactly one private WebView preparation delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity WebView settings delegated")
