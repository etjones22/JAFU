package dev.jafu.client.feature.mining.powder;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.jafu.client.module.JafuModules;

public final class PowderChestTracker {
    private static final String GEMSTONE_POWDER = "Gemstone Powder";
    private static final long COUNTDOWN_MILLIS = 5_000L;

    private final Map<String, Long> totals = new LinkedHashMap<>();
    private boolean awaitingRewards;
    private boolean collectingRewards;
    private boolean countedCurrentChest;
    private boolean resetCountdownActive;
    private int chests;
    private int lastCountdownSecond;
    private long sessionStartMillis;
    private long lastChestOpenedMillis;
    private long resetCountdownStartMillis;
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
            markChestActivity();
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

    public void tick(MessageSink messageSink) {
        if (!JafuModules.isEnabled(JafuModules.POWDER_CHEST_TRACKER)) {
            return;
        }
        if (!PowderChestSettings.INSTANCE.autoResetEnabled()) {
            cancelAutoResetCountdown();
            return;
        }
        if (chests <= 0 || lastChestOpenedMillis == 0L) {
            return;
        }

        long nowMillis = System.currentTimeMillis();
        long idleMillis = nowMillis - lastChestOpenedMillis;
        if (!resetCountdownActive) {
            if (idleMillis >= PowderChestSettings.INSTANCE.autoResetMillis()) {
                resetCountdownActive = true;
                resetCountdownStartMillis = nowMillis;
                lastCountdownSecond = 0;
            } else {
                return;
            }
        }

        long countdownElapsedMillis = nowMillis - resetCountdownStartMillis;
        int secondsRemaining = (int) Math.ceil((COUNTDOWN_MILLIS - countdownElapsedMillis) / 1000.0D);
        if (secondsRemaining > 0) {
            if (secondsRemaining != lastCountdownSecond) {
                lastCountdownSecond = secondsRemaining;
                messageSink.accept("Powder Chest Tracker will reset in " + secondsRemaining + " second" + (secondsRemaining == 1 ? "" : "s") + ".");
            }
            return;
        }

        reset();
        messageSink.accept("Powder Chest Tracker reset after " + formatSeconds(PowderChestSettings.INSTANCE.autoResetSeconds()) + " without a chest.");
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

    public void reset() {
        totals.clear();
        awaitingRewards = false;
        collectingRewards = false;
        countedCurrentChest = false;
        resetCountdownActive = false;
        chests = 0;
        lastCountdownSecond = 0;
        sessionStartMillis = 0L;
        lastChestOpenedMillis = 0L;
        resetCountdownStartMillis = 0L;
        currentChestGemstonePowder = 0L;
        bestChestGemstonePowder = 0L;
    }

    private void countCurrentChest() {
        if (!countedCurrentChest) {
            if (sessionStartMillis == 0L) {
                sessionStartMillis = System.currentTimeMillis();
            }
            markChestActivity();
            chests++;
            countedCurrentChest = true;
        }
    }

    private void markChestActivity() {
        lastChestOpenedMillis = System.currentTimeMillis();
        cancelAutoResetCountdown();
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

    private void cancelAutoResetCountdown() {
        resetCountdownActive = false;
        resetCountdownStartMillis = 0L;
        lastCountdownSecond = 0;
    }

    private static String formatSeconds(double seconds) {
        int roundedSeconds = (int) Math.round(seconds);
        if (roundedSeconds < 60) {
            return roundedSeconds + " seconds";
        }
        int minutes = roundedSeconds / 60;
        int remainingSeconds = roundedSeconds % 60;
        if (remainingSeconds == 0) {
            return minutes + " minute" + (minutes == 1 ? "" : "s");
        }
        return minutes + "m " + remainingSeconds + "s";
    }

    @FunctionalInterface
    public interface MessageSink {
        void accept(String message);
    }
}
