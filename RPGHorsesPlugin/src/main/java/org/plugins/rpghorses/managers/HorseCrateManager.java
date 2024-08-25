package org.plugins.rpghorses.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.crates.HorseCrate;
import org.plugins.rpghorses.horseinfo.AbstractHorseInfo;
import org.plugins.rpghorses.horseinfo.HorseInfo;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.utils.ItemUtil;

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
			this.horseCrates.add(new HorseCrate(crateName, config.getConfigurationSection("horse-crates." + crateName)));
		}
		
		this.defaultHorseCrate = getHorseCrate(config.getString("horse-options.default-horse"));
	}
	
	public HorseCrate getHorseCrate(String name) {
		if (name == null || name.isEmpty()) return null;

		for (HorseCrate horseCrate : this.horseCrates) {
			if (horseCrate.getName().equalsIgnoreCase(name)) {
				return horseCrate;
			}
		}
		return null;
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
