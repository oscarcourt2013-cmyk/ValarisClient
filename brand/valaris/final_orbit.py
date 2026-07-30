"""Valaris — final orbit mark. Star + dimensional ring, three optical sizes."""
import os, shutil, subprocess
from build_orbit import mark, svg
from measure import stats, rasterise

HERE = os.path.dirname(os.path.abspath(__file__))
DEST = r"C:\Users\Dave\Downloads\Oscar\Claude\StellarClient\brand\valaris"
PNG = os.path.join(DEST, "png")
ARCH = os.path.join(DEST, "archive-nova")
os.makedirs(PNG, exist_ok=True)
os.makedirs(ARCH, exist_ok=True)

# ---------------------------------------------------------------- tier specs
TIERS = {
    # display: hairline flares present, finest ring
    "display": dict(star_r=220, rx=232, ry_f=0.52, tilt=-30, base=12,   k=0.75, gap=10,
                    flares=True),
    # compact: flares dropped, ring carries enough weight to survive 20-48px
    "compact": dict(star_r=210, rx=224, ry_f=0.53, tilt=-29, base=25,   k=0.55, gap=16,
                    flares=False),
    # micro: cardinal-only star so the interior does not fill in at 16px
    "micro":   dict(star_r=196, rx=214, ry_f=0.56, tilt=-28, base=26,   k=0.50, gap=18,
                    flares=False, quad=True),
}

# Optical centring. The perspective taper puts extra mass on the near (lower)
# arc, dragging the ink centroid down and right of the geometric centre. A full
# centroid correction overshoots and throws the bounding box high, so each tier
# is corrected by 30% of its own measured offset — the point where the mark sits
# level in a tile without the bbox drifting.
CORRECTION = 0.30

# Frame fill. The display mark keeps generous clear space, as a logomark should.
# The small tiers are scaled up to nearly fill the canvas instead: at 16px every
# wasted row of padding is a wasted pixel of the mark.
FILL = {"display": 0.845, "compact": 0.955, "micro": 0.965}

SPEC = {}
for name, p in TIERS.items():
    st = stats(svg(*mark(**p)))
    s = (512.0 * FILL[name]) / max(st["w"], st["h"])
    dx = -(st["ink_cx"] - 256) * CORRECTION * s
    dy = -(st["ink_cy"] - 256) * CORRECTION * s
    SPEC[name] = dict(scale=round(s, 4), dx=round(dx, 2), dy=round(dy, 2))
    print(f"{name:8} ink off ({st['ink_cx']-256:+5.1f},{st['ink_cy']-256:+5.1f})  "
          f"bbox {st['w']:.0f}x{st['h']:.0f}  ->  scale {s:.3f}  "
          f"shift ({dx:+6.2f},{dy:+6.2f})")


def tier(name, fg="#fff", scale=1.0, bg=None, rx_tile=None, size=512):
    p = dict(TIERS[name])
    sp = SPEC[name]
    s = sp["scale"] * scale
    for key in ("star_r", "rx", "base", "gap"):
        p[key] = p[key] * s
    dx, dy = sp["dx"] * scale, sp["dy"] * scale
    return svg(*mark(fg=fg, dx=dx, dy=dy, **p), bg=bg, rx_tile=rx_tile, size=size)


# Archive the superseded flare-only mark, once. Guarded so re-running the export
# never sweeps the current orbit assets into the archive.
if not os.listdir(ARCH):
    for f_ in os.listdir(DEST):
        src = os.path.join(DEST, f_)
        if os.path.isfile(src) and (f_.endswith(".svg") or f_ in
                                    ("valaris-overview.png", "README.md")):
            shutil.move(src, os.path.join(ARCH, f_))
    for f_ in list(os.listdir(PNG)):
        shutil.move(os.path.join(PNG, f_), os.path.join(ARCH, f_))

# ---------------------------------------------------------------- assets
ASSETS = {
    "valaris-mark.svg":            tier("display", "currentColor"),
    "valaris-mark-white.svg":      tier("display", "#ffffff"),
    "valaris-mark-black.svg":      tier("display", "#000000"),
    "valaris-mark-compact.svg":    tier("compact", "currentColor"),
    "valaris-mark-micro.svg":      tier("micro",   "currentColor"),
    "valaris-icon-dark.svg":       tier("display", "#ffffff", 0.80, "#000000", 112),
    "valaris-icon-light.svg":      tier("display", "#000000", 0.80, "#ffffff", 112),
    "valaris-icon-dark-small.svg": tier("compact", "#ffffff", 0.80, "#000000", 112),
}
for n, s in ASSETS.items():
    open(os.path.join(DEST, n), "w", encoding="utf-8").write(s)

for px in (1024, 512, 256, 128, 64, 48):
    rasterise(tier("display", "#ffffff"), os.path.join(PNG, f"mark-white-{px}.png"), px)
for px in (32, 24, 20):
    rasterise(tier("compact", "#ffffff"), os.path.join(PNG, f"mark-white-{px}.png"), px)
rasterise(tier("micro", "#ffffff"), os.path.join(PNG, "mark-white-16.png"), 16)
for px in (1024, 512, 256):
    rasterise(tier("display", "#000000"), os.path.join(PNG, f"mark-black-{px}.png"), px)
rasterise(ASSETS["valaris-icon-dark.svg"], os.path.join(PNG, "icon-dark-1024.png"), 1024, False)
rasterise(ASSETS["valaris-icon-light.svg"], os.path.join(PNG, "icon-light-1024.png"), 1024, False)

# ---------------------------------------------------------------- overview
D_W, D_B = tier("display", "#ffffff"), tier("display", "#0d0d0d")
C_W, M_W = tier("compact", "#ffffff"), tier("micro", "#ffffff")
C_B, M_B = tier("compact", "#0d0d0d"), tier("micro", "#0d0d0d")


def px_row(sv, sizes, bg="#000"):
    return "".join(f'<div class=c style="background:{bg}"><div style="width:{p}px">{sv}</div>'
                   f'<span>{p}</span></div>' for p in sizes)


sheet = f"""<!doctype html><meta charset=utf-8><style>
*{{box-sizing:border-box}}
body{{background:#0d0d0d;color:#7a7a7a;font:12px/1.5 ui-monospace,SFMono-Regular,monospace;
margin:0;padding:40px}}
h1{{color:#f2f2f2;font-size:13px;letter-spacing:.34em;text-transform:uppercase;
font-weight:400;margin:0 0 4px}}
p.sub{{margin:0 0 34px;color:#5c5c5c;letter-spacing:.05em}}
h3{{font-size:10px;letter-spacing:.2em;text-transform:uppercase;color:#5c5c5c;
font-weight:400;margin:38px 0 14px;padding-bottom:9px;border-bottom:1px solid #222}}
.two{{display:grid;grid-template-columns:1fr 1fr;gap:2px}}
.pane{{aspect-ratio:1;display:flex;align-items:center;justify-content:center}}
.pane>div{{width:86%}} svg{{display:block;width:100%;height:auto}}
.row{{display:flex;gap:20px;align-items:flex-end;flex-wrap:wrap}}
.c{{display:flex;flex-direction:column;align-items:center;gap:9px;padding:14px;
border:1px solid #1e1e1e}}
.c span{{font-size:9px;color:#4a4a4a;letter-spacing:.1em}}
.tiles{{display:flex;gap:24px}} .tiles>div{{width:132px}}
.tier{{display:grid;grid-template-columns:repeat(3,1fr);gap:2px}}
.tier .pane{{background:#000}} .tier .pane>div{{width:72%}}
.cap{{display:flex;gap:2px;margin-top:2px}}
.cap div{{flex:1;padding:11px 14px;background:#131313;color:#6a6a6a;font-size:10px;
letter-spacing:.08em}}
.cap b{{color:#c8c8c8;font-weight:400;display:block;margin-bottom:3px;letter-spacing:.16em;
text-transform:uppercase}}
</style>
<h1>Valaris</h1><p class=sub>Primary mark — needle star with a dimensional orbit</p>

<h3>Primary mark</h3>
<div class=two><div class=pane style="background:#000"><div>{D_W}</div></div>
<div class=pane style="background:#fafafa"><div>{D_B}</div></div></div>

<h3>Optical size family</h3>
<div class=tier><div class=pane><div>{D_W}</div></div>
<div class=pane><div>{C_W}</div></div><div class=pane><div>{M_W}</div></div></div>
<div class=cap><div><b>Display</b>48px and up — hairline flares, finest ring</div>
<div><b>Compact</b>20–48px — flares dropped, ring weighted up</div>
<div><b>Micro</b>16px and below — cardinal-only star</div></div>

<h3>Rendered at true size — correct tier per size</h3>
<div class=row>{px_row(D_W,(96,64,48))}{px_row(C_W,(32,24,20))}{px_row(M_W,(16,))}</div>

<h3>Inverted, true size</h3>
<div class=row>{px_row(D_B,(96,64,48),'#fafafa')}{px_row(C_B,(32,24,20),'#fafafa')}
{px_row(M_B,(16,),'#fafafa')}</div>

<h3>Application tiles</h3>
<div class=tiles><div>{tier("display","#ffffff",0.80,"#000000",112)}</div>
<div>{tier("display","#000000",0.80,"#ffffff",112)}</div>
<div>{tier("display","#ffffff",0.80,"#242424",112)}</div>
<div>{tier("compact","#ffffff",0.80,"#000000",112)}</div></div>
"""
open(os.path.join(HERE, "showcase_orbit.html"), "w", encoding="utf-8").write(sheet)
subprocess.run([r"C:\Program Files\Google\Chrome\Application\chrome.exe", "--headless=new",
                "--disable-gpu", "--hide-scrollbars",
                f"--screenshot={os.path.join(DEST,'valaris-overview.png')}",
                "--window-size=1080,1900", f"--user-data-dir={os.path.join(HERE,'.cud')}",
                "file:///" + os.path.join(HERE, "showcase_orbit.html").replace("\\", "/")],
               capture_output=True)

open(os.path.join(DEST, "README.md"), "w", encoding="utf-8").write(f"""# Valaris — logomark

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
| display | 84.5% | {SPEC['display']['scale']} | ({SPEC['display']['dx']}, {SPEC['display']['dy']}) |
| compact | 95.5% | {SPEC['compact']['scale']} | ({SPEC['compact']['dx']}, {SPEC['compact']['dy']}) |
| micro | 96.5% | {SPEC['micro']['scale']} | ({SPEC['micro']['dx']}, {SPEC['micro']['dy']}) |

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
""")

print("\nwrote", DEST)
for f_ in sorted(os.listdir(DEST)):
    print("  ", f_)
