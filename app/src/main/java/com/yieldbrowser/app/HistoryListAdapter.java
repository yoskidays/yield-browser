package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.COLOR_SUBTEXT;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Recycler-backed, paged history list. Only visible rows are materialized. */
final class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.Holder> {
    interface Listener {
        void onOpen(HistoryItemData item);

        void onDelete(HistoryItemData item);

        void onBindFavicon(String url, ImageView target, TextView fallback);
    }

    private final Context context;
    private final Listener listener;
    private final HistoryItemPresentation presentation = HistoryItemPresentation.createDefault();
    private final ArrayList<HistoryItemData> items = new ArrayList<>();
    private final Set<Long> knownIds = new HashSet<>();

    HistoryListAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= items.size()) return RecyclerView.NO_ID;
        long id = items.get(position).id;
        return id > 0L ? id : position;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView section = new TextView(context);
        section.setTextColor(COLOR_SUBTEXT);
        section.setTextSize(14);
        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(-1, -2);
        sectionParams.setMargins(0, dp(12), 0, dp(6));
        root.addView(section, sectionParams);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));
        root.addView(row, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout iconWrap = new FrameLayout(context);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        TextView fallback = new TextView(context);
        fallback.setGravity(Gravity.CENTER);
        fallback.setTextColor(Color.parseColor("#DCE2EC"));
        fallback.setTextSize(15);
        fallback.setTypeface(Typeface.DEFAULT_BOLD);
        fallback.setBackground(roundRect(Color.parseColor("#2C3038"), dp(21)));
        iconWrap.addView(fallback, new FrameLayout.LayoutParams(-1, -1));

        ImageView favicon = new ImageView(context);
        favicon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        favicon.setPadding(dp(9), dp(9), dp(9), dp(9));
        favicon.setBackground(roundRect(Color.parseColor("#2A2D33"), dp(21)));
        favicon.setVisibility(View.GONE);
        iconWrap.addView(favicon, new FrameLayout.LayoutParams(-1, -1));
        row.addView(iconWrap, iconParams);

        LinearLayout textBox = new LinearLayout(context);
        textBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1f);
        textParams.setMargins(dp(14), 0, dp(8), 0);

        TextView title = new TextView(context);
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textBox.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(context);
        subtitle.setTextColor(COLOR_SUBTEXT);
        subtitle.setTextSize(13);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textBox.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));
        row.addView(textBox, textParams);

        TextView delete = new TextView(context);
        delete.setText("×");
        delete.setTextColor(Color.parseColor("#C9CED8"));
        delete.setTextSize(24);
        delete.setGravity(Gravity.CENTER);
        delete.setContentDescription("Hapus riwayat");
        delete.setClickable(true);
        delete.setFocusable(true);
        row.addView(delete, new LinearLayout.LayoutParams(dp(40), dp(40)));

        return new Holder(root, section, row, fallback, favicon, title, subtitle, delete);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        HistoryItemData item = items.get(position);
        HistoryItemData previous = position > 0 ? items.get(position - 1) : null;
        if (presentation.shouldShowDayHeader(item, previous)) {
            holder.section.setVisibility(View.VISIBLE);
            holder.section.setText(presentation.dayLabel(item.time, System.currentTimeMillis()));
        } else {
            holder.section.setVisibility(View.GONE);
            holder.section.setText("");
        }

        holder.title.setText(presentation.title(item));
        holder.subtitle.setText(presentation.subtitle(item));

        holder.fallback.setText(presentation.fallbackInitial(item));
        holder.fallback.setVisibility(View.VISIBLE);
        holder.favicon.setImageDrawable(null);
        holder.favicon.setVisibility(View.GONE);
        holder.favicon.setTag(item.url);
        if (listener != null) listener.onBindFavicon(item.url, holder.favicon, holder.fallback);

        holder.row.setOnClickListener(v -> {
            if (listener != null) listener.onOpen(item);
        });
        holder.delete.setOnTouchListener((v, event) -> {
            if (event != null && event.getAction() == MotionEvent.ACTION_UP) v.performClick();
            return true;
        });
        holder.delete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    void replaceAll(List<HistoryItemData> fresh) {
        items.clear();
        knownIds.clear();
        appendInternal(fresh);
        notifyDataSetChanged();
    }

    void appendPage(List<HistoryItemData> page) {
        int start = items.size();
        int added = appendInternal(page);
        if (added > 0) notifyItemRangeInserted(start, added);
    }

    private int appendInternal(List<HistoryItemData> page) {
        if (page == null || page.isEmpty()) return 0;
        int added = 0;
        for (HistoryItemData item : page) {
            if (item == null) continue;
            if (item.id > 0L && !knownIds.add(item.id)) continue;
            items.add(item);
            added++;
        }
        return added;
    }

    int removeById(long id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == id) {
                items.remove(i);
                knownIds.remove(id);
                notifyItemRemoved(i);
                if (i < items.size()) notifyItemChanged(i);
                return i;
            }
        }
        return -1;
    }

    void clear() {
        int oldSize = items.size();
        items.clear();
        knownIds.clear();
        if (oldSize > 0) notifyItemRangeRemoved(0, oldSize);
    }

    HistoryItemData lastItem() {
        return items.isEmpty() ? null : items.get(items.size() - 1);
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView section;
        final LinearLayout row;
        final TextView fallback;
        final ImageView favicon;
        final TextView title;
        final TextView subtitle;
        final TextView delete;

        Holder(@NonNull View itemView, TextView section, LinearLayout row,
               TextView fallback, ImageView favicon, TextView title,
               TextView subtitle, TextView delete) {
            super(itemView);
            this.section = section;
            this.row = row;
            this.fallback = fallback;
            this.favicon = favicon;
            this.title = title;
            this.subtitle = subtitle;
            this.delete = delete;
        }
    }
}
