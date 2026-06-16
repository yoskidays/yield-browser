# Yield Browser v0.9.32

Perbaikan fokus dari laporan testing:

- Memperbaiki penyimpanan Pintasan/Home Shortcut.
  - Shortcut sekarang disimpan dengan baris normal, bukan teks literal `\\n`.
  - Data lama yang sudah terlanjur tersimpan dengan format lama otomatis dimigrasikan.
  - Menambah shortcut tidak lagi menghapus shortcut lain setelah aplikasi ditutup/dibuka ulang.

- Memperbaiki Desktop Mode.
  - Toggle mobile/desktop tidak memakai reload mentah lagi.
  - Browser memilih URL aman sebelum memuat ulang halaman.
  - Redirect iklan seperti `invest-tracing.com` dicegah agar tidak menggantikan halaman utama.
  - Jika URL aktif mencurigakan, browser kembali ke URL aman atau halaman home.

- Memperbaiki Hapus Semua Histori.
  - Histori benar-benar dikosongkan dari SharedPreferences dan file history.
  - Halaman web aktif ditutup ke `about:blank`, lalu kembali ke Home.
  - WebView history/back-forward juga dibersihkan.
  - Setelah hapus histori, situs yang masih terbuka tidak langsung tercatat ulang.
