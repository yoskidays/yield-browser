from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "SiteCompatibilityActivePolicy.evaluate(" in text:
    print("Site compatibility active-state delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean isSiteCompatibilityModeActiveForUrl(String url) {
        try {
            long now = System.currentTimeMillis();
            String host = hostOfUrl(url);
            if (host == null || host.length() == 0) return false;
            String h = host.toLowerCase(Locale.US);
            if (h.startsWith("www.")) h = h.substring(4);

            // Legacy single-host guard tetap didukung.
            if (siteCompatibilityHost != null && siteCompatibilityHost.length() > 0 && now <= siteCompatibilityUntilMs) {
                String legacy = siteCompatibilityHost.toLowerCase(Locale.US);
                if (legacy.startsWith("www.")) legacy = legacy.substring(4);
                if (h.equals(legacy) || h.endsWith("." + legacy)) return true;
            }

            // v0.9.78: multi-host compatibility untuk banyak tab.
            try {
                ArrayList<String> expired = new ArrayList<>();
                for (Map.Entry<String, Long> e : siteCompatibilityHosts.entrySet()) {
                    String base = e.getKey();
                    long until = e.getValue() == null ? 0L : e.getValue();
                    if (now > until) {
                        expired.add(base);
                        continue;
                    }
                    if (base != null && base.length() > 0 && (h.equals(base) || h.endsWith("." + base))) return true;
                }
                for (String dead : expired) siteCompatibilityHosts.remove(dead);
            } catch (Exception ignored) {
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
'''

new_method = '''    private boolean isSiteCompatibilityModeActiveForUrl(String url) {
        try {
            SiteCompatibilityActivePolicy.Result result =
                    SiteCompatibilityActivePolicy.evaluate(
                            hostOfUrl(url),
                            siteCompatibilityHost,
                            siteCompatibilityUntilMs,
                            siteCompatibilityHosts,
                            System.currentTimeMillis());
            try {
                for (String dead : result.expiredHosts) siteCompatibilityHosts.remove(dead);
            } catch (Exception ignored) {
            }
            return result.active;
        } catch (Exception e) {
            return false;
        }
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy site compatibility active method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("SiteCompatibilityActivePolicy.evaluate(") != 1:
    raise SystemExit("Expected exactly one site compatibility active delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity site compatibility active-state delegated")
