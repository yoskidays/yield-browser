package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BrowserPageCommitPolicyTest {
    @Test
    public void extractedUrlKeepsPriorityOverRawUrl() {
        assertEquals("https://original.example/page",
                BrowserPageCommitPolicy.chooseFinalUrl(
                        "https://original.example/page",
                        "https://proxy.example/page"));
        assertEquals("https://proxy.example/page",
                BrowserPageCommitPolicy.chooseFinalUrl(
                        null, "https://proxy.example/page"));
    }

    @Test
    public void viewOwnerKeepsPriorityOverCurrentTabFallback() {
        TabInfo viewOwner = new TabInfo("View", "", false);
        TabInfo currentOwner = new TabInfo("Current", "", false);

        assertSame(viewOwner,
                BrowserPageCommitPolicy.chooseOwner(viewOwner, currentOwner));
        assertSame(currentOwner,
                BrowserPageCommitPolicy.chooseOwner(null, currentOwner));
    }

    @Test
    public void normalAndCompatibilityPagesKeepDistinctEffectPlans() {
        BrowserPageCommitPolicy.EffectPlan normal =
                BrowserPageCommitPolicy.effectPlan(false, false, true);
        assertFalse(normal.compatibilityPage);
        assertTrue(normal.injectShieldFallback);
        assertTrue(normal.injectStandardCss);
        assertFalse(normal.injectCompatibilityShield);
        assertFalse(normal.scheduleCompatibilityRepair);

        BrowserPageCommitPolicy.EffectPlan compatibility =
                BrowserPageCommitPolicy.effectPlan(false, true, true);
        assertTrue(compatibility.compatibilityPage);
        assertTrue(compatibility.injectShieldFallback);
        assertFalse(compatibility.injectStandardCss);
        assertTrue(compatibility.injectCompatibilityShield);
        assertTrue(compatibility.scheduleCompatibilityRepair);
    }

    @Test
    public void compatibilityRepairRunsEvenWhenAdBlockIsDisabled() {
        BrowserPageCommitPolicy.EffectPlan plan =
                BrowserPageCommitPolicy.effectPlan(true, false, false);

        assertTrue(plan.compatibilityPage);
        assertFalse(plan.injectShieldFallback);
        assertFalse(plan.injectStandardCss);
        assertFalse(plan.injectCompatibilityShield);
        assertTrue(plan.scheduleCompatibilityRepair);
        assertTrue(BrowserPageCommitPolicy.shouldApplyUserFilters(true));
        assertFalse(BrowserPageCommitPolicy.shouldApplyUserFilters(false));
    }
}
