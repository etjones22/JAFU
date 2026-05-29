package dev.jafu.client.mixin;

import dev.jafu.client.feature.qol.tokenguard.TokenGuardFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.client.session.Session.class)
public abstract class SessionMixin {
    @Inject(method = "getAccessToken", at = @At("HEAD"), cancellable = true)
    private void jafu$guardAccessToken(CallbackInfoReturnable<String> cir) {
        if (TokenGuardFeature.shouldBlockTokenAccess("access token")) {
            cir.setReturnValue("");
        }
    }

    @Inject(method = "getSessionId", at = @At("HEAD"), cancellable = true)
    private void jafu$guardSessionId(CallbackInfoReturnable<String> cir) {
        if (TokenGuardFeature.shouldBlockTokenAccess("session id")) {
            cir.setReturnValue("token:blocked");
        }
    }
}
