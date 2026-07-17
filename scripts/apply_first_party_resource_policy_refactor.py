from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "FirstPartyResourcePolicy.isFirstParty(" in text:
    print("First-party resource delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean isFirstPartyResourceForCurrentPage(String resourceUrl, String pageUrl) {
        try {
            if (!isHttpOrHttpsUrl(resourceUrl) || !isHttpOrHttpsUrl(pageUrl)) return false;
            String resourceHost = normalizeHostForAdBlock(resourceUrl);
            String pageHost = normalizeHostForAdBlock(pageUrl);
            return resourceHost.length() > 0 && pageHost.length() > 0 && sameOrSubDomain(resourceHost, pageHost);
        } catch (Exception e) {
            return false;
        }
    }
'''

new_method = '''    private boolean isFirstPartyResourceForCurrentPage(String resourceUrl, String pageUrl) {
        try {
            return FirstPartyResourcePolicy.isFirstParty(
                    resourceUrl,
                    pageUrl,
                    MainActivity.this::isHttpOrHttpsUrl,
                    MainActivity.this::normalizeHostForAdBlock,
                    MainActivity.this::sameOrSubDomain);
        } catch (Exception e) {
            return false;
        }
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one first-party resource method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("FirstPartyResourcePolicy.isFirstParty(") != 1:
    raise SystemExit("Expected exactly one first-party resource delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity first-party resource classification delegated")
