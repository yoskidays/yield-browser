package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StrictCompatibilityHostPolicyTest {
    @Test
    public void matchesExactHostIgnoringCaseAndWww() {
        assertTrue(StrictCompatibilityHostPolicy.isKnownHost(
                "WWW.Example.COM", new String[]{"example.com", "other.com"}));
    }

    @Test
    public void matchesSubdomainButNotUnrelatedSuffix() {
        assertTrue(StrictCompatibilityHostPolicy.isKnownHost(
                "sub.example.com", new String[]{"example.com"}));
        assertFalse(StrictCompatibilityHostPolicy.isKnownHost(
                "notexample.com", new String[]{"example.com"}));
    }

    @Test
    public void emptyHostAndMissingListAreRejected() {
        assertFalse(StrictCompatibilityHostPolicy.isKnownHost(
                "", new String[]{"example.com"}));
        assertFalse(StrictCompatibilityHostPolicy.isKnownHost(
                "example.com", null));
    }

    @Test
    public void emptyArrayDoesNotMatch() {
        assertFalse(StrictCompatibilityHostPolicy.isKnownHost(
                "example.com", new String[0]));
    }

    @Test
    public void nullEntryDoesNotCauseFalseMatch() {
        assertFalse(StrictCompatibilityHostPolicy.isKnownHost(
                "example.com", new String[]{null, "other.com"}));
    }
}
