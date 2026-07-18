package com.yieldbrowser.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.File;

/** Owns page-level utility dialogs and display actions that do not mutate browser policy. */
final class PageToolsController {
    private final Activity activity;

    PageToolsController(Activity activity) {
        this.activity = activity;
    }

    void showFindInPage(WebView webView) {
        if (!isVisiblePage(webView)) {
            QuietToast.makeText(activity, "Buka halaman dulu untuk mencari teks",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), 0, dp(8), 0);

        EditText input = new EditText(activity);
        input.setHint("Cari teks di halaman");
        input.setSingleLine(true);
        box.addView(input);

        new AlertDialog.Builder(activity)
                .setTitle("Cari di halaman")
                .setView(box)
                .setPositiveButton("Cari", (dialog, which) -> {
                    String query = input.getText().toString().trim();
                    if (query.length() == 0) return;
                    webView.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
                        if (isDoneCounting) {
                            QuietToast.makeText(activity, numberOfMatches + " hasil ditemukan",
                                    QuietToast.LENGTH_SHORT).show();
                        }
                    });
                    webView.findAllAsync(query);
                })
                .setNeutralButton("Berikutnya", (dialog, which) -> webView.findNext(true))
                .setNegativeButton("Tutup", null)
                .show();
    }

    void sharePage(String url) {
        if (isBlank(url)) {
            QuietToast.makeText(activity, "Belum ada halaman untuk dibagikan",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, url);
        activity.startActivity(Intent.createChooser(send, "Bagikan halaman"));
    }

    void copyLink(String url) {
        if (isBlank(url)) {
            QuietToast.makeText(activity, "Belum ada link untuk disalin",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard =
                (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Yield Browser URL", url));
            QuietToast.makeText(activity, "Link disalin", QuietToast.LENGTH_SHORT).show();
        }
    }

    void showPageInfo(WebView webView, String url) {
        String title = webView != null ? webView.getTitle() : "Yield Browser";
        String safeUrl = url == null ? "Home" : url;
        new AlertDialog.Builder(activity)
                .setTitle("Info halaman")
                .setMessage("Judul:\n" + (title == null ? "-" : title)
                        + "\n\nURL:\n" + safeUrl)
                .setPositiveButton("Salin link", (dialog, which) -> copyLink(safeUrl))
                .setNegativeButton("Tutup", null)
                .show();
    }

    void toggleFullscreen(View topBar, View bottomNavigation) {
        boolean entering = topBar != null && topBar.getVisibility() == View.VISIBLE;
        if (!entering) {
            exitFullscreen(topBar, bottomNavigation);
            return;
        }
        if (topBar != null) topBar.setVisibility(View.GONE);
        if (bottomNavigation != null) bottomNavigation.setVisibility(View.GONE);
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        QuietToast.makeText(activity,
                "Mode layar penuh aktif. Tekan Back untuk keluar.",
                QuietToast.LENGTH_LONG).show();
    }

    void exitFullscreen(View topBar, View bottomNavigation) {
        if (topBar != null) topBar.setVisibility(View.VISIBLE);
        if (bottomNavigation != null) bottomNavigation.setVisibility(View.VISIBLE);
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    void savePageOffline(WebView webView) {
        if (!isVisiblePage(webView)) {
            QuietToast.makeText(activity, "Buka halaman dulu untuk disimpan",
                    QuietToast.LENGTH_SHORT).show();
            return;
        }
        try {
            File dir = new File(activity.getExternalFilesDir(null), "OfflinePages");
            if (!dir.exists()) dir.mkdirs();
            File output = new File(dir, "page_" + System.currentTimeMillis() + ".mht");
            webView.saveWebArchive(output.getAbsolutePath());
            QuietToast.makeText(activity, "Halaman disimpan: " + output.getName(),
                    QuietToast.LENGTH_LONG).show();
        } catch (Exception error) {
            QuietToast.makeText(activity,
                    "Gagal menyimpan halaman: " + error.getMessage(),
                    QuietToast.LENGTH_SHORT).show();
        }
    }

    private static boolean isVisiblePage(WebView webView) {
        return webView != null && webView.getVisibility() == View.VISIBLE;
    }

    private static boolean isBlank(String value) {
        return value == null || value.length() == 0;
    }

    private int dp(int value) {
        return YieldUi.dp(activity, value);
    }
}
