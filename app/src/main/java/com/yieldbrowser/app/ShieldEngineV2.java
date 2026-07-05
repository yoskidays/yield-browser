package com.yieldbrowser.app;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * High-confidence ad/navigation policy used by Yield Shield Engine V2.
 *
 * Ordinary pages still prefer allowing uncertain traffic. Reader pages use a stricter main-frame
 * boundary: first-party chapter navigation and direct content assets remain available, while an
 * unexpected cross-site takeover is quarantined even when a stolen touch carries a user gesture.
 */
final class ShieldEngineV2 {
    private static final Pattern DIRECT_CONTENT_ASSET = Pattern.compile(
            ".*\\.(?:avif|bmp|gif|ico|jpe?g|png|svg|webp|woff2?|ttf|otf|mp4|m4v|mov|webm|mkv|m3u8|mpd|m4s|ts|mp3|aac|wav|ogg|pdf|zip|rar|7z)(?:$|[?#]).*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SAFE_CONTENT_PATH = Pattern.compile(
            "(?:^|[/_-])(?:manga|manhwa|manhua|comic|komik|chapter|chapitre|capitulo|episode|reader|read(?:-online)?|reading|baca|novel)(?:[/_-]|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern POPUP_ISOLATED_VIDEO_PATH = Pattern.compile(
            "(?:^|[/_-])(?:video|videos|watch|movie|movies|film|stream|player|embed|play)(?:[/_-]|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TRUSTED_VIDEO_HOST = Pattern.compile(
            "(?:^|\\.)(?:youtube\\.[a-z.]+|youtu\\.be|googlevideo\\.com|ytimg\\.com|ggpht\\.com|vimeo\\.com|dailymotion\\.com)$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SEARCH_PATH_HINT = Pattern.compile(
            "(?:^|/)(?:search|search-results?|results?|web|html|find|s)(?:/|$|\\.)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SEARCH_QUERY_HINT = Pattern.compile(
            "(?:^|[?&])(?:q|query|p|text|wd|keyword|keywords|search|search_query|term)=",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern KNOWN_SEARCH_HOST = Pattern.compile(
            "(?:^|\\.)(?:google\\.[a-z.]+|bing\\.com|duckduckgo\\.com|yahoo\\.[a-z.]+|yandex\\.[a-z.]+|baidu\\.com|brave\\.com|startpage\\.com|ecosia\\.org|qwant\\.com|mojeek\\.com|kagi\\.com|naver\\.com|aol\\.com|ask\\.com|swisscows\\.com|metager\\.[a-z.]+)$",
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
            // A chapter/episode link is a first-party content navigation, not an opaque relay.
            // This explicit allow-lane must run before relay scoring because long chapter slugs
            // (for example /title-chapter-6-1/) otherwise look like opaque tracking tokens.
            if (isSafeSameSiteReaderNavigation(targetUrl, sourceUrl)) return false;
            return isHighConfidenceSameOriginRelay(targetUrl, sourceUrl, compatibilityOrReaderContext);
        }

        boolean hardSignal = isKnownAdHost(targetHost)
                || hasHardAdToken(targetUrl)
                || legacySuspicious;
        if (hardSignal) return true;

        // A chapter/reader page must not lose its main tab to an unknown cross-site destination.
        // Popunder/direct-link scripts inherit hasGesture from the user's image tap, so gesture is
        // not sufficient consent here. Explicit address-bar/search/download flows are handled by
        // the explicitlyTrusted allow-lane above. Direct image/media assets were already allowed.
        if (compatibilityOrReaderContext && isPopupIsolationContentPage(sourceUrl)) {
            return true;
        }

        // Compatibility-only pages keep the older high-confidence rule to avoid breaking login
        // and front-door redirects that legitimately switch domains.
        if (compatibilityOrReaderContext && isCheapAdHost(targetHost)
                && (hasOpaquePath(targetUrl) || hasRedirectParameter(targetUrl))) {
            return true;
        }

        // Unknown, clean cross-site links remain usable on ordinary pages.
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
        if (isSafeSameSiteReaderNavigation(targetUrl, sourceUrl)) return false;
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

    /**
     * Allows legitimate previous/next chapter navigation before the relay heuristic runs.
     *
     * Reader sites commonly use long semantic slugs such as
     * /series-name-chapter-6-1/. The old opaque-segment score treated those slugs as an ad
     * token when the source was already a chapter page. Only same-site, non-relay,
     * non-ad destinations are eligible for this allow-lane.
     */
    static boolean isSafeSameSiteReaderNavigation(String targetUrl, String sourceUrl) {
        if (!isHttpOrHttps(targetUrl) || !isHttpOrHttps(sourceUrl)) return false;
        String targetHost = hostOf(targetUrl);
        String sourceHost = hostOf(sourceUrl);
        if (targetHost.isEmpty() || sourceHost.isEmpty() || !sameSite(targetHost, sourceHost)) return false;
        if (isDirectContentAsset(targetUrl) || isRelayPath(targetUrl) || hasHardAdToken(targetUrl)) return false;

        String targetPath = pathOf(targetUrl);
        boolean targetReader = ReaderCompatibilityPolicy.hasReaderPathHint(targetUrl)
                || SAFE_CONTENT_PATH.matcher(targetPath).find();
        if (!targetReader) return false;

        String sourcePath = pathOf(sourceUrl);
        boolean sourceReaderOrListing = ReaderCompatibilityPolicy.hasReaderPathHint(sourceUrl)
                || SAFE_CONTENT_PATH.matcher(sourcePath).find();
        return sourceReaderOrListing;
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

    static boolean isSearchResultsPage(String url) {
        if (!isHttpOrHttps(url)) return false;
        String host = hostOf(url);
        if (host.isEmpty()) return false;
        String path = pathOf(url);
        String query = queryOf(url);
        boolean hasSearchQuery = SEARCH_QUERY_HINT.matcher(query).find();
        boolean hasSearchPath = SEARCH_PATH_HINT.matcher(path).find();
        boolean knownSearchHost = KNOWN_SEARCH_HOST.matcher(host).find();

        // Known engines use many regional domains and URL shapes. Generic engines such as
        // SearX/SearXNG are also supported when both a search-like path and query are present.
        return (knownSearchHost && (hasSearchQuery || hasSearchPath))
                || (hasSearchPath && hasSearchQuery);
    }

    static boolean isReaderOrContentPage(String url) {
        if (!isHttpOrHttps(url) || isSearchResultsPage(url)) return false;
        String path = pathOf(url);
        return SAFE_CONTENT_PATH.matcher(path).find() || ReaderCompatibilityPolicy.hasReaderPathHint(url);
    }

    static boolean isPopupIsolationContentPage(String url) {
        if (!isHttpOrHttps(url) || isSearchResultsPage(url)) return false;
        String host = hostOf(url);
        if (host.isEmpty()) return false;
        if (TRUSTED_VIDEO_HOST.matcher(host).find()) return false;
        if (isReaderOrContentPage(url)) return true;
        return POPUP_ISOLATED_VIDEO_PATH.matcher(pathOf(url)).find();
    }

    /**
     * The legacy premium click guard predates Shield Engine V2 and has no DOM-aware reader
     * recovery. Running both guards on a chapter page can consume the same touch twice, so
     * reader/content pages use only the V2 guard. Ordinary pages keep the legacy guard as an
     * additional compatibility layer.
     */
    static boolean shouldUseLegacyClickGuard(String pageUrl, boolean clickHijackEnabled) {
        return clickHijackEnabled && !isPopupIsolationContentPage(pageUrl);
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

    private static String queryOf(String url) {
        if (url == null) return "";
        try {
            URI parsed = URI.create(url.trim());
            String query = parsed.getRawQuery();
            return query == null ? "" : "?" + query.toLowerCase(Locale.US);
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
                + "function contentPath(p){return /(?:^|[\\/_-])(?:manga|manhwa|manhua|comic|komik|chapter|chapitre|capitulo|episode|reader|read(?:-online)?|reading|baca|novel)(?:[\\/_-]|$)/i.test(p||'');}"
                + "function videoPath(p){return /(?:^|[\\/_-])(?:video|videos|watch|movie|movies|film|stream|player|embed|play)(?:[\\/_-]|$)/i.test(p||'');}"
                + "function trustedVideoHost(h){return /(?:^|\\.)(?:youtube\\.[a-z.]+|youtu\\.be|googlevideo\\.com|ytimg\\.com|ggpht\\.com|vimeo\\.com|dailymotion\\.com)$/i.test(h||'');}"
                + "function searchPage(){try{var h=host(location.href),p=path(location.href),q=low(location.search||'');var known=/(?:^|\\.)(?:google\\.[a-z.]+|bing\\.com|duckduckgo\\.com|yahoo\\.[a-z.]+|yandex\\.[a-z.]+|baidu\\.com|brave\\.com|startpage\\.com|ecosia\\.org|qwant\\.com|mojeek\\.com|kagi\\.com|naver\\.com|aol\\.com|ask\\.com|swisscows\\.com|metager\\.[a-z.]+)$/i.test(h);var qp=/(?:^|[?&])(?:q|query|p|text|wd|keyword|keywords|search|search_query|term)=/i.test(q);var pp=/(?:^|\\/)(?:search|search-results?|results?|web|html|find|s)(?:\\/|$|\\.)/i.test(p);return (known&&(qp||pp))||(pp&&qp);}catch(e){return false;}}"
                + "function reader(){if(searchPage())return false;var p=location.pathname||'';if(contentPath(p))return true;try{return D.querySelectorAll('img[data-src],img[data-lazy-src],img[data-original],picture source[data-srcset]').length>=3;}catch(e){return false;}}"
                + "function isolated(){if(searchPage())return false;var h=host(location.href),p=location.pathname||'';return reader()||(!trustedVideoHost(h)&&videoPath(p));}"
                + "function relay(u){var p=path(u);return /(?:^|\\/)(?:r|go|out|away|jump|visit|redirect|redir|click|link|external|open|track|tracking|offer|offers|promo|ads?|interstitial)(?:\\/|$)/i.test(p);}"
                + "function redirParam(u){return /[?&](?:url|u|to|target|dest|destination|redirect|redirect_url|redirect_uri|redir|r|go|out|link|click|next|continue|return|return_to|return_url|navigate_url)=/i.test(dec(u));}"
                + "function hardToken(u){return /(?:adclick|ad_click|adurl|clickunder|onclickads|popunder|popupads|interstitial|affiliate|aff_sub|af_click|click_id|campaign_id|tracking_id|utm_medium=affiliates|deep_and_deferred|navigate_url|reactpath)/i.test(dec(u));}"
                + "function adHost(h){return /(?:^|\\.)(?:doubleclick\\.net|googlesyndication\\.com|googleadservices\\.com|onclickads\\.net|clickadu\\.com|popads\\.net|popcash\\.net|propellerads\\.com|adsterra\\.com|hilltopads\\.net|exoclick\\.com|trafficjunky\\.net|juicyads\\.com|admaven\\.com|realsrv\\.com|taboola\\.com|outbrain\\.com|mgid\\.com|revcontent\\.com)$/i.test(h||'');}"
                + "function cheap(h){return /\\.(?:click|cfd|cam|monster|quest|buzz|icu|cyou|xyz|top|shop|site|space|online|live|fun|lol)$/i.test(h||'');}"
                + "function navMeta(node){try{var el=node&&node.closest?node.closest('a[href],area[href],button,[role=button],form[action]'):null;if(!el)return'';var rel=low(el.getAttribute&&el.getAttribute('rel'));var meta=low((el.id||'')+' '+(typeof el.className==='string'?el.className:'')+' '+(el.getAttribute&&el.getAttribute('aria-label')||'')+' '+(el.getAttribute&&el.getAttribute('title')||'')+' '+(el.innerText||el.textContent||''));return rel+' '+meta;}catch(e){return'';}}"
                + "function navControl(node){return /(?:^|[ _-])(next|prev|previous|chapter|chapters|episode|bab|lanjut|selanjut|berikut|sebelum|daftar[ _-]*chapter)(?:[ _-]|$)/i.test(navMeta(node));}"
                + "function safeReaderNav(u,node){var a=abs(u),h=host(a),c=host(location.href),p=path(a);if(!h||!c||!same(h,c)||asset(a)||relay(a)||hardToken(a))return false;if(contentPath(p))return true;return reader()&&navControl(node)&&!redirParam(a);}"
                + "function sameRelay(u,node){var a=abs(u),h=host(a),c=host(location.href),p=path(a);if(!h||!c||!same(h,c)||asset(a)||safeReaderNav(a,node)||contentPath(p))return false;var n=0;if(relay(a))n+=3;if(redirParam(a))n+=2;if(hardToken(a))n+=2;if(/(?:^|\\/)[a-z0-9_-]{16,}(?:\\/|$)/i.test(p))n+=1;if(reader())n+=2;return n>=4;}"
                + "function bad(u,node){if(!S.config.enabled||!u)return false;var a=abs(u),l=low(a);if(/^(intent|market|shopeeid|lazada|tokopedia):/.test(l))return true;if(!/^https?:/i.test(a)||asset(a))return false;var h=host(a),c=host(location.href);if(same(h,c)){if(safeReaderNav(a,node))return false;return S.config.redirect&&sameRelay(a,node);}if(adHost(h)||hardToken(a))return true;if(isolated())return true;return cheap(h)&&(redirParam(a)||/(?:^|\\/)[a-z0-9_-]{16,}(?:\\/|$)/i.test(path(a)));}"
                + "function badNav(u,node){if(bad(u,node))return true;var a=abs(u);if(!/^https?:/i.test(a)||asset(a))return false;var h=host(a),c=host(location.href);return isolated()&&!!h&&!!c&&!same(h,c);}"
                + "function report(u){try{var k=String(u||''),n=Date.now();if(k&&S.lastBlockedUrl===k&&n-(S.lastBlockedAt||0)<800)return;S.lastBlockedUrl=k;S.lastBlockedAt=n;if(W.YieldAdBlockBridge&&YieldAdBlockBridge.onAdRedirect)YieldAdBlockBridge.onAdRedirect(k);}catch(e){}}"
                + "function cancelEvent(e){try{if(e){e.preventDefault();e.stopPropagation();if(e.stopImmediatePropagation)e.stopImmediatePropagation();}}catch(x){}return false;}"
                + "function deny(e,u){try{if(u)report(u);}catch(x){}return cancelEvent(e);}"
                + "function targetUrl(node){try{var a=node&&node.closest?node.closest('a[href],area[href]'):null;if(a)return a.href||a.getAttribute('href')||'';var f=node&&node.closest?node.closest('form[action]'):null;if(f)return f.action||f.getAttribute('action')||'';}catch(e){}return'';}"
                + "function candidateUrl(node){try{var el=node&&node.closest?node.closest('a[href],area[href],form[action],button,[role=button],[role=link],[data-href],[data-url],[data-link],[data-next],[data-prev],[data-chapter-url]'):node;if(!el)return'';var direct=targetUrl(el);if(direct)return abs(direct);var attrs=['data-href','data-url','data-link','data-next','data-prev','data-chapter-url'];for(var i=0;i<attrs.length;i++){var v=el.getAttribute&&el.getAttribute(attrs[i]);if(v&&/^(?:https?:|\\/\\/|\\/|\\.\\.?\\/|\\?)/i.test(String(v).trim()))return abs(v);}var oc=el.getAttribute&&el.getAttribute('onclick')||'';var m=String(oc).match(/(?:window\\.)?location(?:\\.href)?\\s*=\\s*['\"]([^'\"]+)['\"]|location\\.(?:assign|replace)\\(\\s*['\"]([^'\"]+)['\"]/i);if(m)return abs(m[1]||m[2]);}catch(e){}return'';}"
                + "function clickSurfaceSuspicious(node){try{var el=node&&node.closest?node.closest('a,button,[role=button],[role=link],[onclick],[style]'):node;if(!el||navControl(el))return false;var u=candidateUrl(el);if(u&&badNav(u,el))return true;var cs=getComputedStyle(el),p=cs.position,z=parseInt(cs.zIndex,10);if(!isFinite(z))z=0;var meta=low((el.id||'')+' '+(typeof el.className==='string'?el.className:'')+' '+(el.getAttribute&&el.getAttribute('aria-label')||''));var transparent=parseFloat(cs.opacity||'1')<0.22||cs.backgroundColor==='rgba(0, 0, 0, 0)'||cs.backgroundColor==='transparent';var marked=/(^|[ _-])(ad|ads|advert|popup|popunder|interstitial|clickunder|overlay|sponsor)([ _-]|$)/i.test(meta);var clickable=!!(el.onclick||(el.getAttribute&&el.getAttribute('onclick'))||cs.cursor==='pointer'||el.tagName==='A'||el.tagName==='BUTTON');return clickable&&(marked||((p==='fixed'||p==='absolute'||p==='sticky')&&transparent&&z>=10));}catch(e){return false;}}"
                + "function eventPoint(e){try{var p=e;if(e&&e.changedTouches&&e.changedTouches.length)p=e.changedTouches[0];else if(e&&e.touches&&e.touches.length)p=e.touches[0];var x=p&&Number(p.clientX),y=p&&Number(p.clientY);return isFinite(x)&&isFinite(y)?{x:x,y:y}:null;}catch(x){return null;}}"
                + "function pointElements(e,blocked){try{var p=eventPoint(e);if(!p)return[];if(typeof D.elementsFromPoint==='function')return D.elementsFromPoint(p.x,p.y)||[];if(typeof D.elementFromPoint!=='function')return[];var style=blocked&&blocked.style,old=style?style.pointerEvents:'';if(style)style.pointerEvents='none';var below=D.elementFromPoint(p.x,p.y);if(style)style.pointerEvents=old;return below?[below]:[];}catch(x){return[];}}"
                + "function safeReaderAtPoint(e,blocked){try{if(!reader()||!e)return'';var list=pointElements(e,blocked),seen=[];for(var i=0;i<list.length;i++){var raw=list[i],el=raw&&raw.closest?raw.closest('a[href],area[href],button,[role=button],[role=link],[data-href],[data-url],[data-link],[data-next],[data-prev],[data-chapter-url]'):null;if(!el||el===blocked||seen.indexOf(el)>=0)continue;seen.push(el);var u=candidateUrl(el);if(u&&safeReaderNav(u,el))return abs(u);}}catch(x){}return'';}"
                + "function recoverReaderClick(e,blockedNode,blockedUrl){try{var safe=safeReaderAtPoint(e,blockedNode);if(!safe)return false;if(blockedUrl)deny(e,blockedUrl);else cancelEvent(e);if(S.readerNavLock)return true;S.readerNavLock=true;setTimeout(function(){try{location.assign(safe);}catch(x){try{location.href=safe;}catch(y){}}setTimeout(function(){S.readerNavLock=false;},500);},0);return true;}catch(x){return false;}}"
                + "function clickGuard(e){if(!S.config.enabled||!S.config.click||S.readerNavLock)return;try{var blocked=e.target&&e.target.closest?e.target.closest('a,button,[role=button],[role=link],[onclick],[style]'):e.target;var u=targetUrl(e.target);if(u&&safeReaderNav(u,e.target))return;if(u&&badNav(u,e.target)){if(recoverReaderClick(e,blocked,u))return;return deny(e,u);}if(!u&&clickSurfaceSuspicious(e.target)&&recoverReaderClick(e,blocked,''))return;}catch(x){}}"
                + "function submitGuard(e){if(!S.config.enabled||!S.config.redirect)return;try{var f=e.target;if(f&&f.action&&safeReaderNav(f.action,f))return;if(f&&f.action&&badNav(f.action,f))return deny(e,f.action);}catch(x){}}"
                + "function visible(el){try{var r=el.getBoundingClientRect(),cs=getComputedStyle(el);return r.width>2&&r.height>2&&cs.display!=='none'&&cs.visibility!=='hidden';}catch(e){return false;}}"
                + "function overlayBad(el){try{if(!visible(el)||navControl(el))return false;try{var safeChild=el.querySelector('a[href][rel=next],a[href][rel=prev],a[href][class*=next],a[href][class*=prev],a[href][class*=chapter],[aria-label*=next i],[aria-label*=prev i]');if(safeChild&&safeReaderNav(targetUrl(safeChild),safeChild))return false;}catch(x){}var cs=getComputedStyle(el),r=el.getBoundingClientRect(),vw=Math.max(1,innerWidth),vh=Math.max(1,innerHeight);var cover=(r.width*r.height)/(vw*vh);if(cover<0.55)return false;var pos=cs.position;if(pos!=='fixed'&&pos!=='absolute'&&pos!=='sticky')return false;var z=parseInt(cs.zIndex,10);if(!isFinite(z))z=0;var meta=low((el.id||'')+' '+(typeof el.className==='string'?el.className:'')+' '+(el.getAttribute('aria-label')||''));var href=targetUrl(el)||'';var child='';try{var n=el.querySelector('a[href],iframe[src],form[action]');if(n)child=n.href||n.src||n.action||'';}catch(x){}var marked=/(^|[ _-])(ad|ads|advert|popup|popunder|interstitial|clickunder|sponsor)([ _-]|$)/i.test(meta);var transparent=parseFloat(cs.opacity||'1')<0.18||cs.backgroundColor==='rgba(0, 0, 0, 0)';var clickable=!!(el.onclick||el.getAttribute('onclick')||el.getAttribute('role')==='link'||cs.cursor==='pointer');var meaningful=false;try{meaningful=!!el.querySelector('img,video,audio,[role=dialog],button,input,textarea,select')||String(el.innerText||el.textContent||'').trim().length>24;}catch(x){}return badNav(href,el)||badNav(child,el)||marked||(reader()&&cover>0.72&&z>=1000&&transparent&&clickable&&!meaningful);}catch(e){return false;}}"
                + "function clean(){if(!S.config.enabled)return;var removed=false;try{if(S.config.resource)D.querySelectorAll('iframe[src],script[src],ins[data-ad-client]').forEach(function(el){var u=el.src||el.getAttribute('data-ad-client')||'';if(u&&bad(u)){el.remove();removed=true;}});}catch(e){}try{if(S.config.click)D.querySelectorAll('[class*=popup],[class*=popunder],[class*=interstitial],[id*=popup],[id*=popunder],[id*=interstitial],[class*=overlay],[id*=overlay],[style*=z-index]').forEach(function(el){if(overlayBad(el)){el.style.setProperty('display','none','important');el.style.setProperty('pointer-events','none','important');removed=true;}});}catch(e){}if(removed){try{if(D.documentElement)D.documentElement.style.removeProperty('overflow');if(D.body)D.body.style.removeProperty('overflow');}catch(e){}}}"
                + "function schedule(){if(S.timer)return;S.timer=setTimeout(function(){S.timer=0;clean();},80);}"
                + "W.__yieldShieldV2SetConfig=function(n){try{if(n)Object.keys(n).forEach(function(k){S.config[k]=!!n[k];});schedule();}catch(e){}};"
                + "W.__yieldShieldV2Run=clean;"
                + "if(!S.installed){S.installed=true;"
                + "try{var nativeOpen=W.open;W.open=function(u,n,f){if(S.config.enabled&&S.config.popup&&badNav(u,null)){report(u);return{closed:true,focus:function(){},close:function(){}};}try{return nativeOpen.call(W,u,n,f);}catch(e){return null;}};}catch(e){}"
                + "function listen(type,fn,touch){try{D.addEventListener(type,fn,touch?{capture:true,passive:false}:true);}catch(e){try{D.addEventListener(type,fn,true);}catch(x){}}}"
                + "listen('click',clickGuard,false);listen('auxclick',clickGuard,false);listen('submit',submitGuard,false);"
                + "['touchstart','touchend','pointerdown','pointerup','mousedown','mouseup'].forEach(function(t){listen(t,clickGuard,true);});"
                + "try{var ac=HTMLAnchorElement.prototype.click;HTMLAnchorElement.prototype.click=function(){if(S.config.enabled&&S.config.click&&!safeReaderNav(this.href,this)&&badNav(this.href,this)){report(this.href);return;}return ac.call(this);};}catch(e){}"
                + "try{var fs=HTMLFormElement.prototype.submit;HTMLFormElement.prototype.submit=function(){if(S.config.enabled&&S.config.redirect&&!safeReaderNav(this.action,this)&&badNav(this.action,this)){report(this.action);return;}return fs.call(this);};}catch(e){}"
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
