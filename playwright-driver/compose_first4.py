import os
from PIL import Image, ImageDraw, ImageFont

SCREENSHOT_DIR = r"D:\owl2gen-dl\thesis\screenshots"

STEPS = [
    ("clean-00-landing.png", "1. Landing"),
    ("clean-01-construct-picker.png", "2. Entities & Constructs"),
    ("clean-02-structure.png", "3. Structure"),
    ("clean-03-reasoning.png", "4. Reasoning & Output"),
]

def load_font(size, bold=False):
    candidates = [
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
    ]
    for c in candidates:
        if os.path.exists(c):
            return ImageFont.truetype(c, size)
    return ImageFont.load_default()

PANEL_WIDTH = 1100
LABEL_H = 56
GAP = 28
BORDER = 2
TITLE_FONT = load_font(30, bold=True)

imgs = []
for fname, label in STEPS:
    im = Image.open(os.path.join(SCREENSHOT_DIR, fname))
    w, h = im.size
    new_h = int(h * (PANEL_WIDTH / w))
    im_scaled = im.resize((PANEL_WIDTH, new_h), Image.LANCZOS)
    imgs.append((label, im_scaled))

cols = 2
rows = 2
row_heights = []
for r in range(rows):
    row_items = imgs[r * cols:(r + 1) * cols]
    row_heights.append(max(im.size[1] for _, im in row_items) + LABEL_H)

total_w = cols * PANEL_WIDTH + (cols + 1) * GAP
total_h = sum(row_heights) + (rows + 1) * GAP

canvas = Image.new("RGB", (total_w, total_h), (247, 247, 250))
draw = ImageDraw.Draw(canvas)

y = GAP
for r in range(rows):
    row_items = imgs[r * cols:(r + 1) * cols]
    x = GAP
    for label, im in row_items:
        draw.text((x, y), label, fill=(25, 25, 35), font=TITLE_FONT)
        img_y = y + LABEL_H
        canvas.paste(im, (x, img_y))
        draw.rectangle(
            [x - BORDER, img_y - BORDER, x + im.size[0] + BORDER, img_y + im.size[1] + BORDER],
            outline=(205, 205, 215), width=BORDER,
        )
        x += PANEL_WIDTH + GAP
    y += row_heights[r] + GAP

out_path = os.path.join(SCREENSHOT_DIR, "owl2gen-wizard-steps1-4.png")
canvas.save(out_path)
print("Saved:", out_path, canvas.size)
