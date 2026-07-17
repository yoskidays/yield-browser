package com.yieldbrowser.app;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StrictCompatibilityHostPolicyTest {
    @Test
    public void matchesExactHostIgnoringCaseAndWww() {
        assertTrue(StrictCompatibilityHostPolicy.isKnownHost(
                "WWW.Example.COM", Arrays.asList("example.com", "other.com")));
    }

    @Test
    public void matchesSubdomainButNotUnrelatedSuffix() {
        assertTrue(StrictCompatibilityHostPolicy.isKnownHost(
                "sub.example.com", Collections.singletonList("example.com")));
        assertFalse(StrictCompatibilityHostPolicy.isKnownHost(
                "notexample.com", Collections.singletonList("example.com")));
    }

    @Test
    public void emptyHostAndMissingListAreRejected() {
        assertFalse(StrictCompatibilityHostPolicy.isKnownHost(
                "", Collections.singletonList("example.com")));
        assertFalse(StrictCompatibilityHostPolicy.isKnownHost(
                "example.com", null));
    }

    @Test
    public void emptyListDoesNotMatch() {
        assertFalse(StrictCompatibilityHostPolicy.isKnownHost(
                "example.com", Collections.emptyList()));
    }

    @Test
    public void iterableFailureReturnsFalse() {
        Iterable<String> broken = () -> {
            throw new IllegalStateException("broken iterator");
        };
        assertFalse(StrictCompatibilityHostPolicy.isKnownHost("example.com", broken));
    }
}
