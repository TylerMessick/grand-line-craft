package com.messickmedia.grandline.fruit;

import net.minecraft.network.chat.Component;

/** One move of a form. Cost is stamina (0-100), cooldown in ticks, tier is the bounty needed. */
public record Move(String id, int cost, int cooldown, Bounty tier) {
    public Component displayName() {
        return Component.translatable("move.grandline." + this.id);
    }
}
