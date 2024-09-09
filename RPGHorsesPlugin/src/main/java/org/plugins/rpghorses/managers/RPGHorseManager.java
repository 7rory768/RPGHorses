package org.plugins.rpghorses.managers;

import lombok.Getter;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.crates.HorseCrate;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.players.HorseRenamer;
import org.plugins.rpghorses.players.RemoveHorseConfirmation;
import org.plugins.rpghorses.tiers.Tier;
import org.plugins.rpghorses.utils.ItemUtil;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import roryslibrary.util.MessagingUtil;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class RPGHorseManager {

	private final RPGHorsesMain plugin;
	private final HorseOwnerManager horseOwnerManager;

	private final List<EntityType> validEntityTypes = new ArrayList<>();
	private final HashMap<UUID, RemoveHorseConfirmation> removeConfirmations = new HashMap<>();
	private final HashMap<UUID, HorseRenamer> horseRenamers = new HashMap<>();
	private final List<Tier> tiers = new ArrayList<>();

	private int healthRegenInterval;
	private double healthRegenAmount;
	private boolean onlyRegenActiveHorses;
	private BukkitTask healthRegenTask;

	public RPGHorseManager(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;

		this.setupValidEntityTypes();
		reload();
	}

	public void reload() {
		tiers.clear();

		FileConfiguration config = plugin.getConfig();

		this.healthRegenInterval = config.getInt("horse-options.health-regen-interval", 20);
		this.healthRegenAmount = config.getDouble("horse-options.health-regen-amount", 1);
		this.onlyRegenActiveHorses = config.getBoolean("horse-options.only-regen-active-horses", false);

		this.startHealthRegenTask();

		ConfigurationSection tiersConfig = config.getConfigurationSection("horse-tiers");

		for (String tierNum : tiersConfig.getKeys(false)) {
			try {
				int tier = Integer.valueOf(tierNum);
				tiers.add(new Tier(tiersConfig.getConfigurationSection(tierNum), tier));
			} catch (IllegalArgumentException e) {
				plugin.getLogger().severe("Invalid tier \"" + tierNum + "\", tiers must be positive integers");
			}
		}
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

	public void startHealthRegenTask() {
		if (this.healthRegenTask != null)
			this.healthRegenTask.cancel();

		if (this.healthRegenAmount <= 0 || this.healthRegenInterval <= 0) return;

		this.healthRegenTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(player);
				if (onlyRegenActiveHorses && horseOwner.getCurrentHorse() == null) continue;

				for (RPGHorse rpgHorse : horseOwner.getRPGHorses()) {
					if (rpgHorse.isDead() || (onlyRegenActiveHorses && horseOwner.getCurrentHorse() != rpgHorse)) continue;
					rpgHorse.setHealth(Math.min(rpgHorse.getMaxHealth(), rpgHorse.getHealth() + this.healthRegenAmount));
					plugin.getStableGuiManager().updateRPGHorse(rpgHorse);
				}
			}
		}, 0, healthRegenInterval);
	}

	public boolean isRPGHorse(Entity entity) {
		return entity != null && entity.hasMetadata("RPGHorse-HorseOwner");
	}

	public RPGHorse getRPGHorse(Entity entity) {
		if (!isRPGHorse(entity)) return null;

		for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners().values()) {
			RPGHorse currentHorse = horseOwner.getCurrentHorse();
			if (currentHorse != null && currentHorse.getHorse() != null && currentHorse.getHorse().getEntityId() == entity.getEntityId()) {
				return currentHorse;
			}
		}
		return null;
	}

	public double getUpgradeCost(RPGHorse rpgHorse) {
		Tier tier = getNextTier(rpgHorse);
		return tier == null ? 0 : tier.getCost();
	}

	public Tier getMaxTier(RPGHorse rpgHorse) {
		HorseCrate crate = plugin.getHorseCrateManager().getHorseCrate(rpgHorse.getSourceCrate());
		if (crate != null && !crate.getUpgradeTiers().isEmpty()) {
			return crate.getUpgradeTiers().stream().max(Comparator.comparingInt(Tier::getTier)).orElse(null);
		}

		return tiers.stream().max(Comparator.comparingInt(Tier::getTier)).orElse(null);
	}

	public Tier getNextTier(RPGHorse rpgHorse) {
		HorseCrate crate = plugin.getHorseCrateManager().getHorseCrate(rpgHorse.getSourceCrate());
		if (crate != null && !crate.getUpgradeTiers().isEmpty()) {
			for (Tier crateTier : crate.getUpgradeTiers()) {
				if (crateTier.getTier() == rpgHorse.getTier()) {
					return crateTier;
				}
			}

			return null;
		}

		for (Tier tierO : tiers) {
			if (tierO.getTier() == rpgHorse.getTier()) return tierO;
		}

		return null;
	}

	@Deprecated
	public Tier getNextTier(int tier) {
		for (Tier tierO : tiers) {
			if (tierO.getTier() == tier) return tierO;
		}

		return null;
	}

	public double getXPNeededToUpgrade(RPGHorse rpgHorse) {
		Tier tier = getNextTier(rpgHorse);
		return tier == null ? 0 : tier.getExpCost();
	}

	public boolean upgradeHorse(Player p, RPGHorse rpgHorse) {
		Tier tier = getNextTier(rpgHorse);
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
					if (ItemUtil.itemIsReal(item) && ItemUtil.isSimilar(item, itemNeeded)) {
						int amount = item.getAmount();

						if (amount <= amountNeeded) {
							inv.setItem(slot, null);
						} else {
							item.setAmount(item.getAmount() - amountNeeded);
						}

						amountNeeded -= amount;

						if (amountNeeded <= 0) break;
					}
				}
			}

			double success = this.getSuccessChance(rpgHorse);
			if (success < 100 && (new Random()).nextDouble() * 100 >= success) return false;

			tier.applyUpgrade(rpgHorse);

			if (rpgHorse.getHorse() != null && rpgHorse.getHorse().isValid()) {
				Entity passenger = rpgHorse.getHorse().getPassenger();

				if (rpgHorse.spawnEntity()) {
					if (passenger != null)
						rpgHorse.getHorse().setPassenger(passenger);
				}
			}

			tier.runCommands(p.getPlayer());

			plugin.getStableGuiManager().updateRPGHorse(rpgHorse);

			return true;
		}
		return false;
	}

	public double getSuccessChance(RPGHorse rpgHorse) {
		Tier tier = getNextTier(rpgHorse);
		return tier == null ? -1 : tier.getSuccessChance();
	}

	public Map<ItemStack, Integer> getMissingItems(Player p, Tier tier) {
		Inventory inv = p.getInventory();
		Set<ItemStack> itemsNeeded = tier.getItemsNeeded();
		Map<ItemStack, Integer> amountMissing = new HashMap<>();

		for (ItemStack itemNeeded : itemsNeeded) {
			int amount = 0, amountNeeded = itemNeeded.getAmount();
			for (ItemStack item : inv) {
				if (ItemUtil.itemIsReal(item) && ItemUtil.isSimilar(item, itemNeeded)) {
					amount += item.getAmount();
				}
			}

			if (amount < amountNeeded) amountMissing.put(itemNeeded, amountNeeded - amount);
		}

		return amountMissing;
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

	public RemoveHorseConfirmation getRemovingHorseConfirmation(Player p) {
		return this.removeConfirmations.get(p.getUniqueId());
	}

	public void removeRemoveConfirmation(UUID uuid) {
		this.removeConfirmations.remove(uuid);
	}

	public boolean isRemovingRPGHorse(Player p) {
		return this.removeConfirmations.containsKey(p.getUniqueId());
	}

	public void addHorseRenamer(Player p, RPGHorse rpgHorse) {
		HorseRenamer horseConfirmation = this.getHorseRenamer(p);
		if (horseConfirmation != null) {
			horseConfirmation.endTask();
		}
		this.horseRenamers.put(p.getUniqueId(), new HorseRenamer(this.plugin, this, this.horseOwnerManager.getHorseOwner(p), rpgHorse));
	}

	public HorseRenamer getHorseRenamer(Player p) {
		return this.horseRenamers.get(p.getUniqueId());
	}

	public void removeHorseRenamer(UUID uuid) {
		this.horseRenamers.remove(uuid);
	}

	public boolean isRenamingHorse(Player p) {
		return this.horseRenamers.containsKey(p.getUniqueId());
	}

	public void openRenameGUI(Player p, HorseOwner horseOwner, RPGHorse rpgHorse) {
		openRenameGUI(p, horseOwner, rpgHorse, true);
	}

	public void openRenameGUI(Player p, HorseOwner horseOwner, RPGHorse rpgHorse, boolean returnToHorseGUI) {
		RPGMessagingUtil messagingUtil = plugin.getMessagingUtil();

		AnvilGUI.Builder builder = new AnvilGUI.Builder().plugin(plugin);
		builder.onClose(player -> {
			if (returnToHorseGUI) {
				new BukkitRunnable() {
					@Override
					public void run() {
						horseOwner.openHorseGUI(horseOwner.getHorseGUI());
					}
				}.runTaskLater(plugin, 1L);
			}
		}
		).onClick((clickSlot, state) -> {
			String oldName = rpgHorse.getName(), name = state.getText();
			if (!plugin.getConfig().getBoolean("horse-options.names.allow-spaces")) {
				name = name.replace(" ", "");
			}

			int length = ChatColor.stripColor(MessagingUtil.format(name)).length(), minLength = plugin.getConfig().getInt("horse-options.names.min-length"), maxLength = plugin.getConfig().getInt("horse-options.names.max-length");
			if (length < minLength) {
				messagingUtil.sendMessageAtPath(p, "messages.short-name", "HORSE-NAME", name, "MIN-LENGTH", "" + minLength, "MAX-LENGTH", "" + maxLength);
				p.closeInventory();
			} else if (length > maxLength) {
				messagingUtil.sendMessageAtPath(p, "messages.long-name", "HORSE-NAME", name, "MIN-LENGTH", "" + minLength, "MAX-LENGTH", "" + maxLength);
				p.closeInventory();
			} else {
				rpgHorse.setName(RPGMessagingUtil.format(name));
				plugin.getStableGuiManager().updateRPGHorse(rpgHorse);
				messagingUtil.sendMessage(p, this.plugin.getConfig().getString("messages.horse-renamed").replace("{OLD-HORSE-NAME}", oldName), rpgHorse);
				if (returnToHorseGUI) {
					horseOwner.openHorseGUI(plugin.getHorseGUIManager().getHorseGUI(rpgHorse));
				}
			}
			return Collections.singletonList(AnvilGUI.ResponseAction.close());
		});

		builder.title(RPGMessagingUtil.format("&6Type the new name")).itemOutput(horseOwner.getHorseGUI().getInventory().getItem(ItemUtil.getSlot(plugin.getConfig(), "horse-gui-options.horse-item")).clone()).text(rpgHorse.getName()).open(p);
	}

}
