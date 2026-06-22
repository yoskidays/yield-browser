package com.yieldbrowser.app;

/**
 * Builds the page-side darkening engine used as a mandatory fallback on Android WebView.
 *
 * <p>AndroidX algorithmic darkening is still enabled when available, but some Android 11
 * WebView providers report the feature as supported without visibly transforming a page.
 * This script therefore applies a reversible DOM-level dark palette as well.</p>
 */
final class NightModePageScript {
    private NightModePageScript() {
    }

    static String enable(boolean compatibilityMode) {
        String mode = compatibilityMode ? "compat" : "normal";
        String script = """
                (function(){
                  'use strict';
                  try {
                    var VERSION='0.9.98';
                    var MODE='__YIELD_NIGHT_MODE__';
                    var root=document.documentElement;
                    if(!root)return 'no-root';

                    var previous=window.__yieldNightEngine;
                    if(previous&&previous.version===VERSION){
                      previous.mode=MODE;
                      previous.enable();
                      previous.scan(document);
                      return 'refreshed';
                    }
                    if(previous&&typeof previous.disable==='function'){
                      try{previous.disable();}catch(ignore){}
                    }

                    var STYLE_ID='yield-night-style';
                    var originals=new WeakMap();
                    var touched=[];
                    var observer=null;
                    var scanTimer=0;
                    var enabled=true;
                    var oldMeta=null;
                    var meta=document.querySelector('meta[name="color-scheme"]');
                    if(meta)oldMeta=meta.getAttribute('content');

                    var SKIP_TAGS={IMG:1,VIDEO:1,CANVAS:1,SVG:1,PICTURE:1,SOURCE:1,IFRAME:1,OBJECT:1,EMBED:1,SCRIPT:1,STYLE:1,LINK:1,META:1,HEAD:1,NOSCRIPT:1};
                    var PROPS=['background-color','color','border-top-color','border-right-color','border-bottom-color','border-left-color','outline-color','text-decoration-color','caret-color','fill','stroke'];

                    function parseColor(value){
                      if(!value||value==='transparent')return null;
                      var m=String(value).match(/^rgba?\\(\\s*([\\d.]+)[,\\s]+([\\d.]+)[,\\s]+([\\d.]+)(?:\\s*[,\\/]\\s*([\\d.]+))?\\s*\\)$/i);
                      if(!m)return null;
                      return {r:+m[1],g:+m[2],b:+m[3],a:m[4]===undefined?1:+m[4]};
                    }
                    function channel(v){v/=255;return v<=0.03928?v/12.92:Math.pow((v+0.055)/1.055,2.4);}
                    function luminance(c){return c?0.2126*channel(c.r)+0.7152*channel(c.g)+0.0722*channel(c.b):0;}
                    function remember(el){
                      if(originals.has(el))return;
                      var state={};
                      for(var i=0;i<PROPS.length;i++){
                        var p=PROPS[i];
                        state[p]=[el.style.getPropertyValue(p),el.style.getPropertyPriority(p)];
                      }
                      originals.set(el,state);
                      touched.push(el);
                    }
                    function set(el,prop,value){
                      remember(el);
                      if(el.style.getPropertyValue(prop)!==value||el.style.getPropertyPriority(prop)!=='important'){
                        el.style.setProperty(prop,value,'important');
                      }
                    }
                    function isSkipped(el){
                      if(!el||el.nodeType!==1)return true;
                      if(SKIP_TAGS[el.tagName])return true;
                      if(el.hasAttribute('data-yield-night-ignore'))return true;
                      return false;
                    }
                    function isFormControl(el){return /^(INPUT|TEXTAREA|SELECT|BUTTON|OPTION)$/.test(el.tagName);}
                    function darkBackground(c){
                      var l=luminance(c);
                      if(l>0.88)return '#111318';
                      if(l>0.72)return '#171a20';
                      if(l>0.52)return '#20242b';
                      if(l>0.36)return '#292e36';
                      return null;
                    }
                    function lightForeground(c){
                      var l=luminance(c);
                      if(l<0.08)return '#eef1f5';
                      if(l<0.20)return '#e2e6eb';
                      if(l<0.38)return '#cfd5dc';
                      return null;
                    }
                    function borderColor(c){
                      var l=luminance(c);
                      if(l>0.60||l<0.10)return '#454b55';
                      return null;
                    }
                    function processElement(el){
                      if(!enabled||isSkipped(el))return;
                      var cs;
                      try{cs=getComputedStyle(el);}catch(e){return;}
                      if(!cs)return;

                      var bg=parseColor(cs.backgroundColor);
                      var bgValue=bg&&bg.a>0.08?darkBackground(bg):null;
                      if(bgValue){
                        // Preserve image/gradient surfaces; darken their surrounding layer instead.
                        if(!cs.backgroundImage||cs.backgroundImage==='none')set(el,'background-color',bgValue);
                      }

                      var fg=parseColor(cs.color);
                      var fgValue=lightForeground(fg);
                      if(fgValue)set(el,'color',fgValue);

                      var borders=['border-top-color','border-right-color','border-bottom-color','border-left-color','outline-color','text-decoration-color'];
                      for(var i=0;i<borders.length;i++){
                        var bc=parseColor(cs.getPropertyValue(borders[i]));
                        var bv=borderColor(bc);
                        if(bv)set(el,borders[i],bv);
                      }

                      if(isFormControl(el)){
                        set(el,'background-color','#20242b');
                        set(el,'color','#eef1f5');
                        set(el,'caret-color','#f4c542');
                      }
                    }
                    function collect(scope){
                      var out=[];
                      if(scope&&scope.nodeType===1)out.push(scope);
                      try{
                        var selector=(scope===document||scope===document.documentElement)?'body,body *':'*';
                        var base=(scope&&scope.querySelectorAll)?scope:document;
                        var list=base.querySelectorAll(selector);
                        for(var i=0;i<list.length;i++)out.push(list[i]);
                      }catch(e){}
                      return out;
                    }
                    function processBatch(nodes,index){
                      if(!enabled)return;
                      var end=Math.min(index+280,nodes.length);
                      for(var i=index;i<end;i++)processElement(nodes[i]);
                      if(end<nodes.length){
                        (window.requestAnimationFrame||function(fn){setTimeout(fn,16);})(function(){processBatch(nodes,end);});
                      }
                    }
                    function scan(scope){
                      if(!enabled)return;
                      var nodes=collect(scope||document);
                      processBatch(nodes,0);
                    }
                    function installStyle(){
                      var style=document.getElementById(STYLE_ID);
                      if(!style){
                        style=document.createElement('style');
                        style.id=STYLE_ID;
                        (document.head||root).appendChild(style);
                      }
                      style.textContent=''
                        +':root{color-scheme:dark!important;background:#0b0d10!important;}'
                        +'html.yield-night-mode,html.yield-night-mode body{background:#0b0d10!important;color:#e8eaed!important;}'
                        +'html.yield-night-mode input,html.yield-night-mode textarea,html.yield-night-mode select,html.yield-night-mode button{color-scheme:dark!important;}'
                        +'html.yield-night-mode a{color:#8ab4f8!important;}'
                        +'html.yield-night-mode ::selection{background:#705d00!important;color:#fff!important;}'
                        +'html.yield-night-mode img,html.yield-night-mode video,html.yield-night-mode canvas,html.yield-night-mode svg,html.yield-night-mode picture,html.yield-night-mode iframe{filter:none!important;mix-blend-mode:normal!important;}'
                        +'html.yield-night-mode ::-webkit-scrollbar{background:#111318!important;}'
                        +'html.yield-night-mode ::-webkit-scrollbar-thumb{background:#4a505a!important;border-radius:8px!important;}';
                    }
                    function enable(){
                      enabled=true;
                      installStyle();
                      root.classList.add('yield-night-mode');
                      root.setAttribute('data-yield-night-mode',VERSION);
                      var m=document.querySelector('meta[name="color-scheme"]');
                      if(!m){m=document.createElement('meta');m.name='color-scheme';(document.head||root).appendChild(m);}
                      m.setAttribute('content','dark light');
                      root.style.setProperty('color-scheme','dark','important');
                      if(document.body)document.body.style.setProperty('color-scheme','dark','important');
                      if(!observer&&document.body){
                        observer=new MutationObserver(function(records){
                          if(!enabled)return;
                          var added=[];
                          for(var i=0;i<records.length;i++){
                            var r=records[i];
                            if(r.type==='childList'){
                              for(var n=0;n<r.addedNodes.length;n++)if(r.addedNodes[n].nodeType===1)added.push(r.addedNodes[n]);
                            }else if(r.type==='attributes'&&r.target&&r.target.nodeType===1){
                              added.push(r.target);
                            }
                          }
                          if(!added.length)return;
                          clearTimeout(scanTimer);
                          scanTimer=setTimeout(function(){for(var j=0;j<added.length;j++)scan(added[j]);},90);
                        });
                        observer.observe(document.body,{subtree:true,childList:true,attributes:true,attributeFilter:['class','hidden']});
                      }
                    }
                    function disable(){
                      enabled=false;
                      clearTimeout(scanTimer);
                      if(observer){try{observer.disconnect();}catch(e){}observer=null;}
                      for(var i=touched.length-1;i>=0;i--){
                        var el=touched[i],state=originals.get(el);
                        if(!el||!state)continue;
                        for(var p=0;p<PROPS.length;p++){
                          var prop=PROPS[p],old=state[prop];
                          if(!old)continue;
                          if(old[0])el.style.setProperty(prop,old[0],old[1]||'');
                          else el.style.removeProperty(prop);
                        }
                      }
                      touched=[];
                      originals=new WeakMap();
                      var style=document.getElementById(STYLE_ID);if(style)style.remove();
                      root.classList.remove('yield-night-mode');
                      root.removeAttribute('data-yield-night-mode');
                      root.style.removeProperty('color-scheme');
                      if(document.body)document.body.style.removeProperty('color-scheme');
                      var m=document.querySelector('meta[name="color-scheme"]');
                      if(m){if(oldMeta===null)m.remove();else m.setAttribute('content',oldMeta);}
                    }

                    window.__yieldNightEngine={version:VERSION,mode:MODE,enable:enable,disable:disable,scan:scan};
                    enable();
                    scan(document);
                    setTimeout(function(){scan(document);},350);
                    setTimeout(function(){scan(document);},1200);
                    return 'enabled';
                  } catch(e) { return 'error:'+String(e); }
                })();
                """;
        return script.replace("__YIELD_NIGHT_MODE__", mode);
    }

    static String disable() {
        return """
                (function(){
                  'use strict';
                  try{
                    if(window.__yieldNightEngine&&typeof window.__yieldNightEngine.disable==='function'){
                      window.__yieldNightEngine.disable();
                      delete window.__yieldNightEngine;
                    }
                    var style=document.getElementById('yield-night-style');if(style)style.remove();
                    var root=document.documentElement;
                    if(root){root.classList.remove('yield-night-mode');root.removeAttribute('data-yield-night-mode');root.style.removeProperty('color-scheme');}
                    if(document.body)document.body.style.removeProperty('color-scheme');
                    return 'disabled';
                  }catch(e){return 'error:'+String(e);}
                })();
                """;
    }
}
