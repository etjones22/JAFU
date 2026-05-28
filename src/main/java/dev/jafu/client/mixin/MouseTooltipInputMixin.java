package dev.jafu.client.mixin;

import dev.jafu.client.feature.gui.scrollabletooltips.ScrollableTooltipRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseTooltipInputMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow
    public abstract double getScaledX(Window window);

    @Shadow
    public abstract double getScaledY(Window window);

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void jafu$scrollTooltip(long windowHandle, double horizontalAmount, double verticalAmount, CallbackInfo ci) {
        if (!jafu$shouldHandleTooltipInput(windowHandle)) {
            return;
        }

        Window window = client.getWindow();
        if (ScrollableTooltipRenderer.mouseScrolled(getScaledX(window), getScaledY(window), verticalAmount)) {
            ci.cancel();
        }
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void jafu$clickTooltipScrollbar(long windowHandle, MouseInput input, int action, CallbackInfo ci) {
        if (!jafu$shouldHandleTooltipInput(windowHandle)) {
            return;
        }

        Window window = client.getWindow();
        double mouseX = getScaledX(window);
        double mouseY = getScaledY(window);
        if (action == 1 && ScrollableTooltipRenderer.mouseClicked(mouseX, mouseY, input.button())) {
            ci.cancel();
            return;
        }

        if (action == 0 && ScrollableTooltipRenderer.mouseReleased()) {
            ci.cancel();
        }
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    private void jafu$dragTooltipScrollbar(long windowHandle, double x, double y, CallbackInfo ci) {
        if (!jafu$shouldHandleTooltipInput(windowHandle)) {
            return;
        }

        if (ScrollableTooltipRenderer.mouseDragged(Mouse.scaleY(client.getWindow(), y))) {
            ci.cancel();
        }
    }

    private boolean jafu$shouldHandleTooltipInput(long windowHandle) {
        Window window = client.getWindow();
        return window != null && window.getHandle() == windowHandle && client.currentScreen != null;
    }
}
