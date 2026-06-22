# Reader Navigation Fix — v0.10.01

## Root cause

Long semantic chapter slugs were treated as opaque relay tokens when navigation started from another chapter page. The reader context score raised the URL to the blocking threshold even though it was a legitimate same-site chapter destination.

## Fix

- Added `isSafeSameSiteReaderNavigation()` before relay scoring.
- Recognizes embedded chapter/episode tokens such as `-chapter-6-1`.
- Allows legitimate same-site Previous/Next navigation from reader pages.
- Added DOM metadata checks for `rel=next`, `rel=prev`, chapter, episode, lanjut, berikut, and sebelum controls.
- Prevents cosmetic overlay cleanup from disabling recognized reader navigation controls.
- Same-origin relay paths and hard advertising tokens remain blocked.

## Validation

- Java policy smoke test passed for next, previous, and relay cases.
- Generated Shield JavaScript passed `node --check`.
- Regression tests were added to `ShieldEngineV2Test` and `ShieldPageScriptTest`.
