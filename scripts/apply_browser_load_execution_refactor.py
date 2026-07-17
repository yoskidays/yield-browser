from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "BrowserLoadExecutionCoordinator.execute(" in text:
    print("Browser load execution delegation already installed")
    raise SystemExit(0)

old = '''        try {
            currentPageUrlForRequest = cleanUrl;
            if (activeLoadTab != null) activeLoadTab.currentPageUrlForRequest = cleanUrl;
            markTrustedMainFrameNavigation(cleanUrl);
            prepareTabForMainFrameNavigation(activeLoadTab, cleanUrl);

            // v0.9.46: untuk situs sensitif seperti lordborg.com, jangan pakai header buatan
            // dan jangan inject/restore agresif. Beberapa situs anti-security menganggap custom
            // Sec-CH/User-Agent header sebagai request tidak normal lalu loading/blank terus.
            if (isStrictSiteCompatibilityUrl(cleanUrl)) {
                enableSiteCompatibilityModeForUrl(cleanUrl);
                applyPlainCompatibilitySettings();
                loadCompatibilityUrlWithCurrentMode(cleanUrl);
                if (desktopMode) {
                    mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 350);
                    mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 1300);
                    mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 2800);
                }
                scheduleCompatibilityLoadFallback(cleanUrl);
                return;
            }

            applyBrowserSettings();
            Map<String, String> headers = BrowserLoadRequestPolicy.requestHeaders(
                    desktopMode, getMobileUserAgent(), getDesktopUserAgent());
            webView.loadUrl(cleanUrl, headers);
        } catch (Exception e) {
            try { webView.loadUrl(cleanUrl); } catch (Exception ignored) {}
        }
'''

new = '''        final String targetUrl = cleanUrl;
        BrowserLoadExecutionCoordinator.execute(
                targetUrl,
                isStrictSiteCompatibilityUrl(targetUrl),
                desktopMode,
                () -> {
                    currentPageUrlForRequest = targetUrl;
                    if (activeLoadTab != null) activeLoadTab.currentPageUrlForRequest = targetUrl;
                },
                () -> markTrustedMainFrameNavigation(targetUrl),
                () -> prepareTabForMainFrameNavigation(activeLoadTab, targetUrl),
                () -> enableSiteCompatibilityModeForUrl(targetUrl),
                MainActivity.this::applyPlainCompatibilitySettings,
                MainActivity.this::loadCompatibilityUrlWithCurrentMode,
                delay -> mainHandler.postDelayed(
                        MainActivity.this::applyDesktopViewportIfNeeded, delay),
                MainActivity.this::scheduleCompatibilityLoadFallback,
                MainActivity.this::applyBrowserSettings,
                () -> BrowserLoadRequestPolicy.requestHeaders(
                        desktopMode, getMobileUserAgent(), getDesktopUserAgent()),
                (target, headers) -> webView.loadUrl(target, headers),
                target -> webView.loadUrl(target));
'''

if text.count(old) != 1:
    raise SystemExit(f"Expected one legacy browser load execution block, found {text.count(old)}")
text = text.replace(old, new, 1)
if text.count("BrowserLoadExecutionCoordinator.execute(") != 1:
    raise SystemExit("Expected exactly one browser load execution delegation")
path.write_text(text, encoding="utf-8")
print("MainActivity browser load execution delegated")
