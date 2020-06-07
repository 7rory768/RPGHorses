package org.plugins.rpghorses.guis;

import org.bukkit.inventory.ItemStack;

public class GUIItem {
	
	private ItemStack item;
	private ItemPurpose itemPurpose;
	private int slot;
	
	public GUIItem(ItemStack item, ItemPurpose itemPurpose, int slot) {
		this.item = item;
		this.itemPurpose = itemPurpose;
		this.slot = slot;
	}
	
	public ItemStack getItem() {
		return item;
	}
	
	public void setItem(ItemStack item) {
		this.item = item;
	}
	
	public ItemPurpose getItemPurpose() {
		return itemPurpose;
	}
	
	public void setItemPurpose(ItemPurpose itemPurpose) {
		this.itemPurpose = itemPurpose;
	}
	
	public int getSlot() {
		return slot;
	}
	
	public void setSlot(int slot) {
		this.slot = slot;
	}
}
