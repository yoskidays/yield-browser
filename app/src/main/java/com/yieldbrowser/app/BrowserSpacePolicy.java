package com.yieldbrowser.app;

/** Pure decisions for switching between the normal and isolated private tab spaces. */
final class BrowserSpacePolicy {
    private BrowserSpacePolicy() {
    }

    static boolean isPrivateSpace(boolean dedicatedPrivateProfile, boolean currentTabPrivate,
                                  int sdkInt) {
        return dedicatedPrivateProfile || (sdkInt < 28 && currentTabPrivate);
    }

    static boolean mustOpenOtherProcess(boolean targetPrivate, boolean dedicatedPrivateProfile,
                                        int sdkInt) {
        if (sdkInt < 28) return false;
        return targetPrivate != dedicatedPrivateProfile;
    }

    static boolean shouldReturnToNormalAfterLastPrivateTab(boolean dedicatedPrivateProfile,
                                                            int remainingTabs) {
        return dedicatedPrivateProfile && remainingTabs <= 0;
    }
}
