package org.plugins.rpghorses.guis;

import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

@Getter
public class PriceGUIItem extends GUIItem {
	
	private int priceChange;
	private Sound sound;
	private float volume, pitch;
	
	public PriceGUIItem(ItemStack item, ItemPurpose itemPurpose, boolean enabled, int slot, int priceChange, Sound sound, float volume, float pitch) {
		super(item, itemPurpose, enabled, slot);
		this.priceChange = priceChange;
		this.sound = sound;
		this.volume = volume;
		this.pitch = pitch;
	}
}
