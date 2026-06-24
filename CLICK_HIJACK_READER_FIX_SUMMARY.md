# Reader-safe Click Hijack Protection — v0.10.04

## Problem

On image-reader sites such as Komiku, enabling **Proteksi Click Hijack** could make Next/Previous Chapter controls appear unresponsive. Two independent capture-phase click guards were active on the same page, and transparent advertising overlays could consume the touch before the legitimate chapter control received it.

## Resolution

1. Reader/content URLs no longer install the older premium click listener. Shield Engine V2 remains active and becomes the single click-hijack authority for those pages.
2. Shield Engine V2 keeps the explicit same-site reader-navigation allow lane for chapter and episode URLs.
3. When a blocked advertising overlay is detected at click time, the engine inspects the DOM stack at the touch coordinates using `elementsFromPoint`.
4. On older engines, it temporarily disables pointer events on the blocked surface and falls back to `elementFromPoint`.
5. If a safe same-site reader control is found underneath, the blocked event is cancelled and the safe destination is opened directly with `location.assign`.
6. Candidate destinations can come from anchors, forms, buttons, `data-href`, `data-url`, `data-link`, `data-next`, `data-prev`, `data-chapter-url`, or simple `location.href`/`location.assign` handlers.

## Protection retained

- Known advertising and tracking hosts remain blocked.
- Dangerous external schemes remain blocked.
- Cheap random cross-site domains with opaque redirect paths remain blocked.
- Same-origin relay endpoints such as `/r/`, `/go/`, `/out/`, and `/redirect/` remain blocked.
- Direct reader assets and legitimate same-site chapter navigation remain allowed.

## Validation

- Pure Java policy harness passed for Komiku chapter 304 to 305.
- Same-origin relay regression remained blocked.
- Generated document-start JavaScript passed `node --check`.
- A mocked DOM runtime test confirmed that an `onclickads.net` overlay click is cancelled and the underlying Komiku next-chapter URL is opened.
