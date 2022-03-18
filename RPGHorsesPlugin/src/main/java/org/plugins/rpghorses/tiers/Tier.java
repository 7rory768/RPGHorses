package org.plugins.rpghorses.tiers;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

@Getter
public class Tier
{

	private final int tier, successChance;
	private final double healthMultiplier, movementSpeedMultiplier, jumpStrengthMultiplier, cost, expCost;
	private final Set<ItemStack> itemsNeeded;
	private final List<String>   commands;

	public Tier(int tier, int successChance, double healthMultiplier, double movementSpeedMultiplier, double jumpStrengthMultiplier, double cost, double expCost, Set<ItemStack> itemsNeeded, List<String> commands)
	{
		this.tier = tier;
		this.successChance = successChance;
		this.healthMultiplier = healthMultiplier;
		this.movementSpeedMultiplier = movementSpeedMultiplier;
		this.jumpStrengthMultiplier = jumpStrengthMultiplier;
		this.cost = cost;
		this.expCost = expCost;
		this.itemsNeeded = itemsNeeded;
		this.commands = commands;
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
