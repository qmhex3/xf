import sys, zlib, time
import numpy as np

BPP = 6


def load(path, w, h):
    a = np.fromfile(path, dtype='<u2').reshape(h, w * 3)
    return a.astype('>u2').view(np.uint8).reshape(h, w * 6)


def paeth(a, b, c):
    p = a.astype(np.int16) + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    return np.where((pa <= pb) & (pa <= pc), a, np.where(pb <= pc, b, c)).astype(np.uint8)


def filt(kind, row, prev):
    left = np.concatenate([np.zeros(BPP, np.uint8), row[:-BPP]])
    up = prev
    ul = np.concatenate([np.zeros(BPP, np.uint8), prev[:-BPP]])
    if kind == 0: return row
    if kind == 1: return row - left
    if kind == 2: return row - up
    if kind == 3: return row - ((left.astype(np.uint16) + up) >> 1).astype(np.uint8)
    return row - paeth(left, up, ul)


def score(f):
    s = f.astype(np.int16)
    s = np.where(s > 127, 256 - s, s)
    return int(s.sum())


def encode(img, mode, level, strategy=zlib.Z_DEFAULT_STRATEGY):
    h = img.shape[0]
    prev = np.zeros(img.shape[1], np.uint8)
    out = bytearray()
    for y in range(h):
        row = img[y]
        if mode == 'adaptive':
            cands = [filt(k, row, prev) for k in range(5)]
            k = min(range(5), key=lambda i: score(cands[i]))
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
    raw = img.size
    print(f'raw {raw}')
    for name, mode in [('none', 0), ('sub', 1), ('up', 2), ('avg', 3), ('paeth', 4), ('adaptive', 'adaptive')]:
        for level in (6, 9):
            t = time.time()
            n = encode(img, mode, level)
            print(f'{name:9s} L{level} {n:9d} {n / raw * 100:6.2f}% {time.time() - t:5.1f}s')
