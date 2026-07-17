from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "AdBlockHostPolicy.normalize(" in text:
    print("Ad-block host policy delegation already installed")
    raise SystemExit(0)

old_normalize = '''    private String normalizeHostForAdBlock(String url) {
        try {
            if (url == null || url.length() == 0) return "";
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null) return "";
            host = host.toLowerCase(Locale.US);
            if (host.startsWith("www.")) host = host.substring(4);
            return host;
        } catch (Exception e) {
            return "";
        }
    }
'''

new_normalize = '''    private String normalizeHostForAdBlock(String url) {
        return AdBlockHostPolicy.normalize(url, BrowserUtils::getHostLower);
    }
'''

old_relation = '''    private boolean sameOrSubDomain(String host, String baseHost) {
        if (host == null || baseHost == null || host.length() == 0 || baseHost.length() == 0) return false;
        return host.equals(baseHost) || host.endsWith("." + baseHost);
    }
'''

new_relation = '''    private boolean sameOrSubDomain(String host, String baseHost) {
        return AdBlockHostPolicy.sameOrSubDomain(host, baseHost);
    }
'''

if text.count(old_normalize) != 1:
    raise SystemExit(
        f"Expected one legacy ad-block host normalizer, found {text.count(old_normalize)}")
if text.count(old_relation) != 1:
    raise SystemExit(
        f"Expected one legacy same/subdomain method, found {text.count(old_relation)}")

text = text.replace(old_normalize, new_normalize, 1)
text = text.replace(old_relation, new_relation, 1)

if text.count("AdBlockHostPolicy.normalize(") != 1:
    raise SystemExit("Expected exactly one ad-block host normalization delegation")
if text.count("AdBlockHostPolicy.sameOrSubDomain(") != 1:
    raise SystemExit("Expected exactly one domain-relation delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity ad-block host helpers delegated")
