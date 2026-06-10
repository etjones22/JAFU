package dev.jafu.client.mixin;

import dev.jafu.client.feature.garden.rng.GardenRngFeature;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void jafu$trackGardenAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        jafu$trackGardenHarvest(pos);
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void jafu$trackGardenBlockBreakingProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        jafu$trackGardenHarvest(pos);
    }

    private static void jafu$trackGardenHarvest(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || pos == null) {
            return;
        }
        BlockState state = client.world.getBlockState(pos);
        GardenRngFeature.recordHarvestAttempt(state, pos);
    }
}
