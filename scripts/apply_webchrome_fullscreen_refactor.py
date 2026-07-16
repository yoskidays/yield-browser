from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "BrowserChromeFullscreenHandler.show(" in text:
    print("WebChrome fullscreen delegation already installed")
    raise SystemExit(0)

old_show = '''            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreenVideoView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                fullscreenVideoView = view;
                fullscreenVideoCallback = callback;
                originalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();

                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.addView(fullscreenVideoView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

                if (topBarView != null) topBarView.setVisibility(View.GONE);
                if (bottomNavView != null) bottomNavView.setVisibility(View.GONE);
                moveVideoControlsToFullscreenOverlay();
                updateVideoModeToggleButton();
                checkAndShowVideoControls();
            }
'''

new_show = '''            @Override
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
'''

old_hide = '''            @Override
            public void onHideCustomView() {
                if (fullscreenVideoView == null) return;

                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.removeView(fullscreenVideoView);
                fullscreenVideoView = null;

                if (fullscreenVideoCallback != null) {
                    fullscreenVideoCallback.onCustomViewHidden();
                    fullscreenVideoCallback = null;
                }

                restoreAfterVideoFullscreen();
                updateVideoModeToggleButton();
            }
'''

new_hide = '''            @Override
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
'''

if text.count(old_show) != 1:
    raise SystemExit(f"Expected one legacy onShowCustomView block, found {text.count(old_show)}")
if text.count(old_hide) != 1:
    raise SystemExit(f"Expected one legacy onHideCustomView block, found {text.count(old_hide)}")

text = text.replace(old_show, new_show, 1)
text = text.replace(old_hide, new_hide, 1)

if text.count("BrowserChromeFullscreenHandler.show(") != 1:
    raise SystemExit("Expected exactly one fullscreen show delegation")
if text.count("BrowserChromeFullscreenHandler.hide(") != 1:
    raise SystemExit("Expected exactly one fullscreen hide delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity WebChrome fullscreen lifecycle delegated")
