package dev.smartsurvival.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ItemsView {
    private ItemsView() {}
    public static String id(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }
    public static ItemStack stack(String id) {
        Identifier key = Identifier.tryParse(id);
        return key == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.getValue(key).getDefaultInstance();
    }
    public static boolean exists(String id) {
        Identifier key = Identifier.tryParse(id);
        return key != null && BuiltInRegistries.ITEM.containsKey(key) && !stack(id).isEmpty();
    }
    public static String name(String id) {
        ItemStack stack = stack(id);
        return stack.isEmpty() ? id : stack.getHoverName().getString();
    }
    public static String id(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }
}
