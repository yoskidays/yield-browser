# Stage 81 guarded user-element-filter extraction.
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
    ("    private String escapeForJsDoubleQuotes(String s) {", ""),
    (
        "    private void loadUserElementFilters() {",
        """    private void loadUserElementFilters() {
        if (userElementFiltersLoaded) return;
        userElementFiltersLoaded = true;
        UserElementFilterStore.load(
                getSharedPreferences(PREFS, MODE_PRIVATE), userElementFilters);
    }""",
    ),
    (
        "    private void persistUserElementFiltersForHost(String host) {",
        """    private void persistUserElementFiltersForHost(String host) {
        UserElementFilterStore.persistHost(
                getSharedPreferences(PREFS, MODE_PRIVATE), userElementFilters, host);
    }""",
    ),
    (
        "    private LinkedHashSet<String> userFiltersForHost(String host) {",
        """    private LinkedHashSet<String> userFiltersForHost(String host) {
        loadUserElementFilters();
        return UserElementFilterStore.filtersForHost(userElementFilters, host);
    }""",
    ),
    (
        "    private boolean addUserElementFilter(String host, String selector) {",
        """    private boolean addUserElementFilter(String host, String selector) {
        loadUserElementFilters();
        boolean added = UserElementFilterStore.add(userElementFilters, host, selector);
        if (added) persistUserElementFiltersForHost(host);
        return added;
    }""",
    ),
    (
        "    private void removeUserElementFilter(String host, String selector) {",
        """    private void removeUserElementFilter(String host, String selector) {
        loadUserElementFilters();
        if (UserElementFilterStore.remove(userElementFilters, host, selector)) {
            persistUserElementFiltersForHost(host);
        }
    }""",
    ),
    (
        "    private String buildUserFilterCss(String host) {",
        """    private String buildUserFilterCss(String host) {
        loadUserElementFilters();
        return UserElementFilterStore.buildCss(userElementFilters, host);
    }""",
    ),
    (
        "    private void applyUserFiltersForCurrentPage() {",
        """    private void applyUserFiltersForCurrentPage() {
        if (webView == null) return;
        try {
            String host = hostOfUrl(getEffectiveCurrentUrl());
            runPageScript(UserElementFilterStore.buildPageScript(buildUserFilterCss(host)));
        } catch (Exception ignored) {
        }
    }""",
    ),
]

for signature, replacement in replacements:
    text = replace_method(text, signature, replacement)

clear_block = """                if (host != null && host.length() > 0) {
                    userElementFilters.remove(host);
                    persistUserElementFiltersForHost(host);
                    applyUserFiltersForCurrentPage();
                }"""
clear_replacement = """                if (UserElementFilterStore.clearHost(userElementFilters, host)) {
                    persistUserElementFiltersForHost(host);
                    applyUserFiltersForCurrentPage();
                }"""
if clear_block not in text:
    raise SystemExit("Missing clear-host manager block")
text = text.replace(clear_block, clear_replacement, 1)

PATH.write_text(text)
