import struct, sys

class Trunc(Exception): pass

class Bits:
    def __init__(s, d): s.d = d; s.pos = 0
    def bit(s):
        b = s.pos >> 3
        if b >= len(s.d): raise Trunc()
        v = (s.d[b] >> (7 - (s.pos & 7))) & 1
        s.pos += 1
        return v
    def u(s, n):
        v = 0
        for _ in range(n): v = (v << 1) | s.bit()
        return v
    def flag(s): return s.bit() == 1
    def skip(s, n):
        s.pos += n
        if (s.pos >> 3) > len(s.d): raise Trunc()
    def uvlc(s):
        zeros = 0
        while s.bit() == 0:
            zeros += 1
            if zeros >= 32: return 0xFFFFFFFF
        return s.u(zeros) + (1 << zeros) - 1

def leb128(d, p):
    v = 0
    for i in range(8):
        if p + i >= len(d): raise Trunc()
        b = d[p + i]
        v |= (b & 0x7F) << (i * 7)
        if not (b & 0x80): return v, p + i + 1
    raise Trunc()

def av1_color_config(r, profile):
    high = r.flag()
    if profile == 2 and high: r.skip(1)
    mono = False if profile == 1 else r.flag()
    desc = r.flag()
    if desc:
        cp, tc, mc = r.u(8), r.u(8), r.u(8)
    else:
        cp = tc = mc = 2
    if mono:
        return (r.flag(), desc, cp, tc, mc)
    if cp == 1 and tc == 13 and mc == 0:
        return (True, desc, cp, tc, mc)
    return (r.flag(), desc, cp, tc, mc)

def av1_sequence_header(r):
    profile = r.u(3)
    r.skip(1)
    reduced = r.flag()
    if reduced:
        r.skip(5)
    else:
        timing = r.flag()
        decoder_model = False
        buf_len = 0
        if timing:
            r.skip(64)
            if r.flag(): r.uvlc()
            decoder_model = r.flag()
            if decoder_model:
                buf_len = r.u(5) + 1
                r.skip(32)
                r.skip(10)
        initial_delay = r.flag()
        count = r.u(5) + 1
        for _ in range(count):
            r.skip(12)
            level = r.u(5)
            if level > 7: r.skip(1)
            if decoder_model:
                if r.flag():
                    r.skip(buf_len * 2 + 1)
            if initial_delay:
                if r.flag(): r.skip(4)
    wbits = r.u(4) + 1
    hbits = r.u(4) + 1
    r.skip(wbits + hbits)
    if not reduced:
        if r.flag(): r.skip(7)
    r.skip(3)
    if not reduced:
        r.skip(4)
        order_hint = r.flag()
        if order_hint: r.skip(2)
        force_sct = 2
        if not r.flag(): force_sct = r.u(1)
        if force_sct > 0:
            if not r.flag(): r.skip(1)
        if order_hint: r.skip(3)
    r.skip(3)
    return av1_color_config(r, profile)

def parse_av1(data):
    p = 0
    if data and (data[0] & 0x80): p = 4
    while p < len(data):
        h = data[p]; p += 1
        typ = (h >> 3) & 0xF
        ext = (h >> 2) & 1
        has_size = (h >> 1) & 1
        if ext: p += 1
        try:
            if has_size: size, p = leb128(data, p)
            else: size = len(data) - p
        except Trunc:
            return None
        end = min(p + size, len(data))
        if typ == 1:
            try:
                return av1_sequence_header(Bits(data[p:end]))
            except Trunc:
                return None
        p = end
    return None

def parse_vpcc(payload):
    if len(payload) < 12: return None
    if payload[0] < 1: return None
    b = payload[6]
    full = (b & 1) == 1
    return (full, payload[7], payload[8], payload[9])

def boxes(d, s, e):
    p = s
    while p + 8 <= e:
        size, typ = struct.unpack('>I4s', d[p:p+8])
        h = 8
        if size == 1:
            size = struct.unpack('>Q', d[p+8:p+16])[0]; h = 16
        elif size == 0: size = e - p
        if size < h or p + size > e: return
        yield typ, p + h, p + size
        p += size

def find_box(d, s, e, path):
    for t, ps, pe in boxes(d, s, e):
        if t == path[0]:
            if len(path) == 1: return ps, pe
            r = find_box(d, ps, pe, path[1:])
            if r: return r

def mp4_codec_boxes(d, typ):
    r = find_box(d, 0, len(d), [b'moov', b'trak', b'mdia', b'minf', b'stbl', b'stsd'])
    ps, pe = r
    entry = ps + 8
    size = struct.unpack('>I', d[entry:entry+4])[0]
    es = entry + 8 + 78
    for t, a, b in boxes(d, es, min(pe, entry + size)):
        if t == typ: return d[a:b]

EBML_IDS = {0x18538067: 'seg', 0x1654AE6B: 'tracks', 0xAE: 'entry', 0x83: 'ttype', 0xE0: 'video', 0x55B0: 'colour',
            0x55B1: 'matrix', 0x55B9: 'range', 0x55BA: 'transfer', 0x55BB: 'primaries'}
MASTER = {'seg', 'tracks', 'entry', 'video', 'colour'}

def ebml_id(d, p):
    if p >= len(d): raise Trunc()
    b = d[p]
    n = 1
    while n <= 4 and not (b & (0x80 >> (n - 1))): n += 1
    if n > 4 or p + n > len(d): raise Trunc()
    return int.from_bytes(d[p:p+n], 'big'), p + n

def ebml_size(d, p):
    if p >= len(d): raise Trunc()
    b = d[p]
    n = 1
    while n <= 8 and not (b & (0x80 >> (n - 1))): n += 1
    if n > 8 or p + n > len(d): raise Trunc()
    v = b & (0xFF >> n)
    unknown = v == (0xFF >> n)
    for i in range(1, n):
        v = (v << 8) | d[p + i]
        unknown = unknown and d[p + i] == 0xFF
    return (-1 if unknown else v), p + n

def mkv_walk(d, s, e, depth, out, state):
    p = s
    while p < e:
        try:
            i, q = ebml_id(d, p)
            size, q = ebml_size(d, q)
        except Trunc:
            return
        end = e if size < 0 else min(q + size, e)
        name = EBML_IDS.get(i)
        if name in MASTER:
            if name == 'entry': state['cur'] = {'video': False, 'colour': None}
            if name == 'colour': state['colour'] = {}
            mkv_walk(d, q, end, depth + 1, out, state)
            if name == 'colour': state['cur']['colour'] = state.pop('colour')
            if name == 'entry':
                if state['cur']['video']: out.append(state['cur']['colour'])
            if name == 'tracks': state['done'] = True
            if state.get('done'): return
        elif name == 'ttype':
            if int.from_bytes(d[q:end], 'big') == 1: state['cur']['video'] = True
        elif name in ('matrix', 'range', 'transfer', 'primaries') and 'colour' in state:
            state['colour'][name] = int.from_bytes(d[q:end], 'big')
        elif name == 'video' :
            pass
        if name not in MASTER:
            if size < 0: return
        p = end

def parse_mkv(d):
    out = []
    state = {}
    mkv_walk(d, 0, len(d), 0, out, state)
    return out
