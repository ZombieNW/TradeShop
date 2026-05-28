package com.zombienw.tradeshop.listeners;

import com.zombienw.tradeshop.Constants;
import com.zombienw.tradeshop.managers.ShopManager;
import com.zombienw.tradeshop.models.ShopData;
import com.zombienw.tradeshop.utils.FormatUtils;
import com.zombienw.tradeshop.utils.InventoryUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class ShopTradeListener implements Listener {

    private final ShopManager shopManager;

    public ShopTradeListener(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @EventHandler
    // when a player interacts with a shop sign
    public void onTradeInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Sign sign)) return;

        // make sure it's a registered shop and not a pending one
        if (!shopManager.isRegisteredShop(sign)) return;

        String line2 = FormatUtils.serialize(sign.getSide(Side.FRONT).line(1));
        String line4 = FormatUtils.serialize(sign.getSide(Side.FRONT).line(3));
        if (line2.endsWith(Constants.HAND_PLACEHOLDER) || line4.endsWith(Constants.HAND_PLACEHOLDER)) return;

        handleTrade(event.getPlayer(), sign);
    }

    // swap items
    private void handleTrade(@NotNull Player player, Sign sign) {
        ShopData shop = shopManager.loadShopData(sign);
        if (shop == null || shop.isPending()) return;

        WallSign wallData = (WallSign) sign.getBlockData();
        Block attached = sign.getBlock().getRelative(wallData.getFacing().getOppositeFace());

        if (!(attached.getState() instanceof Container container)) return;

        Inventory shopInventory = container.getInventory();
        Inventory playerInventory = player.getInventory();

        // make sure player has currency
        if (!InventoryUtils.hasItems(playerInventory, shop.inputItem(), shop.inputAmount())) {
            player.sendMessage(Component.text("You don't have enough " + FormatUtils.formatName(shop.inputMaterial()) + "!", NamedTextColor.RED));
            return;
        }

        // make sure shop has stock
        if (!InventoryUtils.hasItems(shopInventory, shop.outputItem(), shop.outputAmount())) {
            player.sendMessage(Component.text("This shop is out of stock!", NamedTextColor.RED));
            return;
        }

        InventoryUtils.removeItems(playerInventory, shop.inputItem(), shop.inputAmount());
        InventoryUtils.addItems(shopInventory, shop.inputItem().asQuantity(shop.inputAmount()));

        InventoryUtils.removeItems(shopInventory, shop.outputItem(), shop.outputAmount());
        InventoryUtils.addItems(playerInventory, shop.outputItem().asQuantity(shop.outputAmount()));

        player.sendMessage(Component.text("Trade successful!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }
}
