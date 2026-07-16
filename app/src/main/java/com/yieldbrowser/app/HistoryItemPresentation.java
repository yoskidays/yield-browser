package com.yieldbrowser.app;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** Pure presentation rules for one browser-history row. */
final class HistoryItemPresentation {
    private final SimpleDateFormat dayKey;
    private final SimpleDateFormat fullDay;
    private final SimpleDateFormat clock;

    static HistoryItemPresentation createDefault() {
        return new HistoryItemPresentation(Locale.getDefault(), TimeZone.getDefault());
    }

    HistoryItemPresentation(Locale locale, TimeZone timeZone) {
        Locale safeLocale = locale == null ? Locale.getDefault() : locale;
        TimeZone safeZone = timeZone == null ? TimeZone.getDefault() : timeZone;
        dayKey = new SimpleDateFormat("yyyyMMdd", safeLocale);
        fullDay = new SimpleDateFormat("EEEE, d MMMM yyyy", safeLocale);
        clock = new SimpleDateFormat("HH:mm", safeLocale);
        dayKey.setTimeZone(safeZone);
        fullDay.setTimeZone(safeZone);
        clock.setTimeZone(safeZone);
    }

    boolean shouldShowDayHeader(HistoryItemData item, HistoryItemData previous) {
        if (item == null) return false;
        return previous == null || !dayKey.format(item.time).equals(dayKey.format(previous.time));
    }

    String dayLabel(long time, long now) {
        Calendar item = Calendar.getInstance(dayKey.getTimeZone());
        item.setTimeInMillis(time);
        Calendar today = Calendar.getInstance(dayKey.getTimeZone());
        today.setTimeInMillis(now > 0L ? now : System.currentTimeMillis());
        if (sameDay(item, today)) return "Hari ini";
        today.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(item, today)) return "Kemarin";
        return fullDay.format(time);
    }

    String title(HistoryItemData item) {
        if (item == null) return "";
        String title = safe(item.title);
        return title.isEmpty() ? safe(item.url) : title;
    }

    String host(HistoryItemData item) {
        if (item == null) return "";
        String stored = safe(item.host);
        return stored.isEmpty() ? shortHost(item.url) : stored;
    }

    String subtitle(HistoryItemData item) {
        if (item == null) return "";
        StringBuilder value = new StringBuilder(host(item))
                .append(" • ")
                .append(clock.format(item.time));
        if (item.visitCount > 1) {
            value.append(" • ").append(item.visitCount).append(" kunjungan");
        }
        return value.toString();
    }

    String fallbackInitial(HistoryItemData item) {
        String value = title(item);
        if (value.isEmpty()) value = host(item);
        if (value.isEmpty()) return "?";
        return value.substring(0, 1).toUpperCase(Locale.getDefault());
    }

    static String shortHost(String url) {
        try {
            String safeUrl = safe(url);
            if (safeUrl.isEmpty()) return "";
            URI uri = URI.create(safeUrl);
            String host = uri.getHost();
            if (host == null || host.isEmpty()) return safeUrl;
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception ignored) {
            return safe(url);
        }
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.ERA) == b.get(Calendar.ERA)
                && a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
