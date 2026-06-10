package dev.jafu.client.feature.garden.rng;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;
import net.minecraft.util.math.MathHelper;

public final class GardenRngSettings implements JafuConfigurable {
    public static final GardenRngSettings INSTANCE = new GardenRngSettings();
    public static final double[] RATE_MULTIPLIER_PRESETS = {0.5D, 0.75D, 1.0D, 1.25D, 1.5D, 2.0D, 3.0D, 5.0D};

    private final ConfigFile config = ConfigFile.named("jafu-garden-rng.properties");
    private final Map<GardenRngStatOption, Boolean> visibleStats = new EnumMap<>(GardenRngStatOption.class);
    private final Map<GardenDropFamily, Boolean> enabledFamilies = new EnumMap<>(GardenDropFamily.class);
    private final Map<String, Double> rateMultipliers = new HashMap<>();
    private boolean enabled = true;
    private boolean feastTracking = true;
    private boolean assumeActiveCrop = true;
    private boolean overbloomEnabled;
    private boolean showProfit = true;
    private int armorPieces = 4;

    private GardenRngSettings() {
        for (GardenRngStatOption option : GardenRngStatOption.all()) {
            visibleStats.put(option, true);
        }
        for (GardenDropFamily family : GardenDropFamily.values()) {
            enabledFamilies.put(family, family != GardenDropFamily.OTHER);
        }
        JafuConfigManager.register(this);
        load();
    }

    @Override
    public String configId() {
        return "garden_rng";
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public boolean isVisible(GardenRngStatOption option) {
        if (option == GardenRngStatOption.VALUE && !showProfit) {
            return false;
        }
        return visibleStats.getOrDefault(option, true);
    }

    public void toggleStat(GardenRngStatOption option) {
        visibleStats.put(option, !visibleStats.getOrDefault(option, true));
        save();
    }

    public boolean familyEnabled(GardenDropFamily family) {
        return enabledFamilies.getOrDefault(family, false);
    }

    public void toggleFamily(GardenDropFamily family) {
        enabledFamilies.put(family, !familyEnabled(family));
        save();
    }

    public boolean feastTracking() {
        return feastTracking;
    }

    public void toggleFeastTracking() {
        feastTracking = !feastTracking;
        save();
    }

    public boolean assumeActiveCrop() {
        return assumeActiveCrop;
    }

    public void toggleAssumeActiveCrop() {
        assumeActiveCrop = !assumeActiveCrop;
        save();
    }

    public boolean overbloomEnabled() {
        return overbloomEnabled;
    }

    public void toggleOverbloom() {
        overbloomEnabled = !overbloomEnabled;
        save();
    }

    public boolean showProfit() {
        return showProfit;
    }

    public void toggleProfit() {
        showProfit = !showProfit;
        save();
    }

    public int armorPieces() {
        return armorPieces;
    }

    public void cycleArmorPieces() {
        armorPieces++;
        if (armorPieces > 4) {
            armorPieces = 2;
        }
        save();
    }

    public double rateMultiplier(String dropId) {
        return rateMultipliers.getOrDefault(dropId, 1.0D);
    }

    public void cycleRateMultiplier(String dropId) {
        double current = rateMultiplier(dropId);
        int nextIndex = 0;
        for (int i = 0; i < RATE_MULTIPLIER_PRESETS.length; i++) {
            if (Math.abs(RATE_MULTIPLIER_PRESETS[i] - current) < 0.001D) {
                nextIndex = (i + 1) % RATE_MULTIPLIER_PRESETS.length;
                break;
            }
        }
        rateMultipliers.put(dropId, RATE_MULTIPLIER_PRESETS[nextIndex]);
        save();
    }

    public double chance(GardenDropDefinition definition) {
        double chance = definition.defaultChance();
        if (definition.family() == GardenDropFamily.ARMOR) {
            chance = armorChance(definition.id());
        }
        if (definition.family() == GardenDropFamily.FEAST && overbloomEnabled && !"seasoning".equals(definition.id())) {
            chance *= 1.5D;
        }
        return MathHelper.clamp(chance * rateMultiplier(definition.id()), 0.0D, 1.0D);
    }

    public boolean shouldTrack(GardenDropDefinition definition) {
        if (definition.family() == GardenDropFamily.FEAST && !feastTracking) {
            return false;
        }
        if (definition.family() == GardenDropFamily.FEAST && !assumeActiveCrop) {
            return false;
        }
        return familyEnabled(definition.family());
    }

    @Override
    public boolean load() {
        if (!config.load()) {
            return false;
        }

        enabled = config.booleanValue("enabled", true);
        feastTracking = config.booleanValue("feast_tracking", true);
        assumeActiveCrop = config.booleanValue("assume_active_crop", true);
        overbloomEnabled = config.booleanValue("overbloom", false);
        showProfit = config.booleanValue("show_profit", true);
        armorPieces = MathHelper.clamp(config.intValue("armor_pieces", 4), 2, 4);

        for (GardenRngStatOption option : GardenRngStatOption.all()) {
            visibleStats.put(option, config.booleanValue("stat." + option.id(), true));
        }
        for (GardenDropFamily family : GardenDropFamily.values()) {
            enabledFamilies.put(family, config.booleanValue("family." + family.id(), family != GardenDropFamily.OTHER));
        }
        for (GardenDropDefinition definition : GardenDropDefinitions.all()) {
            double multiplier = config.doubleValue("rate_multiplier." + definition.id(), 1.0D);
            rateMultipliers.put(definition.id(), sanitizeMultiplier(multiplier));
        }
        return true;
    }

    @Override
    public boolean save() {
        config.clear();
        config.setBoolean("enabled", enabled);
        config.setBoolean("feast_tracking", feastTracking);
        config.setBoolean("assume_active_crop", assumeActiveCrop);
        config.setBoolean("overbloom", overbloomEnabled);
        config.setBoolean("show_profit", showProfit);
        config.setInt("armor_pieces", armorPieces);
        for (GardenRngStatOption option : GardenRngStatOption.all()) {
            config.setBoolean("stat." + option.id(), visibleStats.getOrDefault(option, true));
        }
        for (GardenDropFamily family : GardenDropFamily.values()) {
            config.setBoolean("family." + family.id(), familyEnabled(family));
        }
        for (Map.Entry<String, Double> entry : rateMultipliers.entrySet()) {
            config.setDouble("rate_multiplier." + entry.getKey(), sanitizeMultiplier(entry.getValue()));
        }
        return config.save("JAFU garden RNG calculator");
    }

    private static double armorChance(String id) {
        int pieces = INSTANCE.armorPieces();
        if ("cropie".equals(id)) {
            return pieces >= 4 ? 0.0005D : pieces == 3 ? 0.0004D : 0.0003D;
        }
        if ("squash".equals(id)) {
            return pieces >= 4 ? 0.0003D : pieces == 3 ? 0.0002D : 0.0001D;
        }
        if ("fermento".equals(id)) {
            return pieces >= 4 ? 0.00007D : pieces == 3 ? 0.00006D : 0.00005D;
        }
        return pieces >= 4 ? 0.00004D : pieces == 3 ? 0.00003D : 0.00002D;
    }

    private static double sanitizeMultiplier(double multiplier) {
        return MathHelper.clamp(Math.round(multiplier * 100.0D) / 100.0D, 0.01D, 100.0D);
    }
}
