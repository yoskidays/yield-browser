# Play While Downloading — Implementation Summary

## Architecture

`ProgressiveDownloadServer` starts an app-private loopback HTTP server on `127.0.0.1` and creates an unguessable token for each playback session. `ProgressiveVideoActivity` plays the generated local URL while the existing download threads continue writing the original staged file.

The server supports `HEAD`, `GET`, and HTTP byte ranges. It does not proxy or redownload the remote URL, so browser cookies and anti-hotlink headers remain confined to the existing download engine.

## Sparse multipart protection

Yield pre-allocates files for multipart downloads. File length alone therefore cannot indicate that every byte is ready. `ProgressivePlaybackPolicy` calculates availability from:

- sequential downloaded bytes for a one-connection download;
- `partStart`, `partEnd`, and `partDone` for multipart downloads;
- completed/public file state after finalization.

A player request blocks briefly when its requested byte range is not available. It resumes as soon as that exact region has been written.

## User experience

- Running supported video: tap the row to open the internal player.
- Overflow menu: **Putar sambil mengunduh**.
- Completed supported video: **Tonton di Yield** or **Buka dengan aplikasi lain**.
- Player status: preparing, downloading percentage, speed, paused, verifying, saving, completed, or failed.
- Player exit: playback stops; download remains active.
- Android 8+: explicit Picture-in-Picture button.
- Private session: screenshot/recent-preview protection through `FLAG_SECURE`.

## Compatibility limits

- Direct progressive containers only in v0.9.91.
- HLS output is opened after segment merge.
- A non-fast-start MP4 may wait for metadata near the end of the file; multipart downloading can make that metadata available earlier.
- Device codec support still determines whether MOV/WebM variants can be decoded.
- DRM and split DASH tracks require a separate manifest-aware player/downloader implementation.
