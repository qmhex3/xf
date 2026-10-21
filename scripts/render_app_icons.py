#!/usr/bin/env python3
"""Renders the launcher-icon art at full resolution (lossless WebP) from the same geometry as gen_app_icons.py.

  python3 scripts/render_app_icons.py     # needs: pip install playwright pillow  (and a Chromium)

Writes
  app/src/main/res/drawable-nodpi/ic_launcher_fg_<accent>_<dark|light>.webp   864x864 (108dp @ 8x), transparent
  store/ic_launcher_1024.png, store/ic_launcher_512.png                        full-bleed icon for the store listing
Dynamic-colour art cannot be rasterised (it uses system colours) and stays vector.
"""
import io, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_app_icons as G
from PIL import Image
from playwright.sync_api import sync_playwright

FG_PX = 864


def stop(o, col, op):
    return f'<stop offset="{o:g}" stop-color="{col}" stop-opacity="{op:g}"/>'


def svg(accent, variant, bg=None):
    pal = G.palette(accent, variant)
    defs, body = [], []
    for name, g in pal.items():
        kind, c, stops = g
        st = ''.join(stop(o, col, op) for o, col, op in stops)
        if kind == 'lin':
            defs.append(f'<linearGradient id="{name}" gradientUnits="userSpaceOnUse" x1="{c[0]}" y1="{c[1]}" x2="{c[2]}" y2="{c[3]}">{st}</linearGradient>')
        else:
            defs.append(f'<radialGradient id="{name}" gradientUnits="userSpaceOnUse" cx="{c[0]}" cy="{c[1]}" r="{c[2]}">{st}</radialGradient>')
    halo = '#000000' + ''
    halo_op = 0x59 / 255 if variant == 'dark' else 0x24 / 255
    for name, fill, eo in G.SHAPES:
        if fill == 'HALO':
            body.append(f'<path d="{G.D[name]}" fill="#000" fill-opacity="{halo_op:.4f}"/>')
        else:
            rule = ' fill-rule="evenodd"' if eo else ''
            body.append(f'<path d="{G.D[name]}" fill="url(#{fill})"{rule}/>')
    bgrect = f'<rect width="108" height="108" fill="{bg}"/>' if bg else ''
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="{FG_PX}" height="{FG_PX}">'
            f'<defs>{"".join(defs)}</defs>{bgrect}'
            f'<g transform="translate({G.TX:.3f} {G.TY:.3f}) scale({G.SCALE})">{"".join(body)}</g></svg>')


def shot(page, markup, px):
    page.set_viewport_size({'width': px, 'height': px})
    page.set_content(f'<html><body style="margin:0;background:transparent">{markup.replace(f"width=\"{FG_PX}\" height=\"{FG_PX}\"", f"width=\"{px}\" height=\"{px}\"")}</body></html>')
    return Image.open(io.BytesIO(page.screenshot(omit_background=True, clip={'x': 0, 'y': 0, 'width': px, 'height': px}))).convert('RGBA')


def main():
    out = os.path.join(G.RES, 'drawable-nodpi'); os.makedirs(out, exist_ok=True)
    store = os.path.join(G.ROOT, 'store'); os.makedirs(store, exist_ok=True)
    with sync_playwright() as pw:
        b = pw.chromium.launch(); page = b.new_page()
        for a in G.ACCENTS:
            for v in ('dark', 'light'):
                im = shot(page, svg(a, v), FG_PX)
                im.save(os.path.join(out, f'ic_launcher_fg_{a}_{v}.webp'), lossless=True, exact=True, quality=100, method=6)
        tile = '#' + G.ACCENTS['indigo'][3]
        big = shot(page, svg('indigo', 'dark', bg=tile), 1024).convert('RGB')
        big.save(os.path.join(store, 'ic_launcher_1024.png'), optimize=True)
        big.resize((512, 512), Image.LANCZOS).save(os.path.join(store, 'ic_launcher_512.png'), optimize=True)
        b.close()
    print('done')


if __name__ == '__main__':
    main()
