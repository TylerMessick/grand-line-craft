package com.messickmedia.grandline.client;

import com.messickmedia.grandline.GrandLine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * The azure dragon: a long serpentine body of chained segments that undulates, a horned head with a snout, jaw and
 * whiskers, four clawed legs, a mane of spines down the back and a tufted tail. Built from cubes, Minecraft style.
 */
public class DragonModel extends Model {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GrandLine.id("seiryu_dragon"), "main");

    private final ModelPart root;
    private final ModelPart neck1, neck2, neck3, head, jaw;
    private final ModelPart[] body = new ModelPart[4];
    private final ModelPart[] tail = new ModelPart[5];
    private final ModelPart legFL, legFR, legBL, legBR;

    public DragonModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        ModelPart b0 = root.getChild("body0");
        this.body[0] = b0;
        this.body[1] = b0.getChild("body1");
        this.body[2] = this.body[1].getChild("body2");
        this.body[3] = this.body[2].getChild("body3");
        this.tail[0] = this.body[3].getChild("tail0");
        for (int i = 1; i < 5; i++) this.tail[i] = this.tail[i - 1].getChild("tail" + i);
        this.neck1 = b0.getChild("neck1");
        this.neck2 = this.neck1.getChild("neck2");
        this.neck3 = this.neck2.getChild("neck3");
        this.head = this.neck3.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.legFL = b0.getChild("leg_fl");
        this.legFR = b0.getChild("leg_fr");
        this.legBL = this.body[2].getChild("leg_bl");
        this.legBR = this.body[2].getChild("leg_br");
    }

    /** Texture 128x128: azure scales 0,0 (64x64) · cream belly 64,0 · ivory horn 64,32 · dark blue mane 96,32 · red eye 64,48. */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // body chain, origin at the chest; the model floats at mid height, legs reach down
        PartDefinition b0 = root.addOrReplaceChild("body0", seg(10, 0), PartPose.offset(0.0F, 10.0F, 0.0F));
        PartDefinition b1 = b0.addOrReplaceChild("body1", seg(10, 10), PartPose.offset(0.0F, 0.0F, 10.0F));
        PartDefinition b2 = b1.addOrReplaceChild("body2", seg(10, 20), PartPose.offset(0.0F, 0.0F, 10.0F));
        PartDefinition b3 = b2.addOrReplaceChild("body3", seg(9, 30), PartPose.offset(0.0F, 0.0F, 10.0F));
        PartDefinition t = b3.addOrReplaceChild("tail0", seg(7, 40), PartPose.offset(0.0F, 0.0F, 9.0F));
        int[] sizes = {6, 5, 4, 3};
        for (int i = 1; i < 5; i++) {
            CubeListBuilder cb = seg(sizes[i - 1], 40 + i * 6);
            if (i == 4) cb.texOffs(96, 32).addBox(-0.5F, -6.0F, 4.0F, 1.0F, 10.0F, 6.0F); // tail fin
            t = t.addOrReplaceChild("tail" + i, cb, PartPose.offset(0.0F, 0.0F, sizes[i - 1] + 1.0F));
        }
        // neck and head, forward (-z)
        PartDefinition n1 = b0.addOrReplaceChild("neck1", seg(8, 60).texOffs(96, 32).addBox(-1.0F, -8.0F, 0.0F, 2.0F, 4.0F, 8.0F), PartPose.offset(0.0F, -1.0F, -8.0F));
        PartDefinition n2 = n1.addOrReplaceChild("neck2", seg(8, 60).texOffs(96, 32).addBox(-1.0F, -8.0F, 0.0F, 2.0F, 4.0F, 8.0F), PartPose.offset(0.0F, 0.0F, -8.0F));
        PartDefinition n3 = n2.addOrReplaceChild("neck3", seg(8, 60).texOffs(96, 32).addBox(-1.0F, -8.0F, 0.0F, 2.0F, 4.0F, 8.0F), PartPose.offset(0.0F, 0.0F, -8.0F));
        PartDefinition head = n3.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, -6.0F, -10.0F, 12.0F, 10.0F, 12.0F)               // skull
                .texOffs(0, 22).addBox(-4.0F, -3.0F, -18.0F, 8.0F, 5.0F, 8.0F)                   // snout
                .texOffs(64, 32).addBox(-5.5F, -14.0F, -4.0F, 2.0F, 9.0F, 2.0F)                  // horns
                .texOffs(64, 32).addBox(3.5F, -14.0F, -4.0F, 2.0F, 9.0F, 2.0F)
                .texOffs(64, 32).addBox(-5.0F, -18.0F, -2.0F, 1.0F, 5.0F, 1.0F)
                .texOffs(64, 32).addBox(4.0F, -18.0F, -2.0F, 1.0F, 5.0F, 1.0F)
                .texOffs(64, 48).addBox(-6.5F, -4.0F, -8.0F, 1.0F, 2.0F, 2.0F)                   // eyes
                .texOffs(64, 48).addBox(5.5F, -4.0F, -8.0F, 1.0F, 2.0F, 2.0F)
                .texOffs(96, 32).addBox(-1.0F, -9.0F, -2.0F, 2.0F, 3.0F, 10.0F)                  // crest
                .texOffs(64, 32).addBox(-9.0F, -1.0F, -17.0F, 5.0F, 1.0F, 1.0F)                  // whiskers
                .texOffs(64, 32).addBox(4.0F, -1.0F, -17.0F, 5.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, -4.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 36).addBox(-3.5F, 0.0F, -8.0F, 7.0F, 2.0F, 9.0F), PartPose.offset(0.0F, 2.0F, -9.0F));
        // legs
        b0.addOrReplaceChild("leg_fl", leg(), PartPose.offset(5.0F, 3.0F, -3.0F));
        b0.addOrReplaceChild("leg_fr", leg(), PartPose.offset(-5.0F, 3.0F, -3.0F));
        b2.addOrReplaceChild("leg_bl", leg(), PartPose.offset(5.0F, 3.0F, 3.0F));
        b2.addOrReplaceChild("leg_br", leg(), PartPose.offset(-5.0F, 3.0F, 3.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** One body segment: a scaled box with a lighter belly plate and a spine. */
    private static CubeListBuilder seg(int size, int v) {
        float h = size / 2.0F;
        return CubeListBuilder.create()
                .texOffs(0, 0).addBox(-h, -h, 0.0F, size, size, size)
                .texOffs(64, 0).addBox(-h + 1.0F, h - 1.0F, 0.5F, size - 2, 1.5F, size - 1)
                .texOffs(96, 32).addBox(-1.0F, -h - 3.0F, 1.0F, 2.0F, 3.0F, Math.max(2, size - 3));
    }

    private static CubeListBuilder leg() {
        return CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F)
                .texOffs(64, 32).addBox(-2.5F, 8.0F, -4.0F, 1.0F, 2.0F, 3.0F)
                .texOffs(64, 32).addBox(-0.5F, 8.0F, -4.0F, 1.0F, 2.0F, 3.0F)
                .texOffs(64, 32).addBox(1.5F, 8.0F, -4.0F, 1.0F, 2.0F, 3.0F);
    }

    /** Procedural animation: a travelling wave down the spine, the head following the look, legs paddling in flight. */
    public void setup(float age, float speed, float pitchDeg, boolean flying) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        float w = age * 0.12F;
        float amp = 0.10F + Math.min(0.12F, speed * 0.4F);
        for (int i = 0; i < 4; i++) {
            this.body[i].yRot = Mth.sin(w - i * 0.7F) * amp;
            this.body[i].xRot = Mth.cos(w * 0.9F - i * 0.7F) * amp * 0.5F;
        }
        for (int i = 0; i < 5; i++) {
            this.tail[i].yRot = Mth.sin(w - (4 + i) * 0.7F) * (amp + 0.05F);
            this.tail[i].xRot = Mth.cos(w * 0.9F - (4 + i) * 0.7F) * amp * 0.6F;
        }
        float pitch = pitchDeg * Mth.DEG_TO_RAD;
        this.neck1.yRot = Mth.sin(w + 0.7F) * amp * 0.6F;
        this.neck1.xRot = pitch * 0.25F - 0.15F;
        this.neck2.xRot = pitch * 0.25F - 0.05F + Mth.sin(age * 0.08F) * 0.04F;
        this.neck3.xRot = pitch * 0.25F + 0.2F;
        this.head.xRot = pitch * 0.25F;
        this.jaw.xRot = 0.1F + 0.08F * Mth.sin(age * 0.07F) + (flying ? 0.05F : 0.0F);
        float paddle = flying ? Mth.sin(age * 0.2F) * 0.5F : Mth.sin(age * 0.5F) * speed * 2.0F;
        this.legFL.xRot = 0.6F + paddle;
        this.legFR.xRot = 0.6F - paddle;
        this.legBL.xRot = 0.6F - paddle;
        this.legBR.xRot = 0.6F + paddle;
        if (flying) {
            this.body[0].xRot += pitch * 0.5F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, int color) {
        this.root.render(ps, vc, light, overlay, color);
    }
}
