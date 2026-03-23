package com.zombienw.tradeShop;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Objects;

public class InventoryUtils {
    private InventoryUtils() {}

    public static boolean hasItems(Inventory inv, Material type, int amount) {
        return inv.containsAtLeast(new ItemStack(type), amount);
    }

    public static void removeItems(Inventory inv, Material type, int amount) {
        inv.removeItem(new ItemStack(type, amount));
    }

    public static void addItems(Inventory inv, ItemStack item) {
        Map<Integer, ItemStack> leftovers = inv.addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            Objects.requireNonNull(inv.getLocation()).getWorld().dropItemNaturally(inv.getLocation(), leftover);
        }
    }
}
