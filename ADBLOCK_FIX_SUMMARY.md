# AdBlock — Bug Fixes & Penyempurnaan (gaya Brave/UC)

Semua perubahan terbatas pada mesin AdBlock di `MainActivity.java`. Fitur stabil
(YouTube Safe AdBlock V6, popup/redirect-to-temp-tab, click-hijack, deteksi
download-intent, compatibility mode) **tidak diubah**.

## Bug yang ditemukan

1. **`setInterval(clean,250)` selamanya + menumpuk.** `injectPremiumAdBlock`
   dipanggil 3x per halaman (onPageFinished, +1800ms, +5200ms) dan IIFE-nya tidak
   punya guard, sehingga setiap pemanggilan menambah interval 250ms baru →
   efektif ~12 eksekusi `clean()` per detik. Ini penyebab utama lag/jank/boros baterai.

2. **Selector cosmetic blanket `[class*=ad-]`, `[id*=ad-]`, `[id^=ad_]`,
   `[class*=ads-]`, `[class*=advert]`.** Cocok-substring "ad-" ikut menyembunyikan
   elemen sah: `download-button`, `upload-zone`, `read-more`, `bread-crumb`,
   `header-bar`, `thread-item`, dll. Inilah yang membuat banyak situs tampak rusak.

3. **Cosmetic hiding baru jalan di `onPageFinished` (terlambat)** dan memakai
   inline-style/`.remove()` alih-alih stylesheet → iklan sempat berkedip muncul
   sebelum dihapus (FOUC). Tidak "smooth" seperti Brave.

4. **Alokasi array per-request.** `new String[]{...}` (~50 + ~35 entri) dibuat di
   **setiap** `shouldInterceptRequest` (ratusan kali per halaman) → tekanan GC →
   jank saat scroll/load.

5. **False-positive substring.** `u.contains("vast")` memblokir `avast.com` & URL
   apa pun berisi "vast"; `u.contains("onclick")` cocok dengan banyak URL/handler sah.

6. **Respons blokir `text/plain` kosong untuk semua tipe** → gambar iklan yang
   diblokir memunculkan ikon "broken image".

## Perbaikan

| # | Bug | Perbaikan |
|---|-----|-----------|
| 1 | Interval menumpuk | Guard idempoten `__yieldAdBlockPremiumV2`; **MutationObserver** (debounce rAF) untuk reaksi instan; satu interval **cerdas 700ms** yang hanya jalan saat ada `<video>` (agar skip iklan video tetap berfungsi). |
| 2 | Selector blanket | Dihapus untuk halaman non-YouTube. Diganti **stylesheet kuratif gaya EasyList**: token-match `[class~='advertisement']`, kelas iklan spesifik (`.ad-banner`, `.ad-slot`, …), iframe iklan by-src, dan id GPT (`div-gpt-ad`, `google_ads`). Tidak ada lagi cocok-substring "ad-". |
| 3 | Berkedip (FOUC) | Stylesheet di-inject **lebih awal** di `onPageCommitVisible` (hide-before-paint) + idempoten (update isi bila berubah). |
| 4 | Alokasi per-request | Blocklist dipindah ke `static final` (`AD_URL_HOST_PATTERNS`, `POPUP_HOST_PATTERNS`). Semantik pencocokan identik, nol alokasi per request. |
| 5 | False-positive | `"onclick"` → `"onclickads"`; `"vast"` → `"/vast"`, `"vast.xml"`, `"=vast"`. |
| 6 | Broken image | `buildBlockedResponse()`: kirim **GIF 1×1 transparan** untuk gambar, body kosong untuk lainnya, dengan status 200 + header CORS/no-store. |

## Sifat perubahan

- **Lebih sedikit false-positive** dari versi lama (situs yang dulu rusak kini normal).
- **Cakupan iklan tetap setara** (adsbygoogle, GPT, kontainer/iframe iklan, jaringan
  popunder, tracker) — hanya lebih presisi.
- **Lebih ringan**: cosmetic via CSS, bukan polling 4×/detik.

## Catatan: validasi

- `MainActivity.java` lolos parse (`javalang`).
- JS hasil `injectPremiumAdBlock` dan injektor CSS lolos `node --check`.
- Build Gradle penuh + uji perangkat tetap perlu dijalankan via GitHub Actions
  (Android SDK tidak tersedia di lingkungan editing ini).

## Saran lanjutan (opsional, tidak dikerjakan agar tetap stabil)

Untuk benar-benar setara EasyList Brave/UC, langkah berikutnya adalah memuat
**filter list jarak jauh (EasyList/EasyPrivacy)** ke dalam `HashSet` host + matcher
pola, dengan pembaruan berkala. Ini perubahan arsitektur lebih besar dan sengaja
ditunda agar versi sekarang tetap stabil.
