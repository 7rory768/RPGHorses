package org.plugins.rpghorses.horses;

public class MarketHorse {

    private RPGHorse rpgHorse;
    private double price;
    private int index;

    public MarketHorse(int index, RPGHorse rpgHorse, double price) {
        this.rpgHorse = rpgHorse;
        this.price = price;
    }

    public int getIndex() {
        return index;
    }

    public RPGHorse getRPGHorse() {
        return rpgHorse;
    }

    public double getPrice() {
        return price;
    }
}
