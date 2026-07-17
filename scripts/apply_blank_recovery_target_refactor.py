from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if text.count("BlankRecoveryTargetPolicy.resolve(") == 2:
    print("Blank recovery target delegation already installed")
    raise SystemExit(0)

old_active = '''                    String activeUrl = extractOriginalUrl(expectedView.getUrl());
                    if (activeUrl == null || activeUrl.length() == 0) {
                        activeUrl = expectedTab.currentPageUrlForRequest;
                    }
                    String activeHost = hostOfUrl(activeUrl);
                    String activeKey = navigationLoopKey(activeUrl);
                    if (!sameOrSubDomain(activeHost, expectedHost)) return;
                    if (!expectedKey.equals(activeKey)) return;
'''

new_active = '''                    String activeUrl = BlankRecoveryTargetPolicy.resolve(
                            expectedView.getUrl(),
                            expectedTab.currentPageUrlForRequest,
                            expectedHost,
                            expectedKey,
                            MainActivity.this::extractOriginalUrl,
                            MainActivity.this::hostOfUrl,
                            MainActivity.this::navigationLoopKey,
                            MainActivity.this::sameOrSubDomain);
                    if (activeUrl == null) return;
'''

old_current = '''                            String currentUrl = extractOriginalUrl(expectedView.getUrl());
                            if (currentUrl == null || currentUrl.length() == 0) {
                                currentUrl = expectedTab.currentPageUrlForRequest;
                            }
                            String currentHost = hostOfUrl(currentUrl);
                            String currentKey = navigationLoopKey(currentUrl);
                            if (!sameOrSubDomain(currentHost, expectedHost)) return;
                            if (!expectedKey.equals(currentKey)) return;
'''

new_current = '''                            String currentUrl = BlankRecoveryTargetPolicy.resolve(
                                    expectedView.getUrl(),
                                    expectedTab.currentPageUrlForRequest,
                                    expectedHost,
                                    expectedKey,
                                    MainActivity.this::extractOriginalUrl,
                                    MainActivity.this::hostOfUrl,
                                    MainActivity.this::navigationLoopKey,
                                    MainActivity.this::sameOrSubDomain);
                            if (currentUrl == null) return;
'''

if text.count(old_active) != 1:
    raise SystemExit(f"Expected one active blank recovery target block, found {text.count(old_active)}")
if text.count(old_current) != 1:
    raise SystemExit(f"Expected one current blank recovery target block, found {text.count(old_current)}")

text = text.replace(old_active, new_active, 1)
text = text.replace(old_current, new_current, 1)

if text.count("BlankRecoveryTargetPolicy.resolve(") != 2:
    raise SystemExit("Expected exactly two blank recovery target delegations")

path.write_text(text, encoding="utf-8")
print("MainActivity blank recovery targets delegated")
