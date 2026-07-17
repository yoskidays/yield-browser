package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PageScriptExecutionPolicyTest {
    @Test
    public void missingScriptIsNotExecuted() {
        assertFalse(PageScriptExecutionPolicy.plan(null, true).execute);
        assertFalse(PageScriptExecutionPolicy.plan("", false).execute);
    }

    @Test
    public void modernExecutionStripsJavascriptPrefix() {
        PageScriptExecutionPolicy.Plan plan =
                PageScriptExecutionPolicy.plan("javascript:alert(1)", true);
        assertTrue(plan.execute);
        assertTrue(plan.evaluateJavascript);
        assertEquals("alert(1)", plan.payload);
    }

    @Test
    public void modernExecutionKeepsPlainCode() {
        PageScriptExecutionPolicy.Plan plan =
                PageScriptExecutionPolicy.plan("alert(1)", true);
        assertEquals("alert(1)", plan.payload);
    }

    @Test
    public void legacyExecutionAddsExactlyOneJavascriptPrefix() {
        assertEquals(
                "javascript:alert(1)",
                PageScriptExecutionPolicy.plan("javascript:alert(1)", false).payload);
        assertEquals(
                "javascript:alert(1)",
                PageScriptExecutionPolicy.plan("alert(1)", false).payload);
    }

    @Test
    public void prefixMatchingRemainsCaseSensitive() {
        PageScriptExecutionPolicy.Plan plan =
                PageScriptExecutionPolicy.plan("JavaScript:alert(1)", true);
        assertEquals("JavaScript:alert(1)", plan.payload);
    }

    @Test
    public void prefixOnlyScriptPreservesOriginalBehavior() {
        assertEquals("", PageScriptExecutionPolicy.plan("javascript:", true).payload);
        assertEquals("javascript:", PageScriptExecutionPolicy.plan("javascript:", false).payload);
    }
}
