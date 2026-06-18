# Yield Browser v0.9.82

## Persistent Tab Session

- Menambahkan penyimpanan sesi tab ke SharedPreferences.
- Tab normal dan tab privat yang masih terbuka akan tetap muncul lagi setelah aplikasi ditutup/dibuka ulang.
- Active tab terakhir dipulihkan.
- URL dan judul tab disimpan ringan; WebView aktif akan dimuat ulang saat app dibuka kembali.
- Tab iklan sementara (`adTab`) tidak ikut dipulihkan agar popup/redirect iklan tidak kembali muncul setelah restart.
- Saat tab ditutup manual, session langsung disimpan sehingga tab tersebut tidak muncul lagi setelah app dibuka ulang.
- Tetap mempertahankan baseline stabil v0.9.81:
  - isolated WebView per tab,
  - WebView thread crash fix,
  - compatibility site stabil,
  - YouTube skip/resume,
  - kontrol video,
  - history/private/tab fix,
  - desktop/mobile/night mode/download.
