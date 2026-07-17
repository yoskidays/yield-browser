from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "ViewportModeApplyPolicy.mode(" in text:
    print("Viewport mode apply delegation already installed")
    raise SystemExit(0)

old_method = '''    private void applyViewportForCurrentMode() {
        if (webView == null) return;
        applyBrowserSettings();
        if (desktopMode) applyDesktopViewportIfNeeded();
        else applyMobileViewportIfNeeded();
    }
'''

new_method = '''    private void applyViewportForCurrentMode() {
        ViewportModeApplyPolicy.Mode mode = ViewportModeApplyPolicy.mode(
                webView != null, desktopMode);
        if (mode == ViewportModeApplyPolicy.Mode.NONE) return;
        applyBrowserSettings();
        if (mode == ViewportModeApplyPolicy.Mode.DESKTOP) applyDesktopViewportIfNeeded();
        else applyMobileViewportIfNeeded();
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy viewport mode apply method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("ViewportModeApplyPolicy.mode(") != 1:
    raise SystemExit("Expected exactly one viewport mode apply delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity viewport mode application delegated")
