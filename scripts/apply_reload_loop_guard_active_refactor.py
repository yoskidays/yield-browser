from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "ReloadLoopGuardActivePolicy.isActive(" in text:
    print("Reload-loop guard active delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean isReloadLoopGuardActiveForUrl(String url) {
        long now = System.currentTimeMillis();
        if (reloadLoopGuardHost == null || reloadLoopGuardHost.length() == 0 || now > reloadLoopGuardUntilMs) return false;
        String host = hostOfUrl(url);
        return host.length() > 0 && (host.equals(reloadLoopGuardHost) || host.endsWith("." + reloadLoopGuardHost));
    }
'''

new_method = '''    private boolean isReloadLoopGuardActiveForUrl(String url) {
        return ReloadLoopGuardActivePolicy.isActive(
                reloadLoopGuardHost,
                reloadLoopGuardUntilMs,
                System.currentTimeMillis(),
                () -> hostOfUrl(url));
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy reload-loop active method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("ReloadLoopGuardActivePolicy.isActive(") != 1:
    raise SystemExit("Expected exactly one reload-loop active delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity reload-loop active-state delegated")
