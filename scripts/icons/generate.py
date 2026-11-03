# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""Build a Vaadin iconset module and its Java enum from an npm icon package.

Usage: uv run scripts/icons/generate.py SET [VERSION]

Downloads the set's package with npm, the latest version unless one is given, puts every SVG of it into the
iconset module, rewrites the enum's constants, updates the set's version in NOTICE, and prints the icons the
new version adds and removes.
"""

import json
import re
import subprocess
import sys
import tarfile
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SETS = {
    "gravity-ui": {
        "package": "@gravity-ui/icons",
        "url": "https://github.com/gravity-ui/icons",
        "svgs": "svgs",
        "size": 16,
        "module": "src/main/frontend/src/component/icon/gravity-ui-icons.js",
        "enum": "src/main/java/com/glebfox/timesheets/component/icon/GravityUiIcon.java",
        "notice": "Gravity UI Icons",
    },
}
# The enum's constants: everything between the opening brace and the first semicolon.
CONSTANTS = re.compile(r"(public enum \w+[^{]*\{\n\n)[A-Z0-9_,\s]+;")


def fetch(package, version, tmp):
    spec = f"{package}@{version}" if version else package
    tarball = subprocess.run(["npm", "pack", spec, "--silent", "--pack-destination", tmp],
                             check=True, capture_output=True, text=True).stdout.strip().splitlines()[-1]
    with tarfile.open(Path(tmp) / tarball) as tar:
        tar.extractall(tmp, filter="data")
    return Path(tmp) / "package"


def read_svg(path):
    svg = re.sub(r"<!--.*?-->", "", path.read_text(encoding="utf-8"), flags=re.S)
    svg = " ".join(re.sub(r">\s+<", "><", svg).split())
    # The SVG goes into a template literal.
    if "`" in svg or "${" in svg:
        raise SystemExit(f"{path.name}: cannot be put into a template literal")
    return svg


def module(name, cfg, version, license_text, icons):
    header = "".join(f"* {line}".rstrip() + "\n" for line in [cfg["url"], version, "", *license_text.strip().splitlines()])
    body = "".join(f'    <g id="{name}:{icon}">{svg}</g>\n' for icon, svg in icons)
    return (f"/*\n{header}*/\n"
            "import '@vaadin/icon/vaadin-iconset.js';\n\n"
            "const template = document.createElement('template');\n\n"
            f'template.innerHTML = `<vaadin-iconset name="{name}" size="{cfg["size"]}">\n'
            f"  <svg><defs>\n{body}  </defs></svg>\n"
            "</vaadin-iconset>`;\n\n"
            "document.head.appendChild(template.content);\n")


def constant(icon):
    name = icon.upper().replace("-", "_")
    # A Java name cannot start with a digit; the enum strips the leading dash this gives back.
    return "_" + name if name[0].isdigit() else name


def build(set_name, version=None):
    cfg = SETS[set_name]
    module_path, enum_path, notice_path = ROOT / cfg["module"], ROOT / cfg["enum"], ROOT / "NOTICE"
    old = re.findall(rf'<g id="{set_name}:([^"]+)"', module_path.read_text(encoding="utf-8")) if module_path.exists() else []

    with tempfile.TemporaryDirectory() as tmp:
        package = fetch(cfg["package"], version, tmp)
        version = json.loads((package / "package.json").read_text(encoding="utf-8"))["version"]
        license_text = (package / "LICENSE").read_text(encoding="utf-8")
        icons = [(p.stem, read_svg(p)) for p in sorted((package / cfg["svgs"]).glob("*.svg"), key=lambda p: p.name)]

    module_path.write_text(module(set_name, cfg, version, license_text, icons), encoding="utf-8")

    enum = enum_path.read_text(encoding="utf-8")
    if len(CONSTANTS.findall(enum)) != 1:
        raise SystemExit(f"{enum_path}: the constants block was not found once")
    constants = ",\n".join(f"    {constant(icon)}" for icon, _ in icons) + ";"
    enum_path.write_text(CONSTANTS.sub(lambda m: m.group(1) + constants, enum), encoding="utf-8")

    notice = notice_path.read_text(encoding="utf-8")
    line = re.compile(rf"^(- {re.escape(cfg['notice'])} )\S+( \()", re.M)
    if line.search(notice):
        notice_path.write_text(line.sub(rf"\g<1>{version}\g<2>", notice), encoding="utf-8")
    else:
        print(f"NOTICE has no line for {cfg['notice']}; add one")

    names = [icon for icon, _ in icons]
    added, removed = sorted(set(names) - set(old)), sorted(set(old) - set(names))
    print(f"{cfg['package']} {version}: {len(names)} icons, {len(added)} added, {len(removed)} removed")
    if added:
        print("Added:", " ".join(added))
    if removed:
        print("Removed:", " ".join(removed))


if __name__ == "__main__":
    if len(sys.argv) not in (2, 3) or sys.argv[1] not in SETS:
        raise SystemExit(f"Usage: uv run scripts/icons/generate.py {{{','.join(SETS)}}} [VERSION]")
    build(*sys.argv[1:])
