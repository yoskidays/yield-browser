package com.yieldbrowser.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Creates Download Manager control rows without owning Activity state. */
final class DownloadControlsFactory {
    private static final int COLOR_BUTTON = 0xFF20232A;
    private static final int COLOR_DANGER = 0xFFE5484D;

    private DownloadControlsFactory() {
    }

    static View createToolRow(Context context,
                              String activeSort,
                              boolean selectionMode,
                              String section,
                              int completedCount,
                              Runnable onSort,
                              Runnable onToggleSelection,
                              Runnable onClearAll,
                              Runnable onShare,
                              Runnable onDelete) {
        LinearLayout tools = new LinearLayout(context);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(0, dp(context, 2), 0, dp(context, 3));

        TextView sort = button(context, "Urut: " + safe(activeSort));
        sort.setOnClickListener(v -> run(onSort));
        tools.addView(sort, new LinearLayout.LayoutParams(0, dp(context, 40), 1));

        TextView select = button(context, selectionMode ? "Batal pilih" : "Pilih");
        select.setOnClickListener(v -> run(onToggleSelection));
        LinearLayout.LayoutParams selectParams = weightedButtonParams(context, 40);
        tools.addView(select, selectParams);

        if (shouldShowClearAll(section, selectionMode, completedCount)) {
            TextView clearAll = button(context, "Hapus semua");
            clearAll.setBackground(roundRect(context, COLOR_DANGER, 18, 0,
                    Color.TRANSPARENT));
            clearAll.setContentDescription("Hapus semua riwayat unduhan selesai");
            clearAll.setOnClickListener(v -> run(onClearAll));
            tools.addView(clearAll, weightedButtonParams(context, 40));
        }

        if (selectionMode) {
            TextView share = button(context, "Bagikan");
            share.setOnClickListener(v -> run(onShare));
            tools.addView(share, weightedButtonParams(context, 40));

            TextView delete = button(context, "Hapus");
            delete.setBackground(roundRect(context, COLOR_DANGER, 18, 0,
                    Color.TRANSPARENT));
            delete.setOnClickListener(v -> run(onDelete));
            tools.addView(delete, weightedButtonParams(context, 40));
        }
        return tools;
    }

    static View createQueueRow(Context context,
                               int activeCount,
                               int maxActive,
                               int queuedCount,
                               Runnable onPauseAll,
                               Runnable onResumeAll,
                               Runnable onQueueSettings) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(context, 3), 0, dp(context, 5));

        TextView pause = button(context, "Jeda semua");
        pause.setOnClickListener(v -> run(onPauseAll));
        row.addView(pause, new LinearLayout.LayoutParams(0, dp(context, 37), 1));

        TextView resume = button(context, "Lanjutkan");
        resume.setOnClickListener(v -> run(onResumeAll));
        row.addView(resume, weightedButtonParams(context, 37));

        TextView queue = button(context, queueLabel(activeCount, maxActive, queuedCount));
        queue.setOnClickListener(v -> run(onQueueSettings));
        row.addView(queue, weightedButtonParams(context, 37));
        return row;
    }

    static boolean shouldShowClearAll(String section, boolean selectionMode,
                                      int completedCount) {
        return "Selesai".equals(section) && !selectionMode && completedCount > 0;
    }

    static String queueLabel(int activeCount, int maxActive, int queuedCount) {
        return "Queue " + Math.max(0, activeCount) + "/" + Math.max(1, maxActive)
                + " • " + Math.max(0, queuedCount);
    }

    private static TextView button(Context context, String text) {
        TextView button = new TextView(context);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setBackground(roundRect(context, COLOR_BUTTON, 18, 1,
                BrowserConstants.COLOR_BORDER));
        return button;
    }

    private static LinearLayout.LayoutParams weightedButtonParams(Context context, int heightDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(context, heightDp), 1);
        params.setMargins(dp(context, 8), 0, 0, 0);
        return params;
    }

    private static GradientDrawable roundRect(Context context, int fill, int radiusDp,
                                              int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(context, radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(context, strokeDp), strokeColor);
        return drawable;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static void run(Runnable action) {
        if (action != null) action.run();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
