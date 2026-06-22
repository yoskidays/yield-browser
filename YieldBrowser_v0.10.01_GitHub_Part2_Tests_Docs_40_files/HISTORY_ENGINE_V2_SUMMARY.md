# History Engine V2 Summary

## Masalah lama yang dihilangkan

Sistem lama membaca dan menulis beberapa salinan riwayat pada SharedPreferences, file internal, file legacy, dan file external. Menu riwayat juga membuat hingga ratusan view sekaligus dan memulai satu thread favicon untuk setiap row. Penghapusan satu item menulis ulang seluruh koleksi dan merender ulang seluruh daftar.

## Arsitektur baru

`WebView navigation -> HistoryRepository -> SQLite/WAL -> keyset pagination -> RecyclerView`

### Database

Tabel `history_entries` menyimpan `id`, `title`, `url`, `host`, `last_visit_time`, dan `visit_count`. URL dibuat unik agar kunjungan berulang memperbarui satu entry dan menaikkan jumlah kunjungan.

Indeks:

- `last_visit_time DESC, id DESC`
- `host`

### Threading

Seluruh operasi database diserialisasi pada executor background tunggal. Callback kembali ke main thread. Tidak ada baca/tulis database pada UI thread.

### Pagination

Daftar memuat 50 entry terbaru. Halaman berikutnya memakai cursor waktu dan ID dari item terakhir, sehingga data baru tidak menyebabkan item lompat atau duplikat seperti pagination berbasis OFFSET.

### Penghapusan

- Hapus satu: `DELETE ... WHERE id = ?`, lalu `notifyItemRemoved()`.
- Hapus semua: satu transaksi `DELETE`, tanpa menutup tab aktif.

### Persistence

Database berada di internal app data dan tidak dihapus saat aplikasi ditutup, recent apps dibersihkan, perangkat restart, atau APK diperbarui. Data hanya hilang jika pengguna menghapus riwayat, menghapus data aplikasi, atau uninstall.

### Legacy reset

Marker `history_engine_v2_initialized` ditulis hanya setelah reset SQLite berhasil. Storage riwayat lama dihapus satu kali dan tidak pernah ditulis ulang.
