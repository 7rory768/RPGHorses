package org.plugins.rpghorses.guis;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
public class TrailGUIItem extends GUIItem {
	
	private String trailName, trailPlaceholder;
	
	public TrailGUIItem(ItemStack item, ItemPurpose itemPurpose, boolean enabled, int slot, String trailName, String trailPlaceholder) {
		super(item, itemPurpose, enabled, slot);
		this.trailName = trailName;
		this.trailPlaceholder = trailPlaceholder;
	}
}
