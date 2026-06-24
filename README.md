# Yield Browser v0.10.05

Android browser source project with professional tab spaces, private profile isolation, History Engine V2, HTTPS-First navigation, Universal Reader Compatibility Repair, integrated AdBlock/Shield Engine V2, persistent manual element filtering, quiet UI, progressive download playback, and long-press link actions.

## Persistent Block Element mode

Version 0.10.05 changes **Blokir elemen** into a continuous picker. After **Blokir & lanjut** is selected, the picker stays active so another element can be selected immediately. A large **X** button on the right side of the orange picker bar is the explicit exit control.

Manual element filters are now independent from the AdBlock master switch. A saved filter remains active when AdBlock is OFF, on compatibility/reader pages, after reopening the site from a bookmark or history, and after restoring a live tab. Filters are stored per normalized host and remain enabled until removed from **Filter situs ini**.

The selector engine now reports the number of matched elements, avoids common dynamic IDs/classes, adds `:nth-of-type` only when required, and validates every selector again on Android. Critical document and media elements cannot be blocked. Pointer/touch event deduplication prevents a single tap from producing duplicate dialogs.

## Reader-safe Click Hijack Protection

Version 0.10.04 fixed reader navigation controls that could become unresponsive while **Proteksi Click Hijack** was enabled. Reader/content pages use the DOM-aware Shield Engine V2 click guard, including click-through recovery when a suspicious transparent overlay covers a legitimate same-site chapter control.

## Back to Home loading fix

Android Back resets the current tab and destroys its WebView when no earlier page history remains. JavaScript, media, redirects, network activity, and late callbacks from the previous page cannot continue behind Home.

## Open link in new tab

Press and hold a normal web link or an image wrapped by a link, choose **Buka link di tab baru**, and Yield Browser creates, activates, and loads the destination in a new tab while preserving the original tab.

## Version

- `versionCode 79`
- `versionName 0.10.05`
- `minSdk 23`
- `targetSdk 35`

## GitHub Actions

The workflow runs unit tests, Android lint, debug APK build, and unsigned release APK build.
