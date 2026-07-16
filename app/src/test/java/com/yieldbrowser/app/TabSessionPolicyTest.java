package com.yieldbrowser.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public final class TabSessionPolicyTest {
    @Test
    public void onlyOpenNormalTabsArePersistedAndRestored() {
        assertTrue(TabSessionPolicy.shouldPersist(false, false, false));
        assertFalse(TabSessionPolicy.shouldPersist(false, true, false));
        assertFalse(TabSessionPolicy.shouldPersist(false, false, true));
        assertFalse(TabSessionPolicy.shouldPersist(true, false, false));

        assertTrue(TabSessionPolicy.shouldRestore(false, false));
        assertFalse(TabSessionPolicy.shouldRestore(true, false));
        assertFalse(TabSessionPolicy.shouldRestore(false, true));
    }

    @Test
    public void nearestNormalTabIsSelectedWhenCurrentTabIsPrivate() {
        boolean[] persistable = {true, false, false, true};
        assertEquals(0, TabSessionPolicy.nearestPersistableIndex(persistable, 1));
        assertEquals(3, TabSessionPolicy.nearestPersistableIndex(persistable, 2));
    }

    @Test
    public void returnsMinusOneWhenOnlyPrivateTabsExist() {
        assertEquals(-1, TabSessionPolicy.nearestPersistableIndex(
                new boolean[]{false, false}, 1));
    }

    @Test
    public void mapsSerializedSelectionAfterEphemeralRowsAreRemoved() {
        assertEquals(0, TabSessionPolicy.restoredSelectionIndex(
                Arrays.asList(0, 3, 5), 2));
        assertEquals(1, TabSessionPolicy.restoredSelectionIndex(
                Arrays.asList(0, 3, 5), 3));
        assertEquals(0, TabSessionPolicy.restoredSelectionIndex(
                Arrays.asList(2, 4), 0));
        assertEquals(-1, TabSessionPolicy.restoredSelectionIndex(
                Collections.emptyList(), 1));
    }

    @Test
    public void clampsPersistedSelectionToSavedRows() {
        assertEquals(0, TabSessionPolicy.persistedSelectionIndex(-5, 3));
        assertEquals(2, TabSessionPolicy.persistedSelectionIndex(9, 3));
        assertEquals(0, TabSessionPolicy.persistedSelectionIndex(2, 0));
    }
}
