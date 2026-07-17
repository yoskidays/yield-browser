from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "SearchResultNavigationPolicy.isAllowed(" in text:
    print("Search-result navigation delegation already installed")
    raise SystemExit(0)

old_method = '''    private boolean isSearchEngineResultNavigation(String targetUrl, String currentUrl) {
        if (!isHttpOrHttpsUrl(targetUrl)) return false;
        // v0.10.07: universal search-result allow lane. Do not depend on a fixed list of
        // .com domains: regional Google/Yahoo/Yandex domains and self-hosted SearX/SearXNG
        // pages are recognized from their search URL shape by ShieldEngineV2.
        return ShieldEngineV2.isSearchResultsPage(currentUrl);
    }
'''

new_method = '''    private boolean isSearchEngineResultNavigation(String targetUrl, String currentUrl) {
        // v0.10.07: universal search-result allow lane. Do not depend on a fixed list of
        // .com domains: regional Google/Yahoo/Yandex domains and self-hosted SearX/SearXNG
        // pages are recognized from their search URL shape by ShieldEngineV2.
        return SearchResultNavigationPolicy.isAllowed(
                isHttpOrHttpsUrl(targetUrl),
                ShieldEngineV2.isSearchResultsPage(currentUrl));
    }
'''

if text.count(old_method) != 1:
    raise SystemExit(
        f"Expected one legacy search-result navigation method, found {text.count(old_method)}")

text = text.replace(old_method, new_method, 1)

if text.count("SearchResultNavigationPolicy.isAllowed(") != 1:
    raise SystemExit("Expected exactly one search-result navigation delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity search-result navigation delegated")
