package com.yieldbrowser.app;

import java.util.Locale;

/** Validation rules for selectors accepted by the manual cosmetic-filter bridge. */
final class UserElementFilterPolicy {
    private UserElementFilterPolicy() {
    }

    static boolean isProtectedTag(String tagName) {
        if (tagName == null) return false;
        String tag = tagName.trim().toLowerCase(Locale.US);
        return tag.equals("html") || tag.equals("body") || tag.equals("head")
                || tag.equals("script") || tag.equals("style") || tag.equals("link")
                || tag.equals("meta") || tag.equals("title") || tag.equals("video")
                || tag.equals("audio") || tag.equals("source");
    }

    static boolean isSafeSelector(String selector, String selectedTag) {
        if (selector == null) return false;
        String s = selector.trim();
        if (s.isEmpty() || s.length() > 600) return false;
        if (isProtectedTag(selectedTag)) return false;
        if (s.indexOf('{') >= 0 || s.indexOf('}') >= 0 || s.indexOf(';') >= 0
                || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0 || s.indexOf('\0') >= 0) {
            return false;
        }
        // The picker generates a single selector. Reject selector lists and at-rules so a page
        // cannot abuse the JavaScript bridge to inject additional CSS rules.
        if (s.indexOf(',') >= 0 || s.startsWith("@")) return false;

        String last = s;
        int child = Math.max(last.lastIndexOf('>'), Math.max(last.lastIndexOf('+'), last.lastIndexOf('~')));
        if (child >= 0 && child + 1 < last.length()) last = last.substring(child + 1).trim();
        int stop = last.length();
        for (char token : new char[]{'.', '#', ':', '['}) {
            int i = last.indexOf(token);
            if (i >= 0 && i < stop) stop = i;
        }
        String terminalTag = last.substring(0, stop).trim();
        return terminalTag.isEmpty() || !isProtectedTag(terminalTag);
    }

    static int normalizedMatchCount(int count) {
        return Math.max(0, Math.min(count, 9999));
    }
}
