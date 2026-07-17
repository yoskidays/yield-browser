from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "ReloadLoopRegistrationPolicy.plan(" in text:
    print("Reload-loop registration delegation already installed")
    raise SystemExit(0)

old_block = '''        if (key.equals(reloadLoopLastKey) && (now - reloadLoopWindowStartMs) <= 12000L) {
            reloadLoopCount++;
        } else {
            reloadLoopLastKey = key;
            reloadLoopWindowStartMs = now;
            reloadLoopCount = 1;
        }

        if (reloadLoopCount >= 4) {
            reloadLoopGuardHost = host;
            reloadLoopGuardKey = key;
            reloadLoopGuardUntilMs = now + 120000L;
            enableSiteCompatibilityModeForUrl(url);
            reloadLoopCount = 0;
            if ((now - reloadLoopToastLastMs) > 6000L) {
                reloadLoopToastLastMs = now;
                QuietToast.makeText(this, "Reload loop dicegah untuk situs ini", QuietToast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
'''

new_block = '''        ReloadLoopRegistrationPolicy.Plan plan = ReloadLoopRegistrationPolicy.plan(
                key,
                reloadLoopLastKey,
                reloadLoopWindowStartMs,
                reloadLoopCount,
                reloadLoopToastLastMs,
                now);
        reloadLoopLastKey = plan.lastKey;
        reloadLoopWindowStartMs = plan.windowStartMs;
        reloadLoopCount = plan.count;

        if (plan.guardTriggered) {
            reloadLoopGuardHost = host;
            reloadLoopGuardKey = key;
            reloadLoopGuardUntilMs = plan.guardUntilMs;
            enableSiteCompatibilityModeForUrl(url);
            if (plan.showToast) {
                reloadLoopToastLastMs = now;
                QuietToast.makeText(this, "Reload loop dicegah untuk situs ini", QuietToast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
'''

if text.count(old_block) != 1:
    raise SystemExit(
        f"Expected one legacy reload-loop registration block, found {text.count(old_block)}")

text = text.replace(old_block, new_block, 1)

if text.count("ReloadLoopRegistrationPolicy.plan(") != 1:
    raise SystemExit("Expected exactly one reload-loop registration delegation")

path.write_text(text, encoding="utf-8")
print("MainActivity reload-loop registration delegated")
