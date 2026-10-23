import os
import re, math
from xml.etree import ElementTree as ET
ROOT=os.environ.get("ICON_SRC","./icon_src") + "/"
def f(x): 
    s=f"{x:.3f}".rstrip('0').rstrip('.')
    return s if s not in ("-0","") else "0"
def circle(cx,cy,r): return f"M{f(cx-r)} {f(cy)}a{f(r)} {f(r)} 0 1 0 {f(2*r)} 0a{f(r)} {f(r)} 0 1 0 {f(-2*r)} 0"
def load(lib,name):
    p = ROOT+("tabler/icons/outline/" if lib=="t" else "lucide/icons/")+name+".svg"
    txt=re.sub(r'<!--.*?-->','',open(p).read(),flags=re.S)
    root=ET.fromstring(txt)
    out=[]
    for e in root.iter():
        t=e.tag.split('}')[-1]; a=e.attrib
        if t=="path": out.append(a["d"])
        elif t=="circle": out.append(circle(float(a["cx"]),float(a["cy"]),float(a["r"])))
        elif t=="ellipse":
            cx,cy,rx,ry=[float(a[k]) for k in("cx","cy","rx","ry")]
            out.append(f"M{f(cx-rx)} {f(cy)}a{f(rx)} {f(ry)} 0 1 0 {f(2*rx)} 0a{f(rx)} {f(ry)} 0 1 0 {f(-2*rx)} 0")
        elif t=="rect":
            x,y,w,h=[float(a.get(k,0)) for k in("x","y","width","height")]
            rx=float(a.get("rx",a.get("ry",0))); ry=float(a.get("ry",rx))
            if rx==0: out.append(f"M{f(x)} {f(y)}h{f(w)}v{f(h)}h{f(-w)}z")
            else: out.append(f"M{f(x+rx)} {f(y)}h{f(w-2*rx)}a{f(rx)} {f(ry)} 0 0 1 {f(rx)} {f(ry)}v{f(h-2*ry)}a{f(rx)} {f(ry)} 0 0 1 {f(-rx)} {f(ry)}h{f(-(w-2*rx))}a{f(rx)} {f(ry)} 0 0 1 {f(-rx)} {f(-ry)}v{f(-(h-2*ry))}a{f(rx)} {f(ry)} 0 0 1 {f(rx)} {f(-ry)}z")
        elif t=="line": out.append(f"M{a['x1']} {a['y1']}L{a['x2']} {a['y2']}")
        elif t in("polyline","polygon"):
            pts=a["points"].replace(',',' ').split()
            pr=[(pts[i],pts[i+1]) for i in range(0,len(pts),2)]
            out.append("M"+"L".join(f"{x} {y}" for x,y in pr)+("z" if t=="polygon" else ""))
    return out

NUM=re.compile(r'[-+]?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?')
def normalize(d):
    """Re-serialise path data with explicit separators (arc flags are split per SVG spec)."""
    cnt={'a':7,'c':6,'s':4,'q':4,'t':2,'m':2,'l':2,'h':1,'v':1,'z':0}
    i=0; out=[]; n=len(d); cmd=None
    def ws():
        nonlocal i
        while i<n and d[i] in ' ,\t\r\n': i+=1
    while True:
        ws()
        if i>=n: break
        if d[i].isalpha(): cmd=d[i]; i+=1; out.append(cmd)
        if cmd.lower()=='z': continue
        k=cnt[cmd.lower()]
        for j in range(k):
            ws()
            if cmd.lower()=='a' and j in (3,4):
                out.append(d[i]); i+=1
            else:
                m=NUM.match(d,i); out.append(m.group()); i=m.end()
        # implicit repeat handled by loop (cmd persists); m->l implicit
        if cmd in ('m','M'):
            cmd='l' if cmd=='m' else 'L'
            ws()
            if i<n and not d[i].isalpha(): out.append(cmd)  # implicit lineto after moveto, make explicit
    if out and out[0]=='m': out[0]='M'  # leading relative moveto is absolute; needed when paths are joined
    s=""
    for t in out:
        if t.isalpha(): s+=t
        else: s+=(" " if s and not s[-1].isalpha() else "")+t
    return s
