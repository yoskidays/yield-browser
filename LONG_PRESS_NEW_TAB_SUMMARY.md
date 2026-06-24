# Long-Press Open Link in New Tab — v0.10.02

## Behavior

1. Press and hold a normal link in a rendered web page.
2. Yield Browser reads the anchor at the last tapped point.
3. A context menu displays **Buka link di tab baru**.
4. Selecting the action creates a new tab, switches to it, and loads the destination.
5. The previous tab remains available with its current page and navigation state.

## Coverage

- Text anchors.
- Images wrapped by anchors.
- Absolute HTTP/HTTPS URLs.
- Relative and root-relative paths.
- Protocol-relative URLs.
- Normal and dedicated private browser profiles.

## Safety and lifecycle controls

- Non-HTTP/HTTPS schemes are rejected.
- The menu is discarded if the originating WebView is no longer active.
- Existing Safe Browsing checks run before the new tab is created.
- URL resolution is isolated in `LongPressLinkPolicy` and covered by unit tests.
