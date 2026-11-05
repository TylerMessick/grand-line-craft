package com.messickmedia.grandline.client;

import com.messickmedia.grandline.GrandLine;
import com.messickmedia.grandline.net.ModNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * What a form looks like on the player: Gear Third's giant fists, Gear Fourth's inflated haki body with its flame
 * pattern, Gear Fifth's cloud wreath and white glow, the light body of the Pika user, and the horns, scales and tail
 * of the Seiryu hybrid. The full dragon is drawn by DragonRenderer instead of the player model.
 */
public class FormLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GrandLine.id("forms"), "main");
    private static final ResourceLocation TEX = GrandLine.id("textures/entity/forms.png");

    private final ModelPart fist, bodyShell, armShell, legShell, cloud, hornL, hornR, tail1, tail2, tail3, plate, mane, balloon;

    public FormLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, ModelPart root) {
        super(parent);
        this.fist = root.getChild("fist");
        this.bodyShell = root.getChild("body_shell");
        this.armShell = root.getChild("arm_shell");
        this.legShell = root.getChild("leg_shell");
        this.cloud = root.getChild("cloud");
        this.hornL = root.getChild("horn_l");
        this.hornR = root.getChild("horn_r");
        this.tail1 = root.getChild("tail1");
        this.tail2 = root.getChild("tail2");
        this.tail3 = root.getChild("tail3");
        this.plate = root.getChild("plate");
        this.mane = root.getChild("mane");
        this.balloon = root.getChild("balloon");
    }

    /** Texture regions (128x128): skin 0,0 · haki 0,32 · white 64,0 · azure 64,32 · ivory 0,64 · gold 64,64 · cream 0,96. */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("fist", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F)
                .texOffs(40, 0).addBox(-5.5F, -1.0F, -5.5F, 11.0F, 3.0F, 2.0F), PartPose.ZERO);
        root.addOrReplaceChild("body_shell", CubeListBuilder.create().texOffs(0, 32).addBox(-6.0F, -1.0F, -4.0F, 12.0F, 14.0F, 8.0F), PartPose.ZERO);
        root.addOrReplaceChild("arm_shell", CubeListBuilder.create().texOffs(0, 32).addBox(-3.0F, -2.5F, -3.0F, 6.0F, 13.0F, 6.0F), PartPose.ZERO);
        root.addOrReplaceChild("leg_shell", CubeListBuilder.create().texOffs(0, 32).addBox(-2.5F, -0.5F, -2.5F, 5.0F, 12.0F, 5.0F), PartPose.ZERO);
        root.addOrReplaceChild("cloud", CubeListBuilder.create().texOffs(64, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.ZERO);
        root.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(0, 64).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F)
                .texOffs(8, 64).addBox(-0.5F, -10.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(0, 64).addBox(-1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F)
                .texOffs(8, 64).addBox(-0.5F, -10.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(64, 32).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 7.0F), PartPose.ZERO);
        root.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(64, 32).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 7.0F), PartPose.ZERO);
        root.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(64, 32).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 7.0F)
                .texOffs(96, 32).addBox(-0.5F, -3.0F, 4.0F, 1.0F, 6.0F, 4.0F), PartPose.ZERO);
        root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(64, 32).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.ZERO);
        root.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(0, 96).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("balloon", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || player.isSleeping()) return;
        ModNetwork.FormView v = ClientState.view(player.getId());
        if (v == null) return;
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        VertexConsumer solid = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        float t = ageInTicks;

        switch (v.form()) {
            case "gear_third" -> {
                // bone balloon: one giant fist (the arm the player favours) and a swollen other hand
                fistOn(ps, solid, light, m.rightArm, 1.0F, 0xFFFFFFFF);
                fistOn(ps, solid, light, m.leftArm, 0.6F, 0xFFFFFFFF);
            }
            case "boundman" -> {
                int haki = 0xFF1C1C22;
                shell(ps, solid, light, m.body, this.bodyShell, 1.0F, haki);
                shell(ps, solid, light, m.rightArm, this.armShell, 1.0F, haki);
                shell(ps, solid, light, m.leftArm, this.armShell, 1.0F, haki);
                fistOn(ps, solid, light, m.rightArm, 0.7F, haki);
                fistOn(ps, solid, light, m.leftArm, 0.7F, haki);
                steamRing(ps, buf, m, t, 0.9F);
            }
            case "snakeman" -> {
                int haki = 0xFF1C1C22;
                shell(ps, solid, light, m.rightArm, this.armShell, 0.75F, haki);
                shell(ps, solid, light, m.leftArm, this.armShell, 0.75F, haki);
                shell(ps, solid, light, m.rightLeg, this.legShell, 0.9F, haki);
                shell(ps, solid, light, m.leftLeg, this.legShell, 0.9F, haki);
                steamRing(ps, buf, m, t, 0.6F);
            }
            case "gear_fifth" -> {
                // the wreath of clouds and the white glow of the Sun God
                VertexConsumer glow = buf.getBuffer(RenderType.entityTranslucentEmissive(TEX));
                ps.pushPose();
                m.body.translateAndRotate(ps);
                for (int i = 0; i < 10; i++) {
                    double a = i / 10.0 * Math.PI * 2 + t * 0.05;
                    float r = 0.42F + 0.04F * Mth.sin(t * 0.3F + i);
                    ps.pushPose();
                    ps.translate(Math.cos(a) * r, 0.02F + 0.03F * Mth.sin(t * 0.2F + i * 2), Math.sin(a) * r);
                    float s = 0.9F + 0.3F * Mth.sin(t * 0.25F + i * 1.3F);
                    ps.scale(s, s, s);
                    this.cloud.render(ps, solid, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                    ps.popPose();
                }
                ps.popPose();
                int white = FastColor.ARGB32.color(90, 255, 255, 255);
                shell(ps, glow, light, m.body, this.bodyShell, 1.0F, white);
                shell(ps, glow, light, m.rightArm, this.armShell, 0.9F, white);
                shell(ps, glow, light, m.leftArm, this.armShell, 0.9F, white);
                shell(ps, glow, light, m.rightLeg, this.legShell, 1.0F, white);
                shell(ps, glow, light, m.leftLeg, this.legShell, 1.0F, white);
            }
            case "seiryu_hybrid" -> {
                int azure = 0xFF3FA7D6;
                ps.pushPose();
                m.head.translateAndRotate(ps);
                for (int side = -1; side <= 1; side += 2) {
                    ps.pushPose();
                    ps.translate(side * 0.2F, -0.45F, 0.05F);
                    ps.mulPose(Axis.XP.rotationDegrees(-35.0F));
                    ps.mulPose(Axis.ZP.rotationDegrees(side * 18.0F));
                    (side < 0 ? this.hornL : this.hornR).render(ps, solid, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                    ps.popPose();
                }
                ps.popPose();
                // tail swaying behind
                ps.pushPose();
                m.body.translateAndRotate(ps);
                ps.translate(0.0F, 0.7F, 0.1F);
                ps.mulPose(Axis.XP.rotationDegrees(-25.0F + 8.0F * Mth.sin(t * 0.1F)));
                ps.mulPose(Axis.YP.rotationDegrees(12.0F * Mth.sin(t * 0.13F)));
                this.tail1.render(ps, solid, light, OverlayTexture.NO_OVERLAY, azure);
                ps.translate(0.0F, 0.0F, 0.42F);
                ps.mulPose(Axis.XP.rotationDegrees(-15.0F + 10.0F * Mth.sin(t * 0.1F + 1)));
                ps.mulPose(Axis.YP.rotationDegrees(14.0F * Mth.sin(t * 0.13F + 1)));
                this.tail2.render(ps, solid, light, OverlayTexture.NO_OVERLAY, azure);
                ps.translate(0.0F, 0.0F, 0.42F);
                ps.mulPose(Axis.XP.rotationDegrees(-10.0F + 12.0F * Mth.sin(t * 0.1F + 2)));
                this.tail3.render(ps, solid, light, OverlayTexture.NO_OVERLAY, azure);
                ps.popPose();
                // scale plates on the shoulders and back spines
                plateOn(ps, solid, light, m.rightArm, azure);
                plateOn(ps, solid, light, m.leftArm, azure);
                ps.pushPose();
                m.body.translateAndRotate(ps);
                for (int i = 0; i < 4; i++) {
                    ps.pushPose();
                    ps.translate(0.0F, 0.08F + i * 0.17F, 0.13F);
                    ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    this.mane.render(ps, solid, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                    ps.popPose();
                }
                ps.popPose();
            }
            default -> { }
        }

        if (v.fruit().equals("pika_pika") && v.toggle()) {
            VertexConsumer glow = buf.getBuffer(RenderType.entityTranslucentEmissive(TEX));
            int gold = FastColor.ARGB32.color(110, 255, 230, 110);
            shell(ps, glow, light, m.body, this.bodyShell, 0.95F, gold);
            shell(ps, glow, light, m.rightArm, this.armShell, 0.85F, gold);
            shell(ps, glow, light, m.leftArm, this.armShell, 0.85F, gold);
            shell(ps, glow, light, m.rightLeg, this.legShell, 0.95F, gold);
            shell(ps, glow, light, m.leftLeg, this.legShell, 0.95F, gold);
        }
        if ((v.flags() & ModNetwork.FormView.BALLOON) != 0) {
            float s = v.form().equals("gear_fifth") ? 2.2F : 1.3F;
            ps.pushPose();
            m.body.translateAndRotate(ps);
            ps.translate(0.0F, 0.38F, 0.0F);
            ps.scale(s, s * 0.95F, s);
            this.balloon.render(ps, buf.getBuffer(RenderType.entityTranslucentCull(TEX)), light, OverlayTexture.NO_OVERLAY,
                    v.form().equals("gear_fifth") ? FastColor.ARGB32.color(230, 255, 255, 255) : FastColor.ARGB32.color(235, 232, 167, 122));
            ps.popPose();
        }
        if ((v.flags() & ModNetwork.FormView.MURAKUMO) != 0) {
            // the blade of light in the right hand
            VertexConsumer glow = buf.getBuffer(RenderType.entityTranslucentEmissive(TEX));
            ps.pushPose();
            m.rightArm.translateAndRotate(ps);
            ps.translate(-0.06F, 0.62F, 0.0F);
            ps.mulPose(Axis.XP.rotationDegrees(-90.0F));
            ps.scale(0.18F, 0.18F, 1.6F);
            this.cloud.render(ps, glow, light, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(220, 255, 240, 160));
            ps.scale(1.6F, 1.6F, 1.05F);
            this.cloud.render(ps, glow, light, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color(60, 255, 240, 160));
            ps.popPose();
        }
    }

    private void fistOn(PoseStack ps, VertexConsumer vc, int light, ModelPart arm, float scale, int color) {
        ps.pushPose();
        arm.translateAndRotate(ps);
        ps.translate(0.0F, 0.72F, 0.0F);
        ps.scale(scale, scale, scale);
        this.fist.render(ps, vc, light, OverlayTexture.NO_OVERLAY, color);
        ps.popPose();
    }

    private void shell(PoseStack ps, VertexConsumer vc, int light, ModelPart part, ModelPart overlay, float scale, int color) {
        ps.pushPose();
        part.translateAndRotate(ps);
        ps.scale(scale, scale, scale);
        overlay.render(ps, vc, light, OverlayTexture.NO_OVERLAY, color);
        ps.popPose();
    }

    private void plateOn(PoseStack ps, VertexConsumer vc, int light, ModelPart arm, int color) {
        ps.pushPose();
        arm.translateAndRotate(ps);
        ps.translate(0.0F, -0.08F, 0.0F);
        this.plate.render(ps, vc, light, OverlayTexture.NO_OVERLAY, color);
        ps.popPose();
    }

    private void steamRing(PoseStack ps, MultiBufferSource buf, PlayerModel<AbstractClientPlayer> m, float t, float r) {
        VertexConsumer glow = buf.getBuffer(RenderType.entityTranslucentEmissive(TEX));
        ps.pushPose();
        m.body.translateAndRotate(ps);
        for (int i = 0; i < 6; i++) {
            double a = i / 6.0 * Math.PI * 2 + t * 0.08;
            ps.pushPose();
            ps.translate(Math.cos(a) * r * 0.5, 0.1F + 0.25F * ((t * 0.04F + i * 0.37F) % 1.0F), Math.sin(a) * r * 0.5);
            float s = 0.8F + 0.5F * ((t * 0.04F + i * 0.37F) % 1.0F);
            ps.scale(s, s, s);
            this.cloud.render(ps, glow, 0xF000F0, OverlayTexture.NO_OVERLAY, FastColor.ARGB32.color((int) (120 * (1.0F - ((t * 0.04F + i * 0.37F) % 1.0F))), 255, 255, 255));
            ps.popPose();
        }
        ps.popPose();
    }
}
