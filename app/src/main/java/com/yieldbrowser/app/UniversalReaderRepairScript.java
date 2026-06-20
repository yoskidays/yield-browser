package com.yieldbrowser.app;

/** Builds the self-contained DOM repair injected into image-reader pages. */
final class UniversalReaderRepairScript {
    private UniversalReaderRepairScript() {
    }

    static String build() {
        return """
                (function(){
                  'use strict';
                  try {
                    var VERSION='0.9.95';
                    var READER_WORDS=/(^|[\\s_\\-])(chapter|chapitre|capitulo|episode|reader|reading|read-content|manga|manhwa|manhua|webtoon|comic|komik|baca|viewer|pages?)([\\s_\\-]|$)/i;
                    var PATH_RE=/(?:^|\\/)(?:chapter|chapitre|capitulo|episode|reader|read-online|reading|baca|ch)(?:[-_\\/]|$)|-chapter-|\\/chapter\\/|\\/episode\\//i;
                    var AD_WORDS=/(^|[\\s_\\-])(ad|ads|advert|advertisement|banner|sponsor|sponsored|promo|promotion|affiliate|popunder|popup|monetize|adsbygoogle|doubleclick|taboola|outbrain)([\\s_\\-]|$)/i;
                    var AD_URL=/(?:doubleclick|googlesyndication|adservice|adserver|adsystem|taboola|outbrain|popads|propellerads|onclicka|trafficjunky|exoclick|\\/ads?[\\/_-]|banner|sponsor|affiliate)/i;
                    var PLACEHOLDER=/(?:transparent|placeholder|spacer|blank(?:\\.|_)|1x1|pixel\\.(?:gif|png)|loading(?:\\.|_))/i;
                    var LAZY_ATTRS=['data-src','data-lazy-src','data-original','data-cfsrc','data-url','data-echo','data-lazy','data-image','data-original-src','data-src-large'];
                    var SRCSET_ATTRS=['data-srcset','data-lazy-srcset','data-original-srcset'];
                    var BG_ATTRS=['data-bg','data-background','data-background-image','data-lazy-background'];

                    function safeText(v){return (v==null?'':String(v)).trim();}
                    function token(el){
                      if(!el||el.nodeType!==1)return '';
                      return safeText((el.id||'')+' '+(el.className&&typeof el.className==='string'?el.className:'')+' '+(el.getAttribute('role')||'')+' '+(el.getAttribute('aria-label')||''));
                    }
                    function absolute(v){
                      v=safeText(v);
                      if(!v)return '';
                      var cssUrl=v.match(/^url\\((["']?)(.*?)\\1\\)$/i);if(cssUrl)v=cssUrl[2];
                      if(v.indexOf(',')>0 && /\\s\\d+(?:\\.\\d+)?[wx](?:,|$)/.test(v))v=v.split(',')[0].trim().split(/\\s+/)[0];
                      try{return new URL(v,document.baseURI).href;}catch(e){return v;}
                    }
                    function validNetworkSource(v){
                      v=safeText(v);
                      return !!v && !/^(?:data:|blob:|about:blank|javascript:)/i.test(v);
                    }
                    function sourcesFor(img){
                      var out=[];
                      function add(v){v=absolute(v);if(validNetworkSource(v)&&out.indexOf(v)<0)out.push(v);}
                      for(var a=0;a<LAZY_ATTRS.length;a++)add(img.getAttribute(LAZY_ATTRS[a]));
                      for(var s=0;s<SRCSET_ATTRS.length;s++){
                        var set=safeText(img.getAttribute(SRCSET_ATTRS[s]));
                        if(set)add(set.split(',')[0].trim().split(/\\s+/)[0]);
                      }
                      var picture=img.parentElement&&img.parentElement.tagName==='PICTURE'?img.parentElement:null;
                      if(picture){
                        var nodes=picture.querySelectorAll('source');
                        for(var n=0;n<nodes.length;n++){
                          for(var q=0;q<SRCSET_ATTRS.length;q++){
                            var ps=safeText(nodes[n].getAttribute(SRCSET_ATTRS[q]));
                            if(ps)add(ps.split(',')[0].trim().split(/\\s+/)[0]);
                          }
                        }
                      }
                      // Keep normal sources after lazy attributes so a lazy URL stays preferred,
                      // while broken/hidden standard reader images can still be retried.
                      add(img.currentSrc);
                      add(img.getAttribute('src'));
                      var normalSet=safeText(img.getAttribute('srcset'));
                      if(normalSet)add(normalSet.split(',')[0].trim().split(/\s+/)[0]);
                      if(picture){
                        var normalNodes=picture.querySelectorAll('source[srcset]');
                        for(var np=0;np<normalNodes.length;np++){
                          var normalPictureSet=safeText(normalNodes[np].getAttribute('srcset'));
                          if(normalPictureSet)add(normalPictureSet.split(',')[0].trim().split(/\s+/)[0]);
                        }
                      }
                      return out;
                    }
                    function backgroundSource(el){
                      for(var a=0;a<BG_ATTRS.length;a++){var v=absolute(el.getAttribute(BG_ATTRS[a]));if(validNetworkSource(v))return v;}
                      return '';
                    }
                    function isLazyCandidate(img){
                      if(!img||img.tagName!=='IMG')return false;
                      for(var a=0;a<LAZY_ATTRS.length;a++)if(safeText(img.getAttribute(LAZY_ATTRS[a])))return true;
                      for(var s=0;s<SRCSET_ATTRS.length;s++)if(safeText(img.getAttribute(SRCSET_ATTRS[s])))return true;
                      return /(?:lazy|lozad|unveil|defer|placeholder)/i.test(token(img));
                    }
                    function isAdLike(el,src){
                      if(src&&AD_URL.test(src))return true;
                      var cur=el;
                      for(var i=0;i<7&&cur&&cur!==document.body;i++,cur=cur.parentElement){
                        if(AD_WORDS.test(token(cur)))return true;
                        var href=cur.getAttribute&&safeText(cur.getAttribute('href'));
                        if(href&&AD_URL.test(href))return true;
                      }
                      return false;
                    }
                    function visualBox(img){
                      var r=img.getBoundingClientRect();
                      var p=img.parentElement?img.parentElement.getBoundingClientRect():{width:0,height:0};
                      return {width:Math.max(r.width||0,p.width||0),height:Math.max(r.height||0,p.height||0)};
                    }
                    function wideEnough(img){
                      var b=visualBox(img),vw=Math.max(320,window.innerWidth||document.documentElement.clientWidth||320);
                      return b.width>=Math.min(320,vw*0.48) && b.height>=90;
                    }
                    function readerAncestor(img){
                      var cur=img;
                      for(var i=0;i<8&&cur&&cur!==document.body;i++,cur=cur.parentElement){
                        if(READER_WORDS.test(token(cur))&&!AD_WORDS.test(token(cur)))return cur;
                      }
                      return null;
                    }
                    function currentLooksPlaceholder(img){
                      var src=safeText(img.getAttribute('src'));
                      if(!src||/^(?:data:|about:blank|javascript:)/i.test(src)||PLACEHOLDER.test(src))return true;
                      if(img.complete&&img.naturalWidth<=2&&img.naturalHeight<=2&&sourcesFor(img).length)return true;
                      return false;
                    }
                    function hiddenOrBroken(img){
                      if(!img||img.tagName!=='IMG')return false;
                      try{
                        var cs=getComputedStyle(img);
                        if(img.hasAttribute('hidden')||cs.display==='none'||cs.visibility==='hidden'||cs.visibility==='collapse'||parseFloat(cs.opacity||'1')===0)return true;
                      }catch(e){}
                      var src=safeText(img.currentSrc||img.getAttribute('src'));
                      return !!(validNetworkSource(src)&&img.complete&&img.naturalWidth===0);
                    }
                    function classify(){
                      var all=Array.prototype.slice.call(document.images||[]);
                      var lazy=[];
                      for(var i=0;i<all.length;i++)if(isLazyCandidate(all[i])&&!isAdLike(all[i],sourcesFor(all[i])[0]||''))lazy.push(all[i]);
                      var backgrounds=[],bgQuery='['+BG_ATTRS.join('],[')+']';
                      try{var bgNodes=document.querySelectorAll(bgQuery);for(var bi=0;bi<bgNodes.length;bi++){var bs=backgroundSource(bgNodes[bi]);if(bs&&!isAdLike(bgNodes[bi],bs))backgrounds.push(bgNodes[bi]);}}catch(e){}
                      var pathHint=PATH_RE.test(location.pathname||'');
                      var titleHint=/(?:chapter|episode|capitulo|chapitre|baca|read online)/i.test(document.title||'');
                      var repairable=lazy.slice();
                      function addRepairable(img){if(img&&repairable.indexOf(img)<0)repairable.push(img);}
                      // Some readers use normal src/srcset and only hide the image, or leave it
                      // broken after an anti-adblock/lazy-loader failure. Include those images when
                      // URL/title/DOM already indicates a reader page.
                      if(pathHint||titleHint){
                        for(var ai=0;ai<all.length;ai++){
                          var candidate=all[ai],candidateSources=sourcesFor(candidate);
                          var candidateSrc=candidateSources.length?candidateSources[0]:'';
                          if(!candidateSrc||isAdLike(candidate,candidateSrc))continue;
                          if(hiddenOrBroken(candidate)||readerAncestor(candidate)||wideEnough(candidate))addRepairable(candidate);
                        }
                      }
                      var candidates=repairable.concat(backgrounds);
                      if(!candidates.length)return null;
                      var containers=[];
                      function record(el,img){
                        if(!el||el===document.documentElement||el===document.body||isAdLike(el,''))return;
                        var found=null;
                        for(var x=0;x<containers.length;x++)if(containers[x].el===el){found=containers[x];break;}
                        if(!found){found={el:el,count:0,wide:0,keyword:READER_WORDS.test(token(el))?1:0};containers.push(found);}
                        found.count++;if(wideEnough(img))found.wide++;
                      }
                      for(var j=0;j<candidates.length;j++){
                        var cur=candidates[j].parentElement;
                        for(var d=0;d<7&&cur&&cur!==document.body;d++,cur=cur.parentElement)record(cur,candidates[j]);
                      }
                      var best=null,bestScore=-1;
                      for(var c=0;c<containers.length;c++){
                        var rec=containers[c];
                        var score=Math.min(rec.count,7)+(rec.wide*3)+(rec.keyword*5);
                        if(score>bestScore){bestScore=score;best=rec;}
                      }
                      var strongContainer=best&&best.count>=3&&(best.wide>=2||best.keyword===1)&&bestScore>=10;
                      if(strongContainer&&best&&best.el){
                        for(var ri=0;ri<all.length;ri++){
                          var readerImg=all[ri];
                          if(!best.el.contains(readerImg))continue;
                          var readerSources=sourcesFor(readerImg),readerSrc=readerSources.length?readerSources[0]:'';
                          if(!readerSrc||isAdLike(readerImg,readerSrc))continue;
                          if(isLazyCandidate(readerImg)||hiddenOrBroken(readerImg)||wideEnough(readerImg))addRepairable(readerImg);
                        }
                      }
                      var hintedReader=(pathHint||titleHint)&&repairable.length>=2;
                      if(!strongContainer&&!hintedReader)return null;
                      return {all:all,lazy:repairable,backgrounds:backgrounds,root:strongContainer?best.el:null,pathHint:pathHint||titleHint};
                    }
                    function revealNode(el,allowDisplay){
                      if(!el||el.nodeType!==1)return;
                      try{
                        var cs=getComputedStyle(el);
                        if(cs.visibility==='hidden'||cs.visibility==='collapse')el.style.setProperty('visibility','visible','important');
                        if(parseFloat(cs.opacity||'1')===0)el.style.setProperty('opacity','1','important');
                        if(allowDisplay&&cs.display==='none')el.style.setProperty('display',el.tagName==='SPAN'?'inline-block':'block','important');
                        if(el.hasAttribute('hidden'))el.removeAttribute('hidden');
                        el.style.removeProperty('content-visibility');
                      }catch(e){}
                    }
                    function installFallback(img,list){
                      if(!list||list.length<2||img.__yieldReaderFallback)return;
                      img.__yieldReaderFallback=true;
                      img.__yieldReaderSources=list;
                      img.__yieldReaderIndex=0;
                      img.addEventListener('error',function(){
                        try{
                          var arr=this.__yieldReaderSources||[];
                          var next=(this.__yieldReaderIndex||0)+1;
                          if(next<arr.length){this.__yieldReaderIndex=next;this.setAttribute('src',arr[next]);}
                        }catch(e){}
                      },false);
                    }
                    function repairBackground(el,root){
                      var src=backgroundSource(el);
                      if(!src||isAdLike(el,src))return 0;
                      var inRoot=!!(root&&root.contains(el));
                      var hinted=!!readerAncestor(el);
                      if(!inRoot&&!hinted&&!wideEnough(el))return 0;
                      try{el.style.setProperty('background-image','url(\"'+src.replace(/\"/g,'%22')+'\")','important');}catch(e){}
                      revealNode(el,true);
                      return 1;
                    }
                    function repairImage(img,root,index){
                      var list=sourcesFor(img),primary=list.length?list[0]:'';
                      if(isAdLike(img,primary))return 0;
                      var inRoot=!!(root&&root.contains(img));
                      var hinted=!!readerAncestor(img);
                      if(!inRoot&&!hinted&&!wideEnough(img))return 0;
                      if(primary&&currentLooksPlaceholder(img)){
                        try{img.removeAttribute('src');img.setAttribute('src',primary);}catch(e){}
                      }
                      for(var s=0;s<SRCSET_ATTRS.length;s++){
                        var set=safeText(img.getAttribute(SRCSET_ATTRS[s]));
                        if(set&&!safeText(img.getAttribute('srcset')))img.setAttribute('srcset',set);
                      }
                      var picture=img.parentElement&&img.parentElement.tagName==='PICTURE'?img.parentElement:null;
                      if(picture){
                        var sourceNodes=picture.querySelectorAll('source');
                        for(var p=0;p<sourceNodes.length;p++){
                          for(var q=0;q<SRCSET_ATTRS.length;q++){
                            var ps=safeText(sourceNodes[p].getAttribute(SRCSET_ATTRS[q]));
                            if(ps&&!safeText(sourceNodes[p].getAttribute('srcset')))sourceNodes[p].setAttribute('srcset',ps);
                          }
                        }
                      }
                      img.setAttribute('loading','eager');
                      if(index<2)try{img.setAttribute('fetchpriority','high');}catch(e){}
                      revealNode(img,true);
                      var cur=img.parentElement;
                      for(var d=0;d<4&&cur&&cur!==document.body;d++,cur=cur.parentElement){
                        if(isAdLike(cur,''))break;
                        revealNode(cur,/(?:lazy|reader|chapter|page|image|comic|manga|webtoon)/i.test(token(cur)));
                        if(root&&cur===root)break;
                      }
                      try{
                        var rect=img.getBoundingClientRect(),vw=Math.max(320,window.innerWidth||320);
                        if(rect.width>vw+8){img.style.setProperty('max-width','100%','important');img.style.setProperty('height','auto','important');}
                      }catch(e){}
                      installFallback(img,list);
                      return 1;
                    }
                    function repairAll(){
                      var state=classify();
                      if(!state)return 0;
                      var repaired=0;
                      for(var i=0;i<state.lazy.length;i++)repaired+=repairImage(state.lazy[i],state.root,i);
                      for(var b=0;b<state.backgrounds.length;b++)repaired+=repairBackground(state.backgrounds[b],state.root);
                      try{window.dispatchEvent(new Event('scroll'));window.dispatchEvent(new Event('resize'));}catch(e){}
                      document.documentElement.setAttribute('data-yield-reader-repair',VERSION);
                      return repaired;
                    }
                    window.__yieldUniversalReaderRepair=repairAll;
                    var initialReaderHint=PATH_RE.test(location.pathname||'')||/(?:chapter|episode|capitulo|chapitre|baca|read online)/i.test(document.title||'');
                    var result=repairAll();
                    if((result>0||initialReaderHint)&&!window.__yieldUniversalReaderObserver&&document.documentElement){
                      window.__yieldUniversalReaderObserver=new MutationObserver(function(mutations){
                        var relevant=false;
                        for(var i=0;i<mutations.length&&!relevant;i++){
                          var m=mutations[i];
                          if(m.type==='childList'&&m.addedNodes&&m.addedNodes.length)relevant=true;
                          else if(m.type==='attributes'&&m.target&&m.target.tagName==='IMG')relevant=true;
                        }
                        if(!relevant)return;
                        clearTimeout(window.__yieldUniversalReaderTimer);
                        window.__yieldUniversalReaderTimer=setTimeout(function(){try{repairAll();}catch(e){}},180);
                      });
                      window.__yieldUniversalReaderObserver.observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['src','srcset','class','hidden'].concat(LAZY_ATTRS).concat(SRCSET_ATTRS).concat(BG_ATTRS)});
                    }
                    return result;
                  } catch(e) { return 0; }
                })();
                """;
    }
}
