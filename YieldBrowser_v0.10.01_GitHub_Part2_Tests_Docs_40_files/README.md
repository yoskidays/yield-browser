# Yield Browser v0.10.01

Android browser source project with professional tab spaces, private profile isolation, History Engine V2, HTTPS-First navigation, Universal Reader Compatibility Repair, integrated AdBlock/Shield Engine V2, quiet UI, and progressive download playback infrastructure.

## Reader navigation fix

Version 0.10.01 fixes a Shield Engine V2 false positive that could block legitimate Previous/Next chapter controls on reader pages with long slugs such as `...-chapter-6-1/`. Same-origin advertising relays such as `/r/<token>` remain blocked.

## Version

- `versionCode 75`
- `versionName 0.10.01`
- `minSdk 23`
- `targetSdk 35`

## GitHub Actions

The workflow runs unit tests, Android lint, debug APK build, and unsigned release APK build.
