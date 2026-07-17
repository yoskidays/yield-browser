package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_BORDER;
import static com.yieldbrowser.app.BrowserConstants.COLOR_SUBTEXT;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Owns the main settings, AdBlock settings, and menu-customization dialog composition. */
final class SettingsPanelController {
    enum Behavior { KEEP, DISMISS, SWITCH }

    enum MainAction {
        TRANSLATE, DOWNLOAD_SETTINGS, BOOKMARKS, PROFILE, CUSTOMIZE, QR_SCAN,
        SEARCH_ENGINE, BLOCK_ELEMENT, SITE_FILTER, HISTORY, FIND_PAGE, SHARE,
        COPY_LINK, PAGE_INFO, FULLSCREEN, VIDEO_CONTROLS, VIDEO_OPTIMIZATION,
        SAVE_OFFLINE, SPEED_MODE, SAFE_MODE, HTTPS_FIRST, NIGHT_MODE, READER_MODE,
        AD_BLOCK_SETTINGS, DATA_SAVER, DESKTOP_MODE, TEXT_ZOOM, CLEAR_CACHE, ABOUT
    }

    enum AdBlockAction {
        MASTER, POPUP, REDIRECT, SCRIPT_IFRAME, CLICK_HIJACK, TEMP_TAB, AUTO_CLOSE
    }

    enum CustomizeAction {
        TOP_RELOAD, TOP_BOOKMARK, TOP_TRANSLATE, RELOAD, DOWNLOAD, BOOKMARK,
        PRIVATE, AD_BLOCK, READER, NIGHT_MODE, QR_SCAN, HISTORY, FIND_PAGE,
        SHARE, FULLSCREEN, BLOCK_ELEMENT, SITE_FILTER, VIDEO_CONTROLS
    }

    interface ActionHandler<A> {
        void handle(A action, Dialog ownerDialog);
    }

    static final class MainState {
        final boolean dedicatedPrivateProfile;
        final String searchEngine;
        final boolean videoControlsEnabled;
        final boolean speedMode;
        final boolean safeMode;
        final boolean httpsFirstEnabled;
        final String nightModeLabel;
        final boolean readerMode;
        final boolean adBlock;
        final String adBlockSummary;
        final boolean dataSaver;
        final boolean desktopMode;
        final int textZoom;

        MainState(boolean dedicatedPrivateProfile,
                  String searchEngine,
                  boolean videoControlsEnabled,
                  boolean speedMode,
                  boolean safeMode,
                  boolean httpsFirstEnabled,
                  String nightModeLabel,
                  boolean readerMode,
                  boolean adBlock,
                  String adBlockSummary,
                  boolean dataSaver,
                  boolean desktopMode,
                  int textZoom) {
            this.dedicatedPrivateProfile = dedicatedPrivateProfile;
            this.searchEngine = searchEngine == null ? "" : searchEngine;
            this.videoControlsEnabled = videoControlsEnabled;
            this.speedMode = speedMode;
            this.safeMode = safeMode;
            this.httpsFirstEnabled = httpsFirstEnabled;
            this.nightModeLabel = nightModeLabel == null ? "" : nightModeLabel;
            this.readerMode = readerMode;
            this.adBlock = adBlock;
            this.adBlockSummary = adBlockSummary == null ? "" : adBlockSummary;
            this.dataSaver = dataSaver;
            this.desktopMode = desktopMode;
            this.textZoom = textZoom;
        }
    }

    static final class MainItem {
        final String section;
        final int iconRes;
        final String title;
        final String description;
        final MainAction action;
        final boolean setting;
        final boolean enabled;
        final Behavior behavior;

        private MainItem(String section,
                         int iconRes,
                         String title,
                         String description,
                         MainAction action,
                         boolean setting,
                         boolean enabled,
                         Behavior behavior) {
            this.section = section;
            this.iconRes = iconRes;
            this.title = title;
            this.description = description;
            this.action = action;
            this.setting = setting;
            this.enabled = enabled;
            this.behavior = behavior;
        }

        static MainItem section(String title) {
            return new MainItem(title, 0, "", "", null, false, false, Behavior.KEEP);
        }

        static MainItem action(int iconRes,
                               String title,
                               String description,
                               MainAction action,
                               Behavior behavior) {
            return new MainItem(null, iconRes, title, description, action, false, false, behavior);
        }

        static MainItem setting(int iconRes,
                                String title,
                                String description,
                                boolean enabled,
                                MainAction action,
                                Behavior behavior) {
            return new MainItem(null, iconRes, title, description, action, true, enabled, behavior);
        }
    }

    static final class AdBlockState {
        final boolean master;
        final boolean popup;
        final boolean redirect;
        final boolean scriptIframe;
        final boolean clickHijack;
        final boolean tempTab;
        final boolean autoClose;

        AdBlockState(boolean master,
                     boolean popup,
                     boolean redirect,
                     boolean scriptIframe,
                     boolean clickHijack,
                     boolean tempTab,
                     boolean autoClose) {
            this.master = master;
            this.popup = popup;
            this.redirect = redirect;
            this.scriptIframe = scriptIframe;
            this.clickHijack = clickHijack;
            this.tempTab = tempTab;
            this.autoClose = autoClose;
        }
    }

    static final class CustomizeState {
        final boolean topReload;
        final boolean topBookmark;
        final boolean topTranslate;
        final boolean reload;
        final boolean download;
        final boolean bookmark;
        final boolean privateShortcut;
        final boolean adBlock;
        final boolean reader;
        final boolean nightMode;
        final boolean qrScan;
        final boolean history;
        final boolean findPage;
        final boolean share;
        final boolean fullscreen;
        final boolean blockElement;
        final boolean siteFilter;
        final boolean videoControls;

        CustomizeState(boolean topReload,
                       boolean topBookmark,
                       boolean topTranslate,
                       boolean reload,
                       boolean download,
                       boolean bookmark,
                       boolean privateShortcut,
                       boolean adBlock,
                       boolean reader,
                       boolean nightMode,
                       boolean qrScan,
                       boolean history,
                       boolean findPage,
                       boolean share,
                       boolean fullscreen,
                       boolean blockElement,
                       boolean siteFilter,
                       boolean videoControls) {
            this.topReload = topReload;
            this.topBookmark = topBookmark;
            this.topTranslate = topTranslate;
            this.reload = reload;
            this.download = download;
            this.bookmark = bookmark;
            this.privateShortcut = privateShortcut;
            this.adBlock = adBlock;
            this.reader = reader;
            this.nightMode = nightMode;
            this.qrScan = qrScan;
            this.history = history;
            this.findPage = findPage;
            this.share = share;
            this.fullscreen = fullscreen;
            this.blockElement = blockElement;
            this.siteFilter = siteFilter;
            this.videoControls = videoControls;
        }
    }

    private final Activity activity;
    private final Handler handler;

    SettingsPanelController(Activity activity, Handler handler) {
        this.activity = activity;
        this.handler = handler;
    }

    void showMain(MainState state, ActionHandler<MainAction> actionHandler) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(16), dp(14), dp(16));
        panel.setBackground(YieldUi.roundRect(
                Color.parseColor("#2B2D33"), dp(24), dp(1), Color.parseColor("#3A3D45")));
        scroll.addView(panel);

        TextView title = new TextView(activity);
        title.setText("Setelan Yield");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(dp(8), 0, 0, dp(8));
        panel.addView(title);

        TextView hint = new TextView(activity);
        hint.setText("Pusat semua fitur. Kalau shortcut menu dimatikan, fitur tetap bisa dibuka dari sini.");
        hint.setTextColor(COLOR_SUBTEXT);
        hint.setTextSize(13);
        hint.setPadding(dp(8), 0, dp(8), dp(12));
        panel.addView(hint);

        for (MainItem item : buildMainItems(state)) {
            if (item.section != null) {
                panel.addView(SettingsUi.sectionTitle(activity, item.section));
                continue;
            }
            View row = item.setting
                    ? SettingsUi.settingRow(activity, item.iconRes, item.title,
                            item.description, item.enabled,
                            view -> dispatch(dialog, item.behavior, item.action, actionHandler))
                    : SettingsUi.actionRow(activity, item.iconRes, item.title,
                            item.description,
                            view -> dispatch(dialog, item.behavior, item.action, actionHandler));
            panel.addView(row);
        }

        dialog.setContentView(scroll);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.82f);
            window.setAttributes(params);
        }
        dialog.show();
    }

    void showAdBlock(AdBlockState state, ActionHandler<AdBlockAction> actionHandler) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setBackground(YieldUi.roundRect(
                Color.parseColor("#26292F"), dp(24), dp(1), COLOR_BORDER));

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(14));
        scroll.addView(box, new ScrollView.LayoutParams(-1, -2));

        TextView title = new TextView(activity);
        title.setText("AdBlock");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
        box.addView(title);

        TextView info = new TextView(activity);
        info.setText("Semua perlindungan iklan tetap berada di dalam AdBlock: popup, redirect, click hijack, script, iframe, dan tracker. Media utama tetap diprioritaskan.");
        info.setTextColor(COLOR_SUBTEXT);
        info.setTextSize(13);
        info.setLineSpacing(0, 1.05f);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(-1, -2);
        infoParams.setMargins(0, dp(8), 0, dp(12));
        box.addView(info, infoParams);

        addAdBlockRow(box, "AdBlock aktif", "Master ON/OFF semua proteksi iklan.",
                state.master, AdBlockAction.MASTER, dialog, actionHandler);
        addAdBlockRow(box, "Blokir popup iklan", "Matikan window.open dan pop-up otomatis.",
                state.popup, AdBlockAction.POPUP, dialog, actionHandler);
        addAdBlockRow(box, "Blokir redirect iklan", "Cegah halaman pindah ke domain iklan random.",
                state.redirect, AdBlockAction.REDIRECT, dialog, actionHandler);
        addAdBlockRow(box, "Blokir script/iframe iklan", "Blokir resource iklan, script, iframe, dan tracker.",
                state.scriptIframe, AdBlockAction.SCRIPT_IFRAME, dialog, actionHandler);
        addAdBlockRow(box, "Proteksi click hijack", "Cegah klik area web membuka link iklan tersembunyi.",
                state.clickHijack, AdBlockAction.CLICK_HIJACK, dialog, actionHandler);
        addAdBlockRow(box, "Alihkan iklan ke tab sementara", "Jika web memaksa redirect iklan, buka sebagai tab iklan sementara agar tab utama tetap aman.",
                state.tempTab, AdBlockAction.TEMP_TAB, dialog, actionHandler);
        addAdBlockRow(box, "Auto close tab iklan", "Tab hasil redirect/popup iklan otomatis ditutup agar tab tidak menumpuk.",
                state.autoClose, AdBlockAction.AUTO_CLOSE, dialog, actionHandler);

        TextView note = new TextView(activity);
        note.setText("Catatan: video playback tidak diberi tombol khusus karena selalu dilindungi otomatis agar file video/YouTube/GoogleVideo tidak terblokir.");
        note.setTextColor(COLOR_SUBTEXT);
        note.setTextSize(12);
        note.setLineSpacing(0, 1.05f);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.setMargins(0, dp(8), 0, dp(4));
        box.addView(note, noteParams);

        LinearLayout bottom = new LinearLayout(activity);
        bottom.setGravity(Gravity.END);
        bottom.setPadding(0, dp(8), 0, 0);
        TextView close = dialogTextButton("TUTUP");
        close.setOnClickListener(view -> dialog.dismiss());
        bottom.addView(close);
        box.addView(bottom);

        dialog.setContentView(scroll);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9f);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
    }

    void showCustomize(CustomizeState state,
                       ActionHandler<CustomizeAction> actionHandler) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(16), dp(14), dp(18));
        panel.setBackgroundColor(Color.parseColor("#1E2024"));
        scroll.addView(panel);

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(18));

        TextView back = headerText("‹", 42);
        back.setOnClickListener(view -> dialog.dismiss());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView title = new TextView(activity);
        title.setText("Sesuaikan menu");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView close = headerText("×", 36);
        close.setOnClickListener(view -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
        panel.addView(header);

        panel.addView(customizeSection("Icon atas"));
        addCustomizeRow(panel, R.drawable.ic_refresh, "Reload di address bar",
                state.topReload, CustomizeAction.TOP_RELOAD, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_bookmark, "Bookmark di address bar",
                state.topBookmark, CustomizeAction.TOP_BOOKMARK, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_translate, "Translate di address bar",
                state.topTranslate, CustomizeAction.TOP_TRANSLATE, dialog, actionHandler);

        panel.addView(customizeSection("Menu utama"));
        addCustomizeRow(panel, R.drawable.ic_refresh, "Reload website",
                state.reload, CustomizeAction.RELOAD, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_download_modern, "Unduhan Yield",
                state.download, CustomizeAction.DOWNLOAD, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_bookmark, "Bookmark",
                state.bookmark, CustomizeAction.BOOKMARK, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_private, "Privat",
                state.privateShortcut, CustomizeAction.PRIVATE, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_shield, "AdBlock",
                state.adBlock, CustomizeAction.AD_BLOCK, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_reader, "Reader / Novel Mode",
                state.reader, CustomizeAction.READER, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_night, "Night Mode",
                state.nightMode, CustomizeAction.NIGHT_MODE, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_qr_scan, "Pindai QR Code",
                state.qrScan, CustomizeAction.QR_SCAN, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_history, "Riwayat",
                state.history, CustomizeAction.HISTORY, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_find_page, "Cari di halaman",
                state.findPage, CustomizeAction.FIND_PAGE, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_share, "Bagikan halaman",
                state.share, CustomizeAction.SHARE, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_fullscreen, "Layar penuh",
                state.fullscreen, CustomizeAction.FULLSCREEN, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_block_element, "Blokir elemen",
                state.blockElement, CustomizeAction.BLOCK_ELEMENT, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_safe, "Filter situs ini",
                state.siteFilter, CustomizeAction.SITE_FILTER, dialog, actionHandler);
        addCustomizeRow(panel, R.drawable.ic_video_control, "Kontrol video",
                state.videoControls, CustomizeAction.VIDEO_CONTROLS, dialog, actionHandler);

        dialog.setContentView(scroll);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.9f);
            window.setAttributes(params);
        }
        dialog.show();
    }

    static List<MainItem> buildMainItems(MainState state) {
        ArrayList<MainItem> items = new ArrayList<>();
        items.add(MainItem.action(R.drawable.ic_translate, "Translate",
                "Pilihan translate, hide bar Google Translate, dan translate teks halaman.",
                MainAction.TRANSLATE, Behavior.KEEP));
        items.add(MainItem.section("Pusat fitur"));
        items.add(MainItem.action(R.drawable.ic_download_modern, "Unduhan Yield",
                "Riwayat, progress, lokasi penyimpanan, dan engine 2 koneksi.",
                MainAction.DOWNLOAD_SETTINGS, Behavior.SWITCH));
        items.add(MainItem.action(R.drawable.ic_bookmark, "Bookmark",
                "Buka daftar bookmark yang tersimpan.",
                MainAction.BOOKMARKS, Behavior.SWITCH));
        items.add(MainItem.action(R.drawable.ic_private,
                state.dedicatedPrivateProfile ? "Tab umum" : "Ruang privat",
                state.dedicatedPrivateProfile
                        ? "Kembali ke profil umum tanpa mencampur data sesi privat."
                        : "Buka profil terisolasi tanpa menyimpan riwayat ke sesi umum.",
                MainAction.PROFILE, Behavior.DISMISS));
        items.add(MainItem.action(R.drawable.ic_customize, "Sesuaikan menu",
                "Atur shortcut yang muncul di menu utama.",
                MainAction.CUSTOMIZE, Behavior.SWITCH));
        items.add(MainItem.action(R.drawable.ic_qr_scan, "Pindai QR Code",
                "Scan QR untuk membuka link atau mencari teks.",
                MainAction.QR_SCAN, Behavior.DISMISS));
        items.add(MainItem.action(R.drawable.ic_search_engine,
                "Search engine: " + state.searchEngine,
                "Pilih Google, Bing, DuckDuckGo, Yahoo, atau Yandex.",
                MainAction.SEARCH_ENGINE, Behavior.KEEP));

        items.add(MainItem.section("Alat halaman"));
        items.add(MainItem.action(R.drawable.ic_block_element, "Blokir elemen",
                "Pilih elemen pada halaman aktif untuk disembunyikan. Tetap tersedia di Setelan meski shortcut menu utama dimatikan.",
                MainAction.BLOCK_ELEMENT, Behavior.DISMISS));
        items.add(MainItem.action(R.drawable.ic_safe, "Filter situs ini",
                "Kelola filter dan elemen yang diblokir untuk situs aktif. Tetap tersedia di Setelan meski shortcut menu utama dimatikan.",
                MainAction.SITE_FILTER, Behavior.SWITCH));
        items.add(MainItem.action(R.drawable.ic_history, "Riwayat browsing",
                "Lihat dan buka kembali halaman yang pernah dikunjungi.",
                MainAction.HISTORY, Behavior.SWITCH));
        items.add(MainItem.action(R.drawable.ic_find_page, "Cari di halaman",
                "Cari teks pada halaman web yang sedang terbuka.",
                MainAction.FIND_PAGE, Behavior.KEEP));
        items.add(MainItem.action(R.drawable.ic_share, "Bagikan halaman",
                "Bagikan link halaman saat ini ke aplikasi lain.",
                MainAction.SHARE, Behavior.KEEP));
        items.add(MainItem.action(R.drawable.ic_copy_link, "Salin link halaman",
                "Salin URL halaman saat ini ke clipboard.",
                MainAction.COPY_LINK, Behavior.KEEP));
        items.add(MainItem.action(R.drawable.ic_page_info, "Info halaman",
                "Tampilkan judul dan URL halaman.",
                MainAction.PAGE_INFO, Behavior.KEEP));
        items.add(MainItem.action(R.drawable.ic_fullscreen, "Mode layar penuh",
                "Sembunyikan toolbar dan navigasi sementara.",
                MainAction.FULLSCREEN, Behavior.KEEP));
        items.add(MainItem.setting(R.drawable.ic_video_control, "Kontrol video online",
                "Tampilkan tombol Play, Pause, Stop, dan Speed pada player web.",
                state.videoControlsEnabled, MainAction.VIDEO_CONTROLS, Behavior.SWITCH));
        items.add(MainItem.action(R.drawable.ic_video_control, "Optimasi video online",
                "Buffer booster, HLS prefetch, kualitas, floating player, dan background play.",
                MainAction.VIDEO_OPTIMIZATION, Behavior.KEEP));
        items.add(MainItem.action(R.drawable.ic_save_page, "Simpan halaman offline",
                "Simpan halaman saat ini sebagai web archive.",
                MainAction.SAVE_OFFLINE, Behavior.KEEP));

        items.add(MainItem.section("Fitur browsing"));
        items.add(MainItem.setting(R.drawable.ic_speed, "Mode cepat",
                "Optimasi cache, gambar, dan resource.", state.speedMode,
                MainAction.SPEED_MODE, Behavior.KEEP));
        items.add(MainItem.setting(R.drawable.ic_safe, "Safe browsing",
                "Blokir URL berisiko sederhana.", state.safeMode,
                MainAction.SAFE_MODE, Behavior.KEEP));
        items.add(MainItem.setting(R.drawable.ic_safe, "HTTPS-First",
                "Coba koneksi HTTPS lebih dulu. Jika HTTPS benar-benar tidak tersedia, kembali ke HTTP dengan perlindungan loop. Alamat lokal dan port khusus dikecualikan.",
                state.httpsFirstEnabled, MainAction.HTTPS_FIRST, Behavior.KEEP));
        items.add(MainItem.action(R.drawable.ic_night,
                "Mode Malam: " + state.nightModeLabel,
                "OFF, ON, Auto ikut sistem, dan pengecualian situs. Tidak menutup menu setelan.",
                MainAction.NIGHT_MODE, Behavior.KEEP));
        items.add(MainItem.setting(R.drawable.ic_reader, "Reader / novel mode",
                "Mode baca ringan untuk artikel.", state.readerMode,
                MainAction.READER_MODE, Behavior.KEEP));
        items.add(MainItem.action(R.drawable.ic_shield,
                "AdBlock: " + (state.adBlock ? "ON" : "OFF"),
                state.adBlockSummary,
                MainAction.AD_BLOCK_SETTINGS, Behavior.KEEP));
        items.add(MainItem.setting(R.drawable.ic_data_saver, "Hemat data",
                "Matikan gambar otomatis saat browsing.", state.dataSaver,
                MainAction.DATA_SAVER, Behavior.KEEP));
        items.add(MainItem.setting(R.drawable.ic_desktop, "Desktop mode",
                "Paksa tampilan lebar seperti PC/laptop.", state.desktopMode,
                MainAction.DESKTOP_MODE, Behavior.KEEP));
        items.add(MainItem.action(R.drawable.ic_text_size,
                "Ukuran teks: " + state.textZoom + "%",
                "Atur ukuran teks dengan slider persentase.",
                MainAction.TEXT_ZOOM, Behavior.KEEP));
        items.add(MainItem.action(R.drawable.ic_clear, "Bersihkan cache",
                "Hapus cache WebView.", MainAction.CLEAR_CACHE, Behavior.KEEP));
        items.add(MainItem.section("Informasi"));
        items.add(MainItem.action(R.drawable.ic_info, "Tentang Yield",
                "Versi aplikasi dan informasi developer.",
                MainAction.ABOUT, Behavior.KEEP));
        return items;
    }

    private <A> void dispatch(Dialog dialog,
                              Behavior behavior,
                              A action,
                              ActionHandler<A> actionHandler) {
        if (behavior == Behavior.DISMISS) {
            dialog.dismiss();
            if (actionHandler != null) actionHandler.handle(action, dialog);
        } else if (behavior == Behavior.SWITCH) {
            if (actionHandler != null) actionHandler.handle(action, dialog);
            handler.postDelayed(() -> dismissIfShowing(dialog), 120L);
        } else if (actionHandler != null) {
            actionHandler.handle(action, dialog);
        }
    }

    private void addAdBlockRow(LinearLayout box,
                               String title,
                               String description,
                               boolean enabled,
                               AdBlockAction action,
                               Dialog dialog,
                               ActionHandler<AdBlockAction> actionHandler) {
        box.addView(SettingsUi.adBlockSwitchRow(
                activity, title, description, enabled,
                view -> {
                    if (actionHandler != null) actionHandler.handle(action, dialog);
                }));
    }

    private void addCustomizeRow(LinearLayout panel,
                                 int iconRes,
                                 String title,
                                 boolean enabled,
                                 CustomizeAction action,
                                 Dialog dialog,
                                 ActionHandler<CustomizeAction> actionHandler) {
        panel.addView(SettingsUi.customizeToggleRow(
                activity, iconRes, title, enabled,
                view -> {
                    if (actionHandler != null) actionHandler.handle(action, dialog);
                }));
    }

    private TextView customizeSection(String title) {
        TextView view = new TextView(activity);
        view.setText(title);
        view.setTextColor(COLOR_SUBTEXT);
        view.setTextSize(16);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(dp(8), 0, 0, dp(12));
        return view;
    }

    private TextView headerText(String value, int textSize) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(Color.WHITE);
        view.setTextSize(textSize);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private TextView dialogTextButton(String label) {
        TextView button = new TextView(activity);
        button.setText(label);
        button.setTextColor(Color.parseColor("#77A7FF"));
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(16), dp(10), dp(16), dp(10));
        return button;
    }

    private static void dismissIfShowing(Dialog dialog) {
        try {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    private int dp(int value) {
        return YieldUi.dp(activity, value);
    }
}
