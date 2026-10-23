import sys, asyncio
from playwright.sync_api import sync_playwright
src, out, w = sys.argv[1], sys.argv[2], int(sys.argv[3])
with sync_playwright() as p:
    b = p.chromium.launch(executable_path="/opt/pw-browsers/chromium")
    pg = b.new_page(viewport={"width": w, "height": 400})
    pg.goto("file://" + src)
    pg.screenshot(path=out, full_page=True)
    b.close()
