package com.yieldbrowser.app;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HttpsOverrideStartPolicyTest {
    private static boolean isHttp(String url) {
        return url != null && url.startsWith("http://");
    }

    @Test
    public void missingViewAndNonHttpTargetsShortCircuitPreparation() {
        AtomicInteger prepareCalls = new AtomicInteger();
        HttpsOverrideStartPolicy.UrlTransformer prepare = url -> {
            prepareCalls.incrementAndGet();
            return "https://example.com";
        };
        assertNull(HttpsOverrideStartPolicy.resolve(
                false, "http://example.com", HttpsOverrideStartPolicyTest::isHttp, prepare));
        assertNull(HttpsOverrideStartPolicy.resolve(
                true, "https://example.com", HttpsOverrideStartPolicyTest::isHttp, prepare));
        assertEquals(0, prepareCalls.get());
    }

    @Test
    public void changedPreparedUrlIsReturned() {
        assertEquals("https://example.com", HttpsOverrideStartPolicy.resolve(
                true, "http://example.com", HttpsOverrideStartPolicyTest::isHttp,
                url -> "https://example.com"));
    }

    @Test
    public void nullAndUnchangedPreparedUrlsDoNotStartOverride() {
        assertNull(HttpsOverrideStartPolicy.resolve(
                true, "http://example.com", HttpsOverrideStartPolicyTest::isHttp,
                url -> null));
        assertNull(HttpsOverrideStartPolicy.resolve(
                true, "http://example.com", HttpsOverrideStartPolicyTest::isHttp,
                url -> url));
    }

    @Test
    public void missingAndFailingDependenciesReturnNull() {
        assertNull(HttpsOverrideStartPolicy.resolve(
                true, "http://example.com", null, url -> "https://example.com"));
        assertNull(HttpsOverrideStartPolicy.resolve(
                true, "http://example.com", HttpsOverrideStartPolicyTest::isHttp, null));
        assertNull(HttpsOverrideStartPolicy.resolve(
                true, "http://example.com",
                url -> { throw new IllegalStateException("http"); },
                url -> "https://example.com"));
        assertNull(HttpsOverrideStartPolicy.resolve(
                true, "http://example.com", HttpsOverrideStartPolicyTest::isHttp,
                url -> { throw new IllegalStateException("prepare"); }));
    }
}
