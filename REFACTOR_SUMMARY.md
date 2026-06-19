# YieldBrowser Source Refactor Summary — v0.9.85

## Struktur

- Konstanta di `BrowserConstants`.
- Helper URL browser di `BrowserUrlUtils`.
- Model tab, bookmark, history, shortcut, dan download dipisahkan dari Activity.
- Serialisasi download di `DownloadHistoryCodec`.
- Validasi protokol di `DownloadProtocol`.
- Limiter bandwidth agregat di `DownloadRateLimiter`.
- Parser dan crypto HLS di `HlsPlaylistParser` serta `HlsAes128`.
- Background keep-alive di `DownloadKeepAliveService`.

## Pembersihan

- Menghapus overload helper yang tidak pernah dipanggil.
- Menghapus prefetch download HLS yang hanya membuang bandwidth.
- Menghapus header request browser palsu dari worker download.
- Mengganti penyimpanan antrean `StringSet` dengan urutan deterministik.
- Mengurangi penulisan SharedPreferences pada setiap perubahan kecil progress.

## Pengujian

- `StorageCodecTest`
- `DownloadHistoryCodecTest`
- `DownloadProtocolTest`
- `HlsPlaylistParserTest`
- `HlsAes128Test`

Workflow GitHub menjalankan unit test, debug APK, dan unsigned release APK menggunakan Gradle 8.10.2 dan Java 17.
