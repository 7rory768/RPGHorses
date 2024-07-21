package org.plugins.rpghorses.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.players.HorseRenamer;
import org.plugins.rpghorses.players.RemoveHorseConfirmation;
import org.plugins.rpghorses.tiers.Tier;
import org.plugins.rpghorses.utils.ItemUtil;

import java.util.*;

public class RPGHorseManager {

	private final RPGHorsesMain plugin;
	private final HorseOwnerManager horseOwnerManager;

	private final List<EntityType> validEntityTypes = new ArrayList<>();
	private HashMap<UUID, RemoveHorseConfirmation> removeConfirmations = new HashMap<>();
	private HashMap<UUID, HorseRenamer> horseRenamers = new HashMap<>();
	private List<Tier> tiers = new ArrayList<>();

	public RPGHorseManager(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;

		this.setupValidEntityTypes();
		reload();
	}

	public void reload() {
		tiers.clear();

		FileConfiguration config = plugin.getConfig();

		for (String tierNum : config.getConfigurationSection("horse-tiers").getKeys(false)) {
			String path = "horse-tiers." + tierNum + ".";
			try {
				int tier = Integer.valueOf(tierNum);
				int successChance = config.getInt(path + "success-chance", 100);
				double healthMultiplier = config.getDouble(path + "health-multiplier", 1);
				double speedMultiplier = config.getDouble(path + "movement-speed-multiplier", 1);
				double strengthMultiplier = config.getDouble(path + "jump-strength-multiplier", 1);
				double cost = config.getDouble(path + "cost", 0);
				double expCost = config.getDouble(path + "exp-cost", 0);
				List<String> commands = config.getStringList(path + "commands");

				Set<ItemStack> itemsNeeded = new HashSet<>();
				if (config.isSet(path + "items-needed")) {
					for (String itemName : config.getConfigurationSection(path + "items-needed").getKeys(false)) {
						itemsNeeded.add(ItemUtil.getItemStack(config, path + "items-needed." + itemName));
					}
				}

				tiers.add(new Tier(tier, successChance, healthMultiplier, speedMultiplier, strengthMultiplier, cost, expCost, itemsNeeded, commands));
			} catch (IllegalArgumentException e) {
				plugin.getLogger().severe("Invalid tier \"" + tierNum + "\", tiers must be positive integers");
			}
		}
	}

	public int getMaxTier() {
		return tiers.size() + 1;
	}

	public boolean isRPGHorse(Entity entity) {
		return entity != null && entity.hasMetadata("RPGHorse-HorseOwner");
	}

	public RPGHorse getRPGHorse(Entity entity) {
		if (!isRPGHorse(entity)) return null;

		for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners().values()) {
			RPGHorse currentHorse = horseOwner.getCurrentHorse();
			if (currentHorse != null && currentHorse.getHorse().getEntityId() == entity.getEntityId()) {
				return currentHorse;
			}
		}
		return null;
	}

	public Tier getTier(int tier) {
		for (Tier tierO : tiers) {
			if (tierO.getTier() == tier) return tierO;
		}

		return null;
	}

	public double getUpgradeCost(RPGHorse rpgHorse) {
		Tier tier = getTier(rpgHorse.getTier());
		return tier == null ? 0 : tier.getCost();
	}

	public double getSuccessChance(RPGHorse rpgHorse) {
		Tier tier = getTier(rpgHorse.getTier());
		return tier == null ? -1 : tier.getSuccessChance();
	}

	public double getXPNeededToUpgrade(RPGHorse rpgHorse) {
		Tier tier = getTier(rpgHorse.getTier());
		return tier == null ? 0 : tier.getExpCost();
	}

	public Map<ItemStack, Integer> getMissingItems(Player p, Tier tier) {
		Inventory inv = p.getInventory();
		Set<ItemStack> itemsNeeded = tier.getItemsNeeded();
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

	public boolean upgradeHorse(Player p, RPGHorse rpgHorse) {
		Tier tier = getTier(rpgHorse.getTier());
		if (tier != null) {

			if (plugin.getEconomy() != null && tier.getCost() > 0)
				plugin.getEconomy().withdrawPlayer(p, tier.getCost());

			Inventory inv = p.getInventory();
			Set<ItemStack> itemsNeeded = tier.getItemsNeeded();

			if (!getMissingItems(p, tier).isEmpty()) return false;

			for (ItemStack itemNeeded : itemsNeeded) {
				int amountNeeded = itemNeeded.getAmount();
				for (int slot = 0; slot < inv.getSize(); slot++) {
					ItemStack item = inv.getItem(slot);
					if (ItemUtil.itemIsReal(item)) {
						if (item.isSimilar(itemNeeded)) {
							int amount = item.getAmount();

							if (amount <= amountNeeded) {
								inv.setItem(slot, null);
							} else {
								item.setAmount(item.getAmount() - amountNeeded);
							}

							amountNeeded -= amount;

							if (amountNeeded <= 0) continue;
						}
					}
				}
			}

			double success = this.getSuccessChance(rpgHorse);
			if (success < 100 && (new Random()).nextDouble() * 100 >= success) return false;

			double health = rpgHorse.getMaxHealth() * tier.getHealthMultiplier();
			double movementSpeed = rpgHorse.getMovementSpeed() * tier.getMovementSpeedMultiplier();
			double jumpStrength = rpgHorse.getJumpStrength() * tier.getJumpStrengthMultiplier();

			rpgHorse.setMaxHealth(health);
			rpgHorse.setMovementSpeed(movementSpeed);
			rpgHorse.setJumpStrength(jumpStrength);
			rpgHorse.setTier(tier.getTier() + 1);

			if (rpgHorse.getHorse() != null && rpgHorse.getHorse().isValid()) {
				boolean wasMax = rpgHorse.getHorse().getHealth() == rpgHorse.getHorse().getMaxHealth();
				Entity passenger = rpgHorse.getHorse().getPassenger();
				rpgHorse.spawnEntity();

				if (passenger != null) rpgHorse.getHorse().setPassenger(passenger);
				if (wasMax) rpgHorse.getHorse().setHealth(rpgHorse.getMaxHealth());
			}

			tier.runCommands(p.getPlayer());

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
