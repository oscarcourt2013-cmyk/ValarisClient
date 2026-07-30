# Valaris — logomark

Wordless star mark: a needle star inside a dimensional orbit. Monochrome only —
pure white or pure black, no hue.

## Construction

**The star.** Eight-point needle star from exact polar geometry. Four solid
cardinal rays (vertical dominant, 1.12 : 1), four short solid diagonals at 0.355
of the vertical radius, and — at display size — four hair-thin diffraction flares
on the diagonals reaching the full vertical radius. Every flank is a cubic whose
handles are resolved in that ray's own frame, giving a true hyperbolic taper into
a notch at 0.125R.

**The orbit.** Not a stroked ellipse. It is a filled ribbon whose width tracks
depth: widest at the near point, narrowest at the far point, and passing through
exactly the base width at both side crossings, so the two halves join with no
step. The ring is then split at those crossings and drawn in three passes —

    far arc  ->  star  ->  near arc

so the ring genuinely passes behind the star at the top and in front of it at the
bottom. That ordering is what makes it read as an object in space rather than a
broken oval laid over a sparkle.

Only the solid star body is allowed into the occlusion mask. The hairline flares
are far too thin to occlude anything convincingly, and letting them cut the ring
shattered it into floating fragments — so they stay out of the mask and are laid
over the ring instead. The far arc therefore has exactly one clean break, behind
the upper vertical ray, with a 10-unit separation.

**Optical centring.** The near arc's extra mass drags the ink centroid down and
to the right of the geometric centre. Each tier is shifted by 30% of its own
measured centroid offset — a full correction overshoots and throws the bounding
box high. The display mark keeps generous clear space; the two small tiers are
scaled to nearly fill the canvas, because at 16px a row of padding is a wasted
pixel. Values applied:

| Tier | frame fill | scale | centring shift |
| --- | --- | --- | --- |
| display | 84.5% | 1.0015 | (-5.39, -9.06) |
| compact | 95.5% | 1.1292 | (-5.58, -9.41) |
| micro | 96.5% | 1.1736 | (-4.99, -8.82) |

## Optical size family

Pick by rendered size — do not scale one tier to do another's job.

| File | Use |
| --- | --- |
| `valaris-mark.svg` | 48px and up. Flares present, finest ring. |
| `valaris-mark-compact.svg` | 20–48px. Flares dropped, ring weighted up. |
| `valaris-mark-micro.svg` | 16px and below. Cardinal-only star. |

These three fill with `currentColor`, so they inherit text colour. `-white` and
`-black` are hardcoded if you need them.

## Tiles

`valaris-icon-dark.svg` / `-light.svg` — mark at 80% of a 512 tile, corner radius
112. `valaris-icon-dark-small.svg` uses the compact tier for favicons and tray
icons.

## Clear space

Keep free space equal to the star's horizontal ray radius on all sides. Never
rotate the mark (the orbit's inclination is part of the mark), stretch it,
outline it, or add a gradient.

## Regenerating

`star.py` is the needle-star engine, `orbit.py` the ribbon and smoothing,
`build_orbit.py` the composition, `final_orbit.py` the spec and export. Change
the tier constants at the top of `final_orbit.py` and re-run to rebuild
everything, including the measured optical centring.

The earlier flare-only mark (no orbit) is kept in `archive-nova/`.
