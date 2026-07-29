#!/usr/bin/env python3
"""Download a CC0 HDRI tonemap and slice it into Minecraft panorama cubemap faces."""

from __future__ import annotations

import math
import sys
import urllib.request
from pathlib import Path

import numpy as np
from PIL import Image

# CC0 — Poly Haven (Greg Zaal): https://polyhaven.com/a/venice_sunset
SOURCE_URL = (
    "https://dl.polyhaven.org/file/ph-assets/HDRIs/extra/Tonemapped%20JPG/venice_sunset.jpg"
)
FACE_SIZE = 1024
FACE_ORDER = ["panorama_0", "panorama_1", "panorama_2", "panorama_3", "panorama_4", "panorama_5"]
FACE_VECTORS = {
    "panorama_0": ((0, 0, 1), (1, 0, 0), (0, 1, 0)),
    "panorama_1": ((1, 0, 0), (0, 0, -1), (0, 1, 0)),
    "panorama_2": ((0, 0, -1), (-1, 0, 0), (0, 1, 0)),
    "panorama_3": ((-1, 0, 0), (0, 0, 1), (0, 1, 0)),
    "panorama_4": ((0, 1, 0), (1, 0, 0), (0, 0, -1)),
    "panorama_5": ((0, -1, 0), (1, 0, 0), (0, 0, 1)),
}


def generate_face(arr: np.ndarray, face_key: str, ow: int, oh: int) -> np.ndarray:
    fwd, rgt, up_ = [np.array(v, np.float64) for v in FACE_VECTORS[face_key]]
    h, w = arr.shape[:2]
    uu, vv = np.meshgrid(np.arange(ow), np.arange(oh))
    u = 2 * (uu + 0.5) / ow - 1
    v = 1 - 2 * (vv + 0.5) / oh
    dx = fwd[0] + u * rgt[0] + v * up_[0]
    dy = fwd[1] + u * rgt[1] + v * up_[1]
    dz = fwd[2] + u * rgt[2] + v * up_[2]
    n = np.sqrt(dx**2 + dy**2 + dz**2)
    dx /= n
    dy /= n
    dz /= n
    phi = np.arcsin(np.clip(dy, -1, 1))
    theta = np.arctan2(dx, dz)
    ex = (theta + math.pi) / (2 * math.pi) * w
    ey = ((math.pi / 2 - phi) / math.pi) * h
    ex0 = np.floor(ex).astype(np.int64)
    ey0 = np.floor(ey).astype(np.int64)
    fx = (ex - ex0)[..., None]
    fy = (ey - ey0)[..., None]
    ex0m = np.mod(ex0, w)
    ex1m = np.mod(ex0 + 1, w)
    ey0c = np.clip(ey0, 0, h - 1)
    ey1c = np.clip(ey0 + 1, 0, h - 1)
    c00 = arr[ey0c, ex0m].astype(np.float64)
    c10 = arr[ey0c, ex1m].astype(np.float64)
    c01 = arr[ey1c, ex0m].astype(np.float64)
    c11 = arr[ey1c, ex1m].astype(np.float64)
    return np.clip((c00 * (1 - fx) + c10 * fx) * (1 - fy) + (c01 * (1 - fx) + c11 * fx) * fy, 0, 255).astype(
        np.uint8
    )


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    cache = root / "tools" / ".cache"
    cache.mkdir(parents=True, exist_ok=True)
    source = cache / "venice_sunset.jpg"

    if not source.exists():
        print(f"Downloading {SOURCE_URL} ...")
        urllib.request.urlretrieve(SOURCE_URL, source)

    img = Image.open(source).convert("RGB")
    arr = np.array(img)
    print(f"Source: {img.width}x{img.height}")

    targets = [
        root / "mc-1.21.11" / "src" / "main" / "resources" / "assets" / "minecraft" / "textures" / "gui" / "title" / "background",
        root / "mc-26.2" / "src" / "main" / "resources" / "assets" / "minecraft" / "textures" / "gui" / "title" / "background",
    ]

    for out_dir in targets:
        out_dir.mkdir(parents=True, exist_ok=True)
        for face in FACE_ORDER:
            face_arr = generate_face(arr, face, FACE_SIZE, FACE_SIZE)
            out = out_dir / f"{face}.png"
            Image.fromarray(face_arr).save(out, optimize=True)
            print(f"Wrote {out.relative_to(root)}")

        # Remove vanilla haze overlay on the custom panorama.
        overlay = out_dir / "panorama_overlay.png"
        Image.new("RGBA", (16, 128), (0, 0, 0, 0)).save(overlay)
        print(f"Wrote {overlay.relative_to(root)}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
