from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "CompatibilityRecoveryThrottlePolicy.plan(" in text:
    print("Compatibility recovery throttle delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean shouldRetryCompatibilityRecovery(String url) {
        try {
            String host = hostOfUrl(url);
            if (host == null || host.length() == 0) return false;
            String key = navigationLoopKey(url);
            long now = System.currentTimeMillis();
            if (host.equals(autoCompatibilityRecoveryHost)
                    && key.equals(autoCompatibilityRecoveryKey)
                    && now < autoCompatibilityRecoveryUntilMs) {
                return false;
            }
            autoCompatibilityRecoveryHost = host;
            autoCompatibilityRecoveryKey = key;
            autoCompatibilityRecoveryUntilMs = now + 300000L;
            return true;
        } catch (Exception e) {
            return false;
        }
    }
'''

new_method = '''    private boolean shouldRetryCompatibilityRecovery(String url) {
        try {
            String host = hostOfUrl(url);
            if (host == null || host.length() == 0) return false;
            CompatibilityRecoveryThrottlePolicy.Plan plan =
                    CompatibilityRecoveryThrottlePolicy.plan(
                            host,
                            navigationLoopKey(url),
                            autoCompatibilityRecoveryHost,
                            autoCompatibilityRecoveryKey,
                            autoCompatibilityRecoveryUntilMs,
                            System.currentTimeMillis());
            if (!plan.retry) return false;
            autoCompatibilityRecoveryHost = plan.host;
            autoCompatibilityRecoveryKey = plan.key;
            autoCompatibilityRecoveryUntilMs = plan.untilMs;
            return true;
        } catch (Exception e) {
            return false;
        }
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy compatibility recovery throttle method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("CompatibilityRecoveryThrottlePolicy.plan(") != 1:
    raise SystemExit("Expected exactly one compatibility recovery throttle delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity compatibility recovery throttle delegated")
