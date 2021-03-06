package org.plugins.rpghorses.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.plugins.rpghorses.RPGHorsesMain;
import org.plugins.rpghorses.guis.GUILocation;
import org.plugins.rpghorses.managers.HorseOwnerManager;
import org.plugins.rpghorses.managers.gui.StableGUIManager;
import org.plugins.rpghorses.players.HorseOwner;

public class InventoryCloseListener implements Listener {
	
	private final HorseOwnerManager horseOwnerManager;
	private final StableGUIManager stableGuiManager;
	
	public InventoryCloseListener(RPGHorsesMain plugin, HorseOwnerManager horseOwnerManager, StableGUIManager stableGuiManager) {
		this.horseOwnerManager = horseOwnerManager;
		this.stableGuiManager = stableGuiManager;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onInvClose(InventoryCloseEvent e) {
		Player p = (Player) e.getPlayer();
		HorseOwner horseOwner = this.horseOwnerManager.getHorseOwner(p);
		if (horseOwner.isInGUI(GUILocation.STABLE_GUI) && this.stableGuiManager.getUpgradeGUI(p) != null) {
			this.stableGuiManager.removeUpgradeGUI(p);
		}
		
		horseOwner.setGUILocation(GUILocation.NONE);
	}
	
}
