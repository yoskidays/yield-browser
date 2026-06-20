package com.yieldbrowser.app;

import java.util.Objects;

/** Immutable presentation snapshot for one download row. */
final class DownloadUiItem {
    final int id;
    final String fileName;
    final String category;
    final String status;
    final String sizeText;
    final String activityText;
    final String detailText;
    final int progressBasisPoints;
    final int progressPercent;
    final boolean showProgress;
    final boolean showPrimaryAction;
    final String primaryAction;
    final boolean selectionMode;
    final boolean selected;

    DownloadUiItem(
            int id,
            String fileName,
            String category,
            String status,
            String sizeText,
            String activityText,
            String detailText,
            int progressBasisPoints,
            int progressPercent,
            boolean showProgress,
            boolean showPrimaryAction,
            String primaryAction,
            boolean selectionMode,
            boolean selected
    ) {
        this.id = id;
        this.fileName = fileName == null ? "" : fileName;
        this.category = category == null ? "Lainnya" : category;
        this.status = status == null ? "" : status;
        this.sizeText = sizeText == null ? "" : sizeText;
        this.activityText = activityText == null ? "" : activityText;
        this.detailText = detailText == null ? "" : detailText;
        this.progressBasisPoints = Math.max(0, Math.min(10_000, progressBasisPoints));
        this.progressPercent = Math.max(0, Math.min(100, progressPercent));
        this.showProgress = showProgress;
        this.showPrimaryAction = showPrimaryAction;
        this.primaryAction = primaryAction == null ? "" : primaryAction;
        this.selectionMode = selectionMode;
        this.selected = selected;
    }

    boolean sameContent(DownloadUiItem other) {
        return other != null
                && id == other.id
                && progressBasisPoints == other.progressBasisPoints
                && progressPercent == other.progressPercent
                && showProgress == other.showProgress
                && showPrimaryAction == other.showPrimaryAction
                && selectionMode == other.selectionMode
                && selected == other.selected
                && Objects.equals(fileName, other.fileName)
                && Objects.equals(category, other.category)
                && Objects.equals(status, other.status)
                && Objects.equals(sizeText, other.sizeText)
                && Objects.equals(activityText, other.activityText)
                && Objects.equals(detailText, other.detailText)
                && Objects.equals(primaryAction, other.primaryAction);
    }
}
