from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(" in text:
    print("Equivalent URL policy delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean equivalentUrlIgnoringSchemeAndFragment(String a, String b) {
        try {
            URI ua = new URI(a);
            URI ub = new URI(b);
            String ha = ua.getHost() == null ? "" : ua.getHost().toLowerCase(Locale.US);
            String hb = ub.getHost() == null ? "" : ub.getHost().toLowerCase(Locale.US);
            if (!ha.equals(hb)) return false;
            int pa = ua.getPort();
            int pb = ub.getPort();
            if (pa == 80 || pa == 443) pa = -1;
            if (pb == 80 || pb == 443) pb = -1;
            if (pa != pb) return false;
            String pathA = ua.getPath() == null || ua.getPath().length() == 0 ? "/" : ua.getPath();
            String pathB = ub.getPath() == null || ub.getPath().length() == 0 ? "/" : ub.getPath();
            String queryA = ua.getQuery() == null ? "" : ua.getQuery();
            String queryB = ub.getQuery() == null ? "" : ub.getQuery();
            return pathA.equals(pathB) && queryA.equals(queryB);
        } catch (Exception ignored) {
            return false;
        }
    }
'''

new_method = '''    private boolean equivalentUrlIgnoringSchemeAndFragment(String a, String b) {
        return EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(a, b);
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy equivalent URL method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("EquivalentUrlPolicy.equivalentIgnoringSchemeAndFragment(") != 1:
    raise SystemExit("Expected exactly one equivalent URL delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity equivalent URL comparison delegated")
