package dev.jafu.client.feature.gui.scrollabletooltips;

import dev.jafu.client.gui.JafuTheme;
import dev.jafu.client.module.JafuCategory;
import dev.jafu.client.module.JafuModule;
import dev.jafu.client.module.JafuModules;

public final class ScrollableTooltipsModule extends JafuModule {
    public ScrollableTooltipsModule() {
        super(
                JafuModules.SCROLLABLE_TOOLTIPS,
                JafuCategory.GUI,
                "Scrollable tooltips",
                "Scrollable long item tooltips with polished controls",
                ScrollableTooltipsSettings.INSTANCE.enabled(),
                JafuTheme.WARN
        );
    }

    @Override
    public void toggle() {
        super.toggle();
        ScrollableTooltipsSettings.INSTANCE.setEnabled(enabled());
    }
}
