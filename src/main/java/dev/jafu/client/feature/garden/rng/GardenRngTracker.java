package dev.jafu.client.feature.garden.rng;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.jafu.client.feature.qol.itemvalue.ItemPriceEstimate;
import dev.jafu.client.feature.qol.itemvalue.SkyBlockItemKey;
import dev.jafu.client.feature.qol.itemvalue.SkyBlockPriceService;
import dev.jafu.client.module.JafuModules;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public final class GardenRngTracker {
    private static final long SAME_BLOCK_DEBOUNCE_MILLIS = 850L;

    private final Map<String, DropCounter> counters = new HashMap<>();
    private GardenCrop activeCrop;
    private BlockPos lastAttemptPos;
    private long lastAttemptMillis;
    private long sessionStartMillis;
    private long sessionAttempts;

    public void recordHarvestAttempt(BlockState state, BlockPos pos) {
        if (!isEnabled()) {
            return;
        }

        GardenCrop.fromBlockState(state).ifPresent(crop -> recordHarvestAttempt(crop, pos));
    }

    public void acceptMessage(String rawMessage) {
        if (!isEnabled() || rawMessage == null || rawMessage.isBlank()) {
            return;
        }

        resetIfIdle(System.currentTimeMillis());
        GardenDropDefinitions.matchMessage(rawMessage).ifPresent(this::recordDrop);
    }

    public GardenRngSnapshot snapshot() {
        if (!isEnabled()) {
            return GardenRngSnapshot.empty();
        }

        long nowMillis = System.currentTimeMillis();
        resetIfIdle(nowMillis);
        long elapsedMillis = sessionStartMillis == 0L ? 0L : nowMillis - sessionStartMillis;
        double attemptsPerHour = elapsedMillis <= 0L ? 0.0D : sessionAttempts * 3_600_000.0D / elapsedMillis;
        List<GardenDropState> drops = visibleDrops().stream()
                .map(this::stateFor)
                .toList();
        double valuePerAttempt = drops.stream()
                .mapToDouble(drop -> drop.chance() * drop.price())
                .sum();
        return new GardenRngSnapshot(activeCrop, sessionAttempts, elapsedMillis, drops, attemptsPerHour, attemptsPerHour * valuePerAttempt);
    }

    public void resetAll() {
        counters.clear();
        activeCrop = null;
        lastAttemptPos = null;
        lastAttemptMillis = 0L;
        sessionStartMillis = 0L;
        sessionAttempts = 0L;
    }

    public void resetActiveDrops() {
        for (GardenDropDefinition definition : visibleDrops()) {
            counters.remove(definition.id());
        }
    }

    public void resetDrop(String id) {
        counters.remove(id);
    }

    public void tick() {
        if (!isEnabled()) {
            return;
        }
        resetIfIdle(System.currentTimeMillis());
    }

    private void recordHarvestAttempt(GardenCrop crop, BlockPos pos) {
        long nowMillis = System.currentTimeMillis();
        resetIfIdle(nowMillis);
        if (pos != null && pos.equals(lastAttemptPos) && nowMillis - lastAttemptMillis < SAME_BLOCK_DEBOUNCE_MILLIS) {
            return;
        }

        activeCrop = crop;
        lastAttemptPos = pos == null ? null : pos.toImmutable();
        lastAttemptMillis = nowMillis;
        if (sessionStartMillis == 0L) {
            sessionStartMillis = nowMillis;
        }
        sessionAttempts++;
        for (GardenDropDefinition definition : visibleDrops()) {
            counter(definition.id()).recordAttempt();
        }
    }

    private void recordDrop(GardenDropDefinition definition) {
        counter(definition.id()).recordDrop();
    }

    private GardenDropState stateFor(GardenDropDefinition definition) {
        DropCounter counter = counters.getOrDefault(definition.id(), DropCounter.EMPTY);
        double chance = GardenRngSettings.INSTANCE.chance(definition);
        return new GardenDropState(
                definition,
                counter.sessionAttempts,
                counter.attemptsSinceDrop,
                counter.drops,
                chance,
                price(definition),
                counter.lastDropMillis
        );
    }

    private List<GardenDropDefinition> visibleDrops() {
        if (activeCrop == null) {
            return List.of();
        }
        return GardenDropDefinitions.forCrop(activeCrop).stream()
                .filter(GardenRngSettings.INSTANCE::shouldTrack)
                .toList();
    }

    private DropCounter counter(String id) {
        return counters.computeIfAbsent(id, ignored -> new DropCounter());
    }

    private static double price(GardenDropDefinition definition) {
        if (!GardenRngSettings.INSTANCE.showProfit()) {
            return 0.0D;
        }
        ItemPriceEstimate estimate = SkyBlockPriceService.INSTANCE.estimate(new SkyBlockItemKey(definition.productId(), definition.auctionName()));
        if (estimate.bazaarPrice() != null) {
            double instantSell = estimate.bazaarPrice().instantSell();
            double instantBuy = estimate.bazaarPrice().instantBuy();
            return instantSell > 0.0D ? instantSell : instantBuy;
        }
        if (estimate.auctionPrice() != null) {
            return estimate.auctionPrice().lowestBin();
        }
        return 0.0D;
    }

    private static boolean isEnabled() {
        return GardenRngSettings.INSTANCE.enabled() && JafuModules.isEnabled(JafuModules.GARDEN_RNG_CALCULATOR);
    }

    private void resetIfIdle(long nowMillis) {
        if (!GardenRngSettings.INSTANCE.autoResetEnabled() || lastAttemptMillis == 0L) {
            return;
        }
        if (nowMillis - lastAttemptMillis >= GardenRngSettings.INSTANCE.autoResetMillis()) {
            resetAll();
        }
    }

    private static final class DropCounter {
        private static final DropCounter EMPTY = new DropCounter();

        private long sessionAttempts;
        private long attemptsSinceDrop;
        private long drops;
        private long lastDropMillis;

        private void recordAttempt() {
            sessionAttempts++;
            attemptsSinceDrop++;
        }

        private void recordDrop() {
            drops++;
            attemptsSinceDrop = 0L;
            lastDropMillis = System.currentTimeMillis();
        }
    }
}
