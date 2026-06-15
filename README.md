# Yield Browser v0.3.5 Download Reload Button

Update ini menambahkan tombol **Reload / download dari awal** untuk unduhan yang gagal, terputus, atau dijeda.

## Perubahan v0.3.5

### Tombol reload
- Jika download **gagal/terputus**, item unduhan menampilkan tombol **↻**.
- Tombol **↻** berarti download ulang dari awal.
- Di menu titik tiga juga ada pilihan **Reload dari awal**.

### Pause / lanjutkan / reload
Status tombol:
- Saat berjalan: **Ⅱ** = Jeda / Pause
- Saat dijeda: **▶** = Lanjutkan
- Saat gagal/terputus: **↻** = Reload dari awal

### Cara kerja reload
- File partial/lama akan dihapus.
- Progress kembali ke 0%.
- Engine mengecek ulang apakah server support split download.
- Jika support, download kembali memakai **2 koneksi paralel**.
- Jika tidak support, fallback ke **1 koneksi**.

## Catatan
Pause masih aman dengan cara menghentikan proses aktif. Reload dipakai ketika koneksi terputus atau file tidak bisa dilanjutkan.
