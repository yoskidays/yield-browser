package com.yieldbrowser.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

/** Dialog composition for element-picker confirmation and per-site filter management. */
final class ElementFilterDialogController {
    interface PickerCallback {
        void onBlock(String host, String selector);

        void onParentRequested();

        void onContinueRequested();
    }

    interface ManagerCallback {
        void onRemove(String host, String selector);

        void onClear(String host);

        void onRefreshRequested();
    }

    private final Activity activity;

    ElementFilterDialogController(Activity activity) {
        this.activity = activity;
    }

    AlertDialog createPickerDialog(String selector,
                                   String preview,
                                   int matchCount,
                                   String host,
                                   PickerCallback callback) {
        String cleanSelector = selector == null ? "" : selector.trim();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Blokir elemen ini?");
        builder.setMessage(buildPickerMessage(cleanSelector, preview, matchCount));
        builder.setPositiveButton("Blokir & lanjut", (dialog, which) -> {
            if (callback != null) callback.onBlock(host, cleanSelector);
        });
        builder.setNeutralButton("Naik 1 induk", (dialog, which) -> {
            if (callback != null) callback.onParentRequested();
        });
        builder.setNegativeButton("Batal pilihan", (dialog, which) -> {
            if (callback != null) callback.onContinueRequested();
        });
        builder.setOnCancelListener(dialog -> {
            if (callback != null) callback.onContinueRequested();
        });
        return builder.create();
    }

    void showManager(String host,
                     Set<String> selectors,
                     ManagerCallback callback) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackground(YieldUi.roundRect(
                Color.parseColor("#2B2D33"),
                dp(20),
                dp(1),
                Color.parseColor("#2D333D")));

        TextView title = new TextView(activity);
        title.setText("Filter elemen — "
                + (host == null || host.length() == 0 ? "(situs ini)" : host));
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setPadding(0, 0, 0, dp(10));
        root.addView(title);

        if (selectors == null || selectors.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText("Belum ada elemen yang diblokir di situs ini.\n"
                    + "Gunakan \"Blokir elemen\" lalu ketuk iklannya.");
            empty.setTextColor(Color.parseColor("#B7BDC8"));
            empty.setTextSize(14);
            root.addView(empty);
        } else {
            ScrollView scroll = new ScrollView(activity);
            LinearLayout list = new LinearLayout(activity);
            list.setOrientation(LinearLayout.VERTICAL);
            for (String selector : new ArrayList<>(selectors)) {
                list.addView(selectorRow(dialog, host, selector, callback));
            }
            scroll.addView(list);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, dp(320)));

            root.addView(SettingsUi.menuDivider(activity));
            root.addView(SettingsUi.menuRow(
                    activity,
                    R.drawable.ic_clear,
                    "Hapus semua filter situs ini",
                    view -> {
                        if (callback != null) callback.onClear(host);
                        dialog.dismiss();
                    }));
        }

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = dp(330);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
        dialog.show();
    }

    static String buildPickerMessage(String selector, String preview, int matchCount) {
        String cleanSelector = selector == null ? "" : selector.trim();
        String previewText = preview == null ? "" : preview.trim();
        if (previewText.length() > 180) {
            previewText = previewText.substring(0, 180) + "…";
        }
        int safeCount = UserElementFilterPolicy.normalizedMatchCount(matchCount);
        StringBuilder message = new StringBuilder();
        message.append("Selector:\n").append(cleanSelector);
        if (safeCount > 0) {
            message.append("\n\nAkan menyembunyikan ")
                    .append(safeCount)
                    .append(safeCount == 1
                            ? " elemen."
                            : " elemen pada halaman ini.");
        }
        if (previewText.length() > 0) {
            message.append("\n\nPratinjau:\n").append(previewText);
        }
        return message.toString();
    }

    private View selectorRow(Dialog dialog,
                             String host,
                             String selector,
                             ManagerCallback callback) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView text = new TextView(activity);
        text.setText(selector);
        text.setTextColor(Color.parseColor("#E6E9EF"));
        text.setTextSize(13);
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView remove = new TextView(activity);
        remove.setText("Hapus");
        remove.setTextColor(Color.parseColor("#F87171"));
        remove.setTextSize(14);
        remove.setTypeface(Typeface.DEFAULT_BOLD);
        remove.setPadding(dp(12), dp(6), dp(6), dp(6));
        remove.setOnClickListener(view -> {
            if (callback != null) callback.onRemove(host, selector);
            dialog.dismiss();
            if (callback != null) callback.onRefreshRequested();
        });
        row.addView(remove);
        return row;
    }

    private int dp(int value) {
        return YieldUi.dp(activity, value);
    }
}
