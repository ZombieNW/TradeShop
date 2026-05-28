package com.zombienw.tradeshop.managers;

import com.zombienw.tradeshop.TradeShop;
import com.zombienw.tradeshop.models.ShopData;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ShopManager {

    private final TradeShop plugin;

    // Normal Shop Item Keys
    private final NamespacedKey keyInputItem;
    private final NamespacedKey keyInputAmount;
    private final NamespacedKey keyOutputItem;
    private final NamespacedKey keyOutputAmount;

    // Pending Item Keys
    private final NamespacedKey keyPendingInputItem;
    private final NamespacedKey keyPendingInputAmount;
    private final NamespacedKey keyPendingOutputItem;
    private final NamespacedKey keyPendingOutputAmount;

    public ShopManager(TradeShop plugin) {
        this.plugin = plugin;

        // Init all the keys
        this.keyInputItem = new NamespacedKey(plugin, "input_item");
        this.keyInputAmount = new NamespacedKey(plugin, "input_amount");
        this.keyOutputItem = new NamespacedKey(plugin, "output_item");
        this.keyOutputAmount = new NamespacedKey(plugin, "output_amount");

        this.keyPendingInputItem = new NamespacedKey(plugin, "pending_input_item");
        this.keyPendingInputAmount = new NamespacedKey(plugin, "pending_input_amount");
        this.keyPendingOutputItem = new NamespacedKey(plugin, "pending_output_item");
        this.keyPendingOutputAmount = new NamespacedKey(plugin, "pending_output_amount");
    }

    // Save shop data to sign block as pdc
    public void saveShopData(Sign sign, ShopData data) {
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        pdc.set(keyInputItem, PersistentDataType.BYTE_ARRAY, serializeItem(data.inputItem()));
        pdc.set(keyInputAmount, PersistentDataType.INTEGER, data.inputAmount());
        pdc.set(keyOutputItem, PersistentDataType.BYTE_ARRAY, serializeItem(data.outputItem()));
        pdc.set(keyOutputAmount, PersistentDataType.INTEGER, data.outputAmount());
        sign.update();
    }

    // Get PDC shop data from sign
    public ShopData loadShopData(Sign sign) {
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        if (!pdc.has(keyInputItem, PersistentDataType.BYTE_ARRAY)) return null;

        try {
            ItemStack in = deserializeItem(pdc.get(keyInputItem, PersistentDataType.BYTE_ARRAY));
            ItemStack out = deserializeItem(pdc.get(keyOutputItem, PersistentDataType.BYTE_ARRAY));
            int amtIn = pdc.get(keyInputAmount, PersistentDataType.INTEGER);
            int amtOut = pdc.get(keyOutputAmount, PersistentDataType.INTEGER);
            return new ShopData(in, amtIn, out, amtOut);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load shop data: " + e.getMessage());
            return null;
        }
    }

    // Save shop data relating to hand/pending shop data to the sign
    public void savePendingData(Sign sign, ShopData data) {
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        if (data.inputItem() != null) {
            pdc.set(keyPendingInputItem, PersistentDataType.BYTE_ARRAY, serializeItem(data.inputItem()));
        }
        if (data.outputItem() != null) {
            pdc.set(keyPendingOutputItem, PersistentDataType.BYTE_ARRAY, serializeItem(data.outputItem()));
        }
        pdc.set(keyPendingInputAmount, PersistentDataType.INTEGER, data.inputAmount());
        pdc.set(keyPendingOutputAmount, PersistentDataType.INTEGER, data.outputAmount());
        sign.update();
    }

    // Get PDC shop data relating to pending/hand from the sign
    public ShopData loadPendingData(Sign sign) {
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        if (!pdc.has(keyPendingInputAmount, PersistentDataType.INTEGER)) return null;

        ItemStack in = pdc.has(keyPendingInputItem, PersistentDataType.BYTE_ARRAY)
                ? deserializeItem(pdc.get(keyPendingInputItem, PersistentDataType.BYTE_ARRAY)) : null;
        ItemStack out = pdc.has(keyPendingOutputItem, PersistentDataType.BYTE_ARRAY)
                ? deserializeItem(pdc.get(keyPendingOutputItem, PersistentDataType.BYTE_ARRAY)) : null;

        int amtIn = pdc.get(keyPendingInputAmount, PersistentDataType.INTEGER);
        int amtOut = pdc.get(keyPendingOutputAmount, PersistentDataType.INTEGER);
        return new ShopData(in, amtIn, out, amtOut);
    }

    // Remove hand/pending data from shop sign's PDC data
    public void clearPendingData(Sign sign) {
        PersistentDataContainer pdc = sign.getPersistentDataContainer();
        pdc.remove(keyPendingInputItem);
        pdc.remove(keyPendingInputAmount);
        pdc.remove(keyPendingOutputItem);
        pdc.remove(keyPendingOutputAmount);
        sign.update();
    }

    // See if a sign is a shop by if it has an item
    public boolean isRegisteredShop(Sign sign) {
        return sign.getPersistentDataContainer().has(keyInputItem, PersistentDataType.BYTE_ARRAY);
    }

    // turn itemstack into data to put on the sign PDC
    private byte[] serializeItem(ItemStack item) {
        if (item == null) return new byte[0];
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
            boos.writeObject(item);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    // turn funny pdc data back into an item stack
    private ItemStack deserializeItem(byte[] data) {
        if (data == null || data.length == 0) return null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
            return (ItemStack) bois.readObject();
        } catch (Exception e) {
            return null;
        }
    }
}
