# /// script
# requires-python = ">=3.11"
# dependencies = ["pillow==13.0.0"]
# ///
"""Verify exported brand dimensions, transparency, vector contents, and hashes."""

import hashlib
import json
from pathlib import Path
from xml.etree import ElementTree as ET

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
BRAND = ROOT / "src/main/resources/META-INF/resources/assets/brand"
manifest = json.loads((BRAND / "manifest.json").read_text())
variants = ("light", "dark", "mono-black", "mono-white")
checks = 0

for entry in manifest["files"]:
    path = BRAND / entry["path"]
    assert hashlib.sha256(path.read_bytes()).hexdigest() == entry["sha256"], path
    checks += 1
    if path.suffix == ".svg":
        root = ET.parse(path).getroot()
        assert root.tag == "{http://www.w3.org/2000/svg}svg", path
        for element in root.iter():
            tag = element.tag.rsplit("}", 1)[-1]
            assert tag not in {"text", "image", "script", "foreignObject"}, (path, tag)
            assert not any(key.rsplit("}", 1)[-1] == "href" for key in element.attrib), path
        checks += 1
    elif path.suffix == ".png":
        with Image.open(path) as image:
            image.verify()
        with Image.open(path) as image:
            assert image.size == (entry["width"], entry["height"]), path
        checks += 1

for variant in variants:
    for folder, stem, dimensions in [("icons", "icon", (512, 512)), ("marks", "mark", (512, 512)),
                                     ("logos", "logo", (2048, 512)), ("logos", "logo-no-tagline", (2048, 384))]:
        with Image.open(BRAND / folder / f"{stem}-{variant}.png") as image:
            assert image.size == dimensions
            assert image.mode == "RGBA"
            alpha = image.getchannel("A")
            assert alpha.getextrema() == (0, 255), (folder, stem, variant)
            bounds = alpha.getbbox()
            assert bounds is not None
            if folder != "icons":
                assert bounds[0] > 0 and bounds[1] > 0 and bounds[2] < image.width and bounds[3] < image.height, bounds
            if variant.startswith("mono"):
                for red, green, blue, alpha in image.get_flattened_data():
                    if alpha:
                        assert red == green == blue, (folder, stem, variant)
        checks += 1

for name in ("apple-touch-icon", "maskable-192", "maskable-512"):
    with Image.open(BRAND / "favicons" / f"{name}.png") as image:
        assert image.convert("RGBA").getchannel("A").getextrema() == (255, 255), name
    checks += 1

with Image.open(BRAND / "favicons/favicon.ico") as image:
    assert image.ico.sizes() == {(16, 16), (32, 32), (48, 48)}
    for size in (16, 32, 48):
        assert image.ico.getimage((size, size)).tobytes() == Image.open(BRAND / f"favicons/favicon-{size}.png").tobytes()
checks += 1

ide = ET.parse(ROOT / ".idea/icon.svg").getroot()
source = ET.parse(BRAND / "icons/icon-dark.svg").getroot()
assert ide.attrib["width"] == ide.attrib["height"] == "64"
for key in ("width", "height"):
    ide.attrib[key] = source.attrib[key]
assert ET.tostring(ide) == ET.tostring(source)
checks += 1

assert (ROOT / "src/main/resources/META-INF/resources/icons/icon.png").read_bytes() == (BRAND / "icons/icon-dark.png").read_bytes()
checks += 1

actual = {str(path.relative_to(BRAND)) for path in BRAND.rglob("*") if path.suffix in {".svg", ".png", ".ico"}}
assert actual == {entry["path"] for entry in manifest["files"]}
assert len(actual) == 41
checks += 1
print(f"PASS: {checks} checks across {len(actual)} assets; outlined SVGs, PNGs, ICO layers, and IDE icon verified.")
