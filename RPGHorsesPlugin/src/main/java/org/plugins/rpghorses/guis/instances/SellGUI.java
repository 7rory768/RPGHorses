package org.plugins.rpghorses.guis.instances;

import org.bukkit.inventory.Inventory;
import org.plugins.rpghorses.horses.RPGHorse;

public class SellGUI {

	private RPGHorse rpgHorse;
	private Inventory inventory;
	private int price = 0;

	public SellGUI(RPGHorse rpgHorse, Inventory inventory) {
		this.rpgHorse = rpgHorse;
		this.inventory = inventory;
	}

	public RPGHorse getRpgHorse() {
		return rpgHorse;
	}

	public Inventory getInventory() {
		return inventory;
	}

	public int increasePrice(int amount) {
		price += amount;
		if (price < 0) {
			price = 0;
		}
		return price;
	}

	public int getPrice() {
		return price;
	}
}
