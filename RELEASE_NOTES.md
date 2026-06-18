# Yield Browser v0.9.83

## Persistent Tabs Build Fix

- Memperbaiki error compile `illegal line end in character literal` pada `MainActivity.java`.
- Memperbaiki penulisan separator sesi tab di `saveTabsSession()` menjadi Java-safe: `\n` dan `\t`.
- Persistent tabs dari v0.9.82 tetap aktif:
  - tab normal tetap terbuka setelah aplikasi ditutup/dibuka lagi,
  - tab privat tetap dipulihkan sebagai privat dan tidak masuk riwayat,
  - tab iklan sementara/adTab tidak dipulihkan.
- Tidak mengubah fitur stabil lain dari v0.9.81.
