package com.messickmedia.grandline.fruit.moves;

import com.messickmedia.grandline.entity.FruitProjectile;
import com.messickmedia.grandline.entity.FruitProjectile.Kind;
import com.messickmedia.grandline.fruit.Cast;
import com.messickmedia.grandline.fruit.Gx;
import com.messickmedia.grandline.fruit.Moves;
import com.messickmedia.grandline.fruit.Scheduler;
import com.messickmedia.grandline.fruit.Vfx;
import com.messickmedia.grandline.net.Fx;
import com.messickmedia.grandline.registry.ModRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Pika Pika no Mi: a body of light. Light bullets, a light blade, light-speed travel, lasers and the Sun Burst form. */
public final class Pika {
    private Pika() {}

    /** An instant laser from a point along a direction: a thick beam, everything in it burns, the end explodes. */
    static void laser(Cast c, Vec3 from, Vec3 dir, double range, float dmg, double radius, double blast) {
        ServerLevel l = c.level();
        ServerPlayer p = c.p();
        Vec3 to = Gx.rayEnd(l, from, dir, range, p);
        Vfx.beam(l, Fx.LIGHT, from, to, radius, 10);
        for (LivingEntity e : Gx.line(l, from, to, radius + 0.6, p)) {
            Gx.hurtNow(e, p, dmg);
            e.igniteForSeconds(4);
            Gx.knock(e, from, 0.6, 0.2);
        }
        Vfx.impact(l, Fx.LIGHT, to);
        Vfx.blast(l, ModRegistries.LIGHT.get(), to, blast);
        Vfx.sphere(l, Fx.LIGHT, to, blast * 0.8, 6);
        for (LivingEntity e : Gx.around(l, to, blast * 1.4, p)) { Gx.hurtNow(e, p, dmg * 0.5F); e.igniteForSeconds(3); }
        Gx.scorch(l, to, blast * 0.7, 4.0F);
        Gx.sound(l, from, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.9F);
        Gx.sound(l, to, SoundEvents.GENERIC_EXPLODE.value(), 0.9F, 1.5F);
    }

    static void teleportTo(Cast c, Vec3 dest) {
        ServerLevel l = c.level();
        ServerPlayer p = c.p();
        Vec3 from = p.position().add(0, 0.9, 0);
        Vfx.beam(l, Fx.LIGHT, from, dest.add(0, 0.9, 0), 0.28, 8);
        Vfx.sphere(l, Fx.LIGHT, from, 1.0, 5);
        Gx.teleport(p, dest);
        Vfx.sphere(l, Fx.LIGHT, dest.add(0, 0.9, 0), 1.2, 6);
        Vfx.dress(l, Fx.LIGHT, dest.add(0, 1, 0), 0.8, 2);
        Gx.sound(l, from, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 2.0F);
        Gx.sound(l, dest, SoundEvents.BEACON_POWER_SELECT, 0.6F, 2.0F);
    }

    public static void register() {
        Moves.register("yasakani", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Scheduler.repeat(8, 1, i -> {
                if (!p.isAlive()) return;
                for (int k = 0; k < 2; k++) {
                    Vec3 dir = p.getLookAngle().add(Vfx.randDir(l).scale(0.09)).normalize();
                    Vec3 from = c.hand().add(c.right().scale((k == 0 ? 0.5 : -0.5)).add(0, (l.random.nextDouble() - 0.5) * 0.6, 0));
                    FruitProjectile.fire(l, p, Kind.LIGHT_BULLET, c.dmg(4), from, dir, 3.2F).life(40);
                }
                if (i % 2 == 0) Gx.sound(l, p.position(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.6F, 2.0F);
            });
            Vfx.sphere(l, Fx.LIGHT, c.hand(), 0.7, 6);
            return true;
        });
        Moves.register("murakumo", c -> {
            c.s().murakumoTicks = 240;
            ServerLevel l = c.level();
            Vec3 hand = c.hand();
            Vfx.beam(l, Fx.LIGHT, hand, hand.add(c.look().scale(1.8)).add(0, 0.3, 0), 0.08, 12);
            Vfx.sphere(l, Fx.LIGHT, hand, 0.5, 6);
            Vfx.arc(l, Fx.LIGHT, c.pos().add(0, 1.1, 0), c.p().getYRot() + 90, 1.6, 170, 0.5, false, 10);
            for (LivingEntity e : Gx.cone(c.p(), 3.2, 0.5)) { Gx.hurtNow(e, c.p(), c.dmg(9)); e.igniteForSeconds(3); }
            Gx.sound(l, hand, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.4F);
            Gx.sound(l, hand, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.6F);
            return true;
        });
        Moves.register("yata", c -> {
            ServerPlayer p = c.p();
            LivingEntity t = Gx.lookTarget(p, 48);
            Vec3 dest;
            if (t != null) {
                // appear right behind the target
                Vec3 back = p.getLookAngle().multiply(1, 0, 1).normalize();
                dest = t.position().add(back.scale(1.6));
                dest = Gx.rayEnd(c.level(), Gx.chest(t), back, 1.6, t).subtract(0, t.getBbHeight() * 0.55, 0);
            } else {
                Vec3 end = Gx.rayEnd(p, 48);
                dest = end.subtract(p.getLookAngle().scale(0.8));
                dest = new Vec3(dest.x, dest.y - 0.9, dest.z);
                if (c.level().getBlockState(net.minecraft.core.BlockPos.containing(dest)).isSolid()) dest = dest.add(0, 1, 0);
            }
            teleportTo(c, dest);
            return true;
        });
        Moves.register("light_kick", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            LivingEntity t = Gx.aimAssist(p, 18);
            Vec3 from = p.position().add(0, 0.9, 0);
            Vec3 to = t != null ? Gx.chest(t) : Gx.lookPoint(p, 14);
            Vfx.beam(l, Fx.LIGHT, from, to, 0.25, 8);
            for (LivingEntity e : Gx.line(l, from, to, 1.2, p)) {
                Gx.hurtNow(e, p, c.dmg(16));
                Gx.effect(e, MobEffects.BLINDNESS, 60, 0);
                Gx.knock(e, from, 2.4, 0.6);
                Vfx.impact(l, Fx.LIGHT, Gx.chest(e));
            }
            Vec3 land = to.subtract(p.getLookAngle().scale(1.2)).subtract(0, 0.9, 0);
            Gx.teleport(p, land);
            Vfx.sphere(l, Fx.LIGHT, to, 1.6, 8);
            Vfx.ring(l, Fx.LIGHT, to, 2.5, true);
            Gx.sound(l, to, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.7F);
            Gx.sound(l, from, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.8F);
            return true;
        });
        Moves.register("laser", c -> {
            laser(c, c.hand(), c.look(), 64, c.dmg(16), 0.3, 2.2);
            return true;
        });
        Moves.register("amaterasu", c -> {
            ServerLevel l = c.level();
            Vec3 at = c.pos().add(0, 1.2, 0);
            Vfx.sphere(l, Fx.LIGHT, at, 3.0, 10);
            Vfx.ring(l, Fx.LIGHT, at, 8, false);
            l.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 3, 0.5D, 0.5D, 0.5D, 0.0D);
            for (LivingEntity e : Gx.around(l, at, 12, c.p())) {
                Gx.effect(e, MobEffects.BLINDNESS, 120, 0);
                Gx.effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
                Gx.hurtNow(e, c.p(), c.dmg(4));
            }
            Gx.sound(l, at, SoundEvents.BEACON_ACTIVATE, 1.4F, 2.0F);
            return true;
        });

        // ---- Sun Burst: the fruit pushed to its limit
        Moves.register("yasakani_rain", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 center = Gx.lookPoint(p, 30);
            Scheduler.repeat(15, 1, i -> {
                if (!p.isAlive()) return;
                for (int k = 0; k < 2; k++) {
                    Vec3 from = center.add((l.random.nextDouble() - 0.5) * 10, 12 + l.random.nextDouble() * 4, (l.random.nextDouble() - 0.5) * 10);
                    Vec3 target = center.add((l.random.nextDouble() - 0.5) * 7, 0, (l.random.nextDouble() - 0.5) * 7);
                    FruitProjectile.fire(l, p, Kind.LIGHT_BULLET, c.dmg(6), from, target.subtract(from), 3.0F).flags(FruitProjectile.FLAG_BOOM).life(40);
                }
                if (i % 3 == 0) Gx.sound(l, center, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.8F, 1.8F);
            });
            Vfx.sphere(l, Fx.LIGHT, p.getEyePosition().add(0, 2, 0), 1.5, 8);
            Vfx.beam(l, Fx.LIGHT, p.getEyePosition(), center.add(0, 14, 0), 0.15, 10);
            return true;
        });
        Moves.register("laser_volley", c -> {
            ServerPlayer p = c.p();
            Scheduler.repeat(5, 2, i -> {
                if (!p.isAlive()) return;
                double a = (i - 2) * 0.12;
                Vec3 look = p.getLookAngle();
                Vec3 right = new Vec3(-look.z, 0, look.x).normalize();
                Vec3 dir = look.add(right.scale(a)).normalize();
                laser(c, p.getEyePosition().add(right.scale(a * 3)).add(0, -0.3, 0), dir, 64, c.dmg(12), 0.22, 1.8);
            });
            return true;
        });
        Moves.register("light_acceleration", c -> {
            // every blow connects at the speed of light for a while: melee hits burst into light
            c.s().accelTicks = 400;
            Gx.effect(c.p(), MobEffects.MOVEMENT_SPEED, 400, 1);
            Vfx.sphere(c.level(), Fx.LIGHT, c.pos().add(0, 1, 0), 1.3, 8);
            Vfx.ring(c.level(), Fx.LIGHT, c.pos().add(0, 1, 0), 2.0, true);
            Gx.sound(c.level(), c.pos(), SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.6F);
            return true;
        });
        Moves.register("laser_kick", c -> {
            // the leg becomes a laser: a dash, then a beam fired from the sole of the foot
            ServerPlayer p = c.p();
            ServerLevel l = c.level();
            LivingEntity t = Gx.aimAssist(p, 16);
            Vec3 to = t != null ? Gx.chest(t) : Gx.lookPoint(p, 12);
            Vec3 d = to.subtract(p.position().add(0, 1, 0));
            Gx.teleport(p, to.subtract(d.normalize().scale(2.0)).subtract(0, 0.9, 0));
            Vfx.beam(l, Fx.LIGHT, p.position().add(0, 0.9, 0).subtract(d.normalize().scale(d.length())), p.position().add(0, 0.9, 0), 0.2, 6);
            Scheduler.schedule(2, () -> { if (p.isAlive()) laser(c, p.position().add(0, 0.6, 0), p.getLookAngle(), 40, c.dmg(22), 0.45, 3.0); });
            return true;
        });
        Moves.register("finger_lasers", c -> {
            // piercing needle beams from the fingertips, one after another
            ServerPlayer p = c.p();
            Scheduler.repeat(5, 2, i -> {
                if (!p.isAlive()) return;
                Vec3 from = c.hand().add(c.right().scale((i - 2) * 0.08));
                Vec3 dir = p.getLookAngle().add(Vfx.randDir(c.level()).scale(0.02)).normalize();
                Vec3 to = Gx.rayEnd(c.level(), from, dir, 48, p);
                Vfx.beam(c.level(), Fx.LIGHT, from, to, 0.08, 6);
                for (LivingEntity e : Gx.line(c.level(), from, to, 0.4, p)) { Gx.hurtNow(e, p, c.dmg(5)); e.igniteForSeconds(2); }
                Vfx.impact(c.level(), Fx.LIGHT, to);
                Gx.sound(c.level(), from, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.5F, 2.0F);
            });
            return true;
        });
        Moves.register("light_clones", c -> {
            // Yasakani no Magatama, the other way: the light particles take the user's own shape and fight beside them
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            for (int i = 0; i < 2; i++) {
                Vec3 at = p.position().add(c.right().scale(i == 0 ? 2.2 : -2.2));
                FruitProjectile.fire(l, p, Kind.LIGHT_CLONE, c.dmg(4), at, p.getLookAngle(), 0.0F).life(300);
                Vfx.sphere(l, Fx.LIGHT, at.add(0, 1, 0), 1.2, 8);
            }
            Gx.sound(l, p.position(), SoundEvents.BEACON_ACTIVATE, 1.0F, 1.8F);
            return true;
        });
        Moves.register("eye_laser", c -> {
            ServerPlayer p = c.p();
            for (int k = -1; k <= 1; k += 2) {
                Vec3 from = p.getEyePosition().add(c.right().scale(k * 0.12));
                Vec3 to = Gx.rayEnd(c.level(), from, p.getLookAngle(), 40, p);
                Vfx.beam(c.level(), Fx.LIGHT, from, to, 0.1, 6);
                for (LivingEntity e : Gx.line(c.level(), from, to, 0.5, p)) { Gx.hurtNow(e, p, c.dmg(8)); e.igniteForSeconds(3); }
                Vfx.impact(c.level(), Fx.LIGHT, to);
            }
            Gx.sound(c.level(), p.position(), SoundEvents.BEACON_POWER_SELECT, 0.8F, 2.0F);
            return true;
        });
        Moves.register("light_hover", c -> {
            // light weighs nothing: the body hangs in the air for a moment
            ServerPlayer p = c.p();
            p.setDeltaMovement(p.getDeltaMovement().multiply(0.2, 0, 0.2).add(0, 0.25, 0));
            p.hurtMarked = true;
            p.resetFallDistance();
            Gx.effect(p, MobEffects.SLOW_FALLING, 80, 0);
            Vfx.ring(c.level(), Fx.LIGHT, p.position().add(0, 0.1, 0), 1.2, false);
            return true;
        });
        Moves.register("ama_no_iwato", c -> {
            // the heavenly cave: a colossal beam from both feet
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            p.setDeltaMovement(p.getDeltaMovement().add(0, 0.8, 0));
            p.hurtMarked = true;
            for (int t = 0; t < 16; t += 4) {
                final double r = 0.8 + t * 0.15;
                Scheduler.schedule(t, () -> { Vfx.sphere(l, Fx.LIGHT, p.position().add(0, 0.4, 0), r, 4); Vfx.dress(l, Fx.LIGHT, p.position(), 2.0, 2); });
            }
            Gx.sound(l, p.position(), SoundEvents.BEACON_ACTIVATE, 1.6F, 1.2F);
            Scheduler.schedule(16, () -> {
                if (!p.isAlive()) return;
                Vec3 from = p.position().add(0, 0.4, 0);
                Vec3 dir = p.getLookAngle();
                Vec3 to = Gx.rayEnd(l, from, dir, 70, p);
                Vfx.beam(l, Fx.LIGHT, from, to, 1.6, 16);
                for (LivingEntity e : Gx.line(l, from, to, 2.2, p)) { Gx.hurtNow(e, p, c.dmg(60)); e.igniteForSeconds(8); Gx.knock(e, from, 2.0, 0.6); }
                Vfx.sphere(l, Fx.LIGHT, to, 6, 14);
                Vfx.ring(l, Fx.LIGHT, to, 9, false);
                Vfx.blast(l, ModRegistries.LIGHT.get(), to, 5);
                for (LivingEntity e : Gx.around(l, to, 8, p)) { Gx.hurtNow(e, p, c.dmg(40)); Gx.knock(e, to, 3.0, 1.0); }
                Gx.scorch(l, to, 5.0, 8.0F);
                Gx.crater(l, to, 4.0, 8.0F);
                Gx.sound(l, to, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.6F);
                Gx.sound(l, from, SoundEvents.BEACON_DEACTIVATE, 1.5F, 0.5F);
            });
            return true;
        });
        Moves.register("sanshu_no_jingi", c -> {
            // the Three Sacred Treasures: Yasakani below launches everyone, Yata into the sky, Murakumo brings them down
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            Vec3 at = p.position();
            Vfx.sphere(l, Fx.LIGHT, at.add(0, 0.2, 0), 2.5, 8);
            Vfx.ring(l, Fx.LIGHT, at.add(0, 0.2, 0), 6, false);
            for (int i = 0; i < 12; i++) FruitProjectile.fire(l, p, Kind.LIGHT_BULLET, c.dmg(5), at.add(0, 0.5, 0), Vfx.randDir(l).multiply(1, 0.3, 1).add(0, 0.9, 0), 2.5F).life(20);
            for (LivingEntity e : Gx.around(l, at, 6, p)) { Gx.hurtNow(e, p, c.dmg(10)); e.setDeltaMovement(e.getDeltaMovement().add(0, 1.3, 0)); e.hurtMarked = true; }
            Gx.sound(l, at, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2F, 1.6F);
            Scheduler.schedule(8, () -> { if (p.isAlive()) teleportTo(c, p.position().add(0, 7, 0)); });
            Scheduler.schedule(14, () -> {
                if (!p.isAlive()) return;
                c.s().murakumoTicks = Math.max(c.s().murakumoTicks, 120);
                Vec3 top = p.position();
                Vec3 ground = Gx.ground(l, top);
                Gx.teleport(p, ground);
                p.resetFallDistance();
                Vfx.beam(l, Fx.LIGHT, top.add(0, 1, 0), ground.add(0, 1, 0), 0.3, 8);
                Vfx.arc(l, Fx.LIGHT, ground.add(0, 1, 0), p.getYRot(), 4, 360, 0.3, false, 12);
                Vfx.slam(l, Fx.LIGHT, ground, 5);
                for (LivingEntity e : Gx.around(l, ground, 6, p)) { Gx.hurtNow(e, p, c.dmg(24)); Gx.knock(e, ground, 2.0, 0.4); }
                Gx.sound(l, ground, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.4F);
            });
            return true;
        });
        Moves.register("solar_lance", c -> {
            ServerLevel l = c.level();
            ServerPlayer p = c.p();
            for (int t = 0; t < 24; t += 4) {
                final double r = 0.6 + t * 0.1;
                Scheduler.schedule(t, () -> { Vfx.sphere(l, Fx.LIGHT, p.getEyePosition().add(p.getLookAngle().scale(1.5)), r, 4); Vfx.dress(l, Fx.LIGHT, p.getEyePosition(), 1.5, 2); });
            }
            Gx.sound(l, p.position(), SoundEvents.BEACON_ACTIVATE, 1.5F, 1.5F);
            Scheduler.schedule(24, () -> {
                if (!p.isAlive()) return;
                FruitProjectile.fire(l, p, Kind.LIGHT_ORB, c.dmg(60), p.getEyePosition().add(p.getLookAngle().scale(1.5)), p.getLookAngle(), 2.0F).size(1.6F).life(60);
                Vfx.ring(l, Fx.LIGHT, p.getEyePosition(), 3, true);
                Gx.sound(l, p.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 1.6F);
            });
            return true;
        });
    }
}
