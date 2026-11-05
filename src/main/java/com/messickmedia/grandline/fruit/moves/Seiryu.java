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
 * Uo Uo no Mi, Model: Seiryu. The azure dragon's weather: Thunder Bagua, wind scythes, flame clouds, Blast and Boro
 * Breath, called lightning, the Dragon Twister and Ragnaraku.
 */
public final class Seiryu {
    private Seiryu() {}

    static void lightning(ServerLevel l, ServerPlayer p, Vec3 at, float dmg) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setCause(p);
            bolt.setDamage(dmg);
            bolt.setVisualOnly(!Gx.griefing(l));
            l.addFreshEntity(bolt);
        }
        Vfx.bolt(l, Fx.LIGHTNING, at.add(0, 24, 0), at);
        Vfx.ring(l, Fx.LIGHTNING, at.add(0, 0.1, 0), 2.5, false);
        for (LivingEntity e : Gx.around(l, at, 3.0, p)) { Gx.hurtNow(e, p, dmg); Gx.knock(e, at, 1.0, 0.4); }
    }

    /** A breath attack: a beam from the mouth, burning everything it crosses, exploding where it lands. */
    static void breath(Cast c, double range, double radius, float dmg, double blast, int style, boolean haki) {
        ServerLevel l = c.level();
        ServerPlayer p = c.p();
        Vec3 from = p.getEyePosition().add(p.getLookAngle().scale(0.6 * c.scale()));
        Vec3 to = Gx.rayEnd(l, from, p.getLookAngle(), range, p);
        Vfx.beam(l, style, from, to, radius, 10);
        for (LivingEntity e : Gx.line(l, from, to, radius + 0.6, p)) {
            if (haki) Gx.haki(e, p, dmg); else Gx.hurtNow(e, p, dmg);
            e.igniteForSeconds(6);
            Gx.knock(e, from, 0.8, 0.3);
        }
        Vfx.blast(l, ParticleTypes.FLAME, to, blast);
        Vfx.sphere(l, Fx.FIRE, to, blast, 8);
        Vfx.impact(l, Fx.FIRE, to);
        for (LivingEntity e : Gx.around(l, to, blast * 1.5, p)) { Gx.hurtNow(e, p, dmg * 0.5F); e.igniteForSeconds(5); Gx.knock(e, to, 1.2, 0.5); }
        Gx.scorch(l, to, blast * 0.8, haki ? 30.0F : 4.0F);
    }

    public static void register() {
        Moves.register("raimei_hakke", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Gomu.dash(p, c.look(), 1.0);
            Vec3 at = p.position().add(0, 1.0 * c.scale(), 0).add(c.look().scale(2.0 * c.scale()));
            Vfx.arc(l, Fx.LIGHTNING, p.position().add(0, 1.1 * c.scale(), 0), p.getYRot() + 90, 2.4 * c.scale(), 170, 0.7, false, 10);
            Vfx.bolt(l, Fx.LIGHTNING, at.add(0, 6, 0), at);
            Vfx.impact(l, Fx.LIGHTNING, at);
            for (LivingEntity e : Gx.cone(p, 4.5 * c.scale(), 0.55)) {
                Gx.haki(e, p, c.dmg(16));
                Gx.knock(e, p.position(), 2.8, 0.7);
                Gx.effect(e, MobEffects.MOVEMENT_SLOWDOWN, 40, 2);
            }
            Gx.sound(l, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2F, 1.5F);
            Gx.sound(l, at, SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.5F);
            Gx.crater(l, Gx.ground(l, at), 1.2, 1.5F);
            return true;
        });
        Moves.register("kaifu", c -> {
            FruitProjectile.shoot(c.level(), c.p(), Kind.KAIFU, c.dmg(12), 1.5F).flags(FruitProjectile.FLAG_PIERCE | FruitProjectile.FLAG_CUT).size(c.scale()).life(50);
            Vfx.arc(c.level(), Fx.WIND, c.pos().add(0, 1.0, 0), c.p().getYRot() + 90, 1.4, 150, 0.6, true, 6);
            Gx.sound(c.level(), c.pos(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.5F);
            Gx.sound(c.level(), c.pos(), SoundEvents.PHANTOM_FLAP, 1.0F, 0.6F);
            return true;
        });
        Moves.register("flame_cloud", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vfx.puff(l, Fx.CLOUD, p.position().add(0, -0.2, 0), 1.6 * c.scale(), 14);
            Vfx.ring(l, Fx.FIRE, p.position().add(0, 0.1, 0), 2.0 * c.scale(), false);
            p.setDeltaMovement(c.look().multiply(0.6, 0, 0.6).add(0, 0.95, 0));
            p.hurtMarked = true;
            p.resetFallDistance();
            Gx.effect(p, MobEffects.SLOW_FALLING, 120, 0);
            c.s().cloudTicks = 120;
            for (LivingEntity e : Gx.around(l, p.position(), 3.0 * c.scale(), p)) { Gx.hurtNow(e, p, c.dmg(5)); e.igniteForSeconds(4); Gx.knock(e, p.position(), 1.2, 0.3); }
            Gx.sound(l, p.position(), SoundEvents.FIRECHARGE_USE, 1.0F, 0.6F);
            return true;
        });
        Moves.register("blast_breath", c -> {
            breath(c, 18, 0.5, c.dmg(11), 1.6, Fx.FIRE, false);
            Gx.sound(c.level(), c.pos(), SoundEvents.BLAZE_SHOOT, 1.2F, 0.6F);
            return true;
        });
        Moves.register("thunder", c -> {
            ServerLevel l = c.level();
            Vec3 at = Gx.ground(l, Gx.lookPoint(c.p(), 32));
            lightning(l, c.p(), at, c.dmg(14));
            return true;
        });
        Moves.register("tatsumaki", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Gx.sound(l, p.position(), SoundEvents.PHANTOM_FLAP, 1.5F, 0.4F);
            Scheduler.repeat(16, 4, i -> {
                if (!p.isAlive()) return;
                Vec3 at = p.position();
                double r = 6 * c.scale();
                Vfx.arc(l, Fx.WIND, at.add(0, 0.3 + (i % 4) * 1.2, 0), i * 45, r * (0.6 + 0.1 * (i % 4)), 300, 0.4, i % 2 == 1, 10);
                Vfx.ring(l, Fx.WIND, at.add(0, 0.5 + (i % 5) * 0.9, 0), r * 0.7, false);
                for (LivingEntity e : Gx.around(l, at, r, p)) {
                    Gx.pull(e, at.add(0, 3, 0), 0.5);
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.25, 0));
                    e.hurtMarked = true;
                    Gx.hurtNow(e, p, c.dmg(3));
                    e.resetFallDistance();
                }
                if (i % 3 == 0) Vfx.debris(l, at, 12, 2.5, 0.4);
                if (i == 15) for (LivingEntity e : Gx.around(l, at, r, p)) Gx.knock(e, at, 3.0, 1.2);
            });
            return true;
        });

        // ---- hybrid
        Moves.register("boro_breath", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            for (int t = 0; t < 8; t += 2) {
                final double r = 0.5 + t * 0.15;
                Scheduler.schedule(t, () -> Vfx.sphere(l, Fx.FIRE, p.getEyePosition().add(p.getLookAngle().scale(0.8 * c.scale())), r * c.scale(), 3));
            }
            Gx.sound(l, p.position(), SoundEvents.ENDER_DRAGON_GROWL, 1.0F, 0.7F);
            Scheduler.repeat(3, 1, i -> {
                if (!p.isAlive()) return;
                breath(c, 44, 1.2 * c.scale(), c.dmg(i == 0 ? 28 : 8), i == 0 ? 4.0 : 1.5, Fx.FIRE, true);
                if (i == 0) {
                    Gx.sound(l, p.position(), SoundEvents.ENDER_DRAGON_SHOOT, 1.6F, 0.5F);
                    Gx.sound(l, p.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.6F);
                }
            });
            Scheduler.schedule(8, () -> {
                if (!p.isAlive()) return;
                breath(c, 44, 1.2 * c.scale(), c.dmg(28), 4.0, Fx.FIRE, true);
                Gx.sound(l, p.position(), SoundEvents.ENDER_DRAGON_SHOOT, 1.6F, 0.5F);
            });
            return true;
        });
        Moves.register("kaen_hakke", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Gomu.dash(p, c.look(), 1.2);
            Vec3 at = p.position().add(0, 1.0 * c.scale(), 0).add(c.look().scale(2.5 * c.scale()));
            Vfx.arc(l, Fx.FIRE, p.position().add(0, 1.2 * c.scale(), 0), p.getYRot() + 90, 3.0 * c.scale(), 180, 0.9, false, 12);
            Vfx.bolt(l, Fx.LIGHTNING, at.add(0, 7, 0), at);
            Scheduler.schedule(2, () -> {
                Vfx.blast(l, ParticleTypes.FLAME, at, 3.0);
                Vfx.sphere(l, Fx.FIRE, at, 3.0 * c.scale(), 8);
                Vfx.impact(l, Fx.FIRE, at);
                for (LivingEntity e : Gx.around(l, at, 4.5 * c.scale(), p)) { Gx.haki(e, p, c.dmg(26)); e.igniteForSeconds(8); Gx.knock(e, p.position(), 3.0, 0.9); }
                Gx.scorch(l, Gx.ground(l, at), 2.5, 4.0F);
                Gx.sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.8F);
                Gx.sound(l, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0F, 1.3F);
            });
            return true;
        });

        Moves.register("thunder_bagua_leap", c -> {
            // leap in, land the Thunder Bagua
            ServerPlayer p = c.p();
            ServerLevel l = c.level();
            LivingEntity t = Gx.aimAssist(p, 12);
            Vec3 to = t != null ? Gx.chest(t) : Gx.lookPoint(p, 8);
            Vec3 d = to.subtract(p.position().add(0, 1, 0));
            Gomu.dash(p, d.normalize().add(0, 0.3, 0), Math.min(2.0, 0.8 + d.length() * 0.15));
            Scheduler.schedule(6, () -> {
                if (!p.isAlive()) return;
                Vec3 at = p.position().add(0, 1, 0).add(p.getLookAngle().scale(1.5));
                Vfx.arc(l, Fx.LIGHTNING, p.position().add(0, 1.1, 0), p.getYRot() + 90, 2.4, 170, 0.7, false, 10);
                Vfx.bolt(l, Fx.LIGHTNING, at.add(0, 6, 0), at);
                for (LivingEntity e : Gx.cone(p, 4.5, 0.5)) { Gx.haki(e, p, c.dmg(18)); Gx.knock(e, p.position(), 2.6, 0.7); }
                Gx.sound(l, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2F, 1.5F);
            });
            return true;
        });
        Moves.register("bolo_barrage", c -> {
            // Bolo Breath split into a spray of fireballs
            ServerPlayer p = c.p();
            Scheduler.repeat(8, 1, i -> {
                if (!p.isAlive()) return;
                Vec3 dir = p.getLookAngle().add(Vfx.randDir(c.level()).scale(0.12)).normalize();
                FruitProjectile.fire(c.level(), p, Kind.FIREBALL, c.dmg(8), p.getEyePosition().add(dir.scale(1.0 * c.scale())), dir, 1.5F).size(0.7F * c.scale()).life(50);
                if (i % 2 == 0) Gx.sound(c.level(), p.position(), SoundEvents.BLAZE_SHOOT, 1.0F, 0.8F);
            });
            return true;
        });
        Moves.register("tail_swipe", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            double r = (c.s().inForm("seiryu_dragon") ? 8 : 4.5) * c.scale() / Math.max(1, c.scale() * 0.6);
            Vec3 at = p.position().add(0, 0.8 * c.scale(), 0);
            Vfx.arc(l, Fx.DRAGON, at, p.getYRot() + 180, r * 0.7, 300, 0.3, false, 12);
            Vfx.ring(l, Fx.DRAGON, at, r * 0.8, false);
            for (LivingEntity e : Gx.around(l, at, r, p)) { Gx.hurtNow(e, p, c.dmg(16)); Gx.knock(e, at, 3.0, 0.8); }
            Gx.sound(l, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.5F);
            Vfx.debris(l, Gx.ground(l, at), 16, r * 0.5, 0.3);
            return true;
        });
        Moves.register("tail_grab", c -> {
            // the prehensile tail snatches a target, holds it, then hurls it
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            LivingEntity t = Gx.aimAssist(p, 10);
            if (t == null) return false;
            Vfx.arm(l, Fx.DRAGON, p.position().add(0, 0.6, 0), Gx.chest(t), 0.22 * c.scale(), 22);
            Gx.root(t, 22);
            Scheduler.repeat(4, 4, i -> { if (t.isAlive()) { Gx.pull(t, p.position().add(0, 1.5, 0).add(c.flat().scale(2)), 1.0); Gx.hurtNow(t, p, c.dmg(3)); } });
            Scheduler.schedule(20, () -> {
                if (!t.isAlive()) return;
                Gx.hurtNow(t, p, c.dmg(12));
                Gx.knock(t, p.position(), 3.5, 1.2);
                Vfx.impact(l, Fx.DRAGON, Gx.chest(t));
                Gx.sound(l, t.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.6F);
            });
            return true;
        });
        Moves.register("kaen_daiko", c -> {
            // Kaen Daiko: breathe a dragon of flame over the whole body
            c.s().daikoTicks = 300;
            ServerLevel l = c.level();
            Vec3 at = c.pos().add(0, 1.0 * c.scale(), 0);
            breath(c, 6 * c.scale(), 0.6 * c.scale(), c.dmg(6), 1.5, Fx.FIRE, false);
            Vfx.sphere(l, Fx.FIRE, at, (c.s().inForm("seiryu_dragon") ? 5.0 : 2.8), 14);
            Vfx.ring(l, Fx.FIRE, at, 6, false);
            Gx.sound(l, at, SoundEvents.ENDER_DRAGON_GROWL, 1.4F, 0.6F);
            Gx.sound(l, at, SoundEvents.FIRECHARGE_USE, 1.4F, 0.5F);
            return true;
        });
        Moves.register("bite", c -> {
            // the dragon chomps down: anything in front of the jaws is crushed and dragged
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 jaw = p.getEyePosition().add(p.getLookAngle().scale(2.5 * c.scale()));
            Gomu.dash(p, c.look(), 1.2);
            Vfx.arc(l, Fx.DRAGON, jaw, p.getYRot() + 90, 2.2 * c.scale(), 160, 1.2, false, 8);
            Vfx.arc(l, Fx.DRAGON, jaw, p.getYRot() + 90, 2.2 * c.scale(), 160, -1.2, false, 8);
            for (LivingEntity e : Gx.cone(p, 5 * c.scale(), 0.6)) {
                Gx.haki(e, p, c.dmg(30));
                Gx.pull(e, jaw, 1.5);
                Gx.root(e, 20);
                Vfx.impact(l, Fx.DRAGON, Gx.chest(e));
            }
            Gx.sound(l, jaw, SoundEvents.ENDER_DRAGON_GROWL, 1.2F, 1.2F);
            Gx.sound(l, jaw, SoundEvents.PLAYER_ATTACK_CRIT, 1.4F, 0.5F);
            return true;
        });
        Moves.register("claw_swipe", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 at = p.position().add(0, 1.0 * c.scale(), 0);
            Vfx.arc(l, Fx.DRAGON, at, p.getYRot() + 90, 3.0 * c.scale(), 150, 0.8, false, 10);
            for (int k = 0; k < 3; k++) Vfx.line(l, Fx.DRAGON, at.add(c.look().scale(1.0)).add(c.right().scale((k - 1) * 0.5)).add(0, 1.5, 0), at.add(c.look().scale(4.0 * c.scale())).add(c.right().scale((k - 1) * 0.8)).add(0, -0.5, 0));
            for (LivingEntity e : Gx.cone(p, 5 * c.scale(), 0.55)) { Gx.hurtNow(e, p, c.dmg(18)); Gx.knock(e, p.position(), 2.4, 0.5); Gx.cut(l, Gx.chest(e), Gx.chest(e).add(c.flat()), 1.0F); }
            Gx.sound(l, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.4F);
            return true;
        });
        Moves.register("tatsumaki_kaifu", c -> {
            // the Dragon Twister throws out wind scythes in every direction
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Gx.sound(l, p.position(), SoundEvents.PHANTOM_FLAP, 1.6F, 0.3F);
            Scheduler.repeat(12, 3, i -> {
                if (!p.isAlive()) return;
                Vec3 at = p.position();
                double r = 8 * c.scale();
                Vfx.arc(l, Fx.WIND, at.add(0, 0.5 + (i % 4) * 1.5, 0), i * 60, r * (0.5 + 0.1 * (i % 4)), 320, 0.4, i % 2 == 1, 10);
                for (LivingEntity e : Gx.around(l, at, r, p)) { Gx.pull(e, at.add(0, 3, 0), 0.4); e.setDeltaMovement(e.getDeltaMovement().add(0, 0.2, 0)); e.hurtMarked = true; Gx.hurtNow(e, p, c.dmg(3)); e.resetFallDistance(); }
                if (i % 2 == 0) {
                    double a = Math.toRadians(i * 60 + p.getYRot());
                    Vec3 dir = new Vec3(-Math.sin(a), 0.05, Math.cos(a));
                    FruitProjectile.fire(l, p, Kind.KAIFU, c.dmg(12), at.add(0, 2, 0).add(dir.scale(2)), dir, 1.6F).flags(FruitProjectile.FLAG_PIERCE | FruitProjectile.FLAG_CUT).size(1.4F).life(60);
                }
            });
            return true;
        });

        // ---- dragon
        Moves.register("kaifu_storm", c -> {
            ServerPlayer p = c.p();
            for (int i = 0; i < 5; i++) {
                double a = (i - 2) * 0.18;
                Vec3 look = p.getLookAngle();
                Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
                Vec3 from = p.getEyePosition().add(look.scale(1.5 * c.scale())).add(right.scale(a * 4)).add(0, (i % 2) * 1.5 - 0.5, 0);
                FruitProjectile.fire(c.level(), p, Kind.KAIFU, c.dmg(12), from, look.add(right.scale(a)).normalize(), 1.6F)
                        .flags(FruitProjectile.FLAG_PIERCE | FruitProjectile.FLAG_CUT).size(1.4F).life(60);
            }
            Vfx.ring(c.level(), Fx.WIND, p.position().add(0, 2, 0), 4, true);
            Gx.sound(c.level(), p.position(), SoundEvents.PHANTOM_FLAP, 1.6F, 0.4F);
            Gx.sound(c.level(), p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.4F);
            return true;
        });
        Moves.register("cloud_lift", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 center = Gx.ground(l, Gx.lookPoint(p, 24));
            Gx.sound(l, center, SoundEvents.FIRECHARGE_USE, 1.6F, 0.4F);
            Scheduler.repeat(24, 1, i -> {
                if (!p.isAlive()) return;
                if (i % 2 == 0) Vfx.puff(l, Fx.CLOUD, center.add(0, 0.3 + i * 0.25, 0), 4.0, 5);
                if (i % 6 == 0) Vfx.ring(l, Fx.FIRE, center.add(0, 0.3 + i * 0.25, 0), 7, false);
                for (LivingEntity e : Gx.around(l, center.add(0, i * 0.25, 0), 7, p)) {
                    if (i < 20) {
                        e.setDeltaMovement(e.getDeltaMovement().multiply(0.6, 0, 0.6).add(0, 0.35, 0));
                        e.hurtMarked = true;
                        e.resetFallDistance();
                        if (i % 5 == 0) { Gx.hurtNow(e, p, c.dmg(3)); e.igniteForSeconds(3); }
                    } else {
                        // the clouds let go: everything comes down hard
                        e.setDeltaMovement(e.getDeltaMovement().add(0, -1.6, 0));
                        e.hurtMarked = true;
                        e.fallDistance = 12;
                    }
                }
            });
            return true;
        });
        Moves.register("bolo_breath", c -> {
            FruitProjectile.shoot(c.level(), c.p(), Kind.FIREBALL, c.dmg(22), 1.6F).size(1.6F).life(60);
            Vfx.sphere(c.level(), Fx.FIRE, c.p().getEyePosition().add(c.look().scale(1.5)), 1.2, 4);
            Gx.sound(c.level(), c.pos(), SoundEvents.ENDER_DRAGON_SHOOT, 1.4F, 0.7F);
            return true;
        });
        Moves.register("thunderstorm", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 center = Gx.ground(l, Gx.lookPoint(p, 40));
            Gx.sound(l, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 0.6F);
            Vfx.puff(l, Fx.CLOUD, center.add(0, 14, 0), 8, 24);
            Scheduler.repeat(8, 3, i -> {
                if (!p.isAlive()) return;
                Vec3 at = i == 0 ? center : Gx.ground(l, center.add((l.random.nextDouble() - 0.5) * 12, 1, (l.random.nextDouble() - 0.5) * 12));
                lightning(l, p, at, c.dmg(12));
            });
            return true;
        });
        Moves.register("ragnaraku", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 target = Gx.ground(l, Gx.lookPoint(p, 30));
            p.setDeltaMovement(0, 2.4, 0);
            p.hurtMarked = true;
            Vfx.puff(l, Fx.CLOUD, p.position(), 3, 20);
            Gx.sound(l, p.position(), SoundEvents.ENDER_DRAGON_FLAP, 2.0F, 0.6F);
            Scheduler.schedule(14, () -> {
                if (!p.isAlive()) return;
                Gx.teleport(p, target.add(0, 18, 0));
                p.setDeltaMovement(0, -3.0, 0);
                p.hurtMarked = true;
                Vfx.beam(l, Fx.LIGHTNING, target.add(0, 18, 0), target, 0.6, 10);
                Gx.sound(l, target, SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.5F);
            });
            Scheduler.schedule(22, () -> {
                if (!p.isAlive()) return;
                Vec3 at = Gx.ground(l, p.position());
                Gx.teleport(p, at);
                p.resetFallDistance();
                double r = 9;
                Vfx.impact(l, Fx.LIGHTNING, at.add(0, 1, 0));
                Vfx.slam(l, Fx.DRAGON, at, r);
                Vfx.ring(l, Fx.LIGHTNING, at.add(0, 0.2, 0), r, false);
                Vfx.ring(l, Fx.FIRE, at.add(0, 0.5, 0), r * 0.7, false);
                for (int k = 0; k < 6; k++) Vfx.bolt(l, Fx.LIGHTNING, at.add(0, 20, 0), at.add(Vfx.randDir(l).multiply(r, 0, r)));
                for (LivingEntity e : Gx.around(l, at, r, p)) { Gx.haki(e, p, c.dmg(40)); Gx.knock(e, at, 3.5, 1.3); e.igniteForSeconds(4); }
                Gx.crater(l, at, 4.0, 8.0F);
                Gx.sound(l, at, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.4F);
                Gx.sound(l, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 0.5F);
            });
            return true;
        });
    }
}
