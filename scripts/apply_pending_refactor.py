# Stage 83 guarded bookmark-panel extraction.
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
replacements = [
    ("    private void applyDarkFullscreenDialog(Dialog dialog) {", ""),
    (
        "    private void showBookmarkHomePanel() {",
        """    private void showBookmarkHomePanel() {
        BookmarkPanelController controller = new BookmarkPanelController(
                this,
                mainHandler,
                bookmarkData,
                getSharedPreferences(PREFS, MODE_PRIVATE),
                MainActivity.this::normalizeShortcutUrl,
                MainActivity.this::loadFavicon,
                url -> {
                    addressBar.setText(url);
                    openAddressBarUrl();
                },
                url -> {
                    newTabInCurrentProfile();
                    addressBar.setText(url);
                    openAddressBarUrl();
                },
                MainActivity.this::openUrlInPrivateSpace);
        controller.showHome();
    }""",
    ),
    ("    private void showBookmarkFolderPanel(String folder) {", ""),
    ("    private View bookmarkFolderRow(String folder, int count, Dialog parent) {", ""),
    ("    private View bookmarkItemRow(BookmarkItemData item, Dialog dialog, Runnable refresh) {", ""),
    ("    private void showBookmarkItemMenu(View anchor, BookmarkItemData item, Dialog dialog, Runnable refresh) {", ""),
    ("    private void showEditBookmarkDialog(BookmarkItemData item, Dialog dialog, Runnable refresh) {", ""),
    ("    private void showMoveBookmarkDialog(BookmarkItemData item, Dialog dialog, Runnable refresh) {", ""),
    ("    private void showCreateBookmarkFolderDialog(Runnable onDone) {", ""),
    ("    private void showBookmarkSortMenu(View anchor) {", ""),
    ("    private void showBookmarkMainMenu(View anchor, Dialog dialog) {", ""),
    ("    private void switchDialogSmooth(Dialog currentDialog, Runnable openNext) {", ""),
    ("    private void refreshDialogSmooth(Dialog dialog, Runnable render) {", ""),
    ("    private ImageButton plainIconButton(int iconRes, View.OnClickListener listener) {", ""),
    ("    private EditText darkSearchInput(String hint) {", ""),
    ("    private TextView circleLetter(String text, int bg, int fg) {", ""),
    ("    private String safeBookmarkTitle(BookmarkItemData item) {", ""),
    ("    private String bookmarkInitial(BookmarkItemData item) {", ""),
    ("    private String historyInitial(HistoryItemData item) {", ""),
    ("    private String shortHost(String url) {", ""),
    ("    private String historyDayLabel(long timeMs) {", ""),
    ("    private boolean sameDay(java.util.Calendar a, java.util.Calendar b) {", ""),
]
for signature, replacement in replacements:
    text = replace_method(text, signature, replacement)

PATH.write_text(text)
