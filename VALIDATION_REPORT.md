# Validation Report — YieldBrowser v0.9.91

## Completed static checks

- Parsed 42 production and unit-test Java files with `javalang`.
- Checked 713 Java method declarations for duplicate signatures.
- Parsed 45 Android XML resource/manifest files.
- Verified all manifest activities, services, and receivers have matching source classes.
- Verified app drawable and mipmap references.
- Verified `versionCode 65` and `versionName 0.9.91`.
- Verified GitHub Actions artifact names use v0.9.91.
- Compiled and executed a pure-Java smoke test for `ProgressivePlaybackPolicy`.

## Progressive playback scenarios verified at policy/source level

1. MP4 and other configured progressive containers are accepted.
2. HLS/m3u8 is excluded from growing-file playback.
3. A sequential download exposes only its completed prefix.
4. A multipart pre-allocated file exposes completed ranges but not sparse holes.
5. A completed file exposes its full byte range.
6. Player requests use a loopback-only server and unguessable session token.
7. Player status follows running, paused, queued, verifying, saving, completed, and failed states.
8. Closing the player does not call pause or cancel on the download item.
9. Private playback adds secure-window protection.
10. Android 8+ Picture-in-Picture is declared on a resizable player activity.

## Required runtime test matrix

- Android 11 direct MP4 with one connection.
- Android 11 MP4 with 2, 3, and 4 multipart connections.
- Pause and resume while playback is active.
- Seek inside a downloaded range.
- Seek into a not-yet-downloaded range and confirm buffering/recovery.
- Download completion while the player remains open, including staged-file export to MediaStore.
- Private-profile playback and secure recent-app preview.
- Picture-in-Picture entry and return.
- MP4 with `moov` metadata at the beginning and at the end.
- WebM/MOV compatibility on representative devices.

## Environment limitation

A full Android Gradle build and emulator/device test were not executed in this container because Android SDK and Gradle are unavailable. The included GitHub Actions workflow runs unit tests and produces debug/release APK artifacts and remains the authoritative full build check.
