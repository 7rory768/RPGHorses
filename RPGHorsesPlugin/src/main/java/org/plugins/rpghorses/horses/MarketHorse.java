package org.plugins.rpghorses.horses;

import lombok.Getter;
import lombok.Setter;

public class MarketHorse {
	
	@Getter
	private RPGHorse rpgHorse;
	@Getter
	private double price;
	@Getter @Setter
	private int id, index;
	
	public MarketHorse(int id, RPGHorse rpgHorse, double price, int index) {
		this.id = id;
		this.rpgHorse = rpgHorse;
		this.price = price;
		this.index = index;
	}
	
	public RPGHorse getRPGHorse() {
		return rpgHorse;
	}
	
}
