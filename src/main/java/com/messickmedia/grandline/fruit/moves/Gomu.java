package com.messickmedia.grandline.fruit.moves;

import com.messickmedia.grandline.entity.FruitProjectile;
import com.messickmedia.grandline.entity.FruitProjectile.Kind;
import com.messickmedia.grandline.fruit.Cast;
import com.messickmedia.grandline.fruit.Gx;
import com.messickmedia.grandline.fruit.Moves;
import com.messickmedia.grandline.fruit.Scheduler;
import com.messickmedia.grandline.fruit.Vfx;
import com.messickmedia.grandline.net.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;


/**
 * Gomu Gomu no Mi. Base rubber moves, Gear Second (jet), Gear Third (gigant/elephant), Gear Fourth (Boundman and
 * Snakeman) and Gear Fifth (the awakened, cartoon-rubber world).
 */
public final class Gomu {
    private Gomu() {}

    // ---------------------------------------------------------------- building blocks

    /** A stretched punch: the arm shoots out to a point, everything along it is hit, the fist lands with an impact. */
    static boolean punch(Cast c, double range, float dmg, double thick, int style, double knock, int life, boolean haki) {
        ServerLevel l = c.level();
        ServerPlayer p = c.p();
        double sc = c.scale();
        Vec3 from = c.hand();
        Vec3 to = Gx.lookPoint(p, range * sc);
        Vfx.arm(l, style, from, to, thick * sc, life);
        boolean hit = false;
        for (LivingEntity e : Gx.line(l, from, to, 0.8 * sc + thick, p)) {
            if (haki) Gx.haki(e, p, dmg); else Gx.hurtNow(e, p, dmg);
            Gx.knock(e, p.position(), knock, 0.25);
            hit = true;
        }
        Vfx.impact(l, style, to);
        Gx.sound(l, to, hit ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, haki ? 0.7F : 1.1F);
        return true;
    }

    /** A circle of rubber limbs around the user (Whip, Dawn Whip). */
    static boolean sweep(Cast c, double radius, float dmg, double knock, int style) {
        ServerLevel l = c.level();
        ServerPlayer p = c.p();
        double sc = c.scale();
        Vec3 at = p.position().add(0, 0.6 * sc, 0);
        Vfx.arc(l, style, at, p.getYRot(), radius * sc * 0.6, 360, 0.15, false, 12);
        Vfx.arm(l, style, p.position().add(0, 0.5 * sc, 0), p.position().add(0, 0.5 * sc, 0).add(c.flat().scale(radius * sc)), 0.22 * sc, 10);
        for (LivingEntity e : Gx.around(l, at, radius * sc, p)) {
            Gx.hurtNow(e, p, dmg);
            Gx.knock(e, p.position(), knock, 0.45);
        }
        Gx.sound(l, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.7F);
        return true;
    }

    static void dash(ServerPlayer p, Vec3 dir, double speed) {
        p.setDeltaMovement(dir.normalize().scale(speed));
        p.hurtMarked = true;
        p.resetFallDistance();
    }

    static FruitProjectile fist(Cast c, Kind kind, float dmg, float speed, float size, Vec3 offset, LivingEntity homing) {
        Vec3 from = c.hand().add(offset);
        FruitProjectile f = FruitProjectile.fire(c.level(), c.p(), kind, dmg, from, c.look(), speed).size(size);
        if (homing != null) f.homing(homing);
        return f;
    }

    // ---------------------------------------------------------------- register

    public static void register() {
        // ---- base
        Moves.register("pistol", c -> punch(c, 9, c.dmg(7), 0.2, Fx.RUBBER, 1.0, 9, false));
        Moves.register("gatling", c -> {
            Scheduler.repeat(12, 2, i -> {
                if (!c.p().isAlive()) return;
                Vec3 look = c.look();
                Vec3 right = c.right();
                Vec3 off = right.scale((i % 2 == 0 ? 0.5 : -0.5) + (c.level().random.nextDouble() - 0.5) * 0.4).add(0, (c.level().random.nextDouble() - 0.5) * 0.8, 0);
                Vec3 from = c.hand().add(off);
                Vec3 to = from.add(look.scale(7 * c.scale())).add(off.scale(1.5));
                to = Gx.rayEnd(c.level(), from, to.subtract(from), 7 * c.scale(), c.p());
                Vfx.arm(c.level(), Fx.RUBBER, from, to, 0.18 * c.scale(), 4);
                for (LivingEntity e : Gx.line(c.level(), from, to, 0.9, c.p())) { Gx.hurtNow(e, c.p(), c.dmg(3)); Gx.knock(e, c.p().position(), 0.25, 0.05); }
                Vfx.impact(c.level(), Fx.RUBBER, to);
                if (i % 3 == 0) Gx.sound(c.level(), from, SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, 1.3F);
            });
            return true;
        });
        Moves.register("bazooka", c -> {
            Vec3 r = c.right().scale(0.35);
            Vec3 to = Gx.lookPoint(c.p(), 6 * c.scale());
            Vfx.arm(c.level(), Fx.RUBBER, c.hand().add(r), to.add(r), 0.22 * c.scale(), 10);
            Vfx.arm(c.level(), Fx.RUBBER, c.hand().subtract(r), to.subtract(r), 0.22 * c.scale(), 10);
            for (LivingEntity e : Gx.cone(c.p(), 6.5 * c.scale(), 0.7)) { Gx.hurtNow(e, c.p(), c.dmg(12)); Gx.knock(e, c.p().position(), 2.6, 0.5); }
            Vfx.ring(c.level(), Fx.RUBBER, to, 1.4, true);
            Vfx.impact(c.level(), Fx.RUBBER, to);
            Gx.sound(c.level(), to, SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 1.6F);
            return true;
        });
        Moves.register("whip", c -> sweep(c, 4.5, c.dmg(8), 1.6, Fx.RUBBER));
        Moves.register("rocket", c -> {
            ServerPlayer p = c.p();
            Vec3 to = Gx.rayEnd(p, 32);
            boolean hitBlock = to.distanceTo(p.getEyePosition()) < 31.5;
            Vfx.arm(c.level(), Fx.RUBBER, c.hand(), to, 0.2 * c.scale(), 14);
            Vec3 d = to.subtract(p.position().add(0, 1, 0));
            double dist = d.length();
            double speed = hitBlock ? Math.min(2.6, 0.6 + dist * 0.12) : 1.5;
            dash(p, d.normalize().add(0, hitBlock ? 0.15 : 0.35, 0), speed);
            Gx.sound(c.level(), p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.5F);
            Vfx.steam(c.level(), p.position(), 6);
            return true;
        });
        Moves.register("balloon", c -> {
            c.s().balloonTicks = 80;
            Vfx.sphere(c.level(), Fx.RUBBER, c.pos().add(0, 0.9, 0), 1.8 * c.scale(), 12);
            Gx.sound(c.level(), c.pos(), SoundEvents.PUFFER_FISH_BLOW_UP, 1.2F, 0.7F);
            for (LivingEntity e : Gx.around(c.level(), c.pos(), 2.5 * c.scale(), c.p())) Gx.knock(e, c.pos(), 1.8, 0.5);
            return true;
        });

        // ---- Gear Second: the jet moves
        Moves.register("jet_pistol", c -> {
            dash(c.p(), c.look(), 0.9);
            Vfx.steam(c.level(), c.pos(), 6);
            Vfx.line(c.level(), Fx.STEAM, c.pos().add(0, 1, 0), c.pos().add(0, 1, 0).subtract(c.look().scale(2)));
            return punch(c, 11, c.dmg(10), 0.2, Fx.RUBBER, 1.6, 6, false);
        });
        Moves.register("jet_gatling", c -> {
            Scheduler.repeat(20, 1, i -> {
                if (!c.p().isAlive()) return;
                Vec3 off = c.right().scale((i % 2 == 0 ? 0.6 : -0.6) + (c.level().random.nextDouble() - 0.5) * 0.5).add(0, (c.level().random.nextDouble() - 0.5) * 1.0, 0);
                Vec3 from = c.hand().add(off);
                Vec3 to = Gx.rayEnd(c.level(), from, c.look().add(off.scale(0.12)), 9 * c.scale(), c.p());
                Vfx.arm(c.level(), Fx.RUBBER, from, to, 0.16 * c.scale(), 3);
                for (LivingEntity e : Gx.line(c.level(), from, to, 0.9, c.p())) { Gx.hurtNow(e, c.p(), c.dmg(3.5F)); Gx.knock(e, c.p().position(), 0.2, 0.05); }
                Vfx.impact(c.level(), Fx.RUBBER, to);
                if (i % 2 == 0) Vfx.steam(c.level(), c.pos(), 2);
                if (i % 4 == 0) Gx.sound(c.level(), from, SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, 1.6F);
            });
            return true;
        });
        Moves.register("jet_bazooka", c -> {
            dash(c.p(), c.look(), 1.2);
            Vfx.steam(c.level(), c.pos(), 8);
            Scheduler.schedule(4, () -> {
                Vec3 r = c.right().scale(0.35);
                Vec3 to = Gx.lookPoint(c.p(), 7 * c.scale());
                Vfx.arm(c.level(), Fx.RUBBER, c.hand().add(r), to.add(r), 0.22 * c.scale(), 8);
                Vfx.arm(c.level(), Fx.RUBBER, c.hand().subtract(r), to.subtract(r), 0.22 * c.scale(), 8);
                for (LivingEntity e : Gx.cone(c.p(), 7.5 * c.scale(), 0.65)) { Gx.hurtNow(e, c.p(), c.dmg(16)); Gx.knock(e, c.p().position(), 3.2, 0.6); }
                Vfx.ring(c.level(), Fx.STEAM, to, 1.8, true);
                Vfx.impact(c.level(), Fx.RUBBER, to);
                Gx.sound(c.level(), to, SoundEvents.GENERIC_EXPLODE.value(), 0.9F, 1.4F);
            });
            return true;
        });
        Moves.register("jet_whip", c -> { Vfx.steam(c.level(), c.pos(), 6); return sweep(c, 5.5, c.dmg(11), 2.2, Fx.RUBBER); });
        Moves.register("jet_stamp", c -> {
            ServerPlayer p = c.p();
            LivingEntity t = Gx.aimAssist(p, 14);
            Vec3 to = t != null ? Gx.chest(t) : Gx.lookPoint(p, 10);
            Vec3 d = to.subtract(p.position().add(0, 1, 0));
            dash(p, d.normalize(), Math.min(2.4, 0.8 + d.length() * 0.15));
            Vfx.steam(c.level(), p.position(), 10);
            Vfx.line(c.level(), Fx.STEAM, p.position().add(0, 1, 0), to);
            Scheduler.schedule(5, () -> {
                Vec3 foot = p.position().add(0, 0.6, 0);
                Vfx.arm(c.level(), Fx.RUBBER, foot, foot.add(c.look().scale(3.5)), 0.26, 8);
                Vfx.impact(c.level(), Fx.RUBBER, foot.add(c.look().scale(3)));
                for (LivingEntity e : Gx.around(c.level(), foot.add(c.look().scale(2)), 2.5 * c.scale(), p)) { Gx.hurtNow(e, p, c.dmg(13)); Gx.knock(e, p.position(), 2.4, 0.4); }
                Gx.sound(c.level(), foot, SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.8F);
            });
            return true;
        });
        Moves.register("red_hawk", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            c.s().hardenTicks = 20;
            Vec3 from = c.hand();
            Vec3 to = Gx.lookPoint(p, 12 * c.scale());
            Vfx.arm(l, Fx.FIRE, from, to, 0.26 * c.scale(), 12);
            Vfx.line(l, Fx.FIRE, from, to);
            for (LivingEntity e : Gx.line(l, from, to, 1.0, p)) { Gx.haki(e, p, c.dmg(14)); e.igniteForSeconds(6); Gx.knock(e, p.position(), 2.0, 0.5); }
            Scheduler.schedule(2, () -> {
                Vfx.blast(l, ParticleTypes.FLAME, to, 2.2);
                Vfx.sphere(l, Fx.FIRE, to, 2.2, 8);
                Vfx.impact(l, Fx.FIRE, to);
                for (LivingEntity e : Gx.around(l, to, 3.0, p)) { Gx.hurtNow(e, p, c.dmg(10)); e.igniteForSeconds(6); Gx.knock(e, to, 1.5, 0.5); }
                Gx.scorch(l, to, 1.8, 3.0F);
                Gx.sound(l, to, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.1F);
            });
            Gx.sound(l, from, SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F);
            return true;
        });

        // ---- Gear Third: bone balloon
        Moves.register("gigant_pistol", c -> {
            Vfx.steam(c.level(), c.pos(), 6);
            fist(c, Kind.GIANT_FIST, c.dmg(20), 1.1F, 1.0F, Vec3.ZERO, null).life(18);
            Gx.sound(c.level(), c.pos(), SoundEvents.PUFFER_FISH_BLOW_UP, 1.0F, 0.5F);
            return true;
        });
        Moves.register("gigant_bazooka", c -> {
            Vec3 r = c.right().scale(1.3);
            fist(c, Kind.GIANT_FIST, c.dmg(18), 1.1F, 0.9F, r, null).life(16);
            fist(c, Kind.GIANT_FIST, c.dmg(18), 1.1F, 0.9F, r.scale(-1), null).life(16);
            Gx.sound(c.level(), c.pos(), SoundEvents.PUFFER_FISH_BLOW_UP, 1.2F, 0.45F);
            return true;
        });
        Moves.register("elephant_gun", c -> {
            c.s().hardenTicks = 30;
            Vfx.bolt(c.level(), Fx.HAKI, c.hand(), c.hand().add(c.right().scale(1.5)));
            fist(c, Kind.ELEPHANT_FIST, c.dmg(28), 1.3F, 1.1F, Vec3.ZERO, null).life(18);
            Gx.sound(c.level(), c.pos(), SoundEvents.PUFFER_FISH_BLOW_UP, 1.2F, 0.4F);
            return true;
        });
        Moves.register("gigant_stamp", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 to = Gx.ground(l, Gx.lookPoint(p, 12));
            Vfx.sphere(l, Fx.RUBBER, to.add(0, 7, 0), 2.6, 6);
            Scheduler.schedule(5, () -> Vfx.sphere(l, Fx.RUBBER, to.add(0, 3.5, 0), 2.6, 5));
            Scheduler.schedule(9, () -> {
                Vfx.sphere(l, Fx.RUBBER, to.add(0, 1.6, 0), 2.6, 10);
                Vfx.slam(l, Fx.RUBBER, to, 5);
                Vfx.ring(l, Fx.RUBBER, to.add(0, 0.2, 0), 5, false);
                for (LivingEntity e : Gx.around(l, to, 5, p)) { Gx.hurtNow(e, p, c.dmg(22)); Gx.knock(e, to, 2.4, 0.9); }
                Gx.crater(l, to, 2.2, 3.0F);
                Gx.sound(l, to, SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.6F);
            });
            return true;
        });
        Moves.register("elephant_gatling", c -> {
            c.s().hardenTicks = 40;
            Scheduler.repeat(6, 4, i -> {
                if (!c.p().isAlive()) return;
                Vec3 off = c.right().scale(i % 2 == 0 ? 1.2 : -1.2).add(0, (i % 3) * 0.4 - 0.4, 0);
                fist(c, Kind.ELEPHANT_FIST, c.dmg(16), 1.4F, 0.8F, off, null).life(16);
                Gx.sound(c.level(), c.pos(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.6F);
            });
            return true;
        });
        Moves.register("grizzly_magnum", c -> {
            c.s().hardenTicks = 40;
            Vfx.steam(c.level(), c.pos(), 14);
            Vec3 r = c.right().scale(1.6);
            fist(c, Kind.ELEPHANT_FIST, c.dmg(30), 1.2F, 1.5F, r, null).life(20);
            fist(c, Kind.ELEPHANT_FIST, c.dmg(30), 1.2F, 1.5F, r.scale(-1), null).life(20);
            Gx.sound(c.level(), c.pos(), SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.5F);
            return true;
        });

        // ---- Gear Fourth: Boundman
        Moves.register("kong_gun", c -> {
            c.s().hardenTicks = 40;
            Vec3 back = c.hand().subtract(c.look().scale(1.2));
            Vfx.puff(c.level(), Fx.STEAM, back, 0.8, 8);
            Vfx.sphere(c.level(), Fx.HAKI, back, 0.7, 8);
            Gx.sound(c.level(), c.pos(), SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.5F);
            Scheduler.schedule(8, () -> {
                if (!c.p().isAlive()) return;
                fist(c, Kind.HAKI_FIST, c.dmg(26), 2.6F, 1.7F, Vec3.ZERO, null).flags(FruitProjectile.FLAG_BOOM).life(14);
                Vfx.ring(c.level(), Fx.STEAM, c.hand(), 1.2, true);
                Gx.sound(c.level(), c.pos(), SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.2F);
            });
            return true;
        });
        Moves.register("rhino_schneider", c -> {
            ServerPlayer p = c.p();
            dash(p, c.look(), 1.6);
            Vfx.steam(c.level(), p.position(), 8);
            for (int k = 0; k < 2; k++) {
                final int kk = k;
                Scheduler.schedule(4 + k * 4, () -> {
                    Vec3 foot = p.position().add(0, 0.7, 0);
                    Vec3 to = foot.add(c.look().scale(4));
                    Vfx.arm(c.level(), Fx.HAKI, foot.add(c.right().scale(kk == 0 ? 0.4 : -0.4)), to, 0.3, 6);
                    Vfx.arc(c.level(), Fx.HAKI, foot.add(c.look()), p.getYRot() + 90, 2.0, 120, kk == 0 ? 0.6 : -0.6, false, 8);
                    for (LivingEntity e : Gx.cone(p, 5, 0.6)) { Gx.haki(e, p, c.dmg(14)); Gx.knock(e, p.position(), 2.0, 0.6); }
                    Gx.sound(c.level(), to, SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.7F);
                });
            }
            return true;
        });
        Moves.register("leo_bazooka", c -> {
            c.s().hardenTicks = 40;
            Vec3 r = c.right().scale(0.6);
            fist(c, Kind.HAKI_FIST, c.dmg(22), 2.2F, 1.5F, r, null).flags(FruitProjectile.FLAG_BOOM).life(16);
            fist(c, Kind.HAKI_FIST, c.dmg(22), 2.2F, 1.5F, r.scale(-1), null).flags(FruitProjectile.FLAG_BOOM).life(16);
            Vfx.puff(c.level(), Fx.STEAM, c.pos().add(0, 1, 0), 1.2, 8);
            Gx.sound(c.level(), c.pos(), SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.0F);
            return true;
        });
        Moves.register("culverin", c -> {
            c.s().hardenTicks = 60;
            LivingEntity t = Gx.aimAssist(c.p(), 30);
            fist(c, Kind.HAKI_FIST, c.dmg(24), 1.3F, 1.5F, c.right().scale(0.8), t).life(90);
            Gx.sound(c.level(), c.pos(), SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.7F);
            return true;
        });
        Moves.register("kong_organ", c -> {
            c.s().hardenTicks = 60;
            Scheduler.repeat(8, 2, i -> {
                if (!c.p().isAlive()) return;
                Vec3 off = c.right().scale((i % 4 - 1.5) * 0.8).add(0, (i / 4) * 0.9 - 0.4, 0);
                fist(c, Kind.HAKI_FIST, c.dmg(16), 2.4F, 1.2F, off, null).flags(FruitProjectile.FLAG_BOOM).life(14);
                Vfx.steam(c.level(), c.pos(), 3);
                if (i % 2 == 0) Gx.sound(c.level(), c.pos(), SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.3F);
            });
            return true;
        });
        Moves.register("king_kong_gun", c -> {
            c.s().hardenTicks = 80;
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            for (int t = 0; t < 20; t += 4) {
                final double r = 0.8 + t * 0.12;
                Scheduler.schedule(t, () -> { Vfx.sphere(l, Fx.HAKI, p.getEyePosition().subtract(p.getLookAngle().scale(1.5)).add(0, -0.5, 0), r, 4); Vfx.steam(l, p.position(), 6); });
            }
            Gx.sound(l, p.position(), SoundEvents.FIRE_EXTINGUISH, 1.4F, 0.4F);
            Scheduler.schedule(20, () -> {
                if (!p.isAlive()) return;
                fist(c, Kind.ELEPHANT_FIST, c.dmg(60), 1.6F, 2.4F, Vec3.ZERO, null).life(24);
                Vfx.ring(l, Fx.HAKI, c.hand(), 3, true);
                Vfx.puff(l, Fx.STEAM, p.position().add(0, 1, 0), 2.5, 20);
                Gx.sound(l, p.position(), SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.7F);
            });
            return true;
        });

        // ---- Gear Fourth: Snakeman
        Moves.register("snake_pistol", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 from = c.hand();
            Vec3 to = Gx.lookPoint(p, 14);
            Vec3 mid = from.lerp(to, 0.5).add(c.right().scale(1.8)).add(0, 0.6, 0);
            Vfx.arm(l, Fx.HAKI, from, mid, 0.2, 5);
            Scheduler.schedule(2, () -> {
                Vfx.arm(l, Fx.HAKI, mid, to, 0.2, 6);
                for (LivingEntity e : Gx.line(l, mid, to, 1.0, p)) { Gx.haki(e, p, c.dmg(10)); Gx.knock(e, p.position(), 1.2, 0.3); }
                Vfx.impact(l, Fx.HAKI, to);
                Gx.sound(l, to, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.4F);
            });
            return true;
        });
        Moves.register("jet_culverin", c -> {
            c.s().hardenTicks = 60;
            LivingEntity t = Gx.aimAssist(c.p(), 30);
            fist(c, Kind.HAKI_FIST, c.dmg(20), 2.2F, 1.3F, c.right().scale(0.8), t).flags(FruitProjectile.FLAG_PIERCE).life(80);
            return true;
        });
        Moves.register("python", c -> {
            c.s().hardenTicks = 60;
            LivingEntity t = Gx.aimAssist(c.p(), 30);
            Scheduler.repeat(3, 4, i -> {
                if (!c.p().isAlive()) return;
                Vec3 off = c.right().scale(i == 0 ? 1.5 : i == 1 ? -1.5 : 0).add(0, i == 2 ? 1.2 : 0, 0);
                fist(c, Kind.HAKI_FIST, c.dmg(16), 1.6F, 1.3F, off, t).life(80);
            });
            return true;
        });
        Moves.register("hydra", c -> {
            c.s().hardenTicks = 60;
            LivingEntity t = Gx.aimAssist(c.p(), 30);
            for (int i = 0; i < 3; i++) {
                Vec3 off = c.right().scale((i - 1) * 1.6).add(0, i == 1 ? 1.0 : 0, 0);
                fist(c, Kind.HAKI_FIST, c.dmg(18), 1.8F, 1.3F, off, t).flags(FruitProjectile.FLAG_PIERCE).life(90);
            }
            Vfx.puff(c.level(), Fx.STEAM, c.pos().add(0, 1, 0), 1.4, 10);
            return true;
        });
        Moves.register("black_mamba", c -> {
            c.s().hardenTicks = 60;
            LivingEntity t = Gx.aimAssist(c.p(), 30);
            Scheduler.repeat(16, 1, i -> {
                if (!c.p().isAlive()) return;
                Vec3 off = c.right().scale((c.level().random.nextDouble() - 0.5) * 3).add(0, (c.level().random.nextDouble() - 0.5) * 2, 0);
                fist(c, Kind.HAKI_FIST, c.dmg(6), 2.6F, 0.9F, off, t).life(60);
                if (i % 3 == 0) Gx.sound(c.level(), c.pos(), SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, 1.6F);
            });
            return true;
        });
        Moves.register("king_cobra", c -> {
            c.s().hardenTicks = 80;
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            LivingEntity t = Gx.aimAssist(p, 40);
            for (int k = 0; k < 16; k += 4) {
                final double r = 0.6 + k * 0.08;
                Scheduler.schedule(k, () -> { Vfx.sphere(l, Fx.HAKI, p.getEyePosition().add(p.getLookAngle().scale(-1.2)), r, 4); Vfx.steam(l, p.position(), 5); });
            }
            Scheduler.schedule(16, () -> {
                if (!p.isAlive()) return;
                FruitProjectile f = fist(c, Kind.ELEPHANT_FIST, c.dmg(50), 1.5F, 1.6F, Vec3.ZERO, t).life(60);
                f.flags(FruitProjectile.FLAG_FIRE);
                Vfx.ring(l, Fx.FIRE, c.hand(), 2.0, true);
                Gx.sound(l, p.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 0.8F);
            });
            return true;
        });

        // ---- Gear Fifth: the Sun God's toy world
        Moves.register("gigant", c -> {
            c.s().gigantTicks = 260;
            ServerLevel l = c.level();
            Vfx.sphere(l, Fx.NIKA, c.pos().add(0, 1.5, 0), 3.5, 14);
            Vfx.ring(l, Fx.NIKA, c.pos().add(0, 0.2, 0), 6, false);
            Vfx.puff(l, Fx.NIKA, c.pos().add(0, 2, 0), 2.5, 20);
            Gx.sound(l, c.pos(), SoundEvents.PUFFER_FISH_BLOW_UP, 1.5F, 0.3F);
            Gx.sound(l, c.pos(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 2.0F, 0.5F);
            for (LivingEntity e : Gx.around(l, c.pos(), 5, c.p())) Gx.knock(e, c.pos(), 2.0, 0.6);
            return true;
        });
        Moves.register("dawn_whip", c -> sweep(c, 9, c.dmg(22), 3.2, Fx.NIKA));
        Moves.register("dawn_rocket", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            LivingEntity t = Gx.aimAssist(p, 24);
            Vec3 to = t != null ? Gx.chest(t) : Gx.lookPoint(p, 20);
            Vec3 d = to.subtract(p.position().add(0, 1, 0));
            Vfx.arm(l, Fx.NIKA, c.hand(), to, 0.35 * c.scale(), 10);
            dash(p, d.normalize(), Math.min(3.2, 1.0 + d.length() * 0.14));
            Vfx.puff(l, Fx.NIKA, p.position(), 1.2, 8);
            Scheduler.schedule(6, () -> {
                Vec3 at = p.position().add(0, 0.8, 0);
                Vfx.impact(l, Fx.NIKA, at);
                Vfx.ring(l, Fx.NIKA, at, 5, false);
                Vfx.ring(l, Fx.NIKA, at, 4, true);
                Vfx.slam(l, Fx.NIKA, Gx.ground(l, at), 5);
                for (LivingEntity e : Gx.around(l, at, 5.5, p)) { Gx.haki(e, p, c.dmg(26)); Gx.knock(e, at, 2.8, 1.0); }
                Gx.crater(l, Gx.ground(l, at), 2.5, 5.0F);
                Gx.sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 0.8F);
            });
            return true;
        });
        Moves.register("fusen", c -> {
            c.s().balloonTicks = 120;
            ServerLevel l = c.level();
            Vfx.sphere(l, Fx.NIKA, c.pos().add(0, 1.2, 0), 4.0 * c.scale(), 14);
            Gx.sound(l, c.pos(), SoundEvents.PUFFER_FISH_BLOW_UP, 1.5F, 0.4F);
            for (LivingEntity e : Gx.around(l, c.pos(), 6 * c.scale(), c.p())) { Gx.knock(e, c.pos(), 3.0, 1.0); Gx.hurtNow(e, c.p(), c.dmg(8)); }
            return true;
        });
        Moves.register("rubber_world", c -> {
            c.s().rubberWorldTicks = 240;
            ServerLevel l = c.level();
            Vfx.ring(l, Fx.NIKA, c.pos().add(0, 0.1, 0), 10, false);
            Vfx.puff(l, Fx.NIKA, c.pos().add(0, 0.5, 0), 3, 16);
            Gx.sound(l, c.pos(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 2.0F, 0.6F);
            Gx.sound(l, c.pos(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.5F);
            return true;
        });
        Moves.register("bajrang_gun", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            c.s().hardenTicks = 120;
            for (int t = 0; t < 30; t += 3) {
                final double r = 1.5 + t * 0.35;
                Scheduler.schedule(t, () -> {
                    Vec3 at = p.getEyePosition().add(0, 4 + r * 0.5, 0).subtract(p.getLookAngle().scale(2));
                    Vfx.sphere(l, Fx.NIKA, at, r, 4);
                    Vfx.puff(l, Fx.NIKA, at, r * 0.6, 3);
                    Vfx.bolt(l, Fx.HAKI, at.add(Vfx.randDir(l).scale(r)), at.add(Vfx.randDir(l).scale(r)));
                });
            }
            Gx.sound(l, p.position(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 2.0F, 0.4F);
            Gx.sound(l, p.position(), SoundEvents.FIRE_EXTINGUISH, 1.6F, 0.3F);
            Scheduler.schedule(30, () -> {
                if (!p.isAlive()) return;
                Vec3 from = p.getEyePosition().add(0, 6, 0).subtract(p.getLookAngle().scale(2));
                FruitProjectile.fire(l, p, Kind.NIKA_FIST, c.dmg(120), from, p.getLookAngle(), 1.3F).size(2.0F).life(40);
                Vfx.ring(l, Fx.NIKA, from, 8, true);
                Gx.sound(l, p.position(), SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.5F);
                Gx.sound(l, p.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5F, 0.6F);
            });
            return true;
        });
    }
}
