# Valaris — logomark

Wordless star mark. Monochrome only: pure white or pure black, no hue.

## Construction
Eight-point needle star drawn from exact polar geometry. Four solid cardinal rays
(vertical dominant, ratio 1.12 : 1), four short solid diagonals at 0.355 of the
vertical radius, and four hair-thin diffraction flares on the diagonals reaching
the full vertical radius. Every ray flank is a cubic whose handles are resolved in
that ray's own frame, giving a true hyperbolic taper into a 0.125R notch.
Diagonal length is derived from the golden ratio family; the flares terminate at
exactly the vertical ray radius so no ray outruns the cardinal cross.

## Optical size family
Pick by rendered size — do not scale one tier to do another's job.

| File | Use |
| --- | --- |
| `valaris-mark.svg` | 48px and up. Flares present. |
| `valaris-mark-compact.svg` | 20–48px. Flares dropped, body thickened. |
| `valaris-mark-micro.svg` | 16px and below. Maximum ink. |

`valaris-mark.svg`, `-compact` and `-micro` fill with `currentColor`, so they
inherit text colour. `-white` / `-black` are hardcoded if you need them.

## Tiles
`valaris-icon-dark.svg` / `-light.svg` — mark at 76% of a 512 tile, corner
radius 112 (iOS-style continuous-ish). `valaris-icon-dark-small.svg` uses the
compact body for favicons and tray icons.

## Clear space
Keep free space equal to the horizontal ray radius (0.89 of the vertical) on all
sides. Never rotate, stretch, outline, or add a gradient.

## Regenerating
`star.py` holds the geometry engine, `final2.py` the spec and export. Change the
constants at the top of `final2.py` and re-run to rebuild every asset and PNG.
