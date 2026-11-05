package com.messickmedia.grandline.client;

import com.messickmedia.grandline.GrandLine;
import com.messickmedia.grandline.net.Fx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Devil fruit effects drawn in 3D: stretched rubber limbs with fists, light lasers and dragon breath as thick glowing
 * beams, crescent sweeps, shockwave rings, lightning bolts, voxel clouds (steam, flame clouds) and tumbling
 * Minecraft-style cube particles that bounce on blocks. Each style dresses the same primitives its own way.
 * The 3D preview page's fx.js mirrors this class.
 */
public final class FxClient {
    private static final ResourceLocation CUBE_TEX = GrandLine.id("textures/fx/cube.png");
    private static final Random R = new Random();
    private static final int MAX_CUBES = 6000;

    /** Primary and core colour of each style (index = Fx style). */
    public static final int[][] COLORS = {
            {0xE8A77A, 0xFFD9B8}, // RUBBER
            {0xE6EEF3, 0xFFFFFF}, // STEAM
            {0x1C1C22, 0x8A1F2A}, // HAKI
            {0xFF5A1F, 0xFFD36A}, // FIRE
            {0xFFE24A, 0xFFFFFF}, // LIGHT
            {0x7FD4FF, 0xFFFFFF}, // LIGHTNING
            {0xBFF3EC, 0xFFFFFF}, // WIND
            {0xFF8A3A, 0xFFE0A0}, // CLOUD
            {0xFFFFFF, 0xFFF7D6}, // NIKA
            {0x3FA7D6, 0xCFF0FF}, // DRAGON
            {0x2F7BFF, 0xBFE6FF}}; // WATER

    // ------------------------------------------------------------------ state

    static final class Ribbon {
        final double[] in, out;
        final int n;
        final float[] cIn, cOut;
        final float life, reveal, tail, alpha, delay;
        final boolean flicker;
        boolean solid, retract;
        float age;

        Ribbon(List<Vec3[]> pairs, int outer, int inner, float life, float reveal, float tail, float alpha, float delay, boolean flicker) {
            n = pairs.size();
            in = new double[n * 3];
            out = new double[n * 3];
            for (int i = 0; i < n; i++) {
                Vec3 a = pairs.get(i)[0], b = pairs.get(i)[1];
                in[i * 3] = a.x; in[i * 3 + 1] = a.y; in[i * 3 + 2] = a.z;
                out[i * 3] = b.x; out[i * 3 + 1] = b.y; out[i * 3 + 2] = b.z;
            }
            cIn = rgb(inner);
            cOut = rgb(outer);
            this.life = life; this.reveal = reveal; this.tail = tail; this.alpha = alpha; this.delay = delay; this.flicker = flicker;
        }
    }

    static final class Cube {
        double x, y, z, px, py, pz, vx, vy, vz;
        float size, life, age, gravity, drag, grow, spin, rx, ry;
        float[] c;
        boolean glow, flat, cloud, solid;
        float alpha = 1, fadeAt = 0.6f;
    }

    private static final List<Ribbon> RIBBONS = new ArrayList<>();
    private static final List<Cube> CUBES = new ArrayList<>();

    /** A stretched limb of a player: drawn with that player's skin from the shoulder (or hip) to the target. */
    static final class Limb {
        int owner, limb, style;
        Vec3 to;
        float thick, life, age;
    }

    private static final List<Limb> LIMBS = new ArrayList<>();

    public static void stretch(com.messickmedia.grandline.net.ModNetwork.Stretch s) {
        Limb k = new Limb();
        k.owner = s.entityId(); k.limb = s.limb(); k.style = s.style();
        k.to = new Vec3(s.x(), s.y(), s.z());
        k.thick = s.thick(); k.life = Math.max(3, s.life());
        LIMBS.add(k);
        // the fist lands: a burst at the far end
        impact(s.style() == Fx.HAKI ? Fx.HAKI : s.style() == Fx.NIKA ? Fx.NIKA : s.style() == Fx.FIRE ? Fx.FIRE : Fx.STEAM, k.to);
    }

    public static float[] rgb(int c) {
        return new float[]{((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f};
    }

    static double rnd(double a, double b) {
        return a + R.nextDouble() * (b - a);
    }

    static Vec3 dir(double yawRad) {
        return new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
    }

    // ------------------------------------------------------------------ building blocks

    static Ribbon ribbon(List<Vec3[]> pairs, int outer, int inner, float life, float reveal, float tail, float alpha, float delay, boolean flicker) {
        Ribbon r = new Ribbon(pairs, outer, inner, life, reveal, tail, alpha, delay, flicker);
        if (r.n >= 2) RIBBONS.add(r);
        return r;
    }

    record ArcPt(double t, Vec3 p, Vec3 d) {}

    static List<ArcPt> arcPoints(Vec3 c, double r, double yawRad, double widthRad, double tilt, int n) {
        List<ArcPt> out = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n, a = yawRad - widthRad / 2 + widthRad * t;
            Vec3 d = dir(a);
            out.add(new ArcPt(t, c.add(d.scale(r)).add(0, tilt * (t - 0.5) * 2, 0), d));
        }
        return out;
    }

    /** Crescent sheet: thick in the middle, sharp at both ends, in the plane of the cut. */
    static void crescent(List<ArcPt> pts, double thick, int outer, int inner, float life, float reveal, float alpha, float delay) {
        List<Vec3[]> pairs = new ArrayList<>();
        for (ArcPt q : pts) {
            double w = thick * Math.pow(Math.sin(Math.PI * q.t), 0.7);
            pairs.add(new Vec3[]{q.p.add(q.d.scale(-w)), q.p.add(q.d.scale(w * 0.25))});
        }
        ribbon(pairs, outer, inner, life, reveal, 1.4f, alpha, delay, false);
    }

    /** Crossed thin ribbons along a polyline, so it reads as a 3D streak (or a tube, with more sides) from any angle. */
    static List<Ribbon> streak(List<Vec3> pts, double width, int outer, int inner, float life, float reveal, float tail, float alpha, float delay,
                               boolean flicker, boolean taper, int sides) {
        Vec3 up = new Vec3(0, 1, 0);
        List<Ribbon> out = new ArrayList<>();
        for (int side = 0; side < sides; side++) {
            List<Vec3[]> pairs = new ArrayList<>();
            for (int i = 0; i < pts.size(); i++) {
                Vec3 d = pts.get(Math.min(pts.size() - 1, i + 1)).subtract(pts.get(Math.max(0, i - 1)));
                d = d.lengthSqr() < 1.0E-8 ? new Vec3(1, 0, 0) : d.normalize();
                Vec3 s1 = d.cross(up);
                if (s1.lengthSqr() < 1.0E-6) s1 = new Vec3(1, 0, 0);
                s1 = s1.normalize();
                Vec3 s2 = d.cross(s1).normalize();
                double ang = Math.PI * side / sides;
                Vec3 s = s1.scale(Math.cos(ang)).add(s2.scale(Math.sin(ang)));
                double t = (double) i / Math.max(1, pts.size() - 1);
                double w = width * (taper ? 0.35 + 0.65 * Math.sin(Math.PI * t) : 1);
                Vec3 p = pts.get(i);
                pairs.add(new Vec3[]{p.add(s.scale(-w)), p.add(s.scale(w))});
            }
            out.add(ribbon(pairs, outer, inner, life, reveal, tail, alpha, delay, flicker));
        }
        return out;
    }

    static void streak(List<Vec3> pts, double width, int outer, int inner, float life, float reveal, float tail, float alpha, float delay, boolean flicker, boolean taper) {
        streak(pts, width, outer, inner, life, reveal, tail, alpha, delay, flicker, taper, 2);
    }

    static List<Vec3> jagged(Vec3 a, Vec3 b, int segs, double amp) {
        List<Vec3> pts = new ArrayList<>();
        pts.add(a);
        for (int i = 1; i < segs; i++) pts.add(a.lerp(b, (double) i / segs).add(rnd(-amp, amp), rnd(-amp, amp), rnd(-amp, amp)));
        pts.add(b);
        return pts;
    }

    static void boltLine(Vec3 a, Vec3 b, int color, float delay, double width) {
        double len = a.distanceTo(b);
        streak(jagged(a, b, Math.max(4, (int) Math.round(len * 4)), 0.15 + len * 0.025), width, color, 0xFFFFFF, 7, 1, 9, 1, delay, true, false);
    }

    public static void cube(Vec3 p, int color, Vec3 vel, double size, float life, float gravity, float drag, boolean glow,
                            boolean flat, float spin, float grow) {
        if (CUBES.size() >= MAX_CUBES) return;
        Cube k = new Cube();
        k.x = k.px = p.x; k.y = k.py = p.y; k.z = k.pz = p.z;
        k.vx = vel.x; k.vy = vel.y; k.vz = vel.z;
        k.size = (float) size; k.life = life; k.gravity = gravity; k.drag = drag; k.glow = glow; k.flat = flat;
        k.spin = spin; k.grow = grow; k.c = rgb(color);
        k.rx = (float) rnd(0, 6.28); k.ry = (float) rnd(0, 6.28);
        CUBES.add(k);
    }

    static void cube(Vec3 p, int color, Vec3 vel, double size, float life, float gravity) {
        cube(p, color, vel, size, life, gravity, 0.92f, true, false, 0.25f, 0);
    }

    /** A voxel cloud: overlapping translucent blocks that drift, slow down, swell and fade together. */
    static void puff(Vec3 p, int color, Vec3 vel, double size, float life, float alpha, float gravity, float drag, float grow, float fadeAt, int n) {
        if (n <= 0) n = 3 + R.nextInt(4);
        for (int i = 0; i < n && CUBES.size() < MAX_CUBES; i++) {
            Cube k = new Cube();
            Vec3 at = p.add(rnd(-0.5, 0.5) * size, rnd(-0.2, 0.35) * size, rnd(-0.5, 0.5) * size);
            k.x = k.px = at.x; k.y = k.py = at.y; k.z = k.pz = at.z;
            k.vx = vel.x + rnd(-0.004, 0.004); k.vy = vel.y + rnd(-0.002, 0.004); k.vz = vel.z + rnd(-0.004, 0.004);
            k.size = (float) (size * rnd(0.55, 1.0));
            k.life = (float) (life * rnd(0.85, 1.15));
            k.gravity = gravity; k.drag = drag; k.grow = grow; k.spin = 0.004f;
            k.cloud = true; k.alpha = alpha; k.fadeAt = fadeAt;
            float j = (float) rnd(0.92, 1.06);
            float[] c = rgb(color);
            k.c = new float[]{Math.min(1, c[0] * j), Math.min(1, c[1] * j), Math.min(1, c[2] * j)};
            k.ry = (float) rnd(0, 6.28);
            CUBES.add(k);
        }
    }

    static void puff(Vec3 p, int color, Vec3 vel, double size, float life, float alpha) {
        puff(p, color, vel, size, life, alpha, -0.0006f, 0.9f, 0.5f, 0.65f, 0);
    }

    /** A solid, lit voxel (fists, balloons): shaded like a block, not additive. */
    static void solidCube(Vec3 p, int color, double size, float life, float alpha, float grow) {
        if (CUBES.size() >= MAX_CUBES) return;
        Cube k = new Cube();
        k.x = k.px = p.x; k.y = k.py = p.y; k.z = k.pz = p.z;
        k.size = (float) size; k.life = life; k.drag = 1; k.grow = grow; k.c = rgb(color);
        k.cloud = true; k.solid = true; k.alpha = alpha; k.fadeAt = 0.75f; k.spin = 0;
        CUBES.add(k);
    }

    // ------------------------------------------------------------------ style dressing

    static void dress(int s, List<ArcPt> pts, double intensity) {
        if (pts.isEmpty()) return;
        volume(s, pts, intensity);
        int n = (int) Math.round(pts.size() * 1.0 * intensity);
        for (int i = 0; i < n; i++) {
            ArcPt q = pts.get(R.nextInt(pts.size()));
            Vec3 p = q.p;
            Vec3 out = q.d.scale(rnd(0.01, 0.05));
            switch (s) {
                case Fx.RUBBER -> cube(p, R.nextBoolean() ? 0xFFFFFF : 0xE8A77A, out.scale(2).add(0, rnd(0, 0.04), 0), rnd(0.04, 0.08), 12, 0.004f, 0.9f, R.nextBoolean(), false, 0.3f, 0);
                case Fx.STEAM -> cube(p, 0xFFFFFF, out.scale(0.8).add(0, rnd(0.02, 0.05), 0), rnd(0.06, 0.1), 24, -0.001f, 0.94f, false, false, 0.1f, 0.6f);
                case Fx.HAKI -> {
                    cube(p, R.nextFloat() < 0.7f ? 0x16161C : 0x3A3A44, out.add(0, rnd(0.01, 0.05), 0), rnd(0.05, 0.1), 18, 0.006f, 0.92f, false, false, 0.3f, 0);
                    if (R.nextFloat() < 0.25f) boltLine(p, p.add(rnd(-0.5, 0.5), rnd(-0.4, 0.5), rnd(-0.5, 0.5)), 0xFF2A3A, 0, 0.03);
                }
                case Fx.FIRE -> {
                    cube(p, R.nextBoolean() ? 0xFFB020 : 0xFF5A1F, out.add(0, rnd(0.02, 0.06), 0), rnd(0.07, 0.14), (float) rnd(14, 24), -0.003f, 0.92f, true, false, 0.25f, -0.4f);
                    if (R.nextFloat() < 0.3f) cube(p, 0x3A3A3A, new Vec3(0, 0.03, 0), 0.12, 30, -0.002f, 0.92f, false, false, 0.2f, 0.8f);
                }
                case Fx.LIGHT -> cube(p, R.nextFloat() < 0.6f ? 0xFFF4A0 : 0xFFFFFF, out.scale(3).add(0, rnd(-0.02, 0.04), 0), rnd(0.03, 0.07), 10, 0, 0.88f, true, false, 0.6f, 0);
                case Fx.LIGHTNING -> cube(p, R.nextBoolean() ? 0xFFFFFF : 0x9FE0FF, new Vec3(rnd(-0.12, 0.12), rnd(-0.05, 0.12), rnd(-0.12, 0.12)), rnd(0.03, 0.06), 9, 0.01f, 0.85f, true, false, 0.25f, 0);
                case Fx.WIND -> cube(p, R.nextFloat() < 0.6f ? 0xEAFFFB : 0xBFF3EC, out.add(0, rnd(0, 0.03), 0).scale(2), rnd(0.04, 0.08), 22, -0.001f, 0.92f, R.nextBoolean(), false, 0.5f, 0);
                case Fx.CLOUD -> cube(p, R.nextBoolean() ? 0xFFB070 : 0xFF8A3A, out.scale(0.6).add(0, rnd(0.01, 0.03), 0), rnd(0.07, 0.12), 26, -0.0015f, 0.93f, true, false, 0.1f, 0.5f);
                case Fx.NIKA -> cube(p, R.nextFloat() < 0.7f ? 0xFFFFFF : 0xFFF0B0, out.scale(2).add(0, rnd(0.01, 0.05), 0), rnd(0.05, 0.1), 16, -0.001f, 0.9f, R.nextBoolean(), false, 0.3f, 0.2f);
                case Fx.DRAGON -> cube(p, R.nextBoolean() ? 0x3FA7D6 : 0x9FE0FF, out.add(0, rnd(0.01, 0.04), 0), rnd(0.05, 0.09), 20, 0.004f, 0.92f, R.nextFloat() < 0.4f, false, 0.25f, 0);
                case Fx.WATER -> cube(p, R.nextBoolean() ? 0x5AA0FF : 0xCFEAFF, out.add(0, rnd(0.04, 0.1), 0), rnd(0.05, 0.1), 26, 0.012f, 0.92f, R.nextFloat() < 0.4f, false, 0.25f, 0);
                default -> { }
            }
        }
    }

    static void volume(int s, List<ArcPt> pts, double intensity) {
        int m = Math.max(1, (int) Math.round(pts.size() * 0.2 * intensity));
        for (int i = 0; i < m; i++) {
            ArcPt q = pts.get(R.nextInt(pts.size()));
            Vec3 p = q.p, d = q.d;
            switch (s) {
                case Fx.STEAM, Fx.RUBBER -> puff(p, R.nextBoolean() ? 0xF2F6F8 : 0xDDE6EC, d.scale(0.05).add(0, 0.03, 0), rnd(0.18, 0.3), 30, 0.45f, -0.001f, 0.92f, 0.7f, 0.6f, 3);
                case Fx.HAKI -> puff(p, 0x101014, d.scale(0.03), rnd(0.16, 0.24), 24, 0.5f, 0, 0.9f, 0.4f, 0.6f, 3);
                case Fx.FIRE -> {
                    int[] fire = {0xFFE08A, 0xFF9A1F, 0xFF4A10};
                    for (int k = 0; k < 3; k++) cube(p.add(rnd(-0.1, 0.1), k * 0.12, rnd(-0.1, 0.1)), fire[k], new Vec3(0, rnd(0.02, 0.04), 0), 0.16 - k * 0.03, (float) rnd(12, 20), -0.002f, 0.92f, true, false, 0.05f, 0);
                    puff(p.add(0, 0.3, 0), 0x2E2A2A, new Vec3(0, 0.025, 0), rnd(0.24, 0.34), 55, 0.5f, -0.0012f, 0.9f, 0.9f, 0.65f, 0);
                }
                case Fx.LIGHT, Fx.NIKA -> { for (int k = 0; k < 3; k++) cube(p.add(rnd(-0.2, 0.2), rnd(-0.2, 0.2), rnd(-0.2, 0.2)), 0xFFFFFF, Vec3.ZERO, rnd(0.05, 0.1), 5, 0, 0.92f, true, false, 0.6f, 1); }
                case Fx.LIGHTNING -> { for (int k = 0; k < 4; k++) cube(p.add(rnd(-0.2, 0.2), rnd(-0.2, 0.2), rnd(-0.2, 0.2)), 0xDFF4FF, Vec3.ZERO, rnd(0.06, 0.12), 4, 0, 0.92f, true, false, 0.6f, 1); }
                case Fx.WIND -> puff(p, 0xD6F0EC, d.scale(0.12).add(0, 0.01, 0), rnd(0.16, 0.24), 32, 0.4f, -0.0006f, 0.9f, 0.6f, 0.65f, 3);
                case Fx.CLOUD -> puff(p, R.nextBoolean() ? 0xFF9A4A : 0xFFC080, d.scale(0.02).add(0, 0.01, 0), rnd(0.3, 0.5), 60, 0.6f, -0.0004f, 0.92f, 0.6f, 0.65f, 4);
                case Fx.DRAGON -> puff(p, 0x2E7FB0, d.scale(0.03), rnd(0.2, 0.3), 36, 0.4f, -0.0006f, 0.9f, 0.5f, 0.65f, 3);
                case Fx.WATER -> puff(p, R.nextBoolean() ? 0xA9D4FF : 0xE6F4FF, d.scale(0.05).add(0, 0.025, 0), rnd(0.16, 0.26), 28, 0.55f, 0.003f, 0.92f, 0.3f, 0.65f, 4);
                default -> { }
            }
        }
    }

    static List<ArcPt> around(Vec3 c, double radius, int n, double up) {
        List<ArcPt> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            Vec3 d = new Vec3(Math.cos(a), up, Math.sin(a)).normalize();
            out.add(new ArcPt(0, c.add(Math.cos(a) * radius, 0, Math.sin(a) * radius), d));
        }
        return out;
    }

    // ------------------------------------------------------------------ primitives

    static void arc(int s, Vec3 c, double r, double yawDeg, double arcDeg, double tilt, boolean light, float life) {
        int[] col = COLORS[s];
        double yaw = Math.toRadians(yawDeg), width = Math.toRadians(arcDeg);
        int n = Math.max(24, (int) Math.round(width * r * 10));
        List<ArcPt> pts = arcPoints(c, r, yaw, width, tilt, n);
        double thick = 0.3 + r * 0.07;
        float[] k = rgb(col[1]);
        int inner = ((int) ((k[0] * 0.6f + 0.4f) * 255) << 16) | ((int) ((k[1] * 0.6f + 0.4f) * 255) << 8) | (int) ((k[2] * 0.6f + 0.4f) * 255);
        crescent(pts, thick, col[0], inner, life > 0 ? life : 14, light ? 1 : 3, 1, 0);
        if (light) {
            dress(s, pts, 0.25);
            return;
        }
        for (double dy : new double[]{-0.07, 0.07}) {
            List<ArcPt> layer = new ArrayList<>();
            for (ArcPt q : pts) layer.add(new ArcPt(q.t, q.p.add(0, dy, 0), q.d));
            crescent(layer, thick * 0.85, col[0], col[0], 12, 3, 0.4f, 0);
        }
        List<Vec3> path = new ArrayList<>();
        for (ArcPt q : pts) path.add(q.p);
        streak(path, 0.025, col[1], 0xFFFFFF, 10, 3, 1.2f, 1, 0, false, true);
        switch (s) {
            case Fx.FIRE -> crescent(arcPoints(c.add(0, 0.12, 0), r * 1.05, yaw, width, tilt, n), thick * 1.25, 0xC81E00, 0xFFB020, 12, 3, 0.7f, 1);
            case Fx.LIGHTNING -> {
                for (int i = 0; i < 3; i++) {
                    Vec3 a = pts.get(R.nextInt(pts.size() / 2)).p, b = pts.get(pts.size() / 2 + R.nextInt(pts.size() / 2)).p;
                    boltLine(a.add(0, rnd(-0.3, 0.3), 0), b.add(0, rnd(-0.3, 0.3), 0), 0xBFE9FF, i, 0.05);
                }
            }
            case Fx.WIND -> {
                for (int k2 = 0; k2 < 3; k2++) {
                    List<Vec3> hel = new ArrayList<>();
                    for (ArcPt q : pts) {
                        double ph = q.t * Math.PI * 4 + k2 * 2.1;
                        hel.add(q.p.add(0, Math.sin(ph) * 0.35, 0).add(q.d.scale(Math.cos(ph) * 0.35)));
                    }
                    streak(hel, 0.03, 0xDFFFF6, 0xFFFFFF, 14, 4, 1.4f, 0.7f, k2, false, true);
                }
            }
            case Fx.HAKI -> {
                for (int i = 0; i < 3; i++) {
                    Vec3 a = pts.get(R.nextInt(pts.size())).p;
                    boltLine(a, a.add(rnd(-0.8, 0.8), rnd(-0.5, 0.8), rnd(-0.8, 0.8)), 0xFF2A3A, i, 0.04);
                }
            }
            case Fx.NIKA, Fx.LIGHT -> crescent(arcPoints(c.add(0, 0.1, 0), r * 1.08, yaw, width * 1.05, tilt, n), thick * 1.3, 0xFFFFFF, 0xFFFFFF, 10, 2, 0.5f, 1);
            default -> { }
        }
        dress(s, pts, 1);
    }

    static void ring(int s, Vec3 c, double r, boolean vertical, boolean light) {
        int[] col = COLORS[s];
        int steps = light ? 1 : 3;
        for (int st = 0; st < steps; st++) {
            double rr = r * (0.45 + 0.55 * (st + 1) / steps);
            int n = (int) Math.round(30 + rr * 14);
            List<Vec3[]> pairs = new ArrayList<>();
            for (int i = 0; i <= n; i++) {
                double a = (double) i / n * Math.PI * 2;
                Vec3 d = vertical ? new Vec3(Math.cos(a), Math.sin(a), 0) : new Vec3(Math.cos(a), 0, Math.sin(a));
                pairs.add(new Vec3[]{c.add(d.scale(rr - 0.12 - rr * 0.08)), c.add(d.scale(rr))});
            }
            ribbon(pairs, st == steps - 1 ? col[0] : col[1], col[1], 12 + st * 2, 1, 3, 0.9f, st * 2, false);
        }
        if (!light) dress(s, around(c.add(0, 0.05, 0), r, 24, 0.3), 0.8);
    }

    static void line(int s, Vec3 a, Vec3 b) {
        int[] col = COLORS[s];
        int n = Math.max(10, (int) Math.round(a.distanceTo(b) * 6));
        List<Vec3> pts = new ArrayList<>();
        for (int i = 0; i <= n; i++) pts.add(a.lerp(b, (double) i / n));
        streak(pts, 0.16, col[0], col[1], 14, 2, 1.4f, 1, 0, false, true);
        streak(pts, 0.03, 0xFFFFFF, 0xFFFFFF, 9, 2, 1.4f, 1, 0, false, true);
        if (s == Fx.LIGHTNING) { boltLine(a, b, 0xBFE9FF, 0, 0.05); boltLine(a.add(0, 0.2, 0), b.add(0, -0.1, 0), 0xBFE9FF, 1, 0.05); }
        if (s == Fx.FIRE) streak(pts, 0.32, 0xC81E00, 0xFFB020, 12, 2, 1.4f, 0.6f, 0, false, true);
        List<ArcPt> ap = new ArrayList<>();
        for (Vec3 p : pts) ap.add(new ArcPt(0, p, new Vec3(rnd(-1, 1), 0.3, rnd(-1, 1)).normalize()));
        dress(s, ap, 0.6);
    }

    static void impact(int s, Vec3 p) {
        int[] col = COLORS[s];
        for (int i = 0; i < 10; i++) {
            Vec3 d = new Vec3(rnd(-1, 1), rnd(-0.3, 1), rnd(-1, 1)).normalize().scale(rnd(0.08, 0.16));
            cube(p, i % 2 == 1 ? 0xFFFFFF : col[1], d, rnd(0.04, 0.07), 8, 0.01f, 0.8f, true, false, 0.25f, 0);
        }
        cube(p, col[0], Vec3.ZERO, 0.35, 5, 0, 0.92f, true, false, 0.6f, 1.5f);
        // impact lines: the classic burst of straight white streaks
        for (int i = 0; i < 6; i++) {
            Vec3 d = new Vec3(rnd(-1, 1), rnd(-1, 1), rnd(-1, 1)).normalize();
            streak(List.of(p.add(d.scale(0.2)), p.add(d.scale(rnd(0.7, 1.3)))), 0.03, 0xFFFFFF, col[1], 6, 1, 2, 1, 0, false, true);
        }
        if (s == Fx.FIRE) for (int i = 0; i < 8; i++) cube(p.add(rnd(-0.3, 0.3), rnd(-0.6, 0.3), rnd(-0.3, 0.3)), 0xFF8A20, new Vec3(0, rnd(0.02, 0.05), 0), 0.1, 22, -0.002f, 0.92f, true, false, 0.25f, -0.5f);
        List<ArcPt> pts = List.of(new ArcPt(0, p, new Vec3(0, 1, 0)), new ArcPt(0, p, new Vec3(1, 0, 0)),
                new ArcPt(0, p, new Vec3(-1, 0, 0)), new ArcPt(0, p, new Vec3(0, 0, 1)));
        dress(s, pts, 4);
    }

    static void slam(int s, Vec3 c, double r) {
        ring(s, c.add(0, 0.05, 0), r, false, false);
        ClientLevel level = Minecraft.getInstance().level;
        for (int i = 0; i < 40; i++) {
            double a = rnd(0, 6.28), d = rnd(0.2, r);
            Vec3 at = c.add(Math.cos(a) * d, 0.1, Math.sin(a) * d);
            int color = 0x6A6A6A;
            if (level != null) {
                BlockPos bp = BlockPos.containing(at.x, at.y - 0.5, at.z);
                int map = level.getBlockState(bp).getMapColor(level, bp).col;
                if (map != 0) color = map;
            }
            cube(at, color, new Vec3(Math.cos(a) * 0.04, rnd(0.1, 0.22), Math.sin(a) * 0.04), rnd(0.1, 0.2), 40, 0.02f, 0.92f, false, false, 0.25f, 0);
        }
        for (int i = 0; i < 22; i++) {
            double a = rnd(0, 6.28), d = rnd(0.3, r);
            puff(c.add(Math.cos(a) * d, 0.25, Math.sin(a) * d), R.nextBoolean() ? 0xB0A898 : 0x8E8577,
                    new Vec3(Math.cos(a) * 0.07, 0.015, Math.sin(a) * 0.07), rnd(0.35, 0.5), (float) rnd(60, 90), 0.5f, -0.0006f, 0.9f, 0.9f, 0.65f, 0);
        }
    }

    /** A stretched rubber limb: a solid tube that shoots out to b and snaps back, with a fist at the end. */
    static void arm(int s, Vec3 a, Vec3 b, double thick, float life) {
        int[] col = COLORS[s];
        int n = Math.max(6, (int) Math.round(a.distanceTo(b) * 3));
        List<Vec3> pts = new ArrayList<>();
        for (int i = 0; i <= n; i++) pts.add(a.lerp(b, (double) i / n));
        int shade = darken(col[0], 0.75f);
        for (Ribbon r : streak(pts, thick, col[0], shade, life, 2, 0.6f, 1, 0, false, false, 3)) { r.solid = true; r.retract = true; }
        // the fist
        Vec3 d = b.subtract(a).normalize();
        double fs = thick * 3.2;
        solidCube(b.subtract(d.scale(fs * 0.3)), col[0], fs, life, 1f, 0);
        solidCube(b.subtract(d.scale(fs * 0.3)).add(0, fs * 0.15, 0), shade, fs * 0.5, life, 1f, 0);
        if (s == Fx.HAKI) {
            for (int i = 0; i < 3; i++) {
                Vec3 p = pts.get(R.nextInt(pts.size()));
                boltLine(p, p.add(rnd(-0.6, 0.6), rnd(-0.5, 0.6), rnd(-0.6, 0.6)), 0xFF2A3A, i, 0.03);
            }
        }
        if (s == Fx.FIRE) streak(pts, thick * 1.8, 0xFF5A1F, 0xFFD36A, life, 2, 0.6f, 0.6f, 0, true, false, 3);
        if (s == Fx.NIKA) streak(pts, thick * 1.6, 0xFFFFFF, 0xFFF7D6, life, 2, 0.6f, 0.4f, 0, false, false, 2);
        List<ArcPt> ap = new ArrayList<>();
        for (int i = 0; i < pts.size(); i += 2) ap.add(new ArcPt(0, pts.get(i), new Vec3(rnd(-1, 1), 0.3, rnd(-1, 1)).normalize()));
        dress(s == Fx.RUBBER ? Fx.STEAM : s, ap, 0.3);
    }

    /** A thick glowing ray: light laser, dragon breath. */
    static void beam(int s, Vec3 a, Vec3 b, double radius, float life) {
        int[] col = COLORS[s];
        int n = Math.max(10, (int) Math.round(a.distanceTo(b) * 4));
        List<Vec3> pts = new ArrayList<>();
        for (int i = 0; i <= n; i++) pts.add(a.lerp(b, (double) i / n));
        streak(pts, radius * 1.6, col[0], col[1], life, 1, 3, 0.5f, 0, false, false, 3);
        streak(pts, radius * 0.9, col[1], 0xFFFFFF, life, 1, 3, 0.9f, 0, false, false, 3);
        streak(pts, radius * 0.35, 0xFFFFFF, 0xFFFFFF, life, 1, 3, 1, 0, false, false, 2);
        if (s == Fx.FIRE) {
            List<Vec3> wv = new ArrayList<>();
            for (int i = 0; i < pts.size(); i++) wv.add(pts.get(i).add(0, Math.sin(i * 0.9) * radius * 0.8, Math.cos(i * 0.7) * radius * 0.8));
            streak(wv, radius * 0.6, 0xFF3A00, 0xFFB020, life, 1, 3, 0.7f, 0, true, false, 2);
        }
        if (s == Fx.LIGHTNING) boltLine(a, b, 0xBFE9FF, 0, radius * 0.5);
        List<ArcPt> ap = new ArrayList<>();
        for (int i = 0; i < pts.size(); i += 2) ap.add(new ArcPt(0, pts.get(i), new Vec3(rnd(-1, 1), rnd(-1, 1), rnd(-1, 1)).normalize()));
        dress(s, ap, 1.2);
        sphere(s, b, radius * 2.5, Math.max(4, life * 0.6f));
    }

    /** A voxel sphere: balloons, giant fists, bursts of light. */
    static void sphere(int s, Vec3 c, double r, float life) {
        int[] col = COLORS[s];
        boolean solid = s == Fx.RUBBER || s == Fx.HAKI || s == Fx.STEAM;
        double step = Math.max(0.25, r / 2.2);
        int m = (int) Math.ceil(r / step);
        for (int x = -m; x <= m; x++) for (int y = -m; y <= m; y++) for (int z = -m; z <= m; z++) {
            double dx = x * step, dy = y * step, dz = z * step;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > r) continue;
            boolean edge = d > r - step;
            int color = edge ? col[0] : col[1];
            if (solid) solidCube(c.add(dx, dy, dz), color, step * 1.05, life, 0.95f, 0.05f);
            else cube(c.add(dx, dy, dz), color, Vec3.ZERO, step * (edge ? 0.9 : 1.05), life, 0, 1, true, false, 0, 0.1f);
        }
        if (!solid) dress(s, around(c, r, 16, 0.5), 0.6);
    }

    static void bolt(int s, Vec3 a, Vec3 b) {
        int color = s == Fx.HAKI ? 0xFF2A3A : s == Fx.FIRE ? 0xFFB020 : s == Fx.LIGHT ? 0xFFF4A0 : 0xBFE9FF;
        double len = a.distanceTo(b);
        boltLine(a, b, color, 0, 0.06 + len * 0.004);
        boltLine(a, b.add(rnd(-1, 1), 0, rnd(-1, 1)), color, 1, 0.04);
        for (int i = 0; i < 3; i++) {
            Vec3 p = a.lerp(b, rnd(0.2, 0.9));
            boltLine(p, p.add(rnd(-1.5, 1.5), rnd(-1.5, 0.5), rnd(-1.5, 1.5)), color, i, 0.03);
        }
        for (int i = 0; i < 12; i++) cube(b, 0xFFFFFF, new Vec3(rnd(-0.2, 0.2), rnd(0, 0.3), rnd(-0.2, 0.2)), rnd(0.04, 0.08), 10, 0.01f, 0.9f, true, false, 0.4f, 0);
    }

    static int darken(int c, float f) {
        return ((int) (((c >> 16) & 255) * f) << 16) | ((int) (((c >> 8) & 255) * f) << 8) | (int) ((c & 255) * f);
    }

    // ------------------------------------------------------------------ packet entry

    public static void handle(Fx p) {
        int s = Math.floorMod(p.style(), COLORS.length);
        float[] f = p.f();
        Vec3 at = new Vec3(f[0], f[1], f[2]);
        switch (p.kind()) {
            case Fx.ARC -> arc(s, at, f[4], f[3], f[5], f[6], f.length > 7 && f[7] > 0, f.length > 8 ? f[8] : 0);
            case Fx.RING -> ring(s, at, f[3], f.length > 4 && f[4] > 0, false);
            case Fx.LINE -> line(s, at, new Vec3(f[3], f[4], f[5]));
            case Fx.IMPACT -> impact(s, at);
            case Fx.SLAM -> slam(s, at, f[3]);
            case Fx.DRESS -> dress(s, around(at, f[3], 30, 0.4), f[4]);
            case Fx.ARM -> arm(s, at, new Vec3(f[3], f[4], f[5]), f[6], f.length > 7 ? f[7] : 8);
            case Fx.BEAM -> beam(s, at, new Vec3(f[3], f[4], f[5]), f[6], f.length > 7 ? f[7] : 10);
            case Fx.PUFF -> {
                int[] col = COLORS[s];
                int n = Math.max(1, Math.round(f[4]));
                for (int i = 0; i < n; i++) {
                    Vec3 q = at.add(rnd(-f[3], f[3]), rnd(-f[3] * 0.4, f[3] * 0.6), rnd(-f[3], f[3]));
                    Vec3 v = q.subtract(at).normalize().scale(0.03).add(0, 0.015, 0);
                    puff(q, R.nextBoolean() ? col[0] : col[1], v, rnd(0.25, 0.45) * Math.max(1, f[3] * 0.5), (float) rnd(40, 70), s == Fx.CLOUD ? 0.7f : 0.5f, -0.0006f, 0.9f, 0.8f, 0.6f, 3);
                }
            }
            case Fx.BOLT -> bolt(s, at, new Vec3(f[3], f[4], f[5]));
            case Fx.SPHERE -> sphere(s, at, f[3], f.length > 4 ? f[4] : 8);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ tick + render

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        ClientLevel level = mc.level;
        RIBBONS.removeIf(r -> ++r.age > r.life + r.delay);
        LIMBS.removeIf(k -> ++k.age > k.life);
        CUBES.removeIf(k -> {
            k.px = k.x; k.py = k.y; k.pz = k.z;
            k.vy -= k.gravity;
            k.vx *= k.drag; k.vy *= k.drag; k.vz *= k.drag;
            k.x += k.vx; k.y += k.vy; k.z += k.vz;
            if (level != null && k.gravity > 0) {
                BlockPos bp = BlockPos.containing(k.x, k.y - k.size / 2, k.z);
                if (!level.getBlockState(bp).getCollisionShape(level, bp).isEmpty()) {
                    k.y = k.py;
                    k.vy = -k.vy * 0.35;
                    k.vx *= 0.6;
                    k.vz *= 0.6;
                }
            }
            k.rx += k.spin * 0.6f;
            k.ry += k.spin;
            return ++k.age >= k.life;
        });
    }

    public static void clear() {
        RIBBONS.clear();
        CUBES.clear();
        LIMBS.clear();
    }

    public static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (RIBBONS.isEmpty() && CUBES.isEmpty() && LIMBS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera cam = e.getCamera();
        Vec3 cp = cam.getPosition();
        PoseStack ps = e.getPoseStack() != null ? e.getPoseStack() : new PoseStack();
        ps.pushPose();
        ps.translate(-cp.x, -cp.y, -cp.z);
        PoseStack.Pose pose = ps.last();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        ClientLevel level = mc.level;

        for (Limb k : LIMBS) drawLimb(buffers, pose, k, pt, level);

        // solid bodies first (rubber limbs, fists, balloons)
        RenderType solidType = RenderType.entityTranslucentCull(CUBE_TEX);
        VertexConsumer solid = buffers.getBuffer(solidType);
        for (Cube k : CUBES) {
            if (k.glow) continue;
            int light = level == null ? 0xF000F0 : LevelRenderer.getLightColor(level, BlockPos.containing(k.x, k.y, k.z));
            drawCube(null, solid, pose, k, pt, light);
        }
        buffers.endBatch(solidType);
        VertexConsumer solidRibbon = buffers.getBuffer(FxRenderTypes.SOLID);
        for (Ribbon r : RIBBONS) if (r.solid) drawRibbon(solidRibbon, pose, r, pt);
        buffers.endBatch(FxRenderTypes.SOLID);

        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.GLOW);
        for (Ribbon r : RIBBONS) if (!r.solid) drawRibbon(glow, pose, r, pt);
        for (Cube k : CUBES) if (k.glow) drawCube(glow, null, pose, k, pt, 0);
        buffers.endBatch(FxRenderTypes.GLOW);
        ps.popPose();
    }

    // ------------------------------------------------------------------ stretched limbs

    /** Skin texture rects (64x64) per limb: [right, front, left, back, bottom] as u,v of the top-left corner; width in px. */
    private static int[][] limbUv(int limb, boolean slim) {
        int w = (limb < 2 && slim) ? 3 : 4;
        return switch (limb) {
            case 0 -> new int[][]{{40, 20}, {44, 20}, {44 + w, 20}, {48 + w, 20}, {44 + w, 16}, {w, 12}};
            case 1 -> new int[][]{{32, 52}, {36, 52}, {36 + w, 52}, {40 + w, 52}, {36 + w, 48}, {w, 12}};
            case 2 -> new int[][]{{0, 20}, {4, 20}, {8, 20}, {12, 20}, {8, 16}, {4, 12}};
            default -> new int[][]{{16, 52}, {20, 52}, {24, 52}, {28, 52}, {24, 48}, {4, 12}};
        };
    }

    private static void drawLimb(MultiBufferSource.BufferSource buffers, PoseStack.Pose pose, Limb k, float pt, ClientLevel level) {
        if (level == null) return;
        net.minecraft.world.entity.Entity e = level.getEntity(k.owner);
        if (!(e instanceof net.minecraft.client.player.AbstractClientPlayer p)) return;
        net.minecraft.client.resources.PlayerSkin skin = p.getSkin();
        boolean slim = skin.model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM;
        float sc = p.getScale();
        float yaw = Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 base = new Vec3(Mth.lerp(pt, p.xo, p.getX()), Mth.lerp(pt, p.yo, p.getY()), Mth.lerp(pt, p.zo, p.getZ()));
        boolean arm = k.limb < 2;
        double side = (k.limb % 2 == 0 ? 1 : -1) * (arm ? 0.3125 : 0.125) * sc;
        Vec3 from = base.add(0, (arm ? 1.4 : 0.75) * sc, 0).add(right.scale(side));
        // extend over the first ticks, hold, then snap back
        float a = k.age + pt;
        float out = Math.min(1f, a / 2f);
        float back = a > k.life - 3 ? Math.max(0f, (k.life - a) / 3f) : 1f;
        float ext = out * back;
        if (ext <= 0.02f) return;
        Vec3 d = k.to.subtract(from);
        double len = d.length() * ext;
        if (len < 0.05) return;
        d = d.normalize();
        Vec3 up = Math.abs(d.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 s1 = d.cross(up).normalize();      // "right" of the limb
        Vec3 s2 = s1.cross(d).normalize();      // "front" of the limb
        float w = k.thick * sc;
        int[][] uv = limbUv(k.limb, slim);
        int wpx = uv[5][0], hpx = uv[5][1];
        float r = 1f, g = 1f, b = 1f;
        if (k.style == Fx.HAKI) { r = 0.12f; g = 0.12f; b = 0.14f; }
        else if (k.style == Fx.NIKA) { r = 1f; g = 1f; b = 1f; }
        else if (k.style == Fx.FIRE) { r = 0.3f; g = 0.2f; b = 0.2f; }
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(from.x, from.y, from.z));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(skin.texture()));
        Vec3 end = from.add(d.scale(len));
        // four side faces of the stretched limb
        Vec3[] corners = {s1.scale(w).add(s2.scale(w)), s1.scale(-w).add(s2.scale(w)), s1.scale(-w).add(s2.scale(-w)), s1.scale(w).add(s2.scale(-w))};
        int[] faceOrder = {1, 2, 3, 0}; // front, left, back, right
        for (int f = 0; f < 4; f++) {
            Vec3 c0 = corners[f], c1 = corners[(f + 1) % 4];
            int[] tex = uv[faceOrder[f]];
            float u0 = tex[0] / 64f, u1 = (tex[0] + wpx) / 64f, v0 = tex[1] / 64f, v1 = (tex[1] + hpx) / 64f;
            Vec3 n = c0.add(c1).normalize();
            quad(vc, pose, from.add(c0), from.add(c1), end.add(c1), end.add(c0), u0, v0, u1, v1, n, light, r, g, b);
        }
        // the fist: a cube at the end wearing the hand texture
        float fw = w * (k.style == Fx.HAKI || k.style == Fx.NIKA ? 1.6f : 1.35f);
        Vec3 fc = end.add(d.scale(fw * 0.6));
        int[] hand = uv[4];
        float hu0 = hand[0] / 64f, hu1 = (hand[0] + wpx) / 64f, hv0 = hand[1] / 64f, hv1 = (hand[1] + 4) / 64f;
        Vec3[] fcs = {s1.scale(fw).add(s2.scale(fw)), s1.scale(-fw).add(s2.scale(fw)), s1.scale(-fw).add(s2.scale(-fw)), s1.scale(fw).add(s2.scale(-fw))};
        Vec3 fa = fc.subtract(d.scale(fw)), fb = fc.add(d.scale(fw));
        for (int f = 0; f < 4; f++) {
            Vec3 c0 = fcs[f], c1 = fcs[(f + 1) % 4];
            int[] tex = uv[faceOrder[f]];
            float u0 = tex[0] / 64f, u1 = (tex[0] + wpx) / 64f, v0 = (tex[1] + hpx - 4) / 64f, v1 = (tex[1] + hpx) / 64f;
            quad(vc, pose, fa.add(c0), fa.add(c1), fb.add(c1), fb.add(c0), u0, v0, u1, v1, c0.add(c1).normalize(), light, r, g, b);
        }
        quad(vc, pose, fb.add(fcs[0]), fb.add(fcs[1]), fb.add(fcs[2]), fb.add(fcs[3]), hu0, hv0, hu1, hv1, d, light, r, g, b);
        quad(vc, pose, fa.add(fcs[3]), fa.add(fcs[2]), fa.add(fcs[1]), fa.add(fcs[0]), hu0, hv0, hu1, hv1, d.scale(-1), light, r, g, b);
        buffers.endBatch(RenderType.entityCutoutNoCull(skin.texture()));
        // dressing along the limb
        if (k.age < 1 && pt < 0.5f) {
            List<Vec3> pts = new ArrayList<>();
            for (int i = 0; i <= 8; i++) pts.add(from.lerp(k.to, i / 8.0));
            if (k.style == Fx.HAKI) for (int i = 0; i < 4; i++) { Vec3 q = pts.get(R.nextInt(pts.size())); boltLine(q, q.add(rnd(-0.6, 0.6), rnd(-0.5, 0.6), rnd(-0.6, 0.6)), 0xFF2A3A, i, 0.03); }
            if (k.style == Fx.FIRE) streak(pts, w * 2.2, 0xFF5A1F, 0xFFD36A, k.life, 2, 0.6f, 0.6f, 0, true, false, 3);
            if (k.style == Fx.NIKA) streak(pts, w * 2.0, 0xFFFFFF, 0xFFF7D6, k.life, 2, 0.6f, 0.4f, 0, false, false, 2);
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float v0, float u1, float v1,
                             Vec3 n, int light, float r, float g, float bl) {
        vtxT(vc, pose, a, u0, v0, n, light, r, g, bl);
        vtxT(vc, pose, b, u1, v0, n, light, r, g, bl);
        vtxT(vc, pose, c, u1, v1, n, light, r, g, bl);
        vtxT(vc, pose, d, u0, v1, n, light, r, g, bl);
    }

    private static void vtxT(VertexConsumer vc, PoseStack.Pose pose, Vec3 p, float u, float v, Vec3 n, int light, float r, float g, float b) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, 1f).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }

    private static void drawRibbon(VertexConsumer vc, PoseStack.Pose pose, Ribbon r, float pt) {
        float a0 = Math.max(0, r.age + pt - r.delay);
        if (r.age + pt < r.delay) return;
        float head = Math.min(1f, a0 / Math.max(0.01f, r.reveal)) * 1.15f;
        float fade = a0 < r.reveal ? 1f : Math.max(0f, 1f - (a0 - r.reveal) / Math.max(1f, r.life - r.reveal));
        float flick = r.flicker && R.nextFloat() < 0.3f ? 0.25f : 1f;
        float cut = r.retract && a0 > r.reveal ? Math.min(1f, (a0 - r.reveal) / Math.max(1f, r.life - r.reveal)) : 0f;
        float[] alpha = new float[r.n];
        for (int i = 0; i < r.n; i++) {
            float u = (float) i / (r.n - 1);
            float a = u <= head ? Math.max(0f, 1f - (head - u) / r.tail) : 0f;
            if (r.solid) {
                a = u <= head ? 1f : 0f;
                if (r.retract) { a = u > 1f - cut ? 0f : a; alpha[i] = a * r.alpha; continue; }
                alpha[i] = a * fade * r.alpha;
                continue;
            }
            alpha[i] = a * fade * r.alpha * flick * (0.6f + 0.4f * Mth.sin((float) Math.PI * u));
        }
        for (int i = 0; i < r.n - 1; i++) {
            if (alpha[i] <= 0.003f && alpha[i + 1] <= 0.003f) continue;
            int j = i + 1;
            vtx(vc, pose, r.in, i, r.cIn, alpha[i]);
            vtx(vc, pose, r.out, i, r.cOut, alpha[i] * (r.solid ? 1f : 0.55f));
            vtx(vc, pose, r.out, j, r.cOut, alpha[j] * (r.solid ? 1f : 0.55f));
            vtx(vc, pose, r.in, j, r.cIn, alpha[j]);
        }
    }

    private static void vtx(VertexConsumer vc, PoseStack.Pose pose, double[] xyz, int i, float[] c, float a) {
        vc.addVertex(pose, (float) xyz[i * 3], (float) xyz[i * 3 + 1], (float) xyz[i * 3 + 2]).setColor(c[0], c[1], c[2], Mth.clamp(a, 0f, 1f));
    }

    private static final float[][] FACES = {
            {0, 1, 0, -1, 1, -1, -1, 1, 1, 1, 1, 1, 1, 1, -1},
            {0, -1, 0, -1, -1, -1, 1, -1, -1, 1, -1, 1, -1, -1, 1},
            {0, 0, -1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1, -1},
            {0, 0, 1, -1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1},
            {-1, 0, 0, -1, -1, -1, -1, -1, 1, -1, 1, 1, -1, 1, -1},
            {1, 0, 0, 1, -1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1}};
    private static final float[][] UV = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};

    private static void drawCube(VertexConsumer glow, VertexConsumer solid, PoseStack.Pose pose, Cube k, float pt, int light) {
        float t = Math.min(1f, (k.age + pt) / k.life);
        float size;
        float alpha;
        if (k.cloud) {
            size = k.size * (1 + k.grow * t);
            alpha = k.alpha * Math.min(1f, t / 0.04f) * (t > k.fadeAt ? 1 - (t - k.fadeAt) / (1 - k.fadeAt) : 1f);
        } else {
            size = k.size * (1 + k.grow * t) * (t > 0.6f ? 1 - (t - 0.6f) / 0.4f : 1f);
            alpha = k.glow ? 0.85f : 1f;
        }
        if (size <= 0.001f || alpha <= 0.003f) return;
        float hx = size / 2, hy = k.flat ? size / 8 : (k.cloud && !k.solid ? size * 0.36f : size / 2), hz = size / 2;
        double x = Mth.lerp(pt, k.px, k.x), y = Mth.lerp(pt, k.py, k.y), z = Mth.lerp(pt, k.pz, k.z);
        Quaternionf q = k.cloud ? new Quaternionf().rotationY(k.ry + k.spin * pt)
                : new Quaternionf().rotationXYZ(k.rx + k.spin * 0.6f * pt, k.ry + k.spin * pt, 0);
        Vector3f v = new Vector3f(), nrm = new Vector3f();
        for (float[] f : FACES) {
            nrm.set(f[0], f[1], f[2]).rotate(q);
            float shade = k.solid ? 0.6f + 0.4f * Math.max(0, nrm.y * 0.7f + 0.3f) + 0.1f * Math.abs(nrm.x) : 1f;
            for (int c = 0; c < 4; c++) {
                v.set(f[3 + c * 3] * hx, f[4 + c * 3] * hy, f[5 + c * 3] * hz).rotate(q);
                float px = (float) (x + v.x), py = (float) (y + v.y), pz = (float) (z + v.z);
                if (glow != null) {
                    glow.addVertex(pose, px, py, pz).setColor(k.c[0], k.c[1], k.c[2], alpha);
                } else {
                    solid.addVertex(pose, px, py, pz).setColor(k.c[0] * shade, k.c[1] * shade, k.c[2] * shade, alpha).setUv(UV[c][0], UV[c][1])
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nrm.x, nrm.y, nrm.z);
                }
            }
        }
    }

    private FxClient() {}
}
