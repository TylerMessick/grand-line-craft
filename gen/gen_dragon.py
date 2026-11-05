#!/usr/bin/env python3
"""The azure dragon: a proper Minecraft-style Eastern dragon.

Defines the rig as a tree of parts (pixel units, Minecraft model space: y down, forward = -z), packs every box into
a painted 256x256 texture (azure scales, cream belly scutes, dark mane, ivory horns/teeth/claws, fin plates), and
writes:
  - src/main/resources/assets/grandline/textures/entity/seiryu_dragon.png
  - src/main/java/com/messickmedia/grandline/client/DragonLayer.java   (the LayerDefinition + part names)
  - preview/dragon.js                                                   (the same rig for the three.js preview)
Run from the repo root: python3 gen/gen_dragon.py
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..')
TEX_PATH = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'grandline', 'textures', 'entity', 'seiryu_dragon.png')
JAVA_PATH = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'messickmedia', 'grandline', 'client', 'DragonLayer.java')
JS_PATH = os.path.join(ROOT, 'preview', 'dragon.js')
TEX_SIZE = 256
random.seed(11)

# ------------------------------------------------------------------ rig description

class Part:
    def __init__(self, name, pivot, rot=(0, 0, 0)):
        self.name = name
        self.pivot = pivot
        self.rot = rot
        self.boxes = []      # (origin, size, paint, inflate)
        self.children = []

    def box(self, origin, size, paint, inflate=0.0):
        self.boxes.append((origin, size, paint, inflate))
        return self

    def child(self, part):
        self.children.append(part)
        return part


def build():
    root = Part('root', (0, 0, 0))
    # ---- body chain: chest at the shoulders, segments run back (+z). Slim and long, so the curve reads smoothly.
    chest = root.child(Part('body0', (0, 7, 0)))
    seg_sizes = [11, 11, 10.5, 10, 9.5, 9]
    seg_len = 9
    prev = chest
    z = 0
    for i, s in enumerate(seg_sizes):
        part = prev if i == 0 else prev.child(Part(f'body{i}', (0, 0, z)))
        h = s / 2
        z0 = -seg_len / 2 if i == 0 else 0
        part.box((-h, -h, z0), (s, s, seg_len), 'scales')
        part.box((-h + 1.5, h - 1.2, z0 + 0.5), (s - 3, 2, seg_len - 1), 'belly', 0.3)
        # dorsal fin: two short plates, the second a little lower, so the ridge ripples along the back
        part.box((-0.5, -h - 3.5, z0 + 1), (1, 4, 3), 'fin')
        part.box((-0.5, -h - 2.5, z0 + 5), (1, 3, 3), 'fin')
        z = seg_len / 2 if i == 0 else seg_len
        prev = part
    # ---- tail: ten tapering segments and a flame-like tuft
    tail_sizes = [8.5, 8, 7.5, 7, 6.5, 6, 5.5, 5, 4.5, 4]
    for i, s in enumerate(tail_sizes):
        part = prev.child(Part(f'tail{i}', (0, 0, z)))
        h = s / 2
        ln = 7
        part.box((-h, -h, 0), (s, s, ln), 'scales')
        part.box((-h + 1, h - 1, 0.5), (s - 2, 1.5, ln - 1), 'belly', 0.2)
        part.box((-0.5, -h - 3, 1), (1, 3.5, 3), 'fin')
        if i % 2 == 0:
            part.box((-0.5, -h - 2, 4.5), (1, 2.5, 2), 'fin')
        z = ln
        prev = part
    tuft = prev.child(Part('tail_tuft', (0, 0, z)))
    tuft.box((-0.5, -7, -1), (1, 14, 8), 'fin')
    tuft.box((-5, -0.5, -1), (10, 1, 7), 'fin')
    tuft.box((-0.5, -3, 6), (1, 6, 4), 'fin')
    # ---- neck: three segments running forward (-z), with a crest of mane spines
    neck_sizes = [10.5, 10, 9.5]
    prev = chest
    z = -seg_len / 2
    for i, s in enumerate(neck_sizes):
        part = prev.child(Part(f'neck{i}', (0, -0.5 if i == 0 else 0, z)))
        h = s / 2
        ln = 9
        part.box((-h, -h, -ln), (s, s, ln), 'scales')
        part.box((-h + 1.5, h - 1.2, -ln + 0.5), (s - 3, 2, ln - 1), 'belly', 0.3)
        for k in range(3):
            part.box((-0.5, -h - 5 + (k % 2), -ln + 1 + k * 2.6), (1, 5.5, 1.6), 'mane')
        part.box((-h - 0.6, -1, -ln + 2), (0.6, 4, ln - 4), 'mane')      # side mane fringes
        part.box((h, -1, -ln + 2), (0.6, 4, ln - 4), 'mane')
        z = -ln
        prev = part
    # ---- head: big, long-snouted, fanged
    head = prev.child(Part('head', (0, 0, z)))
    head.box((-8, -9, -14), (16, 12, 14), 'scales')                  # skull
    head.box((-6, -5.5, -27), (12, 8, 13), 'scales')                 # snout
    head.box((-4, -6.5, -26), (8, 1.5, 7), 'scales')                 # nose ridge
    head.box((-5, -7, -27.5), (2.5, 1.5, 2.5), 'dark')               # nostrils
    head.box((2.5, -7, -27.5), (2.5, 1.5, 2.5), 'dark')
    head.box((-5.5, 2, -26.5), (11, 1.8, 12), 'teeth')               # upper fangs
    head.box((-8.5, -10.5, -13.5), (6, 2, 8), 'scales')              # brow ridges
    head.box((2.5, -10.5, -13.5), (6, 2, 8), 'scales')
    head.box((-8.5, -7.5, -12.5), (1, 3, 4), 'eye')                  # eyes
    head.box((7.5, -7.5, -12.5), (1, 3, 4), 'eye')
    head.box((-8.6, -3, -9), (0.6, 7, 7), 'mane')                    # cheek mane
    head.box((8, -3, -9), (0.6, 7, 7), 'mane')
    for k in range(4):
        head.box((-0.5, -15 + (k % 2), -12 + k * 2.6), (1, 6, 1.6), 'mane')   # crest spines
    head.box((-12, 0.5, -25), (8, 1, 1), 'whisker')                  # whiskers
    head.box((4, 0.5, -25), (8, 1, 1), 'whisker')
    head.box((-13, -3, -24), (1, 4.5, 1), 'whisker')
    head.box((12, -3, -24), (1, 4.5, 1), 'whisker')
    for side in (-1, 1):
        ear = head.child(Part(f'ear_{"l" if side > 0 else "r"}', (side * 7.5, -8, -5), (0, 0, side * 0.55)))
        ear.box((-1, -5.5, -2), (2, 5.5, 4.5), 'fin')
        horn = head.child(Part(f'horn_{"l" if side > 0 else "r"}', (side * 4.5, -9, -4), (-0.8, 0, side * 0.3)))
        horn.box((-1, -13, -1), (2, 13, 2), 'horn')
        horn.box((-0.5, -18, -0.5), (1, 6, 1), 'horn')
        horn.box((-0.5, -10, -6), (1, 1, 6), 'horn')                 # branch
        horn.box((-0.5, -13, -7), (1, 4, 1), 'horn')
        horn2 = head.child(Part(f'horn2_{"l" if side > 0 else "r"}', (side * 2, -9, -9), (-0.55, 0, side * 0.2)))
        horn2.box((-0.5, -7, -0.5), (1, 7, 1), 'horn')
    jaw = head.child(Part('jaw', (0, 3, -13)))
    jaw.box((-5.5, 0, -13.5), (11, 3.5, 13), 'scales')
    jaw.box((-5, -1.5, -13), (10, 1.8, 11), 'teeth')                 # lower fangs
    jaw.box((-3.5, 3.5, -12), (7, 5, 2), 'mane')                     # beard
    jaw.box((-1, 3.5, -7), (2, 8, 4), 'mane')
    # ---- legs: four small clawed arms
    for name, parent, px, pz in (('leg_fl', chest, 5.5, -2), ('leg_fr', chest, -5.5, -2), ('leg_bl', prev_body(root, 4), 4.5, 4), ('leg_br', prev_body(root, 4), -4.5, 4)):
        side = 1 if px > 0 else -1
        up = parent.child(Part(name, (px, 3, pz), (0, 0, side * 0.7)))
        up.box((-2, 0, -2), (4, 6, 4), 'scales')
        up.box((1.5 if side > 0 else -2.5, 1.5, 1.5), (1, 3.5, 2.5), 'fin')      # elbow tuft
        low = up.child(Part(name + '_low', (0, 6, 0), (0, 0, -side * 0.55)))
        low.box((-1.5, 0, -1.5), (3, 6, 3), 'scales')
        foot = low.child(Part(name + '_foot', (0, 6, 0)))
        foot.box((-2.5, 0, -3), (5, 1.8, 5), 'scales')
        for k, cx in enumerate((-2.6, -0.6, 1.4)):
            foot.box((cx, -0.4, -6), (1.2, 1.8, 3.5), 'claw')
        foot.box((-0.6, -0.4, 1.2), (1.2, 1.8, 2.2), 'claw')          # dew claw
    return root


def prev_body(root, i):
    p = root.children[0]
    for _ in range(i):
        p = [c for c in p.children if c.name.startswith('body')][0]
    return p


# ------------------------------------------------------------------ UV packing and painting

def unwrap_size(size):
    w, h, d = size
    return math.ceil(2 * (w + d)), math.ceil(h + d)


def pack(parts):
    """Shelf-pack every box. Returns {(part name, box index): (u, v)}."""
    items = []
    for p in parts:
        for i, (o, s, paint, inf) in enumerate(p.boxes):
            items.append((p.name, i, unwrap_size(s)))
    items.sort(key=lambda t: -t[2][1])
    pos = {}
    x = y = shelf = 0
    for name, i, (w, h) in items:
        if x + w > TEX_SIZE:
            x = 0
            y += shelf
            shelf = 0
        pos[(name, i)] = (x, y)
        x += w
        shelf = max(shelf, h)
    if y + shelf > TEX_SIZE:
        raise SystemExit(f'texture too small: needs {y + shelf} rows')
    return pos


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, 255)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def paint_face(img, x0, y0, w, h, paint, face):
    """face: 'top' 'bottom' 'north' 'south' 'west' 'east'."""
    px = img.load()
    for y in range(h):
        for x in range(w):
            r = random.random()
            if paint == 'scales':
                base, deep, light = rgb(0x3F9AD0), rgb(0x1F5F8E), rgb(0x8CD2F2)
                # scale lattice: overlapping arcs; top faces lighter, sides a touch darker
                # staggered rounded scales: each 5x4 cell is a scale with a dark lower edge and a bright crown
                col = x // 5
                sy = (y + (2 if col % 2 else 0)) % 4
                sx = x % 5
                c = base
                edge = (sy == 3) or (sy == 2 and sx in (0, 4))
                crown = (sy == 0 and 1 <= sx <= 3)
                if edge:
                    c = deep
                elif crown:
                    c = light
                elif sy == 1 and sx in (0, 4):
                    c = mix(base, deep, 0.5)
                if face == 'top':
                    c = mix(c, light, 0.18)
                elif face == 'bottom':
                    c = mix(c, deep, 0.35)
                if r < 0.06:
                    c = mix(c, deep, 0.5)
            elif paint == 'belly':
                base, deep, light = rgb(0xE8D4A8), rgb(0xB89C6A), rgb(0xFFF1CC)
                c = base
                if y % 3 == 0:
                    c = deep                         # scute seams
                elif y % 3 == 1:
                    c = light
                if r < 0.08:
                    c = mix(c, deep, 0.4)
            elif paint == 'fin':
                base, deep, light = rgb(0x2E7FC0), rgb(0x173E6B), rgb(0x9FDBFF)
                c = base
                if (x + y // 2) % 4 == 0:
                    c = light                        # ribs
                if y == 0 or x == 0:
                    c = deep
                if r < 0.1:
                    c = mix(c, deep, 0.5)
            elif paint == 'mane':
                base, deep, light = rgb(0x18284A), rgb(0x0B1326), rgb(0x4A78B8)
                c = base
                if y < 2 and face not in ('top', 'bottom'):
                    c = light                        # lit tips
                elif (x + y) % 3 == 0:
                    c = deep
                elif r > 0.9:
                    c = mix(base, light, 0.5)
            elif paint == 'horn':
                base, deep, light = rgb(0xEDE3C8), rgb(0xB9A77E), rgb(0xFFFBEE)
                c = base if (y % 3) else deep
                if r > 0.9:
                    c = light
            elif paint == 'teeth' or paint == 'claw':
                base, deep = rgb(0xFFFBF0), rgb(0xD2C7B0)
                c = base
                if paint == 'teeth' and x % 2 == 1:
                    c = deep                         # the gaps between fangs
                if paint == 'claw' and y > h - 2:
                    c = deep
            elif paint == 'eye':
                c = rgb(0xFF3A2A) if not (1 <= x <= 2 and 0 <= y <= 1) else rgb(0x2A0A08)
            elif paint == 'whisker':
                c = rgb(0xEDE3C8) if r > 0.2 else rgb(0xC9B48C)
            elif paint == 'dark':
                c = rgb(0x0C1526)
            else:
                c = rgb(0xFF00FF)
            px[x0 + x, y0 + y] = c


def paint(parts, pos):
    img = Image.new('RGBA', (TEX_SIZE, TEX_SIZE), (0, 0, 0, 0))
    for p in parts:
        for i, (o, s, paintk, inf) in enumerate(p.boxes):
            u, v = pos[(p.name, i)]
            w, h, d = [math.ceil(c) for c in s]
            # Minecraft box unwrap: top at (u+d, v) w x d; bottom at (u+d+w, v) w x d;
            # row 2 (v+d): right d x h | front w x h | left d x h | back w x h
            paint_face(img, u + d, v, w, d, paintk, 'top')
            paint_face(img, u + d + w, v, w, d, paintk, 'bottom')
            paint_face(img, u, v + d, d, h, paintk, 'west')
            paint_face(img, u + d, v + d, w, h, paintk, 'north')
            paint_face(img, u + d + w, v + d, d, h, paintk, 'east')
            paint_face(img, u + 2 * d + w, v + d, w, h, paintk, 'south')
    return img


# ------------------------------------------------------------------ emit Java + JS

def flatten(part, out):
    out.append(part)
    for c in part.children:
        flatten(c, out)
    return out


def fmt(v):
    return f'{float(v):.2f}F'.replace('.00F', 'F')


def java(root, pos):
    lines = []
    names = []
    def emit(part, parent_var):
        var = part.name.replace('-', '_') + 'P'
        names.append(part.name)
        cubes = 'CubeListBuilder.create()'
        for i, (o, s, paintk, inf) in enumerate(part.boxes):
            u, v = pos[(part.name, i)]
            cubes += f'\n                .texOffs({u}, {v}).addBox({fmt(o[0])}, {fmt(o[1])}, {fmt(o[2])}, {fmt(s[0])}, {fmt(s[1])}, {fmt(s[2])}' + (f', new CubeDeformation({fmt(inf)})' if inf else '') + ')'
        rx, ry, rz = part.rot
        pose = f'PartPose.offset({fmt(part.pivot[0])}, {fmt(part.pivot[1])}, {fmt(part.pivot[2])})' if not any(part.rot) else \
            f'PartPose.offsetAndRotation({fmt(part.pivot[0])}, {fmt(part.pivot[1])}, {fmt(part.pivot[2])}, {fmt(rx)}, {fmt(ry)}, {fmt(rz)})'
        lines.append(f'        PartDefinition {var} = {parent_var}.addOrReplaceChild("{part.name}", {cubes}, {pose});')
        for c in part.children:
            emit(c, var)
    for c in root.children:
        emit(c, 'root')
    body = '\n'.join(lines)
    return f'''package com.messickmedia.grandline.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.util.List;

/** GENERATED by gen/gen_dragon.py: the azure dragon rig and its UV layout. Do not edit by hand. */
@SuppressWarnings("unused")
public final class DragonLayer {{
    private DragonLayer() {{}}

    public static final List<String> PART_NAMES = List.of({', '.join(f'"{n}"' for n in names)});

    public static LayerDefinition create() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{body}
        return LayerDefinition.create(mesh, {TEX_SIZE}, {TEX_SIZE});
    }}
}}
'''


def js(root, pos):
    def node(part):
        return {'name': part.name, 'pivot': list(part.pivot), 'rot': list(part.rot),
                'boxes': [{'o': list(o), 's': list(s), 'uv': list(pos[(part.name, i)]), 'inf': inf} for i, (o, s, k, inf) in enumerate(part.boxes)],
                'children': [node(c) for c in part.children]}
    return 'const DRAGON_RIG = ' + json.dumps([node(c) for c in root.children]) + ';\n'


if __name__ == '__main__':
    root = build()
    parts = flatten(root, [])[1:]
    pos = pack(parts)
    img = paint(parts, pos)
    os.makedirs(os.path.dirname(TEX_PATH), exist_ok=True)
    img.save(TEX_PATH)
    with open(JAVA_PATH, 'w') as f:
        f.write(java(root, pos))
    os.makedirs(os.path.dirname(JS_PATH), exist_ok=True)
    with open(JS_PATH, 'w') as f:
        f.write(js(root, pos))
    print(len(parts), 'parts,', sum(len(p.boxes) for p in parts), 'boxes')
