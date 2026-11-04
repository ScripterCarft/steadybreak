package io.github.scriptercarft.steadybreak;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Predicate;

/**
 * Decides whether two stacks that are not component-identical are still the same physical tool, so
 * a server-side refresh of the held item does not count as switching tools.
 *
 * <p>Only called once vanilla's comparison has already failed, so the common path costs nothing.
 */
public final class ToolIdentity {
    /** Components that never affect mining speed. Mending changes DAMAGE. */
    private static final Predicate<DataComponentType<?>> COSMETIC_OR_WEAR =
            type -> type == DataComponents.DAMAGE || type == DataComponents.LORE;

    private ToolIdentity() {
    }

    public static boolean isSameTool(ItemStack a, ItemStack b) {
        if (!ItemStack.isSameItem(a, b)) {
            return false;
        }
        // Hypixel SkyBlock stores a per-item "uuid" in custom_data. When both stacks have one it is
        // authoritative: fuel and counter updates keep it, and a second copy of a tool never shares it.
        String uuidA = skyblockUuid(a);
        String uuidB = skyblockUuid(b);
        if (uuidA != null && uuidB != null) {
            return uuidA.equals(uuidB);
        }
        return ItemStack.matchesIgnoringComponents(a, b, COSMETIC_OR_WEAR);
    }

    private static String skyblockUuid(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        return data.copyTag().getString("uuid").filter(uuid -> !uuid.isEmpty()).orElse(null);
    }
}
