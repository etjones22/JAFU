package dev.jafu.client.feature.qol.tokenguard;

import java.lang.StackWalker.Option;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class TokenGuardFeature {
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);
    private static final Set<String> WARNED_CALLERS = ConcurrentHashMap.newKeySet();

    private TokenGuardFeature() {
    }

    public static boolean shouldBlockTokenAccess(String tokenKind) {
        if (!TokenGuardSettings.INSTANCE.enabled()) {
            return false;
        }

        String caller = firstExternalCaller();
        if (caller == null || trustedCaller(caller)) {
            return false;
        }

        warnBlocked(caller, tokenKind);
        return true;
    }

    private static String firstExternalCaller() {
        return STACK_WALKER.walk(frames -> frames
                .map(frame -> frame.getDeclaringClass().getName())
                .filter(name -> !internalFrame(name))
                .findFirst()
                .orElse(null)
        );
    }

    private static boolean internalFrame(String className) {
        return className.equals("net.minecraft.client.session.Session")
                || className.equals("dev.jafu.client.mixin.SessionMixin")
                || className.startsWith("dev.jafu.client.feature.qol.tokenguard.")
                || className.startsWith("java.")
                || className.startsWith("jdk.")
                || className.startsWith("sun.")
                || className.startsWith("org.spongepowered.asm.mixin.");
    }

    private static boolean trustedCaller(String className) {
        return className.startsWith("net.minecraft.")
                || className.startsWith("com.mojang.")
                || className.startsWith("net.fabricmc.")
                || className.startsWith("org.spongepowered.")
                || className.startsWith("dev.jafu.");
    }

    private static void warnBlocked(String caller, String tokenKind) {
        if (!WARNED_CALLERS.add(caller + ":" + tokenKind)) {
            return;
        }

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("[JAFU] TokenGuard blocked " + tokenKind + " access from " + caller + "."), false);
                    }
                });
            }
        } catch (RuntimeException ignored) {
            // Token access can happen during early startup. Never crash while trying to warn.
        }
    }
}
