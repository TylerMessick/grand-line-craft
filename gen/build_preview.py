#!/usr/bin/env python3
"""Builds preview/index.html: a three.js page that shows the real fruit item models (same JSON and textures the
mod ships), the azure dragon model, the form overlays and a few animated moves. Run from the repo root."""
import base64
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), '..')
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'grandline')


def b64(path):
    with open(path, 'rb') as f:
        return 'data:image/png;base64,' + base64.b64encode(f.read()).decode()


models = {}
textures = {}
for name in ['gomu_gomu', 'pika_pika', 'uo_uo_seiryu']:
    with open(os.path.join(ASSETS, 'models', 'item', f'{name}.json')) as f:
        models[name] = json.load(f)
    textures[name] = b64(os.path.join(ASSETS, 'textures', 'item', f'{name}.png'))
textures['dragon'] = b64(os.path.join(ASSETS, 'textures', 'entity', 'seiryu_dragon.png'))
textures['forms'] = b64(os.path.join(ASSETS, 'textures', 'entity', 'forms.png'))

html = r'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Grand Line Craft Preview</title>
<style>
  :root { --bg: #0f1218; --panel: #171c26; --ink: #e8ecf2; --mute: #8a93a6; --accent: #ffcf4a; }
  * { box-sizing: border-box; }
  html, body { margin: 0; height: 100%; background: var(--bg); color: var(--ink); font: 14px/1.4 system-ui, -apple-system, Segoe UI, Roboto, sans-serif; }
  #ui { position: fixed; left: 16px; top: 16px; right: 16px; display: flex; gap: 8px; flex-wrap: wrap; z-index: 2; }
  button { background: var(--panel); color: var(--ink); border: 1px solid #2a3140; border-radius: 8px; padding: 8px 12px; cursor: pointer; font: inherit; }
  button.on { border-color: var(--accent); color: var(--accent); }
  #info { position: fixed; left: 16px; bottom: 16px; right: 16px; max-width: 560px; background: rgba(23,28,38,.92); border: 1px solid #2a3140; border-radius: 10px; padding: 12px 14px; z-index: 2; }
  #info h2 { margin: 0 0 4px; font-size: 16px; color: var(--accent); }
  #info p { margin: 0; color: var(--mute); }
  canvas { display: block; }
</style>
</head>
<body>
<div id="ui"></div>
<div id="info"><h2 id="title"></h2><p id="desc"></p></div>
<script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
<script>
const MODELS = __MODELS__;
__RIG__
const TEX = __TEX__;

const scenes = [
  { id: 'fruits', label: 'Devil fruits', title: 'The three fruits', desc: 'The exact item models the mod ships, at 1/32-block resolution with the swirl and scale art painted in 3D. Gomu Gomu: violet with pale swirls and a curling stem. Pika Pika: a golden bell under sky-blue leaves. Uo Uo: Seiryu: azure dragon scales, two horn stems, orange leaf tuft.' },
  { id: 'dragon', label: 'Azure dragon', title: 'Uo Uo no Mi, Model: Seiryu — full dragon', desc: 'The model that replaces the player in dragon form, built and textured by gen/gen_dragon.py: a fanged, antlered head with whiskers and a dark mane, a long slim body of azure scales over cream belly scutes, a rippling dorsal fin, four small clawed arms and a flame-tuft tail. Drag to orbit.' },
  { id: 'gear3', label: 'Gear Third', title: 'Gear Third', desc: 'Bone balloon: one giant fist, a swollen off hand.' },
  { id: 'gear4', label: 'Boundman', title: 'Gear Fourth: Boundman', desc: 'Inflated haki shell with flame markings over the torso and arms, haki fists, a ring of steam.' },
  { id: 'gear5', label: 'Gear Fifth', title: 'Gear Fifth', desc: 'White glow over the whole body and a wreath of clouds that drifts around the neck.' },
  { id: 'hybrid', label: 'Hybrid dragon', title: 'Seiryu hybrid', desc: 'Horns, shoulder scale plates, a mane of spines down the back and a swaying tail.' },
  { id: 'gear2', label: 'Gear Second', title: 'Gear Second', desc: 'A red flush over the skin and steam rising off the body; the jet moves dash before they hit.' },
  { id: 'pistol', label: 'Move: Pistol', title: 'Gomu Gomu no Pistol', desc: 'In game the player\'s own arm (their skin) shoots out from the shoulder to the target and snaps back; the fist lands with impact lines.' },
  { id: 'gatling', label: 'Move: Gatling', title: 'Gomu Gomu no Gatling', desc: 'Both arms fire alternately, a dozen stretched punches in a loose cone, each one a skinned arm.' },
  { id: 'redhawk', label: 'Move: Red Hawk', title: 'Gomu Gomu no Red Hawk', desc: 'A haki-black arm wreathed in flame, a fire line to the target and a burning explosion.' },
  { id: 'kingkong', label: 'Move: King Kong Gun', title: 'Gomu Gomu no King Kong Gun', desc: 'The arm compresses back into a growing black sphere, then a colossal haki fist flies out.' },
  { id: 'yasakani', label: 'Move: Yasakani', title: 'Yasakani no Magatama', desc: 'A torrent of light bullets from both hands, each one bursting on impact.' },
  { id: 'yata', label: 'Move: Yata no Kagami', title: 'Yata no Kagami', desc: 'A beam of light from where the user stood to where the light lands; the user reappears there.' },
  { id: 'clones', label: 'Move: Light clones', title: 'Yasakani no Magatama: Clones', desc: 'Two glowing doubles hover beside the user and fire light at anything hostile.' },
  { id: 'tatsumaki', label: 'Move: Tatsumaki', title: 'Tatsumaki (Dragon Twister)', desc: 'Stacked wind rings spin up around the dragon and drag everything into the air.' },
  { id: 'ragnaraku', label: 'Move: Ragnaraku', title: 'Ragnaraku', desc: 'The dragon drops from the sky as a lightning column and slams into the ground with rings of fire and lightning.' },
  { id: 'laser', label: 'Move: Laser', title: 'Pika Pika: Laser Beam', desc: 'An instant thick beam of light with a white core and a burst at the far end.' },
  { id: 'boro', label: 'Move: Boro Breath', title: 'Boro Breath', desc: 'A fire beam from the dragon\'s mouth, wrapped in a spiralling flame ribbon, exploding where it lands.' },
  { id: 'kong', label: 'Move: Kong Gun', title: 'Gomu Gomu no Kong Gun', desc: 'The haki fist compresses back, then fires forward trailing its arm, with red haki lightning.' },
];

const renderer = new THREE.WebGLRenderer({ antialias: true });
renderer.setPixelRatio(Math.min(2, window.devicePixelRatio));
renderer.setSize(innerWidth, innerHeight);
document.body.appendChild(renderer.domElement);
const scene = new THREE.Scene();
scene.background = new THREE.Color(0x0f1218);
const camera = new THREE.PerspectiveCamera(45, innerWidth / innerHeight, 0.05, 200);
scene.add(new THREE.HemisphereLight(0xdfe8ff, 0x3a2e22, 0.9));
const sun = new THREE.DirectionalLight(0xffffff, 0.9); sun.position.set(4, 8, 5); scene.add(sun);
const ground = new THREE.Mesh(new THREE.PlaneGeometry(60, 60), new THREE.MeshLambertMaterial({ color: 0x2a3140 }));
ground.rotation.x = -Math.PI / 2; ground.position.y = 0; scene.add(ground);
const grid = new THREE.GridHelper(60, 60, 0x3a4356, 0x1e2430); grid.position.y = 0.001; scene.add(grid);
let root = new THREE.Group(); scene.add(root);
let orbit = 0, dragging = false, lastX = 0, camDist = 6, camH = 2.2, target = new THREE.Vector3(0, 1, 0);
renderer.domElement.addEventListener('pointerdown', e => { dragging = true; lastX = e.clientX; });
addEventListener('pointerup', () => dragging = false);
addEventListener('pointermove', e => { if (dragging) { orbit += (e.clientX - lastX) * 0.01; lastX = e.clientX; } });
addEventListener('wheel', e => { camDist = Math.max(2, Math.min(40, camDist + e.deltaY * 0.01)); });
addEventListener('resize', () => { camera.aspect = innerWidth / innerHeight; camera.updateProjectionMatrix(); renderer.setSize(innerWidth, innerHeight); });

function texture(src) {
  const t = new THREE.TextureLoader().load(src);
  t.magFilter = THREE.NearestFilter; t.minFilter = THREE.NearestFilter;
  return t;
}
const textures = {};
for (const k in TEX) textures[k] = texture(TEX[k]);

// ---- Minecraft element model → meshes (same JSON the game reads)
function elementMesh(el, tex) {
  const f = el.from, t = el.to;
  const size = [(t[0] - f[0]) / 16, (t[1] - f[1]) / 16, (t[2] - f[2]) / 16];
  const geo = new THREE.BoxGeometry(Math.max(size[0], 0.001), Math.max(size[1], 0.001), Math.max(size[2], 0.001));
  // three face order: +x east, -x west, +y up, -y down, +z south, -z north
  const order = ['east', 'west', 'up', 'down', 'south', 'north'];
  const uv = geo.attributes.uv;
  order.forEach((face, i) => {
    const fc = el.faces[face];
    const u = fc ? fc.uv : [0, 0, 16, 16];
    const u0 = u[0] / 16, v0 = 1 - u[1] / 16, u1 = u[2] / 16, v1 = 1 - u[3] / 16;
    const base = i * 4;
    uv.setXY(base, u0, v0); uv.setXY(base + 1, u1, v0); uv.setXY(base + 2, u0, v1); uv.setXY(base + 3, u1, v1);
  });
  uv.needsUpdate = true;
  const mesh = new THREE.Mesh(geo, new THREE.MeshLambertMaterial({ map: tex }));
  mesh.position.set((f[0] + t[0]) / 32, (f[1] + t[1]) / 32, (f[2] + t[2]) / 32);
  if (el.rotation) {
    const g = new THREE.Group();
    const o = el.rotation.origin.map(v => v / 16);
    g.position.set(o[0], o[1], o[2]);
    mesh.position.sub(new THREE.Vector3(o[0], o[1], o[2]));
    g.add(mesh);
    const a = THREE.MathUtils.degToRad(el.rotation.angle);
    if (el.rotation.axis === 'x') g.rotation.x = a; else if (el.rotation.axis === 'y') g.rotation.y = a; else g.rotation.z = a;
    return g;
  }
  return mesh;
}
function itemModel(name) {
  const g = new THREE.Group();
  for (const el of MODELS[name].elements) g.add(elementMesh(el, textures[name]));
  g.position.set(-0.5, 0, -0.5); // center the 16-unit cube
  const outer = new THREE.Group(); outer.add(g);
  return outer;
}

// ---- cube helpers (Minecraft-style rigs, 1/16 units)
function cube(parent, tex, x, y, z, w, h, d, uvw, uvh, u, v, color) {
  const geo = new THREE.BoxGeometry(w / 16, h / 16, d / 16);
  const mat = tex ? new THREE.MeshLambertMaterial({ map: tex, color: color || 0xffffff }) : new THREE.MeshLambertMaterial({ color: color || 0xffffff });
  if (tex) {
    // uniform region sampling: every face takes the same small swatch at (u,v)
    const uv = geo.attributes.uv;
    for (let i = 0; i < 6; i++) {
      const u0 = u / uvw, v0 = 1 - v / uvh, u1 = (u + 8) / uvw, v1 = 1 - (v + 8) / uvh;
      const b = i * 4; uv.setXY(b, u0, v0); uv.setXY(b + 1, u1, v0); uv.setXY(b + 2, u0, v1); uv.setXY(b + 3, u1, v1);
    }
    uv.needsUpdate = true;
  }
  const m = new THREE.Mesh(geo, mat);
  m.position.set((x + w / 2) / 16, (y + h / 2) / 16, (z + d / 2) / 16);
  parent.add(m);
  return m;
}
function pivot(parent, x, y, z) { const g = new THREE.Group(); g.position.set(x / 16, y / 16, z / 16); parent.add(g); return g; }

// ---- the player figure (a plain blocky figure; the mod draws these on the real player model)
function figure(opts) {
  const T = textures.forms;
  const g = new THREE.Group();
  const skin = 0xd9a77c, shirt = 0x3b6ea5, pants = 0x2d3a63;
  const body = pivot(g, 0, 12, 0);
  cube(body, null, -4, 0, -2, 8, 12, 4, 0, 0, 0, 0, shirt);
  const head = pivot(g, 0, 24, 0); cube(head, null, -4, 0, -4, 8, 8, 8, 0, 0, 0, 0, skin);
  const armR = pivot(g, -5, 22, 0); cube(armR, null, -3, -10, -2, 4, 12, 4, 0, 0, 0, 0, skin);
  const armL = pivot(g, 5, 22, 0); cube(armL, null, -1, -10, -2, 4, 12, 4, 0, 0, 0, 0, skin);
  const legR = pivot(g, -2, 12, 0); cube(legR, null, -2, -12, -2, 4, 12, 4, 0, 0, 0, 0, pants);
  const legL = pivot(g, 2, 12, 0); cube(legL, null, -2, -12, -2, 4, 12, 4, 0, 0, 0, 0, pants);
  armR.rotation.x = -0.2; armL.rotation.x = 0.2;
  g.userData = { body, head, armR, armL, legR, legL };
  const o = opts || {};
  if (o.fists) {
    const s = o.fists;
    for (const [arm, sc, off] of [[armR, s, -1], [armL, s * 0.6, 1]]) {
      const f = pivot(arm, off * 1, -11, 0); f.scale.setScalar(sc);
      cube(f, T, -5, -5, -5, 10, 10, 10, 128, 128, 0, 0, o.fistColor || 0xffffff);
      cube(f, T, -5.5, -1, -5.5, 11, 3, 2, 128, 128, 0, 0, o.fistColor || 0xffffff);
    }
  }
  if (o.shell) {
    const c = o.shellColor, a = o.shellAlpha;
    const mat = (m) => { m.material.transparent = a < 1; m.material.opacity = a; return m; };
    mat(cube(body, T, -6, -1, -4, 12, 14, 8, 128, 128, 0, 32, c));
    mat(cube(armR, T, -3, -10.5, -3, 6, 13, 6, 128, 128, 0, 32, c)).position.x -= 1 / 16;
    mat(cube(armL, T, -3, -10.5, -3, 6, 13, 6, 128, 128, 0, 32, c)).position.x += 1 / 16;
    if (o.legs) { mat(cube(legR, T, -2.5, -12.5, -2.5, 5, 12, 5, 128, 128, 0, 32, c)); mat(cube(legL, T, -2.5, -12.5, -2.5, 5, 12, 5, 128, 128, 0, 32, c)); }
  }
  if (o.clouds) {
    const ring = pivot(body, 0, 10, 0); g.userData.cloudRing = ring;
    for (let i = 0; i < 10; i++) { const a = i / 10 * Math.PI * 2; const c = cube(ring, T, -2, -2, -2, 4, 4, 4, 128, 128, 64, 0, 0xffffff); c.position.set(Math.cos(a) * 0.42, 0, Math.sin(a) * 0.42); c.userData.i = i; }
  }
  if (o.steam) {
    const ring = pivot(body, 0, 2, 0); g.userData.steam = ring;
    for (let i = 0; i < 6; i++) { const c = cube(ring, null, -2, -2, -2, 4, 4, 4, 0, 0, 0, 0, 0xffffff); c.material.transparent = true; c.material.opacity = 0.5; c.userData.i = i; }
  }
  if (o.horns) {
    for (const s of [-1, 1]) {
      const h = pivot(head, s * 3.2, 7.5, 1); h.rotation.x = THREE.MathUtils.degToRad(-35); h.rotation.z = THREE.MathUtils.degToRad(s * 18);
      cube(h, T, -1, 0, -1, 2, 7, 2, 128, 128, 0, 64); cube(h, T, -0.5, 7, -0.5, 1, 4, 1, 128, 128, 0, 64);
    }
    const t1 = pivot(body, 0, 0.5, 2); g.userData.tail = [t1];
    cube(t1, T, -2, -2, 0, 4, 4, 7, 128, 128, 64, 32, 0x3fa7d6);
    const t2 = pivot(t1, 0, 0, 7); g.userData.tail.push(t2); cube(t2, T, -1.5, -1.5, 0, 3, 3, 7, 128, 128, 64, 32, 0x3fa7d6);
    const t3 = pivot(t2, 0, 0, 7); g.userData.tail.push(t3); cube(t3, T, -1, -1, 0, 2, 2, 7, 128, 128, 64, 32, 0x3fa7d6); cube(t3, T, -0.5, -3, 4, 1, 6, 4, 128, 128, 96, 32);
    cube(armR, T, -3.5, 0.5, -2.5, 5, 3, 5, 128, 128, 64, 32, 0x3fa7d6); cube(armL, T, -1.5, 0.5, -2.5, 5, 3, 5, 128, 128, 64, 32, 0x3fa7d6);
    for (let i = 0; i < 4; i++) cube(body, T, -1, 2 + i * 2.7, 2, 2, 1, 3, 128, 128, 0, 96);
  }
  return g;
}

// ---- the dragon rig: built from the generated DRAGON_RIG exactly like the game's LayerDefinition
function rigBox(b, tex) {
  const [w, h, d] = b.s; const [u, v] = b.uv; const inf = b.inf || 0;
  const geo = new THREE.BoxGeometry(w / 16 + inf / 8, h / 16 + inf / 8, d / 16 + inf / 8);
  const W = Math.ceil(w), H = Math.ceil(h), D = Math.ceil(d), S = 256;
  // three face order: +x, -x, +y, -y, +z, -z  →  Minecraft regions: east, west, bottom(+y, model down), top(-y), south, north
  const regions = [[u + D + W, v + D, D, H], [u, v + D, D, H], [u + D + W, v, W, D], [u + D, v, W, D], [u + 2 * D + W, v + D, W, H], [u + D, v + D, W, H]];
  const uv = geo.attributes.uv;
  regions.forEach(([x, y, rw, rh], i) => {
    const u0 = x / S, u1 = (x + rw) / S, v0 = 1 - y / S, v1 = 1 - (y + rh) / S;
    const b0 = i * 4; uv.setXY(b0, u0, v0); uv.setXY(b0 + 1, u1, v0); uv.setXY(b0 + 2, u0, v1); uv.setXY(b0 + 3, u1, v1);
  });
  uv.needsUpdate = true;
  const m = new THREE.Mesh(geo, new THREE.MeshLambertMaterial({ map: tex, alphaTest: 0.5 }));
  m.position.set((b.o[0] + w / 2) / 16, (b.o[1] + h / 2) / 16, (b.o[2] + d / 2) / 16);
  return m;
}
function rigNode(n, tex, parts) {
  const g = new THREE.Group();
  g.position.set(n.pivot[0] / 16, n.pivot[1] / 16, n.pivot[2] / 16);
  g.rotation.order = 'ZYX';
  g.rotation.set(n.rot[0], n.rot[1], n.rot[2]);
  g.userData.base = [n.rot[0], n.rot[1], n.rot[2]];
  for (const b of n.boxes) g.add(rigBox(b, tex));
  for (const c of n.children) g.add(rigNode(c, tex, parts));
  parts[n.name] = g;
  return g;
}
function dragon() {
  const parts = {};
  const inner = new THREE.Group();
  for (const n of DRAGON_RIG) inner.add(rigNode(n, textures.dragon, parts));
  inner.scale.set(-1, -1, 1);          // Minecraft model space: y down, z forward
  inner.position.set(0, 1.5, 0);
  const g = new THREE.Group(); g.add(inner);
  const names = (p) => Object.keys(parts).filter(k => k.startsWith(p)).sort().map(k => parts[k]);
  g.userData = { parts, body: names('body'), tails: names('tail').filter(t => t !== parts.tail_tuft), neck: names('neck'), head: parts.head, jaw: parts.jaw,
    legs: [parts.leg_fl, parts.leg_fr, parts.leg_bl, parts.leg_br], lows: [parts.leg_fl_low, parts.leg_fr_low, parts.leg_bl_low, parts.leg_br_low], tuft: parts.tail_tuft };
  g.userData.reset = () => { for (const k in parts) { const b = parts[k].userData.base; parts[k].rotation.set(b[0], b[1], b[2]); } };
  return g;
}
function animateDragon(d, t, flying, speed) {
  const u = d.userData; u.reset();
  const w = t * 2.2, amp = 0.09 + Math.min(0.14, speed * 0.5);
  u.body.forEach((b, i) => { b.rotation.x += Math.cos(w * 0.9 - i * 0.65) * amp * 0.45; b.rotation.y += Math.sin(w - i * 0.65) * amp; });
  u.tails.forEach((b, i) => { b.rotation.x += Math.cos(w * 0.9 - (5 + i) * 0.65) * amp * 0.55; b.rotation.y += Math.sin(w - (5 + i) * 0.65) * (amp + 0.04); });
  u.tuft.rotation.x += Math.sin(t * 5) * 0.3; u.tuft.rotation.y += Math.cos(t * 4) * 0.25;
  const bob = Math.sin(t * 1.4) * 0.05;
  u.neck[0].rotation.x += -0.12 + bob; u.neck[0].rotation.y += Math.sin(w + 0.7) * amp * 0.6;
  u.neck[1].rotation.x += -0.08 + bob; u.neck[1].rotation.y += Math.sin(w + 1.3) * amp * 0.4;
  u.neck[2].rotation.x += 0.15; u.head.rotation.x += 0.05;
  u.jaw.rotation.x += 0.12 + 0.1 * Math.sin(t * 1.8) + (flying ? 0.08 : 0);
  const stroke = flying ? Math.sin(t * 3.6) * 0.55 : Math.sin(t * 10) * Math.min(1, speed * 3) * 0.8;
  u.legs[0].rotation.x += -0.4 + stroke; u.legs[1].rotation.x += -0.4 - stroke; u.legs[2].rotation.x += -0.3 - stroke; u.legs[3].rotation.x += -0.3 + stroke;
  const curl = 0.6 + 0.3 * Math.sin(t * 3.6 + 1); u.lows.forEach(l => l.rotation.x += curl);
  ['leg_fl_foot', 'leg_fr_foot', 'leg_bl_foot', 'leg_br_foot'].forEach(k => u.parts[k].rotation.x += -0.5);
}

// ---- move primitives (simplified mirrors of FxClient)
function tube(a, b, radius, color, opacity, emissive) {
  const d = new THREE.Vector3().subVectors(b, a); const len = d.length();
  const geo = new THREE.CylinderGeometry(radius, radius, len, 6, 1);
  const mat = emissive ? new THREE.MeshBasicMaterial({ color, transparent: opacity < 1, opacity, blending: THREE.AdditiveBlending, depthWrite: false })
                       : new THREE.MeshLambertMaterial({ color, transparent: opacity < 1, opacity });
  const m = new THREE.Mesh(geo, mat);
  m.position.copy(a).add(d.clone().multiplyScalar(0.5));
  m.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), d.clone().normalize());
  return m;
}
function voxelSphere(c, r, color, opacity, emissive) {
  const g = new THREE.Group(); const step = Math.max(0.25, r / 2.2); const m = Math.ceil(r / step);
  for (let x = -m; x <= m; x++) for (let y = -m; y <= m; y++) for (let z = -m; z <= m; z++) {
    const dx = x * step, dy = y * step, dz = z * step; if (Math.hypot(dx, dy, dz) > r) continue;
    const geo = new THREE.BoxGeometry(step * 1.05, step * 1.05, step * 1.05);
    const mat = emissive ? new THREE.MeshBasicMaterial({ color, transparent: true, opacity, blending: THREE.AdditiveBlending, depthWrite: false }) : new THREE.MeshLambertMaterial({ color, transparent: opacity < 1, opacity });
    const b = new THREE.Mesh(geo, mat); b.position.set(c.x + dx, c.y + dy, c.z + dz); g.add(b);
  }
  return g;
}

let anim = null, t0 = 0;
function clear() { scene.remove(root); root = new THREE.Group(); scene.add(root); anim = null; t0 = performance.now(); }
function build(id) {
  clear();
  camDist = 6; camH = 2.2; target.set(0, 1, 0);
  if (id === 'fruits') {
    const names = ['gomu_gomu', 'pika_pika', 'uo_uo_seiryu'];
    const items = names.map((n, i) => { const m = itemModel(n); m.position.set((i - 1) * 3.2, 1.3, 0); m.scale.setScalar(2.4); root.add(m); const ped = new THREE.Mesh(new THREE.CylinderGeometry(1.1, 1.2, 0.3, 12), new THREE.MeshLambertMaterial({ color: 0x3a4356 })); ped.position.set((i - 1) * 3.2, 0.15, 0); root.add(ped); return m; });
    anim = (t) => items.forEach((m, i) => { m.rotation.y = t * 0.6 + i; m.position.y = 1.4 + Math.sin(t * 1.5 + i) * 0.08; });
    camDist = 7.5; camH = 2.0; target.set(0, 2.2, 0);
  } else if (id === 'dragon') {
    const d = dragon(); d.scale.setScalar(3.4); d.position.y = 2; root.add(d);
    camDist = 26; camH = 8; target.set(0, 4, 0);
    anim = (t) => { animateDragon(d, t, true, 0.1); d.position.y = 2 + Math.sin(t) * 0.4; };
  } else if (id === 'gear3' || id === 'gear4' || id === 'gear5' || id === 'hybrid') {
    const opts = { gear3: { fists: 1.0 }, gear4: { fists: 0.7, fistColor: 0x1c1c22, shell: true, shellColor: 0x1c1c22, shellAlpha: 1, steam: true },
      gear5: { shell: true, shellColor: 0xffffff, shellAlpha: 0.35, legs: true, clouds: true }, hybrid: { horns: true } }[id];
    const f = figure(opts); f.position.y = 0; root.add(f);
    camDist = 5; camH = 1.9; target.set(0, 1.1, 0);
    anim = (t) => { const u = f.userData; u.armR.rotation.x = -0.2 + Math.sin(t * 2) * 0.15; u.armL.rotation.x = 0.2 - Math.sin(t * 2) * 0.15;
      if (u.cloudRing) u.cloudRing.children.forEach(c => { const i = c.userData.i, a = i / 10 * Math.PI * 2 + t; const s = 0.9 + 0.3 * Math.sin(t * 5 + i); c.position.set(Math.cos(a) * 0.42, 0.03 * Math.sin(t * 4 + i * 2), Math.sin(a) * 0.42); c.scale.setScalar(s); });
      if (u.steam) u.steam.children.forEach(c => { const i = c.userData.i, k = (t * 0.8 + i * 0.37) % 1; const a = i / 6 * Math.PI * 2 + t * 1.6; c.position.set(Math.cos(a) * 0.45, 0.1 + k * 0.5, Math.sin(a) * 0.45); c.scale.setScalar(0.8 + k); c.material.opacity = 0.5 * (1 - k); });
      if (u.tail) u.tail.forEach((s, i) => { s.rotation.x = -0.4 + 0.15 * Math.sin(t * 2 + i); s.rotation.y = 0.25 * Math.sin(t * 2.6 + i); }); };
  } else if (id === 'pistol' || id === 'kong') {
    const haki = id === 'kong';
    const f = figure(haki ? { fists: 0.7, fistColor: 0x1c1c22, shell: true, shellColor: 0x1c1c22, shellAlpha: 1, steam: true } : {}); root.add(f);
    const dummy = new THREE.Mesh(new THREE.BoxGeometry(0.6, 1.8, 0.6), new THREE.MeshLambertMaterial({ color: 0x5a6b7c })); dummy.position.set(0, 0.9, -7); root.add(dummy);
    camDist = 9; camH = 3; target.set(0, 1, -3);
    let arm = null, fist = null, bolts = [];
    anim = (t) => { const u = f.userData; u.armR.rotation.x = -Math.PI / 2;
      const cycle = (t % 2.2) / 2.2; let ext;
      if (haki) ext = cycle < 0.3 ? -0.15 * (cycle / 0.3) : cycle < 0.5 ? -0.15 + 1.15 * ((cycle - 0.3) / 0.2) : Math.max(0, 1 - (cycle - 0.5) / 0.4);
      else ext = cycle < 0.25 ? cycle / 0.25 : Math.max(0, 1 - (cycle - 0.25) / 0.5);
      if (arm) { root.remove(arm); root.remove(fist); bolts.forEach(b => root.remove(b)); bolts = []; }
      const a = new THREE.Vector3(-0.35, 1.4, -0.3), b = new THREE.Vector3(-0.35, 1.3, -0.3 - 6.2 * Math.max(0.05, ext));
      const col = haki ? 0x1c1c22 : 0xd9a77c; arm = skinArm(a, b, haki ? 0.2 : 0.125, col); root.add(arm);
      fist = fistCube(b, haki ? 0.32 : 0.17, col); root.add(fist);
      if (haki && ext > 0.2) for (let i = 0; i < 3; i++) { const p = a.clone().lerp(b, Math.random()); const q = p.clone().add(new THREE.Vector3(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5)); const l = tube(p, q, 0.02, 0xff2a3a, 1, true); root.add(l); bolts.push(l); }
      if (u.steam) u.steam.children.forEach(c => { const i = c.userData.i, k = (t * 0.8 + i * 0.37) % 1; const an = i / 6 * Math.PI * 2 + t * 1.6; c.position.set(Math.cos(an) * 0.45, 0.1 + k * 0.5, Math.sin(an) * 0.45); c.scale.setScalar(0.8 + k); c.material.opacity = 0.5 * (1 - k); }); };
  } else if (id === 'laser' || id === 'boro') {
    const fire = id === 'boro';
    let src;
    let dr = null; if (fire) { dr = dragon(); dr.scale.setScalar(2); dr.position.set(0, 1.0, 3); root.add(dr); src = new THREE.Vector3(0, 2.6, 3 - 2 * 3.1); }
    else { const f = figure({ shell: true, shellColor: 0xffe24a, shellAlpha: 0.4, legs: true }); root.add(f); f.userData.armR.rotation.x = -Math.PI / 2; src = new THREE.Vector3(-0.35, 1.4, -0.4); }
    const end = new THREE.Vector3(0, 1.2, -14);
    const wall = new THREE.Mesh(new THREE.BoxGeometry(6, 4, 0.5), new THREE.MeshLambertMaterial({ color: 0x4a5566 })); wall.position.set(0, 2, -14.3); root.add(wall);
    camDist = 14; camH = 4; target.set(0, 1.5, -6);
    let parts = [];
    anim = (t) => { parts.forEach(p => root.remove(p)); parts = []; if (dr) animateDragon(dr, t, true, 0.05); const cycle = (t % 2.5) / 2.5; if (cycle > 0.6) return; const k = Math.min(1, cycle / 0.08);
      const b = src.clone().lerp(end, k); const col = fire ? 0xff5a1f : 0xffe24a, core = fire ? 0xffd36a : 0xffffff, r = fire ? 0.7 : 0.3;
      parts.push(tube(src, b, r * 1.6, col, 0.35, true), tube(src, b, r * 0.9, core, 0.7, true), tube(src, b, r * 0.35, 0xffffff, 1, true));
      if (fire) for (let i = 0; i < 8; i++) { const p = src.clone().lerp(b, i / 8), q = src.clone().lerp(b, (i + 1) / 8); p.y += Math.sin(i * 0.9 + t * 8) * r * 0.8; q.y += Math.sin((i + 1) * 0.9 + t * 8) * r * 0.8; parts.push(tube(p, q, r * 0.3, 0xff3a00, 0.8, true)); }
      if (k >= 1) { const burst = voxelSphere(end, (fire ? 3.5 : 2.0) * (0.6 + 0.4 * Math.min(1, (cycle - 0.08) / 0.1)), core, 0.5, true); parts.push(burst); }
      parts.forEach(p => root.add(p)); };
  } else if (id === 'gear2') {
    const f = figure({ shell: true, shellColor: 0xff4a5a, shellAlpha: 0.3, legs: true, steam: true }); root.add(f);
    camDist = 5; camH = 1.9; target.set(0, 1.1, 0);
    anim = (t) => { const u = f.userData; u.armR.rotation.x = -0.3 + Math.sin(t * 6) * 0.2; u.armL.rotation.x = 0.3 - Math.sin(t * 6) * 0.2;
      if (u.steam) u.steam.children.forEach(c => { const i = c.userData.i, k = (t * 1.2 + i * 0.37) % 1; const an = i / 6 * Math.PI * 2 + t * 2; c.position.set(Math.cos(an) * 0.4, 0.1 + k * 0.7, Math.sin(an) * 0.4); c.scale.setScalar(0.7 + k); c.material.opacity = 0.6 * (1 - k); }); };
  } else if (id === 'gatling' || id === 'redhawk') {
    const red = id === 'redhawk';
    const f = figure(red ? { shell: true, shellColor: 0xff4a5a, shellAlpha: 0.3, legs: true } : {}); root.add(f);
    const dummy = new THREE.Mesh(new THREE.BoxGeometry(0.6, 1.8, 0.6), new THREE.MeshLambertMaterial({ color: 0x5a6b7c })); dummy.position.set(0, 0.9, -7); root.add(dummy);
    camDist = 9; camH = 3; target.set(0, 1, -3);
    let parts = [];
    anim = (t) => { parts.forEach(p => root.remove(p)); parts = []; const u = f.userData; u.armR.rotation.x = -Math.PI / 2; u.armL.rotation.x = -Math.PI / 2;
      if (!red) {
        for (let i = 0; i < 2; i++) {
          const ph = ((t * 6 + i * 0.5) % 1);
          const ext = ph < 0.4 ? ph / 0.4 : Math.max(0, 1 - (ph - 0.4) / 0.4);
          const sx = i === 0 ? -0.35 : 0.35;
          const a = new THREE.Vector3(sx, 1.4, -0.3), b = new THREE.Vector3(sx * 2.2 + Math.sin(t * 7 + i) * 0.6, 1.3 + Math.cos(t * 5 + i) * 0.5, -0.3 - 6.0 * Math.max(0.05, ext));
          parts.push(skinArm(a, b, 0.125, i === 0 ? 0xd9a77c : 0xd9a77c), fistCube(b, 0.17, 0xd9a77c));
        }
      } else {
        const cycle = (t % 2.4) / 2.4; const ext = cycle < 0.15 ? cycle / 0.15 : cycle < 0.6 ? 1 : Math.max(0, 1 - (cycle - 0.6) / 0.3);
        const a = new THREE.Vector3(-0.35, 1.4, -0.3), b = new THREE.Vector3(-0.35, 1.2, -0.3 - 6.3 * Math.max(0.05, ext));
        parts.push(skinArm(a, b, 0.2, 0x2a1a1a), fistCube(b, 0.3, 0x1c1c22), tube(a, b, 0.32, 0xff5a1f, 0.35, true), tube(a, b, 0.12, 0xffd36a, 0.6, true));
        if (cycle > 0.15 && cycle < 0.7) { const k = Math.min(1, (cycle - 0.15) / 0.12); parts.push(voxelSphere(new THREE.Vector3(-0.35, 1.0, -6.8), 2.2 * k, 0xff8a20, 0.45, true)); }
      }
      parts.forEach(p => root.add(p)); };
  } else if (id === 'kingkong') {
    const f = figure({ fists: 0.7, fistColor: 0x1c1c22, shell: true, shellColor: 0x1c1c22, shellAlpha: 1, steam: true }); root.add(f);
    const wall = new THREE.Mesh(new THREE.BoxGeometry(8, 5, 0.6), new THREE.MeshLambertMaterial({ color: 0x4a5566 })); wall.position.set(0, 2.5, -16); root.add(wall);
    camDist = 16; camH = 5; target.set(0, 2, -6);
    let parts = [];
    anim = (t) => { parts.forEach(p => root.remove(p)); parts = []; const u = f.userData; u.armR.rotation.x = -Math.PI / 2;
      const cycle = (t % 3.2) / 3.2;
      if (cycle < 0.45) { const k = cycle / 0.45; const back = new THREE.Vector3(-0.35, 1.3, 1.2 + k * 0.6); parts.push(skinArm(new THREE.Vector3(-0.35, 1.4, -0.1), back, 0.2 + k * 0.2, 0x1c1c22), voxelSphere(back, 0.6 + k * 1.4, 0x1c1c22, 0.95, false)); }
      else { const k = Math.min(1, (cycle - 0.45) / 0.3); const fistP = new THREE.Vector3(-0.35, 1.4, -0.5 - 14 * k); parts.push(skinArm(new THREE.Vector3(-0.35, 1.4, -0.1), fistP, 0.5, 0x1c1c22), fistCube(fistP, 1.5, 0x1c1c22)); if (k >= 1) { parts.push(voxelSphere(new THREE.Vector3(0, 2.2, -15.2), 3.5, 0x8a1f2a, 0.5, true)); } }
      if (u.steam) u.steam.children.forEach(c => { const i = c.userData.i, k = (t * 0.8 + i * 0.37) % 1; const an = i / 6 * Math.PI * 2 + t * 1.6; c.position.set(Math.cos(an) * 0.45, 0.1 + k * 0.5, Math.sin(an) * 0.45); c.scale.setScalar(0.8 + k); c.material.opacity = 0.5 * (1 - k); });
      parts.forEach(p => root.add(p)); };
  } else if (id === 'yasakani' || id === 'clones') {
    const clones = id === 'clones';
    const f = figure({ shell: true, shellColor: 0xffe24a, shellAlpha: 0.4, legs: true }); root.add(f);
    const dummies = [-3, 0, 3].map(x => { const d = new THREE.Mesh(new THREE.BoxGeometry(0.6, 1.8, 0.6), new THREE.MeshLambertMaterial({ color: 0x5a6b7c })); d.position.set(x, 0.9, -10); root.add(d); return d; });
    let cl = [];
    if (clones) cl = [-2.2, 2.2].map(x => { const g = figure({ shell: true, shellColor: 0xffe24a, shellAlpha: 0.75, legs: true }); g.position.set(x, 0.3, 0.4); g.traverse(o => { if (o.material && !o.material.transparent) { o.material = new THREE.MeshBasicMaterial({ color: 0xffe9a0, transparent: true, opacity: 0.6, blending: THREE.AdditiveBlending, depthWrite: false }); } }); root.add(g); return g; });
    camDist = 12; camH = 4; target.set(0, 1.5, -5);
    let parts = [];
    anim = (t) => { parts.forEach(p => root.remove(p)); parts = []; const u = f.userData; u.armR.rotation.x = u.armL.rotation.x = -Math.PI / 2;
      const srcs = clones ? cl.map(g => new THREE.Vector3(g.position.x, 1.3 + Math.sin(t * 2) * 0.1, 0.4)) : [new THREE.Vector3(-0.35, 1.4, -0.3), new THREE.Vector3(0.35, 1.4, -0.3)];
      cl.forEach(g => { g.position.y = 0.3 + Math.sin(t * 2) * 0.12; });
      for (let i = 0; i < 24; i++) { const k = ((t * 1.6 + i * 0.137) % 1); const src = srcs[i % srcs.length]; const tgt = dummies[i % 3].position.clone().add(new THREE.Vector3(Math.sin(i * 7.1) * 0.8, 0.6 + Math.cos(i * 3.3) * 0.5, 0)); const p = src.clone().lerp(tgt, k); const b = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.12, 0.5), new THREE.MeshBasicMaterial({ color: 0xfff4a0, transparent: true, opacity: 0.95, blending: THREE.AdditiveBlending, depthWrite: false })); b.position.copy(p); b.lookAt(tgt); parts.push(b); parts.push(tube(p, p.clone().lerp(src, 0.08), 0.04, 0xffe24a, 0.6, true)); if (k > 0.96) parts.push(voxelSphere(tgt, 0.6, 0xffffff, 0.6, true)); }
      parts.forEach(p => root.add(p)); };
  } else if (id === 'yata') {
    const f = figure({ shell: true, shellColor: 0xffe24a, shellAlpha: 0.4, legs: true }); root.add(f);
    const f2 = figure({ shell: true, shellColor: 0xffe24a, shellAlpha: 0.4, legs: true }); f2.position.set(6, 3, -9); root.add(f2);
    const ledge = new THREE.Mesh(new THREE.BoxGeometry(3, 3, 3), new THREE.MeshLambertMaterial({ color: 0x4a5566 })); ledge.position.set(6, 1.5, -9); root.add(ledge);
    camDist = 13; camH = 4; target.set(3, 2, -4);
    let parts = [];
    anim = (t) => { parts.forEach(p => root.remove(p)); parts = []; const cycle = (t % 2.5) / 2.5;
      const a = new THREE.Vector3(0, 1.0, 0), b = new THREE.Vector3(6, 4.0, -9);
      f.visible = cycle < 0.35; f2.visible = cycle >= 0.4;
      if (cycle >= 0.3 && cycle < 0.6) { const k = Math.min(1, (cycle - 0.3) / 0.08); parts.push(tube(a, a.clone().lerp(b, k), 0.28, 0xffe24a, 0.6, true), tube(a, a.clone().lerp(b, k), 0.1, 0xffffff, 1, true), voxelSphere(a, 1.0, 0xffffff, 0.5, true)); if (k >= 1) parts.push(voxelSphere(b, 1.2, 0xffffff, 0.5, true)); }
      parts.forEach(p => root.add(p)); };
  } else if (id === 'tatsumaki' || id === 'ragnaraku') {
    const d = dragon(); d.scale.setScalar(2.2); root.add(d);
    const dummies = [[-5, 0, -2], [4, 0, -5], [1, 0, 5]].map(v => { const m = new THREE.Mesh(new THREE.BoxGeometry(0.6, 1.8, 0.6), new THREE.MeshLambertMaterial({ color: 0x5a6b7c })); m.position.set(v[0], 0.9, v[2]); root.add(m); return m; });
    camDist = 22; camH = 8; target.set(0, 3, 0);
    let parts = [];
    anim = (t) => { parts.forEach(p => root.remove(p)); parts = []; animateDragon(d, t, true, 0.1);
      if (id === 'tatsumaki') {
        d.position.y = 3; d.rotation.y = t * 1.5;
        for (let i = 0; i < 6; i++) { const r = 4 + i * 1.1, y = 0.5 + i * 1.3; const g = new THREE.Mesh(new THREE.TorusGeometry(r, 0.12, 6, 40), new THREE.MeshBasicMaterial({ color: 0xbff3ec, transparent: true, opacity: 0.6, blending: THREE.AdditiveBlending, depthWrite: false })); g.rotation.x = Math.PI / 2; g.position.y = y; g.rotation.z = t * (1 + i * 0.1); parts.push(g); }
        dummies.forEach((m, i) => { const a = t * 3 + i * 2; m.position.set(Math.cos(a) * 5, 1 + ((t + i) % 3) * 1.5, Math.sin(a) * 5); });
      } else {
        const cycle = (t % 3) / 3;
        if (cycle < 0.35) { d.position.y = 3 + cycle / 0.35 * 20; d.rotation.x = -0.6; }
        else if (cycle < 0.55) { const k = (cycle - 0.35) / 0.2; d.position.y = 23 - k * 21; d.rotation.x = 1.2; parts.push(tube(new THREE.Vector3(0, 24, 0), new THREE.Vector3(0, d.position.y, 0), 0.6, 0x7fd4ff, 0.5, true)); }
        else { d.position.y = 2; d.rotation.x = 0; const k = Math.min(1, (cycle - 0.55) / 0.15); for (const [r, col] of [[9, 0x7fd4ff], [6.5, 0xff5a1f], [4, 0xffffff]]) { const g = new THREE.Mesh(new THREE.TorusGeometry(r * k, 0.2, 6, 48), new THREE.MeshBasicMaterial({ color: col, transparent: true, opacity: 0.8 * (1 - k * 0.5), blending: THREE.AdditiveBlending, depthWrite: false })); g.rotation.x = Math.PI / 2; g.position.y = 0.3; parts.push(g); } for (let i = 0; i < 5; i++) parts.push(tube(new THREE.Vector3(Math.sin(i * 2.1) * 7 * k, 20, Math.cos(i * 2.1) * 7 * k), new THREE.Vector3(Math.sin(i * 2.1) * 7 * k, 0, Math.cos(i * 2.1) * 7 * k), 0.08, 0xbfe9ff, 0.9, true)); dummies.forEach((m, i) => { m.position.y = 0.9 + k * 3; m.position.x *= 1 + k * 0.05; }); }
      }
      parts.forEach(p => root.add(p)); };
  }
}

const ui = document.getElementById('ui');
scenes.forEach((s, i) => { const b = document.createElement('button'); b.textContent = s.label; b.onclick = () => select(i); ui.appendChild(b); });
let current = 0;
function select(i) { current = i; [...ui.children].forEach((b, k) => b.classList.toggle('on', k === i)); document.getElementById('title').textContent = scenes[i].title; document.getElementById('desc').textContent = scenes[i].desc; build(scenes[i].id); }
const q = new URLSearchParams(location.search); const start = scenes.findIndex(s => s.id === q.get('scene'));
select(start >= 0 ? start : 0);
if (q.get('orbit')) orbit = parseFloat(q.get('orbit'));

function frame() {
  requestAnimationFrame(frame);
  const t = (performance.now() - t0) / 1000;
  if (!dragging && !q.get('still')) orbit += 0.004;
  camera.position.set(target.x + Math.sin(orbit) * camDist, camH + target.y, target.z + Math.cos(orbit) * camDist);
  camera.lookAt(target);
  if (anim) anim(q.get('t') ? parseFloat(q.get('t')) : t);
  renderer.render(scene, camera);
}
frame();
</script>
</body>
</html>
'''
with open(os.path.join(ROOT, 'preview', 'dragon.js')) as f:
    rig_js = f.read()
html = html.replace('__MODELS__', json.dumps(models)).replace('__TEX__', json.dumps(textures)).replace('__RIG__', rig_js)
os.makedirs(os.path.join(ROOT, 'preview'), exist_ok=True)
with open(os.path.join(ROOT, 'preview', 'index.html'), 'w') as f:
    f.write(html)
print('preview written', len(html) // 1024, 'KB')
