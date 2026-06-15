# Yield Browser v0.4.3 Download Speed Indicator

Update ini menambahkan indikator kecepatan download pada item unduhan.

## Perubahan v0.4.3

- Di bawah progress bar, teks persen sekarang menampilkan speed:
  - `57% • 2.40 MB/s`
  - `30% • 850 KB/s`
- Speed dihitung dari perubahan byte per interval waktu.
- Speed tampil saat download berjalan.
- Saat download dijeda/gagal/selesai, speed otomatis direset.
- Notifikasi progress juga menampilkan speed download.

## Contoh tampilan
`57% • 2.40 MB/s`

## Catatan
Fitur Premium Fast Engine tetap memakai 2 koneksi paralel jika server mendukung HTTP Range, dan fallback ke 1 koneksi jika server menolak split.
