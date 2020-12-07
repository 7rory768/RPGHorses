package org.plugins.rpghorses.managers.gui;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.GUIItem;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.guis.ItemPurpose;
import org.plugins.rpghorses.guis.PriceGUIItem;
import org.plugins.rpghorses.guis.instances.HorseGUI;
import org.plugins.rpghorses.guis.instances.SellGUI;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import roryslibrary.util.ItemUtil;

import java.util.HashSet;

public class SellGUIManager {
	
	private final RPGHorsesMain plugin;
	private final StableGUIManager stableGUIManager;
	private final HorseOwnerManager horseOwnerManager;
	
	private HashSet<GUIItem> guiItems = new HashSet<GUIItem>();
	private int horseSlot;
	private String title = "";
	private int rows;
	
	public SellGUIManager(RPGHorsesMain plugin, StableGUIManager stableGUIManager, HorseOwnerManager horseOwnerManager) {
		this.plugin = plugin;
		this.stableGUIManager = stableGUIManager;
		this.horseOwnerManager = horseOwnerManager;
		
		reload();
	}
	
	public void reload() {
		guiItems.clear();
		
		FileConfiguration config = plugin.getConfig();
		
		String path = "sell-gui-options.";
		title = RPGMessagingUtil.format(config.getString(path + "title"));
		rows = config.getInt(path + "rows");
		horseSlot = ItemUtil.getSlot(config, path + "horse-slot");
		
		for (String key : config.getConfigurationSection(path + "items").getKeys(false)) {
			path = "sell-gui-options.items." + key + ".";
			ItemStack item = ItemUtil.getItemStack(config, path);
			int slot = ItemUtil.getSlot(config, path);
			ItemPurpose itemPurpose = ItemPurpose.valueOf(config.getString(path + "purpose"));
			GUIItem guiItem;
			if (itemPurpose == ItemPurpose.CHANGE_PRICE) {
				Sound sound = null;
				String soundString = config.getString(path + "sound.sound").toUpperCase();
				try {
					sound = Sound.valueOf(soundString);
				} catch (IllegalArgumentException e) {
					if (soundString.equals("BLOCK_NOTE_BLOCK_PLING")) {
						sound = Sound.valueOf("NOTE_PLING");
					} else if (soundString.equals("ENTITY_PLAYER_LEVELUP")) {
						sound = Sound.valueOf("LEVEL_UP");
					} else if (soundString.equals("BLOCK_ANVIL_PLACE")) {
						sound = Sound.valueOf("ANVIL_LAND");
					}
				}
				guiItem = new PriceGUIItem(item, itemPurpose, slot, config.getInt(path + "price-change"), sound, Float.valueOf(config.getString(path + "sound.volume", "1.0")), Float.valueOf(config.getString(path + "sound.pitch", "1.0")));
				ItemUtil.fillPlaceholders(guiItem.getItem(), "PRICE-CHANGE", "" + Math.abs(((PriceGUIItem) guiItem).getPriceChange()));
			} else {
				guiItem = new GUIItem(item, itemPurpose, slot);
			}
			guiItems.add(guiItem);
		}
		
		for (Player p : Bukkit.getOnlinePlayers()) {
			HorseOwner horseOwner = horseOwnerManager.getHorseOwner(p);
			if (horseOwner.getGUILocation() == GUILocation.SELL_GUI) {
				horseOwner.openSellGUI(createSellGUI(horseOwner.getHorseGUI()));
			}
		}
	}
	
	public SellGUI createSellGUI(HorseGUI horseGUI) {
		RPGHorse rpgHorse = horseGUI.getRpgHorse();
		HorseOwner horseOwner = rpgHorse.getHorseOwner();
		
		Inventory inv = Bukkit.createInventory(horseOwner.getPlayer(), rows * 9, title);
		
		for (GUIItem guiItem : guiItems) {
			ItemPurpose itemPurpose = guiItem.getItemPurpose();
			if (itemPurpose != ItemPurpose.FILL) {
				if (itemPurpose == ItemPurpose.CONFIRM) {
					inv.setItem(guiItem.getSlot(), guiItem.getItem());
				} else {
					inv.setItem(guiItem.getSlot(), guiItem.getItem());
				}
			}
		}
		
		inv.setItem(horseSlot, stableGUIManager.fillPlaceholders(stableGUIManager.getHorseItem(rpgHorse), rpgHorse));
		
		GUIItem fillItem = getGUIItem(ItemPurpose.FILL);
		if (fillItem != null) {
			ItemStack item = fillItem.getItem();
			for (int i = 0; i < inv.getSize(); i++) {
				if (inv.getItem(i) == null) {
					inv.setItem(i, item);
				}
			}
		}
		
		SellGUI sellGUI = new SellGUI(rpgHorse, inv);
		fillPlaceholders(sellGUI);
		
		return new SellGUI(rpgHorse, inv);
	}
	
	public void performUpdate(SellGUI sellGUI, GUIItem guiItem) {
		if (guiItem instanceof PriceGUIItem) {
			
			
			PriceGUIItem priceGUIItem = (PriceGUIItem) guiItem;
			sellGUI.increasePrice(priceGUIItem.getPriceChange());
			fillPlaceholders(sellGUI);
		}
	}
	
	public GUIItem getGUIItem(ItemPurpose itemPurpose) {
		for (GUIItem guiItem : guiItems) {
			if (guiItem.getItemPurpose() == itemPurpose) {
				return guiItem;
			}
		}
		return null;
	}
	
	public GUIItem getGUIItem(int slot) {
		for (GUIItem item : guiItems) {
			if (item.getSlot() == slot) {
				return item;
			}
		}
		return null;
	}
	
	public void fillPlaceholders(SellGUI sellGUI) {
		GUIItem confirmItem = getGUIItem(ItemPurpose.CONFIRM);
		sellGUI.getInventory().setItem(confirmItem.getSlot(), ItemUtil.fillPlaceholders(confirmItem.getItem().clone(), "{PRICE}", "" + sellGUI.getPrice(), "{HORSE-NAME}", sellGUI.getRpgHorse().getName()));
	}
}
