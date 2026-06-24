package com.yieldbrowser.app;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserElementFilterPolicyTest {
    @Test
    public void rejectsProtectedTargetsAndCssInjection() {
        assertFalse(UserElementFilterPolicy.isSafeSelector("body", "body"));
        assertFalse(UserElementFilterPolicy.isSafeSelector("body>main>video:nth-of-type(1)", "video"));
        assertFalse(UserElementFilterPolicy.isSafeSelector(".ad{display:block}", "div"));
        assertFalse(UserElementFilterPolicy.isSafeSelector(".ad,.content", "div"));
    }

    @Test
    public void acceptsNormalGeneratedSelectors() {
        assertTrue(UserElementFilterPolicy.isSafeSelector("#ad-container", "div"));
        assertTrue(UserElementFilterPolicy.isSafeSelector("body>main>div.banner:nth-of-type(2)", "div"));
        assertTrue(UserElementFilterPolicy.isSafeSelector("iframe.sponsor-slot", "iframe"));
    }
}
