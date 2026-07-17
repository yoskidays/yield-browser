from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "HttpsFallbackGuardPolicy.isActive(" in text:
    print("HTTPS fallback guard delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean isHttpsFallbackGuardActive(TabInfo tab, String httpUrl) {
        if (tab == null || !isHttpUrl(httpUrl)) return false;
        if (System.currentTimeMillis() > tab.httpsFallbackAllowedUntilMs) return false;
        String host = hostOfUrl(httpUrl);
        return host.length() > 0 && host.equals(tab.httpsFallbackHost);
    }
'''

new_method = '''    private boolean isHttpsFallbackGuardActive(TabInfo tab, String httpUrl) {
        if (tab == null) return false;
        return HttpsFallbackGuardPolicy.isActive(
                httpUrl,
                tab.httpsFallbackAllowedUntilMs,
                System.currentTimeMillis(),
                tab.httpsFallbackHost,
                MainActivity.this::isHttpUrl,
                MainActivity.this::hostOfUrl);
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy HTTPS fallback guard method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("HttpsFallbackGuardPolicy.isActive(") != 1:
    raise SystemExit("Expected exactly one HTTPS fallback guard delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity HTTPS fallback guard delegated")
