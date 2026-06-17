# Yield Browser v0.9.63

## YouTube Network Safe Fix

- Khusus YouTube saja.
- Adblock situs umum, Lordborg/site compatibility, direct image guard, desktop/mobile, night mode, video fullscreen, translate, dan download tidak disentuh.
- Pada halaman YouTube, request network tidak lagi diblokir oleh adblock global.
- Tujuannya mencegah player utama YouTube hitam/stuck setelah iklan karena metadata/resource iklan diblokir terlalu kasar di Android WebView.
- YouTube AdBlock tetap berjalan lewat script aman: klik Skip/Lewati dan speed iklan hanya saat sinyal iklan kuat.
- Tidak memblokir `googlevideo.com`, `ytimg.com`, `youtubei/player`, maupun request eksternal YouTube yang dibutuhkan player.
