package dev.jafu.client.feature.garden.rng;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class GardenDropDefinitions {
    private static final double FEAST_RARE_CROP_CHANCE = 1.0D / 18_000.0D;
    private static final double SEASONING_CHANCE = 1.0D / 2_250.0D;

    private static final List<GardenDropDefinition> DEFINITIONS = buildDefinitions();

    private GardenDropDefinitions() {
    }

    public static List<GardenDropDefinition> all() {
        return DEFINITIONS;
    }

    public static List<GardenDropDefinition> forCrop(GardenCrop crop) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.appliesTo(crop))
                .sorted(Comparator.comparing(GardenDropDefinition::family).thenComparing(GardenDropDefinition::displayName))
                .toList();
    }

    public static Optional<GardenDropDefinition> byId(String id) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.id().equals(id))
                .findFirst();
    }

    public static Optional<GardenDropDefinition> matchMessage(String message) {
        String normalized = normalize(message);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        return DEFINITIONS.stream()
                .filter(definition -> normalized.contains(normalize(definition.displayName())))
                .findFirst();
    }

    private static List<GardenDropDefinition> buildDefinitions() {
        List<GardenDropDefinition> definitions = new ArrayList<>();
        definitions.add(drop("seasoning", "Seasoning", GardenDropFamily.FEAST, SEASONING_CHANCE, "SEASONING", "Seasoning", 0xFFFFD94A, GardenCrop.values()));
        definitions.add(drop("cornucopia", "Cornucopia", GardenDropFamily.FEAST, FEAST_RARE_CROP_CHANCE, "CORNUCOPIA", "Cornucopia", 0xFFFFAA00, GardenCrop.WHEAT));
        definitions.add(drop("carrot_zest", "Carrot Zest", GardenDropFamily.FEAST, FEAST_RARE_CROP_CHANCE, "CARROT_ZEST", "Carrot Zest", 0xFFFF7F2A, GardenCrop.CARROT));
        definitions.add(drop("deepfries", "Deepfries", GardenDropFamily.FEAST, FEAST_RARE_CROP_CHANCE, "DEEPFRIES", "Deepfries", 0xFFFFD35D, GardenCrop.POTATO));
        definitions.add(drop("aggourdian", "Aggourdian", GardenDropFamily.FEAST, FEAST_RARE_CROP_CHANCE, "AGGOURDIAN", "Aggourdian", 0xFFFF8F38, GardenCrop.PUMPKIN));
        definitions.add(drop("cane_knot", "Cane Knot", GardenDropFamily.FEAST, FEAST_RARE_CROP_CHANCE, "CANE_KNOT", "Cane Knot", 0xFFB6F36E, GardenCrop.SUGAR_CANE));
        definitions.add(drop("melon_juice", "Melon Juice", GardenDropFamily.FEAST, FEAST_RARE_CROP_CHANCE, "MELON_JUICE", "Melon Juice", 0xFFFF6C8A, GardenCrop.MELON));
        definitions.add(drop("cactus_flower", "Cactus Flower", GardenDropFamily.FEAST, FEAST_RARE_CROP_CHANCE, "CACTUS_FLOWER", "Cactus Flower", 0xFF65E572, GardenCrop.CACTUS));
        definitions.add(drop("designer_coffee_beans", "Designer Coffee Beans", GardenDropFamily.FEAST, FEAST_RARE_CROP_CHANCE, "DESIGNER_COFFEE_BEANS", "Designer Coffee Beans", 0xFFC47B45, GardenCrop.COCOA_BEANS));
        definitions.add(drop("feastfungus", "Feastfungus", GardenDropFamily.FEAST, FEAST_RARE_CROP_CHANCE, "FEASTFUNGUS", "Feastfungus", 0xFFD58FFF, GardenCrop.MUSHROOM));
        definitions.add(drop("botroot", "Botroot", GardenDropFamily.FEAST, FEAST_RARE_CROP_CHANCE, "BOTROOT", "Botroot", 0xFFB061FF, GardenCrop.NETHER_WART));
        definitions.add(drop("salted_sunflower_seeds", "Salted Sunflower Seeds", GardenDropFamily.FEAST, FEAST_RARE_CROP_CHANCE, "SALTED_SUNFLOWER_SEEDS", "Salted Sunflower Seeds", 0xFFFFE56D, GardenCrop.SUNFLOWER));
        definitions.add(drop("crystalized_moonlight", "Crystalized Moonlight", GardenDropFamily.FEAST, FEAST_RARE_CROP_CHANCE, "CRYSTALIZED_MOONLIGHT", "Crystalized Moonlight", 0xFF9ED6FF, GardenCrop.MOONFLOWER));
        definitions.add(drop("floral_gelatin", "Floral Gelatin", GardenDropFamily.FEAST, FEAST_RARE_CROP_CHANCE, "FLORAL_GELATIN", "Floral Gelatin", 0xFFFF77B7, GardenCrop.WILD_ROSE));

        definitions.add(drop("cropie", "Cropie", GardenDropFamily.ARMOR, 0.0005D, "CROPIE", "Cropie", 0xFF55FF55, GardenCrop.WHEAT, GardenCrop.CARROT, GardenCrop.POTATO));
        definitions.add(drop("squash", "Squash", GardenDropFamily.ARMOR, 0.0003D, "SQUASH", "Squash", 0xFFFFAA00, GardenCrop.PUMPKIN, GardenCrop.MELON, GardenCrop.COCOA_BEANS));
        definitions.add(drop("fermento", "Fermento", GardenDropFamily.ARMOR, 0.00007D, "FERMENTO", "Fermento", 0xFFAA55FF, GardenCrop.SUGAR_CANE, GardenCrop.CACTUS, GardenCrop.MUSHROOM, GardenCrop.NETHER_WART));
        definitions.add(drop("helianthus", "Helianthus", GardenDropFamily.ARMOR, 0.00004D, "HELIANTHUS", "Helianthus", 0xFFFFEE55, GardenCrop.SUNFLOWER, GardenCrop.MOONFLOWER, GardenCrop.WILD_ROSE));

        definitions.add(drop("warty", "Warty", GardenDropFamily.OTHER, 0.0005D, "WARTY", "Warty", 0xFFAA3355, GardenCrop.NETHER_WART));
        definitions.add(drop("burrowing_spores", "Burrowing Spores", GardenDropFamily.OTHER, 0.000001D, "BURROWING_SPORES", "Burrowing Spores", 0xFFA05A32, GardenCrop.MUSHROOM));
        definitions.add(drop("wild_strawberry_dye", "Wild Strawberry Dye", GardenDropFamily.OTHER, 0.000001D, "DYE_WILD_STRAWBERRY", "Wild Strawberry Dye", 0xFFFF5C8A, GardenCrop.values()));
        definitions.add(drop("ray_of_helios", "Ray of Helios", GardenDropFamily.OTHER, 0.000001D, "RAY_OF_HELIOS", "Ray of Helios", 0xFFFFF182, GardenCrop.values()));
        return List.copyOf(definitions);
    }

    private static GardenDropDefinition drop(
            String id,
            String displayName,
            GardenDropFamily family,
            double defaultChance,
            String productId,
            String auctionName,
            int color,
            GardenCrop... crops
    ) {
        return new GardenDropDefinition(id, displayName, family, List.of(crops), defaultChance, productId, auctionName, color);
    }

    private static String normalize(String value) {
        return value == null ? "" : value
                .toLowerCase(Locale.ROOT)
                .replaceAll("§.", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
