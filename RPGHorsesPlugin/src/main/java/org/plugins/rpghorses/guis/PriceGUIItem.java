package org.plugins.rpghorses.guis;

import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

public class PriceGUIItem extends GUIItem {
	
	private int priceChange;
	private Sound sound;
	private float volume, pitch;
	
	public PriceGUIItem(ItemStack item, ItemPurpose itemPurpose, int slot, int priceChange, Sound sound, float volume, float pitch) {
		super(item, itemPurpose, slot);
		this.priceChange = priceChange;
		this.sound = sound;
		this.volume = volume;
		this.pitch = pitch;
	}
	
	public int getPriceChange() {
		return priceChange;
	}
	
	public Sound getSound() {
		return sound;
	}
	
	public float getVolume() {
		return volume;
	}
	
	public float getPitch() {
		return pitch;
	}
}
