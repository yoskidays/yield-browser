> Catatan v0.9.93: bagian perbaikan reader khusus Komiku di dokumen historis ini telah digantikan oleh `UNIVERSAL_READER_COMPATIBILITY_SUMMARY.md`.

# Night Mode and Komiku Compatibility Fix

## Root cause
The application targets API 35. `WebSettings.setForceDark()` no longer affects apps targeting API 33 or newer, so Android 11 showed the setting as ON without darkening web content.

## Resolution
- Added AndroidX WebKit 1.15.0, which still supports the project minimum API 23.
- Enabled `WebSettingsCompat.setAlgorithmicDarkeningAllowed()` when the installed WebView reports `ALGORITHMIC_DARKENING`.
- Kept a conservative DOM fallback and prevented compatibility mode from disabling night mode.

## Komiku reader resolution
- Compatibility navigation within `komiku.org` and its subdomains bypasses aggressive main-frame recovery.
- Network images are explicitly enabled in the compatibility profile.
- Lazy image attributes are promoted to real `src`/`srcset` values and hidden chapter images are made visible.
- AdBlock network/cosmetic injection remains disabled for the active compatibility page.
