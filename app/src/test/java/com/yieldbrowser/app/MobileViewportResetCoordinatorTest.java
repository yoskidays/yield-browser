package com.yieldbrowser.app;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class MobileViewportResetCoordinatorTest {
    @Test
    public void schedulesThreeActionsAndAppliesWhileTokenRemainsValid() {
        List<Long> delays = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        AtomicInteger token = new AtomicInteger(4);
        AtomicBoolean desktop = new AtomicBoolean(false);
        AtomicInteger applies = new AtomicInteger();

        MobileViewportResetCoordinator.schedule(
                false,
                4,
                (action, delay) -> {
                    actions.add(action);
                    delays.add(delay);
                },
                token::get,
                desktop::get,
                applies::incrementAndGet);

        assertEquals(java.util.Arrays.asList(120L, 500L, 1200L), delays);
        for (Runnable action : actions) action.run();
        assertEquals(3, applies.get());
    }

    @Test
    public void pendingActionsStopAfterTokenOrModeChanges() {
        List<Runnable> actions = new ArrayList<>();
        AtomicInteger token = new AtomicInteger(4);
        AtomicBoolean desktop = new AtomicBoolean(false);
        AtomicInteger applies = new AtomicInteger();

        MobileViewportResetCoordinator.schedule(
                false,
                4,
                (action, delay) -> actions.add(action),
                token::get,
                desktop::get,
                applies::incrementAndGet);

        actions.get(0).run();
        token.set(5);
        actions.get(1).run();
        token.set(4);
        desktop.set(true);
        actions.get(2).run();
        assertEquals(1, applies.get());
    }

    @Test
    public void desktopModeDoesNotScheduleAnything() {
        AtomicInteger scheduled = new AtomicInteger();
        MobileViewportResetCoordinator.schedule(
                true,
                1,
                (action, delay) -> scheduled.incrementAndGet(),
                () -> 1,
                () -> true,
                () -> { });
        assertEquals(0, scheduled.get());
    }
}
