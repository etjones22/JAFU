package dev.jafu.client.feature.mining.powder;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.jafu.client.module.JafuModules;

public final class PowderChestTracker {
    private final Map<String, Long> totals = new LinkedHashMap<>();
    private boolean awaitingRewards;
    private boolean collectingRewards;
    private boolean countedCurrentChest;
    private int chests;

    public void acceptMessage(String rawMessage) {
        if (!JafuModules.isEnabled(JafuModules.POWDER_CHEST_TRACKER)) {
            return;
        }

        String message = PowderChestParser.clean(rawMessage);
        if (message.isEmpty()) {
            return;
        }

        if (PowderChestParser.isChestStart(message)) {
            awaitingRewards = true;
            collectingRewards = false;
            countedCurrentChest = false;
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
            awaitingRewards = false;
            collectingRewards = false;
            countedCurrentChest = false;
            return;
        }

        PowderChestParser.parseReward(message).ifPresent(this::recordDrop);
    }

    public PowderChestSnapshot snapshot() {
        return new PowderChestSnapshot(chests, Map.copyOf(totals));
    }

    private void countCurrentChest() {
        if (!countedCurrentChest) {
            chests++;
            countedCurrentChest = true;
        }
    }

    private void recordDrop(PowderChestDrop drop) {
        totals.merge(drop.name(), drop.amount(), Long::sum);
    }
}
