package org.plugins.rpghorses.guis;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
public class TrailGUIItem extends GUIItem {
	
	private String trailName;
	
	public TrailGUIItem(ItemStack item, ItemPurpose itemPurpose, boolean enabled, int slot, String trailName) {
		super(item, itemPurpose, enabled, slot);
		this.trailName = trailName;
	}
}
