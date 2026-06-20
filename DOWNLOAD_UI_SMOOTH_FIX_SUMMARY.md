# YieldBrowser v0.9.88 — Download UI Smooth Fix Summary

## Masalah yang diperbaiki

Sebelumnya, progres satu file memanggil `removeAllViews()` dan membangun ulang seluruh daftar download. File besar juga tampak berhenti pada 98–99% karena proses penyalinan ke folder Downloads tidak memiliki status atau progres tersendiri.

## Implementasi utama

### 1. Incremental list rendering

- `RecyclerView`
- `ListAdapter`
- `DiffUtil.ItemCallback`
- stable item ID
- immutable `DownloadUiItem`

Hanya row yang berubah yang di-bind ulang.

### 2. Smooth progress pipeline

- Skala progress: 0–10.000
- UI refresh: 300 ms
- Progress animation: 260 ms pada Android lama atau native animated progress pada Android baru
- Speed smoothing: EMA 78% nilai sebelumnya + 22% sampel baru
- ETA: berdasarkan smoothed throughput

### 3. File finalization

- `verifying`: memastikan staging file ada dan ukurannya benar
- `saving`: menyalin ke MediaStore atau SAF dengan progress callback
- `completed`: hanya diberikan setelah file siap dibuka

Copy menggunakan buffer 256 KB. State finalisasi disimpan berkala dan partial destination dibersihkan jika terjadi error.

### 4. Storage cleanup

Setelah ekspor berhasil, staging file dihapus. Riwayat download menyimpan content URI sebagai lokasi utama file.

### 5. Nonintrusive start interaction

Unduhan tidak lagi membuka Download Manager secara otomatis. Banner sementara menampilkan nama file dan tombol **Lihat**.

## File baru

- `DownloadListAdapter.java`
- `DownloadUiItem.java`
- `DownloadUiMetrics.java`
- `DownloadUiMetricsTest.java`

## Kompatibilitas

Perubahan tidak menghapus queue, multipart resume, Google Drive optimization, HLS, foreground service, incognito process, atau notification policy v0.9.87.
