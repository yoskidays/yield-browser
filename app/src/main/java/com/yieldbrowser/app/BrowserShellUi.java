package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_ACCENT;
import static com.yieldbrowser.app.BrowserConstants.COLOR_BG;
import static com.yieldbrowser.app.BrowserConstants.COLOR_BORDER;
import static com.yieldbrowser.app.BrowserConstants.COLOR_SUBTEXT;
import static com.yieldbrowser.app.BrowserConstants.COLOR_SURFACE_2;
import static com.yieldbrowser.app.BrowserConstants.COLOR_TEXT;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Builds the static browser shell while forwarding all browser actions to MainActivity. */
final class BrowserShellUi {
    interface Host {
        void hideKeyboardAndClearFocus(View view);

        void openAddressBarUrl();

        void openHomeSearchUrl();

        void reloadCurrentWebsite();

        void toggleBookmark();

        void toggleTranslate();

        void showQuickMenu();

        void openNormalBrowserSpace();

        void newTabInCurrentProfile();

        void navigateCurrentTabHome();

        void showBookmarkList();

        void showTabsPanel();

        String currentUrl();

        String normalizeShortcutUrl(String value);

        String guessLabelFromUrl(String url);

        void saveShortcuts();

        void loadFavicon(String url, ImageView target, TextView fallback);

        void showMessage(String message);
    }

    private final Activity activity;
    private final boolean dedicatedPrivateProfile;
    private final List<ShortcutItemData> shortcuts;
    private final Host host;

    private EditText addressBar;
    private EditText homeSearchInput;
    private ImageButton reloadButton;
    private ImageButton bookmarkButton;
    private ImageButton translateButton;
    private LinearLayout shortcutContainer;
    private TextView tabsCountText;

    BrowserShellUi(Activity activity,
                   boolean dedicatedPrivateProfile,
                   List<ShortcutItemData> shortcuts,
                   Host host) {
        this.activity = activity;
        this.dedicatedPrivateProfile = dedicatedPrivateProfile;
        this.shortcuts = shortcuts;
        this.host = host;
    }

    View createTopBar() {
        LinearLayout wrap = new LinearLayout(activity);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(14), dp(12), dp(14), dp(10));
        wrap.setBackgroundColor(COLOR_BG);

        if (dedicatedPrivateProfile) {
            wrap.addView(createPrivateProfileStrip(), new LinearLayout.LayoutParams(-1, dp(42)));
            wrap.addView(YieldUi.space(activity, dp(8)));
        }

        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(12), dp(8), dp(8), dp(8));
        bar.setBackground(YieldUi.roundRect(COLOR_SURFACE_2, dp(18), dp(1), COLOR_BORDER));

        ImageView searchIcon = new ImageView(activity);
        searchIcon.setImageResource(R.drawable.ic_search);
        searchIcon.setColorFilter(Color.parseColor("#D3D8E0"));
        bar.addView(searchIcon, new LinearLayout.LayoutParams(dp(22), dp(22)));

        addressBar = new EditText(activity);
        addressBar.setBackgroundColor(Color.TRANSPARENT);
        addressBar.setHint("Telusuri Google atau ketik URL");
        addressBar.setHintTextColor(Color.parseColor("#A7ADB8"));
        addressBar.setTextColor(COLOR_TEXT);
        addressBar.setTextSize(16);
        addressBar.setSingleLine(true);
        addressBar.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        addressBar.setPadding(dp(10), 0, dp(8), 0);
        addressBar.setOnEditorActionListener((view, actionId, event) -> {
            boolean enter = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_UP;
            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                if (host != null) {
                    host.hideKeyboardAndClearFocus(view);
                    host.openAddressBarUrl();
                }
                return true;
            }
            return false;
        });
        bar.addView(addressBar, new LinearLayout.LayoutParams(0, -2, 1));

        reloadButton = smallTopIcon(
                R.drawable.ic_refresh, "Reload website",
                view -> run(() -> host.reloadCurrentWebsite()));
        bookmarkButton = smallTopIcon(
                R.drawable.ic_star, "Bookmark",
                view -> run(() -> host.toggleBookmark()));
        translateButton = smallTopIcon(
                R.drawable.ic_translate, "Translate",
                view -> run(() -> host.toggleTranslate()));
        ImageButton menu = smallTopIcon(
                R.drawable.ic_menu_more, "Menu",
                view -> run(() -> host.showQuickMenu()));
        addTopButton(bar, reloadButton);
        addTopButton(bar, bookmarkButton);
        addTopButton(bar, translateButton);
        addTopButton(bar, menu);

        wrap.addView(bar, new LinearLayout.LayoutParams(-1, dp(54)));
        return wrap;
    }

    ScrollView createHomeContent() {
        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(10), dp(16), dp(24));
        content.setMinimumHeight(activity.getResources().getDisplayMetrics().heightPixels - dp(120));
        content.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#20232A"), Color.parseColor("#15171C")}));
        content.addView(YieldUi.space(activity, dp(16)));
        content.addView(createHomeTitle());

        TextView subtitle = new TextView(activity);
        subtitle.setText(dedicatedPrivateProfile
                ? "Sesi terisolasi. Riwayat dan data situs tidak disimpan."
                : "Cepat, ringan, dan siap dipakai.");
        subtitle.setTextColor(COLOR_SUBTEXT);
        subtitle.setTextSize(17);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-2, -2);
        subtitleParams.setMargins(0, dp(8), 0, dp(24));
        content.addView(subtitle, subtitleParams);

        if (dedicatedPrivateProfile) {
            content.addView(createPrivateHomeActions());
            content.addView(YieldUi.space(activity, dp(18)));
        }

        content.addView(createHomeSearchCard(), new LinearLayout.LayoutParams(-1, -2));
        content.addView(YieldUi.space(activity, dp(28)));
        content.addView(createShortcutHeader());
        content.addView(YieldUi.space(activity, dp(14)));

        HorizontalScrollView shortcutScroll = new HorizontalScrollView(activity);
        shortcutScroll.setHorizontalScrollBarEnabled(false);
        shortcutContainer = new LinearLayout(activity);
        shortcutContainer.setOrientation(LinearLayout.HORIZONTAL);
        renderShortcuts();
        shortcutScroll.addView(shortcutContainer);
        content.addView(shortcutScroll, new LinearLayout.LayoutParams(-1, -2));
        content.addView(YieldUi.space(activity, dp(64)));

        TextView footer = new TextView(activity);
        footer.setText("Yield Browser • Modern download manager");
        footer.setTextColor(Color.parseColor("#808794"));
        footer.setTextSize(13);
        content.addView(footer);
        scrollView.addView(content, new ScrollView.LayoutParams(-1, -1));
        return scrollView;
    }

    View createBottomNav(int tabCount) {
        LinearLayout nav = new LinearLayout(activity);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(4), dp(8), dp(6));
        nav.setBackgroundColor(Color.parseColor("#090A0D"));
        nav.addView(bottomNavButton(
                R.drawable.ic_home, "Home",
                view -> run(() -> host.navigateCurrentTabHome())));
        nav.addView(bottomNavButton(
                R.drawable.ic_bookmark, "Bookmark",
                view -> run(() -> host.showBookmarkList())));
        nav.addView(bottomNavButton(R.drawable.ic_search, "Search", view -> {
            if (homeSearchInput != null && homeSearchInput.isShown()) homeSearchInput.requestFocus();
            else if (addressBar != null) addressBar.requestFocus();
        }));
        nav.addView(tabsNavButton(tabCount, view -> run(() -> host.showTabsPanel())));
        nav.addView(bottomNavButton(
                R.drawable.ic_menu_more, "Menu",
                view -> run(() -> host.showQuickMenu())));
        return nav;
    }

    void renderShortcuts() {
        if (shortcutContainer == null) return;
        shortcutContainer.removeAllViews();
        if (shortcuts != null) {
            for (ShortcutItemData shortcut : shortcuts) {
                shortcutContainer.addView(shortcutItem(shortcut));
            }
        }
        shortcutContainer.addView(addShortcutItem());
    }

    EditText addressBar() {
        return addressBar;
    }

    EditText homeSearchInput() {
        return homeSearchInput;
    }

    ImageButton reloadButton() {
        return reloadButton;
    }

    ImageButton bookmarkButton() {
        return bookmarkButton;
    }

    ImageButton translateButton() {
        return translateButton;
    }

    TextView tabsCountText() {
        return tabsCountText;
    }

    static String shortcutInitial(String label) {
        if (label == null || label.trim().length() == 0) return "?";
        String trimmed = label.trim();
        if (trimmed.length() >= 2 && trimmed.equals(trimmed.toUpperCase())) {
            return trimmed.substring(0, 2);
        }
        return trimmed.substring(0, 1).toUpperCase();
    }

    static int shortcutColorIndex(String label, int colorCount) {
        if (colorCount <= 0) return 0;
        return Math.abs(label == null ? 0 : label.hashCode()) % colorCount;
    }

    private View createPrivateProfileStrip() {
        LinearLayout strip = new LinearLayout(activity);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setPadding(dp(12), dp(5), dp(6), dp(5));
        strip.setBackground(YieldUi.roundRect(
                Color.parseColor("#26143D"), dp(16), dp(1), Color.parseColor("#6941A5")));
        ImageView icon = new ImageView(activity);
        icon.setImageResource(R.drawable.ic_private);
        icon.setColorFilter(Color.parseColor("#D8C5FF"));
        strip.addView(icon, new LinearLayout.LayoutParams(dp(20), dp(20)));

        LinearLayout texts = new LinearLayout(activity);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(9), 0, dp(8), 0);
        TextView title = new TextView(activity);
        title.setText("Mode Privat");
        title.setTextColor(Color.WHITE);
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        texts.addView(title);
        TextView subtitle = new TextView(activity);
        subtitle.setText("Profil terisolasi");
        subtitle.setTextColor(Color.parseColor("#BFADE2"));
        subtitle.setTextSize(10);
        texts.addView(subtitle);
        strip.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView normal = actionText("Umum", 12);
        normal.setContentDescription("Beralih ke tab umum");
        normal.setTextColor(Color.parseColor("#17121F"));
        normal.setBackground(YieldUi.roundRect(
                Color.parseColor("#D8C5FF"), dp(14), 0, Color.TRANSPARENT));
        normal.setOnClickListener(view -> run(() -> host.openNormalBrowserSpace()));
        strip.addView(normal, new LinearLayout.LayoutParams(dp(72), dp(30)));
        return strip;
    }

    private View createPrivateHomeActions() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackground(YieldUi.roundRect(
                Color.parseColor("#21162F"), dp(20), dp(1), Color.parseColor("#5B3A82")));
        TextView info = new TextView(activity);
        info.setText("Tab pada ruang ini tetap privat. Gunakan ruang Umum untuk sesi browsing biasa.");
        info.setTextColor(Color.parseColor("#D8CDE8"));
        info.setTextSize(13);
        card.addView(info);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView normal = actionText("Buka tab umum", 13);
        normal.setBackground(YieldUi.roundRect(
                Color.parseColor("#37303F"), dp(16), dp(1), Color.parseColor("#665B70")));
        normal.setOnClickListener(view -> run(() -> host.openNormalBrowserSpace()));
        actions.addView(normal, new LinearLayout.LayoutParams(0, -1, 1));
        TextView addPrivate = actionText("+ Tab privat", 13);
        addPrivate.setBackground(YieldUi.roundRect(
                Color.parseColor("#6D28D9"), dp(16), 0, Color.TRANSPARENT));
        addPrivate.setOnClickListener(view -> run(() -> host.newTabInCurrentProfile()));
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(0, -1, 1);
        addParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(addPrivate, addParams);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(42));
        actionsParams.setMargins(0, dp(12), 0, 0);
        card.addView(actions, actionsParams);
        return card;
    }

    private View createHomeTitle() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView yield = new TextView(activity);
        yield.setText("Yield");
        styleHomeTitle(yield, Color.parseColor("#F4F6FA"));
        row.addView(yield);
        TextView browser = new TextView(activity);
        browser.setText(dedicatedPrivateProfile ? " Privat" : " Browser");
        styleHomeTitle(browser, dedicatedPrivateProfile
                ? Color.parseColor("#C4A7FF") : Color.parseColor("#DDA13A"));
        row.addView(browser);
        return row;
    }

    private View createHomeSearchCard() {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(10), dp(10), dp(10));
        card.setBackground(YieldUi.roundRect(
                Color.parseColor("#252830"), dp(26), dp(1), Color.parseColor("#232730")));
        homeSearchInput = new EditText(activity);
        homeSearchInput.setBackgroundColor(Color.TRANSPARENT);
        homeSearchInput.setHint("Telusuri atau ketik URL");
        homeSearchInput.setHintTextColor(Color.parseColor("#9BA2AE"));
        homeSearchInput.setTextColor(COLOR_TEXT);
        homeSearchInput.setTextSize(16);
        homeSearchInput.setSingleLine(true);
        homeSearchInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        homeSearchInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        homeSearchInput.setPadding(0, 0, dp(10), 0);
        homeSearchInput.setOnEditorActionListener((view, actionId, event) -> {
            boolean enter = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_UP;
            if (actionId == EditorInfo.IME_ACTION_GO || enter) {
                if (host != null) {
                    host.hideKeyboardAndClearFocus(view);
                    host.openHomeSearchUrl();
                }
                return true;
            }
            return false;
        });
        card.addView(homeSearchInput, new LinearLayout.LayoutParams(0, -2, 1));
        TextView button = actionText("Cari", 16);
        button.setTextColor(Color.parseColor("#111111"));
        button.setBackground(YieldUi.roundRect(COLOR_ACCENT, dp(24), 0, Color.TRANSPARENT));
        button.setOnClickListener(view -> {
            if (host != null) {
                host.hideKeyboardAndClearFocus(homeSearchInput);
                host.openHomeSearchUrl();
            }
        });
        card.addView(button, new LinearLayout.LayoutParams(dp(96), dp(48)));
        return card;
    }

    private View createShortcutHeader() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(activity);
        title.setText("Pintasan");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView helper = new TextView(activity);
        helper.setText("Tekan lama untuk mengedit");
        helper.setTextColor(COLOR_SUBTEXT);
        helper.setTextSize(13);
        row.addView(helper);
        return row;
    }

    private LinearLayout shortcutItem(ShortcutItemData shortcut) {
        LinearLayout item = shortcutContainerItem();
        FrameLayout iconWrap = new FrameLayout(activity);
        item.addView(iconWrap, new LinearLayout.LayoutParams(dp(60), dp(60)));
        TextView fallback = new TextView(activity);
        fallback.setText(shortcutInitial(shortcut.label));
        fallback.setTextColor(Color.WHITE);
        fallback.setTypeface(Typeface.DEFAULT_BOLD);
        fallback.setTextSize(16);
        fallback.setGravity(Gravity.CENTER);
        fallback.setBackground(YieldUi.roundRect(
                colorForShortcut(shortcut.label), dp(22), dp(1), Color.parseColor("#31353C")));
        iconWrap.addView(fallback, new FrameLayout.LayoutParams(-1, -1));
        ImageView favicon = new ImageView(activity);
        favicon.setPadding(dp(12), dp(12), dp(12), dp(12));
        favicon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        favicon.setBackground(YieldUi.roundRect(
                Color.parseColor("#1C1F26"), dp(22), dp(1), Color.parseColor("#31353C")));
        favicon.setVisibility(View.GONE);
        iconWrap.addView(favicon, new FrameLayout.LayoutParams(-1, -1));
        if (host != null) host.loadFavicon(shortcut.url, favicon, fallback);

        TextView delete = new TextView(activity);
        delete.setText("×");
        delete.setTextColor(Color.WHITE);
        delete.setTextSize(15);
        delete.setTypeface(Typeface.DEFAULT_BOLD);
        delete.setGravity(Gravity.CENTER);
        delete.setBackground(YieldUi.roundRect(
                Color.parseColor("#E5484D"), dp(12), 0, Color.TRANSPARENT));
        delete.setVisibility(View.GONE);
        FrameLayout.LayoutParams deleteParams = new FrameLayout.LayoutParams(dp(24), dp(24));
        deleteParams.gravity = Gravity.TOP | Gravity.START;
        deleteParams.leftMargin = dp(-2);
        deleteParams.topMargin = dp(-2);
        iconWrap.addView(delete, deleteParams);
        item.addView(shortcutLabel(shortcut.label));
        item.setOnClickListener(view -> {
            if (delete.getVisibility() == View.VISIBLE) {
                delete.setVisibility(View.GONE);
                return;
            }
            if (addressBar != null) addressBar.setText(shortcut.url);
            run(() -> host.openAddressBarUrl());
        });
        item.setOnLongClickListener(view -> {
            delete.setVisibility(View.VISIBLE);
            if (host != null) host.showMessage("Ketuk X untuk hapus pintasan");
            return true;
        });
        delete.setOnClickListener(view -> {
            if (shortcuts != null) shortcuts.remove(shortcut);
            if (host != null) host.saveShortcuts();
            renderShortcuts();
            if (host != null) host.showMessage("Pintasan dihapus");
        });
        return item;
    }

    private LinearLayout addShortcutItem() {
        LinearLayout item = shortcutContainerItem();
        TextView circle = new TextView(activity);
        circle.setText("+");
        circle.setTextColor(Color.WHITE);
        circle.setTypeface(Typeface.DEFAULT_BOLD);
        circle.setTextSize(24);
        circle.setGravity(Gravity.CENTER);
        circle.setBackground(YieldUi.roundRect(
                Color.parseColor("#1C1F26"), dp(22), dp(1), Color.parseColor("#31353C")));
        item.addView(circle, new LinearLayout.LayoutParams(dp(60), dp(60)));
        item.addView(shortcutLabel("Tambah"));
        item.setOnClickListener(view -> showAddShortcutDialog());
        return item;
    }

    private void showAddShortcutDialog() {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(4), dp(8), 0);
        EditText name = new EditText(activity);
        name.setHint("Nama pintasan, contoh: YouTube");
        name.setSingleLine(true);
        box.addView(name);
        EditText urlInput = new EditText(activity);
        urlInput.setHint("URL website, contoh: youtube.com");
        urlInput.setSingleLine(true);
        urlInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        if (host != null && host.currentUrl() != null) urlInput.setText(host.currentUrl());
        box.addView(urlInput);
        new AlertDialog.Builder(activity)
                .setTitle("Tambah pintasan")
                .setView(box)
                .setPositiveButton("Tambah", (dialog, which) -> {
                    if (host == null) return;
                    String url = host.normalizeShortcutUrl(urlInput.getText().toString().trim());
                    if (url == null) {
                        host.showMessage("URL tidak valid");
                        return;
                    }
                    String label = name.getText().toString().trim();
                    if (label.length() == 0) label = host.guessLabelFromUrl(url);
                    if (shortcuts != null) {
                        for (int index = shortcuts.size() - 1; index >= 0; index--) {
                            ShortcutItemData old = shortcuts.get(index);
                            if (old != null && url.equals(host.normalizeShortcutUrl(old.url))) {
                                shortcuts.remove(index);
                            }
                        }
                        shortcuts.add(new ShortcutItemData(label, url));
                    }
                    host.saveShortcuts();
                    renderShortcuts();
                    host.showMessage("Pintasan ditambahkan");
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private LinearLayout bottomNavButton(int iconRes,
                                         String description,
                                         View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(activity);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setOnClickListener(listener);
        item.setContentDescription(description);
        item.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1));
        ImageView icon = new ImageView(activity);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#F6F7FA"));
        item.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        return item;
    }

    private LinearLayout tabsNavButton(int tabCount, View.OnClickListener listener) {
        LinearLayout item = bottomNavButton(R.drawable.ic_tabs, "Tabs", listener);
        item.removeAllViews();
        FrameLayout box = new FrameLayout(activity);
        item.addView(box, new LinearLayout.LayoutParams(dp(24), dp(24)));
        ImageView icon = new ImageView(activity);
        icon.setImageResource(R.drawable.ic_tabs);
        icon.setColorFilter(Color.parseColor("#F6F7FA"));
        box.addView(icon, new FrameLayout.LayoutParams(-1, -1));
        tabsCountText = new TextView(activity);
        tabsCountText.setText(String.valueOf(tabCount));
        tabsCountText.setTextColor(Color.parseColor("#F6F7FA"));
        tabsCountText.setTextSize(10);
        tabsCountText.setTypeface(Typeface.DEFAULT_BOLD);
        tabsCountText.setGravity(Gravity.CENTER);
        box.addView(tabsCountText, new FrameLayout.LayoutParams(-1, -1));
        return item;
    }

    private LinearLayout shortcutContainerItem() {
        LinearLayout item = new LinearLayout(activity);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(84), -2);
        params.setMargins(0, 0, dp(10), 0);
        item.setLayoutParams(params);
        return item;
    }

    private TextView shortcutLabel(String value) {
        TextView label = new TextView(activity);
        label.setText(value);
        label.setTextColor(COLOR_TEXT);
        label.setTextSize(13);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(8), 0, 0);
        label.setLayoutParams(params);
        return label;
    }

    private ImageButton smallTopIcon(int iconRes,
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

    private void addTopButton(LinearLayout bar, ImageButton button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(30), dp(30));
        params.setMargins(dp(2), 0, 0, 0);
        bar.addView(button, params);
    }

    private TextView actionText(String value, int size) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(Color.WHITE);
        view.setTextSize(size);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private void styleHomeTitle(TextView view, int color) {
        view.setTextColor(color);
        view.setTextSize(31);
        view.setLetterSpacing(-0.01f);
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    }

    private int colorForShortcut(String label) {
        int[] colors = new int[]{
                Color.parseColor("#4285F4"), Color.parseColor("#24292F"),
                Color.parseColor("#FF0033"), Color.parseColor("#1DA1F2"),
                Color.parseColor("#22C55E"), Color.parseColor("#8B5CF6"),
                Color.parseColor("#F97316")};
        return colors[shortcutColorIndex(label, colors.length)];
    }

    private void run(Runnable action) {
        if (host != null && action != null) action.run();
    }

    private int dp(int value) {
        return YieldUi.dp(activity, value);
    }
}
