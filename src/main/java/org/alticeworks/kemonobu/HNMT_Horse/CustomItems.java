package org.alticeworks.kemonobu.HNMT_Horse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class CustomItems {

    public static NamespacedKey keyWhip;
    public static NamespacedKey keyEmptyCan;
    public static NamespacedKey keyHorseCanned;

    public static void register(HNMT_Horse plugin) {
        keyWhip = new NamespacedKey(plugin, "item_whip");
        keyEmptyCan = new NamespacedKey(plugin, "item_empty_can");
        keyHorseCanned = new NamespacedKey(plugin, "item_horse_canned");

        registerWhipRecipe(plugin);
        registerEmptyCanRecipe(plugin);
        registerHorseCannedRecipe(plugin);
    }

    // 鞭の生成
    public static ItemStack createWhip() {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "馬の鞭");
            meta.setLore(List.of(
                    ChatColor.GRAY + "馬に乗って使うとスピードアップ！",
                    ChatColor.RED + "消費: スタミナ 10 / 体力 0.5",
                    ChatColor.YELLOW + "効果時間は重ねがけ可能"
            ));
            meta.getPersistentDataContainer().set(keyWhip, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    // 空き缶の生成
    public static ItemStack createEmptyCan() {
        ItemStack item = new ItemStack(Material.FLOWER_POT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "空き缶");
            meta.setLore(List.of(ChatColor.GRAY + "ホースカンズメの材料"));
            meta.getPersistentDataContainer().set(keyEmptyCan, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ホースカンズメの生成
    public static ItemStack createHorseCanned() {
        ItemStack item = new ItemStack(Material.BEETROOT_SOUP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "ホースカンズメ");
            meta.setLore(List.of(
                    ChatColor.GRAY + "馬にスニーク右クリックで与える",
                    ChatColor.AQUA + "効果: スタミナ全回復"
            ));
            meta.getPersistentDataContainer().set(keyHorseCanned, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    // レシピ登録: 鞭
    private static void registerWhipRecipe(HNMT_Horse plugin) {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "recipe_whip");
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createWhip());
        recipe.shape("  C", " C ", "C  ");
        recipe.setIngredient('C', Material.CHAIN);
        Bukkit.addRecipe(recipe);
    }

    // レシピ登録: 空き缶
    private static void registerEmptyCanRecipe(HNMT_Horse plugin) {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "recipe_empty_can");
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createEmptyCan());
        recipe.shape("I I", "I I", "III");
        recipe.setIngredient('I', Material.IRON_INGOT);
        Bukkit.addRecipe(recipe);
    }

    // レシピ登録: ホースカンズメ
    private static void registerHorseCannedRecipe(HNMT_Horse plugin) {
        NamespacedKey recipeKey = new NamespacedKey(plugin, "recipe_horse_canned");
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createHorseCanned());
        recipe.shape("WGW", "CCC", " E ");
        recipe.setIngredient('W', Material.WHEAT);
        recipe.setIngredient('G', Material.SHORT_GRASS); // 1.21では SHORT_GRASS
        recipe.setIngredient('C', Material.CARROT);
        recipe.setIngredient('E', Material.FLOWER_POT); // 空き缶のベース
        Bukkit.addRecipe(recipe);
    }
}