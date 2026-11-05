package com.messickmedia.grandline;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config (grandline-server.toml in the world's serverconfig folder). */
public final class Config {
    private Config() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue BLOCK_DAMAGE;
    public static final ModConfigSpec.DoubleValue DAMAGE;
    public static final ModConfigSpec.DoubleValue STAMINA_REGEN;
    public static final ModConfigSpec.BooleanValue LOSE_FRUIT_ON_DEATH;
    public static final ModConfigSpec.BooleanValue SEA_WEAKNESS;
    public static final ModConfigSpec.DoubleValue FRUIT_CHEST_CHANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        BLOCK_DAMAGE = b.comment("Big moves break blocks (also requires the mobGriefing gamerule).").define("blockDamage", true);
        DAMAGE = b.comment("Devil fruit damage multiplier.").defineInRange("fruitDamage", 1.0D, 0.0D, 20.0D);
        STAMINA_REGEN = b.comment("Stamina regeneration multiplier.").defineInRange("staminaRegen", 1.0D, 0.0D, 20.0D);
        LOSE_FRUIT_ON_DEATH = b.comment("Like the anime: when a fruit user dies, the power leaves them and the fruit drops where they fell.").define("loseFruitOnDeath", false);
        SEA_WEAKNESS = b.comment("Fruit users sink in water, lose their powers while wet, and drown fast.").define("seaWeakness", true);
        FRUIT_CHEST_CHANCE = b.comment("Chance of a devil fruit in a shipwreck, buried-treasure or end-city chest.").defineInRange("fruitChestChance", 0.12D, 0.0D, 1.0D);
        SPEC = b.build();
    }
}
