from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "HttpsFirstUpgradePolicy.upgrade(" in text:
    print("HTTPS-First upgrade delegation already installed")
    raise SystemExit(0)

old_method = '''    private String buildHttpsUpgradeUrl(String httpUrl) {
        try {
            if (!isHttpUrl(httpUrl) || isHttpsFirstExemptUrl(httpUrl)) return httpUrl;
            URI source = new URI(httpUrl.trim());
            int sourcePort = source.getPort();
            int securePort = sourcePort == 80 ? -1 : sourcePort;
            URI secure = new URI(
                    "https",
                    source.getUserInfo(),
                    source.getHost(),
                    securePort,
                    source.getPath(),
                    source.getQuery(),
                    source.getFragment());
            return secure.toASCIIString();
        } catch (Exception ignored) {
            return httpUrl;
        }
    }
'''

new_method = '''    private String buildHttpsUpgradeUrl(String httpUrl) {
        return HttpsFirstUpgradePolicy.upgrade(
                httpUrl,
                MainActivity.this::isHttpUrl,
                MainActivity.this::isHttpsFirstExemptUrl);
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy HTTPS-First upgrade method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("HttpsFirstUpgradePolicy.upgrade(") != 1:
    raise SystemExit("Expected exactly one HTTPS-First upgrade delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity HTTPS-First URL construction delegated")
