from PIL import Image, ImageDraw
import math
import os

bases = [
    r"C:\Users\Zorat\Desktop\Plugins MC\Elysia Client\mc-1.21.11\src\main\resources\assets\StellarClient\textures\cosmetics",
    r"C:\Users\Zorat\Desktop\Plugins MC\Elysia Client\mc-26.2\src\main\resources\assets\StellarClient\textures\cosmetics",
]


def cape(path, bg, accent, highlight, motif="prime"):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for y in range(0, 32):
        for x in range(0, 22):
            t = y / 31.0
            r = int(bg[0] * (1 - t) + accent[0] * t)
            g = int(bg[1] * (1 - t) + accent[1] * t)
            b = int(bg[2] * (1 - t) + accent[2] * t)
            if (x + y) % 5 == 0:
                r = min(255, r + 18)
                g = min(255, g + 12)
                b = min(255, b + 10)
            img.putpixel((x, y), (r, g, b, 255))
    d.rectangle([0, 0, 21, 31], outline=highlight + (255,))
    if motif == "prime":
        d.polygon([(7, 6), (14, 10), (7, 14), (9, 10)], fill=highlight + (255,))
        d.rectangle([8, 14, 12, 24], fill=highlight + (230,))
    elif motif == "star":
        cx, cy = 11, 12
        pts = []
        for i in range(10):
            ang = -math.pi / 2 + i * math.pi / 5
            rad = 6 if i % 2 == 0 else 2.5
            pts.append((cx + rad * math.cos(ang), cy + rad * math.sin(ang)))
        d.polygon(pts, fill=highlight + (255,))
    elif motif == "crimson":
        d.ellipse([5, 8, 17, 20], outline=highlight + (255,), width=2)
        d.line([(11, 10), (11, 22)], fill=highlight + (255,), width=2)
    else:
        d.rectangle([6, 8, 16, 22], outline=highlight + (255,), width=2)
        for px, py in [(9, 11), (13, 14), (10, 17), (14, 19)]:
            img.putpixel((px, py), highlight + (255,))
    img.save(path)


def wings(path, c1, c2, vein):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(2, 8), (28, 4), (30, 28), (8, 34), (2, 22)], fill=c1 + (210,))
    d.polygon([(2, 8), (28, 4), (22, 16), (6, 18)], fill=c2 + (180,))
    d.polygon([(62, 8), (36, 4), (34, 28), (56, 34), (62, 22)], fill=c1 + (210,))
    d.polygon([(62, 8), (36, 4), (42, 16), (58, 18)], fill=c2 + (180,))
    d.line([(4, 12), (26, 10)], fill=vein + (255,), width=1)
    d.line([(6, 20), (28, 18)], fill=vein + (255,), width=1)
    d.line([(60, 12), (38, 10)], fill=vein + (255,), width=1)
    d.line([(58, 20), (36, 18)], fill=vein + (255,), width=1)
    d.ellipse([24, 2, 32, 10], fill=c2 + (160,))
    d.ellipse([32, 2, 40, 10], fill=c2 + (160,))
    img.save(path)


caps = {
    "cape_prime.png": ((20, 40, 120), (59, 130, 246), (147, 197, 253), "prime"),
    "cape_star.png": ((80, 55, 10), (255, 215, 0), (255, 248, 180), "star"),
    "cape_crimson.png": ((60, 10, 20), (225, 29, 72), (254, 202, 202), "crimson"),
    "cape_midnight.png": ((15, 15, 45), (99, 102, 241), (199, 210, 254), "midnight"),
}
wset = {
    "wings_ember.png": ((255, 80, 30), (255, 180, 60), (120, 20, 10)),
    "wings_aurora.png": ((34, 211, 238), (167, 139, 250), (8, 60, 80)),
}

for base in bases:
    os.makedirs(base, exist_ok=True)
    for name, (bg, ac, hi, motif) in caps.items():
        cape(os.path.join(base, name), bg, ac, hi, motif)
    for name, (c1, c2, vein) in wset.items():
        wings(os.path.join(base, name), c1, c2, vein)
    print("wrote", base, sorted(os.listdir(base)))
