# Perbaikan v0.9.92 — Download buffer, "blank" play-while-download, & kontrol video

Tiga perbaikan, semuanya berisiko rendah dan tidak mengubah arsitektur inti.

## 1. Kecepatan download: buffer baca/tulis adaptif

Sebelumnya semua unduhan memakai buffer tetap 128 KB. Sekarang ukuran buffer
menyesuaikan ukuran file lewat `chooseDownloadBufferSize(totalBytes)`:

| Ukuran file | Buffer |
|-------------|--------|
| < 4 MB | 64 KB |
| 4–64 MB | 128 KB (default lama) |
| 64–512 MB | 256 KB |
| > 512 MB | 512 KB |
| tidak diketahui | 256 KB |

Buffer besar pada file besar mengurangi jumlah operasi I/O ke flash → throughput
naik. Tetap kecil pada file kecil agar hemat RAM. Aman: buffer dialokasikan per
koneksi, maksimum 512 KB × 4 koneksi = 2 MB per unduhan. Diterapkan pada dua loop
unduhan progressive/multipart di `MainActivity.java`.

## 2. Video "blank" saat diputar sambil mengunduh (jalur ringan: A + B + C)

**Akar masalah:** banyak MP4 (dan `videoplayback`) menaruh atom **`moov`
(indeks video) di akhir file**. `MediaPlayer` butuh `moov` sebelum bisa mulai,
tetapi ujung file justru paling akhir terunduh. Server lama lalu **menggantung
diam** (menunggu byte hingga 120 dtk) padahal sudah mengirim header `206` +
`Content-Length` penuh → `MediaPlayer` timeout → layar blank. Status `playable`
juga palsu (cukup prefix 256 KB), memicu start prematur yang gagal berulang.

Perbaikan di `ProgressiveDownloadServer.java` + policy:

- **A. Origin side-fetch (ala UC).** Jika byte yang diminta belum diunduh lokal
  (mis. `moov` di akhir / seek ke depan), server mengambilnya **langsung dari URL
  sumber** (`serveFromOrigin`) memakai UA/Referer/Cookie milik unduhan, lalu
  meneruskannya ke player — download latar tetap jalan. Hanya diterima bila sumber
  menghormati `Range` (HTTP 206); jika balas `200` (file penuh), dibatalkan agar
  tidak mengunduh ulang seluruh file.
- **B. `playable` jujur** (`computePlayable`). Memindai struktur kotak MP4 dari
  prefix yang tersedia untuk menemukan `moov`. Hanya melaporkan playable bila
  `moov` benar-benar dapat dibaca (faststart di depan) atau dapat diambil dari
  sumber saat seek. Menghapus start prematur penyebab loop error.
- **C. Tidak menggantung.** `serveMedia` kini menyajikan **hanya bagian yang
  tersedia sekarang** (clamp ke batas terunduh) lalu membiarkan player meminta
  lanjutannya; bila byte awal belum ada → origin-fetch; bila gagal → tunggu
  singkat (≤ 8 dtk) lalu balas `416` yang jelas, bukan menggantung 120 dtk.

## 3. Kontrol video tidak muncul

`MediaController` bawaan (dipakai di dalam `FrameLayout` dengan overlay) sering
tidak tampil/terblok. Diganti dengan **kontrol kustom** di `ProgressiveVideoActivity`:
tombol play/pause, seek bar (dengan secondary progress = posisi unduhan), serta
waktu berjalan/total. Ketuk area video untuk menampilkan/menyembunyikan
(auto-hide 3,5 dtk saat memutar). Seek dibatasi hingga bagian yang sudah terunduh
agar tidak melompat ke data yang belum ada.

## Catatan kompatibilitas (pra-ada, di luar perubahan ini)

`getContentLengthLong()` (butuh API 24) masih dipakai di `MainActivity` (≈baris
8921, 9025) dan `DownloadProtocol` (≈baris 72), padahal `minSdk 23`. Berpotensi
crash pada Android 6 di jalur tersebut. Kode baru di perbaikan ini **tidak**
memakai API itu (memakai parsing header `Content-Length` manual). Disarankan
menambal tiga pemanggilan lama itu pada iterasi berikutnya.

## Validasi

- `MainActivity.java`, `ProgressiveDownloadServer.java`,
  `ProgressiveVideoActivity.java` lolos parse `javalang`; tidak ada duplikasi
  signature method.
- Build Gradle penuh + uji perangkat tetap perlu via GitHub Actions
  (Android SDK tidak tersedia di lingkungan editing ini).

### Matriks uji yang disarankan

- MP4 dengan `moov` di depan (faststart) dan di akhir, 1 dan 2–4 koneksi.
- Seek ke bagian sudah-terunduh dan ke bagian belum-terunduh (cek origin-fetch).
- File besar (> 512 MB) untuk efek buffer adaptif.
- Kontrol: muncul/sembunyi saat diketuk, play/pause, seek, auto-hide, PiP.
