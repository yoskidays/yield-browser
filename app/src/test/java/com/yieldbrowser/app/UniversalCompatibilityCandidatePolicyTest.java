package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UniversalCompatibilityCandidatePolicyTest {
    @Test
    public void normalWebHostIsCandidate() {
        assertTrue(UniversalCompatibilityCandidatePolicy.isCandidate(
                "https://example.com/article", "example.com"));
    }

    @Test
    public void pdfUrlsAreRejected() {
        assertFalse(UniversalCompatibilityCandidatePolicy.isCandidate(
                "https://example.com/file.PDF?download=1", "example.com"));
        assertFalse(UniversalCompatibilityCandidatePolicy.isCandidate(
                "https://example.com/view?type=application/pdf", "example.com"));
    }

    @Test
    public void missingHostIsRejected() {
        assertFalse(UniversalCompatibilityCandidatePolicy.isCandidate(
                "https://example.com", ""));
        assertFalse(UniversalCompatibilityCandidatePolicy.isCandidate(
                "https://example.com", null));
    }

    @Test
    public void youtubeHostsAreRejected() {
        assertFalse(UniversalCompatibilityCandidatePolicy.isCandidate(
                "https://www.youtube.com/watch?v=1", "WWW.YouTube.COM"));
        assertFalse(UniversalCompatibilityCandidatePolicy.isCandidate(
                "https://m.youtube.com/watch?v=1", "m.youtube.com"));
        assertFalse(UniversalCompatibilityCandidatePolicy.isCandidate(
                "https://youtu.be/abc", "youtu.be"));
    }

    @Test
    public void searchProviderHostsAndSubdomainsAreRejected() {
        String[] hosts = {
                "google.com", "news.google.com", "google.co.id", "maps.google.co.id",
                "bing.com", "www.bing.com", "duckduckgo.com", "links.duckduckgo.com",
                "startpage.com", "www.startpage.com"
        };
        for (String host : hosts) {
            assertFalse(host, UniversalCompatibilityCandidatePolicy.isCandidate(
                    "https://" + host + "/page", host));
        }
    }

    @Test
    public void unrelatedSuffixIsNotMistakenForBlockedHost() {
        assertTrue(UniversalCompatibilityCandidatePolicy.isCandidate(
                "https://notgoogle.com/page", "notgoogle.com"));
        assertTrue(UniversalCompatibilityCandidatePolicy.isCandidate(
                "https://myyoutube.com/page", "myyoutube.com"));
    }
}
