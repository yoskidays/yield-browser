package com.yieldbrowser.app;

import android.content.SharedPreferences;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Persistence, selector collection, CSS generation, and page script for manual element filters. */
final class UserElementFilterStore {
    private static final String KEY_HOSTS = "ef_hosts";
    private static final String KEY_PREFIX = "ef_";
    private static final String HIDE_RULE =
            "{display:none!important;visibility:hidden!important;"
                    + "height:0!important;min-height:0!important;max-height:0!important;"
                    + "margin:0!important;padding:0!important;border:0!important;"
                    + "overflow:hidden!important;pointer-events:none!important;}\n";

    private UserElementFilterStore() {
    }

    static void load(SharedPreferences prefs,
                     Map<String, LinkedHashSet<String>> filters) {
        if (prefs == null || filters == null) return;
        try {
            Set<String> hosts = prefs.getStringSet(KEY_HOSTS, null);
            if (hosts == null) return;
            for (String host : hosts) {
                if (host == null || host.length() == 0) continue;
                Set<String> selectors = prefs.getStringSet(KEY_PREFIX + host, null);
                if (selectors == null || selectors.isEmpty()) continue;
                filters.put(host, new LinkedHashSet<>(selectors));
            }
        } catch (Exception ignored) {
        }
    }

    static void persistHost(SharedPreferences prefs,
                            Map<String, LinkedHashSet<String>> filters,
                            String host) {
        if (prefs == null || filters == null || host == null || host.length() == 0) return;
        try {
            SharedPreferences.Editor editor = prefs.edit();
            LinkedHashSet<String> selectors = filters.get(host);
            Set<String> savedHosts = prefs.getStringSet(KEY_HOSTS, new LinkedHashSet<>());
            LinkedHashSet<String> hosts = savedHosts == null
                    ? new LinkedHashSet<>()
                    : new LinkedHashSet<>(savedHosts);
            if (selectors == null || selectors.isEmpty()) {
                editor.remove(KEY_PREFIX + host);
                hosts.remove(host);
            } else {
                editor.putStringSet(KEY_PREFIX + host, new LinkedHashSet<>(selectors));
                hosts.add(host);
            }
            editor.putStringSet(KEY_HOSTS, hosts);
            editor.apply();
        } catch (Exception ignored) {
        }
    }

    static LinkedHashSet<String> filtersForHost(
            Map<String, LinkedHashSet<String>> filters,
            String host) {
        if (filters == null || host == null || host.length() == 0) return null;
        return filters.get(host);
    }

    static boolean add(Map<String, LinkedHashSet<String>> filters,
                       String host,
                       String selector) {
        if (filters == null || host == null || host.length() == 0 || selector == null) return false;
        String clean = selector.trim();
        if (!UserElementFilterPolicy.isSafeSelector(clean, "")) return false;
        LinkedHashSet<String> selectors = filters.get(host);
        if (selectors == null) {
            selectors = new LinkedHashSet<>();
            filters.put(host, selectors);
        }
        return selectors.add(clean);
    }

    static boolean remove(Map<String, LinkedHashSet<String>> filters,
                          String host,
                          String selector) {
        if (filters == null || host == null || selector == null) return false;
        LinkedHashSet<String> selectors = filters.get(host);
        if (selectors == null || !selectors.remove(selector)) return false;
        if (selectors.isEmpty()) filters.remove(host);
        return true;
    }

    static boolean clearHost(Map<String, LinkedHashSet<String>> filters, String host) {
        return filters != null && host != null && host.length() > 0 && filters.remove(host) != null;
    }

    static String buildCss(Map<String, LinkedHashSet<String>> filters, String host) {
        LinkedHashSet<String> selectors = filtersForHost(filters, host);
        if (selectors == null || selectors.isEmpty()) return "";
        StringBuilder css = new StringBuilder();
        for (String selector : selectors) {
            if (!UserElementFilterPolicy.isSafeSelector(selector, "")) continue;
            css.append(selector.trim()).append(HIDE_RULE);
        }
        return css.toString();
    }

    static String buildPageScript(String css) {
        String safeCss = escapeForJsDoubleQuotes(css == null ? "" : css);
        return "javascript:(function(){try{"
                + "var css=\"" + safeCss + "\";"
                + "var id='yield-user-filters';"
                + "window.__yieldUserFiltersCss=css;"
                + "window.__yieldEnsureUserFilters=function(){try{"
                + "var root=document.head||document.documentElement||document.body;if(!root)return;"
                + "var el=document.getElementById(id);"
                + "if(!window.__yieldUserFiltersCss){if(el)el.textContent='';return;}"
                + "if(!el){el=document.createElement('style');el.id=id;el.setAttribute('type','text/css');root.appendChild(el);}"
                + "if(el.textContent!==window.__yieldUserFiltersCss)el.textContent=window.__yieldUserFiltersCss;"
                + "}catch(e){}};"
                + "window.__yieldEnsureUserFilters();"
                + "if(!css){"
                + "if(window.__yieldUserFiltersObserver){try{window.__yieldUserFiltersObserver.disconnect();}catch(e){}window.__yieldUserFiltersObserver=null;}"
                + "return;"
                + "}"
                + "if(!window.__yieldUserFiltersObserver&&window.MutationObserver&&document.documentElement){"
                + "window.__yieldUserFiltersObserver=new MutationObserver(function(){window.__yieldEnsureUserFilters();});"
                + "window.__yieldUserFiltersObserver.observe(document.documentElement,{childList:true,subtree:true});"
                + "}"
                + "setTimeout(window.__yieldEnsureUserFilters,250);"
                + "setTimeout(window.__yieldEnsureUserFilters,1000);"
                + "setTimeout(window.__yieldEnsureUserFilters,3000);"
                + "}catch(e){}})();";
    }

    static String escapeForJsDoubleQuotes(String value) {
        if (value == null) return "";
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\': escaped.append("\\\\"); break;
                case '"': escaped.append("\\\""); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                case '<': escaped.append("\\u003C"); break;
                default: escaped.append(character);
            }
        }
        return escaped.toString();
    }
}
