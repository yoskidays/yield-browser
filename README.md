# YieldBrowser v0.9.85 — Download Engine Integrity & Google Drive Performance

Versi ini berfokus pada stabilitas, integritas file, resume, antrean, dan performa download file besar—terutama Google Drive.

## Perubahan utama

- Download baru selalu melewati queue manager; batas download aktif tidak lagi dapat dilewati.
- Google Drive memakai URL download yang dinormalisasi, final redirect URL yang di-cache, dan mode adaptif 3 koneksi untuk file besar.
- Signed URL yang kedaluwarsa otomatis kembali ke URL sumber untuk memperoleh URL final baru.
- Request multipart wajib menerima `206 Partial Content` dan `Content-Range` yang persis sesuai.
- Setiap part harus selesai sesuai panjangnya sebelum file dapat berstatus completed.
- Resume 3 koneksi dan 4 koneksi menggunakan posisi masing-masing part.
- Resume memakai `ETag`/`Last-Modified` serta `If-Range` untuk mencegah pencampuran versi file.
- Pause, reload, remove, dan retry memakai generation token agar worker lama tidak menulis ke sesi baru.
- Speed limiter berlaku agregat per file, bukan dikalikan pada setiap koneksi.
- Urutan antrean disimpan dalam format ordered history, bukan `StringSet`.
- Foreground keep-alive service dan partial wake lock menjaga download aktif di background.

## HLS

- Resume pada batas segmen.
- Partial segment dibuang saat pause/error agar file tidak korup.
- Pemilihan variant berdasarkan bandwidth tertinggi.
- Dukungan `EXT-X-MAP`, `EXT-X-BYTERANGE`, fMP4, dan AES-128 CBC.
- IV AES-128 dapat berasal dari atribut `IV` atau media sequence.
- `SAMPLE-AES` dan AES-128 byte-range yang tidak aman ditolak secara eksplisit.

## Struktur download

- `DownloadItem.java` — state runtime/persisten dan generation token.
- `DownloadHistoryCodec.java` — serialisasi state multipart/HLS.
- `DownloadProtocol.java` — validasi HTTP Range dan Content-Range.
- `DownloadRateLimiter.java` — limiter agregat lintas koneksi.
- `HlsPlaylistParser.java` — parser master/media playlist.
- `HlsAes128.java` — dekripsi AES-128 HLS.
- `DownloadKeepAliveService.java` — foreground keep-alive dan wake lock.
- `MainActivity.java` — orkestrasi UI dan lifecycle download.

## Build

- Android Gradle Plugin 8.7.3
- Gradle 8.10.2
- Java 17
- compileSdk 35
- targetSdk 35

```bash
gradle testDebugUnitTest
gradle assembleDebug
gradle assembleRelease
```

Project belum menyertakan Gradle Wrapper. Workflow GitHub sudah dikonfigurasi untuk test dan build artifact.
