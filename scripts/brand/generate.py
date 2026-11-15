# /// script
# requires-python = ">=3.11"
# dependencies = ["fonttools[woff]==4.68.0", "cairosvg==2.9.1", "pillow==13.0.0"]
# ///
"""Build the Timesheets brand assets from shared geometry and outlined type."""

import hashlib
import json
from functools import cache
from pathlib import Path
import struct
from xml.sax.saxutils import escape

import cairosvg
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/META-INF/resources/assets/brand"
FONT = Path(__file__).with_name("fonts") / "InstrumentSans.woff2"
# The brand's colors in hex, though the theme writes its colors in oklch: CairoSVG, which draws the PNG exports, reads
# only named, hex, rgb(), and rgba() colors and paints any other color black, and the tools outside the browser that
# open the SVG files read oklch no better. In oklch: blue oklch(0.46 0.15 255), the theme's --ts-brand-blue-color;
# ivory oklch(0.976 0.007 88.6); mint oklch(0.876 0.139 153.9), the theme's --ts-brand-mint-color.
BLUE, IVORY, MINT = "#0256A9", "#F9F7F2", "#87F1AC"
TOP = "M36 0H416L496 80V108Q496 136 468 136H44L0 92V36Q0 0 36 0Z"
MIDDLE = "M136 164H388Q400 164 400 176V236Q400 274 362 274H92V208Z"
BOTTOM = "M160 304H378Q390 304 390 316V398Q390 410 378 410H124Q116 410 116 402V350Z"
FOLD = "M416 0L496 80H438Q416 80 416 58Z"
PALETTES = {
    "light": (BLUE, MINT, IVORY),
    "dark": (IVORY, MINT, BLUE),
    "mono-black": ("#000000", "#000000", "#FFFFFF"),
    "mono-white": ("#FFFFFF", "#FFFFFF", "#000000"),
}


# The artwork's parts carry classes, so a page that puts an SVG inline can recolor them from its stylesheet: "ink"
# for the strips and the lettering, "fold" for the mint corner. A fill set by a stylesheet wins over the fill
# attribute, and every other reader of the files ignores the classes.
def mark(color, accent, x, y, scale, small=False):
    fold = "" if small else f'<path class="fold" d="{FOLD}" fill="{accent}"/>'
    return (f'<g class="ink" transform="translate({x:g} {y:g}) scale({scale:g})" fill="{color}">'
            f'<path d="{TOP}"/><path d="{MIDDLE}"/><path d="{BOTTOM}"/>{fold}</g>')


def document(body, width, height, title):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
            f'viewBox="0 0 {width} {height}" role="img" aria-labelledby="title">\n'
            f'  <title id="title">{escape(title)}</title>\n'
            '  <desc>Timesheets: three time-entry strips form a T, with a folded corner. '
            'All lettering is outlined; no external resources are required.</desc>\n'
            f'  {body}\n</svg>\n')


@cache
def typeface(weight):
    return instantiateVariableFont(TTFont(FONT), {"wght": weight, "wdth": 100}, inplace=True)


def lettering(text, weight, size, x, baseline, color, tracking=0):
    """Returns the outlined text and its ink bounds (left, top, right, bottom) in the document's units."""
    font = typeface(weight)
    glyphs = font.getGlyphSet()
    cmap = font.getBestCmap()
    unit_scale = size / font["head"].unitsPerEm
    cursor = 0
    paths = []
    ink = None
    for char in text:
        name = cmap[ord(char)]
        pen = SVGPathPen(glyphs)
        glyphs[name].draw(pen)
        if pen.getCommands():
            paths.append(f'<path transform="translate({cursor:.4f} 0)" d="{pen.getCommands()}"/>')
            bounds = BoundsPen(glyphs)
            glyphs[name].draw(bounds)
            x_min, y_min, x_max, y_max = bounds.bounds
            box = (x_min + cursor, y_min, x_max + cursor, y_max)
            ink = box if ink is None else (min(ink[0], box[0]), min(ink[1], box[1]),
                                           max(ink[2], box[2]), max(ink[3], box[3]))
        cursor += font["hmtx"].metrics[name][0] + tracking / unit_scale
    svg = (f'<g class="ink" fill="{color}" aria-label="{escape(text)}" '
           f'transform="translate({x:g} {baseline:g}) scale({unit_scale:g} {-unit_scale:g})">'
           + "".join(paths) + "</g>")
    # Font units point up from the baseline; the document's point down
    return svg, (x + ink[0] * unit_scale, baseline - ink[3] * unit_scale,
                 x + ink[2] * unit_scale, baseline - ink[1] * unit_scale)


def icon(variant, size=512, square=False, maskable=False):
    color, accent, surface = PALETTES[variant]
    scale = 0.58 if maskable else 0.75
    x, y = (512 - 496 * scale) / 2, (512 - 410 * scale) / 2
    body = f'<rect width="512" height="512" rx="{0 if square else 100}" fill="{surface}"/>'
    body += mark(color, accent, x, y, scale, small=size < 32)
    svg = document(body, 512, 512, f"Timesheets app icon, {variant}")
    if size != 512:
        svg = svg.replace('width="512" height="512" viewBox=', f'width="{size}" height="{size}" viewBox=', 1)
    return svg


# The clear space around a lockup's artwork, in the lockup's units: just enough for a transparent edge. A lockup is
# cropped to its artwork, so it lines up with what is beside it and is as large as its box allows; the layout around
# it gives it room.
LOCKUP_MARGIN = 4


def lockup(variant, tagline):
    """Returns the lockup's SVG and its size in its own units."""
    color, accent, _ = PALETTES[variant]
    if tagline:
        x, y, scale = 48, 62, 0.60
        word, word_ink = lettering("timesheets", 700, 172, 410, 210, color, -1.2)
        line, line_ink = lettering("Time, composed.", 450, 52, 415, 291, color, 4)
        inks = [word_ink, line_ink]
        title = "Timesheets. Time, composed."
    else:
        x, y, scale = 48, 41.5, 0.50
        word, word_ink = lettering("timesheets", 700, 172, 360, 199, color, -1.2)
        line = ""
        inks = [word_ink]
        title = "Timesheets"
    inks.append((x, y, x + 496 * scale, y + 410 * scale))
    left = min(ink[0] for ink in inks) - LOCKUP_MARGIN
    top = min(ink[1] for ink in inks) - LOCKUP_MARGIN
    width = round(max(ink[2] for ink in inks) + LOCKUP_MARGIN - left)
    height = round(max(ink[3] for ink in inks) + LOCKUP_MARGIN - top)
    body = (f'<g transform="translate({-left:g} {-top:g})">'
            + mark(color, accent, x, y, scale) + word + line + "</g>")
    return document(body, width, height, title), width, height


def export(stem, svg, width, height):
    path = OUT / stem
    path.parent.mkdir(parents=True, exist_ok=True)
    path.with_suffix(".svg").write_text(svg, encoding="utf-8")
    cairosvg.svg2png(bytestring=svg.encode(), write_to=str(path.with_suffix(".png")),
                    output_width=width, output_height=height)


def overview():
    sheet = Image.new("RGB", (1920, 1160), IVORY)
    draw = ImageDraw.Draw(sheet)
    heading = ImageFont.load_default(size=36)
    label = ImageFont.load_default(size=24)
    draw.text((48, 32), "Timesheets / Time, composed.", font=heading, fill=BLUE)
    for index, variant in enumerate(PALETTES):
        left = index * 480
        background = {"light": IVORY, "dark": BLUE, "mono-black": "#FFFFFF", "mono-white": "#111111"}[variant]
        foreground = "#FFFFFF" if variant in {"dark", "mono-white"} else "#111111"
        draw.rectangle((left, 112, left + 479, 1159), fill=background)
        draw.text((left + 28, 138), variant.upper(), font=label, fill=foreground)
        for name, y, width, height in [(f"icons/icon-{variant}.png", 212, 320, 320),
                                      (f"marks/mark-{variant}.png", 590, 72, 72),
                                      (f"logos/logo-{variant}.png", 724, 400, None),
                                      (f"logos/logo-no-tagline-{variant}.png", 902, 400, None)]:
            with Image.open(OUT / name) as image:
                height = height or round(image.height * width / image.width)
                image = image.resize((width, height), Image.Resampling.LANCZOS)
                sheet.paste(image, (left + (480 - width) // 2, y), image)
    sheet.save(OUT / "overview.png")


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    for variant, (color, accent, _) in PALETTES.items():
        export(f"icons/icon-{variant}", icon(variant), 512, 512)
        svg = document(mark(color, accent, 8, 51, 1), 512, 512, f"Timesheets standalone mark, {variant}")
        export(f"marks/mark-{variant}", svg, 512, 512)
        # A lockup's PNG is 512 px high with the tagline and 384 px without, as wide as its artwork
        for stem, tagline, png_height in [("logo", True, 512), ("logo-no-tagline", False, 384)]:
            svg, width, height = lockup(variant, tagline)
            export(f"logos/{stem}-{variant}", svg, round(width * png_height / height), png_height)

    favicon_dir = OUT / "favicons"
    favicon_dir.mkdir(exist_ok=True)
    for size in (16, 32, 48):
        cairosvg.svg2png(bytestring=icon("dark", size).encode(),
                        write_to=str(favicon_dir / f"favicon-{size}.png"), output_width=size, output_height=size)
    (favicon_dir / "favicon.svg").write_text(icon("dark", 32), encoding="utf-8")
    sizes = (16, 32, 48)
    payloads = [(favicon_dir / f"favicon-{size}.png").read_bytes() for size in sizes]
    offset = 6 + 16 * len(sizes)
    entries = []
    for size, payload in zip(sizes, payloads):
        entries.append(struct.pack("<BBBBHHII", size, size, 0, 0, 1, 32, len(payload), offset))
        offset += len(payload)
    (favicon_dir / "favicon.ico").write_bytes(struct.pack("<HHH", 0, 1, len(sizes)) + b"".join(entries + payloads))
    for name, size, maskable in [("apple-touch-icon", 180, False), ("maskable-192", 192, True), ("maskable-512", 512, True)]:
        cairosvg.svg2png(bytestring=icon("dark", size, square=True, maskable=maskable).encode(),
                        write_to=str(favicon_dir / f"{name}.png"), output_width=size, output_height=size)

    # The IDE uses the same paths and palette as the dark app icon.
    (ROOT / ".idea/icon.svg").write_text(icon("dark", 64), encoding="utf-8")
    (ROOT / "src/main/resources/META-INF/resources/icons/icon.png").write_bytes(
        (OUT / "icons/icon-dark.png").read_bytes())
    overview()
    manifest = {"name": "Timesheets", "tagline": "Time, composed.",
                "colors": {"primary": BLUE, "ivory": IVORY, "mint": MINT},
                "font": "Instrument Sans", "font_source_sha256": hashlib.sha256(FONT.read_bytes()).hexdigest(),
                "files": []}
    for path in sorted(OUT.rglob("*")):
        if path.suffix not in {".svg", ".png", ".ico"}:
            continue
        entry = {"path": str(path.relative_to(OUT)), "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}
        if path.suffix in {".png", ".ico"}:
            with Image.open(path) as img:
                entry.update(width=img.width, height=img.height, mode=img.mode)
        manifest["files"].append(entry)
    (OUT / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(manifest['files'])} assets in {OUT}")


if __name__ == "__main__":
    build()
