package dev.jafu.client.feature.qol.itemvalue;

public record ItemPriceEstimate(
        String productId,
        BazaarPrice bazaarPrice,
        AuctionPrice auctionPrice,
        boolean loading,
        long updatedAtMillis
) {
    public boolean hasAnyPrice() {
        return bazaarPrice != null || auctionPrice != null;
    }

    public record BazaarPrice(double instantSell, double instantBuy) {
    }

    public record AuctionPrice(double lowestBin, int samples) {
    }
}
