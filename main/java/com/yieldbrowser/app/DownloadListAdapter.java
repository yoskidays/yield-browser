package com.yieldbrowser.app;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

/** Recycler-backed download list. Only changed rows are rebound. */
final class DownloadListAdapter extends ListAdapter<DownloadUiItem, DownloadListAdapter.DownloadViewHolder> {
    interface Callback {
        void onRowClick(int downloadId, View anchor);
        boolean onRowLongClick(int downloadId);
        void onPrimaryAction(int downloadId);
        void onMore(int downloadId, View anchor);
    }

    private static final int COLOR_CARD = Color.parseColor("#171A20");
    private static final int COLOR_BORDER = Color.parseColor("#2B3038");
    private static final int COLOR_TEXT = Color.parseColor("#F4F6F8");
    private static final int COLOR_SUBTEXT = Color.parseColor("#AAB0BA");
    private static final int COLOR_ACCENT = Color.parseColor("#F4C542");
    private static final int COLOR_TRACK = Color.parseColor("#343943");

    private final Callback callback;

    DownloadListAdapter(Callback callback) {
        super(new DiffUtil.ItemCallback<DownloadUiItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull DownloadUiItem oldItem, @NonNull DownloadUiItem newItem) {
                return oldItem.id == newItem.id;
            }

            @Override
            public boolean areContentsTheSame(@NonNull DownloadUiItem oldItem, @NonNull DownloadUiItem newItem) {
                return oldItem.sameContent(newItem);
            }
        });
        this.callback = callback;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).id;
    }

    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DownloadViewHolder(parent.getContext(), callback);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static final class DownloadViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoryIcon;
        private final TextView name;
        private final TextView size;
        private final ProgressBar progress;
        private final TextView activity;
        private final TextView percent;
        private final TextView detail;
        private final TextView primaryAction;
        private final TextView more;
        private final TextView selection;
        private final Callback callback;
        private DownloadUiItem boundItem;

        DownloadViewHolder(Context context, Callback callback) {
            super(buildRoot(context));
            this.callback = callback;
            LinearLayout root = (LinearLayout) itemView;

            categoryIcon = buildCategoryIcon(context);
            root.addView(categoryIcon, marginLayout(dp(context, 48), dp(context, 48), 0, 0, dp(context, 12), 0));

            LinearLayout content = new LinearLayout(context);
            content.setOrientation(LinearLayout.VERTICAL);
            root.addView(content, marginLayout(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f, 0, dp(context, 10), 0));

            name = new TextView(context);
            name.setTextColor(COLOR_TEXT);
            name.setTextSize(15);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            content.addView(name, new LinearLayout.LayoutParams(-1, -2));

            size = new TextView(context);
            size.setTextColor(COLOR_SUBTEXT);
            size.setTextSize(12);
            size.setSingleLine(true);
            size.setEllipsize(TextUtils.TruncateAt.END);
            content.addView(size, topMarginLayout(-1, -2, dp(context, 3)));

            progress = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(10_000);
            progress.setProgressTintList(ColorStateList.valueOf(COLOR_ACCENT));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(COLOR_TRACK));
            content.addView(progress, topMarginLayout(-1, dp(context, 5), dp(context, 9)));

            LinearLayout liveLine = new LinearLayout(context);
            liveLine.setOrientation(LinearLayout.HORIZONTAL);
            liveLine.setGravity(Gravity.CENTER_VERTICAL);
            content.addView(liveLine, topMarginLayout(-1, -2, dp(context, 5)));

            activity = new TextView(context);
            activity.setTextColor(Color.parseColor("#CDD2D9"));
            activity.setTextSize(12);
            activity.setSingleLine(true);
            activity.setEllipsize(TextUtils.TruncateAt.END);
            liveLine.addView(activity, new LinearLayout.LayoutParams(0, -2, 1));

            percent = new TextView(context);
            percent.setTextColor(COLOR_TEXT);
            percent.setTextSize(12);
            percent.setTypeface(Typeface.DEFAULT_BOLD);
            percent.setGravity(Gravity.END);
            liveLine.addView(percent, new LinearLayout.LayoutParams(-2, -2));

            detail = new TextView(context);
            detail.setTextColor(COLOR_SUBTEXT);
            detail.setTextSize(11);
            detail.setSingleLine(true);
            detail.setEllipsize(TextUtils.TruncateAt.END);
            content.addView(detail, topMarginLayout(-1, -2, dp(context, 3)));

            primaryAction = actionButton(context);
            root.addView(primaryAction, marginLayout(dp(context, 42), dp(context, 42), 0, 0, dp(context, 6), 0));

            more = actionButton(context);
            more.setText("⋮");
            more.setTextSize(25);
            root.addView(more, new LinearLayout.LayoutParams(dp(context, 38), dp(context, 42)));

            selection = actionButton(context);
            selection.setTextSize(18);
            selection.setTextColor(Color.parseColor("#151515"));
            selection.setVisibility(View.GONE);
            root.addView(selection, new LinearLayout.LayoutParams(dp(context, 38), dp(context, 38)));

            itemView.setOnClickListener(v -> {
                if (boundItem != null) callback.onRowClick(boundItem.id, itemView);
            });
            itemView.setOnLongClickListener(v -> boundItem != null && callback.onRowLongClick(boundItem.id));
            primaryAction.setOnClickListener(v -> {
                if (boundItem != null) callback.onPrimaryAction(boundItem.id);
            });
            more.setOnClickListener(v -> {
                if (boundItem != null) callback.onMore(boundItem.id, more);
            });
            selection.setOnClickListener(v -> {
                if (boundItem != null) callback.onRowClick(boundItem.id, selection);
            });
        }

        void bind(DownloadUiItem item) {
            boundItem = item;
            name.setText(item.fileName);
            categoryIcon.setText(categoryShortLabel(item.category));
            size.setText(item.sizeText);
            activity.setText(item.activityText);
            detail.setText(item.detailText);
            percent.setText(item.progressPercent + "%");

            boolean showLive = item.showProgress;
            progress.setVisibility(showLive ? View.VISIBLE : View.GONE);
            activity.setVisibility(item.activityText.isEmpty() ? View.GONE : View.VISIBLE);
            percent.setVisibility(showLive ? View.VISIBLE : View.GONE);
            detail.setVisibility(item.detailText.isEmpty() ? View.GONE : View.VISIBLE);

            if (showLive) animateProgress(progress, item.progressBasisPoints);
            else progress.setProgress(item.progressBasisPoints);

            primaryAction.setVisibility(item.showPrimaryAction && !item.selectionMode ? View.VISIBLE : View.GONE);
            primaryAction.setText(item.primaryAction);
            more.setVisibility(item.selectionMode ? View.GONE : View.VISIBLE);
            selection.setVisibility(item.selectionMode ? View.VISIBLE : View.GONE);
            selection.setText(item.selected ? "✓" : "");
            selection.setBackground(roundRect(item.selected ? COLOR_ACCENT : COLOR_TRACK, dp(itemView.getContext(), 18), 1, item.selected ? COLOR_ACCENT : COLOR_BORDER));

            itemView.setAlpha("failed".equals(item.status) ? 0.88f : 1f);
        }

        private static void animateProgress(ProgressBar progress, int target) {
            int current = progress.getProgress();
            if (Math.abs(target - current) > 3_000 || target < current) {
                progress.setProgress(target);
                return;
            }
            if (Build.VERSION.SDK_INT >= 24) {
                progress.setProgress(target, true);
            } else {
                ObjectAnimator animator = ObjectAnimator.ofInt(progress, "progress", current, target);
                animator.setDuration(260L);
                animator.start();
            }
        }

        private static LinearLayout buildRoot(Context context) {
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(dp(context, 12), dp(context, 12), dp(context, 8), dp(context, 12));
            root.setBackground(roundRect(COLOR_CARD, dp(context, 18), 1, COLOR_BORDER));
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
            lp.setMargins(0, 0, 0, dp(context, 10));
            root.setLayoutParams(lp);
            return root;
        }

        private static TextView buildCategoryIcon(Context context) {
            TextView icon = new TextView(context);
            icon.setGravity(Gravity.CENTER);
            icon.setTextColor(Color.parseColor("#151515"));
            icon.setTextSize(10);
            icon.setTypeface(Typeface.DEFAULT_BOLD);
            icon.setBackground(roundRect(COLOR_ACCENT, dp(context, 15), 0, COLOR_ACCENT));
            return icon;
        }

        private static TextView actionButton(Context context) {
            TextView button = new TextView(context);
            button.setGravity(Gravity.CENTER);
            button.setTextColor(COLOR_TEXT);
            button.setTextSize(17);
            button.setTypeface(Typeface.DEFAULT_BOLD);
            button.setBackground(roundRect(Color.parseColor("#22262D"), dp(context, 18), 1, COLOR_BORDER));
            button.setClickable(true);
            button.setFocusable(true);
            return button;
        }

        private static String categoryShortLabel(String category) {
            if ("Video".equals(category)) return "VID";
            if ("Musik".equals(category)) return "AUD";
            if ("Dokumen".equals(category)) return "DOC";
            if ("APK".equals(category)) return "APK";
            return "FILE";
        }

        private static LinearLayout.LayoutParams topMarginLayout(int width, int height, int top) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
            lp.setMargins(0, top, 0, 0);
            return lp;
        }

        private static LinearLayout.LayoutParams marginLayout(int width, int height, float weight, int left, int right, int top) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height, weight);
            lp.setMargins(left, top, right, 0);
            return lp;
        }

        private static GradientDrawable roundRect(int fill, int radius, int strokeWidth, int strokeColor) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(fill);
            drawable.setCornerRadius(radius);
            if (strokeWidth > 0) drawable.setStroke(strokeWidth, strokeColor);
            return drawable;
        }

        private static int dp(Context context, int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }
    }
}
