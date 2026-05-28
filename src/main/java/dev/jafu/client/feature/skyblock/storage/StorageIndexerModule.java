package dev.jafu.client.feature.skyblock.storage;

import dev.jafu.client.gui.JafuTheme;
import dev.jafu.client.module.JafuCategory;
import dev.jafu.client.module.JafuModule;
import dev.jafu.client.module.JafuModules;

public final class StorageIndexerModule extends JafuModule {
    public StorageIndexerModule() {
        super(
                JafuModules.STORAGE_INDEXER,
                JafuCategory.SKYBLOCK,
                "Storage indexer",
                "Remembers opened backpacks, ender chest, sacks, and storage menus",
                StorageIndexerSettings.INSTANCE.enabled(),
                JafuTheme.WARN
        );
    }

    @Override
    public void toggle() {
        super.toggle();
        StorageIndexerSettings.INSTANCE.setEnabled(enabled());
    }
}
