package com.messickmedia.grandline.client;

import com.messickmedia.grandline.GrandLine;
import com.messickmedia.grandline.entity.FruitProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

/**
 * Light bullets and orbs are glowing cubes with a halo; wind scythes are a flat spinning crescent slab; fireballs
 * tumble; fists are solid blocky knuckles that face their flight.
 */
public class ProjectileRenderer extends EntityRenderer<FruitProjectile> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GrandLine.id("fruit_projectile"), "main");
    private static final ResourceLocation TEX = GrandLine.id("textures/entity/projectile.png");

    private final ModelPart core, shell, fist, knuckles, blade;

    public ProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = context.bakeLayer(LAYER);
        this.core = root.getChild("core");
        this.shell = root.getChild("shell");
        this.fist = root.getChild("fist");
        this.knuckles = root.getChild("knuckles");
        this.blade = root.getChild("blade");
        this.shadowRadius = 0.0F;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        // texture: 64x64. rows: 0-15 light, 16-31 rubber skin, 32-47 haki black, 48-63 wind/white
        mesh.getRoot().addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("shell", CubeListBuilder.create().texOffs(16, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("fist", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -4.0F, -3.0F, 8.0F, 8.0F, 6.0F), PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("knuckles", CubeListBuilder.create().texOffs(28, 16).addBox(-4.0F, -4.5F, -5.0F, 8.0F, 3.0F, 3.0F)
                .texOffs(28, 22).addBox(-4.5F, -1.0F, -1.0F, 2.0F, 5.0F, 4.0F), PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("blade", CubeListBuilder.create().texOffs(0, 48).addBox(-8.0F, -0.5F, -3.0F, 16.0F, 1.0F, 6.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(FruitProjectile p, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        FruitProjectile.Kind kind = p.kind();
        float t = p.tickCount + partialTick;
        float scale = kind.scale * p.sizeMul();
        pose.pushPose();
        pose.translate(0.0F, 0.25F * scale, 0.0F);
        switch (kind) {
            case KAIFU -> {
                pose.mulPose(Axis.YP.rotationDegrees(p.getYRot()));
                pose.mulPose(Axis.XP.rotationDegrees(-p.getXRot()));
                pose.mulPose(Axis.ZP.rotationDegrees(t * 40.0F));
                pose.scale(scale, scale, scale);
                this.blade.render(pose, buffer.getBuffer(RenderType.entityTranslucentEmissive(TEX)), 0xF000F0, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(200, 255, 255, 255));
                pose.scale(1.3F, 1.0F, 1.3F);
                this.blade.render(pose, buffer.getBuffer(RenderType.entityTranslucentEmissive(TEX)), 0xF000F0, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(70, 255, 255, 255));
            }
            case FIST, HAKI_FIST, GIANT_FIST, ELEPHANT_FIST, NIKA_FIST -> {
                pose.mulPose(Axis.YP.rotationDegrees(p.getYRot() + 180.0F));
                pose.mulPose(Axis.XP.rotationDegrees(p.getXRot()));
                pose.scale(scale, scale, scale);
                int color = kind.isHaki() ? 0xFF1C1C22 : kind == FruitProjectile.Kind.NIKA_FIST ? 0xFFFFFFFF : 0xFFE8A77A;
                if (kind == FruitProjectile.Kind.NIKA_FIST) color = 0xFFFFFFFF;
                var vc = buffer.getBuffer(RenderType.entitySolid(TEX));
                this.fist.render(pose, vc, light, OverlayTexture.NO_OVERLAY, color);
                this.knuckles.render(pose, vc, light, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.multiply(color, 0xFFBBBBBB));
                if (kind.isHaki()) {
                    pose.scale(1.08F, 1.08F, 1.08F);
                    this.fist.render(pose, buffer.getBuffer(RenderType.entityTranslucentEmissive(TEX)), 0xF000F0, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(70, 255, 40, 60));
                }
            }
            case FIREBALL -> {
                pose.scale(scale, scale, scale);
                pose.mulPose(Axis.YP.rotationDegrees(t * 23.0F));
                pose.mulPose(Axis.XP.rotationDegrees(t * 31.0F));
                this.core.render(pose, buffer.getBuffer(RenderType.entityTranslucentEmissive(TEX)), 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFB020);
                pose.mulPose(Axis.ZP.rotationDegrees(t * -17.0F));
                this.shell.render(pose, buffer.getBuffer(RenderType.entityTranslucentEmissive(TEX)), 0xF000F0, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(170, 255, 90, 30));
                float pulse = 1.35F + 0.15F * (float) Math.sin(t * 0.6F);
                pose.scale(pulse, pulse, pulse);
                this.shell.render(pose, buffer.getBuffer(RenderType.entityTranslucentEmissive(TEX)), 0xF000F0, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(60, 255, 60, 20));
            }
            default -> {
                pose.scale(scale, scale, scale);
                pose.mulPose(Axis.YP.rotationDegrees(t * 23.0F));
                pose.mulPose(Axis.XP.rotationDegrees(t * 31.0F));
                this.core.render(pose, buffer.getBuffer(RenderType.entityTranslucentEmissive(TEX)), 0xF000F0, OverlayTexture.NO_OVERLAY);
                pose.mulPose(Axis.ZP.rotationDegrees(t * -17.0F));
                this.shell.render(pose, buffer.getBuffer(RenderType.entityTranslucentEmissive(TEX)), 0xF000F0, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(170, 255, 240, 150));
                float pulse = 1.35F + 0.15F * (float) Math.sin(t * 0.6F);
                pose.scale(pulse, pulse, pulse);
                this.shell.render(pose, buffer.getBuffer(RenderType.entityTranslucentEmissive(TEX)), 0xF000F0, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(60, 255, 240, 150));
            }
        }
        pose.popPose();
        super.render(p, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(FruitProjectile p) {
        return TEX;
    }
}
