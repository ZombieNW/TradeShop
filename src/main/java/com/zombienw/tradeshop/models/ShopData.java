package com.zombienw.tradeshop.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import javax.annotation.Nullable;

// This is a data model record thing for the data we want stored in a shop sign's PDC.
// Java records give us get'er's for each of the data types automagically

public record ShopData(@Nullable ItemStack inputItem, int inputAmount, @Nullable ItemStack outputItem, int outputAmount) {

    public boolean isPending() {
        return inputItem == null || outputItem == null;
    }

    public Material inputMaterial() {
        return inputItem != null ? inputItem.getType() : null;
    }

    public Material outputMaterial() {
        return outputItem != null ? outputItem.getType() : null;
    }
}