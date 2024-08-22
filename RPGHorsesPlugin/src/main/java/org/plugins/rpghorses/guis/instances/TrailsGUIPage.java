package org.plugins.rpghorses.guis.instances;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.Inventory;
import org.plugins.rpghorses.guis.ItemPurpose;
import org.plugins.rpghorses.guis.TrailGUIItem;
import org.plugins.rpghorses.players.HorseOwner;
import org.plugins.rpghorses.utils.ItemUtil;

import java.util.HashSet;

/*
 * @author Rory Skipper (Roree) on 2024-07-05
 */
@Getter
@Setter
public class TrailsGUIPage {

	private final Inventory inventory;
	private final HashSet<TrailGUIItem> trails, unknownTrails;
	private final int pageNum;
	private final HorseOwner horseOwner;
	private TrailGUIItem currentTrail;

	public TrailsGUIPage(int pageNum, HorseOwner horseOwner, Inventory inventory, HashSet<TrailGUIItem> trails, HashSet<TrailGUIItem> unknownTrails, TrailGUIItem currentTrail) {
		this.pageNum = pageNum;
		this.horseOwner = horseOwner;
		this.inventory = inventory;
		this.trails = trails;
		this.unknownTrails = unknownTrails;
	}

	public void setCurrentTrail(TrailGUIItem newTrail) {
		if (newTrail != currentTrail && currentTrail != null && trails.contains(currentTrail)) {
			ItemUtil.removeDurabilityGlow(inventory.getItem(currentTrail.getSlot()));
		}

		currentTrail = newTrail;

		if (newTrail != null && trails.contains(newTrail)) {
			ItemUtil.addDurabilityGlow(inventory.getItem(newTrail.getSlot()));
		}
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
