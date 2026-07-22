# Stable APK Updates and Bookmark Migration

Yield Browser uses the fixed package name `com.yieldbrowser.install`. Android can install a newer APK over an older APK without deleting application data only when both APK files use the same signing certificate and the newer APK has an equal or higher `versionCode`.

## One-time stable signing setup

Create one keystore and keep it permanently. Do not regenerate it for later releases.

```bash
keytool -genkeypair -v \
  -keystore yield-release.jks \
  -alias yield \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Convert the keystore to Base64.

Linux:

```bash
base64 -w 0 yield-release.jks > yield-release.jks.base64.txt
```

Windows PowerShell:

```powershell
[Convert]::ToBase64String(
  [IO.File]::ReadAllBytes("yield-release.jks")
) | Set-Content -NoNewline "yield-release.jks.base64.txt"
```

In GitHub, open **Settings > Secrets and variables > Actions** and create these repository secrets:

- `YIELD_KEYSTORE_BASE64`: contents of `yield-release.jks.base64.txt`
- `YIELD_KEYSTORE_PASSWORD`: keystore password
- `YIELD_KEY_ALIAS`: `yield`, or the alias selected when the key was created
- `YIELD_KEY_PASSWORD`: key password

After all four secrets exist, the workflow produces:

- `YieldBrowser-stable-release-apk`: normal APK for daily use and future updates
- `YieldBrowser-bookmark-migration-apk`: temporary debuggable APK used only to restore data from an older debug build
- `YieldBrowser-debug-apk`: development and testing build, not recommended for daily installation

Always install future versions from `YieldBrowser-stable-release-apk`. Never change the package name or signing key.

## Updating without uninstalling

Try the normal update first:

```bash
adb install -r app-release.apk
```

You can also open the APK directly from Android and choose **Update**. When the signing certificate matches, bookmarks, settings, tabs, and history remain in place.

## Migrating bookmarks from an older debug APK

Older GitHub Actions debug APK files may have different signing certificates. When Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE` or a package conflict, use this one-time migration before uninstalling the old APK.

### 1. Save the old application preferences

Connect the Android device with USB debugging enabled. Keep the old APK installed and run:

Windows Command Prompt:

```bat
adb exec-out run-as com.yieldbrowser.install cat shared_prefs/yield_browser_prefs.xml > yield_browser_prefs.xml
```

Linux or macOS:

```bash
adb exec-out run-as com.yieldbrowser.install cat shared_prefs/yield_browser_prefs.xml > yield_browser_prefs.xml
```

Confirm that `yield_browser_prefs.xml` exists and is not empty. This file contains bookmarks and normal browser settings. Keep it private because it may also contain browsing preferences.

### 2. Remove the old APK and install the migration APK

```bash
adb uninstall com.yieldbrowser.install
adb install app-migration.apk
```

Do not open Yield Browser yet.

### 3. Restore the preferences

Copy the backup to the phone:

```bash
adb push yield_browser_prefs.xml /sdcard/Download/yield_browser_prefs.xml
```

Stop the application and restore the file:

```bash
adb shell am force-stop com.yieldbrowser.install
adb shell "run-as com.yieldbrowser.install sh -c 'mkdir -p shared_prefs && cat /sdcard/Download/yield_browser_prefs.xml > shared_prefs/yield_browser_prefs.xml && chmod 600 shared_prefs/yield_browser_prefs.xml'"
```

Open the migration APK and confirm that the bookmarks are present.

### 4. Replace the migration APK with the normal release APK

Both APK files use the same stable signing key, so the normal release can replace the migration APK without deleting restored data.

```bash
adb install -r app-release.apk
```

After this one-time transition, future stable release APK files can be installed normally without uninstalling the previous version.

## Important safeguards

- Back up the keystore and passwords in at least two secure locations.
- Never commit the keystore or passwords to this public repository.
- Do not use `YieldBrowser-debug-apk` for routine updates.
- Increase `versionCode` for every release.
- Test installation with `adb install -r` before deleting any existing APK.
