package com.yieldbrowser.app;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/** Encoding helper for delimiter-based values stored in SharedPreferences and text files. */
final class StorageCodec {
    private static final String UTF_8 = "UTF-8";

    private StorageCodec() {
        // Utility class.
    }

    static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, UTF_8);
        } catch (UnsupportedEncodingException ignored) {
            return "";
        }
    }

    static String decode(String value) {
        try {
            return URLDecoder.decode(value == null ? "" : value, UTF_8);
        } catch (UnsupportedEncodingException | IllegalArgumentException ignored) {
            return "";
        }
    }
}
