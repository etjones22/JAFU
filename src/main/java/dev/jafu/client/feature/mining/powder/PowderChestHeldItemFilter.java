package dev.jafu.client.feature.mining.powder;

import java.util.Locale;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

final class PowderChestHeldItemFilter {
    private PowderChestHeldItemFilter() {
    }

    static boolean shouldShow(MinecraftClient client) {
        if (!PowderChestSettings.INSTANCE.onlyShowWithMiningTool()) {
            return true;
        }
        if (client.player == null) {
            return false;
        }
        return isMiningTool(client.player.getMainHandStack()) || isMiningTool(client.player.getOffHandStack());
    }

    private static boolean isMiningTool(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String itemId = Registries.ITEM.getId(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        if (itemId.endsWith("_pickaxe") || itemId.contains("pickaxe")) {
            return true;
        }

        String name = stack.getName().getString()
                .replaceAll("§.", "")
                .toLowerCase(Locale.ROOT);
        return name.contains("drill") || name.contains("pickaxe");
    }
}
