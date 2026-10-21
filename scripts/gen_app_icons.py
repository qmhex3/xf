#!/usr/bin/env python3
"""Generates the launcher-icon resources: XML (adaptive icons, tile colours, dynamic-colour vector art) and manifest aliases.

The fixed-accent foreground art is rasterised by scripts/render_app_icons.py (lossless WebP, 864 px).

  python3 scripts/gen_app_icons.py            # writes into app/src/main/res and prints the manifest aliases

Accents x modes:  indigo ocean emerald sunset rose gold black dynamic  x  dark light amoled
Art colours come from one palette per accent (same keys as FrameAccents); the tile colour comes from the mode.
"""
import math, os, sys, re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'app/src/main/res')

# ---------------------------------------------------------------- geometry (icon 08 "Frame Burst")
def sub(a, b): return (a[0]-b[0], a[1]-b[1])
def add(a, b): return (a[0]+b[0], a[1]+b[1])
def mul(a, k): return (a[0]*k, a[1]*k)
def unit(v):
    l = math.hypot(*v); return (v[0]/l, v[1]/l)
def f(x):
    t = ('%.1f' % x).rstrip('0').rstrip('.'); return '0' if t in ('-0', '') else t
def P(p): return f(p[0]) + ' ' + f(p[1])

def inset(q, d):
    n = len(q); cx = sum(p[0] for p in q)/n; cy = sum(p[1] for p in q)/n; lines = []
    for i in range(n):
        a, b = q[i], q[(i+1) % n]; t = unit(sub(b, a)); nm = (-t[1], t[0])
        mid = ((a[0]+b[0])/2, (a[1]+b[1])/2)
        if (cx-mid[0])*nm[0] + (cy-mid[1])*nm[1] < 0: nm = (-nm[0], -nm[1])
        lines.append((add(a, mul(nm, d)), t))
    out = []
    for i in range(n):
        p1, t1 = lines[i-1]; p2, t2 = lines[i]
        den = t1[0]*t2[1] - t1[1]*t2[0]
        s = ((p2[0]-p1[0])*t2[1] - (p2[1]-p1[1])*t2[0]) / den
        out.append(add(p1, mul(t1, s)))
    return out

def rounded(pts, r, closed=True):
    n = len(pts); d = ''
    for i in range(n):
        V = pts[i]
        if not (closed or 0 < i < n-1):
            d += ('M' if i == 0 else 'L') + P(V) + ' '; continue
        A = pts[i-1]; B = pts[(i+1) % n]
        rr = r[i] if isinstance(r, list) else r
        rr = min(rr, math.hypot(*sub(A, V))/2, math.hypot(*sub(B, V))/2)
        p1 = add(V, mul(unit(sub(A, V)), rr)); p2 = add(V, mul(unit(sub(B, V)), rr))
        d += ('M' if i == 0 else 'L') + P(p1) + ' Q' + P(V) + ' ' + P(p2) + ' '
    return d + ('Z' if closed else '')

def at_x(a, b, x): t = (x-a[0])/(b[0]-a[0]); return (x, a[1]+(b[1]-a[1])*t)
def at_y(a, b, y): t = (y-a[1])/(b[1]-a[1]); return (a[0]+(b[0]-a[0])*t, y)

def lband(outer, w, r, cutx, cuty):
    TL, TR, BR, BL = outer; TLi, TRi, BRi, BLi = inset(outer, w)
    ri = max(r - w*0.55, 8)
    po = [at_x(TL, TR, cutx), TR, BR, BL, at_y(BL, TL, cuty)]
    pi = [at_y(BLi, TLi, cuty), BLi, BRi, TRi, at_x(TLi, TRi, cutx)]
    return rounded(po, r, False) + ' ' + rounded(pi, ri, False).replace('M', 'L', 1) + ' Z'

front = [(178, 266), (684, 210), (656, 598), (166, 640)]
c2 = [(240, 332), (746, 276), (718, 664), (228, 706)]
c3 = [(300, 440), (822, 378), (790, 752), (310, 778)]
fin = inset(front, 44)
D = dict(
    ring=rounded(front, 40) + ' ' + rounded(fin, 14),
    screen=rounded(fin, 14),
    c2=lband(c2, 38, 40, 668, 630),
    c3=lband(c3, 36, 40, 730, 690),
    halo=rounded(inset(front, -16), 52),
    halo2=rounded(inset(c2, -12), 50),
    mount=rounded([(262, 550), (352, 396), (398, 457), (436, 428), (508, 527)], [10, 16, 10, 14, 10]),
    sun='M472 362 a31 31 0 1 0 62 0 a31 31 0 1 0 -62 0 Z',
)
# (name, path, fill, evenodd, in_monochrome)
SHAPES = [
    ('c3', 'g3', False), ('halo2', 'HALO', False), ('c2', 'g2', False), ('halo', 'HALO', False),
    ('ring', 'gF', True), ('screen', 'gS', False), ('screen', 'gGlow', False),
    ('mount', 'gM', False), ('sun', 'gSun', False),
]
CX, CY = (166+822)/2, (210+778)/2
SCALE = 0.074
TX, TY = 54 - CX*SCALE, 54 - CY*SCALE

# ---------------------------------------------------------------- colours
def hx(c): c = c.lstrip('#'); return tuple(int(c[i:i+2], 16) for i in (0, 2, 4))
def th(t): return '%02x%02x%02x' % tuple(max(0, min(255, round(v))) for v in t)
def mix(a, b, t): A, B = hx(a), hx(b); return th(tuple(A[i]*(1-t) + B[i]*t for i in range(3)))
def shade(a, k): return th(tuple(v*k for v in hx(a)))

ACCENTS = {  # lo (bottom-left)  mid  hi (top-right)   dark tile   light tile
    'indigo':  ('7a4cf6', '6a8bff', '52f0ff', '030F36', 'E3E7FA'),
    'ocean':   ('3b6df6', '2fa6f5', '5af0ff', '031A38', 'DCEEFA'),
    'emerald': ('0e9f8c', '25d89f', 'b4f25e', '03221E', 'DAF2E8'),
    'sunset':  ('e8457f', 'f4623a', 'ffb13d', '2A0E10', 'FBE3D8'),
    'rose':    ('a259f5', 'ee4c9a', 'ff86a8', '280A1E', 'FBDFE9'),
    'gold':    ('f2700f', 'f7a911', 'ffe45c', '291B05', 'F9EDCB'),
    'black':   ('8c8c8c', 'c4c4c4', 'f5f5f5', '141414', 'E6E6E6'),
}
# neutral accent: the light tile needs charcoal art (shading light greys gives too little contrast)
LIGHT_ART = {'black': ('222222', '444444', '767676')}
MODES = ('dark', 'light', 'amoled')
ORDER = list(ACCENTS) + ['dynamic']
SYS = '@android:color/system_'

def palette(accent, variant):
    """variant 'dark' (also AMOLED) or 'light'. Returns gradient dict id -> (kind, coords, stops)."""
    if accent == 'dynamic':
        if variant == 'dark': lo, mid, hi = SYS+'accent1_400', SYS+'accent2_300', SYS+'accent3_200'
        else: lo, mid, hi = SYS+'accent1_600', SYS+'accent2_500', SYS+'accent3_400'
        hi3 = hi
        scr0, scr1 = SYS+'accent1_900', SYS+'accent1_800'
        glow = '#FFFFFF'
        m0, m1, s0, s1 = SYS+'accent1_200', SYS+'accent2_200', SYS+'accent2_100', SYS+'accent3_100'
    else:
        lo0, mid0, hi0 = ACCENTS[accent][:3]
        k = 1.0 if variant == 'dark' else 0.8
        if variant == 'light' and accent in LIGHT_ART:
            lo, mid, hi = ['#'+c for c in LIGHT_ART[accent]]
            hi3 = '#'+shade(LIGHT_ART[accent][2], .92)
        else:
            lo, mid, hi = ['#'+shade(c, k) for c in (lo0, mid0, hi0)]
            hi3 = '#'+shade(hi0, k*0.92)
        base = '0A0A0A' if accent == 'black' else '040A26'   # screen tint base (neutral for black)
        scr0, scr1 = '#'+mix(base, mid0, .16), '#'+mix(base, mid0, .27)
        glow = '#'+hi0
        m0, m1 = '#'+mix(lo0, 'ffffff', .28), '#'+mix(mid0, 'ffffff', .32)
        s0, s1 = '#'+mix(mid0, 'ffffff', .30), '#'+mix(hi0, 'ffffff', .20)
    ga = .5 if accent != 'dynamic' else .3
    return {
        'gF':    ('lin', (166, 640, 690, 205), [(0, lo, 1), (.42, mid, 1), (1, hi, 1)]),
        'g2':    ('lin', (228, 706, 750, 270), [(0, lo, 1), (.5, mid, 1), (1, hi, 1)]),
        'g3':    ('lin', (310, 778, 825, 376), [(0, lo, 1), (.5, mid, 1), (1, hi3, 1)]),
        'gS':    ('lin', (220, 600, 630, 260), [(0, scr0, 1), (1, scr1, 1)]),
        'gGlow': ('rad', (590, 285, 170), [(0, glow, ga), (1, glow, 0)]),
        'gM':    ('lin', (262, 550, 440, 400), [(0, m0, 1), (1, m1, 1)]),
        'gSun':  ('lin', (472, 392, 534, 332), [(0, s0, 1), (1, s1, 1)]),
    }

def argb(c, op):
    if c.startswith('@'): return c
    c = c.lstrip('#'); return '#%02X%s' % (round(op*255), c.upper())

HEAD = '''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:pivotX="0"
        android:pivotY="0"
        android:scaleX="%g"
        android:scaleY="%g"
        android:translateX="%.3f"
        android:translateY="%.3f">
''' % (SCALE, SCALE, TX, TY)

def grad(g, indent=12):
    kind, c, stops = g; sp = ' ' * indent
    if kind == 'lin':
        top = f'<gradient android:type="linear" android:startX="{c[0]}" android:startY="{c[1]}" android:endX="{c[2]}" android:endY="{c[3]}">'
    else:
        top = f'<gradient android:type="radial" android:centerX="{c[0]}" android:centerY="{c[1]}" android:gradientRadius="{c[2]}">'
    out = f'{sp}<aapt:attr name="android:fillColor">\n{sp}    {top}\n'
    for o, col, op in stops: out += f'{sp}        <item android:offset="{o:g}" android:color="{argb(col, op)}" />\n'
    return out + f'{sp}    </gradient>\n{sp}</aapt:attr>\n'

def foreground_xml(accent, variant):
    G = palette(accent, variant); x = HEAD
    for name, fill, eo in SHAPES:
        ft = ' android:fillType="evenOdd"' if eo else ''
        if fill == 'HALO':
            x += f'        <path android:fillColor="{"#59000000" if variant == "dark" else "#24000000"}" android:pathData="{D[name]}" />\n'
        else:
            x += f'        <path{ft} android:pathData="{D[name]}">\n{grad(G[fill])}        </path>\n'
    return x + '    </group>\n</vector>\n'

def write(path, text):
    full = os.path.join(RES, path); os.makedirs(os.path.dirname(full), exist_ok=True)
    open(full, 'w', encoding='utf-8').write(text)

def bg_name(accent, mode): return 'ic_launcher_bg_amoled' if mode == 'amoled' else f'ic_launcher_bg_{accent}_{mode}'
def fg_name(accent, mode): return f'ic_launcher_fg_{accent}_' + ('light' if mode == 'light' else 'dark')

def main():
    # ---- foreground vectors (dark art is shared by dark + AMOLED)
    for a in ORDER:
        for v in ('dark', 'light'):
            if a == 'dynamic':
                write(f'drawable-v31/ic_launcher_fg_dynamic_{v}.xml', foreground_xml(a, v))
                write(f'drawable/ic_launcher_fg_dynamic_{v}.xml',   # < Android 12: falls back to the indigo art
                      f'<?xml version="1.0" encoding="utf-8"?>\n<layer-list xmlns:android="http://schemas.android.com/apk/res/android">\n'
                      f'    <item android:drawable="@drawable/ic_launcher_fg_indigo_{v}" />\n</layer-list>\n')
            # fixed-colour accents are high-resolution WebP made by scripts/render_app_icons.py
            # (drawable-nodpi/ic_launcher_fg_<accent>_<dark|light>.webp); only the dynamic art stays vector
    # ---- tile colours
    cols = ['<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_bg_amoled">#000000</color>']
    for a, v in ACCENTS.items():
        cols.append(f'    <color name="ic_launcher_bg_{a}_dark">#{v[3]}</color>')
        cols.append(f'    <color name="ic_launcher_bg_{a}_light">#{v[4]}</color>')
    cols.append('    <color name="ic_launcher_bg_dynamic_dark">#030F36</color>')
    cols.append('    <color name="ic_launcher_bg_dynamic_light">#E3E7FA</color>')
    write('values/ic_launcher_colors.xml', '\n'.join(cols) + '\n</resources>\n')
    write('values-v31/ic_launcher_colors.xml',
          '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
          '    <color name="ic_launcher_bg_dynamic_dark">@android:color/system_accent1_900</color>\n'
          '    <color name="ic_launcher_bg_dynamic_light">@android:color/system_accent1_100</color>\n</resources>\n')
    # ---- launcher icons: adaptive (API 26+) and layer-list (API 24-25)
    aliases = []
    for a in ORDER:
        for m in MODES:
            key = f'{a}_{m}'
            write(f'mipmap-anydpi-v26/ic_launcher_{key}.xml',
                  '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
                  f'    <background android:drawable="@color/{bg_name(a, m)}" />\n'
                  f'    <foreground android:drawable="@drawable/{fg_name(a, m)}" />\n'
                  '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n</adaptive-icon>\n')
            write(f'mipmap-anydpi-v24/ic_launcher_{key}.xml',
                  '<?xml version="1.0" encoding="utf-8"?>\n<layer-list xmlns:android="http://schemas.android.com/apk/res/android">\n'
                  '    <item>\n        <inset android:insetLeft="7%" android:insetTop="7%" android:insetRight="7%" android:insetBottom="7%">\n'
                  '            <shape android:shape="rectangle">\n'
                  f'                <solid android:color="@color/{bg_name(a, m)}" />\n'
                  '                <corners android:radius="12dp" />\n            </shape>\n        </inset>\n    </item>\n'
                  f'    <item android:drawable="@drawable/{fg_name(a, m)}" />\n</layer-list>\n')
            aliases.append(key)
    # default icon (application icon) = indigo / dark
    write('mipmap-anydpi-v26/ic_launcher.xml',
          '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
          '    <background android:drawable="@color/ic_launcher_bg_indigo_dark" />\n'
          '    <foreground android:drawable="@drawable/ic_launcher_fg_indigo_dark" />\n'
          '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n</adaptive-icon>\n')
    write('mipmap-anydpi-v24/ic_launcher.xml', open(os.path.join(RES, 'mipmap-anydpi-v24/ic_launcher_indigo_dark.xml')).read())
    # ---- manifest aliases
    out = []
    for key in aliases:
        cls = ''.join(p.capitalize() for p in key.split('_'))
        on = 'true' if key == 'indigo_dark' else 'false'
        out.append(f'''        <activity-alias
            android:name=".icon.{cls}"
            android:targetActivity=".MainActivity"
            android:enabled="{on}"
            android:exported="true"
            android:icon="@mipmap/ic_launcher_{key}"
            android:label="@string/app_name">
            <meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts" />
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity-alias>''')
    open(os.path.join(ROOT, 'scripts/icon_aliases.xml.txt'), 'w').write('\n'.join(out) + '\n')
    print('done:', len(aliases), 'aliases')

if __name__ == '__main__':
    main()
