package dev.jafu.client.feature.garden.rng;

import java.util.Locale;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

final class GardenRngHeldItemFilter {
    private GardenRngHeldItemFilter() {
    }

    static boolean shouldShow(MinecraftClient client) {
        if (!GardenRngSettings.INSTANCE.onlyShowWithWheatHoe()) {
            return true;
        }
        if (client.player == null) {
            return false;
        }
        return isWheatHoe(client.player.getMainHandStack()) || isWheatHoe(client.player.getOffHandStack());
    }

    private static boolean isWheatHoe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String itemId = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        String name = strippedName(stack);
        return name.contains("wheat hoe") || (name.contains("wheat") && (name.contains("hoe") || itemId.contains("hoe")));
    }

    private static String strippedName(ItemStack stack) {
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '\u00A7' && i + 1 < name.length()) {
                i++;
                continue;
            }
            builder.append(c);
        }
        return builder.toString();
    }
}
