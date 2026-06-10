package dev.jafu.client.feature.garden.rng;

import java.util.List;
import java.util.Optional;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;

public enum GardenCrop {
    WHEAT("wheat", "Wheat", "wheat"),
    CARROT("carrot", "Carrot", "carrots"),
    POTATO("potato", "Potato", "potatoes"),
    PUMPKIN("pumpkin", "Pumpkin", "pumpkin"),
    SUGAR_CANE("sugar_cane", "Sugar Cane", "sugar_cane"),
    MELON("melon", "Melon", "melon"),
    CACTUS("cactus", "Cactus", "cactus"),
    COCOA_BEANS("cocoa_beans", "Cocoa Beans", "cocoa"),
    MUSHROOM("mushroom", "Mushroom", "red_mushroom", "brown_mushroom"),
    NETHER_WART("nether_wart", "Nether Wart", "nether_wart"),
    SUNFLOWER("sunflower", "Sunflower", "sunflower"),
    MOONFLOWER("moonflower", "Moonflower", "oxeye_daisy"),
    WILD_ROSE("wild_rose", "Wild Rose", "rose_bush", "poppy");

    private static final List<GardenCrop> VALUES = List.of(values());

    private final String id;
    private final String label;
    private final List<String> blockIds;

    GardenCrop(String id, String label, String... blockIds) {
        this.id = id;
        this.label = label;
        this.blockIds = List.of(blockIds);
    }

    public static Optional<GardenCrop> fromBlockState(BlockState state) {
        if (state == null || state.isAir()) {
            return Optional.empty();
        }

        String blockId = Registries.BLOCK.getId(state.getBlock()).getPath();
        for (GardenCrop crop : VALUES) {
            if (crop.blockIds.contains(blockId)) {
                return Optional.of(crop);
            }
        }
        return Optional.empty();
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }
}
