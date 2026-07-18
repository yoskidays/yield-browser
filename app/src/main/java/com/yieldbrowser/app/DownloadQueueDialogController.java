package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_ACCENT;
import static com.yieldbrowser.app.BrowserConstants.COLOR_BORDER;
import static com.yieldbrowser.app.BrowserConstants.COLOR_SUBTEXT;

import android.app.Activity;
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

/** Owns Download Queue dialog composition while the activity owns queue state and engine actions. */
final class DownloadQueueDialogController {
    interface MaxActiveHandler {
        void set(int value);
    }

    interface SummaryProvider {
        String get();
    }

    static final class State {
        final boolean queueEnabled;
        final int maxActive;

        State(boolean queueEnabled, int maxActive) {
            this.queueEnabled = queueEnabled;
            this.maxActive = normalizeMaxActive(maxActive);
        }
    }

    private final Activity activity;

    DownloadQueueDialogController(Activity activity) {
        this.activity = activity;
    }

    void show(State state,
              Runnable toggleQueue,
              MaxActiveHandler setMaxActive,
              Runnable pauseAll,
              Runnable resumeAll,
              Runnable sortByQueue,
              SummaryProvider summaryProvider) {
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
        title.setText("Download Queue");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setIncludeFontPadding(false);
        box.addView(title);

        TextView info = new TextView(activity);
        info.setText("Atur antrian download tanpa keluar dari menu.");
        info.setTextColor(COLOR_SUBTEXT);
        info.setTextSize(13);
        info.setPadding(0, dp(8), 0, dp(12));
        box.addView(info);

        final TextView[] status = new TextView[1];
        final int[] selectedMax = new int[]{state == null ? 2 : state.maxActive};
        boolean enabled = state != null && state.queueEnabled;
        box.addView(SettingsUi.videoOptSwitchRow(
                activity,
                "Download Queue",
                "Batasi download aktif agar koneksi lebih stabil.",
                enabled,
                view -> {
                    run(toggleQueue);
                    refreshStatus(status[0], summaryProvider);
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
            styleChip(chips[0], 2, selectedMax[0]);
            styleChip(chips[1], 3, selectedMax[0]);
            styleChip(chips[2], 4, selectedMax[0]);
            refreshStatus(status[0], summaryProvider);
        };
        chips[0] = choiceChip("2", 2, selectedMax, setMaxActive, refreshChoices);
        chips[1] = choiceChip("3", 3, selectedMax, setMaxActive, refreshChoices);
        chips[2] = choiceChip("4", 4, selectedMax, setMaxActive, refreshChoices);
        choices.addView(chips[0], new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams middleParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        middleParams.setMargins(dp(8), 0, 0, 0);
        choices.addView(chips[1], middleParams);
        LinearLayout.LayoutParams lastParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        lastParams.setMargins(dp(8), 0, 0, 0);
        choices.addView(chips[2], lastParams);
        refreshChoices.run();

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, dp(6), 0, dp(8));
        box.addView(actions, new LinearLayout.LayoutParams(-1, -2));

        TextView pause = DownloadControlsFactory.createButton(activity, "Pause semua");
        pause.setOnClickListener(view -> {
            run(pauseAll);
            refreshStatus(status[0], summaryProvider);
        });
        actions.addView(pause, new LinearLayout.LayoutParams(0, dp(42), 1));

        TextView resume = DownloadControlsFactory.createButton(activity, "Resume semua");
        resume.setOnClickListener(view -> {
            run(resumeAll);
            refreshStatus(status[0], summaryProvider);
        });
        LinearLayout.LayoutParams resumeParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        resumeParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(resume, resumeParams);

        TextView sort = DownloadControlsFactory.createButton(activity, "Urutkan: Antrian");
        sort.setOnClickListener(view -> run(sortByQueue));
        box.addView(sort, new LinearLayout.LayoutParams(-1, dp(42)));

        status[0] = new TextView(activity);
        status[0].setTextColor(COLOR_SUBTEXT);
        status[0].setTextSize(12);
        status[0].setGravity(Gravity.CENTER);
        status[0].setPadding(0, dp(12), 0, dp(4));
        box.addView(status[0]);
        refreshStatus(status[0], summaryProvider);

        LinearLayout bottom = new LinearLayout(activity);
        bottom.setGravity(Gravity.END);
        bottom.setPadding(0, dp(8), 0, 0);
        TextView close = new TextView(activity);
        close.setText("TUTUP");
        close.setTextColor(COLOR_ACCENT);
        close.setTextSize(13);
        close.setTypeface(Typeface.DEFAULT_BOLD);
        close.setGravity(Gravity.CENTER);
        close.setPadding(dp(16), dp(12), dp(16), dp(12));
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

    static int normalizeMaxActive(int value) {
        return Math.max(2, Math.min(4, value));
    }

    private TextView choiceChip(String label,
                                int value,
                                int[] selectedMax,
                                MaxActiveHandler handler,
                                Runnable refresh) {
        TextView chip = SettingsUi.queueChoiceChip(activity, label, view -> {
            selectedMax[0] = value;
            if (handler != null) handler.set(value);
            run(refresh);
        });
        styleChip(chip, value, selectedMax[0]);
        return chip;
    }

    private void styleChip(TextView chip, int value, int selected) {
        if (chip == null) return;
        boolean active = value == selected;
        chip.setTextColor(active ? Color.parseColor("#111111") : Color.WHITE);
        chip.setBackground(YieldUi.roundRect(
                active ? COLOR_ACCENT : Color.parseColor("#20232A"),
                dp(18), dp(1), active ? COLOR_ACCENT : COLOR_BORDER));
    }

    private static void refreshStatus(TextView status, SummaryProvider provider) {
        if (status != null && provider != null) status.setText(provider.get());
    }

    private static void run(Runnable action) {
        if (action != null) action.run();
    }

    private int dp(int value) {
        return YieldUi.dp(activity, value);
    }
}
