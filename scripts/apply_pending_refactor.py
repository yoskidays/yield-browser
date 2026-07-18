from pathlib import Path

ROOT = Path("app/src/main/java/com/yieldbrowser/app")
MAIN = ROOT / "MainActivity.java"
STATE = ROOT / "YieldActivityState.java"
BRIDGE = ROOT / "AdBlockBridge.java"
BUILDER = ROOT / "ShieldScriptBuilder.java"
PART1 = ROOT / "ShieldScriptPartOne.java"
PART2 = ROOT / "ShieldScriptPartTwo.java"
PART3 = ROOT / "ShieldScriptPartThree.java"


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Unexpected {label} anchor count: {count}")
    return text.replace(old, new, 1)

# Bridge: carry a real download-control gesture and the trusted popup target to native code.
bridge = BRIDGE.read_text()
bridge = replace_once(
    bridge,
    """        void onAdRedirect(String url);\n\n        void onElementPicked(String selector, String preview);""",
    """        void onAdRedirect(String url);\n\n        void onTrustedDownloadGesture();\n\n        void onTrustedDownloadOpen(String url);\n\n        void onElementPicked(String selector, String preview);""",
    "bridge callback methods",
)
bridge = replace_once(
    bridge,
    """    @JavascriptInterface\n    public void onAdRedirect(String url) {\n        callback.onAdRedirect(url);\n    }\n\n    @JavascriptInterface\n    public void onElementPicked""",
    """    @JavascriptInterface\n    public void onAdRedirect(String url) {\n        callback.onAdRedirect(url);\n    }\n\n    @JavascriptInterface\n    public void onTrustedDownloadGesture() {\n        callback.onTrustedDownloadGesture();\n    }\n\n    @JavascriptInterface\n    public void onTrustedDownloadOpen(String url) {\n        callback.onTrustedDownloadOpen(url);\n    }\n\n    @JavascriptInterface\n    public void onElementPicked""",
    "bridge javascript methods",
)
BRIDGE.write_text(bridge)

builder = BUILDER.read_text()
builder = replace_once(
    builder,
    "var S=W.__yieldShieldV2State||{installed:false,observer:null,timer:0};",
    "var S=W.__yieldShieldV2State||{installed:false,observer:null,timer:0,downloadClickUntil:0};",
    "shield state",
)
BUILDER.write_text(builder)

part1 = PART1.read_text()
part1 = replace_once(
    part1,
    r"(?:^|[ _-])(download|unduh|save|mirror|server|quality|resolution|batch|480p|720p|1080p|2160p)(?:[ _-]|$)",
    r"(?:^|[ _-])(download|unduh|save|mirror|server|quality|resolution|batch|gofile|mediafire|google[ _-]*drive|drive|mega|pixeldrain|dropbox|onedrive|480p|720p|1080p|2160p)(?:[ _-]|$)",
    "download control labels",
)
part1 = replace_once(
    part1,
    "return downloadControl(node)&&trustedDownloadHost(h);",
    "return trustedDownloadHost(h)&&(downloadControl(node)||Date.now()<=(S.downloadClickUntil||0));",
    "trusted popup download window",
)
PART1.write_text(part1)

part2 = PART2.read_text()
part2 = replace_once(
    part2,
    """                + "function clickSurfaceSuspicious(node){""",
    """                + "function rememberDownloadClick(node){try{if(!downloadPage()||!downloadControl(node))return false;S.downloadClickUntil=Date.now()+4000;if(W.YieldAdBlockBridge&&YieldAdBlockBridge.onTrustedDownloadGesture)YieldAdBlockBridge.onTrustedDownloadGesture();return true;}catch(e){return false;}}"\n                + "function clickSurfaceSuspicious(node){""",
    "download gesture helper",
)
part2 = replace_once(
    part2,
    "function clickGuard(e){if(!S.config.enabled||!S.config.click||S.readerNavLock)return;try{var blocked=",
    "function clickGuard(e){if(!S.config.enabled||!S.config.click||S.readerNavLock)return;try{rememberDownloadClick(e&&e.target);var blocked=",
    "click guard gesture capture",
)
PART2.write_text(part2)

part3 = PART3.read_text()
part3 = replace_once(
    part3,
    """        return "function overlayBad(el){""",
    r'''        return "function fakeRewardAd(el){try{if(!downloadPage()||!visible(el))return false;var t=low((el.innerText||el.textContent||'').replace(/\\s+/g,' ')).trim();if(t.length<20||t.length>700)return false;var n=0;if(/get\\s*paid\\s*in\\s*\\d+\\s*minutes?/i.test(t))n++;if(/subscribe\\s*(?:&|and)?\\s*watch.*receive\\s*payment/i.test(t))n++;if(/\\badd\\s*cash\\b/i.test(t)&&/\\bcash\\s*out\\b/i.test(t))n++;if(/\\$\\s*[\\d,.]+\\s*available/i.test(t))n++;return n>=2;}catch(e){return false;}}"
                + "function rewardVictim(el){try{var v=el;for(var i=0;i<4&&v&&v.parentElement;i++){var p=v.parentElement;if(!fakeRewardAd(p))break;v=p;}return v;}catch(e){return el;}}"
                + "function overlayBad(el){''',
    "inline reward ad classifier",
)
part3 = replace_once(
    part3,
    "function clean(){if(!S.config.enabled)return;var removed=false;try{if(S.config.resource)",
    "function clean(){if(!S.config.enabled)return;var removed=false;try{var rw=D.querySelectorAll('div,section,aside,figure,a,p');for(var ri=rw.length-1;ri>=0;ri--){var re=rw[ri];if(!re||!re.isConnected)continue;if(fakeRewardAd(re)){var victim=rewardVictim(re);if(victim&&victim.isConnected){victim.remove();removed=true;}}}}catch(e){}try{if(S.config.resource)",
    "inline reward ad cleanup",
)
part3 = replace_once(
    part3,
    "try{var nativeOpen=W.open;W.open=function(u,n,f){if(S.config.enabled&&S.config.popup&&badNav(u,null)){report(u);return{closed:true,focus:function(){},close:function(){}};}try{return nativeOpen.call(W,u,n,f);}catch(e){return null;}};}catch(e){}",
    "try{var nativeOpen=W.open;W.open=function(u,n,f){if(S.config.enabled&&Date.now()<=(S.downloadClickUntil||0)&&safeDownloadNav(u,null)){try{if(W.YieldAdBlockBridge&&YieldAdBlockBridge.onTrustedDownloadOpen){S.downloadClickUntil=0;YieldAdBlockBridge.onTrustedDownloadOpen(abs(u));return{closed:true,focus:function(){},close:function(){}};}}catch(x){}}if(S.config.enabled&&S.config.popup&&badNav(u,null)){report(u);return{closed:true,focus:function(){},close:function(){}};}try{return nativeOpen.call(W,u,n,f);}catch(e){return null;}};}catch(e){}",
    "trusted window open bridge",
)
PART3.write_text(part3)

state = STATE.read_text()
state = replace_once(
    state,
    """    final AtomicBoolean downloadUiRefreshPosted = new AtomicBoolean(false);\n\n    void runOnUiThreadIfAlive""",
    """    final AtomicBoolean downloadUiRefreshPosted = new AtomicBoolean(false);\n    volatile long lastTrustedDownloadGestureAtMs = 0L;\n\n    void runOnUiThreadIfAlive""",
    "trusted gesture state",
)
state = replace_once(
    state,
    """    void runOnUiThreadIfAlive(Runnable action) {\n        if (action == null || !lifecycleCallbackGate.isActive()) return;\n        runOnUiThread(() -> {\n            if (lifecycleCallbackGate.isActive()) action.run();\n        });\n    }\n\n    // ===== History Engine V2 =====""",
    """    void runOnUiThreadIfAlive(Runnable action) {\n        if (action == null || !lifecycleCallbackGate.isActive()) return;\n        runOnUiThread(() -> {\n            if (lifecycleCallbackGate.isActive()) action.run();\n        });\n    }\n\n    @Override\n    public void onTrustedDownloadGesture() {\n        lastTrustedDownloadGestureAtMs = System.currentTimeMillis();\n    }\n\n    @Override\n    public void onTrustedDownloadOpen(String url) {\n        runOnUiThreadIfAlive(() -> openTrustedDownloadPopupIfAllowed(url));\n    }\n\n    boolean openTrustedDownloadPopupIfAllowed(String url) {\n        String clean = url == null ? "" : url.trim();\n        if (clean.length() == 0) return false;\n        long now = System.currentTimeMillis();\n        boolean allowed = TrustedDownloadPopupPolicy.canOpen(\n                ShieldEngineV2.isDownloadPage(getEffectiveCurrentUrl()),\n                isTrustedDownloadIntentUrl(clean),\n                lastTrustedDownloadGestureAtMs,\n                now);\n        if (!allowed) return false;\n\n        lastTrustedDownloadGestureAtMs = 0L;\n        newTabInCurrentProfile();\n        markTrustedMainFrameNavigation(clean);\n        prepareTabForMainFrameNavigation(getCurrentTab(), clean);\n        if (addressBar != null) addressBar.setText(clean);\n        openAddressBarUrl();\n        return true;\n    }\n\n    // ===== History Engine V2 =====""",
    "trusted popup callbacks",
)
STATE.write_text(state)

main = MAIN.read_text()
main = replace_once(
    main,
    """        String safeUrl = url.trim();\n\n        if (!adBlockRedirectToTempTab)""",
    """        String safeUrl = url.trim();\n        if (openTrustedDownloadPopupIfAllowed(safeUrl)) return true;\n\n        if (!adBlockRedirectToTempTab)""",
    "native trusted popup fallback",
)
if len(main.splitlines()) > 3000:
    raise SystemExit(f"MainActivity target regressed: {len(main.splitlines())} lines")
MAIN.write_text(main)

# Guard the intended scope.
for required in (
    "onTrustedDownloadGesture", "onTrustedDownloadOpen", "downloadClickUntil",
    "function fakeRewardAd", "openTrustedDownloadPopupIfAllowed"):
    combined = "\n".join(p.read_text() for p in (BRIDGE, BUILDER, PART1, PART2, PART3, STATE, MAIN))
    if required not in combined:
        raise SystemExit(f"Missing Stage 102 marker: {required}")

print(f"MainActivity final line count: {len(main.splitlines())}")
