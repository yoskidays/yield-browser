from pathlib import Path

ROOT = Path("app/src/main/java/com/yieldbrowser/app")
BRIDGE = ROOT / "AdBlockBridge.java"
PART2 = ROOT / "ShieldScriptPartTwo.java"
PART3 = ROOT / "ShieldScriptPartThree.java"
STATE = ROOT / "YieldActivityState.java"
POLICY = ROOT / "TrustedDownloadPopupPolicy.java"
MAIN = ROOT / "MainActivity.java"


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Unexpected {label} anchor count: {count}")
    return text.replace(old, new, 1)

bridge = BRIDGE.read_text()
bridge = replace_once(
    bridge,
    """        void onTrustedDownloadGesture();

        void onTrustedDownloadOpen(String url);""",
    """        void onTrustedDownloadGesture(String sourceUrl);

        void onTrustedDownloadOpen(String url, String sourceUrl);""",
    "bridge callback signatures",
)
bridge = replace_once(
    bridge,
    """    public void onTrustedDownloadGesture() {
        callback.onTrustedDownloadGesture();
    }

    @JavascriptInterface
    public void onTrustedDownloadOpen(String url) {
        callback.onTrustedDownloadOpen(url);
    }""",
    """    public void onTrustedDownloadGesture(String sourceUrl) {
        callback.onTrustedDownloadGesture(sourceUrl);
    }

    @JavascriptInterface
    public void onTrustedDownloadOpen(String url, String sourceUrl) {
        callback.onTrustedDownloadOpen(url, sourceUrl);
    }""",
    "bridge javascript methods",
)
BRIDGE.write_text(bridge)

part2 = PART2.read_text()
part2 = replace_once(
    part2,
    "YieldAdBlockBridge.onTrustedDownloadGesture();",
    "YieldAdBlockBridge.onTrustedDownloadGesture(String(location.href||''));",
    "gesture source bridge",
)
PART2.write_text(part2)

part3 = PART3.read_text()
part3 = replace_once(
    part3,
    "YieldAdBlockBridge.onTrustedDownloadOpen(abs(u));return{closed:true,focus:function(){},close:function(){}};",
    "YieldAdBlockBridge.onTrustedDownloadOpen(abs(u),String(location.href||''));return{closed:false,focus:function(){},close:function(){}};",
    "trusted popup live handle",
)
if part3.count("return{closed:false,focus:function(){},close:function(){}}") != 1:
    raise SystemExit("Trusted popup must expose exactly one live handle")
if part3.count("return{closed:true,focus:function(){},close:function(){}}") < 1:
    raise SystemExit("Blocked popup handle must remain closed")
PART3.write_text(part3)

policy = '''package com.yieldbrowser.app;

/** Restricts trusted file-host popups to the source page and a short real-gesture window. */
final class TrustedDownloadPopupPolicy {
    static final long GESTURE_WINDOW_MS = 4000L;

    private TrustedDownloadPopupPolicy() {
    }

    static boolean canOpen(boolean sourceIsDownloadPage,
                           boolean sourceMatchesGesture,
                           boolean targetIsTrustedDownload,
                           long gestureAtMs,
                           long nowMs) {
        if (!sourceIsDownloadPage || !sourceMatchesGesture
                || !targetIsTrustedDownload || gestureAtMs <= 0L) {
            return false;
        }
        long ageMs = nowMs - gestureAtMs;
        return ageMs >= 0L && ageMs <= GESTURE_WINDOW_MS;
    }

    static boolean sameSourcePage(String gestureSource, String callbackSource) {
        String expected = withoutFragment(gestureSource);
        String actual = withoutFragment(callbackSource);
        return !expected.isEmpty() && expected.equals(actual);
    }

    private static String withoutFragment(String value) {
        if (value == null) return "";
        String clean = value.trim();
        int hash = clean.indexOf('#');
        return hash >= 0 ? clean.substring(0, hash) : clean;
    }
}
'''
POLICY.write_text(policy)

state = STATE.read_text()
state = replace_once(
    state,
    """    volatile long lastTrustedDownloadGestureAtMs = 0L;

    void runOnUiThreadIfAlive""",
    """    volatile long lastTrustedDownloadGestureAtMs = 0L;
    volatile String lastTrustedDownloadSourceUrl = "";

    void runOnUiThreadIfAlive""",
    "trusted source state",
)
state = replace_once(
    state,
    """    @Override
    public void onTrustedDownloadGesture() {
        lastTrustedDownloadGestureAtMs = System.currentTimeMillis();
    }

    @Override
    public void onTrustedDownloadOpen(String url) {
        runOnUiThreadIfAlive(() -> openTrustedDownloadPopupIfAllowed(url));
    }

    boolean openTrustedDownloadPopupIfAllowed(String url) {
        String clean = url == null ? "" : url.trim();
        if (clean.length() == 0) return false;
        long now = System.currentTimeMillis();
        boolean allowed = TrustedDownloadPopupPolicy.canOpen(
                ShieldEngineV2.isDownloadPage(getEffectiveCurrentUrl()),
                isTrustedDownloadIntentUrl(clean),
                lastTrustedDownloadGestureAtMs,
                now);
        if (!allowed) return false;

        lastTrustedDownloadGestureAtMs = 0L;
        newTabInCurrentProfile();
        markTrustedMainFrameNavigation(clean);
        prepareTabForMainFrameNavigation(getCurrentTab(), clean);
        if (addressBar != null) addressBar.setText(clean);
        openAddressBarUrl();
        return true;
    }""",
    """    @Override
    public void onTrustedDownloadGesture(String sourceUrl) {
        lastTrustedDownloadSourceUrl = sourceUrl == null ? "" : sourceUrl.trim();
        lastTrustedDownloadGestureAtMs = System.currentTimeMillis();
    }

    @Override
    public void onTrustedDownloadOpen(String url, String sourceUrl) {
        runOnUiThreadIfAlive(() -> openTrustedDownloadPopupIfAllowed(url, sourceUrl));
    }

    boolean openTrustedDownloadPopupIfAllowed(String url) {
        return openTrustedDownloadPopupIfAllowed(url, lastTrustedDownloadSourceUrl);
    }

    boolean openTrustedDownloadPopupIfAllowed(String url, String callbackSourceUrl) {
        String clean = url == null ? "" : url.trim();
        if (clean.length() == 0) return false;
        String gestureSource = lastTrustedDownloadSourceUrl == null
                ? "" : lastTrustedDownloadSourceUrl.trim();
        long now = System.currentTimeMillis();
        boolean allowed = TrustedDownloadPopupPolicy.canOpen(
                ShieldEngineV2.isDownloadPage(gestureSource),
                TrustedDownloadPopupPolicy.sameSourcePage(
                        gestureSource, callbackSourceUrl),
                isTrustedDownloadIntentUrl(clean),
                lastTrustedDownloadGestureAtMs,
                now);
        if (!allowed) return false;

        lastTrustedDownloadGestureAtMs = 0L;
        lastTrustedDownloadSourceUrl = "";
        newTabInCurrentProfile();
        markTrustedMainFrameNavigation(clean);
        prepareTabForMainFrameNavigation(getCurrentTab(), clean);
        if (addressBar != null) addressBar.setText(clean);
        openAddressBarUrl();
        return true;
    }""",
    "trusted popup callbacks",
)
STATE.write_text(state)

main = MAIN.read_text()
if len(main.splitlines()) > 3000:
    raise SystemExit(f"MainActivity target regressed: {len(main.splitlines())} lines")
if main.count("openTrustedDownloadPopupIfAllowed(safeUrl)") != 1:
    raise SystemExit("MainActivity trusted-popup delegation changed unexpectedly")

combined = "\n".join(p.read_text() for p in (BRIDGE, PART2, PART3, STATE, POLICY))
for marker in (
    "onTrustedDownloadGesture(String sourceUrl)",
    "onTrustedDownloadOpen(String url, String sourceUrl)",
    "lastTrustedDownloadSourceUrl",
    "sameSourcePage",
    "closed:false",
):
    if marker not in combined:
        raise SystemExit(f"Missing Stage 103 marker: {marker}")

print(f"MainActivity unchanged at {len(main.splitlines())} lines")
