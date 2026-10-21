import sys, zlib, time
import numpy as np
sys.path.insert(0, '/home/claude/w/scripts/color_verify')
from png_filter_bench import load, filt, BPP


def entropy_score(f):
    c = np.bincount(f, minlength=256).astype(np.float64)
    c = c[c > 0]
    return float(-(c * np.log2(c / c.sum())).sum())


def sad_score(f):
    s = f.astype(np.int16)
    return int(np.where(s > 127, 256 - s, s).sum())


def hi_score(f):
    hi = f[0::2].astype(np.int16)
    hi = np.where(hi > 127, 256 - hi, hi)
    return int(hi.sum())


def run(img, mode, level, strategy):
    h = img.shape[0]
    prev = np.zeros(img.shape[1], np.uint8)
    out = bytearray()
    for y in range(h):
        row = img[y]
        if mode in ('entropy', 'sad', 'hi'):
            cands = [filt(k, row, prev) for k in range(5)]
            fn = {'entropy': entropy_score, 'sad': sad_score, 'hi': hi_score}[mode]
            k = min(range(5), key=lambda i: fn(cands[i]))
            f = cands[k]
        else:
            k = int(mode)
            f = filt(k, row, prev)
        out.append(k)
        out += f.tobytes()
        prev = row
    c = zlib.compressobj(level, zlib.DEFLATED, 15, 9, strategy)
    return len(c.compress(bytes(out)) + c.flush())


if __name__ == '__main__':
    path, w, h = sys.argv[1], int(sys.argv[2]), int(sys.argv[3])
    img = load(path, w, h)
    for mode in (0, 1, 'entropy', 'hi'):
        for strat, sn in ((zlib.Z_DEFAULT_STRATEGY, 'def'), (zlib.Z_FILTERED, 'filt')):
            for level in (6, 9):
                n = run(img, mode, level, strat)
                print(f'{str(mode):8s} {sn:4s} L{level} {n:9d} {n / img.size * 100:6.2f}%')
