package com.messickmedia.grandline.client;

import com.messickmedia.grandline.fruit.Bounty;
import com.messickmedia.grandline.fruit.FruitData;
import com.messickmedia.grandline.fruit.FruitInfo;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Left of the hotbar: fruit and current form, bounty with tier progress, stamina bar, six move chips (Z R G H J Y)
 * with cooldowns and locks, then the form (V) and toggle (B) chips. A move list sits at the left edge of the screen.
 */
public final class PirateHud {
    private PirateHud() {}

    private static final String[] KEYS = {"Z", "R", "G", "H", "J", "Y"};

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !ClientState.hasFruit()) return;
        Font font = mc.font;
        FruitInfo f = FruitData.byId(ClientState.fruit);
        if (f == null) return;
        int color = 0xFF000000 | ClientState.formColor;
        int x = Math.max(4, g.guiWidth() / 2 - 91 - 172);
        int y = g.guiHeight() - 58;

        g.drawString(font, f.displayName(), x, y, 0xFF000000 | f.color(), true);
        Component form = Component.translatable("form.grandline." + ClientState.form);
        g.drawString(font, form, x + 164 - font.width(form), y, color, true);
        if (ClientState.wet) {
            g.drawString(font, Component.translatable("hud.grandline.wet"), x, y - 10, (System.currentTimeMillis() / 300L) % 2 == 0 ? 0xFF4FC3F7 : 0xFF1E88E5, true);
        } else if (ClientState.exhaust > 0) {
            g.drawString(font, Component.translatable("hud.grandline.exhausted"), x, y - 10, 0xFF8A8F98, true);
        } else if (ClientState.toggle) {
            g.drawString(font, Component.translatable("toggle.grandline." + f.toggle()), x, y - 10, color, true);
        }

        // stamina and bounty progress
        int barY = y + 11, barW = 164;
        int fill = Math.round(barW * ClientState.stamina / 100.0F);
        g.fill(x - 1, barY - 1, x + barW + 1, barY + 5, 0xAA000000);
        g.fill(x, barY, x + fill, barY + 4, ClientState.stamina < 20 ? 0xFFD03030 : color);
        g.fill(x, barY, x + fill, barY + 1, 0x60FFFFFF);
        g.fill(x, barY + 6, x + barW, barY + 7, 0x66000000);
        g.fill(x, barY + 6, x + Math.round(barW * ClientState.bountyProgress / 100.0F), barY + 7, 0xFFE8C547);
        Bounty tier = Bounty.values()[Math.max(0, Math.min(Bounty.values().length - 1, ClientState.bounty))];
        Component bounty = Component.literal("฿ " + Bounty.berries(ClientState.berries) + "  ").append(tier.display());
        g.pose().pushPose();
        g.pose().scale(0.75F, 0.75F, 1.0F);
        g.drawString(font, bounty, (int) ((x) / 0.75F), (int) ((barY + 9) / 0.75F), 0xFFE8C547, true);
        g.pose().popPose();

        // move chips
        int chipY = barY + 18;
        for (int i = 0; i < 6; i++) {
            chip(g, font, x + i * 27, chipY, KEYS[i], ClientState.cooldownPct[i], (ClientState.lockMask & (1 << i)) != 0, false, color);
        }
        int sx = x + 6 * 27 + 4;
        if (f.forms().size() > 1) chip(g, font, sx, chipY, "V", ClientState.cooldownPct[6], false, !ClientState.form.equals(f.forms().get(0).id()), color);
        if (f.hasToggle()) chip(g, font, sx + 27, chipY, "B", ClientState.cooldownPct[7], false, ClientState.toggle, color);

        // move list at the left edge
        g.pose().pushPose();
        g.pose().scale(0.75F, 0.75F, 1.0F);
        int ly = (int) ((g.guiHeight() / 2 - 40) / 0.75F);
        for (int i = 0; i < 6; i++) {
            String id = ClientState.moves[i];
            if (id == null || id.isEmpty()) continue;
            boolean locked = (ClientState.lockMask & (1 << i)) != 0;
            boolean cd = ClientState.cooldownPct[i] > 0;
            Component name = Component.translatable("move.grandline." + id);
            int col = locked ? 0xFF55555C : cd ? 0xFF8A8F98 : 0xFFFFFFFF;
            g.drawString(font, Component.literal("[" + KEYS[i] + "] ").append(name), 6, ly + i * 11, col, true);
        }
        if (f.forms().size() > 1) g.drawString(font, Component.literal("[V] ").append(Component.translatable("hud.grandline.shift")), 6, ly + 6 * 11 + 4, 0xFFBBBBBB, true);
        if (f.hasToggle()) g.drawString(font, Component.literal("[B] ").append(Component.translatable("toggle.grandline." + f.toggle())), 6, ly + 7 * 11 + 4, 0xFFBBBBBB, true);
        g.pose().popPose();
    }

    private static void chip(GuiGraphics g, Font font, int cx, int cy, String key, int cdPct, boolean locked, boolean active, int color) {
        g.fill(cx, cy, cx + 24, cy + 16, active ? (0xCC000000 | (color & 0x00FFFFFF)) & 0xCCFFFFFF : 0xAA000000);
        if (cdPct > 0) g.fill(cx, cy + 16 - Math.round(16 * cdPct / 100.0F), cx + 24, cy + 16, 0x88555555);
        else if (!locked) g.fill(cx, cy + 15, cx + 24, cy + 16, color);
        if (locked) {
            g.fill(cx + 9, cy + 7, cx + 15, cy + 12, 0xFF77777C);
            g.fill(cx + 10, cy + 4, cx + 14, cy + 7, 0xFF77777C);
        } else {
            g.drawCenteredString(font, key, cx + 12, cy + 4, cdPct > 0 ? 0xFF777777 : 0xFFFFFFFF);
        }
    }
}
