# YieldBrowser v0.9.87 — Incognito Profile & Notification Fix Summary

## 1. Architecture

```text
Normal browser task/process
└── MainActivity
    └── Android WebView default profile

Private browser task/process (:incognito)
└── PrivateBrowserActivity
    └── Android WebView data directory: yield_incognito

Shared foreground download service
└── One download-progress notification
```

The private WebView process is created only on Android 9+, where `WebView.setDataDirectorySuffix()` is available. This separates Chromium/WebView site storage at the engine-profile level rather than attempting to toggle the process-global `CookieManager` from individual tabs.

## 2. Incognito guarantees on Android 9+

- Normal and private cookies cannot share the same WebView data directory.
- Local storage, IndexedDB, HTTP auth, service-worker data, and cache are isolated.
- Private tabs and navigation history are not serialized.
- Private browsing history cannot overwrite normal history.
- Private form/autofill state is disabled.
- Third-party cookies are disabled for private WebViews.
- Private profile data is purged on profile startup and Activity destruction.
- Screenshots and Recent Apps previews are blocked.

## 3. Android 6–8 behavior

Android WebView before API 28 has no supported per-process data-directory suffix. Yield therefore retains a non-persistent private-tab fallback but does not claim full engine-storage isolation on those versions.

## 4. Download notification behavior

The old keep-alive notification has been removed. The foreground service now owns one actual download-progress notification. Normal and incognito processes report their active counts to the same service, which prevents either process from prematurely removing the notification or wake lock while another download remains active.

## 5. Files added

- `YieldBrowserApplication.java`
- `PrivateBrowserActivity.java`
- `PrivateProfileCleaner.java`
- `PrivateProfilePolicy.java`
- `PrivateProfilePolicyTest.java`

## 6. Files materially changed

- `MainActivity.java`
- `DownloadKeepAliveService.java`
- `AndroidManifest.xml`
- `app/build.gradle`
- GitHub Actions artifact metadata
