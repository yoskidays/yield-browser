package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompatibilityThirdPartyResourcePreflightPolicyTest {
    @Test
    public void allowsClassificationOnlyForEligibleWebResources() {
        assertTrue(CompatibilityThirdPartyResourcePreflightPolicy.shouldClassify(
                true, true, true, false, false));
    }

    @Test
    public void rejectsWhenAdBlockOrEitherWebUrlIsUnavailable() {
        assertFalse(CompatibilityThirdPartyResourcePreflightPolicy.shouldClassify(
                false, true, true, false, false));
        assertFalse(CompatibilityThirdPartyResourcePreflightPolicy.shouldClassify(
                true, false, true, false, false));
        assertFalse(CompatibilityThirdPartyResourcePreflightPolicy.shouldClassify(
                true, true, false, false, false));
    }

    @Test
    public void rejectsTrustedDownloadsAndYouTubeCoreResources() {
        assertFalse(CompatibilityThirdPartyResourcePreflightPolicy.shouldClassify(
                true, true, true, true, false));
        assertFalse(CompatibilityThirdPartyResourcePreflightPolicy.shouldClassify(
                true, true, true, false, true));
    }
}
