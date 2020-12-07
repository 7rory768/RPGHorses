package org.plugins.rpghorses.crates;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horseinfo.AbstractHorseInfo;
import org.plugins.rpghorses.horseinfo.HorseInfo;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.players.HorseOwner;
import roryslibrary.util.ItemUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class HorseCrate {
	
	private String name;
	private double price;
	private Set<ItemStack> itemsNeeded;
	private double minHealth, maxHealth, minMovementSpeed, maxMovementSpeed, minJumpStrength, maxJumpStrength;
	private AbstractHorseInfo horseInfo;
	private int tier;
	
	public HorseCrate(String name, double price, Set<ItemStack> itemsNeeded, double minHealth, double maxHealth, double minMovementSpeed, double maxMovementSpeed, double minJumpStrength, double maxJumpStrength, AbstractHorseInfo horseInfo, int tier) {
		this.name = name;
		this.price = price;
		this.itemsNeeded = itemsNeeded;
		this.minHealth = minHealth;
		this.maxHealth = maxHealth;
		this.minMovementSpeed = minMovementSpeed;
		this.maxMovementSpeed = maxMovementSpeed;
		this.minJumpStrength = minJumpStrength;
		this.maxJumpStrength = maxJumpStrength;
		this.horseInfo = horseInfo;
		this.tier = tier;
	}
	
	public RPGHorse getRPGHorse(HorseOwner horseOwner) {
		Random random = new Random();
		double health = random.nextDouble() * (this.maxHealth - this.minHealth) + this.minHealth;
		double movementSpeed = random.nextDouble() * (this.maxMovementSpeed - this.minMovementSpeed) + this.minMovementSpeed;
		double jumpStrength = random.nextDouble() * (this.maxJumpStrength - this.minJumpStrength) + this.minJumpStrength;
		AbstractHorseInfo abstractHorseInfo;
		Horse.Color color = horseInfo.getColor() == null ? Horse.Color.values()[random.nextInt(Horse.Color.values().length)] : horseInfo.getColor();
		Horse.Style style = horseInfo.getStyle() == null ? Horse.Style.values()[random.nextInt(Horse.Style.values().length)] : horseInfo.getStyle();
		
		if (RPGHorsesMain.getVersion().getWeight() < 11) {
			Horse.Variant variant = ((LegacyHorseInfo) horseInfo).getVariant();
			if (variant == null) {
				String[] variants = RPGHorsesMain.getInstance().getRpgHorseManager().getVariantTypesList().split(", ");
				variant = Horse.Variant.valueOf(variants[random.nextInt(variants.length)]);
			}
			abstractHorseInfo = new LegacyHorseInfo(style, color, variant);
		} else {
			EntityType type = horseInfo.getEntityType();
			if (type == null) {
				RPGHorseManager rpgHorseManager = RPGHorsesMain.getInstance().getRpgHorseManager();
				if (rpgHorseManager != null) {
					String[] entityTypes = rpgHorseManager.getEntityTypesList().replace("LLAMA", "HORSE").split(", ");
					type = EntityType.valueOf(entityTypes[random.nextInt(entityTypes.length)]);
				} else {
					type = EntityType.HORSE;
				}
			}
			abstractHorseInfo = new HorseInfo(type, style, color);
		}
		return new RPGHorse(horseOwner, this.tier, 0, null, health, movementSpeed, jumpStrength, abstractHorseInfo, false, null);
	}
	
	public String getName() {
		return name;
	}
	
	public double getPrice() {
		return price;
	}
	
	public Set<ItemStack> getItemsNeeded() {
		return itemsNeeded;
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
