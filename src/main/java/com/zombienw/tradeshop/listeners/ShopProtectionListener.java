package com.zombienw.tradeshop.listeners;

import com.zombienw.tradeshop.Constants;
import com.zombienw.tradeshop.managers.ShopManager;
import com.zombienw.tradeshop.utils.BlockUtils;
import com.zombienw.tradeshop.utils.FormatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class ShopProtectionListener implements Listener {

    private final ShopManager shopManager;

    public ShopProtectionListener(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @EventHandler
    // prevent breaking a shop sign if you didn't make it
    public void onSignBreak(BlockBreakEvent event) {
        if (!(event.getBlock().getState() instanceof Sign sign)) return;

        // check for either pending and registered PDC data
        String line3 = FormatUtils.serialize(sign.getSide(Side.FRONT).line(2));
        if (!shopManager.isRegisteredShop(sign) && !line3.equals(Constants.SIGN_DIVIDER)) return;

        Player player = event.getPlayer();
        String owner = BlockUtils.getSignOwner(sign);

        // shun the player
        if (!player.getName().equalsIgnoreCase(owner) && !player.hasPermission("tradeshop.admin")) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You do not own this shop!", NamedTextColor.RED));
        }
    }

    @EventHandler
    // prevent breaking a chest with a shop sign on it
    public void onContainerBreak(BlockBreakEvent event) {
        if (!(event.getBlock().getState() instanceof Container)) return;

        // shun the player
        if (hasAttachedShopSign(event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Remove the shop sign first!", NamedTextColor.RED));
        }
    }

    @EventHandler
    // don't let other players open shop chests
    public void onContainerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Container)) return;

        Player player = event.getPlayer();
        if (player.hasPermission("tradeshop.admin")) return;

        // check other part of double chest
        for (Block part : BlockUtils.getConnectedContainerBlocks(event.getClickedBlock())) {
            for (BlockFace face : BlockUtils.HORIZONTAL_FACES) {
                Block relative = part.getRelative(face);

                if (!(relative.getState() instanceof Sign sign)) continue;
                if (!(relative.getBlockData() instanceof WallSign ws) || ws.getFacing() != face) continue;

                // Validate if it's actually our plugin's shop sign
                if (!shopManager.isRegisteredShop(sign)) {
                    String line3 = FormatUtils.serialize(sign.getSide(Side.FRONT).line(2));
                    if (!line3.equals(com.zombienw.tradeshop.Constants.SIGN_DIVIDER)) continue;
                }

                // shun the player
                String owner = BlockUtils.getSignOwner(sign);
                if (!player.getName().equalsIgnoreCase(owner)) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("This shop's inventory is locked!", NamedTextColor.RED));
                    return;
                }
            }
        }
    }

    // helper to check if a chest has a shop sign
    private boolean hasAttachedShopSign(@NotNull Block block) {
        for (Block part : BlockUtils.getConnectedContainerBlocks(block)) {
            for (BlockFace face : BlockUtils.HORIZONTAL_FACES) {
                Block relative = part.getRelative(face);
                if (!(relative.getState() instanceof Sign sign)) continue;

                if (relative.getBlockData() instanceof WallSign ws && ws.getFacing() == face) {
                    if (shopManager.isRegisteredShop(sign)) return true;
                    String line3 = FormatUtils.serialize(sign.getSide(Side.FRONT).line(2));
                    if (line3.equals(Constants.SIGN_DIVIDER)) return true;
                }
            }
        }
        return false;
    }
}
