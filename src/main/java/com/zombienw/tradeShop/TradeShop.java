package com.zombienw.tradeShop;

import org.bukkit.plugin.java.JavaPlugin;

public final class TradeShop extends JavaPlugin {

    @Override
    public void onEnable() {
        // register events
        getServer().getPluginManager().registerEvents(new Events(), this);

        getLogger().info("ZNW's Tradeshop Loaded!");
    }
}
