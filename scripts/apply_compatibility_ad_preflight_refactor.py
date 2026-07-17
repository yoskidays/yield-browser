from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "CompatibilityAdNavigationPreflightPolicy.initial(" in text:
    print("Compatibility ad preflight delegation already installed")
    raise SystemExit(0)

old_block = '''        try {
            if (!adBlock || !isHttpOrHttpsUrl(targetUrl)) return isExternalSchemeUrl(targetUrl);
            if (isTrustedDownloadIntentUrl(targetUrl) || isSearchEngineResultNavigation(targetUrl, currentUrl)) return false;

            String targetHost = normalizeHostForAdBlock(targetUrl);
'''

new_block = '''        try {
            CompatibilityAdNavigationPreflightPolicy.Decision preflight =
                    CompatibilityAdNavigationPreflightPolicy.initial(
                            adBlock,
                            isHttpOrHttpsUrl(targetUrl),
                            isExternalSchemeUrl(targetUrl));
            if (preflight.resolved) return preflight.block;
            if (CompatibilityAdNavigationPreflightPolicy.isExplicitlyAllowed(
                    isTrustedDownloadIntentUrl(targetUrl),
                    isSearchEngineResultNavigation(targetUrl, currentUrl))) return false;

            String targetHost = normalizeHostForAdBlock(targetUrl);
'''

if text.count(old_block) != 1:
    raise SystemExit(
        f"Expected one compatibility ad preflight block, found {text.count(old_block)}")

text = text.replace(old_block, new_block, 1)

if text.count("CompatibilityAdNavigationPreflightPolicy.initial(") != 1:
    raise SystemExit("Expected one compatibility ad initial delegation")
if text.count("CompatibilityAdNavigationPreflightPolicy.isExplicitlyAllowed(") != 1:
    raise SystemExit("Expected one compatibility ad allow delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity compatibility ad preflight delegated")
