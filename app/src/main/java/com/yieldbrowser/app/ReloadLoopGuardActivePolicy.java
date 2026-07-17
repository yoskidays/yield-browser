package com.yieldbrowser.app;

/** Pure active-state check for the temporary reload-loop host guard. */
final class ReloadLoopGuardActivePolicy {
    interface HostSupplier {
        String get();
    }

    private ReloadLoopGuardActivePolicy() {
    }

    static boolean isActive(String guardHost,
                            long guardUntilMs,
                            long nowMs,
                            HostSupplier hostSupplier) {
        try {
            if (guardHost == null || guardHost.length() == 0 || nowMs > guardUntilMs) {
                return false;
            }
            if (hostSupplier == null) return false;
            String host = hostSupplier.get();
            return host != null
                    && host.length() > 0
                    && (host.equals(guardHost) || host.endsWith("." + guardHost));
        } catch (Exception ignored) {
            return false;
        }
    }
}
