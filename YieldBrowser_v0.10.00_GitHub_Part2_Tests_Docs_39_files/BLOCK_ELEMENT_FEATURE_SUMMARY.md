# Block Element — Element Picker manual (gaya uBlock/Brave)

Fitur baru: pengguna bisa **mengetuk iklan/elemen yang lolos AdBlock** lalu
menyembunyikannya secara permanen per situs. Murni **cosmetic filtering** —
**tidak ada perubahan pada network layer** (`shouldInterceptRequest`,
`buildBlockedResponse`, blocklist host) sehingga risiko regresi minimal. Mesin
AdBlock yang sudah stabil (YouTube Safe AdBlock V6, popup→temp-tab, click-hijack,
deteksi download-intent, compatibility mode) **tidak diubah**.

## Cara pakai

1. Buka menu **⋮ → "Blokir elemen"**.
2. Banner "Mode Blokir Elemen" muncul di atas; ketuk iklan/elemen yang ingin disembunyikan.
3. Elemen tersorot (kotak oranye) dan muncul **dialog Android** "Blokir elemen ini?"
   berisi selektor + pratinjau, dengan tombol:
   - **Blokir** — sembunyikan sekarang & simpan permanen untuk domain ini.
   - **Naik 1 induk** — perlebar pilihan ke elemen induk (untuk iklan yang dibungkus
     container), lalu dialog tampil ulang dengan selektor baru.
   - **Batal** — keluar dari mode pilih.
4. Kelola via **⋮ → "Filter situs ini"**: lihat/hapus per-selektor atau
   "Hapus semua filter situs ini".

## Cara kerja

- **Picker** (`buildElementPickerJs`): overlay JS yang menyorot elemen di bawah
  sentuhan, menelan klik agar tautan iklan tidak ikut terbuka, lalu menghitung
  **CSS selector stabil**: prioritas `#id` unik → tag + maksimal 2 kelas non-acak
  (kelas hash/CSS-module dibuang) → jalur `:nth-of-type` hingga 5 level. Selektor
  & pratinjau dikirim ke Android via `AdBlockBridge.onElementPicked`.
- **Penyimpanan** (`PREFS`, tanpa dependensi baru): per host disimpan sebagai
  `StringSet` kunci `ef_<host>`, dengan indeks host di `ef_hosts`. Mengikuti pola
  `putStringSet` yang sudah dipakai project.
- **Penerapan ulang** (`applyUserFiltersForCurrentPage`): satu
  `<style id="yield-user-filters">` idempoten diinject **lebih awal di
  `onPageCommitVisible`** (hide-before-paint, tanpa kedip), dengan jaring pengaman
  di `onPageFinished` untuk halaman SPA. Saat sebuah filter dihapus, isi stylesheet
  diperbarui (atau dikosongkan) tanpa reload.

## Keamanan & batasan

- `<video>`, `<html>`, `<body>` tidak pernah disembunyikan.
- Halaman dalam **compatibility mode** dikecualikan dari injeksi (konsisten dengan
  guard yang ada) agar situs anti-adblock tidak rusak/loop.
- Picker hanya aktif di halaman `http/https`.

## Berkas yang diubah

- `app/src/main/java/com/yieldbrowser/app/MainActivity.java`
  - Field state filter + status picker.
  - `AdBlockBridge`: `onElementPicked`, `onPickerExited`.
  - Dua entri menu di `showQuickMenu` ("Blokir elemen", "Filter situs ini").
  - Hook penerapan filter di `onPageCommitVisible` & `onPageFinished`.
  - Blok helper: persistensi, injektor CSS, picker JS, dialog konfirmasi,
    manajer filter.
- `app/src/main/res/drawable/ic_block_element.xml` (ikon crosshair, baru).

## Validasi

- `MainActivity.java` lolos parse `javalang`; tidak ada duplikasi signature method.
- Picker JS dan seluruh injektor CSS (apply/clear/parent/commit) lolos `node --check`.
- Vector drawable baru lolos parse XML.
- Build Gradle penuh + uji perangkat tetap perlu dijalankan via GitHub Actions
  (Android SDK tidak tersedia di lingkungan editing ini).
