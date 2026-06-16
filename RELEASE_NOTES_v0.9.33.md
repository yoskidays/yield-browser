# Yield Browser v0.9.33

## Translate compatible stability fix

- Memperbaiki bug Translate Kompatibel yang bisa reload berulang pada beberapa website.
- Tombol Matikan Translate sekarang benar-benar menghentikan sesi translate aktif.
- Callback translate lama tidak bisa mengaktifkan translate lagi setelah dimatikan.
- Injeksi JavaScript translate dipindahkan ke evaluateJavascript agar tidak dianggap navigasi/reload halaman.
- Translate compatible tetap bisa digunakan manual tanpa bentrok dengan Google Translate proxy, adblock, desktop mode, dan fullscreen/video control.
