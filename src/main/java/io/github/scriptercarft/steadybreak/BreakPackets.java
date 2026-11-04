package io.github.scriptercarft.steadybreak;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

/** Packet checks that stay outside the mixins so the verify run can call them directly. */
public final class BreakPackets {
    private BreakPackets() {
    }

    public static boolean isDirectionChange(Packet<?> packet) {
        return packet instanceof ServerboundPlayerActionPacket action
                && action.getAction() == ServerboundPlayerActionPacket.Action.CHANGE_DESTROY_DIRECTION;
    }
}
