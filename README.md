# Yield Browser v0.4.7 Pause Resume Fix + Home Bookmark Clear

Update ini memperbaiki dua masalah utama.

## Perubahan v0.4.7

### Pause / Resume download
- Tombol **Ⅱ** tetap untuk jeda.
- Tombol **▶** sekarang mencoba melanjutkan dari file partial yang sudah ada.
- Tombol **↻** tetap khusus untuk reload/download ulang dari awal.
- Untuk download **2 koneksi**, aplikasi menyimpan progres part 1 dan part 2.
- Untuk fallback **1 koneksi**, aplikasi mencoba resume memakai HTTP Range dari byte terakhir.
- Kalau server menolak resume, baru fallback mulai ulang sesuai respons server.

### Home dan bookmark
- Saat klik tombol **Home**, address bar dikosongkan.
- Star bookmark tidak lagi ikut menyala/nempel dari situs sebelumnya.
- Tab aktif dianggap kembali ke halaman awal.

## Catatan
Resume download bergantung pada dukungan server terhadap HTTP Range. Jika server/file host menolak resume, reload dari awal tetap tersedia lewat tombol **↻**.
