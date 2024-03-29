package org.plugins.rpghorses.managers.gui;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
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
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.managers.ParticleManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.ItemUtil;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import org.plugins.rpghorses.version.Version;

import java.util.*;

public class TrailGUIManager {

	private final RPGHorsesMain plugin;
	private final ParticleManager particleManager;
	private final HorseOwnerManager horseOwnerManager;

	private ItemStack unknownTrailItem, trailItem, fillItem;
	private GUIItem clearTrailItem, backItem;
	private final LinkedHashMap<String, TrailGUIItem> validTrails = new LinkedHashMap<>();
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
		clearTrailItem = new GUIItem(ItemUtil.getItemStack(config, path + "clear-trail-item"), ItemPurpose.CLEAR_TRAIL, true, ItemUtil.getSlot(config, path + "clear-trail-item"));
		fillItem = ItemUtil.getItemStack(config, path + "fill-item");
		backItem = new GUIItem(ItemUtil.getItemStack(config, path + "back-item"), ItemPurpose.BACK, true, ItemUtil.getSlot(config, path + "back-item"));

		for (String trailName : config.getConfigurationSection(path + "trails").getKeys(false)) {
			if (particleManager.isValidParticle(trailName)) {
				ItemStack item = trailItem.clone();
				ItemMeta itemMeta = trailItem.getItemMeta();
				ItemMeta newMeta;

				if (Version.getVersion().getWeight() >= Version.v1_20.getWeight()) {
					newMeta = ItemUtil.applyCustomHead(itemMeta.clone(), config.getString(path + "trails." + trailName + ".textures-url", ""), config.getString(path + "trails." + trailName + ".skin-value", ""));
				} else {
					newMeta = ItemUtil.applyCustomHead(itemMeta.clone(), config.getString(path + "trails." + trailName, ""));
				}

				if (newMeta != null) {
					item.setItemMeta(newMeta);
				}
				validTrails.put(trailName, replacePlaceholders(new TrailGUIItem(item, ItemPurpose.TRAIL, true, -1, trailName)));
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

		for (TrailGUIItem trailGUIItem : validTrails.values()) {
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
		backItem.setSlot(((rows - 1) * 9) + 3);
		clearTrailItem.setSlot(((rows - 1) * 9) + 5);

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

		boolean perHorsePerms = plugin.getConfig().getBoolean("trails-options.per-horse-permissions", false);
		boolean hasStarPerm = perHorsePerms ? p.hasPermission("rpghorses.trail." + horseGUI.getRpgHorse().getIndex() + ".*") : p.hasPermission("rpghorses.trail.*");
		for (TrailGUIItem trailGUIItem : validTrails.values()) {
			ItemStack item;
			if (hasStarPerm || (perHorsePerms && p.hasPermission("rpghorses.trail." + horseGUI.getRpgHorse().getIndex() + "." + trailGUIItem.getTrailName().toLowerCase())) || (!perHorsePerms && p.hasPermission("rpghorses.trail." + trailGUIItem.getTrailName().toLowerCase()))) {
				trails.add(trailGUIItem);
				item = trailGUIItem.getItem();
			} else {
				unknownTrails.add(trailGUIItem);
				item = unknownTrailItem;
			}
			inv.setItem(trailGUIItem.getSlot(), item.clone());
		}

		inv.setItem(backItem.getSlot(), backItem.getItem());
		inv.setItem(clearTrailItem.getSlot(), clearTrailItem.getItem());

		for (int i = 0; i < inv.getSize(); i++) {
			if (inv.getItem(i) == null) {
				inv.setItem(i, fillItem);
			}
		}

		TrailGUIItem currentTrail = null;

		if (RPGHorsesMain.getVersion().getWeight() < 9) {
			Effect effect = ((LegacyHorseInfo) rpgHorse.getHorseInfo()).getEffect();
			if (effect != null) {
				currentTrail = validTrails.get(effect.name());
			}
		} else {
			String particleName = rpgHorse.getParticle() == null ? null : rpgHorse.getParticle().name();
			if (particleName != null) {
				currentTrail = validTrails.get(particleName);
			}
		}

		return new TrailsGUI(rpgHorse, inv, trails, unknownTrails, currentTrail);
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
		} else if (slot == clearTrailItem.getSlot()) {
			return ItemPurpose.CLEAR_TRAIL;
		}

		return trailsGUI.getItemPurpose(slot);
	}
}
