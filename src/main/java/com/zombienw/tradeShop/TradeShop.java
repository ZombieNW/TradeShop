package com.zombienw.tradeShop;

import org.bukkit.plugin.java.JavaPlugin;

public final class TradeShop extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("One second");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
