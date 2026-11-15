# Timesheets brand assets

The three time-entry strips form a compact T. The mint corner folds the top strip into a sheet. The wordmark is lowercase **timesheets**; the tagline is **Time, composed.**

## Files

Every icon, standalone mark, and logo is supplied as editable SVG paths and a matching transparent PNG. In the SVGs the strips and the lettering carry the class `ink` and the mint corner the class `fold`, so a page that puts a file inline can recolor them from its stylesheet, as the application's drawer does; a lockup's title is the brand's name, followed by the tagline where the lockup shows it, and is its accessible name inline. The SVGs contain no embedded raster images, external resources, or live text. PNGs are exported directly from the same SVGs.

| Directory | Contents | PNG size |
| --- | --- | --- |
| `icons/` | Rounded app tiles: `icon-light`, `icon-dark`, `icon-mono-black`, `icon-mono-white` | 512 × 512 |
| `marks/` | Standalone symbols without a tile: `mark-light`, `mark-dark`, `mark-mono-black`, `mark-mono-white` | 512 × 512 |
| `logos/` | Horizontal logos with the tagline: `logo-*` | 512 high, as wide as the artwork |
| `logos/` | Horizontal logos without the tagline: `logo-no-tagline-*` | 384 high, as wide as the artwork |
| `favicons/` | PNG favicons at 16, 32, and 48 px; SVG favicon; ICO containing all three PNG sizes | As named |
| `favicons/` | Full-bleed Apple touch icon and maskable PWA icons | 180, 192, and 512 px |
| `overview.png` | Contact sheet of the four palettes | 1920 × 1160 |
| `manifest.json` | Export inventory, dimensions, color values, and SHA-256 hashes | — |

`light` has blue artwork for light surfaces; its app tile is ivory. `dark` has ivory artwork for dark surfaces; its app tile is blue. The mint corner is retained in both color variants. `mono-black` and `mono-white` use only the named foreground color; their app tiles use the opposite background color. The standalone marks and logos have transparent backgrounds.

## Usage

- Use the full logo when there is room for the tagline to remain readable; use the version without the tagline in compact headers.
- Use the standalone mark when an existing surface supplies the background. Use the app tile for launchers, project lists, and app directories.
- Use the prepared 16 px favicon rather than shrinking the full logo: its corner detail is omitted for clarity. The ICO contains that same small-size artwork.
- Apple touch and maskable icons have square, opaque backgrounds so the platform can apply its own mask. The maskable versions place the symbol inside the central safe circle.
- Keep at least one strip's height of clear space around the symbol when placing it with unrelated content. Retain the spacing built into the supplied lockups. A lockup is cropped to its artwork, with only a hairline of transparent edge, so the layout that places it gives it its clear space and lines it up by the artwork itself.
- Keep the mint fold as part of the mark. Do not recolor individual strips, stretch the logo, or add outlines.

The primary blue is `#0256A9`, `oklch(0.46 0.15 255)`, the application's `--ts-brand-blue-color` and its light-theme accent. Ivory is `#F9F7F2`, `oklch(0.976 0.007 88.6)`; mint is `#87F1AC`, `oklch(0.876 0.139 153.9)`, the application's `--ts-brand-mint-color`. The files give the colors in hex: CairoSVG, which draws the PNG exports, paints a color it cannot read, such as `oklch()`, black, and tools outside the browser read `oklch()` no better. Dark-surface branding retains the same blue identity rather than using the UI's dark-mode accent, a neutral white.

Lettering is outlined from Instrument Sans, the application's typeface: weight 700 for the wordmark and 450 for the tagline. Its unmodified source font and OFL license are in `scripts/brand/fonts/` for reproducible exports. No font installation is required to display the finished assets.

The assets are served under `/assets/brand/`. The IDE project icon at `.idea/icon.svg` uses the dark app tile at 64 px. The application icon at `src/main/resources/META-INF/resources/icons/icon.png` is an exact copy of the dark 512 px app tile. The login page's high-contrast logo, `src/main/resources/META-INF/resources/themes/timesheets-aura/view/login-view-logo-mask.svg`, is an exact copy of `logos/logo-mono-white.svg`, kept in the theme's folder for the login stylesheet's `url()`. Neither the generator nor the verifier knows it: copy the logo there again after a regeneration that changes it; `ThemeStylesheetUrlsTest` fails while the two differ. Browser favicon configuration is separate from this asset collection.

## Rebuild and verify

Run from the repository root with Python 3.11 or later, `uv`, and a system Cairo library:

```sh
uv run scripts/brand/generate.py
uv run scripts/brand/verify.py
```

On macOS with Homebrew Cairo, if the library is not on the loader's search path:

```sh
DYLD_FALLBACK_LIBRARY_PATH=/opt/homebrew/lib uv run scripts/brand/generate.py
```

The generator is the source for shared symbol geometry, palettes, spacing, text outlines, SVGs, PNGs, favicons, the overview, `.idea/icon.svg`, and the application icon. Regenerate the collection after changing it; do not update only one exported file. The verifier checks dimensions, hashes, transparency, monochrome exports, self-contained vector contents, ICO layers, and IDE and application icon parity.
