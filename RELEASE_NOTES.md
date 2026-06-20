# YieldBrowser v0.9.91

## Putar sambil mengunduh

- Added an internal progressive video player for active downloads.
- Added a loopback-only HTTP Range server bound to `127.0.0.1`.
- Added sparse-aware multipart playback so unfinished pre-allocated file regions are not read as valid media.
- Added live download progress and speed inside the player.
- Added automatic waiting/buffering when playback reaches data that has not arrived yet.
- Added retry handling when a video container needs more metadata before it can start.
- Added optional Picture-in-Picture on Android 8+.
- Added **Putar sambil mengunduh** and **Tonton di Yield** actions in the download manager.
- Added a download setting to enable or disable progressive playback.
- Added secure-window protection for playback launched from a private tab/profile.

## Supported scope

- Progressive MP4, M4V, 3GP, WebM, MOV, and direct `videoplayback` downloads.
- HLS/m3u8 continues to use the existing segment downloader and becomes playable after merge.
- DRM and separate DASH audio/video representations are intentionally excluded from this first implementation.

## Existing improvements retained

- Professional **Umum | Privat** tab spaces.
- Isolated Android 9+ incognito WebView process/profile.
- Adaptive 1–4 connection download engine.
- Pause/resume, queueing, automatic retry, ETA, speed smoothing, and finalization progress.
