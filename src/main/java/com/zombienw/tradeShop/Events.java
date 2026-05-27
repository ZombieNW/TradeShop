package com.zombienw.tradeShop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
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
        ShopSign shop = ShopUtils.parseNewShopSign(event.lines(), event.getPlayer().getName());

        if(shop == null) {
            if(ShopUtils.serialize(event.line(0)).equals("[Shop]")) {
                event.getPlayer().sendMessage("Invalid Format!");
            }
            return;
        }

        // Format sign
        ShopUtils.stylizeSign(event, shop);

        // save PDC on next tick (can't do it this tick or we die)
        event.getPlayer().getServer().getScheduler().runTask(
                TradeShop.getPlugin(TradeShop.class),
                () -> {
                    if (!(block.getState() instanceof Sign sign)) return;
                    if (shop.isPending()) {
                        ShopUtils.savePendingData(sign, shop);
                    } else {
                        ShopUtils.saveShopData(sign, shop);
                    }
                }
        );

        if (shop.isPending()) {
            event.getPlayer().sendMessage(Component.text("Right-click the sign with each item to finish setup.", NamedTextColor.YELLOW));
        } else {
            event.getPlayer().sendMessage(Component.text("Shop created!", NamedTextColor.GREEN));
        }
    }

    @EventHandler
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Sign sign = ShopUtils.isShopSign(event.getClickedBlock());
        if (sign == null) return;

        Player player = event.getPlayer();

        String line2 = ShopUtils.serialize(sign.getSide(Side.FRONT).line(1));
        String line4 = ShopUtils.serialize(sign.getSide(Side.FRONT).line(3));

        if (line2.endsWith("[Hand]") || line4.endsWith("[Hand]")) {
            handleHandResolution(player, sign, line2, line4);
            return;
        }

        handleTrade(player, sign);
    }

    private void handleHandResolution(Player player, Sign sign, String line2, String line4) {
        if (!player.getName().equalsIgnoreCase(ShopUtils.getShopSignOwner(sign))) {
            player.sendMessage(Component.text("This shop isn't set up yet!", NamedTextColor.RED));
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            player.sendMessage(Component.text("Hold the item you want to assign!", NamedTextColor.RED));
            return;
        }

        ItemStack template = heldItem.clone();
        template.setAmount(1);

        ShopSign pending = ShopUtils.loadPendingData(sign);
        if (pending == null) {
            player.sendMessage(Component.text("Something went wrong!", NamedTextColor.RED));
            return;
        }

        boolean line2IsHand = line2.endsWith("[Hand]");

        ItemStack resolvedIn  = (line2IsHand  && pending.inputItem()  == null) ? template : pending.inputItem();
        ItemStack resolvedOut = (!line2IsHand && pending.outputItem() == null) ? template : pending.outputItem();

        if (resolvedIn == null || resolvedOut == null) {
            player.sendMessage(Component.text("Something went wrong loading shop data!", NamedTextColor.RED));
            return;
        }

        ShopSign resolved = new ShopSign(resolvedIn, pending.inputAmount(), resolvedOut, pending.outputAmount());

        ShopUtils.updateSignLine(sign, line2IsHand ? 1 : 3,
                line2IsHand ? resolved.inputAmount()  : resolved.outputAmount(),
                line2IsHand ? resolved.inputItem()    : resolved.outputItem());

        if (resolved.isPending()) {
            ShopUtils.savePendingData(sign, resolved);
            player.sendMessage(Component.text("Now right-click with the output item!", NamedTextColor.YELLOW));
            return;
        }

        ShopUtils.saveShopData(sign, resolved);
        ShopUtils.clearPendingData(sign);
        player.sendMessage(Component.text("Shop created!", NamedTextColor.GREEN));
    }

    private void handleTrade(Player player, Sign sign) {
        ShopSign shop = ShopUtils.loadShopData(sign);
        if (shop == null) return;

        WallSign wallData = (WallSign) sign.getBlockData();
        Block attached = sign.getBlock().getRelative(wallData.getFacing().getOppositeFace());

        if (!(attached.getState() instanceof Container container)) return;

        Inventory shopInventory   = container.getInventory();
        Inventory playerInventory = player.getInventory();

        if (!InventoryUtils.hasItems(playerInventory, shop.inputItem(), shop.inputAmount())) {
            player.sendMessage(Component.text("You don't have enough " + ShopUtils.formatName(shop.inputMaterial()) + "!", NamedTextColor.RED));
            return;
        }

        if (!InventoryUtils.hasItems(shopInventory, shop.outputItem(), shop.outputAmount())) {
            player.sendMessage(Component.text("This shop is out of stock!", NamedTextColor.RED));
            return;
        }

        // Do the trade
        InventoryUtils.removeItems(playerInventory, shop.inputItem(),  shop.inputAmount());
        InventoryUtils.addItems(shopInventory, shop.inputItem().asQuantity(shop.inputAmount()));

        InventoryUtils.removeItems(shopInventory,   shop.outputItem(), shop.outputAmount());
        InventoryUtils.addItems(playerInventory, shop.outputItem().asQuantity(shop.outputAmount()));

        player.sendMessage(Component.text("Trade successful!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
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
