package org.plugins.rpghorses.managers.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.guis.instances.StableGUI;
import org.plugins.rpghorses.guis.instances.StableGUIPage;
import org.plugins.rpghorses.guis.instances.UpgradeGUI;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import org.plugins.rpghorses.utils.SkinValueUtil;
import roryslibrary.util.ItemUtil;
import roryslibrary.util.MessagingUtil;
import roryslibrary.util.TimeUtil;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class StableGUIManager {
	
	private final RPGHorsesMain plugin;
	private final HorseOwnerManager horseOwnerManager;
	private final RPGHorseManager rpgHorseManager;
	private final ItemUtil itemUtil;
	private final MessagingUtil messagingUtil;
	
	@Deprecated
	private Inventory upgradeInventory;
	private ItemStack stableFillItem, upgradeFillItem, upgradeItem, aliveHorseItem, deadHorseItem, marketHorseItem, previousPageItem, nextPageItem;
	private int stableRows, upgradeRows, previousPageSlot, nextPageSlot, upgradeItemSlot, deathCooldown;
	private BukkitTask cooldownTask;
	private String progressBarChar, completedColor, missingColor;
	private int progressBarCount;
	
	@Deprecated
	private HashMap<UUID, UpgradeGUI> upgradeGUIs = new HashMap<>();
	
	public StableGUIManager(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, RPGHorseManager rpgHorseManager, ItemUtil itemUtil, RPGMessagingUtil messagingUtil) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;
		this.rpgHorseManager = rpgHorseManager;
		this.itemUtil = itemUtil;
		this.messagingUtil = messagingUtil;
		
		this.reload();
		this.startCooldownTask();
	}
	
	public int getPreviousPageSlot() {
		return previousPageSlot;
	}
	
	public int getNextPageSlot() {
		return nextPageSlot;
	}
	
	@Deprecated
	public void setupUpgradeGUI() {
		this.upgradeInventory = Bukkit.createInventory(null, this.upgradeRows * 9, MessagingUtil.format(this.plugin.getConfig().getString("upgrade-options.title")));
		
		for (int slot = 0; slot < this.upgradeInventory.getSize(); slot++) {
			if (slot == this.upgradeItemSlot) {
				this.upgradeInventory.setItem(this.upgradeItemSlot, this.upgradeItem);
			} else {
				this.upgradeInventory.setItem(slot, this.upgradeFillItem);
			}
		}
		
		for (UUID uuid : this.upgradeGUIs.keySet()) {
			this.openUpgradeGUI(Bukkit.getPlayer(uuid), this.upgradeGUIs.get(uuid).getRPGHorse());
		}
	}
	
	@Deprecated
	public void openUpgradeGUI(Player p, RPGHorse rpgHorse) {
		HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
		
		Inventory cloneGUI = Bukkit.createInventory(null, this.upgradeInventory.getSize(), MessagingUtil.format(this.plugin.getConfig().getString("upgrade-options.title")));
		
		for (int slot = 0; slot < this.upgradeInventory.getSize(); slot++) {
			ItemStack item = this.upgradeInventory.getItem(slot);
			if (item != null) {
				if (slot == this.getUpgradeItemSlot()) {
					item = item.clone();
					ItemMeta itemMeta = item.getItemMeta();
					int tier = rpgHorse.getTier() + 1;
					double cost = this.rpgHorseManager.getUpgradeCost(rpgHorse), successChance = this.rpgHorseManager.getSuccessChance(rpgHorse);
					String horseName = MessagingUtil.format("&7" + rpgHorse.getName());
					if (itemMeta.hasDisplayName()) {
						itemMeta.setDisplayName(itemMeta.getDisplayName().replace("{TIER}", "" + tier).replace("{COST}", "" + cost).replace("{SUCCESS-CHANCE}", "" + successChance).replace("{HORSE-NAME}", horseName));
					}
					if (itemMeta.hasLore()) {
						List<String> lore = new ArrayList<>();
						for (String line : itemMeta.getLore()) {
							lore.add(line.replace("{TIER}", "" + tier).replace("{COST}", "" + cost).replace("{SUCCESS-CHANCE}", "" + successChance).replace("{HORSE-NAME}", horseName));
						}
						itemMeta.setLore(lore);
					}
					item.setItemMeta(itemMeta);
				}
				cloneGUI.setItem(slot, item);
			}
		}
		
		p.openInventory(cloneGUI);
		this.upgradeGUIs.put(p.getUniqueId(), new UpgradeGUI(rpgHorse, cloneGUI));
		horseOwner.setGUILocation(GUILocation.STABLE_GUI);
	}
	
	public int getUpgradeItemSlot() {
		return upgradeItemSlot;
	}
	
	public ItemStack getHorseItem(RPGHorse rpgHorse) {
		ItemStack item;
		if (rpgHorse.isInMarket()) {
			item = marketHorseItem.clone();
			if (plugin.getConfig().isSet("stable-options.market-horse-item.skin-value")) {
				return item;
			}
		} else {
			item = rpgHorse.isDead() ? this.deadHorseItem.clone() : this.aliveHorseItem.clone();
		}
		
		return SkinValueUtil.applySkin(rpgHorse, item);
	}
	
	public void setupStableGUI(HorseOwner horseOwner) {
		List<RPGHorse> rpgHorses = horseOwner.getRPGHorses();
		int rpgHorsesAdded = 0;
		String title = MessagingUtil.format(this.plugin.getConfig().getString("stable-options.title"));
		int horseSpaces = (stableRows - 2) * 7, totalRows = stableRows, nextPageSlot = getNextPageSlot(), previousPageSlot = getPreviousPageSlot();
		if (rpgHorses.size() / (double) horseSpaces > 1) {
			if (stableRows == 3) {
				totalRows++;
				nextPageSlot = totalRows * 9 - 1;
				previousPageSlot = nextPageSlot - 8;
			} else {
				horseSpaces -= 7;
			}
		}
		Inventory gui = Bukkit.createInventory(null, totalRows * 9, title);
		
		int row = 1, pageNum = 1, slot = 10, totalHorses = rpgHorses.size(), horsesLeft = totalHorses, skipSlot = -1;
		
		List<StableGUIPage> stableGUIPages = new ArrayList<>();
		HashMap<Integer, RPGHorse> rpgHorseSlots = new HashMap<>();
		
		if (horsesLeft < 7) {
			if (horsesLeft % 2 == 0) {
				skipSlot = (row * 9) + 4;
				slot += ((6 - horsesLeft) / 2);
			} else {
				slot += ((7 - horsesLeft) / 2);
			}
		}
		
		while (rpgHorsesAdded < rpgHorses.size()) {
			RPGHorse rpgHorse = rpgHorses.get(rpgHorsesAdded);
			ItemStack item = getHorseItem(rpgHorse);
			item = fillPlaceholders(item, rpgHorse);
			
			if (slot == skipSlot) {
				slot++;
				skipSlot = -1;
			}
			
			gui.setItem(slot, item);
			rpgHorseSlots.put(slot, rpgHorse);
			
			rpgHorsesAdded++;
			horsesLeft--;
			
			slot++;
			
			if (rpgHorsesAdded % 7 == 0) {
				row++;
				slot = (row * 9) + 1;
				if (horsesLeft < 7) {
					if (horsesLeft % 2 == 0) {
						skipSlot = (row * 9) + 4;
						slot += ((6 - horsesLeft) / 2);
					} else {
						slot += ((7 - horsesLeft) / 2);
					}
				}
			}
			
			if (rpgHorsesAdded % horseSpaces == 0 || rpgHorsesAdded == rpgHorses.size()) {
				for (String key : plugin.getConfig().getConfigurationSection("stable-options.background-items").getKeys(false)) {
					int fillSlot = ItemUtil.getSlot(plugin.getConfig(), "stable-options.background-items." + key);
					
					if (gui.getItem(fillSlot) == null) {
						gui.setItem(fillSlot, ItemUtil.getItemStack(plugin, "stable-options.background-items." + key));
					}
				}
				
				for (int fillSlot = 0; fillSlot < gui.getSize(); fillSlot++) {
					if (gui.getItem(fillSlot) == null) {
						gui.setItem(fillSlot, this.stableFillItem);
					}
				}
				
				if (pageNum > 1) {
					gui.setItem(previousPageSlot, previousPageItem);
				}
				if (rpgHorsesAdded < rpgHorses.size()) {
					gui.setItem(nextPageSlot, nextPageItem);
				}
				
				stableGUIPages.add(new StableGUIPage(horseOwner, pageNum++, gui, rpgHorseSlots));
				rpgHorseSlots = new HashMap<>();
				gui = Bukkit.createInventory(null, totalRows * 9, title);
				
				if (rpgHorsesAdded < rpgHorses.size()) {
					row = 1;
					slot = 10;
					if (horsesLeft < 7) {
						if (horsesLeft % 2 == 0) {
							skipSlot = (row * 9) + 4;
							slot += ((6 - horsesLeft) / 2);
						} else {
							slot += ((7 - horsesLeft) / 2);
						}
					}
				}
			}
		}
		
		if (rpgHorsesAdded == 0) {
			for (int fillSlot = 0; fillSlot < gui.getSize(); fillSlot++) {
				gui.setItem(fillSlot, stableFillItem);
			}
			stableGUIPages.add(new StableGUIPage(horseOwner, pageNum++, gui, rpgHorseSlots));
		}
		
		StableGUI stableGUI = new StableGUI(horseOwner, stableGUIPages);
		horseOwner.setStableGUI(stableGUI);
	}
	
	public void updateRPGHorse(RPGHorse rpgHorse) {
		if (rpgHorse != null) {
			HorseOwner horseOwner = rpgHorse.getHorseOwner();
			StableGUI stableGUI = horseOwner.getStableGUI();
			pageLoop:
			for (StableGUIPage stableGUIPage : stableGUI.getStableGUIPages()) {
				HashMap<Integer, RPGHorse> rpgHorses = stableGUIPage.getHorseSlots();
				for (Integer slot : rpgHorses.keySet()) {
					RPGHorse loopHorse = rpgHorses.get(slot);
					if (loopHorse == rpgHorse) {
						ItemStack item = getHorseItem(rpgHorse);
						item = this.fillPlaceholders(item, rpgHorse);
						stableGUIPage.getGUI().setItem(slot, item);
						break pageLoop;
					}
				}
			}
		}
	}
	
	public Long getDeathDifferent(RPGHorse rpgHorse) {
		return rpgHorse.getDeathTime() + this.deathCooldown - System.currentTimeMillis();
	}
	
	public ItemStack fillPlaceholders(ItemStack item, RPGHorse rpgHorse) {
		if (item != null && rpgHorse != null) {
			ItemMeta itemMeta = item.getItemMeta();
			
			String cooldownTime = "";
			if (rpgHorse.isDead()) {
				cooldownTime = TimeUtil.formatTime(this.getDeathDifferent(rpgHorse) / 1000L);
			}
			
			NumberFormat formatter = new DecimalFormat("#0.0##");
			String xpBar = messagingUtil.getProgressBar(rpgHorse.getXp(), rpgHorseManager.getXPNeededToUpgrade(rpgHorse), progressBarCount, completedColor, missingColor, progressBarChar);
			String name = rpgHorse.getName(), tier = "" + rpgHorse.getTier(), movementSpeed = formatter.format(rpgHorse.getMovementSpeed()), jumpStrength = formatter.format(rpgHorse.getJumpStrength()), health = formatter.format(rpgHorse.getHealth()), maxHealth = formatter.format(rpgHorse.getMaxHealth());
			
			if (name == null) {
				name = "RPGHorse";
			}
			name = MessagingUtil.format(name);
			
			if (itemMeta.hasDisplayName()) {
				itemMeta.setDisplayName(itemMeta.getDisplayName().replace("{HEALTH}", health).replace("{MAX-HEALTH}", maxHealth).replace("{TIER}", tier).replace("{XP}", xpBar).replace("{HORSE-NAME}", name).replace("{MOVEMENT-SPEED}", movementSpeed).replace("{JUMP-STRENGTH}", jumpStrength).replace("{DEATH-COOLDOWN}", "" + cooldownTime));
			}
			
			if (itemMeta.hasLore()) {
				List<String> newLore = new ArrayList<>();
				for (String line : itemMeta.getLore()) {
					newLore.add(line.replace("{HEALTH}", health).replace("{MAX-HEALTH}", maxHealth).replace("{TIER}", tier).replace("{XP}", xpBar).replace("{HORSE-NAME}", name).replace("{MOVEMENT-SPEED}", movementSpeed).replace("{JUMP-STRENGTH}", jumpStrength).replace("{DEATH-COOLDOWN}", "" + cooldownTime));
				}
				itemMeta.setLore(newLore);
			}
			
			item.setItemMeta(itemMeta);
		}
		return item;
	}
	
	public void reloadAllStableGUIS() {
		for (HorseOwner horseOwner : this.horseOwnerManager.getHorseOwners().values()) {
			this.setupStableGUI(horseOwner);
		}
	}
	
	public boolean reload() {
		this.progressBarChar = this.plugin.getConfig().getString("progress-bar.character");
		this.completedColor = this.plugin.getConfig().getString("progress-bar.completed-color");
		this.missingColor = this.plugin.getConfig().getString("progress-bar.missing-color");
		this.progressBarCount = this.plugin.getConfig().getInt("progress-bar.count");
		
		boolean reloadItems = false;
		reloadItems = this.reloadStableFillItem() || reloadItems;
		reloadItems = this.reloadAliveHorseItem() || reloadItems;
		reloadItems = this.reloadDeadHorseItem() || reloadItems;
		reloadItems = this.reloadMarketHorseItem() || reloadItems;
		reloadItems = this.reloadPreviousPageItem() || reloadItems;
		reloadItems = this.reloadNextPageItem() || reloadItems;
		reloadItems = this.reloadStableNumbers() || reloadItems;
		reloadItems = this.reloadDeathCooldown() || reloadItems;
		
		if (reloadItems) {
			this.reloadAllStableGUIS();
		}

/*		boolean reloadUpgradeGUI = false;
		reloadUpgradeGUI = this.reloadUpgradeNumbers() || reloadUpgradeGUI;
		reloadUpgradeGUI = this.reloadUpgradeFillItem() || reloadUpgradeGUI;
		reloadUpgradeGUI = this.reloadUpgradeItem() || reloadUpgradeGUI;

		if (reloadUpgradeGUI) {
			this.setupUpgradeGUI();
		}*/
		
		return reloadItems;
	}
	
	private boolean reloadStableFillItem() {
		ItemStack oldStableFillItem = this.stableFillItem;
		this.stableFillItem = this.itemUtil.getItemStack("stable-options.fill-item");
		return ItemUtil.isSimilar(oldStableFillItem, stableFillItem);
	}
	
	private boolean reloadAliveHorseItem() {
		ItemStack oldAliveHorseItem = this.aliveHorseItem;
		this.aliveHorseItem = this.itemUtil.getItemStack("stable-options.alive-horse-item");
		return ItemUtil.isSimilar(oldAliveHorseItem, aliveHorseItem);
	}
	
	private boolean reloadDeadHorseItem() {
		ItemStack oldDeadHorseItem = this.deadHorseItem;
		this.deadHorseItem = this.itemUtil.getItemStack("stable-options.dead-horse-item");
		return ItemUtil.isSimilar(oldDeadHorseItem, deadHorseItem);
	}
	
	private boolean reloadMarketHorseItem() {
		ItemStack oldMarketHorseItem = this.marketHorseItem;
		this.marketHorseItem = this.itemUtil.getItemStack("stable-options.market-horse-item");
		return ItemUtil.isSimilar(oldMarketHorseItem, marketHorseItem);
	}
	
	private boolean reloadPreviousPageItem() {
		ItemStack oldPreviousPageItem = this.previousPageItem;
		this.previousPageItem = this.itemUtil.getItemStack("stable-options.previous-page-item");
		return ItemUtil.isSimilar(oldPreviousPageItem, previousPageItem);
	}
	
	private boolean reloadNextPageItem() {
		ItemStack oldNextPageItem = this.nextPageItem;
		this.nextPageItem = this.itemUtil.getItemStack("stable-options.next-page-item");
		return ItemUtil.isSimilar(oldNextPageItem, nextPageItem);
	}
	
	private boolean reloadStableNumbers() {
		int oldStableRows = this.stableRows;
		this.stableRows = this.plugin.getConfig().getInt("stable-options.rows");
		
		if (stableRows < 3) {
			stableRows = 3;
		}
		
		this.nextPageSlot = this.stableRows * 9 - 1;
		this.previousPageSlot = this.nextPageSlot - 8;
		
		return this.stableRows != oldStableRows;
	}
	
	private boolean reloadDeathCooldown() {
		int oldDeathCooldown = this.deathCooldown;
		this.deathCooldown = this.plugin.getConfig().getInt("horse-options.death-cooldown") * 1000;
		
		return this.deathCooldown != oldDeathCooldown;
	}
	
	@Deprecated
	private boolean reloadUpgradeFillItem() {
		ItemStack oldUpgradeFillItem = this.upgradeFillItem;
		this.upgradeFillItem = this.itemUtil.getItemStack("upgrade-options.fill-item");
		return ItemUtil.isSimilar(oldUpgradeFillItem, upgradeFillItem);
	}
	
	@Deprecated
	private boolean reloadUpgradeItem() {
		ItemStack oldUpgradeItem = this.upgradeItem;
		this.upgradeItem = this.itemUtil.getItemStack("upgrade-options.upgrade-item");
		return ItemUtil.isSimilar(oldUpgradeItem, upgradeItem);
	}
	
	@Deprecated
	private boolean reloadUpgradeNumbers() {
		int oldUpgradeRows = this.upgradeRows;
		this.upgradeRows = this.plugin.getConfig().getInt("upgrade-options.rows");
		
		int oldUpgradeItemSlot = this.upgradeItemSlot;
		this.upgradeItemSlot = this.itemUtil.getSlot("upgrade-options.upgrade-item");
		
		return this.upgradeRows != oldUpgradeRows || this.upgradeItemSlot != oldUpgradeItemSlot;
	}
	
	public void startCooldownTask() {
		this.cancelCooldownTask();
		
		this.cooldownTask = new BukkitRunnable() {
			@Override
			public void run() {
				for (HorseOwner horseOwner : horseOwnerManager.getHorseOwners().values()) {
					if (horseOwner.isInGUI(GUILocation.STABLE_GUI)) {
						StableGUIPage stableGUIPage = horseOwner.getCurrentStableGUIPage();
						if (stableGUIPage != null) {
							for (int slot : stableGUIPage.getHorseSlots().keySet()) {
								RPGHorse rpgHorse = stableGUIPage.getRPGHorse(slot);
								if (rpgHorse.isDead()) {
									if (getDeathDifferent(rpgHorse) <= 0) {
										rpgHorse.setDead(false);
									}
									updateRPGHorse(rpgHorse);
								}
							}
						}
					}
				}
			}
		}.runTaskTimer(this.plugin, 0L, 10L);
	}
	
	public void cancelCooldownTask() {
		if (this.cooldownTask != null) {
			this.cooldownTask.cancel();
			this.cooldownTask = null;
		}
	}
	
	@Deprecated
	public UpgradeGUI getUpgradeGUI(Player p) {
		return this.upgradeGUIs.get(p.getUniqueId());
	}
	
	@Deprecated
	public void removeUpgradeGUI(Player p) {
		this.upgradeGUIs.remove(p.getUniqueId());
	}
	
}
