# Yield Browser v0.9.65

## Universal Blank Site Compatibility

- Menambahkan **Universal Blank Compatibility Recovery** agar situs yang blank saat AdBlock ON tidak perlu ditambahkan domain satu-satu lagi.
- Jika halaman HTTP/HTTPS selesai dimuat tetapi terdeteksi hampir kosong/blank, Yield akan otomatis:
  - mengaktifkan mode kompatibel untuk host tersebut,
  - memuat ulang halaman **sekali** dengan profile compatibility,
  - dan tetap menjaga popup/redirect iklan lintas-domain masuk ke tab sementara.
- Sistem ini **tidak menyentuh YouTube** dan **tidak mengubah flow adblock situs lain** yang sudah stabil.
- Situs strict compatibility yang sudah ada seperti **lordborg.com** dan **instant-monitor.com** tetap didukung.
- Desktop mode pada situs compatibility tetap mengikuti toggle Desktop ON/OFF yang sudah ada.
- Recovery otomatis diberi cooldown per host/url agar tidak memicu reload loop baru.
