package com.yieldbrowser.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TabSessionPolicyTest {
    @Test
    public void onlyOpenNormalTabsArePersisted() {
        assertTrue(TabSessionPolicy.shouldPersist(false, false, false));
        assertFalse(TabSessionPolicy.shouldPersist(false, true, false));
        assertFalse(TabSessionPolicy.shouldPersist(false, false, true));
        assertFalse(TabSessionPolicy.shouldPersist(true, false, false));
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
}
