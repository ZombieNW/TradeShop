package com.zombienw.tradeShop;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;

public final class TradeShop extends JavaPlugin {
    public record ShopSign(@Nullable Material inputMaterial, int inputAmount, @Nullable Material outputMaterial, int outputAmount) {
        public boolean isPending() {
            return inputMaterial == null || outputMaterial == null;
        }
    }

    @Override
    public void onEnable() {
        // register events
        getServer().getPluginManager().registerEvents(new Events(), this);

        getLogger().info("ZNW's Tradeshop Loaded!");
    }
}
