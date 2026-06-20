# YieldBrowser v0.9.91 — Play While Downloading

YieldBrowser v0.9.91 adds professional progressive video playback while retaining the v0.9.90 two-space **Umum | Privat** browser architecture.

## Putar sambil mengunduh

- Direct progressive videos can be opened before the download finishes.
- The internal Yield player reads only byte ranges that are already available.
- Multipart downloads are sparse-safe: pre-allocated but unfinished regions are never exposed as valid video data.
- HTTP Range requests allow the player to request the beginning, metadata, or a downloaded seek region without duplicating the download.
- Download progress, averaged speed, paused state, verification, and final save state remain visible inside the player.
- Closing the player does not stop the download.
- Optional Picture-in-Picture is available on Android 8+.
- Private playback sessions enable `FLAG_SECURE` and use an unguessable loopback token.

## Professional download flow

1. Start a supported video download.
2. Open **Unduhan**.
3. Tap the running video or choose **Putar sambil mengunduh** from its menu.
4. Yield waits until enough verified initial bytes are available.
5. Playback starts while the original background download continues.
6. If playback catches the available data, the player buffers instead of reading empty multipart regions.

Supported first-stage containers: MP4, M4V, 3GP, WebM, MOV, and direct `videoplayback` responses.

HLS/m3u8 remains available after segment merge completes. DRM streams and separate DASH audio/video tracks are not treated as progressive files.

## Browser spaces retained

- **Umum**: persistent browsing session, normal history, normal cookies, and restorable tabs.
- **Privat**: isolated WebView process/profile on Android 9+, non-persistent tabs, separated cookies and WebStorage.
- Existing tabs are never converted between profiles.
- The tab switcher provides an explicit **Umum | Privat** selector.
- Closing the final private tab returns to the normal browser space.

## Build

The repository uses Java 17, compile/target SDK 35, and GitHub Actions with Gradle 8.10.2.

```bash
gradle testDebugUnitTest --stacktrace
gradle assembleDebug --stacktrace
```

Version metadata:

- `versionCode 65`
- `versionName 0.9.91`
