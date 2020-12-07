package org.plugins.rpghorses.guis.instances;

import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.ItemPurpose;
import org.plugins.rpghorses.guis.TrailGUIItem;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.RPGHorse;
import roryslibrary.util.ItemUtil;

import java.util.HashSet;

public class TrailsGUI {

	private RPGHorse rpgHorse;
	private Inventory inventory;
	private HashSet<TrailGUIItem> trails, unknownTrails;
	private TrailGUIItem currentTrail;

	public TrailsGUI(RPGHorse rpgHorse, Inventory inventory, HashSet<TrailGUIItem> trails, HashSet<TrailGUIItem> unknownTrails) {
		this.rpgHorse = rpgHorse;
		this.inventory = inventory;
		this.trails = trails;
		this.unknownTrails = unknownTrails;
	}

	public RPGHorse getRpgHorse() {
		return rpgHorse;
	}

	public Inventory getInventory() {
		return inventory;
	}

	public boolean applyTrail(int slot) {
		TrailGUIItem trailGUIItem = getTrailGUIItem(slot);

		if (trailGUIItem == null || unknownTrails.contains(trailGUIItem)) return false;

		if (currentTrail != null) {
			currentTrail.getItem().removeEnchantment(Enchantment.DURABILITY);
		}

		currentTrail = trailGUIItem;
		ItemUtil.addDurabilityGlow(currentTrail.getItem());
		String trailName = currentTrail.getTrailName();

		if (RPGHorsesMain.getVersion().getWeight() < 9) {
			((LegacyHorseInfo) rpgHorse.getHorseInfo()).setEffect(Effect.valueOf(trailName.toUpperCase()));
		} else {
			rpgHorse.setParticle(Particle.valueOf(trailName.toUpperCase()));
		}

		return true;
	}

	public TrailGUIItem getTrailGUIItem(int slot) {
		for (TrailGUIItem trailGUIItem : trails) {
			if (trailGUIItem.getSlot() == slot) {
				return trailGUIItem;
			}
		}

		for (TrailGUIItem trailGUIItem : unknownTrails) {
			if (trailGUIItem.getSlot() == slot) {
				return trailGUIItem;
			}
		}

		return null;
	}

	public ItemPurpose getItemPurpose(int slot) {
		TrailGUIItem trailGUIItem = getTrailGUIItem(slot);
		return trailGUIItem == null ? ItemPurpose.NOTHING : trailGUIItem.getItemPurpose();
	}

	public String getTrailName(int slot) {
		TrailGUIItem trailGUIItem = getTrailGUIItem(slot);
		return trailGUIItem == null ? "" : trailGUIItem.getTrailName();
	}
}
