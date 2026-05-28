package dev.jafu.client.mixin;

import dev.jafu.client.feature.skyblock.storage.StorageIndexerFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenStorageIndexerMixin {
    @Shadow @Final protected ScreenHandler handler;

    @Inject(method = "handledScreenTick", at = @At("TAIL"))
    private void jafu$captureStorageScreenOnTick(CallbackInfo ci) {
        jafu$captureStorageScreen();
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void jafu$captureStorageScreenOnClose(CallbackInfo ci) {
        jafu$captureStorageScreen();
    }

    @Inject(
            method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD")
    )
    private void jafu$rememberClickedStorage(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (slot != null) {
            StorageIndexerFeature.rememberClickedStorage(slot.getStack());
        }
    }

    private void jafu$captureStorageScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        String title = ((Screen) (Object) this).getTitle().getString();
        StorageIndexerFeature.captureScreen(title, handler, client.player.getInventory());
    }
}
