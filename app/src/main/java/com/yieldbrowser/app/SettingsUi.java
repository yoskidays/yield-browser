package com.yieldbrowser.app;

import static com.yieldbrowser.app.BrowserConstants.*;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Stateless builders for the Yield settings / menu UI (rows, cards, chips, dividers).
 *
 * <p>These used to be private instance methods on {@code MainActivity}. They are pure view
 * builders: they only need a {@link Context} plus the caller-supplied arguments (including any
 * {@link View.OnClickListener}), and never read or write {@code MainActivity} state. Moving them
 * here trims {@code MainActivity} without changing behavior: it keeps thin wrappers that delegate
 * to these methods, so every existing call site compiles and behaves exactly as before.</p>
 */
final class SettingsUi {
    private SettingsUi() {
        // Utility class.
    }

    static TextView sectionTitle(Context context, String text) {
        TextView title = new TextView(context);
        title.setText(text);
        title.setTextColor(COLOR_SUBTEXT);
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(YieldUi.dp(context, 8), YieldUi.dp(context, 14), 0, YieldUi.dp(context, 8));
        return title;
    }

    static View aboutInfoCard(Context context, String heading, String value) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(YieldUi.dp(context, 18), YieldUi.dp(context, 16), YieldUi.dp(context, 18), YieldUi.dp(context, 16));
        card.setBackground(YieldUi.roundRect(Color.parseColor("#2A2D33"), YieldUi.dp(context, 20), YieldUi.dp(context, 1), Color.parseColor("#343841")));

        TextView t1 = new TextView(context);
        t1.setText(heading);
        t1.setTextColor(Color.parseColor("#E9E9EC"));
        t1.setTextSize(18);
        t1.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(t1);

        TextView t2 = new TextView(context);
        t2.setText(value);
        t2.setTextColor(Color.parseColor("#BFC2C9"));
        t2.setTextSize(15);
        t2.setLineSpacing(0f, 1.1f);
        LinearLayout.LayoutParams t2lp = new LinearLayout.LayoutParams(-1, -2);
        t2lp.setMargins(0, YieldUi.dp(context, 6), 0, 0);
        card.addView(t2, t2lp);

        return card;
    }

    static View adBlockSwitchRow(Context context, String title, String desc, boolean enabled, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(YieldUi.dp(context, 14), YieldUi.dp(context, 12), YieldUi.dp(context, 14), YieldUi.dp(context, 12));
        row.setBackground(YieldUi.roundRect(Color.parseColor("#30333A"), YieldUi.dp(context, 18), YieldUi.dp(context, 1), Color.parseColor("#3A3D45")));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, YieldUi.dp(context, 8));
        row.setLayoutParams(rowLp);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(context);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F5F6F8"));
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        texts.addView(t);

        TextView d = new TextView(context);
        d.setText(desc);
        d.setTextColor(Color.parseColor("#AEB4BF"));
        d.setTextSize(12);
        d.setLineSpacing(0, 1.03f);
        d.setPadding(0, YieldUi.dp(context, 6), YieldUi.dp(context, 8), 0);
        texts.addView(d);

        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView status = new TextView(context);
        final boolean[] current = new boolean[]{enabled};
        status.setText(current[0] ? "ON" : "OFF");
        status.setTextColor(current[0] ? Color.parseColor("#65D889") : COLOR_SUBTEXT);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setBackground(YieldUi.roundRect(current[0] ? Color.parseColor("#173A25") : Color.parseColor("#3A3D45"), YieldUi.dp(context, 16), YieldUi.dp(context, 1), current[0] ? Color.parseColor("#20B35A") : Color.parseColor("#4A4D55")));
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(YieldUi.dp(context, 58), YieldUi.dp(context, 32));
        statusLp.setMargins(YieldUi.dp(context, 12), 0, 0, 0);
        row.addView(status, statusLp);

        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            current[0] = !current[0];
            status.setText(current[0] ? "ON" : "OFF");
            status.setTextColor(current[0] ? Color.parseColor("#65D889") : COLOR_SUBTEXT);
            status.setBackground(YieldUi.roundRect(current[0] ? Color.parseColor("#173A25") : Color.parseColor("#3A3D45"), YieldUi.dp(context, 16), YieldUi.dp(context, 1), current[0] ? Color.parseColor("#20B35A") : Color.parseColor("#4A4D55")));
        });
        return row;
    }

    static View advancedSwitchRow(Context context, String title, String desc, boolean enabled, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(YieldUi.dp(context, 14), YieldUi.dp(context, 12), YieldUi.dp(context, 14), YieldUi.dp(context, 12));
        row.setBackground(YieldUi.roundRect(Color.parseColor("#30333A"), YieldUi.dp(context, 18), YieldUi.dp(context, 1), Color.parseColor("#3A3D45")));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, YieldUi.dp(context, 8));
        row.setLayoutParams(rowLp);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(context);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F5F6F8"));
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        texts.addView(t);

        TextView d = new TextView(context);
        d.setText(desc);
        d.setTextColor(Color.parseColor("#AEB4BF"));
        d.setTextSize(12);
        d.setLineSpacing(0, 1.03f);
        d.setPadding(0, YieldUi.dp(context, 6), YieldUi.dp(context, 8), 0);
        texts.addView(d);

        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView status = new TextView(context);
        final boolean[] current = new boolean[]{enabled};
        status.setText(current[0] ? "ON" : "OFF");
        status.setTextColor(current[0] ? Color.parseColor("#65D889") : COLOR_SUBTEXT);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setBackground(YieldUi.roundRect(current[0] ? Color.parseColor("#173A25") : Color.parseColor("#3A3D45"), YieldUi.dp(context, 16), YieldUi.dp(context, 1), current[0] ? Color.parseColor("#20B35A") : Color.parseColor("#4A4D55")));
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(YieldUi.dp(context, 58), YieldUi.dp(context, 32));
        statusLp.setMargins(YieldUi.dp(context, 12), 0, 0, 0);
        row.addView(status, statusLp);

        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            current[0] = !current[0];
            status.setText(current[0] ? "ON" : "OFF");
            status.setTextColor(current[0] ? Color.parseColor("#65D889") : COLOR_SUBTEXT);
            status.setBackground(YieldUi.roundRect(current[0] ? Color.parseColor("#173A25") : Color.parseColor("#3A3D45"), YieldUi.dp(context, 16), YieldUi.dp(context, 1), current[0] ? Color.parseColor("#20B35A") : Color.parseColor("#4A4D55")));
        });
        return row;
    }

    static View advancedInfoRow(Context context, String title) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(YieldUi.dp(context, 14), YieldUi.dp(context, 12), YieldUi.dp(context, 14), YieldUi.dp(context, 12));
        row.setBackground(YieldUi.roundRect(Color.parseColor("#2C2F36"), YieldUi.dp(context, 16), YieldUi.dp(context, 1), Color.parseColor("#373B43")));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, YieldUi.dp(context, 52));
        rowLp.setMargins(0, 0, 0, YieldUi.dp(context, 8));
        row.setLayoutParams(rowLp);

        TextView check = new TextView(context);
        check.setText("✓");
        check.setTextColor(Color.parseColor("#65D889"));
        check.setTextSize(16);
        check.setTypeface(Typeface.DEFAULT_BOLD);
        check.setGravity(Gravity.CENTER);
        check.setBackground(YieldUi.roundRect(Color.parseColor("#173A25"), YieldUi.dp(context, 14), 0, Color.TRANSPARENT));
        row.addView(check, new LinearLayout.LayoutParams(YieldUi.dp(context, 30), YieldUi.dp(context, 30)));

        TextView t = new TextView(context);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F5F6F8"));
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, -2, 1);
        tLp.setMargins(YieldUi.dp(context, 12), 0, 0, 0);
        row.addView(t, tLp);
        return row;
    }

    static View videoOptSwitchRow(Context context, String title, String desc, boolean enabled, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(YieldUi.dp(context, 14), YieldUi.dp(context, 12), YieldUi.dp(context, 14), YieldUi.dp(context, 12));
        row.setBackground(YieldUi.roundRect(Color.parseColor("#30333A"), YieldUi.dp(context, 18), YieldUi.dp(context, 1), Color.parseColor("#3A3D45")));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, YieldUi.dp(context, 8));
        row.setLayoutParams(rowLp);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(context);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F5F6F8"));
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        texts.addView(t);

        TextView d = new TextView(context);
        d.setText(desc);
        d.setTextColor(Color.parseColor("#AEB4BF"));
        d.setTextSize(12);
        d.setLineSpacing(0, 1.03f);
        d.setPadding(0, YieldUi.dp(context, 6), YieldUi.dp(context, 8), 0);
        texts.addView(d);

        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView status = new TextView(context);
        final boolean[] current = new boolean[]{enabled};
        status.setText(current[0] ? "ON" : "OFF");
        status.setTextColor(current[0] ? Color.parseColor("#65D889") : COLOR_SUBTEXT);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setBackground(YieldUi.roundRect(current[0] ? Color.parseColor("#173A25") : Color.parseColor("#3A3D45"), YieldUi.dp(context, 16), YieldUi.dp(context, 1), current[0] ? Color.parseColor("#20B35A") : Color.parseColor("#4A4D55")));
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(YieldUi.dp(context, 58), YieldUi.dp(context, 32));
        statusLp.setMargins(YieldUi.dp(context, 12), 0, 0, 0);
        row.addView(status, statusLp);

        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            current[0] = !current[0];
            status.setText(current[0] ? "ON" : "OFF");
            status.setTextColor(current[0] ? Color.parseColor("#65D889") : COLOR_SUBTEXT);
            status.setBackground(YieldUi.roundRect(current[0] ? Color.parseColor("#173A25") : Color.parseColor("#3A3D45"), YieldUi.dp(context, 16), YieldUi.dp(context, 1), current[0] ? Color.parseColor("#20B35A") : Color.parseColor("#4A4D55")));
        });
        return row;
    }

    static View videoOptInfoRow(Context context, String title, String desc) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(YieldUi.dp(context, 14), YieldUi.dp(context, 12), YieldUi.dp(context, 14), YieldUi.dp(context, 12));
        row.setBackground(YieldUi.roundRect(Color.parseColor("#2C2F36"), YieldUi.dp(context, 16), YieldUi.dp(context, 1), Color.parseColor("#373B43")));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, YieldUi.dp(context, 8));
        row.setLayoutParams(rowLp);

        TextView t = new TextView(context);
        t.setText(title);
        t.setTextColor(Color.parseColor("#F5F6F8"));
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        row.addView(t);

        TextView d = new TextView(context);
        d.setText(desc);
        d.setTextColor(Color.parseColor("#AEB4BF"));
        d.setTextSize(12);
        d.setPadding(0, YieldUi.dp(context, 6), 0, 0);
        row.addView(d);
        return row;
    }

    static View menuDivider(Context context) {
        View divider = new View(context);
        divider.setBackgroundColor(Color.parseColor("#2D333D"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, YieldUi.dp(context, 1));
        params.setMargins(YieldUi.dp(context, 12), YieldUi.dp(context, 8), YieldUi.dp(context, 12), YieldUi.dp(context, 8));
        divider.setLayoutParams(params);
        return divider;
    }

    static View customizeToggleRow(Context context, int iconRes, String label, boolean enabled, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(YieldUi.dp(context, 14), YieldUi.dp(context, 12), YieldUi.dp(context, 14), YieldUi.dp(context, 12));
        row.setBackground(YieldUi.roundRect(Color.parseColor("#383A3E"), YieldUi.dp(context, 10), 0, Color.TRANSPARENT));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, YieldUi.dp(context, 70));
        rowParams.setMargins(0, 0, 0, YieldUi.dp(context, 6));
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#E7E8EA"));
        row.addView(icon, new LinearLayout.LayoutParams(YieldUi.dp(context, 24), YieldUi.dp(context, 24)));

        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(16);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMargins(YieldUi.dp(context, 14), 0, YieldUi.dp(context, 8), 0);
        row.addView(text, textParams);

        TextView status = new TextView(context);
        final boolean[] current = new boolean[]{enabled};
        status.setText(current[0] ? "ON" : "OFF");
        status.setTextColor(current[0] ? COLOR_ON : COLOR_SUBTEXT);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setBackground(YieldUi.roundRect(current[0] ? Color.parseColor("#15351F") : Color.parseColor("#343740"), YieldUi.dp(context, 12), YieldUi.dp(context, 1), current[0] ? COLOR_ON : COLOR_BORDER));
        row.addView(status, new LinearLayout.LayoutParams(YieldUi.dp(context, 46), YieldUi.dp(context, 28)));

        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            current[0] = !current[0];
            status.setText(current[0] ? "ON" : "OFF");
            status.setTextColor(current[0] ? COLOR_ON : COLOR_SUBTEXT);
            status.setBackground(YieldUi.roundRect(current[0] ? Color.parseColor("#15351F") : Color.parseColor("#343740"), YieldUi.dp(context, 12), YieldUi.dp(context, 1), current[0] ? COLOR_ON : COLOR_BORDER));
        });
        return row;
    }

    static View settingRow(Context context, int iconRes, String title, String desc, boolean enabled, View.OnClickListener listener) {
        LinearLayout row = baseSettingsRow(context, iconRes, title, desc, null);
        TextView status = new TextView(context);
        final boolean[] current = new boolean[]{enabled};
        status.setText(current[0] ? "ON" : "OFF");
        status.setTextColor(current[0] ? COLOR_ON : COLOR_SUBTEXT);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setBackground(YieldUi.roundRect(current[0] ? Color.parseColor("#15351F") : Color.parseColor("#343740"), YieldUi.dp(context, 12), YieldUi.dp(context, 1), current[0] ? COLOR_ON : COLOR_BORDER));
        row.addView(status, new LinearLayout.LayoutParams(YieldUi.dp(context, 46), YieldUi.dp(context, 28)));

        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            current[0] = !current[0];
            status.setText(current[0] ? "ON" : "OFF");
            status.setTextColor(current[0] ? COLOR_ON : COLOR_SUBTEXT);
            status.setBackground(YieldUi.roundRect(current[0] ? Color.parseColor("#15351F") : Color.parseColor("#343740"), YieldUi.dp(context, 12), YieldUi.dp(context, 1), current[0] ? COLOR_ON : COLOR_BORDER));
        });
        return row;
    }

    static View actionRow(Context context, int iconRes, String title, String desc, View.OnClickListener listener) {
        LinearLayout row = baseSettingsRow(context, iconRes, title, desc, listener);
        ImageView arrow = new ImageView(context);
        arrow.setImageResource(R.drawable.ic_forward);
        arrow.setColorFilter(COLOR_SUBTEXT);
        row.addView(arrow, new LinearLayout.LayoutParams(YieldUi.dp(context, 20), YieldUi.dp(context, 20)));
        return row;
    }

    static LinearLayout baseSettingsRow(Context context, int iconRes, String title, String desc, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(YieldUi.dp(context, 10), YieldUi.dp(context, 11), YieldUi.dp(context, 10), YieldUi.dp(context, 11));
        row.setOnClickListener(listener);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#F3F5F8"));
        row.addView(icon, new LinearLayout.LayoutParams(YieldUi.dp(context, 24), YieldUi.dp(context, 24)));

        LinearLayout textBox = new LinearLayout(context);
        textBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textBoxParams = new LinearLayout.LayoutParams(0, -2, 1);
        textBoxParams.setMargins(YieldUi.dp(context, 14), 0, YieldUi.dp(context, 8), 0);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        textBox.addView(titleView);

        TextView descView = new TextView(context);
        descView.setText(desc);
        descView.setTextColor(COLOR_SUBTEXT);
        descView.setTextSize(12);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        descParams.setMargins(0, YieldUi.dp(context, 3), 0, 0);
        textBox.addView(descView, descParams);

        row.addView(textBox, textBoxParams);
        return row;
    }

    static View menuRow(Context context, int iconRes, String label, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(YieldUi.dp(context, 14), YieldUi.dp(context, 14), YieldUi.dp(context, 14), YieldUi.dp(context, 14));
        row.setOnClickListener(listener);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.parseColor("#F3F5F8"));
        row.addView(icon, new LinearLayout.LayoutParams(YieldUi.dp(context, 22), YieldUi.dp(context, 22)));

        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(17);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(-2, -2);
        textParams.setMargins(YieldUi.dp(context, 18), 0, 0, 0);
        row.addView(text, textParams);
        return row;
    }

    static View nightChoiceRow(Context context, String label, boolean checked, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(YieldUi.dp(context, 4), YieldUi.dp(context, 12), YieldUi.dp(context, 4), YieldUi.dp(context, 12));

        TextView radio = new TextView(context);
        radio.setText(checked ? "◉" : "○");
        radio.setTextColor(checked ? COLOR_ACCENT : COLOR_SUBTEXT);
        radio.setTextSize(24);
        radio.setGravity(Gravity.CENTER);
        row.addView(radio, new LinearLayout.LayoutParams(YieldUi.dp(context, 42), YieldUi.dp(context, 42)));

        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(18);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1);
        tp.setMargins(YieldUi.dp(context, 10), 0, 0, 0);
        row.addView(text, tp);

        row.setOnClickListener(v -> {
            if (listener != null) listener.onClick(v);
            View parent = (View) row.getParent();
            if (parent instanceof LinearLayout) {
                LinearLayout list = (LinearLayout) parent;
                for (int i = 0; i < list.getChildCount(); i++) {
                    View child = list.getChildAt(i);
                    if (child instanceof LinearLayout) {
                        LinearLayout childRow = (LinearLayout) child;
                        if (childRow.getChildCount() > 0 && childRow.getChildAt(0) instanceof TextView) {
                            TextView r = (TextView) childRow.getChildAt(0);
                            if ("◉".contentEquals(r.getText()) || "○".contentEquals(r.getText())) {
                                r.setText("○");
                                r.setTextColor(COLOR_SUBTEXT);
                            }
                        }
                    }
                }
            }
            radio.setText("◉");
            radio.setTextColor(COLOR_ACCENT);
        });
        return row;
    }

    static TextView profileSpaceChip(Context context, String label, boolean selected, boolean privateSpace) {
        TextView chip = new TextView(context);
        chip.setText(label);
        chip.setTextSize(14);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setTextColor(selected
                ? (privateSpace ? Color.WHITE : Color.parseColor("#111111"))
                : Color.parseColor("#B6BBC5"));
        int fill = selected
                ? (privateSpace ? Color.parseColor("#6D28D9") : COLOR_ACCENT)
                : Color.TRANSPARENT;
        chip.setBackground(YieldUi.roundRect(fill, YieldUi.dp(context, 16), 0, Color.TRANSPARENT));
        return chip;
    }
}
