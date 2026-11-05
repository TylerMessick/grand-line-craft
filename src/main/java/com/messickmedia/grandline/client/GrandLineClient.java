package com.messickmedia.grandline.client;

import com.messickmedia.grandline.GrandLine;
import com.messickmedia.grandline.net.ModNetwork;
import com.messickmedia.grandline.registry.ModRegistries;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Client wiring: keys (Z R G H J Y moves, V form, B toggle), HUD, particles, renderers, form layers, 3D effects. */
@Mod(value = GrandLine.MODID, dist = Dist.CLIENT)
public class GrandLineClient {
    private static final String CATEGORY = "key.categories.grandline";
    public static final KeyMapping[] KEYS = {
            key("move1", GLFW.GLFW_KEY_Z), key("move2", GLFW.GLFW_KEY_R), key("move3", GLFW.GLFW_KEY_G),
            key("move4", GLFW.GLFW_KEY_H), key("move5", GLFW.GLFW_KEY_J), key("move6", GLFW.GLFW_KEY_Y),
            key("form", GLFW.GLFW_KEY_V), key("toggle", GLFW.GLFW_KEY_B), key("page", GLFW.GLFW_KEY_U)};

    private static final ResourceLocation DRAGON_TEX = GrandLine.id("textures/entity/seiryu_dragon.png");
    private static DragonModel dragon;

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.grandline." + name, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    public GrandLineClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(GrandLineClient::keys);
        modBus.addListener(GrandLineClient::particles);
        modBus.addListener(GrandLineClient::renderers);
        modBus.addListener(GrandLineClient::layerDefinitions);
        modBus.addListener(GrandLineClient::addLayers);
        modBus.addListener(GrandLineClient::hud);
        NeoForge.EVENT_BUS.addListener(GrandLineClient::tick);
        NeoForge.EVENT_BUS.addListener(FxClient::render);
        NeoForge.EVENT_BUS.addListener(GrandLineClient::renderPlayer);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { FxClient.clear(); ClientState.clear(); });

        ModNetwork.ClientSink.sync = ClientState::accept;
        ModNetwork.ClientSink.form = ClientState::accept;
        ModNetwork.ClientSink.fx = FxClient::handle;
        ModNetwork.ClientSink.stretch = FxClient::stretch;
    }

    static void keys(RegisterKeyMappingsEvent e) {
        for (KeyMapping k : KEYS) e.register(k);
    }

    private static void mote(RegisterParticleProvidersEvent e, SimpleParticleType type, float r, float g, float b, float size, int life) {
        e.registerSpriteSet(type, sprites -> new GlowMote.Provider(sprites, r, g, b, size, life));
    }

    static void particles(RegisterParticleProvidersEvent e) {
        mote(e, ModRegistries.RUBBER.get(), 0.95F, 0.70F, 0.55F, 0.09F, 12);
        mote(e, ModRegistries.HAKI.get(), 0.08F, 0.08F, 0.1F, 0.1F, 16);
        mote(e, ModRegistries.LIGHT.get(), 1.0F, 0.9F, 0.4F, 0.08F, 10);
        mote(e, ModRegistries.WIND.get(), 0.8F, 0.97F, 0.95F, 0.08F, 16);
        mote(e, ModRegistries.DRAGON.get(), 0.3F, 0.68F, 0.88F, 0.09F, 16);
        mote(e, ModRegistries.SPARK.get(), 1.0F, 0.92F, 0.7F, 0.04F, 6);
    }

    static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModRegistries.PROJECTILE.get(), ProjectileRenderer::new);
    }

    static void layerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(ProjectileRenderer.LAYER, ProjectileRenderer::createLayer);
        e.registerLayerDefinition(FormLayer.LAYER, FormLayer::createLayer);
        e.registerLayerDefinition(DragonModel.LAYER, DragonModel::createLayer);
    }

    static void addLayers(EntityRenderersEvent.AddLayers e) {
        for (PlayerSkin.Model skin : e.getSkins()) {
            if (e.getSkin(skin) instanceof PlayerRenderer r) r.addLayer(new FormLayer(r, e.getEntityModels().bakeLayer(FormLayer.LAYER)));
        }
        dragon = new DragonModel(e.getEntityModels().bakeLayer(DragonModel.LAYER));
    }

    static void hud(RegisterGuiLayersEvent e) {
        e.registerAbove(VanillaGuiLayers.HOTBAR, GrandLine.id("pirate_hud"), PirateHud::render);
    }

    static void tick(ClientTickEvent.Post event) {
        FxClient.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        for (int i = 0; i < KEYS.length; i++) {
            while (KEYS[i].consumeClick()) PacketDistributor.sendToServer(new ModNetwork.UseKey(i));
        }
    }

    /** In dragon form the player model is replaced by the azure dragon. */
    static void renderPlayer(RenderPlayerEvent.Pre e) {
        Player player = e.getEntity();
        ModNetwork.FormView v = ClientState.view(player.getId());
        if (v == null || !v.form().equals("seiryu_dragon") || dragon == null || !(player instanceof AbstractClientPlayer p)) return;
        e.setCanceled(true);
        PoseStack ps = e.getPoseStack();
        float pt = e.getPartialTick();
        float yaw = Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot);
        float pitch = Mth.lerp(pt, p.xRotO, p.getXRot());
        float sc = p.getScale();
        double speed = p.getDeltaMovement().horizontalDistance();
        boolean flying = !p.onGround();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        ps.scale(sc, sc, sc);
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.translate(0.0F, -1.501F, 0.0F);
        dragon.setup(p.tickCount + pt, (float) speed, pitch, flying);
        dragon.renderToBuffer(ps, e.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(DRAGON_TEX)), e.getPackedLight(), OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        ps.popPose();
    }
}
