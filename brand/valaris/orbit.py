"""Valaris — orbit mark. Dimensional ring: near arc in front, far arc behind,
ring weight varying through the perspective.
"""
import math
from star import needle_star, tips_8, f, PHI, CX, CY

TAPER = dict(inpull=0.26, waist=0.045, tipx=0.028, shoulder=0.44)
FLARE = dict(inpull=0.22, waist=0.03, tipx=0.011, shoulder=0.60)
HR = 1.12


# ---------------------------------------------------------------- the star
def star_body(r, dr=0.355, valley=0.125, cx=CX, cy=CY):
    return needle_star(tips_8(r, r / HR, r * dr), r * valley, cx=cx, cy=cy, **TAPER)


def star_flare(r, w=0.016):
    return needle_star([(45 + i * 90, r) for i in range(4)], r * w, **FLARE)


# ---------------------------------------------------------------- smoothing
def smooth(pts, close=False):
    """Catmull-Rom through pts, emitted as cubic Beziers."""
    n = len(pts)
    d = [f"M{f(pts[0][0])} {f(pts[0][1])}"]
    last = n if close else n - 1
    for i in range(last):
        p0 = pts[(i - 1) % n] if close else pts[max(i - 1, 0)]
        p1, p2 = pts[i % n], pts[(i + 1) % n]
        p3 = pts[(i + 2) % n] if close else pts[min(i + 2, n - 1)]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
        d.append(f"C{f(c1[0])} {f(c1[1])} {f(c2[0])} {f(c2[1])} {f(p2[0])} {f(p2[1])}")
    return " ".join(d)


def rot(p, deg, cx=CX, cy=CY):
    a = math.radians(deg)
    x, y = p[0] - cx, p[1] - cy
    return (cx + x * math.cos(a) - y * math.sin(a),
            cy + x * math.sin(a) + y * math.cos(a))


def ribbon(t0, t1, rx, ry, tilt, base, k, n=40, cx=CX, cy=CY):
    """Elliptical arc as a filled ribbon whose width tracks depth.

    Local frame has +y toward the viewer, so sin(t) is the depth cue: the arc is
    widest at the near point (t=pi/2) and narrowest at the far point (t=3pi/2),
    and passes through exactly `base` at t=0 and t=pi where the two arcs meet —
    so front and back join with no step.
    """
    outer, inner = [], []
    for i in range(n + 1):
        t = t0 + (t1 - t0) * i / n
        px, py = rx * math.cos(t), ry * math.sin(t)
        dx, dy = -rx * math.sin(t), ry * math.cos(t)
        L = math.hypot(dx, dy)
        nx, ny = -dy / L, dx / L
        w = base * (1 + k * math.sin(t)) / 2
        outer.append(rot((cx + px + nx * w, cy + py + ny * w), tilt, cx, cy))
        inner.append(rot((cx + px - nx * w, cy + py - ny * w), tilt, cx, cy))
    return (smooth(outer) + f" L{f(inner[-1][0])} {f(inner[-1][1])} "
            + smooth(inner[::-1])[1:] + " Z")


PI = math.pi


def orbit_mark(star_r=190, rx=228, ry_f=0.40, tilt=-24, base=13, k=0.45,
               gap=15, flares=True, flare_on_top=True, fg="#fff", uid="o",
               cx=CX, cy=CY):
    """Draw order is the whole trick:

        far arc (masked by the SOLID body only)  ->  star  ->  near arc

    Only the solid body is allowed into the mask. The hairline flares are far too
    thin to occlude anything convincingly, and letting them cut the ring is what
    shattered it into floating fragments — so they stay out of the mask and are
    laid over the ring instead, leaving the far arc with exactly one clean break
    behind the upper vertical ray.
    """
    ry = rx * ry_f
    body = star_body(star_r, cx=cx, cy=cy)
    fl = star_flare(star_r) if flares else ""
    back = ribbon(PI, 2 * PI, rx, ry, tilt, base, k, cx=cx, cy=cy)
    front = ribbon(0, PI, rx, ry, tilt, base, k, cx=cx, cy=cy)
    mask = (f'<mask id="{uid}"><rect width="512" height="512" fill="#fff"/>'
            f'<path d="{body}" fill="#000" stroke="#000" stroke-width="{f(gap)}" '
            f'stroke-linejoin="round"/></mask>')
    flare_el = f'<path d="{fl}" fill="{fg}"/>' if fl else ""
    return (mask,
            f'<g mask="url(#{uid})"><path d="{back}" fill="{fg}"/></g>'
            + (f'<path d="{body}" fill="{fg}"/>' + flare_el +
               f'<path d="{front}" fill="{fg}"/>' if not flare_on_top else
               f'<path d="{body}" fill="{fg}"/>'
               f'<path d="{front}" fill="{fg}"/>' + flare_el))


def doc(defs, body, size=512, bg="#0a0a0a"):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" '
            f'width="{size}" height="{size}"><defs>{defs}</defs>'
            + (f'<rect width="512" height="512" fill="{bg}"/>' if bg else "")
            + body + "</svg>")


def build(uid, bg="#0a0a0a", fg="#fff", **kw):
    d, b = orbit_mark(uid=uid, fg=fg, **kw)
    return doc(d, b, bg=bg)
