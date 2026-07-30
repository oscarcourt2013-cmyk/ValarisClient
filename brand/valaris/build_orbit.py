"""Valaris orbit mark — single source of truth for the composition."""
import math
from star import needle_star, tips_8, tips_4, f, CX, CY
from orbit import star_body, ribbon, PI

TAPER = dict(inpull=0.26, waist=0.045, tipx=0.028, shoulder=0.44)
FLARE = dict(inpull=0.22, waist=0.03, tipx=0.011, shoulder=0.60)
HR = 1.12
_uid = [0]


def flare(r, w=0.016, cx=CX, cy=CY):
    return needle_star([(45 + i * 90, r) for i in range(4)], r * w,
                       cx=cx, cy=cy, **FLARE)


def quad_body(r, valley=0.115, cx=CX, cy=CY):
    """Cardinal cross only — for micro sizes where diagonals turn to mush."""
    return needle_star(tips_4(r, r / HR), r * valley, cx=cx, cy=cy,
                       inpull=0.34, waist=0.07, tipx=0.07, shoulder=0.38)


def mark(star_r=220, rx=232, ry_f=0.52, tilt=-30, base=12, k=0.75, gap=13,
         flares=True, quad=False, dx=0.0, dy=0.0, fg="#fff",
         star_dr=0.355, star_valley=0.125, uid=None):
    """Star + one dimensional orbit.

    dx/dy shift the whole composition. The shift is applied to the geometry
    rather than via a transform on purpose: the far arc is clipped by a mask
    built from the star silhouette in absolute coordinates, so translating the
    drawn content without translating the mask would slide the occlusion gap
    off the star.
    """
    _uid[0] += 1
    mid = uid or f"vm{_uid[0]}"
    cx, cy = CX + dx, CY + dy
    ry = rx * ry_f
    body = (quad_body(star_r, cx=cx, cy=cy) if quad else
            needle_star(tips_8(star_r, star_r / HR, star_r * star_dr),
                        star_r * star_valley, cx=cx, cy=cy, **TAPER))
    fl = flare(star_r, cx=cx, cy=cy) if flares else ""
    back = ribbon(PI, 2 * PI, rx, ry, tilt, base, k, cx=cx, cy=cy)
    front = ribbon(0, PI, rx, ry, tilt, base, k, cx=cx, cy=cy)
    defs = (f'<mask id="{mid}"><rect width="512" height="512" fill="#fff"/>'
            f'<path d="{body}" fill="#000" stroke="#000" stroke-width="{f(gap)}" '
            f'stroke-linejoin="round"/></mask>')
    el = (f'<g mask="url(#{mid})"><path d="{back}" fill="{fg}"/></g>'
          f'<path d="{body}" fill="{fg}"/>'
          f'<path d="{front}" fill="{fg}"/>'
          + (f'<path d="{fl}" fill="{fg}"/>' if fl else ""))
    return defs, el


def svg(defs, el, size=512, bg=None, rx_tile=None):
    tile = (f'<rect width="512" height="512" rx="{rx_tile}" fill="{bg}"/>'
            if bg and rx_tile is not None else
            f'<rect width="512" height="512" fill="{bg}"/>' if bg else "")
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" '
            f'width="{size}" height="{size}"><defs>{defs}</defs>{tile}{el}</svg>')


def render(bg=None, rx_tile=None, size=512, **kw):
    d, e = mark(**kw)
    return svg(d, e, size=size, bg=bg, rx_tile=rx_tile)
