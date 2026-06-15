# Yield Browser v0.3.9 Premium Fast Download Engine

Update ini mempertahankan default **2 koneksi paralel** agar stabil, lalu mengoptimalkan engine download agar terasa lebih cepat dan premium.

## Perubahan v0.3.9

### Premium Fast Engine
- Default tetap **2 koneksi paralel**.
- Jika server support HTTP Range:
  - file dipecah menjadi 2 bagian
  - dua bagian diunduh bersamaan
  - progress digabung menjadi satu
- Jika server tidak support Range:
  - otomatis fallback ke **1 koneksi**

### Optimasi kecepatan
- Buffer download dinaikkan dari 8KB menjadi **64KB**.
- Header download diperkuat:
  - User-Agent WebView
  - Cookie session WebView
  - Accept-Encoding identity
  - Keep-alive
  - No-cache
- Timeout koneksi/read dibuat lebih aman untuk file besar.
- Update UI/notifikasi dibuat lebih ringan agar proses download tidak terlalu terbebani.

### Tampilan
- Info engine ditampilkan sebagai:
  - `Premium Fast Engine: 2 koneksi paralel`
  - `Premium Fast Engine: fallback 1 koneksi`

## Catatan
2 koneksi dipilih sebagai default karena lebih stabil dibanding 3 koneksi pada banyak server, tapi tetap memberi efek percepatan seperti download manager.
