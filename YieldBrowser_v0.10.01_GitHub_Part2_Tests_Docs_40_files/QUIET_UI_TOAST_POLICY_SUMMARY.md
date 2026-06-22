# YieldBrowser v0.9.98 — Quiet UI Toast Policy

## Objective

Reduce visual interruptions during normal browsing. Routine state changes and
automatic recovery actions no longer display Toast messages.

## Suppressed notifications

- Tab opened, selected, or closed
- AdBlock, Reader Mode, video controls, desktop mode, and Night Mode state changes
- HTTPS-First enabled/disabled, HTTPS upgrade, and automatic HTTP fallback
- Cache cleared, bookmark added/removed, link copied, and other successful actions
- Download queued, paused, resumed, prioritized, or completed
- Automatic ad-tab closure, compatibility recovery, and reload-loop prevention
- Search result counts, quality/speed changes, translation state, and similar status text

## Critical messages retained

Only errors that normally require the user to take action remain eligible, including:

- Invalid URL
- Required camera permission or camera open failure
- Failure to open the private/general profile
- File missing, share/rename/open failure
- Download start/action failure
- History deletion failure
- Folder picker unavailable
- Player open failure

The policy is centralized in `QuietToast.java`, so future routine Toast calls are
suppressed by default without scattering conditional checks throughout the browser.
