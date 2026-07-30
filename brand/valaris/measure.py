import os, subprocess, numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"


def rasterise(svg_text, out, px, transparent=True):
    html = os.path.join(HERE, "_m.html")
    open(html, "w", encoding="utf-8").write(
        '<!doctype html><meta charset=utf-8><style>html,body{margin:0;padding:0;'
        f'overflow:hidden}}svg{{display:block;width:{px}px;height:{px}px}}</style>' + svg_text)
    cmd = [CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars",
           f"--screenshot={out}", f"--window-size={px},{px}",
           f"--user-data-dir={os.path.join(HERE,'.cud')}"]
    if transparent:
        cmd.append("--default-background-color=00000000")
    cmd.append("file:///" + html.replace("\\", "/"))
    subprocess.run(cmd, capture_output=True)
    return out


def stats(svg_text, px=512):
    """Alpha bbox + ink centroid in viewBox units (512), plus ink coverage."""
    p = rasterise(svg_text, os.path.join(HERE, "_m.png"), px)
    a = np.asarray(Image.open(p).convert("RGBA"))[:, :, 3].astype(float) / 255.0
    ys, xs = np.nonzero(a > 0.04)
    if len(xs) == 0:
        return None
    s = 512.0 / px
    bbox = (xs.min() * s, ys.min() * s, xs.max() * s, ys.max() * s)
    w = a[ys, xs]
    cx, cy = (xs * w).sum() / w.sum() * s, (ys * w).sum() / w.sum() * s
    return dict(bbox=bbox, w=bbox[2] - bbox[0], h=bbox[3] - bbox[1],
                bbox_cx=(bbox[0] + bbox[2]) / 2, bbox_cy=(bbox[1] + bbox[3]) / 2,
                ink_cx=cx, ink_cy=cy, coverage=a.sum() / (px * px))


def report(name, st):
    print(f"{name:20} bbox {st['w']:6.1f}x{st['h']:6.1f}  "
          f"bboxC ({st['bbox_cx']:6.1f},{st['bbox_cy']:6.1f})  "
          f"inkC ({st['ink_cx']:6.1f},{st['ink_cy']:6.1f})  "
          f"cover {st['coverage']*100:5.2f}%")
