from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "TrustedMainFramePolicy.activation(" in text:
    print("Trusted main-frame delegation already installed")
    raise SystemExit(0)

old_mark = '''    private void markTrustedMainFrameNavigation(String url) {
        try {
            if (!isHttpOrHttpsUrl(url)) return;
            String host = normalizeHostForAdBlock(url);
            if (host.length() == 0) return;
            trustedMainFrameHost = host;
            trustedMainFrameUntilMs = System.currentTimeMillis() + 10000L;
        } catch (Exception ignored) {
        }
    }
'''

new_mark = '''    private void markTrustedMainFrameNavigation(String url) {
        try {
            if (!isHttpOrHttpsUrl(url)) return;
            TrustedMainFramePolicy.Activation activation =
                    TrustedMainFramePolicy.activation(
                            true,
                            normalizeHostForAdBlock(url),
                            System.currentTimeMillis());
            if (!activation.activate) return;
            trustedMainFrameHost = activation.host;
            trustedMainFrameUntilMs = activation.untilMs;
        } catch (Exception ignored) {
        }
    }
'''

old_check = '''    private boolean isTrustedMainFrameNavigation(String url) {
        try {
            if (trustedMainFrameHost == null || trustedMainFrameHost.length() == 0) return false;
            if (System.currentTimeMillis() > trustedMainFrameUntilMs) return false;
            String host = normalizeHostForAdBlock(url);
            return host.length() > 0 && sameOrSubDomain(host, trustedMainFrameHost);
        } catch (Exception e) {
            return false;
        }
    }
'''

new_check = '''    private boolean isTrustedMainFrameNavigation(String url) {
        try {
            return TrustedMainFramePolicy.isTrusted(
                    trustedMainFrameHost,
                    trustedMainFrameUntilMs,
                    System.currentTimeMillis(),
                    normalizeHostForAdBlock(url),
                    MainActivity.this::sameOrSubDomain);
        } catch (Exception e) {
            return false;
        }
    }
'''

if text.count(old_mark) != 1:
    raise SystemExit(f"Expected one trusted navigation mark method, found {text.count(old_mark)}")
if text.count(old_check) != 1:
    raise SystemExit(f"Expected one trusted navigation check method, found {text.count(old_check)}")

text = text.replace(old_mark, new_mark, 1)
text = text.replace(old_check, new_check, 1)

if text.count("TrustedMainFramePolicy.activation(") != 1:
    raise SystemExit("Expected one trusted activation delegation")
if text.count("TrustedMainFramePolicy.isTrusted(") != 1:
    raise SystemExit("Expected one trusted check delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity trusted main-frame handling delegated")
