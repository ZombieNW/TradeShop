package com.zombienw.tradeshop.utils;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class InventoryUtils {

    private InventoryUtils() {}

    // See if amount many items exist in inventory the old-fashioned way
    public static boolean hasItems(Inventory inv, ItemStack template, int amount) {
        int found = 0;

        // loop through the itemstacks in the inventory
        for (ItemStack stack : inv.getContents()) {
            if (stack == null) continue;

            if (itemMatches(stack, template)) {
                found += stack.getAmount();
                if (found >= amount) return true; // we got what we came here for
            }
        }
        return false;
    }

    // Remove amount many items from inventory the old-fashioned way
    public static void removeItems(Inventory inv, ItemStack template, int amount) {
        int remaining = amount;
        // virtual inventory copy
        ItemStack[] contents = inv.getContents();

        // loop while there are items in the inventory and items remaining
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];

            if (!itemMatches(stack, template)) continue;

            // remove from quota
            if (stack.getAmount() <= remaining) {
                // remove the whole stack and keep going
                remaining -= stack.getAmount();
                contents[i] = null;
            } else {
                // remove what we want from the stack and stop
                stack.setAmount(stack.getAmount() - remaining);
                remaining = 0;
            }
        }

        // put the inventory back
        inv.setContents(contents);
    }

    // Add items to inventory and drop them if it overflows
    public static void addItems(Inventory inv, ItemStack item) {
        Map<Integer, ItemStack> leftovers = inv.addItem(item.clone());

        // drop the remaining items
        if (inv.getLocation() != null && inv.getLocation().getWorld() != null) {
            for (ItemStack leftover : leftovers.values()) {
                inv.getLocation().getWorld().dropItemNaturally(inv.getLocation(), leftover);
            }
        }
    }

    // Compare two objects based on their material and desired nbt tags
    public static boolean itemMatches(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        return a.isSimilar(b);
    }
}
