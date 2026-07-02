package com.zombienw.tradeshop.listeners;

import com.zombienw.tradeshop.Constants;
import com.zombienw.tradeshop.TradeShop;
import com.zombienw.tradeshop.managers.ShopManager;
import com.zombienw.tradeshop.models.ShopData;
import com.zombienw.tradeshop.utils.BlockUtils;
import com.zombienw.tradeshop.utils.FormatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ShopSetupListener implements Listener {

    private final ShopManager shopManager;
    private final TradeShop plugin;

    public ShopSetupListener(ShopManager shopManager, TradeShop plugin) {
        this.shopManager = shopManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignCreate(SignChangeEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (!(block.getBlockData() instanceof WallSign wallSign)) return;
        if (!(block.getRelative(wallSign.getFacing().getOppositeFace()).getState() instanceof Container)) return;

        // try to get the data we need and scold otherwise
        ShopData shopData = parseShopInput(event.lines());
        if (shopData == null) {
            if (FormatUtils.serialize(event.line(0)).equalsIgnoreCase(Constants.SIGN_HEADER)) {
                player.sendMessage(Component.text("Invalid Format!", NamedTextColor.RED));
            }
            return;
        }

        stylizeSign(event, shopData);

        // schedule the PDC so it gets set after the sign change goes into full effect
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!(block.getState() instanceof Sign sign)) return;
            if (shopData.isPending()) {
                shopManager.savePendingData(sign, shopData);
            } else {
                shopManager.saveShopData(sign, shopData);
            }

            checkIncorrectFormatting(sign);
        });

        if (shopData.isPending()) {
            player.sendMessage(Component.text("Right-click the sign with each item to finish setup.", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("Shop created!", NamedTextColor.GREEN));
        }
    }

    @EventHandler
    // when a player interacts with a sign with a [Hand] on it
    public void onSignHandInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Sign sign)) return;

        String line2 = FormatUtils.serialize(sign.getSide(Side.FRONT).line(1));
        String line4 = FormatUtils.serialize(sign.getSide(Side.FRONT).line(3));

        if (!line2.endsWith(Constants.HAND_PLACEHOLDER) && !line4.endsWith(Constants.HAND_PLACEHOLDER)) return;

        // send it to the handler
        handleHandResolution(event.getPlayer(), sign, line2);
    }

    /**
     * Due to a reported bug involving a plugin that removes sign formatting,
     * this method checks for format symbols and removes them.
     * @param sign Affected sign
     */
    private void checkIncorrectFormatting(Sign sign) {
        String regex = "^&[0-9a-fk-orA-FK-OR].*";

        // username line
        String line0 = FormatUtils.serialize(sign.getSide(Side.FRONT).line(0));
        if (line0.matches(regex)) {
            sign.getSide(Side.FRONT).line(0, net.kyori.adventure.text.Component.text(line0.substring(2)));
        }

        // arrow line
        String line2 = FormatUtils.serialize(sign.getSide(Side.FRONT).line(2));
        if (line2.matches(regex)) {
            sign.getSide(Side.FRONT).line(2, net.kyori.adventure.text.Component.text(line2.substring(2)));
        }

        sign.update();
    }

    // handler for filling in [Hand]'s
    private void handleHandResolution(Player player, Sign sign, String line2) {
        // shoo away other players
        if (!player.getName().equalsIgnoreCase(BlockUtils.getSignOwner(sign))) {
            player.sendMessage(Component.text("This shop isn't set up yet!", NamedTextColor.RED));
            return;
        }

        // make sure they're holding an item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            player.sendMessage(Component.text("Hold the item you want to assign!", NamedTextColor.RED));
            return;
        }

        ItemStack template = heldItem.clone();
        template.setAmount(1);

        ShopData pending = shopManager.loadPendingData(sign);
        if (pending == null) {
            player.sendMessage(Component.text("Something went wrong loading shop data!", NamedTextColor.RED));
            return;
        }

        boolean line2IsHand = line2.endsWith(Constants.HAND_PLACEHOLDER);

        ItemStack resolvedIn = (line2IsHand && pending.inputItem() == null) ? template : pending.inputItem();
        ItemStack resolvedOut = (!line2IsHand && pending.outputItem() == null) ? template : pending.outputItem();

        ShopData resolvedShop = new ShopData(resolvedIn, pending.inputAmount(), resolvedOut, pending.outputAmount());

        updateSignLine(sign, line2IsHand ? 1 : 3,
                line2IsHand ? resolvedShop.inputAmount() : resolvedShop.outputAmount(),
                line2IsHand ? resolvedShop.inputItem() : resolvedShop.outputItem());

        if (resolvedShop.isPending()) {
            shopManager.savePendingData(sign, resolvedShop);
            player.sendMessage(Component.text("Now right-click with the output item!", NamedTextColor.YELLOW));
            return;
        }

        shopManager.saveShopData(sign, resolvedShop);
        shopManager.clearPendingData(sign);
        player.sendMessage(Component.text("Shop created!", NamedTextColor.GREEN));
    }

    // convert written player sign in to a formatted shop sign
    private ShopData parseShopInput(@NotNull List<Component> lines) {
        try {
            String line1 = FormatUtils.serialize(lines.get(0));
            String line2 = FormatUtils.serialize(lines.get(1));
            String line3 = FormatUtils.serialize(lines.get(2));
            String line4 = FormatUtils.serialize(lines.get(3));

            if (!line1.equalsIgnoreCase(Constants.SIGN_HEADER) || !line3.equalsIgnoreCase(Constants.SIGN_FOR)) {
                return null;
            }

            String[] splitIn = line2.split(" ", 2);
            String[] splitOut = line4.split(" ", 2);

            int amountIn = Integer.parseInt(splitIn[0]);
            int amountOut = Integer.parseInt(splitOut[0]);

            ItemStack itemIn = parseItemString(splitIn[1]);
            ItemStack itemOut = parseItemString(splitOut[1]);

            return new ShopData(itemIn, amountIn, itemOut, amountOut);
        } catch (Exception e) {
            return null;
        }
    }

    // player-written string into an itemstack (or at least try)
    private ItemStack parseItemString(String itemName) {
        if (itemName.equalsIgnoreCase(Constants.HAND_PLACEHOLDER)) return null;
        Material m = Material.matchMaterial(itemName.replace(" ", "_").toUpperCase());
        return m != null ? new ItemStack(m) : null;
    }

    // set all sign lines how they're meant to be
    private void stylizeSign(SignChangeEvent event, ShopData shop) {
        event.line(0, Component.text(event.getPlayer().getName(), NamedTextColor.DARK_GREEN));
        event.line(1, FormatUtils.formatSignLine(shop.inputAmount(), shop.inputMaterial()));
        event.line(2, Component.text(Constants.SIGN_DIVIDER, NamedTextColor.GRAY));
        event.line(3, FormatUtils.formatSignLine(shop.outputAmount(), shop.outputMaterial()));

        if (event.getBlock().getState() instanceof Sign signState) {
            signState.setWaxed(true);
            signState.update();
        }
    }

    // set sign line to "N Item"
    private void updateSignLine(Sign sign, int lineIndex, int amount, ItemStack item) {
        sign.getSide(Side.FRONT).line(lineIndex, Component.text(amount + " " + FormatUtils.formatName(item.getType())));
        sign.update();
    }
}
