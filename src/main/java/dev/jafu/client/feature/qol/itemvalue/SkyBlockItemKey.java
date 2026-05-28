package dev.jafu.client.feature.qol.itemvalue;

import java.util.Locale;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;

public record SkyBlockItemKey(String productId, String auctionName) {
    public static SkyBlockItemKey from(ItemStack stack) {
        String productId = extractSkyBlockId(stack);
        String auctionName = cleanName(stack.getName().getString());
        if (productId.isBlank()) {
            productId = fallbackProductId(stack);
        }
        return new SkyBlockItemKey(productId, auctionName);
    }

    private static String extractSkyBlockId(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) {
            return "";
        }

        NbtCompound nbt = customData.copyNbt();
        NbtCompound extraAttributes = nbt.getCompoundOrEmpty("ExtraAttributes");
        return extraAttributes.getString("id", "").trim();
    }

    private static String fallbackProductId(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).getPath().toUpperCase(Locale.ROOT);
    }

    public static String auctionLookupKey(String name) {
        return cleanName(name)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[✪➊➋➌➍➎➏➐➑➒➓]", "")
                .replaceAll("\\[[^]]+]", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String cleanName(String value) {
        return value == null ? "" : value
                .replaceAll("Â§.", "")
                .replaceAll("Ã‚Â§.", "")
                .replaceAll("[\\p{Cc}\\p{Cf}]", "")
                .trim();
    }
}
