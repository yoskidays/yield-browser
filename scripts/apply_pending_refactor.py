# Stage 82 guarded history-panel extraction.
from pathlib import Path

PATH = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")


def replace_method(source: str, signature: str, replacement: str) -> str:
    start = source.find(signature)
    if start < 0:
        if replacement and replacement in source:
            return source
        if not replacement:
            return source
        raise SystemExit(f"Missing method signature: {signature}")
    brace = source.find("{", start)
    if brace < 0:
        raise SystemExit(f"Missing opening brace: {signature}")
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
old_fields = """    // ===== History Engine V2 =====
    private static final int HISTORY_PAGE_SIZE = 50;
    private static final String KEY_HISTORY_ENGINE_V2_INITIALIZED = "history_engine_v2_initialized";
    private HistoryRepository historyRepository;
    private Dialog activeHistoryDialog;
    private HistoryListAdapter activeHistoryAdapter;
    private RecyclerView activeHistoryRecyclerView;
    private TextView activeHistoryEmptyView;
    private ProgressBar activeHistoryProgress;
    private String activeHistoryQuery = "";
    private long activeHistoryBeforeTime = Long.MAX_VALUE;
    private long activeHistoryBeforeId = Long.MAX_VALUE;
    private boolean activeHistoryLoading = false;
    private boolean activeHistoryEndReached = false;
    private int activeHistoryGeneration = 0;
    private Runnable pendingHistorySearch;
    private boolean historyV2InitializationStarted = false;"""
new_fields = """    // ===== History Engine V2 =====
    private static final String KEY_HISTORY_ENGINE_V2_INITIALIZED = "history_engine_v2_initialized";
    private HistoryRepository historyRepository;
    private HistoryPanelController historyPanelController;
    private boolean historyV2InitializationStarted = false;"""
if old_fields not in text:
    raise SystemExit("Missing History Engine V2 field block")
text = text.replace(old_fields, new_fields, 1)

replacements = [
    (
        "    private void showHistoryPanel() {",
        """    private void showHistoryPanel() {
        if (dedicatedPrivateProfile || isCurrentPrivateTab()) {
            QuietToast.makeText(this, "Riwayat tidak disimpan dalam mode Privat",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }
        initializeHistoryEngineV2();
        recordCurrentPageToHistory();
        historyPanelController = new HistoryPanelController(
                this,
                mainHandler,
                historyRepository,
                url -> {
                    addressBar.setText(url);
                    openAddressBarUrl();
                },
                MainActivity.this::loadFavicon,
                MainActivity.this::clearBrowserHistoryManually);
        historyPanelController.show();
    }

    private void refreshHistoryPanelIfShowing() {
        if (historyPanelController == null || !historyPanelController.isShowing()) return;
        mainHandler.postDelayed(historyPanelController::refresh, 120L);
    }""",
    ),
    ("    private void resetActiveHistoryQuery(String query) {", ""),
    ("    private void loadNextHistoryPage() {", ""),
    ("    private void setActiveHistoryLoading(boolean loading) {", ""),
    ("    private void updateActiveHistoryEmptyState() {", ""),
    (
        "    private void applyDarkFullscreenDialog(Dialog dialog) {",
        """    private void applyDarkFullscreenDialog(Dialog dialog) {
        FullscreenDialogStyler.apply(this, dialog);
    }""",
    ),
]
for signature, replacement in replacements:
    text = replace_method(text, signature, replacement)

initialization_refresh = """                if (activeHistoryDialog != null && activeHistoryDialog.isShowing()) {
                    mainHandler.postDelayed(() -> resetActiveHistoryQuery(activeHistoryQuery), 120L);
                }"""
if initialization_refresh not in text:
    raise SystemExit("Missing history initialization refresh block")
text = text.replace(initialization_refresh, "                refreshHistoryPanelIfShowing();", 1)

failure_refresh = "                resetActiveHistoryQuery(activeHistoryQuery);"
if failure_refresh not in text:
    raise SystemExit("Missing history failure refresh")
text = text.replace(failure_refresh, "                refreshHistoryPanelIfShowing();", 1)

PATH.write_text(text)
