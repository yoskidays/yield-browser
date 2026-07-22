package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_ACCENT;
import static com.yieldbrowser.app.BrowserConstants.COLOR_BG;
import static com.yieldbrowser.app.BrowserConstants.COLOR_BORDER;
import static com.yieldbrowser.app.BrowserConstants.COLOR_SUBTEXT;
import static com.yieldbrowser.app.BrowserConstants.COLOR_TEXT;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Owns tab-list dialog composition for the normal and private browser spaces. */
final class TabsPanelController {
    interface Host {
        void selectSpace(boolean privateSpace);

        void createTab(boolean privateSpace);

        void selectTab(TabInfo tab);

        void closeTab(TabInfo tab, boolean privateSpace);

        boolean isActivityFinishing();
    }

    private final Activity activity;
    private final List<TabInfo> tabs;
    private final TabInfo currentTab;
    private final Host host;

    TabsPanelController(Activity activity,
                        List<TabInfo> tabs,
                        TabInfo currentTab,
                        Host host) {
        this.activity = activity;
        this.tabs = tabs;
        this.currentTab = currentTab;
        this.host = host;
    }

    void show(boolean privateSpace) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(14));
        root.setBackgroundColor(COLOR_BG);

        root.addView(buildHeader(dialog, privateSpace));
        root.addView(buildSpaceSelector(dialog, privateSpace), selectorParams());

        TextView hint = new TextView(activity);
        hint.setText(privateSpace
                ? "Tab privat memakai profil terisolasi dan tidak disimpan ke sesi umum."
                : "Tab umum menyimpan sesi agar dapat dipulihkan saat aplikasi dibuka kembali.");
        hint.setTextColor(COLOR_SUBTEXT);
        hint.setTextSize(13);
        hint.setPadding(dp(2), 0, dp(2), dp(12));
        root.addView(hint);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);

        int displayNumber = 0;
        if (tabs != null) {
            for (TabInfo tab : tabs) {
                // Quarantine/ad tabs are implementation details. They must never appear as user
                // tabs while waiting for their short silent auto-close window.
                if (tab == null || tab.closed || tab.adTab || tab.privateTab != privateSpace) continue;
                displayNumber++;
                list.addView(buildTabRow(tab, displayNumber, dialog, privateSpace));
            }
        }
        if (displayNumber == 0) list.addView(buildEmptyState(privateSpace));

        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(params);
        }
        dialog.show();
    }

    static int countVisible(List<TabInfo> tabs, boolean privateSpace) {
        int count = 0;
        if (tabs == null) return 0;
        for (TabInfo tab : tabs) {
            if (tab != null && !tab.closed && !tab.adTab && tab.privateTab == privateSpace) count++;
        }
        return count;
    }

    static String displayTitle(TabInfo tab, boolean privateSpace) {
        if (tab == null || tab.title == null || tab.title.length() == 0) {
            return privateSpace ? "Tab privat" : "Tab baru";
        }
        return tab.title;
    }

    static String displayUrl(TabInfo tab, boolean privateSpace) {
        String value = tab == null || tab.url == null || tab.url.length() == 0
                ? "Halaman awal" : tab.url;
        return privateSpace ? "Privat • " + value : value;
    }

    private LinearLayout buildHeader(Dialog dialog, boolean privateSpace) {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout heading = new LinearLayout(activity);
        heading.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(activity);
        title.setText(privateSpace ? "Tab Privat" : "Tab Umum");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        heading.addView(title);
        TextView count = new TextView(activity);
        count.setText(countVisible(tabs, privateSpace) + " tab terbuka");
        count.setTextColor(COLOR_SUBTEXT);
        count.setTextSize(12);
        heading.addView(count);
        header.addView(heading, new LinearLayout.LayoutParams(0, dp(58), 1));

        TextView plus = new TextView(activity);
        plus.setText("+");
        plus.setContentDescription(privateSpace ? "Buka tab privat baru" : "Buka tab umum baru");
        plus.setTextColor(privateSpace ? Color.WHITE : Color.parseColor("#111111"));
        plus.setTextSize(28);
        plus.setTypeface(Typeface.DEFAULT_BOLD);
        plus.setGravity(Gravity.CENTER);
        plus.setBackground(YieldUi.roundRect(
                privateSpace ? Color.parseColor("#6D28D9") : COLOR_ACCENT,
                dp(18), 0, Color.TRANSPARENT));
        plus.setOnClickListener(view -> {
            dialog.dismiss();
            if (host != null) host.createTab(privateSpace);
        });
        header.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(42)));

        TextView close = new TextView(activity);
        close.setText("×");
        close.setTextColor(Color.parseColor("#D7DAE0"));
        close.setTextSize(34);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(view -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        closeParams.setMargins(dp(8), 0, 0, 0);
        header.addView(close, closeParams);
        return header;
    }

    private LinearLayout buildSpaceSelector(Dialog dialog, boolean privateSpace) {
        LinearLayout selector = new LinearLayout(activity);
        selector.setOrientation(LinearLayout.HORIZONTAL);
        selector.setPadding(dp(4), dp(4), dp(4), dp(4));
        selector.setBackground(YieldUi.roundRect(
                Color.parseColor("#17191F"), dp(20), dp(1), COLOR_BORDER));
        TextView normal = SettingsUi.profileSpaceChip(activity, "Umum", !privateSpace, false);
        TextView privateChip = SettingsUi.profileSpaceChip(activity, "Privat", privateSpace, true);
        normal.setOnClickListener(view -> switchSpace(dialog, privateSpace, false));
        privateChip.setOnClickListener(view -> switchSpace(dialog, privateSpace, true));
        selector.addView(normal, new LinearLayout.LayoutParams(0, dp(42), 1));
        LinearLayout.LayoutParams privateParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        privateParams.setMargins(dp(6), 0, 0, 0);
        selector.addView(privateChip, privateParams);
        return selector;
    }

    private void switchSpace(Dialog dialog, boolean currentPrivateSpace, boolean requestedPrivateSpace) {
        if (currentPrivateSpace == requestedPrivateSpace) return;
        dialog.dismiss();
        if (host != null) host.selectSpace(requestedPrivateSpace);
    }

    private View buildEmptyState(boolean privateSpace) {
        LinearLayout empty = new LinearLayout(activity);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(20), dp(48), dp(20), dp(48));
        ImageView icon = new ImageView(activity);
        icon.setImageResource(privateSpace ? R.drawable.ic_private : R.drawable.ic_tabs);
        icon.setColorFilter(privateSpace ? Color.parseColor("#C4A7FF") : COLOR_ACCENT);
        empty.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView title = new TextView(activity);
        title.setText(privateSpace ? "Belum ada tab privat" : "Belum ada tab umum");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.setMargins(0, dp(14), 0, dp(6));
        empty.addView(title, titleParams);
        TextView hint = new TextView(activity);
        hint.setText("Tekan + untuk membuka tab baru di ruang ini.");
        hint.setTextColor(COLOR_SUBTEXT);
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER);
        empty.addView(hint);
        return empty;
    }

    private View buildTabRow(TabInfo tab,
                             int displayNumber,
                             Dialog dialog,
                             boolean privateSpace) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(10), dp(12));
        boolean active = tab == currentTab;
        int activeColor = privateSpace ? Color.parseColor("#8B5CF6") : COLOR_ACCENT;
        row.setBackground(YieldUi.roundRect(
                active ? Color.parseColor("#20232A") : Color.parseColor("#15171D"),
                dp(18), dp(1), active ? activeColor : COLOR_BORDER));

        TextView badge = new TextView(activity);
        badge.setText(privateSpace ? "P" : String.valueOf(displayNumber));
        badge.setTextColor(privateSpace ? Color.WHITE : Color.parseColor("#111111"));
        badge.setTextSize(13);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(YieldUi.roundRect(
                privateSpace ? Color.parseColor("#6D28D9") : COLOR_ACCENT,
                dp(14), 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        badgeParams.setMargins(0, 0, dp(12), 0);
        row.addView(badge, badgeParams);

        LinearLayout texts = new LinearLayout(activity);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(activity);
        title.setText(displayTitle(tab, privateSpace));
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(title);
        TextView url = new TextView(activity);
        url.setText(displayUrl(tab, privateSpace));
        url.setTextColor(COLOR_SUBTEXT);
        url.setTextSize(12);
        url.setSingleLine(true);
        url.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(url);
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView close = new TextView(activity);
        close.setText("×");
        close.setContentDescription("Tutup tab");
        close.setTextColor(Color.WHITE);
        close.setTextSize(26);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(view -> {
            if (host == null) return;
            host.closeTab(tab, privateSpace);
            if (!host.isActivityFinishing()) {
                dialog.dismiss();
                host.selectSpace(privateSpace);
            }
        });
        row.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));
        row.setOnClickListener(view -> {
            dialog.dismiss();
            if (host != null) host.selectTab(tab);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(params);
        return row;
    }

    private LinearLayout.LayoutParams selectorParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(50));
        params.setMargins(0, dp(8), 0, dp(12));
        return params;
    }

    private int dp(int value) {
        return YieldUi.dp(activity, value);
    }
}
