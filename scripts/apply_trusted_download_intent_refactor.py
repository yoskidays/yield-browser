from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "TrustedDownloadIntentPolicy.isTrusted(" in text:
    print("Trusted download intent delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean isTrustedDownloadIntentUrl(String url) {
        try {
            if (url == null) return false;
            String raw = url.trim();
            if (!isHttpOrHttpsUrl(raw)) return false;

            String u = raw.toLowerCase(Locale.US);
            String decoded = u;
            try { decoded = URLDecoder.decode(u, "UTF-8").toLowerCase(Locale.US); } catch (Exception ignored) {}
            String host = normalizeHostForAdBlock(raw);
            if (host.length() == 0) return false;

            boolean trustedHost = isTrustedDownloadHostForAllow(host);
            boolean marker = hasTrustedDownloadMarker(u) || hasTrustedDownloadMarker(decoded);
            boolean file = hasDirectFileDownloadExtension(u) || hasDirectFileDownloadExtension(decoded);
            boolean hardAdToken = hasHardAdClickToken(u) || hasHardAdClickToken(decoded);
            boolean suspiciousHost = isSuspiciousAdHostForDownloadAllow(host);

            // Universal download-safe lane: tombol download asli tidak boleh ikut keblokir AdBlock.
            if (trustedHost && (marker || file)) return true;
            if ((marker || file) && !suspiciousHost && !hardAdToken) return true;
            return false;
        } catch (Exception e) {
            return false;
        }
    }
'''

new_method = '''    private boolean isTrustedDownloadIntentUrl(String url) {
        try {
            return TrustedDownloadIntentPolicy.isTrusted(
                    url,
                    MainActivity.this::isHttpOrHttpsUrl,
                    MainActivity.this::normalizeHostForAdBlock,
                    MainActivity.this::isTrustedDownloadHostForAllow,
                    MainActivity.this::hasTrustedDownloadMarker,
                    MainActivity.this::hasDirectFileDownloadExtension,
                    MainActivity.this::hasHardAdClickToken,
                    MainActivity.this::isSuspiciousAdHostForDownloadAllow);
        } catch (Exception e) {
            return false;
        }
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one trusted download intent method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("TrustedDownloadIntentPolicy.isTrusted(") != 1:
    raise SystemExit("Expected exactly one trusted download intent delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity trusted download intent delegated")
