package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_BG;
import static com.yieldbrowser.app.BrowserConstants.COLOR_SUBTEXT;
import static com.yieldbrowser.app.BrowserConstants.KEY_BOOKMARK_FOLDERS;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Owns bookmark folders, rows, menus, editing, sorting, and full-screen dialogs. */
final class BookmarkPanelController {
    interface UrlNormalizer {
        String normalize(String value);
    }

    interface UrlOpener {
        void open(String url);
    }

    interface FaviconBinder {
        void bind(String url, ImageView target, TextView fallback);
    }

    private final Activity activity;
    private final Handler handler;
    private final List<BookmarkItemData> bookmarks;
    private final SharedPreferences preferences;
    private final UrlNormalizer urlNormalizer;
    private final FaviconBinder faviconBinder;
    private final UrlOpener currentUrlOpener;
    private final UrlOpener newTabOpener;
    private final UrlOpener privateUrlOpener;

    BookmarkPanelController(Activity activity,
                            Handler handler,
                            List<BookmarkItemData> bookmarks,
                            SharedPreferences preferences,
                            UrlNormalizer urlNormalizer,
                            FaviconBinder faviconBinder,
                            UrlOpener currentUrlOpener,
                            UrlOpener newTabOpener,
                            UrlOpener privateUrlOpener) {
        this.activity = activity;
        this.handler = handler;
        this.bookmarks = bookmarks;
        this.preferences = preferences;
        this.urlNormalizer = urlNormalizer;
        this.faviconBinder = faviconBinder;
        this.currentUrlOpener = currentUrlOpener;
        this.newTabOpener = newTabOpener;
        this.privateUrlOpener = privateUrlOpener;
    }

    void showHome() {
        Dialog dialog = createFullscreenDialog();
        LinearLayout root = createRoot();
        LinearLayout top = createTopRow();

        ImageButton back = plainIconButton(R.drawable.ic_back, view -> dialog.dismiss());
        top.addView(back, new LinearLayout.LayoutParams(dp(40), dp(40)));
        addTitle(top, "Bookmark");
        addHeaderActions(top, dialog);
        root.addView(top);

        EditText search = darkSearchInput("Telusuri bookmark");
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(54));
        searchParams.setMargins(0, dp(14), 0, dp(16));
        root.addView(search, searchParams);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        for (String folder : BookmarkStore.getFolders(bookmarks, preferences)) {
            list.addView(folderRow(
                    folder,
                    BookmarkStore.countInFolder(bookmarks, folder),
                    dialog));
        }

        dialog.setContentView(root);
        dialog.show();
        FullscreenDialogStyler.apply(activity, dialog);
    }

    private void showFolder(String folder) {
        Dialog dialog = createFullscreenDialog();
        LinearLayout root = createRoot();
        LinearLayout top = createTopRow();

        ImageButton back = plainIconButton(
                R.drawable.ic_back,
                view -> switchDialogSmooth(dialog, this::showHome));
        top.addView(back, new LinearLayout.LayoutParams(dp(40), dp(40)));
        addTitle(top, folder);
        addHeaderActions(top, dialog);
        root.addView(top);

        EditText search = darkSearchInput("Telusuri bookmark");
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(54));
        searchParams.setMargins(0, dp(14), 0, dp(12));
        root.addView(search, searchParams);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        final Runnable[] renderRef = new Runnable[1];
        renderRef[0] = () -> renderFolder(folder, search, list, dialog, renderRef[0]);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                if (renderRef[0] != null) renderRef[0].run();
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
        renderRef[0].run();

        dialog.setContentView(root);
        dialog.show();
        FullscreenDialogStyler.apply(activity, dialog);
    }

    private void renderFolder(String folder,
                              EditText search,
                              LinearLayout list,
                              Dialog dialog,
                              Runnable refresh) {
        list.removeAllViews();
        String query = search.getText().toString().trim().toLowerCase(Locale.US);
        List<BookmarkItemData> visible = filterFolder(bookmarks, folder, query);
        if (visible.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText("Belum ada bookmark di folder ini.");
            empty.setTextColor(COLOR_SUBTEXT);
            empty.setTextSize(16);
            empty.setPadding(0, dp(20), 0, 0);
            list.addView(empty);
            return;
        }
        for (BookmarkItemData item : visible) {
            list.addView(itemRow(item, dialog, refresh));
        }
    }

    private View folderRow(String folder, int count, Dialog parent) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(14), 0, dp(14));
        row.setOnClickListener(view -> switchDialogSmooth(parent, () -> showFolder(folder)));

        ImageView icon = new ImageView(activity);
        icon.setImageResource(R.drawable.ic_folder);
        icon.setColorFilter(Color.parseColor("#5B2A1F"));
        icon.setPadding(dp(12), dp(12), dp(12), dp(12));
        icon.setBackground(YieldUi.roundRect(
                Color.parseColor("#E6E8EF"), dp(28), 0, Color.TRANSPARENT));
        row.addView(icon, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout texts = new LinearLayout(activity);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMargins(dp(14), 0, 0, 0);
        TextView title = new TextView(activity);
        title.setText(folder + " (" + count + ")");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        texts.addView(title);
        row.addView(texts, textParams);
        return row;
    }

    private View itemRow(BookmarkItemData item, Dialog dialog, Runnable refresh) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        row.setOnClickListener(view -> {
            dialog.dismiss();
            if (currentUrlOpener != null) currentUrlOpener.open(item.url);
        });

        FrameLayout iconWrap = new FrameLayout(activity);
        TextView fallback = circleLetter(
                bookmarkInitial(item), Color.parseColor("#1F232A"), Color.WHITE);
        iconWrap.addView(fallback, new FrameLayout.LayoutParams(-1, -1));
        ImageView favicon = new ImageView(activity);
        favicon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        favicon.setPadding(dp(9), dp(9), dp(9), dp(9));
        favicon.setBackground(YieldUi.roundRect(
                Color.parseColor("#2A2D33"), dp(23), 0, Color.TRANSPARENT));
        favicon.setVisibility(View.GONE);
        iconWrap.addView(favicon, new FrameLayout.LayoutParams(-1, -1));
        if (faviconBinder != null) faviconBinder.bind(item.url, favicon, fallback);
        row.addView(iconWrap, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout texts = new LinearLayout(activity);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMargins(dp(14), 0, dp(8), 0);
        TextView title = new TextView(activity);
        title.setText(safeTitle(item));
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setSingleLine(true);
        texts.addView(title);
        TextView url = new TextView(activity);
        url.setText(shortHost(item.url));
        url.setTextColor(COLOR_SUBTEXT);
        url.setTextSize(13);
        url.setSingleLine(true);
        texts.addView(url);
        row.addView(texts, textParams);

        TextView more = new TextView(activity);
        more.setText("⋮");
        more.setTextColor(Color.parseColor("#D3D8E1"));
        more.setTextSize(22);
        more.setGravity(Gravity.CENTER);
        more.setOnClickListener(view -> showItemMenu(view, item, dialog, refresh));
        row.addView(more, new LinearLayout.LayoutParams(dp(36), dp(36)));
        return row;
    }

    private void showItemMenu(View anchor,
                              BookmarkItemData item,
                              Dialog dialog,
                              Runnable refresh) {
        PopupMenu menu = new PopupMenu(activity, anchor);
        menu.getMenu().add("Pilih");
        menu.getMenu().add("Edit");
        menu.getMenu().add("Salin link");
        menu.getMenu().add("Pindahkan ke...");
        menu.getMenu().add("Hapus");
        menu.getMenu().add("Berpindah ke atas");
        menu.getMenu().add("Buka di tab baru");
        menu.getMenu().add("Buka di tab Privat");
        menu.setOnMenuItemClickListener(menuItem -> {
            String title = String.valueOf(menuItem.getTitle());
            if ("Pilih".equals(title)) {
                toast("Mode pilih bookmark aktif");
            } else if ("Edit".equals(title)) {
                showEditDialog(item, dialog, refresh);
            } else if ("Salin link".equals(title)) {
                ClipboardManager clipboard = (ClipboardManager)
                        activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("bookmark", item.url));
                }
                toast("Link bookmark disalin");
            } else if ("Pindahkan ke...".equals(title)) {
                showMoveDialog(item, dialog, refresh);
            } else if ("Hapus".equals(title)) {
                bookmarks.remove(item);
                save();
                refreshDialogSmooth(dialog, refresh);
            } else if ("Berpindah ke atas".equals(title)) {
                bookmarks.remove(item);
                bookmarks.add(0, item);
                save();
                refreshDialogSmooth(dialog, refresh);
            } else if ("Buka di tab baru".equals(title)) {
                dialog.dismiss();
                if (newTabOpener != null) newTabOpener.open(item.url);
            } else if ("Buka di tab Privat".equals(title)) {
                dialog.dismiss();
                if (privateUrlOpener != null) privateUrlOpener.open(item.url);
            }
            return true;
        });
        menu.show();
    }

    private void showEditDialog(BookmarkItemData item, Dialog dialog, Runnable refresh) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(4), dp(8), 0);
        EditText title = new EditText(activity);
        title.setHint("Judul");
        title.setText(item.title);
        box.addView(title);
        EditText url = new EditText(activity);
        url.setHint("URL");
        url.setText(item.url);
        box.addView(url);
        new AlertDialog.Builder(activity)
                .setTitle("Edit bookmark")
                .setView(box)
                .setPositiveButton("Simpan", (prompt, which) -> {
                    item.title = title.getText().toString().trim();
                    String typedUrl = url.getText().toString().trim();
                    String normalized = urlNormalizer == null ? null : urlNormalizer.normalize(typedUrl);
                    item.url = normalized == null ? typedUrl : normalized;
                    save();
                    refreshDialogSmooth(dialog, refresh);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showMoveDialog(BookmarkItemData item, Dialog dialog, Runnable refresh) {
        List<String> folders = BookmarkStore.getFolders(bookmarks, preferences);
        String[] choices = folders.toArray(new String[0]);
        new AlertDialog.Builder(activity)
                .setTitle("Pindahkan ke folder")
                .setItems(choices, (prompt, which) -> {
                    item.folder = choices[which];
                    save();
                    refreshDialogSmooth(dialog, refresh);
                })
                .show();
    }

    private void showCreateFolderDialog(Runnable onDone) {
        EditText input = new EditText(activity);
        input.setHint("Nama folder");
        new AlertDialog.Builder(activity)
                .setTitle("Folder bookmark baru")
                .setView(input)
                .setPositiveButton("Tambah", (prompt, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.length() == 0) return;
                    Set<String> folders = new LinkedHashSet<>(
                            BookmarkStore.getFolders(bookmarks, preferences));
                    folders.add(name);
                    preferences.edit()
                            .putStringSet(KEY_BOOKMARK_FOLDERS, new LinkedHashSet<>(folders))
                            .apply();
                    toast("Folder ditambahkan");
                    if (onDone != null) onDone.run();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showSortMenu(View anchor) {
        PopupMenu menu = new PopupMenu(activity, anchor);
        menu.getMenu().add("Urutkan terbaru");
        menu.getMenu().add("Urutkan A-Z");
        menu.setOnMenuItemClickListener(item -> {
            String title = String.valueOf(item.getTitle());
            if (title.contains("A-Z")) {
                Collections.sort(bookmarks,
                        (left, right) -> safeTitle(left).compareToIgnoreCase(safeTitle(right)));
            } else {
                Collections.sort(bookmarks, (left, right) -> Long.compare(right.time, left.time));
            }
            save();
            toast("Urutan bookmark diperbarui");
            return true;
        });
        menu.show();
    }

    private void showMainMenu(View anchor, Dialog dialog) {
        PopupMenu menu = new PopupMenu(activity, anchor);
        menu.getMenu().add("Pilih");
        menu.getMenu().add("Tutup");
        menu.setOnMenuItemClickListener(item -> {
            if ("Tutup".equals(String.valueOf(item.getTitle()))) dialog.dismiss();
            else toast("Mode pilih bookmark aktif");
            return true;
        });
        menu.show();
    }

    private void addHeaderActions(LinearLayout top, Dialog dialog) {
        ImageButton filter = plainIconButton(R.drawable.ic_customize, this::showSortMenu);
        top.addView(filter, new LinearLayout.LayoutParams(dp(40), dp(40)));
        ImageButton addFolder = plainIconButton(
                R.drawable.ic_add_tab,
                view -> showCreateFolderDialog(
                        () -> switchDialogSmooth(dialog, this::showHome)));
        top.addView(addFolder, new LinearLayout.LayoutParams(dp(40), dp(40)));
        ImageButton close = plainIconButton(R.drawable.ic_exit, view -> dialog.dismiss());
        top.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        ImageButton more = plainIconButton(
                R.drawable.ic_menu_more,
                view -> showMainMenu(view, dialog));
        top.addView(more, new LinearLayout.LayoutParams(dp(40), dp(40)));
    }

    private void switchDialogSmooth(Dialog currentDialog, Runnable openNext) {
        try {
            if (openNext != null) openNext.run();
        } catch (Exception exception) {
            toast("Gagal membuka menu: " + exception.getMessage());
        }
        if (currentDialog != null) {
            handler.postDelayed(() -> {
                try {
                    if (currentDialog.isShowing()) currentDialog.dismiss();
                } catch (Exception ignored) {
                }
            }, 120L);
        }
    }

    private void refreshDialogSmooth(Dialog dialog, Runnable render) {
        try {
            if (render != null) render.run();
        } catch (Exception ignored) {
        }
        if (dialog != null && !dialog.isShowing()) {
            try {
                dialog.show();
            } catch (Exception ignored) {
            }
        }
    }

    private Dialog createFullscreenDialog() {
        return new Dialog(activity, android.R.style.Theme_Black_NoTitleBar);
    }

    private LinearLayout createRoot() {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(18), dp(18), dp(18), dp(10));
        return root;
    }

    private LinearLayout createTopRow() {
        LinearLayout top = new LinearLayout(activity);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        return top;
    }

    private void addTitle(LinearLayout top, String text) {
        TextView title = new TextView(activity);
        title.setText(text);
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMargins(dp(8), 0, 0, 0);
        top.addView(title, params);
    }

    private ImageButton plainIconButton(int iconRes, View.OnClickListener listener) {
        ImageButton button = new ImageButton(activity);
        button.setImageResource(iconRes);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setColorFilter(Color.parseColor("#E9EDF5"));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setOnClickListener(listener);
        return button;
    }

    private EditText darkSearchInput(String hint) {
        EditText input = new EditText(activity);
        input.setHint(hint);
        input.setHintTextColor(Color.parseColor("#A5ACB8"));
        input.setTextColor(Color.WHITE);
        input.setSingleLine(true);
        input.setBackground(YieldUi.roundRect(
                Color.parseColor("#2A2C32"), dp(16), 0, Color.TRANSPARENT));
        input.setPadding(dp(18), 0, dp(18), 0);
        return input;
    }

    private TextView circleLetter(String text, int backgroundColor, int foregroundColor) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextColor(foregroundColor);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextSize(16);
        view.setGravity(Gravity.CENTER);
        view.setBackground(YieldUi.roundRect(
                backgroundColor, dp(23), 0, Color.TRANSPARENT));
        return view;
    }

    private void save() {
        BookmarkStore.save(bookmarks, preferences);
    }

    private void toast(String message) {
        QuietToast.makeText(activity, message, QuietToast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return YieldUi.dp(activity, value);
    }

    static List<BookmarkItemData> filterFolder(List<BookmarkItemData> source,
                                               String folder,
                                               String query) {
        ArrayList<BookmarkItemData> result = new ArrayList<>();
        if (source == null || folder == null) return result;
        String cleanQuery = query == null ? "" : query.trim().toLowerCase(Locale.US);
        for (BookmarkItemData item : source) {
            if (item == null || !folder.equals(item.folder)) continue;
            String haystack = ((item.title == null ? "" : item.title)
                    + " " + (item.url == null ? "" : item.url)).toLowerCase(Locale.US);
            if (cleanQuery.length() == 0 || haystack.contains(cleanQuery)) result.add(item);
        }
        return result;
    }

    static String safeTitle(BookmarkItemData item) {
        if (item == null) return "";
        return item.title == null || item.title.length() == 0 ? item.url : item.title;
    }

    static String bookmarkInitial(BookmarkItemData item) {
        String title = safeTitle(item);
        return title == null || title.length() == 0
                ? "B"
                : title.substring(0, 1).toUpperCase(Locale.US);
    }

    static String shortHost(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return url;
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception exception) {
            return url;
        }
    }
}
