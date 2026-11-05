package com.messickmedia.grandline.fruit;

import com.messickmedia.grandline.Config;
import com.messickmedia.grandline.GrandLine;
import com.messickmedia.grandline.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/** Shared helpers: damage, targeting, areas, motion, terrain. */
public final class Gx {
    private Gx() {}

    /** Ordinary devil fruit damage. */
    public static final ResourceKey<DamageType> FRUIT = ResourceKey.create(Registries.DAMAGE_TYPE, GrandLine.id("fruit"));
    /** Armament-hardened damage: ignores a logia's intangibility and armor partially. */
    public static final ResourceKey<DamageType> HAKI = ResourceKey.create(Registries.DAMAGE_TYPE, GrandLine.id("haki"));

    public static Pirate data(Player p) {
        return p.getData(ModRegistries.PIRATE.get());
    }

    // ---------------------------------------------------------------- damage

    public static DamageSource source(Level level, ResourceKey<DamageType> key, @Nullable Entity direct, @Nullable Entity owner) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key), direct, owner);
    }

    private static float scaled(float amount) {
        return amount * (float) (double) Config.DAMAGE.get();
    }

    public static boolean hurt(LivingEntity target, @Nullable Entity owner, float amount) {
        return target.hurt(source(target.level(), FRUIT, owner, owner), scaled(amount));
    }

    public static boolean hurt(LivingEntity target, @Nullable Entity direct, @Nullable Entity owner, float amount) {
        return target.hurt(source(target.level(), FRUIT, direct, owner), scaled(amount));
    }

    /** Ignores hurt invulnerability frames, for rapid multi-hits (Gatling). */
    public static boolean hurtNow(LivingEntity target, @Nullable Entity owner, float amount) {
        target.invulnerableTime = 0;
        return hurt(target, owner, amount);
    }

    /** Haki-hardened hit. */
    public static boolean haki(LivingEntity target, @Nullable Entity owner, float amount) {
        target.invulnerableTime = 0;
        return target.hurt(source(target.level(), HAKI, owner, owner), scaled(amount));
    }

    // ---------------------------------------------------------------- areas

    public static boolean isAlly(Entity e, @Nullable Entity owner) {
        if (owner == null) return false;
        if (e == owner) return true;
        if (e instanceof net.minecraft.world.entity.TamableAnimal t && t.getOwner() == owner) return true;
        return owner instanceof Player a && e instanceof Player b && a.isAlliedTo(b);
    }

    public static List<LivingEntity> around(ServerLevel level, Vec3 c, double r, @Nullable Entity owner) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && !isAlly(e, owner) && e.distanceToSqr(c) <= r * r);
    }

    public static Vec3 chest(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.55D, 0);
    }

    @Nullable
    public static LivingEntity lookTarget(ServerPlayer player, double range) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(range));
        HitResult block = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) to = block.getLocation();
        AABB box = player.getBoundingBox().expandTowards(to.subtract(from)).inflate(1.5D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, from, to, box,
                e -> e instanceof LivingEntity l && l.isAlive() && !e.isSpectator() && !isAlly(e, player));
        return hit != null ? (LivingEntity) hit.getEntity() : null;
    }

    /** Nearest enemy in a loose cone in front of the player, for homing and auto-aim. */
    @Nullable
    public static LivingEntity aimAssist(ServerPlayer player, double range) {
        LivingEntity direct = lookTarget(player, range);
        if (direct != null) return direct;
        Vec3 look = player.getLookAngle();
        return around(player.serverLevel(), player.getEyePosition(), range, player).stream()
                .filter(e -> e instanceof net.minecraft.world.entity.monster.Enemy || e instanceof Player || e instanceof Mob)
                .filter(e -> chest(e).subtract(player.getEyePosition()).normalize().dot(look) > 0.8D)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }

    public static Vec3 rayEnd(ServerPlayer player, double range) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(range));
        return player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
    }

    public static Vec3 rayEnd(Level level, Vec3 from, Vec3 dir, double range, @Nullable Entity ignore) {
        Vec3 to = from.add(dir.normalize().scale(range));
        HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ignore == null ? CollisionContext.empty() : CollisionContext.of(ignore)));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    public static Vec3 lookPoint(ServerPlayer player, double range) {
        LivingEntity t = lookTarget(player, range);
        return t != null ? chest(t) : rayEnd(player, range);
    }

    /** The ground under a point (the top of the first solid block below, up to 32 blocks down). */
    public static Vec3 ground(Level level, Vec3 p) {
        HitResult hit = level.clip(new ClipContext(p.add(0, 0.5D, 0), p.add(0, -32, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? p : hit.getLocation();
    }

    /** Entities along a line, nearest first. */
    public static List<LivingEntity> line(ServerLevel level, Vec3 from, Vec3 to, double width, @Nullable Entity owner) {
        AABB box = new AABB(from, to).inflate(width);
        Vec3 d = to.subtract(from);
        double len2 = Math.max(1.0E-6D, d.lengthSqr());
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (!e.isAlive() || e.isSpectator() || isAlly(e, owner)) return false;
            Vec3 c = chest(e);
            double t = Math.max(0, Math.min(1, c.subtract(from).dot(d) / len2));
            return c.distanceTo(from.add(d.scale(t))) <= width + e.getBbWidth() * 0.5D;
        }).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(from))).toList();
    }

    public static List<LivingEntity> cone(ServerPlayer player, double range, double minDot) {
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        return around(player.serverLevel(), eye, range, player).stream()
                .filter(e -> chest(e).subtract(eye).normalize().dot(look) >= minDot).toList();
    }

    // ---------------------------------------------------------------- motion

    public static void knock(Entity e, Vec3 from, double strength, double up) {
        Vec3 d = e.position().subtract(from);
        d = new Vec3(d.x, 0, d.z);
        if (d.lengthSqr() < 1.0E-4D) d = new Vec3(0, 0, 1);
        d = d.normalize().scale(strength);
        double resist = e instanceof LivingEntity l ? Math.max(0.0D, 1.0D - l.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.7D) : 1.0D;
        e.setDeltaMovement(e.getDeltaMovement().add(d.x * resist, up * resist, d.z * resist));
        e.hurtMarked = true;
    }

    /** Launch an entity along a direction (no resistance check for the caster's own dashes). */
    public static void launch(Entity e, Vec3 dir, double speed) {
        e.setDeltaMovement(dir.normalize().scale(speed));
        e.hurtMarked = true;
    }

    public static void pull(Entity e, Vec3 to, double strength) {
        Vec3 d = to.subtract(e.position().add(0, e.getBbHeight() * 0.5D, 0));
        double dist = d.length();
        if (dist < 0.3D) return;
        e.setDeltaMovement(e.getDeltaMovement().scale(0.6D).add(d.normalize().scale(Math.min(strength, dist * 0.25D))));
        e.hurtMarked = true;
    }

    public static void teleport(Entity e, Vec3 p) {
        Vec3 v = e.getDeltaMovement();
        e.teleportTo(p.x, p.y, p.z);
        e.setDeltaMovement(v);
        e.hurtMarked = true;
        e.resetFallDistance();
    }

    // ---------------------------------------------------------------- terrain

    public static boolean griefing(ServerLevel level) {
        return Config.BLOCK_DAMAGE.get() && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    /** Breaks breakable blocks in a sphere (no drops). Respects the config and the mobGriefing rule. */
    public static void crater(ServerLevel level, Vec3 c, double r, float maxHardness) {
        if (!griefing(level)) return;
        BlockPos center = BlockPos.containing(c);
        int ir = (int) Math.ceil(r);
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-ir, -ir, -ir), center.offset(ir, ir, ir))) {
            if (p.distToCenterSqr(c.x, c.y, c.z) > r * r) continue;
            BlockState s = level.getBlockState(p);
            float h = s.getDestroySpeed(level, p);
            if (!s.isAir() && h >= 0.0F && h <= maxHardness && s.getFluidState().isEmpty()) level.destroyBlock(p, false);
        }
    }

    /** Burns a crater: breaks soft blocks and sets fire around it. */
    public static void scorch(ServerLevel level, Vec3 c, double r, float maxHardness) {
        crater(level, c, r, maxHardness);
        if (!griefing(level)) return;
        BlockPos center = BlockPos.containing(c);
        int ir = (int) Math.ceil(r) + 1;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-ir, -2, -ir), center.offset(ir, 2, ir))) {
            if (level.random.nextFloat() > 0.25F) continue;
            if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isSolidRender(level, p.below())) {
                level.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
            }
        }
    }

    /** Cuts soft blocks (glass, leaves, plants, wool...) along a line. */
    public static void cut(ServerLevel level, Vec3 from, Vec3 to, float maxHardness) {
        if (!griefing(level)) return;
        Vec3 d = to.subtract(from);
        int n = (int) (d.length() * 2);
        for (int i = 0; i <= n; i++) {
            BlockPos p = BlockPos.containing(from.add(d.scale(i / (double) Math.max(1, n))));
            BlockState s = level.getBlockState(p);
            float h = s.getDestroySpeed(level, p);
            if (!s.isAir() && h >= 0.0F && h <= maxHardness && s.getFluidState().isEmpty()) level.destroyBlock(p, false);
        }
    }

    // ---------------------------------------------------------------- misc

    public static void sound(ServerLevel level, Vec3 p, SoundEvent s, float volume, float pitch) {
        level.playSound(null, p.x, p.y, p.z, s, SoundSource.PLAYERS, volume, pitch);
    }

    public static void effect(LivingEntity e, Holder<MobEffect> effect, int ticks, int amp) {
        e.addEffect(new MobEffectInstance(effect, ticks, amp, false, false, true));
    }

    public static void msg(ServerPlayer p, Component c) {
        p.displayClientMessage(c, true);
    }

    /** Root a target in place: movement, jumping and AI navigation all stop. */
    public static void root(LivingEntity e, int ticks) {
        effect(e, MobEffects.MOVEMENT_SLOWDOWN, ticks, 9);
        e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
        e.hurtMarked = true;
        if (e instanceof Mob m) m.getNavigation().stop();
    }

    public static float yaw(Vec3 dir) {
        return (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
    }
}
