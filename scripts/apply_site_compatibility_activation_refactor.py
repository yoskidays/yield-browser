from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "SiteCompatibilityActivationPolicy.plan(" in text:
    print("Site compatibility activation delegation already installed")
    raise SystemExit(0)

old_method = '''    private void enableSiteCompatibilityModeForUrl(String url) {
        try {
            if (!isHttpOrHttpsUrl(url)) return;
            String host = hostOfUrl(url);
            if (host == null || host.length() == 0) return;
            long until = System.currentTimeMillis() + 300000L;
            siteCompatibilityHost = host;
            siteCompatibilityUntilMs = until;
            try {
                String normalized = host.toLowerCase(Locale.US);
                if (normalized.startsWith("www.")) normalized = normalized.substring(4);
                siteCompatibilityHosts.put(normalized, until);
            } catch (Exception ignored) {
            }
            if (System.currentTimeMillis() - siteCompatibilityToastLastMs > 8000L) {
                siteCompatibilityToastLastMs = System.currentTimeMillis();
                QuietToast.makeText(this, "Mode kompatibel aktif untuk situs ini", QuietToast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
        }
    }
'''

new_method = '''    private void enableSiteCompatibilityModeForUrl(String url) {
        try {
            if (!isHttpOrHttpsUrl(url)) return;
            SiteCompatibilityActivationPolicy.Plan plan =
                    SiteCompatibilityActivationPolicy.plan(
                            true,
                            hostOfUrl(url),
                            System.currentTimeMillis(),
                            System.currentTimeMillis(),
                            siteCompatibilityToastLastMs);
            if (!plan.activate) return;
            siteCompatibilityHost = plan.host;
            siteCompatibilityUntilMs = plan.untilMs;
            try {
                siteCompatibilityHosts.put(plan.host, plan.untilMs);
            } catch (Exception ignored) {
            }
            if (plan.showToast) {
                siteCompatibilityToastLastMs = System.currentTimeMillis();
                QuietToast.makeText(this, "Mode kompatibel aktif untuk situs ini", QuietToast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
        }
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy site compatibility activation method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("SiteCompatibilityActivationPolicy.plan(") != 1:
    raise SystemExit("Expected exactly one site compatibility activation delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity site compatibility activation delegated")
