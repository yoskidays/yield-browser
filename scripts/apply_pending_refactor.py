from pathlib import Path

STATE = Path("app/src/main/java/com/yieldbrowser/app/YieldActivityState.java")
MAIN = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")

state = STATE.read_text()
main = MAIN.read_text()

state_anchor = """    final Handler mainHandler = new Handler(Looper.getMainLooper());
    final AtomicBoolean downloadUiRefreshPosted = new AtomicBoolean(false);"""
state_replacement = """    final Handler mainHandler = new Handler(Looper.getMainLooper());
    final LifecycleCallbackGate lifecycleCallbackGate = new LifecycleCallbackGate();
    final AtomicBoolean downloadUiRefreshPosted = new AtomicBoolean(false);"""
if state.count(state_anchor) != 1:
    raise SystemExit("Unexpected lifecycle field anchor count")
state = state.replace(state_anchor, state_replacement, 1)

callback_count = main.count("runOnUiThread(() ->")
if callback_count != 10:
    raise SystemExit(f"Expected 10 bridge UI callbacks, found {callback_count}")
main = main.replace("runOnUiThread(() ->", "runOnUiThreadIfAlive(() ->")

callback_heading = """// ===== WebView JavaScript bridge callbacks =====
    // The JS bridges"""
helper = """// ===== WebView JavaScript bridge callbacks =====
    void runOnUiThreadIfAlive(Runnable action) {
        if (action == null || !lifecycleCallbackGate.isActive()) return;
        runOnUiThread(() -> {
            if (lifecycleCallbackGate.isActive()) action.run();
        });
    }

    // The JS bridges"""
if main.count(callback_heading) != 1:
    raise SystemExit("Missing JavaScript callback heading")
main = main.replace(callback_heading, helper, 1)

translate_anchor = """    public void translateText(int index, String text) {
        if (text == null) return;"""
translate_replacement = """    public void translateText(int index, String text) {
        if (!lifecycleCallbackGate.isActive() || text == null) return;"""
if main.count(translate_anchor) != 1:
    raise SystemExit("Missing translate lifecycle guard anchor")
main = main.replace(translate_anchor, translate_replacement, 1)

create_anchor = """        super.onCreate(savedInstanceState);
        dedicatedPrivateProfile = useDedicatedPrivateProfile();"""
create_replacement = """        super.onCreate(savedInstanceState);
        lifecycleCallbackGate.markActive();
        dedicatedPrivateProfile = useDedicatedPrivateProfile();"""
if main.count(create_anchor) != 1:
    raise SystemExit("Missing onCreate lifecycle anchor")
main = main.replace(create_anchor, create_replacement, 1)

folder_anchor = """            Uri treeUri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            selectedDownloadTreeUri = treeUri.toString();"""
folder_replacement = """            Uri treeUri = data.getData();
            int takeFlags = PersistableUriPermissionPolicy.takeFlags(
                    data.getFlags(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (!PersistableUriPermissionPolicy.hasAccess(takeFlags)) {
                QuietToast.makeText(this, "Izin folder tidak diberikan",
                        QuietToast.LENGTH_SHORT).show();
                return;
            }
            try {
                getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
            } catch (Exception e) {
                QuietToast.makeText(this, "Izin folder tidak dapat disimpan",
                        QuietToast.LENGTH_SHORT).show();
                return;
            }
            selectedDownloadTreeUri = treeUri.toString();"""
if main.count(folder_anchor) != 1:
    raise SystemExit("Missing download folder permission anchor")
main = main.replace(folder_anchor, folder_replacement, 1)

destroy_anchor = """    protected void onDestroy() {
        downloadUiTickerRunning = false;
        mainHandler.removeCallbacks(downloadUiTicker);"""
destroy_replacement = """    protected void onDestroy() {
        lifecycleCallbackGate.markDestroyed();
        downloadUiTickerRunning = false;
        mainHandler.removeCallbacksAndMessages(null);"""
if main.count(destroy_anchor) != 1:
    raise SystemExit("Missing onDestroy lifecycle anchor")
main = main.replace(destroy_anchor, destroy_replacement, 1)

if main.count("runOnUiThreadIfAlive(() ->") != 10:
    raise SystemExit("Not all bridge callbacks use the lifecycle gate")
if main.count("runOnUiThread(() ->") != 1:
    raise SystemExit("Unexpected direct runOnUiThread calls after helper insertion")
if "selectedDownloadTreeUri = treeUri.toString();" not in main:
    raise SystemExit("Folder URI persistence assignment was lost")
if len(main.splitlines()) > 3000:
    raise SystemExit(f"MainActivity target regressed: {len(main.splitlines())} lines")

STATE.write_text(state)
MAIN.write_text(main)
print(f"MainActivity final line count: {len(main.splitlines())}")
