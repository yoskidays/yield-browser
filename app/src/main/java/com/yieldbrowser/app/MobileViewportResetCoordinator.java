package com.yieldbrowser.app;

/** Schedules cancellable mobile viewport resets without owning Activity state. */
final class MobileViewportResetCoordinator {
    interface DelayScheduler {
        void schedule(Runnable action, long delayMs);
    }

    interface IntValue {
        int get();
    }

    interface BooleanValue {
        boolean get();
    }

    private MobileViewportResetCoordinator() {
    }

    static void schedule(boolean desktopMode,
                         int capturedToken,
                         DelayScheduler scheduler,
                         IntValue currentToken,
                         BooleanValue currentDesktopMode,
                         Runnable applyMobileViewport) {
        if (!MobileViewportResetPolicy.shouldSchedule(desktopMode)
                || scheduler == null || currentToken == null
                || currentDesktopMode == null || applyMobileViewport == null) {
            return;
        }

        for (long delay : MobileViewportResetPolicy.delays()) {
            scheduler.schedule(() -> {
                if (MobileViewportResetPolicy.shouldApply(
                        capturedToken,
                        currentToken.get(),
                        currentDesktopMode.get())) {
                    applyMobileViewport.run();
                }
            }, delay);
        }
    }
}
