# Stage 92 guarded swipe-navigation and loading-transition extraction.
from pathlib import Path

PATH = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")


def replace_method(source: str, signature: str, replacement: str) -> str:
    start = source.find(signature)
    if start < 0:
        raise SystemExit(f"Missing method signature: {signature}")
    brace = source.find("{", start)
    depth = 0
    end = -1
    for index in range(brace, len(source)):
        char = source[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                end = index + 1
                break
    if end < 0:
        raise SystemExit(f"Unbalanced method: {signature}")
    return source[:start] + replacement + source[end:]


text = PATH.read_text()
old_ui_fields = """    private FrameLayout navigationLoadingOverlay;
    private boolean smoothSearchTransitionActive = false;"""
new_ui_fields = """    private FrameLayout navigationLoadingOverlay;
    private NavigationTransitionController navigationTransitionController;
    private SwipeNavigationController swipeNavigationController;"""
if old_ui_fields not in text:
    raise SystemExit("Missing navigation UI fields")
text = text.replace(old_ui_fields, new_ui_fields, 1)

old_swipe_fields = """    // ===== Navigation gesture state =====
    private float swipeStartX = 0f;
    private float swipeStartY = 0f;
    private long swipeStartTime = 0L;
    // v0.9.69: situs desktop/horizontal-scroll seperti h-metrics.com tidak boleh"""
new_swipe_fields = """    // ===== Navigation gesture state =====
    // v0.9.69: situs desktop/horizontal-scroll seperti h-metrics.com tidak boleh"""
if old_swipe_fields not in text:
    raise SystemExit("Missing swipe state fields")
text = text.replace(old_swipe_fields, new_swipe_fields, 1)

old_overlay = """        navigationLoadingOverlay = createNavigationLoadingOverlay();
        navigationLoadingOverlay.setVisibility(View.GONE);
        contentFrame.addView(navigationLoadingOverlay, new FrameLayout.LayoutParams(-1, -1));"""
new_overlay = """        navigationLoadingOverlay = NavigationTransitionController.createOverlay(this);
        navigationLoadingOverlay.setVisibility(View.GONE);
        contentFrame.addView(navigationLoadingOverlay, new FrameLayout.LayoutParams(-1, -1));
        navigationTransitionController = new NavigationTransitionController(
                navigationLoadingOverlay, homeScroll, webView);"""
if old_overlay not in text:
    raise SystemExit("Missing navigation overlay initialization")
text = text.replace(old_overlay, new_overlay, 1)
text = text.replace("        installSwipeNavigation(root);", "        initializeSwipeNavigation(root);", 1)

initializer = """    private void initializeSwipeNavigation(View root) {
        swipeNavigationController = new SwipeNavigationController(
                this,
                homeScroll,
                webView,
                MainActivity.this::shouldProtectWebHorizontalSwipeGesture,
                MainActivity.this::restoreHiddenWebPage,
                MainActivity.this::showHome);
        swipeNavigationController.install(root);
    }"""
text = replace_method(text, "    private void installSwipeNavigation(View root) {", initializer)
text = replace_method(text, "    private boolean handleSwipeTouch(MotionEvent event) {", "")
text = replace_method(text, "    private void navigateSwipeBack() {", "")
text = replace_method(text, "    private void navigateSwipeForward() {", "")
text = replace_method(text, "    private FrameLayout createNavigationLoadingOverlay() {", "")
text = replace_method(
        text,
        "    private void startSmoothSearchTransition() {",
        """    private void startSmoothSearchTransition() {
        if (navigationTransitionController != null) navigationTransitionController.start();
    }""")
text = replace_method(
        text,
        "    private void finishSmoothSearchTransition() {",
        """    private void finishSmoothSearchTransition() {
        if (navigationTransitionController != null) navigationTransitionController.finish();
    }""")
text = replace_method(
        text,
        "    private void cancelSmoothSearchTransition() {",
        """    private void cancelSmoothSearchTransition() {
        if (navigationTransitionController != null) navigationTransitionController.cancel();
    }

    private boolean isSmoothSearchTransitionActive() {
        return navigationTransitionController != null && navigationTransitionController.isActive();
    }""")
text = text.replace("smoothSearchTransitionActive", "isSmoothSearchTransitionActive()")
PATH.write_text(text)
