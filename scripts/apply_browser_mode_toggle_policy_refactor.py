from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "BrowserModeTogglePolicy.plan(" in text:
    print("Browser mode toggle policy delegation already installed")
    raise SystemExit(0)

old = '''    private void toggleDesktopModeSafely() {
        boolean previousDesktopMode = desktopMode;
        try {
            String targetUrl = getSafeReloadUrlForModeChange();
            boolean wasShowingWeb = webView != null && webView.getVisibility() == View.VISIBLE;
            desktopMode = !desktopMode;
            targetUrl = normalizeUrlForCurrentBrowserMode(targetUrl);
            saveSettings();

            if (targetUrl != null && targetUrl.length() > 0) {
                addressBar.setText(targetUrl);
            }

            if (wasShowingWeb && targetUrl != null && targetUrl.length() > 0) {
                // v0.9.40: Browser-professional style mode switch.
                // Jangan pakai reload biasa, karena WebView/Google bisa mempertahankan DOM lama
                // sehingga Desktop ON/OFF baru terasa setelah pencarian baru.
                // Solusinya: buat ulang WebView, terapkan profile mode dulu, baru request ulang URL.
                hardReloadUrlWithCurrentBrowserMode(targetUrl, true);
            } else {
                applyBrowserSettings();
                if (!wasShowingWeb) showHome();
            }

            QuietToast.makeText(this, desktopMode ? "Desktop mode aktif" : "Mode mobile aktif", QuietToast.LENGTH_SHORT).show();
        } catch (Exception e) {
            desktopMode = previousDesktopMode;
            try {
                applyBrowserSettings();
                saveSettings();
            } catch (Exception ignored) {}
        }
    }
'''

new = '''    private void toggleDesktopModeSafely() {
        boolean previousDesktopMode = desktopMode;
        try {
            String targetUrl = getSafeReloadUrlForModeChange();
            boolean wasShowingWeb = webView != null && webView.getVisibility() == View.VISIBLE;
            desktopMode = BrowserModeTogglePolicy.nextDesktopMode(desktopMode);
            targetUrl = normalizeUrlForCurrentBrowserMode(targetUrl);
            saveSettings();

            BrowserModeTogglePolicy.Plan plan = BrowserModeTogglePolicy.plan(
                    desktopMode, wasShowingWeb, targetUrl);
            if (plan.updateAddressBar) addressBar.setText(targetUrl);

            if (plan.hardReload) {
                hardReloadUrlWithCurrentBrowserMode(targetUrl, true);
            } else {
                if (plan.applySettings) applyBrowserSettings();
                if (plan.showHome) showHome();
            }

            QuietToast.makeText(
                    this, plan.statusMessage, QuietToast.LENGTH_SHORT).show();
        } catch (Exception e) {
            desktopMode = previousDesktopMode;
            try {
                applyBrowserSettings();
                saveSettings();
            } catch (Exception ignored) {}
        }
    }
'''

if text.count(old) != 1:
    raise SystemExit(f"Expected one legacy browser mode toggle method, found {text.count(old)}")
text = text.replace(old, new, 1)
if text.count("BrowserModeTogglePolicy.plan(") != 1:
    raise SystemExit("Expected exactly one browser mode toggle plan delegation")
path.write_text(text, encoding="utf-8")
print("MainActivity browser mode toggle decisions delegated")
