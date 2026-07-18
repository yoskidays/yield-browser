from pathlib import Path

STATE = Path("app/src/main/java/com/yieldbrowser/app/YieldActivityState.java")
MAIN = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")

state = STATE.read_text()
main_before = MAIN.read_text()

old = """        lastTrustedDownloadGestureAtMs = 0L;
        lastTrustedDownloadSourceUrl = "";
        newTabInCurrentProfile();
        markTrustedMainFrameNavigation(clean);
        prepareTabForMainFrameNavigation(getCurrentTab(), clean);
        if (addressBar != null) addressBar.setText(clean);
        openAddressBarUrl();
        return true;"""

new = """        lastTrustedDownloadGestureAtMs = 0L;
        lastTrustedDownloadSourceUrl = "";

        // Seed the destination before the WebView starts. A blank tab has no lastSafeUrl or
        // isolationHost, so recovery guards can incorrectly restore the source download page.
        TabInfo trustedTab = createProfileTab(
                "Download", "Download privat", clean, false);
        tabs.add(trustedTab);
        switchToTab(trustedTab);
        return true;"""

if state.count(old) != 1:
    raise SystemExit(f"Unexpected trusted popup launch anchor count: {state.count(old)}")
state = state.replace(old, new, 1)

if "newTabInCurrentProfile();\n        markTrustedMainFrameNavigation(clean);" in state:
    raise SystemExit("Blank-tab trusted popup flow remains")
if "TabInfo trustedTab = createProfileTab(" not in state:
    raise SystemExit("Direct trusted tab seed missing")
if "switchToTab(trustedTab);" not in state:
    raise SystemExit("Trusted seeded tab is not activated")
if MAIN.read_text() != main_before:
    raise SystemExit("MainActivity must not change in Stage 103")
if len(main_before.splitlines()) > 3000:
    raise SystemExit(f"MainActivity target regressed: {len(main_before.splitlines())} lines")

STATE.write_text(state)
print(f"MainActivity unchanged: {len(main_before.splitlines())} lines")
