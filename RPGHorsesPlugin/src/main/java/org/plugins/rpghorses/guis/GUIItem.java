package org.plugins.rpghorses.guis;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.plugins.rpghorses.utils.ItemUtil;

@Getter
@Setter
public class GUIItem {
	
	private ItemStack item;
	private ItemPurpose itemPurpose;
	private boolean enabled;
	private int slot;

	public GUIItem(ConfigurationSection config) {
		this (ItemUtil.getItemStack(config), ItemPurpose.valueOf(config.getString("purpose", "NOTHING")), config.getBoolean("enabled", true), ItemUtil.getSlot(config));
	}
	
	public GUIItem(ItemStack item, ItemPurpose itemPurpose, boolean enabled, int slot) {
		this.item = item;
		this.itemPurpose = itemPurpose;
		this.enabled = enabled;
		this.slot = slot;
	}
}
