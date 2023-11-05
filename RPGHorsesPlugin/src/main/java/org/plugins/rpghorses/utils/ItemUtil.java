package org.plugins.rpghorses.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import roryslibrary.guis.GUIItem;
import roryslibrary.util.MessagingUtil;
import roryslibrary.util.Version;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/*
 * @author Rory Skipper (Roree) on 2023-10-27
 */
public class ItemUtil extends roryslibrary.util.ItemUtil {

	private final JavaPlugin plugin;

	public ItemUtil(JavaPlugin plugin) {
		super(plugin);
		this.plugin = plugin;
	}

	public GUIItem getGUIItem(String path) {
		path = path.substring(path.startsWith(".") ? 1 : 0, path.length() - (path.endsWith(".") ? 1 : 0));
		return getGUIItem(this.plugin.getConfig().getConfigurationSection(path));
	}

	public static GUIItem getGUIItem(JavaPlugin plugin, String path) {
		path = path.substring(path.startsWith(".") ? 1 : 0, path.length() - (path.endsWith(".") ? 1 : 0));
		return getGUIItem(plugin.getConfig().getConfigurationSection(path));
	}

	public static GUIItem getGUIItem(FileConfiguration config, String path) {
		path = path.substring(path.startsWith(".") ? 1 : 0, path.length() - (path.endsWith(".") ? 1 : 0));
		return getGUIItem(config.getConfigurationSection(path));
	}

	public static GUIItem getGUIItem(ConfigurationSection section, String path) {
		path = path.substring(path.startsWith(".") ? 1 : 0, path.length() - (path.endsWith(".") ? 1 : 0));
		return getGUIItem(section.getConfigurationSection(path));
	}

	public static GUIItem getGUIItem(ConfigurationSection section) {
		return new GUIItem(getSlot(section), getItemStack(section));
	}

	public ItemStack getItemStack(String path) {
		return getItemStack(plugin.getConfig(), path);
	}

	public static ItemStack getItemStack(JavaPlugin plugin, String path) {
		return getItemStack(plugin.getConfig(), path);
	}

	public static ItemStack getItemStack(FileConfiguration config, String path) {
		if (path.endsWith(".")) {
			path = path.substring(0, path.length() - 1);
		}

		return getItemStack(config.getConfigurationSection(path));
	}

	public static ItemStack getItemStack(ConfigurationSection section, String path) {
		path = path.substring(path.startsWith(".") ? 1 : 0, path.length() - (path.endsWith(".") ? 1 : 0));
		return getItemStack(section.getConfigurationSection(path));
	}

	public static ItemStack getItemStack(ConfigurationSection section) {
		String matString = section.getString("material", "NULL").toUpperCase();

		Material mat;
		try {
			mat = Material.valueOf(matString);
		} catch (IllegalArgumentException var15) {
			if (matString.equals("PLAYER_HEAD")) {
				mat = Material.valueOf("SKULL_ITEM");
			} else {
				if (!matString.equalsIgnoreCase("GRAY_STAINED_GLASS_PANE")) {
					Bukkit.getLogger().info("[ItemUtil] Invalid material '" + matString + "' @ " + section.getCurrentPath());
					return null;
				}

				mat = Material.valueOf("STAINED_GLASS_PANE");
			}
		}

		int amount = section.getInt("amount", 1);
		ItemStack item = new ItemStack(mat, amount);
		short data = (short) section.getInt("data", item.getDurability());
		if (matString.equalsIgnoreCase("GRAY_STAINED_GLASS_PANE")) {
			data = 7;
		}

		item.setDurability(data);
		ItemMeta itemMeta = item.getItemMeta();
		String name = section.getString("name", "");
		if (!name.equals("")) {
			itemMeta.setDisplayName(MessagingUtil.format(name, new String[0]));
		}

		List<String> loreLines = section.getStringList("lore");
		Iterator var10;
		String enchantInfo;
		if (!loreLines.isEmpty()) {
			List<String> lore = new ArrayList();
			var10 = loreLines.iterator();

			while (var10.hasNext()) {
				enchantInfo = (String) var10.next();
				lore.add(MessagingUtil.format(enchantInfo, new String[0]));
			}

			itemMeta.setLore(lore);
		}

		List<String> enchants = section.getStringList("enchants");
		var10 = enchants.iterator();

		while (var10.hasNext()) {
			enchantInfo = (String) var10.next();
			int colonIndex = enchantInfo.indexOf(":");
			Enchantment enchantment = Enchantment.getByName(enchantInfo.substring(0, colonIndex));
			if (enchantment != null) {
				int level = Integer.parseInt(enchantInfo.substring(colonIndex + 1, enchantInfo.length()));
				itemMeta.addEnchant(enchantment, level, true);
			}
		}

		List<String> itemFlags = section.getStringList("item-flags");
		Iterator var18 = itemFlags.iterator();

		String pluginName;
		while (var18.hasNext()) {
			pluginName = (String) var18.next();
			itemMeta.addItemFlags(new ItemFlag[]{ItemFlag.valueOf(pluginName)});
		}

		if (mat.name().equals("PLAYER_HEAD") || mat.name().equals("SKULL_ITEM")) {
			if (mat.name().equals("SKULL_ITEM") && !section.isSet("data")) {
				item.setDurability((short) 3);
			}

			if (section.isSet("skin-value")) {
				ItemMeta oldItemMeta = itemMeta;
				itemMeta = applyCustomHead(itemMeta, section.getString("skin-value"));
				if (itemMeta == null) {
					itemMeta = oldItemMeta;
					pluginName = ChatColor.stripColor(MessagingUtil.format(section.getString("prefix", "ItemUtil").split("\\s")[0], new String[0]));
					Bukkit.getLogger().info("[" + pluginName + "] Failed to load skull skin-value @ " + section.getCurrentPath());
				}
			}
		}

		if ((mat == Material.LEATHER_BOOTS || mat == Material.LEATHER_CHESTPLATE || mat == Material.LEATHER_HELMET || mat == Material.LEATHER_LEGGINGS) && section.isSet("color")) {
			LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) itemMeta;
			leatherArmorMeta.setColor(Color.fromRGB(section.getInt("color")));
			itemMeta = leatherArmorMeta;
		}

		if (section.isSet("custom-model-data") && Version.isRunningMinimum(Version.v1_14)) {
			itemMeta.setCustomModelData(section.getInt("custom-model-data"));
		}

		item.setItemMeta(itemMeta);
		return item;
	}

	public static SkullMeta applyCustomHead(ItemMeta itemMeta, String value) {
		try {
			if (value.length() <= 16) {
				SkullMeta skullMeta = (SkullMeta) itemMeta;
				skullMeta.setOwner(value);
				return skullMeta;
			}

			UUID uuid = UUID.fromString(value);
			if (uuid != null) {
				return applyCustomHead(itemMeta, uuid);
			}
		} catch (Exception var9) {
		}

		boolean isURL = false;
		URL url = null;

		try {
			url = new URL(value);
			isURL = true;
		} catch (MalformedURLException var8) {
		}

		SkullMeta skullMeta = (SkullMeta) itemMeta;
		if (isURL && value.contains("textures.minecraft.net")) {
			PlayerProfile playerProfile = Bukkit.getServer().createPlayerProfile(UUID.randomUUID(), "");
			playerProfile.getTextures().setSkin(url);
			playerProfile.getTextures().setCape(url);
			skullMeta.setOwnerProfile(playerProfile);
		} else {
			/*if (Version.getVersion().getWeight() >= Version.v1_20.getWeight()) {
				Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to apply legacy skull skin-value " + value + " (1.20+)", new IllegalArgumentException());
				return skullMeta;
			}*/

			try {
				GameProfile gameProfile = new GameProfile(UUID.randomUUID(), value);
				gameProfile.getProperties().put("textures", new Property("textures", value));

				Field profileField = skullMeta.getClass().getDeclaredField("profile");
				profileField.setAccessible(true);
				profileField.set(skullMeta, gameProfile);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException var7) {
				return null;
			}
		}

		return skullMeta;
	}


}
