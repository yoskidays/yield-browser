package com.yieldbrowser.app;

import java.util.Locale;

/** Selects a conservative video output path for vendor-specific Android media stacks. */
final class VideoOutputCompatibilityPolicy {
    static final int OUTPUT_TEXTURE = 0;
    static final int OUTPUT_SURFACE = 1;

    private VideoOutputCompatibilityPolicy() {}

    static int preferredOutput(int sdk, String brand, String manufacturer, String model) {
        return isRealmeAndroid11(sdk, brand, manufacturer, model)
                ? OUTPUT_SURFACE : OUTPUT_TEXTURE;
    }

    static int alternateOutput(int current) {
        return current == OUTPUT_SURFACE ? OUTPUT_TEXTURE : OUTPUT_SURFACE;
    }

    static boolean isRealmeAndroid11(int sdk, String brand, String manufacturer, String model) {
        if (sdk != 30) return false;
        String identity = (safe(brand) + " " + safe(manufacturer) + " " + safe(model))
                .toLowerCase(Locale.US);
        return identity.contains("realme") || identity.contains("rmx1971");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
