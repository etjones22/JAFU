package dev.jafu.client.command;

import dev.jafu.client.feature.general.updater.AutoUpdater;
import dev.jafu.client.feature.general.updater.UpdateChannel;
import dev.jafu.client.gui.JafuScreen;
import dev.jafu.client.gui.HudLayoutScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

public final class JafuCommands {
    private JafuCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("jafu").executes(context -> {
                    openMenu();
                    return 1;
                }).then(ClientCommandManager.literal("layout").executes(context -> {
                    openLayout();
                    return 1;
                })).then(ClientCommandManager.literal("update").executes(context -> {
                    AutoUpdater.checkNow(true);
                    return 1;
                }).then(ClientCommandManager.literal("check").executes(context -> {
                    AutoUpdater.checkNow(true);
                    return 1;
                })).then(ClientCommandManager.literal("stable").executes(context -> {
                    AutoUpdater.setChannel(UpdateChannel.STABLE, true);
                    return 1;
                })).then(ClientCommandManager.literal("dev").executes(context -> {
                    AutoUpdater.setChannel(UpdateChannel.DEV, true);
                    return 1;
                })))));
    }

    private static void openMenu() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> client.setScreen(new JafuScreen()));
    }

    private static void openLayout() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> client.setScreen(new HudLayoutScreen(null)));
    }
}
