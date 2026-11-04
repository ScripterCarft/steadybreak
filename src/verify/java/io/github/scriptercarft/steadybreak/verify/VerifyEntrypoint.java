package io.github.scriptercarft.steadybreak.verify;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerEquipment;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Runs inside the real Fabric loader, before the game window would open, and calls the actual
 * patched vanilla methods. Exits the JVM with 0 when every check passes.
 */
public final class VerifyEntrypoint implements PreLaunchEntrypoint {
    private static final BlockPos POS = new BlockPos(1, 2, 3);
    private static final BlockPos OTHER_POS = new BlockPos(9, 9, 9);

    private int failures;
    private Unsafe unsafe;
    private MultiPlayerGameMode gameMode;
    private Inventory inventory;
    private FirstPersonHandsAndItems hands;
    private LocalPlayer player;

    @Override
    public void onPreLaunch() {
        int exit = 1;
        try {
            setUp();
            miningChecks();
            equipAnimationChecks();
            exit = failures == 0 ? 0 : 1;
            System.out.println("[verify] " + (failures == 0 ? "ALL PASSED" : failures + " FAILED"));
        } catch (Throwable t) {
            t.printStackTrace(System.out);
        }
        System.out.flush();
        Runtime.getRuntime().halt(exit);
    }

    private void setUp() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // The game binds default components while loading datapacks, which never happens here.
        bindDefaultComponents(Items.DIAMOND_PICKAXE, 1, 1561);
        bindDefaultComponents(Items.IRON_PICKAXE, 1, 250);
        bindDefaultComponents(Items.STONE, 64, 0);

        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (Unsafe) unsafeField.get(null);

        Minecraft minecraft = allocate(Minecraft.class);
        player = allocate(LocalPlayer.class);
        minecraft.player = player;
        EntityEquipment equipment = new PlayerEquipment(player);
        inventory = new Inventory(player, equipment);
        setField(LivingEntity.class, player, "equipment", equipment);
        setField(Player.class, player, "inventory", inventory);
        setField(LocalPlayer.class, player, "minecraft", minecraft);
        setField(Minecraft.class, minecraft, "itemModelResolver", allocate(ItemModelResolver.class));

        gameMode = allocate(MultiPlayerGameMode.class);
        setField(MultiPlayerGameMode.class, gameMode, "minecraft", minecraft);
        hands = allocate(FirstPersonHandsAndItems.class);
    }

    private void miningChecks() throws Exception {
        ItemStack start = tool("uuid-a", 10);
        ItemStack refreshed = tool("uuid-a", 500);
        refreshed.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Fuel: 500"))));
        refreshed.setDamageValue(3);

        check("identical stack keeps progress", mining(start, start.copy(), POS), true);
        check("same uuid, fuel/lore/damage changed keeps progress", mining(start, refreshed, POS), true);
        check("accepted update refreshes destroyingItem", getField(MultiPlayerGameMode.class, gameMode, "destroyingItem") == refreshed, true);
        check("item update after a finished break still starts a new break", mining(start, tool("uuid-a", 500), POS, false), false);
        check("different uuid resets", mining(start, tool("uuid-b", 10), POS), false);
        check("uuid on only one side resets", mining(start, new ItemStack(Items.DIAMOND_PICKAXE), POS), false);
        check("other block position resets", mining(start, refreshed, OTHER_POS), false);

        ItemStack plain = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack mended = plain.copy();
        mended.setDamageValue(7);
        check("mending damage change keeps progress", mining(plain, mended, POS), true);

        ItemStack withLore = plain.copy();
        withLore.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("x"))));
        check("lore change keeps progress", mining(plain, withLore, POS), true);

        ItemStack renamed = plain.copy();
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("other"));
        check("other component change resets (no uuid)", mining(plain, renamed, POS), false);

        ItemStack otherData = plain.copy();
        otherData.set(DataComponents.CUSTOM_DATA, CustomData.of(tag("counter", "1")));
        check("custom data change resets (no uuid)", mining(plain, otherData, POS), false);

        check("different item resets", mining(plain, new ItemStack(Items.IRON_PICKAXE), POS), false);
    }

    private void equipAnimationChecks() throws Exception {
        check("same uuid skips equip animation", instantReplace(tool("uuid-a", 10), tool("uuid-a", 500)), true);

        ItemStack plain = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack renamed = plain.copy();
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("other"));
        check("real change keeps equip animation", instantReplace(plain, renamed), false);
        check("different item keeps equip animation", instantReplace(plain, new ItemStack(Items.IRON_PICKAXE)), false);

        ItemStack blocks = new ItemStack(Items.STONE, 2);
        check("count change keeps equip animation", instantReplace(blocks, new ItemStack(Items.STONE, 1)), false);
    }

    private boolean mining(ItemStack started, ItemStack heldNow, BlockPos target) throws Exception {
        return mining(started, heldNow, target, true);
    }

    private boolean mining(ItemStack started, ItemStack heldNow, BlockPos target, boolean breakActive) throws Exception {
        setField(MultiPlayerGameMode.class, gameMode, "isDestroying", breakActive);
        setField(MultiPlayerGameMode.class, gameMode, "destroyBlockPos", POS);
        setField(MultiPlayerGameMode.class, gameMode, "destroyingItem", started);
        inventory.setItem(inventory.getSelectedSlot(), heldNow);
        Method method = MultiPlayerGameMode.class.getDeclaredMethod("sameDestroyTarget", BlockPos.class);
        method.setAccessible(true);
        return (boolean) method.invoke(gameMode, target);
    }

    private boolean instantReplace(ItemStack visible, ItemStack held) throws Exception {
        Method method = FirstPersonHandsAndItems.class.getDeclaredMethod(
                "shouldInstantlyReplaceVisibleItem", ItemStack.class, ItemStack.class, LocalPlayer.class);
        method.setAccessible(true);
        return (boolean) method.invoke(hands, visible, held, player);
    }

    private static void bindDefaultComponents(Item item, int maxStackSize, int maxDamage) {
        DataComponentMap.Builder components = DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, maxStackSize)
                .set(DataComponents.LORE, ItemLore.EMPTY);
        if (maxDamage > 0) {
            components.set(DataComponents.MAX_DAMAGE, maxDamage).set(DataComponents.DAMAGE, 0);
        }
        item.builtInRegistryHolder().bindComponents(components.build());
    }

    private static ItemStack tool(String uuid, int fuel) {
        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        CompoundTag data = tag("uuid", uuid);
        data.putInt("drill_fuel", fuel);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    private static CompoundTag tag(String key, String value) {
        CompoundTag tag = new CompoundTag();
        tag.putString(key, value);
        return tag;
    }

    private void check(String name, boolean actual, boolean expected) {
        boolean ok = actual == expected;
        if (!ok) {
            failures++;
        }
        System.out.println("[verify] " + (ok ? "PASS " : "FAIL ") + name + " (got " + actual + ")");
    }

    @SuppressWarnings("unchecked")
    private <T> T allocate(Class<T> type) throws InstantiationException {
        return (T) unsafe.allocateInstance(type);
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Class<?> owner, Object target, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
