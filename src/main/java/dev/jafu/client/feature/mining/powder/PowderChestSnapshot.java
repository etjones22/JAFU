package dev.jafu.client.feature.mining.powder;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record PowderChestSnapshot(int chests, Map<String, Long> totals, PowderChestSessionStats stats) {
    private static final String GEMSTONE_POWDER = "Gemstone Powder";

    public long gemstonePowder() {
        return totals.getOrDefault(GEMSTONE_POWDER, 0L);
    }

    public List<PowderChestDrop> topDrops(int limit) {
        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(entry -> new PowderChestDrop(entry.getKey(), entry.getValue()))
                .toList();
    }
}
