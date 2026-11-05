package com.messickmedia.grandline.fruit;

import net.minecraft.network.chat.Component;

/** Bounty tiers. Forms and moves unlock as your bounty climbs. Values are in Berries. */
public enum Bounty {
    ROOKIE("rookie", 0L),
    SUPERNOVA("supernova", 50_000_000L),
    WARLORD("warlord", 100_000_000L),
    COMMANDER("commander", 500_000_000L),
    EMPEROR("emperor", 1_500_000_000L);

    public final String id;
    public final long threshold;

    Bounty(String id, long threshold) {
        this.id = id;
        this.threshold = threshold;
    }

    public static Bounty forBerries(long berries) {
        Bounty b = ROOKIE;
        for (Bounty t : values()) if (berries >= t.threshold) b = t;
        return b;
    }

    public boolean atLeast(Bounty other) {
        return this.ordinal() >= other.ordinal();
    }

    public Bounty next() {
        return this == EMPEROR ? EMPEROR : values()[this.ordinal() + 1];
    }

    public Component display() {
        return Component.translatable("bounty.grandline." + this.id);
    }

    public static String berries(long b) {
        if (b >= 1_000_000_000L) return String.format("%.2fB", b / 1_000_000_000.0D);
        if (b >= 1_000_000L) return String.format("%.1fM", b / 1_000_000.0D);
        if (b >= 1_000L) return (b / 1_000L) + "K";
        return Long.toString(b);
    }
}
