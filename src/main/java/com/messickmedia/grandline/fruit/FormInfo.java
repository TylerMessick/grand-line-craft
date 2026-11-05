package com.messickmedia.grandline.fruit;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * One form of a fruit: the base body, a gear, a zoan stage. Each form carries its own six moves and a stamina drain
 * per second while it is held (0 for the base form).
 */
public record FormInfo(String id, Bounty tier, float drain, int color, List<Move> moves) {
    public Component displayName() {
        return Component.translatable("form.grandline." + this.id);
    }
}
