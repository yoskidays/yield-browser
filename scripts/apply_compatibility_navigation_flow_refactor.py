from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "CompatibilityNavigationPolicy.isFlow(" in text:
    print("Compatibility navigation delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean isCompatibilityNavigationFlow(String targetUrl, String sourceUrl) {
        try {
            boolean compatibility = isSiteCompatibilityModeActiveForUrl(targetUrl)
                    || isSiteCompatibilityModeActiveForUrl(sourceUrl)
                    || isStrictSiteCompatibilityUrl(targetUrl)
                    || isStrictSiteCompatibilityUrl(sourceUrl);
            if (!compatibility) return false;
            String targetHost = hostOfUrl(targetUrl);
            String sourceHost = hostOfUrl(sourceUrl);
            if (targetHost.length() == 0 || sourceHost.length() == 0) return compatibility;
            return sameOrSubDomain(targetHost, sourceHost) || sameOrSubDomain(sourceHost, targetHost);
        } catch (Exception e) {
            return false;
        }
    }
'''

new_method = '''    private boolean isCompatibilityNavigationFlow(String targetUrl, String sourceUrl) {
        try {
            boolean compatibility = isSiteCompatibilityModeActiveForUrl(targetUrl)
                    || isSiteCompatibilityModeActiveForUrl(sourceUrl)
                    || isStrictSiteCompatibilityUrl(targetUrl)
                    || isStrictSiteCompatibilityUrl(sourceUrl);
            if (!compatibility) return false;
            return CompatibilityNavigationPolicy.isFlow(
                    true,
                    hostOfUrl(targetUrl),
                    hostOfUrl(sourceUrl),
                    MainActivity.this::sameOrSubDomain);
        } catch (Exception e) {
            return false;
        }
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy compatibility navigation method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("CompatibilityNavigationPolicy.isFlow(") != 1:
    raise SystemExit("Expected exactly one compatibility navigation delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity compatibility navigation flow delegated")
