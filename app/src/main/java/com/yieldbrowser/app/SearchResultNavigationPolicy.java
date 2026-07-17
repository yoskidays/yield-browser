package com.yieldbrowser.app;

/** Pure allow decision for navigation originating from search-result pages. */
final class SearchResultNavigationPolicy {
    private SearchResultNavigationPolicy() {
    }

    static boolean isAllowed(boolean targetHttpOrHttps,
                             boolean currentPageIsSearchResults) {
        return targetHttpOrHttps && currentPageIsSearchResults;
    }
}
