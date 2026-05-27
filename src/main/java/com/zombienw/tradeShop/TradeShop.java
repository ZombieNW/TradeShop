package com.zombienw.tradeShop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;

public final class TradeShop extends JavaPlugin {
    public record ShopSign(@Nullable ItemStack inputItem, int inputAmount, @Nullable ItemStack outputItem, int outputAmount) {
        public boolean isPending() {
            return inputItem == null || outputItem == null;
        }

        // material helpers
        public org.bukkit.Material inputMaterial()  { return inputItem  != null ? inputItem.getType()  : null; }
        public org.bukkit.Material outputMaterial() { return outputItem != null ? outputItem.getType() : null; }
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new Events(), this);
        getLogger().info("ZNW's Tradeshop Loaded!");
    }
}
