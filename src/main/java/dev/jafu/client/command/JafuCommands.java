package dev.jafu.client.command;

import dev.jafu.client.feature.general.updater.AutoUpdater;
import dev.jafu.client.feature.general.updater.UpdateChannel;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigManager.ConfigOperationResult;
import dev.jafu.client.gui.HudLayoutScreen;
import dev.jafu.client.gui.JafuScreens;
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
                })).then(ClientCommandManager.literal("config").then(ClientCommandManager.literal("save").executes(context -> {
                    return saveConfigs();
                })).then(ClientCommandManager.literal("reload").executes(context -> {
                    return reloadConfigs();
                }))).then(ClientCommandManager.literal("update").executes(context -> {
                    AutoUpdater.checkNow(true);
                    return 1;
                }).then(ClientCommandManager.literal("check").executes(context -> {
                    AutoUpdater.checkNow(true);
                    return 1;
                })).then(ClientCommandManager.literal("stable").executes(context -> {
                    AutoUpdater.setChannel(UpdateChannel.STABLE, true);
                    return 1;
                })).then(ClientCommandManager.literal("snapshot").executes(context -> {
                    AutoUpdater.setChannel(UpdateChannel.SNAPSHOT, true);
                    return 1;
                })).then(ClientCommandManager.literal("dev").executes(context -> {
                    AutoUpdater.setChannel(UpdateChannel.SNAPSHOT, true);
                    return 1;
                })))));
    }

    private static void openMenu() {
        JafuScreens.openSelected();
    }

    private static void openLayout() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> client.setScreen(new HudLayoutScreen(null)));
    }

    private static int saveConfigs() {
        ConfigOperationResult result = JafuConfigManager.saveAll();
        notifyConfigResult("saved", result);
        return result.successful() ? 1 : 0;
    }

    private static int reloadConfigs() {
        ConfigOperationResult result = JafuConfigManager.reloadAll();
        notifyConfigResult("reloaded", result);
        return result.successful() ? 1 : 0;
    }

    private static void notifyConfigResult(String action, ConfigOperationResult result) {
        if (result.successful()) {
            AutoUpdater.notifyPlayer("JAFU configs " + action + " (" + result.total() + ").");
            return;
        }
        AutoUpdater.notifyPlayer("JAFU configs " + action + " with " + result.failed() + " failure(s).");
    }
}
