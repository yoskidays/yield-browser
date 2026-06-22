package com.yieldbrowser.app;

/** Pure decisions shared by the normal and dedicated incognito browser profiles. */
final class PrivateProfilePolicy {
    private PrivateProfilePolicy() {
    }

    static boolean shouldLaunchDedicatedProfile(boolean alreadyDedicated, int sdkInt) {
        return !alreadyDedicated && sdkInt >= 28;
    }

    static boolean allowThirdPartyCookies(boolean privateProfile) {
        return !privateProfile;
    }

    static boolean shouldPersistBrowserSession(boolean dedicatedPrivateProfile) {
        return !dedicatedPrivateProfile;
    }

    static boolean shouldCloseWindowAfterLastTab(boolean dedicatedPrivateProfile,
                                                  int remainingTabs) {
        return dedicatedPrivateProfile && remainingTabs <= 0;
    }
}
