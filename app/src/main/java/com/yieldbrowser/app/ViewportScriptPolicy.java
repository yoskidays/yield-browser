package com.yieldbrowser.app;

/** Pure JavaScript generation for mobile reset and desktop viewport locking. */
final class ViewportScriptPolicy {
    private static final int DESKTOP_WIDTH_PX = 1200;

    private ViewportScriptPolicy() {
    }

    static String mobileResetScript() {
        return "javascript:(function(){try{"
                + "var h=document.head||document.getElementsByTagName('head')[0]||document.documentElement;"
                + "var m=document.querySelector('meta[name=viewport]');"
                + "if(!m){m=document.createElement('meta');m.name='viewport';h.appendChild(m);}"
                + "m.setAttribute('content','width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes');"
                + "document.documentElement.style.removeProperty('min-width');"
                + "document.documentElement.style.removeProperty('width');"
                + "if(document.body){document.body.style.removeProperty('min-width');document.body.style.removeProperty('width');}"
                + "try{window.dispatchEvent(new Event('resize'));}catch(e){}"
                + "}catch(e){}})()";
    }

    static String desktopLockScript() {
        return "javascript:(function(){try{"
                + "var w=" + DESKTOP_WIDTH_PX + ";"
                + "var h=document.head||document.getElementsByTagName('head')[0]||document.documentElement;"
                + "var m=document.querySelector('meta[name=viewport]');"
                + "if(!m){m=document.createElement('meta');m.name='viewport';h.appendChild(m);}"
                + "m.setAttribute('content','width='+w+', initial-scale=1.0, maximum-scale=5.0, user-scalable=yes');"
                + "document.documentElement.style.minWidth=w+'px';"
                + "if(document.body){document.body.style.minWidth=w+'px';}"
                + "}catch(e){}})()";
    }
}
