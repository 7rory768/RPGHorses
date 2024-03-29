package org.plugins.rpghorses.guis.instances;

import lombok.Getter;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.ItemPurpose;
import org.plugins.rpghorses.guis.TrailGUIItem;
import org.plugins.rpghorses.horseinfo.LegacyHorseInfo;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.utils.ItemUtil;

import java.util.HashMap;
import java.util.HashSet;

public class TrailsGUI {

	private final RPGHorse rpgHorse;
	private final Inventory inventory;
	private final HashSet<TrailGUIItem> trails, unknownTrails;
	@Getter
	private TrailGUIItem currentTrail;

	public TrailsGUI(RPGHorse rpgHorse, Inventory inventory, HashSet<TrailGUIItem> trails, HashSet<TrailGUIItem> unknownTrails, TrailGUIItem currentTrail) {
		this.rpgHorse = rpgHorse;
		this.inventory = inventory;
		this.trails = trails;
		this.unknownTrails = unknownTrails;
		this.currentTrail = currentTrail;

		if (currentTrail != null) {
			ItemUtil.addDurabilityGlow(inventory.getItem(currentTrail.getSlot()));
		}
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
			inventory.getItem(currentTrail.getSlot()).removeEnchantment(Enchantment.DURABILITY);
		}

		currentTrail = trailGUIItem;
		ItemUtil.addDurabilityGlow(inventory.getItem(currentTrail.getSlot()));
		String trailName = currentTrail.getTrailName();

		if (RPGHorsesMain.getVersion().getWeight() < 9) {
			((LegacyHorseInfo) rpgHorse.getHorseInfo()).setEffect(Effect.valueOf(trailName.toUpperCase()));
		} else {
			rpgHorse.setParticle(Particle.valueOf(trailName.toUpperCase()));
		}

		return true;
	}

	public boolean removeTrail() {
		boolean hadTrail = false;

		if (currentTrail != null) {
			currentTrail.getItem().removeEnchantment(Enchantment.DURABILITY);
		}

		currentTrail = null;

		if (RPGHorsesMain.getVersion().getWeight() < 9) {
			hadTrail = ((LegacyHorseInfo) rpgHorse.getHorseInfo()).getEffect() != null;
			((LegacyHorseInfo) rpgHorse.getHorseInfo()).setEffect(null);
		} else {
			hadTrail = rpgHorse.getParticle() != null;
			rpgHorse.setParticle(null);
		}

		return hadTrail;
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
