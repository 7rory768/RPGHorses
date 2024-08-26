package org.plugins.rpghorses.tiers;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.horseinfo.AbstractHorseInfo;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.utils.ItemUtil;
import org.plugins.rpghorses.version.Version;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class Tier {

	private final int tier, successChance;
	private final double healthMultiplier, movementSpeedMultiplier, jumpStrengthMultiplier;
	private final double health, movementSpeed, jumpStrength;
	private final double cost, expCost;
	private final Set<ItemStack> itemsNeeded;
	private final List<String> commands;
	private final AbstractHorseInfo horseInfo;

	public Tier(ConfigurationSection config, int tier) {
		this.tier = tier;
		successChance = config.getInt("success-chance", 100);
		healthMultiplier = config.getDouble("health-multiplier", -1);
		movementSpeedMultiplier = config.getDouble("movement-speed-multiplier", -1);
		jumpStrengthMultiplier = config.getDouble("jump-strength-multiplier", -1);
		health = config.getDouble("health", -1);
		movementSpeed = config.getDouble("movement-speed", -1);
		jumpStrength = config.getDouble("jump-strength", -1);
		cost = config.getDouble("cost", 0);
		expCost = config.getDouble("exp-cost", 0);
		commands = config.getStringList("commands");

		itemsNeeded = new HashSet<>();
		if (config.isSet("items-needed")) {
			for (String itemName : config.getConfigurationSection("items-needed").getKeys(false)) {
				itemsNeeded.add(ItemUtil.getItemStack(config, "items-needed." + itemName));
			}
		}

		config = config.getConfigurationSection("horse-info");
		if (config != null) horseInfo = AbstractHorseInfo.loadFromConfig(config, false);
		else horseInfo = null;
	}

	public double getNewHealth(double currentHealth) {
		if (healthMultiplier != -1) {
			return currentHealth * healthMultiplier;
		} else if (health != -1) {
			return health;
		}
		return currentHealth;
	}

	public double getNewMovementSpeed(double currentMovementSpeed) {
		if (movementSpeedMultiplier != -1) {
			return currentMovementSpeed * movementSpeedMultiplier;
		} else if (movementSpeed != -1) {
			return movementSpeed;
		}
		return currentMovementSpeed;
	}

	public double getNewJumpStrength(double currentJumpStrength) {
		if (jumpStrengthMultiplier != -1) {
			return currentJumpStrength * jumpStrengthMultiplier;
		} else if (jumpStrength != -1) {
			return jumpStrength;
		}
		return currentJumpStrength;
	}

	public void applyUpgrade(RPGHorse rpgHorse) {
		boolean wasMaxHealth = rpgHorse.getHorse() != null && rpgHorse.getHorse().isValid() && rpgHorse.getHorse().getHealth() >= rpgHorse.getMaxHealth();

		double health = getNewHealth(rpgHorse.getMaxHealth());
		double movementSpeed = getNewMovementSpeed(rpgHorse.getMovementSpeed());
		double jumpStrength = getNewJumpStrength(rpgHorse.getJumpStrength());

		rpgHorse.setMaxHealth(health);
		rpgHorse.setMovementSpeed(movementSpeed);
		rpgHorse.setJumpStrength(jumpStrength);

		if (wasMaxHealth) rpgHorse.setHealth(health);

		if (horseInfo != null) {
			AbstractHorseInfo currentInfo = rpgHorse.getHorseInfo();
			AbstractHorseInfo newInfo = horseInfo.populateNewRandomInfo();

			// Only change filled values
			if (newInfo.getEntityType() != null) currentInfo.setEntityType(newInfo.getEntityType());
			if (newInfo.getColor() != null) currentInfo.setColor(newInfo.getColor());
			if (newInfo.getStyle() != null) currentInfo.setStyle(newInfo.getStyle());

			if (Version.getVersion().getWeight() < 11) {
				LegacyHorseInfo legacyHorseInfo = (LegacyHorseInfo) currentInfo;
				LegacyHorseInfo newLegacyHorseInfo = (LegacyHorseInfo) newInfo;
				if (newLegacyHorseInfo.getVariant() != null)
					legacyHorseInfo.setVariant(newLegacyHorseInfo.getVariant());
			}

			rpgHorse.setHorseInfo(currentInfo);
		}

		rpgHorse.setTier(tier + 1);
	}

	public void runCommands(Player player) {
		ConsoleCommandSender console = Bukkit.getConsoleSender();
		Server server = Bukkit.getServer();

		for (String cmd : commands) {
			if (cmd.startsWith("CONSOLE:")) {
				server.dispatchCommand(console, cmd.substring(8).replace("{PLAYER}", player.getName()));
			} else if (cmd.startsWith("PLAYER:")) {
				player.performCommand(cmd.substring(7).replace("{PLAYER}", player.getName()));
			}
		}
	}
}
