package org.plugins.rpghorses.guis.instances;

import org.bukkit.inventory.Inventory;
import org.plugins.rpghorses.horses.RPGHorse;

public class HorseGUI {

	private final RPGHorse rpgHorse;
	private final Inventory inventory;

	public HorseGUI(RPGHorse rpgHorse, Inventory inventory) {
		this.rpgHorse = rpgHorse;
		this.inventory = inventory;
	}

	public RPGHorse getRpgHorse() {
		return rpgHorse;
	}

	public Inventory getInventory() {
		return inventory;
	}
}
