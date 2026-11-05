package com.messickmedia.grandline.fruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Everything a move needs: who used it, their state, the level, the output multiplier and the move itself. */
public record Cast(ServerPlayer p, Pirate s, ServerLevel level, float power, Move move) {
    public float dmg(float base) { return base * this.power; }
    public Vec3 eye() { return this.p.getEyePosition(); }
    public Vec3 look() { return this.p.getLookAngle(); }
    public Vec3 pos() { return this.p.position(); }
    /** Flat look direction. */
    public Vec3 flat() { Vec3 l = this.look(); Vec3 f = new Vec3(l.x, 0, l.z); return f.lengthSqr() < 1.0E-4D ? new Vec3(0, 0, 1) : f.normalize(); }
    /** A point in front of the caster at hand height. */
    public Vec3 hand() { return this.p.getEyePosition().add(this.p.getLookAngle().scale(0.8D)).add(0, -0.35D, 0); }
    /** Horizontal right vector. */
    public Vec3 right() { Vec3 l = this.look(); return new Vec3(-l.z, 0, l.x).normalize(); }
    public float scale() { return (float) this.p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE); }
}
