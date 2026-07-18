# Stage 95 guarded extraction of page tools and browser utility dialogs.
from pathlib import Path

PATH = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")


def replace_method(source: str, signature: str, replacement: str) -> str:
    start = source.find(signature)
    if start < 0:
        raise SystemExit(f"Missing method signature: {signature}")
    brace = source.find("{", start)
    if brace < 0:
        raise SystemExit(f"Missing method brace: {signature}")
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
old_field = "    private BrowserShellUi browserShellUi;"
new_fields = """    private BrowserShellUi browserShellUi;
    private PageToolsController pageToolsController;
    private BrowserUtilityDialogsController browserUtilityDialogsController;"""
if old_field not in text:
    raise SystemExit("Missing BrowserShellUi field")
text = text.replace(old_field, new_fields, 1)

page_tools = """    private void ensurePageToolsController() {
        if (pageToolsController == null) pageToolsController = new PageToolsController(this);
    }

    private void showFindInPageDialog() {
        ensurePageToolsController();
        pageToolsController.showFindInPage(webView);
    }"""
text = replace_method(text, "    private void showFindInPageDialog() {", page_tools)
text = replace_method(
    text,
    "    private void shareCurrentPage() {",
    """    private void shareCurrentPage() {
        ensurePageToolsController();
        pageToolsController.sharePage(getEffectiveCurrentUrl());
    }""")
text = replace_method(
    text,
    "    private void copyCurrentLink() {",
    """    private void copyCurrentLink() {
        ensurePageToolsController();
        pageToolsController.copyLink(getEffectiveCurrentUrl());
    }""")
text = replace_method(
    text,
    "    private void showPageInfoDialog() {",
    """    private void showPageInfoDialog() {
        ensurePageToolsController();
        pageToolsController.showPageInfo(webView, getEffectiveCurrentUrl());
    }""")
text = replace_method(
    text,
    "    private void toggleFullscreenMode() {",
    """    private void toggleFullscreenMode() {
        ensurePageToolsController();
        pageToolsController.toggleFullscreen(topBarView, bottomNavView);
    }""")
text = replace_method(
    text,
    "    private void exitFullscreenMode() {",
    """    private void exitFullscreenMode() {
        ensurePageToolsController();
        pageToolsController.exitFullscreen(topBarView, bottomNavView);
    }""")
text = replace_method(
    text,
    "    private void saveCurrentPageOffline() {",
    """    private void saveCurrentPageOffline() {
        ensurePageToolsController();
        pageToolsController.savePageOffline(webView);
    }""")

text_zoom = """    private void ensureBrowserUtilityDialogsController() {
        if (browserUtilityDialogsController == null) {
            browserUtilityDialogsController = new BrowserUtilityDialogsController(this);
        }
    }

    private void showTextZoomDialog(Dialog parentDialog) {
        ensureBrowserUtilityDialogsController();
        browserUtilityDialogsController.showTextZoom(
                parentDialog,
                textZoom,
                new BrowserUtilityDialogsController.TextZoomHandler() {
                    @Override
                    public void saveZoom(int zoom) {
                        textZoom = zoom;
                        applyBrowserSettings();
                        saveSettings();
                    }

                    @Override
                    public void reopenSettings() {
                        showSettingsPanel();
                    }
                });
    }"""
text = replace_method(text, "    private void showTextZoomDialog(Dialog parentDialog) {", text_zoom)

folder_dialog = """    private void showDownloadFolderDialog(Dialog parentDialog) {
        ensureBrowserUtilityDialogsController();
        browserUtilityDialogsController.showDownloadFolder(
                parentDialog,
                downloadSubfolder,
                getDownloadLocationText(),
                new BrowserUtilityDialogsController.DownloadFolderHandler() {
                    @Override
                    public void saveSubfolder(String subfolder) {
                        downloadSubfolder = subfolder;
                        saveSettings();
                    }

                    @Override
                    public void choosePhoneFolder(String subfolder) {
                        downloadSubfolder = subfolder;
                        saveSettings();
                        chooseExternalDownloadFolder();
                    }

                    @Override
                    public void resetDefault(String subfolder) {
                        downloadSubfolder = subfolder;
                        selectedDownloadTreeUri = "";
                        saveSettings();
                    }

                    @Override
                    public void reopenDownloadSettings() {
                        showDownloadSettingsPanel();
                    }
                });
    }"""
text = replace_method(text, "    private void showDownloadFolderDialog(Dialog parentDialog) {", folder_dialog)
text = replace_method(text, "    private TextView darkDialogActionButton(String text) {", "")

PATH.write_text(text)
