package com.zombienw.tradeshop;

import com.zombienw.tradeshop.listeners.ShopProtectionListener;
import com.zombienw.tradeshop.listeners.ShopSetupListener;
import com.zombienw.tradeshop.listeners.ShopTradeListener;
import com.zombienw.tradeshop.managers.ShopManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class TradeShop extends JavaPlugin {

    @Override
    public void onEnable() {
        ShopManager shopManager = new ShopManager(this);

        // Register events
        getServer().getPluginManager().registerEvents(new ShopSetupListener(shopManager, this), this);
        getServer().getPluginManager().registerEvents(new ShopTradeListener(shopManager), this);
        getServer().getPluginManager().registerEvents(new ShopProtectionListener(shopManager), this);

        getLogger().info("ZNW's Tradeshop Loaded & Ready :3");
    }
}
