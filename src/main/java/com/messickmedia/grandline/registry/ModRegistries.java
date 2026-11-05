package com.messickmedia.grandline.registry;

import com.messickmedia.grandline.GrandLine;
import com.messickmedia.grandline.entity.FruitProjectile;
import com.messickmedia.grandline.fruit.FruitData;
import com.messickmedia.grandline.fruit.FruitInfo;
import com.messickmedia.grandline.fruit.Pirate;
import com.messickmedia.grandline.item.DevilFruitItem;
import com.messickmedia.grandline.loot.DevilFruitLootModifier;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModRegistries {
    private ModRegistries() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GrandLine.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, GrandLine.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, GrandLine.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GrandLine.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GrandLine.MODID);
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, GrandLine.MODID);

    // ---------------------------------------------------------------- player data
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Pirate>> PIRATE =
            ATTACHMENTS.register("pirate", () -> AttachmentType.builder(() -> new Pirate()).serialize(Pirate.CODEC).copyOnDeath().build());

    // ---------------------------------------------------------------- particles
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RUBBER = particle("rubber_mote");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HAKI = particle("haki_mote");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LIGHT = particle("light_mote");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WIND = particle("wind_mote");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRAGON = particle("dragon_mote");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK = particle("spark");

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> particle(String name) {
        return PARTICLES.register(name, () -> new SimpleParticleType(true));
    }

    // ---------------------------------------------------------------- entities
    public static final DeferredHolder<EntityType<?>, EntityType<FruitProjectile>> PROJECTILE =
            ENTITIES.register("fruit_projectile", () -> EntityType.Builder.<FruitProjectile>of(FruitProjectile::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(10).updateInterval(1).build("fruit_projectile"));

    // ---------------------------------------------------------------- items
    public static final Map<String, DeferredItem<Item>> FRUITS = new LinkedHashMap<>();
    static {
        for (FruitInfo f : FruitData.ALL) {
            FRUITS.put(f.id(), ITEMS.register(f.id(), () -> new DevilFruitItem(f)));
        }
    }

    @Nullable
    public static Item fruitItem(String fruitId) {
        DeferredItem<Item> d = FRUITS.get(fruitId);
        return d == null ? null : d.get();
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.grandline"))
            .icon(() -> new ItemStack(FRUITS.get("gomu_gomu").get()))
            .displayItems((params, out) -> {
                for (DeferredHolder<Item, ? extends Item> h : ITEMS.getEntries()) out.accept(h.get());
            }).build());

    // ---------------------------------------------------------------- loot
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<DevilFruitLootModifier>> FRUIT_LOOT =
            LOOT_MODIFIERS.register("devil_fruit", () -> DevilFruitLootModifier.CODEC);

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
        PARTICLES.register(bus);
        ENTITIES.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        LOOT_MODIFIERS.register(bus);
    }
}
