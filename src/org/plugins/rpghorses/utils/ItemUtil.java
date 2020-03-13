package org.plugins.rpghorses.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Rory on 6/30/2017.
 */
public class ItemUtil {

    private JavaPlugin plugin;
    private MessagingUtil messagingUtil;

    public ItemUtil(JavaPlugin plugin, MessagingUtil messagingUtil) {
        this.plugin = plugin;
        this.messagingUtil = messagingUtil;
    }

    public ItemStack getItemStack(String path) {
        ItemStack item = null;
        if (!path.endsWith(".")) {
            path += ".";
        }
        try {
            Material mat = Material.valueOf(this.plugin.getConfig().getString(path + "material", "NULL"));
            int amount = this.plugin.getConfig().getInt(path + "amount", 1);
            item = new ItemStack(mat, amount);
            short data = (short) this.plugin.getConfig().getInt(path + "data", item.getDurability());
            item.setDurability(data);
            ItemMeta itemMeta = item.getItemMeta();
            String name = this.plugin.getConfig().getString(path + "name", "");
            if (!name.equals("")) {
                itemMeta.setDisplayName(this.messagingUtil.placeholders(name));
            }

            List<String> loreLines = this.plugin.getConfig().getStringList(path + "lore");
            if (!loreLines.isEmpty()) {
                List<String> lore = new ArrayList<String>();
                for (String loreLine : loreLines) {
                    lore.add(this.messagingUtil.placeholders(loreLine));
                }
                itemMeta.setLore(lore);
            }

            List<String> enchants = this.plugin.getConfig().getStringList(path + "enchants");
            for (String enchantInfo : enchants) {
                int colonIndex = enchantInfo.indexOf(":");
                Enchantment enchantment = Enchantment.getByName(enchantInfo.substring(0, colonIndex));
                if (enchantment != null) {
                    int level = Integer.parseInt(enchantInfo.substring(colonIndex + 1, enchantInfo.length()));
                    itemMeta.addEnchant(enchantment, level, true);
                }
            }

            List<String> itemFlags = this.plugin.getConfig().getStringList(path + "item-flags");
            for (String itemFlag : itemFlags) {
                itemMeta.addItemFlags(ItemFlag.valueOf(itemFlag));
            }

            if (mat == Material.SKULL_ITEM && this.plugin.getConfig().isSet(path + "skin-value")) {
                item.setDurability((short) 3);
                ItemMeta oldItemMeta = itemMeta;
                itemMeta = this.applyCustomHead(itemMeta, this.plugin.getConfig().getString(path + "skin-value"));
                if (itemMeta == null) {
                    itemMeta = oldItemMeta;
                    this.plugin.getLogger().info("[" + this.plugin.getName() + "] Failed to load skull skin-value @ " + path);
                }
            }

            item.setItemMeta(itemMeta);

            return item;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public SkullMeta applyCustomHead(ItemMeta itemMeta, String value) {
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), null);
        gameProfile.getProperties().put("textures", new Property("textures", value));

        SkullMeta skullMeta = (SkullMeta) itemMeta;
        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, gameProfile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException exception) {
            return null;
        }

        return skullMeta;
    }

    public int getSlot(String path) {
        int xCord = this.plugin.getConfig().getInt(path + ".x-cord");
        int yCord = this.plugin.getConfig().getInt(path + ".y-cord");
        return ItemUtil.getSlot(xCord, yCord);
    }

    public static int getSlot(int xCord, int yCord) {
        return ((yCord - 1) * 9) + xCord - 1;
    }

    public static int getX(int slot) {
        return slot % 9 + 1;
    }

    public static int getY(int slot) {
        return slot / 9 + 1;
    }

    public static ItemStack getItemStack(FileConfiguration config, String path) {
        ItemStack item = null;
        if (!path.endsWith(".")) {
            path += ".";
        }
        try {
            Material mat = Material.valueOf(config.getString(path + "material", "NULL"));
            int amount = config.getInt(path + "amount", 1);
            item = new ItemStack(mat, amount);
            short data = (short) config.getInt(path + "data", item.getDurability());
            item.setDurability(data);
            ItemMeta itemMeta = item.getItemMeta();
            String name = config.getString(path + "name", "");
            if (!name.equals("")) {
                itemMeta.setDisplayName(MessagingUtil.format(name));
            }

            List<String> loreLines = config.getStringList(path + "lore");
            if (!loreLines.isEmpty()) {
                List<String> lore = new ArrayList<String>();
                for (String loreLine : loreLines) {
                    lore.add(MessagingUtil.format(loreLine));
                }
                itemMeta.setLore(lore);
            }

            List<String> enchants = config.getStringList(path + "enchants");
            for (String enchantInfo : enchants) {
                int colonIndex = enchantInfo.indexOf(":");
                String enchantName = enchantInfo.substring(0, colonIndex);
                int level = Integer.parseInt(enchantInfo.substring(colonIndex + 1, enchantInfo.length()));
                itemMeta.addEnchant(Enchantment.getByName(enchantName), level, true);
            }

            List<String> itemFlags = config.getStringList(path + "item-flags");
            for (String itemFlag : itemFlags) {
                itemMeta.addItemFlags(ItemFlag.valueOf(itemFlag));
            }

            String skullOwner = config.getString(path + "skull-owner", "");
            if (mat == Material.SKULL_ITEM && data == 3 && !skullOwner.equals("")) {
                SkullMeta skullMeta = (SkullMeta) itemMeta;
                skullMeta.setOwner(skullOwner);
                item.setItemMeta(skullMeta);
            } else {
                item.setItemMeta(itemMeta);
            }
            return item;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void addDurabilityGlow(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.addEnchant(Enchantment.DURABILITY, 3, true);
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(itemMeta);
    }

    public static void storeItemStack(ItemStack item, FileConfiguration config, String path) {
        if (!path.endsWith(".")) {
            path += ".";
        }
        config.set(path + "material", item.getType().name());
        config.set(path + "amount", item.getAmount());
        config.set(path + "data", item.getDurability());

        ItemMeta itemMeta = item.getItemMeta();

        if (itemMeta.hasDisplayName()) {
            config.set(path + "name", itemMeta.getDisplayName());
        }

        if (itemMeta.hasLore()) {
            config.set(path + "lore", itemMeta.getLore());
        }

        if (itemMeta.hasEnchants()) {
            List<String> enchants = new ArrayList<String>();
            for (Map.Entry<Enchantment, Integer> enchant : itemMeta.getEnchants().entrySet()) {
                enchants.add(enchant.getKey().getName() + ":" + enchant.getValue());
                config.set(path + "enchants", enchants);

            }
        }

        List<String> itemFlags = new ArrayList<>();
        for (ItemFlag itemFlag : itemMeta.getItemFlags()) {
            itemFlags.add(itemFlag.name());
        }
        if (!itemFlags.isEmpty()) {
            config.set(path + "item-flags", itemFlags);
        }

        if (item.getType() == Material.SKULL_ITEM && item.getDurability() == 3) {
            SkullMeta skullMeta = (SkullMeta) itemMeta;
            config.set(path + "skull-owner", skullMeta.getOwner());
        }
    }

}
