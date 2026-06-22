# Android 11 NoSuchMethodError Fix — v0.9.95

## Crash

`java.lang.NoSuchMethodError: java.lang.String.formatted(...)` pada `NightModePageScript.enable()`.

## Penyebab

`String.formatted(Object...)` tersedia pada Java desktop modern, tetapi tidak tersedia pada runtime Android 11/API 30. Build berhasil karena source dikompilasi menggunakan JDK 17, lalu aplikasi crash ketika metode itu dipanggil di perangkat.

## Perbaikan

- Menghapus seluruh pemanggilan `String.formatted(...)`.
- Menggunakan placeholder unik dan `String.replace(...)`, tersedia pada seluruh rentang Android yang didukung Yield.
- Mempertahankan script mode malam yang sama; hanya proses penyusunan string Java yang diubah.
- Menaikkan versi ke `versionCode 69` / `versionName 0.9.95`.
