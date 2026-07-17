from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "ReaderRepairTargetPolicy.resolve(" in text:
    print("Reader repair target delegation already installed")
    raise SystemExit(0)

old_block = '''                try {
                    String active = extractOriginalUrl(expectedView.getUrl());
                    if (active == null || active.length() == 0) active = expectedTab.currentPageUrlForRequest;
                    if (!ReaderCompatibilityPolicy.isEligiblePageUrl(active)) return;
                    String activeHost = hostOfUrl(active);
                    if (expectedHost.length() > 0 && activeHost.length() > 0
                            && !sameOrSubDomain(activeHost, expectedHost)
                            && !sameOrSubDomain(expectedHost, activeHost)) return;
                    repairUniversalReaderPage(active);
                } catch (Exception ignored) {
                }
'''

new_block = '''                try {
                    String active = ReaderRepairTargetPolicy.resolve(
                            expectedView.getUrl(),
                            expectedTab.currentPageUrlForRequest,
                            expectedHost,
                            MainActivity.this::extractOriginalUrl,
                            ReaderCompatibilityPolicy::isEligiblePageUrl,
                            MainActivity.this::hostOfUrl,
                            MainActivity.this::sameOrSubDomain);
                    if (active == null) return;
                    repairUniversalReaderPage(active);
                } catch (Exception ignored) {
                }
'''

if text.count(old_block) != 1:
    raise SystemExit(
        f"Expected one legacy reader repair target block, found {text.count(old_block)}")

text = text.replace(old_block, new_block, 1)

if text.count("ReaderRepairTargetPolicy.resolve(") != 1:
    raise SystemExit("Expected exactly one reader repair target delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity reader repair target delegated")
