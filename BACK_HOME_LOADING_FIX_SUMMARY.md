# Back-to-Home Loading Lifecycle Fix — v0.10.03

## Problem

When Android Back reached the browser Home screen, the previous implementation called `stopLoading()` and hid the active `WebView`. Hiding a WebView does not terminate its renderer. JavaScript timers, media, redirects, subresource requests, and delayed callbacks could therefore continue behind Home. The progress bar could also remain visible with stale state.

## Fix

- `onBackPressed()` still calls `WebView.goBack()` when genuine page history exists.
- When no earlier page remains, it now calls `navigateCurrentTabHome()`.
- `navigateCurrentTabHome()` resets the tab navigation state and destroys the owned WebView.
- `destroyTabWebView()` detaches clients and bridges, stops loading, loads `about:blank`, removes the view, and destroys the renderer.
- `showHome()` now resets progress to zero and hides the progress bar.

## Result

Returning to Home through Android Back is now a real lifecycle exit from the current page. The left page cannot continue loading, playing media, redirecting, or updating browser UI behind Home.
