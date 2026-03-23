package com.zombienw.tradeShop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import com.zombienw.tradeShop.TradeShop.ShopSign;
import org.bukkit.block.Sign;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;

import java.util.*;
import java.util.stream.Collectors;

public class ShopUtils {
    private ShopUtils() {}

    public static final BlockFace[] HORIZONTAL_FACES = {
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    };

    // Get the numbers and materials from a sign; weather its being placed or traded with
    public static ShopSign parseShopSign(List<Component> lines, boolean beingCreated) {
        try {
            String line1 = serialize(lines.get(0));
            String line2 = serialize(lines.get(1));
            String line3 = serialize(lines.get(2));
            String line4 = serialize(lines.get(3));

            // Expect different text if we're creating the sign vs using it
            if (beingCreated) {
                if (!line1.equalsIgnoreCase("[Shop]")) return null;
                if (!line3.equalsIgnoreCase("for")) return null;
            } else {
                if (!line3.equals("↓")) return null;
            }

            // Get amounts and materials
            String[] splitIn  = line2.split(" ", 2);
            String[] splitOut = line4.split(" ", 2);

            int amountIn = Integer.parseInt(splitIn[0]);
            Material materialIn = Material.matchMaterial(splitIn[1].replace(" ", "_").toUpperCase());

            int amountOut = Integer.parseInt(splitOut[0]);
            Material materialOut = Material.matchMaterial(splitOut[1].replace(" ", "_").toUpperCase());

            if (materialIn == null || materialOut == null) return null;

            return new ShopSign(materialIn, amountIn, materialOut, amountOut);
        } catch (Exception e) {
            return null;
        }
    }

    // Make the sign pretty as part of confirmation feedback; also used to place creator's username
    public static void stylizeSign(SignChangeEvent event, ShopSign shop) {
        event.line(0, Component.text(event.getPlayer().getName(), NamedTextColor.DARK_GREEN));
        event.line(1, Component.text(shop.inputAmount() + " " + formatName(shop.inputMaterial())));
        event.line(2, Component.text("↓", NamedTextColor.GRAY));
        event.line(3, Component.text(shop.outputAmount() + " " + formatName(shop.outputMaterial())));

        // Wax it, like your mother's cooter
        if (event.getBlock().getState() instanceof Sign signState) {
            signState.setWaxed(true);
            signState.update();
        }
    }

    // returns the sign block object if it's a valid shop sign, otherwise returns null
    public static Sign isShopSign(Block block) {
        // make sure it's a wall sign
        if (block == null || !(block.getBlockData() instanceof WallSign)) return null;
        if (!(block.getState() instanceof Sign sign)) return null;

        // TODO find more secure way to check if it's a shop sign
        // look for arrow
        String marker = PlainTextComponentSerializer.plainText().serialize(sign.getSide(Side.FRONT).line(2));
        return marker.equals("↓") ? sign : null;
    }

    // returns the shop sign owner's name stored in line 0
    public static String getShopSignOwner(Sign sign) {
        // get top line of text
        return PlainTextComponentSerializer.plainText()
                .serialize(sign.getSide(Side.FRONT).line(0))
                .trim();
    }

    // returns true if any neighboring block is a shop sign
    public static boolean hasAttachedShopSign(Block block) {
        // for all surrounding blocks
        for (Block part : getConnectedBlocks(block)) {
            // for all faces
            for (BlockFace face : HORIZONTAL_FACES) {
                // check if it's a shop sign
                Block relative = part.getRelative(face);
                if (isShopSign(relative) == null) continue;

                // make sure it's attached properly
                if (relative.getBlockData() instanceof WallSign ws && ws.getFacing() == face) {
                    return true;
                }
            }
        }
        return false;
    }

    // get all surrounding blocks horizontally
    public static Set<Block> getConnectedBlocks(Block block) {
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

    // Component Text -> Normal Text
    public static String serialize(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component).trim();
    }

    // DIAMOND_SWORD -> Diamond Sword
    public static String formatName(Material material) {
        return Arrays.stream(material.name().replace("_", " ").toLowerCase().split(" "))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
