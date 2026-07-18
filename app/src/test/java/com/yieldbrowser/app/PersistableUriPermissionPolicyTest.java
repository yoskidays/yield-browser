package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PersistableUriPermissionPolicyTest {
    private static final int READ = 1;
    private static final int WRITE = 2;
    private static final int UNRELATED = 8;

    @Test
    public void takeFlagsKeepsOnlyGrantedReadAndWriteAccess() {
        assertEquals(READ | WRITE,
                PersistableUriPermissionPolicy.takeFlags(
                        READ | WRITE | UNRELATED, READ, WRITE));
        assertEquals(READ,
                PersistableUriPermissionPolicy.takeFlags(
                        READ | UNRELATED, READ, WRITE));
    }

    @Test
    public void accessRequiresAtLeastOneGrantedPermission() {
        assertFalse(PersistableUriPermissionPolicy.hasAccess(0));
        assertTrue(PersistableUriPermissionPolicy.hasAccess(READ));
        assertTrue(PersistableUriPermissionPolicy.hasAccess(WRITE));
    }
}
