# Yield Browser v0.9.66

## Build Fix + YouTube Smart Auto Bypass

### Build Fix

- Memperbaiki error compile Java 8 pada Universal Blank Compatibility.
- Regex JavaScript `\s+` sekarang di-escape aman di dalam string Java sehingga tidak dianggap fitur text block/source 15.

### Universal Blank Compatibility

- Tetap mempertahankan deteksi halaman blank saat AdBlock ON.
- Host yang blank otomatis masuk compatibility mode tanpa perlu tambah domain satu-satu.
- Recovery diberi cooldown agar tidak membuat reload-loop baru.

### YouTube Smart Auto Bypass

- Khusus YouTube saja.
- Saat iklan terdeteksi, Yield tetap mencoba klik Skip/Lewati dan speed iklan jika diperlukan.
- Setelah tombol Skip diklik atau iklan selesai, YouTube AdBlock masuk bypass/recovery sementara sekitar 8 detik.
- Tujuannya agar video utama YouTube bisa play normal dan tidak blank/hitam karena script adblock masih menempel.

### Tidak disentuh

- AdBlock situs umum.
- Lordborg / situs sejenis.
- Instant Monitor compatibility.
- Smart redirect context.
- Direct image guard.
- Desktop/Mobile mode.
- Night Mode.
- Kontrol video fullscreen.
- Translate.
- Download Manager.
