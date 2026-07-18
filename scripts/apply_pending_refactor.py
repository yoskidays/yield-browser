from pathlib import Path
import re

SRC=Path('app/src/main/java/com/yieldbrowser/app/MainActivity.java')
OUT=SRC.parent
text=SRC.read_text()
text=text.replace('\nprivate String buildHlsFingerprint(HlsPlaylistParser.Playlist playlist) throws Exception {', '\n    private String buildHlsFingerprint(HlsPlaylistParser.Playlist playlist) throws Exception {', 1)
class_marker='public class MainActivity extends Activity\n        implements TranslateBridge.Callback, VideoBridge.Callback, AdBlockBridge.Callback {'
ci=text.find(class_marker)
if ci<0: raise SystemExit('class marker missing')
prefix=text[:ci]
class_open=text.find('{',ci)
D=0; class_close=None
for i in range(class_open,len(text)):
    c=text[i]
    if c=='{':D+=1
    elif c=='}':
        D-=1
        if D==0:
            class_close=i;break
body_start=class_open+1
body_end=class_close

pat=re.compile(r'(?m)^    (?:(?:public|private|protected|static|final|synchronized|native|abstract|strictfp)\s+)+(?:<[^>]+>\s+)?[\w\[\]<>?,.@ ]+\s+(\w+)\s*\([^;]*?\)\s*(?:throws [^{]+)?\{')
methods=[]
for m in pat.finditer(text, body_start, body_end):
    sig_start=m.start(); brace=text.find('{',m.start(),m.end()+5)
    d=0; end=None
    for j in range(brace,body_end):
        if text[j]=='{': d+=1
        elif text[j]=='}':
            d-=1
            if d==0:
                end=j+1;break
    if end is None: raise SystemExit('unbalanced '+m.group(1))
    line_start=text.rfind('\n',0,sig_start)+1
    start=line_start
    scan=line_start
    while scan>body_start:
        prev_end=scan-1
        prev_start=text.rfind('\n',body_start,prev_end)+1
        line=text[prev_start:prev_end].strip()
        include=line=='' or line.startswith('@') or line.startswith('//') or line.startswith('*') or line.startswith('/*') or line.endswith('*/')
        if include:
            start=prev_start;scan=prev_start
        else: break
    header=text[sig_start:brace]
    sl=text.count('\n',0,sig_start)+1
    methods.append(dict(name=m.group(1),start=start,sig_start=sig_start,end=end,header=header,sl=sl,text=text[start:end],top_override='@Override' in text[start:sig_start]))
methods.sort(key=lambda x:x['sig_start'])
for idx,m in enumerate(methods):
    if idx and m['start']<methods[idx-1]['end']:
        m['start']=m['sig_start']

parts=[]; pos=body_start
for m in methods:
    parts.append(text[pos:m['start']]);pos=m['end']
parts.append(text[pos:body_end])
state=''.join(parts)
state=re.sub(r'(?m)^    private\s+', '    ', state)
state=re.sub(r'\n[ \t]*\n(?:[ \t]*\n)+','\n\n',state).strip('\n')

def is_static(m):
    return bool(re.search(r'\bstatic\b',m['header']))

def widen_method(s):
    rel=s.find('    private ')
    if rel>=0:
        s=s[:rel]+'    '+s[rel+12:]
    return s

def abstract_decl(m):
    h=m['header'].strip()
    h=re.sub(r'^(private\s+)', '', h)
    h=re.sub(r'\b(final|synchronized|native|strictfp)\s+', '', h)
    if h.startswith('public '): h='public abstract '+h[len('public '):]
    elif h.startswith('protected '): h='protected abstract '+h[len('protected '):]
    else: h='abstract '+h
    return '    '+h.strip()+';'

root_static=[]; main=[]; download=[]; web=[]; decls=[]
for m in methods:
    if is_static(m):
        root_static.append(widen_method(m['text']).replace('MainActivity.this','YieldActivityState.this'))
    else:
        if not m['top_override']:
            decls.append(abstract_decl(m))
        s=widen_method(m['text'])
        if 2500 <= m['sl'] < 6300:
            s=s.replace('MainActivity.this','YieldDownloadActivity.this')
            download.append(s)
        elif m['sl'] >= 7000:
            s=s.replace('MainActivity.this','YieldWebRuntimeActivity.this')
            web.append(s)
        else:
            main.append(s)

seen=set(); uniq=[]
for d in decls:
    key=re.sub(r'\s+',' ',d)
    if key not in seen: seen.add(key);uniq.append(d)

imports=prefix
root=imports+'''abstract class YieldActivityState extends Activity
        implements TranslateBridge.Callback, VideoBridge.Callback, AdBlockBridge.Callback {\n\n'''+state+'\n\n'+'\n\n'.join(root_static)+'\n\n    // Cross-layer virtual contract generated from the original MainActivity methods.\n'+'\n'.join(uniq)+'\n}\n'
down=imports+'''abstract class YieldDownloadActivity extends YieldActivityState {\n\n'''+ '\n\n'.join(download).strip()+'\n}\n'
webs=imports+'''abstract class YieldWebRuntimeActivity extends YieldDownloadActivity {\n\n'''+ '\n\n'.join(web).strip()+'\n}\n'
mainf=imports+'''public class MainActivity extends YieldWebRuntimeActivity
        implements TranslateBridge.Callback, VideoBridge.Callback, AdBlockBridge.Callback {\n\n'''+ '\n\n'.join(main).strip()+'\n}\n'
outputs={}
for name,data in [('YieldActivityState.java',root),('YieldDownloadActivity.java',down),('YieldWebRuntimeActivity.java',webs),('MainActivity.java',mainf)]:
    data=re.sub(r'\n[ \t]*\n(?:[ \t]*\n)+','\n\n',data)
    outputs[name]=data
    (OUT/name).write_text(data)
    print(name,len(data.splitlines()),len(data))
print('method groups',len(main),len(download),len(web),len(root_static),'decls',len(uniq))
if len(outputs['MainActivity.java'].splitlines()) > 3000:
    raise SystemExit(f"MainActivity target not reached: {len(outputs['MainActivity.java'].splitlines())} lines")
settings=OUT/'SettingsStore.java'
settings_text=settings.read_text()
settings_text=settings_text.replace('static void load(MainActivity a, SharedPreferences p)', 'static void load(YieldActivityState a, SharedPreferences p)')
settings_text=settings_text.replace('static void save(MainActivity a, SharedPreferences p)', 'static void save(YieldActivityState a, SharedPreferences p)')
settings.write_text(settings_text)
if not (download and web and main):
    raise SystemExit('Generated domain split is incomplete')