from pathlib import Path

path = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
text = path.read_text(encoding="utf-8")

if "MobileModeMigration.apply(" in text:
    print("Mobile mode migration delegation already installed")
    raise SystemExit(0)

old = '''    void forceMobileModeAfterUpdateIfNeeded(SharedPreferences p) {
        try {
            if (!p.getBoolean("forceMobileModeV0939", false)) {
                desktopMode = false;
                p.edit()
                        .putBoolean("desktopMode", false)
                        .putBoolean("forceMobileModeV0939", true)
                        .apply();
            }
        } catch (Exception ignored) {
        }
    }
'''

new = '''    void forceMobileModeAfterUpdateIfNeeded(SharedPreferences p) {
        MobileModeMigration.apply(p, () -> desktopMode = false);
    }
'''

if text.count(old) != 1:
    raise SystemExit(f"Expected one legacy mobile mode migration method, found {text.count(old)}")
text = text.replace(old, new, 1)
if text.count("MobileModeMigration.apply(") != 1:
    raise SystemExit("Expected exactly one mobile mode migration delegation")
path.write_text(text, encoding="utf-8")
print("MainActivity mobile mode migration delegated")
