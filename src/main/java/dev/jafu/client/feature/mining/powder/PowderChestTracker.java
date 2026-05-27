package dev.jafu.client.feature.mining.powder;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.jafu.client.module.JafuModules;

public final class PowderChestTracker {
    private static final String GEMSTONE_POWDER = "Gemstone Powder";

    private final Map<String, Long> totals = new LinkedHashMap<>();
    private boolean awaitingRewards;
    private boolean collectingRewards;
    private boolean countedCurrentChest;
    private int chests;
    private long sessionStartMillis;
    private long currentChestGemstonePowder;
    private long bestChestGemstonePowder;

    public void acceptMessage(String rawMessage) {
        if (!JafuModules.isEnabled(JafuModules.POWDER_CHEST_TRACKER)) {
            return;
        }

        String message = PowderChestParser.clean(rawMessage);
        if (message.isEmpty()) {
            return;
        }

        if (PowderChestParser.isChestStart(message)) {
            finishCurrentChest();
            awaitingRewards = true;
            collectingRewards = false;
            countedCurrentChest = false;
            currentChestGemstonePowder = 0L;
            return;
        }

        if (awaitingRewards && PowderChestParser.isRewardsHeader(message)) {
            collectingRewards = true;
            countCurrentChest();
            return;
        }

        if (!collectingRewards) {
            return;
        }

        if (PowderChestParser.isSeparator(message)) {
            finishCurrentChest();
            awaitingRewards = false;
            collectingRewards = false;
            countedCurrentChest = false;
            currentChestGemstonePowder = 0L;
            return;
        }

        PowderChestParser.parseReward(message).ifPresent(this::recordDrop);
    }

    public PowderChestSnapshot snapshot() {
        long elapsedMillis = sessionStartMillis == 0L ? 0L : System.currentTimeMillis() - sessionStartMillis;
        PowderChestSessionStats stats = new PowderChestSessionStats(
                elapsedMillis,
                chests,
                totals.getOrDefault(GEMSTONE_POWDER, 0L),
                Math.max(bestChestGemstonePowder, currentChestGemstonePowder)
        );
        return new PowderChestSnapshot(chests, Map.copyOf(totals), stats);
    }

    private void countCurrentChest() {
        if (!countedCurrentChest) {
            if (sessionStartMillis == 0L) {
                sessionStartMillis = System.currentTimeMillis();
            }
            chests++;
            countedCurrentChest = true;
        }
    }

    private void recordDrop(PowderChestDrop drop) {
        totals.merge(drop.name(), drop.amount(), Long::sum);
        if (GEMSTONE_POWDER.equals(drop.name())) {
            currentChestGemstonePowder += drop.amount();
        }
    }

    private void finishCurrentChest() {
        if (countedCurrentChest) {
            bestChestGemstonePowder = Math.max(bestChestGemstonePowder, currentChestGemstonePowder);
        }
    }
}
