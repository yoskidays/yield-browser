package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_ACCENT;
import static com.yieldbrowser.app.BrowserConstants.COLOR_BORDER;
import static com.yieldbrowser.app.BrowserConstants.COLOR_SUBTEXT;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Builds text-zoom and download-folder dialogs while the activity owns state and persistence. */
final class BrowserUtilityDialogsController {
    interface TextZoomHandler {
        void saveZoom(int zoom);

        void reopenSettings();
    }

    interface DownloadFolderHandler {
        void saveSubfolder(String subfolder);

        void choosePhoneFolder(String subfolder);

        void resetDefault(String subfolder);

        void reopenDownloadSettings();
    }

    private final Activity activity;

    BrowserUtilityDialogsController(Activity activity) {
        this.activity = activity;
    }

    void showTextZoom(Dialog parentDialog, int currentZoom, TextZoomHandler handler) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setBackground(YieldUi.roundRect(
                Color.parseColor("#2B2D33"), dp(24), dp(1), Color.parseColor("#3A3D45")));

        TextView title = text("Ukuran teks", Color.WHITE, 22, Typeface.DEFAULT_BOLD);
        panel.addView(title);

        TextView description = text(
                "Geser untuk mengatur ukuran teks halaman web.", COLOR_SUBTEXT, 13, null);
        description.setPadding(0, dp(8), 0, dp(14));
        panel.addView(description);

        int initialZoom = clampZoom(currentZoom);
        TextView percent = text(initialZoom + "%", COLOR_ACCENT, 30, Typeface.DEFAULT_BOLD);
        percent.setGravity(Gravity.CENTER);
        panel.addView(percent, new LinearLayout.LayoutParams(-1, -2));

        SeekBar slider = new SeekBar(activity);
        slider.setMax(80);
        slider.setProgress(initialZoom - 70);
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(-1, dp(52));
        sliderParams.setMargins(0, dp(10), 0, dp(8));
        panel.addView(slider, sliderParams);

        LinearLayout labels = new LinearLayout(activity);
        labels.setOrientation(LinearLayout.HORIZONTAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        labels.addView(label("70%", Gravity.LEFT), new LinearLayout.LayoutParams(0, -2, 1));
        labels.addView(label("100%", Gravity.CENTER), new LinearLayout.LayoutParams(0, -2, 1));
        labels.addView(label("150%", Gravity.RIGHT), new LinearLayout.LayoutParams(0, -2, 1));
        panel.addView(labels);

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams buttonRowParams = new LinearLayout.LayoutParams(-1, dp(52));
        buttonRowParams.setMargins(0, dp(18), 0, 0);

        TextView reset = actionButton("Reset", false);
        buttons.addView(reset, new LinearLayout.LayoutParams(0, dp(46), 1));
        TextView save = actionButton("Simpan", true);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        saveParams.setMargins(dp(10), 0, 0, 0);
        buttons.addView(save, saveParams);
        panel.addView(buttons, buttonRowParams);

        final int[] selectedZoom = new int[]{initialZoom};
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedZoom[0] = progress + 70;
                percent.setText(selectedZoom[0] + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        reset.setOnClickListener(view -> {
            selectedZoom[0] = 100;
            slider.setProgress(30);
            percent.setText("100%");
        });
        save.setOnClickListener(view -> {
            if (handler != null) handler.saveZoom(selectedZoom[0]);
            QuietToast.makeText(activity, "Ukuran teks: " + selectedZoom[0] + "%",
                    QuietToast.LENGTH_SHORT).show();
            dialog.dismiss();
            dismiss(parentDialog);
            if (handler != null) handler.reopenSettings();
        });

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

    void showDownloadFolder(Dialog parentDialog,
                            String subfolder,
                            String locationText,
                            DownloadFolderHandler handler) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(20), dp(22), dp(16));
        box.setBackground(YieldUi.roundRect(
                Color.parseColor("#2B2D33"), dp(22), dp(1), COLOR_BORDER));

        box.addView(text("Folder unduhan", Color.WHITE, 22, Typeface.DEFAULT_BOLD));
        TextView info = text(
                "Default hasil download masuk ke folder Download/Yield Browser. "
                        + "Folder app tetap dipakai sebagai staging agar download 2 koneksi tetap stabil.",
                COLOR_SUBTEXT, 14, null);
        info.setLineSpacing(0, 1.08f);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(-1, -2);
        infoParams.setMargins(0, dp(8), 0, dp(14));
        box.addView(info, infoParams);

        EditText input = new EditText(activity);
        input.setText(subfolder == null ? "Download" : subfolder);
        input.setSingleLine(true);
        input.setHint("Nama subfolder staging");
        input.setHintTextColor(Color.parseColor("#8D929C"));
        input.setTextColor(Color.WHITE);
        input.setTextSize(17);
        input.setSelectAllOnFocus(false);
        try {
            input.setBackgroundTintList(ColorStateList.valueOf(COLOR_ACCENT));
        } catch (Exception ignored) {
        }
        box.addView(input, new LinearLayout.LayoutParams(-1, -2));

        TextView current = text(locationText == null ? "" : locationText, COLOR_SUBTEXT, 12, null);
        current.setLineSpacing(0, 1.05f);
        LinearLayout.LayoutParams currentParams = new LinearLayout.LayoutParams(-1, -2);
        currentParams.setMargins(0, dp(12), 0, dp(10));
        box.addView(current, currentParams);

        TextView choose = darkActionButton("PILIH FOLDER HP");
        choose.setOnClickListener(view -> {
            String value = sanitizeSubfolder(input.getText().toString());
            if (handler != null) handler.choosePhoneFolder(value);
            dialog.dismiss();
            dismiss(parentDialog);
        });
        box.addView(choose, new LinearLayout.LayoutParams(-1, dp(46)));

        TextView reset = darkActionButton("RESET DEFAULT\nDOWNLOAD/YIELD BROWSER");
        reset.setTextSize(12);
        reset.setMaxLines(2);
        reset.setLineSpacing(0, 0.92f);
        reset.setOnClickListener(view -> {
            String value = sanitizeSubfolder(input.getText().toString());
            if (handler != null) handler.resetDefault(value);
            QuietToast.makeText(activity, "Default: Download/Yield Browser",
                    QuietToast.LENGTH_SHORT).show();
            dialog.dismiss();
            dismiss(parentDialog);
            if (handler != null) handler.reopenDownloadSettings();
        });
        box.addView(reset, new LinearLayout.LayoutParams(-1, dp(62)));

        LinearLayout bottom = new LinearLayout(activity);
        bottom.setGravity(Gravity.END);
        bottom.setPadding(0, dp(8), 0, 0);
        TextView cancel = dialogTextButton("BATAL");
        cancel.setOnClickListener(view -> dialog.dismiss());
        bottom.addView(cancel);

        TextView save = dialogTextButton("SIMPAN");
        save.setOnClickListener(view -> {
            String value = sanitizeSubfolder(input.getText().toString());
            if (handler != null) handler.saveSubfolder(value);
            QuietToast.makeText(activity, "Subfolder staging disimpan",
                    QuietToast.LENGTH_SHORT).show();
            dialog.dismiss();
            dismiss(parentDialog);
            if (handler != null) handler.reopenDownloadSettings();
        });
        bottom.addView(save);
        box.addView(bottom);

        dialog.setContentView(box);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.88f);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
    }

    static int clampZoom(int zoom) {
        return Math.max(70, Math.min(150, zoom));
    }

    static String sanitizeSubfolder(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() == 0) clean = "Download";
        return clean.replace("/", "-").replace("\\", "-");
    }

    private TextView text(String value, int color, float size, Typeface typeface) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        if (typeface != null) view.setTypeface(typeface);
        return view;
    }

    private TextView label(String value, int gravity) {
        TextView view = text(value, COLOR_SUBTEXT, 12, null);
        view.setGravity(gravity);
        return view;
    }

    private TextView actionButton(String value, boolean primary) {
        TextView view = text(value, primary ? Color.parseColor("#111111") : Color.WHITE,
                15, Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackground(YieldUi.roundRect(
                primary ? COLOR_ACCENT : Color.parseColor("#2A2E36"),
                dp(18), primary ? 0 : dp(1), primary ? Color.TRANSPARENT : COLOR_BORDER));
        return view;
    }

    private TextView darkActionButton(String value) {
        TextView view = text(value, Color.WHITE, 13, Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(false);
        view.setMaxLines(2);
        view.setLineSpacing(0, 0.95f);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        view.setBackground(YieldUi.roundRect(
                Color.parseColor("#343740"), dp(14), dp(1), COLOR_BORDER));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(46));
        params.setMargins(0, dp(6), 0, 0);
        view.setLayoutParams(params);
        return view;
    }

    private TextView dialogTextButton(String value) {
        TextView view = text(value, COLOR_ACCENT, 13, Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(16), dp(12), dp(16), dp(12));
        return view;
    }

    private static void dismiss(Dialog dialog) {
        if (dialog == null) return;
        try {
            if (dialog.isShowing()) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    private int dp(int value) {
        return YieldUi.dp(activity, value);
    }
}
