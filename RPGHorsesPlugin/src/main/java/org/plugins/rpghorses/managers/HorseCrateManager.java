package org.plugins.rpghorses.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.crates.HorseCrate;
import org.plugins.rpghorses.horseinfo.AbstractHorseInfo;
import org.plugins.rpghorses.horseinfo.HorseInfo;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import rorys.library.util.ItemUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class HorseCrateManager {
	
	private final RPGHorsesMain plugin;
	
	private HashSet<HorseCrate> horseCrates = new HashSet<>();
	private HorseCrate defaultHorseCrate;
	
	public HorseCrateManager(RPGHorsesMain plugin) {
		this.plugin = plugin;
		
		this.loadHorseCrates();
	}
	
	public void loadHorseCrates() {
		this.horseCrates.clear();
		FileConfiguration config = this.plugin.getConfig();
		for (String crateName : config.getConfigurationSection("horse-crates").getKeys(false)) {
			String path = "horse-crates." + crateName + ".horse-info.";
			double[] healthValues = this.getMinAndMaxValues(config.getString(path + "health"));
			double minHealth = healthValues[0];
			double maxHealth = healthValues[1];
			double[] movementSpeedValues = this.getMinAndMaxValues(config.getString(path + "movement-speed"));
			double minMovementSpeed = movementSpeedValues[0];
			double maxMovementSpeed = movementSpeedValues[1];
			double[] jumpStrengthValues = this.getMinAndMaxValues(config.getString(path + "jump-strength"));
			double minJumpStrength = jumpStrengthValues[0];
			double maxJumpStrength = jumpStrengthValues[1];
			int tier = config.getInt(path + "tier");
			EntityType entityType = EntityType.HORSE;
			Horse.Variant variant = Horse.Variant.HORSE;
			Horse.Color color = Horse.Color.BROWN;
			Horse.Style style = Horse.Style.NONE;
			try {
				color = Horse.Color.valueOf(config.getString(path + "color", "BROWN"));
			} catch (IllegalArgumentException e) {
				if (!config.getString(path + "color").equalsIgnoreCase("RANDOM")) {
					Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + crateName + " ( " + config.getString(path + "color") + " is not a valid color )");
				} else {
					color = null;
				}
			}
			try {
				style = Horse.Style.valueOf(config.getString(path + "style", "NONE"));
			} catch (IllegalArgumentException e) {
				if (!config.getString(path + "style").equalsIgnoreCase("RANDOM")) {
					Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + crateName + " ( " + config.getString(path + "style") + " is not a valid style )");
				} else {
					style = null;
				}
			}
			
			AbstractHorseInfo horseInfo;
			
			if (plugin.getVersion().getWeight() < 11) {
				try {
					variant = Horse.Variant.valueOf(config.getString(path + "type", "HORSE"));
				} catch (IllegalArgumentException e) {
					if (!config.getString(path + "type").equalsIgnoreCase("RANDOM")) {
						Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + crateName + "( " + config.getString(path + "type") + " is not a valid variant )");
					} else {
						variant = null;
					}
				}
				horseInfo = new LegacyHorseInfo(style, color, variant);
			} else {
				try {
					entityType = EntityType.valueOf(config.getString(path + "type", "HORSE"));
				} catch (IllegalArgumentException e) {
					if (!config.getString(path + "type").equalsIgnoreCase("RANDOM")) {
						Bukkit.getLogger().log(Level.SEVERE, "[RPGHorses] Failed to load " + crateName + "( " + config.getString(path + "type") + " is not a valid entityType )");
					} else {
						entityType = null;
					}
				}
				horseInfo = new HorseInfo(entityType, style, color);
			}
			
			path = "horse-crates." + crateName + ".";
			
			double price = config.getDouble(path + "price");
			
			Set<ItemStack> itemsNeeded = new HashSet<>();
			
			if (config.isSet(path + "items-needed") && config.getConfigurationSection(path + "items-needed") != null) {
				for (String itemName : config.getConfigurationSection(path + "items-needed").getKeys(false)) {
					itemsNeeded.add(ItemUtil.getItemStack(config, path + "items-needed." + itemName));
				}
			}
			
			HorseCrate horseCrate = new HorseCrate(crateName, price, itemsNeeded, minHealth, maxHealth, minMovementSpeed, maxMovementSpeed, minJumpStrength, maxJumpStrength, horseInfo, tier);
			this.horseCrates.add(horseCrate);
		}
		
		this.defaultHorseCrate = getHorseCrate(config.getString("horse-options.default-horse"));
	}
	
	public HorseCrate getHorseCrate(String name) {
		for (HorseCrate horseCrate : this.horseCrates) {
			if (horseCrate.getName().equalsIgnoreCase(name)) {
				return horseCrate;
			}
		}
		return null;
	}
	
	public double[] getMinAndMaxValues(String info) {
		double[] values = new double[2];
		values[0] = 0;
		values[1] = 0;
		int index = info.indexOf("-");
		if (index > -1) {
			double min = Double.parseDouble(info.substring(0, index).trim());
			values[0] = min;
			double max = Double.parseDouble(info.substring(index + 1).trim());
			values[1] = max;
		}
		return values;
	}
	
	public HorseCrate getDefaultHorseCrate() {
		return defaultHorseCrate;
	}
	
	public String getHorseCrateList() {
		String list = "";
		for (HorseCrate horseCrate : this.horseCrates) {
			list += horseCrate.getName() + ", ";
		}
		return list.substring(0, list.length() - 2);
	}
	
}
