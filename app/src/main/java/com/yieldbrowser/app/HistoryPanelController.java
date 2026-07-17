package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_BG;
import static com.yieldbrowser.app.BrowserConstants.COLOR_SUBTEXT;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** Owns the full-screen History V2 dialog and its asynchronous paging session. */
final class HistoryPanelController {
    private static final int PAGE_SIZE = 50;

    interface UrlOpener {
        void open(String url);
    }

    interface FaviconBinder {
        void bind(String url, ImageView target, TextView fallback);
    }

    interface ClearHistoryAction {
        void clear(Runnable afterClear);
    }

    private final Activity activity;
    private final Handler handler;
    private final HistoryRepository repository;
    private final UrlOpener urlOpener;
    private final FaviconBinder faviconBinder;
    private final ClearHistoryAction clearHistoryAction;

    private Dialog dialog;
    private HistoryListAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private ProgressBar progress;
    private String query = "";
    private long beforeTime = Long.MAX_VALUE;
    private long beforeId = Long.MAX_VALUE;
    private boolean loading;
    private boolean endReached;
    private int generation;
    private Runnable pendingSearch;

    HistoryPanelController(Activity activity,
                           Handler handler,
                           HistoryRepository repository,
                           UrlOpener urlOpener,
                           FaviconBinder faviconBinder,
                           ClearHistoryAction clearHistoryAction) {
        this.activity = activity;
        this.handler = handler;
        this.repository = repository;
        this.urlOpener = urlOpener;
        this.faviconBinder = faviconBinder;
        this.clearHistoryAction = clearHistoryAction;
    }

    boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    void show() {
        dismiss();
        final Dialog nextDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(18), dp(18), dp(18), dp(10));

        LinearLayout top = new LinearLayout(activity);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(activity);
        title.setText("Riwayat");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        ImageButton close = plainIconButton(R.drawable.ic_exit, view -> nextDialog.dismiss());
        top.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        root.addView(top);

        EditText searchInput = darkSearchInput("Cari judul, situs, atau alamat");
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(50));
        searchParams.setMargins(0, dp(14), 0, dp(10));
        root.addView(searchInput, searchParams);

        TextView clearText = new TextView(activity);
        clearText.setText("Hapus semua riwayat");
        clearText.setTextColor(Color.parseColor("#F97352"));
        clearText.setTextSize(14);
        clearText.setTypeface(Typeface.DEFAULT_BOLD);
        clearText.setGravity(Gravity.CENTER_VERTICAL);
        clearText.setPadding(0, dp(4), 0, dp(10));
        root.addView(clearText, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout listFrame = new FrameLayout(activity);
        RecyclerView recycler = new RecyclerView(activity);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        recycler.setLayoutManager(layoutManager);
        recycler.setItemAnimator(null);
        recycler.setHasFixedSize(false);
        recycler.setClipToPadding(false);
        recycler.setPadding(0, 0, 0, dp(18));

        TextView empty = new TextView(activity);
        empty.setText("Riwayat masih kosong.");
        empty.setTextColor(COLOR_SUBTEXT);
        empty.setTextSize(16);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);

        ProgressBar loadingView = new ProgressBar(activity);
        loadingView.setIndeterminate(true);
        loadingView.setVisibility(View.GONE);

        listFrame.addView(recycler, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams emptyParams = new FrameLayout.LayoutParams(-1, -1);
        emptyParams.gravity = Gravity.CENTER;
        listFrame.addView(empty, emptyParams);
        FrameLayout.LayoutParams loadingParams = new FrameLayout.LayoutParams(dp(44), dp(44));
        loadingParams.gravity = Gravity.CENTER;
        listFrame.addView(loadingView, loadingParams);
        root.addView(listFrame, new LinearLayout.LayoutParams(-1, 0, 1));

        HistoryListAdapter nextAdapter = new HistoryListAdapter(activity,
                new HistoryListAdapter.Listener() {
                    @Override
                    public void onOpen(HistoryItemData item) {
                        if (item == null || item.url == null || item.url.trim().isEmpty()) return;
                        nextDialog.dismiss();
                        if (urlOpener != null) urlOpener.open(item.url);
                    }

                    @Override
                    public void onDelete(HistoryItemData item) {
                        if (item == null || repository == null) return;
                        if (adapter != null) adapter.removeById(item.id);
                        updateEmptyState();
                        repository.deleteById(item.id, success -> {
                            if (!success) {
                                QuietToast.makeText(activity,
                                        "Riwayat gagal dihapus. Daftar dimuat ulang.",
                                        QuietToast.LENGTH_SHORT).show();
                                refresh();
                            }
                        });
                    }

                    @Override
                    public void onBindFavicon(String url, ImageView target, TextView fallback) {
                        if (faviconBinder != null) faviconBinder.bind(url, target, fallback);
                    }
                });
        recycler.setAdapter(nextAdapter);

        dialog = nextDialog;
        adapter = nextAdapter;
        recyclerView = recycler;
        emptyView = empty;
        progress = loadingView;

        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
                super.onScrolled(view, dx, dy);
                int last = layoutManager.findLastVisibleItemPosition();
                if (HistoryPanelPresentation.shouldLoadNextPage(
                        dy, loading, endReached, last, nextAdapter.getItemCount())) {
                    loadNextPage();
                }
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
                String normalized = HistoryPanelPresentation.normalizeQuery(
                        value == null ? "" : value.toString());
                pendingSearch = () -> reset(normalized);
                handler.postDelayed(pendingSearch, HistoryPanelPresentation.SEARCH_DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });

        clearText.setOnClickListener(view -> new AlertDialog.Builder(activity)
                .setTitle("Hapus semua riwayat?")
                .setMessage("Riwayat browsing akan dihapus permanen. Tab yang sedang terbuka tidak ditutup.")
                .setPositiveButton("Hapus", (prompt, which) -> {
                    if (clearHistoryAction == null) return;
                    clearHistoryAction.clear(() -> {
                        if (adapter != null) adapter.clear();
                        endReached = true;
                        updateEmptyState();
                    });
                })
                .setNegativeButton("Batal", null)
                .show());

        nextDialog.setContentView(root);
        nextDialog.setOnKeyListener((target, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event != null
                    && event.getAction() == KeyEvent.ACTION_UP) {
                target.dismiss();
                return true;
            }
            return false;
        });
        nextDialog.setOnDismissListener(target -> clearSession(nextDialog));
        nextDialog.show();
        FullscreenDialogStyler.apply(activity, nextDialog);
        reset("");
    }

    void refresh() {
        if (!isShowing()) return;
        reset(query);
    }

    void dismiss() {
        if (dialog != null) {
            try {
                dialog.dismiss();
            } catch (Exception ignored) {
            }
        }
    }

    private void reset(String nextQuery) {
        if (!isShowing()) return;
        generation++;
        query = HistoryPanelPresentation.normalizeQuery(nextQuery);
        beforeTime = Long.MAX_VALUE;
        beforeId = Long.MAX_VALUE;
        endReached = false;
        loading = false;
        if (adapter != null) adapter.clear();
        if (recyclerView != null) recyclerView.scrollToPosition(0);
        updateEmptyState();
        loadNextPage();
    }

    private void loadNextPage() {
        if (repository == null || adapter == null || !isShowing() || loading || endReached) return;
        loading = true;
        int requestGeneration = generation;
        setLoading(true);
        repository.queryPage(query, beforeTime, beforeId, PAGE_SIZE, page -> {
            if (requestGeneration != generation || !isShowing() || adapter == null) return;
            loading = false;
            setLoading(false);
            if (page == null || page.isEmpty()) {
                endReached = true;
                updateEmptyState();
                return;
            }
            appendPage(page);
        });
    }

    private void appendPage(List<HistoryItemData> page) {
        adapter.appendPage(page);
        HistoryItemData last = page.get(page.size() - 1);
        beforeTime = last.time;
        beforeId = last.id;
        endReached = HistoryPanelPresentation.isEndReached(page.size(), PAGE_SIZE);
        updateEmptyState();
    }

    private void setLoading(boolean value) {
        if (progress == null) return;
        boolean adapterEmpty = adapter == null || adapter.isEmpty();
        progress.setVisibility(
                HistoryPanelPresentation.shouldShowInitialLoading(value, adapterEmpty)
                        ? View.VISIBLE : View.GONE);
    }

    private void updateEmptyState() {
        if (emptyView == null || adapter == null) return;
        boolean show = HistoryPanelPresentation.shouldShowEmpty(loading, adapter.isEmpty());
        emptyView.setText(HistoryPanelPresentation.emptyMessage(query));
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void clearSession(Dialog dismissedDialog) {
        generation++;
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
        if (dialog != dismissedDialog) return;
        dialog = null;
        adapter = null;
        recyclerView = null;
        emptyView = null;
        progress = null;
        loading = false;
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

    private int dp(int value) {
        return YieldUi.dp(activity, value);
    }
}
