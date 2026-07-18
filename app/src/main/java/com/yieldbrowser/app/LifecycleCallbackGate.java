package com.yieldbrowser.app;

import java.util.concurrent.atomic.AtomicBoolean;

/** Prevents delayed bridge callbacks from touching an Activity after destruction. */
final class LifecycleCallbackGate {
    private final AtomicBoolean active = new AtomicBoolean(false);

    void markActive() {
        active.set(true);
    }

    void markDestroyed() {
        active.set(false);
    }

    boolean isActive() {
        return active.get();
    }
}
