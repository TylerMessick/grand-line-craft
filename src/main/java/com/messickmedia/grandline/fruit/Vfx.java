package com.messickmedia.grandline.fruit;

import com.messickmedia.grandline.net.Fx;
import com.messickmedia.grandline.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side effect choreography. The 3D primitives (arms, beams, arcs, clouds) go out as Fx packets and are drawn by
 * FxClient; vanilla particles fill in sparks, debris and smoke.
 */
public final class Vfx {
    private Vfx() {}

    // ---------------------------------------------------------------- 3D primitives

    public static void arm(ServerLevel l, int style, Vec3 a, Vec3 b, double thick, int life) {
        Fx.send(l, Fx.ARM, style, (float) a.x, (float) a.y, (float) a.z, (float) b.x, (float) b.y, (float) b.z, (float) thick, life);
    }

    public static void beam(ServerLevel l, int style, Vec3 a, Vec3 b, double radius, int life) {
        Fx.send(l, Fx.BEAM, style, (float) a.x, (float) a.y, (float) a.z, (float) b.x, (float) b.y, (float) b.z, (float) radius, life);
    }

    public static void arc(ServerLevel l, int style, Vec3 c, double yawDeg, double radius, double arcDeg, double tilt, boolean light, int life) {
        Fx.send(l, Fx.ARC, style, (float) c.x, (float) c.y, (float) c.z, (float) yawDeg, (float) radius, (float) arcDeg, (float) tilt, light ? 1 : 0, life);
    }

    public static void ring(ServerLevel l, int style, Vec3 c, double radius, boolean vertical) {
        Fx.send(l, Fx.RING, style, (float) c.x, (float) c.y, (float) c.z, (float) radius, vertical ? 1 : 0);
    }

    public static void line(ServerLevel l, int style, Vec3 a, Vec3 b) {
        Fx.send(l, Fx.LINE, style, (float) a.x, (float) a.y, (float) a.z, (float) b.x, (float) b.y, (float) b.z);
    }

    public static void impact(ServerLevel l, int style, Vec3 p) {
        Fx.send(l, Fx.IMPACT, style, (float) p.x, (float) p.y, (float) p.z);
    }

    public static void slam(ServerLevel l, int style, Vec3 p, double radius) {
        Fx.send(l, Fx.SLAM, style, (float) p.x, (float) p.y, (float) p.z, (float) radius);
    }

    public static void dress(ServerLevel l, int style, Vec3 p, double radius, double amount) {
        Fx.send(l, Fx.DRESS, style, (float) p.x, (float) p.y, (float) p.z, (float) radius, (float) amount);
    }

    public static void puff(ServerLevel l, int style, Vec3 p, double radius, double amount) {
        Fx.send(l, Fx.PUFF, style, (float) p.x, (float) p.y, (float) p.z, (float) radius, (float) amount);
    }

    public static void bolt(ServerLevel l, int style, Vec3 a, Vec3 b) {
        Fx.send(l, Fx.BOLT, style, (float) a.x, (float) a.y, (float) a.z, (float) b.x, (float) b.y, (float) b.z);
    }

    public static void sphere(ServerLevel l, int style, Vec3 c, double radius, int life) {
        Fx.send(l, Fx.SPHERE, style, (float) c.x, (float) c.y, (float) c.z, (float) radius, life);
    }

    // ---------------------------------------------------------------- vanilla particles

    public static Vec3 randDir(ServerLevel level) {
        double u = level.random.nextDouble() * 2.0D - 1.0D;
        double a = level.random.nextDouble() * Math.PI * 2.0D;
        double r = Math.sqrt(1.0D - u * u);
        return new Vec3(r * Math.cos(a), u, r * Math.sin(a));
    }

    public static void dot(ServerLevel level, ParticleOptions type, Vec3 p, Vec3 v, double speed) {
        level.sendParticles(type, p.x, p.y, p.z, 0, v.x, v.y, v.z, speed);
    }

    public static void burst(ServerLevel level, ParticleOptions type, Vec3 c, int count, double speed) {
        for (int i = 0; i < count; i++) dot(level, type, c, randDir(level), speed * (0.4D + level.random.nextDouble() * 0.7D));
    }

    public static void ringP(ServerLevel level, ParticleOptions type, Vec3 c, double r, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double a = i / (double) count * Math.PI * 2.0D;
            Vec3 d = new Vec3(Math.cos(a), 0.02D, Math.sin(a));
            dot(level, type, c.add(d.scale(r)), d, speed);
        }
    }

    public static void lineP(ServerLevel level, ParticleOptions type, Vec3 from, Vec3 to, double step) {
        Vec3 d = to.subtract(from);
        int n = Math.max(1, (int) (d.length() / step));
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(d.scale(i / (double) n));
            level.sendParticles(type, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    public static void sparks(ServerLevel level, Vec3 c, int count, double speed) {
        for (int i = 0; i < count; i++) dot(level, ModRegistries.SPARK.get(), c, randDir(level), speed * (0.5D + level.random.nextDouble()));
        level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, count, 0.1D, 0.1D, 0.1D, speed);
    }

    /** Debris: block particles made from whatever the ground at that spot is made of. */
    public static void debris(ServerLevel level, Vec3 at, int count, double spread, double speed) {
        BlockPos below = BlockPos.containing(at.x, at.y - 0.5D, at.z);
        BlockState state = level.getBlockState(below);
        if (state.isAir()) state = level.getBlockState(below.below());
        if (state.isAir()) return;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y + 0.2D, at.z, count, spread, 0.3D, spread, speed);
    }

    /** Explosion dressing: flash, embers, smoke and debris. */
    public static void blast(ServerLevel level, ParticleOptions glow, Vec3 c, double r) {
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        burst(level, glow, c, (int) (30 * r), 0.25D * r);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y, c.z, (int) (6 * r), r * 0.4D, r * 0.3D, r * 0.4D, 0.03D);
        debris(level, c, (int) (24 * r), r * 0.5D, 0.4D);
        sparks(level, c, (int) (8 * r), 0.6D);
    }

    public static void steam(ServerLevel level, Vec3 c, int count) {
        for (int i = 0; i < count; i++) {
            Vec3 d = randDir(level);
            level.sendParticles(ParticleTypes.CLOUD, c.x + d.x * 0.4D, c.y + 0.6D + d.y * 0.6D, c.z + d.z * 0.4D, 0, d.x * 0.3D, 0.15D + level.random.nextDouble() * 0.1D, d.z * 0.3D, 0.05D);
        }
    }
}
