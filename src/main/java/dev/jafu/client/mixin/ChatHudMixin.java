package dev.jafu.client.mixin;

import dev.jafu.client.feature.general.chat.ChatEnhancementsSettings;
import dev.jafu.client.feature.general.globalsettings.GlobalSettings;
import dev.jafu.client.gui.CleanFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Unique
    private static final long JAFU_ANIMATION_MILLIS = 220L;

    @Shadow
    @Final
    private MinecraftClient client;

    @Unique
    private long jafu$lastMessageMillis;

    @Unique
    private boolean jafu$pushedMatrix;

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"))
    private void jafu$markMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        if (ChatEnhancementsSettings.INSTANCE.smoothChatEnabled()) {
            jafu$lastMessageMillis = System.currentTimeMillis();
        }
    }

    @Inject(method = "getChatScale", at = @At("HEAD"), cancellable = true)
    private void jafu$scaleChat(CallbackInfoReturnable<Double> cir) {
        double scale = ChatEnhancementsSettings.INSTANCE.chatScale();
        if (Math.abs(scale - 1.0D) <= 0.001D) {
            return;
        }
        cir.setReturnValue(client.options.getChatScale().getValue() * scale);
    }

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), argsOnly = true)
    private Text jafu$cleanSimpleMessage(Text message) {
        return jafu$cleanMessage(message);
    }

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), argsOnly = true)
    private Text jafu$cleanSignedMessage(Text message) {
        return jafu$cleanMessage(message);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("HEAD"))
    private void jafu$beginSmoothRender(DrawContext context, TextRenderer textRenderer, int ticks, int mouseX, int mouseY, boolean focused, boolean refresh, CallbackInfo ci) {
        jafu$pushedMatrix = false;
        if (!ChatEnhancementsSettings.INSTANCE.smoothChatEnabled()) {
            return;
        }

        long age = System.currentTimeMillis() - jafu$lastMessageMillis;
        if (age < 0L || age >= JAFU_ANIMATION_MILLIS) {
            return;
        }

        float progress = age / (float) JAFU_ANIMATION_MILLIS;
        float eased = 1.0F - (float) Math.pow(1.0F - progress, 3.0F);
        float offset = (1.0F - eased) * 9.0F;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0.0F, offset);
        jafu$pushedMatrix = true;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("RETURN"))
    private void jafu$endSmoothRender(DrawContext context, TextRenderer textRenderer, int ticks, int mouseX, int mouseY, boolean focused, boolean refresh, CallbackInfo ci) {
        if (jafu$pushedMatrix) {
            context.getMatrices().popMatrix();
            jafu$pushedMatrix = false;
        }
    }

    @Unique
    private Text jafu$cleanMessage(Text message) {
        if (!GlobalSettings.INSTANCE.customFontEnabled()) {
            return message;
        }
        return CleanFont.apply(message);
    }
}
