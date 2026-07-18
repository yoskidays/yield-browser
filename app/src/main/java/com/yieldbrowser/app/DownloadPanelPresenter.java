package com.yieldbrowser.app;

import android.app.Activity;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Builds immutable download row snapshots and updates the Download Manager bindings. */
final class DownloadPanelPresenter {
    interface Host {
        boolean canPlay(DownloadItem item);

        boolean hasFinalizingDownload();

        String storageUsageText();

        int activeCount();

        int queuedCount();

        int completedHistoryCount();

        void showSort();

        void toggleSelectMode();

        void clearCompletedHistory();

        void shareSelected();

        void deleteSelected();

        void pauseAll();

        void resumeAll();

        void showQueueSettings();
    }

    static final class State {
        final String section;
        final String category;
        final String query;
        final String sort;
        final boolean selectMode;
        final Set<Integer> selectedIds;
        final boolean queuePaused;
        final int maxActive;

        State(String section,
              String category,
              String query,
              String sort,
              boolean selectMode,
              Set<Integer> selectedIds,
              boolean queuePaused,
              int maxActive) {
            this.section = section == null ? "" : section;
            this.category = category == null ? "" : category;
            this.query = query == null ? "" : query;
            this.sort = sort == null ? "" : sort;
            this.selectMode = selectMode;
            this.selectedIds = selectedIds;
            this.queuePaused = queuePaused;
            this.maxActive = Math.max(1, maxActive);
        }
    }

    private final Activity activity;
    private final List<DownloadItem> items;
    private final Host host;
    private String controlsSignature = "";
    private long lastStorageQueryMs;
    private String cachedStorageText = "Penyimpanan tersedia";

    DownloadPanelPresenter(Activity activity,
                           List<DownloadItem> items,
                           Host host) {
        this.activity = activity;
        this.items = items;
        this.host = host;
    }

    void render(DownloadManagerShell.Bindings bindings, State state) {
        if (bindings == null || bindings.adapter == null || state == null || host == null) return;
        ArrayList<DownloadItem> filtered;
        synchronized (items) {
            filtered = DownloadListPolicy.filterAndSort(
                    items, state.section, state.category, state.query, state.sort);
        }
        ArrayList<DownloadUiItem> snapshots = new ArrayList<>(filtered.size());
        for (DownloadItem item : filtered) {
            snapshots.add(buildUiItem(item, state));
        }
        bindings.adapter.submitList(snapshots);

        if (bindings.emptyView != null) {
            boolean empty = snapshots.isEmpty();
            bindings.emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            bindings.emptyView.setText(DownloadPanelPresentation.emptyMessage(
                    state.section, state.query));
        }
        if (bindings.titleView != null) {
            bindings.titleView.setText(DownloadPanelPresentation.title(
                    state.selectMode, selectedCount(state.selectedIds)));
        }
        if (bindings.storageView != null) {
            long now = System.currentTimeMillis();
            boolean finalizing = host.hasFinalizingDownload();
            if (!finalizing && now - lastStorageQueryMs
                    >= DownloadFinalizationPolicy.STORAGE_QUERY_INTERVAL_MS) {
                cachedStorageText = host.storageUsageText();
                lastStorageQueryMs = now;
            }
            bindings.storageView.setText(DownloadPanelPresentation.storageSummary(
                    cachedStorageText,
                    finalizing,
                    host.activeCount(),
                    host.queuedCount()));
        }
        renderControls(bindings, state);
    }

    void invalidateControls() {
        controlsSignature = "";
    }

    static int queuePosition(List<DownloadItem> items, DownloadItem target) {
        if (items == null || target == null) return 0;
        int position = 0;
        synchronized (items) {
            for (DownloadItem item : items) {
                if (item == null || !"queued".equals(item.status)) continue;
                position++;
                if (item == target || item.id == target.id) return position;
            }
        }
        return 0;
    }

    private DownloadUiItem buildUiItem(DownloadItem item, State state) {
        return DownloadUiItemFactory.build(
                item,
                DownloadItemUtils.getDownloadCategory(item),
                DownloadItemUtils.getDownloadHost(item),
                DownloadItemUtils.getDownloadSize(item),
                host.canPlay(item),
                queuePosition(items, item),
                state.selectMode,
                state.selectedIds != null && state.selectedIds.contains(item.id));
    }

    private void renderControls(DownloadManagerShell.Bindings bindings, State state) {
        if (bindings.controlsPanel == null) return;
        int selected = selectedCount(state.selectedIds);
        int active = host.activeCount();
        int queued = host.queuedCount();
        int completed = host.completedHistoryCount();
        String nextSignature = DownloadPanelPresentation.controlsSignature(
                state.section,
                state.sort,
                state.selectMode,
                selected,
                active,
                queued,
                state.queuePaused,
                state.maxActive,
                completed);
        if (nextSignature.equals(controlsSignature)) return;
        controlsSignature = nextSignature;
        bindings.controlsPanel.removeAllViews();
        bindings.controlsPanel.addView(DownloadControlsFactory.createToolRow(
                activity,
                state.sort,
                state.selectMode,
                state.section,
                completed,
                host::showSort,
                host::toggleSelectMode,
                host::clearCompletedHistory,
                host::shareSelected,
                host::deleteSelected));
        if ("Mengunduh".equals(state.section)) {
            bindings.controlsPanel.addView(DownloadControlsFactory.createQueueRow(
                    activity,
                    active,
                    state.maxActive,
                    queued,
                    host::pauseAll,
                    host::resumeAll,
                    host::showQueueSettings));
        }
    }

    private static int selectedCount(Set<Integer> selectedIds) {
        return selectedIds == null ? 0 : selectedIds.size();
    }
}
