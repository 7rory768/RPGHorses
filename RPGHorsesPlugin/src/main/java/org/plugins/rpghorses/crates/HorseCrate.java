package org.plugins.rpghorses.crates;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.horseinfo.AbstractHorseInfo;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.tiers.Tier;
import org.plugins.rpghorses.utils.ItemUtil;

import java.math.BigDecimal;
import java.util.*;

@Getter
public class HorseCrate {

	private final String name;
	private final double price;
	private final Set<ItemStack> itemsNeeded;
	private final double minHealth, maxHealth, minMovementSpeed, maxMovementSpeed, minJumpStrength, maxJumpStrength;
	private final AbstractHorseInfo horseInfo;
	private final int tier;
	private final List<Tier> upgradeTiers = new ArrayList<>();

	public HorseCrate(String name, ConfigurationSection config) {
		this.name = name;

		this.price = config.getDouble("price");

		this.itemsNeeded = new HashSet<>();

		if (config.isSet("items-needed") && config.getConfigurationSection("items-needed") != null) {
			for (String itemName : config.getConfigurationSection("items-needed").getKeys(false)) {
				itemsNeeded.add(ItemUtil.getItemStack(config, "items-needed." + itemName));
			}
		}

		upgradeTiers.clear();

		ConfigurationSection upgradeTiersConfig = config.getConfigurationSection("upgrade-tiers");
		if (upgradeTiersConfig != null) {
			for (String tierStr : upgradeTiersConfig.getKeys(false)) {
				if (!upgradeTiersConfig.isSet(tierStr)) continue;

				Tier tier = new Tier(config.getConfigurationSection("upgrade-tiers." + tierStr), Integer.parseInt(tierStr));
				upgradeTiers.add(tier);
			}
		}

		config = config.getConfigurationSection("horse-info");

		double[] healthValues = this.getMinAndMaxValues(config.getString("health"));
		this.minHealth = healthValues[0];
		this.maxHealth = healthValues[1];
		double[] movementSpeedValues = this.getMinAndMaxValues(config.getString("movement-speed"));
		this.minMovementSpeed = movementSpeedValues[0];
		this.maxMovementSpeed = movementSpeedValues[1];
		double[] jumpStrengthValues = this.getMinAndMaxValues(config.getString("jump-strength"));
		this.minJumpStrength = jumpStrengthValues[0];
		this.maxJumpStrength = jumpStrengthValues[1];
		this.tier = config.getInt("tier");
		this.horseInfo = AbstractHorseInfo.loadFromConfig(config);
	}

	private double[] getMinAndMaxValues(String info) {
		double[] values = new double[2];
		values[0] = 0;
		values[1] = 0;
		if (info == null) return values;
		int index = info.indexOf("-");
		if (index > -1) {
			double min = Double.parseDouble(info.substring(0, index).trim());
			values[0] = min;
			double max = Double.parseDouble(info.substring(index + 1).trim());
			values[1] = max;
		}
		return values;
	}

	public RPGHorse getRPGHorse(HorseOwner horseOwner) {
		Random random = new Random();
		double health = random.nextDouble() * (this.maxHealth - this.minHealth) + this.minHealth;
		double movementSpeed = random.nextDouble() * (this.maxMovementSpeed - this.minMovementSpeed) + this.minMovementSpeed;
		double jumpStrength = random.nextDouble() * (this.maxJumpStrength - this.minJumpStrength) + this.minJumpStrength;

		AbstractHorseInfo abstractHorseInfo = horseInfo.populateNewRandomInfo();

		return new RPGHorse(horseOwner, this.name, this.tier, 0, null, health, health, movementSpeed, jumpStrength, abstractHorseInfo, false, null);
	}

	public Map<ItemStack, Integer> getMissingItems(Player p) {
		Inventory inv = p.getInventory();
		Set<ItemStack> itemsNeeded = this.getItemsNeeded();
		Map<ItemStack, Integer> amountMissing = new HashMap<>();

		for (ItemStack itemNeeded : itemsNeeded) {
			int amount = 0, amountNeeded = itemNeeded.getAmount();
			for (ItemStack item : inv.getContents()) {
				if (ItemUtil.itemIsReal(item) && item.isSimilar(itemNeeded)) {
					amount += item.getAmount();
				}
			}

			if (amount < amountNeeded) amountMissing.put(itemNeeded, amountNeeded - amount);
		}

		return amountMissing;
	}
}
