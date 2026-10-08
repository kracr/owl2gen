import os
from PIL import Image, ImageChops, ImageDraw, ImageFont

SCREENSHOT_DIR = r"D:\owl2gen-dl\thesis\screenshots"

STEPS = [
    ("raw-00-landing.png", "1. Landing"),
    ("raw-01-construct-picker.png", "2. Entities & Constructs"),
    ("raw-02-structure.png", "3. Structure"),
    ("raw-03-reasoning.png", "4. Reasoning & Output"),
    ("raw-04-review.png", "5. Review & Generate"),
    ("raw-05-results.png", "6. Results"),
    ("raw-06-graph.png", "7. Graph View"),
]

def autocrop_bottom(im, pad=24):
    """Crop trailing whitespace off the bottom only; keep full width."""
    rgb = im.convert("RGB")
    bg = Image.new("RGB", rgb.size, (255, 255, 255))
    diff = ImageChops.difference(rgb, bg)
    bbox = diff.getbbox()
    if not bbox:
        return im
    _, _, _, bottom = bbox
    bottom = min(im.height, bottom + pad)
    return im.crop((0, 0, im.width, bottom))

def load_font(size):
    candidates = [
        r"C:\Windows\Fonts\segoeuib.ttf",
        r"C:\Windows\Fonts\arialbd.ttf",
        r"C:\Windows\Fonts\arial.ttf",
    ]
    for c in candidates:
        if os.path.exists(c):
            return ImageFont.truetype(c, size)
    return ImageFont.load_default()

cropped = []
for fname, label in STEPS:
    path = os.path.join(SCREENSHOT_DIR, fname)
    im = Image.open(path)
    im = autocrop_bottom(im)
    out_name = fname.replace("raw-", "clean-")
    im.save(os.path.join(SCREENSHOT_DIR, out_name))
    cropped.append((out_name, label, im))
    print(f"{fname}: {Image.open(path).size} -> {im.size}")

# --- Composite: 2-column grid, each panel scaled to a common width ---
PANEL_WIDTH = 900
LABEL_H = 44
GAP = 20
BORDER = 2
FONT = load_font(26)

scaled = []
for out_name, label, im in cropped:
    w, h = im.size
    new_h = int(h * (PANEL_WIDTH / w))
    im_scaled = im.resize((PANEL_WIDTH, new_h), Image.LANCZOS)
    scaled.append((label, im_scaled))

n = len(scaled)
cols = 2
rows = (n + cols - 1) // cols

col_widths = [PANEL_WIDTH] * cols
row_heights = []
for r in range(rows):
    row_items = scaled[r * cols:(r + 1) * cols]
    row_heights.append(max(im.size[1] for _, im in row_items) + LABEL_H)

total_w = cols * PANEL_WIDTH + (cols + 1) * GAP
total_h = sum(row_heights) + (rows + 1) * GAP

canvas = Image.new("RGB", (total_w, total_h), (245, 245, 248))
draw = ImageDraw.Draw(canvas)

y = GAP
for r in range(rows):
    row_items = scaled[r * cols:(r + 1) * cols]
    x = GAP
    for label, im in row_items:
        draw.text((x, y), label, fill=(20, 20, 30), font=FONT)
        img_y = y + LABEL_H
        canvas.paste(im, (x, img_y))
        draw.rectangle(
            [x - BORDER, img_y - BORDER, x + im.size[0] + BORDER, img_y + im.size[1] + BORDER],
            outline=(200, 200, 210), width=BORDER,
        )
        x += PANEL_WIDTH + GAP
    y += row_heights[r] + GAP

composite_path = os.path.join(SCREENSHOT_DIR, "owl2gen-wizard-composite.png")
canvas.save(composite_path)
print("Composite saved:", composite_path, canvas.size)
