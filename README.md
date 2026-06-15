# Yield Browser v0.1.4 UI Source

Target test: Android 11 / Realme 5 Pro.

Important fix:
- Real APK must be built with Android Studio/Gradle, not manually assembled DEX.
- applicationId: com.yieldbrowser.install
- MainActivity class: com.yieldbrowser.app.MainActivity
- minSdk 23, targetSdk 30.

UI features:
- Lightweight WebView browser shell
- Top toolbar
- Address/search bar
- Drawable/vector icon buttons: back, forward, refresh, go, download, home
- Download button is placeholder; multi-connection downloader will be next feature stage.

Build in Android Studio:
1. Open the `YieldBrowser` folder.
2. Let Gradle sync.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Install generated app-debug.apk.
