#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(relative_path: str) -> str:
    return (ROOT / relative_path).read_text(encoding="utf-8")


def write(relative_path: str, text: str) -> None:
    (ROOT / relative_path).write_text(text, encoding="utf-8")


def replace_once(relative_path: str, old: str, new: str) -> bool:
    text = read(relative_path)
    if old in text:
        write(relative_path, text.replace(old, new, 1))
        return True
    if new in text:
        return False
    raise RuntimeError(
        f"Expected source fragment was not found in {relative_path}: {old[:100]!r}"
    )


def insert_before(relative_path: str, token: str, addition: str, identity: str) -> bool:
    text = read(relative_path)
    if identity in text:
        return False
    index = text.find(token)
    if index < 0:
        raise RuntimeError(
            f"Insertion token was not found in {relative_path}: {token[:100]!r}"
        )
    write(relative_path, text[:index] + addition + text[index:])
    return True


def mark(changed: list[str], path: str, result: bool) -> None:
    if result:
        changed.append(path)


def main() -> None:
    changed: list[str] = []

    path = "app/src/main/java/com/yieldbrowser/app/ShieldScriptBuilder.java"
    mark(changed, path, replace_once(
        path,
        "var S=W.__yieldShieldV2State||{installed:false,observer:null,timer:0,downloadClickUntil:0};W.__yieldShieldV2State=S;S.config=C;",
        "var S=W.__yieldShieldV2State||{installed:false,observer:null,timer:0,downloadClickUntil:0,clickIntentUrl:'',clickIntentHost:'',clickSourceHost:'',clickIntentUntil:0};W.__yieldShieldV2State=S;S.config=C;S.clickIntentUrl=S.clickIntentUrl||'';S.clickIntentHost=S.clickIntentHost||'';S.clickSourceHost=S.clickSourceHost||'';S.clickIntentUntil=Number(S.clickIntentUntil||0);",
    ))

    path = "app/src/main/java/com/yieldbrowser/app/ShieldScriptPartOne.java"
    part_one_addition = r'''                + "function popupRiskPath(p){return /(?:^|[\\/_-])(?:drama|drakor|subtitle|sub(?:title)?-?indo|series|season|episode|watch|movie|film|stream|player|embed|play)(?:[\\/_-]|$)/i.test(p||'');}"
                + "function popupRiskPage(){if(searchPage())return false;var h=host(location.href),p=path(location.href);return isolated()||popupRiskPath(p)||/(?:^|\\.)(?:dramaencode\\.net)$/i.test(h||'');}"
'''
    mark(changed, path, insert_before(
        path,
        '                + "function relay(u)',
        part_one_addition,
        "function popupRiskPage()",
    ))

    path = "app/src/main/java/com/yieldbrowser/app/ShieldScriptPartTwo.java"
    mark(changed, path, replace_once(
        path,
        "if(downloadPage())return true;if(isolated())return true;return cheap(h)&&",
        "if(downloadPage())return true;if(isolated())return true;if(popupRiskPage()&&!cleanClickIntent(a,node))return true;return cheap(h)&&",
    ))

    part_two_addition = r'''                + "function cleanClickIntent(u,node){try{var a=abs(u),h=host(a),c=host(location.href);if(!/^https?:/i.test(a)||!h||adHost(h)||hardToken(a)||redirParam(a)||relay(a))return false;if(cheap(h)&&(!c||!same(h,c)||/(?:^|\\/)[a-z0-9_-]{16,}(?:\\/|$)/i.test(path(a))))return false;var m=navMeta(node);if(/(?:^|[ _-])(ad|ads|advert|sponsor|promo|affiliate|popup|popunder|interstitial|clickunder)(?:[ _-]|$)/i.test(m))return false;if(popupRiskPage()&&c&&!same(h,c)&&!contentRegion(node)&&!downloadControl(node))return false;return true;}catch(e){return false;}}"
                + "function rememberClickIntent(e,node){try{if(!popupRiskPage()||!e||e.isTrusted===false)return false;var u=candidateUrl(node),a=u?abs(u):'',h=host(a),c=host(location.href);S.clickSourceHost=c;S.clickIntentUrl='';S.clickIntentHost='';S.clickIntentUntil=Date.now()+1600;if(a&&cleanClickIntent(a,node)){S.clickIntentUrl=a;S.clickIntentHost=h;}return true;}catch(x){return false;}}"
                + "function clickIntentActive(){return popupRiskPage()&&Date.now()<=(S.clickIntentUntil||0);}"
                + "function matchesClickIntent(u){try{if(!clickIntentActive()||!S.clickIntentUrl)return false;var a=abs(u),h=host(a);if(a===S.clickIntentUrl)return true;return !!h&&!!S.clickIntentHost&&same(h,S.clickIntentHost);}catch(e){return false;}}"
                + "function conflictsClickIntent(u){try{if(!clickIntentActive()||!u)return false;var a=abs(u),h=host(a),c=S.clickSourceHost||host(location.href);if(!/^https?:/i.test(a)||asset(a)||safeDownloadNav(a,null)||matchesClickIntent(a))return false;if(h&&c&&same(h,c)&&!relay(a)&&!hardToken(a)&&!redirParam(a))return false;return true;}catch(e){return false;}}"
'''
    mark(changed, path, insert_before(
        path,
        '                + "function rememberDownloadClick(node)',
        part_two_addition,
        "function rememberClickIntent(e,node)",
    ))
    mark(changed, path, replace_once(
        path,
        "function clickGuard(e){if(!S.config.enabled||!S.config.click||S.readerNavLock)return;try{rememberDownloadClick(e&&e.target);",
        "function clickGuard(e){if(!S.config.enabled||!S.config.click||S.readerNavLock)return;try{rememberClickIntent(e,e&&e.target);rememberDownloadClick(e&&e.target);",
    ))

    path = "app/src/main/java/com/yieldbrowser/app/ShieldScriptPartThree.java"
    mark(changed, path, replace_once(
        path,
        "if(S.config.enabled&&S.config.popup&&badNav(u,null)){report(u);return{closed:true,focus:function(){},close:function(){}};}",
        "if(S.config.enabled&&S.config.popup&&conflictsClickIntent(u)){report(u);return{closed:true,focus:function(){},close:function(){}};}if(S.config.enabled&&S.config.popup&&badNav(u,null)){report(u);return{closed:true,focus:function(){},close:function(){}};}",
    ))
    mark(changed, path, replace_once(
        path,
        "if(S.config.enabled&&S.config.click&&!safeDownloadNav(this.href,this)&&!safeReaderNav(this.href,this)&&badNav(this.href,this)){report(this.href);return;}",
        "if(S.config.enabled&&S.config.click&&conflictsClickIntent(this.href)){report(this.href);return;}if(S.config.enabled&&S.config.click&&!safeDownloadNav(this.href,this)&&!safeReaderNav(this.href,this)&&badNav(this.href,this)){report(this.href);return;}",
    ))
    part_three_addition = r'''                + "try{var nativeAssign=Location.prototype.assign;Location.prototype.assign=function(u){if(S.config.enabled&&S.config.redirect&&conflictsClickIntent(u)){report(u);return;}return nativeAssign.call(this,u);};}catch(e){}"
                + "try{var nativeReplace=Location.prototype.replace;Location.prototype.replace=function(u){if(S.config.enabled&&S.config.redirect&&conflictsClickIntent(u)){report(u);return;}return nativeReplace.call(this,u);};}catch(e){}"
'''
    mark(changed, path, insert_before(
        path,
        '                + "try{S.observer=new MutationObserver(schedule)',
        part_three_addition,
        "Location.prototype.assign",
    ))

    path = "app/src/main/java/com/yieldbrowser/app/ShieldUrlRules.java"
    rules_addition = r'''    static final Pattern POPUP_RISK_CONTENT_PATH = Pattern.compile(
            "(?:^|[/_-])(?:drama|drakor|subtitle|sub(?:title)?-?indo|series|season|episode|watch|movie|film|stream|player|embed|play)(?:[/_-]|$)",
            Pattern.CASE_INSENSITIVE);

    static final Pattern POPUP_RISK_HOST = Pattern.compile(
            "(?:^|\\.)(?:dramaencode\\.net)$",
            Pattern.CASE_INSENSITIVE);

'''
    mark(changed, path, insert_before(
        path,
        "    static final Pattern TRUSTED_VIDEO_HOST = Pattern.compile(",
        rules_addition,
        "POPUP_RISK_CONTENT_PATH",
    ))

    path = "app/src/main/java/com/yieldbrowser/app/ShieldNavigationPolicy.java"
    mark(changed, path, replace_once(
        path,
        "        if (explicitlyTrusted) return false;\n        if (!ShieldUrlRules.isHttpOrHttps(targetUrl)) {",
        "        if (!ShieldUrlRules.isHttpOrHttps(targetUrl)) {",
    ))
    mark(changed, path, replace_once(
        path,
        "        if (hardSignal) return true;\n\n        if (isDownloadPage(sourceUrl)",
        "        if (hardSignal) return true;\n        // User gestures must never whitelist destinations with hard ad signals.\n        if (explicitlyTrusted) return false;\n\n        if (isDownloadPage(sourceUrl)",
    ))
    mark(changed, path, replace_once(
        path,
        "        if (ShieldUrlRules.TRUSTED_VIDEO_HOST.matcher(host).find()) return false;\n        if (isDownloadPage(url) || isReaderOrContentPage(url)) return true;\n\n        return ShieldUrlRules.POPUP_ISOLATED_VIDEO_PATH\n                .matcher(ShieldUrlRules.pathOf(url)).find();",
        "        if (ShieldUrlRules.TRUSTED_VIDEO_HOST.matcher(host).find()) return false;\n        if (ShieldUrlRules.POPUP_RISK_HOST.matcher(host).find()) return true;\n        if (isDownloadPage(url) || isReaderOrContentPage(url)) return true;\n\n        String path = ShieldUrlRules.pathOf(url);\n        return ShieldUrlRules.POPUP_ISOLATED_VIDEO_PATH.matcher(path).find()\n                || ShieldUrlRules.POPUP_RISK_CONTENT_PATH.matcher(path).find();",
    ))

    print("Updated files:")
    for item in sorted(set(changed)):
        print(f"- {item}")


if __name__ == "__main__":
    main()
