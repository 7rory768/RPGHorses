package org.plugins.rpghorses.managers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.players.HorseRenamer;
import org.plugins.rpghorses.players.RemoveHorseConfirmation;

import java.util.*;

public class RPGHorseManager {
	
	private final RPGHorsesMain plugin;
	private final HorseOwnerManager horseOwnerManager;
	private final Economy economy;
	
	private final List<EntityType> validEntityTypes = new ArrayList<>();
	private HashMap<UUID, RemoveHorseConfirmation> removeConfirmations = new HashMap<>();
	private HashMap<UUID, HorseRenamer> horseRenamers = new HashMap<>();
	
	public RPGHorseManager(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, Economy economy) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;
		this.economy = economy;
		
		this.setupValidEntityTypes();
	}
	
	public int getMaxTier() {
		return this.plugin.getConfig().getConfigurationSection("horse-tiers").getKeys(false).size() + 1;
	}
	
	public RPGHorse getRPGHorse(Entity entity) {
		if (entity != null) {
			for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners()) {
				RPGHorse currentHorse = horseOwner.getCurrentHorse();
				if (currentHorse != null && currentHorse.getHorse().getEntityId() == entity.getEntityId()) {
					return currentHorse;
				}
			}
		}
		return null;
	}
	
	public double getUpgradeCost(RPGHorse rpgHorse) {
		return this.plugin.getConfig().getDouble("horse-tiers." + rpgHorse.getTier() + ".cost");
	}
	
	public double getSuccessChance(RPGHorse rpgHorse) {
		int tier = rpgHorse.getTier();
		if (tier < this.getMaxTier()) {
			return this.plugin.getConfig().getDouble("horse-tiers." + tier + ".success-chance");
		}
		return -1;
	}
	
	public double getXPNeededToUpgrade(RPGHorse rpgHorse) {
		return plugin.getConfig().getDouble("horse-tiers." + rpgHorse.getTier() + ".exp-cost");
	}
	
	public boolean upgradeHorse(Player p, RPGHorse rpgHorse) {
		int tier = rpgHorse.getTier();
		if (tier < this.getMaxTier()) {
			
			this.economy.withdrawPlayer(p, this.getUpgradeCost(rpgHorse));
			
			double success = this.getSuccessChance(rpgHorse);
			if ((new Random()).nextDouble() * 100 >= success) {
				return false;
			}
			
			double health = rpgHorse.getMaxHealth() * this.plugin.getConfig().getDouble("horse-tiers." + tier + ".health-multiplier");
			double movementSpeed = rpgHorse.getMovementSpeed() * this.plugin.getConfig().getDouble("horse-tiers." + tier + ".movement-speed-multiplier");
			double jumpStrength = rpgHorse.getMovementSpeed() * this.plugin.getConfig().getDouble("horse-tiers." + tier + ".jump-strength-multiplier");
			
			rpgHorse.setMaxHealth(health);
			rpgHorse.setMovementSpeed(movementSpeed);
			rpgHorse.setJumpStrength(jumpStrength);
			rpgHorse.setTier(tier + 1);
			return true;
		}
		return false;
	}
	
	public boolean isValidEntityType(String typeArg) {
		try {
			EntityType type = EntityType.valueOf(typeArg.toUpperCase());
			return this.isValidEntityType(type);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	public boolean isValidEntityType(EntityType entityType) {
		return this.validEntityTypes.contains(entityType);
	}
	
	public boolean isValidVariant(String variantArg) {
		try {
			Horse.Variant.valueOf(variantArg.toUpperCase());
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}
	
	public boolean isValidColor(String colorArg) {
		try {
			Horse.Color.valueOf(colorArg.toUpperCase());
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}
	
	public boolean isValidStyle(String styleArg) {
		try {
			Horse.Style.valueOf(styleArg.toUpperCase());
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}
	
	public void setupValidEntityTypes() {
		if (plugin.getVersion().getWeight() >= 11) {
			this.validEntityTypes.add(EntityType.DONKEY);
			this.validEntityTypes.add(EntityType.MULE);
			this.validEntityTypes.add(EntityType.SKELETON_HORSE);
			this.validEntityTypes.add(EntityType.ZOMBIE_HORSE);
			this.validEntityTypes.add(EntityType.LLAMA);
		}
		this.validEntityTypes.add(EntityType.HORSE);
	}
	
	public String getEntityTypesList() {
		String entityTypes = "";
		for (EntityType type : this.validEntityTypes) {
			entityTypes += type.name() + ", ";
		}
		return entityTypes.substring(0, entityTypes.length() - 2);
	}
	
	public String getVariantTypesList() {
		String variantTypes = "";
		for (Horse.Variant variant : Horse.Variant.values()) {
			variantTypes += variant.name() + ", ";
		}
		return variantTypes.substring(0, variantTypes.length() - 2);
	}
	
	public String getColorsList() {
		String validColors = "";
		for (Horse.Color variant : Horse.Color.values()) {
			validColors += variant.name() + ", ";
		}
		return validColors.substring(0, validColors.length() - 2);
	}
	
	public String getStylesList() {
		String validStyles = "";
		for (Horse.Style variant : Horse.Style.values()) {
			validStyles += variant.name() + ", ";
		}
		return validStyles.substring(0, validStyles.length() - 2);
	}
	
	public void updateHorse(RPGHorse rpgHorse) {
		HorseOwner horseOwner = rpgHorse.getHorseOwner();
		if (horseOwner.getCurrentHorse() == rpgHorse) {
			rpgHorse.spawnEntity();
		}
		
	}
	
	public void addRemoveConfirmation(Player p, RPGHorse rpgHorse) {
		RemoveHorseConfirmation horseConfirmation = this.getRemovingHorseConfirmation(p);
		if (horseConfirmation != null) {
			horseConfirmation.endTask();
		}
		this.removeConfirmations.put(p.getUniqueId(), new RemoveHorseConfirmation(this.plugin, this, this.horseOwnerManager.getHorseOwner(p), rpgHorse));
	}
	
	public void removeRemoveConfirmation(UUID uuid) {
		this.removeConfirmations.remove(uuid);
	}
	
	public boolean isRemovingRPGHorse(Player p) {
		return this.removeConfirmations.containsKey(p.getUniqueId());
	}
	
	public RemoveHorseConfirmation getRemovingHorseConfirmation(Player p) {
		return this.removeConfirmations.get(p.getUniqueId());
	}
	
	public void addHorseRenamer(Player p, RPGHorse rpgHorse) {
		HorseRenamer horseConfirmation = this.getHorseRenamer(p);
		if (horseConfirmation != null) {
			horseConfirmation.endTask();
		}
		this.horseRenamers.put(p.getUniqueId(), new HorseRenamer(this.plugin, this, this.horseOwnerManager.getHorseOwner(p), rpgHorse));
	}
	
	public void removeHorseRenamer(UUID uuid) {
		this.horseRenamers.remove(uuid);
	}
	
	public boolean isRenamingHorse(Player p) {
		return this.horseRenamers.containsKey(p.getUniqueId());
	}
	
	public HorseRenamer getHorseRenamer(Player p) {
		return this.horseRenamers.get(p.getUniqueId());
	}
	
}
