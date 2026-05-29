package dev.jafu.client.feature.general.itemview;

import dev.jafu.client.module.JafuModules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public final class ItemViewModelTransform {
    private static final long TICK_NANOS = 50_000_000L;
    private static final float VANILLA_SWING_TICKS = 6.0F;

    private static final SwingAnimationState RIGHT_SWING = new SwingAnimationState();
    private static final SwingAnimationState LEFT_SWING = new SwingAnimationState();

    private static float rightSwingProgress;
    private static float leftSwingProgress;
    private static float rightEquipProgress;
    private static float leftEquipProgress;
    private static AbstractClientPlayerEntity currentRenderPlayer;
    private static Hand currentRenderHand;

    private ItemViewModelTransform() {
    }

    public static void beginFirstPersonItem(AbstractClientPlayerEntity player, Hand hand) {
        currentRenderPlayer = player;
        currentRenderHand = hand;
    }

    public static void endFirstPersonItem() {
        currentRenderPlayer = null;
        currentRenderHand = null;
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
            swingState(arm).reset();
            return targetProgress;
        }
        float speed = (float) ItemViewSettings.INSTANCE.value(ItemViewSetting.SPEED);
        if (Math.abs(speed - 1.0F) < 0.005F) {
            swingState(arm).reset();
            return targetProgress;
        }
        if (currentRenderPlayer != null && currentRenderHand != null) {
            return scaledSwing(swingState(arm), currentRenderPlayer, currentRenderHand, targetProgress, speed);
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

    private static float scaledSwing(
            SwingAnimationState state,
            AbstractClientPlayerEntity player,
            Hand hand,
            float targetProgress,
            float speed
    ) {
        long now = System.nanoTime();
        boolean vanillaSwingingThisHand = player.handSwinging && player.preferredHand == hand;
        int vanillaSwingTicks = vanillaSwingingThisHand ? player.handSwingTicks : 0;
        boolean newVanillaSwing = vanillaSwingingThisHand
                && (!state.vanillaSwinging || vanillaSwingTicks < state.lastVanillaSwingTicks);

        if (!vanillaSwingingThisHand) {
            state.waitingForVanillaEnd = false;
        }
        if (newVanillaSwing || (!state.active && vanillaSwingingThisHand && targetProgress > 0.001F && !state.waitingForVanillaEnd)) {
            if (state.active) {
                state.pendingSwing = true;
            } else {
                state.start(now);
            }
        }

        state.vanillaSwinging = vanillaSwingingThisHand;
        state.lastVanillaSwingTicks = vanillaSwingTicks;

        if (state.active) {
            return state.advance(now, MathHelper.clamp(speed, 0.0F, 2.0F));
        }
        if (state.waitingForVanillaEnd && vanillaSwingingThisHand) {
            return 0.0F;
        }
        return targetProgress;
    }

    private static SwingAnimationState swingState(Arm arm) {
        return arm == Arm.RIGHT ? RIGHT_SWING : LEFT_SWING;
    }

    private static final class SwingAnimationState {
        private boolean active;
        private boolean pendingSwing;
        private boolean vanillaSwinging;
        private boolean waitingForVanillaEnd;
        private int lastVanillaSwingTicks;
        private float visualProgress;
        private long lastUpdateNanos;

        private void start(long now) {
            active = true;
            pendingSwing = false;
            waitingForVanillaEnd = false;
            visualProgress = 0.0F;
            lastUpdateNanos = now;
        }

        private float advance(long now, float speed) {
            if (lastUpdateNanos == 0L) {
                lastUpdateNanos = now;
                return visualProgress;
            }

            long elapsedNanos = Math.max(0L, now - lastUpdateNanos);
            lastUpdateNanos = now;
            float elapsedTicks = Math.min(elapsedNanos / (float) TICK_NANOS, 3.0F);
            visualProgress += elapsedTicks * speed / VANILLA_SWING_TICKS;

            while (visualProgress >= 1.0F) {
                if (pendingSwing) {
                    visualProgress -= 1.0F;
                    pendingSwing = false;
                } else {
                    active = false;
                    waitingForVanillaEnd = true;
                    visualProgress = 0.0F;
                    return 0.0F;
                }
            }

            return visualProgress;
        }

        private void reset() {
            active = false;
            pendingSwing = false;
            vanillaSwinging = false;
            waitingForVanillaEnd = false;
            lastVanillaSwingTicks = 0;
            visualProgress = 0.0F;
            lastUpdateNanos = 0L;
        }
    }
}
