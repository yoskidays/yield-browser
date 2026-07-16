package com.yieldbrowser.app;

final class ShieldScriptPartOne {
    private ShieldScriptPartOne() {
    }

    static String value() {
        return "function low(v){return String(v||'').toLowerCase();}"
                + "function dec(v){try{return decodeURIComponent(String(v||''));}catch(e){return String(v||'');}}"
                + "function abs(u){try{return new URL(String(u||''),location.href).href;}catch(e){return String(u||'');}}"
                + "function host(u){try{return(new URL(abs(u))).hostname.replace(/^www\\./,'').toLowerCase();}catch(e){return'';}}"
                + "function path(u){try{return(new URL(abs(u))).pathname.toLowerCase();}catch(e){return'';}}"
                + "function same(a,b){return !!a&&!!b&&(a===b||a.endsWith('.'+b)||b.endsWith('.'+a));}"
                + "function asset(u){return /\\.(?:avif|bmp|gif|ico|jpe?g|png|svg|webp|woff2?|ttf|otf|mp4|m4v|mov|webm|mkv|m3u8|mpd|m4s|ts|mp3|aac|wav|ogg|pdf|zip|rar|7z)$/i.test(path(u));}"
                + "function contentPath(p){return /(?:^|[\\/_-])(?:manga|manhwa|manhua|comic|komik|chapter|chapitre|capitulo|episode|reader|read(?:-online)?|reading|baca|novel)(?:[\\/_-]|$)/i.test(p||'');}"
                + "function downloadPath(p){return /(?:^|[\\/_-])(?:download(?:er|ing)?|unduh|get-file|file-download|download-file|episode-download|download-episode|download-drama)(?:[\\/_-]|$)/i.test(p||'');}"
                + "function downloadTarget(u){return /(?:^|[\\/_.?&=\\-])(?:download|unduh|get-file|file|files|media|mirror|server|dl)(?:[\\/_.?&=\\-]|$)/i.test(dec(abs(u)));}"
                + "function videoPath(p){return /(?:^|[\\/_-])(?:video|videos|watch|movie|movies|film|stream|player|embed|play)(?:[\\/_-]|$)/i.test(p||'');}"
                + "function trustedVideoHost(h){return /(?:^|\\.)(?:youtube\\.[a-z.]+|youtu\\.be|googlevideo\\.com|ytimg\\.com|ggpht\\.com|vimeo\\.com|dailymotion\\.com)$/i.test(h||'');}"
                + "function searchPage(){try{var h=host(location.href),p=path(location.href),q=low(location.search||'');var known=/(?:^|\\.)(?:google\\.[a-z.]+|bing\\.com|duckduckgo\\.com|yahoo\\.[a-z.]+|yandex\\.[a-z.]+|baidu\\.com|brave\\.com|startpage\\.com|ecosia\\.org|qwant\\.com|mojeek\\.com|kagi\\.com|naver\\.com|aol\\.com|ask\\.com|swisscows\\.com|metager\\.[a-z.]+)$/i.test(h);var qp=/(?:^|[?&])(?:q|query|p|text|wd|keyword|keywords|search|search_query|term)=/i.test(q);var pp=/(?:^|\\/)(?:search|search-results?|results?|web|html|find|s)(?:\\/|$|\\.)/i.test(p);return (known&&(qp||pp))||(pp&&qp);}catch(e){return false;}}"
                + "function downloadPage(){if(searchPage())return false;return downloadPath(location.pathname||'');}"
                + "function reader(){if(searchPage())return false;if(downloadPage())return false;var p=location.pathname||'';if(contentPath(p))return true;try{return D.querySelectorAll('img[data-src],img[data-lazy-src],img[data-original],picture source[data-srcset]').length>=3;}catch(e){return false;}}"
                + "function isolated(){if(searchPage())return false;var h=host(location.href),p=location.pathname||'';return reader()||downloadPage()||(!trustedVideoHost(h)&&videoPath(p));}"
                + "function relay(u){var p=path(u);return /(?:^|\\/)(?:r|go|out|away|jump|visit|redirect|redir|click|link|external|open|track|tracking|offer|offers|promo|ads?|interstitial)(?:\\/|$)/i.test(p);}"
                + "function redirParam(u){return /[?&](?:url|u|to|target|dest|destination|redirect|redirect_url|redirect_uri|redir|r|go|out|link|click|next|continue|return|return_to|return_url|navigate_url)=/i.test(dec(u));}"
                + "function hardToken(u){return /(?:adclick|ad_click|adurl|clickunder|onclickads|popunder|popupads|interstitial|affiliate|aff_sub|af_click|click_id|campaign_id|tracking_id|utm_medium=affiliates|deep_and_deferred|navigate_url|reactpath)/i.test(dec(u));}"
                + "function adHost(h){return /(?:^|\\.)(?:doubleclick\\.net|googlesyndication\\.com|googleadservices\\.com|adservice\\.google\\.[a-z.]+|onclickads\\.net|clickadu\\.com|popads\\.net|popcash\\.net|propellerads\\.com|adsterra\\.com|hilltopads\\.net|exoclick\\.com|trafficjunky\\.net|juicyads\\.com|admaven\\.com|realsrv\\.com|taboola\\.com|outbrain\\.com|mgid\\.com|revcontent\\.com|hotterydiseur\\.[a-z.]+|sewarsremeets\\.[a-z.]+|invest-tracing\\.[a-z.]+)$/i.test(h||'');}"
                + "function cheap(h){return /\\.(?:click|cfd|cam|monster|quest|buzz|icu|cyou|xyz|top|shop|site|space|online|live|fun|lol)$/i.test(h||'');}"
                + "function navMeta(node){try{var el=node&&node.closest?node.closest('a[href],area[href],button,[role=button],form[action]'):null;if(!el)return'';var rel=low(el.getAttribute&&el.getAttribute('rel'));var meta=low((el.id||'')+' '+(typeof el.className==='string'?el.className:'')+' '+(el.getAttribute&&el.getAttribute('aria-label')||'')+' '+(el.getAttribute&&el.getAttribute('title')||'')+' '+(el.innerText||el.textContent||''));return rel+' '+meta;}catch(e){return'';}}"
                + "function navControl(node){return /(?:^|[ _-])(next|prev|previous|chapter|chapters|episode|bab|lanjut|selanjut|berikut|sebelum|daftar[ _-]*chapter)(?:[ _-]|$)/i.test(navMeta(node));}"
                + "function contentRegion(node){try{return !!(node&&node.closest&&node.closest('article,main,.entry-content,.post-content,.article-content,.post-body,.download-links,.download-box,.link-download,[class*=download-link],[id*=download-link]'));}catch(e){return false;}}"
                + "function downloadControl(node){try{var el=node&&node.closest?node.closest('a[href],area[href],button,[role=button],form[action]'):null;if(!el)return false;var m=navMeta(el);if(/(?:^|[ _-])(ad|ads|advert|sponsor|promo|affiliate|popup|popunder)(?:[ _-]|$)/i.test(m))return false;var named=/(?:^|[ _-])(download|unduh|save|mirror|server|quality|resolution|batch|480p|720p|1080p|2160p)(?:[ _-]|$)/i.test(m);return !!(el.hasAttribute&&el.hasAttribute('download'))||(named&&contentRegion(el));}catch(e){return false;}}"
                + "function cleanDownload(u){var a=abs(u),h=host(a);return /^https?:/i.test(a)&&!!h&&!adHost(h)&&!cheap(h)&&!hardToken(a)&&!redirParam(a)&&!relay(a);}"
                + "function safeDownloadNav(u,node){if(!downloadPage()||!u)return false;var a=abs(u),h=host(a),c=host(location.href);if(!cleanDownload(a))return false;if(asset(a))return true;if(h&&c&&same(h,c))return true;return downloadControl(node)&&downloadTarget(a);}"
                + "function safeReaderNav(u,node){var a=abs(u),h=host(a),c=host(location.href),p=path(a);if(downloadPage()||!h||!c||!same(h,c)||asset(a)||relay(a)||hardToken(a))return false;if(contentPath(p))return true;return reader()&&navControl(node)&&!redirParam(a);}";
    }
}
