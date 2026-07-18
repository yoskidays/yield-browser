# Stage 91 guarded element-filter dialog extraction.
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
field_marker = "    private AlertDialog elementPickerDialog = null;\n"
field_replacement = field_marker + (
        "    private ElementFilterDialogController elementFilterDialogController;\n")
if field_marker not in text:
    raise SystemExit("Missing element-picker field marker")
text = text.replace(field_marker, field_replacement, 1)

picker = """    private void onPickerElementSelected(String selector, String preview,
                                         int matchCount, String selectedTag) {
        if (!elementPickerActive) return;
        if (!UserElementFilterPolicy.isSafeSelector(selector, selectedTag)) {
            QuietToast.makeText(this,
                    "Elemen penting atau selector tidak aman tidak dapat diblokir",
                    QuietToast.LENGTH_SHORT).show();
            continueElementPickerAfterSelection();
            return;
        }
        final String cleanSelector = selector.trim();
        final String host = hostOfUrl(getEffectiveCurrentUrl());
        if (host == null || host.length() == 0) {
            QuietToast.makeText(this,
                    "Tidak bisa menentukan domain halaman ini",
                    QuietToast.LENGTH_SHORT).show();
            finishElementPicker(false);
            return;
        }
        if (elementPickerDialog != null) {
            try {
                elementPickerDialog.dismiss();
            } catch (Exception ignored) {
            }
            elementPickerDialog = null;
        }
        if (elementFilterDialogController == null) {
            elementFilterDialogController = new ElementFilterDialogController(this);
        }
        elementPickerDialog = elementFilterDialogController.createPickerDialog(
                cleanSelector,
                preview,
                matchCount,
                host,
                new ElementFilterDialogController.PickerCallback() {
                    @Override
                    public void onBlock(String targetHost, String targetSelector) {
                        boolean added = addUserElementFilter(targetHost, targetSelector);
                        applyUserFiltersForCurrentPage();
                        continueElementPickerAfterSelection();
                        QuietToast.makeText(MainActivity.this,
                                added ? "Elemen diblokir permanen di " + targetHost
                                        : "Filter ini sudah tersimpan di " + targetHost,
                                QuietToast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onParentRequested() {
                        elementPickerDialog = null;
                        mainHandler.postDelayed(() -> runPageScript(
                                "javascript:(function(){try{if(window.__yieldPickerParent)window.__yieldPickerParent();}catch(e){}})();"
                        ), 80L);
                    }

                    @Override
                    public void onContinueRequested() {
                        continueElementPickerAfterSelection();
                    }
                });
        elementPickerDialog.setOnDismissListener(dialog -> {
            if (elementPickerDialog == dialog) elementPickerDialog = null;
        });
        try {
            elementPickerDialog.show();
        } catch (Exception ignored) {
        }
    }"""
text = replace_method(
        text,
        "    private void onPickerElementSelected(String selector, String preview,",
        picker)

manager = """    private void showUserFiltersManager() {
        final String host = hostOfUrl(getEffectiveCurrentUrl());
        loadUserElementFilters();
        LinkedHashSet<String> selectors =
                host == null || host.length() == 0 ? null : userElementFilters.get(host);
        if (elementFilterDialogController == null) {
            elementFilterDialogController = new ElementFilterDialogController(this);
        }
        elementFilterDialogController.showManager(
                host,
                selectors,
                new ElementFilterDialogController.ManagerCallback() {
                    @Override
                    public void onRemove(String targetHost, String selector) {
                        removeUserElementFilter(targetHost, selector);
                        applyUserFiltersForCurrentPage();
                    }

                    @Override
                    public void onClear(String targetHost) {
                        if (UserElementFilterStore.clearHost(userElementFilters, targetHost)) {
                            persistUserElementFiltersForHost(targetHost);
                            applyUserFiltersForCurrentPage();
                        }
                        QuietToast.makeText(MainActivity.this,
                                "Filter situs dihapus",
                                QuietToast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onRefreshRequested() {
                        showUserFiltersManager();
                    }
                });
    }"""
text = replace_method(text, "    private void showUserFiltersManager() {", manager)
PATH.write_text(text)
