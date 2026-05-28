package dev.jafu.client.feature.qol.itemvalue;

import dev.jafu.client.gui.JafuTheme;
import dev.jafu.client.module.JafuCategory;
import dev.jafu.client.module.JafuModule;
import dev.jafu.client.module.JafuModules;

public final class ItemValueOverlayModule extends JafuModule {
    public ItemValueOverlayModule() {
        super(
                JafuModules.ITEM_VALUE_OVERLAY,
                JafuCategory.QOL,
                "Item value overlay",
                "Shows Bazaar and auction estimates in item tooltips",
                ItemValueOverlaySettings.INSTANCE.enabled(),
                JafuTheme.WARN
        );
    }

    @Override
    public void toggle() {
        super.toggle();
        ItemValueOverlaySettings.INSTANCE.setEnabled(enabled());
    }
}
