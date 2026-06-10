package dev.jafu.client.feature.garden.rng;

import dev.jafu.client.gui.JafuTheme;
import dev.jafu.client.module.JafuCategory;
import dev.jafu.client.module.JafuModule;
import dev.jafu.client.module.JafuModules;

public final class GardenRngModule extends JafuModule {
    public GardenRngModule() {
        super(
                JafuModules.GARDEN_RNG_CALCULATOR,
                JafuCategory.GARDEN,
                "Garden RNG calculator",
                "Tracks farming drop odds, dry streaks, and expected value",
                GardenRngSettings.INSTANCE.enabled(),
                JafuTheme.GOOD
        );
    }

    @Override
    public void toggle() {
        super.toggle();
        GardenRngSettings.INSTANCE.setEnabled(enabled());
    }
}
