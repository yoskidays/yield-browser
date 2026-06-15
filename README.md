# Yield Browser v0.4.1 Gofile / Anti-Hotlink Download Fix

Update ini memperkuat download engine untuk website file host seperti **gofile.io**, SFile, Pixeldrain, dan host lain yang sering memakai anti-hotlink.

## Perubahan v0.4.1

### Anti-hotlink safe headers
Downloader sekarang membawa header lebih mirip browser:
- User-Agent dari WebView/DownloadListener
- Cookie dari file URL
- Cookie dari halaman referer
- Cookie dari domain utama
- Cookie `gofile.io`
- Referer halaman asal
- Origin halaman asal
- Sec-Fetch headers
- Accept-Encoding identity
- Keep-alive

### Redirect aman
- Redirect 301/302/303/307/308 diikuti manual.
- Header anti-hotlink tetap dikirim ulang di setiap redirect.

### Split download lebih aman
- Tetap coba **2 koneksi paralel**.
- Jika server/file host menolak split range, otomatis fallback ke **1 koneksi browser-like**.
- Ini penting karena beberapa host mengizinkan download normal tapi menolak multi-range.

### Deteksi gagal lebih jelas
- Jika server memberi HTTP 403/404/500, alasan gagal ditampilkan di item download.
- Jika link mengarah ke halaman HTML bukan file, aplikasi memberi pesan agar klik tombol download asli dari halaman.

## Catatan penting
Gofile kadang memakai token/captcha/session yang dibuat dari JavaScript. Jika tombol download asli belum menghasilkan direct file URL, WebView tidak selalu bisa mengambil file langsung. Update ini membuat request lebih aman dari hotlink, tetapi captcha/token tetap harus dilewati oleh user di halaman web.
