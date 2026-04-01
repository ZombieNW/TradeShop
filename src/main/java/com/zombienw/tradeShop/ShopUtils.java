package com.zombienw.tradeShop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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

    // PDC Classes
    private static TradeShop plugin() { return TradeShop.getPlugin(TradeShop.class); }

    private static NamespacedKey keyInputMaterial() { return new NamespacedKey(plugin(), "input_material"); }
    private static NamespacedKey keyInputAmount() { return new NamespacedKey(plugin(), "input_amount"); }
    private static NamespacedKey keyOutputMaterial() { return new NamespacedKey(plugin(), "output_material"); }
    private static NamespacedKey keyOutputAmount() { return new NamespacedKey(plugin(), "output_amount"); }
    private static NamespacedKey keyPendingInputMaterial() { return new NamespacedKey(plugin(), "pending_input_material"); }
    private static NamespacedKey keyPendingInputAmount() { return new NamespacedKey(plugin(), "pending_input_amount"); }
    private static NamespacedKey keyPendingOutputMaterial() { return new NamespacedKey(plugin(), "pending_output_material"); }
    private static NamespacedKey keyPendingOutputAmount() { return new NamespacedKey(plugin(), "pending_output_amount"); }

    // Save shop data to sign PDC
    public static void saveShopData(Sign sign, ShopSign shop) {
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        pdc.set(keyInputMaterial(),  PersistentDataType.STRING,  shop.inputMaterial().name());
        pdc.set(keyInputAmount(),    PersistentDataType.INTEGER, shop.inputAmount());
        pdc.set(keyOutputMaterial(), PersistentDataType.STRING,  shop.outputMaterial().name());
        pdc.set(keyOutputAmount(),   PersistentDataType.INTEGER, shop.outputAmount());
        sign.update();
    }

    // Load shop data from sign PDC
    public static ShopSign loadShopData(Sign sign) {
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        if (!pdc.has(keyInputMaterial(), PersistentDataType.STRING)) return null;

        try {
            Material in  = Material.valueOf(pdc.get(keyInputMaterial(),  PersistentDataType.STRING));
            Material out = Material.valueOf(pdc.get(keyOutputMaterial(), PersistentDataType.STRING));
            int amtIn    = pdc.get(keyInputAmount(),  PersistentDataType.INTEGER);
            int amtOut   = pdc.get(keyOutputAmount(), PersistentDataType.INTEGER);
            return new ShopSign(in, amtIn, out, amtOut);
        } catch (Exception e) {
            return null;
        }
    }

    public static void savePendingData(Sign sign, ShopSign shop) {
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        if (shop.inputMaterial() != null)
            pdc.set(keyPendingInputMaterial(),  PersistentDataType.STRING,  shop.inputMaterial().name());
        if (shop.outputMaterial() != null)
            pdc.set(keyPendingOutputMaterial(), PersistentDataType.STRING,  shop.outputMaterial().name());
        pdc.set(keyPendingInputAmount(),  PersistentDataType.INTEGER, shop.inputAmount());
        pdc.set(keyPendingOutputAmount(), PersistentDataType.INTEGER, shop.outputAmount());
        sign.update();
    }

    public static ShopSign loadPendingData(Sign sign) {
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        if (!pdc.has(keyPendingInputAmount(), PersistentDataType.INTEGER)) return null;

        // Materials may be null if that slot was [Hand]
        Material in = pdc.has(keyPendingInputMaterial(), PersistentDataType.STRING)
                ? Material.valueOf(pdc.get(keyPendingInputMaterial(), PersistentDataType.STRING)) : null;
        Material out = pdc.has(keyPendingOutputMaterial(), PersistentDataType.STRING)
                ? Material.valueOf(pdc.get(keyPendingOutputMaterial(), PersistentDataType.STRING)) : null;

        int amtIn  = pdc.get(keyPendingInputAmount(),  PersistentDataType.INTEGER);
        int amtOut = pdc.get(keyPendingOutputAmount(),  PersistentDataType.INTEGER);

        return new ShopSign(in, amtIn, out, amtOut);
    }

    public static void clearPendingData(Sign sign) {
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        pdc.remove(keyPendingInputMaterial());
        pdc.remove(keyPendingInputAmount());
        pdc.remove(keyPendingOutputMaterial());
        pdc.remove(keyPendingOutputAmount());
        sign.update();
    }

    // new separate class for creating a sign
    // [Hand] is for fields that are too long
    public static ShopSign parseNewShopSign(List<Component> lines, String playerName) {
        try {
            String line1 = serialize(lines.get(0));
            String line2 = serialize(lines.get(1));
            String line3 = serialize(lines.get(2));
            String line4 = serialize(lines.get(3));

            if (!line1.equalsIgnoreCase("[Shop]")) return null;
            if (!line3.equalsIgnoreCase("for"))    return null;

            String[] splitIn  = line2.split(" ", 2);
            String[] splitOut = line4.split(" ", 2);

            int amountIn  = Integer.parseInt(splitIn[0]);
            int amountOut = Integer.parseInt(splitOut[0]);

            // [Hand] means material will be filled in later; otherwise resolve now
            Material materialIn  = splitIn[1].equalsIgnoreCase("[Hand]")
                    ? null : Material.matchMaterial(splitIn[1].replace(" ", "_").toUpperCase());
            Material materialOut = splitOut[1].equalsIgnoreCase("[Hand]")
                    ? null : Material.matchMaterial(splitOut[1].replace(" ", "_").toUpperCase());

            // Non-[Hand] entries must be valid materials
            if (materialIn == null  && !splitIn[1].equalsIgnoreCase("[Hand]"))  return null;
            if (materialOut == null && !splitOut[1].equalsIgnoreCase("[Hand]")) return null;

            return new ShopSign(materialIn, amountIn, materialOut, amountOut);
        } catch (Exception e) {
            return null;
        }
    }

    // Make the sign pretty as part of confirmation feedback; also used to place creator's username
    public static void stylizeSign(SignChangeEvent event, ShopSign shop) {
        event.line(0, Component.text(event.getPlayer().getName(), NamedTextColor.DARK_GREEN));
        event.line(1, shop.inputMaterial()  != null
                ? Component.text(shop.inputAmount()  + " " + formatName(shop.inputMaterial()))
                : Component.text(shop.inputAmount()  + " [Hand]", NamedTextColor.GOLD));
        event.line(2, Component.text("↓", NamedTextColor.GRAY));
        event.line(3, shop.outputMaterial() != null
                ? Component.text(shop.outputAmount() + " " + formatName(shop.outputMaterial()))
                : Component.text(shop.outputAmount() + " [Hand]", NamedTextColor.GOLD));

        // Wax it, like your mother's cooter
        if (event.getBlock().getState() instanceof Sign signState) {
            signState.setWaxed(true);
            signState.update();
        }
    }

    // change sign line
    public static void updateSignLine(Sign sign, int lineIndex, int amount, Material material) {
        sign.getSide(Side.FRONT).line(lineIndex, Component.text(amount + " " + formatName(material)));
        sign.update();
    }

    // returns the sign block object if it's a valid shop sign, otherwise returns null
    public static Sign isShopSign(Block block) {
        if (block == null || !(block.getBlockData() instanceof WallSign)) return null;
        if (!(block.getState() instanceof Sign sign)) return null;

        // PDC check
        if (sign.getPersistentDataContainer().has(keyInputMaterial(), PersistentDataType.STRING)) return sign;

        // check if it still has Hand markers
        String line3 = serialize(sign.getSide(Side.FRONT).line(2));
        String line2 = serialize(sign.getSide(Side.FRONT).line(1));
        String line4 = serialize(sign.getSide(Side.FRONT).line(3));
        if (line3.equals("↓") && (line2.endsWith("[Hand]") || line4.endsWith("[Hand]"))) return sign;

        return null;
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
        String[] words = material.name().replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();

        // only abbreviate multiple word names
        for (String word : words) {
            String formatted = words.length > 1
                    ? abbreviateWord(word)
                    : Character.toUpperCase(word.charAt(0)) + word.substring(1);
            result.append(formatted).append(" ");
        }

        return result.toString().trim();
    }

    private static String abbreviateWord(String word) {
        return switch (word) {
            // Armor/Tool Types
            case "diamond"   -> "Dia.";
            case "netherite" -> "Neth.";
            case "golden"    -> "Gld.";
            case "wooden"    -> "Wdn.";
            case "chainmail" -> "Chn.";

            // Food
            case "cooked"     -> "Ckd.";
            case "baked"      -> "Bkd.";
            case "enchanted"  -> "Ench.";
            case "suspicious" -> "Sus.";
            case "porkchop"   -> "Pork";

            default -> Character.toUpperCase(word.charAt(0)) + word.substring(1);
        };
    }
}
