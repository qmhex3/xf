import numpy as np, math
SIZE=4096; REF=203.0; HLG_PEAK=1000.0
def pq_eotf(s):
    m1=2610/16384; m2=2523/32; c1=3424/4096; c2=2413/128; c3=2392/128
    p=np.power(s,1/m2)
    return np.power(np.maximum(p-c1,0)/np.maximum(c2-c3*p,0.0001),1/m1)*10000.0
def hlg_scene(s):
    a=0.17883277; b=1-4*a; c=0.5-a*math.log(4*a)
    return np.where(s<=0.5, s*s/3.0, (np.exp((s-c)/a)+b)/12.0)
def srgb_enc(l):
    return np.where(l<=0.0031308, l*12.92, 1.055*np.power(l,1/2.4)-0.055)
g=np.arange(SIZE+1)/SIZE
PQ=(pq_eotf(g)/REF).astype(np.float32); HLG=hlg_scene(g).astype(np.float32)
OOTF=(np.power(g,1.2-1.0)*HLG_PEAK/REF).astype(np.float32)
SRGB=(srgb_enc(g)*255.0).astype(np.float32)
def idx(v): return (np.clip(v,0,1)*SIZE+0.5).astype(np.int64)
W={0:(0.2126,0.7152,0.0722),2:(0.2290,0.6917,0.0793),3:(0.2152,0.7091,0.0757),1:(0.2627,0.6780,0.0593)}
M={0:None,1:[1.6605,-0.5876,-0.0728,-0.1246,1.1329,-0.0083,-0.0182,-0.1006,1.1187],
 2:[1.2249,-0.2249,0,-0.0420,1.0420,0,-0.0196,-0.0786,1.0983],
 3:[1.157516,-0.154962,-0.002554,-0.041500,1.045568,-0.004068,-0.018050,-0.078578,1.096628]}
def bayer():
    m=np.array([[0]]); n=1
    while n<8:
        s=n*2; nx=np.zeros((s,s),dtype=int)
        nx[:n,:n]=m*4; nx[:n,n:]=m*4+2; nx[n:,:n]=m*4+3; nx[n:,n:]=m*4+1
        m=nx; n=s
    return (m+0.5)/64.0
TONE_KNEE=0.55; DESAT_KNEE=0.75; CHROMA_KEEP=0.75
def tone_luma(y,white,knee=TONE_KNEE):
    head=1-knee; span=(white-knee)/head
    u=(y-knee)/head
    mapped=knee+head*u*(1+u/(span*span))/(1+u)
    return np.where(y<=knee,np.maximum(y,0),np.minimum(mapped,1.0))
def desat_weight(y,white,strength,exponent,knee=DESAT_KNEE):
    if strength<=0: return np.zeros_like(y)
    t=np.clip((y-knee)/(white-knee),0,1)
    steps=1024
    tab=strength*np.power(np.arange(steps+1)/steps,exponent)
    return np.where(y<=knee,0.0,tab[(t*steps+0.5).astype(np.int64)])
def tonemap(rgb16,hlg,prim,peak,strength=0.0,exponent=1.5):
    h,w,_=rgb16.shape
    sig=rgb16[...,:3].astype(np.float64)/65535.0
    tab=HLG if hlg else PQ
    lin=tab[idx(sig)].astype(np.float64)
    wt=W[prim]
    luma=lambda c: wt[0]*c[...,0]+wt[1]*c[...,1]+wt[2]*c[...,2]
    if hlg:
        k=OOTF[idx(luma(lin))].astype(np.float64); lin=lin*k[...,None]
    white=max(HLG_PEAK/REF if hlg else peak/REF,2.0)
    src=luma(lin); tgt=tone_luma(src,white)
    scale=np.where(src>1e-6,tgt/np.maximum(src,1e-6),1.0)
    wgt=desat_weight(lin.max(-1),white,strength,exponent)
    keep=scale*(1-wgt)
    c=lin*keep[...,None]+(tgt*wgt)[...,None]
    if M[prim] is not None:
        m=np.array(M[prim]).reshape(3,3); c=c@m.T
    y=np.clip(tgt,0,1)[...,None]
    low=c.min(-1,keepdims=True)
    t=np.where(low<0,np.where(y-low>1e-6,y/np.maximum(y-low,1e-9),0.0),1.0)
    c=y+(c-y)*t
    high=c.max(-1,keepdims=True)
    t=(1-y)/np.maximum(high-y,1e-9)
    fitted=(1-CHROMA_KEEP)*(y+(c-y)*t)+CHROMA_KEEP*(c/np.maximum(high,1e-9))
    c=np.where(high>1,fitted,c)
    th=bayer()
    thr=np.tile(th,(h//8+1,w//8+1))[:h,:w][...,None]
    out=np.minimum((SRGB[idx(c)]+thr).astype(np.int64),255)
    ratio=(src+1/64)/(tgt+1/64)
    return out
