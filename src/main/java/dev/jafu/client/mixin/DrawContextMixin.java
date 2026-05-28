package dev.jafu.client.mixin;

import java.util.List;

import dev.jafu.client.feature.gui.scrollabletooltips.ScrollableTooltipRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    @Inject(method = "drawItemTooltip", at = @At("HEAD"))
    private void jafu$captureItemTooltip(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo ci) {
        ScrollableTooltipRenderer.rememberItem(stack);
    }

    @Inject(
            method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;Lnet/minecraft/util/Identifier;Z)V",
            at = @At("HEAD")
    )
    private void jafu$prepareTooltip(
            TextRenderer textRenderer,
            List<TooltipComponent> components,
            int x,
            int y,
            TooltipPositioner positioner,
            Identifier texture,
            boolean immediate,
            CallbackInfo ci
    ) {
        ScrollableTooltipRenderer.prepareTooltip(components, textRenderer);
    }

    @Inject(
            method = "drawTooltipImmediately",
            at = @At("HEAD"),
            cancellable = true
    )
    private void jafu$renderScrollableTooltip(
            TextRenderer textRenderer,
            List<TooltipComponent> components,
            int x,
            int y,
            TooltipPositioner positioner,
            Identifier texture,
            CallbackInfo ci
    ) {
        if (ScrollableTooltipRenderer.render((DrawContext) (Object) this, textRenderer, components, x, y, positioner, texture)) {
            ci.cancel();
        }
    }
}
