package dev.jafu.client.feature.skyblock.storage;

import java.util.List;

public record StorageSnapshot(String title, String sourceName, long capturedAtMillis, List<StorageIndexedItem> items) {
    public int occupiedSlots() {
        return items.size();
    }

    public int totalItems() {
        int total = 0;
        for (StorageIndexedItem item : items) {
            total += Math.max(0, item.count());
        }
        return total;
    }
}
