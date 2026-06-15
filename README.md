# Yield Browser v0.4.5 Private Mode + Multi Tab

Update ini mengaktifkan **Mode Privat** dan **Multi Tab**.

## Perubahan v0.4.5

### Mode Privat
- Menu **Privat** sekarang sudah aktif.
- Saat diklik, Yield membuat **tab privat baru**.
- Tab privat tidak menyimpan riwayat browsing.
- Saat tab privat ditutup, cache/history WebView dibersihkan ringan.

### Multi Tab
- Tombol kotak angka di bottom navigation sekarang bisa diklik.
- Saat diklik, muncul halaman daftar tab.
- Ada tombol **+** untuk membuat tab baru.
- Ada tombol **Privat** untuk membuat tab privat.
- Tab bisa dipilih untuk pindah halaman.
- Tab bisa ditutup dengan tombol **×**.
- Angka pada ikon tab mengikuti jumlah tab aktif.

## Catatan
Sistem tab saat ini memakai satu WebView aktif dan menyimpan URL/judul per tab. Ini ringan dan cocok untuk tahap awal. Riwayat WebView per-tab yang benar-benar terpisah bisa dikembangkan di versi berikutnya.


## Perubahan v0.4.6
- Panel riwayat sekarang punya tombol **X** di kanan atas.
- Kategori unduhan (**Semua / Video / APK / Dokumen / Musik / Lainnya**) disegarkan lebih stabil saat diklik.
- Klasifikasi unduhan diperkuat memakai nama file, URL, dan MIME type supaya file video/APK lebih akurat masuk kategori yang benar.
