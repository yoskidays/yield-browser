#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(relative_path: str, old: str, new: str) -> bool:
    path = ROOT / relative_path
    text = path.read_text(encoding="utf-8")
    if new in text:
        return False
    if old not in text:
        raise RuntimeError(f"Expected source fragment was not found in {relative_path}: {old[:120]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")
    return True


def insert_before(relative_path: str, marker: str, addition: str) -> bool:
    path = ROOT / relative_path
    text = path.read_text(encoding="utf-8")
    if addition in text:
        return False
    if marker not in text:
        raise RuntimeError(f"Insertion marker was not found in {relative_path}: {marker[:120]!r}")
    path.write_text(text.replace(marker, addition + marker, 1), encoding="utf-8")
    return True


def main() -> None:
    changed = []

    path = "app/src/main/java/com/yieldbrowser/app/ShieldScriptBuilder.java"
    if replace_once(
        path,
        "var S=W.__yieldShieldV2State||{installed:false,observer:null,timer:0,downloadClickUntil:0};W.__yieldShieldV2State=S;S.config=C;",
        "var S=W.__yieldShieldV2State||{installed:false,observer:null,timer:0,downloadClickUntil:0,clickIntentUrl:'',clickIntentHost:'',clickSourceHost:'',clickIntentUntil:0};W.__yieldShieldV2State=S;S.config=C;S.clickIntentUrl=S.clickIntentUrl||'';S.clickIntentHost=S.clickIntentHost||'';S.clickSourceHost=S.clickSourceHost||'';S.clickIntentUntil=Number(S.clickIntentUntil||0);",
    ):
        changed.append(path)

    path = "app/src/main/java/com/yieldbrowser/app/ShieldScriptPartOne.java"
    marker = '                + "function relay(u){var p=path(u);return /(?:^|\\/)(?:r|go|out|away|jump|visit|redirect|redir|click|link|external|open|track|tracking|offer|offers|promo|ads?|interstitial)(?:\\/|$)/i.test(p);}"\n'
    addition = (
        '                + "function popupRiskPath(p){return /(?:^|[\\/_-])(?:drama|drakor|subtitle|sub(?:title)?-?indo|series|season|episode|watch|movie|film|stream|player|embed|play)(?:[\\/_-]|$)/i.test(p||\'\');}"\n'
        '                + "function popupRiskPage(){if(searchPage())return false;var h=host(location.href),p=path(location.href);return isolated()||popupRiskPath(p)||/(?:^|\\.)(?:dramaencode\\.net)$/i.test(h||\'\');}"\n'
    )
    if insert_before(path, marker, addition):
        changed.append(path)

    path = "app/src/main/java/com/yieldbrowser/app/ShieldScriptPartTwo.java"
    old_bad = "if(downloadPage())return true;if(isolated())return true;return cheap(h)&&(redirParam(a)||/(?:^|\\/)[a-z0-9_-]{16,}(?:\\/|$)/i.test(path(a)));"
    new_bad = "if(downloadPage())return true;if(isolated())return true;if(popupRiskPage()&&!cleanClickIntent(a,node))return true;return cheap(h)&&(redirParam(a)||/(?:^|\\/)[a-z0-9_-]{16,}(?:\\/|$)/i.test(path(a)));"
    if replace_once(path, old_bad, new_bad):
        changed.append(path)

    marker = '                + "function rememberDownloadClick(node){try{if(!downloadPage()||!downloadControl(node))return false;S.downloadClickUntil=Date.now()+4000;if(W.YieldAdBlockBridge&&YieldAdBlockBridge.onTrustedDownloadGesture)YieldAdBlockBridge.onTrustedDownloadGesture(String(location.href||\'\'));return true;}catch(e){return false;}}"\n'
    addition = (
        '                + "function cleanClickIntent(u,node){try{var a=abs(u),h=host(a),c=host(location.href);if(!/^https?:/i.test(a)||!h||adHost(h)||hardToken(a)||redirParam(a)||relay(a))return false;if(cheap(h)&&(!c||!same(h,c)||/(?:^|\\/)[a-z0-9_-]{16,}(?:\\/|$)/i.test(path(a))))return false;var m=navMeta(node);if(/(?:^|[ _-])(ad|ads|advert|sponsor|promo|affiliate|popup|popunder|interstitial|clickunder)(?:[ _-]|$)/i.test(m))return false;if(popupRiskPage()&&c&&!same(h,c)&&!contentRegion(node)&&!downloadControl(node))return false;return true;}catch(e){return false;}}"\n'
        '                + "function rememberClickIntent(e,node){try{if(!popupRiskPage()||!e||e.isTrusted===false)return false;var u=candidateUrl(node),a=u?abs(u):\'\',h=host(a),c=host(location.href);S.clickSourceHost=c;S.clickIntentUrl=\'\';S.clickIntentHost=\'\';S.clickIntentUntil=Date.now()+1600;if(a&&cleanClickIntent(a,node)){S.clickIntentUrl=a;S.clickIntentHost=h;}return true;}catch(x){return false;}}"\n'
        '                + "function clickIntentActive(){return popupRiskPage()&&Date.now()<=(S.clickIntentUntil||0);}"\n'
        '                + "function matchesClickIntent(u){try{if(!clickIntentActive()||!S.clickIntentUrl)return false;var a=abs(u),h=host(a);if(a===S.clickIntentUrl)return true;return !!h&&!!S.clickIntentHost&&same(h,S.clickIntentHost);}catch(e){return false;}}"\n'
        '                + "function conflictsClickIntent(u){try{if(!clickIntentActive()||!u)return false;var a=abs(u),h=host(a),c=S.clickSourceHost||host(location.href);if(!/^https?:/i.test(a)||asset(a)||safeDownloadNav(a,null)||matchesClickIntent(a))return false;if(h&&c&&same(h,c)&&!relay(a)&&!hardToken(a)&&!redirParam(a))return false;return true;}catch(e){return false;}}"\n'
    )
    if insert_before(path, marker, addition):
        changed.append(path)

    old_click_guard = "function clickGuard(e){if(!S.config.enabled||!S.config.click||S.readerNavLock)return;try{rememberDownloadClick(e&&e.target);"
    new_click_guard = "function clickGuard(e){if(!S.config.enabled||!S.config.click||S.readerNavLock)return;try{rememberClickIntent(e,e&&e.target);rememberDownloadClick(e&&e.target);"
    if replace_once(path, old_click_guard, new_click_guard):
        changed.append(path)

    path = "app/src/main/java/com/yieldbrowser/app/ShieldScriptPartThree.java"
    old_popup = "if(S.config.enabled&&S.config.popup&&badNav(u,null)){report(u);return{closed:true,focus:function(){},close:function(){}};}"
    new_popup = "if(S.config.enabled&&S.config.popup&&conflictsClickIntent(u)){report(u);return{closed:true,focus:function(){},close:function(){}};}if(S.config.enabled&&S.config.popup&&badNav(u,null)){report(u);return{closed:true,focus:function(){},close:function(){}};}"
    if replace_once(path, old_popup, new_popup):
        changed.append(path)

    old_anchor = "if(S.config.enabled&&S.config.click&&!safeDownloadNav(this.href,this)&&!safeReaderNav(this.href,this)&&badNav(this.href,this)){report(this.href);return;}"
    new_anchor = "if(S.config.enabled&&S.config.click&&conflictsClickIntent(this.href)){report(this.href);return;}if(S.config.enabled&&S.config.click&&!safeDownloadNav(this.href,this)&&!safeReaderNav(this.href,this)&&badNav(this.href,this)){report(this.href);return;}"
    if replace_once(path, old_anchor, new_anchor):
        changed.append(path)

    marker = '                + "try{S.observer=new MutationObserver(schedule);S.observer.observe(D.documentElement||D,{childList:true,subtree:true,attributes:true,attributeFilter:[\'src\',\'href\',\'action\',\'style\',\'class\']});}catch(e){}"\n'
    addition = (
        '                + "try{var nativeAssign=Location.prototype.assign;Location.prototype.assign=function(u){if(S.config.enabled&&S.config.redirect&&conflictsClickIntent(u)){report(u);return;}return nativeAssign.call(this,u);};}catch(e){}"\n'
        '                + "try{var nativeReplace=Location.prototype.replace;Location.prototype.replace=function(u){if(S.config.enabled&&S.config.redirect&&conflictsClickIntent(u)){report(u);return;}return nativeReplace.call(this,u);};}catch(e){}"\n'
    )
    if insert_before(path, marker, addition):
        changed.append(path)

    path = "app/src/main/java/com/yieldbrowser/app/ShieldUrlRules.java"
    marker = "    static final Pattern TRUSTED_VIDEO_HOST = Pattern.compile(\n"
    addition = (
        "    static final Pattern POPUP_RISK_CONTENT_PATH = Pattern.compile(\n"
        "            \"(?:^|[/_-])(?:drama|drakor|subtitle|sub(?:title)?-?indo|series|season|episode|watch|movie|film|stream|player|embed|play)(?:[/_-]|$)\",\n"
        "            Pattern.CASE_INSENSITIVE);\n\n"
        "    static final Pattern POPUP_RISK_HOST = Pattern.compile(\n"
        "            \"(?:^|\\\\.)(?:dramaencode\\\\.net)$\",\n"
        "            Pattern.CASE_INSENSITIVE);\n\n"
    )
    if insert_before(path, marker, addition):
        changed.append(path)

    path = "app/src/main/java/com/yieldbrowser/app/ShieldNavigationPolicy.java"
    if replace_once(
        path,
        "        if (explicitlyTrusted) return false;\n        if (!ShieldUrlRules.isHttpOrHttps(targetUrl)) {",
        "        if (!ShieldUrlRules.isHttpOrHttps(targetUrl)) {",
    ):
        changed.append(path)
    if replace_once(
        path,
        "        if (hardSignal) return true;\n\n        if (isDownloadPage(sourceUrl)",
        "        if (hardSignal) return true;\n        // A user gesture must never whitelist a destination that still carries a hard ad signal.\n        if (explicitlyTrusted) return false;\n\n        if (isDownloadPage(sourceUrl)",
    ):
        changed.append(path)

    old_isolation = (
        "        if (ShieldUrlRules.TRUSTED_VIDEO_HOST.matcher(host).find()) return false;\n"
        "        if (isDownloadPage(url) || isReaderOrContentPage(url)) return true;\n\n"
        "        return ShieldUrlRules.POPUP_ISOLATED_VIDEO_PATH\n"
        "                .matcher(ShieldUrlRules.pathOf(url)).find();"
    )
    new_isolation = (
        "        if (ShieldUrlRules.TRUSTED_VIDEO_HOST.matcher(host).find()) return false;\n"
        "        if (ShieldUrlRules.POPUP_RISK_HOST.matcher(host).find()) return true;\n"
        "        if (isDownloadPage(url) || isReaderOrContentPage(url)) return true;\n\n"
        "        String path = ShieldUrlRules.pathOf(url);\n"
        "        return ShieldUrlRules.POPUP_ISOLATED_VIDEO_PATH.matcher(path).find()\n"
        "                || ShieldUrlRules.POPUP_RISK_CONTENT_PATH.matcher(path).find();"
    )
    if replace_once(path, old_isolation, new_isolation):
        changed.append(path)

    print("Updated files:")
    for item in sorted(set(changed)):
        print(f"- {item}")


if __name__ == "__main__":
    main()
