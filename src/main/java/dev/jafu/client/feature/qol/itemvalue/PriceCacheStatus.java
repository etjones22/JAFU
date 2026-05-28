package dev.jafu.client.feature.qol.itemvalue;

public record PriceCacheStatus(int bazaarProducts, int auctionItems, boolean loading, long updatedAtMillis) {
    public String ageLabel() {
        if (updatedAtMillis <= 0L) {
            return "not loaded";
        }

        long elapsedSeconds = Math.max(0L, (System.currentTimeMillis() - updatedAtMillis) / 1000L);
        if (elapsedSeconds < 60L) {
            return elapsedSeconds + "s ago";
        }

        long minutes = elapsedSeconds / 60L;
        if (minutes < 60L) {
            return minutes + "m ago";
        }

        return (minutes / 60L) + "h ago";
    }
}
