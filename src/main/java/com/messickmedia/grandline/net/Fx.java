package com.messickmedia.grandline.net;

import com.messickmedia.grandline.GrandLine;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * One 3D effect primitive, sent to nearby players and drawn by FxClient as glowing ribbons, voxel clouds and cube
 * particles. Parameters per kind:
 * ARC     cx cy cz yawDeg radius arcDeg tilt light life
 * RING    cx cy cz radius vertical
 * LINE    ax ay az bx by bz
 * IMPACT  x y z
 * SLAM    x y z radius
 * DRESS   x y z radius amount
 * ARM     ax ay az bx by bz thickness life     (stretched rubber limb with a fist at b; extends then snaps back)
 * BEAM    ax ay az bx by bz radius life        (thick ray: light laser, dragon breath)
 * PUFF    x y z radius amount                  (volumetric cloud: steam, flame clouds, dust)
 * BOLT    ax ay az bx by bz
 * SPHERE  x y z radius life                    (balloon, giant fist, light burst)
 */
public record Fx(int kind, int style, float[] f) implements CustomPacketPayload {
    public static final int ARC = 0, RING = 1, LINE = 2, IMPACT = 3, SLAM = 4, DRESS = 5, ARM = 6, BEAM = 7, PUFF = 8, BOLT = 9, SPHERE = 10;
    /** Styles (palettes and dressing). */
    public static final int RUBBER = 0, STEAM = 1, HAKI = 2, FIRE = 3, LIGHT = 4, LIGHTNING = 5, WIND = 6, CLOUD = 7, NIKA = 8, DRAGON = 9, WATER = 10;

    public static final Type<Fx> TYPE = new Type<>(GrandLine.id("fx"));
    public static final StreamCodec<ByteBuf, Fx> CODEC = StreamCodec.of((buf, p) -> {
        VarInt.write(buf, p.kind);
        VarInt.write(buf, p.style);
        VarInt.write(buf, p.f.length);
        for (float v : p.f) buf.writeFloat(v);
    }, buf -> {
        int kind = VarInt.read(buf), style = VarInt.read(buf), n = Math.min(32, VarInt.read(buf));
        float[] f = new float[n];
        for (int i = 0; i < n; i++) f[i] = buf.readFloat();
        return new Fx(kind, style, f);
    });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerLevel level, int kind, int style, float... f) {
        PacketDistributor.sendToPlayersNear(level, null, f[0], f[1], f[2], 128, new Fx(kind, style, f));
    }
}
