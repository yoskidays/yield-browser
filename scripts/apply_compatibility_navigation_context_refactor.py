from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "CompatibilityNavigationContextPolicy.any(" in text:
    print("Compatibility navigation context delegation already installed")
    raise SystemExit(0)

old_block = '''            boolean compatibility = isSiteCompatibilityModeActiveForUrl(targetUrl)
                    || isSiteCompatibilityModeActiveForUrl(sourceUrl)
                    || isStrictSiteCompatibilityUrl(targetUrl)
                    || isStrictSiteCompatibilityUrl(sourceUrl);
'''

new_block = '''            boolean compatibility = CompatibilityNavigationContextPolicy.any(
                    () -> isSiteCompatibilityModeActiveForUrl(targetUrl),
                    () -> isSiteCompatibilityModeActiveForUrl(sourceUrl),
                    () -> isStrictSiteCompatibilityUrl(targetUrl),
                    () -> isStrictSiteCompatibilityUrl(sourceUrl));
'''

if text.count(old_block) != 1:
    raise SystemExit(
        f"Expected one legacy compatibility context block, found {text.count(old_block)}")

text = text.replace(old_block, new_block, 1)

if text.count("CompatibilityNavigationContextPolicy.any(") != 1:
    raise SystemExit("Expected exactly one compatibility context delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity compatibility navigation context delegated")
