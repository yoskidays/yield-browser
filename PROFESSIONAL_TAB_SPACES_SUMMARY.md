# YieldBrowser v0.9.90 — Professional Tab Spaces

## Behavior

- **Umum** and **Privat** are separate browser spaces.
- A tab keeps the profile in which it was created; profile conversion is not allowed.
- On Android 9+, switching spaces brings the dedicated normal or incognito task forward.
- On Android 6–8, the same selector filters the compatibility-mode tabs locally.
- The `+` button follows the selected space.
- Closing the last private tab returns to the normal space.

## UI

- Segmented `Umum | Privat` selector in the tab switcher.
- Distinct purple private-profile styling.
- Persistent `Mode Privat — Profil terisolasi` banner.
- Private home actions: `Buka tab umum` and `+ Tab privat`.
- Profile-aware actions in Quick Menu and Settings.

## Security invariant

Switching spaces never transfers a live WebView, URL history, cookies, WebStorage, or session state between profiles.
