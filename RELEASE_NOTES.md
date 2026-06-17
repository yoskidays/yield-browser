# Yield Browser v0.9.50

## Smart Redirect Context Fix

- Memperbaiki situs mirip Lordborg yang link/menu-nya gagal diklik karena dianggap direct ads.
- Domain seperti `invest-tracing.com` sekarang dibedakan berdasarkan konteks:
  - jika muncul dari redirect otomatis/pop-up tersembunyi, tetap diarahkan ke tab sementara dan auto-close;
  - jika dibuka dari klik user, address bar, atau hasil pencarian, diizinkan masuk dengan compatibility mode.
- Proteksi direct image `.jpg/.jpeg/.png/.webp/.gif` tetap aktif agar tab utama tidak berubah menjadi gambar mentah.
- Adblock situs umum tetap dipertahankan; YouTube, Desktop/Mobile, Night Mode, Download Manager, dan kontrol video tidak disentuh.
