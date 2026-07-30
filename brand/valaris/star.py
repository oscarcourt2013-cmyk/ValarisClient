"""Valaris mark geometry core."""
import math

PHI = (1 + 5 ** 0.5) / 2
CX = CY = 256.0


def u(deg):
    a = math.radians(deg - 90.0)
    return math.cos(a), math.sin(a)


def pt(deg, r, cx=CX, cy=CY):
    ux, uy = u(deg)
    return cx + ux * r, cy + uy * r


def f(v):
    s = f"{v:.3f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def needle_star(tips, valley_r, inpull=0.42, waist=0.10, tipx=0.045,
                shoulder=0.34, sweep=0.0, cx=CX, cy=CY):
    """Concave-ray star.

    tips    : [(angle_deg, radius)] in increasing angle
    valley_r: radius of the notches between rays
    inpull  : how hard the flank collapses toward the ray axis at the valley
    waist   : how far along the ray that collapse happens (small = deep waist)
    tipx    : residual half-width near the tip (0 = cusp, needs > 0 to print)
    shoulder: how long the needle holds its point
    sweep   : degrees each tip is rotated relative to its valleys, which makes
              the two flanks of every ray asymmetric -> cyclonic motion
    """
    n = len(tips)
    valleys = []
    for i in range(n):
        a0 = tips[i][0]
        a1 = tips[(i + 1) % n][0]
        if a1 <= a0:
            a1 += 360.0
        valleys.append((a0 + a1) / 2.0)

    d = []
    for i in range(n):
        ang, R = tips[i]
        ang += sweep
        T = pt(ang, R, cx, cy)
        ax, ay = u(ang)
        px, py = -ay, ax

        def edge(v_ang):
            V = pt(v_ang, valley_r, cx, cy)
            vx = (V[0] - cx) * px + (V[1] - cy) * py   # valley in ray frame
            vy = (V[0] - cx) * ax + (V[1] - cy) * ay
            span = R - vy
            c1l, c1a = vx * inpull, vy + span * waist
            c2l, c2a = vx * tipx, vy + span * (1 - shoulder)
            return (V,
                    (cx + px * c1l + ax * c1a, cy + py * c1l + ay * c1a),
                    (cx + px * c2l + ax * c2a, cy + py * c2l + ay * c2a))

        V1, a1c, a2c = edge(valleys[(i - 1) % n])
        if i == 0:
            d.append(f"M{f(V1[0])} {f(V1[1])}")
        d.append(f"C{f(a1c[0])} {f(a1c[1])} {f(a2c[0])} {f(a2c[1])} {f(T[0])} {f(T[1])}")
        V2, b1c, b2c = edge(valleys[i])
        d.append(f"C{f(b2c[0])} {f(b2c[1])} {f(b1c[0])} {f(b1c[1])} {f(V2[0])} {f(V2[1])}")
    d.append("Z")
    return " ".join(d)


def tips_8(rv, rh, rd):
    return [(0, rv), (45, rd), (90, rh), (135, rd),
            (180, rv), (225, rd), (270, rh), (315, rd)]


def tips_4(rv, rh):
    return [(0, rv), (90, rh), (180, rv), (270, rh)]


def svg(body, defs="", bg="#0a0a0a", size=512):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" '
            f'width="{size}" height="{size}">{defs}'
            f'<rect width="512" height="512" fill="{bg}"/>{body}</svg>')


def sheet(items, path, cols=4, thumbs=(56, 32, 20)):
    cells = "".join(
        f'<figure><div class=box>{s}</div><figcaption>{n}</figcaption></figure>'
        for n, s in items)
    rows = ""
    for px in thumbs:
        strip = "".join(
            f'<div style="width:{px}px">{s}</div>' for _, s in items)
        rows += f'<div class=row>{strip}</div>'
    open(path, "w").write(f"""<!doctype html><meta charset=utf-8><style>
body{{background:#141414;color:#8a8a8a;font:12px ui-monospace,monospace;margin:0;padding:22px}}
.grid{{display:grid;grid-template-columns:repeat({cols},1fr);gap:16px}}
figure{{margin:0}}
.box{{background:#0a0a0a;border:1px solid #242424;aspect-ratio:1}}
.box svg{{width:100%;height:auto;display:block}}
figcaption{{padding-top:6px;letter-spacing:.07em;text-transform:uppercase}}
.row{{display:flex;gap:16px;align-items:center;margin-top:22px;padding-top:18px;
border-top:1px solid #242424}}
.row svg{{width:100%;height:auto;display:block}}
</style><div class=grid>{cells}</div>{rows}""")
