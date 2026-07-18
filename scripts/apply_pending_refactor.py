from pathlib import Path

STATE = Path("app/src/main/java/com/yieldbrowser/app/YieldActivityState.java")
MAIN = Path("app/src/main/java/com/yieldbrowser/app/MainActivity.java")
DOWNLOAD = Path("app/src/main/java/com/yieldbrowser/app/YieldDownloadActivity.java")

state = STATE.read_text()
main = MAIN.read_text()
download = DOWNLOAD.read_text()

field_anchor = """    final Handler mainHandler = new Handler(Looper.getMainLooper());
    final LifecycleCallbackGate lifecycleCallbackGate = new LifecycleCallbackGate();
    final AtomicBoolean downloadUiRefreshPosted = new AtomicBoolean(false);"""
state_replacement = """    final Handler mainHandler = new Handler(Looper.getMainLooper());
    final LifecycleCallbackGate lifecycleCallbackGate = new LifecycleCallbackGate();
    final AtomicBoolean downloadUiRefreshPosted = new AtomicBoolean(false);

    void runOnUiThreadIfAlive(Runnable action) {
        if (action == null || !lifecycleCallbackGate.isActive()) return;
        runOnUiThread(() -> {
            if (lifecycleCallbackGate.isActive()) action.run();
        });
    }"""
if state.count(field_anchor) != 1:
    raise SystemExit("Unexpected shared lifecycle field anchor")
state = state.replace(field_anchor, state_replacement, 1)

main_helper = """    void runOnUiThreadIfAlive(Runnable action) {
        if (action == null || !lifecycleCallbackGate.isActive()) return;
        runOnUiThread(() -> {
            if (lifecycleCallbackGate.isActive()) action.run();
        });
    }

"""
if main.count(main_helper) != 1:
    raise SystemExit("Unexpected MainActivity lifecycle helper count")
main = main.replace(main_helper, "", 1)

ui_callback_count = download.count("runOnUiThread(() ->")
if ui_callback_count != 3:
    raise SystemExit(f"Expected 3 direct download UI callbacks, found {ui_callback_count}")
download = download.replace("runOnUiThread(() ->", "runOnUiThreadIfAlive(() ->")

if download.count("runOnUiThread(() ->") != 0:
    raise SystemExit("Direct download UI callback remains")
if download.count("runOnUiThreadIfAlive(() ->") != 3:
    raise SystemExit("Download lifecycle callback replacement incomplete")
if len(main.splitlines()) > 3000:
    raise SystemExit(f"MainActivity target regressed: {len(main.splitlines())} lines")

STATE.write_text(state)
MAIN.write_text(main)
DOWNLOAD.write_text(download)
print(f"MainActivity final line count: {len(main.splitlines())}")
