package org.plugins.rpghorses.managers.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.GUIItem;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.guis.ItemPurpose;
import org.plugins.rpghorses.guis.instances.HorseGUI;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.managers.RPGHorseManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.tiers.Tier;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import org.plugins.rpghorses.utils.ItemUtil;
import roryslibrary.util.MessagingUtil;
import roryslibrary.util.NumberUtil;

import java.util.HashSet;

public class HorseGUIManager {
	
	private final RPGHorsesMain plugin;
	private final HorseOwnerManager horseOwnerManager;
	private final StableGUIManager stableGUIManager;
	private final RPGHorseManager rpgHorseManager;
	
	private String title;
	private int rows;
	private HashSet<GUIItem> guiItems = new HashSet<>();
	
	public HorseGUIManager(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, StableGUIManager stableGUIManager) {
		this.plugin = plugin;
		this.horseOwnerManager = horseOwnerManager;
		this.stableGUIManager = stableGUIManager;
		this.rpgHorseManager = plugin.getRpgHorseManager();
		
		reload();
	}
	
	public void reload() {
		guiItems.clear();
		
		String path = "horse-gui-options.";
		FileConfiguration config = plugin.getConfig();
		this.title = RPGMessagingUtil.format(config.getString(path + "title"));
		this.rows = config.getInt(path + "rows");
		for (String itemID : config.getConfigurationSection(path + "items").getKeys(false)) {
			path = "horse-gui-options.items." + itemID;
			guiItems.add(new GUIItem(config.getConfigurationSection(path)));
		}
		
		for (Player p : Bukkit.getOnlinePlayers()) {
			HorseOwner horseOwner = horseOwnerManager.getHorseOwner(p);
			if (horseOwner.getGUILocation() == GUILocation.HORSE_GUI) {
				horseOwner.openHorseGUI(getHorseGUI(horseOwner.getHorseGUI().getRpgHorse()));
			}
		}
	}
	
	public HorseGUI getHorseGUI(RPGHorse rpgHorse) {
		String horseTitle = RPGMessagingUtil.format(title, rpgHorse);
		
		if (ChatColor.stripColor(horseTitle).length() > 32 && RPGHorsesMain.getVersion().getWeight() < 9) {
			
			int lengthNoName = title.replace("{HORSE-NAME}", "").length();
			int beforeName = title.indexOf("{HORSE-NAME}");
			int afterName = beforeName + "{HORSE-NAME}".length();
			
			if (beforeName != -1) {
				horseTitle = title.substring(0, beforeName) + rpgHorse.getName().substring(0, (32 - lengthNoName)) + title.substring(afterName);
			} else {
				horseTitle = title.substring(0, 32);
			}
			
			horseTitle = MessagingUtil.format(horseTitle);
		}
		
		Inventory inventory = Bukkit.createInventory(null, rows * 9, horseTitle);
		
		for (GUIItem guiItem : guiItems) {
			if (guiItem.isEnabled() && guiItem.getItemPurpose() != ItemPurpose.TOGGLE_AUTOMOUNT_OFF && guiItem.getItemPurpose() != ItemPurpose.TOGGLE_AUTOMOUNT_ON) {
				inventory.setItem(guiItem.getSlot(), guiItem.getItem());
			}
		}
		
		inventory.setItem(ItemUtil.getSlot(plugin.getConfig(), "horse-gui-options.horse-item"), stableGUIManager.fillPlaceholders(stableGUIManager.getHorseItem(rpgHorse), rpgHorse));
		
		Tier tier = rpgHorseManager.getNextTier(rpgHorse);
		GUIItem guiItem = getGUIItem(ItemPurpose.UPGRADE);
		inventory.setItem(guiItem.getSlot(), ItemUtil.fillPlaceholders(guiItem.getItem(), "COST", NumberUtil.getCommaString(tier == null ? 0 : (int) tier.getCost()), "HORSE-EXP-NEEDED", NumberUtil.getCommaString(tier == null ? 0 : (int) tier.getExpCost())));
		
		GUIItem autoMountItem = rpgHorse.getHorseOwner().autoMountOn() ? getGUIItem(ItemPurpose.TOGGLE_AUTOMOUNT_ON) : getGUIItem(ItemPurpose.TOGGLE_AUTOMOUNT_OFF);
		if (autoMountItem.isEnabled()) inventory.setItem(autoMountItem.getSlot(), autoMountItem.getItem());
		
		ItemStack fillItem = getGUIItem(ItemPurpose.FILL).getItem();
		for (int slot = 0; slot < inventory.getSize(); slot++) {
			if (inventory.getItem(slot) == null) {
				inventory.setItem(slot, fillItem);
			}
		}
		
		return new HorseGUI(rpgHorse, inventory);
	}
	
	public void toggleAutoMount(HorseGUI horseGUI) {
		HorseOwner horseOwner = horseGUI.getRpgHorse().getHorseOwner();
		horseOwner.setAutoMount(!horseOwner.autoMountOn());
		Inventory inv = horseGUI.getInventory();
		GUIItem onItem = getGUIItem(ItemPurpose.TOGGLE_AUTOMOUNT_ON), offItem = getGUIItem(ItemPurpose.TOGGLE_AUTOMOUNT_OFF);
		ItemStack fillItem = getGUIItem(ItemPurpose.FILL).getItem();
		if (horseOwner.autoMountOn()) {
			inv.setItem(offItem.getSlot(), fillItem);
			inv.setItem(onItem.getSlot(), onItem.getItem());
		} else {
			inv.setItem(onItem.getSlot(), fillItem);
			inv.setItem(offItem.getSlot(), offItem.getItem());
		}
	}
	
	public ItemPurpose getItemPurpose(int slot) {
		for (GUIItem guiItem : guiItems) {
			if (guiItem.isEnabled() && guiItem.getSlot() == slot && (guiItem.getItemPurpose() != ItemPurpose.NOTHING || guiItem.getItemPurpose() != ItemPurpose.FILL)) {
				return guiItem.getItemPurpose();
			}
		}
		return ItemPurpose.NOTHING;
	}
	
	public GUIItem getGUIItem(ItemPurpose itemPurpose) {
		for (GUIItem guiItem : guiItems) {
			if (guiItem.isEnabled() && guiItem.getItemPurpose() == itemPurpose) {
				return guiItem;
			}
		}
		return null;
	}
	
}
