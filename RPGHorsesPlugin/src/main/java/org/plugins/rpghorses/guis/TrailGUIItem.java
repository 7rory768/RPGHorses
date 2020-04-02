package org.plugins.rpghorses.guis;

import org.bukkit.inventory.ItemStack;

public class TrailGUIItem extends GUIItem {

	private String trailName;

	public TrailGUIItem(ItemStack item, ItemPurpose itemPurpose, int slot, String trailName) {
		super(item, itemPurpose, slot);
		this.trailName = trailName;
	}

	public String getTrailName() {
		return trailName;
	}
}
