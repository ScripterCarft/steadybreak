package io.github.scriptercarft.steadybreak;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;
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
        Optional<String> uuidA = skyblockUuid(a);
        Optional<String> uuidB = skyblockUuid(b);
        if (uuidA.isPresent() && uuidB.isPresent()) {
            return uuidA.equals(uuidB);
        }
        return ItemStack.matchesIgnoringComponents(a, b, COSMETIC_OR_WEAR);
    }

    private static Optional<String> skyblockUuid(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getString("uuid")
                .filter(Predicate.not(String::isEmpty));
    }
}
