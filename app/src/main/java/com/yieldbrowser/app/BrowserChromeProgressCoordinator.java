package com.yieldbrowser.app;

/** Applies the isolated WebChromeClient#onProgressChanged state transition. */
final class BrowserChromeProgressCoordinator {
    interface BooleanSupplier {
        boolean get();
    }

    interface IntConsumer {
        void accept(int value);
    }

    interface BooleanConsumer {
        void accept(boolean value);
    }

    private BrowserChromeProgressCoordinator() {
    }

    static boolean handle(
            boolean activeView,
            int newProgress,
            BooleanSupplier homeVisibleSupplier,
            BooleanSupplier pageVisibleSupplier,
            IntConsumer progressUpdater,
            BooleanConsumer progressVisibilityUpdater) {
        if (!activeView) return false;

        boolean homeVisible = homeVisibleSupplier != null
                && homeVisibleSupplier.get();
        boolean pageVisible = true;
        if (!homeVisible) {
            pageVisible = pageVisibleSupplier != null
                    && pageVisibleSupplier.get();
        }

        if (BrowserChromeProgressPolicy.shouldResetProgress(
                homeVisible, pageVisible)) {
            accept(progressUpdater, 0);
            accept(progressVisibilityUpdater, false);
            return true;
        }

        accept(progressUpdater, newProgress);
        accept(progressVisibilityUpdater,
                BrowserChromeProgressPolicy.shouldShowProgress(newProgress));
        return true;
    }

    private static void accept(IntConsumer consumer, int value) {
        if (consumer != null) consumer.accept(value);
    }

    private static void accept(BooleanConsumer consumer, boolean value) {
        if (consumer != null) consumer.accept(value);
    }
}
