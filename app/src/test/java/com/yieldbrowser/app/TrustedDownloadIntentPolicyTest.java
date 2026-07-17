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
    public void encodedDownloadMarkerIsRecognized() {
        assertTrue(TrustedDownloadIntentPolicy.isTrusted(
                "https://example.com/?action=%64ownload", TrustedDownloadIntentPolicyTest::isHttp,
                url -> "example.com", host -> false,
                value -> value.contains("download"), value -> false,
                value -> false, host -> false));
    }

    @Test
    public void trustedHostAllowsSignalEvenWithAdIndicators() {
        assertTrue(TrustedDownloadIntentPolicy.isTrusted(
                "https://downloads.example/file.zip?adclick=1", TrustedDownloadIntentPolicyTest::isHttp,
                url -> "downloads.example", host -> true,
                value -> false, value -> value.contains(".zip"),
                value -> value.contains("adclick"), host -> true));
    }

    @Test
    public void cleanUntrustedHostAllowsDownloadSignal() {
        assertTrue(TrustedDownloadIntentPolicy.isTrusted(
                "https://files.example/archive.pdf", TrustedDownloadIntentPolicyTest::isHttp,
                url -> "files.example", host -> false,
                value -> false, value -> value.contains(".pdf"),
                value -> false, host -> false));
    }

    @Test
    public void suspiciousHostOrHardAdTokenBlocksUntrustedHost() {
        assertFalse(TrustedDownloadIntentPolicy.isTrusted(
                "https://ads.example/archive.pdf", TrustedDownloadIntentPolicyTest::isHttp,
                url -> "ads.example", host -> false,
                value -> false, value -> value.contains(".pdf"),
                value -> false, host -> true));
        assertFalse(TrustedDownloadIntentPolicy.isTrusted(
                "https://files.example/archive.pdf?adclick=1", TrustedDownloadIntentPolicyTest::isHttp,
                url -> "files.example", host -> false,
                value -> false, value -> value.contains(".pdf"),
                value -> value.contains("adclick"), host -> false));
    }

    @Test
    public void missingSignalEmptyHostOrDependencyFailureRejects() {
        assertFalse(TrustedDownloadIntentPolicy.isTrusted(
                "https://example.com/page", TrustedDownloadIntentPolicyTest::isHttp,
                url -> "example.com", host -> true,
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
