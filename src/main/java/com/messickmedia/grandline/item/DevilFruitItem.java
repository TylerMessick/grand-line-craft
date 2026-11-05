package com.messickmedia.grandline.item;

import com.messickmedia.grandline.fruit.FruitInfo;
import com.messickmedia.grandline.fruit.Systems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A devil fruit. Tastes horrible, grants the power, and a second one kills you. */
public class DevilFruitItem extends Item {
    private static final FoodProperties FRUIT = new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).alwaysEdible().build();

    public final FruitInfo fruit;

    public DevilFruitItem(FruitInfo fruit) {
        super(new Item.Properties().food(FRUIT).rarity(Rarity.EPIC).stacksTo(1).fireResistant());
        this.fruit = fruit;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        ItemStack result = super.finishUsingItem(stack, level, user);
        if (user instanceof ServerPlayer p) Systems.eat(p, this.fruit);
        return result;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 48;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("fruit.grandline.type." + this.fruit.type().name().toLowerCase(java.util.Locale.ROOT)).withColor(this.fruit.color()));
        tooltip.add(Component.translatable("fruit.grandline." + this.fruit.id() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("fruit.grandline.warning").withStyle(ChatFormatting.DARK_AQUA));
    }
}
