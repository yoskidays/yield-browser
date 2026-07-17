package com.yieldbrowser.app;

/** Selects the first safe mode-reload URL while preserving candidate priority. */
final class BrowserModeReloadSelector {
    interface UrlMapper {
        String map(String url);
    }

    interface SafetyPredicate {
        boolean isSafe(String url, boolean explicitCurrentPage);
    }

    private BrowserModeReloadSelector() {
    }

    static String select(String[] candidates,
                         UrlMapper originalUrlMapper,
                         SafetyPredicate safetyPredicate) {
        if (candidates == null || safetyPredicate == null) return null;
        for (int index = 0; index < candidates.length; index++) {
            String candidate = candidates[index];
            String clean = originalUrlMapper == null
                    ? candidate : originalUrlMapper.map(candidate);
            if (clean == null || clean.trim().length() == 0) clean = candidate;
            boolean explicitCurrentPage = index < 2;
            if (safetyPredicate.isSafe(clean, explicitCurrentPage)) return clean;
        }
        return null;
    }
}
