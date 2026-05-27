package dev.jafu.client.gui;

import dev.jafu.client.feature.general.globalsettings.ClickGuiVersion;
import dev.jafu.client.feature.general.globalsettings.GlobalSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public final class JafuScreens {
    private JafuScreens() {
    }

    public static Screen createSelected() {
        return create(GlobalSettings.INSTANCE.clickGuiVersion());
    }

    public static Screen create(ClickGuiVersion version) {
        return switch (version) {
            case V2 -> new JafuV2Screen();
            case V1 -> new JafuScreen();
        };
    }

    public static void openSelected() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> client.setScreen(createSelected()));
    }

    public static void switchTo(ClickGuiVersion version) {
        GlobalSettings.INSTANCE.setClickGuiVersion(version);
        MinecraftClient.getInstance().setScreen(create(version));
    }
}
