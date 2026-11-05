package com.messickmedia.grandline.fruit;

import com.messickmedia.grandline.fruit.moves.Gomu;
import com.messickmedia.grandline.fruit.moves.Pika;
import com.messickmedia.grandline.fruit.moves.Seiryu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import java.util.HashMap;
import java.util.Map;

/** Move registry and the use pipeline: fruit, sea weakness, exhaustion, tier, cooldown, stamina, then the effect. */
public final class Moves {
    private Moves() {}

    private static final Map<String, MoveFx> FX = new HashMap<>();

    public static void register(String id, MoveFx fx) {
        FX.put(id, fx);
    }

    static {
        Gomu.register();
        Pika.register();
        Seiryu.register();
    }

    public static boolean has(String id) {
        return FX.containsKey(id);
    }

    /** Output multiplier from the form, bounty and buffs. */
    public static float power(Pirate s) {
        float p = 1.0F + 0.12F * s.bounty().ordinal();
        if (s.inForm("gear_second")) p *= 1.3F;
        if (s.inForm("gear_third")) p *= 1.6F;
        if (s.inForm("boundman") || s.inForm("snakeman")) p *= 2.0F;
        if (s.inForm("gear_fifth")) p *= 2.6F;
        if (s.inForm("seiryu_hybrid")) p *= 1.5F;
        if (s.inForm("seiryu_dragon")) p *= 2.2F;
        if (s.inForm("sun_burst")) p *= 1.8F;
        if (s.exhaustTicks > 0) p *= 0.5F;
        return p;
    }

    public static Move resolve(Pirate s, int slot) {
        if (!s.hasFruit() || slot < 0 || slot >= 6) return null;
        return s.formInfo().moves().get(slot);
    }

    public static void use(ServerPlayer p, int slot) {
        Pirate s = Gx.data(p);
        if (!s.hasFruit() || p.isSpectator()) return;
        Move m = resolve(s, slot);
        if (m == null) return;
        if (Systems.isWeak(p, s)) {
            Gx.msg(p, Component.translatable("message.grandline.sea_weak").withStyle(ChatFormatting.AQUA));
            return;
        }
        if (s.exhaustTicks > 0) {
            Gx.msg(p, Component.translatable("message.grandline.exhausted").withStyle(ChatFormatting.GRAY));
            return;
        }
        boolean creative = p.getAbilities().instabuild;
        if (!creative && !s.bounty().atLeast(m.tier())) {
            Gx.msg(p, Component.translatable("message.grandline.locked", m.tier().display()).withStyle(ChatFormatting.GRAY));
            return;
        }
        if (s.cd[slot] > 0) return;
        if (!s.spend(m.cost(), creative)) {
            Gx.msg(p, Component.translatable("message.grandline.no_stamina").withStyle(ChatFormatting.GRAY));
            return;
        }
        MoveFx fx = FX.get(m.id());
        boolean ok = fx != null && fx.run(new Cast(p, s, p.serverLevel(), power(s), m));
        if (!ok) {
            if (!creative) s.stamina = Math.min(100.0F, s.stamina + m.cost());
            s.setCooldown(slot, 8);
            return;
        }
        int cd = creative ? Math.min(m.cooldown(), 20) : m.cooldown();
        s.setCooldown(slot, cd);
        s.lastMove = m.id();
        p.swing(InteractionHand.MAIN_HAND, true);
        Gx.msg(p, m.displayName().copy().withColor(s.formInfo().color()));
        Systems.afterMove(p, s, m);
    }
}
