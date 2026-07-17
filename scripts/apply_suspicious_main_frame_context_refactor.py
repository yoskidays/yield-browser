from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "SuspiciousMainFrameContextPolicy.beforeCompatibility(" in text:
    print("Suspicious main-frame context delegation already installed")
    raise SystemExit(0)

old_block = '''            if (!suspicious) return false;
            if (sameSite) return true;
            if (fromSearch) return true;

            // v0.9.97: on compatibility/reader pages, a touch event can be stolen by an
            // advertising click-hijacker. Do not treat that inherited user gesture as consent
            // to leave the reader for a suspicious cross-site destination.
            boolean compatibilitySource = isStrictSiteCompatibilityUrl(currentUrl)
                    || isSiteCompatibilityModeActiveForUrl(currentUrl)
                    || isReloadLoopGuardActiveForUrl(currentUrl)
                    || ShieldEngineV2.isPopupIsolationContentPage(currentUrl);
            if (compatibilitySource) return false;

            return hasGesture && currentHost.length() > 0;
'''

new_block = '''            SuspiciousMainFrameContextPolicy.Decision contextDecision =
                    SuspiciousMainFrameContextPolicy.beforeCompatibility(
                            suspicious, sameSite, fromSearch);
            if (contextDecision.resolved) return contextDecision.allow;

            // v0.9.97: on compatibility/reader pages, a touch event can be stolen by an
            // advertising click-hijacker. Do not treat that inherited user gesture as consent
            // to leave the reader for a suspicious cross-site destination.
            boolean compatibilitySource = isStrictSiteCompatibilityUrl(currentUrl)
                    || isSiteCompatibilityModeActiveForUrl(currentUrl)
                    || isReloadLoopGuardActiveForUrl(currentUrl)
                    || ShieldEngineV2.isPopupIsolationContentPage(currentUrl);
            return SuspiciousMainFrameContextPolicy.allowCrossSite(
                    compatibilitySource,
                    hasGesture,
                    currentHost.length() > 0);
'''

if text.count(old_block) != 1:
    raise SystemExit(
        f"Expected one suspicious main-frame decision block, found {text.count(old_block)}")

text = text.replace(old_block, new_block, 1)

if text.count("SuspiciousMainFrameContextPolicy.beforeCompatibility(") != 1:
    raise SystemExit("Expected one suspicious context early delegation")
if text.count("SuspiciousMainFrameContextPolicy.allowCrossSite(") != 1:
    raise SystemExit("Expected one suspicious context cross-site delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity suspicious main-frame context delegated")
