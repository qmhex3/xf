import numpy as np, subprocess, sys
import kt_mirror as K
exe=sys.argv[1]; strength=float(sys.argv[2]); exponent=float(sys.argv[3]) if len(sys.argv)>3 else 1.5
rng=np.random.default_rng(7)
w,h=256,128
def img():
    a=rng.integers(0,65536,(h,w,4),dtype=np.uint16)
    # saturated + gradient content: bias toward valid HDR range
    a[...,3]=65535
    g=np.linspace(0,65535,w).astype(np.uint16)
    a[:16,:,0]=g; a[:16,:,1]=g; a[:16,:,2]=g
    a[16:32,:,0]=g; a[16:32,:,1]=(g*0.3).astype(np.uint16); a[16:32,:,2]=(g*0.2).astype(np.uint16)
    return a
for hlg,prim,peak in [(0,1,1000),(0,1,4000),(0,0,1000),(0,2,1000),(0,3,1000),(1,1,1000)]:
    a=img(); a.tofile('in.bin')
    subprocess.check_call([exe,str(w),str(h),str(hlg),str(prim),str(peak),'in.bin','out.bin'])
    raw=np.fromfile('out.bin',dtype=np.uint8)
    cpp=raw[:w*h*4].reshape(h,w,4)[...,:3].astype(int)
    kt=K.tonemap(a,hlg,prim,peak,strength,exponent)
    d=np.abs(cpp-kt)
    print(f'hlg={hlg} prim={prim} peak={peak}: mean|d|={d.mean():.4f} max={d.max()} frac>1={(d>1).mean():.5f}')
