package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FirstPartyResourcePolicyTest {
    @Test
    public void invalidResourceOrPageUrlRejectsBeforeHostResolution() {
        AtomicInteger hostCalls = new AtomicInteger();
        FirstPartyResourcePolicy.HostResolver resolver = url -> {
            hostCalls.incrementAndGet();
            return "example.com";
        };

        assertFalse(FirstPartyResourcePolicy.isFirstParty(
                "file:///asset.js", "https://example.com", url -> url.startsWith("http"),
                resolver, String::equals));
        assertFalse(FirstPartyResourcePolicy.isFirstParty(
                "https://cdn.example.com/a.js", "about:blank", url -> url.startsWith("http"),
                resolver, String::equals));
        assertTrue(hostCalls.get() == 0);
    }

    @Test
    public void missingHostsOrDependenciesAreRejected() {
        assertFalse(FirstPartyResourcePolicy.isFirstParty(
                "https://cdn.example.com/a.js", "https://example.com", null,
                url -> "example.com", String::equals));
        assertFalse(FirstPartyResourcePolicy.isFirstParty(
                "https://cdn.example.com/a.js", "https://example.com", url -> true,
                null, String::equals));
        assertFalse(FirstPartyResourcePolicy.isFirstParty(
                "https://cdn.example.com/a.js", "https://example.com", url -> true,
                url -> "", String::equals));
        assertFalse(FirstPartyResourcePolicy.isFirstParty(
                "https://cdn.example.com/a.js", "https://example.com", url -> true,
                url -> "example.com", null));
    }

    @Test
    public void sameOrSubdomainRelationDeterminesFirstPartyResult() {
        FirstPartyResourcePolicy.HostResolver resolver = url ->
                url.contains("cdn") ? "cdn.example.com" : "example.com";
        assertTrue(FirstPartyResourcePolicy.isFirstParty(
                "https://cdn.example.com/a.js", "https://example.com", url -> true,
                resolver, (candidate, page) -> candidate.equals(page)
                        || candidate.endsWith("." + page)));
        assertFalse(FirstPartyResourcePolicy.isFirstParty(
                "https://cdn.example.com/a.js", "https://other.com", url -> true,
                url -> url.contains("cdn") ? "cdn.example.com" : "other.com",
                (candidate, page) -> candidate.equals(page)
                        || candidate.endsWith("." + page)));
    }

    @Test
    public void dependencyFailureReturnsFalse() {
        assertFalse(FirstPartyResourcePolicy.isFirstParty(
                "https://cdn.example.com/a.js", "https://example.com", url -> true,
                url -> { throw new IllegalStateException("host"); }, String::equals));
        assertFalse(FirstPartyResourcePolicy.isFirstParty(
                "https://cdn.example.com/a.js", "https://example.com", url -> true,
                url -> "example.com",
                (candidate, page) -> { throw new IllegalStateException("relation"); }));
    }
}
