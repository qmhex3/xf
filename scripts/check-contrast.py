from pathlib import Path
import re
import sys

source = Path("designsystem/src/main/java/com/frameout/designsystem/theme/Color.kt").read_text(encoding="utf-8")
colors = dict(re.findall(r"val\s+(\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)", source))

def luminance(value):
    rgb = [int(value[i:i + 2], 16) / 255 for i in (2, 4, 6)]
    rgb = [v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4 for v in rgb]
    return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]

def contrast(a, b):
    x, y = sorted((luminance(a), luminance(b)), reverse=True)
    return (x + 0.05) / (y + 0.05)

pairs = [
    ("DarkOnSurface/DarkBackground", "DarkOnSurface", "DarkBackground"),
    ("DarkOnSurfaceVariant/DarkBackground", "DarkOnSurfaceVariant", "DarkBackground"),
    ("DarkPrimary/DarkBackground", "DarkPrimary", "DarkBackground"),
    ("DarkOnPrimary/DarkPrimary", "DarkOnPrimary", "DarkPrimary"),
    ("DarkSuccess/DarkBackground", "DarkSuccess", "DarkBackground"),
    ("DarkWarning/DarkBackground", "DarkWarning", "DarkBackground"),
    ("DarkError/DarkBackground", "DarkError", "DarkBackground"),
    ("LightOnSurface/LightBackground", "LightOnSurface", "LightBackground"),
    ("LightOnSurfaceVariant/LightBackground", "LightOnSurfaceVariant", "LightBackground"),
    ("LightPrimary/LightBackground", "LightPrimary", "LightBackground"),
    ("LightOnPrimary/LightPrimary", "LightOnPrimary", "LightPrimary"),
    ("LightSuccess/LightBackground", "LightSuccess", "LightBackground"),
    ("LightWarning/LightBackground", "LightWarning", "LightBackground"),
    ("LightError/LightBackground", "LightError", "LightBackground"),
]
failed = False
for label, fg, bg in pairs:
    ratio = contrast(colors[fg], colors[bg])
    print(f"{label}: {ratio:.2f}:1")
    if ratio < 4.5:
        failed = True

accents = Path("designsystem/src/main/java/com/frameout/designsystem/theme/Accent.kt").read_text(encoding="utf-8")
pattern = re.compile(
    r"key = \"(\w+)\",\s*stops = longArrayOf\(([^)]*)\),\s*dark = longArrayOf\(([^)]*)\),\s*light = longArrayOf\(([^)]*)\)(?:,\s*tint = ([\d.]+)f)?"
)

def hexes(text):
    return [h.upper() for h in re.findall(r"0x([0-9A-Fa-f]{8})", text)]

def mix(base, over, amount):
    out = [round(int(base[i:i + 2], 16) * (1 - amount) + int(over[i:i + 2], 16) * amount) for i in (2, 4, 6)]
    return "FF" + "".join(f"{v:02X}" for v in out)

for key, stops, dark, light, tint in pattern.findall(accents):
    amount = float(tint) if tint else 0.06
    stops, dark, light = hexes(stops), hexes(dark), hexes(light)
    dark_bg = mix(colors["DarkBackground"], stops[0], amount)
    light_bg = mix(colors["LightBackground"], stops[0], amount * 0.8)
    checks = [
        (f"{key}: darkPrimary/darkBackground", dark[0], dark_bg),
        (f"{key}: darkOnPrimary/darkPrimary", dark[1], dark[0]),
        (f"{key}: lightPrimary/lightBackground", light[0], light_bg),
        (f"{key}: lightOnPrimary/lightPrimary", light[1], light[0]),
    ]
    for label, fg, bg in checks:
        ratio = contrast(fg, bg)
        print(f"{label}: {ratio:.2f}:1")
        if ratio < 4.5:
            failed = True
if failed:
    sys.exit(1)
