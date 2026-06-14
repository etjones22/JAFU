package dev.jafu.client.mixin;

import dev.jafu.client.feature.general.dev.PacketLogFeature;
import dev.jafu.client.feature.qol.modhider.ModHiderFeature;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void jafu$blockModAnnouncer(Packet<?> packet, ChannelFutureListener callback, boolean flush, CallbackInfo ci) {
        if (packet instanceof CustomPayloadC2SPacket customPayloadPacket
                && ModHiderFeature.shouldBlock(customPayloadPacket.payload())) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"))
    private void jafu$logOutbound(Packet<?> packet, ChannelFutureListener callback, boolean flush, CallbackInfo ci) {
        PacketLogFeature.logOutbound(packet);
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"))
    private void jafu$logInbound(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        PacketLogFeature.logInbound(packet);
    }
}
