from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "BrowserPageStartCoordinator.prepare(" in text:
    print("WebView page-start delegation already installed")
    raise SystemExit(0)

old = '''                if (view == webView && elementPickerActive) finishElementPicker(false);
                if (view != webView) {
                    TabInfo owner = findTabByWebView(view);
                    if (owner != null) {
                        String inactiveUrl = extractOriginalUrl(url) != null ? extractOriginalUrl(url) : url;
                        owner.currentPageUrlForRequest = inactiveUrl;
                    }
                    super.onPageStarted(view, url, favicon);
                    return;
                }
                TabInfo activeOwner = findTabByWebView(view);
                if (activeOwner == null) activeOwner = getCurrentTab();
                String safeBeforePageStarted = getTabReferenceUrl(activeOwner);
                if ((safeBeforePageStarted == null || safeBeforePageStarted.length() == 0) && lastSafeHttpUrl != null) safeBeforePageStarted = lastSafeHttpUrl;
                String startedUrl = extractOriginalUrl(url) != null ? extractOriginalUrl(url) : url;
                if (ReaderCompatibilityPolicy.isTransientBlankUrl(startedUrl)
                        && isShieldReaderOrCompatibilityContext(safeBeforePageStarted)) {
                    restoreAfterBlockedNavigation(view, startedUrl);
                    return;
                }
                boolean startedLegacySuspicious = isKnownPopupHost(startedUrl)
                        || isLikelyAdClickUrl(startedUrl)
                        || isAdUrl(startedUrl)
                        || isSuspiciousPopupNavigation(startedUrl, safeBeforePageStarted);
                if (shouldShieldBlockMainFrame(startedUrl, safeBeforePageStarted, false, startedLegacySuspicious)) {
                    restoreAfterBlockedNavigation(view, startedUrl);
                    return;
                }
                currentPageUrlForRequest = startedUrl;
                if (activeOwner != null) activeOwner.currentPageUrlForRequest = currentPageUrlForRequest;
                webHorizontalGestureGuard = false;
                webHorizontalGestureGuardHost = hostOfUrl(currentPageUrlForRequest);
                syncNightModeWebSettingsForUrl(currentPageUrlForRequest);
                boolean strictCompatibilityActive = isStrictSiteCompatibilityUrl(url);
                boolean reloadLoopGuarded = registerNavigationLoopGuard(url);
                boolean siteCompatibilityActive = isSiteCompatibilityModeActiveForUrl(url);
                if (strictCompatibilityActive) {
                    enableSiteCompatibilityModeForUrl(url);
                    applyPlainCompatibilitySettings();
                    scheduleCompatibilityLoadFallback(url);
                } else {
                    applyBrowserSettings();
                }
                if (!strictCompatibilityActive && !reloadLoopGuarded && !siteCompatibilityActive) {
                    if (desktopMode) {
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 250);
                    } else {
                        mainHandler.postDelayed(() -> applyMobileViewportIfNeeded(), 250);
                    }
                } else {
                    // v0.9.42: jangan stopLoading untuk seluruh host. Stop loading membuat halaman
                    // seperti lordborg.com blank saat user klik menu internal. Guard cukup mematikan
                    // injection/restore agresif, sementara load utama tetap dibiarkan selesai.
                    try { if (progressBar != null) progressBar.setVisibility(View.GONE); } catch (Exception ignored) {}
                    // v0.9.60: Compatibility mode tidak boleh mematikan Desktop Mode.
                    // Untuk situs model Lordborg, tetap terapkan desktop profile/viewport saat toggle Desktop ON,
                    // tapi hanya di domain compatibility agar situs lain yang sudah stabil tidak berubah.
                    if (desktopMode) {
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 280);
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 1200);
                        mainHandler.postDelayed(() -> applyDesktopViewportIfNeeded(), 2600);
                    } else {
                        mainHandler.postDelayed(() -> applyMobileViewportIfNeeded(), 280);
                    }
                }
'''

new = '''                if (view == webView && elementPickerActive) finishElementPicker(false);
                BrowserPageStartCoordinator.Preparation pageStart =
                        BrowserPageStartCoordinator.prepare(
                                webView,
                                view,
                                url,
                                lastSafeHttpUrl,
                                MainActivity.this::findTabByWebView,
                                MainActivity.this::getCurrentTab,
                                MainActivity.this::getTabReferenceUrl,
                                MainActivity.this::extractOriginalUrl,
                                ReaderCompatibilityPolicy::isTransientBlankUrl,
                                MainActivity.this::isShieldReaderOrCompatibilityContext,
                                MainActivity.this::isKnownPopupHost,
                                MainActivity.this::isLikelyAdClickUrl,
                                MainActivity.this::isAdUrl,
                                MainActivity.this::isSuspiciousPopupNavigation,
                                MainActivity.this::shouldShieldBlockMainFrame,
                                MainActivity.this::restoreAfterBlockedNavigation,
                                MainActivity.this::isStrictSiteCompatibilityUrl,
                                MainActivity.this::registerNavigationLoopGuard,
                                MainActivity.this::isSiteCompatibilityModeActiveForUrl);
                if (pageStart.inactiveView) {
                    super.onPageStarted(view, url, favicon);
                    return;
                }
                if (pageStart.restored) return;

                TabInfo activeOwner = pageStart.owner;
                String safeBeforePageStarted = pageStart.safeReferenceUrl;
                String startedUrl = pageStart.startedUrl;
                currentPageUrlForRequest = startedUrl;
                if (activeOwner != null) activeOwner.currentPageUrlForRequest = currentPageUrlForRequest;
                webHorizontalGestureGuard = false;
                webHorizontalGestureGuardHost = hostOfUrl(currentPageUrlForRequest);
                syncNightModeWebSettingsForUrl(currentPageUrlForRequest);

                BrowserPageStartCoordinator.applyProfile(
                        pageStart,
                        desktopMode,
                        () -> enableSiteCompatibilityModeForUrl(url),
                        MainActivity.this::applyPlainCompatibilitySettings,
                        () -> scheduleCompatibilityLoadFallback(url),
                        MainActivity.this::applyBrowserSettings,
                        () -> {
                            try {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                            } catch (Exception ignored) {
                            }
                        },
                        delay -> mainHandler.postDelayed(
                                MainActivity.this::applyDesktopViewportIfNeeded, delay),
                        delay -> mainHandler.postDelayed(
                                MainActivity.this::applyMobileViewportIfNeeded, delay));
'''

if text.count(old) != 1:
    raise SystemExit(f"Expected one legacy page-start block, found {text.count(old)}")

text = text.replace(old, new, 1)

if text.count("BrowserPageStartCoordinator.prepare(") != 1:
    raise SystemExit("Expected exactly one page-start coordinator delegation")
if text.count("BrowserPageStartCoordinator.applyProfile(") != 1:
    raise SystemExit("Expected exactly one page-start profile delegation")
if "boolean strictCompatibilityActive = isStrictSiteCompatibilityUrl(url);" in text:
    raise SystemExit("Legacy inline compatibility profile logic remained")

path.write_text(text, encoding="utf-8")
print("MainActivity WebView page-start handling delegated")
