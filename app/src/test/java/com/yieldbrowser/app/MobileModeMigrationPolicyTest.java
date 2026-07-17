package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MobileModeMigrationPolicyTest {
    @Test
    public void migratesOnlyWhenMarkerHasNotBeenApplied() {
        assertTrue(MobileModeMigrationPolicy.shouldMigrate(false));
        assertFalse(MobileModeMigrationPolicy.shouldMigrate(true));
    }

    @Test
    public void preservesExistingPreferenceKeys() {
        assertEquals("desktopMode", MobileModeMigrationPolicy.DESKTOP_MODE_KEY);
        assertEquals("forceMobileModeV0939",
                MobileModeMigrationPolicy.MIGRATION_MARKER_KEY);
    }
}
