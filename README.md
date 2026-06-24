# Yield Browser v0.10.04

Android browser source project with professional tab spaces, private profile isolation, History Engine V2, HTTPS-First navigation, Universal Reader Compatibility Repair, integrated AdBlock/Shield Engine V2, quiet UI, progressive download playback, and long-press link actions.

## Reader-safe Click Hijack Protection

Version 0.10.04 fixes reader navigation controls that could become unresponsive while **Proteksi Click Hijack** was enabled.

Reader and content pages now use only the DOM-aware Shield Engine V2 click guard instead of stacking it with the older premium click listener. This prevents a legitimate touch from being consumed twice before a Next/Previous Chapter link receives it.

Shield Engine V2 also performs click-through recovery. When a suspicious transparent overlay captures a touch above a legitimate same-site reader control, the ad navigation is cancelled and the safe chapter destination underneath the touch point is opened directly. The recovery supports normal anchors, buttons, common `data-*` URL attributes, and simple `location.assign`/`location.href` handlers.

High-confidence advertising behavior remains blocked, including known ad hosts, dangerous external schemes, opaque cross-site click destinations, and same-origin relay paths such as `/r/`, `/go/`, `/out/`, and `/redirect/`.

## Back to Home loading fix

Android Back resets the current tab and destroys its WebView when no earlier page history remains. JavaScript, media, redirects, network activity, and late callbacks from the previous page cannot continue behind Home.

## Open link in new tab

Press and hold a normal web link or an image wrapped by a link, choose **Buka link di tab baru**, and Yield Browser creates, activates, and loads the destination in a new tab while preserving the original tab.

## Version

- `versionCode 78`
- `versionName 0.10.04`
- `minSdk 23`
- `targetSdk 35`

## GitHub Actions

The workflow runs unit tests, Android lint, debug APK build, and unsigned release APK build.
