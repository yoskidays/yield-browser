package com.yieldbrowser.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Builds the compact browser menu and the About Yield screen from immutable UI state. */
final class QuickMenuController {
    enum Action {
        DOWNLOADS, BOOKMARKS, PROFILE, AD_BLOCK, READER, NIGHT_MODE, QR_SCAN,
        HISTORY, FIND_PAGE, SHARE, FULLSCREEN, VIDEO_CONTROLS, RELOAD,
        BLOCK_ELEMENT, SITE_FILTER, SETTINGS, CUSTOMIZE, EXIT
    }

    interface ActionHandler {
        void handle(Action action);
    }

    static final class State {
        final boolean download;
        final boolean bookmark;
        final boolean privateSpace;
        final boolean adBlock;
        final boolean reader;
        final boolean nightMode;
        final boolean qrScan;
        final boolean history;
        final boolean findPage;
        final boolean share;
        final boolean fullscreen;
        final boolean videoControlsShortcut;
        final boolean reload;
        final boolean blockElement;
        final boolean siteFilter;
        final boolean dedicatedPrivateProfile;
        final boolean adBlockEnabled;
        final boolean readerEnabled;
        final boolean videoControlsEnabled;
        final String nightModeLabel;

        State(boolean download,
              boolean bookmark,
              boolean privateSpace,
              boolean adBlock,
              boolean reader,
              boolean nightMode,
              boolean qrScan,
              boolean history,
              boolean findPage,
              boolean share,
              boolean fullscreen,
              boolean videoControlsShortcut,
              boolean reload,
              boolean blockElement,
              boolean siteFilter,
              boolean dedicatedPrivateProfile,
              boolean adBlockEnabled,
              boolean readerEnabled,
              boolean videoControlsEnabled,
              String nightModeLabel) {
            this.download = download;
            this.bookmark = bookmark;
            this.privateSpace = privateSpace;
            this.adBlock = adBlock;
            this.reader = reader;
            this.nightMode = nightMode;
            this.qrScan = qrScan;
            this.history = history;
            this.findPage = findPage;
            this.share = share;
            this.fullscreen = fullscreen;
            this.videoControlsShortcut = videoControlsShortcut;
            this.reload = reload;
            this.blockElement = blockElement;
            this.siteFilter = siteFilter;
            this.dedicatedPrivateProfile = dedicatedPrivateProfile;
            this.adBlockEnabled = adBlockEnabled;
            this.readerEnabled = readerEnabled;
            this.videoControlsEnabled = videoControlsEnabled;
            this.nightModeLabel = nightModeLabel == null ? "" : nightModeLabel;
        }
    }

    static final class Item {
        final int iconRes;
        final String label;
        final Action action;
        final boolean switchDialog;

        Item(int iconRes, String label, Action action, boolean switchDialog) {
            this.iconRes = iconRes;
            this.label = label;
            this.action = action;
            this.switchDialog = switchDialog;
        }
    }

    private final Activity activity;
    private final Handler handler;
    private final State state;
    private final ActionHandler actionHandler;

    QuickMenuController(Activity activity,
                        Handler handler,
                        State state,
                        ActionHandler actionHandler) {
        this.activity = activity;
        this.handler = handler;
        this.state = state;
        this.actionHandler = actionHandler;
    }

    void show() {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout menu = new LinearLayout(activity);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(12), dp(12), dp(12), dp(12));
        menu.setBackground(YieldUi.roundRect(
                Color.parseColor("#2B2D33"), dp(24), dp(1), Color.parseColor("#2D333D")));

        for (Item item : buildItems(state)) {
            addMenuItem(menu, dialog, item);
        }

        menu.addView(SettingsUi.menuDivider(activity));
        addMenuItem(menu, dialog,
                new Item(R.drawable.ic_settings, "Setelan", Action.SETTINGS, true));
        addMenuItem(menu, dialog,
                new Item(R.drawable.ic_customize, "Sesuaikan menu", Action.CUSTOMIZE, true));
        addMenuItem(menu, dialog,
                new Item(R.drawable.ic_exit, "Keluar", Action.EXIT, false));

        dialog.setContentView(menu);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM | Gravity.END;
            params.width = dp(292);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.x = dp(12);
            params.y = dp(76);
            window.setAttributes(params);
        }
        dialog.show();
    }

    private void addMenuItem(LinearLayout menu, Dialog dialog, Item item) {
        menu.addView(SettingsUi.menuRow(activity, item.iconRes, item.label, view -> {
            if (item.switchDialog) {
                dispatch(item.action);
                handler.postDelayed(() -> dismissIfShowing(dialog), 120L);
            } else {
                dialog.dismiss();
                dispatch(item.action);
            }
        }));
    }

    private void dispatch(Action action) {
        if (actionHandler != null) actionHandler.handle(action);
    }

    private static void dismissIfShowing(Dialog dialog) {
        try {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    static List<Item> buildItems(State state) {
        ArrayList<Item> items = new ArrayList<>();
        if (state == null) return items;
        if (state.download) items.add(new Item(
                R.drawable.ic_download_modern, "Unduhan Yield", Action.DOWNLOADS, true));
        if (state.bookmark) items.add(new Item(
                R.drawable.ic_bookmark, "Bookmark", Action.BOOKMARKS, true));
        if (state.privateSpace) items.add(new Item(
                R.drawable.ic_private,
                state.dedicatedPrivateProfile ? "Beralih ke tab umum" : "Buka ruang privat",
                Action.PROFILE,
                false));
        if (state.adBlock) items.add(new Item(
                R.drawable.ic_shield,
                "AdBlock " + (state.adBlockEnabled ? "ON" : "OFF"),
                Action.AD_BLOCK,
                false));
        if (state.reader) items.add(new Item(
                R.drawable.ic_reader,
                "Reader Mode " + (state.readerEnabled ? "ON" : "OFF"),
                Action.READER,
                false));
        if (state.nightMode) items.add(new Item(
                R.drawable.ic_night,
                "Mode Malam: " + state.nightModeLabel,
                Action.NIGHT_MODE,
                true));
        if (state.qrScan) items.add(new Item(
                R.drawable.ic_qr_scan, "Pindai QR Code", Action.QR_SCAN, false));
        if (state.history) items.add(new Item(
                R.drawable.ic_history, "Riwayat", Action.HISTORY, true));
        if (state.findPage) items.add(new Item(
                R.drawable.ic_find_page, "Cari di halaman", Action.FIND_PAGE, true));
        if (state.share) items.add(new Item(
                R.drawable.ic_share, "Bagikan halaman", Action.SHARE, false));
        if (state.fullscreen) items.add(new Item(
                R.drawable.ic_fullscreen, "Layar penuh", Action.FULLSCREEN, false));
        if (state.videoControlsShortcut) items.add(new Item(
                R.drawable.ic_video_control,
                "Kontrol video " + (state.videoControlsEnabled ? "ON" : "OFF"),
                Action.VIDEO_CONTROLS,
                false));
        if (state.reload) items.add(new Item(
                R.drawable.ic_refresh, "Reload website", Action.RELOAD, false));
        if (state.blockElement) items.add(new Item(
                R.drawable.ic_block_element, "Blokir elemen", Action.BLOCK_ELEMENT, false));
        if (state.siteFilter) items.add(new Item(
                R.drawable.ic_safe, "Filter situs ini", Action.SITE_FILTER, true));
        return items;
    }

    static void showAbout(Activity activity) {
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#17191E"));
        root.setPadding(dp(activity, 18), dp(activity, 18), dp(activity, 18), dp(activity, 18));

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView back = headerIcon(activity, R.drawable.ic_back);
        header.addView(back, new LinearLayout.LayoutParams(dp(activity, 28), dp(activity, 28)));

        TextView title = new TextView(activity);
        title.setText("Tentang Yield");
        title.setTextColor(Color.parseColor("#EDEDF0"));
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1f);
        titleParams.setMargins(dp(activity, 18), 0, dp(activity, 18), 0);
        header.addView(title, titleParams);

        ImageView close = headerIcon(activity, R.drawable.ic_close);
        header.addView(close, new LinearLayout.LayoutParams(dp(activity, 28), dp(activity, 28)));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        addAboutCard(activity, root, "Versi aplikasi",
                "Yield Browser " + appVersionName(activity), 20);
        addAboutCard(activity, root, "Sistem operasi",
                "Android " + Build.VERSION.RELEASE + " ; Build/" + Build.ID, 10);
        addAboutCard(activity, root, "Developer", "develop by yoski days", 10);
        root.addView(new View(activity), new LinearLayout.LayoutParams(-1, 0, 1f));

        View.OnClickListener dismiss = view -> dialog.dismiss();
        back.setOnClickListener(dismiss);
        close.setOnClickListener(dismiss);
        dialog.setContentView(root);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#17191E")));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    static String appVersionName(Activity activity) {
        try {
            PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            if (info != null && info.versionName != null && info.versionName.length() > 0) {
                return info.versionName;
            }
        } catch (Exception ignored) {
        }
        return "0.9.99";
    }

    private static void addAboutCard(Activity activity,
                                     LinearLayout root,
                                     String heading,
                                     String value,
                                     int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(activity, topMarginDp), 0, 0);
        root.addView(SettingsUi.aboutInfoCard(activity, heading, value), params);
    }

    private static ImageView headerIcon(Activity activity, int iconRes) {
        ImageView icon = new ImageView(activity);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#D8D8DB"));
        return icon;
    }

    private int dp(int value) {
        return dp(activity, value);
    }

    private static int dp(Activity activity, int value) {
        return YieldUi.dp(activity, value);
    }
}
