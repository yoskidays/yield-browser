from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "HttpsFirstExemptionPolicy.isExempt(" in text:
    print("HTTPS-First exemption delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean isHttpsFirstExemptUrl(String url) {
        try {
            if (!isHttpUrl(url)) return true;
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            if (host == null || isLocalOrPrivateHost(host)) return true;
            int port = uri.getPort();
            return port != -1 && port != 80 && port != 443;
        } catch (Exception ignored) {
            return true;
        }
    }
'''

new_method = '''    private boolean isHttpsFirstExemptUrl(String url) {
        return HttpsFirstExemptionPolicy.isExempt(
                url,
                MainActivity.this::isHttpUrl,
                MainActivity.this::isLocalOrPrivateHost);
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy HTTPS-First exemption method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("HttpsFirstExemptionPolicy.isExempt(") != 1:
    raise SystemExit("Expected exactly one HTTPS-First exemption delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity HTTPS-First exemption delegated")
