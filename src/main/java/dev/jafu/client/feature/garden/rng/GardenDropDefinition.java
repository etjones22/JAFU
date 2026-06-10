package dev.jafu.client.feature.garden.rng;

import java.util.List;

public record GardenDropDefinition(
        String id,
        String displayName,
        GardenDropFamily family,
        List<GardenCrop> crops,
        double defaultChance,
        String productId,
        String auctionName,
        int color
) {
    public boolean appliesTo(GardenCrop crop) {
        return crop != null && crops.contains(crop);
    }
}
