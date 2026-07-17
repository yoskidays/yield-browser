package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SearchResultNavigationPolicyTest {
    @Test
    public void allowsWebTargetFromSearchResultsPage() {
        assertTrue(SearchResultNavigationPolicy.isAllowed(true, true));
    }

    @Test
    public void rejectsNonWebTargetEvenFromSearchResultsPage() {
        assertFalse(SearchResultNavigationPolicy.isAllowed(false, true));
    }

    @Test
    public void rejectsWebTargetOutsideSearchResultsPage() {
        assertFalse(SearchResultNavigationPolicy.isAllowed(true, false));
    }

    @Test
    public void rejectsWhenNeitherConditionIsPresent() {
        assertFalse(SearchResultNavigationPolicy.isAllowed(false, false));
    }
}
