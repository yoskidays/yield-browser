# Yield Browser v0.9.83

Source revisi Yield Browser v0.9.83.

Fokus update: build fix untuk Persistent Tabs Session v0.9.82.

Perbaikan:
- Memperbaiki error compile Java pada writer session tab.
- Literal newline/tab pada `saveTabsSession()` dibuat aman untuk Java source 8.
- Persistent tabs tetap dipertahankan: tab terbuka tersimpan saat aplikasi ditutup dan dipulihkan saat dibuka lagi.
- Baseline stabil v0.9.81 tetap dipertahankan.


## v0.9.84 YouTube auto assistant only

Revisi kecil khusus UI AdBlock dan YouTube:
- Menghapus aksi manual `AdBlock OFF 2 menit` dari UI.
- Tidak menambah toggle baru.
- YouTube auto assistant tetap otomatis: deteksi tombol Skip/Lewati, klik otomatis, lalu resume video utama satu kali bila kepause setelah iklan dilewati.
- Bagian stabil seperti tab session, restore tab, adTab guard, lastSafeUrl, dan ad guard session tidak diubah.

## v0.9.84 YouTube assistant +10s ad forward

Revisi kecil khusus YouTube:
- Saat iklan YouTube terdeteksi, assistant mencoba maju +10 detik secara berkala.
- Jika tombol Skip/Lewati muncul, tombol itu diklik otomatis.
- Jika video utama kepause setelah iklan selesai/dilewati, video dibantu play lagi satu kali.
- Jika tidak ada iklan, video tidak autoplay sendiri.
- Bagian tab session, restore tab, adTab guard, lastSafeUrl, dan filter direct-link tidak diubah.
