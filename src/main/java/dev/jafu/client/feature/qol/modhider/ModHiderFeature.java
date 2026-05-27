package dev.jafu.client.feature.qol.modhider;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class ModHiderFeature {
    private static final Identifier FIRMAMENT_MOD_LIST = Identifier.of("firmament", "mod_list");

    private ModHiderFeature() {
    }

    public static boolean shouldBlock(CustomPayload payload) {
        if (!ModHiderSettings.INSTANCE.enabled() || payload == null || payload.getId() == null) {
            return false;
        }
        return FIRMAMENT_MOD_LIST.equals(payload.getId().id());
    }
}
