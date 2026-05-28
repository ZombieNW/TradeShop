package com.zombienw.tradeshop.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;

public class FormatUtils {

    private FormatUtils() {}

    // component -> string
    public static String serialize(Component component) {
        if (component == null) return "";
        return PlainTextComponentSerializer.plainText().serialize(component).trim();
    }

    // format "N Item"
    public static Component formatSignLine(int amount, Material material) {
        if (material == null) {
            return Component.text(amount + " " + com.zombienw.tradeshop.Constants.HAND_PLACEHOLDER, NamedTextColor.GOLD);
        }
        return Component.text(amount + " " + formatName(material));
    }

    // DIAMOND_SWORD -> "Diamond Sword"
    public static String formatName(Material material) {
        String[] words = material.name().replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            String formatted = words.length > 1 ? abbreviateWord(word) : capitalizeFirst(word);
            result.append(formatted).append(" ");
        }

        return result.toString().trim();
    }

    // diamond -> Diamond
    private static String capitalizeFirst(String word) {
        if (word.isEmpty()) return word;
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    // shorten a few words for the sign
    private static String abbreviateWord(String word) {
        return switch (word) {
            case "diamond" -> "Dia.";
            case "netherite" -> "Neth.";
            case "golden" -> "Gld.";
            case "wooden" -> "Wdn.";
            case "chainmail" -> "Chn.";
            case "cooked" -> "Ckd.";
            case "baked" -> "Bkd.";
            case "enchanted" -> "Ench.";
            case "suspicious" -> "Sus.";
            case "porkchop" -> "Pork";
            default -> capitalizeFirst(word);
        };
    }
}
