package com.yieldbrowser.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure encoder and decoder for the delimiter-based browser tab session format.
 *
 * The decoder accepts legacy four-column rows and the current six-column rows. Runtime-only
 * WebView state is deliberately excluded from this format.
 */
final class TabSessionCodec {
    static final class Record {
        final String title;
        final String url;
        final boolean privateTab;
        final boolean adTab;
        final String isolationHost;
        final String tabId;
        final int sourceIndex;

        Record(String title,
               String url,
               boolean privateTab,
               boolean adTab,
               String isolationHost,
               String tabId,
               int sourceIndex) {
            this.title = safe(title);
            this.url = safe(url);
            this.privateTab = privateTab;
            this.adTab = adTab;
            this.isolationHost = safe(isolationHost);
            this.tabId = safe(tabId);
            this.sourceIndex = Math.max(0, sourceIndex);
        }

        static Record persisted(String title,
                                String url,
                                String isolationHost,
                                String tabId) {
            return new Record(title, url, false, false, isolationHost, tabId, 0);
        }
    }

    private TabSessionCodec() {
    }

    static List<Record> decode(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();

        ArrayList<Record> records = new ArrayList<>();
        int serializedIndex = 0;
        String[] rows = raw.split("\\n");
        for (String row : rows) {
            if (row == null || row.trim().isEmpty()) continue;
            int sourceIndex = serializedIndex++;
            String[] parts = row.split("\\t", -1);
            if (parts.length < 4) continue;

            records.add(new Record(
                    StorageCodec.decode(parts[0]),
                    StorageCodec.decode(parts[1]),
                    "1".equals(parts[2]),
                    "1".equals(parts[3]),
                    parts.length >= 5 ? StorageCodec.decode(parts[4]) : "",
                    parts.length >= 6 ? StorageCodec.decode(parts[5]) : "",
                    sourceIndex));
        }
        return records;
    }

    static String encode(List<Record> records) {
        if (records == null || records.isEmpty()) return "";
        StringBuilder output = new StringBuilder();
        for (Record record : records) {
            if (record == null) continue;
            if (output.length() > 0) output.append('\n');
            output.append(StorageCodec.encode(record.title)).append('\t')
                    .append(StorageCodec.encode(record.url)).append('\t')
                    .append(record.privateTab ? '1' : '0').append('\t')
                    .append(record.adTab ? '1' : '0').append('\t')
                    .append(StorageCodec.encode(record.isolationHost)).append('\t')
                    .append(StorageCodec.encode(record.tabId));
        }
        return output.toString();
    }

    static String normalizedTitle(String title) {
        return title == null || title.trim().isEmpty() ? "Tab baru" : title;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
