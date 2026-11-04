package io.github.scriptercarft.steadybreak.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.scriptercarft.steadybreak.ToolIdentity;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The same item refresh also replays the equip animation; skip it for the same tool. */
@Mixin(FirstPersonHandsAndItems.class)
abstract class FirstPersonHandsAndItemsMixin {
    @ModifyReturnValue(method = "shouldInstantlyReplaceVisibleItem", at = @At("RETURN"))
    private boolean steadybreak$noEquipAnimationOnItemUpdate(
            boolean replaceInstantly,
            @Local(argsOnly = true, ordinal = 0) ItemStack visible,
            @Local(argsOnly = true, ordinal = 1) ItemStack held
    ) {
        // A count change is a real change the player should see, as in vanilla.
        return replaceInstantly || (visible.getCount() == held.getCount() && ToolIdentity.isSameTool(visible, held));
    }
}
