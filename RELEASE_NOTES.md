# Yield Browser v0.9.83

## Persistent Tabs Build Fix

- Memperbaiki error compile `illegal line end in character literal` pada `MainActivity.java`.
- Memperbaiki penulisan separator sesi tab di `saveTabsSession()` menjadi Java-safe: `\n` dan `\t`.
- Persistent tabs dari v0.9.82 tetap aktif:
  - tab normal tetap terbuka setelah aplikasi ditutup/dibuka lagi,
  - tab privat tetap dipulihkan sebagai privat dan tidak masuk riwayat,
  - tab iklan sementara/adTab tidak dipulihkan.
- Tidak mengubah fitur stabil lain dari v0.9.81.

## v0.9.84 YouTube auto assistant only
- Removed the manual UI action `AdBlock OFF 2 menit`.
- YouTube remains automatic: it only watches for Skip/Lewati ads, clicks them, then resumes the main video once if YouTube leaves it paused.
- No manual 2-minute pause/off action is exposed in the UI.
- Stable tab session, restore tab, ad-tab guard, lastSafeUrl, and existing YouTube resume-after-skip logic were left unchanged.

## v0.9.84 YouTube assistant kill switch
- Added a YouTube-only kill switch so when AdBlock Premium is turned OFF, any already-injected YouTube Auto Assistant stops immediately without requiring a reload.
- YouTube assistant remains passive: no autoplay on first open, one-shot resume only after Skip/Lewati is clicked.
- Removed no UI toggle or session logic changes; stable tab session/ad tab guard remains untouched.

## v0.9.84 YouTube assistant +10s ad forward
- YouTube assistant now attempts a safe +10s forward only while a YouTube ad is detected.
- If Skip/Lewati appears, it is clicked first instead of forwarding.
- If the main video is paused after the ad is skipped, it resumes once.
- No autoplay is triggered when no YouTube ad is detected.
- Stable tab session, restore tab, adTab guard, lastSafeUrl, and direct-link filtering were not changed.
