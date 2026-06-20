package com.yieldbrowser.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PrivateProfilePolicyTest {
    @Test
    public void dedicatedProfileRequiresAndroidNineOrNewer() {
        assertFalse(PrivateProfilePolicy.shouldLaunchDedicatedProfile(false, 27));
        assertTrue(PrivateProfilePolicy.shouldLaunchDedicatedProfile(false, 28));
        assertFalse(PrivateProfilePolicy.shouldLaunchDedicatedProfile(true, 35));
    }

    @Test
    public void privateProfileBlocksThirdPartyCookiesAndSessionPersistence() {
        assertFalse(PrivateProfilePolicy.allowThirdPartyCookies(true));
        assertTrue(PrivateProfilePolicy.allowThirdPartyCookies(false));
        assertFalse(PrivateProfilePolicy.shouldPersistBrowserSession(true));
        assertTrue(PrivateProfilePolicy.shouldPersistBrowserSession(false));
    }

    @Test
    public void closingLastDedicatedPrivateTabClosesItsWindow() {
        assertTrue(PrivateProfilePolicy.shouldCloseWindowAfterLastTab(true, 0));
        assertFalse(PrivateProfilePolicy.shouldCloseWindowAfterLastTab(true, 1));
        assertFalse(PrivateProfilePolicy.shouldCloseWindowAfterLastTab(false, 0));
    }
}
