package org.plugins.rpghorses.guis.instances;

import org.bukkit.inventory.Inventory;
import org.plugins.rpghorses.horses.RPGHorse;

import java.util.HashMap;

public class MarketGUIPage {

	private int pageNum;
	private Inventory gui;
	HashMap<Integer, RPGHorse> horseSlots;

	public MarketGUIPage(int pageNum, Inventory gui, HashMap<Integer, RPGHorse> horseSlots) {
		this.pageNum = pageNum;
		this.gui = gui;
		this.horseSlots = horseSlots;
	}

	public int getPageNum() {
		return pageNum;
	}

	public Inventory getGUI() {
		return gui;
	}

	public void setGUI(Inventory gui) {
		this.gui = gui;
	}

	public RPGHorse getRPGHorse(int slot) {
		return this.horseSlots.get(slot);
	}

	public HashMap<Integer, RPGHorse> getHorseSlots() {
		return horseSlots;
	}
}
