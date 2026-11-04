package io.github.scriptercarft.steadybreak.mixin;

import io.github.scriptercarft.steadybreak.ToolIdentity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla aborts the current break (client progress and, through ABORT_DESTROY_BLOCK, server
 * progress) as soon as any component of the held item changes. Keep the break going when the item
 * is still the same tool.
 */
@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private ItemStack destroyingItem;
    @Shadow private boolean isDestroying;

    @ModifyExpressionValue(
            method = "sameDestroyTarget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"
            )
    )
    private boolean steadybreak$keepProgressOnItemUpdate(boolean sameComponents) {
        // Without an active break there is no progress to keep. continueDestroyBlock also gets here
        // after a finished break, and it needs the false result to send a new START.
        if (sameComponents || !isDestroying) {
            return sameComponents;
        }
        ItemStack held = minecraft.player.getMainHandItem();
        if (!ToolIdentity.isSameTool(held, destroyingItem)) {
            return false;
        }
        // Later ticks take vanilla's fast path again instead of re-reading custom data.
        destroyingItem = held;
        return true;
    }
}
