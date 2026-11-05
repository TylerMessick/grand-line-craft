package com.messickmedia.grandline.loot;

import com.messickmedia.grandline.Config;
import com.messickmedia.grandline.registry.ModRegistries;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.ArrayList;
import java.util.List;

/** Puts a random devil fruit in treasure chests, at the configured chance. */
public class DevilFruitLootModifier extends LootModifier {
    public static final MapCodec<DevilFruitLootModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i).apply(i, DevilFruitLootModifier::new));

    public DevilFruitLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        if (ctx.getRandom().nextDouble() < Config.FRUIT_CHEST_CHANCE.get()) {
            List<String> ids = new ArrayList<>(ModRegistries.FRUITS.keySet());
            String id = ids.get(ctx.getRandom().nextInt(ids.size()));
            loot.add(new ItemStack(ModRegistries.FRUITS.get(id).get()));
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
