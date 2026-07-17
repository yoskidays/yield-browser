from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "MobileViewportResetCoordinator.schedule(" in text:
    print("Mobile viewport reset delegation already installed")
    raise SystemExit(0)

old = '''    private void scheduleMobileViewportReset() {
        if (desktopMode) return;
        int token = browserModeToken;
        mainHandler.postDelayed(() -> { if (token == browserModeToken && !desktopMode) applyMobileViewportIfNeeded(); }, 120);
        mainHandler.postDelayed(() -> { if (token == browserModeToken && !desktopMode) applyMobileViewportIfNeeded(); }, 500);
        mainHandler.postDelayed(() -> { if (token == browserModeToken && !desktopMode) applyMobileViewportIfNeeded(); }, 1200);
    }
'''

new = '''    private void scheduleMobileViewportReset() {
        MobileViewportResetCoordinator.schedule(
                desktopMode,
                browserModeToken,
                (action, delay) -> mainHandler.postDelayed(action, delay),
                () -> browserModeToken,
                () -> desktopMode,
                MainActivity.this::applyMobileViewportIfNeeded);
    }
'''

if text.count(old) != 1:
    raise SystemExit(f"Expected one legacy mobile viewport reset method, found {text.count(old)}")
text = text.replace(old, new, 1)
if text.count("MobileViewportResetCoordinator.schedule(") != 1:
    raise SystemExit("Expected exactly one mobile viewport reset delegation")
path.write_text(text, encoding="utf-8")
print("MainActivity mobile viewport reset delegated")
