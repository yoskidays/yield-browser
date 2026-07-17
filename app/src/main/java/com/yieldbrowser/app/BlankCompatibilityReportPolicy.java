package com.yieldbrowser.app;

/** Pure parser and classifier for universal blank-page compatibility reports. */
final class BlankCompatibilityReportPolicy {
    private BlankCompatibilityReportPolicy() {
    }

    static boolean isLikelyBlank(String report) {
        try {
            String raw = report == null ? "" : report.trim();
            if (raw.length() == 0) return false;
            String[] parts = raw.split("\\|", -1);
            if (parts.length < 7) return false;

            int textLen = Integer.parseInt(parts[0]);
            int nodeCount = Integer.parseInt(parts[1]);
            int mediaCount = Integer.parseInt(parts[2]);
            int linkCount = Integer.parseInt(parts[3]);
            int htmlLen = Integer.parseInt(parts[4]);
            int scrollHeight = Integer.parseInt(parts[5]);
            String readyState = parts[6];

            boolean complete = "complete".equalsIgnoreCase(readyState)
                    || "interactive".equalsIgnoreCase(readyState);
            if (!complete) return false;

            boolean stronglyBlank = textLen <= 8
                    && mediaCount == 0
                    && nodeCount <= 18
                    && htmlLen <= 1600;
            boolean sparseBlank = textLen <= 20
                    && mediaCount == 0
                    && nodeCount <= 30
                    && linkCount <= 8
                    && htmlLen <= 3000;
            boolean viewportBlank = textLen <= 8
                    && mediaCount == 0
                    && nodeCount <= 40
                    && scrollHeight >= 300;
            return stronglyBlank || sparseBlank || viewportBlank;
        } catch (Exception ignored) {
            return false;
        }
    }
}
