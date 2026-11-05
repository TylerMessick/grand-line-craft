package com.messickmedia.grandline.entity;

import com.messickmedia.grandline.fruit.Gx;
import com.messickmedia.grandline.fruit.Vfx;
import com.messickmedia.grandline.net.Fx;
import com.messickmedia.grandline.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Every thrown or fired devil fruit attack: Yasakani light bullets, Kaifu wind scythes, Bolo Breath fireballs,
 * Culverin homing fists (drawn with a rubber arm back to the owner) and the King Kong Gun.
 */
public class FruitProjectile extends Projectile {
    public enum Kind {
        LIGHT_BULLET(0.35F, 0, false, Fx.LIGHT), KAIFU(1.2F, 0, false, Fx.WIND), FIREBALL(1.1F, 0, false, Fx.FIRE),
        FIST(0.9F, 0, true, Fx.RUBBER), HAKI_FIST(1.2F, 0, true, Fx.HAKI), GIANT_FIST(3.0F, 0, false, Fx.RUBBER), ELEPHANT_FIST(3.2F, 0, false, Fx.HAKI), NIKA_FIST(6.0F, 0, false, Fx.NIKA),
        LIGHT_ORB(0.8F, 0, false, Fx.LIGHT);

        public final float scale, gravity;
        public final boolean homing;
        public final int style;
        Kind(float scale, float gravity, boolean homing, int style) { this.scale = scale; this.gravity = gravity; this.homing = homing; this.style = style; }
        public boolean isFist() { return this == FIST || this == HAKI_FIST || this == GIANT_FIST || this == ELEPHANT_FIST || this == NIKA_FIST; }
        public boolean isHaki() { return this == HAKI_FIST || this == ELEPHANT_FIST || this == NIKA_FIST; }
    }

    public static final int FLAG_PIERCE = 1, FLAG_FIRE = 2, FLAG_BOOM = 4, FLAG_CUT = 8;

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(FruitProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(FruitProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(FruitProjectile.class, EntityDataSerializers.INT);

    private float damage = 6.0F;
    private int life = 80;
    private int flags;
    private int homingTarget = -1;
    private final Set<Integer> hit = new HashSet<>();

    public FruitProjectile(EntityType<? extends FruitProjectile> type, Level level) {
        super(type, level);
    }

    public static FruitProjectile shoot(ServerLevel level, LivingEntity owner, Kind kind, float damage, float speed) {
        Vec3 look = owner.getLookAngle();
        Vec3 from = owner.getEyePosition().add(look.scale(1.0D)).add(0, -0.25D, 0);
        return fire(level, owner, kind, damage, from, look, speed);
    }

    public static FruitProjectile fire(ServerLevel level, @Nullable Entity owner, Kind kind, float damage, Vec3 from, Vec3 dir, float speed) {
        FruitProjectile p = new FruitProjectile(ModRegistries.PROJECTILE.get(), level);
        p.setOwner(owner);
        p.entityData.set(KIND, kind.ordinal());
        p.entityData.set(OWNER_ID, owner == null ? -1 : owner.getId());
        p.damage = damage;
        p.life = kind == Kind.FIST || kind == Kind.HAKI_FIST ? 60 : 80;
        p.setPos(from.x, from.y, from.z);
        p.setDeltaMovement(dir.normalize().scale(speed));
        ProjectileUtil.rotateTowardsMovement(p, 1.0F);
        level.addFreshEntity(p);
        return p;
    }

    public FruitProjectile flags(int f) { this.flags |= f; return this; }
    public FruitProjectile size(float s) { this.entityData.set(SIZE, s); return this; }
    public FruitProjectile homing(@Nullable Entity target) { this.homingTarget = target == null ? -1 : target.getId(); return this; }
    public FruitProjectile life(int ticks) { this.life = ticks; return this; }

    public Kind kind() { return Kind.values()[Math.floorMod(this.entityData.get(KIND), Kind.values().length)]; }
    public float sizeMul() { return this.entityData.get(SIZE); }
    public int ownerId() { return this.entityData.get(OWNER_ID); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, 0);
        builder.define(SIZE, 1.0F);
        builder.define(OWNER_ID, -1);
    }

    @Override
    public boolean isNoGravity() { return true; }

    @Override
    public void tick() {
        super.tick();
        Kind kind = this.kind();
        Vec3 v = this.getDeltaMovement();
        if (this.level() instanceof ServerLevel level) {
            if (--this.life <= 0) {
                this.impact(level, this.position(), null);
                this.discard();
                return;
            }
            if (this.homingTarget >= 0) {
                Entity t = level.getEntity(this.homingTarget);
                if (t != null && t.isAlive()) {
                    Vec3 want = Gx.chest(t).subtract(this.position()).normalize().scale(v.length());
                    v = v.lerp(want, 0.2D);
                }
            }
            this.setDeltaMovement(v);
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                this.onHit(hit);
                if (this.isRemoved()) return;
            }
            if ((this.flags & FLAG_CUT) != 0 && this.tickCount % 2 == 0) Gx.cut(level, this.position(), this.position().add(v), 1.5F);
            if (kind == Kind.KAIFU && this.tickCount % 2 == 0) {
                Vfx.arc(level, Fx.WIND, this.position(), Gx.yaw(v) + 90, 1.4 * this.sizeMul(), 140, 0.5, true, 6);
            }
            if (kind == Kind.LIGHT_BULLET && this.tickCount % 2 == 0) Vfx.line(level, Fx.LIGHT, this.position(), this.position().subtract(v.scale(1.5)));
            if (kind == Kind.FIREBALL) { Vfx.dress(level, Fx.FIRE, this.position(), 0.6 * this.sizeMul(), 1.2); if (this.tickCount % 3 == 0) Vfx.puff(level, Fx.FIRE, this.position(), 0.8 * this.sizeMul(), 2); }
            if (kind.isFist() && this.tickCount % 2 == 0) {
                Entity o = this.getOwner();
                if (o != null) Vfx.arm(level, kind.style, o.getEyePosition().add(0, -0.4, 0), this.position(), 0.22 * kind.scale * this.sizeMul(), 3);
                else Vfx.sphere(level, kind.style, this.position(), 0.45 * kind.scale * this.sizeMul(), 3);
            }
            if (kind == Kind.LIGHT_ORB && this.tickCount % 2 == 0) Vfx.sphere(level, Fx.LIGHT, this.position(), 0.5 * this.sizeMul(), 4);
        } else {
            this.trail(kind, v);
        }
        this.setPos(this.getX() + v.x, this.getY() + v.y, this.getZ() + v.z);
        ProjectileUtil.rotateTowardsMovement(this, 0.5F);
    }

    private void trail(Kind kind, Vec3 v) {
        Level level = this.level();
        float s = kind.scale * this.sizeMul();
        int n = Math.max(1, (int) (s * 3));
        var type = switch (kind) {
            case LIGHT_BULLET, LIGHT_ORB -> ModRegistries.LIGHT.get();
            case KAIFU -> ModRegistries.WIND.get();
            case FIREBALL -> ParticleTypes.FLAME;
            case HAKI_FIST, ELEPHANT_FIST -> ModRegistries.HAKI.get();
            case NIKA_FIST -> ParticleTypes.END_ROD;
            default -> ModRegistries.RUBBER.get();
        };
        for (int i = 0; i < n; i++) {
            double ox = (level.random.nextDouble() - 0.5D) * s, oy = (level.random.nextDouble() - 0.5D) * s, oz = (level.random.nextDouble() - 0.5D) * s;
            level.addParticle(type, this.getX() + ox, this.getY() + 0.25D * s + oy, this.getZ() + oz, -v.x * 0.08D, -v.y * 0.08D, -v.z * 0.08D);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !this.hit.contains(target.getId()) && !Gx.isAlly(target, this.getOwner());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level() instanceof ServerLevel level && result.getEntity() instanceof LivingEntity e) {
            this.hit.add(e.getId());
            this.impact(level, result.getLocation(), e);
            if ((this.flags & FLAG_PIERCE) == 0) this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel level) {
            this.impact(level, result.getLocation(), null);
            this.discard();
        }
    }

    private void impact(ServerLevel level, Vec3 at, @Nullable LivingEntity target) {
        Entity owner = this.getOwner();
        Kind k = this.kind();
        float dmg = this.damage;
        float size = this.sizeMul();
        if (target != null) {
            if (k.isHaki()) Gx.haki(target, owner, dmg);
            else Gx.hurtNow(target, this, owner, dmg);
        }
        switch (k) {
            case LIGHT_BULLET -> {
                Vfx.impact(level, Fx.LIGHT, at);
                for (LivingEntity e : Gx.around(level, at, 1.5D * size, owner)) if (e != target) Gx.hurtNow(e, this, owner, dmg * 0.5F);
                if (this.random.nextInt(3) == 0) level.playSound(null, BlockPos.containing(at), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.5F, 1.9F);
                if ((this.flags & FLAG_BOOM) != 0) { Vfx.blast(level, ModRegistries.LIGHT.get(), at, 1.2D); Gx.crater(level, at, 1.2D, 2.0F); }
            }
            case LIGHT_ORB -> {
                Vfx.sphere(level, Fx.LIGHT, at, 3.0D * size, 10);
                Vfx.ring(level, Fx.LIGHT, at, 4.0D * size, false);
                Vfx.blast(level, ModRegistries.LIGHT.get(), at, 2.5D * size);
                level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 1.5F);
                for (LivingEntity e : Gx.around(level, at, 4.0D * size, owner)) { Gx.hurtNow(e, this, owner, dmg); Gx.knock(e, at, 1.6D, 0.5D); Gx.effect(e, MobEffects.BLINDNESS, 60, 0); }
                Gx.crater(level, at, 2.5D * size, 5.0F);
            }
            case KAIFU -> {
                Vfx.arc(level, Fx.WIND, at, Gx.yaw(this.getDeltaMovement()) + 90, 1.8 * size, 180, 0.6, false, 12);
                for (LivingEntity e : Gx.around(level, at, 2.0D * size, owner)) if (e != target) Gx.hurtNow(e, this, owner, dmg * 0.6F);
                if (target != null) Gx.knock(target, at.subtract(this.getDeltaMovement()), 1.0D, 0.3D);
                level.playSound(null, BlockPos.containing(at), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.6F);
                Gx.cut(level, at.add(-1.5 * size, 0, 0), at.add(1.5 * size, 0, 0), 2.0F);
            }
            case FIREBALL -> {
                float power = 2.0F * size;
                Vfx.blast(level, ParticleTypes.FLAME, at, power);
                Vfx.sphere(level, Fx.FIRE, at, power * 1.2, 8);
                Vfx.impact(level, Fx.FIRE, at);
                Level.ExplosionInteraction mode = Gx.griefing(level) ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
                level.explode(this, Gx.source(level, Gx.FRUIT, this, owner), null, at.x, at.y, at.z, power, true, mode);
                for (LivingEntity e : Gx.around(level, at, power * 1.8D, owner)) {
                    Gx.hurtNow(e, this, owner, dmg * (float) Math.max(0.3D, 1.0D - e.distanceTo(this) / (power * 2.2)));
                    e.igniteForSeconds(8.0F);
                }
            }
            case FIST, HAKI_FIST -> {
                Vfx.impact(level, k.style, at);
                Vfx.ring(level, k.style, at, 1.5 * size, true);
                if (target != null) Gx.knock(target, this.position().subtract(this.getDeltaMovement()), 2.0D * size, 0.5D);
                level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.6F);
                if (k == Kind.HAKI_FIST) { Vfx.bolt(level, Fx.HAKI, at, at.add(Vfx.randDir(level).scale(1.5))); Gx.crater(level, at, 1.2D, 2.5F); }
            }
            case GIANT_FIST, ELEPHANT_FIST, NIKA_FIST -> {
                double r = (k == Kind.NIKA_FIST ? 7.0D : 4.0D) * size;
                Vfx.impact(level, k.style, at);
                Vfx.ring(level, k.style, at, r, false);
                Vfx.ring(level, k.style, at, r * 0.8, true);
                Vfx.slam(level, k.style, Gx.ground(level, at), r);
                Vfx.blast(level, k == Kind.NIKA_FIST ? ParticleTypes.END_ROD : ModRegistries.HAKI.get(), at, r * 0.6);
                level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 0.6F);
                level.playSound(null, BlockPos.containing(at), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0F, 1.4F);
                for (LivingEntity e : Gx.around(level, at, r, owner)) {
                    if (e != target) { float d = dmg * (float) Math.max(0.4D, 1.0D - e.distanceTo(this) / (r * 1.2)); if (k.isHaki()) Gx.haki(e, owner, d); else Gx.hurtNow(e, this, owner, d); }
                    Gx.knock(e, at, 3.0D, 1.1D);
                }
                Gx.crater(level, at, r * 0.7, k == Kind.NIKA_FIST ? 60.0F : 10.0F);
                if (k == Kind.NIKA_FIST) for (LivingEntity e : Gx.around(level, at, r * 1.6, owner)) { e.setDeltaMovement(e.getDeltaMovement().add(0, 1.4, 0)); e.hurtMarked = true; }
            }
        }
        if ((this.flags & FLAG_FIRE) != 0 && target != null) target.igniteForSeconds(4.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", this.entityData.get(KIND));
        tag.putFloat("Size", this.sizeMul());
        tag.putFloat("Damage", this.damage);
        tag.putInt("Life", this.life);
        tag.putInt("Flags", this.flags);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(KIND, tag.getInt("Kind"));
        this.entityData.set(SIZE, tag.contains("Size") ? tag.getFloat("Size") : 1.0F);
        this.damage = tag.getFloat("Damage");
        this.life = tag.getInt("Life");
        this.flags = tag.getInt("Flags");
    }
}
