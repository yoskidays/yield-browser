package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrustedDownloadIntentPolicyTest {
    private static boolean isHttp(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    @Test
    public void nullAndNonHttpUrlsRejectBeforeHostResolution() {
        AtomicInteger hostCalls = new AtomicInteger();
        TrustedDownloadIntentPolicy.HostResolver resolver = url -> {
            hostCalls.incrementAndGet();
            return "example.com";
        };

        assertFalse(TrustedDownloadIntentPolicy.isTrusted(
                null, TrustedDownloadIntentPolicyTest::isHttp, resolver,
                host -> false, value -> true, value -> false, value -> false, host -> false));
        assertFalse(TrustedDownloadIntentPolicy.isTrusted(
                "file:///download.zip", TrustedDownloadIntentPolicyTest::isHttp, resolver,
                host -> false, value -> true, value -> false, value -> false, host -> false));
        assertTrue(hostCalls.get() == 0);
    }

    @Test
    public void queryEmbeddedDownloadTargetDoesNotPromoteAdPage() {
        assertFalse(TrustedDownloadIntentPolicy.isTrusted(
                "https://mpofunkelas.com/register?next=https%3A%2F%2Fmediafire.com%2Ffile%2Fmovie.mp4",
                TrustedDownloadIntentPolicyTest::isHttp,
                url -> "mpofunkelas.com", host -> false,
                value -> value.contains("/file/"), value -> value.contains(".mp4"),
                value -> false, host -> false));
    }

    @Test
    public void trustedDownloadHostAllowsVerifiedDestinationWithoutMarker() {
        assertTrue(TrustedDownloadIntentPolicy.isTrusted(
                "https://files.fm/u/abc123", TrustedDownloadIntentPolicyTest::isHttp,
                url -> "files.fm", host -> true,
                value -> false, value -> false,
                value -> false, host -> false));
    }

    @Test
    public void hardAdTokenOrSuspiciousHostAlwaysRejects() {
        assertFalse(TrustedDownloadIntentPolicy.isTrusted(
                "https://mediafire.com/file/archive.zip?adclick=1",
                TrustedDownloadIntentPolicyTest::isHttp,
                url -> "mediafire.com", host -> true,
                value -> value.contains("/file/"), value -> value.contains(".zip"),
                value -> value.contains("adclick"), host -> false));
        assertFalse(TrustedDownloadIntentPolicy.isTrusted(
                "https://ads.example/archive.pdf", TrustedDownloadIntentPolicyTest::isHttp,
                url -> "ads.example", host -> false,
                value -> false, value -> value.contains(".pdf"),
                value -> false, host -> true));
    }

    @Test
    public void cleanUntrustedHostAllowsPathDownloadSignal() {
        assertTrue(TrustedDownloadIntentPolicy.isTrusted(
                "https://files.example/archive.pdf?token=abc", TrustedDownloadIntentPolicyTest::isHttp,
                url -> "files.example", host -> false,
                value -> false, value -> value.contains(".pdf"),
                value -> false, host -> false));
        assertTrue(TrustedDownloadIntentPolicy.isTrusted(
                "https://files.example/download/item", TrustedDownloadIntentPolicyTest::isHttp,
                url -> "files.example", host -> false,
                value -> value.contains("/download"), value -> false,
                value -> false, host -> false));
    }

    @Test
    public void missingSignalEmptyHostOrDependencyFailureRejects() {
        assertFalse(TrustedDownloadIntentPolicy.isTrusted(
                "https://example.com/page", TrustedDownloadIntentPolicyTest::isHttp,
                url -> "example.com", host -> false,
                value -> false, value -> false, value -> false, host -> false));
        assertFalse(TrustedDownloadIntentPolicy.isTrusted(
                "https://example.com/file.zip", TrustedDownloadIntentPolicyTest::isHttp,
                url -> "", host -> true,
                value -> false, value -> true, value -> false, host -> false));
        assertFalse(TrustedDownloadIntentPolicy.isTrusted(
                "https://example.com/file.zip", TrustedDownloadIntentPolicyTest::isHttp,
                url -> { throw new IllegalStateException("host"); }, host -> true,
                value -> false, value -> true, value -> false, host -> false));
    }
}
