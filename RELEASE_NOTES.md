# Release Notes — YieldBrowser v0.9.89

## Smooth download UI

- Mengganti `ScrollView + LinearLayout` dengan `RecyclerView + ListAdapter + DiffUtil`.
- Menambahkan stable item ID dan immutable UI snapshot.
- Menghindari `removeAllViews()` pada setiap perubahan progres.
- Menambahkan tab **Mengunduh** dan **Selesai**.
- Menambahkan empty state, filter kategori, pencarian, sorting, dan mode multi-select yang tetap ringan.

## Live progress

- Progres presisi 0–10.000 dan animasi progress bar.
- UI ticker 300 ms yang berhenti otomatis ketika Activity berada di background.
- Exponential moving average untuk kecepatan.
- Estimasi waktu tersisa berdasarkan throughput stabil.
- Perbaikan tampilan progres HLS berdasarkan jumlah segmen, bukan mencampur byte dengan jumlah segmen.

## Finalization pipeline

- Menambahkan status `verifying` dan `saving`.
- Menampilkan progres penyalinan ke MediaStore/SAF.
- Buffer ekspor dinaikkan menjadi 256 KB.
- Partial MediaStore/SAF output dibersihkan jika ekspor gagal atau dibatalkan.
- Staging file dihapus setelah ekspor berhasil.
- Rename file mendukung content URI/MediaStore setelah staging file dihapus.
- Pemulihan finalisasi dapat melanjutkan langsung tanpa mengunduh ulang payload yang sudah lengkap.

## Interaction

- Download Manager tidak lagi terbuka otomatis 250 ms setelah unduhan dimulai.
- Menambahkan banner nonintrusif dengan tombol **Lihat**.
- Foreground notification menampilkan progres finalisasi yang sebenarnya.
- Notifikasi antrean lama dibatalkan ketika item mulai menjadi download aktif.

## Tests

- Menambahkan `DownloadUiMetricsTest`.
- Menambahkan pengujian persistensi status finalisasi.
- Mempertahankan seluruh pengujian protocol, HLS, storage, tab lifecycle, dan private profile.

## AndroidX build configuration

- Menambahkan root `gradle.properties` dengan `android.useAndroidX=true`.
- Menonaktifkan Jetifier karena project tidak memakai `android.support.*`.
- Menambahkan konfigurasi JVM, parallel build, dan Gradle build cache.
- Memperbaiki kegagalan `:app:checkDebugAarMetadata` pada GitHub Actions.
