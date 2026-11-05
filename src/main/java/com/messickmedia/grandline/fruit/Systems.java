package com.messickmedia.grandline.fruit;

import com.messickmedia.grandline.Config;
import com.messickmedia.grandline.GrandLine;
import com.messickmedia.grandline.net.Fx;
import com.messickmedia.grandline.net.ModNetwork;
import com.messickmedia.grandline.registry.ModRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The rules around the moves: form passives and drains, the sea weakness, logia and rubber body defences, bounty,
 * eating a fruit, and the V (form) and B (toggle) keys.
 */
public final class Systems {
    private Systems() {}

    private static final ResourceLocation SPEED = GrandLine.id("form_speed");
    private static final ResourceLocation ATTACK = GrandLine.id("form_attack");
    private static final ResourceLocation HEALTH = GrandLine.id("form_health");
    private static final ResourceLocation KB = GrandLine.id("form_kb");
    private static final ResourceLocation SCALE = GrandLine.id("form_scale");
    private static final ResourceLocation ARMOR = GrandLine.id("form_armor");
    private static final ResourceLocation JUMP = GrandLine.id("form_jump");
    private static final ResourceLocation STEP = GrandLine.id("form_step");
    private static final ResourceLocation SAFE_FALL = GrandLine.id("form_fall");

    // ---------------------------------------------------------------- tick

    public static void tickPlayer(ServerPlayer p) {
        Pirate s = Gx.data(p);
        ServerLevel level = p.serverLevel();
        boolean had = s.hasFruit();
        s.tick(p.isSprinting(), p.getFoodData().getFoodLevel());
        if (!had) {
            if (s.flying) revokeFlight(p, s);
            clearModifiers(p);
            if (p.tickCount % 20 == 0) ModNetwork.sync(p);
            return;
        }
        FruitInfo f = s.info();
        FormInfo form = s.formInfo();

        // ---- sea weakness: sink, go limp, lose every power
        boolean weak = false;
        if (Config.SEA_WEAKNESS.get() && (p.isInWaterOrBubble() || p.isInWater())) {
            s.wetTicks = Math.min(200, s.wetTicks + 2);
        } else if (s.wetTicks > 0) {
            s.wetTicks--;
        }
        if (s.wetTicks > 10 && Config.SEA_WEAKNESS.get()) {
            weak = true;
            if (p.isInWater()) {
                p.setDeltaMovement(p.getDeltaMovement().add(0, -0.06D, 0));
                p.hurtMarked = p.tickCount % 5 == 0;
                if (p.tickCount % 2 == 0) p.setAirSupply(Math.max(-19, p.getAirSupply() - 1));
                Gx.effect(p, MobEffects.MOVEMENT_SLOWDOWN, 30, 2);
                Gx.effect(p, MobEffects.WEAKNESS, 30, 1);
                Gx.effect(p, MobEffects.DIG_SLOWDOWN, 30, 1);
            }
            if (s.form != 0) setForm(p, s, 0, true);
            if (s.toggle) { s.toggle = false; }
            if (p.tickCount % 40 == 0) Gx.msg(p, Component.translatable("message.grandline.sea_weak").withStyle(ChatFormatting.AQUA));
        }

        // ---- form drain and collapse
        if (form.drain() > 0 && !p.getAbilities().instabuild) {
            s.stamina -= form.drain() / 20.0F;
            if (s.stamina <= 0) {
                s.stamina = 0;
                collapse(p, s);
            }
        }
        if (s.toggle && !p.getAbilities().instabuild) {
            float drain = f.id().equals("pika_pika") ? 2.0F : 1.5F;
            s.stamina -= drain / 20.0F;
            if (s.stamina <= 0) {
                s.stamina = 0;
                s.toggle = false;
                Gx.msg(p, Component.translatable("message.grandline.no_stamina").withStyle(ChatFormatting.GRAY));
            }
        }

        // ---- passives and visuals
        applyForm(p, s, weak);
        boolean fly = !weak && (s.toggle && (f.id().equals("pika_pika") || f.id().equals("uo_uo_seiryu"))
                || s.inForm("seiryu_dragon") || s.inForm("boundman") || s.inForm("gear_fifth"));
        if (fly && !s.flying) grantFlight(p, s);
        if (!fly && s.flying) revokeFlight(p, s);
        if (s.flying) {
            float speed = s.is("pika_pika") ? 0.22F : s.inForm("seiryu_dragon") ? 0.14F : 0.08F;
            if (p.getAbilities().getFlyingSpeed() != speed) { p.getAbilities().setFlyingSpeed(speed); p.onUpdateAbilities(); }
        }
        tickVisuals(p, s, level);
        if (s.rubberWorldTicks > 0 && p.tickCount % 4 == 0) rubberWorld(p, s, level);
        if (s.gigantTicks == 1) Gx.msg(p, Component.translatable("message.grandline.gigant_end").withStyle(ChatFormatting.WHITE));

        if (p.tickCount % 10 == 0) ModNetwork.sync(p);
    }

    public static boolean isWeak(ServerPlayer p, Pirate s) {
        return Config.SEA_WEAKNESS.get() && s.wetTicks > 10;
    }

    /** A gear ran out of stamina: Gear 2 leaves fatigue, Gear 5 leaves a collapse, zoan forms simply drop. */
    private static void collapse(ServerPlayer p, Pirate s) {
        String id = s.formInfo().id();
        int exhaust = switch (id) {
            case "gear_second" -> 100;
            case "gear_third" -> 60;
            case "boundman", "snakeman" -> 200;
            case "gear_fifth" -> 400;
            case "sun_burst" -> 120;
            default -> 40;
        };
        setForm(p, s, 0, true);
        s.exhaustTicks = exhaust;
        Gx.effect(p, MobEffects.WEAKNESS, exhaust, 1);
        Gx.effect(p, MobEffects.MOVEMENT_SLOWDOWN, exhaust, 1);
        Gx.msg(p, Component.translatable("message.grandline.collapse").withStyle(ChatFormatting.DARK_GRAY));
        Gx.sound(p.serverLevel(), p.position(), SoundEvents.PLAYER_BREATH, 1.0F, 0.6F);
    }

    // ---------------------------------------------------------------- forms

    private static void mod(Player p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        if (amount == 0) { inst.removeModifier(id); return; }
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null && existing.amount() == amount) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }

    private static void clearModifiers(Player p) {
        mod(p, Attributes.MOVEMENT_SPEED, SPEED, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mod(p, Attributes.ATTACK_DAMAGE, ATTACK, 0, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.MAX_HEALTH, HEALTH, 0, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.KNOCKBACK_RESISTANCE, KB, 0, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.SCALE, SCALE, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mod(p, Attributes.ARMOR, ARMOR, 0, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.JUMP_STRENGTH, JUMP, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mod(p, Attributes.STEP_HEIGHT, STEP, 0, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL, 0, AttributeModifier.Operation.ADD_VALUE);
    }

    /** Attribute passives per fruit and form. Cleared while the player is wet. */
    private static void applyForm(ServerPlayer p, Pirate s, boolean weak) {
        if (weak) { clearModifiers(p); return; }
        String form = s.formInfo().id();
        double speed = 0, attack = 0, health = 0, kb = 0, scale = 0, armor = 0, jump = 0, step = 0, fall = 0;
        switch (s.fruit) {
            case "gomu_gomu" -> {
                fall = 100; // rubber bodies bounce
                switch (form) {
                    case "gear_second" -> { speed = 0.6; attack = 2; }
                    case "gear_third" -> { speed = -0.2; attack = 6; kb = 0.6; armor = 4; }
                    case "boundman" -> { speed = 0.35; attack = 8; kb = 0.8; armor = 8; health = 20; scale = 0.25; jump = 1.2; step = 1.0; }
                    case "snakeman" -> { speed = 0.9; attack = 6; kb = 0.3; armor = 4; jump = 0.6; }
                    case "gear_fifth" -> { speed = 0.5; attack = 10; kb = 1.0; armor = 10; health = 40; jump = 1.5; step = 1.5; }
                    default -> { }
                }
                if (s.gigantTicks > 0) scale = 2.2;
            }
            case "pika_pika" -> {
                speed = s.toggle ? 0.8 : 0.25;
                attack = 2;
                if (form.equals("sun_burst")) { attack = 5; speed += 0.3; }
            }
            case "uo_uo_seiryu" -> {
                switch (form) {
                    case "seiryu_human" -> { health = 10; attack = 2; kb = 0.3; armor = 2; }
                    case "seiryu_hybrid" -> { health = 40; attack = 7; kb = 0.8; armor = 8; scale = 0.5; step = 1.0; speed = 0.15; }
                    case "seiryu_dragon" -> { health = 100; attack = 12; kb = 1.0; armor = 14; scale = 2.4; step = 2.0; speed = 0.1; fall = 100; }
                    default -> { }
                }
            }
            default -> { }
        }
        mod(p, Attributes.MOVEMENT_SPEED, SPEED, speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mod(p, Attributes.ATTACK_DAMAGE, ATTACK, attack, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.MAX_HEALTH, HEALTH, health, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.KNOCKBACK_RESISTANCE, KB, kb, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.SCALE, SCALE, scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mod(p, Attributes.ARMOR, ARMOR, armor, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.JUMP_STRENGTH, JUMP, jump, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mod(p, Attributes.STEP_HEIGHT, STEP, step, AttributeModifier.Operation.ADD_VALUE);
        mod(p, Attributes.SAFE_FALL_DISTANCE, SAFE_FALL, fall, AttributeModifier.Operation.ADD_VALUE);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());

        // zoan regeneration and fire body
        if (s.is("uo_uo_seiryu")) {
            if (p.tickCount % 40 == 0 && p.getHealth() < p.getMaxHealth()) p.heal(form.equals("seiryu_dragon") ? 3 : form.equals("seiryu_hybrid") ? 2 : 0.5F);
            if (p.isOnFire()) p.clearFire();
        }
        if (s.is("pika_pika") && s.toggle) p.setGlowingTag(true);
        else if (s.is("pika_pika") && p.hasGlowingTag()) p.setGlowingTag(false);
    }

    private static void grantFlight(ServerPlayer p, Pirate s) {
        s.flying = true;
        p.getAbilities().mayfly = true;
        p.onUpdateAbilities();
    }

    private static void revokeFlight(ServerPlayer p, Pirate s) {
        s.flying = false;
        if (!p.getAbilities().instabuild && !p.isSpectator()) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.getAbilities().setFlyingSpeed(0.05F);
            p.onUpdateAbilities();
        }
    }

    /** The ambient look of each form: steam, light, clouds, the drums of liberation. */
    private static void tickVisuals(ServerPlayer p, Pirate s, ServerLevel level) {
        String form = s.formInfo().id();
        Vec3 c = p.position();
        switch (form) {
            case "gear_second" -> { if (p.tickCount % 2 == 0) Vfx.steam(level, c, 2); }
            case "gear_third" -> { if (p.tickCount % 10 == 0) Vfx.steam(level, c.add(0, 0.4, 0), 1); }
            case "boundman", "snakeman" -> {
                if (p.tickCount % 2 == 0) Vfx.steam(level, c.add(0, 0.6, 0), 3);
                if (form.equals("boundman") && p.onGround() && p.getDeltaMovement().horizontalDistanceSqr() > 0.01D && p.tickCount % 6 == 0) {
                    // Boundman is always bouncing
                    p.setDeltaMovement(p.getDeltaMovement().add(0, 0.42D, 0));
                    p.hurtMarked = true;
                    Vfx.puff(level, Fx.STEAM, c, 0.6, 2);
                }
            }
            case "gear_fifth" -> {
                if (p.tickCount % 3 == 0) Vfx.puff(level, Fx.NIKA, c.add(0, 1.2, 0), 0.5, 1);
                if (p.tickCount % 2 == 0) level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1.0D, c.z, 1, 0.4D, 0.5D, 0.4D, 0.0D);
                int beat = p.tickCount % 24; // the Drums of Liberation
                if (beat == 0 || beat == 6) Gx.sound(level, c, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.2F, beat == 0 ? 0.55F : 0.7F);
            }
            case "seiryu_hybrid" -> { if (p.tickCount % 6 == 0) Vfx.dress(level, Fx.DRAGON, c.add(0, 1.0, 0), 0.6, 0.3); }
            case "seiryu_dragon" -> {
                if (p.tickCount % 2 == 0) Vfx.puff(level, Fx.CLOUD, c.add(0, -0.3, 0), 1.6, 2);
                if (p.tickCount % 8 == 0) Vfx.dress(level, Fx.DRAGON, c.add(0, 2.0, 0), 2.5, 0.5);
            }
            case "sun_burst" -> { if (p.tickCount % 2 == 0) Vfx.dress(level, Fx.LIGHT, c.add(0, 1.0, 0), 0.8, 0.6); }
            default -> { }
        }
        if (s.toggle) {
            if (s.is("pika_pika")) {
                if (p.getDeltaMovement().lengthSqr() > 0.05D) Vfx.line(level, Fx.LIGHT, c.add(0, 0.9, 0), c.add(0, 0.9, 0).subtract(p.getDeltaMovement().scale(3)));
                else if (p.tickCount % 3 == 0) Vfx.dress(level, Fx.LIGHT, c.add(0, 1.0, 0), 0.5, 0.3);
            } else if (s.is("uo_uo_seiryu")) {
                s.cloudTicks = 10;
                if (p.tickCount % 2 == 0) Vfx.puff(level, Fx.CLOUD, c.add(0, -0.15, 0), 0.9, 2);
            }
        }
        if (s.balloonTicks > 0) {
            if (p.tickCount % 2 == 0) Vfx.sphere(level, Fx.RUBBER, c.add(0, 0.9, 0), 1.6 * p.getScale(), 3);
            for (LivingEntity e : Gx.around(level, c.add(0, 0.9, 0), 1.8 * p.getScale(), p)) Gx.knock(e, c, 0.9, 0.3);
        }
        if (s.daikoTicks > 0) {
            // Kaen Daiko: a dragon of flame wraps the body; anything near it burns
            double r = s.inForm("seiryu_dragon") ? 5.0 : 2.6;
            if (p.tickCount % 2 == 0) Vfx.puff(level, Fx.FIRE, c.add(0, 1.0 * p.getScale(), 0), r * 0.6, 3);
            if (p.tickCount % 6 == 0) Vfx.ring(level, Fx.FIRE, c.add(0, 0.8 * p.getScale(), 0), r, p.tickCount % 12 == 0);
            if (p.tickCount % 10 == 0) for (LivingEntity e : Gx.around(level, c, r, p)) { Gx.hurtNow(e, p, 4.0F); e.igniteForSeconds(4); }
            if (p.tickCount % 5 == 0) Gx.scorch(level, Gx.ground(level, c.add(Vfx.randDir(level).multiply(r, 0, r))), 0.4, 0.0F);
        }
        if (s.accelTicks > 0 && p.tickCount % 3 == 0) Vfx.dress(level, Fx.LIGHT, c.add(0, 1.0, 0), 0.6, 0.5);
        if (s.murakumoTicks > 0 && p.tickCount % 2 == 0) {
            Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.5D)).add(0, -0.4, 0);
            Vfx.line(level, Fx.LIGHT, hand, hand.add(p.getLookAngle().scale(1.6D)).add(0, 0.2, 0));
        }
    }

    /** Gear 5 Rubber World: everything near the user is rubber; enemies bounce helplessly. */
    private static void rubberWorld(ServerPlayer p, Pirate s, ServerLevel level) {
        Vec3 c = p.position();
        for (LivingEntity e : Gx.around(level, c, 10, p)) {
            if (e.onGround()) {
                e.setDeltaMovement(e.getDeltaMovement().add((level.random.nextDouble() - 0.5D) * 0.4D, 0.75D, (level.random.nextDouble() - 0.5D) * 0.4D));
                e.hurtMarked = true;
                Gx.hurt(e, p, 2.0F);
                Vfx.arc(level, Fx.NIKA, e.position().add(0, 0.1, 0), level.random.nextFloat() * 360, 0.8, 360, 0, true, 8);
            }
        }
        if (p.tickCount % 8 == 0) Vfx.ring(level, Fx.NIKA, c.add(0, 0.1, 0), 10, false);
    }

    // ---------------------------------------------------------------- keys

    /** V: shift to the next unlocked form (or back to the base form). */
    public static void shiftForm(ServerPlayer p) {
        Pirate s = Gx.data(p);
        if (!s.hasFruit()) return;
        if (isWeak(p, s)) { Gx.msg(p, Component.translatable("message.grandline.sea_weak").withStyle(ChatFormatting.AQUA)); return; }
        if (s.exhaustTicks > 0) { Gx.msg(p, Component.translatable("message.grandline.exhausted").withStyle(ChatFormatting.GRAY)); return; }
        if (s.cd[Pirate.FORM_SLOT] > 0) return;
        FruitInfo f = s.info();
        if (f.forms().size() <= 1) { Gx.msg(p, Component.translatable("message.grandline.no_forms").withStyle(ChatFormatting.GRAY)); return; }
        boolean creative = p.getAbilities().instabuild;
        int next = s.form;
        for (int i = 1; i <= f.forms().size(); i++) {
            int k = (s.form + i) % f.forms().size();
            if (k == 0 || creative || s.bounty().atLeast(f.forms().get(k).tier())) { next = k; break; }
        }
        if (next == s.form) return;
        if (next != 0 && s.stamina < 20 && !creative) { Gx.msg(p, Component.translatable("message.grandline.no_stamina").withStyle(ChatFormatting.GRAY)); return; }
        setForm(p, s, next, false);
        s.setCooldown(Pirate.FORM_SLOT, 30);
        ModNetwork.sync(p);
    }

    public static void setForm(ServerPlayer p, Pirate s, int index, boolean forced) {
        FruitInfo f = s.info();
        if (f == null) return;
        int old = s.form;
        s.form = Math.floorMod(index, f.forms().size());
        s.page = 0;
        s.formTicks = 0;
        s.gigantTicks = 0;
        s.rubberWorldTicks = 0;
        FormInfo form = s.formInfo();
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position().add(0, 1, 0);
        if (s.form != 0 && !forced) {
            switch (form.id()) {
                case "gear_second" -> { Vfx.puff(level, Fx.STEAM, c, 1.4, 14); Gx.sound(level, c, SoundEvents.FIRE_EXTINGUISH, 1.2F, 0.8F); Gx.sound(level, c, SoundEvents.PLAYER_BREATH, 1.0F, 1.4F); }
                case "gear_third" -> { Vfx.sphere(level, Fx.RUBBER, c.add(p.getLookAngle().scale(0.8D)), 1.8, 10); Gx.sound(level, c, SoundEvents.PUFFER_FISH_BLOW_UP, 1.2F, 0.6F); }
                case "boundman", "snakeman" -> { Vfx.puff(level, Fx.STEAM, c, 2.2, 24); Vfx.ring(level, Fx.HAKI, c, 2.5, false); Gx.sound(level, c, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.3F); Gx.sound(level, c, SoundEvents.FIRE_EXTINGUISH, 1.2F, 0.6F); }
                case "gear_fifth" -> {
                    Vfx.sphere(level, Fx.NIKA, c, 2.5, 16); Vfx.ring(level, Fx.NIKA, c, 4, false); Vfx.ring(level, Fx.NIKA, c, 4, true);
                    Gx.sound(level, c, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.5F, 0.5F); Gx.sound(level, c, SoundEvents.BEACON_ACTIVATE, 1.2F, 1.6F);
                    level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 40, 0.8D, 0.8D, 0.8D, 0.1D);
                }
                case "seiryu_hybrid" -> { Vfx.dress(level, Fx.DRAGON, c, 1.5, 3); Vfx.puff(level, Fx.CLOUD, c.add(0, -1, 0), 1.6, 10); Gx.sound(level, c, SoundEvents.ENDER_DRAGON_GROWL, 0.6F, 1.4F); }
                case "seiryu_dragon" -> {
                    Vfx.puff(level, Fx.CLOUD, c, 3.5, 40); Vfx.ring(level, Fx.DRAGON, c, 4, false); Vfx.sphere(level, Fx.DRAGON, c.add(0, 1.5, 0), 3, 12);
                    Gx.sound(level, c, SoundEvents.ENDER_DRAGON_GROWL, 1.4F, 0.8F); Gx.sound(level, c, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0F, 0.9F);
                }
                case "sun_burst" -> { Vfx.sphere(level, Fx.LIGHT, c, 2.5, 10); Vfx.ring(level, Fx.LIGHT, c, 5, false); Gx.sound(level, c, SoundEvents.BEACON_POWER_SELECT, 1.2F, 1.8F); }
                default -> { }
            }
            Gx.msg(p, form.displayName().copy().withColor(form.color()));
        } else if (s.form == 0 && old != 0) {
            Vfx.puff(level, Fx.STEAM, c, 1.2, 8);
            Gx.sound(level, c, SoundEvents.FIRE_EXTINGUISH, 0.8F, 1.1F);
            String oldId = f.forms().get(old).id();
            if (oldId.equals("gear_second") && !forced) { s.exhaustTicks = 60; Gx.effect(p, MobEffects.MOVEMENT_SLOWDOWN, 60, 0); }
            if (oldId.equals("gear_fifth") && !forced) { s.exhaustTicks = 200; Gx.effect(p, MobEffects.WEAKNESS, 200, 1); Gx.effect(p, MobEffects.MOVEMENT_SLOWDOWN, 200, 1); }
        }
        ModNetwork.broadcastForm(p);
    }

    /** B: the fruit's toggle. Light Form (Pika), Flame Clouds (Seiryu). */
    public static void toggle(ServerPlayer p) {
        Pirate s = Gx.data(p);
        if (!s.hasFruit() || !s.info().hasToggle()) return;
        if (isWeak(p, s)) { Gx.msg(p, Component.translatable("message.grandline.sea_weak").withStyle(ChatFormatting.AQUA)); return; }
        if (s.cd[Pirate.TOGGLE_SLOT] > 0) return;
        s.toggle = !s.toggle;
        s.setCooldown(Pirate.TOGGLE_SLOT, 10);
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position().add(0, 1, 0);
        if (s.is("pika_pika")) {
            if (s.toggle) { Vfx.sphere(level, Fx.LIGHT, c, 1.4, 8); Gx.sound(level, c, SoundEvents.BEACON_POWER_SELECT, 0.8F, 2.0F); }
            else Gx.sound(level, c, SoundEvents.BEACON_DEACTIVATE, 0.6F, 2.0F);
        } else if (s.is("uo_uo_seiryu")) {
            if (s.toggle) { Vfx.puff(level, Fx.CLOUD, c.add(0, -0.8, 0), 1.5, 14); Gx.sound(level, c, SoundEvents.FIRECHARGE_USE, 0.8F, 0.7F); }
        }
        Component name = Component.translatable("toggle.grandline." + s.info().toggle());
        Gx.msg(p, Component.translatable(s.toggle ? "message.grandline.toggle_on" : "message.grandline.toggle_off", name).withColor(s.info().color()));
        ModNetwork.broadcastForm(p);
        ModNetwork.sync(p);
    }

    /** U: flip to the next page of moves of the current form. */
    public static void nextPage(ServerPlayer p) {
        Pirate s = Gx.data(p);
        if (!s.hasFruit()) return;
        int pages = s.pages();
        if (pages <= 1) return;
        s.page = (s.page + 1) % pages;
        Gx.sound(p.serverLevel(), p.position(), SoundEvents.UI_BUTTON_CLICK.value(), 0.5F, 1.4F);
        ModNetwork.sync(p);
    }

    public static void afterMove(ServerPlayer p, Pirate s, Move m) {
        // Every move carries a little bounty: the world notices
        s.berries += 50_000L;
    }

    // ---------------------------------------------------------------- damage

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource src = event.getSource();
        if (victim.level().isClientSide) return;

        // Ama no Murakumo: the light blade cuts for extra and leaves a slash of light
        if (src.getEntity() instanceof ServerPlayer attacker && src.getDirectEntity() == attacker && !(victim instanceof Player && victim == attacker)) {
            Pirate a = Gx.data(attacker);
            if (a.hasFruit() && a.murakumoTicks > 0 && !src.is(Gx.FRUIT) && !src.is(Gx.HAKI)) {
                event.setAmount(event.getAmount() + 6.0F * Moves.power(a));
                Vfx.arc(attacker.serverLevel(), Fx.LIGHT, Gx.chest(victim), attacker.getYRot() + 90, 1.2, 150, 0.4, false, 10);
                victim.igniteForSeconds(2);
            }
            if (a.hasFruit() && a.accelTicks > 0 && !src.is(Gx.FRUIT) && !src.is(Gx.HAKI)) {
                event.setAmount(event.getAmount() + 10.0F * Moves.power(a));
                Vfx.sphere(attacker.serverLevel(), Fx.LIGHT, Gx.chest(victim), 1.2, 6);
                Vfx.beam(attacker.serverLevel(), Fx.LIGHT, attacker.getEyePosition(), Gx.chest(victim), 0.2, 5);
                for (LivingEntity o : Gx.around(attacker.serverLevel(), Gx.chest(victim), 2.5, attacker)) if (o != victim) Gx.hurtNow(o, attacker, 6.0F);
                Gx.sound(attacker.serverLevel(), victim.position(), SoundEvents.GENERIC_EXPLODE.value(), 0.6F, 1.8F);
            }
            if (a.hasFruit() && a.inForm("seiryu_dragon") && !src.is(Gx.FRUIT)) {
                event.setAmount(event.getAmount() + 6.0F);
                Gx.knock(victim, attacker.position(), 1.2, 0.4);
            }
        }

        if (!(victim instanceof ServerPlayer p)) return;
        Pirate s = Gx.data(p);
        if (!s.hasFruit()) return;
        boolean weak = isWeak(p, s);
        float amount = event.getAmount();

        switch (s.fruit) {
            case "gomu_gomu" -> {
                if (src.is(DamageTypes.LIGHTNING_BOLT)) { event.setCanceled(true); Vfx.sparks(p.serverLevel(), Gx.chest(p), 10, 0.3); return; }
                if (src.is(DamageTypeTags.IS_FALL)) { event.setCanceled(true); return; }
                // rubber: blunt hits (unarmed mobs, explosions) bounce off; blades and arrows still cut
                boolean blunt = src.is(DamageTypeTags.IS_EXPLOSION) || (src.getDirectEntity() instanceof LivingEntity l && !(l instanceof Player) && l.getMainHandItem().isEmpty() && !(l instanceof Enemy && l.getMainHandItem().getItem() instanceof net.minecraft.world.item.SwordItem));
                if (blunt && !weak) amount *= 0.45F;
                if (s.cannonballTicks > 0 && !weak && src.getDirectEntity() instanceof LivingEntity attacker && attacker != p) {
                    // Tankman: the blow sinks in and the attacker is fired back out
                    event.setCanceled(true);
                    s.cannonballTicks = 0;
                    Gx.haki(attacker, p, event.getAmount() * 2.0F + 20.0F);
                    Gx.knock(attacker, p.position(), 4.0, 1.0);
                    Vfx.impact(p.serverLevel(), Fx.HAKI, Gx.chest(attacker));
                    Vfx.ring(p.serverLevel(), Fx.HAKI, Gx.chest(p), 2.5, true);
                    Gx.sound(p.serverLevel(), p.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 0.9F);
                    return;
                }
                if (s.balloonTicks > 0 && !weak) {
                    if (src.getDirectEntity() instanceof Projectile proj) {
                        proj.setDeltaMovement(proj.getDeltaMovement().scale(-1.2D));
                        proj.setOwner(p);
                        proj.hurtMarked = true;
                        event.setCanceled(true);
                        Vfx.impact(p.serverLevel(), Fx.RUBBER, proj.position());
                        return;
                    }
                    if (blunt) { event.setCanceled(true); if (src.getEntity() != null) Gx.knock(src.getEntity(), p.position(), 1.5, 0.4); return; }
                }
                if (s.inForm("boundman") || s.inForm("gear_fifth")) amount *= 0.7F;
                if (s.inForm("gear_third") && src.getDirectEntity() instanceof Projectile) amount *= 0.5F;
            }
            case "pika_pika" -> {
                // Logia: physical attacks pass through a body of light. Haki, the sea and exhaustion still land.
                boolean physical = src.is(DamageTypeTags.IS_PROJECTILE) || src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypeTags.IS_FALL)
                        || src.is(DamageTypeTags.IS_FIRE) || src.is(DamageTypes.MOB_ATTACK) || src.is(DamageTypes.MOB_ATTACK_NO_AGGRO) || src.is(DamageTypes.PLAYER_ATTACK)
                        || src.is(DamageTypes.CACTUS) || src.is(DamageTypes.SWEET_BERRY_BUSH) || src.is(DamageTypes.FALLING_BLOCK) || src.is(DamageTypes.FALLING_ANVIL)
                        || src.is(DamageTypes.MOB_PROJECTILE) || src.is(DamageTypes.ARROW) || src.is(DamageTypes.TRIDENT) || src.is(DamageTypes.FIREBALL) || src.is(DamageTypes.THROWN);
                boolean haki = src.is(Gx.HAKI) || src.getEntity() instanceof Warden || src.getEntity() instanceof WitherBoss || src.getEntity() instanceof EnderDragon
                        || (src.getEntity() instanceof ServerPlayer a && Gx.data(a).hardenTicks > 0);
                if (physical && !haki && !weak && s.exhaustTicks == 0) {
                    event.setCanceled(true);
                    ServerLevel level = p.serverLevel();
                    Vec3 at = src.getSourcePosition() != null ? src.getSourcePosition() : p.position();
                    Vfx.dress(level, Fx.LIGHT, Gx.chest(p), 0.7, 1.5);
                    Vfx.line(level, Fx.LIGHT, Gx.chest(p), Gx.chest(p).add(Gx.chest(p).subtract(at).normalize().scale(1.5)));
                    if (p.tickCount % 5 == 0) Gx.sound(level, p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.8F);
                    return;
                }
            }
            case "uo_uo_seiryu" -> {
                if (src.is(DamageTypeTags.IS_FIRE)) { event.setCanceled(true); return; }
                if (s.inForm("seiryu_hybrid")) amount *= 0.75F;
                if (s.inForm("seiryu_dragon")) amount *= 0.55F;
            }
            default -> { }
        }
        event.setAmount(amount);
    }

    // ---------------------------------------------------------------- bounty

    public static void onKill(ServerPlayer p, LivingEntity victim) {
        Pirate s = Gx.data(p);
        long gain;
        if (victim instanceof EnderDragon) gain = 600_000_000L;
        else if (victim instanceof WitherBoss) gain = 300_000_000L;
        else if (victim instanceof Warden) gain = 150_000_000L;
        else if (victim instanceof Player) gain = Math.max(20_000_000L, Gx.data((Player) victim).berries / 4);
        else if (victim instanceof Enemy) gain = 1_000_000L + (long) (victim.getMaxHealth() * 80_000L) + p.getRandom().nextInt(2_000_000);
        else gain = 100_000L;
        addBerries(p, gain);
        s.kills++;
    }

    public static void addBerries(ServerPlayer p, long amount) {
        Pirate s = Gx.data(p);
        Bounty before = s.bounty();
        s.berries = Math.max(0, s.berries + amount);
        Bounty after = s.bounty();
        if (after != before && amount > 0) {
            Gx.msg(p, Component.translatable("message.grandline.bounty_up", Bounty.berries(s.berries), after.display()).withStyle(ChatFormatting.GOLD));
            p.sendSystemMessage(Component.translatable("message.grandline.bounty_up", Bounty.berries(s.berries), after.display()).withStyle(ChatFormatting.GOLD));
            Gx.sound(p.serverLevel(), p.position(), SoundEvents.PLAYER_LEVELUP, 1.0F, 0.7F);
        }
        ModNetwork.sync(p);
    }

    // ---------------------------------------------------------------- eating

    /** Eating a devil fruit. A second fruit is fatal, as it should be. */
    public static void eat(ServerPlayer p, FruitInfo fruit) {
        Pirate s = Gx.data(p);
        ServerLevel level = p.serverLevel();
        Vec3 c = p.position().add(0, 1, 0);
        if (s.hasFruit()) {
            p.sendSystemMessage(Component.translatable("message.grandline.second_fruit").withStyle(ChatFormatting.DARK_RED));
            level.explode(null, c.x, c.y, c.z, 2.0F, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            Vfx.blast(level, ModRegistries.RUBBER.get(), c, 2.0);
            p.hurt(Gx.source(level, Gx.HAKI, null, null), Float.MAX_VALUE);
            if (p.isAlive()) p.kill();
            return;
        }
        s.fruit = fruit.id();
        s.form = 0;
        s.toggle = false;
        s.resetLive();
        s.stamina = 100;
        Gx.effect(p, MobEffects.CONFUSION, 120, 0);
        p.sendSystemMessage(Component.translatable("message.grandline.ate", fruit.displayName()).withColor(fruit.color()));
        p.sendSystemMessage(Component.translatable("fruit.grandline." + fruit.id() + ".desc").withStyle(ChatFormatting.GRAY));
        p.sendSystemMessage(Component.translatable("message.grandline.controls").withStyle(ChatFormatting.DARK_GRAY));
        Gx.sound(level, c, SoundEvents.PLAYER_BURP, 1.0F, 0.6F);
        Gx.sound(level, c, SoundEvents.TOTEM_USE, 0.6F, 1.6F);
        switch (fruit.id()) {
            case "gomu_gomu" -> { Vfx.sphere(level, Fx.RUBBER, c, 1.6, 12); Vfx.ring(level, Fx.RUBBER, c, 3, false); }
            case "pika_pika" -> { Vfx.sphere(level, Fx.LIGHT, c, 2.0, 12); Vfx.ring(level, Fx.LIGHT, c, 3.5, false); level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 30, 0.6D, 0.6D, 0.6D, 0.08D); }
            case "uo_uo_seiryu" -> { Vfx.puff(level, Fx.CLOUD, c, 2.0, 16); Vfx.dress(level, Fx.DRAGON, c, 1.5, 3); Gx.sound(level, c, SoundEvents.ENDER_DRAGON_GROWL, 0.5F, 1.6F); }
            default -> { }
        }
        ModNetwork.broadcastForm(p);
        ModNetwork.sync(p);
    }

    /** On death with the config on: the power leaves and the fruit drops where the user fell. */
    public static void onDeath(ServerPlayer p) {
        Pirate s = Gx.data(p);
        if (!s.hasFruit()) return;
        if (Config.LOSE_FRUIT_ON_DEATH.get()) {
            net.minecraft.world.item.Item item = ModRegistries.fruitItem(s.fruit);
            if (item != null) {
                net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(p.level(), p.getX(), p.getY() + 0.5D, p.getZ(), new net.minecraft.world.item.ItemStack(item));
                drop.setGlowingTag(true);
                p.level().addFreshEntity(drop);
            }
            s.fruit = "";
            s.form = 0;
            s.toggle = false;
            p.sendSystemMessage(Component.translatable("message.grandline.fruit_lost").withStyle(ChatFormatting.DARK_PURPLE));
        } else {
            s.form = 0;
            s.toggle = false;
        }
        s.resetLive();
        revokeFlight(p, s);
        clearModifiers(p);
        ModNetwork.broadcastForm(p);
    }

    public static void clearFruit(ServerPlayer p) {
        Pirate s = Gx.data(p);
        s.fruit = "";
        s.form = 0;
        s.toggle = false;
        s.resetLive();
        revokeFlight(p, s);
        clearModifiers(p);
        if (p.hasGlowingTag()) p.setGlowingTag(false);
        ModNetwork.broadcastForm(p);
        ModNetwork.sync(p);
    }

}
