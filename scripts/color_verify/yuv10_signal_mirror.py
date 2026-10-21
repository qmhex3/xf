import sys
import numpy as np

BT2020 = dict(rv=1.4746, gu=0.16455, gv=0.57135, bu=1.8814)
BT709 = dict(rv=1.5748, gu=0.1873, gv=0.4681, bu=1.8556)
F = np.float32


def mix(a, b, c, d, fx, fy):
    top = a + (b - a) * fx
    bottom = c + (d - c) * fx
    return top + (bottom - top) * fy


def chroma_420(plane, h, w):
    ch, cw = plane.shape
    rows = np.arange(h)
    k = (rows - 1) >> 1
    y0 = np.clip(k, 0, ch - 1)
    y1 = np.clip(k + 1, 0, ch - 1)
    fy = np.where(rows & 1 == 1, F(0.25), F(0.75)).astype(F)
    cols = np.arange(w)
    x0 = np.minimum(cols >> 1, cw - 1)
    odd = (cols & 1) == 1
    x1 = np.where(odd, np.minimum(x0 + 1, cw - 1), x0)
    fx = np.where(odd, F(0.5), F(0.0)).astype(F)
    p = plane.astype(F)
    a = p[np.ix_(y0, x0)]
    b = p[np.ix_(y0, x1)]
    c = p[np.ix_(y1, x0)]
    d = p[np.ix_(y1, x1)]
    return mix(a, b, c, d, fx[None, :], fy[:, None])


def signal16(y, u, v, coeff, limited=True, subsampled=True):
    h, w = y.shape
    if subsampled:
        uu = (chroma_420(u, h, w) - F(512)) * F(1.0 / (896 if limited else 1023))
        vv = (chroma_420(v, h, w) - F(512)) * F(1.0 / (896 if limited else 1023))
    else:
        scale = F(1.0 / (896 if limited else 1023))
        uu = (u.astype(F) - F(512)) * scale
        vv = (v.astype(F) - F(512)) * scale
    yy = ((y.astype(F) - F(64)) / F(876)) if limited else (y.astype(F) / F(1023))
    r = yy + F(coeff['rv']) * vv
    g = yy - F(coeff['gu']) * uu - F(coeff['gv']) * vv
    b = yy + F(coeff['bu']) * uu
    out = np.empty((h, w, 3), np.uint16)
    for i, c in enumerate((r, g, b)):
        out[..., i] = (np.clip(c, 0, 1) * F(65535) + F(0.5)).astype(np.int64).astype(np.uint16)
    return out


def orient_index(rotation, flip, w, h):
    if rotation == 90:
        o = (h, w, (w - 1) * h + h - 1, -h, -1) if flip else (h, w, h - 1, h, -1)
    elif rotation == 180:
        o = (w, h, (h - 1) * w, 1, -w) if flip else (w, h, w * h - 1, -1, -w)
    elif rotation == 270:
        o = (h, w, 0, h, 1) if flip else (h, w, (w - 1) * h, -h, 1)
    else:
        o = (w, h, w - 1, -1, w) if flip else (w, h, 0, 1, w)
    ow, oh, base, cs, rs = o
    cols = np.arange(w)[None, :]
    rows = np.arange(h)[:, None]
    return ow, oh, base + cols * cs + rows * rs


def oriented(sig, rotation, flip):
    h, w, _ = sig.shape
    ow, oh, idx = orient_index(rotation, flip, w, h)
    out = np.zeros((oh * ow, 3), np.uint16)
    out[idx.reshape(-1)] = sig.reshape(-1, 3)
    return out.reshape(oh, ow, 3)


def load_yuv420(path, w, h):
    raw = np.fromfile(path, dtype='<u2')
    y = raw[:w * h].reshape(h, w)
    u = raw[w * h:w * h + (w // 2) * (h // 2)].reshape(h // 2, w // 2)
    v = raw[w * h + (w // 2) * (h // 2):].reshape(h // 2, w // 2)
    return y, u, v


def load_yuv444(path, w, h):
    raw = np.fromfile(path, dtype='<u2')
    n = w * h
    return raw[:n].reshape(h, w), raw[n:2 * n].reshape(h, w), raw[2 * n:3 * n].reshape(h, w)


def stats(a, b, label):
    d = np.abs(a.astype(np.int32) - b.astype(np.int32))
    print(f'{label}: max {d.max()}  mean {d.mean():.3f}  p99 {np.percentile(d, 99):.0f}  p99.9 {np.percentile(d, 99.9):.0f}  <=2: {(d <= 2).mean() * 100:.2f}%  <=16: {(d <= 16).mean() * 100:.2f}%  nonzero: {(d > 0).mean() * 100:.2f}%')
    return d


if __name__ == '__main__':
    name, w, h, sub = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), sys.argv[4]
    ref = np.fromfile(f'/home/claude/samples/ref_{name}.rgb48', '<u2').reshape(h, w, 3)
    if sub == '420':
        y, u, v = load_yuv420(f'/home/claude/samples/yuv_{name}.raw', w, h)
        mine = signal16(y, u, v, BT2020, True, True)
    else:
        y, u, v = load_yuv444(f'/home/claude/samples/yuv_{name}.raw', w, h)
        mine = signal16(y, u, v, BT2020, True, False)
    stats(mine, ref, f'{name} {sub} vs swscale')
