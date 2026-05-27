package dev.jafu.client.feature.general.itemview;

import dev.jafu.client.module.JafuModules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;

public final class ItemViewModelTransform {
    private static float rightSwingProgress;
    private static float leftSwingProgress;
    private static float rightEquipProgress;
    private static float leftEquipProgress;

    private ItemViewModelTransform() {
    }

    public static void apply(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            MatrixStack matrices
    ) {
        if (!enabledFor(entity, stack, displayContext)) {
            return;
        }

        ItemViewSettings settings = ItemViewSettings.INSTANCE;
        float scale = (float) (settings.value(ItemViewSetting.SIZE) / ItemViewSetting.SIZE.defaultValue());
        float x = (float) settings.value(ItemViewSetting.OFFSET_X) / 100.0F;
        float y = (float) -settings.value(ItemViewSetting.OFFSET_Y) / 100.0F;
        float z = (float) settings.value(ItemViewSetting.OFFSET_Z) / 100.0F;

        matrices.translate(x, y, z);
        matrices.scale(scale, scale, scale);
    }

    public static float smoothSwing(Arm arm, float targetProgress) {
        if (!JafuModules.isEnabled(JafuModules.ITEM_VIEW)) {
            return targetProgress;
        }
        if (arm == Arm.RIGHT) {
            rightSwingProgress = smooth(rightSwingProgress, targetProgress);
            return rightSwingProgress;
        }

        leftSwingProgress = smooth(leftSwingProgress, targetProgress);
        return leftSwingProgress;
    }

    public static float smoothEquip(Arm arm, float targetProgress) {
        if (!JafuModules.isEnabled(JafuModules.ITEM_VIEW)) {
            return targetProgress;
        }
        if (arm == Arm.RIGHT) {
            rightEquipProgress = smooth(rightEquipProgress, targetProgress);
            return rightEquipProgress;
        }

        leftEquipProgress = smooth(leftEquipProgress, targetProgress);
        return leftEquipProgress;
    }

    private static boolean enabledFor(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext) {
        if (!JafuModules.isEnabled(JafuModules.ITEM_VIEW) || stack.isEmpty() || !displayContext.isFirstPerson()) {
            return false;
        }

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && entity == player;
    }

    private static float smooth(float currentProgress, float targetProgress) {
        float speed = (float) ItemViewSettings.INSTANCE.value(ItemViewSetting.SPEED);
        if (speed >= 0.995F) {
            return targetProgress;
        }

        float factor = MathHelper.clamp(speed, 0.0F, 1.0F);
        return currentProgress + (targetProgress - currentProgress) * factor;
    }
}
