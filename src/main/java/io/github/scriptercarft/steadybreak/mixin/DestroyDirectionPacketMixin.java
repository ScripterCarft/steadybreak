package io.github.scriptercarft.steadybreak.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import io.github.scriptercarft.steadybreak.BreakPackets;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Minecraft 26.3 tells the server whenever the crosshair moves to another face of the block being
 * broken. On Hypixel the break stops while this packet is sent, and the stall disappears without it.
 * The new action took the wire position of ABORT_DESTROY_BLOCK, which is the likely cause, but the
 * server side cannot be inspected. STOP_DESTROY_BLOCK still carries the final direction.
 */
@Mixin(MultiPlayerGameMode.class)
abstract class DestroyDirectionPacketMixin {
    @WrapWithCondition(
            method = "continueDestroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private boolean steadybreak$keepDirectionChangeLocal(ClientPacketListener connection, Packet<?> packet) {
        return !BreakPackets.isDirectionChange(packet);
    }
}
