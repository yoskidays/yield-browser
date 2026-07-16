from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "TabSessionStore.StoredSession stored = TabSessionStore.read(this);" in text:
    print("Tab session store delegation already installed")
    raise SystemExit(0)


def replace_method(source: str, signature: str, replacement: str) -> str:
    start = source.find(signature)
    if start < 0:
        raise SystemExit(f"Method not found: {signature}")
    brace = source.find("{", start)
    if brace < 0:
        raise SystemExit(f"Method body not found: {signature}")

    depth = 0
    in_string = False
    escaped = False
    quote = ""
    end = -1
    for index in range(brace, len(source)):
        char = source[index]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                in_string = False
            continue
        if char in ('"', "'"):
            in_string = True
            quote = char
        elif char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                end = index + 1
                break
    if end < 0:
        raise SystemExit(f"Unclosed method: {signature}")
    return source[:start] + replacement + source[end:]


restore = '''    private void restoreTabsSession() {
        if (dedicatedPrivateProfile) {
            tabs.clear();
            tabs.add(createProfileTab("Tab utama", "Tab privat", "", true));
            currentTabIndex = 0;
            tabCount = 1;
            return;
        }
        try {
            TabSessionStore.StoredSession stored = TabSessionStore.read(this);
            if (stored.raw.trim().length() == 0) return;

            ArrayList<TabInfo> restored = new ArrayList<>();
            ArrayList<Integer> restoredSourceIndexes = new ArrayList<>();
            for (TabSessionCodec.Record record : TabSessionCodec.decode(stored.raw)) {
                // Private and ad tabs remain ephemeral even when reading legacy session rows.
                if (!TabSessionPolicy.shouldRestore(record.privateTab, record.adTab)) continue;

                String title = TabSessionCodec.normalizedTitle(record.title);
                String url = cleanTabSessionUrl(record.url);
                if (url.length() > 0 && !isRestorableTabSessionUrl(url)) continue;

                TabInfo restoredTab = new TabInfo(record.tabId, title, url, false, false);
                if (url.length() > 0) {
                    restoredTab.lastSafeUrl = url;
                    restoredTab.lastSafeTitle = title;
                    restoredTab.currentPageUrlForRequest = url;
                    String host = record.isolationHost.length() > 0
                            ? record.isolationHost
                            : BrowserUrlUtils.safeHostForTabIsolation(url);
                    restoredTab.isolationHost = host == null ? "" : host;
                }
                restored.add(restoredTab);
                restoredSourceIndexes.add(record.sourceIndex);
            }

            if (!restored.isEmpty()) {
                tabs.clear();
                tabs.addAll(restored);
                int selected = TabSessionPolicy.restoredSelectionIndex(
                        restoredSourceIndexes, stored.requestedIndex);
                currentTabIndex = TabNavigationPolicy.clampIndex(
                        selected < 0 ? 0 : selected, tabs.size());
                tabCount = TabNavigationPolicy.countForUi(tabs.size());
            }
        } catch (Exception ignored) {
        }
    }'''

save = '''    private void saveTabsSession() {
        if (!PrivateProfilePolicy.shouldPersistBrowserSession(dedicatedPrivateProfile)) return;
        try {
            if (tabs.isEmpty()) ensureDefaultTab();
            TabInfo persistedSelection = findPersistedSelectionCandidate();
            ArrayList<TabSessionCodec.Record> records = new ArrayList<>();
            int savedIndex = 0;

            for (TabInfo tab : tabs) {
                if (!isPersistableTab(tab)) continue;
                String url = getSafeUrlForSession(tab);
                if (url == null) continue;
                if (tab == persistedSelection) savedIndex = records.size();

                String title = tab.title == null ? "" : tab.title;
                if (url.length() > 0 && tab.lastSafeTitle != null
                        && tab.lastSafeTitle.length() > 0
                        && !canCommitUrlToTab(tab, tab.url)) {
                    title = tab.lastSafeTitle;
                }

                String isolationHost = tab.isolationHost == null ? "" : tab.isolationHost;
                if (isolationHost.length() == 0 && url.length() > 0) {
                    isolationHost = BrowserUrlUtils.safeHostForTabIsolation(url);
                }
                records.add(TabSessionCodec.Record.persisted(
                        title, url, isolationHost, tab.id));
            }

            if (records.isEmpty()) {
                TabInfo fallback = createProfileTab("Tab utama", "Tab privat", "", false);
                records.add(TabSessionCodec.Record.persisted(
                        fallback.title, "", "", fallback.id));
                savedIndex = 0;
            }

            TabSessionStore.write(
                    this,
                    TabSessionCodec.encode(records),
                    TabSessionPolicy.persistedSelectionIndex(savedIndex, records.size()));
        } catch (Exception ignored) {
        }
    }'''

text = replace_method(
    text,
    "    private void restoreTabsSession() {",
    restore,
)
text = replace_method(
    text,
    "    private void saveTabsSession() {",
    save,
)

path.write_text(text, encoding="utf-8")
print("MainActivity tab session serialization and preference storage delegated")
