package org.plugins.rpghorses.guis.instances;

import org.bukkit.inventory.Inventory;
import org.plugins.rpghorses.horses.MarketHorse;

import java.util.HashMap;

public class YourHorsesGUIPage {

    private int pageNum;
    private Inventory gui;
    HashMap<Integer, MarketHorse> horseSlots;

    public YourHorsesGUIPage(int pageNum, Inventory gui, HashMap<Integer, MarketHorse> horseSlots) {
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

    public MarketHorse getMarketHorse(int slot) {
        return this.horseSlots.get(slot);
    }

    public HashMap<Integer, MarketHorse> getHorseSlots() {
        return horseSlots;
    }
}
