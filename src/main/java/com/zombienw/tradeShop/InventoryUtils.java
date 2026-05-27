package com.zombienw.tradeShop;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Objects;

public class InventoryUtils {
    private InventoryUtils() {}

    // Checks if amount many items exist in given inventoryReturns true if the inventory contains at least amount many of the given item
    public static boolean hasItems(Inventory inv, ItemStack template, int amount) {
        int found = 0;
        for (ItemStack stack : inv.getContents()) {
            if (stack == null) continue;
            if (itemMatches(stack, template)) {
                found += stack.getAmount();
                if (found >= amount) return true;
            }
        }
        return false;
    }

    // removes amount many items from given inventory based on template
    public static void removeItems(Inventory inv, ItemStack template, int amount) {
        int remaining = amount;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || !itemMatches(stack, template)) continue;

            if (stack.getAmount() <= remaining) {
                remaining -= stack.getAmount();
                contents[i] = null;
            } else {
                stack.setAmount(stack.getAmount() - remaining);
                remaining = 0;
            }
        }
        inv.setContents(contents);
    }

    // give items to player/chest
    public static void addItems(Inventory inv, ItemStack item) {
        Map<Integer, ItemStack> leftovers = inv.addItem(item.clone());
        // drop item on ground if player's inventory is full
        for (ItemStack leftover : leftovers.values()) {
            Objects.requireNonNull(inv.getLocation()).getWorld().dropItemNaturally(inv.getLocation(), leftover);
        }
    }

    // checks if two items matched based on material and nbt
    public static boolean itemMatches(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        // isSimilar checks type + meta/components, ignores amount
        return a.isSimilar(b);
    }
}
