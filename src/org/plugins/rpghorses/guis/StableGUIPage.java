package org.plugins.rpghorses.guis;

import org.bukkit.inventory.Inventory;
import org.plugins.rpghorses.horses.RPGHorse;
import org.plugins.rpghorses.players.HorseOwner;

import java.util.HashMap;

public class StableGUIPage {

    private HorseOwner horseOwner;
    private int pageNum;
    private Inventory gui;
    HashMap<Integer, RPGHorse> horseSlots;

    public StableGUIPage(HorseOwner horseOwner, int pageNum, Inventory gui, HashMap<Integer, RPGHorse> horseSlots) {
        this.horseOwner = horseOwner;
        this.pageNum = pageNum;
        this.gui = gui;
        this.horseSlots = horseSlots;
    }

    public HorseOwner getHorseOwner() {
        return horseOwner;
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
