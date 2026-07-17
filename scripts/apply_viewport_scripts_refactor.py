from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "ViewportScriptPolicy.mobileResetScript()" in text:
    print("Viewport script delegation already installed")
    raise SystemExit(0)

mobile_start = "    private void injectMobileViewportReset() {\n"
desktop_apply = "\n    private void applyDesktopViewportIfNeeded() {\n"
desktop_start = "    private void injectDesktopViewportLock() {\n"
trusted_start = "\n    private void markTrustedMainFrameNavigation(String url) {\n"

for marker in (mobile_start, desktop_apply, desktop_start, trusted_start):
    if text.count(marker) != 1:
        raise SystemExit(f"Expected one marker {marker!r}, found {text.count(marker)}")

mobile_begin = text.index(mobile_start)
mobile_end = text.index(desktop_apply, mobile_begin)
new_mobile = '''    private void injectMobileViewportReset() {
        if (desktopMode || webView == null) return;
        runPageScript(ViewportScriptPolicy.mobileResetScript());
    }
'''
text = text[:mobile_begin] + new_mobile + text[mobile_end:]

desktop_begin = text.index(desktop_start)
desktop_end = text.index(trusted_start, desktop_begin)
new_desktop = '''    private void injectDesktopViewportLock() {
        if (!desktopMode || webView == null) return;
        runPageScript(ViewportScriptPolicy.desktopLockScript());
    }
'''
text = text[:desktop_begin] + new_desktop + text[desktop_end:]

if text.count("ViewportScriptPolicy.mobileResetScript()") != 1:
    raise SystemExit("Expected exactly one mobile viewport script delegation")
if text.count("ViewportScriptPolicy.desktopLockScript()") != 1:
    raise SystemExit("Expected exactly one desktop viewport script delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity viewport scripts delegated")
