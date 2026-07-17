from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "new BrowserChromeClient(" in text:
    print("BrowserChromeClient delegation already installed")
    raise SystemExit(0)

old = '''        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                BrowserChromeProgressHandler.handle(
                        webView,
                        view,
                        newProgress,
                        homeScroll != null && homeScroll.getVisibility() == View.VISIBLE,
                        progressBar);
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                BrowserChromeFullscreenHandler.show(
                        fullscreenVideoView,
                        view,
                        callback,
                        getWindow(),
                        topBarView,
                        bottomNavView,
                        (fullscreenView, fullscreenCallback, originalVisibility) -> {
                            fullscreenVideoView = fullscreenView;
                            fullscreenVideoCallback = fullscreenCallback;
                            originalSystemUiVisibility = originalVisibility;
                        },
                        MainActivity.this::setRequestedOrientation,
                        MainActivity.this::moveVideoControlsToFullscreenOverlay,
                        MainActivity.this::updateVideoModeToggleButton,
                        MainActivity.this::checkAndShowVideoControls);
            }

            @Override
            public void onHideCustomView() {
                BrowserChromeFullscreenHandler.hide(
                        fullscreenVideoView,
                        fullscreenVideoCallback,
                        getWindow(),
                        () -> fullscreenVideoView = null,
                        () -> fullscreenVideoCallback = null,
                        MainActivity.this::restoreAfterVideoFullscreen,
                        MainActivity.this::updateVideoModeToggleButton);
            }
        });
'''

new = '''        webView.setWebChromeClient(new BrowserChromeClient(
                (view, newProgress) -> BrowserChromeProgressHandler.handle(
                        webView,
                        view,
                        newProgress,
                        homeScroll != null && homeScroll.getVisibility() == View.VISIBLE,
                        progressBar),
                (view, callback) -> BrowserChromeFullscreenHandler.show(
                        fullscreenVideoView,
                        view,
                        callback,
                        getWindow(),
                        topBarView,
                        bottomNavView,
                        (fullscreenView, fullscreenCallback, originalVisibility) -> {
                            fullscreenVideoView = fullscreenView;
                            fullscreenVideoCallback = fullscreenCallback;
                            originalSystemUiVisibility = originalVisibility;
                        },
                        MainActivity.this::setRequestedOrientation,
                        MainActivity.this::moveVideoControlsToFullscreenOverlay,
                        MainActivity.this::updateVideoModeToggleButton,
                        MainActivity.this::checkAndShowVideoControls),
                () -> BrowserChromeFullscreenHandler.hide(
                        fullscreenVideoView,
                        fullscreenVideoCallback,
                        getWindow(),
                        () -> fullscreenVideoView = null,
                        () -> fullscreenVideoCallback = null,
                        MainActivity.this::restoreAfterVideoFullscreen,
                        MainActivity.this::updateVideoModeToggleButton)));
'''

if text.count(old) != 1:
    raise SystemExit(f"Expected one anonymous WebChromeClient block, found {text.count(old)}")
text = text.replace(old, new, 1)
if text.count("new BrowserChromeClient(") != 1:
    raise SystemExit("Expected exactly one dedicated BrowserChromeClient")
path.write_text(text, encoding="utf-8")
print("MainActivity anonymous WebChromeClient extracted")
