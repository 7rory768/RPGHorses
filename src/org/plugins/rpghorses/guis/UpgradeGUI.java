package org.plugins.rpghorses.guis;

import org.bukkit.inventory.Inventory;
import org.plugins.rpghorses.horses.RPGHorse;

public class UpgradeGUI {

    private Inventory inventory;
    private RPGHorse rpgHorse;

    public UpgradeGUI(RPGHorse rpgHorse, Inventory inventory) {
        this.rpgHorse = rpgHorse;
        this.inventory = inventory;
    }

    public RPGHorse getRPGHorse() {
        return rpgHorse;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

}
