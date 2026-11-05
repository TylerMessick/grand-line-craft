package com.messickmedia.grandline.fruit;

import com.messickmedia.grandline.Config;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Per-player devil fruit state, stored as a player attachment (survives death unless the config says the fruit leaves).
 * Saved: fruit, bounty, stamina, form, toggle. Everything under "live-only" resets on relog.
 */
public class Pirate {
    public static final Codec<Pirate> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("fruit", "").forGetter(p -> p.fruit),
            Codec.LONG.optionalFieldOf("berries", 0L).forGetter(p -> p.berries),
            Codec.FLOAT.optionalFieldOf("stamina", 100.0F).forGetter(p -> p.stamina),
            Codec.INT.optionalFieldOf("form", 0).forGetter(p -> p.form),
            Codec.BOOL.optionalFieldOf("toggle", false).forGetter(p -> p.toggle),
            Codec.INT.optionalFieldOf("kills", 0).forGetter(p -> p.kills)
    ).apply(i, Pirate::new));

    public String fruit;
    public long berries;
    public float stamina;
    public int form;
    public boolean toggle;
    public int kills;

    // ---------------------------------------------------------------- live-only
    /** 0-11 move cooldowns (two pages of six), 12 form shift, 13 toggle. */
    public static final int FORM_SLOT = 12, TOGGLE_SLOT = 13;
    public final int[] cd = new int[14];
    public final int[] cdMax = new int[14];
    /** Which page of six moves the keys drive. */
    public int page;
    /** Ticks of exhaustion after a gear ends (Gear 2 fatigue, Gear 5 collapse): no moves, weakness. */
    public int exhaustTicks;
    /** Ticks the player has been in water (sea weakness ramps up). */
    public int wetTicks;
    public int balloonTicks, murakumoTicks, gigantTicks, rubberWorldTicks, cloudTicks, hardenTicks, accelTicks, daikoTicks, cannonballTicks;
    public int formTicks;
    public float drainAcc;
    public String lastMove = "";
    public boolean flying;
    public int lastSyncHash;

    public Pirate() {
        this("", 0L, 100.0F, 0, false, 0);
    }

    public Pirate(String fruit, long berries, float stamina, int form, boolean toggle, int kills) {
        this.fruit = fruit;
        this.berries = berries;
        this.stamina = stamina;
        this.form = form;
        this.toggle = toggle;
        this.kills = kills;
    }

    public boolean hasFruit() {
        return !this.fruit.isEmpty() && FruitData.byId(this.fruit) != null;
    }

    @Nullable
    public FruitInfo info() {
        return FruitData.byId(this.fruit);
    }

    public FormInfo formInfo() {
        FruitInfo f = info();
        return f == null ? FruitData.GOMU.form(0) : f.form(this.form);
    }

    public boolean is(String fruitId) {
        return this.fruit.equals(fruitId);
    }

    public boolean inForm(String formId) {
        return hasFruit() && formInfo().id().equals(formId);
    }

    public Bounty bounty() {
        return Bounty.forBerries(this.berries);
    }

    public boolean spend(float amount, boolean creative) {
        if (creative) return true;
        if (this.stamina < amount) return false;
        this.stamina -= amount;
        return true;
    }

    public int pages() {
        return hasFruit() ? (formInfo().moves().size() + 5) / 6 : 1;
    }

    public void setCooldown(int slot, int ticks) {
        this.cd[slot] = ticks;
        this.cdMax[slot] = Math.max(1, ticks);
    }

    public void resetLive() {
        Arrays.fill(this.cd, 0);
        this.exhaustTicks = this.wetTicks = this.balloonTicks = this.murakumoTicks = this.gigantTicks = this.rubberWorldTicks = this.cloudTicks = this.hardenTicks = 0;
        this.accelTicks = this.daikoTicks = this.cannonballTicks = 0;
        this.page = 0;
        this.formTicks = 0;
        this.drainAcc = 0;
        this.flying = false;
    }

    /** Cooldowns, timers and stamina regeneration. Form drain is applied by Systems. */
    public void tick(boolean sprinting, int foodLevel) {
        for (int i = 0; i < this.cd.length; i++) if (this.cd[i] > 0) this.cd[i]--;
        if (this.exhaustTicks > 0) this.exhaustTicks--;
        if (this.balloonTicks > 0) this.balloonTicks--;
        if (this.murakumoTicks > 0) this.murakumoTicks--;
        if (this.gigantTicks > 0) this.gigantTicks--;
        if (this.rubberWorldTicks > 0) this.rubberWorldTicks--;
        if (this.cloudTicks > 0) this.cloudTicks--;
        if (this.hardenTicks > 0) this.hardenTicks--;
        if (this.accelTicks > 0) this.accelTicks--;
        if (this.daikoTicks > 0) this.daikoTicks--;
        if (this.cannonballTicks > 0) this.cannonballTicks--;
        this.formTicks++;
        float regen = 100.0F / 600.0F * (float) (double) Config.STAMINA_REGEN.get();
        if (foodLevel <= 6) regen *= 0.4F;
        if (sprinting) regen *= 0.5F;
        if (this.exhaustTicks > 0) regen *= 0.25F;
        this.stamina = Math.max(0.0F, Math.min(100.0F, this.stamina + regen));
    }
}
