from pathlib import Path

main_path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = main_path.read_text(encoding="utf-8")

if "DownloadListPolicy.filterAndSort(" in text:
    print("Download list policy delegation already installed")
    raise SystemExit(0)

start_marker = "    private ArrayList<DownloadItem> getFilteredDownloadItems() {"
end_marker = "    private String getDownloadCategory(DownloadItem item) {"

if text.count(start_marker) != 1 or text.count(end_marker) != 1:
    raise SystemExit("MainActivity markers changed; refusing unsafe refactor")

start = text.index(start_marker)
end = text.index(end_marker, start)
replacement = """    private ArrayList<DownloadItem> getFilteredDownloadItems() {
        synchronized (downloadItems) {
            return DownloadListPolicy.filterAndSort(
                    downloadItems,
                    activeDownloadSection,
                    activeDownloadCategory,
                    activeDownloadSearchQuery,
                    activeDownloadSort);
        }
    }

"""

main_path.write_text(text[:start] + replacement + text[end:], encoding="utf-8")
print("MainActivity download list filtering delegated to DownloadListPolicy")
