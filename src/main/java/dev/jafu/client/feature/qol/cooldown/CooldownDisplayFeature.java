package dev.jafu.client.feature.qol.cooldown;

import dev.jafu.client.JafuClient;
import dev.jafu.client.module.JafuModules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class CooldownDisplayFeature {
    private static final CooldownDisplayHud HUD = new CooldownDisplayHud();
    private static ActiveCooldown activeCooldown;
    private static boolean wasUsePressed;
    private static boolean wasAttackPressed;

    private CooldownDisplayFeature() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(CooldownDisplayFeature::tick);
        HudElementRegistry.addLast(Identifier.of(JafuClient.MOD_ID, "cooldown_display"), HUD::render);
    }

    public static CooldownDisplayState state() {
        if (activeCooldown == null) {
            return CooldownDisplayState.empty();
        }

        long remaining = activeCooldown.remainingMillis(System.currentTimeMillis());
        if (remaining <= 0L) {
            activeCooldown = null;
            return CooldownDisplayState.empty();
        }
        return new CooldownDisplayState(true, activeCooldown.itemName(), remaining, activeCooldown.durationMillis());
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null) {
            activeCooldown = null;
            wasUsePressed = false;
            wasAttackPressed = false;
            return;
        }

        boolean usePressed = client.options.useKey.isPressed();
        boolean attackPressed = client.options.attackKey.isPressed();
        if (enabled() && client.currentScreen == null) {
            if (CooldownDisplaySettings.INSTANCE.isEnabled(CooldownDisplayOption.TRACK_RIGHT_CLICK) && usePressed && !wasUsePressed) {
                recordUse(client.player);
            }
            if (CooldownDisplaySettings.INSTANCE.isEnabled(CooldownDisplayOption.TRACK_LEFT_CLICK) && attackPressed && !wasAttackPressed) {
                recordStack(client.player.getMainHandStack());
            }
        }

        wasUsePressed = usePressed;
        wasAttackPressed = attackPressed;
        if (activeCooldown != null && activeCooldown.remainingMillis(System.currentTimeMillis()) <= 0L) {
            activeCooldown = null;
        }
    }

    private static void recordUse(ClientPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (!mainHand.isEmpty()) {
            recordStack(mainHand);
            return;
        }
        recordStack(player.getOffHandStack());
    }

    private static void recordStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        long nowMillis = System.currentTimeMillis();
        long durationMillis = CooldownDisplaySettings.INSTANCE.cooldownMillis();
        activeCooldown = new ActiveCooldown(
                key(stack),
                stack.getName().getString(),
                nowMillis,
                durationMillis
        );
    }

    private static boolean enabled() {
        return CooldownDisplaySettings.INSTANCE.enabled() && JafuModules.isEnabled(JafuModules.COOLDOWN_DISPLAY);
    }

    private static String key(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()) + "|" + stack.getName().getString();
    }

    private record ActiveCooldown(String key, String itemName, long startedAtMillis, long durationMillis) {
        private long remainingMillis(long nowMillis) {
            return Math.max(0L, startedAtMillis + durationMillis - nowMillis);
        }
    }
}
