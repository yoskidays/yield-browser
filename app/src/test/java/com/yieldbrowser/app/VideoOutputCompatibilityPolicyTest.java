package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VideoOutputCompatibilityPolicyTest {
    @Test
    public void realme5ProAndroid11PrefersSurfaceThenTexture() {
        assertTrue(VideoOutputCompatibilityPolicy.isRealmeAndroid11(
                30, "realme", "realme", "RMX1971"));
        int first = VideoOutputCompatibilityPolicy.preferredOutput(
                30, "realme", "realme", "RMX1971");
        assertEquals(VideoOutputCompatibilityPolicy.OUTPUT_SURFACE, first);
        assertEquals(VideoOutputCompatibilityPolicy.OUTPUT_TEXTURE,
                VideoOutputCompatibilityPolicy.alternateOutput(first));
    }

    @Test
    public void otherDevicesKeepTextureAsDefault() {
        assertFalse(VideoOutputCompatibilityPolicy.isRealmeAndroid11(
                30, "generic", "generic", "device"));
        assertEquals(VideoOutputCompatibilityPolicy.OUTPUT_TEXTURE,
                VideoOutputCompatibilityPolicy.preferredOutput(
                        30, "generic", "generic", "device"));
    }

    @Test
    public void realmeOnOtherAndroidVersionIsNotForced() {
        assertFalse(VideoOutputCompatibilityPolicy.isRealmeAndroid11(
                29, "realme", "realme", "RMX1971"));
    }
}
