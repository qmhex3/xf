import subprocess, sys
import numpy as np

W, H = 512, 256
N = W * H
K = N // 2
CHAIN = ("format=gbrp16le,zscale=tin=smpte2084:pin=bt2020:min=gbr:rin=full:t=linear:npl=203:m=gbr:p=bt2020,"
         "format=gbrpf32le,zscale=tin=linear:pin=bt2020:min=gbr:t=linear:p=bt709:m=gbr,{tm}"
         "zscale=tin=linear:pin=bt709:min=gbr:t=iec61966-2-1:p=bt709:m=gbr:r=full:dither=none,format=gbrp")
M709_TO_2020 = np.array([[0.6274039, 0.3292830, 0.0433131], [0.0690973, 0.9195404, 0.0113623], [0.0163914, 0.0880133, 0.8955953]])


def pq_inverse(nits):
    m1, m2 = 2610 / 16384, 2523 / 32
    c1, c2, c3 = 3424 / 4096, 2413 / 128, 2392 / 128
    p = np.power(np.clip(nits / 10000, 0, 1), m1)
    return np.power((c1 + c2 * p) / (1 + c3 * p), m2)


def make_samples(path):
    rng = np.random.default_rng(5)
    base = rng.uniform(0, 1, (N, 3))
    grey = base.mean(1, keepdims=True)
    col = np.clip(grey + (base - grey) * rng.uniform(0, 1, (N, 1)), 0, 1)
    lin = np.concatenate([col[:K] * 0.75, col[K:] * (1000 / 203)])
    sig = pq_inverse(np.clip(lin @ M709_TO_2020.T, 0, None) * 203)
    np.round(np.clip(sig, 0, 1) * 65535).astype(np.uint16).reshape(H, W, 3).tofile(path)


def reference(src, name, tonemap):
    tm = "" if tonemap is None else f"tonemap=tonemap={tonemap}:desat=0:peak=4.926,"
    out = f"ref_{name}.gbrp"
    subprocess.check_call(["ffmpeg", "-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb48le", "-s", f"{W}x{H}", "-i", src,
                           "-vf", CHAIN.format(tm=tm), "-f", "rawvideo", "-pix_fmt", "gbrp", out])
    planes = np.fromfile(out, np.uint8).reshape(3, N).astype(int)
    return np.stack([planes[2], planes[0], planes[1]], 1)


def report(name, fx, ref):
    for label, part in (("below knee", slice(0, K)), ("above knee", slice(K, N))):
        d = np.abs(fx[part] - ref[part])
        print(f"{name:9s} {label:10s} meanAbs={d.mean():6.3f} max={d.max():3d} frac<=1={(d.max(1) <= 1).mean():.3f}")


if __name__ == "__main__":
    make_samples("pq.rgb48")
    if len(sys.argv) < 2:
        print("usage: hdr_ffmpeg_compare.py <fx.rgb>  (fx.rgb: HdrDump.kt output for pq.rgb48; run once without args to create pq.rgb48)")
        sys.exit(0)
    fx = np.fromfile(sys.argv[1], np.uint8).reshape(N, 3).astype(int)
    for name, tm in (("clip", None), ("hable", "hable"), ("mobius", "mobius")):
        report(name, fx, reference("pq.rgb48", name, tm))
