package com.yieldbrowser.app;

/** Builds the persistent element-picker UI used by the manual cosmetic filter feature. */
final class ElementPickerScript {
    private ElementPickerScript() {
    }

    static String build() {
        return "javascript:" + """
                (function(){
                  'use strict';
                  if(window.__yieldPickerActive){
                    try{if(window.__yieldPickerContinue)window.__yieldPickerContinue();}catch(e){}
                    return;
                  }
                  window.__yieldPickerActive=true;
                  var cur=null;
                  var listeners=[];
                  var DEFAULT_TEXT='Mode Blokir Elemen — ketuk elemen/iklan';
                  var lastPickAt=0;
                  var noticeTimer=0;

                  var overlay=document.createElement('div');
                  overlay.id='__yield_picker_overlay';
                  overlay.style.cssText='position:fixed;z-index:2147483646;pointer-events:none;background:rgba(243,154,34,0.25);border:2px solid #F39A22;box-shadow:0 0 0 100000px rgba(0,0,0,0.10);border-radius:3px;left:0;top:0;width:0;height:0;display:none;box-sizing:border-box;';

                  var bar=document.createElement('div');
                  bar.id='__yield_picker_bar';
                  bar.style.cssText='position:fixed;z-index:2147483647;left:0;right:0;top:0;min-height:48px;background:#15171C;color:#F39A22;font:600 14px sans-serif;padding:0 8px 0 14px;display:flex;align-items:center;justify-content:center;border-bottom:1px solid #F39A22;box-sizing:border-box;pointer-events:auto;';

                  var label=document.createElement('div');
                  label.id='__yield_picker_label';
                  label.textContent=DEFAULT_TEXT;
                  label.style.cssText='flex:1;text-align:center;padding:10px 4px 10px 38px;line-height:20px;pointer-events:none;';

                  var closeBtn=document.createElement('button');
                  closeBtn.id='__yield_picker_close';
                  closeBtn.type='button';
                  closeBtn.setAttribute('aria-label','Tutup mode blokir elemen');
                  closeBtn.textContent='×';
                  closeBtn.style.cssText='width:40px;height:40px;min-width:40px;margin-left:4px;padding:0;border:0;border-radius:20px;background:transparent;color:#F39A22;font:400 30px/38px sans-serif;text-align:center;cursor:pointer;-webkit-tap-highlight-color:transparent;';

                  bar.appendChild(label);
                  bar.appendChild(closeBtn);

                  function attach(){
                    try{
                      var p=document.body||document.documentElement;
                      if(overlay.parentNode!==p)p.appendChild(overlay);
                      if(bar.parentNode!==p)p.appendChild(bar);
                    }catch(e){}
                  }
                  attach();

                  function isPickerUi(el){
                    try{return !!el&&(el===overlay||el===bar||bar.contains(el));}catch(e){return false;}
                  }

                  function notice(text){
                    try{
                      label.textContent=text||DEFAULT_TEXT;
                      if(noticeTimer)clearTimeout(noticeTimer);
                      noticeTimer=setTimeout(function(){try{label.textContent=DEFAULT_TEXT;}catch(e){}},1400);
                    }catch(e){}
                  }

                  function unique(sel){
                    try{return document.querySelectorAll(sel).length===1;}catch(e){return false;}
                  }

                  function stableId(node){
                    try{
                      var id=(node.id||'').trim();
                      if(!/^[A-Za-z][A-Za-z0-9_-]*$/.test(id))return '';
                      if(id.length>64||/\\d{5,}/.test(id)||/^(?:ember|react|vue|ng|rand|uuid)[-_]?/i.test(id))return '';
                      return id;
                    }catch(e){return '';}
                  }

                  function stableClasses(node){
                    var out=[];
                    try{
                      var raw=(node.className&&node.className.baseVal!==undefined)?node.className.baseVal:node.className;
                      if(typeof raw!=='string')return out;
                      var arr=raw.trim().split(/\\s+/);
                      for(var i=0;i<arr.length&&out.length<3;i++){
                        var c=arr[i];
                        if(!c||!/^[A-Za-z][A-Za-z0-9_-]*$/.test(c))continue;
                        if(c.length>40||/\\d{5,}/.test(c))continue;
                        if(/^(?:css|sc|jsx|makeStyles|MuiBox|emotion|ng-|react-)/i.test(c))continue;
                        out.push(c);
                      }
                    }catch(e){}
                    return out;
                  }

                  function isProtected(el){
                    try{
                      if(!el||el.nodeType!==1)return true;
                      if(isPickerUi(el))return true;
                      var tag=(el.tagName||'').toLowerCase();
                      return /^(?:html|body|head|script|style|link|meta|title|video|audio|source)$/.test(tag);
                    }catch(e){return true;}
                  }

                  function nthOfType(node){
                    try{
                      var i=1,s=node.previousElementSibling;
                      while(s){if(s.tagName===node.tagName)i++;s=s.previousElementSibling;}
                      return i;
                    }catch(e){return 0;}
                  }

                  function segment(node,forceNth){
                    var tag=(node.tagName||'').toLowerCase();
                    var sel=tag;
                    var cls=stableClasses(node);
                    if(cls.length)sel+='.'+cls.join('.');
                    if(forceNth){
                      var nth=nthOfType(node);
                      if(nth>0)sel+=':nth-of-type('+nth+')';
                    }
                    return sel;
                  }

                  function siblingNeedsNth(node,base){
                    try{
                      var p=node.parentElement;
                      if(!p)return false;
                      return p.querySelectorAll(':scope>'+base).length>1;
                    }catch(e){
                      try{
                        var count=0,kids=node.parentElement?node.parentElement.children:[];
                        for(var i=0;i<kids.length;i++)if(kids[i].tagName===node.tagName)count++;
                        return count>1;
                      }catch(x){return true;}
                    }
                  }

                  function path(el){
                    try{
                      if(!el||el.nodeType!==1||isProtected(el))return '';
                      var id=stableId(el);
                      if(id&&unique('#'+id))return '#'+id;
                      var direct=segment(el,false);
                      if(direct&&unique(direct))return direct;

                      var parts=[];
                      var node=el;
                      var depth=0;
                      while(node&&node.nodeType===1&&depth<7){
                        var tag=(node.tagName||'').toLowerCase();
                        if(tag==='html')break;
                        if(tag==='body'){parts.unshift('body');break;}
                        var nodeId=stableId(node);
                        if(nodeId&&unique('#'+nodeId)){parts.unshift('#'+nodeId);break;}
                        var base=segment(node,false);
                        parts.unshift(segment(node,siblingNeedsNth(node,base)));
                        var joined=parts.join('>');
                        if(unique(joined))return joined;
                        node=node.parentElement;
                        depth++;
                      }
                      return parts.join('>');
                    }catch(e){return '';}
                  }

                  function matchCount(sel){
                    try{return sel?document.querySelectorAll(sel).length:0;}catch(e){return 0;}
                  }

                  function highlight(el){
                    try{
                      if(!el||isProtected(el)){overlay.style.display='none';return;}
                      var r=el.getBoundingClientRect();
                      overlay.style.display='block';
                      overlay.style.left=Math.max(0,r.left)+'px';
                      overlay.style.top=Math.max(0,r.top)+'px';
                      overlay.style.width=Math.max(0,r.width)+'px';
                      overlay.style.height=Math.max(0,r.height)+'px';
                    }catch(e){}
                  }

                  function send(el){
                    try{
                      var sel=path(el);
                      var pv='';
                      var count=matchCount(sel);
                      var tag=(el&&el.tagName?String(el.tagName).toLowerCase():'');
                      try{pv=(el.outerHTML||'').replace(/\\s+/g,' ').slice(0,220);}catch(e){}
                      if(window.YieldAdBlockBridge&&YieldAdBlockBridge.onElementPickedV2){
                        YieldAdBlockBridge.onElementPickedV2(sel,pv,count,tag);
                      }else if(window.YieldAdBlockBridge&&YieldAdBlockBridge.onElementPicked){
                        YieldAdBlockBridge.onElementPicked(sel,pv);
                      }
                    }catch(e){}
                  }

                  function select(el){
                    if(!el||isPickerUi(el))return;
                    if(isProtected(el)){
                      notice('Elemen penting tidak dapat diblokir');
                      overlay.style.display='none';
                      return;
                    }
                    cur=el;
                    highlight(el);
                    send(el);
                  }

                  window.__yieldPickerParent=function(){
                    try{
                      if(!cur||!cur.parentElement)return;
                      var p=cur.parentElement;
                      if(isProtected(p)){
                        notice('Batas aman tercapai');
                        return;
                      }
                      select(p);
                    }catch(e){}
                  };

                  window.__yieldPickerContinue=function(){
                    try{
                      cur=null;
                      overlay.style.display='none';
                      label.textContent=DEFAULT_TEXT;
                      attach();
                    }catch(e){}
                  };

                  function cleanup(){
                    try{window.__yieldPickerActive=false;}catch(e){}
                    try{if(noticeTimer)clearTimeout(noticeTimer);}catch(e){}
                    try{
                      for(var i=0;i<listeners.length;i++){
                        var L=listeners[i];
                        document.removeEventListener(L[0],L[1],true);
                      }
                    }catch(e){}
                    listeners=[];
                    try{if(overlay&&overlay.parentNode)overlay.parentNode.removeChild(overlay);}catch(e){}
                    try{if(bar&&bar.parentNode)bar.parentNode.removeChild(bar);}catch(e){}
                    try{window.__yieldPickerParent=null;}catch(e){}
                    try{window.__yieldPickerContinue=null;}catch(e){}
                    try{window.__yieldPickerCommit=null;}catch(e){}
                    try{window.__yieldPickerCancel=null;}catch(e){}
                  }

                  window.__yieldPickerCommit=function(){cleanup();};
                  window.__yieldPickerCancel=function(){
                    cleanup();
                    try{
                      if(window.YieldAdBlockBridge&&YieldAdBlockBridge.onPickerExited){
                        YieldAdBlockBridge.onPickerExited();
                      }
                    }catch(e){}
                  };

                  closeBtn.addEventListener('click',function(e){
                    try{e.preventDefault();e.stopPropagation();if(e.stopImmediatePropagation)e.stopImmediatePropagation();}catch(x){}
                    try{window.__yieldPickerCancel();}catch(x){}
                    return false;
                  },true);

                  function pt(e){
                    var x=e.clientX,y=e.clientY;
                    if((x===null||x===undefined)&&e.touches&&e.touches[0]){x=e.touches[0].clientX;y=e.touches[0].clientY;}
                    if((x===null||x===undefined)&&e.changedTouches&&e.changedTouches[0]){x=e.changedTouches[0].clientX;y=e.changedTouches[0].clientY;}
                    return {x:x,y:y};
                  }

                  function stop(e){
                    try{e.preventDefault();e.stopPropagation();if(e.stopImmediatePropagation)e.stopImmediatePropagation();}catch(x){}
                    return false;
                  }

                  function onDown(e){
                    try{
                      if(e&&e.isPrimary===false)return stop(e);
                      if(isPickerUi(e.target))return true;
                      var now=Date.now();
                      if(now-lastPickAt<320)return stop(e);
                      lastPickAt=now;
                      var p=pt(e);
                      if(p.x!==null&&p.x!==undefined){
                        var el=document.elementFromPoint(p.x,p.y);
                        if(el)select(el);
                      }
                    }catch(x){}
                    return stop(e);
                  }

                  function swallow(e){
                    if(isPickerUi(e.target))return true;
                    return stop(e);
                  }

                  function onMove(e){
                    try{
                      if(isPickerUi(e.target))return;
                      var p=pt(e);
                      if(p.x!==null&&p.x!==undefined){
                        var el=document.elementFromPoint(p.x,p.y);
                        if(el&&!isPickerUi(el)&&!isProtected(el))highlight(el);
                      }
                    }catch(x){}
                  }

                  function add(t,fn){
                    try{document.addEventListener(t,fn,{capture:true,passive:false});}
                    catch(e){document.addEventListener(t,fn,true);}
                    listeners.push([t,fn]);
                  }

                  if(window.PointerEvent){
                    add('pointerdown',onDown);
                    add('pointerup',swallow);
                    add('pointermove',onMove);
                  }else{
                    add('touchstart',onDown);
                    add('touchend',swallow);
                    add('mousedown',onDown);
                    add('mouseup',swallow);
                    add('mousemove',onMove);
                  }
                  add('click',swallow);
                  add('auxclick',swallow);
                  add('contextmenu',swallow);
                })();
                """;
    }
}
