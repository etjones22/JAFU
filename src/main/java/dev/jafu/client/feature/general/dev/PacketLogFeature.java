package dev.jafu.client.feature.general.dev;

import dev.jafu.client.feature.general.updater.AutoUpdater;
import net.minecraft.network.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development helper that logs every inbound and outbound network packet to the
 * client log. Toggled at runtime with {@code /jafu dev packetlog}. Disabled by
 * default and never persisted, so it never runs unless a developer turns it on.
 */
public final class PacketLogFeature {
    private static final Logger LOGGER = LoggerFactory.getLogger("jafu/packetlog");
    private static volatile boolean enabled;

    private PacketLogFeature() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean toggle() {
        return setEnabled(!enabled);
    }

    public static boolean setEnabled(boolean value) {
        enabled = value;
        String state = value ? "enabled" : "disabled";
        LOGGER.info("Packet logging {}", state);
        AutoUpdater.notifyPlayer("Packet logging " + state + ".");
        return value;
    }

    public static void logInbound(Packet<?> packet) {
        log("IN ", packet);
    }

    public static void logOutbound(Packet<?> packet) {
        log("OUT", packet);
    }

    private static void log(String direction, Packet<?> packet) {
        if (!enabled || packet == null) {
            return;
        }
        LOGGER.info("[{}] {}", direction, packet.getClass().getName());
    }
}
