package com.zombienw.tradeShop;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public final class TradeShop extends JavaPlugin {
    public record ShopSign(Material inputMaterial, int inputAmount, Material outputMaterial, int outputAmount) {}

    @Override
    public void onEnable() {
        // register events
        getServer().getPluginManager().registerEvents(new Events(), this);

        getLogger().info("ZNW's Tradeshop Loaded!");
    }
}
