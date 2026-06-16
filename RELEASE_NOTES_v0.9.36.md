# Yield Browser v0.9.36

## Fix utama
- Memperbaiki Mobile/Desktop Mode yang masih tampak seperti desktop walau Desktop Mode OFF.
- Mobile mode sekarang memakai user-agent mobile eksplisit.
- Saat keluar dari Desktop Mode, WebView melakukan hard reset singkat lewat about:blank agar DOM/viewport desktop lama tidak ikut terbawa.
- Mobile mode dipaksa memakai viewport `width=device-width` dan scale normal.
- Desktop viewport tidak lagi mengubah class/id HTML halaman, sehingga tidak merusak layout mobile situs seperti Google.

## Catatan test
- Test Google search saat Desktop Mode OFF.
- Test Desktop Mode ON lalu OFF kembali.
- Test Landscape Video agar tidak mengubah mode desktop/mobile.
