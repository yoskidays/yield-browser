# Validation Report — YieldBrowser v0.9.88

## Pemeriksaan yang lulus

- 36 file Java produksi/test berhasil diparse dengan parser Java.
- 45 XML resource/AndroidManifest berhasil diparse.
- Tidak ditemukan duplicate Java method signature.
- Tidak ditemukan import Java yang terdeteksi tidak terpakai.
- Pure-Java download classes berhasil dikompilasi dengan `javac`.
- 25 unit-test method lulus melalui lightweight JUnit-compatible runner.
- Standalone harness tambahan lulus untuk:
  - progres basis-point;
  - smoothing kecepatan;
  - ETA;
  - serialisasi finalization state;
  - immutable UI snapshot comparison.
- Resource reference dan manifest class checks lulus setelah Android framework resources dikecualikan.
- Version metadata dan GitHub artifact names selaras pada v0.9.88.

## Skenario yang dicakup

1. File >100 MB mengubah progres tanpa membangun ulang seluruh daftar.
2. Beberapa download aktif hanya memperbarui row masing-masing.
3. Speed berfluktuasi tetapi teks kecepatan tetap stabil.
4. ETA diperbarui dari throughput rata-rata.
5. Network transfer selesai lalu UI berpindah ke **Memverifikasi**.
6. Copy ke Downloads menampilkan progres **Menyimpan**.
7. Copy gagal membersihkan partial destination dan staging file tetap dapat dibuka.
8. Copy berhasil menghapus staging file dan memakai content URI.
9. Proses berhenti saat finalisasi lalu dipulihkan tanpa mengunduh ulang payload lengkap.
10. HLS menampilkan progres segmen secara benar.
11. Activity masuk background dan UI ticker berhenti tanpa menghentikan download engine.
12. Unduhan dimulai tanpa membuka Download Manager secara paksa.

## Batas lingkungan

APK Android penuh belum dikompilasi secara lokal karena runtime ini tidak menyediakan Android SDK dan Gradle. Workflow GitHub yang disertakan tetap menjalankan unit test serta build debug/release APK.
