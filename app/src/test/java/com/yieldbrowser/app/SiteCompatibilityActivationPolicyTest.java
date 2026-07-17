package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SiteCompatibilityActivationPolicyTest {
    @Test
    public void rejectsNonWebAndMissingHosts() {
        SiteCompatibilityActivationPolicy.Plan nonWeb =
                SiteCompatibilityActivationPolicy.plan(false, "example.com", 100L, 100L, 0L);
        SiteCompatibilityActivationPolicy.Plan missing =
                SiteCompatibilityActivationPolicy.plan(true, " ", 100L, 100L, 0L);

        assertFalse(nonWeb.activate);
        assertFalse(missing.activate);
        assertEquals("", nonWeb.host);
        assertEquals(0L, missing.untilMs);
    }

    @Test
    public void normalizesHostAndUsesFiveMinuteWindow() {
        SiteCompatibilityActivationPolicy.Plan plan =
                SiteCompatibilityActivationPolicy.plan(
                        true, " WWW.Example.COM ", 2000L, 2000L, 0L);

        assertTrue(plan.activate);
        assertEquals("example.com", plan.host);
        assertEquals(302000L, plan.untilMs);
    }

    @Test
    public void toastRequiresStrictlyMoreThanEightSeconds() {
        assertFalse(SiteCompatibilityActivationPolicy.plan(
                true, "example.com", 0L, 9000L, 1000L).showToast);
        assertTrue(SiteCompatibilityActivationPolicy.plan(
                true, "example.com", 0L, 9001L, 1000L).showToast);
    }

    @Test
    public void nullHostNormalizesToEmpty() {
        assertEquals("", SiteCompatibilityActivationPolicy.normalizeHost(null));
    }

    @Test
    public void hostWithoutWwwIsKeptLowercase() {
        assertEquals(
                "sub.example.com",
                SiteCompatibilityActivationPolicy.normalizeHost("Sub.Example.COM"));
    }
}
