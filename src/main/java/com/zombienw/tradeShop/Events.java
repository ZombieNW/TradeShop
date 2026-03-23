package com.zombienw.tradeShop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.Wall;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class Events implements Listener{

    public record ShopSign(Material inputMaterial, int inputAmount, Material outputMaterial, int outputAmount){}

    @EventHandler
    public void signContentChanged(SignChangeEvent event){
        Block block = event.getBlock();

        // Wall sign on a container
        if (!(block.getBlockData() instanceof org.bukkit.block.data.type.WallSign wallSign)) return;
        if (!(block.getRelative(wallSign.getFacing().getOppositeFace()).getState() instanceof Container)) return;

        // Parse sign contents
        ShopSign shop = parseShopSign(event.lines());

        if(shop == null) {
            String header = serialize(event.line(0));
            if(header.equals("[Shop]")) {
                event.getPlayer().sendMessage("Invalid Format!");
            }
            return;
        }

        // Format sign
        stylizeSign(event, shop);
    }

    @EventHandler
    public void onSignInteract(PlayerInteractEvent event) {
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();

        // Ensure Shop Sign
        Sign sign = isShopSign(block);
        if (sign == null) return;

        // Trade Logic
        ShopSign shop = parseExistingShopSign(sign.getSide(Side.FRONT).lines());
        if (shop == null) return;

        Player buyer = event.getPlayer();

        WallSign wallData = (WallSign) sign.getBlockData();
        Block attached = sign.getBlock().getRelative(wallData.getFacing().getOppositeFace());

        if (!(attached.getState() instanceof  Container container)) return;
        Inventory shopInventory = container.getInventory();
        Inventory playerInventory = buyer.getInventory();

        // Check Player Contents
        if (!hasItems(playerInventory, shop.inputMaterial, shop.inputAmount)) {
            buyer.sendMessage(Component.text("You don't have enough " + formatName(shop.inputMaterial()) + "!", NamedTextColor.RED));
            return;
        }

        // Check Shop Contents
        if (!hasItems(shopInventory, shop.outputMaterial(), shop.outputAmount())) {
            buyer.sendMessage(Component.text("This shop is out of stock!", NamedTextColor.RED));
            return;
        }

        // Do the trade
        // Buyer -> Shop
        removeItems(playerInventory, shop.inputMaterial, shop.inputAmount);
        addItems(shopInventory, new ItemStack(shop.inputMaterial, shop.inputAmount));

        // Shop -> Buyer
        removeItems(shopInventory, shop.outputMaterial, shop.outputAmount);
        addItems(playerInventory, new ItemStack(shop.outputMaterial, shop.outputAmount));

        // Success!
        buyer.sendMessage(Component.text("Trade successful!", NamedTextColor.GREEN));
        buyer.playSound(buyer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    @EventHandler
    public void onSignBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Ensure Shop Sign
        Sign sign = isShopSign(block);
        if (sign == null) return;

        // Only shop owner can break it
        String ownerName = PlainTextComponentSerializer.plainText().serialize(sign.getSide(Side.FRONT).line(0)).trim();
        if (!player.getName().equalsIgnoreCase(ownerName)) {
            if (player.isOp()) return;

            event.setCancelled(true);
            player.sendMessage(Component.text("You do not own this shop!", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onContainerBreak(BlockBreakEvent event) {
        if (!(event.getBlock().getState() instanceof Container)) return;

        // Check all sides and blocks
        for (Block part : getConnectedBlocks(event.getBlock())) {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                // Get ShopSign
                Block relative = part.getRelative(face);
                if (isShopSign(relative) == null) continue;

                // Double check attachment
                if (relative.getBlockData() instanceof WallSign ws && ws.getFacing() == face) {
                    // Scold the player
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(Component.text("Remove the shop sign first!", NamedTextColor.RED));
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onContainerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (!(event.getClickedBlock().getState() instanceof Container)) return;

        Player player = event.getPlayer();
        if (player.isOp()) return;

        // Check all sides and blocks
        for (Block part : getConnectedBlocks(event.getClickedBlock())) {
            for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                // get sign shop
                Block relative = part.getRelative(face);
                Sign shopSign = isShopSign(relative);
                if (shopSign == null) continue;

                // Check if sign is actually attached to THIS part
                if (!(relative.getBlockData() instanceof WallSign ws) || ws.getFacing() != face) continue;

                String owner = PlainTextComponentSerializer.plainText().serialize(shopSign.getSide(Side.FRONT).line(0)).trim();
                if (!player.getName().equalsIgnoreCase(owner)) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("This shop's inventory is locked!", NamedTextColor.RED));
                    return;
                }
            }
        }
    }

    public void stylizeSign(SignChangeEvent event, ShopSign shop) {
        // Stylize
        event.line(0, Component.text(event.getPlayer().getName(), NamedTextColor.DARK_GREEN));
        event.line(1, Component.text(shop.inputAmount + " " + formatName(shop.inputMaterial)));
        event.line(2, Component.text("↓", NamedTextColor.GRAY));
        event.line(3, Component.text(shop.outputAmount + " " + formatName(shop.outputMaterial)));

        // Wax the sign
        if (event.getBlock().getState() instanceof Sign signState) {
            signState.setWaxed(true);
            signState.update();
        }
    }

    public ShopSign parseShopSign(List<Component> lines){
        try {
            String line1 = serialize(lines.get(0)); // [Shop]
            String line2 = serialize(lines.get(1)); // 9 Diamond
            String line3 = serialize(lines.get(2)); // for
            String line4 = serialize(lines.get(3)); // 1 Elytra

            // Check for Shop
            if (!line1.equalsIgnoreCase("[Shop]")) return null;
            if (!line3.equalsIgnoreCase("for")) return null;

            // Parse Input
            String[] splitIn = line2.split(" ", 2);
            int amountIn = Integer.parseInt(splitIn[0]);
            Material materialIn = Material.matchMaterial(splitIn[1].replace(" ", "_").toUpperCase());

            // Parse Output
            String[] splitOut = line4.split(" ", 2);
            int amountOut = Integer.parseInt(splitOut[0]);
            Material materialOut = Material.matchMaterial(splitOut[1].replace(" ", "_").toUpperCase());

            if (materialIn == null || materialOut == null) return null;

            return new ShopSign(materialIn, amountIn, materialOut, amountOut);
        } catch (Exception e) {
            return null;
        }
    }

    public ShopSign parseExistingShopSign(List<Component> lines){
        try {
            String line2 = serialize(lines.get(1)); // 9 Diamond
            String line3 = serialize(lines.get(2)); // down arrow
            String line4 = serialize(lines.get(3)); // 1 Elytra

            // Check for Shop
            if (!line3.equalsIgnoreCase("↓")) return null;

            // Parse Input
            String[] splitIn = line2.split(" ", 2);
            int amountIn = Integer.parseInt(splitIn[0]);
            Material materialIn = Material.matchMaterial(splitIn[1].replace(" ", "_").toUpperCase());

            // Parse Output
            String[] splitOut = line4.split(" ", 2);
            int amountOut = Integer.parseInt(splitOut[0]);
            Material materialOut = Material.matchMaterial(splitOut[1].replace(" ", "_").toUpperCase());

            if (materialIn == null || materialOut == null) return null;

            return new ShopSign(materialIn, amountIn, materialOut, amountOut);
        } catch (Exception e) {
            return null;
        }
    }

    private String serialize(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component).trim();
    }

    private String formatName(Material material) {
        String name = material.name().replace("_", " ").toLowerCase();
        return Arrays.stream(name.split(" "))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private Sign isShopSign(Block block) {
        if (block == null || !(block.getBlockData() instanceof WallSign)) return null;
        if (!(block.getState() instanceof Sign sign)) return null;

        // TODO find more secure way to check if its a shop sign
        SignSide side = sign.getSide(Side.FRONT);
        String marker = PlainTextComponentSerializer.plainText().serialize(side.line(2));
        if (!marker.equals("↓")) return null;

        return sign;
    }

    private Set<Block> getConnectedBlocks(Block block) {
        Set<Block> blocks = new HashSet<>();
        blocks.add(block);

        if (block.getState() instanceof Chest chest) {
            Inventory inventory = chest.getInventory();
            if (inventory instanceof DoubleChestInventory doubleInv) {
                blocks.add(Objects.requireNonNull(doubleInv.getLeftSide().getLocation()).getBlock());
                blocks.add(Objects.requireNonNull(doubleInv.getRightSide().getLocation()).getBlock());
            }
        }
        return blocks;
    }

    private boolean hasItems(Inventory inv, Material type, int amount) {
        return inv.containsAtLeast(new ItemStack(type), amount);
    }

    private void removeItems(Inventory inv, Material type, int amount) {
        inv.removeItem(new ItemStack(type, amount));
    }

    private void addItems(Inventory inv, ItemStack item) {
        Map<Integer, ItemStack> leftovers = inv.addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            Objects.requireNonNull(inv.getLocation()).getWorld().dropItemNaturally(inv.getLocation(), leftover);
        }
    }
}
