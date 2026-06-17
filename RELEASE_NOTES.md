# Yield Browser v0.9.58

## Build Fix
- Fixed `settings.gradle` so it is guaranteed to be a valid UTF-8 Gradle text file.
- Prevents GitHub Actions build failure caused by `settings.gradle` being accidentally overwritten by PNG/binary content.

## YouTube Safe AdBlock
- Keeps the latest YouTube ad recovery changes from v0.9.57.
- Does not modify general site adblock, Lordborg compatibility, direct image guard, desktop/mobile mode, night mode, fullscreen video controls, translate, or download manager.
