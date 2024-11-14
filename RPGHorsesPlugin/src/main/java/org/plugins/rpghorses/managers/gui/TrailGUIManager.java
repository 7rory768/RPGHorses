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
import org.plugins.rpghorses.guis.instances.TrailsGUIPage;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.managers.ParticleManager;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.ItemUtil;
import org.plugins.rpghorses.utils.RPGMessagingUtil;
import org.plugins.rpghorses.version.Version;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

public class TrailGUIManager {

	private final RPGHorsesMain plugin;
	private final ParticleManager particleManager;
	private final HorseOwnerManager horseOwnerManager;

	private ItemStack unknownTrailItem, trailItem, fillItem;
	private GUIItem clearTrailItem, backItem, previousPageItem, nextPageItem;
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
		backItem = new GUIItem(ItemUtil.getItemStack(config, path + "back-item"), ItemPurpose.BACK, config.getBoolean(path + "back-item.enabled", true), ItemUtil.getSlot(config, path + "back-item"));
		previousPageItem = new GUIItem(ItemUtil.getItemStack(config, path + "previous-page-item"), ItemPurpose.PREVIOUS_PAGE, true, ItemUtil.getSlot(config, path + "previous-page-item"));
		nextPageItem = new GUIItem(ItemUtil.getItemStack(config, path + "next-page-item"), ItemPurpose.NEXT_PAGE, true, ItemUtil.getSlot(config, path + "next-page-item"));

		for (String trailName : particleManager.getValidParticles()) {
			ItemStack item = trailItem.clone();
			ItemMeta itemMeta = trailItem.getItemMeta();
			ItemMeta newMeta = itemMeta;

			String trailPlaceholder = config.getString(path + "trails." + trailName + ".placeholder", trailName);

			if (!config.getBoolean(path + "trails." + trailName + ".enabled", true))
				continue;

			if (Version.getVersion().getWeight() >= Version.v1_19.getWeight()) {
				if (config.isSet(path + "trails." + trailName + ".textures-url")) {
					newMeta = ItemUtil.applyCustomHead(itemMeta.clone(), config.getString(path + "trails." + trailName + ".textures-url", ""), config.getString(path + "trails." + trailName + ".skin-value", ""));
				}
			} else {
				newMeta = ItemUtil.applyCustomHead(itemMeta.clone(), config.getString(path + "trails." + trailName, ""));
			}

			if (newMeta != null && newMeta != itemMeta) {
				item.setItemMeta(newMeta);
			}

			validTrails.put(trailName, replacePlaceholders(new TrailGUIItem(item, ItemPurpose.TRAIL, true, -1, trailName, trailPlaceholder)));
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
				skipSlot = -1;
			}

			trailGUIItem.setSlot(slot);

			trailsLeft--;
			if ((totalTrails - trailsLeft) % 7 == 0) {
				if (++row == 4) row = 1; // New page

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

		rows = totalTrails >= 21 ? 6 : row + 3;
		backItem.setSlot(((rows - 1) * 9) + 3);
		clearTrailItem.setSlot(((rows - 1) * 9) + (backItem.isEnabled() ? 5 : 4));

		if (previousPageItem.getSlot() <= 0) previousPageItem.setSlot((rows - 1) * 9 + 1);
		if (nextPageItem.getSlot() <= 0) nextPageItem.setSlot((rows - 1) * 9 + 7);

		for (HorseOwner horseOwner : horseOwnerManager.getHorseOwners().values()) {
			if (horseOwner.getGUILocation() == GUILocation.TRAILS_GUI) {
				horseOwner.openTrailsGUIPage(setupTrailsGUI(horseOwner.getHorseGUI()).getPage(1));
			}
		}
	}

	public TrailsGUI setupTrailsGUI(HorseGUI horseGUI) {
		RPGHorse rpgHorse = horseGUI.getRpgHorse();
		Player p = rpgHorse.getHorseOwner().getPlayer();

		boolean perHorsePerms = plugin.getConfig().getBoolean("trails-options.per-horse-permissions", false);
		boolean hasStarPerm = perHorsePerms ? p.hasPermission("rpghorses.trail." + horseGUI.getRpgHorse().getIndex() + ".*") : p.hasPermission("rpghorses.trail.*");

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

		HashSet<TrailGUIItem> trails = new HashSet<>(), unknownTrails = new HashSet<>();
		List<TrailsGUIPage> pages = new ArrayList<>();
		int trailsDone = 0;

		Inventory inv = Bukkit.createInventory(p, rows * 9, RPGMessagingUtil.format(plugin.getConfig().getString("trail-gui-options.title")));

		for (TrailGUIItem trailGUIItem : validTrails.values()) {
			ItemStack item;
			if (hasStarPerm || (perHorsePerms && p.hasPermission("rpghorses.trail." + horseGUI.getRpgHorse().getIndex() + "." + trailGUIItem.getTrailName().toLowerCase())) || (!perHorsePerms && p.hasPermission("rpghorses.trail." + trailGUIItem.getTrailName().toLowerCase()))) {
				trails.add(trailGUIItem);
				item = trailGUIItem.getItem();
			} else {
				unknownTrails.add(trailGUIItem);
				item = unknownTrailItem;
			}

			item = item.clone();

			if (currentTrail == trailGUIItem) {
				ItemUtil.addDurabilityGlow(item);
			}

			inv.setItem(trailGUIItem.getSlot(), item);

			if (++trailsDone % 21 == 0 || trailsDone == validTrails.size()) {
				TrailsGUIPage page = new TrailsGUIPage(pages.size() + 1, horseGUI.getRpgHorse().getHorseOwner(), inv, trails, unknownTrails, currentTrail);
				pages.add(page);

				if (page.getTrails().contains(currentTrail) || page.getUnknownTrails().contains(currentTrail)) {
					page.setCurrentTrail(currentTrail);
				}

				if (backItem != null && backItem.isEnabled())
					inv.setItem(backItem.getSlot(), backItem.getItem());

				inv.setItem(clearTrailItem.getSlot(), clearTrailItem.getItem());

				if (trailsDone < validTrails.size())
					inv.setItem(nextPageItem.getSlot(), nextPageItem.getItem());

				if (pages.size() > 1)
					inv.setItem(previousPageItem.getSlot(), previousPageItem.getItem());

				for (int i = 0; i < inv.getSize(); i++) {
					if (inv.getItem(i) == null) {
						inv.setItem(i, fillItem);
					}
				}

				trails = new HashSet<>();
				unknownTrails = new HashSet<>();
				inv = Bukkit.createInventory(p, rows * 9, RPGMessagingUtil.format(plugin.getConfig().getString("trail-gui-options.title")));
			}
		}

		return new TrailsGUI(rpgHorse, pages, currentTrail);
	}

	public TrailGUIItem replacePlaceholders(TrailGUIItem trailGUIItem) {
		String name = trailGUIItem.getTrailPlaceholder();

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

	public ItemPurpose getItemPurpose(int slot, TrailsGUIPage trailsGUI) {
		if (slot == previousPageItem.getSlot()) {
			return ItemPurpose.PREVIOUS_PAGE;
		} else if (slot == nextPageItem.getSlot()) {
			return ItemPurpose.NEXT_PAGE;
		} else if (slot == backItem.getSlot()) {
			return ItemPurpose.BACK;
		} else if (slot == clearTrailItem.getSlot()) {
			return ItemPurpose.CLEAR_TRAIL;
		}

		return trailsGUI.getItemPurpose(slot);
	}
}
