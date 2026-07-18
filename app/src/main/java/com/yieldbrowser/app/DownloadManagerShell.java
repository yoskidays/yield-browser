package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_ACCENT;
import static com.yieldbrowser.app.BrowserConstants.COLOR_BG;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Builds the full-screen Download Manager shell without owning download-engine state. */
final class DownloadManagerShell {
    static final String[] CATEGORIES =
            new String[]{"Semua", "Video", "APK", "Dokumen", "Musik", "Lainnya"};
    static final String[] SORT_OPTIONS =
            new String[]{"Tanggal", "Antrian", "Nama", "Ukuran"};

    interface Callback {
        void onSearchRequested();

        void onSettingsRequested();

        void onSectionSelected(String section);

        void onCategorySelected(String category);

        void onDismissed();
    }

    interface ValueCallback {
        void accept(String value);
    }

    static final class Bindings {
        final Dialog dialog;
        final RecyclerView recyclerView;
        final DownloadListAdapter adapter;
        final LinearLayout categoryPanel;
        final LinearLayout controlsPanel;
        final TextView titleView;
        final TextView storageView;
        final TextView runningTab;
        final TextView completedTab;
        final TextView emptyView;

        Bindings(Dialog dialog,
                 RecyclerView recyclerView,
                 DownloadListAdapter adapter,
                 LinearLayout categoryPanel,
                 LinearLayout controlsPanel,
                 TextView titleView,
                 TextView storageView,
                 TextView runningTab,
                 TextView completedTab,
                 TextView emptyView) {
            this.dialog = dialog;
            this.recyclerView = recyclerView;
            this.adapter = adapter;
            this.categoryPanel = categoryPanel;
            this.controlsPanel = controlsPanel;
            this.titleView = titleView;
            this.storageView = storageView;
            this.runningTab = runningTab;
            this.completedTab = completedTab;
            this.emptyView = emptyView;
        }
    }

    private final Activity activity;

    DownloadManagerShell(Activity activity) {
        this.activity = activity;
    }

    Bindings show(String activeSection,
                  String activeCategory,
                  DownloadListAdapter.Callback adapterCallback,
                  Callback callback) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(10));
        root.setBackgroundColor(COLOR_BG);

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(activity);
        title.setText("Unduhan");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(50), 1));

        ImageButton search = smallIcon(
                R.drawable.ic_search,
                "Cari unduhan",
                view -> {
                    if (callback != null) callback.onSearchRequested();
                });
        header.addView(search, new LinearLayout.LayoutParams(dp(42), dp(42)));
        ImageButton settings = smallIcon(
                R.drawable.ic_settings,
                "Pengaturan unduhan",
                view -> {
                    if (callback != null) callback.onSettingsRequested();
                });
        header.addView(settings, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView close = new TextView(activity);
        close.setText("×");
        close.setTextColor(Color.parseColor("#D7DAE0"));
        close.setTextSize(34);
        close.setGravity(Gravity.CENTER);
        close.setContentDescription("Tutup Download Manager");
        close.setOnClickListener(view -> dialog.dismiss());
        header.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));
        root.addView(header);

        TextView storage = new TextView(activity);
        storage.setTextColor(COLOR_SUBTEXT);
        storage.setTextSize(12);
        storage.setPadding(0, 0, 0, dp(10));
        root.addView(storage);

        LinearLayout sectionTabs = new LinearLayout(activity);
        sectionTabs.setOrientation(LinearLayout.HORIZONTAL);
        sectionTabs.setGravity(Gravity.CENTER_VERTICAL);
        sectionTabs.setPadding(0, dp(2), 0, dp(10));
        TextView running = sectionTab("Mengunduh", callback);
        TextView completed = sectionTab("Selesai", callback);
        sectionTabs.addView(running, new LinearLayout.LayoutParams(0, dp(42), 1));
        LinearLayout.LayoutParams completedParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        completedParams.setMargins(dp(8), 0, 0, 0);
        sectionTabs.addView(completed, completedParams);
        root.addView(sectionTabs);
        styleSectionTabs(running, completed, activeSection);

        HorizontalScrollView categoryScroll = new HorizontalScrollView(activity);
        categoryScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout categoryPanel = new LinearLayout(activity);
        categoryPanel.setOrientation(LinearLayout.HORIZONTAL);
        categoryPanel.setPadding(0, 0, 0, dp(8));
        categoryScroll.addView(categoryPanel);
        root.addView(categoryScroll, new LinearLayout.LayoutParams(-1, -2));
        renderCategories(categoryPanel, activeCategory, callback);

        LinearLayout controls = new LinearLayout(activity);
        controls.setOrientation(LinearLayout.VERTICAL);
        root.addView(controls, new LinearLayout.LayoutParams(-1, -2));

        RecyclerView recycler = new RecyclerView(activity);
        recycler.setLayoutManager(new LinearLayoutManager(activity));
        recycler.setHasFixedSize(false);
        recycler.setItemAnimator(null);
        recycler.setClipToPadding(false);
        recycler.setPadding(0, dp(8), 0, dp(18));
        DownloadListAdapter adapter = new DownloadListAdapter(adapterCallback);
        recycler.setAdapter(adapter);

        FrameLayout frame = new FrameLayout(activity);
        frame.addView(recycler, new FrameLayout.LayoutParams(-1, -1));
        TextView empty = new TextView(activity);
        empty.setTextColor(COLOR_SUBTEXT);
        empty.setTextSize(15);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(20), dp(40), dp(20), dp(40));
        empty.setVisibility(View.GONE);
        frame.addView(empty, new FrameLayout.LayoutParams(-1, -1));
        root.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));

        dialog.setContentView(root);
        dialog.setOnDismissListener(target -> {
            if (callback != null) callback.onDismissed();
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(params);
        }

        return new Bindings(
                dialog,
                recycler,
                adapter,
                categoryPanel,
                controls,
                title,
                storage,
                running,
                completed,
                empty);
    }

    void styleSectionTabs(Bindings bindings, String activeSection) {
        if (bindings == null) return;
        styleSectionTabs(bindings.runningTab, bindings.completedTab, activeSection);
    }

    void renderCategories(Bindings bindings,
                          String activeCategory,
                          Callback callback) {
        if (bindings == null) return;
        renderCategories(bindings.categoryPanel, activeCategory, callback);
    }

    void showSearchDialog(String currentValue, ValueCallback callback) {
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setText(currentValue == null ? "" : currentValue);
        input.setHint("Cari nama file atau sumber");
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(activity)
                .setTitle("Cari unduhan")
                .setView(input)
                .setPositiveButton("Cari", (dialog, which) -> {
                    if (callback != null) callback.accept(input.getText().toString().trim());
                })
                .setNeutralButton("Reset", (dialog, which) -> {
                    if (callback != null) callback.accept("");
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    void showSortDialog(String currentValue, ValueCallback callback) {
        int checked = selectedSortIndex(currentValue);
        new AlertDialog.Builder(activity)
                .setTitle("Urutkan unduhan")
                .setSingleChoiceItems(SORT_OPTIONS, checked, (dialog, which) -> {
                    if (callback != null) callback.accept(SORT_OPTIONS[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    static int selectedSortIndex(String value) {
        if (value == null) return 0;
        for (int index = 0; index < SORT_OPTIONS.length; index++) {
            if (SORT_OPTIONS[index].equals(value)) return index;
        }
        return 0;
    }

    private TextView sectionTab(String label, Callback callback) {
        return SettingsUi.downloadSectionTab(activity, label, view -> {
            if (callback != null) callback.onSectionSelected(label);
        });
    }

    private void styleSectionTabs(TextView running,
                                  TextView completed,
                                  String activeSection) {
        styleSectionTab(running, "Mengunduh".equals(activeSection));
        styleSectionTab(completed, "Selesai".equals(activeSection));
    }

    private void styleSectionTab(TextView tab, boolean selected) {
        if (tab == null) return;
        tab.setTextColor(selected ? Color.parseColor("#141414") : Color.WHITE);
        tab.setBackground(YieldUi.roundRect(
                selected ? COLOR_ACCENT : Color.parseColor("#20242B"),
                dp(17),
                dp(1),
                selected ? COLOR_ACCENT : COLOR_BORDER));
    }

    private void renderCategories(LinearLayout panel,
                                  String activeCategory,
                                  Callback callback) {
        if (panel == null) return;
        panel.removeAllViews();
        for (String label : CATEGORIES) {
            boolean selected = label.equals(activeCategory);
            panel.addView(SettingsUi.downloadCategoryChip(
                    activity,
                    label,
                    selected,
                    view -> {
                        if (callback != null) callback.onCategorySelected(label);
                    }));
        }
    }

    private ImageButton smallIcon(int iconRes,
                                  String description,
                                  View.OnClickListener listener) {
        ImageButton button = new ImageButton(activity);
        button.setImageResource(iconRes);
        button.setColorFilter(Color.parseColor("#E9EDF5"));
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(dp(4), dp(4), dp(4), dp(4));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setContentDescription(description);
        button.setOnClickListener(listener);
        return button;
    }

    private int dp(int value) {
        return YieldUi.dp(activity, value);
    }
}
