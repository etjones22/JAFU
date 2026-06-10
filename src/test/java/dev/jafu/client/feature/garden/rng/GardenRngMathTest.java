package dev.jafu.client.feature.garden.rng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GardenRngMathTest {
    @Test
    void chanceAtLeastOneUsesBinomialComplement() {
        double chance = GardenRngMath.chanceAtLeastOne(10L, 0.1D);
        assertEquals(1.0D - Math.pow(0.9D, 10.0D), chance, 0.0000001D);
    }

    @Test
    void dryChanceIsComplement() {
        double chance = GardenRngMath.dryChance(100L, 0.01D);
        assertEquals(Math.pow(0.99D, 100.0D), chance, 0.0000001D);
    }

    @Test
    void expectedDropsIsAttemptsTimesChance() {
        assertEquals(2.5D, GardenRngMath.expectedDrops(5_000L, 0.0005D), 0.0000001D);
    }

    @Test
    void parserMatchesKnownDropNames() {
        assertTrue(GardenDropDefinitions.matchMessage("RARE DROP! Cropie").isPresent());
        assertEquals("designer_coffee_beans", GardenDropDefinitions.matchMessage("You found Designer Coffee Beans!").orElseThrow().id());
    }
}
