package com.messickmedia.grandline;

import com.messickmedia.grandline.fruit.Bounty;
import com.messickmedia.grandline.fruit.FruitData;
import com.messickmedia.grandline.fruit.FruitInfo;
import com.messickmedia.grandline.fruit.Gx;
import com.messickmedia.grandline.fruit.Pirate;
import com.messickmedia.grandline.fruit.Scheduler;
import com.messickmedia.grandline.fruit.Systems;
import com.messickmedia.grandline.net.ModNetwork;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class GameEvents {
    private GameEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Scheduler.tick(event.getServer());
        if (event.getServer().getTickCount() % 10 == 0) {
            for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) if (Gx.data(p).hasFruit()) ModNetwork.broadcastForm(p);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Scheduler.clear();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Gx.data(p).resetLive();
            ModNetwork.sync(p);
            ModNetwork.syncAllForms(p.serverLevel().getServer(), p);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) ModNetwork.syncAllForms(p.serverLevel().getServer(), p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Pirate s = Gx.data(p);
            s.resetLive();
            s.stamina = 60;
            s.form = 0;
            s.toggle = false;
            ModNetwork.sync(p);
            ModNetwork.broadcastForm(p);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p) Systems.tickPlayer(p);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Systems.onIncomingDamage(event);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        Entity killer = event.getSource().getEntity();
        if (killer instanceof ServerPlayer p && p != victim) Systems.onKill(p, victim);
        if (victim instanceof ServerPlayer dead) Systems.onDeath(dead);
    }

    // ---------------------------------------------------------------- commands

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
        LiteralArgumentBuilder<CommandSourceStack> give = Commands.literal("give");
        for (FruitInfo f : FruitData.ALL) {
            set.then(Commands.literal(f.id()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Systems.clearFruit(p);
                Systems.eat(p, f);
                return Command.SINGLE_SUCCESS;
            }));
            give.then(Commands.literal(f.id()).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                p.getInventory().placeItemBackInInventory(new net.minecraft.world.item.ItemStack(com.messickmedia.grandline.registry.ModRegistries.FRUITS.get(f.id()).get()));
                return Command.SINGLE_SUCCESS;
            }));
        }
        LiteralArgumentBuilder<CommandSourceStack> bounty = Commands.literal("bounty");
        for (Bounty b : Bounty.values()) {
            bounty.then(Commands.literal(b.id).executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Pirate s = Gx.data(p);
                Systems.addBerries(p, b.threshold - s.berries);
                return Command.SINGLE_SUCCESS;
            }));
        }
        bounty.then(Commands.argument("berries", LongArgumentType.longArg(0)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Pirate s = Gx.data(p);
            Systems.addBerries(p, LongArgumentType.getLong(c, "berries") - s.berries);
            return Command.SINGLE_SUCCESS;
        }));
        event.getDispatcher().register(Commands.literal("fruit")
                .requires(src -> src.hasPermission(2))
                .then(set)
                .then(give)
                .then(bounty)
                .then(Commands.literal("clear").executes(c -> {
                    Systems.clearFruit(c.getSource().getPlayerOrException());
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("refill").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Pirate s = Gx.data(p);
                    s.stamina = 100;
                    s.exhaustTicks = 0;
                    s.wetTicks = 0;
                    java.util.Arrays.fill(s.cd, 0);
                    ModNetwork.sync(p);
                    return Command.SINGLE_SUCCESS;
                })));

        // /pirate: anyone can check their own status
        event.getDispatcher().register(Commands.literal("pirate").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Pirate s = Gx.data(p);
            if (!s.hasFruit()) {
                c.getSource().sendSuccess(() -> Component.translatable("message.grandline.no_fruit").withStyle(ChatFormatting.GRAY), false);
                return Command.SINGLE_SUCCESS;
            }
            c.getSource().sendSuccess(() -> Component.translatable("message.grandline.info", s.info().displayName(), s.formInfo().displayName(),
                    Bounty.berries(s.berries), s.bounty().display()).withColor(s.info().color()), false);
            c.getSource().sendSuccess(() -> Component.translatable("fruit.grandline." + s.fruit + ".passive").withStyle(ChatFormatting.GRAY), false);
            return Command.SINGLE_SUCCESS;
        }));
    }
}
