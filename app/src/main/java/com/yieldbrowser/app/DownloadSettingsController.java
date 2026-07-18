package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_ACCENT;
import static com.yieldbrowser.app.BrowserConstants.COLOR_BORDER;
import static com.yieldbrowser.app.BrowserConstants.COLOR_SUBTEXT;

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

/** Dialog composition for download settings, queue controls, and advanced download features. */
final class DownloadSettingsController {
    enum Action {
        OPEN_MANAGER,
        CHOOSE_FOLDER,
        CLEAR_COMPLETED,
        TOGGLE_QUEUE,
        SET_MAX_ACTIVE,
        PAUSE_ALL,
        RESUME_ALL,
        SORT_QUEUE,
        TOGGLE_DYNAMIC_CONNECTIONS,
        TOGGLE_AUTO_RETRY,
        TOGGLE_HLS,
        TOGGLE_PLAY_WHILE_DOWNLOADING,
        SET_SPEED_LIMIT
    }

    interface Host {
        State state();

        String queueSummary();

        void handle(Action action, int value, Dialog ownerDialog);
    }

    static final class State {
        final String locationText;
        final boolean queueEnabled;
        final int maxActive;
        final boolean dynamicConnections;
        final boolean autoRetry;
        final boolean hlsEnabled;
        final boolean playWhileDownloading;
        final int speedLimitKbps;

        State(String locationText,
              boolean queueEnabled,
              int maxActive,
              boolean dynamicConnections,
              boolean autoRetry,
              boolean hlsEnabled,
              boolean playWhileDownloading,
              int speedLimitKbps) {
            this.locationText = locationText == null ? "" : locationText;
            this.queueEnabled = queueEnabled;
            this.maxActive = Math.max(1, maxActive);
            this.dynamicConnections = dynamicConnections;
            this.autoRetry = autoRetry;
            this.hlsEnabled = hlsEnabled;
            this.playWhileDownloading = playWhileDownloading;
            this.speedLimitKbps = Math.max(0, speedLimitKbps);
        }
    }

    private static final int[] SPEED_VALUES = new int[]{0, 256, 512, 1024, 2048};
    private static final String[] SPEED_LABELS =
            new String[]{"OFF", "256 KB/s", "512 KB/s", "1024 KB/s", "2048 KB/s"};

    private final Activity activity;
    private final Host host;

    DownloadSettingsController(Activity activity, Host host) {
        this.activity = activity;
        this.host = host;
    }

    void showMain() {
        State state = host.state();
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(18));
        panel.setBackground(YieldUi.roundRect(
                Color.parseColor("#2B2D33"), dp(24), dp(1), Color.parseColor("#3A3D45")));

        TextView title = title("Pengaturan Unduhan");
        panel.addView(title);

        TextView path = new TextView(activity);
        path.setText(state.locationText);
        path.setTextColor(COLOR_SUBTEXT);
        path.setTextSize(13);
        path.setPadding(0, dp(8), 0, dp(12));
        panel.addView(path);

        panel.addView(SettingsUi.actionRow(
                activity,
                R.drawable.ic_download_modern,
                "Buka riwayat unduhan",
                "Lihat file, progress, open, hapus riwayat/file.",
                view -> {
                    dialog.dismiss();
                    host.handle(Action.OPEN_MANAGER, 0, dialog);
                }));
        panel.addView(SettingsUi.actionRow(
                activity,
                R.drawable.ic_folder,
                "Lokasi / folder unduhan",
                "Default: Download/Yield Browser, atau pilih folder HP.",
                view -> host.handle(Action.CHOOSE_FOLDER, 0, dialog)));
        panel.addView(SettingsUi.actionRow(
                activity,
                R.drawable.ic_clear,
                "Bersihkan riwayat selesai",
                "Hanya menghapus riwayat, file tetap aman.",
                view -> {
                    host.handle(Action.CLEAR_COMPLETED, 0, dialog);
                    dialog.dismiss();
                    showMain();
                }));
        panel.addView(SettingsUi.actionRow(
                activity,
                R.drawable.ic_settings,
                "Yield Fast Download",
                advancedSummary(state.speedLimitKbps),
                view -> showAdvanced(dialog)));
        panel.addView(SettingsUi.actionRow(
                activity,
                R.drawable.ic_settings,
                "Download Queue: " + (state.queueEnabled ? "ON" : "OFF"),
                host.queueSummary(),
                view -> showQueue()));

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

    private void showQueue() {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = darkScroll();
        LinearLayout box = dialogBox();
        scroll.addView(box, new ScrollView.LayoutParams(-1, -2));
        box.addView(title("Download Queue"));
        box.addView(description("Atur antrian download tanpa keluar dari menu."));

        final TextView[] statusText = new TextView[1];
        State initial = host.state();
        box.addView(SettingsUi.videoOptSwitchRow(
                activity,
                "Download Queue",
                "Batasi download aktif agar koneksi lebih stabil.",
                initial.queueEnabled,
                view -> {
                    host.handle(Action.TOGGLE_QUEUE, 0, dialog);
                    updateQueueStatus(statusText[0]);
                }));

        TextView maxTitle = new TextView(activity);
        maxTitle.setText("Batas maksimal download aktif");
        maxTitle.setTextColor(Color.WHITE);
        maxTitle.setTextSize(15);
        maxTitle.setTypeface(Typeface.DEFAULT_BOLD);
        maxTitle.setPadding(0, dp(8), 0, dp(8));
        box.addView(maxTitle);

        LinearLayout choices = new LinearLayout(activity);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        choices.setGravity(Gravity.CENTER);
        choices.setPadding(0, 0, 0, dp(8));
        box.addView(choices, new LinearLayout.LayoutParams(-1, -2));

        final TextView[] chips = new TextView[3];
        Runnable refreshChoices = () -> {
            int selected = host.state().maxActive;
            updateQueueChoice(chips[0], 2, selected);
            updateQueueChoice(chips[1], 3, selected);
            updateQueueChoice(chips[2], 4, selected);
            updateQueueStatus(statusText[0]);
        };
        chips[0] = queueChoice("2", 2, dialog, refreshChoices);
        chips[1] = queueChoice("3", 3, dialog, refreshChoices);
        chips[2] = queueChoice("4", 4, dialog, refreshChoices);
        choices.addView(chips[0], new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        secondParams.setMargins(dp(8), 0, 0, 0);
        choices.addView(chips[1], secondParams);
        LinearLayout.LayoutParams thirdParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        thirdParams.setMargins(dp(8), 0, 0, 0);
        choices.addView(chips[2], thirdParams);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(6), 0, dp(8));
        box.addView(actions, new LinearLayout.LayoutParams(-1, -2));

        TextView pause = toolButton("Pause semua");
        pause.setOnClickListener(view -> {
            host.handle(Action.PAUSE_ALL, 0, dialog);
            updateQueueStatus(statusText[0]);
        });
        actions.addView(pause, new LinearLayout.LayoutParams(0, dp(42), 1));

        TextView resume = toolButton("Resume semua");
        resume.setOnClickListener(view -> {
            host.handle(Action.RESUME_ALL, 0, dialog);
            updateQueueStatus(statusText[0]);
        });
        LinearLayout.LayoutParams resumeParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        resumeParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(resume, resumeParams);

        TextView sort = toolButton("Urutkan: Antrian");
        sort.setOnClickListener(view -> host.handle(Action.SORT_QUEUE, 0, dialog));
        box.addView(sort, new LinearLayout.LayoutParams(-1, dp(42)));

        statusText[0] = new TextView(activity);
        statusText[0].setTextColor(COLOR_SUBTEXT);
        statusText[0].setTextSize(12);
        statusText[0].setGravity(Gravity.CENTER);
        statusText[0].setPadding(0, dp(12), 0, dp(4));
        box.addView(statusText[0]);
        refreshChoices.run();

        addCloseRow(box, dialog, null);
        configureCompactDialog(dialog, scroll, 0.9f, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void showAdvanced(Dialog parentDialog) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = darkScroll();
        LinearLayout box = dialogBox();
        scroll.addView(box, new ScrollView.LayoutParams(-1, -2));
        box.addView(title("Yield Fast Download"));

        TextView info = description(
                "Safe Brave-Class Layer v3: memilih Safe 1, Stable 2, Balanced 3, atau Turbo 4 koneksi sesuai host, file, dan prediksi bandwidth.");
        info.setLineSpacing(0, 1.05f);
        box.addView(info);

        State state = host.state();
        box.addView(advancedToggle(
                "Smart Turbo Download",
                "Auto pilih Safe 1, Stable 2, Balanced 3, atau Turbo 4 koneksi. Ada fallback aman jika server mulai throttle.",
                state.dynamicConnections,
                Action.TOGGLE_DYNAMIC_CONNECTIONS,
                dialog));
        box.addView(advancedToggle(
                "Retry otomatis",
                "Jika koneksi putus, Yield mencoba ulang sampai 3x dan lanjut dari progres terakhir.",
                state.autoRetry,
                Action.TOGGLE_AUTO_RETRY,
                dialog));
        box.addView(advancedToggle(
                "Download HLS/m3u8",
                "Playlist m3u8 akan dideteksi dan segmen video digabung ke file TS.",
                state.hlsEnabled,
                Action.TOGGLE_HLS,
                dialog));
        box.addView(advancedToggle(
                "Putar sambil mengunduh",
                "Video progresif dapat ditonton dari bagian yang sudah tersedia. Download tetap berjalan di latar belakang.",
                state.playWhileDownloading,
                Action.TOGGLE_PLAY_WHILE_DOWNLOADING,
                dialog));

        box.addView(SettingsUi.advancedInfoRow(activity, "Player internal + HTTP Range lokal"));
        box.addView(SettingsUi.advancedInfoRow(activity, "Smart resume + hard pause"));
        box.addView(SettingsUi.advancedInfoRow(activity, "Bandwidth prediction"));
        box.addView(SettingsUi.advancedInfoRow(activity, "Per-file optimization"));
        box.addView(SettingsUi.advancedInfoRow(activity, "Host-aware stable mode"));

        TextView limiter = darkActionButton(
                "SPEED LIMITER: " + speedLabel(state.speedLimitKbps));
        limiter.setOnClickListener(view -> showSpeedLimiter(dialog, parentDialog));
        LinearLayout.LayoutParams limiterParams = new LinearLayout.LayoutParams(-1, dp(46));
        limiterParams.setMargins(0, dp(2), 0, 0);
        box.addView(limiter, limiterParams);

        addCloseRow(box, dialog, () -> {
            if (parentDialog != null) {
                parentDialog.dismiss();
                showMain();
            }
        });
        configureCompactDialog(
                dialog,
                scroll,
                0.9f,
                (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.78f));
    }

    private void showSpeedLimiter(Dialog advancedDialog, Dialog parentDialog) {
        State state = host.state();
        int checked = speedIndex(state.speedLimitKbps);
        new AlertDialog.Builder(activity)
                .setTitle("Speed limiter")
                .setSingleChoiceItems(SPEED_LABELS, checked, (prompt, which) -> {
                    host.handle(Action.SET_SPEED_LIMIT, SPEED_VALUES[which], advancedDialog);
                    prompt.dismiss();
                    if (advancedDialog != null) advancedDialog.dismiss();
                    showAdvanced(parentDialog);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    static String advancedSummary(int speedLimitKbps) {
        return "Dynamic 2/4 koneksi, retry, HLS/m3u8, speed limiter: "
                + (speedLimitKbps > 0 ? speedLimitKbps + " KB/s" : "tanpa limit");
    }

    static String speedLabel(int speedLimitKbps) {
        return speedLimitKbps > 0 ? speedLimitKbps + " KB/s" : "OFF";
    }

    static int speedIndex(int speedLimitKbps) {
        for (int index = 0; index < SPEED_VALUES.length; index++) {
            if (SPEED_VALUES[index] == speedLimitKbps) return index;
        }
        return 0;
    }

    private View advancedToggle(String title,
                                String description,
                                boolean enabled,
                                Action action,
                                Dialog dialog) {
        return SettingsUi.advancedSwitchRow(
                activity,
                title,
                description,
                enabled,
                view -> host.handle(action, 0, dialog));
    }

    private TextView queueChoice(String label,
                                 int value,
                                 Dialog dialog,
                                 Runnable refresh) {
        TextView chip = SettingsUi.queueChoiceChip(activity, label, view -> {
            host.handle(Action.SET_MAX_ACTIVE, value, dialog);
            if (refresh != null) refresh.run();
        });
        updateQueueChoice(chip, value, host.state().maxActive);
        return chip;
    }

    private void updateQueueChoice(TextView chip, int value, int selected) {
        if (chip == null) return;
        boolean active = selected == value;
        chip.setTextColor(active ? Color.parseColor("#111111") : Color.WHITE);
        chip.setBackground(YieldUi.roundRect(
                active ? COLOR_ACCENT : Color.parseColor("#20232A"),
                dp(18),
                dp(1),
                active ? COLOR_ACCENT : COLOR_BORDER));
    }

    private void updateQueueStatus(TextView status) {
        if (status != null) status.setText(host.queueSummary());
    }

    private ScrollView darkScroll() {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setBackground(YieldUi.roundRect(
                Color.parseColor("#26292F"), dp(24), dp(1), COLOR_BORDER));
        return scroll;
    }

    private LinearLayout dialogBox() {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(14));
        return box;
    }

    private TextView title(String text) {
        TextView title = new TextView(activity);
        title.setText(text);
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
        return title;
    }

    private TextView description(String text) {
        TextView info = new TextView(activity);
        info.setText(text);
        info.setTextColor(COLOR_SUBTEXT);
        info.setTextSize(13);
        info.setPadding(0, dp(8), 0, dp(12));
        return info;
    }

    private TextView toolButton(String text) {
        TextView button = new TextView(activity);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(YieldUi.roundRect(
                Color.parseColor("#20232A"), dp(14), dp(1), COLOR_BORDER));
        return button;
    }

    private TextView darkActionButton(String text) {
        TextView button = toolButton(text);
        button.setTextColor(Color.parseColor("#E7EBF2"));
        return button;
    }

    private void addCloseRow(LinearLayout box, Dialog dialog, Runnable afterClose) {
        LinearLayout bottom = new LinearLayout(activity);
        bottom.setGravity(Gravity.END);
        bottom.setPadding(0, dp(8), 0, 0);
        TextView close = new TextView(activity);
        close.setText("TUTUP");
        close.setTextColor(Color.parseColor("#77A7FF"));
        close.setTextSize(14);
        close.setTypeface(Typeface.DEFAULT_BOLD);
        close.setGravity(Gravity.CENTER);
        close.setPadding(dp(16), dp(10), dp(16), dp(10));
        close.setOnClickListener(view -> {
            dialog.dismiss();
            if (afterClose != null) afterClose.run();
        });
        bottom.addView(close);
        box.addView(bottom);
    }

    private void configureCompactDialog(Dialog dialog,
                                        View content,
                                        float widthFraction,
                                        int height) {
        dialog.setContentView(content);
        dialog.show();
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * widthFraction);
        params.height = height;
        window.setAttributes(params);
    }

    private int dp(int value) {
        return YieldUi.dp(activity, value);
    }
}
