from pathlib import Path
from PIL import Image

src = Path(r"C:\Users\Zorat\Downloads\4b40025a-824c-4b90-8aef-020464fc02e5.ico")
res = Path(__file__).resolve().parents[1] / "resources"
tauri = Path(__file__).resolve().parents[1] / "src-tauri" / "icons"
res.mkdir(parents=True, exist_ok=True)
tauri.mkdir(parents=True, exist_ok=True)

(res / "icon.ico").write_bytes(src.read_bytes())
(tauri / "icon.ico").write_bytes(src.read_bytes())

ico = Image.open(src)
sizes = sorted(ico.ico.sizes(), key=lambda s: s[0] * s[1], reverse=True)
print("available", sizes)
largest = sizes[0]
im = ico.ico.getimage(largest).convert("RGBA")
print("exporting", im.size)

im.save(res / "icon.png")
im.save(tauri / "icon.png")
print("done", res / "icon.png", (res / "icon.png").stat().st_size)
