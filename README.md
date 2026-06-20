# YieldBrowser v0.9.90 — Professional Tab Spaces

YieldBrowser v0.9.90 introduces a professional two-space tab model while preserving the isolated WebView incognito architecture and the smooth download manager from previous versions.

## Browser spaces

- **Umum**: persistent browsing session, normal history, normal cookies, and restorable tabs.
- **Privat**: isolated WebView process/profile on Android 9+, non-persistent tabs, separated cookies and WebStorage, and protected recent-app previews.
- Existing tabs are never converted between profiles.
- The tab switcher provides an explicit **Umum | Privat** selector.
- The `+` action creates a tab in the selected space.
- Closing the final private tab returns to the normal browser space.

## Private-space UX

- Persistent `Mode Privat — Profil terisolasi` strip.
- Distinct purple private styling.
- Quick action to return to normal browsing.
- Private home actions for switching to normal browsing or opening another private tab.
- Profile-aware actions in Quick Menu and Settings.
- `Buka di tab Privat` forwards the URL directly to the isolated process and never opens it in a normal tab first.

## Existing platform features retained

- Adaptive 1–4 connection download engine with Range validation and resume protection.
- Google Drive URL resolution and adaptive connection fallback.
- Foreground background downloads with download-only notifications.
- RecyclerView-based smooth download UI, ETA, averaged speed, and finalization progress.
- AndroidX support enabled through root `gradle.properties`.

## Build

The repository uses Java 17, compile/target SDK 35, and GitHub Actions with Gradle 8.10.2.

```bash
gradle testDebugUnitTest --stacktrace
gradle assembleDebug --stacktrace
```

Version metadata:

- `versionCode 64`
- `versionName 0.9.90`
