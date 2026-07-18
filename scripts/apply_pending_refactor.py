# Stage 89 guarded browser-shell UI extraction.
from pathlib import Path

PATH = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")


def replace_between(source: str, start_marker: str, end_marker: str, replacement: str) -> str:
    start = source.find(start_marker)
    end = source.find(end_marker, start + len(start_marker))
    if start < 0 or end < 0:
        raise SystemExit(f"Missing guarded markers: {start_marker} -> {end_marker}")
    return source[:start] + replacement + "\n\n" + source[end:]


text = PATH.read_text()
field_marker = "    private TextView tabsCountText;\n"
field_replacement = field_marker + "    private BrowserShellUi browserShellUi;\n"
if field_marker not in text:
    raise SystemExit("Missing browser-shell field marker")
text = text.replace(field_marker, field_replacement, 1)

old_on_create = """        topBarView = createTopBar();
        root.addView(topBarView, new LinearLayout.LayoutParams(-1, -2));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);"""
new_on_create = """        initializeBrowserShellUi();
        topBarView = browserShellUi.createTopBar();
        addressBar = browserShellUi.addressBar();
        reloadButton = browserShellUi.reloadButton();
        bookmarkButton = browserShellUi.bookmarkButton();
        translateButton = browserShellUi.translateButton();
        root.addView(topBarView, new LinearLayout.LayoutParams(-1, -2));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);"""
if old_on_create not in text:
    raise SystemExit("Missing top-bar creation block")
text = text.replace(old_on_create, new_on_create, 1)

old_home = """        homeScroll = createHomeContent();
        contentFrame.addView(homeScroll, new FrameLayout.LayoutParams(-1, -1));"""
new_home = """        homeScroll = browserShellUi.createHomeContent();
        homeSearchInput = browserShellUi.homeSearchInput();
        contentFrame.addView(homeScroll, new FrameLayout.LayoutParams(-1, -1));"""
if old_home not in text:
    raise SystemExit("Missing home creation block")
text = text.replace(old_home, new_home, 1)

old_bottom = """        bottomNavView = createBottomNav();
        root.addView(bottomNavView, new LinearLayout.LayoutParams(-1, dp(64)));"""
new_bottom = """        bottomNavView = browserShellUi.createBottomNav(tabCount);
        tabsCountText = browserShellUi.tabsCountText();
        root.addView(bottomNavView, new LinearLayout.LayoutParams(-1, dp(64)));"""
if old_bottom not in text:
    raise SystemExit("Missing bottom navigation block")
text = text.replace(old_bottom, new_bottom, 1)

replacement = """    private void initializeBrowserShellUi() {
        browserShellUi = new BrowserShellUi(
                this,
                dedicatedPrivateProfile,
                shortcutsData,
                new BrowserShellUi.Host() {
                    @Override
                    public void hideKeyboardAndClearFocus(View view) {
                        MainActivity.this.hideKeyboardAndClearFocus(view);
                    }

                    @Override
                    public void openAddressBarUrl() {
                        MainActivity.this.openAddressBarUrl();
                    }

                    @Override
                    public void openHomeSearchUrl() {
                        MainActivity.this.openHomeSearchUrl();
                    }

                    @Override
                    public void reloadCurrentWebsite() {
                        MainActivity.this.reloadCurrentWebsite();
                    }

                    @Override
                    public void toggleBookmark() {
                        MainActivity.this.toggleBookmark();
                    }

                    @Override
                    public void toggleTranslate() {
                        MainActivity.this.toggleTranslate();
                    }

                    @Override
                    public void showQuickMenu() {
                        MainActivity.this.showQuickMenu();
                    }

                    @Override
                    public void openNormalBrowserSpace() {
                        MainActivity.this.openNormalBrowserSpace();
                    }

                    @Override
                    public void newTabInCurrentProfile() {
                        MainActivity.this.newTabInCurrentProfile();
                    }

                    @Override
                    public void navigateCurrentTabHome() {
                        MainActivity.this.navigateCurrentTabHome();
                    }

                    @Override
                    public void showBookmarkList() {
                        MainActivity.this.showBookmarkList();
                    }

                    @Override
                    public void showTabsPanel() {
                        MainActivity.this.showTabsPanel();
                    }

                    @Override
                    public String currentUrl() {
                        return MainActivity.this.getEffectiveCurrentUrl();
                    }

                    @Override
                    public String normalizeShortcutUrl(String value) {
                        return MainActivity.this.normalizeShortcutUrl(value);
                    }

                    @Override
                    public String guessLabelFromUrl(String url) {
                        return MainActivity.this.guessLabelFromUrl(url);
                    }

                    @Override
                    public void saveShortcuts() {
                        MainActivity.this.saveShortcuts();
                    }

                    @Override
                    public void loadFavicon(String url, ImageView target, TextView fallback) {
                        MainActivity.this.loadFavicon(url, target, fallback);
                    }

                    @Override
                    public void showMessage(String message) {
                        QuietToast.makeText(MainActivity.this, message,
                                QuietToast.LENGTH_SHORT).show();
                    }
                });
    }

    private void renderShortcuts() {
        if (browserShellUi != null) browserShellUi.renderShortcuts();
    }

    private String normalizeShortcutUrl(String text) {
        if (text == null || text.trim().length() == 0) return null;
        text = text.trim();
        if (!text.startsWith("http://") && !text.startsWith("https://")) {
            if (!text.contains(".") || text.contains(" ")) return null;
            text = "https://" + text;
        }
        return text;
    }

    private String guessLabelFromUrl(String url) {
        try {
            String clean = url.replace("https://", "").replace("http://", "");
            if (clean.startsWith("www.")) clean = clean.substring(4);
            int slash = clean.indexOf("/");
            if (slash > 0) clean = clean.substring(0, slash);
            String host = clean.split("[.]")[0];
            if (host.length() == 0) return "Web";
            return host.substring(0, 1).toUpperCase() + host.substring(1);
        } catch (Exception e) {
            return "Web";
        }
    }

    private void loadFavicon(String url, ImageView target, TextView fallback) {
        historyFaviconLoader.load(url, target, fallback);
    }"""
text = replace_between(
        text,
        "    private View createTopBar() {",
        "    private TabInfo findTabByWebView(WebView candidate) {",
        replacement)
PATH.write_text(text)
