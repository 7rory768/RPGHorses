package org.plugins.rpghorses.tiers;

import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class Tier {
	
	private final int tier, successChance;
	private final double healthMultiplier, movementSpeedMultiplier, jumpStrengthMultiplier, cost, expCost;
	private final Set<ItemStack> itemsNeeded;
	
	public Tier(int tier, int successChance, double healthMultiplier, double movementSpeedMultiplier, double jumpStrengthMultiplier, double cost, double expCost, Set<ItemStack> itemsNeeded) {
		this.tier = tier;
		this.successChance = successChance;
		this.healthMultiplier = healthMultiplier;
		this.movementSpeedMultiplier = movementSpeedMultiplier;
		this.jumpStrengthMultiplier = jumpStrengthMultiplier;
		this.cost = cost;
		this.expCost = expCost;
		this.itemsNeeded = itemsNeeded;
	}
	
	public int getTier() {
		return tier;
	}
	
	public int getSuccessChance() {
		return successChance;
	}
	
	public double getHealthMultiplier() {
		return healthMultiplier;
	}
	
	public double getMovementSpeedMultiplier() {
		return movementSpeedMultiplier;
	}
	
	public double getJumpStrengthMultiplier() {
		return jumpStrengthMultiplier;
	}
	
	public double getCost() {
		return cost;
	}
	
	public double getExpCost() {
		return expCost;
	}
	
	public Set<ItemStack> getItemsNeeded() {
		return itemsNeeded;
	}
}
