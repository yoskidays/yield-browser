package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_SUBTEXT;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/** QR scanner and search-engine selection dialogs. */
final class BrowserUtilityDialogs {
    interface ValueHandler {
        void accept(String value);
    }

    private static final String[] SEARCH_ENGINES =
            new String[]{"Google", "Bing", "DuckDuckGo", "Yahoo", "Yandex"};

    private BrowserUtilityDialogs() {
    }

    static void showQrScanner(Activity activity, ValueHandler handler) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(activity, 14), dp(activity, 14), dp(activity, 14), dp(activity, 14));
        panel.setBackground(YieldUi.roundRect(
                Color.parseColor("#2B2D33"),
                dp(activity, 24),
                dp(activity, 1),
                Color.parseColor("#3A3D45")));

        TextView title = new TextView(activity);
        title.setText("Pindai QR Code");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(title);

        TextView hint = new TextView(activity);
        hint.setText("Arahkan kamera ke QR. Jika QR berisi link, Yield akan langsung membukanya.");
        hint.setTextColor(COLOR_SUBTEXT);
        hint.setTextSize(13);
        hint.setPadding(0, dp(activity, 6), 0, dp(activity, 10));
        panel.addView(hint);

        QrScannerView scanner = new QrScannerView(activity, result -> {
            dialog.dismiss();
            if (handler != null) handler.accept(result);
        });
        panel.addView(scanner, new LinearLayout.LayoutParams(-1, dp(activity, 360)));

        TextView cancel = new TextView(activity);
        cancel.setText("Tutup");
        cancel.setTextColor(Color.WHITE);
        cancel.setGravity(Gravity.CENTER);
        cancel.setTextSize(16);
        cancel.setTypeface(Typeface.DEFAULT_BOLD);
        cancel.setPadding(0, dp(activity, 12), 0, dp(activity, 4));
        cancel.setOnClickListener(view -> dialog.dismiss());
        panel.addView(cancel, new LinearLayout.LayoutParams(-1, dp(activity, 52)));

        dialog.setOnDismissListener(target -> scanner.stopCamera());
        dialog.setContentView(panel);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
        dialog.show();
    }

    static void showSearchEngine(Activity activity,
                                 String currentEngine,
                                 ValueHandler handler) {
        int checked = selectedEngineIndex(currentEngine);
        new AlertDialog.Builder(activity)
                .setTitle("Pilih search engine")
                .setSingleChoiceItems(SEARCH_ENGINES, checked, (dialog, which) -> {
                    if (handler != null) handler.accept(SEARCH_ENGINES[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    static int selectedEngineIndex(String engine) {
        if (engine == null) return 0;
        for (int index = 0; index < SEARCH_ENGINES.length; index++) {
            if (SEARCH_ENGINES[index].equals(engine)) return index;
        }
        return 0;
    }

    static String normalizeQrValue(String result) {
        return result == null ? "" : result.trim();
    }

    private static int dp(Activity activity, int value) {
        return YieldUi.dp(activity, value);
    }
}
