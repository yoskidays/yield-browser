package com.yieldbrowser.app;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * High-confidence ad/navigation policy used by Yield Shield Engine V2.
 *
 * The policy intentionally prefers allowing uncertain traffic. It blocks only when a request
 * matches several independent ad/relay signals, so compatibility mode can keep first-party
 * reader images, scripts, login flows and media working.
 */
final class ShieldEngineV2 {
    private static final Pattern DIRECT_CONTENT_ASSET = Pattern.compile(
            ".*\\.(?:avif|bmp|gif|ico|jpe?g|png|svg|webp|woff2?|ttf|otf|mp4|m4v|mov|webm|mkv|m3u8|mpd|m4s|ts|mp3|aac|wav|ogg|pdf|zip|rar|7z)(?:$|[?#]).*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SAFE_CONTENT_PATH = Pattern.compile(
            "(?:^|/)(?:manga|manhwa|manhua|comic|komik|chapter|chapitre|capitulo|episode|reader|read|reading|baca|novel|article|post|category|search)(?:[-_/]|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RELAY_SEGMENT = Pattern.compile(
            "(?:^|/)(?:r|go|out|away|jump|visit|redirect|redir|click|link|external|open|track|tracking|offer|offers|promo|ads?|interstitial)(?:/|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern OPAQUE_SEGMENT = Pattern.compile(
            "(?:^|/)[a-z0-9_-]{16,}(?:/|$)", Pattern.CASE_INSENSITIVE);

    private static final Pattern REDIRECT_PARAMETER = Pattern.compile(
            "(?:[?&](?:url|u|to|target|dest|destination|redirect|redirect_url|redirect_uri|redir|r|go|out|link|click|next|continue|return|return_to|return_url|navigate_url)=)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern HARD_AD_TOKEN = Pattern.compile(
            "(?:adclick|ad_click|adurl|clickunder|onclickads|popunder|popupads|interstitial|affiliate|aff_sub|af_click|click_id|campaign_id|tracking_id|utm_medium=affiliates|deep_and_deferred|navigate_url|reactpath)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern KNOWN_AD_HOST = Pattern.compile(
            "(?:^|\\.)(?:doubleclick\\.net|googlesyndication\\.com|googleadservices\\.com|adservice\\.google\\.[a-z.]+|onclickads\\.net|clickadu\\.com|popads\\.net|popcash\\.net|propellerads\\.com|adsterra\\.com|hilltopads\\.net|exoclick\\.com|trafficjunky\\.net|juicyads\\.com|admaven\\.com|realsrv\\.com|taboola\\.com|outbrain\\.com|mgid\\.com|revcontent\\.com|hotterydiseur\\.[a-z.]+|sewarsremeets\\.[a-z.]+|invest-tracing\\.[a-z.]+)$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CHEAP_AD_TLD = Pattern.compile(
            ".*\\.(?:click|cfd|cam|monster|quest|buzz|icu|cyou|xyz|top|shop|site|space|online|live|fun|lol)$",
            Pattern.CASE_INSENSITIVE);

    private ShieldEngineV2() {
    }

    static boolean shouldBlockMainFrameNavigation(String targetUrl,
                                                  String sourceUrl,
                                                  boolean hasGesture,
                                                  boolean compatibilityOrReaderContext,
                                                  boolean explicitlyTrusted,
                                                  boolean legacySuspicious) {
        if (explicitlyTrusted) return false;
        if (!isHttpOrHttps(targetUrl)) return isDangerousExternalScheme(targetUrl);
        if (isDirectContentAsset(targetUrl)) return false;

        String targetHost = hostOf(targetUrl);
        String sourceHost = hostOf(sourceUrl);
        if (targetHost.isEmpty()) return false;

        boolean sameSite = !sourceHost.isEmpty() && sameSite(targetHost, sourceHost);
        if (sameSite) {
            return isHighConfidenceSameOriginRelay(targetUrl, sourceUrl, compatibilityOrReaderContext);
        }

        boolean hardSignal = isKnownAdHost(targetHost)
                || hasHardAdToken(targetUrl)
                || legacySuspicious;
        if (hardSignal) return true;

        // A user gesture can be inherited by a click-hijacker. In a reader/compatibility page,
        // a cheap random domain plus an opaque path is enough to quarantine the navigation.
        if (compatibilityOrReaderContext && isCheapAdHost(targetHost)
                && (hasOpaquePath(targetUrl) || hasRedirectParameter(targetUrl))) {
            return true;
        }

        // Unknown, clean cross-site links remain usable. This preserves login and normal links.
        return false;
    }

    static boolean shouldBlockSubresource(String resourceUrl, String pageUrl, boolean legacyHardAd) {
        if (!isHttpOrHttps(resourceUrl) || !isHttpOrHttps(pageUrl)) return false;

        String resourceHost = hostOf(resourceUrl);
        String pageHost = hostOf(pageUrl);
        if (resourceHost.isEmpty() || pageHost.isEmpty()) return false;
        boolean directAsset = isDirectContentAsset(resourceUrl);
        if (sameSite(resourceHost, pageHost)) {
            // Do not broadly block first-party scripts. Only obvious relay/advert endpoints are
            // denied, because many old sites use innocent filenames containing "ad".
            return hasHardAdToken(resourceUrl) && isRelayPath(resourceUrl);
        }

        if (directAsset) {
            // Preserve ordinary image/font/media CDNs. Block an asset only when its host or URL
            // carries a high-confidence advertising signal.
            return isKnownAdHost(resourceHost) || hasHardAdToken(resourceUrl);
        }
        return legacyHardAd || isKnownAdHost(resourceHost) || hasHardAdToken(resourceUrl);
    }

    static boolean isHighConfidenceSameOriginRelay(String targetUrl,
                                                    String sourceUrl,
                                                    boolean compatibilityOrReaderContext) {
        if (!isHttpOrHttps(targetUrl) || !isHttpOrHttps(sourceUrl)) return false;
        String targetHost = hostOf(targetUrl);
        String sourceHost = hostOf(sourceUrl);
        if (targetHost.isEmpty() || sourceHost.isEmpty() || !sameSite(targetHost, sourceHost)) return false;
        if (isDirectContentAsset(targetUrl)) return false;

        String targetPath = pathOf(targetUrl);
        if (SAFE_CONTENT_PATH.matcher(targetPath).find()) return false;

        int score = 0;
        if (RELAY_SEGMENT.matcher(targetPath).find()) score += 3;
        if (hasRedirectParameter(targetUrl)) score += 2;
        if (hasHardAdToken(targetUrl)) score += 2;
        if (OPAQUE_SEGMENT.matcher(targetPath).find()) score += 1;
        if (compatibilityOrReaderContext) score += 2;
        if (ReaderCompatibilityPolicy.hasReaderPathHint(sourceUrl)) score += 1;

        // `/r/<opaque token>` and equivalent reader relays must be blocked before the main tab
        // leaves the chapter. Outside reader mode, require more independent evidence.
        return score >= (compatibilityOrReaderContext ? 4 : 6);
    }

    static boolean isKnownAdOrTrackerUrl(String url) {
        if (url == null) return false;
        String host = hostOf(url);
        return (!host.isEmpty() && isKnownAdHost(host)) || hasHardAdToken(url);
    }

    static boolean isRelayPath(String url) {
        return RELAY_SEGMENT.matcher(pathOf(url)).find();
    }

    static boolean isDirectContentAsset(String url) {
        if (url == null) return false;
        return DIRECT_CONTENT_ASSET.matcher(url.toLowerCase(Locale.US)).matches();
    }

    static boolean isReaderOrContentPage(String url) {
        if (!isHttpOrHttps(url)) return false;
        String path = pathOf(url);
        return SAFE_CONTENT_PATH.matcher(path).find() || ReaderCompatibilityPolicy.hasReaderPathHint(url);
    }

    private static boolean hasRedirectParameter(String url) {
        return url != null && REDIRECT_PARAMETER.matcher(decodedLower(url)).find();
    }

    private static boolean hasHardAdToken(String url) {
        return url != null && HARD_AD_TOKEN.matcher(decodedLower(url)).find();
    }

    private static boolean hasOpaquePath(String url) {
        return OPAQUE_SEGMENT.matcher(pathOf(url)).find();
    }

    private static boolean isKnownAdHost(String host) {
        return host != null && KNOWN_AD_HOST.matcher(host).find();
    }

    private static boolean isCheapAdHost(String host) {
        return host != null && CHEAP_AD_TLD.matcher(host).matches();
    }

    private static boolean isDangerousExternalScheme(String url) {
        if (url == null) return false;
        String lower = url.trim().toLowerCase(Locale.US);
        return lower.startsWith("intent:")
                || lower.startsWith("market:")
                || lower.startsWith("shopeeid:")
                || lower.startsWith("lazada:")
                || lower.startsWith("tokopedia:");
    }

    private static boolean isHttpOrHttps(String url) {
        if (url == null) return false;
        String lower = url.trim().toLowerCase(Locale.US);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static String decodedLower(String url) {
        if (url == null) return "";
        String lower = url.toLowerCase(Locale.US);
        try {
            return URLDecoder.decode(lower, "UTF-8");
        } catch (Exception ignored) {
            return lower;
        }
    }

    private static String hostOf(String url) {
        if (url == null) return "";
        try {
            URI parsed = URI.create(url.trim());
            String host = parsed.getHost();
            if (host == null) return "";
            host = host.toLowerCase(Locale.US);
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String pathOf(String url) {
        if (url == null) return "";
        try {
            URI parsed = URI.create(url.trim());
            String path = parsed.getPath();
            return path == null ? "" : path.toLowerCase(Locale.US);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean sameSite(String first, String second) {
        return first.equals(second) || first.endsWith("." + second) || second.endsWith("." + first);
    }
}

/** Builds the universal document-start and runtime scripts for Shield Engine V2. */
final class ShieldPageScript {
    private ShieldPageScript() {
    }

    static String documentStart(boolean enabled,
                                boolean popupBlocker,
                                boolean redirectBlocker,
                                boolean scriptIframeBlocker,
                                boolean clickHijackBlocker) {
        String config = "{enabled:" + enabled
                + ",popup:" + popupBlocker
                + ",redirect:" + redirectBlocker
                + ",resource:" + scriptIframeBlocker
                + ",click:" + clickHijackBlocker + "}";

        return "(function(){'use strict';try{"
                + "var W=window,D=document;"
                + "var C=" + config + ";"
                + "var S=W.__yieldShieldV2State||{installed:false,observer:null,timer:0};W.__yieldShieldV2State=S;S.config=C;"
                + "function low(v){return String(v||'').toLowerCase();}"
                + "function dec(v){try{return decodeURIComponent(String(v||''));}catch(e){return String(v||'');}}"
                + "function abs(u){try{return new URL(String(u||''),location.href).href;}catch(e){return String(u||'');}}"
                + "function host(u){try{return(new URL(abs(u))).hostname.replace(/^www\\./,'').toLowerCase();}catch(e){return'';}}"
                + "function path(u){try{return(new URL(abs(u))).pathname.toLowerCase();}catch(e){return'';}}"
                + "function same(a,b){return !!a&&!!b&&(a===b||a.endsWith('.'+b)||b.endsWith('.'+a));}"
                + "function asset(u){return /\\.(?:avif|bmp|gif|ico|jpe?g|png|svg|webp|woff2?|ttf|otf|mp4|m4v|mov|webm|mkv|m3u8|mpd|m4s|ts|mp3|aac|wav|ogg|pdf|zip|rar|7z)(?:$|[?#])/i.test(String(u||''));}"
                + "function contentPath(p){return /(?:^|\\/)(?:manga|manhwa|manhua|comic|komik|chapter|chapitre|capitulo|episode|reader|read|reading|baca|novel|article|post|category|search)(?:[-_\\/]|$)/i.test(p||'');}"
                + "function reader(){var p=location.pathname||'';if(contentPath(p))return true;try{return D.querySelectorAll('img[data-src],img[data-lazy-src],img[data-original],picture source[data-srcset]').length>=3;}catch(e){return false;}}"
                + "function relay(u){var p=path(u);return /(?:^|\\/)(?:r|go|out|away|jump|visit|redirect|redir|click|link|external|open|track|tracking|offer|offers|promo|ads?|interstitial)(?:\\/|$)/i.test(p);}"
                + "function redirParam(u){return /[?&](?:url|u|to|target|dest|destination|redirect|redirect_url|redirect_uri|redir|r|go|out|link|click|next|continue|return|return_to|return_url|navigate_url)=/i.test(dec(u));}"
                + "function hardToken(u){return /(?:adclick|ad_click|adurl|clickunder|onclickads|popunder|popupads|interstitial|affiliate|aff_sub|af_click|click_id|campaign_id|tracking_id|utm_medium=affiliates|deep_and_deferred|navigate_url|reactpath)/i.test(dec(u));}"
                + "function adHost(h){return /(?:^|\\.)(?:doubleclick\\.net|googlesyndication\\.com|googleadservices\\.com|onclickads\\.net|clickadu\\.com|popads\\.net|popcash\\.net|propellerads\\.com|adsterra\\.com|hilltopads\\.net|exoclick\\.com|trafficjunky\\.net|juicyads\\.com|admaven\\.com|realsrv\\.com|taboola\\.com|outbrain\\.com|mgid\\.com|revcontent\\.com)$/i.test(h||'');}"
                + "function cheap(h){return /\\.(?:click|cfd|cam|monster|quest|buzz|icu|cyou|xyz|top|shop|site|space|online|live|fun|lol)$/i.test(h||'');}"
                + "function sameRelay(u){var a=abs(u),h=host(a),c=host(location.href),p=path(a);if(!h||!c||!same(h,c)||asset(a)||contentPath(p))return false;var n=0;if(relay(a))n+=3;if(redirParam(a))n+=2;if(hardToken(a))n+=2;if(/(?:^|\\/)[a-z0-9_-]{16,}(?:\\/|$)/i.test(p))n+=1;if(reader())n+=2;return n>=4;}"
                + "function bad(u){if(!S.config.enabled||!u)return false;var a=abs(u),l=low(a);if(/^(intent|market|shopeeid|lazada|tokopedia):/.test(l))return true;if(!/^https?:/i.test(a)||asset(a))return false;var h=host(a),c=host(location.href);if(same(h,c))return S.config.redirect&&sameRelay(a);if(adHost(h)||hardToken(a))return true;return reader()&&cheap(h)&&(redirParam(a)||/(?:^|\\/)[a-z0-9_-]{16,}(?:\\/|$)/i.test(path(a)));}"
                + "function report(u){try{if(W.YieldAdBlockBridge&&YieldAdBlockBridge.onAdRedirect)YieldAdBlockBridge.onAdRedirect(String(u));}catch(e){}}"
                + "function deny(e,u){try{report(u);if(e){e.preventDefault();e.stopPropagation();if(e.stopImmediatePropagation)e.stopImmediatePropagation();}}catch(x){}return false;}"
                + "function targetUrl(node){try{var a=node&&node.closest?node.closest('a[href],area[href]'):null;if(a)return a.href||a.getAttribute('href')||'';var f=node&&node.closest?node.closest('form[action]'):null;if(f)return f.action||f.getAttribute('action')||'';}catch(e){}return'';}"
                + "function clickGuard(e){if(!S.config.enabled||!S.config.click)return;try{var u=targetUrl(e.target);if(u&&bad(u))return deny(e,u);}catch(x){}}"
                + "function submitGuard(e){if(!S.config.enabled||!S.config.redirect)return;try{var f=e.target;if(f&&f.action&&bad(f.action))return deny(e,f.action);}catch(x){}}"
                + "function visible(el){try{var r=el.getBoundingClientRect(),cs=getComputedStyle(el);return r.width>2&&r.height>2&&cs.display!=='none'&&cs.visibility!=='hidden';}catch(e){return false;}}"
                + "function overlayBad(el){try{if(!visible(el))return false;var cs=getComputedStyle(el),r=el.getBoundingClientRect(),vw=Math.max(1,innerWidth),vh=Math.max(1,innerHeight);var cover=(r.width*r.height)/(vw*vh);if(cover<0.55)return false;var pos=cs.position;if(pos!=='fixed'&&pos!=='absolute'&&pos!=='sticky')return false;var z=parseInt(cs.zIndex,10);if(!isFinite(z))z=0;var meta=low((el.id||'')+' '+(typeof el.className==='string'?el.className:'')+' '+(el.getAttribute('aria-label')||''));var href=targetUrl(el)||'';var child='';try{var n=el.querySelector('a[href],iframe[src],form[action]');if(n)child=n.href||n.src||n.action||'';}catch(x){}var marked=/(^|[ _-])(ad|ads|advert|popup|popunder|interstitial|clickunder|sponsor)([ _-]|$)/i.test(meta);var transparent=parseFloat(cs.opacity||'1')<0.18||cs.backgroundColor==='rgba(0, 0, 0, 0)';var clickable=!!(el.onclick||el.getAttribute('onclick')||el.getAttribute('role')==='link'||cs.cursor==='pointer');var meaningful=false;try{meaningful=!!el.querySelector('img,video,audio,[role=dialog],button,input,textarea,select')||String(el.innerText||el.textContent||'').trim().length>24;}catch(x){}return bad(href)||bad(child)||marked||(reader()&&cover>0.72&&z>=1000&&transparent&&clickable&&!meaningful);}catch(e){return false;}}"
                + "function clean(){if(!S.config.enabled)return;var removed=false;try{if(S.config.resource)D.querySelectorAll('iframe[src],script[src],ins[data-ad-client]').forEach(function(el){var u=el.src||el.getAttribute('data-ad-client')||'';if(u&&bad(u)){el.remove();removed=true;}});}catch(e){}try{if(S.config.click)D.querySelectorAll('[class*=popup],[class*=popunder],[class*=interstitial],[id*=popup],[id*=popunder],[id*=interstitial],[class*=overlay],[id*=overlay],[style*=z-index]').forEach(function(el){if(overlayBad(el)){el.style.setProperty('display','none','important');el.style.setProperty('pointer-events','none','important');removed=true;}});}catch(e){}if(removed){try{if(D.documentElement)D.documentElement.style.removeProperty('overflow');if(D.body)D.body.style.removeProperty('overflow');}catch(e){}}}"
                + "function schedule(){if(S.timer)return;S.timer=setTimeout(function(){S.timer=0;clean();},80);}"
                + "W.__yieldShieldV2SetConfig=function(n){try{if(n)Object.keys(n).forEach(function(k){S.config[k]=!!n[k];});schedule();}catch(e){}};"
                + "W.__yieldShieldV2Run=clean;"
                + "if(!S.installed){S.installed=true;"
                + "try{var nativeOpen=W.open;W.open=function(u,n,f){if(S.config.enabled&&S.config.popup&&bad(u)){report(u);return{closed:true,focus:function(){},close:function(){}};}try{return nativeOpen.call(W,u,n,f);}catch(e){return null;}};}catch(e){}"
                + "D.addEventListener('click',clickGuard,true);D.addEventListener('auxclick',clickGuard,true);D.addEventListener('submit',submitGuard,true);"
                + "try{var ac=HTMLAnchorElement.prototype.click;HTMLAnchorElement.prototype.click=function(){if(S.config.enabled&&S.config.click&&bad(this.href)){report(this.href);return;}return ac.call(this);};}catch(e){}"
                + "try{var fs=HTMLFormElement.prototype.submit;HTMLFormElement.prototype.submit=function(){if(S.config.enabled&&S.config.redirect&&bad(this.action)){report(this.action);return;}return fs.call(this);};}catch(e){}"
                + "try{S.observer=new MutationObserver(schedule);S.observer.observe(D.documentElement||D,{childList:true,subtree:true,attributes:true,attributeFilter:['src','href','action','style','class']});}catch(e){}"
                + "}"
                + "if(D.readyState==='loading')D.addEventListener('DOMContentLoaded',function(){clean();setTimeout(clean,350);},true);else schedule();"
                + "}catch(e){}})();";
    }

    static String runtimeConfig(boolean enabled,
                                boolean popupBlocker,
                                boolean redirectBlocker,
                                boolean scriptIframeBlocker,
                                boolean clickHijackBlocker) {
        return "javascript:(function(){try{if(window.__yieldShieldV2SetConfig)window.__yieldShieldV2SetConfig({enabled:"
                + enabled + ",popup:" + popupBlocker + ",redirect:" + redirectBlocker
                + ",resource:" + scriptIframeBlocker + ",click:" + clickHijackBlocker
                + "});if(window.__yieldShieldV2Run)window.__yieldShieldV2Run();}catch(e){}})();";
    }
}
