# Stage 80 guarded bookmark-store extraction.
from pathlib import Path

PATH = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")


def replace_method(source: str, signature: str, replacement: str) -> str:
    start = source.find(signature)
    if start < 0:
        if replacement in source:
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
    (
        "    private Set<String> getBookmarks() {",
        """    private Set<String> getBookmarks() {
        return BookmarkStore.getBookmarks(
                bookmarkData, getSharedPreferences(PREFS, MODE_PRIVATE));
    }""",
    ),
    (
        "    private BookmarkItemData findBookmarkByUrl(String url) {",
        """    private BookmarkItemData findBookmarkByUrl(String url) {
        return BookmarkStore.findByUrl(bookmarkData, url);
    }""",
    ),
    (
        "    private List<String> getBookmarkFolders() {",
        """    private List<String> getBookmarkFolders() {
        return BookmarkStore.getFolders(
                bookmarkData, getSharedPreferences(PREFS, MODE_PRIVATE));
    }""",
    ),
    (
        "    private int countBookmarksInFolder(String folder) {",
        """    private int countBookmarksInFolder(String folder) {
        return BookmarkStore.countInFolder(bookmarkData, folder);
    }""",
    ),
    (
        "    private void loadBookmarkData() {",
        """    private void loadBookmarkData() {
        BookmarkStore.load(
                bookmarkData,
                getSharedPreferences(PREFS, MODE_PRIVATE),
                MainActivity.this::guessLabelFromUrl);
    }""",
    ),
    (
        "    private void saveBookmarkData() {",
        """    private void saveBookmarkData() {
        BookmarkStore.save(
                bookmarkData, getSharedPreferences(PREFS, MODE_PRIVATE));
    }""",
    ),
]

for signature, replacement in replacements:
    text = replace_method(text, signature, replacement)

PATH.write_text(text)
