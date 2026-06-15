# Yield Browser v0.4.2 Compile Fix

Update ini memperbaiki error build dari v0.4.1.

## Perbaikan v0.4.2

- Fix error compile:
  `method beginDownloadFromWeb cannot be applied to given types`
- Memperbaiki pemanggilan lama:
  `beginDownloadFromWeb(url, contentDisposition, mimeType)`
- Menambahkan overload kompatibilitas agar pemanggilan 3 argumen tetap aman.
- Fitur v0.4.1 tetap dipertahankan:
  - download auto open menu
  - anti-hotlink safe headers
  - Referer / Origin / Cookie / User-Agent
  - fallback split 2 koneksi ke 1 koneksi
  - detail gagal download
  - reload dari awal
