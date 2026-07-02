package com.zombienw.tradeshop.utils;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;

import java.util.HashSet;
import java.util.Set;

public class BlockUtils {

    public static final BlockFace[] HORIZONTAL_FACES = {
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST
    };

    private BlockUtils() {}

    // get username as string from top line of sign
    public static String getSignOwner(Sign sign) {
        String usernameLine = FormatUtils.serialize(sign.getSide(Side.FRONT).line(0));
        return usernameLine;
    }

    // check if a container block has connected container blocks (detect double chests)
    public static Set<Block> getConnectedContainerBlocks(Block block) {
        Set<Block> blocks = new HashSet<>();
        blocks.add(block);

        if (block.getState() instanceof Chest chest) {
            Inventory inventory = chest.getInventory();
            if (inventory instanceof DoubleChestInventory doubleInv) {
                if (doubleInv.getLeftSide().getLocation() != null) {
                    blocks.add(doubleInv.getLeftSide().getLocation().getBlock());
                }
                if (doubleInv.getRightSide().getLocation() != null) {
                    blocks.add(doubleInv.getRightSide().getLocation().getBlock());
                }
            }
        }
        return blocks;
    }

}
