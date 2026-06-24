# HTTPS-First Navigation — YieldBrowser v0.9.98

## Perilaku utama

- URL `http://` publik dari address bar dicoba sebagai `https://` terlebih dahulu.
- Link utama HTTP dari halaman web juga ditingkatkan sebelum navigasi dilanjutkan.
- Jika koneksi HTTPS gagal karena host lookup, koneksi, timeout, atau I/O, Yield kembali ke URL HTTP asli.
- Kegagalan sertifikat/TLS tidak diturunkan diam-diam ke HTTP.
- `localhost`, hostname intranet, domain lokal, alamat IP privat, `.onion`, dan port nonstandar dikecualikan.
- Port eksplisit `:80` dipetakan ke port HTTPS standar; `:443` dipertahankan.
- Guard fallback per-tab selama lima menit mencegah loop HTTP ↔ HTTPS.
- Bookmark HTTP diperbarui ke URL HTTPS final setelah upgrade berhasil.
- Pengaturan `HTTPS-First` tersedia dan aktif secara default.

## Keamanan

Fallback hanya dijalankan untuk error konektivitas utama. Error sertifikat tidak dianggap sebagai bukti bahwa HTTPS tidak tersedia karena hal tersebut dapat menunjukkan konfigurasi sertifikat yang rusak atau serangan jaringan.

## Quiet UI policy

HTTPS upgrade, successful HTTPS navigation, HTTP fallback, bookmark upgrade, and
loop-guard recovery run silently. They do not display informational Toast messages.
Only failures that require direct user action may surface a compact error Toast.
