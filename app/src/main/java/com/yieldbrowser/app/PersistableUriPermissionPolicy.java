package com.yieldbrowser.app;

/** Pure permission-flag policy for persisted Storage Access Framework folders. */
final class PersistableUriPermissionPolicy {
    private PersistableUriPermissionPolicy() {
    }

    static int takeFlags(int resultFlags, int readFlag, int writeFlag) {
        return resultFlags & (readFlag | writeFlag);
    }

    static boolean hasAccess(int flags) {
        return flags != 0;
    }
}
