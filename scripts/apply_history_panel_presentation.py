from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "HistoryPanelPresentation.shouldLoadNextPage(" in text:
    print("History panel presentation delegation already installed")
    raise SystemExit(0)

replacements = [
    (
        '''                if (dy <= 0 || activeHistoryLoading || activeHistoryEndReached) return;
                int last = layoutManager.findLastVisibleItemPosition();
                if (last >= Math.max(0, adapter.getItemCount() - 8)) {
                    loadNextHistoryPage();
                }''',
        '''                int last = layoutManager.findLastVisibleItemPosition();
                if (HistoryPanelPresentation.shouldLoadNextPage(
                        dy,
                        activeHistoryLoading,
                        activeHistoryEndReached,
                        last,
                        adapter.getItemCount())) {
                    loadNextHistoryPage();
                }'''
    ),
    (
        '''                String query = s == null ? "" : s.toString().trim();
                pendingHistorySearch = () -> resetActiveHistoryQuery(query);
                mainHandler.postDelayed(pendingHistorySearch, 220L);''',
        '''                String query = HistoryPanelPresentation.normalizeQuery(
                        s == null ? "" : s.toString());
                pendingHistorySearch = () -> resetActiveHistoryQuery(query);
                mainHandler.postDelayed(pendingHistorySearch,
                        HistoryPanelPresentation.SEARCH_DEBOUNCE_MS);'''
    ),
    (
        '''        activeHistoryQuery = query == null ? "" : query.trim();''',
        '''        activeHistoryQuery = HistoryPanelPresentation.normalizeQuery(query);'''
    ),
    (
        '''                    activeHistoryEndReached = page.size() < HISTORY_PAGE_SIZE;''',
        '''                    activeHistoryEndReached = HistoryPanelPresentation.isEndReached(
                            page.size(), HISTORY_PAGE_SIZE);'''
    ),
    (
        '''        boolean initial = activeHistoryAdapter == null || activeHistoryAdapter.isEmpty();
        activeHistoryProgress.setVisibility(loading && initial ? View.VISIBLE : View.GONE);''',
        '''        boolean adapterEmpty = activeHistoryAdapter == null || activeHistoryAdapter.isEmpty();
        activeHistoryProgress.setVisibility(
                HistoryPanelPresentation.shouldShowInitialLoading(loading, adapterEmpty)
                        ? View.VISIBLE : View.GONE);'''
    ),
    (
        '''        boolean show = !activeHistoryLoading && activeHistoryAdapter.isEmpty();
        activeHistoryEmptyView.setText(activeHistoryQuery == null || activeHistoryQuery.isEmpty()
                ? "Riwayat masih kosong."
                : "Tidak ada riwayat yang cocok.");
        activeHistoryEmptyView.setVisibility(show ? View.VISIBLE : View.GONE);''',
        '''        boolean adapterEmpty = activeHistoryAdapter.isEmpty();
        boolean show = HistoryPanelPresentation.shouldShowEmpty(
                activeHistoryLoading, adapterEmpty);
        activeHistoryEmptyView.setText(
                HistoryPanelPresentation.emptyMessage(activeHistoryQuery));
        activeHistoryEmptyView.setVisibility(show ? View.VISIBLE : View.GONE);'''
    ),
]

for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"History panel block changed; expected one match, found {count}: {old[:80]!r}")
    text = text.replace(old, new)

path.write_text(text, encoding="utf-8")
print("History panel state and text delegated to HistoryPanelPresentation")
