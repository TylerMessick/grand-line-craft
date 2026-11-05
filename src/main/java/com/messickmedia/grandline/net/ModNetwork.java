package com.messickmedia.grandline.net;

import com.messickmedia.grandline.GrandLine;
import com.messickmedia.grandline.fruit.Bounty;
import com.messickmedia.grandline.fruit.Gx;
import com.messickmedia.grandline.fruit.Move;
import com.messickmedia.grandline.fruit.Moves;
import com.messickmedia.grandline.fruit.Pirate;
import com.messickmedia.grandline.fruit.Systems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {}

    /** C2S keys: 0-5 moves (Z R G H J Y), 6 form shift (V), 7 toggle (B), 8 next move page (U). */
    public record UseKey(int key) implements CustomPacketPayload {
        public static final Type<UseKey> TYPE = new Type<>(GrandLine.id("use_key"));
        public static final StreamCodec<ByteBuf, UseKey> CODEC = ByteBufCodecs.VAR_INT.map(UseKey::new, UseKey::key);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S2C HUD state for the local player. */
    public record Sync(String fruit, String form, float stamina, boolean toggle, int bounty, int bountyProgress, long berries,
                       int[] cd, int lockMask, String[] moves, int exhaust, boolean wet, int formColor, int page, int pages) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(GrandLine.id("sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> CODEC = StreamCodec.of((buf, s) -> {
            buf.writeUtf(s.fruit); buf.writeUtf(s.form); buf.writeFloat(s.stamina); buf.writeBoolean(s.toggle);
            buf.writeVarInt(s.bounty); buf.writeVarInt(s.bountyProgress); buf.writeLong(s.berries);
            for (int i = 0; i < 8; i++) buf.writeVarInt(s.cd[i]);
            buf.writeVarInt(s.lockMask);
            for (int i = 0; i < 6; i++) buf.writeUtf(s.moves[i] == null ? "" : s.moves[i]);
            buf.writeVarInt(s.exhaust); buf.writeBoolean(s.wet); buf.writeInt(s.formColor); buf.writeVarInt(s.page); buf.writeVarInt(s.pages);
        }, buf -> {
            String fruit = buf.readUtf(), form = buf.readUtf(); float st = buf.readFloat(); boolean tog = buf.readBoolean();
            int bounty = buf.readVarInt(), prog = buf.readVarInt(); long berries = buf.readLong();
            int[] cd = new int[8];
            for (int i = 0; i < 8; i++) cd[i] = buf.readVarInt();
            int lock = buf.readVarInt();
            String[] mv = new String[6];
            for (int i = 0; i < 6; i++) mv[i] = buf.readUtf();
            return new Sync(fruit, form, st, tog, bounty, prog, berries, cd, lock, mv, buf.readVarInt(), buf.readBoolean(), buf.readInt(), buf.readVarInt(), buf.readVarInt());
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S2C: what a player looks like right now (for the form render layers), broadcast to everyone. */
    public record FormView(int entityId, String fruit, String form, boolean toggle, int flags) implements CustomPacketPayload {
        public static final int BALLOON = 1, MURAKUMO = 2, GIGANT = 4, EXHAUSTED = 8, WET = 16;
        public static final Type<FormView> TYPE = new Type<>(GrandLine.id("form_view"));
        public static final StreamCodec<FriendlyByteBuf, FormView> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, FormView::entityId, ByteBufCodecs.STRING_UTF8, FormView::fruit, ByteBufCodecs.STRING_UTF8, FormView::form,
                ByteBufCodecs.BOOL, FormView::toggle, ByteBufCodecs.VAR_INT, FormView::flags, FormView::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** S2C: a limb of a player stretches out to a point (drawn with that player's own skin). limb 0-3: right arm, left arm, right leg, left leg. */
    public record Stretch(int entityId, int limb, float x, float y, float z, float thick, int life, int style) implements CustomPacketPayload {
        public static final Type<Stretch> TYPE = new Type<>(GrandLine.id("stretch"));
        public static final StreamCodec<FriendlyByteBuf, Stretch> CODEC = StreamCodec.of((buf, s) -> {
            buf.writeVarInt(s.entityId); buf.writeVarInt(s.limb); buf.writeFloat(s.x); buf.writeFloat(s.y); buf.writeFloat(s.z);
            buf.writeFloat(s.thick); buf.writeVarInt(s.life); buf.writeVarInt(s.style);
        }, buf -> new Stretch(buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(UseKey.TYPE, UseKey.CODEC, ModNetwork::onKey);
        r.playToClient(Sync.TYPE, Sync.CODEC, (s, ctx) -> ClientSink.sync.accept(s));
        r.playToClient(FormView.TYPE, FormView.CODEC, (v, ctx) -> ClientSink.form.accept(v));
        r.playToClient(Fx.TYPE, Fx.CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientSink.fx.accept(p)));
        r.playToClient(Stretch.TYPE, Stretch.CODEC, (p, ctx) -> ctx.enqueueWork(() -> ClientSink.stretch.accept(p)));
    }

    /** Indirection so the common payload handlers never load client classes on a dedicated server. */
    public static final class ClientSink {
        public static java.util.function.Consumer<Sync> sync = s -> { };
        public static java.util.function.Consumer<FormView> form = v -> { };
        public static java.util.function.Consumer<Fx> fx = p -> { };
        public static java.util.function.Consumer<Stretch> stretch = p -> { };
    }

    private static void onKey(UseKey p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player) || !player.isAlive()) return;
        int k = p.key();
        if (k >= 0 && k <= 5) Moves.use(player, k);
        else if (k == 6) Systems.shiftForm(player);
        else if (k == 7) Systems.toggle(player);
        else if (k == 8) Systems.nextPage(player);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        Pirate s = Gx.data(player);
        int pages = s.pages();
        int page = Math.floorMod(s.page, pages);
        int[] pct = new int[8];
        for (int i = 0; i < 6; i++) { int k = page * 6 + i; pct[i] = s.cd[k] <= 0 ? 0 : Math.min(100, Math.round(100.0F * s.cd[k] / Math.max(1, s.cdMax[k]))); }
        pct[6] = s.cd[Pirate.FORM_SLOT] <= 0 ? 0 : Math.min(100, Math.round(100.0F * s.cd[Pirate.FORM_SLOT] / Math.max(1, s.cdMax[Pirate.FORM_SLOT])));
        pct[7] = s.cd[Pirate.TOGGLE_SLOT] <= 0 ? 0 : Math.min(100, Math.round(100.0F * s.cd[Pirate.TOGGLE_SLOT] / Math.max(1, s.cdMax[Pirate.TOGGLE_SLOT])));
        int lock = 0;
        String[] mv = new String[6];
        boolean creative = player.getAbilities().instabuild;
        for (int i = 0; i < 6; i++) {
            Move m = Moves.resolve(s, page * 6 + i);
            mv[i] = m == null ? "" : m.id();
            if (m != null && !creative && !s.bounty().atLeast(m.tier())) lock |= 1 << i;
        }
        Bounty b = s.bounty();
        int prog = b == Bounty.EMPEROR ? 100 : (int) Math.round(100.0D * (s.berries - b.threshold) / Math.max(1, b.next().threshold - b.threshold));
        PacketDistributor.sendToPlayer(player, new Sync(s.fruit, s.hasFruit() ? s.formInfo().id() : "", s.stamina, s.toggle, b.ordinal(), prog, s.berries,
                pct, lock, mv, s.exhaustTicks, Systems.isWeak(player, s), s.hasFruit() ? s.formInfo().color() : 0xFFFFFF, page, pages));
    }

    public static void stretch(ServerPlayer player, int limb, net.minecraft.world.phys.Vec3 to, double thick, int life, int style) {
        PacketDistributor.sendToPlayersNear(player.serverLevel(), null, player.getX(), player.getY(), player.getZ(), 128,
                new Stretch(player.getId(), limb, (float) to.x, (float) to.y, (float) to.z, (float) thick, life, style));
    }

    public static FormView viewOf(ServerPlayer player) {
        Pirate s = Gx.data(player);
        int flags = 0;
        if (s.balloonTicks > 0) flags |= FormView.BALLOON;
        if (s.murakumoTicks > 0) flags |= FormView.MURAKUMO;
        if (s.gigantTicks > 0) flags |= FormView.GIGANT;
        if (s.exhaustTicks > 0) flags |= FormView.EXHAUSTED;
        if (Systems.isWeak(player, s)) flags |= FormView.WET;
        return new FormView(player.getId(), s.fruit, s.hasFruit() ? s.formInfo().id() : "", s.toggle, flags);
    }

    /** Everyone in the player's dimension learns what the player looks like. */
    public static void broadcastForm(ServerPlayer player) {
        PacketDistributor.sendToPlayersInDimension(player.serverLevel(), viewOf(player));
    }

    /** A joining player learns everyone's look, and everyone learns theirs. */
    public static void syncAllForms(MinecraftServer server, ServerPlayer joined) {
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(joined, viewOf(other));
        }
        broadcastForm(joined);
    }
}
