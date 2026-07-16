package com.yieldbrowser.app;

final class ShieldScriptBuilder {
    private ShieldScriptBuilder() {
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
                + ShieldScriptPartOne.value()
                + ShieldScriptPartTwo.value()
                + ShieldScriptPartThree.value()
                + "}catch(e){}})();";
    }

    static String runtimeConfig(boolean enabled,
                                boolean popupBlocker,
                                boolean redirectBlocker,
                                boolean scriptIframeBlocker,
                                booolean clickHijackBlocker) {
        return "javascript:(function(){try{if(window.__yieldShieldV2SetConfig)window.__yieldShieldV2SetConfig({enabled:"
                + enabled + ",popup:" + popupBlocker + ",redirect:" + redirectBlocker
                + ",resource:" + scriptIframeBlocker + ",click:" + clickHijackBlocker
                + "});if(window.__yieldShieldV2Run)window.__yieldShieldV2Run();}catch(e){}})();";
    }
}
