package com.yieldbrowser.app;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class UserElementFilterStoreTest {
    @Test
    public void addRemoveAndClearManagePerHostSelectors() {
        Map<String, LinkedHashSet<String>> filters = new LinkedHashMap<>();

        assertTrue(UserElementFilterStore.add(filters, "example.test", ".advert-card"));
        assertFalse(UserElementFilterStore.add(filters, "example.test", ".advert-card"));
        assertEquals(1, filters.get("example.test").size());
        assertFalse(UserElementFilterStore.add(filters, "", ".advert-card"));
        assertFalse(UserElementFilterStore.add(filters, "example.test", "html"));

        assertTrue(UserElementFilterStore.remove(filters, "example.test", ".advert-card"));
        assertNull(filters.get("example.test"));
        assertFalse(UserElementFilterStore.remove(filters, "example.test", ".advert-card"));

        assertTrue(UserElementFilterStore.add(filters, "example.test", "#sponsor"));
        assertTrue(UserElementFilterStore.clearHost(filters, "example.test"));
        assertFalse(UserElementFilterStore.clearHost(filters, "example.test"));
    }

    @Test
    public void cssIncludesSafeSelectorsAndSkipsUnsafeStoredValues() {
        Map<String, LinkedHashSet<String>> filters = new LinkedHashMap<>();
        LinkedHashSet<String> selectors = new LinkedHashSet<>();
        selectors.add(".advert-card");
        selectors.add("html");
        selectors.add(" #sponsor ");
        filters.put("example.test", selectors);

        String css = UserElementFilterStore.buildCss(filters, "example.test");

        assertTrue(css.contains(".advert-card{display:none!important"));
        assertTrue(css.contains("#sponsor{display:none!important"));
        assertFalse(css.contains("html{"));
        assertEquals("", UserElementFilterStore.buildCss(filters, "missing.test"));
    }

    @Test
    public void pageScriptEscapesCssAndKeepsObserverRepair() {
        String script = UserElementFilterStore.buildPageScript(
                ".ad\n[data-name=\"x\"]<tag{display:none}");

        assertTrue(script.startsWith("javascript:(function(){try{"));
        assertTrue(script.contains("\\n"));
        assertTrue(script.contains("\\\"x\\\""));
        assertTrue(script.contains("\\u003Ctag"));
        assertTrue(script.contains("MutationObserver"));
        assertTrue(script.contains("setTimeout(window.__yieldEnsureUserFilters,3000)"));
    }

    @Test
    public void escapingHandlesNullAndControlCharacters() {
        assertEquals("", UserElementFilterStore.escapeForJsDoubleQuotes(null));
        assertEquals("a\\\\b\\\"c\\nd\\re\\tf\\u003Cg",
                UserElementFilterStore.escapeForJsDoubleQuotes("a\\b\"c\nd\re\tf<g"));
    }
}
