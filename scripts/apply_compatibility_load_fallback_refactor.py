from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "CompatibilityLoadFallbackPolicy.resolve(" in text:
    print("Compatibility load fallback delegation already installed")
    raise SystemExit(0)

old_block = '''            try {
                String active = extractOriginalUrl(expectedView.getUrl());
                if (active == null || active.length() == 0) active = expectedTab.currentPageUrlForRequest;
                String activeHost = hostOfUrl(active);
                if (expectedHost.length() > 0 && activeHost.length() > 0
                        && sameOrSubDomain(activeHost, expectedHost)) {
                    cancelSmoothSearchTransition();
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (expectedView.getVisibility() != View.VISIBLE) {
                        if (homeScroll != null) homeScroll.setVisibility(View.GONE);
                        expectedView.setVisibility(View.VISIBLE);
                    }
                    expectedView.setAlpha(1f);
                    scheduleUniversalReaderCompatibilityRepair(active);
                }
            } catch (Exception ignored) {
            }
'''

new_block = '''            try {
                String active = CompatibilityLoadFallbackPolicy.resolve(
                        expectedView.getUrl(),
                        expectedTab.currentPageUrlForRequest,
                        expectedHost,
                        MainActivity.this::extractOriginalUrl,
                        MainActivity.this::hostOfUrl,
                        MainActivity.this::sameOrSubDomain);
                if (active == null) return;
                cancelSmoothSearchTransition();
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (expectedView.getVisibility() != View.VISIBLE) {
                    if (homeScroll != null) homeScroll.setVisibility(View.GONE);
                    expectedView.setVisibility(View.VISIBLE);
                }
                expectedView.setAlpha(1f);
                scheduleUniversalReaderCompatibilityRepair(active);
            } catch (Exception ignored) {
            }
'''

if text.count(old_block) != 1:
    raise SystemExit(
        f"Expected one legacy compatibility fallback block, found {text.count(old_block)}")

text = text.replace(old_block, new_block, 1)

if text.count("CompatibilityLoadFallbackPolicy.resolve(") != 1:
    raise SystemExit("Expected exactly one compatibility fallback delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity compatibility load fallback delegated")
