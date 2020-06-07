package org.plugins.rpghorses.horses;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horseinfo.AbstractHorseInfo;
import org.plugins.rpghorses.horseinfo.HorseInfo;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.players.HorseOwner;

import java.util.Random;

public class HorseCrate {
	
	private double price;
	private String name;
	private double minHealth, maxHealth, minMovementSpeed, maxMovementSpeed, minJumpStrength, maxJumpStrength;
	private AbstractHorseInfo horseInfo;
	private int tier;
	
	public HorseCrate(String name, double price, double minHealth, double maxHealth, double minMovementSpeed, double maxMovementSpeed, double minJumpStrength, double maxJumpStrength, AbstractHorseInfo horseInfo, int tier) {
		this.name = name;
		this.price = price;
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
	
	public double getPrice() {
		return price;
	}
	
	public String getName() {
		return name;
	}
}
