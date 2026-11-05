#!/usr/bin/env python3
"""High-detail devil fruit item models.

Each fruit is a voxel solid of revolution at 1/32-block resolution, merged into axis-aligned boxes (Minecraft
model elements). Its art is painted in 3D: every texel of six orthographic views is traced back onto the fruit's
surface and coloured by a 3D pattern (spirals, scales), so the pattern flows continuously across every face.
Stems, curls, leaves and horns are extra boxes. Run from the repo root: python3 gen/gen_fruits.py
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'grandline')
TEX = os.path.join(ROOT, 'textures', 'item')
MODELS = os.path.join(ROOT, 'models', 'item')
V = 0.5          # voxel size in model units (1/32 block)
VIEW = 128       # texels per orthographic view
random.seed(3)


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, 255)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3)) + (255,)


def norm(v):
    l = math.sqrt(sum(c * c for c in v)) or 1.0
    return tuple(c / l for c in v)


# ------------------------------------------------------------------ geometry

def voxelize(profile, cy, ymin, ymax, cx=8.0, cz=8.0, squash=1.0):
    """profile(y) -> radius at height y (model units, y relative to cy). Returns a set of voxel cells (ix, iy, iz)."""
    cells = set()
    iy0, iy1 = int(math.floor((cy + ymin) / V)), int(math.ceil((cy + ymax) / V))
    for iy in range(iy0, iy1):
        y = (iy + 0.5) * V - cy
        r = profile(y)
        if r is None or r <= 0:
            continue
        rx, rz = r, r * squash
        ix0, ix1 = int(math.floor((cx - rx) / V)), int(math.ceil((cx + rx) / V))
        iz0, iz1 = int(math.floor((cz - rz) / V)), int(math.ceil((cz + rz) / V))
        for ix in range(ix0, ix1):
            for iz in range(iz0, iz1):
                x = (ix + 0.5) * V - cx
                z = (iz + 0.5) * V - cz
                if (x / rx) ** 2 + (z / rz) ** 2 <= 1.0:
                    cells.add((ix, iy, iz))
    return cells


def merge(cells):
    """Greedy merge of voxels into boxes: runs along x, then stack identical runs along z, then along y."""
    cells = set(cells)
    boxes = []
    while cells:
        ix, iy, iz = min(cells)
        # run along x
        x1 = ix
        while (x1 + 1, iy, iz) in cells:
            x1 += 1
        # extend along z while the whole x-run exists
        z1 = iz
        while all((x, iy, z1 + 1) in cells for x in range(ix, x1 + 1)):
            z1 += 1
        # extend along y while the whole x-z slab exists
        y1 = iy
        while all((x, y1 + 1, z) in cells for x in range(ix, x1 + 1) for z in range(iz, z1 + 1)):
            y1 += 1
        for x in range(ix, x1 + 1):
            for y in range(iy, y1 + 1):
                for z in range(iz, z1 + 1):
                    cells.discard((x, y, z))
        boxes.append(((ix * V, iy * V, iz * V), ((x1 + 1) * V, (y1 + 1) * V, (z1 + 1) * V)))
    return boxes


# ------------------------------------------------------------------ painting

# atlas layout: 3 x 2 views, each VIEW px. order: north(-z), south(+z), west(-x), east(+x), up(+y), down(-y)
VIEWS = ['north', 'south', 'west', 'east', 'up', 'down']
ATLAS_W, ATLAS_H = VIEW * 3, VIEW * 2


def view_tile(name):
    i = VIEWS.index(name)
    return (i % 3) * VIEW, (i // 3) * VIEW


def view_axes(name):
    """For a view, which model axes map to texel u (right) and v (down), and the sign of the depth axis."""
    return {
        'north': ((0, +1), (1, -1), (2, -1)),   # looking from -z toward +z: u = +x? MC north face: u runs +x->-x. We handle per-face below.
        'south': ((0, +1), (1, -1), (2, +1)),
        'west': ((2, +1), (1, -1), (0, -1)),
        'east': ((2, +1), (1, -1), (0, +1)),
        'up': ((0, +1), (2, +1), (1, +1)),
        'down': ((0, +1), (2, -1), (1, -1)),
    }[name]


def paint(name, surface_point, colour, span=16.0):
    """Paints the six views. surface_point(view, a, b) -> 3D point on the fruit for view coords (a, b in model units)
    or None. colour(p3) -> RGBA. Returns the atlas image."""
    img = Image.new('RGBA', (ATLAS_W, ATLAS_H), (0, 0, 0, 0))
    px = img.load()
    for view in VIEWS:
        ox, oy = view_tile(view)
        (ua, us), (va, vs), (da, ds) = view_axes(view)
        for ty in range(VIEW):
            for tx in range(VIEW):
                # texel centre in model units along the view's u/v axes
                a = (tx + 0.5) / VIEW * span
                b = (ty + 0.5) / VIEW * span
                coords = [0.0, 0.0, 0.0]
                coords[ua] = a if us > 0 else span - a
                coords[va] = b if vs > 0 else span - b
                p = surface_point(da, ds, coords)
                if p is None:
                    continue
                px[ox + tx, oy + ty] = colour(p)
    return img


def face_uv(view, frm, to, span=16.0):
    """UV rect (in 0..16 texture units of the atlas) of a box face seen in a view."""
    ox, oy = view_tile(view)
    (ua, us), (va, vs), _ = view_axes(view)
    def tex(a, b):
        u = a if us > 0 else span - a
        v = b if vs > 0 else span - b
        return ox + u / span * VIEW, oy + v / span * VIEW
    u0, v0 = tex(frm[ua], frm[va])
    u1, v1 = tex(to[ua], to[va])
    s = 16.0
    return [round(min(u0, u1) / ATLAS_W * s, 4), round(min(v0, v1) / ATLAS_H * s, 4),
            round(max(u0, u1) / ATLAS_W * s, 4), round(max(v0, v1) / ATLAS_H * s, 4)]


FACE_VIEW = {'north': 'north', 'south': 'south', 'west': 'west', 'east': 'east', 'up': 'up', 'down': 'down'}


def box_element(frm, to, tex='#body', uv_fn=None, rotation=None):
    faces = {}
    for face in ['north', 'south', 'west', 'east', 'up', 'down']:
        uv = uv_fn(face, frm, to) if uv_fn else [0, 0, 1, 1]
        f = {'uv': uv, 'texture': tex}
        # Minecraft's default face orientation: north/west faces run right-to-left relative to +x/+z. Our atlas
        # views are painted as seen from outside, so north and west need a horizontal flip of the uv rect.
        if face in ('north', 'east'):
            f['uv'] = [uv[2], uv[1], uv[0], uv[3]]
        faces[face] = f
    e = {'from': [round(c, 3) for c in frm], 'to': [round(c, 3) for c in to], 'faces': faces}
    if rotation:
        e['rotation'] = rotation
    return e


def solid_uv(u, v):
    """A uniform swatch at atlas pixel (u, v), 4x4 texels."""
    def fn(face, frm, to):
        return [round(u / ATLAS_W * 16, 4), round(v / ATLAS_H * 16, 4), round((u + 4) / ATLAS_W * 16, 4), round((v + 4) / ATLAS_H * 16, 4)]
    return fn


def swatch(img, u, v, colour, dark, light):
    px = img.load()
    for y in range(v, v + 4):
        for x in range(u, u + 4):
            r = random.random()
            px[x, y] = dark if r < 0.2 else light if r > 0.85 else colour


def write_model(name, elements, gui_scale=0.6):
    model = {
        'credit': 'Grand Line Craft devil fruit — generated by gen/gen_fruits.py',
        'gui_light': 'front',
        'textures': {'body': f'grandline:item/{name}', 'particle': f'grandline:item/{name}'},
        'elements': elements,
        'display': {
            'gui': {'rotation': [28, 40, 0], 'translation': [0, -0.6, 0], 'scale': [gui_scale, gui_scale, gui_scale]},
            'ground': {'translation': [0, 1.5, 0], 'scale': [0.4, 0.4, 0.4]},
            'fixed': {'rotation': [0, 0, 0], 'scale': [0.75, 0.75, 0.75]},
            'thirdperson_righthand': {'rotation': [0, 0, 0], 'translation': [0, 2.5, 1], 'scale': [0.4, 0.4, 0.4]},
            'thirdperson_lefthand': {'rotation': [0, 0, 0], 'translation': [0, 2.5, 1], 'scale': [0.4, 0.4, 0.4]},
            'firstperson_righthand': {'rotation': [0, 40, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'firstperson_lefthand': {'rotation': [0, 220, 0], 'translation': [0, 2, 0], 'scale': [0.5, 0.5, 0.5]},
            'head': {'translation': [0, 12, 0], 'scale': [0.9, 0.9, 0.9]},
        },
    }
    os.makedirs(MODELS, exist_ok=True)
    with open(os.path.join(MODELS, f'{name}.json'), 'w') as f:
        json.dump(model, f, separators=(',', ':'))


# ------------------------------------------------------------------ patterns

def tangent_basis(n):
    n = norm(n)
    helper = (0.0, 1.0, 0.0) if abs(n[1]) < 0.9 else (1.0, 0.0, 0.0)
    t = norm((n[1] * helper[2] - n[2] * helper[1], n[2] * helper[0] - n[0] * helper[2], n[0] * helper[1] - n[1] * helper[0]))
    b = (n[1] * t[2] - n[2] * t[1], n[2] * t[0] - n[0] * t[2], n[0] * t[1] - n[1] * t[0])
    return t, b


def fib_sphere(n, seed=0.0):
    pts = []
    g = math.pi * (3 - math.sqrt(5))
    for i in range(n):
        y = 1 - 2 * (i + 0.5) / n
        r = math.sqrt(1 - y * y)
        a = g * i + seed
        pts.append((math.cos(a) * r, y, math.sin(a) * r))
    return pts


def spiral_field(centres, turns=2.2, extent=0.62, width=0.11):
    """A field on the unit sphere: 1 inside a spiral stripe around any centre, 0 elsewhere, plus the stripe edge."""
    bases = [(c, tangent_basis(c)) for c in centres]

    def f(d):
        best = 0.0
        for c, (t, b) in bases:
            cosang = max(-1.0, min(1.0, d[0] * c[0] + d[1] * c[1] + d[2] * c[2]))
            r = math.acos(cosang)
            if r > extent:
                continue
            u = d[0] * t[0] + d[1] * t[1] + d[2] * t[2]
            v = d[0] * b[0] + d[1] * b[1] + d[2] * b[2]
            theta = math.atan2(v, u)
            # spiral: the stripe sits where (theta - turns*r/extent*2pi) wraps near 0
            phase = (theta - r / extent * turns * 2 * math.pi) % (2 * math.pi)
            dist = min(phase, 2 * math.pi - phase) * (r + 0.08)   # angular distance scaled by radius
            taper = 1.0 - r / extent
            stripe = max(0.0, 1.0 - dist / (width * (0.6 + taper)))
            best = max(best, stripe)
        return best
    return f


# ------------------------------------------------------------------ fruits

def sphere_surface(cx, cy, cz, R, squash=1.0, profile=None):
    """surface_point for a solid of revolution around the y axis: returns the first surface hit along the view's depth
    axis, or None. profile(y)->radius (default sphere)."""
    prof = profile or (lambda y: math.sqrt(max(0.0, R * R - y * y)))

    def inside(p):
        y = p[1] - cy
        r = prof(y)
        if r is None or r <= 0:
            return False
        return ((p[0] - cx) / r) ** 2 + ((p[2] - cz) / (r * squash)) ** 2 <= 1.0

    def sp(da, ds, coords):
        # ds < 0: the viewer sits on the negative side of the axis, so the nearest surface is the minimum coordinate
        centre = (cx, cy, cz)[da]
        others = [coords[i] - (cx, cy, cz)[i] for i in range(3) if i != da]
        if others[0] ** 2 + others[1] ** 2 > (R * 1.25) ** 2:
            return None
        steps = 140
        for i in range(steps):
            t = i / steps
            c = list(coords)
            c[da] = centre + ds * (R * 1.3) * (1 - 2 * t)
            if inside(c):
                return c
        return None
    return sp


def gomu():
    """Gomu Gomu no Mi: a plump round violet fruit covered in pale swirl stripes, with a curling golden-brown stem."""
    name = 'gomu_gomu'
    cx, cy, cz, R = 8.0, 7.0, 8.0, 6.6
    cells = voxelize(lambda y: math.sqrt(max(0.0, R * R - y * y)) if abs(y) <= R else None, cy, -R, R, cx, cz)
    boxes = merge(cells)
    base, deep, pale, stripe = rgb(0x7A3FA8), rgb(0x4E2470), rgb(0xA873D0), rgb(0xE4D0F6)
    field = spiral_field(fib_sphere(9, 0.7), turns=2.0, extent=0.62, width=0.24)

    def colour(p):
        d = norm((p[0] - cx, p[1] - cy, p[2] - cz))
        s = field(d)
        shade = 0.82 + 0.18 * max(0.0, d[1] * 0.6 + d[0] * 0.3 + 0.5)
        c = mix(deep, base, shade)
        if s > 0.55:
            c = mix(c, stripe, 0.9)
        elif s > 0.25:
            c = mix(c, pale, 0.6)
        return c
    img = paint(name, sphere_surface(cx, cy, cz, R), colour)
    swatch(img, 0, 0, rgb(0x8A6A3A), rgb(0x5C4524), rgb(0xB08A4E))   # stem swatch (over the north view corner; outside the fruit)
    uvf = lambda face, f, t: face_uv(FACE_VIEW[face], f, t)
    el = [box_element(f, t, uv_fn=uvf) for f, t in boxes]
    # stem: a stalk rising from the top, bending over and curling
    stem_uv = solid_uv(0, 0)
    el.append(box_element([7.4, 13.2, 7.4], [8.6, 15.4, 8.6], uv_fn=stem_uv))
    el.append(box_element([7.5, 15.0, 7.5], [10.6, 16.0, 8.5], uv_fn=stem_uv))
    el.append(box_element([10.0, 15.2, 7.6], [12.2, 16.0, 8.4], uv_fn=stem_uv, rotation={'origin': [10.2, 15.6, 8], 'axis': 'z', 'angle': -22.5}))
    el.append(box_element([11.6, 14.0, 7.6], [12.4, 15.9, 8.4], uv_fn=stem_uv, rotation={'origin': [12.0, 15.2, 8], 'axis': 'z', 'angle': 22.5}))
    el.append(box_element([10.6, 13.6, 7.6], [12.0, 14.4, 8.4], uv_fn=stem_uv))
    os.makedirs(TEX, exist_ok=True)
    img.save(os.path.join(TEX, f'{name}.png'))
    write_model(name, el, gui_scale=0.62)
    return len(el)


def pika():
    """Pika Pika no Mi: a golden bell-shaped fruit, white light-swirls, a crown of sky-blue leaves and a curled stem."""
    name = 'pika_pika'
    cx, cy, cz = 8.0, 6.4, 8.0
    H = 6.2   # half height

    def prof(y):
        # bell: narrow at the top, widest near the bottom, flaring lip, flat-ish underside
        if y > H or y < -H:
            return None
        t = (y + H) / (2 * H)          # 0 bottom .. 1 top
        if t < 0.1:
            return 5.2 + 7.0 * t          # lip flare
        if t < 0.22:
            return 5.9 + (0.22 - t) * 2.0
        return 6.3 * math.sqrt(max(0.0, 1 - ((t - 0.22) / 0.78) ** 1.6)) * (0.97) + 0.15
    cells = voxelize(prof, cy, -H, H, cx, cz)
    boxes = merge(cells)
    base, deep, pale, stripe = rgb(0xF0BE2C), rgb(0xB88212), rgb(0xFFD85C), rgb(0xFFF8DC)
    field = spiral_field(fib_sphere(8, 1.9), turns=2.4, extent=0.62, width=0.16)

    def colour(p):
        d = norm((p[0] - cx, (p[1] - cy) * 1.1, p[2] - cz))
        s = field(d)
        shade = 0.8 + 0.2 * max(0.0, d[1] * 0.5 + d[0] * 0.35 + 0.5)
        c = mix(deep, base, shade)
        if s > 0.55:
            c = mix(c, stripe, 0.9)
        elif s > 0.25:
            c = mix(c, pale, 0.6)
        return c
    img = paint(name, sphere_surface(cx, cy, cz, 6.6, profile=prof), colour)
    swatch(img, 0, 0, rgb(0x5AA7E6), rgb(0x2F6FA8), rgb(0x9CD2FF))   # leaf blue
    swatch(img, 4, 0, rgb(0x3E7FC4), rgb(0x27578A), rgb(0x6FA9DE))   # stem blue
    uvf = lambda face, f, t: face_uv(FACE_VIEW[face], f, t)
    el = [box_element(f, t, uv_fn=uvf) for f, t in boxes]
    leaf, stem = solid_uv(0, 0), solid_uv(4, 0)
    # crown of six leaves draping down from the top
    for i in range(6):
        a = math.radians(i * 60 + 15)
        x, z = cx + 3.2 * math.cos(a), cz + 3.2 * math.sin(a)
        if abs(math.cos(a)) >= abs(math.sin(a)):
            rot = {'origin': [x, 13.0, z], 'axis': 'z', 'angle': 22.5 if math.cos(a) > 0 else -22.5}
        else:
            rot = {'origin': [x, 13.0, z], 'axis': 'x', 'angle': -22.5 if math.sin(a) > 0 else 22.5}
        el.append(box_element([x - 0.9, 10.2, z - 0.9], [x + 0.9, 13.2, z + 0.9], uv_fn=leaf, rotation=rot))
    el.append(box_element([6.2, 12.6, 6.2], [9.8, 13.6, 9.8], uv_fn=leaf))
    # stem with a curl
    el.append(box_element([7.5, 13.4, 7.5], [8.5, 16.6, 8.5], uv_fn=stem))
    el.append(box_element([7.6, 16.0, 7.6], [10.4, 16.8, 8.4], uv_fn=stem))
    el.append(box_element([9.8, 14.6, 7.6], [10.6, 16.6, 8.4], uv_fn=stem))
    el.append(box_element([8.8, 14.4, 7.6], [10.4, 15.2, 8.4], uv_fn=stem))
    img.save(os.path.join(TEX, f'{name}.png'))
    write_model(name, el, gui_scale=0.6)
    return len(el)


def seiryu():
    """Uo Uo no Mi, Model: Seiryu: an oblong azure fruit armoured in dragon scales, each with a dark oval and a tiny
    spiral; two horn-like stems (violet and pink) over a tuft of orange leaves."""
    name = 'uo_uo_seiryu'
    cx, cy, cz = 8.0, 7.2, 8.0
    H, Rw = 6.8, 5.6

    def prof(y):
        if abs(y) > H:
            return None
        t = y / H
        return Rw * math.sqrt(max(0.0, 1 - t * t)) * (1.0 - 0.12 * max(0.0, t))   # slightly pointed top (pinecone)
    cells = voxelize(prof, cy, -H, H, cx, cz)
    boxes = merge(cells)
    light, base, deep, oval, spiral = rgb(0x8CCBEA), rgb(0x3F93C6), rgb(0x1E5C8A), rgb(0x123E63), rgb(0xBFE6F7)

    def colour(p):
        d = norm((p[0] - cx, (p[1] - cy) * 0.82, p[2] - cz))
        # scales: a lattice of latitude bands, offset every other row
        lat = math.asin(max(-1.0, min(1.0, d[1])))
        lon = math.atan2(d[2], d[0])
        rows = 7.0
        band = (lat + math.pi / 2) / math.pi * rows
        row = int(band)
        cols = max(3, int(round(10 * math.cos(lat))))
        col_f = (lon / (2 * math.pi) + (0.5 if row % 2 else 0.0)) * cols
        fu = col_f - math.floor(col_f) - 0.5
        fv = band - row - 0.5
        rr = math.sqrt(fu * fu * 1.1 + fv * fv * 1.6)
        shade = 0.85 + 0.15 * max(0.0, d[1] * 0.4 + d[0] * 0.3 + 0.5)
        c = mix(deep, base, shade)
        if rr > 0.47:
            c = mix(c, light, 0.8)            # scale rim
        elif rr < 0.26:
            c = mix(c, oval, 0.85)            # dark oval
            ang = math.atan2(fv, fu)
            ph = (ang - rr * 14.0) % (2 * math.pi)
            if min(ph, 2 * math.pi - ph) < 0.55 and rr > 0.06:
                c = mix(c, spiral, 0.9)       # the tiny spiral inside
        elif rr < 0.33:
            c = mix(c, light, 0.35)
        return c
    img = paint(name, sphere_surface(cx, cy, cz, 7.0, profile=prof), colour)
    swatch(img, 0, 0, rgb(0xF08A2E), rgb(0xB95E16), rgb(0xFFB766))   # orange leaf
    swatch(img, 4, 0, rgb(0x8A4FC8), rgb(0x5B2E8C), rgb(0xB58AE6))   # violet horn
    swatch(img, 8, 0, rgb(0xE88ACB), rgb(0xB85C9E), rgb(0xFFC0E6))   # pink horn
    uvf = lambda face, f, t: face_uv(FACE_VIEW[face], f, t)
    el = [box_element(f, t, uv_fn=uvf) for f, t in boxes]
    leaf, violet, pink = solid_uv(0, 0), solid_uv(4, 0), solid_uv(8, 0)
    # tuft of orange leaves
    for i in range(5):
        a = math.radians(i * 72)
        x, z = cx + 1.6 * math.cos(a), cz + 1.6 * math.sin(a)
        el.append(box_element([x - 1.1, 13.4, z - 0.5], [x + 1.1, 14.6, z + 0.5], uv_fn=leaf,
                              rotation={'origin': [cx, 13.8, cz], 'axis': 'y', 'angle': 0}))
    el.append(box_element([6.4, 13.0, 6.4], [9.6, 14.2, 9.6], uv_fn=leaf))
    # two horn stems, leaning outward with a kink
    for sx, uvh in ((-1, violet), (1, pink)):
        x = cx + sx * 1.4
        el.append(box_element([x - 0.6, 14.0, 7.4], [x + 0.6, 17.2, 8.6], uv_fn=uvh,
                              rotation={'origin': [x, 14.0, 8], 'axis': 'z', 'angle': -22.5 * sx}))
        el.append(box_element([x + sx * 1.0 - 0.45, 16.6, 7.55], [x + sx * 1.0 + 0.45, 18.4, 8.45], uv_fn=uvh,
                              rotation={'origin': [x + sx * 1.0, 16.6, 8], 'axis': 'z', 'angle': -45 * sx}))
    img.save(os.path.join(TEX, f'{name}.png'))
    write_model(name, el, gui_scale=0.58)
    return len(el)


if __name__ == '__main__':
    for fn in (gomu, pika, seiryu):
        print(fn.__name__, fn(), 'elements')
