# Yield Browser v0.9.31

## Status
Final polish source for APK build.

## Launcher
- Launcher label shortened to **Yield** so it does not appear as “Yield Bro...” on Realme launcher.
- Internal branding and splash still use **Yield Browser**.

## GitHub Actions artifacts
After upload to GitHub, open **Actions → Build Yield Browser APK**.

Artifacts:
- `YieldBrowser-v0.9.31-installable-debug`
  - This is the APK to install/test directly on Android.
- `YieldBrowser-v0.9.31-release-unsigned`
  - Unsigned release APK for later signing.
  - Android usually cannot install unsigned release APK directly.

## Recommended install file
Use the debug artifact unless you have signing configured.
