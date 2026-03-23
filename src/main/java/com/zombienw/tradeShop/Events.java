package com.zombienw.tradeShop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.zombienw.tradeShop.TradeShop.ShopSign;


public class Events implements Listener{

    @EventHandler
    public void signContentChanged(SignChangeEvent event){
        Block block = event.getBlock();

        // Wall sign on a container
        if (!(block.getBlockData() instanceof org.bukkit.block.data.type.WallSign wallSign)) return;
        if (!(block.getRelative(wallSign.getFacing().getOppositeFace()).getState() instanceof Container)) return;

        // Parse sign contents
        ShopSign shop = ShopUtils.parseShopSign(event.lines(), true);

        if(shop == null) {
            if(ShopUtils.serialize(event.line(0)).equals("[Shop]")) {
                event.getPlayer().sendMessage("Invalid Format!");
            }
            return;
        }

        // Format sign
        ShopUtils.stylizeSign(event, shop);
    }

    @EventHandler
    public void onSignInteract(PlayerInteractEvent event) {
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Ensure Shop Sign
        Sign sign = ShopUtils.isShopSign(event.getClickedBlock());
        if (sign == null) return;

        // Trade Logic
        ShopSign shop = ShopUtils.parseShopSign(sign.getSide(Side.FRONT).lines(), false);
        if (shop == null) return;

        Player buyer = event.getPlayer();

        WallSign wallData = (WallSign) sign.getBlockData();
        Block attached = sign.getBlock().getRelative(wallData.getFacing().getOppositeFace());

        if (!(attached.getState() instanceof  Container container)) return;

        Inventory shopInventory = container.getInventory();
        Inventory playerInventory = buyer.getInventory();

        // Check Player Contents
        if (!InventoryUtils.hasItems(playerInventory, shop.inputMaterial(), shop.inputAmount())) {
            buyer.sendMessage(Component.text("You don't have enough " + ShopUtils.formatName(shop.inputMaterial()) + "!", NamedTextColor.RED));
            return;
        }

        // Check Shop Contents
        if (!InventoryUtils.hasItems(shopInventory, shop.outputMaterial(), shop.outputAmount())) {
            buyer.sendMessage(Component.text("This shop is out of stock!", NamedTextColor.RED));
            return;
        }

        // Do the trade
        // Buyer -> Shop
        InventoryUtils.removeItems(playerInventory, shop.inputMaterial(), shop.inputAmount());
        InventoryUtils.addItems(shopInventory, new ItemStack(shop.inputMaterial(), shop.inputAmount()));

        // Shop -> Buyer
        InventoryUtils.removeItems(shopInventory, shop.outputMaterial(), shop.outputAmount());
        InventoryUtils.addItems(playerInventory, new ItemStack(shop.outputMaterial(), shop.outputAmount()));

        // Success!
        buyer.sendMessage(Component.text("Trade successful!", NamedTextColor.GREEN));
        buyer.playSound(buyer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    @EventHandler
    public void onSignBreak(BlockBreakEvent event) {
        Sign sign = ShopUtils.isShopSign(event.getBlock());
        if (sign == null) return;

        Player player = event.getPlayer();
        String owner = ShopUtils.getShopSignOwner(sign);

        if (!player.getName().equalsIgnoreCase(owner) && !player.isOp()) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You do not own this shop!", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onContainerBreak(BlockBreakEvent event) {
        if (!(event.getBlock().getState() instanceof Container)) return;

        if (ShopUtils.hasAttachedShopSign(event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Remove the shop sign first!", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onContainerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Container)) return;

        Player player = event.getPlayer();
        if (player.isOp()) return;

        // Check all sides and blocks
        for (Block part : ShopUtils.getConnectedBlocks(event.getClickedBlock())) {
            for (BlockFace face : ShopUtils.HORIZONTAL_FACES) {
                // Get Shop sign
                Block relative = part.getRelative(face);
                Sign shopSign = ShopUtils.isShopSign(relative);
                if (shopSign == null) continue;

                // Make sure it's attached
                if (!(relative.getBlockData() instanceof WallSign ws) || ws.getFacing() != face) continue;

                // See if player is NOT the owner
                String owner = ShopUtils.getShopSignOwner(shopSign);
                if (!player.getName().equalsIgnoreCase(owner)) {
                    // Scold the player
                    event.setCancelled(true);
                    player.sendMessage(Component.text("This shop's inventory is locked!", NamedTextColor.RED));
                    return;
                }
            }
        }
    }
}
