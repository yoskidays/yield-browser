# Yield Browser v0.9.35

## Fix
- Fix Desktop Mode OFF masih tampil seperti desktop pada halaman Google/situs responsif.
- Mobile mode sekarang memakai mobile User-Agent eksplisit, bukan reset null.
- Saat Desktop Mode OFF, viewport halaman dipaksa kembali ke `width=device-width` dan style desktop injection dibersihkan.
- Landscape Video Mode tidak lagi ikut mengubah tampilan halaman menjadi desktop.
- Setelah pindah Desktop -> Mobile, browser menjalankan reset viewport mobile beberapa kali agar halaman tidak nyangkut di layout lebar.

## Version
- versionCode: 10
- versionName: 0.9.35
