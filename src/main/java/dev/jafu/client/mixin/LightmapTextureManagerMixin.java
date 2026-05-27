package dev.jafu.client.mixin;

import dev.jafu.client.module.JafuModules;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Inject(method = "getBrightness(Lnet/minecraft/world/dimension/DimensionType;I)F", at = @At("HEAD"), cancellable = true)
    private static void jafu$fullbrightDimension(DimensionType type, int lightLevel, CallbackInfoReturnable<Float> cir) {
        if (JafuModules.isEnabled(JafuModules.FULLBRIGHT)) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "getBrightness(FI)F", at = @At("HEAD"), cancellable = true)
    private static void jafu$fullbrightAmbient(float ambientLight, int lightLevel, CallbackInfoReturnable<Float> cir) {
        if (JafuModules.isEnabled(JafuModules.FULLBRIGHT)) {
            cir.setReturnValue(1.0F);
        }
    }
}
