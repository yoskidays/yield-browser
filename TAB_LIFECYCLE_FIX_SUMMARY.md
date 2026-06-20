# YieldBrowser v0.9.86 — Tab Lifecycle & Home Reset Fix

## Masalah yang diperbaiki

1. URL tab privat dapat menjadi fallback global setelah tab ditutup, lalu memengaruhi tab umum.
2. Tab privat ikut diserialisasi ke sesi dan dapat muncul kembali setelah proses aplikasi dibuat ulang.
3. Callback WebView yang terlambat masih dapat menulis state setelah WebView/tab ditutup.
4. Penutupan tab bergantung pada indeks daftar yang mudah berubah.
5. Tombol Home hanya menyembunyikan WebView sehingga URL lama dapat muncul kembali.
6. Delayed navigation/compatibility work dari tab lama dapat berjalan pada tab pengganti.

## Perbaikan

- Setiap tab memiliki ID stabil, status closed, dan WebView generation token.
- WebView diikat ke pemilik tab melalui `WebViewBinding`; callback stale ditolak.
- Konteks global URL/trust/reload guard disinkronkan ulang pada setiap aktivasi tab.
- Penutupan dan pemilihan tab memakai identitas objek, bukan indeks UI yang dapat stale.
- Tab privat dan tab iklan tidak pernah dipersistenkan; data sesi lama yang mengandung tab privat dimigrasikan dengan aman.
- Private WebView dibersihkan dari history/form/SSL state sebelum dihancurkan tanpa membersihkan cache/cookie global milik tab umum.
- Tombol Home sekarang menghapus URL, safe URL, WebView state, history stack, trust state, dan WebView tab tersebut.
- Delayed translate/night-mode/browser-mode work dibatalkan saat tab berganti atau ditutup.
- Compatibility recovery dan blocked-navigation recovery terikat pada tab, WebView, dan generation yang sama.

## Perilaku setelah perbaikan

- Menekan Android Home lalu kembali ke aplikasi mempertahankan tab privat hanya selama proses aplikasi masih hidup.
- Jika proses aplikasi mati atau aplikasi dibuka ulang dari proses baru, tab privat tidak dipulihkan.
- Menutup tab privat tidak mengubah URL, title, state, atau WebView tab umum.
- Menekan tombol Home Yield mengubah tab aktif menjadi tab Home kosong; berpindah tab atau membuka aplikasi kembali tidak memuat URL lama.
