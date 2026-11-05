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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Gomu Gomu no Mi. Every stretched limb is the player's own arm or leg, drawn with their skin, shooting out to the
 * target and snapping back. Base rubber moves, Gear Second (jet), Gear Third (gigant/elephant), Gear Fourth
 * (Boundman and Snakeman) and Gear Fifth (the awakened, cartoon-rubber world).
 */
public final class Gomu {
    private Gomu() {}

    static final int R_ARM = 0, L_ARM = 1, R_LEG = 2, L_LEG = 3;
    static final double ARM = 0.125, LEG = 0.125, GIGANT = 0.42, HAKI_ARM = 0.2, NIKA_ARM = 0.26;

    // ---------------------------------------------------------------- building blocks

    /** A stretched strike: the limb shoots to a point, everything along the way is hit, the fist lands with an impact. */
    static boolean strike(Cast c, int limb, double range, float dmg, double thick, int style, double knock, int life, boolean haki) {
        ServerLevel l = c.level();
        ServerPlayer p = c.p();
        double sc = c.scale();
        Vec3 from = limb < 2 ? c.hand() : p.position().add(0, 0.6 * sc, 0);
        Vec3 to = Gx.lookPoint(p, range * sc);
        Vfx.limb(l, p, limb, to, thick * sc, life, style);
        boolean hit = false;
        for (LivingEntity e : Gx.line(l, from, to, 0.8 * sc + thick * 3, p)) {
            if (haki) Gx.haki(e, p, dmg); else Gx.hurtNow(e, p, dmg);
            Gx.knock(e, p.position(), knock, 0.25);
            hit = true;
        }
        Vfx.impact(l, style, to);
        Gx.sound(l, to, hit ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, haki ? 0.7F : 1.1F);
        return true;
    }

    /** A rapid barrage of alternating punches in a loose cone (Gatling and its gears). */
    static void barrage(Cast c, int count, int every, double range, float dmg, double thick, int style, double spread, boolean haki) {
        Scheduler.repeat(count, every, i -> {
            ServerPlayer p = c.p();
            if (!p.isAlive()) return;
            ServerLevel l = c.level();
            Vec3 off = c.right().scale((i % 2 == 0 ? spread : -spread) + (l.random.nextDouble() - 0.5) * spread).add(0, (l.random.nextDouble() - 0.5) * spread * 1.6, 0);
            Vec3 from = c.hand().add(off);
            Vec3 to = Gx.rayEnd(l, from, p.getLookAngle().add(off.scale(0.12)), range * c.scale(), p);
            Vfx.limb(l, p, i % 2 == 0 ? R_ARM : L_ARM, to, thick * c.scale(), every + 2, style);
            for (LivingEntity e : Gx.line(l, from, to, 0.9 + thick * 2, p)) { if (haki) Gx.haki(e, p, dmg); else Gx.hurtNow(e, p, dmg); Gx.knock(e, p.position(), 0.25, 0.05); }
            Vfx.impact(l, style, to);
            if (i % 3 == 0) Gx.sound(l, from, SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, haki ? 0.9F : 1.3F);
            if (style == Fx.STEAM || c.s().inForm("gear_second")) Vfx.steam(l, p.position(), 1);
        });
    }

    /** Both arms drawn back then slammed forward together (Bazooka and its gears). */
    static boolean bazooka(Cast c, double range, float dmg, double thick, int style, double knock, boolean haki, int charge) {
        ServerPlayer p = c.p();
        ServerLevel l = c.level();
        Vec3 back = c.hand().subtract(c.look().scale(1.5));
        Vfx.limb(l, p, R_ARM, back.add(c.right().scale(0.4)), thick * c.scale(), charge + 2, style);
        Vfx.limb(l, p, L_ARM, back.subtract(c.right().scale(0.4)), thick * c.scale(), charge + 2, style);
        Scheduler.schedule(charge, () -> {
            if (!p.isAlive()) return;
            Vec3 r = c.right().scale(0.35 * c.scale());
            Vec3 to = Gx.lookPoint(p, range * c.scale());
            Vfx.limb(l, p, R_ARM, to.add(r), thick * c.scale(), 10, style);
            Vfx.limb(l, p, L_ARM, to.subtract(r), thick * c.scale(), 10, style);
            for (LivingEntity e : Gx.cone(p, (range + 0.5) * c.scale(), 0.65)) { if (haki) Gx.haki(e, p, dmg); else Gx.hurtNow(e, p, dmg); Gx.knock(e, p.position(), knock, 0.5); }
            Vfx.ring(l, style, to, 1.4 + thick * 3, true);
            Vfx.impact(l, style, to);
            Gx.sound(l, to, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, haki ? 1.0F : 1.6F);
        });
        return true;
    }

    /** A circle kick around the user (Whip and its gears). */
    static boolean sweep(Cast c, double radius, float dmg, double knock, int style, double thick) {
        ServerLevel l = c.level();
        ServerPlayer p = c.p();
        double sc = c.scale();
        Vec3 at = p.position().add(0, 0.6 * sc, 0);
        Vfx.arc(l, style, at, p.getYRot(), radius * sc * 0.6, 360, 0.15, false, 12);
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(p.getYRot() + i * 90);
            Vfx.limb(l, p, R_LEG, at.add(-Math.sin(a) * radius * sc, 0, Math.cos(a) * radius * sc), thick * sc, 4 + i, style);
        }
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

    /** A dash that lands a kick (Stamp, Spear and the jet versions). */
    static boolean dashKick(Cast c, double range, float dmg, double knock, int style, double thick, boolean twoLegs, int delay) {
        ServerPlayer p = c.p();
        ServerLevel l = c.level();
        LivingEntity t = Gx.aimAssist(p, range);
        Vec3 to = t != null ? Gx.chest(t) : Gx.lookPoint(p, range * 0.7);
        Vec3 d = to.subtract(p.position().add(0, 1, 0));
        dash(p, d.normalize(), Math.min(2.4, 0.8 + d.length() * 0.15));
        if (style == Fx.STEAM) Vfx.steam(l, p.position(), 10);
        Scheduler.schedule(delay, () -> {
            if (!p.isAlive()) return;
            Vec3 foot = p.position().add(0, 0.6 * c.scale(), 0);
            Vec3 end = Gx.rayEnd(l, foot, p.getLookAngle(), 3.5 * c.scale(), p);
            Vfx.limb(l, p, R_LEG, end, thick * c.scale(), 8, style);
            if (twoLegs) Vfx.limb(l, p, L_LEG, end.add(c.right().scale(0.3)), thick * c.scale(), 8, style);
            Vfx.impact(l, style, end);
            for (LivingEntity e : Gx.around(l, foot.add(p.getLookAngle().scale(2)), 2.5 * c.scale(), p)) { Gx.hurtNow(e, p, dmg); Gx.knock(e, p.position(), knock, 0.4); }
            Gx.sound(l, foot, SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.8F);
        });
        return true;
    }

    /** Grapple: the arm shoots to a block and the body follows (Rocket and Jet Rocket). */
    static boolean rocket(Cast c, double range, double speedMul) {
        ServerPlayer p = c.p();
        Vec3 to = Gx.rayEnd(p, range);
        boolean hitBlock = to.distanceTo(p.getEyePosition()) < range - 0.5;
        Vfx.limb(c.level(), p, R_ARM, to, ARM * c.scale(), 14, Fx.RUBBER);
        Vfx.limb(c.level(), p, L_ARM, to.add(c.right().scale(0.3)), ARM * c.scale(), 14, Fx.RUBBER);
        Vec3 d = to.subtract(p.position().add(0, 1, 0));
        double dist = d.length();
        double speed = (hitBlock ? Math.min(2.6, 0.6 + dist * 0.12) : 1.5) * speedMul;
        dash(p, d.normalize().add(0, hitBlock ? 0.15 : 0.35, 0), speed);
        Gx.sound(c.level(), p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.5F);
        Vfx.steam(c.level(), p.position(), 6);
        return true;
    }

    /** The arm twists (spins) before the punch: Rifle and its gears deal more and spin the target. */
    static boolean rifle(Cast c, double range, float dmg, double thick, int style, boolean haki) {
        ServerLevel l = c.level();
        ServerPlayer p = c.p();
        // wind-up: the arm coils around the body
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(p.getYRot() + 90 + i * 90);
            Vfx.limb(l, p, R_ARM, p.position().add(0, 1.2, 0).add(-Math.sin(a) * 0.9, 0, Math.cos(a) * 0.9), thick * c.scale() * 0.8, 3, style);
        }
        Scheduler.schedule(6, () -> {
            if (!p.isAlive()) return;
            Vec3 from = c.hand();
            Vec3 to = Gx.lookPoint(p, range * c.scale());
            Vfx.limb(l, p, R_ARM, to, thick * c.scale(), 10, style);
            Vfx.arc(l, style, to, p.getYRot(), 1.0, 360, 0.2, true, 8);
            for (LivingEntity e : Gx.line(l, from, to, 0.9 + thick * 3, p)) {
                if (haki) Gx.haki(e, p, dmg); else Gx.hurtNow(e, p, dmg);
                Gx.knock(e, p.position(), 1.6, 0.4);
                Gx.effect(e, MobEffects.CONFUSION, 100, 0);
                e.setYRot(e.getYRot() + 180);
            }
            Vfx.impact(l, style, to);
            Gx.sound(l, to, SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.6F);
        });
        return true;
    }

    // ---------------------------------------------------------------- register

    public static void register() {
        // ---- base rubber
        Moves.register("pistol", c -> strike(c, R_ARM, 9, c.dmg(7), ARM, Fx.RUBBER, 1.0, 9, false));
        Moves.register("gatling", c -> { barrage(c, 12, 2, 7, c.dmg(3), ARM, Fx.RUBBER, 0.5, false); return true; });
        Moves.register("bazooka", c -> bazooka(c, 6, c.dmg(14), ARM, Fx.RUBBER, 2.6, false, 8));
        Moves.register("whip", c -> sweep(c, 4.5, c.dmg(8), 1.6, Fx.RUBBER, LEG));
        Moves.register("rocket", c -> rocket(c, 32, 1.0));
        Moves.register("balloon", c -> {
            c.s().balloonTicks = 80;
            Gx.sound(c.level(), c.pos(), SoundEvents.PUFFER_FISH_BLOW_UP, 1.2F, 0.7F);
            for (LivingEntity e : Gx.around(c.level(), c.pos(), 2.5 * c.scale(), c.p())) Gx.knock(e, c.pos(), 1.8, 0.5);
            return true;
        });
        Moves.register("stamp", c -> strike(c, R_LEG, 8, c.dmg(9), LEG, Fx.RUBBER, 2.2, 9, false));
        Moves.register("spear", c -> dashKick(c, 10, c.dmg(11), 1.8, Fx.RUBBER, LEG, true, 5));
        Moves.register("rifle", c -> rifle(c, 9, c.dmg(12), ARM, Fx.RUBBER, false));
        Moves.register("bullet", c -> {
            // a short, fast, close-range punch that hits hard
            dash(c.p(), c.look(), 0.6);
            return strike(c, R_ARM, 4, c.dmg(13), ARM, Fx.RUBBER, 2.0, 6, false);
        });
        Moves.register("bell", c -> {
            // headbutt: the neck stretches back then snaps forward
            ServerPlayer p = c.p();
            ServerLevel l = c.level();
            Vec3 to = Gx.lookPoint(p, 6 * c.scale());
            Vfx.sphere(l, Fx.RUBBER, p.getEyePosition().subtract(p.getLookAngle().scale(1.2)), 0.35, 5);
            Scheduler.schedule(5, () -> {
                if (!p.isAlive()) return;
                Vfx.sphere(l, Fx.RUBBER, to, 0.45 * c.scale(), 6);
                Vfx.impact(l, Fx.RUBBER, to);
                for (LivingEntity e : Gx.line(l, p.getEyePosition(), to, 0.9, p)) { Gx.hurtNow(e, p, c.dmg(14)); Gx.knock(e, p.position(), 2.2, 0.5); Gx.effect(e, MobEffects.CONFUSION, 60, 0); }
                Gx.sound(l, to, SoundEvents.PLAYER_ATTACK_STRONG, 1.3F, 0.6F);
            });
            return true;
        });
        Moves.register("storm", c -> {
            // Gomu Gomu no Storm: spin into the air and rain punches down
            ServerPlayer p = c.p();
            p.setDeltaMovement(c.look().multiply(0.4, 0, 0.4).add(0, 1.1, 0));
            p.hurtMarked = true;
            Vfx.ring(c.level(), Fx.STEAM, p.position().add(0, 0.5, 0), 2, false);
            Scheduler.schedule(8, () -> barrage(c, 18, 1, 9, c.dmg(4), ARM, Fx.RUBBER, 1.2, false));
            return true;
        });

        // ---- Gear Second: the jet moves
        Moves.register("jet_pistol", c -> {
            dash(c.p(), c.look(), 0.9);
            Vfx.steam(c.level(), c.pos(), 6);
            Vfx.line(c.level(), Fx.STEAM, c.pos().add(0, 1, 0), c.pos().add(0, 1, 0).subtract(c.look().scale(2)));
            return strike(c, R_ARM, 11, c.dmg(12), ARM, Fx.RUBBER, 1.6, 6, false);
        });
        Moves.register("jet_gatling", c -> { barrage(c, 20, 1, 9, c.dmg(4), ARM, Fx.STEAM, 0.6, false); return true; });
        Moves.register("jet_bazooka", c -> { dash(c.p(), c.look(), 1.2); Vfx.steam(c.level(), c.pos(), 8); return bazooka(c, 7, c.dmg(22), ARM, Fx.STEAM, 3.2, false, 4); });
        Moves.register("jet_whip", c -> { Vfx.steam(c.level(), c.pos(), 6); return sweep(c, 5.5, c.dmg(11), 2.2, Fx.STEAM, LEG); });
        Moves.register("jet_stamp", c -> dashKick(c, 14, c.dmg(13), 2.4, Fx.STEAM, LEG, false, 5));
        Moves.register("red_hawk", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            c.s().hardenTicks = 20;
            Vec3 from = c.hand();
            Vec3 to = Gx.lookPoint(p, 12 * c.scale());
            Vfx.limb(l, p, R_ARM, to, HAKI_ARM * c.scale(), 12, Fx.FIRE);
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
        Moves.register("jet_spear", c -> dashKick(c, 14, c.dmg(16), 2.6, Fx.STEAM, LEG, true, 4));
        Moves.register("jet_rifle", c -> { Vfx.steam(c.level(), c.pos(), 6); return rifle(c, 11, c.dmg(18), ARM, Fx.STEAM, false); });
        Moves.register("jet_bullet", c -> { dash(c.p(), c.look(), 1.0); Vfx.steam(c.level(), c.pos(), 5); return strike(c, R_ARM, 5, c.dmg(19), ARM, Fx.STEAM, 2.4, 5, false); });
        Moves.register("jet_rocket", c -> rocket(c, 40, 1.5));
        Moves.register("jet_hammer", c -> {
            // both legs grab the target and hammer it into the ground
            ServerPlayer p = c.p();
            ServerLevel l = c.level();
            LivingEntity t = Gx.aimAssist(p, 12);
            if (t == null) return false;
            Vfx.limb(l, p, R_LEG, Gx.chest(t), LEG, 14, Fx.STEAM);
            Vfx.limb(l, p, L_LEG, Gx.chest(t).add(c.right().scale(0.4)), LEG, 14, Fx.STEAM);
            Gx.root(t, 14);
            t.setDeltaMovement(0, 0.9, 0);
            t.hurtMarked = true;
            Scheduler.schedule(10, () -> {
                if (!t.isAlive()) return;
                t.setDeltaMovement(0, -2.5, 0);
                t.hurtMarked = true;
                Gx.hurtNow(t, p, c.dmg(16));
            });
            Scheduler.schedule(14, () -> {
                Vec3 at = Gx.ground(l, t.position());
                Gx.hurtNow(t, p, c.dmg(10));
                Vfx.slam(l, Fx.STEAM, at, 3);
                Vfx.impact(l, Fx.STEAM, at.add(0, 0.5, 0));
                Gx.crater(l, at, 1.5, 3.0F);
                Gx.sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.2F);
            });
            return true;
        });
        Moves.register("jet_cannon", c -> {
            // two legs, one dash, one cannon kick with a shockwave
            ServerPlayer p = c.p();
            dash(p, c.look(), 1.6);
            Vfx.steam(c.level(), p.position(), 12);
            Scheduler.schedule(4, () -> {
                if (!p.isAlive()) return;
                Vec3 to = Gx.lookPoint(p, 6 * c.scale());
                Vfx.limb(c.level(), p, R_LEG, to, LEG * c.scale(), 10, Fx.STEAM);
                Vfx.limb(c.level(), p, L_LEG, to.add(c.right().scale(0.35)), LEG * c.scale(), 10, Fx.STEAM);
                Vfx.ring(c.level(), Fx.STEAM, to, 2.5, true);
                Vfx.impact(c.level(), Fx.STEAM, to);
                for (LivingEntity e : Gx.cone(p, 7 * c.scale(), 0.6)) { Gx.hurtNow(e, p, c.dmg(26)); Gx.knock(e, p.position(), 3.4, 0.7); }
                Gx.sound(c.level(), to, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.3F);
            });
            return true;
        });

        // ---- Gear Third: bone balloon
        Moves.register("gigant_pistol", c -> {
            Vfx.steam(c.level(), c.pos(), 6);
            Gx.sound(c.level(), c.pos(), SoundEvents.PUFFER_FISH_BLOW_UP, 1.0F, 0.5F);
            return strike(c, R_ARM, 10, c.dmg(22), GIGANT, Fx.RUBBER, 2.6, 14, false);
        });
        Moves.register("gigant_bazooka", c -> bazooka(c, 8, c.dmg(34), GIGANT, Fx.RUBBER, 3.4, false, 8));
        Moves.register("elephant_gun", c -> {
            c.s().hardenTicks = 30;
            Vfx.bolt(c.level(), Fx.HAKI, c.hand(), c.hand().add(c.right().scale(1.5)));
            Gx.sound(c.level(), c.pos(), SoundEvents.PUFFER_FISH_BLOW_UP, 1.2F, 0.4F);
            return strike(c, R_ARM, 11, c.dmg(30), GIGANT, Fx.HAKI, 3.0, 14, true);
        });
        Moves.register("gigant_stamp", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 to = Gx.ground(l, Gx.lookPoint(p, 12));
            Vfx.limb(l, p, R_LEG, to.add(0, 1.2, 0), GIGANT * c.scale(), 14, Fx.RUBBER);
            Scheduler.schedule(5, () -> {
                Vfx.slam(l, Fx.RUBBER, to, 5);
                Vfx.ring(l, Fx.RUBBER, to.add(0, 0.2, 0), 5, false);
                for (LivingEntity e : Gx.around(l, to, 5, p)) { Gx.hurtNow(e, p, c.dmg(24)); Gx.knock(e, to, 2.4, 0.9); }
                Gx.crater(l, to, 2.2, 3.0F);
                Gx.sound(l, to, SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.6F);
            });
            return true;
        });
        Moves.register("elephant_gatling", c -> { c.s().hardenTicks = 40; barrage(c, 8, 3, 10, c.dmg(14), GIGANT, Fx.HAKI, 1.4, true); return true; });
        Moves.register("grizzly_magnum", c -> {
            c.s().hardenTicks = 40;
            Vfx.steam(c.level(), c.pos(), 14);
            Gx.sound(c.level(), c.pos(), SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.5F);
            return bazooka(c, 9, c.dmg(60), GIGANT * 1.3, Fx.HAKI, 4.0, true, 10);
        });
        Moves.register("gigant_rifle", c -> rifle(c, 10, c.dmg(30), GIGANT, Fx.RUBBER, false));
        Moves.register("gigant_axe", c -> {
            // the giant leg rises and drops like an axe
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 to = Gx.ground(l, Gx.lookPoint(p, 10));
            Vfx.limb(l, p, R_LEG, to.add(0, 9, 0), GIGANT * c.scale(), 8, Fx.RUBBER);
            Scheduler.schedule(8, () -> {
                if (!p.isAlive()) return;
                Vfx.limb(l, p, R_LEG, to.add(0, 0.4, 0), GIGANT * c.scale(), 12, Fx.RUBBER);
                Vfx.slam(l, Fx.RUBBER, to, 6);
                Vfx.impact(l, Fx.RUBBER, to.add(0, 0.6, 0));
                for (LivingEntity e : Gx.around(l, to, 6, p)) { Gx.hurtNow(e, p, c.dmg(32)); Gx.knock(e, to, 2.8, 1.0); }
                Gx.crater(l, to, 3.0, 4.0F);
                Gx.sound(l, to, SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 0.5F);
            });
            return true;
        });
        Moves.register("gigant_whip", c -> sweep(c, 7, c.dmg(20), 2.8, Fx.RUBBER, GIGANT));
        Moves.register("gigant_jet_shell", c -> {
            // every limb and the neck at once: a shell of giant blows all around
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vfx.steam(l, p.position(), 16);
            for (int i = 0; i < 4; i++) {
                final int k = i;
                Scheduler.schedule(i * 2, () -> {
                    double a = Math.toRadians(p.getYRot() + k * 90);
                    Vec3 to = p.position().add(0, 1.2, 0).add(-Math.sin(a) * 6, 0, Math.cos(a) * 6);
                    Vfx.limb(l, p, k, to, GIGANT * c.scale(), 8, Fx.RUBBER);
                    Vfx.impact(l, Fx.RUBBER, to);
                });
            }
            for (LivingEntity e : Gx.around(l, p.position(), 7, p)) { Gx.hurtNow(e, p, c.dmg(28)); Gx.knock(e, p.position(), 3.0, 0.8); }
            Gx.sound(l, p.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.7F);
            return true;
        });
        Moves.register("red_roc", c -> {
            c.s().hardenTicks = 40;
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 to = Gx.lookPoint(p, 14 * c.scale());
            Vfx.limb(l, p, R_ARM, to, GIGANT * c.scale(), 14, Fx.FIRE);
            Vfx.beam(l, Fx.FIRE, c.hand(), to, 0.6, 8);
            for (LivingEntity e : Gx.line(l, c.hand(), to, 1.6, p)) { Gx.haki(e, p, c.dmg(28)); e.igniteForSeconds(8); Gx.knock(e, p.position(), 2.6, 0.6); }
            Scheduler.schedule(3, () -> {
                Vfx.blast(l, ParticleTypes.FLAME, to, 4.0);
                Vfx.sphere(l, Fx.FIRE, to, 4.0, 10);
                Vfx.ring(l, Fx.FIRE, to, 6, false);
                for (LivingEntity e : Gx.around(l, to, 6, p)) { Gx.hurtNow(e, p, c.dmg(18)); e.igniteForSeconds(8); Gx.knock(e, to, 2.2, 0.8); }
                Gx.scorch(l, to, 3.0, 6.0F);
                Gx.sound(l, to, SoundEvents.GENERIC_EXPLODE.value(), 1.8F, 0.7F);
            });
            return true;
        });
        Moves.register("thor_elephant_gun", c -> {
            // a lightning-charged giant haki fist
            c.s().hardenTicks = 40;
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 to = Gx.lookPoint(p, 12 * c.scale());
            Vfx.bolt(l, Fx.LIGHTNING, c.hand().add(0, 12, 0), c.hand());
            Vfx.limb(l, p, R_ARM, to, GIGANT * c.scale(), 14, Fx.HAKI);
            Vfx.bolt(l, Fx.LIGHTNING, c.hand(), to);
            for (LivingEntity e : Gx.line(l, c.hand(), to, 1.8, p)) { Gx.haki(e, p, c.dmg(40)); Gx.knock(e, p.position(), 3.2, 0.8); }
            Scheduler.schedule(3, () -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l);
                if (bolt != null) { bolt.moveTo(to.x, to.y, to.z); bolt.setCause(p); bolt.setDamage(c.dmg(10)); bolt.setVisualOnly(!Gx.griefing(l)); l.addFreshEntity(bolt); }
                Vfx.ring(l, Fx.LIGHTNING, to, 5, false);
                for (LivingEntity e : Gx.around(l, to, 5, p)) { Gx.hurtNow(e, p, c.dmg(14)); Gx.knock(e, to, 2.0, 0.8); }
                Gx.crater(l, to, 2.5, 6.0F);
            });
            return true;
        });

        // ---- Gear Fourth: Boundman
        Moves.register("kong_gun", c -> {
            c.s().hardenTicks = 40;
            ServerPlayer p = c.p();
            ServerLevel l = c.level();
            Vec3 back = c.hand().subtract(c.look().scale(1.4));
            Vfx.limb(l, p, R_ARM, back, HAKI_ARM * c.scale(), 10, Fx.HAKI);
            Vfx.puff(l, Fx.STEAM, back, 0.8, 8);
            Gx.sound(l, p.position(), SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.5F);
            Scheduler.schedule(8, () -> {
                if (!p.isAlive()) return;
                fist(c, Kind.HAKI_FIST, c.dmg(26), 2.6F, 1.7F, Vec3.ZERO, null).flags(FruitProjectile.FLAG_BOOM).life(14);
                Vfx.ring(l, Fx.STEAM, c.hand(), 1.2, true);
                Gx.sound(l, p.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.2F);
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
                    if (!p.isAlive()) return;
                    Vec3 to = Gx.lookPoint(p, 5 * c.scale());
                    Vfx.limb(c.level(), p, kk == 0 ? R_LEG : L_LEG, to, HAKI_ARM * c.scale(), 7, Fx.HAKI);
                    Vfx.arc(c.level(), Fx.HAKI, p.position().add(0, 1, 0).add(c.look()), p.getYRot() + 90, 2.0, 120, kk == 0 ? 0.6 : -0.6, false, 8);
                    for (LivingEntity e : Gx.cone(p, 5, 0.6)) { Gx.haki(e, p, c.dmg(14)); Gx.knock(e, p.position(), 2.0, 0.6); }
                    Gx.sound(c.level(), to, SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.7F);
                });
            }
            return true;
        });
        Moves.register("leo_bazooka", c -> { c.s().hardenTicks = 40; return bazooka(c, 8, c.dmg(44), HAKI_ARM, Fx.HAKI, 3.6, true, 6); });
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
        Moves.register("king_kong_gun", c -> kingKong(c, 20, c.dmg(60), 2.4F, Fx.HAKI));
        Moves.register("double_culverin", c -> {
            c.s().hardenTicks = 60;
            LivingEntity t = Gx.aimAssist(c.p(), 30);
            fist(c, Kind.HAKI_FIST, c.dmg(22), 1.4F, 1.5F, c.right().scale(0.9), t).life(90);
            fist(c, Kind.HAKI_FIST, c.dmg(22), 1.4F, 1.5F, c.right().scale(-0.9), t).life(90);
            Vfx.puff(c.level(), Fx.STEAM, c.pos().add(0, 1, 0), 1.2, 8);
            return true;
        });
        Moves.register("leo_rex_bazooka", c -> { c.s().hardenTicks = 60; Vfx.puff(c.level(), Fx.STEAM, c.pos().add(0, 1, 0), 1.6, 12); return bazooka(c, 9, c.dmg(64), HAKI_ARM * 1.3, Fx.HAKI, 4.4, true, 10); });
        Moves.register("kong_gatling", c -> { c.s().hardenTicks = 60; barrage(c, 10, 2, 10, c.dmg(14), HAKI_ARM, Fx.HAKI, 1.0, true); return true; });
        Moves.register("cannonball", c -> {
            // Tankman: swallow the next blow and fire the attacker back out
            c.s().cannonballTicks = 100;
            c.s().balloonTicks = 100;
            Vfx.sphere(c.level(), Fx.HAKI, c.pos().add(0, 1, 0), 2.0 * c.scale(), 10);
            Gx.sound(c.level(), c.pos(), SoundEvents.PUFFER_FISH_BLOW_UP, 1.3F, 0.4F);
            return true;
        });
        Moves.register("kong_rifle", c -> { c.s().hardenTicks = 40; return rifle(c, 11, c.dmg(34), HAKI_ARM, Fx.HAKI, true); });
        Moves.register("over_kong_gun", c -> kingKong(c, 26, c.dmg(80), 3.0F, Fx.HAKI));

        // ---- Gear Fourth: Snakeman
        Moves.register("snake_pistol", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 from = c.hand();
            Vec3 to = Gx.lookPoint(p, 14);
            Vec3 mid = from.lerp(to, 0.5).add(c.right().scale(1.8)).add(0, 0.6, 0);
            Vfx.limb(l, p, R_ARM, mid, HAKI_ARM, 5, Fx.HAKI);
            Scheduler.schedule(2, () -> {
                Vfx.limb(l, p, R_ARM, to, HAKI_ARM, 7, Fx.HAKI);
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
                fist(c, Kind.ELEPHANT_FIST, c.dmg(50), 1.5F, 1.6F, Vec3.ZERO, t).life(60).flags(FruitProjectile.FLAG_FIRE);
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
        Moves.register("dawn_whip", c -> sweep(c, 9, c.dmg(22), 3.2, Fx.NIKA, NIKA_ARM));
        Moves.register("dawn_rocket", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            LivingEntity t = Gx.aimAssist(p, 24);
            Vec3 to = t != null ? Gx.chest(t) : Gx.lookPoint(p, 20);
            Vec3 d = to.subtract(p.position().add(0, 1, 0));
            Vfx.limb(l, p, R_ARM, to, NIKA_ARM * c.scale(), 10, Fx.NIKA);
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
        Moves.register("mogura_pistol", c -> {
            // the fist goes into the ground and the ground itself punches the target from the side
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            LivingEntity t = Gx.aimAssist(p, 18);
            Vec3 to = t != null ? Gx.chest(t) : Gx.lookPoint(p, 12);
            Vec3 down = Gx.ground(l, p.position());
            Vfx.limb(l, p, R_ARM, down, NIKA_ARM * c.scale(), 16, Fx.NIKA);
            Vfx.slam(l, Fx.NIKA, down, 1.5);
            Scheduler.schedule(6, () -> {
                Vec3 side = to.add(c.right().scale(l.random.nextBoolean() ? 3 : -3));
                Vec3 g = Gx.ground(l, side);
                Vfx.arm(l, Fx.NIKA, g, to, NIKA_ARM * 1.4, 10);
                Vfx.debris(l, g, 20, 0.6, 0.5);
                Vfx.impact(l, Fx.NIKA, to);
                for (LivingEntity e : Gx.around(l, to, 2.0, p)) { Gx.haki(e, p, c.dmg(24)); Gx.knock(e, side, 2.4, 0.6); }
                Gx.sound(l, to, SoundEvents.PLAYER_ATTACK_STRONG, 1.3F, 0.6F);
            });
            return true;
        });
        Moves.register("star_gun", c -> {
            // spin up, then a giant hook that leaves stars circling the target
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            c.s().hardenTicks = 60;
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(p.getYRot() + i * 90);
                Scheduler.schedule(i, () -> Vfx.limb(l, p, R_ARM, p.position().add(0, 1.2, 0).add(-Math.sin(a) * 1.4, 0, Math.cos(a) * 1.4), NIKA_ARM, 3, Fx.NIKA));
            }
            Scheduler.schedule(8, () -> {
                if (!p.isAlive()) return;
                Vec3 to = Gx.lookPoint(p, 12);
                Vfx.limb(l, p, R_ARM, to, GIGANT, 12, Fx.FIRE);
                Vfx.impact(l, Fx.NIKA, to);
                for (LivingEntity e : Gx.line(l, c.hand(), to, 2.0, p)) {
                    Gx.haki(e, p, c.dmg(44));
                    Gx.knock(e, p.position(), 3.0, 0.8);
                    Gx.effect(e, MobEffects.CONFUSION, 200, 0);
                    Gx.effect(e, MobEffects.MOVEMENT_SLOWDOWN, 100, 3);
                    for (int k = 0; k < 5; k++) Vfx.arc(l, Fx.LIGHT, Gx.chest(e).add(0, 0.6, 0), k * 72, 0.8, 300, 0.0, true, 40);
                }
                Gx.sound(l, to, SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.9F);
                Gx.sound(l, to, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.5F);
            });
            return true;
        });
        Moves.register("dawn_cymbal", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            LivingEntity t = Gx.aimAssist(p, 12);
            if (t == null) return false;
            Vec3 ct = Gx.chest(t);
            Vec3 r = c.right().scale(2.5);
            Vfx.limb(l, p, R_ARM, ct.add(r), GIGANT, 10, Fx.NIKA);
            Vfx.limb(l, p, L_ARM, ct.subtract(r), GIGANT, 10, Fx.NIKA);
            Gx.root(t, 10);
            Scheduler.schedule(5, () -> {
                if (!t.isAlive()) return;
                Vfx.limb(l, p, R_ARM, ct, GIGANT, 8, Fx.NIKA);
                Vfx.limb(l, p, L_ARM, ct, GIGANT, 8, Fx.NIKA);
                Vfx.impact(l, Fx.NIKA, ct);
                Vfx.ring(l, Fx.NIKA, ct, 3, true);
                Gx.haki(t, p, c.dmg(40));
                Gx.effect(t, MobEffects.MOVEMENT_SLOWDOWN, 60, 4);
                Gx.sound(l, ct, SoundEvents.ANVIL_LAND, 1.4F, 0.8F);
                Scheduler.schedule(10, () -> { if (t.isAlive()) { Gx.knock(t, p.position(), 4.0, 1.2); Vfx.line(l, Fx.NIKA, Gx.chest(t), Gx.chest(t).add(c.flat().scale(6))); } });
            });
            return true;
        });
        Moves.register("kaminari", c -> {
            // grab a lightning bolt out of the sky and throw it
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 hand = c.hand().add(0, 0.6, 0);
            Vfx.bolt(l, Fx.LIGHTNING, hand.add(0, 24, 0), hand);
            Vfx.limb(l, p, R_ARM, hand.add(0, 3, 0), NIKA_ARM, 8, Fx.NIKA);
            Gx.sound(l, hand, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.4F, 1.4F);
            Scheduler.schedule(8, () -> {
                if (!p.isAlive()) return;
                Vec3 to = Gx.ground(l, Gx.lookPoint(p, 30));
                Vfx.bolt(l, Fx.LIGHTNING, hand, to);
                Vfx.beam(l, Fx.LIGHTNING, hand, to, 0.3, 6);
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l);
                if (bolt != null) { bolt.moveTo(to.x, to.y, to.z); bolt.setCause(p); bolt.setDamage(c.dmg(16)); bolt.setVisualOnly(!Gx.griefing(l)); l.addFreshEntity(bolt); }
                Vfx.ring(l, Fx.LIGHTNING, to, 4, false);
                for (LivingEntity e : Gx.around(l, to, 4, p)) { Gx.haki(e, p, c.dmg(20)); Gx.knock(e, to, 1.5, 0.6); }
            });
            return true;
        });
        Moves.register("dawn_gatling", c -> { c.s().hardenTicks = 60; barrage(c, 12, 2, 11, c.dmg(14), GIGANT * 0.8, Fx.NIKA, 1.4, true); return true; });
        Moves.register("dawn_stamp", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 from = p.position().add(0, 0.6, 0);
            Vec3 to = Gx.rayEnd(p, 16);
            Vfx.limb(l, p, R_LEG, to, NIKA_ARM, 12, Fx.NIKA);
            for (LivingEntity e : Gx.line(l, from, to, 1.2, p)) { Gx.haki(e, p, c.dmg(22)); Gx.knock(e, p.position(), 2.4, 0.5); Vfx.impact(l, Fx.NIKA, Gx.chest(e)); }
            Gx.sound(l, to, SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.7F);
            return true;
        });
    }

    /** King Kong Gun and Over Kong Gun: a long compression, then the giant haki fist. */
    static boolean kingKong(Cast c, int charge, float dmg, float size, int style) {
        c.s().hardenTicks = 80;
        ServerLevel l = c.level();
        ServerPlayer p = c.p();
        for (int t = 0; t < charge; t += 4) {
            final double r = 0.8 + t * 0.12;
            Scheduler.schedule(t, () -> {
                Vec3 back = p.getEyePosition().subtract(p.getLookAngle().scale(1.5)).add(0, -0.5, 0);
                Vfx.limb(l, p, R_ARM, back, (float) (HAKI_ARM + r * 0.12), 5, Fx.HAKI);
                Vfx.sphere(l, Fx.HAKI, back, r, 4);
                Vfx.steam(l, p.position(), 6);
            });
        }
        Gx.sound(l, p.position(), SoundEvents.FIRE_EXTINGUISH, 1.4F, 0.4F);
        Scheduler.schedule(charge, () -> {
            if (!p.isAlive()) return;
            fist(c, Kind.ELEPHANT_FIST, dmg, 1.6F, size, Vec3.ZERO, null).life(24);
            Vfx.ring(l, Fx.HAKI, c.hand(), 3, true);
            Vfx.puff(l, Fx.STEAM, p.position().add(0, 1, 0), 2.5, 20);
            Gx.sound(l, p.position(), SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.7F);
        });
        return true;
    }
}
