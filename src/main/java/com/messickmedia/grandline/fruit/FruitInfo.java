package com.messickmedia.grandline.fruit;

import net.minecraft.network.chat.Component;

import java.util.List;

/** A devil fruit: its type, colour, the toggle it owns (B) and its forms (V cycles them). */
public record FruitInfo(String id, Type type, int color, String toggle, List<FormInfo> forms) {
    public enum Type { PARAMECIA, ZOAN, LOGIA }

    public Component displayName() {
        return Component.translatable("fruit.grandline." + this.id);
    }

    public FormInfo form(int index) {
        return this.forms.get(Math.floorMod(index, this.forms.size()));
    }

    public int formIndex(String formId) {
        for (int i = 0; i < this.forms.size(); i++) if (this.forms.get(i).id().equals(formId)) return i;
        return 0;
    }

    public boolean hasToggle() {
        return !this.toggle.isEmpty();
    }
}
