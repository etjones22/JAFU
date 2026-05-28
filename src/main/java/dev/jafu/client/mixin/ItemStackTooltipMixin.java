package dev.jafu.client.mixin;

import java.util.ArrayList;
import java.util.List;

import dev.jafu.client.feature.qol.itemvalue.ItemValueOverlayFeature;
import dev.jafu.client.feature.skyblock.storage.StorageIndexerFeature;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {
    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void jafu$appendStorageIndex(Item.TooltipContext context, PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
        List<Text> tooltip = new ArrayList<>(cir.getReturnValue());
        ItemValueOverlayFeature.appendTooltip((ItemStack) (Object) this, tooltip);
        StorageIndexerFeature.appendTooltip((ItemStack) (Object) this, tooltip);
        cir.setReturnValue(tooltip);
    }
}
