package com.yieldbrowser.app;

import java.net.URI;
import java.util.Locale;

/**
 * Conservative YouTube page guard.
 *
 * The guard deliberately avoids blocking googlevideo or player network requests because
 * advertisements and the requested video may use the same delivery infrastructure.
 */
final class YouTubeAdGuardScript {
    private YouTubeAdGuardScript() {
    }

    static boolean shouldInstall(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) return false;
            scheme = scheme.toLowerCase(Locale.US);
            host = host.toLowerCase(Locale.US);
            if (!"http".equals(scheme) && !"https".equals(scheme)) return false;
            if (host.startsWith("www.")) host = host.substring(4);
            return host.equals("youtube.com")
                    || host.endsWith(".youtube.com")
                    || host.equals("youtube-nocookie.com")
                    || host.endsWith(".youtube-nocookie.com");
        } catch (Exception ignored) {
            return false;
        }
    }

    static String script() {
        return PAGE_SCRIPT;
    }

    private static final String PAGE_SCRIPT =
            "(function(){'use strict';try{"
                    + "if(window.__yieldYouTubeAdGuardV1){window.__yieldYouTubeAdGuardV1.run();return;}"
                    + "var S={timer:0,observer:null,mutedByGuard:false,oldMuted:false,oldRate:1,adSince:0};"
                    + "var SKIP='.ytp-ad-skip-button,.ytp-skip-ad-button,.ytp-ad-skip-button-modern,button.ytp-ad-skip-button,[id^=skip-button] button,ytd-button-renderer#skip-button button,.videoAdUiSkipButton,.ytp-ad-overlay-close-button';"
                    + "var HIDE='.ytp-ad-overlay-container,.ytp-ad-image-overlay,ytd-display-ad-renderer,ytd-promoted-sparkles-web-renderer,ytd-in-feed-ad-layout-renderer,ytd-ad-slot-renderer,ytd-banner-promo-renderer,ytd-statement-banner-renderer,ytd-companion-slot-renderer,ytd-player-legacy-desktop-watch-ads-renderer,#masthead-ad,ytm-promoted-sparkles-web-renderer';"
                    + "function visible(e){try{var r=e.getBoundingClientRect(),s=getComputedStyle(e);return r.width>0&&r.height>0&&s.display!=='none'&&s.visibility!=='hidden';}catch(x){return false;}}"
                    + "function clickSkips(){var clicked=false;try{document.querySelectorAll(SKIP).forEach(function(e){if(!visible(e))return;try{e.click();clicked=true;}catch(x){}});}catch(x){}return clicked;}"
                    + "function hideAds(){try{document.querySelectorAll(HIDE).forEach(function(e){try{e.style.setProperty('display','none','important');e.setAttribute('aria-hidden','true');}catch(x){}});}catch(x){}}"
                    + "function player(){return document.querySelector('#movie_player');}"
                    + "function video(){var p=player();return p&&p.querySelector?p.querySelector('video'):document.querySelector('video');}"
                    + "function adActive(){var p=player();return !!(p&&p.classList&&(p.classList.contains('ad-showing')||p.classList.contains('ad-interrupting')));}"
                    + "function restore(v){if(!S.mutedByGuard||!v)return;try{v.muted=S.oldMuted;v.playbackRate=S.oldRate>0?S.oldRate:1;}catch(x){}S.mutedByGuard=false;S.adSince=0;}"
                    + "function accelerateAd(skipClicked){var v=video();if(!v)return;if(!adActive()){restore(v);return;}var now=Date.now();if(!S.adSince)S.adSince=now;if(!S.mutedByGuard){S.oldMuted=!!v.muted;S.oldRate=v.playbackRate||1;S.mutedByGuard=true;}try{v.muted=true;if((v.playbackRate||1)<16)v.playbackRate=16;if(v.paused&&v.play)v.play().catch(function(){});}catch(x){}if(skipClicked||now-S.adSince<900)return;try{var d=Number(v.duration),c=Number(v.currentTime);if(isFinite(d)&&isFinite(c)&&d>1&&d<=120&&d-c>0.8)v.currentTime=Math.max(c,d-0.25);}catch(x){}}"
                    + "function run(){var skipped=clickSkips();hideAds();accelerateAd(skipped);}"
                    + "S.run=run;window.__yieldYouTubeAdGuardV1=S;"
                    + "function start(){if(S.observer||S.timer)return;try{S.observer=new MutationObserver(function(){run();});S.observer.observe(document.documentElement||document,{childList:true,subtree:true,attributes:true,attributeFilter:['class','style','hidden']});}catch(x){}S.timer=setInterval(run,500);run();}"
                    + "if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',start,{once:true});else start();"
                    + "}catch(e){}})();";
}
