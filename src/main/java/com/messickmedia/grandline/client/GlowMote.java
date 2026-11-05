package com.messickmedia.grandline.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Full-bright blocky mote; colour, size and life vary by type. */
public class GlowMote extends TextureSheetParticle {
    private final float startSize;

    protected GlowMote(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                       SpriteSet sprites, float r, float g, float b, float size, int life) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        float tint = 0.85F + this.random.nextFloat() * 0.15F;
        this.rCol = r * tint;
        this.gCol = g * tint;
        this.bCol = b * tint;
        this.startSize = size * (0.6F + this.random.nextFloat() * 0.8F);
        this.quadSize = this.startSize;
        this.lifetime = life + this.random.nextInt(Math.max(1, life / 2));
        this.hasPhysics = false;
        this.friction = 0.9F;
        this.gravity = 0.0F;
        this.alpha = 0.95F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float life = this.age / (float) this.lifetime;
        this.alpha = 0.95F * (1.0F - life * life);
        this.quadSize = this.startSize * (1.0F - life * 0.6F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public record Provider(SpriteSet sprites, float r, float g, float b, float size, int life) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new GlowMote(level, x, y, z, dx, dy, dz, this.sprites, this.r, this.g, this.b, this.size, this.life);
        }
    }
}
