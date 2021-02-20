package org.plugins.rpghorses.managers.gui;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.GUIItem;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.guis.ItemPurpose;
import org.plugins.rpghorses.guis.TrailGUIItem;
import org.plugins.rpghorses.guis.instances.HorseGUI;
import org.plugins.rpghorses.guis.instances.TrailsGUI;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.managers.ParticleManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import roryslibrary.util.ItemUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TrailGUIManager {
	
	private final RPGHorsesMain plugin;
	private final ParticleManager particleManager;
	private final HorseOwnerManager horseOwnerManager;
	
	private ItemStack unknownTrailItem, trailItem, fillItem;
	private GUIItem backItem;
	private List<TrailGUIItem> validTrails = new ArrayList<>();
	private int rows = 3;
	
	public TrailGUIManager(RPGHorsesMain plugin, ParticleManager particleManager, HorseOwnerManager horseOwnerManager) {
		this.plugin = plugin;
		this.particleManager = particleManager;
		this.horseOwnerManager = horseOwnerManager;
		
		reload();
	}
	
	public void reload() {
		validTrails.clear();
		
		FileConfiguration config = plugin.getConfig();
		
		String path = "trail-gui-options.";
		
		unknownTrailItem = ItemUtil.getItemStack(config, path + "unknown-trail");
		trailItem = ItemUtil.getItemStack(config, path + "trail-item");
		fillItem = ItemUtil.getItemStack(config, path + "fill-item");
		backItem = new GUIItem(ItemUtil.getItemStack(config, path + "back-item"), ItemPurpose.BACK, ItemUtil.getSlot(config, path + "back-item"));
		
		for (String trailName : config.getConfigurationSection(path + "trails").getKeys(false)) {
			if (particleManager.isValidParticle(trailName)) {
				ItemStack item = trailItem.clone();
				ItemMeta itemMeta = trailItem.getItemMeta();
				ItemMeta newMeta = ItemUtil.applyCustomHead(itemMeta.clone(), config.getString(path + "trails." + trailName));
				if (newMeta != null) {
					item.setItemMeta(newMeta);
				}
				validTrails.add(replacePlaceholders(new TrailGUIItem(item, ItemPurpose.TRAIL, -1, trailName)));
			}
		}
		
		int totalTrails = validTrails.size(), slot = 10, trailsLeft = totalTrails, row = 1;
		int skipSlot = -1;
		if (trailsLeft < 7) {
			if (trailsLeft % 2 == 0) {
				skipSlot = (row * 9) + 4;
				slot += ((6 - trailsLeft) / 2);
			} else {
				slot += ((7 - trailsLeft) / 2);
			}
		}
		
		for (TrailGUIItem trailGUIItem : validTrails) {
			if (slot == skipSlot) {
				slot++;
			}
			
			trailGUIItem.setSlot(slot);
			
			trailsLeft--;
			if ((totalTrails - trailsLeft) % 7 == 0) {
				row++;
				slot = (row * 9) + 1;
				if (trailsLeft < 7) {
					if (trailsLeft % 2 == 0) {
						skipSlot = (row * 9) + 4;
						slot += ((6 - trailsLeft) / 2);
					} else {
						slot += ((7 - trailsLeft) / 2);
					}
				}
			} else {
				slot++;
			}
		}
		
		rows = row + 3;
		backItem.setSlot(((rows - 1) * 9) + 4);
		
		for (HorseOwner horseOwner : horseOwnerManager.getHorseOwners().values()) {
			if (horseOwner.getGUILocation() == GUILocation.TRAILS_GUI) {
				horseOwner.openTrailsGUI(setupTrailsGUI(horseOwner.getHorseGUI()));
			}
		}
		
	}
	
	public TrailsGUI setupTrailsGUI(HorseGUI horseGUI) {
		RPGHorse rpgHorse = horseGUI.getRpgHorse();
		Player p = rpgHorse.getHorseOwner().getPlayer();
		
		HashSet<TrailGUIItem> trails = new HashSet<>(), unknownTrails = new HashSet<>();
		
		Inventory inv = Bukkit.createInventory(p, rows * 9, RPGMessagingUtil.format(plugin.getConfig().getString("trail-gui-options.title")));
		
		boolean hasStarPerm = p.hasPermission("rpghorses.trail.*");
		for (TrailGUIItem trailGUIItem : validTrails) {
			ItemStack item;
			if (hasStarPerm || p.hasPermission("rpghorses.trail." + trailGUIItem.getTrailName().toLowerCase())) {
				trails.add(trailGUIItem);
				item = trailGUIItem.getItem();
			} else {
				unknownTrails.add(trailGUIItem);
				item = unknownTrailItem;
			}
			inv.setItem(trailGUIItem.getSlot(), item);
		}
		
		inv.setItem(backItem.getSlot(), backItem.getItem());
		
		for (int i = 0; i < inv.getSize(); i++) {
			if (inv.getItem(i) == null) {
				inv.setItem(i, fillItem);
			}
		}
		
		return new TrailsGUI(rpgHorse, inv, trails, unknownTrails);
	}
	
	public TrailGUIItem replacePlaceholders(TrailGUIItem trailGUIItem) {
		String name = "";
		for (String word : trailGUIItem.getTrailName().toLowerCase().replace("_", " ").split("\\s")) {
			name += word.substring(0, 1).toUpperCase() + word.substring(1) + " ";
		}
		name = name.trim();
		
		ItemMeta itemMeta = trailGUIItem.getItem().getItemMeta();
		
		if (itemMeta.hasDisplayName()) {
			itemMeta.setDisplayName(itemMeta.getDisplayName().replace("{TRAIL}", name));
		}
		
		if (itemMeta.hasLore()) {
			List<String> lore = new ArrayList<>();
			for (String line : itemMeta.getLore()) {
				lore.add(line.replace("{TRAIL}", name));
			}
			itemMeta.setLore(lore);
		}
		
		trailGUIItem.getItem().setItemMeta(itemMeta);
		return trailGUIItem;
	}
	
	public ItemPurpose getItemPurpose(int slot, TrailsGUI trailsGUI) {
		if (slot == backItem.getSlot()) {
			return ItemPurpose.BACK;
		}
		
		return trailsGUI.getItemPurpose(slot);
	}
}
