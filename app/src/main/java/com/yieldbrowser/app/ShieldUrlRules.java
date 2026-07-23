package com.yieldbrowser.app;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.regex.Pattern;

final class ShieldUrlRules {
    static final Pattern SAFE_CONTENT_PATH = Pattern.compile(
            "(?:^|[/_-])(?:manga|manhwa|manhua|comic|komik|chapter|chapitre|capitulo|episode|reader|read(?:-online)?|reading|baca|novel)(?:[/_-]|$)",
            Pattern.CASE_INSENSITIVE);

    static final Pattern DOWNLOAD_PAGE_PATH = Pattern.compile(
            "(?:^|[/_-])(?:download(?:er|ing)?|unduh|get-file|file-download|download-file|episode-download|download-episode|download-drama)(?:[/_-]|$)",
            Pattern.CASE_INSENSITIVE);

    static final Pattern DOWNLOAD_LISTING_HOST = Pattern.compile(
            "(?:^|\\.)(?:dramaencode\\.net)$",
            Pattern.CASE_INSENSITIVE);

    static final Pattern AD_HEAVY_PORTAL_HOST = Pattern.compile(
            "(?:^|\\.)oploverz\\.[a-z0-9-]+$",
            Pattern.CASE_INSENSITIVE);

    static final Pattern PORTAL_ALLOWED_THIRD_PARTY_HOST = Pattern.compile(
            "(?:^|\\.)(?:cloudflare\\.com|disqus\\.com|disquscdn\\.com|facebook\\.com|fbcdn\\.net|facebook\\.net|googleapis\\.com|gstatic\\.com)$",
            Pattern.CASE_INSENSITIVE);

    static final Pattern DOWNLOAD_TARGET_HINT = Pattern.compile(
            "(?:^|[/_.?&=\\-])(?:download|unduh|get-file|file|files|media|mirror|server|dl)(?:[/_.?&=\\-]|$)",
            Pattern.CASE_INSENSITIVE);

    static final Pattern POPUP_ISOLATED_VIDEO_PATH = Pattern.compile(
            "(?:^|[/_-])(?:video|videos|watch|movie|movies|film|stream|player|embed|play)(?:[/_-]|$)",
            Pattern.CASE_INSENSITIVE);

    static final Pattern TRUSTED_VIDEO_HOST = Pattern.compile(
            "(?:^|\\.)(?:youtube\\.[a-z.]+|youtu\\.be|googlevideo\\.com|ytimg\\.com|ggpht\\.com|vimeo\\.com|dailymotion\\.com)$",
            Pattern.CASE_INSENSITIVE);

    static final Pattern SEARCH_PATH_HINT = Pattern.compile(
            "(?:^|/)(?:search|search-results?|results?|web|html|find|s)(?:/|$|\\.)",
            Pattern.CASE_INSENSITIVE);

    static final Pattern SEARCH_QUERY_HINT = Pattern.compile(
            "(?:^|[?&])(?:q|query|p|text|wd|keyword|keywords|search|search_query|term)=",
            Pattern.CASE_INSENSITIVE);

    static final Pattern KNOWN_SEARCH_HOST = Pattern.compile(
            "(?:^|\\.)(?:google\\.[a-z.]+|bing\\.com|duckduckgo\\.com|yahoo\\.[a-z.]+|yandex\\.[a-z.]+|baidu\\.com|brave\\.com|startpage\\.com|ecosia\\.org|qwant\\.com|mojeek\\.com|kagi\\.com|naver\\.com|aol\\.com|ask\\.com|swisscows\\.com|metager\\.[a-z.]+)$",
            Pattern.CASE_INSENSITIVE);

    static final Pattern RELAY_SEGMENT = Pattern.compile(
            "(?:^|/)(?:r|go|out|away|jump|visit|redirect|redir|click|link|external|open|track|tracking|offer|offers|promo|ads?|interstitial)(?:/|$)",
            Pattern.CASE_INSENSITIVE);

    static final Pattern OPAQUE_SEGMENT = Pattern.compile(
            "(?:^|/)[a-z0-9_-]{16,}(?:/|$)", Pattern.CASE_INSENSITIVE);

    static final Pattern REDIRECT_PARAMETER = Pattern.compile(
            "(?:[?&](?:url|u|to|target|dest|destination|redirect|redirect_url|redirect_uri|redir|r|go|out|link|click|next|continue|return|return_to|return_url|navigate_url)=)",
            Pattern.CASE_INSENSITIVE);

    static final Pattern HARD_AD_TOKEN = Pattern.compile(
            "(?:adclick|ad_click|adurl|clickunder|onclickads|popunder|popupads|interstitial|affiliate|aff_sub|af_click|click_id|campaign_id|tracking_id|utm_medium=affiliates|deep_and_deferred|navigate_url|reactpath)",
            Pattern.CASE_INSENSITIVE);

    static final Pattern KNOWN_AD_HOST = Pattern.compile(
            "(?:^|\\.)(?:doubleclick\\.net|googlesyndication\\.com|googleadservices\\.com|adservice\\.google\\.[a-z.]+|onclickads\\.net|clickadu\\.com|popads\\.net|popcash\\.net|propellerads\\.com|adsterra\\.com|hilltopads\\.net|exoclick\\.com|trafficjunky\\.net|juicyads\\.com|admaven\\.com|realsrv\\.com|taboola\\.com|outbrain\\.com|mgid\\.com|revcontent\\.com|hotterydiseur\\.[a-z.]+|sewarsremeets\\.[a-z.]+|invest-tracing\\.[a-z.]+|moonlighttha[a-z0-9.-]*)$",
            Pattern.CASE_INSENSITIVE);

    static final Pattern TRUSTED_DOWNLOAD_HOST = Pattern.compile(
            "(?:^|\\.)(?:drive\\.usercontent\\.google\\.com|drive\\.google\\.com|docs\\.google\\.com|googleusercontent\\.com|github\\.com|githubusercontent\\.com|sourceforge\\.net|mediafire\\.com|dropbox\\.com|dropboxusercontent\\.com|onedrive\\.live\\.com|1drv\\.ms|mega\\.nz|pixeldrain\\.com|gofile\\.io|archive\\.org|uptobox\\.(?:com|net)|files\\.fm|mp4upload\\.(?:com|net)|safefileku\\.(?:com|net)|fastdown\\.io|fast-down\\.com|streamlare\\.(?:com|net)|hxfile\\.(?:co|to|com)|racaty\\.(?:net|io)|zippyshare\\.(?:com|net)|doodrive\\.(?:com|net)|letsupload\\.(?:io|cc|co|com)|solidfiles\\.com|filemoon\\.sx|upstream\\.to|vidguard\\.to|hexupload\\.net|send\\.cm|megaup\\.net|streamwish\\.to|streamtape\\.com|krakenfiles\\.com|terabox\\.com|teraboxapp\\.com|mirrorace\\.(?:org|com)|mirrored\\.to|multiup\\.io|acefile\\.co|katfile\\.com|clicknupload\\.(?:to|co)|userscloud\\.com|uploadrar\\.com|dailyuploads\\.net|upload\\.ee|workupload\\.com|qiwi\\.gg|buzzheavier\\.com|filedon\\.co|1fichier\\.com|rapidgator\\.net|turbobit\\.net)$",
            Pattern.CASE_INSENSITIVE);

    static final Pattern CHEAP_AD_TLD = Pattern.compile(
            ".*\\.(?:click|cfd|cam|monster|quest|buzz|icu|cyou|xyz|top|shop|site|space|online|live|fun|lol)$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DIRECT_CONTENT_ASSET = Pattern.compile(
            ".*\\.(?:avif|bmp|gif|ico|jpe?g|png|svg|webp|woff2?|ttf|otf|mp4|m4v|mov|webm|mkv|m3u8|mpd|m4s|ts|mp3|aac|wav|ogg|pdf|zip|rar|7z)$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SCRIPT_LIKE_RESOURCE = Pattern.compile(
            "(?:.*\\.js$|(?:^|/)(?:js|javascript|scripts?)(?:/|$)|(?:^|[/_-])(?:pop(?:up|under)?|ads?|advert|tag|loader)(?:[/_.-]|$))",
            Pattern.CASE_INSENSITIVE);

    private ShieldUrlRules() {
    }

    static boolean isDirectContentAsset(String url) {
        if (!isHttpOrHttps(url)) return false;
        String path = pathOf(url);
        return !path.isEmpty() && DIRECT_CONTENT_ASSET.matcher(path).matches();
    }

    static boolean isScriptLikeResource(String url) {
        if (!isHttpOrHttps(url)) return false;
        String path = pathOf(url);
        return !path.isEmpty() && SCRIPT_LIKE_RESOURCE.matcher(path).find();
    }

    static boolean isRelayPath(String url) {
        return RELAY_SEGMENT.matcher(pathOf(url)).find();
    }

    static boolean hasRedirectParameter(String url) {
        return url != null && REDIRECT_PARAMETER.matcher(decodedLower(url)).find();
    }

    static boolean hasHardAdToken(String url) {
        return url != null && HARD_AD_TOKEN.matcher(decodedLower(url)).find();
    }

    static boolean hasOpaquePath(String url) {
        return OPAQUE_SEGMENT.matcher(pathOf(url)).find();
    }

    static boolean isKnownAdHost(String host) {
        return host != null && KNOWN_AD_HOST.matcher(host).find();
    }

    static boolean isKnownDownloadListingHost(String host) {
        return host != null && DOWNLOAD_LISTING_HOST.matcher(host).find();
    }

    static boolean isAdHeavyPortalHost(String host) {
        return host != null && AD_HEAVY_PORTAL_HOST.matcher(host).find();
    }

    static boolean isAllowedPortalThirdPartyHost(String host) {
        return host != null && PORTAL_ALLOWED_THIRD_PARTY_HOST.matcher(host).find();
    }

    static boolean isTrustedDownloadHost(String host) {
        return host != null && TRUSTED_DOWNLOAD_HOST.matcher(host).find();
    }

    static boolean isCheapAdHost(String host) {
        return host != null && CHEAP_AD_TLD.matcher(host).matches();
    }

    static boolean isDangerousExternalScheme(String url) {
        if (url == null) return false;
        String lower = url.trim().toLowerCase(Locale.US);
        return lower.startsWith("intent:")
                || lower.startsWith("market:")
                || lower.startsWith("shopeeid:")
                || lower.startsWith("lazada:")
                || lower.startsWith("tokopedia:");
    }

    static boolean isHttpOrHttps(String url) {
        if (url == null) return false;
        String lower = url.trim().toLowerCase(Locale.US);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    static String decodedLower(String url) {
        if (url == null) return "";
        String lower = url.toLowerCase(Locale.US);
        try {
            return URLDecoder.decode(lower, "UTF-8");
        } catch (Exception ignored) {
            return lower;
        }
    }

    static String hostOf(String url) {
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

    static String pathOf(String url) {
        if (url == null) return "";
        try {
            URI parsed = URI.create(url.trim());
            String path = parsed.getPath();
            return path == null ? "" : path.toLowerCase(Locale.US);
        } catch (Exception ignored) {
            return "";
        }
    }

    static String queryOf(String url) {
        if (url == null) return "";
        try {
            URI parsed = URI.create(url.trim());
            String query = parsed.getRawQuery();
            return query == null ? "" : "?" + query.toLowerCase(Locale.US);
        } catch (Exception ignored) {
            return "";
        }
    }

    static boolean sameSite(String first, String second) {
        return first.equals(second)
                || first.endsWith("." + second)
                || second.endsWith("." + first);
    }
}
